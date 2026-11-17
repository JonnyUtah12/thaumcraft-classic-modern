package com.jonnyutah12.thaumcraftclassic.entities.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Parity pins for {@link ThaumicSlimeAiLogic} — every number quotes the 1.7.10
 * decompiled line it came from
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/EntityThaumicSlime.java}).
 */
@DisplayName("ThaumicSlimeAiLogic parity (TC-M6-06-s-e)")
final class ThaumicSlimeAiLogicTest {

    @Test
    @DisplayName("EntityThaumicSlime.java:31-34 the ctor size roll is 1 << rand(3)")
    void initialSize() {
        assertEquals(1, ThaumicSlimeAiLogic.initialSize(0), "1 << 0");
        assertEquals(2, ThaumicSlimeAiLogic.initialSize(1), "1 << 1");
        assertEquals(4, ThaumicSlimeAiLogic.initialSize(2), "1 << 2 — rand(3) tops out at 2");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:84 the hitbox is 0.25*sqrt(size)+0.25 on BOTH axes")
    void sizeToBox() {
        assertEquals(0.5F, ThaumicSlimeAiLogic.sizeToBox(1), 1.0E-6F, "size 1 -> 0.25+0.25");
        assertEquals(0.603553F, ThaumicSlimeAiLogic.sizeToBox(2), 1.0E-5F, "size 2 -> 0.25*sqrt(2)+0.25");
        assertEquals(0.75F, ThaumicSlimeAiLogic.sizeToBox(4), 1.0E-6F, "size 4 -> 0.75");
        assertEquals(1.0F, ThaumicSlimeAiLogic.sizeToBox(9), 1.0E-6F, "size 9 -> 1.0");
        assertEquals(1.5F, ThaumicSlimeAiLogic.sizeToBox(25), 1.0E-6F, "size 25 -> 1.5");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:86-88 the health attribute IS the size and xp is (int)sqrt(size)")
    void healthAndXp() {
        assertEquals(4.0, ThaumicSlimeAiLogic.sizeToMaxHealth(4), 0.0, ":86 MAX_HEALTH = the size");
        assertEquals(9.0, ThaumicSlimeAiLogic.sizeToMaxHealth(9), 0.0, ":86 ditto");
        assertEquals(1, ThaumicSlimeAiLogic.sizeToExperience(1), ":88 (int)sqrt(1)");
        assertEquals(2, ThaumicSlimeAiLogic.sizeToExperience(4), ":88 (int)sqrt(4)");
        assertEquals(3, ThaumicSlimeAiLogic.sizeToExperience(9), ":88 (int)sqrt(9)");
        assertEquals(3, ThaumicSlimeAiLogic.sizeToExperience(15), ":88 (int)sqrt(15) truncates");
        assertEquals(4, ThaumicSlimeAiLogic.sizeToExperience(16), ":88 (int)sqrt(16)");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:84-90 + the save NBT round-trips the size through the box")
    void sizeNbtRoundTrip() {
        for (int size = 1; size <= 16; size++) {
            assertEquals(size, ThaumicSlimeAiLogic.nbtToSize(ThaumicSlimeAiLogic.sizeToNbt(size)),
                    "the size must survive a save/load cycle at size " + size);
        }
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:50-52 the ctor seeds launched=10 and the spit counter=100")
    void ctorCounters() {
        assertEquals(10, ThaumicSlimeAiLogic.LAUNCHED_START, ":50 field_70743_aJ = 10");
        assertEquals(100, ThaumicSlimeAiLogic.SPIT_COUNTER_START, ":52 field_70751_h = 100");
        assertEquals(101, ThaumicSlimeAiLogic.SPIT_COUNTER_RESET, ":52 the reset value is 101");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:246 the jump delay is rand(16) + 8")
    void jumpDelay() {
        assertEquals(8, ThaumicSlimeAiLogic.jumpDelay(0), "Line 246 getJumpDelay() = rand(16) + 8");
        assertEquals(23, ThaumicSlimeAiLogic.jumpDelay(15), "Line 246 rand(16) tops out at 15 -> 23");
        assertEquals(3, ThaumicSlimeAiLogic.JUMP_DELAY_TARGET_DIVISOR, ":37 jumpTarget / 3");
        assertEquals(10, ThaumicSlimeAiLogic.initialJumpDelay(0), ":36 rand(20) seeds the delay at 10");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:125-135 the jump impulse: 0.4 scale on XZ and 0.42 up")
    void jumpImpulse() {
        assertEquals(0.4, ThaumicSlimeAiLogic.JUMP_XZ_SCALE, 0.0, ":130 the strafe scale is 0.4");
        assertEquals(0.42, ThaumicSlimeAiLogic.JUMP_Y_IMPULSE, 0.0, ":131 the vertical impulse is 0.42");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:148-166 squish bookkeeping: 0.5 lerp, -0.5 on land, 1.0 on jump, 0.6 decay")
    void squishConstants() {
        assertEquals(0.5F, ThaumicSlimeAiLogic.SQUISH_LERP, 0.0F, ":151 the squish lerp is 0.5F");
        assertEquals(-0.5F, ThaumicSlimeAiLogic.SQUISH_ON_LAND, 0.0F, ":153 the landing squash");
        assertEquals(1.0F, ThaumicSlimeAiLogic.SQUISH_ON_JUMP, 0.0F, ":155 the jump stretch");
        assertEquals(0.6F, ThaumicSlimeAiLogic.SQUISH_DECAY, 0.0F, ":157 the decay");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:244-254 the search radii: 16 blocks, 10 aggro, 20 de-aggro")
    void searchRanges() {
        assertEquals(16.0, ThaumicSlimeAiLogic.TARGET_SEARCH_RANGE, 0.0, ":245 the scan box is 16");
        assertEquals(10.0F, ThaumicSlimeAiLogic.AGGRO_RANGE, 0.0F, ":248 aggro at 10");
        assertEquals(20.0F, ThaumicSlimeAiLogic.DEAGGRO_RANGE, 0.0F, ":249 de-aggro at 20");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:270-292 the spit throw: velocity 1.5, inaccuracy 1.0, lead 0.2, aim /3, jitter 0.0075")
    void throwConstants() {
        assertEquals(1.5F, ThaumicSlimeAiLogic.THROW_VELOCITY, 0.0F, ":275 the projectile velocity");
        assertEquals(1.0F, ThaumicSlimeAiLogic.THROW_INACCURACY, 0.0F, ":276 the inaccuracy");
        assertEquals(0.2, ThaumicSlimeAiLogic.THROW_LEAD_FACTOR, 0.0, ":271 the target lead");
        assertEquals(3.0F, ThaumicSlimeAiLogic.THROW_AIM_HEIGHT_DIVISOR, 0.0F, ":272 aim at height/3");
        assertEquals(0.0075F, ThaumicSlimeAiLogic.THROW_GAUSSIAN_SCALE, 0.0F, ":292 the gaussian scale");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:192-193 the merge scan is 16/8/16")
    void mergeScan() {
        assertEquals(16.0, ThaumicSlimeAiLogic.MERGE_SCAN_XZ, 0.0, ":192 horizontal 16");
        assertEquals(8.0, ThaumicSlimeAiLogic.MERGE_SCAN_Y, 0.0, ":193 vertical 8");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:344-350 the split children are size 1 at +0.5 up")
    void splitChildren() {
        assertEquals(1, ThaumicSlimeAiLogic.CHILD_SIZE, ":344 every child is size 1");
        assertEquals(0.5, ThaumicSlimeAiLogic.CHILD_SPAWN_Y_OFFSET, 0.0, ":345 the spawn offset is 0.5");
        assertEquals(2, ThaumicSlimeAiLogic.splitCount(4), "Line 254 the split count is (int)sqrt(size)");
        assertEquals(1, ThaumicSlimeAiLogic.splitCount(1), "Line 254 size 1 splits into 1");
        assertEquals(3, ThaumicSlimeAiLogic.splitCount(9), "Line 254 size 9 splits into 3");
        assertTrue(ThaumicSlimeAiLogic.splitsOnDeath(4, -1.0F),
                "Line 255 the split fires server side when the count > 1 and health is at or below 0");
        assertTrue(!ThaumicSlimeAiLogic.splitsOnDeath(1, -1.0F),
                "Line 255 a size-1 slime (count 1) never splits");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:382-403 the goo drop is meta 11 with a 1-in-3 roll")
    void goo() {
        assertEquals(11, ThaumicSlimeAiLogic.TAINTED_GOO_META, ":387 the goo meta is 11");
        assertEquals(ThaumicSlimeAiLogic.SPIT_SOUND_ID, "thaumcraft:gore",
                "the spit cue is thaumcraft:gore");
        assertEquals(3, ThaumicSlimeAiLogic.BIG_SOUND_SIZE, "the big-slime sound switches at size 3");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:431-452 the client box is 0.6x the real box")
    void clientBox() {
        for (int size = 1; size <= 9; size++) {
            assertEquals(0.6F * (float) Math.sqrt(size), ThaumicSlimeAiLogic.clientBox(size), 1.0E-5F,
                "the client-side box is CLIENT_BOX_FACTOR * sqrt(size) — NOT the 0.25*sqrt+0.25 hitbox "
                        + "multiplied, at size " + size);
        }
        assertEquals(0.6F, ThaumicSlimeAiLogic.CLIENT_BOX_FACTOR, 0.0F, ":436 the factor is 0.6F");
    }

    @Test
    @DisplayName("EntityThaumicSlime.java:404-430 the player reach index and its distance gate")
    void playerReach() {
        // Lines 269-282: reach index (int)max(1, sqrt(size)), launched size-2 slimes
        // bump to 3, and the touch gate is distanceSq < (0.8*index)^2.
        assertEquals(1, ThaumicSlimeAiLogic.playerReachIndex(1, 0), "Line 269 size 1 -> index 1");
        assertEquals(2, ThaumicSlimeAiLogic.playerReachIndex(4, 0), "Line 269 size 4 -> index 2");
        assertEquals(3, ThaumicSlimeAiLogic.playerReachIndex(4, 5),
                "Lines 271-274 the launched-size-2 quirk: a mid-air size-4 slime reads index 3");
        assertTrue(ThaumicSlimeAiLogic.canTouchPlayer(2, 2.0),
                "Line 277 index 2 gates at (0.8*2)^2 = 2.56, so 2.0 is in reach");
        assertTrue(!ThaumicSlimeAiLogic.canTouchPlayer(2, 3.0),
                "Line 277 and 3.0 is outside it");
        assertTrue(ThaumicSlimeAiLogic.canDamagePlayer(1), "Line 285 canDamagePlayer is size > 0");
        assertTrue(!ThaumicSlimeAiLogic.canDamagePlayer(0), "Line 285 a size-0 slime cannot hurt you");
    }
}
