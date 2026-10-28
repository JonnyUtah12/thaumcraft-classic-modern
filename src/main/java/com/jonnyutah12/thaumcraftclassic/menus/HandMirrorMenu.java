package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.items.ItemHandMirror;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Hand-mirror GUI (content-mapping.csv {@code thaumcraft:GUI_HAND_MIRROR},
 * original gui_id 16 / {@code ContainerHandMirror}).
 *
 * <p>Original: one input slot at (80,24) over the standard 36-slot player
 * inventory (rows at y=84/102/120... original 84+i*18 and hotbar 142). Any
 * item placed in the input slot is teleported out of the mirror the held
 * mirror item is linked to ({@code ItemHandMirror.transport}); the slot is
 * cleared only on success, so a broken link leaves the item in the slot.
 * Placing a copy of the held mirror itself into the slot closed the original's
 * GUI (its {@code player.openContainer = player.inventoryContainer} trick).
 * Leaving the GUI returns the slot contents to the player. The container stays
 * valid without any reachability check (original {@code doCheckKeyBinds}
 * returned true).</p>
 *
 * <p>1.21.1 realization: the transport check runs from
 * {@link #broadcastChanges()} — the server-side per-tick slot diff point,
 * since 1.21.1 has no container-level slotChanged hook. The mirror stack is
 * the player's selected hotbar stack captured at open time, exactly as the
 * original captured {@code InventoryPlayer.getCurrentItem()}.</p>
 */
public class HandMirrorMenu extends AbstractContainerMenu {

    /** Original input slot index (the only container slot). */
    public static final int SLOT_INPUT = 0;

    private final SimpleContainer input = new SimpleContainer(1);
    private final Player player;
    private final ItemStack mirror;

    public HandMirrorMenu(final int id, final Inventory playerInventory) {
        super(CatalogTrinketsLoot.GUI_HAND_MIRROR.get(), id);
        this.player = playerInventory.player;
        this.mirror = playerInventory.getSelected();

        // Original slot layout, verbatim coordinates.
        this.addSlot(new Slot(this.input, SLOT_INPUT, 80, 24));
        bindPlayerInventory(playerInventory);
    }

    private void bindPlayerInventory(final Inventory inventoryPlayer) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventoryPlayer, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventoryPlayer, col, 8 + col * 18, 142));
        }
    }

    /**
     * Original slotChangedListener: an item entering the input slot is
     * transported through the mirror; a copy of the held mirror instead closes
     * the GUI (the original returned it to the player inventory that way).
     */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (player.level().isClientSide) {
            return;
        }
        final ItemStack in = this.slots.get(SLOT_INPUT).getItem();
        if (in.isEmpty()) {
            return;
        }
        if (ItemStack.isSameItem(in, mirror)) {
            this.slots.get(SLOT_INPUT).set(ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(in);
            player.closeContainer();
            return;
        }
        if (ItemHandMirror.transport(mirror, in, player, player.level())) {
            this.slots.get(SLOT_INPUT).set(ItemStack.EMPTY);
        }
    }

    /**
     * Original shift-click: input slot merges into the player inventory,
     * player slots merge into the input slot; the held mirror itself may not
     * be shift-clicked into the input slot.
     */
    @Override
    public ItemStack quickMoveStack(final Player playerMoving, final int index) {
        ItemStack result = ItemStack.EMPTY;
        final Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == SLOT_INPUT) {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof ItemHandMirror) {
                return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    /** Original onContainerClosed: return the input slot contents. */
    @Override
    public void removed(final Player playerLeaving) {
        super.removed(playerLeaving);
        if (!playerLeaving.level().isClientSide) {
            final ItemStack leftover = this.input.removeItemNoUpdate(SLOT_INPUT);
            if (leftover != null && !leftover.isEmpty()) {
                playerLeaving.getInventory().placeItemBackInInventory(leftover);
            }
        }
    }

    /** Original doCheckKeyBinds: always valid. */
    @Override
    public boolean stillValid(final Player playerCheck) {
        return true;
    }
}
