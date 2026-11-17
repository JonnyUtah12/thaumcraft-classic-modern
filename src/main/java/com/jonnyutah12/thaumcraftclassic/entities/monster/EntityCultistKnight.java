package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

import net.minecraft.world.level.ServerLevelAccessor;

import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AICultistHurtByTarget;

/**
 * The Crimson Knight — {@code thaumcraft:cultistknight} (content-mapping
 * {@code Thaumcraft:CultistKnight}, CSV line 325, mod_index 27, display name
 * "Crimson Knight"), the original's
 * {@code thaumcraft.common.entities.monster.EntityCultistKnight} (73 lines,
 * {@code extends EntityCultist}). Registration
 * {@code ConfigEntities.java:111} — {@code registerModEntity(..., 64, 3, true)}.
 *
 * <p>The knight is the base plus four things: a health override (36.0,
 * {@code :41}) — no attack override, no armour value, its defence is the plate
 * it wears — the plate set ({@code :45-49}), the weapon roll ({@code :50-62}),
 * and a light-gated weapon enchantment ({@code :66-72}). The goal set is the
 * family's ({@code :25-35}, slots and parameters on
 * {@link CultistAiLogic.GOAL_SLOT_*}).</p>
 *
 * <p>The equipment writes are gated on the live item registry:
 * {@code thaumcraft:helmcultistplate}, {@code chestcultistplate},
 * {@code legscultistplate}, {@code bootscultist}, {@code swordvoid} and
 * {@code swordthaumium} are unregistered content (their ledger rows are
 * {@code not_started}), so each branch lights up when its item lands, with no
 * invented stand-in (BD-68's shape, docs/testing.md). The one real item in the
 * roll is {@code minecraft:iron_sword} ({@code :62},
 * {@code Items.field_151040_l}), which ships today. The roll's arithmetic is
 * pinned world-free on {@link CultistAiLogic}.</p>
 */
public class EntityCultistKnight extends EntityCultist {

    public EntityCultistKnight(final EntityType<? extends EntityCultistKnight> type,
            final Level level) {
        super(type, level);
    }

    /**
     * {@code ConfigEntities.java:111} — {@code registerModEntity(EntityCultistKnight.class,
     * "CultistKnight", 27, instance, 64, 3, true)}: tracking 64 blocks (4
     * chunks), update interval 3, velocity updates on. The box is the base's
     * 0.6 x 1.8 ({@code EntityCultist:18}).
     */
    public static EntityType<EntityCultistKnight> buildType(final MobCategory category) {
        return EntityType.Builder.<EntityCultistKnight>of(EntityCultistKnight::new, category)
                .sized(CultistAiLogic.WIDTH, CultistAiLogic.HEIGHT)
                .clientTrackingRange(CultistAiLogic.TRACK_RANGE_CHUNKS)
                .updateInterval(CultistAiLogic.UPDATE_INTERVAL)
                .setShouldReceiveVelocityUpdates(CultistAiLogic.VELOCITY_UPDATES)
                .build("cultistknight");
    }

    public static AttributeSupplier.Builder createAttributes() {
        // :41 — maxHealth 36.0 over the base's three; the knight adds no attack
        // and no armour value.
        return EntityCultist.createAttributes()
                .add(Attributes.MAX_HEALTH, CultistAiLogic.KNIGHT_MAX_HEALTH);
    }

    /**
     * The spawn pass, wired. The original reaches its two passes from
     * {@code EntityLiving#onInitialSpawn}: the equipment roll
     * ({@code EntityCultistKnight.java:45-64} {@code func_82164_bB},
     * {@code addRandomEquipment}) and the light-gated weapon enchantment
     * ({@code :66-72} {@code func_82162_bC}, {@code enchantItemWithRandom}).
     *
     * <p>1.21.1 does NOT reach them from {@code Mob#finalizeSpawn}: that body
     * (target jar, decompiled {@code Mob.java:1192-1204}) only adds the
     * follow-range spawn modifier, rolls the left-hand flag and records the
     * spawn type. Every mob that carries equipment calls the pair from its OWN
     * override — {@code Vindicator.java:131-132}, {@code Piglin.java:215-216},
     * {@code AbstractSkeleton.java:139}, {@code Fox.java:340} — in the original's
     * order, slots first then enchantments. The knight follows that idiom so
     * {@code :45} and {@code :66} run on the same spawn pass they ran on in 1.7.10
     * (recorded deviation BD-98: the seam is the per-mob override, not the base's
     * {@code finalizeSpawn}).</p>
     */
    @Override
    public SpawnGroupData finalizeSpawn(final ServerLevelAccessor level,
            final DifficultyInstance difficulty, final MobSpawnType spawnType,
            @Nullable final SpawnGroupData groupData) {
        final SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, groupData);
        final RandomSource random = level.getRandom();
        this.populateDefaultEquipmentSlots(random, difficulty);                 // :45-64
        this.populateDefaultEquipmentEnchantments(level, random, difficulty);   // :66-72
        return data;
    }

    /**
     * {@code :25-35} — {@code func_70622_b} (the goal set). Slot-for-slot, the
     * original's order:
     *
     * <pre>
     *   0  EntityAISwimming                     → FloatGoal
     *   3  AIAttackOnCollide(EntityLivingBase, 1.0, false) → MeleeAttackGoal
     *   4  EntityAIRestrictOpenDoor             → OpenDoorGoal(this, false)
     *   5  EntityAIOpenDoor(this, true)         → OpenDoorGoal(this, true)
     *   6  EntityAIMoveTowardsRestriction(0.8)  → MoveTowardsRestrictionGoal
     *   7  EntityAIWander(0.8)                  → RandomStrollGoal
     *   8  EntityAIWatchClosest(Player, 8.0F)   → LookAtPlayerGoal (0.02 chance)
     *   8  EntityAILookIdle                     → RandomLookAroundGoal
     *   target 1 AICultistHurtByTarget(this, true)
     *   target 2 EntityAINearestAttackableTarget(Player, 0, true)
     * </pre>
     *
     * <p>The melee goal's {@code EntityLivingBase} class argument is 1.21.1's
     * {@code MeleeAttackGoal}, which follows the mob's target selector — the
     * faction pair on target slots 1/2 is what puts a target in it, and
     * {@link EntityCultist#canAttack} admits the faction (the original's
     * {@code :101-103}).</p>
     *
     * <p>Slot 4 is the one goal with no 1.21.1 counterpart:
     * {@code EntityAIRestrictOpenDoor} closes doors behind the mob and has no
     * analogue in the target's goal library, so the port installs the
     * {@code closeDoor=false} variant of {@code OpenDoorGoal} at the original's
     * slot and keeps slot 5's {@code closeDoor=true} form. Both share the
     * movement flag exactly as the original's pair contended for {@code MOVE},
     * so the higher slot wins while it can run. Recorded deviation (BD-92).</p>
     *
     * <p>Slots 8 and 9 are the original's collision at {@code :32}/{@code :33}:
     * 1.7.10's {@code GoalSelect} is a {@code TreeMap} keyed by priority, so the
     * {@code EntityAIWatchClosest} of {@code :32} is REPLACED by the
     * {@code EntityAILookIdle} of {@code :33} and the knight nets seven movement
     * goals on top of the swim goal. 1.21.1's {@code GoalSelector} keeps every
     * goal, so the port installs the pair at distinct slots and the family ships
     * both. Recorded deviation (BD-112), measured on the cleric's goal set by
     * {@code CultistClericGameTest.theClericGoalSetIsTheFamilySetPlusTheAltarAndRangedGoals}.</p>
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_SWIM, new FloatGoal(this));   // :26
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_MELEE,
                new MeleeAttackGoal(this, CultistAiLogic.MELEE_SPEED,
                        CultistAiLogic.MELEE_LONG_MEMORY));                              // :27
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_DOOR_RESTRICT,
                new OpenDoorGoal(this, false));                                          // :28
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_DOOR_OPEN,
                new OpenDoorGoal(this, true));                                           // :29
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_RESTRICTION,
                new MoveTowardsRestrictionGoal(this, CultistAiLogic.RESTRICTION_SPEED));  // :30
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_WANDER,
                new RandomStrollGoal(this, CultistAiLogic.WANDER_SPEED));                 // :31
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_WATCH,
                new LookAtPlayerGoal(this, Player.class, CultistAiLogic.WATCH_PLAYER_RANGE,
                        CultistAiLogic.WATCH_PLAYER_PROBABILITY));                        // :32
        this.goalSelector.addGoal(CultistAiLogic.GOAL_SLOT_LOOK_IDLE,
                new RandomLookAroundGoal(this));                                         // :33
        this.targetSelector.addGoal(CultistAiLogic.TARGET_SLOT_HURT,
                // Knight:34 — AICultistHurtByTarget(this, true), the family goal:
                // the faction re-share (the base's canAttack/canAttackType pair),
                // the agressors table, the warp gate and the 64-block shock wave,
                // all cited line by line on the class and on CultistAiLogic.
                new AICultistHurtByTarget(this, CultistAiLogic.IS_AGRESSORS));  // :34
        this.targetSelector.addGoal(CultistAiLogic.TARGET_SLOT_PLAYER,
                new NearestAttackableTargetGoal<>(this, Player.class, true));            // :35
    }

    /**
     * {@code :45-64} — {@code func_70634_aV()}
     * ({@code addRandomEquipment}), reached from the original's
     * {@code onInitialSpawn} ({@code :45}) and from 1.21.1's
     * {@code Mob#populateDefaultEquipmentSlots} in the same spawn pass.
     *
     * <p>The plate ({@code :46-49}) goes on unconditionally. Then the elite
     * roll ({@code :50}): {@code 0.05F} on HARD, {@code 0.01F} otherwise.
     * Elites roll {@code nextInt(5)} ({@code :51}): roll 0 takes the void sword
     * and the robe helmet ({@code :52-53}); anything else takes the thaumium
     * sword and, on {@code nextBoolean()}, no helmet at all ({@code :56-59}).
     * Non-elites take the iron sword ({@code :62}).</p>
     */
    @Override
    protected void populateDefaultEquipmentSlots(final RandomSource random,
            final DifficultyInstance difficulty) {
        equipFromTable(CultistAiLogic.knightPlate());                                    // :46-49
        final boolean hard = difficulty.isHard();                 // :50
        if (CultistAiLogic.knightIsElite(random.nextFloat(), hard)) {                    // :50
            final int eliteRoll = random.nextInt(CultistAiLogic.KNIGHT_ELITE_ROLL);      // :51
            if (CultistAiLogic.knightCarriesVoidSword(eliteRoll)) {                      // :52
                setSlotIfReal(CultistAiLogic.SLOT_MAINHAND, CultistAiLogic.SWORD_VOID);
                setSlotIfReal(CultistAiLogic.SLOT_HELMET, CultistAiLogic.HELMET_CULTIST_ROBE);
            } else {                                                                      // :56-59
                setSlotIfReal(CultistAiLogic.SLOT_MAINHAND, CultistAiLogic.SWORD_THAUMIUM);
                if (random.nextBoolean()) {
                    this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                }
            }
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, stackFor(CultistAiLogic.SWORD_IRON)); // :62
        }
    }

    /**
     * {@code :66-72} — {@code func_70640_b(DifficultyInstance, Random)}:
     * with a held item, on {@code rand() < 0.25F * light} (the light at the
     * knight's feet), the weapon is enchanted at
     * {@code (int)(5.0F + light * rand.nextInt(18))}.
     *
     * <p>1.21.1's seam for the same moment in the same spawn pass is
     * {@code Mob#populateDefaultEquipmentEnchantments}. 1.7.10's
     * {@code EnchantmentHelper.addRandomEnchantment(rand, stack, levels)} adds
     * one enchantment; the target's public equivalent is
     * {@code EnchantmentHelper.enchantItem}, whose {@code selectEnchantment}
     * rolls against the same level figure and can spend fewer points (and can
     * turn a book into an enchanted book, which never happens here). Recorded
     * deviation (BD-93): the level figure and the light gate are exact, the
     * one-enchantment-per-roll shape is not.</p>
     */
    @Override
    protected void populateDefaultEquipmentEnchantments(final net.minecraft.world.level.ServerLevelAccessor level,
            final RandomSource random, final DifficultyInstance difficulty) {
        final ItemStack weapon = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (weapon.isEmpty()) {                                                          // :69
            return;
        }
        final float light = lightAtFeet();                                               // :68
        if (!CultistAiLogic.knightEnchantsWeapon(random.nextFloat(), light)) {           // :69
            return;
        }
        final int levels = CultistAiLogic.knightEnchantLevel(light,
                random.nextInt(CultistAiLogic.ENCHANT_LEVEL_ROLL));                      // :70
        // 21.1.259's 5-arg form takes an Optional<HolderSet<Enchantment>> for the
        // eligible set (javap: enchantItem(RandomSource, ItemStack, int,
        // RegistryAccess, Optional<? extends HolderSet<Enchantment>>)), so the
        // vanilla mob-spawn tag resolves as a NAMED TAG SET, not as a holder:
        // Registry#getTag, and the whole enchantable set when the tag is absent.
        EnchantmentHelper.enchantItem(random, weapon, levels, level.registryAccess(),
                level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                        .getTag(net.minecraft.tags.EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT)); // :70
    }

    /**
     * {@code :68} — {@code world.func_147462_b(x, y, z)} is the original's
     * {@code getLightFromNeighbors}, whose 1.21.1 body is the light engine's
     * raw brightness with a 10-block darkening allowance.
     */
    private float lightAtFeet() {
        return this.level().getLightEngine().getRawBrightness(this.blockPosition(), 10);
    }

    private void setSlotIfReal(final int ordinal, final ResourceLocation id) {
        final EquipmentSlot slot = slotForOrdinal(ordinal);
        if (slot == null) {
            return;
        }
        final ItemStack stack = stackFor(id);
        if (!stack.isEmpty()) {
            this.setItemSlot(slot, stack);
        }
    }

    /** The knight's own id, for probes that resolve the type from the live registry. */
    public static final ResourceLocation TYPE_ID =
            ResourceLocation.parse("thaumcraft:cultistknight");

    /** Live-registry check, the BD-68 shape. */
    public static boolean isWeaponRegistered(final ResourceLocation id) {
        return BuiltInRegistries.ITEM.containsKey(id);
    }

    /** The mob's category is the family's: {@code MONSTER}. */
    public static MobCategory category() {
        return MobCategory.MONSTER;
    }

    /** {@code Monster}-typed alias for the attribute wiring line. */
    public static AttributeSupplier.Builder monsterAttributes() {
        return Monster.createMonsterAttributes();
    }
}
