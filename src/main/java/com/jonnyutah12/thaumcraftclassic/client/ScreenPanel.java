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
        final int playerPanelTop = ContainerLayout.playerPanelTop(playerGridTop);
        final int panelHeight = ContainerLayout.panelHeight(playerGridTop);
        guiGraphics.fill(left, top, left + ContainerLayout.PANEL_WIDTH, top + playerPanelTop,
                ContainerLayout.PANEL_FILL);
        guiGraphics.renderOutline(left, top, ContainerLayout.PANEL_WIDTH, panelHeight,
                ContainerLayout.PANEL_FRAME);
        guiGraphics.blit(ContainerLayout.PLAYER_PANEL,
                left, top + playerPanelTop,
                ContainerLayout.PLAYER_PANEL_U, ContainerLayout.PLAYER_PANEL_V,
                ContainerLayout.PANEL_WIDTH, ContainerLayout.PLAYER_PANEL_HEIGHT);
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
     * A flat fill at panel-relative coordinates: the authored stand-in for an
     * original sheet's texels (gauge fills, status icons), which ship only in
     * the overlay build (BD-17). Same convention as the deconstruction break
     * bar's {@code DECON_BAR_FILL}.
     */
    static void fillArea(final GuiGraphics guiGraphics, final int left, final int top,
                         final int x, final int y, final int width, final int height, final int color) {
        guiGraphics.fill(left + x, top + y, left + x + width, top + y + height, color);
    }

    private ScreenPanel() {
    }
}
