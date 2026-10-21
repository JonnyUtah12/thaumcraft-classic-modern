package com.jonnyutah12.thaumcraftclassic.world;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.level.ChunkPos;

/**
 * Per-chunk ambient vis view over the aura data in {@link WorldMagicData}
 * (TC-M4-02; design record: docs/architecture/vis.md).
 *
 * <p>Parity note: Thaumcraft 4.2.3.5 has no runtime ambient-vis API — the
 * beta-era per-chunk aura store was removed, and the only surviving "aura" is
 * the generation-time biome aura value ({@code BiomeHandler.getBiomeAura},
 * docs/balance-values.md §6.1). The port's per-chunk aura values (density,
 * purity, aspects) are the M2-02 world-data fields that TC-M5-01's aura
 * generation fills; this reader is the stable read-side contract consumers
 * (recharge devices, the thaumometer, M5-02 node behavior) use, so the value
 * semantics never leak into consumer code.</p>
 *
 * <p>Mapping decision (provisional until TC-M5-01 defines generation): the
 * stored aura aspect amounts are the ambient vis amounts per aspect, verbatim;
 * density/purity are exposed as the scalars they are. A chunk with no aura
 * record or density ≤ 0 has no ambient vis — reading never invents values.</p>
 */
public record AmbientVis(int density, int purity, AspectList aspects) {

    public static final AmbientVis EMPTY = new AmbientVis(0, 0, new AspectList());

    /** Ambient vis of one aspect in this chunk. */
    public int getVis(final Aspect aspect) {
        return aspects.getAmount(aspect);
    }

    public boolean isEmpty() {
        return density <= 0 || aspects.size() == 0;
    }

    /**
     * Read this chunk's ambient vis from the level's magic store. Unknown
     * aspect tags are skipped leniently (the store is written lenient too —
     * docs/architecture/persistence.md).
     */
    public static AmbientVis of(final WorldMagicData data, final ChunkPos pos) {
        return of(data, pos, Aspect::getAspect);
    }

    /** Resolver-parameterized form: unit-testable outside a running game. */
    public static AmbientVis of(
            final WorldMagicData data, final ChunkPos pos, final Function<String, Aspect> resolver) {
        final int density = data.getAuraDensity(pos);
        if (density <= 0) {
            return EMPTY;
        }
        final AspectList aspects = new AspectList();
        for (final Map.Entry<String, Integer> entry : data.getAuraAspects(pos).entrySet()) {
            final Aspect aspect = resolver.apply(entry.getKey());
            if (aspect != null) {
                aspects.add(aspect, entry.getValue());
            }
        }
        return new AmbientVis(density, data.getAuraPurity(pos), aspects);
    }
}
