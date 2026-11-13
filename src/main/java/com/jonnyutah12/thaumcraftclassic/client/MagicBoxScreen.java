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
 * <p><b>Page model (TC-M7-04-m4, BD-17).</b> The box contents are a growable
 * list, and the original container bound exactly one 3-row page at a time
 * ({@code bindBoxInventory(row)} + {@code refreshInventory}). This screen
 * shows that page: {@link MagicBoxMenu} binds up to 27 box slots, the frame
 * covers exactly the bound slots, and two authored 10x10 arrows in the title
 * band send {@link MagicBoxMenu#BUTTON_PAGE_PREV} /
 * {@link MagicBoxMenu#BUTTON_PAGE_NEXT} — the container-rebind model the
 * original container itself used (its GUI had no paging control; the
 * ClientHandler swapped in a {@code ContainerChest}, which this port does not
 * port). A "page/total" counter sits between the title and the arrows; both
 * numbers are menu data slots, so the client displays synced state, not a
 * guess. A one-page box shows a counter and no live arrows.</p>
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
                continue; // a slot outside the panel's box region cannot be framed
            }
            ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, slot);
        }

        // The authored page widget (BD-17: the original GUI had no paging
        // control; this port binds one 3-row page like the original container
        // and moves it with menu buttons). Prev is framed-dimmed at page 0 and
        // next at the last page; the counter is the synced page and the synced
        // slot count, not a client guess.
        final int pageCount = this.menu.getPageCount();
        final int page = ContainerLayout.clampPage(this.menu.getCurrentPage(), pageCount);
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.MAGIC_BOX_PAGE_PREV_X, ContainerLayout.MAGIC_BOX_PAGE_PREV_Y,
                ContainerLayout.MAGIC_BOX_PAGE_BUTTON_SIZE, ContainerLayout.MAGIC_BOX_PAGE_BUTTON_SIZE);
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.MAGIC_BOX_PAGE_NEXT_X, ContainerLayout.MAGIC_BOX_PAGE_NEXT_Y,
                ContainerLayout.MAGIC_BOX_PAGE_BUTTON_SIZE, ContainerLayout.MAGIC_BOX_PAGE_BUTTON_SIZE);
        guiGraphics.drawString(this.font, pageLabel(page, pageCount),
                this.leftPos + ContainerLayout.MAGIC_BOX_PAGE_LABEL_X,
                this.topPos + ContainerLayout.MAGIC_BOX_PAGE_LABEL_Y,
                ContainerLayout.MAGIC_BOX_PAGE_LABEL_COLOR, false);
    }

    /** The page counter text, e.g. {@code 2/3}; a one-page box shows {@code 1/1}. */
    static Component pageLabel(final int page, final int pageCount) {
        return Component.literal((page + 1) + "/" + pageCount);
    }

    /** The page a click on the arrow widget moves to, or -1 (the click passes through). */
    int pageButtonAt(final int relX, final int relY) {
        if (inButton(relX, relY, ContainerLayout.MAGIC_BOX_PAGE_PREV_X)) {
            return this.menu.getCurrentPage() - 1;
        }
        if (inButton(relX, relY, ContainerLayout.MAGIC_BOX_PAGE_NEXT_X)) {
            return this.menu.getCurrentPage() + 1;
        }
        return -1;
    }

    private static boolean inButton(final int relX, final int relY, final int buttonX) {
        return relY >= ContainerLayout.MAGIC_BOX_PAGE_NEXT_Y
                && relY < ContainerLayout.MAGIC_BOX_PAGE_NEXT_Y + ContainerLayout.MAGIC_BOX_PAGE_BUTTON_SIZE
                && relX >= buttonX
                && relX < buttonX + ContainerLayout.MAGIC_BOX_PAGE_BUTTON_SIZE;
    }

    /**
     * The arrows (authored, BD-17): each sends its page button; the menu
     * clamps, so a click past an end is a no-op server-side.
     */
    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || this.minecraft == null) {
            return false;
        }
        final int relX = (int) mouseX - this.leftPos;
        final int relY = (int) mouseY - this.topPos;
        final int target = pageButtonAt(relX, relY);
        if (target == this.menu.getCurrentPage()) {
            return true; // a press on a clamped end still consumes the click
        }
        if (target >= 0) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                    target < this.menu.getCurrentPage()
                            ? MagicBoxMenu.BUTTON_PAGE_PREV
                            : MagicBoxMenu.BUTTON_PAGE_NEXT);
            return true;
        }
        return false;
    }
}
