package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.DeconstructionTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The Deconstruction Table screen (content-mapping.csv
 * {@code thaumcraft:gui_decon_table}, original gui_id 8 /
 * {@code GuiDeconstructionTable} + {@code ContainerDeconstructionTable}).
 *
 * <p>Parity target, measured off
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiDeconstructionTable.java}:
 * a 1.7.10 default-size panel (176x166) blitting
 * {@code textures/gui/gui_decontable.png}, the input slot at {@code (64,16)},
 * a break bar that drains downward at {@code x = 93, y = 15} with
 * {@code getBreakTimeScaled(46)} px of a 46 px rail, and a 16x16 aspect box at
 * {@code (64,48)}. Clicking that box sends <b>menu button 1</b>
 * ({@code handleInventoryButtonClick(containerId, 1)}), which is
 * {@link DeconstructionTableMenu#clickMenuButton} adding the revealed aspect to
 * the player's aspect pool, and plays {@code thaumcraft:hhoff}. The original
 * could read the tile's {@code aspect} field directly because it held the tile;
 * 1.21.1 hands a screen only the menu, so the revealed aspect reaches it through
 * the menu's container data (BD-17, closed by
 * {@link DeconstructionTableMenu#getRevealedAspect()}).</p>
 *
 * <p>What this screen draws: the panel and player piece, the input slot frame,
 * the 46 px break bar rail with the synced break-time fill
 * ({@code menu.getBreakTime()} → the menu's data slot 0, the original's own
 * sync channel), and at {@code (64,48)} the original's tag — the 16x16 box is
 * framed (its sheet art is overlay-only, D-004) and {@link AspectTagRenderer}
 * draws the revealed aspect into it exactly where
 * {@code GuiDeconstructionTable.java:39} did, now that the aspect reaches this
 * menu as data slot 1 (BD-17 closed for the icon: see
 * {@link DeconstructionTableMenu#getRevealedAspect()}).</p>
 */
@OnlyIn(Dist.CLIENT)
public class DeconstructionTableScreen extends AbstractContainerScreen<DeconstructionTableMenu> {

    /** The lang key of the deconstruction-table menu (already shipped for the menu type). */
    static final String TITLE_KEY = "container.thaumcraft.gui_decon_table";

    /** Original {@code ContainerDeconstructionTable}: the menu button that collects. */
    static final int BUTTON_COLLECT_ASPECT = 1;

    /** Original {@code ContainerDeconstructionTable}: the only container slot. */
    static final int SLOT_INPUT = 0;

    public DeconstructionTableScreen(final DeconstructionTableMenu menu, final Inventory inventory,
                                     final Component title) {
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
        ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, this.menu.slots.get(SLOT_INPUT));

        // The original break bar: a 46 px rail that drains downward, filled by
        // getBreakTimeScaled(46) = breaktime * 46 / 40.
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.DECON_BAR_X, ContainerLayout.DECON_BAR_TOP,
                ContainerLayout.DECON_BAR_WIDTH, ContainerLayout.DECON_BAR_PIXELS);
        final int fill = ContainerLayout.scaledBarHeight(this.menu.getBreakTime(),
                ContainerLayout.DECON_BAR_PIXELS);
        if (fill > 0) {
            final int barTop = ContainerLayout.barTop(ContainerLayout.DECON_BAR_TOP,
                    ContainerLayout.DECON_BAR_PIXELS, fill);
            guiGraphics.fill(this.leftPos + ContainerLayout.DECON_BAR_X,
                    this.topPos + barTop,
                    this.leftPos + ContainerLayout.DECON_BAR_X + ContainerLayout.DECON_BAR_WIDTH,
                    this.topPos + barTop + fill,
                    ContainerLayout.DECON_BAR_FILL);
        }

        // The original's aspect box: the 176x166 sheet's frame at (64,48) is
        // ARR-overlay (D-004), so the port frames the region, then draws the tag
        // the original drew inside it — GuiDeconstructionTable.java:38-39, inside a
        // `if (this.tableInventory.aspect != null)` guard:
        //   UtilsFX.drawTag(k + 64, l + 48, this.tableInventory.aspect, 0.0F, 0, this.field_73735_i);
        // That is the SIX-argument overload (UtilsFX.java:802-804), which forwards
        // blend 771, alpha 1.0F and bw = false — the full aspect colour at full
        // alpha, NOT the 3-argument overload's black-and-white tag (that one is the
        // in-world tile renderers': TileAlembicRenderer.java, TileJarRenderer.java).
        // The (64,48) here is the box top-left: this caller does NOT apply the
        // workbench's -8 point shift, so the port passes the box coordinates through.
        // The revealed aspect reaches this menu as data slot 1 (BD-17), so an empty
        // box stays the original's empty box and a revealed one draws its tag.
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.DECON_ASPECT_X, ContainerLayout.DECON_ASPECT_Y,
                ContainerLayout.DECON_ASPECT_SIZE, ContainerLayout.DECON_ASPECT_SIZE);
        AspectTagRenderer.drawTag(guiGraphics, this.font,
                this.leftPos + ContainerLayout.DECON_ASPECT_X,
                this.topPos + ContainerLayout.DECON_ASPECT_Y,
                this.menu.getRevealedAspect(), 0.0F, 0);
    }

    /**
     * The original {@code func_73864_a}: super handles the slots, then the
     * {@code (64,48)} 16x16 box sends menu button 1. Same hitbox, same button, same
     * order. {@link DeconstructionTableMenu#clickMenuButton} is the authority — it
     * pays only when the tile holds an aspect — and the send is additionally gated
     * on the synced aspect (BD-17), so an empty box does not raise a no-op packet
     * on a dedicated server, where {@code ServerPacketListeners} logs a menu
     * exception for it. The gate reads the same state the button validates, so it
     * cannot desync the pay: the click that reaches a loaded aspect still pays.
     */
    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        final int relX = (int) mouseX - (this.leftPos + ContainerLayout.DECON_ASPECT_X);
        final int relY = (int) mouseY - (this.topPos + ContainerLayout.DECON_ASPECT_Y);
        if (button == 0 && relX >= 0 && relY >= 0
                && relX < ContainerLayout.DECON_ASPECT_SIZE && relY < ContainerLayout.DECON_ASPECT_SIZE) {
            if (this.minecraft != null && this.menu.getRevealedAspect() != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, BUTTON_COLLECT_ASPECT);
            }
            return true;
        }
        return false;
    }
}
