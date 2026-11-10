package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTaintMobs;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * TC-M6-06-s-e: the thaumic slime (content-mapping.csv row {@code thaumslime},
 * original {@code thaumcraft.common.entities.monster.EntityThaumicSlime},
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/
 * EntityThaumicSlime.java}).
 *
 * <p>The original is NOT a 1.7.10 {@code EntitySlime}: it extends
 * {@code EntityMob} (line 20) and hand-rolls the whole slime loop. Every number
 * is a {@link ThaumicSlimeAiLogic} constant quoting a decompiled line, so this
 * class only walks state.</p>
 *
 * <p>Kept quirks, pinned by {@code ThaumicSlimeAiLogicTest}:</p>
 * <ul>
 *   <li>the size is synced data {@code 16} as a <b>Byte</b> (ctor line 88,
 *       {@code getSlimeSize} line 93); the NBT {@code "Size"} is written as
 *       {@code size - 1} and read as {@code + 1} (lines 101 / 106), so a saved
 *       size-1 slime loads as size 2 after a size-0 write;</li>
 *   <li>the merge scan reassigns its running distance for EVERY scanned slime
 *       (line 180, outside the {@code if}): "closest" means "closer than the
 *       previously scanned one";</li>
 *   <li>the merge gives the combined size to the OTHER slime and kills this
 *       one (lines 212-213);</li>
 *   <li>the spit shrinks THIS slime by 1 (line 206) and throws a size-1 slime
 *       (lines 199-201);</li>
 *   <li>death splits into {@code sqrt(size)} size-1 slimes, gated on health
 *       already being {@code <= 0} (lines 253-266);</li>
 *   <li>{@code experienceValue = (int) sqrt(size)} (line 88) while the reward
 *       is overridden to 0 (line 322) — a killed thaumic slime pays nothing;</li>
 *   <li>difficulty id 0 (peaceful) erases the slime, server side (line 118).</li>
 * </ul>
 */
public class EntityThaumicSlime extends GoallessTaintMonster {

    /** 1.7.10 synced data 16 (ctor line 88): the size, as a Byte. */
    private static final EntityDataAccessor<Byte> DATA_SIZE =
            SynchedEntityData.defineId(EntityThaumicSlime.class, EntityDataSerializers.BYTE);

    /** {@code field_70813_a} squishAmount — plain fields, the original does not watch them. */
    private float squishAmount;
    /** {@code field_70811_b} squishFactor. */
    private float squishFactor;
    /** {@code field_70812_c} prevSquishFactor. */
    private float prevSquishFactor;

    private int slimeJumpDelay;
    /** Line 26: {@code int launched = 10}. */
    private int launched = ThaumicSlimeAiLogic.LAUNCHED_START;
    /** Line 27: {@code int spitCounter = 100}. */
    private int spitCounter = ThaumicSlimeAiLogic.SPIT_COUNTER_START;
    /** The merge scan accumulator, quirk included (lines 168-181). */
    private final ThaumicSlimeAiLogic.ScanState mergeScan = new ThaumicSlimeAiLogic.ScanState();

    public EntityThaumicSlime(final EntityType<? extends Monster> type, final Level level) {
        super(type, level);
        // ctor lines 31-34: size = 1 << rand(3), jump delay = rand(20) + 10.
        // Ctor line 32 (yOffset = 0) has no 1.21.1 analogue: the eye height
        // comes from the entity dimensions, so there is nothing to zero.
        this.setSlimeSize(ThaumicSlimeAiLogic.initialSize(this.random.nextInt(3)));
        this.slimeJumpDelay = ThaumicSlimeAiLogic.initialJumpDelay(this.random.nextInt(20));
    }

    /**
     * The original sets no attack damage and derives health from the size
     * (line 86), so the attribute defaults are placeholders that
     * {@link #setSlimeSize(int)} overwrites on every spawn, load and merge.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.15);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // ctor line 88: data 16 is created with the value 1.
        builder.define(DATA_SIZE, (byte) 1);
    }

    // ------------------------------------------------------------------ size

    public int getSlimeSize() {
        return this.entityData.get(DATA_SIZE);
    }

    /**
     * Lines 83-90: box {@code 0.25*sqrt(size)+0.25} on BOTH axes, the max-health
     * attribute IS the size, health refills to that max, and
     * {@code experienceValue} becomes {@code (int) sqrt(size)}.
     */
    public void setSlimeSize(final int size) {
        this.entityData.set(DATA_SIZE, (byte) size);
        this.refreshDimensions();
        this.getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(ThaumicSlimeAiLogic.sizeToMaxHealth(size));
        this.setHealth(this.getMaxHealth());
        this.xpReward = ThaumicSlimeAiLogic.sizeToExperience(size);
    }

    /**
     * 1.7.10 {@code setSlimeSize} (lines 84-85) sizes the box directly; 1.21.1
     * derives it, so the slime family overrides {@code getDimensions} — the
     * same seam vanilla 1.21.1 {@code Slime} uses — with the original
     * expression {@code 0.25*sqrt(size)+0.25} on both axes.
     *
     * <p>Lines 155-158 are the client half: on the client the box is
     * {@code 0.6*sqrt(size)} (the render pulse), while the server keeps the
     * collision box. The gate is kept, so the server box never shrinks.</p>
     */
    @Override
    protected net.minecraft.world.entity.EntityDimensions getDefaultDimensions(final Pose pose) {
        final float box = this.level() != null && this.level().isClientSide
                ? ThaumicSlimeAiLogic.clientBox(this.getSlimeSize())
                : ThaumicSlimeAiLogic.sizeToBox(this.getSlimeSize());
        return net.minecraft.world.entity.EntityDimensions.scalable(box, box);
    }

    /** Lines 91-97: {@code getAttackStrength()} is the raw size. */
    protected int getAttackStrength() {
        return ThaumicSlimeAiLogic.attackStrength(this.getSlimeSize());
    }

    /**
     * Line 322: {@code func_70646_bf() { return 0; }} — the slime sets
     * {@code experienceValue} and then refuses to pay it. Kept verbatim, with
     * the field still carrying {@code sqrt(size)}.
     */
    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    // --------------------------------------------------------------- onUpdate

    @Override
    public void tick() {
        // func_70071_h_ lines 117-153.
        final Level level = this.level();
        if (!level.isClientSide && level.getDifficulty() == Difficulty.PEACEFUL
                && ThaumicSlimeAiLogic.selfDestructsOnPeaceful(this.getSlimeSize())) {
            // line 118: difficulty id 0 (peaceful) erases the slime, server side.
            this.discard();
            return;
        }

        // lines 122-123: the squish lerp runs on both sides.
        this.squishFactor = ThaumicSlimeAiLogic.squishStep(this.squishFactor, this.squishAmount);
        this.prevSquishFactor = this.squishFactor;

        final boolean wasOnGround = this.onGround();
        super.tick();

        if (this.launched > 0) {
            // line 128: the counter decrements on BOTH sides.
            this.launched--;
            if (level.isClientSide) {
                // lines 129-133, the world.field_72995_K half: the client draws
                // i * (launched + 1) particles. A game-test server is never
                // client-side, so the extracted count is pinned in the unit
                // test and the broadcast below carries the burst to real clients.
                this.clientSlimeParticles(ThaumicSlimeAiLogic.launchedParticleCount(
                        this.getSlimeSize(), this.launched));
            }
        }

        if (this.onGround() && !wasOnGround) {
            if (level.isClientSide) {
                this.clientSlimeParticles(ThaumicSlimeAiLogic.landingParticleCount(this.getSlimeSize()));
            }
            if (ThaumicSlimeAiLogic.makesSoundOnLand(this.getSlimeSize())) {
                // line 143: the landing sound reuses the jump sample.
                this.playSlimeSound();
            }
            this.squishAmount = ThaumicSlimeAiLogic.SQUISH_ON_LAND;
        } else if (!this.onGround() && wasOnGround) {
            this.squishAmount = ThaumicSlimeAiLogic.SQUISH_ON_JUMP;
        }

        // line 149: func_70808_l()
        this.squishAmount *= ThaumicSlimeAiLogic.SQUISH_DECAY;
        // lines 155-158 (the client box pulse) live in getDimensions, which is
        // the 1.21.1 seam for a variable-size entity box.
    }

    /** The client particle bursts of lines 129-131 and 136-138. */
    private void clientSlimeParticles(final int count) {
        for (int j = 0; j < count; j++) {
            this.level().addParticle(ParticleTypes.ITEM_SLIME,
                    this.getX() + (this.random.nextFloat() - 0.5F) * this.getBbWidth() * 2.0F,
                    this.getY() + this.random.nextFloat() * this.getBbHeight(),
                    this.getZ() + (this.random.nextFloat() - 0.5F) * this.getBbWidth() * 2.0F,
                    0.0, 0.0, 0.0);
        }
    }

    /** The squish pair, exposed for the render layer and the game test. */
    public float getSquishFactor() {
        return this.squishFactor;
    }

    public float getPrevSquishFactor() {
        return this.prevSquishFactor;
    }

    public int getLaunched() {
        return this.launched;
    }

    public int getSpitCounter() {
        return this.spitCounter;
    }

    // ------------------------------------------------- updateEntityActionState

    @Override
    protected void customServerAiStep() {
        // func_70626_be lines 177-238. The original's first statement is
        // func_70623_bb() (onEntityUpdate: potion ticks, attack/hurt counters),
        // which 1.21.1 runs inside LivingEntity.aiStep.
        final Level level = this.level();
        final Player player = level.getNearestPlayer(this, ThaumicSlimeAiLogic.TARGET_SEARCH_RANGE);
        if (player != null) {
            if (this.spitCounter > 0) {
                this.spitCounter = ThaumicSlimeAiLogic.tickSpitCounter(this.spitCounter);
            }
            aggro(player);
            if (ThaumicSlimeAiLogic.spits(this.distanceTo(player), this.spitCounter, this.getSlimeSize())) {
                this.spitCounter = ThaumicSlimeAiLogic.SPIT_COUNTER_RESET;
                // lines 199-201: the thrown mini slime, server side.
                final EntityThaumicSlime flyslime = CatalogTaintMobs.THAUMIC_SLIME.get().create(level);
                if (flyslime != null) {
                    flyslime.setSlimeSize(1);
                    flyslime.thrownFrom(this, player);
                    level.addFreshEntity(flyslime);
                }
                // line 204: the gore sound, outside the client gate.
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        com.jonnyutah12.thaumcraftclassic.sound.ModSounds.GORE.get(),
                        this.getSoundSource(), 1.0F,
                        (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                this.setSlimeSize(this.getSlimeSize() - 1);
            }
        } else {
            final EntityThaumicSlime slime = this.getClosestMergableSlime();
            if (slime != null) {
                aggro(slime);
                if (ThaumicSlimeAiLogic.mergesAt(this.distanceTo(slime), this.getBbWidth(), slime.getBbWidth())) {
                    // lines 212-213: the other slime absorbs this one; this one
                    // dies with full health, so its death split does NOT fire.
                    slime.setSlimeSize(
                            ThaumicSlimeAiLogic.mergedSize(slime.getSlimeSize(), this.getSlimeSize()));
                    this.discard();
                    return;
                }
            }
        }

        if (this.onGround() && this.slimeJumpDelay-- <= 0) {
            this.slimeJumpDelay = ThaumicSlimeAiLogic.jumpDelay(this.random.nextInt(16));
            if (player != null) {
                this.slimeJumpDelay /= ThaumicSlimeAiLogic.JUMP_DELAY_TARGET_DIVISOR;
            }
            // lines 224-229: the jump. 1.7.10 drives it through isJumping; in
            // 1.21.1 JumpControl owns that flag (its tick writes it), and the
            // slime impulse is applied by the entity itself — vanilla 1.21.1
            // Slime.java:353 does the same — so the port raises the flag AND
            // writes the original 1.7.10 EntityLiving impulse vector.
            this.getJumpControl().jump();
            final Vec3 motion = this.getDeltaMovement();
            final double flat = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            final double nx = flat < 1.0E-6 ? 0.0 : motion.x / flat;
            final double nz = flat < 1.0E-6 ? 0.0 : motion.z / flat;
            this.setDeltaMovement(nx * ThaumicSlimeAiLogic.JUMP_XZ_SCALE,
                    ThaumicSlimeAiLogic.JUMP_Y_IMPULSE,
                    nz * ThaumicSlimeAiLogic.JUMP_XZ_SCALE);
            if (ThaumicSlimeAiLogic.makesSoundOnJump(this.getSlimeSize())) {
                this.playSlimeSound();
            }
            // lines 231-232 → the 1.21.1 movement inputs (1.7.10
            // moveStrafing = xxa, moveForward = zza), with the speed attribute
            // as the travel magnitude, as 1.21.1's MoveControl does.
            this.setXxa(ThaumicSlimeAiLogic.jumpStrafe(this.random.nextFloat()));
            this.setZza(ThaumicSlimeAiLogic.jumpForward(this.getSlimeSize()));
            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
        } else {
            this.setJumping(false);
            if (this.onGround()) {
                // lines 234-237
                this.setXxa(0.0F);
                this.setZza(0.0F);
                this.setSpeed(0.0F);
            }
        }
    }

    /**
     * Lines 162-182: the merge scan, quirk verbatim. The box is the entity's
     * POINT inflated 16 / 8 / 16 — {@code AxisAlignedBB.getBoundingBox(x,y,z,x,y,z)
     * .expand(16, 8, 16)} — so the scan is centred on the feet, not the box.
     */
    protected EntityThaumicSlime getClosestMergableSlime() {
        this.mergeScan.reset(Double.MAX_VALUE);
        final AABB box = new AABB(this.position(), this.position())
                .expandTowards(ThaumicSlimeAiLogic.MERGE_SCAN_XZ, ThaumicSlimeAiLogic.MERGE_SCAN_Y,
                        ThaumicSlimeAiLogic.MERGE_SCAN_XZ);
        final java.util.List<EntityThaumicSlime> near =
                this.level().getEntitiesOfClass(EntityThaumicSlime.class, box);
        for (final EntityThaumicSlime slime : near) {
            if (slime == this) {
                continue;
            }
            ThaumicSlimeAiLogic.mergeScanVisit(this.mergeScan, slime.getId(), this.getId(),
                    slime.tickCount, slime.getSlimeSize(), this.distanceToSqr(slime));
        }
        if (this.mergeScan.closestId < 0) {
            return null;
        }
        return near.stream().filter(s -> s.getId() == this.mergeScan.closestId)
                .findFirst().orElse(null);
    }

    /**
     * Lines 189 / 195: {@code setEntityAttackTarget(target, 10.0F, 20.0F)} —
     * take the target inside 10, drop it beyond 20.
     */
    private void aggro(final LivingEntity target) {
        final float d = this.distanceTo(target);
        if (d <= ThaumicSlimeAiLogic.AGGRO_RANGE) {
            this.setTarget(target);
        } else if (d > ThaumicSlimeAiLogic.DEAGGRO_RANGE && this.getTarget() == target) {
            this.setTarget(null);
        }
    }

    /**
     * The throwing ctor (lines 37-55): size 1, launched from the source's AABB
     * mid-height, aiming at {@code target.minY + target.height/3} with a
     * {@code distance * 0.2} lead; the whole heading is skipped when the
     * horizontal distance is below {@code 1.0E-7}.
     */
    void thrownFrom(final EntityThaumicSlime source, final LivingEntity target) {
        final Vec3 src = source.position();
        this.setPos(src.x, (source.getBoundingBox().minY + source.getBoundingBox().maxY) / 2.0, src.z);
        final double dx = target.getX() - src.x;
        final double dy = target.getBoundingBox().minY + target.getBbHeight()
                / ThaumicSlimeAiLogic.THROW_AIM_HEIGHT_DIVISOR - this.getY();
        final double dz = target.getZ() - src.z;
        final double flat = Math.sqrt(dx * dx + dz * dz);
        if (flat >= ThaumicSlimeAiLogic.THROW_MIN_DISTANCE) {
            final double lead = flat * ThaumicSlimeAiLogic.THROW_LEAD_FACTOR;
            this.setThrowableHeading(dx, dy + lead, dz,
                    ThaumicSlimeAiLogic.THROW_VELOCITY, ThaumicSlimeAiLogic.THROW_INACCURACY);
        }
    }

    /**
     * {@code setThrowableHeading} lines 62-74, verbatim: normalise, add
     * {@code gaussian * 0.0075 * inaccuracy} per axis, scale by the velocity,
     * then face the resulting vector.
     */
    private void setThrowableHeading(final double dx, final double dy, final double dz,
            final float velocity, final float inaccuracy) {
        final float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        double x = dx / len;
        double y = dy / len;
        double z = dz / len;
        x += this.random.nextGaussian() * ThaumicSlimeAiLogic.THROW_GAUSSIAN_SCALE * inaccuracy;
        y += this.random.nextGaussian() * ThaumicSlimeAiLogic.THROW_GAUSSIAN_SCALE * inaccuracy;
        z += this.random.nextGaussian() * ThaumicSlimeAiLogic.THROW_GAUSSIAN_SCALE * inaccuracy;
        x *= velocity;
        y *= velocity;
        z *= velocity;
        this.setDeltaMovement(x, y, z);
        final float flat = (float) Math.sqrt(x * x + z * z);
        this.setYRot((float) (Math.atan2(x, z) * 180.0 / Math.PI));
        this.setXRot((float) (Math.atan2(y, flat) * 180.0 / Math.PI));
    }

    // ---------------------------------------------------------- player touch

    /**
     * Lines 264-282, the 1.7.10 {@code onPlayerUpdate(EntityPlayer)} hook: in
     * 1.21.1 the collision seam is {@link Entity#playerTouch(Player)}, the same
     * seam vanilla slimes use for their contact damage.
     */
    @Override
    public void playerTouch(final Player player) {
        if (!ThaumicSlimeAiLogic.canDamagePlayer(this.getSlimeSize())) {
            return;
        }
        final int i = ThaumicSlimeAiLogic.playerReachIndex(this.getSlimeSize(), this.launched);
        if (canAttackTarget(player)
                && ThaumicSlimeAiLogic.canTouchPlayer(i, this.distanceToSqr(player))) {
            if (player.hurt(this.level().damageSources().mobAttack(this), this.getAttackStrength())) {
                // line 280: the hit sound only on a landed hit.
                this.playSound(SoundEvents.SLIME_ATTACK, 1.0F,
                        (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }
        }
    }

    // --------------------------------------------------------------- splitting

    /**
     * Lines 253-267 ({@code func_70106_y} = setEntityDead) plus the drop hook
     * lines 300-304. The split is gated on health already being {@code <= 0}:
     * a slime removed by the merge (full health) does not split.
     */
    @Override
    public void die(final DamageSource source) {
        final Level level = this.level();
        if (!level.isClientSide) {
            final int i = ThaumicSlimeAiLogic.splitCount(this.getSlimeSize());
            if (ThaumicSlimeAiLogic.splitsOnDeath(this.getSlimeSize(), this.getHealth())) {
                for (int k = 0; k < i; k++) {
                    final EntityThaumicSlime child = CatalogTaintMobs.THAUMIC_SLIME.get().create(level);
                    if (child == null) {
                        continue;
                    }
                    child.setSlimeSize(ThaumicSlimeAiLogic.CHILD_SIZE);
                    child.setPos(this.getX() + ThaumicSlimeAiLogic.splitOffsetX(k, i),
                            this.getY() + ThaumicSlimeAiLogic.CHILD_SPAWN_Y_OFFSET,
                            this.getZ() + ThaumicSlimeAiLogic.splitOffsetZ(k, i));
                    child.setYRot(this.random.nextFloat() * 360.0F);
                    level.addFreshEntity(child);
                }
            }
            // lines 300-304: size < 3 and rand(3) == 0 → one Tainted Goo
            // (itemResource meta 11) at half the entity height.
            if (ThaumicSlimeAiLogic.dropsTaintedGoo(this.getSlimeSize(), this.random.nextInt(3))) {
                this.spawnAtLocation(com.jonnyutah12.thaumcraftclassic.items.ItemResource.stack(11),
                        this.getBbHeight() / 2.0F);
            }
        }
        super.die(source);
    }

    /**
     * Lines 113-115 / 288-294 / 306: jump, ambient and hurt all resolve to the
     * size-selected slime sample at volume {@code 0.1*sqrt(size)}. The vanilla
     * samples are used because the port ships no {@code mob.slime.big/small}
     * assets — the original's own {@code thaumcraft:gore} spit sample exists in
     * the shipped sound registry and is used for the spit.
     */
    private void playSlimeSound() {
        final int size = this.getSlimeSize();
        final net.minecraft.sounds.SoundEvent event = size > ThaumicSlimeAiLogic.BIG_SOUND_SIZE
                ? net.minecraft.sounds.SoundEvents.SLIME_SQUISH
                : net.minecraft.sounds.SoundEvents.SLIME_SQUISH_SMALL;
        this.playSound(event, this.getSoundVolume(),
                (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        // func_70621_aR (line 288): the ambient sample is the size-selected slime.
        return this.getSlimeSize() > ThaumicSlimeAiLogic.BIG_SOUND_SIZE
                ? net.minecraft.sounds.SoundEvents.SLIME_SQUISH
                : net.minecraft.sounds.SoundEvents.SLIME_SQUISH_SMALL;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(final DamageSource source) {
        // func_70673_aS (line 292): the hurt sample is the same pair.
        return this.getAmbientSound();
    }

    @Override
    protected float getSoundVolume() {
        return ThaumicSlimeAiLogic.soundVolume(this.getSlimeSize());
    }

    // ------------------------------------------------------------------- nbt

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // line 101: "Size" is stored as size - 1.
        tag.putInt("Size", ThaumicSlimeAiLogic.sizeToNbt(this.getSlimeSize()));
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // line 106: read back as + 1.
        this.setSlimeSize(ThaumicSlimeAiLogic.nbtToSize(tag.getInt("Size")));
    }

    /** The sound ids the original asks for, for the docs and the probe message. */
    public static ResourceLocation jumpSoundId() {
        return ResourceLocation.withDefaultNamespace(ThaumicSlimeAiLogic.slimeSound(4));
    }
}
