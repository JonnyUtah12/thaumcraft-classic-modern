package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.menus.ArcaneWorkbenchMenu;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M403ArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.wands.IVisContainerItem;
import com.jonnyutah12.thaumcraftclassic.wands.IVisStaffItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Arcane workbench inventory (content-mapping.csv tile_entity row
 * {@code thaumcraft:tilearcaneworkbench}; TC-M4-03).
 *
 * <p>Original: {@code TileArcaneWorkbench extends TileMagicWorkbench}, an
 * 11-slot IInventory/ISidedInventory: 0-8 crafting grid, 9 soft crafting
 * output (recomputed by the open container, never a real stored stack), 10
 * wand slot. The ISidedInventory face arrays are {@code {10}} on every face
 * — hoppers may only push/pull the wand slot, never the grid or the output
 * (original quirk, preserved verbatim).</p>
 *
 * <p>The grid persists in the tile (the original stored it in NBT), unlike
 * the vanilla crafting table whose grid lives in the menu. The output slot
 * stays empty here: the menu's result slot is a {@code ResultContainer}
 * (1.21.1 idiom), matching the original's "soft" slot semantics — the
 * output is always recomputed from the grid, never saved.</p>
 */
public class TileArcaneWorkbench extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int GRID_SIZE = 9;
    /** Soft output slot; always empty in the tile (menu-owned). */
    public static final int SLOT_OUTPUT = 9;
    public static final int SLOT_WAND = 10;
    public static final int SIZE = 11;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public TileArcaneWorkbench(final BlockPos pos, final BlockState state) {
        super(M403ArcaneWorkbench.TILE_ARCANE_WORKBENCH.get(), pos, state);
    }

    /** Original SlotLimitedByWand.isItemValid: wands yes, staffs and amulets no. */
    public static boolean isWandSlotStack(final ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof IVisContainerItem
                && !(stack.getItem() instanceof IVisStaffItem);
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        // Original IInventory.func_145825_b() = "container.arcaneworkbench".
        return Component.translatable("container.arcaneworkbench");
    }

    @Override
    public AbstractContainerMenu createMenu(final int windowId, final Inventory playerInventory,
                                            final Player player) {
        return new ArcaneWorkbenchMenu(windowId, playerInventory, this);
    }

    // NOTE: no writeScreenOpeningData override — NeoForge 21.1 has no such
    // server hook; BlockDeviceBase opens via player.openMenu(provider, pos),
    // which writes the block pos into the menu buffer automatically, and the
    // buf constructor reads it back.

    // ------------------------------------------------------------- container

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot < 0 || slot >= SIZE ? ItemStack.EMPTY : items.get(slot);
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (slot < 0 || slot >= SIZE) {
            return;
        }
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        final ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = current.split(count);
        setItem(slot, current);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        final ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        setItem(slot, ItemStack.EMPTY);
        return current;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (slot == SLOT_WAND) {
            return isWandSlotStack(stack);
        }
        // The soft output is menu-owned; the original's slot 9 was only ever
        // written by setInventorySlotContentsSoftly from the container.
        return slot != SLOT_OUTPUT;
    }

    /**
     * Original ISidedInventory quirk: every face exposes only slot 10
     * ({@code getSlotsForFace → {10}}), so hoppers interact with the wand
     * slot and nothing else.
     */
    @Override
    public int[] getSlotsForFace(final Direction side) {
        return new int[]{SLOT_WAND};
    }

    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack,
                                           @Nullable final Direction side) {
        return slot == SLOT_WAND && isWandSlotStack(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack,
                                          @Nullable final Direction side) {
        return slot == SLOT_WAND;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SIZE; i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean stillValid(final Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5) <= 64.0;
    }

    // ------------------------------------------------------------------ nbt

    @Override
    protected void saveAdditional(final CompoundTag tag,
                                  final net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag,
                                  final net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, items, registries);
    }
}
