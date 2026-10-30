package com.jonnyutah12.thaumcraftclassic.foci;

import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgradeType;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgrades;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Each focus's per-rank upgrade tree and its {@code canApplyUpgrade} rules
 * (TC-M6-03-s): the port of {@code getPossibleUpgradesByRank} /
 * {@code canApplyUpgrade} on the ten original focus classes.
 *
 * <p>The trees are transcribed rank for rank, in the original's own order
 * inside each rank (the order the Focal Manipulator renders, TC-M7-04):</p>
 * <pre>
 * fire         1 frugal potency | 2 frugal potency alchemistsfire | 3 frugal potency fireball firebeam | 4 frugal potency alchemistsfire | 5 frugal potency
 * shock        1 frugal potency | 2 frugal potency | 3 frugal potency chainlightning earthshock | 4 frugal potency enlarge | 5 frugal potency enlarge
 * hellbat      1 frugal potency | 2 frugal potency | 3 frugal potency batbombs devilbats | 4 frugal potency | 5 frugal potency vampirebats
 * frost        1 frugal potency alchemistsfrost | 2 frugal potency | 3 frugal potency scattershot iceboulder alchemistsfrost | 4 frugal potency | 5 frugal potency alchemistsfrost
 * trade        1 frugal enlarge | 2 frugal enlarge | 3 frugal enlarge treasure architect | 4 frugal enlarge | 5 frugal enlarge silktouch
 * excavation   1 frugal potency treasure | 2 frugal potency enlarge | 3 frugal potency treasure dowsing | 4 frugal potency enlarge | 5 frugal potency treasure silktouch
 * portablehole 1..5 frugal enlarge extend
 * pech         1 frugal potency | 2 frugal potency extend | 3 frugal potency | 4 frugal potency extend | 5 frugal potency nightshade
 * warding      1 frugal | 2 frugal architect | 3 frugal enlarge | 4 frugal enlarge | 5 frugal enlarge
 * primal       1 frugal | 2 frugal | 3 frugal seeker | 4 frugal | 5 frugal
 * </pre>
 *
 * <p>A rank the original did not answer for ({@code default: return null})
 * yields null, exactly as the original did; {@link FocusUpgradeList} separately
 * enforces the "one upgrade per rank, ranks 1..5" rule that
 * {@code applyUpgrade} implemented.</p>
 *
 * <h2>canApplyUpgrade</h2>
 * Four foci overrode it ({@code ItemFocusFire.java:157-162},
 * {@code ItemFocusShock.java:241-244}, {@code ItemFocusWarding.java:174-177},
 * {@code ItemFocusHellbat.java:171-174}); every other focus inherited
 * {@code true}:
 * <ul>
 *   <li>fire — {@code alchemistsfire} is refused once {@code fireball} and
 *       {@code alchemistsfire} are both already applied (the second
 *       alchemistsfire rank of the tree is only reachable without fireball)</li>
 *   <li>shock — {@code enlarge} needs {@code chainlightning} or
 *       {@code earthshock}</li>
 *   <li>warding — {@code enlarge} needs {@code architect}</li>
 *   <li>hellbat — {@code vampirebats} needs the {@code VAMPBAT} research entry,
 *       which the caller passes in as a boolean so the rule is testable without
 *       a player (the port's research API is TC-M3's)</li>
 * </ul>
 */
public final class FocusUpgradeTrees {

    private static final Map<String, List<List<FocusUpgradeType>>> TREES = trees();

    private static Map<String, List<List<FocusUpgradeType>>> trees() {
        final Map<String, List<List<FocusUpgradeType>>> out = new LinkedHashMap<>();
        out.put("fire", List.of(
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.ALCHEMISTSFIRE),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.FIREBALL, FocusUpgrades.FIREBEAM),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.ALCHEMISTSFIRE),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY)));
        out.put("shock", List.of(
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.CHAINLIGHTNING, FocusUpgrades.EARTHSHOCK),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.ENLARGE),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.ENLARGE)));
        out.put("hellbat", List.of(
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.BATBOMBS, FocusUpgrades.DEVILBATS),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.VAMPIREBATS)));
        out.put("frost", List.of(
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.ALCHEMISTSFROST),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.SCATTERSHOT,
                        FocusUpgrades.ICEBOULDER, FocusUpgrades.ALCHEMISTSFROST),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.ALCHEMISTSFROST)));
        out.put("trade", List.of(
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.ENLARGE),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.ENLARGE),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.ENLARGE, FocusUpgrades.TREASURE, FocusUpgrades.ARCHITECT),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.ENLARGE),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.ENLARGE, FocusUpgrades.SILKTOUCH)));
        out.put("excavation", List.of(
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.TREASURE),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.ENLARGE),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.TREASURE, FocusUpgrades.DOWSING),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.ENLARGE),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.TREASURE, FocusUpgrades.SILKTOUCH)));
        final List<FocusUpgradeType> hole =
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.ENLARGE, FocusUpgrades.EXTEND);
        out.put("portablehole", List.of(hole, hole, hole, hole, hole));
        out.put("pech", List.of(
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.EXTEND),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.EXTEND),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY, FocusUpgrades.NIGHTSHADE)));
        out.put("warding", List.of(
                List.of(FocusUpgrades.FRUGAL),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.ARCHITECT),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.ENLARGE),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.ENLARGE),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.ENLARGE)));
        out.put("primal", List.of(
                List.of(FocusUpgrades.FRUGAL),
                List.of(FocusUpgrades.FRUGAL),
                List.of(FocusUpgrades.FRUGAL, FocusUpgrades.SEEKER),
                List.of(FocusUpgrades.FRUGAL),
                List.of(FocusUpgrades.FRUGAL)));
        if (!out.keySet().equals(new java.util.LinkedHashSet<>(Focuses.tags()))) {
            throw new IllegalStateException("the upgrade trees must cover exactly the ten foci, got " + out.keySet());
        }
        return out;
    }

    /** The upgrades the original offered at {@code rank} (1-based), or null where it answered null. */
    public static List<FocusUpgradeType> byRank(final String tag, final int rank) {
        final List<List<FocusUpgradeType>> tree = TREES.get(tag);
        if (tree == null || rank < 1 || rank > FocusUpgradeList.RANKS) {
            return null;
        }
        return tree.get(rank - 1);
    }

    /** Whether the original's tree offers {@code type} at {@code rank} for this focus. */
    public static boolean offers(final String tag, final int rank, final FocusUpgradeType type) {
        final List<FocusUpgradeType> rankList = byRank(tag, rank);
        return rankList != null && rankList.contains(type);
    }

    /**
     * Original {@code canApplyUpgrade}: the tree's offer plus the four
     * per-focus extra rules. {@code researchComplete} is
     * {@code ThaumcraftApiHelper.isResearchComplete(player, "VAMPBAT")} for the
     * hellbat rule, passed in so this is decidable without a player.
     */
    public static boolean canApply(final String tag, final FocusUpgradeList applied,
                                   final FocusUpgradeType type, final int rank,
                                   final boolean researchComplete) {
        if (type == null || !offers(tag, rank, type)) {
            return false;
        }
        return switch (tag) {
            // !type.equals(alchemistsfire) || !isUpgradedWith(fireball) || !isUpgradedWith(alchemistsfire)
            case "fire" -> type != FocusUpgrades.ALCHEMISTSFIRE
                    || !applied.isUpgradedWith(FocusUpgrades.FIREBALL)
                    || !applied.isUpgradedWith(FocusUpgrades.ALCHEMISTSFIRE);
            // !type.equals(enlarge) || isUpgradedWith(chainlightning) || isUpgradedWith(earthshock)
            case "shock" -> type != FocusUpgrades.ENLARGE
                    || applied.isUpgradedWith(FocusUpgrades.CHAINLIGHTNING)
                    || applied.isUpgradedWith(FocusUpgrades.EARTHSHOCK);
            // type.equals(enlarge) ? isUpgradedWith(architect) : true
            case "warding" -> type != FocusUpgrades.ENLARGE
                    || applied.isUpgradedWith(FocusUpgrades.ARCHITECT);
            // !type.equals(vampirebats) || isResearchComplete(player, "VAMPBAT")
            case "hellbat" -> type != FocusUpgrades.VAMPIREBATS || researchComplete;
            default -> true;
        };
    }

    /** The ten tags the table covers. */
    public static java.util.Set<String> tags() {
        return java.util.Collections.unmodifiableSet(TREES.keySet());
    }

    private FocusUpgradeTrees() {
    }
}
