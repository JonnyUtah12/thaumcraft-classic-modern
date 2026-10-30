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
                ContainerLayout.PANEL_WIDTH, ContainerLayout.panelHeight(playerGridTop));
    }

    /**
     * The same panel for a container whose own sheet is not the 1.7.10 default
     * size. The focus pouch's {@code gui_focuspouch.png} is 175x232
     * ({@code GuiFocusPouch.java:19-20}), so the vanilla player piece — 176 px
     * wide and 96 px tall, i.e. 234 px down from the piece top the piece
     * implies — is cropped to the panel on both axes: 1 px of its right edge
     * and 2 px of its bottom shadow. The 1 px frame is drawn over the full
     * panel, so the crop stays inside the frame (docs/BEHAVIORAL_DEVIATIONS.md
     * BD-18).
     */
    static void drawBackground(final GuiGraphics guiGraphics, final int left, final int top,
                               final int playerGridTop, final int panelWidth, final int panelHeight) {
        final int playerPanelTop = ContainerLayout.playerPanelTop(playerGridTop);
        guiGraphics.fill(left, top, left + panelWidth, top + playerPanelTop,
                ContainerLayout.PANEL_FILL);
        guiGraphics.renderOutline(left, top, panelWidth, panelHeight,
                ContainerLayout.PANEL_FRAME);
        guiGraphics.blit(ContainerLayout.PLAYER_PANEL,
                left, top + playerPanelTop,
                ContainerLayout.PLAYER_PANEL_U, ContainerLayout.PLAYER_PANEL_V,
                panelWidth, ContainerLayout.playerPieceHeight(panelHeight, playerGridTop));
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

    private ScreenPanel() {
    }
}
