package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.blocks.ArcaneFurnaceStructure;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceDynamicState;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnaceNozzle;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
 * <p>The port ships all eleven metadata (0 = the base plate, 10 = the nozzle,
 * {@code 2..10} = the shell's nine grid positions, TC-M4-06-m / t_5eb27d02), so
 * these probes drive the whole machine: which side the partner is on, the run
 * level, the model each value names, and — for the shell — the grid position the
 * builder used to write, the faces the grid answers, and the tile the metadata
 * carries. The shipped JSON side of the contract — the sprite
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

    /**
     * TC-M4-06-m (t_5eb27d02): build the machine and read the shell back. The
     * original's grid position is not a placement-time fact for the port —
     * 4.2.3.5 numbered the cells inside {@code WandManager.replaceArcaneFurnace}
     * ({@code :457-480}, {@code step = xx + 3*zz + 1} per layer, x fastest) — so
     * the base plate's cell is the anchor and {@link ArcaneFurnaceStructure}
     * publishes the grid, the vertical-run level and the nozzle-touch side into
     * every shell cell. Each cell then names the model the cited face rule
     * answers ({@code ArcaneFurnaceFaceTable.shellFace}) and carries the tile
     * {@code createTileEntity} ({@code :355-362}) names for its metadata: the
     * nozzle tile for 2, 4, 5, 6, 8 and nothing at all for the corner metas
     * 1, 3, 7, 9 (the blueprint's nether brick).
     */
    @GameTest(template = "empty_24", timeoutTicks = 900)
    public static void thebuiltmachineshellpublishesitsgridfacesandtile(final GameTestHelper helper) {
        final BlockArcaneFurnace block = CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get();
        final BlockPos plate = ArcaneFurnaceFaceTable.centre().above();
        final BlockPos nozzle = plate.west();
        helper.setBlock(plate, block.defaultBlockState()
                .setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_BASE));
        helper.setBlock(nozzle, block.defaultBlockState()
                .setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_NOZZLE));
        for (final BlockPos cell : ArcaneFurnaceStructure.window(plate)) {
            if (cell.equals(plate) || cell.equals(nozzle)) {
                continue;
            }
            helper.setBlock(cell, block.defaultBlockState()
                    .setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_SHELL_FIRST));
        }
        ArcaneFurnaceStructure.publishMachine(helper.getLevel(), helper.absolutePos(plate));

        final Set<Integer> grids = new TreeSet<>();
        final Set<String> models = new TreeSet<>();
        int shells = 0;
        for (final BlockPos cell : ArcaneFurnaceStructure.window(plate)) {
            if (cell.equals(plate) || cell.equals(nozzle)) {
                continue;
            }
            final BlockState state = ArcaneFurnaceFaceTable.stateAt(helper, cell);
            helper.assertTrue(state.getLightEmission() == 0,
                    "a shell cell is dark — getLightValue (:179-182) answers super for the shell metadata, so the"
                            + " glow stays on the lava plate and the nozzle; got light " + state.getLightEmission()
                            + " in " + state);
            final int grid = ArcaneFurnaceStructure.gridFor(helper.getLevel(), helper.absolutePos(cell));
            helper.assertTrue(grid >= ArcaneFurnaceStructure.GRID_FIRST
                            && grid <= ArcaneFurnaceStructure.GRID_LAST,
                    "the cell at offset " + offset(cell, plate) + " of the plate's 3x3x3 publishes grid " + grid
                            + " — replaceArcaneFurnace (:457-480) numbers the machine's cells 1..9 per layer, x"
                            + " fastest, the cell at dx/dz offset publishing dx + 3*dz + 5 — got " + state);
            helper.assertTrue(state.getValue(BlockArcaneFurnace.VARIANT) == grid + 1,
                    "the shell cell at offset " + offset(cell, plate) + " publishes variant=" + (grid + 1)
                            + " (variant = metadata + 1, TC-M4-06-m), got " + state);
            final int stack = state.getValue(BlockArcaneFurnace.STACK);
            final int attached = state.getValue(BlockArcaneFurnace.ATTACHED);
            helper.assertTrue(attached == nozzleSideFrom(cell, nozzle),
                    "the cell at offset " + offset(cell, plate) + " publishes attached=" + nozzleSideFrom(cell, nozzle)
                            + " — the side a metadata-10 nozzle touches it, the add gate (:61-63) — got " + state);
            final String ref = ArcaneFurnaceFaceTable.modelRef(block, state);
            final ArcaneFurnaceFaceTable.Model model = ArcaneFurnaceFaceTable.model(ref);
            helper.assertTrue(model != null, "the state " + state + " names a committed model, got " + ref);
            if (model != null) {
                models.add(ref);
                for (final String side : ArcaneFurnaceFaceTable.SHELL_SIDES) {
                    helper.assertTrue(
                            ArcaneFurnaceFaceTable.shellFace(grid, stack, attached, side)
                                    .equals(model.faces().get(side)),
                            "the shell cell at grid " + grid + " (stack " + stack + ", attached " + attached
                                    + ") must draw its " + side + " face as the cited face rule answers, got " + model);
                }
            }
            final BlockEntity tile = tileAt(helper, cell);
            switch (BlockArcaneFurnace.tileFor(state)) {
                case NOZZLE -> helper.assertTrue(tile instanceof TileArcaneFurnaceNozzle,
                        "metadata " + grid + " carries the nozzle tile (createTileEntity :355-362), got " + tile);
                case NONE -> helper.assertTrue(tile == null,
                        "metadata " + grid + " is a shell corner and the original gives it no tile at all"
                                + " (createTileEntity :355-362 answers null for 1, 3, 7, 9), got " + tile);
                case PLATE -> helper.fail("the shell publishes no plate tile in " + state);
            }
            grids.add(grid);
            shells++;
        }
        helper.assertTrue(shells == 25, "25 shell cells around the plate, got " + shells);
        grids.add(ArcaneFurnaceStructure.gridFor(helper.getLevel(), helper.absolutePos(nozzle)));
        helper.assertTrue(grids.size() == 9,
                "the built machine covers all nine grid positions of the 3x3 atlas, got " + grids);
        helper.assertTrue(models.size() >= 9,
                "the grid's face rules name a model per position, got " + models.size());
        helper.succeed();
    }

        /**
     * TC-M4-06-m (t_5eb27d02), the recorded delta: the shell metas 2, 4, 5, 6
     * and 8 carry the nozzle tile in the original ({@code createTileEntity},
     * {@code :355-362}), and that tile's {@code drawEssentia} pulls one FIRE
     * unit from the connectable in front of it, so a built 4.2.3.5 furnace fed
     * from up to five sides at once. The port's extraction-rate work
     * (TC-M4-06-s) pins the feed per furnace, so a shell meta's nozzle tile
     * publishes its faces and does not draw — BD-18 records that delta.
     *
     * <p>Both fixtures are the same shape: a base plate, the drawing cell
     * directly under it, and a jar of FIRE under that. The tile aims away from
     * the furnace tile it finds in its six-cell scan
     * ({@code TileArcaneFurnaceNozzle.java:16-21}), so it aims down, and a jar
     * is connectable on its top face only ({@code TileJarFillable.canOutputTo},
     * {@code :172-174}) — the one side a downward-aiming nozzle asks it for at
     * {@code drawEssentia}'s {@code back} gate ({@code :165-186}). The two
     * tiles are therefore both in a position to draw; only the shell meta's
     * branch refuses.</p>
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void onlytheregisterednozzledrawsessentiaandtheshellmetasdonot(final GameTestHelper helper) {
        final BlockArcaneFurnace block = CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get();
        final Aspect ignis = Aspect.getAspect("ignis");

        final BlockPos controlPlate = ArcaneFurnaceFaceTable.centre().above();
        final BlockPos shellPlate = controlPlate.east(6);
        helper.setBlock(controlPlate, block.defaultBlockState()
                .setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_BASE));
        helper.setBlock(shellPlate, block.defaultBlockState()
                .setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_BASE));
        // The registered nozzle (metadata 10) under the control plate; shell
        // metadata 2 (variant 3, a nozzle-tile meta) under the test plate.
        final BlockPos controlCell = controlPlate.below();
        final BlockPos shellCell = shellPlate.below();
        helper.setBlock(controlCell, block.defaultBlockState()
                .setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_NOZZLE));
        helper.setBlock(shellCell, block.defaultBlockState()
                .setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.VARIANT_SHELL_FIRST + 1));
        final BlockPos controlJar = fillJar(helper, controlCell.below(), ignis);
        final BlockPos shellJar = fillJar(helper, shellCell.below(), ignis);

        final BlockEntity controlTile = tileAt(helper, controlCell);
        final BlockEntity shellTile = tileAt(helper, shellCell);
        helper.assertTrue(controlTile instanceof TileArcaneFurnaceNozzle,
                "the registered nozzle places the drawing tile, got " + controlTile);
        helper.assertTrue(shellTile instanceof TileArcaneFurnaceNozzle,
                "shell metadata 2 carries the nozzle tile (createTileEntity :355-362), got " + shellTile);

        // One tick: the registered nozzle aims at the jar under it — facing is
        // the side away from the furnace tile its six-cell scan found
        // (TileArcaneFurnaceNozzle.java:16-21). The shell meta's tile takes the
        // port's shell branch and returns before that scan
        // (:86-89), so it publishes its faces and stays unaimed: that is the
        // delta this probe pins, stated as what the code does.
        serverTick(helper, controlCell);
        serverTick(helper, shellCell);
        final Direction controlAim = ((TileArcaneFurnaceNozzle) controlTile).getFacing();
        final Direction shellAim = ((TileArcaneFurnaceNozzle) shellTile).getFacing();
        helper.assertTrue(controlAim == Direction.DOWN,
                "the registered nozzle (metadata 10) aims at the jar under it — facing = the side away"
                        + " from the furnace tile its scan found (TileArcaneFurnaceNozzle.java:16-21), got "
                        + controlAim);
        helper.assertTrue(shellAim == null,
                "a shell meta's nozzle tile publishes its faces and returns before the facing scan"
                        + " (TileArcaneFurnaceNozzle.java:86-89) — it never aims at a jar, so it cannot"
                        + " feed the furnace it sits on; got " + shellAim);

        for (int tick = 0; tick < 20; tick++) {
            serverTick(helper, controlPlate);
            serverTick(helper, controlCell);
            serverTick(helper, shellPlate);
            serverTick(helper, shellCell);
        }

        helper.assertTrue(drawn(helper, controlJar) >= 1,
                "the registered nozzle (metadata 10) pulls FIRE from the connectable in front of it"
                        + " (TileArcaneFurnaceNozzle.java:16-21, drawEssentia :165-186) — the control jar lost "
                        + drawn(helper, controlJar));
        helper.assertTrue(drawn(helper, shellJar) == 0,
                "a shell meta's nozzle tile publishes its faces and does not feed — TC-M4-06-s pins the"
                        + " extraction rate per furnace (the original's five-way feed is TC-M4-06) and the jar"
                        + " under the shell cell lost " + drawn(helper, shellJar));
        helper.succeed();
    }


    /**
     * The tile at a structure-relative probe position. The helper's own
     * accessors convert structure-relative to absolute; a probe that reads
     * {@code getLevel()} directly must convert the same way, which is why the
     * grid, the tile map and the jar lookup all walk through here.
     */
    private static BlockEntity tileAt(final GameTestHelper helper, final BlockPos pos) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static void serverTick(final GameTestHelper helper, final BlockPos pos) {
        final BlockEntity tile = tileAt(helper, pos);
        if (tile instanceof BlockDeviceBase.TickingDeviceTile ticking) {
            ticking.serverTick();
        }
    }

    private static List<Integer> offset(final BlockPos cell, final BlockPos centre) {
        return List.of(cell.getX() - centre.getX(), cell.getY() - centre.getY(),
                cell.getZ() - centre.getZ());
    }

    private static int nozzleSideFrom(final BlockPos cell, final BlockPos nozzle) {
        for (final Direction side : Direction.values()) {
            if (cell.relative(side).equals(nozzle)) {
                return side.ordinal();
            }
        }
        return BlockArcaneFurnace.ATTACHED_NONE;
    }

    /**
     * The side of {@code cell} that faces away from {@code centre} — exactly
     * the nozzle tile's facing, which is {@code dir.getOpposite()} of the
     * direction it found the furnace tile in
     * ({@code TileArcaneFurnaceNozzle.java:16-21}). Null for a cell that only
     * touches on an edge or a corner: the original's six-cell scan finds
     * nothing there, so the tile has no feed side at all.
     */
    private static Direction awayFrom(final BlockPos centre, final BlockPos cell) {
        final int dx = cell.getX() - centre.getX();
        final int dy = cell.getY() - centre.getY();
        final int dz = cell.getZ() - centre.getZ();
        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) {
            return null;
        }
        if (dx == 1) {
            return Direction.EAST;
        }
        if (dx == -1) {
            return Direction.WEST;
        }
        if (dy == 1) {
            return Direction.UP;
        }
        if (dy == -1) {
            return Direction.DOWN;
        }
        return dz == 1 ? Direction.SOUTH : Direction.NORTH;
    }

    private static BlockPos fillJar(final GameTestHelper helper, final BlockPos pos, final Aspect aspect) {
        helper.setBlock(pos, CatalogEssentiaStorage.jarState(false));
        final BlockEntity tile = tileAt(helper, pos);
        if (tile instanceof TileJarFillable jar) {
            jar.addToContainer(aspect, 40);
        } else {
            helper.fail("no jar tile at " + pos + " (blockjar), got " + tile);
        }
        return pos;
    }

    private static int drawn(final GameTestHelper helper, final BlockPos pos) {
        final BlockEntity tile = tileAt(helper, pos);
        return tile instanceof TileJarFillable jar ? 40 - jar.getEssentiaAmount(Direction.UP) : 40;
    }
}
