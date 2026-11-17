package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.aspect.AspectCombinations;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Pixel;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The research-note screen's state: the hex grid, the paginated aspect pool,
 * the drag state machine, the click arms, the veil gate and the combine cost
 * (NOTE-SCREEN-2, t_32a29c93).
 *
 * <p><b>Why this class exists.</b> The 1.7.10 original is immediate-mode GL —
 * every layout number, every gate and every sound sits inline in
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchTable.java}
 * ({@code Tessellator} / {@code GL11}, no 1.21.1 equivalent to javap against),
 * and the game-test harness is a dedicated server that never constructs a
 * {@code Screen} and fires no mouse event. So the measurable half of the
 * screen — grid geometry, the gate order, the veil gate, the combine cost —
 * lives here, in a class that imports <b>no</b> Minecraft, NeoForge or Mojang
 * type, and {@code client.ResearchNoteScreen} draws exactly what this
 * computes. That is the posture docs/porting-decisions.md D-019 requires and
 * the split {@code ResearchBrowserModel} / {@code ResearchDetailModel} already
 * ship for the book.</p>
 *
 * <p>Every number below is the original's, cited to the line it comes from.
 * The state the window draws is the port's own seam: the original read the
 * research-table tile in-process, the port's screen reads its menu
 * ({@code menus.ResearchNoteMenu}, BD-26), and this model is fed from the
 * menu's note state plus the synced knowledge attachment.</p>
 *
 * <p>The original's research-table {@code bonusAspects} pool has no port-side
 * counterpart (the tile is not ported), so every bonus-pool term the original
 * reads is the empty pool here — the same BD-26 deviation the menu's write arm
 * documents.</p>
 */
public final class NoteScreenModel {

    // ------------------------------------------------------------- the panel
    /** Original {@code GuiResearchTable.java:70} — {@code ySize = 255}. */
    public static final int PANEL_WIDTH = 255;
    /** Original {@code GuiResearchTable.java:71} — {@code xSize = 255}. */
    public static final int PANEL_HEIGHT = 255;

    // ------------------------------------------------------------- the grid
    /** Original {@code GuiResearchTable.java:43} — {@code HEX_SIZE = 9}. */
    public static final int HEX_SIZE = 9;
    /** Original {@code GuiResearchTable.java:153} — the grid centre x. */
    public static final int GRID_CENTRE_X = 169;
    /** Original {@code GuiResearchTable.java:154} — the grid centre y. */
    public static final int GRID_CENTRE_Y = 83;
    /**
     * Original {@code GuiResearchTable.java:533,553,558} — the tag plate sits
     * at the hex centre offset by this: {@code x + 161 + pix.x} with the centre
     * at 169, i.e. −8. The hex sprite spans the same ±8 ({@code :402-405}).
     */
    public static final int HEX_PLATE_OFFSET = -8;
    /** The original's parchment sheet: {@code :456} blit (94, 8, 150, 150). */
    public static final int SHEET_X = 94;
    public static final int SHEET_Y = 8;
    public static final int SHEET_SIZE = 150;
    /**
     * The original's panel is two blits, not one square: {@code :236}
     * (0, 0, 255, 167) and {@code :237} (40, 167, 184, 88) — the lower strip
     * carries the player-inventory rows and is narrower than the panel.
     */
    public static final int PANEL_FIELD_HEIGHT = 167;
    public static final int PANEL_STRIP_X = 40;
    public static final int PANEL_STRIP_Y = 167;
    public static final int PANEL_STRIP_WIDTH = 184;
    public static final int PANEL_STRIP_HEIGHT = 88;

    // -------------------------------------------------------- the aspect pool
    /** Original {@code GuiResearchTable.java:136-137,258} — pool origin. */
    public static final int POOL_X = 10;
    public static final int POOL_Y = 40;
    /** Original {@code :275-276} — {@code drawn / 5 * 16}, {@code drawn % 5 * 16}. */
    public static final int POOL_CELL = 16;
    /** Original {@code :273} — five columns, and the draw cap of 25 cells. */
    public static final int POOL_COLUMNS = 5;
    public static final int POOL_CELL_COUNT = 25;
    /** Original {@code :273} — a page starts at index {@code page * 5}. */
    public static final int PAGE_STEP = 5;
    /** Original {@code :268} — {@code lastPage = (count - 20) / 5}. */
    public static final int LAST_PAGE_OFFSET = 20;
    /** Original {@code :138} — the press area is the pool's 80x80 block. */
    public static final int POOL_PRESS_SIZE = 80;
    /** Original {@code :274} — a 0-amount entry draws faded, at alpha 0.33. */
    public static final float FADED_ALPHA = 0.33F;

    // --------------------------------------------------------- the two slots
    /** Original {@code :305,309} — the pool-relative select slots (3, 99) / (61, 99). */
    public static final int SELECT1_X = POOL_X + 3;
    public static final int SELECT2_X = POOL_X + 61;
    public static final int SELECT_Y = POOL_Y + 99;
    /** Original {@code :177-187} — the drop boxes are ±16 around (20, 146) / (79, 146). */
    public static final int DROP1_X = 20;
    public static final int DROP2_X = 79;
    public static final int DROP_Y = 146;
    public static final int DROP_RADIUS = 16;
    /** Original {@code :662-672} — the clear boxes: (11, 137) and (71, 137), 16x16. */
    public static final int CLEAR1_X = 11;
    public static final int CLEAR2_X = 71;
    public static final int CLEAR_Y = 137;
    public static final int CLEAR_SIZE = 16;

    // --------------------------------------------------------------- buttons
    /** Original {@code :247,630} — the combine button (35, 139) 32x16. */
    public static final int COMBINE_X = 35;
    public static final int COMBINE_Y = 139;
    public static final int COMBINE_W = 32;
    public static final int COMBINE_H = 16;
    /** Original {@code :631} — {@code butcount2 = nanoTime() + 200000000L}. */
    public static final long COMBINE_COOLDOWN_NANOS = 200_000_000L;
    /** Original {@code :239-243,651,657} — the page arrows (27/51, 121) 24x8. */
    public static final int PAGE_PREV_X = 27;
    public static final int PAGE_NEXT_X = 51;
    public static final int PAGE_ARROW_Y = 121;
    public static final int PAGE_ARROW_W = 24;
    public static final int PAGE_ARROW_H = 8;
    /** Original {@code :105-107,255,684} — the copy plate (37, 5) 24x24. */
    public static final int DUPE_X = 37;
    public static final int DUPE_Y = 5;
    public static final int DUPE_SIZE = 24;
    /** Original {@code :114-129} — the copy-cost panel at (100, 21) 48x16. */
    public static final int COPY_PANEL_X = 100;
    public static final int COPY_PANEL_Y = 21;
    public static final int COPY_PANEL_W = 48;
    public static final int COPY_PANEL_H = 16;
    /** Original {@code :129} — the panel's header line sits 9 px above the plate. */
    public static final int COPY_HEADER_Y = 12;
    /**
     * Original {@code :124} — the copy plate's tags sit to the RIGHT of the
     * 48-wide plate, at the plate's y, one 16px cell per tag:
     * {@code gx + 100 + 48 + count * 16}, {@code gy + 21}.
     */
    public static final int COPY_TAG_X = COPY_PANEL_X + COPY_PANEL_W;
    public static final int COPY_TAG_Y = COPY_PANEL_Y;
    public static final int COPY_TAG_STEP = 16;
    /** Original {@code :218-226} — the out-of-ink tooltip block at (157 - halfWidth, 84). */
    public static final int NO_INK_TOOLTIP_X = 157;
    public static final int NO_INK_TOOLTIP_Y = 84;

    // --------------------------------------------------------------- the veil
    /** Original {@code :544} — the undiscovered plate is drawn black at alpha 0.5. */
    public static final float VEIL_ALPHA = 0.5F;
    /** Original {@code :395,401} — the blank hex sprite draws at alpha 0.25. */
    public static final float HEX_OUTLINE_ALPHA = 0.25F;

    // ------------------------------------------------------- the sweep + orb
    /**
     * Original {@code drawLine} ({@code :433-450}): the connection strip is a
     * fixed colour — {@code glColor4f(0.0F, 0.6F, 0.8F, alpha)} at
     * {@code glLineWidth(3)} — NOT the aspect's colour. The port draws it as
     * 3px step fills (the card's "GL line strips → 1px step fills").
     */
    public static final int CONNECTION_LINE_COLOUR = 0xFF0099CC;
    public static final int CONNECTION_LINE_WIDTH = 3;
    /**
     * The sweep's alpha, the original's animation ({@code :434-435}):
     * {@code 0.3F + MathHelper.sin((count + x)) * 0.3F + 0.3F} where
     * {@code count} is the client's tick counter ({@code Minecraft.player.ticksExisted}).
     */
    public static float connectionAlpha(final int tickCount, final double x) {
        final float alpha = 0.3F + (float) Math.sin((float) (tickCount + x)) * 0.3F + 0.3F;
        return Math.max(0.0F, Math.min(1.0F, alpha));
    }

    /**
     * The required hex's orb, the original's animated colour ({@code :860-867}):
     * red {@code 0.7 + sin((count + x) / 10) * 0.15 + 0.15}, green on
     * {@code (count + x + y) / 11}, blue on {@code (count + y) / 12}.
     */
    public static int orbColour(final int tickCount, final double x, final double y) {
        final float red = 0.7F + (float) Math.sin((float) ((tickCount + x) / 10.0)) * 0.15F + 0.15F;
        final float green = 0.7F + (float) Math.sin((float) ((tickCount + x + y) / 11.0)) * 0.15F + 0.15F;
        final float blue = 0.7F + (float) Math.sin((float) ((tickCount + y) / 12.0)) * 0.15F + 0.15F;
        return 0xFF000000
                | (channel(red) << 16) | (channel(green) << 8) | channel(blue);
    }

    private static int channel(final float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
    }

    /** Original {@code :559} — an unhighlighted player-placed tag draws at alpha 0.66. */
    public static final float PLACED_ALPHA = 0.66F;
    /** Original {@code :554} — a required or highlighted tag draws at alpha 1.0. */
    public static final float REQUIRED_ALPHA = 1.0F;

    // --------------------------------------------------------------- the arms
    /** Original {@code :75} — the tooltip preview arm needs RESEARCHER1. */
    public static final String RESEARCH_RESEARCHER1 = "RESEARCHER1";
    /** Original {@code :76,694} — the shift-combine arm needs RESEARCHER2. */
    public static final String RESEARCH_RESEARCHER2 = "RESEARCHER2";
    /** Original {@code :77,254,683} — the copy plate needs RESEARCHDUPE. */
    public static final String RESEARCH_DUPE = "RESEARCHDUPE";

    /** The original's hex entry types ({@code ResearchManager.HexEntry}). */
    public static final int TYPE_EMPTY = 0;
    public static final int TYPE_REQUIRED = 1;
    public static final int TYPE_PLACED = 2;

    /**
     * The sounds the window plays, each with the original's volume and pitch
     * ({@code GuiResearchTable.java:770-858}). The screen forwards these to the
     * registered {@code thaumcraft:} sound events; the probes assert the table
     * so the screen cannot quietly drop one.
     */
    public enum UiSound {
        /** {@code :770-783} — the combine/copy click. */
        BUTTON_CLICK("cameraclack", 0.4F, 1.0F),
        /** {@code :785-798} — an aspect picked up, dropped on a slot, cleared. */
        BUTTON_ASPECT("hhoff", 0.2F, -1.0F),
        /** {@code :800-813} — a combine fires. */
        BUTTON_COMBINE("hhon", 0.3F, 1.0F),
        /** {@code :815-828} — a write lands on the grid. */
        BUTTON_WRITE("write", 0.2F, 1.0F),
        /** {@code :830-843} — a cell is erased. */
        BUTTON_ERASE("erase", 0.2F, -1.0F),
        /** {@code :845-858} — a page arrow. */
        BUTTON_SCROLL("key", 0.3F, 1.0F);

        private final String id;
        private final float volume;
        private final float pitch;

        UiSound(final String id, final float volume, final float pitch) {
            this.id = id;
            this.volume = volume;
            this.pitch = pitch;
        }

        /** The registered sound path; the full id is {@code thaumcraft:<path>}. */
        public String soundPath() {
            return this.id;
        }

        public float volume() {
            return this.volume;
        }

        /** A negative pitch means the original randomises it: 1 + rand * 0.1. */
        public boolean pitchRandomised() {
            return this.pitch < 0.0F;
        }

        public float pitch() {
            return pitchRandomised() ? 1.0F : this.pitch;
        }
    }

    /** One pool cell as drawn: the tag, its top-left pixel, its amount, faded. */
    public record PoolCell(String tag, int x, int y, int amount, boolean faded) {
    }

    /**
     * One grid cell as drawn. {@code outline} is the empty/blank hex sprite
     * (drawn only on an open note, {@code :522-524}), {@code orb} the animated
     * orb of a required hex ({@code :533}), {@code veiled} the black plate over
     * an aspect the player has not discovered, {@code highlighted} the
     * connection-lit cell, and {@code tagAlpha} the original's icon alpha:
     * 1.0 for a required or highlighted cell ({@code :551-555}), 0.66 for a
     * player-placed one ({@code :556-560}), 0 when the cell draws no tag.
     *
     * @param outlineAlpha the blank hex's alpha, the original's 0.25 ({@code :395}),
     *                     0 when no outline is drawn.
     * @param hovered      the cell under the cursor. The original draws the hover
     *                     highlight only on a NON-required cell of an open note
     *                     ({@code :522-531}), so this is false on a type-1 cell.
     */
    public record HexCell(String key, int q, int r, int centreX, int centreY,
                          int plateX, int plateY, int type, String tag,
                          boolean veiled, boolean outline, boolean orb,
                          boolean highlighted, boolean hovered, float tagAlpha,
                          float outlineAlpha) {
    }

    /** The drop arm's decision, in the original's gate order ({@code :150-208}). */
    public enum DropKind { HEX, SELECT_ONE, SELECT_TWO, POOL_PICKUP_ONE, POOL_PICKUP_TWO, NOTHING }

    /**
     * A drop decision: the kind, the hex for a grid write, and the sounds the
     * original plays in its own order — a grid write plays the combine jingle
     * AND the write ({@code :157-158}), a select-slot drop plays hhoff
     * ({@code :180,188}) and the pool pick-up plays NOTHING ({@code :193-202}).
     */
    public record Drop(DropKind kind, String hexKey, List<UiSound> sounds) {
    }

    /**
     * A press decision ({@code :135-145}): the aspect the cursor is over,
     * whether this press ARMED the drag (the original's {@code isMouseButtonDown
     * == 0} gate), and the sounds — the pick-up plays hhoff ({@code :141}), and
     * only when the press armed the drag.
     */
    public record Press(String tag, boolean started, List<UiSound> sounds) {
    }

    /** The click arms, in the original's gate order ({@code :623-724}). */
    public enum ClickAction {
        /** {@code :630-647} — the combine button: click THEN combine (:632-633). */
        COMBINE(UiSound.BUTTON_CLICK, UiSound.BUTTON_COMBINE),
        /** {@code :651-654} — page arrow left. */
        PAGE_PREV(UiSound.BUTTON_SCROLL),
        /** {@code :657-660} — page arrow right. */
        PAGE_NEXT(UiSound.BUTTON_SCROLL),
        /** {@code :661-669} — the select-1 clear box. */
        CLEAR_ONE(UiSound.BUTTON_ASPECT),
        /** {@code :670-679} — the select-2 clear box. */
        CLEAR_TWO(UiSound.BUTTON_ASPECT),
        /** {@code :726-740} — the erase arm: combine THEN erase (:731-732). */
        ERASE(UiSound.BUTTON_COMBINE, UiSound.BUTTON_ERASE),
        /** {@code :683-691} — the copy plate (the original's button 5). */
        DUPLICATE(UiSound.BUTTON_CLICK),
        /** {@code :694-718} — shift-combine a pool aspect's two components. */
        SHIFT_COMBINE(UiSound.BUTTON_COMBINE),
        /** A click inside a button's box that its own gate refused. */
        REFUSED,
        /** Nothing in range. */
        NONE;

        private final List<UiSound> sounds;

        ClickAction(final UiSound... sounds) {
            this.sounds = List.of(sounds);
        }

        /** The sounds the original's arm plays, in its order. */
        public List<UiSound> sounds() {
            return this.sounds;
        }
    }

    /** A combine: the result tag (null when the pair has no result) and its cost. */
    public record Combination(String resultTag, Map<String, Integer> cost, boolean affordable) {
    }

    // ------------------------------------------------------------- the state
    private ResearchNoteData note;
    private PlayerKnowledgeData knowledge = PlayerKnowledgeData.empty();
    private int page;
    private String select1;
    private String select2;
    private String draggedAspect;
    private long combineBlockedUntilNanos;
    private int inkRemaining = Integer.MAX_VALUE;
    private boolean scribePresent;

    public NoteScreenModel() {
    }

    public NoteScreenModel(final ResearchNoteData note, final PlayerKnowledgeData knowledge) {
        this.note = note;
        setKnowledge(knowledge);
    }

    public ResearchNoteData note() {
        return this.note;
    }

    public void setNote(final ResearchNoteData value) {
        this.note = value;
    }

    public PlayerKnowledgeData knowledge() {
        return this.knowledge;
    }

    /** The knowledge the pool, the veil and the arms read (the synced attachment). */
    public final void setKnowledge(final PlayerKnowledgeData value) {
        this.knowledge = value == null ? PlayerKnowledgeData.empty() : value;
    }

    /** The menu's ink mirror ({@code DATA_INK}); a scribe with 0 ink shows the tooltip. */
    public void setInk(final boolean hasScribe, final int remaining) {
        this.scribePresent = hasScribe;
        this.inkRemaining = remaining;
    }

    public boolean hasNote() {
        return this.note != null;
    }

    public boolean noteComplete() {
        return this.note != null && this.note.complete();
    }

    public int copies() {
        return this.note == null ? 0 : this.note.copies();
    }

    // ------------------------------------------------------------- the pool
    public int page() {
        return this.page;
    }

    public void setPage(final int value) {
        this.page = Math.max(0, value);
    }

    /** The discovered aspects in the pool's draw order: {@code getAspectsSorted()}. */
    public List<String> discoveredTags() {
        final Set<String> tags = new LinkedHashSet<>();
        for (final PlayerKnowledgeData.AspectEntry entry : this.knowledge.aspectsDiscovered()) {
            if (entry.key() != null) {
                tags.add(entry.key());
            }
        }
        // AspectList.getAspectsSorted() sorts by tag; the pool walks that order.
        return List.copyOf(new java.util.TreeSet<>(tags));
    }

    /**
     * The original's {@code lastPage = (count - 20) / 5} ({@code :268}), kept
     * with the original's integer arithmetic — below 20 discovered aspects it
     * is negative, and {@code page < lastPage} is then never true.
     */
    public int lastPage() {
        return (discoveredTags().size() - LAST_PAGE_OFFSET) / PAGE_STEP;
    }

    public boolean canPageBack() {
        return this.page > 0;
    }

    public boolean canPageForward() {
        return this.page < lastPage();
    }

    /** The 25 pool cells of the current page, in the original's walk ({@code :272-289}). */
    public List<PoolCell> poolCells() {
        final List<String> sorted = discoveredTags();
        final List<PoolCell> cells = new ArrayList<>();
        int drawn = 0;
        for (int index = 0; index < sorted.size(); index++) {
            if (index >= this.page * PAGE_STEP && drawn < POOL_CELL_COUNT) {
                final String tag = sorted.get(index);
                final int amount = this.knowledge.aspectAmount(tag);
                cells.add(new PoolCell(tag,
                        POOL_X + drawn / POOL_COLUMNS * POOL_CELL,
                        POOL_Y + drawn % POOL_COLUMNS * POOL_CELL,
                        amount,
                        amount <= 0));
                drawn++;
            }
        }
        return cells;
    }

    /**
     * Original {@code getClickedAspect} ({@code :742-768}): the aspect under the
     * cursor, only when it is payable (amount &gt; 0) unless {@code ignoreZero}
     * (the shift arm reads 0-amount entries too).
     */
    public String aspectAt(final int mouseX, final int mouseY, final boolean ignoreZero) {
        for (final PoolCell cell : poolCells()) {
            final int dx = mouseX - cell.x();
            final int dy = mouseY - cell.y();
            if ((ignoreZero || cell.amount() > 0)
                    && dx >= 0 && dy >= 0 && dx < POOL_CELL && dy < POOL_CELL) {
                return cell.tag();
            }
        }
        return null;
    }

    /**
     * The original's select-slot tooltip arms ({@code :360-379}): the slot under
     * the cursor, at the pool-relative (3, 99) / (61, 99) 16x16 boxes, or null.
     */
    public String selectSlotAt(final int mouseX, final int mouseY) {
        if (this.select1 != null && inBox(mouseX, mouseY, SELECT1_X, SELECT_Y, POOL_CELL, POOL_CELL)) {
            return this.select1;
        }
        if (this.select2 != null && inBox(mouseX, mouseY, SELECT2_X, SELECT_Y, POOL_CELL, POOL_CELL)) {
            return this.select2;
        }
        return null;
    }

    public int select1Amount() {
        return this.select1 == null ? 0 : this.knowledge.aspectAmount(this.select1);
    }

    public int select2Amount() {
        return this.select2 == null ? 0 : this.knowledge.aspectAmount(this.select2);
    }

    /**
     * Original {@code drawAspects} ({@code :292-302}): a select slot whose
     * aspect has left the pool is dropped by the window itself, every frame.
     */
    public void clearUnpayableSelections() {
        if (this.select1 != null && this.knowledge.aspectAmount(this.select1) <= 0) {
            this.select1 = null;
        }
        if (this.select2 != null && this.knowledge.aspectAmount(this.select2) <= 0) {
            this.select2 = null;
        }
    }

    public String select1() {
        return this.select1;
    }

    public String select2() {
        return this.select2;
    }

    public void setSelect1(final String tag) {
        this.select1 = tag;
    }

    public void setSelect2(final String tag) {
        this.select2 = tag;
    }

    public String draggedAspect() {
        return this.draggedAspect;
    }

    // ------------------------------------------------------------- the grid
    /** The grid's cells keyed by the original's {@code "q:r"} string. */
    public Map<String, HexEntry> gridEntries() {
        if (this.note == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(this.note.hexEntries());
    }

    /** The hex under the cursor: original {@code :153-155} / {@code :500-502}. */
    public Hex hexAt(final int mouseX, final int mouseY) {
        return new Pixel(mouseX - GRID_CENTRE_X, mouseY - GRID_CENTRE_Y).toHex(HEX_SIZE);
    }

    /** The cell lit up under the cursor, computed the way the window reads it. */
    public List<HexCell> hexCells(final int mouseX, final int mouseY) {
        if (this.note == null) {
            return List.of();
        }
        final Hex hovered = hexAt(mouseX, mouseY);
        final Set<String> highlighted = highlightedHexes();
        final List<HexCell> cells = new ArrayList<>();
        final boolean complete = noteComplete();
        for (final Map.Entry<String, HexEntry> gridEntry : this.note.hexEntries().entrySet()) {
            final Hex hex = Hex.fromKey(gridEntry.getKey());
            final HexEntry entry = gridEntry.getValue();
            final Pixel pixel = hex.toPixel(HEX_SIZE);
            final int centreX = GRID_CENTRE_X + (int) Math.round(pixel.x());
            final int centreY = GRID_CENTRE_Y + (int) Math.round(pixel.y());
            final boolean discovered = entry.aspectTag() != null && this.knowledge.hasAspectDiscovered(entry.aspectTag());
            final boolean veiled = entry.aspectTag() != null && !this.knowledge.hasAspectDiscovered(entry.aspectTag());
            final boolean lit = highlighted.contains(gridEntry.getKey());
            final float alpha;
            if (lit || entry.type() == TYPE_REQUIRED) {
                alpha = REQUIRED_ALPHA;
            } else if (entry.type() == TYPE_PLACED) {
                alpha = PLACED_ALPHA;
            } else {
                alpha = 0.0F;
            }
            cells.add(new HexCell(gridEntry.getKey(), hex.q(), hex.r(), centreX, centreY,
                    centreX + HEX_PLATE_OFFSET, centreY + HEX_PLATE_OFFSET,
                    entry.type(), entry.aspectTag(),
                    veiled,
                    // The outline sprite is only drawn while the note is open
                    // (:522) and only on a non-required cell (:524).
                    !complete && entry.type() != TYPE_REQUIRED,
                    entry.type() == TYPE_REQUIRED,
                    lit,
                    // :522-531 — the hover highlight lives inside the
                    // non-required branch of an open note: a required hex, and
                    // every hex of a completed note, never highlight.
                    !complete && hex.equals(hovered) && entry.type() != TYPE_REQUIRED,
                    alpha,
                    !complete && entry.type() != TYPE_REQUIRED ? HEX_OUTLINE_ALPHA : 0.0F));
        }
        return cells;
    }

    public List<HexCell> hexCells() {
        return hexCells(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    /**
     * The connection sweep, ported from {@code checkConnections}
     * ({@code :570-597}): every required, discovered cell seeds a walk that
     * links to a neighbour of type &gt;= 1 when both aspects are discovered and
     * one is a direct component of the other. The lit set is what the window
     * draws at full alpha.
     */
    public List<Hex[]> connections() {
        final Map<String, Hex[]> lines = new LinkedHashMap<>();
        if (this.note == null) {
            return List.of();
        }
        final List<String> checked = new ArrayList<>();
        for (final Map.Entry<String, HexEntry> gridEntry : this.note.hexEntries().entrySet()) {
            final HexEntry entry = gridEntry.getValue();
            if (entry.type() == TYPE_REQUIRED && this.knowledge.hasAspectDiscovered(entry.aspectTag())) {
                walkConnections(Hex.fromKey(gridEntry.getKey()), checked, lines);
            }
        }
        return new ArrayList<>(lines.values());
    }

    /** The cells a connection lights up (the original's {@code highlight} list). */
    public Set<String> highlightedHexes() {
        final Set<String> lit = new LinkedHashSet<>();
        for (final Hex[] line : connections()) {
            lit.add(line[1].toString());
        }
        return lit;
    }

    private void walkConnections(final Hex hex, final List<String> checked, final Map<String, Hex[]> lines) {
        checked.add(hex.toString());
        final HexEntry self = this.note.hexEntries().get(hex.toString());
        for (int direction = 0; direction < 6; direction++) {
            final Hex target = hex.getNeighbour(direction);
            final String key = target.toString();
            final HexEntry entry = this.note.hexEntries().get(key);
            if (!checked.contains(key) && entry != null && entry.type() >= TYPE_REQUIRED) {
                final String first = self == null ? null : self.aspectTag();
                final String second = entry.aspectTag();
                if (first != null && second != null
                        && this.knowledge.hasAspectDiscovered(first)
                        && this.knowledge.hasAspectDiscovered(second)
                        && NotePuzzle.componentsMatch(first, second)) {
                    final String forward = hex + ":" + key;
                    final String back = key + ":" + hex;
                    if (!lines.containsKey(forward) && !lines.containsKey(back)) {
                        lines.put(forward, new Hex[]{hex, target});
                    }
                    // :593 — the original recurses on EVERY qualifying neighbour,
                    // not only when the pair added a new line.
                    walkConnections(target, checked, lines);
                }
            }
        }
    }

    // ------------------------------------------------------- the drag machine
    /**
     * The press arm ({@code :135-145}): a press inside the pool's 80x80 block
     * picks up the payable aspect under the cursor and nothing else.
     */
    public Press pressAt(final int mouseX, final int mouseY) {
        final int dx = mouseX - POOL_X;
        final int dy = mouseY - POOL_Y;
        if (this.draggedAspect == null && dx >= 0 && dy >= 0
                && dx < POOL_PRESS_SIZE && dy < POOL_PRESS_SIZE) {
            final String tag = aspectAt(mouseX, mouseY, false);
            if (tag != null) {
                this.draggedAspect = tag;
                // :141-143 — the pick-up plays hhoff as it arms the drag.
                return new Press(tag, true, List.of(UiSound.BUTTON_ASPECT));
            }
        }
        return new Press(this.draggedAspect, false, List.of());
    }

    /**
     * The release arm, in the original's gate order ({@code :150-208}): a
     * writeable hex first, then the select-1 box, then the select-2 box, then a
     * pick-up on the pool cell the drag started from.
     */
    public Drop releaseAt(final int mouseX, final int mouseY) {
        final String dragged = this.draggedAspect;
        if (dragged == null) {
            return new Drop(DropKind.NOTHING, null, List.of());
        }
        if (this.note != null) {
            final Hex hex = hexAt(mouseX, mouseY);
            final HexEntry entry = this.note.hexEntries().get(hex.toString());
            if (entry != null && entry.type() == TYPE_EMPTY) {
                this.draggedAspect = null;
                // :157-158 — the write plays the combine jingle AND the write.
                return new Drop(DropKind.HEX, hex.toString(),
                        List.of(UiSound.BUTTON_COMBINE, UiSound.BUTTON_WRITE));
            }
        }
        if (inDropBox(mouseX, mouseY, DROP1_X)) {
            this.select1 = dragged;
            this.draggedAspect = null;
            return new Drop(DropKind.SELECT_ONE, null, List.of(UiSound.BUTTON_ASPECT));
        }
        if (inDropBox(mouseX, mouseY, DROP2_X)) {
            this.select2 = dragged;
            this.draggedAspect = null;
            return new Drop(DropKind.SELECT_TWO, null, List.of(UiSound.BUTTON_ASPECT));
        }
        final String clicked = aspectAt(mouseX, mouseY, false);
        if (clicked != null && clicked.equals(dragged)) {
            if (this.select1 == null) {
                this.select1 = dragged;
                this.draggedAspect = null;
                // :193-202 — the pick-up arm fills a slot and plays NOTHING.
                return new Drop(DropKind.POOL_PICKUP_ONE, null, List.of());
            }
            if (this.select2 == null) {
                this.select2 = dragged;
                this.draggedAspect = null;
                return new Drop(DropKind.POOL_PICKUP_TWO, null, List.of());
            }
        }
        return new Drop(DropKind.NOTHING, null, List.of());
    }

    /** The drag is released with no target: the orb disappears, nothing spends. */
    public void cancelDrag() {
        this.draggedAspect = null;
    }

    private static boolean inDropBox(final int mouseX, final int mouseY, final int boxX) {
        final int dx = mouseX - boxX;
        final int dy = mouseY - DROP_Y;
        return dx >= -DROP_RADIUS && dy >= -DROP_RADIUS && dx < DROP_RADIUS && dy < DROP_RADIUS;
    }

    // ------------------------------------------------------------ click arms
    /**
     * The click arms in the original's gate order ({@code :623-724}). The
     * caller passes the panel-relative cursor, the shift state, and the
     * nano-time the combine cooldown is measured against; the model applies
     * every gate the original applies and returns the arm that fires.
     */
    public ClickAction clickAt(final int mouseX, final int mouseY, final boolean shift, final long nowNanos) {
        if (this.combineBlockedUntilNanos > nowNanos) {
            return ClickAction.NONE; // :625 — both button cooldowns gate the whole chain
        }
        if (inBox(mouseX, mouseY, COMBINE_X, COMBINE_Y, COMBINE_W, COMBINE_H)) {
            // :630 — the button arm needs the cooldown clear AND both slots filled.
            if (this.combineBlockedUntilNanos <= nowNanos && this.select1 != null && this.select2 != null) {
                this.combineBlockedUntilNanos = nowNanos + COMBINE_COOLDOWN_NANOS;
                return ClickAction.COMBINE;
            }
            return ClickAction.REFUSED;
        }
        if (canPageBack() && inBox(mouseX, mouseY, PAGE_PREV_X, PAGE_ARROW_Y, PAGE_ARROW_W, PAGE_ARROW_H)) {
            this.page--;
            return ClickAction.PAGE_PREV;
        }
        if (canPageForward() && inBox(mouseX, mouseY, PAGE_NEXT_X, PAGE_ARROW_Y, PAGE_ARROW_W, PAGE_ARROW_H)) {
            this.page++;
            return ClickAction.PAGE_NEXT;
        }
        if (this.select1 != null && inBox(mouseX, mouseY, CLEAR1_X, CLEAR_Y, CLEAR_SIZE, CLEAR_SIZE)) {
            this.select1 = null;
            return ClickAction.CLEAR_ONE;
        }
        if (this.select2 != null && inBox(mouseX, mouseY, CLEAR2_X, CLEAR_Y, CLEAR_SIZE, CLEAR_SIZE)) {
            this.select2 = null;
            return ClickAction.CLEAR_TWO;
        }
        if (this.note != null) {
            // :682 — the erase arm runs before the copy plate, and does not return.
            final ClickAction erase = eraseAt(mouseX, mouseY);
            if (erase == ClickAction.ERASE) {
                return erase;
            }
            if (canDuplicate() && inBox(mouseX, mouseY, DUPE_X, DUPE_Y, DUPE_SIZE, DUPE_SIZE)) {
                return ClickAction.DUPLICATE;
            }
        }
        if (shift && ResearchManager.isResearchComplete(this.knowledge, RESEARCH_RESEARCHER2)) {
            final String tag = aspectAt(mouseX, mouseY, true);
            if (tag != null && !isPrimal(tag)) {
                final List<String> components = componentsOf(tag);
                if (components.size() == 2
                        && this.knowledge.aspectAmount(components.get(0)) > 0
                        && this.knowledge.aspectAmount(components.get(1)) > 0) {
                    return ClickAction.SHIFT_COMBINE;
                }
            }
        }
        return ClickAction.NONE;
    }

    /** The erase arm ({@code :726-740}): a click on a player-placed (type 2) hex. */
    public ClickAction eraseAt(final int mouseX, final int mouseY) {
        if (this.note == null) {
            return ClickAction.NONE;
        }
        final Hex hex = hexAt(mouseX, mouseY);
        final HexEntry entry = this.note.hexEntries().get(hex.toString());
        return entry != null && entry.type() == TYPE_PLACED ? ClickAction.ERASE : ClickAction.NONE;
    }

    /** The hex an erase click lands on, for the payload the screen sends. */
    public Hex eraseTarget(final int mouseX, final int mouseY) {
        if (this.note == null) {
            return null;
        }
        final Hex hex = hexAt(mouseX, mouseY);
        final HexEntry entry = this.note.hexEntries().get(hex.toString());
        return entry != null && entry.type() == TYPE_PLACED ? hex : null;
    }

    public boolean combineReady(final long nowNanos) {
        return this.combineBlockedUntilNanos <= nowNanos;
    }

    /** The combine button's three draw states of {@code :246-252}. */
    public CombineButton combineButtonState(final long nowNanos) {
        if (this.select1 == null || this.select2 == null) {
            return CombineButton.HIDDEN;
        }
        return combineReady(nowNanos) ? CombineButton.READY : CombineButton.COOLDOWN;
    }

    public enum CombineButton { HIDDEN, READY, COOLDOWN }

    // ---------------------------------------------------------- the combine
    /**
     * The combine preview: the result of the pair and its cost. The cost is
     * the original's — one unit of each of the two ({@code
     * PacketAspectCombinationToServer.java:76-101}, one {@code addAspectPool
     * (-1)} per side) — and the result is the shipped combination table
     * ({@code AspectCombinations.combinationResultTag}). A pair with no result
     * still pays: the original spends both sides before it checks for a result.
     */
    public Combination combinePreview(final String first, final String second) {
        final Map<String, Integer> cost = new TreeMap<>();
        if (first != null) {
            cost.merge(first, 1, Integer::sum);
        }
        if (second != null) {
            cost.merge(second, 1, Integer::sum);
        }
        boolean affordable = true;
        for (final Map.Entry<String, Integer> entry : cost.entrySet()) {
            if (this.knowledge.aspectAmount(entry.getKey()) < entry.getValue()) {
                affordable = false;
            }
        }
        return new Combination(AspectCombinations.combinationResultTag(first, second), cost, affordable);
    }

    /** The shift arm's pair: the two components of the pool aspect under the cursor. */
    public Combination shiftCombinePreview(final String tag) {
        final List<String> components = componentsOf(tag);
        if (components.size() != 2) {
            return new Combination(null, Map.of(), false);
        }
        return combinePreview(components.get(0), components.get(1));
    }

    // ----------------------------------------------------------- the copy arm
    /**
     * The copy plate's gate ({@code :104,254,683}): a note, RESEARCHDUPE and a
     * completed grid.
     */
    public boolean canDuplicate() {
        return noteComplete() && ResearchManager.isResearchComplete(this.knowledge, RESEARCH_DUPE);
    }

    /**
     * The copy-cost plate: the entry's tags with the note's copy count added to
     * each ({@code :115-119}), in the entry's tag order. Empty when the graph
     * does not know the note's key — the same arm that refuses the copy.
     */
    public Map<String, Integer> copyCosts(final ResearchEntryData entry) {
        // :123 — the plate walks the tags in getAspectsSorted() order (by tag).
        final Map<String, Integer> costs = new TreeMap<>();
        if (this.note == null || entry == null || entry.tags() == null) {
            return costs;
        }
        for (final ResearchEntryData.AspectTag tag : entry.tags()) {
            costs.put(tag.aspect(), tag.amount() + this.note.copies());
        }
        return costs;
    }

    /** The pool preview arm ({@code :331-350}): RESEARCHER1 and a non-primal tag. */
    public boolean showsComponentPreview(final String tag) {
        return tag != null
                && ResearchManager.isResearchComplete(this.knowledge, RESEARCH_RESEARCHER1)
                && !isPrimal(tag);
    }

    /**
     * The out-of-ink tooltip block ({@code :211-227}): a note in the slot and
     * no ink in the scribing tool (no tool, or the tool's damage at maximum).
     */
    public boolean showsNoInkTooltip() {
        return this.note != null && (!this.scribePresent || this.inkRemaining <= 0);
    }

    /** The original's ink gate reads the scribe slot; the screen draws this text. */
    public static final String NO_INK_LINE_ONE = "tile.researchtable.noink.0";
    public static final String NO_INK_LINE_TWO = "tile.researchtable.noink.1";
    /** The copy plate's header ({@code :111}). */
    public static final String COPY_HEADER_KEY = "tc.research.copy";

    /** The shipped definition data, keyed by tag (the original's {@code AspectCache}). */
    private static final Map<String, AspectDefinitions.Definition> DEFINITIONS = definitions();

    private static Map<String, AspectDefinitions.Definition> definitions() {
        final Map<String, AspectDefinitions.Definition> byTag = new LinkedHashMap<>();
        for (final AspectDefinitions.Definition definition : AspectDefinitions.all()) {
            byTag.put(definition.tag(), definition);
        }
        return Map.copyOf(byTag);
    }

    /** The definition behind a tag, or null when the shipped data does not know it. */
    private static AspectDefinitions.Definition definition(final String tag) {
        return tag == null ? null : DEFINITIONS.get(tag);
    }

    /**
     * The aspect's display colour, ARGB: {@code AspectDefinitions.color}, the
     * same colour the original tints its icon with
     * ({@code GuiResearchTable.java:297-299} passes the aspect's colour to the
     * tessellator). Black for a tag the shipped data does not know.
     */
    public static int aspectColourRgb(final String tag) {
        final AspectDefinitions.Definition definition = definition(tag);
        return definition == null ? 0xFF000000 : 0xFF000000 | (definition.color() & 0x00FFFFFF);
    }

    /** Original {@code Aspect.getComponents()} as tags, public for the window. */
    public List<String> componentTagsOf(final String tag) {
        return componentsOf(tag);
    }

    /** Original {@code Aspect.isPrimal()} on the same shipped definition data. */
    private boolean isPrimal(final String tag) {
        final AspectDefinitions.Definition definition = definition(tag);
        return definition != null && definition.primal();
    }

    /** Original {@code Aspect.getComponents()}: the two declared component tags. */
    private List<String> componentsOf(final String tag) {
        final AspectDefinitions.Definition definition = definition(tag);
        return definition == null || definition.components() == null
                ? List.of() : definition.components();
    }

    private static boolean inBox(final int mouseX, final int mouseY, final int x, final int y,
                                 final int width, final int height) {
        final int dx = mouseX - x;
        final int dy = mouseY - y;
        return dx >= 0 && dy >= 0 && dx < width && dy < height;
    }
}
