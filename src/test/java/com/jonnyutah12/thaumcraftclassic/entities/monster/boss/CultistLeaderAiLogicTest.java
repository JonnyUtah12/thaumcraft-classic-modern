package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Praetor world-free (TC-M6-06-s-c5-1) — no level, no entity instance,
 * no registry. Every expectation quotes the decompiled line it was read from in
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityCultistLeader.java}
 * (203 lines) — so "cleaning up" one of the original's figures (a title, a goal
 * slot, a constructor argument, a light gate, an aura radius, an amplifier, an
 * aim lift, a sound pitch pair, a slot ordinal, a meta) turns a test red. The
 * shipped wiring is proven by {@code BossMobGameTest}.
 */
class CultistLeaderAiLogicTest {

    private static final float F = 1.0E-4F;

    // ------------------------------------------------------- registration figures

    @Test
    void theRegistrationRowIsTheOriginals() {
        // registerModEntity(EntityCultistLeader.class, "CultistLeader", 29,
        // instance, 64, 3, true) — ConfigEntities.java:115
        assertEquals(64, CultistLeaderAiLogic.TRACK_RANGE_BLOCKS);
        assertEquals(4, CultistLeaderAiLogic.TRACK_RANGE_CHUNKS);       // 64 blocks = 4 chunks
        assertEquals(3, CultistLeaderAiLogic.UPDATE_INTERVAL);
        assertTrue(CultistLeaderAiLogic.VELOCITY_UPDATES);
        // :56 func_70105_a(0.75F, 2.25F)
        assertEquals(0.75F, CultistLeaderAiLogic.WIDTH, F);
        assertEquals(2.25F, CultistLeaderAiLogic.HEIGHT, F);
        // :66 field_70728_aV = 40 (over the base's 50)
        assertEquals(40, CultistLeaderAiLogic.EXPERIENCE);
    }

    @Test
    void theAttributesAreTheOriginalsOverTheBossBasePair() {
        // :72-74 — speed 0.32, maxHealth 125.0, attack 5.0
        assertEquals(0.32, CultistLeaderAiLogic.MOVEMENT_SPEED, 1.0E-9);
        assertEquals(125.0, CultistLeaderAiLogic.MAX_HEALTH, 1.0E-9);
        assertEquals(5.0, CultistLeaderAiLogic.ATTACK_DAMAGE, 1.0E-9);
        // the super pass (:71) carries the base's pair — EntityThaumcraftBoss:53-54
        assertEquals(0.95, ThaumcraftBossAiLogic.KNOCKBACK_RESISTANCE, 1.0E-9);
        assertEquals(40.0, ThaumcraftBossAiLogic.FOLLOW_RANGE, 1.0E-9);
    }

    // ------------------------------------------------------- goal table (:57-65)

    @Test
    void everyGoalSitsAtItsOriginalSlot() {
        assertEquals(0, CultistLeaderAiLogic.GOAL_SLOT_SWIM);           // :57
        assertEquals(2, CultistLeaderAiLogic.GOAL_SLOT_RANGED);         // :58
        assertEquals(3, CultistLeaderAiLogic.GOAL_SLOT_MELEE);          // :59
        assertEquals(6, CultistLeaderAiLogic.GOAL_SLOT_RESTRICTION);    // :60
        assertEquals(7, CultistLeaderAiLogic.GOAL_SLOT_WANDER);         // :61
        assertEquals(8, CultistLeaderAiLogic.GOAL_SLOT_WATCH);          // :62
        assertEquals(8, CultistLeaderAiLogic.GOAL_SLOT_LOOK_IDLE);      // :63
        assertEquals(1, CultistLeaderAiLogic.TARGET_SLOT_HURT);         // :64
        assertEquals(2, CultistLeaderAiLogic.TARGET_SLOT_PLAYER);       // :65
    }

    @Test
    void theRangedGoalCarriesTheLeadersOwnConstructorArguments() {
        // :58 AILongRangeAttack(this, 16.0, 1.0, 30, 40, 24.0F)
        assertEquals(16.0, CultistLeaderAiLogic.RANGED_MIN_DISTANCE, 1.0E-9);
        assertEquals(1.0, CultistLeaderAiLogic.RANGED_SPEED, 1.0E-9);
        assertEquals(30, CultistLeaderAiLogic.RANGED_INTERVAL_MIN);
        assertEquals(40, CultistLeaderAiLogic.RANGED_INTERVAL_MAX);
        assertEquals(24.0F, CultistLeaderAiLogic.RANGED_RANGE, F);
    }

    @Test
    void theMeleeRestrictionWanderWatchAndTargetFiguresAreTheOriginals() {
        // :59 AIAttackOnCollide(this, EntityLivingBase.class, 1.1, false)
        assertEquals(1.1, CultistLeaderAiLogic.MELEE_SPEED, 1.0E-9);
        assertFalse(CultistLeaderAiLogic.MELEE_LONG_MEMORY);
        // :60-61 MoveTowardsRestriction(0.8), Wander(0.8)
        assertEquals(0.8, CultistLeaderAiLogic.RESTRICTION_SPEED, 1.0E-9);
        assertEquals(0.8, CultistLeaderAiLogic.WANDER_SPEED, 1.0E-9);
        // :62 EntityAIWatchClosest(this, EntityPlayer.class, 8.0F)
        assertEquals(8.0F, CultistLeaderAiLogic.WATCH_PLAYER_RANGE, F);
        // the family twin's watch probability (Knight:32 idiom)
        assertEquals(0.02F, CultistLeaderAiLogic.WATCH_PLAYER_PROBABILITY, F);
        // :64 AICultistHurtByTarget(this, true)
        assertTrue(CultistLeaderAiLogic.HURT_IS_AGRESSORS);
        // :65 EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true)
        assertEquals(0, CultistLeaderAiLogic.TARGET_SORT_TYPE);
        assertTrue(CultistLeaderAiLogic.TARGET_MUST_SEE);
    }

    // ------------------------------------------------------- titles (:36-52, :80-99, :101-111)

    @Test
    void theFifteenTitlesAreVerbatimAndInOrder() {
        // :36-52 — the exact array, element by element, in the original's order.
        final String[] expected = {
            "Alberic", "Anselm", "Bastian", "Beturian", "Chabier", "Chorache", "Chuse",
            "Dodorol", "Ebardo", "Ferrando", "Fertus", "Guillen", "Larpe", "Obano", "Zelipe",
        };
        assertEquals(expected.length, CultistLeaderAiLogic.TITLES.length);
        for (int a = 0; a < expected.length; a++) {
            assertEquals(expected[a], CultistLeaderAiLogic.TITLES[a],
                    "EntityCultistLeader.java:" + (37 + a) + " — title " + a);
        }
        // :80 addObject(16, (byte) 0); :104/:110 the NBT key
        assertEquals(16, CultistLeaderAiLogic.TITLE_WATCHER_INDEX);
        assertEquals("title", CultistLeaderAiLogic.NBT_TITLE);
        // :160 rand.nextInt(titles.length)
        assertEquals(0, CultistLeaderAiLogic.titleFor(0));
        assertEquals(14, CultistLeaderAiLogic.titleFor(14));
        assertEquals(0, CultistLeaderAiLogic.titleFor(15));            // wraps
    }

    @Test
    void theChampionNameRuleNeedsAChampionValueAtOrAboveZero() {
        // :85-86 int t = (int) attribute(tc.mobmod).func_111126_e(); if (t >= 0)
        assertTrue(CultistLeaderAiLogic.championNameApplies(0));
        assertTrue(CultistLeaderAiLogic.championNameApplies(11));
        assertFalse(CultistLeaderAiLogic.championNameApplies(-1));
        // :88 String.format(entity.Thaumcraft.CultistLeader.name, getTitle(), modName)
        // the row's display column is "Praetor %s the %d"
        assertEquals("Praetor Alberic the Kdata",
                CultistLeaderAiLogic.nameFor(0, "Kdata"));
        assertEquals("Praetor Zelipe the Vex",
                CultistLeaderAiLogic.nameFor(14, "Vex"));
        assertEquals("Praetor Alberic the Kdata",
                CultistLeaderAiLogic.nameFor(15, "Kdata"));            // titleFor wraps
    }

    // ------------------------------------------------------- equipment (:113-132)

    @Test
    void thePlateSetRidesTheOriginalSlotOrdinals() {
        // :115-118 func_70062_b(4, leader helm), (3, leader chest), (2, leader legs), (1, cultist boots)
        final Map<Integer, ?> plate = CultistLeaderAiLogic.leaderPlate();
        assertEquals(4, plate.size());
        assertEquals(CultistLeaderAiLogic.HELMET_LEADER_PLATE, plate.get(4));
        assertEquals(CultistLeaderAiLogic.CHEST_LEADER_PLATE, plate.get(3));
        assertEquals(CultistLeaderAiLogic.LEGS_LEADER_PLATE, plate.get(2));
        assertEquals(CultistLeaderAiLogic.BOOTS_CULTIST, plate.get(1));
        assertEquals("helmcultistleaderplate", CultistLeaderAiLogic.HELMET_LEADER_PLATE.getPath());
        assertEquals("chestcultistleaderplate", CultistLeaderAiLogic.CHEST_LEADER_PLATE.getPath());
        assertEquals("legscultistleaderplate", CultistLeaderAiLogic.LEGS_LEADER_PLATE.getPath());
        assertEquals("bootscultist", CultistLeaderAiLogic.BOOTS_CULTIST.getPath());
    }

    @Test
    void theWeaponIsVoidOnEasyAndCrimsonEverywhereElse() {
        // :119-123 EnumDifficulty.EASY ? itemSwordVoid : itemSwordCrimson
        assertEquals(CultistLeaderAiLogic.SWORD_VOID, CultistLeaderAiLogic.weaponFor(true));
        assertEquals(CultistLeaderAiLogic.SWORD_CRIMSON, CultistLeaderAiLogic.weaponFor(false));
        assertEquals("swordvoid", CultistLeaderAiLogic.SWORD_VOID.getPath());
        assertEquals("swordcrimson", CultistLeaderAiLogic.SWORD_CRIMSON.getPath());
    }

    @Test
    void theEnchantGateIsHalfTheLightAndTheLevelIsSevenPlusLightOver22() {
        // :129 func_70694_bm() != null && rand.nextFloat() < 0.5F * f
        assertTrue(CultistLeaderAiLogic.enchantFires(0.4999F, 1.0F));
        assertFalse(CultistLeaderAiLogic.enchantFires(0.5F, 1.0F));
        assertFalse(CultistLeaderAiLogic.enchantFires(0.0F, 0.0F));
        assertEquals(0.5F, CultistLeaderAiLogic.ENCHANT_LIGHT_FACTOR, F);
        // :130 (int)(7.0F + f * rand.nextInt(22))
        assertEquals(7, CultistLeaderAiLogic.enchantLevel(1.0F, 0));
        assertEquals(18, CultistLeaderAiLogic.enchantLevel(1.0F, 11));
        assertEquals(28, CultistLeaderAiLogic.enchantLevel(1.0F, 21));
        assertEquals(9, CultistLeaderAiLogic.enchantLevel(0.25F, 8));
        assertEquals(22, CultistLeaderAiLogic.ENCHANT_LEVEL_ROLL);
        assertEquals(7.0F, CultistLeaderAiLogic.ENCHANT_BASE_LEVEL, F);
    }

    // ------------------------------------------------------- faction (:134-141)

    @Test
    void theFactionPairIsTheOriginals() {
        // :135-137 func_142014_c: EntityCultist || EntityCultistLeader, the expression alone
        assertFalse(CultistLeaderAiLogic.isEnemyFaction("not an entity"));
        assertFalse(CultistLeaderAiLogic.isEnemyFaction(null));
        // :139-141 func_70686_a: cleric, leader, knight refused; the rest takes super
        assertFalse(CultistLeaderAiLogic.isTargetableClass(
                com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistCleric.class));
        assertFalse(CultistLeaderAiLogic.isTargetableClass(
                com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistKnight.class));
        assertFalse(CultistLeaderAiLogic.isTargetableClass(EntityCultistLeader.class));
        assertFalse(CultistLeaderAiLogic.isTargetableClass(
                com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist.class));
        assertTrue(CultistLeaderAiLogic.isTargetableClass(
                net.minecraft.world.entity.player.Player.class));
        assertTrue(CultistLeaderAiLogic.isTargetableClass(
                net.minecraft.world.entity.monster.Zombie.class));
    }

    // ------------------------------------------------------- aura (:164-178)

    @Test
    void theHasteAuraIsEightBlocksSixtyTicksAmplifierOneOnlyWhenAbsent() {
        // :168 EntityUtils.getEntitiesInRange(..., EntityCultist.class, 8.0)
        assertEquals(8.0, CultistLeaderAiLogic.AURA_RADIUS, 1.0E-9);
        // :173 new PotionEffect(Potion.field_76428_l.field_76415_H, 60, 1)
        assertEquals(60, CultistLeaderAiLogic.AURA_HASTE_TICKS);
        assertEquals(1, CultistLeaderAiLogic.AURA_HASTE_AMPLIFIER);
        // :172 !func_82165_m(haste)
        assertTrue(CultistLeaderAiLogic.auraApplies(false));
        assertFalse(CultistLeaderAiLogic.auraApplies(true));
    }

    // ------------------------------------------------------- ranged (:180-201)

    @Test
    void theOrbIsTheRedFlagWithThePinnedAimAndSound() {
        // :191 new EntityGolemOrb(world, this, target, true)
        assertTrue(CultistLeaderAiLogic.ORB_IS_RED);
        // :192-193 x += motionX / 2.0; z += motionY / 2.0 (the field pairing, verbatim)
        assertEquals(2.0, CultistLeaderAiLogic.ORB_NUDGE_FACTOR, 1.0E-9);
        // :188-189 / :195-197 the 30/30 look roll
        assertEquals(30, CultistLeaderAiLogic.ORB_LOOK_ROLL);
        // :198 func_70186_c(d0, d1 + 2.0, d2, 0.66F, 3.0F)
        assertEquals(2.0, CultistLeaderAiLogic.ORB_AIM_LIFT, 1.0E-9);
        assertEquals(0.66F, CultistLeaderAiLogic.ORB_SPEED, F);
        assertEquals(3.0F, CultistLeaderAiLogic.ORB_SPREAD, F);
        // :199 func_85030_a("thaumcraft:egattack", 1.0F, 1.0F + rand*0.1F)
        assertEquals(1.0F, CultistLeaderAiLogic.ORB_SOUND_VOLUME, F);
        assertEquals(1.0F, CultistLeaderAiLogic.ORB_SOUND_PITCH_BASE, F);
        assertEquals(0.1F, CultistLeaderAiLogic.ORB_SOUND_PITCH_JITTER, F);
    }

    // ------------------------------------------------------- drops (:147-150)

    @Test
    void theDeathDropIsTheLootbagMeta2AtOneAndAHalf() {
        // :149 func_70099_a(new ItemStack(ConfigItems.itemLootbag, 1, 2), 1.5F)
        assertEquals("itemlootbag", CultistLeaderAiLogic.LOOTBAG.getPath());
        assertEquals("thaumcraft", CultistLeaderAiLogic.LOOTBAG.getNamespace());
        assertEquals(2, CultistLeaderAiLogic.LOOTBAG_META);
        assertEquals(1.5F, CultistLeaderAiLogic.LOOTBAG_CHANCE, F);
        // :152-154 func_70600_l(int) is EMPTY: dropAllEquipped never fires. The base's
        // :205 false gate (DAPS_LOOT) gates the whole death-loot block in both worlds.
        assertFalse(ThaumcraftBossAiLogic.DAPS_LOOT);
    }
}
