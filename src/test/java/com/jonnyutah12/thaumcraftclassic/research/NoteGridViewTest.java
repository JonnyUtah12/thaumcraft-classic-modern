package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.Cell;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.Chain;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.Request;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.TrayCell;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The research-note grid model (NOTE-SCREEN-2): the geometry, the hit test, the
 * two grid arms, the chain walk and the aspect tray, as pure arithmetic.
 *
 * <p>Every claim here is the shape of a player action: where a cell is drawn,
 * which cell a click lands on, what a click on a blank / typed / required cell
 * asks the server for, which cells a closed chain lights, how the tray pages
 * and fades. The decompiled-original parity of the constants is
 * {@code ResearchNoteScreenGridParityTest}; the shipped-data behaviour is
 * {@code ResearchNoteGridGameTest}. This class is the arithmetic that has to
 * hold for both.</p>
 */
class NoteGridViewTest {

    private static final String NOTE_KEY = "item.thaumcraftclassic.itemresearchnotes";
    private static final Predicate<String> ALL = tag -> true;

    private static ResearchNoteData note(final Object... keysAndEntries) {
        final Map<String, HexEntry> cells = new LinkedHashMap<>();
        for (int i = 0; i < keysAndEntries.length; i += 2) {
            cells.put((String) keysAndEntries[i], (HexEntry) keysAndEntries[i + 1]);
        }
        return new ResearchNoteData(NOTE_KEY, 1, false, 0, cells);
    }

    // ------------------------------------------------------------- geometry

    @Test
    void theHexSilhouetteIsASymmetricalFlatTopHexagon() {
        final int span = NoteGridView.CELL_SPAN;
        final int[] widths = NoteGridView.hexWidths(span);
        assertEquals(span, widths.length, "the silhouette does not cover the cell's 16 rows");
        for (int row = 0; row < span; row++) {
            assertEquals(widths[row], widths[span - 1 - row],
                    "the hex silhouette is not top-bottom symmetric at row " + row);
            assertEquals((span - widths[row]) / 2, NoteGridView.hexLeft(span, widths[row]),
                    "row " + row + " is not centred in the cell");
            if (row + 1 < span / 2) {
                assertTrue(widths[row] < widths[row + 1],
                        "the hex does not widen toward its points at row " + row);
            }
        }
        assertEquals(span / 2, widths[0], "the hex's top row is not half as wide as the cell");
        assertEquals(span, widths[span / 2], "the hex does not reach the cell's width at its points");
    }

    @Test
    void theCellCentresAreTheOriginalsPixelOffsets() {
        for (final Hex hex : List.of(new Hex(0, 0), new Hex(1, 0), new Hex(0, 1), new Hex(-2, 3), new Hex(4, -1))) {
            final var pixel = hex.toPixel(NoteGridView.HEX_SIZE);
            assertEquals(Math.round(NoteGridView.GRID_X + pixel.x()), NoteGridView.centerX(hex),
                    "hex " + hex + " is not centred at 169 + toPixel(9).x");
            assertEquals(Math.round(NoteGridView.GRID_Y + pixel.y()), NoteGridView.centerY(hex),
                    "hex " + hex + " is not centred at 83 + toPixel(9).y");
        }
        // Neighbour spacing: 1.5 * size across, sqrt(3) * size down the column.
        assertEquals(Math.round(1.5 * NoteGridView.HEX_SIZE),
                NoteGridView.centerX(new Hex(1, 0)) - NoteGridView.centerX(new Hex(0, 0)),
                "a q step is not 1.5 hex sizes across");
        assertEquals(Math.round(Math.sqrt(3.0) * NoteGridView.HEX_SIZE),
                NoteGridView.centerY(new Hex(0, 1)) - NoteGridView.centerY(new Hex(0, 0)),
                "an r step is not sqrt(3) hex sizes down");
    }

    @Test
    void theGridDrawsOneViewPerNoteCellInAStableOrder() {
        final ResearchNoteData open = note("0:0", new HexEntry("ignis", 1),
                "1:0", new HexEntry(null, 0),
                "0:2", new HexEntry("lux", 2),
                "3:1", new HexEntry("aer", 1));
        final List<Cell> cells = NoteGridView.cells(open, ALL, Chain.empty(), null);
        assertEquals(open.hexEntries().size(), cells.size(), "the grid skipped a note cell");
        for (final Cell cell : cells) {
            assertNotNull(open.entryAt(new Hex(cell.q(), cell.r())), "the grid drew " + cell.key() + " for no note cell");
            assertEquals(open.entryAt(new Hex(cell.q(), cell.r())).type(), cell.type(),
                    "cell " + cell.key() + " draws the wrong type");
            assertEquals(cell.centerX() + NoteGridView.TAG_DX, cell.left(), "the tag quad is not offset -8 from the centre");
            assertEquals(cell.centerY() + NoteGridView.TAG_DY, cell.top(), "the tag quad is not offset -8 from the centre");
            assertEquals(NoteGridView.CELL_SPAN, cell.span(), "the tag quad is not the original's 16px quad");
        }
        assertEquals(cells, NoteGridView.cells(open, ALL, Chain.empty(), null),
                "the draw order moved between two frames of the same note");
        // Row-major and deterministic: the r rows in order, q ascending inside
        // a row — the order the original's own hexEntries walk visits.
        assertEquals(List.of("0:0", "1:0", "3:1", "0:2"), cells.stream().map(Cell::key).toList(),
                "the draw order is not the original's row-major walk of the grid");
    }

    // ------------------------------------------------------------- hit test

    @Test
    void theHitTestLandsOnTheCellItDraws() {
        final ResearchNoteData grid = note("0:0", new HexEntry("ignis", 1),
                "1:0", new HexEntry(null, 0),
                "1:1", new HexEntry("lux", 2),
                "-2:3", new HexEntry("aer", 1));
        for (final Cell cell : NoteGridView.cells(grid, ALL, Chain.empty(), null)) {
            assertEquals(new Hex(cell.q(), cell.r()), NoteGridView.hexAt(cell.centerX(), cell.centerY()),
                    "the hit test at the centre of " + cell.key() + " landed elsewhere");
            final Cell hit = NoteGridView.cellAt(grid, cell.centerX(), cell.centerY(), ALL, Chain.empty());
            assertNotNull(hit, "a click at the centre of " + cell.key() + " hit nothing");
            assertEquals(cell.key(), hit.key(), "a click at the centre of " + cell.key() + " hit " + hit.key());
            for (final int[] offset : new int[][]{{5, 0}, {-5, 0}, {0, 5}, {0, -5}}) {
                final Cell near = NoteGridView.cellAt(grid, cell.centerX() + offset[0], cell.centerY() + offset[1],
                        ALL, Chain.empty());
                assertNotNull(near, "a click " + offset[0] + "," + offset[1] + " off " + cell.key() + " fell off the grid");
                assertEquals(cell.key(), near.key(),
                        "a click " + offset[0] + "," + offset[1] + " off the centre of " + cell.key()
                                + " landed on " + near.key());
            }
        }
        assertNull(NoteGridView.cellAt(grid, NoteGridView.GRID_X + 400, NoteGridView.GRID_Y + 400, ALL, Chain.empty()),
                "a click far off the grid reported a cell");
    }

    // ---------------------------------------------------------------- arms

    @Test
    void theArmsFollowTheOriginalsTypeGates() {
        final ResearchNoteData grid = note("0:0", new HexEntry("ignis", NoteGridView.TYPE_REQUIRED),
                "1:0", new HexEntry(null, NoteGridView.TYPE_BLANK),
                "2:0", new HexEntry("lux", NoteGridView.TYPE_TYPED));

        final Cell blank = cell(grid, 1, 0);
        final Request write = NoteGridView.requestAt(grid, blank.centerX(), blank.centerY(), "lux", ALL);
        assertEquals(NoteGridView.Arm.WRITE, write.arm(), "a click on a blank cell with an armed aspect did not write");
        assertEquals(1, write.q());
        assertEquals(0, write.r());
        assertEquals("lux", write.aspectTag(), "the write did not carry the armed aspect");
        assertEquals(NoteGridView.Arm.NONE,
                NoteGridView.requestAt(grid, blank.centerX(), blank.centerY(), null, ALL).arm(),
                "a blank cell wrote with nothing armed");

        final Cell typed = cell(grid, 2, 0);
        for (final String armed : new String[]{null, "lux"}) {
            final Request erase = NoteGridView.requestAt(grid, typed.centerX(), typed.centerY(), armed, ALL);
            assertEquals(NoteGridView.Arm.ERASE, erase.arm(), "a click on a typed cell did not erase (armed " + armed + ")");
            assertEquals(2, erase.q());
            assertNull(erase.aspectTag(), "an erase carried an aspect: the original sends a null aspect");
        }

        final Cell required = cell(grid, 0, 0);
        assertEquals(NoteGridView.Arm.NONE,
                NoteGridView.requestAt(grid, required.centerX(), required.centerY(), "lux", ALL).arm(),
                "a required cell took a click: the original has no arm for the printed cells");

        // A completed note has no blank cell to land on, and the server refuses
        // a write to it; the client must not arm one.
        final ResearchNoteData done = grid.withComplete(true);
        assertEquals(NoteGridView.Arm.NONE,
                NoteGridView.requestAt(done, blank.centerX(), blank.centerY(), "lux", ALL).arm(),
                "a completed note armed a write");
        // The erase arm's gate is the cell's type alone — the server owns the ink.
        assertEquals(NoteGridView.Arm.ERASE,
                NoteGridView.requestAt(done, typed.centerX(), typed.centerY(), null, ALL).arm(),
                "a completed note refused the erase arm the original had");
    }

    // --------------------------------------------------------------- chain

    @Test
    void theChainWalkLightsTheCellsTheRuleConnects() {
        final ResearchNoteData grid = note("0:0", new HexEntry("ignis", NoteGridView.TYPE_REQUIRED),
                "1:0", new HexEntry("lux", NoteGridView.TYPE_TYPED),
                "2:0", new HexEntry("ignis", NoteGridView.TYPE_REQUIRED));
        final Predicate<String> knows = tag -> tag.equals("ignis") || tag.equals("lux");

        final Chain chain = NoteGridView.chain(grid, knows);
        assertEquals(2, chain.links().size(),
                "ignis-lux-ignis (lux = aer + ignis) drew " + chain.links().size() + " lines, the rule connects 2");
        assertTrue(chain.highlighted().contains("1:0") && chain.highlighted().contains("2:0"),
                "the chain left the cells it reached unlit: " + chain.highlighted());
        final List<Cell> cells = NoteGridView.cells(grid, knows, chain, null);
        for (final NoteGridView.Link link : chain.links()) {
            assertNotNull(centreAt(cells, link.x1(), link.y1()), "a line starts off the grid");
            assertNotNull(centreAt(cells, link.x2(), link.y2()), "a line ends off the grid");
        }

        // An undiscovered aspect in the middle: nothing connects.
        final Chain blind = NoteGridView.chain(grid, tag -> tag.equals("ignis"));
        assertTrue(blind.links().isEmpty(), "a chain through an undiscovered aspect drew lines");
        assertTrue(blind.highlighted().isEmpty(), "a chain through an undiscovered aspect lit cells");

        // Two primals never connect (the shipped rule: components of the other).
        final ResearchNoteData primals = note("0:0", new HexEntry("ignis", NoteGridView.TYPE_REQUIRED),
                "1:0", new HexEntry("aer", NoteGridView.TYPE_TYPED));
        assertTrue(NoteGridView.chain(primals, ALL).links().isEmpty(),
                "two primal aspects drew a connection line");
    }

    @Test
    void theCellAlphasAreTheOriginalsDraws() {
        final ResearchNoteData grid = note("0:0", new HexEntry("cognitio", NoteGridView.TYPE_REQUIRED),
                "1:0", new HexEntry("lux", NoteGridView.TYPE_TYPED),
                "2:0", new HexEntry(null, NoteGridView.TYPE_BLANK));
        final Predicate<String> knowsLux = tag -> tag.equals("lux");
        final List<Cell> cells = NoteGridView.cells(grid, knowsLux, NoteGridView.chain(grid, knowsLux), null);

        for (final Cell cell : cells) {
            switch (cell.key()) {
                case "0:0" -> assertTrue(cell.unknown(),
                        "an undiscovered aspect drew its tag, not the unknown overlay");
                case "1:0" -> {
                    assertFalse(cell.unknown(), "a discovered aspect drew the unknown overlay");
                    assertEquals(NoteGridView.TYPED_ALPHA, cell.fillColour() >>> 24,
                            "a typed cell outside the chain does not draw at the original's 0.66 alpha");
                }
                case "2:0" -> assertEquals(NoteGridView.HEX_OUTLINE_COLOUR, cell.fillColour(),
                        "a blank cell is not the original's white-at-0.25 outline");
                default -> throw new IllegalStateException(cell.key());
            }
        }

        // In the chain, the typed cell draws at full alpha.
        final ResearchNoteData lit = note("0:0", new HexEntry("ignis", NoteGridView.TYPE_REQUIRED),
                "1:0", new HexEntry("lux", NoteGridView.TYPE_TYPED));
        final Chain chain = NoteGridView.chain(lit, ALL);
        final Cell bright = NoteGridView.cells(lit, ALL, chain, null).stream()
                .filter(cell -> cell.key().equals("1:0")).findFirst().orElseThrow();
        assertTrue(bright.highlighted(), "a cell the chain reached drew unlit");
        assertEquals(NoteGridView.FULL_ALPHA, bright.fillColour() >>> 24,
                "a chain cell does not draw at full alpha");
    }

    // ----------------------------------------------------------- plate, tray

    @Test
    void theCopyPlateBoxIsTheOriginals() {
        assertTrue(NoteGridView.inPlate(NoteGridView.PLATE_X, NoteGridView.PLATE_Y));
        assertTrue(NoteGridView.inPlate(NoteGridView.PLATE_X + NoteGridView.PLATE_SPAN - 1,
                NoteGridView.PLATE_Y + NoteGridView.PLATE_SPAN - 1));
        assertFalse(NoteGridView.inPlate(NoteGridView.PLATE_X + NoteGridView.PLATE_SPAN, NoteGridView.PLATE_Y),
                "the plate box runs past its right edge");
        assertFalse(NoteGridView.inPlate(NoteGridView.PLATE_X, NoteGridView.PLATE_Y - 1),
                "the plate box runs above its top edge");
    }

    @Test
    void theTrayPagesInTheOriginalsLayoutAndFadesAnEmptyPool() {
        final List<String> tags = new ArrayList<>();
        for (final AspectDefinitions.Definition definition : AspectDefinitions.all()) {
            tags.add(definition.tag());
        }
        tags.sort(String::compareTo);
        final String dry = tags.get(3);
        final Map<String, Integer> pools = new LinkedHashMap<>();
        for (final String tag : tags) {
            pools.put(tag, tag.equals(dry) ? 0 : 7);
        }

        final List<TrayCell> page0 = NoteGridView.tray(tags, 0, pools::get, null);
        assertEquals(NoteGridView.TRAY_MAX, page0.size(), "the first page is not the original's 25 cells");
        assertEquals(NoteGridView.TRAY_X, page0.get(0).left());
        assertEquals(NoteGridView.TRAY_Y, page0.get(0).top());
        // Column-major: 5 rows per column.
        assertEquals(NoteGridView.TRAY_X + NoteGridView.TRAY_CELL, page0.get(5).left());
        assertEquals(NoteGridView.TRAY_Y, page0.get(5).top());
        assertEquals(NoteGridView.TRAY_Y + NoteGridView.TRAY_CELL, page0.get(6).top());

        final List<TrayCell> page1 = NoteGridView.tray(tags, 1, pools::get, null);
        assertEquals(tags.get(5), page1.get(0).tag(), "page 1 does not open on the sixth aspect");
        assertEquals(0, NoteGridView.tray(tags, 60, pools::get, null).size(),
                "a page past the last page drew cells");
        assertEquals((tags.size() - 20) / NoteGridView.TRAY_PAGE_STEP, NoteGridView.lastPage(tags.size()),
                "the page count is not the original's (size - 20) / 5");

        final TrayCell faded = page0.stream().filter(cell -> cell.tag().equals(dry)).findFirst().orElseThrow();
        assertTrue(faded.faded(), "an aspect with an empty pool drew unfaded");
        assertNull(NoteGridView.trayCellAt(page0, faded.left(), faded.top(), true),
                "a click armed an aspect the player holds none of");
        assertNotNull(NoteGridView.trayCellAt(page0, faded.left(), faded.top(), false),
                "the tray lost its faded cell");

        final TrayCell funded = page0.get(0);
        assertFalse(funded.faded());
        assertEquals(funded.tag(), NoteGridView.trayCellAt(page0, funded.left() + 1, funded.top() + 1, true).tag(),
                "a click on a funded cell did not arm it");
        final TrayCell beyond = NoteGridView.trayCellAt(page0, funded.left() + NoteGridView.TRAY_CELL,
                funded.top(), true);
        assertTrue(beyond == null || !beyond.tag().equals(funded.tag()),
                "the tray cell's box is wider than the original's 16px cell");
        assertTrue(NoteGridView.inTray(NoteGridView.TRAY_X, NoteGridView.TRAY_Y));
        assertFalse(NoteGridView.inTray(NoteGridView.TRAY_X + NoteGridView.TRAY_REGION, NoteGridView.TRAY_Y),
                "the tray's region is not the original's 80px box");
    }

    @Test
    void thePageArrowsSitOnThePanelTheyPage() {
        assertTrue(NoteGridView.inPageNext(NoteGridView.PAGE_NEXT_X, NoteGridView.PAGE_ARROW_Y));
        assertTrue(NoteGridView.inPagePrev(NoteGridView.PAGE_PREV_X, NoteGridView.PAGE_ARROW_Y));
        assertFalse(NoteGridView.inPageNext(NoteGridView.PAGE_NEXT_X + NoteGridView.PAGE_ARROW_W,
                NoteGridView.PAGE_ARROW_Y));
        assertTrue(NoteGridView.PAGE_ARROW_Y + NoteGridView.PAGE_ARROW_H <= NoteGridView.MAIN_PANEL_H,
                "the page arrows fall off the panel");
    }

    // -------------------------------------------------------------- colours

    @Test
    void theColoursComeFromTheShippedDefinitions() {
        for (final AspectDefinitions.Definition definition : AspectDefinitions.all()) {
            assertEquals(definition.color() & 0xFFFFFF, NoteGridView.AspectColour.of(definition.tag()),
                    "the grid colour for " + definition.tag() + " is not the shipped definition's");
        }
        assertEquals(0xFFFFFF, NoteGridView.AspectColour.of("not-an-aspect"),
                "an unknown aspect did not fall back to white");
        // The original's line colour: color4f(0, 0.6, 0.8).
        assertEquals(0x00, NoteGridView.LINK_COLOUR >> 16 & 0xFF, "the connection line's red is not 0");
        assertEquals(153, NoteGridView.LINK_COLOUR >> 8 & 0xFF, "the connection line's green is not 0.6");
        assertEquals(204, NoteGridView.LINK_COLOUR & 0xFF, "the connection line's blue is not 0.8");
    }

    @Test
    void everyRegionOfThePanelSitsInsideTheWindow() {
        assertEquals(255, NoteGridView.WINDOW_SPAN, "the window is not the original's 255x255");
        assertTrue(NoteGridView.SHEET_X + NoteGridView.SHEET_SPAN <= NoteGridView.WINDOW_SPAN
                        && NoteGridView.SHEET_Y + NoteGridView.SHEET_SPAN <= NoteGridView.WINDOW_SPAN,
                "the note sheet falls off the window");
        assertTrue(NoteGridView.FOOT_PANEL_X + NoteGridView.FOOT_PANEL_W <= NoteGridView.WINDOW_SPAN
                        && NoteGridView.FOOT_PANEL_Y + NoteGridView.FOOT_PANEL_H <= NoteGridView.WINDOW_SPAN,
                "the foot panel falls off the window");
        assertTrue(NoteGridView.COST_TAG_X + 4 * NoteGridView.COST_TAG_STEP <= NoteGridView.WINDOW_SPAN,
                "the cost tags spill off the window");
        assertTrue(NoteGridView.INK_TIP_Y + 22 <= NoteGridView.MAIN_PANEL_H
                        && NoteGridView.INK_TIP_X <= NoteGridView.MAIN_PANEL_W,
                "the out-of-ink lines fall off the panel they draw on");
        assertTrue(NoteGridView.TRAY_X + NoteGridView.TRAY_REGION <= NoteGridView.WINDOW_SPAN
                        && NoteGridView.TRAY_Y + NoteGridView.TRAY_REGION <= NoteGridView.WINDOW_SPAN,
                "the tray falls off the window");
    }

    // -------------------------------------------------------------- helpers

    private static Cell cell(final ResearchNoteData note, final int q, final int r) {
        return NoteGridView.viewOf(note, new Hex(q, r), ALL, Chain.empty(), false);
    }

    private static Cell centreAt(final List<Cell> cells, final int x, final int y) {
        for (final Cell cell : cells) {
            if (cell.centerX() == x && cell.centerY() == y) {
                return cell;
            }
        }
        return null;
    }
}
