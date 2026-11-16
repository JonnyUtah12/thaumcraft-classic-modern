package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import com.jonnyutah12.thaumcraftclassic.api.entities.IEldritchMob;
import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AIAttackOnCollide;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AILongRangeAttack;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchGuardian;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityEldritchOrb;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchCrab;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchGuardian;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;

/**
 * The Eldritch Warden — {@code thaumcraft:eldritchwarden} (content-mapping
 * {@code Thaumcraft:EldritchWarden}, mod_index 26, row 324),
 * {@code thaumcraft.common.entities.monster.boss.EntityEldritchWarden} (418 lines,
 * {@code extends EntityThaumcraftBoss implements IRangedAttackMob, IEldritchMob}
 * at {@code :43} — the boss base, NOT the taintacle-giant family the custody note
 * suggested; the hierarchy was measured in {@code reference/} first, as the card
 * directs). Registration {@code ConfigEntities.java:109} — {@code (64, 3, true)}.
 *
 * <p>The warden is the boss base plus the field frenzy, every figure on
 * {@link WardenAiLogic} and pinned by {@code WardenAiLogicTest}: the 1.5 x 3.5
 * box, door-opening pathing and 500-point experience ({@code :80}, {@code :69},
 * {@code :415-417}), the 200 / 0.33 / 10 attributes plus armour 4 and eye height
 * 3.1 ({@code :104-106}, {@code :127-129}, {@code :336-338}), the ten-row goal
 * table at the original slots ({@code :70-79}), the fifteen-title watched byte with
 * its NBT round-trip ({@code :44-60}, {@code :93-125}), the 150-tick materialisation
 * with the status-18 broadcast and the 66% heal ({@code :145-149}, {@code :328-334}),
 * the death swap: a killing hit flips {@code fieldFrenzy} and the 150-tick counter
 * ({@code :317-320}), during which the warden is invulnerable ({@code :309-310}),
 * its goals frozen ({@code :132-135}) and the field frenzy ring of airy-11 cells
 * expands from it ({@code :208-245}) after the one-shot teleport home
 * ({@code :247-306}). The 80% {@link EntityEldritchOrb} / 20% sonic-blast ranged
 * seam ({@code :340-380}), the drown/wither refusal ({@code :315}), the four
 * corner cells it walks airy-11 into ({@code :190-197}), the 25-tick regen to the
 * 66% cap ({@code :137-142}), the Guardian-faction refusal ({@code :403-405}) and
 * the {@code egidle}/{@code egdeath} pair ({@code :407-413}) ride the same
 * reference, cited at every site.</p>
 *
 * <p>Seams, recorded in the row's {@code known_differences} and the BD rows, never
 * faked: the champion title ({@code :84-91}) rides the unregistered
 * {@code tc.mobmod} attribute and 21.1.259 has no {@code generateName} seam, so a
 * fresh warden carries no custom name (the rule ships, the probe asserts the plain
 * name); the client FX ({@code wispFXEG} {@code :161-163}, the 33×
 * {@code smokeSpiral} {@code :164-180}, the 128 portal particles {@code :289-300})
 * is the FX lane's draw — the state the renderer reads ({@code :64-65},
 * {@code :153-159}) ships, the pixels do not; the three packets ({@code FXSonic}
 * {@code :357-361}, {@code FXBlockArc} {@code :224-229}, {@code FXBlockSparkle}
 * {@code :230-236}) are the FX lane's sends, the 0.3 roll shipping as the rule;
 * the frenzy swap pins health to 1.0F where 1.7.10 loitered at 0.0F behind the
 * base's empty death tick (21.1.259's death pass removes a 0-health mob at
 * deathTime 20, {@code LivingEntity.java:565-571}); the {@code EnderTeleportEvent}
 * ({@code :248-251}) has no 21.1.259 class — the platform twin
 * {@code EntityTeleportEvent.EnderEntity} rides ({@code EnderMan.java:319}); the
 * goal-frozen frenzy gate rides the family's melee-goal seam (BD-132's shape);
 * the corner-cell and ring writes ship server-side (1.7.10 ran them on both sides,
 * the client's local ghost write has no 21.1.259 counterpart, block sync covers
 * the client); the sonic blast's 1.7.10 {@code try/catch} ships dropped
 * ({@code MobEffectInstance} construction cannot throw — the guardian's pair).</p>
 */
public class EntityEldritchWarden extends EntityThaumcraftBoss
        implements RangedAttackMob, IEldritchMob {

    /**
     * {@code :112} — {@code addObject(16, (byte) 0)}: the watched title byte, index
     * 16 pinned on {@link WardenAiLogic} as data (1.21.1 allocates synched ids;
     * the leader's BD-101 shape).
     */
    private static final EntityDataAccessor<Byte> TITLE =
            SynchedEntityData.defineId(EntityEldritchWarden.class, EntityDataSerializers.BYTE);

    /** {@code :61-65} — the frenzy pair and the renderer's arm-lift fields. */
    private boolean fieldFrenzy = false;
    private int fieldFrenzyCounter = 0;
    private boolean lastBlast = false;
    public float armLiftL = 0.0F;
    public float armLiftR = 0.0F;

    public EntityEldritchWarden(final EntityType<? extends EntityEldritchWarden> type,
            final Level level) {
        super(type, level);
        this.xpReward = WardenAiLogic.EXPERIENCE;                                       // :415-417
        // :69 — func_70105_a's sibling: getNavigator().setCanOpenDoors(true). 1.21.1
        // carries the door flag on GroundPathNavigation (EntityEldritchCrab.java:104-106
        // is the shipped pattern).
        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation g) {
            g.setCanOpenDoors(WardenAiLogic.NAVIGATION_OPENS_DOORS);                    // :69
        }
    }

    /**
     * {@code ConfigEntities.java:109} — {@code registerModEntity(EntityEldritchWarden.class,
     * "EldritchWarden", 26, instance, 64, 3, true)}: tracking 64 blocks = 4 chunks,
     * update interval 3, velocity updates true; the box is {@code :80}'s 1.5 x 3.5
     * with the {@code :336-338} eye height 3.1.
     */
    public static EntityType<EntityEldritchWarden> buildType(final MobCategory category) {
        return EntityType.Builder.<EntityEldritchWarden>of(EntityEldritchWarden::new, category)
                .sized(WardenAiLogic.WIDTH, WardenAiLogic.HEIGHT)                      // :80
                .clientTrackingRange(WardenAiLogic.TRACK_RANGE_CHUNKS)                  // :109, 64
                .updateInterval(WardenAiLogic.UPDATE_INTERVAL)                          // :109, 3
                .setShouldReceiveVelocityUpdates(WardenAiLogic.VELOCITY_UPDATES)        // :109, true
                .build("eldritchwarden");
    }

    /** {@code :101-107} + {@code :127-129} — the base's pair plus 0.33 / 200.0 / 10.0 / armour 4. */
    public static AttributeSupplier.Builder createAttributes() {
        return WardenAiLogic.createAttributes();
    }

    /** {@code :336-338} — {@code func_70047_e() = 3.1F}: 1.21.1 derives it from the box. */
    @Override
    protected EntityDimensions getDefaultDimensions(final Pose pose) {
        return EntityDimensions.fixed(WardenAiLogic.WIDTH, WardenAiLogic.HEIGHT)        // :80
                .withEyeHeight(WardenAiLogic.EYE_HEIGHT);                               // :336-338
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TITLE, (byte) 0);                                                // :112
    }

    // ------------------------------------------------------------------ title (:93-125)

    /** {@code :93-95} — {@code getTitle()} reads the watched byte. */
    public int getTitle() {
        return this.entityData.get(TITLE).intValue();
    }

    /** {@code :97-99} — {@code setTitle(int)} writes it. */
    public void setTitle(final int title) {
        this.entityData.set(TITLE, (byte) title);
    }

    /** {@code :115-119} — the watched byte rides the NBT key {@code title}. */
    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(WardenAiLogic.NBT_TITLE, (byte) getTitle());                        // :118
    }

    /** {@code :121-125} — read back, defaulting to 0 on an absent key as 1.7.10's {@code getByte}. */
    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTitle(tag.getByte(WardenAiLogic.NBT_TITLE));                                 // :124
    }

    /**
     * {@code :83-91} — {@code generateName()}: the champion name fires ONLY with a
     * registered {@code tc.mobmod} attribute reading {@code >= 0}. The attribute is
     * unregistered content and 21.1.259 has no {@code generateName} seam (the
     * leader's shape), so no name is set at spawn and the lang label
     * {@code entity.thaumcraft.eldritchwarden} (the en_US.lang :80 literal
     * {@code "%s the %d"}) shows; the rule ships on
     * {@link WardenAiLogic#championNameApplies(int)} and a probe asserts the plain
     * name (deviation row).
     */
    public String championTitleFor(final int championModIndex) {
        if (!WardenAiLogic.championNameApplies(championModIndex)) {                     // :86
            return null;
        }
        return String.format(getEncodeName(), WardenAiLogic.titleAt(getTitle()), "");   // :87-89
    }

    /** The lang key {@code :88} formats ({@code "entity.Thaumcraft.EldritchWarden.name"}). */
    private String getEncodeName() {
        return this.getType().getDescriptionId();                                       // :88 StatCollector
    }

    // ------------------------------------------------------------------ goals (:70-79)

    /**
     * {@code :70-79} — the goal table, slot for slot, in the original's order:
     *
     * <pre>
     *   0  EntityAISwimming                        → FloatGoal
     *   2  AILongRangeAttack(3.0, 1.0, 20, 40, 24.0F)  → the family's TC goal class
     *   3  AIAttackOnCollide(EntityLivingBase, 1.1, false) → TC goal: MeleeAttackGoal + class filter
     *   5  EntityAIMoveTowardsRestriction(0.8)     → MoveTowardsRestrictionGoal
     *   7  EntityAIWander(1.0)                     → RandomStrollGoal
     *   8  EntityAIWatchClosest(Player, 8.0F)      → LookAtPlayerGoal (0.02 chance)
     *   8  EntityAILookIdle                        → RandomLookAroundGoal
     *   target 1 EntityAIHurtByTarget(this, false) → HurtByTargetGoal (the family's plain form)
     *   target 2 EntityAINearestAttackableTarget(Player, 0, true)
     *   target 3 EntityAINearestAttackableTarget(Cultist, 0, true)
     * </pre>
     *
     * <p>Slots {@code :75}/{@code :76} collide at priority 8: 1.7.10's TreeMap
     * netted the pair to the LookIdle alone; 1.21.1 keeps both at the slot (the
     * leader's measured pair, BD-112's shape) and the port installs both. The
     * {@code :72} melee goal is Thaumcraft's own class carrying the family's
     * goal-tick gate; 1.21.1's {@code Monster#registerGoals} constructor quartet
     * is stripped by not calling {@code super.registerGoals()} (the family idiom),
     * so the measured set is exactly this table.</p>
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(WardenAiLogic.GOAL_SLOT_SWIM, new FloatGoal(this));   // :70
        this.goalSelector.addGoal(WardenAiLogic.GOAL_SLOT_RANGED,
                new AILongRangeAttack(this, WardenAiLogic.RANGED_MIN_DISTANCE,
                        WardenAiLogic.RANGED_SPEED, WardenAiLogic.RANGED_INTERVAL_MIN,
                        WardenAiLogic.RANGED_INTERVAL_MAX, WardenAiLogic.RANGED_RANGE)); // :71
        this.goalSelector.addGoal(WardenAiLogic.GOAL_SLOT_MELEE,
                new AIAttackOnCollide(this, LivingEntity.class,
                        WardenAiLogic.MELEE_SPEED, WardenAiLogic.MELEE_LONG_MEMORY));   // :72
        this.goalSelector.addGoal(WardenAiLogic.GOAL_SLOT_RESTRICTION,
                new MoveTowardsRestrictionGoal(this, WardenAiLogic.RESTRICTION_SPEED)); // :73
        this.goalSelector.addGoal(WardenAiLogic.GOAL_SLOT_WANDER,
                new RandomStrollGoal(this, WardenAiLogic.WANDER_SPEED));                // :74
        this.goalSelector.addGoal(WardenAiLogic.GOAL_SLOT_WATCH,
                new LookAtPlayerGoal(this, Player.class, WardenAiLogic.WATCH_PLAYER_RANGE,
                        WardenAiLogic.WATCH_PLAYER_PROBABILITY));                       // :75
        this.goalSelector.addGoal(WardenAiLogic.GOAL_SLOT_LOOK_IDLE,
                new RandomLookAroundGoal(this));                                        // :76
        this.targetSelector.addGoal(WardenAiLogic.TARGET_SLOT_HURT,
                new HurtByTargetGoal(this));                                            // :77
        this.targetSelector.addGoal(WardenAiLogic.TARGET_SLOT_PLAYER,
                new NearestAttackableTargetGoal<>(this, Player.class,
                        WardenAiLogic.TARGET_MUST_SEE));                                // :78
        this.targetSelector.addGoal(WardenAiLogic.TARGET_SLOT_CULTIST,
                new NearestAttackableTargetGoal<>(this, EntityCultist.class,
                        WardenAiLogic.TARGET_MUST_SEE));                                // :79
    }

    // ------------------------------------------------------------------ spawn (:328-334)

    /**
     * {@code :328-334} — {@code onInitialSpawn}: the 150-tick materialisation, the
     * title roll, the 66% heal. The original runs them BEFORE {@code super}; the
     * family's BD-98 idiom (the leader at {@code EntityCultistLeader:219-229}) runs
     * the body after {@code super} (the base's {@code :72-75} 24-block home goes
     * first); all three legs are independent of it. The roll rides the entity's own
     * random, verbatim ({@code :331}).
     */
    @Override
    public SpawnGroupData finalizeSpawn(final ServerLevelAccessor level,
            final net.minecraft.world.DifficultyInstance difficulty,
            final MobSpawnType reason, final SpawnGroupData groupData) {
        final SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        this.setSpawnTimer(WardenAiLogic.SPAWN_TIMER_START_TICKS);                      // :330
        this.setTitle(this.random.nextInt(WardenAiLogic.TITLES.length));                // :331
        this.setHealth(WardenAiLogic.spawnHealthFor(this.getHealth(),
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                        .getValue()));                                                  // :332
        return data;
    }

    // ------------------------------------------------------------------ frenzy state (:61-62)

    /** {@code :61} — the frenzy latch. */
    public boolean isFieldFrenzy() {
        return this.fieldFrenzy;
    }

    /** {@code :62} — the frenzy countdown. */
    public int getFieldFrenzyCounter() {
        return this.fieldFrenzyCounter;
    }

    /** The probe seam: park the countdown ({@code :62}, measurement-only writer). */
    public void setFieldFrenzyCounterForTest(final int counter) {
        this.fieldFrenzyCounter = counter;
    }

    /** The probe seam: the counter start ({@code :319}). */
    public void startFieldFrenzy() {
        this.fieldFrenzy = true;                                                        // :318
        this.fieldFrenzyCounter = WardenAiLogic.FRENZY_START_TICKS;                     // :319
    }

    /**
     * {@code :132-135} — {@code func_70619_bc()}: the goal pass runs ONLY while the
     * frenzy counter is idle, over the base's materialisation gate. 1.21.1's goal
     * pass ({@code Mob#serverAiStep}) is final, so the composite rides the family's
     * shipped seam {@code EntityThaumcraftBoss#goalsMayTick()} (BD-132's shape,
     * consulted by {@code ai/boss/AIAttackOnCollide.canUse}).
     */
    @Override
    public boolean goalsMayTick() {
        return WardenAiLogic.goalsMayTick(getSpawnTimer(), this.fieldFrenzyCounter);    // :133 over the base's :70-80
    }

    // ------------------------------------------------------------------ tick (:145-206)

    /**
     * {@code :145-149} — the materialisation broadcast: while the spawn timer reads
     * 150 the status-18 goes out (the golem's {@code :283-284} shape). The
     * client half of {@code :152-181} (the arm-lift decay, the wisp pair, the
     * 33× smoke spiral) is the renderer/FX lane's: the arm-lift state ships here
     * ({@link #armLiftDecayClient()}), the pixels do not (deviation row).
     */
    @Override
    public void tick() {
        if (getSpawnTimer() == WardenAiLogic.SPAWN_TIMER_BROADCAST_TICKS) {             // :147
            this.level().broadcastEntityEvent(this, WardenAiLogic.STATUS_SPAWN);        // :148
        }
        super.tick();                                                                   // :151
        if (this.level().isClientSide) {
            armLiftDecayClient();                                                       // :152-159
            // :160-163 wispFXEG and :164-180 the 33 smokeSpiral are the FX lane's
            // client draw (docs/fx-events.md): state-only seam, not faked.
        }
    }

    /** {@code :153-159} — the client arm-lift decay, extracted for the probe. */
    public void armLiftDecayClient() {
        this.armLiftL = WardenAiLogic.armLiftAfterTick(this.armLiftL);                  // :153-155
        this.armLiftR = WardenAiLogic.armLiftAfterTick(this.armLiftR);                  // :157-159
    }

    /**
     * {@code :184-206} — {@code onLivingUpdate}'s body: the four corner cells walk
     * airy-11 (air cells only), and the server-side frenzy legs (the one-shot
     * teleport home at counter 150, then the ring pass every tick). 1.7.10 ran the
     * corner writes on BOTH sides — the client's local ghost setBlock has no
     * 1.21.1 counterpart and block sync covers the client, so the write ships
     * server-side (deviation row). The per-tick body rides {@code aiStep} per the
     * family's map.
     */
    @Override
    public void aiStep() {
        super.aiStep();                                                                 // :185
        if (!this.level().isClientSide) {                                               // :199 (the legs)
            final int i = net.minecraft.util.Mth.floor(this.getX());                    // :186
            final int j = net.minecraft.util.Mth.floor(this.getY());                    // :187
            final int k = net.minecraft.util.Mth.floor(this.getZ());                    // :188
            for (int l = 0; l < WardenAiLogic.CORNER_SCAN_CELLS; l++) {                 // :190
                final int cx = net.minecraft.util.Mth.floor(
                        this.getX() + WardenAiLogic.cornerOffsetX(l));                  // :191
                final int cz = net.minecraft.util.Mth.floor(
                        this.getZ() + WardenAiLogic.cornerOffsetZ(l));                  // :193
                final BlockPos cell = new BlockPos(cx, j, cz);                          // :192
                if (this.level().getBlockState(cell).isAir()) {                         // :194 func_147437_c
                    placeAiryPortal(this.level(), cell);                                // :195
                }
            }
            if (this.fieldFrenzyCounter > 0) {                                          // :199
                if (this.fieldFrenzyCounter == WardenAiLogic.FRENZY_START_TICKS) {      // :200
                    teleportHome();                                                     // :201
                }
                performFieldFrenzy(i, j, k);                                            // :204
            }
        }
        if (WardenAiLogic.regens(this.tickCount, this.hurtTime)) {             // :137
            final int cap = WardenAiLogic.regenCap(
                    this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                            .getValue());                                               // :138
            if (this.getHealth() < cap) {
                this.setHealth(this.getHealth() + WardenAiLogic.REGEN_AMOUNT);          // :140
            }
        }
    }

    /** {@code :195/:222} — the airy meta-11 write, flags 3 (client + neighbour updates). */
    private void placeAiryPortal(final Level level, final BlockPos pos) {
        final BlockState state = CatalogDevices.BLOCK_AIRY.get()
                .withVariant(WardenAiLogic.AIRY_PORTAL_META);                           // :195/:222
        level.setBlock(pos, state, 3);                                                  // flags 3
    }

    /**
     * {@code :208-245} — the ring pass: status 17 under tick 121 on every tenth,
     * the expanding circle of airy-11 cells (air cell, solid below), each scheduled
     * for its 250+rand(150) expiry update, the 0.3 arc / 0.7 sparkle packet roll
     * (the FX lane's sends — the rule ships, the packets do not), then the
     * {@code thaumcraft:zap} at 1.0 / 0.9+rand*0.1, and the countdown.
     */
    private void performFieldFrenzy(final int i, final int j, final int k) {
        final Level level = this.level();
        if (WardenAiLogic.broadcastsFieldFrenzy(this.fieldFrenzyCounter)
                && level instanceof ServerLevel serverLevel) {                          // :209
            level.broadcastEntityEvent(this, WardenAiLogic.STATUS_ARM_BOTH);            // :210
            final double radius = WardenAiLogic.frenzyRadius(this.fieldFrenzyCounter);  // :211
            final int d = WardenAiLogic.frenzyStep(this.fieldFrenzyCounter);            // :212
            for (int q = 0; q < WardenAiLogic.frenzyLoopLimit(d); q++) {                // :217
                final double radians = WardenAiLogic.frenzyRadians(q, d);               // :218
                final int x = i + WardenAiLogic.frenzyDeltaX(radius, radians);          // :219
                final int z = k + WardenAiLogic.frenzyDeltaZ(radius, radians);          // :220
                final BlockPos cell = new BlockPos(x, j, z);
                if (level.getBlockState(cell).isAir()                                   // :221 func_147437_c
                        && isSolidBelow(level, cell)) {                                 // :221 func_147445_c
                    placeAiryPortal(level, cell);                                       // :222
                    serverLevel.scheduleTick(cell, CatalogDevices.BLOCK_AIRY.get(),
                            WardenAiLogic.airyUpdateDelay(
                                    this.random.nextInt(WardenAiLogic.AIRY_UPDATE_JITTER_TICKS))); // :223
                    // :224-236 — PacketFXBlockArc on the 0.3 roll, the sparkle on the
                    // rest: the FX lane's send (docs/fx-events.md). The roll ships as
                    // WardenAiLogic.arcFires; no packet is faked.
                    WardenAiLogic.arcFires(this.random.nextFloat());
                }
            }
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.ZAP.get(), this.getSoundSource(), WardenAiLogic.ZAP_VOLUME,
                    WardenAiLogic.ZAP_PITCH_BASE
                            + this.random.nextFloat() * WardenAiLogic.ZAP_PITCH_JITTER); // :240-241
        }
        this.fieldFrenzyCounter--;                                                      // :244
    }

    /** {@code :221/:269} — 1.7.10's {@code func_147445_c} (solid face below): the platform twin. */
    private static boolean isSolidBelow(final Level level, final BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid();                              // :221
    }

    /**
     * {@code :247-306} — {@code teleportHome}: the teleport event (1.7.10's
     * {@code EnderTeleportEvent} has no 21.1.259 class; the platform twin
     * {@code EntityTeleportEvent.EnderEntity} rides, posted the way {@code
     * EnderMan.java:319} posts it — cancel respected, target honoured), the raw
     * position write ({@code :255-257} = {@code setPositionRaw}), the loaded-cell
     * test ({@code :262}), the 20-try search for solid-below / non-solid with the
     * ±{@code nextInt(8)} jitter, the set at the cell centre ({@code :279}), the
     * collision-box admission ({@code :280} = {@code Level#getEntityCollisions}),
     * the restore on failure, and on success the {@code mob.endermen.portal} pair
     * ({@code :302-303} = {@code SoundEvents.ENDERMAN_TELEPORT}). The 128 portal
     * particles ({@code :289-300}) are the FX lane's draw (deviation row).
     */
    protected void teleportHome() {
        final BlockPos home = this.getRestrictCenter();                                 // :249 func_110172_bL (family seam)
        final net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderEntity event =
                net.neoforged.neoforge.event.EventHooks.onEnderTeleport(this,
                        home.getX(), home.getY(), home.getZ());                         // :248-251
        if (event.isCanceled()) {                                                       // :251
            return;
        }
        final double d3 = this.getX();                                                  // :252
        final double d4 = this.getY();                                                  // :253
        final double d5 = this.getZ();                                                  // :254
        this.setPos(event.getTargetX(), event.getTargetY(), event.getTargetZ());    // :255-257
        boolean flag = false;                                                           // :258
        int i = net.minecraft.util.Mth.floor(this.getX());                              // :259
        final int j = net.minecraft.util.Mth.floor(this.getY());                        // :260
        int k = net.minecraft.util.Mth.floor(this.getZ());                              // :261
        if (this.level().isLoaded(new BlockPos(i, j, k))) {                             // :262 func_72899_e
            boolean flag1 = false;                                                      // :263
            int tries = WardenAiLogic.TELEPORT_TRIES;                                   // :264
            while (!flag1 && tries > 0) {
                final BlockState below = this.level().getBlockState(new BlockPos(i, j - 1, k)); // :267
                final BlockState cell = this.level().getBlockState(new BlockPos(i, j, k));      // :268
                if (WardenAiLogic.landingCellAccepts(below.isSolid(), cell.isSolid())) {// :269
                    flag1 = true;
                } else {                                                                // :271-274
                    i = WardenAiLogic.jitteredCell(net.minecraft.util.Mth.floor(this.getX()),
                            this.random.nextInt(WardenAiLogic.TELEPORT_JITTER_ROLL),
                            this.random.nextInt(WardenAiLogic.TELEPORT_JITTER_ROLL));
                    k = WardenAiLogic.jitteredCell(net.minecraft.util.Mth.floor(this.getZ()),
                            this.random.nextInt(WardenAiLogic.TELEPORT_JITTER_ROLL),
                            this.random.nextInt(WardenAiLogic.TELEPORT_JITTER_ROLL));
                    tries--;
                }
            }
            if (flag1) {                                                                // :278
                this.setPos(i + WardenAiLogic.TELEPORT_CENTER_OFFSET,
                        j + WardenAiLogic.TELEPORT_Y_OFFSET,
                        k + WardenAiLogic.TELEPORT_CENTER_OFFSET);                      // :279
                if (this.level().getEntityCollisions(this, this.getBoundingBox())
                        .isEmpty()) {                                                   // :280
                    flag = true;
                }
            }
        }
        if (!flag) {                                                                    // :286-287
            this.setPos(d3, d4, d5);
            return;
        }
        // :289-300 — the 128 portal particles: the FX lane's draw (docs/fx-events.md).
        this.level().playSound(null, d3, d4, d5, SoundEvents.ENDERMAN_TELEPORT,
                this.getSoundSource(), 1.0F, 1.0F);                                     // :302
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);                      // :303
    }

    // ------------------------------------------------------------------ damage (:308-326)

    /**
     * {@code :308-311} — {@code func_85032_ar()}: invulnerable through the frenzy,
     * over the base's materialisation pair (1.21.1's seam is
     * {@code isInvulnerableTo(DamageSource)}, the family's map).
     */
    @Override
    public boolean isInvulnerableTo(final DamageSource source) {
        return super.isInvulnerableTo(source)
                || WardenAiLogic.invulnerableForFrenzy(this.fieldFrenzyCounter);        // :310
    }

    /**
     * {@code :313-326} — {@code func_70097_a}: no damage while invulnerable, and
     * NONE from drown ({@code field_76369_e}) or wither ({@code field_82727_n}) —
     * the pair resolved as {@code RunicShielding.isBlockedSource} resolves them
     * (BOSS_PREP §7's "anvil/cactus" guess superseded, recorded). The damage that
     * lands pays the boss base's damage leg; the death swap
     * ({@code :317-320}) runs from {@link #die(DamageSource)} per the base's
     * contract (the swap cannot ride {@code hurt}: 21.1.259 ends the health in
     * {@code super.hurt} before this body would see it).
     */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (WardenAiLogic.refusesSource(this.isInvulnerableTo(source),
                source == this.level().damageSources().drown(),
                source == this.level().damageSources().wither())) {                     // :315
            return false;
        }
        return super.hurt(source, bossDamageLeg(source, amount));                       // :316
    }

    /**
     * {@code :317-320} — the death swap: the first killing blow flips the frenzy
     * latch, starts the 150-tick counter and CANCELS the death (no {@code super},
     * so 21.1.259's death pass never begins); the warden rides the health pin of
     * {@link WardenAiLogic#FRENZY_SWAP_HEALTH} where the original loitered at 0.0F
     * behind the base's empty 1.7.10 death tick (deviation row). The second
     * killing blow (post-frenzy) takes the vanilla death.
     */
    @Override
    public void die(final DamageSource source) {
        if (!this.isRemoved() && WardenAiLogic.frenzySwaps(true, this.fieldFrenzy,
                this.getHealth())) {                                                    // :317
            this.setHealth(WardenAiLogic.FRENZY_SWAP_HEALTH);                           // deviation pin
            startFieldFrenzy();                                                         // :318-319
            return;
        }
        super.die(source);
    }

    // ------------------------------------------------------------------ status (:382-396)

    /**
     * {@code :382-396} — {@code func_70103_a(byte)}: 15 lifts the left arm, 16 the
     * right, 17 both, 18 re-arms the materialisation timer, else the parent. The
     * golem's {@code :238-279} shape (21.1.259's twin is
     * {@code handleEntityEvent(byte)}, shipped-side, the family's map).
     */
    @Override
    public void handleEntityEvent(final byte id) {
        if (id == WardenAiLogic.STATUS_ARM_LEFT) {                                      // :384-385
            this.armLiftL = WardenAiLogic.ARM_LIFT_HALF;
        } else if (id == WardenAiLogic.STATUS_ARM_RIGHT) {                              // :386-387
            this.armLiftR = WardenAiLogic.ARM_LIFT_HALF;
        } else if (id == WardenAiLogic.STATUS_ARM_BOTH) {                               // :388-390
            this.armLiftL = WardenAiLogic.ARM_LIFT_FULL;
            this.armLiftR = WardenAiLogic.ARM_LIFT_FULL;
        } else if (id == WardenAiLogic.STATUS_SPAWN) {                                  // :391-392
            this.setSpawnTimer(WardenAiLogic.spawnTimerForStatus(id));
        } else {
            super.handleEntityEvent(id);                                                // :393-394
        }
    }

    // ------------------------------------------------------------------ faction (:403-405)

    /**
     * {@code :403-405} — {@code func_70686_a(Class)}: the Guardian class is refused
     * outright (the warden never shoots its own guardian), everything else takes the
     * base's answer. 1.21.1's twin is {@code Mob#canAttackType(EntityType)} (NeoForge;
     * the leader's pair at {@code EntityCultistLeader:325-333}), the exact-class
     * write compares the registered types.
     */
    @Override
    public boolean canAttackType(final EntityType<?> type) {
        if (WardenAiLogic.refusesTargetType(type == CatalogEldritchGuardian
                .ELDRITCH_GUARDIAN.get())) {                                            // :404
            return false;
        }
        return super.canAttackType(type);                                               // :404
    }

    // ------------------------------------------------------------------ ranged (:340-380)

    /**
     * {@code :340-380} — {@code func_82196_d}: 80% of shots
     * ({@link WardenAiLogic#firesOrb(float)}) fire the {@link EntityEldritchOrb}
     * from the alternating arm ({@code :343-344} toggle, status 16/15, muzzle 90°
     * right / 180° left, the flat 0.13 drop, the raw-difference aim at 1.0 / 2.0 —
     * the guardian lane's {@code shoot} lesson: the platform normalises, no
     * hand-rolled division), {@code thaumcraft:egattack} at 2.0 / 1.0+rand*0.1.
     * The 20% branch is the sonic blast behind {@code func_70685_l} (the CLASS
     * twin that rides {@code func_70686_a} — c5-1's :181 measurement, so the
     * Guardian-faction refusal at {@code :403-405} gates the blast): the
     * {@code FXSonic} packet (FX lane's send, pinned), the yaw knockback (1.5 /
     * 0.1 / 1.5), WITHER + WEAKNESS 400 ticks amplifier 0 (the fields csv pair;
     * the 1.7.10 {@code try/catch} ships dropped), sticky warp
     * {@code 3 + worldRandom(3)} on players (the WORLD random, verbatim), and
     * {@code thaumcraft:egscreech} at 4.0 / 1.0+rand*0.1.
     */
    @Override
    public void performRangedAttack(final LivingEntity target, final float distanceFactor) {
        final Level level = this.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (WardenAiLogic.firesOrb(this.random.nextFloat())) {                          // :341
            final EntityEldritchOrb blast =
                    new EntityEldritchOrb(CatalogEldritchCrab.ELDRITCH_ORB.get(), level, this); // :342
            this.lastBlast = !this.lastBlast;                                           // :343
            level.broadcastEntityEvent(this, this.lastBlast
                    ? WardenAiLogic.STATUS_ARM_RIGHT : WardenAiLogic.STATUS_ARM_LEFT);  // :344
            final int rr = WardenAiLogic.muzzleDegrees(this.lastBlast);                 // :345
            final double rad = Math.toRadians((this.getYRot() + rr) % 360.0F);          // :346/:348
            final double xx = Math.cos(rad) * WardenAiLogic.MUZZLE_RADIUS;              // :346
            final double yy = WardenAiLogic.MUZZLE_Y_DROP;                              // :347
            final double zz = Math.sin(rad) * WardenAiLogic.MUZZLE_RADIUS;              // :348
            blast.setPos(blast.getX() - xx, blast.getY() - yy, blast.getZ() - zz);      // :349
            final double d0 = target.getX() + target.getDeltaMovement().x()
                    - this.getX();                                                      // :350
            final double d1 = target.getY() - this.getY() - target.getBbHeight() / 2.0F; // :351
            final double d2 = target.getZ() + target.getDeltaMovement().z()
                    - this.getZ();                                                      // :352
            blast.shoot(d0, d1, d2, WardenAiLogic.ORB_SPEED, WardenAiLogic.ORB_INACCURACY); // :353
            this.playSound(ModSounds.EGATTACK.get(), WardenAiLogic.ATTACK_SOUND_VOLUME,
                    WardenAiLogic.SOUND_PITCH_BASE
                            + this.random.nextFloat() * WardenAiLogic.SOUND_PITCH_JITTER); // :354
            level.addFreshEntity(blast);                                                // :355
            return;
        }
        if (target != this && this.canAttackType(target.getType())) {                   // :356 func_70685_l — the CLASS twin that rides func_70686_a (c5-1's :181 measurement)
            // :357-361 — PacketFXSonic to tracking players within 32 blocks: the
            // FX lane's send (docs/fx-events.md). The shape ships on WardenAiLogic.
            target.setDeltaMovement(target.getDeltaMovement().add(                          // :362-366
                    WardenAiLogic.sonicKnockX(this.getYRot()),
                    WardenAiLogic.SONIC_KNOCK_UP,
                    WardenAiLogic.sonicKnockZ(this.getYRot())));                        // the golem's twin (:345)
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,
                    WardenAiLogic.SONIC_WITHER_TICKS,
                    WardenAiLogic.SONIC_WITHER_AMPLIFIER));                             // :369
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    WardenAiLogic.SONIC_WEAKNESS_TICKS,
                    WardenAiLogic.SONIC_WEAKNESS_AMPLIFIER));                           // :370
            if (target instanceof ServerPlayer player) {                                // :374
                ResearchManager.applyWarp(player,
                        WardenAiLogic.SONIC_WARP_BASE + serverLevel.getRandom()
                                .nextInt(WardenAiLogic.SONIC_WARP_JITTER),
                        SyncWarpPayload.WARP_STICKY);                                   // :375
            }
            this.playSound(ModSounds.EGSCREECH.get(), WardenAiLogic.SCREECH_VOLUME,
                    WardenAiLogic.SOUND_PITCH_BASE
                            + this.random.nextFloat() * WardenAiLogic.SOUND_PITCH_JITTER); // :378
        }
    }

    // ------------------------------------------------------------------ sound (:407-413)

    /** {@code :407-409} — ambient {@code thaumcraft:egidle} (the audio lane's id). */
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return ModSounds.EGIDLE.get();
    }

    /** {@code :411-413} — death {@code thaumcraft:egdeath}. */
    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return ModSounds.EGDEATH.get();
    }
}
