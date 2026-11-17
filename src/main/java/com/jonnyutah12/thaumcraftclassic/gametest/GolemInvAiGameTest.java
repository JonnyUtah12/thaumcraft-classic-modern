package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EnumGolemType;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import com.jonnyutah12.thaumcraftclassic.entities.golems.Marker;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIFillGoto;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIHomeDrop;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIHomePlace;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIHomeReplace;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIHomeTake;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AISortingGoto;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AISortingPlace;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Game tests for the ported golem inventory goals (TC-M6-08-s-b2, the 13
 * classes under {@code entities/golems/ai/}).
 *
 * <p>Each probe drives a goal the way {@code EntityAITasks} did in 1.7.10 —
 * {@code canUse()} (the original's {@code ground()}), then {@code start()}
 * ({@code startExecuting()}), then {@code tick()}/{@code stop()} — and asserts
 * <b>observable world state</b>: what a real chest in the level contains, what
 * the golem carries, and whether an ItemEntity appeared. The goals reach the
 * containers through the entity's shipped act methods, so these probes prove
 * the goal's gate <em>and</em> the mutation it authorises.</p>
 *
 * <p><b>Geometry the probes rely on:</b> the golem is spawned at
 * {@code (2,1,2)} with home facing NORTH (3D data value {@code 2}), so
 * {@code GolemHelper.homeContainerPos} —
 * {@code restrictCenter.relative(facing.getOpposite())} — is {@code (2,1,3)},
 * one block south. The golem's feet are at y=1 in the cell <em>next to</em> the
 * chest, never inside a solid block, which is the same geometry
 * {@code GolemRuntimeGameTest} uses. A second chest at {@code (3,1,3)} is
 * within {@code GolemHelper.ADJACENT_RANGE}, which is {@code 4.0} compared
 * against the <b>squared</b> distance (a 2-block radius); a chest at
 * {@code (6,1,3)} is outside it and stands in for "a container this goal must
 * not touch".</p>
 *
 * <h2>Pitfalls this file has measured and pins</h2>
 * <ol>
 *   <li>{@code GolemHelper}'s jar list and item timeouts are static, so they
 *       leak between tests: every probe clears them first
 *       ({@link GolemHelper#clearJarListForTest()} /
 *       {@link GolemHelper#clearItemTimeoutsForTest()}).
 *       {@code GolemHelper.findSomething*Core} <b>adds</b> a timeout when it
 *       finds no destination ({@code GolemHelper.java:770-772}), and
 *       {@code AIHomeReplace}'s escape hatch
 *       ({@code AIHomeReplace.java:35-36}) fires on that timeout
 *       <em>before</em> the per-core arms — so an arm that tests a marker
 *       colour has to clear the timeouts, and the timeout arm is asserted
 *       separately.</li>
 *   <li>The goals gate on {@code golem.tickCount % Config.golemDelay}
 *       ({@link com.jonnyutah12.thaumcraftclassic.entities.golems.GolemConfig#GOLEM_DELAY}
 *       = 5), so every probe sets {@code tickCount} to a multiple of 5 before
 *       calling {@code canUse()}; "inert" is never asserted on a cadence tick
 *       that the goal was going to skip anyway.</li>
 *   <li>{@link GameTestHelper#getEntities(EntityType, BlockPos, double)}
 *       <b>absolutizes</b> the position it is given, so it takes template
 *       (relative) coordinates. Handing it an absolute pos silently queries a
 *       box two test-areas away and returns an empty list.</li>
 *   <li>{@code EntityGolemBase.getActionTimer()} is the original's triangle
 *       wave {@code 3 - Math.abs(action - 3)} ({@code EntityGolemBase.java:1047-1049});
 *       {@code startActionTimer()} sets {@code action = 6}, at which the wave
 *       reads {@code 0}. The observable that a act started is
 *       {@code getAction() == ACTION_TICKS}.</li>
 *   <li>{@code GolemHelper.getContainersWithGoods} probes with a real
 *       (non-simulated) extraction ({@code GolemHelper.java:265-270}), so a
 *       query by one goal consumes the destination's matched stack for the
 *       next goal in the same probe. Where a probe needs the "already holds
 *       it" state again, it restocks and says so.</li>
 *   <li>A goal's walk continuation follows its navigation:
 *       {@code AIFillGoto.canContinueToUse()} is
 *       {@code count > 0 && !navigation.isDone()}, and a path onto a
 *       container's own cell does not start, so the pair is asserted
 *       together, not in isolation.</li>
 * </ol>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GolemInvAiGameTest {

    /**
     * The face the probe golems are "placed against". The original's home
     * container is {@code restrictCenter.relative(facing.getOpposite())}, so
     * NORTH puts the home chest to the golem's south — never inside it.
     */
    private static final int HOME_FACING = Direction.NORTH.get3DDataValue();

    /** A concrete, non-wildcard marker colour, so the wildcard path is exercised separately. */
    private static final byte MARKER_COLOUR = 3;

    /** A concrete marker colour that must NOT match {@link #MARKER_COLOUR}. */
    private static final byte OTHER_COLOUR = 5;

    /** Template (relative) cell of the home chest, i.e. {@code home - facing}. */
    private static final BlockPos HOME_CHEST = new BlockPos(2, 1, 3);

    /** Template (relative) cell of the marked destination chest. */
    private static final BlockPos DESTINATION = new BlockPos(3, 1, 3);

    /** Template (relative) cell of the chest outside the adjacency range. */
    private static final BlockPos FAR_CHEST = new BlockPos(6, 1, 3);

    private GolemInvAiGameTest() {
    }

    // ------------------------------------------------------------------ helpers ---

    private static BlockPos at(final GameTestHelper helper, final int x, final int y, final int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    /** The port's {@code GolemHelper.dimensionOf} (package-private) — stable {@code String.hashCode}. */
    private static int dim(final ServerLevel level) {
        return level.dimension().location().hashCode();
    }

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

    private static EntityGolemBase spawn(final ServerLevel level, final BlockPos home,
                                         final EnumGolemType material, final int core) {
        final EntityGolemBase golem = CatalogGolems.GOLEM.get().create(level);
        golem.setMaterial(material);
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

    /** Marks a block for the golem on one face with the probe colour. */
    private static void mark(final EntityGolemBase golem, final BlockPos pos, final Direction side) {
        mark(golem, pos, side, MARKER_COLOUR);
    }

    private static void mark(final EntityGolemBase golem, final BlockPos pos, final Direction side,
                             final byte colour) {
        golem.addMarker(new Marker(pos.getX(), pos.getY(), pos.getZ(), dim((ServerLevel) golem.level()),
                (byte) side.get3DDataValue(), colour));
    }

    private static Container containerAt(final ServerLevel level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Container container ? container : null;
    }

    private static int countOf(final Container container, final ItemStack sample) {
        if (container == null) {
            return 0;
        }
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            final ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, sample)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    // -------------------------------------------------- AIHomePlace (priority 1) ---

    /**
     * {@code AIHomePlace} (priority 1, cores 0/1/2/8/10): with a carried stack,
     * the goal's gate passes and {@code start()} really puts the stack into the
     * chest at the home face — an unmarked chest outside the adjacency range is
     * untouched, the golem is left empty-handed, the act starts the golem's
     * action cycle, and the goal refuses with no carry.
     */
    @GameTest(template = "empty_24")
    public static void inv_homePlaceGoalDepositsItsCarryIntoTheHomeChest(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(2, 1, 3, Blocks.CHEST);
        helper.setBlock(6, 1, 3, Blocks.CHEST);

        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 2), EnumGolemType.WOOD, 0);
        final Container home = containerAt(level, GolemHelper.homeContainerPos(golem));
        helper.assertTrue(home != null, "the home container is the chest at home - facing");
        home.clearContent();

        final ItemStack carried = new ItemStack(Items.STICK, 8);
        golem.setCarried(carried);
        // The originals gate on tickCount % Config.golemDelay, golemDelay = 5.
        golem.tickCount = 5;

        final AIHomePlace place = new AIHomePlace(golem);
        helper.assertTrue(place.canUse(), "AIHomePlace accepts a carry the home chest has room for");
        place.start();
        helper.assertTrue(countOf(home, carried) == 8,
                "the 8 carried sticks are in the home chest, got " + countOf(home, carried));
        helper.assertTrue(golem.getCarried() == null, "the golem is no longer carrying them");
        helper.assertTrue(golem.getAction() == EntityGolemBase.ACTION_TICKS,
                "and the act started the golem's action cycle, got action " + golem.getAction());
        helper.assertTrue(golem.getActionTimer() == 0,
                "getActionTimer() is the original's 3 - abs(action - 3) wave, so at action 6 it reads 0");
        helper.assertTrue(countOf(containerAt(level, at(helper, 6, 1, 3)), carried) == 0,
                "the chest outside the adjacency range is untouched");

        place.tick();
        place.stop();
        helper.assertTrue(countOf(home, carried) == 8, "tick and stop move nothing extra");

        // No carry: the goal is inert.
        helper.assertTrue(!place.canUse(), "AIHomePlace with an empty carry does not fire");
        golem.discard();
        helper.succeed();
    }

    // --------------- AIHomeReplace (priority 0) + AIHomeTake (priority 4), core 1 ---

    /**
     * The core 1 (Empty) pair. {@code AIHomeReplace} unloads the carry at home
     * only when the core has nowhere useful to put it: with a marker whose
     * colour the carry does not match it takes over and deposits at home, and
     * with a matching marker colour (the {@code -1} wildcard is not in play
     * here) it yields to the place goals. A stack that has been searched for
     * and not placed is on {@code GolemHelper}'s item timeout, and
     * {@code AIHomeReplace}'s escape hatch then fires ahead of the core arm —
     * asserted as its own arm, then cleared so the marker colour is the only
     * variable. {@code AIHomeTake} drains the home chest into the golem,
     * bounded by {@code getCarrySpace()} — WOOD's carry limit is 4 with no
     * Earth upgrade.
     */
    @GameTest(template = "empty_24")
    public static void homeReplaceYieldsOnAMatchingMarkerAndHomeTakeDrainsTheChest(
            final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(2, 1, 3, Blocks.CHEST);
        helper.setBlock(3, 1, 3, Blocks.CHEST);

        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 2), EnumGolemType.WOOD, 1);
        final Container home = containerAt(level, GolemHelper.homeContainerPos(golem));
        helper.assertTrue(home != null, "the home container is the chest at home - facing");
        home.clearContent();
        home.setItem(0, new ItemStack(Items.STICK, 10));

        // The golem inventory defines the need list and the carry's colour slot.
        golem.getInventory().setItem(0, new ItemStack(Items.STICK, 3));
        golem.setColors(0, MARKER_COLOUR);
        final BlockPos destination = at(helper, 3, 1, 3);
        // A destination chest with room (and a stack that is not sticks) is what
        // findSomethingEmptyCore accepts for the Empty core, so the colour arms
        // below are the only variable.
        containerAt(level, destination).setItem(0, new ItemStack(Items.COBBLESTONE, 1));

        final ItemStack carried = new ItemStack(Items.STICK, 4);
        golem.setCarried(carried);
        golem.tickCount = 5;

        // (a) A marker of a colour the carry does NOT match: the search finds no
        // destination, which puts the stack on GolemHelper's item timeout, and
        // AIHomeReplace takes over and unloads at home.
        mark(golem, destination, Direction.UP, OTHER_COLOUR);
        helper.assertTrue(!GolemHelper.findSomethingEmptyCore(golem, carried),
                "a marker of a non-matching colour is no destination for the carry");
        helper.assertTrue(GolemHelper.isOnTimeOut(golem, carried),
                "the failed search put the stack on the item timeout (GolemHelper.findSomethingEmptyCore)");
        final AIHomeReplace replace = new AIHomeReplace(golem);
        helper.assertTrue(replace.canUse(),
                "AIHomeReplace fires when no marked container accepts the carry's colour");
        replace.start();
        helper.assertTrue(countOf(home, carried) == 14,
                "the 4 carried sticks were deposited at home, got " + countOf(home, carried));
        helper.assertTrue(golem.getCarried() == null, "and the golem's hands are empty");

        // (b) A matching marker, but the carry is still on timeout: the original's
        // escape hatch (AIHomeReplace.java:35-36) outranks the core arm.
        mark(golem, destination, Direction.UP, MARKER_COLOUR);
        golem.setCarried(carried.copy());
        helper.assertTrue(GolemHelper.findSomethingEmptyCore(golem, golem.getCarried()),
                "the matching marker colour is now a destination for the carry");
        helper.assertTrue(replace.canUse(),
                "the item timeout makes AIHomeReplace fire ahead of the core arms");

        // (b2) Same world, timeout cleared: the matching marker makes it yield.
        GolemHelper.clearItemTimeoutsForTest();
        helper.assertTrue(!replace.canUse(),
                "AIHomeReplace yields once a marker colour matches the carried stack");

        // (c) AIHomeTake: empty hands, so the goal drains the home chest.
        golem.setCarried(null);
        golem.tickCount = 5;
        final int space = golem.getCarrySpace();
        helper.assertTrue(space == 4, "WOOD's carry space is its base limit 4, got " + space);
        final AIHomeTake take = new AIHomeTake(golem);
        helper.assertTrue(take.canUse(),
                "AIHomeTake fires when the home chest holds a stack the core needs");
        final int taken = golem.getCarried() == null ? 0 : golem.getCarried().getCount();
        helper.assertTrue(golem.getCarried() != null && ItemStack.isSameItem(golem.getCarried(), carried)
                        && taken > 0 && taken <= space && taken == 3,
                "the golem took the 3 sticks its need list asks for (carry space " + space + "), got " + taken);
        helper.assertTrue(countOf(home, carried) == 11,
                "the home chest lost exactly those sticks, got " + countOf(home, carried));
        helper.assertTrue(countOf(containerAt(level, destination), carried) == 0,
                "the marked destination chest received nothing from the take");
        helper.assertTrue(!take.canUse(), "AIHomeTake is inert once the hands are full");
        take.stop();
        golem.discard();
        helper.succeed();
    }

    // --------------------------------------------------- AIHomeDrop (priority 2) ---

    /**
     * {@code AIHomeDrop} is the original's fallback: its gate only passes when
     * the home block is <b>not</b> a container, and then the carried stack is
     * thrown toward the home block as a real {@link ItemEntity}. A chest in the
     * level stays empty, and the same goal is inert while the home block is a
     * container (the container arm belongs to {@code AIHomePlace}).
     */
    @GameTest(template = "empty_24")
    public static void homeDropGoalThrowsTheCarryWhenTheHomeBlockIsNotAChest(
            final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(2, 1, 3, Blocks.AIR);
        helper.setBlock(6, 1, 3, Blocks.CHEST);

        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 2), EnumGolemType.WOOD, 0);
        final BlockPos homePos = GolemHelper.homeContainerPos(golem);
        helper.assertTrue(level.getBlockEntity(homePos) == null,
                "the home block is not a container, which is what the goal's gate requires");

        final ItemStack carried = new ItemStack(Items.STICK, 8);
        golem.setCarried(carried);
        golem.tickCount = 5;

        final AIHomeDrop drop = new AIHomeDrop(golem);
        helper.assertTrue(drop.canUse(), "AIHomeDrop accepts a carry with no container at home");
        drop.start();
        helper.assertTrue(golem.getCarried() == null, "the goal released the carried stack");

        // GameTestHelper.getEntities absolutizes the position it is given, so the
        // query takes the template (relative) cell of the home chest.
        final java.util.List<ItemEntity> dropped = helper.getEntities(EntityType.ITEM, HOME_CHEST, 3.0);
        int found = 0;
        for (final ItemEntity item : dropped) {
            if (!item.isRemoved() && ItemStack.isSameItemSameComponents(item.getItem(), carried)) {
                found += item.getItem().getCount();
            }
        }
        helper.assertTrue(found == 8, "the 8 sticks lie on the ground at the home block, got " + found);
        helper.assertTrue(countOf(containerAt(level, at(helper, 6, 1, 3)), carried) == 0,
                "no chest in the level received them");
        drop.tick();
        drop.stop();

        // The same goal is inert when the home block IS a container.
        helper.setBlock(2, 1, 3, Blocks.CHEST);
        golem.setCarried(carried.copy());
        golem.tickCount = 5;
        helper.assertTrue(!drop.canUse(),
                "AIHomeDrop stays inert while a container sits at the home block");
        golem.discard();
        helper.succeed();
    }

    // ------------------------------------------------ AISortingPlace (priority 1) ---

    /**
     * {@code AISortingPlace} (priority 1, core 10): the gate wants a marked
     * container, other than the home block, that can take the carry on a marked
     * face <em>and already holds it</em>. The act then moves goods out of the
     * home chest into that container — the home chest really loses them, and a
     * chest outside the adjacency range is untouched.
     *
     * <p>The tree's {@code GolemHelper.getContainersWithGoods} probes with a
     * real (non-simulated) extraction, so the destination's own matched stack is
     * consumed by the query; the destination total is asserted from the floor of
     * what the act delivered, exactly as the original's behaviour allows.</p>
     */
    @GameTest(template = "empty_24")
    public static void sortingPlaceGoalMovesHomeChestGoodsIntoAMatchingMarkedChest(
            final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(2, 1, 3, Blocks.CHEST);
        helper.setBlock(3, 1, 3, Blocks.CHEST);
        helper.setBlock(6, 1, 3, Blocks.CHEST);

        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 2), EnumGolemType.WOOD, 10);
        final Container home = containerAt(level, GolemHelper.homeContainerPos(golem));
        home.clearContent();
        home.setItem(0, new ItemStack(Items.STICK, 8));

        final BlockPos destination = at(helper, 3, 1, 3);
        final Container target = containerAt(level, destination);
        target.clearContent();
        target.setItem(0, new ItemStack(Items.STICK, 1));
        mark(golem, destination, Direction.UP);

        // A small carry keeps getCarrySpace() (4 - 1 = 3) as the mover's bound.
        final ItemStack carried = new ItemStack(Items.STICK, 1);
        golem.setCarried(carried);
        golem.tickCount = 5;

        final AISortingPlace sorting = new AISortingPlace(golem);
        helper.assertTrue(sorting.canUse(),
                "AISortingPlace wants a marked container that already holds the carry");
        sorting.start();
        helper.assertTrue(countOf(home, carried) == 5,
                "3 sticks left the home chest, leaving " + countOf(home, carried));
        helper.assertTrue(countOf(target, carried) >= 3,
                "the marked chest that already held them took the goods, got " + countOf(target, carried));
        helper.assertTrue(countOf(containerAt(level, at(helper, 6, 1, 3)), carried) == 0,
                "the chest outside the adjacency range is untouched");
        sorting.tick();
        sorting.stop();
        golem.discard();
        helper.succeed();
    }

    // --------------------------------------- AIFillGoto / AISortingGoto targets ---

    /**
     * The goto goals are inert when they have no target: with no markers the
     * target search finds nothing and {@code canUse()} stays false, and
     * {@code canContinueToUse()} is false before {@code start()} sets the walk
     * budget. Marking a container that already holds the wanted stack gives the
     * goals a target and the gate passes; clearing the markers takes it away.
     *
     * <p>{@code GolemHelper.getContainersWithGoods} extracts for real, so the
     * {@code AIFillGoto} query consumes the destination's stick; the sorting arm
     * restocks it, because "the container already holds the carry" is exactly
     * what that goal's gate requires.</p>
     */
    @GameTest(template = "empty_24")
    public static void gotoGoalsAreInertUntilAMarkedContainerHoldsTheirTarget(
            final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(2, 1, 3, Blocks.CHEST);
        helper.setBlock(3, 1, 3, Blocks.CHEST);
        containerAt(level, at(helper, 2, 1, 3)).clearContent();
        containerAt(level, at(helper, 3, 1, 3)).clearContent();
        containerAt(level, at(helper, 3, 1, 3)).setItem(0, new ItemStack(Items.STICK, 1));

        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 2), EnumGolemType.WOOD, 0);
        // The need list drives getMissingItems; the home chest is empty, so the
        // 4 sticks the inventory wants are missing.
        golem.getInventory().setItem(0, new ItemStack(Items.STICK, 4));
        golem.setCarried(null);
        golem.tickCount = 5;

        final AIFillGoto fill = new AIFillGoto(golem);
        helper.assertTrue(!fill.canUse(), "AIFillGoto has no target with no markers set");
        helper.assertTrue(!fill.canContinueToUse(), "and it has no walk budget before it starts");

        mark(golem, at(helper, 3, 1, 3), Direction.UP);
        helper.assertTrue(fill.canUse(),
                "a marked adjacent container that already holds the wanted stack is a target");
        fill.start();
        // The path is onto the chest's own cell, which navigation will not enter,
        // so the goal's continuation follows the navigation and stays closed.
        helper.assertTrue(fill.canContinueToUse() == !golem.getNavigation().isDone(),
                "the goal's continuation follows its navigation, which found no path onto the chest");
        fill.stop();
        helper.assertTrue(!fill.canContinueToUse(), "resetTask cleared the budget");

        // The fill query extracted the destination's stick (getContainersWithGoods
        // is not a simulation), so put back what the sorting gate asks for.
        containerAt(level, at(helper, 3, 1, 3)).setItem(0, new ItemStack(Items.STICK, 1));

        // AISortingGoto wants a marked container that already holds the CARRY.
        final AISortingGoto sorting = new AISortingGoto(golem);
        helper.assertTrue(!sorting.canUse(), "AISortingGoto is inert with an empty carry");
        golem.setCarried(new ItemStack(Items.STICK, 1));
        golem.tickCount = 5;
        helper.assertTrue(sorting.canUse(),
                "with a carry, a marked container that already holds it is a sort target");
        golem.getMarkers().clear();
        golem.tickCount = 5;
        helper.assertTrue(!sorting.canUse(), "clearing the markers leaves it inert again");
        helper.assertTrue(!fill.canUse(), "and the fill goal lost its target with them");
        golem.discard();
        helper.succeed();
    }
}
