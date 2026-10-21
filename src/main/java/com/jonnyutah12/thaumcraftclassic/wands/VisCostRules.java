package com.jonnyutah12.thaumcraftclassic.wands;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import java.util.List;

/**
 * Vis cost application rules (TC-M4-02; design record: docs/architecture/vis.md).
 *
 * <p>Ports the consumption-modifier chain of
 * {@code ItemWandCasting.getConsumptionModifier} ({@code common/items/wands/
 * ItemWandCasting.java:254-274}) and the cost arithmetic of
 * {@code consumeVis}/{@code consumeAllVis} (lines 305-347). The effective cost
 * of a focus action is {@code base × modifier} with the original's truncating
 * int cast; the modifier is built from the cap profile, worn-gear discounts,
 * Frugal focus upgrades and the sceptre bonus, floored at 10%.</p>
 *
 * <p>The rule engine is parameterized over its inputs so it is testable against
 * the TC-M0-06 tables (docs/balance-values.md §2) before the wand/bauble items
 * exist: TC-M6-01 supplies {@link WandCapCost} from registered caps, TC-M8-02
 * supplies a {@link VisDiscountSource} from worn equipment. Focus upgrades are
 * plain levels (the original reads them from the focus component; without a
 * focus the level is 0, so the {@code focus != null} gate at line 266 is
 * equivalent to {@code frugal > 0}).</p>
 */
public final class VisCostRules {

    /** Floor on the consumption modifier (original {@code Math.max(mod, 0.1F)}, line 273). */
    public static final float MIN_CONSUMPTION_MODIFIER = 0.1F;

    /**
     * The cost-relevant half of a wand cap (original {@code api/wands/WandCap}
     * fields {@code baseCostModifier}/{@code specialCostModifierAspects}/
     * {@code specialCostModifier}; value table docs/balance-values.md §2.1).
     */
    public record WandCapCost(float baseCostModifier,
                              List<Aspect> specialCostModifierAspects,
                              float specialCostModifier) {
    }

    /**
     * Per-aspect discount source in fraction-of-cost units (0.05 = 5%): the
     * already-summed total the original computed in
     * {@code WandManager.getTotalVisDiscount} (percent total / 100,
     * {@code common/items/wands/WandManager.java:50-84}). TC-M8-02 wires this
     * to worn equipment and the Vis Exhaustion potions.
     */
    @FunctionalInterface
    public interface VisDiscountSource {
        float getVisDiscount(Aspect aspect);
    }

    /**
     * The original's discount aggregation (WandManager.java:50-84): sum of worn
     * {@code IVisDiscountGear} percentages, minus {@code (max(amplifier)+1)×10}
     * when the player has Vis Exhaustion or Infectious Vis Exhaustion (amplifier
     * 0-based; pass -1 for "not under the effect"), divided by 100.
     */
    public static float totalDiscount(final int gearPercentTotal,
                                     final int visExhaustAmplifier,
                                     final int infectiousVisExhaustAmplifier) {
        int total = gearPercentTotal;
        if (visExhaustAmplifier >= 0 || infectiousVisExhaustAmplifier >= 0) {
            total -= (Math.max(visExhaustAmplifier, infectiousVisExhaustAmplifier) + 1) * 10;
        }
        return total / 100.0F;
    }

    /**
     * Effective consumption modifier for one aspect — the original chain,
     * preserved term-for-term:
     * <pre>
     * mod = cap.specialCostModifier if aspect in cap.specialAspects else cap.baseCostModifier
     * mod -= total gear discount            (player term, line 262-264)
     * mod -= frugalLevel / 10               (focus term, skipped while crafting, line 266-268)
     * mod -= 0.1                            (sceptre term, line 270-272)
     * return max(mod, 0.1)                 (floor, line 273)
     * </pre>
     */
    public static float consumptionModifier(final WandCapCost cap,
                                           final VisDiscountSource gear,
                                           final Aspect aspect,
                                           final int frugalUpgradeLevel,
                                           final boolean sceptre,
                                           final boolean crafting) {
        float modifier = cap.specialCostModifierAspects() != null
                && cap.specialCostModifierAspects().contains(aspect)
                ? cap.specialCostModifier()
                : cap.baseCostModifier();

        if (gear != null) {
            modifier -= gear.getVisDiscount(aspect);
        }

        if (frugalUpgradeLevel > 0 && !crafting) {
            modifier -= frugalUpgradeLevel / 10.0F;
        }

        if (sceptre) {
            modifier -= 0.1F;
        }

        return Math.max(modifier, MIN_CONSUMPTION_MODIFIER);
    }

    /**
     * Apply a modifier to one base cost with the original's truncating cast
     * ({@code (int)(cost * modifier)}, ItemWandCasting.java:306,330). Float
     * arithmetic is preserved exactly — do not "clean up" to doubles.
     */
    public static int applyCost(final int baseAmount, final float modifier) {
        return (int) (baseAmount * modifier);
    }

    /**
     * Per-aspect effective costs for a whole {@link AspectList} price
     * (original {@code consumeAllVis} cost loop, ItemWandCasting.java:327-331).
     * Feed the result to {@link WandVis#consumeAllVis} for the all-or-nothing
     * payment, or {@link #canPay} to test affordability first.
     */
    public static AspectList effectiveCosts(final AspectList baseCosts,
                                            final WandCapCost cap,
                                            final VisDiscountSource gear,
                                            final int frugalUpgradeLevel,
                                            final boolean sceptre,
                                            final boolean crafting) {
        final AspectList costs = new AspectList();
        for (final Aspect aspect : baseCosts.getAspects()) {
            final float modifier =
                    consumptionModifier(cap, gear, aspect, frugalUpgradeLevel, sceptre, crafting);
            costs.add(aspect, applyCost(baseCosts.getAmount(aspect), modifier));
        }
        return costs;
    }

    /** Whether a pool covers an already-modified cost list (the check loop of {@code consumeAllVis}). */
    public static boolean canPay(final VisPool pool, final AspectList centiCosts) {
        return WandVis.consumeAllVis(pool, centiCosts) != null;
    }

    private VisCostRules() {
    }
}
