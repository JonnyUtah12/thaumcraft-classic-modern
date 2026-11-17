package com.jonnyutah12.thaumcraftclassic.entities.monster;

/**
 * Level-free rule set for the firebat — {@code thaumcraft:firebat}
 * (content-mapping.csv {@code Thaumcraft:Firebat}, mod_index 22).
 *
 * <p>Original: {@code thaumcraft.common.entities.monster.EntityFireBat}
 * (404 lines), an {@code EntityMob} that flies <b>by hand</b>. It installs
 * <b>no goal and no navigation class</b>: the hanging / flight / steering body
 * is written directly inside {@code func_70626_be()} ({@code onEntityUpdate},
 * {@code :223-297}) and the motion halves of {@code func_70071_h_()}
 * ({@code Entity#onUpdate}, {@code :188-193}). It is on 1.7.10's new-AI path
 * — {@code func_70814_o()} (1.7.10 {@code isAIEnabled}, the repo's resolved
 * SRG name for that hook) returns <b>true</b> at {@code :401-403} — and still
 * installs nothing, so the target and the melee come from the two methods the
 * class supplies itself: {@code func_70782_k()} ({@code theEntityToAttack}
 * provider, {@code :371-373}) and {@code func_70785_a(Entity, float)}
 * ({@code attackEntity}, {@code :329-369}) with its own in-body gate.
 * {@code func_70619_bc()} ({@code :299-301}) only calls super.</p>
 *
 * <p>1.21.1 reaches {@code Mob.doHurtTarget} only from a goal, so the port
 * keeps the original's empty goal selector and drives that target provider and
 * that melee body from {@code customServerAiStep} — the observable behaviour
 * is reproduced without inventing goals the original never had.</p>
 *
 * <p>Every figure below quotes the decompiled line it was read from, and
 * {@code FirebatAiLogicTest} pins the rules world-free. The entity class only
 * supplies the world (blocks, players, level events, the explosion, sounds)
 * and asks these methods what to do.</p>
 */
public final class FirebatAiLogic {

    // ---------------------------------------------------------------- identity

    /** {@code func_70105_a(0.5F, 0.9F)} — the hitbox, {@code :30}. */
    public static final float WIDTH = 0.5F;
    public static final float HEIGHT = 0.9F;

    /** {@code func_70013_c(float)} returns {@code 1.0F} — {@code :45-47}: the
     * bat renders at full brightness, it is its own light source. */
    public static final float BRIGHTNESS = 1.0F;

    /** {@code func_70070_b(float)} returns {@code 15728880} ({@code 0xF03030})
     * — {@code :41-43}: the client-side brightness/particle tint. Carried as a
     * constant so the renderer card (TC-M7-03-s) reads the same figure. */
    public static final int TINT_RGB = 15728880;

    /** {@code func_70599_aP()} returns {@code 0.1F} — {@code :49-51}
     * ({@code getSoundVolume}). */
    public static final float SOUND_VOLUME = 0.1F;

    /** {@code func_70647_i()} returns {@code super * 0.95F} — {@code :53-55}. */
    public static final float SOUND_PITCH_FACTOR = 0.95F;

    /** {@code func_70639_aQ()}: while hanging, {@code nextInt(4) != 0} mutes
     * the ambient sound — {@code :57-59}. */
    public static final int AMBIENT_WHILE_HANGING_ROLL = 4;

    /** The vanilla sound names the original plays by name — {@code :58}
     * ({@code mob.bat.idle}), {@code :62} ({@code mob.bat.hurt}), {@code :66}
     * ({@code mob.bat.death}). */
    public static final String SOUND_IDLE = "mob.bat.idle";
    public static final String SOUND_HURT = "mob.bat.hurt";
    public static final String SOUND_DEATH = "mob.bat.death";

    // ------------------------------------------------------------------- flags

    /** The synched byte at index 16 ({@code :35-38}) carrying five bits. 1.21.1
     * assigns synched ids itself, so what is pinned is the byte and this bit
     * layout, not the index. */
    public static final int FLAG_HANGING = 0;    // :79-90
    public static final int FLAG_SUMMONED = 1;   // :92-105
    public static final int FLAG_EXPLOSIVE = 2;  // :107-118
    public static final int FLAG_DEVIL = 3;      // :120-135
    public static final int FLAG_VAMPIRE = 4;    // :137-148

    /** The save keys of that byte and of the summoner's damage bonus:
     * {@code "BatFlags"} / {@code "damBonus"} — :378-379, :384-385. */
    public static final String NBT_FLAGS = "BatFlags";
    public static final String NBT_DAM_BONUS = "damBonus";

    /** {@code func_110147_ax:75}: max health 5.0, or 15.0 while the devil bit
     * is set. */
    public static final float HEALTH_NORMAL = 5.0F;
    public static final float HEALTH_DEVIL = 15.0F;

    public static float healthFor(final boolean devil) {
        // :75  getIsDevil() ? 15.0 : 5.0
        return devil ? HEALTH_DEVIL : HEALTH_NORMAL;
    }

    /** {@code func_110147_ax:76}, {@code setIsSummoned:104} and
     * {@code setIsDevil:133}: attack 1.0 when not summoned, and
     * {@code (devil ? 3 : 2) + damBonus} when it is. The {@code setIsDevil}
     * copy writes {@code (par1 ? 3 : 2)} with {@code par1} already tested true
     * (:133), i.e. 3 for a summoned devil bat. */
    public static float attackDamageFor(final boolean summoned, final boolean devil,
            final int damBonus) {
        return summoned ? (devil ? 3.0F : 2.0F) + damBonus : 1.0F;
    }

    /** {@code Utils.getBit/setBit/clearBit}
     * ({@code thaumcraft.common.lib.utils.Utils}), used by all five flag
     * getters and setters. */
    public static boolean getBit(final int value, final int index) {
        return (value & (1 << index)) != 0;
    }

    public static int setBit(final int value, final int index) {
        return value | (1 << index);
    }

    public static int clearBit(final int value, final int index) {
        return value & ~(1 << index);
    }

    // ------------------------------------------------------------- environment

    /** {@code func_70636_d()} ({@code :154-160}): while wet
     * ({@code func_70026_G}) the bat takes 1.0 drowning damage
     * ({@code DamageSource.field_76369_e} = {@code drown}, mcp fields csv
     * line 3618) <em>before</em> calling super. */
    public static final float WET_DAMAGE = 1.0F;

    /** {@code func_70071_h_}: hanging zeroes all three motion axes and snaps
     * the y to {@code floor(posY) + 1 - height}; not hanging damps the vertical
     * motion by 0.6 (so after the platform's own gravity the bat sinks) —
     * {@code :188-193}. */
    public static final float GRAVITY_DAMP = 0.6F;
    public static final double HANGING_Y_OFFSET = 1.0;

    /** The hanging branch of {@code func_70626_be}: the support test is
     * {@code func_147445_c(floor(posX), (int)posY + 1, floor(posZ), false)}
     * (1.7.10 {@code canBlockSeeTheSky}); a failing test clears the flag and
     * fires level event {@code 1015} at the feet — {@code :226-229}. The same
     * event fires when a player closes in (:235-237). 1.21.1 removed
     * {@code canBlockSeeTheSky}/{@code canSeeSky}; the shipped seam is
     * {@code LevelReader.canSeeSkyFromBelowWater}, which reads the raw sky
     * light at the position (see {@code EntityFireBat}, documented seam). */
    public static final int HANGING_SUPPORT_EVENT = 1015;
    public static final int SUPPORT_SCAN_Y_OFFSET = 1;

    /** While hanging, {@code nextInt(200) == 0} writes the head yaw from
     * {@code nextInt(360)} — {@code :231-232}. */
    public static final int IDLE_HEAD_YAW_ROLL = 200;
    public static final int HEAD_YAW_SPAN = 360;

    /** While hanging, a player found within 4.0 ({@code func_72890_a})
     * un-hangs the bat — {@code :235}. */
    public static final float UNHANG_PLAYER_DISTANCE = 4.0F;

    /** Flying with no target and summoned: 2.0 generic damage
     * ({@code DamageSource.field_76377_j} = {@code generic}, mcp fields csv
     * line 3625) — {@code :241-243}. */
    public static final float SUMMONED_NO_TARGET_DAMAGE = 2.0F;

    // ---------------------------------------------------------------- flight

    /** The flight target is dropped when its block is not air
     * ({@code func_147437_c}) or its y is below 1 — {@code :246-253}. */
    public static final int FLIGHT_TARGET_MIN_Y = 1;

    /** Re-pick the waypoint when there is none, on {@code nextInt(30) == 0},
     * or when the current one is closer than 4.0 blocks
     * ({@code ChunkCoordinates.getDistance} {@code < 4.0F}) — :255-257. */
    public static final int FLIGHT_TARGET_REPICK_ROLL = 30;
    public static final float FLIGHT_TARGET_REPICK_DISTANCE = 4.0F;

    /** The waypoint: {@code (int)posX + nextInt(7) - nextInt(7)},
     * {@code (int)posY + nextInt(6) - 2}, {@code (int)posZ + nextInt(7) -
     * nextInt(7)} — {@code :258-262}. */
    public static final int WAYPOINT_AXIS_ROLL = 7;
    public static final int WAYPOINT_Y_ROLL = 6;
    public static final int WAYPOINT_Y_DROP = 2;

    /** The aim point inside the waypoint block: {@code +0.5} on x/z and
     * {@code +0.1} on y — {@code :265-267}. */
    public static final double WAYPOINT_XZ_CENTER = 0.5;
    public static final double WAYPOINT_Y_CENTER = 0.1;

    /** Steering: {@code motion + (signum(delta) * bound - motion) * 0.1} with
     * bounds 0.5 / 0.7 / 0.5 — {@code :267-269}, repeated verbatim by the
     * has-target branch at {@code :283-285}. */
    public static final float STEER_XZ = 0.5F;
    public static final float STEER_Y = 0.7F;
    public static final float STEER_LERP = 0.1F;

    /** {@code field_70701_bs = 0.5F} (the legacy walk/speed value) written by
     * both flight branches — {@code :273}, {@code :289}. It has no effect on
     * motion written directly, so the port carries it as a constant. */
    public static final float WALK_VALUE = 0.5F;

    /** {@code nextInt(100) == 0} and the sky is visible above the feet — the
     * bat hangs again — {@code :275-279}. */
    public static final int RE_HANG_ROLL = 100;

    /** The has-target branch aims at {@code target.posY +
     * target.getEyeHeight() * 0.66F} — {@code :281-282}. */
    public static final float TARGET_EYE_HEIGHT_FACTOR = 0.66F;

    /** {@code func_70782_k()} ({@code :371-374}): the closest <em>player</em>
     * within 12.0 ({@code world.func_72856_b(this, 12.0)}), and null while
     * summoned. */
    public static final float TARGET_DISTANCE = 12.0F;

    /** The target is dropped when it is a creative player
     * ({@code field_71075_bZ.field_75102_a}) — {@code :293-295}. */
    public static boolean clearsTarget(final boolean targetIsPlayer, final boolean targetIsCreative) {
        return targetIsPlayer && targetIsCreative;
    }

    public static float yawFromMotion(final double motionX, final double motionZ) {
        // :271  (atan2(motionY? no: atan2(motionZ, motionX)) * 180 / PI) - 90
        return (float) (Math.atan2(motionZ, motionX) * 180.0 / Math.PI) - 90.0F;
    }

    public static float yawDelta(final float targetYaw, final float currentYaw) {
        return wrapAngleTo180(targetYaw - currentYaw);
    }

    /** {@code MathHelper.func_76142_g} (1.7.10 {@code wrapAngleTo180_float}),
     * called at :272 and :288. */
    public static float wrapAngleTo180(float delta) {
        delta %= 360.0F;
        if (delta >= 180.0F) {
            delta -= 360.0F;
        }
        if (delta < -180.0F) {
            delta += 360.0F;
        }
        return delta;
    }

    public static float steer(final float motion, final double delta, final float bound) {
        return (float) (motion + (Math.signum(delta) * bound - motion) * STEER_LERP);
    }

    public static int waypointAxis(final int origin, final int rollA, final int rollB) {
        return origin + rollA - rollB;
    }

    public static int waypointY(final int origin, final int roll6) {
        return origin + roll6 - WAYPOINT_Y_DROP;
    }

    /** The waypoint is invalid when its block is not air or its y is below 1. */
    public static boolean flightTargetInvalid(final boolean blockIsAir, final int targetY) {
        return !blockIsAir || targetY < FLIGHT_TARGET_MIN_Y;
    }

    public static boolean repicksFlightTarget(final boolean targetNull, final int roll30,
            final double distanceToTarget) {
        return targetNull || roll30 == 0 || distanceToTarget < FLIGHT_TARGET_REPICK_DISTANCE;
    }

    public static boolean hangsAgain(final int roll100, final boolean skyVisibleAboveFeet) {
        return roll100 == 0 && skyVisibleAboveFeet;
    }

    /** The hanging idle head yaw: returns the new head yaw, or -1 for "keep
     * the current one" ({@code :231-232}). */
    public static int idleHeadYaw(final int roll200, final int roll360) {
        return roll200 == 0 ? roll360 : -1;
    }

    /** The y the hanging body writes: {@code floor(posY) + 1 - height}
     * ({@code :190}). */
    public static double hangingY(final double posY, final float height) {
        return Math.floor(posY) + HANGING_Y_OFFSET - height;
    }

    /** The vertical motion the flying body writes: damped by 0.6
     * ({@code :192}). */
    public static double flyingMotionY(final double motionY) {
        return motionY * GRAVITY_DAMP;
    }

    // ------------------------------------------------------------------ melee

    /** The melee gate ({@code func_70785_a}, {@code :330-333}):
     * {@code attackTime <= 0}, {@code dist < max(2.5F, targetWidth * 1.1F)}
     * and the two bounding-box height overlaps. {@code dist} is the distance
     * the caller passes as {@code par2}. */
    public static final int ATTACK_COOLDOWN = 20;               // :346
    public static final float ATTACK_REACH_FLOOR = 2.5F;        // :331
    public static final float ATTACK_REACH_WIDTH_FACTOR = 1.1F; // :331

    public static float attackReach(final float targetWidth) {
        return Math.max(ATTACK_REACH_FLOOR, targetWidth * ATTACK_REACH_WIDTH_FACTOR);
    }

    public static boolean canMelee(final int attackTime, final float distance, final float targetWidth,
            final double targetBoxMaxY, final double ownBoxMinY,
            final double targetBoxMinY, final double ownBoxMaxY) {
        return attackTime <= 0
                && distance < attackReach(targetWidth)
                && targetBoxMaxY > ownBoxMinY
                && targetBoxMinY < ownBoxMaxY;
    }

    /** Summoned hits mark the target recently hit for 100 ticks
     * ({@code EntityUtils.setRecentlyHit(target, 100)}) — {@code :334-335}.
     * 1.21.1 has no {@code recentlyHit} counter; the nearest carrier is the
     * last-hurt-by bookkeeping, and the gap is a recorded deviation. */
    public static final int RECENTLY_HIT_TICKS = 100;

    /** Vampire: the owner gains regeneration for 26 ticks at amplifier 1 when
     * it does not already have it, and the bat heals 1.0 — {@code :338-344}
     * ({@code Potion.field_76428_l} = regeneration, {@code func_70691_i} =
     * heal). */
    public static final int VAMPIRE_REGEN_DURATION = 26;
    public static final int VAMPIRE_REGEN_AMPLIFIER = 1;
    public static final float VAMPIRE_HEAL = 1.0F;

    public static boolean appliesVampireRegen(final boolean ownerPresent,
            final boolean ownerAlreadyRegenerating) {
        return ownerPresent && !ownerAlreadyRegenerating;
    }

    /** {@code func_70097_a} ({@code :317-327}): damage is refused while dead
     * ({@code func_85032_ar}), from fire ({@code func_76347_k} =
     * {@code isFireDamage}) or from magic ({@code func_94541_c} =
     * {@code isMagicDamage}); a landed server-side hit un-hangs the bat first
     * ({@code :319-321}). */
    public static boolean acceptsDamage(final boolean isDead, final boolean fire, final boolean magic) {
        return !isDead && !fire && !magic;
    }

    /** Explosion branch ({@code :347-353}): fires when the bat is explosive
     * <em>or</em> on {@code nextInt(10) == 0} (the roll is the level's
     * {@code field_73012_v}), server-side only, and never for a devil. Radius
     * {@code 1.5F + (explosive ? damBonus * 0.33F : 0F)}; the target's
     * invulnerability ({@code field_70172_ad}) is zeroed first ({@code :348})
     * and the bat removes itself. The explosion is {@code isFlaming = false}
     * and {@code isBreakingBlocks = false} ({@code :351}), i.e. no fire and no
     * block damage. */
    public static final int EXPLOSION_ROLL = 10;
    public static final float EXPLOSION_BASE = 1.5F;
    public static final float EXPLOSION_DAM_BONUS_FACTOR = 0.33F;

    public static boolean blowsUp(final boolean explosive, final int roll10, final boolean devil,
            final boolean clientSide) {
        return (explosive || roll10 == 0) && !clientSide && !devil;
    }

    public static float explosionRadius(final boolean explosive, final int damBonus) {
        return EXPLOSION_BASE + (explosive ? damBonus * EXPLOSION_DAM_BONUS_FACTOR : 0.0F);
    }

    /** Exhaustion branch ({@code :354-355}): taken when the bat is not vampire
     * and a coin flip ({@code nextBoolean()}) comes up false; the target is
     * exhausted by 4 while summoned, 2 otherwise. */
    public static final int EXHAUSTION_SUMMONED = 4;
    public static final int EXHAUSTION_NORMAL = 2;

    public static boolean usesExhaustion(final boolean vampire, final boolean coin) {
        return !vampire && !coin;
    }

    public static int exhaustionFor(final boolean summoned) {
        return summoned ? EXHAUSTION_SUMMONED : EXHAUSTION_NORMAL;
    }

    /** The plain-melee branch ({@code :356-365}): the hit is dealt with
     * {@code func_70652_k} (1.7.10 {@code attackTargetEntityWithCurrentItem};
     * the 1.21.1 seam is {@code Mob.doHurtTarget(Entity)}), the target's motion
     * is saved (:357-359) and restored after the hit (:362-364) with
     * {@code isAirBorne} cleared (:362) — i.e. the knockback the vanilla hit
     * applies is cancelled. 1.21.1 has no {@code isAirBorne} field, so only
     * the motion restore survives. */
    public static final int MELEE_KNOCKBACK_CANCELLED = 1;

    /** The hit sound ({@code :367-368}): {@code mob.bat.hurt} at volume 0.5
     * and pitch {@code 0.9F + nextFloat() * 0.2F}, played at the bat. */
    public static final float ATTACK_SOUND_VOLUME = 0.5F;
    public static final float ATTACK_SOUND_PITCH_BASE = 0.9F;
    public static final float ATTACK_SOUND_PITCH_SPAN = 0.2F;

    public static float attackSoundPitch(final float roll) {
        return ATTACK_SOUND_PITCH_BASE + roll * ATTACK_SOUND_PITCH_SPAN;
    }

    // ------------------------------------------------------------------ spawn

    /** {@code func_70601_bi()} ({@code :388-395}): the light at the feet
     * (1.7.10 {@code getLightFromNeighbors}, {@code func_72957_l}) above
     * {@code nextInt(7)} refuses the spawn; otherwise the base check applies.
     * The original's {@code floor_double} on the three coordinates is the
     * 1.7.10 convention this repo keeps (no {@code (int)} casts). */
    public static final int SPAWN_LIGHT_ROLL = 7;

    public static boolean canSpawnAtLight(final int lightAtFeet, final int roll7) {
        return !(lightAtFeet > roll7);
    }

    /** The y the original reads the spawn light from:
     * {@code floor_double(boundingBox.minY)} — {@code :389}. */
    public static int spawnLightY(final double boxMinY) {
        return (int) Math.floor(boxMinY);
    }

    // --------------------------------------------------------------- the drop

    /** {@code func_146068_u()} ({@code getDropItem}, {@code :397-399}) returns
     * gunpowder ({@code Items.field_151016_H} = {@code gunpowder}, mcp fields
     * csv line 1618) when the bat is not summoned, and
     * {@code Item.func_150899_d(0)} — id 0, i.e. <b>nothing</b> — when it is.
     *
     * <p>The roll that turns that item into a world drop lives in 1.7.10's
     * <em>vanilla</em> {@code EntityLiving.dropAllLoot}, which is <b>not in
     * this repo's reference tree</b> (it holds only the Thaumcraft and Baubles
     * decompiles), so the port ships no drop body and no loot table for the
     * firebat: the item identity and the summoned rule are pinned here, and
     * the vanilla roll is a filed follow-up (see the card's
     * {@code left_out_of_scope}). Shipping a guessed rate would be a
     * fabrication.</p>
     */
    public static final String DROP_ITEM_ID = "minecraft:gunpowder";

    /** {@code func_146068_u()} returns {@code Item.func_150899_d(0)} = null
     * for a summoned bat, so it never drops the item. */
    public static boolean dropsItsItem(final boolean summoned) {
        return !summoned;
    }

    // --------------------------------------------------- documented non-shapes

    /** The booleans the original hard-codes, kept as pinned constants so a
     * probe and a unit test can name them. The SRG names are the ones this
     * repo has already resolved (see the mob-hook table in the port's notes);
     * each entry quotes the decompiled line.
     * <ul>
     *   <li>{@code IS_PUSHABLE} — {@code func_70104_M()} false at
     *       {@code :69-71} <b>and</b> {@code func_70041_e_()} false at
     *       {@code :303-305}; both hooks resolve to {@code isPushable()} in
     *       this port (same resolution as {@code EntityWisp:303}), and they
     *       agree here.</li>
     *   <li>{@code IS_BOSS} — {@code func_70650_aV()} false at
     *       {@code :150-152}; 1.21.1's default, nothing to wire.</li>
     *   <li>{@code IS_AI_ENABLED} — {@code func_70814_o()} <b>true</b> at
     *       {@code :401-403}: the goal system is on in 1.7.10 and the class
     *       installs nothing, which is why the port registers an empty goal
     *       selector and drives the target/melee itself.</li>
     *   <li>{@code ORIGINAL_NO_AI} — {@code func_145773_az()} true at
     *       {@code :313-315}, the hook this repo resolves to {@code isNoAi()}.
     *       It is deliberately <b>not</b> wired: 1.21.1's NoAi flag flips
     *       {@code isEffectiveAi()} and gates {@code travel()}, which would
     *       switch off the hand-written flight motion this card is proving.
     *       The flag's observable effect in the original (no goal ticking, no
     *       pathfinding) is already reproduced by the empty goal selector, and
     *       the gap is recorded here and in the docs.</li>
     *   <li>{@code NO_FALL_DAMAGE} — {@code func_70064_a(double,boolean)} has
     *       an empty body at {@code :310-311} (1.7.10 fall damage), so the
     *       port overrides {@code checkFallDamage(double,boolean,BlockState,
     *       BlockPos)} to a no-op.</li>
     *   <li>{@code SILENT_FOOTSTEPS} — {@code func_70069_a(float)} is empty at
     *       {@code :307-308} (the step sound), so {@code playStepSound} is
     *       silent.</li>
     *   <li>{@code FIRE_IMMUNE} — {@code field_70178_ae = true} at
     *       {@code :32} ({@code isImmuneToFire}), carried by
     *       {@code EntityType.Builder.fireImmune()}.</li>
     * </ul>
     * {@code func_70013_c(float)} = 1.0 ({@code :45}, brightness) and
     * {@code func_70070_b(float)} = 15728880 ({@code :41}, the client
     * tint) are render-side facts for TC-M7-03-s and carry no server seam. */
    public static final boolean IS_PUSHABLE = false;
    public static final boolean IS_BOSS = false;
    public static final boolean IS_AI_ENABLED = true;
    public static final boolean ORIGINAL_NO_AI = true;
    public static final boolean NO_FALL_DAMAGE = true;
    public static final boolean SILENT_FOOTSTEPS = true;
    public static final boolean FIRE_IMMUNE = true;

    /** The original's {@code EntityLivingBase} attack-time counter
     * ({@code field_70724_aR}): written 20 on a hit ({@code :346}) and
     * decremented once per living update. */
    public static final int ATTACK_TIME_INITIAL = 0;

    private FirebatAiLogic() {
    }
}
