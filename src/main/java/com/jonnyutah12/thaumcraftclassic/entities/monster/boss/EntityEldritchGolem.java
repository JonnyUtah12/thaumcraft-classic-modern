package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import com.jonnyutah12.thaumcraftclassic.api.entities.IEldritchMob;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockLoot;
import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AIAttackOnCollide;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AILongRangeAttack;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityGolemOrb;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Eldritch Golem — {@code thaumcraft:eldritchgolem} (content-mapping
 * {@code Thaumcraft:EldritchGolem}, mod_index 31), {@code thaumcraft.common.entities.monster.boss.EntityEldritchGolem}
 * (370 lines, {@code extends EntityThaumcraftBoss implements IEldritchMob, IRangedAttackMob}).
 * Registration {@code ConfigEntities.java:119} — {@code (64, 3, true)}, egg pair {@code :120}.
 *
 * <p>This class replaces the TC-M6-08-s-c registration stub ({@code extends Monster}, no boss
 * body) on the SAME registered id — the registration stays single, in
 * {@code CatalogGolems.ELDRITCH_GOLEM}, and is repointed at this class. Every figure rides
 * {@link EldritchGolemAiLogic} and is pinned by {@code EldritchGolemAiLogicTest}; the shipped
 * wiring is measured by the golem probes of {@code BossMobGameTest}.</p>
 *
 * <p>The boss is the base plus the headless phase machine: the constructor's goal table has NO
 * ranged goal ({@code :46-53}), and the ONLY writer of the slot-2 {@code AILongRangeAttack(3.0,
 * 1.0, 5, 5, 24.0F)} is {@code makeHeadless()} ({@code :185-187}), which the damage rule of
 * {@code :170-183} calls once — on a server-side hit above the golem's own max health it sets
 * the headless byte, restarts the 100-tick spawn timer, detonates a 2.0F blast at the yaw
 * offset, and consumes the hit. From then on the charge machine ({@code :346-358}) fills
 * {@code beamCharge} to 150 one tick at a time, broadcasting status 19 per tick, and the ranged
 * goal spends it in 15..19-point draws on <b>blue</b> {@code EntityGolemOrb}s ({@code :225}, the
 * {@code false} flag of the pair the cleric lane pinned — the praetor's {@code true} is red).</p>
 *
 * <p>The melee swing ({@code :189-211}) is the {@code func_70652_k} twin
 * {@code Mob#doHurtTarget}: a 10-tick cadence, the status-4 broadcast, damage at
 * {@code attackDamage * 0.75F}, a +0.2 lift, and while headless the yaw knockback of
 * {@code :201-205}. The footstep legs of {@code :135-167} crush a {@code BlockLoot} cell and
 * any block of hardness in {@code [0.0, 0.15]} under the feet. Seams, recorded as deviations and
 * never faked: the particle pair of {@code :141-151} and the client FX of {@code :292-345}
 * (spark, vent particles, {@code arcLightning}) have no seam in this card — the renderer lane
 * owns the drawing, and this class ships the state it reads (the arc anchor, the arc countdown,
 * the headless byte, the eye-height pair).</p>
 */
public class EntityEldritchGolem extends EntityThaumcraftBoss
        implements IEldritchMob, RangedAttackMob {

    /**
     * {@code :66-70} — {@code func_70088_a} ({@code initEntity}): {@code addObject(12, (byte)0)},
     * the watched headless byte. 21.1.259 allocates the slot itself; the index 12 is pinned as
     * data on {@code EldritchGolemAiLogic} per BD-132's family rule.
     */
    private static final EntityDataAccessor<Byte> HEADLESS =
            SynchedEntityData.defineId(EntityEldritchGolem.class, EntityDataSerializers.BYTE);

    /** {@code :36} — the beam's ammo, charged 0..150 and spent by {@code :215}. */
    private int beamCharge;
    /** {@code :37} — the charge flag: true while the beam is refilling. */
    private boolean chargingBeam;
    /** {@code :38} — the arc countdown, written by status 19 and drained by the client draw. */
    private int arcing;
    /** {@code :39-41} — the arc anchor cell picked by status 19 ({@code :260-262}). */
    private int arcX;
    private int arcY;
    private int arcZ;
    /** {@code :42} — the swing cadence timer ({@code :190-193}, {@code :131-133}). */
    private int attackTimer;

    public EntityEldritchGolem(final EntityType<? extends EntityEldritchGolem> type,
            final Level level) {
        super(type, level);
        // :55 this.field_70178_ae = true (setAIEnabled) is 1.21.1's standing state, and the
        // box of :54 rides the EntityType builder (CatalogGolems), not a setSize call.
    }

    /** {@code :104-109} over the boss base's pair, plus the armour bonus of {@code :100}. */
    public static AttributeSupplier.Builder createAttributes() {
        return EldritchGolemAiLogic.createAttributes();
    }

    /** {@code :66-70} — the watched headless byte. */
    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HEADLESS, (byte) 0);                                           // :69
    }

    // ------------------------------------------------------------------ headless state (:72-101)

    /** {@code :72-74} — {@code isHeadless()} reads the watched byte against 1. */
    public boolean isHeadless() {
        return this.entityData.get(HEADLESS).intValue() == 1;
    }

    /** {@code :76-78} — {@code setHeadless(boolean)} writes the byte. */
    public void setHeadless(final boolean headless) {
        this.entityData.set(HEADLESS, (byte) (headless ? 1 : 0));
    }

    /** {@code :80-84} — {@code writeToNBT}: the {@code headless} boolean. */
    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("headless", isHeadless());                                     // :83
    }

    /**
     * {@code :86-93} — {@code readFromNBT}: the byte is restored and, when it says headless,
     * {@code makeHeadless()} re-installs the slot-2 ranged goal. The goal table is not saved,
     * so this read is the ONLY thing that puts the mid-fight goal back on a loaded golem.
     */
    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHeadless(tag.getBoolean("headless"));                                      // :89
        if (isHeadless()) {
            makeHeadless();                                                           // :90-92
        }
    }

    /**
     * {@code :95-97} — {@code func_70047_e()} ({@code getEyeHeight}): 3.0F, 3.33F headless.
     * 21.1.259's eye height is {@code Entity#getEyeHeight(Pose)} and is {@code final}, derived
     * from the box's {@code EntityDimensions}, so the override has no seam: the pair ships as
     * this accessor, feeding the explosion height of {@code :177} and reading as the head
     * height for the renderer lane.
     */
    public float eldritchEyeHeight() {
        return EldritchGolemAiLogic.eyeHeightFor(isHeadless());
    }

    /** {@code :99-101} — {@code func_70658_aO()} ({@code getArmor()} = {@code super + 6}). */
    public double armorBonus() {
        return EldritchGolemAiLogic.ARMOR_BONUS;
    }

    /**
     * {@code :112-114} — {@code func_70599_a(DamageSource)} ({@code getHurtSound}):
     * {@code mob.irongolem.hit}, which 21.1.259 names {@code SoundEvents.IRON_GOLEM_HURT}.
     */
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(final DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    /** {@code :115-117} — {@code func_70639_aQ()} ({@code getDeathSound}): the death twin. */
    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    /**
     * {@code :118-121} — {@code func_70636_d}'s step arm:
     * {@code func_145780_a("mob.irongolem.walk", 1.0F, 1.0F)} — the walk sample at a flat
     * 1.0F/1.0F pair, so the step is the golem's own event, not the {@code SoundType} of the
     * block under it.
     */
    @Override
    protected void playStepSound(final BlockPos pos, final BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, EldritchGolemAiLogic.SOUND_VOLUME,
                EldritchGolemAiLogic.SOUND_PITCH);
    }

    /** {@code :111-121} — the trio's volume: 1.0F. */
    @Override
    protected float getSoundVolume() {
        return EldritchGolemAiLogic.SOUND_VOLUME;
    }

    /** {@code :111-121} — the trio's pitch: 1.0F, flat (vanilla's body jitters by 0.2F). */
    @Override
    public float getVoicePitch() {
        return EldritchGolemAiLogic.SOUND_PITCH;
    }

    // ------------------------------------------------------------------ goals (:46-53, :186)

    /**
     * {@code :46-53} — the constructor's goal table, slot for slot, in the original's order:
     *
     * <pre>
     *   0  EntityAISwimming                        → FloatGoal
     *   3  AIAttackOnCollide(EntityLivingBase, 1.1, false) → TC goal: MeleeAttackGoal + class filter
     *   6  EntityAIMoveTowardsRestriction(0.8)     → MoveTowardsRestrictionGoal
     *   7  EntityAIWander(0.8)                     → RandomStrollGoal
     *   8  EntityAIWatchClosest(Player, 8.0F)      → LookAtPlayerGoal (0.02 chance)
     *   8  EntityAILookIdle                        → RandomLookAroundGoal
     *   target 1 EntityAIHurtByTarget(this, true)
     *   target 2 EntityAINearestAttackableTarget(Player, 0, true)
     * </pre>
     *
     * <p>There is NO ranged goal here: the slot-2 {@code AILongRangeAttack(3.0, 1.0, 5, 5,
     * 24.0F)} is installed by {@code makeHeadless()} alone ({@code :185-187}), which the phase
     * rule of {@code :170-183} reaches once. The melee row is Thaumcraft's own
     * {@code ai/combat/AIAttackOnCollide} (the vanilla melee body plus the {@code classTarget}
     * admission and the boss's spawn-timer gate), the same class the praetor ships.</p>
     *
     * <p>Slots {@code :50}/{@code :51} collide at priority 8 — 1.7.10's {@code TreeMap} let the
     * LookIdle replace the WatchClosest (five live movement goals), 1.21.1's {@code GoalSelector}
     * keeps both (BD-112, measured on the cleric's set), so the pair ships at the original slot
     * and the set measures six. The {@code Monster} constructor's own goals are stripped by not
     * calling {@code super.registerGoals()} — the family's idiom — and {@code :52}'s
     * {@code EntityAIHurtByTarget(this, true)} is 1.7.10 vanilla's class, so the port ships
     * vanilla's {@code HurtByTargetGoal} (its alert-others twin {@code setAlertOthers} is
     * deliberately never called, the golem lane's reading of the flag).</p>
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(EldritchGolemAiLogic.GOAL_SLOT_SWIM, new FloatGoal(this)); // :46
        this.goalSelector.addGoal(EldritchGolemAiLogic.GOAL_SLOT_MELEE,
                new AIAttackOnCollide(this, LivingEntity.class,
                        EldritchGolemAiLogic.MELEE_SPEED,
                        EldritchGolemAiLogic.MELEE_LONG_MEMORY));                   // :47
        this.goalSelector.addGoal(EldritchGolemAiLogic.GOAL_SLOT_RESTRICTION,
                new MoveTowardsRestrictionGoal(this, EldritchGolemAiLogic.RESTRICTION_SPEED)); // :48
        this.goalSelector.addGoal(EldritchGolemAiLogic.GOAL_SLOT_WANDER,
                new RandomStrollGoal(this, EldritchGolemAiLogic.WANDER_SPEED));      // :49
        this.goalSelector.addGoal(EldritchGolemAiLogic.GOAL_SLOT_WATCH,
                new LookAtPlayerGoal(this, Player.class, EldritchGolemAiLogic.WATCH_PLAYER_RANGE,
                        EldritchGolemAiLogic.WATCH_PLAYER_PROBABILITY));             // :50
        this.goalSelector.addGoal(EldritchGolemAiLogic.GOAL_SLOT_LOOK_IDLE,
                new RandomLookAroundGoal(this));                                    // :51
        this.targetSelector.addGoal(EldritchGolemAiLogic.TARGET_SLOT_HURT,
                new HurtByTargetGoal(this));                                        // :52
        this.targetSelector.addGoal(EldritchGolemAiLogic.TARGET_SLOT_PLAYER,
                new NearestAttackableTargetGoal<>(this, Player.class,
                        EldritchGolemAiLogic.TARGET_MUST_SEE));                     // :53
    }

    /**
     * {@code :185-187} — {@code makeHeadless()}: the ONE write of the mid-fight goal,
     * {@code AILongRangeAttack(this, 3.0, 1.0, 5, 5, 24.0F)} at slot 2. It installs nothing
     * else — the headless byte and the spawn timer are the damage leg's ({@code :173-174}), and
     * the NBT read of {@code :90-92} calls this method alone.
     */
    void makeHeadless() {
        this.goalSelector.addGoal(EldritchGolemAiLogic.GOAL_SLOT_RANGED,
                new AILongRangeAttack(this, EldritchGolemAiLogic.RANGED_MIN_DISTANCE,
                        EldritchGolemAiLogic.RANGED_SPEED, EldritchGolemAiLogic.RANGED_INTERVAL_MIN,
                        EldritchGolemAiLogic.RANGED_INTERVAL_MAX, EldritchGolemAiLogic.RANGED_RANGE)); // :186
    }

    // ------------------------------------------------------------------ spawn (:123-127)

    /**
     * {@code :123-127} — {@code onInitialSpawn}: the 100-tick spawn phase, then super. The
     * family's BD-98 idiom runs the mob's own pass after the super call (the boss base adds
     * its 24-block home there, {@code EntityThaumcraftBoss:72-75}); the spawn timer is
     * independent of it, so the order is the family's, recorded as its own deviation.
     */
    @Override
    public SpawnGroupData finalizeSpawn(final ServerLevelAccessor level,
            final DifficultyInstance difficulty, final MobSpawnType reason,
            final SpawnGroupData groupData) {
        setSpawnTimer(EldritchGolemAiLogic.SPAWN_TIMER_PHASE_TICKS);                   // :125
        return super.finalizeSpawn(level, difficulty, reason, groupData);              // :126
    }

    // ------------------------------------------------------------------ damage (:170-183)

    /**
     * {@code :170-183} — {@code func_70097_a(DamageSource, float)}
     * ({@code attackEntityFrom}): on a server-side hit whose amount is above the golem's own
     * max health ({@code func_110143_aJ()}) and while it is not headless yet, the golem is
     * beheaded: the byte is set, the spawn timer restarts at 100, a 2.0F blast fires at
     * {@code (x + cos(yaw)*0.75, y + getEyeHeight(), z + sin(yaw)*0.75)} and
     * {@code makeHeadless()} installs the mid-fight goal — and the hit is CONSUMED, so the base's
     * aggro/rage/clamp legs never see it. Every other hit rides the boss base's damage leg.
     */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (!this.level().isClientSide                                                // :172
                && EldritchGolemAiLogic.shouldGoHeadless(amount, this.getMaxHealth(),
                        isHeadless())) {                                              // :172
            setHeadless(true);                                                        // :173
            setSpawnTimer(EldritchGolemAiLogic.SPAWN_TIMER_PHASE_TICKS);              // :174
            final double xx = EldritchGolemAiLogic.explosionOffsetX(this.getYRot());  // :175
            final double zz = EldritchGolemAiLogic.explosionOffsetZ(this.getYRot());  // :176
            this.level().explode(this, this.getX() + xx,
                    this.getY() + eldritchEyeHeight(), this.getZ() + zz,
                    EldritchGolemAiLogic.EXPLOSION_STRENGTH, false,
                    Level.ExplosionInteraction.MOB);                                  // :177
            makeHeadless();                                                           // :178
            return false;                                                             // :179
        }
        return super.hurt(source, bossDamageLeg(source, amount));                     // :181
    }

    // ------------------------------------------------------------------ melee (:189-211)

    /**
     * {@code :189-211} — {@code func_70652_k(Entity)} ({@code attackEntityFrom(Entity)}, the
     * melee swing the melee goal calls when the target is in reach; 1.21.1's twin is
     * {@code Mob#doHurtTarget}). The 10-tick gate runs first, the timer is written ON the gate,
     * status 4 is broadcast, and the damage is {@code attackDamage * 0.75F}. On a landed hit the
     * target takes a +0.2 lift, and while the golem is headless the yaw knockback of
     * {@code :201-205} is added on top ({@code addVelocity(-sin(yaw)*1.5, 0.1, cos(yaw)*1.5)}).
     *
     * <p>The one seam: the original attributes the hit to
     * {@code DamageSource.func_76358_a(this)} — 1.7.10's <em>player</em>-sourced damage built
     * from a mob — and 21.1.259's player damage source takes a {@code Player}, so a mob cannot
     * build one; the hit rides the melee seam's own {@code mobAttack} type. Recorded as a
     * deviation; amount, cadence, broadcast, lift and knockback are the original's.</p>
     */
    @Override
    public boolean doHurtTarget(final Entity target) {
        if (!EldritchGolemAiLogic.meleeGate(this.attackTimer)) {                      // :190-192
            return false;
        }
        this.attackTimer = EldritchGolemAiLogic.ATTACK_TIMER_TICKS;                   // :193
        this.level().broadcastEntityEvent(this, EldritchGolemAiLogic.STATUS_MELEE);   // :194
        final boolean flag = target.hurt(this.damageSources().mobAttack(this),
                EldritchGolemAiLogic.meleeDamageFor(
                        (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE)));   // :195-197
        if (flag) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0,
                    EldritchGolemAiLogic.MELEE_LIFT, 0.0));                           // :199
            if (isHeadless()) {                                                       // :200
                target.setDeltaMovement(target.getDeltaMovement().add(
                        EldritchGolemAiLogic.headlessKnockbackX(this.getYRot()),
                        EldritchGolemAiLogic.HEADLESS_KNOCKBACK_LIFT,
                        EldritchGolemAiLogic.headlessKnockbackZ(this.getYRot())));    // :201-205
            }
        }
        return flag;                                                                  // :209
    }

    // ------------------------------------------------------------------ beam (:213-236)

    /**
     * {@code :213-236} — {@code func_82196_d(EntityLivingBase, float)}
     * ({@code performRangedAttack}), driven by the headless goal of {@code :186}. The gate is
     * all three arms of {@code :214}: the target twin (BD-128's family finding —
     * {@code func_70685_l} is the CLASS twin, not the faction predicate, and this class
     * overrides neither), and the beam's own state, {@code !chargingBeam && beamCharge > 0}.
     * The spend is {@code 15 + random.nextInt(5)} ({@code :215}); the look helper aims at
     * {@code (x, boundingBox.minY + height/2, z)} with speed 30.0F/30.0F ({@code :216-223});
     * the projectile is the <b>blue</b> orb of {@code :225} ({@code false} — the flag pair the
     * cleric lane pinned: blue lives 160 ticks and hits for 0.6, red lives 240 for 1.0 —
     * {@code EntityGolemOrb.java:84-99}),
     * nudged along the GOLEM's own look vector ({@code :224-228}, not the orb's motion, which
     * is where this class differs from the praetor's nudge of BD-133); the aim adds the
     * target's own motion to x/z and drops half its height at y ({@code :229-231}); the shot
     * leaves at 0.66F with spread 5.0F ({@code :232}) — no aim lift, unlike the praetor's +2.0 —
     * and {@code thaumcraft:egattack} plays at 1.0F / 1.0F + rand*0.1F ({@code :233}).
     */
    @Override
    public void performRangedAttack(final LivingEntity target, final float distanceFactor) {
        if (!EldritchGolemAiLogic.beamGate(isBeamTargetable(target), this.chargingBeam,
                this.beamCharge)) {                                                   // :214
            return;
        }
        this.beamCharge -= EldritchGolemAiLogic.beamSpendFor(
                this.random.nextInt(EldritchGolemAiLogic.BEAM_SPEND_JITTER));         // :215
        this.getLookControl().setLookAt(target.getX(),
                target.getBoundingBox().minY + target.getBbHeight() / 2.0F,           // :219
                target.getZ(), EldritchGolemAiLogic.AIM_ROTATION_SPEED,
                EldritchGolemAiLogic.AIM_ROTATION_SPEED);                             // :216-223
        final Vec3 look = this.getLookAngle();                                        // :224
        final EntityGolemOrb blast = new EntityGolemOrb(CatalogGolems.GOLEM_ORB.get(),
                this.level(), this, target, EldritchGolemAiLogic.ORB_IS_RED);         // :225
        blast.setPos(blast.getX() + look.x() * EldritchGolemAiLogic.ORB_NUDGE_LOOK_SCALE, // :226
                blast.getY(),
                blast.getZ() + look.z() * EldritchGolemAiLogic.ORB_NUDGE_LOOK_SCALE); // :227-228
        final double d0 = target.getX() + target.getDeltaMovement().x() - this.getX();// :229
        final double d1 = target.getY() - this.getY() - target.getBbHeight() / 2.0F;  // :230
        final double d2 = target.getZ() + target.getDeltaMovement().z() - this.getZ();// :231
        blast.shoot(d0, d1, d2, EldritchGolemAiLogic.ORB_SPEED, EldritchGolemAiLogic.ORB_SPREAD); // :232
        this.playSound(ModSounds.EGATTACK.get(), EldritchGolemAiLogic.ORB_SOUND_VOLUME,
                EldritchGolemAiLogic.ORB_SOUND_PITCH_BASE
                        + this.random.nextFloat() * EldritchGolemAiLogic.ORB_SOUND_PITCH_JITTER); // :233
        this.level().addFreshEntity(blast);                                           // :234
    }

    /**
     * {@code :214} — {@code func_70685_l(EntityLivingBase)}: 1.7.10's target twin (alive, in
     * range, class-admitted). The shipped arms are the self-exclusion and
     * {@code Mob#canAttackType}, the praetor's reading of BD-128; the class-admission arm is
     * the live one because this class overrides neither half of the can-attack pair.
     */
    private boolean isBeamTargetable(final LivingEntity target) {
        return target != null && target != this && target.isAlive()
                && this.canAttackType(target.getType());
    }

    // ------------------------------------------------------------------ tick (:129-168, :281-359)

    /**
     * {@code :281-359} ({@code func_70071_h_}, {@code onUpdate}) plus {@code :129-168}
     * ({@code func_70636_d}, {@code onLivingUpdate}), which 1.7.10's {@code onUpdate} calls from
     * inside its own super pass — so the legs land here in the original's order.
     *
     * <ol>
     *   <li>{@code :283-285} — a client-side 150-tick spawn timer rebroadcasts status 18. The
     *       server's own timer starts at 100 ({@code :125}), so this arm is reachable only after
     *       the client's status-18 handler wrote 150; it ships as written.</li>
     *   <li>{@code :287-289} — while the spawn timer runs, the golem heals 2.0F per tick,
     *       BEFORE the super pass (so the base's decrement runs after the heal).</li>
     *   <li>The super pass — the boss base's spawn-in body.</li>
     *   <li>{@code :131-133} — the swing timer decays.</li>
     *   <li>{@code :135-156} — the footstep gate: one roll in five over real horizontal motion,
     *       reading the block row at {@code floor(y - 0.2 - height)}. The server arm breaks a
     *       {@code BlockLoot} cell there ({@code :153-155}); the client arm's blockcrack
     *       particles ({@code :141-151}) are the FX lane's.</li>
     *   <li>{@code :158-167} — the crush gate: server-side, the block at
     *       {@code (floor(x + motionX), floor(boundingBox.minY), floor(z + motionZ))} goes away
     *       when its hardness sits in {@code [0.0, 0.15]}.</li>
     *   <li>{@code :346-358} — the server charge machine: a headless golem with an empty beam
     *       starts charging, and every charge tick adds 1, broadcasts status 19, and stops at
     *       exactly 150. The field is NOT capped — {@code :354} compares against 150, so 150 is
     *       as full as the beam gets.</li>
     *   <li>{@code :292-345} — the client's headless branch: the pitch is pinned to 0 and the
     *       arc countdown drains. The spark, the vent particles and the {@code arcLightning}
     *       draw of that branch are the renderer lane's; this class ships the state they read
     *       ({@code arcing}, the arc anchor) and nothing is faked in their place.</li>
     * </ol>
     */
    @Override
    public void aiStep() {
        if (getSpawnTimer() == EldritchGolemAiLogic.SPAWN_TIMER_BROADCAST_TICKS) {    // :283
            this.level().broadcastEntityEvent(this, EldritchGolemAiLogic.STATUS_SPAWN_PHASE); // :284
        }
        if (getSpawnTimer() > 0) {                                                    // :287
            this.heal(EldritchGolemAiLogic.SPAWN_HEAL_AMOUNT);                        // :288
        }
        super.aiStep();                                                               // :291
        if (this.attackTimer > 0) {
            this.attackTimer--;                                                       // :131-133
        }
        final Level level = this.level();
        if (EldritchGolemAiLogic.footstepGate(this.getDeltaMovement().x(),
                this.getDeltaMovement().z(),
                this.random.nextInt(EldritchGolemAiLogic.FOOTSTEP_CHANCE_DIVISOR))) { // :135
            final int i = Mth.floor(this.getX());                                     // :136
            final int j = Mth.floor(this.getY() - EldritchGolemAiLogic.FOOTSTEP_DEPTH_BELOW_FEET
                    - this.getBbHeight());                                            // :137
            final int k = Mth.floor(this.getZ());                                     // :138
            final BlockState state = level.getBlockState(new BlockPos(i, j, k));      // :139
            if (!state.isAir() && !level.isClientSide
                    && state.getBlock() instanceof BlockLoot) {                       // :153-155
                level.destroyBlock(new BlockPos(i, j, k), true);                      // :154
            }
        }
        if (!level.isClientSide) {                                                    // :158
            final BlockPos crush = new BlockPos(Mth.floor(this.getX() + this.getDeltaMovement().x()),
                    Mth.floor(this.getBoundingBox().minY),                            // :160
                    Mth.floor(this.getZ() + this.getDeltaMovement().z()));
            final BlockState crushState = level.getBlockState(crush);                 // :162
            if (EldritchGolemAiLogic.crushGate(
                    crushState.getDestroySpeed(level, crush))) {                      // :163-164
                level.destroyBlock(crush, true);                                      // :165
            }
        }
        if (level.isClientSide) {                                                     // :292
            if (isHeadless()) {
                this.setXRot(0.0F);                                                   // :294
                if (this.arcing > 0) {
                    this.arcing--;                                                    // :343
                }
            }
        } else {
            if (EldritchGolemAiLogic.chargeStarts(isHeadless(), this.beamCharge)) {   // :347-349
                this.chargingBeam = true;
            }
            if (EldritchGolemAiLogic.chargeAdvances(isHeadless(), this.chargingBeam)) {// :351
                this.beamCharge += EldritchGolemAiLogic.CHARGE_STEP;                  // :352
                level.broadcastEntityEvent(this, EldritchGolemAiLogic.STATUS_CHARGE); // :353
                if (EldritchGolemAiLogic.chargeStops(this.beamCharge)) {              // :354
                    this.chargingBeam = false;                                        // :355
                }
            }
        }
    }

    // ------------------------------------------------------------------ client status (:238-279)

    /**
     * {@code :238-279} — {@code func_70103_a(byte)} ({@code handleEntityEvent}), the client
     * half of the three broadcasts this class makes. Status 4 is the swing (the 10-tick timer
     * and the golem's attack sound at 1.0F/1.0F — the 1.7.10 name {@code mob.irongolem.throw}
     * is 21.1.259's {@code SoundEvents.IRON_GOLEM_ATTACK}); 18 is the 150-tick spawn phase; 19
     * is a charge tick, whose body picks the arc anchor cell and plays {@code thaumcraft:jacobs}
     * (the arc's {@code arcLightning} draw is the renderer lane's, and it reads the anchor and
     * countdown shipped here). Anything else rides super — as {@code :276-278} writes it.
     */
    @Override
    public void handleEntityEvent(final byte status) {
        if (status == EldritchGolemAiLogic.STATUS_MELEE) {                            // :240
            this.attackTimer = EldritchGolemAiLogic.ATTACK_TIMER_TICKS;               // :241
            this.playSound(SoundEvents.IRON_GOLEM_ATTACK,
                    EldritchGolemAiLogic.STATUS_MELEE_VOLUME,
                    EldritchGolemAiLogic.STATUS_MELEE_PITCH);                         // :242
        } else if (status == EldritchGolemAiLogic.STATUS_SPAWN_PHASE) {               // :243
            setSpawnTimer(EldritchGolemAiLogic.SPAWN_TIMER_BROADCAST_TICKS);          // :244
        } else if (status == EldritchGolemAiLogic.STATUS_CHARGE) {                    // :245
            pickArcAnchor();                                                          // :246-275
        } else {
            super.handleEntityEvent(status);                                          // :276-278
        }
    }

    /**
     * {@code :246-275} — the arc anchor pick of status 19. Only runs while the countdown is
     * idle ({@code :246}); the anchor cell is thrown 2..4 blocks out at a random bearing,
     * walked DOWN at most five steps while the cell stays air ({@code :255-257}), and accepted
     * when the cell above is air and the cell itself is solid ({@code :259}) — then the
     * countdown starts at 8..12 ({@code :263}) and {@code thaumcraft:jacobs} plays at 0.8F
     * with the jittered pitch of {@code :271}.
     */
    private void pickArcAnchor() {
        if (this.arcing != 0) {                                                       // :246
            return;
        }
        final double radius = EldritchGolemAiLogic.arcRadiusFor(this.random.nextFloat()); // :247
        final double radians = Math.toRadians(this.random.nextInt(360));              // :248-250
        final int bx = Mth.floor(this.getX() + radius * Math.cos(radians));           // :251
        int by = Mth.floor(this.getY());                                              // :252
        final int bz = Mth.floor(this.getZ() + radius * Math.sin(radians));           // :253
        for (int c = 0; c < EldritchGolemAiLogic.ARC_SCAN_STEPS
                && isAirCell(bx, by, bz); c++) {                                      // :255-257
            by--;
        }
        if (isAirCell(bx, by + 1, bz) && !isAirCell(bx, by, bz)) {                    // :259
            this.arcX = bx;                                                           // :260
            this.arcY = by;                                                           // :261
            this.arcZ = bz;                                                           // :262
            this.arcing = EldritchGolemAiLogic.arcTicksFor(
                    this.random.nextInt(EldritchGolemAiLogic.ARC_TICKS_JITTER));      // :263
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                    ModSounds.JACOBS.get(), SoundSource.NEUTRAL,
                    EldritchGolemAiLogic.ARC_SOUND_VOLUME,
                    EldritchGolemAiLogic.ARC_SOUND_PITCH_BASE + (this.random.nextFloat()
                            - this.random.nextFloat()) * EldritchGolemAiLogic.ARC_SOUND_PITCH_JITTER,
                    false);                                                           // :264-273
        }
    }

    /** {@code :255}, {@code :259} — {@code world.isAirBlock(x, y, z)}. */
    private boolean isAirCell(final int x, final int y, final int z) {
        return this.level().isLoaded(new BlockPos(x, y, z))
                && this.level().getBlockState(new BlockPos(x, y, z)).isAir();
    }

    // ------------------------------------------------------------------ accessors (:361-364, state)

    /**
     * {@code :361-364} — {@code getAttackTimer()} ({@code @SideOnly(CLIENT)}): the swing
     * counter the renderer reads. The renderer lane's arm-swing approximation of
     * {@code ModelEldritchGolem} currently rides 1.21.1's {@code getAttackAnim}
     * (t_03c5d088); this is the original's counter, now readable.
     */
    public int getAttackTimer() {
        return this.attackTimer;
    }

    /** {@code :36} — the beam's ammo, the charge machine's field. */
    public int getBeamCharge() {
        return this.beamCharge;
    }

    /** {@code :37} — the charge flag. */
    public boolean isChargingBeam() {
        return this.chargingBeam;
    }

    /**
     * {@code :36} probe seam — writes the beam's ammo. The field is the charge machine's and
     * the ranged gate's; the house's shape for a hook the probes must reach (as
     * {@code EntityGolemOrb#getDefaultGravityForProbe} and the portal's {@code rollCultistDrops}
     * are), so the golem probes can set up the mid-charge and dry-beam arms of {@code :214}.
     */
    public void setBeamChargeForProbe(final int charge) {
        this.beamCharge = charge;
    }

    /** {@code :37} probe seam — writes the charge flag (the same seam rule as above). */
    public void setChargingBeamForProbe(final boolean charging) {
        this.chargingBeam = charging;
    }

    /** {@code :38} — the arc countdown. */
    public int getArcing() {
        return this.arcing;
    }

    /** {@code :39-41} — the arc anchor cell. */
    public Vec3 getArcAnchor() {
        return new Vec3(this.arcX, this.arcY, this.arcZ);
    }
}
