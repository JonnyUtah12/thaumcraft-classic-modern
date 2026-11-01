package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

/**
 * Level-free rules for the taintacle family (TC-M6-06-s-e).
 *
 * <p>Every constant and branch below is read straight off the decompiled 1.7.10
 * sources; the citation in each javadoc line is the file:line in
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/}.
 * The entity classes walk state; the arithmetic and gates live here so the JUnit
 * tests can pin them without a {@code Level}.
 *
 * <p>Preserved quirks, never modernised:
 * <ul>
 *   <li>the attack sound pitch goes NEGATIVE for a full-grown taintacle
 *       (1.3 − maxHealth/10 with maxHealth 50 = −3.7, {@code EntityTaintacle.java:283-285})
 *       — the client clamps it; the formula ships verbatim;</li>
 *   <li>the attack reach is the mob's <em>max health</em> (50 blocks for a
 *       taintacle, {@code EntityTaintacle.java:154});</li>
 *   <li>the agitation window is a squared distance compared against
 *       {@code maxHealth*7 * maxHealth*7} ({@code EntityTaintacle.java:252}), i.e.
 *       a 350-block radius for a taintacle — the doubled multiplication ships verbatim;</li>
 *   <li>a taintacle is rooted: horizontal motion is zeroed and any upward motion
 *       is clamped to 0 in the movement integration hook
 *       ({@code EntityTaintacle.java:98-106});</li>
 *   <li>taking a hit from more than 16 blocks away spawns taintlets at the
 *       attacker, not at the taintacle ({@code EntityTaintacle.java:240-249}).</li>
 * </ul>
 */
public final class TaintacleAiLogic {

    /** {@code EntityTaintacle.java:30} — {@code setSize(0.66F, 3.0F)}. */
    public static final float WIDTH = 0.66F;
    /** {@code EntityTaintacle.java:30}. */
    public static final float HEIGHT = 3.0F;
    /** {@code EntityTaintacle.java:31} — {@code xpReward}. */
    public static final int XP = 10;
    /** {@code EntityTaintacle.java:60} — MAX_HEALTH. */
    public static final double HEALTH = 50.0;
    /** {@code EntityTaintacle.java:61} — ATTACK_DAMAGE. */
    public static final double ATTACK = 7.0;
    /** {@code EntityTaintacle.java:54-56} — {@code getEyeHeight() = 0.25F}. */
    public static final float EYE_HEIGHT = 0.25F;

    /** {@code EntityTaintacle.java:44} — the spawn proximity box grows by this. */
    public static final double SPAWN_SCAN_XZ = 24.0;
    /** {@code EntityTaintacle.java:44}. */
    public static final double SPAWN_SCAN_Y = 8.0;

    /** {@code EntityTaintacle.java:78} — target scan box = maxHealth * 6 / 3 / 6. */
    public static final float TARGET_SCAN_XZ_PER_HEALTH = 6.0F;
    /** {@code EntityTaintacle.java:78}. */
    public static final float TARGET_SCAN_Y_PER_HEALTH = 3.0F;

    /** {@code EntityTaintacle.java:252} — agitation radius = maxHealth * 7. */
    public static final float AGITATION_RADIUS_PER_HEALTH = 7.0F;

    /** {@code EntityTaintacle.java:157} — melee cooldown, ticks. */
    public static final int ATTACK_COOLDOWN = 20;
    /** {@code EntityTaintacle.java:220} — tentacle burst cooldown base. */
    public static final int TENTACLE_COOLDOWN_BASE = 40;
    /** {@code EntityTaintacle.java:220} — plus {@code rand.nextInt(20)}. */
    public static final int TENTACLE_COOLDOWN_JITTER = 20;
    /** {@code EntityTaintacle.java:243} — hurt-from-far tentacle burst range. */
    public static final float HURT_TENTACLE_RANGE = 16.0F;
    /** {@code EntityTaintacle.java:110} — face-the-target turn speed. */
    public static final float FACE_SPEED = 5.0F;

    /** {@code EntityTaintacle.java:117} — off-taint starve cadence. */
    public static final int STARVE_INTERVAL = 20;
    /** {@code EntityTaintacle.java:120} — {@code DamageSource.starve}, 1 heart. */
    public static final float STARVE_DAMAGE = 1.0F;

    /** {@code EntityTaintacle.java:124} — flail only starts after age &gt; maxHealth*10. */
    public static final float FLAIL_AGE_PER_HEALTH = 10.0F;
    /** {@code EntityTaintacle.java:128}. */
    public static final float FLAIL_MAX = 3.0F;
    /** {@code EntityTaintacle.java:129}/{@code :132} — ±0.2 per tick. */
    public static final float FLAIL_STEP = 0.2F;
    /** {@code EntityTaintacle.java:131} — the flail never decays below 1.0. */
    public static final float FLAIL_MIN = 1.0F;

    /** {@code EntityTaintacle.java:169} — {@code i += 3 << strengthAmp}. */
    public static final int STRENGTH_BONUS_PER_LEVEL = 3;
    /** {@code EntityTaintacle.java:173} — {@code i -= 2 << weaknessAmp}. */
    public static final int WEAKNESS_PENALTY_PER_LEVEL = 2;
    /** {@code EntityTaintacle.java:188} — knockback y. */
    public static final double KNOCKBACK_Y = 0.1;
    /** {@code EntityTaintacle.java:186}/{@code :188} — knockback xz scale. */
    public static final float KNOCKBACK_XZ_SCALE = 0.5F;
    /** {@code EntityTaintacle.java:190-191} — self drag after a knockback hit. */
    public static final double SELF_DRAG_AFTER_KNOCKBACK = 0.6;
    /** {@code EntityTaintacle.java:196} — fire aspect seconds = level * 4. */
    public static final int FIRE_SECONDS_PER_LEVEL = 4;

    /** {@code EntityTaintacle.java:287-289} — sound volume = maxHealth / 8. */
    public static final float SOUND_VOLUME_PER_HEALTH = 8.0F;
    /** {@code EntityTaintacle.java:283-285} — sound pitch = 1.3 − maxHealth / 10. */
    public static final float SOUND_PITCH_BASE = 1.3F;
    /** {@code EntityTaintacle.java:283-285}. */
    public static final float SOUND_PITCH_PER_HEALTH = 10.0F;

    /** {@code EntityTaintacleSmall.java:14} — box of a taintlet. */
    public static final float SMALL_WIDTH = 0.22F;
    /** {@code EntityTaintacleSmall.java:14}. */
    public static final float SMALL_HEIGHT = 1.0F;
    /** {@code EntityTaintacleSmall.java:15} — a taintlet is worth no experience. */
    public static final int SMALL_XP = 0;
    /** {@code EntityTaintacleSmall.java:21}. */
    public static final double SMALL_HEALTH = 8.0;
    /** {@code EntityTaintacleSmall.java:22}. */
    public static final double SMALL_ATTACK = 2.0;
    /** {@code EntityTaintacleSmall.java:10} — 200 ticks, then it dies of old age. */
    public static final int SMALL_LIFETIME = 200;
    /** {@code EntityTaintacleSmall.java:29} — {@code DamageSource.magic}, 10 damage. */
    public static final float SMALL_LIFETIME_DAMAGE = 10.0F;
    /** {@code EntityTaintacleSmall.java:28} — the lifetime check uses {@code lifetime-- <= 0}. */
    public static final int SMALL_LIFETIME_START = SMALL_LIFETIME;

    /** {@code EntityTaintacle.java:235} — spread fibres meta: 1 in 4 is grass. */
    public static final int FIBRES_SPREAD_GRASS_ODDS = 4;

    private TaintacleAiLogic() {
    }

    /** {@code EntityTaintacle.java:283-285}. */
    public static float soundPitch(final float maxHealth) {
        return SOUND_PITCH_BASE - maxHealth / SOUND_PITCH_PER_HEALTH;
    }

    /** {@code EntityTaintacle.java:287-289}. */
    public static float soundVolume(final float maxHealth) {
        return maxHealth / SOUND_VOLUME_PER_HEALTH;
    }

    /**
     * {@code EntityTaintacle.java:251-253} — the agitation window. The original
     * compares a squared distance against {@code (maxHealth*7) * (maxHealth*7)};
     * with no target it is false.
     */
    public static boolean agitated(final boolean hasTarget, final double distanceSquared, final float maxHealth) {
        if (!hasTarget) {
            return false;
        }
        final float r = maxHealth * AGITATION_RADIUS_PER_HEALTH;
        return distanceSquared < r * r;
    }

    /**
     * {@code EntityTaintacle.java:152-164} — the melee branch.
     *
     * @param attackTime     the original's {@code field_70724_aR} cooldown counter
     * @param distance       {@code getDistanceToEntity(target)}
     * @param maxHealth      the reach IS the max health ({@code :154})
     * @param verticalOverlap target AABB overlaps the taintacle's vertically
     *                        ({@code :155-156}: target.maxY &gt; self.minY &amp;&amp; target.minY &lt; self.maxY)
     */
    public static boolean canMelee(final int attackTime, final float distance, final float maxHealth,
            final boolean verticalOverlap) {
        return attackTime <= 0
                && distance <= maxHealth
                && verticalOverlap;
    }

    /**
     * {@code EntityTaintacle.java:160-162} — the far branch: out of melee reach,
     * the target is standing on ground, and the mob is not a taintlet.
     */
    public static boolean wantsTentacleBurst(final int attackTime, final float distance, final float maxHealth,
            final boolean targetOnGround, final boolean isSmall) {
        return attackTime <= 0 && distance > maxHealth && targetOnGround && !isSmall;
    }

    /** {@code EntityTaintacle.java:220} — cooldown after spawning taintlets. */
    public static int tentacleCooldown(final RandomSource random) {
        return TENTACLE_COOLDOWN_BASE + random.nextInt(TENTACLE_COOLDOWN_JITTER);
    }

    /**
     * {@code EntityTaintacle.java:166-181} — base attack damage plus the
     * strength/weakness shift plus the attacker's weapon enchantments
     * (sharpness/smite/bane, knockback, fire aspect are applied by the caller).
     */
    public static float meleeDamage(final float attributeDamage, final int strengthAmplifier,
            final int weaknessAmplifier, final int weaponEnchantmentBonus) {
        float damage = attributeDamage;
        if (strengthAmplifier >= 0) {
            damage += STRENGTH_BONUS_PER_LEVEL << strengthAmplifier;
        }
        if (weaknessAmplifier >= 0) {
            damage -= WEAKNESS_PENALTY_PER_LEVEL << weaknessAmplifier;
        }
        return damage + weaponEnchantmentBonus;
    }

    /** {@code EntityTaintacle.java:186-188} — knockback x from the taintacle's yaw. */
    public static double knockbackX(final float yawDegrees, final int knockbackLevel) {
        return -Math.sin(yawDegrees * (float) Math.PI / 180.0F) * knockbackLevel * KNOCKBACK_XZ_SCALE;
    }

    /** {@code EntityTaintacle.java:188} — knockback z from the taintacle's yaw. */
    public static double knockbackZ(final float yawDegrees, final int knockbackLevel) {
        return Math.cos(yawDegrees * (float) Math.PI / 180.0F) * knockbackLevel * KNOCKBACK_XZ_SCALE;
    }

    /** {@code EntityTaintacle.java:116-121} — the off-taint starve tick. */
    public static boolean starves(final int tickCount, final boolean onTaint) {
        return !onTaint && tickCount % STARVE_INTERVAL == 0;
    }

    /**
     * {@code EntityTaintacle.java:123-133} — the client-side flail ramp.
     * Agitated = the mob is old enough AND (hurt, attacked, or a target is inside
     * melee reach). The intensity walks to 3.0 while agitated, back to 1.0 while not.
     */
    public static float nextFlailIntensity(final float current, final int tickCount, final float maxHealth,
            final boolean hurtOrAttacked, final boolean targetInMeleeReach) {
        final boolean agitated = tickCount > maxHealth * FLAIL_AGE_PER_HEALTH
                && (hurtOrAttacked || targetInMeleeReach);
        if (agitated) {
            return current < FLAIL_MAX ? current + FLAIL_STEP : current;
        }
        return current > FLAIL_MIN ? current - FLAIL_STEP : current;
    }

    /**
     * {@code EntityTaintacle.java:135-137} — the arise effect plays while the mob
     * is younger than {@code maxHealth * 10} ticks and standing on ground.
     */
    public static boolean playsAriseEffect(final int tickCount, final float maxHealth, final boolean onGround) {
        return tickCount < maxHealth * FLAIL_AGE_PER_HEALTH && onGround;
    }

    /**
     * {@code EntityTaintacle.java:34-51} — the spawn gate: no taintacle inside the
     * 24x8x24 box, the block under the mob is taint fibres meta 0 or taint meta 1,
     * the biome is taint, and vanilla mob spawning agrees.
     */
    public static boolean canSpawnHere(final int taintaclesInBox, final boolean standingOnTaint,
            final boolean inTaintBiome, final boolean vanillaAllows) {
        return taintaclesInBox <= 0 && standingOnTaint && inTaintBiome && vanillaAllows;
    }

    /** {@code EntityTaintacle.java:40-45} — the proximity scan box. */
    public static AABB spawnScanBox(final double x, final double y, final double z) {
        return new AABB(x, y, z, x, y, z).inflate(SPAWN_SCAN_XZ, SPAWN_SCAN_Y, SPAWN_SCAN_XZ);
    }

    /** {@code EntityTaintacle.java:74-96} — the target scan box. */
    public static AABB targetScanBox(final double x, final double y, final double z, final float maxHealth) {
        return new AABB(x, y, z, x, y, z)
                .inflate(maxHealth * TARGET_SCAN_XZ_PER_HEALTH,
                        maxHealth * TARGET_SCAN_Y_PER_HEALTH,
                        maxHealth * TARGET_SCAN_XZ_PER_HEALTH);
    }

    /** {@code EntityTaintacle.java:87} — a taint mob is never a taintacle's target. */
    public static boolean validTarget(final boolean isTaintedMob) {
        return !isTaintedMob;
    }

    /**
     * {@code EntityTaintacle.java:241-245} — being hit from out of melee range
     * (and not being a taintlet, and not on the client) bursts taintlets at the attacker.
     */
    public static boolean burstsTentaclesWhenHurt(final boolean isSmall, final boolean hasAttacker,
            final float distanceToAttacker, final boolean clientSide) {
        return !isSmall && hasAttacker && distanceToAttacker > HURT_TENTACLE_RANGE && !clientSide;
    }

    /** {@code EntityTaintacle.java:235} — the fibres meta written by the spread. */
    public static int spreadFibresMeta(final RandomSource random) {
        return random.nextInt(FIBRES_SPREAD_GRASS_ODDS) == 0 ? 1 : 0;
    }

    /**
     * {@code EntityTaintacle.java:299-307} — death loot: a coin flip between
     * tainted goo (itemResource meta 11) and tainted fungus (meta 12).
     *
     * @return true for goo, false for fungus
     */
    public static boolean rollsGoo(final RandomSource random) {
        return random.nextBoolean();
    }

    /** {@code EntityTaintacle.java:301} — drop offset = maxHealth / 2. */
    public static float dropOffset(final float maxHealth) {
        return maxHealth / 2.0F;
    }

    /** itemResource meta 11 — tainted goo ({@code EntityTaintacle.java:301}). */
    public static final int META_TAINTED_GOO = 11;
    /** itemResource meta 12 — tainted fungus ({@code EntityTaintacle.java:303}). */
    public static final int META_TAINTED_FUNGUS = 12;

    /** {@code EntityTaintacle.java:199} — the caller fires fire-aspect burn at level * 4. */
    public static int fireSeconds(final int fireAspectLevel) {
        return fireAspectLevel * FIRE_SECONDS_PER_LEVEL;
    }
}
