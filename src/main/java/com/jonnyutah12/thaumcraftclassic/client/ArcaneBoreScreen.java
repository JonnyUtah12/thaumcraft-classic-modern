package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.ArcaneBoreMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The arcane bore screen (content-mapping.csv {@code thaumcraft:gui_arcane_bore},
 * original gui_id 15 / {@code GuiArcaneBore} + {@code ContainerArcaneBore}).
 *
 * <p>Parity target, measured off
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiArcaneBore.java}:
 * the original's short panel (it overrides the 1.7.10 default to 176x141;
 * this screen keeps the vanilla derivation from the container's own player
 * origin at {@code 59}, which gives 142 — the 1 px is the original sheet's
 * rounding, {@link ContainerLayout#BORE_SHEET_HEIGHT}) over
 * {@code textures/gui/gui_arcanebore.png}, the focus slot at {@code (26,18)},
 * the pickaxe slot at {@code (74,18)}, the 16x16 "about to break" icon
 * blitted over the pickaxe slot when {@code damage + 1 >= maxDamage}, and
 * the 0.5-scaled readout block translated to {@code (112,8)}: Width at
 * {@code (0,0)}, Speed at {@code (0,10)}, "Other properties:" at
 * {@code (0,24)}, then the property rows from {@code 34} stepping 9 — Native
 * Clusters in {@code 0xC0C0C0}, Fortune in {@code 0xEEC64A}, Silk Touch in
 * {@code 0x8080FF}. The sheet ships only in the overlay build (D-004), so
 * the vanilla panel is the background and the widgets are framed in
 * {@link ScreenPanel} (BD-17).</p>
 *
 * <p>All readout state is synced: area/maxRadius/speed/fortune ride the
 * menu's four data slots, and the property rows read the pickaxe slot's
 * synced stack — nothing here is faked. The panel geometry difference (142
 * vs 141) is recorded in the docs, not compensated.</p>
 */
@OnlyIn(Dist.CLIENT)
public class ArcaneBoreScreen extends AbstractContainerScreen<ArcaneBoreMenu> {

    /** The lang key of the bore window (the original's literal "Arcane Bore"). */
    static final String TITLE_KEY = "container.thaumcraft.gui_arcane_bore";

    /** {@code ContainerArcaneBore}'s machine slots. */
    static final int SLOT_FOCUS = 0;
    static final int SLOT_PICKAXE = 1;

    public ArcaneBoreScreen(final ArcaneBoreMenu menu, final Inventory inventory,
                            final Component title) {
        super(menu, inventory, title.getString().isEmpty() ? Component.translatable(TITLE_KEY) : title);
        this.imageWidth = ContainerLayout.PANEL_WIDTH;
        // The original overrides the panel to 141; the vanilla derivation from
        // this container's own player origin (59) is 142 (see class doc).
        this.imageHeight = ContainerLayout.panelHeight(ContainerLayout.BORE_PLAYER_GRID_TOP);
        this.titleLabelX = ContainerLayout.titleLabelX();
        this.titleLabelY = ContainerLayout.titleLabelY();
        this.inventoryLabelX = ContainerLayout.titleLabelX();
        this.inventoryLabelY = ContainerLayout.inventoryLabelY(ContainerLayout.BORE_PLAYER_GRID_TOP);
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
        ScreenPanel.drawBackground(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.BORE_PLAYER_GRID_TOP);
        ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, this.menu.slots.get(SLOT_FOCUS));
        ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, this.menu.slots.get(SLOT_PICKAXE));

        // The 16x16 "about to break" icon over the pickaxe slot, gated on the
        // synced pickaxe stack (GuiArcaneBore: damage + 1 >= maxDamage). The
        // original blits gui_arcanebore.png texels, which ship overlay-only
        // (BD-17): the fill is the authored stand-in, the DECON_BAR_FILL
        // precedent.
        final ItemStack pickaxe = this.menu.getPickaxeStack();
        if (!pickaxe.isEmpty()
                && ContainerLayout.borePickaxeNearBroken(pickaxe.getDamageValue(), pickaxe.getMaxDamage())) {
            ScreenPanel.fillArea(guiGraphics, this.leftPos, this.topPos,
                    ContainerLayout.BORE_BROKEN_X, ContainerLayout.BORE_BROKEN_Y,
                    ContainerLayout.BORE_BROKEN_SIZE, ContainerLayout.BORE_BROKEN_SIZE,
                    ContainerLayout.BORE_BROKEN_FILL);
            ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                    ContainerLayout.BORE_BROKEN_X, ContainerLayout.BORE_BROKEN_Y,
                    ContainerLayout.BORE_BROKEN_SIZE, ContainerLayout.BORE_BROKEN_SIZE);
        }

        // The 0.5-scaled readout block at (112,8) — GuiArcaneBore's exact
        // translate/scale, with the Width/Speed/Other/property rows.
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + ContainerLayout.BORE_TEXT_X,
                this.topPos + ContainerLayout.BORE_TEXT_Y, 200.0F);
        guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
        guiGraphics.drawString(this.font, Component.translatable("gui.thaumcraft.arcane_bore.width",
                ContainerLayout.boreWidth(this.menu.getArea(), this.menu.getMaxRadius())),
                0, ContainerLayout.BORE_TEXT_WIDTH_Y, ContainerLayout.BORE_COLOR_TEXT);
        guiGraphics.drawString(this.font, Component.translatable("gui.thaumcraft.arcane_bore.speed",
                this.menu.getSpeed()),
                0, ContainerLayout.BORE_TEXT_SPEED_Y, ContainerLayout.BORE_COLOR_TEXT);
        guiGraphics.drawString(this.font, Component.translatable("gui.thaumcraft.arcane_bore.other"),
                0, ContainerLayout.BORE_TEXT_OTHER_Y, ContainerLayout.BORE_COLOR_TEXT);
        int row = ContainerLayout.BORE_TEXT_ROWS_TOP;
        if (ArcaneBoreMenu.isElementalPickaxe(pickaxe)) {
            guiGraphics.drawString(this.font, Component.translatable("gui.thaumcraft.arcane_bore.native"),
                    0, row, ContainerLayout.BORE_COLOR_NATIVE);
            row += ContainerLayout.BORE_TEXT_ROW_STEP;
        }
        if (this.menu.getFortune() > 0) {
            guiGraphics.drawString(this.font, Component.translatable("gui.thaumcraft.arcane_bore.fortune",
                    this.menu.getFortune()),
                    0, row, ContainerLayout.BORE_COLOR_FORTUNE);
            row += ContainerLayout.BORE_TEXT_ROW_STEP;
        }
        if (this.menu.hasSilkTouch(pickaxe)) {
            guiGraphics.drawString(this.font, Component.translatable("gui.thaumcraft.arcane_bore.silk"),
                    0, row, ContainerLayout.BORE_COLOR_SILK);
        }
        guiGraphics.pose().popPose();
    }
}
