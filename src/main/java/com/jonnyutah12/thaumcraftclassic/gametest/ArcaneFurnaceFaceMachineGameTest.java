package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceDynamicState;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnaceNozzle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * BD-18 slice 1 (t_fd5265f3): the alchemical furnace's 27-icon face machine,
 * driven from live tile data through <b>published block states</b> (D-020).
 *
 * <p>4.2.3.5 recomputed the furnace's faces inside its render hooks every frame
 * from (metadata, vertical-run level, neighbour state) —
 * {@code BlockArcaneFurnace.java:57-156} picks the face icon,
 * {@code :158-177} answers the run level from the blocks above and below,
 * {@code :61-63} sets {@code add = 3} when a metadata-10 nozzle touches a face
 * ({@code BlockUtils.java:342-379}), and the ISMR
 * ({@code BlockArcaneFurnaceRenderer.java:16-59}) draws the base plate's cube
 * with the lava override + fire pass and the nozzle's three art quads on the
 * side of the plate it finds. 1.21.1 has no such hook, so the tile publishes
 * the original's condition — {@code stack} (the run level) and
 * {@code attached} (the touching partner half) — and the blockstate names a
 * port-authored model per value.</p>
 *
 * <p>The port ships the two metadata that carry behaviour (0 = the base plate,
 * 10 = the nozzle; the shell metas 1-9 are TC-M4-06-m), so these probes drive
 * that reachable slice: which side the partner is on, the run level, and the
 * model each value names. The shipped JSON side of the contract — the sprite
 * every face names — is gated by
 * {@code BlockStateCoverageGameTest#arcanefurnaceFaceshiptheoriginalsfacemachine};
 * what is proved here is that the <em>values</em> follow the tile, that the
 * state write keeps the block entity, and that a change of value changes the
 * model the state addresses. Pixels need {@code runClient}, which this suite
 * does not run.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArcaneFurnaceFaceMachineGameTest {

    private ArcaneFurnaceFaceMachineGameTest() {
    }

    /**
     * The base plate publishes which side a nozzle is attached to, from its own
     * {@code serverTick()} — the original's {@code add} gate
     * ({@code BlockArcaneFurnace.java:61-63}) and the direction its
     * {@code getFacing} scan walks (west, east, north, default south,
     * {@code TileArcaneFurnace.getFacing}).
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theplatesstateknowswhichsideanozzleattaches(final GameTestHelper helper) {
        final BlockPos platePos = ArcaneFurnaceFaceTable.centre();
        final BlockArcaneFurnace block = CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get();
        helper.setBlock(platePos, block.defaultBlockState().setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_BASE));

        final BlockEntity be = helper.getBlockEntity(platePos);
        helper.assertTrue(be instanceof TileArcaneFurnace,
                "a base plate must place the furnace tile that publishes the face machine, got " + be
                        + " (placed " + ArcaneFurnaceFaceTable.stateAt(helper, platePos) + ")");
        final TileArcaneFurnace tile = (TileArcaneFurnace) be;

        tile.serverTick();
        helper.assertTrue(ArcaneFurnaceFaceTable.stateAt(helper, platePos)
                        .getValue(BlockArcaneFurnace.ATTACHED) == ArcaneFurnaceFaceTable.ATTACHED_NONE,
                "an unattached base plate publishes attached=none (the original's add == 0 case, "
                        + "BlockArcaneFurnace.java:61-63), got "
                        + ArcaneFurnaceFaceTable.stateAt(helper, platePos));

        for (final Direction side : new Direction[]{Direction.WEST, Direction.NORTH, Direction.UP}) {
            helper.setBlock(platePos.relative(side),
                    block.defaultBlockState().setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_NOZZLE));
            tile.serverTick();
            final int published = ArcaneFurnaceFaceTable.stateAt(helper, platePos)
                    .getValue(BlockArcaneFurnace.ATTACHED);
            helper.assertTrue(published == side.ordinal(),
                    "a nozzle on the " + side.getName() + " side of the plate must publish attached="
                            + side.ordinal() + " so the blockstate can name the model for that side, got "
                            + published + " (state " + ArcaneFurnaceFaceTable.stateAt(helper, platePos) + ")");
            helper.assertTrue(helper.getBlockEntity(platePos) == tile,
                    "publishing the face machine replaced the block state — the plate's block entity (its"
                            + " smelting inventory) must survive the write");
            helper.setBlock(platePos.relative(side), Blocks.AIR.defaultBlockState());
        }

        tile.serverTick();
        helper.assertTrue(ArcaneFurnaceFaceTable.stateAt(helper, platePos)
                        .getValue(BlockArcaneFurnace.ATTACHED) == ArcaneFurnaceFaceTable.ATTACHED_NONE,
                "losing every nozzle returns the plate to attached=none — the published value must follow"
                        + " the neighbours, not latch, got "
                        + ArcaneFurnaceFaceTable.stateAt(helper, platePos));
        helper.succeed();
    }

    /**
     * The nozzle aims its art at the furnace it feeds from: the ISMR's 4-way
     * branch (west, east, north, else the +Z/south fallback) picks the side the
     * three quads land on ({@code BlockArcaneFurnaceRenderer.java:26-56}), and
     * the model the published state names carries them at the original's
     * depths — 0.625 (W10), 0.8 and 0.9 blocks into the plate's cell, with the
     * flame quad 24/16 tall (the block is re-bounded to 1.5 at {@code :33}).
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void thenozzleaimsitartattheplateitfeedson(final GameTestHelper helper) {
        final BlockPos platePos = ArcaneFurnaceFaceTable.centre();
        final BlockArcaneFurnace block = CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get();
        helper.setBlock(platePos, block.defaultBlockState().setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_BASE));

        for (final Direction plateSide : new Direction[]{Direction.EAST, Direction.NORTH, Direction.UP}) {
            final BlockPos nozzlePos = platePos.relative(plateSide);
            helper.setBlock(nozzlePos, block.defaultBlockState().setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_NOZZLE));
            final BlockEntity be = helper.getBlockEntity(nozzlePos);
            helper.assertTrue(be instanceof TileArcaneFurnaceNozzle,
                    "a nozzle variant must place the nozzle tile, got " + be);
            final TileArcaneFurnaceNozzle nozzle = (TileArcaneFurnaceNozzle) be;
            nozzle.serverTick();

            final BlockState state = ArcaneFurnaceFaceTable.stateAt(helper, nozzlePos);
            final int expectedAttached = plateSide == Direction.UP
                    ? ArcaneFurnaceFaceTable.ATTACHED_NONE
                    : plateSide.getOpposite().ordinal();
            helper.assertTrue(state.getValue(BlockArcaneFurnace.ATTACHED) == expectedAttached,
                    "a nozzle with its plate on the " + plateSide.getName() + " side must publish attached="
                            + expectedAttached + " (the ISMR draws its art on the side of the plate it finds,"
                            + " BlockArcaneFurnaceRenderer.java:26-56), got " + state);

            final String expectedSide = ArcaneFurnaceFaceTable.artSideFor(expectedAttached);
            assertArtQuads(helper, block, state, expectedSide,
                    "nozzle with its plate to the " + plateSide.getName());
            helper.assertTrue(helper.getBlockEntity(nozzlePos) == nozzle,
                    "the nozzle's block entity (its FIRE essentia feed) must survive the appearance write");
            helper.setBlock(nozzlePos, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    /**
     * The vertical-run level the tile publishes: 3 stacked nozzles are a run,
     * so the middle answers the original's 9 and the top answers its 18
     * ({@code calculateLevel}, {@code BlockArcaneFurnace.java:158-177}). For
     * the metadata-10 nozzle that value is the face change the original made
     * from it — {@code level != 9 ? icon[7] : icon[6]} on its four sides
     * ({@code :104-155}) — so the run level is the dynamic property the shipped
     * blockstate keys on ({@link DeviceDynamicState}).
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theverticalrunswapsnoszzlefurnacesides(final GameTestHelper helper) {
        final BlockPos base = ArcaneFurnaceFaceTable.centre();
        final BlockArcaneFurnace block = CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get();

        for (int i = 0; i < 3; i++) {
            helper.setBlock(base.relative(Direction.UP, i),
                    block.defaultBlockState().setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_NOZZLE));
        }
        final int[] expected = {ArcaneFurnaceFaceTable.STACK_TOP, ArcaneFurnaceFaceTable.STACK_MID,
                ArcaneFurnaceFaceTable.STACK_STANDALONE};
        for (int i = 0; i < 3; i++) {
            final BlockPos pos = base.relative(Direction.UP, i);
            final BlockEntity be = helper.getBlockEntity(pos);
            helper.assertTrue(be instanceof TileArcaneFurnaceNozzle,
                    "stacked nozzle variants must place the nozzle tile, got " + be);
            ((TileArcaneFurnaceNozzle) be).serverTick();
            final BlockState state = ArcaneFurnaceFaceTable.stateAt(helper, pos);
            helper.assertTrue(state.getValue(BlockArcaneFurnace.STACK) == expected[i],
                    "the " + (i == 0 ? "bottom" : i == 1 ? "middle" : "top") + " of a 3-tall nozzle run must"
                            + " publish stack=" + expected[i] + " (BlockArcaneFurnace.java:158-177 answers 18"
                            + " for the top of a run, 9 for the mid, 0 standalone), got " + state);
            final String expectedSide = ArcaneFurnaceFaceTable.nozzleBodySide(state.getValue(BlockArcaneFurnace.STACK));
            final ArcaneFurnaceFaceTable.Model model =
                    ArcaneFurnaceFaceTable.model(ArcaneFurnaceFaceTable.modelRef(block, state));
            helper.assertTrue(model != null,
                    "the state " + state + " addresses no shipped model");
            for (final String side : List.of("north", "south", "west", "east")) {
                helper.assertTrue(expectedSide.equals(model.faces().get(side)),
                        "the nozzle run publishes stack=" + state.getValue(BlockArcaneFurnace.STACK)
                                + ", whose model must name " + expectedSide + " on the " + side
                                + " face (icon[7] / icon[6] by level, BlockArcaneFurnace.java:104-155), got "
                                + model.faces() + " from " + model.ref());
            }
        }

        // A lone nozzle is standalone: the unlit face set, and the side sprite
        // that differs from the run's — the published value changes the model.
        final BlockPos lone = base.east(4);
        helper.setBlock(lone, block.defaultBlockState().setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_NOZZLE));
        ((TileArcaneFurnaceNozzle) helper.getBlockEntity(lone)).serverTick();
        final BlockState loneState = ArcaneFurnaceFaceTable.stateAt(helper, lone);
        helper.assertTrue(loneState.getValue(BlockArcaneFurnace.STACK) == ArcaneFurnaceFaceTable.STACK_STANDALONE,
                "a lone nozzle publishes stack=0, got " + loneState);
        final ArcaneFurnaceFaceTable.Model midModel = ArcaneFurnaceFaceTable.model(
                ArcaneFurnaceFaceTable.modelRef(block, loneState.setValue(BlockArcaneFurnace.STACK,
                        ArcaneFurnaceFaceTable.STACK_MID)));
        final ArcaneFurnaceFaceTable.Model loneModel =
                ArcaneFurnaceFaceTable.model(ArcaneFurnaceFaceTable.modelRef(block, loneState));
        helper.assertTrue(midModel != null && loneModel != null && !midModel.ref().equals(loneModel.ref()),
                "the run level must address a model of its own: stack=2 -> " + (midModel == null ? "none"
                        : midModel.ref()) + ", stack=0 -> " + (loneModel == null ? "none" : loneModel.ref()));
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** The three art quads the ISMR drew, at the original's depths on {@code expectedSide}. */
    private static void assertArtQuads(final GameTestHelper helper, final BlockArcaneFurnace block,
                                       final BlockState state, final String expectedSide,
                                       final String subject) {
        final ArcaneFurnaceFaceTable.Model model =
                ArcaneFurnaceFaceTable.model(ArcaneFurnaceFaceTable.modelRef(block, state));
        helper.assertTrue(model != null, "the state " + state + " addresses no shipped model");
        final java.util.Map<String, ArcaneFurnaceFaceTable.Art> byTexture = new java.util.LinkedHashMap<>();
        for (final ArcaneFurnaceFaceTable.Art art : model.arts()) {
            byTexture.put(art.texture(), art);
        }
        helper.assertTrue(byTexture.get(ArcaneFurnaceFaceTable.FURNACE13) != null
                        && byTexture.get(ArcaneFurnaceFaceTable.FURNACE15) != null
                        && byTexture.get(ArcaneFurnaceFaceTable.SPRITE_FIRE) != null,
                subject + ": the model " + model.ref() + " must carry the nozzle's three art quads —"
                        + " furnace13 at x-W10, furnace15 at x-0.8 and the fire quad at x-0.9"
                        + " (BlockArcaneFurnaceRenderer.java:26-56) — got " + byTexture.keySet());
        for (final String texture : List.of(ArcaneFurnaceFaceTable.FURNACE13, ArcaneFurnaceFaceTable.FURNACE15,
                ArcaneFurnaceFaceTable.SPRITE_FIRE)) {
            final ArcaneFurnaceFaceTable.Art art = byTexture.get(texture);
            helper.assertTrue(expectedSide.equals(art.side()),
                    subject + ": the " + texture + " art quad must sit on the " + expectedSide
                            + " side (the ISMR's west/east/north/else-+Z branch), got " + art.side()
                            + " in " + model.ref());
        }
        // The original's offsets from the nozzle's own cell: the plate at W10 =
        // 0.625 (BlockRenderer.java:22), the nozzle face at 0.8, the flame at 0.9.
        final float[] expectedDepths = {10f, 12.8f, 14.4f};
        final float tolerance = 0.4f;
        final List<String> order = List.of(ArcaneFurnaceFaceTable.FURNACE13, ArcaneFurnaceFaceTable.FURNACE15,
                ArcaneFurnaceFaceTable.SPRITE_FIRE);
        for (int i = 0; i < 3; i++) {
            final float centre = byTexture.get(order.get(i)).centre();
            final float depth = switch (expectedSide) {
                case "west", "north" -> -centre;
                default -> centre - 16f;
            };
            helper.assertTrue(Math.abs(depth - expectedDepths[i]) <= tolerance,
                    subject + ": the " + order.get(i) + " quad sits " + expectedDepths[i] + "/16 outside the"
                            + " nozzle's own cell (x-W10, x-0.8, x-0.9 of"
                            + " BlockArcaneFurnaceRenderer.java:26-56); measured " + depth + "/16 on the "
                            + expectedSide + " side of " + model.ref());
        }
        final ArcaneFurnaceFaceTable.Art flame = byTexture.get(ArcaneFurnaceFaceTable.SPRITE_FIRE);
        helper.assertTrue(Math.abs(flame.top() - 24f) <= 0.5f,
                subject + ": the flame quad is drawn after the block is re-bounded to 1.5 tall"
                        + " (BlockArcaneFurnaceRenderer.java:33), so it spans y 0..24/16, got to="
                        + flame.top() + " in " + model.ref());
    }
}

