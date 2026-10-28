package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.blocks.VariantDevice;
import com.jonnyutah12.thaumcraftclassic.entities.EntityItemGrate;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Item grate (content-mapping.csv {@code thaumcraft:tilegrate}, hosted on
 * blockMetalDevice variants 5/6 — TC-M6-04c's block).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileGrate}. A one-slot pseudo
 * inventory: putting an item in (only on the open grate, variant 5, from
 * above) spawns an {@link EntityItemGrate} that drifts down through the
 * grate; the slot itself never holds anything.</p>
 */
public class TileGrate extends BlockEntity implements WorldlyContainer {

    public TileGrate(final BlockPos pos, final BlockState state) {
        super(CatalogFurniture.TILE_GRATE.get(), pos, state);
    }

    /** True when this grate is the open variant (original metadata == 5). */
    private boolean isOpen() {
        final BlockState state = getBlockState();
        return state.getBlock() instanceof VariantDevice device && device.variant(state) == 5;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isEmpty() {
        // The grate slot is never filled: items pass through to EntityItemGrate.
        return true;
    }

    @Override
    public void clearContent() {
        // Nothing is ever stored in the slot; the Clearable contract only needs
        // the member to exist.
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (!this.level.isClientSide && !stack.isEmpty()) {
            final EntityItemGrate entity = EntityItemGrate.of(this.level, this.worldPosition, stack);
            entity.setDeltaMovement(0.0, -0.1, 0.0);
            this.level.addFreshEntity(entity);
        }
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return isOpen();
    }

    @Override
    public int[] getSlotsForFace(final Direction side) {
        return isOpen() && side == Direction.UP ? new int[]{0} : new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, @Nullable final Direction side) {
        return isOpen() && side == Direction.UP;
    }

    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
        return false;
    }

    @Override
    public boolean stillValid(final Player player) {
        return false;
    }
}
