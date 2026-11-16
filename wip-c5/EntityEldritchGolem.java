package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.Vec3;

import com.jonnyutah12.thaumcraftclassic.api.entities.IEldritchMob;
import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AIAttackOnCollide;
import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AIGolemAttackOnCollide;
import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AIHurtByTarget;
import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AINearestAttackableTarget;
import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AIWander;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AILongRangeAttack;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityGolemOrb;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;

/**
 * {@code thaumcraft:eldritchgolem} — original
 * {@code thaumcraft.common.entities.monster.boss.EntityEldritchGolem} (370 lines,
 * content-mapping {@code Thaumcraft:EldritchGolem}, mod_index 31, line 329),
 * {@code extends EntityThaumcraftBoss implements IEldritchMob, IRangedAttackMob}.
 * Registration {@code ConfigEntities.java:119} — {@code (64, 3, true)}.
 *
 * <p><b>This is the boss body the id always named.</b> The id was live on main
 * since TC-M6-08-s-c as a stub ({@code extends Monster}, no boss body, follow
 * range left at 16.0); this card repoints the ONE registration to this class —
 * same id, same 1.75x3.5 box, same tracking 64 / interval 3, same egg colours —
 * and the boss base's follow range 40.0 ({@code EntityThaumcraftBoss:54}) is the
 * parity fix the row's note names.</p>
 *
 * <p>The phase machine: 250 health, 10 attack, fire-immune, armour +6, a 100-tick
 * materialisation, the ONLY headless trigger being a hit bigger than the whole
 * health bar (the hit is consumed), and the mid-fight ranged goal
 * {@code AILongRangeAttack(this, 3.0, 1.0, 5, 5, 24.0F)} at slot 2 that
 * {@code makeHeadless()} installs ({@code :185-187}) — never present at
 * construction. The beam spends 15+rand(5) charge per shot inside a 150-tick
 * charge window. Every figure is on {@link EldritchgolemAiLogic}.</p>
 *
 * <p>Seams (recorded, not faked): the headless FX pair ({@code :178-182}, {@code
 * :311-355} — spark/vent/lightning and the {@code jacobs} arc target pick) is the
 * renderer/FX lane TC-M7-03-s; the block-crush pass ({@code :148-166}) names
 * {@code BlockLoot} and a hardness window, and {@code BlockLoot} is unregistered,
 * so the pass ships as the pinned rule only.</p>
 */
public class EntityEldritchGolem extends EntityThaumcraftBoss
        implements RangedAttackMob, IEldritchMob {

    /** The stub's carried constants — {@code :54} and the registration/egg rows. */
    public static final float WIDTH = EldritchgolemAiLogic.WIDTH;
    public static final float HEIGHT = EldritchgolemAiLogic.HEIGHT;
    public static final int TRACKING_RANGE = EldritchgolemAiLogic.TRACKING_RANGE;
    public static final int UPDATE_INTERVAL = EldritchgolemAiLogic.UPDATE_INTERVAL;
    public static final int EGG_PRIMARY = EldritchgolemAiLogic.EGG_PRIMARY;
    public static final int EGG_SECONDARY = EldritchgolemAiLogic.EGG_SECONDARY;

    /** {@code :69} — the watched headless byte (vanilla index 12 is owned by 1.21.1). */
    private static final EntityDataAccessor<Byte> HEADLESS =
            SynchedEntityData.defineId(EntityEldritchGolem.class, EntityDataSerializers.BYTE);

    private int attackTimer;
    private int beamCharge;
    private boolean chargingBeam;

    public EntityEldritchGolem(final EntityType<? extends EntityEldritchGolem> type, final Level level) {
        super(type, level);
        this.setSize(WIDTH, HEIGHT);                                                    // :54
        this.setPersistenceRequired();                                                  // :110 (the stub's bv())
    }

    /** {@code :106-108} over the boss pair, plus the {@code :99} armour bonus. */
    public static AttributeSupplier.Builder createAttributes() {
        return EldritchgolemAiLogic.createAttributes()
                .add(Attributes.ARMOR, EldritchgolemAiLogic.ARMOUR_BONUS);              // :99
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HEADLESS, (byte) 0);                                             // :69
    }

    public boolean isHeadless() {
        return this.entityData.get(HEADLESS).intValue() != 0;                           // :71-73
    }

    public void setHeadless(final boolean headless) {
        this.entityData.set(HEADLESS, (byte) (headless ? 1 : 0));                       // :75-77
    }

    /** {@code :95} — the eye is taller without the head. */
    @Override
    public float getEyeHeight(final net.minecraft.world.entity.Pose pose) {
        return isHeadless() ? EldritchgolemAiLogic.EYE_HEIGHT_HEADLESS
                : EldritchgolemAiLogic.EYE_HEIGHT;
    }

    /** {@code :55} — fire immunity. */
    @Override
    public boolean isFireImmune() {
        return EldritchgolemAiLogic.FIRE_IMMUNE;
    }

    // ------------------------------------------------------------------ goals (:46-53)

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(EldritchgolemAiLogic.GOAL_SWIM, new FloatGoal(this));  // :46
        this.goalSelector.addGoal(EldritchgolemAiLogic.GOAL_MELEE, new AIGolemAttackOnCollide(
                this, EldritchgolemAiLogic.MELEE_SPEED,
                EldritchgolemAiLogic.MELEE_LONG_MEMORY));                               // :47
        this.goalSelector.addGoal(EldritchgolemAiLogic.GOAL_RESTRICTION,
                new MoveTowardsRestrictionGoal(this, EldritchgolemAiLogic.RESTRICTION_SPEED)); // :48
        this.goalSelector.addGoal(EldritchgolemAiLogic.GOAL_WANDER,
                new AIWander(this, EldritchgolemAiLogic.WANDER_SPEED));                 // :49
        this.goalSelector.addGoal(EldritchgolemAiLogic.GOAL_WATCH, new LookAtPlayerGoal(
                this, Player.class, EldritchgolemAiLogic.WATCH_PLAYER_RANGE));          // :50
        this.goalSelector.addGoal(EldritchgolemAiLogic.GOAL_LOOK_IDLE,
                new RandomLookAroundGoal(this));                                        // :51
        this.targetSelector.addGoal(EldritchgolemAiLogic.TARGET_HURT,
                new AIHurtByTarget(this, EldritchgolemAiLogic.HURT_IS_AGRESSORS));      // :52
        this.targetSelector.addGoal(EldritchgolemAiLogic.TARGET_PLAYER,
                new AINearestAttackableTarget<>(this, Player.class,
                        EldritchgolemAiLogic.TARGET_SORT_TYPE,
                        EldritchgolemAiLogic.TARGET_ALWAYS_CLOSE));                     // :53
        // :46-53 — NO ranged goal at construction. Slot 2 appears only from makeHeadless().
    }

    /** {@code :185-187} — the mid-fight goal the headless phase installs at slot 2. */
    public void makeHeadless() {
        setHeadless(true);                                                              // :176
        setSpawnTimer(EldritchgolemAiLogic.headlessSpawnTimer());                       // :177
        this.goalSelector.addGoal(EldritchgolemAiLogic.GOAL_RANGED_PHASE2,
                new AILongRangeAttack(this, EldritchgolemAiLogic.RANGED_MIN_DISTANCE,
                        EldritchgolemAiLogic.RANGED_SPEED, EldritchgolemAiLogic.RANGED_INTERVAL_MIN,
                        EldritchgolemAiLogic.RANGED_INTERVAL_MAX, EldritchgolemAiLogic.RANGED_RADIUS)); // :186
    }

    // ------------------------------------------------------------------ spawn (:124-127)

    @Override
    public SpawnGroupData finalizeSpawn(final ServerLevelAccessor level,
            final DifficultyInstance difficulty, final MobSpawnType reason,
            final SpawnGroupData groupData) {
        final SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        setSpawnTimer(EldritchgolemAiLogic.SPAWN_TIMER);                                // :126
        return data;
    }

    // ------------------------------------------------------------------ phase rule (:170-183)

    /**
     * {@code :170-183} — the ONLY headless trigger: the whole health bar in one hit,
     * and the hit is CONSUMED. Otherwise the family's damage leg runs (aggro + rage +
     * 35.0F clamp, {@code EntityThaumcraftBoss:222-255}).
     */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (EldritchgolemAiLogic.becomesHeadless(amount, getMaxHealth(), isHeadless())) {
            makeHeadless();                                                             // :176-177
            return false;                                                               // :182 (the hit is consumed)
        }
        return super.hurt(source, bossDamageLeg(source, amount));
    }

    // ------------------------------------------------------------------ beam (:213-236, :281-359)

    @Override
    public void aiStep() {
        if (isSpawningIn() && !this.level().isClientSide) {                             // :283-285
            heal(EldritchgolemAiLogic.SPAWN_HEAL);                                      // (before super, verbatim)
        }
        super.aiStep();
        if (this.attackTimer > 0) {
            this.attackTimer--;                                                         // :191-195
        }
        if (this.level().isClientSide) {
            return;
        }
        if (EldritchgolemAiLogic.beamStarts(isHeadless(), this.beamCharge)) {           // :301
            this.chargingBeam = true;
        }
        if (this.chargingBeam) {                                                        // :302-306
            this.beamCharge = EldritchgolemAiLogic.beamTick(this.beamCharge);
            if (EldritchgolemAiLogic.beamEnds(this.beamCharge)) {
                this.chargingBeam = false;
            }
        }
        meleeLeg();                                                                     // :189-211
    }

    /**
     * {@code :189-211} — the golem's collision hit: the 10-tick attack gate, damage
     * {@code attackDamage * 0.75}, the target's 0.2 lift, and headless only the
     * (±1.5, 0.1, ±1.5) throw. The reach is the platform's melee reach of the goal
     * that swings (1.7.10 used the same collision test); the figures are pinned on
     * the rule class.
     */
    private void meleeLeg() {
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || this.attackTimer > 0) {              // :191-195
            return;
        }
        if (target.distanceToSqr(this) > (double) (this.getBbWidth() + 2.5F)) {         // reach seam
            return;
        }
        this.attackTimer = EldritchgolemAiLogic.ATTACK_TIMER;                           // :194
        target.hurt(target.damageSources().mobAttack(this),
                EldritchgolemAiLogic.meleeDamage(getAttackDamage()));                   // :197
        target.setDeltaMovement(target.getDeltaMovement().add(0.0, EldritchgolemAiLogic.MELEE_LIFT, 0.0)); // :198
        if (isHeadless()) {                                                             // :200-205
            final float yaw = this.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
            target.setDeltaMovement(new Vec3(
                    -net.minecraft.util.Mth.sin(yaw) * EldritchgolemAiLogic.HEADLESS_KNOCKBACK,
                    EldritchgolemAiLogic.HEADLESS_LIFT,
                    net.minecraft.util.Mth.cos(yaw) * EldritchgolemAiLogic.HEADLESS_KNOCKBACK));
        }
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);                      // :207-209 (status 4)
    }

    /**
     * {@code :213-236} — the beam shot. The gate is the charge state, the spend is
     * {@code 15 + rand(5)}, and the orb is the NON-red pair (lifetime 160 / damage
     * x1.5) — the flag the cleric lane pinned per caller.
     */
    @Override
    public void performRangedAttack(final LivingEntity target, final float distanceFactor) {
        if (!EldritchgolemAiLogic.beamCanFire(isHeadless(), this.chargingBeam, this.beamCharge)) {
            return;                                                                     // :215-219
        }
        this.beamCharge -= EldritchgolemAiLogic.beamSpend(this.random.nextInt(
                EldritchgolemAiLogic.BEAM_SPEND_RANDOM));                               // :221
        final EntityGolemOrb orb = new EntityGolemOrb(CatalogGolems.GOLEM_ORB.get(),
                this.level(), this, target, EldritchgolemAiLogic.ORB_IS_RED);           // :225
        orb.setPos(this.getX(), this.getY() + getEyeHeight(), this.getZ());
        this.level().addFreshEntity(orb);
        this.playSound(ModSounds.EGATTACK.get(), 2.0F,
                1.0F / (this.random.nextFloat() * 0.4F + 0.8F));                        // :232-234
    }

    public int getBeamCharge() {
        return this.beamCharge;
    }

    public boolean isChargingBeam() {
        return this.chargingBeam;
    }

    public int getAttackTimer() {
        return this.attackTimer;
    }

    // ------------------------------------------------------------------ sounds (:111-121)

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_AMBIENT;                                          // :111-113
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;                                             // :115-117
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;                                            // :119-121
    }

    @Override
    protected void playStepSound(final net.minecraft.core.BlockPos pos,
            final net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);                        // :118 (walk)
    }

    // ------------------------------------------------------------------ save (:81-93)

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(EldritchgolemAiLogic.NBT_HEADLESS, isHeadless());                // :83
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean(EldritchgolemAiLogic.NBT_HEADLESS)) {                        // :86
            makeHeadless();                                                             // :87 (a load re-runs it)
        }
    }

    /** The crush rule (the block itself is unregistered — {@code :148-166}). */
    public static boolean crushesBlock(final float hardness) {
        return EldritchgolemAiLogic.crushesBlock(hardness);
    }

    /** The goal-slot census the probes assert: 6 goal rows, 2 target rows, no ranged goal. */
    public static int SHIPPED_GOAL_COUNT = 6;
    public static int SHIPPED_TARGET_COUNT = 2;
}
