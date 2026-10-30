package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.AlchemyFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The alchemical furnace screen (content-mapping.csv
 * {@code thaumcraft:gui_alchemy_furnace}, original gui_id 9 /
 * {@code GuiAlchemyFurnace} + {@code ContainerAlchemyFurnace}).
 *
 * <p>Parity target, measured off
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiAlchemyFurnace.java}:
 * a 1.7.10 default-size panel (176x166) over
 * {@code textures/gui/gui_alchemyfurnace.png}, the two machine slots at
 * {@code (80,8)} and {@code (80,48)}, the 16x20 fire rail at {@code (80,26)},
 * the 9x46 cook rail at {@code (106,13)}, and the 10x55 vis tank at
 * {@code (60,8)} with an 8x48 contents column at {@code (61,12)}. The sheet
 * ships only in the overlay build (D-004), so the vanilla panel is the
 * background and every widget is framed in {@link ScreenPanel} (BD-17).</p>
 *
 * <p>Drawn from synced state: the cook rail fills from the menu's two data
 * slots ({@code furnaceCookTime}/{@code furnaceMaxCookTime}) using the shared
 * bottom-anchored gauge ({@link ContainerLayout#scaledProgress}). The fire
 * rail and the tank's contents column are NOT drawn: this screen rides
 * {@code TileArcaneFurnace} (TC-M4-06's arcane-furnace body), which burns no
 * fuel and holds no 50-unit vis tank — the 1.7.10 body that fed those
 * gauges ({@code TileAlchemyFurnace}) is not ported, and drawing a fake
 * gauge is forbidden; recorded as BD-18.</p>
 */
@OnlyIn(Dist.CLIENT)
public class AlchemyFurnaceScreen extends AbstractContainerScreen<AlchemyFurnaceMenu> {

    /** The lang key of the alchemical furnace window (title of the menu type). */
    static final String TITLE_KEY = "container.thaumcraft.gui_alchemy_furnace";

    /** {@code ContainerAlchemyFurnace}'s machine slots. */
    static final int SLOT_INPUT = 0;
    static final int SLOT_SECONDARY = 1;

    public AlchemyFurnaceScreen(final AlchemyFurnaceMenu menu, final Inventory inventory,
                                final Component title) {
        super(menu, inventory, title.getString().isEmpty() ? Component.translatable(TITLE_KEY) : title);
        this.imageWidth = ContainerLayout.PANEL_WIDTH;
        // The 1.7.10 GuiContainer default panel, which the original keeps.
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
        ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, this.menu.slots.get(SLOT_INPUT));
        ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, this.menu.slots.get(SLOT_SECONDARY));

        // The 9x46 cook rail at (106,13), filled from the menu's synced data
        // slots (the original's getCookProgressScaled(46), bottom-anchored).
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.FURNACE_COOK_X, ContainerLayout.FURNACE_COOK_TOP,
                ContainerLayout.FURNACE_COOK_WIDTH, ContainerLayout.FURNACE_COOK_PIXELS);
        final int cookFill = ContainerLayout.scaledProgress(
                this.menu.getCookTime(), this.menu.getMaxCookTime(),
                ContainerLayout.FURNACE_COOK_PIXELS);
        if (cookFill > 0) {
            ScreenPanel.fillArea(guiGraphics, this.leftPos, this.topPos,
                    ContainerLayout.FURNACE_COOK_X,
                    ContainerLayout.barTop(ContainerLayout.FURNACE_COOK_TOP,
                            ContainerLayout.FURNACE_COOK_PIXELS, cookFill),
                    ContainerLayout.FURNACE_COOK_WIDTH, cookFill, ContainerLayout.FURNACE_COOK_FILL);
        }

        // The 10x55 vis tank at (60,8): frame only. Its 8x48 contents column
        // has no state behind it on this tile (BD-18), and the 16x20 fire at
        // (80,26) likewise (no fuel) — not drawn, per the no-fake rule.
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.FURNACE_TANK_X, ContainerLayout.FURNACE_TANK_TOP,
                ContainerLayout.FURNACE_TANK_WIDTH, ContainerLayout.FURNACE_TANK_HEIGHT);
    }
}
