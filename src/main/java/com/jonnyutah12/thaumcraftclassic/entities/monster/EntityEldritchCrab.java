package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The Eldritch Crab — {@code thaumcraft:eldritchcrab} (content-mapping.csv
 * line 330, {@code Thaumcraft:EldritchCrab}, mod_index 121,
 * {@code ConfigEntities.java:121}, tracking 64 blocks / update 3 / velocity true).
 *
 * <p>Original: {@code thaumcraft.common.entities.monster.EntityEldritchCrab},
 * 215 lines, {@code extends EntityMob}. The shell mechanic is the core: the
 * crab spawns shelled ({@code 1/3} below HARD, guaranteed on HARD,
 * {@code :72-76}), carries 5 armour while shelled ({@code :67-69}) and moves
 * at 0.275 ({@code :55}), loses the shell at half health — dropping
 * {@code itemChestCultistplate} and returning to speed 0.3 ({@code :160-169}) —
 * and an unshelled crab mid-leap under a bareheaded player climbs onto them
 * ({@code :120-129}) and punches its mount every 10..19 ticks with a 20%
 * dismount ({@code :131-137}). The spawn data is the 1.7.10 spider GroupData:
 * on HARD above the local-difficulty-scaled roll a potion id is picked and
 * applied for {@code Integer.MAX_VALUE} ticks ({@code :78-92}).</p>
 *
 * <p>Shape notes, each figure on {@link EldritchCrabAiLogic}: the synched
 * Flags byte ({@code :58-61}, index 22) is a byte accessor with the shell in
 * bit 0 and round-trips under the key {@code Flags} ({@code :171-182}); the
 * runtime armour getter of {@code :67-69} has no 1.21.1 hook, so the shell
 * flag drives the per-entity ARMOR base value instead — set at spawn, in
 * {@link #setShell} and on load (the original's {@code func_110147_ax} ran
 * from {@code onInitialSpawn} after the helm roll, {@code EntityLivingBase.java:951},
 * which is the same point our {@link #finalizeSpawn} reaches). The hurt-time
 * chest-plate drop is gated on the live item registry (BD-68's shape) —
 * {@code thaumcraft:chestcultistplate} is {@code not_started}.</p>
 *
 * <p>The unregistered {@code thaumcraft:crabclaw}/{@code crabtalk}/{@code crabdeath}
 * ids are pinned in {@link EldritchCrabAiLogic} and NOT fired (D-004); the
 * hurt is the vanilla legacy {@code game.hostile.hurt}, which 1.21.1's
 * {@code SoundType.HOSTILE} default already fires, and the step override rides
 * {@code SoundType.SPIDER} ({@code :200-202}). The spider GroupData potion ids
 * reordered between the engines (1.7.10 strength 8 → 1.21.1 strength 5); the
 * roll mechanism, the {@code > 0} gate and the {@code Integer.MAX_VALUE}
 * duration ship intact — the id-table reorder is the ledger row.</p>
 */
public class EntityEldritchCrab extends Monster {

    /** {@code :58-61} — the synched Flags byte (watcher 22 in the original). */
    private static final EntityDataAccessor<Byte> DATA_FLAGS =
            SynchedEntityData.defineId(EntityEldritchCrab.class,
                    EntityDataSerializers.BYTE);

    public EntityEldritchCrab(final EntityType<? extends EntityEldritchCrab> type,
            final Level level) {
        super(type, level);
        // :34 — field_70728_aV = 6 (see EldritchCrabAiLogic.XP for why the
        // constructor carries the reward in this port).
        this.xpReward = EldritchCrabAiLogic.XP;
        // :35-36 — getNavigator().setCanBreakDoors(true) / setCanSwim(true):
        // 1.21.1 splits the pair (door flag on GroundPathNavigation,
        // EntityCultist.java:97-99 is the shipped pattern).
        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation g) {
            g.setCanOpenDoors(true);
        }
        this.getNavigation().setCanFloat(true);
        // :97-99 — func_70650_aV() (canPickUpLoot) true: the field as well as
        // the override (Mob reads the field in its pickup path).
        this.setCanPickUpLoot(true);
    }

    /** The original's registration args are {@code (64, 3, true)} —
     *  {@code ConfigEntities.java:121}; 64 blocks = 4 chunks
     *  ({@code BrainyZombieAiLogicTest:38}). */
    public static EntityType<EntityEldritchCrab> buildType(final MobCategory category) {
        return EntityType.Builder.<EntityEldritchCrab>of(EntityEldritchCrab::new, category)
                .sized(EldritchCrabAiLogic.WIDTH, EldritchCrabAiLogic.HEIGHT)  // :33
                .clientTrackingRange(EldritchCrabAiLogic.TRACK_RANGE_CHUNKS)    // 64 blocks (:121)
                .updateInterval(EldritchCrabAiLogic.UPDATE_INTERVAL)            // 3 (:121)
                .build("eldritchcrab");
    }

    /** {@code :51-56} — health 20, attack 4, speed 0.3 (the builder baseline;
     *  a shelled spawn writes 0.275 through {@link #finalizeSpawn}, the point
     *  the original's {@code func_110147_ax} re-read {@code hasHelm()}). */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, EldritchCrabAiLogic.MAX_HEALTH)         // :53
                .add(Attributes.ATTACK_DAMAGE, EldritchCrabAiLogic.ATTACK_DAMAGE)   // :54
                .add(Attributes.MOVEMENT_SPEED, EldritchCrabAiLogic.SPEED_NO_SHELL) // :55
                .add(Attributes.ARMOR, EldritchCrabAiLogic.ARMOR_NONE);             // :67-69 runtime
    }

    // ------------------------------------------------------------------ goals

    /** {@code :37-44} — the crab's goal set, slot for slot: swim 0, leap 2
     *  (0.63F), melee 3 (1.0, false — the family's AIAttackOnCollide mapping),
     *  wander 7 (0.8), look-idle 8; hurt-by-target 1 (the {@code true} alert
     *  flag is 1.21.1's plain-goal default, alerting the crab's own class),
     *  player 2, EntityCultist 3. No WatchClosest, so no slot-8 collision. */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(EldritchCrabAiLogic.GOAL_SLOT_SWIM, new FloatGoal(this));   // :37
        this.goalSelector.addGoal(EldritchCrabAiLogic.GOAL_SLOT_LEAP,
                new LeapAtTargetGoal(this, EldritchCrabAiLogic.LEAP_CHANCE));                 // :38
        this.goalSelector.addGoal(EldritchCrabAiLogic.GOAL_SLOT_MELEE,
                new MeleeAttackGoal(this, EldritchCrabAiLogic.MELEE_SPEED,
                        EldritchCrabAiLogic.MELEE_LONG_MEMORY));                              // :39
        this.goalSelector.addGoal(EldritchCrabAiLogic.GOAL_SLOT_WANDER,
                new RandomStrollGoal(this, EldritchCrabAiLogic.WANDER_SPEED));                // :40
        this.goalSelector.addGoal(EldritchCrabAiLogic.GOAL_SLOT_LOOK_IDLE,
                new RandomLookAroundGoal(this));                                              // :41
        this.targetSelector.addGoal(EldritchCrabAiLogic.TARGET_SLOT_HURT,
                new HurtByTargetGoal(this));                                                  // :42 (this, true)
        this.targetSelector.addGoal(EldritchCrabAiLogic.TARGET_SLOT_PLAYER,
                new NearestAttackableTargetGoal<>(this, Player.class, true));                 // :43
        this.targetSelector.addGoal(EldritchCrabAiLogic.TARGET_SLOT_CULTIST,
                new NearestAttackableTargetGoal<>(this, EntityCultist.class, true));          // :44
    }

    // ------------------------------------------------------------------ shell

    /** {@code :58-61} — one synched byte, the Flags. */
    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS, (byte) 0);
    }

    /** {@code :101-103} — {@code (flags & 1) != 0}. */
    public boolean hasShell() {
        return EldritchCrabAiLogic.shellIn(this.entityData.get(DATA_FLAGS));
    }

    /** {@code :105-112} — the bit-0 write. 1.21.1's armour is the ARMOR
     *  attribute (no {@code getArmorValue} hook), so the shell flag also
     *  writes the per-entity ARMOR and MOVEMENT_SPEED bases: the original's
     *  armour getter ({@code :67-69}) and the {@code :55}/{@code :165} speed
     *  writes ride the same flag transition. */
    public void setShell(final boolean shell) {
        final byte flags = this.entityData.get(DATA_FLAGS);
        this.entityData.set(DATA_FLAGS, shell
                ? EldritchCrabAiLogic.shellOn(flags)
                : EldritchCrabAiLogic.shellOff(flags));
        this.getAttribute(Attributes.ARMOR).setBaseValue(EldritchCrabAiLogic.armorFor(shell));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(EldritchCrabAiLogic.speedFor(shell));
    }

    /** {@code :171-182} — the Flags byte round-trips under {@code Flags};
     *  on load, the original re-asserts speed 0.3 when the shell is off
     *  ({@code :174-176}) and its armour getter reads the flag live. The port
     *  re-asserts both attribute bases from the loaded flag (the attributes
     *  are rebuilt from the supplier on every construction, before the data
     *  arrives — the same write the original needed at spawn). */
    @Override
    public void addAdditionalSaveData(final CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putByte(EldritchCrabAiLogic.NBT_FLAGS, this.entityData.get(DATA_FLAGS));
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(DATA_FLAGS, compound.getByte(EldritchCrabAiLogic.NBT_FLAGS));
        this.getAttribute(Attributes.ARMOR)
                .setBaseValue(EldritchCrabAiLogic.armorFor(hasShell()));
        this.getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(EldritchCrabAiLogic.speedFor(hasShell()));
    }

    // ------------------------------------------------------------------ spawn

    /**
     * {@code :71-95} — {@code onInitialSpawn}: the helm roll first
     * ({@link EldritchCrabAiLogic#rollsShell}: HARD → shell, else
     * {@code entityRandom < 0.33F}); then, with no spawn data, a fresh spider
     * GroupData — on HARD above {@code worldRandom < 0.1F * localDifficulty}
     * it picks a potion — and a GroupData with potion id &gt; 0 applies
     * {@code new PotionEffect(id, Integer.MAX_VALUE)}; finally the parent
     * runs, which is where the original's {@code func_110147_ax} re-read
     * {@code hasHelm()} for the speed ({@code EntityLivingBase.java:951}).
     *
     * <p>The local-difficulty float of {@code :82} is the
     * {@link DifficultyInstance} 1.21.1 hands {@code finalizeSpawn} for the
     * spawn position ({@link DifficultyInstance#getSpecialMultiplier()}, the
     * same LCG-weighted figure 1.7.10's {@code World#func_147462_b} computed at
     * the mob's feet). The carrier is {@link EldritchCrabGroupData}: 1.21.1's
     * spider {@code SpiderEffectsGroupData} stores a {@code Holder<MobEffect>}
     * in a {@code protected} field with NO accessor (verified on
     * 21.1.259), so a mod subclass cannot read the vanilla carrier, and its
     * roll is a uniform {@code nextInt(4)} where 1.7.10's is the
     * coin-flip-then-25%-cascade. This port therefore ships the 1.7.10 roll and
     * the 1.7.10 carrier shape ({@link EldritchCrabGroupData}) over the same
     * four-entry effect table, and the vanilla pair's differences are the
     * ledger row; the gate, the duration ({@code Integer.MAX_VALUE}) and the
     * HARD-and-local-difficulty chance ship intact.</p>
     */
    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(
            final ServerLevelAccessor level, final DifficultyInstance difficulty,
            final MobSpawnType reason,
            @Nullable final SpawnGroupData spawnData) {
        // :72-76 — the helm roll uses the ENTITY random (field_70146_Z).
        setShell(EldritchCrabAiLogic.rollsShell(difficulty.isHard(), this.random.nextFloat()));
        SpawnGroupData data = spawnData;
        if (data == null) {                                                     // :78
            final EldritchCrabGroupData group = new EldritchCrabGroupData();
            // :80-84 — HARD, the WORLD random (field_73012_v) and the local
            // difficulty at the spawn position.
            if (EldritchCrabAiLogic.rollsGroupPotion(difficulty.isHard(),
                    level.getRandom().nextFloat(), difficulty.getSpecialMultiplier())) {
                group.marker = EldritchCrabAiLogic.rollGroupEffect(level.getRandom());
            }
            data = group;
        }
        if (data instanceof EldritchCrabGroupData group                         // :87-89
                && EldritchCrabAiLogic.appliesGroupEffect(group.marker,
                        groupEffectOf(group.marker) != null)) {
            this.addEffect(new MobEffectInstance(groupEffectOf(group.marker),
                    EldritchCrabAiLogic.GROUP_EFFECT_MAX_AGE));                 // :90
        }
        return super.finalizeSpawn(level, difficulty, reason, data);            // :94
    }

    /**
     * {@code :78-92} — the spawn-data carrier: 1.7.10's
     * {@code EntityLivingData.GroupData} is a public {@code int} potion id, and
     * 1.21.1's spider carrier keeps its effect out of reach of a subclass, so
     * the crab carries the roll itself as the table marker of {@link
     * EldritchCrabAiLogic#rollGroupEffect(net.minecraft.util.RandomSource)}.
     */
    public static final class EldritchCrabGroupData implements SpawnGroupData {
        /** The picked table entry; {@code 0} = nothing picked (the {@code > 0}
         *  gate of {@code :88}). */
        public int marker = EldritchCrabAiLogic.GROUP_EFFECT_NONE;
    }

    /** The spider buff table, read from the marker: speed, strength,
     *  regeneration, invisibility — 1.7.10's {@code GroupData} entries
     *  resolved through 1.21.1's {@link MobEffects} holders. */
    public static Holder<MobEffect> groupEffectOf(final int marker) {
        return switch (marker) {
            case EldritchCrabAiLogic.GROUP_EFFECT_SPEED -> MobEffects.MOVEMENT_SPEED;
            case EldritchCrabAiLogic.GROUP_EFFECT_STRENGTH -> MobEffects.DAMAGE_BOOST;
            case EldritchCrabAiLogic.GROUP_EFFECT_REGENERATION -> MobEffects.REGENERATION;
            case EldritchCrabAiLogic.GROUP_EFFECT_INVISIBILITY -> MobEffects.INVISIBILITY;
            default -> null;
        };
    }

    // ------------------------------------------------------------------ tick

    /**
     * {@code :114-138} — the three-part tick body after the parent:
     * the 20-tick fall immunity ({@code :116-118}); the mount gate
     * ({@link EldritchCrabAiLogic#canMountPlayer}) →
     * {@code startRiding(closestPlayer)} ({@code :128}, the original's
     * {@code func_70078_a}, fired on both sides as the original writes it);
     * and the server-only mount punch: attack timer down →
     * {@code attackTime = 10 + nextInt(10)}, {@code doHurtTarget(mount)},
     * 20% dismount ({@code :131-137}).
     */
    @Override
    public void tick() {
        super.tick();
        // :116-118 — fall damage off for the first 20 ticks of life.
        if (this.tickCount < EldritchCrabAiLogic.FALL_IMMUNE_AGE_TICKS) {
            this.fallDistance = 0.0F;
        }

        // :120-129 — the climb: a mid-leap, unshelled crab lands on the head
        // of the nearest player who rides nobody. func_70643_av() =
        // getClosestPlayerToMe(): the nearest player, distance-unbounded
        // (1.21.1's getNearestPlayer(this, -1.0) — EntityTaintSwarm.java:360
        // is the shipped accessor pattern).
        if (this.getVehicle() == null) {
            final Player player = this.level().getNearestPlayer(this, -1.0);
            if (player != null && EldritchCrabAiLogic.canMountPlayer(
                    this.getVehicle() != null, true,
                    player.getFirstPassenger() != null,      // :122 — field_70153_n (rider)
                    this.onGround(),                         // :123 — !onGround required
                    hasShell(),                              // :124
                    player.isRemoved(),                      // :125 — field_70128_L (isDead)
                    this.getY(), player.getY(), player.getBbHeight(),
                    this.distanceToSqr(player))) {           // :127 — squared, < 4.0
                this.startRiding(player, false);             // :128 — func_70078_a
            }
        }

        // :131-137 — server-only mount punch on the attack timer, then the
        // 20% dismount (func_110145_l(mount) = dismount from the mount).
        if (!this.level().isClientSide && this.getVehicle() != null
                && this.invulnerableTime <= 0) {
            this.invulnerableTime = EldritchCrabAiLogic.punchCooldownFor(
                    this.random.nextInt(EldritchCrabAiLogic.PUNCH_COOLDOWN_JITTER));
            this.doHurtTarget(this.getVehicle());
            if (this.getVehicle() != null
                    && EldritchCrabAiLogic.dismountsAfterPunch(this.random.nextFloat())) {
                this.stopRiding();
            }
        }
    }

    // ------------------------------------------------------------------ combat / hurt

    /** {@code :160-169} — after the parent applies damage, a shelled crab at
     *  or below half health loses the shell: helm off, the chest plate
     *  dropped (registry-gated, BD-68's shape), speed back to 0.3 — all of it
     *  through {@link #setShell}, which writes the flag, the ARMOR base and
     *  the speed in the original's one transition. */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        final boolean applied = super.hurt(source, amount);
        if (EldritchCrabAiLogic.shellBreaks(hasShell(), this.getHealth(),
                this.getMaxHealth())) {
            setShell(false);
            dropShellPlate();
        }
        return applied;
    }

    /** {@code :164} — {@code func_70669_a(new ItemStack(itemChestCultistPlate))}
     *  at offset 0.0. The plate id is not registered (its content-mapping row
     *  is {@code not_started}), so the drop is gated on the live registry and
     *  nothing is invented. */
    private void dropShellPlate() {
        final ResourceLocation id = EldritchCrabAiLogic.CHEST_CULTIST_PLATE;
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return;
        }
        this.spawnAtLocation(new ItemStack(BuiltInRegistries.ITEM.get(id)), 0.0F);
    }

    /** {@code :151-158} — on a landed melee hit the claw screech fires. The
     *  id is unregistered (D-004), so the call is pinned — volume, pitch
     *  jitter and the WORLD random ({@code :153}) are recorded on
     *  {@link EldritchCrabAiLogic}; no substitute sound is fabricated. */
    @Override
    public boolean doHurtTarget(final Entity target) {
        final boolean landed = super.doHurtTarget(target);
        if (landed) {
            // :153 — worldRandom.nextFloat() * 0.2F pitch jitter on 0.9.
            // Pinned plan (the family's unregistered-sound rule):
            // playSound(SoundSource.HOSTILE, crabclaw, 1.0F, 0.9F + jitter).
            this.crabClawFxPlayed = true;
        }
        return landed;
    }

    /** The pinned claw-sound seam: true once the original's sound site ran.
     *  The game test observes the seam; the audio lane flips it to a real
     *  playSound the day {@code thaumcraft:crabclaw} registers. */
    private boolean crabClawFxPlayed;

    public boolean wasClawFxRequested() {
        return this.crabClawFxPlayed;
    }

    /** {@code :144-149} — one golden sword on {@code recentlyHit &&
     *  (nextInt(3) == 0 || nextInt(1 + LOOTING) > 0)}; 1.21.1's hook carries
     *  no looting level (BD-90's shape) so the second arm reads LOOTING 0 and
     *  can never fire — the 1-in-3 arm stands. The sword is a real vanilla
     *  item (no gate). */
    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source,
            final boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (EldritchCrabAiLogic.dropsGoldenSword(recentlyHit,
                this.random.nextInt(EldritchCrabAiLogic.GOLDEN_SWORD_ROLL_MODULO),
                this.random.nextInt(1 + EldritchCrabAiLogic.DROP_LOOTING_BASELINE))) {
            this.spawnAtLocation(new ItemStack(Items.GOLDEN_SWORD),
                    0.0F);   // :147 — func_145779_a(golden_sword, 1) (count in the stack)
        }
    }

    /**
     * {@code :212-214} — {@code func_142014_c} ({@code canAttack}): the crab
     * attacks crabs, the original's whole body, so the vanilla checks are
     * replaced. 21.1.259 declares no mob-side {@code canAttackTarget} to
     * override (its shipped seams are {@code LivingEntity#canAttack}/
     * {@code canAttackType} and the goal-side filter is {@code
     * TargetGoal#canAttack}), so this is the port's own predicate under the
     * family's name — {@code GoallessTaintMonster:59} declares the same
     * predicate for the taintacle drive, and the crab's goals consult the
     * vanilla {@code Targeting#canAttackTarget} on top. The unit test drives
     * the rule; the BD row records the vanilla targetability gate.
     */
    protected boolean canAttackTarget(final LivingEntity target) {
        return EldritchCrabAiLogic.attacksOnlyCrabs(target instanceof EntityEldritchCrab);
    }

    /** {@code :204-206} — {@code EnumCreatureAttribute.ARTHROPOD} is pinned
     *  as {@link EldritchCrabAiLogic#MOB_TYPE_ARTHROPOD_PINNED_NOT_EXPRESSED}
     *  and NOT expressed: 21.1.259 removed the creature-attribute accessor from
     *  {@code Entity}/{@code Mob} (no {@code getMobType} to override), so the
     *  smite / bane-of-arthropods bonus those categories gate lands ungated,
     *  exactly the shipped posture of {@code EntityTaintacle:420-422}. */

    /**
     * {@code :208-210} — {@code func_70687_e}: poison never applies. 21.1.259
     * has no mob-side effect-applicability hook to override (no
     * {@code isPotionApplicable} on {@code LivingEntity}, and the shipped code
     * overrides nothing of the kind), so this is the port's plain predicate
     * over the original's rule. The original's other half — :160-166,
     * {@code isSelfEffect} (the crab's own group-effect cast, marker in the
     * {@code DataWatcher}, never applies to a mob) — has no 1.21.1 hook either;
     * the rule is pinned as {@link EldritchCrabAiLogic#isSelfEffect} and
     * recorded as a BD row.
     */
    public boolean isPotionApplicable(final MobEffectInstance effect) {
        return EldritchCrabAiLogic.effectApplies(effect.is(MobEffects.POISON));
    }

    // ------------------------------------------------------------------ senses / sounds

    /** {@code :184-186} — 160 ticks of air. 21.1.259 has no
     *  {@code getMaxAirSupply} to override (the air seams are
     *  {@code LivingEntity#increaseAirSupply}/{@code decreaseAirSupply}), so
     *  this is the port's own accessor, the family's idiom at {@code
     *  EntityCultistCleric:443}, and the platform's vanilla air supply (300)
     *  drives the crab — BD row. */
    public int getMaxAirSupply() {
        return EldritchCrabAiLogic.MAX_AIR;
    }

    /** {@code :47-49} — volume 0.5 while airborne, 0.0 grounded. 1.7.10's
     *  {@code Entity#isAirborne()} is the not-on-the-ground test (plus the
     *  riding chain, which never applies here: a crab that rides is the one
     *  doing the riding, and 1.7.10's {@code isAirborne} walks UP the chain from
     *  the rider); 21.1.259 has no {@code isAirborne()}/{@code isAirbourne()} on
     *  the entity classes, so the flag is the platform's {@code onGround()}. */
    @Override
    protected float getSoundVolume() {
        return EldritchCrabAiLogic.soundVolumeFor(!this.onGround());
    }

    /** {@code :188-190} / {@code :196-198} — {@code thaumcraft:crabtalk} and
     *  {@code thaumcraft:crabdeath} are unregistered (D-004, pinned on
     *  {@link EldritchCrabAiLogic}); firing missing ids warns every tick, so
     *  the family's null rule applies. */
    @Override
    @Nullable
    public SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    @Nullable
    public SoundEvent getDeathSound() {
        return null;
    }

    /** {@code :200-202} — the spider footstep sound ({@code mob.spider.step},
     *  {@code SoundType.SPIDER} at 0.15F/1.0F) is pinned as
     *  {@link EldritchCrabAiLogic#SOUND_SPIDER} and NOT expressed: 21.1.259 has
     *  no mob-side {@code getSoundType} seam to override (the block sound type
     *  comes from the block under the feet) and no {@code SoundType.SPIDER}
     *  constant, so the crab's footsteps land on the vanilla surface sound — BD
     *  row, the shipped posture of {@code EntityTaintacle:420-422}. */

    /** The registered id, pinned so a probe can name it from the class (the
     *  family's idiom, {@code EntityCultistCleric:448}). */
    public static final ResourceLocation TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "eldritchcrab");

    // ---------------------------------------------------------------- probes

    /** The raw Flags byte (watcher 22 in the original), for the probe that pins
     *  the bit-0 write and the NBT round-trip. */
    public byte shellFlagsForProbe() {
        return this.entityData.get(DATA_FLAGS);
    }

    /** The ARMOR / MOVEMENT_SPEED bases as the shell flag left them. */
    public double armorBaseForProbe() {
        return this.getAttributeBaseValue(Attributes.ARMOR);
    }

    public double speedBaseForProbe() {
        return this.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
    }
}
