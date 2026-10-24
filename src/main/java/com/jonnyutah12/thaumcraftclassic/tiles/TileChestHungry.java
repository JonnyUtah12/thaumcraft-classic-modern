package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604dFurniture;
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
        super(M604dFurniture.TILE_CHEST_HUNGRY.get(), pos, state);
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
        return this.chestContents[slot];
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        final ItemStack removed = super.removeItem(slot, count);
        setChanged();
        return removed;
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

    @Override
    public int[] getContainerData(final int index) {
        // Vanilla chest container protocol: id 1 = viewers, id 2 = lid angle.
        return index == 1 ? new int[]{this.numUsingPlayers} : new int[]{(int) (this.lidAngle * 10)};
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
        return new net.minecraft.world.inventory.ChestMenu(id, inventory, this);
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
                this.chestContents[i].save(entry);
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
            if (slot >= 0 && slot < this.chestContents.length) {
                this.chestContents[slot] = ItemStack.of(item);
            }
        }
    }
}
