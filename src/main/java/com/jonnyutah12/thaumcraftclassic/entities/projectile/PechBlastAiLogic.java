package com.jonnyutah12.thaumcraftclassic.entities.projectile;

/**
 * Every constant and every rule of the Pech Blast projectile, kept
 * {@code Level}-free so the unit test can pin them without a world.
 *
 * <p>Original: {@code thaumcraft.common.entities.projectile.EntityPechBlast},
 * 182 lines, {@code extends EntityThrowable} (the file located with
 * {@code find reference/decompiled -ipath '*PechBlast*'} — it lives under
 * {@code entities/projectile/}, not {@code entities/monster/}). Ledger row
 * {@code docs/content-mapping.csv:308} ({@code Thaumcraft:PechBlast} →
 * {@code thaumcraft:pechblast}, mod_index 10).</p>
 *
 * <p>Registration ({@code common/config/ConfigEntities.java:86}):
 * {@code registerModEntity("PechBlast", EntityPechBlast.class, "PechBlast", 10,
 * Thaumcraft.instance, 64, 20, true)} — tracking range 64, update interval
 * <b>20</b> (ten times the pech's own 3), velocity updates ON. The update
 * interval is what makes this projectile ride the server's motion: the original
 * relies on the tracker for its arc.</p>
 */
public final class PechBlastAiLogic {

    // ---- registration (ConfigEntities.java:86) ----
    public static final int TRACK_RANGE_BLOCKS = 64;
    /** The original's 64 blocks become 4 CHUNKS in 1.21.1's
     *  {@code clientTrackingRange} (the family rule, {@code
     *  BrainyZombieAiLogicTest:38}). */
    public static final int TRACK_RANGE_CHUNKS = TRACK_RANGE_BLOCKS / 16;
    public static final int UPDATE_INTERVAL = 20;
    public static final boolean VELOCITY_UPDATES = true;

    // ---- geometry ----
    /** 1.7.10 {@code EntityThrowable} {@code func_70026_a(0.5F, 0.5F)}. */
    public static final float WIDTH = 0.5F;
    public static final float HEIGHT = 0.5F;

    /**
     * {@code :38-40} — {@code func_70185_h() { return 0.025F; }}. 1.21.1's
     * gravity hook is the {@code Gravity} attribute
     * ({@code Entity#getDefaultGravity()}), whose vanilla default is 0.08:
     * the override is required, not cosmetic.
     */
    public static final float GRAVITY = 0.025F;

    /**
     * The launch speed and inaccuracy of the aim: the caller
     * {@code EntityPech.java:486} passes
     * {@code func_70182_d(d0, d1 + f1*0.1F, d2, 1.5F, 4.0F)} —
     * {@code :42-44} pins the speed at {@code 1.5F}.
     */
    public static final float SPEED = 1.5F;
    public static final float AIM_INACCURACY = 4.0F;
    /** The caller's eye-height term at {@code EntityPech.java:486}. */
    public static final float AIM_EYE_BIAS = 0.1F;

    /** {@code :79-81} — {@code if (ticksExisted > 500) func_70106_y();}. */
    public static final int MAX_AGE = 500;

    /** {@code :142} — the target scan box is {@code func_72314_b(2, 2, 2)}. */
    public static final double SCAN_INFLATE = 2.0;

    // ---- impact (onImpact, :84-177) ----
    /**
     * {@code :147} — {@code func_70097_a(attack, strength + 2)}. At the only
     * call site ({@code EntityPech.java:481}) {@code strength} is 1, so the
     * blast deals <b>3</b>. The 1.7.10 magic damage source applied the
     * {@code DamageUtils.playerMagic(par3)} difficulty scale in the parent —
     * the blast itself passes the figure verbatim; 1.21.1's magic damage source
     * applies no difficulty scalar, recorded as a behavioural deviation.
     */
    public static int damageFor(final int strength) {
        return strength + 2;
    }

    /**
     * {@code :149-171} — the effect block, wrapped in {@code try/catch
     * (Exception)} in the original. The three rolls are three independent
     * {@code rand.nextInt(3) == 0} draws, so a nightshade blast can stack all
     * three effects.
     */
    public static final int EFFECT_ROLL_BOUND = 3;
    public static final int EFFECT_BASE_DURATION = 100;
    public static final int EFFECT_DURATION_PER_DURATION_ARG = 40;
    /** {@code :154} is the one amplifier that is {@code strength + 1}. */
    public static final int SLOWNESS_AMPLIFIER_OFFSET = 1;
    /** {@code :481} at the pech call site: {@code nextFloat() < 0.1F}. */
    public static final float NIGHTSHADE_CHANCE = 0.1F;

    /** {@code :144} — the blast never touches its own species (and 1.7.10's
     *  {@code EntityThrowable.canHitEntity} already excludes the thrower). */
    public static boolean canAffect(final boolean pech, final boolean living) {
        return living && !pech;
    }

    public static int effectDuration(final int duration) {
        return EFFECT_BASE_DURATION + duration * EFFECT_DURATION_PER_DURATION_ARG;
    }

    public static boolean nightshadeFor(final float roll) {
        return roll < NIGHTSHADE_CHANCE;
    }

    public static boolean isExpired(final int age) {
        return age > MAX_AGE;
    }

    // ---- client FX: the trail (:46-76) and the impact (:86-138), both
    //      {@code field_72995_K} (client) only ----
    /** {@code :48} / {@code :86} — the loop counts. */
    public static final int TRAIL_ROUNDS = 3;
    public static final int IMPACT_ROUNDS = 9;
    /** {@code :52-54} / {@code :87-89} — the per-axis jitter factor. */
    public static final float TRAIL_POS_JITTER = 0.2F;
    public static final float SPARKLE_POS_JITTER = 0.1F;
    public static final float IMPACT_POS_JITTER = 0.3F;
    /** {@code :96-98} — the {@code wispFX3} target is {@code pos + jitter*8.0F}. */
    public static final float IMPACT_TARGET_SCALE = 8.0F;
    /** The {@code wispFX2} {@code (size, type, ...)} pair, both trail calls. */
    public static final float PARTICLE_SIZE = 0.3F;
    public static final float PARTICLE_FADE = 0.02F;
    /** Trail: {@code :56} type 3 at the position, {@code :67} type 2 at the
     *  midpoint of position and position+motion ({@code :61-66}). */
    public static final int TRAIL_TYPE_AT_POSITION = 3;
    public static final int TRAIL_TYPE_AT_MIDPOINT = 2;
    /** {@code :73} — the sparkle's type. */
    public static final int TRAIL_SPARKLE_TYPE = 5;
    /** Impact: {@code wispFX3} types 3, 2, 0 in that order per round. */
    public static final int[] IMPACT_TYPES = {3, 2, 0};
    /** The midpoint divisor of {@code :61-66}. */
    public static final double MIDPOINT_DIVISOR = 2.0;

    /**
     * One pinned Thaumcraft generic-particle call. The proxy's
     * {@code wispFX2}/{@code wispFX3}/{@code sparkles} have no 1.21.1 sprite
     * (the generic-particle atlas is not ported), so no substitute particle is
     * invented here: the client bodies roll these parameters and the send
     * belongs to the FX lane (t_6eb0c413). Mirrors {@code EntityFireBat.ExplosiveFx}.
     */
    public record BlastFxCall(double dx, double dy, double dz, double tx, double ty, double tz,
            int type, float size, float fade) {

        /** One trail particle at the blast's own position ({@code :49-60}). */
        public static BlastFxCall atPosition(final net.minecraft.util.RandomSource random,
                final double x, final double y, final double z) {
            return new BlastFxCall(jitter(random, TRAIL_POS_JITTER), jitter(random, TRAIL_POS_JITTER),
                    jitter(random, TRAIL_POS_JITTER), x, y, z, TRAIL_TYPE_AT_POSITION, PARTICLE_SIZE,
                    PARTICLE_FADE);
        }

        /** The trail's second particle, at {@code (pos + motion)/2} ({@code :61-67}). */
        public static BlastFxCall atMidpoint(final net.minecraft.util.RandomSource random,
                final double midX, final double midY, final double midZ) {
            return new BlastFxCall(jitter(random, TRAIL_POS_JITTER), jitter(random, TRAIL_POS_JITTER),
                    jitter(random, TRAIL_POS_JITTER), midX, midY, midZ, TRAIL_TYPE_AT_MIDPOINT,
                    PARTICLE_SIZE, PARTICLE_FADE);
        }

        /** The trail's {@code sparkle} call ({@code :68-74}), jitter factor 0.1F. */
        public static BlastFxCall sparkle(final net.minecraft.util.RandomSource random,
                final double x, final double y, final double z) {
            return new BlastFxCall(jitter(random, SPARKLE_POS_JITTER), jitter(random, SPARKLE_POS_JITTER),
                    jitter(random, SPARKLE_POS_JITTER), x, y, z, TRAIL_SPARKLE_TYPE, 0.0F, 0.0F);
        }

        /** One impact particle ({@code :90-103}); the target triple is the
         *  original's {@code pos + jitter*8.0F} shape, not a velocity. */
        public static BlastFxCall impact(final net.minecraft.util.RandomSource random, final double x,
                final double y, final double z, final int typeIndex) {
            final double fx = jitter(random, IMPACT_POS_JITTER);
            final double fy = jitter(random, IMPACT_POS_JITTER);
            final double fz = jitter(random, IMPACT_POS_JITTER);
            return new BlastFxCall(x + fx, y + fy, z + fz, x + fx * IMPACT_TARGET_SCALE,
                    y + fy * IMPACT_TARGET_SCALE, z + fz * IMPACT_TARGET_SCALE,
                    IMPACT_TYPES[typeIndex], PARTICLE_SIZE, PARTICLE_FADE);
        }

        /** {@code (nextFloat() - nextFloat()) * factor}, the original's jitter idiom. */
        public static double jitter(final net.minecraft.util.RandomSource random, final float factor) {
            return (random.nextFloat() - random.nextFloat()) * factor;
        }
    }

    private PechBlastAiLogic() {
    }
}
