package com.jonnyutah12.thaumcraftclassic.world;

import com.jonnyutah12.thaumcraftclassic.world.FluxModel.EffectSpec;
import com.jonnyutah12.thaumcraftclassic.world.FluxModel.FluxDelta;
import com.jonnyutah12.thaumcraftclassic.world.FluxModel.FluxSource;
import com.jonnyutah12.thaumcraftclassic.world.FluxModel.Rand;
import com.jonnyutah12.thaumcraftclassic.world.FluxModel.SlimeSpawn;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M5-03 acceptance: flux math — generation amounts, decay, thresholds —
 * pinned against the TC-M0-06 constants (docs/balance-values.md §8) and the
 * original's placement sites. Design record: docs/architecture/flux.md.
 */
class FluxModelTest {

    private static Rand seeded(final long seed) {
        return FluxModel.randOf(new Random(seed));
    }

    // ------------------------------------------------------------- scale/meta

    @Test
    void metaScaleMatchesOriginalQuanta() {
        // BlockFluxGoo.getQuanta() == quantaPerBlock == 8 (BlockFluxGoo.java:47-49);
        // meta 8 is the "full" placement (BlockAiry.java:347-349).
        assertEquals(8, FluxModel.COLUMN_CAP);
        assertEquals(8, FluxModel.MAX_LEVEL);
        assertEquals(1, FluxModel.quantaOfMeta(0)); // trace flux still counts
        assertEquals(7, FluxModel.quantaOfMeta(7)); // infusion-crash column
        assertEquals(8, FluxModel.quantaOfMeta(8)); // reservoir/explosion column
    }

    @Test
    void levelCapsAtMetaScale() {
        assertEquals(0, FluxModel.level(0));
        assertEquals(5, FluxModel.level(5));
        assertEquals(8, FluxModel.level(8));
        assertEquals(8, FluxModel.level(10_000));
    }

    // ------------------------------------------------------------ generation

    @Test
    void spellCastAddsOneAirQuantumPerVisUnit() {
        // Port design constant (no TC4 casting-flux counterpart — see FluxModel docs).
        assertEquals(new FluxDelta(5, 0), FluxModel.generation(FluxSource.SPELL_CAST, 5, seeded(0)));
        assertEquals(new FluxDelta(0, 0), FluxModel.generation(FluxSource.SPELL_CAST, 0, seeded(0)));
    }

    @Test
    void crucibleSpillIsOneQuantumCoinFlippedMedium() {
        // TileCrucible.java:196-204: meta-0 gas or goo, 50/50 (1/4 gate at call site).
        final FluxDelta air = FluxModel.generation(FluxSource.CRUCIBLE_SPILL, 1, seeded(1));
        final FluxDelta ground = FluxModel.generation(FluxSource.CRUCIBLE_SPILL, 1, seeded(2));
        assertEquals(1, air.air() + air.ground());
        assertEquals(1, ground.air() + ground.ground());
    }

    @Test
    void infusionFailurePlacesMetaSevenColumns() {
        // TileInfusionMatrix.java:639-642: goo meta 7 for types 1/3, gas meta 7 for 2/4.
        assertEquals(new FluxDelta(0, 7), FluxModel.generation(FluxSource.INFUSION_FAILURE_GROUND, 1, seeded(0)));
        assertEquals(new FluxDelta(7, 0), FluxModel.generation(FluxSource.INFUSION_FAILURE_AIR, 1, seeded(0)));
    }

    @Test
    void reservoirOverflowPlacesMetaEightColumns() {
        // BlockEssentiaReservoir.java:99-101.
        assertEquals(new FluxDelta(0, 8), FluxModel.generation(FluxSource.RESERVOIR_OVERFLOW_GROUND, 1, seeded(0)));
        assertEquals(new FluxDelta(8, 0), FluxModel.generation(FluxSource.RESERVOIR_OVERFLOW_AIR, 1, seeded(0)));
    }

    @Test
    void nodeConverterExplosionPlacesFiftyColumns() {
        // BlockAiry.explodify: 50 attempts, meta-8 goo below / gas above (BlockAiry.java:336-353).
        assertEquals(50, FluxModel.explosionPlacements());
        assertEquals(8, FluxSource.NODE_CONVERTER_EXPLOSION.quantaPerUnit());
    }

    @Test
    void taintDecayAndThaumhiaAmounts() {
        // BlockTaint.java:188-193 (meta = getQuanta()); PotionThaumarhia.java:44 (meta 0).
        assertEquals(new FluxDelta(0, 8), FluxModel.generation(FluxSource.TAINT_DECAY, 1, seeded(0)));
        assertEquals(new FluxDelta(0, 1), FluxModel.generation(FluxSource.THAUMARHIA, 1, seeded(0)));
    }

    // ------------------------------------------------------------------ decay

    @Test
    void decayPassIntervalDerivation() {
        // 1/30 per random tick (BlockFluxGoo.java:104-112; TC-M0-06 §8) under
        // 1.7.10 randomTickSpeed=3 over 4096 blocks/section: 30 * 4096 / 3.
        assertEquals(40_960L, FluxModel.DECAY_PASS_TICKS);
    }

    @Test
    void columnsAndDecayStep() {
        assertEquals(0, FluxModel.columns(0));
        assertEquals(1, FluxModel.columns(1));
        assertEquals(1, FluxModel.columns(8));
        assertEquals(2, FluxModel.columns(9));
        assertEquals(2, FluxModel.columns(16));

        // Each column loses one level per pass (the original decays every goo
        // block independently at the same expected rate).
        assertEquals(0, FluxModel.decayStep(0));
        assertEquals(0, FluxModel.decayStep(1));
        assertEquals(7, FluxModel.decayStep(8));
        assertEquals(7, FluxModel.decayStep(9)); // two columns: 8+1 -> 7+0
        assertEquals(14, FluxModel.decayStep(16));
        assertTrue(FluxModel.decayStep(100) < 100);
    }

    @Test
    void groundDecayVentsGasAtFiftyPercent() {
        // BlockFluxGoo.java:108-111: on decay, 50% place flux gas above.
        int emitted = 0;
        for (int seed = 0; seed < 20; seed++) {
            emitted += FluxModel.groundDecayAirEmission(100, seeded(seed));
        }
        final int trials = 20 * 100;
        final double rate = (double) emitted / trials;
        assertTrue(rate > 0.40 && rate < 0.60, "vent rate out of band: " + rate);
        assertEquals(0, FluxModel.groundDecayAirEmission(0, seeded(0)));
    }

    // -------------------------------------------------------------- diffusion

    @Test
    void diffusionOnlyAboveOneFullColumn() {
        assertEquals(0, FluxModel.diffusionOut(0));
        assertEquals(0, FluxModel.diffusionOut(8)); // exactly one column stays
        assertEquals(1, FluxModel.diffusionOut(9));
        assertEquals(1, FluxModel.diffusionOut(1000));
    }

    // ------------------------------------------------------- effect thresholds

    @Test
    void slimeSpawnThresholdsAndRate() {
        // BlockFluxGoo.java:88-99: meta 2..5 -> 1/25 size 1; meta >= 6 -> 1/25 size 2.
        for (int seed = 0; seed < 50; seed++) {
            assertEquals(SlimeSpawn.NONE, FluxModel.slimeSpawnRoll(0, seeded(seed)));
            assertEquals(SlimeSpawn.NONE, FluxModel.slimeSpawnRoll(1, seeded(seed)));
        }
        int size1 = 0;
        int size2 = 0;
        for (int seed = 0; seed < 2000; seed++) {
            final SlimeSpawn low = FluxModel.slimeSpawnRoll(2, seeded(seed));
            if (low.occurred()) {
                assertEquals(1, low.size());
                size1++;
            }
            final SlimeSpawn high = FluxModel.slimeSpawnRoll(6, seeded(seed));
            if (high.occurred()) {
                assertEquals(2, high.size());
                size2++;
            }
        }
        // 1/25 expected rate over 2000 seeded trials: ~80 hits each.
        assertTrue(size1 > 40 && size1 < 130, "size-1 spawn rate off: " + size1);
        assertTrue(size2 > 40 && size2 < 130, "size-2 spawn rate off: " + size2);
    }

    @Test
    void taintConversionRequiresLevelConfigAndRate() {
        // BlockFluxGoo.java:100-103 + Config.taintFromFlux (Config.java:88,497-499).
        for (int seed = 0; seed < 500; seed++) {
            assertFalse(FluxModel.taintConversionRoll(5, true, seeded(seed)));
            assertFalse(FluxModel.taintConversionRoll(8, false, seeded(seed)));
        }
        int hits = 0;
        for (int seed = 0; seed < 5000; seed++) {
            if (FluxModel.taintConversionRoll(6, true, seeded(seed))) {
                hits++;
            }
        }
        // 1/50 expected: ~100 hits over 5000.
        assertTrue(hits > 50 && hits < 160, "taint conversion rate off: " + hits);
    }

    @Test
    void gasContactEffectsMatchOriginal() {
        // BlockFluxGas.java:59-77: 1/10 contact -> 50/50 Vis Exhaustion 1200t
        // (amp meta/3, no curatives) or Nausea 80+meta*20t.
        int rolls = 0;
        int hits = 0;
        for (int seed = 0; seed < 500; seed++) {
            rolls++;
            if (FluxModel.gasContactRoll(8, seeded(seed)) != null) {
                hits++;
            }
        }
        // ~1/10 contact rate: expected 50 hits over 500 seeded rolls.
        assertTrue(hits > 20 && hits < 90, "gas contact rate off: " + hits + "/" + rolls);
        EffectSpec exhaust = null;
        EffectSpec nausea = null;
        for (int seed = 0; seed < 500 && (exhaust == null || nausea == null); seed++) {
            final EffectSpec spec = FluxModel.gasContactRoll(6, seeded(seed));
            if (spec == null) {
                continue;
            }
            if (EffectSpec.VIS_EXHAUST.equals(spec.effectId())) {
                exhaust = spec;
            } else {
                nausea = spec;
            }
        }
        assertNotNull(exhaust, "no Vis Exhaustion outcome in 500 seeded rolls");
        assertEquals(1200, exhaust.durationTicks());
        assertEquals(6 / 3, exhaust.amplifier());
        assertTrue(exhaust.noCuratives());
        assertNotNull(nausea, "no Nausea outcome in 500 seeded rolls");
        assertEquals(80 + 6 * 20, nausea.durationTicks());
        assertEquals(0, nausea.amplifier());
    }

    @Test
    void gooContactEffectMatchesOriginal() {
        // BlockFluxGoo.java:63-65: Vis Exhaustion 600t, amp meta/3, curatives cleared.
        final EffectSpec spec = FluxModel.gooContactEffect(7);
        assertEquals(EffectSpec.VIS_EXHAUST, spec.effectId());
        assertEquals(600, spec.durationTicks());
        assertEquals(7 / 3, spec.amplifier());
        assertTrue(spec.noCuratives());
    }

    @Test
    void slimeGrowthRequiresBiggerMeta() {
        // BlockFluxGoo.java:55-61: grows only while slimeSize < meta, coin flip.
        for (int seed = 0; seed < 50; seed++) {
            assertFalse(FluxModel.slimeGrowthRoll(1, 1, seeded(seed)));
            assertFalse(FluxModel.slimeGrowthRoll(0, 0, seeded(seed)));
        }
        // One stream, not per-trial seeds: java.util.Random's FIRST draw is
        // biased for small seeds, which would skew a per-seed coin-flip loop.
        final Rand stream = seeded(12345);
        int grew = 0;
        for (int i = 0; i < 200; i++) {
            if (FluxModel.slimeGrowthRoll(3, 2, stream)) {
                grew++;
            }
        }
        assertTrue(grew > 60 && grew < 140, "growth coin flip off: " + grew);
    }
}
