package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectCombinations;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTags;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Deconstruction table (content-mapping.csv {@code thaumcraft:tiledeconstructiontable},
 * hosted on blockTable variant 14).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileDeconstructionTable}. Over
 * 40 ticks it "breaks" one item in its slot: the item's object+bonus tags are
 * reduced to primals, and with probability {@code visSize/80} the table holds
 * one of those primal aspects for the player to collect into their aspect
 * pool via the GUI (see {@link com.jonnyutah12.thaumcraftclassic.client.menu.MenuDeconstructionTable}).
 * Only items that carry aspects may be inserted (original isItemValidForSlot /
 * SlotLimitedHasAspects).</p>
 */
public class TileDeconstructionTable extends BlockEntity implements WorldlyContainer, com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.TickingDeviceTile, net.minecraft.world.MenuProvider {

    private static final int BREAK_TIME = 40;

    private final ItemStack[] itemStacks = new ItemStack[1];
    @Nullable
    public Aspect aspect;
    public int breaktime;

    public TileDeconstructionTable(final BlockPos pos, final BlockState state) {
        super(CatalogFurniture.TILE_DECONSTRUCTION_TABLE.get(), pos, state);
        // A fresh table holds no item: the original's slot was an empty stack,
        // so every read path can rely on a real (possibly empty) item.
        this.itemStacks[0] = ItemStack.EMPTY;
    }

    public void serverTick() {
        boolean dirty = false;
        if (this.breaktime == 0 && canBreak()) {
            this.breaktime = BREAK_TIME;
            dirty = true;
        }
        if (this.breaktime > 0 && canBreak()) {
            this.breaktime--;
            if (this.breaktime == 0) {
                breakItem();
                dirty = true;
            }
        } else {
            this.breaktime = 0;
        }
        if (dirty) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private boolean canBreak() {
        // AspectList exposes size(), not isEmpty().
        return this.itemStacks[0] != null && this.aspect == null && objectTags(this.itemStacks[0]).size() > 0;
    }

    /** Original breakItem(): roll one primal from the reduced tag list. */
    public void breakItem() {
        if (!canBreak()) {
            return;
        }
        final AspectList primals = AspectCombinations.reduceToPrimals(objectTags(this.itemStacks[0]));
        if (level.random.nextInt(80) < primals.visSize()) {
            this.aspect = primals.getAspects().get(level.random.nextInt(primals.getAspects().size()));
        }
        this.itemStacks[0].shrink(1);
        if (this.itemStacks[0].isEmpty()) {
            this.itemStacks[0] = ItemStack.EMPTY;
        }
    }

    private static AspectList objectTags(final ItemStack stack) {
        // Deviation: the port's EssentiaTags surface exposes only object tags —
        // the original's per-item "bonus" essentia tags have no counterpart in
        // this aspect API, so nothing folds them in.
        final AspectList al = EssentiaTags.getObjectTags(stack);
        return al == null ? new AspectList() : al;
    }

    /** True when the stack carries aspects and may enter the table slot. */
    public boolean canAccept(final ItemStack stack) {
        return !stack.isEmpty() && objectTags(stack).size() > 0;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public ItemStack getItem(final int slot) {
        final ItemStack stack = this.itemStacks[0];
        return slot == 0 && stack != null ? stack : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        // 1.21.1 has no inherited container to delegate to; mirror the vanilla
        // remove-item result for this single slot.
        if (slot != 0 || this.itemStacks[0].isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack current = this.itemStacks[0];
        final ItemStack removed = current.copy();
        if (current.getCount() <= count) {
            this.itemStacks[0] = ItemStack.EMPTY;
        } else {
            current.setCount(current.getCount() - count);
            removed.setCount(count);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (slot == 0 && !this.itemStacks[0].isEmpty()) {
            final ItemStack removed = this.itemStacks[0];
            this.itemStacks[0] = ItemStack.EMPTY;
            setChanged();
            return removed;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isEmpty() {
        return this.itemStacks[0].isEmpty();
    }

    @Override
    public void clearContent() {
        this.itemStacks[0] = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (slot == 0) {
            this.itemStacks[0] = stack == null ? ItemStack.EMPTY : stack;
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return canAccept(stack);
    }

    @Override
    public int[] getSlotsForFace(final net.minecraft.core.Direction side) {
        // Original ISidedInventory: bottom can insert, top can extract, sides none.
        return switch (side) {
            case DOWN -> new int[]{0};
            case UP -> new int[0];
            default -> new int[0];
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, @Nullable final net.minecraft.core.Direction side) {
        return side != net.minecraft.core.Direction.UP && canAccept(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final net.minecraft.core.Direction side) {
        return true;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.thaumcraft.deconstructiontable");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(final int id,
            final net.minecraft.world.entity.player.Inventory inventory, final Player player) {
        return new com.jonnyutah12.thaumcraftclassic.menus.DeconstructionTableMenu(id, inventory, this);
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getCenter()) <= 64.0;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.aspect != null) {
            tag.putString("Aspect", this.aspect.getTag());
        }
        final ListTag items = new ListTag();
        for (int i = 0; i < this.itemStacks.length; i++) {
            if (!this.itemStacks[i].isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", this.itemStacks[i].save(registries));
                items.add(entry);
            }
        }
        tag.put("Items", items);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.aspect = tag.contains("Aspect") ? Aspect.getAspect(tag.getString("Aspect")) : null;
        this.itemStacks[0] = ItemStack.EMPTY;
        for (final Tag entry : tag.getList("Items", Tag.TAG_COMPOUND)) {
            final CompoundTag item = (CompoundTag) entry;
            final int slot = item.getByte("Slot") & 255;
            if (slot == 0 && item.contains("Item")) {
                this.itemStacks[0] = ItemStack.parseOptional(registries, item.getCompound("Item"));
            }
        }
    }
}
