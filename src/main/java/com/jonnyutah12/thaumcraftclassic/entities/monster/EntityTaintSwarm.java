package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * TC-M6-06-s-e: the taint swarm (content-mapping.csv row {@code Thaumcraft:TaintSwarm}
 * → {@code thaumcraft:taintswarm}, original
 * {@code thaumcraft.common.entities.monster.EntityTaintSwarm},
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/EntityTaintSwarm.java},
 * cited below as {@code :NNN}).
 *
 * <p>The original is an {@code EntityMob implements ITaintedMob} ({@code :26}) that
 * installs <b>no goal selectors</b>: the flight, the target search, the hover height
 * and the yaw are written by hand in {@code func_70071_h_} (onUpdate, {@code :101-116})
 * and {@code func_70626_be} (updateEntityActionState, {@code :118-174}). The port
 * therefore extends {@link GoallessTaintMonster} (whose {@code registerGoals()} is
 * empty on purpose) and reproduces those two bodies in {@link #tick()} and
 * {@link #customServerAiStep()}. Every number, literal and branch is a
 * {@link TaintSwarmAiLogic} constant quoting a decompiled line, so this class only
 * walks state.</p>
 *
 * <h2>Hook map</h2>
 * <ul>
 *   <li>{@code func_70071_h_} → {@link #tick()}, with the {@code world.field_72995_K}
 *       half ({@code :104-115}) split into {@link #clientSwarmParticles()} and gated on
 *       {@code level().isClientSide} so a game-test server proves the other half;</li>
 *   <li>{@code func_70626_be} → {@link #customServerAiStep()} — the same seam the
 *       thaumic slime and the taintacle use;</li>
 *   <li>{@code func_70628_a} (dropFewItems, {@code :257-261}) →
 *       {@link #dropCustomDeathLoot(ServerLevel, DamageSource, boolean)}, 1.21.1's
 *       per-mob death-drop hook;</li>
 *   <li>{@code func_70601_bi} (getCanSpawnHere, {@code :240-247}) →
 *       {@link #checkSpawnRules(LevelAccessor, MobSpawnType)};</li>
 *   <li>{@code func_70782_k} (the target provider, {@code :223-226}) → the
 *       {@code setTarget(...)} write at the top of {@link #customServerAiStep()}:
 *       1.7.10's {@code EntityCreature.updateEntityActionState} re-reads that
 *       provider every tick, and that call is the {@code super.func_70626_be()} at
 *       {@code :119}.</li>
 * </ul>
 *
 * <h2>1.7.10 hooks with no 1.21.1 seam (documented, not invented)</h2>
 * <ul>
 *   <li>{@code :41-44} {@code getBrightness(float) = 15728880} — client-only
 *       lightmap override. 1.21.1 has no entity-side lightmap hook; the value is
 *       pinned as {@link TaintSwarmAiLogic#FULL_BRIGHTNESS_LIGHTMAP} for the render
 *       layer.</li>
 *   <li>{@code :46-48} {@code func_70692_ba()} → true. The repo resolves this name
 *       as the Looting gate ({@link boss.EntityTaintacleGiant}, which documents the
 *       same hook at its {@code :96-98} and forbids wiring it to 1.21.1's
 *       {@code LivingEntity#shouldDropLoot()}, since that gates the whole death-loot
 *       block). The swarm returns the vanilla default {@code true}, so there is
 *       nothing to carry and nothing is wired.</li>
 *   <li>{@code :70-72} {@code func_70104_M()} → false, and {@code :180-182}
 *       {@code func_70041_e_()} → false: the repo resolves the first to the push
 *       hook (the taintacle maps its {@code func_70104_M() = true} to
 *       {@code isPushable() → true}) and the second to {@code isPushable() → false}
 *       ({@link EntityWisp}). 1.7.10 reads {@code func_70041_e_} as the ladder flag,
 *       which scales the 0.08/tick gravity down to 0.02 — the swarm returns
 *       {@code false}, i.e. full vanilla gravity, so no 1.21.1 flag is set for it
 *       either. Both readings of both names agree on {@code false}, and the port
 *       carries one override: {@link #isPushable()} → false.</li>
 *   <li>{@code :93-95} {@code func_70650_aV()} → false (1.7.10's boss flag):
 *       1.21.1 expresses "not a boss" by the entity type, and boss bars are
 *       explicit, so there is nothing to carry.</li>
 *   <li>{@code :184-185} {@code func_70069_a(float)} (jump) and
 *       {@code func_70064_a(double, boolean)} (fall) are empty in the original. The
 *       jump hook is inert here because no goal and no {@code JumpControl} input
 *       exists; the fall hook maps to
 *       {@link LivingEntity#checkFallDamage(double, boolean, net.minecraft.world.level.block.state.BlockState, BlockPos)},
 *       overridden to a no-op so the swarm takes no fall damage.</li>
 *   <li>{@code :190-192} {@code func_145773_az()} ({@code isNoAi()}) → true: 1.21.1's
 *       NoAi flag also flips {@code Mob#isEffectiveAi()}, which gates
 *       {@code LivingEntity#travel}, so setting it would freeze a mob whose motion
 *       is hand-written at {@code :103} and {@code :151-168}. The flag is therefore
 *       NOT carried; the port stays AI-inert the way the original is — empty goal
 *       selectors, and {@link #attackEntity(Entity, float)} is wired to no caller
 *       (see below).</li>
 *   <li>{@code :249-251} {@code func_70814_o()} → true is 1.7.10's
 *       {@code isAIEnabled()} flag, the vanilla default for a living entity, so the
 *       port sets no flag for it. Notably the swarm does NOT float: it writes no
 *       flying flag, sets no gravity flag, and its hover is an equilibrium between
 *       vanilla's 0.08/tick gravity, the swarm's own {@code motionY *= 0.6F}
 *       ({@code :103}) and the {@code signum(delta) * 0.7} vertical steer
 *       ({@code :152}, {@code :163}) around a waypoint that sits 2 blocks under it
 *       ({@code :143}). The port reproduces exactly that: gravity runs, {@link
 *       #tick()} applies the 0.6 drag after {@code super}, and
 *       {@link #customServerAiStep()} writes the steer. 1.21.1's
 *       {@code LivingEntity#onClimbable} override is NOT used — it routes travel
 *       through the climbing path, which multiplies the motion by 0.2 every tick,
 *       and the original never did that.</li>
 *   <li>{@code :253-255} {@code getDropItem()} returns itemResource. 1.7.10's real
 *       drop is the {@code dropFewItems} override, so the getter is informational;
 *       1.21.1 has no per-entity drop-item getter (loot tables), and the drop ships
 *       through {@link #dropCustomDeathLoot(ServerLevel, DamageSource, boolean)}.</li>
 *   <li>Substrate deviation: 1.7.10's {@code EntityLiving} drags motion by 0.6 on
 *       every axis (the swarm's own copy of that factor survives at {@code :103});
 *       1.21.1's {@code LivingEntity} drags 0.6 only while standing and 0.98 in the
 *       air, and exposes no per-mob friction override. The swarm therefore drifts
 *       further per tick than the original does at the same steering lerp.</li>
 * </ul>
 */
public class EntityTaintSwarm extends GoallessTaintMonster {

    /** {@code :36-39} — the 1.7.10 dataWatcher index 16, a Byte initialised to 0. */
    private static final EntityDataAccessor<Byte> DATA_FLAGS =
            SynchedEntityData.defineId(EntityTaintSwarm.class, EntityDataSerializers.BYTE);

    /** {@code :27} — {@code ChunkCoordinates currentFlightTarget}. */
    private BlockPos currentFlightTarget;
    /** {@code :28} — {@code public int damBonus = 0}. */
    private int damBonus = TaintSwarmAiLogic.DAM_BONUS_START;
    /**
     * {@code :29} — {@code public ArrayList swarm}, the client's tracked FX
     * entities. 1.21.1 particles are not entities, so the list's observable role
     * (its size against the FX budget, {@code :112}) is kept as a count; see the
     * class javadoc for the FX seam.
     */
    private int clientFxCount;
    /** {@code :199}/{@code :207} — {@code field_70724_aR}, the swing cooldown. */
    private int attackTime;

    public EntityTaintSwarm(final EntityType<? extends GoallessTaintMonster> type, final Level level) {
        super(type, level);
        // :249-251 isAIEnabled() = true is the 1.7.10 vanilla default, so no flag is
        // set. The original sets NO flying flag and NO gravity flag: it hovers on an
        // equilibrium between vanilla's 0.08/tick gravity, its own motionY *= 0.6F
        // (:103) and the signum(delta) * 0.7 vertical steer (:152, :163) toward a
        // waypoint 2 blocks below it (:143). The port reproduces that as-is.
    }

    /**
     * {@code :74-78} — the original registers only max health 30.0 and attack
     * damage {@code 2 + damBonus}. 1.7.10 evaluates that expression when
     * {@code func_110147_ax} runs, which is the {@code EntityLivingBase}
     * constructor, i.e. always with {@code damBonus == 0}: a swarm loaded from NBT
     * with a bonus keeps swinging at 2.0 (preserved quirk — see
     * {@link #readAdditionalSaveData(CompoundTag)}).
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, TaintSwarmAiLogic.HEALTH)
                .add(Attributes.ATTACK_DAMAGE, TaintSwarmAiLogic.attackDamage(TaintSwarmAiLogic.DAM_BONUS_START));
        // movement_speed keeps 1.21.1's createMobAttributes default: the original
        // never registers it, and every step of the swarm's motion is hand-written.
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // :38 — data 16 is created as a Byte holding 0.
        builder.define(DATA_FLAGS, (byte) 0);
    }

    /**
     * {@code :33} box 2.0x2.0 plus {@code :50-52} eye height 1.0. 1.7.10 sizes the
     * box from the ctor's {@code setSize}; 1.21.1 derives it from the type, so the
     * family overrides {@code getDefaultDimensions} — the same seam the taintacle
     * and the thaumic slime use — and {@code getEyeHeight(Pose)} is final.
     */
    @Override
    protected EntityDimensions getDefaultDimensions(final Pose pose) {
        return EntityDimensions.fixed(TaintSwarmAiLogic.WIDTH, TaintSwarmAiLogic.HEIGHT)
                .withEyeHeight(TaintSwarmAiLogic.EYE_HEIGHT);
    }

    /**
     * {@code :70-72} — {@code func_70104_M() = false}. The repo resolves that name to
     * the push hook (the taintacle maps its {@code func_70104_M() = true} to
     * {@code isPushable() → true}), and {@code :180-182} {@code func_70041_e_() =
     * false} is the name {@link EntityWisp} resolves to {@code isPushable() → false}.
     * Both readings point the same way here, so the swarm is not pushable: the
     * 2.0x2.0 cloud drifts through the player.
     */
    @Override
    public boolean isPushable() {
        return false;
    }

    /** {@code :194-196} — {@code if (isInvulnerable()) return false;} before super. */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (TaintSwarmAiLogic.hurtBlocked(this.isInvulnerable())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    /** {@code :54-56} — {@code getSoundVolume() = 0.1F}. */
    @Override
    protected float getSoundVolume() {
        return TaintSwarmAiLogic.SOUND_VOLUME;
    }

    /**
     * {@code :58-60} — the ambient sample is the empty string: the swarm never
     * loops a sound. 1.21.1 spells "no ambient" as a null {@code getAmbientSound}
     * ({@code Mob#playAmbientSound} skips a null sample).
     */
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    /** {@code :62-64} — hurt {@code "thaumcraft:swarmattack"}. */
    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return ModSounds.SWARMATTACK.get();
    }

    /** {@code :66-68} — death {@code "thaumcraft:swarmattack"}. */
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SWARMATTACK.get();
    }

    /** {@code :184-185} — {@code fall(double, boolean)} is empty: no fall damage. */
    @Override
    protected void checkFallDamage(final double y, final boolean onGround,
            final net.minecraft.world.level.block.state.BlockState state, final BlockPos pos) {
        // Verbatim: the original's fall hook has an empty body.
    }

    // ------------------------------------------------------------ flags/state

    /** {@code :80-82} — {@code (data16 & 2) != 0}. */
    public boolean getIsSummoned() {
        return TaintSwarmAiLogic.isSummoned(this.entityData.get(DATA_FLAGS));
    }

    /** {@code :84-91} — set {@code flags | 2}, clear {@code flags & -4}. */
    public void setIsSummoned(final boolean summoned) {
        this.entityData.set(DATA_FLAGS,
                TaintSwarmAiLogic.flagsWithSummoned(this.entityData.get(DATA_FLAGS), summoned));
    }

    /** {@code :28} — the legacy damage bonus. */
    public int getDamBonus() {
        return this.damBonus;
    }

    /** {@code :28} — the legacy damage bonus, for the spawner/probe. */
    public void setDamBonus(final int bonus) {
        this.damBonus = bonus;
    }

    /** {@code :27} — the flight target (1.7.10 {@code ChunkCoordinates}). */
    public BlockPos getCurrentFlightTarget() {
        return this.currentFlightTarget;
    }

    /** {@code :27} — force the flight target (probe seam). */
    public void setCurrentFlightTarget(final BlockPos pos) {
        this.currentFlightTarget = pos;
    }

    /** The swing counter (1.7.10 {@code field_70724_aR}, {@code :199}). */
    public int getAttackTime() {
        return this.attackTime;
    }

    /** For probes: force the swing counter. */
    public void setAttackTime(final int ticks) {
        this.attackTime = ticks;
    }

    /** The client's tracked FX count (1.7.10 {@code swarm.size()}, {@code :112}). */
    public int getClientFxCount() {
        return this.clientFxCount;
    }

    // ------------------------------------------------------------------- tick

    @Override
    public void tick() {
        // func_70071_h_ :101-116.
        super.tick();                                        // :102
        // :103 — the swarm's own y drag, applied AFTER the super update.
        final Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x,
                TaintSwarmAiLogic.dragVertical(motion.y),
                motion.z);

        if (this.level().isClientSide) {
            // :104 — the whole rest of the body is the world.field_72995_K half.
            this.clientSwarmParticles();
        }
    }

    /**
     * {@code :105-115} — the client-only FX list maintenance, split out of
     * {@link #tick()} so a game-test server proves it never runs.
     *
     * <p>The rule the original applies is pinned in {@link TaintSwarmAiLogic}:
     * the scan removes ONE entry per tick — it breaks at the first null/dead FX
     * entry ({@code :106-110}) — and adds an FX whenever
     * {@code swarm.size() < Math.max(Thaumcraft.proxy.particleCount(25), 10)}
     * ({@code :112}) via {@code Thaumcraft.proxy.swarmParticleFX(world, this,
     * 0.22F, 15.0F, 0.08F)} ({@code :113}). This port ships no proxy/particle
     * seam, so no FX is created and nothing enters the list; the count stays 0
     * and the two rules are pinned by {@code TaintSwarmAiLogicTest}.</p>
     */
    private void clientSwarmParticles() {
        final int removable = TaintSwarmAiLogic.firstRemovableFxIndex(new boolean[this.clientFxCount]);
        if (removable >= 0) {
            this.clientFxCount--;
        }
        if (TaintSwarmAiLogic.spawnsFx(this.clientFxCount, TaintSwarmAiLogic.FX_BUDGET_REQUEST)) {
            // :113 — Thaumcraft.proxy.swarmParticleFX(world, this, 0.22F, 15.0F,
            // 0.08F). No FX seam in this port: left out of scope, list in the card.
        }
    }

    // ------------------------------------------------- updateEntityActionState

    /**
     * {@code :118-174} — the hand-written flight, hover and target steering.
     *
     * <p>The original's first statement is {@code super.func_70626_be()}
     * ({@code :119}); 1.7.10's {@code EntityCreature} body re-reads the target
     * provider {@code func_70782_k()} every tick, so that provider
     * ({@code :223-226}) is reproduced as the {@code setTarget} write below.
     * 1.7.10's {@code func_70623_bb()} (onEntityUpdate: potion ticks, the
     * attack/hurt counters) is plain 1.21.1 plumbing inside
     * {@code LivingEntity.aiStep}, so it has no line here.</p>
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();                          // :119

        // :223-226 func_70782_k(): double var1 = 12.0; return getIsSummoned() ?
        // null : world.getClosestPlayerToEntity(this, var1).
        this.setTarget(TaintSwarmAiLogic.providerSearches(this.getIsSummoned())
                ? this.level().getNearestPlayer(this, TaintSwarmAiLogic.TARGET_RANGE)
                : null);

        LivingEntity target = this.getTarget();
        final Level level = this.level();

        if (target == null) {
            // :121-123 — a summoned swarm burns 5.0 of DamageSource.generic
            // (field_76377_j) every tick it has no target: a summoned swarm dies.
            // This branch IS the "no target" half of the gate (:120), so the rule
            // is handed the branch's known state.
            if (TaintSwarmAiLogic.summonSelfDamageApplies(this.getIsSummoned(), false)) {
                this.hurt(level.damageSources().generic(), TaintSwarmAiLogic.SUMMON_SELF_DAMAGE);
            }

            // :125-136 — drop a flight target that is inside a block, below y 1,
            // above world.getHeight(x,z) + 8, or outside the taint biome
            // (1.7.10 compares world.getBiomeGenForCoords(x,z).biomeID).
            if (this.currentFlightTarget != null) {
                final int targetY = this.currentFlightTarget.getY();
                final int heightLimit = TaintSwarmAiLogic.flightHeightLimit(
                        level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                                this.currentFlightTarget.getX(), this.currentFlightTarget.getZ()));
                // 1.7.10 biomes are per-column; 1.21.1's palette needs a y, so the
                // lookup y is clamped into the world — the read stays column-equivalent.
                final BlockPos column = new BlockPos(this.currentFlightTarget.getX(),
                        Mth.clamp(targetY, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1),
                        this.currentFlightTarget.getZ());
                if (TaintSwarmAiLogic.flightTargetIsStale(level.isEmptyBlock(this.currentFlightTarget),
                        targetY, heightLimit,
                        TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, column)))) {
                    this.currentFlightTarget = null;
                }
            }

            // :138-146 — re-pick when there is no target, on a 1-in-30 roll, or on
            // arrival. The roll is written last-in-chain so that the null case
            // short-circuits exactly like the original and consumes no RNG.
            final int selfX = TaintSwarmAiLogic.truncate(this.getX());
            final int selfY = TaintSwarmAiLogic.truncate(this.getY());
            final int selfZ = TaintSwarmAiLogic.truncate(this.getZ());
            boolean repick = this.currentFlightTarget == null;
            if (!repick) {
                repick = TaintSwarmAiLogic.wantsNewFlightTarget(
                        this.random.nextInt(TaintSwarmAiLogic.FLIGHT_REPICK_ROLL),
                        TaintSwarmAiLogic.flightTargetDistanceSquared(this.currentFlightTarget,
                                selfX, selfY, selfZ));
            }
            if (repick) {
                // :141-145 — the rolls are consumed in source order.
                this.currentFlightTarget = TaintSwarmAiLogic.pickFlightTarget(
                        new BlockPos(selfX, selfY, selfZ),
                        this.random.nextInt(TaintSwarmAiLogic.FLIGHT_ROLL_XZ),
                        this.random.nextInt(TaintSwarmAiLogic.FLIGHT_ROLL_XZ),
                        this.random.nextInt(TaintSwarmAiLogic.FLIGHT_ROLL_Y),
                        this.random.nextInt(TaintSwarmAiLogic.FLIGHT_ROLL_XZ),
                        this.random.nextInt(TaintSwarmAiLogic.FLIGHT_ROLL_XZ));
            }

            // :148-157 — steer toward the waypoint, then face the motion vector.
            final double dx = TaintSwarmAiLogic.idleDeltaX(this.currentFlightTarget.getX(), this.getX());
            final double dy = TaintSwarmAiLogic.idleDeltaY(this.currentFlightTarget.getY(), this.getY());
            final double dz = TaintSwarmAiLogic.idleDeltaZ(this.currentFlightTarget.getZ(), this.getZ());
            final Vec3 m = this.getDeltaMovement();
            this.setDeltaMovement(
                    TaintSwarmAiLogic.steerHorizontal(m.x, dx, TaintSwarmAiLogic.STEER_IDLE_XZ_LERP),
                    TaintSwarmAiLogic.steerVertical(m.y, dy),
                    TaintSwarmAiLogic.steerHorizontal(m.z, dz, TaintSwarmAiLogic.STEER_IDLE_XZ_LERP));
            final Vec3 steered = this.getDeltaMovement();
            this.setYRot(this.getYRot() + TaintSwarmAiLogic.yawStep(steered.x, steered.z, this.getYRot()));
            this.setZza(TaintSwarmAiLogic.MOVE_FORWARD_INPUT);   // :156 field_70701_bs = 0.1F
        } else if (target != null) {
            // :158 — the original's mirrored null guard
            // (`else if (this.field_70789_a != null)`), kept verbatim.
            // :158-168 — steer toward the target. The x and z lerps are NOT the
            // same literal in the original (:162 vs :164); both ship verbatim.
            final double dx = TaintSwarmAiLogic.targetDeltaX(target.getX(), this.getX());
            final double dy = TaintSwarmAiLogic.targetDeltaY(target.getY(), target.getEyeHeight(),
                    this.getY());
            final double dz = TaintSwarmAiLogic.targetDeltaZ(target.getZ(), this.getZ());
            final Vec3 m = this.getDeltaMovement();
            this.setDeltaMovement(
                    TaintSwarmAiLogic.steerHorizontal(m.x, dx, TaintSwarmAiLogic.STEER_TARGET_X_LERP),
                    TaintSwarmAiLogic.steerVertical(m.y, dy),
                    TaintSwarmAiLogic.steerHorizontal(m.z, dz, TaintSwarmAiLogic.STEER_TARGET_Z_LERP));
            final Vec3 steered = this.getDeltaMovement();
            this.setYRot(this.getYRot() + TaintSwarmAiLogic.yawStep(steered.x, steered.z, this.getYRot()));
            this.setZza(TaintSwarmAiLogic.MOVE_FORWARD_INPUT);   // :167
        }

        // :171-173 — a creative player is dropped as a target.
        target = this.getTarget();
        if (target instanceof Player player
                && TaintSwarmAiLogic.clearsTarget(player.isCreative())) {
            this.setTarget(null);
        }
    }

    /**
     * {@code :198-221} — 1.7.10's {@code attackEntity(Entity, float)} hook, ported
     * line for line. The original never calls it: it installs no goal selectors,
     * and its own hand-written {@code func_70626_be} has no call site, so the hook
     * is dead code there. The port keeps it dead for the same reason — calling it
     * would be a modernisation, not a port — and pins its cadence and gates in
     * {@link TaintSwarmAiLogic#canMelee(int, float, boolean)} and
     * {@link TaintSwarmAiLogic#meleeCooldown(net.minecraft.util.RandomSource)}.
     *
     * <p>The two 1.7.10 details worth naming: the swing saves the target's motion
     * and writes it back ({@code :208-210}, {@code :216-218}), which cancels the
     * vanilla knockback {@code func_70652_k} just applied; and {@code :215} writes
     * the target's {@code onGround} flag ({@code field_70160_al}) to false — 1.21.1
     * keeps {@code onGround} private with a read-only {@code onGround()} accessor,
     * so that write has no seam and is not carried.</p>
     */
    protected void attackEntity(final Entity target, final float distance) {
        final boolean overlap = target.getBoundingBox().maxY > this.getBoundingBox().minY
                && target.getBoundingBox().minY < this.getBoundingBox().maxY;   // :201-202
        if (!TaintSwarmAiLogic.canMelee(this.attackTime, distance, overlap)) {
            return;
        }

        if (this.getIsSummoned()) {
            // :203-205 — EntityUtils.setRecentlyHit(target, 100): 1.7.10 writes the
            // private EntityLiving.recentlyHit (field_70718_bc) by reflection. The
            // 1.21.1 counter with the same job (a hit entity stops despawning) is
            // LivingEntity#setNoActionTime.
            if (target instanceof LivingEntity living) {
                living.setNoActionTime(TaintSwarmAiLogic.RECENTLY_HIT_TICKS);
            }
        }

        this.attackTime = TaintSwarmAiLogic.meleeCooldown(this.random);          // :207
        final Vec3 saved = target.getDeltaMovement();                           // :208-210
        // :211-213 — the hit, then weakness 100 ticks amp 0, server side only
        // (customServerAiStep is server-only, so the original's !isRemote half is
        // already the gate).
        if (this.doHurtTarget(target) && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    TaintSwarmAiLogic.WEAKNESS_TICKS, TaintSwarmAiLogic.WEAKNESS_AMPLIFIER));
        }
        target.setDeltaMovement(saved);                                         // :216-218
        // :219 — world.playSound(this, "thaumcraft:swarmattack", 0.3F,
        // 0.9F + world.rand.nextFloat() * 0.2F): the WORLD's rng drives the pitch.
        this.playSound(ModSounds.SWARMATTACK.get(), TaintSwarmAiLogic.ATTACK_SOUND_VOLUME,
                TaintSwarmAiLogic.attackSoundPitch(level().random.nextFloat()));
    }

    // ---------------------------------------------------------------- spawning

    /** {@code :240-247} — light gate first, then the vanilla check. */
    @Override
    public boolean checkSpawnRules(final LevelAccessor level, final MobSpawnType spawnType) {
        final int y = Mth.floor(this.getBoundingBox().minY);   // :241 floor_double(boundingBox.minY)
        final int x = Mth.floor(this.getX());                 // :242 floor_double(posX)
        final int z = Mth.floor(this.getZ());                 // :243 floor_double(posZ)
        final int light = level.getMaxLocalRawBrightness(new BlockPos(x, y, z)); // :244
        return TaintSwarmAiLogic.lightAllowsSpawn(light,
                this.random.nextInt(TaintSwarmAiLogic.SPAWN_LIGHT_ROLL_BOUND))    // :245-246
                && super.checkSpawnRules(level, spawnType);                       // :246
    }

    // -------------------------------------------------------------------- loot

    /**
     * {@code :257-261} — {@code dropFewItems}: a coin flip drops one Tainted Goo
     * (itemResource meta 11) at the 1.7.10 {@code height} field's half-height
     * offset. The looting level and the "recently hit" flag are ignored by the
     * original, so they are ignored here.
     */
    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source,
            final boolean killedByPlayer) {
        if (TaintSwarmAiLogic.dropsGoo(this.random.nextBoolean())) {              // :258
            this.spawnAtLocation(ItemResource.stack(TaintSwarmAiLogic.META_TAINTED_GOO),
                    TaintSwarmAiLogic.dropOffset());                             // :259
        }
        super.dropCustomDeathLoot(level, source, killedByPlayer);
    }

    // --------------------------------------------------------------------- nbt

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // :236-237 — "Flags" is the raw flag byte; "damBonus" is narrowed to a byte.
        tag.putByte(TaintSwarmAiLogic.NBT_FLAGS, this.entityData.get(DATA_FLAGS));
        tag.putByte(TaintSwarmAiLogic.NBT_DAM_BONUS,
                TaintSwarmAiLogic.damBonusToNbt(this.damBonus));
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // :230-231 — both reads are ungated on hasKey: a swarm saved by a version
        // that never wrote them loads with flags 0 and bonus 0. The bonus is read
        // as a byte, so a value over 127 wraps to a negative. 1.7.10 evaluated
        // "2 + damBonus" in the constructor, so the loaded value never reaches the
        // attribute — the port matches by leaving the attribute alone.
        this.entityData.set(DATA_FLAGS, tag.getByte(TaintSwarmAiLogic.NBT_FLAGS));
        this.damBonus = TaintSwarmAiLogic.damBonusFromNbt(tag.getByte(TaintSwarmAiLogic.NBT_DAM_BONUS));
    }
}
