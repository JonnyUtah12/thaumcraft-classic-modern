package com.jonnyutah12.thaumcraftclassic.entities.projectile;

import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityPech;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * The Pech Blast — {@code thaumcraft:pechblast} (content-mapping row 308,
 * mod_index 10). Original:
 * {@code thaumcraft.common.entities.projectile.EntityPechBlast}, 182 lines,
 * {@code extends EntityThrowable} (located with
 * {@code find reference/decompiled -ipath '*PechBlast*'} — it lives under
 * {@code entities/projectile/}).
 *
 * <p>The blast is three things: a slow, nearly gravity-free arc
 * ({@code :38-44}), an area hit that damages every non-pech living entity in a
 * 2-block-inflated box and rolls Thaumcraft effects ({@code :141-176}), and a
 * 500-tick fuse ({@code :79-81}). Every figure lives in
 * {@link PechBlastAiLogic}; this class only moves them.</p>
 *
 * <p>It has exactly one spawner in the original: the pech's type-1 ranged
 * attack, {@code new EntityPechBlast(world, this, 1, 0, rand.nextFloat() <
 * 0.1F)} ({@code EntityPech.java:481}) — strength 1, duration 0, nightshade
 * 10%.</p>
 */
public class EntityPechBlast extends ThrowableProjectile {

    /** {@code :16-18} — the three constructor-carried fields. The original
     *  writes NO NBT for them (it overrides no NBT hook at all), so a saved
     *  blast restarts at (0, 0, false). Recorded, not "fixed". */
    private final int strength;
    private final int duration;
    private final boolean nightshade;

    // The (type, level) form takes the EXACT EntityType<EntityPechBlast>:
    // EntityType.Builder.of(factory, category) infers its type variable from the
    // ctor reference, and a `? extends EntityPechBlast` parameter makes javac
    // infer T = Entity and reject the builder.
    public EntityPechBlast(final EntityType<EntityPechBlast> type, final Level level) {
        super(type, level);
        this.strength = 0;
        this.duration = 0;
        this.nightshade = false;
    }

    /** {@code :24-29} — the thrower constructor the pech uses. */
    public EntityPechBlast(final EntityType<? extends EntityPechBlast> type, final Level level,
            final LivingEntity thrower, final int strength, final int duration,
            final boolean nightshade) {
        super(type, thrower, level);
        this.strength = strength;
        this.duration = duration;
        this.nightshade = nightshade;
    }

    /** {@code :31-36} — the position constructor (the client FX's decorative
     *  blast). No caller in this card: the FX lane owns its send. */
    public EntityPechBlast(final EntityType<? extends EntityPechBlast> type, final Level level,
            final double x, final double y, final double z, final int strength, final int duration,
            final boolean nightshade) {
        super(type, x, y, z, level);
        this.strength = strength;
        this.duration = duration;
        this.nightshade = nightshade;
    }

    /**
     * {@code ConfigEntities.java:86} — {@code registerModEntity("PechBlast",
     * EntityPechBlast.class, "PechBlast", 10, Thaumcraft.instance, 64, 20,
     * true)}: tracking range 64, update interval <b>20</b> (ten times the
     * pech's own 3), velocity updates ON.
     */
    public static EntityType<EntityPechBlast> buildType(final MobCategory category) {
        // The explicit type witness pins the builder's type variable: the
        // overload set makes `EntityPechBlast::new` ambiguous for inference.
        return EntityType.Builder.<EntityPechBlast>of(EntityPechBlast::new, category)
                .sized(PechBlastAiLogic.WIDTH, PechBlastAiLogic.HEIGHT)   // EntityThrowable 0.5x0.5
                .clientTrackingRange(PechBlastAiLogic.TRACK_RANGE_BLOCKS)  // 64
                .updateInterval(PechBlastAiLogic.UPDATE_INTERVAL)          // 20
                .setShouldReceiveVelocityUpdates(PechBlastAiLogic.VELOCITY_UPDATES)
                .build("pechblast");
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        // the original defines no synched data of its own
    }

    /**
     * {@code :38-40} — {@code func_70185_h() = 0.025F}. 1.21.1's gravity is the
     * {@code Gravity} attribute (vanilla default 0.08), so this override is
     * what keeps the original's near-flat arc.
     */
    @Override
    protected double getDefaultGravity() {
        return PechBlastAiLogic.GRAVITY;
    }

    /** {@code :179-181} — {@code func_70053_R() = 0.1F} (the crosshair reach). */
    @Override
    public float getPickRadius() {
        return 0.1F;
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) {
            spawnClientTrailFx();                                     // :46-76
        }
        super.tick();
        if (PechBlastAiLogic.isExpired(this.tickCount)) {              // :79-81
            this.discard();
        }
    }

    /**
     * {@code :46-76} — the client half, split out because a game-test server is
     * never {@code field_72995_K} (isRemote): 3 rounds, each a {@code wispFX2}
     * at the position (type 3), a {@code wispFX2} at the midpoint of
     * {@code (posX + motionX)/2} (the original's own arithmetic, {@code :61-66})
     * and a {@code sparkle} (type 5), jittered by
     * {@code (nextFloat()-nextFloat())*0.2F} / {@code *0.1F}.
     *
     * <p>The generic-particle proxy has no 1.21.1 sprite, so each call's
     * parameters are rolled into {@link PechBlastAiLogic.BlastFxCall} and this
     * stays the single place the send belongs (FX lane t_6eb0c413). No
     * substitute particle is invented; the parent draws its own vanilla
     * projectile trail, which is not a stand-in for them.</p>
     */
    public void spawnClientTrailFx() {
        for (int round = 0; round < PechBlastAiLogic.TRAIL_ROUNDS; round++) {
            PechBlastAiLogic.BlastFxCall.atPosition(this.random, this.getX(), this.getY(), this.getZ());
            final double midX = (this.getX() + this.getDeltaMovement().x())
                    / PechBlastAiLogic.MIDPOINT_DIVISOR;                        // :61
            final double midY = (this.getY() + this.getDeltaMovement().y())
                    / PechBlastAiLogic.MIDPOINT_DIVISOR;                        // :62
            final double midZ = (this.getZ() + this.getDeltaMovement().z())
                    / PechBlastAiLogic.MIDPOINT_DIVISOR;                        // :63
            PechBlastAiLogic.BlastFxCall.atMidpoint(this.random, midX, midY, midZ);
            PechBlastAiLogic.BlastFxCall.sparkle(this.random, this.getX(), this.getY(), this.getZ());
        }
    }

    // 1.21.1's Projectile has no default-particle hook (1.20.x's
    // getDefaultParticle is gone from the source jar), so the parent's own
    // trail draws. The 1.7.10 mod particle with the smoke flag (:45-48) is the
    // FX send lane — see the class javadoc.

    /**
     * {@code :84-177} — {@code func_70184_a}. The client half is the 9-round
     * impact burst ({@code :86-138}); the server half ({@code :141-176}) scans
     * the box inflated by 2 in every direction, skips pechs, damages every other
     * living entity with {@code strength + 2} from an indirect-magic source,
     * rolls the effects and discards the blast.
     *
     * <p>1.7.10's {@code DamageSource.func_76356_a(this, thrower)} is
     * {@code causeIndirectMagicDamage} →
     * {@code DamageSources#indirectMagic}. The 1.7.10 magic source applied the
     * difficulty scale in the parent; 1.21.1's applies none, so the figure
     * reaches the target verbatim (recorded deviation).</p>
     */
    @Override
    protected void onHit(final HitResult hit) {
        if (this.level().isClientSide) {
            spawnClientImpactFx();                                     // :86-138
            return;                                                    // :141 !isRemote
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final AABB box = this.getBoundingBox().inflate(PechBlastAiLogic.SCAN_INFLATE,
                PechBlastAiLogic.SCAN_INFLATE, PechBlastAiLogic.SCAN_INFLATE);   // :142
        final List<Entity> targets = serverLevel.getEntities(this, box);
        for (final Entity target : targets) {
            if (target instanceof EntityPech) {
                continue;                                                       // :146 !(EntityPech)
            }
            if (!(target instanceof LivingEntity living)) {
                continue;                                                       // :146
            }
            living.hurt(serverLevel.damageSources().indirectMagic(this, this.getOwner()),
                    PechBlastAiLogic.damageFor(this.strength));                  // :147
            rollEffects(living);                                                 // :149-171
        }
        this.discard();                                                          // :175
    }

    /**
     * {@code :149-171}. Nightshade: all three effects. Otherwise
     * {@code nextInt(3)}: 0 = poison, 1 = slowness (the only amplifier that is
     * {@code strength + 1}), 2 = weakness. Every duration is
     * {@code 100 + duration*40} — 100 at the pech call site, whose duration arg
     * is 0 ({@code EntityPech.java:481}). The original wraps the block in
     * {@code try/catch (Exception)}; the catch is kept as the same guard and is
     * recorded, not removed.
     *
     * <p>{@code Potion.field_76436_u / field_76421_d / field_76437_t} are
     * {@code poison} / {@code moveSlowdown} / {@code weakness}
     * ({@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv}) →
     * {@link MobEffects#POISON}, {@link MobEffects#MOVEMENT_SLOWDOWN},
     * {@link MobEffects#WEAKNESS}.</p>
     */
    private void rollEffects(final LivingEntity target) {
        try {
            if (this.nightshade) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON,
                        PechBlastAiLogic.effectDuration(this.duration), this.strength));          // :151
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        PechBlastAiLogic.effectDuration(this.duration),
                        this.strength + PechBlastAiLogic.SLOWNESS_AMPLIFIER_OFFSET));             // :153
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                        PechBlastAiLogic.effectDuration(this.duration), this.strength));          // :154
            } else {
                switch (this.random.nextInt(PechBlastAiLogic.EFFECT_ROLL_BOUND)) {                // :156
                    case 0 -> target.addEffect(new MobEffectInstance(MobEffects.POISON,
                            PechBlastAiLogic.effectDuration(this.duration), this.strength));      // :159
                    case 1 -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            PechBlastAiLogic.effectDuration(this.duration),
                            this.strength + PechBlastAiLogic.SLOWNESS_AMPLIFIER_OFFSET));         // :163
                    default -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                            PechBlastAiLogic.effectDuration(this.duration), this.strength));      // :167
                }
            }
        } catch (final RuntimeException ignored) {
            // :170-171 — the original's silent catch (Exception)
        }
    }

    /** {@code :86-138} — 9 rounds of three {@code wispFX3} calls (types 3, 2,
     *  0), jitter {@code (nextFloat()-nextFloat())*0.3F}, target
     *  {@code pos + jitter*8.0F}, size 0.3F, fade 0.02F. Pinned, not sent: see
     *  {@link #spawnClientTrailFx()}. */
    public void spawnClientImpactFx() {
        for (int round = 0; round < PechBlastAiLogic.IMPACT_ROUNDS; round++) {
            for (int type = 0; type < PechBlastAiLogic.IMPACT_TYPES.length; type++) {
                PechBlastAiLogic.BlastFxCall.impact(this.random, this.getX(), this.getY(), this.getZ(), type);
            }
        }
    }

    public int strength() {
        return this.strength;
    }

    public int durationArg() {
        return this.duration;
    }

    public boolean nightshade() {
        return this.nightshade;
    }
}
