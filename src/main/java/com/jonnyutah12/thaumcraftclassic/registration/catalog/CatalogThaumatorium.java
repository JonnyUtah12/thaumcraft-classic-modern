package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory;
import com.jonnyutah12.thaumcraftclassic.menus.ThaumatoriumMenu;
import com.jonnyutah12.thaumcraftclassic.tiles.TileThaumatorium;
import com.jonnyutah12.thaumcraftclassic.tiles.TileThaumatoriumTop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * TC-M7-04-m2c catalog holder: the thaumatorium window — the base and top
 * block entities, their (block, variant) bindings on {@code blockmetaldevice},
 * and the {@code gui_thaumatorium} menu type.
 *
 * <p>Owns exactly the content-mapping.csv rows this card flips on the GUI
 * side: the tile-entity rows {@code thaumcraft:tilethaumatorium} (metas 10)
 * and {@code thaumcraft:tilethaumatoriumtop} (metas 11) — the original has no
 * block classes for them, {@code BlockMetalDevice.createTileEntity} hands the
 * metas their tile classes (BlockMetalDevice.java:366-370) — and
 * {@code thaumcraft:gui_thaumatorium}, the menu type whose screen is wired in
 * {@code client/ModClientScreens}. The host block is looked up by id, the
 * {@link CatalogCrucible} pattern: TC-M6-04c owns {@code blockmetaldevice},
 * and feature cards never edit the device block classes
 * (docs/catalog-triage.md hotspot rule).</p>
 *
 * <p>Out of this holder by design: the thaumatorium machine tick loop, the
 * essentia transport wire-up and the Mnemonic Matrix (the tile rows' machine
 * halves — BD-19), and the Thaumonomicon entry that unlocks it (TC-M3-04).</p>
 */
public final class CatalogThaumatorium {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ThaumcraftClassic.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileThaumatorium>> TILE_THAUMATORIUM =
            BLOCK_ENTITY_TYPES.register("tilethaumatorium",
                    () -> BlockEntityType.Builder.of(TileThaumatorium::new,
                            metalDevice()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileThaumatoriumTop>> TILE_THAUMATORIUM_TOP =
            BLOCK_ENTITY_TYPES.register("tilethaumatoriumtop",
                    () -> BlockEntityType.Builder.of(TileThaumatoriumTop::new,
                            metalDevice()).build(null));

    /**
     * {@code gui_thaumatorium} — named after the csv row it closes. The
     * client-side factory reads the block pos, the same wire shape
     * {@link CatalogArcaneWorkbench}'s menu type uses.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<ThaumatoriumMenu>> MENU_THAUMATORIUM =
            MENUS.register("gui_thaumatorium", () -> IMenuTypeExtension.create(
                    (final int windowId, final Inventory inv, final RegistryFriendlyByteBuf buf) ->
                            new ThaumatoriumMenu(windowId, inv,
                                    (TileThaumatorium) inv.player.level().getBlockEntity(buf.readBlockPos()))));

    /** The host block, looked up by id (TC-M6-04c owns {@code blockmetaldevice}). */
    private static Block metalDevice() {
        return BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blockmetaldevice"));
    }

    /** One wiring line from ModRegistration.init (catalog-triage pattern). */
    public static void init(final IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
        MENUS.register(bus);
        bus.addListener((final RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                wireDeviceTiles();
            }
        });
    }

    /** The (block, variant) → tile bindings, per the original's createTileEntity. */
    private static void wireDeviceTiles() {
        final Block metal = metalDevice();
        if (metal == Blocks.AIR) {
            return; // blockmetaldevice not registered (TC-M6-04c unwired)
        }
        // blockMetalDevice 10 = thaumatorium, 11 = its top: the original's
        // createTileEntity metadata 10/11 (BlockMetalDevice.java:366-370).
        DeviceTileFactory.register(metal, 10, TileThaumatorium::new);
        DeviceTileFactory.register(metal, 11, TileThaumatoriumTop::new);
    }

    private CatalogThaumatorium() {
    }
}
