package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.blocks.VariantDevice;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604cBlockEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.ContainerHelper;

import com.jonnyutah12.thaumcraftclassic.recipes.SmeltingBonuses;

/**
 * Arcane furnace core (content-mapping.csv {@code thaumcraft:tilearcanefurnace},
 * hosted on blockArcaneFurnace variant 0).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcaneFurnace}. A 32-slot
 * smelting multiblock: items thrown into the structure are absorbed, smelted
 * with vanilla smelting recipes, and ejected out the opening (the variant-10
 * iron-bars part determines the facing). No fuel — heat comes from ambient
 * Fire vis (drained each tick) or Fire essentia pushed in through a nozzle.
 * Bellows facing the structure two blocks away cut the cook time (140 →
 * 80 with vis speed, −20 per bellows, max 3) and raise the smelting-bonus
 * odds (25% bare, +44% per bellows).</p>
 *
 * <p>Deferred: the ambient Fire-vis drain is a stub returning 0 until the vis
 * network lands (TC-M4-05); the lava-bubble client effect (block event 3) is
 * TC-M7-01. The structure itself is assembled by the wand (TC-M6-01).</p>
 */
public class TileArcaneFurnace extends BlockEntity {

    public static final int SLOTS = 32;

    protected ItemStack[] furnaceItemStacks = new ItemStack[SLOTS];
    public int furnaceCookTime = 0;
    public int furnaceMaxCookTime = 0;
    public int speedyTime = 0;
    public int facingX = -5;
    public int facingZ = -5;

    public TileArcaneFurnace(final BlockPos pos, final BlockState state) {
        this(M604cBlockEntities.TILE_ARCANE_FURNACE.get(), pos, state);
    }

    public TileArcaneFurnace(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public void serverTick() {
        if (facingX == -5) {
            getFacing();
        }
        boolean cookedflag = false;
        if (furnaceCookTime > 0) {
            furnaceCookTime--;
            cookedflag = true;
        }
        if (cookedflag && speedyTime > 0) {
            speedyTime--;
        }
        if (speedyTime <= 0) {
            speedyTime = drainVisFire();
        }
        if (furnaceMaxCookTime == 0) {
            furnaceMaxCookTime = calcCookTime();
        }
        if (furnaceCookTime > furnaceMaxCookTime) {
            furnaceCookTime = furnaceMaxCookTime;
        }
        if (furnaceCookTime == 0 && cookedflag) {
            for (int a = 0; a < SLOTS; a++) {
                if (!furnaceItemStacks[a].isEmpty()) {
                    final ItemStack result = smeltResult(furnaceItemStacks[a]);
                    if (!result.isEmpty()) {
                        ejectItem(result.copy(), furnaceItemStacks[a]);
                        furnaceItemStacks[a].shrink(1);
                        if (furnaceItemStacks[a].isEmpty()) {
                            furnaceItemStacks[a] = ItemStack.EMPTY;
                        }
                        setChanged();
                        break;
                    }
                }
            }
        }
        if (furnaceCookTime == 0 && !cookedflag) {
            for (int ax = 0; ax < SLOTS; ax++) {
                if (canSmelt(ax)) {
                    furnaceMaxCookTime = calcCookTime();
                    furnaceCookTime = furnaceMaxCookTime;
                    break;
                }
            }
        }
    }

    /**
     * Original {@code VisNetHandler.drainVis(FIRE, 5)}: ambient Fire vis buys
     * the furnace speed. Stubbed at 0 until the vis network (TC-M4-05) — the
     * nozzle path below is the working speed source meanwhile.
     */
    protected int drainVisFire() {
        return 0;
    }

    /** Original: bellows two blocks out on any face but the top, max 3. */
    public int getBellows() {
        int bellows = 0;
        for (final Direction dir : Direction.values()) {
            if (dir == Direction.UP) {
                continue;
            }
            final BlockPos at = worldPosition.relative(dir, 2);
            if (level.getBlockEntity(at) instanceof TileBellows b
                    && b.orientation == dir.getOpposite()
                    && !level.hasNeighborSignal(at)) {
                bellows++;
            }
        }
        return Math.min(3, bellows);
    }

    public int calcCookTime() {
        return (speedyTime > 0 ? 80 : 140) - 20 * getBellows();
    }

    /** Original {@code addItemsToInventory}: absorb a thrown/pushed stack. */
    public boolean addItemsToInventory(final ItemStack items) {
        for (int a = 0; a < SLOTS; a++) {
            if (!furnaceItemStacks[a].isEmpty()
                    && ItemStack.isSameItem(furnaceItemStacks[a], items)
                    && furnaceItemStacks[a].getCount() + items.getCount() <= items.getMaxStackSize()) {
                furnaceItemStacks[a].grow(items.getCount());
                if (!canSmelt(a)) {
                    destroyItem(a);
                }
                setChanged();
                return true;
            }
            if (furnaceItemStacks[a].isEmpty()) {
                furnaceItemStacks[a] = items.copy();
                if (!canSmelt(a)) {
                    destroyItem(a);
                }
                setChanged();
                return true;
            }
        }
        return false;
    }

    /** Original {@code destroyItem}: non-smeltable input is fizzed away. */
    private void destroyItem(final int slot) {
        furnaceItemStacks[slot] = ItemStack.EMPTY;
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                0.3F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.LAVA,
                    worldPosition.getX() + level.random.nextDouble(),
                    worldPosition.getY() + 1.0,
                    worldPosition.getZ() + level.random.nextDouble(),
                    0.0, 0.0, 0.0);
        }
    }

    /** Original {@code getFacing}: the adjacent iron-bars part (variant 10) sets the opening. */
    private void getFacing() {
        facingX = 0;
        facingZ = 0;
        for (final Direction dir : new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}) {
            final BlockPos at = worldPosition.relative(dir);
            final BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof VariantDevice device && device.variant(state) == 10) {
                facingX = dir.getStepX();
                facingZ = dir.getStepZ();
                return;
            }
        }
        facingZ = 1;
    }

    /** Original {@code ejectItem}: base output, smelting bonus, XP orbs. */
    public void ejectItem(final ItemStack items, final ItemStack furnaceItemStack) {
        if (items.isEmpty()) {
            return;
        }
        final int bellows = getBellows();
        final double lx = 0.5 + facingX * 1.2;
        final double lz = 0.5 + facingZ * 1.2;
        spawnItem(items, lx, lz);

        final ItemStack bonus = SmeltingBonuses.getSmeltingBonus(furnaceItemStack);
        if (!bonus.isEmpty()) {
            if (bellows == 0) {
                if (level.random.nextInt(4) == 0) {
                    bonus.grow(1);
                }
            } else {
                for (int a = 0; a < bellows; a++) {
                    if (level.random.nextFloat() < 0.44F) {
                        bonus.grow(1);
                    }
                }
            }
            if (bonus.getCount() > 0) {
                spawnItem(bonus, lx, lz);
            }
        }

        // XP: original split the result's experience across orbs the same way
        // vanilla does (ExperienceOrb.getExperienceValue).
        int xpTotal = experienceFor(furnaceItemStack, items);
        while (xpTotal > 0) {
            final int value = ExperienceOrb.getExperienceValue(xpTotal);
            xpTotal -= value;
            final ExperienceOrb xp = new ExperienceOrb(level,
                    worldPosition.getX() + lx, worldPosition.getY() + 0.4, worldPosition.getZ() + lz, value);
            level.addFreshEntity(xp);
        }
    }

    private void spawnItem(final ItemStack stack, final double lx, final double lz) {
        final ItemEntity entityitem = new ItemEntity(level,
                worldPosition.getX() + lx, worldPosition.getY() + 0.4, worldPosition.getZ() + lz, stack);
        entityitem.setDeltaMovement(
                facingX == 0 ? (level.random.nextFloat() - level.random.nextFloat()) * 0.03 : facingX * 0.13,
                0.0,
                facingZ == 0 ? (level.random.nextFloat() - level.random.nextFloat()) * 0.03 : facingZ * 0.13);
        level.addFreshEntity(entityitem);
    }

    /** Vanilla smelting result for a stack in this level, or EMPTY. */
    public ItemStack smeltResult(final ItemStack input) {
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level)
                .map(r -> r.value().getResultItem(level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private int experienceFor(final ItemStack input, final ItemStack output) {
        final float experience = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level)
                .map(r -> r.value().experience()).orElse(0.0F);
        int count = output.getCount();
        if (experience == 0.0F) {
            return 0;
        }
        if (experience < 1.0F) {
            final int floor = (int) (count * experience);
            if (floor < Math.ceil(count * experience) && level.random.nextFloat() < count * experience - floor) {
                count = floor + 1;
            } else {
                count = floor;
            }
        }
        return count;
    }

    public boolean canSmelt(final int slotIn) {
        return !furnaceItemStacks[slotIn].isEmpty() && !smeltResult(furnaceItemStacks[slotIn]).isEmpty();
    }

    /** Absorb item entities that fall into the structure (original block entityInside). */
    public void absorbItems(final AABB area) {
        for (final ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (item.isAlive() && !item.getItem().isEmpty()) {
                if (addItemsToInventory(item.getItem())) {
                    item.discard();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, java.util.Arrays.asList(furnaceItemStacks), "Items", level.registryAccess());
        tag.putShort("CookTime", (short) furnaceCookTime);
        tag.putShort("MaxCookTime", (short) furnaceMaxCookTime);
        tag.putShort("SpeedyTime", (short) speedyTime);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        furnaceItemStacks = new ItemStack[SLOTS];
        final java.util.List<ItemStack> items = new java.util.ArrayList<>();
        ContainerHelper.loadAllItems(tag, items, "Items", level.registryAccess());
        for (int a = 0; a < Math.min(SLOTS, items.size()); a++) {
            furnaceItemStacks[a] = items.get(a);
        }
        furnaceCookTime = tag.getShort("CookTime");
        furnaceMaxCookTime = tag.getShort("MaxCookTime");
        speedyTime = tag.getShort("SpeedyTime");
    }
}
