package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Pixel;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * The research-note screen's presentation model (TC-M3-04-s-iii, NOTE-SCREEN-2,
 * closes BD-20): what the hex grid looks like, which cell the cursor is on, and
 * what clicking a region of the panel asks the server for.
 *
 * <p>This is the geometry and the click arithmetic of the original
 * {@code thaumcraft.client.gui.GuiResearchTable}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchTable.java}),
 * lifted out of the screen so the shipped contract can be measured headless:
 * the grid origin, the hex size, the parchment sheet, the aspect palette and its
 * paging, the RESEARCHDUPE copy plate, the copy-cost panel and the out-of-ink
 * tooltip position are all constants the original hard-coded, and
 * {@code ResearchNoteScreenGridParityTest} pins every one of them against that
 * source text.</p>
 *
 * <p>Nothing here names a client class — the model reads the note state
 * ({@link ResearchNoteData}, the state the container already syncs), the aspect
 * definitions and the player's discovery view — so the same code runs on a
 * dedicated server, and a game probe can drive it with a real note. The screen
 * is the only consumer that turns a {@link Request} into a payload.</p>
 *
 * <h2>What the model answers</h2>
 * <ul>
 *   <li><b>{@link #cells}</b> — one drawable view per {@code hexEntries} cell,
 *       in a stable order, at the original's pixel offsets: the cell quad is
 *       centred on {@code (GRID_X, GRID_Y) + hex.toPixel(9)} and the 16×16 tag
 *       quad inside it sits at {@code (-8, -8)} from that centre
 *       ({@code GuiResearchTable.java:500-533}, the {@code drawHex} /
 *       {@code drawOrb} / {@code drawTag} calls that all pass
 *       {@code x + 169 + pix.x, y + 75 + pix.y} for a tag and
 *       {@code x + 169 + pix.x, y + 83 + pix.y} for the hex itself).</li>
 *   <li><b>{@link #chain}</b> — the connection lines the original's
 *       {@code checkConnections} ({@code :570-597}) walked and the cells it
 *       marked bright. The walk is the same rule {@link NotePuzzle#checkCompletion}
 *       validates server-side: a neighbour joins the chain when its type is
 *       {@code >= 1}, both aspects are discovered and one is a direct component
 *       of the other.</li>
 *   <li><b>{@link #requestAt}</b> — the two grid arms, verbatim: a drop on a
 *       cell whose type is 0 writes the carried aspect
 *       ({@code :147-172}, {@code PacketAspectPlaceToServer} with the aspect),
 *       and a click on a cell whose type is 2 erases it
 *       ({@code checkClickedHex}, {@code :726-740}, the same packet with a
 *       {@code null} aspect). Every other region answers {@link Arm#NONE}:
 *       a type-1 (required) cell takes no click, and a completed note takes no
 *       write — the original has no type-0 cell left to land on, and the
 *       server's write arm refuses a completed note
 *       ({@code ResearchNoteMenu.placeAspect}).</li>
 *   <li><b>{@link #palette}</b> — the aspect tray: the discovered aspects in the
 *       original's sorted order, 25 per page in a 5×5 grid of 16 px cells,
 *       faded when the pool amount is 0, and the page arithmetic
 *       {@code lastPage = (size - 20) / 5} ({@code :264-291}, {@code :742-762}).
 *       In the original the tray is a drag source; the port selects with a click
 *       and writes with the click on the cell, so a write needs an armed aspect
 *       (BD-20 records that one interaction deviation).</li>
 * </ul>
 *
 * <p>Two shapes the original drew as textures and the port draws as code
 * (ARR, D-004): the hex outline ({@code textures/gui/hex1.png}) and the hex
 * highlight ({@code hex2.png}). {@link #hexWidths} describes the flat-top
 * silhouette the port fills row by row; the hit test stays the original's
 * {@code toHex(9)}, which is what the texture covered.</p>
 */
public final class NoteGridView {

    // ------------------------------------------------------------- panel frame

    /** The original's window: {@code xSize = ySize = 255} ({@code :70-71}). */
    public static final int WINDOW_SPAN = 255;
    /** The parchment panel behind the grid: 255×167 at the window origin ({@code :242}). */
    public static final int MAIN_PANEL_W = 255;
    public static final int MAIN_PANEL_H = 167;
    /** The second panel: 184×88 at (40, 167) ({@code :243}). */
    public static final int FOOT_PANEL_X = 40;
    public static final int FOOT_PANEL_Y = 167;
    public static final int FOOT_PANEL_W = 184;
    public static final int FOOT_PANEL_H = 88;

    /** The note sheet: a 150×150 parchment at (94, 8) ({@code :456}). */
    public static final int SHEET_X = 94;
    public static final int SHEET_Y = 8;
    public static final int SHEET_SPAN = 150;

    // ----------------------------------------------------------------- the grid

    /** The hex size every {@code toPixel}/{@code toHex} call passes ({@code :153,500,510}). */
    public static final int HEX_SIZE = 9;
    /** The grid origin the hexes are laid out around ({@code :500-533}). */
    public static final int GRID_X = 169;
    public static final int GRID_Y = 83;
    /** The tag quad inside a cell: 16×16, offset (-8, -8) from the cell centre ({@code :533,554}). */
    public static final int CELL_SPAN = 16;
    public static final int TAG_DX = -8;
    public static final int TAG_DY = -8;

    /** The original's hex entry types ({@code ResearchNoteData} javadoc, {@code :509-556}). */
    public static final int TYPE_BLANK = 0;
    public static final int TYPE_REQUIRED = 1;
    public static final int TYPE_TYPED = 2;

    /** The connection line colour: {@code color4f(0, 0.6, 0.8, alpha)} ({@code :443}). */
    public static final int LINK_COLOUR = 0xFF0099CC;
    /** The hex outline fill: white at 0.25 alpha ({@code drawHex}, {@code :390-409}). */
    public static final int HEX_OUTLINE_COLOUR = 0x40FFFFFF;
    /** The hover highlight: white at full alpha ({@code drawHexHighlight}, {@code :411-437}). */
    public static final int HEX_HIGHLIGHT_COLOUR = 0xFFFFFFFF;
    /** An undiscovered cell's overlay: black at 0.5 alpha ({@code :541-549}). */
    public static final int UNKNOWN_COLOUR = 0x80000000;
    /** A typed cell that is not part of the chain draws at 0.66 alpha ({@code :563-566}). */
    public static final int TYPED_ALPHA = 0xA8;
    /** A cell that is part of the chain, or required, draws at full alpha ({@code :554-560}). */
    public static final int FULL_ALPHA = 0xFF;

    // ------------------------------------------------------------- copy plate

    /** The RESEARCHDUPE plate: 24×24 at (37, 5), gated on a complete note ({@code :254-256,683-690}). */
    public static final int PLATE_X = 37;
    public static final int PLATE_Y = 5;
    public static final int PLATE_SPAN = 24;
    /** The copy-cost panel: 48×16 at (100, 21) ({@code :114}). */
    public static final int COST_PANEL_X = 100;
    public static final int COST_PANEL_Y = 21;
    public static final int COST_PANEL_W = 48;
    public static final int COST_PANEL_H = 16;
    /** The cost tags: 16 px apart starting at 100 + 48, y 21 ({@code :122-124}). */
    public static final int COST_TAG_X = COST_PANEL_X + COST_PANEL_W;
    public static final int COST_TAG_STEP = 16;
    /** The cost header at (100, 12) ({@code :129}). */
    public static final int COST_HEADER_Y = 12;

    // ------------------------------------------------------------- ink warning

    /** The out-of-ink tooltip: centred on x 157 (minus half the widest line), y 84 ({@code :223-225}). */
    public static final int INK_TIP_X = 157;
    public static final int INK_TIP_Y = 84;

    // ------------------------------------------------------------- aspect tray

    /** The aspect tray: 80×80 at (10, 40) ({@code :136-138,258}). */
    public static final int TRAY_X = 10;
    public static final int TRAY_Y = 40;
    public static final int TRAY_REGION = 80;
    /** 25 cells of 16 px, column-major: {@code x = drawn / 5 * 16}, {@code y = drawn % 5 * 16} ({@code :275-276}). */
    public static final int TRAY_CELL = 16;
    public static final int TRAY_MAX = 25;
    /** Five entries per page step, and the page arithmetic of {@code :268}. */
    public static final int TRAY_PAGE_STEP = 5;
    /** The page arrows: 24×8 at (51, 121) / (27, 121) ({@code :244-250}). */
    public static final int PAGE_NEXT_X = 51;
    public static final int PAGE_PREV_X = 27;
    public static final int PAGE_ARROW_Y = 121;
    public static final int PAGE_ARROW_W = 24;
    public static final int PAGE_ARROW_H = 8;
    /** The fade of a tray cell whose pool is empty: 0.33 alpha ({@code :279-282}). */
    public static final int FADED_ALPHA = 0x54;

    private NoteGridView() {
    }

    // ------------------------------------------------------------------ views

    /** One grid cell as the screen draws it. Offsets are from the panel origin. */
    public record Cell(String key, int q, int r, int centerX, int centerY,
                       int left, int top, int span, int type, String aspectTag,
                       int colour, boolean discovered, boolean highlighted, boolean hovered) {

        public boolean required() {
            return type == TYPE_REQUIRED;
        }

        public boolean blank() {
            return type == TYPE_BLANK;
        }

        public boolean typed() {
            return type == TYPE_TYPED;
        }

        /** The aspect's colour with the alpha the original drew this cell at. */
        public int fillColour() {
            if (aspectTag == null) {
                return HEX_OUTLINE_COLOUR;
            }
            int alpha = (!discovered || blank()) ? 0xFF : (highlighted || required() ? FULL_ALPHA : TYPED_ALPHA);
            return (alpha << 24) | (colour & 0xFFFFFF);
        }

        /** True when the cell draws the original's black "unknown aspect" overlay. */
        public boolean unknown() {
            return aspectTag != null && !discovered;
        }
    }

    /** One connection line, in panel offsets, centre to centre. */
    public record Link(int x1, int y1, int x2, int y2) {
    }

    /** The chain walk: the lines to draw and the cells to draw bright. */
    public record Chain(List<Link> links, Set<String> highlighted) {

        /** An empty walk: no lines, nothing bright. */
        public static Chain empty() {
            return new Chain(List.of(), Set.of());
        }
    }

    /** What a click on the grid asks the server for. */
    public enum Arm {
        /** No arm. */
        NONE,
        /** Write the armed aspect into a blank cell (the original's drop arm). */
        WRITE,
        /** Clear a typed cell (the original's {@code checkClickedHex}). */
        ERASE
    }

    /** A grid arm with its coordinates; {@code aspectTag} is null for {@link Arm#ERASE}. */
    public record Request(Arm arm, int q, int r, String aspectTag) {

        public static final Request NONE = new Request(Arm.NONE, 0, 0, null);

        public boolean writes() {
            return arm == Arm.WRITE;
        }

        public boolean erases() {
            return arm == Arm.ERASE;
        }
    }

    /** One aspect-tray cell in the original's 5×5 layout. */
    public record TrayCell(String tag, int colour, int left, int top, int span,
                           int amount, boolean faded, boolean selected) {
    }

    // -------------------------------------------------------------- geometry

    /** The cell centre's panel-x offset: {@code 169 + hex.toPixel(9).x} ({@code :500}). */
    public static int centerX(final Hex hex) {
        return (int) Math.round(GRID_X + hex.toPixel(HEX_SIZE).x());
    }

    /** The cell centre's panel-y offset: {@code 83 + hex.toPixel(9).y} ({@code :500}). */
    public static int centerY(final Hex hex) {
        return (int) Math.round(GRID_Y + hex.toPixel(HEX_SIZE).y());
    }

    /**
     * The flat-top hex silhouette the original's {@code hex1.png} covered: the
     * width of each row of a {@code span}×{@code span} hexagon whose left and
     * right points sit at the vertical middle. The port fills these rows with
     * {@code hLine} (ARR, D-004).
     */
    public static int[] hexWidths(final int span) {
        final int inset = span / 4;
        final int half = span / 2;
        final int[] widths = new int[span];
        for (int row = 0; row < span; row++) {
            final int step = row < half ? row : span - 1 - row;
            final int grow = half <= 1 ? 2 * inset : (int) Math.round(2.0 * inset * step / (half - 1));
            widths[row] = span - 2 * inset + Math.min(grow, 2 * inset);
        }
        return widths;
    }

    /** The x offset a hex row of {@code width} starts at, centred in {@code span}. */
    public static int hexLeft(final int span, final int width) {
        return (span - width) / 2;
    }

    // ------------------------------------------------------------------ chain

    /**
     * The original's {@code checkConnections} walk ({@code GuiResearchTable.java:570-597}),
     * run from every required cell whose aspect the player has discovered. The
     * lines are keyed both ways like the original's map, so each pair appears
     * once; the target of every accepted edge joins the highlight set.
     */
    public static Chain chain(final ResearchNoteData note, final Predicate<String> discovered) {
        final Map<String, Hex[]> lines = new LinkedHashMap<>();
        final Set<String> checked = new LinkedHashSet<>();
        final Set<String> highlight = new LinkedHashSet<>();
        if (note == null) {
            return new Chain(List.of(), Set.of());
        }
        for (Hex hex : orderedHexes(note)) {
            final HexEntry entry = note.entryAt(hex);
            if (entry != null && entry.type() == TYPE_REQUIRED
                    && entry.aspectTag() != null && discovered.test(entry.aspectTag())) {
                checkConnections(note, discovered, hex, checked, lines, highlight);
            }
        }
        final List<Link> links = new ArrayList<>(lines.size());
        for (Hex[] pair : lines.values()) {
            links.add(new Link(centerX(pair[0]), centerY(pair[0]), centerX(pair[1]), centerY(pair[1])));
        }
        return new Chain(links, highlight);
    }

    private static void checkConnections(final ResearchNoteData note, final Predicate<String> discovered,
                                         final Hex hex, final Set<String> checked,
                                         final Map<String, Hex[]> lines, final Set<String> highlight) {
        checked.add(hex.toString());
        final HexEntry own = note.entryAt(hex);
        if (own == null || own.aspectTag() == null) {
            return;
        }
        for (int direction = 0; direction < 6; direction++) {
            final Hex target = hex.getNeighbour(direction);
            final String key = target.toString();
            if (checked.contains(key)) {
                continue;
            }
            final HexEntry entry = note.entryAt(target);
            if (entry == null || entry.type() < TYPE_REQUIRED || entry.aspectTag() == null) {
                continue;
            }
            if (!discovered.test(own.aspectTag()) || !discovered.test(entry.aspectTag())
                    || !NotePuzzle.componentsMatch(own.aspectTag(), entry.aspectTag())) {
                continue;
            }
            final String forward = hex + ":" + key;
            final String backward = key + ":" + hex;
            if (!lines.containsKey(forward) && !lines.containsKey(backward)) {
                lines.put(forward, new Hex[]{hex, target});
                highlight.add(key);
            }
            checkConnections(note, discovered, target, checked, lines, highlight);
        }
    }

    // ------------------------------------------------------------------ cells

    /** The note's hexes in a stable draw order: row by row, then column. */
    public static List<Hex> orderedHexes(final ResearchNoteData note) {
        final List<Hex> hexes = new ArrayList<>(note.hexes().values());
        hexes.sort(Comparator.comparingInt(Hex::r).thenComparingInt(Hex::q));
        return hexes;
    }

    /**
     * One view per {@code hexEntries} cell. The {@code chain} decides which
     * typed cells draw bright; {@code hoveredKey} is the key of the cell under
     * the cursor (the original draws its highlight quad, {@code :525-527}).
     */
    public static List<Cell> cells(final ResearchNoteData note, final Predicate<String> discovered,
                                   final Chain chain, final String hoveredKey) {
        final List<Cell> out = new ArrayList<>();
        if (note == null) {
            return out;
        }
        for (Hex hex : orderedHexes(note)) {
            final Cell cell = viewOf(note, hex, discovered, chain, hex.toString().equals(hoveredKey));
            if (cell != null) {
                out.add(cell);
            }
        }
        return out;
    }

    /** The view of one hex, or null when the note has no entry for it. */
    public static Cell viewOf(final ResearchNoteData note, final Hex hex, final Predicate<String> discovered,
                              final Chain chain, final boolean hovered) {
        final HexEntry entry = note.entryAt(hex);
        if (entry == null) {
            return null;
        }
        final String tag = entry.aspectTag();
        final int colour = tag == null ? 0 : AspectColour.of(tag);
        final boolean known = tag != null && discovered.test(tag);
        return new Cell(hex.toString(), hex.q(), hex.r(), centerX(hex), centerY(hex),
                centerX(hex) + TAG_DX, centerY(hex) + TAG_DY, CELL_SPAN, entry.type(), tag,
                colour, known, chain.highlighted().contains(hex.toString()), hovered);
    }

    /**
     * The cell under a cursor position, using the original's hit test:
     * {@code new Pixel(mx - (gx + 169), my - (gy + 83)).toHex(9)} and the note's
     * own cell map ({@code :151-155}, {@code :500-502}, {@code :726-731}).
     * Null when the point lands on no cell.
     */
    public static Cell cellAt(final ResearchNoteData note, final int panelX, final int panelY,
                              final Predicate<String> discovered, final Chain chain) {
        final Hex hex = hexAt(panelX, panelY);
        return note == null ? null : viewOf(note, hex, discovered, chain, true);
    }

    /** The hex a panel point lands on, in the original's pixel space. */
    public static Hex hexAt(final int panelX, final int panelY) {
        return new Pixel(panelX - GRID_X, panelY - GRID_Y).toHex(HEX_SIZE);
    }

    /**
     * The grid arm for a cursor position. {@code armedTag} is the aspect the
     * player picked out of the tray (the original's {@code draggedAspect}).
     *
     * <p>Gate for gate: a blank cell takes the write only while the note is not
     * complete (a complete note has no blank cell to land on, and the server's
     * write arm refuses it, {@code ResearchNoteMenu.placeAspect}), and a typed
     * cell takes the erase with no ink gate client-side — the server gates the
     * ink, as the original's {@code checkClickedHex} did.</p>
     */
    public static Request requestAt(final ResearchNoteData note, final int panelX, final int panelY,
                                    final String armedTag, final Predicate<String> discovered) {
        if (note == null) {
            return Request.NONE;
        }
        final Cell cell = cellAt(note, panelX, panelY, discovered, chain(note, discovered));
        if (cell == null) {
            return Request.NONE;
        }
        if (cell.blank() && armedTag != null && !note.complete()) {
            return new Request(Arm.WRITE, cell.q(), cell.r(), armedTag);
        }
        if (cell.typed()) {
            return new Request(Arm.ERASE, cell.q(), cell.r(), null);
        }
        return Request.NONE;
    }

    // ------------------------------------------------------------ copy plate

    /**
     * The copy plate's hit box: 24×24 at (37, 5) ({@code :683-688}). The gate
     * that lights the plate (note + complete + RESEARCHDUPE) stays the menu's
     * {@code canDuplicate()}; this is only the geometry.
     */
    public static boolean inPlate(final int panelX, final int panelY) {
        final int localX = panelX - PLATE_X;
        final int localY = panelY - PLATE_Y;
        return localX >= 0 && localY >= 0 && localX < PLATE_SPAN && localY < PLATE_SPAN;
    }

    // ------------------------------------------------------------ aspect tray

    /** The original's {@code lastPage = (aspects.size() - 20) / 5} ({@code :268}). */
    public static int lastPage(final int discoveredCount) {
        return (discoveredCount - 20) / TRAY_PAGE_STEP;
    }

    /**
     * The tray page: entries {@code page * 5} and up, at most 25, laid out
     * column-major in 16 px cells ({@code :271-284}). {@code pool} is the
     * player's aspect pool, and a cell fades when it holds none of the aspect.
     */
    public static List<TrayCell> tray(final List<String> discoveredTags, final int page,
                                      final ToIntFunction<String> pool, final String selectedTag) {
        final List<TrayCell> out = new ArrayList<>();
        int drawn = 0;
        int index = 0;
        for (String tag : discoveredTags) {
            if (index++ < page * TRAY_PAGE_STEP) {
                continue;
            }
            if (drawn >= TRAY_MAX) {
                break;
            }
            final int column = drawn / TRAY_PAGE_STEP;
            final int row = drawn % TRAY_PAGE_STEP;
            final int amount = pool.applyAsInt(tag);
            out.add(new TrayCell(tag, AspectColour.of(tag),
                    TRAY_X + column * TRAY_CELL, TRAY_Y + row * TRAY_CELL, TRAY_CELL,
                    amount, amount <= 0, tag.equals(selectedTag)));
            drawn++;
        }
        return out;
    }

    /**
     * The tray cell under a cursor position, the original's
     * {@code getClickedAspect} ({@code :742-762}): the 16 px cell test inside
     * the tray, and with {@code requirePool} the pool gate that keeps an empty
     * pool cell from being picked up.
     */
    public static TrayCell trayCellAt(final List<TrayCell> tray, final int panelX, final int panelY,
                                      final boolean requirePool) {
        for (TrayCell cell : tray) {
            final int localX = panelX - cell.left();
            final int localY = panelY - cell.top();
            if (localX >= 0 && localY >= 0 && localX < cell.span() && localY < cell.span()
                    && (!requirePool || cell.amount() > 0)) {
                return cell;
            }
        }
        return null;
    }

    /** True when a panel point is inside the tray's 80×80 region ({@code :136-138}). */
    public static boolean inTray(final int panelX, final int panelY) {
        final int localX = panelX - TRAY_X;
        final int localY = panelY - TRAY_Y;
        return localX >= 0 && localY >= 0 && localX < TRAY_REGION && localY < TRAY_REGION;
    }

    /** True when a panel point is on the next-page arrow. */
    public static boolean inPageNext(final int panelX, final int panelY) {
        return panelX >= PAGE_NEXT_X && panelX < PAGE_NEXT_X + PAGE_ARROW_W
                && panelY >= PAGE_ARROW_Y && panelY < PAGE_ARROW_Y + PAGE_ARROW_H;
    }

    /** True when a panel point is on the previous-page arrow. */
    public static boolean inPagePrev(final int panelX, final int panelY) {
        return panelX >= PAGE_PREV_X && panelX < PAGE_PREV_X + PAGE_ARROW_W
                && panelY >= PAGE_ARROW_Y && panelY < PAGE_ARROW_Y + PAGE_ARROW_H;
    }

    /** The aspect's display colour, from the shipped aspect definitions. */
    public static final class AspectColour {

        private AspectColour() {
        }

        public static int of(final String tag) {
            for (AspectDefinitions.Definition definition : AspectDefinitions.all()) {
                if (definition.tag().equals(tag)) {
                    return definition.color() & 0xFFFFFF;
                }
            }
            return 0xFFFFFF;
        }
    }
}
