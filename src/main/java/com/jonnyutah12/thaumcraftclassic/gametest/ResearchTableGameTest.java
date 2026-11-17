package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectCombinations;
import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.menus.ResearchTableMenu;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import com.jonnyutah12.thaumcraftclassic.tiles.TileDeconstructionTable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileResearchTable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TC-M7-04-m2b acceptance: the research table's window — the tile the original
 * owned, the menu type for {@code thaumcraft:gui_research_table} (original
 * gui_id 10), the synced aspect pool it draws, and the aspect-combination rule
 * the original's packet ran on the server.
 *
 * <p>Every expectation is measured off the decompiled originals
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/tiles/TileResearchTable.java},
 * {@code .../common/container/ContainerResearchTable.java},
 * {@code .../client/gui/GuiResearchTable.java} and
 * {@code .../common/lib/network/playerdata/PacketAspectCombinationToServer.java}),
 * and the citations are in the assertion messages so a future parity change has
 * to argue with a line of the original, not with this file.</p>
 *
 * <ul>
 *   <li>{@link #theTableIsTheMenuProviderTheBlockOpens}: {@code blockTable}
 *       variants 2-5 (the original's direction-encoded research bases) create
 *       {@link TileResearchTable}, the tile is the {@code MenuProvider} the
 *       block opens, its title is the gui row's lang key, and the menu type is
 *       registered under {@code gui_research_table}.</li>
 *   <li>{@link #menuSlotsAreTheOriginalsOrderAndAnchors}: slot 0 at (14,10),
 *       slot 1 at (70,10), the player grid at (48,175) and the hotbar at
 *       (48,233) — the original's {@code ContainerResearchTable} anchors, read
 *       from the same {@link ContainerLayout} constants the screen draws.</li>
 *   <li>{@link #menuSlotGatesAndShiftClickRouting}: the two machine slots'
 *       gates, and the original's {@code transferStackInSlot} routing — machine
 *       slots empty into the player, and the player's stacks are refused by the
 *       gates (which name the unmerged scribing-tool and note seams).</li>
 *   <li>{@link #menuCycleSyncsTheDiscoveredAspectPool}: the pool, the table's
 *       bonus for each aspect, the discovered count and the three research
 *       gates reach the window as data slots — the screen never reads the
 *       knowledge component, and emptying the pool empties the rows.</li>
 *   <li>{@link #combinePaysThePoolAndDiscoversTheCompound}: the original's
 *       combination: one of each aspect out of the player's pool, and on a first
 *       discovery +2 to each input and the compound discovered.</li>
 *   <li>{@link #combineGatesRefuseWithoutTheResearcherFlags}: RESEARCHER1 gates
 *       the whole rule, a known compound needs RESEARCHER2, an empty pool with
 *       no bonus pays nothing, and a bonus-fed pair drains the table.</li>
 *   <li>{@link #outOfInkFlagTracksTheScribingSlot}: the original's notice
 *       condition — empty scribing slot, or a tool at its full damage.</li>
 *   <li>{@link #duplicateIsTheWiredSeamAndRefusesWithoutANote}: button 5 is the
 *       original's {@code case 5: tileEntity.duplicate(player)}, the other ids
 *       are refused, and the duplicate itself is the documented refusal the
 *       note lane owns.</li>
 *   <li>{@link #theTableStateSurvivesASaveLoadRoundTrip} (TC-M7-04-m2b-ii): the
 *       tile's server-side persistence — the original's {@code writeCustomNBT}
 *       keys ship, and a reload restores both machine slots (item, count and
 *       damage), the whole bonus pool and {@code nextRecalc}.</li>
 *   <li>{@link #theMenuContractSurvivesASaveLoadRoundTrip} (TC-M7-04-m2b-ii):
 *       the container the registered {@code gui_research_table} type opens is the
 *       same container after a save/reload — slot count, every anchor, the
 *       out-of-ink condition and every synced row — and its bonus column comes
 *       from the reloaded tile, not from the live one.</li>
 *   <li>{@link #theOriginalsLoadFiltersRunOnAConstructedSave} (TC-M7-04-m2b-ii):
 *       the original's three load filters — the 2-entry read cap, the {@code Slot}
 *       byte range check and the unresolvable-aspect-tag drop — run on the port's
 *       load path.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchTableGameTest {

    private ResearchTableGameTest() {
    }

    // --- helpers ---------------------------------------------------------------

    /**
     * Place a research-table base: {@code blockTable} variant 2, the original's
     * first direction-encoded research base (TileResearchTable rides metas 2-5).
     */
    private static TileResearchTable tableAt(final GameTestHelper helper) {
        return tableAt(helper, TABLE_POS);
    }

    /** The same table placed in a cell of the caller's choosing (two-machine probes need two). */
    private static TileResearchTable tableAt(final GameTestHelper helper, final BlockPos cell) {
        helper.setBlock(cell, tableState(2));
        final BlockEntity entity = helper.getBlockEntity(cell);
        helper.assertTrue(entity instanceof TileResearchTable,
                "blockTable variant 2 must create the TileResearchTable the original hosts on its "
                        + "research bases (metas 2-5), got " + (entity == null ? "null" : entity.getClass()));
        return (TileResearchTable) entity;
    }

    private static BlockState tableState(final int variant) {
        final IntegerProperty property = (IntegerProperty) CatalogFurniture.BLOCK_TABLE.get()
                .getStateDefinition().getProperty("variant");
        if (property == null) {
            throw new IllegalStateException("blockTable carries no variant property");
        }
        return CatalogFurniture.BLOCK_TABLE.get().defaultBlockState().setValue(property, variant);
    }

    /**
     * A mock player with a clean slate: the harness hands out one mock player per
     * test, so its knowledge and inventory are reset here before each probe seeds
     * what it needs. The harness player is a plain {@code Player}, not a
     * {@code ServerPlayer} ({@code ArcaneWorkbenchGameTest} documents the same
     * seam), which is why {@code TileResearchTable.combineAspects} takes a
     * {@code Player} and pushes its pool payloads only to a server player.
     */
    private static Player seededPlayer(final GameTestHelper helper, final List<String> research,
                                       final Map<String, Integer> pool) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(1.5D, 2.0D, 1.5D);
        final Inventory inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            inv.setItem(slot, ItemStack.EMPTY);
        }
        PlayerKnowledgeData knowledge = PlayerKnowledgeData.empty();
        for (final String key : research) {
            knowledge = knowledge.withResearchCompleted(key);
        }
        for (final Map.Entry<String, Integer> entry : pool.entrySet()) {
            knowledge = knowledge.withAspectDiscovered(entry.getKey(), entry.getValue());
        }
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
        return player;
    }

    private static PlayerKnowledgeData knowledgeOf(final Player player) {
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        return knowledge == null ? PlayerKnowledgeData.empty() : knowledge;
    }

    /** The first compound in the original's combination table whose two parts are primals. */
    private static Aspect[] primalPairWithACompound() {
        for (final Map.Entry<String, String> entry : AspectCombinations.combinationTable().entrySet()) {
            final String[] tags = entry.getKey().split("\\+");
            final Aspect first = Aspect.getAspect(tags[0]);
            final Aspect second = Aspect.getAspect(tags[1]);
            final Aspect result = AspectCombinations.getCombinationResult(first, second);
            if (first != null && second != null && result != null
                    && first.isPrimal() && second.isPrimal()) {
                return new Aspect[]{first, second, result};
            }
        }
        throw new IllegalStateException("the aspect registry has no primal pair with a compound result");
    }

    /** The menu row showing {@code aspect}, or -1: the rows are in the knowledge's own order. */
    private static int rowOf(final ResearchTableMenu menu, final Aspect aspect) {
        for (int row = 0; row < ResearchTableMenu.MAX_ROWS; row++) {
            if (menu.aspectAt(row) == aspect) {
                return row;
            }
        }
        return -1;
    }

    /** Which container slot carries this stack (item, components, count and damage), or -1. */
    private static int slotHolding(final List<Slot> slots, final ItemStack expected) {
        for (int i = 0; i < slots.size(); i++) {
            final ItemStack stack = slots.get(i).getItem();
            if (ItemStack.isSameItemSameComponents(stack, expected)
                    && stack.getCount() == expected.getCount()
                    && stack.getDamageValue() == expected.getDamageValue()) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------- the block wire

    /**
     * The block the original opened: {@code blockTable} metas 2-5 are the
     * research bases and carry the research tile, the tile is the
     * {@code MenuProvider} whose title is the gui row's key, and the menu type
     * is registered under that same id.
     */
    @GameTest(template = "gametest_pad_32")
    public static void theTableIsTheMenuProviderTheBlockOpens(final GameTestHelper helper) {
        final TileResearchTable table = tableAt(helper);
        final Player player = seededPlayer(helper, List.of(), Map.of());

        helper.assertTrue(table instanceof net.minecraft.world.MenuProvider,
                "TC-M7-04-m2b: the research tile must be the MenuProvider its block opens");
        final var title = table.getDisplayName().getContents();
        helper.assertTrue(title instanceof TranslatableContents contents
                        && "container.thaumcraft.gui_research_table".equals(contents.getKey()),
                "the menu title must be the translatable key the gui row ships, got " + title);
        helper.assertTrue(table.createMenu(7, player.getInventory(), player) instanceof ResearchTableMenu,
                "the provider must build the menu the registered MenuType names");
        final var menuTypeId = BuiltInRegistries.MENU.getKey(CatalogFurniture.MENU_RESEARCH_TABLE.get());
        helper.assertTrue(menuTypeId != null && "gui_research_table".equals(menuTypeId.getPath())
                        && "thaumcraft".equals(menuTypeId.getNamespace()),
                "the menu type is registered under the content-mapping.csv gui row id, got " + menuTypeId);

        // The variant domain: metas 2-5 research, 14 deconstruction, the rest plain tables.
        for (int variant = 0; variant <= 14; variant++) {
            // A plane of its own: setBlock over a cell that already holds a block
            // entity leaves that entity in place, so the sweep must not reuse (1,1,1),
            // where this probe placed the research base first.
            final BlockPos pos = new BlockPos(1 + (variant % 5) * 3, 4, 1 + (variant / 5) * 3);
            helper.setBlock(pos, tableState(variant));
            final BlockEntity entity = helper.getBlockEntity(pos);
            final boolean researchBase = variant >= 2 && variant <= 5;
            helper.assertTrue(entity instanceof TileResearchTable == researchBase,
                    "variant " + variant + " must " + (researchBase ? "" : "NOT ")
                            + "host TileResearchTable (the original's blockTable metas 2-5 are the "
                            + "direction-encoded research bases), got "
                            + (entity == null ? "null" : entity.getClass().getSimpleName()));
            if (variant == 14) {
                helper.assertTrue(entity instanceof TileDeconstructionTable,
                        "variant 14 stays the deconstruction table this repo shipped in TC-M7-04-s");
            }
            if (variant == 0 || variant == 6) {
                helper.assertTrue(entity instanceof TileTable,
                        "variant " + variant + " stays a plain worktable, got "
                                + (entity == null ? "null" : entity.getClass().getSimpleName()));
            }
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ the menu

    /**
     * The original's slot order and anchors: {@code SlotLimitedByClass(IScribeTools,
     * tile, 0, 14, 10)}, {@code SlotLimitedByClass(ItemResearchNotes, tile, 1, 70,
     * 10)}, the player grid at {@code 48 + col*18, 175 + row*18}, the hotbar at
     * {@code 48 + i*18, 233} (ContainerResearchTable.java:22-36). The menu reads
     * its anchors from {@link ContainerLayout}, the same constants the screen
     * draws, so a drawn frame cannot drift off the slot it frames.
     */
    @GameTest(template = "empty")
    public static void menuSlotsAreTheOriginalsOrderAndAnchors(final GameTestHelper helper) {
        final TileResearchTable table = tableAt(helper);
        final Player player = seededPlayer(helper, List.of(), Map.of());
        final ResearchTableMenu menu = new ResearchTableMenu(1, player.getInventory(), table);

        helper.assertTrue(menu.slots.size() == ResearchTableMenu.SLOT_COUNT,
                "the original's menu is 2 machine slots + 36 player slots = "
                        + ResearchTableMenu.SLOT_COUNT + ", got " + menu.slots.size());
        assertAnchor(helper, menu, TileResearchTable.SLOT_INK, ContainerLayout.RT_INK_X,
                ContainerLayout.RT_INK_Y, "scribing tool");
        assertAnchor(helper, menu, TileResearchTable.SLOT_NOTES, ContainerLayout.RT_NOTES_X,
                ContainerLayout.RT_NOTES_Y, "research note");
        assertAnchor(helper, menu, ResearchTableMenu.PLAYER_FIRST,
                ContainerLayout.researchTablePlayerX(0), ContainerLayout.RT_PLAYER_GRID_TOP, "player grid");
        assertAnchor(helper, menu, ResearchTableMenu.PLAYER_FIRST + 26,
                ContainerLayout.researchTablePlayerX(8), ContainerLayout.researchTablePlayerY(2),
                "player grid end");
        assertAnchor(helper, menu, ResearchTableMenu.HOTBAR_FIRST,
                ContainerLayout.researchTablePlayerX(0), ContainerLayout.RT_HOTBAR_TOP, "hotbar");
        assertAnchor(helper, menu, ResearchTableMenu.SLOT_COUNT - 1,
                ContainerLayout.researchTablePlayerX(8), ContainerLayout.RT_HOTBAR_TOP, "hotbar end");

        helper.assertTrue(menu.slots.get(TileResearchTable.SLOT_INK).container == table
                        && menu.slots.get(TileResearchTable.SLOT_NOTES).container == table,
                "the two machine slots are the tile's slots 0 and 1 (the tile is a 2-slot IInventory: "
                        + "TileResearchTable.java:36)");
        helper.assertTrue(menu.slots.get(ResearchTableMenu.PLAYER_FIRST).container == player.getInventory(),
                "the player rows are the player's inventory, as the original bound them");
        helper.assertTrue(ResearchTableMenu.DATA_COUNT
                        == ResearchTableMenu.DATA_INDEX_BASE + 3 * ResearchTableMenu.MAX_ROWS,
                "the display travels as " + ResearchTableMenu.DATA_INDEX_BASE + " flags plus 3 wire "
                        + "values per aspect row (the original read the tile in-process; BD-19)");
        helper.assertTrue(menu.dataValue(-1) == 0 && menu.dataValue(ResearchTableMenu.DATA_COUNT) == 0,
                "the wire view clamps: a row the client never sent cannot read past the array");
        helper.succeed();
    }

    private static void assertAnchor(final GameTestHelper helper, final ResearchTableMenu menu,
                                     final int index, final int x, final int y, final String what) {
        final Slot slot = menu.slots.get(index);
        helper.assertTrue(slot.x == x && slot.y == y,
                what + " slot anchor: the menu says (" + slot.x + "," + slot.y + "), the original's "
                        + "ContainerResearchTable and client/ContainerLayout both say (" + x + "," + y + ")");
    }

    /**
     * The two machine slots' gates, and the original's shift-click routing
     * ({@code ContainerResearchTable.transferStackInSlot}: machine slots to
     * {@code moveItemStackTo(stack, 2, size, true)}, player rows to
     * {@code moveItemStackTo(stack, 0, 2, false)}). The gates are the two seams
     * BD-19 names: no port item implements {@code IScribeTools} and
     * {@code thaumcraft:itemresearchnotes} is not registered, so both slots
     * refuse every stack today — the routing branch, not a guess.
     */
    @GameTest(template = "empty")
    public static void menuSlotGatesAndShiftClickRouting(final GameTestHelper helper) {
        final TileResearchTable table = tableAt(helper);
        final Player player = seededPlayer(helper, List.of(), Map.of());
        final Inventory inv = player.getInventory();
        final ResearchTableMenu menu = new ResearchTableMenu(1, inv, table);

        final Slot ink = menu.slots.get(TileResearchTable.SLOT_INK);
        final Slot notes = menu.slots.get(TileResearchTable.SLOT_NOTES);
        helper.assertTrue(!ink.mayPlace(new ItemStack(Items.STONE)),
                "the original's slot 0 is SlotLimitedByClass(IScribeTools.class): stone never lands");
        helper.assertTrue(!ink.mayPlace(new ItemStack(Items.INK_SAC)),
                "the scribing-tool seam is the IScribeTools interface, not the ink sac item — "
                        + "no port item implements it today (BD-19)");
        helper.assertTrue(!notes.mayPlace(new ItemStack(Items.PAPER)),
                "the original's slot 1 is ItemResearchNotes at damage < 64 (TileResearchTable.java:442-452); "
                        + "thaumcraft:itemresearchnotes is not registered, so the gate refuses (BD-19)");
        helper.assertTrue(!table.canPlaceItem(TileResearchTable.SLOT_NOTES, new ItemStack(Items.BOOK)),
                "the tile-side gate agrees with the slot's: the notes slot is empty of a domain today");
        helper.assertTrue(table.getContainerSize() == 2,
                "the original's tile is a 2-slot IInventory (contents = new ItemStack[2])");

        // Machine slot -> player: the original's `if (slot < 2)` branch. The tile's
        // gate is bypassed by writing the slot directly, which is what lets the
        // routing be exercised at all while the seams refuse.
        table.setItem(TileResearchTable.SLOT_INK, new ItemStack(Items.GOLD_INGOT));
        menu.quickMoveStack(player, TileResearchTable.SLOT_INK);
        helper.assertTrue(table.getItem(TileResearchTable.SLOT_INK).isEmpty(),
                "the machine-slot item must have left the tile — the original's slot<2 branch");
        boolean goldInPlayer = false;
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            if (inv.getItem(slot).is(Items.GOLD_INGOT)) {
                goldInPlayer = true;
            }
        }
        helper.assertTrue(goldInPlayer, "and it landed in the player's rows (moveItemStackTo(2, size, true))");

        // Player row -> machine slots: the original's `else` branch, gated by mayPlace.
        // The harness player ships with a kit, so the rows are cleared: this is about
        // the routing, not about where the kit sat.
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            inv.setItem(slot, ItemStack.EMPTY);
        }
        inv.setItem(9, new ItemStack(Items.COBBLESTONE));
        menu.quickMoveStack(player, ResearchTableMenu.PLAYER_FIRST);
        helper.assertTrue(inv.getItem(9).is(Items.COBBLESTONE),
                "the player's shift-click runs the original's moveItemStackTo(stack, 0, 2, false) and "
                        + "both machine slots refuse it, so the stack stays with the player");
        helper.assertTrue(table.getItem(TileResearchTable.SLOT_INK).isEmpty()
                        && table.getItem(TileResearchTable.SLOT_NOTES).isEmpty(),
                "and nothing entered the machine slots");
        helper.succeed();
    }

    // -------------------------------------------------------- the synced display

    /**
     * What the window draws, synced: the discovered count the original's paging
     * runs on, the three research gates, and per discovered aspect the player's
     * pool and this table's bonus (the original read
     * {@code Thaumcraft.proxy.playerKnowledge.getAspectPoolFor} and
     * {@code tile.bonusAspects} in-process — 1.21.1 gives the screen only its
     * menu, BD-19). Emptying the pool empties the rows, which is what proves the
     * rows are synced and not a client-side guess.
     */
    @GameTest(template = "empty")
    public static void menuCycleSyncsTheDiscoveredAspectPool(final GameTestHelper helper) {
        final TileResearchTable table = tableAt(helper);
        final Aspect[] triple = primalPairWithACompound();
        final Aspect first = triple[0];
        final Aspect second = triple[1];
        final Aspect compound = triple[2];
        final Player player = seededPlayer(helper,
                List.of("RESEARCHER1", "RESEARCHER2", "RESEARCHDUPE"),
                Map.of(first.getTag(), 3, second.getTag(), 1, compound.getTag(), 0));

        // The table's own bonus pool: the original's public `bonusAspects` field.
        table.getBonusAspects().add(first, 2);

        final ResearchTableMenu menu = new ResearchTableMenu(1, player.getInventory(), table);
        menu.broadcastChanges();

        helper.assertTrue(menu.getDiscoveredCount() == 3,
                "the discovered count is the size the original's grid paginates over "
                        + "(GuiResearchTable.java:266), got " + menu.getDiscoveredCount());
        helper.assertTrue(menu.knowsResearcher1() && menu.knowsResearcher2() && menu.knowsResearchDupe(),
                "the original's three gates (RESEARCHER1 / RESEARCHER2 / RESEARCHDUPE) are synced flags, "
                        + "not a client-side research lookup");
        final int rowFirst = rowOf(menu, first);
        final int rowCompound = rowOf(menu, compound);
        helper.assertTrue(rowFirst >= 0 && rowCompound >= 0,
                "every discovered aspect gets a row: the grid walks the knowledge list");
        helper.assertTrue(menu.poolAt(rowFirst) == 3 && menu.bonusAt(rowFirst) == 2,
                "the row carries the player's pool (3) and this table's bonus (2) — the pair the "
                        + "original drew as a tag plus a second dimmer tag, got "
                        + menu.poolAt(rowFirst) + "/" + menu.bonusAt(rowFirst));
        helper.assertTrue(menu.poolAt(rowCompound) == 0 && menu.bonusAt(rowCompound) == 0,
                "a discovered aspect with no pool and no bonus keeps its row at zero: that is the "
                        + "original's faded rule (0.33F), not a dropped row");
        helper.assertTrue(ContainerLayout.withAlpha(first.getColor(), ContainerLayout.RT_FADED_ALPHA)
                        != first.getColor(),
                "the faded alpha is the original's 0.33F, and it changes the colour it is given");
        // The same values on the wire, at the published indices: the screen reads
        // the named accessors, the probe reads the wire, and the two must agree.
        helper.assertTrue(menu.dataValue(ResearchTableMenu.DATA_DISCOVERED_COUNT) == 3
                        && menu.dataValue(ResearchTableMenu.DATA_RESEARCHER1) == 1
                        && menu.dataValue(ResearchTableMenu.DATA_RESEARCHER2) == 1
                        && menu.dataValue(ResearchTableMenu.DATA_RESEARCH_DUPE) == 1,
                "the discovered count and the three research flags sit at their published indices");
        final int rowWire = ResearchTableMenu.DATA_INDEX_BASE + 3 * rowFirst;
        helper.assertTrue(menu.dataValue(rowWire + ResearchTableMenu.DATA_POOL_OFFSET) == 3
                        && menu.dataValue(rowWire + ResearchTableMenu.DATA_BONUS_OFFSET) == 2,
                "the row's pool and bonus sit at the published offsets, which is what the screen's "
                        + "poolAt/bonusAt pair reads");

        // The rows follow the knowledge: empty the component and the window loses them.
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, PlayerKnowledgeData.empty());
        menu.broadcastChanges();
        helper.assertTrue(menu.getDiscoveredCount() == 0 && menu.aspectAt(0) == null,
                "an empty knowledge component means an empty grid — the pool is synced, not guessed");
        helper.assertTrue(!menu.knowsResearcher1() && !menu.knowsResearchDupe(),
                "and the gates follow the same sync");
        helper.succeed();
    }

    // ------------------------------------------------------------- combination

    /**
     * The original's combination (the research tile's rule, and
     * {@code PacketAspectCombinationToServer.onMessage} at its :71-126): pay one
     * of each input out of the player's pool, and on a first discovery of the
     * compound add 2 to each input and discover the compound with 1.
     */
    @GameTest(template = "empty")
    public static void combinePaysThePoolAndDiscoversTheCompound(final GameTestHelper helper) {
        final TileResearchTable table = tableAt(helper);
        final Aspect[] triple = primalPairWithACompound();
        final Aspect first = triple[0];
        final Aspect second = triple[1];
        final Aspect compound = triple[2];
        final Player player = seededPlayer(helper, List.of("RESEARCHER1"),
                Map.of(first.getTag(), 1, second.getTag(), 1));

        helper.assertTrue(table.combineAspects(player, first, second, false, false),
                "a RESEARCHER1 player with one of each input combines (the original's happy path)");
        final PlayerKnowledgeData after = knowledgeOf(player);
        helper.assertTrue(after.aspectAmount(first.getTag()) == 2
                        && after.aspectAmount(second.getTag()) == 2,
                "one paid from each pool, then the original's discovery bonus of +2 to each input: "
                        + "1 - 1 + 2 = 2, got " + after.aspectAmount(first.getTag()) + "/"
                        + after.aspectAmount(second.getTag()));
        helper.assertTrue(after.hasAspectDiscovered(compound.getTag())
                        && after.aspectAmount(compound.getTag()) == 1,
                "the compound " + compound.getTag() + " is discovered with one unit (the original's "
                        + "completeAspects discovery of the combination result)");
        helper.succeed();
    }

    /**
     * The original's refusals, each paying nothing: RESEARCHER1 gates the whole
     * rule, a compound the player already knows needs RESEARCHER2, and a pair
     * with no pool and no table bonus is refused. A pair fed from the table's
     * bonus (the original's {@code fromTable} flags) drains the table, not the
     * player.
     */
    @GameTest(template = "empty")
    public static void combineGatesRefuseWithoutTheResearcherFlags(final GameTestHelper helper) {
        final TileResearchTable table = tableAt(helper);
        final Aspect[] triple = primalPairWithACompound();
        final Aspect first = triple[0];
        final Aspect second = triple[1];
        final Aspect compound = triple[2];

        // 1. No RESEARCHER1: refused, pool untouched.
        Player player = seededPlayer(helper, List.of(), Map.of(first.getTag(), 5, second.getTag(), 5));
        helper.assertTrue(!table.combineAspects(player, first, second, false, false),
                "the original's handler returns when the player has not researched RESEARCHER1");
        helper.assertTrue(knowledgeOf(player).aspectAmount(first.getTag()) == 5,
                "and a refusal pays nothing");

        // 2. The compound is already known and RESEARCHER2 is not: refused.
        player = seededPlayer(helper, List.of("RESEARCHER1"),
                Map.of(first.getTag(), 5, second.getTag(), 5, compound.getTag(), 1));
        helper.assertTrue(!table.combineAspects(player, first, second, false, false),
                "re-combining a known compound is the RESEARCHER2 rule (the original sends its "
                        + "researcher2 tooltip and returns)");
        helper.assertTrue(knowledgeOf(player).aspectAmount(first.getTag()) == 5, "and it pays nothing");

        // 3. An empty pool with no table bonus: refused. The compound is NOT
        // discovered here, so the refusal is the pool gate, not RESEARCHER2.
        player = seededPlayer(helper, List.of("RESEARCHER1"), Map.of());
        helper.assertTrue(!table.combineAspects(player, first, second, false, false),
                "no pool unit and no bonus flag: the original's amount<=0 && !fromTable return");
        helper.assertTrue(!table.combineAspects(player, first, second, true, false)
                        && !table.combineAspects(player, first, second, true, true),
                "the bonus flag alone is not enough — the table's bonus pool must actually hold one "
                        + "(the original's playerKnowledge.addAspectPool(-1) on the table's list)");

        // 4. The table's bonus feeds both wells: the table pays, the player does
        // not. The compound is already discovered (at 0), and RESEARCHER2 is
        // known, so this is the re-combine path the second research unlocks —
        // which pays no discovery bonus, exactly what isolates the payer.
        table.getBonusAspects().add(first, 1);
        table.getBonusAspects().add(second, 1);
        player = seededPlayer(helper, List.of("RESEARCHER1", "RESEARCHER2"), Map.of(compound.getTag(), 0));
        helper.assertTrue(table.combineAspects(player, first, second, true, true),
                "a bonus-fed pair combines once RESEARCHER2 unlocks the known compound");
        helper.assertTrue(table.getBonus(first) == 0 && table.getBonus(second) == 0,
                "and one of each left the table's bonus pool");
        helper.assertTrue(knowledgeOf(player).aspectAmount(first.getTag()) == 0
                        && knowledgeOf(player).aspectAmount(second.getTag()) == 0
                        && knowledgeOf(player).aspectAmount(compound.getTag()) == 0,
                "and the player's pool paid nothing: 0 in, 0 out — the fromTable flags moved the "
                        + "payment to the table");
        helper.succeed();
    }

    // -------------------------------------------------------------- ink and dupes

    /**
     * The original's out-of-ink notice condition (GuiResearchTable.java:213-228):
     * the scribing slot is empty, or its tool is at full damage.
     */
    @GameTest(template = "empty")
    public static void outOfInkFlagTracksTheScribingSlot(final GameTestHelper helper) {
        final TileResearchTable table = tableAt(helper);
        final Player player = seededPlayer(helper, List.of(), Map.of());
        final ResearchTableMenu menu = new ResearchTableMenu(1, player.getInventory(), table);
        menu.broadcastChanges();

        helper.assertTrue(menu.isOutOfInk(),
                "an empty scribing slot shows the original's two-line notice");
        final ItemStack full = new ItemStack(Items.IRON_PICKAXE);
        table.setItem(TileResearchTable.SLOT_INK, full);
        menu.broadcastChanges();
        helper.assertTrue(!menu.isOutOfInk(), "a tool with ink left does not");
        final ItemStack spent = new ItemStack(Items.IRON_PICKAXE);
        spent.setDamageValue(spent.getMaxDamage());
        table.setItem(TileResearchTable.SLOT_INK, spent);
        menu.broadcastChanges();
        helper.assertTrue(menu.isOutOfInk(),
                "damage == maxDamage is the original's second half (its itemDamage == itemMaxDamage test)");
        helper.succeed();
    }

    /**
     * The original's menu buttons: {@code button == 1} is accepted, {@code 5} is
     * {@code tileEntity.duplicate(player)} (ContainerResearchTable.java:39-48),
     * every other id is refused. The duplicate is the documented refusal while
     * the note lane is unmerged (BD-19): the wired seam runs and changes nothing
     * — no note, no paper, no ink sac, no copy.
     */
    @GameTest(template = "empty")
    public static void duplicateIsTheWiredSeamAndRefusesWithoutANote(final GameTestHelper helper) {
        final TileResearchTable table = tableAt(helper);
        final Player player = seededPlayer(helper, List.of("RESEARCHER1", "RESEARCHDUPE"), Map.of());
        final Inventory inv = player.getInventory();
        inv.setItem(0, new ItemStack(Items.PAPER));
        inv.setItem(1, new ItemStack(Items.INK_SAC));
        final int before = countItems(inv);
        final ResearchTableMenu menu = new ResearchTableMenu(1, inv, table);

        helper.assertTrue(menu.clickMenuButton(player, ResearchTableMenu.BUTTON_LEARN),
                "the original's container returns true for button 1 (its `if (button == 1) return true`)");
        helper.assertTrue(!menu.clickMenuButton(player, 2)
                        && !menu.clickMenuButton(player, 3)
                        && !menu.clickMenuButton(player, 4)
                        && !menu.clickMenuButton(player, 6),
                "the original's switch has no case 2, 3, 4 or 6 — every other id is refused");
        helper.assertTrue(menu.clickMenuButton(player, ResearchTableMenu.BUTTON_DUPLICATE),
                "button 5 is the original's `case 5: tileEntity.duplicate(player); return true`");
        helper.assertTrue(table.getItem(TileResearchTable.SLOT_NOTES).isEmpty(),
                "the duplicate ran and refused: slot 1 holds no finished note (damage 64), and no "
                        + "note item is registered to put one there (BD-19)");
        helper.assertTrue(countItems(inv) == before,
                "a refused duplicate consumes no paper and no ink sac (the original consumes them only "
                        + "after its pool check passes, TileResearchTable.java:454-455)");
        helper.succeed();
    }

    private static int countItems(final Inventory inv) {
        int total = 0;
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            total += inv.getItem(slot).getCount();
        }
        return total;
    }

    // ------------------------------------------- persistence (TC-M7-04-m2b-ii)

    /** The cell {@link #tableAt} places the research base on. */
    private static final BlockPos TABLE_POS = new BlockPos(1, 1, 1);

    /** Aspects with a positive amount in the table's bonus pool — the list the original writes. */
    private static int positiveBonusEntries(final TileResearchTable table) {
        int entries = 0;
        for (final Aspect aspect : table.getBonusAspects().getAspects()) {
            if (aspect != null && table.getBonusAspects().getAmount(aspect) > 0) {
                entries++;
            }
        }
        return entries;
    }

    /** One {@code Inventory} entry in the original's shape: a {@code Slot} byte plus the inline item. */
    private static CompoundTag inventoryEntry(final int slot, final ItemStack stack,
                                              final HolderLookup.Provider provider) {
        final CompoundTag entry = new CompoundTag();
        entry.putByte("Slot", (byte) slot);
        entry.put("Item", stack.save(provider));
        return entry;
    }

    /** Reload a saved tile the way the chunk provider does (the {@code CatalogFurnitureGameTest} seam). */
    private static TileResearchTable reloadSavedTable(final GameTestHelper helper, final CompoundTag tag) {
        final BlockEntity reloaded = BlockEntity.loadStatic(TABLE_POS, helper.getBlockState(TABLE_POS),
                tag, helper.getLevel().registryAccess());
        helper.assertTrue(reloaded instanceof TileResearchTable,
                "the saved research base did not reload as a research base, got "
                        + (reloaded == null ? "null" : reloaded.getClass().getName()));
        return (TileResearchTable) reloaded;
    }

    /**
     * The chunk provider's own reload. The tile is rebuilt by
     * {@code BlockEntity#loadStatic} — the call the chunk provider makes with the saved
     * tag, so {@code loadAdditional} runs on the real registries — then handed to the
     * chunk with {@code addAndRegisterBlockEntity}, the registration the same loader
     * uses. The cell is emptied first so the live tile is gone and the chunk has no
     * entry to collide with; the block state goes to {@code loadStatic} directly, as it
     * does mid-load before the sections are placed. This is the reload that proves the
     * window: {@code ResearchTableMenu.broadcastChanges} refreshes the display only for
     * a tile that has a level, so a detached {@code loadStatic} reload can prove the
     * tile and never the container it builds.
     */
    private static TileResearchTable reloadSavedTableInWorld(final GameTestHelper helper, final CompoundTag tag,
                                                             final BlockPos cell) {
        helper.setBlock(cell, Blocks.AIR.defaultBlockState());
        // GameTestHelper#getBlockEntity ASSERTS the tile is there, so the emptiness
        // check has to go to the level directly (its lookup returns null).
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(cell)) == null,
                "emptying the cell must drop the tile it was hosting");
        // The chunk only registers a tile for a cell whose state carries a block entity
        // (LevelChunk#addAndRegisterBlockEntity guards on hasBlockEntity), so the base
        // goes back in before the saved tile is handed over. Nothing may read
        // getBlockEntity between here and the insert: that would create the fresh tile
        // lazily and the chunk would keep it instead of the loaded one.
        helper.setBlock(cell, tableState(2));
        // The helper's positions are relative to the test structure; the level and the
        // chunk need the real one.
        final BlockPos worldPos = helper.absolutePos(cell);
        final BlockEntity reloaded = BlockEntity.loadStatic(worldPos, helper.getBlockState(cell), tag,
                helper.getLevel().registryAccess());
        helper.assertTrue(reloaded instanceof TileResearchTable,
                "the saved research base did not reload as a research base, got "
                        + (reloaded == null ? "null" : reloaded.getClass().getName()));
        reloaded.setLevel(helper.getLevel());
        final net.minecraft.world.level.chunk.LevelChunk chunk = helper.getLevel().getChunkAt(worldPos);
        // Put the tile in the chunk's map, then run the loader's registration so the
        // tile gets its onLoad/level wiring the way the chunk load does.
        chunk.setBlockEntity(reloaded);
        chunk.addAndRegisterBlockEntity(reloaded);
        final TileResearchTable inWorld = helper.getBlockEntity(cell);
        helper.assertTrue(inWorld == reloaded,
                "the reloaded tile must be the one the level registered at " + worldPos + " (state has a "
                        + "block entity: " + helper.getLevel().getBlockState(worldPos).hasBlockEntity()
                        + ", chunk says: " + (helper.getLevel().getBlockEntity(worldPos) == null ? "null"
                                : helper.getLevel().getBlockEntity(worldPos).getClass().getName())
                        + "), got " + (inWorld == null ? "null" : inWorld.getClass().getName()));
        return inWorld;
    }

    /**
     * TC-M7-04-m2b-ii — the tile's server-side persistence. The original's
     * {@code writeCustomNBT}/{@code readCustomNBT} pair (TileResearchTable.java:43-93)
     * is the whole of the research table's state: the {@code Inventory} list, the
     * {@code nextRecalc} counter and the {@code bonusAspects} pool. This probe saves a
     * filled table, reloads it, and requires every one of those to come back — the
     * expectations are counted off the tile and the aspect registry, never off a list
     * written into this file.
     */
    @GameTest(template = "empty")
    public static void theTableStateSurvivesASaveLoadRoundTrip(final GameTestHelper helper) {
        final HolderLookup.Provider provider = helper.getLevel().registryAccess();
        final TileResearchTable table = tableAt(helper);

        // Both machine slots: a partly-used scribing stand-in and a note-shaped stack.
        // setItem is the tile's own write path; the two slot gates are BD-19's refusals,
        // and the save path has to preserve what the tile holds either way.
        final ItemStack ink = new ItemStack(Items.IRON_PICKAXE);
        ink.setDamageValue(3);
        table.setItem(TileResearchTable.SLOT_INK, ink);
        table.setItem(TileResearchTable.SLOT_NOTES, new ItemStack(Items.PAPER, 5));

        // The bonus pool, seeded from the registry: the primals it publishes. The
        // original's own flows only ever put ONE of an aspect in this pool —
        // recalculateBonus guards `getAmount(aspect) < 1` before it merges
        // (TileResearchTable.java:216-233) — and its write emits ONE entry per
        // positive aspect (:83-92) while its read merges 1 per entry (:57-65). So
        // the pool survives a save as a set of aspects, each at 1.
        final List<Aspect> seeded = new ArrayList<>();
        for (final Aspect aspect : Aspect.getPrimalAspects()) {
            table.getBonusAspects().add(aspect, 1);
            seeded.add(aspect);
            if (seeded.size() == 3) {
                break;
            }
        }
        helper.assertTrue(seeded.size() == 3,
                "the aspect registry must publish at least 3 primals for this probe to seed the pool");
        // One aspect above that domain, to pin the collapse rule the write/read pair implies.
        final Aspect stacked = seeded.get(0);
        table.getBonusAspects().add(stacked, 2);
        helper.assertTrue(table.getBonus(stacked) == 3,
                "the live pool holds 3 of " + stacked.getTag() + ", got " + table.getBonus(stacked));
        Aspect unseeded = null;
        for (final Aspect aspect : Aspect.values()) {
            if (!table.hasBonusAspect(aspect)) {
                unseeded = aspect;
                break;
            }
        }
        helper.assertTrue(unseeded != null, "the registry has an aspect this probe did not seed");

        int filledSlots = 0;
        for (int slot = 0; slot < table.getContainerSize(); slot++) {
            if (!table.getItem(slot).isEmpty()) {
                filledSlots++;
            }
        }

        // The original's write: the "Inventory" list with a Slot byte and the inline
        // item (:71-81), "nextRecalc" (:82) and "bonusAspects" as {tag} compounds (:83-92).
        final CompoundTag tag = table.saveWithFullMetadata(provider);
        helper.assertTrue(tag.contains("Inventory") && tag.contains("nextRecalc") && tag.contains("bonusAspects"),
                "the save must carry the original's three keys (TileResearchTable.java:81-93), got "
                        + tag.getAllKeys());
        final ListTag savedItems = tag.getList("Inventory", Tag.TAG_COMPOUND);
        helper.assertTrue(savedItems.size() == filledSlots,
                "the original writes one Inventory entry per non-empty slot (:71-80): " + filledSlots
                        + " filled slots, " + savedItems.size() + " entries");
        for (int i = 0; i < savedItems.size(); i++) {
            final CompoundTag entry = savedItems.getCompound(i);
            final int slot = entry.getByte("Slot") & 255;
            helper.assertTrue(slot < table.getContainerSize() && !table.getItem(slot).isEmpty()
                            && entry.contains("Item"),
                    "the " + i + "-th Inventory entry names a slot the tile actually fills, got slot=" + slot);
        }
        final ListTag savedBonus = tag.getList("bonusAspects", Tag.TAG_COMPOUND);
        helper.assertTrue(savedBonus.size() == positiveBonusEntries(table),
                "the bonus list is one {tag} per positive aspect (:83-92), got " + savedBonus.size());
        for (int i = 0; i < savedBonus.size(); i++) {
            final CompoundTag entry = savedBonus.getCompound(i);
            helper.assertTrue(entry.contains("tag", Tag.TAG_STRING)
                            && Aspect.getAspect(entry.getString("tag")) != null,
                    "every bonus entry carries an aspect tag the registry resolves (:88-90)");
        }
        // What string goes under "tag" is a persistence contract, not a label: the
        // load reads it back as a registry id, and Aspect.getName() capitalises the
        // tag ("Aer"), which is not a legal path character. The original writes
        // aspect.getTag() (:88), so that is what the save must ship.
        for (int i = 0; i < savedBonus.size(); i++) {
            final String saved = savedBonus.getCompound(i).getString("tag");
            final Aspect resolved = Aspect.getAspect(saved);
            helper.assertTrue(resolved != null && saved.equals(resolved.getTag()),
                    "the bonus entry ships the registry tag (\"" + saved + "\"), not the capitalised "
                            + "display name: the load resolves this string through Aspect.getAspect "
                            + "(TileResearchTable.java:60-63) and the original writes getTag() (:88)");
        }
        for (final Aspect aspect : seeded) {
            boolean written = false;
            for (int i = 0; i < savedBonus.size(); i++) {
                if (aspect.getTag().equals(savedBonus.getCompound(i).getString("tag"))) {
                    written = true;
                }
            }
            helper.assertTrue(written,
                    "the pool entry for " + aspect.getTag() + " is in the saved bonusAspects list");
        }

        // nextRecalc has no setter: the original's counter is written by the 600-tick
        // world scan this port refuses (recalculateBonus, BD-19), so seed the tile's own
        // key and prove the load reads it and the next save writes it back.
        tag.putInt("nextRecalc", 137);

        final TileResearchTable restored = reloadSavedTable(helper, tag);
        helper.assertTrue(restored.getContainerSize() == 2,
                "the original's tile is a 2-slot IInventory (contents = new ItemStack[2] at :36, "
                        + "func_70302_i_ returns 2 at :306), got " + restored.getContainerSize());
        for (int slot = 0; slot < table.getContainerSize(); slot++) {
            final ItemStack before = table.getItem(slot);
            final ItemStack after = restored.getItem(slot);
            helper.assertTrue(ItemStack.isSameItemSameComponents(before, after)
                            && after.getCount() == before.getCount()
                            && after.getDamageValue() == before.getDamageValue(),
                    "slot " + slot + " must survive the save/reload intact (item, count and damage), got "
                            + before + " -> " + after);
        }
        for (final Aspect aspect : seeded) {
            helper.assertTrue(restored.getBonus(aspect) == 1,
                    "the pool entry for " + aspect.getTag() + " comes back at 1: the write ships one entry "
                            + "per positive aspect (:83-92) and the read merges 1 per entry (:57-65), so the "
                            + "pool survives as a set of aspects — the original's own rule, in BD-19");
        }
        helper.assertTrue(restored.getBonus(stacked) == 1,
                "a pool above the original's 1-per-aspect domain collapses to 1 across the save exactly as "
                        + "the original's write/read pair does it: 3 -> " + restored.getBonus(stacked));
        helper.assertTrue(restored.getBonus(unseeded) == 0,
                "an aspect the table never held loads as 0, not as a phantom entry");
        helper.assertTrue(positiveBonusEntries(restored) == positiveBonusEntries(table),
                "the reloaded table holds exactly the bonus entries the saved one shipped");
        helper.assertTrue(restored.getNextRecalc() == 137,
                "the original's nextRecalc key is read (:55), got " + restored.getNextRecalc());
        final CompoundTag resaved = restored.saveWithFullMetadata(provider);
        helper.assertTrue(resaved.getInt("nextRecalc") == 137,
                "and written back by the next save (:82), got " + resaved.getInt("nextRecalc"));
        helper.assertTrue(restored.getBonusAspects() != table.getBonusAspects(),
                "the reload builds a fresh pool object, not a view of the live table's");
        restored.getBonusAspects().add(unseeded, 4);
        helper.assertTrue(table.getBonus(unseeded) == 0,
                "writing to the reloaded pool cannot change the table it was saved from: "
                        + table.getBonus(unseeded));
        helper.succeed();
    }

    /**
     * TC-M7-04-m2b-ii — the container contract across a save/reload, anchored on the
     * registered {@code thaumcraft:gui_research_table} menu type: the same window the
     * block opens has to come back out of the reloaded tile with the same slots, the
     * same anchors, the same out-of-ink condition and the same synced rows, and the
     * bonus column must be the SAVED pool (the live table's pool is wiped after the
     * save, so anything it shows can only have come off the tag).
     */
    @GameTest(template = "gametest_pad_32")
    public static void theMenuContractSurvivesASaveLoadRoundTrip(final GameTestHelper helper) {
        final ResourceLocation menuId = ResourceLocation.fromNamespaceAndPath(
                ThaumcraftClassic.MOD_ID, "gui_research_table");
        helper.assertTrue(BuiltInRegistries.MENU.containsKey(menuId),
                "TC-M7-04-m2b-ii: the menu type " + menuId + " is not registered, so the container this "
                        + "probe round-trips has no window the client could ever open");
        final MenuType<?> registeredType = BuiltInRegistries.MENU.get(menuId);
        helper.assertTrue(registeredType != null, "the registered menu type must resolve, got null");

        final HolderLookup.Provider provider = helper.getLevel().registryAccess();
        // Two cells: the live table and the cell its saved tag reloads into.
        final BlockPos reloadCell = new BlockPos(4, 1, 1);
        final TileResearchTable table = tableAt(helper, reloadCell);

        // Registry-derived seeds: the first three aspects the registry publishes get a
        // row in the player's knowledge and a bonus entry on the table.
        final List<Aspect> seeded = Aspect.values().subList(0, 3);
        helper.assertTrue(seeded.size() == 3, "the aspect registry publishes at least 3 aspects");
        final Map<String, Integer> pool = new LinkedHashMap<>();
        for (int i = 0; i < seeded.size(); i++) {
            pool.put(seeded.get(i).getTag(), i + 2);
        }
        final Player player = seededPlayer(helper, List.of("RESEARCHER1"), pool);
        for (int i = 0; i < seeded.size(); i++) {
            table.getBonusAspects().add(seeded.get(i), 1);
        }
        final ItemStack spent = new ItemStack(Items.IRON_PICKAXE);
        spent.setDamageValue(spent.getMaxDamage());
        table.setItem(TileResearchTable.SLOT_INK, spent);
        // Snapshot the expectation: emptying the cell to reload the tile DROPS the live
        // tile's contents (the block's dropResources), and the drop mutates the very
        // stack object the tile was holding. Everything after the reload is compared
        // against this snapshot, not against the live stack.
        final ItemStack expectedInk = table.getItem(TileResearchTable.SLOT_INK).copy();
        helper.assertTrue(!expectedInk.isEmpty() && expectedInk.getDamageValue() == spent.getMaxDamage(),
                "the table took the spent tool into its scribing slot, got " + expectedInk);

        final ResearchTableMenu menu = (ResearchTableMenu) table.createMenu(1, player.getInventory(), player);
        menu.broadcastChanges();
        helper.assertTrue(menu.getDiscoveredCount() == seeded.size(),
                "the seeded knowledge fills " + seeded.size() + " rows, got " + menu.getDiscoveredCount());
        helper.assertTrue(menu.isOutOfInk(),
                "a tool at full damage is the original's second out-of-ink half (GuiResearchTable.java:213-228)");
        // Which container slot carries the scribing stack is read off the live container
        // now: after the reload the live tile is unloaded (its contents drop), so this is
        // the only moment the live layout can be observed.
        final int liveInkIndex = slotHolding(menu.slots, expectedInk);
        helper.assertTrue(liveInkIndex >= 0,
                "the live container carries the scribing stack in one of its slots, got " + liveInkIndex);

        final CompoundTag tag = table.saveWithFullMetadata(provider);

        // Wipe the LIVE table's bonus pool after the save: the reloaded container's
        // bonus column can then only be the persisted pool.
        for (int i = 0; i < seeded.size(); i++) {
            table.getBonusAspects().remove(seeded.get(i));
        }
        helper.assertTrue(table.getBonus(seeded.get(0)) == 0, "the live table's pool is empty before the reload");

        final TileResearchTable restored = reloadSavedTableInWorld(helper, tag, reloadCell);
        final ResearchTableMenu reloaded =
                (ResearchTableMenu) restored.createMenu(1, player.getInventory(), player);
        reloaded.broadcastChanges();

        // The contract is read off the two containers, not off a list in this file.
        helper.assertTrue(reloaded.slots.size() == menu.slots.size(),
                "the reloaded table builds the same container: " + menu.slots.size() + " slots, got "
                        + reloaded.slots.size());
        for (int i = 0; i < menu.slots.size(); i++) {
            final Slot was = menu.slots.get(i);
            final Slot now = reloaded.slots.get(i);
            helper.assertTrue(now.x == was.x && now.y == was.y,
                    "slot " + i + " keeps the original's anchor (" + was.x + "," + was.y + "), the reload "
                            + "built it at (" + now.x + "," + now.y + ")");
        }
        // The expectation for the scribing stack is the SAVED stack, read back out of the
        // tag: the live tile is unloaded by the reload, so the tag is what defines the
        // contract. Which slot carries it is read off the container, not assumed.
        ItemStack savedInk = ItemStack.EMPTY;
        final ListTag savedInventory = tag.getList("Inventory", Tag.TAG_COMPOUND);
        helper.assertTrue(!savedInventory.isEmpty(), "the saved tag carries the table's stacks (:43-65)");
        for (int i = 0; i < savedInventory.size(); i++) {
            final CompoundTag entry = savedInventory.getCompound(i);
            final ItemStack parsed = ItemStack.parseOptional(provider, entry.getCompound("Item"));
            if (parsed.is(expectedInk.getItem())) {
                savedInk = parsed;
                helper.assertTrue((entry.getByte("Slot") & 255) == TileResearchTable.SLOT_INK,
                        "the scribing stack is saved with the slot byte the tile reads it back at "
                                + "(:47-52), got " + (entry.getByte("Slot") & 255));
            }
        }
        helper.assertTrue(!savedInk.isEmpty()
                        && savedInk.getCount() == expectedInk.getCount()
                        && savedInk.getDamageValue() == expectedInk.getDamageValue(),
                "the tag carries the scribing stack as it was written: count " + savedInk.getCount()
                        + " (saved " + expectedInk.getCount() + "), damage " + savedInk.getDamageValue()
                        + " (saved " + expectedInk.getDamageValue() + ")");
        final int reloadedInkIndex = slotHolding(reloaded.slots, savedInk);
        helper.assertTrue(reloadedInkIndex >= 0 && liveInkIndex == reloadedInkIndex,
                "the scribing stack sits in the same container slot before and after the reload: "
                        + liveInkIndex + " -> " + reloadedInkIndex + " (loaded stack " + savedInk + ")");
        helper.assertTrue(ItemStack.isSameItemSameComponents(reloaded.slots.get(reloadedInkIndex).getItem(),
                        savedInk)
                        && reloaded.slots.get(reloadedInkIndex).getItem().getDamageValue()
                                == expectedInk.getMaxDamage(),
                "the container's scribing slot is the reloaded tile's stack, damage and all: loaded "
                        + reloaded.slots.get(reloadedInkIndex).getItem() + " (damage "
                        + reloaded.slots.get(reloadedInkIndex).getItem().getDamageValue()
                        + "), saved " + savedInk);
        helper.assertTrue(reloaded.isOutOfInk(),
                "the out-of-ink condition rides the reloaded tile — the screen reads it through the container");
        helper.assertTrue(reloaded.getDiscoveredCount() == menu.getDiscoveredCount(),
                "the row count is the same container: " + menu.getDiscoveredCount() + " -> "
                        + reloaded.getDiscoveredCount());
        for (int row = 0; row < menu.getDiscoveredCount(); row++) {
            helper.assertTrue(reloaded.aspectAt(row) == menu.aspectAt(row)
                            && reloaded.poolAt(row) == menu.poolAt(row)
                            && reloaded.bonusAt(row) == menu.bonusAt(row),
                    "row " + row + " survives the reload: (" + menu.aspectAt(row) + "," + menu.poolAt(row) + ","
                            + menu.bonusAt(row) + ") -> (" + reloaded.aspectAt(row) + "," + reloaded.poolAt(row)
                            + "," + reloaded.bonusAt(row) + ")");
        }
        final int rowZero = rowOf(reloaded, seeded.get(0));
        helper.assertTrue(rowZero >= 0 && reloaded.bonusAt(rowZero) == 1 && restored.getBonus(seeded.get(0)) == 1,
                "the bonus column is the SAVED pool: the live table is wiped to " + table.getBonus(seeded.get(0))
                        + ", the reloaded tile reads " + restored.getBonus(seeded.get(0)) + " and the container "
                        + (rowZero < 0 ? "has no row for it" : "shows " + reloaded.bonusAt(rowZero)));
        helper.assertTrue(restored.createMenu(3, player.getInventory(), player) instanceof ResearchTableMenu
                        && restored.getDisplayName().getContents() instanceof TranslatableContents contents
                        && "container.thaumcraft.gui_research_table".equals(contents.getKey()),
                "the reloaded tile is still the MenuProvider the registered " + menuId + " window opens through");
        helper.succeed();
    }

    /**
     * TC-M7-04-m2b-ii — the original's three load filters, run on a save this probe
     * builds by hand: the read stops at {@code Math.min(2, n)} entries
     * (TileResearchTable.java:46), the {@code Slot} byte is range-checked (:50), and a
     * bonus tag the aspect registry cannot resolve is dropped (:62). The expectations
     * are derived from the registry (the bogus tag is asserted unresolvable first) and
     * from the tile's own container size.
     */
    @GameTest(template = "empty")
    public static void theOriginalsLoadFiltersRunOnAConstructedSave(final GameTestHelper helper) {
        final HolderLookup.Provider provider = helper.getLevel().registryAccess();
        final TileResearchTable table = tableAt(helper);
        final CompoundTag tag = table.saveWithFullMetadata(provider);

        final Aspect known = Aspect.values().get(0);
        final String bogusTag = "tc_m7_04_m2b_ii_not_an_aspect";
        helper.assertTrue(Aspect.getAspect(bogusTag) == null,
                "the bogus tag must not resolve in the aspect registry — the probe's expectation is derived "
                        + "from the registry, not assumed");

        final ListTag items = new ListTag();
        items.add(inventoryEntry(TileResearchTable.SLOT_NOTES, new ItemStack(Items.PAPER, 4), provider));
        items.add(inventoryEntry(99, new ItemStack(Items.BOOK), provider));
        items.add(inventoryEntry(TileResearchTable.SLOT_INK, new ItemStack(Items.ACACIA_BOAT), provider));
        tag.put("Inventory", items);

        final ListTag bonus = new ListTag();
        final CompoundTag resolvable = new CompoundTag();
        resolvable.putString("tag", known.getTag());
        bonus.add(resolvable);
        final CompoundTag unresolvable = new CompoundTag();
        unresolvable.putString("tag", bogusTag);
        bonus.add(unresolvable);
        bonus.add(new CompoundTag());
        tag.put("bonusAspects", bonus);

        final TileResearchTable restored = reloadSavedTable(helper, tag);
        helper.assertTrue(ItemStack.isSameItem(restored.getItem(TileResearchTable.SLOT_NOTES),
                        new ItemStack(Items.PAPER))
                        && restored.getItem(TileResearchTable.SLOT_NOTES).getCount() == 4,
                "the first entry loads into the slot its Slot byte names (:48-52), got "
                        + restored.getItem(TileResearchTable.SLOT_NOTES));
        helper.assertTrue(restored.getItem(TileResearchTable.SLOT_INK).isEmpty(),
                "the read stops at Math.min(2, n) entries (:46), so the third entry never loads, got "
                        + restored.getItem(TileResearchTable.SLOT_INK));
        helper.assertTrue(positiveBonusEntries(restored) == 1 && restored.getBonus(known) == 1,
                "the resolvable tag merges once (:57-65) and the unresolvable and keyless entries are dropped "
                        + "(:62), got " + positiveBonusEntries(restored) + " entries");
        helper.succeed();
    }
}
