package com.jonnyutah12.thaumcraftclassic.entities.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TC-M6-06-s-e: the taint swarm's numbers and rules (content-mapping.csv row
 * {@code Thaumcraft:TaintSwarm} → {@code thaumcraft:taintswarm}).
 *
 * <p>Every literal here is read off the decompiled 1.7.10
 * {@code thaumcraft.common.entities.monster.EntityTaintSwarm} (262 lines, cited as
 * {@code :NNN} in each assertion message). They are pinned, not restated: the
 * original is full of hand-tuned floats and quirks that a "clean" rewrite drops —
 * two different 0.025 lerp literals, an {@code (int)} truncation instead of floor,
 * a byte-typed damage bonus, a drop offset taken from the entity {@code height}
 * field, and an FX list that prunes one entry per tick.</p>
 *
 * <p>The swarm installs NO goal selectors ({@code :30-32} is an empty
 * {@code registerGoals}), so unlike the taintacle there is no goal set to assert —
 * the parity surface is the arithmetic and the gates in
 * {@link TaintSwarmAiLogic}.</p>
 */
class TaintSwarmAiLogicTest {

    @Test
    @DisplayName("body, attributes and light pin the original's setSize and attribute registrations")
    void originalNumbers() {
        assertEquals(2.0F, TaintSwarmAiLogic.WIDTH, 0.0,
                "setSize(2.0F, 2.0F) in the ctor, EntityTaintSwarm.java:33");
        assertEquals(2.0F, TaintSwarmAiLogic.HEIGHT, 0.0,
                "setSize(2.0F, 2.0F) in the ctor, EntityTaintSwarm.java:33");
        assertEquals(1.0F, TaintSwarmAiLogic.EYE_HEIGHT, 0.0,
                "getEyeHeight(float) returns 1.0F, EntityTaintSwarm.java:50-52");
        assertEquals(30.0, TaintSwarmAiLogic.HEALTH, 0.0,
                "maxHealth base 30.0, EntityTaintSwarm.java:76");
        assertEquals(2.0, TaintSwarmAiLogic.ATTACK_BASE, 0.0,
                "attackDamage base 2 (+damBonus), EntityTaintSwarm.java:77");
        assertEquals(2.0, TaintSwarmAiLogic.attackDamage(0), 0.0,
                "the ctor runs func_110147_ax with damBonus == 0, EntityTaintSwarm.java:28 + :77");
        assertEquals(9.0, TaintSwarmAiLogic.attackDamage(7), 0.0,
                "2 + damBonus, EntityTaintSwarm.java:77");
        assertEquals(0.1F, TaintSwarmAiLogic.SOUND_VOLUME, 0.0,
                "getSoundVolume() = 0.1F, EntityTaintSwarm.java:54-56");
        assertEquals(15728880, TaintSwarmAiLogic.FULL_BRIGHTNESS_LIGHTMAP,
                "the client getBrightness(float) override returns 15728880, EntityTaintSwarm.java:41-44");
        assertEquals(0xF000F0, TaintSwarmAiLogic.FULL_BRIGHTNESS_LIGHTMAP,
                "15728880 == 0xF000F0: full sky AND full block light, EntityTaintSwarm.java:43");
    }

    @Test
    @DisplayName("the 1.7.10 registration is range 64 blocks, update 3")
    void registrationNumbers() {
        assertEquals(64, TaintSwarmAiLogic.TRACKING_RANGE_BLOCKS,
                "ConfigEntities.java:137 registration: EntityTaintSwarm.class, 64, 3, true");
        assertEquals(4, TaintSwarmAiLogic.TRACKING_RANGE_CHUNKS,
                "the 1.21.1 builder takes chunks: 64 blocks / 16");
    }

    @Test
    @DisplayName("summoned flag: data 16, bit 2 set, cleared with flags & -4")
    void flags() {
        assertEquals(16, TaintSwarmAiLogic.DATA_FLAGS_INDEX,
                "dataWatcher index 16 is created as a Byte, EntityTaintSwarm.java:36-39");
        assertEquals(2, TaintSwarmAiLogic.SUMMONED_BIT,
                "getIsSummoned tests (data16 & 2) != 0, EntityTaintSwarm.java:80-82");
        assertTrue(TaintSwarmAiLogic.isSummoned((byte) 2), "bit 2 set, EntityTaintSwarm.java:82");
        assertTrue(TaintSwarmAiLogic.isSummoned((byte) 3), "the mask is a test, not an equality, :82");
        assertFalse(TaintSwarmAiLogic.isSummoned((byte) 1),
                "bit 0 alone is not summoned, EntityTaintSwarm.java:82");
        assertFalse(TaintSwarmAiLogic.isSummoned((byte) 0), "a fresh swarm is not summoned, :38");

        assertEquals((byte) 3, TaintSwarmAiLogic.flagsWithSummoned((byte) 1, true),
                "setIsSummoned writes flags | 2, EntityTaintSwarm.java:84-91");
        assertEquals((byte) 2, TaintSwarmAiLogic.flagsWithSummoned((byte) 0, true),
                "setIsSummoned writes flags | 2, EntityTaintSwarm.java:84-91");
        assertEquals(-4, TaintSwarmAiLogic.FLAG_CLEAR_MASK,
                "clear writes flags & -4, EntityTaintSwarm.java:89");
        assertEquals((byte) 0, TaintSwarmAiLogic.flagsWithSummoned((byte) 3, false),
                "QUIRK: clearing the summoned bit also wipes bit 0 (the mask is & -4, not & ~2), :89");
        assertEquals((byte) 4, TaintSwarmAiLogic.flagsWithSummoned((byte) 5, false),
                "bit 2 clears, bit 2 of 5 goes, bit 0 is collateral damage, :89");
    }

    @Test
    @DisplayName("the summoned-with-no-target self damage, the invulnerable gate, the target provider")
    void gates() {
        assertTrue(TaintSwarmAiLogic.summonSelfDamageApplies(true, false),
                "if (getIsSummoned()) attackEntityFrom(DamageSource.field_76377_j, 5.0F) inside the "
                        + "entityToAttack == null branch, EntityTaintSwarm.java:120-123");
        assertFalse(TaintSwarmAiLogic.summonSelfDamageApplies(true, true),
                "the self damage is inside the no-target branch, :120");
        assertFalse(TaintSwarmAiLogic.summonSelfDamageApplies(false, false),
                "an unsummoned swarm never self-damages, :121");
        assertEquals(5.0F, TaintSwarmAiLogic.SUMMON_SELF_DAMAGE, 0.0,
                "the self damage amount, EntityTaintSwarm.java:122");

        assertTrue(TaintSwarmAiLogic.hurtBlocked(true),
                "if (isInvulnerable()) return false; — no hit at all, EntityTaintSwarm.java:194-196");
        assertFalse(TaintSwarmAiLogic.hurtBlocked(false),
                "an ordinary swarm takes the hit, EntityTaintSwarm.java:194-196");

        assertTrue(TaintSwarmAiLogic.providerSearches(false),
                "func_70782_k returns getClosestPlayerToEntity(this, 12.0) when unsummoned, :223-226");
        assertFalse(TaintSwarmAiLogic.providerSearches(true),
                "a summoned swarm never gains a target, EntityTaintSwarm.java:225");
        assertEquals(12.0, TaintSwarmAiLogic.TARGET_RANGE, 0.0,
                "double var1 = 12.0, EntityTaintSwarm.java:224");
        assertTrue(TaintSwarmAiLogic.clearsTarget(true),
                "if (entityToAttack instanceof EntityPlayer && .capabilities.isCreativeMode) = null, :171-173");
        assertFalse(TaintSwarmAiLogic.clearsTarget(false),
                "a survival player stays the target, EntityTaintSwarm.java:171");
    }

    @Test
    @DisplayName("the hover steering reproduces both literal sets, idle and target-tracking")
    void steering() {
        assertEquals(0.5, TaintSwarmAiLogic.STEER_XZ_SPEED, 0.0,
                "MathHelper.floor_float(delta) * 0.5 for x/z, EntityTaintSwarm.java:151 + :162 + :164");
        assertEquals(0.7F, TaintSwarmAiLogic.STEER_Y_SPEED, 0.0,
                "the vertical push is 0.7, EntityTaintSwarm.java:152 + :163");
        assertEquals(0.1F, TaintSwarmAiLogic.STEER_Y_LERP, 0.0,
                "the vertical lerp is 0.1 in BOTH branches, EntityTaintSwarm.java:152 + :163");
        assertEquals(0.015000000014901161, TaintSwarmAiLogic.STEER_IDLE_XZ_LERP, 0.0,
                "the idle lerp literal, EntityTaintSwarm.java:151");
        assertEquals(0.025000000149011613, TaintSwarmAiLogic.STEER_TARGET_X_LERP, 0.0,
                "x lerps at 0.025000000149011613 with a target, EntityTaintSwarm.java:162");
        assertEquals(0.02500000001490116, TaintSwarmAiLogic.STEER_TARGET_Z_LERP, 0.0,
                "z lerps at 0.02500000001490116 with a target, EntityTaintSwarm.java:164");
        assertNotEquals(TaintSwarmAiLogic.STEER_TARGET_X_LERP, TaintSwarmAiLogic.STEER_TARGET_Z_LERP, 1e-12,
                "QUIRK: x and z steer with DIFFERENT literals in the original (:162 vs :164); a "
                        + "modernisation that shares one constant fails here");
        assertEquals(0.1F, TaintSwarmAiLogic.MOVE_FORWARD_INPUT, 0.0,
                "field_70701_bs (moveForward) = 0.1F, EntityTaintSwarm.java:156 + :167");
        assertEquals(0.6F, TaintSwarmAiLogic.Y_DRAG, 0.0,
                "motionY *= 0.6F after super.onUpdate(), EntityTaintSwarm.java:103");
        assertEquals(TaintSwarmAiLogic.Y_DRAG, TaintSwarmAiLogic.dragVertical(1.0), 0.0,
                "the swarm's own y drag runs AFTER the vanilla tick, and the factor is the FLOAT "
                        + "literal 0.6F promoted to double, so 1.0 drags to 0.6000000238418579 "
                        + "exactly as 1.7.10's double motionY *= 0.6F does, EntityTaintSwarm.java:103");

        // :151 motionX += (MathHelper.signPositive(delta) * 0.5 - motionX) * lerp
        assertEquals(0.5 * TaintSwarmAiLogic.STEER_IDLE_XZ_LERP,
                TaintSwarmAiLogic.steerHorizontal(0.0, 3.0, TaintSwarmAiLogic.STEER_IDLE_XZ_LERP), 1e-12,
                "from rest, an idle swarm gains sign*0.5 * 0.015... per tick, EntityTaintSwarm.java:151");
        assertEquals(-0.5 * TaintSwarmAiLogic.STEER_IDLE_XZ_LERP,
                TaintSwarmAiLogic.steerHorizontal(0.0, -3.0, TaintSwarmAiLogic.STEER_IDLE_XZ_LERP), 1e-12,
                "the sign comes from the delta, EntityTaintSwarm.java:151");
        // The original multiplies a FLOAT 0.7F by a FLOAT 0.1F, each widened to double
        // (0.7f = 0.69999998807907104, 0.1f = 0.10000000149011612), so the step is
        // 0.06999999985098837, not the float product 0.07f. Pinned.
        assertEquals((double) TaintSwarmAiLogic.STEER_Y_SPEED * (double) TaintSwarmAiLogic.STEER_Y_LERP,
                TaintSwarmAiLogic.steerVertical(0.0, 5.0), 0.0,
                "the vertical step from rest is 0.7F * 0.1F widened to double, "
                        + "EntityTaintSwarm.java:152");
        assertEquals(0.5 * TaintSwarmAiLogic.STEER_TARGET_X_LERP,
                TaintSwarmAiLogic.steerHorizontal(0.0, 1.0, TaintSwarmAiLogic.STEER_TARGET_X_LERP), 1e-12,
                "the target-tracking x step, EntityTaintSwarm.java:162");
        assertEquals(0.5 * TaintSwarmAiLogic.STEER_TARGET_Z_LERP,
                TaintSwarmAiLogic.steerHorizontal(0.0, 1.0, TaintSwarmAiLogic.STEER_TARGET_Z_LERP), 1e-12,
                "the target-tracking z step uses the OTHER literal, EntityTaintSwarm.java:164");

        // :148-150 idle deltas: x/z centre on +0.5, y sits at +0.1 above the target block
        assertEquals(1.25, TaintSwarmAiLogic.idleDeltaX(10, 9.25), 1e-12,
                "deltaX = target.x + 0.5 - posX, EntityTaintSwarm.java:148");
        assertEquals(0.1, TaintSwarmAiLogic.idleDeltaY(64, 64.0), 1e-12,
                "deltaY = target.y + 0.1 - posY (0.1, NOT the 0.5 used for x/z), :149");
        assertEquals(-1.0, TaintSwarmAiLogic.idleDeltaZ(20, 21.5), 1e-12,
                "deltaZ = target.z + 0.5 - posZ = 20 + 0.5 - 21.5, EntityTaintSwarm.java:150");
        // :159-161 target deltas: bare differences, y aims at the eye height
        assertEquals(1.75, TaintSwarmAiLogic.targetDeltaX(11.0, 9.25), 1e-12,
                "deltaX = target.posX - posX with no +0.5, EntityTaintSwarm.java:159");
        assertEquals(3.0, TaintSwarmAiLogic.targetDeltaY(62.0, 1.0, 60.0), 1e-12,
                "deltaY = target.posY + target.getEyeHeight() - posY, EntityTaintSwarm.java:160");
        assertEquals(-2.5, TaintSwarmAiLogic.targetDeltaZ(20.0, 22.5), 1e-12,
                "deltaZ = target.posZ - posZ, EntityTaintSwarm.java:161");
    }

    @Test
    @DisplayName("the swarm faces its motion vector: atan2 - 90 - yaw, wrapped to 180")
    void yawFollowsMotion() {
        assertEquals(90.0, TaintSwarmAiLogic.YAW_BEARING_OFFSET, 0.0,
                "the bearing is the atan2 bearing MINUS 90, EntityTaintSwarm.java:154 + :165");
        assertEquals(-90.0F, TaintSwarmAiLogic.yawStep(1.0, 0.0, 0.0F), 1e-6,
                "func_70622_a(atan2(motionZ, motionX) * 180 / PI - 90 - rotationYaw): due east is -90, :154");
        assertEquals(0.0F, TaintSwarmAiLogic.yawStep(0.0, 1.0, 0.0F), 1e-6,
                "due south is 0, EntityTaintSwarm.java:154");
        assertEquals(90.0F, TaintSwarmAiLogic.yawStep(-1.0, 0.0, 0.0F), 1e-6,
                "due west is +90, EntityTaintSwarm.java:154");
        assertEquals(-180.0F, TaintSwarmAiLogic.yawStep(0.0, -1.0, 0.0F), 1e-6,
                "due north wraps to -180 (wrapAngleTo180 keeps the half-open range), :154");
        assertEquals(-135.0F, TaintSwarmAiLogic.yawStep(1.0, -1.0, 0.0F), 1e-6,
                "north-east is -135, EntityTaintSwarm.java:154");
        assertEquals(0.0F, TaintSwarmAiLogic.yawStep(1.0, 0.0, -90.0F), 1e-6,
                "the step is RELATIVE: -90 - (-90) = 0, so a swarm already facing its motion does not turn, :154");
    }

    @Test
    @DisplayName("the flight target: (int) truncation, the roll shape, the arrival bound")
    void flightTarget() {
        assertEquals(7, TaintSwarmAiLogic.FLIGHT_ROLL_XZ,
                "x/z rolls are nextInt(7) - nextInt(7), EntityTaintSwarm.java:142 + :144");
        assertEquals(6, TaintSwarmAiLogic.FLIGHT_ROLL_Y,
                "the y roll is nextInt(6) - 2, EntityTaintSwarm.java:143");
        assertEquals(2, TaintSwarmAiLogic.FLIGHT_Y_DROP,
                "the target sits 2 blocks under the rolled y, EntityTaintSwarm.java:143");
        assertEquals(new BlockPos(8, 64, 15),
                TaintSwarmAiLogic.pickFlightTarget(new BlockPos(10, 64, 20), 3, 5, 2, 1, 6),
                "target = (x + roll - roll, y + roll - 2, z + roll - roll), EntityTaintSwarm.java:141-145");
        assertEquals(new BlockPos(10, 63, 20),
                TaintSwarmAiLogic.pickFlightTarget(new BlockPos(10, 64, 20), 0, 0, 1, 0, 0),
                "the smallest roll set puts the target one block below, :143");

        assertEquals(-3, TaintSwarmAiLogic.truncate(-3.5),
                "QUIRK: the source casts with (int) this.posX — truncation toward zero, not floor, :142");
        assertEquals(3, TaintSwarmAiLogic.truncate(3.5),
                "the same cast is a no-op on the positive side, EntityTaintSwarm.java:142");

        assertEquals(29.0, TaintSwarmAiLogic.flightTargetDistanceSquared(
                new BlockPos(8, 64, 15), 10, 64, 20), 1e-9,
                "ChunkCoordinates.func_71569_e is the plain integer-component distance, "
                        + "with no +0.5 centre offset, EntityTaintSwarm.java:140");
        assertEquals(4.0F, TaintSwarmAiLogic.FLIGHT_ARRIVAL_DIST_SQR, 0.0,
                "the arrival test is distanceSquared < 4.0F (2 blocks), EntityTaintSwarm.java:140");
        assertEquals(30, TaintSwarmAiLogic.FLIGHT_REPICK_ROLL,
                "the re-pick roll is rand.nextInt(30) == 0, EntityTaintSwarm.java:139");
        assertTrue(TaintSwarmAiLogic.wantsNewFlightTarget(0, 999.0),
                "the 1-in-30 roll re-picks regardless of distance, EntityTaintSwarm.java:139");
        assertTrue(TaintSwarmAiLogic.wantsNewFlightTarget(17, 3.99),
                "arrival re-picks, EntityTaintSwarm.java:140");
        assertFalse(TaintSwarmAiLogic.wantsNewFlightTarget(17, 4.0),
                "the bound is strict (4.0F is NOT < 4.0F), EntityTaintSwarm.java:140");
    }

    @Test
    @DisplayName("the flight target goes stale in a block, out of the y band, or off the taint biome")
    void flightTargetExpiry() {
        assertEquals(1, TaintSwarmAiLogic.FLIGHT_MIN_Y,
                "a target with y < 1 is dropped, EntityTaintSwarm.java:129");
        assertEquals(8, TaintSwarmAiLogic.FLIGHT_HEIGHT_BONUS,
                "the ceiling is world.getHeight(x, z) + 8, EntityTaintSwarm.java:130-131");
        assertEquals(78, TaintSwarmAiLogic.flightHeightLimit(70),
                "world.getHeight(x,z) + 8, EntityTaintSwarm.java:131");

        assertFalse(TaintSwarmAiLogic.flightTargetIsStale(true, 64, 78, true),
                "air, in band, taint biome: the swarm keeps flying at it, EntityTaintSwarm.java:127-133");
        assertTrue(TaintSwarmAiLogic.flightTargetIsStale(false, 64, 78, true),
                "the target is stale once the target block is not air, EntityTaintSwarm.java:127");
        assertTrue(TaintSwarmAiLogic.flightTargetIsStale(true, 0, 78, true),
                "y 0 is below the floor of the band, EntityTaintSwarm.java:129");
        assertTrue(TaintSwarmAiLogic.flightTargetIsStale(true, 79, 78, true),
                "one over the ceiling, EntityTaintSwarm.java:130-131");
        assertTrue(TaintSwarmAiLogic.flightTargetIsStale(true, 64, 78, false),
                "the taint biome test (1.7.10 world.getBiomeGenForCoords(x,z).biomeID != "
                        + "Config.biomeTaintID) drops the target off-taint, EntityTaintSwarm.java:132");
    }

    @Test
    @DisplayName("the melee swing: bare 3.0F reach, 10..14 cooldown, weakness 100 ticks amp 0")
    void melee() {
        assertEquals(3.0F, TaintSwarmAiLogic.MELEE_REACH, 0.0,
                "the reach test is a bare par2 < 3.0F — no player-width bonus, EntityTaintSwarm.java:200");
        assertEquals(10, TaintSwarmAiLogic.ATTACK_COOLDOWN_BASE,
                "attackTime = 10 + rand.nextInt(5), EntityTaintSwarm.java:207");
        assertEquals(5, TaintSwarmAiLogic.ATTACK_COOLDOWN_JITTER,
                "the swing jitter, EntityTaintSwarm.java:207");
        assertTrue(TaintSwarmAiLogic.canMelee(0, 2.99F, true),
                "cool == 0 && dist < 3 && the boxes overlap vertically, EntityTaintSwarm.java:199-202");
        assertFalse(TaintSwarmAiLogic.canMelee(0, 3.0F, true),
                "the reach bound is strict: 3.0F is not < 3.0F, EntityTaintSwarm.java:200");
        assertFalse(TaintSwarmAiLogic.canMelee(1, 2.0F, true),
                "a mid-cooldown swarm cannot swing, EntityTaintSwarm.java:199");
        assertFalse(TaintSwarmAiLogic.canMelee(0, 2.0F, false),
                "target.maxY > self.minY && target.minY < self.maxY gates the swing, :201-202");

        final RandomSource random = RandomSource.create(0x5EEDL);
        for (int a = 0; a < 200; a++) {
            final int cooldown = TaintSwarmAiLogic.meleeCooldown(random);
            assertTrue(cooldown >= 10 && cooldown <= 14,
                    "10 + nextInt(5) is 10..14, EntityTaintSwarm.java:207, got " + cooldown);
        }

        assertEquals(100, TaintSwarmAiLogic.RECENTLY_HIT_TICKS,
                "EntityUtils.setRecentlyHit(target, 100) on a summoned swarm's hit, :203-205");
        assertEquals(100, TaintSwarmAiLogic.WEAKNESS_TICKS,
                "Potion.weakness for 100 ticks, EntityTaintSwarm.java:212");
        assertEquals(0, TaintSwarmAiLogic.WEAKNESS_AMPLIFIER,
                "amplifier 0, EntityTaintSwarm.java:212");
        assertEquals(0.3F, TaintSwarmAiLogic.ATTACK_SOUND_VOLUME, 0.0,
                "playSound(this, \"thaumcraft:swarmattack\", 0.3F, ...), EntityTaintSwarm.java:219");
        assertEquals(0.9F, TaintSwarmAiLogic.ATTACK_SOUND_PITCH_BASE, 0.0,
                "pitch base 0.9F, EntityTaintSwarm.java:219");
        assertEquals(0.2F, TaintSwarmAiLogic.ATTACK_SOUND_PITCH_SPREAD, 0.0,
                "world.rand.nextFloat() * 0.2F — the WORLD's rng, not the entity's, :219");
        assertEquals(0.9F, TaintSwarmAiLogic.attackSoundPitch(0.0F), 1e-6,
                "pitch at roll 0, EntityTaintSwarm.java:219");
        assertEquals(1.1F, TaintSwarmAiLogic.attackSoundPitch(1.0F), 1e-6,
                "pitch at roll 1, EntityTaintSwarm.java:219");
    }

    @Test
    @DisplayName("the spawn light gate reads light then rolls 0..6")
    void spawnLight() {
        assertEquals(7, TaintSwarmAiLogic.SPAWN_LIGHT_ROLL_BOUND,
                "byte var5 = 7; light > rand.nextInt(7) ? false : super, EntityTaintSwarm.java:245-246");
        assertTrue(TaintSwarmAiLogic.lightAllowsSpawn(0, 0),
                "light 0 beats the smallest roll, EntityTaintSwarm.java:246");
        assertTrue(TaintSwarmAiLogic.lightAllowsSpawn(6, 6),
                "6 is not > 6, so dark spawns pass, EntityTaintSwarm.java:246");
        assertFalse(TaintSwarmAiLogic.lightAllowsSpawn(7, 6),
                "the brightest passable light is 6, EntityTaintSwarm.java:246");
        assertFalse(TaintSwarmAiLogic.lightAllowsSpawn(1, 0),
                "the roll is 0..6, so light 1 fails on a roll of 0 — the gate is a dice roll, :246");
    }

    @Test
    @DisplayName("client FX: budget Math.max(count, 10) and a one-entry-per-tick prune")
    void clientFx() {
        assertEquals(25, TaintSwarmAiLogic.FX_BUDGET_REQUEST,
                "Thaumcraft.proxy.particleCount(25), EntityTaintSwarm.java:112");
        assertEquals(10, TaintSwarmAiLogic.FX_MIN_COUNT,
                "the budget is Math.max(particleCount(25), 10), EntityTaintSwarm.java:112");
        assertEquals(25, TaintSwarmAiLogic.fxCountTarget(25),
                "a granted budget is used as-is, EntityTaintSwarm.java:112");
        assertEquals(10, TaintSwarmAiLogic.fxCountTarget(3),
                "a starved particle system still wants 10 FX, EntityTaintSwarm.java:112");
        assertTrue(TaintSwarmAiLogic.spawnsFx(24, 25),
                "swarm.size() < Math.max(count, 10) adds an FX, EntityTaintSwarm.java:112");
        assertFalse(TaintSwarmAiLogic.spawnsFx(25, 25),
                "at budget the swarm stops adding, EntityTaintSwarm.java:112");
        assertTrue(TaintSwarmAiLogic.spawnsFx(9, 3),
                "the floor of 10 keeps FX alive even on a starved budget (9 < 10), :112");
        assertFalse(TaintSwarmAiLogic.spawnsFx(10, 3),
                "the floor is 10, so a list of 10 stops adding on a starved budget, :112");
        assertEquals(0.22F, TaintSwarmAiLogic.FX_PARTICLE_SCALE, 0.0,
                "swarmParticleFX(world, this, 0.22F, 15.0F, 0.08F), EntityTaintSwarm.java:113");
        assertEquals(15.0F, TaintSwarmAiLogic.FX_VIEW_RANGE, 0.0,
                "the FX view range argument, EntityTaintSwarm.java:113");
        assertEquals(0.08F, TaintSwarmAiLogic.FX_SPEED, 0.0,
                "the FX speed argument, EntityTaintSwarm.java:113");

        assertEquals(2, TaintSwarmAiLogic.firstRemovableFxIndex(
                new boolean[] {false, false, true, true}),
                "QUIRK: the scan breaks at the FIRST null/dead entry, so it prunes one FX per tick, "
                        + "EntityTaintSwarm.java:106-110");
        assertEquals(0, TaintSwarmAiLogic.firstRemovableFxIndex(new boolean[] {true, true}),
                "the scan always restarts at index 0, EntityTaintSwarm.java:106");
        assertEquals(-1, TaintSwarmAiLogic.firstRemovableFxIndex(new boolean[] {false, false}),
                "a clean list prunes nothing, EntityTaintSwarm.java:105-111");
        assertEquals(-1, TaintSwarmAiLogic.firstRemovableFxIndex(new boolean[0]),
                "an empty list prunes nothing, EntityTaintSwarm.java:105");
    }

    @Test
    @DisplayName("the drop: a coin flip, itemResource meta 11, offset from the height FIELD not the box")
    void drop() {
        assertEquals("itemResource", TaintSwarmAiLogic.DROP_ITEM,
                "getDropItem() returns ConfigItems.itemResource, EntityTaintSwarm.java:253-255");
        assertEquals(11, TaintSwarmAiLogic.META_TAINTED_GOO,
                "new ItemStack(ConfigItems.itemResource, 1, 11) — Tainted Goo, EntityTaintSwarm.java:259");
        assertEquals(0.1F, TaintSwarmAiLogic.DROP_OFFSET_HEIGHT_FIELD, 0.0,
                "MCP names field_70131_O `height` — EntityLivingBase initialises it to 0.1F and the "
                        + "swarm never writes it, EntityTaintSwarm.java:259");
        assertEquals(0.05F, TaintSwarmAiLogic.dropOffset(), 1e-6,
                "the offset is field_70131_O / 2.0F = 0.05, NOT the bounding-box height (2.0/2 = 1.0), :259");
        assertTrue(TaintSwarmAiLogic.dropsGoo(true),
                "dropFewItems drops on a coin flip, EntityTaintSwarm.java:258");
        assertFalse(TaintSwarmAiLogic.dropsGoo(false),
                "the other half of the coin flip drops nothing, EntityTaintSwarm.java:258");
    }

    @Test
    @DisplayName("NBT: Flags is the raw byte and damBonus is a byte, with no hasKey guard")
    void nbt() {
        assertEquals("Flags", TaintSwarmAiLogic.NBT_FLAGS,
                "data16 = tag.getByte(\"Flags\"), EntityTaintSwarm.java:230");
        assertEquals("damBonus", TaintSwarmAiLogic.NBT_DAM_BONUS,
                "damBonus = tag.getByte(\"damBonus\"), EntityTaintSwarm.java:231");
        assertEquals(-56, TaintSwarmAiLogic.damBonusFromNbt((byte) 200),
                "QUIRK: the stored byte widens to int, so 200 reads back as -56, :231");
        assertEquals(0, TaintSwarmAiLogic.damBonusFromNbt((byte) 0),
                "a saved 0 reads as 0, EntityTaintSwarm.java:231");
        assertEquals(44, TaintSwarmAiLogic.damBonusToNbt(300),
                "QUIRK: the write narrows with (byte) damBonus, so 300 saves as 44, :237");
        assertEquals((byte) -1, TaintSwarmAiLogic.damBonusToNbt(255),
                "255 saves as -1 and reads back as -1, EntityTaintSwarm.java:237 + :231");
    }
}
