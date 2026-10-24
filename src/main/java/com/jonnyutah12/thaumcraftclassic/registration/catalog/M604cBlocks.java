package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockAiry;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockAlchemyFurnace;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockMetalDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockStoneDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockWoodenDevice;
import com.jonnyutah12.thaumcraftclassic.menus.MenuAlchemyFurnace;
import com.jonnyutah12.thaumcraftclassic.menus.MenuArcaneBore;
import com.jonnyutah12.thaumcraftclassic.tiles.TileAlchemyFurnace;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneBore;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M6-04c catalog holder: the six shared multi-variant device blocks
 * (content-mapping.csv rows {@code blockstonedevice}, {@code blockmetaldevice},
 * {@code blockwoodendevice}, {@code blockarcanefurnace},
 * {@code blockalchemyfurnace}, {@code blockairy}) with their block items,
 * plus the two GUI rows this card owns ({@code gui_alchemy_furnace},
 * {@code gui_arcane_bore} — realized as menu types; screens are TC-M7-01).
 *
 * <p>Per the catalog-triage rules this card registers through its OWN holder
 * ({@link #init} is the single wiring line in {@code ModRegistration.init});
 * nothing here edits a sibling holder. The block classes carry the original
 * metadata verbatim in a {@code variant} property (D-001); the meta-to-tile
 * map lives in {@link M604cBlockEntities} for this card's tiles and in the
 * owning feature cards' holders for foreign tiles (docs/catalog-triage.md
 * shared multi-meta block map).</p>
 *
 * <p>Block items are plain {@link BlockItem}s: the original's
 * {@code Block*DeviceItem} classes were creative-inventory meta pickers, and
 * in 1.21.1 the variant rides the vanilla {@code minecraft:block_state}
 * component (docs/architecture/registration.md ITEM_META convention; the
 * per-variant creative-tab entries and models are TC-M7-01).</p>
 */
public final class M604cBlocks {

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU,
                    ThaumcraftClassic.MOD_ID);

    // --- blocks (ids and meta ranges verbatim from the original) --------------

    public static final DeferredBlock<BlockStoneDevice> BLOCK_STONE_DEVICE =
            BLOCKS.register("blockstonedevice", BlockStoneDevice::new);
    public static final DeferredBlock<BlockMetalDevice> BLOCK_METAL_DEVICE =
            BLOCKS.register("blockmetaldevice", BlockMetalDevice::new);
    public static final DeferredBlock<BlockWoodenDevice> BLOCK_WOODEN_DEVICE =
            BLOCKS.register("blockwoodendevice", BlockWoodenDevice::new);
    public static final DeferredBlock<BlockArcaneFurnace> BLOCK_ARCANE_FURNACE =
            BLOCKS.register("blockarcanefurnace", BlockArcaneFurnace::new);
    public static final DeferredBlock<BlockAlchemyFurnace> BLOCK_ALCHEMY_FURNACE =
            BLOCKS.register("blockalchemyfurnace", BlockAlchemyFurnace::new);
    public static final DeferredBlock<BlockAiry> BLOCK_AIRY =
            BLOCKS.register("blockairy", BlockAiry::new);

    // --- block items (same ids as the blocks; variant via block_state) -------

    public static final DeferredItem<BlockItem> ITEM_STONE_DEVICE =
            ITEMS.register("blockstonedevice",
                    () -> new BlockItem(BLOCK_STONE_DEVICE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ITEM_METAL_DEVICE =
            ITEMS.register("blockmetaldevice",
                    () -> new BlockItem(BLOCK_METAL_DEVICE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ITEM_WOODEN_DEVICE =
            ITEMS.register("blockwoodendevice",
                    () -> new BlockItem(BLOCK_WOODEN_DEVICE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ITEM_ARCANE_FURNACE =
            ITEMS.register("blockarcanefurnace",
                    () -> new BlockItem(BLOCK_ARCANE_FURNACE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ITEM_ALCHEMY_FURNACE =
            ITEMS.register("blockalchemyfurnace",
                    () -> new BlockItem(BLOCK_ALCHEMY_FURNACE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ITEM_AIRY =
            ITEMS.register("blockairy",
                    () -> new BlockItem(BLOCK_AIRY.get(), new Item.Properties()));

    // --- GUI rows (menus; screens deferred to TC-M7-01) -----------------------

    public static final DeferredHolder<MenuType<?>, MenuType<MenuAlchemyFurnace>> GUI_ALCHEMY_FURNACE =
            MENUS.register("gui_alchemy_furnace", () -> new MenuType<>(
                    (IContainerFactory<MenuAlchemyFurnace>) (id, inv, buf) ->
                            new MenuAlchemyFurnace(id, inv, (TileAlchemyFurnace) requireBlockEntity(
                                    inv, buf.readBlockPos(), TileAlchemyFurnace.class))));
    public static final DeferredHolder<MenuType<?>, MenuType<MenuArcaneBore>> GUI_ARCANE_BORE =
            MENUS.register("gui_arcane_bore", () -> new MenuType<>(
                    (IContainerFactory<MenuArcaneBore>) (id, inv, buf) ->
                            new MenuArcaneBore(id, inv, (TileArcaneBore) requireBlockEntity(
                                    inv, buf.readBlockPos(), TileArcaneBore.class))));

    private static BlockEntity requireBlockEntity(
            final net.minecraft.world.entity.player.Inventory inv,
            final net.minecraft.core.BlockPos pos, final Class<?> expected) {
        final BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (expected.isInstance(be)) {
            return be;
        }
        throw new IllegalStateException("expected " + expected.getSimpleName() + " at " + pos);
    }

    /** Attach every TC-M6-04c holder to the mod bus (single wiring line). */
    public static void init(final IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
    }

    private M604cBlocks() {
    }
}
