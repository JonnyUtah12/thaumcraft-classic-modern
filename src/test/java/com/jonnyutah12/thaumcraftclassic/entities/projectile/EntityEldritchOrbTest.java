package com.jonnyutah12.thaumcraftclassic.entities.projectile;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Eldritch Orb world-free (TC-M6-06-s-c4b-1) — the crab's own
 * projectile, 92 decompiled lines in
 * {@code thaumcraft.common.entities.projectile.EntityEldritchOrb}, registered
 * at {@code thaumcraft/common/config/ConfigEntities.java:87} (content-mapping
 * row 275 at this card's base, mod_index 11).
 * Every expectation quotes the line it was read from; the world side (the
 * burst on a live hit, the client event, the weakness cast, the 100-tick
 * despawn) is proven by {@code EldritchCrabGameTest}.
 */
class EntityEldritchOrbTest {

    private static final float T = 1.0E-4F;

    @Test
    void registrationArgsAreTheOriginals() {
        // registerModEntity(EntityEldritchOrb.class, "EldritchOrb", id++,
        // Thaumcraft.instance, 64, 20, true) — ConfigEntities.java:87, the row
        // whose content-mapping mod_index is 11. (intValue: MOD_INDEX is an
        // Integer, and JUnit 5's assertEquals overloads make (int, Integer)
        // ambiguous — the platform's own compile error, not a style choice.)
        assertEquals(11, EntityEldritchOrb.MOD_INDEX.intValue());
        assertEquals(64, EntityEldritchOrb.TRACK_RANGE_BLOCKS);
        // 1.21.1's clientTrackingRange counts CHUNKS (BrainyZombieAiLogicTest:38).
        assertEquals(EntityEldritchOrb.TRACK_RANGE_BLOCKS / 16,
                EntityEldritchOrb.TRACK_RANGE_CHUNKS);
        assertEquals(20, EntityEldritchOrb.UPDATE_INTERVAL);
        // thaumcraft:eldritchorb — content-mapping row 275.
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "eldritchorb"),
                EntityEldritchOrb.TYPE_ID);
        assertFalse(EntityEldritchOrb.FIRE_IMMUNE);
    }

    @Test
    void theOrbIsSmallFloatsAndDiesAtOneHundredTicks() {
        // :24-26 the orb floats (func_70185_h() = 0.0F); :28-33 the 100-tick
        // despawn, and :84 the impact's write. The box: the 92 lines call NO
        // setSize (grepped), so the vanilla EntityThrowable box — 0.25 × 0.25 —
        // is what the original has, and the pin names the source.
        assertTrue(EntityEldritchOrb.SIZE_FROM_VANILLA_CTOR);
        assertEquals(0.25F, EntityEldritchOrb.WIDTH, T);
        assertEquals(0.25F, EntityEldritchOrb.HEIGHT, T);
        assertEquals(0.0, EntityEldritchOrb.GRAVITY, 1.0E-4);
        assertEquals(100, EntityEldritchOrb.LIFETIME_TICKS);
    }

    @Test
    void burstRadiusAndDamageFactorAreTheOriginals() {
        // :65 the AABB inflated by 2.0 for the area scan (the orb's own box, and
        // the swept set excludes the THROWER); :73 the damage is the thrower's
        // attack-damage attribute times 0.666.
        assertEquals(2.0, EntityEldritchOrb.AOE_INFLATE, 1.0E-4);
        assertEquals(0.666F, EntityEldritchOrb.DAMAGE_MULTIPLIER, T);
        assertEquals(0.0F, EntityEldritchOrb.burstDamage(0.0), T);
        assertEquals(6.66F, EntityEldritchOrb.burstDamage(10.0), T);
        // the crab's own 4.0 attack punch through the factor
        assertEquals(2.664F, EntityEldritchOrb.burstDamage(4.0), T);
    }

    @Test
    void theBurstSkipsTheUndeadAndStrikesEverythingElseLiving() {
        // :69 — `instanceof EntityLivingBase && !func_70662_br()`: players
        // included (1.21.1's Player is not a Mob), the undead exempt.
        assertTrue(EntityEldritchOrb.burstStrikes(true, false));
        assertFalse(EntityEldritchOrb.burstStrikes(true, true));
        assertFalse(EntityEldritchOrb.burstStrikes(false, false));
    }

    @Test
    void weaknessIsSixteenTicksOfAmplifierZero() {
        // :76-79 — Potion.field_76437_t (weakness, mapping CSV line 3659) for
        // 160 ticks at amplifier 0, inside the original's own try/catch.
        assertEquals(160, EntityEldritchOrb.WEAKNESS_TICKS);
        assertEquals(0, EntityEldritchOrb.WEAKNESS_AMPLIFIER);
    }

    @Test
    void launchSoundAndBurstEventAreTheOriginals() {
        // :83 the IMPACT fizz at volume 0.5 and pitch 2.6 + (r - r) * 0.8 — the
        // spread is SYMMETRIC, so a one-sided 2.6..3.4 pitch is a defect this
        // pin catches (the 92 lines have no launch sound at all);
        // :85 the client event id 16, whose client half (:35-61) carries the
        // particle payload (30 motes, 0.3 jitter, 8.0 offset scale, 0.3 size,
        // type 5, 0.02 fade);
        // :89-91 func_70053_R() = 0.1F.
        assertEquals(0.5F, EntityEldritchOrb.SOUND_VOLUME, T);
        assertEquals(2.6F, EntityEldritchOrb.SOUND_PITCH_BASE, T);
        assertEquals(0.8F, EntityEldritchOrb.SOUND_PITCH_SPREAD, T);
        assertEquals((byte) 16, EntityEldritchOrb.BURST_EVENT);
        assertEquals(30, EntityEldritchOrb.BURST_PARTICLES);
        assertEquals(0.3F, EntityEldritchOrb.BURST_JITTER, T);
        assertEquals(8.0F, EntityEldritchOrb.BURST_OFFSET_SCALE, T);
        assertEquals(0.3F, EntityEldritchOrb.BURST_SIZE, T);
        assertEquals(5, EntityEldritchOrb.BURST_TYPE);
        assertEquals(0.02F, EntityEldritchOrb.BURST_FADE, T);
        assertEquals(0.1F, EntityEldritchOrb.PICK_RADIUS, T);
    }
}
