package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.tiles.TileResearchTable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Research table menu (content-mapping.csv {@code thaumcraft:gui_research_table},
 * original {@code ContainerResearchTable}).
 *
 * <p>Slot 0 takes only scribe tools, slot 1 only unfinished research notes
 * (the original's {@code SlotLimitedByClass}); layout coordinates are the
 * original's. Menu button 5 is the original's duplicate-note action.</p>
 */
public class ResearchTableMenu extends AbstractContainerMenu {

    public static final int BUTTON_DUPLICATE = 5;

    private final TileResearchTable table;

    public ResearchTableMenu(final int id, final Inventory playerInventory, final TileResearchTable table) {
        super(null, id);
        this.table = table;
        this.addSlot(new Slot(table, 0, 14, 10) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return ResearchTableMenu.this.table.canPlaceItem(0, stack);
            }
        });
        this.addSlot(new Slot(table, 1, 70, 10) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return ResearchTableMenu.this.table.canPlaceItem(1, stack);
            }
        });
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 48 + j * 18, 175 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 48 + i * 18, 233));
        }
    }

    @Override
    public void clickMenuButton(final Player player, final int buttonId) {
        if (buttonId == BUTTON_DUPLICATE) {
            this.table.duplicate(player);
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
        return stack.copy();
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.table.stillValid(player);
    }
}
