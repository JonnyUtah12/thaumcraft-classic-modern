package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.ResearchNoteMenu;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectCombinationToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPlaceToServerPayload;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.NoteScreenModel;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The research-note window: the hex grid, the aspect pool, the drag, the
 * combine and the copy plate (NOTE-SCREEN-2, t_32a29c93).
 *
 * <p>It is the drawing half of {@link NoteScreenModel}: every layout number,
 * gate and sound on this window comes from that model, whose javadoc cites each
 * of them to the line of
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchTable.java}
 * it is ported from. This class is client-only and names no server behaviour:
 * a complete grid is a <em>claim</em> the server validates (the note menu's
 * write arm), the client never decides completion.</p>
 *
 * <p><b>Original-sheet deviation (BD).</b> The 1.7.10 window draws the
 * research-table sheet, the hex outlines, the orbs and the arrows from
 * {@code Guis/ResearchTable.png}, an <b>All Rights Reserved</b> asset that is
 * never committed (docs/porting-decisions.md D-004, the BD-17 / BD-22
 * precedent). This window draws its own flat panel — a generated texture
 * (<code>assets/thaumcraft/textures/gui/research_note.png</code>, MIT), tinted
 * hex fills and vanilla slot sprites. The geometry, alpha and colour channels
 * are the original's, so the layout is the original's; the pixels are not.
 * Recorded in docs/content-mapping.csv as a BD row, not silently.</p>
 *
 * <p>The original's research-table {@code bonusAspects} pool has no port-side
 * counterpart (the tile is not ported): every bonus-pool term the original
 * reads is the empty pool here, the same BD-26 deviation the note menu's
 * write arm documents.</p>
 */
public final class ResearchNoteScreen extends AbstractContainerScreen<ResearchNoteMenu> {

    /**
     * The panel's palette. The original blits {@code Guis/ResearchTable.png},
     * an All Rights Reserved sheet that is never committed (D-004); this window
     * draws its own flat panel from code — the fills below carry the original's
     * layout, and the colour channels follow its parchment grey.
     */
    private static final int PANEL_FIELD = 0xFF38322A;
    private static final int PANEL_BEVEL_LIGHT = 0xFF6E5E44;
    private static final int PANEL_BEVEL_DARK = 0xFF18130E;
    private static final int POOL_FIELD_EDGE = 0xFF231D16;
    private static final int GRID_FIELD_EDGE = 0xFF6E5E44;
    private static final int MARK_CLEAR = 0xFFC83C32;
    private static final int MARK_ARROW = 0xFFD2BE8C;

    /**
     * The original's layout colours: the aspect plate is a black quad
     * ({@code GuiResearchTable.java:533,553}), the connection line is drawn in
     * the aspect's blend colour, the combine button in the panel's grey, and
     * the "no ink" tooltip in vanilla's grey tooltip block.
     */
    private static final int HEX_PLATE = 0xFF14100C;
    private static final int HEX_PLATE_EDGE = 0xFF2A2219;
    private static final int POOL_PLATE = 0xFF1B1610;
    private static final int BUTTON_FILL = 0xFF3B3327;
    private static final int BUTTON_FILL_COOLDOWN = 0xFF231F19;
    private static final int BUTTON_EDGE = 0xFF6B5E45;
    private static final int DRAG_GHOST = 0x80FFFFFF;
    private static final int TEXT_LIGHT = 0xFFF0E6CF;
    private static final int TEXT_GOLD = 0xFFFFD700;
    private static final int TOOLTIP_BACKGROUND = 0xF0100002;
    private static final int TOOLTIP_FRAME_TOP = 0x50000000;
    private static final int TOOLTIP_FRAME_SIDES = 0x50000000;

    private final NoteScreenModel model = new NoteScreenModel();

    public ResearchNoteScreen(final ResearchNoteMenu menu, final Inventory playerInventory,
                              final Component title) {
        super(menu, playerInventory, title);
        // The panel is 255x255 (GuiResearchTable.java:70-71) and the menu's slot
        // coordinates are absolute against that panel.
        this.imageWidth = NoteScreenModel.PANEL_WIDTH;
        this.imageHeight = NoteScreenModel.PANEL_HEIGHT;
        this.inventoryLabelY = NoteScreenModel.PANEL_HEIGHT - 12;
        this.titleLabelY = 4;
    }

    // ------------------------------------------------------------------ frame

    @Override
    protected void init() {
        super.init();
        this.model.setPage(0);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderFrame(graphics, mouseX, mouseY);
    }

    /**
     * The window's frame: background, panel, slots, grid, pool, buttons, the
     * dragged orb and the tooltip previews. Split out so a headless caller can
     * drive the same code path with a recorder (see the note-screen probes).
     */
    public void renderFrame(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        this.renderBackground(graphics, mouseX, mouseY, 0.0F);
        refreshModel();
        super.render(graphics, mouseX, mouseY, 0.0F);
        drawConnections(graphics);
        drawGrid(graphics, mouseX, mouseY);
        drawPool(graphics, mouseX, mouseY);
        drawSelectSlots(graphics, mouseX, mouseY);
        drawButtons(graphics, mouseX, mouseY);
        drawDraggedAspect(graphics, mouseX, mouseY);
        drawPreviews(graphics, mouseX, mouseY);
    }

    /**
     * The window reads the state the menu publishes: the note in the note slot,
     * the synced knowledge attachment, the six {@code ContainerData} values
     * (the menu's ink mirror) and the ink in the scribing slot.
     */
    private void refreshModel() {
        final ResearchNoteMenu menu = this.getMenu();
        final ResearchNoteData note = menu.notePresent()
                ? ResearchNoteMenu.noteOf(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem())
                : null;
        this.model.setNote(note);
        this.model.setKnowledge(menu.getKnowledge());
        this.model.setInk(!menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).getItem().isEmpty(),
                menu.inkRemainingValue());
        // The original drops a selection whose aspect has left the pool, every
        // frame (GuiResearchTable.java:292-302).
        this.model.clearUnpayableSelections();
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX, final int mouseY) {
        // The flat panel (BD row): the field, a two-tone bevel, the pool field
        // and the grid field ring — every rect at the original's coordinates.
        final int x = this.leftPos;
        final int y = this.topPos;
        // BD: the original blits its panel from the rights-reserved
        // researchNote.png (:236-237, :456) and no sheet is committed here. The
        // panel's GEOMETRY is port-side data, not art, so the port fills the
        // original's OWN two-fill shape: the 255x167 field (:236) and the
        // 184x88 strip hanging below it at x=40 (:237) — a square 255x255 panel
        // would cover the two inventory rows the strip leaves visible.
        graphics.fill(x, y, x + NoteScreenModel.PANEL_WIDTH,
                y + NoteScreenModel.PANEL_FIELD_HEIGHT, PANEL_FIELD);
        graphics.fill(x + NoteScreenModel.PANEL_STRIP_X, y + NoteScreenModel.PANEL_STRIP_Y,
                x + NoteScreenModel.PANEL_STRIP_X + NoteScreenModel.PANEL_STRIP_WIDTH,
                y + NoteScreenModel.PANEL_STRIP_Y + NoteScreenModel.PANEL_STRIP_HEIGHT, PANEL_FIELD);
        drawRectRing(graphics, x, y, NoteScreenModel.PANEL_WIDTH, NoteScreenModel.PANEL_HEIGHT, PANEL_BEVEL_DARK);
        drawRectRing(graphics, x + 1, y + 1,
                NoteScreenModel.PANEL_WIDTH - 2, NoteScreenModel.PANEL_HEIGHT - 2, PANEL_BEVEL_LIGHT);
        drawRectRing(graphics, x + NoteScreenModel.POOL_X - 1, y + NoteScreenModel.POOL_Y - 1,
                NoteScreenModel.POOL_PRESS_SIZE + 2, NoteScreenModel.POOL_PRESS_SIZE + 2, POOL_FIELD_EDGE);
        drawFieldRing(graphics, x + NoteScreenModel.GRID_CENTRE_X, y + NoteScreenModel.GRID_CENTRE_Y, 66);
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        // The original draws no label on the panel; the note's research key is
        // the copy plate's business, drawn in drawButtons.
    }

    // ------------------------------------------------------------------- grid

    private void drawGrid(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        for (final NoteScreenModel.HexCell cell : this.model.hexCells(mouseX, mouseY)) {
            final int centreX = this.leftPos + cell.centreX();
            final int centreY = this.topPos + cell.centreY();
            if (cell.outline()) {
                // The blank hex: the original's outline sprite at alpha 0.25
                // (:524-525, :395), here a 1px step ring (the card's "GL line
                // strips -> 1px step fills").
                drawHexRing(graphics, centreX, centreY,
                        withAlpha(HEX_PLATE_EDGE, cell.outlineAlpha()));
            }
            if (cell.orb()) {
                // The required hex's orb (:533, :860-867): NOT the aspect's
                // colour — the original tints the orb with its own animated
                // white/blue shimmer and draws the aspect's tag on top of it.
                drawHexFill(graphics, centreX, centreY,
                        NoteScreenModel.orbColour(clientTicks(), cell.plateX(), cell.plateY()), 1.0F);
            }
            if (cell.tag() != null && !cell.veiled() && cell.tagAlpha() > 0.0F) {
                drawHexFill(graphics, centreX, centreY, NoteScreenModel.aspectColourRgb(cell.tag()), cell.tagAlpha());
            }
            if (cell.tag() != null) {
                // The plate under the icon (:533,553,558) and the icon itself.
                drawHexFill(graphics, this.leftPos + cell.plateX(), this.topPos + cell.plateY(),
                        HEX_PLATE, cell.veiled() ? NoteScreenModel.VEIL_ALPHA : 0.75F);
                if (!cell.veiled()) {
                    drawAspectIcon(graphics, cell.tag(), this.leftPos + cell.plateX() + 4,
                            this.topPos + cell.plateY() + 4);
                }
            }
            if (cell.hovered()) {
                // The hover highlight: the original's hover quad (:530-542).
                drawHexRing(graphics, centreX, centreY, 0xFFFFFFFF);
            }
        }
    }

    /**
     * The connection sweep ({@code :505-508,570-597}): the port's
     * {@code NotePuzzle.checkConnections} relation, drawn as step fills between
     * the linked hex centres. The model gates the pair (a completed note draws
     * none, {@code :214-220}) and carries the original's colour and animated
     * alpha ({@code :433-450}); the window only maps hexes to pixels.
     */
    private void drawConnections(final GuiGraphics graphics) {
        final List<Hex[]> links = this.model.connections();
        for (final Hex[] link : links) {
            final int fromX = this.leftPos + NoteScreenModel.GRID_CENTRE_X
                    + (int) Math.round(link[0].toPixel(NoteScreenModel.HEX_SIZE).x());
            final int fromY = this.topPos + NoteScreenModel.GRID_CENTRE_Y
                    + (int) Math.round(link[0].toPixel(NoteScreenModel.HEX_SIZE).y());
            final int toX = this.leftPos + NoteScreenModel.GRID_CENTRE_X
                    + (int) Math.round(link[1].toPixel(NoteScreenModel.HEX_SIZE).x());
            final int toY = this.topPos + NoteScreenModel.GRID_CENTRE_Y
                    + (int) Math.round(link[1].toPixel(NoteScreenModel.HEX_SIZE).y());
            // :433-450 — the strip is ONE fixed colour (0.0, 0.6, 0.8) at a 3px
            // width, with its alpha animated off the client player's tick
            // counter. The port draws the original's colour and animated alpha as
            // a step fill (the card's "GL line strips -> 1px step fills"); the
            // aspect's own colour never enters the sweep.
            drawLine(graphics, fromX, fromY, toX, toY, NoteScreenModel.CONNECTION_LINE_COLOUR,
                    NoteScreenModel.connectionAlpha(clientTicks(), fromX - this.leftPos));
        }
    }

    // ------------------------------------------------------------------- pool

    private void drawPool(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        final List<NoteScreenModel.PoolCell> cells = this.model.poolCells();
        for (final NoteScreenModel.PoolCell cell : cells) {
            final int x = this.leftPos + cell.x();
            final int y = this.topPos + cell.y();
            graphics.fill(x, y, x + NoteScreenModel.POOL_CELL, y + NoteScreenModel.POOL_CELL,
                    withAlpha(POOL_PLATE, cell.faded() ? NoteScreenModel.FADED_ALPHA : 0.85F));
            drawAspectIcon(graphics, cell.tag(), x, y);
            if (cell.faded()) {
                // The faded 0-amount entry (:274) — the veil's pool twin.
                graphics.fill(x, y, x + NoteScreenModel.POOL_CELL, y + NoteScreenModel.POOL_CELL,
                        withAlpha(0xFF000000, NoteScreenModel.FADED_ALPHA));
            }
        }
        // The page arrows (:238-244) are drawn ONLY when their gate allows the
        // page: page > 0 for back, page < maxPage for forward. A refused arrow is
        // not drawn at all, so the arrows move as the page moves (:651,657).
        if (this.model.canPageBack()) {
            drawArrow(graphics, NoteScreenModel.PAGE_PREV_X, true, true);
        }
        if (this.model.canPageForward()) {
            drawArrow(graphics, NoteScreenModel.PAGE_NEXT_X, true, false);
        }
    }

    private void drawSelectSlots(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        drawSelectSlot(graphics, NoteScreenModel.SELECT1_X, NoteScreenModel.SELECT_Y,
                this.model.select1(), this.model.select1Amount());
        drawSelectSlot(graphics, NoteScreenModel.SELECT2_X, NoteScreenModel.SELECT_Y,
                this.model.select2(), this.model.select2Amount());
        // The clear arms (:229-237) only draw when the slot is filled (:661,670).
        if (this.model.select1() != null) {
            drawClearPlate(graphics, NoteScreenModel.CLEAR1_X);
        }
        if (this.model.select2() != null) {
            drawClearPlate(graphics, NoteScreenModel.CLEAR2_X);
        }
    }

    private void drawSelectSlot(final GuiGraphics graphics, final int x, final int y,
                                final String tag, final int amount) {
        final int px = this.leftPos + x;
        final int py = this.topPos + y;
        graphics.fill(px, py, px + 16, py + 16, withAlpha(POOL_PLATE, 0.85F));
        if (tag != null) {
            drawAspectIcon(graphics, tag, px, py);
        }
    }

    private void drawClearPlate(final GuiGraphics graphics, final int x) {
        final int px = this.leftPos + x;
        final int py = this.topPos + NoteScreenModel.CLEAR_Y;
        graphics.fill(px, py, px + NoteScreenModel.CLEAR_SIZE, py + NoteScreenModel.CLEAR_SIZE, POOL_PLATE);
        drawRectRing(graphics, px, py, NoteScreenModel.CLEAR_SIZE, NoteScreenModel.CLEAR_SIZE, BUTTON_EDGE);
        for (int step = 3; step < NoteScreenModel.CLEAR_SIZE - 3; step++) {
            // The clear mark: the original's red X quad (:662-672).
            graphics.fill(px + step, py + step - 3, px + step + 1, py + step - 2, MARK_CLEAR);
            graphics.fill(px + NoteScreenModel.CLEAR_SIZE - step - 1, py + step - 3,
                    px + NoteScreenModel.CLEAR_SIZE - step, py + step - 2, MARK_CLEAR);
        }
    }

    // ---------------------------------------------------------------- buttons

    private void drawButtons(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        // The combine button (:246-252): hidden, greyed on cooldown, bright.
        final NoteScreenModel.CombineButton state =
                this.model.combineButtonState(System.nanoTime());
        if (state != NoteScreenModel.CombineButton.HIDDEN) {
            final int x = this.leftPos + NoteScreenModel.COMBINE_X;
            final int y = this.topPos + NoteScreenModel.COMBINE_Y;
            graphics.fill(x, y, x + NoteScreenModel.COMBINE_W, y + NoteScreenModel.COMBINE_H,
                    state == NoteScreenModel.CombineButton.READY ? BUTTON_FILL : BUTTON_FILL_COOLDOWN);
            drawRectRing(graphics, x, y, NoteScreenModel.COMBINE_W, NoteScreenModel.COMBINE_H, BUTTON_EDGE);
            // The combine mark: the original's button sprite (:247), as a plus of
            // 1px fills in the button's centre.
            final int midX = x + NoteScreenModel.COMBINE_W / 2;
            final int midY = y + NoteScreenModel.COMBINE_H / 2;
            graphics.fill(midX - 5, midY - 1, midX + 5, midY + 1, TEXT_LIGHT);
            graphics.fill(midX - 1, midY - 5, midX + 1, midY + 5, TEXT_LIGHT);
        }
        // The copy plate (:104,254,683): a complete grid and RESEARCHDUPE.
        if (this.model.canDuplicate()) {
            final int x = this.leftPos + NoteScreenModel.DUPE_X;
            final int y = this.topPos + NoteScreenModel.DUPE_Y;
            graphics.fill(x, y, x + NoteScreenModel.DUPE_SIZE, y + NoteScreenModel.DUPE_SIZE, BUTTON_FILL);
            drawRectRing(graphics, x, y, NoteScreenModel.DUPE_SIZE, NoteScreenModel.DUPE_SIZE, BUTTON_EDGE);
            // The copy plate's mark: a second plate behind the first (:684).
            drawRectRing(graphics, x + 4, y + 4,
                    NoteScreenModel.DUPE_SIZE - 8, NoteScreenModel.DUPE_SIZE - 8, MARK_ARROW);
            drawRectRing(graphics, x + 8, y + 7,
                    NoteScreenModel.DUPE_SIZE - 8, NoteScreenModel.DUPE_SIZE - 8, PANEL_FIELD);
            if (isInBox(mouseX, mouseY, NoteScreenModel.DUPE_X, NoteScreenModel.DUPE_Y,
                    NoteScreenModel.DUPE_SIZE, NoteScreenModel.DUPE_SIZE)) {
                drawCopyCostPlate(graphics);
            }
        }
        // The out-of-ink tooltip block (:211-227): a note in the slot, no ink.
        if (this.model.showsNoInkTooltip()) {
            final int width = this.font.width("tile.researchtable.noink.1");
            final int x = this.leftPos + NoteScreenModel.NO_INK_TOOLTIP_X - width / 2;
            drawTooltipBlock(graphics, x, this.topPos + NoteScreenModel.NO_INK_TOOLTIP_Y,
                    List.of(Component.translatable(NoteScreenModel.NO_INK_LINE_ONE),
                            Component.translatable(NoteScreenModel.NO_INK_LINE_TWO)));
        }
    }

    /**
     * The copy-cost plate ({@code :114-129}): the target's research name at
     * (100, 12) and ONE 48x16 plate at (100, 21) ({@code :120}), with the cost
     * tags hanging to the RIGHT of the plate at plate-x + 48 + 16 per tag at the
     * plate's y ({@code :124}), in the original's sorted-tag order. The costs
     * come from the menu's own {@code copyCosts()} — the same arm that refuses
     * the copy — and only the RESEARCHDUPE arm opens this plate at all
     * ({@code :683-691}).
     */
    private void drawCopyCostPlate(final GuiGraphics graphics) {
        final Map<String, Integer> costs = this.getMenu().copyCosts();
        if (costs.isEmpty()) {
            return;
        }
        final int x = this.leftPos + NoteScreenModel.COPY_PANEL_X;
        final int y = this.topPos + NoteScreenModel.COPY_PANEL_Y;
        graphics.drawString(this.font, Component.translatable(NoteScreenModel.COPY_HEADER_KEY),
                x, this.topPos + NoteScreenModel.COPY_HEADER_Y, TEXT_GOLD);
        graphics.fill(x, y, x + NoteScreenModel.COPY_PANEL_W, y + NoteScreenModel.COPY_PANEL_H,
                withAlpha(POOL_PLATE, 0.92F));
        int count = 0;
        for (final Map.Entry<String, Integer> cost : costs.entrySet()) {
            final int tagX = this.leftPos + NoteScreenModel.COPY_TAG_X
                    + NoteScreenModel.COPY_TAG_STEP * count;
            drawAspectIcon(graphics, cost.getKey(), tagX, y);
            graphics.drawString(this.font, Component.literal(String.valueOf(cost.getValue())),
                    tagX + 4, y + 9, TEXT_LIGHT);
            count++;
        }
    }

    private void drawDraggedAspect(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        final String dragged = this.model.draggedAspect();
        if (dragged == null) {
            return;
        }
        // The dragged orb (:352-357): the icon at the cursor, plus a drop-target ring.
        drawAspectIcon(graphics, dragged, mouseX - 8, mouseY - 8);
        graphics.fill(mouseX - 9, mouseY - 9, mouseX + 9, mouseY - 8, DRAG_GHOST);
        graphics.fill(mouseX - 9, mouseY + 8, mouseX + 9, mouseY + 9, DRAG_GHOST);
        graphics.fill(mouseX - 9, mouseY - 9, mouseX - 8, mouseY + 9, DRAG_GHOST);
        graphics.fill(mouseX + 8, mouseY - 9, mouseX + 9, mouseY + 9, DRAG_GHOST);
    }

    /**
     * The tooltip preview arms ({@code :322-379}). RESEARCHER1 gates them on the
     * server (NOTE-SCREEN-1's arm); the arms themselves are: a hovered pool
     * aspect shows its name plus the component preview the original derives from
     * that aspect's own two components ({@code :742-768}), and a hovered select
     * slot has its own arm ({@code :360-379}). The port ships no aspect-name lang
     * keys, so an aspect is named by its tag — recorded as a BD deviation, never
     * invented keys.
     */
    private void drawPreviews(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        final String hovered = this.model.aspectAt(mouseX, mouseY, true);
        if (this.model.showsComponentPreview(hovered)) {
            final NoteScreenModel.Combination preview = this.model.shiftCombinePreview(hovered);
            final List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(hovered));
            preview.cost().forEach((tag, amount) -> lines.add(
                    Component.literal(tag).append(Component.literal(" x" + amount))));
            if (preview.resultTag() != null) {
                lines.add(Component.literal("= " + preview.resultTag()));
            }
            drawTooltipBlock(graphics, mouseX + 12, mouseY - 12, lines);
            return;
        }
        final String slot = this.model.selectSlotAt(mouseX, mouseY);
        if (slot != null) {
            drawTooltipBlock(graphics, mouseX + 12, mouseY - 12, List.of(Component.literal(slot)));
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(final double screenX, final double screenY, final int button) {
        final int mouseX = (int) screenX - this.leftPos;
        final int mouseY = (int) screenY - this.topPos;
        if (button == 0) {
            // The click arms run in the original's gate order (:623-724); the
            // model applies every gate and says which arm fired.
            final NoteScreenModel.ClickAction action =
                    this.model.clickAt(mouseX, mouseY, Screen.hasShiftDown(),
                            System.nanoTime());
            // The arm plays its OWN sounds, in the original's order: the combine
            // button plays the cameraclack THEN the combine jingle (:632-633),
            // the erase arm the jingle THEN the erase (:731-732), a clear arm a
            // single hhoff (:662,676). The model's table is that order and the
            // window never re-derives it.
            playAll(action.sounds());
            switch (action) {
                case COMBINE -> sendCombination(this.model.select1(), this.model.select2());
                case PAGE_PREV, PAGE_NEXT -> { }
                case CLEAR_ONE, CLEAR_TWO -> play(NoteScreenModel.UiSound.BUTTON_ASPECT);
                case ERASE -> {
                    final Hex hex = this.model.eraseTarget(mouseX, mouseY);
                    if (hex != null) {
                        // The erase arm's payload: an absent aspect tag (:161-169).
                        PacketDistributor.sendToServer(new AspectPlaceToServerPayload(
                                BlockPos.ZERO, Optional.empty(), hex.q(), hex.r()));
                    }
                }
                case DUPLICATE -> {
                    // The copy arm's vanilla button channel: the menu's
                    // clickMenuButton(BUTTON_DUPLICATE) runs the server arm.
                    final Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(
                                this.getMenu().containerId, ResearchNoteMenu.BUTTON_DUPLICATE);
                    }
                }
                case SHIFT_COMBINE -> {
                    final String tag = this.model.aspectAt(mouseX, mouseY, true);
                    final List<String> components = this.model.componentTagsOf(tag);
                    if (components.size() == 2) {
                        sendCombination(components.get(0), components.get(1));
                    }
                }
                case REFUSED, NONE -> {
                    // Nothing to send; the original plays nothing on a refused arm.
                }
            }
            // The press arm (:135-145) runs after the click arms, on button 0:
            // inside the pool box (4, 4, 96, 96) with NO drag armed yet, it arms
            // the drag on the aspect under the cursor and plays one hhoff (:141).
            // The model's started() flag carries the isMouseButtonDown == 0 gate,
            // so a press that only follows an armed drag plays nothing (:135).
            playAll(this.model.pressAt(mouseX, mouseY).sounds());
            return true;
        }
        if (button == 1) {
            // :146-148 — the right button does nothing on this window.
            return true;
        }
        return super.mouseClicked(screenX, screenY, button);
    }

    @Override
    public boolean mouseReleased(final double screenX, final double screenY, final int button) {
        if (button == 0) {
            final int mouseX = (int) screenX - this.leftPos;
            final int mouseY = (int) screenY - this.topPos;
            final String dragged = this.model.draggedAspect();
            final NoteScreenModel.Drop drop = this.model.releaseAt(mouseX, mouseY);
            // The drop arm plays its own list, in the original's order: a grid
            // write the combine jingle THEN the write (:157-158), a select-slot
            // drop a single hhoff (:180,188), and the pool pick-up NOTHING at all
            // (:193-202).
            playAll(drop.sounds());
            switch (drop.kind()) {
                case HEX -> {
                    final Hex hex = Hex.fromKey(drop.hexKey());
                    // The write arm (:156-172): the payload the server validates.
                    PacketDistributor.sendToServer(new AspectPlaceToServerPayload(
                            BlockPos.ZERO, Optional.ofNullable(dragged), hex.q(), hex.r()));
                }
                case SELECT_ONE, SELECT_TWO, POOL_PICKUP_ONE, POOL_PICKUP_TWO -> {
                }
                case NOTHING -> {
                    // :174-207 — the original clears the drag on a release with no
                    // target; the drop box arms run only inside the release arm.
                    this.model.cancelDrag();
                }
            }
            return true;
        }
        return super.mouseReleased(screenX, screenY, button);
    }

    private void sendCombination(final String first, final String second) {
        if (first == null || second == null) {
            return;
        }
        // The combine payload (:770-783): the table position is the note menu's
        // sentinel — there is no research-table tile behind this window (BD-26).
        PacketDistributor.sendToServer(new AspectCombinationToServerPayload(
                BlockPos.ZERO, first, second, false, false));
        this.model.setSelect1(null);
        this.model.setSelect2(null);
    }

    /**
     * Play one arm's whole sound list, in order. The LIST and every entry's
     * volume and pitch are the model's ({@link NoteScreenModel.ClickAction#sounds()},
     * {@link NoteScreenModel.Drop#sounds()}, {@link NoteScreenModel.Press#sounds()});
     * the values are the reference's own, cited there, not tuned guesses.
     */
    private void playAll(final List<NoteScreenModel.UiSound> sounds) {
        for (final NoteScreenModel.UiSound sound : sounds) {
            play(sound);
        }
    }

    /**
     * The animation clock: the original reads the client player's tick counter
     * ({@code Minecraft.player.ticksExisted} at {@code :434} and {@code :860}).
     * 0 with no player, which only ever parks the animation.
     */
    private int clientTicks() {
        final net.minecraft.client.player.LocalPlayer player =
                Minecraft.getInstance().player;
        return player == null ? 0 : player.tickCount;
    }

    private void play(final NoteScreenModel.UiSound sound) {
        final net.minecraft.sounds.SoundEvent event = switch (sound.soundPath()) {
            case "cameraclack" -> ModSounds.CAMERACLACK.get();
            case "hhoff" -> ModSounds.HHOFF.get();
            case "hhon" -> ModSounds.HHON.get();
            case "write" -> ModSounds.WRITE.get();
            case "erase" -> ModSounds.ERASE.get();
            case "key" -> ModSounds.KEY.get();
            default -> null;
        };
        if (event == null) {
            return;
        }
        // The original's random pitch: 1 + rand * 0.1 (GuiResearchTable.java:785-798,
        // :830-843 — the hhoff and erase sounds).
        final float pitch = sound.pitchRandomised()
                ? 1.0F + RandomSource.createNewThreadLocalInstance().nextFloat() * 0.1F
                : sound.pitch();
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(event, pitch, sound.volume()));
    }

    // ------------------------------------------------------------- primitives

    /**
     * One aspect's icon: a tinted orb from the shipped aspect colour
     * ({@code AspectDefinitions.color}, the same colour the original tints its
     * icon with). The original blits the aspect sheet — an All Rights Reserved
     * asset — so the port draws the orb, not the sprite (BD row).
     */
    private static void drawAspectIcon(final GuiGraphics graphics, final String tag, final int x, final int y) {
        drawAspectIcon(graphics, tag, x, y, 1.0F);
    }

    private static void drawAspectIcon(final GuiGraphics graphics, final String tag, final int x, final int y,
                                       final float alpha) {
        final int colour = NoteScreenModel.aspectColourRgb(tag);
        graphics.fill(x + 2, y, x + 14, y + 16, withAlpha(colour, alpha));
        graphics.fill(x, y + 2, x + 16, y + 14, withAlpha(colour, alpha));
        // The orb's highlight: a lighter core, as the original's icon reads.
        graphics.fill(x + 4, y + 3, x + 12, y + 13, withAlpha(lighten(colour), alpha));
        graphics.fill(x + 6, y + 5, x + 10, y + 9, withAlpha(0xFFFFFFFF, alpha * 0.35F));
    }

    private static int lighten(final int colour) {
        final int r = Math.min(255, ((colour >> 16) & 0xFF) + 48);
        final int g = Math.min(255, ((colour >> 8) & 0xFF) + 48);
        final int b = Math.min(255, (colour & 0xFF) + 48);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * The hex as 1px step fills: the sprite's quad spans ±8 around the centre
     * ({@code :402-405}); the ring is its outline, the fill its interior.
     */
    private static void drawHexRing(final GuiGraphics graphics, final int centreX, final int centreY, final int colour) {
        final int size = -NoteScreenModel.HEX_PLATE_OFFSET;
        graphics.fill(centreX - size, centreY - size, centreX + size, centreY - size + 1, colour);
        graphics.fill(centreX - size, centreY + size - 1, centreX + size, centreY + size, colour);
        graphics.fill(centreX - size, centreY - size, centreX - size + 1, centreY + size, colour);
        graphics.fill(centreX + size - 1, centreY - size, centreX + size, centreY + size, colour);
        for (int step = 0; step < size; step++) {
            // The 1px step fills of the original's diagonal line strips.
            graphics.fill(centreX - size + step, centreY - size + step,
                    centreX - size + step + 1, centreY - size + step + 1, colour);
            graphics.fill(centreX + size - step - 1, centreY - size + step,
                    centreX + size - step, centreY - size + step + 1, colour);
            graphics.fill(centreX - size + step, centreY + size - step - 1,
                    centreX - size + step + 1, centreY + size - step, colour);
            graphics.fill(centreX + size - step - 1, centreY + size - step - 1,
                    centreX + size - step, centreY + size - step, colour);
        }
    }

    private static void drawHexFill(final GuiGraphics graphics, final int centreX, final int centreY,
                                    final int colour, final float alpha) {
        final int size = -NoteScreenModel.HEX_PLATE_OFFSET;
        graphics.fill(centreX - size + 1, centreY - size + 1, centreX + size - 1, centreY + size - 1,
                withAlpha(colour, alpha));
    }

    private static void drawLine(final GuiGraphics graphics, final int fromX, final int fromY,
                                 final int toX, final int toY, final int colour, final float alpha) {
        final int steps = Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));
        for (int step = 0; step <= steps; step++) {
            final int x = fromX + (toX - fromX) * step / Math.max(1, steps);
            final int y = fromY + (toY - fromY) * step / Math.max(1, steps);
            graphics.fill(x, y, x + 1, y + 1, withAlpha(colour, alpha));
        }
    }

    /** The grid field's ring: the original's parchment circle, as 1px step fills. */
    private static void drawFieldRing(final GuiGraphics graphics, final int centreX, final int centreY,
                                      final int radius) {
        for (int step = 0; step < 360; step++) {
            final int px = centreX + (int) Math.round(radius * Math.cos(Math.toRadians(step)));
            final int py = centreY + (int) Math.round(radius * Math.sin(Math.toRadians(step)));
            graphics.fill(px, py, px + 1, py + 1, GRID_FIELD_EDGE);
        }
    }

    private static void drawRectRing(final GuiGraphics graphics, final int x, final int y,
                                     final int width, final int height, final int colour) {
        graphics.fill(x, y, x + width, y + 1, colour);
        graphics.fill(x, y + height - 1, x + width, y + height, colour);
        graphics.fill(x, y, x + 1, y + height, colour);
        graphics.fill(x + width - 1, y, x + width, y + height, colour);
    }

    private void drawArrow(final GuiGraphics graphics, final int x, final boolean enabled, final boolean left) {
        final int px = this.leftPos + x;
        final int py = this.topPos + NoteScreenModel.PAGE_ARROW_Y;
        for (int step = 0; step < NoteScreenModel.PAGE_ARROW_H; step++) {
            // The arrow as 1px step fills (the original's arrow quad, :651,657).
            final int length = NoteScreenModel.PAGE_ARROW_H - step;
            final int startX = left ? px + 2 + step : px + 2 + NoteScreenModel.PAGE_ARROW_H - length - step;
            graphics.fill(startX, py + step, startX + length, py + step + 1, MARK_ARROW);
        }
        if (!enabled) {
            graphics.fill(px, py, px + NoteScreenModel.PAGE_ARROW_W, py + NoteScreenModel.PAGE_ARROW_H,
                    withAlpha(0xFF000000, 0.5F));
        }
    }

    private void drawTooltipBlock(final GuiGraphics graphics, final int x, final int y,
                                  final List<Component> lines) {
        int width = 0;
        for (final Component line : lines) {
            width = Math.max(width, this.font.width(line));
        }
        final int height = 8 + lines.size() * 10;
        graphics.fill(x - 3, y - 4, x + width + 3, y + height - 4, TOOLTIP_BACKGROUND);
        graphics.fill(x - 3, y - 4, x + width + 3, y - 3, TOOLTIP_FRAME_TOP);
        graphics.fill(x - 3, y + height - 5, x + width + 3, y + height - 4, TOOLTIP_FRAME_TOP);
        graphics.fill(x - 3, y - 4, x - 2, y + height - 4, TOOLTIP_FRAME_SIDES);
        graphics.fill(x + width + 2, y - 4, x + width + 3, y + height - 4, TOOLTIP_FRAME_SIDES);
        int line = 0;
        for (final Component text : lines) {
            graphics.drawString(this.font, text, x, y + line * 10, TEXT_LIGHT);
            line++;
        }
    }

    private boolean isInBox(final int mouseX, final int mouseY, final int x, final int y,
                            final int width, final int height) {
        final int dx = mouseX - this.leftPos - x;
        final int dy = mouseY - this.topPos - y;
        return dx >= 0 && dy >= 0 && dx < width && dy < height;
    }

    private static int withAlpha(final int colour, final float alpha) {
        final int a = (int) Math.clamp(alpha * 255.0F, 0.0F, 255.0F);
        return (a << 24) | (colour & 0x00FFFFFF);
    }
}
