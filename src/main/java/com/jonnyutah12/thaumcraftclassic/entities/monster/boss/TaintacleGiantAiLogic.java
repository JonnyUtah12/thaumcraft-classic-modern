package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

/**
 * Level-free rules for the Giant Taintacle boss (content-mapping.csv
 * {@code thaumcraft:taintaclegiant}, row 345).
 *
 * <p>Every constant and branch below is read straight off the decompiled 1.7.10
 * source
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityTaintacleGiant.java}
 * (131 lines), cited as {@code EntityTaintacleGiant.java:<line>}, plus the two
 * helpers it calls: {@code common/lib/utils/EntityUtils.java} (cited as
 * {@code EntityUtils.java:<line>}) and {@code common/entities/monster/mods/ChampionModifier.java}.
 * The entity class walks state; the arithmetic and gates live here so the JUnit
 * suite ({@code TaintacleGiantAiLogicTest}) can pin them without a {@code Level}.</p>
 *
 * <p>The giant is a bare subclass of {@code EntityTaintacle}: it installs no goals
 * of its own (the family installs none at all, see
 * {@link com.jonnyutah12.thaumcraftclassic.entities.monster.GoallessTaintMonster}),
 * so the tendril AI it inherits runs with boss-sized numbers — a 125 maxHealth
 * gives it a 125-block melee reach, a ±750-block target scan (±375 in y), an
 * 875-block agitation window, a 15.625 sound volume and a −11.2 sound pitch (the
 * family derives all of those from max health, {@code EntityTaintacle.java:154},
 * {@code :78}, {@code :252}, {@code :283-289}). Those are pinned in the parity
 * test against this file's {@link #HEALTH}.</p>
 *
 * <p>Quirks kept verbatim, never modernised — see this card's
 * {@code quirks_preserved} list and the javadoc on each rule.</p>
 */
public final class TaintacleGiantAiLogic {

    // ------------------------------------------------------------- ctor (:22-26)

    /** {@code EntityTaintacleGiant.java:24} — {@code setSize(1.1F, 6.0F)} width. */
    public static final float WIDTH = 1.1F;
    /** {@code EntityTaintacleGiant.java:24} — {@code setSize(1.1F, 6.0F)} height. */
    public static final float HEIGHT = 6.0F;
    /** {@code EntityTaintacleGiant.java:25} — {@code field_70728_aV = 20} (xpReward). */
    public static final int XP = 20;

    // ------------------------------------------------------- attributes (:29-33)

    /** {@code EntityTaintacleGiant.java:31} — MAX_HEALTH {@code 125.0}. */
    public static final double HEALTH = 125.0;
    /** {@code EntityTaintacleGiant.java:32} — field_111264_e (ATTACK_DAMAGE) {@code 9.0}. */
    public static final double ATTACK = 9.0;

    // ------------------------------------------------------------ anger (:68-79)

    /**
     * {@code EntityTaintacleGiant.java:70} — the anger slot is data-watcher id 14,
     * registered as a {@code Short}. 1.21.1 assigns synched-data ids itself, so the
     * number is a citation, not a wire value; the Short type is kept.
     */
    public static final int ANGER_WATCHER_ID = 14;
    /** {@code EntityTaintacleGiant.java:116} — a fresh enrage sets anger to 200. */
    public static final int ANGER_ON_ENRAGE = 200;
    /**
     * The NBT key this port writes the anger to. 1.7.10 serialised the whole
     * data-watcher table, so the original has no key of its own (:68-79 register
     * the watcher and never touch NBT); 1.21.1 does not persist synched data, so
     * the port chose this key to keep the 1.7.10 save/load behaviour.
     */
    public static final String ANGER_NBT_KEY = "Anger";

    // ------------------------------------------------------------- enrage (:109-130)

    /**
     * {@code EntityTaintacleGiant.java:110} — the enrage gate is the server side
     * AND {@code damage > 35.0F} (strict).
     */
    public static final float ENRAGE_DAMAGE_THRESHOLD = 35.0F;
    /** {@code EntityTaintacleGiant.java:126} — the same hit is then capped to 35.0F. */
    public static final float ENRAGE_DAMAGE_CAP = 35.0F;
    /** {@code EntityTaintacleGiant.java:113-115} — all three effects last 200 ticks. */
    public static final int ENRAGE_EFFECT_DURATION = 200;
    /** {@code EntityTaintacleGiant.java:113} — {@code (int)(damage / 15.0F)} amplifier divisor. */
    public static final float ENRAGE_REGEN_AMPLIFIER_DIVISOR = 15.0F;
    /** {@code EntityTaintacleGiant.java:114}/{@code :115} — {@code (int)(damage / 40.0F)}. */
    public static final float ENRAGE_OTHER_AMPLIFIER_DIVISOR = 40.0F;

    /**
     * {@code EntityTaintacleGiant.java:113} — {@code Potion.field_76428_l} is the
     * 1.7.10 potion id 10, {@code potion.regeneration} (its colour 13458603
     * matches 1.21.1's regeneration effect exactly).
     */
    public static final String ENRAGE_EFFECT_REGENERATION_ID = "minecraft:regeneration";
    /**
     * {@code EntityTaintacleGiant.java:114} — {@code Potion.field_76420_g} is the
     * 1.7.10 potion id 5, {@code potion.damageBoost} → 1.21.1 {@code strength}.
     */
    public static final String ENRAGE_EFFECT_STRENGTH_ID = "minecraft:strength";
    /**
     * {@code EntityTaintacleGiant.java:115} — {@code Potion.field_76424_c} is the
     * 1.7.10 potion id 1, {@code potion.moveSpeed} → 1.21.1 {@code speed}.
     */
    public static final String ENRAGE_EFFECT_SPEED_ID = "minecraft:speed";

    /**
     * {@code EntityTaintacleGiant.java:122} — the enrage chat line is the entity
     * name, a space, then {@code StatCollector.translateToLocal("tc.boss.enrage")}.
     * The lang row is wired by the catalog parent card.
     */
    public static final String ENRAGE_LANG_KEY = "tc.boss.enrage";

    // -------------------------------------------------- client anger particles (:47-60)

    /** {@code EntityTaintacleGiant.java:47} — the particle rolls on {@code nextInt(15) == 0}. */
    public static final int PARTICLE_ROLL_BOUND = 15;
    /** {@code EntityTaintacleGiant.java:48-50} — each velocity is {@code nextGaussian() * 0.02}. */
    public static final double PARTICLE_VELOCITY_SCALE = 0.02;
    /** {@code EntityTaintacleGiant.java:54}/{@code :56} — the xz jitter span is the box width. */
    public static final float PARTICLE_AXIS_WIDTH_MULTIPLIER = 1.0F;
    /** {@code EntityTaintacleGiant.java:54}/{@code :56} — minus half the box width. */
    public static final float PARTICLE_AXIS_HALF_WIDTH_DIVISOR = 2.0F;
    /**
     * {@code EntityTaintacleGiant.java:55} — the y is
     * {@code boundingBox.minY + this.field_70131_O + rand*0.5}. {@code field_70131_O}
     * is {@code Entity.height} — the 6.0 from {@code setSize(1.1F, 6.0F)} at :24 —
     * NOT maxHealth (the card brief reads it as 125; the field is the box height,
     * and the same field is the drop height at :92, which only makes sense as the
     * box). So the particle rides the top of the 6-block box. Pinned as 1.0×height.
     */
    public static final float PARTICLE_Y_HEIGHT_MULTIPLIER = 1.0F;
    /** {@code EntityTaintacleGiant.java:55} — plus {@code nextFloat() * 0.5}. */
    public static final float PARTICLE_Y_JITTER_SPAN = 0.5F;

    // ------------------------------------------------------------- regen (:63-65)

    /** {@code EntityTaintacleGiant.java:63} — heal every 30 ticks ({@code field_70173_aa % 30 == 0}). */
    public static final int REGEN_INTERVAL = 30;
    /** {@code EntityTaintacleGiant.java:64} — {@code func_70691_i(1.0F)} = heal 1.0F. */
    public static final float REGEN_AMOUNT = 1.0F;

    // ----------------------------------------------------------- champion (:35-38)

    /**
     * {@code EntityTaintacleGiant.java:36} — {@code EntityUtils.makeChampion(this, true)}:
     * the {@code persist} flag is true, and that is the only half this port ships
     * (the persist half is {@code entity.setPersistenceRequired()},
     * {@code EntityUtils.java:303-305}).
     */
    public static final boolean CHAMPION_PERSIST = true;
    /**
     * {@code ChampionModifier.java:12-26} — 13 champion mods, so the roll is
     * {@code rand.nextInt(13)} ({@code EntityUtils.java:282}). The champion
     * modifier itself (the CHAMPION_MOD/"tc.mobmod" attribute, CHAMPION_HEALTH,
     * CHAMPION_DAMAGE, the 25-heart heal at {@code EntityUtils.java:297} and the
     * "Bold Giant Taintacle" name prefix) is the un-inventoried champion
     * subsystem and is OUT OF SCOPE for this card.
     */
    public static final int CHAMPION_MOD_COUNT = 13;
    /**
     * {@code EntityUtils.java:297} — a non-boss champion heals 25.0F. Pinned for
     * the champion card; not applied here (out of scope).
     */
    public static final double CHAMPION_HEAL = 25.0;

    // --------------------------------------------------------- death drop (:86-94)

    /**
     * {@code EntityTaintacleGiant.java:89} — the "is any other giant around" scan
     * range; {@code EntityUtils.java:223} expands a zero-size box by the range on
     * all three axes.
     */
    public static final double DEATH_DROP_SCAN_RANGE = 48.0;
    /**
     * {@code EntityTaintacleGiant.java:92} — the drop is
     * {@code new ItemStack(ConfigItems.itemEldritchObject, 1, 3)}: eldritch object
     * meta 3 (the Runed Tablet family — content-mapping.csv row 268 names the five
     * metas "Eldritch Eye / Crimson Rites / Runed Tablet / Primordial Pearl /
     * Eldritch Obelisk Placer").
     */
    public static final int DEATH_DROP_META = 3;
    /** {@code EntityTaintacleGiant.java:92} — drop height = {@code field_70131_O / 2.0F} (box height/2). */
    public static final float DEATH_DROP_HEIGHT_DIVISOR = 2.0F;

    // ------------------------------------------------- boss bar (:21, IBossDisplayData)

    /**
     * {@code EntityTaintacleGiant.java:21} — the class implements 1.7.10's
     * {@code IBossDisplayData}, whose whole contract is "draw my health as the boss
     * bar", i.e. progress = health / maxHealth. 21.1.259 has no
     * {@code net.minecraft.world.level.boss.BossBar} and no {@code Boss} interface:
     * the seam is a {@code ServerBossEvent} fed from the tracking-player hooks
     * (the vanilla shape is {@code WitherBoss.java:74-80, 352, 379-391}).
     */
    public static final String BOSS_BAR_NAME_KEY = "entity.thaumcraft.taintaclegiant";

    // ----------------------------------------------------------- gates (:80-106)

    /** {@code EntityTaintacleGiant.java:80-84} — {@code getCanSpawnHere() = false}: never a natural spawn. */
    public static final boolean NATURAL_SPAWN_ALLOWED = false;
    /** {@code EntityTaintacleGiant.java:96-98} — {@code func_70692_ba() = false}: the Looting gate. */
    public static final boolean LOOTING_BONUS_ALLOWED = false;
    /** {@code EntityTaintacleGiant.java:100-102} — {@code func_70648_aU() = true}: the original declares itself invulnerable. */
    public static final boolean DECLARES_INVULNERABLE = true;
    /** {@code EntityTaintacleGiant.java:104-106} — {@code func_70682_h(air)} returns the air unchanged: it never drowns. */
    public static final boolean AIR_DRAINS = false;

    private TaintacleGiantAiLogic() {
    }

    // ------------------------------------------------------------------ rules

    /**
     * {@code EntityTaintacleGiant.java:43-45} — the anger counter ticks down by 1
     * while it is above 0, and stays put at 0. The original runs this on BOTH sides
     * ({@code func_70071_h_}); 1.21.1 syncs the watcher server→client, so the
     * client copy is authoritative from the server.
     */
    public static int angerAfterTick(final int anger) {
        return anger > 0 ? anger - 1 : anger;
    }

    /**
     * {@code EntityTaintacleGiant.java:78} — {@code setAnger(int)} writes the
     * watcher as {@code (short) par1}, so any value is narrowed to 16 bits;
     * {@code :74} reads the Short back. The narrowing is a quirk, kept.
     */
    public static short angerAsShort(final int anger) {
        return (short) anger;
    }

    /** {@code EntityTaintacleGiant.java:74} — {@code getAnger()} is the stored Short. */
    public static int angerFromShort(final short stored) {
        return stored;
    }

    /**
     * {@code EntityTaintacleGiant.java:110-111} — the enrage fires on the server,
     * only above 35 damage, and only while the anger is exactly 0 (one enrage per
     * 200-tick anger window).
     */
    public static boolean canEnrage(final float damage, final int anger, final boolean clientSide) {
        return !clientSide
                && damage > ENRAGE_DAMAGE_THRESHOLD
                && anger == 0;
    }

    /**
     * {@code EntityTaintacleGiant.java:110}+{@code :126} — the cap lives inside the
     * {@code !isRemote && damage > 35} branch, so client-side hits and hits of
     * 35-or-less pass through untouched.
     */
    public static float cappedDamage(final float damage, final boolean clientSide) {
        return !clientSide && damage > ENRAGE_DAMAGE_THRESHOLD ? ENRAGE_DAMAGE_CAP : damage;
    }

    /** {@code EntityTaintacleGiant.java:113} — {@code (int)(damage / 15.0F)} on the regeneration effect. */
    public static int regenerationAmplifier(final float damage) {
        return (int) (damage / ENRAGE_REGEN_AMPLIFIER_DIVISOR);
    }

    /** {@code EntityTaintacleGiant.java:114} — {@code (int)(damage / 40.0F)} on the strength effect. */
    public static int strengthAmplifier(final float damage) {
        return (int) (damage / ENRAGE_OTHER_AMPLIFIER_DIVISOR);
    }

    /** {@code EntityTaintacleGiant.java:115} — {@code (int)(damage / 40.0F)} on the speed effect. */
    public static int speedAmplifier(final float damage) {
        return (int) (damage / ENRAGE_OTHER_AMPLIFIER_DIVISOR);
    }

    /**
     * {@code EntityTaintacleGiant.java:47} — the anger particle needs
     * {@code nextInt(15) == 0} AND anger above 0.
     */
    public static boolean rollsAngerParticle(final int roll, final int anger) {
        return roll == 0 && anger > 0;
    }

    /** {@code EntityTaintacleGiant.java:48-50} — one velocity component. */
    public static double particleVelocity(final double gaussian) {
        return gaussian * PARTICLE_VELOCITY_SCALE;
    }

    /**
     * {@code EntityTaintacleGiant.java:54} (x) and {@code :56} (z) — the axis is
     * jittered across the box width: {@code axis + rand * width - width / 2}.
     */
    public static double particleAxis(final double axis, final double width, final double roll) {
        return axis + roll * width * PARTICLE_AXIS_WIDTH_MULTIPLIER
                - width / PARTICLE_AXIS_HALF_WIDTH_DIVISOR;
    }

    /**
     * {@code EntityTaintacleGiant.java:55} —
     * {@code boundingBox.minY + height + rand * 0.5}. See
     * {@link #PARTICLE_Y_HEIGHT_MULTIPLIER}: the multiplier is on the box height
     * ({@code field_70131_O}), not on maxHealth.
     */
    public static double particleY(final double minY, final double height, final double roll) {
        return minY + height * PARTICLE_Y_HEIGHT_MULTIPLIER + roll * PARTICLE_Y_JITTER_SPAN;
    }

    /**
     * {@code EntityTaintacleGiant.java:63-64} — the server heal, on the 30-tick
     * cadence. Tick 0 counts ({@code field_70173_aa % 30 == 0} with no age guard).
     */
    public static boolean regenerates(final int tickCount) {
        return tickCount % REGEN_INTERVAL == 0;
    }

    /**
     * {@code EntityTaintacleGiant.java:35-38} — {@code onInitialSpawn} calls
     * {@code makeChampion} unconditionally: every giant is a champion, and this
     * call site has no {@code Config.championMobs} gate
     * ({@code TcConfigCatalog.CHAMPION_MOBS} gates the natural-spawn callers).
     */
    public static boolean championRolledOnEverySpawn() {
        return true;
    }

    /** {@code EntityUtils.java:282} — {@code rand.nextInt(ChampionModifier.mods.length)}. */
    public static int championModIndex(final int roll) {
        return Math.floorMod(roll, CHAMPION_MOD_COUNT);
    }

    /** {@code EntityUtils.java:283-285} — a creeper is forced to mod 0 ("bold"). */
    public static int championModIndexForced(final int index, final boolean isCreeper) {
        return isCreeper ? 0 : index;
    }

    /**
     * {@code EntityTaintacleGiant.java:88-93} + {@code EntityUtils.java:221-234} —
     * the giant drops its eldritch object only when no OTHER giant is inside the
     * 48-block cube (the helper drops the caller itself by entity id,
     * {@code EntityUtils.java:227}, and a null/empty list counts as "nobody").
     */
    public static boolean dropsEldritchObject(final int otherGiantsInScanBox) {
        return otherGiantsInScanBox <= 0;
    }

    /** {@code EntityUtils.java:223} — the scan cube: a point box inflated by the range. */
    public static net.minecraft.world.phys.AABB deathDropScanBox(final double x, final double y, final double z) {
        return new net.minecraft.world.phys.AABB(x, y, z, x, y, z)
                .inflate(DEATH_DROP_SCAN_RANGE, DEATH_DROP_SCAN_RANGE, DEATH_DROP_SCAN_RANGE);
    }

    /** {@code EntityTaintacleGiant.java:92} — {@code field_70131_O / 2.0F}. */
    public static float deathDropOffset(final float height) {
        return height / DEATH_DROP_HEIGHT_DIVISOR;
    }

    /** {@code EntityTaintacleGiant.java:21} — the boss bar reads the health fraction. */
    public static float bossHealthProgress(final float health, final float maxHealth) {
        return maxHealth <= 0.0F ? 0.0F : health / maxHealth;
    }

    /**
     * {@code EntityTaintacleGiant.java:104-106} — {@code func_70682_h(int air)}
     * returns {@code air}, so the drown counter never moves.
     */
    public static int airSupplyAfterTick(final int currentAir) {
        return currentAir;
    }
}
