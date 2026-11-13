package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.PechMenu;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Pech trade window screen (content-mapping.csv {@code Thaumcraft:GUI_PECH} →
 * {@code thaumcraft:gui_pech}, original gui_id 1, {@code client/gui/GuiPech.java},
 * 81 lines). TC-M7-04-m3-b.
 *
 * <p>The window is <b>175x232</b> — {@code GuiPech.java:19-20}
 * ({@code field_146999_f = 175}, {@code field_147000_g = 232}); it is the one
 * authored panel in the port that is NOT the 176-wide vanilla width, and the
 * 232 height is the panel height, not the art height: the slot rows end at the
 * 142 hotbar ({@code ContainerPech.java:48}) and the ~76 px below them carried
 * only the original's sheet art. 1.21.1 draws the vanilla panel at the original
 * coordinates (D-004 / BD-37), so the port's panel is the vanilla height derived
 * from the 84 px player grid, and the sheet-only art region is BD-71.</p>
 *
 * <p>The 25x25 dice overlay sits at (67, 24) ({@code GuiPech.java:40}), blitted
 * in the original from {@code textures/gui/gui_pech.png} at sheet coordinate
 * (176, 0) and drawn only when the predicate holds — slot 0 non-empty, slots
 * 1..4 all empty, and {@code pech.isValued(offer)} ({@code GuiPech.java:34-39}).
 * That sheet ships only in the asset-overlay build (D-004 / BD-37), so the landed
 * screen draws the same rect as a framed vanilla-toned well, lit while the
 * predicate bit is set; the predicate is computed on the server and arrives
 * through the menu's 1-int {@code ContainerData} slot (see {@link PechMenu} for
 * why the port needs one and the original did not). Clicking inside the rect
 * plays the pech_dice cue client-side ({@code GuiPech.java:63,67-80}: 0.5 volume,
 * {@code 0.95 + rand*0.1} pitch) and sends the original's button id 0
 * ({@code GuiPech.java:62} → {@code ContainerPech.java:64-70}). The server side
 * of that click is the unported loot-table roll ({@code ContainerPech.java:83-159},
 * D-007), so nothing is consumed and no contents appear (BD-71).</p>
 *
 * <p>{@code GuiPech.java:24-25} — {@code drawScreen}'s foreground layer is empty:
 * the original draws <b>no</b> title and no "Inventory" label, because both are
 * baked into the 175x232 sheet. The landed screen uses the house posture of the
 * other GUI screens (vanilla labels over the vanilla panel), which is part of the
 * BD-37 sheet substitution.</p>
 */
@OnlyIn(Dist.CLIENT)
public class PechScreen extends AbstractContainerScreen<PechMenu> {

    /** The title fallback: the menu type's own shipped key (D-001 id
     *  {@code thaumcraft:gui_pech}); the packet's title normally carries it. */
    static final String TITLE_KEY = "container.thaumcraft.gui_pech";

    /** {@code GuiPech.java:28} — {@code UtilsFX.bindTexture("textures/gui/gui_pech.png")}.
     *  The sheet ships only in the asset-overlay build (D-004 / BD-37), so this is
     *  the citation for the substitution, not a blit target: the landed screen
     *  references no original texel. */
    @SuppressWarnings("unused")
    private static final ResourceLocation ORIGINAL_SHEET =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_pech.png");

    public PechScreen(final PechMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title.getString().isEmpty() ? Component.translatable(TITLE_KEY) : title);
        this.imageWidth = ContainerLayout.PECH_WINDOW_WIDTH;                                // GuiPech.java:19
        this.imageHeight = ContainerLayout.PECH_WINDOW_HEIGHT;                              // GuiPech.java:20
        this.inventoryLabelY = ContainerLayout.PECH_PLAYER_GRID_TOP - 12;                   // ContainerPech.java:43
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTicks, final int mouseX,
            final int mouseY) {
        final int left = this.leftPos;
        final int top = this.topPos;
        final int playerPanelTop = ContainerLayout.playerPanelTop(ContainerLayout.PECH_PLAYER_GRID_TOP);
        final int panelHeight = ContainerLayout.panelHeight(ContainerLayout.PECH_PLAYER_GRID_TOP);
        // Vanilla panel at the 175-wide window: fill, outline, player piece (BD-37).
        guiGraphics.fill(left, top, left + ContainerLayout.PECH_WINDOW_WIDTH, top + playerPanelTop,
                ContainerLayout.PANEL_FILL);
        guiGraphics.renderOutline(left, top, ContainerLayout.PECH_WINDOW_WIDTH, panelHeight,
                ContainerLayout.PANEL_FRAME);
        guiGraphics.blit(ContainerLayout.PLAYER_PANEL, left, top + playerPanelTop,
                ContainerLayout.PLAYER_PANEL_U, ContainerLayout.PLAYER_PANEL_V,
                ContainerLayout.PECH_WINDOW_WIDTH, ContainerLayout.PLAYER_PANEL_HEIGHT);
        for (final Slot slot : this.menu.slots) {
            if (slot.index < 5) {                                                           // ContainerPech.java:33,37
                ScreenPanel.frameSlot(guiGraphics, left, top, slot);
            }
        }
        // GuiPech.java:34-40 — the 25x25 dice well at (67, 24). The original blits
        // gui_pech.png (176, 0), an overlay-only sheet (D-004 / BD-37), and only while
        // the predicate holds; the landed frame is the vanilla-toned well, lit by the
        // synced predicate bit (BD-71).
        ScreenPanel.frameArea(guiGraphics, left, top, ContainerLayout.PECH_DICE_X, ContainerLayout.PECH_DICE_Y,
                ContainerLayout.PECH_DICE_SIZE, ContainerLayout.PECH_DICE_SIZE);
        if (this.menu.diceActive()) {
            guiGraphics.renderOutline(left + ContainerLayout.PECH_DICE_X, top + ContainerLayout.PECH_DICE_Y,
                    ContainerLayout.PECH_DICE_SIZE, ContainerLayout.PECH_DICE_SIZE, ContainerLayout.DECON_BAR_FILL);
        }
    }

    /**
     * {@code GuiPech.java:46-65} — the original runs the container's own click
     * handling first ({@code :47}), then tests the 25x25 dice rect at (67, 24)
     * against the same predicate that lights the overlay, and on a hit sends
     * button id 0 ({@code :62}) and plays the pech_dice cue ({@code :63,67-80}:
     * 0.5 volume, {@code 0.95 + rand*0.1} pitch). The landed screen gates on the
     * synced predicate bit (the server's copy of the same predicate) and sends the
     * same button id; the server accepts no roll, so the sound and the lit well
     * are the shipped half of the original's click (BD-71).
     */
    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final boolean handled = super.mouseClicked(mouseX, mouseY, button);                 // GuiPech.java:47
        // GuiPech.java:50-61 tests only the rect and the predicate — it never
        // inspects which mouse button was pressed, so neither do we.
        if (mouseX >= this.leftPos + ContainerLayout.PECH_DICE_X
                && mouseX < this.leftPos + ContainerLayout.PECH_DICE_X + ContainerLayout.PECH_DICE_SIZE
                && mouseY >= this.topPos + ContainerLayout.PECH_DICE_Y
                && mouseY < this.topPos + ContainerLayout.PECH_DICE_Y + ContainerLayout.PECH_DICE_SIZE
                && this.menu.diceActive()) {                                                // GuiPech.java:52-61
            final var player = this.minecraft.player;
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                    ModSounds.PECH_DICE.get(), SoundSource.PLAYERS, 0.5f,
                    0.95f + player.getRandom().nextFloat() * 0.1f);                         // GuiPech.java:75-77
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, PechMenu.DICE_BUTTON);     // GuiPech.java:62
            return true;
        }
        return handled;
    }
}
