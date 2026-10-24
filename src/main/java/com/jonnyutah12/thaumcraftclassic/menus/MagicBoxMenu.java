package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.tiles.TileMagicBox;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Magic box menu (content-mapping.csv {@code thaumcraft:gui_magic_box},
 * original {@code ContainerMagicBox}).
 *
 * <p>The box inventory is a growable list; slots are laid out for the
 * container size at open time (the original GUI pages through them — the
 * paged screen is TC-M7-01 screen work; the menu exposes the full grid up
 * to a sane cap so large linked clusters stay reachable).</p>
 */
public class MagicBoxMenu extends AbstractContainerMenu {

    private static final int MAX_GRID_SLOTS = 27 * 8;

    private final TileMagicBox box;

    public MagicBoxMenu(final int id, final Inventory playerInventory, final TileMagicBox box) {
        super(null, id);
        this.box = box;
        final int slots = Math.min(box.getContainerSize(), MAX_GRID_SLOTS);
        for (int i = 0; i < slots; i++) {
            this.addSlot(new Slot(box, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }
        final int boxSlotCount = this.slots.size();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
        this.boxSlotCountRef = boxSlotCount;
    }

    private int boxSlotCountRef;

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
        slot.onQuickMove(stack);
        return stack.copy();
    }

    @Override
    public void clickMenuButton(final Player player, final int buttonId) {
        if (buttonId == 1) {
            this.box.sort();
        }
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.box.stillValid(player);
    }
}
