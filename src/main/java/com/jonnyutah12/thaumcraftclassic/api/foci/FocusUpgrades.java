package com.jonnyutah12.thaumcraftclassic.api.foci;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>The twenty-one original {@code FocusUpgradeType} entries (TC-M6-03-s), ids
 * 0-20 in the original's {@code types[]} order, transcribed value for value from
 * {@code reference/decompiled/…/api/wands/FocusUpgradeType.java:16-78} (ids
 * 0-8) and the ten focus classes (ids 9-20):
 *
 * <pre>
 *  0 potency         telum 1                 (FocusUpgradeType.java:16)
 *  1 frugal          fames 1                 (…:23)
 *  2 treasure        lucrum 1                (…:30)
 *  3 enlarge         iter 1                  (…:37)
 *  4 alchemistsfire  potentia 1 limus 1      (…:44)
 *  5 alchemistsfrost gelum 1 vinculum 1      (…:51)
 *  6 architect       fabrico 1               (…:58)
 *  7 extend          permutatio 1            (…:65)
 *  8 silktouch       lucrum 1                (…:72)
 *  9 fireball        tenebrae 1              (ItemFocusFire.java:27)
 * 10 firebeam        potentia 1 aer 1        (ItemFocusFire.java:34)
 * 11 scattershot     gelum 1 telum 1         (ItemFocusFrost.java:23)
 * 12 iceboulder      gelum 1 vitreus 1       (ItemFocusFrost.java:30)
 * 13 batbombs        potentia 1 vinculum 1   (ItemFocusHellbat.java:32)
 * 14 devilbats       tutamen 1               (ItemFocusHellbat.java:39)
 * 15 nightshade      victus 1 venenum 1 praecantatio 1  (ItemFocusPech.java:30)
 * 16 seeker          sensus 1 cognitio 1     (ItemFocusPrimal.java:23)
 * 17 chainlightning  tempestas 1             (ItemFocusShock.java:38)
 * 18 earthshock      tempestas 1             (ItemFocusShock.java:45)
 * 19 vampirebats     fames 1 victus 1        (ItemFocusHellbat.java:46)
 * 20 dowsing         perfodio 1             (ItemFocusExcavation.java:44)
 * </pre>
 *
 * <p>The original's aspect <em>object</em> names map to the port's tags
 * through the shipped {@code aspect/AspectDefinitions} table, so the costs
 * below are the original entries with the same numbers under the port's
 * lowercased aspect tags (D-001 for aspects is the shipped table's own
 * provenance, docs/aspects.md).</p>
 *
 * <p>The original also grew its {@code types[]} array past 20 when the last id
 * arrived ({@code dowsing}, id 20); the port's set is exactly the twenty-one the
 * shipped 4.2.3.5 tree declares, so a 22nd id is a code change, not a data
 * one.</p>
 */
public final class FocusUpgrades {

    /** {@code FocusUpgradeType.potency} (id 0). */
    public static final FocusUpgradeType POTENCY = type(0, "potency", cost("telum", 1));
    /** {@code FocusUpgradeType.frugal} (id 1). */
    public static final FocusUpgradeType FRUGAL = type(1, "frugal", cost("fames", 1));
    /** {@code FocusUpgradeType.treasure} (id 2). */
    public static final FocusUpgradeType TREASURE = type(2, "treasure", cost("lucrum", 1));
    /** {@code FocusUpgradeType.enlarge} (id 3). */
    public static final FocusUpgradeType ENLARGE = type(3, "enlarge", cost("iter", 1));
    /** {@code ItemFocusFire.alchemistsfire} — a shared entry, id 4. */
    public static final FocusUpgradeType ALCHEMISTSFIRE =
            type(4, "alchemistsfire", cost("potentia", 1), cost("limus", 1));
    /** {@code ItemFocusFrost.alchemistsfrost} — a shared entry, id 5. */
    public static final FocusUpgradeType ALCHEMISTSFROST =
            type(5, "alchemistsfrost", cost("gelum", 1), cost("vinculum", 1));
    /** {@code FocusUpgradeType.architect} (id 6). */
    public static final FocusUpgradeType ARCHITECT = type(6, "architect", cost("fabrico", 1));
    /** {@code FocusUpgradeType.extend} (id 7). */
    public static final FocusUpgradeType EXTEND = type(7, "extend", cost("permutatio", 1));
    /** {@code FocusUpgradeType.silktouch} (id 8). */
    public static final FocusUpgradeType SILKTOUCH = type(8, "silktouch", cost("lucrum", 1));
    /** {@code ItemFocusFire.fireball} (id 9). */
    public static final FocusUpgradeType FIREBALL = type(9, "fireball", cost("tenebrae", 1));
    /** {@code ItemFocusFire.firebeam} (id 10). */
    public static final FocusUpgradeType FIREBEAM =
            type(10, "firebeam", cost("potentia", 1), cost("aer", 1));
    /** {@code ItemFocusFrost.scattershot} (id 11). */
    public static final FocusUpgradeType SCATTERSHOT =
            type(11, "scattershot", cost("gelum", 1), cost("telum", 1));
    /** {@code ItemFocusFrost.iceboulder} (id 12). */
    public static final FocusUpgradeType ICEBOULDER =
            type(12, "iceboulder", cost("gelum", 1), cost("vitreus", 1));
    /** {@code ItemFocusHellbat.batbombs} (id 13). */
    public static final FocusUpgradeType BATBOMBS =
            type(13, "batbombs", cost("potentia", 1), cost("vinculum", 1));
    /** {@code ItemFocusHellbat.devilbats} (id 14). */
    public static final FocusUpgradeType DEVILBATS = type(14, "devilbats", cost("tutamen", 1));
    /** {@code ItemFocusPech.nightshade} (id 15). */
    public static final FocusUpgradeType NIGHTSHADE = type(15, "nightshade",
            cost("victus", 1), cost("venenum", 1), cost("praecantatio", 1));
    /** {@code ItemFocusPrimal.seeker} (id 16). */
    public static final FocusUpgradeType SEEKER =
            type(16, "seeker", cost("sensus", 1), cost("cognitio", 1));
    /** {@code ItemFocusShock.chainlightning} (id 17). */
    public static final FocusUpgradeType CHAINLIGHTNING = type(17, "chainlightning", cost("tempestas", 1));
    /** {@code ItemFocusShock.earthshock} (id 18). */
    public static final FocusUpgradeType EARTHSHOCK = type(18, "earthshock", cost("tempestas", 1));
    /** {@code ItemFocusHellbat.vampirebats} (id 19). */
    public static final FocusUpgradeType VAMPIREBATS =
            type(19, "vampirebats", cost("fames", 1), cost("victus", 1));

    /**
     * {@code ItemFocusExcavation.dowsing} (id 20) — the last id the shipped
     * 4.2.3.5 tree hands out, and the one that grew the original's
     * {@code new FocusUpgradeType[20]} to 21 entries
     * ({@code FocusUpgradeType.java:86-96}).
     */
    public static final FocusUpgradeType DOWSING = type(20, "dowsing", cost("perfodio", 1));

    /** The research entry the original gated the {@code vampirebats} upgrade on ({@code ItemFocusHellbat.java:173}). */
    public static final String VAMPIREBATS_RESEARCH = "VAMPBAT";

    private static final List<FocusUpgradeType> ALL;
    private static final Map<Integer, FocusUpgradeType> BY_ID = new LinkedHashMap<>();
    private static final Map<String, FocusUpgradeType> BY_TAG = new LinkedHashMap<>();

    static {
        ALL = List.of(POTENCY, FRUGAL, TREASURE, ENLARGE, ALCHEMISTSFIRE, ALCHEMISTSFROST, ARCHITECT,
                EXTEND, SILKTOUCH, FIREBALL, FIREBEAM, SCATTERSHOT, ICEBOULDER, BATBOMBS, DEVILBATS,
                NIGHTSHADE, SEEKER, CHAINLIGHTNING, EARTHSHOCK, VAMPIREBATS, DOWSING);
        if (ALL.size() != 21) {
            throw new IllegalStateException("the original declares 21 focus upgrade ids (0..20), got " + ALL.size());
        }
        for (final FocusUpgradeType type : ALL) {
            if (BY_ID.put(type.id(), type) != null) {
                throw new IllegalStateException("duplicate focus upgrade id: " + type.id());
            }
            if (BY_TAG.put(type.tag(), type) != null) {
                throw new IllegalStateException("duplicate focus upgrade tag: " + type.tag());
            }
            if (type.id() != ALL.indexOf(type)) {
                throw new IllegalStateException("focus upgrade " + type.tag()
                        + " is declared out of id order (id " + type.id() + " at index " + ALL.indexOf(type) + ")");
            }
        }
    }

    private static FocusUpgradeType type(final int id, final String tag, final Focus.Cost... cost) {
        return new FocusUpgradeType(id, tag, "focus.upgrade." + tag + ".name",
                "focus.upgrade." + tag + ".text", List.of(cost));
    }

    private static Focus.Cost cost(final String aspectTag, final int amount) {
        return new Focus.Cost(aspectTag, amount);
    }

    /** Every upgrade in the original {@code types[]} order (id ascending, 0..19). */
    public static List<FocusUpgradeType> all() {
        return Collections.unmodifiableList(new ArrayList<>(ALL));
    }

    /** An upgrade by its original {@code short id}, or null. */
    public static FocusUpgradeType byId(final int id) {
        return BY_ID.get(id);
    }

    /** An upgrade by its port tag (the original icon's file name), or null. */
    public static FocusUpgradeType byTag(final String tag) {
        return tag == null ? null : BY_TAG.get(tag);
    }

    private FocusUpgrades() {
    }
}
