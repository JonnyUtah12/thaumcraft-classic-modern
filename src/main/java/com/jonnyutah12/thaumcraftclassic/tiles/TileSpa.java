package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604dFurniture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;

/**
 * Arcane spa (content-mapping.csv {@code thaumcraft:tilespa}, hosted on
 * blockStoneDevice variant 11 — TC-M6-04c's block; GUI_SPA).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileSpa}. A 5000 mB tank with
 * one bath-salt slot. Every 40 ticks, unpowered and with ingredients, it
 * places fluid above the spa: with mix on, water + bath salts become the
 * purifying fluid (blockFluidPure, TC-M6-04b, reached by id); with mix off
 * the tank's own fluid is dispensed, spreading to valid spots in a 5×5
 * area. Fluid is filled/drained through {@link #fill}/{@link #drain}
 * (1.21.1 has no common fluid handler interface; capability adapters land
 * with the pipe/relay cards).</p>
 */
public class TileSpa extends BlockEntity implements WorldlyContainer, com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.TickingDeviceTile, net.minecraft.world.MenuProvider {

    private static final int TANK_CAPACITY = 5000;

    private final ItemStack[] itemStacks = new ItemStack[1];
    private boolean mix = true;
    private int counter = 0;
    @Nullable
    private ResourceLocation tankFluid;
    private int tankAmount = 0;

    public TileSpa(final BlockPos pos, final BlockState state) {
        super(M604dFurniture.TILE_SPA.get(), pos, state);
    }

    public void toggleMix() {
        this.mix = !this.mix;
        this.level.blockEntityChanged(this.worldPosition);
        this.setChanged();
    }

    public boolean getMix() {
        return this.mix;
    }

    public int getTankAmount() {
        return this.tankAmount;
    }

    @Nullable
    public ResourceLocation getTankFluid() {
        return this.tankFluid;
    }

    public void serverTick() {
        if (this.counter++ % 40 == 0
                && !this.level.hasNeighborSignal(this.worldPosition)
                && hasIngredients()) {
            final BlockState above = this.level.getBlockState(this.worldPosition.above());
            final BlockState target = dispenseTarget();
            if (above.getBlock() == target.getBlock() && isFullFluid(above)) {
                for (int xx = -2; xx <= 2; xx++) {
                    for (int zz = -2; zz <= 2; zz++) {
                        if (isValidLocation(this.worldPosition.offset(xx, 1, zz), true, target)) {
                            consumeIngredients();
                            placeFluid(this.worldPosition.offset(xx, 1, zz), target);
                            return;
                        }
                    }
                }
            } else if (isValidLocation(this.worldPosition.above(), false, target)) {
                consumeIngredients();
                placeFluid(this.worldPosition.above(), target);
            }
        }
    }

    private BlockState dispenseTarget() {
        if (this.mix) {
            final net.minecraft.world.level.block.Block pure = BuiltInRegistries.BLOCK.get(
                    ResourceLocation.fromNamespaceAndPath(
                            com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "blockfluidpure"));
            return pure == Blocks.AIR ? Blocks.WATER.defaultBlockState() : pure.defaultBlockState();
        }
        final Fluid fluid = BuiltInRegistries.FLUID.get(this.tankFluid);
        return fluid.defaultFluidState().createLegacyBlock();
    }

    private static boolean isFullFluid(final BlockState state) {
        return !state.hasProperty(BlockStateProperties.LEVEL_FLOWING) || state.getValue(BlockStateProperties.LEVEL_FLOWING) == 0;
    }

    /** Original checkQuanta: top up a partial fluid to a full source. */
    private void placeFluid(final BlockPos pos, final BlockState target) {
        this.level.setBlock(pos, target, 3);
        final BlockState placed = this.level.getBlockState(pos);
        if (placed.hasProperty(BlockStateProperties.LEVEL_FLOWING)) {
            this.level.setBlock(pos, placed.setValue(BlockStateProperties.LEVEL_FLOWING, 0), 3);
        }
    }

    private boolean hasIngredients() {
        if (this.mix) {
            if (!Fluids.WATER.location().equals(this.tankFluid) || this.tankAmount < 1000) {
                return false;
            }
            return this.itemStacks[0] != null && isBathSalts(this.itemStacks[0]);
        }
        return this.tankFluid != null && this.tankAmount >= 1000
                && BuiltInRegistries.FLUID.get(this.tankFluid).defaultFluidState().createLegacyBlock().getBlock() != Blocks.AIR;
    }

    private void consumeIngredients() {
        if (this.mix) {
            removeItem(0, 1);
        }
        this.tankAmount -= 1000;
        if (this.tankAmount <= 0) {
            this.tankAmount = 0;
            this.tankFluid = null;
        }
        this.setChanged();
    }

    private boolean isValidLocation(final int x, final int y, final int z, final boolean mustBeAdjacent, final BlockState target) {
        final BlockPos pos = new BlockPos(x, y, z);
        final Fluid targetFluid = target.getFluidState().getType();
        if ((targetFluid == Fluids.WATER || targetFluid == Fluids.LAVA)
                && !this.level.dimensionType().hasSkyLight()) {
            return false;
        }
        final BlockState b = this.level.getBlockState(pos);
        final BlockState below = this.level.getBlockState(pos.below());
        if (below.isFaceSturdy(this.level, pos.below(), Direction.UP)
                && b.getBlock().isReplaceable(this.level, pos)
                && !(b.getBlock() == target.getBlock() && isFullFluid(b))) {
            if (!mustBeAdjacent) {
                return true;
            }
            for (final Direction dir : Direction.Plane.HORIZONTAL) {
                final BlockState side = this.level.getBlockState(pos.relative(dir));
                if (side.getBlock() == target.getBlock() && isFullFluid(side)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isValidLocation(final BlockPos pos, final boolean mustBeAdjacent, final BlockState target) {
        return isValidLocation(pos.getX(), pos.getY(), pos.getZ(), mustBeAdjacent, target);
    }

    // --- tank access (capability adapters land with the pipe/relay cards) ---

    public int fill(@Nullable final Fluid fluid, final int amount, final boolean doFill) {
        if (fluid == null || amount <= 0) {
            return 0;
        }
        if (this.tankFluid != null && !this.tankFluid.equals(fluid.location())) {
            return 0;
        }
        final int filled = Math.min(amount, TANK_CAPACITY - this.tankAmount);
        if (filled > 0 && doFill) {
            this.tankFluid = fluid.location();
            this.tankAmount += filled;
            this.level.blockEntityChanged(this.worldPosition);
            this.setChanged();
        }
        return filled;
    }

    public int drain(final int maxDrain, final boolean doDrain) {
        final int drained = Math.min(maxDrain, this.tankAmount);
        if (drained > 0 && doDrain) {
            this.tankAmount -= drained;
            if (this.tankAmount <= 0) {
                this.tankAmount = 0;
                this.tankFluid = null;
            }
            this.level.blockEntityChanged(this.worldPosition);
            this.setChanged();
        }
        return drained;
    }

    public boolean canFill(final Direction from) {
        return from != Direction.UP;
    }

    public boolean canDrain(final Direction from) {
        return from != Direction.UP;
    }

    // --- inventory: one bath-salt slot (sides except DOWN) ---

    private static boolean isBathSalts(final ItemStack stack) {
        return stack.is(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "itembathsalts")));
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot == 0 ? nullSafe(this.itemStacks[0]) : ItemStack.EMPTY;
    }

    private static ItemStack nullSafe(final ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack;
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        if (slot == 0 && nullSafe(this.itemStacks[0]).isEmpty() == false) {
            final ItemStack current = this.itemStacks[0];
            final ItemStack removed;
            if (current.getCount() <= count) {
                removed = current;
                this.itemStacks[0] = null;
            } else {
                removed = current.split(count);
            }
            setChanged();
            return removed;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (slot == 0 && nullSafe(this.itemStacks[0]).isEmpty() == false) {
            final ItemStack stack = this.itemStacks[0];
            this.itemStacks[0] = null;
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (slot == 0) {
            this.itemStacks[0] = stack;
            if (stack != null && stack.getCount() > stack.getMaxStackSize()) {
                stack.setCount(stack.getMaxStackSize());
            }
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return isBathSalts(stack);
    }

    @Override
    public int[] getSlotsForFace(final Direction side) {
        return side != Direction.DOWN ? new int[]{0} : new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, @Nullable final Direction side) {
        return side != Direction.DOWN;
    }

    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
        return side != Direction.DOWN;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.thaumcraft.spa");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(final int id,
            final net.minecraft.world.entity.player.Inventory inventory, final Player player) {
        return new com.jonnyutah12.thaumcraftclassic.menus.SpaMenu(id, inventory, this);
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getCenter()) <= 64.0;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("mix", this.mix);
        if (this.tankFluid != null && this.tankAmount > 0) {
            tag.putString("FluidName", this.tankFluid.toString());
            tag.putInt("Amount", this.tankAmount);
        }
        final ListTag items = new ListTag();
        for (int i = 0; i < this.itemStacks.length; i++) {
            if (nullSafe(this.itemStacks[i]).isEmpty() == false) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                this.itemStacks[i].save(entry);
                items.add(entry);
            }
        }
        tag.put("Items", items);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mix = tag.getBoolean("mix");
        this.tankFluid = tag.contains("FluidName") ? ResourceLocation.parse(tag.getString("FluidName")) : null;
        this.tankAmount = tag.getInt("Amount");
        this.itemStacks[0] = null;
        for (final Tag entry : tag.getList("Items", Tag.TAG_COMPOUND)) {
            final CompoundTag item = (CompoundTag) entry;
            final int slot = item.getByte("Slot") & 255;
            if (slot == 0) {
                this.itemStacks[0] = ItemStack.of(item);
            }
        }
    }
}
