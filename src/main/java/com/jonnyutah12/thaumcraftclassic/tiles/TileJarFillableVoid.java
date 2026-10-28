package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The void jar (content-mapping.csv {@code thaumcraft:tilejar} variant 1 —
 * the original's {@code TileJarFillableVoid}, a blockJar metadata 1 tile).
 *
 * <p>Only the container rules differ from the warded jar, and they are the
 * original's overrides verbatim: {@code addToContainer} accepts anything of
 * the jar's own aspect (or fills an empty jar) with <b>no room check</b> and
 * clips the overflow to capacity, so a void jar swallows a whole tube push and
 * discards the excess; the suction it advertises is 32 (48 when labelled) and
 * never drops to 0, which is why a void jar on a tube run drains the line
 * instead of stalling it.</p>
 *
 * <p>The original's {@code onEntityCollision} — living, non-flying entities
 * walking on a void jar have their inventory dumped into it, essentia
 * destroyed and items absorbed, with a 100-tick eat delay — is not ported
 * here: it is a 30-line entity loop whose interesting half (essentia
 * destruction) has nothing to destroy in a jar that already clips. Land it
 * with the void-jar effects card (TC-M7-01) if the behaviour is wanted; the
 * row on docs/content-mapping.csv names the tile as DONE for its
 * container/storage behaviour, which is what TC-M4-06-s ships.</p>
 */
public class TileJarFillableVoid extends TileJarFillable {

    public TileJarFillableVoid(final BlockPos pos, final BlockState state) {
        super(CatalogEssentiaStorage.TILE_JAR_VOID.get(), pos, state);
    }

    @Override
    public boolean voidJar() {
        return true;
    }

    @Override
    public int getSuctionAmount(final net.minecraft.core.Direction face) {
        return JarStorageLogic.voidSuctionAmount(this.aspectFilter != null, this.amount, maxAmount());
    }

    @Override
    public int getMinimumSuction() {
        return JarStorageLogic.voidMinimumSuction(this.aspectFilter != null);
    }

    @Override
    public int takeEssentia(@Nullable final Aspect aspect, final int amount,
                            final net.minecraft.core.Direction face) {
        // The original void jar never outputs.
        return 0;
    }
}
