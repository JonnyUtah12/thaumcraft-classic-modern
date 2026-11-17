package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.recipes.SmeltingBonuses;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The infernal furnace body (content-mapping.csv
 * {@code thaumcraft:tilearcanefurnace}) — the essentia extraction source: it
 * smelts inputs fast and throws the smelting bonus drops that the rest of the
 * essentia chain feeds on.
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcaneFurnace}. The
 * behaviour is ported cell for cell: a 32-slot inventory with a 64-stack
 * limit; inputs that have no smelt result are destroyed on the spot (the
 * original's fizz + lava puff); one item is consumed per cook and its result
 * is <em>ejected</em> out of the nozzle side, together with the
 * {@link SmeltingBonuses} roll for that input and the furnace's
 * experience; the cook time is
 * {@code (speedyTime > 0 ? 80 : 140) - 20 * bellows} (capped at 3 bellows) and
 * counts down; {@code speedyTime} decays one per tick while cooking and is
 * topped up by a FIRE vis drain of 5 (the nozzle's essentia draw is the
 * source that is live today, see {@link #drainVisFire()}).</p>
 *
 * <p>Every rate lives in {@link ArcaneFurnaceLogic} and
 * {@link SmeltingBonuses}, which is where ArcaneFurnaceLogicTest and
 * SmeltingBonusesTest pin them (the card's "extraction-rate" evidence).</p>
 *
 * <h2>Not in this slice (named, with their cards)</h2>
 * <ul>
 *   <li>the original is a <b>3×3×3 multiblock</b> (metadata 1-9 are the shell
 *   blocks, dropped as nether brick / quartz, and a complete shell spawns the
 *   bound blaze with its 5-minute invisibility and 10-minute fire
 *   resistance). This slice ships the base (variant 0) and the nozzle
 *   (variant 10 → {@code variant} 1) as ordinary single blocks: the shell's
 *   ten metadata variants, the structure check with its
 *   {@code restoreBlocks} rollback and the blaze summon are TC-M4-06-m;</li>
 *   <li>the FIRE vis drain: {@code VisNetHandler} is TC-M5 and has no ported
 *   counterpart, so {@link #drainVisFire()} returns 0 today; the seam is the
 *   same one {@code TileArcaneBore} uses. The fast-cook path is reachable
 *   through the nozzle's essentia draw, which <em>is</em> wired;</li>
 *   <li>the client lava animation, the {@code liquid.lavapop} loop and the
 *   {@code random.fizz} on destroy ride with TC-M7-01 — the sounds that do
 *   exist in 1.21.1 are played;</li>
 *   <li>the original's {@code getDrops} for the shell metas (nether brick,
 *   quartz, and a vanilla furnace for the nozzle) belongs to the shell, so the
 *   base and the nozzle drop themselves.</li>
 * </ul>
 */
public class TileArcaneFurnace extends BlockEntity
        implements BlockDeviceBase.TickingDeviceTile, net.minecraft.world.Container, MenuProvider {

    private final ItemStack[] items = new ItemStack[ArcaneFurnaceLogic.SLOTS];

    public int furnaceCookTime = 0;
    public int furnaceMaxCookTime = 0;
    public int speedyTime = 0;
    /** Original {@code facingX}/{@code facingZ}: the eject direction, from the nozzle. */
    public int facingX = -5;
    public int facingZ = -5;

    public TileArcaneFurnace(final BlockPos pos, final BlockState state) {
        super(CatalogEssentiaStorage.TILE_ARCANE_FURNACE.get(), pos, state);
        clearInventory();
    }

    protected TileArcaneFurnace(final net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        clearInventory();
    }

    /**
     * The original's inventory is a {@code ItemStack[]} of {@code null}s; the
     * port uses {@link ItemStack#EMPTY} for the empty slots, so every read
     * path (tick, save, GUI-less insert) can dereference a slot without a
     * null check. Both constructors and the load path call this.
     */
    private void clearInventory() {
        for (int a = 0; a < items.length; a++) {
            items[a] = ItemStack.EMPTY;
        }
    }

    // --- inventory (original IInventory surface, minus the GUI) ------------

    public int getSizeInventory() {
        return ArcaneFurnaceLogic.SLOTS;
    }

    public ItemStack getStackInSlot(final int slot) {
        return items[slot];
    }

    public void setInventorySlotContents(final int slot, final ItemStack stack) {
        items[slot] = stack == null ? ItemStack.EMPTY : stack;
        if (!items[slot].isEmpty() && items[slot].getCount() > ArcaneFurnaceLogic.STACK_LIMIT) {
            items[slot].setCount(ArcaneFurnaceLogic.STACK_LIMIT);
        }
        setChanged();
    }

    public ItemStack decrStackSize(final int slot, final int count) {
        final ItemStack stack = items[slot];
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack out;
        if (stack.getCount() <= count) {
            out = stack.copy();
            items[slot] = ItemStack.EMPTY;
        } else {
            out = stack.split(count);
        }
        setChanged();
        return out;
    }

    // --- Container surface for the gui_id 9 menu (TC-M7-04-m1) -------------

    /** The tile's inventory width; the GUI exposes only slots 0 and 1 (the original container). */
    public static final int SLOTS = ArcaneFurnaceLogic.SLOTS;

    @Override
    public int getContainerSize() {
        return getSizeInventory();
    }

    @Override
    public boolean isEmpty() {
        for (int a = 0; a < items.length; a++) {
            if (!items[a].isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        return decrStackSize(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        final ItemStack stack = items[slot];
        items[slot] = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        setInventorySlotContents(slot, stack);
    }

    @Override
    public void clearContent() {
        clearInventory();
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return ArcaneFurnaceLogic.STACK_LIMIT;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        // GUI slot 0 takes exactly the smeltable class (the original's
        // SlotLimitedHasAspects analogue); the tile's own intake,
        // addItemsToInventory, keeps its original rules untouched.
        return slot != 0 || canSmeltStack(stack);
    }

    /** The stack class the input slot and shift-click fill accept. */
    public boolean canSmeltStack(final ItemStack stack) {
        return !stack.isEmpty() && smeltFor(stack).isPresent();
    }

    /** Cook progress for the menu's data slot 0 (the original syncs this to the GUI). */
    public int getFurnaceCookTime() {
        return furnaceCookTime;
    }

    /** Menu-side mirror of the synced cook progress (client copy only). */
    public void setFurnaceCookTime(final int value) {
        furnaceCookTime = value;
    }

    /** Cook duration for the menu's data slot 1. */
    public int getFurnaceMaxCookTime() {
        return furnaceMaxCookTime;
    }

    /** Menu-side mirror of the synced cook duration (client copy only). */
    public void setFurnaceMaxCookTime(final int value) {
        furnaceMaxCookTime = value;
    }

    @Override
    public boolean stillValid(final Player player) {
        // The TileSpa seam: a player 8+ blocks away closes the window.
        if (level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    // --- MenuProvider (gui_id 9, TC-M7-04-m1) ------------------------------

    @Override
    public Component getDisplayName() {
        // The original tile names this window container.alchemyfurnace
        // ("Alchemical Furnace" in the 1.7.10 lang); the port keys GUI titles
        // by gui id, the container.thaumcraft.gui_* convention.
        return Component.translatable("container.thaumcraft.gui_alchemy_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(final int windowId, final Inventory playerInventory,
                                            final Player player) {
        return new com.jonnyutah12.thaumcraftclassic.menus.AlchemyFurnaceMenu(
                windowId, playerInventory, this);
    }

    // --- ticking (original updateEntity) ----------------------------------

    @Override
    public void serverTick() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (this.facingX == -5) {
            getFacing();
        }

        boolean cookedflag = false;
        if (this.furnaceCookTime > 0) {
            this.furnaceCookTime -= 1;
            cookedflag = true;
        }
        if (cookedflag && this.speedyTime > 0) {
            this.speedyTime -= ArcaneFurnaceLogic.SPEEDY_DECAY;
        }
        if (this.speedyTime <= 0) {
            this.speedyTime = drainVisFire();
        }
        if (this.furnaceMaxCookTime == 0) {
            this.furnaceMaxCookTime = calcCookTime();
        }
        if (this.furnaceCookTime > this.furnaceMaxCookTime) {
            this.furnaceCookTime = this.furnaceMaxCookTime;
        }

        if (this.furnaceCookTime == 0 && cookedflag) {
            finishCook();
        }
        if (this.furnaceCookTime == 0 && !cookedflag) {
            startCook();
        }
    }

    /** Original: the tick the cook hit 0 — eject the first smeltable slot's result. */
    private void finishCook() {
        for (int a = 0; a < getSizeInventory(); a++) {
            final ItemStack slot = items[a];
            if (slot.isEmpty()) {
                continue;
            }
            final Optional<RecipeHolder<SmeltingRecipe>> holder = smeltFor(slot);
            if (holder.isEmpty()) {
                continue;
            }
            final SmeltingRecipe recipe = holder.get().value();
            final ItemStack result = recipe.getResultItem(this.level.registryAccess()).copy();
            ejectItem(result, slot, recipe.getExperience());
            slot.shrink(1);
            if (slot.getCount() <= 0) {
                items[a] = ItemStack.EMPTY;
            }
            this.level.playSound(null, worldPosition,
                    getBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.7F, 0.8F);
            setChanged();
            break;
        }
    }

    /** Original: idle with something smeltable in the inventory — start a cook. */
    private void startCook() {
        for (int a = 0; a < getSizeInventory(); a++) {
            if (!items[a].isEmpty() && canSmelt(a)) {
                this.furnaceMaxCookTime = calcCookTime();
                this.furnaceCookTime = this.furnaceMaxCookTime;
                setChanged();
                break;
            }
        }
    }

    /** Original {@code calcCookTime}. */
    public int calcCookTime() {
        return ArcaneFurnaceLogic.cookTime(this.speedyTime, getBellows());
    }

    /**
     * Original {@code getBellows}: a bellows two cells out on any face but the
     * top, pointing back at the furnace, counts; the cap is 3. The original's
     * extra {@code !isAirBlock} guard on the bellows' own cell is always true
     * for a placed bellows, so it is not reproduced.
     */
    public int getBellows() {
        int bellows = 0;
        for (final Direction dir : Direction.values()) {
            if (dir == Direction.UP) {
                continue;
            }
            final BlockPos at = worldPosition.relative(dir, 2);
            final BlockEntity be = this.level == null ? null : this.level.getBlockEntity(at);
            if (be instanceof TileBellows b && b.orientation == dir.getOpposite()) {
                bellows++;
            }
        }
        return ArcaneFurnaceLogic.bellows(bellows);
    }

    /**
     * Original {@code VisNetHandler.drainVis(world, pos, Aspect.FIRE, 5)} —
     * the drained amount <em>is</em> speedyTime. The vis net is TC-M5 and has
     * no ported counterpart, so this returns 0 (the same seam
     * {@code TileArcaneBore.drainVisEntropy} leaves); TC-M5 overrides it.
     */
    protected int drainVisFire() {
        return 0;
    }

    /** Original {@code canSmelt}. */
    public boolean canSmelt(final int slot) {
        return !items[slot].isEmpty() && smeltFor(items[slot]).isPresent();
    }

    /** Original {@code FurnaceRecipes.instance().getSmeltingResult}. */
    public Optional<RecipeHolder<SmeltingRecipe>> smeltFor(final ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), this.level);
    }

    /**
     * Original {@code addItemsToInventory}: merge into a matching slot,
     * otherwise take the first free one — and destroy the item right away if
     * it has no smelt result. True means the stack was taken in.
     */
    public boolean addItemsToInventory(final ItemStack incoming) {
        if (incoming.isEmpty()) {
            return false;
        }
        for (int a = 0; a < getSizeInventory(); a++) {
            final ItemStack slot = items[a];
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, incoming)
                    && slot.getCount() + incoming.getCount() <= incoming.getMaxStackSize()) {
                slot.grow(incoming.getCount());
                if (!canSmelt(a)) {
                    destroyItem(a);
                }
                setChanged();
                return true;
            }
            if (slot.isEmpty()) {
                setInventorySlotContents(a, incoming.copy());
                if (!canSmelt(a)) {
                    destroyItem(a);
                }
                setChanged();
                return true;
            }
        }
        return false;
    }

    /** Original {@code destroyItem}: the item is consumed with a fizz. */
    private void destroyItem(final int slot) {
        items[slot] = ItemStack.EMPTY;
        if (this.level != null && !this.level.isClientSide) {
            this.level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH,
                    SoundSource.BLOCKS, 0.3F, 2.6F + (this.level.random.nextFloat()
                            - this.level.random.nextFloat()) * 0.8F);
        }
    }

    /**
     * Original {@code ejectItem}: throw the result out of the nozzle side,
     * roll the smelting bonus for the input, and pay the experience.
     */
    public void ejectItem(final ItemStack result, final ItemStack input, final float xp) {
        if (result.isEmpty() || this.level == null) {
            return;
        }
        final RandomSource random = this.level.random;
        final int bellows = getBellows();
        final double lx = ArcaneFurnaceLogic.ejectOffsetAxis(this.facingX);
        final double lz = ArcaneFurnaceLogic.ejectOffsetAxis(this.facingZ);
        final double x = worldPosition.getX() + lx;
        final double y = worldPosition.getY() + 0.4D;
        final double z = worldPosition.getZ() + lz;

        spawnDrop(new ItemEntity(this.level, x, y, z, result.copy()),
                ArcaneFurnaceLogic.ejectVelocity(this.facingX, random),
                ArcaneFurnaceLogic.ejectVelocity(this.facingZ, random));

        final SmeltingBonuses.Bonus bonus = SmeltingBonuses.getBonus(input);
        final ItemStack bonusStack = SmeltingBonuses.rollStack(bonus, bellows, random);
        if (!bonusStack.isEmpty()) {
            spawnDrop(new ItemEntity(this.level, x, y, z, bonusStack),
                    ArcaneFurnaceLogic.ejectVelocity(this.facingX, random),
                    ArcaneFurnaceLogic.ejectVelocity(this.facingZ, random));
        }

        final int orbs = ArcaneFurnaceLogic.experience(result.getCount(), xp, random);
        if (orbs > 0 && this.level instanceof ServerLevel serverLevel) {
            ExperienceOrb.award(serverLevel, new Vec3(x, y, z), orbs);
        }
    }

    private void spawnDrop(final ItemEntity entity, final double mx, final double mz) {
        entity.setDeltaMovement(mx, 0.0D, mz);
        this.level.addFreshEntity(entity);
    }

    /**
     * Original {@code getFacing}: the eject direction is set by the adjacent
     * nozzle (original metadata 10), west first, defaulting to south.
     */
    private void getFacing() {
        this.facingX = 0;
        this.facingZ = 0;
        if (isNozzleAt(worldPosition.west())) {
            this.facingX = -1;
        } else if (isNozzleAt(worldPosition.east())) {
            this.facingX = 1;
        } else if (isNozzleAt(worldPosition.north())) {
            this.facingZ = -1;
        } else {
            this.facingZ = 1;
        }
    }

    private boolean isNozzleAt(final BlockPos pos) {
        if (this.level == null) {
            return false;
        }
        final BlockState state = this.level.getBlockState(pos);
        return state.is(CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get())
                && state.getValue(BlockArcaneFurnace.VARIANT) == BlockArcaneFurnace.VARIANT_NOZZLE;
    }

    // --- entity handling (original block entityUpdate, meta 0) ------------

    /**
     * Original {@code onEntityCollidedWithBlock} on the base plate: items
     * resting on it are lifted into the inventory, and anything that walks
     * into it (that cannot fly) is burnt and popped.
     */
    public void deviceEntityInside(final net.minecraft.world.entity.Entity entity) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        final double dx = centreDelta(entity.getX(), worldPosition.getX() + 0.5D);
        final double dz = centreDelta(entity.getZ(), worldPosition.getZ() + 0.5D);
        if (entity instanceof ItemEntity item) {
            entity.setDeltaMovement(dx, 0.025D, dz);
            if (entity.onGround() && addItemsToInventory(item.getItem())) {
                item.discard();
            }
        } else if (entity instanceof net.minecraft.world.entity.LivingEntity living
                && !(living instanceof net.minecraft.world.entity.player.Player player
                        && player.getAbilities().flying)) {
            entity.hurt(this.level.damageSources().inFire(), 3.0F);
            // Original's knockback: a jump off the plate.
            living.setDeltaMovement(living.getDeltaMovement().x, 0.42D, living.getDeltaMovement().z);
        }
    }

    private static double centreDelta(final double entityAxis, final double centre) {
        if (entityAxis < centre - 0.2D) {
            return 0.025D;
        }
        if (entityAxis > centre + 0.2D) {
            return -0.025D;
        }
        return 0.0D;
    }

    // --- persistence (original NBT keys) ----------------------------------

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putShort("CookTime", (short) this.furnaceCookTime);
        tag.putShort("SpeedyTime", (short) this.speedyTime);
        final ListTag list = new ListTag();
        for (int a = 0; a < getSizeInventory(); a++) {
            if (!items[a].isEmpty()) {
                final CompoundTag itemTag = (CompoundTag) items[a].save(registries);
                itemTag.putByte("Slot", (byte) a);
                list.add(itemTag);
            }
        }
        tag.put("Items", list);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        clearInventory();
        final ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag itemTag = list.getCompound(i);
            final int slot = itemTag.getByte("Slot") & 0xFF;
            if (slot < getSizeInventory()) {
                items[slot] = ItemStack.parseOptional(registries, itemTag);
            }
        }
        this.furnaceCookTime = tag.getShort("CookTime");
        this.speedyTime = tag.getShort("SpeedyTime");
    }
}
