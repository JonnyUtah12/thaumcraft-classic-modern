package com.jonnyutah12.thaumcraftclassic.client.gui;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.menus.ArcaneWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Arcane workbench GUI (TC-M4-03; original {@code GuiArcaneWorkbench}).
 *
 * <p>Layout and behaviour mirror the original: 190x234 texture
 * {@code textures/gui/gui_arcaneworkbench.png} (ARR, local build overlay per
 * D-004), the six primal cost readouts on the hexagon at the original
 * positions, the grayed result + "Insufficient vis" warning when the wand
 * cannot pay, and the same client-side recomputation of the match/cost the
 * original GUI performed every frame.</p>
 *
 * <p>Documented deviations: the original drew animated essentia tag sprites
 * (UtilsFX.drawTag, pulsing alpha, full-alpha when payable); the port's tag
 * renderer is a later card, so costs render as colored aspect name + amount
 * text with the same payable-alpha rule. The original showed nothing for a
 * research-locked recipe (the result slot was simply empty); the acceptance
 * criteria require the locked state to be visible, so a red "Locked" caption
 * is drawn at the result slot.</p>
 */
public class ArcaneWorkbenchScreen extends AbstractContainerScreen<ArcaneWorkbenchMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ThaumcraftClassic.MOD_ID, "textures/gui/gui_arcaneworkbench.png");

    /** Original GuiArcaneWorkbench.aspectLocs, in Aspect.getPrimalAspects() order. */
    private static final int[][] ASPECT_LOCS = {
            {72, 21}, {24, 43}, {24, 102}, {72, 124}, {120, 102}, {120, 43}
    };

    public ArcaneWorkbenchScreen(final ArcaneWorkbenchMenu menu, final Inventory playerInventory,
                                 final Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 190;
        this.imageHeight = 234;
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick,
                            final int mouseX, final int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight,
                256, 256);
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        // The original draws no title text (drawGuiContainerForegroundLayer is empty).
        final ArcaneWorkbenchMenu.Overlay overlay = this.menu.overlay();
        if (overlay.recipe() == null) {
            return;
        }

        if (overlay.researchLocked()) {
            // Port addition (the original left the slot silently empty); the
            // acceptance criteria require the locked state to be visible.
            final Component locked = Component.literal("Locked");
            graphics.drawString(this.font, locked,
                    160 - this.font.width(locked) / 2, 84, 0xFF5555, false);
            return;
        }

        final List<Aspect> primals = Aspect.getPrimalAspects();
        for (int i = 0; i < Math.min(primals.size(), ASPECT_LOCS.length); i++) {
            final Aspect primal = primals.get(i);
            final int amount = overlay.effectiveCost() == null
                    ? 0 : overlay.effectiveCost().getAmount(primal);
            if (amount <= 0) {
                continue;
            }
            // Original: tag sprite at (loc-8) with pulsing alpha, full alpha
            // when the wand covers this aspect. Port: colored text, same
            // payable rule (alpha via brightness).
            final int x = ASPECT_LOCS[i][0];
            final int y = ASPECT_LOCS[i][1];
            final int color = 0xFF000000 | primal.getColor();
            final String label = primal.getName();
            graphics.drawString(this.font, label, x - this.font.width(label) / 2, y - 4, color, false);
            graphics.drawString(this.font, String.valueOf(amount),
                    x - this.font.width(String.valueOf(amount)) / 2, y + 5,
                    overlay.payable() ? color : 0x999999, false);
        }

        if (!overlay.payable()) {
            // Original: gray overlay over the result item + literal
            // "Insufficient vis" (the original hard-codes the English string).
            graphics.fill(152, 56, 178, 82, 0x66000000);
            final Component warn = Component.literal("Insufficient vis");
            graphics.drawString(this.font, warn, 168 - this.font.width(warn) / 2, 46, 0xEE4444, false);
        }
    }
}
