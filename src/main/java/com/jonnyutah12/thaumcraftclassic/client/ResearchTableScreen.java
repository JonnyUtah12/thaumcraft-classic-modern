package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectCombinations;
import com.jonnyutah12.thaumcraftclassic.menus.ResearchTableMenu;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectCombinationToServerPayload;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The research table's screen — TC-M7-04-m2b, content-mapping row
 * {@code thaumcraft:gui_research_table} (original gui_id 10, original sheet
 * {@code gui/guiresearchtable2.png}).
 *
 * <p>Parity, measured off {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchTable.java}
 * and {@code .../common/container/ContainerResearchTable.java}: a
 * <b>255&nbsp;x&nbsp;255</b> window (the original's {@code xSize = 255},
 * {@code ySize = 255}); the scribing slot at {@code (14,10)} and the note slot at
 * {@code (70,10)}; the player grid at {@code (48,175)} and the hotbar at
 * {@code (48,233)}; the discovered-aspect grid at {@code (10,40)} on a
 * <b>16 px</b> pitch, five per row, twenty-five per page, the page moving the
 * start index by five; the page rails at {@code (27,121)} and {@code (51,121)},
 * 24x8, the back rail only while {@code page > 0} and the forward rail only while
 * {@code page < (count - 20) / 5}; the two select wells as 16x16 tags at
 * {@code (13,139)} and {@code (71,139)} with clear hitboxes at {@code (11,137)}
 * and {@code (71,137)}; the combine plate 32x16 at {@code (35,139)}, drawn only
 * while both wells are set and pressed only on that same rect, arming a
 * 200&nbsp;ms cooldown; the camera plate 24x24 at {@code (37,5)}; and the
 * out-of-ink notice centred on {@code (157,84)}. Every number is a named constant
 * in {@link ContainerLayout}, shared with {@link ResearchTableMenu}'s slot
 * anchors; the panel and slot art are the shared {@link ScreenPanel} recipe.</p>
 *
 * <p><b>The pool is read from the menu, never guessed.</b> The original reached
 * straight for {@code Thaumcraft.proxy.playerKnowledge.getAspectPoolFor(...)} and
 * {@code tile.bonusAspects} and for {@code ResearchManager.isResearchKnown(...)}.
 * This window has only its menu, so each cell's amount, the table's bonus for it,
 * the discovered count that drives the paging and the RESEARCHER1 / RESEARCHER2 /
 * RESEARCHDUPE gates arrive as synced data slots
 * ({@link ResearchTableMenu#aspectAt(int)}, {@code poolAt}, {@code bonusAt}). A
 * cell whose pool and bonus are both zero is drawn at the original's
 * {@code 0.33F} alpha, which is the original's own "faded" rule.</p>
 *
 * <p><b>Combination leaves the window on the original's own channel.</b> The
 * original's press built {@code PacketAspectCombinationToServer(x, y, z, aspect1,
 * aspect2, ab1, ab2)}; the port sends {@link AspectCombinationToServerPayload}
 * with the same five fields, and the server's tile runs the rule
 * ({@code TileResearchTable.combineAspects}). The sneak-plus-compound shortcut is
 * reproduced on the client as the original's: shift on the press, the synced
 * RESEARCHER2 flag set, a compound of the two selected aspects exists, and the
 * same packet goes out. The wells clear and the cooldown arms, as in the original.</p>
 *
 * <p><b>What this window cannot show, and says so.</b> The original drew each
 * aspect with {@code UtilsFX.drawTag} (a 16x16 tag from {@code gear.png} plus an
 * amount glyph) and dragged the tag under the cursor with GL colour masks; the
 * port ships no such atlas, so a cell is the aspect's own colour block with its
 * amount as text and the selection is made by clicking a cell (BD-19). The
 * camera plate needs a finished note in the notes slot, and the notes slot cannot
 * hold one while the note item and its data component are unmerged — the plate is
 * therefore never drawn today, the button id 5 and the 24x24 hitbox are wired and
 * pinned by tests, and BD-19 names the lane that lights it.</p>
 */
@OnlyIn(Dist.CLIENT)
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {

    /** The lang key of the research table menu (the menu type is named for the gui row). */
    static final String TITLE_KEY = "container.thaumcraft.gui_research_table";

    /** The original drew no title; the port centres it in its own 255 px panel at vanilla y. */
    static final int TITLE_X = ContainerLayout.RT_PANEL_WIDTH / 2;
    static final int TITLE_Y = 6;

    /** The original's {@code page}, {@code select1}/{@code select2} and their bonus flags. */
    private int page;
    private Aspect select1;
    private Aspect select2;
    private int bonus1;
    private int bonus2;
    /** The original's {@code butcount2}: a nanosecond deadline, 200 ms after a press. */
    private long combineCooldownUntil;

    public ResearchTableScreen(final ResearchTableMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title.getString().isEmpty() ? Component.translatable(TITLE_KEY) : title);
        this.imageWidth = ContainerLayout.RT_PANEL_WIDTH;
        this.imageHeight = ContainerLayout.panelHeight(ContainerLayout.RT_PLAYER_GRID_TOP);
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = ContainerLayout.RT_PLAYER_PIECE_X + ContainerLayout.titleLabelX();
        this.inventoryLabelY = ContainerLayout.inventoryLabelY(ContainerLayout.RT_PLAYER_GRID_TOP);
    }

    /** The page this window is showing (pinned by {@code ResearchTableGameTest}). */
    int page() {
        return this.page;
    }

    Aspect select1() {
        return this.select1;
    }

    Aspect select2() {
        return this.select2;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX,
                            final int mouseY) {
        ScreenPanel.drawBackground(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.RT_PLAYER_GRID_TOP, ContainerLayout.RT_PANEL_WIDTH,
                ContainerLayout.RT_PLAYER_PIECE_X);
        for (final net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, slot);
        }
        renderAspectGrid(guiGraphics);
        renderSelectWells(guiGraphics);
        renderPageRails(guiGraphics);
        renderCombinePlate(guiGraphics);
        renderCameraPlate(guiGraphics);
    }

    // ------------------------------------------------------------- the aspect grid

    /**
     * The original's walk: start at {@code page * 5}, keep twenty-five cells, five
     * to a row, 16 px pitch from {@code (10,40)}. Each cell is the aspect's colour
     * block with its pool amount beside it, at the original's alpha —
     * {@code 0.33F} when neither the player's pool nor the table's bonus has one.
     */
    private void renderAspectGrid(final GuiGraphics guiGraphics) {
        for (int drawn = 0; drawn < ContainerLayout.RT_ASPECTS_PER_PAGE; drawn++) {
            final int index = ContainerLayout.researchTableIndexAt(drawn, this.page);
            final Aspect aspect = this.menu.aspectAt(index);
            if (aspect == null) {
                continue;
            }
            final int x = ContainerLayout.researchTableGridX(drawn);
            final int y = ContainerLayout.researchTableGridY(drawn);
            final int pool = this.menu.poolAt(index);
            final int bonus = this.menu.bonusAt(index);
            final boolean faded = pool <= 0 && bonus <= 0;
            final int colour = ContainerLayout.withAlpha(aspect.getColor(),
                    faded ? ContainerLayout.RT_FADED_ALPHA : 1.0F);
            ScreenPanel.fillArea(guiGraphics, this.leftPos, this.topPos,
                    x + 1, y + 1, ContainerLayout.RT_ASPECT_CELL - 2, ContainerLayout.RT_ASPECT_CELL - 2,
                    colour);
            if (pool > 0) {
                // The original's amount glyph, in the tag's bottom-right corner.
                guiGraphics.drawString(this.font, Integer.toString(pool),
                        this.leftPos + x + ContainerLayout.RT_ASPECT_CELL - this.font.width(Integer.toString(pool)) - 1,
                        this.topPos + y + ContainerLayout.RT_ASPECT_CELL - this.font.lineHeight,
                        0xFFFFFFFF, true);
            }
        }
    }

    /** The two select wells: 16x16 frames at the original's points, filled when set. */
    private void renderSelectWells(final GuiGraphics guiGraphics) {
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.RT_SELECT1_X, ContainerLayout.RT_SELECT_Y,
                ContainerLayout.RT_SELECT_SIZE, ContainerLayout.RT_SELECT_SIZE);
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.RT_SELECT2_X, ContainerLayout.RT_SELECT_Y,
                ContainerLayout.RT_SELECT_SIZE, ContainerLayout.RT_SELECT_SIZE);
        drawSelected(guiGraphics, this.select1, this.bonus1, ContainerLayout.RT_SELECT1_X);
        drawSelected(guiGraphics, this.select2, this.bonus2, ContainerLayout.RT_SELECT2_X);
    }

    private void drawSelected(final GuiGraphics guiGraphics, final Aspect aspect, final int bonus, final int x) {
        if (aspect == null) {
            return;
        }
        // The original drew a second, dimmer tag when the well was fed by the table's
        // bonus pool; the port marks the same state with a grey frame inside the well.
        ScreenPanel.fillArea(guiGraphics, this.leftPos, this.topPos, x + 1, ContainerLayout.RT_SELECT_Y + 1,
                ContainerLayout.RT_SELECT_SIZE - 2, ContainerLayout.RT_SELECT_SIZE - 2,
                ContainerLayout.withAlpha(aspect.getColor(), bonus > 0 ? 0.5F : 1.0F));
    }

    /** The page rails: the original's 24x8 plates, shown only when they can be pressed. */
    private void renderPageRails(final GuiGraphics guiGraphics) {
        if (this.page > 0) {
            ScreenPanel.fillPlate(guiGraphics, this.leftPos, this.topPos,
                    ContainerLayout.RT_PAGE_PREV_X, ContainerLayout.RT_PAGE_Y,
                    ContainerLayout.RT_PAGE_WIDTH, ContainerLayout.RT_PAGE_HEIGHT,
                    ContainerLayout.RT_PLATE_FILL);
        }
        if (this.page < ContainerLayout.researchTableLastPage(this.menu.getDiscoveredCount())) {
            ScreenPanel.fillPlate(guiGraphics, this.leftPos, this.topPos,
                    ContainerLayout.RT_PAGE_NEXT_X, ContainerLayout.RT_PAGE_Y,
                    ContainerLayout.RT_PAGE_WIDTH, ContainerLayout.RT_PAGE_HEIGHT,
                    ContainerLayout.RT_PLATE_FILL);
        }
    }

    /** The combine plate: drawn only while both wells are set, as the original drew it. */
    private void renderCombinePlate(final GuiGraphics guiGraphics) {
        if (this.select1 == null || this.select2 == null) {
            return;
        }
        ScreenPanel.fillPlate(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.RT_COMBINE_X, ContainerLayout.RT_COMBINE_Y,
                ContainerLayout.RT_COMBINE_WIDTH, ContainerLayout.RT_COMBINE_HEIGHT,
                ContainerLayout.RT_PLATE_FILL);
        if (Util.getNanos() < this.combineCooldownUntil) {
            // The original's second, darkened blit over the same rect while its
            // 200 ms cooldown ran (GuiResearchTable.java:752-757).
            ScreenPanel.fillArea(guiGraphics, this.leftPos, this.topPos,
                    ContainerLayout.RT_COMBINE_X, ContainerLayout.RT_COMBINE_Y,
                    ContainerLayout.RT_COMBINE_WIDTH, ContainerLayout.RT_COMBINE_HEIGHT,
                    ContainerLayout.RT_PLATE_COOLDOWN_TINT);
        }
    }

    /**
     * The camera plate: the original drew it at {@code (37,5)} only while
     * {@code RESEARCHDUPE} was researched <b>and</b> the notes slot held a finished
     * note. The second half can never be true today (BD-19: no note item, no note
     * data component), so the plate is not drawn; the hitbox and the button id
     * below are the wired seam, and {@code ResearchTableGameTest} pins both.
     */
    private void renderCameraPlate(final GuiGraphics guiGraphics) {
        if (!canDuplicate()) {
            return;
        }
        ScreenPanel.fillPlate(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.RT_DUPE_X, ContainerLayout.RT_DUPE_Y,
                ContainerLayout.RT_DUPE_SIZE, ContainerLayout.RT_DUPE_SIZE,
                ContainerLayout.RT_PLATE_FILL);
    }

    /**
     * The original's gate on its camera plate: {@code isResearchKnown("RESEARCHDUPE")}
     * — synced, so real — and a note at damage 64 in the notes slot, which the port
     * cannot see because no note item is registered and its data component is
     * unmerged. That second test is the documented refusal, not a guess.
     */
    private boolean canDuplicate() {
        return this.menu.knowsResearchDupe() && noteIsComplete();
    }

    /** The note lane's completion test: no note item, so never true (BD-19). */
    private boolean noteIsComplete() {
        return false;
    }

    // ------------------------------------------------------------------ tooltips

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY,
                       final float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderAspectTooltip(guiGraphics, mouseX, mouseY);
        renderOutOfInk(guiGraphics);
    }

    /**
     * The original's aspect tooltip: {@code drawTxt(... aspect.getName(), "" + amt)}
     * beside the grid. The port adds the table's bonus on its own line, because the
     * original showed that state as a second tag and this window has no tag art —
     * the information, not the art.
     */
    private void renderAspectTooltip(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        final int index = hoveredAspectIndex(mouseX, mouseY);
        if (index < 0) {
            return;
        }
        final Aspect aspect = this.menu.aspectAt(index);
        if (aspect == null) {
            return;
        }
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(aspect.getName()));
        lines.add(Component.literal(Integer.toString(this.menu.poolAt(index))));
        final int bonus = this.menu.bonusAt(index);
        if (bonus > 0) {
            lines.add(Component.translatable("container.thaumcraft.research_table_from_table", bonus));
        }
        guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    /**
     * The original's out-of-ink notice (GuiResearchTable.java:213-228): two
     * shadowed white lines centred on {@code (157,84)}, shown while the scribing
     * slot is empty or its tool is fully used.
     */
    private void renderOutOfInk(final GuiGraphics guiGraphics) {
        if (!this.menu.isOutOfInk()) {
            return;
        }
        final Component first = Component.translatable("tile.researchtable.noink.0");
        final Component second = Component.translatable("tile.researchtable.noink.1");
        final int widest = Math.max(this.font.width(first.getString()),
                this.font.width(second.getString()));
        final int x = this.leftPos + ContainerLayout.researchTableNoInkX(widest);
        guiGraphics.drawString(this.font, first, x, this.topPos + ContainerLayout.RT_NOINK_Y,
                ContainerLayout.RT_NOTICE_COLOUR, true);
        guiGraphics.drawString(this.font, second, x,
                this.topPos + ContainerLayout.RT_NOINK_Y + ContainerLayout.RT_NOTICE_LINE_GAP,
                ContainerLayout.RT_NOTICE_COLOUR, true);
    }

    // ------------------------------------------------------------------ the click

    /**
     * The original's {@code mouseClicked} chain, in its order: the camera plate,
     * the combine plate, the shift-shortcut compound, the page rails, the two clear
     * boxes, then the aspect grid. Only one of them consumes the click, exactly as
     * the original's early-returning branches do; everything else goes to vanilla.
     */
    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final int relX = (int) mouseX - this.leftPos;
        final int relY = (int) mouseY - this.topPos;
        if (button == 1 && this.minecraft != null) {
            // 1. The camera plate — the original's sendAnimationRequest(..., 1, 5),
            //    whose port seam is the container's own button 5, the original's
            //    `case 5: tileEntity.duplicate(player)` (ContainerResearchTable:127).
            if (canDuplicate()
                    && ContainerLayout.inRect(relX, relY, ContainerLayout.RT_DUPE_X, ContainerLayout.RT_DUPE_Y,
                    ContainerLayout.RT_DUPE_SIZE, ContainerLayout.RT_DUPE_SIZE)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                        ResearchTableMenu.BUTTON_DUPLICATE);
                return true;
            }
            // 2. The combine plate — the original's :622-628 rect.
            if (this.select1 != null && this.select2 != null
                    && ContainerLayout.inRect(relX, relY, ContainerLayout.RT_COMBINE_X, ContainerLayout.RT_COMBINE_Y,
                    ContainerLayout.RT_COMBINE_WIDTH, ContainerLayout.RT_COMBINE_HEIGHT)) {
                sendCombination();
                return true;
            }
            // 3. The original's shift-shortcut (:695-727): `if (func_146272_n() &&
            //    RESEARCHER_2)` — the shift press combines a compound, and the
            //    RESEARCHER2 flag gates it; the flag is the synced data slot, not
            //    a client-side research lookup. The port combines the compound of
            //    the two selected aspects, because it has no tag to click in the
            //    grid (BD-19).
            if (hasShiftDown() && this.menu.knowsResearcher2()
                    && this.select1 != null && this.select2 != null
                    && AspectCombinations.getCombinationResult(this.select1, this.select2) != null) {
                sendCombination();
                return true;
            }
            // 4. The page rails (the original's 30x12 arrows at x=7 / x=244).
            final int lastPage = ContainerLayout.researchTableLastPage(this.menu.getDiscoveredCount());
            if (this.page > 0
                    && ContainerLayout.inRect(relX, relY, ContainerLayout.RT_PAGE_PREV_X, ContainerLayout.RT_PAGE_Y,
                    ContainerLayout.RT_PAGE_WIDTH, ContainerLayout.RT_PAGE_HEIGHT)) {
                this.page--;
                return true;
            }
            if (this.page < lastPage
                    && ContainerLayout.inRect(relX, relY, ContainerLayout.RT_PAGE_NEXT_X, ContainerLayout.RT_PAGE_Y,
                    ContainerLayout.RT_PAGE_WIDTH, ContainerLayout.RT_PAGE_HEIGHT)) {
                this.page++;
                return true;
            }
            // 5. The two clear boxes, at the original's own 16x16 hitboxes, and
            //    only while that well is set (GuiResearchTable.java:653-671).
            if (this.select1 != null
                    && ContainerLayout.inRect(relX, relY, ContainerLayout.RT_CLEAR1_X, ContainerLayout.RT_CLEAR_Y,
                    ContainerLayout.RT_CLEAR_SIZE, ContainerLayout.RT_CLEAR_SIZE)) {
                this.select1 = null;
                this.bonus1 = 0;
                return true;
            }
            if (this.select2 != null
                    && ContainerLayout.inRect(relX, relY, ContainerLayout.RT_CLEAR2_X, ContainerLayout.RT_CLEAR_Y,
                    ContainerLayout.RT_CLEAR_SIZE, ContainerLayout.RT_CLEAR_SIZE)) {
                this.select2 = null;
                this.bonus2 = 0;
                return true;
            }
            // 6. The aspect grid: the cell under the cursor goes into the free well.
            //    The original's {@code getClickedAspect(..., false)} skips a cell
            //    whose pool and bonus are both empty, so this press skips it too.
            final int index = hoveredAspectIndex(relX, relY);
            if (index >= 0) {
                final Aspect aspect = this.menu.aspectAt(index);
                if (aspect != null
                        && (this.menu.poolAt(index) > 0 || this.menu.bonusAt(index) > 0)) {
                    selectAspect(aspect, this.menu.bonusAt(index));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * The cell under the cursor among the twenty-five this page draws, as a menu
     * row index, or -1. The original's drag-drop targets ({@link ContainerLayout#inDragTarget})
     * are the same rects on its own {@code mouseReleased} path; the port selects on
     * the press because it has no tag to drag (BD-19).
     */
    private int hoveredAspectIndex(final int mouseX, final int mouseY) {
        final double lx = mouseX - this.leftPos;
        final double ly = mouseY - this.topPos;
        for (int drawn = 0; drawn < ContainerLayout.RT_ASPECTS_PER_PAGE; drawn++) {
            final int x = ContainerLayout.researchTableGridX(drawn);
            final int y = ContainerLayout.researchTableGridY(drawn);
            if (ContainerLayout.inRect(lx, ly, x, y, ContainerLayout.RT_ASPECT_CELL,
                    ContainerLayout.RT_ASPECT_CELL)) {
                return ContainerLayout.researchTableIndexAt(drawn, this.page);
            }
        }
        return -1;
    }

    /**
     * The original's selection rule, run on the press instead of on the drag
     * release: the first free well takes it
     * ({@code if (this.select1 == null) this.select1 = this.draggedAspect; else if
     * (this.select2 == null) this.select2 = this.draggedAspect} —
     * GuiResearchTable.java:700-717). The bonus flag is the original's
     * {@code tile.bonusAspects.getAmount(aspect) > 0}, read from the synced row.
     */
    private void selectAspect(final Aspect aspect, final int bonus) {
        if (this.select1 == null) {
            this.select1 = aspect;
            this.bonus1 = bonus;
        } else if (this.select2 == null) {
            this.select2 = aspect;
            this.bonus2 = bonus;
        }
    }

    /**
     * The original's press: send the aspect pair to the server, clear both wells
     * and arm the 200 ms cooldown. The payload is the port's decoding of the
     * original's packet — the table's position and the two tags, with the two
     * "paid from the table" flags.
     */
    private void sendCombination() {
        PacketDistributor.sendToServer(new AspectCombinationToServerPayload(
                this.menu.table().getBlockPos(),
                this.select1.getTag(),
                this.select2.getTag(),
                this.bonus1 > 0,
                this.bonus2 > 0));
        this.select1 = null;
        this.select2 = null;
        this.bonus1 = 0;
        this.bonus2 = 0;
        this.combineCooldownUntil = Util.getNanos() + ContainerLayout.RT_BUTTON_COOLDOWN_NANOS;
    }

    /** The plate's cooldown state, exposed for the probe that pins the 200 ms window. */
    boolean combineCoolingDown() {
        return Util.getNanos() < this.combineCooldownUntil;
    }
}
