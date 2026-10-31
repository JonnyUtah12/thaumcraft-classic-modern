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
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
}
