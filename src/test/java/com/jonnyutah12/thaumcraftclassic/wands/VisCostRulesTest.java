package com.jonnyutah12.thaumcraftclassic.wands;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-02 acceptance: vis cost application rules against the TC-M0-06
 * tables — cap cost modifiers (docs/balance-values.md §2.1), the modifier
 * chain (§2.4, ItemWandCasting.java:254-274), gear discounts and the Vis
 * Exhaustion penalty (§2.5, WandManager.java:50-84), and the truncating cost
 * cast (ItemWandCasting.java:306,330).
 */
class VisCostRulesTest {

    private static Aspect aspect(final String tag) {
        return new Aspect(AspectDefinitions.all().stream()
                .filter(def -> def.tag().equals(tag))
                .findFirst()
                .orElseThrow());
    }

    private static final Aspect AER = aspect("aer");
    private static final Aspect TERRA = aspect("terra");
    private static final Aspect IGNIS = aspect("ignis");
    private static final Aspect AQUA = aspect("aqua");
    private static final Aspect ORDO = aspect("ordo");
    private static final Aspect PERDITIO = aspect("perditio");

    // Cap table, docs/balance-values.md §2.1 (ConfigItems.java:287-291, 316-322).
    private static final VisCostRules.WandCapCost IRON =
            new VisCostRules.WandCapCost(1.1F, null, 0.0F);
    private static final VisCostRules.WandCapCost GOLD =
            new VisCostRules.WandCapCost(1.0F, null, 0.0F);
    private static final VisCostRules.WandCapCost THAUMIUM =
            new VisCostRules.WandCapCost(0.9F, null, 0.0F);
    private static final VisCostRules.WandCapCost VOID =
            new VisCostRules.WandCapCost(0.8F, null, 0.0F);
    private static final VisCostRules.WandCapCost COPPER =
            new VisCostRules.WandCapCost(1.1F, List.of(ORDO, PERDITIO), 1.0F);
    private static final VisCostRules.WandCapCost SILVER =
            new VisCostRules.WandCapCost(1.0F, List.of(AER, TERRA, IGNIS, AQUA), 0.95F);

    // ------------------------------------------------------- cap modifiers §2.1

    @Test
    void plainCapsUseTheirBaseModifierForEveryAspect() {
        assertEquals(1.1F, VisCostRules.consumptionModifier(IRON, null, AER, 0, false, false));
        assertEquals(1.0F, VisCostRules.consumptionModifier(GOLD, null, ORDO, 0, false, false));
        assertEquals(0.9F, VisCostRules.consumptionModifier(THAUMIUM, null, TERRA, 0, false, false));
        assertEquals(0.8F, VisCostRules.consumptionModifier(VOID, null, IGNIS, 0, false, false));
    }

    @Test
    void specialCapsUseTheirSpecialModifierOnlyForTheirAspects() {
        // Copper: ORDER/ENTROPY -> 1.0, everything else 1.1.
        assertEquals(1.0F, VisCostRules.consumptionModifier(COPPER, null, ORDO, 0, false, false));
        assertEquals(1.0F, VisCostRules.consumptionModifier(COPPER, null, PERDITIO, 0, false, false));
        assertEquals(1.1F, VisCostRules.consumptionModifier(COPPER, null, AER, 0, false, false));
        // Silver: AIR/EARTH/FIRE/WATER -> 0.95, everything else 1.0.
        assertEquals(0.95F, VisCostRules.consumptionModifier(SILVER, null, AER, 0, false, false));
        assertEquals(0.95F, VisCostRules.consumptionModifier(SILVER, null, TERRA, 0, false, false));
        assertEquals(0.95F, VisCostRules.consumptionModifier(SILVER, null, IGNIS, 0, false, false));
        assertEquals(0.95F, VisCostRules.consumptionModifier(SILVER, null, AQUA, 0, false, false));
        assertEquals(1.0F, VisCostRules.consumptionModifier(SILVER, null, ORDO, 0, false, false));
    }

    // ---------------------------------------------------- modifier chain §2.4

    @Test
    void gearDiscountSubtractsFromTheModifier() {
        // §2.5: goggles 5% -> total 0.05; gold cap 1.0 -> 0.95.
        final VisCostRules.VisDiscountSource goggles = aspect -> 0.05F;
        assertEquals(0.95F, VisCostRules.consumptionModifier(GOLD, goggles, AER, 0, false, false), 1e-6F);
    }

    @Test
    void frugalUpgradesSubtractTenPercentPerLevelButNotWhileCrafting() {
        // ItemWandCasting.java:266-268: frugal/10, gated off for crafting.
        assertEquals(0.8F, VisCostRules.consumptionModifier(GOLD, null, AER, 2, false, false), 1e-6F);
        assertEquals(1.0F, VisCostRules.consumptionModifier(GOLD, null, AER, 2, false, true), 1e-6F);
    }

    @Test
    void sceptreSubtractsTenPercent() {
        assertEquals(0.9F, VisCostRules.consumptionModifier(GOLD, null, AER, 0, true, false), 1e-6F);
        // Full chain: void cap 0.8 - 0.2 frugal - 0.1 sceptre = 0.5.
        assertEquals(0.5F, VisCostRules.consumptionModifier(VOID, null, AER, 2, true, false), 1e-6F);
    }

    @Test
    void modifierFloorsAtTenPercent() {
        // Heavy discount gear + sceptre cannot push the cost below 10%.
        final VisCostRules.VisDiscountSource everything = aspect -> 1.0F;
        assertEquals(0.1F, VisCostRules.consumptionModifier(GOLD, everything, AER, 0, false, false));
        // 1.1 - 0.9 - 0.1 in float leaves a hair above the floor (0.100000046);
        // the original's float chain does the same, so assert with tolerance.
        assertEquals(0.1F, VisCostRules.consumptionModifier(IRON, null, AER, 9, true, false), 1e-5F);
    }

    // ------------------------------------------------- aggregation §2.5 / WandManager

    @Test
    void gearPercentagesSumAndDivideByHundred() {
        // Goggles 5 + void robe 5 + hover harness 2 = 12% -> 0.12.
        assertEquals(0.12F, VisCostRules.totalDiscount(12, -1, -1), 1e-6F);
        assertEquals(0.0F, VisCostRules.totalDiscount(0, -1, -1));
    }

    @Test
    void visExhaustionPenaltyRaisesCostsByLevel() {
        // WandManager.java:70-83: total -= (max(amplifier)+1)*10 when potioned.
        // Amplifier 0 (level I) -> -10% discount, i.e. costs go UP by 0.1.
        assertEquals(-0.1F, VisCostRules.totalDiscount(0, 0, -1), 1e-6F);
        assertEquals(-0.3F, VisCostRules.totalDiscount(0, 2, -1), 1e-6F);
        // Infectious uses the same max(); only the higher amplifier counts.
        assertEquals(-0.3F, VisCostRules.totalDiscount(0, 1, 2), 1e-6F);
        // With gear: 12% gear - 20% penalty = -0.08.
        assertEquals(-0.08F, VisCostRules.totalDiscount(12, 1, -1), 1e-6F);
    }

    @Test
    void penaltyFlowsThroughTheModifierChain() {
        // Gold cap under Vis Exhaustion I: 1.0 - (-0.1) = 1.1.
        final VisCostRules.VisDiscountSource exhausted =
                aspect -> VisCostRules.totalDiscount(0, 0, -1);
        assertEquals(1.1F, VisCostRules.consumptionModifier(GOLD, exhausted, AER, 0, false, false), 1e-6F);
    }

    // --------------------------------------------------------- cost arithmetic

    @Test
    void costsApplyWithTheOriginalTruncatingCast() {
        // (int)(cost * modifier): 5 x 0.95 -> 4, 3 x 0.8 -> 2, 10 x 1.1 -> 11.
        assertEquals(4, VisCostRules.applyCost(5, 0.95F));
        assertEquals(2, VisCostRules.applyCost(3, 0.8F));
        assertEquals(11, VisCostRules.applyCost(10, 1.1F));
        assertEquals(0, VisCostRules.applyCost(1, 0.1F));
    }

    @Test
    void effectiveCostsApplyPerAspectAndFeedTheAllOrNothingPayment() {
        // Focus price 5 aer + 3 terra with a silver cap: aer special 0.95 -> 4,
        // terra special 0.95 -> 2 (terra is in the silver special set).
        final AspectList base = new AspectList();
        base.add(AER, 5);
        base.add(TERRA, 3);
        final AspectList costs = VisCostRules.effectiveCosts(base, SILVER, null, 0, false, false);
        assertEquals(4, costs.getAmount(AER));
        assertEquals(2, costs.getAmount(TERRA));

        final VisPool pool = WandVis.addRealVis(
                WandVis.addRealVis(VisPool.EMPTY, AER, 400, 5000).pool(), TERRA, 1, 5000).pool();
        assertFalse(VisCostRules.canPay(pool, costs)); // terra short (1 < 2)
        final VisPool funded = WandVis.addRealVis(pool, TERRA, 1, 5000).pool();
        assertTrue(VisCostRules.canPay(funded, costs));
    }
}
