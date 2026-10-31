package com.jonnyutah12.thaumcraftclassic.entities.golems;

import java.util.List;

/**
 * The animation-core → task table, enumerated from the decompiled source
 * (TC-M6-08-s-b). The original has <b>twelve</b> cores, metas {@code 0..11},
 * plus the blank core at meta {@code 100}; this enum is the mapping the card
 * says is "a fact to look up, not to assume".
 *
 * <h2>Where each fact comes from</h2>
 * <ul>
 *   <li>The id set is {@code ItemGolemCore.icon[12]} plus
 *       {@code ItemGolemCore.blankIcon}: {@code ItemGolemCore.java:17-41},
 *       listed in {@code func_150895_a} as meta 100 first and then 0..11
 *       ({@code ItemGolemCore.java:50-56}).</li>
 *   <li>The names are the original's per-meta lang keys
 *       {@code item.ItemGolemCore.<meta>.name} from
 *       {@code reference/assets/thaumcraft/lang/en_US.lang:464-476}:
 *       Fill, Empty, Gather, Harvest, Guard, Decanting, Alchemy, Chop, Use,
 *       Butcher, Sorting, Fishing, and Blank.</li>
 *   <li>{@link #goalNames()} is the goal list installed by
 *       {@code EntityGolemBase.setupGolem()} — the {@code switch
 *       (this.getCore())} at {@code EntityGolemBase.java:220-287}, transcribed
 *       case by case with the original's priorities. The common tail
 *       ({@code EntityGolemBase.java:289-296}) applies to every real core.</li>
 *   <li>{@link #hasGui()} / {@link #canSort()} / {@link #hasInventory()} are
 *       the three static switches in {@code ItemGolemCore.java:66-119}
 *       (hasGUI 0,1,2,4,5,8,10 — canSort 0,1,2,8,10 — hasInventory 0,1,2,5,8).</li>
 *   <li>{@link #itemsNeededMode()} is the {@code switch (golem.getCore())} in
 *       {@code GolemHelper.getItemsNeeded} ({@code GolemHelper.java:624-646}).</li>
 * </ul>
 *
 * <p>The original's {@code setupGolem()} installs vanilla-style goals
 * ({@code EntityAITasks} → 1.21.1's {@code GoalSelector}); it has no custom
 * navigator, and neither does this port. {@link #goalNames()} exists so the
 * runtime and the tests can name the goals a core owes <em>before</em> those
 * goals are ported: a core whose goals are not implemented yet installs
 * nothing, and {@link #implemented()} is false so no caller can mistake it for
 * shipped behaviour.</p>
 */
public enum GolemCoreTask {

    /** 0 Fill — {@code AIHomeReplace, AIHomePlace, AIHomeDrop, AIFillTake, AIFillGoto}. */
    FILL(0, "Fill",
            List.of("AIHomeReplace", "AIHomePlace", "AIHomeDrop", "AIFillTake", "AIFillGoto"),
            ItemsNeeded.NONE, true),
    /** 1 Empty — {@code AIHomeReplace, AIEmptyPlace, AIEmptyDrop, AIEmptyGoto, AIHomeTake}. */
    EMPTY(1, "Empty",
            List.of("AIHomeReplace", "AIEmptyPlace", "AIEmptyDrop", "AIEmptyGoto", "AIHomeTake"),
            ItemsNeeded.GOLEM_INVENTORY, true),
    /** 2 Gather — {@code AIHomeReplace, AIHomePlace, AIItemPickup}. */
    GATHER(2, "Gather",
            List.of("AIHomeReplace", "AIHomePlace", "AIItemPickup"),
            ItemsNeeded.NONE, true),
    /** 3 Harvest — {@code AIHarvestCrops}. */
    HARVEST(3, "Harvest",
            List.of("AIHarvestCrops"),
            ItemsNeeded.NONE, false),
    /** 4 Guard — {@code [AIDartAttack when the decoration has "R"], AIGolemAttackOnCollide} + targets. */
    GUARD(4, "Guard",
            List.of("AIGolemAttackOnCollide", "AIHurtByTarget", "AINearestAttackableTarget"),
            ItemsNeeded.NONE, false),
    /** 5 Decanting — {@code AILiquidEmpty, AILiquidGather, AILiquidGoto}. */
    DECANTING(5, "Decanting",
            List.of("AILiquidEmpty", "AILiquidGather", "AILiquidGoto"),
            ItemsNeeded.NONE, false),
    /** 6 Alchemy — {@code AIEssentiaEmpty, AIEssentiaGather, AIEssentiaGoto}. */
    ALCHEMY(6, "Alchemy",
            List.of("AIEssentiaEmpty", "AIEssentiaGather", "AIEssentiaGoto"),
            ItemsNeeded.NONE, true),
    /** 7 Chop — {@code AIHarvestLogs}. */
    CHOP(7, "Chop",
            List.of("AIHarvestLogs"),
            ItemsNeeded.NONE, false),
    /** 8 Use — {@code AIHomeReplace, AIUseItem, AIHomeTake}. */
    USE(8, "Use",
            List.of("AIHomeReplace", "AIUseItem", "AIHomeTake"),
            ItemsNeeded.GOLEM_INVENTORY, true),
    /** 9 Butcher — {@code [AIDartAttack when the decoration has "R"], AIGolemAttackOnCollide} + {@code AINearestButcherTarget}. */
    BUTCHER(9, "Butcher",
            List.of("AIGolemAttackOnCollide", "AINearestButcherTarget"),
            ItemsNeeded.NONE, false),
    /** 10 Sorting — {@code AIHomeReplace, AISortingPlace, AISortingGoto, AIHomeTakeSorting}. */
    SORTING(10, "Sorting",
            List.of("AIHomeReplace", "AISortingPlace", "AISortingGoto", "AIHomeTakeSorting"),
            ItemsNeeded.HOME_CONTAINER, true),
    /** 11 Fishing — {@code AIFish}. */
    FISHING(11, "Fishing",
            List.of("AIFish"),
            ItemsNeeded.NONE, false);

    /**
     * The original's blank core metadata ({@code ItemGolemCore.func_77617_a(100)}
     * → {@code blankIcon}) — a slotted core item that drives no task.
     */
    public static final int BLANK_META = 100;
    /** Original {@code getCore() == -1} — a golem with no core at all. */
    public static final int NO_CORE = -1;

    /**
     * Which factory {@code GolemHelper.getItemsNeeded} routes through, pinned
     * from {@code GolemHelper.java:624-646}: core 1 sums the golem inventory and
     * filters through the empty-core test; core 8 does the same through the
     * use-core test; core 10 starts from the home container and filters through
     * the sorting-core test; every other core returns {@code null} (no list).
     */
    public enum ItemsNeeded {
        /** {@code default: return needed;} — the switch's fall-through, an unpopulated list. */
        NONE,
        /** Core 1: {@code inventory.getItemsNeeded(fuzzy)} + {@code filterEmptyCore}. */
        GOLEM_INVENTORY,
        /** Core 10: {@code getItemsInHomeContainer(golem)} + {@code filterSortCore}. */
        HOME_CONTAINER
    }

    private final int id;
    private final String name;
    private final List<String> goalNames;
    private final ItemsNeeded itemsNeededMode;
    private final boolean implemented;

    GolemCoreTask(final int id, final String name, final List<String> goalNames,
                  final ItemsNeeded itemsNeededMode, final boolean implemented) {
        this.id = id;
        this.name = name;
        this.goalNames = List.copyOf(goalNames);
        this.itemsNeededMode = itemsNeededMode;
        this.implemented = implemented;
    }

    public int id() {
        return this.id;
    }

    /** The original's English name ({@code item.ItemGolemCore.<id>.name}). */
    public String coreName() {
        return this.name;
    }

    /** The goal class names the original's {@code setupGolem()} installs for this core, in priority order. */
    public List<String> goalNames() {
        return this.goalNames;
    }

    public ItemsNeeded itemsNeededMode() {
        return this.itemsNeededMode;
    }

    /**
     * Whether this card ported the core's task. False means the mapping is
     * pinned but the act/goal classes are not written yet, so the runtime
     * installs nothing for that core and no probe may claim its behaviour.
     */
    public boolean implemented() {
        return this.implemented;
    }

    /** Original {@code ItemGolemCore.hasGUI(int)}. */
    public boolean hasGui() {
        return switch (this.id) {
            case 0, 1, 2, 4, 5, 8, 10 -> true;
            default -> false;
        };
    }

    /** Original {@code ItemGolemCore.canSort(int)}. */
    public boolean canSort() {
        return switch (this.id) {
            case 0, 1, 2, 8, 10 -> true;
            default -> false;
        };
    }

    /** Original {@code ItemGolemCore.hasInventory(int)} — the cores whose golem gets a sized inventory. */
    public boolean hasInventory() {
        return switch (this.id) {
            case 0, 1, 2, 5, 8 -> true;
            default -> false;
        };
    }

    /**
     * The original's {@code switch (this.getCore())} arm that runs its task:
     * the {@code golem_act} step the runtime drives. Only the cores this card
     * ports are listed as {@link #implemented()}; every core's arm is still
     * named here so the enumeration is complete and testable.
     */
    public GolemTaskAction action() {
        return switch (this.id) {
            case 0 -> GolemTaskAction.HOME_PLACE;    // AIHomePlace + AIHomeDrop (FILL deposits)
            case 1 -> GolemTaskAction.HOME_TAKE;      // AIHomeTake
            case 2 -> GolemTaskAction.HOME_PLACE;     // AIHomePlace + AIItemPickup
            case 6 -> GolemTaskAction.ESSENTIA_EMPTY; // AIEssentiaEmpty (the jar arm)
            case 8 -> GolemTaskAction.HOME_TAKE;      // AIHomeTake (after AIUseItem)
            case 10 -> GolemTaskAction.SORTING_PLACE; // AISortingPlace
            default -> GolemTaskAction.NONE;
        };
    }

    /** The seventeen values the original's {@code ItemGolemCore} can carry that are <em>not</em> a core. */
    public static GolemCoreTask byId(final int id) {
        for (final GolemCoreTask task : values()) {
            if (task.id == id) {
                return task;
            }
        }
        return null;
    }

    /** Whether the id is a real core (0..11). Blank (100) and -1 are not. */
    public static boolean isCore(final int id) {
        return byId(id) != null;
    }

    /**
     * The original's {@code setupGolem()} common tail
     * ({@code EntityGolemBase.java:289-296}), installed for every core above
     * {@code -1}: {@code AIOpenDoor(5), AIReturnHome(6),
     * EntityAIWatchClosest(Player, 6.0F)=7, EntityAILookIdle(8)}, plus
     * {@code AIAvoidCreeperSwell(0)} when a core is present
     * ({@code EntityGolemBase.java:217-219}).
     */
    public static List<String> commonGoalNames() {
        return List.of("AIAvoidCreeperSwell", "AIOpenDoor", "AIReturnHome",
                "EntityAIWatchClosest", "EntityAILookIdle");
    }

    /** The goals the Guard core adds when the decoration carries "R" (dart launcher). */
    public static List<String> guardDecorationGoalNames() {
        return List.of("AIDartAttack");
    }

    /**
     * The act step a core's task performs on the world. The original splits
     * each core's work between <em>goto</em> goals (which move the golem along
     * the path) and <em>act</em> goals (which mutate the container); this enum
     * names the act half, which is the half a game test can assert on without
     * depending on pathfinding.
     */
    public enum GolemTaskAction {
        /** The core's act goal is not ported yet. */
        NONE,
        /** Empty the golem's carry stack into a marked container with room. */
        HOME_PLACE,
        /** Take needed items from the container at the home face into the golem inventory. */
        HOME_TAKE,
        /** Push the carried essentia into the nearest jar with room. */
        ESSENTIA_EMPTY,
        /** Distribute the home container's goods into marked containers that already hold them. */
        SORTING_PLACE
    }
}
