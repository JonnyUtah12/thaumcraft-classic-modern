package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the shared boss body world-free (TC-M6-06-s-c5-1) — no level, no entity
 * instance, no registry. Every expectation quotes the decompiled line it was read
 * from in {@code thaumcraft.common.entities.monster.boss.EntityThaumcraftBoss}
 * (259 lines) and {@code thaumcraft.common.lib.utils.EntityUtils} — so
 * "cleaning up" one of the original's figures (an attribute, a threshold, an
 * amplifier divisor, a scan interval, the 16384 cap, the +25/1.1 pair, a
 * modifier amount, a clamp) turns a test red. The shipped wiring is proven by
 * {@code BossMobGameTest}; the attributes ride as constants here, as the family
 * ships them (registry bootstrap lives in the game-test lane).
 */
class ThaumcraftBossAiLogicTest {

    private static final float F = 1.0E-4F;

    // ------------------------------------------------------- base figures

    @Test
    void theBaseCarriesTheOriginalExperienceAndAttributeFigures() {
        // field_70728_aV = 50 — EntityThaumcraftBoss.java:31
        assertEquals(50, ThaumcraftBossAiLogic.EXPERIENCE);
        // :53 func_110148_a(field_111266_c).func_111128_a(0.95) — knockback resistance
        assertEquals(0.95, ThaumcraftBossAiLogic.KNOCKBACK_RESISTANCE, 1.0E-9);
        // :54 func_110148_a(field_111265_b).func_111128_a(40.0) — follow range
        assertEquals(40.0, ThaumcraftBossAiLogic.FOLLOW_RANGE, 1.0E-9);
    }

    @Test
    void theWatcherAndTimerFiguresAreTheOriginals() {
        // :59 func_70096_w().addObject(14, Short.MAX_VALUE) — the rage timer index
        assertEquals(14, ThaumcraftBossAiLogic.ANGER_WATCHER_INDEX);
        // :240 setAnger(200) — the rage window
        assertEquals(200, ThaumcraftBossAiLogic.ANGER_RAGE_TICKS);
        // :91-93 the spawn-in timer idles at 0
        assertEquals(0, ThaumcraftBossAiLogic.SPAWN_TIMER_MIN);
        // :72-75 func_110171_b(x, y, z, 24) — the home radius
        assertEquals(24, ThaumcraftBossAiLogic.HOME_RADIUS);
        // :116-118 every 30 ticks heal 1.0F
        assertEquals(30, ThaumcraftBossAiLogic.HEAL_PERIOD_TICKS);
        assertEquals(1.0F, ThaumcraftBossAiLogic.HEAL_AMOUNT, F);
        // :120 the aggro pass runs every 20 ticks
        assertEquals(20, ThaumcraftBossAiLogic.AGGRO_PERIOD_TICKS);
        // :106-107 the chase-cut literal, as written
        assertEquals(224.0, ThaumcraftBossAiLogic.TARGET_DROP_DISTANCE_SQR, 1.0E-9);
        // :132 !(func_70068_e(newTarget) > 16384.0) — 128 blocks, squared
        assertEquals(16384.0, ThaumcraftBossAiLogic.AGGRO_MAX_DISTANCE_SQ, 1.0E-9);
        // :130 ca > ad + 25 && ca > ad * 1.1
        assertEquals(25, ThaumcraftBossAiLogic.AGGRO_MIN_GAIN);
        assertEquals(1.1, ThaumcraftBossAiLogic.AGGRO_RATIO, 1.0E-9);
        // :99-115 the client anger puff rides the 1-in-15 roll
        assertEquals(15, ThaumcraftBossAiLogic.ANGER_PARTICLE_CHANCE);
        assertTrue(ThaumcraftBossAiLogic.angerParticleFires(0));
        assertFalse(ThaumcraftBossAiLogic.angerParticleFires(1));
    }

    // ------------------------------------------------------- rage rule (:234-251)

    @Test
    void theRageGateIsStrictlyOver35AndTheClampIsUnconditional() {
        // :234 damage > 35.0F — 35.0 itself is NOT a rage (float-boundary lesson, 35.0F)
        final ThaumcraftBossAiLogic.Rage at =
                ThaumcraftBossAiLogic.rageFor(35.0F, 0);
        assertFalse(at.enrages());
        assertEquals(35.0F, at.damageAfterClamp(), F);
        // just over — rage, haste (int)(d/15), resist/strength (int)(d/40), clamp 35.0
        final ThaumcraftBossAiLogic.Rage over =
                ThaumcraftBossAiLogic.rageFor(35.01F, 0);
        assertTrue(over.enrages());
        assertEquals(2, over.hasteAmplifier());                      // :237 (int)(35.01/15)=2
        assertEquals(0, over.resistanceAmplifier());                 // :238 (int)(35.01/40)=0
        assertEquals(0, over.strengthAmplifier());                   // :239
        assertEquals(35.0F, over.damageAfterClamp(), F);             // :250 clamp
    }

    @Test
    void anAngryBossStillTakesTheClampButNeverReAppliesTheRage() {
        // :235 getAnger() == 0 gates the effects; :250 clamps every over-threshold hit
        final ThaumcraftBossAiLogic.Rage angry =
                ThaumcraftBossAiLogic.rageFor(100.0F, 50);
        assertFalse(angry.enrages());
        assertEquals(35.0F, angry.damageAfterClamp(), F);
        final ThaumcraftBossAiLogic.Rage fresh =
                ThaumcraftBossAiLogic.rageFor(100.0F, 0);
        assertTrue(fresh.enrages());
        assertEquals(6, fresh.hasteAmplifier());                     // :237 (int)(100/15)=6
        assertEquals(2, fresh.resistanceAmplifier());                // :238 (int)(100/40)=2
        assertEquals(2, fresh.strengthAmplifier());                  // :239
        assertEquals(35.0F, fresh.damageAfterClamp(), F);            // :250
    }

    @Test
    void aHitUnderTheThresholdIsDealtWhole() {
        // :234 the clamp lives inside the over-35 arm only
        final ThaumcraftBossAiLogic.Rage light =
                ThaumcraftBossAiLogic.rageFor(12.5F, 0);
        assertFalse(light.enrages());
        assertEquals(12.5F, light.damageAfterClamp(), F);
    }

    // ------------------------------------------------------- aggro map (:222-232, :120-150)

    @Test
    void aggroSumsRawDamagePerAttackerEntityId() {
        // :226 int ad = (int) damage; :227-229 ad += previous
        final Map<Integer, Integer> aggro = new HashMap<>();
        aggro.put(7, 12);
        assertEquals(12, ThaumcraftBossAiLogic.aggroEntry(aggro, 9, 12.7F));  // (int) 12.7 = 12
        assertEquals(24, ThaumcraftBossAiLogic.aggroEntry(aggro, 7, 12.9F));  // 12 + 12
    }

    @Test
    void theRetargetScanCarriesTheOriginalsOwnQuirksVerbatim() {
        // :131 looks up HEI (the incumbent id), not the candidate ei — with the
        // incumbent absent the candidate is DROPPED even though its score wins.
        final Map<Integer, Integer> aggro = new HashMap<>();
        aggro.put(3, 100);
        final ThaumcraftBossAiLogic.AggroScan dead = ThaumcraftBossAiLogic.scanAggro(
                aggro, 9, 0, id -> null, id -> 0.0, id -> true);
        assertEquals(java.util.List.of(3), dead.dropIds());          // :139
        assertFalse(dead.switched());

        // :133 hei = ei, :134 ld = ei (the entity id, not the score — verbatim).
        // The incumbent "p9" (a stand-in entity) is in range; the candidate 3 scores 100.
        final ThaumcraftBossAiLogic.AggroScan scan = ThaumcraftBossAiLogic.scanAggro(
                aggro, 9, 0, id -> id == 9 ? "p9" : null, id -> 16.0, id -> id.startsWith("p"));
        assertEquals(3, scan.targetId());
        assertTrue(scan.switched());                                 // :148
        assertEquals(1, scan.players());                             // :135-136
        assertTrue(scan.dropIds().isEmpty());
    }

    @Test
    void theScanDropsCandidatesBeyond128Blocks() {
        // :132 func_70068_e(newTarget) > 16384.0
        final Map<Integer, Integer> aggro = new HashMap<>();
        aggro.put(3, 100);
        final ThaumcraftBossAiLogic.AggroScan far = ThaumcraftBossAiLogic.scanAggro(
                aggro, 9, 0, id -> id == 9 ? "p9" : null, id -> 16385.0, id -> true);
        assertEquals(java.util.List.of(3), far.dropIds());
        assertFalse(far.switched());
        // exactly at the cap is inside
        final ThaumcraftBossAiLogic.AggroScan atCap = ThaumcraftBossAiLogic.scanAggro(
                aggro, 9, 0, id -> id == 9 ? "p9" : null, id -> 16384.0, id -> true);
        assertTrue(atCap.dropIds().isEmpty());
    }

    @Test
    void theNonPlayerCandidateNeverCountsTowardThePartyScale() {
        // :135-136 players++ runs only on the Player arm
        final Map<Integer, Integer> aggro = new HashMap<>();
        aggro.put(3, 100);
        final ThaumcraftBossAiLogic.AggroScan mob = ThaumcraftBossAiLogic.scanAggro(
                aggro, 9, 0, id -> id == 9 ? "m9" : null, id -> 16.0, id -> id.startsWith("p"));
        assertEquals(3, mob.targetId());
        assertEquals(0, mob.players());
    }

    // ------------------------------------------------------- player scaling (:152-167, EntityUtils:43-58)

    @Test
    void thePlayerScalingIsFivePlus50HealthAndFivePlusHalfDamage() {
        // EntityUtils.java:43-49 — HPBUFF: five additive +50.0 max-health modifiers
        assertEquals(5, ThaumcraftBossAiLogic.HPBUFF.length);
        for (int a = 0; a < 5; a++) {
            assertEquals(50.0, ThaumcraftBossAiLogic.HPBUFF[a].amount(), 1.0E-9);
            assertEquals(AttributeModifier.Operation.ADD_VALUE,
                    ThaumcraftBossAiLogic.HPBUFF[a].operation());
            assertEquals("health_buff_" + (a + 1),
                    ThaumcraftBossAiLogic.HPBUFF[a].id().getPath());
        }
        // EntityUtils.java:50-56 — DMGBUFF: five additive +0.5 attack-damage modifiers
        assertEquals(5, ThaumcraftBossAiLogic.DMGBUFF.length);
        for (int a = 0; a < 5; a++) {
            assertEquals(0.5, ThaumcraftBossAiLogic.DMGBUFF[a].amount(), 1.0E-9);
            assertEquals(AttributeModifier.Operation.ADD_VALUE,
                    ThaumcraftBossAiLogic.DMGBUFF[a].operation());
            assertEquals("damage_buff_" + (a + 1),
                    ThaumcraftBossAiLogic.DMGBUFF[a].id().getPath());
        }
    }

    @Test
    void theBuffCountIsTheOriginalsMinFivePlayersMinusOne() {
        // :161 Math.min(5, players - 1) — verbatim, so a 0-player scan yields -1
        // and the re-add loop is a no-op (the original's own behaviour).
        assertEquals(-1, ThaumcraftBossAiLogic.buffCount(0));
        assertEquals(0, ThaumcraftBossAiLogic.buffCount(1));
        assertEquals(2, ThaumcraftBossAiLogic.buffCount(3));
        assertEquals(5, ThaumcraftBossAiLogic.buffCount(6));
        assertEquals(5, ThaumcraftBossAiLogic.buffCount(9));
    }

    @Test
    void theCurrentHealthRescalesByTheMaxHealthRatio() {
        // :166-167 health = health * (newMaxHealth / oldMaxHealth)
        assertEquals(140.0F, ThaumcraftBossAiLogic.rescaledHealth(125.0, 175.0, 100.0F), F);
        assertEquals(100.0F, ThaumcraftBossAiLogic.rescaledHealth(125.0, 125.0, 100.0F), F);
    }

    // ------------------------------------------------------- hooks

    @Test
    void theInvulnerabilityAndPushRulesFollowTheSpawnTimer() {
        // :172-173 isInvulnerable = super || spawnTimer > 0
        assertTrue(ThaumcraftBossAiLogic.invulnerableForSpawnTimer(1));
        assertFalse(ThaumcraftBossAiLogic.invulnerableForSpawnTimer(0));
        // :180 func_70104_M() = super && !isInvulnerable()
        assertTrue(ThaumcraftBossAiLogic.pushableForVulnerable(true));
        assertFalse(ThaumcraftBossAiLogic.pushableForVulnerable(false));
    }

    @Test
    void thePickUpFlagAndTheLootGateAreTheOriginals() {
        // :176 func_70648_aU() = true — EntityLivingBase#getCanPickUpLoot
        assertTrue(ThaumcraftBossAiLogic.CAN_PICK_UP_LOOT);
        // :205 func_70692_ba() = false — bosses drop no death loot
        assertFalse(ThaumcraftBossAiLogic.DAPS_LOOT);
    }

    @Test
    void theCustomDropsAreTheEldritchObjectAndTheLootbag() {
        // :214 new ItemStack(ConfigItems.itemEldritchObject, 1, 3) via func_146069_c
        assertEquals("itemeldritchobject", ThaumcraftBossAiLogic.ELDRITCH_OBJECT.getPath());
        assertEquals("thaumcraft", ThaumcraftBossAiLogic.ELDRITCH_OBJECT.getNamespace());
        assertEquals(3, ThaumcraftBossAiLogic.ELDRITCH_OBJECT_META);
        // :215 dropItem(new ItemStack(ConfigItems.itemLootbag, 1, 2), 1.5F)
        assertEquals("itemlootbag", ThaumcraftBossAiLogic.LOOTBAG.getPath());
        assertEquals(2, ThaumcraftBossAiLogic.LOOTBAG_META);
        assertEquals(1.5F, ThaumcraftBossAiLogic.LOOTBAG_CHANCE, F);
    }
}
