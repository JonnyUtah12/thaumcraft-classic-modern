package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory;
import com.jonnyutah12.thaumcraftclassic.recipes.SmeltingBonuses;
import com.jonnyutah12.thaumcraftclassic.tiles.TileAlchemyFurnace;
import com.jonnyutah12.thaumcraftclassic.tiles.TileAlchemyFurnaceAdvanced;
import com.jonnyutah12.thaumcraftclassic.tiles.TileAlchemyFurnaceAdvancedNozzle;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneBore;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneBoreBase;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnaceNozzle;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneLamp;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneLampFertility;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneLampGrowth;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcanePressurePlate;
import com.jonnyutah12.thaumcraftclassic.tiles.TileBellows;
import com.jonnyutah12.thaumcraftclassic.tiles.TileNitor;
import com.jonnyutah12.thaumcraftclassic.tiles.TileOwned;
import com.jonnyutah12.thaumcraftclassic.tiles.TileSensor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * TC-M6-04c block-entity holder: the tile entities this card owns
 * (content-mapping.csv tile_entity rows {@code tilearcanefurnace},
 * {@code tilearcanefurnacenozzle}, {@code tilebellows}, {@code tilesensor},
 * {@code tilearcanepressureplate}, {@code tilearcanebore},
 * {@code tilearcaneborebase}, {@code tilearcanelamp},
 * {@code tilearcanelampgrowth}, {@code tilearcanelampfertility},
 * {@code tileowned}, {@code tilenitor}, {@code tilealchemyfurnace},
 * {@code tilealchemyfurnaceadvanced}, {@code tilealchemyfurnaceadvancednozzle}).
 *
 * <p>Also the single place this card's (block, variant) → tile suppliers are
 * registered into {@link DeviceTileFactory} — the binding meta map from
 * docs/catalog-triage.md, matching the originals' {@code createTileEntity}:
 * blockStoneDevice 0; blockMetalDevice 7/8/13; blockWoodenDevice 0/1/2/3/4/5;
 * blockArcaneFurnace 0 + nozzles 2/4/5/6/8; blockAlchemyFurnace 0/1;
 * blockAiry 1. Foreign metas are supplied by their owning cards through the
 * same factory (e.g. TC-M6-04d wires stone 9/11, metal 5/6, wooden 8,
 * airy 4).</p>
 *
 * <p>{@code tileowned} is the original's shared ownership base class; it is
 * registered for id parity with an empty valid-block set (nothing places it
 * directly — TC-M6-04d's door and this card's bore/lamps subclass it).</p>
 */
public final class M604cBlockEntities {

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.createBlockEntities(ThaumcraftClassic.MOD_ID);

    // --- infernal furnace (blockArcaneFurnace 0; nozzles 2/4/5/6/8) ----------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneFurnace>> TILE_ARCANE_FURNACE =
            BLOCK_ENTITY_TYPES.register("tilearcanefurnace",
                    () -> BlockEntityType.Builder.of(TileArcaneFurnace::new,
                            M604cBlocks.BLOCK_ARCANE_FURNACE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneFurnaceNozzle>> TILE_ARCANE_FURNACE_NOZZLE =
            BLOCK_ENTITY_TYPES.register("tilearcanefurnacenozzle",
                    () -> BlockEntityType.Builder.of(TileArcaneFurnaceNozzle::new,
                            M604cBlocks.BLOCK_ARCANE_FURNACE.get()).build(null));

    // --- alchemical furnaces (blockStoneDevice 0; blockAlchemyFurnace 0/1) ----

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAlchemyFurnace>> TILE_ALCHEMY_FURNACE =
            BLOCK_ENTITY_TYPES.register("tilealchemyfurnace",
                    () -> BlockEntityType.Builder.of(TileAlchemyFurnace::new,
                            M604cBlocks.BLOCK_STONE_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAlchemyFurnaceAdvanced>> TILE_ALCHEMY_FURNACE_ADVANCED =
            BLOCK_ENTITY_TYPES.register("tilealchemyfurnaceadvanced",
                    () -> BlockEntityType.Builder.of(TileAlchemyFurnaceAdvanced::new,
                            M604cBlocks.BLOCK_ALCHEMY_FURNACE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAlchemyFurnaceAdvancedNozzle>> TILE_ALCHEMY_FURNACE_ADVANCED_NOZZLE =
            BLOCK_ENTITY_TYPES.register("tilealchemyfurnaceadvancednozzle",
                    () -> BlockEntityType.Builder.of(TileAlchemyFurnaceAdvancedNozzle::new,
                            M604cBlocks.BLOCK_ALCHEMY_FURNACE.get()).build(null));

    // --- wooden devices (blockWoodenDevice 0/1/2/3/4/5) ------------------------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileBellows>> TILE_BELLOWS =
            BLOCK_ENTITY_TYPES.register("tilebellows",
                    () -> BlockEntityType.Builder.of(TileBellows::new,
                            M604cBlocks.BLOCK_WOODEN_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSensor>> TILE_SENSOR =
            BLOCK_ENTITY_TYPES.register("tilesensor",
                    () -> BlockEntityType.Builder.of(TileSensor::new,
                            M604cBlocks.BLOCK_WOODEN_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcanePressurePlate>> TILE_ARCANE_PRESSURE_PLATE =
            BLOCK_ENTITY_TYPES.register("tilearcanepressureplate",
                    () -> BlockEntityType.Builder.of(TileArcanePressurePlate::new,
                            M604cBlocks.BLOCK_WOODEN_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneBoreBase>> TILE_ARCANE_BORE_BASE =
            BLOCK_ENTITY_TYPES.register("tilearcaneborebase",
                    () -> BlockEntityType.Builder.of(TileArcaneBoreBase::new,
                            M604cBlocks.BLOCK_WOODEN_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneBore>> TILE_ARCANE_BORE =
            BLOCK_ENTITY_TYPES.register("tilearcanebore",
                    () -> BlockEntityType.Builder.of(TileArcaneBore::new,
                            M604cBlocks.BLOCK_WOODEN_DEVICE.get()).build(null));

    // --- arcane lamps (blockMetalDevice 7/8/13) --------------------------------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneLamp>> TILE_ARCANE_LAMP =
            BLOCK_ENTITY_TYPES.register("tilearcanelamp",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new TileArcaneLamp(TILE_ARCANE_LAMP.get(), pos, state),
                            M604cBlocks.BLOCK_METAL_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneLampGrowth>> TILE_ARCANE_LAMP_GROWTH =
            BLOCK_ENTITY_TYPES.register("tilearcanelampgrowth",
                    () -> BlockEntityType.Builder.of(TileArcaneLampGrowth::new,
                            M604cBlocks.BLOCK_METAL_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneLampFertility>> TILE_ARCANE_LAMP_FERTILITY =
            BLOCK_ENTITY_TYPES.register("tilearcanelampfertility",
                    () -> BlockEntityType.Builder.of(TileArcaneLampFertility::new,
                            M604cBlocks.BLOCK_METAL_DEVICE.get()).build(null));

    // --- nitor (blockAiry 1) ---------------------------------------------------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileNitor>> TILE_NITOR =
            BLOCK_ENTITY_TYPES.register("tilenitor",
                    () -> BlockEntityType.Builder.of(TileNitor::new,
                            M604cBlocks.BLOCK_AIRY.get()).build(null));

    // --- shared ownership base (id parity; never placed directly) --------------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileOwned>> TILE_OWNED =
            BLOCK_ENTITY_TYPES.register("tileowned",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new TileOwned(TILE_OWNED.get(), pos, state))
                            .build(null));

    /**
     * Attach to the mod bus and wire this card's device-tile suppliers when
     * the block registry flushes (blocks flush before block entities, so the
     * holders resolve in both directions). Also schedules the original's
     * smelting-bonus table (CSV rows 225-242) at common setup, when every
     * item holder is resolved.
     */
    public static void init(final IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener((final RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                wireDeviceTiles();
            }
        });
        modEventBus.addListener((final net.neoforged.neoforge.event.ModCommonSetupEvent event) ->
                event.enqueueWork(SmeltingBonuses::registerOriginalBonuses));
    }

    /** The binding meta map (docs/catalog-triage.md), per the originals' createTileEntity. */
    private static void wireDeviceTiles() {
        DeviceTileFactory.register(M604cBlocks.BLOCK_STONE_DEVICE.get(), 0, TileAlchemyFurnace::new);

        DeviceTileFactory.register(M604cBlocks.BLOCK_METAL_DEVICE.get(), 7,
                (pos, state) -> new TileArcaneLamp(TILE_ARCANE_LAMP.get(), pos, state));
        DeviceTileFactory.register(M604cBlocks.BLOCK_METAL_DEVICE.get(), 8, TileArcaneLampGrowth::new);
        DeviceTileFactory.register(M604cBlocks.BLOCK_METAL_DEVICE.get(), 13, TileArcaneLampFertility::new);

        DeviceTileFactory.register(M604cBlocks.BLOCK_WOODEN_DEVICE.get(), 0, TileBellows::new);
        DeviceTileFactory.register(M604cBlocks.BLOCK_WOODEN_DEVICE.get(), 1, TileSensor::new);
        DeviceTileFactory.register(M604cBlocks.BLOCK_WOODEN_DEVICE.get(), 2, TileArcanePressurePlate::new);
        DeviceTileFactory.register(M604cBlocks.BLOCK_WOODEN_DEVICE.get(), 3, TileArcanePressurePlate::new);
        DeviceTileFactory.register(M604cBlocks.BLOCK_WOODEN_DEVICE.get(), 4, TileArcaneBoreBase::new);
        DeviceTileFactory.register(M604cBlocks.BLOCK_WOODEN_DEVICE.get(), 5, TileArcaneBore::new);

        DeviceTileFactory.register(M604cBlocks.BLOCK_ARCANE_FURNACE.get(), 0, TileArcaneFurnace::new);
        for (final int nozzle : new int[]{2, 4, 5, 6, 8}) {
            DeviceTileFactory.register(M604cBlocks.BLOCK_ARCANE_FURNACE.get(), nozzle,
                    TileArcaneFurnaceNozzle::new);
        }

        DeviceTileFactory.register(M604cBlocks.BLOCK_ALCHEMY_FURNACE.get(), 0, TileAlchemyFurnaceAdvanced::new);
        DeviceTileFactory.register(M604cBlocks.BLOCK_ALCHEMY_FURNACE.get(), 1, TileAlchemyFurnaceAdvancedNozzle::new);

        DeviceTileFactory.register(M604cBlocks.BLOCK_AIRY.get(), 1, TileNitor::new);
    }

    private M604cBlockEntities() {
    }
}
