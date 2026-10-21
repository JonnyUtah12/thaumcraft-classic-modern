package com.jonnyutah12.thaumcraftclassic.world;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import java.util.function.Function;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-02 acceptance: the ambient-vis read-side contract over the M2-02
 * aura fields. Reading never invents values: no record, zero density, or an
 * unknown aspect tag all resolve leniently.
 */
class AmbientVisTest {

    /**
     * Memoized definition-backed resolver — the registry form needs a running
     * game. AspectList keys aspects by instance (in-game they are registry
     * singletons), so one instance per tag is required here too.
     */
    private static final java.util.Map<String, Aspect> RESOLVED = new java.util.HashMap<>();
    private static final Function<String, Aspect> RESOLVER = tag -> RESOLVED.computeIfAbsent(tag, t ->
            AspectDefinitions.all().stream()
                    .filter(def -> def.tag().equals(t))
                    .findFirst()
                    .map(Aspect::new)
                    .orElse(null));

    private static Aspect aspect(final String tag) {
        return RESOLVER.apply(tag);
    }

    @Test
    void chunkWithoutAuraRecordHasNoAmbientVis() {
        final WorldMagicData data = new WorldMagicData();
        final AmbientVis vis = AmbientVis.of(data, new ChunkPos(3, 4), RESOLVER);
        assertEquals(AmbientVis.EMPTY, vis);
        assertTrue(vis.isEmpty());
        assertEquals(0, vis.getVis(aspect("aer")));
    }

    @Test
    void zeroOrNegativeDensityMeansNoVisEvenWithStoredAspects() {
        final WorldMagicData data = new WorldMagicData();
        final ChunkPos pos = new ChunkPos(3, 4);
        data.setAura(pos, 0, 50);
        data.setAuraAspect(pos, "aer", 3);
        assertTrue(AmbientVis.of(data, pos, RESOLVER).isEmpty());

        data.setAura(pos, -5, 50);
        assertTrue(AmbientVis.of(data, pos, RESOLVER).isEmpty());
    }

    @Test
    void storedAuraAspectsAreTheAmbientVisVerbatim() {
        final WorldMagicData data = new WorldMagicData();
        final ChunkPos pos = new ChunkPos(7, -11);
        data.setAura(pos, 120, 40);
        data.setAuraAspect(pos, "aer", 3);
        data.setAuraAspect(pos, "ignis", 4);

        final AmbientVis vis = AmbientVis.of(data, pos, RESOLVER);
        assertEquals(120, vis.density());
        assertEquals(40, vis.purity());
        assertEquals(3, vis.getVis(aspect("aer")));
        assertEquals(4, vis.getVis(aspect("ignis")));
        assertEquals(0, vis.getVis(aspect("terra")));
        assertEquals(2, vis.aspects().size());
    }

    @Test
    void unknownAspectTagsAreSkippedLeniently() {
        final WorldMagicData data = new WorldMagicData();
        final ChunkPos pos = new ChunkPos(1, 1);
        data.setAura(pos, 100, 10);
        data.setAuraAspect(pos, "aer", 2);
        data.setAuraAspect(pos, "not_an_aspect", 9);

        final AmbientVis vis = AmbientVis.of(data, pos, RESOLVER);
        assertEquals(1, vis.aspects().size());
        assertEquals(2, vis.getVis(aspect("aer")));
    }
}
