package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.FocusPouchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The focus-pouch screen (content-mapping.csv {@code thaumcraft:gui_focus_pouch},
 * original gui_id 5 / {@code GuiFocusPouch} + {@code ContainerFocusPouch}).
 *
 * <p>Parity target, measured off
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiFocusPouch.java}:
 * a <b>175x232</b> panel ({@code guiWidth}/{@code guiHeight}, lines 19-20 — the
 * only non-default panel size in this GUI family) blitting
 * {@code textures/gui/gui_focuspouch.png} at {@code (0,0)}, 18 focus slots in a
 * 6x3 grid at {@code (37 + i%6*18, 51 + i/6*18)} and a player grid at
 * {@code 151}/{@code 209} from {@code ContainerFocusPouch}, plus one foreground
 * draw: the 16x16 marker at {@code (8 + currentItem*18, 209)} that marks the
 * hotbar slot carrying the pouch ({@code func_146979_b}, lines 23-32, drawn at
 * z = 200 over the slot). It overrides no button and sends no menu button
 * ({@code func_146983_a} returns false).</p>
 *
 * <p>What this screen draws: the panel at the original 175x232 (the vanilla
 * player piece cropped to it, BD-18), the 18 vanilla slot frames at the
 * original coordinates, and the frame of the carrier-slot marker box at the
 * original coordinates — the original marker texels ship only in the
 * asset-overlay build (D-004, BD-17). The "close when the pouch leaves that
 * slot" rule the original drew client-side is
 * {@link FocusPouchMenu#stillValid(net.minecraft.world.entity.player.Player)}
 * here (BD-18), so nothing in this class needs to observe it.</p>
 */
@OnlyIn(Dist.CLIENT)
public class FocusPouchScreen extends AbstractContainerScreen<FocusPouchMenu> {

    /** The lang key of the focus-pouch menu type. */
    static final String TITLE_KEY = "container.thaumcraft.gui_focus_pouch";

    public FocusPouchScreen(final FocusPouchMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title.getString().isEmpty() ? Component.translatable(TITLE_KEY) : title);
        this.imageWidth = ContainerLayout.POUCH_PANEL_WIDTH;
        this.imageHeight = ContainerLayout.POUCH_PANEL_HEIGHT;
        this.titleLabelX = ContainerLayout.titleLabelX();
        this.titleLabelY = ContainerLayout.titleLabelY();
        this.inventoryLabelX = ContainerLayout.titleLabelX();
        this.inventoryLabelY = ContainerLayout.inventoryLabelY(ContainerLayout.POUCH_PLAYER_GRID_TOP);
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
                ContainerLayout.POUCH_PLAYER_GRID_TOP,
                ContainerLayout.POUCH_PANEL_WIDTH, ContainerLayout.POUCH_PANEL_HEIGHT);

        // The 18 focus slots, at ContainerFocusPouch's own coordinates.
        for (int slot = 0; slot < FocusPouchMenu.POUCH_SLOTS; slot++) {
            ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, this.menu.slots.get(slot));
        }

        // The carrier-slot marker box: (8 + currentItem*18, 209), 16x16, exactly
        // the slot's item anchor. The original blits a texel here; the frame is
        // the code-only stand-in (BD-17/BD-18).
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.pouchMarkerX(this.menu.carrierHotbarSlot()),
                ContainerLayout.POUCH_HOTBAR_TOP,
                ContainerLayout.POUCH_MARKER_SIZE, ContainerLayout.POUCH_MARKER_SIZE);
    }
}
