package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.menus.ResearchNoteMenu;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.Chain;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.Request;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The research-note grid against the decompiled original (NOTE-SCREEN-2).
 *
 * <p>Every number the screen draws with is read out of {@code
 * reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchTable.java}
 * at the line that owns it, and compared to the constant in {@link
 * NoteGridView}. A drift here is a visible drift in game: the cell draws where
 * the cursor does not land, the copy plate's click misses the plate, the cost
 * tags spill off the window, the page arrows stop paging.</p>
 *
 * <p>Skipped when the decompiled tree is not checked out (the idiom the sibling
 * parity tests use), so CI runs the presentation and shipped-data tests and
 * this one runs in the developer's tree.</p>
 */
class ResearchNoteScreenGridParityTest {

    private static final String TABLE = "decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchTable.java";
    private static final Predicate<String> ALL = tag -> true;

    private static String original() {
        return ReferenceOrigins.read(TABLE);
    }

    @Test
    void theWindowAndPanelsAreTheOriginals() {
        final String src = original();
        assertEquals(255, num(src, "this\\.field_146999_f = (\\d+);", 1, "the window's width"),
                "the note menu's window is not the original's xSize");
        assertEquals(NoteGridView.WINDOW_SPAN, num(src, "this\\.field_147000_g = (\\d+);", 1, "the window's height"),
                "the window's height is not the model's WINDOW_SPAN");
        assertEquals(NoteGridView.MAIN_PANEL_W, num(src, "func_73729_b\\(var5, var6, 0, 0, (\\d+), \\d+\\);", 1,
                "the main panel's width"), "the parchment panel's width moved");
        assertEquals(NoteGridView.MAIN_PANEL_H, num(src, "func_73729_b\\(var5, var6, 0, 0, \\d+, (\\d+)\\);", 1,
                "the main panel's height"), "the parchment panel's height moved");
        assertEquals(NoteGridView.FOOT_PANEL_X, num(src, "func_73729_b\\(var5 \\+ (\\d+), var6 \\+ (\\d+), 0, 166,", 1,
                "the foot panel's x"), "the foot panel moved right");
        assertEquals(NoteGridView.FOOT_PANEL_Y, num(src, "func_73729_b\\(var5 \\+ \\d+, var6 \\+ (\\d+), 0, 166,", 1,
                "the foot panel's y"), "the foot panel moved down");
        assertEquals(NoteGridView.FOOT_PANEL_W, num(src, "func_73729_b\\(var5 \\+ \\d+, var6 \\+ \\d+, 0, 166, (\\d+),", 1,
                "the foot panel's width"), "the foot panel's width moved");
        assertEquals(NoteGridView.FOOT_PANEL_H, num(src, "func_73729_b\\(var5 \\+ \\d+, var6 \\+ \\d+, 0, 166, \\d+, (\\d+)\\);",
                1, "the foot panel's height"), "the foot panel's height moved");
        assertEquals(NoteGridView.SHEET_X, num(src, "func_73729_b\\(x \\+ (\\d+), y \\+ \\d+, 0, 0, \\d+, \\d+\\);", 1,
                "the note sheet's x"), "the note sheet slid off the table");
        assertEquals(NoteGridView.SHEET_Y, num(src, "func_73729_b\\(x \\+ \\d+, y \\+ (\\d+), 0, 0, \\d+, \\d+\\);", 1,
                "the note sheet's y"), "the note sheet slid off the table");
        assertEquals(NoteGridView.SHEET_SPAN, num(src, "func_73729_b\\(x \\+ \\d+, y \\+ \\d+, 0, 0, (\\d+), \\d+\\);", 1,
                "the note sheet's width"), "the note sheet is no longer 150 wide");
    }

    @Test
    void theGridIsAtTheOriginalsOffsetWithTheOriginalsHexMath() {
        final String src = original();
        // The three places the original converts a cursor to a hex: drop arm
        // (:153), the sheet's hover pass (:500) and the erase arm (:727).
        // Three sites convert a cursor to a hex: the drop arm (:153), the
        // sheet's hover pass (:500) and the erase arm (:727). The orbs' sites
        // (20,146)/(79,146) belong to the combine arm, which this card does not
        // port, so only the grid's own offset is pinned.
        int seen = 0;
        final Matcher origin = Pattern.compile("mx - \\((?:gx|x) \\+ (\\d+)\\);\\s*int mouseY = my - \\((?:gy|y) \\+ (\\d+)\\);")
                .matcher(src);
        while (origin.find()) {
            if (Integer.parseInt(origin.group(1)) == NoteGridView.GRID_X
                    && Integer.parseInt(origin.group(2)) == NoteGridView.GRID_Y) {
                seen++;
            }
        }
        assertTrue(seen >= 3, "the original's grid offset (169, 83) no longer carries the cursor-to-hex conversion: "
                + seen + " sites, expected 3 — the cell drawn and the cell clicked split apart");

        final Matcher size = Pattern.compile("new HexUtils\\.Pixel\\(mouseX, mouseY\\)\\.toHex\\((\\d+)\\)").matcher(src);
        assertTrue(size.find(), "the original's cursor-to-hex call moved");
        assertEquals(NoteGridView.HEX_SIZE, Integer.parseInt(size.group(1)), "the hex size the grid is measured in moved");
        assertEquals(NoteGridView.HEX_SIZE, num(src, "HexUtils\\.Pixel pix = hex\\.toPixel\\((\\d+)\\);", 1,
                "the hex size the pixels are drawn at"), "the draw size and the hit-test size split");

        // The 16px quad drawHex fills (:402-405), and the tag quad's offset
        // from the grid origin (:547: x + 161 + pix.x, y + 75 + pix.y).
        final Matcher corner = Pattern.compile("func_78374_a\\((-\\d+)\\.0, (\\d+)\\.0, this\\.field_73735_i,"
                + " 0\\.0, 1\\.0\\);").matcher(src);
        assertTrue(corner.find(), "the original's cell quad stopped being drawn from corner vertices");
        assertEquals(Integer.parseInt(corner.group(2)) - Integer.parseInt(corner.group(1)), NoteGridView.CELL_SPAN,
                "the cell quad is not the original's 16px square");
        assertEquals(NoteGridView.GRID_X + NoteGridView.TAG_DX,
                num(src, "GL11\\.glTranslated\\(x \\+ (\\d+) \\+ pix\\.x,", 1, "the unknown overlay's x"),
                "the cell's 16px quad is no longer centred on the hex's pixel offset");
        assertEquals(NoteGridView.GRID_Y + NoteGridView.TAG_DY,
                num(src, "GL11\\.glTranslated\\(x \\+ \\d+ \\+ pix\\.x, y \\+ (\\d+) \\+ pix\\.y,", 1,
                        "the unknown overlay's y"),
                "the cell's 16px quad is no longer centred on the hex's pixel offset");
    }

    @Test
    void theTypeGatesAndDrawAlphasAreTheOriginals() {
        final String src = original();
        assertEquals(NoteGridView.TYPE_BLANK,
                num(src, "this\\.note\\.hexEntries\\.containsKey\\(hp\\.toString\\(\\)\\) &&"
                        + " this\\.note\\.hexEntries\\.get\\(hp\\.toString\\(\\)\\)\\.type == (\\d+)\\) \\{", 1,
                        "the write arm's cell gate"),
                "the write arm no longer lands on the original's blank cells");
        assertEquals(NoteGridView.TYPE_TYPED,
                num(src, "this\\.note\\.hexes\\.containsKey\\(hp\\.toString\\(\\)\\) &&"
                        + " this\\.note\\.hexEntries\\.get\\(hp\\.toString\\(\\)\\)\\.type == (\\d+)\\) \\{", 1,
                        "the erase arm's gate"),
                "the erase arm no longer lands on the original's typed cells");
        assertEquals(NoteGridView.TYPE_REQUIRED,
                num(src, "\\.type == (\\d+) \\|\\| this\\.highlight\\.contains", 1, "the required-cell draw gate"),
                "the required cells no longer draw lit");
        assertEquals(NoteGridView.TYPE_TYPED,
                num(src, "\\.type == (\\d+)\\) \\{\\s*HexUtils\\.Pixel pix = hexxx\\.toPixel", 1, "the typed-cell draw gate"),
                "the typed-cell draw pass moved off the original's type");
        assertTrue(src.contains("0.66F, true"),
                "the original's typed-cell draw no longer fades to 0.66: the model's fade is a guess");
        assertEquals(NoteGridView.TYPED_ALPHA, Math.round(0.66F * 255.0F),
                "a typed cell outside the chain no longer draws at the original's 0.66 alpha");
        assertEquals(NoteGridView.UNKNOWN_COLOUR >>> 24, Math.round(0.5F * 255.0F),
                "the unknown overlay no longer draws at the original's 0.5 black");
    }

    @Test
    void theChainLineIsTheOriginalsCyan() {
        final String src = original();
        final Matcher colour = Pattern.compile("func_78369_a\\(([\\d.]+)F, ([\\d.]+)F, ([\\d.]+)F, alpha\\)").matcher(src);
        assertTrue(colour.find(), "the connection line's colour call moved");
        final float r = Float.parseFloat(colour.group(1));
        final float g = Float.parseFloat(colour.group(2));
        final float b = Float.parseFloat(colour.group(3));
        assertEquals(Math.round(r * 255.0F), NoteGridView.LINK_COLOUR >> 16 & 0xFF, "the connection line's red moved");
        assertEquals(Math.round(g * 255.0F), NoteGridView.LINK_COLOUR >> 8 & 0xFF, "the connection line's green moved");
        assertEquals(Math.round(b * 255.0F), NoteGridView.LINK_COLOUR & 0xFF, "the connection line's blue moved");
    }

    @Test
    void theCopyPlateAndCostPanelAreTheOriginals() {
        final String src = original();
        assertEquals(NoteGridView.PLATE_X,
                num(src, "func_73729_b\\(var5 \\+ (\\d+), var6 \\+ \\d+, 232, 200,", 1, "the copy plate's x"),
                "the copy plate slid off its atlas cell");
        assertEquals(NoteGridView.PLATE_Y,
                num(src, "func_73729_b\\(var5 \\+ \\d+, var6 \\+ (\\d+), 232, 200,", 1, "the copy plate's y"),
                "the copy plate slid off its atlas cell");
        assertEquals(NoteGridView.PLATE_SPAN,
                num(src, "func_73729_b\\(var5 \\+ \\d+, var6 \\+ \\d+, 232, 200, (\\d+), \\d+\\);", 1,
                        "the copy plate's width"),
                "the copy plate is not the original's 24px box");
        final Matcher plate = Pattern.compile("int var7 = mx - \\(gx \\+ (\\d+)\\);\\s*int var8 = my - \\(gy \\+ (\\d+)\\);"
                + "\\s*if \\(var7 >= 0 && var8 >= 0 && var7 < (\\d+) && var8 < (\\d+)\\)").matcher(src);
        assertTrue(plate.find(), "the original's copy-plate hit test moved");
        assertEquals(NoteGridView.PLATE_X, Integer.parseInt(plate.group(1)), "the plate's click target left its drawn box");
        assertEquals(NoteGridView.PLATE_Y, Integer.parseInt(plate.group(2)), "the plate's click target left its drawn box");
        assertEquals(NoteGridView.PLATE_SPAN, Integer.parseInt(plate.group(3)),
                "the plate's click box is not the original's 24px square");
        assertEquals(NoteGridView.PLATE_SPAN, Integer.parseInt(plate.group(4)),
                "the plate's click box is not the original's 24px square");

        assertEquals(NoteGridView.COST_PANEL_X,
                num(src, "func_73729_b\\(gx \\+ (\\d+), gy \\+ \\d+, 184, 224, \\d+, \\d+\\);", 1, "the cost panel's x"),
                "the copy-cost panel slid");
        assertEquals(NoteGridView.COST_PANEL_Y,
                num(src, "func_73729_b\\(gx \\+ \\d+, gy \\+ (\\d+), 184, 224, \\d+, \\d+\\);", 1, "the cost panel's y"),
                "the copy-cost panel slid");
        assertEquals(NoteGridView.COST_PANEL_W,
                num(src, "func_73729_b\\(gx \\+ \\d+, gy \\+ \\d+, 184, 224, (\\d+), \\d+\\);", 1, "the cost panel's width"),
                "the copy-cost panel's box moved");
        assertEquals(NoteGridView.COST_PANEL_X + NoteGridView.COST_PANEL_W,
                num(src, "UtilsFX\\.drawTag\\(gx \\+ (\\d+) \\+ 48 \\+ count \\* 16,", 1, "the cost tags' anchor")
                        + NoteGridView.COST_PANEL_W,
                "the cost tags no longer start at the cost panel's right edge");
        assertEquals(NoteGridView.COST_TAG_STEP,
                num(src, "UtilsFX\\.drawTag\\(gx \\+ 100 \\+ 48 \\+ count \\* (\\d+),", 1, "the cost tags' step"),
                "the cost tags no longer step one 16px cell apart");
        assertEquals(NoteGridView.COST_PANEL_Y,
                num(src, "UtilsFX\\.drawTag\\(gx \\+ 100 \\+ 48 \\+ count \\* 16, gy \\+ (\\d+),", 1, "the cost tags' y"),
                "the cost tags no longer sit on the cost panel");
        assertEquals(NoteGridView.COST_HEADER_Y, num(src, "func_78261_a\\(ss, gx \\+ 100, gy \\+ (\\d+), -1\\);", 1,
                "the copy header's y"), "the tc.research.copy header moved off its line");
    }

    @Test
    void theTrayAndItsArrowsAreTheOriginals() {
        final String src = original();
        assertEquals(NoteGridView.TRAY_X, num(src, "int sx = gx \\+ (\\d+);\\s*int sy = gy \\+ \\d+;", 1, "the tray's x"),
                "the aspect tray slid right");
        assertEquals(NoteGridView.TRAY_Y, num(src, "int sx = gx \\+ \\d+;\\s*int sy = gy \\+ (\\d+);", 1, "the tray's y"),
                "the aspect tray slid down");
        assertEquals(NoteGridView.TRAY_REGION, num(src, "mx >= sx && mx < sx \\+ (\\d+)", 1, "the tray's width"),
                "the tray's click region is not the original's 80px box");
        assertEquals(NoteGridView.TRAY_X, num(src, "int var7 = mx - \\(gx \\+ xx \\+ (\\d+)\\);", 1, "the tray cell's x origin"),
                "the tray's cell hit test left its drawn grid");
        assertEquals(NoteGridView.TRAY_PAGE_STEP, num(src, "int xx = drawn / (\\d+) \\* 16;", 1, "the tray's column step"),
                "the tray no longer packs 5 cells to a column");
        assertEquals(NoteGridView.TRAY_CELL, num(src, "int xx = drawn / 5 \\* (\\d+);", 1, "the tray's column width"),
                "the tray's columns are not 16px");
        assertEquals(NoteGridView.TRAY_MAX, num(src, "&& drawn < (\\d+)\\) \\{", 1, "the tray's page size"),
                "a tray page no longer draws the original's 25 cells");
        assertEquals(NoteGridView.TRAY_PAGE_STEP, num(src, ">= this\\.page \\* (\\d+) &&", 1, "the tray's page step"),
                "the page step moved, so the pages overlap or skip");
        assertEquals(20, num(src, "this\\.lastPage = \\(count - (\\d+)\\) / 5;", 1, "the tray's page offset"),
                "the page count formula's offset moved");
        assertEquals(NoteGridView.TRAY_PAGE_STEP, num(src, "this\\.lastPage = \\(count - 20\\) / (\\d+);", 1,
                "the tray's page divisor"), "the page count formula's divisor moved");
        assertEquals(NoteGridView.PAGE_NEXT_X, num(src, "func_73729_b\\(var5 \\+ (\\d+), var6 \\+ 121, 208, 208,", 1,
                "the next-page arrow's x"), "the next-page arrow slid off its drawn cell");
        assertEquals(NoteGridView.PAGE_PREV_X, num(src, "func_73729_b\\(var5 \\+ (\\d+), var6 \\+ 121, 184, 208,", 1,
                "the prev-page arrow's x"), "the prev-page arrow slid off its drawn cell");
        assertEquals(NoteGridView.PAGE_ARROW_Y, num(src, "func_73729_b\\(var5 \\+ \\d+, var6 \\+ (\\d+), 208, 208,", 1,
                "the next-page arrow's y"), "the page arrows left their row");
        assertEquals(NoteGridView.PAGE_ARROW_W, num(src, "func_73729_b\\(var5 \\+ \\d+, var6 \\+ 121, 208, 208, (\\d+), 8\\);",
                1, "the arrow's width"), "the page arrows' click box is not the drawn 24px");
        assertEquals(NoteGridView.PAGE_ARROW_W,
                num(src, "this\\.page < this\\.lastPage && var7 >= 0 && var8 >= 0 && var7 < (\\d+) &&", 1,
                        "the arrow's width"), "the page arrows' click box is not the drawn 24px");
        assertEquals(NoteGridView.PAGE_ARROW_H,
                num(src, "this\\.page < this\\.lastPage && var7 >= 0 && var8 >= 0 && var7 < 24 && var8 < (\\d+)\\)", 1,
                        "the arrow's height"), "the page arrows' click box is not the drawn 8px");
    }

    @Test
    void theInkWarningIsTheOriginalsPairOfLines() {
        final String src = original();
        assertTrue(src.contains("tile.researchtable.noink.0") && src.contains("tile.researchtable.noink.1"),
                "the original's out-of-ink lines moved");
        assertEquals(NoteGridView.INK_TIP_X, num(src, "gx \\+ (\\d+) - sx,", 1, "the ink warning's centred x"),
                "the ink warning slid off its centred x");
        assertEquals(NoteGridView.INK_TIP_Y, num(src, "gy \\+ (\\d+),\\s*11\\s*\\);", 1, "the ink warning's y"),
                "the ink warning slid off its line");
        // The gate: a note in the table and the tool empty or at max damage.
        assertTrue(Pattern.compile("this\\.note != null\\s*&&\\s*\\(this\\.tileEntity\\.func_70301_a\\(0\\) == null"
                        + "\\s*\\|\\|\\s*this\\.tileEntity\\.func_70301_a\\(0\\)\\.func_77960_j\\(\\) =="
                        + "\\s*this\\.tileEntity\\.func_70301_a\\(0\\)\\.func_77958_k\\(\\)\\)")
                .matcher(src)
                .find(), "the out-of-ink gate is no longer the original's empty-or-max-damage scribe tool");
    }

    @Test
    void theCopyRidesTheOriginalsButtonId() {
        final String src = original();
        assertEquals(ResearchNoteMenu.BUTTON_DUPLICATE,
                num(src, "func_78756_a\\(this\\.field_147002_h\\.field_75152_c, (\\d+)\\);", 1, "the copy button id"),
                "the copy plate no longer sends the original's button id, so the server's copy arm never runs");
    }

    @Test
    void theModelArmsMatchTheOriginalsArmsOnTheSameGrid() {
        // The two arms, on a grid built to the original's own shape: the drop
        // arm writes on a type-0 cell, the erase arm clears a type-2 cell, and
        // a type-1 cell has no arm at all.
        final Map<String, HexEntry> cells = new LinkedHashMap<>();
        cells.put("0:0", new HexEntry("ignis", NoteGridView.TYPE_REQUIRED));
        cells.put("1:0", new HexEntry(null, NoteGridView.TYPE_BLANK));
        cells.put("1:1", new HexEntry("lux", NoteGridView.TYPE_TYPED));
        final ResearchNoteData note = new ResearchNoteData("item.thaumcraftclassic.itemresearchnotes", 1, false, 0, cells);

        final Request write = NoteGridView.requestAt(note, NoteGridView.centerX(
                new com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex(1, 0)),
                NoteGridView.centerY(new com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex(1, 0)), "aer", ALL);
        assertEquals(NoteGridView.Arm.WRITE, write.arm(), "the drop arm's gate is not the original's type-0 cell");
        final Request erase = NoteGridView.requestAt(note,
                NoteGridView.centerX(new com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex(1, 1)),
                NoteGridView.centerY(new com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex(1, 1)), null, ALL);
        assertEquals(NoteGridView.Arm.ERASE, erase.arm(), "the erase arm's gate is not the original's type-2 cell");

        // The chain rule, straight from checkConnections: a neighbour connects
        // when the other cell's aspect has this cell's aspect as a component.
        final Map<String, HexEntry> chainCells = new LinkedHashMap<>();
        chainCells.put("0:0", new HexEntry("ignis", NoteGridView.TYPE_REQUIRED));
        chainCells.put("1:0", new HexEntry("lux", NoteGridView.TYPE_TYPED));
        final Chain chain = NoteGridView.chain(
                new ResearchNoteData("item.thaumcraftclassic.itemresearchnotes", 1, false, 0, chainCells), ALL);
        assertEquals(1, chain.links().size(),
                "ignis -> lux (aer + ignis) is the original's own example of a connection, and it drew "
                        + chain.links().size() + " lines");
    }

    // ------------------------------------------------------------------ helpers

    /**
     * The first number the pattern's group holds; group 0 means the whole match
     * is a literal the test only needs to find.
     */
    private static int num(final String source, final String regex, final int group, final String what) {
        final Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(source);
        assertTrue(matcher.find(), "the decompiled original no longer carries " + what + " (" + regex + ")");
        if (group == 0) {
            return 0;
        }
        final String value = matcher.group(group);
        assertTrue(value != null, what + " has no number at group " + group);
        return Integer.parseInt(value);
    }
}
