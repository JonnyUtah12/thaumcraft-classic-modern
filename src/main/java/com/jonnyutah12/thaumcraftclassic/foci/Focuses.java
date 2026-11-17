package com.jonnyutah12.thaumcraftclassic.foci;

import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The wand focus table (TC-M6-02): the port of the ten focus items
 * {@code ConfigItems.java:335-354} registers, with the values their classes
 * declared in {@code common/items/wands/foci/}.
 *
 * <p>Order is the original registration order and the docs/content-mapping.csv
 * row order (fire, shock, hellbat, frost, trade, excavation, portablehole,
 * pech, warding, primal) — the focus pouch sorts by
 * {@link Focus#sortingHelper()} on this table.</p>
 *
 * <p>Original values, verbatim (cost with no upgrades = {@code getVisCost} on a
 * bare stack; colour = {@code getFocusColor}; cooldown =
 * {@code getActivationCooldown} with no upgrades):</p>
 * <pre>
 * fire          AF   ignis 10                              colour 15028484  cooldown    0  per-tick
 * shock         BL   aer 25                                colour 10466239  cooldown  250
 * hellbat       HH   ignis 200  perditio 100  aer 100     colour 14431746  cooldown 1000
 * frost         BF   aqua 5  ignis 2  perditio 2          colour  5204428  cooldown  200
 * trade         BT   perditio 5  terra 5  ordo 5          colour  8747923  cooldown    0
 * excavation    BE   terra 15                             colour   409606  cooldown    0  per-tick
 * portablehole  BPH  perditio 10  aer 10                  colour   594985  cooldown    0
 * pech          PP   terra 10  perditio 10  aqua 10       colour  2267460  cooldown  250
 * warding       BWA  terra 25  ordo 25  aqua 10           colour 16771535  cooldown    0
 * primal        FP   rolled per cast (see below)          colour 10854849  cooldown  500
 * </pre>
 *
 * <p>The primal focus is the one focus whose price is not a constant: the
 * original rolled {@code 50 + rand.nextInt(5) * 50} independently for each of
 * the six primals, seeded from {@code System.currentTimeMillis() / 200}
 * ({@code ItemFocusPrimal.getVisCost}). {@link #primalCost(long)} keeps that
 * arithmetic and takes the seed, so the rule is testable; the live cast path
 * passes the original's clock-derived seed.</p>
 *
 * <p>Deliberately absent: the per-focus upgrade trees
 * ({@code getPossibleUpgradesByRank}) and the upgrade-dependent cost/cooldown
 * variants ({@code costBeam}/{@code costBall}/... on an upgraded stack). Those
 * are per-focus behaviour and land with TC-M6-03-s; every value here is the
 * original's no-upgrade value.</p>
 */
public final class Focuses {

    /** The primal focus's tag — the one focus whose price is rolled, not declared. */
    public static final String PRIMAL_TAG = "primal";

    /** The primal focus's roll step and roll count ({@code ItemFocusPrimal.getVisCost}). */
    public static final int PRIMAL_COST_STEP = 50;
    public static final int PRIMAL_COST_ROLLS = 5;
    /** {@code ItemFocusPrimal.getVisCost} seeds its {@link Random} from this divisor of the clock. */
    public static final long PRIMAL_COST_SEED_DIVISOR = 200L;

    private static final List<Focus> FOCI = new ArrayList<>(10);
    private static final Map<String, Focus> BY_TAG = new LinkedHashMap<>();

    static {
        focus("fire", "FOCUSFIRE", "AF", 15028484, 0, true, cost("ignis", 10));
        focus("shock", "FOCUSSHOCK", "BL", 10466239, 250, false, cost("aer", 25));
        focus("hellbat", "FOCUSHELLBAT", "HH", 14431746, 1000, false,
                cost("ignis", 200), cost("perditio", 100), cost("aer", 100));
        focus("frost", "FOCUSFROST", "BF", 5204428, 200, false,
                cost("aqua", 5), cost("ignis", 2), cost("perditio", 2));
        focus("trade", "FOCUSTRADE", "BT", 8747923, 0, false,
                cost("perditio", 5), cost("terra", 5), cost("ordo", 5));
        focus("excavation", "FOCUSEXCAVATION", "BE", 409606, 0, true, cost("terra", 15));
        focus("portablehole", "FOCUSPORTABLEHOLE", "BPH", 594985, 0, false,
                cost("perditio", 10), cost("aer", 10));
        // PECH (not FOCUSPECH): the original gates Pech's Curse on the PECH
        // research entry, and the CSV carries no FOCUSPECH research row.
        focus("pech", "PECH", "PP", 2267460, 250, false,
                cost("terra", 10), cost("perditio", 10), cost("aqua", 10));
        focus("warding", "FOCUSWARDING", "BWA", 16771535, 0, false,
                cost("terra", 25), cost("ordo", 25), cost("aqua", 10));
        // The primal focus rolls its price instead; see primalCost(long).
        focus(PRIMAL_TAG, "FOCUSPRIMAL", "FP", 10854849, 500, false);
    }

    private static void focus(final String tag, final String researchKey, final String sortingHelper,
                              final int focusColor, final int activationCooldown,
                              final boolean visCostPerTick, final Focus.Cost... baseCost) {
        final Focus entry = new Focus(tag, researchKey, sortingHelper, List.of(baseCost), focusColor,
                activationCooldown, visCostPerTick);
        FOCI.add(entry);
        if (BY_TAG.put(tag, entry) != null) {
            throw new IllegalStateException("duplicate focus tag: " + tag);
        }
    }

    private static Focus.Cost cost(final String aspectTag, final int amount) {
        return new Focus.Cost(aspectTag, amount);
    }

    // ------------------------------------------------------------------ lookups

    /** Every focus in original registration / CSV row order (10). */
    public static List<Focus> all() {
        return Collections.unmodifiableList(FOCI);
    }

    /** A focus by tag, or null — the original's item lookup by id path. */
    public static Focus byTag(final String tag) {
        return tag == null ? null : BY_TAG.get(tag);
    }

    /** The focus tags in table order (the pouch's sort key set). */
    public static Collection<String> tags() {
        return Collections.unmodifiableCollection(BY_TAG.keySet());
    }

    /** The research entry that gates a focus, or null for an unknown tag. */
    public static String researchKey(final String tag) {
        final Focus focus = byTag(tag);
        return focus == null ? null : focus.researchKey();
    }

    // ------------------------------------------------------------ primal price

    /**
     * One primal's rolled price: the original's {@code 50 + rand.nextInt(5) * 50}
     * ({@code ItemFocusPrimal.getVisCost}), split out so the arithmetic is
     * provable without a live aspect registry.
     *
     * @param roll the {@code nextInt(5)} result, 0..4
     */
    public static int primalRoll(final int roll) {
        if (roll < 0 || roll >= PRIMAL_COST_ROLLS) {
            throw new IllegalArgumentException("a primal roll is 0.." + (PRIMAL_COST_ROLLS - 1) + ", got " + roll);
        }
        return PRIMAL_COST_STEP + roll * PRIMAL_COST_STEP;
    }

    /**
     * {@code ItemFocusPrimal.getVisCost}: every primal gets
     * {@code 50 + rand(0..4) * 50} centi-vis from one shared roll stream, in the
     * original's {@code Aspect.getPrimalAspects()} order. The amounts are the
     * <em>base</em> price — the TC-M4-02 cost chain still applies the cap/gear
     * modifiers to them at payment time.
     */
    public static AspectList primalCost(final Random random) {
        final AspectList list = new AspectList();
        for (final Aspect primal : Aspect.getPrimalAspects()) {
            list.add(primal, primalRoll(random.nextInt(PRIMAL_COST_ROLLS)));
        }
        return list;
    }

    /** The original's clock-derived seed: {@code System.currentTimeMillis() / 200}. */
    public static long primalSeed(final long currentTimeMillis) {
        return currentTimeMillis / PRIMAL_COST_SEED_DIVISOR;
    }

    /** {@link #primalCost(Random)} seeded the way the original did. */
    public static AspectList primalCost(final long currentTimeMillis) {
        return primalCost(new Random(primalSeed(currentTimeMillis)));
    }

    /**
     * The price of {@code tag} on an unupgraded stack, or an empty list for an
     * unknown / unported focus. The primal focus needs a clock, so it is
     * resolved by the caller through {@link #primalCost(long)}.
     */
    public static AspectList baseCost(final String tag) {
        final Focus focus = byTag(tag);
        return focus == null ? new AspectList() : focus.cost();
    }

    private Focuses() {
    }
}
