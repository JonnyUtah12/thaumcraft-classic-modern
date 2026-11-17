package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTube;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTubeItem;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCentrifuge;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEssentiaCrystalizer;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTube;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeBuffer;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeFilter;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeOneway;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeRestrict;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeValve;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M4-07-s: the essentia transport network — the tube block
 * ({@code thaumcraft:blocktube}, the original's metadata 0..7 on a
 * {@code variant} property) and the six conduit tile entities it hosts.
 *
 * <p>One {@code Catalog<Name>} unit per the registration architecture
 * (docs/architecture/registration.md): its own {@code DeferredRegister}s, a
 * single {@code init(IEventBus)} line in
 * {@link com.jonnyutah12.thaumcraftclassic.registration.ModRegistration}, and
 * the ids exactly as docs/content-mapping.csv lists them — {@code blocktube}
 * and {@code tiletube}, {@code tiletubevalve}, {@code tiletubefilter},
 * {@code tiletubebuffer}, {@code tiletuberestrict}, {@code tiletubeoneway}.</p>
 *
 * <p>The original's metadata 2 (Alchemical Centrifuge, {@code tilecentrifuge})
 * and metadata 7 (Essentia Crystallizer, {@code tileessentiacrystalizer}) are
 * <em>machines</em> hosted on this block, not conduits. They landed with their
 * own card (TC-M4-07-m): their ids are registered here as the two extra block
 * entities, and {@link BlockTube} maps their variants to
 * {@link TileCentrifuge} / {@link TileEssentiaCrystalizer} exactly as the
 * original's {@code createTileEntity} did.</p>
 */
public final class CatalogEssentiaTransport {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    // --- block + item ------------------------------------------------------

    /** thaumcraft:blocktube — metadata 0..7 on the {@code variant} property. */
    public static final DeferredBlock<BlockTube> BLOCK_TUBE =
            BLOCKS.register("blocktube", BlockTube::new);

    public static final DeferredItem<BlockTubeItem> ITEM_TUBE =
            ITEMS.register("blocktube", () -> new BlockTubeItem(BLOCK_TUBE.get(), new Item.Properties()));

    // --- block entities (the six conduits) ---------------------------------

    @SuppressWarnings("deprecation")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTube>> TILE_TUBE =
            BLOCK_ENTITY_TYPES.register("tiletube",
                    () -> BlockEntityType.Builder.of(CatalogEssentiaTransport::createTube, BLOCK_TUBE.get())
                            .build(null));

    @SuppressWarnings("deprecation")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeValve>> TILE_TUBE_VALVE =
            BLOCK_ENTITY_TYPES.register("tiletubevalve",
                    () -> BlockEntityType.Builder.of(TileTubeValve::new, BLOCK_TUBE.get()).build(null));

    @SuppressWarnings("deprecation")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeFilter>> TILE_TUBE_FILTER =
            BLOCK_ENTITY_TYPES.register("tiletubefilter",
                    () -> BlockEntityType.Builder.of(TileTubeFilter::new, BLOCK_TUBE.get()).build(null));

    @SuppressWarnings("deprecation")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeBuffer>> TILE_TUBE_BUFFER =
            BLOCK_ENTITY_TYPES.register("tiletubebuffer",
                    () -> BlockEntityType.Builder.of(TileTubeBuffer::new, BLOCK_TUBE.get()).build(null));

    @SuppressWarnings("deprecation")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeRestrict>> TILE_TUBE_RESTRICT =
            BLOCK_ENTITY_TYPES.register("tiletuberestrict",
                    () -> BlockEntityType.Builder.of(TileTubeRestrict::new, BLOCK_TUBE.get()).build(null));

    @SuppressWarnings("deprecation")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeOneway>> TILE_TUBE_ONEWAY =
            BLOCK_ENTITY_TYPES.register("tiletubeoneway",
                    () -> BlockEntityType.Builder.of(TileTubeOneway::new, BLOCK_TUBE.get()).build(null));

    // --- block entities (the two machines, TC-M4-07-m) ----------------------

    @SuppressWarnings("deprecation")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCentrifuge>> TILE_CENTRIFUGE =
            BLOCK_ENTITY_TYPES.register("tilecentrifuge",
                    () -> BlockEntityType.Builder.of(TileCentrifuge::new, BLOCK_TUBE.get()).build(null));

    @SuppressWarnings("deprecation")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEssentiaCrystalizer>> TILE_CRYSTALIZER =
            BLOCK_ENTITY_TYPES.register("tileessentiacrystalizer",
                    () -> BlockEntityType.Builder.of(TileEssentiaCrystalizer::new, BLOCK_TUBE.get()).build(null));

    private CatalogEssentiaTransport() {
    }

    public static void init(final IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
    }

    // --- tile factories (the block's newBlockEntity seam) ------------------

    public static TileTube createTube(final BlockPos pos, final BlockState state) {
        return new TileTube(TILE_TUBE.get(), pos, state);
    }

    public static TileTubeValve createValve(final BlockPos pos, final BlockState state) {
        return new TileTubeValve(pos, state);
    }

    public static TileTubeFilter createFilter(final BlockPos pos, final BlockState state) {
        return new TileTubeFilter(pos, state);
    }

    public static TileTubeBuffer createBuffer(final BlockPos pos, final BlockState state) {
        return new TileTubeBuffer(pos, state);
    }

    public static TileTubeRestrict createRestrict(final BlockPos pos, final BlockState state) {
        return new TileTubeRestrict(pos, state);
    }

    public static TileTubeOneway createOneway(final BlockPos pos, final BlockState state) {
        return new TileTubeOneway(pos, state);
    }

    public static TileCentrifuge createCentrifuge(final BlockPos pos, final BlockState state) {
        return new TileCentrifuge(pos, state);
    }

    public static TileEssentiaCrystalizer createCrystalizer(final BlockPos pos, final BlockState state) {
        return new TileEssentiaCrystalizer(pos, state);
    }

    /** The block state for one of the original's metadata values. */
    public static BlockState state(final int variant) {
        return BLOCK_TUBE.get().defaultBlockState().setValue(BlockTube.VARIANT, variant);
    }
}
