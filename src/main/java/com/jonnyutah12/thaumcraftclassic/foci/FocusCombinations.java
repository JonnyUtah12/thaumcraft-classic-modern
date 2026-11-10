package com.jonnyutah12.thaumcraftclassic.foci;

import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgradeType;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgrades;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The original combination effects of the ten foci (TC-M6-03-s): what changes
 * when an upgrade combines with a focus.
 *
 * <p>Every original focus resolved its price, its cooldown and its effect with
 * a ternary chain over the applied upgrades — the <em>combination</em>:</p>
 * <pre>
 * fire         isUpgradedWith(firebeam) ? costBeam : isUpgradedWith(fireball) ? costBall : costBase   (ItemFocusFire.java:62-79)
 * shock        chainlightning ? costChain : earthshock ? costGround : costBase                        (ItemFocusShock.java:73-85)
 * hellbat      batbombs ? costBomb : devilbats ? costDevil : costBase                                 (ItemFocusHellbat.java:144-151)
 * frost        scattershot ? costScatter : iceboulder ? costBoulder : costBase                        (ItemFocusFrost.java:93-100)
 * trade        silktouch ? cost2 : cost                                                               (ItemFocusTrade.java:222-239)
 * excavation   silktouch ? cost2 : dowsing ? cost2 : cost                                            (ItemFocusExcavation.java:72-96)
 * pech         nightshade ? costAll : cost                                                            (ItemFocusPech.java:84-86)
 * warding      cost (no combination)                                                                  (ItemFocusWarding.java:81-83)
 * portablehole cost (no combination; the body multiplies it by the hole length)                       (ItemFocusPortableHole.java:55-57,133-137)
 * primal       rolled per cast (no combination)                                                       (ItemFocusPrimal.java:75-84)
 * </pre>
 *
 * <p>The chain's <em>order is load-bearing</em> and is preserved here: frost
 * with both scattershot and iceboulder prices as scattershot, fire with both
 * firebeam and fireball prices as firebeam, and excavation checks silktouch
 * before dowsing. {@link #variants(String)} returns each focus's combinations
 * in that order with the uncombined variant last, and {@link #resolve} picks
 * the first one whose upgrade is applied — the original's chain exactly.</p>
 *
 * <h2>One deliberate deviation</h2>
 * {@code ItemFocusTrade} and {@code ItemFocusExcavation} cached their
 * upgrade-dependent price in a <em>static</em> field shared by all stacks
 * ({@code ItemFocusTrade.java:37}, {@code ItemFocusExcavation.java:37}), so
 * whichever variant was priced first in a session leaked into every later
 * lookup — a stack with only {@code dowsing} returned the silktouch price once
 * any silktouch stack had been priced. The port computes each combination's
 * price independently and keeps the original's <em>values</em> and check order;
 * the cross-stack leak is not reproduced (it is a defect, not behaviour).</p>
 *
 * <p>Costs are held as {@link Focus.Cost} tags like the parent's
 * {@link Focus} record; {@link #cost(String, FocusUpgradeList)} resolves them
 * through the live aspect registry.</p>
 */
public final class FocusCombinations {

    /**
     * One combination: the upgrade that selects it, its price, its cooldown and
     * whether its body runs per tick while the wand is held.
     *
     * @param id                the variant id — the selecting upgrade's tag, or
     *                          {@link #BASE} for the uncombined variant
     * @param requires          the upgrade that selects this combination, null for the base
     * @param cost              the price with that combination applied ({@code Focus.Cost}
     *                          tags; empty for the primal focus, which rolls its price)
     * @param activationCooldown the original's {@code getActivationCooldown} for this combination, ms
     * @param channeled         whether the original's body for this combination ran in
     *                          {@code onUsingFocusTick} (held) rather than on the right-click
     */
    public record Combination(String id, FocusUpgradeType requires, List<Focus.Cost> cost,
                              int activationCooldown, boolean channeled) {

        public Combination {
            cost = List.copyOf(cost);
        }

        /** Whether {@code applied} selects this combination; the base is always selected. */
        public boolean selectedBy(final FocusUpgradeList applied) {
            return requires == null || applied.isUpgradedWith(requires);
        }

        /** Whether this is the uncombined variant. */
        public boolean base() {
            return requires == null;
        }
    }

    /** The uncombined variant's id. */
    public static final String BASE = "base";

    private static final Map<String, List<Combination>> BY_TAG = combinations();

    private static Map<String, List<Combination>> combinations() {
        final Map<String, List<Combination>> out = new LinkedHashMap<>();

        // ItemFocusFire.java:23-25,62-79
        out.put("fire", List.of(
                combo(FocusUpgrades.FIREBEAM, 0, true, cost("ignis", 10), cost("ordo", 3)),
                combo(FocusUpgrades.FIREBALL, 1000, false, cost("ignis", 66), cost("perditio", 33)),
                base(0, true, cost("ignis", 10))));

        // ItemFocusShock.java:35-37,73-85
        out.put("shock", List.of(
                combo(FocusUpgrades.CHAINLIGHTNING, 500, true, cost("aer", 40), cost("aqua", 10)),
                combo(FocusUpgrades.EARTHSHOCK, 1000, false, cost("aer", 75), cost("terra", 25)),
                base(250, true, cost("aer", 25))));

        // ItemFocusHellbat.java:29-31,144-151 (all three declare the 1000 ms cooldown)
        out.put("hellbat", List.of(
                combo(FocusUpgrades.BATBOMBS, 1000, false,
                        cost("ignis", 100), cost("perditio", 200), cost("aer", 100)),
                combo(FocusUpgrades.DEVILBATS, 1000, false,
                        cost("ignis", 100), cost("perditio", 100), cost("aer", 100), cost("terra", 100)),
                base(1000, false, cost("ignis", 200), cost("perditio", 100), cost("aer", 100))));

        // ItemFocusFrost.java:20-22,93-100 (cooldown 200 up to 500 once either combination applies)
        out.put("frost", List.of(
                combo(FocusUpgrades.SCATTERSHOT, 500, false,
                        cost("aqua", 20), cost("ignis", 2), cost("perditio", 2), cost("aer", 5)),
                combo(FocusUpgrades.ICEBOULDER, 500, false,
                        cost("aqua", 20), cost("ignis", 2), cost("perditio", 2), cost("terra", 5)),
                base(200, false, cost("aqua", 5), cost("ignis", 2), cost("perditio", 2))));

        // ItemFocusTrade.java:36,222-239 — the silktouch price is cost + one of every primal
        out.put("trade", List.of(
                combo(FocusUpgrades.SILKTOUCH, 0, false,
                        cost("perditio", 6), cost("terra", 6), cost("ordo", 6),
                        cost("aer", 1), cost("ignis", 1), cost("aqua", 1)),
                base(0, false, cost("perditio", 5), cost("terra", 5), cost("ordo", 5))));

        // ItemFocusExcavation.java:36,72-96 — silktouch = earth 15 + one of each primal, checked first
        out.put("excavation", List.of(
                combo(FocusUpgrades.SILKTOUCH, 0, true,
                        cost("terra", 16), cost("aer", 1), cost("ignis", 1),
                        cost("aqua", 1), cost("ordo", 1), cost("perditio", 1)),
                combo(FocusUpgrades.DOWSING, 0, true,
                        cost("ignis", 2), cost("ordo", 2), cost("terra", 15)),
                base(0, true, cost("terra", 15))));

        out.put("portablehole", List.of(base(0, false, cost("perditio", 10), cost("aer", 10))));

        // ItemFocusPech.java:22-29,84-86 — nightshade prices all six primals at 10
        out.put("pech", List.of(
                combo(FocusUpgrades.NIGHTSHADE, 250, false,
                        cost("aer", 10), cost("ignis", 10), cost("terra", 10),
                        cost("ordo", 10), cost("perditio", 10), cost("aqua", 10)),
                base(250, false, cost("terra", 10), cost("perditio", 10), cost("aqua", 10))));

        out.put("warding", List.of(base(0, false, cost("terra", 25), cost("ordo", 25), cost("aqua", 10))));

        // ItemFocusPrimal.java:75-84 rolls its price per cast, so both combinations carry no cost.
        out.put("primal", List.of(
                combo(FocusUpgrades.SEEKER, 500, false),
                base(500, false)));

        if (!out.keySet().equals(new java.util.LinkedHashSet<>(Focuses.tags()))) {
            throw new IllegalStateException("the combination table must cover exactly the ten foci, got " + out.keySet());
        }
        return out;
    }

    private static Combination combo(final FocusUpgradeType requires, final int cooldown,
                                     final boolean channeled, final Focus.Cost... cost) {
        return new Combination(requires.tag(), requires, List.of(cost), cooldown, channeled);
    }

    private static Combination base(final int cooldown, final boolean channeled, final Focus.Cost... cost) {
        return new Combination(BASE, null, List.of(cost), cooldown, channeled);
    }

    private static Focus.Cost cost(final String aspectTag, final int amount) {
        return new Focus.Cost(aspectTag, amount);
    }

    // ------------------------------------------------------------------ lookups

    /** A focus's combinations in the original's check order, the uncombined variant last. */
    public static List<Combination> variants(final String tag) {
        return BY_TAG.getOrDefault(tag, List.of());
    }

    /** Every focus tag the table covers, in the focus table's order. */
    public static List<String> tags() {
        return Collections.unmodifiableList(new ArrayList<>(BY_TAG.keySet()));
    }

    /** The combination a stack with {@code applied} casts with — the original's ternary chain. */
    public static Combination resolve(final String tag, final FocusUpgradeList applied) {
        for (final Combination combination : variants(tag)) {
            if (combination.selectedBy(applied == null ? FocusUpgradeList.NONE : applied)) {
                return combination;
            }
        }
        return null;
    }

    /** The combination's id for a stack — {@link #BASE} when uncombined, null for an unknown focus. */
    public static String variantId(final String tag, final FocusUpgradeList applied) {
        final Combination combination = resolve(tag, applied);
        return combination == null ? null : combination.id();
    }

    /** The resolved combination's price, resolved against the live aspect registry. */
    public static AspectList cost(final String tag, final FocusUpgradeList applied) {
        final Combination combination = resolve(tag, applied);
        return combination == null ? new AspectList() : Focus.resolveCost(combination.cost());
    }

    /** The resolved combination's original cooldown in ms, or 0 for an unknown focus. */
    public static int cooldown(final String tag, final FocusUpgradeList applied) {
        final Combination combination = resolve(tag, applied);
        return combination == null ? 0 : combination.activationCooldown();
    }

    /**
     * Whether the resolved combination's body is the per-tick one
     * ({@code onUsingFocusTick}): fire's base/beam stream, shock's bolt,
     * excavation's dig. The original also gated the channel on
     * {@code onFocusRightClick} starting the use, which is the port's
     * {@code WandCasting.Status#CHANNELED}.
     */
    public static boolean channeled(final String tag, final FocusUpgradeList applied) {
        final Combination combination = resolve(tag, applied);
        return combination != null && combination.channeled();
    }

    private FocusCombinations() {
    }
}
