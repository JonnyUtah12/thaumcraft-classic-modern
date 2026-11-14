package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.resources.ResourceLocation;

/**
 * World-free constants and pure decisions for the Eldritch Crab —
 * {@code thaumcraft:eldritchcrab} (content-mapping.csv line 330,
 * {@code Thaumcraft:EldritchCrab}, mod_index 32, registered at
 * {@code ConfigEntities.java:121}).
 *
 * <p>Original: {@code thaumcraft.common.entities.monster.EntityEldritchCrab},
 * 215 lines, {@code extends EntityMob}. Every figure here carries the source
 * line it was read from; {@link EntityEldritchCrab} is the thin live shell.
 * The crab is the helmet-carrying shell of the Outer Lands guardians: a
 * 0.8 x 0.6 carapace ({@code :33}) that leaps ({@code :38}), climbs onto a
 * bareheaded player standing under it ({@code :120-129}) and punches its
 * mount on a 10..19-tick cooldown ({@code :131-137}), and whose shell is the
 * 1.7.10 spider GroupData potion host ({@code :78-92}).</p>
 */
public final class EldritchCrabAiLogic {

    private EldritchCrabAiLogic() {
    }

    // ---------------------------------------------------------------- registration

    /** {@code ConfigEntities.java:121} — {@code registerModEntity(crab, "EldritchCrab", id++, ..., 64, 3, true)}:
     *  tracking 64 blocks = 4 chunks (family rule pinned at
     *  {@code BrainyZombieAiLogicTest:38} — 1.21.1 counts chunks), update
     *  interval 3, velocity updates true (always true for a mob built this way).
     *  The 32 is the {@code mod_index} column of the crab's content-mapping row
     *  (CSV line 330), the ledger's record of the original's {@code id++}. */
    public static final int CRAB_MOD_INDEX = 32;
    public static final int TRACK_RANGE_BLOCKS = 64;
    public static final int TRACK_RANGE_CHUNKS = 4;
    public static final int UPDATE_INTERVAL = 3;
    public static final boolean VELOCITY_UPDATES = true;

    /** {@code ConfigEntities.java:87} — the crab's own projectile
     *  ({@code thaumcraft:eldritchorb}, CSV line 309, mod_index 11): the
     *  projectile triple 64 / 20 / velocity true, so the orb's figures ride
     *  {@link com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityEldritchOrb}'s
     *  own constants. */
    public static final int ORB_MOD_INDEX = 11;
    public static final int ORB_TRACK_RANGE = 64;
    public static final int ORB_UPDATE_INTERVAL = 20;

    /** {@code :33} — {@code func_70105_a(0.8F, 0.6F)}: width 0.8, height 0.6. */
    public static final float WIDTH = 0.8F;
    public static final float HEIGHT = 0.6F;

    /** {@code :34} — {@code field_70728_aV = 6} (experienceValue). 1.21.1's
     *  {@code EntityType.Builder} carries no XP knob (it is a vanilla
     *  register-time constructor arg, and a mod builder leaves it 0), so the
     *  reward rides the constructor field write, the family's pattern
     *  ({@code EntityTaintacle:187}, {@code EntityCultist}). */
    public static final int XP = 6;

    // ---------------------------------------------------------------- attributes

    /** {@code :53-55} — health 20.0, attack 4.0, speed {@code hasHelm() ?
     *  0.275 : 0.3}. Armour is not an attribute write in the original — it is
     *  the runtime {@code func_70658_aO()} getter of {@code :67-69}
     *  ({@code hasHelm() ? 5 : 0}); 1.21.1 reads armour from the ARMOR
     *  attribute, so the shell flag drives the per-entity ARMOR base value
     *  (set at spawn, on {@link #armorFor(boolean)} and on load). */
    public static final double MAX_HEALTH = 20.0;
    public static final double ATTACK_DAMAGE = 4.0;
    public static final double SPEED_NO_SHELL = 0.3;
    public static final double SPEED_SHELL = 0.275;
    public static final double ARMOR_SHELL = 5.0;
    public static final double ARMOR_NONE = 0.0;

    /** {@code :55} / {@code :67-69} — the shell decides both figures. */
    public static double speedFor(final boolean shell) {
        return shell ? SPEED_SHELL : SPEED_NO_SHELL;
    }

    public static double armorFor(final boolean shell) {
        return shell ? ARMOR_SHELL : ARMOR_NONE;
    }

    // ---------------------------------------------------------------- goals

    /** {@code :37} Swimming slot 0 → 1.21.1 {@code FloatGoal}. */
    public static final int GOAL_SLOT_SWIM = 0;
    /** {@code :38} — {@code EntityAILeapAtTarget(this, 0.63F)} slot 2. */
    public static final int GOAL_SLOT_LEAP = 2;
    public static final float LEAP_CHANCE = 0.63F;
    /** {@code :39} — {@code AIAttackOnCollide(this, EntityLivingBase.class,
     *  1.0, false)} slot 3 → the family's melee mapping
     *  ({@code CultistAiLogic.GOAL_SLOT_MELEE}: MeleeAttackGoal(speed,
     *  longMemory)). */
    public static final int GOAL_SLOT_MELEE = 3;
    public static final float MELEE_SPEED = 1.0F;
    public static final boolean MELEE_LONG_MEMORY = false;
    /** {@code :40} — {@code EntityAIWander(this, 0.8)} slot 7 →
     *  {@code RandomStrollGoal(this, 0.8)} (the family's wander mapping,
     *  {@code CultistAiLogic.GOAL_SLOT_WANDER}). */
    public static final int GOAL_SLOT_WANDER = 7;
    public static final float WANDER_SPEED = 0.8F;
    /** {@code :41} — {@code EntityAILookIdle(this)} slot 8 →
     *  {@code RandomLookAroundGoal}. The crab has no WatchClosest, so slot 8
     *  carries no collision (unlike the guardian's pair). */
    public static final int GOAL_SLOT_LOOK_IDLE = 8;
    /** {@code :42} — {@code EntityAIHurtByTarget(this, true)} target slot 1.
     *  The {@code true} is the same-class alert: 1.21.1's plain
     *  {@code HurtByTargetGoal(this)} defaults its to-alert set to the mob's
     *  own class, which is the exact 1.7.10 {@code true} semantic. */
    public static final int TARGET_SLOT_HURT = 1;
    /** {@code :43} — {@code EntityAINearestAttackableTarget(this,
     *  EntityPlayer.class, 0, true)} target slot 2 (the {@code 0} is 1.7.10's
     *  goal priority, overridden by the slot; the {@code true} is the
     *  line-of-sight gate → 1.21.1's {@code mustSee}). */
    public static final int TARGET_SLOT_PLAYER = 2;
    /** {@code :44} — {@code EntityAINearestAttackableTarget(this,
     *  EntityCultist.class, 0, true)} target slot 3. The abstract,
     *  unregistered base class works as a selector filter in both engines
     *  (the scan tests the class of each found entity, so the registered
     *  {@code EntityCultistKnight}/{@code EntityCultistCleric} match). */
    public static final int TARGET_SLOT_CULTIST = 3;

    /** The target classes of the two {@code NearestAttackableTargetGoal}s —
     *  {@code EntityPlayer.class} at :43 (21.1.259's carrier of the legacy
     *  {@code EntityPlayer}, the shipped goal uses {@code Player}) and {@code
     *  EntityCultist.class} at :44 (the abstract cultist base, so the knight
     *  and the cleric both match — {@code EntityCultistKnight}/{@code
     *  EntityCultistCleric}). Pinned so a unit test can name the classes the
     *  goals are built with. */
    public static final Class<net.minecraft.world.entity.player.Player> TARGET_CLASS_PLAYER =
            net.minecraft.world.entity.player.Player.class;
    public static final Class<EntityCultist> TARGET_CLASS_CULTIST = EntityCultist.class;

    // ---------------------------------------------------------------- shell

    /** {@code :60} — the synched Flags byte (watcher index 22; 1.21.1
     *  allocates its own id, the original's index is pinned as data only).
     *  Bit 0 is the shell ({@code :101-112}). */
    public static final int FLAG_BIT_SHELL = 1;
    /** {@code :173} — the NBT key the whole Flags byte round-trips under. */
    public static final String NBT_FLAGS = "Flags";

    /** {@code :72-76} — the helm roll: HARD → shell guaranteed; otherwise
     *  {@code entityRandom.nextFloat() < 0.33F}. */
    public static final float SHELL_CHANCE = 0.33F;

    public static boolean rollsShell(final boolean hard, final float roll) {
        return hard || roll < SHELL_CHANCE;
    }

    /** {@code :78-84} — when no spawn data arrives, a fresh spider GroupData
     *  is created, and on HARD it picks a potion when
     *  {@code worldRandom.nextFloat() < 0.1F * localDifficulty} (the local
     *  difficulty float at the spawn position, {@code :82}). The roll uses the
     *  WORLD random ({@code field_73012_v}), unlike the helm roll's entity
     *  random — the two carriers are pinned apart on purpose. */
    public static final float GROUP_POTION_BASE_CHANCE = 0.1F;

    public static boolean rollsGroupPotion(final boolean hard, final float roll,
            final float localDifficulty) {
        return hard && roll < GROUP_POTION_BASE_CHANCE * localDifficulty;
    }

    /**
     * {@code :78-92} — the spider buff table, as the ORIGINAL carried it.
     * 1.7.10 stores a potion ID in {@code GroupData.field_111105_a} and rolls
     * it through {@code GroupData.func_111104_a}: the speed potion on a
     * coin-flip, then each of strength / regeneration / invisibility on
     * {@code nextFloat() < 0.25F * i} where {@code i} is 1 only if the
     * coin-flip landed, so the cascade can overwrite the pick up to three times
     * and a lost coin-flip yields nothing at all. 1.21.1 keeps the same four
     * effects ({@code MobEffects.MOVEMENT_SPEED}, {@code DAMAGE_BOOST},
     * {@code REGENERATION}, {@code INVISIBILITY}) in
     * {@code Spider.SpiderEffectsGroupData#setRandomEffect}, and its
     * {@code effect} field is protected with no accessor, so a mod subclass
     * cannot read the vanilla carrier at all. This port therefore carries the
     * roll as a marker over the same four-entry table and maps the marker to
     * the effect itself; the shipped cascade is verbatim 1.7.10.
     */
    public static final int GROUP_EFFECT_NONE = 0;
    public static final int GROUP_EFFECT_SPEED = 1;
    public static final int GROUP_EFFECT_STRENGTH = 2;
    public static final int GROUP_EFFECT_REGENERATION = 3;
    public static final int GROUP_EFFECT_INVISIBILITY = 4;
    public static final float GROUP_EFFECT_REROLL_CHANCE = 0.25F;

    /**
     * The {@code func_111104_a} cascade, world-free (seed a {@code RandomSource}
     * in a unit test and pin the distribution): coin-flip → speed, then three
     * 25% re-rolls in the source's order.
     */
    public static int rollGroupEffect(final net.minecraft.util.RandomSource random) {
        int marker = GROUP_EFFECT_NONE;
        int armed = 0;
        if (random.nextBoolean()) {                                   // :111104_a #1
            marker = GROUP_EFFECT_SPEED;
            armed = 1;
        }
        if (random.nextFloat() < GROUP_EFFECT_REROLL_CHANCE * armed) { // #2
            marker = GROUP_EFFECT_STRENGTH;
        }
        if (random.nextFloat() < GROUP_EFFECT_REROLL_CHANCE * armed) { // #3
            marker = GROUP_EFFECT_REGENERATION;
        }
        if (random.nextFloat() < GROUP_EFFECT_REROLL_CHANCE * armed) { // #4
            marker = GROUP_EFFECT_INVISIBILITY;
        }
        return marker;
    }

    /** {@code :87-92} — a GroupData with potion id &gt; 0 (and a non-null
     *  potion in the 1.7.10 id table) is applied with
     *  {@code new PotionEffect(id, Integer.MAX_VALUE)}. The {@code > 0} half is
     *  the "something was picked" test, which is the marker being set; the
     *  "non-null in the id table" half is the registry-resolvable test, passed
     *  in as a flag because 21.1.259 resolves effects through {@code Holder}s
     *  and a unit test has no registry. All four table entries are registered
     *  on this platform, so the shipped gate is the marker half alone.
     *  1.7.10's numeric ids are not 1.21.1's (the id table reordered), so the
     *  marker is the carrier and no numeric id crosses the seam — the reorder is
     *  the ledger row, the roll, the gate and the duration ship intact. */
    public static final int GROUP_EFFECT_MAX_AGE = Integer.MAX_VALUE;

    public static boolean appliesGroupEffect(final int marker, final boolean resolvable) {
        return marker > GROUP_EFFECT_NONE && resolvable;
    }

    /** The 1.7.10 id-table gate in its numeric form, kept for the probe that
     *  pins the {@code id > 0} half without a registry. */
    public static boolean appliesGroupPotion(final int potionId) {
        return potionId > 0;
    }

    // ---------------------------------------------------------------- tick body

    /** {@code :116-118} — for the first 20 ticks of life fall damage is zero:
     *  {@code if (age < 20) fallDistance = 0}. */
    public static final int FALL_IMMUNE_AGE_TICKS = 20;

    /**
     * {@code :120-129} — the mount (climb) gate. All conditions must hold:
     * <ol>
     *   <li>the crab rides nothing ({@code field_70154_o == null}),</li>
     *   <li>a nearest player exists ({@code func_70643_av()} =
     *       {@code getClosestPlayerToMe()}: the nearest player to the mob,
     *       distance-unbounded),</li>
     *   <li>that player rides nobody ({@code field_70153_n == null} = the
     *       entity riding the player),</li>
     *   <li>the crab is NOT on the ground (mid-leap, {@code :123}),</li>
     *   <li>the crab wears no shell ({@code :124}),</li>
     *   <li>the player is alive ({@code :125}),</li>
     *   <li>the crab's feet are at least half the player's height above the
     *       player's feet ({@code :126}),</li>
     *   <li>the SQUARED distance to the player is under 4.0
     *       ({@code func_70068_e} = getDistanceSq, {@code :127}) — under two
     *       blocks.</li>
     * </ol>
     * Then {@code :128} mounts the player.
     */
    public static boolean canMountPlayer(final boolean crabRiding, final boolean playerFound,
            final boolean playerRidingSomeone, final boolean crabOnGround, final boolean hasShell,
            final boolean playerDead, final double crabY, final double playerY,
            final double playerHeight, final double distanceSquared) {
        return !crabRiding
                && playerFound
                && !playerRidingSomeone
                && !crabOnGround
                && !hasShell
                && !playerDead
                && crabY - playerY >= playerHeight / 2.0
                && distanceSquared < 4.0;
    }

    /** {@code :131-137} — server-only: while the crab rides something and the
     *  attack timer is down, it punches the mount
     *  ({@code func_70652_k(field_70154_o)}) and re-arms
     *  {@code attackTime = 10 + entityRandom.nextInt(10)} ({@code :132});
     *  with 20% chance it dismounts ({@code :134-135}). */
    public static final int PUNCH_COOLDOWN_BASE = 10;
    public static final int PUNCH_COOLDOWN_JITTER = 10;
    public static final float PUNCH_DISMOUNT_CHANCE = 0.2F;

    public static int punchCooldownFor(final int jitterRoll) {
        return PUNCH_COOLDOWN_BASE + jitterRoll;
    }

    public static boolean dismountsAfterPunch(final float roll) {
        return roll < PUNCH_DISMOUNT_CHANCE;
    }

    // ---------------------------------------------------------------- drops / attack

    /** {@code :144-149} — {@code dropFewItems(recentlyHit, looting)}:
     *  {@code recentlyHit && (nextInt(3) == 0 || nextInt(1 + looting) > 0)}
     *  drops one golden sword ({@code Items.field_151079_bi} =
     *  {@code items.golden_sword} — a real 1.21.1 item, so this branch is not
     *  gated). 1.21.1's {@code dropCustomDeathLoot} hook carries no looting
     *  level (BD-90's shape), so the roll reads LOOTING 0 and the
     *  {@code nextInt(1)} arm can never fire; the 1-in-3 arm stands. */
    public static final int GOLDEN_SWORD_ROLL_MODULO = 3;
    public static final int DROP_LOOTING_BASELINE = 0;
    public static final ResourceLocation GOLDEN_SWORD =
            ResourceLocation.withDefaultNamespace("golden_sword");

    public static boolean dropsGoldenSword(final boolean recentlyHit, final int rollMod3,
            final int rollLootingArm) {
        return recentlyHit && (rollMod3 == 0 || rollLootingArm > 0);
    }

    /** {@code :151-158} — on a successful melee hit the crab fires
     *  {@code thaumcraft:crabclaw} at volume 1.0, pitch
     *  {@code 0.9 + worldRandom.nextFloat() * 0.2}. The sound is unregistered
     *  (the audio lane owns the registry, D-004) and firing a missing id
     *  warns every hit, so the id is pinned here and NOT fired (the family's
     *  unregistered-sound rule; docs/sound-events.md). */
    public static final ResourceLocation SOUND_CLAW =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "crabclaw");
    public static final float CLAW_VOLUME = 1.0F;
    public static final float CLAW_PITCH_BASE = 0.9F;
    public static final float CLAW_PITCH_JITTER = 0.2F;

    /** {@code :188-198} — ambient {@code thaumcraft:crabtalk}, death
     *  {@code thaumcraft:crabdeath}: unregistered ids, pinned not fired.
     *  {@code :192-194} — hurt is {@code game.hostile.hurt}, the 1.7.10
     *  vanilla legacy path; its 1.21.1 successor
     *  ({@code minecraft:entity.hostile.hurt}) is the Monster default of the
     *  crab's {@code SoundType.HOSTILE}, so the hurt fires from vanilla with
     *  no override, and the step override rides {@code SoundType.SPIDER}
     *  ({@code mob.spider.step}, {@code :200-202}). */
    public static final ResourceLocation SOUND_AMBIENT =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "crabtalk");
    public static final ResourceLocation SOUND_DEATH =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "crabdeath");
    public static final ResourceLocation SOUND_HURT =
            ResourceLocation.withDefaultNamespace("entity/hostile/hurt");
    public static final ResourceLocation SOUND_STEP =
            ResourceLocation.withDefaultNamespace("entity/spider/step");

    /** {@code :160-169} — after any applied damage, if the crab wears a shell
     *  and its health at or below half, the shell breaks: helm cleared,
     *  {@code itemChestCultistPlate} dropped, speed back to 0.3. The chest
     *  plate is unregistered content (content-mapping row for
     *  {@code thaumcraft:chestcultistplate}, {@code not_started}), so the drop
     *  is gated on the live registry (BD-68's shape). */
    public static final ResourceLocation CHEST_CULTIST_PLATE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "chestcultistplate");

    public static boolean shellBreaks(final boolean hasShell, final float health,
            final float maxHealth) {
        return hasShell && maxHealth > 0.0F && health / maxHealth <= 0.5F;
    }

    /** {@code :184-186} — {@code getMaxAirSupply()} is 160 (the crab is a
     *  land creature, the vanilla 300 does not apply). */
    public static final int MAX_AIR = 160;

    /** {@code :212-214} — {@code func_142014_c} (canAttack): the crab's whole
     *  body is {@code el instanceof EntityEldritchCrab}, replacing the vanilla
     *  checks, so a crab targets crabs and nothing else. 1.21.1's seam is
     *  {@code LivingEntity#canAttackTarget(LivingEntity)} — the pair
     *  {@code Mob#canAttackType} + {@code canAttack} of this build is
     *  targetability/type, not the original's predicate (the shipped precedent
     *  is {@code EntityTaintacle:295}). */
    public static boolean attacksOnlyCrabs(final boolean targetIsCrab) {
        return targetIsCrab;
    }

    /** {@code :204-206} — {@code EnumCreatureAttribute.ARTHROPOD} (the crab is
     *  smite- and bane-of-arthropods-able). 21.1.259 exposes no creature-attribute
     *  accessor on an entity, so the category is pinned as data and NOT
     *  expressed; the enchantments that read it sum their bonus ungated, the
     *  shipped posture at {@code EntityTaintacle:420-422}. */
    public static final boolean MOB_TYPE_ARTHROPOD_PINNED_NOT_EXPRESSED = true;

    /**
     * {@code :208-210} — {@code func_70687_e} is a one-rule predicate: a poison
     * effect never applies ({@code Potion.field_76436_u} = {@code poison},
     * mapping line 123; 1.21.1's carrier is the {@code MobEffects.POISON}
     * holder). {@code :160-166} — {@code func_70688_a(List)} adds the second
     * half: the {@code isSelfEffect} gate, the crab's own group-effect cast
     * (a mob {@code DataWatcher} has no effect field), never applies. The
     * platform has no mob-side applicability hook in 21.1.259, so both ride the
     * crab's plain predicate and the {@link #isSelfEffect} rule (BD rows).
     */
    public static final boolean POISON_IMMUNE = true;
    public static final String SELF_EFFECT_MARKER = " thaum998";

    public static boolean effectApplies(final boolean isPoison) {
        return !isPoison;
    }

    public static boolean isSelfEffect(final String effectName) {
        return effectName != null && effectName.contains(SELF_EFFECT_MARKER);
    }

    /** {@code :47-49} — sound volume: airborne → 0.5, grounded → 0.0. */
    public static float soundVolumeFor(final boolean airborne) {
        return airborne ? 0.5F : 0.0F;
    }

    /** {@code :101-112} — the Flags byte accessors: bit 0 set on,
     *  bit 0 cleared with {@code & ~1}. */
    public static byte shellOn(final byte flags) {
        return (byte) (flags | FLAG_BIT_SHELL);
    }

    public static byte shellOff(final byte flags) {
        return (byte) (flags & ~FLAG_BIT_SHELL);
    }

    public static boolean shellIn(final byte flags) {
        return (flags & FLAG_BIT_SHELL) != 0;
    }
}
