package com.jonnyutah12.thaumcraftclassic.world;

import com.jonnyutah12.thaumcraftclassic.world.FluxModel.FluxDelta;
import com.jonnyutah12.thaumcraftclassic.world.FluxModel.FluxSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;

/**
 * Server-side entry points for flux generation and reads (TC-M5-03; design
 * record: docs/architecture/flux.md).
 *
 * <p>This is the port's stand-in for the original's scattered flux placement
 * sites: every magic-use feature that polluted the world in Thaumcraft 4
 * (crucible spills, infusion crashes, reservoir overflows, node-converter
 * detonations, taint decay, thaumhia) calls {@link #addFlux} instead of
 * placing goo/gas blocks directly, and the future casting milestone
 * (TC-M6-02/03) calls it with {@link FluxSource#SPELL_CAST}. The amounts and
 * medium rules are pinned in {@link FluxModel} against TC-M0-06.</p>
 *
 * <p>Reads: {@link #fluxAir}/{@link #fluxGround}/{@link #levelAt} are the
 * stable hooks the visualization milestone (TC-M7-02, thaumometer/goggles)
 * and the taint milestone (TC-M5-04) consume.</p>
 */
public final class FluxEvents {

    private FluxEvents() {
    }

    /** Adapter from the game's RNG to the model's deterministic seam. */
    public static FluxModel.Rand mcRand(final RandomSource source) {
        return new FluxModel.Rand() {
            @Override
            public int nextInt(final int bound) {
                return source.nextInt(bound);
            }

            @Override
            public boolean nextBoolean() {
                return source.nextBoolean();
            }
        };
    }

    /**
     * Record one magic-use event at a block position: adds the source's flux
     * quanta to the containing chunk's air/ground scalars. No-op on an empty
     * delta. Server side only (the store is server-authoritative).
     */
    public static void addFlux(final ServerLevel level, final BlockPos at, final FluxSource source, final int units) {
        final FluxDelta delta = FluxModel.generation(source, units, mcRand(level.getRandom()));
        addFlux(level, new ChunkPos(at), delta);
    }

    /** Add an explicit delta to a chunk (diffusion writes, tests, device code). */
    public static void addFlux(final ServerLevel level, final ChunkPos pos, final FluxDelta delta) {
        if (delta.air() == 0 && delta.ground() == 0) {
            return;
        }
        final WorldMagicData data = WorldMagicData.get(level);
        data.setFlux(pos, data.getFluxAir(pos) + delta.air(), data.getFluxGround(pos) + delta.ground());
        FluxTickHandler.track(level, pos);
    }

    /** Current air flux (gas-scale quanta) of the chunk containing {@code at}. */
    public static int fluxAir(final ServerLevel level, final BlockPos at) {
        return WorldMagicData.get(level).getFluxAir(new ChunkPos(at));
    }

    /** Current ground flux (goo-scale quanta) of the chunk containing {@code at}. */
    public static int fluxGround(final ServerLevel level, final BlockPos at) {
        return WorldMagicData.get(level).getFluxGround(new ChunkPos(at));
    }

    /**
     * Representative flux level (0..8, the original's block-metadata scale) at
     * a position — the visualization hook: what goggles/thaumometer render
     * and what effect thresholds compare against.
     */
    public static int levelAt(final ServerLevel level, final BlockPos at) {
        final WorldMagicData data = WorldMagicData.get(level);
        final ChunkPos pos = new ChunkPos(at);
        return FluxModel.level(Math.max(data.getFluxAir(pos), data.getFluxGround(pos)));
    }
}
