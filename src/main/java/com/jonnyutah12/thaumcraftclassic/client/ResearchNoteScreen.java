package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.ResearchNoteMenu;
import com.jonnyutah12.thaumcraftclassic.network.ClientNetworkData;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPlaceToServerPayload;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.Chain;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.Cell;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.Request;
import com.jonnyutah12.thaumcraftclassic.research.NoteGridView.TrayCell;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The research-note screen (TC-M3-04-s-iii, NOTE-SCREEN-2): the hex grid, the
 * scribe/note slot panels, the RESEARCHDUPE copy plate and its cost panel, the
 * aspect tray and the out-of-ink lines of {@code GuiResearchTable}, drawn as
 * code against the menu contract NOTE-SCREEN-1 shipped.
 *
 * <p>Every coordinate on this panel is the original's, and the arithmetic that
 * turns a cursor position into a cell lives in {@link NoteGridView} — so does
 * the geometry of the sheet, the plate, the cost panel, the tray and the ink
 * lines. {@code ResearchNoteScreenGridParityTest} pins those constants against
 * the decompiled {@code GuiResearchTable.java} text; the game probes drive the
 * model against real notes. This class only draws and sends.</p>
 *
 * <h2>What a click does</h2>
 * <ul>
 *   <li>A tray cell with pool in it arms that aspect (the original's drag
 *       pickup, {@code :136-143}).</li>
 *   <li>A blank grid cell with an aspect armed writes it: one
 *       {@code thaumcraft:aspect_place_to_server} payload with (q, r) and the
 *       tag, the original's drop arm ({@code :147-172}). The server owns the
 *       rest — ink, the pool, the chain, completion — through
 *       {@code ResearchNoteMenu.placeAspect}.</li>
 *   <li>A typed cell erases: the same payload with no tag
 *       ({@code checkClickedHex}, {@code :726-740}).</li>
 *   <li>The copy plate, while the menu's duplicate gate is lit, sends the
 *       original's button 5 ({@code :683-690}); the server spends the copies.</li>
 * </ul>
 *
 * <p><b>This screen never claims a completion.</b> There is no completion path
 * here at all: the client sends writes and the copy button, and only
 * {@code ResearchManager.completeResearch} (driven by the server's write arm
 * through {@code NotePuzzle.checkCompletion}) completes research. The two
 * interaction shapes the original drew with textures and drags — the hex
 * silhouette and drag-and-drop — are the port's code-drawn hexagon and
 * click-to-arm/click-to-write. BD-20 records that, with the rest.</p>
 *
 * <p>No original texture is referenced (ARR, D-004) and no legacy GL is used:
 * the parchment, the hex outlines, the plate and the tray chips are fills and
 * outlines, in the original's colours and alphas.</p>
 */
@OnlyIn(Dist.CLIENT)
public class ResearchNoteScreen extends AbstractContainerScreen<ResearchNoteMenu> {

    /** The lang key of the note menu (the registered id's key, shipped). */
    static final String TITLE_KEY = "container.thaumcraft.gui_research_note";

    /** The original's two out-of-ink lines (GuiResearchTable.java:209-215). */
    static final String NO_INK_LINE_0 = "tile.researchtable.noink.0";
    static final String NO_INK_LINE_1 = "tile.researchtable.noink.1";
    /** The original's copy-cost header (GuiResearchTable.java:327). */
    static final String COPY_HEADER = "tc.research.copy";

    /** The parchment tone of the panels and the note sheet. */
    static final int PARCHMENT = 0xFFB5A179;
    /** The panel frame, drawn as a 1 px outline. */
    static final int PANEL_FRAME = 0xFF555555;
    /** The tray cell's dark well behind each aspect chip. */
    static final int TRAY_WELL = 0xFF3A3A3A;
    /** The ink tooltip's box, in the vanilla tooltip colours. */
    static final int TOOLTIP_FILL = 0xF0101010;
    static final int TOOLTIP_FRAME = 0x505050FF;
    /** The cost/amount text colour: the original's white tag text. */
    static final int TAG_TEXT = 0xFFFFFF;

    /** The tray page (the original's {@code page}, {@code :264-268}). */
    private int page;
    /** The aspect armed for the next write (the original's {@code draggedAspect}). */
    private String armed;

    public ResearchNoteScreen(final ResearchNoteMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        // The original's window: xSize = ySize = 255 (GuiResearchTable.java:70-71).
        this.imageWidth = NoteGridView.WINDOW_SPAN;
        this.imageHeight = NoteGridView.WINDOW_SPAN;
        this.titleLabelX = 10;
        this.titleLabelY = 6;
    }

    // ------------------------------------------------------------------ draw

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick,
                            final int mouseX, final int mouseY) {
        final int left = this.leftPos;
        final int top = this.topPos;
        // The two parchment panels, at the original's sizes (GuiResearchTable.java:242-243).
        panel(graphics, left, top, NoteGridView.MAIN_PANEL_W, NoteGridView.MAIN_PANEL_H);
        panel(graphics, left + NoteGridView.FOOT_PANEL_X, top + NoteGridView.FOOT_PANEL_Y,
                NoteGridView.FOOT_PANEL_W, NoteGridView.FOOT_PANEL_H);

        final ResearchNoteData note = note();
        if (note != null) {
            // The note's sheet: 150×150 at (94, 8) (:456).
            panel(graphics, left + NoteGridView.SHEET_X, top + NoteGridView.SHEET_Y,
                    NoteGridView.SHEET_SPAN, NoteGridView.SHEET_SPAN);
        }

        // Page arrows, gated on the page like the original's (:244-250).
        final int lastPage = Math.max(0, NoteGridView.lastPage(discoveredTags().size()));
        if (this.page < lastPage) {
            graphics.fill(left + NoteGridView.PAGE_NEXT_X, top + NoteGridView.PAGE_ARROW_Y,
                    left + NoteGridView.PAGE_NEXT_X + NoteGridView.PAGE_ARROW_W,
                    top + NoteGridView.PAGE_ARROW_Y + NoteGridView.PAGE_ARROW_H, PANEL_FRAME);
        }
        if (this.page > 0) {
            graphics.fill(left + NoteGridView.PAGE_PREV_X, top + NoteGridView.PAGE_ARROW_Y,
                    left + NoteGridView.PAGE_PREV_X + NoteGridView.PAGE_ARROW_W,
                    top + NoteGridView.PAGE_ARROW_Y + NoteGridView.PAGE_ARROW_H, PANEL_FRAME);
        }

        // The copy plate and the cost panel: the menu's gate is RESEARCHDUPE +
        // a complete note (:104-107, :254-256).
        if (this.menu.canDuplicate()) {
            graphics.fill(left + NoteGridView.PLATE_X, top + NoteGridView.PLATE_Y,
                    left + NoteGridView.PLATE_X + NoteGridView.PLATE_SPAN,
                    top + NoteGridView.PLATE_Y + NoteGridView.PLATE_SPAN, PARCHMENT);
            graphics.renderOutline(left + NoteGridView.PLATE_X, top + NoteGridView.PLATE_Y,
                    NoteGridView.PLATE_SPAN, NoteGridView.PLATE_SPAN, PANEL_FRAME);
            graphics.fill(left + NoteGridView.COST_PANEL_X, top + NoteGridView.COST_PANEL_Y,
                    left + NoteGridView.COST_PANEL_X + NoteGridView.COST_PANEL_W,
                    top + NoteGridView.COST_PANEL_Y + NoteGridView.COST_PANEL_H, PARCHMENT);
            graphics.renderOutline(left + NoteGridView.COST_PANEL_X, top + NoteGridView.COST_PANEL_Y,
                    NoteGridView.COST_PANEL_W, NoteGridView.COST_PANEL_H, PANEL_FRAME);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        // The menu title, from the open-screen packet (key TITLE_KEY). The
        // original draws no inventory label on this panel.
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // The original's post-item pass (its drawBoard runs after the slots):
        // the grid, the tray, the cost tags and the ink lines draw over the
        // panel, and the vanilla item tooltips stay on top of all of it.
        final int localX = mouseX - this.leftPos;
        final int localY = mouseY - this.topPos;
        final ResearchNoteData note = note();
        if (note != null) {
            final Chain chain = NoteGridView.chain(note, this::discovered);
            drawLinks(graphics, chain);
            for (Cell cell : NoteGridView.cells(note, this::discovered, chain, hoveredKey(note, localX, localY))) {
                drawCell(graphics, cell, note);
            }
        }
        drawTray(graphics);
        drawCostTags(graphics);
        drawInkWarning(graphics);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    /** The key of the cell under the cursor, for the original's highlight quad. */
    private String hoveredKey(final ResearchNoteData note, final int localX, final int localY) {
        final Cell hovered = NoteGridView.cellAt(note, localX, localY, this::discovered, Chain.empty());
        return hovered == null ? null : hovered.key();
    }

    private void panel(final GuiGraphics graphics, final int x, final int y, final int width, final int height) {
        graphics.fill(x, y, x + width, y + height, PARCHMENT);
        graphics.renderOutline(x, y, width, height, PANEL_FRAME);
    }

    /** The chain lines, in the original's (0, 0.6, 0.8) (:443), routed L-style. */
    private void drawLinks(final GuiGraphics graphics, final Chain chain) {
        for (NoteGridView.Link link : chain.links()) {
            final int x1 = this.leftPos + link.x1();
            final int y1 = this.topPos + link.y1();
            final int x2 = this.leftPos + link.x2();
            final int y2 = this.topPos + link.y2();
            final int midY = (y1 + y2) / 2;
            graphics.vLine(x1, Math.min(y1, midY), Math.max(y1, midY), NoteGridView.LINK_COLOUR);
            graphics.hLine(Math.min(x1, x2), Math.max(x1, x2), midY, NoteGridView.LINK_COLOUR);
            graphics.vLine(x2, Math.min(y2, midY), Math.max(y2, midY), NoteGridView.LINK_COLOUR);
        }
    }

    /**
     * One grid cell, in the original's two-pass order ({@code :505-566}): the
     * hex outline / orb pass first, then the tag pass (unknown overlay, bright
     * chain cells, faded typed cells).
     */
    private void drawCell(final GuiGraphics graphics, final Cell cell, final ResearchNoteData note) {
        final int cx = this.leftPos + cell.centerX();
        final int cy = this.topPos + cell.centerY();
        if (!note.complete()) {
            if (!cell.required()) {
                if (cell.hovered()) {
                    hexRim(graphics, cx, cy, NoteGridView.HEX_HIGHLIGHT_COLOUR);
                }
                hexRim(graphics, cx, cy, NoteGridView.HEX_OUTLINE_COLOUR);
            } else {
                // The original's particle orb behind a required tag (:533, :860-880):
                // a code-drawn disc in the note's own colour, 0.4 alpha.
                disc(graphics, cx, cy, 8, 0x66000000 | (note.color() & 0xFFFFFF));
            }
        }
        if (cell.unknown()) {
            fillHex(graphics, cx, cy, NoteGridView.UNKNOWN_COLOUR);
        } else if (cell.aspectTag() != null && (cell.required() || cell.highlighted() || cell.typed())) {
            fillHex(graphics, cx, cy, cell.fillColour());
            hexRim(graphics, cx, cy, 0xC0FFFFFF);
        }
    }

    /** A code-drawn hexagon: the original's {@code hex1.png} silhouette, filled. */
    private void fillHex(final GuiGraphics graphics, final int centreX, final int centreY, final int colour) {
        final int span = NoteGridView.CELL_SPAN;
        final int[] widths = NoteGridView.hexWidths(span);
        final int top = centreY - span / 2;
        final int left = centreX - span / 2;
        for (int row = 0; row < span; row++) {
            final int x0 = left + NoteGridView.hexLeft(span, widths[row]);
            graphics.hLine(x0, x0 + widths[row] - 1, top + row, colour);
        }
    }

    /** The same silhouette, one pixel wide: the outline the texture carried. */
    private void hexRim(final GuiGraphics graphics, final int centreX, final int centreY, final int colour) {
        final int span = NoteGridView.CELL_SPAN;
        final int[] widths = NoteGridView.hexWidths(span);
        final int top = centreY - span / 2;
        final int left = centreX - span / 2;
        for (int row = 0; row < span; row++) {
            final int width = widths[row];
            final int x0 = left + NoteGridView.hexLeft(span, width);
            if (row == 0 || row == span - 1) {
                graphics.hLine(x0, x0 + width - 1, top + row, colour);
            } else {
                graphics.hLine(x0, x0, top + row, colour);
                graphics.hLine(x0 + width - 1, x0 + width - 1, top + row, colour);
            }
        }
    }

    private void disc(final GuiGraphics graphics, final int centreX, final int centreY, final int radius,
                      final int colour) {
        for (int row = -radius; row <= radius; row++) {
            final int half = (int) Math.round(Math.sqrt((double) radius * radius - (double) row * row));
            graphics.hLine(centreX - half, centreX + half, centreY + row, colour);
        }
    }

    /** The aspect tray: 25 chips of the discovered aspects, 16 px apart (:264-291). */
    private void drawTray(final GuiGraphics graphics) {
        for (TrayCell cell : tray()) {
            final int x = this.leftPos + cell.left();
            final int y = this.topPos + cell.top();
            graphics.fill(x, y, x + cell.span(), y + cell.span(), TRAY_WELL);
            fillHex(graphics, x + cell.span() / 2, y + cell.span() / 2,
                    (cell.faded() ? NoteGridView.FADED_ALPHA : NoteGridView.FULL_ALPHA) << 24
                            | (cell.colour() & 0xFFFFFF));
            if (cell.selected()) {
                graphics.renderOutline(x, y, cell.span(), cell.span(), 0xFFFFFFFF);
            }
            if (cell.amount() > 0) {
                graphics.drawString(this.font, Integer.toString(cell.amount()),
                        x + cell.span() - this.font.width(Integer.toString(cell.amount())) - 1,
                        y + cell.span() - 5, TAG_TEXT);
            }
        }
    }

    /** The copy-cost tags: (100 + 48 + n*16, 21), the header at (100, 12) (:122-129). */
    private void drawCostTags(final GuiGraphics graphics) {
        if (!this.menu.canDuplicate()) {
            return;
        }
        graphics.drawString(this.font, Component.translatable(COPY_HEADER),
                this.leftPos + NoteGridView.COST_PANEL_X, this.topPos + NoteGridView.COST_HEADER_Y, 0xFFFFFF);
        int index = 0;
        for (Map.Entry<String, Integer> cost : this.menu.copyCosts().entrySet()) {
            final int x = this.leftPos + NoteGridView.COST_TAG_X + index * NoteGridView.COST_TAG_STEP;
            final int y = this.topPos + NoteGridView.COST_PANEL_Y;
            fillHex(graphics, x + 8, y + 8,
                    NoteGridView.AspectColour.of(cost.getKey()) & 0xFFFFFF | 0xFF000000);
            final String text = Integer.toString(cost.getValue());
            graphics.drawString(this.font, text, x + 16 - this.font.width(text) - 1, y + 11, TAG_TEXT);
            index++;
        }
    }

    /**
     * The out-of-ink lines, centred on x 157 at y 84, gated exactly as the
     * original gates them: a note in the slot and no ink on the scribe tool
     * ({@code :209-226}).
     */
    private void drawInkWarning(final GuiGraphics graphics) {
        if (!this.menu.notePresent() || this.menu.hasInk()) {
            return;
        }
        final Component line0 = Component.translatable(NO_INK_LINE_0);
        final Component line1 = Component.translatable(NO_INK_LINE_1);
        final int widest = Math.max(this.font.width(line0), this.font.width(line1));
        final int x = this.leftPos + NoteGridView.INK_TIP_X - widest / 2;
        final int y = this.topPos + NoteGridView.INK_TIP_Y;
        graphics.fill(x - 2, y - 2, x + widest + 2, y + 22, TOOLTIP_FILL);
        graphics.renderOutline(x - 2, y - 2, widest + 4, 24, TOOLTIP_FRAME);
        graphics.drawString(this.font, line0, x, y, TAG_TEXT);
        graphics.drawString(this.font, line1, x, y + 10, TAG_TEXT);
    }

    // ---------------------------------------------------------------- actions

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        final int localX = (int) (mouseX - this.leftPos);
        final int localY = (int) (mouseY - this.topPos);

        // Page arrows (:244-250) — the tray's own paging, not a grid arm.
        if (NoteGridView.inPageNext(localX, localY)) {
            final int lastPage = Math.max(0, NoteGridView.lastPage(discoveredTags().size()));
            if (this.page < lastPage) {
                this.page++;
                return true;
            }
            return false;
        }
        if (NoteGridView.inPagePrev(localX, localY) && this.page > 0) {
            this.page--;
            return true;
        }

        final ResearchNoteData note = note();
        if (note == null) {
            return false;
        }

        // The copy plate: the original sends button 5 (:683-690); the server's
        // duplicate arm spends the copies. The gate is the menu's.
        if (NoteGridView.inPlate(localX, localY) && this.menu.canDuplicate() && this.minecraft != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                    ResearchNoteMenu.BUTTON_DUPLICATE);
            return true;
        }

        // The tray: arm the aspect (the original's drag pickup, :136-143).
        if (NoteGridView.inTray(localX, localY)) {
            final TrayCell cell = NoteGridView.trayCellAt(tray(), localX, localY, true);
            if (cell != null) {
                this.armed = cell.tag();
                return true;
            }
            return false;
        }

        // The grid: a typed cell erases (checkClickedHex, :726-740), a blank
        // cell with an armed aspect writes (:147-172). The server decides ink,
        // the pool, the chain and completion; nothing here claims a completion.
        final Request request = NoteGridView.requestAt(note, localX, localY, this.armed, this::discovered);
        if (request.erases()) {
            sendPlace(request.q(), request.r(), Optional.empty());
            this.armed = null;
            return true;
        }
        if (request.writes()) {
            sendPlace(request.q(), request.r(), Optional.ofNullable(request.aspectTag()));
            this.armed = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        final int lastPage = Math.max(0, NoteGridView.lastPage(discoveredTags().size()));
        if (scrollY < 0 && this.page < lastPage) {
            this.page++;
            return true;
        }
        if (scrollY > 0 && this.page > 0) {
            this.page--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** The write arm's payload: {@code thaumcraft:aspect_place_to_server}. */
    private void sendPlace(final int q, final int r, final Optional<String> aspectTag) {
        PacketDistributor.sendToServer(new AspectPlaceToServerPayload(BlockPos.ZERO, aspectTag, q, r));
    }

    // ------------------------------------------------------------------ state

    /** The note the container is showing (the original's {@code ResearchManager.getData}). */
    private ResearchNoteData note() {
        return ResearchNoteMenu.noteOf(this.menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem());
    }

    /**
     * The client's mirror of the player's discovered aspects and their pool
     * amounts — the same view the Thaumonomicon reads, and the same amount the
     * server's write arm gates a write against
     * ({@code PlayerKnowledgeData.aspectAmount}).
     */
    private Map<String, Integer> discoveredAspects() {
        return ClientNetworkData.discoveredAspects();
    }

    private boolean discovered(final String tag) {
        return tag != null && discoveredAspects().containsKey(tag);
    }

    private int poolAmount(final String tag) {
        return discoveredAspects().getOrDefault(tag, 0);
    }

    /** The tray's source list: the discovered aspects, sorted (the original's {@code getAspectsSorted}). */
    private List<String> discoveredTags() {
        final List<String> tags = new ArrayList<>(discoveredAspects().keySet());
        tags.sort(String::compareTo);
        return tags;
    }

    private List<TrayCell> tray() {
        return NoteGridView.tray(discoveredTags(), this.page, this::poolAmount, this.armed);
    }
}
