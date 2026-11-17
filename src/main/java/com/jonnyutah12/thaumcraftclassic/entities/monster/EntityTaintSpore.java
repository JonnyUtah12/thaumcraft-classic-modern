package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaintFibres;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTaint;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import java.util.ArrayList;
import java.util.List;

/**
 * The taint spore (TC-M6-06-s-f, content-mapping.csv {@code thaumcraft:taintspore}).
 *
 * <p>Port of {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/EntityTaintSpore.java}
 * (237 lines). Every number and branch is quoted from that file; the arithmetic
 * lives in {@link TaintSporeAiLogic} so the JUnit suite can pin it without a
 * world. The swarmer is the original's top-level subclass and lives in
 * {@link EntityTaintSporeSwarmer}.</p>
 *
 * <p>1.7.10 → 1.21.1 hook map used here:</p>
 * <ul>
 *   <li>{@code func_70088_a} (entityInit, the data-watcher 16 byte) →
 *       {@link #defineSynchedData(SynchedEntityData.Builder)};
 *       {@code func_70014_b}/{@code func_70037_a} (writeToNBT / readFromNBT) →
 *       {@link #addAdditionalSaveData(CompoundTag)} /
 *       {@link #readAdditionalSaveData(CompoundTag)};</li>
 *   <li>{@code IEntityAdditionalSpawnData} (the 1.7.10 {@code ByteBuf} pair) →
 *       NeoForge 1.21.1's {@link IEntityWithComplexSpawn}
 *       ({@code RegistryFriendlyByteBuf}); the port had no prior implementation of
 *       that seam, so this class is the first one (see the card report);</li>
 *   <li>{@code func_70105_a(w,h)} + {@code func_70107_b(x,y,z)} + {@code func_70053_R}
 *       (setSize / setPosition / getEyeHeight) → the {@link #sporeBox} field,
 *       {@link #getDefaultDimensions(Pose)} and {@link #refreshDimensions()} —
 *       1.21.1 carries the eye height inside {@link EntityDimensions} and
 *       {@code getEyeHeight(Pose)} is final;</li>
 *   <li>{@code func_70091_d(dx,dy,dz)} (moveEntity) → {@link #travel(Vec3)}: x/z are
 *       thrown away, upward motion is clamped to 0, and on a mature spore stalk the
 *       original skips the integration entirely ({@code :79-92});</li>
 *   <li>{@code func_70626_be} (updateEntityActionState) is EMPTY in the original
 *       ({@code :94-95}) — no target search, no jump, no steering — and the goals
 *       are empty by {@link GoallessTaintMonster}, so nothing overrides the AI step;</li>
 *   <li>{@code func_70071_h_} (onUpdate) → {@link #tick()}, with the
 *       {@code world.field_72995_K} halves split into
 *       {@link #serverBiomeStarve()} and {@link #clientSporeVisuals()};</li>
 *   <li>{@code func_70100_b_(player)} (onPlayerInteract) →
 *       {@link #mobInteract(Player, InteractionHand)};</li>
 *   <li>{@code func_70106_y} (setDead) → {@link #discard()};
 *       {@code world.func_72921_c(x,y,z,3,3)} (setBlockMetadataWithMeta, flags 3 =
 *       {@code UPDATE_NEIGHBORS|UPDATE_CLIENTS}) → {@link TaintManager#setFibres};</li>
 *   <li>{@code func_146068_u}/{@code func_70628_a} (getDropItemId / dropRareDrop) →
 *       {@link #dropFromLootTable(DamageSource, boolean)}: the port has no
 *       {@code loot_table/entities} rows, so the original's code-driven drop ships
 *       as code.</li>
 * </ul>
 *
 * <p>Client-only, no seam: {@code func_70112_a} (isInRangeToRenderDist, {@code :97-99}),
 * {@code func_70070_b} (getBrightness, {@code :101-104}) and
 * {@code Thaumcraft.proxy.swarmParticleFX / splooshFX} ({@code :141}, {@code :191}).
 * Their rules ship pinned in {@link TaintSporeAiLogic}; the effects themselves are
 * left to the client-FX card (this card's left_out_of_scope).</p>
 */
public class EntityTaintSpore extends GoallessTaintMonster implements IEntityWithComplexSpawn {

    /**
     * The 1.7.10 spawn id of the taint spider the burst releases
     * ({@code EntityTaintSpore.java:165}). Resolved from the live registry at call
     * time, the house seam used by {@link TaintManager#spawnGuarded}.
     */
    public static final ResourceLocation TAINT_SPIDER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "taintspider");

    /** NBT key of the size, verbatim from {@code EntityTaintSpore.java:41}/{@code :46}. */
    public static final String TAG_SIZE = "Size";

    /**
     * {@code EntityTaintSpore.java:36} — data watcher 16, a byte, initialised to 1.
     * 1.21.1 assigns the id itself, so only the serializer and the default carry over.
     */
    private static final EntityDataAccessor<Byte> DATA_SPORE_SIZE =
            SynchedEntityData.defineId(EntityTaintSpore.class, EntityDataSerializers.BYTE);

    /**
     * 1.7.10 {@code Thaumcraft.proxy.swarmParticleFX} handles
     * ({@code EntityTaintSpore.java:25}, {@code :133-142}) — the client-side swarm
     * budget. The port has no proxy FX seam, so nothing lands in the list; the
     * cull/add cadence and the budget rules it feeds are kept verbatim
     * ({@link TaintSporeAiLogic#sporeWantsSwarmFx}, {@link #cullSwarmFx()}).
     */
    protected final List<Object> swarmFx = new ArrayList<>();

    /** {@code EntityTaintSpore.java:26} — {@code protected int growth}. */
    protected int growth;

    /** {@code EntityTaintSpore.java:27} — {@code public float displaySize} (client ramp). */
    public float displaySize;

    /**
     * The box written by {@code func_70105_a} at {@code :52} — the size ladder's
     * 0.15-per-step, 0.5-floor box; the swarmer pins it at 1.0
     * ({@code EntityTaintSporeSwarmer.java:27}).
     */
    protected float sporeBox = TaintSporeAiLogic.SPORE_BOX_MIN;

    public EntityTaintSpore(final EntityType<? extends EntityTaintSpore> type, final Level level) {
        super(type, level);
        // :31 — setSporeSize(2). Dynamic dispatch: the swarmer's ctor lands in ITS
        // override, exactly as in the original.
        this.setSporeSize(TaintSporeAiLogic.SPORE_INITIAL_SIZE);
    }

    /** :62-65 — super first (the vanilla mob set), then health 1.0 and attack 1.0. */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, TaintSporeAiLogic.HEALTH)
                .add(Attributes.ATTACK_DAMAGE, TaintSporeAiLogic.ATTACK);
        // movement_speed and follow_range keep 1.21.1's createMobAttributes
        // defaults: the original registers nothing else, and the spore is rooted
        // ({@code travel} clamp, :79-92).
    }

    /** :34-37 — entityInit: watcher 16 (byte) initialised to 1. */
    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPORE_SIZE, (byte) TaintSporeAiLogic.SPORE_SIZE_DATA_INIT);
    }

    /** :39-42 — {@code tag.putInt("Size", getSporeSize() - 1)}. */
    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_SIZE, this.getSporeSize() - 1);
    }

    /**
     * :44-47 — {@code setSporeSize(tag.getInt("Size") + 1)}. Kept verbatim, quirk
     * included: an entity loaded WITHOUT the tag reads 0 and lands on size 1, not on
     * its constructor size.
     */
    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSporeSize(tag.getInt(TAG_SIZE) + 1);
    }

    /**
     * :49-55 — the size setter: watcher write, box
     * {@code max(0.15 * size, 0.5)} on BOTH axes, a self-position write
     * ({@code func_70107_b}) and {@code field_70728_aV = size} (the xp IS the size).
     */
    public void setSporeSize(final int sporeSize) {
        this.setSporeSizeData(sporeSize);                                  // :50
        this.sporeBox = TaintSporeAiLogic.sporeBoxSize(sporeSize);         // :51
        this.refreshDimensions();                                          // :52
        this.setPos(this.getX(), this.getY(), this.getZ());                // :53
        this.xpReward = TaintSporeAiLogic.xpForSize(sporeSize);            // :54
    }

    /** The watcher write of :50, split out so the swarmer can reuse it verbatim. */
    protected final void setSporeSizeData(final int sporeSize) {
        this.entityData.set(DATA_SPORE_SIZE, (byte) sporeSize);
    }

    /** :57-59 — {@code getSporeSize()} reads the (signed byte) watcher. */
    public int getSporeSize() {
        return this.entityData.get(DATA_SPORE_SIZE);
    }

    /** :30-32 + :51-52 — the box; :67-69 — the 0.0 eye height, carried on the dimensions. */
    @Override
    protected EntityDimensions getDefaultDimensions(final Pose pose) {
        return EntityDimensions.fixed(this.sporeBox, this.sporeBox)
                .withEyeHeight(TaintSporeAiLogic.EYE_HEIGHT);
    }

    /** :71-73 — {@code canBePushed() = true} (the 1.21.1 seam is {@code isPushable}). */
    @Override
    public boolean isPushable() {
        return true;
    }

    /**
     * :75-77 — {@code isChild() = false}. 1.21.1 has no child gate on a {@code Mob},
     * and nothing in the vanilla pipeline consults one for this mob, so the override
     * has no seam (see this card's seams_missing).
     */
    public boolean isChildSpore() {
        return false;
    }

    /** :210-212 — ambient sound interval 200. */
    @Override
    public int getAmbientSoundInterval() {
        return TaintSporeAiLogic.AMBIENT_SOUND_INTERVAL;
    }

    /** :214-216 — ambient {@code thaumcraft:swarm}. */
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SWARM.get();
    }

    /** :218-220 — hurt {@code thaumcraft:gore}. */
    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return ModSounds.GORE.get();
    }

    /** :222-224 — death {@code thaumcraft:gore}. */
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GORE.get();
    }

    /** :206-208 — volume 0.1. */
    @Override
    protected float getSoundVolume() {
        return TaintSporeAiLogic.SOUND_VOLUME;
    }

    /**
     * :79-92 — the movement integration hook. x and z are thrown away, upward
     * motion is clamped to 0, and while the spore stands on a MATURE spore stalk
     * (fibres meta 4) the original never calls super at all, so the spore is rooted
     * in place: no locomotion, no fall, no collision pass.
     */
    @Override
    public void travel(final Vec3 travelVector) {
        if (this.isRootedOnMatureStalk()) {
            return;
        }
        super.travel(new Vec3(0.0, TaintSporeAiLogic.travelY(travelVector.y()), 0.0));
    }

    /** :86-89 — the stalk cell: {@code floor(x), floor(aabb.minY) - 1, floor(z)}. */
    protected BlockPos stalkCellBelow() {
        return new BlockPos(Mth.floor(this.getX()),
                Mth.floor(this.getBoundingBox().minY) - 1,
                Mth.floor(this.getZ()));
    }

    /** :89 — the stalk test: taint fibres carrying meta 4. */
    protected boolean isMatureStalkAt(final BlockPos pos) {
        final BlockState under = this.level().getBlockState(pos);
        return TaintSporeAiLogic.isMatureSporeStalk(under.is(CatalogTaint.BLOCK_TAINT_FIBRES.get()),
                under.hasProperty(BlockTaintFibres.VARIANT)
                        ? under.getValue(BlockTaintFibres.VARIANT)
                        : BlockTaintFibres.META_FIBRES);
    }

    /** The stalk test of {@link #stalkCellBelow()} (the original repeats it at :89, :148, :179). */
    protected boolean isRootedOnMatureStalk() {
        return this.isMatureStalkAt(this.stalkCellBelow());
    }

    /** :110-120 — onUpdate: the biome starve tick, then {@code sporeOnUpdate()}. */
    @Override
    public void tick() {
        super.tick();
        this.serverBiomeStarve();
        this.sporeOnUpdate();
    }

    /**
     * :112-117 — server half: one starve heart ({@code DamageSource.field_76366_f})
     * every 20 ticks while the biome under the spore is not the taint biome. A spore
     * has 1 health, so leaving the taint biome kills it, and the kill feeds the
     * {@code hurtDuration > 0} branch of {@link #sporeOnUpdate()}.
     */
    protected void serverBiomeStarve() {
        if (this.level().isClientSide) {
            return;
        }
        if (TaintSporeAiLogic.starves(this.tickCount,
                TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(this.level(), this.blockPosition())))) {
            this.hurt(this.level().damageSources().starve(), TaintSporeAiLogic.STARVE_DAMAGE);
        }
    }

    /**
     * :122-153 — the hand-rolled update. The growth ladder carries NO side gate in
     * the original, so it runs on both sides; the display ramp and the swarm-FX
     * budget are the client half; the stalk check and the hurt flash burst run on
     * both sides.
     */
    protected void sporeOnUpdate() {
        final int size = this.getSporeSize();
        // :123-126 — `growth++ == 1200` is compared PRE-increment and only runs
        // while size < 10 (short-circuit).
        if (TaintSporeAiLogic.grows(size, this.growth)) {
            this.setSporeSize(size + 1);
            this.growth = 0;
        } else {
            this.growth = TaintSporeAiLogic.nextGrowth(size, this.growth);
        }

        if (this.level().isClientSide) {
            this.clientSporeVisuals();
        }

        // :145-152
        if (!this.isRootedOnMatureStalk()) {
            this.spiderBurst();
        } else if (this.hurtDuration > 0) {
            // :150 — `field_70725_aQ > 0` is the hurt DURATION counter (1.7.10
            // maxHurtTime), whose 1.21.1 field is LivingEntity#hurtDuration — not
            // the invulnerability timer the sibling taintacle card mapped.
            this.spiderBurst();
        }
    }

    /** :128-143 — the client half, split out so a game-test server proves both sides. */
    protected void clientSporeVisuals() {
        if (this.displaySize < this.getSporeSize()) {
            this.displaySize = TaintSporeAiLogic.nextDisplaySize(this.displaySize, this.getSporeSize());
        }
        this.cullSwarmFx();
        if (TaintSporeAiLogic.sporeWantsSwarmFx(this.swarmFx.size(), this.getSporeSize())) {
            this.addSwarmFx();
        }
    }

    /** :133-138 — remove the FIRST dead handle and break: one cull per tick. */
    protected void cullSwarmFx() {
        for (int a = 0; a < this.swarmFx.size(); a++) {
            if (this.swarmFx.get(a) == null) {
                this.swarmFx.remove(a);
                break;
            }
        }
    }

    /**
     * :141 — {@code swarm.add(Thaumcraft.proxy.swarmParticleFX(world, this, 0.1F,
     * 10.0F, 0.0F))}. The port has no proxy FX seam, so the handle list stays empty;
     * the budget rule that gates this call ships pinned
     * ({@link TaintSporeAiLogic#sporeWantsSwarmFx}) and the effect is the client-FX
     * card's (see this card's left_out_of_scope).
     */
    protected void addSwarmFx() {
        // no FX seam
    }

    /** :155-157 — onPlayerInteract: a right click pops the spore. */
    @Override
    protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        this.spiderBurst();
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /**
     * :159-187 — the burst. Server: the gore cue, {@code size / 3 + rand(size / 2 + 1)}
     * spiderlings jittered around the spore, the mature stalk drops back to meta 3
     * (the spore is spent), then setDead. Client: {@code sploosh(50)}.
     */
    protected void spiderBurst() {
        if (this.level().isClientSide) {
            this.sploosh(TaintSporeAiLogic.BURST_SPLLOOSH);   // :185
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // :161 — world.playSoundAtEntity(this, "thaumcraft:gore", 1.0F,
        // 0.9F + world.rand.nextFloat() * 0.1F): the WORLD random drives the pitch.
        serverLevel.playSound(null, this, ModSounds.GORE.get(), SoundSource.NEUTRAL,
                TaintSporeAiLogic.GORE_VOLUME,
                TaintSporeAiLogic.gorePitch(serverLevel.getRandom().nextFloat()));

        final int size = this.getSporeSize();
        final int q = TaintSporeAiLogic.burstCount(size,
                serverLevel.getRandom().nextInt(TaintSporeAiLogic.burstRollBound(size)));   // :162
        for (int a = 0; a < q; a++) {
            this.spawnSpiderling(serverLevel);                                              // :164-174
        }

        final BlockPos stalk = this.stalkCellBelow();                                       // :176-178
        if (this.isMatureStalkAt(stalk)) {
            // :180 — world.setBlockMetadataWithMeta(x, y, z, 3, 3): flags 3 are
            // UPDATE_NEIGHBORS|UPDATE_CLIENTS, the same pair TaintManager.setFibres uses.
            TaintManager.setFibres(serverLevel, stalk, BlockTaintFibres.META_SPORE_STALK);
        }

        this.discard();                                                                     // :183
    }

    /**
     * :165-173 — one spiderling, jittered ±1 block in x/z (two independent
     * {@code nextFloat()} draws, kept verbatim), lifted by one {@code nextFloat()}
     * in y, with a random yaw and 0 pitch. Resolved through the live registry, the
     * {@link TaintManager#spawnGuarded} shape: inert while the id is unregistered.
     */
    protected void spawnSpiderling(final ServerLevel level) {
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(TAINT_SPIDER_ID);
        if (type == null) {
            return;
        }
        final Entity spiderling = type.create(level);
        if (spiderling == null) {
            return;
        }
        final RandomSource random = level.random;
        spiderling.setPos(this.getX() + random.nextFloat() - random.nextFloat(),
                this.getY() + random.nextFloat(),
                this.getZ() + random.nextFloat() - random.nextFloat());
        spiderling.setYRot(random.nextFloat() * 360.0F);
        spiderling.setXRot(0.0F);
        level.addFreshEntity(spiderling);
    }

    /**
     * :189-193 — {@code for (a = 0; a < amt; a++) Thaumcraft.proxy.splooshFX(this)}.
     * No proxy FX seam exists in the port, so the loop is the pinned rule and the
     * body is the client-FX card's; every call site and amount is kept
     * ({@link TaintSporeAiLogic#BURST_SPLLOOSH}, {@link TaintSporeAiLogic#SWARMER_HURT_SPLLOOSH},
     * {@link TaintSporeAiLogic#SWARMER_EVENT_SPLLOOSH}).
     */
    protected void sploosh(final int amt) {
        for (int a = 0; a < amt; a++) {
            // Thaumcraft.proxy.splooshFX(this) — no FX seam
        }
    }

    /**
     * :195-197 — the spawn packet sends the spore size as a FLOAT.
     * {@code IEntityAdditionalSpawnData}'s {@code ByteBuf} pair maps to NeoForge's
     * {@link IEntityWithComplexSpawn} {@code RegistryFriendlyByteBuf} pair.
     */
    @Override
    public void writeSpawnData(final RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(this.getSporeSize());
    }

    /**
     * :199-204 — the client drops the spawn float into {@code displaySize}, NOT into
     * the size: the ramp starts at the true size, and the watcher carries the size
     * itself. Kept verbatim, including the swallow-everything try/catch.
     */
    @Override
    public void readSpawnData(final RegistryFriendlyByteBuf additionalData) {
        try {
            this.displaySize = additionalData.readFloat();
        } catch (Exception ignored) {
            // :202-203 — the original catches Exception and does nothing.
        }
    }

    /**
     * :226-236 — the original's getDropItemId (itemResource) plus dropRareDrop: a
     * coin flip between tainted goo (meta 11) and tainted fungus (meta 12), thrown
     * {@code field_70131_O / 2} = the entity HEIGHT / 2 blocks up.
     */
    @Override
    protected void dropFromLootTable(final DamageSource source, final boolean attackedRecently) {
        final ItemStack drop = ItemResource.stack(TaintSporeAiLogic.rollsGoo(this.random)
                ? TaintSporeAiLogic.META_TAINTED_GOO : TaintSporeAiLogic.META_TAINTED_FUNGUS);
        this.spawnAtLocation(drop, TaintSporeAiLogic.dropOffset(this.getBbHeight()));   // :232/:234
        super.dropFromLootTable(source, attackedRecently);                              // :235
    }

    /** The size ladder's growth counter (1.7.10 {@code growth}, {@code :26}), for probes. */
    public int getGrowth() {
        return this.growth;
    }

    /** For probes: force the growth counter. */
    public void setGrowth(final int ticks) {
        this.growth = ticks;
    }

    /** The client display ramp (1.7.10 {@code displaySize}, {@code :27}). */
    public float getDisplaySize() {
        return this.displaySize;
    }

    /** The client swarm-FX handles (1.7.10 {@code swarm}, {@code :25}). */
    public List<Object> getSwarmFx() {
        return this.swarmFx;
    }
}
