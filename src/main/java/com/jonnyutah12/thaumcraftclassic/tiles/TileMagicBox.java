package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604dFurniture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Magic box (content-mapping.csv {@code thaumcraft:tilemagicbox}).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileMagicBox}. Contents are a
 * growable list, not fixed slots; capacity is 27 × (adjacent-box count + 1)
 * (refreshLinks flood-fills touching magic boxes, cap 1024). A box whose NBT
 * carries a master coordinate delegates its contents to that box — the shared
 * inventory mechanism; in 4.2.3.5 nothing assigns master in code (legacy
 * field, only read from NBT), so adjacency grows capacity while each box
 * keeps its own contents, exactly as the original behaves. Optional sorting
 * (sort modes 0/1/2 = name asc/desc, mod+name) compacts and orders the list;
 * like the original it is only ever restored from NBT.</p>
 */
public class TileMagicBox extends BlockEntity implements WorldlyContainer, com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.TickingDeviceTile, net.minecraft.world.MenuProvider {

    private final List<ItemStack> boxContents = new ArrayList<>();
    @Nullable
    private BlockPos master;
    private byte sorting = -1;
    private short linkedBoxes = -1;

    public TileMagicBox(final BlockPos pos, final BlockState state) {
        super(M604dFurniture.TILE_MAGIC_BOX.get(), pos, state);
    }

    public void serverTick() {
        if (getInventory() == this && this.linkedBoxes < 0) {
            refreshLinks();
        }
    }

    private TileMagicBox getInventory() {
        if (this.master != null) {
            final BlockEntity tile = this.level.getBlockEntity(this.master);
            if (tile instanceof TileMagicBox box) {
                return box;
            }
        }
        return this;
    }

    private List<ItemStack> getContents() {
        return getInventory().boxContents;
    }

    /** Original refreshLinks(): count the touching-box cluster (this box included). */
    public void refreshLinks() {
        if (getInventory() == this) {
            final List<BlockPos> list = new ArrayList<>();
            findBoxes(this.worldPosition, list);
            this.linkedBoxes = (short) list.size();
        }
    }

    private void findBoxes(final BlockPos pos, final List<BlockPos> list) {
        if (list.size() >= 1024) {
            return;
        }
        for (final Direction dir : Direction.values()) {
            final BlockPos next = pos.relative(dir);
            if (this.level.getBlockEntity(next) instanceof TileMagicBox) {
                if (!list.contains(next)) {
                    list.add(next);
                    findBoxes(next, list);
                }
            }
        }
    }

    @Override
    public int getContainerSize() {
        return 27 * (Math.max(0, getInventory().linkedBoxes) + 1);
    }

    @Override
    public ItemStack getItem(final int slot) {
        final List<ItemStack> contents = getContents();
        return slot >= contents.size() ? ItemStack.EMPTY : contents.get(slot);
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        final List<ItemStack> contents = getContents();
        if (slot >= contents.size() || contents.get(slot).isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack current = contents.get(slot);
        final ItemStack removed;
        if (current.getCount() <= count) {
            removed = current;
            contents.remove(slot);
        } else {
            removed = current.split(count);
        }
        getInventory().setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        final List<ItemStack> contents = getContents();
        if (slot < contents.size() && !contents.get(slot).isEmpty()) {
            final ItemStack stack = contents.remove(slot);
            getInventory().setChanged();
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        final List<ItemStack> contents = getContents();
        if (slot >= contents.size() && !stack.isEmpty()) {
            contents.add(stack);
        } else if (!stack.isEmpty()) {
            contents.set(slot, stack);
        } else if (slot < contents.size()) {
            contents.remove(slot);
        }
        getInventory().setChanged();
        sort();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return true;
    }

    @Override
    public int[] getSlotsForFace(final Direction side) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, @Nullable final Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
        return false;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.thaumcraft.magicbox");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(final int id,
            final net.minecraft.world.entity.player.Inventory inventory, final Player player) {
        return new com.jonnyutah12.thaumcraftclassic.menus.MagicBoxMenu(id, inventory, this);
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getCenter()) <= 64.0;
    }

    /** Original sort(): bubble-order + stack-merge while sorting is enabled. */
    public void sort() {
        if (this.sorting < 0 || this.level == null || this.level.isClientSide) {
            return;
        }
        final List<ItemStack> contents = getContents();
        boolean done = false;
        while (!done) {
            done = true;
            for (int i = 0; i < contents.size() - 1; i++) {
                done = swapSlots(contents, i, i + 1);
                final ItemStack a = contents.get(i);
                final ItemStack next = i + 1 < contents.size() ? contents.get(i + 1) : ItemStack.EMPTY;
                if (a.getCount() < a.getMaxStackSize() && ItemStack.isSameItemSameComponents(a, next)) {
                    final int moved = Math.min(a.getMaxStackSize() - a.getCount(), next.getCount());
                    a.grow(moved);
                    next.shrink(moved);
                    done = false;
                    if (next.getCount() <= 0) {
                        contents.remove(i + 1);
                        break;
                    }
                }
            }
        }
    }

    private boolean swapSlots(final List<ItemStack> contents, final int i, final int j) {
        final String s1 = sortKey(contents.get(i));
        final String s2 = sortKey(contents.get(j));
        final int r = s1.compareToIgnoreCase(s2);
        final boolean swap = (r > 0 && (this.sorting == 0 || this.sorting == 2)) || (r < 0 && this.sorting == 1);
        if (swap) {
            final ItemStack tmp = contents.get(i);
            contents.set(i, contents.get(j));
            contents.set(j, tmp);
            return false;
        }
        return true;
    }

    private String sortKey(final ItemStack stack) {
        final StringBuilder key = new StringBuilder();
        if (this.sorting == 2) {
            key.append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
        }
        key.append(stack.getHoverName().getString());
        if (stack.hasTag()) {
            key.append(stack.getTag().hashCode());
        }
        return key.toString();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final ListTag items = new ListTag();
        for (final ItemStack stack : this.boxContents) {
            if (!stack.isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                stack.save(entry);
                items.add(entry);
            }
        }
        tag.put("Items", items);
        tag.putByte("sort", this.sorting);
        if (this.master != null) {
            tag.putInt("w_x", this.master.getX());
            tag.putInt("w_y", this.master.getY());
            tag.putInt("w_z", this.master.getZ());
        }
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.boxContents.clear();
        for (final Tag entry : tag.getList("Items", Tag.TAG_COMPOUND)) {
            this.boxContents.add(ItemStack.of((CompoundTag) entry));
        }
        this.sorting = tag.getByte("sort");
        this.master = tag.contains("w_x") ? new BlockPos(tag.getInt("w_x"), tag.getInt("w_y"), tag.getInt("w_z")) : null;
    }
}
