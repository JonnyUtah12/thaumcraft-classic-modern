package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTube;
import com.jonnyutah12.thaumcraftclassic.items.ItemResonator;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTube;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeBuffer;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeValve;
import com.jonnyutah12.thaumcraftclassic.tiles.TubeTransportLogic;
import com.jonnyutah12.thaumcraftclassic.wands.WandRayTracer;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/**
 * TC-M4-07-f evidence: the wand/resonator configuration of the essentia tubes,
 * driven through a <b>real click path</b> — a placed block, a mock player aimed
 * at a real sub-hit box, and {@code BlockState.useItemOn} (the call
 * {@code ServerPlayerGameMode.useItemOn} ends in) — asserting the state the
 * original's tiles end in.
 *
 * <ol>
 *   <li>{@link #aWandFaceClickTogglesTheSideFromBothEnds} — the original's
 *       {@code TileTube.onWandRightClick} sub-hit 0..5: the aimed face flips on
 *       this tube <em>and</em> the neighbouring tube across it takes the same
 *       value on the side pointing back, so a side opens and closes from both
 *       ends; and a wand on a valve flips a <em>face</em> without touching
 *       {@code allowFlow} (the original's metadata-1 branch excluded a wand in
 *       hand).</li>
 *   <li>{@link #theCentreBoxAimsAValve} — sub-hit 6 with the valve's own gate
 *       ({@code !canConnectSide(candidate)}): NORTH → SOUTH → WEST → EAST on a
 *       lone valve, and on a valve caged by tubes on five faces the walk lands on
 *       the one bare face.</li>
 *   <li>{@link #theCentreBoxAimsABaseTubeThroughItsNeighbour} — the base tube's
 *       gate ({@code canConnectSide(opposite) && isConnectable(opposite)}): the
 *       walk aims the face opposite the live connection, and once that side is
 *       closed the walk's 20-step bound finds nothing and leaves {@code facing}
 *       alone.</li>
 *   <li>{@link #aSneakUseChokesABufferFace} — the buffer's cycle 0 → 1 → 2 → 0
 *       with the suction figure the network obeys (idle 1, bellows-ignored 1,
 *       closed 0), and a plain use propagating the open flag to the neighbouring
 *       <em>buffer</em> — the original's second {@code instanceof} branch, which
 *       a buffer needs because {@code TileTubeBuffer} is not a {@code TileTube}.</li>
 *   <li>{@link #theResonatorReadsTheFaceItWasAimedAt} — the resonator's
 *       {@code RayTracer.retraceBlock} face refinement: aimed at the UP stub of a
 *       directional tube the read moves to that face (the one carrying the jar's
 *       32−1), aimed at the centre the clicked side stands.</li>
 * </ol>
 *
 * <p>The geometry is the original's, not a convenience: the eye is parked on a
 * stub's own mid-height band (the stubs span {@code 0.42..0.58} across the two
 * axes they do not point along, {@code TileTube.addTraceableCuboids}) and the
 * look vector is built from the yaw/pitch pair {@code Entity.getLookAngle()}
 * turns into the wanted direction, so each probe measures the sub-hit the
 * original's ray-tracer would have measured. Every probe asserts its own aim
 * ({@link #placeEye}) and the sub-hit its boxes answer, so a geometry drift
 * fails here rather than silently passing a toggle.</p>
 *
 * <p>Needs the {@code gameTestServer} run configuration (build.gradle).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TubeConfigurationGameTest {

    private TubeConfigurationGameTest() {
    }

    /** Local structure coordinates -> world coordinates. */
    private static BlockPos at(final GameTestHelper helper, final int x, final int y, final int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    /** The centre of a placed block, in world coordinates. */
    private static Vec3 centre(final GameTestHelper helper, final int x, final int y, final int z) {
        return Vec3.atCenterOf(at(helper, x, y, z));
    }

    private static <T extends BlockEntity> T tubeAt(final GameTestHelper helper, final int x, final int y, final int z,
                                                    final Class<T> type) {
        final BlockEntity be = helper.getLevel().getBlockEntity(at(helper, x, y, z));
        return type.isInstance(be) ? type.cast(be) : null;
    }

    /** A "wand" for the block's seam: any stack carrying the TC-M4-02 VIS_POOL. */
    private static ItemStack wand() {
        final ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        WandVis.setPool(stack, new VisPool(Map.of("aer", 100)));
        return stack;
    }

    /**
     * Park the player's <em>eye</em> at {@code eye} and aim it at {@code target}.
     * 1.21.1's look vector is
     * {@code (-sin(yaw)·cos(pitch), -sin(pitch), cos(yaw)·cos(pitch))}, so the
     * yaw/pitch pair comes straight off the wanted direction — and the probe
     * asserts the round trip, which is what makes the sub-hit below
     * reproducible.
     */
    private static Player aiming(final GameTestHelper helper, final Vec3 eye, final Vec3 target) {
        return aiming(helper, eye, target, Pose.STANDING);
    }

    /**
     * Pose the player <em>before</em> aiming: 1.21.1's eye position follows the
     * pose (crouching lowers it by the crouch eye-height delta), so a mid-band
     * aim computed standing misses the 0.42..0.58 stub band once the player
     * ducks — the sneak clicks are aimed from the crouching eye.
     */
    private static Player aiming(final GameTestHelper helper, final Vec3 eye, final Vec3 target, final Pose pose) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPose(pose);
        final Vec3 offset = player.getEyePosition(1.0F).subtract(player.position());
        player.setPos(eye.x - offset.x, eye.y - offset.y, eye.z - offset.z);
        final Vec3 dir = target.subtract(eye).normalize();
        player.setXRot((float) -Math.toDegrees(Math.asin(Mth.clamp(dir.y, -1.0D, 1.0D))));
        player.setYRot((float) Math.toDegrees(Math.atan2(-dir.x, dir.z)));
        helper.assertTrue(player.getLookAngle().distanceToSqr(dir) < 1.0E-6D,
                "the probe's aim must be exact: wanted " + dir + " got " + player.getLookAngle());
        return player;
    }

    /** The engine's own click: {@code ServerPlayerGameMode.useItemOn} ends in this call. */
    private static ItemInteractionResult click(final GameTestHelper helper, final BlockPos pos, final Direction side,
                                               final ItemStack stack, final Player player) {
        final BlockState state = helper.getLevel().getBlockState(pos);
        return state.useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false));
    }

    /** The sub-hit the aimed-at boxes answer, asserted by every probe before it acts. */
    private static int subHit(final GameTestHelper helper, final Player player, final BlockPos pos) {
        return WandRayTracer.retraceBlock(helper.getLevel(), player, pos);
    }

    /** The stub boxes run from the centre to the block edge, so this is their mid-band eye spot. */
    private static Vec3 eyeOnMidBandEast(final Vec3 blockCentre) {
        return blockCentre.add(2.0D, 0.0D, 0.0D);
    }

    // --- the face toggle, from both ends ----------------------------------

    @GameTest(template = "empty_24")
    public static void aWandFaceClickTogglesTheSideFromBothEnds(final GameTestHelper helper) {
        helper.setBlock(1, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        helper.setBlock(2, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));

        final TileTube a = tubeAt(helper, 1, 1, 1, TileTube.class);
        final TileTube b = tubeAt(helper, 2, 1, 1, TileTube.class);
        helper.assertTrue(a != null && b != null, "both tubes must have placed their tiles");

        final BlockPos posA = at(helper, 1, 1, 1);
        final BlockPos posB = at(helper, 2, 1, 1);
        final Player east = aiming(helper, eyeOnMidBandEast(centre(helper, 1, 1, 1)), centre(helper, 1, 1, 1));
        helper.assertTrue(subHit(helper, east, posA) == 5,
                "aimed along the EAST stub (0.42..0.58 across y/z) the sub-hit must be 5, got "
                        + subHit(helper, east, posA));

        helper.assertTrue(a.openSides[5] && b.openSides[4], "every face starts open");
        final ItemInteractionResult first = click(helper, posA, Direction.EAST, wand(), east);
        helper.assertTrue(first.consumesAction(), "the tile answers the wand, so the click is consumed");
        helper.assertTrue(!a.openSides[5], "the aimed face closed on this tube");
        helper.assertTrue(!b.openSides[4],
                "and the neighbour across it took the same value on its side pointing back "
                        + "(TileTube.onWandRightClick: ((TileTube) tile).openSides[dir.getOpposite().ordinal()] = openSides[subHit])");
        helper.assertTrue(!a.isConnectable(Direction.EAST) && !b.isConnectable(Direction.WEST),
                "a closed side is not connectable from either end, which is what stops the flow");

        click(helper, posA, Direction.EAST, wand(), east);
        helper.assertTrue(a.openSides[5] && b.openSides[4], "a second click reopens the same side from the same end");

        // The same side from the other end: the eye west of B, looking east at
        // B's WEST stub.
        final Player west = aiming(helper, centre(helper, 2, 1, 1).add(-1.5D, 0.0D, 0.0D), centre(helper, 2, 1, 1));
        helper.assertTrue(subHit(helper, west, posB) == 4,
                "aimed along B's WEST stub the sub-hit must be 4, got " + subHit(helper, west, posB));
        click(helper, posB, Direction.WEST, wand(), west);
        helper.assertTrue(!b.openSides[4], "the face closed on the tube that was clicked");
        helper.assertTrue(!a.openSides[5], "and on the tube across it — the side toggles from both ends");

        // A wand on a valve toggles a face; it never flips the valve.
        helper.setBlock(6, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_VALVE));
        helper.setBlock(7, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        final TileTubeValve valve = tubeAt(helper, 6, 1, 1, TileTubeValve.class);
        helper.assertTrue(valve != null, "the valve tile must be placed");
        final BlockPos posV = at(helper, 6, 1, 1);
        final Player valveAim = aiming(helper, eyeOnMidBandEast(centre(helper, 6, 1, 1)), centre(helper, 6, 1, 1));
        helper.assertTrue(subHit(helper, valveAim, posV) == 5,
                "the valve carries the same stub set, got " + subHit(helper, valveAim, posV));
        helper.assertTrue(valve.allowFlow, "the valve starts open");
        helper.assertTrue(click(helper, posV, Direction.EAST, wand(), valveAim).consumesAction(),
                "the valve tile answers the wand");
        helper.assertTrue(valve.allowFlow,
                "and the wand click never flips the valve's flow (BlockTube.onBlockActivated metadata 1 excluded a wand)");
        helper.assertTrue(!valve.openSides[5], "it toggles the aimed face instead");

        helper.succeed();
    }

    // --- the centre box: aiming -------------------------------------------

    @GameTest(template = "empty_24")
    public static void theCentreBoxAimsAValve(final GameTestHelper helper) {
        helper.setBlock(1, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_VALVE));
        final TileTubeValve valve = tubeAt(helper, 1, 1, 1, TileTubeValve.class);
        helper.assertTrue(valve != null, "the valve tile must be placed");

        final BlockPos pos = at(helper, 1, 1, 1);
        // A valve on its own has no connectable side, so it carries no stub box:
        // a ray straight down the middle lands on the centre box (sub-hit 6).
        final Player down = aiming(helper, centre(helper, 1, 1, 1).add(0.0D, 2.0D, 0.0D), centre(helper, 1, 1, 1));
        helper.assertTrue(subHit(helper, down, pos) == TubeTransportLogic.SUBHIT_CENTRE,
                "aimed down the middle of a tube with no live side the sub-hit is the centre box (6), got "
                        + subHit(helper, down, pos));

        helper.assertTrue(valve.facing == Direction.NORTH, "facing defaults to NORTH (the original's field default)");
        // TileTubeValve.onWandRightClick: while (++a < 20) if (!canConnectSide(getOrientation(a % 6))) { facing = ...; break; }
        click(helper, pos, Direction.UP, wand(), down);
        helper.assertTrue(valve.facing == Direction.SOUTH,
                "the walk steps from NORTH to the next face with no neighbour (SOUTH), got " + valve.facing);
        helper.assertTrue(!valve.isConnectable(Direction.SOUTH),
                "the valve is not connectable on the face it aims at (TileTubeValve.isConnectable)");
        click(helper, pos, Direction.UP, wand(), down);
        helper.assertTrue(valve.facing == Direction.WEST, "and on to WEST, got " + valve.facing);
        click(helper, pos, Direction.UP, wand(), down);
        helper.assertTrue(valve.facing == Direction.EAST, "and on to EAST, got " + valve.facing);

        // The valve's inverse gate in the other direction: caged by tubes on
        // DOWN/UP/NORTH/SOUTH/WEST, its only bare face is EAST, and the walk —
        // which skips every face that *has* a neighbour — lands on it.
        helper.setBlock(10, 2, 10, CatalogEssentiaTransport.state(BlockTube.VARIANT_VALVE));
        helper.setBlock(10, 1, 10, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        helper.setBlock(10, 3, 10, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        helper.setBlock(10, 2, 9, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        helper.setBlock(10, 2, 11, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        helper.setBlock(9, 2, 10, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        final TileTubeValve caged = tubeAt(helper, 10, 2, 10, TileTubeValve.class);
        helper.assertTrue(caged != null, "the caged valve must have placed its tile");
        final BlockPos posC = at(helper, 10, 2, 10);
        helper.assertTrue(caged.canConnectSide(0) && caged.canConnectSide(1) && caged.canConnectSide(2)
                        && caged.canConnectSide(3) && caged.canConnectSide(4) && !caged.canConnectSide(5),
                "the cage must be live on five faces and bare on EAST");
        // A corner-diagonal aim: the centre box (0.34375..0.65625) is wider than
        // the stub band (0.42..0.58), so along the diagonal it is entered first.
        final Player corner = aiming(helper, centre(helper, 10, 2, 10).add(2.5D, 2.5D, 2.5D),
                centre(helper, 10, 2, 10));
        helper.assertTrue(subHit(helper, corner, posC) == TubeTransportLogic.SUBHIT_CENTRE,
                "the centre box must win the diagonal near-side race, got " + subHit(helper, corner, posC));
        click(helper, posC, Direction.UP, wand(), corner);
        helper.assertTrue(caged.facing == Direction.EAST,
                "the walk skips the five live faces and aims the one bare face (EAST), got " + caged.facing);

        helper.succeed();
    }

    @GameTest(template = "empty_24")
    public static void theCentreBoxAimsABaseTubeThroughItsNeighbour(final GameTestHelper helper) {
        helper.setBlock(1, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        helper.setBlock(2, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        final TileTube a = tubeAt(helper, 1, 1, 1, TileTube.class);
        helper.assertTrue(a != null, "the tube tile must be placed");

        final BlockPos pos = at(helper, 1, 1, 1);
        // Aim west-to-east along the mid band: the centre box is entered at 0.34375
        // and the EAST stub at 0.42, so the centre wins.
        final Player centre = aiming(helper, centre(helper, 1, 1, 1).add(-1.5D, 0.0D, 0.0D), centre(helper, 1, 1, 1));
        helper.assertTrue(subHit(helper, centre, pos) == TubeTransportLogic.SUBHIT_CENTRE,
                "the centre box must answer, got " + subHit(helper, centre, pos));

        // TileTube.onWandRightClick's centre branch:
        //   canConnectSide(candidate.getOpposite()) && isConnectable(candidate.getOpposite())
        // The only live side is EAST, so the accepted candidate is the face whose
        // opposite is EAST — i.e. WEST, the face that *points away* from the
        // neighbour, which is the original's expression verbatim.
        helper.assertTrue(a.facing == Direction.NORTH, "facing defaults to NORTH");
        click(helper, pos, Direction.UP, wand(), centre);
        helper.assertTrue(a.facing == Direction.WEST,
                "the walk aims the face opposite the live connection (WEST), got " + a.facing);
        click(helper, pos, Direction.UP, wand(), centre);
        helper.assertTrue(a.facing == Direction.WEST, "the second walk lands on WEST again, got " + a.facing);

        // Close that live side and the walk's gate reads isConnectable, finds
        // nothing in its 20 steps, and leaves facing alone.
        final Player east = aiming(helper, eyeOnMidBandEast(centre(helper, 1, 1, 1)), centre(helper, 1, 1, 1));
        click(helper, pos, Direction.EAST, wand(), east);
        helper.assertTrue(!a.openSides[5], "the east side is now closed");
        helper.assertTrue(subHit(helper, east, pos) == 5,
                "the stub box stays where the live neighbour is, got " + subHit(helper, east, pos));
        click(helper, pos, Direction.UP, wand(), centre);
        helper.assertTrue(a.facing == Direction.WEST,
                "with the live side closed the walk accepts nothing and facing stays put, got " + a.facing);

        helper.succeed();
    }

    // --- the buffer's choke ------------------------------------------------

    @GameTest(template = "empty_24")
    public static void aSneakUseChokesABufferFace(final GameTestHelper helper) {
        helper.setBlock(1, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_BUFFER));
        helper.setBlock(2, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_BUFFER));
        final TileTubeBuffer a = tubeAt(helper, 1, 1, 1, TileTubeBuffer.class);
        final TileTubeBuffer b = tubeAt(helper, 2, 1, 1, TileTubeBuffer.class);
        helper.assertTrue(a != null && b != null, "both buffers must have placed their tiles");

        final BlockPos pos = at(helper, 1, 1, 1);
        // TileTubeBuffer.onWandRightClick, sneak branch: chokedSides[hit]++ with
        // the > 2 wrap, and the squeak. 1.21.1's isCrouching() reads the CROUCHING
        // pose, not the shift flag (the EssentiaTransportGameTest lesson) — and the
        // pose lowers the eye, so the sneak is aimed from the crouching eye.
        final Player east = aiming(helper, eyeOnMidBandEast(centre(helper, 1, 1, 1)),
                centre(helper, 1, 1, 1), Pose.CROUCHING);
        helper.assertTrue(east.isCrouching(), "the probe's sneak must be visible to isCrouching()");
        helper.assertTrue(subHit(helper, east, pos) == 5,
                "the buffer carries the same stub set (TileTubeBuffer.addTraceableCuboids), got "
                        + subHit(helper, east, pos));
        helper.assertTrue(a.getSuctionAmount(Direction.EAST) == TubeTransportLogic.BUFFER_IDLE_SUCTION,
                "an open face with no bellows advertises the original's idle 1, got "
                        + a.getSuctionAmount(Direction.EAST));
        click(helper, pos, Direction.EAST, wand(), east);
        helper.assertTrue(a.chokedSides[5] == TubeTransportLogic.CHOKE_RESTRICTED,
                "first sneak-use: choke 0 -> 1 (bellows ignored), got " + a.chokedSides[5]);
        helper.assertTrue(a.getSuctionAmount(Direction.EAST) == TubeTransportLogic.BUFFER_IDLE_SUCTION,
                "state 1 ignores the bellows (bufferSuction: choke != 1 gates the bellows term), got "
                        + a.getSuctionAmount(Direction.EAST));
        helper.assertTrue(a.openSides[5], "the choke cycle never touches the open flag");
        click(helper, pos, Direction.EAST, wand(), east);
        helper.assertTrue(a.chokedSides[5] == TubeTransportLogic.CHOKE_OFF,
                "second sneak-use: choke 1 -> 2 (closed), got " + a.chokedSides[5]);
        helper.assertTrue(a.getSuctionAmount(Direction.EAST) == 0,
                "a closed face advertises 0 — nothing is pulled through it, got "
                        + a.getSuctionAmount(Direction.EAST));
        click(helper, pos, Direction.EAST, wand(), east);
        helper.assertTrue(a.chokedSides[5] == 0,
                "third sneak-use wraps back to open (the original's > 2 reset), got " + a.chokedSides[5]);

        // The plain (non-sneak) branch, with the original's second instanceof:
        // a buffer hands its open flag to a neighbouring *buffer* too, which it
        // needs because TileTubeBuffer is not a TileTube.
        // Standing back up is a new player: the eye rises again, so a fresh
        // mid-band aim models the player who stood up and clicked.
        final Player plain = aiming(helper, eyeOnMidBandEast(centre(helper, 1, 1, 1)), centre(helper, 1, 1, 1));
        helper.assertTrue(!plain.isCrouching(), "standing: the eye is back at the mid-band");
        helper.assertTrue(a.openSides[5] && b.openSides[4], "both east sides are open");
        helper.assertTrue(subHit(helper, plain, pos) == 5,
                "the standing aim still lands on the EAST stub, got " + subHit(helper, plain, pos));
        click(helper, pos, Direction.EAST, wand(), plain);
        helper.assertTrue(!a.openSides[5], "the plain click closes the aimed face");
        helper.assertTrue(!b.openSides[4],
                "and the neighbouring buffer's side pointing back (the buffer's own propagation branch), got "
                        + b.openSides[4]);

        helper.succeed();
    }

    // --- the resonator reads the aimed face -------------------------------

    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void theResonatorReadsTheFaceItWasAimedAt(final GameTestHelper helper) {
        // The EssentiaTransportGameTest geometry verbatim: a jar with room (it
        // advertises 32) under a directional tube aimed UP — the suction enters
        // the face opposite facing and is advertised at facing, so the tube's tip
        // reads 32 - 1 = 31. A plain tube above makes UP a live side, so the UP
        // stub box exists for the resonator to be aimed at (the probe then shows
        // the read sitting on the aimed face — the original's RayTracer.retrace-
        // Block sub-hit refinement).
        helper.setBlock(1, 1, 1, CatalogEssentiaStorage.jarState(false));
        helper.setBlock(1, 2, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_ONEWAY));
        helper.setBlock(1, 3, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
        final TileTube tube = tubeAt(helper, 1, 2, 1, TileTube.class);
        helper.assertTrue(tube != null, "the directional tube must have placed its tile");
        tube.facing = Direction.UP;
        tube.touch();

        // A third of a second of real ticks: the suction pass runs every 2 ticks.
        helper.runAtTickTime(20L, () -> {
            final BlockPos pos = at(helper, 1, 2, 1);
            helper.assertTrue(tube.getSuctionAmount(Direction.UP) == 31,
                    "the jar's 32 arrives through the face opposite the aim and is read at the aimed face, "
                            + "minus one, got " + tube.getSuctionAmount(Direction.UP));

            final Player up = aiming(helper, centre(helper, 1, 2, 1).add(0.0D, 2.0D, 0.0D), centre(helper, 1, 2, 1));
            helper.assertTrue(subHit(helper, up, pos) == 1,
                    "aimed from above, with the jar above making UP a live side, the ray lands on the UP stub (1), got "
                            + subHit(helper, up, pos));
            helper.assertTrue(ItemResonator.readFace(helper.getLevel(), up, pos, Direction.NORTH) == Direction.UP,
                    "the resonator's face is the aimed sub-hit, not the clicked side");

            // And the item path itself runs on that aim: consumed, on a transport tile.
            final ItemStack resonator = new ItemStack(CatalogTrinketsLoot.ITEM_RESONATOR.get());
            final net.minecraft.world.InteractionResult read = resonator.getItem().onItemUseFirst(resonator,
                    new UseOnContext(helper.getLevel(), up, InteractionHand.MAIN_HAND, resonator,
                            new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false)));
            helper.assertTrue(read.consumesAction(), "the resonator consumes the click on a transport tile");

            // Aimed at the centre there is no face sub-hit ahead of it, so the
            // clicked side stands (the original's `subHit >= 0 && subHit < 6`
            // guard). The UP stub exists (the jar), but the centre box is
            // entered at 0.34375 before the stub band's 0.42 along the ray.
            final Player side = aiming(helper, eyeOnMidBandEast(centre(helper, 1, 2, 1)), centre(helper, 1, 2, 1));
            helper.assertTrue(subHit(helper, side, pos) == TubeTransportLogic.SUBHIT_CENTRE,
                    "a mid-band aim enters the centre box (0.34375..0.65625 across the axis) before the UP stub's band, got "
                            + subHit(helper, side, pos));
            helper.assertTrue(
                    ItemResonator.readFace(helper.getLevel(), side, pos, Direction.NORTH) == Direction.NORTH,
                    "a centre aim keeps the clicked face");

            // The per-face observable the refinement moves the read on: a
            // buffer's suction is choke-driven, so it differs face to face
            // (a tube's getSuctionAmount is face-blind, the original's included).
            // Buffer at (5,1,1) with a tube NORTH and UP, its NORTH face choked
            // closed: the clicked face (UP) reads the idle 1, the aimed face
            // (NORTH, choked 2) reads 0.
            helper.setBlock(5, 1, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_BUFFER));
            helper.setBlock(5, 1, 0, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
            helper.setBlock(5, 2, 1, CatalogEssentiaTransport.state(BlockTube.VARIANT_TUBE));
            final TileTubeBuffer buffer = tubeAt(helper, 5, 1, 1, TileTubeBuffer.class);
            helper.assertTrue(buffer != null, "the buffer must have placed its tile");
            buffer.chokedSides[2] = TubeTransportLogic.CHOKE_OFF;
            buffer.touch();
            helper.assertTrue(buffer.getSuctionAmount(Direction.UP) == TubeTransportLogic.BUFFER_IDLE_SUCTION
                            && buffer.getSuctionAmount(Direction.NORTH) == 0,
                    "the probe needs face-dependent reads: UP (open, no bellows) is 1, NORTH (choked closed) is 0, got "
                            + buffer.getSuctionAmount(Direction.UP) + " and "
                            + buffer.getSuctionAmount(Direction.NORTH));

            final BlockPos posB = at(helper, 5, 1, 1);
            // The eye sits 0.05 below the block's mid-line so the ray runs
            // inside the NORTH stub's 0.42..0.58 band and outside the UP stub's
            // 0.5..1.0 — a mid-line eye would tie the two stubs exactly, and the
            // ray-tracer keeps the earlier box (UP) on a tie.
            final Player north = aiming(helper,
                    centre(helper, 5, 1, 1).add(0.0D, -0.05D, -2.0D), centre(helper, 5, 1, 1));
            helper.assertTrue(subHit(helper, north, posB) == 2,
                    "aimed along the NORTH stub, the sub-hit is 2, got " + subHit(helper, north, posB));
            helper.assertTrue(
                    ItemResonator.readFace(helper.getLevel(), north, posB, Direction.UP) == Direction.NORTH,
                    "the resonator's face moves from the clicked UP to the aimed NORTH");
            helper.assertTrue(
                    buffer.getSuctionAmount(ItemResonator.readFace(helper.getLevel(), north, posB, Direction.UP))
                            != buffer.getSuctionAmount(Direction.UP),
                    "the read moves to the aimed face: that face's per-face suction (the choked 0) differs "
                            + "from the clicked face's idle 1");

            helper.succeed();
        });
    }
}
