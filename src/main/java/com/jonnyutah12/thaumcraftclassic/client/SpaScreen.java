package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.SpaMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The Arcane Spa screen (content-mapping.csv {@code thaumcraft:gui_spa},
 * original gui_id 19 / {@code GuiSpa} + {@code ContainerSpa}).
 *
 * <p>Parity target, measured off
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiSpa.java}:
 * a 1.7.10 default-size panel (176x166) over
 * {@code textures/gui/gui_spa.png}, the bath-salt slot at {@code (65,31)}, the
 * 10x55 tank at {@code (106,11)} with a fluid column at {@code (107,15)} up to
 * 48 px, and an 8x8 mix indicator at {@code (89,35)} — clicking that box sends
 * <b>menu button 1</b> ({@code handleInventoryButtonClick(containerId, 1)}),
 * which is {@link SpaMenu#clickMenuButton} toggling {@code TileSpa.toggleMix},
 * and plays {@code thaumcraft:cameraclack}. The original read the tile directly
 * for both the fluid level and {@code getMix()}; a 1.21.1 screen gets only the
 * menu, and this menu has no data slots, so neither state is client-visible
 * here (BD-17).</p>
 *
 * <p>What this screen draws: the panel and player piece, the bath-salt slot
 * frame, the tank frame at the original coordinates, and the frame of the 8x8
 * mix box — so the toggle sits exactly where the original put it.</p>
 */
@OnlyIn(Dist.CLIENT)
public class SpaScreen extends AbstractContainerScreen<SpaMenu> {

    /** The lang key of the arcane-spa menu (already shipped for the menu type). */
    static final String TITLE_KEY = "container.thaumcraft.gui_spa";

    /** Original {@code ContainerSpa}: the menu button that toggles the mix. */
    static final int BUTTON_TOGGLE_MIX = 1;

    /** The original 8x8 mix box the click is bound to. */
    static final int MIX_X = 89;
    static final int MIX_Y = 35;
    static final int MIX_SIZE = 8;

    /** Original {@code ContainerSpa}: the only container slot. */
    static final int SLOT_BATH_SALT = 0;

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
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick,
                            final int mouseX, final int mouseY) {
        ScreenPanel.drawBackground(guiGraphics, this.leftPos, this.topPos, ContainerLayout.PLAYER_GRID_TOP);
        ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, this.menu.slots.get(SLOT_BATH_SALT));

        // The original tank frame; its fluid column needs the tank synced, which
        // this menu does not do (BD-17).
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.SPA_TANK_X, ContainerLayout.SPA_TANK_TOP,
                ContainerLayout.SPA_TANK_WIDTH, ContainerLayout.SPA_TANK_HEIGHT);
        // The original 8x8 mix box: the toggle's hitbox, framed because the
        // mix state itself is not synced (BD-17).
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos, MIX_X, MIX_Y, MIX_SIZE, MIX_SIZE);
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
