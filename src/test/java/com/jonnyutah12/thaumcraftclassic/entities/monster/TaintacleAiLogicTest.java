package com.jonnyutah12.thaumcraftclassic.entities.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Parity pins for {@link TaintacleAiLogic} — every number and branch quotes the
 * 1.7.10 decompiled line it came from ({@code reference/decompiled/thaumcraft/
 * thaumcraft/common/entities/monster/EntityTaintacle.java} and
 * {@code EntityTaintacleSmall.java}).
 */
@DisplayName("TaintacleAiLogic parity (TC-M6-06-s-e)")
final class TaintacleAiLogicTest {

    // ---------------------------------------------------------------- dims (ctor)

    @Test
    @DisplayName("EntityTaintacle.java:30 func_70105_a(0.66F, 3.0F) and :31 xp 10")
    void ctorDimsAndXp() {
        assertEquals(0.66F, TaintacleAiLogic.WIDTH, 0.0F, "EntityTaintacle.java:30 width");
        assertEquals(3.0F, TaintacleAiLogic.HEIGHT, 0.0F, "EntityTaintacle.java:30 height");
        assertEquals(10, TaintacleAiLogic.XP, "EntityTaintacle.java:31 field_70728_aV = 10");
    }

    @Test
    @DisplayName("EntityTaintacle.java:54-56 eye height 0.25F and :60-61 attributes 50 / 7")
    void ctorEyeAndAttributes() {
        assertEquals(0.25F, TaintacleAiLogic.EYE_HEIGHT, 0.0F, "EntityTaintacle.java:55 eye height");
        assertEquals(50.0, TaintacleAiLogic.HEALTH, 0.0, "EntityTaintacle.java:60 MAX_HEALTH = 50");
        assertEquals(7.0, TaintacleAiLogic.ATTACK, 0.0, "EntityTaintacle.java:61 ATTACK_DAMAGE = 7");
    }

    @Test
    @DisplayName("EntityTaintacleSmall.java:14-22: 0.22x1.0, xp 0, 8 health, 2 attack, eye 0.25")
    void smallCtor() {
        assertEquals(0.22F, TaintacleAiLogic.SMALL_WIDTH, 0.0F, "EntityTaintacleSmall.java:14 width");
        assertEquals(1.0F, TaintacleAiLogic.SMALL_HEIGHT, 0.0F, "EntityTaintacleSmall.java:14 height");
        assertEquals(0, TaintacleAiLogic.SMALL_XP, "EntityTaintacleSmall.java:15 xp reward 0");
        assertEquals(8.0, TaintacleAiLogic.SMALL_HEALTH, 0.0, "EntityTaintacleSmall.java:21 health 8");
        assertEquals(2.0, TaintacleAiLogic.SMALL_ATTACK, 0.0, "EntityTaintacleSmall.java:22 attack 2");
        assertEquals(0.25F, TaintacleAiLogic.EYE_HEIGHT, 0.0F,
                "EntityTaintacleSmall.java:16-18 eye height 0.25F — the parent's getEyeHeight is shared");
    }

    // ---------------------------------------------------------------- sound

    @Test
    @DisplayName("EntityTaintacle.java:287-289 getSoundVolume() = maxHealth / 8")
    void soundVolume() {
        assertEquals(6.25F, TaintacleAiLogic.soundVolume(50.0F), 0.0F,
                "EntityTaintacle.java:288 field_70713_aO / 8.0F for a 50-health taintacle");
        assertEquals(1.0F, TaintacleAiLogic.soundVolume(8.0F), 0.0F,
                "EntityTaintacleSmall.java:21 an 8-health taintlet is 1.0");
        assertEquals(8.0F, TaintacleAiLogic.SOUND_VOLUME_PER_HEALTH, 0.0F, "the divisor is 8.0F");
    }

    @Test
    @DisplayName("EntityTaintacle.java:283-285 pitch is 1.3F MINUS maxHealth/10F")
    void soundPitch() {
        // The original SUBTRACTS, so a 50-heart taintacle plays at a NEGATIVE pitch.
        assertEquals(-3.7F, TaintacleAiLogic.soundPitch(50.0F), 1.0E-4F,
                "EntityTaintacle.java:283-285 pitch is 1.3F - maxHealth/10F = -3.7 — the negative pitch "
                        + "is the original's quirk, not a modernisation target");
        assertEquals(1.3F, TaintacleAiLogic.SOUND_PITCH_BASE, 0.0F, "the pitch base is 1.3F");
        assertEquals(10.0F, TaintacleAiLogic.SOUND_PITCH_PER_HEALTH, 0.0F, "the pitch divisor is 10.0F");
    }

    // ---------------------------------------------------------------- combat

    @Test
    @DisplayName("EntityTaintacle.java:139-143 attack cooldown 20 and :145-170 tentacle window 40+rand(20)")
    void attackCadence() {
        assertEquals(20, TaintacleAiLogic.ATTACK_COOLDOWN, ":142 field_70716_ep = 20");
        assertEquals(40, TaintacleAiLogic.TENTACLE_COOLDOWN_BASE, ":170 the tentacle cooldown base is 40");
        assertEquals(20, TaintacleAiLogic.TENTACLE_COOLDOWN_JITTER, ":170 rand(20) jitter");
        assertEquals(16.0F, TaintacleAiLogic.HURT_TENTACLE_RANGE, 0.0F,
                ":112 the hurt-burst radius is 16.0F");
    }

    @Test
    @DisplayName("EntityTaintacle.java:375-395 melee damage: attack + 3*STR - 2*WSK")
    void meleeDamage() {
        // The original shifts, so the base amplifier (0) is NOT free: 3 << 0 = +3
        // and 2 << 0 = -2. A clean-handed 7-attack taintacle therefore lands 8.
        assertEquals(8.0F, TaintacleAiLogic.meleeDamage(7.0F, 0, 0, 0), 0.0F,
                ":166-181 attack 7 + (3<<0) - (2<<0) = 8 — the amplifier is 0-based");
        assertEquals(29.0F, TaintacleAiLogic.meleeDamage(7.0F, 3, 0, 0), 0.0F,
                ":166-181 amplifier 3 is 3<<3 = +24, so 7 + 24 - 2 = 29");
        assertEquals(2.0F, TaintacleAiLogic.meleeDamage(7.0F, 0, 2, 0), 0.0F,
                ":166-181 weakness amplifier 2 is 2<<2 = -8, so 7 + 3 - 8 = 2");
        assertEquals(11.0F, TaintacleAiLogic.meleeDamage(7.0F, 0, 0, 3), 0.0F,
                ":166-181 the weapon enchantment bonus is added plain, not shifted");
        assertEquals(3.0, TaintacleAiLogic.STRENGTH_BONUS_PER_LEVEL, 0.0, ":172 the STR base is 3 (shifted)");
        assertEquals(2.0, TaintacleAiLogic.WEAKNESS_PENALTY_PER_LEVEL, 0.0, ":175 the WSK base is 2 (shifted)");
    }

    @Test
    @DisplayName("EntityTaintacle.java:157-159 sweep knockback: 0.5 scale, 0.1 up, then 0.6 drag")
    void knockbackConstants() {
        assertEquals(0.5F, TaintacleAiLogic.KNOCKBACK_XZ_SCALE, 0.0F, ":158 the sweep strength is 0.5F");
        assertEquals(0.1, TaintacleAiLogic.KNOCKBACK_Y, 0.0, ":158 the vertical kick is 0.1");
        assertEquals(0.6, TaintacleAiLogic.SELF_DRAG_AFTER_KNOCKBACK, 0.0,
                ":159 the mob's own motion is multiplied by 0.6");
        assertEquals(0.0, TaintacleAiLogic.knockbackX(0.0F, 0), 1.0E-9,
                ":157-159 a level-0 knockback moves nothing");
        assertEquals(0.0, TaintacleAiLogic.knockbackZ(0.0F, 0), 1.0E-9, ":157-159 ditto on Z");
    }

    @Test
    @DisplayName("EntityTaintacle.java:162-164 fire aspect: 4 seconds per level")
    void fireAspect() {
        assertEquals(4, TaintacleAiLogic.FIRE_SECONDS_PER_LEVEL, ":163 4 * level seconds");
        assertEquals(0, TaintacleAiLogic.fireSeconds(0), "no level, no fire");
        assertEquals(8, TaintacleAiLogic.fireSeconds(2), ":163 level 2 is 8 seconds");
    }

    // ------------------------------------------------------------- taintlet life

    @Test
    @DisplayName("EntityTaintacleSmall.java:32-40: a 200-tick life that burns 10 health when it expires")
    void smallLifetime() {
        assertEquals(200, TaintacleAiLogic.SMALL_LIFETIME, ":32-38 the life counter starts at 200");
        assertEquals(200, TaintacleAiLogic.SMALL_LIFETIME_START, ":33 the ctor seeds it at 200");
        assertEquals(10.0F, TaintacleAiLogic.SMALL_LIFETIME_DAMAGE, 0.0F,
                ":36-37 the expiry damage is 10.0F generic");
    }

    // ---------------------------------------------------------------- drops

    @Test
    @DisplayName("EntityTaintacle.java:171-176 + :301: 1-in-4 fibres spread, metas 11/12, drop offset maxHealth/2")
    void drops() {
        assertEquals(4, TaintacleAiLogic.FIBRES_SPREAD_GRASS_ODDS,
                ":171-173 the grass spread roll is rand(4) == 0");
        assertEquals(11, TaintacleAiLogic.META_TAINTED_GOO, ":176 the goo drop is meta 11");
        assertEquals(12, TaintacleAiLogic.META_TAINTED_FUNGUS, ":176 the fungus drop is meta 12");
        assertEquals(25.0F, TaintacleAiLogic.dropOffset(50.0F), 1.0E-4F,
                "EntityTaintacle.java:301 the drop offset is maxHealth / 2.0F");
    }

    // ------------------------------------------------------------- taint gates

    @Test
    @DisplayName("EntityTaintacle.java:114-121: one starve heart every 20 ticks off taint")
    void starveCadence() {
        assertTrue(TaintacleAiLogic.starves(20, false), ":115-118 the 20-tick gate fires off taint");
        assertFalse(TaintacleAiLogic.starves(21, false), ":115 only on the 20-tick boundary");
        assertFalse(TaintacleAiLogic.starves(20, true), ":115 the gate is closed on taint");
        assertEquals(20, TaintacleAiLogic.STARVE_INTERVAL, ":118 the cadence is 20");
        assertEquals(1.0F, TaintacleAiLogic.STARVE_DAMAGE, 0.0F, ":118 the damage is 1.0F");
    }

    @Test
    @DisplayName("EntityTaintacle.java:42-44 the spawn scan box is 24/8/24 around the mob")
    void spawnScanBox() {
        assertEquals(24.0, TaintacleAiLogic.SPAWN_SCAN_XZ, 0.0, ":43 the horizontal inflation is 24.0F");
        assertEquals(8.0, TaintacleAiLogic.SPAWN_SCAN_Y, 0.0, ":43 the vertical inflation is 8.0F");
        final net.minecraft.world.phys.AABB box = TaintacleAiLogic.spawnScanBox(0.0, 0.0, 0.0);
        assertEquals(48.0, box.maxX - box.minX, 1.0E-6, "the box spans 48 blocks on X");
        assertEquals(16.0, box.maxY - box.minY, 1.0E-6, "and 16 on Y");
    }

    @Test
    @DisplayName("EntityTaintacle.java:186-199 the aggro box scales with max health (6.0F / 3.0F per heart)")
    void targetScanBox() {
        assertEquals(6.0F, TaintacleAiLogic.TARGET_SCAN_XZ_PER_HEALTH, 0.0F, ":196 6.0F per heart");
        assertEquals(3.0F, TaintacleAiLogic.TARGET_SCAN_Y_PER_HEALTH, 0.0F, ":196 3.0F per heart");
        final net.minecraft.world.phys.AABB box = TaintacleAiLogic.targetScanBox(0.0, 0.0, 0.0, 50.0F);
        assertEquals(600.0, box.maxX - box.minX, 1.0E-4, "a 50-heart taintacle scans 300 blocks out on X");
        assertEquals(300.0, box.maxY - box.minY, 1.0E-4, "and 150 on Y (3.0F per heart)");
    }

    @Test
    @DisplayName("EntityTaintacle.java:201-203 the flail ramp: age maxHealth*10, step 0.2F, floor 1.0F, cap 3.0F")
    void flailConstants() {
        assertEquals(10.0F, TaintacleAiLogic.FLAIL_AGE_PER_HEALTH, 0.0F, ":127 age = maxHealth * 10");
        assertEquals(0.2F, TaintacleAiLogic.FLAIL_STEP, 0.0F, ":134 the ramp step is 0.2F");
        assertEquals(1.0F, TaintacleAiLogic.FLAIL_MIN, 0.0F, ":134 the ramp floor is 1.0F");
        assertEquals(3.0F, TaintacleAiLogic.FLAIL_MAX, 0.0F, ":132-136 the cap is 3.0F");
        assertEquals(7.0F, TaintacleAiLogic.AGITATION_RADIUS_PER_HEALTH, 0.0F,
                ":199 the aggro radius scales 7.0F per heart");
    }
}
