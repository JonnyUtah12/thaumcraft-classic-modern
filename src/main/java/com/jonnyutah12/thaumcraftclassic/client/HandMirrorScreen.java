package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.HandMirrorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The Magic Hand Mirror screen (content-mapping.csv {@code thaumcraft:gui_hand_mirror},
 * original gui_id 16 / {@code GuiHandMirror} + {@code ContainerHandMirror}).
 *
 * <p>Parity target, measured off
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiHandMirror.java}:
 * the original is a plain {@code GuiContainer} at the 1.7.10 default size
 * (176x166) that blits {@code textures/gui/guihandmirror.png} at {@code (0,0)}
 * and overrides nothing else — no foreground drawing, no widgets, no tooltips.
 * {@code ContainerHandMirror} puts its one input slot at {@code (80,24)} over
 * the standard player grid ({@code y = 84} rows, hotbar {@code 142}), which is
 * what {@link HandMirrorMenu} carries verbatim.</p>
 *
 * <p>So this screen is the panel, the player piece and the input slot frame at
 * the original coordinates, and nothing else: the transport itself is
 * {@link HandMirrorMenu}'s server-side per-tick slot diff. Background deviation
 * (the original sheet is overlay-only) is
 * docs/BEHAVIORAL_DEVIATIONS.md BD-17.</p>
 */
@OnlyIn(Dist.CLIENT)
public class HandMirrorScreen extends AbstractContainerScreen<HandMirrorMenu> {

    /** The lang key of the hand-mirror menu (already shipped for the menu type). */
    static final String TITLE_KEY = "container.thaumcraft.gui_hand_mirror";

    public HandMirrorScreen(final HandMirrorMenu menu, final Inventory inventory, final Component title) {
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
        // The single container slot; the player rows are framed by the vanilla piece.
        ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, this.menu.slots.get(HandMirrorMenu.SLOT_INPUT));
    }
}
