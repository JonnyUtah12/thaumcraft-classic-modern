package com.jonnyutah12.thaumcraftclassic.aspect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TC-M4-01 acceptance: discovery semantics. The pure amount arithmetic of
 * the original {@code ScanManager.checkAndSyncAspectKnowledge} — first
 * discovery +2, square-root compression at the pool cap, collapse to 1 at
 * 1.25x the cap. The wiring (packets, player state) is covered in-game by
 * AspectSystemGameTest.
 */
class AspectDiscoveryRulesTest {

    /** The shipped default of {@code aspect_total_cap} (TcConfigCatalog). */
    private static final int CAP = 100;

    @Test
    void firstDiscoveryAddsTheOriginalBonusTwo() {
        assertEquals(7, AspectKnowledge.effectiveAmount(0, 5, true, CAP));
        assertEquals(2, AspectKnowledge.effectiveAmount(0, 0, true, CAP));
    }

    @Test
    void repeatGainsPassThroughBelowTheCap() {
        assertEquals(5, AspectKnowledge.effectiveAmount(7, 5, false, CAP));
        assertEquals(99, AspectKnowledge.effectiveAmount(0, 99, false, CAP));
        assertEquals(0, AspectKnowledge.effectiveAmount(7, 0, false, CAP));
    }

    @Test
    void gainsAreSquareRootCompressedAtTheCap() {
        // Original: amount = (int) Math.sqrt(amount) once pool >= cap.
        assertEquals(3, AspectKnowledge.effectiveAmount(100, 10, false, CAP));
        assertEquals(1, AspectKnowledge.effectiveAmount(100, 1, false, CAP));
        assertEquals(0, AspectKnowledge.effectiveAmount(100, 0, false, CAP));
        // First discovery at the cap: +2 applies before the compression.
        assertEquals(3, AspectKnowledge.effectiveAmount(100, 7, true, CAP));
    }

    @Test
    void multiPointGainsCollapseToOneAtOneAndAQuarterCap() {
        // Original: amount > 1 && pool >= cap * 1.25F -> amount = 1.
        assertEquals(1, AspectKnowledge.effectiveAmount(125, 10, false, CAP));
        // Boundary: at 124 the sqrt compression applies but the collapse
        // threshold (cap * 1.25F = 125) does not.
        assertEquals(3, AspectKnowledge.effectiveAmount(124, 10, false, CAP));
        assertEquals(1, AspectKnowledge.effectiveAmount(125, 0, true, CAP));   // +2 -> sqrt(2)=1
        // A gain already compressed to 1 is not touched by the collapse rule.
        assertEquals(1, AspectKnowledge.effectiveAmount(124, 1, false, CAP));
    }

    @Test
    void capIsReadFromConfigNotHardcoded() {
        // The rules key off the cap value passed in (TcConfig aspect_total_cap).
        assertEquals(10, AspectKnowledge.effectiveAmount(50, 10, false, 100));
        assertEquals(3, AspectKnowledge.effectiveAmount(50, 10, false, 50));
    }
}
