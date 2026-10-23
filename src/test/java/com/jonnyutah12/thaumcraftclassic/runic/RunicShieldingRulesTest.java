package com.jonnyutah12.thaumcraftclassic.runic;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M8-02 acceptance: the pure rule core of {@code RunicShielding} against
 * the original {@code EventHandlerRunic} (common/lib/events/
 * EventHandlerRunic.java) and the value pins in docs/balance-values.md
 * §1.7/§10.3. The event wiring itself is covered by BaubleGameTest.
 */
class RunicShieldingRulesTest {

    // ------------------------------------------------ getFinalCharge (343-354)

    @Test
    void finalChargeAddsHardening() {
        assertEquals(8, RunicShielding.finalCharge(8, 0));
        assertEquals(11, RunicShielding.finalCharge(8, 3));
        assertEquals(0, RunicShielding.finalCharge(0, 0));
    }

    // ------------------------------------------------- scan accumulation (52-84)

    @Test
    void gearRowSumsChargeAndCountsFlags() {
        // Ring meta1 (5) + charged ring meta2 (4, charged) + amulet meta1
        // (7, emergency) + kinetic girdle meta1 (9, kinetic).
        final var row = RunicShielding.gearRow(List.of(
                new RunicShielding.RunicGear(5, false, false, false, false),
                new RunicShielding.RunicGear(4, true, false, false, false),
                new RunicShielding.RunicGear(7, false, false, false, true),
                new RunicShielding.RunicGear(9, false, true, false, false)));
        assertEquals(25, row.max());
        assertEquals(1, row.charged());
        assertEquals(1, row.kinetic());
        assertEquals(0, row.healing());
        assertEquals(1, row.emergency());
    }

    @Test
    void emptyGearRowIsZero() {
        final var row = RunicShielding.gearRow(List.of());
        assertEquals(0, row.max());
        assertEquals(RunicShielding.GearRow.NONE, row);
    }

    // ------------------------------------------------- recharge interval (127)

    @Test
    void chargedRingsShortenIntervalByFiveHundred() {
        assertEquals(2000, RunicShielding.rechargeIntervalMs(2000, 0));
        assertEquals(1500, RunicShielding.rechargeIntervalMs(2000, 1));
        assertEquals(0, RunicShielding.rechargeIntervalMs(2000, 4));
    }

    // ---------------------------------------------------- absorption (208-216)

    @Test
    void absorptionTruncatesChargeAndZeroesDamageWhenPoolCovers() {
        // Original: charge > ammount → charge = (int)(charge - ammount), damage 0.
        final var a = RunicShielding.absorb(10, 4.5F);
        assertEquals(5, a.chargeLeft());
        assertEquals(0.0F, a.damageLeft());
    }

    @Test
    void absorptionDrainsPoolAndLeaksDamageWhenPoolIsSmaller() {
        final var b = RunicShielding.absorb(3, 7.5F);
        assertEquals(0, b.chargeLeft());
        assertEquals(4.5F, b.damageLeft());
    }

    @Test
    void equalChargeAndDamageDrainsPoolExactly() {
        // charge > damage is false at equality — original's boundary.
        final var c = RunicShielding.absorb(5, 5.0F);
        assertEquals(0, c.chargeLeft());
        assertEquals(0.0F, c.damageLeft());
    }

    // -------------------------------------------------- emergency refill (246-248)

    @Test
    void emergencyRefillIsEightPerAmuletCappedByMax() {
        assertEquals(8, RunicShielding.emergencyRefill(19, 1));
        assertEquals(16, RunicShielding.emergencyRefill(19, 2));
        assertEquals(5, RunicShielding.emergencyRefill(5, 1));
    }

    // ------------------------------------------------------ kinetic radius (226)

    @Test
    void kineticRadiusGrowsByHalfPerGirdle() {
        assertEquals(1.5F, RunicShielding.kineticRadius(0));
        assertEquals(2.0F, RunicShielding.kineticRadius(1));
        assertEquals(2.5F, RunicShielding.kineticRadius(2));
    }

    // --------------------------------------------------- upgrade cooldown gate

    @Test
    void cooldownConstantsMatchOriginal() {
        assertEquals(20_000L, RunicShielding.UPGRADE_COOLDOWN_MS);
        assertEquals(60_000L, RunicShielding.EMERGENCY_COOLDOWN_MS);
    }

    @Test
    void gearRowNoneIsAllZero() {
        assertFalse(RunicShielding.GearRow.NONE.max() > 0);
        assertTrue(RunicShielding.GearRow.NONE.charged() == 0);
    }
}
