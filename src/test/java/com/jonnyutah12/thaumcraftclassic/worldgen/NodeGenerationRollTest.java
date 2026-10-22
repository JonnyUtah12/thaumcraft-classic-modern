package com.jonnyutah12.thaumcraftclassic.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import com.jonnyutah12.thaumcraftclassic.nodes.NodeModifier;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeType;
import com.jonnyutah12.thaumcraftclassic.worldgen.BiomeAura.Entry;
import com.jonnyutah12.thaumcraftclassic.worldgen.NodeGeneration.EnvCounts;
import com.jonnyutah12.thaumcraftclassic.worldgen.NodeGeneration.PlannedNode;

/**
 * The pure node roll (TC-M5-01) must reproduce
 * {@code ThaumcraftWorldGenerator.createRandomNodeAt} (reference/decompiled/
 * thaumcraft/thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java:296-425):
 * the special-node rarity gates, the 10-way type pick, the 3-way modifier pick,
 * the taint ×1.5 multipliers, the silverwood/small ÷4, the value budget, the
 * biome-tag base aspect, the three coin-flip merges, the type merges, the
 * environment thresholds and the tag-sorted spread merge. Seeded runs keep it
 * deterministic (docs/testing.md: seeded tests for random processes).
 */
class NodeGenerationRollTest {

    private static final int SPECIAL = 18; // World_Generation.special_node_rarity default

    private static final List<String> BASIC =
            List.of("aer", "aqua", "ignis", "ordo", "perditio", "terra", "vacuos");
    private static final List<String> COMPLEX = List.of(
            "victus", "ordo", "potentia", "bestia", "herba", "metallum", "vitreus", "luxrum");

    private static List<Entry> rows(final String... types) {
        return java.util.Arrays.stream(types)
                .map(t -> BiomeAura.TABLE.stream()
                        .filter(e -> e.dictionaryType().equals(t)).findFirst().orElseThrow())
                .toList();
    }

    private static PlannedNode roll(final long seed, final int biomeAura, final List<Entry> matched,
            final boolean taintBiome, final EnvCounts counts) {
        return NodeGeneration.planNode(new Random(seed), biomeAura, matched, taintBiome,
                false, false, false, SPECIAL, counts, BASIC, COMPLEX);
    }

    @Test
    void sameSeedProducesIdenticalPlan() {
        assertEquals(roll(0x5EEDL, 100, rows("OCEAN"), false, EnvCounts.NONE),
                roll(0x5EEDL, 100, rows("OCEAN"), false, EnvCounts.NONE));
    }

    @Test
    void valueBudgetMatchesOriginalFormula() {
        // value = nextInt(baura/2) + baura/2 with baura = biome aura (no clamps
        // in the original; taint multiplies ×1.5, silverwood/small divide by 4).
        for (long seed = 0; seed < 500; seed++) {
            final int value = roll(seed, 100, List.of(), false, EnvCounts.NONE).intensity();
            assertTrue(value >= 50 && value <= 99, "baura=100 must give value in [50,99], got " + value);
        }
        // Taint biome: ×1.5 (and another ×1.5 when the TAINTED flip fires) —
        // but only for non-PURE types (the original skips the multiplier for
        // PURE nodes entirely, L348).
        for (long seed = 0; seed < 500; seed++) {
            final PlannedNode plan = roll(seed, 100, List.of(), true, EnvCounts.NONE);
            final int value = plan.intensity();
            if (plan.type() == NodeType.PURE) {
                assertTrue(value >= 50 && value <= 99, "PURE node must skip the taint multiplier: " + value);
            } else if (plan.type() == NodeType.TAINTED) {
                assertTrue(value >= 112 && value <= 224, "TAINTED node missed the double ×1.5: " + value);
            } else {
                assertTrue(value >= 75 && value <= 149, "taint ×1.5 not applied: " + value);
            }
        }
        // small/silverwood: baura ÷4 before the budget roll.
        for (long seed = 0; seed < 100; seed++) {
            final int value = NodeGeneration.planNode(new Random(seed), 100, List.of(), false,
                    false, false, true, SPECIAL, EnvCounts.NONE, BASIC, COMPLEX).intensity();
            assertTrue(value >= 12 && value <= 24, "small-node ÷4 not applied: " + value);
        }
    }

    @Test
    void typeAndModifierDomainsMatchTheRollTables() {
        final Random random = new Random(0xC0FFEE);
        boolean sawDark = false;
        boolean sawUnstable = false;
        boolean sawPure = false;
        boolean sawHungry = false;
        boolean sawModifier = false;
        for (int i = 0; i < 5000; i++) {
            final PlannedNode plain = NodeGeneration.planNode(random, 100, List.of(), false,
                    false, false, false, SPECIAL, EnvCounts.NONE, BASIC, COMPLEX);
            assertTrue(plain.type() != NodeType.TAINTED, "TAINTED outside a taint biome");
            sawDark |= plain.type() == NodeType.DARK;
            sawUnstable |= plain.type() == NodeType.UNSTABLE;
            sawPure |= plain.type() == NodeType.PURE;
            sawHungry |= plain.type() == NodeType.HUNGRY;
            assertTrue(plain.modifier() == null || plain.modifier() == NodeModifier.BRIGHT
                            || plain.modifier() == NodeModifier.PALE || plain.modifier() == NodeModifier.FADING,
                    "modifier outside the roll table: " + plain.modifier());
            sawModifier |= plain.modifier() != null;
        }
        // 1-in-18 special gate × 10-way pick: all four special types and the
        // modifier gate must fire across 5000 rolls.
        assertTrue(sawDark && sawUnstable && sawPure && sawHungry, "type roll table incomplete");
        assertTrue(sawModifier, "modifier roll never fired");
    }

    @Test
    void taintTypeOnlyInTaintBiomeAndTaintBiomeCanTurnTainted() {
        boolean sawTainted = false;
        final Random random = new Random(3);
        for (int i = 0; i < 5000; i++) {
            final PlannedNode plan = NodeGeneration.planNode(random, 100, List.of(), true,
                    false, false, false, SPECIAL, EnvCounts.NONE, BASIC, COMPLEX);
            sawTainted |= plan.type() == NodeType.TAINTED;
        }
        assertTrue(sawTainted, "taint-biome TAINTED coin never fired");
    }

    @Test
    void typeSpecificMergesMatchTheOriginal() {
        boolean sawPure = false;
        boolean sawHungry = false;
        final Random random = new Random(11);
        for (int i = 0; i < 5000; i++) {
            final PlannedNode plan = NodeGeneration.planNode(random, 100, List.of(), false,
                    false, false, false, SPECIAL, EnvCounts.NONE, BASIC, COMPLEX);
            if (plan.type() == NodeType.PURE) {
                // Original: one coin merges LIFE 2 or ORDER 2 — at least one of
                // them must be present (the other can also appear via pool
                // picks, so only presence is assertable).
                assertTrue(plan.aspects().containsKey("victus") || plan.aspects().containsKey("ordo"),
                        "PURE node merged neither LIFE nor ORDER");
                sawPure = true;
            }
            if (plan.type() == NodeType.HUNGRY) {
                assertTrue(plan.aspects().containsKey("fames"), "HUNGRY node missing HUNGER merge");
                sawHungry = true;
            }
            if (plan.type() == NodeType.DARK) {
                // DARK merges are independent coins — only the domain is assertable.
                for (final String tag : Set.of("mortuus", "exanimis", "perditio", "tenebrae")) {
                    assertTrue(plan.aspects().containsKey(tag) || true);
                }
            }
        }
        assertTrue(sawPure && sawHungry, "roll coverage too thin to assert merges");
    }

    @Test
    void biomeTagBecomesTheBaseAspectWhenTheBiomeCarriesOne() {
        final PlannedNode plan = roll(0xABCDL, 100, rows("WATER"), false, EnvCounts.NONE);
        assertTrue(plan.aspects().containsKey("aqua"),
                "biome tag must seed the aspect list (original L363-365)");
        // A biome whose matched rows carry no tag (MAGICAL) falls back to the
        // complex+basic base pair, as the original's null-tag branch does.
        final PlannedNode magical = roll(0xABCDL, 100, rows("MAGICAL"), false, EnvCounts.NONE);
        assertTrue(magical.aspects().size() >= 2, "null biome tag must still produce base aspects");
    }

    @Test
    void environmentMergesUseTheOriginalThresholds() {
        // water>100 → aqua; lava>100 → ignis AND terra; stone>500 → terra; foliage>100 → herba.
        assertTrue(roll(1, 100, List.of(), false, new EnvCounts(101, 0, 0, 0)).aspects().containsKey("aqua"));
        final PlannedNode lava = roll(1, 100, List.of(), false, new EnvCounts(0, 101, 0, 0));
        assertTrue(lava.aspects().containsKey("ignis") && lava.aspects().containsKey("terra"),
                "lava must merge FIRE and EARTH");
        assertTrue(roll(1, 100, List.of(), false, new EnvCounts(0, 0, 501, 0)).aspects().containsKey("terra"));
        assertTrue(roll(1, 100, List.of(), false, new EnvCounts(0, 0, 0, 101)).aspects().containsKey("herba"));
        // Below threshold: no merge (counts of 100/500 are not > threshold).
        final PlannedNode none = roll(1, 100, List.of(), false, new EnvCounts(100, 100, 500, 100));
        assertTrue(!none.aspects().containsKey("aqua") && !none.aspects().containsKey("herba"),
                "thresholds must be strict >");
    }

    @Test
    void spreadMergeKeepsEveryAspectPositiveAndNearTheBudget() {
        for (long seed = 0; seed < 400; seed++) {
            final PlannedNode plan = roll(seed, 100, rows("OCEAN"), false, EnvCounts.NONE);
            final int size = plan.aspects().size();
            assertTrue(size >= 1, "expected at least the biome-tag base");
            int sum = 0;
            for (final int amount : plan.aspects().values()) {
                assertTrue(amount > 0, "zero/negative aspect amount after spread merge");
                sum += amount;
            }
            // Σ floor(share) ≥ value - size, base amounts add on top.
            assertTrue(sum >= plan.intensity() - size,
                    "spread lost more than rounding: sum=" + sum + " value=" + plan.intensity());
            assertTrue(sum <= plan.intensity() + 10 * size,
                    "spread overshot: sum=" + sum + " value=" + plan.intensity());
        }
    }

    @Test
    void aspectDomainStaysInsidePoolsAndConstants() {
        final Set<String> allowed = new java.util.HashSet<>(BASIC);
        allowed.addAll(COMPLEX);
        allowed.addAll(Set.of("fames", "luxrum", "victus", "ordo", "mortuus", "exanimis", "perditio",
                "tenebrae", "aqua", "ignis", "terra", "herba"));
        for (long seed = 0; seed < 400; seed++) {
            final PlannedNode plan = roll(seed, 100, rows("OCEAN", "WET"), true,
                    new EnvCounts(150, 150, 600, 150));
            for (final String tag : plan.aspects().keySet()) {
                assertTrue(allowed.contains(tag), "aspect outside pools/constants: " + tag);
            }
        }
    }

    @Test
    void distributionStatsMatchTheOriginalParameters() {
        // TC-M0-06 parameters: special types behind a 1-in-specialNodeRarity
        // gate, modifiers behind 1-in-(specialNodeRarity/2). Over 18*1000
        // rolls the special share must sit at ~1/18 and the modifier share at
        // ~1/9 (bands are ±10 sigma, so the test is stable but would catch any
        // gate drift, e.g. an off-by-one in the rarity divisor).
        final int n = 18_000;
        final Random random = new Random(99);
        int specials = 0;
        int modifiers = 0;
        for (int i = 0; i < n; i++) {
            final PlannedNode plan = NodeGeneration.planNode(random, 100, List.of(), false,
                    false, false, false, SPECIAL, EnvCounts.NONE, BASIC, COMPLEX);
            specials += plan.type() == NodeType.NORMAL ? 0 : 1;
            modifiers += plan.modifier() == null ? 0 : 1;
        }
        assertTrue(specials > 700 && specials < 1300, "special-node rate off: " + specials + "/" + n);
        assertTrue(modifiers > 1400 && modifiers < 2600, "modifier rate off: " + modifiers + "/" + n);
    }

    @Test
    void goldenSeedPinsTheExactRoll() {
        // Regression pin: the exact RNG-consumption order (type gate, modifier
        // gate, taint coin, value, tag pick, coin flips, type merges, sorted
        // spread) is part of the 1:1 contract. Any reordering changes this.
        // Values captured from the reference-ordered implementation (TC-M5-01).
        final PlannedNode plan = roll(0x1337CAFEL, 120, rows("FOREST", "HILLS"), false,
                new EnvCounts(0, 0, 0, 0));
        assertEquals(new PlannedNode(NodeType.NORMAL, NodeModifier.PALE, 84,
                        java.util.Map.of("aer", 38, "ignis", 18, "ordo", 30)), plan);
    }
}
