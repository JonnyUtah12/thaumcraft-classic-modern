package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchCrab;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Eldritch Crab world-free (TC-M6-06-s-c4b-1) — no level, no entity
 * instance, no registry. Every expectation quotes the decompiled line it was
 * read from, in {@code thaumcraft.common.entities.monster.EntityEldritchCrab}
 * (215 lines) and its registration at {@code ConfigEntities.java:121}, so
 * "cleaning up" one of the original's figures (a shell roll, a difficulty
 * gate, an RNG cascade, a goal slot, a mount test, a drop rate) turns a test
 * red.
 *
 * <p>The world side — the ids on the live {@code EntityType}, the hitbox and
 * attributes on a spawned crab, the goal list, the mount / dismount, the shell
 * drop and the golden-sword drop — is proven by {@code EldritchCrabGameTest}.</p>
 */
class EldritchCrabAiLogicTest {

    private static final double T = 1.0E-4;

    // ------------------------------------------------------ registration

    @Test
    void registrationArgsAreTheOriginals() {
        // registerModEntity(EntityEldritchCrab.class, "EldritchCrab", id++,
        // Thaumcraft.instance, 64, 3, true) — ConfigEntities.java:121, the row
        // at content-mapping line 330 (mod_index 32, "Eldritch Crab").
        assertEquals(64, EldritchCrabAiLogic.TRACK_RANGE_BLOCKS);
        assertEquals(3, EldritchCrabAiLogic.UPDATE_INTERVAL);
        assertTrue(EldritchCrabAiLogic.VELOCITY_UPDATES);
        // 1.21.1's clientTrackingRange counts CHUNKS: 64 blocks is 4 (the
        // family's rule, BrainyZombieAiLogicTest:38).
        assertEquals(EldritchCrabAiLogic.TRACK_RANGE_BLOCKS / 16,
                EldritchCrabAiLogic.TRACK_RANGE_CHUNKS);
        assertEquals(32, EldritchCrabAiLogic.CRAB_MOD_INDEX);
        // The ids: thaumcraft:eldritchcrab (content-mapping row 296 at this
        // card's base; registration ConfigEntities.java:121, verified).
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "eldritchcrab"),
                EntityEldritchCrab.TYPE_ID);
        assertEquals("eldritchcrab", CatalogEldritchCrab.ELDRITCH_CRAB.getKey().location().getPath());
    }

    @Test
    void neitherCrabNorOrbIsFireImmune() {
        // The original's 215 crab lines / 92 orb lines set no isImmuneToFire, so
        // the builders call no EntityType.Builder#fireImmune().
        assertFalse(EntityEldritchCrab.FIRE_IMMUNE);
        assertFalse(com.jonnyutah12.thaumcraftclassic.entities.projectile
                .EntityEldritchOrb.FIRE_IMMUNE);
    }

    @Test
    void theCrabHasNoEggRow() {
        // No thaumcraft:spawn_egg entry names the crab (content-mapping has no
        // egg row for it), so no egg item is registered by this slice.
        assertFalse(CatalogEldritchCrab.HAS_SPAWN_EGG_ROW);
    }

    // ---------------------------------------------------------------- size

    @Test
    void sizeAndXpAreTheOriginals() {
        // func_70105_a(0.8F, 0.6F) — :33; field_70728_aV = 6 — :34.
        assertEquals(0.8F, EldritchCrabAiLogic.WIDTH, 1.0E-4F);
        assertEquals(0.6F, EldritchCrabAiLogic.HEIGHT, 1.0E-4F);
        assertEquals(6, EldritchCrabAiLogic.XP);
    }

    // ---------------------------------------------------------- attributes

    @Test
    void attributesAreTheOriginalsAndTheShellPairIsTheOnlyDelta() {
        // MaxHealth 20 (:52), attack damage 4 (the crab's melee is the plain
        // mob-attack attribute, :131), speed 0.3 bare / 0.275 shelled and armour
        // 0 / 5 from the spawn pass (:78-84) and the shell break (:160-169).
        assertEquals(20.0, EldritchCrabAiLogic.MAX_HEALTH, T);
        assertEquals(4.0, EldritchCrabAiLogic.ATTACK_DAMAGE, T);
        assertEquals(0.3, EldritchCrabAiLogic.SPEED_NO_SHELL, T);
        assertEquals(0.275, EldritchCrabAiLogic.SPEED_SHELL, T);
        assertEquals(0.0, EldritchCrabAiLogic.ARMOR_NONE, T);
        assertEquals(5.0, EldritchCrabAiLogic.ARMOR_SHELL, T);
        assertEquals(EldritchCrabAiLogic.SPEED_SHELL, EldritchCrabAiLogic.speedFor(true), T);
        assertEquals(EldritchCrabAiLogic.SPEED_NO_SHELL, EldritchCrabAiLogic.speedFor(false), T);
        assertEquals(EldritchCrabAiLogic.ARMOR_SHELL, EldritchCrabAiLogic.armorFor(true), T);
        assertEquals(EldritchCrabAiLogic.ARMOR_NONE, EldritchCrabAiLogic.armorFor(false), T);
    }

    // -------------------------------------------------------------- goals

    @Test
    void goalSlotsAndParametersAreTheOriginals() {
        // taskWriter: Swimming 0 (:37), EntityAILeapAtTarget(0.63F) 2 (:38),
        // AIAttackOnCollide(EntityLivingBase.class, 1.0, false) 3 (:39),
        // EntityAIWander(0.8) 7 (:40), EntityAILookIdle 8 (:41) — the crab calls
        // the vanilla attack goal, not a class of its own. The melee pair's
        // speed 1.0 / long-memory false is the shipped MeleeAttackGoal mapping.
        assertEquals(0, EldritchCrabAiLogic.GOAL_SLOT_SWIM);
        assertEquals(2, EldritchCrabAiLogic.GOAL_SLOT_LEAP);
        assertEquals(0.63F, EldritchCrabAiLogic.LEAP_CHANCE, 1.0E-4F);
        assertEquals(3, EldritchCrabAiLogic.GOAL_SLOT_MELEE);
        assertEquals(1.0F, EldritchCrabAiLogic.MELEE_SPEED, 1.0E-4F);
        assertFalse(EldritchCrabAiLogic.MELEE_LONG_MEMORY);
        assertEquals(7, EldritchCrabAiLogic.GOAL_SLOT_WANDER);
        assertEquals(0.8F, EldritchCrabAiLogic.WANDER_SPEED, 1.0E-4F);
        assertEquals(8, EldritchCrabAiLogic.GOAL_SLOT_LOOK_IDLE);
    }

    @Test
    void targetSlotsAndClassesAreTheOriginals() {
        // targetList: HurtByTarget(true) 1 (:42), NearestAttackableTarget(player)
        // 2 (:43), NearestAttackableTarget(EntityCultist) 3 (:44) — the crab
        // calls players and cultists, and cultists only answer the call the
        // faction rule allows.
        assertEquals(1, EldritchCrabAiLogic.TARGET_SLOT_HURT);
        assertEquals(2, EldritchCrabAiLogic.TARGET_SLOT_PLAYER);
        assertEquals(3, EldritchCrabAiLogic.TARGET_SLOT_CULTIST);
        assertEquals(net.minecraft.world.entity.player.Player.class,
                EldritchCrabAiLogic.TARGET_CLASS_PLAYER);
        assertEquals(EntityCultist.class, EldritchCrabAiLogic.TARGET_CLASS_CULTIST);
    }

    // ------------------------------------------------------- shell / Flags

    @Test
    void flagsByteAccessorsRoundTripTheShellBit() {
        // getDataWatcher: Flags byte, bit 0 is the shell (:101-112); the NBT key
        // is "Flags" (:99-112 read/write).
        assertEquals(1, EldritchCrabAiLogic.FLAG_BIT_SHELL);
        assertEquals("Flags", EldritchCrabAiLogic.NBT_FLAGS);
        assertFalse(EldritchCrabAiLogic.shellIn((byte) 0));
        assertTrue(EldritchCrabAiLogic.shellIn(EldritchCrabAiLogic.shellOn((byte) 0)));
        assertFalse(EldritchCrabAiLogic.shellIn(EldritchCrabAiLogic.shellOff(
                EldritchCrabAiLogic.shellOn((byte) 3))));
        // the other bits ride through untouched
        assertEquals(3, EldritchCrabAiLogic.shellOn((byte) 2) & 0xFF);
        assertEquals(2, EldritchCrabAiLogic.shellOff((byte) 3) & 0xFF);
    }

    @Test
    void shellSpawnRollIsHardGuaranteedAndAThirdForEverythingElse() {
        // :72-76 — two branches, not one: HARD calls setHelm(true) with no roll
        // at all (:72-73), and every other difficulty shells on
        // field_70146_Z.nextFloat() < 0.33F (:75). A rule that reads the
        // :72-73 arm as a modifier of the 0.33 roll — "hard AND under a third" —
        // loses the guaranteed branch, which is what this pin catches.
        assertEquals(0.33F, EldritchCrabAiLogic.SHELL_CHANCE, 1.0E-4F);
        assertTrue(EldritchCrabAiLogic.rollsShell(true, 0.999F),
                ":72-73 — HARD is the setHelm(true) branch, no random");
        assertTrue(EldritchCrabAiLogic.rollsShell(true, 0.0F));
        assertTrue(EldritchCrabAiLogic.rollsShell(false, 0.3299F), ":75");
        assertFalse(EldritchCrabAiLogic.rollsShell(false, 0.33F), ":75 is strict <");
        assertFalse(EldritchCrabAiLogic.rollsShell(false, 0.999F), ":75");
    }

    @Test
    void shellBreaksAtHalfHealthAndClearsTheHelm() {
        // :160-169 — shelled, health at or below half → the shell breaks.
        assertTrue(EldritchCrabAiLogic.shellBreaks(true, 10.0F, 20.0F));
        assertTrue(EldritchCrabAiLogic.shellBreaks(true, 9.99F, 20.0F));
        assertFalse(EldritchCrabAiLogic.shellBreaks(true, 10.01F, 20.0F));
        assertFalse(EldritchCrabAiLogic.shellBreaks(false, 1.0F, 20.0F));
        // the plate it drops is unregistered content (its content-mapping row
        // is not_started), so the drop is gated on the live registry.
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "chestcultistplate"),
                EldritchCrabAiLogic.CHEST_CULTIST_PLATE);
    }

    // ------------------------------------------------------- group effects

    @Test
    void groupPotionGateIsHardTimesLocalDifficulty() {
        // :80-83 — HARD (:80) and a 0.1 * localDifficulty roll of the WORLD
        // random (:81-82) arm the cast; never on easy / normal, and the ceiling
        // is the 0.1 base times the local difficulty, not the bare 0.1.
        assertEquals(0.1F, EldritchCrabAiLogic.GROUP_POTION_BASE_CHANCE, 1.0E-4F);
        assertTrue(EldritchCrabAiLogic.rollsGroupPotion(true, 0.09F, 1.0F));
        assertFalse(EldritchCrabAiLogic.rollsGroupPotion(true, 0.1F, 1.0F));
        assertFalse(EldritchCrabAiLogic.rollsGroupPotion(true, 0.2F, 1.0F));
        assertFalse(EldritchCrabAiLogic.rollsGroupPotion(false, 0.0F, 1.0F));
    }

    @Test
    void theGroupEffectCascadeIsTheOriginalsCoinFlipAndQuarterReroll() {
        // :78-92 is the cast site and the vanilla carrier is
        // net.minecraft.entity.monster.EntitySpider.GroupData (imported at :16,
        // NOT in the decompiled TC tree — BD-113 pins the table as data);
        // a coin flip disarms the whole cascade; with the flip won,
        // 0.25 of the picks re-roll before the marker lands.
        int verified = 0;
        int counted = 0;
        for (long seed = 0; seed < 4000; seed++) {
            final boolean flip = RandomSource.create(seed).nextBoolean();
            final int marker = EldritchCrabAiLogic.rollGroupEffect(RandomSource.create(seed));
            if (!flip) {
                assertEquals(EldritchCrabAiLogic.GROUP_EFFECT_NONE, marker,
                        "seed " + seed + ": a lost coin flip must disarm the cascade");
                verified++;
            }
            if (marker == EldritchCrabAiLogic.GROUP_EFFECT_STRENGTH) {
                counted++;
            }
        }
        assertTrue(verified > 1800 && verified < 2200, "coin flip split: " + verified);
        assertTrue(counted > 300, "strength never fired: " + counted);
        assertEquals(0, EldritchCrabAiLogic.GROUP_EFFECT_NONE);
        assertEquals(4, EldritchCrabAiLogic.GROUP_EFFECT_INVISIBILITY);
        assertEquals(0.25F, EldritchCrabAiLogic.GROUP_EFFECT_REROLL_CHANCE, 1.0E-4F);
    }

    @Test
    void everyGroupEffectIsCastAndNoneExpires() {
        // :116-128 — the marker picks the potion; every effect of the group is a
        // real 1.7.10 potion id, and the group's duration is the cast's own
        // (there is no expiry rule for it, so the age cap is unbounded).
        assertTrue(EldritchCrabAiLogic.appliesGroupEffect(
                EldritchCrabAiLogic.GROUP_EFFECT_SPEED, true));
        assertTrue(EldritchCrabAiLogic.appliesGroupEffect(
                EldritchCrabAiLogic.GROUP_EFFECT_STRENGTH, true));
        assertTrue(EldritchCrabAiLogic.appliesGroupEffect(
                EldritchCrabAiLogic.GROUP_EFFECT_REGENERATION, true));
        assertTrue(EldritchCrabAiLogic.appliesGroupEffect(
                EldritchCrabAiLogic.GROUP_EFFECT_INVISIBILITY, true));
        assertFalse(EldritchCrabAiLogic.appliesGroupEffect(
                EldritchCrabAiLogic.GROUP_EFFECT_NONE, true));
        // an unresolvable id (a potion the audio/content lane has not shipped)
        // never applies.
        assertFalse(EldritchCrabAiLogic.appliesGroupEffect(
                EldritchCrabAiLogic.GROUP_EFFECT_INVISIBILITY, false));
        assertEquals(Integer.MAX_VALUE, EldritchCrabAiLogic.GROUP_EFFECT_MAX_AGE);
        for (int id = 1; id <= 4; id++) {
            assertTrue(EldritchCrabAiLogic.appliesGroupPotion(id), "potion " + id);
        }
        assertFalse(EldritchCrabAiLogic.appliesGroupPotion(0));
    }

    @Test
    void theCrabIsTargetableAndAttacksOnlyCrabs() {
        // :212-214 — func_142014_c is `el instanceof EntityEldritchCrab`, so the
        // crab targets crabs and nothing else.
        assertTrue(EldritchCrabAiLogic.attacksOnlyCrabs(true));
        assertFalse(EldritchCrabAiLogic.attacksOnlyCrabs(false));
    }

    // ------------------------------------------------------------- mount

    @Test
    void mountTestIsTheOriginalsFullConjunction() {
        // :120-129 — one clause per case, and every case sits on the base's
        // PASSING values so exactly the clause named is what fails: free crab
        // (:120), a player found (:121), that player riding nobody (:122), the
        // crab off the ground (:123), no shell (:124), the player alive (:125),
        // the crab's feet at least half the player's height — 1.8 / 2 = 0.9 —
        // above the player's feet (:126), squared distance under 4.0 (:127).
        final double h = 1.8;
        final double y = 101.0;                      // 1.0 over the player, over 0.9
        assertTrue(EldritchCrabAiLogic.canMountPlayer(false, true, false, false, false,
                false, y, 100.0, h, 3.9));
        assertFalse(EldritchCrabAiLogic.canMountPlayer(true, true, false, false, false,
                false, y, 100.0, h, 3.9));           // :120 already riding
        assertFalse(EldritchCrabAiLogic.canMountPlayer(false, false, false, false, false,
                false, y, 100.0, h, 3.9));           // :121 no player
        assertFalse(EldritchCrabAiLogic.canMountPlayer(false, true, true, false, false,
                false, y, 100.0, h, 3.9));           // :122 player rides something
        assertFalse(EldritchCrabAiLogic.canMountPlayer(false, true, false, true, false,
                false, y, 100.0, h, 3.9));           // :123 grounded
        assertFalse(EldritchCrabAiLogic.canMountPlayer(false, true, false, false, true,
                false, y, 100.0, h, 3.9));           // :124 shelled
        assertFalse(EldritchCrabAiLogic.canMountPlayer(false, true, false, false, false,
                true, y, 100.0, h, 3.9));            // :125 dead player
        assertFalse(EldritchCrabAiLogic.canMountPlayer(false, true, false, false, false,
                false, 100.8, 100.0, h, 3.9));       // :126 under half height
        assertFalse(EldritchCrabAiLogic.canMountPlayer(false, true, false, false, false,
                false, y, 100.0, h, 4.0));           // :127 4.0 is not under 4
        assertFalse(EldritchCrabAiLogic.canMountPlayer(false, true, false, false, false,
                false, 99.9, 100.0, h, 1.0));        // :126 below the player
    }

    // ------------------------------------------------------------- punch

    @Test
    void punchCooldownAndDismountAreTheOriginals() {
        // :131-137 — the melee punch re-arms on a 10 + nextInt(10) cooldown, and
        // 0.2 of punches throw the rider.
        assertEquals(10, EldritchCrabAiLogic.PUNCH_COOLDOWN_BASE);
        assertEquals(10, EldritchCrabAiLogic.PUNCH_COOLDOWN_JITTER);
        assertEquals(10, EldritchCrabAiLogic.punchCooldownFor(0));
        assertEquals(19, EldritchCrabAiLogic.punchCooldownFor(9));
        assertEquals(0.2F, EldritchCrabAiLogic.PUNCH_DISMOUNT_CHANCE, 1.0E-4F);
        assertTrue(EldritchCrabAiLogic.dismountsAfterPunch(0.1999F));
        assertFalse(EldritchCrabAiLogic.dismountsAfterPunch(0.2F));
    }

    @Test
    void goldenSwordDropIsTheOriginalsModuloAndLootingArm() {
        // :144-149 — a recently-hit crab drops the sword on nextInt(3)==0, on any
        // looting arm, and never un-hit.
        assertEquals(3, EldritchCrabAiLogic.GOLDEN_SWORD_ROLL_MODULO);
        assertEquals(0, EldritchCrabAiLogic.DROP_LOOTING_BASELINE);
        assertTrue(EldritchCrabAiLogic.dropsGoldenSword(true, 0, 0));
        assertTrue(EldritchCrabAiLogic.dropsGoldenSword(true, 1, 1));
        assertTrue(EldritchCrabAiLogic.dropsGoldenSword(true, 2, 2));
        assertFalse(EldritchCrabAiLogic.dropsGoldenSword(true, 1, 0));
        assertFalse(EldritchCrabAiLogic.dropsGoldenSword(false, 0, 3));
        assertEquals(ResourceLocation.withDefaultNamespace("golden_sword"),
                EldritchCrabAiLogic.GOLDEN_SWORD);
    }

    // ------------------------------------------------------ fall / air

    @Test
    void fallImmunityWindowAndAirSupplyAreTheOriginals() {
        // :116-118 — func_70071_h_ zeroes fallDistance for the first 20 ticks;
        // :184-186 — the getMaxAirSupply override is 160.
        assertEquals(20, EldritchCrabAiLogic.FALL_IMMUNE_AGE_TICKS);
        assertEquals(160, EldritchCrabAiLogic.MAX_AIR);
    }

    // --------------------------------------------------------- effects

    @Test
    void poisonNeverAppliesAndEveryOtherEffectDoes() {
        // :208-210 — func_70687_e is one rule: potionID != poison. The 215
        // reference lines have no second half (no func_70688_a, and the string
        // "thaum998" appears nowhere in reference/decompiled — grepped), and
        // 21.1.259 has no mob-side applicability hook to carry the rule on
        // (LivingEntity#addEffect is final; BD-115), so this predicate is the
        // whole port of the rule.
        assertTrue(EldritchCrabAiLogic.POISON_IMMUNE);
        assertFalse(EldritchCrabAiLogic.effectApplies(true));
        assertTrue(EldritchCrabAiLogic.effectApplies(false));
    }

    @Test
    void arthropodCategoryIsPinnedNotExpressed() {
        // :204-206 — EnumCreatureAttribute.ARTHROPOD has no 21.1.259 accessor to
        // override, so the smite / bane-of-arthropods bonus lands ungated; the
        // pin is data-only (the shipped posture of EntityTaintacle:420-422).
        assertTrue(EldritchCrabAiLogic.MOB_TYPE_ARTHROPOD_PINNED_NOT_EXPRESSED);
    }

    // ---------------------------------------------------------- sounds

    @Test
    void soundIdsArePinnedAndTheUnregisteredOnesAreNotFired() {
        // :151-158 crabclaw (volume 1.0, pitch 0.9 + 0.2*rand), :188-198
        // crabtalk / crabdeath, :200-202 the spider footstep. The three
        // thaumcraft ids are unregistered (the audio lane owns the registry,
        // D-004), so the crab pins them and fires none of them; the hurt rides
        // the vanilla hostile-hurt path.
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "crabclaw"),
                EldritchCrabAiLogic.SOUND_CLAW);
        assertEquals(1.0F, EldritchCrabAiLogic.CLAW_VOLUME, 1.0E-4F);
        assertEquals(0.9F, EldritchCrabAiLogic.CLAW_PITCH_BASE, 1.0E-4F);
        assertEquals(0.2F, EldritchCrabAiLogic.CLAW_PITCH_JITTER, 1.0E-4F);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "crabtalk"),
                EldritchCrabAiLogic.SOUND_AMBIENT);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "crabdeath"),
                EldritchCrabAiLogic.SOUND_DEATH);
        assertEquals(ResourceLocation.withDefaultNamespace("entity/hostile/hurt"),
                EldritchCrabAiLogic.SOUND_HURT);
        assertEquals(ResourceLocation.withDefaultNamespace("entity/spider/step"),
                EldritchCrabAiLogic.SOUND_STEP);
        // :47-49 — the volume is 0.5 airborne, 0.0 grounded.
        assertEquals(0.5F, EldritchCrabAiLogic.soundVolumeFor(true), 1.0E-4F);
        assertEquals(0.0F, EldritchCrabAiLogic.soundVolumeFor(false), 1.0E-4F);
    }
}
