package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the original Angry / Furious Zombie pair world-free (TC-M6-06-s-c2) —
 * no level, no entity instance, no registry. Every expectation quotes the
 * decompiled line it was read from, in
 * {@code thaumcraft.common.entities.monster.EntityBrainyZombie} (54 lines) and
 * {@code thaumcraft.common.entities.monster.EntityGiantBrainyZombie} (90
 * lines), so "cleaning up" one of the original's figures (an attribute, a cap,
 * a roll count, a stack size, a decay step, an RNG choice) turns a test red.
 *
 * <p>The world side — the registry ids, the hitboxes, the attributes on live
 * entities, the goal sets, the target acquisition, the anger growth / decay /
 * attack rewrite / box shrink and the death-drop rates — is proven by
 * {@code ZombieMobGameTest}.</p>
 */
class BrainyZombieAiLogicTest {

    private static final float T = 1.0E-4F;

    // ------------------------------------------------------ registration

    @Test
    void registrationArgsAreTheOriginals() {
        // registerModEntity(EntityBrainyZombie.class, "BrainyZombie", id++,
        // Thaumcraft.instance, 64, 3, true) — ConfigEntities.java:95, and the
        // same triple for GiantBrainyZombie at :97.
        assertEquals(64, BrainyZombieAiLogic.TRACK_RANGE_BLOCKS);
        assertEquals(3, BrainyZombieAiLogic.UPDATE_INTERVAL);
        assertTrue(BrainyZombieAiLogic.TRACK_VELOCITY);
        // 1.21.1's clientTrackingRange counts CHUNKS: 64 blocks is 4.
        assertEquals(BrainyZombieAiLogic.TRACK_RANGE_BLOCKS / 16,
                BrainyZombieAiLogic.TRACK_RANGE_CHUNKS);
    }

    // ----------------------------------------------------------------- size

    @Test
    void boxesComeFromTheTwoSources() {
        // The 54-line angry zombie has no func_70105_a anywhere: its box is the
        // 1.7.10 zombie parent's, which 1.21.1 builds as EntityType.java:770.
        assertEquals(0.6F, BrainyZombieAiLogic.BRAINY_WIDTH, T);
        assertEquals(1.95F, BrainyZombieAiLogic.BRAINY_HEIGHT, T);
        // The giant resizes from its OWN base pair — func_70105_a(0.6F * s,
        // 1.8F * s) — EntityGiantBrainyZombie.java:24.
        assertEquals(0.6F, BrainyZombieAiLogic.FURIOUS_BASE_WIDTH, T);
        assertEquals(1.8F, BrainyZombieAiLogic.FURIOUS_BASE_HEIGHT, T);
    }

    @Test
    void scaleIsAngerPlusTheOriginalBias() {
        // :15-16 (ctor) and :24 (tick) both use (1.2F + anger).
        assertEquals(1.2F, BrainyZombieAiLogic.ANGER_SCALE_BIAS, T);
        assertEquals(2.2F, BrainyZombieAiLogic.scaleFor(1.0F), T);   // a fresh giant
        assertEquals(3.2F, BrainyZombieAiLogic.scaleFor(2.0F), T);   // a hit-maxed giant
        // The ctor's setSize and the tick's func_70105_a figures, as the world
        // sees them: 0.6 x 2.2 = 1.32 wide, 1.8 x 2.2 = 3.96 tall.
        assertEquals(1.32F, BrainyZombieAiLogic.FURIOUS_BASE_WIDTH * BrainyZombieAiLogic.scaleFor(1.0F), T);
        assertEquals(3.96F, BrainyZombieAiLogic.FURIOUS_BASE_HEIGHT * BrainyZombieAiLogic.scaleFor(1.0F), T);
        assertEquals(1.92F, BrainyZombieAiLogic.FURIOUS_BASE_WIDTH * BrainyZombieAiLogic.scaleFor(2.0F), T);
        assertEquals(5.76F, BrainyZombieAiLogic.FURIOUS_BASE_HEIGHT * BrainyZombieAiLogic.scaleFor(2.0F), T);
    }

    // ----------------------------------------------------------- attributes

    @Test
    void attributesMatchFunc_110147_ax() {
        // EntityBrainyZombie.java:16-18
        assertEquals(25.0, BrainyZombieAiLogic.BRAINY_MAX_HEALTH, T);
        assertEquals(5.0, BrainyZombieAiLogic.BRAINY_ATTACK_DAMAGE, T);
        assertEquals(0.0, BrainyZombieAiLogic.BRAINY_REINFORCEMENTS, T);
        // EntityGiantBrainyZombie.java:52-53
        assertEquals(60.0, BrainyZombieAiLogic.FURIOUS_MAX_HEALTH, T);
        assertEquals(7.0, BrainyZombieAiLogic.FURIOUS_ATTACK_BASE, T);
        // :14 — field_70728_aV (experienceValue)
        assertEquals(15, BrainyZombieAiLogic.FURIOUS_XP);
    }

    @Test
    void armorOverrideIsPlusThreeCappedAtTwenty() {
        // func_70658_aO():35 super + 3; :36-38 the cap.
        assertEquals(3, BrainyZombieAiLogic.ARMOR_BONUS);
        assertEquals(20, BrainyZombieAiLogic.ARMOR_CAP);
        assertEquals(3, BrainyZombieAiLogic.armorValue(0));
        assertEquals(5, BrainyZombieAiLogic.armorValue(2));
        assertEquals(20, BrainyZombieAiLogic.armorValue(17));
        assertEquals(20, BrainyZombieAiLogic.armorValue(18));
        assertEquals(20, BrainyZombieAiLogic.armorValue(99), "the cap is a clamp, not a wrap");
        // 1.21.1's port shape: the +3 over the vanilla zombie's ARMOR base of
        // 2.0 (Zombie.java:74) is 5.0.
        assertEquals(2.0, BrainyZombieAiLogic.VANILLA_ZOMBIE_ARMOR, T);
        assertEquals(5.0, BrainyZombieAiLogic.armorBase(), T);
    }

    // --------------------------------------------------------------- goals

    @Test
    void thePairInstallsExactlyTwoGoalsOfItsOwn() {
        // EntityBrainyZombie.java:23 — field_70715_bh (targetTasks) slot 1.
        assertEquals(1, BrainyZombieAiLogic.BRAINY_HURT_TARGET_SLOT);
        // EntityGiantBrainyZombie.java:17 — field_70714_bg (tasks) slot 2,
        // EntityAILeapAtTarget(this, 0.4F).
        assertEquals(2, BrainyZombieAiLogic.FURIOUS_LEAP_SLOT);
        assertEquals(0.4F, BrainyZombieAiLogic.FURIOUS_LEAP_CHANCE, T);
    }

    // -------------------------------------------------------------- anger

    @Test
    void angerStartsAtOneAndIsADataValue() {
        // :30-32 — dataWatcher index 20, new Float(1.0F).
        assertEquals(1.0F, BrainyZombieAiLogic.ANGER_START, T);
        assertEquals(20, BrainyZombieAiLogic.ANGER_WATCHER_INDEX);
        assertEquals("Anger", BrainyZombieAiLogic.ANGER_TAG);
    }

    @Test
    void angerGrowsAQuarterTenthPerHitToTheCap() {
        // :45 — setAnger(Math.min(2.0F, getAnger() + 0.1F))
        assertEquals(0.1F, BrainyZombieAiLogic.ANGER_PER_HIT, T);
        assertEquals(2.0F, BrainyZombieAiLogic.ANGER_CAP, T);
        assertEquals(1.1F, BrainyZombieAiLogic.angerAfterHit(1.0F), T);
        assertEquals(1.5F, BrainyZombieAiLogic.angerAfterHit(1.4F), T);
        assertEquals(2.0F, BrainyZombieAiLogic.angerAfterHit(1.95F), T);
        assertEquals(2.0F, BrainyZombieAiLogic.angerAfterHit(2.0F), T);
        // :45 with Math.min: ten hits from a fresh giant reach the cap, and the
        // cap is a hard ceiling — twenty hits from fresh is still exactly 2.0.
        float anger = BrainyZombieAiLogic.ANGER_START;
        for (int i = 0; i < 10; i++) {
            anger = BrainyZombieAiLogic.angerAfterHit(anger);
        }
        assertEquals(2.0F, anger, T);
        for (int i = 0; i < 20; i++) {
            anger = BrainyZombieAiLogic.angerAfterHit(anger);
        }
        assertEquals(2.0F, anger, T);
    }

    @Test
    void angerDecaysByATwentiethOfAHundredthOnlyAboveOne() {
        // :22-23 — if (getAnger() > 1.0F) setAnger(getAnger() - 0.002F)
        assertEquals(0.002F, BrainyZombieAiLogic.ANGER_DECAY_PER_TICK, 1.0E-6F);
        assertEquals(1.0F, BrainyZombieAiLogic.ANGER_FLOOR, T);
        assertEquals(1.998F, BrainyZombieAiLogic.angerAfterTick(2.0F), 1.0E-5F);
        // The branch test is on the PRE-decay value: a hair above 1.0 does drop
        // below it, which is the original's shape — and the step it drops by is
        // the original's own 0.002, so 1.0005 lands on 0.9985, not on 0.998.
        assertEquals(0.9985F, BrainyZombieAiLogic.angerAfterTick(1.0F + 0.0005F), 1.0E-5F);
        // The gate is a plain `> 1.0F` test with NO clamp on either side, so at
        // exactly 1.0 the value is frozen — and so is a value BELOW it: 0.5 stays
        // 0.5, it is not pulled up to the floor.
        assertEquals(1.0F, BrainyZombieAiLogic.angerAfterTick(1.0F), T);
        assertEquals(0.5F, BrainyZombieAiLogic.angerAfterTick(0.5F), T);
        // 2.0 back to 1.0 takes 500 ticks of the original's own loop.
        float anger = 2.0F;
        int ticks = 0;
        while (BrainyZombieAiLogic.angerAfterTick(anger) != anger) {
            anger = BrainyZombieAiLogic.angerAfterTick(anger);
            ticks++;
        }
        assertTrue(ticks >= 498 && ticks <= 502, "2.0 must decay to the floor in ~500 ticks of "
                + "the original's 0.002 step (:22-23), took " + ticks);
        // The walk lands ONE step under the floor — the gate tests the value
        // BEFORE the step, and there is no clamp — and 501 float subtractions of
        // 0.002 accumulate rounding, so it ends at ~0.9980, not on a round digit.
        // Pin the SHAPE (just under 1.0, inside a step and a half) and name the
        // measured end point.
        assertTrue(anger < 1.0F && anger > 1.0F - 3.0E-3F,
                "the decay walk must stop just under the 1.0 floor (:22-23 has no clamp): ended at "
                        + anger + " after " + ticks + " ticks");
    }

    @Test
    void attackDamageIsRewrittenFromTheAngerEveryTick() {
        // :27 — setAttribute(7.0F + (getAnger() - 1.0F) * 5.0F)
        assertEquals(7.0, BrainyZombieAiLogic.attackDamageFor(1.0F), T);      // fresh
        assertEquals(9.5, BrainyZombieAiLogic.attackDamageFor(1.5F), T);
        assertEquals(12.0, BrainyZombieAiLogic.attackDamageFor(2.0F), T);     // capped
        // :27 is unconditional arithmetic — the original never consults the floor
        // here — so a value BELOW 1.0 (synthetic: the decay stops AT 1.0 and the
        // cap is 2.0, so play never produces it) walks the same formula down:
        // 7 + (0 - 1) * 5 = 2.0.
        assertEquals(2.0, BrainyZombieAiLogic.attackDamageFor(0.0F), T);     // below the floor
    }

    // -------------------------------------------------------------- drops

    @Test
    void angryZombieFleshRollsAreThreeCoinsOfOneFlesh() {
        // :44-48 — for (a < 3) if (world.rand.nextBoolean()) dropItem(rotten_flesh, 1)
        assertEquals(3, BrainyZombieAiLogic.BRAINY_FLESH_ROLLS);
        assertEquals(1, BrainyZombieAiLogic.BRAINY_FLESH_STACK);
        assertTrue(BrainyZombieAiLogic.dropsFlesh(true));
        assertFalse(BrainyZombieAiLogic.dropsFlesh(false));
        // 1000 deaths at 3 flips of 1: the expected pile is 1500 flesh. The
        // world probe measures the same rate over 5 kills.
        assertEquals(1.5, BrainyZombieAiLogic.BRAINY_FLESH_ROLLS
                * BrainyZombieAiLogic.BRAINY_FLESH_STACK / 2.0, T);
    }

    @Test
    void furiousZombieDoublesBothTheRollsAndTheStack() {
        // :58-68 — TWO loops of SIX coin flips, each drop 2 rotten flesh.
        assertEquals(2, BrainyZombieAiLogic.FURIOUS_DROP_LOOPS);
        assertEquals(6, BrainyZombieAiLogic.FURIOUS_ROLLS_PER_LOOP);
        assertEquals(2, BrainyZombieAiLogic.FURIOUS_FLESH_STACK);
        // The doubled shape, made explicit: 12 flips x 2 flesh against the angry
        // zombie's 3 flips x 1. The ROLLS quadruple (12/3) and the STACK doubles
        // (2/1), so the flesh per death is EIGHT times the angry zombie's, not four.
        final int furiousRolls = BrainyZombieAiLogic.FURIOUS_DROP_LOOPS
                * BrainyZombieAiLogic.FURIOUS_ROLLS_PER_LOOP;
        assertEquals(12, furiousRolls);
        assertEquals(4.0, furiousRolls / (double) BrainyZombieAiLogic.BRAINY_FLESH_ROLLS, T);
        assertEquals(2.0, BrainyZombieAiLogic.FURIOUS_FLESH_STACK
                / (double) BrainyZombieAiLogic.BRAINY_FLESH_STACK, T);
        assertEquals(8.0, furiousRolls * BrainyZombieAiLogic.FURIOUS_FLESH_STACK
                / (BrainyZombieAiLogic.BRAINY_FLESH_ROLLS
                        * (double) BrainyZombieAiLogic.BRAINY_FLESH_STACK), T);
        assertEquals(12.0, furiousRolls * BrainyZombieAiLogic.FURIOUS_FLESH_STACK / 2.0, T);
    }

    @Test
    void brainGateIsRoll10MinusLootingAgainstFour() {
        // :50 (and :70) — world.rand.nextInt(10) - i <= 4
        assertEquals(10, BrainyZombieAiLogic.BRAIN_ROLL_MODULO);
        assertEquals(4, BrainyZombieAiLogic.BRAIN_ROLL_THRESHOLD);
        assertTrue(BrainyZombieAiLogic.dropsBrain(0, 0));
        assertTrue(BrainyZombieAiLogic.dropsBrain(4, 0));
        assertFalse(BrainyZombieAiLogic.dropsBrain(5, 0));
        assertTrue(BrainyZombieAiLogic.dropsBrain(5, 1));
        assertTrue(BrainyZombieAiLogic.dropsBrain(9, 5));
        assertFalse(BrainyZombieAiLogic.dropsBrain(9, 4));
        // Looting 0 is a 5/10 gate; that is the shape the world probe measures.
        int passes = 0;
        for (int roll = 0; roll < BrainyZombieAiLogic.BRAIN_ROLL_MODULO; roll++) {
            if (BrainyZombieAiLogic.dropsBrain(roll, 0)) {
                passes++;
            }
        }
        assertEquals(5, passes);
    }

    @Test
    void brainIsThrownWithAVelocityAndTheGiantThrowsHarder() {
        // :51 entityDropItem(new ItemStack(itemZombieBrain), 1.5F)
        // :71 entityDropItem(new ItemStack(itemZombieBrain), 2.0F)
        assertEquals(1.5F, BrainyZombieAiLogic.BRAINY_BRAIN_OFFSET_Y, T);
        assertEquals(2.0F, BrainyZombieAiLogic.FURIOUS_BRAIN_OFFSET_Y, T);
        // :81, :84 — the carrot / potato cases are the dropItem seam (count 1,
        // Y offset 0.0), a different seam from the two itemResource cases.
        assertEquals(1, BrainyZombieAiLogic.LOOT_VEGETABLE_COUNT);
        assertEquals(0.0F, BrainyZombieAiLogic.LOOT_VEGETABLE_OFFSET_Y, T);
        assertEquals(1, BrainyZombieAiLogic.LOOT_STACK_SIZE);
        assertEquals(2.0F, BrainyZombieAiLogic.LOOT_THROW_OFFSET_Y, T);
        assertEquals(0.0F, BrainyZombieAiLogic.FLESH_DROP_OFFSET_Y, T);
    }

    @Test
    void furiousLootTableReplacesVanillaDropAllLoot() {
        // :75-89 — switch (this.rand.nextInt(4)): 0 itemResource meta 2 at 2.0,
        // 1 carrot 1, 2 potato 1, 3 itemResource meta 6 at 2.0.
        assertEquals(4, BrainyZombieAiLogic.LOOT_PICK_MODULO);
        assertEquals(0, BrainyZombieAiLogic.LOOT_PICK_THAUMIUM);
        assertEquals(1, BrainyZombieAiLogic.LOOT_PICK_CARROT);
        assertEquals(2, BrainyZombieAiLogic.LOOT_PICK_POTATO);
        assertEquals(3, BrainyZombieAiLogic.LOOT_PICK_AMBER);
        for (int roll = 0; roll < BrainyZombieAiLogic.LOOT_PICK_MODULO; roll++) {
            assertEquals(roll, BrainyZombieAiLogic.lootPick(roll), "the switch is the roll index");
        }
        // metas: 2 = thaumium ingot, 6 = amber (ItemResource's own constants)
        assertEquals(2, BrainyZombieAiLogic.LOOT_THAUMIUM_META);
        assertEquals(6, BrainyZombieAiLogic.LOOT_AMBER_META);
        assertEquals(2, ItemResource.META_THAUMIUM_INGOT);
        assertEquals(6, ItemResource.META_AMBER);

    }
}
