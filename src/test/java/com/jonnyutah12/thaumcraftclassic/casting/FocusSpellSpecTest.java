package com.jonnyutah12.thaumcraftclassic.casting;

import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgrades;
import com.jonnyutah12.thaumcraftclassic.foci.FocusCombinations;
import com.jonnyutah12.thaumcraftclassic.foci.FocusUpgradeList;
import com.jonnyutah12.thaumcraftclassic.foci.Focuses;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-03-s acceptance: the effect each focus combination produces, number for
 * number, against the ten original focus classes' bodies
 * ({@code reference/decompiled/.../common/items/wands/foci/}) and the seven
 * projectiles they spawn
 * ({@code common/entities/projectile/}).
 *
 * <p>The spec is a pure function of the wand's upgrade levels, so every
 * arithmetic expression the original wrote can be pinned here:
 * {@code 2 + getFocusPotency} embers, {@code (2 + potency + 0.5F) * 1.5F} beam
 * damage, {@code (int)(5 + potency * 1.33)} shock-orb damage,
 * {@code 120 + 60 * extend} hole duration. The spell levels used are
 * potency 2, enlarge 1, extend 1, treasure 3, distance 4 — chosen so every
 * formula's term is non-zero and a swapped term shows up.</p>
 */
class FocusSpellSpecTest {

    private static final FocusSpellSpecs.SpellInput LEVELS =
            new FocusSpellSpecs.SpellInput(FocusUpgradeList.NONE, 2, 1, 1, 3, 4, 5);

    @Test
    void everyFocusCombinationProducesTheOriginalsEffect() {
        assertEquals(10, FocusCombinations.tags().size());
        for (final String tag : FocusCombinations.tags()) {
            assertNotNull(spec(tag, FocusCombinations.BASE), tag + " has no uncombined effect");
            for (final FocusCombinations.Combination combination : FocusCombinations.variants(tag)) {
                assertNotNull(spec(tag, combination.id()), tag + "/" + combination.id() + " has no effect");
            }
        }
        assertFalse(FocusSpellSpecs.has("nosuchfocus", FocusCombinations.BASE));
        assertEquals(null, FocusSpellSpecs.spec("fire", null, LEVELS));
    }

    @Test
    void theFireCombinationsFollowItemFocusFire() {
        // ItemFocusFire.java:120-127: 2 + potency embers, scatter 15.0F, damage 2 + potency,
        // 20-tick life; the beam narrows the scatter to 0.25F, adds 0.5F before the 1.5F
        // multiplier and lives 30 ticks.
        final SpellEffect.Projectile base = projectile("fire", FocusCombinations.BASE);
        assertEquals(SpellEffect.Projectile.EMBER, base.entityId());
        assertEquals(4, base.count(), "2 + potency(2)");
        assertEquals(4.0F, base.damage());
        assertEquals(15.0F, base.scatter());
        assertEquals(20, base.duration());
        assertFalse(base.firey(), "alchemist's fire is not applied");
        final SpellEffect.Projectile beam = projectile("fire", "firebeam");
        assertEquals(4, beam.count());
        assertEquals(6.75F, beam.damage(), "(2 + 2 + 0.5F) * 1.5F");
        assertEquals(0.25F, beam.scatter());
        assertEquals(30, beam.duration());

        // ItemFocusFire.java:87-91: one EntityExplosiveOrb, strength 1.0F + potency * 0.4F,
        // onFire from the alchemist's fire level (EntityExplosiveOrb.java:14 is the 1.0F).
        final SpellEffect.Projectile ball = projectile("fire", "fireball");
        assertEquals(SpellEffect.Projectile.EXPLOSIVE_ORB, ball.entityId());
        assertEquals(1, ball.count());
        assertEquals(1.8F, ball.strength(), 0.0001F, "1.0F + 2 * 0.4F");
        assertFalse(ball.onFire());

        // The alchemist's fire level rides on the same spec when applied at its second rank.
        final FocusSpellSpecs.SpellInput firey = new FocusSpellSpecs.SpellInput(
                FocusUpgradeList.single(FocusUpgrades.ALCHEMISTSFIRE, 2), 2, 1, 1, 3, 0, 1);
        assertTrue(projectileSpec("fire", FocusCombinations.BASE, firey).firey());
        assertTrue(projectileSpec("fire", "fireball", firey).onFire());
    }

    @Test
    void theShockCombinationsFollowItemFocusShock() {
        // ItemFocusShock.java:196-197: (cl > 0 ? 6 : 4) + potency on the pointed entity, and
        // cl = 2 * chainlightning level + 2 * enlarge level once it chains (lines 196-199).
        final SpellEffect.Bolt base = bolt("shock", FocusCombinations.BASE);
        assertEquals(6.0F, base.damage(), "4 + potency(2)");
        assertEquals(0, base.jumps(), "no chainlightning, no jumps");
        assertEquals(6.0F, base.jumpDamage());
        assertEquals(20.0D, base.range(), "EntityUtils.getPointedEntity(..., 20.0, 1.1F)");
        assertEquals(1.1F, base.padding());

        final SpellEffect.Bolt chain = bolt("shock", "chainlightning");
        assertEquals(8.0F, chain.damage(), "6 + potency(2)");
        assertEquals(4, chain.jumps(), "2 * chain level(1) + 2 * enlarge level(1)");

        // ItemFocusShock.java:119-121: area 4 + 2 * enlarge, damage (int)(5 + potency * 1.33)
        // off EntityShockOrb's 4/5 defaults (EntityShockOrb.java:16-17).
        final SpellEffect.Projectile orb = projectile("shock", "earthshock");
        assertEquals(SpellEffect.Projectile.SHOCK_ORB, orb.entityId());
        assertEquals(6.0F, orb.area(), "4 + 2 * enlarge(1)");
        assertEquals(7.0F, orb.damage(), "(int)(5 + 2 * 1.33)");
    }

    @Test
    void theFrostCombinationsFollowItemFocusFrost() {
        // ItemFocusFrost.java:59-77: 5 + 2 * potency shards at speed 8.0F and damage 1.0F,
        // fragile; the boulder is one shard of 4 + 2 * potency with bounce 0.8 / limit 6;
        // the base shard is 3.0 + 1.5 * potency. EntityFrostShard's defaults are 0.5 / 3.
        final SpellEffect.Projectile scatter = projectile("frost", "scattershot");
        assertEquals(SpellEffect.Projectile.FROST_SHARD, scatter.entityId());
        assertEquals(9, scatter.count(), "5 + 2 * potency(2)");
        assertEquals(8.0D, scatter.speed());
        assertEquals(1.0F, scatter.damage());
        assertTrue(scatter.fragile());

        final SpellEffect.Projectile boulder = projectile("frost", "iceboulder");
        assertEquals(1, boulder.count());
        assertEquals(1.0D, boulder.speed());
        assertEquals(8.0F, boulder.damage(), "4 + 2 * potency(2)");
        assertEquals(0.8F, boulder.bounce());
        assertEquals(6, boulder.bounceLimit());
        assertFalse(boulder.fragile());

        final SpellEffect.Projectile base = projectile("frost", FocusCombinations.BASE);
        assertEquals(6.0F, base.damage(), "3.0F + 1.5F * potency(2)");
        assertEquals(0.5F, base.bounce(), "EntityFrostShard's default");
        assertEquals(3, base.bounceLimit());

        final FocusSpellSpecs.SpellInput frosted = new FocusSpellSpecs.SpellInput(
                FocusUpgradeList.single(FocusUpgrades.ALCHEMISTSFROST, 1), 0, 0, 0, 0, 0, 1);
        assertEquals(1, projectileSpec("frost", FocusCombinations.BASE, frosted).frosty());
    }

    @Test
    void theRemainingCombinationsFollowTheirOriginals() {
        // ItemFocusPech.java:66-68: EntityPechBlast(potency, extend, nightshade).
        final SpellEffect.Projectile pech = projectile("pech", FocusCombinations.BASE);
        assertEquals(SpellEffect.Projectile.PECH_BLAST, pech.entityId());
        assertEquals(2, pech.potency());
        assertEquals(1, pech.extend());
        assertFalse(pech.nightshade());
        assertTrue(projectile("pech", "nightshade").nightshade());

        // ItemFocusPrimal.java:59: EntityPrimalOrb(world, player, seeker).
        assertEquals(SpellEffect.Projectile.PRIMAL_ORB, projectile("primal", FocusCombinations.BASE).entityId());
        assertFalse(projectile("primal", FocusCombinations.BASE).seeker());
        assertTrue(projectile("primal", "seeker").seeker());

        // ItemFocusHellbat.java:105-122: one summoned bat, damBonus = potency, and the three
        // upgrade flags.
        final SpellEffect.Projectile bat = projectile("hellbat", FocusCombinations.BASE);
        assertEquals(SpellEffect.Projectile.FIRE_BAT, bat.entityId());
        assertEquals(2, bat.potency(), "damBonus = getFocusPotency");
        assertFalse(bat.explosive());
        assertFalse(bat.devil());
        assertFalse(bat.vampire());
        assertTrue(projectile("hellbat", "batbombs").explosive());
        assertTrue(projectile("hellbat", "devilbats").devil());
        final FocusSpellSpecs.SpellInput vampire = new FocusSpellSpecs.SpellInput(
                FocusUpgradeList.single(FocusUpgrades.VAMPIREBATS, 5), 0, 0, 0, 0, 0, 1);
        assertTrue(projectileSpec("hellbat", FocusCombinations.BASE, vampire).vampire(),
                "vampirebats is a flag on every hellbat combination, not a price variant");

        // ItemFocusWarding.java:180-182: getMaxAreaSize = 3 + enlarge level.
        final SpellEffect.Ward ward = assertInstanceOf(SpellEffect.Ward.class,
                spec("warding", FocusCombinations.BASE));
        assertEquals(4, ward.areaSize());

        // ItemFocusTrade.java:155,260-262: the single-position reach is 3 + enlarge, the
        // architect area 3 + 2 * enlarge.
        final SpellEffect.Swap swap = assertInstanceOf(SpellEffect.Swap.class,
                spec("trade", FocusCombinations.BASE));
        assertEquals(4, swap.radius());
        assertEquals(5, swap.areaSize());
        assertFalse(swap.silktouch());
        assertTrue(assertInstanceOf(SpellEffect.Swap.class, spec("trade", "silktouch")).silktouch());
        final FocusSpellSpecs.SpellInput architect = new FocusSpellSpecs.SpellInput(
                FocusUpgradeList.single(FocusUpgrades.ARCHITECT, 3), 0, 0, 0, 0, 0, 1);
        assertTrue(assertInstanceOf(SpellEffect.Swap.class, spec("trade", FocusCombinations.BASE, architect))
                .architect());

        // ItemFocusExcavation.java:170-177: 0.05F + 0.1F * potency on stone and
        // 0.25F + 0.25F * potency on the soft set; the dowsing/silktouch flags and the
        // treasure (fortune) level ride along.
        final SpellEffect.Excavate dig = assertInstanceOf(SpellEffect.Excavate.class,
                spec("excavation", FocusCombinations.BASE));
        assertEquals(0.25F, dig.speed(), 0.0001F, "0.05F + 0.1F * 2");
        assertEquals(0.75F, dig.softSpeed(), 0.0001F, "0.25F + 0.25F * 2");
        assertEquals(3, dig.treasure());
        assertEquals(1, dig.enlarge());
        assertFalse(dig.silktouch());
        assertFalse(dig.dowsing());
        assertTrue(assertInstanceOf(SpellEffect.Excavate.class, spec("excavation", "dowsing")).dowsing());
        assertTrue(assertInstanceOf(SpellEffect.Excavate.class, spec("excavation", "silktouch")).silktouch());

        // ItemFocusPortableHole.java:100,141-142: maxdis 33 + 8 * enlarge, duration
        // 120 + 60 * extend, and the hole is `distance + 1` blocks long.
        final SpellEffect.Hole hole = assertInstanceOf(SpellEffect.Hole.class,
                spec("portablehole", FocusCombinations.BASE));
        assertEquals(4, hole.distance(), "the scan's result");
        assertEquals(41, hole.maxDistance(), "33 + 8 * enlarge(1)");
        assertEquals(180, hole.duration(), "120 + 60 * extend(1)");
        assertEquals(5, hole.side(), "the pointed face");
    }

    @Test
    void everyProjectileIsAnInventoriedContentRow() throws IOException {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        assertTrue(Files.isRegularFile(csv), "content-mapping.csv not wired — run tests through ./gradlew test");
        final Set<String> inventory = new HashSet<>();
        for (final ContentMappingCsv.Row row : ContentMappingCsv.read(csv)) {
            inventory.add(row.newId());
        }
        final Set<String> ids = new HashSet<>();
        for (final String tag : Focuses.tags()) {
            final FocusSpellSpecs.SpellInput levels = new FocusSpellSpecs.SpellInput(
                    FocusUpgradeList.NONE, 0, 0, 0, 0, 0, 1);
            for (final FocusCombinations.Combination combination : FocusCombinations.variants(tag)) {
                final SpellEffect effect = FocusSpellSpecs.spec(tag, combination, levels);
                if (effect instanceof SpellEffect.Projectile projectile) {
                    ids.add(projectile.entityId());
                    assertTrue(inventory.contains(projectile.entityId()),
                            projectile.entityId() + " is not an entity row of docs/content-mapping.csv");
                }
            }
        }
        assertEquals(7, ids.size(), "the foci use the seven Thaumcraft projectiles: " + ids);
    }

    // ------------------------------------------------------------------ helpers

    private static SpellEffect spec(final String tag, final String variantId) {
        return spec(tag, variantId, LEVELS);
    }

    private static SpellEffect spec(final String tag, final String variantId,
                                    final FocusSpellSpecs.SpellInput input) {
        return FocusSpellSpecs.spec(tag,
                FocusCombinations.variants(tag).stream()
                        .filter(combination -> combination.id().equals(variantId)).findFirst().orElse(null),
                input);
    }

    private static SpellEffect.Projectile projectile(final String tag, final String variantId) {
        return assertInstanceOf(SpellEffect.Projectile.class, spec(tag, variantId));
    }

    private static SpellEffect.Projectile projectileSpec(final String tag, final String variantId,
                                                        final FocusSpellSpecs.SpellInput input) {
        return assertInstanceOf(SpellEffect.Projectile.class, spec(tag, variantId, input));
    }

    private static SpellEffect.Bolt bolt(final String tag, final String variantId) {
        return assertInstanceOf(SpellEffect.Bolt.class, spec(tag, variantId));
    }
}
