package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
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
