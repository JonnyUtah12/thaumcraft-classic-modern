package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Inhabited Zombie world-free (TC-M6-06-s-c4b-2) — no level, no
 * entity instance, no registry. Every expectation quotes the decompiled line
 * it was read from, in
 * {@code thaumcraft.common.entities.monster.EntityInhabitedZombie} (115 lines)
 * and its registration at {@code ConfigEntities.java:123}, so "cleaning up"
 * one of the original's figures (an equipment roll, the crab's y, the death
 * guard, the XP split table, an inherited goal slot, a stripped goal) turns a
 * test red.
 *
 * <p>The world side — the id on the live {@code EntityType}, the goal list on
 * a spawned zombie, the plate equipment, the crab and the XP orbs on death,
 * and the reinforced-zombie suppression — is proven by
 * {@code EldritchMobGameTest}.</p>
 */
class InhabitedZombieAiLogicTest {

    private static final double T = 1.0E-4;

    // ------------------------------------------------------ registration

    @Test
    void registrationArgsAreTheOriginals() {
        // registerModEntity(EntityInhabitedZombie.class, "InhabitedZombie",
        // id++, Thaumcraft.instance, 64, 3, true) — ConfigEntities.java:123,
        // the row at content-mapping line 331 (mod_index 33, "Inhabited
        // Zombie").
        assertEquals(4, InhabitedZombieAiLogic.TRACK_RANGE_CHUNKS);        // 64 blocks / 16
        assertEquals(3, InhabitedZombieAiLogic.UPDATE_INTERVAL);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "inhabitedzombie"),
                ResourceLocation.parse("thaumcraft:inhabitedzombie"));
    }

    // ------------------------------------------------------------ size

    @Test
    void sizeAndXpComeFromTheVanillaZombie() {
        // The 115 lines set no size, eye height or XP figure (no setSize, no
        // func_70047_e override, no func_70693_a) — every figure is inherited
        // from the 1.7.10 EntityZombie, and 1.21.1 has no inheritance for a
        // builder, so the builder ships the vanilla constants (BD-120).
        assertEquals(0.6F, InhabitedZombieAiLogic.WIDTH, T);
        assertEquals(1.95F, InhabitedZombieAiLogic.HEIGHT, T);
        assertEquals(1.74F, InhabitedZombieAiLogic.EYE_HEIGHT, T);
        assertEquals(5, InhabitedZombieAiLogic.XP);                       // Mob field_70728_aV
    }

    @Test
    void attributesAreTheZombiesWithNoReinforcements() {
        // func_110147_ax: maxHealth 30.0 (from EntityLiving), attack damage 5.0
        // (from EntityMob), armour 0.0 (from Entity), follow range 32.0, and
        // SPAWN_REINFORCEMENTS 0.0 — the vanilla zombie's 0.1, which feeds the
        // vanilla reinforcement pass the 115 lines never call (BD-120).
        assertEquals(30.0, InhabitedZombieAiLogic.MAX_HEALTH, T);
        assertEquals(5.0, InhabitedZombieAiLogic.ATTACK_DAMAGE, T);
        assertEquals(0.0, InhabitedZombieAiLogic.SPAWN_REINFORCEMENTS, T);
        // Baby, villager-targeting, and the head-vanilla goals are all off: the
        // original calls no setIsBaby and strips the goals it does not want.
        assertTrue(InhabitedZombieAiLogic.stripsMeleeGoal());
    }

    // ----------------------------------------------------------- goals

    @Test
    void goalSlotsQuoteTheConstructor() {
        // :29 HurtByTargetGoal(true) → target-slot 1, :30
        // NearestAttackableTargetGoal(EntityCultist) → target-slot 3; the
        // retained Zombie goals are melee slot 2 (1.0F, longMemory off),
        // village-patrol slot 6, stroll slot 7, LookAtPlayer slot 8 (8.0F) and
        // RandomLookAround slot 8 (the collision the port ships at 9).
        assertEquals(1, InhabitedZombieAiLogic.TARGET_SLOT_HURT);
        assertEquals(3, InhabitedZombieAiLogic.TARGET_SLOT_CULTIST);
        assertEquals(2, InhabitedZombieAiLogic.TARGET_SLOT_PLAYER);
        assertEquals(2, InhabitedZombieAiLogic.GOAL_SLOT_MELEE);
        assertEquals(6, InhabitedZombieAiLogic.GOAL_SLOT_VILLAGE);
        assertEquals(7, InhabitedZombieAiLogic.GOAL_SLOT_WANDER);
        assertEquals(8, InhabitedZombieAiLogic.GOAL_SLOT_WATCH);
        assertEquals(8, InhabitedZombieAiLogic.GOAL_SLOT_LOOK_IDLE);
        assertEquals(4, InhabitedZombieAiLogic.VILLAGE_REPATH);
        assertEquals(8.0F, InhabitedZombieAiLogic.WATCH_PLAYER_RANGE, T);
        assertFalse(InhabitedZombieAiLogic.MELEE_LONG_MEMORY);
    }

    @Test
    void theGoalCensusIsTheOriginalsFiveAndThree() {
        // The card's assertion: the shipped goal list is the two declared goals
        // plus the Zombie set the 115 lines keep, with the eight vanilla
        // strips, the four target strips and the movement strip applied.
        assertEquals(5, InhabitedZombieAiLogic.SHIPPED_GOAL_COUNT);
        assertEquals(3, InhabitedZombieAiLogic.SHIPPED_TARGET_COUNT);
    }

    // ------------------------------------------------------- equipment

    @Test
    void equipmentChanceIsTheDifficultySwitch() {
        // :37 — HARD ? 0.9F : 0.6F.
        assertEquals(0.9F, InhabitedZombieAiLogic.equipmentChance(true), T);
        assertEquals(0.6F, InhabitedZombieAiLogic.equipmentChance(false), T);
        assertEquals(0.9F, InhabitedZombieAiLogic.EQUIPMENT_CHANCE_HARD, T);
        assertEquals(0.6F, InhabitedZombieAiLogic.EQUIPMENT_CHANCE, T);
    }

    @Test
    void thePlateRollIsInclusive() {
        // :39 / :43 — nextFloat() <= factor, so a roll exactly on the factor
        // equips.
        assertTrue(InhabitedZombieAiLogic.equips(0.6F, 0.6F));
        assertTrue(InhabitedZombieAiLogic.equips(0.59F, 0.6F));
        assertFalse(InhabitedZombieAiLogic.equips(0.61F, 0.6F));
        assertTrue(InhabitedZombieAiLogic.equips(0.9F, 0.9F));
        assertFalse(InhabitedZombieAiLogic.equips(0.91F, 0.9F));
    }

    @Test
    void thePlatesAreTheCultistPlates() {
        // :38 / :40 / :44 — the helmet (unconditional), chest and legs plates,
        // the same ids the cultists wear (BD-68's gated write).
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "helmcultistplate"),
                InhabitedZombieAiLogic.HELM_PLATE);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "chestcultistplate"),
                InhabitedZombieAiLogic.CHEST_PLATE);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "legscultistplate"),
                InhabitedZombieAiLogic.LEGS_PLATE);
    }

    // ------------------------------------------------------------ death

    @Test
    void everyDeathSpawnsOneCrabAtTheFeetAtEyeHeight() {
        // :57-62 — no roll, no condition beyond the side check: y = posY +
        // getEyeHeight() (the 1.7.10 read of 1.95 = 1.755), and the crab's helm
        // flag is set true.
        assertEquals(6.755, InhabitedZombieAiLogic.crabSpawnY(5.0, 1.755), 1.0E-3);
        assertEquals(1.755, InhabitedZombieAiLogic.crabSpawnY(0.0, 1.755), 1.0E-3);
        assertTrue(InhabitedZombieAiLogic.crabSpawnsWithHelm());
        assertEquals(20, InhabitedZombieAiLogic.DEATH_PARTICLES);          // :71-77
        assertEquals(0.02F, InhabitedZombieAiLogic.DEATH_PARTICLE_JITTER, T);
    }

    @Test
    void experienceOnlyDropsOnAPlayerSideKill() {
        // :63 — (recentlyHit > 0 || isPlayer) && canDropLoot && doMobLoot. A
        // kill nobody claims throws nothing, exactly as on 1.7.10.
        assertTrue(InhabitedZombieAiLogic.dropsExperience(true, false, true, true));
        assertTrue(InhabitedZombieAiLogic.dropsExperience(false, true, true, true));
        assertFalse(InhabitedZombieAiLogic.dropsExperience(false, false, true, true));
        assertFalse(InhabitedZombieAiLogic.dropsExperience(true, false, false, true));
        assertFalse(InhabitedZombieAiLogic.dropsExperience(true, false, true, false));
    }

    @Test
    void theExperienceSplitIsThe1710Table() {
        // :64-70 — the loop walks 1.7.10's EntityXPOrb.getOrbValue (73/17/7/3/1
        // with the < 7 / < 37 / < 167 floors) for the zombie's five points,
        // name-mapped onto 1.21.1's ExperienceOrb.getExperienceValue (BD-121).
        assertEquals(List.of(3, 2), InhabitedZombieAiLogic.experienceOrbValues(5));
        assertEquals(List.of(), InhabitedZombieAiLogic.experienceOrbValues(0));
        assertEquals(List.of(1), InhabitedZombieAiLogic.experienceOrbValues(1));
        assertEquals(List.of(3), InhabitedZombieAiLogic.experienceOrbValues(3));
        assertEquals(List.of(7), InhabitedZombieAiLogic.experienceOrbValues(7));
        assertEquals(List.of(7, 3), InhabitedZombieAiLogic.experienceOrbValues(10));
        assertEquals(List.of(73, 17, 7, 3), InhabitedZombieAiLogic.experienceOrbValues(100));
        assertEquals(List.of(2477), InhabitedZombieAiLogic.experienceOrbValues(2477));
    }

    // ---------------------------------------------------------- misc

    @Test
    void theZombieTalksLikeACrabAndDropsNoItem() {
        // :23-25 / :93-95 — the ambient is the crab's own crabtalk sound; the
        // death and hurt sounds are NOT overridden (the vanilla zombie's ship);
        // :97-99 getDropItem is null, which 1.21.1 spells as the air item.
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "crabtalk"),
                InhabitedZombieAiLogic.SOUND_AMBIENT);
        assertEquals(ResourceLocation.parse("minecraft:air"),
                InhabitedZombieAiLogic.DROP_ITEM);
    }

    @Test
    void theSpawnBoxExpandsThirtyTwoBySixteen() {
        // :101-113 — the box expands 32 on x/z and 16 on y and refuses the
        // spawn on any IEldritchMob in a probed cell.
        assertEquals(32.0, InhabitedZombieAiLogic.SPAWN_BOX_XZ_EXPAND, T);
        assertEquals(16.0, InhabitedZombieAiLogic.SPAWN_BOX_Y_EXPAND, T);
    }
}
