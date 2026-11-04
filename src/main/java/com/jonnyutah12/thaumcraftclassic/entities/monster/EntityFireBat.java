package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The firebat — {@code thaumcraft:firebat} (content-mapping.csv
 * {@code Thaumcraft:Firebat}, mod_index 22).
 *
 * <p>Original: {@code thaumcraft.common.entities.monster.EntityFireBat}, an
 * {@code EntityMob} that flies by hand. It installs <b>no goal</b> and uses
 * <b>no {@code PathNavigation}</b>; the hanging / flight / steering body is
 * written directly in {@code func_70626_be()} ({@code onEntityUpdate},
 * {@code :223-297}). It sits on 1.7.10's new-AI path
 * ({@code func_70814_o()} = {@code isAIEnabled} returns <b>true</b> at
 * {@code :401-403}) and still registers no goal, so its target comes from the
 * provider it supplies itself ({@code func_70782_k()}, {@code :371-374}: the
 * closest player within 12.0, {@code null} while summoned) and its melee from
 * its own body ({@code func_70785_a(Entity,float)}, {@code :329-369}) with the
 * rate limit inside that body. 1.21.1 reaches {@code doHurtTarget} only from a
 * goal, so this class keeps the original's <b>empty</b> goal selector
 * ({@link #registerGoals()}, asserted by the game probe) and drives that
 * provider and that body from {@link #customServerAiStep()}. Every figure lives
 * in {@link FirebatAiLogic} and is pinned world-free by
 * {@code FirebatAiLogicTest}; this class only supplies the world.</p>
 *
 * <h2>Shape mapping</h2>
 * <ul>
 *   <li>{@code func_70636_d} ({@code onLivingUpdate}, :154-160) — the 1.0
 *       drown damage while wet ({@code DamageSource.field_76369_e} = drown),
 *       applied before super: top of {@link #aiStep()}.</li>
 *   <li>{@code func_70071_h_} (:188-193) — the hanging motion-zeroing and
 *       {@code floor(y)+1-height} snap / the 0.6 vertical damping, between the
 *       wet check and {@code super.aiStep()}.</li>
 *   <li>The client half of {@code func_70071_h_} (:164-220) — the smoke +
 *       flame particles and the explosive FX — is the extracted
 *       {@link #spawnClientParticles()}, so a game-test server (never
 *       client-side, {@code world.field_72995_K}) can assert both halves
 *       separately.</li>
 *   <li>{@code func_70626_be} (:223-297) — the whole hanging/flight/steering
 *       body: {@link #customServerAiStep()}.</li>
 *   <li>{@code func_70785_a} (:329-369) — {@link #firebatMelee(Entity, float)},
 *       called every tick a target exists.</li>
 *   <li>{@code field_70178_ae = true} (:32, {@code isImmuneToFire}) —
 *       {@code .fireImmune()} on the entity type in {@code CatalogEntities}.</li>
 *   <li>{@code func_70104_M()} (:69-71) and {@code func_70041_e_()}
 *       (:303-305) — both resolve to {@code isPushable()} in this port (the
 *       resolution {@code EntityWisp:303} already uses) and both return false
 *       here, so {@link #isPushable()} is false.</li>
 *   <li>{@code func_70650_aV()} (:150-152) — the 1.7.10 {@code isBoss()} flag,
 *       false, which is 1.21.1's default: nothing to wire.</li>
 *   <li>{@code func_145773_az()} (:313-315) — the hook this repo resolves to
 *       {@code isNoAi()}, true in the original. Deliberately <b>not</b> wired:
 *       1.21.1's NoAi flag flips {@code isEffectiveAi()} and gates
 *       {@code travel()}, which would switch off the hand-written motion this
 *       card is proving. Its observable effect in the original — no goal
 *       ticking, no pathfinding — is reproduced by the empty goal selector;
 *       the gap is recorded in {@code docs/FEATURE_MATRIX.md}.</li>
 *   <li>{@code func_70064_a(double,boolean)} (:310-311) is empty, so fall
 *       damage is off: {@link #checkFallDamage} is a no-op.
 *       {@code func_70069_a(float)} (:307-308) is empty, so
 *       {@link #playStepSound} is silent.</li>
 *   <li>1.7.10's {@code canBlockSeeTheSky} (:227, :277) has no 1.21.1
 *       equivalent ({@code canSeeSky} was removed); the shipped seam is
 *       {@code LevelReader.canSeeSkyFromBelowWater}, which reads the raw sky
 *       light at the position.</li>
 *   <li>{@code field_70160_al} (:362, 1.7.10 {@code isAirBorne}) does not
 *       exist in 1.21.1: the motion restore survives, the flag is
 *       documented away.</li>
 * </ul>
 */
public class EntityFireBat extends Monster {

    /** The original's synched-data index 16 (:35-38). 1.21.1 assigns synched
     * ids itself, so what is pinned is the byte and its five bits, not the
     * index. */
    private static final EntityDataAccessor<Byte> DATA_FLAGS =
            SynchedEntityData.defineId(EntityFireBat.class, EntityDataSerializers.BYTE);

    /** {@code currentFlightTarget} — a {@code ChunkCoordinates} in the
     * original (:24), i.e. integer block coordinates. */
    private BlockPos currentFlightTarget;

    /** 1.7.10's {@code EntityLivingBase.attackTime} ({@code field_70724_aR}):
     * set to 20 on a hit (:346), decremented once per living update. */
    private int attackTime = FirebatAiLogic.ATTACK_TIME_INITIAL;

    /** {@code public EntityPlayer owner} (:25) and {@code public int damBonus}
     * (:26) — written by whatever summons the bat. */
    public Player owner;
    public int damBonus;

    public EntityFireBat(final EntityType<? extends EntityFireBat> type, final Level level) {
        super(type, level);
        // ctor (:30-32): setIsBatHanging(true) — the bat spawns hanging.
        setFlag(FirebatAiLogic.FLAG_HANGING, true);
    }

    /** {@code func_110147_ax} (:73-77): health 5.0 (15.0 while devil) and
     * attack 1.0 ({@code (devil ? 3 : 2) + damBonus} while summoned). */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, FirebatAiLogic.HEALTH_NORMAL)
                .add(Attributes.ATTACK_DAMAGE, FirebatAiLogic.attackDamageFor(false, false, 0));
    }

    /** The category and hitbox of the entity type, read from the original:
     * {@code MobCategory.MONSTER} (the {@code EntityMob} base) and
     * {@code func_70105_a(0.5F, 0.9F)} ({@code :30}). */
    public static EntityType<EntityFireBat> buildType(final MobCategory category) {
        return EntityType.Builder.of(EntityFireBat::new, category)
                .sized(FirebatAiLogic.WIDTH, FirebatAiLogic.HEIGHT)
                .fireImmune()                       // field_70178_ae = true (:32)
                .clientTrackingRange(8)
                .build("firebat");
    }

    /** The original installs no goal and no navigation class (see the class
     * javadoc). Left empty on purpose. */
    @Override
    protected void registerGoals() {
        // deliberately empty — no goal and no navigation, like the original
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS, (byte) 0);
    }

    // ------------------------------------------------------------------ flags

    public boolean getFlag(final int bit) {
        return FirebatAiLogic.getBit(getEntityData().get(DATA_FLAGS), bit);
    }

    public void setFlag(final int bit, final boolean value) {
        final int flags = getEntityData().get(DATA_FLAGS);
        getEntityData().set(DATA_FLAGS, (byte) (value
                ? FirebatAiLogic.setBit(flags, bit)
                : FirebatAiLogic.clearBit(flags, bit)));
    }

    public boolean getIsBatHanging() {
        return getFlag(FirebatAiLogic.FLAG_HANGING);
    }

    public void setIsBatHanging(final boolean hanging) {
        setFlag(FirebatAiLogic.FLAG_HANGING, hanging);
    }

    public boolean getIsSummoned() {
        return getFlag(FirebatAiLogic.FLAG_SUMMONED);
    }

    /** {@code setIsSummoned} (:96-105) re-applies the attack attribute. */
    public void setIsSummoned(final boolean summoned) {
        setFlag(FirebatAiLogic.FLAG_SUMMONED, summoned);
        applyAttackDamage();
    }

    public boolean getIsExplosive() {
        return getFlag(FirebatAiLogic.FLAG_EXPLOSIVE);
    }

    public void setIsExplosive(final boolean explosive) {
        setFlag(FirebatAiLogic.FLAG_EXPLOSIVE, explosive);
    }

    public boolean getIsDevil() {
        return getFlag(FirebatAiLogic.FLAG_DEVIL);
    }

    /** {@code setIsDevil} (:124-135): the attack attribute is re-applied only
     * when the flag is set, and only for a summoned bat. */
    public void setIsDevil(final boolean devil) {
        setFlag(FirebatAiLogic.FLAG_DEVIL, devil);
        if (devil) {
            applyAttackDamage();
        }
    }

    public boolean getIsVampire() {
        return getFlag(FirebatAiLogic.FLAG_VAMPIRE);
    }

    public void setIsVampire(final boolean vampire) {
        setFlag(FirebatAiLogic.FLAG_VAMPIRE, vampire);
    }

    public int getDamBonus() {
        return damBonus;
    }

    private void applyAttackDamage() {
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
                    FirebatAiLogic.attackDamageFor(getIsSummoned(), getIsDevil(), damBonus));
        }
    }

    /** The devil flag also carries health (:75). The original applies it in
     * {@code func_110147_ax}, which runs before the summoner's flags are
     * written; applying the same figure on the tick after the flag lands is the
     * faithful result for a summoned bat. */
    private void applyDevilHealth() {
        final float wanted = FirebatAiLogic.healthFor(getIsDevil());
        if (this.getAttributeBaseValue(Attributes.MAX_HEALTH) != wanted) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(wanted);
            this.setHealth(Math.min(this.getHealth(), wanted));
        }
    }

    // ------------------------------------------------------------------- tick

    @Override
    public void aiStep() {
        // func_70636_d (:154-160): the wet damage lands before super.
        if (!level().isClientSide && this.isInWaterRainOrBubble()) {
            this.hurt(this.damageSources().drown(), FirebatAiLogic.WET_DAMAGE);
        }

        // EntityLivingBase.onLivingUpdate decrements attackTime every tick.
        if (this.attackTime > 0) {
            this.attackTime--;
        }

        // func_70071_h_ (:188-193).
        if (this.getIsBatHanging()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setPosRaw(this.getX(),
                    FirebatAiLogic.hangingY(this.getY(), this.getBbHeight()),
                    this.getZ());
        } else {
            final Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, FirebatAiLogic.flyingMotionY(motion.y), motion.z);
        }

        // The client half of func_70071_h_ (:164-220). A game-test server is
        // never client-side (world.field_72995_K), so the body is extracted
        // and both halves are asserted separately by FirebatGameTest.
        if (level().isClientSide) {
            spawnClientParticles();
        }

        super.aiStep();
    }

    /** {@code :195-220} — the "smoke" and "flame" vanilla particles at the
     * body centre with {@code (nextFloat - nextFloat) * 0.2} jitter and zero
     * velocity, suppressed while vampire. {@code world.rand}
     * ({@code field_73012_v}) is the level random source, not the entity's. */
    public void spawnClientParticles() {
        final RandomSource levelRandom = level().getRandom();
        if (!getIsVampire()) {
            for (int i = 0; i < 2; i++) {
                level().addParticle(i == 0 ? ParticleTypes.SMOKE : ParticleTypes.FLAME,
                        this.getX() + (levelRandom.nextFloat() - levelRandom.nextFloat()) * 0.2,
                        this.getY() + this.getBbHeight() / 2.0
                                + (levelRandom.nextFloat() - levelRandom.nextFloat()) * 0.2,
                        this.getZ() + (levelRandom.nextFloat() - levelRandom.nextFloat()) * 0.2,
                        0.0, 0.0, 0.0);
            }
        }
        // :164-186 is the client-side explosive FX: Thaumcraft's
        // drawGenericParticles(..., type 151, quantity 1, delay 7+nextInt(5),
        // size 1.0+nextFloat()*0.5). The generic-particle proxy has no 1.21.1
        // port surface yet (TC-M7-01-s / TC-M7-03-s), so the parameters are
        // pinned in ExplosiveFx and this site stays the single place the call
        // belongs; no substitute particle is invented here.
        if (getIsExplosive()) {
            ExplosiveFx.roll(levelRandom);
        }
    }

    /** The pinned parameters of the original's explosive-FX particle call
     * (:164-186): white, alpha 0.8, {@code delay = 7 + nextInt(5)},
     * {@code size = 1.0 + nextFloat() * 0.5}, jitter
     * {@code (nextFloat - nextFloat) * 0.1} per axis. */
    public record ExplosiveFx(double jitterX, double jitterY, double jitterZ, int delay, float size) {
        public static final int DELAY_BASE = 7;
        public static final int DELAY_ROLL = 5;
        public static final float SIZE_BASE = 1.0F;
        public static final float SIZE_SPAN = 0.5F;
        public static final double JITTER_SPAN = 0.1;

        public static ExplosiveFx roll(final RandomSource random) {
            return new ExplosiveFx(
                    (random.nextFloat() - random.nextFloat()) * JITTER_SPAN,
                    (random.nextFloat() - random.nextFloat()) * JITTER_SPAN,
                    (random.nextFloat() - random.nextFloat()) * JITTER_SPAN,
                    DELAY_BASE + random.nextInt(DELAY_ROLL),
                    SIZE_BASE + random.nextFloat() * SIZE_SPAN);
        }
    }

    // --------------------------------------------------------------- the body

    @Override
    protected void customServerAiStep() {
        applyDevilHealth();

        // func_70782_k() (:371-374): the original's own target provider — the
        // closest player within 12.0, and null while summoned. Re-derived
        // every tick, which is what a goal-less entity does here.
        if (getIsSummoned()) {
            this.setTarget(null);
        } else {
            this.setTarget(level().getNearestPlayer(this, FirebatAiLogic.TARGET_DISTANCE));
        }
        final LivingEntity acquired = this.getTarget();
        if (acquired != null && FirebatAiLogic.clearsTarget(acquired instanceof Player,
                acquired instanceof Player p && p.isCreative())) {
            // :293-295 — a creative player is not a target.
            this.setTarget(null);
        }

        if (getIsBatHanging()) {
            // :225-239 — hanging: a missing support block, or a player within
            // 4.0, un-hangs and fires level event 1015 at the feet.
            final BlockPos feet = BlockPos.containing(this.getX(), this.getY(), this.getZ());
            final BlockPos aboveFeet = feet.above();
            if (!level().canSeeSkyFromBelowWater(aboveFeet)) {
                setIsBatHanging(false);
                level().levelEvent(FirebatAiLogic.HANGING_SUPPORT_EVENT, feet, 0);
            } else {
                final int headYaw = FirebatAiLogic.idleHeadYaw(
                        random.nextInt(FirebatAiLogic.IDLE_HEAD_YAW_ROLL),
                        random.nextInt(FirebatAiLogic.HEAD_YAW_SPAN));
                if (headYaw >= 0) {
                    this.setYHeadRot(headYaw);
                }
                if (level().getNearestPlayer(this, FirebatAiLogic.UNHANG_PLAYER_DISTANCE) != null) {
                    setIsBatHanging(false);
                    level().levelEvent(FirebatAiLogic.HANGING_SUPPORT_EVENT, feet, 0);
                }
            }
        } else if (this.getTarget() == null) {
            // :241-279 — flying with no target: wander to a random waypoint.
            if (getIsSummoned()) {
                this.hurt(this.damageSources().generic(), FirebatAiLogic.SUMMONED_NO_TARGET_DAMAGE);
            }

            if (currentFlightTarget != null) {
                final BlockState atTarget = level().getBlockState(currentFlightTarget);
                if (FirebatAiLogic.flightTargetInvalid(atTarget.isAir(), currentFlightTarget.getY())) {
                    currentFlightTarget = null;
                }
            }

            final BlockPos here = BlockPos.containing(this.getX(), this.getY(), this.getZ());
            final double distanceToTarget = currentFlightTarget == null
                    ? 0.0 : Math.sqrt(currentFlightTarget.distSqr(here));
            if (FirebatAiLogic.repicksFlightTarget(currentFlightTarget == null,
                    random.nextInt(FirebatAiLogic.FLIGHT_TARGET_REPICK_ROLL), distanceToTarget)) {
                currentFlightTarget = new BlockPos(
                        FirebatAiLogic.waypointAxis(here.getX(),
                                random.nextInt(FirebatAiLogic.WAYPOINT_AXIS_ROLL),
                                random.nextInt(FirebatAiLogic.WAYPOINT_AXIS_ROLL)),
                        FirebatAiLogic.waypointY(here.getY(),
                                random.nextInt(FirebatAiLogic.WAYPOINT_Y_ROLL)),
                        FirebatAiLogic.waypointAxis(here.getZ(),
                                random.nextInt(FirebatAiLogic.WAYPOINT_AXIS_ROLL),
                                random.nextInt(FirebatAiLogic.WAYPOINT_AXIS_ROLL)));
            }

            final double dx = currentFlightTarget.getX() + FirebatAiLogic.WAYPOINT_XZ_CENTER - this.getX();
            final double dy = currentFlightTarget.getY() + FirebatAiLogic.WAYPOINT_Y_CENTER - this.getY();
            final double dz = currentFlightTarget.getZ() + FirebatAiLogic.WAYPOINT_XZ_CENTER - this.getZ();
            steerTowards(dx, dy, dz);

            if (FirebatAiLogic.hangsAgain(random.nextInt(FirebatAiLogic.RE_HANG_ROLL),
                    level().canSeeSkyFromBelowWater(aboveFlightScan()))) {
                setIsBatHanging(true);
            }
        } else {
            // :280-292 — flying with a target: steer at the target's eyes.
            final LivingEntity target = this.getTarget();
            steerTowards(target.getX() - this.getX(),
                    target.getY() + target.getEyeHeight() * FirebatAiLogic.TARGET_EYE_HEIGHT_FACTOR
                            - this.getY(),
                    target.getZ() - this.getZ());
        }

        // The original's melee path is its own attackEntity: it runs every
        // tick a target exists, and the gate inside the body is what limits
        // the hits, so it is kept verbatim.
        final LivingEntity meleeTarget = this.getTarget();
        if (meleeTarget != null) {
            firebatMelee(meleeTarget, (float) meleeTarget.distanceTo(this));
        }
    }

    /** The cell the re-hang roll scans: the original reads
     * {@code (floor(posX), (int)posY + 1, floor(posZ))} — {@code :277}. */
    private BlockPos aboveFlightScan() {
        return BlockPos.containing(this.getX(), this.getY(), this.getZ());
    }

    /** :268-274 and :284-290 — the shared 0.5/0.7/0.5 steering at 0.1, then
     * the yaw derived from the resulting motion (the original also writes the
     * legacy {@code field_70701_bs = 0.5F}, which has no effect on
     * directly-written motion, so it is carried as a constant). */
    private void steerTowards(final double dx, final double dy, final double dz) {
        final Vec3 motion = this.getDeltaMovement();
        final double nx = FirebatAiLogic.steer((float) motion.x, dx, FirebatAiLogic.STEER_XZ);
        final double ny = FirebatAiLogic.steer((float) motion.y, dy, FirebatAiLogic.STEER_Y);
        final double nz = FirebatAiLogic.steer((float) motion.z, dz, FirebatAiLogic.STEER_XZ);
        this.setDeltaMovement(nx, ny, nz);
        final float yaw = FirebatAiLogic.yawFromMotion(nx, nz);
        this.setYRot(this.getYRot() + FirebatAiLogic.yawDelta(yaw, this.getYRot()));
    }

    // ------------------------------------------------------------------ melee

    /** {@code func_70785_a(Entity, float)} (:329-369), transcribed. */
    public void firebatMelee(final Entity target, final float distance) {
        if (!FirebatAiLogic.canMelee(this.attackTime, distance, target.getBbWidth(),
                target.getBoundingBox().maxY, this.getBoundingBox().minY,
                target.getBoundingBox().minY, this.getBoundingBox().maxY)) {
            return;
        }

        if (getIsSummoned() && target instanceof LivingEntity living) {
            // EntityUtils.setRecentlyHit(target, 100) (:335) writes the
            // target's recentlyHit counter; 1.21.1 splits it into the
            // last-hurt-mob / last-hurt-by-player timestamps.
            living.setLastHurtByMob(this);
            if (this.owner != null) {
                living.setLastHurtByPlayer(this.owner);
            }
        }

        if (getIsVampire()) {
            // :338-344 — the owner gains regeneration 26/1 when it has none,
            // and the bat heals 1.0 (Potion.field_76428_l = regeneration).
            if (FirebatAiLogic.appliesVampireRegen(this.owner != null,
                    this.owner != null && this.owner.hasEffect(MobEffects.REGENERATION))) {
                this.owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                        FirebatAiLogic.VAMPIRE_REGEN_DURATION, FirebatAiLogic.VAMPIRE_REGEN_AMPLIFIER));
            }
            this.heal(FirebatAiLogic.VAMPIRE_HEAL);
        }

        this.attackTime = FirebatAiLogic.ATTACK_COOLDOWN;

        if (FirebatAiLogic.blowsUp(getIsExplosive(), random.nextInt(FirebatAiLogic.EXPLOSION_ROLL),
                getIsDevil(), level().isClientSide)) {
            // :348-353 — clear the target's damage cooldown, explode (no fire,
            // no block damage), remove the bat.
            if (target instanceof LivingEntity living) {
                living.invulnerableTime = 0;
            }
            level().explode(this, this.getX(), this.getY(), this.getZ(),
                    FirebatAiLogic.explosionRadius(getIsExplosive(), damBonus),
                    Level.ExplosionInteraction.NONE);
            this.discard();
        } else if (FirebatAiLogic.usesExhaustion(getIsVampire(), random.nextBoolean())) {
            // :354-355 — addExhaustion(4/2); in 1.7.10 that only bites for
            // players, so the player guard is the faithful shape.
            if (target instanceof Player player) {
                player.getFoodData().addExhaustion(FirebatAiLogic.exhaustionFor(getIsSummoned()));
            }
        } else {
            // :356-365 — the plain melee hit with the target's motion restored
            // (the original cancels the knockback it just applied).
            final Vec3 before = target.getDeltaMovement();
            this.doHurtTarget(target);
            target.setDeltaMovement(before);
        }

        // :367-368 — volume 0.5, pitch 0.9 + nextFloat()*0.2, played at the bat.
        level().playSound(this, this.blockPosition(), SoundEvents.BAT_HURT, this.getSoundSource(),
                FirebatAiLogic.ATTACK_SOUND_VOLUME,
                FirebatAiLogic.attackSoundPitch(random.nextFloat()));
    }

    // --------------------------------------------------------------- behaviour

    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        // func_70097_a (:317-327): no damage while removed (!func_85032_ar()),
        // from fire (par1DamageSource.func_76347_k() -> 1.7.10 isFireDamage) or
        // from magic (par1DamageSource.func_94541_c() -> 1.7.10 isMagicDamage).
        // javap: 21.1.259 DamageTypeTags carries IS_FIRE but NO IS_MAGIC, and
        // DamageTypes exposes the magic damage types as ResourceKeys
        // (MAGIC, INDIRECT_MAGIC), so the 1.7.10 magic gate is the id pair.
        if (!FirebatAiLogic.acceptsDamage(this.isRemoved(),
                source.is(DamageTypeTags.IS_FIRE),
                source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC))) {
            return false;
        }
        if (!level().isClientSide && getIsBatHanging()) {
            setIsBatHanging(false);
        }
        return super.hurt(source, amount);
    }

    /** {@code func_70104_M()} (:69-71) and {@code func_70041_e_()}
     * (:303-305) both return false, and both hooks resolve to
     * {@code isPushable()} in this port (the resolution {@code EntityWisp:303}
     * uses), so the firebat is not pushable. */
    @Override
    public boolean isPushable() {
        return FirebatAiLogic.IS_PUSHABLE;
    }

    /** {@code func_70064_a(double,boolean)} (:310-311) is an empty body: the
     * original takes no fall damage, so the 1.21.1 seam is a no-op. */
    @Override
    protected void checkFallDamage(final double y, final boolean onGround, final BlockState state,
            final BlockPos pos) {
        // deliberately empty — the original's fall handler is empty
    }

    @Override
    public boolean checkSpawnRules(final LevelAccessor level, final MobSpawnType spawnType) {
        // func_70601_bi (:388-395): the light at the feet above nextInt(7)
        // refuses the spawn, otherwise the platform's own monster check
        // applies. The y is floor_double(boundingBox.minY) (:389).
        final BlockPos feet = BlockPos.containing(this.getX(),
                FirebatAiLogic.spawnLightY(this.getBoundingBox().minY), this.getZ());
        return FirebatAiLogic.canSpawnAtLight(level.getMaxLocalRawBrightness(feet),
                random.nextInt(FirebatAiLogic.SPAWN_LIGHT_ROLL))
                && super.checkSpawnRules(level, spawnType);
    }

    // ----------------------------------------------------------------- sounds

    @Override
    protected float getSoundVolume() {
        return FirebatAiLogic.SOUND_VOLUME; // :49-51
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // :57-59 — while hanging, three of four rolls are silent.
        if (getIsBatHanging() && random.nextInt(FirebatAiLogic.AMBIENT_WHILE_HANGING_ROLL) != 0) {
            return null;
        }
        return SoundEvents.BAT_AMBIENT;
    }

    @Override
    public void playAmbientSound() {
        final SoundEvent sound = getAmbientSound();
        if (sound != null) {
            // func_70647_i() (:53-55) = the platform pitch * 0.95; 1.21.1 has
            // no getSoundPitch(), so the factor lands at this call site.
            this.playSound(sound, getSoundVolume(), FirebatAiLogic.SOUND_PITCH_FACTOR);
        }
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return SoundEvents.BAT_HURT; // :61-63
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH; // :65-67
    }

    /** {@code func_70069_a(float)} (:307-308) is empty: the bat is silent on
     * footsteps. */
    @Override
    protected void playStepSound(final BlockPos pos, final BlockState state) {
        // deliberately silent
    }

    // ------------------------------------------------------------------- data

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(FirebatAiLogic.NBT_FLAGS, getEntityData().get(DATA_FLAGS));   // :384
        tag.putByte(FirebatAiLogic.NBT_DAM_BONUS, (byte) this.damBonus);          // :385
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        getEntityData().set(DATA_FLAGS, tag.getByte(FirebatAiLogic.NBT_FLAGS));   // :378
        this.damBonus = tag.getByte(FirebatAiLogic.NBT_DAM_BONUS);                // :379
        applyAttackDamage();
    }

    // ------------------------------------------------------------ probe seams

    /** The melee gate with explicit inputs, for the probes. */
    public static boolean meleeGate(final int attackTime, final float distance, final float targetWidth,
            final double targetMaxY, final double ownMinY, final double targetMinY, final double ownMaxY) {
        return FirebatAiLogic.canMelee(attackTime, distance, targetWidth, targetMaxY, ownMinY,
                targetMinY, ownMaxY);
    }

    public int getAttackTime() {
        return this.attackTime;
    }

    public void setAttackTime(final int value) {
        this.attackTime = value;
    }

    public BlockPos getCurrentFlightTarget() {
        return this.currentFlightTarget;
    }

    /** The 1.21.1 spawn predicate shape for the spawn-placement registration
     * (the mob_spawn rows are the spawn-list card, TC-M6-06-s-c6). */
    public static boolean checkFirebatSpawnRules(final EntityType<EntityFireBat> type,
            final LevelAccessor level, final MobSpawnType spawnType, final BlockPos pos,
            final RandomSource random) {
        return level.getDifficulty().getId() > 0
                && FirebatAiLogic.canSpawnAtLight(level.getMaxLocalRawBrightness(pos),
                        random.nextInt(FirebatAiLogic.SPAWN_LIGHT_ROLL));
    }

    /** Unused today: the drop seam stays unshipped on purpose (see
     * {@link FirebatAiLogic#DROP_ITEM_ID} — the vanilla 1.7.10 roll is not in
     * the reference tree, so the port ships no drop body). Kept as the
     * documented seam the follow-up card fills. */
    protected void firebatDropSeam(final ServerLevel level, final DamageSource source) {
        // no body — see FirebatAiLogic.DROP_ITEM_ID
    }
}
