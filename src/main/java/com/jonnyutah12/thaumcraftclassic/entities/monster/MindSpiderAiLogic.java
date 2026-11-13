package com.jonnyutah12.thaumcraftclassic.entities.monster;

/**
 * Every constant and rule of the Mind Spider, kept {@code Level}-free so the
 * unit test can pin them without a world.
 *
 * <p>Original: {@code thaumcraft.common.entities.monster.EntityMindSpider}, 115
 * lines (read whole, {@code reference/decompiled/thaumcraft/thaumcraft/common/
 * entities/monster/EntityMindSpider.java}), {@code extends EntitySpider}. Ledger
 * row {@code docs/content-mapping.csv} line 322 ({@code Thaumcraft:MindSpider} →
 * {@code thaumcraft:mindspider}); the line number is the current one on
 * {@code origin/main}, the ledger id is the contract.</p>
 *
 * <p>The card's scope line calls the behaviour "the jump/attack/poison
 * behaviour at {@code :72}". Read from the source, {@code :72-77} is the <b>tick
 * override</b> — {@code super.tick()} plus a server-only lifespan discard — and
 * the file contains no jump, attack or poison code of its own: the leap
 * ({@code LeapAtTargetGoal}), the melee attack ({@code SpiderAttackGoal} →
 * {@code attackEntityAsMob}) and the poison immunity all come from the
 * {@code EntitySpider} parent. What the class adds is a harmless mode, a viewer
 * name, a lifespan, an AI shutdown and a drop/loot shutdown. Every figure is
 * cited below, and the card's claim is corrected in the row's
 * {@code known_differences}.</p>
 */
public final class MindSpiderAiLogic {

    // ---- registration (ConfigEntities.java:105-106) ----
    /** {@code registerModEntity("MindSpider", EntityMindSpider.class,
     *  "MindSpider", 24, Thaumcraft.instance, 64, 3, true)}. */
    public static final int MOD_INDEX = 24;
    public static final int TRACK_RANGE_BLOCKS = 64;
    /** 1.21.1's {@code EntityType.Builder#clientTrackingRange} is measured in
     *  CHUNKS, not blocks: the original's 64 blocks is 4 chunks. Handing the
     *  builder the raw 64 would ask for a 1024-block tracking range. */
    public static final int TRACK_RANGE_CHUNKS = TRACK_RANGE_BLOCKS / 16;
    public static final int UPDATE_INTERVAL = 3;
    public static final boolean VELOCITY_UPDATES = true;

    // ---- geometry / xp ----
    /** {@code :18} — {@code func_70105_a(0.3F, 0.3F)} in the constructor. */
    public static final float WIDTH = 0.3F;
    public static final float HEIGHT = 0.3F;
    /** {@code :67-70} — the client {@code spiderScaleAmount()} is the same 0.3. */
    public static final float SCALE = 0.3F;
    /** {@code :19} — {@code field_70728_aV = 1} (the death xp value). */
    public static final int XP = 1;

    // ---- attributes (:26-30) ----
    /** {@code field_111267_a} = {@code maxHealth}, {@code field_111264_e} =
     *  {@code attackDamage} ({@code fields_1.7.10_mcp_stable_11.csv}) — both
     *  written to 1.0. The movement speed is NOT touched, so it stays the
     *  parent's. */
    public static final double MAX_HEALTH = 1.0;
    public static final double ATTACK_DAMAGE = 1.0;

    // ---- harmless mode ----
    /** {@code :14} — {@code lifeSpan = Integer.MAX_VALUE} (a normal spider
     *  never expires). */
    public static final int DEFAULT_LIFESPAN = Integer.MAX_VALUE;
    /** {@code :52} — {@code setHarmless(true)} arms a 1200-tick (60 s) fuse. */
    public static final int HARMLESS_LIFESPAN = 1200;
    /** {@code :34} — data watcher index 22, {@code new Byte((byte)0)}. */
    public static final int DATA_HARMLESS_INDEX = 22;
    /** {@code :35} — data watcher index 23, {@code new String("")}. */
    public static final int DATA_VIEWER_INDEX = 23;
    /** {@code :79-81} — the crosshair reach: harmless spiders cannot be picked. */
    public static final float PICK_RADIUS_HARMLESS = 0.0F;
    public static final float PICK_RADIUS_NORMAL = 0.1F;

    // ---- sound ----
    /** {@code :58-60} — {@code func_70647_i() = 0.7F}, the sound-volume hook
     *  (1.21.1's seam is {@code Mob#getSoundVolume()}). */
    public static final float SOUND_VOLUME = 0.7F;

    // ---- target lookup ----
    /** {@code :62-65} — {@code func_70782_k()}: the parent's target hook
     *  re-pointed at {@code world.getClosestPlayer(this, 12.0)}. */
    public static final double CLOSEST_PLAYER_RANGE = 12.0;

    // ---- NBT (:104-114) ----
    public static final String NBT_HARMLESS = "harmless";
    public static final String NBT_VIEWER = "viewer";

    /** {@code :22-24} — {@code func_70693_a}: a harmless spider grants no xp. */
    public static int experienceFor(final boolean harmless, final int parentValue) {
        return harmless ? 0 : parentValue;
    }

    /** {@code :74-76} — {@code !isRemote && ticksExisted > lifeSpan} → remove. */
    public static boolean expires(final int age, final int lifeSpan) {
        return age > lifeSpan;
    }

    /** {@code :98-102} — {@code func_70785_a} (attackEntityAsMob) runs only for
     *  a non-harmless spider. */
    public static boolean attacks(final boolean harmless) {
        return !harmless;
    }

    /**
     * The goal set, reconciled. The 1.7.10 vanilla {@code EntitySpider}
     * decompile is NOT vendored under {@code reference/}, so the parent's slots
     * are transcribed from 1.21.1 {@code Spider.java:54-65} minus the
     * post-1.7.10 features — the same rule this family used for the zombie pair
     * (c2, {@code FEATURE_MATRIX} row). Recorded per goal:
     *
     * <ul>
     *   <li>KEEP slot 1 {@code FloatGoal} — 1.7.10 {@code EntityAIFloat}.</li>
     *   <li>REMOVE slot 2 {@code AvoidEntityGoal(Armadillo, 6.0F, 1.0, 1.2)} —
     *       the armadillo is a 1.20.11 mob; the 1.7.10 spider has no such goal.</li>
     *   <li>KEEP slot 3 {@code LeapAtTargetGoal(0.4F)} — 1.7.10
     *       {@code EntityAILeapAtTarget}; this is the "jump" the card cites.</li>
     *   <li>KEEP slot 4 {@code SpiderAttackGoal} — 1.7.10
     *       {@code EntitySpider$EntityAIAttackOnCollide}, same slot; the melee
     *       hit is the "attack" the card cites.</li>
     *   <li>KEEP slot 5 {@code WaterAvoidingRandomStrollGoal(0.8)} — 1.7.10
     *       {@code EntityAIWander(0.8)}.</li>
     *   <li>KEEP slot 6 {@code LookAtPlayerGoal(Player, 8.0F)} and slot 6
     *       {@code RandomLookAroundGoal} — 1.7.10 slots 7/8 (the slot drift is
     *       recorded; both are {@code Goal.Flag.MOVE}-free so the order does not
     *       change which goals can run together).</li>
     *   <li>TARGET 1 {@code HurtByTargetGoal}, 2 {@code SpiderTargetGoal(Player)},
     *       3 {@code SpiderTargetGoal(IronGolem)} — 1.7.10 ordered the hurt goal
     *       LAST (slot 3) and had no villager target; the slot drift is recorded.
     *       The "poison" the card cites is the parent's bite effect
     *       ({@code SpiderAttackGoal} → {@code attackEntityAsMob}, which this
     *       class gates at {@code :98-102}), not a goal.</li>
     * </ul>
     *
     * <p>Every one of them is <b>inert in the original</b>: {@code :90-92}
     * ({@code func_145773_az() = true}) is the AI-off flag, and 1.21.1 gates the
     * whole goal engine the same way
     * ({@code Mob#isEffectiveAi() = super && !isNoAi()}, {@code Mob.java:1421}).
     * A probe asserts that.</p>
     */
    public static final int[] GOAL_SLOTS = {1, 3, 4, 5, 6, 6};
    public static final int[] TARGET_SLOTS = {1, 2, 3};

    private MindSpiderAiLogic() {
    }
}
