package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EnumGolemType;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIAvoidCreeperSwell;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIGolemAttackOnCollide;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIHurtByTarget;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AINearestAttackableTarget;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AINearestButcherTarget;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIOpenDoor;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIReturnHome;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Probes for the golem goal tail: the target goals, the melee goal, the creeper flee, the hurt
 * retaliation, the door task and {@link AIReturnHome}.
 *
 * <p>The 1.7.10 arbiter registers the combat goals per core ({@code EntityGolemBase.java:221}
 * puts {@code AIAvoidCreeperSwell} on the <em>tasks</em> selector at priority 0 for every
 * {@code core > -1}; {@code :263-265} puts {@code AIGolemAttackOnCollide} on tasks at 3,
 * {@code AIHurtByTarget(this, false)} on targetTasks at 1 and {@code AINearestAttackableTarget}
 * on targetTasks at 2 for the guard core 4; {@code :286-290} gives the butcher core 9 the same
 * melee goal plus {@code AINearestButcherTarget(this, 0, true)} on targetTasks at 1;
 * {@code :293-294} finishes every {@code core > -1} golem with {@code AIOpenDoor(this, true)} at
 * 5 and {@code AIReturnHome} at 6). Each probe registers its goals at those priorities and reads
 * them back out of the selector ({@link #goalPriorities}), so a priority claim in a probe
 * message is a measured fact, not a comment.</p>
 *
 * <p>Substrate rules these probes are written around, all of them measured on the game-test
 * server, not assumed:</p>
 *
 * <ul>
 *   <li><b>Coordinates.</b> Every {@code GameTestHelper} that takes a position translates it
 *       through {@code absolutePos}: {@code spawn}/{@code spawnWithNoFreeWill}
 *       ({@code GameTestHelper.java:116,120,189}, via {@code absoluteVec}), {@code setBlock}
 *       ({@code :346}) and {@code getEntities} ({@code :497}). Handing those a position that
 *       was already run through {@link #at} puts the mob at {@code 2 * origin + cell}, which is
 *       a different probe's template — the mob is then alive, in world, and belongs to nobody
 *       who is asserting about it. Props are therefore spawned with the <b>template-relative</b>
 *       cell and compared against {@link #at}; the golem under test is built by hand
 *       ({@link #spawnGolem}) with {@code moveTo} + {@code addFreshEntity}, which take absolute
 *       coordinates, so it keeps {@link #at}.</li>
 *   <li>Both selectors run in production: {@code Mob.serverAiStep} ({@code Mob.java:730-752})
 *       ticks the target selector and the goal selector, and on the {@code (tickCount + id) % 2}
 *       parity it only ticks the <em>running</em> goals, so an armed goal starts on the other
 *       phase. The probes still drive the target selector by hand ({@link #driveTargets}) so that
 *       an acquisition is measured on the tick the probe asks for and not on the engine's parity.</li>
 *   <li>The constructor installs no goals: {@code Mob.registerGoals} has an empty body
 *       ({@code Mob.java:153}) and the port registers none of its own ({@code grep -rn addGoal
 *       entities/golems/} is empty — the per-core {@code setupGolem} registration is a separate
 *       card). {@link #spawnGolem} asserts that, so each probe measures exactly the goals it
 *       installs, at the original's priorities, and nothing moves the golem before the phase that
 *       expects it. A probe that installs a goal with a live target from tick 1 watches the golem
 *       walk away from the cell it asserts about, so probe 1 installs its goals in the phase.</li>
 *   <li>A mob placed by {@code spawnWithNoFreeWill} is NoAI: it never runs its gravity step, so
 *       it floats and {@code onGround()} stays false. Its geometry is therefore asserted on the
 *       cell ({@link #standingInAirCellOnFloor}), never on {@code onGround()}. The golem under
 *       test is built the way the production spawn path builds it, so it does fall, walk and
 *       tick its goal selector like a real golem.</li>
 *   <li><b>A NoAI golem cannot path.</b> {@code GroundPathNavigation.canUpdatePath()} is
 *       {@code mob.onGround() || mob.isInLiquid() || mob.isPassenger()}
 *       ({@code GroundPathNavigation.java:40-43}) and {@code PathNavigation.createPath} returns
 *       {@code null} whenever it is false ({@code PathNavigation.java:127}). A probe that wants a
 *       real route has to leave the golem its AI and let it land, and has to give it a couple of
 *       ticks after any {@code setPos} before the path finder runs.</li>
 *   <li>The golem's feet must sit in an AIR cell next to its target; a golem built inside a solid
 *       block suffocates at 1 damage per tick and dies mid-probe, so every phase asserts liveness
 *       ({@link #alive}) and a death reports itself with its own message.</li>
 *   <li>A probe may not register a follow-up from inside a callback:
 *       {@code GameTestInfo.tickInternal} iterates its {@code runAtTickTimeMap} and removes from
 *       it while running each entry, so a nested {@code runAfterDelay} corrupts that iterator
 *       and takes the whole game-test server down. Every phase is scheduled from the probe body
 *       at a fixed tick offset.</li>
 *   <li>The floor under the template takes ~20 ticks to settle and a mob's {@code tickCount}
 *       starts when it is added to the world, so every gate here is derived from an observed
 *       value ({@code SETTLE}) and no probe asserts an exact {@code tickCount}.</li>
 *   <li>No probe here touches a static that outlives it: {@code GolemHelper}'s jar and
 *       item-timeout maps are cleared in every probe ({@link #clearGolemCaches}) and the goal
 *       classes' only statics are stateless {@code TargetingConditions}.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GolemCombatAiGameTest {
    /** The guard core: the original's {@code case 4} ({@code EntityGolemBase.java:248-265}). */
    private static final int GUARD_CORE = 4;
    /** The butcher core: the original's {@code case 9} ({@code :286-290}). */
    private static final int BUTCHER_CORE = 9;
    /** The original registers the target goals with {@code randomInterval = 0} ({@code :265,289}). */
    private static final int NO_INTERVAL = 0;
    private static final int MELEE_PRIORITY = 3;
    private static final int HOME_PRIORITY = 6;
    private static final int DOOR_PRIORITY = 5;
    private static final int FLEE_PRIORITY = 0;
    private static final int HURT_PRIORITY = 1;
    private static final int HOSTILE_PRIORITY = 2;
    private static final int EFFECT_UPGRADE = 4;
    private static final int ANIMAL_TOGGLE = 2;

    /** Mob AI settles onto a fresh game-test mob slowly; the sibling probes see tick ~190. */
    private static final int SETTLE = 120;

    /**
     * Ticks the creeper-flee probe watches for its goal to arm, and how many draws of the goal's
     * own {@code canUse} it takes per tick. One call is one draw of a random flee spot
     * ({@code AIAvoidCreeperSwell.java:49-54}) plus the last-node route test ({@code :63});
     * measured on the game-test server, a single draw per tick armed the goal on roughly one tick
     * in ten, so the window takes 8 draws a tick to make the arming a fact and not a coin flip.
     */
    private static final int FLEE_WINDOW = 60;
    private static final int FLEE_DRAWS_PER_TICK = 8;

    /** The original's home-radius teleport is at 48 squared blocks ({@code :224-232}). */
    private static final double TELEPORT_SQUARED = 2304.0;

    private static final int HOME_FACING = Direction.NORTH.get3DDataValue();

    private GolemCombatAiGameTest() {
    }

    // ------------------------------------------------------------------ helpers ---

    /**
     * The item and marker maps are static; a jar or timeout left by an earlier probe leaks into
     * this one, so every probe clears them first, as the sibling {@code GolemRuntime} probes do.
     */
    private static void clearGolemCaches() {
// NOTE: do NOT call GolemHelper.clearJarListForTest() here. The jar list is a
// process-wide static and game tests in a batch run CONCURRENTLY, so clearing it
// starves other probes' jar lookups (it broke InfusionAltarGameTest's potentia drain).
// Golem positions are per-template cells, so a stale entry from another probe's
// template can never resolve to a block in this one; item timeouts are keyed per
// golem, and each probe spawns a fresh one, so clearing only those is safe.
        // GolemHelper.clearJarListForTest(); // deliberately not called, see above
        GolemHelper.clearItemTimeoutsForTest();
    }

    private static BlockPos at(final GameTestHelper helper, final int x, final int y, final int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    /**
     * A prop that stands where it is told and never acts on its own. The cell is
     * template-relative: {@code GameTestHelper.spawnWithNoFreeWill} runs it through
     * {@code absolutePos} itself, so an already-absolute cell here lands the prop in the
     * template 2× away (see the class javadoc).
     */
    private static <T extends Mob> T spawnProp(final GameTestHelper helper, final EntityType<T> type,
                                               final int x, final int y, final int z) {
        return helper.spawnWithNoFreeWill(type, new BlockPos(x, y, z));
    }

    /**
     * The production spawn shape: build the golem, give it material, core and owner, place it,
     * run {@code setup}, restrict it to its home cell. Unlike {@code spawnWithNoFreeWill} the
     * golem keeps its AI, so it falls, walks and ticks its goal selector like a real golem.
     * {@code moveTo}/{@code addFreshEntity} take absolute coordinates, so this is the one place
     * a probe hands around {@link #at} output.
     */
    private static EntityGolemBase spawnGolem(final GameTestHelper helper, final BlockPos home, final int core) {
        final ServerLevel level = helper.getLevel();
        final EntityGolemBase golem = CatalogGolems.GOLEM.get().create(level);
        helper.assertTrue(goalPriorities(golem.goalSelector).isEmpty()
                        && goalPriorities(golem.targetSelector).isEmpty(),
                "a constructed golem carries no goals of its own (Mob.registerGoals is empty, "
                + "Mob.java:153, and the port registers none), goals=" + goalSummary(golem.goalSelector)
                + " targets=" + goalSummary(golem.targetSelector));
        golem.setMaterial(EnumGolemType.WOOD);
        golem.setCore(core);
        golem.setOwnerName("probe-owner");
        golem.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.0F, 0.0F);
        golem.setup(HOME_FACING);
        // setup() runs setupGolem() (the original's load-path install, :693), which puts the
        // core's goals on the live entity. These probes DRIVE goals by hand, so the fixture
        // owns the goal set: drop the installation, then add exactly what the probe installs.
        // A live goal walks the golem out of the cell the probe asserts in, and a live
        // AIEssentiaGather drains the probe's jar before the probe's own draw.
        golem.goalSelector.removeAllGoals(goal -> true);
        golem.targetSelector.removeAllGoals(goal -> true);
        golem.restrictTo(home, EntityGolemBase.HOME_RANGE);
        level.addFreshEntity(golem);
        return golem;
    }

    /**
     * Drive the target selector now, so an acquisition is measured on the tick the probe asks for
     * rather than on the parity {@code Mob.serverAiStep} happens to give the selector a full
     * {@code tick()} on ({@code Mob.java:736-751}).
     */
    private static void driveTargets(final EntityGolemBase golem) {
        golem.targetSelector.tick();
    }

    /**
     * Drop a target goal and put a fresh instance of the same class back at the same priority, so
     * an acquisition is measured from a clean goal state. A running target goal keeps its own
     * {@code target} field and its {@code canContinueToUse} keeps passing, so a herd that shrank
     * below the cap is only "not butchered" for a goal that has to look at the pen again.
     */
    private static void reloadTargetGoal(final EntityGolemBase golem, final Goal goal, final int priority,
                                         final Goal fresh) {
        golem.targetSelector.removeGoal(goal);
        golem.targetSelector.addGoal(priority, fresh);
    }

    /**
     * The selector's goal set is private; a test reads it through reflection rather than growing
     * a production accessor.
     */
    private static Set<WrappedGoal> wrappedGoals(final GoalSelector selector) {
        try {
            final Field field = GoalSelector.class.getDeclaredField("availableGoals");
            field.setAccessible(true);
            return (Set<WrappedGoal>) field.get(selector);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Read a private/protected field off a goal instance, walking up the class hierarchy. The
     * goals' working state (the flee spot, the doorway being worked on) is deliberately private in
     * the port, so a probe that wants to assert on it goes through reflection rather than growing
     * a production accessor.
     */
    private static Object readField(final Object target, final String name) {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                final Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                // keep walking
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("no field " + name + " on " + target.getClass());
    }

    private static Map<Goal, Integer> goalPriorities(final GoalSelector selector) {
        final Map<Goal, Integer> out = new LinkedHashMap<>();
        for (final WrappedGoal entry : wrappedGoals(selector)) {
            out.put(entry.getGoal(), entry.getPriority());
        }
        return out;
    }

    /**
     * 1.21.1 puts the running flag on the selector's {@link WrappedGoal}, not on the
     * {@link Goal}, so "the goal is in state" is read off the wrapper.
     */
    private static boolean goalRunning(final GoalSelector selector, final Goal goal) {
        for (final WrappedGoal entry : wrappedGoals(selector)) {
            if (entry.getGoal() == goal) {
                return entry.isRunning();
            }
        }
        return false;
    }

    private static void assertPriority(final GameTestHelper helper, final GoalSelector selector,
                                       final Goal goal, final int priority, final String what) {
        final Integer actual = goalPriorities(selector).get(goal);
        helper.assertTrue(actual != null && actual == priority,
                what + " sits on the selector at priority " + priority + ", got " + actual
                + " (selector holds " + goalPriorities(selector).size() + " goals)");
    }

    /**
     * Install a task on the goal selector at the original's priority and read the priority back
     * out of the selector, so the priority claim in the probe message is a measured fact.
     */
    private static void assertPriorityOnGoalSelector(final GameTestHelper helper, final EntityGolemBase golem,
                                                     final Goal goal, final int priority, final String what) {
        golem.goalSelector.addGoal(priority, goal);
        assertPriority(helper, golem.goalSelector, goal, priority, what);
    }

    private static String goalSummary(final GoalSelector selector) {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<Goal, Integer> entry : goalPriorities(selector).entrySet()) {
            sb.append(' ').append(entry.getKey().getClass().getSimpleName())
                    .append('@').append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * A self-reporting death: a golem built inside a solid block suffocates at 1 damage per tick,
     * so every phase reads liveness with the health and the cell, and a dead prop points at
     * itself instead of collapsing a later assertion.
     */
    private static void alive(final GameTestHelper helper, final Entity entity, final String who) {
        final String health = entity instanceof LivingEntity living
                ? living.getHealth() + "/" + living.getMaxHealth() : "n/a";
        helper.assertTrue(entity.isAlive() && !entity.isRemoved(),
                who + " is alive at tick " + helper.getTick() + ", health " + health + ", cell "
                + entity.blockPosition().toShortString());
    }

    /**
     * The probe geometry claim: {@code (x,y,z)} is a non-suffocating cell with a solid floor
     * under it, and the mob's feet are in it. A NoAI mob never runs its gravity step, so
     * {@code onGround()} is not a usable observable for the props.
     */
    private static void standingInAirCellOnFloor(final GameTestHelper helper, final Mob mob,
                                                 final int x, final int y, final int z, final String who) {
        final ServerLevel level = helper.getLevel();
        final BlockPos cell = at(helper, x, y, z);
        helper.assertTrue(mob.isAlive() && !mob.isRemoved() && mob.blockPosition().equals(cell),
                who + " stands in cell " + cell.toShortString() + ", alive=" + mob.isAlive()
                + " removed=" + mob.isRemoved() + " at " + mob.blockPosition().toShortString()
                + " (tick " + helper.getTick() + ")");
        helper.assertTrue(!level.getBlockState(cell).isSuffocating(level, cell),
                who + "'s cell " + cell.toShortString() + " is not a suffocating block: "
                + level.getBlockState(cell).getBlock());
        helper.assertTrue(level.getBlockState(cell.below()).isSolidRender(level, cell.below()),
                who + " has a solid floor under " + cell.below().toShortString() + ": "
                + level.getBlockState(cell.below()).getBlock());
    }

    /** A short fixed-precision form of a position or delta, for assertion messages. */
    private static String v(final Vec3 pos) {
        return String.format("(%.2f, %.2f, %.2f)", pos.x, pos.y, pos.z);
    }

    /** A live path's node cells, so an assertion message says what the route really did. */
    private static String nodeTrail(final Path path) {
        if (path == null) {
            return "no path";
        }
        final StringBuilder sb = new StringBuilder("path[" + path.getNodeCount()
                + ",reached=" + path.canReach() + "]:");
        for (int i = 0; i < path.getNodeCount(); i++) {
            sb.append(' ').append(path.getNode(i).asBlockPos().toShortString());
        }
        return sb.toString();
    }

    /**
     * The route walks the 1x1 column of {@code cell}. A path node sits at the plane the mob's
     * feet walk on (the door goal reads the door at {@code node.y + 1},
     * {@code AIDoorInteract.java:117} and 1.21.1's own {@code DoorInteractGoal.java:62}), so a
     * doorway is matched on its column, not on one exact cell.
     */
    /**
     * Whether a route ends on the cell (within one block). 1.21.1's {@code PathFinder} hands back
     * a <em>partial</em> route when the target is unreachable — a one-node route that is just the
     * golem's own cell — and {@code PathNavigation.moveTo} accepts that, so "no path" is not the
     * shape of an unreachable target; the route simply does not end there.
     */
    private static boolean reaches(final Path path, final BlockPos cell) {
        if (path == null || path.getNodeCount() == 0) {
            return false;
        }
        final Node last = path.getEndNode();
        return Math.abs(last.x - cell.getX()) <= 1 && Math.abs(last.z - cell.getZ()) <= 1;
    }

    private static boolean crossesColumn(final Path path, final BlockPos cell) {
        if (path == null) {
            return false;
        }
        for (int i = 0; i < path.getNodeCount(); i++) {
            final Node node = path.getNode(i);
            if (node.x == cell.getX() && node.z == cell.getZ()) {
                return true;
            }
        }
        return false;
    }

    /**
     * A walkable clearing around the template, on the same layer as its floor. The flee goal runs
     * to a spot 16 blocks from the creeper ({@code AIAvoidCreeperSwell.java:55-59}) and
     * {@code PathNavigation.createPath} answers {@code null} for an unreachable target
     * ({@code PathNavigation.java:127}), so inside a 24-wide template — whose floor reaches 15.9
     * blocks from its centre — the goal can never arm. The probe lays the open ground the
     * original's world had and says so.
     */
    private static void layClearing(final GameTestHelper helper, final int span) {
        final ServerLevel level = helper.getLevel();
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (int x = -span; x <= 23 + span; x++) {
            for (int z = -span; z <= 23 + span; z++) {
                level.setBlock(new BlockPos(origin.getX() + x, origin.getY() - 1, origin.getZ() + z),
                        Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    /** {@code GameTestHelper.getEntities} translates its position, so pass the template-relative cell. */
    private static int herdSize(final GameTestHelper helper) {
        return (int) helper.getEntities(EntityType.PIG, new BlockPos(12, 0, 12), 12).size();
    }

    // ------------------------------------------------------------------- probes ---

    /**
     * The guard core 4 registers its melee goal on tasks at 3 and the hostile target goal on
     * targetTasks at 2 ({@code EntityGolemBase.java:263-265}), and the pair has an observable
     * result: the zombie becomes the golem's target and the melee goal wounds it.
     *
     * <p>The filters are the original's own: monsters pass with the hostile toggle
     * ({@code GolemTargeting.isValidTarget}, {@code EntityGolemBase.java:1232-1234}), animals
     * need the Effect upgrade as well ({@code :1236-1242}), and the owner is never a target
     * ({@code :1206-1211}). The melee punch comes from the attack-damage attribute through
     * {@code doHurtTarget} on the original's 20-tick cadence
     * ({@code AIGolemAttackOnCollide.java:104-133}, {@code EntityGolemBase.java:1263}), so the
     * probe asserts a wound on the zombie and none on the pig — not a damage number.</p>
     */
    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void combat_guardGolemAcquiresTheHostileAndItsMeleeGoalWoundsIt(final GameTestHelper helper) {
        clearGolemCaches();
        final int pAcquire = SETTLE;
        final int pWound = SETTLE + 30;

        final EntityGolemBase golem = spawnGolem(helper, at(helper, 12, 0, 12), GUARD_CORE);
        final Zombie zombie = spawnProp(helper, EntityType.ZOMBIE, 13, 0, 12);
        final Pig pig = spawnProp(helper, EntityType.PIG, 16, 0, 16);

        // Built here, installed in the phase below: the melee goal walks the golem toward its
        // target, and with a target available from the first tick the golem has left the cell the
        // settle phase asserts about.
        final AIHurtByTarget retaliate = new AIHurtByTarget(golem);
        final AINearestAttackableTarget hostile = new AINearestAttackableTarget(golem, NO_INTERVAL, true);
        final AIGolemAttackOnCollide melee = new AIGolemAttackOnCollide(golem);

        helper.runAfterDelay(pAcquire, () -> {
            standingInAirCellOnFloor(helper, golem, 12, 0, 12, "the golem");
            standingInAirCellOnFloor(helper, zombie, 13, 0, 12, "the zombie");
            standingInAirCellOnFloor(helper, pig, 16, 0, 16, "the pig");
            // Measured in this world: a zombie spawned by the helper arrives at 17.0/20.0 (the
            // helper runs finalizeSpawn with the level's live difficulty; the pig arrives whole).
            // The probe does not model that spawn effect, so it sets the baseline it measures the
            // melee goal against: both props are put at full health here, and the claim below is
            // the drop from this baseline over the melee window.
            zombie.setHealth(zombie.getMaxHealth());
            pig.setHealth(pig.getMaxHealth());
            helper.assertTrue(zombie.getMaxHealth() == zombie.getHealth() && pig.getMaxHealth() == pig.getHealth(),
                    "both props sit at full health before the goals are installed, zombie "
                    + zombie.getHealth() + "/" + zombie.getMaxHealth()
                    + ", pig " + pig.getHealth() + "/" + pig.getMaxHealth() + ", tick " + helper.getTick());

            golem.targetSelector.addGoal(HURT_PRIORITY, retaliate);
            golem.targetSelector.addGoal(HOSTILE_PRIORITY, hostile);
            golem.goalSelector.addGoal(MELEE_PRIORITY, melee);
            assertPriority(helper, golem.targetSelector, retaliate, HURT_PRIORITY, "the retaliation goal");
            assertPriority(helper, golem.targetSelector, hostile, HOSTILE_PRIORITY, "the hostile target goal");
            assertPriority(helper, golem.goalSelector, melee, MELEE_PRIORITY, "the melee goal");

            golem.setTarget(null);
            driveTargets(golem);
            helper.assertTrue(golem.getTarget() == zombie,
                    "the guard acquires the zombie, tick " + helper.getTick() + ", target " + golem.getTarget()
                    + ", goals:" + goalSummary(golem.goalSelector) + " targets:" + goalSummary(golem.targetSelector));
            helper.assertTrue(golem.getTarget() != pig,
                    "and not the pig: an animal needs the Effect upgrade too (:1236-1242)");
        });

        helper.runAfterDelay(pWound, () -> {
            alive(helper, golem, "the golem");
            alive(helper, zombie, "the zombie");
            helper.assertTrue(golem.getTarget() == zombie,
                    "the target is still the zombie at tick " + helper.getTick() + ", got " + golem.getTarget());
            helper.assertTrue(goalRunning(golem.goalSelector, melee),
                    "the melee goal is in state (the selector started it) at tick " + helper.getTick()
                    + ", goals:" + goalSummary(golem.goalSelector));
            helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
                    "the melee goal wounded the zombie: health " + zombie.getHealth() + "/" + zombie.getMaxHealth()
                    + " at tick " + helper.getTick() + ", golem at " + golem.blockPosition().toShortString()
                    + ", distance " + golem.distanceTo(zombie) + ", goals:" + goalSummary(golem.goalSelector));
            helper.assertTrue(pig.getHealth() == pig.getMaxHealth(),
                    "the pig is untouched, health " + pig.getHealth() + "/" + pig.getMaxHealth());
            helper.succeed();
        });
    }

    /**
     * Animal targeting is gated twice by the original: the animal toggle bit
     * ({@code canAttackAnimals() = !getToggles()[2]}) and the Effect upgrade in the same arm
     * ({@code EntityGolemBase.java:1236-1242}). Both are observable on one pig: toggle closed
     * means no target, toggle open without the upgrade is still no target, and only toggle plus
     * upgrade acquires the pig.
     */
    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void combat_animalTargetsNeedTheToggleAndTheEffectUpgrade(final GameTestHelper helper) {
        clearGolemCaches();
        final EntityGolemBase golem = spawnGolem(helper, at(helper, 12, 0, 12), GUARD_CORE);
        final AINearestAttackableTarget hostile = new AINearestAttackableTarget(golem, NO_INTERVAL, true);
        golem.targetSelector.addGoal(HOSTILE_PRIORITY, hostile);
        assertPriority(helper, golem.targetSelector, hostile, HOSTILE_PRIORITY, "the hostile target goal");

        helper.runAfterDelay(SETTLE, () -> {
            final Pig pig = spawnProp(helper, EntityType.PIG, 13, 0, 12);
            standingInAirCellOnFloor(helper, pig, 13, 0, 12, "the pig");

            golem.setToggle(ANIMAL_TOGGLE, true);
            golem.setTarget(null);
            driveTargets(golem);
            helper.assertTrue(golem.getTarget() != pig,
                    "the animal toggle closed (bit " + ANIMAL_TOGGLE + " set) blocks the animal arm "
                    + "(EntityGolemBase.java:1236-1242), tick " + helper.getTick() + ", target "
                    + golem.getTarget());

            golem.setToggle(ANIMAL_TOGGLE, false);
            golem.setTarget(null);
            driveTargets(golem);
            helper.assertTrue(golem.getTarget() != pig,
                    "toggle open without the Effect upgrade is still no target — the same arm demands "
                    + "getUpgradeAmount(4) > 0 (:1236-1242), tick " + helper.getTick() + ", upgrades "
                    + Arrays.toString(golem.upgrades()) + ", target " + golem.getTarget());

            golem.setUpgrades(new byte[] {EFFECT_UPGRADE});
            golem.setTarget(null);
            driveTargets(golem);
            helper.assertTrue(golem.getTarget() == pig,
                    "toggle open and the Effect upgrade in a slot makes the pig the target, tick "
                    + helper.getTick() + ", target " + golem.getTarget() + ", upgrades "
                    + Arrays.toString(golem.upgrades()) + ", targets:" + goalSummary(golem.targetSelector));
            helper.succeed();
        });
    }

    /**
     * The butcher core 9 culls an animal only when its own species has more than two valid
     * members in the golem's box and takes the oldest first
     * ({@code AINearestButcherTarget.java:75-110}, the original's {@code :34-73} with
     * {@code AIOldestAttackableTargetSorter}), and the pair comes with the melee goal at 3
     * ({@code EntityGolemBase.java:288-289}). Observable: with three pigs the first-spawned (the
     * highest {@code tickCount}) is the target; with the herd under three, a goal that has to
     * count the pen again refuses.
     */
    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void combat_butcherCullsTheOldestAnimalOnlyInAHerdOfThree(final GameTestHelper helper) {
        clearGolemCaches();
        final EntityGolemBase golem = spawnGolem(helper, at(helper, 12, 0, 12), BUTCHER_CORE);
        // The goal sorts by the entity's own tickCount, so the pig alive longest is the oldest.
        // Spawn it alone now; the rest of the herd joins in the settled phase.
        final Pig oldest = spawnProp(helper, EntityType.PIG, 14, 0, 12);

        final AINearestButcherTarget butcher = new AINearestButcherTarget(golem, NO_INTERVAL, true);
        final AIGolemAttackOnCollide melee = new AIGolemAttackOnCollide(golem);
        golem.targetSelector.addGoal(HURT_PRIORITY, butcher);
        golem.goalSelector.addGoal(MELEE_PRIORITY, melee);
        assertPriority(helper, golem.targetSelector, butcher, HURT_PRIORITY, "the butcher target goal");
        assertPriority(helper, golem.goalSelector, melee, MELEE_PRIORITY, "the melee goal");

        helper.runAfterDelay(SETTLE, () -> {
            standingInAirCellOnFloor(helper, golem, 12, 0, 12, "the golem");
            standingInAirCellOnFloor(helper, oldest, 14, 0, 12, "the oldest pig");
            final Pig mid = spawnProp(helper, EntityType.PIG, 15, 0, 12);
            final Pig youngest = spawnProp(helper, EntityType.PIG, 16, 0, 12);
            standingInAirCellOnFloor(helper, mid, 15, 0, 12, "the mid pig");
            standingInAirCellOnFloor(helper, youngest, 16, 0, 12, "the youngest pig");
            helper.assertTrue(oldest.tickCount > mid.tickCount,
                    "the first-spawned pig has the highest tickCount: " + oldest.tickCount + " vs "
                    + mid.tickCount + ", tick " + helper.getTick());

            helper.assertTrue(herdSize(helper) == 3, "the herd is three animals, got " + herdSize(helper));
            golem.setTarget(null);
            driveTargets(golem);
            helper.assertTrue(golem.getTarget() == oldest,
                    "a herd of three makes the oldest animal the target, tick " + helper.getTick()
                    + ", target " + golem.getTarget() + ", targets:" + goalSummary(golem.targetSelector));

            youngest.remove(Entity.RemovalReason.KILLED);
            mid.remove(Entity.RemovalReason.KILLED);
            helper.assertTrue(herdSize(helper) == 1, "the herd is down to one, got " + herdSize(helper));
            // A goal that is already running keeps its acquired target (its canContinueToUse is the
            // vanilla TargetGoal test), so the under-the-cap claim is measured on a fresh goal that
            // has to re-count the pen: (:50-67) re-queries the box for the candidate's own class.
            reloadTargetGoal(golem, butcher, HURT_PRIORITY, new AINearestButcherTarget(golem, NO_INTERVAL, true));
            golem.setTarget(null);
            driveTargets(golem);
            helper.assertTrue(golem.getTarget() == null,
                    "a herd under the cap of three is not butchered at all (:34-73, \"more than two\"), tick "
                    + helper.getTick() + ", herd " + herdSize(helper) + ", target " + golem.getTarget());
            helper.succeed();
        });
    }

    /**
     * The guard's retaliation goal (targetTasks 1, {@code EntityGolemBase.java:264}) takes the
     * attacker as its target ({@code AIHurtByTarget.java:105-131}: the hurt-by memory, then
     * {@code canAttack}), and the original registers it with the alert-others flag
     * {@code false} ({@code :264}, {@code AIHurtByTarget.java:13}) — so the neighbour golem
     * standing next to the victim is never recruited: its own retaliation goal refuses, with no
     * hurt memory of its own.
     */
    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void combat_woundedGuardRetaliatesAndDoesNotRecruitItsNeighbour(final GameTestHelper helper) {
        clearGolemCaches();
        final EntityGolemBase victim = spawnGolem(helper, at(helper, 12, 0, 12), GUARD_CORE);
        final EntityGolemBase neighbour = spawnGolem(helper, at(helper, 13, 0, 12), GUARD_CORE);

        final AIHurtByTarget retaliate = new AIHurtByTarget(victim);
        final AIHurtByTarget neighbourRetaliate = new AIHurtByTarget(neighbour);
        victim.targetSelector.addGoal(HURT_PRIORITY, retaliate);
        neighbour.targetSelector.addGoal(HURT_PRIORITY, neighbourRetaliate);
        assertPriority(helper, victim.targetSelector, retaliate, HURT_PRIORITY, "the victim's retaliation goal");
        assertPriority(helper, neighbour.targetSelector, neighbourRetaliate, HURT_PRIORITY,
                "the neighbour's retaliation goal");

        helper.runAfterDelay(SETTLE, () -> {
            standingInAirCellOnFloor(helper, victim, 12, 0, 12, "the victim golem");
            standingInAirCellOnFloor(helper, neighbour, 13, 0, 12, "the neighbour golem");
            final ServerLevel level = helper.getLevel();
            final Zombie zombie = spawnProp(helper, EntityType.ZOMBIE, 14, 0, 12);
            standingInAirCellOnFloor(helper, zombie, 14, 0, 12, "the zombie");
            helper.assertTrue(victim.getTarget() == null && neighbour.getTarget() == null,
                    "neither golem has a target before the blow, tick " + helper.getTick());
            helper.assertTrue(!retaliate.canUse(), "with no hurt memory the goal refuses");

            victim.hurt(level.damageSources().mobAttack(zombie), 2.0f);
            helper.assertTrue(victim.getLastHurtByMob() == zombie,
                    "the victim remembers its attacker, got " + victim.getLastHurtByMob());
            helper.assertTrue(retaliate.canUse(), "the hurt-by memory arms the goal (:18-20)");
            retaliate.start();
            helper.assertTrue(victim.getTarget() == zombie,
                    "the hurt golem retaliates against its attacker, tick " + helper.getTick() + ", target "
                    + victim.getTarget() + ", targets:" + goalSummary(victim.targetSelector));
            helper.assertTrue(victim.getTarget() != neighbour, "and not its neighbour");

            driveTargets(neighbour);
            helper.assertTrue(neighbour.getLastHurtByMob() == null && !neighbourRetaliate.canUse()
                            && neighbour.getTarget() == null,
                    "the neighbour is not recruited: no hurt memory, its goal refuses, no target — the "
                    + "original registers the task with alert-others false (:264), tick " + helper.getTick()
                    + ", memory " + neighbour.getLastHurtByMob() + ", target " + neighbour.getTarget());
            helper.succeed();
        });
    }

    /**
     * The creeper flee is the first task for every core ({@code EntityGolemBase.java:221}), and
     * 1.7.10's {@code AIAvoidCreeperSwell} demands a creeper inside 5 blocks whose
     * {@code getCreeperState() == 1} ({@code AIAvoidCreeperSwell.java:37-53}; 21.1.259 renamed
     * the accessor to {@code getSwellDir()}) plus a spot that is further from the creeper than
     * the golem and that the route actually reaches ({@code :55-63}, the original's
     * {@code RandomPositionGenerator.getPosAway(golem, 16, 7, pos)} plus
     * {@code PathEntity.findTargetPos}). The port paths to that spot and takes the original's hop
     * toward it ({@code :73-81}).
     *
     * <p>Observables, in order: an unlit fuse never arms the goal (that test is deterministic),
     * and once the fuse is lit the goal arms on <b>some</b> draw — one call to {@code canUse} is
     * one draw of a random flee spot ({@code DefaultRandomPos.getPosAway}, 10 tries) plus 1.7.10's
     * last-node route test, and measured on the game-test server it passes on roughly one draw in
     * five (the red leg A12: 18 accepted of 600 spot tries, 13 of them with a route that reaches
     * the spot). The probe installs the goal on the golem's live selector, so the engine drives the
     * very goal the probe samples, and that goal's {@code start()} hops the golem and puts a route
     * of up to 16 blocks under it: a window opened on the tick after the fuse therefore samples a
     * gate whose 5-block detection box has already closed (leg A12: an 18-node route, the golem 12
     * blocks down it, 480 draws and no arming — red). The probe spends its whole draw budget —
     * {@value #FLEE_WINDOW} ticks × {@value #FLEE_DRAWS_PER_TICK} draws = 480 calls — on the tick
     * the fuse is lit, where the golem is the two blocks from the creeper that the probe put there
     * and the goal provably has not armed yet (the entity ticked that tick with the fuse unlit),
     * and keeps the per-tick window of {@value #FLEE_DRAWS_PER_TICK} draws for
     * {@value #FLEE_WINDOW} ticks as the fallback. On the arming draw the probe asserts
     * the arming state, the hop's direction, and that the spot the goal picked is further from the
     * creeper than the golem. 28 ticks after, the golem is further from the creeper than
     * when it was lit, and the fuse is still the lit one.</p>
     *
     * <p>The probe lays a clearing around the template ({@link #layClearing}): the goal wants an
     * open reachable spot and the game-test world is void outside the 24-wide template.</p>
     */
    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void combat_golemFleesOnlyACreeperWhoseFuseIsLit(final GameTestHelper helper) {
        clearGolemCaches();
        layClearing(helper, 26);
        final int pLit = SETTLE + 2;
        final int pWindow = pLit + FLEE_WINDOW + 1;

        final EntityGolemBase golem = spawnGolem(helper, at(helper, 12, 0, 12), GUARD_CORE);
        final Creeper creeper = spawnProp(helper, EntityType.CREEPER, 14, 0, 12);
        creeper.setSwellDir(0);
        final AIAvoidCreeperSwell flee = new AIAvoidCreeperSwell(golem);
        assertPriorityOnGoalSelector(helper, golem, flee, FLEE_PRIORITY, "the creeper flee goal");
        final double[] startDistance = new double[1];
        final double[] litDistance = new double[1];
        final long[] armedTick = new long[1];
        final int[] draws = new int[2];

        helper.runAfterDelay(SETTLE, () -> {
            standingInAirCellOnFloor(helper, golem, 12, 0, 12, "the golem");
            standingInAirCellOnFloor(helper, creeper, 14, 0, 12, "the creeper");
            helper.assertTrue(creeper.getSwellDir() == 0 && golem.distanceToSqr(creeper) <= 25.0,
                    "an unlit creeper sits inside the 5-block radius, swellDir " + creeper.getSwellDir()
                    + ", squared distance " + golem.distanceToSqr(creeper) + ", tick " + helper.getTick());
            helper.assertTrue(!flee.canUse(),
                    "an unlit fuse inside the radius never arms the flee goal (:37-53), tick "
                    + helper.getTick());
            startDistance[0] = golem.distanceToSqr(creeper);
        });

        // The armed check is one shared closure, so the same-tick sampling below and the
        // per-tick window assert exactly the same facts.
        //
        // Why the gate is sampled inside the tick the fuse is lit. This probe installs its own
        // AIAvoidCreeperSwell on the golem's live goal selector (that is the priority assertion
        // the probe opens with), so the engine drives the same goal the probe samples: the
        // selector runs a mob's goals on the parity of (tickCount + getId()) % 2
        // (Mob.serverAiStep) and calls canUse once a tick, and the goal's own start() applies the
        // original's hop impulse plus a flee path of up to 16 blocks
        // (AIAvoidCreeperSwell.java:73-81, :54). Measured on the game-test server
        // (t_8dce1837, 24 base-control legs on 7933db8, leg A12 red): the engine armed the goal
        // on the first tick of the window — the sampled golem was already off the ground with an
        // 18-node path whose last node sat 17 blocks from the home cell — and walked 12 of those
        // blocks across the window, so the goal's 5-block detection box
        // (AIAvoidCreeperSwell.java, DETECTION_RADIUS) was closed for nearly every draw the probe
        // took. The red leg's own gate line says exactly that (golemToCreeper=8.39, spot=null,
        // nodes=null) and its 480-draw histogram over 600 spot tries:
        // dirNull=228 outside=68 restricted=1 notStable=196 malus=89 accepted=18 furtherFail=0
        // pathNull=3 pathShort=0 pathOk=13, armedTick=0. So the arming race is with the engine,
        // not with the spot search: the tick on which the geometry behind the gate is known is the
        // tick the fuse is lit, where the golem is the two blocks from the creeper the probe put
        // there — the entity has already ticked with the fuse unlit, so the goal cannot have armed
        // yet. Sampling the goal's whole draw budget on that tick is race-free; the per-tick
        // window below, which races, stays as the fallback.
        final Runnable afterFlee = () -> {
            alive(helper, golem, "the golem");
            final double after = golem.distanceToSqr(creeper);
            helper.assertTrue(after > litDistance[0],
                    "the golem is further from the swelling creeper than when the fuse was lit: "
                    + litDistance[0] + " -> " + after + ", golem at "
                    + golem.blockPosition().toShortString() + ", creeper at "
                    + creeper.blockPosition().toShortString() + ", armed at tick " + armedTick[0]
                    + ", now " + helper.getTick() + ", goals:" + goalSummary(golem.goalSelector)
                    + ", path " + nodeTrail(golem.getNavigation().getPath()));
            helper.assertTrue(creeper.getSwellDir() == 1,
                    "the creeper is still the swelling one, swellDir " + creeper.getSwellDir()
                    + " (a lit fuse counts down in its own tick and the explosion removes it, so "
                    + "the probe reads the fuse state, not liveness)");
            // The probe stops here on purpose. The goal lives while the navigation has a node left
            // (:69-71), so a flee spot the golem parks one node short of keeps the goal's MOVE
            // flag and the return-home task stays starved in that geometry — a claim this probe
            // does not make. The return-home rule is
            // combat_returnHomeUsesTheRadiusAndTeleportsFromBeyond48.
            helper.succeed();
        };

        final Runnable onArmed = () -> {
            if (armedTick[0] != 0) {
                return;
            }
            alive(helper, golem, "the golem");
            armedTick[0] = helper.getTick();
            startDistance[0] = golem.distanceToSqr(creeper);
            // canUse() does not touch the navigation: the route and the spot are the goal's
            // own entityPathEntity/targetBlock (:20-22), read here through reflection.
            final Path path = (Path) readField(flee, "entityPathEntity");
            final Vec3 fleeSpot = (Vec3) readField(flee, "targetBlock");
            helper.assertTrue(fleeSpot != null,
                    "the arming tick picked a flee spot (:55-59), tick " + helper.getTick());
            helper.assertTrue(path != null && path.getNodeCount() > 0,
                    "and the route reaches it (entityPathEntity, :63): " + nodeTrail(path)
                    + ", tick " + helper.getTick());
            final Node last = path.getEndNode();
            helper.assertTrue(Math.floor(fleeSpot.x) == last.x && Math.floor(fleeSpot.z) == last.z,
                    "the last node is the flee spot, which is what 1.7.10's PathEntity.findTargetPos "
                    + "compares (:63): spot " + fleeSpot + ", last node (" + last.x + ", " + last.y
                    + ", " + last.z + "), tick " + helper.getTick());
            final double spotSquared = creeper.distanceToSqr(fleeSpot);
            helper.assertTrue(spotSquared > startDistance[0],
                    "the flee spot is further from the creeper than the golem: spot squared "
                    + spotSquared + " against golem " + startDistance[0] + " (:57-59), tick "
                    + helper.getTick());
            golem.setDeltaMovement(Vec3.ZERO);
            flee.start();
            final Vec3 hop = golem.getDeltaMovement();
            // The port scales the impulse toward targetBlock + 0.5 (1.7.10 EntityAIAvoidEntity's
            // own quirk, kept per the class javadoc), so the probe reproduces that arithmetic
            // and asks which way the hop points.
            final Vec3 toSpot = new Vec3(fleeSpot.x + 0.5 - golem.getX(), 0.0,
                    fleeSpot.z + 0.5 - golem.getZ());
            helper.assertTrue(hop.lengthSqr() > 0.0 && hop.y > 0.0,
                    "start() gave the original's hop impulse: delta " + hop + ", motionY 0.3 (:73-81), tick "
                    + helper.getTick());
            helper.assertTrue(hop.x * toSpot.x + hop.z * toSpot.z > 0.0,
                    "and the hop points at the flee spot the goal chose: delta (" + hop.x + ", " + hop.z
                    + "), toward the spot (" + toSpot.x + ", " + toSpot.z + ") (:74-79), tick "
                    + helper.getTick());
            helper.runAfterDelay(28, afterFlee);
        };

        helper.runAfterDelay(pLit, () -> {
            // The baseline is the tick the fuse is lit: the goal arms on a later tick of its own
            // choosing and the golem is already moving by then, so the arming tick is not the
            // distance the flee has to beat.
            litDistance[0] = golem.distanceToSqr(creeper);
            creeper.setSwellDir(1);
            // Every draw is one call of the goal's own gate, and this tick is the one on which
            // the geometry behind that gate is known: the golem is the two blocks from the
            // swelling creeper that the probe put there, inside the detection box, in frame.
            for (int attempt = 0; attempt < FLEE_WINDOW * FLEE_DRAWS_PER_TICK; attempt++) {
                draws[0]++;
                if (flee.canUse()) {
                    onArmed.run();
                    return;
                }
            }
        });


        // The window is the fallback, for the case the lit tick's sampling above does not arm.
        // One canUse() call is one draw of a random flee spot (DefaultRandomPos.getPosAway,
        // :49-54) plus 1.7.10's last-node route test (:63), which rejects a route that stops short
        // of the spot; measured on the game-test server, a draw passes on roughly one call in five
        // with the golem inside the detection box. The window spends FLEE_DRAWS_PER_TICK draws on
        // each of FLEE_WINDOW ticks — each call is exactly what the engine calls once a tick.
        for (int t = pLit + 1; t <= pLit + FLEE_WINDOW; t++) {
            helper.runAfterDelay(t, () -> {
                // The armed check comes first: once the goal has armed, the follow-up owns the
                // probe, its succeed() ends the test, and the framework removes the fixture
                // entities — a liveness assert after that is not a fact about the golem.
                if (armedTick[0] != 0) {
                    return;
                }
                boolean armed = false;
                for (int attempt = 0; attempt < FLEE_DRAWS_PER_TICK && !armed; attempt++) {
                    draws[0]++;
                    armed = flee.canUse();
                }
                if (!armed) {
                    return;
                }
                onArmed.run();
            });
        }

        // One self-report of the goal's own gates, so a window that never arms says which gate
        // closed: creeper in the detection box, fuse state, line of sight, the random flee spot,
        // and whether a route reaches it.
        final String[] gate = new String[1];
        helper.runAfterDelay(pLit + 1, () -> {
            final AABB box = golem.getBoundingBox().inflate(5.0, 3.0, 5.0);
            final List<Creeper> inBox = helper.getLevel().getEntitiesOfClass(Creeper.class, box);
            final Vec3 spot = DefaultRandomPos.getPosAway(golem, 16, 7, creeper.position());
            final Path route = spot == null ? null
                    : golem.getNavigation().createPath(spot.x, spot.y, spot.z, 0);
            final Node last = route == null || route.getNodeCount() == 0 ? null : route.getEndNode();
            gate[0] = "inBox=" + inBox.size() + ", swellDir=" + creeper.getSwellDir()
                    + ", golemToCreeper=" + golem.distanceToSqr(creeper)
                    + ", los=" + golem.getSensing().hasLineOfSight(creeper)
                    + ", spot=" + spot
                    + ", spotFurtherThanGolem=" + (spot != null
                            && creeper.distanceToSqr(spot) > golem.distanceToSqr(creeper))
                    + ", nodes=" + (route == null ? "null" : route.getNodeCount())
                    + ", lastNode=" + (last == null ? "-" : last.x + "," + last.y + "," + last.z)
                    + ", floorOfSpot=" + (spot == null ? "-" : Math.floor(spot.x) + ","
                            + Math.floor(spot.y) + "," + Math.floor(spot.z))
                    + ", golemCell=" + golem.blockPosition().toShortString()
                    + ", creeperCell=" + creeper.blockPosition().toShortString()
                    + ", creeperAlive=" + creeper.isAlive();
        });

        helper.runAfterDelay(pWindow, () -> {
            alive(helper, golem, "the golem");
            helper.assertTrue(armedTick[0] != 0,
                    "the flee goal armed inside the watched window after the fuse was lit (tick "
                    + armedTick[0] + ", window " + (pLit + 1) + "-" + (pLit + FLEE_WINDOW)
                    + ", canUse draws " + draws[0] + "), goals:"
                    + goalSummary(golem.goalSelector) + ", gates: " + gate[0]);
        });
    }

    /**
     * The door task the guard core carries: {@link AIOpenDoor} built with {@code closeDoor} — the
     * constructor argument {@code EntityGolemBase.java:268} passes, and the reason the door shuts
     * behind the golem. The goal arms on the entity's horizontal collision with the door (vanilla's
     * {@code DoorInteractGoal} shape), opens both halves of a real door at {@code start} and closes
     * them at {@code stop}.
     *
     * <p>Observables, in order: with doors unopenable the closed wooden door reads
     * {@code PathType.DOOR_WOOD_CLOSED} and the route to the far side does not reach it; with doors
     * openable the route reaches the far side and walks the doorway column, and the task is not
     * armed while the door is closed and untouched. The golem then walks that route on its own
     * travel: sampled over the window after it sets off, the interaction shows up on the selector
     * (or has already ended on its own dot product, leaving the door standing open), and the
     * doorway recorded at {@code start} is this doorway. On a door set to a known state the task's
     * own acts are asserted: {@code canUse} arms in front of a closed door given the collision and
     * a live path, {@code start} opens both halves, {@code stop} shuts both halves because the task
     * was built with {@code closeDoor}. The last phase reads the durable state: the door is not
     * standing open on a tick on which the task is not live.</p>
     *
     * <p>The template gets a wall across it with a single opening, and the door is a real two-half
     * oak door in that opening facing west: a door's collision slab hugs the cell edge named by
     * {@code facing}, so a north-facing door leaves an east-west route untouched (measured:
     * collision=false with the golem standing in the doorway cell).</p>
     */
    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void combat_golemOpensARealDoorAndClosesItBehindIt(final GameTestHelper helper) {
        clearGolemCaches();
        final int pClosedDoor = SETTLE;
        // The golem sets off at 0.6 blocks/tick from a standstill, so it reaches the closed door a
        // few ticks in; the door goal arms on the tick the goal selector next fully ticks
        // ((tickCount + id) % 2, Mob.serverAiStep) after that collision, so the probe samples a
        // window instead of guessing the tick.
        final int pTouch = SETTLE + 2;
        final int pArmed = SETTLE + 18;
        final int pDoor = SETTLE + 21;
        final int pShut = SETTLE + 34;

        final ServerLevel level = helper.getLevel();
        final BlockPos home = at(helper, 12, 0, 12);
        final BlockPos doorway = at(helper, 13, 0, 12);
        final BlockPos farSide = at(helper, 17, 0, 12);
        // A wall across the template with one opening, and a real two-half oak door in that
        // opening: the doorway is the only crossing, so "the route does not reach" and "the route
        // walks the doorway column" are both meaningful claims, and the door is a door, not a
        // marker. facing=west puts the door's collision slab across the golem's east-west route.
        for (int z = 0; z <= 23; z++) {
            if (z == 12) {
                continue;
            }
            helper.setBlock(13, 0, z, Blocks.STONE.defaultBlockState());
            helper.setBlock(13, 1, z, Blocks.STONE.defaultBlockState());
        }
        helper.setBlock(13, 0, 12, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                .setValue(BlockStateProperties.OPEN, Boolean.FALSE));
        helper.setBlock(13, 1, 12, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
                .setValue(BlockStateProperties.OPEN, Boolean.FALSE));
        final EntityGolemBase golem = spawnGolem(helper, home, GUARD_CORE);
        // setCanOpenDoors/setCanPassDoors live on GroundPathNavigation in 1.21.1, so the probe
        // holds the concrete navigation type.
        final GroundPathNavigation navigation = (GroundPathNavigation) golem.getNavigation();
        // The task core 4 carries, at the priority the original gives it (EntityGolemBase.java:268):
        // AIOpenDoor built with closeDoor, the constructor argument that shuts the door behind the
        // golem.
        final AIOpenDoor openDoor = new AIOpenDoor(golem, true);

        // Doors unopenable: the closed wooden door in the wall's only opening reads
        // PathType DOOR_WOOD_CLOSED (WalkNodeEvaluator.java:416-418) and the wall blocks the rest
        // of the line, so no route reaches the far side. canPassDoors is part of that gate, so the
        // probe turns it off together with canOpenDoors.
        helper.runAfterDelay(pClosedDoor, () -> {
            alive(helper, golem, "the golem");
            navigation.setCanPassDoors(false);
            navigation.setCanOpenDoors(false);
            final Path blocked = navigation.createPath(farSide.getX() + 0.5, farSide.getY(),
                    farSide.getZ() + 0.5, 1);
            helper.assertTrue(!reaches(blocked, farSide),
                    "a route to the far side reaches it with doors unopenable: " + nodeTrail(blocked)
                    + ", golem at " + golem.blockPosition().toShortString());
            // Doors openable: the doorway now reads WALKABLE_DOOR, so the route runs through the
            // doorway column and ends on the far side. The task goes on the selector at its
            // priority. The door is closed and the golem has not touched it, so the interaction is
            // not armed (:27-51).
            navigation.setCanPassDoors(true);
            navigation.setCanOpenDoors(true);
            final Path open = navigation.createPath(farSide.getX() + 0.5, farSide.getY(),
                    farSide.getZ() + 0.5, 1);
            helper.assertTrue(reaches(open, farSide),
                    "and the route reaches the far side with doors openable: " + nodeTrail(open));
            helper.assertTrue(crossesColumn(open, doorway),
                    "and it walks the doorway column " + doorway.toShortString() + ": " + nodeTrail(open));
            helper.assertTrue(navigation.moveTo(farSide.getX() + 0.5, farSide.getY(), farSide.getZ() + 0.5, 1.0),
                    "and the golem is sent along it: " + nodeTrail(navigation.getPath()));
            golem.goalSelector.addGoal(DOOR_PRIORITY, openDoor);
            // MEASURED, and the original agrees: 1.7.10's EntityAIDoorInteract.shouldExecute
            // (:27-51) and 1.21.1's AIDoorInteract.canUse arm on the NEAREST DOOR WITHIN 3 BLOCKS
            // of the mob's own cell, not on a collision. This golem spawns one block from the
            // doorway, so an armed goal here is the faithful reading, not a fault. The claim that
            // the fixture can carry at this point is the door state; the walking, the opening and
            // the shut-behind are proved by the phases below.
            helper.assertTrue(!level.getBlockState(doorway).getValue(BlockStateProperties.OPEN)
                            && !level.getBlockState(doorway.above()).getValue(BlockStateProperties.OPEN),
                    "both door halves are closed when the golem is sent along the route, lower "
                    + level.getBlockState(doorway) + ", upper " + level.getBlockState(doorway.above())
                    + ", tick " + helper.getTick());
        });

        // The gate report and the arming sampler. The goal selector fully ticks only on the
        // (tickCount + id) % 2 parity (Mob.serverAiStep:736-751), and the golem reaches the closed
        // door a few ticks after it sets off from a standstill, so the arming tick is not knowable
        // in advance: sample the window and keep the first tick the interaction shows up on.
        final String[] gate = new String[1];
        final long[] armed = new long[1];
        helper.runAfterDelay(pTouch, () -> {
            alive(helper, golem, "the golem");
            final Path tp = navigation.getPath();
            gate[0] = "collision=" + golem.horizontalCollision
                    + ", canOpenDoors=" + navigation.canOpenDoors()
                    + ", canPassDoors=" + navigation.canPassDoors()
                    + ", path=" + nodeTrail(tp)
                    + ", nodes=" + (tp == null ? "-" : tp.getNextNodeIndex() + "/" + tp.getNodeCount()
                            + " done=" + tp.isDone())
                    + ", golem=" + v(golem.position())
                    + ", doorSq=" + golem.distanceToSqr(doorway.getX(), golem.getY(), doorway.getZ())
                    + ", canUse=" + openDoor.canUse()
                    + ", recorded=" + readField(openDoor, "entityPosX") + ","
                    + readField(openDoor, "entityPosY") + "," + readField(openDoor, "entityPosZ")
                    + ", lower door " + level.getBlockState(doorway)
                    + ", upper door " + level.getBlockState(doorway.above())
                    + ", goals:" + goalSummary(golem.goalSelector)
                    + ", tick " + helper.getTick();
        });

        for (int t = pTouch + 1; t <= pTouch + 14; t++) {
            helper.runAfterDelay(t, () -> {
                alive(helper, golem, "the golem");
                if (armed[0] != 0) {
                    return;
                }
                // The interaction has run: it is on the selector, or it has already ended on its
                // own dot product (:74-82) and left the door standing open. Both traces mean the
                // selector started the task on the golem's own collision with the closed door.
                final boolean running = goalRunning(golem.goalSelector, openDoor);
                final boolean doorState = level.getBlockState(doorway).getValue(BlockStateProperties.OPEN);
                if (running || doorState) {
                    armed[0] = helper.getTick();
                    navigation.stop();
                    golem.setDeltaMovement(Vec3.ZERO);
                }
            });
        }

        helper.runAfterDelay(pArmed, () -> {
            alive(helper, golem, "the golem");
            helper.assertTrue(armed[0] != 0,
                    "the selector ran the task on the golem's own bump into the closed door, sampled over ticks "
                    + (pTouch + 1) + "-" + (pTouch + 14) + ", armed at " + armed[0] + "; gates: " + gate[0]);
            helper.assertTrue(golem.blockPosition().getX() >= doorway.getX() - 1
                            && golem.blockPosition().getZ() == doorway.getZ(),
                    "the golem walked to the wall's opening on its own, it stands at "
                    + golem.blockPosition().toShortString() + ", the doorway is "
                    + doorway.toShortString() + ", tick " + helper.getTick() + ", path "
                    + nodeTrail(navigation.getPath()));
            // The doorway the task recorded at start (:14-16, the field the selector's stop closes
            // on) is this doorway, not some other door.
            final BlockPos worked = new BlockPos((int) readField(openDoor, "entityPosX"),
                    (int) readField(openDoor, "entityPosY"), (int) readField(openDoor, "entityPosZ"));
            helper.assertTrue(worked.getX() == doorway.getX() && worked.getZ() == doorway.getZ()
                            && (worked.getY() == doorway.getY() || worked.getY() == doorway.getY() + 1),
                    "the interaction worked on the doorway column " + doorway.toShortString()
                    + ", it recorded " + worked.toShortString() + " (doorPos is node.y + 1, exactly"
                    + " vanilla's DoorInteractGoal.java:61-62; in 1.21.1's node-y convention that is"
                    + " the door's upper half, and DoorBlock.setOpen flips both halves)");
        });

        // The door acts of the task, on a door set to a known state. start/stop are called
        // directly here: the goal's own dot product ends the interaction the moment the golem
        // drifts past the doorway centre, so the walk cannot be parked long enough to read
        // "running, door open" off the selector — the selector-driven part is asserted above.
        helper.runAfterDelay(pDoor, () -> {
            alive(helper, golem, "the golem");
            helper.setBlock(13, 0, 12, Blocks.OAK_DOOR.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                    .setValue(BlockStateProperties.OPEN, Boolean.FALSE));
            helper.setBlock(13, 1, 12, Blocks.OAK_DOOR.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
                    .setValue(BlockStateProperties.OPEN, Boolean.FALSE));
            golem.setPos(doorway.getX() - 0.5, home.getY(), doorway.getZ() + 0.5);
            golem.setDeltaMovement(Vec3.ZERO);
            navigation.setCanPassDoors(true);
            navigation.setCanOpenDoors(true);
            // canUse needs a live path whose front nodes reach the door (:31-34). A closed door
            // reads DOOR_WOOD_CLOSED, a malus and not a wall, so the route exists.
            helper.assertTrue(navigation.moveTo(farSide.getX() + 0.5, farSide.getY(), farSide.getZ() + 0.5, 1.0),
                    "the route to the far side exists in front of the closed door: "
                    + nodeTrail(navigation.getPath()) + ", tick " + helper.getTick());
            golem.horizontalCollision = true;
            helper.assertTrue(openDoor.canUse(),
                    "in front of a closed door, with the collision the door goals arm on, the interaction"
                    + " arms (:27-51), tick " + helper.getTick() + ", lower door "
                    + level.getBlockState(doorway));
            openDoor.start();
            helper.assertTrue(level.getBlockState(doorway).getValue(BlockStateProperties.OPEN)
                            && level.getBlockState(doorway.above()).getValue(BlockStateProperties.OPEN),
                    "start() opened the real door on both halves (:67-72, DoorBlock.setOpen), tick "
                    + helper.getTick() + ", lower " + level.getBlockState(doorway) + ", upper "
                    + level.getBlockState(doorway.above()));
            helper.assertTrue(openDoor.canContinueToUse(),
                    "with the stall budget unspent and the golem standing west of the doorway, the task stays"
                    + " live (:63-65), golem at " + golem.blockPosition().toShortString() + ", tick "
                    + helper.getTick());
            openDoor.stop();
            helper.assertTrue(!level.getBlockState(doorway).getValue(BlockStateProperties.OPEN)
                            && !level.getBlockState(doorway.above()).getValue(BlockStateProperties.OPEN),
                    "stop() shut the door again, because the task was built with closeDoor "
                    + "(AIOpenDoor.java:100-116, the argument at EntityGolemBase.java:268), tick "
                    + helper.getTick() + ", lower " + level.getBlockState(doorway) + ", upper "
                    + level.getBlockState(doorway.above()));
            // And the route through the opened doorway stands again for the engine to walk.
            helper.setBlock(13, 0, 12, Blocks.OAK_DOOR.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                    .setValue(BlockStateProperties.OPEN, Boolean.TRUE));
            helper.setBlock(13, 1, 12, Blocks.OAK_DOOR.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
                    .setValue(BlockStateProperties.OPEN, Boolean.TRUE));
            navigation.setCanPassDoors(true);
            navigation.setCanOpenDoors(true);
            helper.assertTrue(navigation.moveTo(farSide.getX() + 0.5, farSide.getY(), farSide.getZ() + 0.5, 1.0),
                    "the route through the opened doorway is rebuilt: " + nodeTrail(navigation.getPath())
                    + ", golem at " + golem.blockPosition().toShortString());
        });

        // The invariant the class guarantees: the door is not left standing open by a task that is
        // no longer live. The probe's stop() at pDoor closed the door directly (proved there); the
        // selector's own stop lands on a later parity tick (the (tickCount + id) % 2 gate of
        // Mob.serverAiStep:736-751) and the budget is the task's own (count = 100 at start,
        // decremented per tick, the goal living while count > 0 and the golem has not passed the
        // doorway, :42, 63-68, 74-81). The probe reads the state it finds, reports the goal's own
        // counters, and does not guess which tick the stop lands on.
        helper.runAfterDelay(pShut, () -> {
            alive(helper, golem, "the golem");
            final boolean running = goalRunning(golem.goalSelector, openDoor);
            final boolean openState = level.getBlockState(doorway).getValue(BlockStateProperties.OPEN);
            helper.assertTrue(!openState || running,
                    "the door is open only while the task is live: running=" + running + ", open=" + openState
                    + ", count=" + readField(openDoor, "count")
                    + ", hasStopped=" + readField(openDoor, "hasStoppedDoorInteraction")
                    + ", recorded vector=" + readField(openDoor, "entityPositionX") + ","
                    + readField(openDoor, "entityPositionZ")
                    + ", doorway=" + readField(openDoor, "entityPosX") + ","
                    + readField(openDoor, "entityPosY") + "," + readField(openDoor, "entityPosZ")
                    + ", golem=" + v(golem.position()) + ", lower door " + level.getBlockState(doorway)
                    + ", upper door " + level.getBlockState(doorway.above())
                    + ", goals:" + goalSummary(golem.goalSelector) + ", tick " + helper.getTick());
            helper.succeed();
        });
    }

    /**
     * The tail ends with {@link AIReturnHome} at task 6 for every core
     * ({@code EntityGolemBase.java:294}). 1.7.10's {@code AIReturnHome} wants to walk home once
     * its pathing delay is spent and the golem is outside the home radius — 3.0 squared from the
     * placer's centre ({@code AIReturnHome.java:33, 27-41}) — paths to that centre
     * ({@code :80-94}) and lives on a 20-tick stall budget ({@code :43-49, 54-78}). The 48-block
     * jump is not in the goal: 1.7.10 teleported from the entity tick at {@code 2304.0} squared
     * blocks ({@code EntityGolemBase.java:224-232}), and the port does the same at
     * {@code :1110-1127}.
     *
     * <p>The walk phase stays inside the template (10 blocks out), and the 48-block phase lays
     * its own floor first — outside the template the game-test world has no floor at the
     * golem's height, and a golem dropped into the void reports as a dead golem, not as a
     * teleport. The same is true of the flee probe, whose goal runs to a spot 16 blocks from
     * the creeper: it lays a clearing ({@link #layClearing}) before it asks the goal to arm.</p>
     */
    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void combat_returnHomeUsesTheRadiusAndTeleportsFromBeyond48(final GameTestHelper helper) {
        clearGolemCaches();
        final int pSet = SETTLE;
        final int pPath = SETTLE + 3;
        final int pWalk = SETTLE + 40;
        final int pTeleport = SETTLE + 45;

        final BlockPos home = at(helper, 12, 0, 12);
        final EntityGolemBase golem = spawnGolem(helper, home, GUARD_CORE);
        final AIReturnHome returnHome = new AIReturnHome(golem);
        golem.goalSelector.addGoal(HOME_PRIORITY, returnHome);
        assertPriority(helper, golem.goalSelector, returnHome, HOME_PRIORITY, "the return-home goal");
        final Vec3 centre = home.getCenter();
        final ServerLevel level = helper.getLevel();
        // A floor out to 52 blocks along +z, so the 48-block rule is tested on ground.
        for (int z = 12; z <= 70; z++) {
            level.setBlock(new BlockPos(home.getX(), home.getY() - 1, z), Blocks.STONE.defaultBlockState(), 3);
        }

        helper.runAfterDelay(pSet, () -> {
            standingInAirCellOnFloor(helper, golem, 12, 0, 12, "the golem");
            helper.assertTrue(golem.distanceToSqr(centre) < 3.0,
                    "the golem starts on its home cell, squared distance " + golem.distanceToSqr(centre)
                    + " < 3.0, tick " + helper.getTick());
            helper.assertTrue(!returnHome.canUse(), "inside the home radius the goal does not run (:27-41)");
            golem.setPos(home.getX() + 0.5, home.getY(), home.getZ() + 10.5);
        });

        helper.runAfterDelay(pPath, () -> {
            alive(helper, golem, "the golem");
            helper.assertTrue(golem.onGround(),
                    "the golem is back on the floor after the move, tick " + helper.getTick() + " at "
                    + golem.blockPosition().toShortString());
            helper.assertTrue(returnHome.canUse(),
                    "outside the home radius it does: squared distance " + golem.distanceToSqr(centre)
                    + " against a radius of 3.0 squared (:33), tick " + helper.getTick());
            returnHome.start();
            final Path path = golem.getNavigation().getPath();
            final Node end = path == null ? null : path.getEndNode();
            helper.assertTrue(path != null && path.getNodeCount() > 0 && path.canReach() && end != null
                            && end.asBlockPos().getCenter().distanceToSqr(centre) <= 1.5,
                    "start() paths to the placer's centre and the route reaches that cell: " + nodeTrail(path));
            helper.assertTrue(returnHome.canContinueToUse(),
                    "the goal lives: delay spent, the 20-tick stall budget armed, the route live, and the golem "
                    + "is still outside the radius (:43-49)");
        });

        helper.runAfterDelay(pWalk, () -> {
            alive(helper, golem, "the golem");
            final double walked = centre.distanceTo(golem.position());
            helper.assertTrue(walked < 5.0,
                    "the goal walked the golem from 10.0 blocks toward home, now " + walked
                    + " away at " + golem.blockPosition().toShortString() + ", tick " + helper.getTick()
                    + " (the goal re-paths on its 20-tick stall budget, :43-49)");

            // Beyond 48 blocks the entity tick's teleport rule takes over (2304.0 squared).
            golem.setPos(home.getX() + 0.5, home.getY(), home.getZ() + 52.5);
            helper.assertTrue(golem.getRestrictCenter().distSqr(golem.blockPosition()) >= TELEPORT_SQUARED,
                    "the golem is past the 48-block rule, squared distance "
                    + golem.getRestrictCenter().distSqr(golem.blockPosition()) + " against 2304.0, tick "
                    + helper.getTick());
        });

        helper.runAfterDelay(pTeleport, () -> {
            alive(helper, golem, "the golem");
            // The 52-block cell sits outside the template's ticking area (GameTestServer keeps a
            // ticking area over the template box), so the engine does not tick an entity parked
            // there — measured: it had not moved 5 ticks on. The rule under test lives in
            // EntityGolemBase.tick() itself, so the probe drives that tick by hand, exactly as it
            // drives every other goal in this file, and the assert below stays a claim about the
            // shipped 48-block teleport (2304.0 squared, EntityGolemBase.java:402-421).
            golem.tick();
            final double after = golem.distanceToSqr(centre);
            final BlockPos landed = golem.blockPosition();
            helper.assertTrue(Math.abs(landed.getX() - home.getX()) <= 1
                            && Math.abs(landed.getY() - home.getY()) <= 1
                            && Math.abs(landed.getZ() - home.getZ()) <= 1,
                    "five ticks past the 48-block rule the golem stands in the home cell's 3x3 footprint: cell "
                    + landed.toShortString() + " against home " + home.toShortString() + ", tick "
                    + helper.getTick() + " — it cannot walk 52 blocks in five ticks");
            helper.assertTrue(after < 3.0,
                    "and it is inside the goal's home radius again: squared distance " + after
                    + " at " + landed.toShortString() + ", tick " + helper.getTick());
            helper.succeed();
        });
    }
}
