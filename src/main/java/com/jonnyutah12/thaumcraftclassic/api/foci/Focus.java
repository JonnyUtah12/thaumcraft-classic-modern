package com.jonnyutah12.thaumcraftclassic.api.foci;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import java.util.List;

/**
 * Registration record of a wand focus (TC-M6-02): the value-carrying half of
 * the original's {@code thaumcraft.api.wands.ItemFocusBasic} subclasses
 * ({@code common/items/wands/foci/ItemFocusFire.java} etc.), which declared
 * their identity and price in static fields.
 *
 * <p>A focus is <em>data</em> in the port: the ten original focus classes differ
 * only in these values plus their per-focus spell behaviour, and the behaviour
 * is TC-M6-03-s. One generic item class ({@code items/foci/ItemFocus}) is
 * registered once per content-mapping.csv {@code item} row, so a focus id is
 * still one registered object per row
 * (docs/architecture/registration.md).</p>
 *
 * <p>Aspect prices are held as <em>tags</em>, not {@link Aspect} objects: the
 * table is built during registration wiring, before the aspect registry is
 * populated — the same rule {@code wands/WandComponents} follows. Use
 * {@link #cost()} to materialise the {@link AspectList} at cast/tooltip time.</p>
 *
 * @param tag               the wand-cap/rod-style identity tag, lowercased per
 *                          D-001 (also the item's content-mapping.csv id path)
 * @param researchKey       the original research entry that gates the focus
 *                          ({@code FOCUSFIRE}, ... — an entry of the shipped
 *                          201-entry research graph)
 * @param sortingHelper     the original {@code getSortingHelper} prefix
 *                          ({@code "AF"}, {@code "BF"}, ...) — the focus
 *                          pouch's fixed sort key
 * @param baseCost          the price with no upgrades applied
 *                          ({@code getVisCost} on a bare stack)
 * @param focusColor        the orb colour the wand renders for this focus
 *                          (original {@code getFocusColor})
 * @param activationCooldown the original {@code getActivationCooldown} with no
 *                          upgrades applied, in milliseconds
 * @param visCostPerTick    original {@code isVisCostPerTick} (tooltip wording
 *                          and the cast loop's per-tick charge)
 */
public record Focus(String tag, String researchKey, String sortingHelper, List<Cost> baseCost,
                    int focusColor, int activationCooldown, boolean visCostPerTick) {

    /** One aspect amount of a focus's per-cast price (an original {@code AspectList} entry). */
    public record Cost(String aspectTag, int amount) {
    }

    /**
     * The focus's price with no upgrades applied, resolved against the live
     * aspect registry. Empty when the focus rolls its price instead — see
     * {@code foci/Focuses#primalCost}.
     */
    public AspectList cost() {
        return resolveCost(baseCost);
    }

    /**
     * A declared cost list resolved against the live aspect registry — the
     * shared implementation of {@link #cost()} and of the TC-M6-03-s
     * combination prices ({@code foci/FocusCombinations}). Aspect tags are held
     * as strings because both tables are built during registration wiring, before
     * the aspect registry is populated; an unknown tag contributes nothing, which
     * is the original's {@code new AspectList().add(null, n)} shape.
     */
    public static AspectList resolveCost(final List<Cost> costs) {
        final AspectList list = new AspectList();
        for (final Cost cost : costs) {
            final Aspect aspect = Aspect.getAspect(cost.aspectTag());
            if (aspect != null) {
                list.add(aspect, cost.amount());
            }
        }
        return list;
    }

    /** Original {@code getSortingHelper}: prefix + the applied upgrade ids. */
    public String sortingHelper() {
        return sortingHelper;
    }
}
