package com.jonnyutah12.thaumcraftclassic.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The shared drawing primitives every authored container screen in this crate
 * uses (TC-M7-04-s). {@link ContainerLayout} carries the arithmetic; this class
 * carries the three calls that do the drawing, so five screens do not each
 * repeat the vanilla panel recipe.
 *
 * <p>The recipe is the one {@code TrunkScreen} (TC-M7-04-s-cr1) established,
 * generalized to a player-grid origin: a flat vanilla-toned panel behind the
 * machine region, a 1 px vanilla frame around the whole panel, the vanilla
 * player-inventory piece of {@code generic_54.png} under the player grid, and
 * one vanilla {@code minecraft:container/slot} sprite per machine slot. No
 * original Thaumcraft sheet is referenced — they ship only in the
 * asset-overlay build (D-004) — and no legacy GL is used: 1.21.1 draws through
 * {@link GuiGraphics} (docs/BEHAVIORAL_DEVIATIONS.md BD-17).</p>
 */
@OnlyIn(Dist.CLIENT)
final class ScreenPanel {

    /** The flat panel plus the vanilla player piece, for a player-grid origin. */
    static void drawBackground(final GuiGraphics guiGraphics, final int left, final int top,
                               final int playerGridTop) {
        drawBackground(guiGraphics, left, top, playerGridTop,
                ContainerLayout.PANEL_WIDTH, 0);
    }

    /**
     * The same recipe for a panel that is wider than vanilla's 176 px, which is
     * what the arcane workbench is ({@code GuiArcaneWorkbench} blits a 234 px
     * sheet): the vanilla player piece is a fixed 176 px, so it is blitted at a
     * caller-chosen x inside the panel and the strips of panel it does not cover
     * are filled with the same vanilla tone.
     *
     * @param playerPieceX panel-relative x of the 176 px {@code generic_54.png} piece
     */
    static void drawBackground(final GuiGraphics guiGraphics, final int left, final int top,
                               final int playerGridTop, final int panelWidth, final int playerPieceX) {
        final int playerPanelTop = ContainerLayout.playerPanelTop(playerGridTop);
        final int panelHeight = ContainerLayout.panelHeight(playerGridTop);
        // The machine region, then the strips beside the fixed-width player piece.
        // A vanilla-width panel (pieceX = 0, panelWidth = 176) has no strips; the
        // arcane workbench (pieceX = 0, panelWidth = 190) has one 14 px strip on
        // the right of the piece.
        guiGraphics.fill(left, top, left + panelWidth, top + playerPanelTop, ContainerLayout.PANEL_FILL);
        if (playerPieceX > 0) {
            guiGraphics.fill(left, top + playerPanelTop, left + playerPieceX,
                    top + panelHeight, ContainerLayout.PANEL_FILL);
        }
        fillRightStrip(guiGraphics, left, top, playerPieceX + ContainerLayout.PANEL_WIDTH,
                panelWidth, playerPanelTop, panelHeight);
        guiGraphics.renderOutline(left, top, panelWidth, panelHeight, ContainerLayout.PANEL_FRAME);
        guiGraphics.blit(ContainerLayout.PLAYER_PANEL,
                left + playerPieceX, top + playerPanelTop,
                ContainerLayout.PLAYER_PANEL_U, ContainerLayout.PLAYER_PANEL_V,
                ContainerLayout.PANEL_WIDTH, ContainerLayout.PLAYER_PANEL_HEIGHT);
    }

    /** The panel tone from {@code fromX} to the panel's right edge; skipped when empty. */
    private static void fillRightStrip(final GuiGraphics guiGraphics, final int left, final int top,
                                       final int fromX, final int panelWidth, final int fromY,
                                       final int panelHeight) {
        if (fromX < panelWidth) {
            guiGraphics.fill(left + fromX, top + fromY, left + panelWidth, top + panelHeight,
                    ContainerLayout.PANEL_FILL);
        }
    }

    /** One vanilla slot square, one pixel up-left of the slot's item anchor. */
    static void frameSlot(final GuiGraphics guiGraphics, final int left, final int top, final Slot slot) {
        guiGraphics.blitSprite(ContainerLayout.SLOT_SPRITE,
                left + ContainerLayout.slotFrameX(slot.x), top + ContainerLayout.slotFrameY(slot.y),
                ContainerLayout.SLOT_FRAME, ContainerLayout.SLOT_FRAME);
    }

    /** A framed rectangle at panel-relative coordinates (an unmovable UI box). */
    static void frameArea(final GuiGraphics guiGraphics, final int left, final int top,
                          final int x, final int y, final int width, final int height) {
        guiGraphics.renderOutline(left + x, top + y, width, height, ContainerLayout.PANEL_FRAME);
    }

    /**
     * A flat filled rectangle at panel-relative coordinates — the primitive for
     * the state a 1.7.10 GUI drew from a texture the port does not ship, e.g. the
     * arcane workbench's vis-cost tags, where the aspect's own colour stands in
     * for {@code UtilsFX.drawTag}'s 16x16 tag (BD-17: no invented art).
     */
    static void fillArea(final GuiGraphics guiGraphics, final int left, final int top,
                         final int x, final int y, final int width, final int height, final int colour) {
        guiGraphics.fill(left + x, top + y, left + x + width, top + y + height, colour);
    }

    private ScreenPanel() {
    }
}
