package com.jonnyutah12.thaumcraftclassic.api.foci;

import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import java.util.List;
import java.util.Objects;

/**
 * Registration record of a wand-focus upgrade (TC-M6-03-s): the port of
 * {@code thaumcraft.api.wands.FocusUpgradeType}
 * ({@code reference/decompiled/…/api/wands/FocusUpgradeType.java:9-111}).
 *
 * <p>The original declared twenty of these in static fields spread over
 * {@code FocusUpgradeType} itself (ids 0-8) and the ten focus classes (ids
 * 9-19), each self-registering into the {@code types[]} array by id
 * ({@code FocusUpgradeType.java:80-97}). The port keeps that set as data in
 * {@link FocusUpgrades}: {@code id} is the original's {@code short id} — the
 * value a focus stack stores — and it is also the index of the original's
 * {@code types[]} array.</p>
 *
 * <p>Only the identity and the application price live here. {@code icon} is a
 * 1.7.10 {@code ResourceLocation} into {@code textures/foci/} (TC-M7-01's
 * texture remap) and is deliberately absent, exactly as the parent's
 * {@link Focus} record omits its icon. {@code nameKey}/{@code textKey} are the
 * original's lang keys verbatim ({@code focus.upgrade.<tag>.name} /
 * {@code …​.text}); the name is what a tooltip shows and the text is the Focal
 * Manipulator's description (TC-M7-04).</p>
 *
 * <p>The aspect prices are held as <em>tags</em>, not {@code Aspect} objects —
 * the same rule {@link Focus} follows, because the table is built before the
 * aspect registry is populated. {@link #cost()} materialises them at use
 * time.</p>
 *
 * @param id         the original {@code short id} (0..19, also the {@code types[]}
 *                   index)
 * @param tag        the original icon's file name, lowercased per D-001 — the
 *                   upgrade's identity in the port (and the variant id a
 *                   {@code foci/FocusCombinations} combination is keyed by)
 * @param nameKey    the original {@code focus.upgrade.<tag>.name} lang key
 * @param textKey    the original {@code focus.upgrade.<tag>.text} lang key
 * @param aspectCost the aspects the original declared for applying the upgrade
 *                   ({@code FocusUpgradeType.aspects})
 */
public record FocusUpgradeType(int id, String tag, String nameKey, String textKey,
                               List<Focus.Cost> aspectCost) {

    public FocusUpgradeType {
        if (id < 0) {
            throw new IllegalArgumentException("a focus upgrade id is a non-negative short, got " + id);
        }
        Objects.requireNonNull(tag, "a focus upgrade needs a tag");
        Objects.requireNonNull(nameKey, "a focus upgrade needs a name key");
        Objects.requireNonNull(textKey, "a focus upgrade needs a text key");
        aspectCost = List.copyOf(aspectCost);
    }

    /**
     * The upgrade's application price with the live aspect registry
     * ({@code FocusUpgradeType.aspects}) — unused by a cast, the aspect cost is
     * what the Focal Manipulator charges for the research/vis of applying it.
     */
    public AspectList cost() {
        return Focus.resolveCost(aspectCost);
    }
}
