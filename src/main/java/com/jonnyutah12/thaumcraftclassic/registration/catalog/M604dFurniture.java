package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockArcaneDoor;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockChestHungry;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockLifter;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockMagicBox;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockMirror;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTable;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory;
import com.jonnyutah12.thaumcraftclassic.entities.EntityItemGrate;
import com.jonnyutah12.thaumcraftclassic.items.ItemArcaneDoor;
import com.jonnyutah12.thaumcraftclassic.items.ItemKey;
import com.jonnyutah12.thaumcraftclassic.menus.DeconstructionTableMenu;
import com.jonnyutah12.thaumcraftclassic.menus.MagicBoxMenu;
import com.jonnyutah12.thaumcraftclassic.menus.SpaMenu;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneDoor;
import com.jonnyutah12.thaumcraftclassic.tiles.TileBanner;
import com.jonnyutah12.thaumcraftclassic.tiles.TileChestHungry;
import com.jonnyutah12.thaumcraftclassic.tiles.TileDeconstructionTable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileGrate;
import com.jonnyutah12.thaumcraftclassic.tiles.TileLifter;
import com.jonnyutah12.thaumcraftclassic.tiles.TileMagicBox;
import com.jonnyutah12.thaumcraftclassic.tiles.TileMirror;
import com.jonnyutah12.thaumcraftclassic.tiles.TileMirrorEssentia;
import com.jonnyutah12.thaumcraftclassic.tiles.TilePedestal;
import com.jonnyutah12.thaumcraftclassic.tiles.TileSpa;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileWandPedestal;
import com.jonnyutah12.thaumcraftclassic.tiles.TileWardingStone;
import com.jonnyutah12.thaumcraftclassic.tiles.TileWardingStoneFence;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * TC-M6-04d catalog holder: furniture and device blocks (tables, chests,
 * mirrors, doors, pedestals, banners, grates, warding stones, spa, magic box,
 * lifter).
 *
 * <p>Owns exactly the content-mapping.csv rows {@code blocktable},
 * {@code blockchesthungry}, {@code blockmagicbox}, {@code blockmirror},
 * {@code blockarcanedoor}, {@code blocklifter}, {@code itemarcanedoor},
 * {@code arcanedoorkey}, {@code specialitemgrate}, the tile_entity rows of
 * this card, and the gui rows {@code gui_decon_table} / {@code gui_magic_box}
 * / {@code gui_spa} (their 1.21.1 realizations are the menus registered
 * here; {@code gui_chest_hungry} has no custom handler in the original and
 * is realized by the vanilla chest menu). Per the catalog-triage rules this
 * card registers through its OWN holder; nothing here edits a sibling holder
 * or device-block class.</p>
 *
 * <h2>Cross-card seams</h2>
 * Tiles hosted on sibling device blocks (blockStoneDevice 9/11,
 * blockMetalDevice 5/6, blockWoodenDevice 8, blockAiry 4, blockCosmeticSolid
 * 3/15) resolve their host by id when the block registry flushes
 * ({@link RegisterEvent}) and register their {@link DeviceTileFactory}
 * suppliers there; if a sibling block is not registered in this launch the
 * supplier is skipped with a log line, exactly as the factory contract
 * allows. Block-entity validity lists resolve the same way — blocks flush
 * before block entities (ModRegistration ordering).</p>
 */
public final class M604dFurniture {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    // --- blocks ---------------------------------------------------------------

    public static final DeferredBlock<BlockTable> BLOCK_TABLE =
            BLOCKS.register("blocktable", BlockTable::new);
    public static final DeferredBlock<BlockChestHungry> BLOCK_CHEST_HUNGRY =
            BLOCKS.register("blockchesthungry", BlockChestHungry::new);
    public static final DeferredBlock<BlockMagicBox> BLOCK_MAGIC_BOX =
            BLOCKS.register("blockmagicbox", BlockMagicBox::new);
    public static final DeferredBlock<BlockMirror> BLOCK_MIRROR =
            BLOCKS.register("blockmirror", BlockMirror::new);
    public static final DeferredBlock<BlockArcaneDoor> BLOCK_ARCANE_DOOR =
            BLOCKS.register("blockarcanedoor", BlockArcaneDoor::new);
    public static final DeferredBlock<BlockLifter> BLOCK_LIFTER =
            BLOCKS.register("blocklifter", BlockLifter::new);

    // --- items ----------------------------------------------------------------

    public static final DeferredItem<ItemArcaneDoor> ITEM_ARCANE_DOOR =
            ITEMS.register("itemarcanedoor", () -> new ItemArcaneDoor(BLOCK_ARCANE_DOOR.get(), new Item.Properties()));
    public static final DeferredItem<ItemKey> ARCANE_DOOR_KEY =
            ITEMS.register("arcanedoorkey", () -> new ItemKey(new Item.Properties()));

    // --- block entities ---------------------------------------------------------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTable>> TILE_TABLE =
            BLOCK_ENTITY_TYPES.register("tiletable",
                    () -> BlockEntityType.Builder.of(TileTable::new, BLOCK_TABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDeconstructionTable>> TILE_DECONSTRUCTION_TABLE =
            BLOCK_ENTITY_TYPES.register("tiledeconstructiontable",
                    () -> BlockEntityType.Builder.of(TileDeconstructionTable::new, BLOCK_TABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileChestHungry>> TILE_CHEST_HUNGRY =
            BLOCK_ENTITY_TYPES.register("tilechesthungry",
                    () -> BlockEntityType.Builder.of(TileChestHungry::new, BLOCK_CHEST_HUNGRY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileMagicBox>> TILE_MAGIC_BOX =
            BLOCK_ENTITY_TYPES.register("tilemagicbox",
                    () -> BlockEntityType.Builder.of(TileMagicBox::new, BLOCK_MAGIC_BOX.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileMirror>> TILE_MIRROR =
            BLOCK_ENTITY_TYPES.register("tilemirror",
                    () -> BlockEntityType.Builder.of(TileMirror::new, BLOCK_MIRROR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileMirrorEssentia>> TILE_MIRROR_ESSENTIA =
            BLOCK_ENTITY_TYPES.register("tilemirroressentia",
                    () -> BlockEntityType.Builder.of(TileMirrorEssentia::new, BLOCK_MIRROR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneDoor>> TILE_ARCANE_DOOR =
            BLOCK_ENTITY_TYPES.register("tilearcanedoor",
                    () -> BlockEntityType.Builder.of(TileArcaneDoor::new, BLOCK_ARCANE_DOOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileLifter>> TILE_LIFTER =
            BLOCK_ENTITY_TYPES.register("tilelifter",
                    () -> BlockEntityType.Builder.of(TileLifter::new, BLOCK_LIFTER.get()).build(null));

    // Device-host tiles: valid blocks resolved from the registry (blocks flush
    // before block entities — ModRegistration ordering).
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TilePedestal>> TILE_PEDESTAL =
            BLOCK_ENTITY_TYPES.register("tilepedestal",
                    () -> BlockEntityType.Builder.of(TilePedestal::new, blockById("blockcosmeticsolid"),
                            // TC-M4-04: the arcane pedestal (blockStoneDevice 1) is the
                            // infusion altar's central + component pedestals.
                            blockById("blockstonedevice")).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWandPedestal>> TILE_WAND_PEDESTAL =
            BLOCK_ENTITY_TYPES.register("tilewandpedestal",
                    () -> BlockEntityType.Builder.of(TileWandPedestal::new, blockById("blockstonedevice")).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSpa>> TILE_SPA =
            BLOCK_ENTITY_TYPES.register("tilespa",
                    () -> BlockEntityType.Builder.of(TileSpa::new, blockById("blockstonedevice")).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileBanner>> TILE_BANNER =
            BLOCK_ENTITY_TYPES.register("tilebanner",
                    () -> BlockEntityType.Builder.of(TileBanner::new, blockById("blockwoodendevice")).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileGrate>> TILE_GRATE =
            BLOCK_ENTITY_TYPES.register("tilegrate",
                    () -> BlockEntityType.Builder.of(TileGrate::new, blockById("blockmetaldevice")).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWardingStone>> TILE_WARDING_STONE =
            BLOCK_ENTITY_TYPES.register("tilewardingstone",
                    () -> BlockEntityType.Builder.of(TileWardingStone::new, blockById("blockcosmeticsolid")).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWardingStoneFence>> TILE_WARDING_STONE_FENCE =
            BLOCK_ENTITY_TYPES.register("tilewardingstonefence",
                    () -> BlockEntityType.Builder.of(TileWardingStoneFence::new, blockById("blockairy")).build(null));

    // --- menus (gui rows) --------------------------------------------------------

    public static final DeferredHolder<MenuType<?>, MenuType<DeconstructionTableMenu>> MENU_DECON_TABLE =
            MENUS.register("gui_decon_table", () -> new MenuType<>(
                    (net.neoforged.neoforge.network.IContainerFactory<DeconstructionTableMenu>)
                            (id, inv, buf) -> new DeconstructionTableMenu(id, inv,
                                    (TileDeconstructionTable) inv.player.level().getBlockEntity(buf.readBlockPos()))));
    public static final DeferredHolder<MenuType<?>, MenuType<MagicBoxMenu>> MENU_MAGIC_BOX =
            MENUS.register("gui_magic_box", () -> new MenuType<>(
                    (net.neoforged.neoforge.network.IContainerFactory<MagicBoxMenu>)
                            (id, inv, buf) -> new MagicBoxMenu(id, inv,
                                    (TileMagicBox) inv.player.level().getBlockEntity(buf.readBlockPos()))));
    public static final DeferredHolder<MenuType<?>, MenuType<SpaMenu>> MENU_SPA =
            MENUS.register("gui_spa", () -> new MenuType<>(
                    (net.neoforged.neoforge.network.IContainerFactory<SpaMenu>)
                            (id, inv, buf) -> new SpaMenu(id, inv,
                                    (TileSpa) inv.player.level().getBlockEntity(buf.readBlockPos()))));

    // --- entity ------------------------------------------------------------------

    public static final DeferredHolder<EntityType<?>, EntityType<EntityItemGrate>> ENTITY_ITEM_GRATE =
            ENTITY_TYPES.register("specialitemgrate", () -> EntityType.Builder
                    .<EntityItemGrate>of(EntityItemGrate::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                    .build("specialitemgrate"));

    private static net.minecraft.world.level.block.Block blockById(final String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path));
    }

    /**
     * Register the DeviceTileFactory suppliers for the variants this card
     * hosts on sibling device blocks. Runs when the block registry flushes,
     * so every card's blocks are present regardless of holder ordering.
     */
    private static void wireDeviceTiles() {
        final net.minecraft.world.level.block.Block stone = blockById("blockstonedevice");
        final net.minecraft.world.level.block.Block metal = blockById("blockmetaldevice");
        final net.minecraft.world.level.block.Block wooden = blockById("blockwoodendevice");
        final net.minecraft.world.level.block.Block airy = blockById("blockairy");
        final net.minecraft.world.level.block.Block cosmetic = blockById("blockcosmeticsolid");
        // blockStoneDevice: 9 wand recharge pedestal, 11 arcane spa.
        host(stone, 9, "blockstonedevice", TileWandPedestal::new);
        host(stone, 11, "blockstonedevice", TileSpa::new);
        // blockMetalDevice: 5/6 item grate (open/closed).
        host(metal, 5, "blockmetaldevice", TileGrate::new);
        host(metal, 6, "blockmetaldevice", TileGrate::new);
        // blockWoodenDevice: 8 hanging banner.
        host(wooden, 8, "blockwoodendevice", TileBanner::new);
        // blockAiry: 4 warding stone fence post.
        host(airy, 4, "blockairy", TileWardingStoneFence::new);
        // blockCosmeticSolid: 3 warding stone, 15 curios pedestal.
        host(cosmetic, 3, "blockcosmeticsolid", TileWardingStone::new);
        host(cosmetic, 15, "blockcosmeticsolid", TilePedestal::new);
        // Own multi-variant block: table family.
        DeviceTileFactory.register(BLOCK_TABLE.get(), 0, TileTable::new);
        DeviceTileFactory.register(BLOCK_TABLE.get(), 1, TileTable::new);
        for (int v = 6; v <= 13; v++) {
            DeviceTileFactory.register(BLOCK_TABLE.get(), v, TileTable::new);
        }
        DeviceTileFactory.register(BLOCK_TABLE.get(), 14, TileDeconstructionTable::new);
    }

    private static void host(final net.minecraft.world.level.block.Block block, final int variant,
            final String id, final DeviceTileFactory.Supplier supplier) {
        if (block == net.minecraft.world.level.block.Blocks.AIR) {
            org.slf4j.LoggerFactory.getLogger("TC-M6-04d").warn(
                    "thaumcraft:{} is not registered; TC-M6-04d tile for variant {} is inert this launch", id, variant);
            return;
        }
        DeviceTileFactory.register(block, variant, supplier);
    }

    /** One wiring line from ModRegistration.init (catalog-triage pattern). */
    public static void register(final IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        MENUS.register(bus);
        ENTITY_TYPES.register(bus);
        bus.addListener((final RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                wireDeviceTiles();
            }
        });
    }

    private M604dFurniture() {
    }
}
