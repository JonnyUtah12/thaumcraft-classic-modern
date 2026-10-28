package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Hungry chest (content-mapping.csv {@code thaumcraft:tilechesthungry}).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileChestHungry} — a 27-slot
 * chest whose lid animates for viewers (client data via containerId 1/2 in the
 * original; here the lid is client render state, M7-01) and whose block
 * swallows dropped items that touch it (BlockChestHungry.entityInside →
 * {@link #absorb}). The original never feeds the player: the card body's
 * "feeds players" phrasing does not match 4.2.3.5 source, which only
 * "doesn't wait to be opened" (its lang tooltip) — items are consumed on
 * contact. The GUI is the vanilla 27-slot chest container (original
 * GUI_CHEST_HUNGRY handler=-).</p>
 */
public class TileChestHungry extends BlockEntity implements WorldlyContainer, com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.TickingDeviceTile, net.minecraft.world.MenuProvider {

    private final ItemStack[] chestContents = new ItemStack[27];

    /** Lid state for the client renderer (original lidAngle; M7-01 consumes it). */
    public float lidAngle;
    public float prevLidAngle;
    public int numUsingPlayers;

    public TileChestHungry(final BlockPos pos, final BlockState state) {
        super(CatalogFurniture.TILE_CHEST_HUNGRY.get(), pos, state);
        // A fresh chest carries no contents: the original's slots held empty
        // stacks, so every read path can rely on a real (possibly empty) item
        // (same constructor contract as TileArcaneBore).
        for (int i = 0; i < this.chestContents.length; i++) {
            this.chestContents[i] = ItemStack.EMPTY;
        }
    }

    public void serverTick() {
        // Original updateEntity only advances the client lid animation; the
        // server-side lid bookkeeping is kept so the container can report it.
        this.prevLidAngle = this.lidAngle;
        final float step = 0.1F;
        if (this.numUsingPlayers > 0 && this.lidAngle == 0.0F) {
            this.level.playSound(null, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5,
                    this.worldPosition.getZ() + 0.5, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F,
                    this.level.random.nextFloat() * 0.1F + 0.9F);
        }
        if (this.numUsingPlayers == 0 && this.lidAngle > 0.0F || this.numUsingPlayers > 0 && this.lidAngle < 1.0F) {
            final float old = this.lidAngle;
            this.lidAngle += this.numUsingPlayers > 0 ? step : -step;
            if (this.lidAngle > 1.0F) {
                this.lidAngle = 1.0F;
            }
            if (this.lidAngle < 0.5F && old >= 0.5F) {
                this.level.playSound(null, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5,
                        this.worldPosition.getZ() + 0.5, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F,
                        this.level.random.nextFloat() * 0.1F + 0.9F);
            }
            if (this.lidAngle < 0.0F) {
                this.lidAngle = 0.0F;
            }
        }
    }

    /** Original BlockChestHungry.onEntityCollidedWithBlock: swallow dropped items. */
    public void absorb(final ItemEntity item) {
        if (this.level.isClientSide || item.isRemoved()) {
            return;
        }
        final ItemStack stack = item.getItem();
        for (int i = 0; i < this.chestContents.length && !stack.isEmpty(); i++) {
            if (this.chestContents[i].isEmpty()) {
                this.chestContents[i] = stack.copy();
                stack.setCount(0);
            } else if (ItemStack.isSameItemSameComponents(this.chestContents[i], stack)) {
                final int room = this.chestContents[i].getMaxStackSize() - this.chestContents[i].getCount();
                if (room > 0) {
                    final int take = Math.min(room, stack.getCount());
                    this.chestContents[i].grow(take);
                    stack.shrink(take);
                }
            }
        }
        if (stack.isEmpty()) {
            item.discard();
            this.level.playSound(null, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5,
                    this.worldPosition.getZ() + 0.5, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F,
                    ((this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            setChanged();
        }
    }

    @Override
    public int getContainerSize() {
        return this.chestContents.length;
    }

    @Override
    public ItemStack getItem(final int slot) {
        final ItemStack stack = this.chestContents[slot];
        return stack == null ? ItemStack.EMPTY : stack;
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        // ContainerHelper.removeItem needs a NonNullList; this chest keeps a plain
        // array, so mirror the vanilla result semantics directly.
        if (slot < 0 || slot >= this.chestContents.length) {
            return ItemStack.EMPTY;
        }
        final ItemStack current = this.chestContents[slot];
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = current.copy();
        if (current.getCount() <= count) {
            this.chestContents[slot] = ItemStack.EMPTY;
        } else {
            current.setCount(current.getCount() - count);
            removed.setCount(count);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (slot < 0 || slot >= this.chestContents.length) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = this.chestContents[slot];
        if (removed.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.chestContents[slot] = ItemStack.EMPTY;
        setChanged();
        return removed;
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack stack : this.chestContents) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.chestContents.length; i++) {
            this.chestContents[i] = ItemStack.EMPTY;
        }
        setChanged();
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        this.chestContents[slot] = stack == null ? ItemStack.EMPTY : stack;
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
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

    // 1.21.1 has no getContainerData(int) hook; container data is delivered through
    // BlockEntity.triggerEvent (vanilla chest: id 1 = open viewer count), which is
    // what the original containerId 1/2 protocol carried. The lid stays client
    // render state (M7-01).
    @Override
    public boolean triggerEvent(final int id, final int type) {
        if (id == 1) {
            this.numUsingPlayers = Math.max(type, 0);
            return true;
        }
        return super.triggerEvent(id, type);
    }

    @Override
    public void startOpen(final Player player) {
        this.numUsingPlayers++;
    }

    @Override
    public void stopOpen(final Player player) {
        this.numUsingPlayers--;
    }

    // --- MenuProvider: the original GUI_CHEST_HUNGRY row has no custom
    // handler; the vanilla chest menu realizes it. ---

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.thaumcraft.chesthungry");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(final int id,
            final net.minecraft.world.entity.player.Inventory inventory, final Player player) {
        // Vanilla 27-slot chest GUI: the three-row chest menu, the same open
        // seam ChestBlockEntity uses (the original GUI_CHEST_HUNGRY row had
        // no custom handler).
        return net.minecraft.world.inventory.ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getCenter()) <= 64.0;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final ListTag items = new ListTag();
        for (int i = 0; i < this.chestContents.length; i++) {
            if (!this.chestContents[i].isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", this.chestContents[i].save(registries));
                items.add(entry);
            }
        }
        tag.put("Items", items);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < this.chestContents.length; i++) {
            this.chestContents[i] = ItemStack.EMPTY;
        }
        for (final Tag entry : tag.getList("Items", Tag.TAG_COMPOUND)) {
            final CompoundTag item = (CompoundTag) entry;
            final int slot = item.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.chestContents.length && item.contains("Item")) {
                this.chestContents[slot] = ItemStack.parseOptional(registries, item.getCompound("Item"));
            }
        }
    }
}
