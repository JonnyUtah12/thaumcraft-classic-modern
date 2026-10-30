package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.menus.DeconstructionTableMenu;
import com.jonnyutah12.thaumcraftclassic.menus.MagicBoxMenu;
import com.jonnyutah12.thaumcraftclassic.menus.SpaMenu;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import com.jonnyutah12.thaumcraftclassic.registration.ModAspects;
import com.jonnyutah12.thaumcraftclassic.registration.TestProbeContent;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileChestHungry;
import com.jonnyutah12.thaumcraftclassic.tiles.TileDeconstructionTable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileMagicBox;
import com.jonnyutah12.thaumcraftclassic.tiles.TileSpa;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M6-04d-s evidence: the card's "registry + inventory persistence test" bar.
 *
 * <ol>
 *   <li>{@link #furnitureRowsResolveInTheirRegistries} — the twenty-six
 *       content-mapping.csv rows this card flipped to done keep resolving in
 *       the registry their kind names (block, item, tile entity, entity,
 *       menu), and every id the card owns appears in the contract.</li>
 *   <li>{@link #furnitureTilesResolveForTheirOwningMetas} — the
 *       (block, variant) → tile suppliers this card registers through
 *       {@code DeviceTileFactory} really produce their tile entity once placed,
 *       and the table metas it deliberately left out (the research-table base
 *       and the arcane workbench, both owned by other cards) produce none.</li>
 *   <li>{@link #storedItemsSurviveASaveLoadRoundTrip} — items stored in the
 *       hungry chest and the magic box survive the vanilla block-entity
 *       serialize/load path, which is what the "inventory persistence" bar
 *       requires: the chest's slots keep their positions, the box keeps its
 *       item list.</li>
 *   <li>{@link #deconstructionTableAspectReachesTheMenuDataSlot},
 *       {@link #spaTankAndMixTravelInMenuDataSlots} and
 *       {@link #magicBoxPagesRebindTheBoundSlots} — TC-M7-04-m4's screen
 *       state: the revealed aspect, the spa tank + mix flag, and the magic
 *       box's page all travel through the menus' container data (BD-17
 *       closure), with the client-side {@code ContainerData#set} landing
 *       proven write-through on each menu.</li>
 * </ol>
 *
 * <p>Needs the {@code gameTestServer} run configuration (build.gradle), which
 * sets {@code -Dtc.contentMappingFile}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CatalogFurnitureGameTest {

    private CatalogFurnitureGameTest() {
    }

    /** Every id this card owns, by the registry its csv kind names. */
    private static List<String> ownedBlockIds() {
        return List.of("blocktable", "blockchesthungry", "blockmagicbox",
                "blockmirror", "blockarcanedoor", "blocklifter");
    }

    private static List<String> ownedItemIds() {
        return List.of("itemarcanedoor", "arcanedoorkey");
    }

    private static List<String> ownedTileIds() {
        return List.of("tiletable", "tiledeconstructiontable", "tilechesthungry",
                "tilemagicbox", "tilemirror", "tilemirroressentia", "tilelifter",
                "tilepedestal", "tilewandpedestal", "tilespa",
                "tilebanner", "tilegrate", "tilewardingstone", "tilewardingstonefence");
    }

    private static List<String> ownedEntityIds() {
        return List.of("specialitemgrate");
    }

    private static List<String> ownedMenuIds() {
        return List.of("gui_decon_table", "gui_magic_box", "gui_spa");
    }

    private static boolean ownsAny(final String name) {
        return ownedBlockIds().contains(name) || ownedItemIds().contains(name)
                || ownedTileIds().contains(name) || ownedEntityIds().contains(name)
                || ownedMenuIds().contains(name);
    }

    private static List<ContentMappingCsv.Row> readContract(final GameTestHelper helper) {
        final String path = System.getProperty("tc.contentMappingFile", "");
        helper.assertTrue(!path.isEmpty() && Files.isRegularFile(Path.of(path)),
                "tc.contentMappingFile is not set or missing — the gameTestServer run config must provide it");
        try {
            return ContentMappingCsv.read(Path.of(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @GameTest(template = "empty")
    public static void furnitureRowsResolveInTheirRegistries(final GameTestHelper helper) {
        var seen = 0;
        for (final ContentMappingCsv.Row row : readContract(helper)) {
            final String namespace = ThaumcraftClassic.MOD_ID + ":";
            if (!row.newId().startsWith(namespace)) {
                continue;
            }
            final String name = row.newId().substring(namespace.length());
            if (!ownsAny(name)) {
                continue;
            }
            helper.assertTrue(row.implementationStatus().equals("done") && row.testStatus().equals("tested"),
                    "card-owned row " + row.newId() + " must be marked done/tested in this commit");
            seen++;
            final ResourceLocation key = ResourceLocation.parse(row.newId());
            switch (row.kind()) {
                case "block":
                    helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(key),
                            "csv row " + row.newId() + " is marked done but does not resolve");
                    // The two rows whose item_class column names a same-id block
                    // item must resolve in the ITEM registry as well.
                    if (name.equals("blocktable") || name.equals("blockmirror")) {
                        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(key),
                                "csv row " + row.newId() + " names an item_class with no same-id item");
                    }
                    break;
                case "item":
                    helper.assertTrue(BuiltInRegistries.ITEM.containsKey(key),
                            "csv row " + row.newId() + " is marked done but its item is not registered");
                    break;
                case "tile_entity":
                    helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(key),
                            "csv row " + row.newId() + " is marked done but its tile entity is not registered");
                    break;
                case "entity":
                    helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(key),
                            "csv row " + row.newId() + " is marked done but its entity type is not registered");
                    break;
                case "gui":
                    helper.assertTrue(BuiltInRegistries.MENU.containsKey(key),
                            "csv row " + row.newId() + " is marked done but its menu type is not registered");
                    break;
                default:
                    helper.assertTrue(false, "unknown csv kind " + row.kind() + " for " + row.newId());
            }
        }
        final int expected = ownedBlockIds().size() + ownedItemIds().size()
                + ownedTileIds().size() + ownedEntityIds().size() + ownedMenuIds().size();
        helper.assertTrue(seen == expected,
                "expected " + expected + " rows owned by this card in the contract, got " + seen);
        helper.succeed();
    }

    private static void expectTile(final GameTestHelper helper, final BlockDeviceBase block,
                                   final int variant, final BlockEntityType<?> expectedType,
                                   final BlockPos pos, final String label) {
        helper.setBlock(pos, block.withVariant(variant));
        final BlockEntity entity = helper.getBlockEntity(pos);
        helper.assertTrue(entity != null, label + " has no block entity at variant " + variant);
        helper.assertTrue(entity.getType() == expectedType,
                label + " at variant " + variant + " created the wrong tile entity");
    }

    private static void expectNoTile(final GameTestHelper helper, final BlockDeviceBase block,
                                     final int variant, final BlockPos pos, final String label) {
        final BlockState state = block.withVariant(variant);
        helper.setBlock(pos, state);
        final BlockEntity entity = block.newBlockEntity(pos, state);
        helper.assertTrue(entity == null,
                label + " at variant " + variant + " is owned by another card and must create no tile here");
    }

    /**
     * One fresh spot per probe: {@code setBlock} over an existing block entity
     * leaves that entity in place, so probing the same coordinate twice would
     * read the previous probe's tile.
     */
    @GameTest(template = "empty")
    public static void furnitureTilesResolveForTheirOwningMetas(final GameTestHelper helper) {
        final var base = new BlockPos(1, 1, 1);
        final var step = 2;
        final BlockTable table = CatalogFurniture.BLOCK_TABLE.get();

        // This card's own multi-variant block: the table family.
        expectTile(helper, table, 0, CatalogFurniture.TILE_TABLE.get(), base, "plain table");
        expectTile(helper, table, 14, CatalogFurniture.TILE_DECONSTRUCTION_TABLE.get(),
                base.offset(0, step, 0), "deconstruction table");
        expectTile(helper, table, 7, CatalogFurniture.TILE_TABLE.get(),
                base.offset(0, 2 * step, 0), "table top half");
        expectNoTile(helper, table, 2, base.offset(0, 3 * step, 0), "research-table base");
        expectNoTile(helper, table, 15, base.offset(1, 0, step), "arcane workbench");

        // The tiles this card hosts on sibling device blocks.
        final BlockDeviceBase stone = (BlockDeviceBase) CatalogDevices.BLOCK_STONE_DEVICE.get();
        final BlockDeviceBase metal = (BlockDeviceBase) CatalogDevices.BLOCK_METAL_DEVICE.get();
        final BlockDeviceBase wooden = (BlockDeviceBase) CatalogDevices.BLOCK_WOODEN_DEVICE.get();
        final BlockDeviceBase airy = (BlockDeviceBase) CatalogDevices.BLOCK_AIRY.get();
        expectTile(helper, stone, 9, CatalogFurniture.TILE_WAND_PEDESTAL.get(),
                new BlockPos(1, 1, 3), "wand pedestal");
        expectTile(helper, stone, 11, CatalogFurniture.TILE_SPA.get(), new BlockPos(3, 1, 3), "arcane spa");
        expectTile(helper, metal, 5, CatalogFurniture.TILE_GRATE.get(), new BlockPos(1, 3, 1), "item grate");
        expectTile(helper, wooden, 8, CatalogFurniture.TILE_BANNER.get(), new BlockPos(3, 3, 1), "banner");
        expectTile(helper, airy, 4, CatalogFurniture.TILE_WARDING_STONE_FENCE.get(),
                new BlockPos(1, 5, 1), "warding stone fence");

        // A standalone block carries its own tile entity on its bottom half.
        helper.setBlock(new BlockPos(3, 5, 1), CatalogFurniture.BLOCK_ARCANE_DOOR.get());
        final BlockEntity door = helper.getBlockEntity(new BlockPos(3, 5, 1));
        helper.assertTrue(door != null, "the arcane door's bottom half hosts no tile entity");
        helper.assertTrue(door.getType() == CatalogFurniture.TILE_ARCANE_DOOR.get(),
                "the arcane door created the wrong tile entity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void storedItemsSurviveASaveLoadRoundTrip(final GameTestHelper helper) {
        final HolderLookup.Provider provider = helper.getLevel().registryAccess();
        final var chestPos = new BlockPos(0, 1, 0);
        final var boxPos = new BlockPos(3, 1, 0);

        // The hungry chest: items must keep their slots across the save path.
        helper.setBlock(chestPos, CatalogFurniture.BLOCK_CHEST_HUNGRY.get());
        final BlockEntity placed = helper.getBlockEntity(chestPos);
        helper.assertTrue(placed instanceof TileChestHungry, "the hungry chest hosts no storage entity");
        final TileChestHungry chest = (TileChestHungry) placed;
        helper.assertTrue(chest.getContainerSize() == 27,
                "the hungry chest is a 27-slot storage chest, got " + chest.getContainerSize());
        final ItemStack first = new ItemStack(TestProbeContent.PROBE_ITEM.get(), 3);
        chest.setItem(0, first.copy());
        chest.setItem(5, new ItemStack(TestProbeContent.PROBE_ITEM.get()));
        // saveWithFullMetadata (not saveWithoutMetadata): the static load path
        // resolves the block-entity type from the tag's "id", which only the
        // full-metadata form writes.
        final CompoundTag chestTag = placed.saveWithFullMetadata(provider);
        final BlockEntity reloaded = BlockEntity.loadStatic(chestPos, helper.getBlockState(chestPos),
                chestTag, provider);
        helper.assertTrue(reloaded instanceof TileChestHungry,
                "the saved chest did not reload as a storage chest");
        final TileChestHungry restored = (TileChestHungry) reloaded;
        helper.assertTrue(ItemStack.isSameItem(restored.getItem(0), first) && restored.getItem(0).getCount() == 3,
                "the chest lost the stack in slot 0 across the save/load round-trip");
        helper.assertTrue(!restored.getItem(5).isEmpty(), "the chest lost the stack in slot 5");
        helper.assertTrue(restored.getItem(1).isEmpty() && restored.getItem(13).isEmpty(),
                "empty chest slots must stay empty");

        // The magic box: a growable item list behind the original's per-box
        // capacity (27 slots for a lone box; linkedBoxes only grows when boxes
        // touch), still every item present across the round-trip.
        helper.setBlock(boxPos, CatalogFurniture.BLOCK_MAGIC_BOX.get());
        final BlockEntity boxEntity = helper.getBlockEntity(boxPos);
        helper.assertTrue(boxEntity instanceof TileMagicBox, "the magic box hosts no storage entity");
        final TileMagicBox box = (TileMagicBox) boxEntity;
        helper.assertTrue(box.getContainerSize() == 27,
                "a lone magic box offers the original 27 slots, got " + box.getContainerSize());
        final ItemStack ring = new ItemStack(TestProbeContent.PROBE_RING.get());
        box.setItem(0, first.copy());
        box.setItem(1, ring.copy());
        helper.assertTrue(box.getItem(0).getCount() + box.getItem(1).getCount() == 4,
                "the box dropped an item on insertion: " + box.getItem(0) + " / " + box.getItem(1));
        final CompoundTag boxTag = boxEntity.saveWithFullMetadata(provider);
        final BlockEntity boxReloaded = BlockEntity.loadStatic(boxPos,
                helper.getBlockState(boxPos), boxTag, provider);
        helper.assertTrue(boxReloaded instanceof TileMagicBox,
                "the saved box did not reload as a magic box");
        final TileMagicBox restoredBox = (TileMagicBox) boxReloaded;
        // The box sorts on insertion, so check membership rather than slots.
        helper.assertTrue(ItemStack.isSameItem(restoredBox.getItem(0), first)
                        || ItemStack.isSameItem(restoredBox.getItem(1), first),
                "the box lost the first stack across the save/load round-trip");
        helper.assertTrue(ItemStack.isSameItem(restoredBox.getItem(0), ring)
                        || ItemStack.isSameItem(restoredBox.getItem(1), ring),
                "the box lost the second stack across the save/load round-trip");
        helper.assertTrue(restoredBox.getItem(0).getCount() + restoredBox.getItem(1).getCount() == 4,
                "the box lost counts across the save/load round-trip: " + restoredBox.getItem(0)
                        + " / " + restoredBox.getItem(1));
        helper.succeed();
    }

    /**
     * TC-M7-04-m4: the deconstruction table's revealed aspect travels in the
     * menu's container data (the 1.7.10 GUI read {@code tableInventory.aspect}
     * off the tile it held; BD-17). The encoding is the aspect's id in the
     * {@code thaumcraft:aspect} registry plus one, 0 = none, and the
     * client-side landing point is {@code ContainerData#set}.
     */
    @GameTest(template = "empty")
    public static void deconstructionTableAspectReachesTheMenuDataSlot(final GameTestHelper helper) {
        // The tile lives on its own block state: BlockEntity validates the
        // state against its type's valid blocks (blockTable variant 14 is the
        // deconstruction table, CatalogFurniture.wireDeviceTiles).
        final BlockPos tablePos = new BlockPos(0, 1, 0);
        helper.setBlock(tablePos,
                CatalogFurniture.BLOCK_TABLE.get().withVariant(BlockTable.VARIANT_DECONSTRUCTION));
        final TileDeconstructionTable table = (TileDeconstructionTable) helper.getBlockEntity(tablePos);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final Inventory inventory = player.getInventory();
        final DeconstructionTableMenu menu = new DeconstructionTableMenu(100, inventory, table);

        final Aspect primal = Aspect.getPrimalAspects().get(0);
        final int expected = ModAspects.registry().getId(primal) + 1;
        helper.assertTrue(expected > 0, "the aspect registry must resolve a live id, got " + expected);
        table.aspect = primal;
        table.breaktime = 20;
        helper.assertTrue(menu.containerData().get(DeconstructionTableMenu.DATA_ASPECT) == expected,
                "the revealed aspect must reach data slot " + DeconstructionTableMenu.DATA_ASPECT
                        + " as its registry id + 1");
        helper.assertTrue(menu.getRevealedAspect() == primal,
                "the screen's getter must expose the revealed aspect");
        helper.assertTrue(menu.containerData().get(DeconstructionTableMenu.DATA_BREAK_TIME) == 20,
                "the break timer keeps its own data slot");
        helper.assertTrue(DeconstructionTableMenu.decodeAspectIndex(
                        DeconstructionTableMenu.encodeAspectIndex(primal)) == primal,
                "encode/decode must round-trip through the registry");
        helper.assertTrue(DeconstructionTableMenu.encodeAspectIndex(null) == 0
                        && DeconstructionTableMenu.decodeAspectIndex(0) == null,
                "no aspect must encode as 0 and decode back to null");

        // The client landing point: data-slot writes drive the tile the screen
        // reads (this is what AbstractContainerMenu does on the client).
        table.aspect = null;
        menu.containerData().set(DeconstructionTableMenu.DATA_ASPECT, expected);
        helper.assertTrue(menu.getRevealedAspect() == primal,
                "a data-slot write (the sync landing) must surface through the getter");
        menu.containerData().set(DeconstructionTableMenu.DATA_ASPECT, 0);
        helper.assertTrue(menu.getRevealedAspect() == null,
                "a cleared data slot must clear the revealed aspect");
        helper.succeed();
    }

    /**
     * TC-M7-04-m4: the spa tank (fluid id, amount, capacity) and the mix flag
     * travel in the menu's container data (the 1.7.10 GUI read {@code spa.tank}
     * and {@code spa.getMix()} off the tile; BD-17), and the mix toggle is the
     * original's menu button 1.
     */
    @GameTest(template = "empty")
    public static void spaTankAndMixTravelInMenuDataSlots(final GameTestHelper helper) {
        // The tile lives on its own block state: BlockEntity validates the
        // state against its type's valid blocks (blockStoneDevice variant 11
        // is the arcane spa, CatalogFurniture.wireDeviceTiles).
        final BlockPos spaPos = new BlockPos(0, 1, 0);
        helper.setBlock(spaPos, CatalogDevices.BLOCK_STONE_DEVICE.get().withVariant(11));
        final TileSpa spa = (TileSpa) helper.getBlockEntity(spaPos);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final SpaMenu menu = new SpaMenu(101, player.getInventory(), spa);

        helper.assertTrue(menu.containerData().get(SpaMenu.DATA_FLUID_ID) == 0,
                "an empty tank encodes as 0");
        helper.assertTrue(menu.containerData().get(SpaMenu.DATA_FLUID_AMOUNT) == 0,
                "an empty tank carries no amount");
        helper.assertTrue(menu.containerData().get(SpaMenu.DATA_CAPACITY) == 5000,
                "the tank keeps the original 5000 mB capacity in its data slot");
        helper.assertTrue(menu.containerData().get(SpaMenu.DATA_MIX) == 1,
                "the original's mix flag starts true (TileSpa: private boolean mix = true)");

        final int filled = spa.fill(Fluids.WATER, 1200, true);
        helper.assertTrue(filled == 1200, "the tank accepts the fill, got " + filled);
        final int waterIdPlusOne = BuiltInRegistries.FLUID.getId(Fluids.WATER) + 1;
        helper.assertTrue(menu.containerData().get(SpaMenu.DATA_FLUID_ID) == waterIdPlusOne,
                "the tank fluid must reach data slot " + SpaMenu.DATA_FLUID_ID
                        + " as its minecraft:fluid registry id + 1");
        helper.assertTrue(menu.containerData().get(SpaMenu.DATA_FLUID_AMOUNT) == 1200,
                "the mB amount travels beside it");
        helper.assertTrue(menu.getTankFluid() == Fluids.WATER && menu.getTankAmount() == 1200,
                "the screen's getters expose the live tank");

        // The original's toggle: menu button 1 flips the mix and the data slot follows.
        helper.assertTrue(menu.clickMenuButton(player, 1), "button 1 must be answered");
        helper.assertTrue(menu.containerData().get(SpaMenu.DATA_MIX) == 0
                && !menu.getMix(), "the mix flag must flip in the data slot");
        menu.clickMenuButton(player, 1);
        helper.assertTrue(menu.containerData().get(SpaMenu.DATA_MIX) == 1
                && menu.getMix(), "and flip back");

        // The client landing point: data-slot writes drive the tile the screen
        // reads (AbstractContainerMenu#broadcastChanges lands every data slot
        // through ContainerData#set on the client).
        spa.drain(1200, true);
        menu.containerData().set(SpaMenu.DATA_FLUID_ID, waterIdPlusOne);
        menu.containerData().set(SpaMenu.DATA_FLUID_AMOUNT, 300);
        menu.containerData().set(SpaMenu.DATA_MIX, 0);
        helper.assertTrue(menu.getTankFluid() == Fluids.WATER && menu.getTankAmount() == 300,
                "a data-slot write (the sync landing) must surface through the tank getters");
        helper.assertTrue(!menu.getMix(), "and through the mix getter");
        helper.succeed();
    }

    /**
     * TC-M7-04-m4: the magic box binds one 3-row page (the original's
     * {@code bindBoxInventory(row)}) and the page buttons rebind the window by
     * rewriting the bound slots' box-slot indices, clamped to the box's page
     * range; page and slot count are data slots, so the client sees synced state.
     */
    @GameTest(template = "empty")
    public static void magicBoxPagesRebindTheBoundSlots(final GameTestHelper helper) {
        final BlockPos first = new BlockPos(0, 1, 0);
        final BlockPos second = new BlockPos(1, 1, 0);
        helper.setBlock(first, CatalogFurniture.BLOCK_MAGIC_BOX.get());
        helper.setBlock(second, CatalogFurniture.BLOCK_MAGIC_BOX.get());
        final TileMagicBox box = (TileMagicBox) helper.getBlockEntity(first);
        box.refreshLinks();
        // The original refreshLinks()/findBoxes() collects NEIGHBOURS only and
        // never seeds the origin, so a connected cluster's leaf re-adds the
        // origin as its neighbour's neighbour: two touching boxes count 2, and
        // the original's 27 * (linkedBoxes + 1) is 81, not 54. The port is
        // bit-faithful to that walk (TileMagicBox.findBoxes), so the probe
        // pins the original number rather than the intuitive one.
        helper.assertTrue(box.getContainerSize() == 81,
                "two touching boxes must yield the original's 27 * (2 + 1) = 81 slots "
                        + "(findBoxes never seeds the origin), got " + box.getContainerSize());

        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final MagicBoxMenu menu = new MagicBoxMenu(102, player.getInventory(), box);
        helper.assertTrue(menu.slots.size() == 27 + 36,
                "the box binds one 27-slot page plus the 36 player slots, got " + menu.slots.size());
        helper.assertTrue(menu.getPageCount() == 3 && menu.getCurrentPage() == 0,
                "81 slots span three pages, starting at page 0");
        helper.assertTrue(menu.containerData().get(MagicBoxMenu.DATA_BOX_SLOTS) == 81,
                "the client learns the box size from its data slot");
        helper.assertTrue(menu.slots.get(0).index == 0 && menu.slots.get(26).index == 26,
                "page 0 binds box slots 0..26 (the original's (j + 0) * 9 + k)");

        helper.assertTrue(menu.clickMenuButton(player, MagicBoxMenu.BUTTON_PAGE_NEXT),
                "the next-page button must be answered");
        helper.assertTrue(menu.getCurrentPage() == 1
                        && menu.containerData().get(MagicBoxMenu.DATA_PAGE) == 1,
                "the page moves and its data slot follows");
        helper.assertTrue(menu.slots.get(0).index == 27 && menu.slots.get(26).index == 53,
                "page 1 rebinds to box slots 27..53 (the original's refreshInventory)");

        helper.assertTrue(menu.clickMenuButton(player, MagicBoxMenu.BUTTON_PAGE_NEXT),
                "the third page must be reachable");
        helper.assertTrue(menu.getCurrentPage() == 2 && menu.slots.get(0).index == 54
                        && menu.slots.get(26).index == 80,
                "page 2 rebinds to the box's last 27 slots, 54..80");

        // Clamps at both ends: a press past the last page keeps it.
        menu.clickMenuButton(player, MagicBoxMenu.BUTTON_PAGE_NEXT);
        helper.assertTrue(menu.getCurrentPage() == 2, "the last page clamps the next button");
        helper.assertTrue(menu.clickMenuButton(player, MagicBoxMenu.BUTTON_PAGE_PREV),
                "the prev-page button must be answered");
        helper.assertTrue(menu.getCurrentPage() == 1
                        && menu.slots.get(0).index == 27 && menu.slots.get(26).index == 53,
                "page 1 rebinds back to box slots 27..53");
        menu.clickMenuButton(player, MagicBoxMenu.BUTTON_PAGE_PREV);
        helper.assertTrue(menu.getCurrentPage() == 0
                        && menu.slots.get(0).index == 0 && menu.slots.get(26).index == 26,
                "page 0 rebinds back to box slots 0..26");
        menu.clickMenuButton(player, MagicBoxMenu.BUTTON_PAGE_PREV);
        helper.assertTrue(menu.getCurrentPage() == 0, "page 0 clamps the prev button");

        // The client landing point: the synced page lands through ContainerData#set.
        menu.containerData().set(MagicBoxMenu.DATA_PAGE, 2);
        helper.assertTrue(menu.getCurrentPage() == 2, "the synced page surfaces on the client menu");
        helper.succeed();
    }
}
