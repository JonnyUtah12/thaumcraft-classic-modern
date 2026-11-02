package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems;
import com.jonnyutah12.thaumcraftclassic.items.ItemMetaAppearance;
import com.jonnyutah12.thaumcraftclassic.items.ItemMetaTints;
import com.jonnyutah12.thaumcraftclassic.registration.ModCreativeTabs;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * BD-17 (t_f04c44fb) — per-metadata item appearance, and the four items whose
 * original draw was a custom renderer.
 *
 * <p>The defect: one registered id + one {@code models/item/<id>.json} covers a
 * whole metadata family, so two metas of a family are the same picture in
 * inventory, hand and creative search, and four items whose original draw was
 * a {@code ISpecialRenderer} use flat art. The fix is data — the per-meta
 * models under {@code models/itemmeta/} plus the {@code overrides} arrays the
 * {@code scripts/generate_itemmeta_models.py} generator derives from
 * {@link ItemMetaAppearance} — and a client colour handler
 * ({@link com.jonnyutah12.thaumcraftclassic.client.ItemMetaClient}).</p>
 *
 * <h2>Registry-derived expected sets, everywhere</h2>
 * The gates never hardcode a family list. The accountable set is the live item
 * registry walked through {@link ModCreativeTabs#isShippedCatalogItem}, cut by
 * {@link ItemMetaAppearance}'s table, which is itself cross-checked against
 * the lang file (probe 3: an id with per-meta labels must have per-meta art)
 * and against the shipped model directory (probe 6: art with no table row is a
 * dead model). A later catalog card that registers a meta-carrying family
 * without art fails this gate the moment it registers; a table row for an id
 * that is not shipped content fails it too.</p>
 *
 * <h2>What the gates can and cannot see</h2>
 * A game test runs on a dedicated server: it sees files, the registry and
 * components, not pixels. So these probes gate the model chain (a file per
 * meta, a wired override per meta, sprites the overlay publishes), the
 * component contract (every meta-carrying path sets the appearance mirror the
 * overrides key on, and the creative tab lists one entry per meta) and the
 * table's cross-links. The colour maths is asserted by the unit-level
 * {@code ItemMetaTintTest} (server-free, no client class), and the actual
 * painted pixel is a client fact no headless gate can reach — recorded as
 * BD-17's residual, same as the thaumometer's live dial.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ItemMetaAppearanceGameTest {

    private static final String ASSET_ROOT = "/assets/" + ThaumcraftClassic.MOD_ID + "/";
    private static final String MODELS_METAS_DIR = ASSET_ROOT + "models/itemmeta/";
    private static final String PREDICATE = "custom_model_data";

    /** A sprite the overlay always publishes — the overlay-presence probe (D-004). */
    private static final String OVERLAY_PROBE_SPRITE = "textures/block/amberore.png";

    private ItemMetaAppearanceGameTest() {
    }

    // ------------------------------------------------------------------ probes

    /**
     * Probe 1: every metadata the original drew differently has a model file,
     * and that model parses, declares an explicit-namespace parent, and names
     * at least one sprite.
     */
    @GameTest(template = "empty")
    public static void everyMetaHasAModel(final GameTestHelper helper) {
        final List<String> broken = new ArrayList<>();
        int checked = 0;
        for (final Map.Entry<String, List<ItemMetaAppearance.MetaArt>> family : familiesOfShippedContent().entrySet()) {
            for (final ItemMetaAppearance.MetaArt art : family.getValue()) {
                checked++;
                final String modelPath = "itemmeta/" + family.getKey() + "_" + art.meta();
                final JsonObject model = parseModel(modelPath);
                if (model == null) {
                    broken.add(family.getKey() + " meta " + art.meta() + " (no models/" + modelPath + ".json)");
                    continue;
                }
                final String parent = model.has("parent") ? model.get("parent").getAsString() : null;
                if (parent == null) {
                    broken.add(modelPath + " (no parent — a bare model declares no geometry)");
                } else if (!parent.contains(":")) {
                    broken.add(modelPath + " (unnamespaced parent '" + parent + "')");
                } else if (parent.startsWith(ThaumcraftClassic.MOD_ID + ":")
                        && !modelExists(parent.substring(ThaumcraftClassic.MOD_ID.length() + 1))) {
                    broken.add(modelPath + " (parent model missing: " + parent + ")");
                }
                if (art.sprites().size() != declaredSprites(model)) {
                    broken.add(modelPath + " (declares " + declaredSprites(model)
                            + " sprite(s), the table names " + art.sprites().size() + ")");
                }
                final List<String> sprites = modelSprites(model);
                for (final String sprite : art.sprites()) {
                    if (!sprites.contains(sprite)) {
                        broken.add(modelPath + " (does not name the table's sprite " + sprite + ")");
                    }
                }
            }
        }
        ThaumcraftClassic.LOGGER.info("[BD-17] per-meta models: {} metas required by the table, {} broken",
                checked, broken.size());
        helper.assertTrue(checked > 0, "the per-meta table names nothing — this probe can never fail");
        helper.assertTrue(broken.isEmpty(),
                broken.size() + " per-meta models are missing or wrong: " + preview(broken));
        helper.succeed();
    }

    /**
     * Probe 2: the per-meta models are actually reachable — the family's base
     * item model carries an {@code overrides} entry per meta, keyed on the
     * int component 1.21.1 can read ({@code custom_model_data}), ascending,
     * naming the table's model path. A model nobody selects is not a fix.
     */
    @GameTest(template = "empty")
    public static void everyMetaOverrideIsWiredInTheItemModel(final GameTestHelper helper) {
        final List<String> broken = new ArrayList<>();
        for (final Map.Entry<String, List<ItemMetaAppearance.MetaArt>> family : familiesOfShippedContent().entrySet()) {
            final String idPath = family.getKey();
            final JsonObject base = parseModel("item/" + idPath);
            if (base == null) {
                broken.add(idPath + " (no base item model to carry the overrides)");
                continue;
            }
            if (!base.has("overrides")) {
                broken.add(idPath + " (no overrides array — every meta renders the base model)");
                continue;
            }
            final JsonArray overrides = base.getAsJsonArray("overrides");
            final List<String> seen = new ArrayList<>();
            int lastValue = Integer.MIN_VALUE;
            for (final JsonElement entry : overrides) {
                final JsonObject o = entry.getAsJsonObject();
                final String predicate = o.has("predicate") ? o.get("predicate").getAsString() : "";
                final int value = o.has("value") ? o.get("value").getAsInt() : -1;
                final String model = o.has("model") ? o.get("model").getAsString() : "";
                seen.add(predicate + "=" + value + "->" + model);
                if (!PREDICATE.equals(predicate)) {
                    broken.add(idPath + " (predicate '" + predicate + "' — 1.21.1's ItemProperties "
                            + "drops an unregistered predicate name, so it can never match)");
                }
                if (value < lastValue) {
                    broken.add(idPath + " (override values must ascend for the int match to pick the "
                            + "closest meta; " + value + " follows " + lastValue + ")");
                }
                lastValue = value;
                if (!model.startsWith(ThaumcraftClassic.MOD_ID + ":itemmeta/" + idPath + "_")) {
                    broken.add(idPath + " (override " + value + " points at '" + model + "')");
                } else if (!modelExists(model.substring(ThaumcraftClassic.MOD_ID.length() + 1))) {
                    broken.add(idPath + " (override " + value + " names a model that is not shipped: " + model + ")");
                }
            }
            for (final ItemMetaAppearance.MetaArt art : family.getValue()) {
                final String want = PREDICATE + "=" + art.meta() + "->"
                        + ThaumcraftClassic.MOD_ID + ":itemmeta/" + idPath + "_" + art.meta();
                if (!seen.contains(want)) {
                    broken.add(idPath + " (meta " + art.meta() + " has no override entry: " + want + ")");
                }
            }
        }
        ThaumcraftClassic.LOGGER.info("[BD-17] override wiring: {} families checked, {} broken",
                familiesOfShippedContent().size(), broken.size());
        helper.assertTrue(broken.isEmpty(),
                broken.size() + " families are not wired to their per-meta models: " + preview(broken));
        helper.succeed();
    }

    /**
     * Probe 3: the table and the shipped content agree in both directions.
     * Every family/custom-shape id is registered shipped content (no dead
     * table row, no fixture id); and every shipped item whose lang file names
     * its metas ({@code item.thaumcraft.<id>.<meta>}) — the label shape the
     * original derives from metadata — is in the table, so a family that
     * promises per-meta identity in its names cannot ship one picture. The
     * colour table is checked the same way: an id it colours must be shipped.
     */
    @GameTest(template = "empty")
    public static void theTablesMatchTheShippedContent(final GameTestHelper helper) {
        final Map<String, ResourceLocation> shipped = shippedItemIds();
        final List<String> problems = new ArrayList<>();

        for (final String id : ItemMetaAppearance.familyIds()) {
            if (!shipped.containsKey(id)) {
                problems.add("the per-meta table names '" + id + "', which is not shipped registered content");
            }
        }
        for (final String id : ItemMetaAppearance.customShapeIds()) {
            if (!shipped.containsKey(id)) {
                problems.add("the custom-renderer table names '" + id + "', which is not shipped registered content");
            }
        }
        for (final String id : ItemMetaTints.tintedIds()) {
            if (!shipped.containsKey(id)) {
                problems.add("the tint table colours '" + id + "', which is not shipped registered content");
            }
        }

        final JsonObject lang = loadJson(ASSET_ROOT + "lang/en_us.json").getAsJsonObject();
        final List<String> unmodelled = new ArrayList<>();
        for (final String path : shipped.keySet()) {
            final List<String> keys = metaLabelKeys(lang, path);
            final boolean hasFamily = ItemMetaAppearance.hasFamily(path);
            final boolean excluded = ItemMetaAppearance.noPerMetaAppearanceIds().contains(path);
            if (!keys.isEmpty() && !hasFamily && !excluded) {
                unmodelled.add(path + " (labels " + keys + " name metas, but the family table has no row and "
                        + "the measured-exclusion table has no reason)");
            }
            if (hasFamily && excluded) {
                unmodelled.add(path + " is in the family table and the measured-exclusion table at once "
                        + "(the original draws one sprite per meta, or it does not)");
            }
        }
        // Measured layer coverage. A family row whose original draws two layers
        // (the jar label: label under label_over) needs a model declaring two
        // layers, and an id whose original tints ONLY the overlay pass (the
        // phial's aspect, the label's band — ItemMetaTints.overlayLayerTintIds)
        // needs at least one two-layer model, or the tint has nothing to paint.
        final List<String> thin = new ArrayList<>();
        for (final Map.Entry<String, List<ItemMetaAppearance.MetaArt>> family : familiesOfShippedContent().entrySet()) {
            for (final ItemMetaAppearance.MetaArt art : family.getValue()) {
                if (art.sprites().size() < 2) {
                    continue;
                }
                final int layers = layersOfMetaModel(family.getKey(), art.meta());
                if (layers < art.sprites().size()) {
                    thin.add(family.getKey() + " meta " + art.meta() + " draws " + art.sprites().size()
                            + " layers in 4.2.3.5; its model declares " + layers);
                }
            }
        }
        for (final String idPath : ItemMetaTints.overlayLayerTintIds()) {
            int best = 0;
            final JsonObject base = parseModel("item/" + idPath);
            if (base != null) {
                best = Math.max(best, layerCount(base));
            }
            for (final int meta : ItemMetaAppearance.metasOf(idPath)) {
                best = Math.max(best, layersOfMetaModel(idPath, meta));
            }
            if (best < 2) {
                thin.add(idPath + " (its original tints only the overlay pass; no model of it declares "
                        + "2 layers — the tint has no layer to colour)");
            }
        }
        ThaumcraftClassic.LOGGER.info("[BD-17] layer coverage: {} thin models", thin.size());
        problems.addAll(thin);
        ThaumcraftClassic.LOGGER.info("[BD-17] table/registry cross-check: {} families, {} custom shapes, "
                        + "{} tinted ids, {} ids with per-meta labels and no art",
                ItemMetaAppearance.familyIds().size(), ItemMetaAppearance.customShapeIds().size(),
                ItemMetaTints.tintedIds().size(), unmodelled.size());
        problems.addAll(unmodelled);
        helper.assertTrue(problems.isEmpty(),
                problems.size() + " table/registry disagreements: " + preview(problems));
        helper.succeed();
    }

    /**
     * Probe 4: every path that records a metadata also sets the appearance
     * component the model overrides key on. This is the defect class the
     * mechanism creates — a stack whose label says one meta and whose sprite
     * says another — so it is gated on the factories themselves, walked from
     * the table (every shipped family, every meta it names).
     */
    @GameTest(template = "empty")
    public static void everyMetaCarryingPathSetsTheAppearanceMirror(final GameTestHelper helper) {
        final List<String> broken = new ArrayList<>();
        int checked = 0;
        for (final Map.Entry<String, List<ItemMetaAppearance.MetaArt>> family : familiesOfShippedContent().entrySet()) {
            final Item item = BuiltInRegistries.ITEM.get(family.getValue().isEmpty() ? null : keyOf(family.getKey()));
            for (final ItemMetaAppearance.MetaArt art : family.getValue()) {
                checked++;
                final ItemStack stack = stackFor(family.getKey(), item, art.meta(), broken);
                if (stack == null) {
                    continue;
                }
                final int stored = stack.getOrDefault(ModDataComponents.ITEM_META.get(), -1);
                final int mirror = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA,
                        CustomModelData.DEFAULT).value();
                if (stored != art.meta()) {
                    broken.add(family.getKey() + " meta " + art.meta()
                            + ": thaumcraft:item_meta = " + stored);
                }
                if (mirror != art.meta()) {
                    broken.add(family.getKey() + " meta " + art.meta()
                            + ": minecraft:custom_model_data mirror = " + mirror
                            + " (the model overrides key on it, so the sprite would be the fallback)");
                }
            }
        }
        ThaumcraftClassic.LOGGER.info("[BD-17] appearance mirror: {} stacks built by the factories, {} broken",
                checked, broken.size());
        helper.assertTrue(checked > 0, "the table names no metas to build stacks for — probe cannot fail");
        helper.assertTrue(broken.isEmpty(),
                broken.size() + " meta-carrying stacks do not carry the appearance mirror: " + preview(broken));
        helper.succeed();
    }

    /**
     * Probe 5: creative shows exactly the metas the original's tab listed for
     * each family — one entry per metadata, each labelled by its meta, carrying
     * both the authoritative meta and the mirror — and no entry for a meta it
     * did not list (an unlabelled variant in creative is a hidden-content leak).
     */
    @GameTest(template = "empty")
    public static void creativeTabCarriesEveryFamilyMeta(final GameTestHelper helper) {
        final java.util.Collection<ItemStack> entries = ModCreativeTabs.TAB_THAUMCRAFT.get().getDisplayItems();
        final List<String> missing = new ArrayList<>();
        final List<String> extras = new ArrayList<>();
        final List<String> mismatched = new ArrayList<>();
        int expected = 0;
        for (final String idPath : familiesOfShippedContent().keySet()) {
            final Item item = BuiltInRegistries.ITEM.get(keyOf(idPath));
            final java.util.Set<Integer> want = new java.util.TreeSet<>();
            for (final int meta : ItemMetaAppearance.tabMetasOf(item)) {
                want.add(meta);
            }
            final java.util.Set<Integer> have = new java.util.TreeSet<>();
            boolean plainEntry = false;
            for (final ItemStack entry : entries) {
                if (!BuiltInRegistries.ITEM.getKey(entry.getItem()).equals(keyOf(idPath))) {
                    continue;
                }
                final int meta = entry.getOrDefault(ModDataComponents.ITEM_META.get(), -1);
                if (meta < 0) {
                    plainEntry = true; // the id's one un-meta'd entry (no per-meta labels)
                    continue;
                }
                have.add(meta);
                if (ItemMetaAppearance.appearanceValue(entry) != meta) {
                    mismatched.add(idPath + " entry meta " + meta + " mirrors "
                            + ItemMetaAppearance.appearanceValue(entry));
                }
            }
            expected += want.isEmpty() ? 1 : want.size();
            if (want.isEmpty()) {
                // A family whose shipped lang names no metas (a crop's growth
                // stages, the compass stone): the original's tab listed one
                // entry, so the port lists one entry — the un-meta'd one.
                if (!plainEntry) {
                    missing.add(idPath + " (no plain entry)");
                }
                for (final int meta : have) {
                    extras.add(idPath + " meta " + meta + " (the lang gives this id no per-meta label, "
                            + "so its tab entry is the un-meta'd one)");
                }
            } else {
                for (final int meta : want) {
                    if (!have.contains(meta)) {
                        missing.add(idPath + " meta " + meta);
                    }
                }
                for (final int meta : have) {
                    if (!want.contains(meta)) {
                        extras.add(idPath + " meta " + meta);
                    }
                }
                if (plainEntry) {
                    extras.add(idPath + " (an un-meta'd entry beside its per-meta entries)");
                }
            }
        }
        ThaumcraftClassic.LOGGER.info("[BD-17] creative tab: {} entries, {} family metas expected, "
                        + "{} missing, {} beyond the table+lang set, {} with a mismatched mirror",
                entries.size(), expected, missing.size(), extras.size(), mismatched.size());
        helper.assertTrue(expected > 0, "no family metas to look for — probe cannot fail");
        helper.assertTrue(missing.isEmpty(),
                missing.size() + " family metas are absent from the creative tab (with meta + mirror): "
                        + preview(missing));
        helper.assertTrue(extras.isEmpty(),
                extras.size() + " creative entries carry a meta no table row and no lang label cover: "
                        + preview(extras));
        helper.assertTrue(mismatched.isEmpty(),
                mismatched.size() + " creative entries label one meta and mirror another: "
                        + preview(mismatched));
        helper.succeed();
    }

    /**
     * Probe 6: the shipped per-meta model directory is exactly what the table
     * produces. A model for a family/meta the table dropped is a dead asset
     * (and the generator's own {@code --check} gate is the in-CI twin of this
     * probe, asserted by {@code ItemMetaModelSyncTest}).
     */
    @GameTest(template = "empty")
    public static void noDeadMetaModels(final GameTestHelper helper) {
        final List<String> committed = committedMetaModelNames();
        helper.assertTrue(!committed.isEmpty(),
                "no models found under " + MODELS_METAS_DIR + " — this probe can never fail; the asset "
                        + "tree is not addressable on the runtime classpath");
        final List<String> dead = new ArrayList<>();
        for (final String name : committed) {
            final int split = name.lastIndexOf('_');
            final String idPath = name.substring(0, split);
            final int meta;
            try {
                meta = Integer.parseInt(name.substring(split + 1));
            } catch (NumberFormatException notAMeta) {
                dead.add(name + " (name is not <id>_<meta>)");
                continue;
            }
            boolean known = false;
            for (final ItemMetaAppearance.MetaArt art : ItemMetaAppearance.family(idPath)) {
                known |= art.meta() == meta;
            }
            if (!known) {
                dead.add(name + " (the table has no row for it)");
            }
        }
        ThaumcraftClassic.LOGGER.info("[BD-17] per-meta model directory: {} committed, {} dead",
                committed.size(), dead.size());
        helper.assertTrue(dead.isEmpty(),
                dead.size() + " models under models/itemmeta are not produced by the table: " + preview(dead));
        helper.succeed();
    }

    /**
     * Probe 7: the four items the card calls custom-renderer items have a
     * replacement that resolves and names the sprite the original actually
     * drew for that item. Measured: only {@code trunkspawner} is drawn by an
     * item-side renderer (a {@code ModelChest} over {@code models/trunk.png}),
     * so it is the one id that gets a box-element shape; the jar's item draw is
     * the jar-bottom face, the arcane workbench's is BlockTable's woodplain
     * face (its worktable shape is the tile renderer's, with the blockstates),
     * and the thaumometer's live dial has no model equivalent, so it keeps the
     * vanilla compass chain SPINE-3 shipped. Every residual is in the BD-17 row.
     */
    @GameTest(template = "empty")
    public static void theCustomRendererItemsResolve(final GameTestHelper helper) {
        final Map<String, String> expectedSprite = new TreeMap<>();
        expectedSprite.put("trunkspawner", "thaumcraft:models/trunk");
        expectedSprite.put("blockjarfilleditem", "thaumcraft:block/jar_bottom");
        expectedSprite.put("tilearcaneworkbench", "thaumcraft:block/woodplain");
        expectedSprite.put("itemthaumometer", "minecraft:item/compass_00");
        final List<String> broken = new ArrayList<>();
        for (final Map.Entry<String, String> entry : expectedSprite.entrySet()) {
            final JsonObject model = parseModel("item/" + entry.getKey());
            if (model == null) {
                broken.add(entry.getKey() + " (no item model)");
                continue;
            }
            final List<String> sprites = modelSprites(model);
            if (!sprites.contains(entry.getValue())) {
                broken.add(entry.getKey() + " (model names " + sprites + ", not the sprite the original "
                        + "drew for this item — " + entry.getValue() + ")");
            }
            if (ItemMetaAppearance.customShape(entry.getKey()) != null && !model.has("elements")) {
                broken.add(entry.getKey() + " (the original rendered a shape; the model declares no elements)");
            }
            if (ItemMetaAppearance.customShape(entry.getKey()) == null
                    && ItemMetaAppearance.noPerMetaAppearanceIds().contains(entry.getKey())
                    && model.has("overrides")) {
                broken.add(entry.getKey() + " (measured to draw identically per meta; it carries overrides)");
            }
        }
        helper.assertTrue(expectedSprite.keySet().containsAll(ItemMetaAppearance.customShapeIds()),
                "a custom-shape id has no expectation in this probe: " + ItemMetaAppearance.customShapeIds());
        ThaumcraftClassic.LOGGER.info("[BD-17] custom-renderer replacements: {} items checked, {} broken",
                expectedSprite.size(), broken.size());
        helper.assertTrue(broken.isEmpty(),
                broken.size() + " custom-renderer replacements do not resolve as the original drew: "
                        + preview(broken));
        helper.succeed();
    }

    /**
     * Probe 8: the per-meta sprites resolve at the published path — gated only
     * when an overlay is installed, exactly like SPINE-3's sprite probe (D-004
     * ships the original textures as a local overlay only).
     */
    @GameTest(template = "empty")
    public static void everyMetaSpriteResolvesWhenTheOverlayIsInstalled(final GameTestHelper helper) {
        if (ItemMetaAppearanceGameTest.class.getResource(ASSET_ROOT + OVERLAY_PROBE_SPRITE) == null) {
            ThaumcraftClassic.LOGGER.info("[BD-17] per-meta sprite resolution not gated: no asset overlay on "
                            + "this classpath ({} is absent) — D-004 ships original textures as a local "
                            + "overlay only", OVERLAY_PROBE_SPRITE);
            helper.succeed();
            return;
        }
        final List<String> unresolved = new ArrayList<>();
        int refs = 0;
        for (final Map.Entry<String, List<ItemMetaAppearance.MetaArt>> family : ItemMetaAppearance.allTables().entrySet()) {
            for (final ItemMetaAppearance.MetaArt art : family.getValue()) {
                final String modelPath = ItemMetaAppearance.customShape(family.getKey()) == null
                        ? "itemmeta/" + family.getKey() + "_" + art.meta()
                        : "item/" + family.getKey();
                for (final String sprite : art.sprites()) {
                    refs++;
                    if (!sprite.contains(":")) {
                        unresolved.add(modelPath + " (unnamespaced sprite " + sprite + ")");
                        continue;
                    }
                    final String texture = "textures/" + sprite.substring(sprite.indexOf(':') + 1) + ".png";
                    if (sprite.startsWith(ThaumcraftClassic.MOD_ID + ":")
                            && ItemMetaAppearanceGameTest.class.getResource(ASSET_ROOT + texture) == null) {
                        unresolved.add(modelPath + " [" + sprite + "] -> " + texture + " not published");
                    }
                }
            }
        }
        ThaumcraftClassic.LOGGER.info("[BD-17] per-meta sprite resolution: {} references, {} unresolved",
                refs, unresolved.size());
        helper.assertTrue(refs > 0, "the table names no sprites — probe cannot fail");
        helper.assertTrue(unresolved.isEmpty(),
                unresolved.size() + " per-meta sprites are not published by the installed overlay: "
                        + preview(unresolved));
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** The families the gates are accountable for: table rows ∩ shipped registry. */
    private static Map<String, List<ItemMetaAppearance.MetaArt>> familiesOfShippedContent() {
        final Map<String, ResourceLocation> shipped = shippedItemIds();
        final Map<String, List<ItemMetaAppearance.MetaArt>> out = new TreeMap<>();
        for (final String idPath : ItemMetaAppearance.familyIds()) {
            if (shipped.containsKey(idPath)) {
                out.put(idPath, ItemMetaAppearance.family(idPath));
            }
        }
        return out;
    }

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

    private static ResourceLocation keyOf(final String idPath) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, idPath);
    }

    /**
     * Build the stack the way shipped code builds it: a plain family item
     * through its own factory, a block item through the variant component.
     * A family with no known construction path is a gate failure, not a skip.
     */
    private static ItemStack stackFor(final String idPath, final Item item, final int meta,
                                      final List<String> broken) {
        if (item == null || !BuiltInRegistries.ITEM.containsKey(keyOf(idPath))) {
            broken.add(idPath + " (not registered)");
            return null;
        }
        if (item instanceof BlockItem blockItem) {
            final ItemStack stateStack = ItemMetaAppearance.variantStack(blockItem, meta);
            if (stateStack == null) {
                broken.add(idPath + " meta " + meta + " is outside the block's int meta-carrier range "
                        + java.util.Arrays.toString(ItemMetaAppearance.metaCarrierRange(blockItem)));
                return null;
            }
            return stateStack;
        }
        // Plain family items: the factory is the port's own meta
        // constructor — any public static {@code ItemStack f(int)}
        // (ItemResource/ItemShard/ItemNugget {@code stack}) or
        // {@code ItemStack f(Item,int)} (ItemLootBag {@code withRarity}).
        for (final java.lang.reflect.Method method : item.getClass().getMethods()) {
            final Class<?>[] params = method.getParameterTypes();
            final boolean staticFactory = java.lang.reflect.Modifier.isStatic(method.getModifiers());
            if (!staticFactory || method.getReturnType() != ItemStack.class) {
                continue;
            }
            try {
                if (params.length == 1 && params[0] == int.class) {
                    return (ItemStack) method.invoke(null, meta);
                }
                if (params.length == 2 && params[0] == Item.class && params[1] == int.class) {
                    return (ItemStack) method.invoke(null, item, meta);
                }
            } catch (ReflectiveOperationException bad) {
                broken.add(idPath + "." + method.getName() + "(" + meta + ") threw " + bad.getCause());
                return null;
            }
        }
        // No factory: the item carries its meta through the mirror
        // primitive itself (loot/commands set it; the label and the model
        // both read it). Asserting the primitive on this stack is the
        // weakest form of the gate; families with a real factory above
        // cover the strong form.
        return ItemMetaAppearance.mirror(new ItemStack(item), meta);
    }

    /** Can this item's block express the meta at all (registry-derived range)? */
    private static boolean tabRangeAllows(final Item item, final int meta) {
        if (!(item instanceof BlockItem blockItem)) {
            return true;
        }
        return java.util.Arrays.stream(ItemMetaAppearance.metaCarrierRange(blockItem)).anyMatch(v -> v == meta);
    }

    /** The per-meta label keys of an id, derived from the shipped lang file. */
    private static List<String> metaLabelKeys(final JsonObject lang, final String idPath) {
        final String prefix = "item." + ThaumcraftClassic.MOD_ID + "." + idPath + ".";
        final String blockPrefix = "block." + ThaumcraftClassic.MOD_ID + "." + idPath + ".";
        final List<String> keys = new ArrayList<>();
        for (final Map.Entry<String, JsonElement> entry : lang.entrySet()) {
            final String key = entry.getKey();
            final String tail = key.startsWith(prefix) ? key.substring(prefix.length())
                    : key.startsWith(blockPrefix) ? key.substring(blockPrefix.length()) : null;
            if (tail != null && tail.chars().allMatch(Character::isDigit)
                    && !entry.getValue().getAsString().isEmpty()) {
                keys.add(key);
            }
        }
        keys.sort(String::compareTo);
        return keys;
    }

    private static List<String> committedMetaModelNames() {
        final java.net.URL dir = ItemMetaAppearanceGameTest.class.getResource(MODELS_METAS_DIR);
        if (dir == null) {
            return List.of();
        }
        final Path root;
        try {
            root = Path.of(dir.toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException("the shipped asset tree is not addressable: " + MODELS_METAS_DIR, e);
        }
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.list(root)) {
            return walk.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().substring(0, p.getFileName().toString().length() - 5))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Every sprite a model declares, following the parent chain for inherits. */
    private static List<String> modelSprites(final JsonObject model) {
        final List<String> sprites = new ArrayList<>();
        final Deque<String> chain = new ArrayDeque<>();
        chain.add(modelPathOf(model));
        final Set<String> seen = new LinkedHashSet<>();
        JsonObject current = model;
        while (current != null) {
            if (current.has("textures")) {
                for (final Map.Entry<String, JsonElement> entry : current.getAsJsonObject("textures").entrySet()) {
                    final String value = entry.getValue().getAsString();
                    if (!value.startsWith("#") && !sprites.contains(value)) {
                        sprites.add(value);
                    }
                }
            }
            final String parent = current.has("parent") ? current.get("parent").getAsString() : null;
            if (parent == null || !parent.startsWith(ThaumcraftClassic.MOD_ID + ":")
                    || !seen.add(parent)) {
                break;
            }
            current = parseModel(parent.substring(ThaumcraftClassic.MOD_ID.length() + 1));
        }
        return sprites;
    }

    private static String modelPathOf(final JsonObject model) {
        return model.has("parent") ? model.get("parent").getAsString() : "(none)";
    }

    private static int declaredSprites(final JsonObject model) {
        return modelSprites(model).size();
    }

    /**
     * How many item layers a model declares: the {@code layer0}/{@code layer1}
     * texture keys a {@code minecraft:item/generated} model draws, followed
     * through a parent this port authored (a model that declares no layers of
     * its own inherits its parent's).
     */
    private static int layerCount(final JsonObject model) {
        int layers = 0;
        JsonObject current = model;
        final List<String> seen = new ArrayList<>();
        while (current != null) {
            if (current.has("textures")) {
                for (final Map.Entry<String, JsonElement> texture :
                        current.getAsJsonObject("textures").entrySet()) {
                    if (texture.getKey().matches("layer\\d+")) {
                        layers++;
                    }
                }
            }
            if (layers > 0 || !current.has("parent")) {
                break;
            }
            final String parent = current.get("parent").getAsString();
            if (!parent.startsWith(ThaumcraftClassic.MOD_ID + ":") || seen.contains(parent)) {
                break;
            }
            seen.add(parent);
            current = parseModel(parent.substring(ThaumcraftClassic.MOD_ID.length() + 1));
        }
        return layers;
    }

    /**
     * The layers of the model that renders one meta: the {@code itemmeta}
     * model when the override points at one, otherwise the base item model.
     */
    private static int layersOfMetaModel(final String idPath, final int meta) {
        final JsonObject metaModel = parseModel("itemmeta/" + idPath + "_" + meta);
        if (metaModel != null) {
            return layerCount(metaModel);
        }
        final JsonObject base = parseModel("item/" + idPath);
        return base == null ? 0 : layerCount(base);
    }

    private static boolean modelExists(final String modelPath) {
        return ItemMetaAppearanceGameTest.class.getResource(ASSET_ROOT + "models/" + modelPath + ".json") != null;
    }

    private static JsonObject parseModel(final String modelPath) {
        try (InputStream in = ItemMetaAppearanceGameTest.class
                .getResourceAsStream(ASSET_ROOT + "models/" + modelPath + ".json")) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            ThaumcraftClassic.LOGGER.warn("[BD-17] unreadable model models/{}.json", modelPath, e);
            return null;
        }
    }

    private static JsonElement loadJson(final String assetRelativePath) {
        try (InputStream in = ItemMetaAppearanceGameTest.class.getResourceAsStream(assetRelativePath)) {
            if (in == null) {
                throw new IllegalStateException(assetRelativePath + " is not on the classpath");
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String preview(final List<String> items) {
        final int shown = Math.min(items.size(), 25);
        return items.subList(0, shown) + (items.size() > shown ? " …(+" + (items.size() - shown) + " more)" : "");
    }
}
