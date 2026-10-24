package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.tiles.TileDeconstructionTable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Deconstruction table menu (content-mapping.csv {@code thaumcraft:gui_decon_table},
 * original {@code ContainerDeconstructionTable}).
 *
 * <p>One input slot (items that carry aspects only) plus the player
 * inventory; the break-time progress is a data slot. Menu button 1 is the
 * original's "collect the revealed aspect into your research pool" action —
 * it adds the aspect to the player's aspect pool via the knowledge
 * attachment (original {@code PlayerKnowledge.addAspectPool}).</p>
 */
public class DeconstructionTableMenu extends AbstractContainerMenu {

    private final TileDeconstructionTable table;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(final int index) {
            return DeconstructionTableMenu.this.table.breaktime;
        }

        @Override
        public void set(final int index, final int value) {
            DeconstructionTableMenu.this.table.breaktime = value;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public DeconstructionTableMenu(final int id, final Inventory playerInventory, final TileDeconstructionTable table) {
        super(null, id);
        this.table = table;
        this.addSlot(new Slot(table, 0, 64, 16) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return DeconstructionTableMenu.this.table.canPlaceItem(0, stack);
            }
        });
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    final void addPlayerInventory(final Inventory playerInventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public void clickMenuButton(final Player player, final int buttonId) {
        if (buttonId == 1 && this.table.aspect != null && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            final PlayerKnowledgeData knowledge = sp.getData(ModAttachments.PLAYER_KNOWLEDGE);
            sp.setData(ModAttachments.PLAYER_KNOWLEDGE,
                    knowledge.withAspectPoolAdded(this.table.aspect.getTag(), 1));
            this.table.aspect = null;
            this.table.setChanged();
            sp.level().blockEntityChanged(this.table.getBlockPos());
        }
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        // Original shift-click rules: input slot only accepts aspect-bearing
        // items (enforced by mayPlace); everything else uses default movement.
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        if (!moveItemStackTo(stack, 1, this.slots.size(), false)) {
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
