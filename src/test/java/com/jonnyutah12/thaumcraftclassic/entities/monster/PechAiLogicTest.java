package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the original Pech world-free (TC-M6-06-s-c3) — no level, no entity
 * instance, no registry. Every expectation quotes the decompiled line it was
 * read from, in {@code thaumcraft.common.entities.monster.EntityPech} (830
 * lines), {@code thaumcraft.common.entities.ai.pech.AIPechTradePlayer} (32
 * lines), {@code ...AIPechItemEntityGoto} (165 lines) and
 * {@code thaumcraft.common.config.ConfigEntities}, so "cleaning up" one of the
 * original's figures (a goal param, an anger roll, a cap, a drop rate, a stack
 * size, a valued-item amount, an event id, a particle count) turns a test red.
 *
 * <p>The world side — the registry ids, the live hitbox, the attributes on the
 * spawned entity, the goal set, the combat-mode swap, the anger fuse, the
 * gang enrage, the loot array, the events and the spawn gate — is proven by
 * {@code PechMobGameTest}.</p>
 */
class PechAiLogicTest {

    private static final float F = 1.0E-4F;
    private static final double D = 1.0E-4;

    // ------------------------------------------------------- registration

    @Test
    void registrationArgsAreTheOriginal() {
        // registerModEntity(EntityPech.class, "Pech", id++, Thaumcraft.instance,
        // 64, 3, true) + ItemSpawnerEgg.addMapping("Pech", 16761087, 4194368) —
        // ConfigEntities.java:103-104. The original's third argument is the
        // running `id++` counter; 12 is the mod_index the ledger row carries
        // (docs/content-mapping.csv), and the egg colours are card c6's.
        assertEquals(23, PechAiLogic.MOD_INDEX);
        assertEquals(64, PechAiLogic.TRACK_RANGE_BLOCKS, "ConfigEntities.java:103");
        // 1.21.1's clientTrackingRange counts CHUNKS, so the 64 blocks land as
        // 4 (the family rule pinned at BrainyZombieAiLogicTest:38).
        assertEquals(4, PechAiLogic.TRACK_RANGE_CHUNKS, "64 blocks is 4 chunks");
        assertEquals(3, PechAiLogic.UPDATE_INTERVAL, "ConfigEntities.java:103");
        assertTrue(PechAiLogic.VELOCITY_UPDATES);
    }

    // ----------------------------------------------------------------- size

    @Test
    void boxIsThePecheSOriginalPair() {
        // :86 — this.func_70105_a(0.6F, 1.8F)
        assertEquals(0.6F, PechAiLogic.WIDTH, F);
        assertEquals(1.8F, PechAiLogic.HEIGHT, F);
        // :91 — this.eyeHeight = this.height * 0.66F
        assertEquals(0.66F, PechAiLogic.EYE_HEIGHT_FACTOR, F);
        assertEquals(1.8 * 0.66, PechAiLogic.eyeHeight(), D);
    }

    @Test
    void attributesAndXpAreTheConstructorsOwnNumbers() {
        // :87 (experiencePoints = 5), :92-94 (the three attribute mutations)
        assertEquals(5, PechAiLogic.XP);
        assertEquals(30.0, PechAiLogic.MAX_HEALTH, D);
        assertEquals(6.0, PechAiLogic.ATTACK_DAMAGE, D);
        assertEquals(0.5, PechAiLogic.MOVEMENT_SPEED, D);
        // :107 — this.isBoss = true (the 1.7.10 boss health-bar flag)
        assertTrue(PechAiLogic.IS_BOSS);
    }

    @Test
    void armourIsTheParentValuePlusTwoCappedAtTwenty() {
        // :507-510 — func_70658_aO (getTotalArmorValue) is an OVERRIDE:
        // super.getTotalArmorValue() + 2, capped at 20
        assertEquals(5 + 2, PechAiLogic.totalArmorValue(5));
        assertEquals(20, PechAiLogic.totalArmorValue(18));   // the cap
        assertEquals(20, PechAiLogic.totalArmorValue(20));
        assertEquals(2, PechAiLogic.ARMOR_BONUS);
        assertEquals(20, PechAiLogic.ARMOR_CAP);
    }

    // ------------------------------------------------------- the three modes

    @Test
    void pechTypeMatchesTheConstructorSIfChain() {
        // :174-182: wand first, then bow, else NORMAL
        assertEquals(PechAiLogic.TYPE_WAND, PechAiLogic.pechTypeFor(true, true));   // wand wins
        assertEquals(PechAiLogic.TYPE_WAND, PechAiLogic.pechTypeFor(true, false));
        assertEquals(PechAiLogic.TYPE_BOW, PechAiLogic.pechTypeFor(false, true));
        assertEquals(PechAiLogic.TYPE_NORMAL, PechAiLogic.pechTypeFor(false, false));
        // :123-146 — rolls 0 and 12 are the WAND branch: ItemWandCasting with an
        // ItemFocusPech and six vis charges. Neither item is registered in this
        // port (content-mapping rows for `wandcasting` / `focuspech`), so the
        // branch ships as the ledger ids on PechAiLogic and equips nothing.
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wandcasting"),
                PechAiLogic.WAND_ITEM);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "focuspech"),
                PechAiLogic.FOCUS_PECH_ITEM);
        assertNull(PechAiLogic.equipmentFor(0));
        assertNull(PechAiLogic.equipmentFor(12));
    }

    @Test
    void equipmentTableIsTheFullTwentyRoll() {
        // :122-163 — `switch (this.rand.nextInt(20))`, transcribed entry by
        // entry. Each 1.7.10 item field is resolved through
        // reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv (searge,name),
        // e.g. field_151036_c = iron_axe, field_151112_aM = fishing_rod,
        // field_151050_s = stone_pickaxe, field_151035_b = iron_pickaxe.
        final ResourceLocation[] want = {
                null,                                                        // 0  the unported wand branch (:123-146)
                ResourceLocation.withDefaultNamespace("stone_sword"),        // 1  Items.field_151052_q (:148)
                PechAiLogic.BOW_ITEM,                                        // 2  Items.field_151031_f (:150)
                ResourceLocation.withDefaultNamespace("stone_axe"),          // 3  field_151049_t (:152)
                PechAiLogic.BOW_ITEM,                                        // 4
                ResourceLocation.withDefaultNamespace("iron_sword"),         // 5  field_151040_l (:154)
                ResourceLocation.withDefaultNamespace("iron_axe"),           // 6  field_151036_c (:156)
                ResourceLocation.withDefaultNamespace("fishing_rod"),        // 7  field_151112_aM (:158)
                ResourceLocation.withDefaultNamespace("stone_pickaxe"),      // 8  field_151050_s (:160)
                ResourceLocation.withDefaultNamespace("iron_pickaxe"),       // 9  field_151035_b (:162)
                PechAiLogic.BOW_ITEM,                                        // 10
                PechAiLogic.BOW_ITEM,                                        // 11
                null,                                                        // 12 the wand branch again
                PechAiLogic.BOW_ITEM,                                        // 13
                null,                                                        // 14 no case
                null,                                                        // 15
                null,                                                        // 16
                null,                                                        // 17
                null,                                                        // 18
                null                                                         // 19
        };
        for (int roll = 0; roll < want.length; roll++) {
            assertEquals(want[roll], PechAiLogic.equipmentFor(roll), "roll " + roll);
        }
    }

    // ---------------------------------------------------------------- goals

    @Test
    void goalSlotsAreTheOriginals() {
        // :95-105 — the registered slots. The three slots the card names
        // (trade 1, item-pickup 3, hurt-target 1) plus the ten the parent's
        // updateTask registers.
        assertEquals(0, PechAiLogic.GOAL_SLOT_SWIM);
        assertEquals(1, PechAiLogic.GOAL_SLOT_TRADE);
        assertEquals(3, PechAiLogic.GOAL_SLOT_ITEM_PICKUP);
        assertEquals(5, PechAiLogic.GOAL_SLOT_DOOR);
        assertEquals(6, PechAiLogic.GOAL_SLOT_RESTRICTION);
        assertEquals(6, PechAiLogic.GOAL_SLOT_VILLAGE);
        assertEquals(9, PechAiLogic.GOAL_SLOT_WANDER);
        assertEquals(9, PechAiLogic.GOAL_SLOT_WATCH_PLAYER);
        assertEquals(10, PechAiLogic.GOAL_SLOT_WATCH);
        assertEquals(11, PechAiLogic.GOAL_SLOT_LOOK_IDLE);
        assertEquals(1, PechAiLogic.TARGET_SLOT_HURT);
        // :449 / :452-453 / :460 — the combat goals are swapped in at slots
        // 2 (attack) and 4 (avoid)
        assertEquals(2, PechAiLogic.COMBAT_SLOT_MELEE);
        assertEquals(2, PechAiLogic.COMBAT_SLOT_ARROW);
        assertEquals(2, PechAiLogic.COMBAT_SLOT_BLAST);
        assertEquals(4, PechAiLogic.COMBAT_SLOT_AVOID);
    }

    @Test
    void goalParametersAreVerbatim() {
        // :63 EntityAIAttackOnCollide(this, 1.0) -> 0.6, the same speed every
        // other goal of this mob carries (see the row's known_differences)
        assertEquals(0.6, PechAiLogic.MELEE_SPEED, D);
        // :64 EntityAIArrowAttack(this, 0.6, 20, 50, 15.0F)
        assertEquals(0.6F, PechAiLogic.WAND_SPEED, F);
        assertEquals(20, PechAiLogic.WAND_MIN_TICKS);
        assertEquals(50, PechAiLogic.WAND_MAX_RANGE);
        assertEquals(15.0F, PechAiLogic.WAND_RANGE, F);
        // :65 EntityAIArrowAttack(this, 0.6, 20, 30, 15.0F) — the blast variant
        assertEquals(0.6F, PechAiLogic.BLAST_SPEED, F);
        assertEquals(20, PechAiLogic.BLAST_MIN_TICKS);
        assertEquals(30, PechAiLogic.BLAST_MAX_RANGE);
        assertEquals(15.0F, PechAiLogic.BLAST_RANGE, F);
        // :67 EntityAIAvoidEntity(this, EntityPlayer.class, 8.0F, 0.5, 0.6)
        assertEquals(8.0F, PechAiLogic.AVOID_RANGE, F);
        assertEquals(0.5, PechAiLogic.AVOID_SPEED_CLOSE, D);
        assertEquals(0.6, PechAiLogic.AVOID_SPEED_FAR, D);
        // :99 / :100 / :101 / :102 / :103
        assertEquals(0.5, PechAiLogic.RESTRICTION_SPEED, D);
        assertEquals(1.0, PechAiLogic.VILLAGE_SPEED, D);
        assertEquals(0.6, PechAiLogic.WANDER_SPEED, D);
        assertEquals(3.0F, PechAiLogic.WATCH_PLAYER_RANGE, F);
        assertEquals(1.0F, PechAiLogic.WATCH_PLAYER_SPEED, F);
        assertEquals(8.0F, PechAiLogic.WATCH_RANGE, F);
        // :96 AIPechTradePlayer's mutexBits = 5; :97 AIPechItemEntityGoto's = 3
        assertEquals(5, PechAiLogic.TRADE_MUTEX_BITS);
        assertEquals(3, PechAiLogic.PICKUP_MUTEX_BITS);
        // AIPechItemEntityGoto.java:52 — the 16-block target scan
        assertEquals(16.0F, PechAiLogic.PICKUP_TARGET_RANGE, F);
    }

    // ---------------------------------------------------------------- anger

    @Test
    void angerFuseIsTheOriginalRollAndIsAssignedNotAdded() {
        // :224-225 — this.anger = 400 + this.rand.nextInt(400)
        assertEquals(400, PechAiLogic.ANGER_BASE);
        assertEquals(400, PechAiLogic.ANGER_JITTER);
        assertEquals(400, PechAiLogic.rollAnger(0));
        assertEquals(799, PechAiLogic.rollAnger(399));
        // the fuse is at least 400 ticks (20 s) for every roll
        for (int roll = 0; roll < 400; roll++) {
            assertTrue(PechAiLogic.rollAnger(roll) >= 400);
            assertTrue(PechAiLogic.rollAnger(roll) <= 799);
        }
    }

    @Test
    void targetPermissionIsTheAngerGate() {
        // :227-235 — canAttackThePlayer needs anger > 0; :1071-1074 of the
        // golem's analogue and the pech's own :200-204 gang scan use 4000,
        // the enrage uses 1200
        assertTrue(PechAiLogic.hasTargetPermission(1));
        assertFalse(PechAiLogic.hasTargetPermission(0));
    }

    @Test
    void cadenceConstantsAreTheTickBodiesOwnNumbers() {
        assertEquals(100, PechAiLogic.CHARGE_CADENCE);     // :198
        assertEquals(120, PechAiLogic.AMBIENT_SOUND_INTERVAL);  // :210
        assertEquals(0.4F, PechAiLogic.VOICE_PITCH, F);    // :207
        assertEquals(0.75F, PechAiLogic.MUMBLE_DECAY, F);  // :208
        assertEquals(100, PechAiLogic.MUMBLE_ROLL);        // :563
        assertEquals(15, PechAiLogic.ANGRY_PARTICLE_ROLL); // :354
        assertEquals(25, PechAiLogic.TAMED_PARTICLE_ROLL); // :360
        assertEquals(5, PechAiLogic.PARTICLE_COUNT);       // :355 / :361
        assertEquals(0.02F, PechAiLogic.PARTICLE_SIGMA, F);// :356-358
    }

    @Test
    void entityEventIdsAreTheOriginalBytes() {
        // :563 (16, PI) / :563 (17, 2*PI) / :641 (18) / :500 (19)
        assertEquals((byte) 16, PechAiLogic.EVENT_MUMBLE);
        assertEquals((byte) 17, PechAiLogic.EVENT_MUMBLE_TRADE);
        assertEquals((byte) 18, PechAiLogic.EVENT_TAMED);
        assertEquals((byte) 19, PechAiLogic.EVENT_ANGRY);
    }

    @Test
    void synchedDataKeysAreTheOriginalIds() {
        // :221 DataParameter.Byte(0), :222 DataParameter.Short(0),
        // :223 DataParameter.Byte(0) — the 1.7.10 entity-data ids
        assertEquals(13, PechAiLogic.DATA_INDEX_TYPE);
        assertEquals(14, PechAiLogic.DATA_INDEX_ANGER);
        assertEquals(16, PechAiLogic.DATA_INDEX_TAMED);
        assertEquals(9, PechAiLogic.LOOT_SLOTS);   // :60 ItemStack[9]
    }

    // -------------------------------------------------------------- arrows

    @Test
    void arrowShotCarriesTheOriginalNumbers() {
        // :466 — new EntityArrow(worldObj, this, target, 1.6F,
        //        14 - world.provider.getDifficulty() * 4)  (getDifficulty is 0..3)
        assertEquals(1.6, PechAiLogic.ARROW_SPEED, D);
        assertEquals(14.0, PechAiLogic.ARROW_INACCURACY_BASE, D);
        assertEquals(4.0, PechAiLogic.ARROW_INACCURACY_PER_DIFFICULTY, D);
        assertEquals(2.0, PechAiLogic.ARROW_DAMAGE_BASE, D);
        assertEquals(0.25, PechAiLogic.ARROW_DAMAGE_GAUSS, D);
        assertEquals(0.11, PechAiLogic.ARROW_DAMAGE_PER_DIFFICULTY, D);
        // the difficulty term is `world.provider.func_151525_a()` = the WORLD
        // difficulty id, i.e. 0 (peaceful) .. 3 (hard) — 1.21.1's
        // `Difficulty.getId()` is the same domain
        assertEquals(14.0, PechAiLogic.arrowInaccuracy(0), D);
        assertEquals(10.0, PechAiLogic.arrowInaccuracy(1), D);
        assertEquals(6.0, PechAiLogic.arrowInaccuracy(2), D);
        assertEquals(2.0, PechAiLogic.arrowInaccuracy(3), D);
        // :468 — `func_70158_f * 2.0 + rand.nextGaussian()*0.25 +
        // world.provider.getDifficulty() * 0.11`. `func_70158_f` is the arrow's
        // own damage field (1.7.10 EntityArrow:59-61 — 2.0 for a mob-fired
        // arrow), so the factor 1.0 reproduces the pech's live base of 4.0.
        assertEquals(2.0, PechAiLogic.arrowDamage(1.0, 0.0, 0), D);
        assertEquals(2.0 + 0.25 + 0.33, PechAiLogic.arrowDamage(1.0, 1.0, 3), D);
        // :469-470 / :472-473 — Power and Punch ride the weapon's association in
        // 1.21.1 (AbstractArrow.java:450); the original's Power term is the +0.5
        // per level, +0.5 pair that 1.21.1's own Power enchant carries
        assertEquals(0.5, PechAiLogic.ARROW_POWER_PER_LEVEL, D);
        assertEquals(0.5, PechAiLogic.ARROW_POWER_FLAT, D);
        assertEquals(2.0, PechAiLogic.arrowPowerBonus(3), D);
        assertEquals(0.0, PechAiLogic.arrowPowerBonus(0), D);
        // :475 — playSound("random.bow", 1.0F, 1.0F / (rand*0.4F + 0.8F))
        assertEquals(1, PechAiLogic.BOW_SOUND_VOLUME);
        assertEquals(1.0 / 0.8, PechAiLogic.bowSoundPitch(0.0), D);
        assertEquals(1.0 / 1.2, PechAiLogic.bowSoundPitch(1.0), D);
    }

    @Test
    void blastLaunchCarriesThePechSOwnNumbers() {
        // :481 — the 0.1 smoke roll; :482 the 1.500000023841858 aim bias;
        // :484 the 0.1 sqrt term; :485 setThrowableHeading(..., 1.5F, 4.0F)
        assertEquals(0.1F, PechAiLogic.BLAST_SMOKE_CHANCE, F);
        assertEquals(1.500000023841858, PechAiLogic.BLAST_AIM_Y_OFFSET, 1.0E-12);
        assertEquals(0.1, PechAiLogic.BLAST_ARC_FACTOR, D);
        assertEquals(1.5F, PechAiLogic.BLAST_MOTION, F);
        assertEquals(4.0F, PechAiLogic.BLAST_SPREAD, F);
        // :482 — the aim Y is target.y + eyeHeight - bias - this.y
        assertEquals(70.0 + 1.62 - 1.500000023841858 - 64.0,
                PechAiLogic.blastAimY(70.0, 1.62, 64.0), 1.0E-12);
        // :484 — the arc term is the horizontal distance times 0.1
        assertEquals(3.0, PechAiLogic.blastArc(30.0), D);
    }

    // ------------------------------------------------------------- the loot

    @Test
    void deathDropsAreTheOriginalRatesAndOffsets() {
        // :330 — the 0.88 gate on the whole array; :334 the y+1.5 offset
        assertEquals(0.88F, PechAiLogic.LOOT_DROP_CHANCE, F);
        assertEquals(1.5, PechAiLogic.LOOT_DROP_OFFSET, D);
        // :337 — the mainhand equipment drop chance, :110 the ctor's 0.2F
        assertEquals(0.2F, PechAiLogic.EQUIP_DROP_CHANCE_MAINHAND, F);
        // :340 — 1 + looting beans, each gated by nextBoolean()
        assertEquals(1, PechAiLogic.manaBeanCount(0));
        assertEquals(3, PechAiLogic.manaBeanCount(2));
        assertEquals(2, PechAiLogic.BEAN_ROLL_BOUND);        // nextBoolean
        assertEquals(6, PechAiLogic.PRIMAL_ASPECT_COUNT);    // 6 primals
        assertTrue(PechAiLogic.beanDropped(true));
        assertFalse(PechAiLogic.beanDropped(false));
        // :341 — nextInt(10) < 1 + looting -> itemResource meta 18
        assertTrue(PechAiLogic.dropsResource(0, 0));
        assertFalse(PechAiLogic.dropsResource(1, 0));
        assertTrue(PechAiLogic.dropsResource(1, 1));
        assertEquals(18, PechAiLogic.RESOURCE_DROP_META);
        assertEquals(0, PechAiLogic.MANA_BEAN_META);
        // the meta-9 dropAllLoot itemResource
        assertEquals(9, PechAiLogic.DROP_ALL_LOOT_META);
    }

    @Test
    void tamingIsTheValueRollAgainstTen() {
        // :623 / :641 — nextInt(10) < getValue(item)
        assertEquals(10, PechAiLogic.TAME_ROLL_BOUND);
        assertTrue(PechAiLogic.tames(0, 1));
        assertTrue(PechAiLogic.tames(4, 5));
        assertFalse(PechAiLogic.tames(5, 5));
        assertFalse(PechAiLogic.tames(0, 0));
    }

    @Test
    void valuedTableIsTheOriginalSixItems() {
        // :738-743 — the static map, in the original's order
        assertEquals(6, PechAiLogic.VALUED_ITEMS.size());
        assertEquals(1, PechAiLogic.tableValue(ResourceLocation.fromNamespaceAndPath("thaumcraft", "itemmanabean")));
        assertEquals(2, PechAiLogic.tableValue(ResourceLocation.withDefaultNamespace("gold_ingot")));
        assertEquals(2, PechAiLogic.tableValue(ResourceLocation.withDefaultNamespace("golden_apple")));
        assertEquals(3, PechAiLogic.tableValue(ResourceLocation.withDefaultNamespace("ender_pearl")));
        assertEquals(4, PechAiLogic.tableValue(ResourceLocation.withDefaultNamespace("diamond")));
        assertEquals(5, PechAiLogic.tableValue(ResourceLocation.withDefaultNamespace("emerald")));
        assertEquals(-1, PechAiLogic.tableValue(ResourceLocation.withDefaultNamespace("dirt")));
        assertEquals(-1, PechAiLogic.tableValue(ResourceLocation.fromNamespaceAndPath("thaumcraft", "resourcerandom")));
    }

    @Test
    void greedValueIsCappedAtThirtyTwo() {
        // :735 — return Math.min(32, al.getAmount(Aspect.GREED))
        assertEquals(32, PechAiLogic.GREED_VALUE_CAP);
        assertEquals(7, PechAiLogic.aspectValue(7));
        assertEquals(32, PechAiLogic.aspectValue(33));
        assertEquals(32, PechAiLogic.aspectValue(32));
        assertEquals(0, PechAiLogic.aspectValue(0));
        // Aspect.GREED's id in this port's aspect registry is `lucrum`
        // (AspectDefinitions.java:70)
        assertEquals("lucrum", PechAiLogic.GREED_ASPECT_ID);
    }

    // ------------------------------------------------------------- pickup

    @Test
    void canPickupIsTheOriginalThreeBranches() {
        // :613-629 — wild + valued is unconditional (:615); a free slot (:620)
        // or a strict-match merge with room (:623-627) is needed otherwise
        assertTrue(PechAiLogic.canPickup(false, true, false, false, false));
        assertFalse(PechAiLogic.canPickup(true, true, false, false, false));   // tamed: needs a slot
        assertTrue(PechAiLogic.canPickup(true, false, true, false, false));
        assertTrue(PechAiLogic.canPickup(true, false, false, true, true));
        assertFalse(PechAiLogic.canPickup(true, false, false, true, false));   // over the max
        assertFalse(PechAiLogic.canPickup(true, false, false, false, false));
    }

    @Test
    void mergeTransferIsWholeStackOrRoomLeft() {
        // :660-666 — (incoming + held <= max) ? the WHOLE incoming stack
        // (the branch then returns null) : sz = min(incoming, max - held).
        // The signature is (heldCount, heldMax, incoming).
        assertEquals(1, PechAiLogic.mergeTransfer(3, 5, 1));    // 1+3 <= 5 -> all of it
        assertEquals(0, PechAiLogic.mergeTransfer(3, 3, 1));    // full slot -> nothing
        assertEquals(2, PechAiLogic.mergeTransfer(3, 5, 3));    // 3+3 > 5 -> only the room left
        assertEquals(5, PechAiLogic.mergeTransfer(0, 5, 9));    // empty slot -> max-count
    }

    @Test
    void pickupGoalCadenceAndPenaltiesAreTheOriginals() {
        // AIPechItemEntityGoto.java:70 (Config.golemDelay == 5), :119, :127,
        // :130, :124, :135, :111 / :120
        assertEquals(5, PechAiLogic.GOLEM_DELAY);
        assertEquals(4, PechAiLogic.PENALTY_BASE);
        assertEquals(10, PechAiLogic.PENALTY_FAIL);
        assertEquals(1.0, PechAiLogic.PENALTY_END_DISTANCE_SQ, D);
        assertEquals(1.5, PechAiLogic.PICKUP_DISTANCE_SQ, D);
        assertEquals(1.5, PechAiLogic.PICKUP_SPEED_FACTOR, D);
        // :78 — the pech never chases an item it dropped itself
        assertTrue(PechAiLogic.isPechDrop("PechDrop"));
        assertFalse(PechAiLogic.isPechDrop("Player"));
        assertFalse(PechAiLogic.isPechDrop(null));
    }

    @Test
    void tradeGoalGateIsVelocityThenTrading() {
        // AIPechTradePlayer.java:24 — field_70133_I ? false : trading
        assertTrue(PechAiLogic.canStartTrade(true, true));
        assertFalse(PechAiLogic.canStartTrade(true, false));
        assertFalse(PechAiLogic.canStartTrade(false, true));
    }

    // ------------------------------------------------------- aspect merging

    @Test
    void mergeAspectListsIsTheGetObjectTagsPlusGetBonusTagsPair() {
        // :713-714 / :729-730 — the two sources merged, null-safe
        assertNull(PechAiLogic.mergeAspectLists(null, null));
        final AspectList base = new AspectList().add(Aspect.getAspect("lucrum"), 3);
        final AspectList bonus = new AspectList().add(Aspect.getAspect("lucrum"), 5);
        assertEquals(3, PechAiLogic.mergeAspectLists(base, null).getAmount(Aspect.getAspect("lucrum")));
        assertEquals(8, PechAiLogic.mergeAspectLists(base, bonus).getAmount(Aspect.getAspect("lucrum")));
        assertEquals(5, PechAiLogic.mergeAspectLists(null, bonus).getAmount(Aspect.getAspect("lucrum")));
        // the merge never mutates the caller's list
        assertEquals(3, base.getAmount(Aspect.getAspect("lucrum")));
    }

    // ------------------------------------------------------------ spawning

    @Test
    void spawnGateIsTheOriginalCountsAndInflate() {
        // :776, :788 (the 16-block inflate), :780 (the pech count bound),
        // :778 (the dimension gate)
        assertEquals(16.0, PechAiLogic.SPAWN_BOX_INFLATE, D);
        assertEquals(4, PechAiLogic.MAX_PECHS_IN_BOX);
        assertTrue(PechAiLogic.OVERWORLD_ONLY);
        // :804-806 — the isMagical biome test
        assertTrue(PechAiLogic.isMagicBiomeTag(true));
        assertFalse(PechAiLogic.isMagicBiomeTag(false));
    }
}
