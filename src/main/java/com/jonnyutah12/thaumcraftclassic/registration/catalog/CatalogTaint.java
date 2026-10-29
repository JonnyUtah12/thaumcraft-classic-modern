package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCustomPlant;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaint;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaintFibres;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantBlockItem;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEtherealBloom;
import com.jonnyutah12.thaumcraftclassic.worldgen.BiomeAura;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M5-04-s catalog holder: the taint content this card owns
 * (content-mapping.csv rows {@code thaumcraft:blockTaint} and
 * {@code thaumcraft:blockTaintFibres}; design record
 * docs/architecture/taint.md).
 *
 * <p>House wiring: one {@code DeferredRegister} pair per registry, an
 * {@code init(final IEventBus)} that registers on the mod bus, and exactly one
 * append-only line in {@code ModRegistration.init} (docs/architecture/
 * registration.md). D-001: ids are the original's lowercased
 * ({@code thaumcraft:blocktaint}, {@code thaumcraft:blocktaintfibres}); the
 * original metadata rides in a {@code variant} property, and the item is the
 * shared {@link VariantBlockItem}, so each variant renders its own original
 * display name (TC-M7-01b's mechanism, the same rows
 * {@code BlockVariantNames} audits).</p>
 *
 * <p>Also in this card, and deliberately not Java: the Tainted Land biome is a
 * datapack entry ({@code data/thaumcraft/worldgen/biome/biome.biomegentaint.json},
 * id {@code thaumcraft:biome.biomegentaint}) that {@code TaintBiomes} writes
 * at runtime, and the config keys
 * ({@code general/biome_taint_spread}, {@code general/biome_taint_from_flux},
 * {@code Monster_Spawning/spawn_taint_spores}) already exist in
 * {@code TcConfigCatalog}, which {@code TaintManager} reads per call.</p>
 *
 * <p>Out of this holder by design, each on its own card: the flux-goo block
 * the crust decays into and the flux-goo → taint seam (the flux resource row,
 * TC-M5-03), the taint creatures the spore branches spawn and the
 * {@code EntityFallingTaint} the crust gravity uses (TC-M6-06), the Taint
 * Poison effect the step poison applies (TC-M6-05), the Ethereal Bloom tile
 * that calls {@code TaintManager.restoreBiome} (TC-M6-05), the biome-modifier
 * hook that places the biome at worldgen with
 * {@code Biomes/taint_biome_weight} (TC-M5-05), the biome's
 * {@code decorateSpecial} blob/fibre/big-tree features (TC-M5-05), the biome's
 * Taintacle spawn entry (TC-M6-06), and every taint texture/model/particle
 * (TC-M5-07). Each seam resolves through the live registries at call time, so
 * none of them needs an edit here.</p>
 */
public final class CatalogTaint {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ThaumcraftClassic.MOD_ID);

    /**
     * Taint crust (0) / soil (1) / flesh (2) / dead taint flowers (3) — the
     * original's metadata verbatim, with the spread, gravity, decay and step
     * poison of {@link TaintManager}.
     */
    public static final DeferredHolder<Block, BlockTaint> BLOCK_TAINT =
            BLOCKS.register("blocktaint", BlockTaint::new);
    public static final DeferredHolder<Item, BlockItem> ITEM_TAINT =
            ITEMS.register("blocktaint", () -> new VariantBlockItem(
                    BLOCK_TAINT.get(), variantAwareItem()));

    /** Taint fibres (0) through the mature spore stalk (4). */
    public static final DeferredHolder<Block, BlockTaintFibres> BLOCK_TAINT_FIBRES =
            BLOCKS.register("blocktaintfibres", BlockTaintFibres::new);
    public static final DeferredHolder<Item, BlockItem> ITEM_TAINT_FIBRES =
            ITEMS.register("blocktaintfibres", () -> new VariantBlockItem(
                    BLOCK_TAINT_FIBRES.get(), variantAwareItem()));

    /**
     * TC-M5-04-s-b: the thaumic plant family (content-mapping.csv row
     * {@code Thaumcraft:blockCustomPlant}), one id with the original's six
     * metas in a {@code variant} property. Meta 4 is the
     * <b>Ethereal Bloom</b> — the cleansing agent of the taint slice: its tile
     * ({@link TileEtherealBloom}) runs the biome-restore roll over the columns
     * around it. {@code BlockCustomPlant}'s javadoc lists which metas stay on
     * which cards (sapling growth, the three crops, the manashroom effect).
     */
    public static final DeferredHolder<Block, BlockCustomPlant> BLOCK_CUSTOM_PLANT =
            BLOCKS.register("blockcustomplant", () -> new BlockCustomPlant(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(MapColor.PLANT)));
    public static final DeferredHolder<Item, BlockItem> ITEM_CUSTOM_PLANT =
            ITEMS.register("blockcustomplant", () -> new VariantBlockItem(
                    BLOCK_CUSTOM_PLANT.get(), variantAwareItem()));

    /**
     * The bloom's tile. The id is the block's own (the port's convention for
     * tile entities, per docs/architecture/registration.md: "tile entities
     * keyed by their block"); the original registered this same class under the
     * name {@code "TilePurifyTotem"} — a copy-paste in its own
     * {@code ConfigBlocks} (line 396: {@code
     * GameRegistry.registerTileEntity(TileEtherealBloom.class,
     * "TilePurifyTotem")}), which is why the content-mapping.csv row that
     * carries this tile is {@code Thaumcraft:TilePurifyTotem}. Pinned by
     * {@code TaintCleansingParityTest}.
     */
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>>
            BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumcraftClassic.MOD_ID);
    public static final java.util.function.Supplier<
            net.minecraft.world.level.block.entity.BlockEntityType<TileEtherealBloom>> BLOOM =
            BLOCK_ENTITIES.register("blockcustomplant",
                    () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                            .of(TileEtherealBloom::new, BLOCK_CUSTOM_PLANT.get()).build(null));

    private CatalogTaint() {
    }

    /** Item properties for a variant-aware block item (the decorative catalog's shape). */
    private static Item.Properties variantAwareItem() {
        return new Item.Properties();
    }

    public static void init(final IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        // TC-M5-04-s-b: the aura/vis half of the taint slice. The node rules
        // (TileNode.handleTaintNode: the taint-biome repaint on count%50, the
        // TAINTED flip on nextInt(500)) and the node generator's
        // baura ×1.5 / ×1.5 (ThaumcraftWorldGenerator:272-279) are all gated
        // on BiomeAura knowing which biome is "the taint biome" — the original
        // compared against ThaumcraftWorldGenerator.biomeTaint by identity,
        // which was never null in a world with Thaumcraft loaded. Registering
        // the key here is what makes those already-ported rules live, and the
        // taint biome's aura (MAGICAL + WASTELAND → 100 and 80, averaged by
        // BiomeHandler.getBiomeAura) reaches the generator through the
        // is_magical / is_wasteland biome tags shipped with this card.
        BiomeAura.registerTaintBiome(TaintBiomes.TAINT_BIOME);
    }
}
