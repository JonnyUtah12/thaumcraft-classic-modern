package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTaint;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import com.jonnyutah12.thaumcraftclassic.taint.TaintRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Ethereal Bloom tile (content-mapping.csv row {@code thaumcraft:tileetherealbloom},
 * original {@code thaumcraft.common.tiles.TileEtherealBloom}; TC-M5-04-s-b,
 * the cleansing half of the taint slice per D-012; design record
 * docs/architecture/taint.md).
 *
 * <p>The original's {@code update()} transcribed 1:1 (its
 * {@code canUpdate()} returns true, which in 1.21.1 is simply "the ticker
 * exists"):</p>
 * <ol>
 *   <li>the counter is seeded once, from {@code world.rand.nextInt(100)}, when
 *       it reads 0 — so every bloom gets its own phase, and the first
 *       cleansing roll lands 20−(seed mod 20) ticks after that;</li>
 *   <li>the server rolls one column per {@code counter % 20 == 0}: a
 *       {@code nextInt(8) - nextInt(8)} offset on each horizontal axis
 *       (-7..7), the column's <b>stored</b> biome must be one of the three
 *       thaumic biomes the original lists (taint, eerie, magical forest), and
 *       the column must be inside the radius gate;</li>
 *   <li>the column's biome is then replaced by the biome the world GENERATES
 *       there (the original asks the chunk manager for the biome-to-generate,
 *       and substitutes plains when even that answer is the taint biome) —
 *       i.e. the restore-to-original-biome path, in the port
 *       {@link TaintManager#restoreBiome};</li>
 *   <li>the client plays {@code thaumcraft:roots} at volume 1.0 / pitch 0.6
 *       exactly once, on the tile's first tick after load.</li>
 * </ol>
 *
 * <p>Distance, verbatim from the original's call shape: it calls the
 * {@code double} overload
 * {@code getDistance(x + tx + 0.5, y, z + tz + 0.5)}, and
 * {@code TileEntity#getDistance} measures from the tile's own centre
 * {@code (x + 0.5, y + 0.5, z + 0.5)} and returns the SQUARED distance. The
 * deltas are therefore {@code (-tx, 0.5, -tz)} and the gate
 * {@code tx² + tz² + 0.25 <= 81.0}: a disc of radius ~9 trimmed by the ±7
 * scan box, not a radius of 81. That arithmetic is the pure rule
 * {@link TaintRules#bloomDistanceSquared}.</p>
 *
 * <p>NBT: the original persists neither counter (no {@code writeToNBT}
 * override), so both are transient here too — an unloading bloom restarts its
 * phase, which is what the shipped 1.7.10 build did.</p>
 */
public class TileEtherealBloom extends BlockEntity {

    /** Original public field {@code counter}; seeded from the world random. */
    private int counter = 0;

    /**
     * Original public field {@code growthCounter}; drives the one-shot client
     * roots cue. The original's growth pass for the bloom's 3×3×3 leaf shell
     * is not in this tile (it lives in the block's growth path), so here the
     * counter carries its documented client-side meaning only.
     */
    private int growthCounter = 0;

    public TileEtherealBloom(final BlockPos pos, final BlockState state) {
        super(CatalogTaint.BLOOM.get(), pos, state);
    }

    public int getCounter() {
        return counter;
    }

    public int getGrowthCounter() {
        return growthCounter;
    }

    /**
     * The original's {@code update()} (TileEtherealBloom.java:14-46). Runs on
     * both sides, as the original's did: the cleansing branch is server-only
     * (the original guards it with {@code !world.isRemote}), the sound branch
     * is client-only ({@code world.isRemote}).
     */
    public void tick() {
        final Level level = getLevel();
        if (level == null) {
            return;
        }
        final BlockPos pos = getBlockPos();
        if (counter == 0) {
            counter = level.getRandom().nextInt(TaintRules.BLOOM_PHASE_MOD);
        }
        counter++;
        if (!level.isClientSide() && counter % TaintRules.BLOOM_PERIOD == 0
                && level instanceof ServerLevel serverLevel) {
            TaintManager.etherealBloomCleanse(serverLevel, pos, level.getRandom());
        }
        if (level.isClientSide() && growthCounter == 0) {
            // The original's client branch, extracted (1.21.1 has no
            // client-only class for a tile, and this is the proven
            // ClientInterception seam idiom in this repo). The original calls
            // world.playSound(x+0.5, y+0.5, z+0.5, "thaumcraft:roots", 1.0F,
            // 0.6F, false), whose 1.7.10 overload plays at NEUTRAL category
            // with no distance delay.
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    com.jonnyutah12.thaumcraftclassic.sound.ModSounds.ROOTS.get(),
                    net.minecraft.sounds.SoundSource.NEUTRAL,
                    TaintRules.BLOOM_ROOTS_VOLUME, TaintRules.BLOOM_ROOTS_PITCH, false);
        }
        growthCounter++;
    }
}
