package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic.GoalSpec;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic.Kind;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic.Target;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the six taint animals' goal tables and stats (TC-M6-06-s-b) without a
 * world. Every expectation quotes the decompiled source it was read from —
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/}
 * — with the line number; the tables live in each original's CONSTRUCTOR
 * (none of the six declares {@code func_70610_a}).
 */
class TaintedAnimalAiLogicTest {

    private static String row(final GoalSpec spec) {
        return spec.priority() + ":" + spec.kind() + ":" + spec.target() + ":" + spec.speed()
                + ":" + spec.range() + ":" + spec.flag();
    }

    // ---------------------------------------------------------------- chicken

    @Test
    void chickenGoalTableIsTheDecompiledConstructorBody() {
        // EntityTaintChicken lines 35-42
        assertEquals(List.of(
                "0:SWIM_GOAL:NONE:0.0:0.0:false",
                "2:ATTACK:PLAYER:1.0:0.0:false",
                "2:LEAP:NONE:0.3:0.0:false",
                "3:ATTACK:VILLAGER:1.0:0.0:true",
                "3:ATTACK:ANIMAL:1.0:0.0:true",
                "3:WANDER:NONE:1.0:0.0:false",
                "4:WATCH:PLAYER:0.0:6.0:false",
                "5:LOOK_IDLE:NONE:0.0:0.0:false"),
                TaintedAnimalAiLogic.chickenGoals().stream().map(TaintedAnimalAiLogicTest::row).toList());
        // EntityTaintChicken lines 43-46
        assertEquals(List.of(
                "0:HURT_BY:NONE:0.0:0.0:false",
                "2:TARGET:PLAYER:0.0:0.0:true",
                "2:TARGET:VILLAGER:0.0:0.0:false",
                "3:TARGET:ANIMAL:0.0:0.0:false"),
                TaintedAnimalAiLogic.chickenTargetGoals().stream()
                        .map(TaintedAnimalAiLogicTest::row).toList());
    }

    // -------------------------------------------------------------- cow / pig

    @Test
    void cowGoalTableIsTheDecompiledConstructorBody() {
        // EntityTaintCow lines 28-34 (goals) and 35-38 (targets)
        assertEquals(List.of(
                "0:SWIM_GOAL:NONE:0.0:0.0:false",
                "2:ATTACK:PLAYER:1.0:0.0:false",
                "3:ATTACK:VILLAGER:1.0:0.0:true",
                "5:WANDER:NONE:1.0:0.0:false",
                "6:WATCH:PLAYER:0.0:6.0:false",
                "7:LOOK_IDLE:NONE:0.0:0.0:false",
                "8:ATTACK:ANIMAL:1.0:0.0:false"),
                TaintedAnimalAiLogic.cowGoals().stream().map(TaintedAnimalAiLogicTest::row).toList());
        assertEquals(List.of(
                "0:HURT_BY:NONE:0.0:0.0:false",
                "2:TARGET:PLAYER:0.0:0.0:true",
                "2:TARGET:VILLAGER:0.0:0.0:false",
                "8:TARGET:ANIMAL:0.0:0.0:false"),
                TaintedAnimalAiLogic.cowTargetGoals().stream()
                        .map(TaintedAnimalAiLogicTest::row).toList());
    }

    @Test
    void pigTableIsTheCowTableVerbatim() {
        // EntityTaintPig lines 28-38 are byte-identical to the cow's
        assertEquals(TaintedAnimalAiLogic.cowGoals(), TaintedAnimalAiLogic.pigGoals());
        assertEquals(TaintedAnimalAiLogic.cowTargetGoals(), TaintedAnimalAiLogic.pigTargetGoals());
    }

    // ------------------------------------------------------------------ sheep

    @Test
    void sheepGoalTableCarriesTheConvertGrassRow() {
        // EntityTaintSheep lines 36-42 and 43-45
        assertEquals(List.of(
                "0:SWIM_GOAL:NONE:0.0:0.0:false",
                "2:CONVERT_GRASS:NONE:0.0:0.0:false",
                "3:ATTACK:PLAYER:1.0:0.0:false",
                "3:ATTACK:VILLAGER:1.0:0.0:true",
                "6:WANDER:NONE:1.0:0.0:false",
                "7:WATCH:PLAYER:0.0:6.0:false",
                "8:LOOK_IDLE:NONE:0.0:0.0:false"),
                TaintedAnimalAiLogic.sheepGoals().stream().map(TaintedAnimalAiLogicTest::row).toList());
        assertEquals(List.of(
                "0:HURT_BY:NONE:0.0:0.0:false",
                "3:TARGET:PLAYER:0.0:0.0:true",
                "3:TARGET:VILLAGER:0.0:0.0:false"),
                TaintedAnimalAiLogic.sheepTargetGoals().stream()
                        .map(TaintedAnimalAiLogicTest::row).toList());
    }

    // ---------------------------------------------------------------- creeper

    @Test
    void creeperGoalTableIsTheDecompiledConstructorBody() {
        // EntityTaintCreeper lines 41-47 and 48-49
        assertEquals(List.of(
                "1:SWIM_GOAL:NONE:0.0:0.0:false",
                "2:SWELL:NONE:0.0:0.0:false",
                "3:AVOID:OCELOT:1.0:6.0:false",
                "4:ATTACK:NONE:1.0:0.0:false",
                "5:WANDER:NONE:1.0:0.0:false",
                "6:WATCH:PLAYER:0.0:8.0:false",
                "6:LOOK_IDLE:NONE:0.0:0.0:false"),
                TaintedAnimalAiLogic.creeperGoals().stream().map(TaintedAnimalAiLogicTest::row).toList());
        assertEquals(List.of(
                "1:TARGET:PLAYER:0.0:0.0:true",
                "2:HURT_BY:NONE:0.0:0.0:false"),
                TaintedAnimalAiLogic.creeperTargetGoals().stream()
                        .map(TaintedAnimalAiLogicTest::row).toList());
    }

    // --------------------------------------------------------------- villager

    @Test
    void villagerGoalTableKeepsTheRestrictOpenDoorAndVillagePriorities() {
        // EntityTaintVillager lines 45-55 and 56-57: the row at 3 is
        // EntityAIRestrictOpenDoor (no speed argument), the restriction row
        // sits at 5 and MoveThroughVillage at 6.
        assertEquals(List.of(
                "0:SWIM_GOAL:NONE:0.0:0.0:false",
                "2:MOVE_INDOORS:NONE:0.0:0.0:false",
                "2:ATTACK:PLAYER:1.0:0.0:false",
                "3:RESTRICT_OPEN_DOOR:NONE:0.0:0.0:false",
                "4:OPEN_DOOR:NONE:0.0:0.0:true",
                "5:RESTRICT:NONE:1.0:0.0:false",
                "6:VILLAGE:NONE:1.0:0.0:false",
                "9:WATCH:PLAYER:0.0:3.0:true",
                "9:WATCH:VILLAGER:0.0:5.0:false",
                "9:WANDER:NONE:1.0:0.0:false",
                "10:WATCH_LIVING:LIVING:0.0:8.0:false"),
                TaintedAnimalAiLogic.villagerGoals().stream()
                        .map(TaintedAnimalAiLogicTest::row).toList());
        assertEquals(List.of(
                "0:HURT_BY:NONE:0.0:0.0:false",
                "2:TARGET:PLAYER:0.0:0.0:true"),
                TaintedAnimalAiLogic.villagerTargetGoals().stream()
                        .map(TaintedAnimalAiLogicTest::row).toList());
    }

    @Test
    void villagerVillageRowCarriesTheDecompiledOnlyAtNightLiteral() {
        final GoalSpec village = TaintedAnimalAiLogic.villagerGoals().stream()
                .filter(spec -> spec.kind() == Kind.VILLAGE).findFirst().orElseThrow();
        // EntityTaintVillager line 51: new EntityAIMoveThroughVillage(this, 1.0, false)
        assertEquals(6, village.priority());
        assertEquals(1.0D, village.speed());
        assertTrue(!village.flag(), "the decompiled third argument is literally false");
    }

    // ------------------------------------------------------------- statistics

    @Test
    void hitboxesAndAttributesQuoteTheDecompiledConstructors() {
        // EntityTaintChicken line 34 / 51-53 / 64-65
        assertEquals(0.5D, TaintedAnimalAiLogic.CHICKEN.width());
        assertEquals(0.8D, TaintedAnimalAiLogic.CHICKEN.height());
        assertEquals(8.0D, TaintedAnimalAiLogic.CHICKEN.health());
        assertEquals(3.0D, TaintedAnimalAiLogic.CHICKEN.attack());
        assertEquals(0.4D, TaintedAnimalAiLogic.CHICKEN.movementSpeed());
        assertEquals(2.0D, TaintedAnimalAiLogic.CHICKEN.armor());

        // EntityTaintCow line 26 / 43-45 (no func_70658_aO override)
        assertEquals(0.9D, TaintedAnimalAiLogic.COW.width());
        assertEquals(1.3D, TaintedAnimalAiLogic.COW.height());
        assertEquals(40.0D, TaintedAnimalAiLogic.COW.health());
        assertEquals(6.0D, TaintedAnimalAiLogic.COW.attack());
        assertEquals(0.27D, TaintedAnimalAiLogic.COW.movementSpeed());
        assertEquals(0.0D, TaintedAnimalAiLogic.COW.armor());

        // EntityTaintPig line 26 / 43-45 / 65-66
        assertEquals(0.9D, TaintedAnimalAiLogic.PIG.width());
        assertEquals(0.9D, TaintedAnimalAiLogic.PIG.height());
        assertEquals(20.0D, TaintedAnimalAiLogic.PIG.health());
        assertEquals(4.0D, TaintedAnimalAiLogic.PIG.attack());
        assertEquals(0.275D, TaintedAnimalAiLogic.PIG.movementSpeed());
        assertEquals(2.0D, TaintedAnimalAiLogic.PIG.armor());

        // EntityTaintSheep line 34 / 50-52 / 63-64
        assertEquals(0.9D, TaintedAnimalAiLogic.SHEEP.width());
        assertEquals(1.3D, TaintedAnimalAiLogic.SHEEP.height());
        assertEquals(20.0D, TaintedAnimalAiLogic.SHEEP.health());
        assertEquals(3.0D, TaintedAnimalAiLogic.SHEEP.attack());
        assertEquals(0.25D, TaintedAnimalAiLogic.SHEEP.movementSpeed());
        assertEquals(1.0D, TaintedAnimalAiLogic.SHEEP.armor());

        // EntityTaintCreeper has no func_70105_a (the vanilla 0.6 x 1.8 box)
        // and its attributes are lines 54-57
        assertEquals(0.6D, TaintedAnimalAiLogic.CREEPER.width());
        assertEquals(1.8D, TaintedAnimalAiLogic.CREEPER.height());
        assertEquals(30.0D, TaintedAnimalAiLogic.CREEPER.health());
        assertEquals(2.0D, TaintedAnimalAiLogic.CREEPER.attack());
        assertEquals(0.25D, TaintedAnimalAiLogic.CREEPER.movementSpeed());
        assertEquals(0.0D, TaintedAnimalAiLogic.CREEPER.armor());

        // EntityTaintVillager has no func_70105_a either; attributes 62-64
        assertEquals(0.6D, TaintedAnimalAiLogic.VILLAGER.width());
        assertEquals(1.8D, TaintedAnimalAiLogic.VILLAGER.height());
        assertEquals(30.0D, TaintedAnimalAiLogic.VILLAGER.health());
        assertEquals(4.0D, TaintedAnimalAiLogic.VILLAGER.attack());
        assertEquals(0.3D, TaintedAnimalAiLogic.VILLAGER.movementSpeed());
        assertEquals(0.0D, TaintedAnimalAiLogic.VILLAGER.armor());
    }

    // ----------------------------------------------------------- sound slots

    @Test
    void everyAnimalPitchIsSevenTenthsAndOnlyTheCowOverridesTheVolume() {
        // func_70647_i (getSoundPitch) returns 0.7F in all six originals:
        // chicken 121-122, cow 76-77, pig 98-99, sheep 161-162, creeper
        // 203-204, villager 151-152.
        assertEquals(0.7F, TaintedAnimalAiLogic.chickenSoundPitch());
        assertEquals(0.7F, TaintedAnimalAiLogic.cowSoundPitch());
        assertEquals(0.7F, TaintedAnimalAiLogic.pigSoundPitch());
        assertEquals(0.7F, TaintedAnimalAiLogic.sheepSoundPitch());
        assertEquals(0.7F, TaintedAnimalAiLogic.creeperSoundPitch());
        assertEquals(0.7F, TaintedAnimalAiLogic.villagerSoundPitch());
        // func_70599_aP (getSoundVolume) is overridden only by the cow, 80-81
        assertEquals(0.4F, TaintedAnimalAiLogic.cowSoundVolume());
    }

    // ------------------------------------------------------------ village net

    @Test
    void villageRefreshConstantsQuoteTheDecompiledBody() {
        // EntityTaintVillager line 85: 70 + nextInt(50)
        assertEquals(70, TaintedAnimalAiLogic.villageRefreshDivider(0));
        assertEquals(119, TaintedAnimalAiLogic.villageRefreshDivider(49));
        // line 89: Village.getVillageWithin(this, 32, ...)
        assertEquals(32, TaintedAnimalAiLogic.VILLAGE_SCAN_RADIUS);
        // line 96: (int)(village.func_82685_c() * 0.6F)
        assertEquals(0.6D, TaintedAnimalAiLogic.VILLAGE_RESTRICTION_FACTOR);
    }

    // -------------------------------------------------- goal-table invariants

    @Test
    void everyTableEntryUsesAGoalKindTheFactoryKnows() {
        final List<List<GoalSpec>> tables = List.of(
                TaintedAnimalAiLogic.chickenGoals(), TaintedAnimalAiLogic.chickenTargetGoals(),
                TaintedAnimalAiLogic.cowGoals(), TaintedAnimalAiLogic.cowTargetGoals(),
                TaintedAnimalAiLogic.sheepGoals(), TaintedAnimalAiLogic.sheepTargetGoals(),
                TaintedAnimalAiLogic.creeperGoals(), TaintedAnimalAiLogic.creeperTargetGoals(),
                TaintedAnimalAiLogic.villagerGoals(), TaintedAnimalAiLogic.villagerTargetGoals());
        for (final List<GoalSpec> table : tables) {
            for (final GoalSpec spec : table) {
                assertTrue(spec.priority() >= 0, "a negative priority would reorder the table");
                assertTrue(spec.target() != Target.NONE || spec.kind() == Kind.HURT_BY
                                || spec.kind() == Kind.SWIM_GOAL || spec.kind() == Kind.WANDER
                                || spec.kind() == Kind.LOOK_IDLE || spec.kind() == Kind.LEAP
                                || spec.kind() == Kind.SWELL || spec.kind() == Kind.CONVERT_GRASS
                                || spec.kind() == Kind.OPEN_DOOR || spec.kind() == Kind.MOVE_INDOORS
                                || spec.kind() == Kind.RESTRICT_OPEN_DOOR
                                || spec.kind() == Kind.RESTRICT || spec.kind() == Kind.VILLAGE
                                || spec.kind() == Kind.ATTACK,
                        "a class-targeted row must name its target class: " + spec);
            }
        }
    }
}
