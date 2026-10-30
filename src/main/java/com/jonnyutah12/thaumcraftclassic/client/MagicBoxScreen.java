package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.MagicBoxMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The Magic Box screen (content-mapping.csv {@code thaumcraft:gui_magic_box},
 * original gui_id 18 / {@code GuiMagicBox} + {@code ContainerMagicBox}).
 *
 * <p>Parity target, measured off
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiMagicBox.java}:
 * the original does <i>not</i> use a Thaumcraft sheet at all — it is a chest
 * screen over vanilla {@code textures/gui/container/generic_54.png}, blitting
 * {@code (0,0,176,71)} for the box rows and {@code (0,126,176,96)} at
 * {@code y = 71} for the player grid, i.e. a 168 px panel for 3 box rows.
 * {@code ContainerMagicBox} binds three rows of nine box slots at
 * {@code y = 18 + row*18} and its player grid at {@code y = 103} (hotbar 161).
 * This port's {@link MagicBoxMenu} keeps the box rows at the original
 * {@code y = 18 + row*18}, but puts the player grid at the original's own
 * default 84/142 (the shared player origin every other container here uses),
 * so the panel is derived from 84 as every screen in this crate is.</p>
 *
 * <p><b>Known deviation (BD-17).</b> The box contents are a growable list and
 * {@link MagicBoxMenu} exposes up to {@code 27 * 8} slots for a linked cluster,
 * while the original rebound the container one 3-row page at a time
 * ({@code ContainerMagicBox.bindBoxInventory(row)} + {@code refreshInventory}).
 * A panel this shape holds one 3-row page, so only the slots that fall inside
 * the machine region are framed ({@link ContainerLayout#fitsInMachineRegion});
 * the rest stay live in the menu but unframed, and the paging/rebinding work is
 * a follow-up card rather than scope creep here.</p>
 */
@OnlyIn(Dist.CLIENT)
public class MagicBoxScreen extends AbstractContainerScreen<MagicBoxMenu> {

    /** The lang key of the magic-box menu (already shipped for the menu type). */
    static final String TITLE_KEY = "container.thaumcraft.gui_magic_box";

    /** The 36 player slots {@link MagicBoxMenu} appends after the box grid. */
    static final int PLAYER_SLOT_COUNT = 36;

    private final int machineRegionHeight;

    public MagicBoxScreen(final MagicBoxMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title.getString().isEmpty() ? Component.translatable(TITLE_KEY) : title);
        this.imageWidth = ContainerLayout.PANEL_WIDTH;
        this.imageHeight = ContainerLayout.panelHeight(ContainerLayout.PLAYER_GRID_TOP);
        this.titleLabelX = ContainerLayout.titleLabelX();
        this.titleLabelY = ContainerLayout.titleLabelY();
        this.inventoryLabelX = ContainerLayout.titleLabelX();
        this.inventoryLabelY = ContainerLayout.inventoryLabelY(ContainerLayout.PLAYER_GRID_TOP);
        this.machineRegionHeight = ContainerLayout.playerPanelTop(ContainerLayout.PLAYER_GRID_TOP);
    }

    /** How many of the menu's slots belong to the box rather than to the player. */
    int boxSlotCount() {
        return Math.max(0, this.menu.slots.size() - PLAYER_SLOT_COUNT);
    }

    /** How many 3-row pages the box currently spans (the original's one page is 1). */
    int boxRows() {
        return ContainerLayout.gridRows(boxSlotCount());
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
        final int boxSlots = boxSlotCount();
        for (int i = 0; i < boxSlots; i++) {
            final Slot slot = this.menu.slots.get(i);
            if (!ContainerLayout.fitsInMachineRegion(slot.y, this.machineRegionHeight)) {
                continue; // a page past the panel's box region (BD-17)
            }
            ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, slot);
        }
    }
}
