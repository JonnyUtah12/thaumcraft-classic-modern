package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneBore;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Arcane bore menu (content-mapping.csv {@code thaumcraft:gui_arcane_bore},
 * original {@code ContainerArcaneBore} / {@code GuiArcaneBore}).
 *
 * <p>Two slots: excavation focus (slot 0) and pickaxe (slot 1), matching the
 * original container. The screen half is TC-M7-01; the menu + open path are
 * live.</p>
 */
public class MenuArcaneBore extends AbstractContainerMenu {

    private final TileArcaneBore bore;
    private final ContainerLevelAccess access;

    public MenuArcaneBore(final int id, final Inventory playerInventory, final TileArcaneBore bore) {
        super(null, id);
        this.bore = bore;
        this.access = ContainerLevelAccess.create(bore.getLevel(), bore.getBlockPos());
        this.checkReachable = true;

        // Original GuiArcaneBore slot coordinates.
        addSlot(new BoreFocusSlot(bore, 0, 80, 26));
        addSlot(new Slot(bore, 1, 98, 8));
        MenuAlchemyFurnace.addPlayerInventory(this, playerInventory);
    }

    /** Original GuiArcaneBore restricted the focus slot to the excavation focus. */
    static final class BoreFocusSlot extends Slot {
        BoreFocusSlot(final net.minecraft.world.WorldlyContainer container, final int slot,
                final int x, final int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            final net.minecraft.resources.ResourceLocation id =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id.getNamespace().equals("thaumcraft")
                    && id.getPath().equals("itemfocusexcavation");
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
        } else if (!moveItemStackTo(stack, 1, 2, false)) {
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
