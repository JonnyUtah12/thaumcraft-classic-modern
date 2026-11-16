package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The Eldritch Golem's figures — {@code thaumcraft.common.entities.monster.boss.EntityEldritchGolem}
 * (370 lines, {@code extends EntityThaumcraftBoss implements IEldritchMob, IRangedAttackMob}),
 * world-free so every one of them is pinned by {@code EldritchGolemAiLogicTest} without a level.
 *
 * <p>Everything the class writes as a literal lives here once: the registration row
 * ({@code ConfigEntities.java:119}), the 1.75 x 3.5 box ({@code :54}), the three attributes
 * ({@code :106-108}) over the boss base's pair, the goal table with its slots ({@code :46-53})
 * and the mid-fight goal the phase rule installs ({@code :186}), the melee figures
 * ({@code :189-211}), the beam figures ({@code :213-236}, {@code :346-358}), the phase rule
 * ({@code :170-183}), the footstep/crush gates ({@code :135-167}) and the client status ids
 * ({@code :238-279}).</p>
 */
public final class EldritchGolemAiLogic {

    private EldritchGolemAiLogic() {
    }

    // ------------------------------------------------- registration (:119-120, :54)

    /** {@code ConfigEntities.java:119} — {@code (…, 64, 3, true)}. */
    public static final int TRACK_RANGE_BLOCKS = 64;
    /** 64 blocks of tracking = 4 chunks (1.21.1's builder unit). */
    public static final int TRACK_RANGE_CHUNKS = 4;
    public static final int UPDATE_INTERVAL = 3;
    public static final boolean VELOCITY_UPDATES = true;

    /** {@code :54} — {@code func_70105_a(1.75F, 3.5F)}. */
    public static final float WIDTH = 1.75F;
    public static final float HEIGHT = 3.5F;

    /** {@code ConfigEntities.java:120} — {@code addMapping("EldritchGolem", 5592405, 4210752)}. */
    public static final int EGG_PRIMARY = 0x555555;
    public static final int EGG_SECONDARY = 0x404040;

    /**
     * {@code :111-121} — the sound trio: {@code mob.irongolem.hit} ({@code :112-114}),
     * {@code mob.irongolem.death} ({@code :115-117}), {@code mob.irongolem.walk}
     * ({@code :118-121}), each at 1.0F volume and 1.0F pitch. 1.7.10's
     * {@code func_70599_a} ({@code getHurtSound}) / {@code func_145780_a}
     * ({@code playSound(name, volume, pitch)}) pair to 21.1.259's {@code getHurtSound},
     * {@code getDeathSound} and {@code playStepSound} overrides (the family's idiom,
     * {@code EntityTaintacle:144-172}); 1.7.10's pinned 1.0F pitch is
     * {@code Entity#getVoicePitch()}, whose vanilla body jitters by 0.2F, so the pair is
     * overridden flat.
     */
    public static final String[] SOUND_TRIO = {"mob.irongolem.hit", "mob.irongolem.death",
            "mob.irongolem.walk"};
    public static final float SOUND_VOLUME = 1.0F;
    public static final float SOUND_PITCH = 1.0F;

    // ------------------------------------------------- attributes (:106-108, :100, :95-97)

    public static final double MOVEMENT_SPEED = 0.3;                               // :106
    public static final double ATTACK_DAMAGE = 10.0;                              // :107
    public static final double MAX_HEALTH = 250.0;                                // :108
    /** {@code :99-101} — {@code func_70658_aO()} = {@code super.getArmor() + 6}. */
    public static final double ARMOR_BONUS = 6.0;

    /**
     * {@code :95-97} — {@code func_70047_e()} ({@code getEyeHeight}): 3.0F, or 3.33F once the
     * golem is headless. 21.1.259's eye height is {@code Entity#getEyeHeight(Pose)}, which is
     * {@code final} and derives from the {@code EntityDimensions} of the box, so the original's
     * override has no seam: the pair ships as this port's own accessor (see the class's
     * {@code eldritchEyeHeight()}) and drives the explosion height of {@code :177}.
     */
    public static final float EYE_HEIGHT = 3.0F;
    public static final float EYE_HEIGHT_HEADLESS = 3.33F;

    /** {@code :55} — {@code field_70178_ae = true} ({@code setAIEnabled(true)}), 1.21.1's default. */
    public static final boolean AI_ENABLED = true;

    // ------------------------------------------------- goal table (:46-53, :186)

    public static final int GOAL_SLOT_SWIM = 0;                                   // :46
    public static final int GOAL_SLOT_MELEE = 3;                                  // :47
    public static final int GOAL_SLOT_RESTRICTION = 6;                            // :48
    public static final int GOAL_SLOT_WANDER = 7;                                 // :49
    public static final int GOAL_SLOT_WATCH = 8;                                  // :50
    public static final int GOAL_SLOT_LOOK_IDLE = 8;                              // :51
    public static final int TARGET_SLOT_HURT = 1;                                 // :52
    public static final int TARGET_SLOT_PLAYER = 2;                               // :53

    /**
     * {@code :186} — the slot of the goal the phase rule installs. The ranged goal is NOT in
     * the constructor's table: {@code makeHeadless()} is the only writer, so the golem fires
     * only after it has been beheaded ({@code :170-183}).
     */
    public static final int GOAL_SLOT_RANGED = 2;

    /** {@code :186} — {@code new AILongRangeAttack(this, 3.0, 1.0, 5, 5, 24.0F)}. */
    public static final double RANGED_MIN_DISTANCE = 3.0;
    public static final double RANGED_SPEED = 1.0;
    public static final int RANGED_INTERVAL_MIN = 5;
    public static final int RANGED_INTERVAL_MAX = 5;
    public static final float RANGED_RANGE = 24.0F;

    /** {@code :47} — {@code new AIAttackOnCollide(this, EntityLivingBase.class, 1.1, false)}. */
    public static final double MELEE_SPEED = 1.1;
    public static final boolean MELEE_LONG_MEMORY = false;

    public static final double RESTRICTION_SPEED = 0.8;                           // :48
    public static final double WANDER_SPEED = 0.8;                                // :49
    public static final float WATCH_PLAYER_RANGE = 8.0F;                          // :50
    /** {@code EntityAIWatchClosest}'s own 0.02 continue-chance, the family's shipped pair. */
    public static final float WATCH_PLAYER_PROBABILITY = 0.02F;

    /** {@code :52} — {@code new EntityAIHurtByTarget(this, true)}: 1.7.10's alert-others flag. */
    public static final boolean HURT_ALERTS_SAME_TYPE = true;
    public static final boolean TARGET_MUST_SEE = true;                           // :53

    /**
     * The movement goals the constructor's table installs ({@code :46-51}) — six writes, and
     * 1.21.1's {@code GoalSelector} keeps the slot-8 pair (BD-112's measured shape) so the
     * shipped count is six.
     */
    public static final int SHIPPED_MOVEMENT_GOALS = 6;
    /** {@code :52-53} — the two target goals. */
    public static final int SHIPPED_TARGET_GOALS = 2;

    // ------------------------------------------------- phase rule (:170-187, :123-127)

    /**
     * {@code :172} — the beheading gate: {@code damage > this.func_110143_aJ()} (the current
     * max health) on a golem that is not headless yet, server-side only ({@code :172} leads with
     * {@code !world.isRemote}). The threshold is the max health itself, so the multiplier is 1.
     */
    public static final float PHASE_DAMAGE_MULTIPLIER = 1.0F;

    /** {@code :125}, {@code :174} — {@code spawnTimer = 100} at spawn and at the beheading. */
    public static final int SPAWN_TIMER_PHASE_TICKS = 100;
    /** {@code :283} — the client's 150-tick value, which the server rebroadcasts as status 18. */
    public static final int SPAWN_TIMER_BROADCAST_TICKS = 150;
    /** {@code :287-289} — {@code if (getSpawnTimer() > 0) heal(2.0F)}, before the super call. */
    public static final float SPAWN_HEAL_AMOUNT = 2.0F;

    /** {@code :177} — {@code world.newExplosion(this, x + xx, y + getEyeHeight(), z + zz, 2.0F, false)}. */
    public static final float EXPLOSION_STRENGTH = 2.0F;
    public static final boolean EXPLOSION_DESTROY_BLOCKS = false;
    /** {@code :175-176} — the 0.75-block yaw offset pair of the explosion position. */
    public static final float EXPLOSION_OFFSET = 0.75F;

    // ------------------------------------------------- melee leg (:189-211)

    /** {@code :190-193} — the swing cadence: a 10-tick cooldown, written on the gate. */
    public static final int ATTACK_TIMER_TICKS = 10;
    /** {@code :196} — {@code attackDamage * 0.75F}. */
    public static final float MELEE_DAMAGE_RATIO = 0.75F;
    /** {@code :199} — {@code target.motionY += 0.2}. */
    public static final double MELEE_LIFT = 0.2;
    /** {@code :201-205} — the headless knockback: {@code addVelocity(-sin(yaw)*1.5, 0.1, cos(yaw)*1.5)}. */
    public static final float HEADLESS_KNOCKBACK_FORCE = 1.5F;
    public static final double HEADLESS_KNOCKBACK_LIFT = 0.1;

    // ------------------------------------------------- beam leg (:213-236, :346-358)

    /** {@code :214} — the firing gate: targetable, not charging, and charge above zero. */
    public static final int BEAM_MIN_CHARGE = 1;
    /** {@code :215} — {@code beamCharge -= 15 + random.nextInt(5)}. */
    public static final int BEAM_SPEND_BASE = 15;
    public static final int BEAM_SPEND_JITTER = 5;
    /** {@code :354-356} — the charge stops at exactly 150 (the field has no cap). */
    public static final int BEAM_CHARGE_TARGET = 150;
    /** {@code :216-223} — {@code getLookHelper().setLookPosition(x, minY + height/2, z, 30F, 30F)}. */
    public static final float AIM_ROTATION_SPEED = 30.0F;
    /** {@code :225} — {@code new EntityGolemOrb(world, this, target, false)}: the BLUE flag pair. */
    public static final boolean ORB_IS_RED = false;
    /** {@code :226-228} — the spawn nudge along the golem's own look vector, x/z only. */
    public static final float ORB_NUDGE_LOOK_SCALE = 1.0F;
    /** {@code :232} — {@code func_70186_c(d0, d1, d2, 0.66F, 5.0F)}. */
    public static final float ORB_SPEED = 0.66F;
    public static final float ORB_SPREAD = 5.0F;
    /** {@code :233} — {@code playSound("thaumcraft:egattack", 1.0F, 1.0F + rand*0.1F)}. */
    public static final float ORB_SOUND_VOLUME = 1.0F;
    public static final float ORB_SOUND_PITCH_BASE = 1.0F;
    public static final float ORB_SOUND_PITCH_JITTER = 0.1F;

    /** {@code :347-348} — {@code if (isHeadless() && beamCharge <= 0) chargingBeam = true}. */
    public static final int CHARGE_RESTART_CHARGE = 0;
    /** {@code :351-357} — the charge tick: {@code ++beamCharge}, broadcast 19, stop at 150. */
    public static final int CHARGE_STEP = 1;

    // ------------------------------------------------- footstep / crush legs (:135-167)

    /** {@code :135} — {@code motionX² + motionZ² > 2.5000003E-7F && random.nextInt(5) == 0}. */
    public static final double FOOTSTEP_MOTION_SQR = 2.5000003E-7F;
    public static final int FOOTSTEP_CHANCE_DIVISOR = 5;
    /** {@code :137} — the block row under the feet: {@code floor(y - 0.2F - height)}. */
    public static final float FOOTSTEP_DEPTH_BELOW_FEET = 0.2F;
    /** {@code :164} — {@code if (h >= 0.0F && h <= 0.15F) world.setBlockToAir(...)}. */
    public static final float CRUSH_HARDNESS_MIN = 0.0F;
    public static final float CRUSH_HARDNESS_MAX = 0.15F;

    // ------------------------------------------------- client status ids (:238-279, :281-359)

    /** {@code :194} / {@code :240-242} — status 4: the melee swing (client sets the 10-tick timer). */
    public static final byte STATUS_MELEE = 4;
    /** {@code :283-284} / {@code :243-244} — status 18: the 150-tick client spawn phase. */
    public static final byte STATUS_SPAWN_PHASE = 18;
    /** {@code :353} / {@code :245-275} — status 19: a charge tick, the arc anchor pick. */
    public static final byte STATUS_CHARGE = 19;

    /** {@code :242} — the swing sound of status 4: vanilla's golem attack, 1.0F / 1.0F. */
    public static final float STATUS_MELEE_VOLUME = 1.0F;
    public static final float STATUS_MELEE_PITCH = 1.0F;

    /** {@code :247-263} — the arc anchor pick of status 19. */
    public static final float ARC_RADIUS_BASE = 2.0F;
    public static final float ARC_RADIUS_JITTER = 2.0F;
    public static final int ARC_SCAN_STEPS = 5;
    public static final int ARC_TICKS_BASE = 8;
    public static final int ARC_TICKS_JITTER = 5;
    /** {@code :264-273} — {@code thaumcraft:jacobs} at 0.8F, pitch {@code 1.0F + (r - r) * 0.05F}. */
    public static final float ARC_SOUND_VOLUME = 0.8F;
    public static final float ARC_SOUND_PITCH_BASE = 1.0F;
    public static final float ARC_SOUND_PITCH_JITTER = 0.05F;

    // ------------------------------------------------- pure gates

    /** {@code :172} — the beheading rule: a hit above the current max health, once. */
    public static boolean shouldGoHeadless(final float damage, final float maxHealth,
            final boolean headless) {
        return damage > maxHealth * PHASE_DAMAGE_MULTIPLIER && !headless;
    }

    /** {@code :95-97} — the eye-height pair. */
    public static float eyeHeightFor(final boolean headless) {
        return headless ? EYE_HEIGHT_HEADLESS : EYE_HEIGHT;
    }

    /** {@code :175} — {@code cos(yaw % 360 / 180 * PI) * 0.75}. */
    public static double explosionOffsetX(final float yawDegrees) {
        return Mth.cos(yawDegrees % 360.0F / 180.0F * Mth.PI) * EXPLOSION_OFFSET;
    }

    /** {@code :176} — {@code sin(yaw % 360 / 180 * PI) * 0.75}. */
    public static double explosionOffsetZ(final float yawDegrees) {
        return Mth.sin(yawDegrees % 360.0F / 180.0F * Mth.PI) * EXPLOSION_OFFSET;
    }

    /** {@code :190-192} — the swing gate: the 10-timer has to run out first. */
    public static boolean meleeGate(final int attackTimer) {
        return attackTimer <= 0;
    }

    /** {@code :196} — the swing's damage: {@code attackDamage * 0.75F}. */
    public static float meleeDamageFor(final float attackDamage) {
        return attackDamage * MELEE_DAMAGE_RATIO;
    }

    /** {@code :202} — the headless knockback's x term: {@code -sin(yaw * PI / 180) * 1.5}. */
    public static double headlessKnockbackX(final float yawDegrees) {
        return -Mth.sin(yawDegrees * Mth.PI / 180.0F) * HEADLESS_KNOCKBACK_FORCE;
    }

    /** {@code :204} — the headless knockback's z term: {@code cos(yaw * PI / 180) * 1.5}. */
    public static double headlessKnockbackZ(final float yawDegrees) {
        return Mth.cos(yawDegrees * Mth.PI / 180.0F) * HEADLESS_KNOCKBACK_FORCE;
    }

    /** {@code :214} — the beam gate, all three arms. */
    public static boolean beamGate(final boolean targetable, final boolean chargingBeam,
            final int beamCharge) {
        return targetable && !chargingBeam && beamCharge >= BEAM_MIN_CHARGE;
    }

    /** {@code :215} — the spend: {@code 15 + random.nextInt(5)} (the roll is the caller's). */
    public static int beamSpendFor(final int roll) {
        return BEAM_SPEND_BASE + roll;
    }

    /** {@code :347-348} — the charge restarts when a headless golem has spent everything. */
    public static boolean chargeStarts(final boolean headless, final int beamCharge) {
        return headless && beamCharge <= CHARGE_RESTART_CHARGE;
    }

    /** {@code :351} — the charge advances only while headless and charging. */
    public static boolean chargeAdvances(final boolean headless, final boolean chargingBeam) {
        return headless && chargingBeam;
    }

    /** {@code :354-356} — the charge stops at 150 and the field is not capped beyond it. */
    public static boolean chargeStops(final int beamCharge) {
        return beamCharge == BEAM_CHARGE_TARGET;
    }

    /** {@code :135} — the footstep gate: real horizontal motion, one roll in five. */
    public static boolean footstepGate(final double motionX, final double motionZ, final int roll) {
        return motionX * motionX + motionZ * motionZ > FOOTSTEP_MOTION_SQR && roll == 0;
    }

    /** {@code :164} — the crush gate: a block of hardness in {@code [0.0, 0.15]} goes under. */
    public static boolean crushGate(final float hardness) {
        return hardness >= CRUSH_HARDNESS_MIN && hardness <= CRUSH_HARDNESS_MAX;
    }

    /** {@code :247} — the arc anchor's radius: {@code 2.0F + random * 2.0F}. */
    public static float arcRadiusFor(final float roll) {
        return ARC_RADIUS_BASE + roll * ARC_RADIUS_JITTER;
    }

    /** {@code :263} — the arc's life: {@code 8 + random.nextInt(5)}. */
    public static int arcTicksFor(final int roll) {
        return ARC_TICKS_BASE + roll;
    }

    // ------------------------------------------------- attributes

    /**
     * {@code :104-109} — {@code applyEntityAttributes}: speed 0.3, attack 10.0, max health 250.0
     * over the boss base's pass, which carries knockback resistance 0.95 and follow range 40.0
     * ({@code EntityThaumcraftBoss:53-54}). The armour bonus of {@code :100} is a 1.7.10
     * {@code getArmor()} override with no 1.21.1 seam, so it rides the armour attribute
     * (the pech's and the brainy zombie's idiom, {@code EntityPech:224}).
     */
    public static AttributeSupplier.Builder createAttributes() {
        return ThaumcraftBossAiLogic.onMonsterAttributes(
                        net.minecraft.world.entity.monster.Monster.createMonsterAttributes())
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)                       // :106
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)                         // :107
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)                               // :108
                .add(Attributes.ARMOR, ARMOR_BONUS);                                  // :100
    }
}
