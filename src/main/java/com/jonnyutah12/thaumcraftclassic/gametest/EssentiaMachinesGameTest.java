package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCentrifuge;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEssentiaCrystalizer;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTube;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jonnyutah12.thaumcraftclassic.blocks.BlockTube.VARIANT_BUFFER;
import static com.jonnyutah12.thaumcraftclassic.blocks.BlockTube.VARIANT_CENTRIFUGE;
import static com.jonnyutah12.thaumcraftclassic.blocks.BlockTube.VARIANT_CRYSTALIZER;
import static com.jonnyutah12.thaumcraftclassic.blocks.BlockTube.VARIANT_TUBE;

/**
 * TC-M4-07-m evidence: world probes that run a <b>real essentia conversion</b>
 * in a real world through the two machines the original hosted on the tube
 * block (centrifuge: metadata 2, crystallizer: metadata 7).
 *
 * <p>Reference: {@code reference/decompiled/thaumcraft/thaumcraft/common/tiles/TileCentrifuge.java}
 * and {@code TileEssentiaCrystalizer.java}. The rules themselves are pinned as
 * numbers in {@code CentrifugeLogicTest} / {@code CrystalizerLogicTest}; what
 * these probes prove is that a machine placed in a world, fed by a real
 * essentia line, runs the conversion end to end: a compound is drawn up out of
 * the line below, spun into one of its components, a primal is refused, the
 * split unit is taken away by a consumer on the output face with the total
 * conserved, and a crystal essence lands in the world carrying the stored
 * aspect.</p>
 *
 * <p>No vis node exists in the test world, so the crystallizer's EARTH draw
 * returns 0 and it advances at the free 1 per fifth tick — exactly what the
 * original does with no node nearby; the probe runs long enough (1050 ticks)
 * to reach the eject at 200.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EssentiaMachinesGameTest {

    private EssentiaMachinesGameTest() {
    }

    private static Aspect potentia() {
        return Aspect.getAspect("potentia");
    }

    private static Aspect ignis() {
        return Aspect.getAspect("ignis");
    }

    private static Aspect ordo() {
        return Aspect.getAspect("ordo");
    }

    // --- the centrifuge: draw, spin, split ----------------------------------

    /**
     * A centrifuge (variant 2) fed by a buffer of 3 potentia below: it draws
     * one unit at tick 5 (suction 128 out-sucks the buffer's 16), spins 39
     * ticks, and splits at tick 45 into one of potentia's two components
     * (ignis/ordo — the coin-flip side is world-seed fixed, so the probe
     * accepts either, the split index itself is pinned in the unit test). The
     * split unit sits on the output face: buffer back to 2, input face ready
     * (128), output face advertising one unit at suction 0.
     * (TileCentrifuge.java:185-195, 223-250, 148-150, 159-163.)
     */
    @GameTest(template = "empty")
    public static void centrifugeSpinsACompoundIntoAComponent(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final BlockPos machinePos = helper.absolutePos(new BlockPos(0, 0, 0));
        helper.setBlock(0, 0, 0, CatalogEssentiaTransport.state(VARIANT_CENTRIFUGE));
        helper.setBlock(0, -1, 0, CatalogEssentiaTransport.state(VARIANT_BUFFER));
        final TileTubeBuffer source = (TileTubeBuffer) level.getBlockEntity(machinePos.below());
        final TileCentrifuge centrifuge = (TileCentrifuge) level.getBlockEntity(machinePos);
        helper.assertTrue(source != null && centrifuge != null,
                "the buffer and the centrifuge must both have placed their tiles");

        source.aspects.add(potentia(), 3);

        helper.runAtTickTime(50L, () -> {
            helper.assertTrue(centrifuge.aspectIn == null,
                    "the spin is over: the input slot must be empty, got " + centrifuge.aspectIn);
            helper.assertTrue(centrifuge.aspectOut != null
                            && (ignis().equals(centrifuge.aspectOut) || ordo().equals(centrifuge.aspectOut)),
                    "the output slot must hold one of potentia's components, got " + centrifuge.aspectOut);
            helper.assertTrue(source.aspects.getAmount(potentia()) == 2,
                    "one unit must have been drawn from the line, got "
                            + source.aspects.getAmount(potentia()));
            helper.assertTrue(centrifuge.getSuctionAmount(Direction.DOWN) == 128,
                    "the input face is ready for the next unit (149)");
            helper.assertTrue(centrifuge.getSuctionAmount(Direction.UP) == 0,
                    "the output face never sucks (149)");
            helper.assertTrue(centrifuge.getEssentiaAmount(Direction.UP) == 1,
                    "the split unit is advertised on the output face (155-157)");
            helper.succeed();
        });
    }

    /**
     * A primal is never drawn in: a buffer of 3 ignis (a primal) under the
     * centrifuge keeps all three units, the machine stays idle at the empty
     * suction (128). (TileCentrifuge.java:241 — the draw's primal refusal.)
     */
    @GameTest(template = "empty")
    public static void centrifugeRefusesPrimals(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final BlockPos machinePos = helper.absolutePos(new BlockPos(0, 0, 0));
        helper.setBlock(0, 0, 0, CatalogEssentiaTransport.state(VARIANT_CENTRIFUGE));
        helper.setBlock(0, -1, 0, CatalogEssentiaTransport.state(VARIANT_BUFFER));
        final TileTubeBuffer source = (TileTubeBuffer) level.getBlockEntity(machinePos.below());
        final TileCentrifuge centrifuge = (TileCentrifuge) level.getBlockEntity(machinePos);
        helper.assertTrue(source != null && centrifuge != null,
                "the buffer and the centrifuge must both have placed their tiles");

        source.aspects.add(ignis(), 3);

        helper.runAtTickTime(60L, () -> {
            helper.assertTrue(source.aspects.getAmount(ignis()) == 3,
                    "a primal never enters a centrifuge, got " + source.aspects.getAmount(ignis()));
            helper.assertTrue(centrifuge.aspectIn == null && centrifuge.aspectOut == null,
                    "the machine stays idle");
            helper.assertTrue(centrifuge.getSuctionAmount(Direction.DOWN) == 128,
                    "empty suction stays at 128 (149)");
            helper.succeed();
        });
    }

    /**
     * The split unit leaves through the output face: with an essentia buffer
     * above, the buffer's fill pass takes the component, and the essentia is
     * conserved: 3 units = 1 in the top buffer + 1 spinning in the machine
     * (the second draw) + 1 left in the source. (TileTubeBuffer.fillBuffer +
     * TileCentrifuge.takeEssentia 164-166.)
     */
    @GameTest(template = "empty_24")
    public static void centrifugeOutputIsTakenByTheNeighbour(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final BlockPos machinePos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.setBlock(0, 1, 0, CatalogEssentiaTransport.state(VARIANT_CENTRIFUGE));
        helper.setBlock(0, 0, 0, CatalogEssentiaTransport.state(VARIANT_BUFFER));
        helper.setBlock(0, 2, 0, CatalogEssentiaTransport.state(VARIANT_BUFFER));
        final TileTubeBuffer source = (TileTubeBuffer) level.getBlockEntity(machinePos.below());
        final TileTubeBuffer sink = (TileTubeBuffer) level.getBlockEntity(machinePos.above());
        final TileCentrifuge centrifuge = (TileCentrifuge) level.getBlockEntity(machinePos);
        helper.assertTrue(source != null && sink != null && centrifuge != null,
                "the machine and both buffers must have placed their tiles");

        source.aspects.add(potentia(), 3);

        helper.runAtTickTime(60L, () -> {
            helper.assertTrue(sink.aspects.visSize() == 1
                            && (sink.aspects.getAmount(ignis()) == 1 || sink.aspects.getAmount(ordo()) == 1),
                    "the top buffer must hold exactly the split component, got " + sink.aspects);
            helper.assertTrue(centrifuge.aspectOut == null,
                    "the output slot was emptied by the neighbour");
            helper.assertTrue(centrifuge.aspectIn != null && potentia().equals(centrifuge.aspectIn),
                    "the machine drew a second unit, got " + centrifuge.aspectIn);
            helper.assertTrue(source.aspects.getAmount(potentia()) == 1,
                    "two units drawn in total, got " + source.aspects.getAmount(potentia()));
            helper.assertTrue(source.aspects.visSize() + sink.aspects.visSize()
                            + centrifuge.getAspects().size()
                            + (centrifuge.aspectIn != null ? 1 : 0) == 3,
                    "essentia is conserved: source " + source.aspects.visSize() + " + sink "
                            + sink.aspects.visSize() + " + machine 1 = 3");
            helper.succeed();
        });
    }

    // --- the crystallizer: any aspect + EARTH vis -> crystal -----------------

    /**
     * The full crystal flow: a tube of one potentia below feeds the
     * crystallizer (variant 7, facing down by default) one unit — the
     * crystallizer takes anything, primals included (367-372); every fifth
     * tick it advances (no node in the test world, so the EARTH draw is 0 and
     * the advance is the free 1, 203-217); at 200 (tick 1005) it ejects a
     * crystal essence carrying the stored aspect. The face opposite the input
     * is air, so the original's world-push fires: the crystal lands as a world
     * item carrying 1 potentia (315-341), and the machine resets.
     */
    @GameTest(template = "empty_24", timeoutTicks = 1200)
    public static void crystalizerGrowsACrystal(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final BlockPos machinePos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.setBlock(0, 1, 0, CatalogEssentiaTransport.state(VARIANT_CRYSTALIZER));
        helper.setBlock(0, 0, 0, CatalogEssentiaTransport.state(VARIANT_TUBE));
        final TileTube tube = (TileTube) level.getBlockEntity(machinePos.below());
        final TileEssentiaCrystalizer crystalizer =
                (TileEssentiaCrystalizer) level.getBlockEntity(machinePos);
        helper.assertTrue(tube != null && crystalizer != null,
                "the tube and the crystallizer must both have placed their tiles");

        tube.facing = Direction.UP;
        tube.essentiaType = potentia();
        tube.essentiaAmount = 1;

        helper.assertTrue(crystalizer.facing == Direction.DOWN, "facing defaults to DOWN (34)");
        helper.assertTrue(crystalizer.getSuctionAmount(Direction.DOWN) == 128,
                "empty suction on the facing face is 128 (176)");
        helper.assertTrue(crystalizer.getSuctionAmount(Direction.UP) == 64,
                "every other face is 64 (176)");
        helper.assertTrue(!crystalizer.canOutputTo(Direction.DOWN),
                "the crystallizer never gives essentia back (151-153)");

        helper.runAtTickTime(1050L, () -> {
            helper.assertTrue(crystalizer.aspect == null && crystalizer.progress == 0,
                    "the machine must have ejected and reset, progress " + crystalizer.progress
                            + " stored " + crystalizer.aspect);
            helper.assertTrue(tube.essentiaAmount == 0,
                    "the unit was consumed from the line, got " + tube.essentiaAmount);
            final List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class,
                    new AABB(machinePos.getX() - 2, machinePos.getY() - 2, machinePos.getZ() - 2,
                            machinePos.getX() + 3, machinePos.getY() + 3, machinePos.getZ() + 3),
                    e -> e.getItem().getItem() instanceof IEssentiaContainerItem);
            helper.assertTrue(drops.size() == 1,
                    "exactly one crystal essence must have been spawned, got " + drops.size());
            final ItemStack crystal = drops.get(0).getItem();
            helper.assertTrue(crystal.getItem() == CatalogResources.ITEM_CRYSTAL_ESSENCE.get()
                            && crystal.getCount() == 1,
                    "the drop must be one crystal essence, got " + crystal);
            final AspectList carried = ((IEssentiaContainerItem) crystal.getItem()).getAspects(crystal);
            helper.assertTrue(carried != null && carried.getAmount(potentia()) == 1,
                    "the crystal carries the stored aspect, got " + carried);
            helper.succeed();
        });
    }

    // --- the machines' container surface + NBT, headless ---------------------

    /**
     * The tiles' container surface and NBT keys, pinned without running a
     * conversion (the {@code TileNBTProbe} trick): the one-unit output with the
     * original's quirks (the {@code am == 0} gate, the centrifuge's
     * {@code am}-ignoring take against its exact-aspect gate, the
     * crystallizer's {@code tt}-ignoring take, the exact-amount container
     * checks), the face gates, the
     * compound intake gate, and both save round-trips — the centrifuge's
     * {@code aspectIn}/{@code aspectOut}/{@code facing} ints and the
     * crystallizer's {@code Aspect}/{@code face} keys.
     * (TileCentrifuge.java:30-47, 60-107, 148-179; TileEssentiaCrystalizer.java:53-64, 82-107, 195-197.)
     */
    @GameTest(template = "empty")
    public static void machineSurfaceAndNbtHeadless(final GameTestHelper helper) {
        final BlockPos pos = helper.absolutePos(new BlockPos(0, 0, 0));
        final TileCentrifuge centrifuge = CatalogEssentiaTransport.createCentrifuge(
                pos, CatalogEssentiaTransport.state(VARIANT_CENTRIFUGE));
        // The face pair (115-127): the vertical pair connects, DOWN takes, UP gives.
        helper.assertTrue(centrifuge.isConnectable(Direction.DOWN) && centrifuge.isConnectable(Direction.UP),
                "the vertical pair connects (115)");
        helper.assertTrue(!centrifuge.isConnectable(Direction.NORTH), "no side faces (115)");
        helper.assertTrue(centrifuge.canInputFrom(Direction.DOWN) && !centrifuge.canInputFrom(Direction.UP),
                "input face DOWN (120)");
        helper.assertTrue(centrifuge.canOutputTo(Direction.UP) && !centrifuge.canOutputTo(Direction.DOWN),
                "output face UP (125)");
        helper.assertTrue(centrifuge.getSuctionAmount(Direction.DOWN) == 128, "free input (149)");
        helper.assertTrue(centrifuge.getSuctionAmount(Direction.UP) == 0, "off face (149)");
        helper.assertTrue(centrifuge.getSuctionType(Direction.DOWN) == null, "never typed (143)");
        // The compound intake gate, through the transport seam (169-179).
        helper.assertTrue(centrifuge.addEssentia(ignis(), 1, Direction.DOWN) == 0, "primal refused (169)");
        helper.assertTrue(centrifuge.addEssentia(potentia(), 1, Direction.DOWN) == 1, "compound taken (170)");
        helper.assertTrue(potentia().equals(centrifuge.aspectIn), "input slot set");
        helper.assertTrue(centrifuge.addEssentia(potentia(), 1, Direction.DOWN) == 0, "slot busy (169)");
        helper.assertTrue(centrifuge.getSuctionAmount(Direction.DOWN) == 64, "busy suction 64 (149)");
        // The one-unit output with the original's quirks (60-107).
        helper.assertTrue(centrifuge.takeEssentia(potentia(), 1, Direction.UP) == 0,
                "output slot empty (164)");
        helper.assertTrue(centrifuge.addToContainer(ignis(), 0) == 0, "am == 0 gate (61)");
        helper.assertTrue(centrifuge.addToContainer(ignis(), 2) == 1, "one unit, leftover 1 (64)");
        helper.assertTrue(ignis().equals(centrifuge.aspectOut), "output slot set");
        helper.assertTrue(centrifuge.addToContainer(ordo(), 2) == 2, "full: all left over (66)");
        helper.assertTrue(centrifuge.takeEssentia(ordo(), 1, Direction.DOWN) == 0, "DOWN never outputs (164)");
        helper.assertTrue(centrifuge.takeEssentia(ordo(), 1, Direction.UP) == 0,
                "the exact-aspect gate: the slot holds ignis, so an ordo take is refused (164-166 -> 72-81)");
        helper.assertTrue(ignis().equals(centrifuge.aspectOut), "the refused take left the slot alone");
        helper.assertTrue(centrifuge.takeEssentia(ignis(), 1, Direction.UP) == 1,
                "UP with the exact aspect hands the unit out (164-166)");
        helper.assertTrue(centrifuge.aspectOut == null, "the take emptied it (77-81)");
        centrifuge.addToContainer(ignis(), 1);
        helper.assertTrue(!centrifuge.takeFromContainer(ordo(), 1),
                "the centrifuge DOES compare tt with the stored aspect (73) - unlike the crystallizer");
        helper.assertTrue(centrifuge.takeFromContainer(ignis(), 5),
                "and the centrifuge IGNORES am (73-77): amount 5 still hands out the one unit");
        helper.assertTrue(centrifuge.aspectOut == null, "the take emptied it (74-77)");
        centrifuge.addToContainer(ignis(), 1);
        helper.assertTrue(centrifuge.doesContainerContainAmount(ignis(), 1), "exact (86)");
        helper.assertTrue(!centrifuge.doesContainerContainAmount(ordo(), 1), "other aspect (86)");
        helper.assertTrue(!centrifuge.doesContainerContainAmount(ignis(), 2), "amt == 1 gate (86)");
        final AspectList list = new AspectList();
        list.add(ignis(), 1);
        helper.assertTrue(centrifuge.doesContainerContain(list), "exact list (91)");
        final AspectList heavy = new AspectList();
        heavy.add(ignis(), 2);
        // The centrifuge's list check (90-97) is a pure existence test: it
        // walks the ARGUMENT's aspects and returns true on the first one
        // identical to aspectOut, and never reads the amounts. The
        // ot.getAmount(tt) != 1 gate is the crystallizer's (TileEssentiaCrystalizer.java:119-125),
        // pinned below.
        helper.assertTrue(centrifuge.doesContainerContain(heavy),
                "a 2-unit ignis list still matches the 1-unit slot: amounts are not read (90-97)");
        final AspectList other = new AspectList();
        other.add(ordo(), 1);
        helper.assertTrue(!centrifuge.doesContainerContain(other), "a list of other aspects (90-97)");
        helper.assertTrue(centrifuge.containerContains(ignis()) == 1, "contains (105)");
        helper.assertTrue(centrifuge.getAspects().size() == 1, "getAspects (49)");
        // The NBT round-trip on the original keys (30-47).
        centrifuge.aspectIn = potentia();
        centrifuge.facing = Direction.UP;
        final CompoundTag tag = new CompoundTag();
        centrifuge.writeCustomNBT(tag);
        helper.assertTrue(tag.contains("aspectIn"), "the aspectIn key (38)");
        helper.assertTrue(tag.contains("aspectOut"), "the aspectOut key (42)");
        helper.assertTrue(tag.getInt("facing") == Direction.UP.ordinal(), "the facing int (45)");
        final TileCentrifuge copy = CatalogEssentiaTransport.createCentrifuge(
                pos, CatalogEssentiaTransport.state(VARIANT_CENTRIFUGE));
        copy.readCustomNBT(tag);
        helper.assertTrue(potentia().equals(copy.aspectIn), "aspectIn round-trip");
        helper.assertTrue(ignis().equals(copy.aspectOut), "aspectOut round-trip");
        helper.assertTrue(copy.facing == Direction.UP, "facing round-trip");
        final TileCentrifuge fresh = CatalogEssentiaTransport.createCentrifuge(
                pos, CatalogEssentiaTransport.state(VARIANT_CENTRIFUGE));
        fresh.readCustomNBT(new CompoundTag());
        helper.assertTrue(fresh.aspectIn == null && fresh.aspectOut == null, "missing keys -> idle (31)");
        // A tag with no facing key reads the int default 0. The original stored
        // ForgeDirection ordinals, where 0 is UNKNOWN; the port stores 1.21.1
        // Direction ordinals, so the same read lands on DOWN. A machine placed
        // through the block has its facing set on add, so this only shows on a
        // bare read.
        helper.assertTrue(fresh.facing == Direction.DOWN, "a missing facing key reads ordinal 0 (39)");
        helper.assertTrue(CatalogEssentiaTransport
                .createCentrifuge(pos, CatalogEssentiaTransport.state(VARIANT_CENTRIFUGE)).facing
                == Direction.NORTH, "a created machine keeps the field's default facing (33)");

        // --- the crystallizer ------------------------------------------------
        final TileEssentiaCrystalizer crystalizer = CatalogEssentiaTransport.createCrystalizer(
                pos, CatalogEssentiaTransport.state(VARIANT_CRYSTALIZER));
        helper.assertTrue(crystalizer.isConnectable(Direction.DOWN) && !crystalizer.isConnectable(Direction.UP),
                "the facing face only (148-150)");
        helper.assertTrue(!crystalizer.canOutputTo(Direction.DOWN), "never outputs (151)");
        helper.assertTrue(crystalizer.getSuctionAmount(Direction.DOWN) == 128, "empty facing (176)");
        helper.assertTrue(crystalizer.getSuctionAmount(Direction.NORTH) == 64, "off face 64 (176)");
        helper.assertTrue(crystalizer.addEssentia(potentia(), 1, Direction.NORTH) == 0, "face gate (196)");
        helper.assertTrue(crystalizer.addEssentia(potentia(), 1, Direction.DOWN) == 1, "accepted (196)");
        helper.assertTrue(potentia().equals(crystalizer.aspect), "reservoir set");
        helper.assertTrue(crystalizer.addEssentia(potentia(), 1, Direction.DOWN) == 0, "full: accepted 0 (196)");
        helper.assertTrue(crystalizer.takeEssentia(potentia(), 1, Direction.DOWN) == 0, "take is 0 (190)");
        // The crystallizer's list check DOES read the amounts (119-125): every
        // aspect of the argument must be the stored one AND carry exactly 1.
        crystalizer.aspect = potentia();
        final AspectList one = new AspectList();
        one.add(potentia(), 1);
        helper.assertTrue(crystalizer.doesContainerContain(one), "the stored aspect, amount 1 (119-125)");
        final AspectList two = new AspectList();
        two.add(potentia(), 2);
        helper.assertTrue(!crystalizer.doesContainerContain(two), "the ot.getAmount(tt) != 1 gate (121)");
        crystalizer.aspect = null;
        helper.assertTrue(!crystalizer.doesContainerContain(one), "an empty container matches nothing (120)");
        crystalizer.aspect = potentia();
        helper.assertTrue(crystalizer.takeFromContainer(ignis(), 1), "the original ignores tt (98-107)");
        helper.assertTrue(crystalizer.aspect == null, "emptied");
        helper.assertTrue(crystalizer.doesContainerContainAmount(potentia(), 1) == false, "empty (115)");
        crystalizer.aspect = potentia();
        crystalizer.facing = Direction.NORTH;
        final CompoundTag ctag = new CompoundTag();
        crystalizer.writeCustomNBT(ctag);
        helper.assertTrue(ctag.contains("Aspect"), "the Aspect key (59)");
        helper.assertTrue(ctag.getByte("face") == Direction.NORTH.ordinal(), "the face byte (61)");
        final TileEssentiaCrystalizer ccopy = CatalogEssentiaTransport.createCrystalizer(
                pos, CatalogEssentiaTransport.state(VARIANT_CRYSTALIZER));
        ccopy.readCustomNBT(ctag);
        helper.assertTrue(potentia().equals(ccopy.aspect), "aspect round-trip");
        helper.assertTrue(ccopy.facing == Direction.NORTH, "face round-trip (54)");
        final TileEssentiaCrystalizer cfresh = CatalogEssentiaTransport.createCrystalizer(
                pos, CatalogEssentiaTransport.state(VARIANT_CRYSTALIZER));
        cfresh.readCustomNBT(new CompoundTag());
        helper.assertTrue(cfresh.aspect == null, "missing key -> empty (53)");
        helper.assertTrue(cfresh.facing == Direction.DOWN, "default facing DOWN (54)");
        helper.succeed();
    }
}
