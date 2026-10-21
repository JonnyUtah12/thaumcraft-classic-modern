package com.jonnyutah12.thaumcraftclassic.wands;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-02 acceptance: vis pool math against the TC-M0-06 tables
 * (docs/balance-values.md §2.2-2.3) and the deposit/withdraw semantics of
 * {@code ItemWandCasting} (centi-vis units, clamping, all-or-nothing
 * payment). Aspects are built straight from {@link AspectDefinitions} — the
 * pool itself is keyed by aspect tag, so no registry is needed (the
 * registry-backed primal enumeration is covered in-game by
 * {@code VisCoreGameTest}).
 */
class VisPoolMathTest {

    private static Aspect aspect(final String tag) {
        return new Aspect(AspectDefinitions.all().stream()
                .filter(def -> def.tag().equals(tag))
                .findFirst()
                .orElseThrow());
    }

    private static final Aspect AER = aspect("aer");
    private static final Aspect IGNIS = aspect("ignis");
    private static final Aspect TERRA = aspect("terra");
    /** "lux" (Light = aer+ignis) — a compound aspect, must be refused as stored vis. */
    private static final Aspect LUX = aspect("lux");

    // ------------------------------------------------------- capacity math §2.2/2.3

    @Test
    void wandCapacitiesMatchTheOriginalRodTable() {
        // docs/balance-values.md §2.2: wood 25, greatwood 50, primal rods 75, silverwood 100.
        assertEquals(2500, WandVis.maxVis(25, false));   // wood
        assertEquals(5000, WandVis.maxVis(50, false));   // greatwood
        assertEquals(7500, WandVis.maxVis(75, false));   // obsidian/blaze/ice/quartz/bone/reed
        assertEquals(10000, WandVis.maxVis(100, false)); // silverwood
        // Staff rods carry their own capacities; without the sceptre flag the
        // multiplier is still x100 (the flag, not the class, drives the bonus).
        assertEquals(12500, WandVis.maxVis(125, false)); // greatwood staff
        assertEquals(17500, WandVis.maxVis(175, false)); // primal staves
        assertEquals(25000, WandVis.maxVis(250, false)); // silverwood/primal staves
    }

    @Test
    void sceptreMultipliesCapacityByOneFifty() {
        // §2.3: maxVis = rod.capacity × (isSceptre ? 150 : 100).
        assertEquals(7500, WandVis.maxVis(50, true));    // greatwood sceptre
        assertEquals(15000, WandVis.maxVis(100, true));  // silverwood sceptre
        assertEquals(37500, WandVis.maxVis(250, true));  // primal staff sceptre
    }

    // ------------------------------------------------------------- deposit rules

    @Test
    void depositUnderCapacityStoresFullAmountWithoutLeftover() {
        final WandVis.AddResult result = WandVis.addVis(VisPool.EMPTY, AER, 30, 5000);
        assertEquals(3000, WandVis.getVis(result.pool(), AER));
        assertEquals(0, result.leftover());
    }

    @Test
    void depositOverCapacityClampsAndReportsLeftoverInRealVis() {
        // Original addVis: storeAmount = cur + amount*100; clamp to max; leftover/100.
        final WandVis.AddResult result = WandVis.addVis(VisPool.EMPTY, AER, 40, 2500); // wood wand
        assertEquals(2500, WandVis.getVis(result.pool(), AER));
        assertEquals(15, result.leftover());
    }

    @Test
    void addRealVisWorksInCentiVisAndReportsCentiLeftover() {
        final WandVis.AddResult result = WandVis.addRealVis(VisPool.EMPTY, AER, 2600, 2500);
        assertEquals(2500, WandVis.getVis(result.pool(), AER));
        assertEquals(100, result.leftover());
    }

    @Test
    void compoundAspectsAreRefusedAsCharge() {
        final WandVis.AddResult result = WandVis.addVis(VisPool.EMPTY, LUX, 10, 5000);
        assertEquals(VisPool.EMPTY, result.pool());
        assertEquals(0, result.leftover());
        assertEquals(0, WandVis.addRealVis(VisPool.EMPTY, LUX, 500, 5000).leftover());
    }

    @Test
    void depositsAccumulateAcrossAspects() {
        VisPool pool = WandVis.addRealVis(VisPool.EMPTY, AER, 1000, 5000).pool();
        pool = WandVis.addRealVis(pool, IGNIS, 700, 5000).pool();
        pool = WandVis.addRealVis(pool, TERRA, 700, 5000).pool();
        assertEquals(1000, WandVis.getVis(pool, AER));
        assertEquals(700, WandVis.getVis(pool, IGNIS));
        assertEquals(700, WandVis.getVis(pool, TERRA));
    }

    // ------------------------------------------------------------ withdraw rules

    @Test
    void withdrawOfCoveredAmountSucceeds() {
        final VisPool charged = WandVis.addRealVis(VisPool.EMPTY, AER, 3000, 5000).pool();
        final VisPool after = WandVis.consumeVis(charged, AER, 1250);
        assertNotNull(after);
        assertEquals(1750, WandVis.getVis(after, AER));
    }

    @Test
    void withdrawOfUncoveredAmountIsRefusedAndChangesNothing() {
        final VisPool charged = WandVis.addRealVis(VisPool.EMPTY, AER, 1000, 5000).pool();
        assertNull(WandVis.consumeVis(charged, AER, 1001));
        assertEquals(1000, WandVis.getVis(charged, AER));
    }

    @Test
    void multiAspectPaymentIsAllOrNothing() {
        // Original consumeAllVis: check every aspect first, subtract none on failure.
        final VisPool charged = WandVis.addRealVis(VisPool.EMPTY, AER, 1000, 5000).pool();
        final AspectList costs = new AspectList();
        costs.add(AER, 1000);
        costs.add(IGNIS, 1);
        assertNull(WandVis.consumeAllVis(charged, costs));

        final VisPool funded = WandVis.addRealVis(charged, IGNIS, 500, 5000).pool();
        final VisPool paid = WandVis.consumeAllVis(funded, costs);
        assertNotNull(paid);
        assertEquals(0, WandVis.getVis(paid, AER));
        assertEquals(499, WandVis.getVis(paid, IGNIS));
    }

    @Test
    void emptyAndNullCostListsNeverPay() {
        final VisPool charged = WandVis.addRealVis(VisPool.EMPTY, AER, 1000, 5000).pool();
        assertNull(WandVis.consumeAllVis(charged, new AspectList()));
        assertNull(WandVis.consumeAllVis(charged, null));
        assertNull(WandVis.consumeAllVisCrafting(charged, new AspectList()));
    }

    @Test
    void craftingCostsScaleByOneHundred() {
        // Original consumeAllVisCrafting: cost = amount * 100 (ItemWandCasting.java:314).
        final VisPool charged = WandVis.addRealVis(VisPool.EMPTY, AER, 3000, 5000).pool();
        final AspectList recipeCost = new AspectList();
        recipeCost.add(AER, 25); // 25 real vis
        final VisPool paid = WandVis.consumeAllVisCrafting(charged, recipeCost);
        assertNotNull(paid);
        assertEquals(500, WandVis.getVis(paid, AER));

        final AspectList tooMuch = new AspectList();
        tooMuch.add(AER, 31);
        assertNull(WandVis.consumeAllVisCrafting(charged, tooMuch));
    }

    // ------------------------------------------------------------------ payload

    @Test
    void poolNormalizesZeroEntriesAway() {
        final VisPool pool = new VisPool(Map.of("aer", 0, "ignis", 500));
        assertEquals(Map.of("ignis", 500), pool.centiVis());
        assertTrue(new VisPool(Map.of()).isEmpty());
    }

    @Test
    void codecRoundTripsThroughNbt() {
        final VisPool pool = WandVis.addRealVis(
                WandVis.addRealVis(VisPool.EMPTY, AER, 3750, 5000).pool(), TERRA, 500, 5000).pool();
        final Tag encoded = VisPool.CODEC.encodeStart(NbtOps.INSTANCE, pool).result().orElseThrow();
        final VisPool decoded = VisPool.CODEC.decode(NbtOps.INSTANCE, encoded).result().orElseThrow().getFirst();
        assertEquals(pool, decoded);
        assertEquals(3750, decoded.getVis(AER));
        assertEquals(500, decoded.getVis(TERRA));
    }

    @Test
    void streamCodecRoundTripsThroughABuffer() {
        final VisPool pool = WandVis.addRealVis(VisPool.EMPTY, IGNIS, 12345, 99999).pool();
        final var buf = Unpooled.buffer();
        VisPool.STREAM_CODEC.encode(buf, pool);
        assertEquals(pool, VisPool.STREAM_CODEC.decode(buf));
    }

    @Test
    void poolIsTagKeyedAndIndependentOfAspectIdentity() {
        // The component stores aspect tags; a freshly built Aspect instance reads the same charge.
        final VisPool pool = WandVis.addRealVis(VisPool.EMPTY, AER, 100, 5000).pool();
        assertEquals(100, WandVis.getVis(pool, aspect("aer")));
        final List<String> tags = List.copyOf(pool.centiVis().keySet());
        assertEquals(List.of("aer"), tags);
    }
}
