package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.tiles.TileAlchemyFurnace;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Alchemical furnace menu (content-mapping.csv {@code thaumcraft:gui_alchemy_furnace},
 * original {@code ContainerAlchemyFurnace} / {@code GuiAlchemyFurnace}).
 *
 * <p>Two container slots — input (slot 0) and fuel (slot 1) — laid out like
 * the original GUI; the stored-aspect buffer is display-only in the original
 * (no slots), so it stays a client-screen concern (TC-M7-01). The screen
 * half is deferred; the menu + open path are live.</p>
 */
public class MenuAlchemyFurnace extends AbstractContainerMenu {

    private final TileAlchemyFurnace furnace;
    private final ContainerLevelAccess access;

    public MenuAlchemyFurnace(final int id, final Inventory playerInventory,
            final TileAlchemyFurnace furnace) {
        super(null, id);
        this.furnace = furnace;
        this.access = ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos());
        this.checkReachable = true;

        // Original GuiAlchemyFurnace slot coordinates.
        addSlot(new Slot(furnace, TileAlchemyFurnace.SLOT_INPUT, 56, 17));
        addSlot(new Slot(furnace, TileAlchemyFurnace.SLOT_FUEL, 56, 53));
        addPlayerInventory(this, playerInventory);
    }

    static void addPlayerInventory(final AbstractContainerMenu menu, final Inventory playerInventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                menu.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            menu.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 2, false)) {
            return ItemStack.EMPTY;
        }
        slot.onQuickMove(stack);
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        return access.evaluate((world, pos) ->
                player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0)
                .orElse(false);
    }
}
