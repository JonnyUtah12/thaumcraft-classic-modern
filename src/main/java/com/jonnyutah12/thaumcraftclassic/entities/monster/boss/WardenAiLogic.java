package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The rules of {@code thaumcraft.common.entities.monster.boss.EntityEldritchWarden}
 * (418 lines), Level-free so every figure is pinned by
 * {@code WardenAiLogicTest} without a world (the family's AiLogic pattern —
 * {@code ThaumcraftBossAiLogic}, {@code CultistLeaderAiLogic},
 * {@code EldritchGolemAiLogic}).
 *
 * <p>Every line citation below is the 1.7.10 decompile at
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityEldritchWarden.java}
 * unless the file is named. The registration row is {@code ConfigEntities.java:109} —
 * {@code registerModEntity(EntityEldritchWarden.class, "EldritchWarden", 26, instance, 64, 3, true)}.</p>
 *
 * <p>The card's custody note calls the warden "built on the taintacle-giant family";
 * the measured hierarchy ({@code :43}) is
 * {@code extends EntityThaumcraftBoss implements IRangedAttackMob, IEldritchMob} —
 * the Praetor/Golem base, not the taintacle. The port follows the reference.</p>
 */
public final class WardenAiLogic {

    private WardenAiLogic() {
    }

    // ------------------------------------------------- registration (:80; ConfigEntities.java:109)

    /** {@code :80} — {@code func_70105_a(1.5F, 3.5F)}: the warden's box. */
    public static final float WIDTH = 1.5F;
    public static final float HEIGHT = 3.5F;

    /**
     * {@code ConfigEntities.java:109} — tracking 64 blocks (1.21.1 counts chunks: 4),
     * update interval 3 ticks, velocity updates true. The family triple (the leader's
     * same 64/3/true at {@code ConfigEntities.java:115}).
     */
    public static final int TRACK_RANGE_BLOCKS = 64;
    public static final int TRACK_RANGE_CHUNKS = TRACK_RANGE_BLOCKS / 16;
    public static final int UPDATE_INTERVAL = 3;
    public static final boolean VELOCITY_UPDATES = true;

    /** {@code :415-417} — {@code func_70627_aG() = 500}: overwrites the boss base's 50. */
    public static final int EXPERIENCE = 500;

    // ------------------------------------------------- attributes (:104-106, :127-129, :336-338)

    /** {@code :104-106} — maxHealth 200.0, movement speed 0.33, attack 10.0. */
    public static final double MAX_HEALTH = 200.0;
    public static final double MOVEMENT_SPEED = 0.33;
    public static final double ATTACK_DAMAGE = 10.0;

    /**
     * {@code :127-129} — {@code func_70658_aO() = super + 4}: 1.21.1's armour is the
     * {@code ARMOR} attribute over the base's 0 (the golem lane's shape, BD-137's
     * attribute pair — {@code EldritchGolemAiLogic.ARMOR_BONUS}).
     */
    public static final double ARMOR_BONUS = 4.0;

    /**
     * {@code :336-338} — {@code func_70047_e() = 3.1F} (eye height); 1.21.1 derives it
     * from the box, so it ships as {@code EntityDimensions.withEyeHeight}.
     */
    public static final float EYE_HEIGHT = 3.1F;

    /** The warden's attribute pass: the boss base's pair plus the warden's four. */
    public static AttributeSupplier.Builder createAttributes() {
        return ThaumcraftBossAiLogic.onMonsterAttributes(
                        net.minecraft.world.entity.monster.Monster.createMonsterAttributes())
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)                        // :105
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)                                 // :104
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)                           // :106
                .add(Attributes.ARMOR, ARMOR_BONUS);                                    // :127-129
    }

    // ------------------------------------------------- titles (:44-60, :83-99, :115-125)

    /** {@code :44-60} — the fifteen titles, in the original array order. */
    public static final String[] TITLES = {
        "Aphoom-Zhah", "Basatan", "Chaugnar Faugn", "Mnomquah", "Nyogtha",
        "Oorn", "Shaikorth", "Rhan-Tegoth", "Rhogog", "Shudde M'ell",
        "Vulthoom", "Yag-Kosha", "Yibb-Tstll", "Zathog", "Zushakon",
    };

    /** {@code :118} — the NBT key carrying the watched title byte. */
    public static final String NBT_TITLE = "title";

    /** {@code :93-95} — the title the watched byte selects. */
    public static String titleAt(final int watchedByte) {
        return TITLES[watchedByte];
    }

    /**
     * {@code :84-91} — {@code generateName()}: a name is set ONLY when the
     * {@code tc.mobmod} champion attribute reads {@code >= 0}. That attribute is
     * NOT registered in this port (the leader row's pair), and 21.1.259 has no
     * {@code generateName} seam at all, so a fresh warden carries no custom name
     * and the plain lang label shows; the rule ships pinned and the probe asserts
     * the plain name (the leader's BD-101/103 shape).
     */
    public static boolean championNameApplies(final int championModIndex) {
        return championModIndex >= 0;                                                   // :86
    }

    // ------------------------------------------------- spawn (:328-334, :145-149)

    /** {@code :330} — {@code spawnTimer = 150} on first spawn (the materialisation). */
    public static final int SPAWN_TIMER_START_TICKS = 150;

    /** {@code :147} — the status-18 broadcast fires while the timer reads 150. */
    public static final int SPAWN_TIMER_BROADCAST_TICKS = 150;

    /** {@code :332} — the spawn heal: current health + {@code maxHealth * 0.66}. */
    public static final double SPAWN_HEAL_FRACTION = 0.66;

    /** {@code :332} — the spawn heal sum (the platform's {@code setHealth} clamps). */
    public static float spawnHealthFor(final float health, final double maxHealth) {
        return (float) (health + maxHealth * SPAWN_HEAL_FRACTION);                      // :332
    }

    // ------------------------------------------------- status bytes (:382-396)

    /** {@code :384-392} — 15 left arm, 16 right arm, 17 both, 18 materialisation. */
    public static final byte STATUS_ARM_LEFT = 15;
    public static final byte STATUS_ARM_RIGHT = 16;
    public static final byte STATUS_ARM_BOTH = 17;
    public static final byte STATUS_SPAWN = 18;

    /** {@code :385/:387} — a single-arm lift is 0.5; {@code :389-390} both arms 0.9. */
    public static final float ARM_LIFT_HALF = 0.5F;
    public static final float ARM_LIFT_FULL = 0.9F;

    /** {@code :154/:158} — the client decay of an arm lift, per tick. */
    public static final float ARM_LIFT_DECAY = 0.05F;

    /** {@code :153-159} — the decay walks: a lift above 0 loses the step, else stays. */
    public static float armLiftAfterTick(final float lift) {
        return lift > 0.0F ? lift - ARM_LIFT_DECAY : lift;
    }

    /** {@code :391-392} — status 18 re-arms the client's materialisation timer. */
    public static int spawnTimerForStatus(final byte status) {
        return status == STATUS_SPAWN ? SPAWN_TIMER_START_TICKS : SPAWN_TIMER_MIN;
    }

    /** The idle value of the spawn timer (the family's constant). */
    public static final int SPAWN_TIMER_MIN = 0;

    // ------------------------------------------------- the field frenzy (:200, :208-245, :317-319)

    /** {@code :319/:200} — the counter enters at 150 and the teleport fires at 150. */
    public static final int FRENZY_START_TICKS = 150;

    /** {@code :209} — the ring leg runs while the counter is under 121… */
    public static final int FRENZY_RING_CEILING = 121;

    /** {@code :209} — …and every tenth tick of it. */
    public static final int FRENZY_RING_EVERY_TICKS = 10;

    /** {@code :211} — ring radius: {@code (150 - counter) / 8.0}. */
    public static final double FRENZY_RADIUS_SPAN = 150.0;
    public static final double FRENZY_RADIUS_DIVISOR = 8.0;

    /** {@code :212} — the sweep step: {@code 1 + counter / 8}. */
    public static final int FRENZY_STEP_DIVISOR = 8;

    /** {@code :217-218} — the half-circle sweep: {@code q < 180 / d}, at {@code q * 2 * d}°. */
    public static final int FRENZY_SWEEP_DEGREES = 180;
    public static final int FRENZY_SWEEP_STEP_MULTIPLIER = 2;

    /** {@code :209} — the ring tick: counter under 121 and a tenth boundary. */
    public static boolean broadcastsFieldFrenzy(final int counter) {
        return counter < FRENZY_RING_CEILING && counter % FRENZY_RING_EVERY_TICKS == 0;
    }

    /** {@code :211} — the expanding radius at this counter. */
    public static double frenzyRadius(final int counter) {
        return (FRENZY_RADIUS_SPAN - counter) / FRENZY_RADIUS_DIVISOR;
    }

    /** {@code :212} — the angular step divisor at this counter. */
    public static int frenzyStep(final int counter) {
        return 1 + counter / FRENZY_STEP_DIVISOR;
    }

    /** {@code :217} — the loop bound for one sweep. */
    public static int frenzyLoopLimit(final int step) {
        return FRENZY_SWEEP_DEGREES / step;
    }

    /** {@code :218} — the angle of sample {@code q}, in radians. */
    public static double frenzyRadians(final int q, final int step) {
        return Math.toRadians(q * FRENZY_SWEEP_STEP_MULTIPLIER * step);
    }

    /** {@code :219-220} — the ring deltas (int truncation, verbatim). */
    public static int frenzyDeltaX(final double radius, final double radians) {
        return (int) (radius * Math.cos(radians));
    }

    public static int frenzyDeltaZ(final double radius, final double radians) {
        return (int) (radius * Math.sin(radians));
    }

    /** {@code :317-319} — the death swap: a landed killing hit with no frenzy yet. */
    public static boolean frenzySwaps(final boolean damageApplied, final boolean frenzy,
            final float healthAfter) {
        return damageApplied && !frenzy && healthAfter <= 0.0F;
    }

    /** {@code :309-310} — invulnerable through the whole frenzy window. */
    public static boolean invulnerableForFrenzy(final int frenzyCounter) {
        return frenzyCounter > 0;
    }

    /**
     * The swap's health pin: 1.7.10's boss base overrides the death-tick body with
     * an EMPTY one (the family's map), so the original warden loiters at 0.0F
     * health through the frenzy. 21.1.259 has no such seam and the vanilla death
     * pass removes a 0-health mob at deathTime 20 ({@code LivingEntity.java:565-571}),
     * so the port pins the swap health to 1.0F and records the deviation (BD row).
     */
    public static final float FRENZY_SWAP_HEALTH = 1.0F;

    // ------------------------------------------------- regen (:137-142)

    /** {@code :137} — the 25-tick cadence, gated by no hurt-resistance. */
    public static final int REGEN_INTERVAL_TICKS = 25;

    /** {@code :138} — regen stops at {@code (int)(maxHealth * 0.66)}. */
    public static final double REGEN_CAP_FRACTION = 0.66;

    /** {@code :140} — one health per regen tick. */
    public static final float REGEN_AMOUNT = 1.0F;

    /** {@code :137} — the cadence. */
    public static boolean regens(final int tickCount, final int hurtResistantTime) {
        return hurtResistantTime <= 0 && tickCount % REGEN_INTERVAL_TICKS == 0;
    }

    /** {@code :138} — the cap. */
    public static int regenCap(final double maxHealth) {
        return (int) (maxHealth * REGEN_CAP_FRACTION);
    }

    // ------------------------------------------------- goals (:69-79)

    public static final int GOAL_SLOT_SWIM = 0;                                         // :70
    public static final int GOAL_SLOT_RANGED = 2;                                       // :71
    public static final int GOAL_SLOT_MELEE = 3;                                        // :72
    public static final int GOAL_SLOT_RESTRICTION = 5;                                  // :73
    public static final int GOAL_SLOT_WANDER = 7;                                       // :74
    public static final int GOAL_SLOT_WATCH = 8;                                        // :75
    public static final int GOAL_SLOT_LOOK_IDLE = 8;                                    // :76
    public static final int TARGET_SLOT_HURT = 1;                                       // :77
    public static final int TARGET_SLOT_PLAYER = 2;                                     // :78
    public static final int TARGET_SLOT_CULTIST = 3;                                    // :79

    /**
     * {@code :69} — {@code func_70661_as().func_75498_b(true)}: the flying path
     * navigation opens doors; 1.21.1's twin is {@code NodeEvaluator#setCanOpenDoors}.
     */
    public static final boolean NAVIGATION_OPENS_DOORS = true;

    /**
     * {@code :71} — {@code AILongRangeAttack(this, 3.0, 1.0, 20, 40, 24.0F)}:
     * Thaumcraft's own goal class (the cleric lane's port), NOT vanilla's
     * {@code RangedAttackGoal}.
     */
    public static final double RANGED_MIN_DISTANCE = 3.0;
    public static final double RANGED_SPEED = 1.0;
    public static final int RANGED_INTERVAL_MIN = 20;
    public static final int RANGED_INTERVAL_MAX = 40;
    public static final float RANGED_RANGE = 24.0F;

    /** {@code :72} — {@code AIAttackOnCollide(this, EntityLivingBase.class, 1.1, false)}. */
    public static final double MELEE_SPEED = 1.1;
    public static final boolean MELEE_LONG_MEMORY = false;

    /** {@code :73-74} — restriction 0.8, wander 1.0. */
    public static final double RESTRICTION_SPEED = 0.8;
    public static final double WANDER_SPEED = 1.0;

    /**
     * {@code :75} — {@code EntityAIWatchClosest(this, EntityPlayer.class, 8.0F)}; the
     * twin {@code LookAtPlayerGoal} takes the family's watch probability (the leader's
     * pair at {@code CultistLeaderAiLogic.WATCH_PLAYER_PROBABILITY}).
     */
    public static final float WATCH_PLAYER_RANGE = 8.0F;
    public static final float WATCH_PLAYER_PROBABILITY =
            com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic
                    .WATCH_PLAYER_PROBABILITY;

    /**
     * {@code :78-79} — {@code EntityAINearestAttackableTarget(this, X.class, 0, true)}:
     * the trailing {@code true} is {@code mustSee}, the family's shipped flag.
     */
    public static final boolean TARGET_MUST_SEE = true;

    /**
     * {@code :132-135} — the goals tick ONLY while the frenzy counter is idle. The
     * 1.7.10 body wraps the whole goal pass; 1.21.1's goal pass
     * ({@code Mob#serverAiStep}) is final, so the gate rides the family's seam —
     * {@code EntityThaumcraftBoss.goalsMayTick()}, consulted by
     * {@code ai/boss/AIAttackOnCollide.canUse} (BD-132's shipped shape).
     */
    public static boolean goalsMayTick(final int spawnTimer, final int frenzyCounter) {
        return spawnTimer == SPAWN_TIMER_MIN && frenzyCounter == 0;                     // :133 + :63
    }

    // ------------------------------------------------- damage (:313-326, :308-311)

    /**
     * {@code :315} — the pair of sources the warden never takes: 1.7.10's
     * {@code DamageSource.field_76369_e} = drown and {@code field_82727_n} =
     * wither (resolved the way {@code RunicShielding.isBlockedSource} resolved the
     * same fields; the BOSS_PREP's "anvil/cactus pair" guess is superseded by the
     * measured resolution, recorded in the BD row).
     */
    public static boolean refusesSource(final boolean invulnerable,
            final boolean drown, final boolean wither) {
        return invulnerable || drown || wither;                                         // :315
    }

    // ------------------------------------------------- orb seam (:340-355)

    /** {@code :341} — {@code rand.nextFloat() > 0.2F}: 80% of shots fire the orb. */
    public static final float ORB_ROLL_FLOOR = 0.2F;

    /** {@code :341} — the branch. */
    public static boolean firesOrb(final float roll) {
        return roll > ORB_ROLL_FLOOR;
    }

    /** {@code :343-345} — the alternating last-blast toggle; right arm at 90°, left at 180°. */
    public static final int MUZZLE_DEGREES_RIGHT = 90;
    public static final int MUZZLE_DEGREES_LEFT = 180;

    /** {@code :345} — the muzzle degrees of this shot. */
    public static int muzzleDegrees(final boolean lastBlast) {
        return lastBlast ? MUZZLE_DEGREES_RIGHT : MUZZLE_DEGREES_LEFT;
    }

    /** {@code :346-348} — the muzzle offsets: 0.5 on the yaw circle, a flat 0.13 drop. */
    public static final double MUZZLE_RADIUS = 0.5;
    public static final double MUZZLE_Y_DROP = 0.13;

    /** {@code :353} — {@code func_70186_c(d0, d1, d2, 1.0F, 2.0F)}: speed 1.0, inaccuracy 2.0. */
    public static final float ORB_SPEED = 1.0F;
    public static final float ORB_INACCURACY = 2.0F;

    /** {@code :354} — {@code thaumcraft:egattack} at volume 2.0, pitch 1.0 + rand*0.1. */
    public static final float ATTACK_SOUND_VOLUME = 2.0F;
    public static final float SOUND_PITCH_BASE = 1.0F;
    public static final float SOUND_PITCH_JITTER = 0.1F;

    // ------------------------------------------------- sonic seam (:356-379)

    /** {@code :362-366} — the knockback: ±1.5 along the yaw, 0.1 up. */
    public static final double SONIC_KNOCK_MAGNITUDE = 1.5;
    public static final double SONIC_KNOCK_UP = 0.1;

    public static double sonicKnockX(final float yawDegrees) {
        return -Math.sin(Math.toRadians(yawDegrees)) * SONIC_KNOCK_MAGNITUDE;           // :363
    }

    public static double sonicKnockZ(final float yawDegrees) {
        return Math.cos(Math.toRadians(yawDegrees)) * SONIC_KNOCK_MAGNITUDE;            // :365
    }

    /**
     * {@code :369-370} — {@code Potion.field_82731_v} (wither) and
     * {@code Potion.field_76437_t} (weakness), both 400 ticks at amplifier 0
     * (the fields csv: wither / weakness).
     */
    public static final int SONIC_WITHER_TICKS = 400;
    public static final int SONIC_WITHER_AMPLIFIER = 0;
    public static final int SONIC_WEAKNESS_TICKS = 400;
    public static final int SONIC_WEAKNESS_AMPLIFIER = 0;

    /** {@code :375} — sticky warp {@code 3 + worldRandom.nextInt(3)} (the WORLD random). */
    public static final int SONIC_WARP_BASE = 3;
    public static final int SONIC_WARP_JITTER = 3;

    /** {@code :378} — {@code thaumcraft:egscreech} at volume 4.0, pitch 1.0 + rand*0.1. */
    public static final float SCREECH_VOLUME = 4.0F;

    // ------------------------------------------------- the corner cells (:184-197)

    /** {@code :190} — the four corners of the warden's centre cell. */
    public static final int CORNER_SCAN_CELLS = 4;

    /** {@code :191-193} — the corner offsets: x on {@code l % 2}, z on {@code l / 2 % 2}. */
    public static final float CORNER_OFFSET = 0.25F;

    public static float cornerOffsetX(final int l) {
        return (l % 2 * 2 - 1) * CORNER_OFFSET;                                         // :191
    }

    public static float cornerOffsetZ(final int l) {
        return (l / 2 % 2 * 2 - 1) * CORNER_OFFSET;                                     // :193
    }

    /** {@code :195/:222} — the airy variant the warden writes: meta 11. */
    public static final int AIRY_PORTAL_META = 11;

    // ------------------------------------------------- the frenzy ring (:208-245)

    /** {@code :223} — the scheduled update: {@code 250 + rand.nextInt(150)} ticks. */
    public static final int AIRY_UPDATE_BASE_TICKS = 250;
    public static final int AIRY_UPDATE_JITTER_TICKS = 150;

    public static int airyUpdateDelay(final int roll) {
        return AIRY_UPDATE_BASE_TICKS + roll;                                           // :223
    }

    /** {@code :224} — a 0.3 roll picks the arc packet; the rest take the sparkle. */
    public static final float ARC_ROLL_CEILING = 0.3F;

    public static boolean arcFires(final float roll) {
        return roll < ARC_ROLL_CEILING;                                                 // :224
    }

    /** {@code :240-241} — {@code thaumcraft:zap} at volume 1.0, pitch 0.9 + rand*0.1. */
    public static final float ZAP_VOLUME = 1.0F;
    public static final float ZAP_PITCH_BASE = 0.9F;
    public static final float ZAP_PITCH_JITTER = 0.1F;

    // ------------------------------------------------- teleport home (:247-306)

    /** {@code :264} — the landing search gets 20 tries. */
    public static final int TELEPORT_TRIES = 20;

    /** {@code :272-273} — each retry jitters x/z by {@code nextInt(8) - nextInt(8)}. */
    public static final int TELEPORT_JITTER_ROLL = 8;

    public static int jitteredCell(final int baseCell, final int rollA, final int rollB) {
        return baseCell + rollA - rollB;                                                // :272/:273
    }

    /** {@code :279} — the set position: cell centre + 0.5, cell floor + 0.1. */
    public static final double TELEPORT_CENTER_OFFSET = 0.5;
    public static final double TELEPORT_Y_OFFSET = 0.1;

    /** {@code :269} — the landing cell needs a solid below and a non-solid cell. */
    public static boolean landingCellAccepts(final boolean belowSolid, final boolean cellSolid) {
        return belowSolid && !cellSolid;                                                // :269
    }

    /** {@code :289-300} — the portal trail: 128 particles (the FX lane's draw). */
    public static final int TELEPORT_PORTAL_PARTICLE_COUNT = 128;

    // ------------------------------------------------- faction (:403-405)

    /**
     * {@code :403-405} — {@code func_70686_a(Class)}: the warden never targets the
     * Guardian class; everything else takes the base's answer. 1.21.1's twin is
     * {@code Mob#canAttackType(EntityType)} (the leader's pair at
     * {@code EntityCultistLeader:325-333}) — the exact-class write compares the
     * registered types.
     */
    public static boolean refusesTargetType(final boolean guardianType) {
        return guardianType;                                                            // :404
    }
}
