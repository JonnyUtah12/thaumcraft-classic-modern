package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaintFibres;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic.GoalSpec;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTaint;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Port of {@code thaumcraft.common.entities.monster.EntityTaintCreeper}
 * (TC-M6-06-s-b). The swell/fuse/explosion body is hand-rolled (the
 * original extends {@code EntityMob}, not the vanilla creeper — its state
 * machine lives in {@code func_70071_h_}), so the port keeps the shape:
 * swell state in synched data slot 16 (byte, init -1), fuse accumulation,
 * the 1.5F blast (the loaded {@code explosionRadius} field is NEVER used —
 * preserved quirk), the taint-poison splash (the parent card's
 * {@code applyTaintPoison} gate), and the 10-try biome bias.
 *
 * <p>Attributes ({@code
 * func_110147_ax} lines 52-58): health 30.0, attack 2.0, speed 0.25. Armour: the original declares NO {@code
 * func_70658_aO} override, so the mob fights with armour 0 — the port
 * matches by not adding {@code Attributes.ARMOR}. The explosion uses
 * {@code Level.ExplosionInteraction.MOB}: 1.7.10's
 * {@code func_72876_a(entity, x, y, z, radius, flaming)} overload always
 * destroys blocks (the {@code mobGriefing} gamerule did not exist), so the
 * port passes MOB unconditionally and bypasses 1.21.1's mobGriefing gate.
 * 1.7.10's explosion engine set blocks to air without item drops, which
 * 1.21.1's MOB interaction does not reproduce — a documented engine-level
 * deviation.</p>
 */
public class EntityTaintCreeper extends EntityTaintedAnimalBase {

    /** {@code field_70713_aJ} (1.7.10 data 16, byte, init -1): swell state. */
    private static final EntityDataAccessor<Byte> DATA_SWELL =
            SynchedEntityData.defineId(EntityTaintCreeper.class, EntityDataSerializers.BYTE);
    /** {@code field_70714_aK} (data 17, byte): the powered flag. */
    private static final EntityDataAccessor<Byte> DATA_POWERED =
            SynchedEntityData.defineId(EntityTaintCreeper.class, EntityDataSerializers.BYTE);

    private int timeSinceIgnited;
    private int lastTimeSinceIgnited;
    /**
     * {@code fuseTime} (field, line 36, init 30): the fuse CAP — loaded
     * from the "Fuse" NBT short, and what "Fuse" is written back as. The
     * original NEVER persists the progress.
     */
    private int fuseTime = TaintCreeperAiLogic.FUSE_TICKS;
    /** NBT round-trips, unused by the blast (the 1.5F quirk, see the class doc). */
    private float explosionRadius = TaintCreeperAiLogic.LOADED_RADIUS_FIELD;

    public EntityTaintCreeper(final EntityType<? extends EntityTaintCreeper> type, final Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // func_110147_ax (lines 52-58): attack 2.0 (56), max health 30.0
        // (55), speed 0.25 (54 + 57)
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, TaintCreeperAiLogic.HEALTH)
                .add(Attributes.ATTACK_DAMAGE, TaintCreeperAiLogic.ATTACK)
                .add(Attributes.MOVEMENT_SPEED, TaintCreeperAiLogic.MOVEMENT_SPEED);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SWELL, (byte) -1);
        builder.define(DATA_POWERED, (byte) 0);
    }

    public int getCreeperState() {
        return this.entityData.get(DATA_SWELL);
    }

    public void setCreeperState(final int state) {
        this.entityData.set(DATA_SWELL, (byte) state);
    }

    public int getTimeSinceIgnited() {
        return this.timeSinceIgnited;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        // func_70071_h_ (method line 120, body through 140): the original
        // runs the AI body only while NOT dead — during the death animation
        // the state machine is off and the fuse is driven by the
        // onDeathUpdate hook (func_70069_a) below
        if (this.isDeadOrDying()) {
            return;
        }
        this.lastTimeSinceIgnited = this.timeSinceIgnited;
        final int state = this.getCreeperState();
        if (state > 0 && this.timeSinceIgnited == 0) {
            // line 125: "random.fuse" at volume 1.0, pitch 0.5
            this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, TaintCreeperAiLogic.FUSE_SOUND_PITCH);
        }
        this.timeSinceIgnited = TaintCreeperAiLogic.nextFuse(this.timeSinceIgnited, state,
                this.fuseTime);
        if (TaintCreeperAiLogic.explodedThisTick(this.timeSinceIgnited, this.fuseTime)) {
            explode();
        }
    }

    /**
     * {@code func_70069_a(float)} (lines 70-77): during the death animation
     * (par1 = the 1.0-per-tick deathTime step) the fuse accelerates by
     * {@code par1 * 1.5F} truncated to int, capped at {@code fuseTime - 5}.
     * 1.21.1's hook for that animation is {@code tickDeath()}.
     */
    @Override
    protected void tickDeath() {
        super.tickDeath();
        if (!this.level().isClientSide) {
            this.timeSinceIgnited = TaintCreeperAiLogic.deathFuseStep(this.timeSinceIgnited, 1.0F);
            if (this.timeSinceIgnited > this.fuseTime - TaintCreeperAiLogic.DEATH_FUSE_HEADROOM) {
                this.timeSinceIgnited = this.fuseTime - TaintCreeperAiLogic.DEATH_FUSE_HEADROOM;
            }
        }
    }

    /**
     * {@code func_82143_as} (lines 65-67): experience is {@code 3} when the
     * creeper has NO target and {@code 3 + (int)(maxHealth - 1)} (= 32 at
     * health 30.0) when it does. 1.21.1's seam is
     * {@code getBaseExperienceReward()}.
     */
    @Override
    protected int getBaseExperienceReward() {
        return TaintCreeperAiLogic.experiencePoints(this.getTarget() != null, this.getMaxHealth());
    }

    /** The decompiled formula's value, for the probes. */
    public int creeperExperiencePoints() {
        return this.getBaseExperienceReward();
    }

    /** {@code explode()} (method lines 133-171; the blast call is line 136,
     *  the setDead call is line 170). */
    protected void explode() {
        if (!this.level().isClientSide) {
            // line 136: the blast radius is always 1.5F, flaming false (the
            // loaded field is a preserved quirk, see the class doc); the
            // 1.7.10 overload always destroys blocks (mobGriefing did not
            // exist), so the port passes MOB unconditionally — bypassing
            // 1.21.1's mobGriefing gate is the faithful behaviour
            this.level().explode(this, this.getX(), this.getY() + this.getBbHeight() / 2.0D,
                    this.getZ(), TaintCreeperAiLogic.EXPLOSION_RADIUS,
                    Level.ExplosionInteraction.MOB);
            // lines 143-152: the poison box is the entity's POSITION (a
            // zero-size AABB) inflated 6.0 on all three axes; the taint gate
            // lives in TaintManager.applyTaintPoison's ITaintedMob check
            final double inflate = TaintCreeperAiLogic.POISON_BOX_INFLATE;
            final AABB box = new AABB(this.getX(), this.getY(), this.getZ(),
                    this.getX(), this.getY(), this.getZ()).inflate(inflate);
            for (final LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box)) {
                if (TaintCreeperAiLogic.poisonsTarget(living instanceof com.jonnyutah12.thaumcraftclassic
                        .api.entities.ITaintedMob, living.isInWaterOrRain())) {
                    TaintManager.applyTaintPoison(living, TaintCreeperAiLogic.POISON_DURATION);
                }
            }
            // lines 156-168: the biome write-up — 10 attempts, each with its
            // OWN coin and biome test, offsets from two independent draws
            if (this.level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < TaintCreeperAiLogic.BIAS_ATTEMPTS; i++) {
                    final int xx = Mth.floor(this.getX()) + TaintCreeperAiLogic.biasOffset(
                            this.getRandom().nextFloat(), this.getRandom().nextFloat());
                    final int zz = Mth.floor(this.getZ()) + TaintCreeperAiLogic.biasOffset(
                            this.getRandom().nextFloat(), this.getRandom().nextFloat());
                    final int yy = Mth.floor(this.getY());
                    final Holder<net.minecraft.world.level.biome.Biome> biome =
                            TaintBiomes.biomeAt(serverLevel, new BlockPos(xx, 0, zz));
                    if (TaintCreeperAiLogic.biasPlacementRoll(this.getRandom().nextBoolean())
                            && !TaintBiomes.isTaintBiome(biome)) {
                        TaintBiomes.setBiomeAtAndSync(serverLevel, xx, zz,
                                TaintBiomes.taintBiomeHolder(serverLevel));
                        final BlockPos below = new BlockPos(xx, yy - 1, zz);
                        final BlockPos at = new BlockPos(xx, yy, zz);
                        // line 161: the cell below must be a solid render block
                        // (func_147445_c) and the cell itself replaceable
                        if (serverLevel.getBlockState(below).isSolidRender(serverLevel, below)
                                && serverLevel.getBlockState(at).canBeReplaced()) {
                            serverLevel.setBlock(at, CatalogTaint.BLOCK_TAINT_FIBRES.get()
                                    .defaultBlockState()
                                    .setValue(BlockTaintFibres.VARIANT,
                                            TaintCreeperAiLogic.FIBRES_PLACED_META), 3);
                        }
                    }
                }
            }
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // lines 82-87: "powered" is written ONLY when the data slot 17 says
        // so (82-83), "Fuse" is the CAP field fuseTime as a short (86 — the
        // progress is NEVER saved) and "ExplosionRadius" a byte (87)
        if (this.entityData.get(DATA_POWERED) != 0) {
            tag.putBoolean(TaintCreeperAiLogic.TAG_POWERED, true);
        }
        tag.putShort(TaintCreeperAiLogic.TAG_FUSE, (short) this.fuseTime);
        tag.putByte(TaintCreeperAiLogic.TAG_EXPLOSION_RADIUS, (byte) this.explosionRadius);
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // line 92: "powered" (default false) lands in data slot 17; lines
        // 93-99: "Fuse" and "ExplosionRadius" load ONLY when the key
        // exists, so the field defaults (30 / 3) survive an absent key
        this.entityData.set(DATA_POWERED, (byte) (tag.getBoolean(TaintCreeperAiLogic.TAG_POWERED) ? 1 : 0));
        if (tag.contains(TaintCreeperAiLogic.TAG_FUSE)) {
            this.fuseTime = tag.getShort(TaintCreeperAiLogic.TAG_FUSE);
        }
        if (tag.contains(TaintCreeperAiLogic.TAG_EXPLOSION_RADIUS)) {
            this.explosionRadius = tag.getByte(TaintCreeperAiLogic.TAG_EXPLOSION_RADIUS);
        }
    }

    @Override
    protected List<GoalSpec> goalTable() {
        return TaintedAnimalAiLogic.creeperGoals();
    }

    @Override
    protected List<GoalSpec> targetTable() {
        return TaintedAnimalAiLogic.creeperTargetGoals();
    }

    /** func_70647_i (getSoundPitch) → 1.21.1's getVoicePitch (lines 203-204): 0.7. */
    @Override
    public float getVoicePitch() {
        return TaintedAnimalAiLogic.creeperSoundPitch();
    }

    // The original swaps two slots and leaves the third vanilla-silent:
    // func_70673_aS (living) lines 199-200 "mob.creeper.death",
    // func_70621_aR (death) lines 195-196 "mob.creeper.say", and NO
    // func_70639_aQ override — a hurt creeper is SILENT (1.7.10's default
    // getHurtSound is null, and 1.21.1's Mob.getHurtSound default is null
    // too, so not overriding reproduces the silence).
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CREEPER_DEATH; // "mob.creeper.death" (199-200)
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREEPER_HURT; // "mob.creeper.say" (195-196)
    }

    /** {@code AICreeperSwell} — canUse 14-18, start 20-23, update 29-38. */
    public final class SwellGoal extends Goal {
        private LivingEntity attackTarget;

        @Override
        public boolean canUse() {
            final LivingEntity target = EntityTaintCreeper.this.getTarget();
            return EntityTaintCreeper.this.getCreeperState() > 0
                    || (target != null && EntityTaintCreeper.this.distanceToSqr(target) < 9.0D);
        }

        @Override
        public void start() {
            EntityTaintCreeper.this.getNavigation().stop();
            this.attackTarget = EntityTaintCreeper.this.getTarget();
        }

        @Override
        public void stop() {
            this.attackTarget = null;
        }

        @Override
        public void tick() {
            EntityTaintCreeper.this.setCreeperState(TaintCreeperAiLogic.swellUpdateState(
                    this.attackTarget != null && this.attackTarget.isAlive(),
                    this.attackTarget == null ? 0.0D : EntityTaintCreeper.this.distanceToSqr(this.attackTarget),
                    this.attackTarget != null
                            && EntityTaintCreeper.this.getSensing().hasLineOfSight(this.attackTarget)));
        }
    }
}
