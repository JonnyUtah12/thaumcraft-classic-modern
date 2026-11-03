package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.blocks.ArcaneFurnaceStructure;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockArcaneFurnace;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceDynamicState;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The infernal furnace nozzle (content-mapping.csv
 * {@code thaumcraft:tilearcanefurnacenozzle}) — the piece that feeds the
 * furnace FIRE essentia out of the tube line, which is what makes it cook fast.
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcaneFurnaceNozzle}, the
 * original's blockArcaneFurnace metadata 10 (the port's {@code variant} 1).
 * Ported verbatim: it finds the furnace body in one of the six neighbours on
 * the first tick and works out which way it points; every
 * {@value ArcaneFurnaceLogic#NOZZLE_DRAW_CADENCE} ticks it pulls
 * {@value ArcaneFurnaceLogic#NOZZLE_DRAW_AMOUNT} FIRE essentia from the
 * connectable in front of it, only while the furnace's speedyTime is under
 * {@value ArcaneFurnaceLogic#NOZZLE_DRAW_LIMIT}, and a successful draw buys
 * the furnace {@value ArcaneFurnaceLogic#NOZZLE_SPEEDY_BONUS} ticks of fast
 * cooking; it advertises suction {@value ArcaneFurnaceLogic#NOZZLE_SUCTION}
 * while speedyTime is under {@value ArcaneFurnaceLogic#NOZZLE_SUCTION_LIMIT}
 * and 0 above, so a fed furnace stops pulling on the line; it never outputs,
 * never has a minimum suction, and does not render as an extended tube.</p>
 *
 * <p>Every number is in {@link ArcaneFurnaceLogic} (ArcaneFurnaceLogicTest
 * pins them). The tube walk through several cells is the original's
 * {@code ThaumcraftApiHelper.getConnectableTile}; the tube network is
 * TC-M4-07, so the port reads the direct neighbour, exactly as
 * {@link TileJarFillable#fillJar()} does.</p>
 */
public class TileArcaneFurnaceNozzle extends BlockEntity
        implements IEssentiaTransport, BlockDeviceBase.TickingDeviceTile {

    /**
     * The aspect this nozzle draws (the original's {@code Aspect.FIRE}). Resolved
     * lazily by tag, the way every ported tile does it — the aspect registry is
     * only populated after registry events.
     */
    private static Aspect fire() {
        return Aspect.getAspect("ignis");
    }

    @Nullable
    private Direction facing = null;
    @Nullable
    private TileArcaneFurnace furnace = null;
    private int drawDelay = 0;

    public TileArcaneFurnaceNozzle(final BlockPos pos, final BlockState state) {
        super(CatalogEssentiaStorage.TILE_ARCANE_FURNACE_NOZZLE.get(), pos, state);
    }

    @Nullable
    public Direction getFacing() {
        return this.facing;
    }

    @Nullable
    public TileArcaneFurnace getFurnace() {
        return this.furnace;
    }

    @Override
    public void serverTick() {
        if (this.level == null) {
            return;
        }
        // TC-M4-06-m: the original hands this tile to the shell metas 2, 4, 5, 6
        // and 8 as well (createTileEntity :355-362), so a formed 3×3×3 runs
        // several nozzle tiles. Their faces publish (they sit in the machine and
        // the face machine reads them); the FIRE-essentia draw stays on the
        // registered nozzle, because the port's TC-M4-06-s extraction rates are
        // pinned per furnace and a five-way feed is TC-M4-06 behaviour (BD-18
        // records the delta).
        final BlockState state = this.level.getBlockState(this.worldPosition);
        if (BlockArcaneFurnace.isShell(state)) {
            publishAppearance();
            return;
        }
        if (this.facing == null && this.furnace == null) {
            this.facing = null;
            for (final Direction dir : Direction.values()) {
                if (this.level.getBlockEntity(this.worldPosition.relative(dir)) instanceof TileArcaneFurnace f) {
                    this.facing = dir.getOpposite();
                    this.furnace = f;
                    break;
                }
            }
        }
        if (this.level.isClientSide) {
            return;
        }
        if (this.furnace != null && ArcaneFurnaceLogic.nozzleCanDraw(this.furnace.speedyTime) && drawEssentia()) {
            this.furnace.speedyTime = ArcaneFurnaceLogic.speedyAfterDraw(this.furnace.speedyTime);
        }
        publishAppearance();
    }

    /**
     * BD-18 slice 1 (t_fd5265f3): publish this nozzle's face-machine inputs
     * (D-020) — the run level, and the side its art lands on. Same shape as
     * {@link TileArcaneFurnace#publishAppearance()}.
     *
     * <p>On a shell meta (TC-M4-06-m, t_5eb27d02) the cell is not the nozzle:
     * its art is the grid's, the {@code add} gate asks for a metadata-10
     * neighbour, and its grid position comes from the plate anchor, so
     * {@link ArcaneFurnaceStructure#publish} answers all three.</p>
     */
    public void publishAppearance() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        final BlockState state = this.level.getBlockState(worldPosition);
        if (BlockArcaneFurnace.isShell(state)) {
            ArcaneFurnaceStructure.publish(this.level, worldPosition);
            return;
        }
        DeviceDynamicState.sync(this, BlockArcaneFurnace.STACK,
                BlockArcaneFurnace.stackFor(
                        BlockArcaneFurnace.calculateLevel(this.level, worldPosition, state)));
        DeviceDynamicState.sync(this, BlockArcaneFurnace.ATTACHED, artSide());
    }

    /**
     * The side of the base plate the ISMR draws its three art quads on: a 4-way
     * branch — plate to the west, else east, else north, <em>else the
     * unconditional +Z (south)</em> ({@code BlockArcaneFurnaceRenderer.java:26-56},
     * which never tests the fourth branch). A plate above or below matches none
     * of the three checks, so it publishes {@link BlockArcaneFurnace#ATTACHED_NONE},
     * and the shipped blockstate gives that value the same +Z (south) model as
     * {@code attached=south}: the original draws the identical art in both
     * cases.
     */
    private int artSide() {
        for (final Direction side : ART_BRANCH_ORDER) {
            if (isBaseAt(worldPosition.relative(side))) {
                return side.ordinal();
            }
        }
        return BlockArcaneFurnace.ATTACHED_NONE;
    }

    /** The ISMR's branch order: west, east, north, then the +Z fallback. */
    private static final Direction[] ART_BRANCH_ORDER = {
            Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};

    private boolean isBaseAt(final BlockPos pos) {
        final BlockState state = this.level.getBlockState(pos);
        return state.is(CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get())
                && state.getValue(BlockArcaneFurnace.VARIANT) == BlockArcaneFurnace.VARIANT_BASE;
    }

    /** Original {@code drawEssentia}: one FIRE unit from the connectable in front. */
    boolean drawEssentia() {
        this.drawDelay++;
        if (!ArcaneFurnaceLogic.nozzleDrawTick(this.drawDelay) || this.facing == null) {
            return false;
        }
        final BlockEntity target = this.level.getBlockEntity(this.worldPosition.relative(this.facing));
        if (!(target instanceof IEssentiaTransport transport)) {
            return false;
        }
        final Direction back = this.facing.getOpposite();
        if (!transport.canOutputTo(back)) {
            return false;
        }
        if (transport.getSuctionAmount(back) < getSuctionAmount(this.facing)
                && transport.takeEssentia(fire(), ArcaneFurnaceLogic.NOZZLE_DRAW_AMOUNT, back)
                == ArcaneFurnaceLogic.NOZZLE_DRAW_AMOUNT) {
            if (target instanceof BlockEntity targetBe) {
                targetBe.setChanged();
            }
            return true;
        }
        return false;
    }

    // --- IEssentiaTransport (original: once aimed, connectable on any face) --

    @Override
    public boolean isConnectable(final Direction face) {
        return this.facing != null;
    }

    @Override
    public boolean canInputFrom(final Direction face) {
        return this.facing != null;
    }

    @Override
    public boolean canOutputTo(final Direction face) {
        return false;
    }

    @Override
    public void setSuction(@Nullable final Aspect aspect, final int amount) {
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public Aspect getSuctionType(final Direction face) {
        return fire();
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        return this.furnace != null
                ? ArcaneFurnaceLogic.nozzleSuction(this.furnace.speedyTime) : 0;
    }

    @Override
    @Nullable
    public Aspect getEssentiaType(final Direction face) {
        return null;
    }

    @Override
    public int getEssentiaAmount(final Direction face) {
        return 0;
    }

    @Override
    public int takeEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
        return 0;
    }

    @Override
    public int addEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
        return 0;
    }
}
