package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.ModCreativeTabs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * SPINE-3 / t_15e43d12: item model coverage — every registered item ships a
 * model JSON, so it renders as itself instead of the missing-model placeholder.
 *
 * <p>The user report: Thaumcraft items are not visible. 1.21.1 resolves an
 * item's inventory model at {@code assets/thaumcraft/models/item/<id>.json}
 * (the id path verbatim, dot included, e.g. {@code spawnegg.foo.json}); with
 * no such file the item is drawn as the magenta/black placeholder. Measured on
 * the parent tip: the item registry carries 52 shipped items and the tree
 * shipped exactly ONE item model ({@code blockcustomore.json}).</p>
 *
 * <p><b>The expected set is derived from the live item registry at test
 * time</b> ({@link ModCreativeTabs#isShippedCatalogItem} — the same
 * namespace-plus-fixture filter the creative tab uses). A test with a
 * hardcoded list of ids can never fail when a later catalog card registers a
 * new item, which is the exact shape of the defect this card closes: the
 * items registered and nobody noticed they had no model. Every current and
 * future catalog item is therefore accountable to this gate the moment it
 * registers, in both directions (probe 2 rejects a model for an id that is
 * not shipped content).</p>
 *
 * <h2>Assets vs models (D-004)</h2>
 * The model JSONs are port-authored and committed. The textures are the
 * original assets, All Rights Reserved, and ship only through the local
 * overlay (docs/asset-mapping.csv is the published-path contract,
 * {@code scripts/check_asset_paths.py} gates both jar flavors). A model whose
 * sprite the overlay does not publish renders magenta — that is the overlay's
 * contract, not this card's, so probe 4 gates sprite resolution <em>only when
 * an overlay is actually installed</em> and logs the dependency when it is
 * not, so a code-only tree cannot be misdiagnosed as broken here.
 *
 * <h2>Per-meta families</h2>
 * {@code itemresource} (20 registered sprites), {@code itemshard},
 * {@code itemnugget} and the meta-carrying block families are ONE registered
 * id each carrying {@code thaumcraft:item_meta} / a {@code variant} property,
 * so they need exactly one model. 1.21.1 picks one model per item id, so the
 * model carries the original's <b>meta-0</b> sprite — the index-0 line of the
 * original's own {@code registerIcons}/{@code registerBlockIcons} body (the
 * rule TC-M6-08-s-c3 established for spawn eggs). Per-meta appearance in the
 * inventory (the original rendered meta-indexed icons through
 * {@code getIconFromDamage}) is a separate fidelity question: {@code
 * item.thaumcraft.<id>.<meta>} keeps per-meta identity in the <em>label</em>,
 * the models do not, and that gap is recorded in the docs, not hidden.
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ItemModelCoverageGameTest {

    /** The asset tree of this mod, on the runtime classpath. */
    private static final String ASSET_ROOT = "/assets/" + ThaumcraftClassic.MOD_ID + "/";
    private static final String MODELS_ITEM_DIR = ASSET_ROOT + "models/item/";

    /**
     * A sprite the real overlay (and the CI fixture overlay built from
     * docs/asset-mapping.csv) always publishes: the ore sprite that made
     * TC-M7-01-s-a's magenta screenshot. Its presence is what tells probe 4
     * that an overlay is installed.
     */
    private static final String OVERLAY_PROBE_SPRITE = "textures/block/amberore.png";

    /**
     * Vanilla sprites are only checkable when the vanilla jar is on this
     * classpath; probe one known vanilla texture instead of assuming.
     */
    private static final boolean VANILLA_PROBE_PRESENT =
            ItemModelCoverageGameTest.class.getResource("/assets/minecraft/textures/item/paper.png") != null;

    /**
     * The same question for model JSONs: the vanilla model tree is CLIENT assets,
     * so a dedicated game-test server may not address it at all. Probe one known
     * vanilla model — the egg template — and log the answer, so probe 1's
     * vanilla-parent-permissiveness is visible in the run log and never reads
     * as a silent pass.
     */
    private static final boolean VANILLA_MODELS_PRESENT =
            ItemModelCoverageGameTest.class.getResource(
                    "/assets/minecraft/models/item/template_spawn_egg.json") != null;

    private ItemModelCoverageGameTest() {
    }

    // ------------------------------------------------------------------ probes

    /**
     * Every shipped item in the live registry has a model JSON at
     * {@code models/item/<id path>.json}. The expected set is enumerated from
     * {@link BuiltInRegistries#ITEM} at test time — never hardcoded.
     */
    @GameTest(template = "empty")
    public static void everyShippedItemHasAModelJson(final GameTestHelper helper) {
        final Map<String, ResourceLocation> expected = shippedItemIds();
        helper.assertTrue(!expected.isEmpty(),
                "the registry-derived expected item set is empty — this probe can never fail");

        final List<String> missing = new ArrayList<>();
        for (final String path : expected.keySet()) {
            if (!modelExists("item/" + path)) {
                missing.add(path);
            }
        }
        ThaumcraftClassic.LOGGER.info("[SPINE-3] item model coverage: {} of {} shipped items have a model, {} missing",
                expected.size() - missing.size(), expected.size(), missing.size());
        helper.assertTrue(missing.isEmpty(),
                missing.size() + " of " + expected.size() + " registered thaumcraft items have no "
                        + MODELS_ITEM_DIR + "<id>.json — they render the missing-model placeholder in "
                        + "inventory: " + preview(missing));
        helper.succeed();
    }

    /**
     * The converse: every committed item model names a shipped registered id.
     * A model for an unregistered id is a dead asset (the lang audit's
     * {@code noDeadLangEntries} in model form), and a model named for a
     * test-fixture item is scaffolding shipped to players. The listing is the
     * shipped tree, so a probe that listed nothing could never fail — that
     * case is asserted, not skipped.
     */
    @GameTest(template = "empty")
    public static void everyItemModelNamesAShippedItem(final GameTestHelper helper) {
        final Set<String> shipped = shippedItemIds().keySet();
        final List<String> committed = committedModelPaths();
        helper.assertTrue(!committed.isEmpty(),
                "no item models found on the classpath under " + MODELS_ITEM_DIR
                        + " — this probe can never fail; the asset tree is not on the runtime classpath");

        final List<String> dead = new ArrayList<>();
        final List<String> fixtures = new ArrayList<>();
        for (final String path : committed) {
            if (shipped.contains(path)) {
                continue;
            }
            if (ModCreativeTabs.isTestFixtureId(path)) {
                fixtures.add(path);
            } else {
                dead.add(path);
            }
        }
        ThaumcraftClassic.LOGGER.info("[SPINE-3] {} committed item models, {} shipped items, {} dead, {} fixture",
                committed.size(), shipped.size(), dead.size(), fixtures.size());
        helper.assertTrue(fixtures.isEmpty(),
                "a test-fixture item has a committed model (scaffolding shipped to players): " + fixtures);
        helper.assertTrue(dead.isEmpty(),
                "committed item models name ids that are not shipped registered content: " + preview(dead));
        helper.succeed();
    }

    /**
     * Structural resolution: every model parses, declares a parent, uses an
     * explicit namespace (an unnamespaced ref defaults to {@code thaumcraft}
     * — the rule {@code scripts/check_asset_paths.py} gate 3 records, so a
     * vanilla parent must be written {@code minecraft:item/generated}), every
     * {@code thaumcraft:} parent exists in the shipped tree, the parent chain
     * terminates, and the chain declares at least one sprite. A model with no
     * sprite anywhere in its chain is invisible in inventory, which is the
     * defect class this card closes.
     */
    @GameTest(template = "empty")
    public static void everyItemModelChainResolves(final GameTestHelper helper) {
        final List<String> broken = new ArrayList<>();
        int checked = 0;
        int vanillaTerminatedChains = 0;
        for (final String path : shippedItemIds().keySet()) {
            if (!modelExists("item/" + path)) {
                broken.add(path + " (no model file)");
                continue;
            }
            checked++;
            final List<String> problems = new ArrayList<>();
            final Deque<String> chain = new ArrayDeque<>();
            chain.add("item/" + path);
            final Set<String> seen = new LinkedHashSet<>();
            int sprites = 0;
            boolean vanillaTerminated = false;
            while (!chain.isEmpty()) {
                final String ref = chain.poll();
                if (!seen.add(ref)) {
                    problems.add("parent cycle at " + ref);
                    break;
                }
                final JsonObject model = parseModel(ref);
                if (model == null) {
                    problems.add("unparsable model " + ref);
                    break;
                }
                if (model.has("textures")) {
                    final JsonObject textures = model.getAsJsonObject("textures");
                    for (final Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                        if (!entry.getValue().getAsString().startsWith("#")) {
                            sprites++;
                        }
                    }
                }
                if (!model.has("parent")) {
                    if (seen.size() == 1) {
                        problems.add("no parent — a bare model declares no geometry");
                    }
                    continue;
                }
                final String parent = model.get("parent").getAsString();
                if (!parent.contains(":")) {
                    problems.add("unnamespaced parent '" + parent + "' resolves to "
                            + ThaumcraftClassic.MOD_ID + ":" + parent + "; a vanilla parent must say minecraft:");
                    continue;
                }
                if (parent.startsWith(ThaumcraftClassic.MOD_ID + ":")) {
                    final String parentPath = parent.substring(ThaumcraftClassic.MOD_ID.length() + 1);
                    if (!modelExists(parentPath)) {
                        problems.add("parent model missing from the repo: " + parent);
                        continue;
                    }
                    chain.add(parentPath);
                }
                if (parent.startsWith(ResourceLocation.DEFAULT_NAMESPACE + ":")) {
                    // A minecraft: parent ships in the vanilla jar and its sprite
                    // is inherited, not re-declared by the child. Measured in the
                    // 1.21.1 client jar: models/item/generated.json parents
                    // builtin/generated (no textures block — the sprite is the
                    // item built-in), and models/item/template_spawn_egg.json
                    // declares layer0 item/spawn_egg + layer1
                    // item/spawn_egg_overlay. So a chain that terminates in a
                    // vanilla parent always renders; demanding a sprite from the
                    // child would fail every vanilla-shaped item, including
                    // every spawn egg (zombie_spawn_egg.json is exactly
                    // {"parent":"minecraft:item/template_spawn_egg"}).
                    vanillaTerminated = true;
                    // Counted precisely when the vanilla model JSONs are
                    // addressable from this classpath (probe 1 logs which case ran).
                    sprites += countVanillaChainSprites(parent, seen);
                }
            }
            if (vanillaTerminated) {
                vanillaTerminatedChains++;
            }
            // A chain that stays inside this namespace and declares no sprite key
            // anywhere renders nothing, which is the defect class this probe closes.
            if (!vanillaTerminated && sprites == 0 && problems.isEmpty()) {
                problems.add("no sprite declared in the model chain");
            }
            for (final String problem : problems) {
                broken.add(path + " (" + problem + ")");
            }
        }
        // What the walk could not see: a minecraft: parent's sprites are counted
        // precisely only when the vanilla model JSONs are addressable from this
        // classpath (they are CLIENT assets). Log it so a permissive pass is
        // never silent.
        ThaumcraftClassic.LOGGER.info(
                "[SPINE-3] model chains: {} models walked, {} end on a vanilla parent "
                        + "(sprites inherited from vanilla; vanilla model JSONs verifiable on this classpath: {}), {} broken",
                checked, vanillaTerminatedChains, VANILLA_MODELS_PRESENT, broken.size());
        helper.assertTrue(broken.isEmpty(),
                broken.size() + " item model chains do not resolve: " + preview(broken));
        helper.succeed();
    }

    /**
     * The overlay contract, gated only where an overlay is installed: every
     * {@code thaumcraft:} sprite a shipped model chain asks for exists at the
     * 1.21.1 published path {@code textures/<dir>/<name>.png}. With no overlay
     * the sprites are absent BY DESIGN (D-004: original assets are All Rights
     * Reserved and never committed), so the probe says so instead of failing
     * a code-only tree — {@code scripts/check_asset_paths.py} plus
     * {@code OriginalTextureOverlayTest} are the gates that see the overlay.
     */
    @GameTest(template = "empty")
    public static void everyModelSpriteResolvesWhenTheOverlayIsInstalled(final GameTestHelper helper) {
        if (!classResourceExists(OVERLAY_PROBE_SPRITE)) {
            ThaumcraftClassic.LOGGER.info("[SPINE-3] sprite resolution not gated: no asset overlay on this "
                    + "classpath ({} is absent) — D-004 ships original textures as a local overlay only; "
                    + "models may reference sprites this tree does not publish", OVERLAY_PROBE_SPRITE);
            helper.succeed();
            return;
        }
        final List<String> unresolved = new ArrayList<>();
        int refs = 0;
        for (final String path : shippedItemIds().keySet()) {
            final Deque<String> chain = new ArrayDeque<>();
            chain.add("item/" + path);
            final Set<String> seen = new LinkedHashSet<>();
            while (!chain.isEmpty()) {
                final String ref = chain.poll();
                if (!seen.add(ref)) {
                    continue;
                }
                final JsonObject model = parseModel(ref);
                if (model == null) {
                    continue;
                }
                if (model.has("textures")) {
                    for (final Map.Entry<String, JsonElement> entry
                            : model.getAsJsonObject("textures").entrySet()) {
                        final String sprite = entry.getValue().getAsString();
                        if (sprite.startsWith("#")) {
                            continue;
                        }
                        if (sprite.startsWith(ThaumcraftClassic.MOD_ID + ":")) {
                            refs++;
                            final String texturePath = "textures/"
                                    + sprite.substring(ThaumcraftClassic.MOD_ID.length() + 1) + ".png";
                            if (!classResourceExists(texturePath)) {
                                unresolved.add(ref + " [" + entry.getKey() + " = " + sprite + "] -> " + texturePath);
                            }
                        } else if (sprite.startsWith("minecraft:")) {
                            // A vanilla fallback sprite (the overlay publishes no
                            // original art for this item) is a real 1.21.1 asset
                            // or it renders magenta; the vanilla jar is on the
                            // game-test classpath, so the same classloader check
                            // closes that defect class.
                            refs++;
                            final String vanillaPath = "assets/minecraft/textures/"
                                    + sprite.substring("minecraft:".length()) + ".png";
                            if (VANILLA_PROBE_PRESENT && ItemModelCoverageGameTest.class
                                    .getResource("/" + vanillaPath) == null) {
                                unresolved.add(ref + " [" + entry.getKey() + " = " + sprite
                                        + "] -> " + vanillaPath + " (not in the vanilla jar)");
                            }
                        }
                    }
                }
                if (model.has("parent") && model.get("parent").getAsString()
                        .startsWith(ThaumcraftClassic.MOD_ID + ":")) {
                    chain.add(model.get("parent").getAsString().substring(ThaumcraftClassic.MOD_ID.length() + 1));
                }
            }
        }
        ThaumcraftClassic.LOGGER.info("[SPINE-3] overlay sprite resolution: {} thaumcraft sprite references, {} unresolved",
                refs, unresolved.size());
        helper.assertTrue(unresolved.isEmpty(),
                unresolved.size() + " model sprites are not published at the 1.21.1 path by the installed "
                        + "overlay: " + preview(unresolved));
        helper.succeed();
    }

    /**
     * Models, lang and tabs stay consistent (minecraft-modding SKILL §15 rule
     * 9: the localization probe's required set is the block/item/menu/
     * entity-type/creative-tab description ids). The registry-derived item set
     * is walked, so this is the same expected set probe 1 gates: every item
     * that must render a name has a shipped label — either its
     * {@code item.<ns>.<id>} description id, or, for the multi-meta families
     * whose {@code getName(ItemStack)} returns the per-meta key
     * ({@code item.thaumcraft.itemresource.5}, the original's per-metadata
     * name shape), at least one shipped key in that family. A family with
     * neither is an item that renders its raw key.
     *
     * <p>Which meta of which family must carry which key is
     * {@code LocalizationGameTest}'s contract (it owns
     * {@code metasOf}/{@code META_COUNT}/{@code VALID_METAS}); this probe only
     * asks the model/lang/tabs consistency question, and this card adds no lang
     * key — models need none, and an unsolicited key is a red gate on
     * {@code noDeadLangEntries}.</p>
     */
    @GameTest(template = "empty")
    public static void everyShippedItemHasALabelKey(final GameTestHelper helper) {
        final JsonObject lang = loadJson(ASSET_ROOT + "lang/en_us.json").getAsJsonObject();
        final Map<String, ResourceLocation> shipped = shippedItemIds();
        final List<String> unlabelled = new ArrayList<>();
        for (final Map.Entry<String, ResourceLocation> entry : shipped.entrySet()) {
            final Item item = BuiltInRegistries.ITEM.get(entry.getValue());
            final String key = item.getDescriptionId();
            final boolean baseShipped = lang.has(key) && !lang.get(key).getAsString().isEmpty();
            final String familyPrefix = key + ".";
            final boolean familyShipped = lang.entrySet().stream()
                    .anyMatch(e -> e.getKey().startsWith(familyPrefix) && !e.getValue().getAsString().isEmpty());
            if (!baseShipped && !familyShipped) {
                unlabelled.add(entry.getKey() + " -> " + key);
            }
        }
        ThaumcraftClassic.LOGGER.info("[SPINE-3] label keys: {} shipped items, {} with no label at all",
                shipped.size(), unlabelled.size());
        helper.assertTrue(unlabelled.isEmpty(),
                unlabelled.size() + " shipped items have neither a base label key nor a per-meta label "
                        + "family in lang/en_us.json: " + preview(unlabelled));
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /**
     * The expected set: every registered item that is shipped content, keyed
     * by registry id path. Derived from the live registry through the tab's
     * own filter, so the fixture items registered under {@code
     * -Dthaumcraft.testprobe=true} are excluded by the same rule that keeps
     * them out of the tab.
     */
    private static Map<String, ResourceLocation> shippedItemIds() {
        final Map<String, ResourceLocation> ids = new TreeMap<>();
        for (final Item item : BuiltInRegistries.ITEM) {
            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (ModCreativeTabs.isShippedCatalogItem(item)) {
                ids.put(id.getPath(), id);
            }
        }
        return ids;
    }

    /** Every {@code models/item/*.json} committed in this mod's asset tree. */
    private static List<String> committedModelPaths() {
        final java.net.URL dir = ItemModelCoverageGameTest.class.getResource(MODELS_ITEM_DIR);
        if (dir == null) {
            return List.of(); // the probe asserts a non-empty listing, so this cannot pass silently
        }
        final Path root;
        try {
            root = Path.of(dir.toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException("the shipped asset tree is not addressable on the classpath: "
                    + MODELS_ITEM_DIR, e);
        }
        if (!Files.isDirectory(root)) {
            return List.of(); // packaged in a jar: listResources-style enumeration is the other gate
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> {
                        final String nested = root.relativize(p).toString()
                                .replace('\\', '/'); // models/item is flat today; keep subdirs correct
                        return nested.substring(0, nested.length() - 5);
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean modelExists(final String modelPath) {
        return classResourceExists("models/" + modelPath + ".json");
    }

    private static JsonObject parseModel(final String modelPath) {
        try (InputStream in = ItemModelCoverageGameTest.class
                .getResourceAsStream(ASSET_ROOT + "models/" + modelPath + ".json")) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            ThaumcraftClassic.LOGGER.warn("[SPINE-3] unreadable model models/{}.json", modelPath, e);
            return null;
        }
    }

    private static boolean classResourceExists(final String relativePath) {
        return ItemModelCoverageGameTest.class.getResource(ASSET_ROOT + relativePath) != null;
    }

    /**
     * The sprite keys one model JSON declares: a {@code textures} value that does
     * not start with {@code #} is a real sprite path.
     */
    private static int countSpriteKeys(final JsonObject model) {
        if (!model.has("textures")) {
            return 0;
        }
        int keys = 0;
        for (final Map.Entry<String, JsonElement> entry : model.getAsJsonObject("textures").entrySet()) {
            if (!entry.getValue().getAsString().startsWith("#")) {
                keys++;
            }
        }
        return keys;
    }

    /**
     * Walk a {@code minecraft:} parent chain through the vanilla jar (it is on the
     * game-test classpath — {@link #VANILLA_PROBE_PRESENT} is the same check) and
     * count the sprite keys its chain declares. Vanilla model JSONs write their
     * parent and texture paths unnamespaced, so an unnamespaced parent resolves to
     * the {@code minecraft} namespace, exactly as the model loader does.
     *
     * <p>When the vanilla jar is not on the classpath the chain is unverifiable:
     * the sprites stay inherited and uncounted, which is this probe's pre-existing
     * permissive contract for a vanilla parent, so no tree is made red by an
     * absent jar.</p>
     */
    private static int countVanillaChainSprites(final String parent, final Set<String> seen) {
        int sprites = 0;
        String ref = parent;
        while (ref != null && ref.startsWith(ResourceLocation.DEFAULT_NAMESPACE + ":")) {
            final String path = ref.substring(ResourceLocation.DEFAULT_NAMESPACE.length() + 1);
            if (!seen.add("vanilla:" + path)) {
                return sprites; // already walked — the caller's cycle guard covers mod chains
            }
            final JsonObject vanilla;
            try (InputStream in = ItemModelCoverageGameTest.class
                    .getResourceAsStream("/assets/" + ResourceLocation.DEFAULT_NAMESPACE + "/models/"
                            + path + ".json")) {
                if (in == null) {
                    return sprites; // no vanilla jar on this classpath: inherited, uncounted
                }
                vanilla = JsonParser
                        .parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                        .getAsJsonObject();
            } catch (IOException | RuntimeException e) {
                ThaumcraftClassic.LOGGER.warn("[SPINE-3] unreadable vanilla model models/{}.json", path, e);
                return sprites;
            }
            sprites += countSpriteKeys(vanilla);
            if (!vanilla.has("parent")) {
                return sprites;
            }
            final String next = vanilla.get("parent").getAsString();
            ref = next.contains(":") ? next : ResourceLocation.DEFAULT_NAMESPACE + ":" + next;
        }
        return sprites;
    }

    private static JsonElement loadJson(final String assetRelativePath) {
        try (InputStream in = ItemModelCoverageGameTest.class.getResourceAsStream(assetRelativePath)) {
            if (in == null) {
                throw new IllegalStateException(assetRelativePath + " is not on the classpath");
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String preview(final List<String> ids) {
        final int shown = Math.min(ids.size(), 25);
        return ids.subList(0, shown) + (ids.size() > shown ? " …(+" + (ids.size() - shown) + " more)" : "");
    }
}
