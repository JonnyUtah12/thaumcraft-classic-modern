package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The per-metadata appearance table — the port of 4.2.3.5's
 * {@code Item.registerIcons}/{@code getIconFromDamage} and
 * {@code Block.getIcon(side, meta)} item-icon mappings (BD-17, t_f04c44fb).
 *
 * <h2>What 4.2.3.5 draws per metadata</h2>
 * The 1.7.10 item stack renderer resolved the icon per stack:
 * {@code Item.getIconFromDamage(stack.getItemDamage())} for item classes, and
 * for a block item {@code RenderBlocks.renderBlockAsItem} used
 * {@code block.getIcon(0, meta)} on every face. This table transcribes, per
 * family, exactly that resolution: which sprite the original showed for each
 * metadata, and for the families whose original draw was a custom renderer,
 * the shape that renderer instantiated.
 *
 * <h2>Per-meta families (measured per class against {@code reference/decompiled/})</h2>
 * Each row of the table below carries its own file:line citation in the static
 * block. The shape of the measurement: an item class answers
 * {@code getIconFromDamage(md)} from the icon array its {@code registerIcons}
 * filled, and a block item is drawn by {@code RenderBlocks.renderBlockAsItem},
 * which paints {@code block.getIcon(0, meta)} (side 0) on every face — so for
 * a block family the item's art is that block's side-0 face per metadata.
 * <ul>
 *   <li>{@code itemresource} 19 metas (icon index == meta,
 *       {@code items/ItemResource.java:50-69}, 73-75; the label, meta 13, is a
 *       two-layer stack: {@code label} under {@code label_over}, 64 + 78-85).</li>
 *   <li>{@code itemnugget} 15 metas — nuggets at 0-7, clusters at 16-21 and
 *       31, sparse because {@code registerIcons} registers AT the meta index
 *       ({@code items/ItemNugget.java:26-40}, 44-46).</li>
 *   <li>{@code itemshard} — {@code shard} for metas 0-5,
 *       {@code shard_balanced} for meta 6 ({@code items/ItemShard.java:26-28}, 32-34);
 *       the six plain metas also carry the per-meta ore colour ({@link ItemMetaTints}).</li>
 *   <li>{@code itemlootbag} — common / uncommon / rare
 *       ({@code items/ItemLootBag.java:30-34}, 37-39).</li>
 *   <li>The baubles — {@code itemamuletrunic}, {@code itemamuletvis},
 *       {@code itemgirdlerunic}, {@code itemringrunic}, {@code itembaubleblanks}:
 *       {@code registerIcons} at the meta index, {@code getIconFromDamage(md) = icon[md]}
 *       ({@code common/items/baubles/}, cited per row).</li>
 *   <li>{@code itemcompassstone} — meta 1 (active) draws
 *       {@code sinister_stone_active} ({@code items/ItemCompassStone.java:32-41});
 *       {@code arcanedoorkey} — iron key at meta 0, gold key at any other
 *       ({@code items/ItemKey.java:38-45}).</li>
 *   <li>The block families — {@code blockcosmeticopaque}, {@code blockmirror},
 *       {@code blockmetaldevice}, {@code blockwoodendevice},
 *       {@code blockcosmeticsolid}, {@code blockcosmeticslabstone},
 *       {@code blockcosmeticslabwood}, {@code blockstonedevice},
 *       {@code blockcustomore}, {@code blockmanapod}: side-0 face per meta,
 *       each row cited to its block's {@code registerIcons} + {@code getIcon}.</li>
 *   <li>{@code itemessence} is NOT a family: the phial's identity is the aspect
 *       (a tinted overlay layer, {@link ItemMetaTints}), the sprite never moves
 *       with the metadata — measured, in the exclusion table with the reason.</li>
 * </ul>
 *
 * <h2>The four items the card calls custom-renderer items</h2>
 * Measured, only one of the four is drawn by an item-side renderer in
 * 4.2.3.5. {@code trunkspawner}: {@code ItemTrunkSpawnerRenderer} binds
 * {@code textures/models/trunk.png} and renders a vanilla {@code ModelChest}
 * ({@code client/renderers/item/ItemTrunkSpawnerRenderer.java:13}, 26, 39;
 * registered as the item renderer at {@code ClientProxy.java:404}) — reproduced
 * as a box element model over that same texture. {@code blockjarfilleditem}:
 * its item renderer draws {@code renderBlockAsItem(blockJar, meta, 1.0F)}, i.e.
 * {@code jar_bottom} for every meta, and the animated contents are the
 * tile-entity renderer's ({@code client/renderers/tile/ItemJarFilledRenderer.java:61}, 69)
 * — so the item's art is the jar-bottom face, no per-meta change.
 * {@code tilearcaneworkbench}: the original registers NO item renderer for it;
 * {@code ModelArcaneWorkbench} over {@code models/worktable.png} is the tile
 * renderer's in-world draw, and the item is a BlockTable block item, so its
 * face is the single {@code woodplain} icon ({@code BlockTable.java:50-52}).
 * {@code itemthaumometer}: its original dial tracked the nearest vis source
 * live, which no static model can express, so it keeps the vanilla compass
 * chain SPINE-3 shipped. Those three residuals are recorded in the BD-17 row
 * of {@code docs/BEHAVIORAL_DEVIATIONS.md}.</p>
 *
 * <h2>Families the original draws identically for every meta</h2>
 * {@link #NO_PER_META_APPEARANCE} — measured, cited. The gate asserts the
 * exclusion side as hard as the inclusion side, so a family that starts
 * drawing per-meta art in a later slice fails the gate until it is added to
 * this file.</p>
 *
 * <h2>The mechanism, and why the mirror lives here</h2>
 * One registered id per family stays (D-018). Per-meta selection runs through
 * the model's {@code overrides} keyed on {@code minecraft:custom_model_data},
 * the int component 1.21.1's {@code ItemProperties} can read — a predicate
 * name vanilla does not know is dropped at load ({@code ItemProperties$Type}
 * has no {@code item_meta} entry), so the override predicate can only be
 * {@code custom_model_data}. {@link #mirror(ItemStack, int)} writes the
 * authoritative {@code thaumcraft:item_meta} (D-018) and the model key in one
 * call, and every meta-carrying path in the port calls it, so a label can
 * never disagree with a sprite. {@code scripts/generate_itemmeta_models.py}
 * generates the model JSONs — the per-meta models and the {@code overrides}
 * arrays — from this table, so the table is the single source of truth.</p>
 */
public final class ItemMetaAppearance {

    /** Full cube 0..16 — the original's block-item projection (render type 3). */
    public static final int SHAPE_CUBE = 0;
    /** Box 1.5..14.5 — the geometry of the in-world model classes. */
    public static final int SHAPE_TABLE = 1;

    /**
     * One metadata's art: -1 = a flat generated layer (the icon-indexed
     * families), {@link #SHAPE_CUBE}/{@link #SHAPE_TABLE} = a box model built
     * from the geometry of the model class the original renderer instantiated.
     */
    public record MetaArt(int meta, int shape, List<String> sprites) {
        public MetaArt {
            sprites = List.copyOf(sprites);
        }

        public boolean flat() {
            return shape < 0;
        }
    }

    private static final Map<String, List<MetaArt>> FAMILIES = new LinkedHashMap<>();
    private static final Map<String, MetaArt> CUSTOM_SHAPES = new LinkedHashMap<>();
    private static final Map<String, String> NO_PER_META_APPEARANCE = new TreeMap<>();

    static {
        // ItemResource: getIconFromDamage(md) = icon[md], and registerIcons
        // fills icon[] at the meta index (items/ItemResource.java:49-68) —
        // meta 14 is Salis Mundus drawn with the dust icon, meta 15 the
        // Primal Charm drawn with the charm icon, meta 16 the void ingot,
        // 17 the void seed, 18 the gold coin. The label (md 13) is a
        // two-layer stack: label under label_over.
        FAMILIES.put("itemresource", List.of(
                flat("alumentum", 0), flat("nitor", 1), flat("thaumiumingot", 2),
                flat("quicksilver", 3), flat("tallow", 4), flat("brain", 5),
                flat("amber", 6), flat("cloth", 7), flat("filter", 8),
                flat("knowledgefragment", 9), flat("mirrorglass", 10),
                flat("taint_slime", 11), flat("taint_tendril", 12),
                two("label", "label_over", 13),
                flat("dust", 14), flat("charm", 15), flat("voidingot", 16),
                flat("voidseed", 17), flat("coin", 18)));
        // ItemNugget: getIconFromDamage(md) = icon[md] and registerIcons
        // registers at the meta index, so the metas are sparse and
        // asymmetric — items/ItemNugget.java:25-40, 43-45: nuggets at
        // 0-7 (iron, copper, tin, silver, lead, quicksilver, thaumium,
        // void), clusters at 16-21 + 31 (iron, copper, tin, silver, lead,
        // cinnabar at 21, gold at 31). 15 metas, matching the original's
        // getSubItems list (0,5,21,6,7,16,31 + the Config-gated metal
        // pairs).
        FAMILIES.put("itemnugget", List.of(
                flat("nuggetiron", 0), flat("nuggetcopper", 1), flat("nuggettin", 2),
                flat("nuggetsilver", 3), flat("nuggetlead", 4), flat("nuggetquicksilver", 5),
                flat("nuggetthaumium", 6), flat("nuggetvoid", 7),
                flat("clusteriron", 16), flat("clustercopper", 17), flat("clustertin", 18),
                flat("clustersilver", 19), flat("clusterlead", 20), flat("clustercinnabar", 21),
                flat("clustergold", 31)));
        // ItemShard: seven metas; getIconFromDamage(md) = md == 6 ?
        // shard_balanced : shard (items/ItemShard.java:22-26), so only the
        // balanced shard draws a different icon — the six plain metas share
        // the shard sprite and differ by the tint pass.
        FAMILIES.put("itemshard", List.of(
                flat("shard", 0), flat("shard", 1), flat("shard", 2), flat("shard", 3),
                flat("shard", 4), flat("shard", 5), flat("shard_balanced", 6)));
        // ItemLootBag: icon[0..2] = lootbag / lootbagunc / lootbagrare,
        // getIconFromDamage(md) = icon[md] (items/ItemLootBag.java:30-37).
        FAMILIES.put("itemlootbag", List.of(
                flat("lootbag", 0), flat("lootbagunc", 1), flat("lootbagrare", 2)));
        FAMILIES.put("blockcosmeticopaque", List.of(
                face("block/amberblock_top", 0), face("block/amberbrick", 1), face("block/warded_glass_1", 2)));
        FAMILIES.put("blockmirror", List.of(face("block/mirrorframe", 0), face("block/mirrorframe2", 6)));
        FAMILIES.put("blockmetaldevice", List.of(
                face("block/metalbase", 0), face("block/metalbase", 1), face("block/metalbase", 2),
                face("block/alchemyblockadv", 3), face("block/metalbase", 5), face("block/metalbase", 6),
                face("block/lamp_side", 7), face("block/lamp_grow_side", 8),
                face("block/alchemyblock", 9), face("block/alchemyblock", 10),
                face("block/alchemyblock", 11), face("block/brainbox", 12),
                face("block/lamp_fert_side", 13), face("block/metalbase", 14)));
        // Baubles and runics: registerIcons fills icon[] at the meta index
        // (getIconFromDamage(md) = icon[md]) — the amulet gets an emergency
        // variant, the vis amulet a lesser one, the ring charges, the blanks
        // differ per slot. Measured per class in common/items/.
        FAMILIES.put("itemamuletrunic", List.of(
                flat("runic_amulet", 0), flat("runic_amulet_emergency", 1)));
        FAMILIES.put("itemamuletvis", List.of(
                flat("vis_amulet_lesser", 0), flat("vis_amulet", 1)));
        FAMILIES.put("itemgirdlerunic", List.of(
                flat("runic_girdle", 0), flat("runic_girdle_kinetic", 1)));
        FAMILIES.put("itemringrunic", List.of(
                flat("runic_ring_lesser", 0), flat("runic_ring", 1),
                flat("runic_ring_charged", 2), flat("runic_ring_regen", 3)));
        FAMILIES.put("itembaubleblanks", List.of(
                flat("bauble_amulet", 0), flat("bauble_ring", 1),
                flat("bauble_belt", 2), flat("bauble_ring_iron", 3)));
        // Cosmetic slabs delegate to the block they copy:
        // BlockCosmeticWoodSlab.java:30-32 — the face of WoodenDevice meta
        // (md&7)+6, i.e. side 0: 0 → planks_greatwood, 1 → planks_silverwood.
        // The slabs delegate their face to the block they copy:
        // BlockCosmeticStoneSlab.java:30-34 asks BlockCosmeticSolid.getIcon(side, 7)
        // for (md&7)==0 and getIcon(side, 11) for ==1, and BlockCosmeticSolid's
        // getIcon (79-107) answers icon[13] = arcane_stone and icon[17] = es_1
        // (registered at :56-60) — NOT CosmeticSolid's own metas 2 and 5.
        FAMILIES.put("blockcosmeticslabstone", List.of(
                face("block/arcane_stone", 0), face("block/es_1", 1)));
        FAMILIES.put("blockcosmeticslabwood", List.of(
                face("block/planks_greatwood", 0), face("block/planks_silverwood", 1)));
        // BlockWoodenDevice: side 0 per meta — 0 → iconDefault woodplain,
        // 6 → planks_greatwood, 7 → planks_silverwood, 2/3 → iconAPPlate[0]
        // applate1, and for 1/4/5/8 the arcane-ear branch, which answers
        // iconAEar[2] = arcaneearbottom while renderState == 0
        // (blocks/BlockWoodenDevice.java:61-75 register, 100-136 getIcon).
        // renderState is mutated only in-world (the block's own md==1 tick
        // path, client/renderers/block/BlockWoodenDeviceRenderer.java:190-227),
        // and the ITEM draw always runs with renderState == 0, so the bell-top
        // icon (iconAEar[6]) is an in-world state, never the item's face.
        // Rows = the original's tab metas (:82-91 = 0,1,2,4,5,6,7,8).
        FAMILIES.put("blockwoodendevice", List.of(
                face("block/woodplain", 0), face("block/arcaneearbottom", 1), face("block/applate1", 2),
                face("block/arcaneearbottom", 4), face("block/arcaneearbottom", 5),
                face("block/planks_greatwood", 6), face("block/planks_silverwood", 7),
                face("block/arcaneearbottom", 8)));
        // BlockCosmeticSolid (16 variants shipped, CatalogDecorativeBlocks:66-68):
        // side 0 per meta — 0/1/8 → icon[0] obsidiantile, 2 → icon[7]
        // paving_stone_travel, 3 → icon[8] paving_stone_warding, 4 → icon[9]
        // thaumiumblock, 5 → icon[11] tallowblock_top (side 0 takes the
        // par1<=1 branch), 6 → icon[12] pedestal_top, 7/9/10 → icon[13]
        // arcane_stone, 11/13/15 → icon[17] es_1, 12 → icon[21] er_1,
        // 14 → icon[25] crust (blocks/BlockCosmeticSolid.java:49-75 register,
        // 79-107 getIcon). The rows are the metas the original's tab lists
        // (:158-173 = 0-9,11,12,14,15); metas 10 and 13 draw what 9 and 11 draw.
        FAMILIES.put("blockcosmeticsolid", List.of(
                face("block/obsidiantile", 0), face("block/obsidiantile", 1),
                face("block/paving_stone_travel", 2), face("block/paving_stone_warding", 3),
                face("block/thaumiumblock", 4), face("block/tallowblock_top", 5),
                face("block/pedestal_top", 6), face("block/arcane_stone", 7),
                face("block/obsidiantile", 8), face("block/arcane_stone", 9),
                face("block/es_1", 11), face("block/er_1", 12), face("block/crust", 14),
                face("block/es_1", 15)));
        // BlockStoneDevice: side 0 is iconPedestal[1] = pedestal_top for every
        // meta EXCEPT 8 (the focus pedestal), whose side-0 face is
        // iconWandPedestalFocus[2] = wandpedestal_focus_bot
        // (blocks/BlockStoneDevice.java:66-80 register, 95-151 getIcon: the
        // md==8 branch at 124-127, the pedestal tail at 149). Rows = the
        // original's tab metas (:180-192 = 0,1,2,5,8,9,10,11,12,13,14).
        FAMILIES.put("blockstonedevice", List.of(
                face("block/pedestal_top", 0), face("block/pedestal_top", 1),
                face("block/pedestal_top", 2), face("block/pedestal_top", 5),
                face("block/wandpedestal_focus_bot", 8), face("block/pedestal_top", 9),
                face("block/pedestal_top", 10), face("block/pedestal_top", 11),
                face("block/pedestal_top", 12), face("block/pedestal_top", 13),
                face("block/pedestal_top", 14)));
        // BlockCustomOre: getIcon(side, meta) answers from the METADATA —
        // meta 0 = cinnibar, metas 1-6 = infusedorestone, meta 7 = amberore;
        // its `side == 7` / `side == 15` branches are dead (sides are 0..6),
        // and the meta-15 frostshard answer is outside the port's 0..7 variant
        // range (blocks/BlockCustomOre.java:45), so it gets no row. Registered
        // at blocks/BlockCustomOre.java:43-49, answered at 52-60; tab 0..7.
        // (The icon-array indices are 0=cinnibar, 1=infusedorestone,
        // 2=infusedore, 3=amberore, 4=frostshard — icon[2] is used by no face
        // of getIcon, which is why no row names it.)
        FAMILIES.put("blockcustomore", List.of(
                face("block/cinnibar", 0), face("block/infusedorestone", 1),
                face("block/infusedorestone", 2), face("block/infusedorestone", 3),
                face("block/infusedorestone", 4), face("block/infusedorestone", 5),
                face("block/infusedorestone", 6), face("block/amberore", 7)));
        // BlockManaPod: the original's metadata IS the growth stage, and its
        // side-0 face walks the stem — 0 → manapod_stem_0, 1 → manapod_stem_1,
        // 2+ → manapod_stem_2 (blocks/BlockManaPod.java:42-46 register, 54-56
        // getIcon). The port expresses the stage as the int "age" property
        // (blocks/BlockManaPod.java:59, 0..7), the meta carrier this family
        // reads — see metaCarrierRange.
        FAMILIES.put("blockmanapod", List.of(
                face("block/manapod_stem_0", 0), face("block/manapod_stem_1", 1),
                face("block/manapod_stem_2", 2), face("block/manapod_stem_2", 3),
                face("block/manapod_stem_2", 4), face("block/manapod_stem_2", 5),
                face("block/manapod_stem_2", 6), face("block/manapod_stem_2", 7)));
        // ItemKey (arcanedoorkey): meta 0 = the iron key, any other meta the
        // gold key (items/ItemKey.java:38-45: registerIcons keys keyiron /
        // keygold, getIconFromDamage(md) = md == 0 ? iconIron : iconGold).
        FAMILIES.put("arcanedoorkey", List.of(flat("keyiron", 0), flat("keygold", 1)));
        // ItemCompassStone: meta 1 (the active stone) draws
        // sinister_stone_active, every other meta sinister_stone
        // (items/ItemCompassStone.java:32-41).
        FAMILIES.put("itemcompassstone", List.of(
                flat("sinister_stone", 0), flat("sinister_stone_active", 1)));

        // The one item-side shape renderer: ItemTrunkSpawnerRenderer binds
        // textures/models/trunk.png (:26) and renders a vanilla ModelChest
        // (:13, :39) — registered as the ITEM renderer for trunkspawner at
        // client/ClientProxy.java:404. The item's own registered icon is
        // "blank" (common/entities/golems/ItemTrunkSpawner.java:36), so the
        // flat art SPINE-3 shipped drew nothing the original drew. Reproduced
        // as a box element over the original's own texture; a 0..16 cube is
        // closer to the chest's full-footprint body than a 1.5..14.5 box.
        CUSTOM_SHAPES.put("trunkspawner",
                new MetaArt(0, SHAPE_CUBE, List.of("thaumcraft:models/trunk")));

        // Measured: the original's item sprite does not move with meta. The
        // gate treats these ids as required exclusions (a meta-labelled family
        // must be in exactly one of the two tables).
        NO_PER_META_APPEARANCE.put("itemessence",
                "The filled phial's identity is the aspect (tinted overlay layer, "
                        + "ItemMetaTints) and the meta label; getIconFromDamage returns the single "
                        + "phial icon for every meta and the filled phial adds a second layer with "
                        + "the separately registered essence icon, tinted by the first aspect "
                        + "(items/ItemEssence.java:37-40 register, 42-44 getIconFromDamage, "
                        + "46-54 per-pass overlay, 57-63 colour)");
        NO_PER_META_APPEARANCE.put("blockairy",
                "BlockAiryItem.java:22-25 register one taint_over_2 icon, 27-29 return it for "
                        + "every meta (the in-world node shape is ItemNodeRenderer's, ClientProxy.java:401)");
        NO_PER_META_APPEARANCE.put("blockcandle",
                "BlockCandle.java:43-46 register one candle icon, 48-51 return it for every side "
                        + "and meta; the per-meta colour is colourPerMetadata = Utils.colors[md] "
                        + "(:76-79), a block-side colour, so it travels with the blockstates card");
        NO_PER_META_APPEARANCE.put("blockcrystal",
                "BlockCrystal.java:51-53 register one crystal icon, 55-57 return it for every side "
                        + "and meta; the per-meta colour (:77-83) is a block-side colour");
        NO_PER_META_APPEARANCE.put("blockjar",
                "BlockJar.java:58-65 register the jar faces, 68-78: side 0 is jar_bottom for "
                        + "metas 0, 1, 2, 3 and every other meta; tab metas 0,1,3 (:85-89)");
        NO_PER_META_APPEARANCE.put("blockjarfilleditem",
                "The item's own registered icon is \"blank\" (common/blocks/ItemJarFilled.java:33-41) "
                        + "and its item renderer is ItemJarFilledRenderer, which draws "
                        + "renderBlocks.renderBlockAsItem(ConfigBlocks.blockJar, meta, 1.0F) "
                        + "(client/renderers/tile/ItemJarFilledRenderer.java:69) — i.e. "
                        + "BlockJar.getIcon(0, meta) = jar_bottom for every meta (BlockJar.java:68-78). "
                        + "The animated contents are the tile-entity renderer's (:61), not item art.");
        NO_PER_META_APPEARANCE.put("blocktable",
                "BlockTable.java:44-47 register one woodplain icon, 50-52 return it for every side "
                        + "and meta; iconQuill is the tile-rendered quill overlay, and the "
                        + "worktable model is the tile renderer's (see tilearcaneworkbench)");
        NO_PER_META_APPEARANCE.put("tilearcaneworkbench",
                "The 1.7.10 id \"tile.arcaneworkbench\" is the block item of BlockTable, so its item "
                        + "draw is Block.getIcon(0, meta) = the single woodplain icon "
                        + "(common/blocks/BlockTable.java:50-52, tab metas 0,14,15 at :59-63). "
                        + "ModelArcaneWorkbench over textures/models/worktable.png is the TILE "
                        + "renderer's in-world draw (client/renderers/tile/TileArcaneWorkbenchRenderer.java:19-23, "
                        + "registered at ClientProxy.java:471) — ClientProxy registers no item "
                        + "renderer for this id (:399-406), so the shape belongs with the block "
                        + "states / tile renderers, not with the item model");
    }

    private ItemMetaAppearance() {
    }

    // ------------------------------------------------------------------ table reads

    /** The per-meta families the model generator and the gates iterate. */
    public static Set<String> familyIds() {
        return FAMILIES.keySet();
    }

    /** The custom-renderer shape replacements. */
    public static Set<String> customShapeIds() {
        return CUSTOM_SHAPES.keySet();
    }

    /** Ids the original draws identically per meta — required exclusions. */
    public static Set<String> noPerMetaAppearanceIds() {
        return NO_PER_META_APPEARANCE.keySet();
    }

    public static String exclusionReason(final String idPath) {
        return NO_PER_META_APPEARANCE.get(idPath);
    }

    public static List<MetaArt> family(final String idPath) {
        return FAMILIES.getOrDefault(idPath, List.of());
    }

    public static boolean hasFamily(final String idPath) {
        return FAMILIES.containsKey(idPath);
    }

    public static MetaArt customShape(final String idPath) {
        return CUSTOM_SHAPES.get(idPath);
    }

    /** Families plus custom shapes, for the generator and the sprite probe. */
    public static Map<String, List<MetaArt>> allTables() {
        final Map<String, List<MetaArt>> all = new TreeMap<>(FAMILIES);
        CUSTOM_SHAPES.forEach((id, art) -> all.put(id, List.of(art)));
        return all;
    }

    /** The metas of a family id, ascending, for tab enumeration and the gates. */
    public static int[] metasOf(final String idPath) {
        return FAMILIES.getOrDefault(idPath, List.of()).stream().mapToInt(MetaArt::meta).toArray();
    }

    /**
     * The metas the creative tab lists for a family — the original's tab listed
     * one entry per metadata its {@code CreativeTabs.getDisplayItemStacks}
     * walked out of {@code Item.getSubItems}, which is the visible metas, not
     * every value the block can store. So the tab set is the table's rows
     * (the measured visible set) clamped to what the stack can carry (the
     * block's int property range, {@link #metaCarrierRange}) and, when the id
     * has per-meta labels in the shipped lang file, to the labelled metas —
     * §15 rule 9 (a tab entry needs a label). An id with no rows, or one with
     * rows but no labels (a crop's growth stages: the original listed one
     * entry), gets one plain entry.
     */
    public static int[] tabMetasOf(final net.minecraft.world.item.Item item) {
        final String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        final int[] rows = metasOf(path);
        if (rows.length == 0) {
            return new int[0];
        }
        int[] metas = rows;
        if (item instanceof BlockItem blockItem) {
            final int[] range = metaCarrierRange(blockItem);
            metas = java.util.Arrays.stream(metas).filter(value -> contains(range, value)).toArray();
        }
        final Set<Integer> labelled = langLabelledMetas(path);
        if (labelled.isEmpty()) {
            return new int[0]; // no per-meta labels: §15 rule 9 — one labelled entry
        }
        return java.util.Arrays.stream(metas).filter(labelled::contains).distinct().sorted().toArray();
    }

    /**
     * The metadata values a block item's stack can carry: the names in
     * {@link VariantStateItems#META_CARRIER_PROPERTIES} — the int
     * {@code variant} property (D-018), and for a growth-stage block the int
     * {@code age} property, which is where this port stores the metadata the
     * original's {@code getIcon(side, meta)} switched on. Empty when the block
     * has neither.
     */
    public static int[] metaCarrierRange(final BlockItem blockItem) {
        for (final String name : VariantStateItems.META_CARRIER_PROPERTIES) {
            final Property<?> property =
                    blockItem.getBlock().getStateDefinition().getProperty(name);
            if (property instanceof IntegerProperty integers) {
                return integers.getPossibleValues().stream().mapToInt(Integer::intValue).sorted().toArray();
            }
        }
        return new int[0];
    }

    /**
     * The stack for one metadata of a block item, built through the block's own
     * int property (never a raw int): {@code null} when that property has no
     * such value, so no caller can invent a meta the block cannot place. The
     * returned stack carries the {@code BLOCK_STATE} component and, through
     * {@code VariantStateItems.withVariant}, the appearance mirror.
     */
    public static ItemStack variantStack(final BlockItem blockItem, final int meta) {
        for (final String name : VariantStateItems.META_CARRIER_PROPERTIES) {
            final Property<?> property =
                    blockItem.getBlock().getStateDefinition().getProperty(name);
            if (property instanceof IntegerProperty integers) {
                if (!integers.getPossibleValues().contains(meta)) {
                    return null;
                }
                return VariantStateItems.withVariant(new ItemStack(blockItem),
                        blockItem.getBlock().defaultBlockState().setValue(integers, meta));
            }
        }
        return null;
    }

    /** The shipped lang file's per-meta label keys of an id, e.g. {@code item.thaumcraft.itemshard.6}. */
    static Set<Integer> langLabelledMetas(final String idPath) {
        return LANG_META_KEYS.computeIfAbsent(idPath, id -> {
            final Set<Integer> metas = new java.util.HashSet<>();
            try (java.io.InputStream in = ItemMetaAppearance.class
                    .getResourceAsStream("/assets/" + ThaumcraftClassic.MOD_ID + "/lang/en_us.json")) {
                if (in == null) {
                    return metas;
                }
                final com.google.gson.JsonObject lang = com.google.gson.JsonParser
                        .parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                        .getAsJsonObject();
                for (final Map.Entry<String, com.google.gson.JsonElement> entry : lang.entrySet()) {
                    for (final String prefix : new String[] {"item." + ThaumcraftClassic.MOD_ID + "." + id + ".",
                            "block." + ThaumcraftClassic.MOD_ID + "." + id + "."}) {
                        if (entry.getKey().startsWith(prefix)) {
                            final String tail = entry.getKey().substring(prefix.length());
                            if (!tail.isEmpty() && tail.chars().allMatch(Character::isDigit)
                                    && entry.getValue().getAsString().isEmpty() == false) {
                                metas.add(Integer.parseInt(tail));
                            }
                        }
                    }
                }
            } catch (java.io.IOException | RuntimeException unreadable) {
                ThaumcraftClassic.LOGGER.warn("[BD-17] the shipped lang file is unreadable; the creative tab "
                        + "falls back to the table rows for '{}'", id, unreadable);
            }
            return metas;
        });
    }

    private static boolean contains(final int[] values, final int value) {
        return java.util.Arrays.stream(values).anyMatch(v -> v == value);
    }

    private static final Map<String, Set<Integer>> LANG_META_KEYS = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ the model path

    /** Where a family meta's generated model lives, as a model id. */
    public static ResourceLocation modelPath(final String idPath, final int meta) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "itemmeta/" + idPath + "_" + meta);
    }

    // ------------------------------------------------------------------ the mirror

    /**
     * Record a metadata on a stack: the authoritative
     * {@code thaumcraft:item_meta} (D-018) and the model key
     * {@code minecraft:custom_model_data} the {@code overrides} arrays match,
     * in one call, so a label and a sprite can never disagree.
     */
    public static ItemStack mirror(final ItemStack stack, final int meta) {
        stack.set(ModDataComponents.ITEM_META.get(), meta);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(meta));
        return stack;
    }

    /** The appearance a stack selects: the mirror, falling back to the meta. */
    public static int appearanceValue(final ItemStack stack) {
        final CustomModelData mirror = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (mirror != null) {
            return mirror.value();
        }
        final Integer meta = stack.get(ModDataComponents.ITEM_META.get());
        return meta == null ? 0 : meta;
    }

    // ------------------------------------------------------------------ table builders

    private static MetaArt flat(final String texture, final int meta) {
        return new MetaArt(meta, -1, List.of("thaumcraft:item/" + texture));
    }

    /** A two-layer flat stack: layer0 under layer1 (the jar label). */
    private static MetaArt two(final String lower, final String overlay, final int meta) {
        return new MetaArt(meta, -1, List.of("thaumcraft:item/" + lower, "thaumcraft:item/" + overlay));
    }

    private static MetaArt face(final String texture, final int meta) {
        return new MetaArt(meta, -1, List.of("thaumcraft:" + texture));
    }
}
