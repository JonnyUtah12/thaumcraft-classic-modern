package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.foci.FocusPouchContents;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocusPouch;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFoci;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The focus-pouch GUI (content-mapping.csv {@code thaumcraft:gui_focus_pouch},
 * original gui_id 5 / {@code ContainerFocusPouch} + {@code InventoryFocusPouch}).
 *
 * <h2>What the original was</h2>
 * Eighteen slots in a 6x3 grid at {@code (37 + i%6*18, 51 + i/6*18)}
 * ({@code ContainerFocusPouch.java:31-33}) over a player grid the original
 * placed low because its panel is 232 px tall: rows at {@code 151 + row*18}
 * and the hotbar at {@code 209} ({@code ContainerFocusPouch.bindPlayerInventory},
 * lines 37-47). Its {@code input} was a live 18-slot {@code InventoryFocusPouch}
 * seeded from the pouch stack's {@code "Inventory"} NBT when the container
 * opened and written back on close ({@code onContainerClosed}, lines 108-121),
 * and every slot accepted <b>only</b> an {@code ItemFocusBasic}
 * ({@code InventoryFocusPouch.isItemValidForSlot}, lines 69-71). The stack the
 * pouch sat on — hotbar slot {@code currentItem}, container slot
 * {@code currentItem + 45} — was fenced off: {@code transferStackInSlot}
 * (shift-click) and {@code slotClick}'s number-key branch both returned null
 * for it (lines 100-106, 51-53), and the GUI closed as soon as that hotbar
 * slot went empty ({@code GuiFocusPouch.func_146976_a}, lines 36-40).
 *
 * <h2>Port shape</h2>
 * <ul>
 *   <li>The 18 pouch slots are a {@link SimpleContainer} seeded from
 *       {@link ItemFocusPouch#contents(ItemStack)} and flushed back through
 *       {@link ItemFocusPouch#setInventory(ItemStack, ItemStack[])} in
 *       {@link #removed(Player)} — the original's open-seed / close-writeback
 *       pair, on the {@code thaumcraft:focus_pouch_contents} component
 *       TC-M6-02 shipped for it.</li>
 *   <li>The focus-only filter is {@link ItemFocusPouch#isValidFocus(ItemStack)}
 *       (the original's {@code instanceof ItemFocusBasic} check, which
 *       TC-M6-02 already ported) on {@link Slot#mayPlace}.</li>
 *   <li>The carrier-slot fence is one {@link Slot#mayPickup(Player)} override
 *       on the slot that carries the pouch. 1.21.1 gates its whole click
 *       surface on that predicate — {@code AbstractContainerMenu#clicked}
 *       consults it on the pick-up, shift-click ({@code QUICK_MOVE}) and
 *       number-key ({@code SWAP}) branches — so the original's
 *       {@code slotClick} and {@code transferStackInSlot} guards collapse onto
 *       one method, and {@link #quickMoveStack} keeps the original's explicit
 *       early return as well.</li>
 *   <li>The original closed the GUI from its <em>client</em> draw method. A
 *       1.21.1 screen gets no such hook, so the same rule is
 *       {@link #stillValid(Player)}: the menu is valid while the pouch is
 *       still the stack in its carrier slot, and
 *       {@code ServerPlayer#tickContainer} closes the container on the tick
 *       that reports it false — server-authoritative, and observable in a
 *       game test (the original's check was client-only, which is the
 *       deviation recorded in docs/BEHAVIORAL_DEVIATIONS.md).</li>
 * </ul>
 */
public class FocusPouchMenu extends AbstractContainerMenu {

    /** The original's 18 container slots ({@code new ItemStack[18]}). */
    public static final int POUCH_SLOTS = FocusPouchContents.CAPACITY;

    /** Columns of the original 6x3 pouch grid. */
    public static final int POUCH_COLUMNS = ContainerLayout.POUCH_COLUMNS;

    /** First container slot of the player's 27-slot main inventory. */
    public static final int PLAYER_INV_START = POUCH_SLOTS;

    /** First container slot of the player hotbar (the original's {@code +45}). */
    public static final int PLAYER_HOTBAR_START = POUCH_SLOTS + 27;

    /** Total container slots: 18 pouch + 27 player + 9 hotbar. */
    public static final int SLOT_COUNT = POUCH_SLOTS + 36;

    /** The pouch stack this menu was opened from (the original's {@code pouch}). */
    private final ItemStack pouch;

    /** Hotbar index carrying the pouch (the original's {@code InventoryPlayer.currentItem}). */
    private final int carrierHotbarSlot;

    /** The container slot that carries the pouch (the original's {@code blockSlot = currentItem + 45}). */
    private final int carrierSlotIndex;

    /** The live 18-slot grid, the original's {@code InventoryFocusPouch}. */
    private final SimpleContainer input = new SimpleContainer(POUCH_SLOTS);

    public FocusPouchMenu(final int id, final Inventory playerInventory) {
        super(CatalogFoci.GUI_FOCUS_POUCH.get(), id);
        this.pouch = playerInventory.getSelected();
        this.carrierHotbarSlot = playerInventory.selected;
        this.carrierSlotIndex = PLAYER_HOTBAR_START + this.carrierHotbarSlot;

        // Original: the 18 focus slots, seeded from the pouch's stored inventory.
        final ItemStack[] stored = ItemFocusPouch.getInventory(this.pouch);
        for (int i = 0; i < POUCH_SLOTS; i++) {
            this.input.setItem(i, stored[i].copy());
            this.addSlot(new SlotFocusPouch(this.input, i,
                    ContainerLayout.pouchSlotX(i), ContainerLayout.pouchSlotY(i)));
        }
        bindPlayerInventory(playerInventory);
    }

    /**
     * Original {@code bindPlayerInventory}: rows at {@code 151 + row*18},
     * hotbar at {@code 209} — the original's own coordinates, not the 84/142
     * every other container in this crate uses, because the pouch panel is
     * 232 px tall. The slot that carries the pouch is fenced from the cursor.
     */
    private void bindPlayerInventory(final Inventory inventoryPlayer) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventoryPlayer, col + row * 9 + 9,
                        8 + col * 18, ContainerLayout.POUCH_PLAYER_GRID_TOP + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            final int index = PLAYER_HOTBAR_START + col;
            if (index == this.carrierSlotIndex) {
                this.addSlot(new SlotCarrierFenced(inventoryPlayer, col, 8 + col * 18,
                        ContainerLayout.POUCH_HOTBAR_TOP));
            } else {
                this.addSlot(new Slot(inventoryPlayer, col, 8 + col * 18,
                        ContainerLayout.POUCH_HOTBAR_TOP));
            }
        }
    }

    /** The stack the menu was opened from. */
    public ItemStack pouch() {
        return this.pouch;
    }

    /** The hotbar index the pouch sat in when the menu opened (original {@code currentItem}). */
    public int carrierHotbarSlot() {
        return this.carrierHotbarSlot;
    }

    /** The container slot that carries the pouch (original {@code blockSlot}). */
    public int carrierSlotIndex() {
        return this.carrierSlotIndex;
    }

    /**
     * Original {@code transferStackInSlot} (lines 51-95): a pouch slot merges
     * into the player region, a player slot merges into the pouch, and the
     * carrier slot is refused outright. The player-side legs keep vanilla's
     * hotbar cycling (main inventory ↔ hotbar) for the stacks a pouch refuses,
     * so a stick shift-clicked in a pouch GUI behaves as it does in the
     * player's own inventory.
     */
    @Override
    public ItemStack quickMoveStack(final Player playerMoving, final int index) {
        if (index == this.carrierSlotIndex) {
            // The original returns null for blockSlot before touching any slot.
            return ItemStack.EMPTY;
        }
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack copy = stack.copy();
        if (index < POUCH_SLOTS) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, POUCH_SLOTS, false)
                    && !this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_HOTBAR_START, false)
                    && !this.moveItemStackTo(stack, PLAYER_HOTBAR_START, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    /**
     * Original {@code onContainerClosed} → {@code ItemFocusPouch.setInventory}
     * (lines 108-112): the grid is written back to the pouch stack, slot
     * positions included, on the server only (the original gated the writeback
     * on {@code !worldObj.isRemote}).
     */
    @Override
    public void removed(final Player playerLeaving) {
        super.removed(playerLeaving);
        if (!playerLeaving.level().isClientSide && !this.pouch.isEmpty()) {
            ItemFocusPouch.setInventory(this.pouch, slotsArray());
        }
    }

    /** The live grid as the original's fixed-length {@code ItemStack[18]}. */
    public ItemStack[] slotsArray() {
        final ItemStack[] out = new ItemStack[POUCH_SLOTS];
        for (int i = 0; i < POUCH_SLOTS; i++) {
            out[i] = this.slots.get(i).getItem();
        }
        return out;
    }

    /**
     * The original closed its GUI from the client draw method the moment the
     * pouch left the hotbar slot that opened it
     * ({@code GuiFocusPouch.func_146976_a}, lines 36-40). Here the same rule is
     * the menu's own validity, which {@code ServerPlayer#tickContainer} turns
     * into a close on the tick it reports false.
     */
    @Override
    public boolean stillValid(final Player playerCheck) {
        if (this.pouch.isEmpty()) {
            return false;
        }
        final ItemStack carried = playerCheck.getInventory().items.get(this.carrierHotbarSlot);
        return ItemStack.isSameItem(this.pouch, carried);
    }

    /**
     * A pouch slot: focus-only, the original's
     * {@code InventoryFocusPouch.isItemValidForSlot} (lines 69-71) — and the
     * nested-pouch case is refused by the same predicate TC-M6-02 shipped.
     */
    private static final class SlotFocusPouch extends Slot {

        SlotFocusPouch(final SimpleContainer container, final int index, final int x, final int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return ItemFocusPouch.isValidFocus(stack);
        }
    }

    /**
     * The hotbar slot carrying the pouch: the original's
     * {@code ContainerFocusPouch.slotClick} number-key guard (lines 100-106)
     * and its {@code transferStackInSlot} guard (lines 100-106), as 1.21.1's
     * one pickup predicate. {@code AbstractContainerMenu#clicked} gates the
     * pick-up, {@code QUICK_MOVE} (shift-click) and {@code SWAP} (number-key)
     * branches on {@link Slot#mayPickup(Player)}, so the pouch cannot leave
     * the slot while its own GUI is open, whichever way the player reaches
     * for it.
     */
    private static final class SlotCarrierFenced extends Slot {

        SlotCarrierFenced(final Inventory inventory, final int index, final int x, final int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPickup(final Player playerReaching) {
            return ItemFocusPouch.isValidFocus(this.getItem());
        }
    }
}
