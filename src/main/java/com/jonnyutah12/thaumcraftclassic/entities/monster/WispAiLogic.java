package com.jonnyutah12.thaumcraftclassic.entities.monster;

/**
 * Level-free rules for {@code thaumcraft:wisp} (TC-M6-06-s), transcribed from
 * {@code thaumcraft.common.entities.monster.EntityWisp} in
 * {@code reference/decompiled}. The wisp installs <em>no</em> vanilla goal at
 * all in the original — every decision lives in its own update body — so the
 * original's numbers and branches are pinned here as pure functions and the
 * entity class only walks its own state and calls them (the
 * {@code TubeTransportLogic} / {@code JarStorageLogic} house pattern, which is
 * what lets a unit test pin the original without a world).
 *
 * <p>Every constant below carries the source line it was read from:</p>
 * <ul>
 *   <li>{@code func_110147_ax} — health 22.0, attack damage 3.0;</li>
 *   <li>{@code func_70105_a(0.9F, 0.9F)} — the 0.9 x 0.9 hitbox;</li>
 *   <li>{@code func_70641_bl()} — experience reward 2;</li>
 *   <li>{@code func_70071_h_} — waypoint re-pick window (squared distance
 *       outside {@code [1.0, 3600.0]}), waypoint offset 16, course-change
 *       cooldown {@code 2 + rand.nextInt(5)}, steering acceleration 0.1,
 *       aggro roll {@code nextInt(1000)}, target search radius 16, attack
 *       range 16, attack cadence 20, damage rolls 0.66 / 0.4 with the +1.0
 *       stationary bonus, cadence reset {@code -20 + nextInt(20)};</li>
 *   <li>{@code func_70601_bi()} / {@code isValidLightLevel} — 8-wisp cap in a
 *       16-block box, difficulty &gt; 0, sky-light roll 32, light-level roll 8;</li>
 *   <li>{@code func_70599_aP()} — sound volume 0.25;</li>
 *   <li>{@code func_70628_a} — 2 units of the wisp's own aspect as essence.</li>
 * </ul>
 */
public final class WispAiLogic {

    /** {@code SharedMonsterAttributes.field_111267_a} = 22.0. */
    public static final double MAX_HEALTH = 22.0;

    /** {@code SharedMonsterAttributes.field_111264_e} = 3.0. */
    public static final double ATTACK_DAMAGE = 3.0;

    /** {@code func_70641_bl()} returns 2 (the original's field_70728_aV of 5 is
     * overridden by this method and never read). */
    public static final int EXPERIENCE_REWARD = 2;

    /** {@code func_70105_a(0.9F, 0.9F)}. */
    public static final float WIDTH = 0.9F;
    public static final float HEIGHT = 0.9F;

    /** The 16-block box {@code func_70601_bi()} counts other wisps in. */
    public static final double SPAWN_SCAN_RADIUS = 16.0;

    /** {@code func_70601_bi()}: spawn is refused at 8 or more nearby wisps. */
    public static final int SPAWN_MAX_NEARBY = 8;

    /** {@code world.func_72856_b(this, 16.0)} — the target-search radius. */
    public static final double AGGRO_RADIUS = 16.0;

    /** Set when a target is acquired. */
    public static final int AGGRO_COOLDOWN = 50;

    /** Set when the wisp is hurt (both attacker and source are checked). */
    public static final int HURT_AGGRO_COOLDOWN = 200;

    /** {@code field_73012_v.nextInt(1000) == 0} gates the target search. */
    public static final int AGGRO_ROLL_BOUND = 1000;

    /** {@code double attackrange = 16.0}. */
    public static final double ATTACK_RANGE = 16.0;

    /** The zap fires when {@code attackCounter == 20}. */
    public static final int ATTACK_INTERVAL = 20;

    /** After a zap: {@code attackCounter = -20 + rand.nextInt(20)}. */
    public static final int ATTACK_COUNTER_RESET_BASE = -20;
    public static final int ATTACK_COUNTER_RESET_BOUND = 20;

    /** A target moving at 0.1 or less on every axis counts as stationary, and
     * then hits for {@code damage + 1.0} on {@code nextFloat() < 0.66}. */
    public static final double STATIONARY_EPSILON = 0.1;
    public static final float STATIONARY_HIT_CHANCE = 0.66F;
    public static final float STATIONARY_DAMAGE_BONUS = 1.0F;

    /** A moving target is hit on {@code nextFloat() < 0.4} for the plain
     * attack-damage attribute, with no bonus. */
    public static final float MOVING_HIT_CHANCE = 0.4F;

    /** A roll that produced no damage. The original simply skips the hurt()
     * call, so the helper returns this sentinel instead of a fake 0.0 hit. */
    public static final double NO_HIT = -1.0;

    /** {@code waypoint = pos + (rand.nextFloat() * 2.0F - 1.0F) * 16.0}. */
    public static final double WAYPOINT_RANGE = 16.0;

    /** The waypoint is re-picked when its squared distance is {@code < 1.0} or
     * {@code > 3600.0} (i.e. 1..60 blocks away). */
    public static final double WAYPOINT_MIN_DIST_SQ = 1.0;
    public static final double WAYPOINT_MAX_DIST_SQ = 3600.0;

    /** {@code courseChangeCooldown += rand.nextInt(5) + 2}. */
    public static final int COURSE_CHANGE_BASE = 2;
    public static final int COURSE_CHANGE_BOUND = 5;

    /** {@code motion += delta / distance * 0.1} per axis. */
    public static final double COURSE_ACCELERATION = 0.1;

    /** {@code isCourseTraversable} looks for a non-air block at the waypoint
     * through {@code y - 0 .. y - 10} (11 samples). */
    public static final int COURSE_DROP_SAMPLES = 11;
    public static final int COURSE_DROP_MAX_INDEX = 10;

    /** {@code isValidLightLevel}: {@code skyLight > rand(32)} refuses, then
     * {@code lightLevel <= rand(8)} accepts. */
    public static final int SKY_LIGHT_ROLL_BOUND = 32;
    public static final int LIGHT_LEVEL_ROLL_BOUND = 8;

    /** {@code func_70599_aP()} = 0.25F. */
    public static final float SOUND_VOLUME = 0.25F;

    /** {@code func_70628_a}: the wisp drops 2 units of its own aspect. */
    public static final int ESSENCE_DROP = 2;

    /**
     * The original's eerie-biome switch pool, in source order:
     * {@code case 0..5} of {@code switch (rand.nextInt(6))} —
     * DARKNESS, UNDEAD, ENTROPY, ELDRITCH, POISON, DEATH.
     */
    public static final String[] EERIE_ASPECTS = {
            "DARKNESS", "UNDEAD", "ENTROPY", "ELDRITCH", "POISON", "DEATH"
    };

    private WispAiLogic() {
    }

    /** {@code switch (this.field_70146_Z.nextInt(6))} for a wisp in the eerie
     * biome — the default arm (case 5) is DEATH, as in the original. */
    public static String eerieAspectForRoll(final int roll) {
        return EERIE_ASPECTS[Math.floorMod(roll, EERIE_ASPECTS.length)];
    }

    /**
     * The original type pick, verbatim:
     * <pre>
     * if (bg == biomeEerie) switch (nextInt(6)) …;
     * else if (nextInt(10) != 0) type = random primal aspect;
     * else                        type = random compound aspect;
     * </pre>
     */
    public static String pickAspectTag(final boolean eerieBiome, final int eerieRoll,
            final int rareRoll, final int primalPick, final int compoundPick,
            final String[] primalTags, final String[] compoundTags) {
        if (eerieBiome) {
            return eerieAspectForRoll(eerieRoll);
        }
        if (rareRoll != 0) {
            return primalTags[Math.floorMod(primalPick, primalTags.length)];
        }
        return compoundTags[Math.floorMod(compoundPick, compoundTags.length)];
    }

    /** True when the current waypoint is still usable: its squared distance is
     * inside 1.0 .. 3600.0. The original re-picks the waypoint when it is not,
     * but keeps steering with the <em>old</em> deltas for the rest of the tick. */
    public static boolean waypointInRange(final double dx, final double dy, final double dz) {
        final double d3 = dx * dx + dy * dy + dz * dz;
        return d3 >= WAYPOINT_MIN_DIST_SQ && d3 <= WAYPOINT_MAX_DIST_SQ;
    }

    /** One waypoint offset from a {@code nextFloat() * 2.0F - 1.0F} roll. */
    public static double waypointOffset(final float signedRoll) {
        return signedRoll * WAYPOINT_RANGE;
    }

    /** {@code courseChangeCooldown += rand.nextInt(5) + 2}. */
    public static int nextCourseChangeCooldown(final int roll) {
        return COURSE_CHANGE_BASE + roll;
    }

    /** One axis of the original's course correction: {@code delta / d3 * 0.1}. */
    public static double steerComponent(final double delta, final double distance) {
        return delta / distance * COURSE_ACCELERATION;
    }

    /** The original's target-acquisition block for one tick. {@code attempt} is
     * the predicate result (the caller performs the search and only re-arms
     * {@link #AGGRO_COOLDOWN} when the search actually returned something,
     * exactly as the original's {@code if (targetedEntity != null)} does). */
    public record Aggro(int cooldown, boolean attempt) {
    }

    /**
     * One tick of:
     * <pre>
     * this.aggroCooldown--;
     * if (rand.nextInt(1000) == 0 &amp;&amp; (targetedEntity == null || this.aggroCooldown-- &lt;= 0)) { … }
     * </pre>
     * The post-decrement inside the condition only runs when the roll is 0
     * <em>and</em> a target already exists — short-circuit order matters, and
     * the {@code before} value is the one compared.
     */
    public static Aggro aggroTick(final int cooldown, final int roll, final boolean hasTarget) {
        int c = cooldown - 1;
        boolean attempt = false;
        if (roll == 0) {
            if (!hasTarget) {
                attempt = true;
            } else {
                final int before = c;
                c = c - 1;
                attempt = before <= 0;
            }
        }
        return new Aggro(c, attempt);
    }

    /** {@code attackCounter++ ; if (attackCounter == 20) … zap …}. */
    public static boolean atZapCadence(final int attackCounter) {
        return attackCounter == ATTACK_INTERVAL;
    }

    /** {@code attackCounter = -20 + rand.nextInt(20)} after a zap. */
    public static int nextAttackCounterAfterZap(final int roll) {
        return ATTACK_COUNTER_RESET_BASE + roll;
    }

    /** {@code !(|vx| > 0.1) &amp;&amp; !(|vy| > 0.1) &amp;&amp; !(|vz| > 0.1)} — the
     * original treats a target this slow as stationary and hits it harder. */
    public static boolean targetIsStationary(final double vx, final double vy, final double vz) {
        return Math.abs(vx) <= STATIONARY_EPSILON
                && Math.abs(vy) <= STATIONARY_EPSILON
                && Math.abs(vz) <= STATIONARY_EPSILON;
    }

    /**
     * The zap damage roll, and the one branch that surprises: a
     * <em>stationary</em> target is hit on 0.66 for {@code damage + 1.0}, a
     * moving one on 0.4 for the plain damage. Returns {@link #NO_HIT} when the
     * roll produced no damage at all.
     */
    public static double attackDamage(final boolean targetStationary, final float roll,
            final double attackDamage) {
        if (targetStationary) {
            return roll < STATIONARY_HIT_CHANCE ? attackDamage + STATIONARY_DAMAGE_BONUS : NO_HIT;
        }
        return roll < MOVING_HIT_CHANCE ? attackDamage : NO_HIT;
    }

    /** {@code isValidLightLevel}: {@code skyLight > rand(32)} refuses, then
     * {@code lightLevel <= rand(8)} accepts. */
    public static boolean isValidLightLevel(final int skyLight, final int skyRoll,
            final int blockLight, final int lightRoll) {
        return skyLight <= skyRoll && blockLight <= lightRoll;
    }

    /** {@code func_70601_bi()}: cap, difficulty, light and the super check. */
    public static boolean canSpawn(final int nearbySameType, final int difficultyId,
            final boolean validLightLevel, final boolean superCheck) {
        return nearbySameType < SPAWN_MAX_NEARBY
                && difficultyId > 0
                && validLightLevel
                && superCheck;
    }

    /**
     * {@code isCourseTraversable}: no collision along the ray from the body to
     * the waypoint, the waypoint block is not a liquid, and a non-air block is
     * found at {@code y - firstSolidBelow} for some index in 0..10 (the
     * original's 11-sample loop).
     */
    public static boolean isCourseTraversable(final int collisions,
            final boolean waypointBlockLiquid, final int firstSolidBelow) {
        return collisions == 0
                && !waypointBlockLiquid
                && firstSolidBelow >= 0
                && firstSolidBelow <= COURSE_DROP_MAX_INDEX;
    }
}
