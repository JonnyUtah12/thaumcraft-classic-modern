package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the original Crimson cultist family world-free (TC-M6-06-s-c4a-1) — no
 * level, no entity instance, no registry. Every expectation quotes the
 * decompiled line it was read from, in
 * {@code thaumcraft.common.entities.monster.EntityCultist} (110 lines),
 * {@code EntityCultistKnight} (74 lines), {@code EntityCultistCleric} (175
 * lines), {@code thaumcraft.common.entities.ai.cultist.AICultistHurtByTarget}
 * (57 lines), {@code AIAltarFocus} (94 lines), {@code AILongRangeAttack} (70
 * lines) and {@code thaumcraft.common.config.ConfigEntities} — so "cleaning up"
 * one of the original's figures (a goal slot, a speed, a roll, a cap, a meta, a
 * slot ordinal, a table entry, a sound id) turns a test red.
 *
 * <p>The Cleric and its GolemOrb are the sibling card {@code t_6160444c}
 * (c4a-2); their figures are pinned here as data because {@code CultistAiLogic}
 * is the shared rule class the family's cards all read, and the shipped
 * behaviour of the knight is proven by {@code CultistKnightGameTest}.</p>
 */
class CultistAiLogicTest {

    private static final float F = 1.0E-4F;
    private static final double D = 1.0E-4;

    // ------------------------------------------------------- registration

    @Test
    void registrationArgsAreTheOriginalsAndTheBaseHasNone() {
        // registerModEntity(EntityCultistKnight.class, "CultistKnight", 27,
        // instance, 64, 3, true) — ConfigEntities.java:111; the Cleric's row is
        // :113 with mod_index 28. 27/28 are the ledger rows' mod_index column.
        assertEquals(27, CultistAiLogic.KNIGHT_MOD_INDEX, "ConfigEntities.java:111");
        assertEquals(28, CultistAiLogic.CLERIC_MOD_INDEX, "ConfigEntities.java:113");
        assertEquals(64, CultistAiLogic.TRACK_RANGE_BLOCKS, "ConfigEntities.java:111");
        // 1.21.1's clientTrackingRange counts CHUNKS, so 64 blocks lands as 4
        // (the family rule pinned at BrainyZombieAiLogicTest:38).
        assertEquals(4, CultistAiLogic.TRACK_RANGE_CHUNKS, "64 blocks is 4 chunks");
        assertEquals(3, CultistAiLogic.UPDATE_INTERVAL, "ConfigEntities.java:111");
        assertTrue(CultistAiLogic.VELOCITY_UPDATES);
        // The family block of ConfigEntities.init registers Knight, Cleric and
        // Leader and NO EntityCultist base: the base has no registration call, no
        // mod index and no ledger row (docs/content-mapping.csv).
        assertFalse(CultistAiLogic.BASE_IS_REGISTERED);
    }

    // --------------------------------------------------------- base geometry

    @Test
    void baseBoxXpAndNavigatorFlagsAreTheConstructorsOwn() {
        // EntityCultist.java:18 — this.func_70105_a(0.6F, 1.8F)
        assertEquals(0.6F, CultistAiLogic.WIDTH, F);
        assertEquals(1.8F, CultistAiLogic.HEIGHT, F);
        // :19 — this.field_70728_aV = 10 (experienceValue)
        assertEquals(10, CultistAiLogic.XP, "EntityCultist.java:19");
        // :20-21 — setCanBreakDoors(true) + setCanSwim(true)
        assertTrue(CultistAiLogic.NAVIGATOR_BREAKS_DOORS);
        assertTrue(CultistAiLogic.NAVIGATOR_SWIMS);
        // :35-37 — getCanPickUpLoot() is false; :39-41 — the boss flag is true;
        // :43-45 — the pickup item is id 0 (air).
        assertFalse(CultistAiLogic.CAN_PICK_UP_LOOT);
        assertTrue(CultistAiLogic.IS_BOSS);
        assertEquals(0, CultistAiLogic.DROP_ITEM_ID);
        assertEquals(ResourceLocation.withDefaultNamespace("air"), EntityCultist.PICKUP_ITEM_ID);
    }

    @Test
    void attributesAreTheThreeOfTheBaseAndTheHealthsAreTheSubclassesOwn() {
        // EntityCultist.java:26-28 — exactly three attribute writes, no maxHealth
        assertEquals(32.0, CultistAiLogic.FOLLOW_RANGE, D, "EntityCultist.java:26");
        assertEquals(0.3, CultistAiLogic.MOVEMENT_SPEED, D, "EntityCultist.java:27");
        assertEquals(4.0, CultistAiLogic.ATTACK_DAMAGE, D, "EntityCultist.java:28");
        // The base never touches maxHealth, so the mob default 20.0 stands (:24-29)
        assertEquals(20.0, CultistAiLogic.BASE_MAX_HEALTH, D);
        // EntityCultistKnight.java:41 (36.0) and EntityCultistCleric.java:51 (30.0)
        assertEquals(36.0, CultistAiLogic.KNIGHT_MAX_HEALTH, D);
        assertEquals(30.0, CultistAiLogic.CLERIC_MAX_HEALTH, D);
        // EntityCultistCleric.java:52 — this.field_70728_aL = 500 (maxAir)
        assertEquals(500, CultistAiLogic.CLERIC_MAX_AIR);
    }

    // ------------------------------------------------------------- goal slots

    @Test
    void knightGoalSlotsAreTheOriginalsOwnNumbers() {
        // EntityCultistKnight.java:26-33 — 0,3,4,5,6,7,8,8 and :34-35 targets 1,2
        assertEquals(0, CultistAiLogic.GOAL_SLOT_SWIM);
        assertEquals(3, CultistAiLogic.GOAL_SLOT_MELEE);
        assertEquals(4, CultistAiLogic.GOAL_SLOT_DOOR_RESTRICT);
        assertEquals(5, CultistAiLogic.GOAL_SLOT_DOOR_OPEN);
        assertEquals(6, CultistAiLogic.GOAL_SLOT_RESTRICTION);
        assertEquals(7, CultistAiLogic.GOAL_SLOT_WANDER);
        assertEquals(8, CultistAiLogic.GOAL_SLOT_WATCH);
        assertEquals(8, CultistAiLogic.GOAL_SLOT_LOOK_IDLE);
        assertEquals(1, CultistAiLogic.TARGET_SLOT_HURT);
        assertEquals(2, CultistAiLogic.TARGET_SLOT_PLAYER);
        // The goal params: 1.0 / false (:27), 0.8 (:30), 0.8 (:31), 8.0F and 0.02 (:32)
        assertEquals(1.0F, CultistAiLogic.MELEE_SPEED, F);
        assertFalse(CultistAiLogic.MELEE_LONG_MEMORY);
        assertEquals(0.8F, CultistAiLogic.RESTRICTION_SPEED, F);
        assertEquals(0.8F, CultistAiLogic.WANDER_SPEED, F);
        assertEquals(8.0F, CultistAiLogic.WATCH_PLAYER_RANGE, F);
        assertEquals(0.02F, CultistAiLogic.WATCH_PLAYER_PROBABILITY, F);
        // :34 — AICultistHurtByTarget(this, true): the knight calls for help
        assertTrue(CultistAiLogic.IS_AGRESSORS);
    }

    @Test
    void clericGoalSlotsAndRangedParamsAreTheOriginalsOwn() {
        // EntityCultistCleric.java:41-48 — altar at slot 1 (priority 7), ranged at
        // 2, door pair 4/5, restriction 6, wander 7, watch 8, idle 8.
        assertEquals(1, CultistAiLogic.GOAL_SLOT_ALTAR);
        assertEquals(7, CultistAiLogic.ALTAR_PRIORITY, "AIAltarFocus.java:16");
        assertEquals(2, CultistAiLogic.GOAL_SLOT_RANGED);
        // AILongRangeAttack.java:22-24 — 2.0 min distance, speed 1.0, interval
        // 20 + nextInt(20), range 24.0F
        assertEquals(2.0, CultistAiLogic.RANGED_MIN_DISTANCE, D);
        assertEquals(1.0, CultistAiLogic.RANGED_SPEED, D);
        assertEquals(20, CultistAiLogic.RANGED_INTERVAL_MIN);
        assertEquals(40, CultistAiLogic.RANGED_INTERVAL_MAX);
        assertEquals(24.0F, CultistAiLogic.RANGED_RANGE, F);
    }

    @Test
    void rangedGateRefusesNoTargetDeadTargetAndACloseRange() {
        // AILongRangeAttack.java:26-34 — the distance test is on the SQUARED
        // distance against minDistance^2, so 2.0 blocks is 4.0 squared.
        assertFalse(CultistAiLogic.rangedGate(false, false, 100.0));
        assertFalse(CultistAiLogic.rangedGate(true, true, 100.0));
        assertFalse(CultistAiLogic.rangedGate(true, false, 3.99),
                ":31-34 — distSq < 2.0*2.0 refuses");
        assertTrue(CultistAiLogic.rangedGate(true, false, 4.0), ":31 — the boundary passes");
        assertTrue(CultistAiLogic.rangedGate(true, false, 9.0));
    }

    // ------------------------------------------------------- the hurt-by gang

    @Test
    void hurtGoalPriorityHelpBoxAndJoinRuleAreTheOriginals() {
        // AICultistHurtByTarget.java:19 — setPriority(1)
        assertEquals(1, CultistAiLogic.HURT_PRIORITY);
        // :35-43 — the box is the unit cube at the feet inflated by the follow
        // range on x/z and 10.0 on y
        assertEquals(32.0, CultistAiLogic.FOLLOW_RANGE, D, "the x/z inflate is getFollowDistance()");
        assertEquals(10.0, CultistAiLogic.HELP_BOX_Y_EXPAND, D, ":36-43");
        // :45-47 — only a cultist that is not the hurt one and has no target joins
        assertTrue(CultistAiLogic.helpsTheHurt(false, false), ":45-50 — an idle cultist joins");
        assertFalse(CultistAiLogic.helpsTheHurt(true, false), ":45 — the hurt one is not in the gang");
        assertFalse(CultistAiLogic.helpsTheHurt(false, true), ":46-47 — one with a target does not");
        // :48-53 — a cleric mid-ritual ignores the call except on nextInt(3)==0
        assertEquals(3, CultistAiLogic.RITUALIST_BREAK_ROLL);
        assertTrue(CultistAiLogic.ritualistBreaks(0));
        assertFalse(CultistAiLogic.ritualistBreaks(1));
        assertFalse(CultistAiLogic.ritualistBreaks(2));
    }

    // ------------------------------------------------------------- the faction

    @Test
    void factionPredicateIsTheOwnKindTestOfTheOriginal() {
        // EntityCultist.java:101-103 — `el instanceof EntityCultist || el
        // instanceof EntityCultistLeader`. World-free: only a cultist instance
        // answers true, and nothing else does — including a lookalike object.
        assertFalse(CultistAiLogic.canAttackFactionMember("EntityCultist"));
        assertFalse(CultistAiLogic.canAttackFactionMember(null));
        assertFalse(CultistAiLogic.canAttackFactionMember(EntityCultistKnight.class));
    }

    @Test
    void theTargetSelectorExcludesTheShippedCultistsByClassAndTheLeaderByName() {
        // EntityCultist.java:105-107 — false for EntityCultistCleric,
        // EntityCultistLeader and EntityCultistKnight. t_6160444c landed the
        // Cleric class, so its arm runs on the CLASS exactly as the original
        // writes it (`!(el instanceof EntityCultistCleric)`); the Leader is card
        // c5's and is not in this tree, so its arm stays the name-carried one
        // (BD-90 — the D-012 slice's carrier). The stand-in below carries only
        // the Leader's simple name, and the Cleric pin names the real class.
        assertTrue(CultistAiLogic.targetClassExcluded(EntityCultistKnight.class), ":105");
        assertTrue(CultistAiLogic.targetClassExcluded(EntityCultistCleric.class),
                ":106 — class-carried since t_6160444c landed the class");
        assertTrue(CultistAiLogic.targetClassExcluded(EntityCultistLeader.class), ":107 (name-carried)");
        assertFalse(CultistAiLogic.targetClassExcluded(net.minecraft.world.entity.monster.Zombie.class));
        assertEquals("EntityCultistLeader", CultistAiLogic.LEADER_CLASS_NAME);
    }

    /** A stand-in whose simple name is the Leader's, for the name-carried arm. */
    static final class EntityCultistLeader {
        private EntityCultistLeader() {
        }
    }

    @Test
    void theCultistTypeIdSetIsTheThreeTheOriginalRegisters() {
        // ConfigEntities.java:111/:113/:115 — Knight 27, Cleric 28, Leader 29.
        // CultistPortal is an entity, not a mob, and the base is unregistered.
        assertEquals(List.of("cultistknight", "cultistcleric", "cultistleader"),
                CultistAiLogic.CULTIST_TYPE_PATHS);
        assertTrue(CultistAiLogic.isCultistTypeId(ResourceLocation.parse("thaumcraft:cultistknight")));
        assertTrue(CultistAiLogic.isCultistTypeId(ResourceLocation.parse("thaumcraft:cultistcleric")));
        assertTrue(CultistAiLogic.isCultistTypeId(ResourceLocation.parse("thaumcraft:cultistleader")));
        // The base has no id, so it is not a cultist type — and the exclusion set
        // is namespace-checked, so a vanilla id never lands in it.
        assertFalse(CultistAiLogic.isCultistTypeId(ResourceLocation.parse("thaumcraft:cultist")));
        assertFalse(CultistAiLogic.isCultistTypeId(ResourceLocation.withDefaultNamespace("cultistknight")));
        assertFalse(CultistAiLogic.isCultistTypeId(ResourceLocation.withDefaultNamespace("zombie")));
    }

    @Test
    void theClericTypeIdIsCarriedByPathForTheSiblingCard() {
        // BD-87: EntityCultistCleric is t_6160444c's class, so AICultistHurtByTarget
        // reads the ritualist arm's subject through the registry id.
        assertEquals("cultistcleric", CultistAiLogic.CLERIC_TYPE_PATH);
        assertTrue(CultistAiLogic.isClericTypeId(ResourceLocation.parse("thaumcraft:cultistcleric")));
        assertFalse(CultistAiLogic.isClericTypeId(ResourceLocation.parse("thaumcraft:cultistknight")));
        assertFalse(CultistAiLogic.isClericTypeId(ResourceLocation.withDefaultNamespace("cultistcleric")));
        assertFalse(CultistAiLogic.isClericTypeId(null));
    }

    // ------------------------------------------------------------- the drops

    @Test
    void dropTableIsTheOriginalsExclusiveTopDownRoll() {
        // EntityCultist.java:47-58 — nextInt(10): r==0 -> 9, r<=1 -> 17,
        // r <= 3+looting -> 18, else nothing.
        assertEquals(10, CultistAiLogic.DROP_ROLL_MODULO);
        assertEquals(9, CultistAiLogic.dropMetaFor(0, 0), ":50");
        assertEquals(17, CultistAiLogic.dropMetaFor(1, 0), ":52");
        assertEquals(18, CultistAiLogic.dropMetaFor(2, 0), ":54 (looting 0: roll<=3)");
        assertEquals(18, CultistAiLogic.dropMetaFor(3, 0), ":54 (the boundary)");
        assertEquals(-1, CultistAiLogic.dropMetaFor(4, 0), ":56 (nothing)");
        // Looting widens the third branch: 3 + 2 = 5.
        assertEquals(18, CultistAiLogic.dropMetaFor(5, 2));
        assertEquals(-1, CultistAiLogic.dropMetaFor(6, 2));
        assertEquals(18, CultistAiLogic.dropMetaFor(6, 3), "3 + 3 = 6 is the boundary");
        assertEquals(-1, CultistAiLogic.dropMetaFor(9, 0));
        assertEquals(ResourceLocation.parse("thaumcraft:itemresource"), CultistAiLogic.DROP_ITEM,
                "the drop is the lootbag resource item, meta-carrying");
    }

    @Test
    void theCreativeKillGiftIsTheEldritchObjectMetaOne() {
        // EntityCultist.java:60-62 — dropAllLoot replaces the equipment drop with
        // one itemEldritchObject meta 1 (Crimson Rites) at offset 1.0.
        assertEquals(ResourceLocation.parse("thaumcraft:itemeldritchobject"), CultistAiLogic.ALL_LOOT_ITEM);
        assertEquals(1, CultistAiLogic.ALL_LOOT_META);
        assertEquals(1.0F, CultistAiLogic.ALL_LOOT_THROW_OFFSET, F);
        assertEquals(1.5F, CultistAiLogic.DROP_THROW_OFFSET, F, ":50-54 spawnAtLocation(..., 1.5F)");
        assertEquals(3, CultistAiLogic.DROP_THIRD_BASE);
        assertEquals(0, CultistAiLogic.DROP_LOOTING_BASELINE, "1.21.1's hook has no looting param (BD-90)");
    }

    // -------------------------------------------------------------- equipment

    @Test
    void theKnightWearsThePlateInTheOriginalSlots() {
        // EntityCultistKnight.java:46-49 — the four plate pieces, mainhand slot 0
        // untouched by the plate pass.
        final Map<Integer, ResourceLocation> plate = CultistAiLogic.knightPlate();
        assertEquals(4, plate.size());
        assertEquals(ResourceLocation.parse("thaumcraft:helmcultistplate"),
                plate.get(CultistAiLogic.SLOT_HELMET));
        assertEquals(ResourceLocation.parse("thaumcraft:chestcultistplate"),
                plate.get(CultistAiLogic.SLOT_CHEST));
        assertEquals(ResourceLocation.parse("thaumcraft:legscultistplate"),
                plate.get(CultistAiLogic.SLOT_LEGS));
        assertEquals(ResourceLocation.parse("thaumcraft:bootscultist"),
                plate.get(CultistAiLogic.SLOT_BOOTS));
        // 1.7.10's armour slot ordinals: 0 mainhand, 1 boots, 2 legs, 3 chest, 4 helmet
        assertEquals(0, CultistAiLogic.SLOT_MAINHAND);
        assertEquals(1, CultistAiLogic.SLOT_BOOTS);
        assertEquals(2, CultistAiLogic.SLOT_LEGS);
        assertEquals(3, CultistAiLogic.SLOT_CHEST);
        assertEquals(4, CultistAiLogic.SLOT_HELMET);
    }

    @Test
    void theClericWearsTheRobeAndRollsItsBootsSeparately() {
        // EntityCultistCleric.java:56-58 — robe helmet/chest/legs; :59 is the boots
        // roll (0.3F on HARD, 0.1F otherwise).
        final Map<Integer, ResourceLocation> robe = CultistAiLogic.clericRobe();
        assertEquals(3, robe.size(), ":56-58 — the robe set has no boots");
        assertEquals(ResourceLocation.parse("thaumcraft:helmcultistrobe"),
                robe.get(CultistAiLogic.SLOT_HELMET));
        assertEquals(ResourceLocation.parse("thaumcraft:chestcultistrobe"),
                robe.get(CultistAiLogic.SLOT_CHEST));
        assertEquals(ResourceLocation.parse("thaumcraft:legscultistrobe"),
                robe.get(CultistAiLogic.SLOT_LEGS));
        assertEquals(0.3F, CultistAiLogic.CLERIC_BOOTS_CHANCE_HARD, F, ":59");
        assertEquals(0.1F, CultistAiLogic.CLERIC_BOOTS_CHANCE, F, ":59");
    }

    @Test
    void theKnightEliteRollAndSwordBranchAreTheOriginals() {
        // EntityCultistKnight.java:50-62 — chance 0.05F on HARD, 0.01F otherwise;
        // elite nextInt(5): roll 0 -> void sword, else thaumium.
        assertEquals(0.05F, CultistAiLogic.KNIGHT_ELITE_CHANCE_HARD, F);
        assertEquals(0.01F, CultistAiLogic.KNIGHT_ELITE_CHANCE, F);
        assertTrue(CultistAiLogic.knightIsElite(0.0499F, true));
        assertFalse(CultistAiLogic.knightIsElite(0.05F, true), ":50 — the test is a strict <");
        assertTrue(CultistAiLogic.knightIsElite(0.0099F, false));
        assertFalse(CultistAiLogic.knightIsElite(0.01F, false));
        assertEquals(5, CultistAiLogic.KNIGHT_ELITE_ROLL, ":51 nextInt(5)");
        assertEquals(0, CultistAiLogic.KNIGHT_ELITE_VOID_ROLL, ":52");
        assertTrue(CultistAiLogic.knightCarriesVoidSword(0));
        assertFalse(CultistAiLogic.knightCarriesVoidSword(1));
        assertFalse(CultistAiLogic.knightCarriesVoidSword(4));
        assertEquals(ResourceLocation.parse("thaumcraft:swordvoid"), CultistAiLogic.SWORD_VOID);
        assertEquals(ResourceLocation.parse("thaumcraft:swordthaumium"), CultistAiLogic.SWORD_THAUMIUM);
        // :62 — the shipped weapon stand-in, the vanilla iron sword (BD-68's shape).
        // Which of the three ids is live is a registry question, and a plain unit
        // JVM has no bootstrapped registries (the RuneSpriteCellParityTest lesson),
        // so the live half is CultistKnightGameTest's spawn-pass probe.
        assertEquals(ResourceLocation.withDefaultNamespace("iron_sword"), CultistAiLogic.SWORD_IRON);
    }

    @Test
    void theWeaponEnchantmentGateAndLevelFigureAreTheOriginals() {
        // EntityCultistKnight.java:69-70 — rand() < 0.25F * light, then
        // (int)(5.0F + light * nextInt(18))
        assertEquals(0.25F, CultistAiLogic.ENCHANT_LIGHT_CHANCE, F);
        assertEquals(5.0F, CultistAiLogic.ENCHANT_BASE_LEVEL, F);
        assertEquals(18, CultistAiLogic.ENCHANT_LEVEL_ROLL);
        assertTrue(CultistAiLogic.knightEnchantsWeapon(0.249F, 1.0F));
        assertFalse(CultistAiLogic.knightEnchantsWeapon(0.25F, 1.0F), ":69 — strict <");
        assertFalse(CultistAiLogic.knightEnchantsWeapon(0.0F, 0.0F), ":68-69 — light 0 never enchants");
        assertTrue(CultistAiLogic.knightEnchantsWeapon(0.9F, 4.0F), "0.25 * 4 = 1.0 passes any draw < 1");
        assertEquals(5, CultistAiLogic.knightEnchantLevel(1.0F, 0));
        assertEquals(22, CultistAiLogic.knightEnchantLevel(1.0F, 17));
        assertEquals(5, CultistAiLogic.knightEnchantLevel(4.0F, 0));
        assertEquals(73, CultistAiLogic.knightEnchantLevel(4.0F, 17));
    }

    // ----------------------------------------------------------- the cleric AI

    @Test
    void altarFocusConstantsAreTheOriginals() {
        // AIAltarFocus.java:16/:35/:37/:38-43
        assertEquals(7, CultistAiLogic.ALTAR_PRIORITY);
        assertEquals(40, CultistAiLogic.ALTAR_CADENCE_TICKS, ":35 — every 40 ticks of age");
        assertEquals(16.0, CultistAiLogic.ALTAR_MAX_DIST_SQ, D, ":37 — the squared test");
        assertEquals(ResourceLocation.parse("thaumcraft:blockeldritch"), CultistAiLogic.ALTAR_HOME_BLOCK,
                ":38-43 — the home block must be the Eldritch Stone");
    }

    @Test
    void ritualistStateIsBitZeroOfTheWatchedByte() {
        // EntityCultistCleric.java:99 (addObject(16, (byte) 0)) and :103 (bit 0)
        assertEquals(16, CultistAiLogic.DATA_INDEX_ORIGINAL, "1.21.1 allocates ids (BD-87)");
        assertEquals(1, CultistAiLogic.DATA_BIT_RITUALIST);
        assertEquals(1, CultistAiLogic.setRitualist((byte) 0, true));
        assertEquals(0, CultistAiLogic.setRitualist((byte) 1, false));
        assertTrue(CultistAiLogic.isRitualist((byte) 1));
        assertFalse(CultistAiLogic.isRitualist((byte) 0));
        // The NBT carries the WHOLE watched byte under "Flags" (:126-134)
        assertEquals("Flags", CultistAiLogic.NBT_FLAGS);
        assertTrue(CultistAiLogic.isRitualist((byte) 3), ":103 — bit 0 only");
    }

    @Test
    void clericLootGateAndSpawnPayloadAreTheOriginals() {
        // :92-94 — func_70692_ba() is !getIsRitualist()
        assertTrue(CultistAiLogic.clericDropsLoot(false));
        assertFalse(CultistAiLogic.clericDropsLoot(true));
        // :115-122 — any damage clears the ritual
        assertTrue(CultistAiLogic.damageClearsRitual());
        // :137-144 — three ints, applied as setHomePosAndDistance(..., 8)
        assertEquals(3, CultistAiLogic.SPAWN_INT_COUNT);
        assertEquals(8, CultistAiLogic.SPAWN_HOME_DISTANCE);
        // EntityCultist.java:86-98 — the home NBT keys and the write gate
        assertEquals("HomeD", CultistAiLogic.NBT_HOME_D);
        assertEquals("HomeX", CultistAiLogic.NBT_HOME_X);
        assertEquals("HomeY", CultistAiLogic.NBT_HOME_Y);
        assertEquals("HomeZ", CultistAiLogic.NBT_HOME_Z);
        assertTrue(CultistAiLogic.writesHome(true, 8.0F));
        assertFalse(CultistAiLogic.writesHome(false, 8.0F), ":93 — no home, nothing written");
        assertFalse(CultistAiLogic.writesHome(true, 0.0F), ":93 — radius must be > 0");
    }

    @Test
    void clericRangedFiringConstantsAreTheOriginals() {
        // EntityCultistCleric.java:69-75 — nextFloat() > 0.66F throws the RED orb,
        // nudged by half its velocity, aimed 2.0 up, at 0.66F / 3.0F, with egattack.
        assertEquals(0.66F, CultistAiLogic.ORB_ROLL_THRESHOLD, F);
        assertTrue(CultistAiLogic.firesOrb(0.67F));
        assertFalse(CultistAiLogic.firesOrb(0.66F), ":69 — the test is a strict >");
        assertTrue(CultistAiLogic.CLERIC_ORB_IS_RED, ":70 — new EntityGolemOrb(..., true)");
        assertEquals(0.5, CultistAiLogic.ORB_NUDGE_FACTOR, D, ":71-73");
        assertEquals(2.0, CultistAiLogic.ORB_AIM_LIFT, D, ":74");
        assertEquals(0.66F, CultistAiLogic.ORB_VELOCITY, F, ":74");
        assertEquals(3.0F, CultistAiLogic.ORB_INACCURACY, F, ":74");
        assertEquals(ResourceLocation.parse("thaumcraft:egattack"), CultistAiLogic.SOUND_ORB_ATTACK, ":75");
        assertEquals(1.0F, CultistAiLogic.SOUND_ORB_VOLUME, F, ":75");
        assertEquals(0.1F, CultistAiLogic.SOUND_ORB_PITCH_JITTER, F, ":75 pitch 1.0 + rand * 0.1");
        // :78-85 — the fireball branch: 3 balls, spread sqrt(dist) * 0.5 on x/z
        // only, the 1009 aux sfx, the +0.5 height offset.
        assertEquals(3, CultistAiLogic.FIREBALL_COUNT);
        assertEquals(0.5F, CultistAiLogic.FIREBALL_SPREAD_FACTOR, F);
        assertEquals(1009, CultistAiLogic.FIREBALL_AUX_SFX, ":79");
        assertEquals(0.5F, CultistAiLogic.FIREBALL_HEIGHT_OFFSET, F, ":85");
        assertEquals(ResourceLocation.parse("thaumcraft:chant"), CultistAiLogic.SOUND_AMBIENT,
                "EntityCultistCleric.java:173-175");
    }

    @Test
    void ritualLookAnglesAreTheOriginalsRadianMath() {
        // EntityCultistCleric.java:152-154 — d3 is the HORIZONTAL plane
        // sqrt(d0^2 + d2^2); yaw = atan2(d2, d0) - 90; pitch = -atan2(d1, d3).
        // The original's arguments are (dx, dy, dz) of the look target.
        final var straight = CultistAiLogic.clericLookAngles(1.0, 0.0, 0.0);
        assertEquals(-90.0, straight.x, D, "looking along +x faces yaw -90");
        assertEquals(0.0, straight.y, D);
        final var up = CultistAiLogic.clericLookAngles(1.0, 1.0, 0.0);
        assertEquals(-45.0, up.y, D, ":154 — pitch is -atan2(dy, flat) with flat = sqrt(dx^2 + dz^2)");
        final var south = CultistAiLogic.clericLookAngles(0.0, 0.0, 1.0);
        assertEquals(0.0, south.x, D);
        // :148-156 — the client look target and its step
        assertEquals(0.5, CultistAiLogic.RITUAL_LOOK_XZ_OFFSET, D);
        assertEquals(1.5, CultistAiLogic.RITUAL_LOOK_Y_OFFSET, D);
        assertEquals(10.0F, CultistAiLogic.RITUAL_LOOK_PITCH_STEP, F, ":155");
    }

    @Test
    void theRitualStareStepsTenDegreesPerTickToItsTarget() {
        // EntityCultistCleric.java:160-171 — updateRotation(current, target, step):
        // f = wrap180(target - current) (:161), clamped to +-step (:162-168), added
        // to current (:170, unwrapped). The stare calls this seam, not
        // Mth#rotateIfNecessary, whose target-minus-clamped-delta shape freezes a
        // head short of its target (measured on the pad: pitch pinned at -80.0).
        final float F = 1.0E-4F;
        assertEquals(-10.0F, CultistAiLogic.updateRotation(0.0F, -90.0F, 10.0F), F,
                ":155 — one tick of the pitch step is the original's 10.0F, not a jump");
        assertEquals(-20.0F, CultistAiLogic.updateRotation(-10.0F, -90.0F, 10.0F), F,
                ":161-170 — the limiter walks the current value toward the target");
        assertEquals(-90.0F, CultistAiLogic.updateRotation(-85.0F, -90.0F, 10.0F), F,
                ":162-168 — inside the step the delta is the raw wrapped delta, so the "
                        + "stare lands ON the target and stays");
        // :161 wraps the DELTA, so a 350-degree gap turns the head the short way;
        // :170 adds to the current value and does NOT wrap it (the original does
        // not), so 350 + 10 is 360 on the nose.
        assertEquals(360.0F, CultistAiLogic.updateRotation(350.0F, 0.0F, 10.0F), F,
                ":161/:170 — wrapDegrees(0 - 350) = +10, added unwrapped to 350");
        assertEquals(-30.0F, CultistAiLogic.updateRotation(0.0F, -90.0F, 30.0F), F,
                ":156 — the yaw arm runs the same limiter at the mob's rotation limit");
        // Convergence: the original's 10.0F/tick from 0.0 reaches -90 in nine ticks.
        float pitch = 0.0F;
        for (int i = 0; i < 9; i++) {
            pitch = CultistAiLogic.updateRotation(pitch, -90.0F, CultistAiLogic.RITUAL_LOOK_PITCH_STEP);
        }
        assertEquals(-90.0F, pitch, F, ":155 — nine steps of 10.0F converge on the altar pitch");
    }

    // ------------------------------------------------------------ the shipped shape

    @Test
    void theKnightDeclaresItsTypeIdAndCategory() {
        // The id the catalog registers and the category rule of the family
        assertEquals(ResourceLocation.parse("thaumcraft:cultistknight"), EntityCultistKnight.TYPE_ID);
        assertEquals(net.minecraft.world.entity.MobCategory.MONSTER, EntityCultistKnight.category());
        assertTrue(EntityCultistKnight.isBossEntity(), "EntityCultist.java:39-41");
        // The attribute builder is the Monster baseline the base adds its three to
        assertTrue(CultistAiLogic.CULTIST_TYPE_PATHS.contains("cultistknight"));
        assertTrue(EntityCultist.class.isAssignableFrom(EntityCultistKnight.class),
                "the knight is the base's subclass, and the base is the shared class");
    }
}
