package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The rules of {@code thaumcraft.common.entities.monster.boss.EntityEldritchGolem}
 * (370 lines) — the headless phase machine — Level-free and pinned by
 * {@code src/test/java/.../entities/monster/boss/EldritchgolemAiLogicTest.java}.
 *
 * <p>Citations are the 1.7.10 decompile
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityEldritchGolem.java}.
 * The id was already live on main as a stub (TC-M6-08-s-c); this card repoints the
 * ONE registration to this class, so the stub's figures (box, tracking, egg colours)
 * are carried verbatim here.</p>
 */
public final class EldritchgolemAiLogic {

    private EldritchgolemAiLogic() {
    }

    /** {@code :54} — {@code func_70105_a(1.75F, 3.5F)} (the stub's own constants). */
    public static final float WIDTH = 1.75F;
    public static final float HEIGHT = 3.5F;
    /** {@code ConfigEntities.java:119} — {@code (64, 3, true)}. */
    public static final int TRACKING_RANGE = 64;
    public static final int UPDATE_INTERVAL = 3;
    /** {@code ConfigEntities.java:120} — 5592405 / 4210752. */
    public static final int EGG_PRIMARY = 0x555555;
    public static final int EGG_SECONDARY = 0x404040;

    /** {@code :55} — {@code theIgnoreFires = true}. */
    public static final boolean FIRE_IMMUNE = true;
    /** {@code :95} — eye height 3.33 headless, 3.0 with the head. */
    public static final float EYE_HEIGHT_HEADLESS = 3.33F;
    public static final float EYE_HEIGHT = 3.0F;
    /** {@code :99} — {@code getArmor() = super + 6}. */
    public static final int ARMOUR_BONUS = 6;

    /** {@code :106-108} — speed 0.3, attack 10.0, maxHealth 250.0. */
    public static final double MOVEMENT_SPEED = 0.3;
    public static final double ATTACK_DAMAGE = 10.0;
    public static final double MAX_HEALTH = 250.0;

    /** {@code :69} — datawatcher index 12, a {@code Byte}: headless. */
    public static final int HEADLESS_WATCHER_INDEX = 12;
    /** {@code :81-93} — the NBT key; a load re-runs {@code makeHeadless()}. */
    public static final String NBT_HEADLESS = "headless";

    /** {@code :124-127} — {@code onInitialSpawn}: {@code spawnTimer = 100}. */
    public static final int SPAWN_TIMER = 100;
    /** {@code :283-285} — while the spawn timer runs the golem heals 2.0F per tick. */
    public static final float SPAWN_HEAL = 2.0F;
    /** {@code :281} / {@code :340} — status 18 at {@code spawnTimer == 150} (the client's 150). */
    public static final int STATUS_SPAWN = 18;
    public static final int SPAWN_TIMER_CLIENT = 150;

    // ---------------------------------------------------------------- goals (:46-53)

    public static final int GOAL_SWIM = 0;                                              // :46
    public static final int GOAL_MELEE = 3;                                             // :47
    public static final int GOAL_RESTRICTION = 6;                                       // :48
    public static final int GOAL_WANDER = 7;                                            // :49
    public static final int GOAL_WATCH = 8;                                             // :50
    public static final int GOAL_LOOK_IDLE = 8;                                         // :51
    public static final int TARGET_HURT = 1;                                            // :52
    public static final int TARGET_PLAYER = 2;                                          // :53
    /** {@code :47} — {@code AIGolemAttackOnCollide(this, 1.1, false)}. */
    public static final double MELEE_SPEED = 1.1;
    public static final boolean MELEE_LONG_MEMORY = false;
    public static final double RESTRICTION_SPEED = 0.8;                                 // :48
    public static final double WANDER_SPEED = 0.8;                                      // :49
    public static final float WATCH_PLAYER_RANGE = 8.0F;                                // :50
    public static final boolean HURT_IS_AGRESSORS = true;                               // :52
    public static final int TARGET_SORT_TYPE = 0;                                       // :53
    public static final boolean TARGET_ALWAYS_CLOSE = true;                             // :53

    /**
     * {@code :185-187} — the MID-FIGHT goal the headless phase installs:
     * {@code AILongRangeAttack(this, 3.0, 1.0, 5, 5, 24.0F)} at goal slot 2.
     */
    public static final int GOAL_RANGED_PHASE2 = 2;
    public static final double RANGED_MIN_DISTANCE = 3.0;
    public static final double RANGED_SPEED = 1.0;
    public static final int RANGED_INTERVAL_MIN = 5;
    public static final int RANGED_INTERVAL_MAX = 5;
    public static final float RANGED_RADIUS = 24.0F;

    // ---------------------------------------------------------------- phase machine (:170-187)

    /**
     * {@code :170-183} — the ONLY headless trigger: the first hit whose damage is
     * greater than the WHOLE max health bar, and the golem is not headless yet.
     * The hit is then consumed (the method returns false).
     */
    public static boolean becomesHeadless(final float damage, final double maxHealth,
            final boolean alreadyHeadless) {
        return !alreadyHeadless && damage > maxHealth;                                  // :172-174
    }

    /** {@code :176-182} — the phase swap: headless, {@code spawnTimer = 100}, the FX cell. */
    public static int headlessSpawnTimer() {
        return SPAWN_TIMER;                                                             // :177
    }

    // ---------------------------------------------------------------- melee (:189-211)

    /** {@code :191-195} — the {@code attackTimer} gate: 10 ticks, status 4. */
    public static final int ATTACK_TIMER = 10;
    public static final int STATUS_ATTACK = 4;
    /** {@code :197} — the damage is {@code attackDamage * 0.75}. */
    public static final float MELEE_DAMAGE_FACTOR = 0.75F;
    /** {@code :198} — the target is lifted: {@code motionY += 0.2}. */
    public static final double MELEE_LIFT = 0.2;
    /** {@code :200-205} — headless only: the golem throws its target (±1.5, 0.1, ±1.5). */
    public static final double HEADLESS_KNOCKBACK = 1.5;
    public static final double HEADLESS_LIFT = 0.1;

    public static float meleeDamage(final double attackDamage) {
        return (float) (attackDamage * MELEE_DAMAGE_FACTOR);                            // :197
    }

    // ---------------------------------------------------------------- beam (:213-236, :281-359)

    /** {@code :215-221} — the beam cadence spends {@code 15 + rand(5)} charge. */
    public static final int BEAM_SPEND_BASE = 15;
    public static final int BEAM_SPEND_RANDOM = 5;

    public static int beamSpend(final int roll5) {
        return BEAM_SPEND_BASE + Math.floorMod(roll5, BEAM_SPEND_RANDOM);               // :221
    }

    /** {@code :225} — {@code new EntityGolemOrb(level, this, target, false)}: NOT red. */
    public static final boolean ORB_IS_RED = false;
    /** {@code :227-231} — aim 30/30, {@code func_70186_c(d0, d1, d2, 0.66F, 5.0F)}. */
    public static final float ORB_SPEED = 0.66F;
    public static final float ORB_SPREAD = 5.0F;

    /** {@code :300-310} — the charge cycle: 150 ticks, and the beam fires while charged. */
    public static final int BEAM_CHARGE_TICKS = 150;
    public static final int STATUS_CHARGE = 19;

    /**
     * {@code :300-310} — the server's charge pass, one tick at a time: a headless
     * golem with no charge starts charging; every tick adds one; at 150 the charge
     * window closes.
     */
    public static boolean beamStarts(final boolean headless, final int charge) {
        return headless && charge <= 0;                                                 // :301
    }

    public static int beamTick(final int charge) {
        return charge + 1;                                                              // :303
    }

    public static boolean beamEnds(final int charge) {
        return charge >= BEAM_CHARGE_TICKS;                                             // :305
    }

    public static boolean beamCanFire(final boolean headless, final boolean charging,
            final int charge) {
        return headless && !charging && charge > 0;                                     // :215-219
    }

    // ---------------------------------------------------------------- block crack (:129-167)

    /** {@code :131-137} — 1-in-5 crack particles while moving. */
    public static final int CRACK_CHANCE = 5;
    /** {@code :148-166} — it destroys a {@code BlockLoot} under it, and any block whose
     *  hardness is in {@code [0, 0.15]} at the next cell (0.15 is the original's bound). */
    public static final float HARDNESS_MAX = 0.15F;
    public static final float HARDNESS_MIN = 0.0F;

    public static boolean crushesBlock(final float hardness) {
        return hardness >= HARDNESS_MIN && hardness <= HARDNESS_MAX;                    // :160-163
    }

    /** The attributes: {@code :106-108} over the boss base's pair (knockback 0.95, follow 40). */
    public static AttributeSupplier.Builder createAttributes() {
        return ThaumcraftBossAiLogic.onMonsterAttributes(
                        net.minecraft.world.entity.monster.Monster.createMonsterAttributes())
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);
    }
}
