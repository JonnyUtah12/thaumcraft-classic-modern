package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

/**
 * The rules of {@code thaumcraft.common.entities.monster.boss.EntityCultistPortal}
 * (314 lines) — the summoner's stage machine — Level-free so every threshold and
 * roll is pinned by {@code src/test/java/.../entities/monster/boss/CultistPortalAiLogicTest.java}.
 *
 * <p>Citations are the 1.7.10 decompile
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityCultistPortal.java}.</p>
 */
public final class CultistPortalAiLogic {

    private CultistPortalAiLogic() {
    }

    /** {@code :40} — {@code super(level, 1.5F, 3.0F)}. */
    public static final float WIDTH = 1.5F;
    public static final float HEIGHT = 3.0F;

    /** {@code :38} fire immunity; {@code :39} experience 30; {@code :43} armour 5. */
    public static final boolean FIRE_IMMUNE = true;
    public static final int EXPERIENCE = 30;
    public static final int ARMOUR = 5;

    /** {@code :63-65} — maxHealth 500.0, attack 0.0, knockbackResistance 1.0. */
    public static final double MAX_HEALTH = 500.0;
    public static final double ATTACK_DAMAGE = 0.0;
    public static final double KNOCKBACK_RESISTANCE = 1.0;

    /** {@code :68} light 0.0; {@code :95} maxHeight 1.0; {@code :263} sound volume 0.75. */
    public static final float LIGHT_LEVEL = 0.0F;
    public static final float MAX_HEIGHT = 1.0F;
    public static final float SOUND_VOLUME = 0.75F;

    /** {@code :267} — {@code getExperiencePoints} 540 (the kill award, not the value). */
    public static final int EXPERIENCE_POINTS = 540;

    /** {@code :76} — {@code isPushable()} false; {@code :80} setCustomMotion no-op. */
    public static final boolean PUSHABLE = false;

    /** {@code :91} the client colour 15728880 (0xF0F0F0); {@code :86} render range 4096. */
    public static final int COLOUR = 15728880;
    public static final double RENDER_DISTANCE_SQ = 4096.0;

    // ---------------------------------------------------------------- stage machine (:99-211)

    /** {@code :32-33} — {@code stage = 0}, {@code stagecounter = 200}. */
    public static final int STAGE_INITIAL = 0;
    public static final int COUNTER_INITIAL = 200;
    /** {@code :243} / {@code :255} — stage 12 is the boss stage, and the machine keeps running. */
    public static final int STAGE_BOSS = 12;
    public static final int COUNTER_MIN = 0;

    /** {@code :103} — with no player within 48 blocks: {@code stagecounter = 30 + rand(30)}. */
    public static final int NO_PLAYER_RANGE = 48.0;
    public static final int NO_PLAYER_BASE = 30;
    public static final int NO_PLAYER_RANDOM = 30;

    public static int noPlayerCounter(final int roll30) {
        return NO_PLAYER_BASE + Math.floorMod(roll30, NO_PLAYER_RANDOM);                // :103
    }

    /**
     * {@code :106-108} — stages 0..4: {@code stagecounter = 15 + rand(10 - stage) - stage}.
     * For stage 4 that is {@code 15 + rand(6) - 4}, so the window closes with the stage.
     */
    public static int earlyStageCounter(final int stage, final int roll) {
        return 15 + Math.floorMod(roll, Math.max(1, 10 - stage)) - stage;               // :107
    }

    /**
     * {@code :110-113} — stages 5..11: {@code t = getTiming(); stagecounter = t + rand(5 + t/3)}.
     * {@code t} is 20 per cultist within {@link #SCAN_RADIUS} ({@code :213-218}), so a
     * full circle lengthens every cadence.
     */
    public static int midStageCounter(final int timing, final int roll) {
        return timing + Math.floorMod(roll, Math.max(1, 5 + timing / 3));               // :112
    }

    /** {@code :115-117} — stage 12: {@code stagecounter = 50 + getTiming() * 2 + rand(50)}. */
    public static int bossStageCounter(final int timing, final int roll50) {
        return 50 + timing * 2 + Math.floorMod(roll50, 50);                             // :116
    }

    /** {@code :213-218} — {@code getTiming() = 20 * (cultists within 32.0)}. */
    public static final double SCAN_RADIUS = 32.0;
    public static final int TIMING_PER_CULTIST = 20;

    public static int timingFor(final int cultists) {
        return TIMING_PER_CULTIST * cultists;                                           // :215-217
    }

    /** {@code :101} / {@code :120} — the counter decrements while it runs; the stage heals 1.0F. */
    public static boolean healsForStage(final int stage) {
        return stage < STAGE_BOSS;                                                      // :122-125
    }
    public static final float HEAL_PER_TICK = 1.0F;                                     // :124

    /** {@code :127-135} — at {@code counter == 160 && stage == 0}: the four banners at ±6. */
    public static final int BANNER_TICK = 160;
    public static final int BANNER_OFFSET = 6;
    /** The four {@code TileBanner.setFacing} values, in the original's direction order. */
    public static final int[] BANNER_FACINGS = {8, 0, 12, 4};                           // :130-133

    public static boolean isBannerTick(final int counter, final int stage) {
        return counter == BANNER_TICK && stage == STAGE_INITIAL;                        // :127
    }

    /**
     * {@code :137-152} — the crate window: {@code 20 < counter < 150 && stage == 0 &&
     * counter % 13 == 0}, at {@code x + rand(5) - rand(5)} on both axes, a solid cell only.
     */
    public static final int CRATE_WINDOW_LOW = 20;
    public static final int CRATE_WINDOW_HIGH = 150;
    public static final int CRATE_PERIOD = 13;
    public static final int CRATE_SPREAD = 5;

    public static boolean isCrateTick(final int counter, final int stage) {
        return counter > CRATE_WINDOW_LOW && counter < CRATE_WINDOW_HIGH
                && stage == STAGE_INITIAL && counter % CRATE_PERIOD == 0;               // :137-139
    }

    /** {@code :142-144} — {@code meta = rand < 0.05 ? 2 : (rand < 0.2 ? 1 : 0)}. */
    public static int crateMeta(final double roll) {
        if (roll < 0.05) {
            return 2;
        }
        return roll < 0.2 ? 1 : 0;                                                      // :143
    }

    // ---------------------------------------------------------------- summoning (:220-255)

    /** {@code :222} — knight when {@code rand() > 0.33}, cleric otherwise. */
    public static final double MINION_KNIGHT_ROLL = 0.33;

    public static boolean spawnsKnight(final double roll) {
        return roll > MINION_KNIGHT_ROLL;                                               // :222
    }

    /** {@code :227-230} — the spawn cell: {@code x + rand - rand}, {@code y + 0.25}, {@code z + rand - rand}. */
    public static final double MINION_Y_OFFSET = 0.25;
    /** {@code :232} — the minion's home is the portal's own cell with radius 32. */
    public static final int MINION_HOME_RADIUS = 32;

    /** {@code :239-241} — above stage 12 the portal hurts itself for {@code 5 + rand(5)}. */
    public static final int SELF_DAMAGE_BASE = 5;
    public static final int SELF_DAMAGE_RANDOM = 5;

    public static int selfDamage(final int stage, final int roll5) {
        return stage > STAGE_BOSS ? SELF_DAMAGE_BASE + Math.floorMod(roll5, SELF_DAMAGE_RANDOM) : 0;
    }

    /** {@code :243-255} — the boss stage spawns ONE CultistLeader (cap: the original has none). */
    public static boolean spawnsBoss(final int stage) {
        return stage == STAGE_BOSS;                                                     // :115, :243
    }

    // ---------------------------------------------------------------- contact / death (:257-313)

    /** {@code :257-261} — a player within squared distance 3.0 takes 8.0F, with the {@code zap} sound. */
    public static final double CONTACT_DISTANCE_SQ = 3.0;
    public static final float CONTACT_DAMAGE = 8.0F;

    /** {@code :307-313} — death: explosion, strength 2.0F, no block destruction, no fire. */
    public static final float DEATH_EXPLOSION = 2.0F;

    /** {@code :291-299} — the client's status 16 sets the pulse to 10 and plays the living sound. */
    public static final int PULSE_TICKS = 10;
    public static final int STATUS_PULSE = 16;

    /** {@code :283} — the ordinary drop is nothing; {@code :287-289} the custom drop is the eldritch object. */
    public static final int DROP_ITEM_ID = 0;
}
