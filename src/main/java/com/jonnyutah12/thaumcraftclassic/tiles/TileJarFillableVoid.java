package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The void essentia jar (TC-M4-06): port of {@code TileJarFillableVoid}.
 * Accepts any amount in one shot (overflow clamps instead of leaving a
 * remainder), advertises stronger suction when filtered, and keeps trying to
 * fill even when already full — the original's distinguishing quirks, kept
 * exactly.
 */
public class TileJarFillableVoid extends TileJarFillable {

    int count = 0;

    public TileJarFillableVoid(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int addToContainer(final Aspect tt, final int am) {
        final boolean up = this.amount < this.maxAmount;
        if (am == 0) {
            return am;
        }
        if (tt == this.aspect || this.amount == 0) {
            this.aspect = tt;
            this.amount += am;
            am = 0;
            if (this.amount > this.maxAmount) {
                this.amount = this.maxAmount;
            }
        }
        if (up) {
            DeviceSync.sync(this);
        }
        return am;
    }

    @Override
    public int getMinimumSuction() {
        return this.aspectFilter != null ? 48 : 32;
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        return this.aspectFilter != null && this.amount < this.maxAmount ? 48 : 32;
    }

    @Override
    public void tick() {
        final var level = getLevel();
        if (level != null && !level.isClientSide && ++this.count % 5 == 0) {
            this.fillJar();
        }
    }
}
