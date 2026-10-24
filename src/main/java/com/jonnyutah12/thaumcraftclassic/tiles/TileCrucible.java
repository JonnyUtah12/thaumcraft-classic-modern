package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTags;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantDevice;
import com.jonnyutah12.thaumcraftclassic.recipes.CrucibleRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.M405CrucibleContent;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M405Crucible;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604cBlocks;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

/**
 * Crucible (content-mapping.csv {@code thaumcraft:tilecrucible}, hosted on
 * blockMetalDevice variant 0 — the original's metadata 0).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileCrucible}. The entropic
 * heat/ignition machine, decay/overflow steps and the per-unit throw loop
 * live in {@link CrucibleStateMachine} (unit-tested without a Level); this
 * class is the world-facing shell: tank, bellows scan, recipe lookup,
 * research gating, ejection and flux spill. Matching is the original's
 * {@code ThaumcraftCraftingManager.findMatchingCrucibleRecipe} (most
 * distinct aspect tags wins) against the data-driven
 * {@link CrucibleRecipe}s.</p>
 *
 * <p>Original quirks kept: the tick counter is not persisted (resets to
 * −100 on chunk reload); a stack of N items processes ceil(N/2) units per
 * collision; the decay step re-renders even when the pool is empty; the
 * primal re-roll can land on a primal again. The original's
 * {@code EntitySpecialItem} ejection is replaced by a plain ItemEntity
 * stamped with {@link #EJECT_TAG} (the entityInside skip the subclass
 * provided), and the block-instance-wide burn delay of
 * {@code BlockMetalDevice.onEntityCollidedWithBlock} becomes per-tile.</p>
 */
public class TileCrucible extends BlockEntity implements BlockDeviceBase.TickingDeviceTile,
        BlockDeviceBase.DeviceEntityInsideTile, BlockDeviceBase.DeviceItemUseTile,
        BlockDeviceBase.DeviceAnalogTile, BlockDeviceBase.DeviceNeighborTile,
        BlockDeviceBase.DeviceDisplayTickTile {

    /** Entity-data stamp marking a crucible-ejected stack (EntitySpecialItem stand-in). */
    public static final String EJECT_TAG = "ThCrucibleEject";
    /** Entity-data key the thrower tag is stored under (original EventHandlerEntity). */
    public static final String THROWER_TAG = "thrower";

    private final CrucibleStateMachine sm = new CrucibleStateMachine();
    /** Water-only tank, capacity 1000 (original FluidTank(WATER, 0, 1000)). */
    private final FluidTank tank = new FluidTank(CrucibleStateMachine.TANK_CAPACITY,
            stack -> stack.is(FluidType.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int bellows = -1;
    private int burnDelay = 0;

    public TileCrucible(final BlockPos pos, final BlockState state) {
        super(M405Crucible.TILE_CRUCIBLE.get(), pos, state);
    }

    public CrucibleStateMachine state() {
        return this.sm;
    }

    public FluidTank tank() {
        return this.tank;
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void serverTick() {
        this.sm.setCounter(this.sm.counter() + 1);
        if (this.bellows < 0) {
            getBellows();
        }
        final var step = this.sm.heatStep(this.tank.getFluidAmount() > 0, heatSourceBelow(), this.bellows);
        if (step.ignited() || step.extinguished()) {
            setChanged();
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
        if (this.sm.overflowDue()) {
            this.sm.takeRandomFromSource(this.level.random);
            spill();
        }
        if (this.sm.decayDue()) {
            final var outcome = this.sm.decayStep(this.level.random);
            if (outcome != CrucibleStateMachine.DecayOutcome.NOTHING) {
                this.tank.drain(CrucibleStateMachine.DECAY_DRAIN, IFluidHandler.FluidAction.EXECUTE);
                if (outcome == CrucibleStateMachine.DecayOutcome.PRIMAL_SPILLED) {
                    spill();
                }
            }
            // Original markDirty + re-render fires even when the pool was empty.
            setChanged();
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Original heat-source test below: lava, fire, or a nitor (blockAiry 1). */
    private boolean heatSourceBelow() {
        final BlockState below = this.level.getBlockState(this.worldPosition.below());
        if (below.is(Blocks.LAVA) || below.is(Blocks.FIRE) || below.is(Blocks.SOUL_FIRE)) {
            return true;
        }
        final Block airy = M604cBlocks.BLOCK_AIRY.get();
        return below.is(airy) && ((VariantDevice) airy).variant(below) == 1;
    }

    /** Original {@code getBellows()}: count blockWoodenDevice 0 on the four sides. */
    public void getBellows() {
        int count = 0;
        final Block wooden = M604cBlocks.BLOCK_WOODEN_DEVICE.get();
        for (final Direction d : Direction.Plane.HORIZONTAL) {
            final BlockState s = this.level.getBlockState(this.worldPosition.relative(d));
            if (s.is(wooden) && ((VariantDevice) wooden).variant(s) == 0) {
                count++;
            }
        }
        this.bellows = count;
    }

    @Override
    public void deviceNeighborChanged() {
        // Original BlockMetalDevice.neighborChanged rescans immediately.
        getBellows();
    }

    @Override
    public void deviceDisplayTick(final net.minecraft.util.RandomSource rand) {
        // Original randomDisplayTick: occasional lavapop while boiling with fluid.
        if (rand.nextInt(10) == 0 && this.tank.getFluidAmount() > 0 && this.sm.heat() > 150) {
            this.level.playLocalSound(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5,
                    this.worldPosition.getZ() + 0.5, SoundEvents.LAVA_POP, SoundSource.BLOCKS,
                    0.1F + rand.nextFloat() * 0.1F, 1.2F + rand.nextFloat() * 0.2F, false);
        }
    }

    // ------------------------------------------------------- item collision

    @Override
    public void deviceEntityInside(final Entity entity) {
        if (entity instanceof ItemEntity item && !entity.isRemoved()
                && !item.getEntityData().getBoolean(EJECT_TAG)) {
            if (this.sm.heat() > 150 && this.tank.getFluidAmount() > 0) {
                attemptSmelt(item);
            }
        } else if (entity instanceof net.minecraft.world.entity.LivingEntity) {
            // Original BlockMetalDevice.onEntityCollidedWithBlock else-branch:
            // living entities standing in a boiling crucible burn every 10 ticks.
            if (this.sm.heat() > 150 && this.tank.getFluidAmount() > 0) {
                if (++this.burnDelay >= 10) {
                    this.burnDelay = 0;
                    entity.hurt(this.level.damageSources().inFire(), 1.0F);
                }
            } else {
                this.burnDelay = 0;
            }
        }
    }

    /**
     * Faithful port of {@code attemptSmelt}: per-unit recipe match (research
     * gated by the thrower tag) or essentia absorption; a tagless item
     * bounces out and aborts the pass.
     */
    public void attemptSmelt(final ItemEntity entity) {
        final ItemStack item = entity.getItem();
        final String username = entity.getEntityData().getString(THROWER_TAG);
        final ServerPlayer thrower = username.isEmpty() || this.level.getServer() == null
                ? null : this.level.getServer().getPlayerList().getPlayerByName(username);
        final var recipes = this.level.getRecipeManager()
                .getAllRecipesFor(M405CrucibleContent.CRUCIBLE_TYPE.get());
        final ItemStack probe = item.copy();
        probe.setCount(1);

        final var result = this.sm.attemptSmelt(item.getCount(), new CrucibleStateMachine.SmeltHooks() {
            @Override
            public boolean hasFluid() {
                return TileCrucible.this.tank.getFluidAmount() > 0;
            }

            @Override
            public Object tryMatch() {
                final var in = new CrucibleRecipe.CrucibleInput(probe, TileCrucible.this.sm.aspects(), thrower);
                return CrucibleStateMachine.findBestMatch(recipes,
                        holder -> holder.value().aspectKeyCount(),
                        holder -> holder.value().researchUnlocked(thrower)
                                && holder.value().matches(in, TileCrucible.this.level));
            }

            @Override
            @Nullable
            public AspectList itemTags() {
                final AspectList ot = EssentiaTags.getObjectTags(item);
                return EssentiaTags.getBonusTags(item, ot);
            }

            @Override
            public void craft(final Object match) {
                final CrucibleRecipe recipe = ((net.minecraft.world.item.crafting.RecipeHolder<CrucibleRecipe>) match).value();
                final ItemStack out = recipe.getResultItem(TileCrucible.this.level.registryAccess()).copy();
                if (thrower != null) {
                    // Original FMLCommonHandler.firePlayerCraftingEvent (research
                    // triggers + warp on craft run through the port's listener).
                    NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                            thrower, out, new SimpleContainer(item.copy())));
                }
                TileCrucible.this.sm.setAspects(recipe.removeMatching(TileCrucible.this.sm.aspects()));
                TileCrucible.this.tank.drain(CrucibleStateMachine.CRAFT_DRAIN, IFluidHandler.FluidAction.EXECUTE);
                ejectItem(out);
            }
        });

        if (result.bounced()) {
            entity.setDeltaMovement(
                    (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F,
                    0.35F,
                    (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F);
            // Original "random.pop" was removed from vanilla; no port sound maps it.
            return;
        }
        if (result.bubble()) {
            this.level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    ModSounds.BUBBLE.get(), SoundSource.BLOCKS, 0.2F,
                    1.0F + this.level.random.nextFloat() * 0.4F);
            setChanged();
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
        if (result.event()) {
            setChanged();
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
        if (result.remaining() <= 0) {
            entity.discard();
        } else {
            final ItemStack rest = item.copy();
            rest.setCount(result.remaining());
            entity.setItem(rest);
        }
        setChanged();
    }

    /**
     * Original {@code ejectItem}: split into max-stack chunks at (x+0.5,
     * y+0.71, z+0.5), first chunk straight up (vy 0.1), later chunks with
     * tiny jitter; stamped so the crucible never re-absorbs its own output.
     */
    private void ejectItem(final ItemStack items) {
        boolean first = true;
        final ItemStack remaining = items.copy();
        while (!remaining.isEmpty()) {
            final ItemStack chunk = remaining.split(remaining.getMaxStackSize());
            final ItemEntity entityitem = new ItemEntity(this.level,
                    this.worldPosition.getX() + 0.5F, this.worldPosition.getY() + 0.71F,
                    this.worldPosition.getZ() + 0.5F, chunk);
            entityitem.setDeltaMovement(0.0D, 0.1D, 0.0D);
            if (!first) {
                entityitem.setDeltaMovement(
                        (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.01F,
                        0.1D,
                        (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.01F);
            }
            entityitem.getEntityData().putBoolean(EJECT_TAG, true);
            this.level.addFreshEntity(entityitem);
            first = false;
        }
    }

    // ------------------------------------------------------------ bucket fill

    @Override
    public ItemInteractionResult useWithItem(final net.minecraft.server.level.ServerLevel level,
                                            final BlockPos pos, final ServerPlayer player,
                                            final net.minecraft.world.InteractionHand hand) {
        if (this.tank.getFluidAmount() >= this.tank.getCapacity()) {
            // Original returns "handled, no-op" when the tank is already full.
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (FluidUtil.interactWithFluidHandler(player, hand, this.tank)) {
            setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
            // Original "game.neutral.swim" was removed from vanilla; closest
            // surviving neutral splash cue.
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.33F,
                    1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.3F);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // ------------------------------------------------------------- comparator

    @Override
    public int getComparatorSignal() {
        return CrucibleStateMachine.comparatorSignal(this.sm.aspects().visSize());
    }

    // ------------------------------------------------------------------ spill

    /** Original {@code spill()}: 1-in-4 gate, flux above, else flux-meta bump, else 3x3x3. */
    public void spill() {
        if (!CrucibleStateMachine.spillGate(this.level.random)) {
            return;
        }
        final BlockPos above = this.worldPosition.above();
        if (this.level.isEmptyBlock(above)) {
            placeFlux(above, this.level.random.nextBoolean() ? "blockfluxgas" : "blockfluxgoo");
            return;
        }
        final BlockState current = this.level.getBlockState(above);
        final Block goo = BuiltInRegistries.BLOCK.get(rl("blockfluxgoo"));
        final Block gas = BuiltInRegistries.BLOCK.get(rl("blockfluxgas"));
        final IntegerProperty age = current.getBlock() == goo || current.getBlock() == gas
                ? ageProperty(current) : null;
        if (age != null && current.getValue(age) < 7) {
            this.level.setBlock(above, current.setValue(age, current.getValue(age) + 1), 3);
        } else {
            final int x = -1 + this.level.random.nextInt(3);
            final int y = -1 + this.level.random.nextInt(3);
            final int z = -1 + this.level.random.nextInt(3);
            final BlockPos target = this.worldPosition.offset(x, y, z);
            if (this.level.isEmptyBlock(target)) {
                placeFlux(target, this.level.random.nextBoolean() ? "blockfluxgas" : "blockfluxgoo");
            }
        }
    }

    @Nullable
    private static IntegerProperty ageProperty(final BlockState state) {
        for (final net.minecraft.world.level.block.state.properties.Property<?> p
                : state.getBlock().getStateDefinition().getProperties()) {
            if (p.getName().equals("age") && p instanceof IntegerProperty ip) {
                return ip;
            }
        }
        return null;
    }

    /** Flux blocks belong to a later catalog card; place them only when registered. */
    private void placeFlux(final BlockPos pos, final String id) {
        BuiltInRegistries.BLOCK.getOptional(rl(id)).ifPresent(block -> {
            if (block != Blocks.AIR) {
                this.level.setBlock(pos, block.defaultBlockState(), 3);
            }
        });
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    /**
     * Original {@code spillRemnants} (block break / sneak wand-use): empty
     * tank and pool, one flux spill per 2 vis.
     */
    public void spillRemnants() {
        if (this.tank.getFluidAmount() > 0 || this.sm.aspects().visSize() > 0) {
            this.tank.setFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
            final int spills = this.sm.aspects().visSize() / 2;
            for (int a = 0; a < spills; a++) {
                spill();
            }
            this.sm.setAspects(new AspectList());
            setChanged();
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Original onBlockRemoved spilled remnants; setRemoved also fires on
        // chunk unload, so only spill when the crucible block is actually gone.
        if (this.level != null && !this.level.isClientSide
                && !this.level.getBlockState(this.worldPosition).is(this.getBlockState().getBlock())) {
            spillRemnants();
        }
    }

    // ------------------------------------------------------------------- NBT

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putShort("Heat", (short) this.sm.heat());
        this.tank.writeToNBT(registries, tag);
        this.sm.aspects().writeToNBT(tag);
        // Original does not persist the tick counter or bellows count.
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.sm.setHeat(tag.getShort("Heat"));
        this.tank.readFromNBT(registries, tag);
        this.sm.aspects().readFromNBT(tag);
    }

    // TODO(TC-M5): sneak wand-use -> spillRemnants (original IWandable); the
    // wand right-click seam lands with the wand pedestal card.
}
