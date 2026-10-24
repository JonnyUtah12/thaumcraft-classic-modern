package com.jonnyutah12.thaumcraftclassic.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
 * Curios display pedestal (content-mapping.csv {@code thaumcraft:tilepedestal},
 * hosted on blockCosmeticSolid variant 15 — TC-M6-04b's block — and on
 * blockStoneDevice variant 1, the arcane pedestal used by the infusion altar,
 * TC-M4-04).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TilePedestal}. A single-slot
 * display inventory: the item sits on the pedestal (rendered by M7-01), and
 * changes push a block update so the client re-renders. The infusion altar
 * fills pedestals through {@link #setItemFromInfusion} (TC-M4-04 consumer).
 * The original's "aspect effects" live on the items placed here, not in this
 * tile — nothing aspect-related exists in 4.2.3.5 TilePedestal.</p>
 */
public class TilePedestal extends BlockEntity implements WorldlyContainer { // no tick (original canUpdate=false)

    private final ItemStack[] inventory = new ItemStack[1];

    public TilePedestal(final BlockPos pos, final BlockState state) {
        super(com.jonnyutah12.thaumcraftclassic.registration.catalog.M604dFurniture.TILE_PEDESTAL.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.inventory[0].isEmpty();
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot == 0 ? this.inventory[0] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        if (slot != 0 || this.inventory[0].isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = this.inventory[0].split(count);
        if (this.inventory[0].isEmpty()) {
            this.inventory[0] = ItemStack.EMPTY;
        }
        setChanged();
        if (!level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (slot == 0 && !this.inventory[0].isEmpty()) {
            final ItemStack removed = this.inventory[0];
            this.inventory[0] = ItemStack.EMPTY;
            return removed;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (slot == 0) {
            this.inventory[0] = stack == null ? ItemStack.EMPTY : stack;
            setChanged();
            if (!level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public void clearItem(final int slot) {
        if (slot == 0) {
            this.inventory[0] = ItemStack.EMPTY;
            setChanged();
        }
    }

    /** Original setInventorySlotContentsFromInfusion (infusion altar path). */
    public void setItemFromInfusion(final int slot, final ItemStack stack) {
        setItem(slot, stack);
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
    public boolean stillValid(final Player player) {
        return this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getCenter()) <= 64.0;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final ListTag items = new ListTag();
        for (int i = 0; i < this.inventory.length; i++) {
            if (!this.inventory[i].isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", this.inventory[i].save(registries));
                items.add(entry);
            }
        }
        tag.put("Items", items);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory[0] = ItemStack.EMPTY;
        for (final Tag entry : tag.getList("Items", Tag.TAG_COMPOUND)) {
            final CompoundTag item = (CompoundTag) entry;
            final int slot = item.getByte("Slot") & 255;
            if (slot == 0 && item.contains("Item")) {
                this.inventory[0] = ItemStack.parseOptional(registries, item.getCompound("Item"));
            }
        }
    }
}
