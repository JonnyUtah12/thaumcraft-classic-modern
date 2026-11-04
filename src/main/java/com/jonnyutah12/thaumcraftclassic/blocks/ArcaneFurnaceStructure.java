package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnaceNozzle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The 3×3×3 alchemical furnace's grid, and the state its shell publishes from
 * it (TC-M4-06-m, t_5eb27d02).
 *
 * <h2>Where the original's grid position came from</h2>
 * 4.2.3.5 built the machine in one pass, from the wand:
 * {@code WandManager.createArcaneFurnace} ({@code :400-420}) →
 * {@code fitArcaneFurnace} ({@code :422-451}) → {@code replaceArcaneFurnace}
 * ({@code :454-492}).
 * <ul>
 *   <li>{@code fitArcaneFurnace}'s blueprint ({@code :427-429}) is the 3×3×3 of
 *       nether brick at the eight corners ({@code Blocks.field_150385_bj}),
 *       obsidian on the twelve edges ({@code Blocks.field_150343_Z}), a lava
 *       source at the centre of the middle layer, obsidian at the centre of the
 *       bottom layer, <b>air</b> at the centre of the top layer, and exactly one
 *       of the four middle-layer edge cells as an iron fence
 *       ({@code Blocks.field_150411_aY}, the {@code fencefound} gate at
 *       {@code :440-448});</li>
 *   <li>{@code replaceArcaneFurnace} ({@code :457-480}) rewrites every cell of
 *       that 3×3×3 as the furnace block with {@code md = step}, {@code step}
 *       counting 1..9 per layer with the x axis fastest
 *       ({@code step = xx + 3*zz + 1} over the scan), the lava cell as
 *       {@code md = 0} (the base plate, the centre of the middle layer) and the
 *       iron fence cell as {@code md = 10} (the nozzle).</li>
 * </ul>
 *
 * <p>So a cell's grid position is a pure function of the plate's cell: with the
 * plate at the centre of the middle layer, the cell at offset
 * {@code (dx, dy, dz)} carries {@code md = (dx + 1) + 3*(dz + 1) + 1 =
 * dx + 3*dz + 5}. That is the same index {@code calculateTexture} reads back
 * with {@code (meta-1)%3 + (meta-1)/3*9}
 * ({@code BlockArcaneFurnace.java:78-84}, the 3×3 layout of the 27-icon atlas:
 * metas 1-9 → 0,1,2 / 9,10,11 / 18,19,20), and it puts the plate's own column at
 * {@code md = 5} — the blueprint's obsidian under the lava, whose top and bottom
 * faces answer the atlas centre ({@code :74-76} returns {@code icon[10]} for
 * metadata 5).</p>
 *
 * <p>The port has no wand builder, so the grid is <b>published</b> the way
 * D-020 publishes every other live condition: the base plate is the anchor, and
 * this class writes the grid — with the vertical-run level
 * ({@code calculateLevel}, {@code :158-177}) and the side a metadata-10 nozzle
 * touches the cell (the {@code add} gate, {@code :61-63}) — into the shell
 * cell's state. The original's re-mark of the whole 3×3×3 for a render update
 * ({@code BlockArcaneFurnace.func_149749_a}, {@code :304-318}) is the same
 * publish: for 4.2.3.5 the faces were recomputed inside the render hook.</p>
 */
public final class ArcaneFurnaceStructure {

    private ArcaneFurnaceStructure() {
    }

    /** A cell that is not part of any plate-centred 3×3×3. */
    public static final int GRID_NONE = 0;
    /** The grid's first scan cell (the original's metadata 1). */
    public static final int GRID_FIRST = 1;
    /** The grid's last scan cell (the original's metadata 9). */
    public static final int GRID_LAST = 9;
    /** The 3×3×3 minus the plate's own cell — the cells of a built machine. */
    public static final int MACHINE_CELLS = 26;
    /** The plate's column ({@code dx = dz = 0}): the blueprint's obsidian. */
    public static final int GRID_PLATE_COLUMN = 5;

    /**
     * The plate whose machine {@code pos} is one of the 26 outer cells of, or
     * null. The plate is the centre of the 3×3×3, so it sits within one cell of
     * every shell cell on all three axes — the same Chebyshev-1 window
     * {@code replaceArcaneFurnace} writes ({@code :457-480}) and the same one
     * the original's neighbour hook sweeps ({@code :304-318}).
     */
    @Nullable
    public static BlockPos anchorOf(final BlockGetter level, final BlockPos pos) {
        for (final BlockPos candidate : window(pos)) {
            if (candidate.equals(pos)) {
                continue;
            }
            if (isAnchor(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * True when {@code plate} is a base plate with the machine around it: a
     * furnace part in the 26 outer cells. The blueprint's top-centre cell is air
     * ({@code WandManager.java:428}, the {@code {bo, air, bo}} row of the top
     * layer), so 25 parts count as a built machine and a part placed at the top
     * centre is the extra cell the original's builder never wrote; the port
     * publishes it as the plate's own column, which is what the original's
     * formula answers for that cell.
     */
    public static boolean isAnchor(final BlockGetter level, final BlockPos plate) {
        if (!BlockArcaneFurnace.isPlate(level.getBlockState(plate))) {
            return false;
        }
        int parts = 0;
        for (final BlockPos cell : window(plate)) {
            if (!cell.equals(plate) && BlockArcaneFurnace.isFurnacePart(level.getBlockState(cell))) {
                parts++;
            }
        }
        return parts >= MACHINE_CELLS - 1;
    }

    /**
     * The grid position (the original's metadata 1-9) of {@code pos} inside its
     * plate-centred machine — {@code replaceArcaneFurnace}'s {@code step}
     * ({@code :458-478}) — or 0 for a cell outside one.
     *
     * <p>The top centre of the machine ({@code dx = dz = 0}, one above the
     * plate) is the cell the blueprint leaves air, and the formula answers the
     * plate's own column ({@code md = 5}) for it — the same answer the obsidian
     * under the plate gets, and the same one {@code calculateTexture} answers
     * the atlas centre for ({@code :74-76}).</p>
     */
    public static int gridFor(final BlockGetter level, final BlockPos pos) {
        final BlockPos plate = anchorOf(level, pos);
        if (plate == null) {
            return GRID_NONE;
        }
        final int dx = pos.getX() - plate.getX();
        final int dz = pos.getZ() - plate.getZ();
        if (dx < -1 || dx > 1 || dz < -1 || dz > 1) {
            return GRID_NONE;
        }
        final int grid = dx + 3 * dz + 5;
        return grid < GRID_FIRST || grid > GRID_LAST ? GRID_NONE : grid;
    }

    /**
     * Publish one shell cell: its grid position, the vertical-run level
     * ({@code calculateLevel}, {@code :158-177}) and the side a metadata-10
     * nozzle touches it (the {@code add} gate, {@code :61-63}).
     *
     * <p>With no plate anchor the cell keeps the grid position it was placed
     * with — the original had no such case, because its builder wrote the grid in
     * the same pass that placed the plate. The write keeps the block, so the tile
     * survives it ({@code UPDATE_CLIENTS} only, D-020's shape); a cell whose grid
     * moves between the tile-carrying metas and the corner metas changes tile
     * with it, because the metadata picks the tile class
     * ({@code createTileEntity}, {@code :355-362}).</p>
     */
    public static void publish(final Level level, final BlockPos pos) {
        if (level == null || level.isClientSide) {
            return;
        }
        final BlockState current = level.getBlockState(pos);
        if (!BlockArcaneFurnace.isShell(current)) {
            return;
        }
        final int grid = gridFor(level, pos);
        BlockState next = current;
        if (grid >= GRID_FIRST && grid <= GRID_LAST) {
            next = next.setValue(BlockArcaneFurnace.VARIANT, BlockArcaneFurnace.variantForShellGrid(grid));
        }
        next = next
                .setValue(BlockArcaneFurnace.STACK,
                        BlockArcaneFurnace.stackFor(BlockArcaneFurnace.calculateLevel(level, pos, next)))
                .setValue(BlockArcaneFurnace.ATTACHED, nozzleSide(level, pos));
        if (next.equals(current)) {
            return;
        }
        // The tile map is the original's (createTileEntity :355-362), and the
        // metadata picks the class, so a cell whose grid moves between the
        // nozzle metas (2,4,5,6,8) and the corner metas (1,3,7,9, which carry no
        // tile at all) changes tile with it. The corner metas are the original's
        // nether brick: no tile, no inventory, nothing of the player's to drop.
        final BlockEntity tile = level.getBlockEntity(pos);
        if (tile != null && !tileMatches(next, tile)) {
            level.removeBlockEntity(pos);
        }
        level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) == null && BlockArcaneFurnace.isNozzleTileMeta(next)
                && next.getBlock() instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
            final BlockEntity created = entityBlock.newBlockEntity(pos, next);
            if (created != null) {
                level.setBlockEntity(created);
            }
        }
    }

    /**
     * Publish the 26 outer cells of the machine centred on {@code plate} — the
     * original's re-mark of the 3×3×3 from the plate's neighbour hook
     * ({@code :304-318}).
     */
    public static void publishMachine(final Level level, final BlockPos plate) {
        if (level == null || level.isClientSide) {
            return;
        }
        for (final BlockPos cell : window(plate)) {
            if (!cell.equals(plate)) {
                publish(level, cell);
            }
        }
    }

    /**
     * The side of {@code pos} a metadata-10 nozzle touches — the gate that sets
     * {@code add = 3} ({@code BlockArcaneFurnace.java:61-63}: the block on that
     * face is this block with metadata 10, {@code BlockUtils.java:342-379}). The
     * base plate answers the same gate through
     * {@code TileArcaneFurnace.attachedNozzleSide}; nothing touching is the
     * original's {@code add == 0}.
     */
    public static int nozzleSide(final BlockGetter level, final BlockPos pos) {
        for (final Direction side : Direction.values()) {
            if (BlockArcaneFurnace.isNozzle(level.getBlockState(pos.relative(side)))) {
                return side.ordinal();
            }
        }
        return BlockArcaneFurnace.ATTACHED_NONE;
    }

    /** The tile class the original's {@code createTileEntity} names for {@code state}. */
    public static boolean tileMatches(final BlockState state, @Nullable final BlockEntity tile) {
        final BlockArcaneFurnace.TileKind want = BlockArcaneFurnace.tileFor(state);
        final BlockArcaneFurnace.TileKind have = tile == null ? BlockArcaneFurnace.TileKind.NONE
                : tile instanceof TileArcaneFurnaceNozzle ? BlockArcaneFurnace.TileKind.NOZZLE
                : BlockArcaneFurnace.TileKind.PLATE;
        return want == have;
    }

    /** The 27 cells of the 3×3×3 centred on {@code centre}, in the builder's scan order. */
    public static List<BlockPos> window(final BlockPos centre) {
        final List<BlockPos> cells = new ArrayList<>(27);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    cells.add(centre.offset(dx, dy, dz));
                }
            }
        }
        return cells;
    }
}
