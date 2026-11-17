package com.jonnyutah12.thaumcraftclassic.entities.monster;

import java.util.List;

import javax.annotation.Nullable;

import com.jonnyutah12.thaumcraftclassic.api.entities.IEldritchMob;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AILongRangeAttack;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityEldritchOrb;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem;
import com.jonnyutah12.thaumcraftclassic.network.payload.MiscEventPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchCrab;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.OuterLands;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * {@code thaumcraft:eldritchguardian} — original
 * {@code thaumcraft.common.entities.monster.EntityEldritchGuardian} (310 lines,
 * content-mapping {@code Thaumcraft:EldritchGuardian}, kind=entity, mod_index
 * 107, display name "Eldritch Guardian", CSV line 323), {@code extends
 * EntityMob implements IRangedAttackMob, IEldritchMob}. Registration
 * {@code ConfigEntities.java:107} — {@code registerModEntity(
 * EntityEldritchGuardian.class, "EldritchGuardian", 107, instance, 64, 3,
 * true)}.
 *
 * <p>The gate-warden of the eldritch gates: 50 health, 7 attack, armour 4,
 * undead, it ignites its melee targets on a difficulty-scaled roll, fires
 * homing-free eldritch orbs from a 24-block ranged goal 90% of the time
 * and the sonic blast (wither + warp) the other 10%, halves magic damage,
 * heals by half its max health when it spawns in the Outer Lands and
 * regenerates there, refuses to spawn within 32/16/32 of another guardian, and
 * whispers its presence to nearby overworld players every 100 ticks. Every
 * figure lives on {@link EldritchGuardianAiLogic}; this class only wires.</p>
 *
 * <p>Per the family's mob rule (docs/mobs.md) this shell extends
 * {@link Mob}-of-{@code Monster} and NOT {@code thaumcraft.common.entities.monster.EntityMob}:
 * the 1.7.10 parent's swiftness-on-hard, raid, walking-animation, ground sound
 * and difficulty-scaled armour blocks are not inherited by 1.21.1's
 * {@code Monster}, and the guardian's own armour is its {@code getArmor}
 * ({@code :84-86}), shipped as the ARMOR attribute base. Size and eye height
 * ride {@code buildType}'s {@code sized(0.8, 2.25).eyeHeight(2.1)}.</p>
 *
 * <p>Sends this card does not fire, pinned on {@link EldritchGuardianAiLogic}:
 * the {@code FXSonic} broadcast and the client EG mote (FX lane,
 * docs/fx-events.md). The sounds ARE fired: {@code thaumcraft:egidle}/
 * {@code egdeath}/{@code egattack}/{@code egscreech} are registered by the
 * audio lane (TC-M2-06-s3), so they ride the {@code ModSounds} holders. The
 * whisper IS fired: {@code PacketMiscEvent(2)} is 1.21.1's
 * {@code MiscEventPayload(EVENT_ELDRITCH)} and the client arm is wired
 * (TC-M4-04).</p>
 *
 * <p>Deviations recorded in {@code docs/BEHAVIORAL_DEVIATIONS.md}: BD-117 (the
 * {@code :60}/{@code :61} slot-8 goal collision — BD-112's shape; {@code
 * func_70814_o} ({@code :263-265}) = {@code isAIEnabled} ships inert, which
 * also pins that the reference declares NO fire immunity; {@code
 * func_98052_bS} ({@code :92-94}) is the pickup rule (false); {@code
 * func_70692_ba} ({@code :243-245}) is the family's looting gate, so the idle
 * flag gates {@code shouldDropLoot} and NOT {@code checkDespawn}; the {@code
 * :185-187} rare drop has no 1.21.1 seam, BD-91's shape), BD-118 (the spawn
 * gate, the Outer-Lands heal/regen pair, the undead accessor (BD-121 of the
 * draft, re-minted) and the essence-drop aspect tags).</p>
 */
public class EntityEldritchGuardian extends Monster implements RangedAttackMob, IEldritchMob {

    /** {@code :189-206} — the home pair, modelled as fields the way
     *  {@code EntityCultist} models its own (the 1.7.10 home fields have no
     *  1.21.1 counterpart), with the original's four NBT keys. */
    @Nullable
    private BlockPos homePos;
    private int homeDist;

    /** {@code :50} — the blast alternation. */
    private boolean lastBlast;
    /** {@code :243-245} — the inert idle flag (BD-116). */
    private boolean idle;

    public EntityEldritchGuardian(final EntityType<EntityEldritchGuardian> type, final Level level) {
        super(type, level);
        this.xpReward = EldritchGuardianAiLogic.XP;                                   // :66
        // :92-94 — func_98052_bS() (getCanPickUpLoot) returns FALSE: the
        // guardian picks up nothing. 1.21.1 reads the FIELD (Mob#canPickUpItem,
        // :1235) — the seam the cultist family pinned as
        // CultistAiLogic.CAN_PICK_UP_LOOT — so the field rides (BD-117).
        this.setCanPickUpLoot(EldritchGuardianAiLogic.CAN_PICK_UP_LOOT);
        // :54 — getNavigator().setAvoidsWater(true) is INERT on 1.21.1: the
        // GroundPathNavigation has no such setter (water avoidance lives in the
        // path evaluator, whose ground evaluator never accepts a water node), so
        // the original's explicit `true` is 1.21.1's default. The figure stays
        // pinned as NAVIGATOR_AVOIDS_WATER — the contract the probe quotes — and
        // no call is shipped (BD-117).
    }

    /** {@code ConfigEntities.java:107} — tracking 64 blocks (4 chunks) and
     *  update interval 3; {@code :65} sizes the frame, {@code :247-249} the eye. */
    public static EntityType.Builder<EntityEldritchGuardian> builder(final MobCategory category) {
        return EntityType.Builder.<EntityEldritchGuardian>of(EntityEldritchGuardian::new, category)
                .sized(EldritchGuardianAiLogic.WIDTH, EldritchGuardianAiLogic.HEIGHT)
                .eyeHeight(EldritchGuardianAiLogic.EYE_HEIGHT)
                .clientTrackingRange(EldritchGuardianAiLogic.TRACK_RANGE_CHUNKS)
                .updateInterval(EldritchGuardianAiLogic.UPDATE_INTERVAL);
    }

    public static EntityType<EntityEldritchGuardian> buildType(final MobCategory category) {
        return builder(category).build("eldritchguardian");
    }

    /** {@code :69-75} + the {@code getArmor} of {@code :84-86} as the ARMOR base. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, EldritchGuardianAiLogic.MAX_HEALTH)      // :71
                .add(Attributes.FOLLOW_RANGE, EldritchGuardianAiLogic.FOLLOW_RANGE)  // :72
                .add(Attributes.MOVEMENT_SPEED, EldritchGuardianAiLogic.MOVEMENT_SPEED) // :73
                .add(Attributes.ATTACK_DAMAGE, EldritchGuardianAiLogic.ATTACK_DAMAGE)   // :74
                .add(Attributes.ARMOR, EldritchGuardianAiLogic.ARMOR);                 // :84-86
    }

    // ------------------------------------------------------------------ goals

    /**
     * {@code :55-64}. 1.7.10's goal map is a {@code TreeMap} of priority to
     * goal and {@code func_75776_a} walks a taken slot upward, so the {@code
     * EntityAILookIdle} of {@code :61} — requested for the slot 8 the {@code
     * EntityAIWatchClosest} of {@code :60} already holds — lands on 9. The
     * guardian's net set is therefore SEVEN movement goals (slots 0, 2, 3, 5,
     * 7, 8, 9) plus THREE targets. 1.21.1's {@code GoalSelector} keys on the
     * goal, not the slot, so the port ships the pair at the original's
     * effective slots (WATCH 8, LOOK_IDLE 9) and the same ten — the census the
     * probe asserts. Recorded deviation BD-117 ({@link
     * EldritchGuardianAiLogic#GOAL_SLOT_WATCH}), BD-112's shape; the pair's
     * {@code canAttackClass} ({@code :307-309}, {@code el instanceof
     * IEldritchMob}) rides the goal's {@link TargetingConditions} selector,
     * the only 1.21.1 seam for a class test the vanilla goal cannot express.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(EldritchGuardianAiLogic.GOAL_SLOT_SWIM,
                new FloatGoal(this));                                                 // :55
        this.goalSelector.addGoal(EldritchGuardianAiLogic.GOAL_SLOT_RANGED,
                new AILongRangeAttack(this, EldritchGuardianAiLogic.RANGED_MIN_DISTANCE,
                        EldritchGuardianAiLogic.RANGED_SPEED,
                        EldritchGuardianAiLogic.RANGED_INTERVAL_MIN,
                        EldritchGuardianAiLogic.RANGED_INTERVAL_MAX,
                        EldritchGuardianAiLogic.RANGED_RANGE));                       // :56
        this.goalSelector.addGoal(EldritchGuardianAiLogic.GOAL_SLOT_MELEE,
                new MeleeAttackGoal(this, EldritchGuardianAiLogic.MELEE_SPEED,
                        EldritchGuardianAiLogic.MELEE_LONG_MEMORY));                   // :57
        this.goalSelector.addGoal(EldritchGuardianAiLogic.GOAL_SLOT_RESTRICTION,
                new MoveTowardsRestrictionGoal(this,
                        EldritchGuardianAiLogic.RESTRICTION_SPEED));                   // :58
        this.goalSelector.addGoal(EldritchGuardianAiLogic.GOAL_SLOT_WANDER,
                new RandomStrollGoal(this, EldritchGuardianAiLogic.WANDER_SPEED));     // :59
        this.goalSelector.addGoal(EldritchGuardianAiLogic.GOAL_SLOT_WATCH,
                new LookAtPlayerGoal(this, Player.class,
                        EldritchGuardianAiLogic.WATCH_PLAYER_RANGE,
                        EldritchGuardianAiLogic.WATCH_PLAYER_PROBABILITY));            // :60
        this.goalSelector.addGoal(EldritchGuardianAiLogic.GOAL_SLOT_LOOK_IDLE_PORT,
                new RandomLookAroundGoal(this));                                      // :61
        this.targetSelector.addGoal(EldritchGuardianAiLogic.TARGET_SLOT_HURT,
                new HurtByTargetGoal(this));                                          // :62
        this.targetSelector.addGoal(EldritchGuardianAiLogic.TARGET_SLOT_PLAYER,
                new NearestAttackableTargetGoal<>(this, Player.class, true,
                        EldritchGuardianAiLogic.targetConditions()));                   // :63
        this.targetSelector.addGoal(EldritchGuardianAiLogic.TARGET_SLOT_CULTIST,
                new NearestAttackableTargetGoal<>(this, EntityCultist.class, true,
                        EldritchGuardianAiLogic.targetConditions()));                   // :64
    }

    // ------------------------------------------------------------- attributes

    /** {@code :267-269} — the sound volume is 1.5. */
    @Override
    protected float getSoundVolume() {
        return EldritchGuardianAiLogic.SOUND_VOLUME;
    }

    /** {@code :155-157} — {@code getMaxAirSupply() = 500}. */
    @Override
    public int getMaxAirSupply() {
        return EldritchGuardianAiLogic.MAX_AIR;
    }

    /** {@code :96-102} — magic damage is halved before the parent takes it. */
    @Override
    public boolean hurt(final net.minecraft.world.damagesource.DamageSource source, final float amount) {
        return super.hurt(source,
                EldritchGuardianAiLogic.damageAfterMagicHalve(
                        source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC), amount));
    }

    /**
     * {@code :135-145} — {@code func_70652_k(Entity)} ({@code attackEntity}):
     * after a landed hit, with an empty mainhand, on the ground, and a
     * {@code nextFloat() < difficultyId * 0.3} roll, the target ignites for
     * {@code 2 * difficultyId} seconds (the difficulty ordinal is 0 peaceful ..
     * 3 hard, so the fire is 0 below NORMAL). 1.21.1's seam is {@code
     * Mob#doHurtTarget} and NeoForge's {@code Entity#igniteForSeconds} (the
     * taintacle's shape, {@code EntityTaintacle.java:406}); the original's
     * {@code func_70015_d(int)} takes SECONDS, so the seconds ship untouched.
     */
    @Override
    public boolean doHurtTarget(final Entity target) {
        final boolean hit = super.doHurtTarget(target);                               // :136
        if (hit) {
            final int difficultyId = this.level().getDifficulty().getId();            // :137
            if (EldritchGuardianAiLogic.setsFire(difficultyId,
                    this.getMainHandItem().isEmpty(),                                 // :138 func_70694_bm()
                    this.onGround(),                                                  // :138 func_70027_ad()
                    this.random.nextFloat())) {
                target.igniteForSeconds(
                        EldritchGuardianAiLogic.fireSecondsFor(difficultyId));         // :139
            }
        }
        return hit;                                                                   // :141
    }

    /** {@code :263-265} — {@code func_70814_o()} ({@code isAIEnabled}, the
     *  1.7.10 per-mob AI-system switch) returns true. 1.21.1's GoalSelector is
     *  the only AI path, so the override has no analogue and ships inert
     *  (BD-117). The 310 lines declare NO fire immunity, so the guardian is
     *  not fire-immune on either engine: the port ships no {@code fireImmune()}
     *  override and no builder {@code fireImmune()} — pinned by {@code
     *  EldritchMobGameTest}, the same pinning the crab lane shipped (a later
     *  sweep that turns fire on has to turn that probe red). */

    /**
     * {@code :181-183} — {@code getCreatureAttribute() = UNDEAD}. 1.21.1 has no
     * mob-type concept (no {@code MobType} seam on {@code Mob}), so the trait
     * ships as the named accessor (BD-118, the crab's shape): queryable, with
     * no platform hook for the effects that keyed on it.
     */
    public boolean isUndead() {
        return true;
    }

    /** {@code :243-245} — {@code func_70692_ba()} (the looting gate the family
     *  pins at BD-92/BD-104) returns {@code !isEntityIdle()}: an idle guardian
     *  drops nothing. 1.21.1 has no entity-idle concept, so the flag ships as
     *  the named seam ({@link #isIdle()}, BD-116's shape re-minted) and the
     *  gate rides {@code LivingEntity#shouldDropLoot()} (:577 — the gate
     *  {@code dropAllDeathLoot} consults at :1465, BD-117). The original has
     *  no despawn override, so {@code checkDespawn} is NOT touched. */
    @Override
    protected boolean shouldDropLoot() {
        return EldritchGuardianAiLogic.dropsLoot(this.isIdle());                     // :243-245
    }

    public boolean isIdle() {
        return this.idle;
    }

    public void setIdle(final boolean idle) {
        this.idle = idle;
    }

    // ---------------------------------------------------------------- home

    /** {@code :189-196} — written only when a home exists and the range is
     *  positive ({@link EldritchGuardianAiLogic#writesHome}). */
    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (EldritchGuardianAiLogic.writesHome(this.homePos != null, this.homeDist)) {
            tag.putInt("HomeD", this.homeDist);                                       // :192
            tag.putInt("HomeX", this.homePos.getX());                                 // :193
            tag.putInt("HomeY", this.homePos.getY());                                 // :194
            tag.putInt("HomeZ", this.homePos.getZ());                                 // :195
        }
    }

    /** {@code :199-206} — read on the {@code HomeD} key's presence. */
    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeD")) {                                                  // :201
            setHomePosition(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"),
                    tag.getInt("HomeZ")), tag.getInt("HomeD"));                       // :202-204
        }
    }

    @Nullable
    public BlockPos getHomePosition() {
        return this.homePos;
    }

    public int getHomeDist() {
        return this.homeDist;
    }

    /** {@code :202-204}'s {@code setHomePosAndDistance(x, y, z, d)}. */
    public void setHomePosition(@Nullable final BlockPos pos, final int dist) {
        this.homePos = pos;
        this.homeDist = dist;
    }

    // --------------------------------------------------- outer-lands behaviour

    /**
     * {@code :208-217} — {@code onInitialSpawn}: in the Outer Lands the
     * guardian heals half its max health on top of its spawn health. The
     * original's {@code super.onInitialSpawn} return is passed through
     * untouched.
     */
    @Nullable
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            final net.minecraft.world.level.ServerLevelAccessor level,
            final DifficultyInstance difficulty, final MobSpawnType reason,
            @Nullable final net.minecraft.world.entity.SpawnGroupData spawnData) {
        final net.minecraft.world.entity.SpawnGroupData data =
                super.finalizeSpawn(level, difficulty, reason, spawnData);
        healAtSpawn();
        return data;
    }

    /** The spawn heal, split out as the probe seam. */
    public void healAtSpawn() {
        if (OuterLands.isOuterLands(this.level())) {
            this.heal(EldritchGuardianAiLogic.spawnHeal(
                    this.getAttributeValue(Attributes.MAX_HEALTH)));                  // :211-213
        }
    }

    /**
     * {@code :104-133} — {@code onLivingUpdate}. The client branch (the arm-lift
     * decay and the per-tick EG mote, {@code :107-117}) is the FX lane's send
     * with the client-only render state: the decay is shipped (the fields are
     * the renderer's contract), the mote is not. The server branch is the
     * overworld whisper, and {@code :219-227} is the Outer Lands regeneration.
     */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.armLiftL > 0.0F) {
                this.armLiftL -= EldritchGuardianAiLogic.ARM_LIFT_DECAY;              // :107-109
            }
            if (this.armLiftR > 0.0F) {
                this.armLiftR -= EldritchGuardianAiLogic.ARM_LIFT_DECAY;              // :111-113
            }
            // :115-117 — Thaumcraft.proxy.wispFXEG (the EG mote): the FX lane's
            // send; pinned as MOTE_JITTER / MOTE_Y_FACTOR, not fired.
            return;
        }
        regenerate();
        whisper();
    }

    /** {@code :219-227} — +1 health every 25 ticks below half, in the Outer
     *  Lands, outside the hurt-resistance window. */
    private void regenerate() {
        if (EldritchGuardianAiLogic.regenerates(OuterLands.isOuterLands(this.level()),
                this.invulnerableTime, this.tickCount, this.getHealth(),
                this.getAttributeValue(Attributes.MAX_HEALTH))) {
            this.heal(EldritchGuardianAiLogic.REGEN_PER_TICK);                        // :224
        }
    }

    /**
     * {@code :118-132} — outside the Outer Lands, on age 0 and every 100th
     * tick, off EASY, every survival player within {@code HARD ? 576 : 256}
     * SQUARED blocks gets {@code PacketMiscEvent((short) 2)} — whose client arm
     * ({@code PacketMiscEvent.java:49-52}) is {@code fogFiddled = true} with
     * {@code fogDuration = max(fogDuration, 200)}: the short eldritch fog. 1.21.1
     * sends the payload per player (the original's {@code sendTo(EntityPlayerMP)})
     * and the port's kind-2 arm is the shipped mist lane (TC-M4-04,
     * {@code EVENT_MIST_SHORT}), so the whisper plays as written.
     */
    private void whisper() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        final boolean hard = serverLevel.getDifficulty() == Difficulty.HARD;
        // :120 — func_70170_p.field_73013_u != EnumDifficulty.EASY: the ONLY
        // excluded difficulty. Peaceful is not excluded on 1.7.10 (a hostile
        // mob cannot live there anyway — Monster#checkDespawn discards on
        // peaceful), so no peaceful arm is added.
        if (!EldritchGuardianAiLogic.sendsWhisper(
                OuterLands.isOuterLands(this.level()), this.tickCount,
                serverLevel.getDifficulty() == Difficulty.EASY)) {
            return;
        }
        final double radiusSq = EldritchGuardianAiLogic.whisperRadiusSq(hard);         // :121
        for (final ServerPlayer player : serverLevel.players()) {
            // :125 — func_70089_S() (isPlayer): the loop is over the world's
            // player list, so the guard is inert on 1.7.10 and on 1.21.1
            // (serverLevel.players() is the same list); shipped as written, no
            // spectator filter added.
            if (player.distanceToSqr(this.getX(), this.getY(), this.getZ()) < radiusSq) { // :126-127
                PacketDistributor.sendToPlayer(player,
                        new MiscEventPayload(MiscEventPayload.EVENT_MIST_SHORT));     // :128
            }
        }
    }

    // ------------------------------------------------------------ spawn gate

    /**
     * {@code :251-261} — {@code getCanSpawnHere()}: the unit box at the feet
     * expanded 32 x 16 x 32; any guardian inside and the spawn is refused.
     * 1.21.1's seam is {@code checkSpawnRules} (the pech's shape,
     * {@code EntityPech.java:543}).
     */
    @Override
    public boolean checkSpawnRules(final LevelAccessor level, final MobSpawnType reason) {
        if (!super.checkSpawnRules(level, reason)) {                                  // :260
            return false;
        }
        if (!(level instanceof Level world)) {
            return true;
        }
        final BlockPos pos = this.blockPosition();
        final AABB box = new AABB(Vec3.atLowerCornerOf(pos),
                Vec3.atLowerCornerOf(pos.offset(1, 1, 1))).expandTowards(
                        EldritchGuardianAiLogic.SPAWN_BOX_XZ_EXPAND,
                        EldritchGuardianAiLogic.SPAWN_BOX_Y_EXPAND,
                        EldritchGuardianAiLogic.SPAWN_BOX_XZ_EXPAND);                 // :255-258
        return world.getEntitiesOfClass(EntityEldritchGuardian.class, box).isEmpty(); // :259-260
    }

    // -------------------------------------------------------------- client state

    /** {@code :48-49} — the renderer's arm-lift fields. */
    public float armLiftL = 0.0F;
    public float armLiftR = 0.0F;

    /**
     * {@code :229-241} — {@code handleEntityEvent(byte)}: 15 lifts the left
     * arm, 16 the right, 17 both, anything else goes to the parent. The
     * guardian broadcasts 15/16 alternating on every orb shot ({@code :274-275}).
     */
    @Override
    public void handleEntityEvent(final byte id) {
        if (id == EldritchGuardianAiLogic.EVENT_ARM_LEFT) {                           // :231-232
            this.armLiftL = EldritchGuardianAiLogic.ARM_LIFT_HALF;
        } else if (id == EldritchGuardianAiLogic.EVENT_ARM_RIGHT) {                    // :233-234
            this.armLiftR = EldritchGuardianAiLogic.ARM_LIFT_HALF;
        } else if (id == EldritchGuardianAiLogic.EVENT_ARM_BOTH) {                     // :235-237
            this.armLiftL = EldritchGuardianAiLogic.ARM_LIFT_FULL;
            this.armLiftR = EldritchGuardianAiLogic.ARM_LIFT_FULL;
        } else {
            super.handleEntityEvent(id);                                              // :238-239
        }
    }

    // ----------------------------------------------------------- ranged attack

    /**
     * {@code :271-305} — {@code attackEntityFromRangedEntity}
     * ({@code func_82196_d}): 90% of shots ({@link
     * EldritchGuardianAiLogic#firesOrb}) fire an {@link EntityEldritchOrb}
     * from the alternating arm muzzle, the other 10% (with line of sight) the
     * sonic blast.
     *
     * <p>The shot vector: the original's {@code doShockwave}
     * ({@code func_70186_c(x, y, z, 1.0F, 2.0F)}) is 1.21.1's
     * {@code ThrowableProjectile#shoot(dx, dy, dz, speed, inaccuracy)} — the
     * platform normalises the direction and applies the inaccuracy, so the
     * {@code :281-283} raw-difference vector ships untouched and no
     * normalisation is written here (the pech arrow's lesson: a hand-rolled
     * division by a zero distance lands NaN in the velocity, which 1.21.1
     * cannot encode).</p>
     */
    @Override
    public void performRangedAttack(final LivingEntity target, final float distanceFactor) {
        final Level level = this.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (EldritchGuardianAiLogic.firesOrb(this.random.nextFloat())) {              // :272
            final EntityEldritchOrb blast =
                    new EntityEldritchOrb(CatalogEldritchCrab.ELDRITCH_ORB.get(), level, this);
            this.lastBlast = !this.lastBlast;                                         // :274
            this.level().broadcastEntityEvent(this, this.lastBlast
                    ? EldritchGuardianAiLogic.EVENT_ARM_RIGHT
                    : EldritchGuardianAiLogic.EVENT_ARM_LEFT);                   // :275
            final int degrees = this.lastBlast
                    ? EldritchGuardianAiLogic.MUZZLE_DEGREES_RIGHT
                    : EldritchGuardianAiLogic.MUZZLE_DEGREES_LEFT;                    // :276
            final double rad = Math.toRadians((this.getYRot() + degrees) % 360.0F);   // :277/:279
            final double xx = Math.cos(rad) * EldritchGuardianAiLogic.MUZZLE_RADIUS;  // :277
            final double yy = EldritchGuardianAiLogic.MUZZLE_Y_FACTOR
                    * this.getBbHeight();                                             // :278
            final double zz = Math.sin(rad) * EldritchGuardianAiLogic.MUZZLE_RADIUS; // :279
            blast.setPos(blast.getX() - xx, blast.getY() - yy, blast.getZ() - zz);     // :280
            blast.shoot(target.getX() + target.getDeltaMovement().x() - this.getX(),    // :281
                    target.getY() - this.getY() - target.getBbHeight() / 2.0F,           // :282
                    target.getZ() + target.getDeltaMovement().z() - this.getZ(),         // :283
                    EldritchGuardianAiLogic.ORB_SPEED,
                    EldritchGuardianAiLogic.ORB_INACCURACY);                           // :284
            this.playSound(ModSounds.EGATTACK.get(), EldritchGuardianAiLogic.ATTACK_SOUND_VOLUME,
                    1.0F + this.random.nextFloat() * EldritchGuardianAiLogic.SOUND_PITCH_JITTER); // :285
            level.addFreshEntity(blast);                                              // :286
            return;
        }
        if (this.hasLineOfSight(target)) {                                            // :287
            sonicBlast(serverLevel, target);
        }
    }

    /**
     * {@code :287-304} — the sonic blast: the {@code FXSonic} broadcast (the
     * FX lane's send, pinned), WITHER 400 ticks amplifier 0 on the target, and
     * sticky warp {@code 1 + worldRandom(3)} when the target is a player.
     *
     * <p>1.7.10's {@code try/catch} around the potion write ships dropped:
     * {@code MobEffectInstance} construction cannot throw in 1.21.1 (the
     * effect is a registry holder, not an id lookup). The
     * {@code thaumcraft:egscreech} sound is the audio lane's id (D-004).</p>
     */
    private void sonicBlast(final ServerLevel serverLevel, final LivingEntity target) {
        // :288-292 — PacketFXSonic to all tracking players within 32 blocks: the
        // FX lane's send (docs/fx-events.md). The shape ships on the AiLogic.
        target.addEffect(new MobEffectInstance(MobEffects.WITHER,
                EldritchGuardianAiLogic.SONIC_WITHER_TICKS,
                EldritchGuardianAiLogic.SONIC_WITHER_AMPLIFIER));                     // :295
        if (target instanceof ServerPlayer player) {                                  // :299
            ResearchManager.applyWarp(player,
                    EldritchGuardianAiLogic.SONIC_WARP_BASE
                            + serverLevel.getRandom().nextInt(EldritchGuardianAiLogic.SONIC_WARP_JITTER),
                    SyncWarpPayload.WARP_STICKY);                                     // :300
        }
        this.playSound(ModSounds.EGSCREECH.get(), EldritchGuardianAiLogic.SCREECH_VOLUME,
                1.0F + serverLevel.getRandom().nextFloat()
                        * EldritchGuardianAiLogic.SOUND_PITCH_JITTER);                // :303
    }

    // ---------------------------------------------------------------- drops

    /**
     * {@code :163-179} — {@code dropFewItems}: two independent world-RNG coin
     * flips, the first dropping a wisp essence imbued with 2 UNDEAD, the second
     * with 2 ELDRITCH, thrown at offset 1.0. The {@code super} call keeps the
     * parent's custom-drop pass; the guardian picks up no loot ({@code :92-94})
     * so there is no equipment to keep.
     */
    @Override
    protected void dropCustomDeathLoot(final ServerLevel level,
            final net.minecraft.world.damagesource.DamageSource source,
            final boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (this.random.nextBoolean()) {                                              // :164
            spawnEssenceDrop(EldritchGuardianAiLogic.ASPECT_TAG_UNDEAD);              // :165-168
        }
        if (this.random.nextBoolean()) {                                              // :171
            spawnEssenceDrop(EldritchGuardianAiLogic.ASPECT_TAG_ELDRITCH);            // :172-175
        }
    }

    /**
     * The essence drop, split out as the probe seam: the item is the ported
     * {@code thaumcraft:itemwispessence} and the aspect resolves through the
     * port's aspect registry, the way {@code EntityWisp.createEssenceDrop}
     * (:411-421) does it.
     */
    public ItemStack createEssenceDrop(final String aspectTag) {
        final Aspect aspect = Aspect.getAspect(aspectTag);
        final ItemStack essence = new ItemStack(CatalogResources.ITEM_WISP_ESSENCE.get());
        if (aspect == null) {
            return ItemStack.EMPTY;
        }
        if (essence.getItem() instanceof IEssentiaContainerItem container) {
            container.setAspects(essence, new AspectList()
                    .add(aspect, EldritchGuardianAiLogic.WISP_ASPECT_AMOUNT));
        }
        return essence;
    }

    private void spawnEssenceDrop(final String aspectTag) {
        final ItemStack essence = createEssenceDrop(aspectTag);
        if (!essence.isEmpty()) {
            this.spawnAtLocation(essence, EldritchGuardianAiLogic.WISP_THROW_OFFSET);
        }
    }

    /**
     * {@code :185-187} — {@code dropRareDrop(int)}: one Eldritch Object (meta
     * 0, the Eldritch Eye). 1.7.10 calls it from the vanilla death pass at
     * 1-in-200; 1.21.1 has no rare-drop seam, so the method ships as the named
     * seam NOT wired (BD-117, BD-91's shape) and the probe drives it directly.
     */
    public ItemStack createRareDrop() {
        final net.minecraft.world.item.Item item =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        EldritchGuardianAiLogic.RARE_DROP_ITEM);
        if (item == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    // ------------------------------------------------------------------ sound

    /**
     * <p>{@code :147-149} — ambient {@code thaumcraft:egidle},
    *  {@code :151-153} death {@code thaumcraft:egdeath}: both ids are
    *  registered by the audio lane ({@code ModSounds.EGIDLE}/{@code EGDEATH},
    *  TC-M2-06-s3), so they play. The hurt is the vanilla default (no override
    *  in the 310 lines), and volume rides {@code :267-269}'s 1.5. */
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return ModSounds.EGIDLE.get();
    }

    /** {@code :151-153} — death {@code thaumcraft:egdeath}. */
    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return ModSounds.EGDEATH.get();
    }

    /** {@code :104-117}: the arm-lift pair is the renderer's contract. */
    public float getArmLiftL() {
        return this.armLiftL;
    }

    public float getArmLiftR() {
        return this.armLiftR;
    }

    public boolean getLastBlast() {
        return this.lastBlast;
    }
}
