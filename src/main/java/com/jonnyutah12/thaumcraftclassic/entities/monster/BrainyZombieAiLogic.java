package com.jonnyutah12.thaumcraftclassic.entities.monster;

/**
 * Level-free rules for the Angry / Furious Zombie pair —
 * {@code thaumcraft:brainyzombie} (content-mapping {@code Thaumcraft:BrainyZombie},
 * mod_index 19) and {@code thaumcraft:giantbrainyzombie}
 * ({@code Thaumcraft:GiantBrainyZombie}, mod_index 20).
 *
 * <p>Originals: {@code thaumcraft.common.entities.monster.EntityBrainyZombie}
 * (54 lines, {@code extends EntityZombie}) and
 * {@code thaumcraft.common.entities.monster.EntityGiantBrainyZombie} (90 lines,
 * {@code extends EntityBrainyZombie}). Every constant and rule below quotes the
 * decompiled line it was read from and is pinned world-free by
 * {@code BrainyZombieAiLogicTest}; the two entity classes only supply the
 * world.</p>
 *
 * <p>What the pair actually is, read from the source: the angry zombie is a
 * vanilla zombie with harder attributes (+3 armour, capped at 20), one extra
 * target goal and a rewritten death-drop body. The furious zombie adds an
 * <b>anger</b> data value that grows by 0.1 per hit up to 2.0, decays by 0.002
 * per tick back toward 1.0, rewrites its own attack damage every tick from that
 * value, scales its hitbox from it (1.2 + anger), doubles its drop rolls and
 * doubles each drop stack, and replaces the vanilla {@code dropAllLoot} body
 * with a 4-way drop of its own. There is <b>no</b> warp, teleport, party,
 * leader, summon or raid behaviour in either class — the pair's whole surface is
 * attributes, the anger value, the goal pair and the drops.</p>
 *
 * <p>1.7.10 symbol resolutions used here, from
 * {@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv}:</p>
 * <ul>
 *   <li>{@code field_111267_a} = {@code maxHealth}, {@code field_111264_e} =
 *       {@code attackDamage} — the two attributes the pair writes.</li>
 *   <li>{@code field_110186_bp} is NOT in the field table (MCP stable_11 left it
 *       unmapped, which is why the decompile shows the raw SRG name). It is the
 *       reinforcement attribute the 1.7.10 zombie parent declares: 1.21.1's
 *       counterpart is {@code Attributes.SPAWN_REINFORCEMENTS_CHANCE}, registered
 *       as {@code "zombie.spawn_reinforcements"} with default 0.0 and range
 *       0.0..1.0 ({@code Attributes.java:89-91}), and it is the only attribute
 *       the pair writes to 0.0.</li>
 *   <li>{@code field_70129_M} = {@code yOffset}, {@code field_70130_N} =
 *       {@code width}, {@code field_70131_O} = {@code height} — the giant's ctor
 *       multiplies width by the anger scale and resizes from width/height.</li>
 *   <li>{@code field_70180_af} = {@code dataWatcher} (the anger value lives at
 *       index 20), {@code field_70146_Z} = {@code rand} (the entity's own RNG),
 *       {@code field_70170_p.field_73012_v} = the WORLD's RNG — the pair uses
 *       both, and the split is preserved (the drops roll the world RNG, the
 *       furious zombie's loot table rolls its own).</li>
 *   <li>{@code field_70714_bg} = {@code tasks} (goal selector),
 *       {@code field_70715_bh} = {@code targetTasks} (target selector).</li>
 *   <li>{@code field_70728_aV} = {@code experienceValue} (1.21.1's
 *       {@code Mob#xpReward}).</li>
 *   <li>{@code field_151078_bh} = {@code rotten_flesh}, {@code field_151172_bF}
 *       = {@code carrot}, {@code field_151174_bG} = {@code potato}.</li>
 * </ul>
 */
public final class BrainyZombieAiLogic {

    private BrainyZombieAiLogic() {
    }

    // ---------------------------------------------------- registration args
    // ConfigEntities.java:95 (BrainyZombie) and :97 (GiantBrainyZombie):
    // registerModEntity(..., 64, 3, true) — tracking range 64 BLOCKS, update
    // interval 3, velocity updates on. 1.21.1's clientTrackingRange is in
    // CHUNKS, so 64 blocks is 4.
    public static final int TRACK_RANGE_BLOCKS = 64;
    public static final int TRACK_RANGE_CHUNKS = 4;
    public static final int UPDATE_INTERVAL = 3;
    public static final boolean TRACK_VELOCITY = true;

    // --------------------------------------------------------- the angry one
    // EntityBrainyZombie never touches its size, so the box is the 1.7.10 zombie
    // parent's — 1.21.1 builds it at EntityType.java:770 (.sized(0.6F, 1.95F)).
    public static final float BRAINY_WIDTH = 0.6F;
    public static final float BRAINY_HEIGHT = 1.95F;
    /** {@code EntityBrainyZombie.java:16} — {@code maxHealth} 25.0. */
    public static final double BRAINY_MAX_HEALTH = 25.0;
    /** {@code :17} — {@code attackDamage} 5.0. */
    public static final double BRAINY_ATTACK_DAMAGE = 5.0;
    /** {@code :18} — the reinforcement attribute at 0.0: an angry zombie never
     *  calls for reinforcements (the vanilla zombie's is 0.0 by default too,
     *  {@code Attributes.java:89-91}, and 1.21.1 only consumes it on HARD). */
    public static final double BRAINY_REINFORCEMENTS = 0.0;

    /** {@code :35} — {@code super.getTotalArmorValue() + 3}. 1.21.1 has no
     *  armour-total hook: worn armour rides on the {@code ARMOR} attribute, so
     *  the original's +3 rides as the attribute base, on the vanilla zombie's
     *  own base of 2.0 ({@code Zombie.java:74 .add(Attributes.ARMOR, 2.0)}). */
    public static final int ARMOR_BONUS = 3;
    /** {@code :36-38} — the total is capped at 20. */
    public static final int ARMOR_CAP = 20;
    /** The 1.21.1 vanilla zombie's armour base — {@code Zombie.java:74}. */
    public static final double VANILLA_ZOMBIE_ARMOR = 2.0;

    /** {@code :34-41} — the armour override. */
    public static int armorValue(final int inheritedArmor) {
        final int total = inheritedArmor + ARMOR_BONUS;      // :35
        return total > ARMOR_CAP ? ARMOR_CAP : total;        // :36-38
    }

    /** The port's armour base: the vanilla zombie base plus the original's +3,
     *  through the same cap. */
    public static double armorBase() {
        return armorValue((int) VANILLA_ZOMBIE_ARMOR);       // :35 with :74 → 5
    }

    // The pair's goal pair. field_70715_bh = targetTasks, field_70714_bg = tasks.
    /** {@code EntityBrainyZombie.java:23} — target slot 1,
     *  {@code EntityAIHurtByTarget(this, false)}: the 1.7.10 second argument is
     *  the "not passive" flag, so it counter-attacks and alerts nobody — which
     *  is 1.21.1's plain {@code HurtByTargetGoal(this)} with no
     *  {@code setAlertOthers} list (the 1.21.1 parent's variant alerts
     *  ZombifiedPiglins, {@code Zombie.java:113}). */
    public static final int BRAINY_HURT_TARGET_SLOT = 1;

    // Death drops, angry zombie: EntityBrainyZombie.java:43-53.
    /** {@code :44} — {@code for (int a = 0; a < 3; a++)}: 3 coin flips. */
    public static final int BRAINY_FLESH_ROLLS = 3;
    /** {@code :46} — each true flip drops 1 rotten flesh
     *  ({@code func_145779_a(Items.field_151078_bh, 1)} = dropItem). */
    public static final int BRAINY_FLESH_STACK = 1;
    /** {@code :50} — the brain gate: {@code world.rand.nextInt(10) - looting <= 4}. */
    public static final int BRAIN_ROLL_MODULO = 10;
    public static final int BRAIN_ROLL_THRESHOLD = 4;
    /** {@code :51} — {@code func_70099_a(new ItemStack(itemZombieBrain), 1.5F)}:
     *  1.7.10's {@code entityDropItem(ItemStack, float)} second argument is the
     *  <b>Y offset above the feet</b> (it builds
     *  {@code new ItemEntity(world, posX, posY + offsetY, posZ, stack)}), and
     *  1.21.1's {@code spawnAtLocation(ItemStack, float)} is the same shape.
     *  <b>Trap:</b> 1.21.1's {@code spawnAtLocation(ItemLike, int)} overload is
     *  {@code spawnAtLocation(new ItemStack(item), (float)count)} —
     *  {@code Entity.java:1853-1855} — so its {@code int} is an OFFSET, not a
     *  stack size. Every counted drop in the pair therefore goes through the
     *  {@code (ItemStack, float)} overload with an explicit count; the measured
     *  shape of the trap was the furious zombie leaving 1 flesh per flip
     *  (28 over 5 kills) where the original leaves 2 (60 expected). */
    public static final float BRAINY_BRAIN_OFFSET_Y = 1.5F;

    /** {@code :44-48} — one coin flip drops one rotten flesh. The flip is the
     *  WORLD's RNG ({@code field_70170_p.field_73012_v}), as in the original. */
    public static boolean dropsFlesh(final boolean worldCoinFlip) {
        return worldCoinFlip;                                // :45
    }

    /** {@code :50} — the looting-sensitive brain gate. Looting 0 passes on
     *  rolls 0..4; looting 5 passes on every roll. */
    public static boolean dropsBrain(final int roll10, final int looting) {
        return roll10 - looting <= BRAIN_ROLL_THRESHOLD;     // :50
    }

    // ------------------------------------------------------- the furious one
    /** {@code EntityGiantBrainyZombie.java:14} — {@code experienceValue} 15
     *  (1.21.1's {@code Mob#xpReward}; {@code Monster} sets 5). */
    public static final int FURIOUS_XP = 15;
    /** {@code :24} — the giant sizes itself from its OWN base, 0.6 x 1.8, not
     *  from the angry zombie's box. */
    public static final float FURIOUS_BASE_WIDTH = 0.6F;
    public static final float FURIOUS_BASE_HEIGHT = 1.8F;
    /** {@code :15-16} and {@code :24} — the size factor is {@code 1.2 + anger}.
     *  1.21.1 carries an entity's size factor as the {@code generic.scale}
     *  attribute: {@code LivingEntity#getScale()} reads it (:528-530),
     *  {@code getDimensions} scales the box by it (:3250) and {@code aiStep}
     *  refreshes the box when it changes (:2440-2443). The original's factor is
     *  exactly that number, so the port ships it as the attribute base. */
    public static final float ANGER_SCALE_BIAS = 1.2F;

    /** {@code :15-16}, {@code :24} — the anger-driven size factor. */
    public static float scaleFor(final float anger) {
        return ANGER_SCALE_BIAS + anger;
    }

    /** {@code :32} — the data value starts at 1.0 ({@code dataWatcher} index 20
     *  defined with {@code new Float(1.0F)}). */
    public static final float ANGER_START = 1.0F;
    /** {@code :22} — the decay only runs while anger is strictly above 1.0. */
    public static final float ANGER_FLOOR = 1.0F;
    /** {@code :23} — 0.002 per tick. */
    public static final float ANGER_DECAY_PER_TICK = 0.002F;
    /** {@code :45} — 0.1 per attack, capped at 2.0. */
    public static final float ANGER_PER_HIT = 0.1F;
    public static final float ANGER_CAP = 2.0F;
    /** {@code :27} — every living-update tick rewrites attack damage to
     *  {@code 7.0 + (anger - 1.0) * 5.0}. */
    public static final double FURIOUS_ATTACK_BASE = 7.0;
    public static final double FURIOUS_DAMAGE_PER_ANGER = 5.0;
    /** {@code :52-53} — health 60.0, attack 7.0. */
    public static final double FURIOUS_MAX_HEALTH = 60.0;
    /** {@code :17} — {@code tasks.addTask(2, new EntityAILeapAtTarget(this, 0.4F))}. */
    public static final int FURIOUS_LEAP_SLOT = 2;
    public static final float FURIOUS_LEAP_CHANCE = 0.4F;

    /** {@code :22-23} — the per-tick decay, with the original's strict
     *  comparison: a value of 1.0005 does drop below 1.0, because the branch
     *  test happens before the subtraction. */
    public static float angerAfterTick(final float anger) {
        return anger > ANGER_FLOOR ? anger - ANGER_DECAY_PER_TICK : anger;
    }

    /** {@code :44-46} — every hit, applied BEFORE {@code super.attackEntityFrom}. */
    public static float angerAfterHit(final float anger) {
        return Math.min(ANGER_CAP, anger + ANGER_PER_HIT);   // :45
    }

    /** {@code :27} — the attack-damage rewrite (1.21.1: the
     *  {@code ATTACK_DAMAGE} attribute base). */
    public static double attackDamageFor(final float anger) {
        return FURIOUS_ATTACK_BASE + (anger - 1.0F) * FURIOUS_DAMAGE_PER_ANGER;
    }

    // Death drops, furious zombie: EntityGiantBrainyZombie.java:56-73.
    /** {@code :58} and {@code :64} — TWO loops of SIX coin flips each. */
    public static final int FURIOUS_DROP_LOOPS = 2;
    public static final int FURIOUS_ROLLS_PER_LOOP = 6;
    /** {@code :60}, {@code :66} — each true flip drops 2 rotten flesh, i.e. the
     *  angry zombie's roll count doubled AND its stack size doubled. */
    public static final int FURIOUS_FLESH_STACK = 2;
    /** {@code :70-71} — the same looting gate, the brain dropped at Y offset 2.0
     *  (see {@link #BRAINY_BRAIN_OFFSET_Y} for the overload trap). */
    public static final float FURIOUS_BRAIN_OFFSET_Y = 2.0F;

    /** {@code :75-89} — the giant REPLACES vanilla {@code dropAllLoot(int)}
     *  (the decompiled CALLERS in this file, :71 and :78/:87, show
     *  {@code func_70099_a} is {@code entityDropItem(ItemStack, float)}, so the
     *  {@code (int)} override is 1.7.10's {@code dropAllLoot(fortune)} — the
     *  same override {@code EntityPech:348}, {@code EntityCultist:60} and the
     *  bosses make). Its 4-way pick uses the ENTITY's RNG ({@code field_70146_Z}),
     *  not the world's. The cases are the original's, in order. */
    public static final int LOOT_PICK_MODULO = 4;
    public static final int LOOT_PICK_THAUMIUM = 0;
    public static final int LOOT_PICK_CARROT = 1;
    public static final int LOOT_PICK_POTATO = 2;
    public static final int LOOT_PICK_AMBER = 3;
    /** {@code :78} and {@code :87} — the two {@code itemResource} cases drop a
     *  stack of 1 at Y offset 2.0 ({@code func_70099_a(ItemStack, float)}); the
     *  meta is the original's damage value, which this port carries in the
     *  {@code thaumcraft:item_meta} component ({@link ItemResource#stack(int)}).
     *  Meta 2 = thaumium ingot, meta 6 = amber ({@code ItemResource.META_THAUMIUM_INGOT},
     *  {@code META_AMBER}). The carrot ({@code :81}) and potato ({@code :84})
     *  cases go through {@code func_145779_a(item, 1)} = {@code dropItem} with
     *  count 1 and Y offset 0.0, i.e. a different seam from the two
     *  {@code itemResource} cases, and the port keeps that split. */
    public static final int LOOT_THAUMIUM_META = 2;
    public static final int LOOT_AMBER_META = 6;
    public static final int LOOT_STACK_SIZE = 1;
    public static final float LOOT_THROW_OFFSET_Y = 2.0F;
    /** {@code :81}, {@code :84} — the carrot / potato cases: count 1, offset 0.0. */
    public static final int LOOT_VEGETABLE_COUNT = 1;
    public static final float LOOT_VEGETABLE_OFFSET_Y = 0.0F;
    /** {@code :60}, {@code :66} — the flesh drops go through {@code dropItem}
     *  (count, Y offset 0.0), never the {@code (ItemStack, float)} seam. */
    public static final float FLESH_DROP_OFFSET_Y = 0.0F;

    /** {@code :76-88} — which of the four drops a roll picks. */
    public static int lootPick(final int roll4) {
        return roll4;                                        // :76 switch index
    }

    // ------------------------------------------------------------- save keys
    /** The persisted anger key. The original persists the data value under
     *  1.7.10's watcher naming ({@code DATA_20}, index 20 at {@code :30-32});
     *  this port writes a readable key because there are no 1.7.10 angry-zombie
     *  saves in this game to read. */
    public static final String ANGER_TAG = "Anger";
    /** {@code :30-32} — the original's watcher index, kept as a comment-visible
     *  fact for anyone diffing the two worlds. */
    public static final int ANGER_WATCHER_INDEX = 20;
}
