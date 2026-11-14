package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.tiles.TileMagicBox;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Magic box menu (content-mapping.csv {@code thaumcraft:gui_magic_box},
 * original {@code ContainerMagicBox}).
 *
 * <p><b>Page model (TC-M7-04-m4).</b> The box inventory is a growable list
 * (27 slots per linked box), and the original container bound exactly
 * <b>three rows of nine</b> box slots at a time —
 * {@code bindBoxInventory(row)} placed slot {@code k + (j + row) * 9} at
 * {@code (8 + k*18, 18 + j*18)}, and {@code refreshInventory} rebound them.
 * This port is faithful to that container model: the menu binds one page of
 * up to 27 box slots, and menu buttons {@link #BUTTON_PAGE_PREV} /
 * {@link #BUTTON_PAGE_NEXT} move the bound window one page at a time by
 * rewriting the bound slots' box-slot indices (1.21.1 has no
 * {@code replaceSlot}; {@code Slot#index} is the public field the original's
 * clear-and-rebind manipulated, so rewriting it is the same operation). The
 * 1.7.10 GUI itself had no paging control — the ClientHandler swapped in a
 * {@code ContainerChest} over every row — so the arrows the screen draws are
 * authored (BD-17). The current page and the box's slot count are data slots,
 * so the screen's arrows, counter, and enabled state are synced, not guessed.</p>
 */
public class MagicBoxMenu extends AbstractContainerMenu {

    /** Menu button that moves the bound window one page back (authored, BD-17). */
    public static final int BUTTON_PAGE_PREV = 2;
    /** Menu button that moves the bound window one page forward (authored, BD-17). */
    public static final int BUTTON_PAGE_NEXT = 3;

    /** Data slot carrying the bound page index. */
    public static final int DATA_PAGE = 0;
    /**
     * Data slot carrying the box's total slot count. The client tile cannot
     * know a cluster's linked-box count on its own (the link count is a
     * server-side flood fill), so the screen's page counter is synced, not
     * derived from a client guess.
     */
    public static final int DATA_BOX_SLOTS = 1;

    private final TileMagicBox box;
    private final int boxSlotCount;
    private int page;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(final int index) {
            return index == DATA_BOX_SLOTS
                    ? MagicBoxMenu.this.boxSlotCount
                    : MagicBoxMenu.this.page;
        }

        @Override
        public void set(final int index, final int value) {
            if (index == DATA_PAGE) {
                // Client side: the synced page drives the screen's counter and
                // arrow enabled-state; slot items arrive through the slot
                // channel, so no rebind is needed on the client.
                MagicBoxMenu.this.page = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public MagicBoxMenu(final int id, final Inventory playerInventory, final TileMagicBox box) {
        super(null, id);
        this.box = box;
        this.boxSlotCount = box.getContainerSize();
        // The original bindBoxInventory(0): three rows of nine, one page.
        final int pageSlots = Math.min(this.boxSlotCount, ContainerLayout.MAGIC_BOX_PAGE_SLOTS);
        for (int i = 0; i < pageSlots; i++) {
            this.addSlot(new Slot(box, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }
        // The bound page's slot count: the rebind rewrites exactly these slots'
        // box-slot indices (Slot#getContainerSlot reads Slot.index).
        this.boxSlotCountRef = pageSlots;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
        addDataSlots(data);
    }

    private final int boxSlotCountRef;

    /** Pages this box spans: 3 rows of 9 per page, at least one. */
    public int getPageCount() {
        return ContainerLayout.magicBoxPageCount(this.boxSlotCount);
    }

    /** The currently bound page (synced to the client through {@link #DATA_PAGE}). */
    public int getCurrentPage() {
        return this.page;
    }

    /** The box's total slot count (synced to the client through {@link #DATA_BOX_SLOTS}). */
    public int getBoxSlotCount() {
        return this.boxSlotCount;
    }

    /** The container data slots of this menu, for probes that prove the sync lands. */
    public ContainerData containerData() {
        return this.data;
    }

    /**
     * The original's rebind: move the window one page and rewrite the bound
     * slots' box-slot indices, clamped to the box's page range.
     */
    private void setPage(final int target) {
        final int next = ContainerLayout.clampPage(target, getPageCount());
        if (next == this.page) {
            return;
        }
        this.page = next;
        final int start = ContainerLayout.magicBoxPageStart(next);
        for (int i = 0; i < this.boxSlotCountRef; i++) {
            this.slots.get(i).index = start + i;
        }
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        if (index < this.boxSlotCountRef) {
            if (!moveItemStackTo(stack, this.boxSlotCountRef, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, this.boxSlotCountRef, false)) {
            return ItemStack.EMPTY;
        }
        // MC 1.21.1 Slot has no onQuickMove hook; the container is already
        // marked changed by Slot#set inside moveItemStackTo.
        return stack.copy();
    }

    // MC 1.21.1 declares clickMenuButton as boolean (true = the button did something).
    @Override
    public boolean clickMenuButton(final Player player, final int buttonId) {
        switch (buttonId) {
            case 1:
                this.box.sort();
                return true;
            case BUTTON_PAGE_PREV:
                setPage(this.page - 1);
                return true;
            case BUTTON_PAGE_NEXT:
                setPage(this.page + 1);
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.box.stillValid(player);
    }
}
