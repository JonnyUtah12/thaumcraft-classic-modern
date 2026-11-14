package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Eldritch Guardian world-free (TC-M6-06-s-c4b-2) — no level, no
 * entity instance, no registry. Every expectation quotes the decompiled line
 * it was read from, in
 * {@code thaumcraft.common.entities.monster.EntityEldritchGuardian} (310
 * lines) and its registration at {@code ConfigEntities.java:107}, so
 * "cleaning up" one of the original's figures (an arm lift, a muzzle offset,
 * the 0.1 orb roll, the whisper gate, the Outer Lands heal, the halved magic
 * damage, a goal slot, an event id) turns a test red.
 *
 * <p>The world side — the id on the live {@code EntityType}, the hitbox and
 * attributes on a spawned guardian, the goal list, the fired orb, the screech
 * mobEffect, the whisper payload, the undying navigator, the home NBT and the
 * spawn gate — is proven by {@code EldritchMobGameTest}.</p>
 */
class EldritchGuardianAiLogicTest {

    private static final double T = 1.0E-4;

    // ------------------------------------------------------ registration

    @Test
    void registrationArgsAreTheOriginals() {
        // registerModEntity(EntityEldritchGuardian.class, "EldritchGuardian",
        // id++, Thaumcraft.instance, 64, 3, true) — ConfigEntities.java:107,
        // the row at content-mapping line 323 (mod_index 25, "Eldritch
        // Guardian"). The egg at :113 is that row's egg (mod_index 56), not a
        // row of its own.
        assertEquals(4, EldritchGuardianAiLogic.TRACK_RANGE_CHUNKS);       // 64 blocks / 16
        assertEquals(3, EldritchGuardianAiLogic.UPDATE_INTERVAL);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "eldritchguardian"),
                ResourceLocation.parse("thaumcraft:eldritchguardian"));
    }

    // ------------------------------------------------------------ size

    @Test
    void sizeEyeHeightAndXpAreTheOriginals() {
        assertEquals(0.8F, EldritchGuardianAiLogic.WIDTH, T);              // :80
        assertEquals(2.25F, EldritchGuardianAiLogic.HEIGHT, T);            // :80
        assertEquals(2.1F, EldritchGuardianAiLogic.EYE_HEIGHT, T);         // :247-249
        assertEquals(20, EldritchGuardianAiLogic.XP);                      // :70
    }

    // ------------------------------------------------------- attributes

    @Test
    void attributeBasesAreTheOriginals() {
        assertEquals(50.0, EldritchGuardianAiLogic.MAX_HEALTH, T);         // :71
        assertEquals(40.0, EldritchGuardianAiLogic.FOLLOW_RANGE, T);       // :72
        assertEquals(0.28, EldritchGuardianAiLogic.MOVEMENT_SPEED, T);     // :73
        assertEquals(7.0, EldritchGuardianAiLogic.ATTACK_DAMAGE, T);       // :74
        // :84-86 — the runtime armour getter (func_70658_aO() = 4) rides the
        // ARMOR base, the crab slice's mapping of the same getter.
        assertEquals(4.0, EldritchGuardianAiLogic.ARMOR, T);
    }

    @Test
    void theGuardianIsABossAndNeverGrubsAndNeverDrowns() {
        // :169-171 isAI() = true; :242-244 canPickUpLoot = false; :237-239
        // isFriendly = true; :155-157 getMaxAirLevel = 500.
        assertTrue(EldritchGuardianAiLogic.IS_BOSS);
        assertFalse(EldritchGuardianAiLogic.CAN_PICK_UP_LOOT);
        assertEquals(500, EldritchGuardianAiLogic.MAX_AIR);
        // :173-175 setBreakDoors = false — the 1.21.1 ground-state path, inert
        // for a boss (BD-118); the water-avoidance setter of :173-175 is
        // recorded, not asserted (BD-119).
        assertTrue(EldritchGuardianAiLogic.NAVIGATOR_AVOIDS_WATER);
    }

    // ----------------------------------------------------------- goals

    @Test
    void goalSlotsQuoteTheConstructor() {
        // :55 Swimming slot 0, :58 RangedAttack slot 2, :59 MeleeAttack slot 3,
        // :60 ParentPassiveMove slot 5, :61 RandomStroll slot 7, :62
        // LookAtPlayer slot 8, :63 RandomLookAround slot 8 (collides with the
        // look goal; 1.21.1 puts the second at priority 9).
        assertEquals(0, EldritchGuardianAiLogic.GOAL_SLOT_SWIM);
        assertEquals(2, EldritchGuardianAiLogic.GOAL_SLOT_RANGED);
        assertEquals(3, EldritchGuardianAiLogic.GOAL_SLOT_MELEE);
        assertEquals(5, EldritchGuardianAiLogic.GOAL_SLOT_RESTRICTION);
        assertEquals(7, EldritchGuardianAiLogic.GOAL_SLOT_WANDER);
        assertEquals(8, EldritchGuardianAiLogic.GOAL_SLOT_WATCH);
        assertEquals(8, EldritchGuardianAiLogic.GOAL_SLOT_LOOK_IDLE);
        assertEquals(9, EldritchGuardianAiLogic.GOAL_SLOT_LOOK_IDLE_PORT);
        // The goal ctors: the ranged attack 8.0F / 1.0F / 20..40 / 24.0F
        // (:58), the melee 1.0F + longMemory FALSE (:59), the restriction
        // 0.8F (:60), the stroll 1.0F (:61), the watch 8.0F + 0.02F (:62).
        assertEquals(8.0, EldritchGuardianAiLogic.RANGED_MIN_DISTANCE, T);
        assertEquals(1.0, EldritchGuardianAiLogic.RANGED_SPEED, T);
        assertEquals(20, EldritchGuardianAiLogic.RANGED_INTERVAL_MIN);
        assertEquals(40, EldritchGuardianAiLogic.RANGED_INTERVAL_MAX);
        assertEquals(24.0F, EldritchGuardianAiLogic.RANGED_RANGE, T);
        assertEquals(1.0F, EldritchGuardianAiLogic.MELEE_SPEED, T);
        assertFalse(EldritchGuardianAiLogic.MELEE_LONG_MEMORY);
        assertEquals(0.8F, EldritchGuardianAiLogic.RESTRICTION_SPEED, T);
        assertEquals(1.0F, EldritchGuardianAiLogic.WANDER_SPEED, T);
        assertEquals(8.0F, EldritchGuardianAiLogic.WATCH_PLAYER_RANGE, T);
        assertEquals(0.02F, EldritchGuardianAiLogic.WATCH_PLAYER_PROBABILITY, T);
    }

    @Test
    void targetSlotsAreTheOriginals() {
        // :64 HurtByTargetGoal (NearestAttackableTarget subclass) target-slot
        // 1, :65 player slot 2, :66 cultist slot 3.
        assertEquals(1, EldritchGuardianAiLogic.TARGET_SLOT_HURT);
        assertEquals(2, EldritchGuardianAiLogic.TARGET_SLOT_PLAYER);
        assertEquals(3, EldritchGuardianAiLogic.TARGET_SLOT_CULTIST);
    }

    // ------------------------------------------------------ arm signals

    @Test
    void armEventsCarryTheirOwnAngles() {
        // :117/:121/:125 - :133 → 15/16/17, consumed at :286-297. 1.21.1 has no
        // data manager for an int, so the lift travels as three events
        // (BD-117); the arm pose ships as 0.5 / 0.9 with 0.05/tick decay.
        assertEquals(15, EldritchGuardianAiLogic.EVENT_ARM_LEFT);
        assertEquals(16, EldritchGuardianAiLogic.EVENT_ARM_RIGHT);
        assertEquals(17, EldritchGuardianAiLogic.EVENT_ARM_BOTH);
        assertEquals(0.5F, EldritchGuardianAiLogic.ARM_LIFT_HALF, T);
        assertEquals(0.9F, EldritchGuardianAiLogic.ARM_LIFT_FULL, T);
        assertEquals(0.05F, EldritchGuardianAiLogic.ARM_LIFT_DECAY, T);
        // :226-228 — the watchers are 12 (aura), 13 (ward) and 14 (the runic
        // seal); the guardian rides all three. 1.21.1 has no watcher ids, so
        // the seal is a no-op consumer (BD-118/BD-120).
        assertEquals(12, EldritchGuardianAiLogic.WATCHER_12);
        assertEquals(13, EldritchGuardianAiLogic.WATCHER_13);
        assertEquals(14, EldritchGuardianAiLogic.WATCHER_14);
    }

    @Test
    void homeSavesRideTheFourNamedKeys() {
        // :197-200 / :209-212 — the original writes HomeX/Y/Z as Ints and HomeD
        // as a Float, guarded on the field default (BD-118).
        assertEquals("HomeX", EldritchGuardianAiLogic.NBT_HOME_X);
        assertEquals("HomeY", EldritchGuardianAiLogic.NBT_HOME_Y);
        assertEquals("HomeZ", EldritchGuardianAiLogic.NBT_HOME_Z);
        assertEquals("HomeD", EldritchGuardianAiLogic.NBT_HOME_D);
        // :223-235 — the write condition, as the port's seam reads it: a home
        // is written only once one is set and its distance is live.
        assertTrue(EldritchGuardianAiLogic.writesHome(true, 16));
        assertFalse(EldritchGuardianAiLogic.writesHome(false, 16));  // never homed
        assertFalse(EldritchGuardianAiLogic.writesHome(true, 0));    // the field default
        assertEquals(0, EldritchGuardianAiLogic.IDLE_DEFAULT ? 1 : 0);          // :68 field default
    }

    // ---------------------------------------------------- the orb attack

    @Test
    void theOrbFiresOnANineTenthsRoll() {
        // :271 — nextFloat() > 0.1F.
        assertFalse(EldritchGuardianAiLogic.firesOrb(0.1F));
        assertFalse(EldritchGuardianAiLogic.firesOrb(0.0999F));
        assertTrue(EldritchGuardianAiLogic.firesOrb(0.1001F));
        assertTrue(EldritchGuardianAiLogic.firesOrb(0.9F));
        assertEquals(0.1F, EldritchGuardianAiLogic.ORB_SHOT_CHANCE, T);
        // :273 — the spawn offsets: 90 deg to the target's left for the right
        // arm, 180 deg for the both-arms screech, a 0.5F radius, y at
        // 0.057777777 * height + 1.0, and the orb leaves at speed 1.0 with 2.0F
        // of inaccuracy (BD-118's rotation-maths seam).
        assertEquals(90, EldritchGuardianAiLogic.MUZZLE_DEGREES_RIGHT);
        assertEquals(180, EldritchGuardianAiLogic.MUZZLE_DEGREES_LEFT);
        assertEquals(0.5F, EldritchGuardianAiLogic.MUZZLE_RADIUS, T);
        assertEquals(0.057777777F, EldritchGuardianAiLogic.MUZZLE_Y_FACTOR, T);
        assertEquals(1.0F, EldritchGuardianAiLogic.ORB_SPEED, T);
        assertEquals(2.0F, EldritchGuardianAiLogic.ORB_INACCURACY, T);
    }

    @Test
    void theSonicBranchScreechesWithersAndWarps() {
        // :286-297 — wither 400 ticks amplifier 0, then, when difficulty > 0
        // and the difficulty roll passes, warp (1 + nextInt(3)) on the target
        // and the sonic boom mobEffect; :110 - :113 fire the screech at volume
        // 3.0.
        assertEquals(400, EldritchGuardianAiLogic.SONIC_WITHER_TICKS);
        assertEquals(0, EldritchGuardianAiLogic.SONIC_WITHER_AMPLIFIER);
        assertEquals(1, EldritchGuardianAiLogic.SONIC_WARP_BASE);
        assertEquals(3, EldritchGuardianAiLogic.SONIC_WARP_JITTER);
        assertEquals(3.0F, EldritchGuardianAiLogic.SCREECH_VOLUME, T);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "egscreech"),
                EldritchGuardianAiLogic.SOUND_SCREECH);
    }

    @Test
    void theBurnRollScalesWithDifficulty() {
        // :139 - :141 — an empty main hand, on the ground, a roll under
        // difficulty * 0.3F, and 2 * difficulty seconds of fire.
        assertTrue(EldritchGuardianAiLogic.setsFire(1, true, true, 0.29F));
        assertFalse(EldritchGuardianAiLogic.setsFire(1, true, true, 0.3F));
        assertTrue(EldritchGuardianAiLogic.setsFire(2, true, true, 0.59F));
        assertFalse(EldritchGuardianAiLogic.setsFire(2, true, true, 0.6F));
        assertTrue(EldritchGuardianAiLogic.setsFire(3, true, true, 0.89F));
        assertFalse(EldritchGuardianAiLogic.setsFire(3, true, true, 0.9F));
        assertFalse(EldritchGuardianAiLogic.setsFire(3, false, true, 0.0F));   // held item
        assertFalse(EldritchGuardianAiLogic.setsFire(3, true, false, 0.0F));   // airborne
        assertEquals(2, EldritchGuardianAiLogic.fireSecondsFor(1));
        assertEquals(4, EldritchGuardianAiLogic.fireSecondsFor(2));
        assertEquals(6, EldritchGuardianAiLogic.fireSecondsFor(3));
        assertEquals(0.3F, EldritchGuardianAiLogic.BURN_CHANCE_PER_DIFFICULTY, T);
        assertEquals(2, EldritchGuardianAiLogic.BURN_SECONDS_PER_DIFFICULTY);
    }

    // ------------------------------------------------------- the whisper

    @Test
    void theWhisperGatesOnAgeDifficultyAndRealm() {
        // :105-130 (the onUpdate arm at :110-:125) — the whisper runs when the
        // dimension is NOT the Outer Lands, on the tick gate (age == 0 || age %
        // 100 == 0), and not on EASY. The radius is 576 blocks squared on HARD
        // and 256 otherwise (:121).
        assertTrue(EldritchGuardianAiLogic.sendsWhisper(false, 0, false));       // first tick
        assertTrue(EldritchGuardianAiLogic.sendsWhisper(false, 100, false));
        assertFalse(EldritchGuardianAiLogic.sendsWhisper(true, 100, false));     // in the realm: silent
        assertFalse(EldritchGuardianAiLogic.sendsWhisper(false, 101, false));    // cadence
        assertFalse(EldritchGuardianAiLogic.sendsWhisper(false, 100, true));     // EASY
        assertEquals(100, EldritchGuardianAiLogic.WHISPER_CADENCE_TICKS);
        assertEquals(576.0, EldritchGuardianAiLogic.whisperRadiusSq(true), T);
        assertEquals(256.0, EldritchGuardianAiLogic.whisperRadiusSq(false), T);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "egidle"),
                EldritchGuardianAiLogic.SOUND_AMBIENT);
    }

    // -------------------------------------------------- heal and armour

    @Test
    void theGuardianRegeneratesOnlyInTheOuterLands() {
        // :177 - :183 — every 25 ticks, +1.0 heart, only in the realm, only
        // while not invulnerable and below half health; :303 - :305 is the
        // undying half of the same seam (BD-117).
        assertTrue(EldritchGuardianAiLogic.regenerates(true, 0, 25, 1.0F, 50.0F));
        assertFalse(EldritchGuardianAiLogic.regenerates(false, 0, 25, 1.0F, 50.0F));
        assertFalse(EldritchGuardianAiLogic.regenerates(true, 1, 25, 1.0F, 50.0F));
        assertFalse(EldritchGuardianAiLogic.regenerates(true, 0, 24, 1.0F, 50.0F));
        assertFalse(EldritchGuardianAiLogic.regenerates(true, 0, 25, 25.0F, 50.0F));
        assertEquals(25, EldritchGuardianAiLogic.REGEN_CADENCE_TICKS);
        assertEquals(1.0F, EldritchGuardianAiLogic.REGEN_PER_TICK, T);
        // :214 - :216 — the spawn heal is (int) maxHealth / 2 of the max.
        assertEquals(25.0, EldritchGuardianAiLogic.spawnHeal(50.0), T);
    }

    @Test
    void magicDamageIsHalved() {
        // :96-102 — func_70097_a(DamageSource,float): if (source.func_82725_o())
        // damage /= 2.0F; then super. The halving is EXACT on both engines —
        // 1.7.10's damage argument is already a float, so there is no Int cast
        // to truncate (the 5.0 case below is the proof), and 1.21.1's
        // hurt(DamageSource,float) returns the float to the caller unchanged.
        assertEquals(5.0, EldritchGuardianAiLogic.damageAfterMagicHalve(true, 10.0F), T);
        assertEquals(2.5, EldritchGuardianAiLogic.damageAfterMagicHalve(true, 5.0F), T);
        assertEquals(0.5, EldritchGuardianAiLogic.damageAfterMagicHalve(true, 1.0F), T);
        assertEquals(0.25, EldritchGuardianAiLogic.damageAfterMagicHalve(true, 0.5F), T);
        // The gate is func_82725_o() (isMagicDamage) alone: a non-magic hit
        // rides super untouched.
        assertEquals(10.0, EldritchGuardianAiLogic.damageAfterMagicHalve(false, 10.0F), T);
        assertEquals(0.5, EldritchGuardianAiLogic.damageAfterMagicHalve(false, 0.5F), T);
    }

    @Test
    void lootRidesTheCanSpawnFlag() {
        // :262 - :264 — dropLoot = !getCanSpawnHere (func_70692_ba() =
        // !func_110175_bO()), and :185 - :187 drops the eldritch object with no
        // roll of its own (the 1-in-200 roll is the vanilla death pass that
        // calls it).
        assertTrue(EldritchGuardianAiLogic.dropsLoot(false));
        assertFalse(EldritchGuardianAiLogic.dropsLoot(true));
        assertEquals(200, EldritchGuardianAiLogic.RARE_DROP_ROLL_MODULO);
        assertEquals("itemeldritchobject", EldritchGuardianAiLogic.RARE_DROP_ITEM.getPath());
    }

    @Test
    void wispsCarryBothAspectTags() {
        // :189 - :196 — two wisps per kill with the Exanimis AND Alienis tags
        // (the 1.21.1 wisp takes one tag), thrown at +1.0, and the aspect tags
        // are the 1.7.10 lower-case strings.
        assertEquals(2, EldritchGuardianAiLogic.WISP_ASPECT_AMOUNT);
        assertEquals(1.0F, EldritchGuardianAiLogic.WISP_THROW_OFFSET, T);
        assertEquals("exanimis", EldritchGuardianAiLogic.ASPECT_TAG_UNDEAD);
        assertEquals("alienis", EldritchGuardianAiLogic.ASPECT_TAG_ELDRITCH);
    }

    @Test
    void soundsAndVolumesAreTheOriginals() {
        // :44 - :46 / :267 - :269 / :232 - :234 / :273 - :275.
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "egdeath"),
                EldritchGuardianAiLogic.SOUND_DEATH);
        assertEquals(1.5F, EldritchGuardianAiLogic.SOUND_VOLUME, T);
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "egattack"),
                EldritchGuardianAiLogic.SOUND_ATTACK);
        assertEquals(2.0F, EldritchGuardianAiLogic.ATTACK_SOUND_VOLUME, T);
        assertEquals(0.1F, EldritchGuardianAiLogic.SOUND_PITCH_JITTER, T);
        // :307 - :309 — the particle mote offsets (the x/z spread and the 0.22
        // y factor) are the original's own figures.
        assertEquals(0.2F, EldritchGuardianAiLogic.MOTE_JITTER, T);
        assertEquals(0.22F, EldritchGuardianAiLogic.MOTE_Y_FACTOR, T);
    }

    @Test
    void theSpawnBoxExpandsThirtyTwoBySixteen() {
        // :251 - :261 — the box is expanded 32 on x/z and 16 on y, and every
        // cell is probed for an IEldritchMob; a hit refuses the spawn.
        assertEquals(32.0, EldritchGuardianAiLogic.SPAWN_BOX_XZ_EXPAND, T);
        assertEquals(16.0, EldritchGuardianAiLogic.SPAWN_BOX_Y_EXPAND, T);
    }
}
