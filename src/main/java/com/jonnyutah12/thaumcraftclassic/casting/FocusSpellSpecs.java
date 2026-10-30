package com.jonnyutah12.thaumcraftclassic.casting;

import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgradeType;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgrades;
import com.jonnyutah12.thaumcraftclassic.foci.FocusCombinations;
import com.jonnyutah12.thaumcraftclassic.foci.FocusUpgradeList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The effect each focus combination produces (TC-M6-03-s): every number the ten
 * original {@code onFocusRightClick}/{@code onUsingFocusTick} bodies fed their
 * projectiles, as a pure function of the wand's upgrade levels.
 *
 * <p>This is the decision half of the port's split (see {@link SpellEffects}):
 * the original interleaved "compute the parameters" with "add the entity to the
 * world", and the port keeps the first part here so the exact arithmetic —
 * {@code 2 + potency} embers per beam tick, {@code (int)(5 + potency * 1.33)}
 * shock-orb damage, {@code 120 + 60 * extend} hole duration — is unit-testable
 * without a server, and the second part behind a seam.</p>
 *
 * <p>The table is keyed by focus tag and combination id
 * ({@link FocusCombinations.Combination#id()}); {@link #has} lets a test assert
 * the two tables cover each other, so a combination can never be priced but
 * unperformable.</p>
 */
public final class FocusSpellSpecs {

    /**
     * The wand's upgrade-derived numbers for one cast
     * ({@code ItemWandCasting.getFocusPotency}/{@code getFocusEnlarge}/
     * {@code getFocusExtend}/{@code getFocusTreasure}), plus the world-derived
     * pair the portable-hole body needs (the scan's {@code distance} and the
     * pointed {@code side}).
     *
     * @param upgrades the focus's applied upgrades — the alchemist's fire/frost
     *                 levels and the {@code architect}/{@code dowsing} flags
     *                 come from here
     * @param potency  {@code getFocusPotency} (focus level + the runed wand's +1)
     * @param enlarge  {@code getFocusEnlarge}
     * @param extend   {@code getFocusExtend}
     * @param treasure {@code getFocusTreasure}
     * @param distance the portable hole's scan result (0 for every other focus)
     * @param side     the pointed face's {@code get3DDataValue()} (1 = up, the original's default)
     */
    public record SpellInput(FocusUpgradeList upgrades, int potency, int enlarge, int extend, int treasure,
                             int distance, int side) {

        /** The level of one applied upgrade. */
        public int level(final FocusUpgradeType type) {
            return upgrades.level(type);
        }

        /** Whether one upgrade is applied. */
        public boolean has(final FocusUpgradeType type) {
            return upgrades.isUpgradedWith(type);
        }
    }

    /** Builds one combination's effect from the cast's numbers. */
    @FunctionalInterface
    public interface SpecFactory {
        SpellEffect spec(SpellInput input);
    }

    private static final Map<String, Map<String, SpecFactory>> BY_TAG = specs();

    private static Map<String, Map<String, SpecFactory>> specs() {
        final Map<String, Map<String, SpecFactory>> out = new LinkedHashMap<>();

        // ItemFocusFire.java:104-137 (onUsingFocusTick) and 82-102 (the fireball branch).
        // The beam's damage is (2 + potency + 0.5F) * 1.5F with duration 30; the base
        // stream scatters 15.0F, the beam 0.25F.
        out.put("fire", Map.of(
                "firebeam", in -> SpellEffect.Projectile.ember(2 + in.potency(),
                        (2 + in.potency() + 0.5F) * 1.5F, 0.25F, 30,
                        in.level(FocusUpgrades.ALCHEMISTSFIRE)),
                "fireball", in -> SpellEffect.Projectile.explosiveOrb(1.0F + in.potency() * 0.4F,
                        in.has(FocusUpgrades.ALCHEMISTSFIRE)),
                FocusCombinations.BASE, in -> SpellEffect.Projectile.ember(2 + in.potency(),
                        2 + in.potency(), 15.0F, 20, in.level(FocusUpgrades.ALCHEMISTSFIRE))));

        // ItemFocusShock.java:141-239. The bolt's chain count is 2 * chainlightning level,
        // plus 2 * enlarge level once it chains; the orb's area is 4 + 2 * enlarge and its
        // damage (int)(5 + potency * 1.33) (EntityShockOrb's defaults are 5 and 4).
        out.put("shock", Map.of(
                "chainlightning", in -> new SpellEffect.Bolt(6 + in.potency(), 2 + in.enlarge() * 2,
                        4 + in.potency(), 20.0D, 1.1F),
                "earthshock", in -> SpellEffect.Projectile.shockOrb(4 + in.enlarge() * 2,
                        (int) (5 + in.potency() * 1.33F)),
                FocusCombinations.BASE, in -> new SpellEffect.Bolt(4 + in.potency(), 0,
                        4 + in.potency(), 20.0D, 1.1F)));

        // ItemFocusHellbat.java:85-136. The bat's damBonus is the potency level; the
        // vampire flag is independent of the cost combination.
        out.put("hellbat", Map.of(
                "batbombs", in -> SpellEffect.Projectile.fireBat(in.potency(), true, false,
                        in.has(FocusUpgrades.VAMPIREBATS)),
                "devilbats", in -> SpellEffect.Projectile.fireBat(in.potency(), false, true,
                        in.has(FocusUpgrades.VAMPIREBATS)),
                FocusCombinations.BASE, in -> SpellEffect.Projectile.fireBat(in.potency(), false, false,
                        in.has(FocusUpgrades.VAMPIREBATS))));

        // ItemFocusFrost.java:53-85. EntityFrostShard's defaults are bounce 0.5, bounceLimit 3;
        // the boulder overrides both (0.8 / 6) and the scattershot launches 5 + 2 * potency
        // shards at speed 8.0F with 1.0F damage.
        out.put("frost", Map.of(
                "scattershot", in -> SpellEffect.Projectile.frostShard(5 + in.potency() * 2, 8.0D, 1.0F,
                        true, 0.5F, 3, in.level(FocusUpgrades.ALCHEMISTSFROST)),
                "iceboulder", in -> SpellEffect.Projectile.frostShard(1, 1.0D, 4 + in.potency() * 2,
                        false, 0.8F, 6, in.level(FocusUpgrades.ALCHEMISTSFROST)),
                FocusCombinations.BASE, in -> SpellEffect.Projectile.frostShard(1, 1.0D,
                        3.0F + in.potency() * 1.5F, false, 0.5F, 3,
                        in.level(FocusUpgrades.ALCHEMISTSFROST))));

        // ItemFocusPech.java:64-76 — EntityPechBlast(strength = potency, duration = extend, nightshade).
        out.put("pech", Map.of(
                "nightshade", in -> SpellEffect.Projectile.pechBlast(in.potency(), in.extend(), true),
                FocusCombinations.BASE, in -> SpellEffect.Projectile.pechBlast(in.potency(), in.extend(), false)));

        // ItemFocusPrimal.java:57-67 — EntityPrimalOrb(world, player, seeker).
        out.put("primal", Map.of(
                "seeker", in -> SpellEffect.Projectile.primalOrb(true),
                FocusCombinations.BASE, in -> SpellEffect.Projectile.primalOrb(false)));

        // ItemFocusWarding.java:86-154 — the warded area's original extent is
        // getMaxAreaSize = 3 + enlarge level (the architect upgrade is what makes the
        // Focal area configurable; the walk itself is the warding machinery's).
        out.put("warding", Map.of(
                FocusCombinations.BASE, in -> new SpellEffect.Ward(3 + in.enlarge())));

        // ItemFocusTrade.java:95-165,260-262 — the single-position swap reach is
        // 3 + enlarge level and the architect area is 3 + 2 * enlarge level.
        out.put("trade", Map.of(
                "silktouch", in -> new SpellEffect.Swap(3 + in.enlarge(), 3 + in.enlarge() * 2,
                        in.has(FocusUpgrades.ARCHITECT), true),
                FocusCombinations.BASE, in -> new SpellEffect.Swap(3 + in.enlarge(), 3 + in.enlarge() * 2,
                        in.has(FocusUpgrades.ARCHITECT), false)));

        // ItemFocusExcavation.java:110-227 — the dig speed is 0.05F + 0.1F * potency on
        // stone, 0.25F + 0.25F * potency on the original's soft material set, and a
        // completed dig takes `enlarge` neighbours with it.
        out.put("excavation", Map.of(
                "silktouch", in -> new SpellEffect.Excavate(0.05F + in.potency() * 0.1F,
                        0.25F + in.potency() * 0.25F, true, in.has(FocusUpgrades.DOWSING),
                        in.treasure(), in.enlarge()),
                "dowsing", in -> new SpellEffect.Excavate(0.05F + in.potency() * 0.1F,
                        0.25F + in.potency() * 0.25F, false, true, in.treasure(), in.enlarge()),
                FocusCombinations.BASE, in -> new SpellEffect.Excavate(0.05F + in.potency() * 0.1F,
                        0.25F + in.potency() * 0.25F, false, false, in.treasure(), in.enlarge())));

        // ItemFocusPortableHole.java:98-143 — maxdis 33 + 8 * enlarge, duration
        // 120 + 60 * extend, and a hole `distance + 1` blocks long.
        out.put("portablehole", Map.of(
                FocusCombinations.BASE, in -> new SpellEffect.Hole(in.distance(),
                        FocusSpells.PORTABLE_HOLE_MAX_DISTANCE
                                + in.enlarge() * FocusSpells.PORTABLE_HOLE_ENLARGE_STEP,
                        FocusSpells.PORTABLE_HOLE_DURATION
                                + in.extend() * FocusSpells.PORTABLE_HOLE_EXTEND_STEP,
                        in.side())));

        if (!out.keySet().equals(new java.util.LinkedHashSet<>(FocusCombinations.tags()))) {
            throw new IllegalStateException("every focus needs its combination specs, got " + out.keySet());
        }
        return out;
    }

    /** The combination ids a focus's spec table covers. */
    public static List<String> variantIds(final String tag) {
        final Map<String, SpecFactory> table = BY_TAG.get(tag);
        return table == null ? List.of() : List.copyOf(table.keySet());
    }

    /** Whether a focus/variant pair has an effect. */
    public static boolean has(final String tag, final String variantId) {
        final Map<String, SpecFactory> table = BY_TAG.get(tag);
        return table != null && table.containsKey(variantId);
    }

    /** The effect a combination produces, or null when the pair is not ported. */
    public static SpellEffect spec(final String tag, final FocusCombinations.Combination combination,
                                   final SpellInput input) {
        if (combination == null) {
            return null;
        }
        final Map<String, SpecFactory> table = BY_TAG.get(tag);
        if (table == null) {
            return null;
        }
        final SpecFactory factory = table.get(combination.id());
        return factory == null ? null : factory.spec(input);
    }

    private FocusSpellSpecs() {
    }
}
