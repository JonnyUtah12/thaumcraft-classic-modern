package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintacleAiLogic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Giant Taintacle rules (content-mapping.csv {@code thaumcraft:taintaclegiant})
 * without a world. Every expectation quotes the decompiled line it was read from —
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityTaintacleGiant.java}
 * (131 lines) plus the helpers it calls — so "cleaning up" one of the original's
 * quirks turns this suite red.
 */
class TaintacleGiantAiLogicTest {

    // --------------------------------------------------- box, xp, attributes (:22-33)

    @Test
    void constructorBoxAndXpReward() {
        // func_70105_a(1.1F, 6.0F)
        assertEquals(1.1F, TaintacleGiantAiLogic.WIDTH, "EntityTaintacleGiant.java:24 setSize(1.1F, 6.0F) width");
        assertEquals(6.0F, TaintacleGiantAiLogic.HEIGHT, "EntityTaintacleGiant.java:24 setSize(1.1F, 6.0F) height");
        // this.field_70728_aV = 20
        assertEquals(20, TaintacleGiantAiLogic.XP, "EntityTaintacleGiant.java:25 xpReward 20");
    }

    @Test
    void attributeOverridesAreHealthAndAttackOnly() {
        // func_110147_ax: super, then 125.0 health and 9.0 attack
        assertEquals(125.0, TaintacleGiantAiLogic.HEALTH, "EntityTaintacleGiant.java:31 MAX_HEALTH 125.0");
        assertEquals(9.0, TaintacleGiantAiLogic.ATTACK, "EntityTaintacleGiant.java:32 attack 9.0");
    }

    @Test
    void theFamilyReachRulesRideOnTheBossHealth() {
        // The giant inherits the family's max-health-derived numbers, so the 125 of
        // EntityTaintacleGiant.java:31 makes its melee reach 125 blocks
        // (EntityTaintacle.java:154), its scan 750x375x750 (EntityTaintacle.java:78)
        // and its agitation radius 875 (EntityTaintacle.java:252).
        final float health = (float) TaintacleGiantAiLogic.HEALTH;
        assertTrue(TaintacleAiLogic.canMelee(0, 125.0F, health, true),
                "EntityTaintacleGiant.java:31 health 125 is the melee reach (EntityTaintacle.java:154)");
        assertFalse(TaintacleAiLogic.canMelee(0, 125.1F, health, true), "reach is inclusive at 125");
        assertTrue(TaintacleAiLogic.agitated(true, 875.0 * 875.0 - 1.0, health),
                "agitation radius is maxHealth*7 = 875 (EntityTaintacle.java:252)");
        assertFalse(TaintacleAiLogic.agitated(true, 875.0 * 875.0, health), "the squared compare is strict");
        // EntityTaintacle.java:78 inflates a point box, so maxHealth*6 is the
        // half-extent: the box is 1500 wide and 750 tall for a giant.
        assertEquals(1500.0, TaintacleAiLogic.targetScanBox(0.0, 0.0, 0.0, health).getXsize(),
                "EntityTaintacle.java:78 half-extent maxHealth*6 = 750 -> 1500 across");
        assertEquals(750.0, TaintacleAiLogic.targetScanBox(0.0, 0.0, 0.0, health).getYsize(),
                "EntityTaintacle.java:78 half-extent maxHealth*3 = 375 -> 750 tall");
    }

    @Test
    void bossSoundVolumeAndPitchComeFromTheBossHealth() {
        // EntityTaintacle.java:287-289 volume = maxHealth/8; :283-285 pitch = 1.3 - maxHealth/10.
        // With the giant's 125 (EntityTaintacleGiant.java:31) the pitch is -11.2: the
        // negative pitch is a family quirk and ships verbatim.
        assertEquals(15.625F, TaintacleAiLogic.soundVolume(125.0F), 1.0E-5F, "125/8 = 15.625");
        assertEquals(-11.2F, TaintacleAiLogic.soundPitch(125.0F), 1.0E-4F, "1.3 - 125/10 = -11.2, kept negative");
    }

    // --------------------------------------------------------------- anger (:68-79)

    @Test
    void angerCounterTicksDownOnlyAboveZero() {
        // if (getAnger() > 0) setAnger(getAnger() - 1)
        assertEquals(199, TaintacleGiantAiLogic.angerAfterTick(200), "EntityTaintacleGiant.java:44 decrements by 1");
        assertEquals(1, TaintacleGiantAiLogic.angerAfterTick(2));
        assertEquals(0, TaintacleGiantAiLogic.angerAfterTick(0), "EntityTaintacleGiant.java:43 the > 0 guard");
        assertEquals(-5, TaintacleGiantAiLogic.angerAfterTick(-5), "a negative anger never moves");
    }

    @Test
    void angerIsAShortAndNarrowsOnTheWayIn() {
        // func_70096_w().add(14, new Short((short)0)); setAnger writes (short) par1
        assertEquals(14, TaintacleGiantAiLogic.ANGER_WATCHER_ID, "EntityTaintacleGiant.java:70 watcher id 14");
        assertEquals(200, TaintacleGiantAiLogic.angerAsShort(200), "EntityTaintacleGiant.java:78 cast");
        assertEquals(4464, TaintacleGiantAiLogic.angerAsShort(70000),
                "EntityTaintacleGiant.java:78 narrows to 16 bits: 70000 -> 4464");
        assertEquals(-1, TaintacleGiantAiLogic.angerAsShort(65535), "the narrowing wraps, kept verbatim");
        assertEquals(300, TaintacleGiantAiLogic.angerFromShort((short) 300), "EntityTaintacleGiant.java:74 reads the Short back");
    }

    @Test
    void angerSurvivesSaveLoad() {
        // 1.7.10 serialised the data watcher for free; the port needs a key.
        assertEquals("Anger", TaintacleGiantAiLogic.ANGER_NBT_KEY,
                "port-chosen key: EntityTaintacleGiant.java:68-79 registers watcher 14 and never touches NBT");
    }

    // ------------------------------------------------------------ enrage (:109-130)

    @Test
    void enrageNeedsMoreThanThirtyFiveDamageOnTheServerWithNoAnger() {
        // if (!world.isRemote && damage > 35.0F) { if (getAnger() == 0) {...} damage = 35.0F; }
        assertTrue(TaintacleGiantAiLogic.canEnrage(35.5F, 0, false), "EntityTaintacleGiant.java:110-111");
        assertFalse(TaintacleGiantAiLogic.canEnrage(35.0F, 0, false), "the compare is strict: > 35.0F");
        assertFalse(TaintacleGiantAiLogic.canEnrage(35.5F, 1, false), "EntityTaintacleGiant.java:111 anger must be 0");
        assertFalse(TaintacleGiantAiLogic.canEnrage(35.5F, 0, true), "EntityTaintacleGiant.java:110 !isRemote");
    }

    @Test
    void theThirtyFiveCapIsServerSideOnly() {
        assertEquals(35.0F, TaintacleGiantAiLogic.cappedDamage(60.0F, false),
                "EntityTaintacleGiant.java:126 damage = 35.0F");
        assertEquals(60.0F, TaintacleGiantAiLogic.cappedDamage(60.0F, true),
                "EntityTaintacleGiant.java:110 the cap is inside the !isRemote branch");
        assertEquals(35.0F, TaintacleGiantAiLogic.cappedDamage(35.0F, false), "35.0 is not > 35.0, uncapped");
        assertEquals(20.0F, TaintacleGiantAiLogic.cappedDamage(20.0F, false), "small hits pass through");
    }

    @Test
    void enrageEffectsAreRegenStrengthSpeedForTwoHundredTicks() {
        // Potion.field_76428_l = potion id 10 "potion.regeneration"; field_76420_g =
        // id 5 "potion.damageBoost"; field_76424_c = id 1 "potion.moveSpeed"
        assertEquals("minecraft:regeneration", TaintacleGiantAiLogic.ENRAGE_EFFECT_REGENERATION_ID,
                "EntityTaintacleGiant.java:113 Potion.field_76428_l (id 10, colour 13458603)");
        assertEquals("minecraft:strength", TaintacleGiantAiLogic.ENRAGE_EFFECT_STRENGTH_ID,
                "EntityTaintacleGiant.java:114 Potion.field_76420_g (id 5, potion.damageBoost)");
        assertEquals("minecraft:speed", TaintacleGiantAiLogic.ENRAGE_EFFECT_SPEED_ID,
                "EntityTaintacleGiant.java:115 Potion.field_76424_c (id 1, potion.moveSpeed)");
        assertEquals(200, TaintacleGiantAiLogic.ENRAGE_EFFECT_DURATION, "EntityTaintacleGiant.java:113-115 duration 200");
    }

    @Test
    void enrageAmplifiersDivideByFifteenThenForty() {
        // new PotionEffect(id, 200, (int)(damage / 15.0F)) / (int)(damage / 40.0F)
        assertEquals(2, TaintacleGiantAiLogic.regenerationAmplifier(40.0F), "EntityTaintacleGiant.java:113 (int)(40/15) = 2");
        assertEquals(3, TaintacleGiantAiLogic.regenerationAmplifier(45.0F), "EntityTaintacleGiant.java:113 (int)(45/15) = 3");
        assertEquals(1, TaintacleGiantAiLogic.strengthAmplifier(40.0F), "EntityTaintacleGiant.java:114 40/40 = 1");
        assertEquals(0, TaintacleGiantAiLogic.strengthAmplifier(39.0F), "the cast truncates");
        assertEquals(1, TaintacleGiantAiLogic.speedAmplifier(40.0F), "EntityTaintacleGiant.java:115 same divisor as :114");
        assertEquals(11, TaintacleGiantAiLogic.regenerationAmplifier(179.0F), "179/15 truncates to 11");
    }

    @Test
    void enrageChatUsesTheTcBossEnrageKey() {
        // func_145747_a(new ChatComponentText(getEntityName() + " " + StatCollector.translateToLocal("tc.boss.enrage")))
        assertEquals("tc.boss.enrage", TaintacleGiantAiLogic.ENRAGE_LANG_KEY, "EntityTaintacleGiant.java:122");
    }

    // ------------------------------------------------- anger particles (:47-60)

    @Test
    void angerParticleNeedsAFifteenthRollAndAnger() {
        // if (world.isRemote && rand.nextInt(15) == 0 && getAnger() > 0)
        assertEquals(15, TaintacleGiantAiLogic.PARTICLE_ROLL_BOUND, "EntityTaintacleGiant.java:47 nextInt(15)");
        assertTrue(TaintacleGiantAiLogic.rollsAngerParticle(0, 1), "EntityTaintacleGiant.java:47");
        assertFalse(TaintacleGiantAiLogic.rollsAngerParticle(0, 0), "the anger > 0 guard");
        assertFalse(TaintacleGiantAiLogic.rollsAngerParticle(1, 200), "roll 1 of 15 does not spawn one");
    }

    @Test
    void angerParticleVelocitiesAreTwoHundredthsOfAGaussian() {
        assertEquals(0.02, TaintacleGiantAiLogic.particleVelocity(1.0), 1.0E-12,
                "EntityTaintacleGiant.java:48-50 nextGaussian() * 0.02");
        assertEquals(-0.04, TaintacleGiantAiLogic.particleVelocity(-2.0), 1.0E-12, "the sign rides through");
        assertEquals(0.0, TaintacleGiantAiLogic.particleVelocity(0.0), 1.0E-12);
    }

    @Test
    void angerParticleSpreadsAcrossTheBoxWidth() {
        // x = posX + rand*height_field_N - N/2 (EntityTaintacleGiant.java:54, :56)
        assertEquals(10.55, TaintacleGiantAiLogic.particleAxis(10.0, 1.1, 1.0), 1.0E-12,
                "EntityTaintacleGiant.java:54 roll 1 -> x + 1.1 - 0.55");
        assertEquals(9.45, TaintacleGiantAiLogic.particleAxis(10.0, 1.1, 0.0), 1.0E-12, "roll 0 -> x - 0.55");
    }

    @Test
    void angerParticleHeightUsesTheBoxHeightNotMaxHealth() {
        // y = boundingBox.minY + field_70131_O + rand*0.5 (EntityTaintacleGiant.java:55).
        // field_70131_O is Entity.height — the 6.0 from setSize(1.1F, 6.0F) at :24, the
        // same field the drop height uses at :92 — NOT maxHealth (the card brief reads
        // it as 125; the field is the box). Pinned so the reading is testable.
        assertEquals(6.5, TaintacleGiantAiLogic.particleY(0.0, 6.0, 1.0), 1.0E-12,
                "EntityTaintacleGiant.java:55 uses 6.0 (box height) + 0.5, not 125 + 0.5");
        assertEquals(6.0, TaintacleGiantAiLogic.particleY(0.0, 6.0, 0.0), 1.0E-12, "minY + height at roll 0");
        assertEquals(131.0, TaintacleGiantAiLogic.particleY(125.0, 6.0, 1.0), 1.0E-12, "minY rides through");
        assertEquals(1.0F, TaintacleGiantAiLogic.PARTICLE_Y_HEIGHT_MULTIPLIER, "1x the box height");
        assertEquals(0.5F, TaintacleGiantAiLogic.PARTICLE_Y_JITTER_SPAN, "EntityTaintacleGiant.java:55 rand*0.5");
    }

    // ------------------------------------------------------------- regen (:63-65)

    @Test
    void regenIsEveryThirtiethTickForOneHeart() {
        // if (!isRemote && field_70173_aa % 30 == 0) func_70691_i(1.0F)
        assertEquals(30, TaintacleGiantAiLogic.REGEN_INTERVAL, "EntityTaintacleGiant.java:63 % 30");
        assertEquals(1.0F, TaintacleGiantAiLogic.REGEN_AMOUNT, "EntityTaintacleGiant.java:64 heal(1.0F)");
        assertTrue(TaintacleGiantAiLogic.regenerates(0), "tick 0 satisfies % 30 == 0 (no age guard)");
        assertTrue(TaintacleGiantAiLogic.regenerates(30));
        assertTrue(TaintacleGiantAiLogic.regenerates(60));
        assertFalse(TaintacleGiantAiLogic.regenerates(29), "EntityTaintacleGiant.java:63 off-cadence ticks heal nothing");
    }

    // ---------------------------------------------------------- champion (:35-38)

    @Test
    void everyGiantRollsChampionAndOnlyThePersistHalfIsPorted() {
        // EntityUtils.makeChampion(this, true) — unconditional, persist = true
        assertTrue(TaintacleGiantAiLogic.championRolledOnEverySpawn(),
                "EntityTaintacleGiant.java:36 makeChampion runs on every onInitialSpawn, no config gate here");
        assertTrue(TaintacleGiantAiLogic.CHAMPION_PERSIST,
                "EntityTaintacleGiant.java:36 the persist flag -> EntityUtils.java:303-305 setPersistenceRequired");
        assertEquals(13, TaintacleGiantAiLogic.CHAMPION_MOD_COUNT,
                "ChampionModifier.java:12-26 mods.length is 13 (bold..infested)");
        assertEquals(25.0, TaintacleGiantAiLogic.CHAMPION_HEAL,
                "EntityUtils.java:297 heal(25.0F) — pinned, out of scope for this card");
    }

    @Test
    void championModRollIsModuloThirteen() {
        // int type = world.rand.nextInt(ChampionModifier.mods.length)
        assertEquals(0, TaintacleGiantAiLogic.championModIndex(0), "EntityUtils.java:282");
        assertEquals(12, TaintacleGiantAiLogic.championModIndex(12));
        assertEquals(0, TaintacleGiantAiLogic.championModIndex(13), "the roll is modulo the 13 mods");
        assertEquals(12, TaintacleGiantAiLogic.championModIndex(-1), "never indexes out of range");
        assertEquals(0, TaintacleGiantAiLogic.championModIndexForced(7, true),
                "EntityUtils.java:283-285 a creeper is forced to mod 0");
        assertEquals(7, TaintacleGiantAiLogic.championModIndexForced(7, false), "a giant is not a creeper");
    }

    // --------------------------------------------------------- death drop (:86-94)

    @Test
    void aLoneGiantDropsItsEldritchObject() {
        // if (ents == null || ents.size() <= 0) entityDropSpecialItem(...)
        assertTrue(TaintacleGiantAiLogic.dropsEldritchObject(0), "EntityTaintacleGiant.java:91 nobody nearby");
        assertFalse(TaintacleGiantAiLogic.dropsEldritchObject(1), "a neighbour giant cancels the drop");
        assertFalse(TaintacleGiantAiLogic.dropsEldritchObject(2));
        assertEquals(48.0, TaintacleGiantAiLogic.DEATH_DROP_SCAN_RANGE, "EntityTaintacleGiant.java:89 range 48.0");
        assertEquals(3, TaintacleGiantAiLogic.DEATH_DROP_META,
                "EntityTaintacleGiant.java:92 new ItemStack(ConfigItems.itemEldritchObject, 1, 3)");
    }

    @Test
    void dropScanIsACubeAndTheDropRidesHalfTheBoxHeight() {
        // EntityUtils.java:223 expands the point box by (range, range, range)
        final var box = TaintacleGiantAiLogic.deathDropScanBox(10.0, 20.0, 30.0);
        assertEquals(-38.0, box.minX, 1.0E-9, "EntityUtils.java:223 inflate(48,48,48)");
        assertEquals(58.0, box.maxX, 1.0E-9);
        assertEquals(-28.0, box.minY, 1.0E-9, "y inflates by the full 48 too");
        assertEquals(68.0, box.maxY, 1.0E-9);
        // :92 dropheight = field_70131_O / 2.0F = 6.0 / 2
        assertEquals(3.0F, TaintacleGiantAiLogic.deathDropOffset(6.0F), "EntityTaintacleGiant.java:92 height/2");
        assertEquals(2.5F, TaintacleGiantAiLogic.deathDropOffset(5.0F));
    }

    // ------------------------------------------------------------- gates (:80-106)

    @Test
    void gatesAndTheirPinnedQuirks() {
        assertFalse(TaintacleGiantAiLogic.NATURAL_SPAWN_ALLOWED,
                "EntityTaintacleGiant.java:80-84 func_70601_bi() returns false");
        assertTrue(TaintacleGiantAiLogic.DECLARES_INVULNERABLE,
                "EntityTaintacleGiant.java:100-102 func_70648_aU() returns true; 21.1.259's only seam is "
                        + "isInvulnerableTo(DamageSource), which would gate the damage path, so the rule "
                        + "stays pinned and unwired");
        assertFalse(TaintacleGiantAiLogic.LOOTING_BONUS_ALLOWED,
                "EntityTaintacleGiant.java:96-98 func_70692_ba() returns false; wiring it to "
                        + "LivingEntity#shouldDropLoot() would also kill the code-driven death drop "
                        + "(LivingEntity.java:1465-1468), so the looting gate is pinned, not wired");
        assertFalse(TaintacleGiantAiLogic.AIR_DRAINS,
                "EntityTaintacleGiant.java:104-106 func_70682_h(air) returns air");
        assertEquals(3456, TaintacleGiantAiLogic.airSupplyAfterTick(3456), "the air counter never moves");
        assertEquals(0, TaintacleGiantAiLogic.airSupplyAfterTick(0), "so it never drowns at 0 either");
    }

    // ------------------------------------------------------------- boss bar (:21)

    @Test
    void bossBarTracksTheHealthFraction() {
        // implements IBossDisplayData (EntityTaintacleGiant.java:21) — the bar is the
        // health fraction; 21.1.259 feeds a ServerBossEvent (no Boss/BossBar type).
        assertEquals(1.0F, TaintacleGiantAiLogic.bossHealthProgress(125.0F, 125.0F), "full health");
        assertEquals(0.5F, TaintacleGiantAiLogic.bossHealthProgress(62.5F, 125.0F), "half health");
        assertEquals(0.0F, TaintacleGiantAiLogic.bossHealthProgress(0.0F, 125.0F), "dead");
        assertEquals(0.0F, TaintacleGiantAiLogic.bossHealthProgress(10.0F, 0.0F), "no maxHealth, no NaN");
        assertEquals("entity.thaumcraft.taintaclegiant", TaintacleGiantAiLogic.BOSS_BAR_NAME_KEY,
                "the boss name lang key (the catalog parent wires the row)");
    }
}
