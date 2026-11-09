package com.jonnyutah12.thaumcraftclassic.entities.monster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the original Mind Spider world-free (TC-M6-06-s-c3). Every expectation
 * quotes the decompiled line it was read from, in
 * {@code thaumcraft.common.entities.monster.EntityMindSpider} (115 lines,
 * {@code extends EntitySpider}), plus the registration pair at
 * {@code ConfigEntities.java:105-106}.
 *
 * <p>The card's "its behaviour is in the update body at {@code :72} (the
 * jump/attack/poison behaviour)" is corrected here: {@code :72} is the
 * lifespan arm, and the leap / melee / venom ride the vanilla
 * {@code EntitySpider} parent (see {@link #goalSlotsAreTheReconciledSpiderSet()}).</p>
 */
class MindSpiderAiLogicTest {

    private static final float F = 1.0E-4F;
    private static final double D = 1.0E-4;

    @Test
    void registrationArgsAreTheOriginals() {
        // registerModEntityWithEgg(EntityMindSpider.class, "MindSpider", 24,
        // Thaumcraft.instance, 64, 3, true) — ConfigEntities.java:105-106.
        assertEquals(24, MindSpiderAiLogic.MOD_INDEX);
        assertEquals(64, MindSpiderAiLogic.TRACK_RANGE_BLOCKS);
        assertEquals(3, MindSpiderAiLogic.UPDATE_INTERVAL);
        assertTrue(MindSpiderAiLogic.VELOCITY_UPDATES);
    }

    @Test
    void theSpiderIsShrunkInTheConstructorNotByAProperty() {
        // :18 — this.func_70105_a(0.3F, 0.3F): the 1.7.10 resize call, applied
        // to the spider's own box. 1.21.1's box is fixed per EntityType, so the
        // builder takes the same 0.3 x 0.3 pair.
        assertEquals(0.3F, MindSpiderAiLogic.WIDTH, F);
        assertEquals(0.3F, MindSpiderAiLogic.HEIGHT, F);
        // :67-70 — the client's spiderScaleAmount() is the same 0.3
        assertEquals(0.3F, MindSpiderAiLogic.SCALE, F);
    }

    @Test
    void attributesAreBothOneAndXpIsOne() {
        // :19 — field_70728_aV = 1 (the death xp)
        assertEquals(1, MindSpiderAiLogic.XP);
        // :26-30 — maxHealth and attackDamage both set to 1.0; the movement
        // speed is NOT touched, so it stays the parent spider's.
        assertEquals(1.0, MindSpiderAiLogic.MAX_HEALTH, D);
        assertEquals(1.0, MindSpiderAiLogic.ATTACK_DAMAGE, D);
    }

    @Test
    void harmlessModeArmsTheTwoMinuteFuse() {
        // :14 — lifeSpan = Integer.MAX_VALUE for a normal spider (it never
        // expires); :52 — setHarmless(true) re-arms 1200
        assertEquals(Integer.MAX_VALUE, MindSpiderAiLogic.DEFAULT_LIFESPAN);
        assertEquals(1200, MindSpiderAiLogic.HARMLESS_LIFESPAN);
        // :74-76 — !isRemote && ticksExisted > lifeSpan → setDead
        assertFalse(MindSpiderAiLogic.expires(1199, 1200));
        assertFalse(MindSpiderAiLogic.expires(1200, 1200));   // strictly greater
        assertTrue(MindSpiderAiLogic.expires(1201, 1200));
        assertFalse(MindSpiderAiLogic.expires(Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
    }

    @Test
    void harmlessSpidersCannotBePickedAttackedOrRewardXp() {
        // :79-81 — the crosshair reach is 0 in harmless mode
        assertEquals(0.0F, MindSpiderAiLogic.PICK_RADIUS_HARMLESS, F);
        assertEquals(0.1F, MindSpiderAiLogic.PICK_RADIUS_NORMAL, F);
        // :22-24 — func_70693_a: a harmless spider grants no xp
        assertEquals(0, MindSpiderAiLogic.experienceFor(true, 1));
        assertEquals(1, MindSpiderAiLogic.experienceFor(false, 1));
        // :98-102 — func_70785_a (attackEntityAsMob) runs only for a real spider:
        // this is the gate on the parent's bite, i.e. the card's "attack/poison"
        assertFalse(MindSpiderAiLogic.attacks(true));
        assertTrue(MindSpiderAiLogic.attacks(false));
    }

    @Test
    void synchedDataAndNbtKeysAreTheOriginals() {
        // :34 — new Byte((byte)0) at watcher index 22; :35 — a String at 23
        assertEquals(22, MindSpiderAiLogic.DATA_HARMLESS_INDEX);
        assertEquals(23, MindSpiderAiLogic.DATA_VIEWER_INDEX);
        // :104-114 — the two keys the original writes
        assertEquals("harmless", MindSpiderAiLogic.NBT_HARMLESS);
        assertEquals("viewer", MindSpiderAiLogic.NBT_VIEWER);
    }

    @Test
    void soundVolumeAndTargetRangeAreTheHooksOwnNumbers() {
        // :58-60 — func_70647_i() = 0.7F (1.21.1: Mob#getSoundVolume())
        assertEquals(0.7F, MindSpiderAiLogic.SOUND_VOLUME, F);
        // :62-65 — func_70782_k(): the parent's target hook re-pointed at
        // world.getClosestPlayer(this, 12.0) (21.1.259: getNearestPlayer)
        assertEquals(12.0, MindSpiderAiLogic.CLOSEST_PLAYER_RANGE, D);
    }

    @Test
    void goalSlotsAreTheReconciledSpiderSet() {
        // The original adds NO goal of its own (zero addTask lines in its 115
        // lines), so the set is the 1.7.10 EntitySpider parent's. Slots kept:
        // 1 Float, 3 LeapAtTarget(0.4F), 4 SpiderAttack, 5 Stroll(0.8),
        // 6 LookAtPlayer(8.0F), 6 RandomLookAround. The armadillo AvoidEntity
        // goal of 1.21.1 Spider.java:56 is REMOVED (a 1.20.11 mob, absent from
        // 1.7.10) — the probe asserts its absence.
        assertEquals(6, MindSpiderAiLogic.GOAL_SLOTS.length);
        assertEquals(1, MindSpiderAiLogic.GOAL_SLOTS[0]);
        assertEquals(3, MindSpiderAiLogic.GOAL_SLOTS[1]);
        assertEquals(4, MindSpiderAiLogic.GOAL_SLOTS[2]);
        assertEquals(5, MindSpiderAiLogic.GOAL_SLOTS[3]);
        assertEquals(6, MindSpiderAiLogic.GOAL_SLOTS[4]);
        assertEquals(6, MindSpiderAiLogic.GOAL_SLOTS[5]);
        // TARGET: HurtByTarget 1, SpiderTargetGoal(Player) 2,
        // SpiderTargetGoal(IronGolem) 3 — 1.7.10 ordered the hurt goal LAST
        // (slot 3) and had no villager target goal (recorded drift).
        assertEquals(3, MindSpiderAiLogic.TARGET_SLOTS.length);
        assertEquals(1, MindSpiderAiLogic.TARGET_SLOTS[0]);
        assertEquals(2, MindSpiderAiLogic.TARGET_SLOTS[1]);
        assertEquals(3, MindSpiderAiLogic.TARGET_SLOTS[2]);
    }
}
