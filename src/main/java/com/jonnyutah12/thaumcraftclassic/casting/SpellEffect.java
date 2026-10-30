package com.jonnyutah12.thaumcraftclassic.casting;

/**
 * What one focus body does when it fires (TC-M6-03-s): the port of the object
 * the original's {@code onFocusRightClick}/{@code onUsingFocusTick} handed the
 * world — a projectile with its parameters set, a lightning bolt, or the
 * block-shaped effect of the architecture foci.
 *
 * <p>The original built these inline, mutating a live entity
 * ({@code EntityEmber orb = new EntityEmber(...); orb.damage = 2 + potency; …}).
 * The port separates <em>deciding</em> the effect from <em>performing</em> it:
 * a behaviour computes a {@code SpellEffect} (pure, so the numbers the original
 * used are unit-testable) and {@code SpellEffects} performs it. Every value in
 * these records is transcribed from the original line cited on the factory.</p>
 *
 * <p>{@link #key()} is the seam id the effect is performed through: the
 * projectile's content-mapping.csv entity id, or one of the four structural
 * keys ({@code bolt} is performed by the port itself — it is plain damage, not
 * content).</p>
 */
public sealed interface SpellEffect
        permits SpellEffect.Projectile, SpellEffect.Bolt, SpellEffect.Ward,
                SpellEffect.Swap, SpellEffect.Excavate, SpellEffect.Hole {

    /** The seam id this effect is performed through. */
    String key();

    /**
     * A projectile the wand spawns. One record carries every knob the original
     * set on the seven Thaumcraft projectiles the foci use; a knob a given
     * projectile does not have stays at its zero value and is documented on the
     * factory that sets it.
     *
     * @param entityId  the content-mapping.csv entity id the original class registers as
     * @param count     how many projectiles the body spawns
     * @param damage    the projectile's damage ({@code EntityEmber.damage}, {@code EntityShockOrb.damage}, …)
     * @param scatter   the aim scatter in degrees ({@code EntityEmber(level, player, scatter)})
     * @param duration  the projectile's life in ticks ({@code EntityEmber.duration})
     * @param speed     the launch speed ({@code EntityFrostShard(level, player, speed)})
     * @param fragile   {@code EntityFrostShard.fragile}
     * @param bounce    {@code EntityFrostShard.bounce} (0.5 default, set by the ice boulder)
     * @param bounceLimit {@code EntityFrostShard.bounceLimit} (3 default)
     * @param firey     {@code EntityEmber.firey} — the alchemist's fire level
     * @param frosty    {@code EntityFrostShard.setFrosty} — the alchemist's frost level
     * @param strength  {@code EntityExplosiveOrb.strength} (1.0F default)
     * @param onFire    {@code EntityExplosiveOrb.onFire}
     * @param area      {@code EntityShockOrb.area} (4 default)
     * @param seeker    {@code EntityPrimalOrb} seeker flag
     * @param potency   {@code EntityPechBlast.strength} / the firebat's {@code damBonus}
     * @param extend    {@code EntityPechBlast.duration} — the focus's extend level
     * @param nightshade {@code EntityPechBlast.nightshade}
     * @param explosive {@code EntityFireBat.setIsExplosive}
     * @param devil     {@code EntityFireBat.setIsDevil}
     * @param vampire   {@code EntityFireBat.setIsVampire}
     */
    record Projectile(String entityId, int count, float damage, float scatter, int duration, double speed,
                      boolean fragile, float bounce, int bounceLimit, boolean firey, int frosty,
                      float strength, boolean onFire, float area, boolean seeker, int potency, int extend,
                      boolean nightshade, boolean explosive, boolean devil, boolean vampire)
            implements SpellEffect {

        /** {@code thaumcraft:ember} (EntityEmber, mod_index 16). */
        public static final String EMBER = "thaumcraft:ember";
        /** {@code thaumcraft:explosiveorb} (EntityExplosiveOrb, mod_index 15). */
        public static final String EXPLOSIVE_ORB = "thaumcraft:explosiveorb";
        /** {@code thaumcraft:shockorb} (EntityShockOrb, mod_index 14). */
        public static final String SHOCK_ORB = "thaumcraft:shockorb";
        /** {@code thaumcraft:frostshard} (EntityFrostShard, mod_index 7). */
        public static final String FROST_SHARD = "thaumcraft:frostshard";
        /** {@code thaumcraft:pechblast} (EntityPechBlast, mod_index 10). */
        public static final String PECH_BLAST = "thaumcraft:pechblast";
        /** {@code thaumcraft:primalorb} (EntityPrimalOrb, mod_index 6). */
        public static final String PRIMAL_ORB = "thaumcraft:primalorb";
        /** {@code thaumcraft:firebat} (EntityFireBat, mod_index 22). */
        public static final String FIRE_BAT = "thaumcraft:firebat";

        @Override
        public String key() {
            return entityId;
        }

        /** {@code ItemFocusFire.onUsingFocusTick} (lines 117-135): {@code 2 + potency} embers. */
        public static Projectile ember(final int count, final float damage, final float scatter,
                                       final int duration, final int firey) {
            return new Projectile(EMBER, count, damage, scatter, duration, 0.0D, false, 0.0F, 0,
                    firey > 0, firey, 0.0F, false, 0.0F, false, 0, 0, false, false, false, false);
        }

        /** {@code ItemFocusFire.onFocusRightClick} fireball branch (lines 87-91). */
        public static Projectile explosiveOrb(final float strength, final boolean onFire) {
            return new Projectile(EXPLOSIVE_ORB, 1, 0.0F, 0.0F, 0, 0.0D, false, 0.0F, 0,
                    false, 0, strength, onFire, 0.0F, false, 0, 0, false, false, false, false);
        }

        /** {@code ItemFocusShock.onFocusRightClick} earthshock branch (lines 119-123). */
        public static Projectile shockOrb(final float area, final float damage) {
            return new Projectile(SHOCK_ORB, 1, damage, 0.0F, 0, 0.0D, false, 0.0F, 0,
                    false, 0, 0.0F, false, area, false, 0, 0, false, false, false, false);
        }

        /** {@code ItemFocusFrost.onFocusRightClick} — all three frost branches (lines 56-78). */
        public static Projectile frostShard(final int count, final double speed, final float damage,
                                           final boolean fragile, final float bounce, final int bounceLimit,
                                           final int frosty) {
            return new Projectile(FROST_SHARD, count, damage, 0.0F, 0, speed, fragile, bounce, bounceLimit,
                    false, frosty, 0.0F, false, 0.0F, false, 0, 0, false, false, false, false);
        }

        /** {@code ItemFocusPech.onFocusRightClick} (lines 66-68). */
        public static Projectile pechBlast(final int potency, final int extend, final boolean nightshade) {
            return new Projectile(PECH_BLAST, 1, 0.0F, 0.0F, 0, 0.0D, false, 0.0F, 0,
                    false, 0, 0.0F, false, 0.0F, false, potency, extend, nightshade, false, false, false);
        }

        /** {@code ItemFocusPrimal.onFocusRightClick} (line 59). */
        public static Projectile primalOrb(final boolean seeker) {
            return new Projectile(PRIMAL_ORB, 1, 0.0F, 0.0F, 0, 0.0D, false, 0.0F, 0,
                    false, 0, 0.0F, false, 0.0F, seeker, 0, 0, false, false, false, false);
        }

        /** {@code ItemFocusHellbat.onFocusRightClick} (lines 105-122). */
        public static Projectile fireBat(final int potency, final boolean explosive, final boolean devil,
                                        final boolean vampire) {
            return new Projectile(FIRE_BAT, 1, 0.0F, 0.0F, 0, 0.0D, false, 0.0F, 0,
                    false, 0, 0.0F, false, 0.0F, false, potency, 0, false, explosive, devil, vampire);
        }
    }

    /**
     * The shock focus's lightning bolt — the one original effect that is plain
     * damage rather than content, so the port performs it itself
     * ({@code ItemFocusShock.doLightningBolt}, lines 146-236).
     *
     * @param damage     the damage the pointed entity takes ({@code (chain > 0 ? 6 : 4) + potency})
     * @param jumps      the chain jumps ({@code 2 * chainlightning level}, plus
     *                   {@code 2 * enlarge level} once chaining — 0 when uncombined)
     * @param jumpDamage what each chained target takes ({@code 4 + potency})
     * @param range      the ray length ({@code EntityUtils.getPointedEntity(…, 20.0, 1.1F)})
     * @param padding    the ray's entity padding ({@code 1.1F})
     */
    record Bolt(float damage, int jumps, int jumpDamage, double range, float padding) implements SpellEffect {

        /** The seam id the port performs itself. */
        public static final String KEY = "bolt";

        @Override
        public String key() {
            return KEY;
        }
    }

    /**
     * The warding focus's warded blocks ({@code ItemFocusWarding.onFocusRightClick},
     * lines 95-127): the blocks the architect walk collected, each becoming a
     * {@code blockWarded} tile the focus cost is paid for.
     *
     * @param areaSize the original {@code getMaxAreaSize} ({@code 3 + enlarge level})
     */
    record Ward(int areaSize) implements SpellEffect {

        /** The seam id the warded-block machinery (TC-M5/M6 warding card) fills. */
        public static final String KEY = "ward";

        @Override
        public String key() {
            return KEY;
        }
    }

    /**
     * The equal-trade focus's block swap ({@code ItemFocusTrade.onFocusRightClick},
     * lines 123-160): sneak-pick the target block, then swap the picked block in
     * over the architect area or a single position.
     *
     * @param radius    the non-architect swap reach the original passed to
     *                  {@code ServerTickEventsFML.addSwapper} ({@code 3 + enlarge level})
     * @param areaSize  {@code getMaxAreaSize} with architect ({@code 3 + 2 * enlarge level})
     * @param architect whether the architect upgrade is applied
     * @param silktouch whether the silktouch upgrade is applied (drop the block as-is)
     */
    record Swap(int radius, int areaSize, boolean architect, boolean silktouch) implements SpellEffect {

        /** The seam id the block-swap machinery fills. */
        public static final String KEY = "swap";

        @Override
        public String key() {
            return KEY;
        }
    }

    /**
     * The excavation focus's dig ({@code ItemFocusExcavation.onUsingFocusTick},
     * lines 163-226): the per-tick break progress on the pointed block.
     *
     * @param speed     the break progress per tick ({@code 0.05F + 0.1F * potency})
     * @param softSpeed the progress for the original's soft material set
     *                  ({@code 0.25F + 0.25F * potency})
     * @param silktouch whether the silktouch upgrade is applied
     * @param dowsing   whether the dowsing upgrade is applied
     * @param treasure  the treasure (fortune/looting) level the drops use
     * @param enlarge   the enlarge level — how many neighbours a completed dig takes
     */
    record Excavate(float speed, float softSpeed, boolean silktouch, boolean dowsing, int treasure,
                    int enlarge) implements SpellEffect {

        /** The seam id the excavation/break machinery fills. */
        public static final String KEY = "excavate";

        @Override
        public String key() {
            return KEY;
        }
    }

    /**
     * The portable-hole focus's hole ({@code ItemFocusPortableHole.onFocusRightClick},
     * lines 82-151): a straight hole of {@code distance + 1} blocks along the
     * pointed face, lasting {@code 120 + 60 * extend} ticks, priced at
     * {@code distance + 1} times the base cost.
     *
     * @param distance   the blocks the scan walked before hitting something it cannot
     *                   pass ({@code createHole(…, (byte) (distance + 1), duration)})
     * @param maxDistance the scan bound ({@code 33 + 8 * enlarge level})
     * @param duration   {@code 120 + 60 * extend level} ticks
     * @param side       the pointed face ({@code mop.sideHit})
     */
    record Hole(int distance, int maxDistance, int duration, int side) implements SpellEffect {

        /** The seam id the {@code blockHole}/TileHole machinery fills. */
        public static final String KEY = "hole";

        @Override
        public String key() {
            return KEY;
        }
    }
}
