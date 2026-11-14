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
     * alert-same-type, and 1.21.1's shape for that is the EMPTY-varargs {@code
     * setAlertOthers()}: the goal's {@code alertOthers} reinforces only the
     * mob's own class, skipping every foreign class. 1.21.1's vanilla zombie
     * adds {@code ZombifiedPiglin.class} to that arm ({@code Zombie.java:113}),
     * a growth with no 1.7.10 counterpart (1.7.10's zombie alerts only
     * zombies), so the shipped slot 1 is {@code HurtByTargetGoal(this)
     * .setAlertOthers()} with no piglin.
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
     * goal and the iron golem goal that had overwritten it, so the zombie nets
     * a target map of {1 hurt, 2 player, 3 CULTIST}. 1.21.1's {@code
     * GoalSelector} keys on the goal, not the slot, so a slot write cannot
     * evict: the port writes the original's net set and STRIPS the goals 1.7.10
     * evicted (villager, iron golem) plus the 1.21.1-only turtle goal at slot 5
     * (turtles arrived in 1.8) - the family's goal rule is the original's set,
     * not the platform's growth. Shipped target set: {1 hurt, 2 player, 3
     * cultist}, asserted by the probe's goal census.</p>
     */
    public static final int TARGET_SLOT_CULTIST = 3;
    public static final int TARGET_SLOT_PLAYER = 2;

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

    /** The census the probe asserts: 5 movement goals, 3 target goals. */
    public static final int SHIPPED_GOAL_COUNT = 5;
    public static final int SHIPPED_TARGET_COUNT = 3;

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
     * {@code :63} - the drop guard, verbatim: {@code (this.field_70718_bc > 0
     * || this.func_70684_aJ()) && this.func_146066_aG() && this.world
     * .func_82736_K().func_82766_b("doMobLoot")}. {@code field_70718_bc} is
     * "Set to 60 when hit by the player or the player's wolf, then decrements.
     * Used to determine whether the entity should drop items on death", whose
     * 1.21.1 counterpart is {@code lastHurtByPlayerTime > 0}; {@code
     * func_70684_aJ()} (isPlayer) is inert for a zombie and shipped as the
     * parameter; {@code func_146066_aG()} (canDropLoot) is 1.7.10's mobGriefing
     * gamerule read, and 1.21.1 has no {@code Mob#canDropLoot} to ride, so the
     * port reads {@code GameRules.RULE_MOBGRIEFING} at the call site. A kill by
     * a non-player throws nothing, exactly as on 1.7.10.
     */
    public static boolean dropsExperience(final boolean recentlyHitByPlayer,
            final boolean isPlayer, final boolean canDropLoot, final boolean doMobLoot) {
        return (recentlyHitByPlayer || isPlayer) && canDropLoot && doMobLoot;
    }

    /**
     * {@code :64-70} - the loop's value sequence: {@code int i =
     * getExperiencePoints(attackingPlayer); while (i > 0) { int j =
     * EntityXPOrb.getXPSplit(i); i -= j; world.spawnEntityInWorld(new
     * EntityXPOrb(this, x, y, z, j)); }}. The split is 1.7.10's static {@code
     * EntityXPOrb.func_70527_a} (getOrbValue / roundToOrbSize), name-mapped
     * onto 1.21.1's public static {@code ExperienceOrb.getExperienceValue},
     * whose table the probe pins: 2477, 1237, 617, 307, 149, 73, 37, 17, 7, 3,
     * 1. The loop is deterministic - 1.7.10's split draws NO random - so the
     * orb count is measured, not inherited, and a 5-point reward (the plain
     * zombie's) lands as orbs of 3 then 2 (BD-122).
     */
    public static java.util.List<Integer> experienceOrbValues(final int total) {
        final java.util.List<Integer> values = new java.util.ArrayList<>();
        int remaining = total;
        while (remaining > 0) {
            final int value = net.minecraft.world.entity.ExperienceOrb
                    .getExperienceValue(remaining);                                     // :67
            remaining -= value;
            values.add(value);
        }
        return values;
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
