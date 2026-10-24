package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory;
import com.jonnyutah12.thaumcraftclassic.items.ItemInkwell;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.menus.ResearchTableMenu;
import com.jonnyutah12.thaumcraftclassic.tiles.TileResearchTable;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * TC-M3-04 catalog holder: research table wiring and research-note items.
 *
 * <p>Owns exactly the content-mapping.csv rows {@code itemresearchnotes},
 * {@code iteminkwell}, the {@code tileresearchtable} tile row, and the gui
 * row {@code gui_research_table} (its 1.21.1 realization is the menu
 * registered here; the screen is a client concern). The {@code blocktable}
 * block itself belongs to TC-M6-04d ({@link M604dFurniture}); this card only
 * supplies the research-table tile suppliers for variants 2-5 through
 * {@link DeviceTileFactory}, as the BlockTable javadoc prescribes.</p>
 */
public final class M3ResearchContent {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.createBlockEntities(ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ThaumcraftClassic.MOD_ID);

    /** Research notes (original {@code ItemResearchNotes}: unstackable). */
    public static final DeferredItem<ItemResearchNotes> ITEM_RESEARCH_NOTES =
            ITEMS.register("itemresearchnotes",
                    () -> new ItemResearchNotes(new Item.Properties().stacksTo(1)));

    /** Scribing tools / inkwell (original {@code ItemInkwell}: unstackable, max damage 100). */
    public static final DeferredItem<ItemInkwell> ITEM_INKWELL =
            ITEMS.register("iteminkwell",
                    () -> new ItemInkwell(new Item.Properties().stacksTo(1)));

    /** Research-table tile for blocktable variants 2-5. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileResearchTable>> TILE_RESEARCH_TABLE =
            BLOCK_ENTITY_TYPES.register("tileresearchtable",
                    () -> BlockEntityType.Builder.of(TileResearchTable::new,
                            M604dFurniture.BLOCK_TABLE.get()).build(null));

    /** Research-table menu (original {@code ContainerResearchTable}). */
    public static final DeferredHolder<MenuType<?>, MenuType<ResearchTableMenu>> MENU_RESEARCH_TABLE =
            MENUS.register("gui_research_table", () -> new MenuType<>(
                    (net.neoforged.neoforge.network.IContainerFactory<ResearchTableMenu>)
                            (id, inv, buf) -> new ResearchTableMenu(id, inv,
                                    (TileResearchTable) inv.player.level().getBlockEntity(buf.readBlockPos()))));

    private M3ResearchContent() {
    }

    private static void wireDeviceTiles() {
        for (int v = 2; v <= 5; v++) {
            DeviceTileFactory.register(M604dFurniture.BLOCK_TABLE.get(), v, TileResearchTable::new);
        }
    }

    /** One wiring line from ModRegistration.init (catalog-triage pattern). */
    public static void register(final IEventBus bus) {
        ITEMS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        MENUS.register(bus);
        bus.addListener((final RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                wireDeviceTiles();
            }
        });
    }
}
