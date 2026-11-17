package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.SpaMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.List;

/**
 * The Arcane Spa screen (content-mapping.csv {@code thaumcraft:gui_spa},
 * original gui_id 19 / {@code GuiSpa} + {@code ContainerSpa}).
 *
 * <p>Parity target, measured off
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiSpa.java} and
 * its {@code TileSpa.renderIsFluidInWorld} tank-rail pass: a 1.7.10
 * default-size panel (176x166) over {@code textures/gui/gui_spa.png}, the
 * bath-salt slot at {@code (65,31)}, the 10x55 tank frame at {@code (106,11)}
 * with the fluid column at {@code (107,15)} up to 48 px — filled from the
 * bottom with {@code bar = fluidAmount / capacity} — and an 8x8 mix indicator
 * at {@code (89,35)}. Clicking the mix box sends <b>menu button 1</b>
 * ({@code handleInventoryButtonClick(containerId, 1)}), which is
 * {@link SpaMenu#clickMenuButton} toggling {@code TileSpa.toggleMix}, and plays
 * {@code thaumcraft:cameraclack}. Hovering the mix box shows the tank fluid's
 * name and hovering the tank shows "name X / 5000 mB" — both boxes measured
 * off the original's own tooltip hitboxes.</p>
 *
 * <p>The original read the tile directly for the fluid and {@code getMix()}; a
 * 1.21.1 screen gets only the menu, so the tank (fluid id, amount, capacity)
 * and the mix flag now travel in the menu's data slots (TC-M7-04-m4, BD-17
 * closure). The original's fluid column is a stitched {@code fluid.png} sprite
 * from an overlay-only atlas, so the port fills the column with the fluid
 * type's tint from {@link IClientFluidTypeExtensions} — the modern,
 * authored-safe equivalent of "the fluid's colour" — and the mix-on indicator
 * is the same authored fill colour the break bar uses, framed in its off
 * state.</p>
 */
@OnlyIn(Dist.CLIENT)
public class SpaScreen extends AbstractContainerScreen<SpaMenu> {

    /** The lang key of the arcane-spa menu (already shipped for the menu type). */
    static final String TITLE_KEY = "container.thaumcraft.gui_spa";

    /** Original {@code ContainerSpa}: the menu button that toggles the mix. */
    static final int BUTTON_TOGGLE_MIX = 1;

    /** The original 8x8 mix box the click is bound to. */
    static final int MIX_X = ContainerLayout.SPA_MIX_X;
    static final int MIX_Y = ContainerLayout.SPA_MIX_Y;
    static final int MIX_SIZE = ContainerLayout.SPA_MIX_SIZE;

    /** Original {@code ContainerSpa}: the only container slot. */
    static final int SLOT_BATH_SALT = 0;

    /** Authored stand-in for the original fluid-stitch colour of a tintless fluid. */
    static final int FLUID_FALLBACK_TINT = 0xFF3F76E4;

    public SpaScreen(final SpaMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title.getString().isEmpty() ? Component.translatable(TITLE_KEY) : title);
        this.imageWidth = ContainerLayout.PANEL_WIDTH;
        this.imageHeight = ContainerLayout.panelHeight(ContainerLayout.PLAYER_GRID_TOP);
        this.titleLabelX = ContainerLayout.titleLabelX();
        this.titleLabelY = ContainerLayout.titleLabelY();
        this.inventoryLabelX = ContainerLayout.titleLabelX();
        this.inventoryLabelY = ContainerLayout.inventoryLabelY(ContainerLayout.PLAYER_GRID_TOP);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY,
                       final float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        // The original set these tooltip strings inside its click handler
        // (func_73864_a); the modern tooltip surface is set per-frame during
        // render, same boxes, same content.
        final Fluid tank = this.menu.getTankFluid();
        if (tank != null) {
            final int relX = mouseX - this.leftPos;
            final int relY = mouseY - this.topPos;
            if (inBox(relX, relY, ContainerLayout.SPA_MIX_TIP_X, ContainerLayout.SPA_MIX_TIP_Y,
                    ContainerLayout.SPA_MIX_TIP_SIZE, ContainerLayout.SPA_MIX_TIP_SIZE)) {
                guiGraphics.renderTooltip(this.font,
                        List.of(tankName(tank).getVisualOrderText()), mouseX, mouseY);
            } else if (inBox(relX, relY, ContainerLayout.SPA_TANK_TIP_X, ContainerLayout.SPA_TANK_TIP_Y,
                    ContainerLayout.SPA_TANK_TIP_WIDTH, ContainerLayout.SPA_TANK_TIP_HEIGHT)) {
                guiGraphics.renderTooltip(this.font,
                        List.of(Component.literal(tankAmountText()).getVisualOrderText()), mouseX, mouseY);
            }
        }
    }

    private static boolean inBox(final int x, final int y, final int boxX, final int boxY,
                                 final int width, final int height) {
        return x >= boxX && y >= boxY && x < boxX + width && y < boxY + height;
    }

    /** The tank fluid's display name: the fluid type's translation key. */
    static Component tankName(final Fluid tank) {
        return Component.translatable(tank.getFluidType().getDescriptionId());
    }

    /** The tank tooltip text: "Name amount / capacity mB" (authored formatting, BD-17). */
    String tankAmountText() {
        return tankName(this.menu.getTankFluid()).getString()
                + " " + this.menu.getTankAmount() + " / " + this.menu.getTankCapacity() + " mB";
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick,
                            final int mouseX, final int mouseY) {
        ScreenPanel.drawBackground(guiGraphics, this.leftPos, this.topPos, ContainerLayout.PLAYER_GRID_TOP);
        ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, this.menu.slots.get(SLOT_BATH_SALT));

        // The original tank frame at (106,11) 10x55 and its fluid column at
        // (107,15): bar = fluidAmount / capacity, floor-scaled to 48 px, filled
        // from the bottom (TileSpa.renderIsFluidInWorld: (int)(48 - 48*bar) of
        // rail left blank above the fill).
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.SPA_TANK_X, ContainerLayout.SPA_TANK_TOP,
                ContainerLayout.SPA_TANK_WIDTH, ContainerLayout.SPA_TANK_HEIGHT);
        final Fluid tank = this.menu.getTankFluid();
        final int fill = ContainerLayout.scaledFluidHeight(this.menu.getTankAmount(),
                this.menu.getTankCapacity(), ContainerLayout.SPA_FLUID_PIXELS);
        if (tank != null && fill > 0) {
            final int top = ContainerLayout.fluidTop(ContainerLayout.SPA_FLUID_TOP,
                    ContainerLayout.SPA_FLUID_PIXELS, fill);
            guiGraphics.fill(this.leftPos + ContainerLayout.SPA_FLUID_X, this.topPos + top,
                    this.leftPos + ContainerLayout.SPA_FLUID_X + ContainerLayout.SPA_FLUID_WIDTH,
                    this.topPos + top + fill, fluidTint(tank));
        }

        // The original 8x8 mix indicator at (89,35): the on-state texel is
        // overlay-only (BD-17), so on fills with the authored indicator colour
        // and off keeps the framed box — the two states the original showed.
        if (this.menu.getMix()) {
            guiGraphics.fill(this.leftPos + MIX_X, this.topPos + MIX_Y,
                    this.leftPos + MIX_X + MIX_SIZE, this.topPos + MIX_Y + MIX_SIZE,
                    ContainerLayout.SPA_MIX_ON_FILL);
        } else {
            ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos, MIX_X, MIX_Y, MIX_SIZE, MIX_SIZE);
        }
    }

    /** The fluid type's client tint, falling back to the authored water tone. */
    static int fluidTint(final Fluid tank) {
        final int tint = IClientFluidTypeExtensions.of(tank).getTintColor();
        return (tint >>> 24) < 16 ? FLUID_FALLBACK_TINT : tint;
    }

    /** The original {@code func_73864_a}: the {@code (89,35)} 8x8 box sends menu button 1. */
    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        final int relX = (int) mouseX - (this.leftPos + MIX_X);
        final int relY = (int) mouseY - (this.topPos + MIX_Y);
        if (button == 0 && relX >= 0 && relY >= 0 && relX < MIX_SIZE && relY < MIX_SIZE) {
            if (this.minecraft != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, BUTTON_TOGGLE_MIX);
            }
            return true;
        }
        return false;
    }
}
