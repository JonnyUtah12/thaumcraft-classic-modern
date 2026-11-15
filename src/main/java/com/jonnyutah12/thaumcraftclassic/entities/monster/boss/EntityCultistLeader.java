package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.jonnyutah12.thaumcraftclassic.entities.ai.boss.AIAttackOnCollide;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AICultistHurtByTarget;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AILongRangeAttack;
import com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityGolemOrb;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCultists;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEntities;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;

/**
 * The Praetor — {@code thaumcraft:cultistleader} (content-mapping
 * {@code Thaumcraft:CultistLeader}, mod_index 29), {@code thaumcraft.common.entities.monster.boss.EntityCultistLeader}
 * (203 lines, {@code extends EntityThaumcraftBoss implements IRangedAttackMob}).
 * Registration {@code ConfigEntities.java:115} — {@code (64, 3, true)}.
 *
 * <p>The leader is the boss base plus six things, every figure on
 * {@link CultistLeaderAiLogic} and pinned by {@code CultistLeaderAiLogicTest}:
 * the 0.75 x 2.25 box and the 40-point experience ({@code :56}, {@code :66}), the
 * 125/5/0.32 attributes over the base's knockback 0.95 and follow 40 ({@code :70-75}),
 * the nine-row goal table at the original slots ({@code :57-65}), the 15-title
 * watched byte with its NBT round-trip ({@code :80}, {@code :101-111}), the gated
 * plate/weapon spawn pass with the light-gated enchantment ({@code :113-132},
 * {@code :156-162}), the 8-block haste aura over the cultist family
 * ({@code :164-178}) and the red energy orb from the 16-block ranged goal
 * ({@code :180-201}).</p>
 *
 * <p>Seams, recorded in the row's {@code known_differences}, never faked: the
 * plate/boots/sword items and the lootbag are unregistered content, so the
 * equipment and death drops ship gated (the knight lane's BD-68 shape; the base's
 * {@code :205} false flag gates the loot drop in both worlds); the
 * {@code tc.mobmod} champion name suffix ({@code :83-91}) rides an unregistered
 * attribute and 21.1.259 has no {@code generateName} seam, so a fresh spawn carries
 * no custom name and the plain "Praetor" lang label shows.</p>
 */
public class EntityCultistLeader extends EntityThaumcraftBoss implements RangedAttackMob {

    /**
     * {@code :80} — {@code addObject(16, (byte) 0)}: the watched title byte. The
     * original's index 16 is pinned on {@link CultistLeaderAiLogic.TITLE_WATCHER_INDEX}
     * as data (1.21.1 allocates synched ids; BD-101's shape).
     */
    private static final EntityDataAccessor<Byte> TITLE =
            SynchedEntityData.defineId(EntityCultistLeader.class, EntityDataSerializers.BYTE);

    public EntityCultistLeader(final EntityType<? extends EntityCultistLeader> type,
            final Level level) {
        super(type, level);
        this.xpReward = CultistLeaderAiLogic.EXPERIENCE;                                // :66
    }

    /**
     * {@code ConfigEntities.java:115} — {@code registerModEntity(EntityCultistLeader.class,
     * "CultistLeader", 29, instance, 64, 3, true)}: tracking 64 blocks = 4 chunks,
     * update interval 3, velocity updates true; the box is {@code :56}'s 0.75 x 2.25
     * (1.21.1 sizes from the builder, not a {@code setSize} call).
     */
    public static EntityType<EntityCultistLeader> buildType(final MobCategory category) {
        return EntityType.Builder.<EntityCultistLeader>of(EntityCultistLeader::new, category)
                .sized(CultistLeaderAiLogic.WIDTH, CultistLeaderAiLogic.HEIGHT)         // :56
                .clientTrackingRange(CultistLeaderAiLogic.TRACK_RANGE_CHUNKS)           // :115, 64
                .updateInterval(CultistLeaderAiLogic.UPDATE_INTERVAL)                   // :115, 3
                .setShouldReceiveVelocityUpdates(CultistLeaderAiLogic.VELOCITY_UPDATES) // :115, true
                .build("cultistleader");
    }

    /** {@code :70-75} — the boss base's pair (knockback 0.95, follow 40.0) plus 0.32 / 125.0 / 5.0. */
    public static AttributeSupplier.Builder createAttributes() {
        return CultistLeaderAiLogic.createAttributes();
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TITLE, (byte) 0);                                                // :80
    }

    // ------------------------------------------------------------------ title (:93-111)

    /** {@code :93-95} — {@code getTitle()} reads the watched byte. */
    public int getTitle() {
        return this.entityData.get(TITLE).intValue();
    }

    /** {@code :97-99} — {@code setTitle(int)} writes it. */
    public void setTitle(final int title) {
        this.entityData.set(TITLE, (byte) title);
    }

    /** {@code :101-105} — the whole watched byte rides the NBT key {@code title}. */
    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(CultistLeaderAiLogic.NBT_TITLE, (byte) getTitle());                 // :104
    }

    /** {@code :107-111} — read back, defaulting to 0 on an absent key as 1.7.10's {@code getByte} does. */
    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTitle(tag.getByte(CultistLeaderAiLogic.NBT_TITLE));                          // :110
    }

    // ------------------------------------------------------------------ goals (:57-65)

    /**
     * {@code :57-65} — the goal table, slot for slot, in the original's order:
     *
     * <pre>
     *   0  EntityAISwimming                        → FloatGoal
     *   2  AILongRangeAttack(16.0, 1.0, 30, 40, 24.0F)  → the family's TC goal class
     *   3  AIAttackOnCollide(EntityLivingBase, 1.1, false) → TC goal: MeleeAttackGoal + class filter
     *   6  EntityAIMoveTowardsRestriction(0.8)     → MoveTowardsRestrictionGoal
     *   7  EntityAIWander(0.8)                     → RandomStrollGoal
     *   8  EntityAIWatchClosest(Player, 8.0F)      → LookAtPlayerGoal (0.02 chance)
     *   8  EntityAILookIdle                        → RandomLookAroundGoal
     *   target 1 AICultistHurtByTarget(this, true)
     *   target 2 EntityAINearestAttackableTarget(Player, 0, true)
     * </pre>
     *
     * <p>The two {@code :58}/{@code :59} goals are Thaumcraft's OWN classes —
     * {@code ai/combat/AILongRangeAttack} (landed by the cleric lane) and
     * {@code ai/combat/AIAttackOnCollide} (the vanilla melee body plus the
     * {@code classTarget} admission and the boss's {@code :63-65} spawn-timer gate)
     * — not the vanilla twins, per the card.</p>
     *
     * <p>Slots {@code :62}/{@code :63} collide at priority 8. 1.7.10's
     * {@code GoalSelect} is a {@code TreeMap} keyed by priority, so the original's
     * WatchClosest is REPLACED by the LookIdle and nets six live movement goals;
     * 1.21.1's {@code GoalSelector} keeps both at the slot (measured on the cleric's
     * set, BD-112), so the port installs the pair at the original slot and ships
     * both. Recorded deviation.</p>
     *
     * <p>1.21.1's {@code Monster#registerGoals} (the constructor pass) installs the
     * Float/Melee/Wander/Look quartet plus a hurt-target goal, and this override
     * does not call {@code super.registerGoals()} — the same idiom the knight and
     * cleric ship — so the Monster-inherited goals the original never had are
     * stripped, and the set measured is exactly the table above.</p>
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(CultistLeaderAiLogic.GOAL_SLOT_SWIM, new FloatGoal(this)); // :57
        this.goalSelector.addGoal(CultistLeaderAiLogic.GOAL_SLOT_RANGED,
                new AILongRangeAttack(this, CultistLeaderAiLogic.RANGED_MIN_DISTANCE,
                        CultistLeaderAiLogic.RANGED_SPEED, CultistLeaderAiLogic.RANGED_INTERVAL_MIN,
                        CultistLeaderAiLogic.RANGED_INTERVAL_MAX,
                        CultistLeaderAiLogic.RANGED_RANGE));                            // :58
        this.goalSelector.addGoal(CultistLeaderAiLogic.GOAL_SLOT_MELEE,
                new AIAttackOnCollide(this, LivingEntity.class,
                        CultistLeaderAiLogic.MELEE_SPEED,
                        CultistLeaderAiLogic.MELEE_LONG_MEMORY));                       // :59
        this.goalSelector.addGoal(CultistLeaderAiLogic.GOAL_SLOT_RESTRICTION,
                new MoveTowardsRestrictionGoal(this, CultistLeaderAiLogic.RESTRICTION_SPEED)); // :60
        this.goalSelector.addGoal(CultistLeaderAiLogic.GOAL_SLOT_WANDER,
                new RandomStrollGoal(this, CultistLeaderAiLogic.WANDER_SPEED));         // :61
        this.goalSelector.addGoal(CultistLeaderAiLogic.GOAL_SLOT_WATCH,
                new LookAtPlayerGoal(this, Player.class, CultistLeaderAiLogic.WATCH_PLAYER_RANGE,
                        CultistLeaderAiLogic.WATCH_PLAYER_PROBABILITY));                // :62
        this.goalSelector.addGoal(CultistLeaderAiLogic.GOAL_SLOT_LOOK_IDLE,
                new RandomLookAroundGoal(this));                                        // :63
        this.targetSelector.addGoal(CultistLeaderAiLogic.TARGET_SLOT_HURT,
                new AICultistHurtByTarget(this, CultistLeaderAiLogic.HURT_IS_AGRESSORS)); // :64
        this.targetSelector.addGoal(CultistLeaderAiLogic.TARGET_SLOT_PLAYER,
                new NearestAttackableTargetGoal<>(this, Player.class,
                        CultistLeaderAiLogic.TARGET_MUST_SEE));                         // :65
    }

    // ------------------------------------------------------------------ spawn (:113-132, :156-162)

    /**
     * {@code :156-162} — {@code onInitialSpawn}: the equipment pass, the enchant
     * pass, the title roll. The original runs them BEFORE {@code super}; the family's
     * BD-98 idiom (the knight at {@code Knight:104-120}) calls the pair from the
     * mob's own {@code finalizeSpawn} after super, and the leader rides that — the
     * boss super adds the 24-block home ({@code EntityThaumcraftBoss:72-75}), the
     * title roll is independent of both. Recorded deviation (BD-98 family).
     */
    @Override
    public SpawnGroupData finalizeSpawn(final ServerLevelAccessor level,
            final DifficultyInstance difficulty, final MobSpawnType reason,
            final SpawnGroupData groupData) {
        final SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);              // :158
        this.populateDefaultEquipmentEnchantments(level, level.getRandom(), difficulty);// :159
        this.setTitle(CultistLeaderAiLogic.titleFor(                                    // :160
                level.getRandom().nextInt(CultistLeaderAiLogic.TITLES.length)));
        return data;
    }

    /**
     * {@code :113-124} — {@code func_82164_bB()}: helm/chest/legs = the leader
     * plate, boots = the cultist boots, weapon = {@code itemSwordVoid} on EASY
     * ({@code :120}) else {@code itemSwordCrimson} ({@code :122}). Every item is
     * unregistered content ({@code not_started} rows), so each write is gated on
     * the live registry — the knight lane's BD-68 shape: the rule ships, the slot
     * stays empty, and the branch lights up when the item lands.
     */
    @Override
    protected void populateDefaultEquipmentSlots(final RandomSource random,
            final DifficultyInstance difficulty) {
        for (final java.util.Map.Entry<Integer, ResourceLocation> row
                : CultistLeaderAiLogic.leaderPlate().entrySet()) {                      // :115-118
            setSlotIfReal(slotForOrdinal(row.getKey()), row.getValue());
        }
        setSlotIfReal(EquipmentSlot.MAINHAND,                                           // :119-123
                CultistLeaderAiLogic.weaponFor(
                        this.level().getDifficulty() == Difficulty.EASY));              // :119 worldObj.getDifficulty()
    }

    /**
     * {@code :126-132} — {@code func_82162_bC()}: with a held item, on
     * {@code rand.nextFloat() < 0.5F * light}, the weapon is enchanted at
     * {@code (int)(7.0F + light * rand.nextInt(22))}. The light read is the family's
     * ({@code CultistAiLogic} knight-lane note: 1.7.10's {@code func_147462_b} ships
     * as {@code getLightEngine().getRawBrightness(pos, 10)}), and the enchantment is
     * {@code EnchantmentHelper.enchantItem} over the mob-spawn tag set — the BD-93/
     * BD-94 pair the knight lane recorded.
     */
    @Override
    protected void populateDefaultEquipmentEnchantments(final ServerLevelAccessor level,
            final RandomSource random, final DifficultyInstance difficulty) {
        final ItemStack weapon = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (weapon.isEmpty()) {                                                         // :129
            return;
        }
        final float light = this.level().getLightEngine()
                .getRawBrightness(this.blockPosition(), 10);                            // :128
        if (!CultistLeaderAiLogic.enchantFires(random.nextFloat(), light)) {            // :129
            return;
        }
        final int levels = CultistLeaderAiLogic.enchantLevel(light,
                random.nextInt(CultistLeaderAiLogic.ENCHANT_LEVEL_ROLL));               // :130
        EnchantmentHelper.enchantItem(random, weapon, levels, level.registryAccess(),
                level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                        .getTag(EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT));               // :130
    }

    private void setSlotIfReal(final EquipmentSlot slot, final ResourceLocation id) {
        final ItemStack stack = stackFor(id);
        if (!stack.isEmpty()) {
            this.setItemSlot(slot, stack);
        }
    }

    private static ItemStack stackFor(final ResourceLocation id) {
        return BuiltInRegistries.ITEM.containsKey(id)
                ? new ItemStack(BuiltInRegistries.ITEM.get(id))
                : ItemStack.EMPTY;
    }

    private static EquipmentSlot slotForOrdinal(final int ordinal) {
        return switch (ordinal) {                                                       // CultistAiLogic.SLOT_*
            case CultistAiLogic.SLOT_MAINHAND -> EquipmentSlot.MAINHAND;
            case CultistAiLogic.SLOT_BOOTS -> EquipmentSlot.FEET;
            case CultistAiLogic.SLOT_LEGS -> EquipmentSlot.LEGS;
            case CultistAiLogic.SLOT_CHEST -> EquipmentSlot.CHEST;
            case CultistAiLogic.SLOT_HELMET -> EquipmentSlot.HEAD;
            default -> null;
        };
    }

    // ------------------------------------------------------------------ faction (:134-141)

    /**
     * {@code :134-137} — {@code func_142014_c(EntityLivingBase)}: the leader's
     * faction enemies are cultists and other leaders. The family's 1.21.1 twin is
     * {@code LivingEntity#canAttack(LivingEntity)} (NeoForge; the target goals and
     * the melee goal answer through it), and the base's pair is the knight lane's
     * wiring ({@code EntityCultist:241-248}). The expression ships verbatim: the
     * original returns the faction answer alone, with no vanilla fall-through.
     */
    @Override
    public boolean canAttack(final LivingEntity target) {
        return CultistLeaderAiLogic.isEnemyFaction(target);                             // :136
    }

    /**
     * {@code :139-141} — {@code func_70686_a(Class)}: cleric, leader and knight
     * classes are refused, anything else takes the base's answer. The 1.21.1 twin
     * is {@code Mob#canAttackType(EntityType)} (NeoForge, the family's pair on
     * {@code EntityCultist:255-265}); the original writes exact class equality, so
     * the shipped form compares the registered type ids.
     */
    @Override
    public boolean canAttackType(final EntityType<?> type) {
        if (type == CatalogCultists.CULTIST_CLERIC.get()
                || type == CatalogCultists.CULTIST_KNIGHT.get()
                || type == CatalogEntities.CULTIST_LEADER.get()) {                      // :140
            return false;
        }
        return super.canAttackType(type);                                               // :140
    }

    // ------------------------------------------------------------------ damage (:222-255 via the base)

    /** The boss base's damage leg: aggro entry, rage rule, 35.0F clamp. */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        return super.hurt(source, bossDamageLeg(source, amount));                       // :222-255
    }

    // ------------------------------------------------------------------ aura (:164-178)

    /**
     * {@code :164-178} — {@code onLivingUpdate}: every {@code EntityCultist} in the
     * 8.0 scan ({@code EntityUtils.getEntitiesInRange} — a radius box about the
     * position, the caller excluded by id) gets haste ({@code Potion.field_76428_l},
     * 1.21.1's {@code MobEffects.DIG_SPEED}) for 60 ticks at amplifier 1, and only
     * while it has none ({@code :172}). The try/catch of {@code :175} guarded 1.7.10's
     * cross-thread watcher read; the 1.21.1 scan needs none. The effect application
     * self-gates server-side (21.1.259 {@code LivingEntity#addEffect}), as
     * {@code func_70690_d} did.
     */
    @Override
    public void aiStep() {
        super.aiStep();
        final double r = CultistLeaderAiLogic.AURA_RADIUS;                              // :168
        final AABB box = new AABB(this.getX() - r, this.getY() - r, this.getZ() - r,
                this.getX() + r, this.getY() + r, this.getZ() + r);                     // :168
        for (final EntityCultist cultist : this.level().getEntitiesOfClass(
                EntityCultist.class, box, e -> e.getId() != this.getId())) {            // :168-169
            if (CultistLeaderAiLogic.auraApplies(cultist.hasEffect(MobEffects.DIG_SPEED))) { // :172
                cultist.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,
                        CultistLeaderAiLogic.AURA_HASTE_TICKS,
                        CultistLeaderAiLogic.AURA_HASTE_AMPLIFIER));                    // :173
            }
        }
    }

    // ------------------------------------------------------------------ ranged (:180-201)

    /**
     * {@code :180-201} — {@code func_82196_d(EntityLivingBase, float)}
     * ({@code performRangedAttack}): the {@code :181} gate is 1.7.10's
     * {@code func_70685_l} (targetable through the faction pair the leader overrides
     * at {@code :135-141}), the living sound of {@code :182} (the leader registers no
     * ambient, so it is silent), then the <b>red</b> orb —
     * {@code new EntityGolemOrb(world, this, target, true)} at {@code :191} (the
     * per-caller flag pair the cleric lane pinned: lifetime 240 / damage x1.0) —
     * nudged by its own motion ({@code :192-193}, zero at construction, verbatim),
     * aimed with {@code setThrowableHeading(d0, d1 + 2.0, d2, 0.66F, 3.0F)}
     * ({@code :198}), {@code thaumcraft:egattack} at 1.0F / 1.0F + rand*0.1F
     * ({@code :199} — the audio lane registered the event, ModSounds:122, so it
     * plays), and the entity added ({@code :200}).
     */
    @Override
    public void performRangedAttack(final LivingEntity target, final float distanceFactor) {
        if (!this.canAttack(target)) {                                                  // :181
            return;
        }
        playAmbientSound();                                                           // :182
        final EntityGolemOrb blast = new EntityGolemOrb(CatalogGolems.GOLEM_ORB.get(),
                this.level(), this, target, CultistLeaderAiLogic.ORB_IS_RED);           // :191
        final Vec3 motion = blast.getDeltaMovement();
        blast.setPos(blast.getX() + motion.x() / CultistLeaderAiLogic.ORB_NUDGE_FACTOR, // :192
                blast.getY(),
                blast.getZ() + motion.y() / CultistLeaderAiLogic.ORB_NUDGE_FACTOR);     // :193
        final double d0 = target.getX() - this.getX();                                  // :195
        final double d1 = (target.getBoundingBox().minY + target.getBbHeight() / 2.0F)
                - (this.getY() + this.getBbHeight() / 2.0F);                            // :196
        final double d2 = target.getZ() - this.getZ();                                  // :197
        blast.shoot(d0, d1 + CultistLeaderAiLogic.ORB_AIM_LIFT, d2,
                CultistLeaderAiLogic.ORB_SPEED, CultistLeaderAiLogic.ORB_SPREAD);       // :198
        this.playSound(ModSounds.EGATTACK.get(), CultistLeaderAiLogic.ORB_SOUND_VOLUME,
                CultistLeaderAiLogic.ORB_SOUND_PITCH_BASE
                        + this.random.nextFloat() * CultistLeaderAiLogic.ORB_SOUND_PITCH_JITTER); // :199
        this.level().addFreshEntity(blast);                                             // :200
    }

    // ------------------------------------------------------------------ drops (:147-154)

    /**
     * {@code :147-150} — {@code func_70628_a(boolean, int)}
     * ({@code dropCustomDeathLoot}): the lootbag, meta 2, at 1.5F. Two gates ship
     * as the original wrote them: the item is unregistered content (BD-68 shape),
     * and the boss base's {@code func_70692_ba()} false ({@code EntityThaumcraftBoss:205})
     * gates the whole death-loot block, so 21.1.259's
     * {@code Mob#dropAllDeathLoot} never reaches this hook — the drop is
     * unreachable in both worlds, which the row records and a probe measures.
     */
    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source,
            final boolean recentlyHit) {
        if (!BuiltInRegistries.ITEM.containsKey(CultistLeaderAiLogic.LOOTBAG)) {        // :149
            return;
        }
        final ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(
                CultistLeaderAiLogic.LOOTBAG));
        stack.setDamageValue(CultistLeaderAiLogic.LOOTBAG_META);                        // :149 meta 2
        this.spawnAtLocation(stack, CultistLeaderAiLogic.LOOTBAG_CHANCE);               // :149
    }
}
