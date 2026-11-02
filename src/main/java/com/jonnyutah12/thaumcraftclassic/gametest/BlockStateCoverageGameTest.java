package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.ModCreativeTabs;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceDynamicState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * SPINE-4: every shipped block has a blockstate that covers its registered
 * state set, and every model that blockstate names resolves to geometry built
 * from published sprites.
 *
 * <h2>Why a probe, not a screenshot</h2>
 * SPINE-3 (t_15e43d12) shipped one item model per registered item and left the
 * block half open: {@code assets/thaumcraft/blockstates/} held a single file
 * ({@code blockcustomore.json}), so 51 registered blocks rendered the
 * missing-model cube with the item's flat icon. A block with no blockstate is
 * invisible to this card's contract in a way no unit test can see: the client
 * picks the model by {@code BlockState → ModelBlockLocation}, and that mapping
 * lives only in the shipped JSON. The expected set therefore comes from the
 * live registry ({@link BuiltInRegistries#BLOCK} through
 * {@link ModCreativeTabs#isShippedCatalogBlock}), never a hardcoded list, and
 * the state keys are enumerated from each block's own
 * {@code StateDefinition#getPossibleStates()} — a property the port renames
 * (the slabs' {@code type}, the stairs' {@code shape}, the door's
 * {@code half}/{@code open}) fails the probe instead of silently rendering one
 * shape.
 *
 * <h2>What the blockstates encode</h2>
 * Each face sprite is the one the 1.7.10 original resolved for that face: the
 * {@code registerBlockIcons} (SRG {@code func_149651_a}) atlas index that
 * {@code getIcon} (SRG {@code func_149691_a}) and the connected-texture hook
 * (SRG {@code func_149673_e}) return for the metadata — e.g. the arcane
 * pedestal's {@code pedestal_side} on the four horizontal faces and
 * {@code pedestal_top} on the vertical ones
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/blocks/BlockStoneDevice.java:66-68,107-124}).
 * The shapes follow the original's bounds hooks (SRG {@code func_149633_g} /
 * {@code func_149676_a}, where {@code BlockRenderer.Wn = n/16}): the candle's
 * 6..10 × 0..8 column, the jar's 3..13 body plus 5..11 neck, the loot crate's
 * three stacked boxes, the mirror's 1/16 panel. Where the original drew a meta
 * through a custom renderer (the device ISMRs, {@code TileCrystalRenderer},
 * {@code TileChestHungryRenderer}, {@code TileArcaneWorkbenchRenderer}) a
 * static model is an approximation; every approximation is listed in
 * {@code docs/BEHAVIORAL_DEVIATIONS.md} BD-17, not hidden.
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlockStateCoverageGameTest {

    private static final String ASSET_ROOT = "/assets/" + ThaumcraftClassic.MOD_ID + "/";
    private static final String BLOCKSTATE_DIR = ASSET_ROOT + "blockstates/";
    private static final String MODELS_BLOCK_DIR = ASSET_ROOT + "models/block/";

    /** Present in every real overlay (and the CI fixture overlay): see SPINE-3. */
    private static final String OVERLAY_PROBE_SPRITE = "textures/block/amberore.png";

    private static final boolean VANILLA_PROBE_PRESENT =
            BlockStateCoverageGameTest.class.getResource("/assets/minecraft/textures/block/dirt.png") != null;

    /**
     * The texture keys each vanilla block parent reads, from the 1.21.1 client
     * jar ({@code assets/minecraft/models/block/<name>.json}). A model that
     * parents one of these and omits a key renders that face with the missing
     * texture, which is exactly the defect class the 9 dead SPINE-3 models
     * showed.
     */
    private static final Map<String, List<String>> VANILLA_PARENT_KEYS = Map.of(
            "minecraft:block/cube", List.of("down", "up", "north", "south", "east", "west"),
            "minecraft:block/cube_all", List.of("all"),
            "minecraft:block/slab", List.of("bottom", "top", "side"),
            "minecraft:block/slab_top", List.of("bottom", "top", "side"),
            "minecraft:block/stairs", List.of("texture"),
            "minecraft:block/door_bottom_right", List.of("bottom"),
            "minecraft:block/door_top_right", List.of("top"),
            "minecraft:block/door_bottom_right_open", List.of("bottom"),
            "minecraft:block/door_top_right_open", List.of("top"),
            // vanilla air declares no geometry and reads no texture key — the model for the
            // metas 4.2.3.5 declines to draw (stone device 3 "air", BlockStoneDeviceRenderer:263)
            "minecraft:block/air", List.of());

    private BlockStateCoverageGameTest() {
    }

    // ------------------------------------------------------------------ probes

    /**
     * Every shipped block has {@code blockstates/<id path>.json}. Expected set:
     * the live registry, so a block registered by a later card is gated here
     * the moment it ships.
     */
    @GameTest(template = "empty")
    public static void everyShippedBlockHasABlockStateJson(final GameTestHelper helper) {
        final Map<String, Block> expected = shippedBlocks();
        helper.assertTrue(!expected.isEmpty(),
                "the registry-derived expected block set is empty — this probe can never fail");

        final List<String> missing = new ArrayList<>();
        for (final String path : expected.keySet()) {
            if (!classResourceExists("blockstates/" + path + ".json")) {
                missing.add(path);
            }
        }
        ThaumcraftClassic.LOGGER.info("[SPINE-4] blockstate coverage: {} of {} shipped blocks have a blockstate, {} missing",
                expected.size() - missing.size(), expected.size(), missing.size());
        helper.assertTrue(missing.isEmpty(),
                missing.size() + " of " + expected.size() + " registered thaumcraft blocks have no "
                        + BLOCKSTATE_DIR + "<id>.json — they render the missing-model cube in world: " + preview(missing));
        helper.succeed();
    }

    /**
     * The blockstate covers the state set the block actually registers, and
     * nothing else. A blockstate key is a <em>partial</em> match: it names some
     * properties and the rest are wildcards (that is how vanilla's own
     * {@code oak_stairs.json} covers 112 states with 40 keys, verified against
     * the 1.21.1 client jar). So the contract is: every key names properties the
     * block registers with legal values; every registered state is matched by at
     * least one key; no key matches zero states (a stale key left behind by a
     * property the port renamed, e.g. a slab's {@code half} for its
     * {@code type}); and no state is matched by two keys (the loader silently
     * keeps the first, which is how one model quietly shadows another).
     */
    @GameTest(template = "empty")
    public static void everyBlockStateCoversItsRegisteredStateSet(final GameTestHelper helper) {
        final List<String> problems = new ArrayList<>();
        int states = 0;
        for (final Map.Entry<String, Block> entry : shippedBlocks().entrySet()) {
            final StateDefinition<Block, BlockState> definition = entry.getValue().getStateDefinition();
            final JsonObject json = loadObject("blockstates/" + entry.getKey() + ".json");
            if (json == null) {
                problems.add(entry.getKey() + " (no blockstate file)");
                continue;
            }
            final JsonElement variantsElement = json.get("variants");
            if (variantsElement == null || !variantsElement.isJsonObject()) {
                problems.add(entry.getKey() + " (no \"variants\" object)");
                continue;
            }
            final List<String> keys = new ArrayList<>(variantsElement.getAsJsonObject().keySet());
            final List<String> local = new ArrayList<>();
            final Map<String, Integer> matchesPerKey = new TreeMap<>();
            for (final String key : keys) {
                for (final String pair : key.isEmpty() ? List.<String>of() : List.of(key.split(","))) {
                    final int eq = pair.indexOf('=');
                    final String name = eq < 0 ? pair : pair.substring(0, eq);
                    final String value = eq < 0 ? "" : pair.substring(eq + 1);
                    if (name.isEmpty()) {
                        local.add("empty property name in key '" + key + "'");
                        continue;
                    }
                    Property<?> found = null;
                    for (final Property<?> candidate : definition.getProperties()) {
                        if (candidate.getName().equals(name)) {
                            found = candidate;
                            break;
                        }
                    }
                    final Property<?> property = found;
                    if (property == null) {
                        local.add("key '" + key + "' names property '" + name + "' which "
                                + entry.getKey() + " does not register (properties: "
                                + definition.getProperties().stream().map(Property::getName).toList() + ")");
                        continue;
                    }
                    final List<String> domain = property.getPossibleValues().stream()
                            .map(v -> serialValue(property, v)).toList();
                    if (!domain.contains(value)) {
                        local.add("key '" + key + "' uses '" + name + "=" + value + "' outside the domain " + domain);
                    }
                }
            }
            final List<String> uncovered = new ArrayList<>();
            final List<String> ambiguous = new ArrayList<>();
            for (final BlockState state : definition.getPossibleStates()) {
                states++;
                final String serial = serialName(state, definition);
                final List<String> matched = new ArrayList<>();
                for (final String key : keys) {
                    if (keyMatches(key, state, definition)) {
                        matched.add(key);
                        matchesPerKey.merge(key, 1, Integer::sum);
                    }
                }
                if (matched.isEmpty()) {
                    uncovered.add(serial);
                } else if (matched.size() > 1) {
                    ambiguous.add(serial + " matched by " + matched);
                }
            }
            final List<String> dead = new ArrayList<>();
            for (final String key : keys) {
                if (!matchesPerKey.containsKey(key)) {
                    dead.add(key);
                }
            }
            if (!uncovered.isEmpty()) {
                local.add("registered states no key matches: " + preview(uncovered));
            }
            if (!dead.isEmpty()) {
                local.add("keys matching no registered state (stale): " + preview(dead));
            }
            if (!ambiguous.isEmpty()) {
                local.add("states matched by more than one key (the loader keeps the first): " + preview(ambiguous));
            }
            for (final String problem : local) {
                problems.add(entry.getKey() + " (" + problem + ")");
            }
        }
        ThaumcraftClassic.LOGGER.info("[SPINE-4] state-set coverage: {} blocks, {} registered states, {} blockstates mismatched",
                shippedBlocks().size(), states, problems.size());
        helper.assertTrue(problems.isEmpty(),
                problems.size() + " blockstates do not match the state set their block registers: " + preview(problems));
        helper.succeed();
    }

    /** True when every {@code prop=value} pair in a blockstate key agrees with the state. */
    private static boolean keyMatches(final String key, final BlockState state,
                                       final StateDefinition<Block, BlockState> definition) {
        if (key.isEmpty()) {
            // Vanilla's convention for a block with no properties (bedrock.json): the
            // empty key is the single state, so it matches by definition.
            return true;
        }
        for (final String pair : key.split(",")) {
            final int eq = pair.indexOf('=');
            if (eq <= 0) {
                return false;
            }
            final String name = pair.substring(0, eq);
            final String value = pair.substring(eq + 1);
            for (final Property<?> property : definition.getProperties()) {
                if (property.getName().equals(name)) {
                    if (!serialValue(property, state.getValue(property)).equals(value)) {
                        return false;
                    }
                    break;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String serialValue(final Property<T> property, final Comparable<?> value) {
        return property.getName((T) value);
    }

    /**
     * Every model a blockstate names parses, carries an explicit namespace,
     * resolves through its parent chain, ends in geometry (an {@code elements}
     * array of its own or a parent), and satisfies the texture keys that
     * parent's geometry reads.
     */
    @GameTest(template = "empty")
    public static void everyBlockStateModelChainResolves(final GameTestHelper helper) {
        final List<String> broken = new ArrayList<>();
        int models = 0;
        for (final String model : referencedModels()) {
            models++;
            final List<String> problems = new ArrayList<>();
            final Deque<String> chain = new ArrayDeque<>();
            chain.add(model);
            final Set<String> seen = new LinkedHashSet<>();
            boolean geometry = false;
            while (!chain.isEmpty()) {
                final String ref = chain.poll();
                if (!seen.add(ref)) {
                    problems.add("parent cycle at " + ref);
                    break;
                }
                if (!ref.contains(":")) {
                    problems.add("unnamespaced model id '" + ref + "' in a blockstate defaults to "
                            + ThaumcraftClassic.MOD_ID + ":; a vanilla model must say minecraft:");
                    continue;
                }
                final String namespace = ref.substring(0, ref.indexOf(':'));
                final String path = ref.substring(ref.indexOf(':') + 1);
                if (namespace.equals(ThaumcraftClassic.MOD_ID)) {
                    final JsonObject m = parseAsset("models/" + path + ".json");
                    if (m == null) {
                        problems.add("model file missing from the shipped tree: " + ref);
                        break;
                    }
                    if (m.has("elements") && m.getAsJsonArray("elements").size() > 0) {
                        geometry = true;
                    }
                    if (!m.has("parent")) {
                        if (!geometry) {
                            problems.add("model has no parent and no elements — it declares no geometry: " + ref);
                        }
                        break;
                    }
                    final String parent = m.get("parent").getAsString();
                    if (!parent.contains(":")) {
                        problems.add("unnamespaced parent '" + parent + "' in " + ref
                                + "; a vanilla parent must be written minecraft:block/…");
                        break;
                    }
                    final String parentNamespace = parent.substring(0, parent.indexOf(':'));
                    final String parentPath = parent.substring(parent.indexOf(':') + 1);
                    if (parentNamespace.equals(ThaumcraftClassic.MOD_ID)) {
                        if (!classResourceExists("models/" + parentPath + ".json")) {
                            problems.add("parent model missing from the shipped tree: " + parent + " (of " + ref + ")");
                            break;
                        }
                        chain.add(parent);
                    } else {
                        final List<String> keys = VANILLA_PARENT_KEYS.get(parent);
                        if (keys == null) {
                            problems.add("unknown vanilla parent '" + parent + "' in " + ref
                                    + " — add its texture keys to VANILLA_PARENT_KEYS");
                        } else {
                            final JsonObject textures = m.has("textures") ? m.getAsJsonObject("textures") : new JsonObject();
                            for (final String key : keys) {
                                if (!textures.has(key)) {
                                    problems.add(ref + " parents " + parent + " without the \"" + key
                                            + "\" texture key that parent's geometry reads");
                                }
                            }
                            for (final Map.Entry<String, JsonElement> t : textures.entrySet()) {
                                final String value = t.getValue().getAsString();
                                if (!value.startsWith("#") && !value.contains(":")) {
                                    problems.add(ref + " texture '" + t.getKey() + " = " + value
                                            + "' has no namespace — it would resolve to "
                                            + ThaumcraftClassic.MOD_ID + ":" + value);
                                }
                            }
                        }
                        geometry = true;
                        break;
                    }
                }
                // A minecraft: model id in a blockstate is a vanilla model: fine,
                // its geometry is vanilla's.
                geometry = true;
            }
            for (final String problem : problems) {
                broken.add(model + " (" + problem + ")");
            }
        }
        ThaumcraftClassic.LOGGER.info("[SPINE-4] model chains: {} referenced models walked, {} broken", models, broken.size());
        helper.assertTrue(broken.isEmpty(),
                broken.size() + " blockstate model chains do not resolve: " + preview(broken));
        helper.succeed();
    }

    /**
     * The overlay contract, gated only where an overlay is installed (D-004:
     * original textures are All Rights Reserved and never committed): every
     * {@code thaumcraft:} sprite in a reachable model chain must exist at the
     * 1.21.1 published path. This is what makes "the sprite is a published row
     * of docs/asset-mapping.csv, never a guess" a checked fact: a model that
     * names art the 4.2.3.5 overlay does not publish renders magenta.
     */
    @GameTest(template = "empty")
    public static void everyBlockStateSpriteResolvesWhenTheOverlayIsInstalled(final GameTestHelper helper) {
        if (!classResourceExists(OVERLAY_PROBE_SPRITE)) {
            ThaumcraftClassic.LOGGER.info("[SPINE-4] sprite resolution not gated: no asset overlay on this "
                    + "classpath ({} is absent) — D-004 ships original textures as a local overlay only",
                    OVERLAY_PROBE_SPRITE);
            helper.succeed();
            return;
        }
        final List<String> unresolved = new ArrayList<>();
        int refs = 0;
        for (final String model : referencedModels()) {
            final Deque<String> chain = new ArrayDeque<>();
            chain.add(model);
            final Set<String> seen = new LinkedHashSet<>();
            while (!chain.isEmpty()) {
                final String ref = chain.poll();
                if (!seen.add(ref) || !ref.contains(":")) {
                    continue;
                }
                final String namespace = ref.substring(0, ref.indexOf(':'));
                if (!namespace.equals(ThaumcraftClassic.MOD_ID)) {
                    continue;
                }
                final JsonObject m = parseAsset("models/" + ref.substring(ref.indexOf(':') + 1) + ".json");
                if (m == null) {
                    continue;
                }
                if (m.has("textures")) {
                    for (final Map.Entry<String, JsonElement> t : m.getAsJsonObject("textures").entrySet()) {
                        final String sprite = t.getValue().getAsString();
                        if (sprite.startsWith("#")) {
                            continue;
                        }
                        if (sprite.startsWith(ThaumcraftClassic.MOD_ID + ":")) {
                            refs++;
                            final String texturePath = "textures/" + sprite.substring(ThaumcraftClassic.MOD_ID.length() + 1) + ".png";
                            if (!classResourceExists(texturePath)) {
                                unresolved.add(ref + " [" + t.getKey() + " = " + sprite + "] -> " + texturePath);
                            }
                        } else if (sprite.startsWith("minecraft:")) {
                            refs++;
                            final String vanillaPath = "assets/minecraft/textures/" + sprite.substring("minecraft:".length()) + ".png";
                            if (VANILLA_PROBE_PRESENT
                                    && BlockStateCoverageGameTest.class.getResource("/" + vanillaPath) == null) {
                                unresolved.add(ref + " [" + t.getKey() + " = " + sprite + "] -> "
                                        + vanillaPath + " (not in the vanilla jar)");
                            }
                        }
                    }
                }
                if (m.has("parent") && m.get("parent").getAsString().startsWith(ThaumcraftClassic.MOD_ID + ":")) {
                    chain.add(m.get("parent").getAsString());
                }
            }
        }
        ThaumcraftClassic.LOGGER.info("[SPINE-4] overlay sprite resolution: {} thaumcraft sprite references, {} unresolved",
                refs, unresolved.size());
        helper.assertTrue(unresolved.isEmpty(),
                unresolved.size() + " block model sprites are not published at the 1.21.1 path by the "
                        + "installed overlay: " + preview(unresolved));
        helper.succeed();
    }

    /**
     * The converse of probe 1 for models: every committed
     * {@code models/block/*.json} is named by a shipped blockstate chain. A
     * block model no blockstate reaches is a dead asset — the SPINE-3 first
     * attempt shipped 9 models that named files which did not exist; this probe
     * is the same audit pointed the other way, over the shipped listing (an
     * empty listing is asserted, not skipped).
     */
    @GameTest(template = "empty")
    public static void everyCommittedBlockModelIsReferenced(final GameTestHelper helper) {
        final Set<String> committed = committedBlockModelIds();
        helper.assertTrue(!committed.isEmpty(),
                "no block models found on the classpath under " + MODELS_BLOCK_DIR
                        + " — this probe can never fail; the asset tree is not on the runtime classpath");
        final Set<String> referenced = referencedModels();
        final List<String> dead = new ArrayList<>(committed);
        dead.removeAll(referenced);
        ThaumcraftClassic.LOGGER.info("[SPINE-4] {} committed block models, {} referenced by a shipped blockstate, {} dead",
                committed.size(), referenced.size(), dead.size());
        helper.assertTrue(dead.isEmpty(),
                dead.size() + " committed block models are not reachable from any shipped blockstate: " + preview(dead));
        helper.succeed();
    }

    /**
     * BD-18 (t_065502ba): the appearance the tile drives ships as
     * <b>states</b>, not as a renderer.
     *
     * <p>A block that publishes a dynamic look through
     * {@link DeviceDynamicState} must satisfy four things, all of them
     * checkable from the shipped JSON, none of them visible to a screenshot of
     * one static block:</p>
     * <ol>
     *   <li>the block registers the property the tile writes;</li>
     *   <li>every blockstate key <em>addresses</em> that property — a wildcard
     *     key pins one model for both values, which is how a lit swap silently
     *     stops changing the look (the mechanism 1.7.10 did not have to worry
     *     about: {@code BlockMetalDevice.java:117-155} and
     *     {@code BlockLifterRenderer.java:50-89} picked the sprite per frame,
     *     not per state);</li>
     *   <li>for at least one state group the two values name <b>different</b>
     *     models — a property no model depends on is decoration. A group whose
     *     two values name the same model is allowed: the plain arcane lamp
     *     (metal device 7) always drew its lit {@code lamp_top}/{@code
     *     lamp_side} pair, so its look really is static;</li>
     *   <li>those two models name <b>different sprites</b>: two models with the
     *     same textures would swap geometry and leave the face alone, which is
     *     exactly the failure this card exists to prevent.</li>
     * </ol>
     *
     * <p>The expected set comes from the live registry (the shipped blocks that
     * implement the interface), so a later card that publishes a new dynamic
     * look is gated here without editing this probe.</p>
     */
    @GameTest(template = "empty")
    public static void dynamicAppearanceShipsAsDistinctStateModels(final GameTestHelper helper) {
        final List<String> sources = new ArrayList<>();
        final List<String> problems = new ArrayList<>();
        for (final Map.Entry<String, Block> entry : shippedBlocks().entrySet()) {
            if (!(entry.getValue() instanceof DeviceDynamicState device)) {
                continue;
            }
            final Property<?> property = device.dynamicAppearanceProperty();
            sources.add(entry.getKey() + ":" + property.getName());
            final StateDefinition<Block, BlockState> definition = entry.getValue().getStateDefinition();
            if (!definition.getProperties().contains(property)) {
                problems.add(entry.getKey() + " publishes " + property.getName()
                        + " through the interface but its StateDefinition does not register it");
                continue;
            }
            final JsonObject json = loadObject("blockstates/" + entry.getKey() + ".json");
            if (json == null || !json.has("variants") || !json.get("variants").isJsonObject()) {
                problems.add(entry.getKey() + " has no blockstate variants to key the dynamic property on");
                continue;
            }
            final List<String> keys = new ArrayList<>(json.getAsJsonObject("variants").keySet());
            for (final String key : keys) {
                if (!keyAddressesProperty(key, property)) {
                    problems.add(entry.getKey() + " blockstate key '" + key + "' leaves " + property.getName()
                            + " a wildcard — one model would be pinned for every value the tile publishes");
                }
            }
            // state group (every property except the dynamic one) -> value -> model
            final Map<String, Map<String, String>> models = new TreeMap<>();
            for (final BlockState state : definition.getPossibleStates()) {
                final String group = groupKey(state, definition, property);
                final String value = serialValue(property, state.getValue(property));
                String model = null;
                for (final String key : keys) {
                    if (keyMatches(key, state, definition)) {
                        model = modelOf(json.getAsJsonObject("variants").getAsJsonObject(key));
                    }
                }
                if (model == null) {
                    problems.add(entry.getKey() + " state " + serialName(state, definition)
                            + " is matched by no key, so the dynamic property has no model to pick");
                    continue;
                }
                models.computeIfAbsent(group, k -> new TreeMap<>()).put(value, model);
            }
            int swapping = 0;
            for (final Map.Entry<String, Map<String, String>> group : models.entrySet()) {
                final List<String> groupModels = new ArrayList<>(new LinkedHashSet<>(group.getValue().values()));
                if (groupModels.size() < 2) {
                    continue;
                }
                swapping++;
                final Map<String, Set<String>> byModel = new LinkedHashMap<>();
                for (final String model : groupModels) {
                    byModel.put(model, spritesOfModel(model));
                }
                final List<String> spriteSets = new ArrayList<>();
                for (final Set<String> sprites : byModel.values()) {
                    spriteSets.add(String.join(",", sprites));
                }
                if (new LinkedHashSet<>(spriteSets).size() < 2) {
                    problems.add(entry.getKey() + " group " + group.getKey() + " swaps the model between "
                            + property.getName() + "'s values but every model names the same sprites ("
                            + groupModels + ") — the state change would not change the appearance");
                    continue;
                }
                // The swap has to name the original's LIT sprites, not just any
                // different ones. The uncharged model carries the `_off` suffix
                // the original uses for that face (BlockMetalDevice.java:124-136
                // answers icon[15]/icon[14] = lamp_grow_top_off/lamp_grow_side_off
                // for an uncharged growth lamp, and :125-131 answers
                // icon[13]/icon[12] = lamp_grow_top/lamp_grow_side once the tile's
                // charges > 0), so the expectation is derived from the shipped off
                // model here rather than from a sprite list.
                final Set<String> litSprites = new LinkedHashSet<>();
                final List<Set<String>> offSprites = new ArrayList<>();
                for (final Set<String> sprites : byModel.values()) {
                    final Set<String> off = new LinkedHashSet<>();
                    for (final String sprite : sprites) {
                        if (sprite.endsWith("_off")) {
                            off.add(sprite);
                        }
                    }
                    if (off.isEmpty()) {
                        litSprites.addAll(sprites);
                    } else {
                        offSprites.add(off);
                    }
                }
                for (final Set<String> off : offSprites) {
                    for (final String sprite : off) {
                        final String lit = sprite.substring(0, sprite.length() - "_off".length());
                        if (!litSprites.contains(lit)) {
                            problems.add(entry.getKey() + " group " + group.getKey() + " swaps to a model that"
                                    + " never names " + lit + " — the charged state must draw the lit sprite the"
                                    + " original picks for the face " + sprite + " covers");
                        }
                    }
                }
            }
            if (swapping == 0) {
                problems.add(entry.getKey() + " publishes " + property.getName()
                        + " on " + models.size() + " state group(s) and none of them names a model that "
                        + "differs between the values — the property changes nothing the player can see");
            }
        }
        ThaumcraftClassic.LOGGER.info("[BD-18] dynamic appearance: {} tile-driven property source(s) — {}",
                sources.size(), sources);
        helper.assertTrue(!sources.isEmpty(),
                "no shipped block publishes a dynamic appearance property — the lit/charged swaps of "
                        + "BlockMetalDevice.java:117-155 and BlockLifterRenderer.java:50-89 are "
                        + "unrepresented, and this probe can never fail");
        helper.assertTrue(problems.isEmpty(),
                problems.size() + " dynamic-appearance contracts are broken: " + preview(problems));
        helper.succeed();
    }

    /** True when a blockstate key names the property (no wildcard on it). */
    private static boolean keyAddressesProperty(final String key, final Property<?> property) {
        if (key.isEmpty()) {
            return false;
        }
        for (final String pair : key.split(",")) {
            if (pair.startsWith(property.getName() + "=")) {
                return true;
            }
        }
        return false;
    }

    /** The state's serial name with the dynamic property omitted — its state group. */
    private static String groupKey(final BlockState state, final StateDefinition<Block, BlockState> definition,
                                   final Property<?> dynamic) {
        final StringBuilder sb = new StringBuilder();
        for (final Property<?> property : definition.getProperties()) {
            if (property.equals(dynamic)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(property.getName()).append('=').append(serialValue(property, state.getValue(property)));
        }
        return sb.toString();
    }

    /** The model a blockstate key names (a weighted list's first entry). */
    private static String modelOf(final JsonObject entry) {
        if (entry.has("model")) {
            return entry.get("model").getAsString();
        }
        if (entry.has("variants") && entry.getAsJsonArray("variants").size() > 0) {
            return entry.getAsJsonArray("variants").get(0).getAsJsonObject().get("model").getAsString();
        }
        return "";
    }

    /**
     * Every sprite reachable from a model: the literal texture values across its
     * parent chain (a {@code #key} entry is a local alias, resolved by the same
     * model's {@code textures} object, so collecting the literals is the sprite
     * set the faces can draw).
     */
    private static Set<String> spritesOfModel(final String modelRef) {
        final Set<String> sprites = new LinkedHashSet<>();
        final Deque<String> chain = new ArrayDeque<>();
        final Set<String> seen = new LinkedHashSet<>();
        chain.add(modelRef);
        while (!chain.isEmpty()) {
            final String ref = chain.poll();
            if (!seen.add(ref) || !ref.contains(":")) {
                continue;
            }
            final String namespace = ref.substring(0, ref.indexOf(':'));
            if (!namespace.equals(ThaumcraftClassic.MOD_ID)) {
                continue;
            }
            final JsonObject model = parseAsset("models/" + ref.substring(ref.indexOf(':') + 1) + ".json");
            if (model == null) {
                continue;
            }
            if (model.has("textures")) {
                for (final Map.Entry<String, JsonElement> texture : model.getAsJsonObject("textures").entrySet()) {
                    final String value = texture.getValue().getAsString();
                    if (!value.startsWith("#") && value.contains(":")) {
                        sprites.add(value);
                    }
                }
            }
            if (model.has("parent") && model.get("parent").getAsString().startsWith(ThaumcraftClassic.MOD_ID + ":")) {
                chain.add(model.get("parent").getAsString());
            }
        }
        return sprites;
    }

    // ------------------------------------------------------------------ helpers

    /** Registered shipped blocks, keyed by registry id path. */
    private static Map<String, Block> shippedBlocks() {
        final Map<String, Block> ids = new TreeMap<>();
        for (final Block block : BuiltInRegistries.BLOCK) {
            final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (ModCreativeTabs.isShippedCatalogBlock(block)) {
                ids.put(id.getPath(), block);
            }
        }
        return ids;
    }

    /** The key a blockstate JSON must use for this state, from the block itself. */
    private static String serialName(final BlockState state, final StateDefinition<Block, BlockState> definition) {
        final StringBuilder key = new StringBuilder();
        for (final Property<?> property : definition.getProperties()) {
            if (key.length() > 0) {
                key.append(',');
            }
            key.append(property.getName()).append('=').append(serialValue(property, state.getValue(property)));
        }
        return key.toString();
    }

    /** Every model id any shipped blockstate names. */
    private static Set<String> referencedModels() {
        final Set<String> models = new LinkedHashSet<>();
        for (final String path : shippedBlocks().keySet()) {
            final JsonObject json = loadObject("blockstates/" + path + ".json");
            if (json == null || json.get("variants") == null) {
                continue;
            }
            for (final Map.Entry<String, JsonElement> variant : json.getAsJsonObject("variants").entrySet()) {
                final JsonElement modelElement = variant.getValue().getAsJsonObject().get("model");
                if (modelElement != null) {
                    models.add(modelElement.getAsString());
                }
            }
        }
        return models;
    }

    /** Every {@code thaumcraft:block/<name>} model file committed in the tree. */
    private static Set<String> committedBlockModelIds() {
        final java.net.URL dir = BlockStateCoverageGameTest.class.getResource(MODELS_BLOCK_DIR);
        if (dir == null) {
            return Set.of(); // probe 5 asserts a non-empty listing, so this cannot pass silently
        }
        final Path root;
        try {
            root = Path.of(dir.toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException("the shipped asset tree is not addressable on the classpath: "
                    + MODELS_BLOCK_DIR, e);
        }
        if (!Files.isDirectory(root)) {
            return Set.of(); // packaged in a jar: the resource-path gates above are the coverage
        }
        try (Stream<Path> walk = Files.walk(root)) {
            final Set<String> ids = new LinkedHashSet<>();
            for (final Path p : walk.filter(f -> f.getFileName().toString().endsWith(".json")).toList()) {
                ids.add(ThaumcraftClassic.MOD_ID + ":block/" + root.relativize(p).toString()
                        .replace('\\', '/').replaceAll("\\.json$", ""));
            }
            return ids;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean classResourceExists(final String relativePath) {
        return BlockStateCoverageGameTest.class.getResource(ASSET_ROOT + relativePath) != null;
    }

    private static JsonObject parseAsset(final String assetRelativePath) {
        try (InputStream in = BlockStateCoverageGameTest.class.getResourceAsStream(ASSET_ROOT + assetRelativePath)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            ThaumcraftClassic.LOGGER.warn("[SPINE-4] unreadable asset {}", assetRelativePath, e);
            return null;
        }
    }

    private static JsonObject loadObject(final String assetRelativePath) {
        return parseAsset(assetRelativePath);
    }

    private static String preview(final List<String> items) {
        final int shown = Math.min(items.size(), 25);
        return items.subList(0, shown) + (items.size() > shown ? " …(+" + (items.size() - shown) + " more)" : "");
    }
}
