package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AICultistHurtByTarget;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AIAltarFocus;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AILongRangeAttack;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityGolemOrb;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/**
 * The Crimson Cleric — {@code thaumcraft:cultistcleric} (content-mapping
 * {@code Thaumcraft:CultistCleric}, CSV line 326, mod_index 28, display name
 * "Crimson Cleric"), the original's
 * {@code thaumcraft.common.entities.monster.EntityCultistCleric} (180 lines,
 * {@code extends EntityCultist implements IRangedAttackMob,
 * IEntityAdditionalSpawnData}). Registration {@code ConfigEntities.java:113} —
 * {@code registerModEntity(..., 64, 3, true)}.
 *
 * <p>The cleric is the knight's shape plus its own two goals, its own ranged
 * attack, and the ritualist state:</p>
 * <ul>
 *   <li>health 30.0 ({@code :51}), max air 500 ({@code :177-179});</li>
 *   <li>goal slot 1 {@code AIAltarFocus} ({@code :35}) and slot 2
 *       {@code AILongRangeAttack(this, 2.0, 1.0, 20, 40, 24.0F)} ({@code :36});</li>
 *   <li>{@code performRangedAttack} ({@code :64-89}): on
 *       {@code nextFloat() > 0.66F} throw a <b>red</b> {@link EntityGolemOrb}
 *       at the target — nudged by half its own velocity, aimed with
 *       {@code setThrowableHeading(d0, d1 + 2.0, d2, 0.66F, 3.0F)} — otherwise
 *       fire three {@code EntitySmallFireball}s spread by
 *       {@code sqrt(distance) * 0.5F} on x and z only, from
 *       {@code posY + height/2 + 0.5};</li>
 *   <li>the ritualist bit ({@code :96-134}): watched byte index 16, bit 0,
 *       NBT key {@code "Flags"}, cleared by any damage that lands
 *       ({@code :115-122}), and the LOOTING gate {@code !getIsRitualist()}
 *       ({@code :92-94});</li>
 *   <li>the home anchor in the spawn payload ({@code :136-144}): three ints —
 *       the home x, y, z — read back as {@code setHomePosAndDistance(x, y, z, 8)};</li>
 *   <li>the client-side ritual stare at its altar ({@code :146-158}).</li>
 * </ul>
 *
 * <p>Deviations, each recorded in {@code docs/BEHAVIORAL_DEVIATIONS.md}:
 * the robe/boots equipment ids are unregistered items, so the writes ride a
 * live registry check (BD-68's shape); the {@code thaumcraft:chant} ambient
 * ({@code :173-175}) and the {@code thaumcraft:egattack} attack sound
 * ({@code :74}) are unregistered sound events owned by the audio lane
 * (docs/sound-events.md), so their ids are pinned on {@link CultistAiLogic} and
 * nothing is fired — firing a missing id writes a warning to the console on
 * every ambient tick; the {@code playAuxSFX(1009)} puff ({@code :79}) is a
 * 1.7.10 aux-sfx id with no 1.21.1 id space, so the particle send is the FX
 * lane's; and the watched-byte index 16 is pinned as the original's (1.21.1
 * allocates synched ids) with the payload — byte, bit 0, NBT key — kept
 * verbatim.</p>
 */
public class EntityCultistCleric extends EntityCultist implements RangedAttackMob, IEntityWithComplexSpawn {

    /** {@code :99} — {@code dataWatcher.addObject(16, (byte) 0)}: the flags byte. */
    private static final EntityDataAccessor<Byte> DATA_FLAGS =
            SynchedEntityData.defineId(EntityCultistCleric.class, EntityDataSerializers.BYTE);

    public EntityCultistCleric(final EntityType<? extends EntityCultistCleric> type, final Level level) {
        super(type, level);
    }

    /**
     * {@code ConfigEntities.java:113} — {@code registerModEntity(EntityCultistCleric.class,
     * "CultistCleric", 28, instance, 64, 3, true)}: tracking 64 blocks (4
     * chunks), interval 3, velocity updates on; the box is the base's 0.6 x 1.8.
     */
    public static EntityType<EntityCultistCleric> buildType(final MobCategory category) {
        return EntityType.Builder.<EntityCultistCleric>of(EntityCultistCleric::new, category)
                .sized(CultistAiLogic.WIDTH, CultistAiLogic.HEIGHT)
                .clientTrackingRange(CultistAiLogic.TRACK_RANGE_CHUNKS)
                .updateInterval(CultistAiLogic.UPDATE_INTERVAL)
                .setShouldReceiveVelocityUpdates(CultistAiLogic.VELOCITY_UPDATES)
                .build("cultistcleric");
    }

    public static AttributeSupplier.Builder createAttributes() {
        // :51 — maxHealth 30.0 over the base's three.
        return EntityCultist.createAttributes()
                .add(Attributes.MAX_HEALTH, CultistAiLogic.CLERIC_MAX_HEALTH);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS, (byte) 0);                                     // :99
    }

    /**
     * {@code :34-45} — the family goal set with the cleric's two extras at
     * slots 1 and 2. Slots 0 and 3-8 and both target slots are the knight's,
     * slot for slot and parameter for parameter (see
     * {@code EntityCultistKnight#registerGoals()} for the mapping of the two
     * 1.7.10-only goals).
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_SWIM, new FloatGoal(this));   // :34
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_ALTAR, new AIAltarFocus(this)); // :35
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_RANGED,
                new AILongRangeAttack(this, CultistAiLogic.RANGED_MIN_DISTANCE,
                        CultistAiLogic.RANGED_SPEED, CultistAiLogic.RANGED_INTERVAL_MIN,
                        CultistAiLogic.RANGED_INTERVAL_MAX, CultistAiLogic.RANGED_RANGE)); // :36
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_MELEE,
                new MeleeAttackGoal(this, CultistAiLogic.MELEE_SPEED,
                        CultistAiLogic.MELEE_LONG_MEMORY));                              // :37
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_DOOR_RESTRICT,
                new OpenDoorGoal(this, false));                                          // :38
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_DOOR_OPEN,
                new OpenDoorGoal(this, true));                                           // :39
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_RESTRICTION,
                new MoveTowardsRestrictionGoal(this, CultistAiLogic.RESTRICTION_SPEED));  // :40
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_WANDER,
                new RandomStrollGoal(this, CultistAiLogic.WANDER_SPEED));                 // :41
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_WATCH,
                new LookAtPlayerGoal(this, Player.class, CultistAiLogic.WATCH_PLAYER_RANGE,
                        CultistAiLogic.WATCH_PLAYER_PROBABILITY));                        // :42
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_LOOK_IDLE,
                new RandomLookAroundGoal(this));                                         // :43
        this.targetSelector.addGoal(CultistAiLogic.TARGET_SLOT_HURT,
                // Cleric:44 — AICultistHurtByTarget(this, true), the family goal:
                // same body as the knight's :34 (see the class javadoc).
                new AICultistHurtByTarget(this, CultistAiLogic.IS_AGRESSORS));  // :44
        this.targetSelector.addGoal(CultistAiLogic.TARGET_SLOT_PLAYER,
                new NearestAttackableTargetGoal<>(this, Player.class, true));            // :45
    }

    // ------------------------------------------------------------- equipment

    /**
     * {@code :55-62} — {@code func_82164_bB()} ({@code addRandomEquipment}):
     * the robe helmet/chest/legs go on unconditionally, and the cultist boots
     * ride a {@code 0.3F} roll on HARD, {@code 0.1F} below it ({@code :59}).
     * The cleric carries no weapon: this is the whole equipment body.
     */
    @Override
    protected void populateDefaultEquipmentSlots(final net.minecraft.util.RandomSource random,
            final DifficultyInstance difficulty) {
        equipFromTable(CultistAiLogic.clericRobe());                                     // :56-58
        final boolean hard = difficulty.isHard();                 // :59
        final float chance = hard ? CultistAiLogic.CLERIC_BOOTS_CHANCE_HARD
                : CultistAiLogic.CLERIC_BOOTS_CHANCE;
        if (random.nextFloat() < chance) {                                               // :59
            final ItemStack boots = stackFor(CultistAiLogic.BOOTS_CULTIST);
            if (!boots.isEmpty()) {
                this.setItemSlot(EquipmentSlot.FEET, boots);
            }
        }
    }

    // ----------------------------------------------------------- ranged attack

    /**
     * {@code :55-62} + {@code :60-61} reached through 1.21.1's spawn pass.
     *
     * <p>The platform's {@code Mob#finalizeSpawn} (target jar, decompiled
     * {@code Mob.java:1137-1204}) does NOT call {@code
     * populateDefaultEquipmentSlots}; every mob that carries equipment calls the
     * pair from its own override ({@code Vindicator.java:131-132}, {@code
     * Piglin.java:215-216}) in the original's order, slots first then
     * enchantments. The cleric follows that idiom, the knight's fix of the same
     * platform gap (BD-98) in the family's own class: 1.7.10's {@code
     * func_82170_bB} ran from the spawn chain, so without this override the
     * robe, the boots and the gold-nugget dice of the base would never run.</p>
     */
    @Override
    public SpawnGroupData finalizeSpawn(final ServerLevelAccessor level,
            final DifficultyInstance difficulty, final MobSpawnType spawnType,
            @Nullable final SpawnGroupData groupData) {
        final SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, groupData);
        final RandomSource random = level.getRandom();
        this.populateDefaultEquipmentSlots(random, difficulty);                 // :55-62
        this.populateDefaultEquipmentEnchantments(level, random, difficulty);   // :60-61 (base :77-83)
        return data;
    }

    /**
     * {@code :64-89} — {@code func_82196_d(EntityLivingBase, float)}
     * ({@code performRangedAttack}). The aim vector is
     * {@code (target.x - x, (targetBB.minY + target.height/2) - (y + height/2),
     * target.z - z)}; the original then plays its living sound (the chant,
     * {@code :68}) and rolls the branch.
     *
     * <p>The orb branch ({@code :69-76}) builds the <b>red</b> orb —
     * {@code new EntityGolemOrb(world, this, target, true)} — nudges its
     * position by half its own velocity on x and z (the original's own
     * arithmetic, {@code :71-73}), re-asserts that position and aims it with
     * {@code setThrowableHeading(d0, d1 + 2.0, d2, 0.66F, 3.0F)} ({@code :74}).
     * The {@code true} is the pinned constructor argument: the golem passes
     * {@code false} ({@code EntityEldritchGolem:225}) and the Leader passes
     * {@code true} ({@code EntityCultistLeader:191}).</p>
     *
     * <p>The fireball branch ({@code :77-88}) spreads by
     * {@code sqrtFloat(distanceFactor) * 0.5F} on x and z only — the y component
     * is the raw {@code d1} — fires three of them, and starts each at
     * {@code posY + height/2 + 0.5}. 1.21.1's {@code SmallFireball(Level,
     * LivingEntity, Vec3)} constructor places the projectile at the owner's
     * position and applies the motion, which is 1.7.10's
     * {@code EntitySmallFireball(World, EntityLivingBase, dX, dY, dZ)} body;
     * the height offset is applied on top of it, exactly as the original writes
     * {@code field_70163_u} after construction.</p>
     */
    @Override
    public void performRangedAttack(final LivingEntity target, final float distanceFactor) {
        final double dx = target.getX() - this.getX();
        final double dy = (target.getBoundingBox().minY + target.getBbHeight() / 2.0F)
                - (this.getY() + this.getBbHeight() / 2.0F);
        final double dz = target.getZ() - this.getZ();
        playLivingSound();                                                              // :68
        if (CultistAiLogic.firesOrb(this.random.nextFloat())) {                         // :69
            final EntityGolemOrb blast = new EntityGolemOrb(
                    CatalogGolems.GOLEM_ORB.get(), this.level(), this, target,
                    CultistAiLogic.CLERIC_ORB_IS_RED);                                  // :70
            final Vec3 motion = blast.getDeltaMovement();
            blast.setPos(blast.getX() + motion.x() / CultistAiLogic.ORB_NUDGE_FACTOR,    // :71
                    blast.getY(),                                                       // :73
                    blast.getZ() + motion.z() / CultistAiLogic.ORB_NUDGE_FACTOR);       // :72
            blast.shoot(dx, dy + CultistAiLogic.ORB_AIM_LIFT, dz,
                    CultistAiLogic.ORB_VELOCITY, CultistAiLogic.ORB_INACCURACY);        // :74
            // :75 — playSound("thaumcraft:egattack", 1.0F, 1.0F + rand*0.1F): the
            // event is the audio lane's (docs/sound-events.md, D-004). Firing an
            // unregistered id writes a console warning on every cast, so the id and
            // the volume/pitch ship as the pinned constants and nothing is played.
            this.level().addFreshEntity(blast);                                         // :76
        } else {
            final float spread = Mth.sqrt(distanceFactor) * CultistAiLogic.FIREBALL_SPREAD_FACTOR; // :78
            // :79 — world.playAuxSFX(1009, (int)x, (int)y, (int)z, 0): 1.7.10's
            // aux-sfx id space has no 1.21.1 counterpart, so the particle send is
            // the FX lane's (docs/fx-events.md). No substitute particle.
            for (int i = 0; i < CultistAiLogic.FIREBALL_COUNT; i++) {                   // :81
                final SmallFireball fireball = new SmallFireball(this.level(), this,
                        new Vec3(dx + this.random.nextGaussian() * spread, dy,
                                dz + this.random.nextGaussian() * spread));             // :82-84
                fireball.setPos(fireball.getX(),
                        this.getY() + this.getBbHeight() / 2.0F + CultistAiLogic.FIREBALL_HEIGHT_OFFSET,
                        fireball.getZ());                                               // :85
                this.level().addFreshEntity(fireball);                                  // :86
            }
        }
    }

    /** {@code :68} — the original's {@code func_71038_i()} plays the ambient
     *  sound; the family's ambient is the unregistered {@code thaumcraft:chant}
     *  (see {@link EntityCultist#getAmbientSound()}), so the call keeps its
     *  shape and produces no send. */
    public void playLivingSound() {
        // 1.21.1’s ambient send is Mob#playAmbientSound (:275); the family’s ambient
        // is the unregistered thaumcraft:chant, so getAmbientSound returns null and
        // the send is a no-op that keeps the original’s call shape.
    }

    // -------------------------------------------------------- ritualist state

    /** {@code :101-103} — {@code getIsRitualist()}: bit 0 of the flags byte. */
    public boolean getIsRitualist() {
        return CultistAiLogic.isRitualist(this.entityData.get(DATA_FLAGS));
    }

    /** {@code :105-113} — {@code setIsRitualist(boolean)}: {@code v | 1} / {@code v & -2}. */
    public void setIsRitualist(final boolean value) {
        this.entityData.set(DATA_FLAGS, CultistAiLogic.setRitualist(this.entityData.get(DATA_FLAGS), value));
    }

    /** {@code :92-94} — {@code func_70692_ba()} (the LOOTING gate) is
     *  {@code !getIsRitualist()}. Per the {@code EntityTaintacleGiant} rule the
     *  gate is NOT wired to 1.21.1's {@code shouldDropLoot()}, which gates the
     *  whole death-loot block: it gates the family's own rolls
     *  ({@link EntityCultist#rollCultistDrops}, {@link EntityCultist#grantLastDeathLoot}). */
    @Override
    protected boolean dropsLoot() {
        return CultistAiLogic.clericDropsLoot(getIsRitualist());
    }

    /** The {@code :92-94} gate as a probe seam: {@link #dropsLoot()} is
     *  protected, so the game test reads the gate through the class. */
    public boolean isLootingGateOpen() {
        return dropsLoot();
    }

    /**
     * {@code :115-122} — {@code func_70097_a(DamageSource, float)}: an
     * invulnerable cleric takes nothing; any damage that lands clears the
     * ritual, then defers to the parent.
     */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (this.isInvulnerableTo(source)) {                                           // :116
            return false;
        }
        this.setIsRitualist(false);                                                    // :118
        return super.hurt(source, amount);                                             // :119
    }

    /** {@code :124-134} — the flags byte round-trips through NBT key "Flags". */
    @Override
    public void addAdditionalSaveData(final CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putByte(CultistAiLogic.NBT_FLAGS, this.entityData.get(DATA_FLAGS));   // :133
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(DATA_FLAGS, compound.getByte(CultistAiLogic.NBT_FLAGS));   // :128
    }

    // ---------------------------------------------------------- spawn payload

    /**
     * {@code :136-139} — {@code writeSpawnData(ByteBuf)}: the home position as
     * THREE ints (x, y, z) and nothing else. 1.7.10's
     * {@code IEntityAdditionalSpawnData} is NeoForge's
     * {@code IEntityWithComplexSpawn}, whose buffer is the registry-friendly
     * form; the payload fields are pinned on
     * {@link CultistAiLogic#SPAWN_INT_COUNT} and {@link CultistAiLogic#SPAWN_HOME_DISTANCE}.
     */
    @Override
    public void writeSpawnData(final RegistryFriendlyByteBuf buffer) {
        final BlockPos home = getHomePosition();
        buffer.writeInt(home == null ? 0 : home.getX());
        buffer.writeInt(home == null ? 0 : home.getY());
        buffer.writeInt(home == null ? 0 : home.getZ());
    }

    /** {@code :141-144} — {@code readSpawnData}: the three ints back, applied as
     *  {@code setHomePosAndDistance(x, y, z, 8)}. */
    @Override
    public void readSpawnData(final RegistryFriendlyByteBuf buffer) {
        setHomePosition(new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt()),
                CultistAiLogic.SPAWN_HOME_DISTANCE);
    }

    // ---------------------------------------------------------- ritual stare

    /**
     * {@code :146-158} — {@code func_70071_h_()}: client-side only
     * ({@code worldObj.isRemote}) and only mid-ritual, the cleric turns its head
     * to the altar at {@code home + (0.5, 1.5, 0.5)} — pitch toward the target at
     * 10.0F per tick, yaw at the mob's rotation limit. 1.7.10's
     * {@code field_70125_A}/{@code field_70759_as} (rotationPitch /
     * rotationYawHead, verified against the fields CSV) are 1.21.1's
     * {@code setXRot}/{@code setHeadRot}, and {@code func_70646_bf()}
     * ({@code getRotationLimit}) is {@code getMaxHeadXRot()}.
     */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && getIsRitualist()) {                            // :147
            final BlockPos home = getHomePosition();                                    // :148
            if (home == null) {
                return;
            }
            applyRitualLook(home);                                                      // :149-156
        }
    }

    /**
     * {@code :149-156} — the stare itself, split out of {@code tick()} so a
     * server-side probe can drive it: {@code tick()}'s arm is gated on
     * {@code worldObj.isRemote} ({@code :147}) and the game-test harness runs a
     * dedicated server, so the gate is never true there (the Mind Spider
     * renderer's client-only arm is the same class of probe).
     *
     * <p>The two angles are the original's own radian maths, kept as the pure
     * function {@link CultistAiLogic#clericLookAngles(double, double, double)}
     * ({@code :152-154}, {@code (yaw, pitch, 0)}), and each is applied through
     * {@code Mth#rotateIfNecessary}: that is 1.21.1's twin of 1.7.10's
     * {@code func_70126_a(float, float, float)} ({@code updateRotation}) —
     * {@code target - clamp(degreesDifference(current, target), -step, step)},
     * the same wrapped-delta clamp. The pitch step is the original's
     * {@code 10.0F} ({@code :155}); the yaw step is the mob's rotation limit
     * {@code func_70646_bf()} = {@link #getMaxHeadXRot()} ({@code :156}).</p>
     */
    public void applyRitualLook(final BlockPos home) {
        final double dx = home.getX() + CultistAiLogic.RITUAL_LOOK_XZ_OFFSET - this.getX();
        final double dy = home.getY() + CultistAiLogic.RITUAL_LOOK_Y_OFFSET
                - (this.getY() + this.getEyeHeight());
        final double dz = home.getZ() + CultistAiLogic.RITUAL_LOOK_XZ_OFFSET - this.getZ();
        final Vec3 look = CultistAiLogic.clericLookAngles(dx, dy, dz);                  // :152-154
        this.setXRot(Mth.rotateIfNecessary(this.getXRot(), (float) look.y(),
                CultistAiLogic.RITUAL_LOOK_PITCH_STEP));                               // :155
        this.setYHeadRot(Mth.rotateIfNecessary(this.getYHeadRot(), (float) look.x(),
                this.getMaxHeadXRot()));                                               // :156
    }

    /** {@code :177-179} — {@code func_70627_aG()} ({@code getMaxAir}): 500. */
    @Override
    public int getMaxAirSupply() {
        return CultistAiLogic.CLERIC_MAX_AIR;
    }

    /** The cleric's own id, for probes that resolve the type from the live registry. */
    public static final ResourceLocation TYPE_ID =
            ResourceLocation.parse("thaumcraft:cultistcleric");

    /** {@code Monster}-typed alias for the attribute wiring line. */
    public static AttributeSupplier.Builder monsterAttributes() {
        return Monster.createMonsterAttributes();
    }
}
