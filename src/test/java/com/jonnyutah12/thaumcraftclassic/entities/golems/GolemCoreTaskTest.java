package com.jonnyutah12.thaumcraftclassic.entities.golems;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-08-s-b: pins the animation-core → task table. The card calls the number
 * of cores and their task names "facts to look up, not to assume", so every
 * assertion below is quoted from the decompiled source:
 *
 * <ul>
 *   <li>the id set is {@code ItemGolemCore.icon[12]} plus the blank icon
 *       ({@code ItemGolemCore.java:17-41});</li>
 *   <li>the names are the original's per-meta lang keys
 *       ({@code reference/assets/thaumcraft/lang/en_US.lang:464-476});</li>
 *   <li>the goal lists are {@code EntityGolemBase.setupGolem()}'s
 *       {@code switch (this.getCore())} ({@code EntityGolemBase.java:220-296});</li>
 *   <li>the three predicates are {@code ItemGolemCore.java:66-119};</li>
 *   <li>the need-list routing is {@code GolemHelper.getItemsNeeded}
 *       ({@code GolemHelper.java:624-646}).</li>
 * </ul>
 */
class GolemCoreTaskTest {

    /**
     * The original has exactly twelve cores, metas {@code 0..11}, in the order
     * its lang file names them. A thirteenth value would mean the table was
     * invented rather than transcribed.
     */
    @Test
    void thereAreExactlyTwelveCoresInSourceOrder() {
        assertEquals(12, GolemCoreTask.values().length,
                "ItemGolemCore declares twelve core icons, metas 0..11");
        assertEquals(0, GolemCoreTask.FILL.id(), "core 0 is Fill");
        assertEquals(1, GolemCoreTask.EMPTY.id(), "core 1 is Empty");
        assertEquals(2, GolemCoreTask.GATHER.id(), "core 2 is Gather");
        assertEquals(3, GolemCoreTask.HARVEST.id(), "core 3 is Harvest");
        assertEquals(4, GolemCoreTask.GUARD.id(), "core 4 is Guard");
        assertEquals(5, GolemCoreTask.DECANTING.id(), "core 5 is Decanting");
        assertEquals(6, GolemCoreTask.ALCHEMY.id(), "core 6 is Alchemy");
        assertEquals(7, GolemCoreTask.CHOP.id(), "core 7 is Chop");
        assertEquals(8, GolemCoreTask.USE.id(), "core 8 is Use");
        assertEquals(9, GolemCoreTask.BUTCHER.id(), "core 9 is Butcher");
        assertEquals(10, GolemCoreTask.SORTING.id(), "core 10 is Sorting");
        assertEquals(11, GolemCoreTask.FISHING.id(), "core 11 is Fishing");
    }

    /** The per-meta lang names: {@code item.ItemGolemCore.<meta>.name} (en_US.lang:464-476). */
    @Test
    void coreNamesAreTheOriginalLangStrings() {
        assertEquals(List.of("Fill", "Empty", "Gather", "Harvest", "Guard", "Decanting",
                        "Alchemy", "Chop", "Use", "Butcher", "Sorting", "Fishing"),
                java.util.Arrays.stream(GolemCoreTask.values()).map(GolemCoreTask::coreName).toList(),
                "the twelve names are the original's per-meta lang strings, in meta order");
    }

    /** The blank core (meta 100) and "no core" (-1) are both outside the table. */
    @Test
    void blankAndAbsentCoresAreNotTasks() {
        assertEquals(100, GolemCoreTask.BLANK_META, "ItemGolemCore's blank icon is meta 100");
        assertEquals(-1, GolemCoreTask.NO_CORE, "the original's empty core sentinel is -1");
        assertNull(GolemCoreTask.byId(GolemCoreTask.BLANK_META), "the blank core drives no task");
        assertNull(GolemCoreTask.byId(GolemCoreTask.NO_CORE), "an absent core drives no task");
        assertNull(GolemCoreTask.byId(12), "meta 12 is past the last core");
        assertFalse(GolemCoreTask.isCore(GolemCoreTask.BLANK_META), "blank is not a real core");
        assertFalse(GolemCoreTask.isCore(GolemCoreTask.NO_CORE), "absent is not a real core");
        for (int id = 0; id <= 11; id++) {
            assertTrue(GolemCoreTask.isCore(id), "meta " + id + " is a real core");
            assertNotNull(GolemCoreTask.byId(id), "and it resolves through byId");
            assertSame(GolemCoreTask.values()[id], GolemCoreTask.byId(id),
                    "byId(" + id + ") is the enum constant with that id");
        }
    }

    /**
     * The three static switches in {@code ItemGolemCore}:
     * {@code hasGUI} 0,1,2,4,5,8,10; {@code canSort} 0,1,2,8,10;
     * {@code hasInventory} 0,1,2,5,8 ({@code ItemGolemCore.java:66-119}).
     */
    @Test
    void theThreeCorePredicatesMatchTheSourceSwitchSets() {
        assertEquals(List.of(0, 1, 2, 4, 5, 8, 10), idsWhere(GolemCoreTask::hasGui), "ItemGolemCore.hasGUI set");
        assertEquals(List.of(0, 1, 2, 8, 10), idsWhere(GolemCoreTask::canSort), "ItemGolemCore.canSort set");
        assertEquals(List.of(0, 1, 2, 5, 8), idsWhere(GolemCoreTask::hasInventory),
                "ItemGolemCore.hasInventory set");
    }

    /**
     * {@code GolemHelper.getItemsNeeded} routes only cores 1, 8 and 10; every
     * other core returns {@code null} (no need list). Getting this wrong makes
     * the carrier task ask for the wrong items.
     */
    @Test
    void theNeedListRoutingMatchesGolemHelperGetItemsNeeded() {
        assertEquals(GolemCoreTask.ItemsNeeded.GOLEM_INVENTORY, GolemCoreTask.EMPTY.itemsNeededMode(),
                "core 1 sums the golem inventory through the empty-core filter");
        assertEquals(GolemCoreTask.ItemsNeeded.GOLEM_INVENTORY, GolemCoreTask.USE.itemsNeededMode(),
                "core 8 sums the golem inventory through the use-core filter");
        assertEquals(GolemCoreTask.ItemsNeeded.HOME_CONTAINER, GolemCoreTask.SORTING.itemsNeededMode(),
                "core 10 starts from the home container through the sorting-core filter");
        for (final GolemCoreTask core : GolemCoreTask.values()) {
            if (core.id() == 1 || core.id() == 8 || core.id() == 10) {
                continue;
            }
            assertEquals(GolemCoreTask.ItemsNeeded.NONE, core.itemsNeededMode(),
                    "core " + core.id() + " (" + core.coreName() + ") has no need list in the original switch");
        }
    }

    /**
     * Every core's goal list is transcribed from {@code setupGolem()}, so none
     * may be empty. The common tail applies to all of them
     * ({@code EntityGolemBase.java:289-296}).
     */
    @Test
    void everyCoreNamesItsGoalsAndTheCommonTailIsShared() {
        for (final GolemCoreTask core : GolemCoreTask.values()) {
            assertFalse(core.goalNames().isEmpty(),
                    "core " + core.id() + " (" + core.coreName() + ") installs at least one goal");
        }
        assertEquals(List.of("AIHomeReplace", "AIHomePlace", "AIHomeDrop", "AIFillTake", "AIFillGoto"),
                GolemCoreTask.FILL.goalNames(), "core 0 Fill's setupGolem() switch arm, in priority order");
        assertEquals(List.of("AIHomeReplace", "AIHomeTake"),
                List.of(GolemCoreTask.EMPTY.goalNames().get(0), GolemCoreTask.EMPTY.goalNames().get(4)),
                "core 1 Empty installs AIHomeReplace first and AIHomeTake last");
        assertEquals(List.of("AIEssentiaEmpty", "AIEssentiaGather", "AIEssentiaGoto"),
                GolemCoreTask.ALCHEMY.goalNames(), "core 6 Alchemy's essentia trio");
        assertEquals(List.of("AIHomeReplace", "AISortingPlace", "AISortingGoto", "AIHomeTakeSorting"),
                GolemCoreTask.SORTING.goalNames(), "core 10 Sorting's four goals");

        final List<String> common = GolemCoreTask.commonGoalNames();
        assertTrue(common.contains("AIOpenDoor"), "the common tail installs AIOpenDoor at priority 5");
        assertTrue(common.contains("AIReturnHome"), "the common tail installs AIReturnHome at priority 6");
        assertTrue(common.contains("EntityAIWatchClosest"), "the common tail installs EntityAIWatchClosest");
        assertTrue(common.contains("EntityAILookIdle"), "the common tail installs EntityAILookIdle");
        assertTrue(common.contains("AIAvoidCreeperSwell"),
                "AIAvoidCreeperSwell is installed for every core above -1 (EntityGolemBase.java:217-219)");
        assertEquals(List.of("AIDartAttack"), GolemCoreTask.guardDecorationGoalNames(),
                "the Guard core adds AIDartAttack when the decoration carries 'R'");
    }

    /**
     * The act half of each core's task. The card requires a probe per task this
     * card <em>implements</em>, so this test pins the invariant the probes rely
     * on: a core is {@link GolemCoreTask#implemented()} exactly when it carries a
     * real act step. It deliberately does not hard-code <em>which</em> six cores
     * those are, so the follow-up card can widen the set without breaking this.
     */
    @Test
    void aCoreIsImplementedExactlyWhenItCarriesAnActStep() {
        for (final GolemCoreTask core : GolemCoreTask.values()) {
            final boolean hasAct = core.action() != GolemCoreTask.GolemTaskAction.NONE;
            assertEquals(core.implemented(), hasAct,
                    core + " must claim implemented() only when action() names a real act step");
        }
        assertTrue(GolemCoreTask.FILL.implemented(), "the Fill core's act step is ported");
        assertTrue(GolemCoreTask.EMPTY.implemented(), "the Empty core's act step is ported");
        assertTrue(GolemCoreTask.GATHER.implemented(), "the Gather core's act step is ported");
        assertTrue(GolemCoreTask.ALCHEMY.implemented(), "the Alchemy core's act step is ported");
        assertTrue(GolemCoreTask.USE.implemented(), "the Use core's act step is ported");
        assertTrue(GolemCoreTask.SORTING.implemented(), "the Sorting core's act step is ported");

        // The act each implemented core drives, pinned from the source goal it replaces.
        assertEquals(GolemCoreTask.GolemTaskAction.HOME_PLACE, GolemCoreTask.FILL.action(),
                "core 0 deposits through AIHomePlace/AIHomeDrop");
        assertEquals(GolemCoreTask.GolemTaskAction.HOME_PLACE, GolemCoreTask.GATHER.action(),
                "core 2 also deposits through AIHomePlace, after AIItemPickup");
        assertEquals(GolemCoreTask.GolemTaskAction.HOME_TAKE, GolemCoreTask.EMPTY.action(),
                "core 1 withdraws through AIHomeTake");
        assertEquals(GolemCoreTask.GolemTaskAction.HOME_TAKE, GolemCoreTask.USE.action(),
                "core 8 withdraws through AIHomeTake after AIUseItem");
        assertEquals(GolemCoreTask.GolemTaskAction.ESSENTIA_EMPTY, GolemCoreTask.ALCHEMY.action(),
                "core 6 hands its essentia to a jar through AIEssentiaEmpty");
        assertEquals(GolemCoreTask.GolemTaskAction.SORTING_PLACE, GolemCoreTask.SORTING.action(),
                "core 10 sorts through AISortingPlace");
    }

    private static List<Integer> idsWhere(final java.util.function.Predicate<GolemCoreTask> predicate) {
        final List<Integer> ids = new java.util.ArrayList<>();
        for (final GolemCoreTask core : GolemCoreTask.values()) {
            if (predicate.test(core)) {
                ids.add(core.id());
            }
        }
        return ids;
    }
}
