package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Eldritch Warden world-free (TC-M6-06-s-c5-4) — no level, no entity
 * instance, no registry, no attribute build (the live pair rides the warden probes
 * of {@code BossMobGameTest}, which run on a bootstrapped server). Every
 * expectation quotes the decompiled line it was read from in
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityEldritchWarden.java}
 * (418 lines), so "cleaning up" one of the original's figures — a goal slot, a
 * constructor argument, the 1.5 x 3.5 box, the 200 / 0.33 / 10 attributes, the
 * armour 4, the 500 experience, the 150-tick spawn, the 66% pair, the frenzy
 * thresholds, the 180/2 sweep, the 0.2 orb roll, the 90/180 muzzles, the 400-tick
 * sonic effects, the 3+3 warp, the 20 teleport tries, the airy-11 write — turns a
 * test red.
 */
class WardenAiLogicTest {

    private static final float F = 1.0E-4F;

    // ------------------------------------------------------- registration (:80; ConfigEntities:109)

    @Test
    void theRegistrationRowIsTheOriginals() {
        // registerModEntity(EntityEldritchWarden.class, "EldritchWarden", 26, instance,
        // 64, 3, true) — ConfigEntities.java:109
        assertEquals(64, WardenAiLogic.TRACK_RANGE_BLOCKS);
        assertEquals(4, WardenAiLogic.TRACK_RANGE_CHUNKS);       // 64 blocks = 4 chunks
        assertEquals(3, WardenAiLogic.UPDATE_INTERVAL);
        assertTrue(WardenAiLogic.VELOCITY_UPDATES);
        // :80 func_70105_a(1.5F, 3.5F)
        assertEquals(1.5F, WardenAiLogic.WIDTH, F);
        assertEquals(3.5F, WardenAiLogic.HEIGHT, F);
        // :415-417 func_70627_aG() = 500 — overwrites the base's 50
        assertEquals(500, WardenAiLogic.EXPERIENCE);
    }

    // ------------------------------------------------------- attributes (:104-106, :127-129, :336-338)

    @Test
    void theAttributesAreTheOriginalsOverTheBossBasePair() {
        assertEquals(200.0, WardenAiLogic.MAX_HEALTH, 1.0E-9);          // :104
        assertEquals(0.33, WardenAiLogic.MOVEMENT_SPEED, 1.0E-9);       // :105
        assertEquals(10.0, WardenAiLogic.ATTACK_DAMAGE, 1.0E-9);        // :106
        assertEquals(4.0, WardenAiLogic.ARMOR_BONUS, 1.0E-9);           // :127-129 super + 4
        assertEquals(3.1F, WardenAiLogic.EYE_HEIGHT, F);                // :336-338 func_70047_e
    }

    // ------------------------------------------------------- titles (:44-60, :84-125)

    @Test
    void theTitleArrayIsTheOriginalsInOrder() {
        assertEquals(15, WardenAiLogic.TITLES.length);                  // :44-60
        assertEquals("Aphoom-Zhah", WardenAiLogic.TITLES[0]);
        assertEquals("Basatan", WardenAiLogic.TITLES[1]);
        assertEquals("Chaugnar Faugn", WardenAiLogic.TITLES[2]);
        assertEquals("Mnomquah", WardenAiLogic.TITLES[3]);
        assertEquals("Nyogtha", WardenAiLogic.TITLES[4]);
        assertEquals("Oorn", WardenAiLogic.TITLES[5]);
        assertEquals("Shaikorth", WardenAiLogic.TITLES[6]);
        assertEquals("Rhan-Tegoth", WardenAiLogic.TITLES[7]);
        assertEquals("Rhogog", WardenAiLogic.TITLES[8]);
        assertEquals("Shudde M'ell", WardenAiLogic.TITLES[9]);
        assertEquals("Vulthoom", WardenAiLogic.TITLES[10]);
        assertEquals("Yag-Kosha", WardenAiLogic.TITLES[11]);
        assertEquals("Yibb-Tstll", WardenAiLogic.TITLES[12]);
        assertEquals("Zathog", WardenAiLogic.TITLES[13]);
        assertEquals("Zushakon", WardenAiLogic.TITLES[14]);
        assertEquals("Zushakon", WardenAiLogic.titleAt(14));            // :93-95
        assertEquals("title", WardenAiLogic.NBT_TITLE);                 // :118
    }

    @Test
    void theChampionTitleNeedsARegisteredAttribute() {
        // :86 — the name fires only when the tc.mobmod attribute reads >= 0. The
        // attribute is unregistered content (the leader's pair), so a fresh warden
        // never gets a name: the shipped answer is the plain label (deviation row).
        assertFalse(WardenAiLogic.championNameApplies(-1));
        assertTrue(WardenAiLogic.championNameApplies(0));
        assertTrue(WardenAiLogic.championNameApplies(7));
    }

    // ------------------------------------------------------- spawn + status (:328-334, :145-149, :382-396)

    @Test
    void theMaterialisationIsTheOriginals() {
        assertEquals(150, WardenAiLogic.SPAWN_TIMER_START_TICKS);       // :330
        assertEquals(150, WardenAiLogic.SPAWN_TIMER_BROADCAST_TICKS);   // :147
        assertEquals(18, WardenAiLogic.STATUS_SPAWN);                   // :148
        assertEquals(0.66, WardenAiLogic.SPAWN_HEAL_FRACTION, 1.0E-9);  // :332
        // :332 — the spawn heal sum (maxHealth 200: 0 + 132)
        assertEquals(132.0F, WardenAiLogic.spawnHealthFor(0.0F, 200.0), F);
        // :145-149 + :391-392 — status 18 is the materialisation BROADCAST, and the
        // timer it re-arms is the 150-tick spawn timer of :330, not the status id
        // (the first assert above pins the id; this one pins the payload).
        assertEquals(WardenAiLogic.SPAWN_TIMER_START_TICKS,
                WardenAiLogic.spawnTimerForStatus(WardenAiLogic.STATUS_SPAWN));
        assertEquals(0, WardenAiLogic.spawnTimerForStatus(WardenAiLogic.STATUS_ARM_BOTH));
    }

    @Test
    void theStatusPairIsTheOriginals() {
        assertEquals(15, WardenAiLogic.STATUS_ARM_LEFT);                // :384-385
        assertEquals(16, WardenAiLogic.STATUS_ARM_RIGHT);               // :386-387
        assertEquals(17, WardenAiLogic.STATUS_ARM_BOTH);                // :388-390
        assertEquals(0.5F, WardenAiLogic.ARM_LIFT_HALF, F);             // :385/:387
        assertEquals(0.9F, WardenAiLogic.ARM_LIFT_FULL, F);             // :389-390
        // :153-159 — the client decay walks to zero and stays
        assertEquals(0.45F, WardenAiLogic.armLiftAfterTick(0.5F), F);
        assertEquals(0.0F, WardenAiLogic.armLiftAfterTick(0.0F), F);
        assertEquals(-0.049F, WardenAiLogic.armLiftAfterTick(0.001F), 1.0E-4F);
    }

    // ------------------------------------------------------- the field frenzy (:200, :208-245, :309-320)

    @Test
    void theFrenzySwapIsTheOriginals() {
        // :317-320 — a landed killing hit with no frenzy yet swaps in the frenzy
        assertTrue(WardenAiLogic.frenzySwaps(true, false, 0.0F));
        assertFalse(WardenAiLogic.frenzySwaps(true, true, 0.0F));      // second kill dies
        assertFalse(WardenAiLogic.frenzySwaps(true, false, 12.0F));    // not a kill
        assertFalse(WardenAiLogic.frenzySwaps(false, false, 0.0F));    // no hit landed
        assertEquals(150, WardenAiLogic.FRENZY_START_TICKS);           // :319
        assertEquals(1.0F, WardenAiLogic.FRENZY_SWAP_HEALTH, F);       // deviation pin
        // :309-310 — invulnerable for the whole window
        assertTrue(WardenAiLogic.invulnerableForFrenzy(150));
        assertTrue(WardenAiLogic.invulnerableForFrenzy(1));
        assertFalse(WardenAiLogic.invulnerableForFrenzy(0));
    }

    @Test
    void theRingSweepIsTheOriginalArithmetic() {
        // :209 — the ring leg runs under 121, every tenth
        assertTrue(WardenAiLogic.broadcastsFieldFrenzy(120));
        assertTrue(WardenAiLogic.broadcastsFieldFrenzy(120 - 10));
        assertFalse(WardenAiLogic.broadcastsFieldFrenzy(121));
        assertFalse(WardenAiLogic.broadcastsFieldFrenzy(119));
        // :211 — radius (150 - counter) / 8.0
        assertEquals(3.75, WardenAiLogic.frenzyRadius(120), 1.0E-9);
        assertEquals(18.75, WardenAiLogic.frenzyRadius(0), 1.0E-9);
        // :212 — the step 1 + counter / 8
        assertEquals(16, WardenAiLogic.frenzyStep(120));
        assertEquals(1, WardenAiLogic.frenzyStep(0));
        // :217-218 — the half-circle bound and the angle
        assertEquals(11, WardenAiLogic.frenzyLoopLimit(16));           // 180 / 16
        assertEquals(0.0, WardenAiLogic.frenzyRadians(0, 16), 1.0E-9);
        assertEquals(Math.PI, WardenAiLogic.frenzyRadians(9, 10), 1.0E-9);
        // :219-220 — the int-truncated deltas
        assertEquals(3, WardenAiLogic.frenzyDeltaX(3.75, 0.0));
        assertEquals(0, WardenAiLogic.frenzyDeltaZ(3.75, 0.0));
        // :223 — the expiry schedule
        assertEquals(250, WardenAiLogic.airyUpdateDelay(0));
        assertEquals(399, WardenAiLogic.airyUpdateDelay(149));
        // :224 — the 0.3 arc roll
        assertTrue(WardenAiLogic.arcFires(0.0F));
        assertFalse(WardenAiLogic.arcFires(0.3F));
        // :190-193 — the four corner cells
        assertEquals(4, WardenAiLogic.CORNER_SCAN_CELLS);
        assertEquals(-0.25F, WardenAiLogic.cornerOffsetX(0), F);
        assertEquals(0.25F, WardenAiLogic.cornerOffsetX(1), F);
        assertEquals(-0.25F, WardenAiLogic.cornerOffsetZ(0), F);
        assertEquals(0.25F, WardenAiLogic.cornerOffsetZ(2), F);
        // :195/:222 — the airy meta
        assertEquals(11, WardenAiLogic.AIRY_PORTAL_META);
    }

    // ------------------------------------------------------- regen (:137-142)

    @Test
    void theRegenIsTheOriginals() {
        // :137 — every 25 ticks, gated by no hurt resistance
        assertTrue(WardenAiLogic.regens(50, 0));
        assertFalse(WardenAiLogic.regens(50, 1));
        assertFalse(WardenAiLogic.regens(51, 0));
        // :138 — the cap (int)(200 * 0.66) = 132
        assertEquals(132, WardenAiLogic.regenCap(200.0));
        assertEquals(1.0F, WardenAiLogic.REGEN_AMOUNT, F);             // :140
    }

    // ------------------------------------------------------- goals (:69-79)

    @Test
    void theGoalSlotsAreTheOriginals() {
        assertEquals(0, WardenAiLogic.GOAL_SLOT_SWIM);                  // :70
        assertEquals(2, WardenAiLogic.GOAL_SLOT_RANGED);                // :71
        assertEquals(3, WardenAiLogic.GOAL_SLOT_MELEE);                 // :72
        assertEquals(5, WardenAiLogic.GOAL_SLOT_RESTRICTION);           // :73
        assertEquals(7, WardenAiLogic.GOAL_SLOT_WANDER);                // :74
        assertEquals(8, WardenAiLogic.GOAL_SLOT_WATCH);                 // :75
        assertEquals(8, WardenAiLogic.GOAL_SLOT_LOOK_IDLE);             // :76
        assertEquals(1, WardenAiLogic.TARGET_SLOT_HURT);                // :77
        assertEquals(2, WardenAiLogic.TARGET_SLOT_PLAYER);              // :78
        assertEquals(3, WardenAiLogic.TARGET_SLOT_CULTIST);             // :79
        // :71 — AILongRangeAttack(this, 3.0, 1.0, 20, 40, 24.0F)
        assertEquals(3.0, WardenAiLogic.RANGED_MIN_DISTANCE, 1.0E-9);
        assertEquals(1.0, WardenAiLogic.RANGED_SPEED, 1.0E-9);
        assertEquals(20, WardenAiLogic.RANGED_INTERVAL_MIN);
        assertEquals(40, WardenAiLogic.RANGED_INTERVAL_MAX);
        assertEquals(24.0F, WardenAiLogic.RANGED_RANGE, F);
        // :72/:73/:74 — 1.1 / false, 0.8, 1.0
        assertEquals(1.1, WardenAiLogic.MELEE_SPEED, 1.0E-9);
        assertFalse(WardenAiLogic.MELEE_LONG_MEMORY);
        assertEquals(0.8, WardenAiLogic.RESTRICTION_SPEED, 1.0E-9);
        assertEquals(1.0, WardenAiLogic.WANDER_SPEED, 1.0E-9);
        // :75 — watch at 8.0 with the family's 0.02 probability
        assertEquals(8.0F, WardenAiLogic.WATCH_PLAYER_RANGE, F);
        assertEquals(0.02F, WardenAiLogic.WATCH_PLAYER_PROBABILITY, F);
        // :78-79 — the trailing true (mustSee)
        assertTrue(WardenAiLogic.TARGET_MUST_SEE);
        // :69 — the door flag
        assertTrue(WardenAiLogic.NAVIGATION_OPENS_DOORS);
    }

    @Test
    void theGoalsTickOnlyWithBothPhasesIdle() {
        // :132-135 gates the goal pass on the frenzy counter and the base's
        // materialisation gate on the spawn timer; the composite is the shipped seam
        assertTrue(WardenAiLogic.goalsMayTick(0, 0));
        assertFalse(WardenAiLogic.goalsMayTick(0, 150));               // :133
        assertFalse(WardenAiLogic.goalsMayTick(150, 0));               // EntityThaumcraftBoss
        assertFalse(WardenAiLogic.goalsMayTick(150, 150));
    }

    // ------------------------------------------------------- damage (:308-326, :403-405)

    @Test
    void theDamageRefusalsAreTheOriginals() {
        // :315 — drown (1.7.10 field_76369_e) and wither (field_82727_n), resolved
        // the way RunicShielding.isBlockedSource resolves the same fields
        assertTrue(WardenAiLogic.refusesSource(false, true, false));
        assertTrue(WardenAiLogic.refusesSource(false, false, true));
        assertTrue(WardenAiLogic.refusesSource(true, false, false));   // :310 frenzy
        assertFalse(WardenAiLogic.refusesSource(false, false, false));
        // :403-405 — the Guardian class is refused as a target
        assertTrue(WardenAiLogic.refusesTargetType(true));
        assertFalse(WardenAiLogic.refusesTargetType(false));
    }

    // ------------------------------------------------------- orb seam (:340-355)

    @Test
    void theOrbSeamIsTheOriginals() {
        // :341 — rand.nextFloat() > 0.2F: 80% of shots fire the orb
        assertTrue(WardenAiLogic.firesOrb(0.21F));
        assertFalse(WardenAiLogic.firesOrb(0.2F));
        assertEquals(90, WardenAiLogic.muzzleDegrees(true));           // :345 right
        assertEquals(180, WardenAiLogic.muzzleDegrees(false));         // :345 left
        assertEquals(0.5, WardenAiLogic.MUZZLE_RADIUS, 1.0E-9);        // :346/:348
        assertEquals(0.13, WardenAiLogic.MUZZLE_Y_DROP, 1.0E-9);       // :347
        assertEquals(1.0F, WardenAiLogic.ORB_SPEED, F);                // :353
        assertEquals(2.0F, WardenAiLogic.ORB_INACCURACY, F);           // :353
        assertEquals(2.0F, WardenAiLogic.ATTACK_SOUND_VOLUME, F);      // :354
        assertEquals(1.0F, WardenAiLogic.SOUND_PITCH_BASE, F);         // :354
        assertEquals(0.1F, WardenAiLogic.SOUND_PITCH_JITTER, F);       // :354
    }

    // ------------------------------------------------------- sonic seam (:356-379)

    @Test
    void theSonicBlastIsTheOriginals() {
        // :362-366 — the yaw knockback, 0.1 up
        assertEquals(0.0, WardenAiLogic.sonicKnockX(0.0F), 1.0E-9);
        assertEquals(1.5, WardenAiLogic.sonicKnockZ(0.0F), 1.0E-9);
        assertEquals(-1.5, WardenAiLogic.sonicKnockX(90.0F), 1.0E-9);
        assertEquals(1.5, WardenAiLogic.SONIC_KNOCK_MAGNITUDE, 1.0E-9);
        assertEquals(0.1, WardenAiLogic.SONIC_KNOCK_UP, 1.0E-9);
        // :369-370 — wither + weakness, 400 ticks amplifier 0 (the fields csv pair)
        assertEquals(400, WardenAiLogic.SONIC_WITHER_TICKS);
        assertEquals(0, WardenAiLogic.SONIC_WITHER_AMPLIFIER);
        assertEquals(400, WardenAiLogic.SONIC_WEAKNESS_TICKS);
        assertEquals(0, WardenAiLogic.SONIC_WEAKNESS_AMPLIFIER);
        // :375 — sticky warp 3 + worldRandom.nextInt(3)
        assertEquals(3, WardenAiLogic.SONIC_WARP_BASE);
        assertEquals(3, WardenAiLogic.SONIC_WARP_JITTER);
        // :378 — the screech at volume 4
        assertEquals(4.0F, WardenAiLogic.SCREECH_VOLUME, F);
    }

    // ------------------------------------------------------- teleport home (:247-306)

    @Test
    void theTeleportSearchIsTheOriginals() {
        assertEquals(20, WardenAiLogic.TELEPORT_TRIES);                 // :264
        assertEquals(8, WardenAiLogic.TELEPORT_JITTER_ROLL);            // :272-273
        assertEquals(7, WardenAiLogic.jitteredCell(5, 3, 1));           // :272
        // :279 — func_70107_b(i + 0.5, j + 0.1, k + 0.5): the accepted cell is
        // re-entered at its CENTRE (0.5 / 0.1 / 0.5). The 8-rolls of the search are
        // TELEPORT_JITTER_ROLL above; there is no 5-cell term in the original.
        assertEquals(0.5, WardenAiLogic.TELEPORT_CENTER_OFFSET, 1.0E-9);      // :279
        assertEquals(0.1, WardenAiLogic.TELEPORT_Y_OFFSET, 1.0E-9);     // :279
        // :269 — solid below, air in the cell
        assertTrue(WardenAiLogic.landingCellAccepts(true, false));
        assertFalse(WardenAiLogic.landingCellAccepts(false, false));
        assertFalse(WardenAiLogic.landingCellAccepts(true, true));
        // :289-300 — the particle count (the FX lane's draw)
        assertEquals(128, WardenAiLogic.TELEPORT_PORTAL_PARTICLE_COUNT);
    }
}
