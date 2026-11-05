package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.menus.ResearchNoteMenu;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPlaceToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ResearchCompletePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.Cell;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.Chain;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.Request;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.TrayCell;
import com.jonnyutah12.thaumcraftclassic.research.NotePuzzle;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraphLoader;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/**
 * The research-note hex grid, measured (NOTE-SCREEN-2, closes BD-20).
 *
 * <p>The screen is client-only, so the presentation is measured through the
 * model the screen draws and sends: {@link NoteGridView}. Every probe states a
 * claim a player can see — the grid draws one cell per note cell, the cell
 * under the cursor is the cell that lights, a blank cell takes a write and a
 * typed cell takes an erase, a required cell takes nothing, the chain lights
 * the cells a write connects, the copy plate and the cost panel sit where the
 * menu's copy contract lights, the tray pages the player's aspects, the ink
 * lines light only while the tool is dry — and drives it with real shipped
 * state: a note the shipped generator produced from a shipped graph entry, a
 * real {@link ServerPlayer}, the shipped write arm, the shipped payload
 * codec.</p>
 *
 * <p>The two probes that carry the client write arm are the strongest pair:
 * they take the {@link Request} the model answers for a cursor position, put it
 * on the wire as {@code thaumcraft:aspect_place_to_server}, decode it back and
 * hand it to the open menu — the chain the screen runs, minus the packet
 * enqueue. The record's own component list is pinned in the same probe: the
 * payload the screen can send carries a position, a cell and a tag, and no
 * completion field, so the client cannot claim a finished note
 * ({@code ResearchManager.completeResearch} is the only completer).</p>
 *
 * <p>No expected coordinate, colour, cost or page count is typed here: each is
 * read from the shipped note, entry, knowledge, pool, aspect definitions, menu
 * contract and payload record. The decompiled-original parity of the geometry
 * constants is pinned by {@code ResearchNoteScreenGridParityTest}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchNoteGridGameTest {

    private static final String NOTE_KEY = "item." + ThaumcraftClassic.MOD_ID + ".itemresearchnotes";
    private static final int META_COMPLETED = 64;
    private static final Predicate<String> EVERYTHING = tag -> true;

    private ResearchNoteGridGameTest() {
    }

    // ------------------------------------------------------------- the grid

    @GameTest(template = "empty")
    public static void everyCellOfAGeneratedPuzzleDrawsOneView(final GameTestHelper helper) {
        // A note the shipped generator produced from a shipped graph entry —
        // the grid the screen has to draw, derived and not typed.
        final ResearchEntryData entry = taggedEntry();
        final ResearchNoteData note = NotePuzzle.generate(entry, new Random(20261105L));
        helper.assertTrue(note != null && !note.hexEntries().isEmpty(),
                "the shipped generator produced no grid for " + entry.key());

        final List<Cell> cells = NoteGridView.cells(note, EVERYTHING, NoteGridView.chain(note, EVERYTHING), null);
        helper.assertTrue(cells.size() == note.hexEntries().size(),
                "the grid drew " + cells.size() + " cells for a note with " + note.hexEntries().size()
                        + " hexEntries");

        final List<String> drawnKeys = new ArrayList<>();
        for (final Cell cell : cells) {
            final HexEntry entryCell = note.entryAt(new Hex(cell.q(), cell.r()));
            helper.assertTrue(entryCell != null, "the grid drew a cell the note has no entry for: " + cell.key());
            helper.assertTrue(cell.key().equals(new Hex(cell.q(), cell.r()).toString()),
                    "the cell key " + cell.key() + " is not the \"q:r\" key of (" + cell.q() + "," + cell.r() + ")");
            helper.assertTrue(entryCell.type() == cell.type(),
                    "cell " + cell.key() + " draws type " + cell.type() + ", the note says " + entryCell.type());
            helper.assertTrue(java.util.Objects.equals(entryCell.aspectTag(), cell.aspectTag()),
                    "cell " + cell.key() + " draws " + cell.aspectTag() + ", the note says " + entryCell.aspectTag());
            helper.assertTrue(cell.centerX() == NoteGridView.centerX(new Hex(cell.q(), cell.r()))
                            && cell.centerY() == NoteGridView.centerY(new Hex(cell.q(), cell.r())),
                    "cell " + cell.key() + " is not at the model's pixel offset for its hex");
            helper.assertTrue(cell.left() == cell.centerX() + NoteGridView.TAG_DX
                            && cell.top() == cell.centerY() + NoteGridView.TAG_DY,
                    "cell " + cell.key() + " draws its 16px quad at (" + cell.left() + "," + cell.top()
                            + "), not " + NoteGridView.TAG_DX + "," + NoteGridView.TAG_DY + " from its centre");
            if (cell.aspectTag() != null) {
                final int expected = NoteGridView.AspectColour.of(cell.aspectTag());
                helper.assertTrue(cell.colour() == expected,
                        "cell " + cell.key() + " draws 0x" + Integer.toHexString(cell.colour()) + ", the shipped "
                                + "aspect definition for " + cell.aspectTag() + " says 0x" + Integer.toHexString(expected));
            }
            drawnKeys.add(cell.key());
        }

        helper.assertTrue(new LinkedHashSet<>(drawnKeys).size() == drawnKeys.size(),
                "the grid drew a cell twice: " + drawnKeys);
        // The map the note is carried in is an immutable copy with no defined
        // iteration order; the draw order has to be the model's own, stable.
        helper.assertTrue(NoteGridView.cells(note, EVERYTHING, Chain.empty(), null).equals(cells),
                "the grid's draw order moved between two frames of the same note");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theGridsHitTestLandsOnTheCellItDraws(final GameTestHelper helper) {
        final ResearchEntryData entry = taggedEntry();
        final ResearchNoteData note = NotePuzzle.generate(entry, new Random(7L));
        final List<Cell> cells = NoteGridView.cells(note, EVERYTHING, Chain.empty(), null);
        helper.assertTrue(!cells.isEmpty(), "the generated note drew no grid");

        int checked = 0;
        for (final Cell cell : cells) {
            final Hex hex = new Hex(cell.q(), cell.r());
            helper.assertTrue(hex.equals(NoteGridView.hexAt(cell.centerX(), cell.centerY())),
                    "the hit test at the centre of cell " + cell.key() + " landed on "
                            + NoteGridView.hexAt(cell.centerX(), cell.centerY()));
            final Cell hit = NoteGridView.cellAt(note, cell.centerX(), cell.centerY(), EVERYTHING, Chain.empty());
            helper.assertTrue(hit != null && cell.key().equals(hit.key()),
                    "a click at the centre of cell " + cell.key() + " hit " + (hit == null ? "nothing" : hit.key()));
            // The original's hex at size 9 is ~18 wide and ~15 tall: a click a
            // few pixels off the centre has to stay on the same cell.
            for (final int[] offset : new int[][]{{5, 0}, {-5, 0}, {0, 5}, {0, -5}}) {
                final Cell near = NoteGridView.cellAt(note, cell.centerX() + offset[0],
                        cell.centerY() + offset[1], EVERYTHING, Chain.empty());
                helper.assertTrue(near != null && cell.key().equals(near.key()),
                        "a click " + offset[0] + "," + offset[1] + " from the centre of " + cell.key()
                                + " landed on " + (near == null ? "nothing" : near.key()));
            }
            checked++;
        }
        helper.assertTrue(checked == cells.size(), "the hit test checked " + checked + " of " + cells.size() + " cells");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aBlankCellTakesAWriteATypedCellAnEraseARequiredCellNothing(final GameTestHelper helper) {
        final ResearchNoteData note = gridOf(
                cell(0, 0, "ignis", NoteGridView.TYPE_REQUIRED),
                cell(1, 0, null, NoteGridView.TYPE_BLANK),
                cell(2, 0, "lux", NoteGridView.TYPE_TYPED),
                cell(3, 0, "aer", NoteGridView.TYPE_REQUIRED));

        // The drop arm (GuiResearchTable.java:147-172): a blank cell and an
        // aspect in hand writes that aspect at the cell's (q, r).
        final Cell blank = cellAt(note, 1, 0);
        final Request write = NoteGridView.requestAt(note, blank.centerX(), blank.centerY(), "lux", EVERYTHING);
        helper.assertTrue(write.writes() && write.q() == 1 && write.r() == 0 && "lux".equals(write.aspectTag()),
                "a click on a blank cell with an armed aspect answered " + write);

        // The same click with nothing armed writes nothing — the original only
        // sends the packet when its drag carries an aspect.
        final Request unarmed = NoteGridView.requestAt(note, blank.centerX(), blank.centerY(), null, EVERYTHING);
        helper.assertTrue(unarmed.arm() == NoteGridView.Arm.NONE,
                "a click on a blank cell with no aspect armed sent " + unarmed);

        // The erase arm (checkClickedHex, :726-740): a typed cell, no gate on
        // what is armed — the server owns the ink.
        final Cell typed = cellAt(note, 2, 0);
        for (final String armed : new String[]{null, "lux"}) {
            final Request erase = NoteGridView.requestAt(note, typed.centerX(), typed.centerY(), armed, EVERYTHING);
            helper.assertTrue(erase.erases() && erase.q() == 2 && erase.r() == 0 && erase.aspectTag() == null,
                    "a click on a typed cell with " + armed + " armed answered " + erase);
        }

        // A required cell is printed on the note: the original has no arm for it.
        for (final int q : new int[]{0, 3}) {
            final Cell required = cellAt(note, q, 0);
            final Request click = NoteGridView.requestAt(note, required.centerX(), required.centerY(), "lux", EVERYTHING);
            helper.assertTrue(click.arm() == NoteGridView.Arm.NONE,
                    "a click on the required cell " + required.key() + " answered " + click);
        }

        // A click off the grid arms nothing.
        final Cell first = cellAt(note, 0, 0);
        helper.assertTrue(NoteGridView.requestAt(note, first.centerX() + 120, first.centerY(), "lux", EVERYTHING)
                        .arm() == NoteGridView.Arm.NONE,
                "a click far off the grid armed an arm");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aCompletedNoteTakesNoWrite(final GameTestHelper helper) {
        // The original's drop arm needs a type-0 cell, and a completed note has
        // none (NotePuzzle.checkCompletion prunes them); the server's write arm
        // refuses a completed note too, so the client must not send one.
        final ResearchNoteData open = gridOf(
                cell(0, 0, "ignis", NoteGridView.TYPE_REQUIRED),
                cell(1, 0, null, NoteGridView.TYPE_BLANK));
        final Cell blank = cellAt(open, 1, 0);
        helper.assertTrue(NoteGridView.requestAt(open, blank.centerX(), blank.centerY(), "lux", EVERYTHING).writes(),
                "the open note stopped taking writes: the completed gate leaked");

        final ResearchNoteData done = open.withComplete(true);
        final Request late = NoteGridView.requestAt(done, blank.centerX(), blank.centerY(), "lux", EVERYTHING);
        helper.assertTrue(late.arm() == NoteGridView.Arm.NONE,
                "a completed note still armed a write: " + late);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theChainLightsTheCellsAWriteConnects(final GameTestHelper helper) {
        // The shipped chain rule: ignis at 0:0 and 2:0, two steps apart, with
        // lux (aer + ignis) written between them. The connection walk the screen
        // draws is the walk NotePuzzle.checkCompletion validates.
        final ResearchNoteData note = gridOf(
                cell(0, 0, "ignis", NoteGridView.TYPE_REQUIRED),
                cell(1, 0, "lux", NoteGridView.TYPE_TYPED),
                cell(2, 0, "ignis", NoteGridView.TYPE_REQUIRED));
        final Predicate<String> knows = tag -> tag.equals("ignis") || tag.equals("lux");

        final Chain chain = NoteGridView.chain(note, knows);
        helper.assertTrue(chain.links().size() == 2,
                "a closed ignis-lux-ignis chain drew " + chain.links().size() + " lines, the rule connects 2");
        final List<Cell> cells = NoteGridView.cells(note, knows, chain, null);
        for (final NoteGridView.Link link : chain.links()) {
            helper.assertTrue(centreAt(cells, link.x1(), link.y1()) != null && centreAt(cells, link.x2(), link.y2()) != null,
                    "a chain line runs to (" + link.x1() + "," + link.y1() + ")-(" + link.x2() + "," + link.y2()
                            + "), which is not a pair of cell centres");
        }
        helper.assertTrue(chain.highlighted().contains("1:0") && chain.highlighted().contains("2:0"),
                "the chain left the cells it connected unlit: " + chain.highlighted());
        for (final Cell cell : cells) {
            if (cell.key().equals("1:0") || cell.key().equals("2:0")) {
                helper.assertTrue(cell.highlighted(), "cell " + cell.key() + " is in the chain and drew unlit");
            }
        }

        // The same grid with lux undiscovered: nothing connects, and the cell
        // draws the original's black unknown overlay (which is drawn INSTEAD of
        // the faded tag: GuiResearchTable.java:538-562 tries the overlay first).
        final Chain blind = NoteGridView.chain(note, tag -> tag.equals("ignis"));
        helper.assertTrue(blind.links().isEmpty() && blind.highlighted().isEmpty(),
                "a chain through an undiscovered aspect drew lines: " + blind.links());
        final Cell overlay = NoteGridView.cells(note, tag -> tag.equals("ignis"), blind, null).stream()
                .filter(cell -> cell.key().equals("1:0")).findFirst().orElseThrow();
        helper.assertTrue(overlay.unknown(), "an undiscovered aspect's cell did not ask for the unknown overlay");

        // A DISCOVERED typed cell the chain never reaches is the original's
        // 0.66-faded draw: ignis and aer are both primals, so no link exists.
        final ResearchNoteData primals = gridOf(
                cell(0, 0, "ignis", NoteGridView.TYPE_REQUIRED),
                cell(1, 0, "aer", NoteGridView.TYPE_TYPED));
        final Chain noLink = NoteGridView.chain(primals, EVERYTHING);
        helper.assertTrue(noLink.links().isEmpty(), "two primal aspects drew a connection line");
        final Cell faded = NoteGridView.cells(primals, EVERYTHING, noLink, null).stream()
                .filter(cell -> cell.key().equals("1:0")).findFirst().orElseThrow();
        helper.assertTrue(!faded.unknown() && !faded.highlighted(),
                "the faded probe is not a discovered, unlit typed cell");
        helper.assertTrue(faded.fillColour() >>> 24 == NoteGridView.TYPED_ALPHA,
                "an unlit typed cell draws at alpha 0x" + Integer.toHexString(faded.fillColour() >>> 24)
                        + ", the original draws it at 0.66 (GuiResearchTable.java:556-560)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aCellThePlayerHasNotDiscoveredDrawsTheUnknownOverlay(final GameTestHelper helper) {
        final ResearchNoteData note = gridOf(
                cell(0, 0, "cognitio", NoteGridView.TYPE_REQUIRED),
                cell(1, 0, "ignis", NoteGridView.TYPE_REQUIRED));
        final Predicate<String> knowsIgnis = tag -> tag.equals("ignis");
        final List<Cell> cells = NoteGridView.cells(note, knowsIgnis, NoteGridView.chain(note, knowsIgnis), null);
        helper.assertTrue(cells.size() == 2, "the grid drew " + cells.size() + " cells for a two-cell note");
        for (final Cell cell : cells) {
            if (cell.aspectTag().equals("cognitio")) {
                helper.assertTrue(cell.unknown(), "an undiscovered aspect drew its tag instead of the unknown overlay");
            } else {
                helper.assertTrue(!cell.unknown(), "a discovered aspect drew the unknown overlay");
            }
        }
        // Hover is the original's own hit test: only the cell under the cursor
        // lights its highlight quad.
        final Cell lit = cellAt(note, 1, 0);
        final List<Cell> hovered = NoteGridView.cells(note, knowsIgnis, Chain.empty(), lit.key());
        helper.assertTrue(hovered.stream().filter(Cell::hovered).count() == 1,
                "the hover pass lit " + hovered.stream().filter(Cell::hovered).count() + " cells, the original lights one");
        helper.succeed();
    }

    // -------------------------------------------------- the client write arm

    @GameTest(template = "empty")
    public static void aGridRequestTravelsThePayloadIntoTheOpenMenusGrid(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final SimpleContainerData data = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu menu = new ResearchNoteMenu(1, player.getInventory(), data);
        final ItemStack inkwell = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).set(inkwell);
        setKnowledge(player, PlayerKnowledgeData.empty()
                .withAspectDiscovered("ignis", 5).withAspectDiscovered("lux", 5));
        final ResearchNoteData note = chainNote();
        menu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(noteStack(note, 0));

        // The cursor on the blank middle cell, lux armed: the model's answer.
        final Cell blank = cellAt(note, 1, 0);
        final Request request = NoteGridView.requestAt(note, blank.centerX(), blank.centerY(), "lux",
                tag -> tag.equals("ignis") || tag.equals("lux"));
        helper.assertTrue(request.writes() && request.q() == 1 && request.r() == 0,
                "the grid did not arm a write on the blank cell: " + request);

        // The payload the screen sends for that arm, on the wire and back, into
        // the open menu — the chain the screen runs minus the packet enqueue.
        final AspectPlaceToServerPayload sent =
                new AspectPlaceToServerPayload(BlockPos.ZERO, Optional.of(request.aspectTag()), request.q(), request.r());
        final AspectPlaceToServerPayload decoded = wire(sent);
        helper.assertTrue(AspectPlaceToServerPayload.TYPE.id().toString().equals("thaumcraft:aspect_place_to_server"),
                "the write arm is not the original's packet: " + AspectPlaceToServerPayload.TYPE.id());
        helper.assertTrue(decoded.q() == 1 && decoded.r() == 0 && decoded.aspectTag().equals(Optional.of("lux")),
                "the payload came off the wire as " + decoded);
        menu.placeAspect(decoded.q(), decoded.r(), decoded.aspectTag().orElse(null), player);

        final ResearchNoteData written = ResearchNoteMenu.noteOf(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem());
        final HexEntry cell = written.entryAt(new Hex(1, 0));
        helper.assertTrue(cell != null && "lux".equals(cell.aspectTag()) && cell.type() == NoteGridView.TYPE_TYPED,
                "the write did not land on the note: " + cell + " grid=" + written.hexEntries());
        helper.assertTrue(inkwell.getDamageValue() == 1,
                "the write spent " + inkwell.getDamageValue() + " ink points, the shipped arm spends 1");
        helper.assertTrue(player.getData(ModAttachments.PLAYER_KNOWLEDGE).aspectAmount("lux") == 4,
                "the write paid " + (5 - player.getData(ModAttachments.PLAYER_KNOWLEDGE).aspectAmount("lux"))
                        + " from the pool, the shipped arm pays 1");
        // ignis-lux-ignis closes, and the SERVER stamps it: the payload the
        // client can send has no completion field at all.
        helper.assertTrue(written.complete() && data.get(ResearchNoteMenu.DATA_COMPLETE) == 1,
                "the closed chain did not complete through the server's check: complete=" + written.complete()
                        + " data=" + data.get(ResearchNoteMenu.DATA_COMPLETE));

        final List<String> fields = Arrays.stream(AspectPlaceToServerPayload.class.getRecordComponents())
                .map(RecordComponent::getName).toList();
        helper.assertTrue(fields.equals(List.of("tablePos", "aspectTag", "q", "r")),
                "the write payload grew a field the client could claim with: " + fields);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theEraseRequestTravelsTheSamePayloadAndClearsTheCell(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final SimpleContainerData data = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu menu = new ResearchNoteMenu(1, player.getInventory(), data);
        menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).set(new ItemStack(M3ResearchContent.ITEM_INKWELL.get()));
        setKnowledge(player, PlayerKnowledgeData.empty()
                .withAspectDiscovered("ignis", 5).withAspectDiscovered("aer", 5));
        menu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(noteStack(chainNote(), 0));
        menu.placeAspect(1, 0, "aer", player);
        final ResearchNoteData typed = ResearchNoteMenu.noteOf(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem());
        helper.assertTrue(typed.entryAt(new Hex(1, 0)).type() == NoteGridView.TYPE_TYPED,
                "the erase probe's setup did not leave a typed cell");

        final Cell cell = cellAt(typed, 1, 0);
        final Request erase = NoteGridView.requestAt(typed, cell.centerX(), cell.centerY(), null, EVERYTHING);
        helper.assertTrue(erase.erases(), "a click on the typed cell did not arm an erase: " + erase);

        final AspectPlaceToServerPayload decoded =
                wire(new AspectPlaceToServerPayload(BlockPos.ZERO, Optional.empty(), erase.q(), erase.r()));
        helper.assertTrue(decoded.aspectTag().isEmpty(), "the erase payload carried " + decoded.aspectTag());
        menu.placeAspect(decoded.q(), decoded.r(), decoded.aspectTag().orElse(null), player);

        final ResearchNoteData cleared = ResearchNoteMenu.noteOf(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem());
        final HexEntry back = cleared.entryAt(new Hex(1, 0));
        helper.assertTrue(back != null && back.aspectTag() == null && back.type() == NoteGridView.TYPE_BLANK,
                "the erase did not clear the cell: " + back);
        helper.succeed();
    }

    // ------------------------------------------------- plate, costs, tray, ink

    @GameTest(template = "empty")
    public static void theCopyPlateAndCostPanelFrameTheMenusCopyContract(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final ResearchEntryData entry = taggedEntry();
        final SimpleContainerData data = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu menu = new ResearchNoteMenu(1, player.getInventory(), data);
        menu.getSlot(ResearchNoteMenu.SLOT_NOTE)
                .set(noteStack(new ResearchNoteData(entry.key(), 1, true, 0, twoCells()), META_COMPLETED));
        setKnowledge(player, fundedPool(entry, 0).withResearchCompleted("RESEARCHDUPE"));
        helper.assertTrue(menu.canDuplicate(),
                "the copy plate's gate is dark on a completed note with RESEARCHDUPE: the plate would never draw");

        // The plate's 24x24 box at (37, 5) and the cost panel under it.
        helper.assertTrue(NoteGridView.inPlate(NoteGridView.PLATE_X, NoteGridView.PLATE_Y),
                "the plate's own top-left pixel is outside its box");
        helper.assertTrue(NoteGridView.inPlate(NoteGridView.PLATE_X + NoteGridView.PLATE_SPAN - 1,
                        NoteGridView.PLATE_Y + NoteGridView.PLATE_SPAN - 1),
                "the plate's bottom-right pixel is outside its box");
        helper.assertTrue(!NoteGridView.inPlate(NoteGridView.PLATE_X + NoteGridView.PLATE_SPAN, NoteGridView.PLATE_Y),
                "the plate box runs past its right edge");
        helper.assertTrue(!NoteGridView.inPlate(NoteGridView.PLATE_X, NoteGridView.PLATE_Y - 1),
                "the plate box runs above the panel's top edge");
        helper.assertTrue(NoteGridView.PLATE_X + NoteGridView.PLATE_SPAN <= NoteGridView.MAIN_PANEL_W
                        && NoteGridView.PLATE_Y + NoteGridView.PLATE_SPAN <= NoteGridView.MAIN_PANEL_H,
                "the copy plate falls outside the parchment panel");

        // The cost rows the menu publishes all fit the window at the tray step.
        final Map<String, Integer> costs = menu.copyCosts();
        helper.assertTrue(!costs.isEmpty(), "a tagged entry published no copy cost to draw");
        final int lastTagRight = NoteGridView.COST_TAG_X + (costs.size() - 1) * NoteGridView.COST_TAG_STEP
                + NoteGridView.CELL_SPAN;
        helper.assertTrue(lastTagRight <= NoteGridView.WINDOW_SPAN,
                "the cost panel spills off the window: " + costs.size() + " tags reach x " + lastTagRight
                        + " of " + NoteGridView.WINDOW_SPAN);
        helper.assertTrue(NoteGridView.COST_TAG_X == NoteGridView.COST_PANEL_X + NoteGridView.COST_PANEL_W
                        && NoteGridView.COST_TAG_STEP == NoteGridView.CELL_SPAN,
                "the cost tags do not start at the cost panel's right edge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theTrayPagesThePlayersAspectsAndFadesAnEmptyPool(final GameTestHelper helper) {
        final List<String> tags = AspectDefinitions.all().stream().map(AspectDefinitions.Definition::tag)
                .sorted().toList();
        helper.assertTrue(tags.size() > 30, "the shipped aspect list is too short to page: " + tags.size());
        final List<String> discovered = new ArrayList<>(tags.subList(0, 30));
        final String dry = discovered.get(3);
        final Map<String, Integer> pools = new LinkedHashMap<>();
        for (final String tag : discovered) {
            pools.put(tag, tag.equals(dry) ? 0 : 5);
        }

        final List<TrayCell> page0 = NoteGridView.tray(discovered, 0, pools::get, null);
        helper.assertTrue(page0.size() == NoteGridView.TRAY_MAX,
                "the first tray page drew " + page0.size() + " cells, the original draws " + NoteGridView.TRAY_MAX);
        helper.assertTrue(page0.get(0).left() == NoteGridView.TRAY_X && page0.get(0).top() == NoteGridView.TRAY_Y,
                "the tray does not start at its own origin: " + page0.get(0));
        helper.assertTrue(page0.get(5).left() == NoteGridView.TRAY_X + NoteGridView.TRAY_CELL
                        && page0.get(5).top() == NoteGridView.TRAY_Y,
                "the tray is not laid out column-major in 16px cells: " + page0.get(5));

        // The page step is 5, so page 1 opens on the sixth aspect.
        final List<TrayCell> page1 = NoteGridView.tray(discovered, 1, pools::get, null);
        helper.assertTrue(page1.get(0).tag().equals(discovered.get(5)),
                "page 1 opens on " + page1.get(0).tag() + ", the page step opens on " + discovered.get(5));
        helper.assertTrue(NoteGridView.tray(discovered, 6, pools::get, null).isEmpty(),
                "a page past the last page drew cells");
        helper.assertTrue(NoteGridView.lastPage(discovered.size()) == (discovered.size() - 20) / 5,
                "the page count is not the original's (size - 20) / 5: " + NoteGridView.lastPage(discovered.size()));

        // A pool of 0 fades the cell and keeps it from being armed.
        final TrayCell faded = page0.stream().filter(cell -> cell.tag().equals(dry)).findFirst().orElseThrow();
        helper.assertTrue(faded.faded(), "an aspect with an empty pool drew unfaded");
        helper.assertTrue(NoteGridView.trayCellAt(page0, faded.left(), faded.top(), true) == null,
                "a click armed an aspect the player holds none of");
        helper.assertTrue(NoteGridView.trayCellAt(page0, faded.left(), faded.top(), false) != null,
                "the tray's own cell test lost the faded cell");
        final TrayCell funded = page0.get(0);
        helper.assertTrue(!funded.faded()
                        && NoteGridView.trayCellAt(page0, funded.left() + 1, funded.top() + 1, true).tag()
                                .equals(funded.tag()),
                "a click on a funded tray cell did not arm it");
        helper.assertTrue(!NoteGridView.trayCellAt(page0, funded.left() + NoteGridView.TRAY_CELL,
                        funded.top(), true)
                        .equals(funded),
                "the tray cell box is wider than the original's 16px cell");
        helper.assertTrue(NoteGridView.inTray(NoteGridView.TRAY_X, NoteGridView.TRAY_Y)
                        && !NoteGridView.inTray(NoteGridView.TRAY_X + NoteGridView.TRAY_REGION, NoteGridView.TRAY_Y),
                "the tray's 80px region is not the original's");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theInkLinesLightOnlyWhileTheToolIsDry(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final SimpleContainerData data = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu menu = new ResearchNoteMenu(1, player.getInventory(), data);
        menu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(noteStack(puzzleNote(2), 0));

        final ItemStack inkwell = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).set(inkwell);
        // The screen reads the screen-state ints the server fills.
        menu.broadcastChanges();
        helper.assertTrue(menu.notePresent(),
                "the note in the slot did not light DATA_NOTE_PRESENT, so the ink lines can never gate");
        helper.assertTrue(menu.hasInk(), "a fresh shipped inkwell read as out of ink");

        // The original's gate: the tool out of ink (empty slot, or damage at
        // max) with a note in the slot. GuiResearchTable.java:209-226.
        inkwell.setDamageValue(inkwell.getMaxDamage());
        helper.assertTrue(menu.notePresent() && !menu.hasInk(),
                "the ink lines stayed dark with a dry scribe tool and a note in the slot");
        menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).set(ItemStack.EMPTY);
        helper.assertTrue(!menu.hasInk(), "an empty scribe slot read as having ink");

        // The lines draw centred on x 157 at y 84, inside the top panel.
        helper.assertTrue(NoteGridView.INK_TIP_Y + 22 <= NoteGridView.MAIN_PANEL_H
                        && NoteGridView.INK_TIP_X <= NoteGridView.MAIN_PANEL_W,
                "the out-of-ink lines fall outside the panel they are drawn on");
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    private static AspectPlaceToServerPayload wire(final AspectPlaceToServerPayload payload) {
        final ByteBuf buffer = Unpooled.buffer();
        AspectPlaceToServerPayload.STREAM_CODEC.encode(buffer, payload);
        return AspectPlaceToServerPayload.STREAM_CODEC.decode(buffer);
    }

    private static ResearchEntryData taggedEntry() {
        for (final ResearchEntryData entry : ResearchGraphLoader.loadDefault().entries()) {
            if (entry.tags() != null && !entry.tags().isEmpty()) {
                return entry;
            }
        }
        throw new IllegalStateException("the shipped graph has no tagged entry");
    }

    private static PlayerKnowledgeData fundedPool(final ResearchEntryData entry, final int offset) {
        PlayerKnowledgeData knowledge = PlayerKnowledgeData.empty();
        for (final var tag : entry.tags()) {
            knowledge = knowledge.withAspectPoolAdded(tag.aspect(), tag.amount() + offset);
        }
        return knowledge;
    }

    private static void setKnowledge(final ServerPlayer player, final PlayerKnowledgeData knowledge) {
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
    }

    /** One cell of a hand-built note. */
    private record GridCell(Hex hex, HexEntry entry) {
    }

    private static ResearchNoteData gridOf(final GridCell... cells) {
        final Map<String, HexEntry> map = new LinkedHashMap<>();
        for (final GridCell cell : cells) {
            map.put(cell.hex().toString(), cell.entry());
        }
        return new ResearchNoteData(NOTE_KEY, 1, false, 0, map);
    }

    private static GridCell cell(final int q, final int r, final String tag, final int type) {
        return new GridCell(new Hex(q, r), new HexEntry(tag, type));
    }

    /** Required ignis at 0:0 and 2:0 with the blank 1:0 between them. */
    private static ResearchNoteData chainNote() {
        final Map<String, HexEntry> cells = new LinkedHashMap<>();
        cells.put("0:0", new HexEntry("ignis", 1));
        cells.put("2:0", new HexEntry("ignis", 1));
        cells.put("1:0", new HexEntry(null, 0));
        return new ResearchNoteData(NOTE_KEY, 1, false, 0, cells);
    }

    private static Map<String, HexEntry> twoCells() {
        final Map<String, HexEntry> cells = new LinkedHashMap<>();
        cells.put("0:0", new HexEntry("ignis", 1));
        cells.put("1:0", new HexEntry("lux", 1));
        return cells;
    }

    private static ResearchNoteData puzzleNote(final int count) {
        final Map<String, HexEntry> cells = new LinkedHashMap<>();
        int q = 0;
        for (int i = 0; i < count; i++) {
            cells.put(q + ":0", new HexEntry("ignis", 1));
            q++;
        }
        cells.put(count + ":0", new HexEntry(null, 0));
        return new ResearchNoteData(NOTE_KEY, 1, false, 0, cells);
    }

    private static Cell cellAt(final ResearchNoteData note, final int q, final int r) {
        return NoteGridView.viewOf(note, new Hex(q, r), EVERYTHING, Chain.empty(), false);
    }

    private static Cell centreAt(final List<Cell> cells, final int x, final int y) {
        for (final Cell cell : cells) {
            if (cell.centerX() == x && cell.centerY() == y) {
                return cell;
            }
        }
        return null;
    }

    private static ItemStack noteStack(final ResearchNoteData note, final int meta) {
        final ItemStack stack = new ItemStack(M3ResearchContent.ITEM_RESEARCH_NOTES.get());
        stack.set(ModDataComponents.RESEARCH_NOTE.get(), note);
        stack.set(ModDataComponents.ITEM_META.get(), meta);
        return stack;
    }

    /** A real {@link ServerPlayer} with a declared connection, as the sibling probes build one. */
    private static ServerPlayer freshPlayer(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "tc-note-grid-" + UUID.randomUUID().toString().substring(0, 8)),
                false);
        final ServerPlayer player =
                new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        final Channel nettyChannel = new EmbeddedChannel();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public Channel channel() {
                return nettyChannel;
            }
        };
        ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
        final var adHoc = ChannelAttributes.getOrCreateAdHocChannels(connection);
        adHoc.add(SyncAttachmentsPayload.TYPE.id());
        adHoc.add(ResearchCompletePayload.TYPE.id());
        adHoc.add(AspectPoolPayload.TYPE.id());
        adHoc.add(SyncWarpPayload.TYPE.id());
        adHoc.add(WarpMessagePayload.TYPE.id());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        player.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        player.setHealth(20.0F);
        player.getInventory().clearContent();
        return player;
    }
}
