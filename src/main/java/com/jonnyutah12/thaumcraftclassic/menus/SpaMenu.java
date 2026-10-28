package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.tiles.TileSpa;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Arcane spa menu (content-mapping.csv {@code thaumcraft:gui_spa}, original
 * {@code ContainerSpa}).
 *
 * <p>One bath-salt slot; menu button 1 toggles the mix mode (original
 * {@code onContainerEvent(button==1) → toggleMix}).</p>
 */
public class SpaMenu extends AbstractContainerMenu {

    private final TileSpa spa;

    public SpaMenu(final int id, final Inventory playerInventory, final TileSpa spa) {
        super(null, id);
        this.spa = spa;
        this.addSlot(new Slot(spa, 0, 65, 31) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return SpaMenu.this.spa.canPlaceItem(0, stack);
            }
        });
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    // MC 1.21.1 declares clickMenuButton as boolean (true = the button did something).
    @Override
    public boolean clickMenuButton(final Player player, final int buttonId) {
        if (buttonId == 1) {
            this.spa.toggleMix();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        // MC 1.21.1 Slot has no onQuickMove hook; moveItemStackTo already
        // marks the container changed via Slot#set.
        return stack.copy();
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.spa.stillValid(player);
    }
}
