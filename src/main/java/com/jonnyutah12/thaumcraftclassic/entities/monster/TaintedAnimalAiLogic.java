package com.jonnyutah12.thaumcraftclassic.entities.monster;

import java.util.List;

/**
 * Level-free goal tables and stats of the taint animals (TC-M6-06-s-b). The
 * originals are NOT vanilla-animal subclasses: every decompiled taint animal
 * extends {@code EntityMob} and builds its goal table in its CONSTRUCTOR
 * (there is no {@code func_70610_a} in any of the six files), so the port
 * mirrors that shape — a {@code Monster} whose selectors are populated from
 * these tables.
 *
 * <p>Every constant quotes the decompiled file and line it came from
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/}); the
 * JUnit test {@code TaintedAnimalAiLogicTest} re-states the same
 * (file, line, value) triples so a drift is a red test.</p>
 */
public final class TaintedAnimalAiLogic {

    public enum Kind {
        ATTACK,             // AIAttackOnCollide(this, class, speed, persist)
        LEAP,               // EntityAILeapAtTarget(this, 0.3F)
        WANDER,             // EntityAIWander(this, speed)
        WATCH,              // EntityAIWatchClosest(this, class, range[, prob])
        WATCH_LIVING,       // EntityAIWatchClosest(this, LivingEntity.class, 8.0F)
        LOOK_IDLE,          // EntityAILookIdle(this)
        SWELL,              // AICreeperSwell(this)
        CONVERT_GRASS,      // AIConvertGrass(this)
        AVOID,              // EntityAIAvoidEntity(this, Ocelot.class, 6.0F, 1.0, 1.2)
        OPEN_DOOR,          // EntityAIOpenDoor(this, true)
        RESTRICT_OPEN_DOOR, // EntityAIRestrictOpenDoor(this)
        MOVE_INDOORS,       // EntityAIMoveIndoors(this)
        RESTRICT,           // EntityAIMoveTowardsRestriction(this, speed)
        VILLAGE,            // EntityAIMoveThroughVillage(this, speed, onlyAtNight)
        SWIM_GOAL,          // EntityAISwimming(this)
        HURT_BY,            // EntityAIHurtByTarget(this, false)
        TARGET              // EntityAINearestAttackableTarget(this, class, 0, mustSee)
    }

    /**
     * One goal-selector row. {@code speed} carries the movement speed for
     * ATTACK/WANDER/RESTRICT/VILLAGE rows, {@code range} the watch/avoid
     * range, and {@code flag} the row's boolean argument — its meaning is
     * per-Kind and quoted on each table entry below.
     */
    public record GoalSpec(int priority, Kind kind, Target target, double speed, double range,
            boolean flag) {
        public GoalSpec(final int priority, final Kind kind, final Target target) {
            this(priority, kind, target, 0.0D, 0.0D, false);
        }
    }

    public enum Target {
        NONE, PLAYER, VILLAGER, ANIMAL, OCELOT, LIVING
    }

    // ---------------------------------------------------------------- chicken

    /**
     * {@code EntityTaintChicken} ctor lines 35-46 (goal selector 35-42,
     * target selector 43-46), in source order:
     * 0 EntityAISwimming, 2 AIAttackOnCollide(player,1.0,false), 2
     * EntityAILeapAtTarget(0.3F), 3 AIAttackOnCollide(villager,1.0,true), 3
     * AIAttackOnCollide(animal,1.0,true), 3 EntityAIWander(1.0), 4
     * EntityAIWatchClosest(player,6.0F), 5 EntityAILookIdle; targets 0
     * EntityAIHurtByTarget(false), 2 NearestAttackableTarget(player,0,true),
     * 2 (villager,0,false), 3 (animal,0,false).
     */
    public static List<GoalSpec> chickenGoals() {
        return List.of(
                new GoalSpec(0, Kind.SWIM_GOAL, Target.NONE),                          // line 35
                new GoalSpec(2, Kind.ATTACK, Target.PLAYER, 1.0D, 0.0D, false),        // line 36
                new GoalSpec(2, Kind.LEAP, Target.NONE, 0.3D, 0.0D, false),            // line 37
                new GoalSpec(3, Kind.ATTACK, Target.VILLAGER, 1.0D, 0.0D, true),       // line 38
                new GoalSpec(3, Kind.ATTACK, Target.ANIMAL, 1.0D, 0.0D, true),         // line 39
                new GoalSpec(3, Kind.WANDER, Target.NONE, 1.0D, 0.0D, false),          // line 40
                new GoalSpec(4, Kind.WATCH, Target.PLAYER, 0.0D, 6.0D, false),         // line 41
                new GoalSpec(5, Kind.LOOK_IDLE, Target.NONE));                         // line 42
    }

    public static List<GoalSpec> chickenTargetGoals() {
        return List.of(
                new GoalSpec(0, Kind.HURT_BY, Target.NONE),                            // line 43
                new GoalSpec(2, Kind.TARGET, Target.PLAYER, 0.0D, 0.0D, true),         // line 44
                new GoalSpec(2, Kind.TARGET, Target.VILLAGER, 0.0D, 0.0D, false),      // line 45
                new GoalSpec(3, Kind.TARGET, Target.ANIMAL, 0.0D, 0.0D, false));       // line 46
    }

    // -------------------------------------------------------------------- cow

    /**
     * {@code EntityTaintCow} ctor lines 28-38: 0 Swimming, 2
     * Attack(player,1.0,false), 3 Attack(villager,1.0,true), 5 Wander, 6
     * Watch(player,6.0), 7 LookIdle, 8 Attack(animal,1.0,false); targets 0
     * HurtBy(false), 2 Target(player,true), 2 Target(villager,false), 8
     * Target(animal,false).
     */
    public static List<GoalSpec> cowGoals() {
        return List.of(
                new GoalSpec(0, Kind.SWIM_GOAL, Target.NONE),                          // line 28
                new GoalSpec(2, Kind.ATTACK, Target.PLAYER, 1.0D, 0.0D, false),        // line 29
                new GoalSpec(3, Kind.ATTACK, Target.VILLAGER, 1.0D, 0.0D, true),       // line 30
                new GoalSpec(5, Kind.WANDER, Target.NONE, 1.0D, 0.0D, false),          // line 31
                new GoalSpec(6, Kind.WATCH, Target.PLAYER, 0.0D, 6.0D, false),         // line 32
                new GoalSpec(7, Kind.LOOK_IDLE, Target.NONE),                          // line 33
                new GoalSpec(8, Kind.ATTACK, Target.ANIMAL, 1.0D, 0.0D, false));       // line 34
    }

    public static List<GoalSpec> cowTargetGoals() {
        return List.of(
                new GoalSpec(0, Kind.HURT_BY, Target.NONE),                            // line 35
                new GoalSpec(2, Kind.TARGET, Target.PLAYER, 0.0D, 0.0D, true),         // line 36
                new GoalSpec(2, Kind.TARGET, Target.VILLAGER, 0.0D, 0.0D, false),      // line 37
                new GoalSpec(8, Kind.TARGET, Target.ANIMAL, 0.0D, 0.0D, false));       // line 38
    }

    // -------------------------------------------------------------------- pig

    /** {@code EntityTaintPig} ctor lines 28-38: byte-identical to the cow's. */
    public static List<GoalSpec> pigGoals() {
        return cowGoals();
    }

    public static List<GoalSpec> pigTargetGoals() {
        return cowTargetGoals();
    }

    // ------------------------------------------------------------------ sheep

    /**
     * {@code EntityTaintSheep} ctor lines 36-45: 0 Swimming, 2
     * AIConvertGrass (the {@code field_48137_c} instance field), 3
     * Attack(player,1.0,false), 3 Attack(villager,1.0,true), 6 Wander, 7
     * Watch(player,6.0), 8 LookIdle; targets 0 HurtBy(false), 3
     * Target(player,true), 3 Target(villager,false).
     */
    public static List<GoalSpec> sheepGoals() {
        return List.of(
                new GoalSpec(0, Kind.SWIM_GOAL, Target.NONE),                          // line 36
                new GoalSpec(2, Kind.CONVERT_GRASS, Target.NONE),                      // line 37
                new GoalSpec(3, Kind.ATTACK, Target.PLAYER, 1.0D, 0.0D, false),        // line 38
                new GoalSpec(3, Kind.ATTACK, Target.VILLAGER, 1.0D, 0.0D, true),       // line 39
                new GoalSpec(6, Kind.WANDER, Target.NONE, 1.0D, 0.0D, false),          // line 40
                new GoalSpec(7, Kind.WATCH, Target.PLAYER, 0.0D, 6.0D, false),         // line 41
                new GoalSpec(8, Kind.LOOK_IDLE, Target.NONE));                         // line 42
    }

    public static List<GoalSpec> sheepTargetGoals() {
        return List.of(
                new GoalSpec(0, Kind.HURT_BY, Target.NONE),                            // line 43
                new GoalSpec(3, Kind.TARGET, Target.PLAYER, 0.0D, 0.0D, true),         // line 44
                new GoalSpec(3, Kind.TARGET, Target.VILLAGER, 0.0D, 0.0D, false));     // line 45
    }

    // ---------------------------------------------------------------- creeper

    /**
     * {@code EntityTaintCreeper} ctor lines 41-49: 1 Swimming, 2
     * AICreeperSwell, 3 EntityAIAvoidEntity(ocelot, 6.0F, 1.0, 1.2) — the
     * escape speed 1.2 is the fifth constructor argument and is fixed in the
     * factory, so the row's flag is unused — 4 AIAttackOnCollide(1.0,false),
     * 5 Wander(1.0), 6 Watch(player,8.0), 6 LookIdle; targets 1
     * Target(player,true), 2 HurtBy(false).
     */
    public static List<GoalSpec> creeperGoals() {
        return List.of(
                new GoalSpec(1, Kind.SWIM_GOAL, Target.NONE),                          // line 41
                new GoalSpec(2, Kind.SWELL, Target.NONE),                              // line 42
                new GoalSpec(3, Kind.AVOID, Target.OCELOT, 1.0D, 6.0D, false),         // line 43
                new GoalSpec(4, Kind.ATTACK, Target.NONE, 1.0D, 0.0D, false),          // line 44
                new GoalSpec(5, Kind.WANDER, Target.NONE, 1.0D, 0.0D, false),          // line 45
                new GoalSpec(6, Kind.WATCH, Target.PLAYER, 0.0D, 8.0D, false),         // line 46
                new GoalSpec(6, Kind.LOOK_IDLE, Target.NONE));                         // line 47
    }

    public static List<GoalSpec> creeperTargetGoals() {
        return List.of(
                new GoalSpec(1, Kind.TARGET, Target.PLAYER, 0.0D, 0.0D, true),         // line 48
                new GoalSpec(2, Kind.HURT_BY, Target.NONE));                           // line 49
    }

    // --------------------------------------------------------------- villager

    /**
     * {@code EntityTaintVillager} ctor lines 45-57: 0 Swimming, 2
     * EntityAIMoveIndoors, 2 Attack(player,1.0,false), 3
     * EntityAIRestrictOpenDoor (NO speed argument), 4 EntityAIOpenDoor(true),
     * 5 EntityAIMoveTowardsRestriction(1.0), 6
     * EntityAIMoveThroughVillage(1.0, false), 9 EntityAIWatchClosest2(player,
     * 3.0F, 1.0F), 9 EntityAIWatchClosest2(villager, 5.0F, 0.02F), 9
     * EntityAIWander(1.0), 10 EntityAIWatchClosest(LivingEntity, 8.0F);
     * targets 0 HurtBy(false), 2 Target(player,true).
     *
     * <p>The VILLAGE row's boolean is the decompiled third constructor
     * argument (onlyAtNight) — literally {@code false}; the original's door
     * handling is its separate shouldPassDoors() behaviour and is passed to
     * 1.21.1's goal as a supplier.</p>
     */
    public static List<GoalSpec> villagerGoals() {
        return List.of(
                new GoalSpec(0, Kind.SWIM_GOAL, Target.NONE),                          // line 45
                new GoalSpec(2, Kind.MOVE_INDOORS, Target.NONE),                       // line 46
                new GoalSpec(2, Kind.ATTACK, Target.PLAYER, 1.0D, 0.0D, false),        // line 47
                new GoalSpec(3, Kind.RESTRICT_OPEN_DOOR, Target.NONE),                 // line 48
                new GoalSpec(4, Kind.OPEN_DOOR, Target.NONE, 0.0D, 0.0D, true),        // line 49
                new GoalSpec(5, Kind.RESTRICT, Target.NONE, 1.0D, 0.0D, false),        // line 50
                new GoalSpec(6, Kind.VILLAGE, Target.NONE, 1.0D, 0.0D, false),         // line 51
                new GoalSpec(9, Kind.WATCH, Target.PLAYER, 0.0D, 3.0D, true),          // line 52
                new GoalSpec(9, Kind.WATCH, Target.VILLAGER, 0.0D, 5.0D, false),       // line 53
                new GoalSpec(9, Kind.WANDER, Target.NONE, 1.0D, 0.0D, false),          // line 54
                new GoalSpec(10, Kind.WATCH_LIVING, Target.LIVING, 0.0D, 8.0D, false)); // line 55
    }

    public static List<GoalSpec> villagerTargetGoals() {
        return List.of(
                new GoalSpec(0, Kind.HURT_BY, Target.NONE),                            // line 56
                new GoalSpec(2, Kind.TARGET, Target.PLAYER, 0.0D, 0.0D, true));        // line 57
    }

    // ------------------------------------------------------------- statistics

    /**
     * Per-animal constructor/attribute constants. {@code width}/{@code height}
     * are {@code func_70105_a}; {@code health}/{@code attack}/
     * {@code movementSpeed} are the {@code func_110148_a} rows of
     * {@code func_110147_ax}; {@code armor} is {@code func_70658_aO}
     * (getTotalArmorValue).
     *
     * <p>The originals set NO {@code field_70728_aV} (experienceValue) — the
     * six animals inherit the vanilla mob default — so the port leaves
     * 1.21.1's default {@code xpReward} alone rather than inventing one. (The
     * taint SPIDER does set it: {@code field_70728_aV = 2}.)</p>
     */
    public record Stats(double width, double height, double health, double attack,
            double movementSpeed, double armor) {
    }

    /** chicken: 0.5x0.8 (line 34), 8.0/3.0/0.4 (51-53), armor 2 (64-65). */
    public static final Stats CHICKEN = new Stats(0.5D, 0.8D, 8.0D, 3.0D, 0.4D, 2.0D);
    /** cow: 0.9x1.3 (26), 40.0/6.0/0.27 (43-45), no armor override. */
    public static final Stats COW = new Stats(0.9D, 1.3D, 40.0D, 6.0D, 0.27D, 0.0D);
    /** pig: 0.9x0.9 (26), 20.0/4.0/0.275 (43-45), armor 2 (65-66). */
    public static final Stats PIG = new Stats(0.9D, 0.9D, 20.0D, 4.0D, 0.275D, 2.0D);
    /** sheep: 0.9x1.3 (34), 20.0/3.0/0.25 (50-52), armor 1 (63-64). */
    public static final Stats SHEEP = new Stats(0.9D, 1.3D, 20.0D, 3.0D, 0.25D, 1.0D);
    /** creeper: no func_70105_a (vanilla 0.6x1.8), 30.0/2.0/0.25 (55-57). */
    public static final Stats CREEPER = new Stats(0.6D, 1.8D, 30.0D, 2.0D, 0.25D, 0.0D);
    /** villager: no func_70105_a (vanilla 0.6x1.8), 30.0/4.0/0.3 (62-64). */
    public static final Stats VILLAGER = new Stats(0.6D, 1.8D, 30.0D, 4.0D, 0.3D, 0.0D);

    // ------------------------------------------------------------- sound slots
    // 1.7.10: func_70599_aP = getSoundVolume, func_70647_i = getSoundPitch.

    /** chicken pitch 0.7 (EntityTaintChicken lines 121-122); volume vanilla. */
    public static float chickenSoundPitch() {
        return 0.7F;
    }

    /** cow pitch 0.7 (EntityTaintCow lines 76-77). */
    public static float cowSoundPitch() {
        return 0.7F;
    }

    /** cow volume 0.4 (EntityTaintCow lines 80-81) — the only animal override. */
    public static float cowSoundVolume() {
        return 0.4F;
    }

    /** pig pitch 0.7 (EntityTaintPig lines 98-99). */
    public static float pigSoundPitch() {
        return 0.7F;
    }

    /** sheep pitch 0.7 (EntityTaintSheep lines 161-162). */
    public static float sheepSoundPitch() {
        return 0.7F;
    }

    /** creeper pitch 0.7 (EntityTaintCreeper lines 203-204). */
    public static float creeperSoundPitch() {
        return 0.7F;
    }

    /** villager pitch 0.7 (EntityTaintVillager lines 151-152). */
    public static float villagerSoundPitch() {
        return 0.7F;
    }

    /**
     * {@code EntityTaintVillager.func_70629_bd} (line 85): every
     * {@code 70 + nextInt(50)} ticks the villager re-reads its village
     * ({@code Village.getVillageWithin(this, 32, villagepos)} at line 89,
     * null → clear restriction, else {@code setRestriction(village,
     * (int)(village.func_82685_c() * 0.6F))} at line 96) — 1.21.1 removed
     * the village system; the port maps the rule onto the bed POI net
     * (documented deviation) with the same divider, scan radius 32 and 0.6
     * radius factor.
     */
    public static int villageRefreshDivider(final int roll50) {
        return 70 + roll50;
    }

    /** {@code Village.getVillageWithin(this, 32, ...)} (line 89): radius 32. */
    public static final int VILLAGE_SCAN_RADIUS = 32;

    /** {@code (int)(village.func_82685_c() * 0.6F)} (line 96): factor 0.6. */
    public static final double VILLAGE_RESTRICTION_FACTOR = 0.6D;

    private TaintedAnimalAiLogic() {
    }
}
