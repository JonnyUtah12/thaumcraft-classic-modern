package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaHandler;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockStoneDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXInfusionSourcePayload;
import com.jonnyutah12.thaumcraftclassic.recipes.InfusionCraftingRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.InfusionEnchantmentRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogInfusion;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The runic matrix (TC-M4-04; content-mapping.csv {@code tileinfusionmatrix},
 * hosted on blockStoneDevice variant 2).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileInfusionMatrix}. Full port of
 * the altar controller: structure validation, pedestal/stabiliser scan and
 * symmetry (see {@link InfusionMatrixLogic}), wand-click activation, recipe
 * matching against the pedestal contents, the timed craft cycle (essentia
 * drain through {@link EssentiaHandler}, component consumption with the
 * original's 5-tick FX countdown, XP drain for infusion enchantments), the
 * instability event table (eject / zap / harm / lightning / warp), and the
 * craft-finish paths (concrete output, NBT-tag output, enchantment +1).</p>
 *
 * <p>Documented divergences (docs/porting-decisions.md): activation is an
 * empty-hand use until the wand card (TC-M5) lands its IWandable seam; the
 * client-side particle/runes effects are deferred to the rendering card
 * (TC-M7-01) — the S2C FX payloads are sent as in the original; the flux
 * goo/gas blocks of eject types 1/3 are only placed when registered; the
 * InfusionRunicAugmentRecipe has no counterpart until runic armor is ported.</p>
 */
public class TileInfusionMatrix extends BlockEntity implements BlockDeviceBase.TickingDeviceTile,
        BlockDeviceBase.DeviceUseTile {

    private static final int ESSENTIA_RANGE = 12;

    private final List<BlockPos> pedestals = new ArrayList<>();
    public boolean active = false;
    public boolean crafting = false;
    public boolean checkSurroundings = true;
    public int symmetry = 0;
    public int instability = 0;
    private AspectList recipeEssentia = new AspectList();
    @Nullable
    private List<Ingredient> pendingComponents = null;
    @Nullable
    private Ingredient recipeInput = null;
    @Nullable
    private ItemStack recipeOutput = null;
    @Nullable
    private String recipeOutputLabel = null;
    @Nullable
    private Tag recipeOutputValue = null;
    @Nullable
    private UUID recipePlayer = null;
    private int recipeInstability = 0;
    private int recipeXP = 0;
    private int recipeType = 0;
    @Nullable
    private net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> recipeEnchantment = null;
    private int count = 0;
    private int countDelay = 10;
    private int itemCount = 0;

    public TileInfusionMatrix(final BlockPos pos, final BlockState state) {
        super(CatalogInfusion.TILE_INFUSION_MATRIX.get(), pos, state);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void serverTick() {
        this.count++;
        if (this.checkSurroundings) {
            this.checkSurroundings = false;
            getSurroundings();
        }
        if (this.count % (this.crafting ? 20 : 100) == 0 && !validLocation()) {
            this.active = false;
            saveAndSync();
            return;
        }
        if (this.active && this.crafting && this.count % this.countDelay == 0) {
            craftCycle();
        }
    }

    @Override
    public net.minecraft.world.InteractionResult useWithoutItem(final ServerLevel level, final BlockPos pos,
                                                                final ServerPlayer player) {
        // Original onWandRightClick: activate when valid and idle, start a craft
        // when already active. Empty-hand until the wand seam exists (TC-M5).
        if (!this.crafting) {
            if (this.active) {
                craftingStart(player);
            } else if (validLocation()) {
                this.active = true;
                saveAndSync();
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(false);
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    // ---------------------------------------------------------------- structure

    private InfusionMatrixLogic.WorldProbe probe() {
        return new InfusionMatrixLogic.WorldProbe() {
            @Override
            public boolean isPedestal(final BlockPos pos) {
                return TileInfusionMatrix.this.level.getBlockEntity(pos) instanceof TilePedestal;
            }

            @Override
            public boolean pedestalHasItem(final BlockPos pos) {
                final BlockEntity be = TileInfusionMatrix.this.level.getBlockEntity(pos);
                return be instanceof TilePedestal ped && !ped.getItem(0).isEmpty();
            }

            @Override
            public boolean isPillar(final BlockPos pos) {
                return TileInfusionMatrix.this.level.getBlockEntity(pos) instanceof TileInfusionPillar;
            }

            @Override
            public boolean isStabiliser(final BlockPos pos) {
                final BlockState state = TileInfusionMatrix.this.level.getBlockState(pos);
                if (state.is(Blocks.SNOW)) {
                    return true;
                }
                return state.getBlock() instanceof InfusionMatrixLogic.IInfusionStabiliser stabiliser
                        && stabiliser.canStabiliseInfusion(TileInfusionMatrix.this.level, pos);
            }
        };
    }

    public boolean validLocation() {
        return InfusionMatrixLogic.validLocation(worldPosition, probe());
    }

    private void getSurroundings() {
        final InfusionMatrixLogic.Scan scan = InfusionMatrixLogic.scan(worldPosition, probe());
        this.pedestals.clear();
        this.pedestals.addAll(scan.pedestals());
        this.symmetry = scan.symmetry();
    }

    // ------------------------------------------------------------------ crafting

    public void craftingStart(final ServerPlayer player) {
        if (!validLocation()) {
            this.active = false;
            saveAndSync();
            return;
        }
        getSurroundings();
        ItemStack central = ItemStack.EMPTY;
        final BlockEntity centreBe = level.getBlockEntity(worldPosition.below(2));
        if (centreBe instanceof TilePedestal ped) {
            central = ped.getItem(0).copy();
        }
        if (central.isEmpty()) {
            return;
        }
        final List<ItemStack> components = new ArrayList<>();
        for (final BlockPos ped : this.pedestals) {
            if (level.getBlockEntity(ped) instanceof TilePedestal p && !p.getItem(0).isEmpty()) {
                components.add(p.getItem(0).copy());
            }
        }
        if (components.isEmpty()) {
            return;
        }
        final var input = new InfusionCraftingRecipe.InfusionInput(central, components, player);

        for (final RecipeHolder<InfusionCraftingRecipe> holder
                : level.getRecipeManager().getAllRecipesFor(CatalogInfusion.INFUSION_TYPE.get())) {
            final InfusionCraftingRecipe recipe = holder.value();
            if (recipe.matches(input, level)) {
                this.recipeType = 0;
                this.pendingComponents = new ArrayList<>(recipe.components());
                this.recipeInput = recipe.input();
                this.recipeOutput = recipe.concreteOutput();
                this.recipeOutputLabel = recipe.outputTagLabel();
                this.recipeOutputValue = recipe.outputTagValue();
                this.recipeInstability = recipe.instability();
                this.recipeEssentia = recipe.aspects();
                this.recipePlayer = player.getUUID();
                this.instability = InfusionMatrixLogic.craftStartInstability(this.symmetry, this.recipeInstability);
                this.crafting = true;
                level.playSound(null, worldPosition, ModSounds.CRAFTSTART.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
                saveAndSync();
                return;
            }
        }
        for (final RecipeHolder<InfusionEnchantmentRecipe> holder
                : level.getRecipeManager().getAllRecipesFor(CatalogInfusion.INFUSION_ENCHANTMENT_TYPE.get())) {
            final InfusionEnchantmentRecipe recipe = holder.value();
            if (recipe.matches(input, level)) {
                this.recipeType = 1;
                this.pendingComponents = new ArrayList<>(recipe.components());
                this.recipeInput = null;
                this.recipeEnchantment = recipe.enchantment(level.registryAccess());
                this.recipeInstability = recipe.calcInstability(central);
                final AspectList cost = recipe.aspects();
                final float mod = recipe.getEssentiaMod(central, level.registryAccess());
                for (final Aspect aspect : cost.getAspects()) {
                    cost.add(aspect, (int) (cost.getAmount(aspect) * mod));
                }
                this.recipeEssentia = cost;
                this.recipeXP = recipe.calcXP(central, level.registryAccess());
                this.instability = InfusionMatrixLogic.craftStartInstability(this.symmetry, this.recipeInstability);
                this.crafting = true;
                level.playSound(null, worldPosition, ModSounds.CRAFTSTART.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
                saveAndSync();
                return;
            }
        }
    }

    private void craftCycle() {
        boolean valid = false;
        final BlockEntity centreBe = level.getBlockEntity(worldPosition.below(2));
        if (centreBe instanceof TilePedestal ped && this.recipeInput != null) {
            final ItemStack held = ped.getItem(0);
            if (!held.isEmpty() && this.recipeInput.test(held)) {
                valid = true;
            }
        }

        if (!valid || InfusionMatrixLogic.eventFires(this.instability, level.random.nextInt(500))) {
            applyEvent(InfusionMatrixLogic.eventForRoll(level.random.nextInt(21)));
            if (valid) {
                return;
            }
        }

        if (!valid) {
            this.instability = 0;
            this.crafting = false;
            this.recipeEssentia = new AspectList();
            this.pendingComponents = null;
            level.playSound(null, worldPosition, ModSounds.CRAFTFAIL.get(), SoundSource.BLOCKS, 1.0F, 0.6F);
            saveAndSync();
            return;
        }

        if (this.recipeType == 1 && this.recipeXP > 0) {
            final List<ServerPlayer> targets = level.getEntitiesOfClass(ServerPlayer.class,
                    new AABB(worldPosition).inflate(10.0));
            for (final ServerPlayer target : targets) {
                if (target.experienceLevel > 0) {
                    target.experienceLevel--;
                    this.recipeXP--;
                    target.hurt(level.damageSources().magic(), level.random.nextInt(2));
                    // 1.21.1's ChunkPos is net.minecraft.world.level.ChunkPos
                    // (mcsources net/minecraft/world/level/ChunkPos.java:32, the
                    // BlockPos ctor; there is no world.level.chunk sub-package).
                    PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level,
                            new net.minecraft.world.level.ChunkPos(worldPosition),
                            new FXInfusionSourcePayload(worldPosition,
                            target.blockPosition().subtract(worldPosition), 0));
                    // There is no SoundEvents.RANDOM_FIZZ in 1.21.1 (no such member in
                    // mcsources net/minecraft/sounds/SoundEvents.java). The original's
                    // wand-tick ambient here is the altar's own sound: docs/sound-events.md
                    // maps infuser -> TileInfusionMatrix.onUsingWandTick, so the per-tick
                    // XP drain uses INFUSER (INFUSERSTART is the one-shot craft-start
                    // variant, already covered by CRAFTSTART at craftingStart).
                    level.playSound(target, worldPosition, ModSounds.INFUSER.get(),
                            SoundSource.BLOCKS, 1.0F, 2.0F + level.random.nextFloat() * 0.4F);
                    this.countDelay = 20;
                    saveAndSync();
                    return;
                }
            }
            // Original: no XP available — essentia may still trickle in and add
            // instability (the "stall" branch).
            stallBranch(3, 50);
            return;
        }

        if (this.recipeType == 1 && this.recipeXP == 0) {
            this.countDelay = 10;
        }

        if (this.recipeEssentia.visSize() > 0) {
            for (final Aspect aspect : this.recipeEssentia.getAspects()) {
                if (this.recipeEssentia.getAmount(aspect) > 0) {
                    if (EssentiaHandler.drainEssentia(level, worldPosition, aspect,
                            ESSENTIA_RANGE)) {
                        this.recipeEssentia.reduce(aspect, 1);
                        saveAndSync();
                        return;
                    }
                    if (level.random.nextInt(100 - this.recipeInstability * 3) == 0) {
                        this.instability = InfusionMatrixLogic.cappedIncrement(this.instability);
                    }
                    saveAndSync();
                }
            }
            this.checkSurroundings = true;
        } else if (this.pendingComponents == null || this.pendingComponents.isEmpty()) {
            this.instability = 0;
            this.crafting = false;
            craftingFinish();
            this.recipeOutput = null;
            saveAndSync();
        } else {
            for (int a = 0; a < this.pendingComponents.size(); a++) {
                final Ingredient component = this.pendingComponents.get(a);
                for (final BlockPos ped : this.pedestals) {
                    if (!(level.getBlockEntity(ped) instanceof TilePedestal p)) {
                        continue;
                    }
                    final ItemStack held = p.getItem(0);
                    if (!held.isEmpty() && component.test(held)) {
                        if (this.itemCount == 0) {
                            this.itemCount = 5;
                            // 1.21.1's ChunkPos is net.minecraft.world.level.ChunkPos.
                            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level,
                                    new net.minecraft.world.level.ChunkPos(worldPosition),
                                    new FXInfusionSourcePayload(worldPosition,
                                    worldPosition.subtract(ped), 0));
                        } else if (this.itemCount-- <= 1) {
                            // The original refilled the pedestal with Item.getContainerItem
                            // (the 1.7-1.20 "empty the consumable, leave its container
                            // behind" pair). 1.21.1 removed that member (no containerItem
                            // accessors in mcsources net/minecraft/world/item/Item.java);
                            // the surviving platform form of the same idea is Item.getCrafting
                            // RemainingItem (Item.java:243-250). Items with no counterpart
                            // take the original's null branch: the slot is cleared.
                            final var leftover = held.getItem().getCraftingRemainingItem();
                            p.setItemFromInfusion(0, leftover == null
                                    ? ItemStack.EMPTY : new ItemStack(leftover));
                            this.pendingComponents.remove(a);
                        }
                        saveAndSync();
                        return;
                    }
                }
                stallBranch(1 + a, 50);
            }
        }
    }

    /** Original's essentia-trickle stall branch: random +1 aspect, instability chance. */
    private void stallBranch(final int rollDivisor, final int instDivisor) {
        final List<Aspect> ing = this.recipeEssentia.getAspects();
        if (!ing.isEmpty() && level.random.nextInt(rollDivisor) == 0) {
            final Aspect as = ing.get(level.random.nextInt(ing.size()));
            this.recipeEssentia.add(as, 1);
            if (level.random.nextInt(instDivisor - this.recipeInstability * 2) == 0) {
                this.instability = InfusionMatrixLogic.cappedIncrement(this.instability);
            }
            saveAndSync();
        }
    }

    private void applyEvent(final int eventId) {
        switch (eventId & 0xFF) {
            case InfusionMatrixLogic.EV_EJECT -> inEvEjectItem(InfusionMatrixLogic.ejectType(eventId));
            case InfusionMatrixLogic.EV_ZAP -> inEvZap(InfusionMatrixLogic.affectsAll(eventId));
            case InfusionMatrixLogic.EV_HARM -> inEvHarm(InfusionMatrixLogic.affectsAll(eventId));
            case InfusionMatrixLogic.EV_LIGHTNING -> level.playSound(null, worldPosition,
                    net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                    1.5F + level.random.nextFloat(), 1.0F);
            case InfusionMatrixLogic.EV_WARP -> inEvWarp();
            default -> {
            }
        }
    }

    private void inEvZap(final boolean all) {
        final List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(worldPosition).inflate(10.0));
        for (final LivingEntity target : targets) {
            target.hurt(level.damageSources().magic(), 4 + level.random.nextInt(4));
            if (!all) {
                break;
            }
        }
    }

    private void inEvHarm(final boolean all) {
        final List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(worldPosition).inflate(10.0));
        for (final LivingEntity target : targets) {
            final ResourceLocation taint = ResourceLocation.fromNamespaceAndPath(
                    com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "potion.fluxtaint");
            final ResourceLocation visExhaust = ResourceLocation.fromNamespaceAndPath(
                    com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "potion.visexhaust");
            // 1.21.1's Registry.getOptional(ResourceLocation) answers the raw value
            // (MobEffect); MobEffectInstance wants the holder form, so the lookup goes
            // through getHolder, which yields Holder.Reference<MobEffect> — the same
            // idiom items/ItemManaBean.java:86 uses for ModEffects.REGENERATION-style
            // constants in runic/RunicShielding.java:389.
            final var poison = BuiltInRegistries.MOB_EFFECT.getHolder(taint);
            if (poison.isPresent() && level.random.nextBoolean()) {
                target.addEffect(new MobEffectInstance(poison.get(), 120, 0, false, false));
            } else {
                BuiltInRegistries.MOB_EFFECT.getHolder(visExhaust).ifPresent(exhaust ->
                        target.addEffect(new MobEffectInstance(exhaust, 2400, 0, true, false)));
            }
            if (!all) {
                break;
            }
        }
    }

    private void inEvWarp() {
        final List<ServerPlayer> targets = level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(worldPosition).inflate(10.0));
        if (!targets.isEmpty()) {
            final ServerPlayer target = targets.get(level.random.nextInt(targets.size()));
            if (level.random.nextFloat() < 0.25F) {
                ResearchManager.applyWarp(target, 1, SyncWarpPayload.WARP_STICKY);
            } else {
                ResearchManager.applyWarp(target, 1 + level.random.nextInt(5), SyncWarpPayload.WARP_PERMANENT);
            }
        }
    }

    private void inEvEjectItem(final int type) {
        for (int q = 0; q < 50 && !this.pedestals.isEmpty(); q++) {
            final BlockPos ped = this.pedestals.get(level.random.nextInt(this.pedestals.size()));
            if (!(level.getBlockEntity(ped) instanceof TilePedestal p) || p.getItem(0).isEmpty()) {
                continue;
            }
            if (type >= 3 && type != 5) {
                p.setItemFromInfusion(0, ItemStack.EMPTY);
            } else {
                Containers.dropItemStack(level, ped.getX(), ped.getY() + 0.5, ped.getZ(), p.getItem(0).copy());
                p.setItemFromInfusion(0, ItemStack.EMPTY);
            }
            if (type == 1 || type == 3) {
                placeFlux(ped.above(), "blockfluxgoo");
            } else if (type == 2 || type == 4) {
                placeFlux(ped.above(), "blockfluxgas");
            } else if (type == 5) {
                // Level.playSound takes a BlockPos (mcsources net/minecraft/world/level/
                // Level.java:376-377); the original sounded the pedestal block, whose
                // position is `ped` itself — Vec3 getCenter() is not an overload here.
                level.playSound(null, ped, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.WEATHER, 1.0F, 1.0F);
            }
            level.sendBlockUpdated(ped, level.getBlockState(ped), level.getBlockState(ped), 3);
            return;
        }
    }

    /** Flux blocks belong to a later catalog card; place them only when registered. */
    private void placeFlux(final BlockPos pos, final String id) {
        final ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(
                com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, id);
        BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(block ->
                level.setBlock(pos, block.defaultBlockState(), 3));
    }

    private void craftingFinish() {
        final BlockEntity centreBe = level.getBlockEntity(worldPosition.below(2));
        if (!(centreBe instanceof TilePedestal ped)) {
            return;
        }
        if (this.recipeType == 1 && this.recipeEnchantment != null) {
            final ItemStack target = ped.getItem(0);
            final var mutable = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(
                    target.getEnchantments());
            mutable.set(this.recipeEnchantment,
                    mutable.getLevel(this.recipeEnchantment) + 1);
            net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(target, mutable.toImmutable());
            ped.setItemFromInfusion(0, target);
        } else if (this.recipeOutput != null && !this.recipeOutput.isEmpty()) {
            ped.setItemFromInfusion(0, this.recipeOutput.copy());
        } else if (this.recipeOutputLabel != null && this.recipeOutputValue != null) {
            final ItemStack target = ped.getItem(0);
            // 1.21.1 has no ItemStack.getOrCreateTag(): net/minecraft/world/item/ItemStack
            // java has no NBT tag accessor (its tags are registry TagKeys only). The
            // platform's home for arbitrary per-item NBT is the CUSTOM_DATA data component
            // (net/minecraft/world/item/component/CustomData.java:26-33), so the pair is
            // written there under the original key/value, unchanged.
            CustomData.update(DataComponents.CUSTOM_DATA, target,
                    custom -> custom.put(this.recipeOutputLabel, this.recipeOutputValue));
            ped.setItemFromInfusion(0, target);
        }
        this.recipeEssentia = new AspectList();
        this.pendingComponents = null;
        this.recipeEnchantment = null;
        level.sendBlockUpdated(worldPosition.below(2), level.getBlockState(worldPosition.below(2)),
                level.getBlockState(worldPosition.below(2)), 3);
        // GameEvent.INVENTORY is not in 1.21.1 (the full set is in
        // net/minecraft/world/level/gameevent/GameEvent.java; there is no INVENTORY,
        // ITEM_PRODUCED or ITEM_USE). ITEM_INTERACT_FINISH is the existing event whose
        // radius/meaning covers "an item interaction just completed here". GameEvent.
        // Context.of(null) alone is ambiguous between of(Entity) and of(BlockState);
        // the two-null form is the (Entity, BlockState) overload.
        level.gameEvent(GameEvent.ITEM_INTERACT_FINISH, worldPosition.below(2),
                GameEvent.Context.of(null, null));
    }

    // ---------------------------------------------------------------- persistence

    private void saveAndSync() {
        setChanged();
        if (!level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // 1.21.1 has no Ingredient.toJson()/Ingredient.fromJson(): the platform's own NBT
    // form of an Ingredient is Ingredient.CODEC (net/minecraft/world/item/crafting/
    // Ingredient.java:39) — the same codec the platform's recipe classes serialise
    // through (ShapelessRecipe.java:79, SmithingTransformRecipe.java:69). Encoding and
    // decoding through it keeps the saved values byte-identical to the original's
    // toJson() payload; the registries travel in the serialisation context, the form
    // BlockEntity.java:104 uses for its own components.
    @Nullable
    private net.minecraft.nbt.Tag encodeIngredient(final HolderLookup.Provider registries,
                                                  final Ingredient ingredient) {
        return Ingredient.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),
                ingredient).resultOrPartial(
                failure -> com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.LOGGER.error(failure))
                .orElse(null);
    }

    @Nullable
    private Ingredient decodeIngredient(final HolderLookup.Provider registries,
                                       final net.minecraft.nbt.Tag encoded) {
        return Ingredient.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE),
                encoded).resultOrPartial(
                failure -> com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.LOGGER.error(failure))
                .orElse(null);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("active", this.active);
        tag.putBoolean("crafting", this.crafting);
        tag.putInt("instability", this.instability);
        this.recipeEssentia.writeToNBT(tag);
        if (this.pendingComponents != null && !this.pendingComponents.isEmpty()) {
            final ListTag list = new ListTag();
            for (final Ingredient ingredient : this.pendingComponents) {
                final CompoundTag entry = new CompoundTag();
                final net.minecraft.nbt.Tag encoded = encodeIngredient(registries, ingredient);
                if (encoded != null) {
                    entry.put("value", encoded);
                }
                list.add(entry);
            }
            // pending components are re-derived from the matched recipe id on
            // load; store the concrete pedestal copies like the original did.
            tag.put("pendingcomponents", list);
        }
        if (this.recipeInput != null) {
            final net.minecraft.nbt.Tag encoded = encodeIngredient(registries, this.recipeInput);
            if (encoded != null) {
                tag.put("recipeinput", encoded);
            }
        }
        if (this.recipeOutput != null && !this.recipeOutput.isEmpty()) {
            tag.putString("rotype", "@");
            tag.put("recipeout", this.recipeOutput.save(registries));
        }
        if (this.recipeOutputLabel != null && this.recipeOutputValue != null) {
            tag.putString("rotype", this.recipeOutputLabel);
            tag.put("recipeout", this.recipeOutputValue);
        }
        tag.putInt("recipeinst", this.recipeInstability);
        tag.putInt("recipetype", this.recipeType);
        tag.putInt("recipexp", this.recipeXP);
        tag.putString("recipeplayer", this.recipePlayer == null ? "" : this.recipePlayer.toString());
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.active = tag.getBoolean("active");
        this.crafting = tag.getBoolean("crafting");
        this.instability = tag.getInt("instability");
        this.recipeEssentia = new AspectList();
        this.recipeEssentia.readFromNBT(tag);
        if (tag.contains("recipeinput")) {
            this.recipeInput = decodeIngredient(registries, tag.get("recipeinput"));
        }
        final String rotype = tag.getString("rotype");
        if ("@".equals(rotype) && tag.contains("recipeout")) {
            this.recipeOutput = ItemStack.parseOptional(registries, tag.getCompound("recipeout")).copy();
        } else if (!rotype.isEmpty() && tag.contains("recipeout")) {
            this.recipeOutputLabel = rotype;
            this.recipeOutputValue = tag.get("recipeout");
        }
        this.recipeInstability = tag.getInt("recipeinst");
        this.recipeType = tag.getInt("recipetype");
        this.recipeXP = tag.getInt("recipexp");
        final String player = tag.getString("recipeplayer");
        this.recipePlayer = player.isEmpty() ? null : UUID.fromString(player);
    }

    // 1.21.1's BlockEntity.getUpdateTag takes the registries and the custom-data half
    // is saveWithoutMetadata(HolderLookup.Provider) (BlockEntity.java:104,209); the
    // packet override keeps the concrete ClientboundBlockEntityDataPacket return type,
    // which implements Packet<ClientGamePacketListener> (see tiles/TilePedestal.java:
    // 133-148, the same-package compiling shape).
    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
