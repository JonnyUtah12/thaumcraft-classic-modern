package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.resources.ResourceLocation;

/**
 * World-free constants and pure decisions for the Eldritch Guardian —
 * {@code thaumcraft:eldritchguardian} (content-mapping.csv line 323,
 * {@code Thaumcraft:EldritchGuardian}, mod_index 107 per
 * {@code ConfigEntities.java:107}).
 *
 * <p>Original: {@code thaumcraft.common.entities.monster.EntityEldritchGuardian},
 * 310 lines, {@code extends EntityMob implements IRangedAttackMob,
 * IEldritchMob}. The gate-warden: 50 health / 7 attack / armour 4
 * ({@code :69-75, :84-86}), a 0.8 x 2.25 frame and 20 XP ({@code :65-66}), it
 * fires homing eldritch orbs from a 24-block ranged goal ({@code :56, :271-286})
 * — 90% of the shots, the other 10% the sonic blast ({@code :287-304}) — halves
 * magic damage ({@code :96-102}), regenerates below half health inside the
 * Outer Lands ({@code :219-227}), heals half a bar at spawn there
 * ({@code :208-217}), refuses to spawn within 32/16/32 of another guardian
 * ({@code :251-261}) and whispers its presence to overworld players on a
 * 100-tick cadence ({@code :118-132}).</p>
 */
public final class EldritchGuardianAiLogic {

    private EldritchGuardianAiLogic() {
    }

    // ---------------------------------------------------------------- registration

    /** {@code ConfigEntities.java:107} — {@code registerModEntity(guardian,
     *  "EldritchGuardian", 107, ..., 64, 3, true)}: 64 blocks = 4 chunks
     *  (family rule, {@code BrainyZombieAiLogicTest:38}), update 3. */
    public static final int TRACK_RANGE_CHUNKS = 4;
    public static final int UPDATE_INTERVAL = 3;

    /** {@code :65} — {@code func_70105_a(0.8F, 2.25F)}. {@code :247-249} —
     *  eye height 2.1. {@code :66} — 20 XP. */
    public static final float WIDTH = 0.8F;
    public static final float HEIGHT = 2.25F;
    public static final float EYE_HEIGHT = 2.1F;
    public static final int XP = 20;

    // ---------------------------------------------------------------- attributes

    /** {@code :69-75} — health 50, follow range 40, speed 0.28, attack 7.
     *  {@code :84-86} — armour 4, a runtime getter (no 1.7.10 attribute
     *  write), so it ships as the ARMOR base. */
    public static final double MAX_HEALTH = 50.0;
    public static final double FOLLOW_RANGE = 40.0;
    public static final double MOVEMENT_SPEED = 0.28;
    public static final double ATTACK_DAMAGE = 7.0;
    public static final double ARMOR = 4.0;

    // ---------------------------------------------------------------- goals

    /** {@code :55} Swimming slot 0 → {@code FloatGoal}. */
    public static final int GOAL_SLOT_SWIM = 0;
    /** {@code :56} — {@code AILongRangeAttack(this, 8.0, 1.0, 20, 40, 24.0F)}
     *  slot 2. The port ships the family's ported goal
     *  ({@code entities/ai/cultist/AILongRangeAttack}, the cleric's
     *  {@code CultistAiLogic.GOAL_SLOT_RANGED} entry) with the guardian's own
     *  min-distance 8.0. */
    public static final int GOAL_SLOT_RANGED = 2;
    public static final double RANGED_MIN_DISTANCE = 8.0;
    public static final double RANGED_SPEED = 1.0;
    public static final int RANGED_INTERVAL_MIN = 20;
    public static final int RANGED_INTERVAL_MAX = 40;
    public static final float RANGED_RANGE = 24.0F;
    /** {@code :57} — {@code AIAttackOnCollide(this, EntityLivingBase.class,
     *  1.0, false)} slot 3 → {@code MeleeAttackGoal(1.0, false)} (the
     *  family's melee mapping). */
    public static final int GOAL_SLOT_MELEE = 3;
    public static final float MELEE_SPEED = 1.0F;
    public static final boolean MELEE_LONG_MEMORY = false;
    /** {@code :58} — {@code EntityAIMoveTowardsRestriction(this, 0.8)} slot 5. */
    public static final int GOAL_SLOT_RESTRICTION = 5;
    public static final float RESTRICTION_SPEED = 0.8F;
    /** {@code :59} — {@code EntityAIWander(this, 1.0)} slot 7. */
    public static final int GOAL_SLOT_WANDER = 7;
    public static final float WANDER_SPEED = 1.0F;
    /** {@code :60}/{@code :61} — the slot-8 collision: WatchClosest and
     *  LookIdle both land on 8; 1.7.10's TreeMap keeps only the LAST (the
     *  look-idle), 1.21.1's set keeps every goal, so the port ships the pair
     *  at 8 and 9 — BD-112's shape, recorded for the guardian as BD-113. */
    public static final int GOAL_SLOT_WATCH = 8;
    public static final int GOAL_SLOT_LOOK_IDLE = 8;
    public static final int GOAL_SLOT_LOOK_IDLE_PORT = 9;
    public static final float WATCH_PLAYER_RANGE = 8.0F;
    public static final float WATCH_PLAYER_PROBABILITY = 0.02F;
    /** {@code :62} — {@code EntityAIHurtByTarget(this, false)} target slot 1.
     *  The {@code false} = alert nobody. 1.21.1's plain goal alerts the mob's
     *  own class, so the exact form is {@code HurtByTargetGoal(this)
     *  .setAlertOthers()} with the empty varargs — the to-alert set the goal
     *  ships is empty, exactly the original's {@code false}. (The brainy card
     *  mapped its {@code (this, false)} to the plain goal; the guardian ships
     *  the literal form and the probe asserts the empty to-alert set.) */
    public static final int TARGET_SLOT_HURT = 1;
    /** {@code :63}/{@code :64} — player target slot 2, EntityCultist slot 3. */
    public static final int TARGET_SLOT_PLAYER = 2;
    public static final int TARGET_SLOT_CULTIST = 3;

    // ---------------------------------------------------------------- synched data

    /** {@code :77-82} — three byte watchers at ids 12/13/14. NOTHING in the
     *  310 lines reads or writes them (the arm-lift fields of {@code :48-49}
     *  are plain client fields, set by {@code handleEntityEvent} and decayed
     *  in {@code onLivingUpdate}): inert watchers, defined as defined, pinned
     *  here (the ids are 1.7.10 watcher indices; 1.21.1 allocates its own). */
    public static final int WATCHER_12 = 12;
    public static final int WATCHER_13 = 13;
    public static final int WATCHER_14 = 14;

    /** {@code :48-49} — the arm-lift decay, {@code 0.05} per client tick. */
    public static final float ARM_LIFT_DECAY = 0.05F;
    /** {@code :229-241} — event 15 lifts the left arm 0.5, 16 the right 0.5,
     *  17 both 0.9; anything else goes to the parent. The blaster alternates
     *  15/16 ({@code :274-275}). */
    public static final byte EVENT_ARM_LEFT = 15;
    public static final byte EVENT_ARM_RIGHT = 16;
    public static final byte EVENT_ARM_BOTH = 17;
    public static final float ARM_LIFT_HALF = 0.5F;
    public static final float ARM_LIFT_FULL = 0.9F;

    // ---------------------------------------------------------------- home

    /** {@code :189-206} — the Home NBT pair: written only when a home exists
     *  and its range is positive, read on {@code HomeD} presence. Same keys
     *  as the cultist family ({@code CultistAiLogic.NBT_HOME_*}) — the
     *  original uses the same EntityLivingBase home fields, so the
     *  world-side contract (altar/lock anchors) is one key set. */
    public static final String NBT_HOME_X = "HomeX";
    public static final String NBT_HOME_Y = "HomeY";
    public static final String NBT_HOME_Z = "HomeZ";
    public static final String NBT_HOME_D = "HomeD";

    public static boolean writesHome(final boolean hasHome, final int dist) {
        return hasHome && dist > 0;
    }

    // ---------------------------------------------------------------- outer-lands behaviour

    /** {@code :208-217}/{@code :219-227} — in the Outer Lands the guardian
     *  heals {@code (int)(maxHealth / 2)} on top of its spawn health, and
     *  every 25 ticks (outside the hurt-resistance window) gains +1 health
     *  while below half. */
    public static final int REGEN_CADENCE_TICKS = 25;
    public static final float REGEN_PER_TICK = 1.0F;

    public static int spawnHeal(final double maxHealth) {
        return (int) maxHealth / 2;
    }

    public static boolean regenerates(final boolean outerLands, final int invulnerableTime,
            final int age, final float health, final double maxHealth) {
        return outerLands
                && invulnerableTime <= 0
                && age % REGEN_CADENCE_TICKS == 0
                && health < (int) maxHealth / 2;
    }

    /**
     * {@code :118-132} — the overworld whisper: outside the Outer Lands, on
     * age 0 and every 100th tick, on any difficulty except EASY, every living
     * player within
     * {@code HARD ? 576.0 : 256.0} SQUARED blocks receives
     * {@code PacketMiscEvent(2)} (1.21.1's {@code MiscEventPayload(2)}; the
     * wired client arm is fog/vignette, kind 2 — TODO(M4) vignette).
     */
    public static final int WHISPER_CADENCE_TICKS = 100;
    public static final double WHISPER_RADIUS_SQ_HARD = 576.0;
    public static final double WHISPER_RADIUS_SQ = 256.0;

    public static boolean sendsWhisper(final boolean outerLands, final int age,
            final boolean easy) {
        return !outerLands && (age == 0 || age % WHISPER_CADENCE_TICKS == 0) && !easy;
    }

    public static double whisperRadiusSq(final boolean hard) {
        return hard ? WHISPER_RADIUS_SQ_HARD : WHISPER_RADIUS_SQ;
    }

    // ---------------------------------------------------------------- ranged attack

    /** {@code :271-286} — 90% of attacks fire the orb
     *  ({@code nextFloat() > 0.1F}). The muzzle offset rotates the yaw by
     *  90° (right arm, after an odd blast) or 180° (left arm), mod 360, times
     *  {@code 0.5F}; y drops {@code 0.057777777 * height}; the spawn lands on
     *  the orb's own fresh position minus that offset ({@code :280}), and the
     *  shot vector aims at target position + target motion, dropping half the
     *  target's height, at speed 1.0 / inaccuracy 2.0 ({@code :284}). */
    public static final float ORB_SHOT_CHANCE = 0.1F;
    public static final int MUZZLE_DEGREES_RIGHT = 90;
    public static final int MUZZLE_DEGREES_LEFT = 180;
    public static final float MUZZLE_RADIUS = 0.5F;
    public static final float MUZZLE_Y_FACTOR = 0.057777777F;
    public static final float ORB_SPEED = 1.0F;
    public static final float ORB_INACCURACY = 2.0F;

    public static boolean firesOrb(final float roll) {
        return roll > ORB_SHOT_CHANCE;
    }

    /** {@code :287-304} — the sonic branch (10%, and only when the mob has
     *  line of sight to the target): FXSonic to players tracking the
     *  guardian, WITHER 400 ticks amplifier 0 on the target, sticky warp
     *  {@code 1 + worldRandom(3)} on a player target. The 1.7.10
     *  {@code try/catch} around the potion write ships dropped
     *  ({@code MobEffectInstance} construction cannot throw); the
     *  {@code thaumcraft:egscreech} sound (volume 3.0, pitch
     *  {@code 1.0 + worldRandom * 0.2}) is unregistered (D-004) — pinned,
     *  not fired. */
    public static final int SONIC_WITHER_TICKS = 400;
    public static final int SONIC_WITHER_AMPLIFIER = 0;
    public static final int SONIC_WARP_BASE = 1;
    public static final int SONIC_WARP_JITTER = 3;
    public static final ResourceLocation SOUND_SCREECH =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "egscreech");
    public static final float SCREECH_VOLUME = 3.0F;

    /** {@code :135-145} — after a landed melee hit: with an empty
     *  in-use item, an active path, and {@code nextFloat() <
     *  difficultyId * 0.3F}, the target burns {@code 2 * difficultyId}
     *  seconds ({@code func_70015_d} takes seconds; the difficulty ordinal
     *  is 0 peaceful .. 3 hard, so the fire is 0 below NORMAL). */
    public static final float BURN_CHANCE_PER_DIFFICULTY = 0.3F;
    public static final int BURN_SECONDS_PER_DIFFICULTY = 2;

    public static boolean setsFire(final int difficultyId, final boolean inUseItemEmpty,
            final boolean hasPath, final float roll) {
        return inUseItemEmpty && hasPath && roll < difficultyId * BURN_CHANCE_PER_DIFFICULTY;
    }

    public static int fireSecondsFor(final int difficultyId) {
        return BURN_SECONDS_PER_DIFFICULTY * difficultyId;
    }

    // ---------------------------------------------------------------- drops / gates

    /** {@code :163-179} — two independent world-RNG coin flips drop one wisp
     *  essence each: the first imbued with 2 UNDEAD, the second with 2
     *  ELDRITCH, thrown at offset 1.0; then {@code super.dropLoot} runs
     *  (vanilla equipment drop). Aspect tags resolve through the port's
     *  aspect registry ({@code Aspect.getAspect(tag)}); both tags exist. */
    public static final int WISP_ASPECT_AMOUNT = 2;
    public static final float WISP_THROW_OFFSET = 1.0F;
    public static final String ASPECT_TAG_UNDEAD = "unDead";
    public static final String ASPECT_TAG_ELDRITCH = "eldritch";

    /** {@code :185-187} — {@code dropRareDrop(int fortune)}: one
     *  {@code itemEldritchObject} meta 0. 1.7.10 calls it from the vanilla
     *  death pass at 1-in-200; 1.21.1 has no rare-drop seam (the vanilla
     *  per-mob rare drops are hardcoded inside {@code dropCustomDeathLoot}),
     *  so the method ships as the named seam NOT wired — BD-91's shape,
     *  pinned and tested, its item id gated on the live registry (the
     *  eldritch object IS registered, CatalogEldritchRealm:195). */
    public static final int RARE_DROP_ROLL_MODULO = 200;
    public static final ResourceLocation RARE_DROP_ITEM =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "itemeldritchobject");

    /** {@code :251-261} — spawn refusal: the unit box at the feet, expanded
     *  32 x 16 x 32; any guardian inside, and the spawn is refused. */
    public static final double SPAWN_BOX_XZ_EXPAND = 32.0;
    public static final double SPAWN_BOX_Y_EXPAND = 16.0;

    /** {@code :263-265} — {@code func_70814_o()} (canPickUpLoot) true. */
    public static final boolean CAN_PICK_UP_LOOT = true;

    /** {@code :88-90} — the boss flag, shipped inert (BD-69's shape: 1.21.1
     *  has no {@code Mob#isBoss()}; no bar is drawn). */
    public static final boolean IS_BOSS = true;

    /** {@code :155-157} — air supply 500. {@code :159-161} — pickup item is
     *  air (inert, the pickup path is open ({@code CAN_PICK_UP_LOOT true})
     *  and the 1.21.1 picked-item slot ships empty, as the original's air). */
    public static final int MAX_AIR = 500;

    /** {@code :243-245} — {@code func_70692_ba()} (dropsLoot) = NOT idle.
     *  The idle flag rides 1.7.10's public {@code setEntityIdle}; 1.21.1 has
     *  no such hook, so the field ships as the named seam (default false =
     *  drops on, the original's live behaviour — nothing on the guardian
     *  ever sets idle true; the 1.7.10 callers are structure code that
     *  never fires for guardians). */
    public static final boolean IDLE_DEFAULT = false;

    /** {@code :147-153} — ambient {@code thaumcraft:egidle}, death
     *  {@code thaumcraft:egdeath}: unregistered (D-004), pinned not fired.
     *  The hurt is the vanilla default (no override in the 310 lines). */
    public static final ResourceLocation SOUND_AMBIENT =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "egidle");
    public static final ResourceLocation SOUND_DEATH =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "egdeath");

    /** {@code :267-269} — sound volume 1.5. */
    public static final float SOUND_VOLUME = 1.5F;

    /** {@code :96-102} — magic damage is halved. */
    public static float damageAfterMagicHalve(final boolean magic, final float damage) {
        return magic ? damage / 2.0F : damage;
    }

    /** {@code :104-117} — the client arm: per client tick both arm lifts
     *  decay by 0.05 while above 0, and one EG mote spawns per tick at
     *  {@code x/z + (rand - rand) * 0.2} and {@code y + 0.22 * height},
     *  targeting the guardian itself (the shipped sender is
     *  {@code TcFxPlans.wispEg} at the fancy particle setting, the
     *  original's {@code particleCount(1)}). */
    public static final float MOTE_JITTER = 0.2F;
    public static final float MOTE_Y_FACTOR = 0.22F;
}
