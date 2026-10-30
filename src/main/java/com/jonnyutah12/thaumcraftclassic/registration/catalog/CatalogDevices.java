package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockAiry;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockAiryItem;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockMetalDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockMetalDeviceItem;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockStoneDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockStoneDeviceItem;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockWoodenDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockWoodenDeviceItem;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory;
import com.jonnyutah12.thaumcraftclassic.menus.ArcaneBoreMenu;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneBore;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneBoreBase;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneLamp;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneLampFertility;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneLampGrowth;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcanePressurePlate;
import com.jonnyutah12.thaumcraftclassic.tiles.TileBellows;
import com.jonnyutah12.thaumcraftclassic.tiles.TileNitor;
import com.jonnyutah12.thaumcraftclassic.tiles.TileOwned;
import com.jonnyutah12.thaumcraftclassic.tiles.TileSensor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * TC-M6-04c-s device-block catalog: the four shared multi-variant device
 * blocks (content-mapping.csv rows {@code blockstonedevice},
 * {@code blockmetaldevice}, {@code blockwoodendevice}, {@code blockairy})
 * with their block items, the ten tile entities this reduced-scope card owns
 * ({@code tilebellows}, {@code tilesensor}, {@code tilearcanepressureplate},
 * {@code tilearcanebore}, {@code tilearcaneborebase}, {@code tilearcanelamp},
 * {@code tilearcanelampgrowth}, {@code tilearcanelampfertility},
 * {@code tilenitor}, {@code tileowned}), and the single place this card's
 * (block, variant) → tile suppliers are registered into
 * {@link DeviceTileFactory}.
 *
 * <p>Scope here is the card's shipping-sized slice: device blocks plus their
 * standalone machines, WITHOUT their GUIs ({@code gui_alchemy_furnace},
 * {@code gui_arcane_bore} are TC-M7-04) and WITHOUT the machines that need
 * essentia transport — the alchemical/infernal furnace machines
 * ({@code blockalchemyfurnace}, {@code blockarcanefurnace},
 * {@code tilealchemyfurnace*}, {@code tilearcanefurnace*}, the basic
 * {@code tilealchemyfurnace} on blockStoneDevice meta 0) and their
 * {@code smelting}/{@code smelting_bonus} recipe rows stay on the follow-up
 * card, so those metas have no supplier yet (the factory returns no block
 * entity, matching the originals' {@code createTileEntity} returning null).</p>
 *
 * <p>Per docs/catalog-triage.md rule 3 this card registers through its OWN
 * holder and appends exactly one wiring line in {@code ModRegistration.init};
 * foreign metas are supplied by their owning cards through the same factory
 * (crucible/alembic/charger/thaumatorium/grate/brainbox/vis-relay for metal,
 * pedestal/infusion/wand-pedestal/stabilizer/transducer/spa/focal-
 * manipulator/flux-scrubber for stone, banner for wooden, node/warding-fence
 * for airy). The block classes carry the original metadata verbatim in a
 * {@code variant} property (D-001).</p>
 *
 * <p>Block items are the original's {@code Block*DeviceItem} classes: thin
 * {@link com.jonnyutah12.thaumcraftclassic.blocks.VariantBlockItem} subclasses
 * registered under the block id. The original classes were creative-inventory
 * meta pickers whose names came from the {@code tile.<id>.<meta>.name} keys;
 * in 1.21.1 the variant rides the vanilla {@code minecraft:block_state}
 * component (docs/architecture/registration.md ITEM_META convention, see
 * {@link VariantStateItems}) and {@code VariantBlockItem} resolves the
 * per-variant label from it (TC-M7-01b). The per-variant creative-tab entries
 * and models are TC-M7-01.</p>
 */
public final class CatalogDevices {

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    /**
     * TC-M7-04-m1: the arcane bore window. The original opened it as
     * {@code gui_id} 15 ({@code GuiArcaneBore}, ClientProxy:381); the port ids
     * GUI menu types by their content-mapping gui id, the convention the
     * furniture catalog set with {@code gui_decon_table}/{@code gui_spa}.
     */
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ThaumcraftClassic.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneBoreMenu>> MENU_ARCANE_BORE =
            MENUS.register("gui_arcane_bore", () -> IMenuTypeExtension.create(
                    (windowId, inventory, buf) -> new ArcaneBoreMenu(
                            windowId, inventory,
                            (TileArcaneBore) inventory.player.level()
                                    .getBlockEntity(buf.readBlockPos()))));

    // --- device blocks (ids and meta ranges verbatim from the original) -----

    public static final DeferredBlock<BlockStoneDevice> BLOCK_STONE_DEVICE =
            BLOCKS.register("blockstonedevice", BlockStoneDevice::new);
    public static final DeferredBlock<BlockMetalDevice> BLOCK_METAL_DEVICE =
            BLOCKS.register("blockmetaldevice", BlockMetalDevice::new);
    public static final DeferredBlock<BlockWoodenDevice> BLOCK_WOODEN_DEVICE =
            BLOCKS.register("blockwoodendevice", BlockWoodenDevice::new);
    public static final DeferredBlock<BlockAiry> BLOCK_AIRY =
            BLOCKS.register("blockairy", BlockAiry::new);

    // --- block items (same ids as the blocks; variant via block_state) ------
    // TC-M7-01b: the named per-card classes are VariantBlockItem subclasses, so
    // a stack carrying a variant renders its own tile.<id>.<meta>.name label
    // instead of the family's meta-0 label.

    public static final DeferredItem<BlockStoneDeviceItem> ITEM_STONE_DEVICE =
            ITEMS.register("blockstonedevice",
                    () -> new BlockStoneDeviceItem(BLOCK_STONE_DEVICE.get(), new Item.Properties()));
    public static final DeferredItem<BlockMetalDeviceItem> ITEM_METAL_DEVICE =
            ITEMS.register("blockmetaldevice",
                    () -> new BlockMetalDeviceItem(BLOCK_METAL_DEVICE.get(), new Item.Properties()));
    public static final DeferredItem<BlockWoodenDeviceItem> ITEM_WOODEN_DEVICE =
            ITEMS.register("blockwoodendevice",
                    () -> new BlockWoodenDeviceItem(BLOCK_WOODEN_DEVICE.get(), new Item.Properties()));
    public static final DeferredItem<BlockAiryItem> ITEM_AIRY =
            ITEMS.register("blockairy",
                    () -> new BlockAiryItem(BLOCK_AIRY.get(), new Item.Properties()));

    // --- standalone wooden devices (blockWoodenDevice 0/1/2/3/4/5) ----------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileBellows>> TILE_BELLOWS =
            BLOCK_ENTITY_TYPES.register("tilebellows",
                    () -> BlockEntityType.Builder.of(TileBellows::new,
                            BLOCK_WOODEN_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSensor>> TILE_SENSOR =
            BLOCK_ENTITY_TYPES.register("tilesensor",
                    () -> BlockEntityType.Builder.of(TileSensor::new,
                            BLOCK_WOODEN_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcanePressurePlate>> TILE_ARCANE_PRESSURE_PLATE =
            BLOCK_ENTITY_TYPES.register("tilearcanepressureplate",
                    () -> BlockEntityType.Builder.of(TileArcanePressurePlate::new,
                            BLOCK_WOODEN_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneBoreBase>> TILE_ARCANE_BORE_BASE =
            BLOCK_ENTITY_TYPES.register("tilearcaneborebase",
                    () -> BlockEntityType.Builder.of(TileArcaneBoreBase::new,
                            BLOCK_WOODEN_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneBore>> TILE_ARCANE_BORE =
            BLOCK_ENTITY_TYPES.register("tilearcanebore",
                    () -> BlockEntityType.Builder.of(TileArcaneBore::new,
                            BLOCK_WOODEN_DEVICE.get()).build(null));

    // --- arcane lamps (blockMetalDevice 7/8/13) -----------------------------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneLamp>> TILE_ARCANE_LAMP =
            BLOCK_ENTITY_TYPES.register("tilearcanelamp",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new TileArcaneLamp(arcaneLampType(), pos, state),
                            BLOCK_METAL_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneLampGrowth>> TILE_ARCANE_LAMP_GROWTH =
            BLOCK_ENTITY_TYPES.register("tilearcanelampgrowth",
                    () -> BlockEntityType.Builder.of(TileArcaneLampGrowth::new,
                            BLOCK_METAL_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneLampFertility>> TILE_ARCANE_LAMP_FERTILITY =
            BLOCK_ENTITY_TYPES.register("tilearcanelampfertility",
                    () -> BlockEntityType.Builder.of(TileArcaneLampFertility::new,
                            BLOCK_METAL_DEVICE.get()).build(null));

    // --- nitor (blockAiry 1) -----------------------------------------------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileNitor>> TILE_NITOR =
            BLOCK_ENTITY_TYPES.register("tilenitor",
                    () -> BlockEntityType.Builder.of(TileNitor::new,
                            BLOCK_AIRY.get()).build(null));

    // --- shared ownership base (id parity; never placed directly) -----------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileOwned>> TILE_OWNED =
            BLOCK_ENTITY_TYPES.register("tileowned",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new TileOwned(ownedType(), pos, state))
                            .build(null));

    /**
     * Attach this card's holders to the mod bus (the single wiring line in
     * {@code ModRegistration.init}) and wire the device-tile suppliers into
     * {@link DeviceTileFactory} when the block registry flushes — blocks flush before block entities, so the
     * holders resolve in both directions.
     */
    public static void init(final IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        modEventBus.addListener((final RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                wireDeviceTiles();
            }
        });
    }

    /** The binding meta map (docs/catalog-triage.md), per the originals' createTileEntity. */
    private static void wireDeviceTiles() {
        DeviceTileFactory.register(BLOCK_METAL_DEVICE.get(), BlockMetalDevice.VARIANT_LAMP,
                (pos, state) -> new TileArcaneLamp(TILE_ARCANE_LAMP.get(), pos, state));
        DeviceTileFactory.register(BLOCK_METAL_DEVICE.get(), BlockMetalDevice.VARIANT_LAMP_GROWTH,
                TileArcaneLampGrowth::new);
        DeviceTileFactory.register(BLOCK_METAL_DEVICE.get(), BlockMetalDevice.VARIANT_LAMP_FERTILITY,
                TileArcaneLampFertility::new);

        DeviceTileFactory.register(BLOCK_WOODEN_DEVICE.get(), 0, TileBellows::new);
        DeviceTileFactory.register(BLOCK_WOODEN_DEVICE.get(), 1, TileSensor::new);
        DeviceTileFactory.register(BLOCK_WOODEN_DEVICE.get(), 2, TileArcanePressurePlate::new);
        DeviceTileFactory.register(BLOCK_WOODEN_DEVICE.get(), 3, TileArcanePressurePlate::new);
        DeviceTileFactory.register(BLOCK_WOODEN_DEVICE.get(), 4, TileArcaneBoreBase::new);
        DeviceTileFactory.register(BLOCK_WOODEN_DEVICE.get(), 5, TileArcaneBore::new);

        DeviceTileFactory.register(BLOCK_AIRY.get(), BlockAiry.VARIANT_NITOR, TileNitor::new);

        // Deferred to the essentia-transport machine card: blockStoneDevice 0
        // (TileAlchemyFurnace), blockArcaneFurnace 0 + nozzles, and
        // blockAlchemyFurnace 0/1 — those blocks/rows are not registered here.
    }

    /** Breaks the static-field self-reference in the tile-entity builders below. */
    private static BlockEntityType<?> ownedType() {
        return TILE_OWNED.get();
    }

    private static BlockEntityType<?> arcaneLampType() {
        return TILE_ARCANE_LAMP.get();
    }

    private CatalogDevices() {
    }
}
