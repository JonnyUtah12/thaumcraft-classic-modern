package com.jonnyutah12.thaumcraftclassic.entities.monster;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Every constant and rule of the crimson cultist family, kept {@code Level}-free
 * so the unit test can pin them without a world.
 *
 * <p>Original: {@code thaumcraft.common.entities.monster.EntityCultist} (108
 * lines, {@code extends EntityMob}), {@code EntityCultistKnight} (73 lines) and
 * {@code EntityCultistCleric} (180 lines, {@code implements IRangedAttackMob,
 * IEntityAdditionalSpawnData}). Ledger rows
 * {@code docs/content-mapping.csv} {@code Thaumcraft:CultistKnight} →
 * {@code thaumcraft:cultistknight} (mod_index 27) and
 * {@code Thaumcraft:CultistCleric} → {@code thaumcraft:cultistcleric}
 * (mod_index 28); registration
 * {@code common/config/ConfigEntities.java:111} and {@code :113} — both
 * {@code (instance, 64, 3, true)}.</p>
 *
 * <p><b>The base takes no registry id.</b> {@code EntityCultist.java} carries no
 * {@code registerModEntity} line and has no ledger row: the original registers
 * only Knight, Cleric, Leader and Portal. The base is the named goal target of
 * {@code EntityEldritchCrab.java:44}, {@code EntityEldritchGuardian.java:64},
 * {@code EntityInhabitedZombie.java:30} and {@code EntityEldritchWarden.java:79},
 * so it lands here as the shared, unregistered parent — see
 * {@link EntityCultist}'s own javadoc.</p>
 *
 * <p>Every figure cites the decompiled line it came from. The 1.7.10 SRG names
 * resolve against {@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv}
 * (verified here: {@code field_111265_b=followRange},
 * {@code field_111267_a=maxHealth}, {@code field_111263_d=movementSpeed},
 * {@code field_111264_e=attackDamage}, {@code field_70728_aV=experienceValue},
 * {@code field_151040_l=iron_sword}, {@code field_70180_af=dataWatcher},
 * {@code field_70714_bg=tasks}, {@code field_70715_bh=targetTasks},
 * {@code field_73013_u=difficultySetting}). The methods CSV is not vendored, so
 * vanilla method hooks are resolved by behaviour and named in the entity's
 * javadoc.</p>
 */
public final class CultistAiLogic {

    // ---- registration (ConfigEntities.java:111 knight, :113 cleric) ----
    /** {@code registerModEntity(EntityCultistKnight.class, "CultistKnight", 27,
     *  instance, 64, 3, true)} — the ledger row's {@code mod_index=27}. */
    public static final int KNIGHT_MOD_INDEX = 27;
    /** {@code registerModEntity(EntityCultistCleric.class, "CultistCleric", 28,
     *  instance, 64, 3, true)} — the ledger row's {@code mod_index=28}. */
    public static final int CLERIC_MOD_INDEX = 28;
    public static final int TRACK_RANGE_BLOCKS = 64;
    /** 1.21.1's {@code EntityType.Builder#clientTrackingRange} counts CHUNKS, so
     *  the original's 64 blocks become 4 — the family rule pinned at
     *  {@code BrainyZombieAiLogicTest:38} ("64 blocks is 4"). */
    public static final int TRACK_RANGE_CHUNKS = TRACK_RANGE_BLOCKS / 16;
    public static final int UPDATE_INTERVAL = 3;
    public static final boolean VELOCITY_UPDATES = true;
    /** The {@code EntityCultist} base has no registration call, no mod index and
     *  no ledger row ({@code EntityCultist.java} — no {@code registerModEntity}
     *  line exists in the family block of {@code ConfigEntities.init}). */
    public static final boolean BASE_IS_REGISTERED = false;

    // ---- base geometry / flags (EntityCultist.java) ----
    /** {@code :18} — {@code func_70105_a(0.6F, 1.8F)}. */
    public static final float WIDTH = 0.6F;
    public static final float HEIGHT = 1.8F;
    /** {@code :19} — {@code field_70728_aV = 10} (experienceValue). */
    public static final int XP = 10;
    /** {@code :20-21} — {@code getNavigator().setCanBreakDoors(true)}
     *  ({@code func_75498_b}) and {@code setCanSwim(true)} ({@code func_75491_a});
     *  the pair is 1.21.1's {@code setCanOpenDoors}/{@code setCanFloat}. */
    public static final boolean NAVIGATOR_BREAKS_DOORS = true;
    public static final boolean NAVIGATOR_SWIMS = true;
    /** {@code :35-37} — {@code func_98052_bS()} ({@code getCanPickUpLoot})
     *  returns {@code false}. */
    public static final boolean CAN_PICK_UP_LOOT = false;
    /** {@code :39-41} — {@code func_70650_aV()} (the boss flag) returns true.
     *  1.21.1 has no {@code isBossEntity} hook (boss bars are
     *  {@code ServerBossEvent}s), so this is pinned as the original's flag and
     *  shipped inert, the same posture the pech took ({@code PechAiLogic.IS_BOSS}). */
    public static final boolean IS_BOSS = true;
    /** {@code :43-45} — {@code func_146068_u()} ({@code getDropItem}) returns
     *  {@code Item.func_150899_d(0)} = item id 0 = air. 1.21.1's pickup path is
     *  gated by {@code canPickUpLoot()}, which this family pins false, so no
     *  pickup-item override is installed (recorded in known_differences). */
    public static final int DROP_ITEM_ID = 0;

    // ---- base attributes (EntityCultist.java:24-29) ----
    /** {@code :26} — {@code field_111265_b} = {@code followRange} = 32.0. */
    public static final double FOLLOW_RANGE = 32.0;
    /** {@code :27} — {@code field_111263_d} = {@code movementSpeed} = 0.3. */
    public static final double MOVEMENT_SPEED = 0.3;
    /** {@code :28} — {@code field_111264_e} = {@code attackDamage} = 4.0. */
    public static final double ATTACK_DAMAGE = 4.0;
    /** The base never touches {@code maxHealth} ({@code field_111267_a}); the
     *  vanilla mob default 20.0 is what the family walks in with. */
    public static final double BASE_MAX_HEALTH = 20.0;
    /** {@code EntityCultistKnight.java:41} — {@code field_111267_a}
     *  ({@code maxHealth}) = 36.0. The knight adds no attack and no armour
     *  value: its defence is the plate it equips ({@code :46-49}). */
    public static final double KNIGHT_MAX_HEALTH = 36.0;
    /** {@code EntityCultistCleric.java:51} — {@code field_111267_a}
     *  ({@code maxHealth}) = 30.0. */
    public static final double CLERIC_MAX_HEALTH = 30.0;
    /** {@code EntityCultistCleric.java:177-179} — {@code func_70627_aG()}
     *  ({@code getMaxAir}) = 500; vanilla's is 300. */
    public static final int CLERIC_MAX_AIR = 500;

    // ---- base death drops (EntityCultist.java:47-62) ----
    /** {@code :48} — {@code field_70146_Z.nextInt(10)}. */
    public static final int DROP_ROLL_MODULO = 10;
    /** {@code :50} — roll 0 → {@code itemResource} meta 9. */
    public static final int DROP_META_FIRST = 9;
    /** {@code :52} — roll ≤ 1 → {@code itemResource} meta 17. */
    public static final int DROP_META_SECOND = 17;
    /** {@code :54} — roll ≤ {@code 3 + looting} → {@code itemResource} meta 18. */
    public static final int DROP_META_THIRD = 18;
    /** {@code :53} — the looting term of the third branch: {@code 3 + i}. */
    public static final int DROP_THIRD_BASE = 3;
    /** {@code :50/:52/:54} — every drop leaves at the 1.5 throw offset. */
    public static final float DROP_THROW_OFFSET = 1.5F;
    /** The item the three rolls drop: the meta-carrying {@code thaumcraft:itemresource}. */
    public static final ResourceLocation DROP_ITEM =
            ResourceLocation.parse("thaumcraft:itemresource");
    /**
     * {@code :47-58} — {@code func_70628_a(boolean, int)} (dropFewItems). The
     * roll is exclusive top-down: {@code r == 0} → meta 9, {@code r <= 1} → meta
     * 17, {@code r <= 3 + looting} → meta 18, else nothing.
     */
    public static int dropMetaFor(final int roll, final int looting) {
        if (roll == 0) {
            return DROP_META_FIRST;
        }
        if (roll <= 1) {
            return DROP_META_SECOND;
        }
        if (roll <= DROP_THIRD_BASE + looting) {
            return DROP_META_THIRD;
        }
        return -1;
    }

    /** {@code :60-62} — {@code func_82160_m(int)} ({@code dropAllLoot}) replaces
     *  the parent's equipment drop with one {@code itemEldritchObject} meta 1
     *  (Crimson Rites) at the 1.0 offset. */
    public static final ResourceLocation ALL_LOOT_ITEM =
            ResourceLocation.parse("thaumcraft:itemeldritchobject");
    public static final int ALL_LOOT_META = 1;
    public static final float ALL_LOOT_THROW_OFFSET = 1.0F;

    // ---- faction hooks (EntityCultist.java:101-107) ----
    /**
     * {@code :101-103} — {@code func_142014_c(EntityLivingBase)}
     * ({@code canAttack}) is {@code el instanceof EntityCultist || el instanceof
     * EntityCultistLeader}: a cultist treats its own faction as attackable, which
     * is what lets {@code AICultistHurtByTarget} retarget the gang onto the
     * attacker. The Leader arrives with card c5, so the port's predicate covers
     * the two shipped cultists plus the base.
     */
    public static boolean canAttackFactionMember(final Object target) {
        return target instanceof EntityCultist;
    }
    /**
     * {@code :105-107} — {@code func_70686_a(Class)} returns false for the Cleric,
     * the Leader and the Knight (so the vanilla target selectors skip their own
     * kind) and defers to {@code super} for everything else. Pinned in the
     * original's class form; 1.21.1's hook is the {@code EntityType} form, so the
     * shipped predicate is {@link #isCultistTypeId(ResourceLocation)}.
     *
     * <p>The Cleric and the Leader are named by class NAME, not by class: the
     * Cleric is card t_6160444c's (c4a-2, the half this lane sliced out) and the
     * Leader is c5's, so neither class is in this tree (BD-87 — a name-carried
     * exclusion arm, the same shape this method already used for the Leader).
     * The behaviour is unchanged for the shipped pair: the Knight is the only
     * cultist registered here, and its exclusion runs on the real class.</p>
     */
    public static boolean targetClassExcluded(final Class<?> targetClass) {
        final String simpleName = targetClass.getSimpleName();
        return CLERIC_CLASS_NAME.equals(simpleName)
                || EntityCultistKnight.class.equals(targetClass)
                || LEADER_CLASS_NAME.equals(simpleName);
    }
    /** {@code EntityCultistCleric} is card t_6160444c's class (c4a-2) and is not
     *  in this tree, so the exclusion set carries its name verbatim from
     *  {@code :106} — the same name-carried arm as the Leader (BD-87). */
    public static final String CLERIC_CLASS_NAME = "EntityCultistCleric";
    /** {@code EntityCultistLeader} is card c5's class and is not in the tree yet,
     *  so the exclusion set carries its name verbatim from {@code :106}. */
    public static final String LEADER_CLASS_NAME = "EntityCultistLeader";
    /** The three cultist types the original registers
     *  ({@code ConfigEntities.java:111/:113/:115} — Knight 27, Cleric 28,
     *  Leader 29): the id set of {@link #isCultistTypeId}. The base is not in it
     *  (it is unregistered) and {@code CultistPortal} is not a mob. The Cleric's
     *  id is carried by path so card t_6160444c needs no edit here. */
    public static final java.util.List<String> CULTIST_TYPE_PATHS =
            java.util.List.of("cultistknight", "cultistcleric", "cultistleader");
    /** The Cleric's registry id path — the one cultist the gang call's ritual arm
     *  of {@code AICultistHurtByTarget:48-53} reads. It is pinned as a path
     *  because the class is card t_6160444c's (BD-87). */
    public static final String CLERIC_TYPE_PATH = "cultistcleric";
    /** The registry-id form of "is this the Cleric": true only once
     *  {@code thaumcraft:cultistcleric} is registered by card t_6160444c, so on
     *  this lane the gang call takes the original's {@code :49-50} arm for every
     *  shipped cultist. */
    public static boolean isClericTypeId(final ResourceLocation id) {
        return id != null && "thaumcraft".equals(id.getNamespace())
                && CLERIC_TYPE_PATH.equals(id.getPath());
    }
    /**
     * The {@code EntityType} form of the {@code :105-107} exclusion: a type is a
     * cultist when its registry id is one of the three the original registers.
     * The Leader's id is carried by path so card c5 needs no edit here; the
     * base's id does not exist, by design.
     */
    public static boolean isCultistTypeId(final ResourceLocation id) {
        return "thaumcraft".equals(id.getNamespace()) && CULTIST_TYPE_PATHS.contains(id.getPath());
    }
    /**
     * {@code EntityCultistCleric.java:152-154} — the ritual look angles inside
     * {@code func_70071_h_}, returned as {@code (yaw, pitch, 0)} in the order the
     * original’s {@code updateRotation} pair takes them. The original writes
     * {@code d3 = sqrt(d0*d0 + d2*d2)} ({@code :152}, the HORIZONTAL plane),
     * {@code f = atan2(d2, d0) * 180/PI - 90} ({@code :153}) and
     * {@code f1 = -(atan2(d1, d3) * 180/PI)} ({@code :154}) — degrees in, degrees
     * out — so the yaw's -90 and the pitch's negation are the original's own, not
     * a platform conversion.
     */
    public static Vec3 clericLookAngles(final double dx, final double dy, final double dz) {
        final double flat = Math.sqrt(dx * dx + dz * dz);                              // :152
        final float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        final float pitch = Mth.wrapDegrees((float) (-Math.toDegrees(Math.atan2(dy, flat))));
        return new Vec3(yaw, pitch, 0.0);
    }

    // ---- goal slots (shared by both registered cultists) ----
    /** {@code EntityCultistKnight.java:26} / {@code EntityCultistCleric.java:34}
     *  — {@code EntityAISwimming} slot 0 → 1.21.1 {@code FloatGoal}. */
    public static final int GOAL_SLOT_SWIM = 0;
    /** {@code EntityCultistCleric.java:35} — {@code AIAltarFocus(this)} slot 1. */
    public static final int GOAL_SLOT_ALTAR = 1;
    /** {@code EntityCultistCleric.java:36} — {@code AILongRangeAttack(this, 2.0,
     *  1.0, 20, 40, 24.0F)} slot 2. */
    public static final int GOAL_SLOT_RANGED = 2;
    /** {@code Knight:27} / {@code Cleric:37} — {@code AIAttackOnCollide(this,
     *  EntityLivingBase.class, 1.0, false)} slot 3. */
    public static final int GOAL_SLOT_MELEE = 3;
    /** {@code Knight:28} / {@code Cleric:38} — {@code EntityAIRestrictOpenDoor}
     *  slot 4. */
    public static final int GOAL_SLOT_DOOR_RESTRICT = 4;
    /** {@code Knight:29} / {@code Cleric:39} — {@code EntityAIOpenDoor(this, true)}
     *  slot 5 → 1.21.1 {@code OpenDoorGoal(this, true)}. */
    public static final int GOAL_SLOT_DOOR_OPEN = 5;
    /** {@code Knight:30} / {@code Cleric:40} — {@code EntityAIMoveTowardsRestriction(this, 0.8)} slot 6. */
    public static final int GOAL_SLOT_RESTRICTION = 6;
    /** {@code Knight:31} / {@code Cleric:41} — {@code EntityAIWander(this, 0.8)} slot 7. */
    public static final int GOAL_SLOT_WANDER = 7;
    /** {@code Knight:32-33} / {@code Cleric:42-43} — {@code EntityAIWatchClosest}
     *  and {@code EntityAILookIdle} share slot 8. */
    /** {@code AICultistHurtByTarget(this, true)} — the {@code true} is the
     *  retaliation flag the original passes at {@code Knight:34}/{@code Cleric:44}. */
    public static final boolean IS_AGRESSORS = true;
    /** 1.21.1’s {@code dropCustomDeathLoot} carries no looting level (Mob:979), so
     *  the roll table reads the window’s floor {@code 3 + 0} (BD-90). */
    public static final int DROP_LOOTING_BASELINE = 0;
    public static final int GOAL_SLOT_WATCH = 8;
    public static final int GOAL_SLOT_LOOK_IDLE = 8;
    /** {@code Knight:34} / {@code Cleric:44} — {@code AICultistHurtByTarget(this,
     *  true)} target slot 1. */
    public static final int TARGET_SLOT_HURT = 1;
    /** {@code Knight:35} / {@code Cleric:45} — {@code EntityAINearestAttackableTarget(this,
     *  EntityPlayer.class, 0, true)} target slot 2 (the {@code 0} is 1.7.10's
     *  goal priority, which the slot overrides, and the {@code true} is the
     *  line-of-sight gate → 1.21.1's {@code mustSee}). */
    public static final int TARGET_SLOT_PLAYER = 2;
    public static final float MELEE_SPEED = 1.0F;
    public static final boolean MELEE_LONG_MEMORY = false;
    public static final float RESTRICTION_SPEED = 0.8F;
    public static final float WANDER_SPEED = 0.8F;
    public static final float WATCH_PLAYER_RANGE = 8.0F;
    /** 1.7.10's {@code EntityAIWatchClosest} hardcodes the 0.02 chance 1.21.1's
     *  ctor takes as a parameter (the pech's rule). */
    public static final float WATCH_PLAYER_PROBABILITY = 0.02F;
    /** {@code EntityCultistCleric.java:36} — {@code AILongRangeAttack} args:
     *  {@code (min 2.0, speed 1.0, interval 20..40, range 24.0F)}. */
    public static final double RANGED_MIN_DISTANCE = 2.0;
    public static final double RANGED_SPEED = 1.0;
    public static final int RANGED_INTERVAL_MIN = 20;
    public static final int RANGED_INTERVAL_MAX = 40;
    public static final float RANGED_RANGE = 24.0F;

    // ---- AILongRangeAttack gate (AILongRangeAttack.java:18-38) ----
    /** {@code :26-29} — a dead target clears the mob's target and the goal
     *  refuses. {@code :31-34} — the distance test is on the SQUARED distance:
     *  {@code distSq < minDistance * minDistance} refuses, so 2.0 means 2.0
     *  blocks, compared squared. */
    public static boolean rangedGate(final boolean hasTarget, final boolean targetDead,
            final double distanceSquared) {
        if (!hasTarget) {
            return false;
        }
        if (targetDead) {
            return false;
        }
        return !(distanceSquared < RANGED_MIN_DISTANCE * RANGED_MIN_DISTANCE);
    }

    // ---- AICultistHurtByTarget (AICultistHurtByTarget.java) ----
    /** {@code :17} — {@code setPriority(1)}; the target-list slot is 1. */
    public static final int HURT_PRIORITY = 1;
    /** {@code :29/:35-43} — the help box is the unit cube at the mob's feet
     *  expanded by the follow range on x/z and 10.0 on y. */
    public static final double HELP_BOX_Y_EXPAND = 10.0;
    /** {@code :45-47} — only a cultist with no target, that cannot attack the
     *  attacker, joins the fight. */
    public static boolean helpsTheHurt(final boolean isSelf, final boolean hasTarget) {
        return !isSelf && !hasTarget;
    }
    /** {@code :48-53} — a cleric mid-ritual ignores the call, except on
     *  {@code world.rand.nextInt(3) == 0}, which abandons the ritual and joins. */
    public static final int RITUALIST_BREAK_ROLL = 3;
    public static boolean ritualistBreaks(final int nextInt) {
        return nextInt == 0;
    }

    // ---- AIAltarFocus (AIAltarFocus.java) ----
    /** {@code :16} — {@code setPriority(7)}; the goal slot is 1 on the cleric. */
    public static final int ALTAR_PRIORITY = 7;
    /** {@code :35} — the check runs every 40 ticks of the cleric's age. */
    public static final int ALTAR_CADENCE_TICKS = 40;
    /** {@code :37} — the ritual breaks when {@code home.distSqr(feet) > 16.0F}
     *  (the squared test, so 4 blocks). */
    public static final double ALTAR_MAX_DIST_SQ = 16.0F;
    /** {@code :38-43} — and when the home block is no longer
     *  {@code ConfigBlocks.blockEldritch}. */
    public static final ResourceLocation ALTAR_HOME_BLOCK =
            ResourceLocation.parse("thaumcraft:blockeldritch");

    // ---- cleric equipment (EntityCultistCleric.java:55-62) ----
    public static final ResourceLocation HELMET_CULTIST_ROBE =
            ResourceLocation.parse("thaumcraft:helmcultistrobe");
    public static final ResourceLocation CHEST_CULTIST_ROBE =
            ResourceLocation.parse("thaumcraft:chestcultistrobe");
    public static final ResourceLocation LEGS_CULTIST_ROBE =
            ResourceLocation.parse("thaumcraft:legscultistrobe");
    public static final ResourceLocation BOOTS_CULTIST =
            ResourceLocation.parse("thaumcraft:bootscultist");
    /** {@code :59} — the boots roll: HARD 0.3F, otherwise 0.1F. */
    public static final float CLERIC_BOOTS_CHANCE_HARD = 0.3F;
    public static final float CLERIC_BOOTS_CHANCE = 0.1F;

    // ---- knight equipment (EntityCultistKnight.java:45-64) ----
    public static final ResourceLocation HELMET_CULTIST_PLATE =
            ResourceLocation.parse("thaumcraft:helmcultistplate");
    public static final ResourceLocation CHEST_CULTIST_PLATE =
            ResourceLocation.parse("thaumcraft:chestcultistplate");
    public static final ResourceLocation LEGS_CULTIST_PLATE =
            ResourceLocation.parse("thaumcraft:legscultistplate");
    public static final ResourceLocation SWORD_VOID =
            ResourceLocation.parse("thaumcraft:swordvoid");
    public static final ResourceLocation SWORD_THAUMIUM =
            ResourceLocation.parse("thaumcraft:swordthaumium");
    /** {@code :62} — the common weapon: {@code Items.field_151040_l} =
     *  {@code iron_sword} — a real 1.21.1 item, so this branch is not gated. */
    public static final ResourceLocation SWORD_IRON =
            ResourceLocation.withDefaultNamespace("iron_sword");
    /** {@code :50} — the elite roll: HARD 0.05F, otherwise 0.01F. */
    public static final float KNIGHT_ELITE_CHANCE_HARD = 0.05F;
    public static final float KNIGHT_ELITE_CHANCE = 0.01F;
    /** {@code :51} — {@code nextInt(5)}; {@code :52} roll 0 = void sword + robe
     *  helmet, {@code :56-59} otherwise thaumium sword and a bare head on
     *  {@code nextBoolean()}. */
    public static final int KNIGHT_ELITE_ROLL = 5;
    public static final int KNIGHT_ELITE_VOID_ROLL = 0;
    /** The knight's slot layout: {@code func_70062_b(slot, stack)} uses 1.7.10's
     *  armour slot ordinals (0 mainhand, 1 boots, 2 legs, 3 chest, 4 helmet). */
    public static final int SLOT_MAINHAND = 0;
    public static final int SLOT_BOOTS = 1;
    public static final int SLOT_LEGS = 2;
    public static final int SLOT_CHEST = 3;
    public static final int SLOT_HELMET = 4;
    /** The plate set the knight always wears ({@code :46-49}), in slot order. */
    public static Map<Integer, ResourceLocation> knightPlate() {
        final Map<Integer, ResourceLocation> plate = new LinkedHashMap<>();
        plate.put(SLOT_HELMET, HELMET_CULTIST_PLATE);
        plate.put(SLOT_CHEST, CHEST_CULTIST_PLATE);
        plate.put(SLOT_LEGS, LEGS_CULTIST_PLATE);
        plate.put(SLOT_BOOTS, BOOTS_CULTIST);
        return plate;
    }
    /** The robe set the cleric always wears ({@code :56-58}) — no boots: those
     *  are the {@code :59} roll. */
    public static Map<Integer, ResourceLocation> clericRobe() {
        final Map<Integer, ResourceLocation> robe = new LinkedHashMap<>();
        robe.put(SLOT_HELMET, HELMET_CULTIST_ROBE);
        robe.put(SLOT_CHEST, CHEST_CULTIST_ROBE);
        robe.put(SLOT_LEGS, LEGS_CULTIST_ROBE);
        return robe;
    }
    /** {@code :50-63} — the elite branch: {@code chance = hard ? 0.05 : 0.01},
     *  then roll 0 → void sword + robe helmet, else thaumium sword and, on
     *  {@code nextBoolean()}, no helmet at all. */
    public static boolean knightIsElite(final float roll, final boolean hard) {
        return roll < (hard ? KNIGHT_ELITE_CHANCE_HARD : KNIGHT_ELITE_CHANCE);
    }
    public static boolean knightCarriesVoidSword(final int eliteRoll) {
        return eliteRoll == KNIGHT_ELITE_VOID_ROLL;
    }

    // ---- knight weapon enchantment (EntityCultistKnight.java:67-72) ----
    /** {@code :68} — {@code world.func_147462_b(x, y, z)} = the light at the
     *  knight's feet (1.7.10's {@code getLightFromNeighbors}, i.e.
     *  {@code getLightEngine().getRawBrightness(pos, 10)} in 1.21.1).
     *  {@code :69-70} — with a held item, on {@code rand < 0.25F * light}, the
     *  weapon is enchanted at {@code (int)(5.0F + light * rand.nextInt(18))}. */
    public static final float ENCHANT_LIGHT_CHANCE = 0.25F;
    public static final float ENCHANT_BASE_LEVEL = 5.0F;
    public static final int ENCHANT_LEVEL_ROLL = 18;
    public static boolean knightEnchantsWeapon(final float roll, final float light) {
        return roll < ENCHANT_LIGHT_CHANCE * light;
    }
    public static int knightEnchantLevel(final float light, final int nextInt) {
        return (int) (ENCHANT_BASE_LEVEL + light * nextInt);
    }

    // ---- cleric ritualist state (EntityCultistCleric.java:96-134) ----
    /** {@code :99} — {@code dataWatcher.addObject(16, (byte) 0)}. 1.21.1
     *  allocates synched ids, so 16 is pinned as the original's index and the
     *  shipped accessor carries the same payload. */
    public static final int DATA_INDEX_ORIGINAL = 16;
    /** {@code :103} — the ritualist bit is bit 0 of that byte. */
    public static final byte DATA_BIT_RITUALIST = 1;
    /** {@code :106-113} — set/clear is {@code v | 1} / {@code v & -2}. */
    public static byte setRitualist(final byte flags, final boolean value) {
        return value ? (byte) (flags | DATA_BIT_RITUALIST) : (byte) (flags & ~DATA_BIT_RITUALIST);
    }
    public static boolean isRitualist(final byte flags) {
        return (flags & DATA_BIT_RITUALIST) != 0;
    }
    /** {@code :125-134} — the byte round-trips through the NBT key
     *  {@code "Flags"} (the original writes the whole watched byte, not a boolean). */
    public static final String NBT_FLAGS = "Flags";
    /** {@code :92-94} — {@code func_70692_ba()} (the LOOTING gate) is
     *  {@code !getIsRitualist()}: a cleric mid-ritual drops nothing. Per the
     *  {@code EntityTaintacleGiant} rule this is NOT wired to 1.21.1's
     *  {@code shouldDropLoot()}, which gates the whole death-loot block. */
    public static boolean clericDropsLoot(final boolean ritualist) {
        return !ritualist;
    }
    /** {@code :115-122} — any damage clears the ritual (and an invulnerable
     *  cleric takes none). */
    public static boolean damageClearsRitual() {
        return true;
    }

    // ---- cleric spawn payload (EntityCultistCleric.java:136-144) ----
    /** {@code :137-139} — {@code writeSpawnData} writes the home position as
     *  THREE ints (x, y, z) and nothing else; {@code :142-144} reads them back
     *  and applies them as {@code setHomePosAndDistance(x, y, z, 8)}. */
    public static final int SPAWN_INT_COUNT = 3;
    public static final int SPAWN_HOME_DISTANCE = 8;

    // ---- home NBT (EntityCultist.java:84-99) ----
    /** {@code :86-88} — read applies the home only when {@code HomeD} is present;
     *  {@code :93-98} — write emits {@code HomeD} (the radius as an int),
     *  {@code HomeX/Y/Z} only when a home exists and its radius is > 0. */
    public static final String NBT_HOME_D = "HomeD";
    public static final String NBT_HOME_X = "HomeX";
    public static final String NBT_HOME_Y = "HomeY";
    public static final String NBT_HOME_Z = "HomeZ";
    public static boolean writesHome(final boolean hasHome, final float radius) {
        return hasHome && radius > 0.0F;
    }

    // ---- cleric ranged attack (EntityCultistCleric.java:64-89) ----
    /** {@code :69} — on {@code nextFloat() > 0.66F} the cleric throws the orb. */
    public static final float ORB_ROLL_THRESHOLD = 0.66F;
    public static boolean firesOrb(final float roll) {
        return roll > ORB_ROLL_THRESHOLD;
    }
    /** {@code :70} — the orb is the RED variant: {@code new EntityGolemOrb(world,
     *  this, target, true)}. */
    public static final boolean CLERIC_ORB_IS_RED = true;
    /** {@code :71-73} — the orb is nudged by half its own velocity, then
     *  {@code :74} — {@code setThrowableHeading(d0, d1 + 2.0, d2, 0.66F, 3.0F)}. */
    public static final double ORB_NUDGE_FACTOR = 0.5;
    public static final double ORB_AIM_LIFT = 2.0;
    public static final float ORB_VELOCITY = 0.66F;
    public static final float ORB_INACCURACY = 3.0F;
    /** {@code :75} — the attack sound: {@code thaumcraft:egattack} at volume 1.0,
     *  pitch {@code 1.0F + rand * 0.1F}. The sound event is the audio lane's
     *  (docs/sound-events.md), not this card's, so the id is pinned and not fired. */
    public static final ResourceLocation SOUND_ORB_ATTACK =
            ResourceLocation.parse("thaumcraft:egattack");
    public static final float SOUND_ORB_VOLUME = 1.0F;
    public static final float SOUND_ORB_PITCH_JITTER = 0.1F;
    /** {@code :78} — the fireball branch spreads by {@code sqrtFloat(distance) *
     *  0.5F} on x and z only. */
    public static final float FIREBALL_SPREAD_FACTOR = 0.5F;
    /** {@code :81} — three fireballs. */
    public static final int FIREBALL_COUNT = 3;
    /** {@code :79} — {@code playAuxSFX(1009, x, y, z, 0)}: the portal-ish puff at
     *  the cleric. 1.21.1 has no aux-sfx ids; the particle is the FX lane's. */
    public static final int FIREBALL_AUX_SFX = 1009;
    /** {@code :85} — each fireball starts at {@code posY + height / 2 + 0.5}. */
    public static final float FIREBALL_HEIGHT_OFFSET = 0.5F;

    // ---- cleric ritualist client look (EntityCultistCleric.java:146-158) ----
    /** {@code :148} — client-side only ({@code field_70172_f.field_72995_K}), and
     *  only while mid-ritual: the cleric stares at its altar.
     *  {@code :149-151} — the look target is the home corner {@code +0.5, +1.5, +0.5};
     *  {@code :155-156} — pitch steps at 10.0F, yaw at the mob's rotation limit. */
    public static final double RITUAL_LOOK_XZ_OFFSET = 0.5;
    public static final double RITUAL_LOOK_Y_OFFSET = 1.5;
    public static final float RITUAL_LOOK_PITCH_STEP = 10.0F;

    /** {@code EntityCultistCleric.java:173-175} — {@code func_70639_aQ()}
     *  (getAmbientSound) = {@code thaumcraft:chant}. The event is the audio lane's,
     *  so the id is pinned and no unregistered event is fired (a missing id is a
     *  boot-log warning on every ambient tick). */
    public static final ResourceLocation SOUND_AMBIENT =
            ResourceLocation.parse("thaumcraft:chant");

    private CultistAiLogic() {
    }
}
