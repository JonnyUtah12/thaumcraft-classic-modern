package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaintFibres;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the original {@code EntityTaintSpore} / {@code EntityTaintSporeSwarmer}
 * rules (TC-M6-06-s-f) without a world. Every expectation quotes the decompiled
 * 1.7.10 line it was read from — {@code EntityTaintSpore.java} is the 237-line
 * file, {@code EntityTaintSporeSwarmer.java} the 163-line one — so if someone
 * "cleans up" one of the original's quirks these go red.
 *
 * <p>The test touches no {@code Level}, no registry and no reference tree: the
 * rules are {@link TaintSporeAiLogic}'s, and the block metas are pinned against
 * {@link BlockTaintFibres}'s compile-time constants.</p>
 */
class TaintSporeAiLogicTest {

    // ------------------------------------------------------------ attributes

    @Test
    void originalSporeAttributesAndVoice() {
        // func_110147_ax (:63-64): health 1.0, attack damage 1.0 — a spore dies to
        // one starve tick, which is exactly how the off-biome kill works
        assertEquals(1.0, TaintSporeAiLogic.HEALTH, "EntityTaintSpore.java:63 max health");
        assertEquals(1.0, TaintSporeAiLogic.ATTACK, "EntityTaintSpore.java:64 attack damage");
        // ctor (:31): setSporeSize(2); entityInit (:36): the watcher starts at 1
        assertEquals(2, TaintSporeAiLogic.SPORE_INITIAL_SIZE, "EntityTaintSpore.java:31 ctor size");
        assertEquals(1, TaintSporeAiLogic.SPORE_SIZE_DATA_INIT, "EntityTaintSpore.java:36 watcher init");
        // func_70053_R (:67-69): the eye height is 0.0F
        assertEquals(0.0F, TaintSporeAiLogic.EYE_HEIGHT, "EntityTaintSpore.java:68 eye height");
        // func_70599_aP (:206-208): volume 0.1; func_70627_aG (:210-212): interval 200
        assertEquals(0.1F, TaintSporeAiLogic.SOUND_VOLUME, "EntityTaintSpore.java:207 sound volume");
        assertEquals(200, TaintSporeAiLogic.AMBIENT_SOUND_INTERVAL,
                "EntityTaintSpore.java:211 ambient interval");
    }

    @Test
    void swarmerAttributesOverwriteTheParents() {
        // func_110147_ax (Swarmer:35-36) runs AFTER the spore's, so 75.0 / 1.0 win
        assertEquals(75.0, TaintSporeAiLogic.SWARMER_HEALTH, "EntityTaintSporeSwarmer.java:35 health");
        assertEquals(1.0, TaintSporeAiLogic.SWARMER_ATTACK, "EntityTaintSporeSwarmer.java:36 attack");
        // ctor (Swarmer:21): setSporeSize(10)
        assertEquals(10, TaintSporeAiLogic.SWARMER_INITIAL_SIZE, "EntityTaintSporeSwarmer.java:21 ctor size");
        // setSporeSize override (Swarmer:27): the box is a FIXED 1.0 x 1.0
        assertEquals(1.0F, TaintSporeAiLogic.SWARMER_BOX, "EntityTaintSporeSwarmer.java:27 fixed box");
        // ambient interval 200 (Swarmer:111-114) — the same number the parent answers
        assertEquals(200, TaintSporeAiLogic.AMBIENT_SOUND_INTERVAL,
                "EntityTaintSporeSwarmer.java:113 ambient interval");
    }

    // ---------------------------------------------------------------- the box

    @Test
    void sporeBoxIsZeroFifteenPerSizeWithAHalfBlockFloor() {
        // :51 — Math.max(0.15F * size, 0.5F) on BOTH axes
        assertEquals(0.5F, TaintSporeAiLogic.sporeBoxSize(1), 1.0E-6, ":51 size 1 hits the 0.5 floor");
        assertEquals(0.5F, TaintSporeAiLogic.sporeBoxSize(2), 1.0E-6, ":51 the ctor size 2 is floored");
        assertEquals(0.5F, TaintSporeAiLogic.sporeBoxSize(3), 1.0E-6, ":51 0.45 is under the floor");
        assertEquals(0.6F, TaintSporeAiLogic.sporeBoxSize(4), 1.0E-6, ":51 size 4 clears the floor");
        assertEquals(1.5F, TaintSporeAiLogic.sporeBoxSize(10), 1.0E-6, ":51 a full spore is 1.5");
        // the swarmer's fixed box is SMALLER than a size-10 spore's box (Swarmer:27)
        assertTrue(TaintSporeAiLogic.SWARMER_BOX < TaintSporeAiLogic.sporeBoxSize(10),
                "EntityTaintSporeSwarmer.java:27 the swarmer box does not grow with the size");
    }

    @Test
    void experienceIsTheSporeSize() {
        // :54 — field_70728_aV = par1, rewritten on every size change
        assertEquals(2, TaintSporeAiLogic.xpForSize(2), ":54 a fresh spore pays 2");
        assertEquals(6, TaintSporeAiLogic.xpForSize(6), ":54 the xp tracks the size exactly");
        assertEquals(10, TaintSporeAiLogic.xpForSize(10),
                ":54 a full spore pays 10; Swarmer:29 keeps the write at size 10");
    }

    // ------------------------------------------------------------------ NBT

    @Test
    void nbtRoundTripCarriesTheOffByOneQuirk() {
        // :41 writes `getSporeSize() - 1`; :46 reads `getInt("Size") + 1`
        assertEquals(1, storedSizeTag(2), ":41 the ctor spore stores 1");
        assertEquals(9, storedSizeTag(10), ":41 a full spore stores 9");
        assertEquals(2, readSizeTag(1), ":46 the round trip is stable");
        // an entity loaded WITHOUT the tag reads 0 and lands on size 1, NOT on its
        // constructor size — the original's quirk, kept verbatim
        assertEquals(1, readSizeTag(0), ":46 a missing tag lands on size 1");
    }

    /** {@code EntityTaintSpore.java:41} — {@code tag.putInt("Size", getSporeSize() - 1)}. */
    private static int storedSizeTag(final int sporeSize) {
        return sporeSize - 1;
    }

    /** {@code EntityTaintSpore.java:46} — {@code setSporeSize(tag.getInt("Size") + 1)}. */
    private static int readSizeTag(final int storedTag) {
        return storedTag + 1;
    }

    // --------------------------------------------------------------- growth

    @Test
    void growthLadderStopsAtTenAndFreezesTheCounter() {
        // :123 — getSporeSize() < 10 && growth++ == 1200
        assertEquals(10, TaintSporeAiLogic.SPORE_MAX_SIZE, ":123 the size cap");
        assertEquals(1200, TaintSporeAiLogic.SPORE_GROWTH_TICKS, ":123 the 1200-tick step");
        assertTrue(TaintSporeAiLogic.grows(9, 1200), ":123 size 9 with growth 1200 steps up");
        assertFalse(TaintSporeAiLogic.grows(9, 1199), ":123 one tick early is too early");
        assertFalse(TaintSporeAiLogic.grows(10, 1200), ":123 the cap blocks the step");
        assertFalse(TaintSporeAiLogic.grows(11, 1200), ":123 the cap is strict");
        // the ++ lives inside the short-circuited arm: at the cap the counter freezes
        assertEquals(1200, TaintSporeAiLogic.nextGrowth(10, 1200), ":123 growth freezes at the cap");
        assertEquals(1199, TaintSporeAiLogic.nextGrowth(10, 1199), ":123 at the cap the counter does not move");
        assertEquals(1200, TaintSporeAiLogic.nextGrowth(9, 1199), ":123 under the cap it climbs");
    }

    @Test
    void displaySizeRampsTwoHundredthsAndHolds() {
        // :129-131 — client side, +0.02 a tick until it catches the real size
        assertEquals(0.02F, TaintSporeAiLogic.DISPLAY_SIZE_STEP, 1.0E-6, ":130 the ramp step");
        assertEquals(0.02F, TaintSporeAiLogic.nextDisplaySize(0.0F, 2), 1.0E-6, ":130 ramps from 0");
        assertEquals(1.98F, TaintSporeAiLogic.nextDisplaySize(1.96F, 2), 1.0E-6, ":130 ramps up to the size");
        assertEquals(2.0F, TaintSporeAiLogic.nextDisplaySize(2.0F, 2), 1.0E-6,
                ":131 at the size it holds — and a client that took the spawn float "
                        + "(:199-204) starts there and never ramps");
    }

    @Test
    void swarmFxBudgetsUseIntegerDivision() {
        // :140 — swarm.size() < getSporeSize() / 3
        assertEquals(3, TaintSporeAiLogic.SWARM_FX_DIVISOR, ":140 the spore budget divisor");
        assertFalse(TaintSporeAiLogic.sporeWantsSwarmFx(0, 2), ":140 size 2 / 3 = 0 — a fresh spore has NO budget");
        assertTrue(TaintSporeAiLogic.sporeWantsSwarmFx(0, 3), ":140 size 3 / 3 = 1");
        assertFalse(TaintSporeAiLogic.sporeWantsSwarmFx(1, 5), ":140 size 5 / 3 = 1 — the budget is spent");
        assertTrue(TaintSporeAiLogic.sporeWantsSwarmFx(1, 6), ":140 size 6 / 3 = 2");
        // Swarmer:67 — swarm.size() < (500 - spawnCounter) / 25
        assertEquals(25, TaintSporeAiLogic.SWARMER_FX_DIVISOR, "Swarmer:67 the swarmer budget divisor");
        assertEquals(500, TaintSporeAiLogic.SWARMER_SPAWN_INTERVAL, "Swarmer:17 the cadence");
        assertFalse(TaintSporeAiLogic.swarmerWantsSwarmFx(0, 500), "Swarmer:67 a fresh counter has NO budget");
        assertTrue(TaintSporeAiLogic.swarmerWantsSwarmFx(0, 475), "Swarmer:67 25/25 = 1");
        assertFalse(TaintSporeAiLogic.swarmerWantsSwarmFx(1, 475), "Swarmer:67 the budget is spent");
        assertTrue(TaintSporeAiLogic.swarmerWantsSwarmFx(11, 200), "Swarmer:67 300/25 = 12");
        assertFalse(TaintSporeAiLogic.swarmerWantsSwarmFx(12, 200), "Swarmer:67 at 12 handles the budget is full");
    }

    @Test
    void swarmFxSweepRemovesOneHandlePerTick() {
        // :133-138 — remove the first dead handle, then break
        assertEquals(4, TaintSporeAiLogic.swarmFxAfterCull(5, true), ":136 one handle dies per tick");
        assertEquals(5, TaintSporeAiLogic.swarmFxAfterCull(5, false), ":134 a live list is untouched");
        assertEquals(0, TaintSporeAiLogic.swarmFxAfterCull(0, true), ":135 an empty list stays empty");
    }

    // ------------------------------------------------------- movement / stalk

    @Test
    void matureStalkIsFibresMetaFour() {
        // :89, :148, :179 — the block must BE taint fibres and carry meta 4
        assertTrue(TaintSporeAiLogic.isMatureSporeStalk(true, 4), ":89 fibres meta 4 is the stalk");
        assertFalse(TaintSporeAiLogic.isMatureSporeStalk(true, 3), ":89 meta 3 (the spent stalk) is not");
        assertFalse(TaintSporeAiLogic.isMatureSporeStalk(true, 0), ":89 plain fibres are not");
        assertFalse(TaintSporeAiLogic.isMatureSporeStalk(false, 4), ":89 meta 4 on another block is not");
        // the seam: the literals must stay glued to the port's block metas
        assertEquals(BlockTaintFibres.META_MATURE_SPORE_STALK, TaintSporeAiLogic.FIBRES_META_MATURE_SPORE_STALK,
                ":148 must equal BlockTaintFibres.META_MATURE_SPORE_STALK");
        assertEquals(BlockTaintFibres.META_SPORE_STALK, TaintSporeAiLogic.FIBRES_META_SPORE_STALK,
                ":180 must equal BlockTaintFibres.META_SPORE_STALK");
        assertEquals(4, BlockTaintFibres.META_MATURE_SPORE_STALK, "the fibres ladder's mature rung");
        assertEquals(3, BlockTaintFibres.META_SPORE_STALK, "the fibres ladder's spore-stalk rung");
    }

    @Test
    void travelThrowsAwayHorizontalAndUpwardMotion() {
        // :80-84 — par1 = 0, par5 = 0, and par3 is clamped to 0 when positive
        assertEquals(0.0, TaintSporeAiLogic.travelY(0.5), ":82-84 upward motion is clamped to 0");
        assertEquals(0.0, TaintSporeAiLogic.travelY(0.0), ":84 0 stays 0");
        assertEquals(-0.0852, TaintSporeAiLogic.travelY(-0.0852), ":84 falling motion survives");
    }

    @Test
    void sporeBurstsEveryTickOffAStalkAndWhileHurtOnOne() {
        // :148-152 — off the stalk it bursts unconditionally; on it, while
        // field_70725_aQ (the hurt DURATION) runs
        assertTrue(TaintSporeAiLogic.sporeBursts(false, 0), ":149 off a mature stalk it always bursts");
        assertFalse(TaintSporeAiLogic.sporeBursts(true, 0), ":148-150 rooted and unhurt: no burst");
        assertTrue(TaintSporeAiLogic.sporeBursts(true, 1), ":150 rooted and hurt: burst");
        assertTrue(TaintSporeAiLogic.sporeBursts(true, 10), ":150 the whole flash window bursts");
    }

    @Test
    void offBiomeStarveIsOneHeartEveryTwentyTicks() {
        // :112-117 — server side, tickCount % 20 == 0, biome != taint
        assertEquals(20, TaintSporeAiLogic.STARVE_INTERVAL, ":113 the cadence");
        assertEquals(1.0F, TaintSporeAiLogic.STARVE_DAMAGE, ":116 DamageSource.field_76366_f (starve)");
        assertTrue(TaintSporeAiLogic.starves(20, false), ":117 off-biome on the cadence tick");
        assertFalse(TaintSporeAiLogic.starves(21, false), ":113 off the cadence");
        assertFalse(TaintSporeAiLogic.starves(20, true), ":115 inside the taint biome: no starve");
        assertTrue(TaintSporeAiLogic.starves(0, false),
                ":113 tick 0 is on the cadence (0 % 20 == 0): a spore spawned outside the "
                        + "taint biome takes its first starve heart on its very first tick");
    }

    // --------------------------------------------------------------- bursting

    @Test
    void burstCountIsSizeThirdsPlusTheJitterRoll() {
        // :162 — q = size / 3 + rand.nextInt(size / 2 + 1)
        assertEquals(3, TaintSporeAiLogic.burstCount(10, 0), ":162 10/3 = 3 spiderlings at minimum");
        assertEquals(8, TaintSporeAiLogic.burstCount(10, 5), ":162 plus the roll");
        assertEquals(6, TaintSporeAiLogic.burstRollBound(10), ":162 nextInt(10/2 + 1) = nextInt(6)");
        assertEquals(0, TaintSporeAiLogic.burstCount(2, 0), ":162 a fresh spore spawns none at minimum");
        assertEquals(2, TaintSporeAiLogic.burstRollBound(2), ":162 nextInt(2/2 + 1) = nextInt(2)");
        assertEquals(1, TaintSporeAiLogic.burstCount(3, 0), ":162 size 3 = one spiderling");
    }

    @Test
    void goreCueUsesTheWorldRandomPitch() {
        // :161, Swarmer:83 — volume 1.0F, pitch 0.9F + world.rand.nextFloat() * 0.1F
        assertEquals(1.0F, TaintSporeAiLogic.GORE_VOLUME, ":161 gore volume");
        assertEquals(0.9F, TaintSporeAiLogic.gorePitch(0.0F), 1.0E-6, ":161 pitch floor");
        assertEquals(1.0F, TaintSporeAiLogic.gorePitch(1.0F), 1.0E-6, ":161 pitch ceiling");
        assertEquals(0.95F, TaintSporeAiLogic.gorePitch(0.5F), 1.0E-6, ":161 the roll scales by 0.1");
    }

    @Test
    void spllooshAmountsArePerCallSite() {
        // :185 client burst = 50; Swarmer:41 hurt = 10; Swarmer:99 burst event = 25
        assertEquals(50, TaintSporeAiLogic.BURST_SPLLOOSH, ":185 the client burst pops 50");
        assertEquals(10, TaintSporeAiLogic.SWARMER_HURT_SPLLOOSH, "Swarmer:41 a hit pops 10");
        assertEquals(25, TaintSporeAiLogic.SWARMER_EVENT_SPLLOOSH, "Swarmer:99 the event pops 25");
    }

    // ------------------------------------------------------------ swarmer loop

    @Test
    void swarmerCounterIsClampedAtZeroAndReArmsOnBurst() {
        // Swarmer:50-52 — if (spawnCounter > 0) spawnCounter--
        assertEquals(499, TaintSporeAiLogic.swarmerNextSpawnCounter(500), "Swarmer:51 the count drops");
        assertEquals(1, TaintSporeAiLogic.swarmerNextSpawnCounter(2), "Swarmer:51 the count drops");
        assertEquals(0, TaintSporeAiLogic.swarmerNextSpawnCounter(0),
                "Swarmer:50 at 0 it never goes negative");
        // Swarmer:54-57 — the burst needs the counter spent AND a player within 16
        assertEquals(16.0, TaintSporeAiLogic.SWARMER_PLAYER_SCAN, "Swarmer:54 getClosestPlayerToEntity range");
        assertTrue(TaintSporeAiLogic.swarmerBursts(0, true), "Swarmer:54 spent counter + player: burst");
        assertFalse(TaintSporeAiLogic.swarmerBursts(0, false), "Swarmer:54 no player: no burst");
        assertFalse(TaintSporeAiLogic.swarmerBursts(1, true), "Swarmer:55 the counter is not spent yet");
        assertEquals(500, TaintSporeAiLogic.SWARMER_SPAWN_INTERVAL, "Swarmer:55 the re-arm value");
    }

    @Test
    void swarmerHurtBurstFiresOnDurationOneExactly() {
        // Swarmer:72 — if (field_70725_aQ == 1) swarmBurst(1): exactly once per hit
        assertTrue(TaintSporeAiLogic.swarmerBurstsOnHurt(1), "Swarmer:72 duration 1 fires");
        assertFalse(TaintSporeAiLogic.swarmerBurstsOnHurt(2), "Swarmer:72 duration 2 does not");
        assertFalse(TaintSporeAiLogic.swarmerBurstsOnHurt(0), "Swarmer:72 duration 0 does not");
        assertFalse(TaintSporeAiLogic.swarmerBurstsOnHurt(10), "Swarmer:72 the flash start does not");
    }

    @Test
    void swarmBurstEventAndOffset() {
        // Swarmer:87 the swarm spawns at y + 0.5; Swarmer:91/:97 the event byte is 6
        assertEquals(0.5, TaintSporeAiLogic.SWARM_SPAWN_Y_OFFSET, "Swarmer:87 the swarm spawn offset");
        assertEquals((byte) 6, TaintSporeAiLogic.SWARMER_BURST_EVENT,
                "Swarmer:91 world.entityEvent(this, (byte) 6)");
        assertEquals(2, TaintSporeAiLogic.SWARMER_DROP_ROLLS, "Swarmer:155 for (a = 0; a <= 1; a++)");
    }

    // ------------------------------------------------------- renderer-side rules

    @Test
    void renderGateSquaresTheDistanceSoSixtyFourBlocks() {
        // :97-99 — isInRangeToRenderDist(d) : d < 4096.0, and 1.7.10 passes d*SQUARED
        assertEquals(4096.0, TaintSporeAiLogic.RENDER_DISTANCE_SQ, ":98 the gate literal");
        assertTrue(TaintSporeAiLogic.isWithinRenderDistance(4095.0), ":98 63.98 blocks renders");
        assertFalse(TaintSporeAiLogic.isWithinRenderDistance(4096.0), ":98 exactly 64 blocks does not");
        assertFalse(TaintSporeAiLogic.isWithinRenderDistance(4097.0), ":98 beyond it does not");
        assertEquals(64.0, Math.sqrt(TaintSporeAiLogic.RENDER_DISTANCE_SQ), ":98 the block radius");
    }

    @Test
    void sporeLightingIsFlatAndTheOverlayIsFullBright() {
        // :101-104 getBrightness(partial) : 15728880; :106-108 getBrightness(float) : 1.0F
        assertEquals(15728880, TaintSporeAiLogic.OVERLAY_COLOR, ":103 the spore overlay colour");
        assertEquals(1.0F, TaintSporeAiLogic.SPORE_BRIGHTNESS, ":107 the spore brightness");
    }

    @Test
    void swarmerSamplesItsOwnColumnSixtySixPercentUp() {
        // Swarmer:127, :140 — d0 = (aabb.maxY - aabb.minY) * 0.66
        assertEquals(0.66F, TaintSporeAiLogic.BRIGHTNESS_SAMPLE_FRACTION, 1.0E-6, "Swarmer:127 the 0.66 factor");
        // Swarmer:128, :141 — k = floor(posY - eyeHeight + d0), the spore's eyeHeight is 0
        assertEquals(64, TaintSporeAiLogic.brightnessSampleY(64.0, 0.0F, 1.0F),
                "Swarmer:141 a 1.0 box samples 0.66 up, inside the cell it stands in");
        assertEquals(65, TaintSporeAiLogic.brightnessSampleY(64.0, 0.25F, 3.0F),
                "Swarmer:141 a 3.0 box 0.25 eye height samples 64 - 0.25 + 1.98");
        assertEquals(-1, TaintSporeAiLogic.brightnessSampleY(-1.0, 0.0F, 1.0F),
                "Swarmer:141 floor(), not truncation, below zero");
    }

    @Test
    void swarmerBrightnessNeedsTheSkyGateAndFallsBackToZero() {
        // Swarmer:136-146 — canBlockSeeSky(i, 0, j) gates the sample; else 0.0F
        assertEquals(1.0F, TaintSporeAiLogic.swarmerBrightness(15, true), "Swarmer:142 full light is 1.0");
        assertEquals(0.0F, TaintSporeAiLogic.swarmerBrightness(15, false), "Swarmer:144 no sky gate: 0.0F");
        assertEquals(0.0F, TaintSporeAiLogic.swarmerBrightness(0, true), "Swarmer:142 pitch dark is 0.0");
        assertEquals(7.0F / 15.0F, TaintSporeAiLogic.swarmerBrightness(7, true), 1.0E-6,
                "Swarmer:142 the light level is normalised by 15");
        // Swarmer:121-133 — the overlay lightmap takes the same gate, else 0
        assertEquals(0xF000F0, TaintSporeAiLogic.swarmerOverlayLightmap(0xF000F0, true),
                "Swarmer:129 the sampled lightmap passes through");
        assertEquals(0, TaintSporeAiLogic.swarmerOverlayLightmap(0xF000F0, false),
                "Swarmer:131 no sky gate: overlay 0");
    }

    // ---------------------------------------------------------------- drops

    @Test
    void dropsAreGooOrFungusAndTheSwarmerRollsTwice() {
        // :231-235 — coin flip between itemResource meta 11 (goo) and 12 (fungus)
        assertEquals(11, TaintSporeAiLogic.META_TAINTED_GOO, ":232 itemResource meta 11");
        assertEquals(12, TaintSporeAiLogic.META_TAINTED_FUNGUS, ":234 itemResource meta 12");
        // Swarmer:155-161 — for (a = 0; a <= 1; a++), each roll independent
        assertEquals(2, TaintSporeAiLogic.SWARMER_DROP_ROLLS, "Swarmer:155 two independent rolls");
    }

    @Test
    void dropOffsetIsTheEntityHeightHalf() {
        // :232 — field_70131_O / 2.0F; 1.7.10's field_70131_O is the setSize HEIGHT
        assertEquals(0.25F, TaintSporeAiLogic.dropOffset(0.5F), ":232 a floored spore drops from 0.25");
        assertEquals(0.75F, TaintSporeAiLogic.dropOffset(1.5F), ":232 a size-10 spore drops from 0.75");
        assertEquals(0.5F, TaintSporeAiLogic.dropOffset(1.0F),
                "Swarmer:157 the swarmer's 1.0 box (Swarmer:27) drops from 0.5");
    }
}
