package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
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
import net.minecraft.world.phys.Vec3;

import com.jonnyutah12.thaumcraftclassic.api.entities.IEldritchMob;
import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AIAttackOnCollide;
import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AIHurtByTarget;
import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AINearestAttackableTarget;
import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AIWander;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AILongRangeAttack;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchGuardian;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityEldritchOrb;
import com.jonnyutah12.thaumcraftclassic.player.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchCrab;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;

/**
 * {@code thaumcraft:eldritchwarden} — original
 * {@code thaumcraft.common.entities.monster.boss.EntityEldritchWarden} (418 lines,
 * content-mapping {@code Thaumcraft:EldritchWarden}, mod_index 26, line 324),
 * {@code extends EntityThaumcraftBoss implements IRangedAttackMob, IEldritchMob}.
 * Registration {@code ConfigEntities.java:109} — {@code (64, 3, true)}.
 *
 * <p>The Warden: 200 health, 10 attack, 0.33 speed, armour +4, a 150-tick
 * materialisation that starts it at +66 % health, and the death fake-out: a lethal
 * hit is CONSUMED and arms the 150-counter field frenzy, during which the Warden
 * is invulnerable, teleports home once, and rings the portal blocks. Eight of ten
 * ranged shots are the eldritch orb ({@code :342}, c4b's class); the tenth is the
 * screech. Every figure is on {@link EldritchWardenAiLogic}.</p>
 *
 * <p>Goal reconciliation: the original installs NO melee-free set — its slot list
 * is the contract (swim 0, ranged 2, melee 3, restriction 5, wander 7 at 1.0,
 * watch 8, idle 8; hurt 1 FALSE, player 2, cultist 3). 1.21.1's {@code Monster}
 * adds no goal of its own, so no inherited goal needs stripping; the record the
 * row carries is that the warden's restriction goal sits at slot 5, not 6, and
 * that {@code :69}'s door opening is the navigator flag, not a goal.</p>
 *
 * <p>Seams (recorded, not faked): the frenzy ring's {@code blockAiry} meta 11
 * placements and the arc/sparkle packets ({@code :196-205}) are unregistered
 * blocks and the FX lane; the screech's {@code PacketFXSonic} and its two
 * 400-tick amplifier-0 potions ({@code :364-370}, the SRG pair resolves to the
 * unregistered TC effects) ship as the pinned rule only; the client arm-lift
 * statuses 15/16/17 belong to the renderer lane.</p>
 */
public class EntityEldritchWarden extends EntityThaumcraftBoss
        implements RangedAttackMob, IEldritchMob {

    /** The registration figures — {@code ConfigEntities.java:109} (64/3). */
    public static final float WIDTH = EldritchWardenAiLogic.WIDTH;
    public static final float HEIGHT = EldritchWardenAiLogic.HEIGHT;
    public static final int TRACKING_RANGE = EldritchWardenAiLogic.TRACKING_RANGE;
    public static final int UPDATE_INTERVAL = EldritchWardenAiLogic.UPDATE_INTERVAL;

    /** {@code :110-113} — the watched title byte. */
    private static final EntityDataAccessor<Byte> TITLE =
            SynchedEntityData.defineId(EntityEldritchWarden.class, EntityDataSerializers.BYTE);

    /** {@code :44-60} / {@code :51} — the frenzy pair. */
    private boolean frenzy;
    private int frenzyCounter;
    /** {@code :50} — the blast alternation. */
    private boolean lastBlast = EldritchWardenAiLogic.LAST_BLAST_START;

    public EntityEldritchWarden(final EntityType<? extends EntityEldritchWarden> type, final Level level) {
        super(type, level);
        this.setSize(EldritchWardenAiLogic.WIDTH, EldritchWardenAiLogic.HEIGHT);         // :80
    }

    /** {@code :104-106} over the boss pair, plus the {@code :127} armour bonus. */
    public static AttributeSupplier.Builder createAttributes() {
        return EldritchWardenAiLogic.createAttributes()
                .add(Attributes.ARMOR, EldritchWardenAiLogic.ARMOUR_BONUS);              // :127
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TITLE, (byte) 0);
    }

    public int getTitle() {
        return this.entityData.get(TITLE).intValue();
    }

    public void setTitle(final int title) {
        this.entityData.set(TITLE, (byte) title);
    }

    @Override
    public float getEyeHeight(final Pose pose) {
        return EldritchWardenAiLogic.EYE_HEIGHT;                                        // :336
    }

    /** {@code :131-143} — the goals are inert for the whole frenzy, on top of the spawn-in gate. */
    @Override
    public boolean isSpawningIn() {
        return super.isSpawningIn() || this.frenzyCounter > 0;
    }

    public boolean isFrenzied() {
        return this.frenzy;
    }

    public int getFrenzyCounter() {
        return this.frenzyCounter;
    }

    // ------------------------------------------------------------------ goals (:70-79)

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(EldritchWardenAiLogic.GOAL_SWIM, new FloatGoal(this)); // :70
        this.goalSelector.addGoal(EldritchWardenAiLogic.GOAL_RANGED, new AILongRangeAttack(
                this, EldritchWardenAiLogic.RANGED_MIN_DISTANCE, EldritchWardenAiLogic.RANGED_SPEED,
                EldritchWardenAiLogic.RANGED_INTERVAL_MIN, EldritchWardenAiLogic.RANGED_INTERVAL_MAX,
                EldritchWardenAiLogic.RANGED_RADIUS));                                  // :71
        this.goalSelector.addGoal(EldritchWardenAiLogic.GOAL_MELEE, new AIAttackOnCollide(
                this, LivingEntity.class, EldritchWardenAiLogic.MELEE_SPEED,
                EldritchWardenAiLogic.MELEE_LONG_MEMORY));                              // :72
        this.goalSelector.addGoal(EldritchWardenAiLogic.GOAL_RESTRICTION,
                new MoveTowardsRestrictionGoal(this, EldritchWardenAiLogic.RESTRICTION_SPEED)); // :73 (slot 5)
        this.goalSelector.addGoal(EldritchWardenAiLogic.GOAL_WANDER,
                new AIWander(this, EldritchWardenAiLogic.WANDER_SPEED));                // :74 (1.0)
        this.goalSelector.addGoal(EldritchWardenAiLogic.GOAL_WATCH, new LookAtPlayerGoal(
                this, Player.class, EldritchWardenAiLogic.WATCH_PLAYER_RANGE));         // :75
        this.goalSelector.addGoal(EldritchWardenAiLogic.GOAL_LOOK_IDLE,
                new RandomLookAroundGoal(this));                                        // :76
        this.targetSelector.addGoal(EldritchWardenAiLogic.TARGET_HURT,
                new AIHurtByTarget(this, EldritchWardenAiLogic.HURT_IS_AGRESSORS));     // :77 (false)
        this.targetSelector.addGoal(EldritchWardenAiLogic.TARGET_PLAYER,
                new AINearestAttackableTarget<>(this, Player.class,
                        EldritchWardenAiLogic.TARGET_SORT_TYPE,
                        EldritchWardenAiLogic.TARGET_ALWAYS_CLOSE));                    // :78
        this.targetSelector.addGoal(EldritchWardenAiLogic.TARGET_CULTIST,
                new AINearestAttackableTarget<>(this, EntityCultist.class,
                        EldritchWardenAiLogic.TARGET_SORT_TYPE,
                        EldritchWardenAiLogic.TARGET_ALWAYS_CLOSE));                    // :79
    }

    // ------------------------------------------------------------------ spawn (:328-334)

    @Override
    public SpawnGroupData finalizeSpawn(final ServerLevelAccessor level,
            final DifficultyInstance difficulty, final MobSpawnType reason,
            final SpawnGroupData groupData) {
        final SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        setSpawnTimer(EldritchWardenAiLogic.SPAWN_TIMER);                               // :330
        setTitle(random.nextInt(EldritchWardenAiLogic.TITLES.length));                  // :331
        generateName();
        heal((float) (getMaxHealth() * EldritchWardenAiLogic.SPAWN_HEAL_FRACTION));      // :333
        return data;
    }

    @Override
    public void generateName() {
        this.setName(net.minecraft.network.chat.Component.literal(
                EldritchWardenAiLogic.nameFor(getTitle())));                            // :113
    }

    // ------------------------------------------------------------------ frenzy legs (:184-326)

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            if (this.frenzyCounter > 0) {
                if (EldritchWardenAiLogic.frenziedTeleports(this.frenzyCounter)) {      // :191
                    teleportHome();                                                     // :192
                }
                performFieldFrenzy();                                                   // :193-205
            }
            regenLeg();                                                                 // :135-141
        }
        super.aiStep();
    }

    /** {@code :135-141} — regen every 25 ticks out of hurt-resistance, capped at 66 %. */
    private void regenLeg() {
        if (EldritchWardenAiLogic.regens(this.tickCount, getHurtTime() /*hurtResistantTime seam*/,
                getHealth(), getMaxHealth())) {
            heal(1.0F);
        }
    }

    /**
     * {@code :208-245} — the ring pass. The counter decrements on every call; the
     * radius/depth/step figures ship pinned on the rule class, the block placements
     * and packets are the gated FX seam, and the {@code zap} sound fires per leg.
     */
    public void performFieldFrenzy() {
        if (EldritchWardenAiLogic.frenzyRingFires(this.frenzyCounter)) {                // :194-195
            this.playSound(ModSounds.ZAP.get(), 1.0F, 1.0F);                            // :204
        }
        if (this.frenzyCounter > 0) {
            this.frenzyCounter--;                                                       // :206
        }
    }

    /** {@code :247-306} — the home teleport: up to 20 tries at ±8 for a walkable cell. */
    public void teleportHome() {
        final BlockPos center = hasRestriction() ? getRestrictCenter() : blockPosition();
        final Vec3 old = position();
        for (int try_ = 0; try_ < EldritchWardenAiLogic.TELEPORT_TRIES; try_++) {       // :252
            final int i = center.getX() + this.random.nextInt(EldritchWardenAiLogic.TELEPORT_RANGE * 2)
                    - EldritchWardenAiLogic.TELEPORT_RANGE;
            final int k = center.getZ() + this.random.nextInt(EldritchWardenAiLogic.TELEPORT_RANGE * 2)
                    - EldritchWardenAiLogic.TELEPORT_RANGE;
            final int j = center.getY() + this.random.nextInt(6) - 3;
            final BlockPos cell = new BlockPos(i, j, k);
            if (this.level().getBlockState(cell.below()).isSolid()
                    && this.level().getBlockState(cell).isAir()
                    && this.level().getBlockState(cell.above()).isAir()) {              // :255-259
                setPos(i + 0.5, j + EldritchWardenAiLogic.TELEPORT_Y_OFFSET, k + 0.5);  // :261
                return;
            }
        }
        setPos(old.x, old.y, old.z);                                                    // :270 (restore)
    }

    // ------------------------------------------------------------------ immunity (:308-326)

    @Override
    public boolean isInvulnerableTo(final DamageSource source) {
        return super.isInvulnerableTo(source)                                           // :308-311
                || EldritchWardenAiLogic.invulnerableForFrenzy(this.frenzy, false);
    }

    /**
     * {@code :313-326} — the void/anvil-cactus pair is ignored, the base's leg runs,
     * and a lethal hit that is not already a frenzy is CONSUMED and arms the frenzy.
     */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (isInvulnerableTo(source)) {                                                 // :314
            return false;
        }
        if (EldritchWardenAiLogic.ignoresDamageSource(source.getDirectType().location().getPath())) {
            return false;                                                               // :315-317
        }
        final float applied = bossDamageLeg(source, amount);
        if (EldritchWardenAiLogic.frenzyStartsOnHit(this.frenzy, getHealth() - applied)) {
            this.frenzy = true;                                                         // :321
            this.frenzyCounter = EldritchWardenAiLogic.FRENZY_COUNTER;                  // :322
            return false;                                                               // :323 (consumed)
        }
        return super.hurt(source, applied);
    }

    /** {@code :415} — the kill award is 500 XP. */
    @Override
    public int getExperiencePoints(final Player player) {
        return EldritchWardenAiLogic.EXPERIENCE_POINTS;
    }

    // ------------------------------------------------------------------ ranged (:340-380)

    /**
     * {@code :340-380} — 8 of 10 shots are the eldritch orb from the alternating arm
     * (the {@code lastBlast} toggle picks the side, the statuses are the renderer's
     * arm lift); the tenth is the screech: the warp and the {@code egscreech} sound,
     * with the sonic FX and the two potions as the recorded seam.
     */
    @Override
    public void performRangedAttack(final LivingEntity target, final float distanceFactor) {
        if (EldritchWardenAiLogic.firesOrb(this.random.nextDouble())) {                 // :341
            this.lastBlast = !this.lastBlast;                                           // :343-346
            final double yaw = Math.toRadians(getYRot() + (this.lastBlast ? 90.0 : 180.0));
            final EntityEldritchOrb orb = new EntityEldritchOrb(
                    CatalogEldritchCrab.ELDRITCH_ORB.get(), this.level(), this);        // :342
            orb.setPos(this.getX() - net.minecraft.util.Mth.cos((float) yaw) * EldritchWardenAiLogic.ORB_OFFSET,
                    this.getY() + EldritchWardenAiLogic.ORB_Y_OFFSET,
                    this.getZ() + net.minecraft.util.Mth.sin((float) yaw) * EldritchWardenAiLogic.ORB_OFFSET);
            this.level().addFreshEntity(orb);
            this.playSound(ModSounds.EGATTACK.get(), EldritchWardenAiLogic.ATTACK_SOUND_VOLUME,
                    1.0F / (this.random.nextFloat() * 0.4F + 0.8F));                    // :360
            return;
        }
        screech(target);                                                                // :362-378
    }

    private void screech(final LivingEntity target) {
        if (target instanceof ServerPlayer player) {                                    // :366-372
            ResearchManager.applyWarp(player,
                    EldritchWardenAiLogic.warpFor(this.random.nextInt(
                            EldritchWardenAiLogic.WARP_RANDOM)),
                    false, SyncWarpPayload.WARP_STICKY);                                // :368
        }
        target.knockback(0.6, net.minecraft.util.Mth.sin(getYRot() * net.minecraft.util.Mth.DEG_TO_RAD),
                -net.minecraft.util.Mth.cos(getYRot() * net.minecraft.util.Mth.DEG_TO_RAD)); // :365
        this.playSound(ModSounds.EGSCREECH.get(), EldritchWardenAiLogic.SCREECH_SOUND_VOLUME, 1.0F); // :376
    }

    public boolean isLastBlast() {
        return this.lastBlast;
    }

    /** {@code :403-405} — the guardian is never the Warden's target. */
    public static boolean wardenIsTargetableEntity(final LivingEntity target) {
        return !(target instanceof EntityEldritchGuardian);
    }

    // ------------------------------------------------------------------ sounds (:407-411)

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.EGIDLE.get();                                                  // :407
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return ModSounds.EGATTACK.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.EGDEATH.get();                                                 // :411
    }

    // ------------------------------------------------------------------ save (:116-125)

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(EldritchWardenAiLogic.NBT_TITLE, (byte) getTitle());
        tag.putBoolean("Frenzy", this.frenzy);
        tag.putInt("FrenzyCounter", this.frenzyCounter);
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTitle(tag.getByte(EldritchWardenAiLogic.NBT_TITLE));
        this.frenzy = tag.getBoolean("Frenzy");
        this.frenzyCounter = tag.getInt("FrenzyCounter");
    }

    /** The goal-slot census the probes assert. */
    public static final int SHIPPED_GOAL_COUNT = 7;
    public static final int SHIPPED_TARGET_COUNT = 3;
}
