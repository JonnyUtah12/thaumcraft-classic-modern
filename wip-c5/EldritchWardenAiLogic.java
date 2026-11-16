package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The rules of {@code thaumcraft.common.entities.monster.boss.EntityEldritchWarden}
 * (418 lines) — the frenzy death-fake-out machine — Level-free and pinned by
 * {@code src/test/java/.../entities/monster/boss/EldritchWardenAiLogicTest.java}.
 *
 * <p>Citations are the 1.7.10 decompile
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityEldritchWarden.java}.</p>
 */
public final class EldritchWardenAiLogic {

    private EldritchWardenAiLogic() {
    }

    /** {@code :80} — {@code super(level, 1.5F, 3.5F)}. */
    public static final float WIDTH = 1.5F;
    public static final float HEIGHT = 3.5F;
    /** {@code :336} — eye height 3.1. */
    public static final float EYE_HEIGHT = 3.1F;
    /** {@code :127} — {@code getArmor() = super + 4}. */
    public static final int ARMOUR_BONUS = 4;

    /** {@code :104-106} — maxHealth 200.0, speed 0.33, attack 10.0. */
    public static final double MAX_HEALTH = 200.0;
    public static final double MOVEMENT_SPEED = 0.33;
    public static final double ATTACK_DAMAGE = 10.0;

    /** {@code :415} — {@code getExperiencePoints} 500. */
    public static final int EXPERIENCE_POINTS = 500;

    /** {@code :328-334} — {@code onInitialSpawn}: {@code spawnTimer = 150}, title roll, +66 % health. */
    public static final int SPAWN_TIMER = 150;
    public static final float SPAWN_HEAL_FRACTION = 0.66F;
    public static final int STATUS_SPAWN = 18;

    /** {@code :44-60} — the 15 eldritch titles, verbatim. */
    public static final String[] TITLES = {
        "Aphoom-Zhah", "Apocaem", "Botis", "Chax", "Ehecatl", "Ib", "Kasck", "Mors",
        "Naberius", "Paimon", "Raove", "Zephar", "Vine", "Verchine", "Zushakon",
    };
    /** {@code :110-113} — datawatcher index 16, a {@code Byte}: the title. */
    public static final int TITLE_WATCHER_INDEX = 16;
    /** {@code :116-125} — the NBT key. */
    public static final String NBT_TITLE = "title";

    public static int titleFor(final int roll) {
        return Math.floorMod(roll, TITLES.length);                                      // :331
    }

    public static String nameFor(final int title) {
        return TITLES[titleFor(title)];                                                 // :113
    }

    // ---------------------------------------------------------------- goals (:70-79)

    public static final int GOAL_SWIM = 0;                                              // :70
    public static final int GOAL_RANGED = 2;                                            // :71
    public static final int GOAL_MELEE = 3;                                             // :72
    public static final int GOAL_RESTRICTION = 5;                                       // :73
    public static final int GOAL_WANDER = 7;                                            // :74
    public static final int GOAL_WATCH = 8;                                             // :75
    public static final int GOAL_LOOK_IDLE = 8;                                         // :76
    public static final int TARGET_HURT = 1;                                            // :77
    public static final int TARGET_PLAYER = 2;                                          // :78
    public static final int TARGET_CULTIST = 3;                                         // :79

    /** {@code :71} — {@code AILongRangeAttack(this, 3.0, 1.0, 20, 40, 24.0F)}. */
    public static final double RANGED_MIN_DISTANCE = 3.0;
    public static final double RANGED_SPEED = 1.0;
    public static final int RANGED_INTERVAL_MIN = 20;
    public static final int RANGED_INTERVAL_MAX = 40;
    public static final float RANGED_RADIUS = 24.0F;

    /** {@code :72} — {@code AIAttackOnCollide(EntityLivingBase, 1.1, false)}. */
    public static final double MELEE_SPEED = 1.1;
    public static final boolean MELEE_LONG_MEMORY = false;
    /** {@code :73-74} — restriction 0.8, wander 1.0 (the warden's is the fast one). */
    public static final double RESTRICTION_SPEED = 0.8;
    public static final double WANDER_SPEED = 1.0;
    public static final float WATCH_PLAYER_RANGE = 8.0F;                                // :75
    /** {@code :77} — {@code EntityAIHurtByTarget(this, false)}. */
    public static final boolean HURT_IS_AGRESSORS = false;
    public static final int TARGET_SORT_TYPE = 0;                                       // :78-79
    public static final boolean TARGET_ALWAYS_CLOSE = true;                             // :78-79
    /** {@code :69} — {@code getNavigator().setCanOpenDoors(true)}. */
    public static final boolean CAN_OPEN_DOORS = true;

    // ---------------------------------------------------------------- frenzy (:184-326)

    /** {@code :131-143} — the goals run ONLY while the frenzy counter is 0. */
    public static boolean goalsRun(final int frenzyCounter) {
        return frenzyCounter == 0;                                                      // :133
    }

    /** {@code :135-141} — regeneration every 25 ticks, only out of hurt-resistance, up to 66 %. */
    public static final int REGEN_PERIOD = 25;
    public static final float REGEN_CAP_FRACTION = 0.66F;

    public static boolean regens(final int tick, final int hurtResistantTime,
            final float health, final double maxHealth) {
        return tick % REGEN_PERIOD == 0 && hurtResistantTime <= 0                        // :136-137
                && health < maxHealth * REGEN_CAP_FRACTION;                             // :139
    }

    /** {@code :189-206} — the frenzy legs: at counter 150 the teleport home, every 10 ticks the ring. */
    public static final int FRENZY_COUNTER = 150;
    public static final int FRENZY_RING_PERIOD = 10;
    public static final int FRENZY_RING_LIMIT = 121;

    public static boolean frenziedTeleports(final int counter) {
        return counter == FRENZY_COUNTER;                                               // :191
    }

    public static boolean frenzyRingFires(final int counter) {
        return counter > 0 && counter < FRENZY_RING_LIMIT && counter % FRENZY_RING_PERIOD == 0;
    }

    /** {@code :196-198} — {@code radius = (150 - counter) / 8}, {@code d = 1 + counter / 8}. */
    public static double frenzyRadius(final int counter) {
        return (FRENZY_COUNTER - counter) / 8.0;                                        // :196
    }

    public static double frenzyDepth(final int counter) {
        return 1.0 + counter / 8.0;                                                     // :197
    }

    /** {@code :199} — the ring loop runs {@code q < 180 / d}. */
    public static int frenzySteps(final int counter) {
        return (int) (180.0 / frenzyDepth(counter));                                    // :199
    }

    public static final int STATUS_FRENZY = 17;                                         // :195
    public static final int STATUS_ARM_LEFT = 15;                                       // :386
    public static final int STATUS_ARM_RIGHT = 16;                                      // :384

    /** {@code :247-306} — the home teleport: up to 20 tries at ±8, the cell needs solid below. */
    public static final int TELEPORT_TRIES = 20;
    public static final int TELEPORT_RANGE = 8;
    public static final double TELEPORT_Y_OFFSET = 0.1;

    // ---------------------------------------------------------------- immunity (:308-326)

    /** {@code :308-311} — invulnerable while the frenzy is armed, on top of the base's rule. */
    public static boolean invulnerableForFrenzy(final boolean frenzy, final boolean base) {
        return frenzy || base;                                                          // :310
    }

    /**
     * {@code :313-326} — the damage gate. The warden ignores void damage and the
     * 1.7.10 anvil/cactus pair ({@code field_76369_e}, {@code field_82727_n}); a
     * lethal hit that is not already a frenzy is CONSUMED and starts the frenzy
     * (the death fake-out, {@code :320-324}).
     */
    public static boolean ignoresDamageSource(final String sourceId) {
        return "outOfWorld".equals(sourceId)                                            // :315
                || "anvil".equals(sourceId)                                             // :317
                || "cactus".equals(sourceId);                                           // :317
    }

    public static boolean frenzyStartsOnHit(final boolean alreadyFrenzy, final float healthAfter) {
        return !alreadyFrenzy && healthAfter <= 0.0F;                                   // :320-322
    }

    // ---------------------------------------------------------------- ranged (:340-380)

    /** {@code :341} — {@code rand() > 0.2} fires the orb; the tenth shot is the screech. */
    public static final double ORB_ROLL = 0.2;
    public static final boolean LAST_BLAST_START = false;                               // :51

    public static boolean firesOrb(final double roll) {
        return roll > ORB_ROLL;                                                         // :341
    }

    /** {@code :342} — {@code new EntityEldritchOrb(level, this)} (c4b's class). */
    public static final int ORB_COUNT = 1;
    /** {@code :351-356} — spawn offset ±(cos/sin(yaw + (90|180)) * 0.5, 0.13). */
    public static final double ORB_OFFSET = 0.5;
    public static final double ORB_Y_OFFSET = 0.13;
    /** {@code :358} — {@code func_70186_c(d0, d1, d2, 1.0F, 2.0F)}. */
    public static final float ORB_SPEED = 1.0F;
    public static final float ORB_SPREAD = 2.0F;
    /** {@code :360} — the {@code egattack} sound at volume 2. */
    public static final float ATTACK_SOUND_VOLUME = 2.0F;

    /** {@code :362-378} — the screech: warp {@code 3 + rand(3)} and two 400-tick effects. */
    public static final int WARP_BASE = 3;
    public static final int WARP_RANDOM = 3;
    public static final int SCREECH_SOUND_VOLUME = 4;

    public static int warpFor(final int roll3) {
        return WARP_BASE + Math.floorMod(roll3, WARP_RANDOM);                           // :368
    }

    /** The attributes: {@code :104-106} over the boss base's pair (knockback 0.95, follow 40). */
    public static AttributeSupplier.Builder createAttributes() {
        return ThaumcraftBossAiLogic.onMonsterAttributes(
                        net.minecraft.world.entity.monster.Monster.createMonsterAttributes())
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);
    }
}
