package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EnumGolemType;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemCoreTask;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import com.jonnyutah12.thaumcraftclassic.entities.golems.Marker;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TC-M6-08-s-b probes: the golem runtime really moves items and essentia.
 *
 * <p>Every probe asserts <b>observable world state</b> — the contents of a real
 * chest, the essentia in a real {@link TileJarFillable}, the fields that survive
 * a real NBT save/load — never "a method was called". The act half of each
 * core's task is driven directly through
 * {@link EntityGolemBase#runCoreTaskStep()} with the golem placed next to its
 * target, so no probe depends on pathfinding.</p>
 *
 * <p><b>Pitfall this class guards against:</b> {@code GolemHelper}'s jar list and
 * item-timeout list are static, so they leak between game tests. Every probe
 * clears them first ({@link GolemHelper#clearJarListForTest()} /
 * {@link GolemHelper#clearItemTimeoutsForTest()}).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GolemRuntimeGameTest {

    /**
     * The face the probe golems were "placed against". The original's home
     * container is {@code restrictCenter.relative(facing.getOpposite())}
     * ({@code GolemHelper.getMissingItems}, {@code GolemHelper.java:192-196}), so
     * NORTH puts the container to the golem's south — never inside it.
     */
    private static final int HOME_FACING = Direction.NORTH.get3DDataValue();

    /** A concrete, non-wildcard marker colour, so the wildcard path is exercised separately. */
    private static final byte MARKER_COLOUR = 3;

    private GolemRuntimeGameTest() {
    }

    private static BlockPos at(final GameTestHelper helper, final int x, final int y, final int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    /** The port's {@code GolemHelper.dimensionOf} (package-private) — stable {@code String.hashCode}. */
    private static int dim(final ServerLevel level) {
        return level.dimension().location().hashCode();
    }

    private static void clearGolemCaches() {
        GolemHelper.clearJarListForTest();
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
        golem.restrictTo(home, EntityGolemBase.HOME_RANGE);
        level.addFreshEntity(golem);
        return golem;
    }

    /** Marks a block for the golem on one face with a concrete colour. */
    private static void mark(final EntityGolemBase golem, final BlockPos pos, final Direction side) {
        golem.addMarker(new Marker(pos.getX(), pos.getY(), pos.getZ(), dim((ServerLevel) golem.level()),
                (byte) side.get3DDataValue(), MARKER_COLOUR));
    }

    private static Container containerAt(final ServerLevel level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Container container ? container : null;
    }

    private static net.minecraft.world.level.block.entity.BlockEntity blockEntityAt(final ServerLevel level,
                                                                                   final BlockPos pos) {
        return level.getBlockEntity(pos);
    }

    private static int countOf(final Container container, final ItemStack sample) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            final ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, sample)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    // ------------------------------------------------------------------ markers ---

    /**
     * The marker colour system is what every task targets through. This probe
     * pins three facts against <b>real chests</b>: an unmarked chest is invisible
     * to {@code getMarkedContainers}; a marked one resolves its marked face; and
     * the {@code -1} query colour is the wildcard that matches a marker of any
     * colour — while a concrete non-matching colour does not.
     */
    @GameTest(template = "empty_24")
    public static void markersTargetOnlyTheMarkedChestOnItsMarkedFace(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(2, 1, 2, Blocks.CHEST);
        helper.setBlock(6, 1, 2, Blocks.CHEST);

        final BlockPos markedPos = at(helper, 2, 1, 2);
        final BlockPos unmarkedPos = at(helper, 6, 1, 2);
        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 3), EnumGolemType.IRON, 0);
        mark(golem, markedPos, Direction.UP);

        final List<Container> marked = GolemHelper.getMarkedContainers(level, golem);
        helper.assertTrue(marked.size() == 1, "only the marked chest is targeted, got " + marked.size());
        helper.assertTrue(marked.get(0) == containerAt(level, markedPos),
                "and it is the chest the marker named");
        helper.assertTrue(containerAt(level, unmarkedPos) != null,
                "the second chest really exists — it is unmarked, not absent");
        for (final Container container : marked) {
            helper.assertTrue(container != containerAt(level, unmarkedPos),
                    "an unmarked chest must never be targeted");
        }

        // The marked face, and the colour rules, on the real block entity.
        helper.assertTrue(GolemHelper.getMarkedSides(golem, blockEntityAt(level, markedPos), MARKER_COLOUR)
                        .equals(List.of(Direction.UP.get3DDataValue())),
                "the marker's own colour resolves its own face");
        helper.assertTrue(GolemHelper.getMarkedSides(golem, blockEntityAt(level, markedPos), (byte) -1)
                        .equals(List.of(Direction.UP.get3DDataValue())),
                "the -1 query colour is the wildcard, so it matches the marked face");
        helper.assertTrue(GolemHelper.getMarkedSides(golem, blockEntityAt(level, markedPos), (byte) 4).isEmpty(),
                "a different concrete colour matches nothing on that face");
        helper.assertTrue(GolemHelper.contained(golem.getMarkers(),
                        new Marker(markedPos.getX(), markedPos.getY(), markedPos.getZ(),
                                dim(level), (byte) Direction.DOWN.get3DDataValue(), (byte) -1)) == false,
                "a face with no marker is not contained, even with the wildcard colour");

        helper.succeed();
    }

    /**
     * {@code ADJACENT_RANGE} is used <b>squared</b>, exactly as the original:
     * {@code func_70092_e(...) < 4.0} ({@code GolemHelper.java:55, 75}) is a
     * squared-distance test, i.e. a 2-block radius. This probe pins that boundary
     * against a real chest — a golem one block away sees it, two blocks away
     * (squared distance 4.25) does not.
     */
    @GameTest(template = "empty_24")
    public static void adjacencyUsesTheSquaredFourRange(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(2, 1, 2, Blocks.CHEST);
        final BlockPos chestPos = at(helper, 2, 1, 2);

        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 3), EnumGolemType.IRON, 0);
        mark(golem, chestPos, Direction.UP);
        helper.assertTrue(GolemHelper.getMarkedContainersAdjacentToGolem(level, golem).size() == 1,
                "a golem one block from the marked chest sees it (squared distance 1.25 < 4.0)");

        // Move to two blocks out: squared distance becomes 0.5^2 + 2.0^2 = 4.25 > 4.0.
        golem.moveTo(at(helper, 2, 1, 4).getX() + 0.5, at(helper, 2, 1, 4).getY(),
                at(helper, 2, 1, 4).getZ() + 0.5, 0.0F, 0.0F);
        helper.assertTrue(GolemHelper.getMarkedContainersAdjacentToGolem(level, golem).isEmpty(),
                "two blocks out the squared distance is 4.25, past the original's < 4.0 boundary "
                        + "— the constant is NOT a 4-block radius");
        helper.assertTrue(GolemHelper.getMarkedContainers(level, golem).size() == 1,
                "the chest is still marked and reachable, just no longer adjacent");
        helper.succeed();
    }

    // ----------------------------------------------------- core 0/2: HOME_PLACE ---

    /**
     * Core 0 (Fill) / core 2 (Gather) deposit: the carried stack really lands in
     * the container at the golem's home face, and an unmarked chest elsewhere in
     * the level is untouched.
     */
    @GameTest(template = "empty_24")
    public static void fillCoreDepositsItsCarryIntoTheHomeContainer(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(2, 1, 3, Blocks.CHEST);
        helper.setBlock(6, 1, 3, Blocks.CHEST);

        for (final int core : List.of(0, 2)) {
            helper.setBlock(2, 1, 3, Blocks.CHEST);
            final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 2), EnumGolemType.WOOD, core);
            final BlockPos home = GolemHelper.homeContainerPos(golem);
            final Container homeChest = containerAt(level, home);
            helper.assertTrue(homeChest != null,
                    "core " + core + ": the home container is the chest at home - facing, at " + home);
            // setBlock on an already-existing chest keeps its block entity, so the
            // second iteration would otherwise see the first core's deposit.
            homeChest.clearContent();

            final ItemStack carried = new ItemStack(Items.STICK, 8);
            golem.setCarried(carried);
            helper.assertTrue(golem.runCoreTaskStep(),
                    "core " + core + " (" + golem.getCoreTask().coreName() + ") performs its deposit act");
            helper.assertTrue(countOf(homeChest, carried) == 8,
                    "all 8 carried sticks landed in the home chest, got " + countOf(homeChest, carried));
            helper.assertTrue(golem.getCarried() == null,
                    "and the golem is no longer carrying them");
            helper.assertTrue(countOf(containerAt(level, at(helper, 6, 1, 3)), carried) == 0,
                    "an unmarked chest elsewhere in the level is untouched");

            // Nothing carried: the act must refuse rather than invent a stack.
            helper.assertTrue(!golem.runCoreTaskStep(),
                    "core " + core + " with an empty carry does nothing");
            golem.discard();
        }
        helper.succeed();
    }

    // ------------------------------------------------------- core 1/8: HOME_TAKE ---

    /**
     * Core 1 (Empty) withdraws from its home container exactly what the core's
     * need list asks for, bounded by {@code getCarrySpace()} — which is
     * {@code getCarryLimit()} for an empty-handed golem. WOOD's carry limit is 4,
     * so a golem holding a 3-stack withdraws 3 and the chest keeps the rest.
     */
    @GameTest(template = "empty_24")
    public static void emptyCoreWithdrawsWhatItsCoreNeedsFromTheHomeContainer(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(2, 1, 3, Blocks.CHEST);
        helper.setBlock(6, 1, 3, Blocks.CHEST);

        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 2), EnumGolemType.WOOD, 1);
        final ItemStack sample = new ItemStack(Items.STICK, 1);

        // The home container the golem withdraws from.
        final Container home = containerAt(level, GolemHelper.homeContainerPos(golem));
        helper.assertTrue(home != null, "the home container exists at home - facing");
        home.setItem(0, new ItemStack(Items.STICK, 10));

        // The golem inventory says which stacks it wants; a marked chest with room
        // elsewhere in range is what makes findSomethingEmptyCore accept them.
        golem.getInventory().setItem(0, new ItemStack(Items.STICK, 3));
        final BlockPos destination = at(helper, 6, 1, 3);
        mark(golem, destination, Direction.UP);

        helper.assertTrue(golem.getCarryLimit() == 4, "WOOD's carry limit is its base 4 with no Earth upgrade");
        helper.assertTrue(golem.runCoreTaskStep(), "core 1 performs its withdraw act");
        helper.assertTrue(countOf(home, sample) == 7,
                "the home chest really lost 3 sticks, got " + countOf(home, sample));
        helper.assertTrue(golem.getCarried() != null && golem.getCarried().getCount() == 3,
                "the golem now carries the 3 it took");
        helper.succeed();
    }

    // -------------------------------------------------- core 6: ESSENTIA_EMPTY ---

    /**
     * Core 6 (Alchemy) hands its carried essentia to a marked jar. The amount the
     * golem keeps is exactly what the jar could <em>not</em> take — the original's
     * {@code jar.addToContainer(aspect, essentiaAmount)} remainder
     * ({@code AIEssentiaEmpty.java:30-60}) — so both the jar's contents and the
     * golem's {@code essentiaAmount} are asserted, not a method call.
     */
    @GameTest(template = "empty_24")
    public static void alchemyCoreHandsItsEssentiaToAJarKeepingOnlyTheRemainder(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(4, 1, 2, CatalogEssentiaStorage.jarState(false));
        final Aspect aer = Aspect.getAspect("aer");
        helper.assertTrue(aer != null, "the shipped aspect graph defines aer");

        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 2), EnumGolemType.WOOD, 6);
        final BlockPos jarPos = at(helper, 4, 1, 2);
        final TileJarFillable jar = level.getBlockEntity(jarPos) instanceof TileJarFillable fillable ? fillable : null;
        helper.assertTrue(jar != null, "blockjar must place a TileJarFillable");
        mark(golem, jarPos, Direction.UP);

        // (a) The jar has room for everything: the golem empties completely.
        golem.setEssentia(aer, 30);
        helper.assertTrue(golem.runCoreTaskStep(), "core 6 performs its essentia act");
        helper.assertTrue(jar.getEssentiaAmount(Direction.UP) == 30,
                "the jar received all 30 essentia, got " + jar.getEssentiaAmount(Direction.UP));
        helper.assertTrue(golem.getEssentiaAmount() == 0 && golem.getEssentia() == null,
                "and the golem is empty, got amount " + golem.getEssentiaAmount());

        // (b) The jar is nearly full: the golem keeps exactly the remainder.
        // setAspects is a faithful no-op (the original fills only through
        // addToContainer), so the jar is loaded the way the game loads it.
        final int capacity = jar.maxAmount();
        final int held = jar.getEssentiaAmount(Direction.UP);
        final int toCap = capacity - 5 - held;
        helper.assertTrue(toCap > 0, "the jar has room to reach capacity - 5 after the first feed");
        helper.assertTrue(jar.addToContainer(aer, toCap) == 0,
                "topping the jar up to capacity - 5 leaves nothing over (it held " + held
                        + " of " + capacity + ")");
        helper.assertTrue(jar.getEssentiaAmount(Direction.UP) == capacity - 5,
                "the jar really holds capacity - 5 before the second feed");
        golem.setEssentia(aer, 30);
        helper.assertTrue(golem.runCoreTaskStep(), "core 6 acts again");
        helper.assertTrue(jar.getEssentiaAmount(Direction.UP) == capacity,
                "the jar is filled to its capacity " + capacity
                        + ", got " + jar.getEssentiaAmount(Direction.UP));
        helper.assertTrue(golem.getEssentiaAmount() == 25,
                "the golem keeps exactly what did not fit (30 - 5), got " + golem.getEssentiaAmount());
        helper.assertTrue(golem.getEssentia() == aer, "and still carries that aspect");
        helper.succeed();
    }

    // --------------------------------------------------- core 10: SORTING_PLACE ---

    /**
     * Core 10 (Sorting) moves goods out of its home container into a marked
     * container that already holds that stack — {@code GolemHelper.getContainersWithGoods}
     * is the "already holds it" query ({@code AISortingPlace}).
     *
     * <p><b>Note on the original's quirk:</b> {@code getContainersWithGoods}
     * probes with {@code extractStack(..., simulate = false)}, i.e. the query
     * itself removes the matched stack from the candidate. The port keeps that
     * ({@code GolemHelper.java:168-186}), so the probe asserts that the goods
     * <em>left the home container</em> and that the destination holds them — not
     * an exact conservation total, which the original does not provide either.</p>
     */
    @GameTest(template = "empty_24")
    public static void sortingCoreMovesGoodsToAContainerThatAlreadyHoldsThem(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(2, 1, 3, Blocks.CHEST);
        helper.setBlock(6, 1, 3, Blocks.CHEST);
        final ItemStack sample = new ItemStack(Items.STICK, 1);

        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 2), EnumGolemType.WOOD, 10);
        final Container home = containerAt(level, GolemHelper.homeContainerPos(golem));
        helper.assertTrue(home != null, "the home container exists at home - facing");
        home.setItem(0, new ItemStack(Items.STICK, 8));

        final BlockPos destination = at(helper, 6, 1, 3);
        final Container target = containerAt(level, destination);
        target.setItem(0, new ItemStack(Items.STICK, 1));
        mark(golem, destination, Direction.UP);

        final List<ItemStack> goods = GolemHelper.getItemsInHomeContainer(golem);
        helper.assertTrue(goods != null && !goods.isEmpty(),
                "the home container's goods are readable for the sort pass");

        helper.assertTrue(golem.runCoreTaskStep(), "core 10 performs its sort act");
        helper.assertTrue(countOf(home, sample) < 8,
                "goods really left the home container, got " + countOf(home, sample));
        helper.assertTrue(countOf(target, sample) >= 1,
                "and the marked container that already held them still holds them, got "
                        + countOf(target, sample));
        helper.succeed();
    }

    // --------------------------------------------------------------- persistence ---

    /**
     * The persistence contract: save a fully populated golem to NBT, build a
     * fresh entity from the same type and load that tag, then assert every field
     * the card names round-trips — owner, core, decoration, upgrades, markers,
     * essentia, health, carry, inventory, home facing and area.
     *
     * <p>The original only writes the carry payload for the core that uses it
     * (core 5 fluids, core 6 essentia) and only the cores with an inventory get
     * one, so this uses <b>two</b> golems: a core 1 golem for the inventory half
     * and a core 6 golem for the essentia half.</p>
     */
    @GameTest(template = "empty_24")
    public static void golemStateSurvivesSaveAndReload(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(2, 1, 3, Blocks.CHEST);
        helper.setBlock(6, 1, 3, Blocks.CHEST);
        final Aspect aer = Aspect.getAspect("aer");

        // ---- (a) core 1: inventory, owner, upgrades, markers, decoration, home ----
        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 2), EnumGolemType.IRON, 1);
        golem.setAdvanced(true);
        golem.setDecoration("HGB");
        golem.setOwnerName("probe-owner");
        // IRON + advanced has material.upgrades (1) + 1 = 2 slots, so the saved
        // array must be that wide: the original resizes to the slot count on load
        // and pads with -1, which would change an over-wide array.
        helper.assertTrue(golem.upgradeSlotCount() == 2,
                "IRON + advanced has 2 upgrade slots, got " + golem.upgradeSlotCount());
        golem.setUpgrades(new byte[] {1, 3});
        golem.setTogglesValue((byte) 0b0010_0101);
        golem.getInventory().setItem(0, new ItemStack(Items.STICK, 5));
        golem.getInventory().setItem(1, new ItemStack(Items.IRON_INGOT, 2));
        golem.setCarried(new ItemStack(Items.DIAMOND, 3));
        mark(golem, at(helper, 6, 1, 3), Direction.UP);
        mark(golem, at(helper, 2, 1, 3), Direction.NORTH);
        golem.setHealth(12.0F);

        final CompoundTag tag = golem.saveWithoutId(new CompoundTag());
        final EntityGolemBase loaded = CatalogGolems.GOLEM.get().create(level);
        helper.assertTrue(loaded != null, "the golem type must build a reload target");
        loaded.load(tag);

        helper.assertTrue(loaded.getOwnerName().equals("probe-owner"),
                "the owner round-trips, got '" + loaded.getOwnerName() + "'");
        helper.assertTrue(loaded.getCore() == 1, "the core round-trips, got " + loaded.getCore());
        helper.assertTrue(loaded.getCoreTask() == GolemCoreTask.EMPTY, "and resolves to its task table row");
        helper.assertTrue(loaded.getDecoration().equals("HGB"),
                "the decoration round-trips, got '" + loaded.getDecoration() + "'");
        helper.assertTrue(loaded.isAdvanced(), "the advanced flag round-trips");
        helper.assertTrue(loaded.getMaterial() == EnumGolemType.IRON,
                "the material round-trips, got " + loaded.getMaterial());
        helper.assertTrue(loaded.getHomeFacing() == HOME_FACING,
                "the home facing round-trips, got " + loaded.getHomeFacing());
        helper.assertTrue(loaded.getRestrictCenter().equals(golem.getRestrictCenter()),
                "the home area round-trips, got " + loaded.getRestrictCenter());
        helper.assertTrue(loaded.getTogglesValue() == golem.getTogglesValue(),
                "the packed toggle byte round-trips");
        helper.assertTrue(loaded.getGolemStrength() == golem.getGolemStrength(),
                "the Earth upgrades round-trip: strength " + loaded.getGolemStrength());
        helper.assertTrue(loaded.getRange() == golem.getRange(),
                "the Water upgrades round-trip: range " + loaded.getRange());
        helper.assertTrue(loaded.getCarryLimit() == golem.getCarryLimit(),
                "so does the load-bearing carry limit, got " + loaded.getCarryLimit());
        helper.assertTrue(loaded.upgradeSlotCount() == golem.upgradeSlotCount(),
                "the advanced extra upgrade slot survives the resize on load");
        helper.assertTrue(loaded.getMarkers().size() == 2,
                "both markers round-trip, got " + loaded.getMarkers().size());
        helper.assertTrue(loaded.getMarkers().contains(golem.getMarkers().get(0))
                        && loaded.getMarkers().contains(golem.getMarkers().get(1)),
                "with their positions, dimensions, faces and colours intact");
        helper.assertTrue(loaded.getCarried() != null
                        && ItemStack.isSameItemSameComponents(loaded.getCarried(), new ItemStack(Items.DIAMOND, 3)),
                "the carried stack round-trips");
        helper.assertTrue(countOf(loaded.getInventory(), new ItemStack(Items.STICK, 1)) == 5
                        && countOf(loaded.getInventory(), new ItemStack(Items.IRON_INGOT, 1)) == 2,
                "the golem inventory round-trips both stacks");
        helper.assertTrue(loaded.getHealth() == 12.0F,
                "health round-trips, got " + loaded.getHealth());
        helper.assertTrue(loaded.getHealthPercentage() == 12.0F / loaded.getMaxHealth(),
                "and so does the synced health percentage, got " + loaded.getHealthPercentage());

        // ---- (b) core 6: the essentia payload ----
        final EntityGolemBase alchemist = spawn(level, at(helper, 2, 1, 4), EnumGolemType.WOOD, 6);
        alchemist.setEssentia(aer, 17);
        final CompoundTag essentiaTag = alchemist.saveWithoutId(new CompoundTag());
        final EntityGolemBase reloadedAlchemist = CatalogGolems.GOLEM.get().create(level);
        reloadedAlchemist.load(essentiaTag);
        helper.assertTrue(reloadedAlchemist.getEssentia() == aer,
                "the carried aspect round-trips, got " + reloadedAlchemist.getEssentia());
        helper.assertTrue(reloadedAlchemist.getEssentiaAmount() == 17,
                "and the amount with it, got " + reloadedAlchemist.getEssentiaAmount());

        // A core with no essentia payload writes none — the original's core gate.
        final EntityGolemBase carrier = spawn(level, at(helper, 6, 1, 4), EnumGolemType.WOOD, 0);
        carrier.setEssentia(aer, 17);
        final EntityGolemBase reloadedCarrier = CatalogGolems.GOLEM.get().create(level);
        reloadedCarrier.load(carrier.saveWithoutId(new CompoundTag()));
        helper.assertTrue(reloadedCarrier.getEssentia() == null && reloadedCarrier.getEssentiaAmount() == 0,
                "a core that does not carry essentia writes no essentia payload (the original's core gate)");
        helper.succeed();
    }

    // --------------------------- persistence: the carried-stack sentinel (t_83e5a20d) ---

    /**
     * The idle golem's <b>second</b> save cycle — the standalone reproduction of the
     * {@code An Entity type entity.thaumcraft.golem has thrown an exception trying to write
     * state. It will not persist.} line the t_04233450 control measured on 5/10 base legs
     * (docs/testing.md#empty-stack-sentinel-save).
     *
     * <p>Why the pre-existing {@link #golemStateSurvivesSaveAndReload} is GREEN over this loss:
     * it saves a freshly <b>constructed</b> golem. The ctor leaves {@code itemCarried} null
     * ({@code EntityGolemBase.java:186}) — 1.7.10's null sentinel, which is why the original's
     * write is {@code if (itemCarried != null)} and an idle golem emits the empty compound
     * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/golems/EntityGolemBase.java:588-594}).
     * The LOAD is the trap: 1.21.1's {@code ItemStack.parseOptional} answers
     * {@code ItemStack.EMPTY} for that empty compound ({@code ItemStack.java:292-294}) where
     * 1.7.10's {@code ItemStack.loadItemStackFromNBT} answered null
     * (original's read, {@code :643-644}), so from the first reload on the field is non-null,
     * the {@code != null} guard lets the encode run, and {@code ItemStack#save} throws
     * {@code IllegalStateException: Cannot encode empty ItemStack} ({@code ItemStack.java:389-391}).
     * NeoForge's patched {@code EntityStorage.storeEntities} catches that per entity — the
     * {@code It will not persist} line, present in {@code compiledWithNeoForge_*.jar}'s
     * {@code EntityStorage.class}, absent from the vanilla decompile — and does not add the tag
     * to the chunk's {@code Entities} list. The golem is gone on the next load, and the game leg
     * stays green because the harness prints {@code All N required tests passed} while the
     * signature rides the shutdown save.</p>
     *
     * <p>So the teeth for this defect class are the SECOND cycle: save → load → save. One cycle
     * cannot see it, and neither can an assert on the live field.</p>
     */
    @GameTest(template = "empty_24")
    public static void anIdleGolemSurvivesASecondSaveCycle(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        // The same populated fixture as golemStateSurvivesSaveAndReload MINUS setCarried:
        // this golem has never held an item in its life.
        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 2), EnumGolemType.IRON, 1);
        populateIdleGolem(helper, golem);
        helper.assertTrue(golem.getCarried() == null,
                "the fixture has never carried an item, getCarried()=" + golem.getCarried());

        // Cycle 1 — the write that has always worked (the field is still the 1.7.10 null).
        final EntityGolemBase loaded = reloadOf(helper, golem);

        // Cycle 2 — the write the census catches. NO carry read before it: the original's
        // getCarried() nulls an emptied field as a side effect (EntityGolemBase.java:687-690),
        // which HEALS the field and the next write — a probe that reads the getter first
        // measures a golem that can never reproduce the census (measured: this probe passed
        // with an assert on getCarried() sitting before this save).
        final CompoundTag second = new CompoundTag();
        try {
            helper.assertTrue(loaded.saveAsPassenger(second),
                    "the chunk-save frame (EntityStorage.storeEntities:95 -> Entity#save:1731 ->"
                            + " Entity#saveAsPassenger) keeps the idle golem; false is the entity"
                            + " being dropped out of the chunk's Entities list");
        } catch (final RuntimeException e) {
            // NOTE for the census grep: do NOT paste EntityStorage's verbatim log phrase in a
            // probe message — the gate counts that string in the leg logs, and a probe that
            // quotes it inflates the count it is measuring (measured: 1 self-match in a leg).
            helper.fail("the idle golem throws on its SECOND save, which is the per-entity catch"
                    + " that makes NeoForge's EntityStorage drop the entity out of the chunk data"
                    + " (the golem never reaches the region file, so it is gone on reload)."
                    + " Chain: " + describe(e));
        }

        // ... and the state that must survive it.
        helper.assertTrue(loaded.getCarried() == null,
                "the sentinel getter reads a loaded golem's EMPTY carry as the original's null —"
                        + " and heals the field for the next write, got " + loaded.getCarried());
        final EntityGolemBase back = CatalogGolems.GOLEM.get().create(level);
        back.load(second);
        back.setup(HOME_FACING);                                     // the load-path install (BD-31)
        assertIdleGolemState(helper, golem, back, "the golem reloaded from the SECOND cycle's tag");
        helper.succeed();
    }

    /**
     * The drop arm on the frame that performs the drop: {@code Entity#save} is what
     * {@code EntityStorage.storeEntities:95} calls, and its {@code false} answer is the entity
     * leaving the chunk data. Both shapes are pinned — the idle golem (the census's) and the
     * burdened golem, whose carried stack must still ENCODE (the fix must gate the encode, not
     * delete the payload).
     */
    @GameTest(template = "empty_24")
    public static void theChunkSaveFrameKeepsTheIdleAndTheBurdenedGolem(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();

        final EntityGolemBase idle = spawn(level, at(helper, 2, 1, 2), EnumGolemType.WOOD, 4);
        idle.setTogglesValue((byte) 0b0001_0010);
        final CompoundTag idleTag = new CompoundTag();
        try {
            helper.assertTrue(idle.save(idleTag),
                    "the storeEntities frame keeps a freshly placed idle golem");
        } catch (final RuntimeException e) {
            helper.fail("a freshly placed idle golem fails the chunk-save frame: " + describe(e));
        }
        helper.assertTrue("thaumcraft:golem".equals(idleTag.getString("id")),
                "the idle golem's chunk entry carries its entity id, got '" + idleTag.getString("id") + "'");
        helper.assertTrue(idleTag.get("ItemCarried") instanceof CompoundTag carriedIdle
                        && carriedIdle.isEmpty(),
                "an idle golem writes ItemCarried as the empty compound the original wrote"
                        + " (EntityGolemBase.java:588-594), got " + idleTag.get("ItemCarried"));

        // The burdened golem: the payload must survive as bytes, not as an empty compound.
        final EntityGolemBase burdened = spawn(level, at(helper, 6, 1, 2), EnumGolemType.WOOD, 1);
        burdened.setCarried(new ItemStack(Items.DIAMOND, 3));
        final CompoundTag burdenedTag = new CompoundTag();
        try {
            helper.assertTrue(burdened.save(burdenedTag),
                    "the storeEntities frame keeps a golem that carries a stack");
        } catch (final RuntimeException e) {
            helper.fail("a burdened golem fails the chunk-save frame: " + describe(e));
        }
        final CompoundTag carriedTag = burdenedTag.getCompound("ItemCarried");
        // 1.21.1's ItemStack codec writes `count` (ItemStack.java:103-114, the CODEC's fields),
        // not 1.7.10's `Count` — pin the key the platform writes, not the one the original wrote.
        helper.assertTrue("minecraft:diamond".equals(carriedTag.getString("id"))
                        && carriedTag.getInt("count") == 3,
                "the carried stack encodes with its 1.21.1 id/count keys, got " + carriedTag);
        final EntityGolemBase burdenedBack = CatalogGolems.GOLEM.get().create(level);
        burdenedBack.load(burdenedTag);
        helper.assertTrue(ItemStack.isSameItemSameComponents(burdenedBack.getCarried(), new ItemStack(Items.DIAMOND, 3)),
                "the reload hands the payload back through the entity's own reader, got "
                        + burdenedBack.getCarried());

        // The census's own shape: the idle golem AFTER a load, saved by the same frame.
        final EntityGolemBase reloaded = reloadOf(helper, idle);
        final CompoundTag again = new CompoundTag();
        try {
            helper.assertTrue(reloaded.save(again),
                    "the storeEntities frame keeps the idle golem on the save AFTER its reload —"
                            + " the census's drop, in-frame");
        } catch (final RuntimeException e) {
            helper.fail("the idle golem fails the chunk-save frame once it has been loaded, so"
                    + " NeoForge drops it from the chunk data: " + describe(e));
        }
        final EntityGolemBase third = CatalogGolems.GOLEM.get().create(level);
        third.load(again);
        helper.assertTrue(third.getType() == idle.getType() && third.getCore() == 4
                        && third.getTogglesValue() == (byte) 0b0001_0010,
                "the entity parses back as the same golem with its toggles, core " + third.getCore()
                        + " toggles " + third.getTogglesValue());
        helper.succeed();
    }

    /**
     * The codec-level arms, pinned so a platform change that moves the sentinel reports by name.
     * {@code ItemStack#save} refuses an empty stack and {@code saveOptional} is vanilla's own
     * gate ({@code ItemStack.java:397-399}, and the same guard mob hand items go through at
     * {@code Mob.java:395-401}); {@code FluidStack#save} refuses an empty one for the core-5
     * bucket arm ({@code neoforge/fluids/FluidStack.java:270-276}). And
     * {@code parseOptional} answers a NON-NULL empty stack, which is what turns the original's
     * null sentinel into a throwing field on the second save (docs/testing.md#empty-stack-sentinel-save).
     */
    @GameTest(template = "empty_24")
    public static void anEmptyCarriedStackIsNeverEncoded(final GameTestHelper helper) {
        final var registries = helper.getLevel().registryAccess();

        boolean itemThrew = false;
        String itemMessage = "";
        try {
            ItemStack.EMPTY.save(registries);
        } catch (final IllegalStateException e) {
            itemThrew = true;
            itemMessage = e.getMessage();
        }
        helper.assertTrue(itemThrew && itemMessage != null && itemMessage.contains("Cannot encode empty ItemStack"),
                "the platform fact this card rests on: ItemStack#save refuses an empty stack, got threw="
                        + itemThrew + " message=" + itemMessage);
        helper.assertTrue(((CompoundTag) ItemStack.EMPTY.saveOptional(registries)).isEmpty(),
                "ItemStack#saveOptional is the vanilla gate that writes the empty compound (ItemStack.java:397-399)");
        helper.assertTrue(ItemStack.parseOptional(registries, new CompoundTag()).isEmpty(),
                "parseOptional answers a NON-NULL empty stack for the empty compound - the load-side"
                        + " half of the trap that makes a one-cycle save probe a false green");

        boolean fluidThrew = false;
        String fluidMessage = "";
        try {
            FluidStack.EMPTY.save(registries);
        } catch (final IllegalStateException e) {
            fluidThrew = true;
            fluidMessage = e.getMessage();
        }
        helper.assertTrue(fluidThrew && fluidMessage != null && fluidMessage.contains("Cannot encode empty FluidStack"),
                "the core-5 sibling: FluidStack#save refuses an empty stack (FluidStack.java:270-276),"
                        + " got threw=" + fluidThrew + " message=" + fluidMessage);
        helper.assertTrue(((CompoundTag) FluidStack.EMPTY.saveOptional(registries)).isEmpty(),
                "FluidStack#saveOptional writes the empty compound for the empty stack");

        // And the golem's own write: idle = empty compound, burdened = real bytes.
        final EntityGolemBase idle = spawn(helper.getLevel(), at(helper, 2, 1, 2), EnumGolemType.STONE, 5);
        final CompoundTag idleTag = idle.saveWithoutId(new CompoundTag());
        helper.assertTrue(idleTag.get("ItemCarried") instanceof CompoundTag carried
                        && carried.isEmpty(),
                "an idle golem's ItemCarried bytes are the original's empty compound, got "
                        + idleTag.get("ItemCarried"));
        helper.assertTrue(!(idleTag.get("fluidCarried") instanceof CompoundTag fluid) || fluid.isEmpty(),
                "a core-5 golem with nothing in the bucket writes no fluid payload (the original's"
                        + " null guard), got " + idleTag.get("fluidCarried"));
        final EntityGolemBase filled = spawn(helper.getLevel(), at(helper, 6, 1, 2), EnumGolemType.STONE, 5);
        filled.setFluidCarried(new FluidStack(Fluids.WATER, 1000));
        final CompoundTag filledTag = filled.saveWithoutId(new CompoundTag());
        final FluidStack filledBack = FluidStack.parseOptional(registries, filledTag.getCompound("fluidCarried"));
        helper.assertTrue(filledBack.getAmount() == 1000 && filledBack.is(Fluids.WATER),
                "a filled bucket round-trips through the same key, got " + filledBack);
        helper.succeed();
    }

    /**
     * The core-5 (Decanting) sibling arm: the bucket payload is written under the same
     * {@code != null} habit ({@code EntityGolemBase.java:1195}), so a bucket golem that has been
     * loaded once — {@code fluidCarried} back as {@code FluidStack.EMPTY}, non-null — throws
     * {@code Cannot encode empty FluidStack} on every later save and is dropped with the whole
     * entity. Same second-cycle teeth as the item arm, plus the filled bucket's round-trip.
     */
    @GameTest(template = "empty_24")
    public static void aBucketGolemSurvivesASecondSaveCycle(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        final EntityGolemBase bucket = spawn(level, at(helper, 2, 1, 2), EnumGolemType.IRON, 5);
        bucket.setDecoration("B");
        helper.assertTrue(bucket.getFluidCarried() == null,
                "the fixture bucket is empty on the field (the original's null sentinel)");

        final EntityGolemBase loaded = reloadOf(helper, bucket);
        // The fluid field has no healing getter (getFluidCarried is a plain read,
        // EntityGolemBase.java:722-724), so this reads exactly what the load wrote: the
        // original's null on a fixed tree, FluidStack.EMPTY on the pre-fix one.
        helper.assertTrue(loaded.getFluidCarried() == null,
                "the reload of a bucket golem with no fluid leaves the field at the original's null"
                        + " sentinel, got " + loaded.getFluidCarried());
        final CompoundTag second = new CompoundTag();
        try {
            helper.assertTrue(loaded.saveAsPassenger(second),
                    "the chunk-save frame keeps the idle bucket golem on its second save");
        } catch (final RuntimeException e) {
            helper.fail("the idle bucket golem throws on its SECOND save (the fluid arm of the same"
                    + " != null habit), so the entity is dropped from the chunk data: " + describe(e));
        }

        // The payload half: a real bucket survives the same two cycles.
        final EntityGolemBase filled = reloadOf(helper, filledWith(level, bucket));
        helper.assertTrue(filled.getFluidCarried() != null
                        && filled.getFluidCarried().getAmount() == 1000
                        && filled.getFluidCarried().is(Fluids.WATER),
                "the 1000 mB of water round-trips through the second cycle, got " + filled.getFluidCarried());
        helper.succeed();
    }

    // -------------------------------------------------- probe helpers for the arms above ---

    /** A populated idle golem: upgrade, toggle, decoration, inventory, marker, health. No carry. */
    private static void populateIdleGolem(final GameTestHelper helper, final EntityGolemBase golem) {
        golem.setAdvanced(true);
        golem.setDecoration("HGB");
        golem.setUpgrades(new byte[] {1, 3});
        golem.setTogglesValue((byte) 0b0010_0101);
        golem.getInventory().setItem(0, new ItemStack(Items.STICK, 5));
        golem.getInventory().setItem(1, new ItemStack(Items.IRON_INGOT, 2));
        mark(golem, at(helper, 6, 1, 3), Direction.UP);
        mark(golem, at(helper, 2, 1, 3), Direction.NORTH);
        golem.setHealth(12.0F);
    }

    /** A copy of the golem that has been put through a filled-bucket write. */
    private static EntityGolemBase filledWith(final ServerLevel level, final EntityGolemBase source) {
        final EntityGolemBase copy = CatalogGolems.GOLEM.get().create(level);
        copy.setMaterial(source.getMaterial());
        copy.setCore(5);
        copy.setDecoration("B");
        copy.setFluidCarried(new FluidStack(Fluids.WATER, 1000));
        copy.moveTo(source.getX(), source.getY(), source.getZ(), 0.0F, 0.0F);
        copy.setup(HOME_FACING);
        return copy;
    }

    /**
     * One save/load cycle on the vanilla frames: {@code Entity#saveAsPassenger} (the frame
     * {@code EntityStorage.storeEntities} calls) into a fresh tag, then {@code Entity#load} into a
     * fresh entity of the same type. A throw out of the first frame is the census's signature and
     * is NOT caught here.
     */
    private static EntityGolemBase reloadOf(final GameTestHelper helper, final EntityGolemBase source) {
        final CompoundTag tag = new CompoundTag();
        final boolean kept;
        try {
            kept = source.saveAsPassenger(tag);
        } catch (final RuntimeException e) {
            helper.fail("the save frame throws for " + source + ": " + describe(e));
            return null;
        }
        helper.assertTrue(kept, "the save frame kept " + source + " in the chunk data");
        final EntityGolemBase loaded = CatalogGolems.GOLEM.get().create(source.level());
        loaded.load(tag);
        return loaded;
    }

    /** Everything the card names as "the state survives": goal install, upgrades, toggles, payload. */
    private static void assertIdleGolemState(final GameTestHelper helper, final EntityGolemBase source,
                                             final EntityGolemBase back, final String when) {
        helper.assertTrue(back.getOwnerName().equals(source.getOwnerName()),
                when + ": the owner round-trips, got '" + back.getOwnerName() + "'");
        helper.assertTrue(back.getCore() == source.getCore()
                        && back.getCoreTask() == source.getCoreTask(),
                when + ": the core and its task row round-trip, got " + back.getCore());
        helper.assertTrue(back.getMaterial() == source.getMaterial(), when + ": the material round-trips");
        helper.assertTrue(back.isAdvanced(), when + ": the advanced flag round-trips");
        helper.assertTrue(back.getDecoration().equals(source.getDecoration()),
                when + ": the decoration round-trips, got '" + back.getDecoration() + "'");
        helper.assertTrue(back.getTogglesValue() == source.getTogglesValue(),
                when + ": the toggle byte round-trips, got " + back.getTogglesValue());
        helper.assertTrue(back.getGolemStrength() == source.getGolemStrength()
                        && back.getRange() == source.getRange()
                        && back.getCarryLimit() == source.getCarryLimit(),
                when + ": the upgrade-derived figures round-trip, strength " + back.getGolemStrength()
                        + " range " + back.getRange() + " carry limit " + back.getCarryLimit());
        helper.assertTrue(back.upgradeSlotCount() == source.upgradeSlotCount(),
                when + ": the upgrade slot count round-trips");
        helper.assertTrue(back.getHomeFacing() == source.getHomeFacing()
                        && back.getRestrictCenter().equals(source.getRestrictCenter()),
                when + ": the home facing and area round-trip");
        helper.assertTrue(back.getMarkers().size() == source.getMarkers().size()
                        && back.getMarkers().containsAll(source.getMarkers()),
                when + ": the markers round-trip, got " + back.getMarkers().size());
        helper.assertTrue(countOf(back.getInventory(), new ItemStack(Items.STICK, 1)) == 5
                        && countOf(back.getInventory(), new ItemStack(Items.IRON_INGOT, 1)) == 2,
                when + ": the golem inventory round-trips");
        helper.assertTrue(back.getHealth() == 12.0F, when + ": health round-trips, got " + back.getHealth());
        helper.assertTrue(back.getCarried() == null,
                when + ": the reload leaves an idle golem's carry at the null sentinel, got " + back.getCarried());
        helper.assertTrue(back.getCarrySpace() == source.getCarrySpace(),
                when + ": getCarrySpace keeps the original's null branch (EntityGolemBase.java:812-816),"
                        + " got " + back.getCarrySpace() + " over " + source.getCarrySpace());
        helper.assertTrue(goalInstall(back.goalSelector).equals(goalInstall(source.goalSelector)),
                when + ": the load-path goal install matches the source's, source="
                        + goalInstall(source.goalSelector) + " reloaded=" + goalInstall(back.goalSelector));
    }

    /** The goal set the load-path install produces: priority -> goal class, sorted. */
    private static List<String> goalInstall(final GoalSelector selector) {
        final List<String> out = new ArrayList<>();
        for (final WrappedGoal entry : selector.getAvailableGoals()) {
            out.add(entry.getPriority() + ":" + entry.getGoal().getClass().getSimpleName());
        }
        Collections.sort(out);
        return out;
    }

    /** The full cause chain of a save failure, on one line, so a red names the mechanism. */
    private static String describe(final Throwable error) {
        final StringBuilder out = new StringBuilder();
        for (Throwable t = error; t != null; t = t.getCause()) {
            out.append(t.getClass().getName()).append(": ").append(t.getMessage()).append(" | ");
        }
        return out.toString();
    }
}
