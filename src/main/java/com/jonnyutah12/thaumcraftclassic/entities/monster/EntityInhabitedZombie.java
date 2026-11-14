package com.jonnyutah12.thaumcraftclassic.entities.monster;

import javax.annotation.Nullable;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchMobs;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * {@code thaumcraft:inhabitedzombie} — original
 * {@code thaumcraft.common.entities.monster.EntityInhabitedZombie} (115 lines,
 * content-mapping {@code Thaumcraft:InhabitedZombie}, kind=entity, mod_index
 * 123, display name "Inhabited Zombie", CSV line 331), {@code extends
 * EntityZombie}. Registration {@code ConfigEntities.java:123} —
 * {@code registerModEntity(EntityInhabitedZombie.class, "InhabitedZombie", 123,
 * instance, 64, 3, true)}.
 *
 * <p>The gate-zombie: 30 health, Crimson plate on spawn (helmet always, chest
 * and legs on a {@code HARD ? 0.9 : 0.6} roll), a melee body that deals nothing
 * ({@code :33-34} is empty), no reinforcement calls ({@code :24}, the
 * reinforcement attribute at 0), a 32/16/32 spawn-refusal box against its own
 * kind ({@code :104-113}), the crab talk as its ambient ({@code :96-98}), and on
 * death it leaves a helm-wearing {@link EntityEldritchCrab} in its place
 * ({@code :57-62}) and throws its XP orbs through the vanilla split loop
 * ({@code :63-71}) before the 20 explode particles ({@code :74-88}).</p>
 *
 * <p><b>The goal set, reconciled.</b> The original adds exactly two goals in its
 * constructor ({@code :29}/{@code :30}) on top of the 1.7.10 vanilla zombie's
 * set, and 1.7.10's goal maps key on the slot — so its {@code HurtByTarget(true)}
 * at target slot 1 replaces vanilla's slot 1 and its Cultist goal at target slot
 * 3 replaces vanilla's villager goal. 1.21.1's {@code GoalSelector} keys on the
 * goal, so a slot write cannot replace. The port ships the original's net set
 * explicitly (no {@code super.registerGoals()}): the vanilla 1.21.1 movement
 * goals the 1.7.10 parent had, and the target set {hurt 1, player 2, CULTIST 3,
 * iron golem 4} — the turtle goal at 5 is 1.21.1-only growth, kept under the
 * family's vanilla-shape rule, and the turtle-egg goal (1.21.1 slot 4) and the
 * villager goal (replaced by the original at 3) are stripped. Census: 5
 * movement + 5 target goals, asserted by {@code EldritchMobGameTest}
 * ({@link InhabitedZombieAiLogic#SHIPPED_GOAL_COUNT}). The slot-3 replacement
 * is recorded as BD-119.</p>
 *
 * <p>Deviations in {@code docs/BEHAVIORAL_DEVIATIONS.md}: BD-119 (the {@code
 * :30} slot-3 replacement has no 1.21.1 seam), BD-118 (the {@code :50-52} {@code
 * getDropItem() = Item(0)} — 1.21.1's zombie loot is table-driven; the port
 * ships no loot-table override and pins the id as air), BD-120 (the {@code :36-48}
 * {@code onInitialSpawn} skips the vanilla zombie's baby/reinforcement/armor
 * randomization — the port skips {@code super.finalizeSpawn} for the same
 * reason, so the shipped spawn is the original's, never a baby).</p>
 */
public class EntityInhabitedZombie extends Zombie {

    public EntityInhabitedZombie(final EntityType<? extends Zombie> type, final Level level) {
        super(type, level);
        this.xpReward = InhabitedZombieAiLogic.XP;                                     // :66 (vanilla)
    }

    /** {@code ConfigEntities.java:123} — tracking 64 (4 chunks), interval 3. */
    public static EntityType.Builder<EntityInhabitedZombie> builder(final MobCategory category) {
        return EntityType.Builder.<EntityInhabitedZombie>of(EntityInhabitedZombie::new, category)
                .sized(InhabitedZombieAiLogic.WIDTH, InhabitedZombieAiLogic.HEIGHT)
                .eyeHeight(InhabitedZombieAiLogic.EYE_HEIGHT)
                .clientTrackingRange(InhabitedZombieAiLogic.TRACK_RANGE_CHUNKS)
                .updateInterval(InhabitedZombieAiLogic.UPDATE_INTERVAL);
    }

    public static EntityType<EntityInhabitedZombie> buildType(final MobCategory category) {
        return builder(category).build("inhabitedzombie");
    }

    /** {@code :20-25} — the parent's attributes with health 30, attack 5 and the
     *  reinforcement attribute at 0. */
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, InhabitedZombieAiLogic.MAX_HEALTH)          // :22
                .add(Attributes.ATTACK_DAMAGE, InhabitedZombieAiLogic.ATTACK_DAMAGE)    // :23
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,
                        InhabitedZombieAiLogic.SPAWN_REINFORCEMENTS);                   // :24
    }

    // ------------------------------------------------------------------ goals

    /**
     * The original's net goal set, written out (see the class javadoc's
     * reconciliation). The movement goals are the 1.7.10 vanilla zombie's at the
     * slots 1.21.1's parent uses; the target set is the original's constructor
     * ({@code :29}/{@code :30}) merged onto the vanilla slots.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(InhabitedZombieAiLogic.GOAL_SLOT_MELEE,
                new ZombieAttackGoal(this, InhabitedZombieAiLogic.MELEE_SPEED,
                        InhabitedZombieAiLogic.MELEE_LONG_MEMORY));
        this.goalSelector.addGoal(InhabitedZombieAiLogic.GOAL_SLOT_VILLAGE,
                new MoveThroughVillageGoal(this, InhabitedZombieAiLogic.VILLAGE_SPEED,
                        true, InhabitedZombieAiLogic.VILLAGE_REPATH, this::canBreakDoors));
        this.goalSelector.addGoal(InhabitedZombieAiLogic.GOAL_SLOT_WANDER,
                new WaterAvoidingRandomStrollGoal(this, InhabitedZombieAiLogic.WANDER_SPEED));
        this.goalSelector.addGoal(InhabitedZombieAiLogic.GOAL_SLOT_WATCH,
                new LookAtPlayerGoal(this, Player.class,
                        InhabitedZombieAiLogic.WATCH_PLAYER_RANGE));
        this.goalSelector.addGoal(InhabitedZombieAiLogic.GOAL_SLOT_LOOK_IDLE,
                new RandomLookAroundGoal(this));

        // :29 — EntityAIHurtByTarget(this, true): the alert-others flag the
        // vanilla 1.21.1 goal already sets for its own kind plus ZombifiedPiglin.
        this.targetSelector.addGoal(InhabitedZombieAiLogic.TARGET_SLOT_HURT,
                new HurtByTargetGoal(this).setAlertOthers(ZombifiedPiglin.class));
        this.targetSelector.addGoal(InhabitedZombieAiLogic.TARGET_SLOT_PLAYER,
                new NearestAttackableTargetGoal<>(this, Player.class, true));
        // :30 — the Cultist goal replaces the vanilla villager goal at slot 3
        // (1.7.10's slot-keyed map, 1.21.1's goal-keyed set: BD-119).
        this.targetSelector.addGoal(InhabitedZombieAiLogic.TARGET_SLOT_CULTIST,
                new NearestAttackableTargetGoal<>(this, EntityCultist.class, true));
        this.targetSelector.addGoal(InhabitedZombieAiLogic.TARGET_SLOT_IRON_GOLEM,
                new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(InhabitedZombieAiLogic.TARGET_SLOT_TURTLE,
                new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false,
                        Turtle.BABY_ON_LAND_SELECTOR));
    }

    /**
     * {@code :24} in 1.21.1's terms: 1.7.10 pins the reinforcement attribute to
     * 0.0 on the attribute map; 1.21.1's {@code Zombie} randomizes that
     * attribute at spawn ({@code randomizeReinforcementsChance}, from
     * {@code handleAttributes}), which would undo the pin. The empty override is
     * the pin: the shipped base value is {@link
     * InhabitedZombieAiLogic#SPAWN_REINFORCEMENTS} = 0, the original's live value,
     * so the inhabited zombie never calls reinforcements.
     */
    @Override
    protected void randomizeReinforcementsChance() {
    }

    // ------------------------------------------------------------- melee body

    /**
     * {@code :33-34} — {@code func_70074_a(EntityLivingBase)} (the melee hit)
     * is EMPTY: the inhabited zombie swings and lands nothing. 1.21.1's seam is
     * {@code Mob#doHurtTarget(Entity)}, and returning false without touching the
     * target is the original's empty body (no damage, no enchantment, no
     * knockback applied here; the vanilla swing animation still plays).
     */
    @Override
    public boolean doHurtTarget(final Entity target) {
        return false;                                                                  // :33-34
    }

    // ------------------------------------------------------------- equipment

    /**
     * {@code :36-48} — {@code onInitialSpawn}: the plate roll factor
     * ({@code HARD ? 0.9 : 0.6}), the helmet always, chest and legs on {@code
     * nextFloat() <= factor}, and the vanilla zombie's {@code onInitialSpawn}
     * (baby chance, reinforcement rolls, random armour) is NOT called — the
     * original returns the passed data untouched, so the port skips {@code
     * super.finalizeSpawn} (BD-120) and the shipped zombie is never a baby.
     */
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(final net.minecraft.world.level.ServerLevelAccessor level,
            final DifficultyInstance difficulty, final MobSpawnType reason,
            @Nullable final SpawnGroupData spawnData) {
        equipPlates(level.getRandom(),
                InhabitedZombieAiLogic.equipmentChance(
                        level.getDifficulty() == Difficulty.HARD));                     // :37
        return spawnData;                                                              // :47
    }

    /** The plate writer, split out as the probe seam (the ids are unregistered
     *  content, so every write is gated on the live registry — BD-68's shape,
     *  the cultists' {@code stackFor}). */
    public void equipPlates(final RandomSource random, final float chance) {
        setPlate(EquipmentSlot.HEAD, InhabitedZombieAiLogic.HELM_PLATE);                // :38
        if (InhabitedZombieAiLogic.equips(random.nextFloat(), chance)) {
            setPlate(EquipmentSlot.CHEST, InhabitedZombieAiLogic.CHEST_PLATE);          // :39-41
        }
        if (InhabitedZombieAiLogic.equips(random.nextFloat(), chance)) {
            setPlate(EquipmentSlot.LEGS, InhabitedZombieAiLogic.LEGS_PLATE);            // :43-45
        }
    }

    private void setPlate(final EquipmentSlot slot, final ResourceLocation id) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return;
        }
        this.setItemSlot(slot, new ItemStack(BuiltInRegistries.ITEM.get(id)));
    }

    // ----------------------------------------------------------------- death

    /**
     * {@code :57-91} — the death update: one {@link EntityEldritchCrab} at the
     * zombie's position at eye height, rotation copied, {@code setHelm(true)},
 * added; then the XP orbs (the vanilla mob-loot guard plus the vanilla split
     * loop, {@code :63-71}); then 20 explode particles scattered over the unit
     * box ({@code :74-88}). 1.21.1's seam is {@code LivingEntity#die}, and the
     * crab takes the zombie's place: {@code setDead()} is 1.21.1's own death
     * state, which the parent sets.
     *
     * <p>The XP loop is the original's own ({@code :63-71}): the guard is
     * {@code (attackTime > 0 || isPlayer) && canDropLoot() && doMobLoot} — the
     * player arm is inert for a zombie, so the live term is "recently hurt",
     * and 1.21.1's built-in guard is "a player landed the last hit within 600
     * ticks" ({@code lastHurtByPlayerTime}), a narrower rule. The port ships
     * the ORIGINAL's loop by suppressing the parent's whole death-drop pass
     * (the reference's empty {@code func_70645_a(DamageSource)} of {@code
     * :93-94}) and re-throwing the XP through 1.21.1's splitter, so the orb
     * figure is the original's and a mob kill throws XP as it did in 1.7.10
     * (BD-122).</p>
     */
    @Override
    public void die(final DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel serverLevel) {
            spawnCrab(serverLevel);                                                     // :58-62
            this.dropExperience(source.getEntity());                                    // :63-71 (field_70717_bb)
            for (int i = 0; i < InhabitedZombieAiLogic.DEATH_PARTICLES; i++) {
                final RandomSource r = serverLevel.getRandom();
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                        this.getX() + r.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(),
                        this.getY() + r.nextFloat() * this.getBbHeight(),
                        this.getZ() + r.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(),
                        1, InhabitedZombieAiLogic.DEATH_PARTICLE_JITTER,
                        InhabitedZombieAiLogic.DEATH_PARTICLE_JITTER,
                        InhabitedZombieAiLogic.DEATH_PARTICLE_JITTER, 0.0);             // :74-88
            }
        }
    }

    /**
     * {@code :93-94} — {@code func_70645_a(DamageSource)} ({@code onDeath}) is
     * EMPTY: the inhabited zombie suppresses the parent's whole death pass —
     * the zombie's loot table, its {@code :54-55} custom drops, its equipment
     * and its XP. 1.21.1's seam for that pass is {@code
     * LivingEntity#dropAllDeathLoot} (called from {@code die} at :1431), so the
     * empty override IS the suppression, and {@link #die} re-adds only the XP
     * the original throws by hand (BD-122). The {@code :96-98}/{@code :100-102}
     * sounds and the {@code func_70106_y()} ({@code setDead}) of {@code :90} are
     * the parent's own death state, which 1.21.1 sets on the tick.
     */
    @Override
    protected void dropAllDeathLoot(final ServerLevel level, final DamageSource source) {
        // :93-94 — the reference's body is empty; nothing runs.
    }

    /**
     * {@code :54-55} — {@code func_70628_a(boolean, int)} ({@code dropFewItems})
     * is EMPTY: no iron, no rotten flesh, no carrot, no equipment drop. 1.21.1's
     * seam is {@code Zombie#dropCustomDeathLoot}; the port ships the empty body
     * so the rule is explicit even though {@link #dropAllDeathLoot} already
     * suppresses the pass that would call it (BD-122).
     */
    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source,
            final boolean recentlyHit) {
        // :54-55 — the reference's body is empty; the vanilla zombie's drops do
        // not happen for the inhabited zombie.
    }

    /** {@code :58-62} — the crab, split out as the probe seam. */
    public void spawnCrab(final ServerLevel level) {
        final EntityEldritchCrab crab =
                new EntityEldritchCrab(CatalogEldritchMobs.ELDRITCH_CRAB.get(), level);
        crab.setPos(this.getX(),
                InhabitedZombieAiLogic.crabSpawnY(this.getY(), this.getEyeHeight()),    // :60
                this.getZ());
        crab.setYRot(this.getYRot());                                                   // :60
        crab.setXRot(this.getXRot());                                                   // :60
        crab.setShell(InhabitedZombieAiLogic.crabSpawnsWithHelm());                     // :61
        level.addFreshEntity(crab);                                                     // :62
    }

    // --------------------------------------------------------------- spawning

    /** {@code :104-113} — {@code getCanSpawnHere()}: the unit box expanded
     *  32/16/32; any inhabited zombie inside refuses the spawn, else the
     *  parent's rules. */
    @Override
    public boolean checkSpawnRules(final LevelAccessor level, final MobSpawnType reason) {
        if (!super.checkSpawnRules(level, reason)) {                                    // :113
            return false;
        }
        for (final Entity entity : level.getEntities(this, this.getBoundingBox()
                .inflate(InhabitedZombieAiLogic.SPAWN_BOX_XZ_EXPAND,
                        InhabitedZombieAiLogic.SPAWN_BOX_Y_EXPAND,
                        InhabitedZombieAiLogic.SPAWN_BOX_XZ_EXPAND))) {
            if (entity instanceof EntityInhabitedZombie && entity != this) {            // :105-112
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ sound

    /** {@code :96-98} — ambient {@code thaumcraft:crabtalk} (registered by the
     *  audio lane). {@code :100-102} — hurt {@code game.hostile.hurt} is 1.21.1's
     *  vanilla hostile hurt default, so the hurt is not overridden. */
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return ModSounds.CRABTALK.get();
    }

    /** The 32/16/32 box, as the probe seam. */
    public static boolean refusesSpawnNearKind(final int kindCount) {
        return kindCount > 0;                                                           // :113
    }
}
