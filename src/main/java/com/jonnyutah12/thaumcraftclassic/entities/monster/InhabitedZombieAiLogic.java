package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * World-free constants and pure decisions for the Inhabited Zombie —
 * {@code thaumcraft:inhabitedzombie} (content-mapping.csv line 331,
 * {@code Thaumcraft:InhabitedZombie}, mod_index 123 per
 * {@code ConfigEntities.java:123}, display name "Inhabited Zombie").
 *
 * <p>Original: {@code thaumcraft.common.entities.monster.EntityInhabitedZombie},
 * 115 lines, {@code extends EntityZombie}. It is the zombie that guards the
 * eldritch gates: it wears Crimson plate on spawn, hits for nothing (its melee
 * body is empty), never calls reinforcements, refuses to spawn near another of
 * its kind, and on death it leaves an Eldritch Crab in its place and throws its
 * XP orbs.</p>
 *
 * <p>Every figure lives here; the entity class only wires. The goal-set
 * reconciliation the card demands is {@link #stripsMeleeGoal()} / {@link
 * #GOAL_SLOT_MELEE} / {@link #TARGET_SLOT_CULTIST} and the shipped-set census in
 * {@link #SHIPPED_GOAL_COUNT} / {@link #SHIPPED_TARGET_COUNT}, asserted by
 * {@code EldritchMobGameTest}.</p>
 */
public final class InhabitedZombieAiLogic {

    private InhabitedZombieAiLogic() {
    }

    // ------------------------------------------------------------- registration

    /** {@code ConfigEntities.java:123} — {@code registerModEntity(..., 64, 3,
     *  true)}: tracking 64 blocks (4 chunks), update interval 3. */
    public static final int TRACK_RANGE_CHUNKS = 4;
    public static final int UPDATE_INTERVAL = 3;

    /** {@code EntityZombie:96-98} (1.7.10's size, inherited) and the eye height. */
    public static final float WIDTH = 0.6F;
    public static final float HEIGHT = 1.95F;
    public static final float EYE_HEIGHT = 1.74F;

    /** The vanilla zombie's XP (the original does not override {@code xpReward}). */
    public static final int XP = 5;

    // --------------------------------------------------------------- attributes

    /** {@code :22} — {@code maxHealth = 30.0}. */
    public static final double MAX_HEALTH = 30.0;

    /** {@code :23} — {@code attackDamage = 5.0}. (The melee body is empty at
     *  {@code :33-34}, so the attribute is what the 1.21.1 hit path reads for
     *  splash/knockback; the original ships both, pinned as-is.) */
    public static final double ATTACK_DAMAGE = 5.0;

    /** {@code :24} — {@code field_110186_bp} (the zombie reinforcement
     *  attribute) = 0.0: the inhabited zombie never calls reinforcements. */
    public static final double SPAWN_REINFORCEMENTS = 0.0;

    // ------------------------------------------------------------------- goals

    /**
     * {@code :29} — the original's own target goal at slot 1: {@code
     * EntityAIHurtByTarget(this, true)}. 1.7.10's {@code true} is
     * alert-same-type; 1.21.1's shape is {@code setAlertOthers()}. The vanilla
     * 1.21.1 zombie's slot 1 is the same goal with {@code setAlertOthers(
     * ZombifiedPiglin.class)} (1.7.10's vanilla zombie slot 1 alerts pigmen,
     * {@code EntityZombie:96}), so the shipped slot 1 keeps the vanilla pair:
     * the original's {@code true} is the alert-others flag the vanilla goal
     * already sets.
     */
    public static final int TARGET_SLOT_HURT = 1;

    /**
     * {@code :30} — the original's own target goal at slot 3: {@code
     * EntityAINearestAttackableTarget(this, EntityCultist.class, 0, true)} —
     * interval 0, always-update true, and 1.7.10's 4-arg constructor defaults
     * {@code mustSee=true}.
     *
     * <p><b>The slot-3 collision (BD-119).</b> The 1.7.10 vanilla zombie owns
     * slot 3 too ({@code EntityAINearestAttackableTarget(EntityVillager)}), and
     * the original's constructor runs AFTER the parent's, so on 1.7.10's
     * {@code TreeMap} of priority→goal the Cultist goal REPLACES the villager
     * goal and the zombie nets a target map of {1 hurt, 2 player, 3 cultist, 4
     * iron golem}. 1.21.1's {@code GoalSelector} keys on the goal, not the
     * slot, so a slot write cannot replace: the port ships the vanilla 1.21.1
     * set minus the goals 1.7.10's vanilla zombie does not have, plus the
     * Cultist goal at slot 3, and the villager goal is stripped (its class
     * predicate, {@link #stripsTargetGoal}). Shipped target set: {1 hurt, 2
     * player, 3 cultist, 4 iron golem, 5 turtle} — the original's map plus the
     * 1.21.1-only turtle goal, which the family keeps (vanilla-shape rule for
     * goals the original's parent had in 1.7.10 form and 1.21.1 grew).</p>
     */
    public static final int TARGET_SLOT_CULTIST = 3;
    public static final int TARGET_SLOT_PLAYER = 2;
    public static final int TARGET_SLOT_IRON_GOLEM = 4;
    public static final int TARGET_SLOT_TURTLE = 5;

    /** The vanilla 1.21.1 movement goals, at the original's parent's slots. */
    public static final int GOAL_SLOT_MELEE = 2;
    public static final int GOAL_SLOT_VILLAGE = 6;
    public static final int GOAL_SLOT_WANDER = 7;
    public static final int GOAL_SLOT_WATCH = 8;
    public static final int GOAL_SLOT_LOOK_IDLE = 8;
    public static final float MELEE_SPEED = 1.0F;
    public static final boolean MELEE_LONG_MEMORY = false;
    public static final float VILLAGE_SPEED = 1.0F;
    public static final int VILLAGE_REPATH = 4;
    public static final float WANDER_SPEED = 1.0F;
    public static final float WATCH_PLAYER_RANGE = 8.0F;

    /**
     * The goal the original's parent set has that 1.21.1's vanilla zombie grew:
     * the turtle-egg goal at slot 4 ({@code Zombie:118}). The 1.7.10 vanilla
     * zombie has no such goal, so the port strips it (the family's goal rule:
     * the original's set, not the platform's growth) — {@link
     * #stripsMovementGoal()}'s first arm.
     */
    public static boolean stripsMeleeGoal() {
        return true;
    }

    /** The census the probe asserts: 5 movement goals, 5 target goals. */
    public static final int SHIPPED_GOAL_COUNT = 5;
    public static final int SHIPPED_TARGET_COUNT = 5;

    // ------------------------------------------------------------- equipment

    /**
     * {@code :37} — the equipment roll factor: {@code HARD ? 0.9 : 0.6}. The
     * chest ({@code :39-41}) and leg ({@code :43-45}) plates appear on {@code
     * rand.nextFloat() <= factor}; the helmet ({@code :38}) is unconditional.
     */
    public static float equipmentChance(final boolean hard) {
        return hard ? EQUIPMENT_CHANCE_HARD : EQUIPMENT_CHANCE;
    }

    public static final float EQUIPMENT_CHANCE_HARD = 0.9F;
    public static final float EQUIPMENT_CHANCE = 0.6F;

    /** {@code :38}/{@code :40}/{@code :44} — the plate ids (the same ids the
     *  cultists wear; {@code not_started} in the ledger, so the equipment write
     *  is gated on the live registry, BD-68's shape). The 1.7.10 armour
     *  ordinals: 4 helmet, 3 chest, 2 legs. */
    public static final ResourceLocation HELM_PLATE =
            ResourceLocation.parse("thaumcraft:helmcultistplate");
    public static final ResourceLocation CHEST_PLATE =
            ResourceLocation.parse("thaumcraft:chestcultistplate");
    public static final ResourceLocation LEGS_PLATE =
            ResourceLocation.parse("thaumcraft:legscultistplate");

    /**
     * {@code :39}/{@code :43} — the roll is {@code nextFloat() <= chance}, so a
     *  roll exactly equal to the factor equips (the original's {@code <=}).
     */
    public static boolean equips(final float roll, final float chance) {
        return roll <= chance;
    }

    // ------------------------------------------------------------------ death

    /**
     * {@code :57-91} — {@code func_70609_aI} (the death update): the server
     * spawns an {@code EntityEldritchCrab} at the zombie's feet-at-eye-height,
     * copies rotation, sets the helm flag, adds it, then runs the vanilla XP
     * split loop, then the 20 explode particles, then {@code setDead()}.
     *
     * <p>The crab spawn has NO roll and no condition beyond the side check:
     * every death spawns exactly one crab. The XP loop's guard is the vanilla
     * mob-loot guard — {@code (recentlyHit > 0 || isPlayer()) && canDropLoot()
     * && doMobLoot} ({@code :63}).</p>
     */
    public static final int DEATH_PARTICLES = 20;
    public static final float DEATH_PARTICLE_JITTER = 0.02F;

    /** {@code :60} — the crab lands at {@code y + getEyeHeight()}. */
    public static double crabSpawnY(final double y, final double eyeHeight) {
        return y + eyeHeight;
    }

    /** {@code :61} — {@code crab.setHelm(true)}. */
    public static boolean crabSpawnsWithHelm() {
        return true;
    }

    /**
     * {@code :67-69} — the XP orb split (1.7.10's {@code EntityXPOrb.getXPSplit}):
     * the vanilla split table, so the port reuses 1.21.1's own
     * {@code ExperienceOrb#getXpSplitValue}? No — 1.21.1's split helper is
     * private; the port ships the original's table, pinned here.
     */
    public static int xpSplit(final int count, final net.minecraft.util.RandomSource random) {
        if (count <= 0) {
            return 0;
        }
        final int tier = splitTier(count, random.nextInt(3));
        return tier == 0 ? 1 : tier;
    }

    /**
     * 1.7.10's {@code EntityXPOrb.getXPSplit} table: a 0/1/2 roll picks 2, 4,
     * 8, doubling while {@code count} exceeds the candidate. The port ships the
     * table as the pure function the probe pins (the same math 1.21.1's private
     * {@code ExperienceOrb#assignExpValue} runs; kept explicit so the orb count
     * is measured, not inherited).
     */
    static int splitTier(final int count, final int roll) {
        int value = 2 << roll;
        int half = value << 1;
        while (count < half) {
            value = roll == 0 ? 1 : value >> 1;
            half = value << 1;
        }
        return Math.min(value, count);
    }

    // ------------------------------------------------------------------- spawn

    /** {@code :104-113} — the unit box expanded 32/16/32: any inhabited zombie
     *  inside refuses the spawn. */
    public static final double SPAWN_BOX_XZ_EXPAND = 32.0;
    public static final double SPAWN_BOX_Y_EXPAND = 16.0;

    // ------------------------------------------------------------------ sound

    /** {@code :96-98} — ambient {@code thaumcraft:crabtalk} (registered by the
     *  audio lane). {@code :100-102} — hurt {@code game.hostile.hurt}, which is
     *  1.21.1's vanilla hostile-fall/hurt default, so the hurt is not overridden. */
    public static final ResourceLocation SOUND_AMBIENT =
            ResourceLocation.parse("thaumcraft:crabtalk");

    /** {@code :50-52} — {@code getDropItem()} is {@code Item(0)} (air): the
     *  inhabited zombie drops no extra item. 1.21.1's zombie loot comes from the
     *  {@code zombie} loot table, which has no 1.7.10 twin for this mob; the
     *  port pins the id as air and ships no loot-table override (BD-118). */
    public static final ResourceLocation DROP_ITEM =
            ResourceLocation.parse("minecraft:air");
}
