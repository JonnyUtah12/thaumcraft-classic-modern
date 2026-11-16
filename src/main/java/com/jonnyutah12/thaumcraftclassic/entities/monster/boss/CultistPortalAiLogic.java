package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import net.minecraft.resources.ResourceLocation;

/**
 * Level-free rule and figure surface for the Crimson Portal summoner
 * (content-mapping.csv {@code thaumcraft:cultistportal}, original
 * {@code thaumcraft.common.entities.monster.boss.EntityCultistPortal}
 * {@code :1-313}).
 *
 * <p>The portal is the boss band's <b>summoner</b>, not a melee boss. Its whole
 * behaviour is the stage machine of {@code :99-201}: a countdown that, when it hits
 * zero, fires a pulse at the client, spawns a wave out of the cultists nearby and
 * raises the stage; and on the way down, at fixed countdown values, walls off the
 * room with four banners and seeds it with loot crates. It installs <b>no goal
 * selectors</b> — {@code :47-49} ({@code entityInit}) writes no data watcher,
 * {@code :83-85} ({@code func_70626_be} = updateEntityActionState) is an EMPTY
 * override, and 1.7.10's {@code EntityMob} constructor registers goals the reference
 * then never ticks, so the port ships an empty goal selector (the Pech card's
 * finding: {@code PechAiLogic:317-324}). It has no attack goal, it never moves
 * ({@code :80-82} {@code func_70091_d} ({@code moveEntity}) is an empty override)
 * and it makes no footsteps ({@code :304-305} {@code func_70069_a(float)}
 * ({@code playStepSound}) is empty). It therefore does NOT extend the landed
 * {@link EntityThaumcraftBoss} base — that base's goal graph, aggro bookkeeping,
 * enrage and shared 1.7.10 methods are not this mob's (D-137).</p>
 *
 * <p>Every figure and verdict below is pinned line-by-line from the reference. The
 * rules are {@code Level}-free: a GameTest feeds its own roll values into
 * {@link #shortCounterFor(int, int)}, {@link #timedCounterFor(int, int)},
 * {@link #bossCounterFor(int, int)}, {@link #noPlayerCounterFor(int)} and
 * {@link #crateMetaFor(float)} and asserts the countdown and the branch, so no
 * probe depends on {@code Entity#random}. {@code Entity#random} is an unseeded
 * {@code RandomSource.create()} ({@code Entity.java:194}) and the crate roll is
 * drawn from the <b>level's</b> random, not the entity's ({@code :189}
 * {@code world.rand}), so a draw taken through the world is run entropy and is
 * never a probe oracle.</p>
 */
public final class CultistPortalAiLogic {

    private CultistPortalAiLogic() {
    }

    // ==================================================================== shape
    // The constructor figures (:36-40). 1.21.1 has no setSize, so they ride the
    // EntityType builder (docs/deviations/entity-builders-and-the-boss-band.md
    // D-137, the shape family of D-031).

    /** {@code :40} — {@code func_70105_a(1.5F, 3.0F)}: the widest entity in the band. */
    public static final float WIDTH = 1.5F;
    /** {@code :40} — the box height; also the loot-drop lift ({@code :288}). */
    public static final float HEIGHT = 3.0F;
    /** {@code :68-70} — {@code func_70053_R() = 0.0F} (getEyeHeight): this mob has no eyes. */
    public static final float EYE_HEIGHT = 0.0F;
    /** {@code ConfigEntities.java:117} — tracking range 64 blocks. */
    public static final int TRACK_RANGE_BLOCKS = 64;
    /** {@code ConfigEntities.java:117} — 64 blocks is 4 chunks in 1.21.1's unit. */
    public static final int TRACK_RANGE_CHUNKS = 4;
    /** {@code ConfigEntities.java:117} — update interval 20 ticks. */
    public static final int UPDATE_INTERVAL = 20;
    /** {@code ConfigEntities.java:117} — the {@code false} arg, the family's velocity-updates carrier. */
    public static final boolean VELOCITY_UPDATES = false;
    /** {@code :38} — {@code field_70178_ae = true} (isImmuneToFire) → {@code Builder#fireImmune}. */
    public static final boolean FIRE_IMMUNE = true;

    // ============================================================ attribute body
    // Pinned by THIS card. The c5-1 base class javadoc named these "c5-2/c5-3's to
    // pin"; this is c5-2, and the figures are the reference's own.

    /** {@code :39} — {@code field_70728_aV = 30} experience on death. */
    public static final int XP_REWARD = 30;
    /**
     * {@code :43-45} — {@code func_70658_aO() = 5} (getTotalArmorValue). 1.21.1
     * moved armour to the {@code minecraft:armor} attribute, the Pech card's
     * resolution of its {@code :507-514} pair ({@code EntityPech:213-216}).
     */
    public static final double ARMOR = 5.0;
    /** {@code :63} — {@code field_111267_a} (max health) {@code = 500.0}. */
    public static final double MAX_HEALTH = 500.0;
    /** {@code :64} — {@code field_111264_e} (attack damage) {@code = 0.0}: the summoner never melee-swings. */
    public static final double ATTACK_DAMAGE = 0.0;
    /** {@code :65} — {@code field_111266_c} (knockback resistance) {@code = 1.0}: the full amount. */
    public static final double KNOCKBACK_RESISTANCE = 1.0;
    /**
     * {@code :267-269} — {@code func_70627_aG() = 540} (getMaxAir): the cleric
     * lane's shape, {@code getAirSupply} cap + {@code getMaxAirSupply} override
     * ({@code EntityCultistCleric:357-369}).
     */
    public static final int MAX_AIR = 540;
    /** {@code :263-265} — {@code func_70599_aP() = 0.75F} (getSoundVolume). */
    public static final float SOUND_VOLUME = 0.75F;

    // =========================================================== client-side pins
    // Renderer-side methods with no 1.21.1 seam in this card (TC-M7-03 owns the
    // renderer); pinned as data per the firebat card's precedent for this family
    // ({@code FirebatAiLogic:39-41}, {@code :458-461}).

    /**
     * {@code :91-93} — {@code func_70070_b(float) = 15728880}
     * ({@code getBrightnessForRender}, {@code @SideOnly(CLIENT)}): the portal
     * renders at full bright, so it is visible inside the boss room.
     */
    public static final int BRIGHTNESS_FOR_RENDER = 15728880;
    /** {@code :95-97} — {@code func_70013_c(float) = 1.0F} (getBrightness): the same full-bright rule. */
    public static final float BRIGHTNESS = 1.0F;
    /** {@code :87-89} — {@code func_70112_a(double) = distance < 4096.0} ({@code isInRangeToRenderDist}): 4x vanilla render range. */
    public static final double RENDER_RANGE = 4096.0;

    // ================================================================= collision

    /** {@code :72-74} — {@code func_70067_L() = true} ({@code canBeCollidedWith}): the portal is solid to a player. */
    public static final boolean CAN_BE_COLLIDED_WITH = true;
    /** {@code :76-78} — {@code func_70104_M() = false} ({@code isPushable}). */
    public static final boolean PUSHABLE = false;
    /** {@code :80-82} — {@code func_70091_d(dx, dy, dz)} is an EMPTY override: the portal never moves. */
    public static final boolean NEVER_MOVES = true;
    /** {@code :83-85} — {@code func_70626_be()} is an EMPTY override: no goal ticking, no vanilla action state. */
    public static final boolean CUSTOM_AI_STEP_EMPTY = true;
    /**
     * {@code :301-302} — {@code func_70690_d(PotionEffect)} (onPotionEffectApplied) is
     * an EMPTY override: the statue draws NO per-effect particles. A visual hook, not
     * an immunity — the portal's only immunity is fire ({@code :38}). 21.1.259 has no
     * counterpart of the hook (the effect write path is {@code final}, the particle
     * draw is not overridable), so this is pinned as data and the port ships no
     * override (D-137).
     */
    public static final boolean DRAWS_EFFECT_PARTICLES = false;
    /** {@code :304-305} — {@code func_70069_a(float)} is an EMPTY override: no footsteps. */
    public static final boolean PLAYS_FOOTSTEPS = false;
    /**
     * {@code :283-285} — {@code func_146068_u() = Item.func_150899_d(0)}: the
     * pickup item is air (id 0). 1.21.1 has no entity pickup-item getter, so the
     * override has no seam and is pinned as data (the taint-spore card's precedent
     * for {@code func_146068_u}).
     */
    public static final int PICKUP_ITEM_ID = 0;

    // ================================================================ the machine
    // :32-34 the fields; :99-201 the tick. Countdown first, then the stage firing.

    /** {@code :32} — {@code int stage = 0}. */
    public static final int STAGE_INITIAL = 0;
    /** {@code :33} — {@code int stagecounter = 200}. */
    public static final int COUNTER_INITIAL = 200;
    /** {@code :34} — {@code public int pulse = 0} (idle). */
    public static final int PULSE_INITIAL = 0;
    /** {@code :294} — the pulse event sets it to 10. */
    public static final int PULSE_TICKS = 10;
    /**
     * {@code :104}, {@code :133}, {@code :188} — {@code setEntityState(this, (byte)16)}:
     * the portal's own entity status, the same byte the cleric lane uses for its
     * banner ({@code EntityCultistCleric:318}) and the taint spore pins
     * ({@code EntityTaintSpore:169-171}).
     */
    public static final byte STATUS_PULSE = 16;
    /** {@code :103} — the stage machine fires only when {@code getClosestPlayer(this, 48.0)} is non-null. */
    public static final double PLAYER_SCAN = 48.0;
    /** {@code :128} — with no player the countdown re-arms to {@code 30 + nextInt(30)}. */
    public static final int NO_PLAYER_COUNTER_BASE = 30;
    /** {@code :128} — the no-player re-arm roll. */
    public static final int NO_PLAYER_COUNTER_ROLL = 30;

    /** {@code :110} — stages 0-4 re-arm on {@code 15 + nextInt(10 - stage) - stage}. */
    public static final int SHORT_COUNTER_BASE = 15;
    /** {@code :110} — the short counter's spread before the stage is subtracted. */
    public static final int SHORT_COUNTER_SPREAD = 10;
    /** {@code :106-112} — the short-counter arm covers stages 0 through 4. */
    public static final int SHORT_COUNTER_STAGE_MAX = 4;
    /** {@code :115-117} — stages 5-11 (and the {@code default} arm) re-arm on {@code t + nextInt(5 + t/3)}. */
    public static final int TIMED_COUNTER_BONUS = 5;
    /** {@code :116} — the timing divisor of the timed arm is 3. */
    public static final int TIMED_COUNTER_DIVISOR = 3;
    /** {@code :121} — stage 12 re-arm base. */
    public static final int BOSS_COUNTER_BASE = 50;
    /** {@code :121} — stage 12 adds twice the timing. */
    public static final int BOSS_COUNTER_TIMING_FACTOR = 2;
    /** {@code :121} — stage 12's roll spread. */
    public static final int BOSS_COUNTER_ROLL = 50;
    /** {@code :120} — stage 12 is the boss arm ({@code spawnBoss}, one Crimson Praetor). */
    public static final int STAGE_BOSS = 12;
    /**
     * {@code :125} — after every firing arm the stage simply increments: there is NO
     * wrap and NO cap. Stage 13 falls into the {@code default} arm of {@code :113}
     * (the timed counter plus {@code spawnMinions}), and {@code :237} starts
     * damaging the portal itself once the stage is past 12.
     */
    public static final boolean STAGE_HAS_NO_WRAP = true;

    /** {@code :132} — the banner wall fires on the exact countdown value 160 … */
    public static final int BANNER_COUNTER = 160;
    /** {@code :132} — … and only in stage 0, i.e. on the first cycle only. */
    public static final int BANNER_STAGE = 0;
    /** {@code :135} — the banner loop walks {@code ForgeDirection} 2,3,4,5 (north, south, west, east). */
    public static final int BANNER_DIRECTION_FROM = 2;
    /** {@code :135} — the loop's exclusive upper bound. */
    public static final int BANNER_DIRECTION_TO = 6;
    /** {@code :138, :141} — the banners sit 6 blocks out, {@code x - offsetX*6} / {@code z + offsetZ*6}. */
    public static final int BANNER_OFFSET = 6;
    /** {@code :142-143} — every banner is {@code blockWoodenDevice} meta 8 (the hanging banner variant). */
    public static final int BANNER_VARIANT = 8;
    /** {@code :157-165} — {@code TileBanner#setFacing} per direction index: 2→8, 3→0, 4→12, 5→4. */
    public static final int BANNER_FACING_NORTH = 8;
    /** {@code :159} — south. */
    public static final int BANNER_FACING_SOUTH = 0;
    /** {@code :161} — west. */
    public static final int BANNER_FACING_WEST = 12;
    /** {@code :163} — east. */
    public static final int BANNER_FACING_EAST = 4;

    /**
     * {@code :141} — the {@code ForgeDirection} of the loop index, read off the
     * original's {@code offsetX}/{@code offsetZ} pair: 2 north (0,-1), 3 south
     * (0,1), 4 west (-1,0), 5 east (1,0). Any other index has no direction in
     * {@code ForgeDirection.getOrientation} (0 down, 1 up are skipped by the loop).
     */
    public static int bannerOffsetX(final int directionIndex) {
        return switch (directionIndex) {
            case 4 -> -1;                                                                 // :141 (WEST)
            case 5 -> 1;                                                                  // :141 (EAST)
            default -> 0;                                                                 // :141 (NORTH/SOUTH)
        };
    }

    /** {@code :141} — the direction's {@code offsetZ} (north is -z in both engines). */
    public static int bannerOffsetZ(final int directionIndex) {
        return switch (directionIndex) {
            case 2 -> -1;                                                                 // :141 (NORTH)
            case 3 -> 1;                                                                  // :141 (SOUTH)
            default -> 0;                                                                 // :141 (WEST/EAST)
        };
    }

    /** {@code :155-167} — the {@code TileBanner} facing the loop index writes. */
    public static int bannerFacing(final int directionIndex) {
        return switch (directionIndex) {
            case 2 -> BANNER_FACING_NORTH;                                                // :157
            case 3 -> BANNER_FACING_SOUTH;                                                // :159
            case 4 -> BANNER_FACING_WEST;                                                 // :161
            case 5 -> BANNER_FACING_EAST;                                                 // :163
            default -> BANNER_FACING_SOUTH;                                               // :155 (the switch's initial 0)
        };
    }

    /** {@code :155-167} — the facings in the loop's own order, for a probe's sweep. */
    public static int[] bannerFacingsInLoopOrder() {
        return new int[] {BANNER_FACING_NORTH, BANNER_FACING_SOUTH, BANNER_FACING_WEST,
            BANNER_FACING_EAST};
    }
    /**
     * The {@code IBossDisplayData} seam ({@code :31}) — the bar's name key. 1.7.10's
     * bar title is the entity's display name, which resolves the mob's own lang key
     * (the giant's shape, {@code TaintacleGiantAiLogic:187}).
     */
    public static final String BOSS_BAR_NAME_KEY = "entity.thaumcraft.cultistportal";

    /** The lang key of the row's label ({@code ConfigEntities.java:116} "Crimson Portal"). */
    public static final String LANG_KEY = "entity.thaumcraft.cultistportal";

    /** {@code :31} — the bar's progress is the health fraction (the giant's helper). */
    public static float bossHealthProgress(final float health, final float maxHealth) {
        return maxHealth <= 0.0F ? 0.0F : health / maxHealth;
    }

    /** {@code :171, :197, :236, :251} — every summon and every placement plays the wandfail track. */
    public static final String SOUND_WANDFAIL = "thaumcraft:wandfail";

    /** {@code :184} — the crate window opens above countdown 20 … */
    public static final int CRATE_COUNTER_MIN = 20;
    /** {@code :184} — … and closes at 150 … */
    public static final int CRATE_COUNTER_MAX = 150;
    /** {@code :184} — … in stage 0 … */
    public static final int CRATE_STAGE = 0;
    /** {@code :184} — … on every countdown value divisible by 13. */
    public static final int CRATE_PERIOD = 13;
    /** {@code :185-186} — the crate cell is {@code (int)x + nextInt(5) - nextInt(5)} on x and z. */
    public static final int CRATE_SPREAD = 5;
    /** {@code :187} — the cell must differ from the portal's own column on BOTH axes … */
    public static final boolean CRATE_SKIPS_OWN_COLUMN = true;
    /** {@code :187} — … and the target cell must not be air ({@code getBlockId(...) > 0}). */
    public static final boolean CRATE_NEEDS_FILLLED_CELL = true;
    /** {@code :190} — the crate's meta: below 0.05 → 2 (rare) … */
    public static final float CRATE_META_RARE_ROLL = 0.05F;
    /** {@code :190} — … below 0.2 → 1 (uncommon) … */
    public static final float CRATE_META_UNCOMMON_ROLL = 0.2F;
    /** {@code :190} — … else 0 (common). */
    public static final int CRATE_META_COMMON = 0;
    /** {@code :190} — the uncommon meta. */
    public static final int CRATE_META_UNCOMMON = 1;
    /** {@code :190} — the rare meta. */
    public static final int CRATE_META_RARE = 2;
    /** {@code :191} — the crate lands on the portal's own y level: {@code (int)y}. */
    public static final int CRATE_Y_OFFSET = 0;

    /** {@code :199-201} — the self-heal runs on every countdown tick while {@code stage < 12}. */
    public static final int HEAL_STAGE_MAX = 12;
    /** {@code :200} — the heal amount is 1.0F. */
    public static final float HEAL_AMOUNT = 1.0F;

    /** {@code :213-215} — the cultist scan is a 32.0 cube around the portal, excluding the portal itself. */
    public static final double SUMMON_SCAN_RADIUS = 32.0;
    /** {@code :216} — the timing is {@code count * 20}; an empty scan gives 0, and the 0 is used, not skipped. */
    public static final int TIMING_PER_CULTIST = 20;
    /** {@code :221} — {@code nextFloat() > 0.33} picks the knight, the {@code else} arm the cleric. */
    public static final float MINION_KNIGHT_ROLL = 0.33F;
    /** {@code :227, :244} — a minion lands 0.25 above the portal's feet. */
    public static final double MINION_Y_OFFSET = 0.25;
    /** {@code :232, :247} — the home radius handed to {@code EntityCultist.setHomePosition}. */
    public static final int MINION_HOME_RADIUS = 32;
    /** {@code :237} — the self-damage gate is {@code stage > 12} (strictly past the boss stage). */
    public static final int SELF_DAMAGE_STAGE_MIN = 13;
    /** {@code :238} — the self-damage base. */
    public static final float SELF_DAMAGE_BASE = 5.0F;
    /** {@code :238} — the self-damage roll. */
    public static final int SELF_DAMAGE_ROLL = 5;

    /** {@code :255} — player contact deals damage inside squared distance 3.0 … */
    public static final double CONTACT_RANGE_SQ = 3.0;
    /** {@code :255} — … for 8.0 damage, mob-attack sourced … */
    public static final float CONTACT_DAMAGE = 8.0F;
    /** {@code :256} — … and the zap fires only on the arm of a successful hit; its pitch is {@code (rand - rand) * 0.1 + 1.0}. */
    public static final float CONTACT_PITCH_SPREAD = 0.1F;
    /** {@code :256} — the zap's pitch centre. */
    public static final float CONTACT_PITCH_BASE = 1.0F;
    /** {@code :256} — the zap's volume. */
    public static final float CONTACT_VOLUME = 1.0F;

    /** {@code :308} — the death explosion is 2.0F … */
    public static final float DEATH_EXPLOSION_POWER = 2.0F;
    /** {@code :308} — … sets no fire … */
    public static final boolean DEATH_EXPLOSION_FIRE = false;
    /** {@code :308} — … and breaks no blocks. */
    public static final boolean DEATH_EXPLOSION_BREAKS_BLOCKS = false;

    /** {@code :287-288} — the custom drop is eldritch object meta 3 … */
    public static final int ELDRITCH_OBJECT_META = 3;
    /** {@code :288} — … thrown at {@code maxHealth / 2} above the portal (500/2 = 250 blocks). */
    public static final double DROP_LIFT_DIVISOR = 2.0;

    /**
     * {@code :51-59} — the ONLY key this entity writes: {@code tag.putInt("stage",
     * stage)}. The countdown and the pulse are NOT saved, so a loaded portal
     * restarts its countdown at 200 with the stage it was saved on.
     */
    public static final String TAG_STAGE = "stage";

    // ============================================================ registry seams
    // Every world-write target is looked up by id at runtime, BD-68's shape, so a
    // slice that has not landed costs the branch, not the compile.

    /** {@code :142} — the banner block, {@code thaumcraft:blockwoodendevice} (landed, {@code CatalogDevices.java:110}). */
    public static final ResourceLocation BLOCK_WOODEN_DEVICE =
            ResourceLocation.parse("thaumcraft:blockwoodendevice");
    /** {@code :191} — the crate block, {@code thaumcraft:blocklootcrate} (landed, {@code CatalogTrinketsLoot.java:69}). */
    public static final ResourceLocation BLOCK_LOOT_CRATE =
            ResourceLocation.parse("thaumcraft:blocklootcrate");
    /** {@code :288} — the custom death drop, {@code thaumcraft:itemeldritchobject} (landed, {@code CatalogEldritchRealm.java:196}). */
    public static final ResourceLocation ITEM_ELDRITCH_OBJECT =
            ResourceLocation.parse("thaumcraft:itemeldritchobject");

    /** {@code :170} — the mob's own ambient track. */
    public static final String SOUND_AMBIENT = "thaumcraft:monolith";
    /** {@code :256, :275} — the hurt sound, also the contact zap. */
    public static final String SOUND_ZAP = "thaumcraft:zap";
    /** {@code :279} — the death sound. */
    public static final String SOUND_DEATH = "thaumcraft:shock";

    /** {@code :222, :224} — the summoned minions are the cultist family's landed classes. */
    public static final ResourceLocation CULTIST_KNIGHT = ResourceLocation.parse("thaumcraft:cultistknight");
    /** {@code :224} — the cleric. */
    public static final ResourceLocation CULTIST_CLERIC = ResourceLocation.parse("thaumcraft:cultistcleric");
    /** {@code :243} — the boss arm's summon. */
    public static final ResourceLocation CULTIST_LEADER = ResourceLocation.parse("thaumcraft:cultistleader");

    /** {@code :170} — the banner/crate FX arc is broadcast to a 32-block radius ({@code TargetPoint(..., 32.0)}). */
    public static final double FX_BROADCAST_RADIUS = 32.0;

    // ==================================================================== rules

    /** The three firing arms of {@code :105-124}. */
    public enum Summon {
        /** Only reachable if the machine is entered with a negative stage, which {@code :125} never produces. */
        NONE,
        /** {@code :111}, {@code :117} — {@code spawnMinions()}: one knight or one cleric. */
        MINIONS,
        /** {@code :122} — {@code spawnBoss()}: one Crimson Praetor. */
        BOSS
    }

    /**
     * {@code :105-124} — which arm a stage takes. Stages 0-4 and 5-11 both call
     * {@code spawnMinions()} (they differ only in the countdown they re-arm with),
     * the {@code default} arm of {@code :113} catches every stage past 12 and also
     * summons minions, and stage 12 is the only {@code spawnBoss()} arm.
     */
    public static Summon actionFor(final int stage) {
        if (stage == STAGE_BOSS) {
            return Summon.BOSS;                                                          // :120-122
        }
        if (stage < 0) {
            return Summon.NONE;                                                          // :105 (no case, no default fallthrough)
        }
        return Summon.MINIONS;                                                           // :111, :117
    }

    /** {@code :106} — is the stage in the short-counter arm (cases 0..4)? */
    public static boolean isShortCounterStage(final int stage) {
        return stage >= 0 && stage <= SHORT_COUNTER_STAGE_MAX;                           // :106-112
    }

    /**
     * {@code :110} — the short countdown: {@code 15 + nextInt(10 - stage) - stage}.
     * The drawn value is a parameter, so a probe pins every value of the interval.
     */
    public static int shortCounterFor(final int stage, final int drawn) {
        return SHORT_COUNTER_BASE + drawn - stage;                                       // :110
    }

    /**
     * {@code :116} — the timed countdown: {@code t + nextInt(5 + t / 3)}, where
     * {@code t} is {@link #timingFor(int)}. Integer division, as in the reference.
     */
    public static int timedCounterFor(final int timing, final int drawn) {
        return timing + drawn;                                                           // :116
    }

    /** {@code :116} — the timed arm's roll interval: {@code 5 + t / 3}. */
    public static int timedCounterRollSpread(final int timing) {
        return TIMED_COUNTER_BONUS + timing / TIMED_COUNTER_DIVISOR;                     // :116
    }

    /** {@code :121} — the boss countdown: {@code 50 + t * 2 + nextInt(50)}. */
    public static int bossCounterFor(final int timing, final int drawn) {
        return BOSS_COUNTER_BASE + timing * BOSS_COUNTER_TIMING_FACTOR + drawn;          // :121
    }

    /** {@code :128} — the no-player re-arm: {@code 30 + nextInt(30)}. */
    public static int noPlayerCounterFor(final int drawn) {
        return NO_PLAYER_COUNTER_BASE + drawn;                                           // :128
    }

    /**
     * {@code :213-216} — the timing: the 32.0 cultist scan's size times 20. An empty
     * scan yields 0, and {@code :116} then draws from a spread of 5, so a portal
     * with no cultists nearby fires on roughly a 5-tick countdown.
     */
    public static int timingFor(final int cultistCount) {
        return cultistCount * TIMING_PER_CULTIST;                                        // :216
    }

    /** {@code :125} — the stage increments after every firing; it never wraps. */
    public static int stageAfterFiring(final int stage) {
        return stage + 1;                                                                // :125
    }

    /** {@code :102} — the machine fires on {@code stagecounter <= 0} and otherwise only counts down. */
    public static boolean fires(final int stageCounter) {
        return stageCounter <= 0;                                                        // :102
    }

    /** {@code :132} — the banner wall's gate: the exact value 160, in stage 0. */
    public static boolean bannerGate(final int stageCounter, final int stage) {
        return stageCounter == BANNER_COUNTER && stage == BANNER_STAGE;                  // :132
    }

    /**
     * {@code :184} — the crate gate: the countdown inside (20, 150) exclusive, stage
     * 0, and the countdown divisible by 13. On the way down from 200 the values are
     * 143, 130, 117, 104, 91, 78, 65, 52, 39, 26.
     */
    public static boolean crateGate(final int stageCounter, final int stage) {
        return stageCounter > CRATE_COUNTER_MIN && stageCounter < CRATE_COUNTER_MAX
                && stage == CRATE_STAGE && stageCounter % CRATE_PERIOD == 0;             // :184
    }

    /**
     * {@code :189-190} — the crate's rarity. The reference draws from
     * {@code world.rand} (the level's random), NOT the entity's, and maps the float
     * to the original's block metadata 0/1/2 (common/uncommon/rare).
     */
    public static int crateMetaFor(final float roll) {
        if (roll < CRATE_META_RARE_ROLL) {
            return CRATE_META_RARE;                                                      // :190
        }
        return roll < CRATE_META_UNCOMMON_ROLL ? CRATE_META_UNCOMMON : CRATE_META_COMMON; // :190
    }

    /**
     * {@code :185-187} — the crate cell. Two {@code nextInt(5)} draws land the x and
     * z offsets (so the offset is -4..4), the cell must differ from the portal's own
     * column on both axes, and the cell must be filled.
     */
    public static boolean crateCellValid(final int cellX, final int ownX,
            final int cellZ, final int ownZ, final boolean cellIsFilled) {
        return cellX != ownX && cellZ != ownZ && cellIsFilled;                           // :187
    }

    /** {@code :199} — the self-heal gate: strictly below stage 12. */
    public static boolean healsForStage(final int stage) {
        return stage < HEAL_STAGE_MAX;                                                   // :199
    }

    /** {@code :221} — the knight arm of {@code spawnMinions}: {@code nextFloat() > 0.33F}. */
    public static boolean summonsKnight(final float roll) {
        return roll > MINION_KNIGHT_ROLL;                                                // :221
    }

    /** {@code :237} — the self-damage gate: strictly past stage 12. */
    public static boolean damagesSelf(final int stage) {
        return stage > STAGE_BOSS;                                                       // :237
    }

    /** {@code :238} — the self-damage amount: {@code 5 + nextInt(5)}, drown-sourced (D-137). */
    public static float selfDamageFor(final int drawn) {
        return SELF_DAMAGE_BASE + drawn;                                                 // :238
    }

    /** {@code :255} — the contact verdict on squared distance. */
    public static boolean contactHits(final double distanceSq) {
        return distanceSq < CONTACT_RANGE_SQ;                                            // :255
    }

    /**
     * {@code :256} — the contact zap's pitch: {@code (rand - rand) * 0.1F + 1.0F}.
     * The two draws are a parameter pair so the pitch interval is provable.
     */
    public static float contactPitchFor(final double drawn, final double drawn2) {
        return (float) ((drawn - drawn2) * CONTACT_PITCH_SPREAD + CONTACT_PITCH_BASE);   // :256
    }

    /** {@code :288} — the drop lift: {@code field_70131_O / 2.0F} = max health over 2. */
    public static double dropLiftFor(final double maxHealth) {
        return maxHealth / DROP_LIFT_DIVISOR;                                            // :288
    }

    /**
     * The countdown tick of {@code :131-201} as a pure verdict record, so a probe
     * can drive the whole machine without a {@code Level}.
     */
    public record CountdownTick(int stageCounter, boolean bannerLeg, boolean crateLeg,
            boolean heals) {
    }

    /**
     * {@code :131-201} — one countdown tick: decrement, then the two fixed-value
     * legs (banner, crate), then the heal. The decrement happens FIRST, so the
     * banner leg sees the value it decremented to — which is why the gate is
     * {@code == 160} and not {@code >= 160}.
     */
    public static CountdownTick countdownTick(final int stageCounterBefore, final int stage) {
        final int counter = stageCounterBefore - 1;                                      // :131
        return new CountdownTick(counter, bannerGate(counter, stage), crateGate(counter, stage),
                healsForStage(stage));                                                   // :132-201
    }
}
