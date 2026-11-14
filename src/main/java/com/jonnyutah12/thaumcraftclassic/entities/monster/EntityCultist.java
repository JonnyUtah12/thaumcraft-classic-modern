package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCultists;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The crimson cultist base — the original's
 * {@code thaumcraft.common.entities.monster.EntityCultist} (108 lines,
 * {@code extends EntityMob}), <b>shipped unregistered</b>.
 *
 * <p>The original registers four cultists and no base:
 * {@code ConfigEntities.init} carries {@code CultistKnight} (mod_index 27),
 * {@code CultistCleric} (28), {@code CultistLeader} (29) and {@code CultistPortal}
 * (30) — {@code EntityCultist.java} has no {@code registerModEntity} line, takes
 * no mod id and has no {@code docs/content-mapping.csv} row. 1.21.1 compounds
 * that: an {@code EntityType} without a supplier and a registry slot cannot be
 * placed in a level at all. So the base lands as the shared parent of the two
 * registered cultists on this card (the Leader is c5) — the shape the
 * content-mapping rows for {@code thaumcraft:cultistknight} (line 325) and
 * {@code thaumcraft:cultistcleric} (line 326) name in their notes.</p>
 *
 * <p>What the base owns, each figure cited on {@link CultistAiLogic}: the 0.6 x
 * 1.8 box and the 10 XP ({@code :18-19}), the door-breaking/swimming navigator
 * ({@code :20-21}), the attribute trio follow-range 32.0 / speed 0.3 / attack
 * 4.0 ({@code :24-29}) — health and armour are the vanilla mob defaults, the
 * base never touches them — the no-loot-pickup ({@code :35-37}) and boss
 * ({@code :39-41}) flags, the three-roll meta-carrying-item death drop
 * ({@code :47-58}) that <b>replaces</b> the parent's equipment drop (the plate
 * and robe sets of the subclasses would otherwise drop on every death), the
 * creative-gift replacement with one Crimson Rites object ({@code :60-62}), the
 * {@code HomeD}/{@code HomeX/Y/Z} persistence pair ({@code :84-99}) and the two
 * faction hooks ({@code :101-107}).</p>
 *
 * <p>Not here: {@code func_146068_u()} ({@code :43-45}) returns item id 0 (air)
 * as the pickup item — 1.21.1 has no such hook, and its pickup path
 * ({@code Mob#getPickupItem}/{@code canPickUpLoot}) is pinned false by
 * {@code :35-37}, so the pickup branch is unreachable by construction (recorded
 * deviation, BD-92); {@code func_70650_aV()} ({@code :39-41}) is the boss flag, and
 * 1.21.1 has no {@code Mob#isBoss()} — boss bars are {@code ServerBossEvent}s and
 * the cultists draw no bar, so the flag ships inert (BD-69's shape, reused
 * verbatim from the pech).</p>
 */
public abstract class EntityCultist extends Monster {

    /** {@code :81} — {@code protected int homeDist}. 1.21.1 keeps the same
     *  concept as {@code PathfinderMob#restrictCenter}/{@code restrictRadius};
     *  the fields are kept here so the original's NBT keys round-trip exactly. */
    private int homeDist;
    @Nullable
    private BlockPos homePos;

    protected EntityCultist(final EntityType<? extends EntityCultist> type, final Level level) {
        super(type, level);
        // :18 — func_70105_a(0.6F, 1.8F) rides buildType's sized(...)
        // :19 — field_70728_aV = 10 (experienceValue)
        this.xpReward = CultistAiLogic.XP;
        // :35-37 — func_98052_bS() (getCanPickUpLoot) returns false: the field
        // as well as the override, because 1.21.1 reads the field in
        // Mob#canPickUpItem (:1235) and in the zombie armour filler.
        this.setCanPickUpLoot(CultistAiLogic.CAN_PICK_UP_LOOT);
        // :20-21 — getNavigator().setCanBreakDoors(true) / setCanSwim(true).
        // 1.21.1's pair is setCanOpenDoors / setCanFloat; PathType restrictions
        // are not used by the original, so the navigator keeps the vanilla
        // ground costs.
        // 1.21.1 moved the door flag onto GroundPathNavigation (:139).
        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation g) {
            g.setCanOpenDoors(CultistAiLogic.NAVIGATOR_BREAKS_DOORS);
        }
        this.getNavigation().setCanFloat(CultistAiLogic.NAVIGATOR_SWIMS);
    }

    /**
     * {@code :24-29} — {@code func_110147_aO()}
     * ({@code entityInit}/{@code applyAttributeBaselines}): the original sets
     * exactly three attributes on the base, so the builder adds exactly three to
     * the vanilla mob baseline. Health (20.0) and armour are the baseline the
     * base never touches; {@code EntityCultistKnight:41} and
     * {@code EntityCultistCleric:51} are the two who do.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, CultistAiLogic.FOLLOW_RANGE)   // :26
                .add(Attributes.MOVEMENT_SPEED, CultistAiLogic.MOVEMENT_SPEED) // :27
                .add(Attributes.ATTACK_DAMAGE, CultistAiLogic.ATTACK_DAMAGE);  // :28
    }

    // The registration shape lives in each subclass's buildType: the original's
    // registerModEntity(instance, 64, 3, true) at ConfigEntities.java:111 (knight)
    // and :113 (cleric) — tracking 64 blocks, update interval 3, velocity sync.
    // 1.21.1's clientTrackingRange counts chunks, so the 64 is 4 (the family rule
    // pinned at BrainyZombieAiLogicTest:38); the 3 rides verbatim as
    // entityDataSyncInterval. The base has no registration of its own.

    // ------------------------------------------------------------- base flags

    /** {@code :35-37} — the original overrides the getter, so the port does too. */
    @Override
    public boolean canPickUpLoot() {
        return CultistAiLogic.CAN_PICK_UP_LOOT;
    }

    /** {@code :39-41} — the original's boss flag. 1.21.1 has no
     *  {@code Mob#isBoss()}; the flag is pinned on {@link CultistAiLogic#IS_BOSS}
     *  and ships inert (BD-69, the pech's shape). */
    public static boolean isBossEntity() {
        return CultistAiLogic.IS_BOSS;
    }

    /** {@code :43-45} — {@code func_146068_u()} (the pickup item) is item id 0,
     *  air. Pinned, not overridden: 1.21.1's pickup path is closed by
     *  {@link #canPickUpLoot()} (see the class javadoc). */
    public static final ResourceLocation PICKUP_ITEM_ID =
            ResourceLocation.withDefaultNamespace("air");

    // ---------------------------------------------------------- death drops

    /**
     * {@code :47-58} — {@code func_70628_a(boolean, int)}
     * ({@code dropFewItems(killedByPlayer, looting)}). One roll of
     * {@code nextInt(10)}: 0 → {@code itemResource} meta 9, ≤1 → meta 17,
     * ≤{@code 3 + looting} → meta 18, else nothing; every drop at the 1.5
     * offset.
     *
     * <p>This override <b>replaces</b> the parent's death loot: 1.21.1's
     * {@code Mob#dropCustomDeathLoot} is the equipment-drop seam, and the
     * original's override calls no super — the plate set of the knight
     * ({@code EntityCultistKnight:46-49}) and the robe set of the cleric
     * ({@code EntityCultistCleric:56-59}) would otherwise drop on every death.
     * The port calls no super for the same reason.</p>
     *
     * <p>The meta-carrying item is the family's {@code thaumcraft:itemresource},
     * which this tree has not registered (content-mapping row 158). Per
     * docs/testing.md the original's unregistered items ride a live
     * {@code BuiltInRegistries} check (BD-68's shape): when the item lands, the
     * meta drop fires, the probe turns on it, and no item is invented.</p>
     */
    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source,
            final boolean recentlyHit) {
        // 1.21.1’s hook (Mob:979) has no looting parameter: the level arrives
        // inside EnchantmentHelper.processEquipmentDropChance (:990) for the
        // equipment drop the original suppresses. The roll table reads LOOTING 0,
        // the floor of the original’s 3 + looting window (BD-88).
        rollCultistDrops(level, CultistAiLogic.DROP_LOOTING_BASELINE);               // :48-55
    }

    /**
     * {@code :47-58} as a seam so the game test can drive the roll directly.
     * The drop is a real world drop when the item is registered; the unit test
     * pins the roll table on {@link CultistAiLogic#dropMetaFor(int, int)}.
     */
    public final void rollCultistDrops(final ServerLevel level, final int looting) {
        final int roll = this.random.nextInt(CultistAiLogic.DROP_ROLL_MODULO);    // :48
        final int meta = CultistAiLogic.dropMetaFor(roll, looting);               // :50/:52/:54
        if (meta < 0) {
            return;
        }
        dropCultistItem(level, CultistAiLogic.DROP_ITEM, meta, CultistAiLogic.DROP_THROW_OFFSET);
    }

    /**
     * {@code :60-62} — {@code func_82160_m(int)} ({@code dropAllLoot}) replaces
     * the parent's equipment drop with one {@code thaumcraft:itemeldritchobject}
     * meta 1 (Crimson Rites) at the 1.0 offset. 1.21.1's hook for the
     * creative-instant-kill gift is {@code LivingEntity#grantLastDeathLoot()}.
     */
    /** 1.21.1 has no creative-kill gift hook (LivingEntity carries no
     *  {@code grantLastDeathLoot}/{@code dropAllLoot} seam), so the Crimson Rites gift
     *  ships as the named seam and is NOT wired (BD-89): its item id, meta, offset
     *  and the cleric’s LOOTING gate are pinned and unit-tested, and the caller is
     *  the death-path card that owns 1.21.1’s gift seam. */
    protected void dropAllLoot() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (!dropsLoot()) {
            return;                                                              // cleric :92-94
        }
        dropCultistItem(level, CultistAiLogic.ALL_LOOT_ITEM, CultistAiLogic.ALL_LOOT_META,
                CultistAiLogic.ALL_LOOT_THROW_OFFSET);                            // :61
    }

    /** The LOOTING gate of {@code EntityCultistCleric:92-94}; the base always drops. */
    protected boolean dropsLoot() {
        return true;
    }

    /**
     * One meta-carrying drop, gated on the live item registry (BD-68's shape).
     * 1.7.10's metas ride the item's damage value; this port's convention is the
     * {@code thaumcraft:item_meta} data component
     * (docs/architecture/registration.md ITEM_META), so the drop carries the
     * meta in the component the port's items read.
     */
    private void dropCultistItem(final ServerLevel level, final ResourceLocation id,
            final int meta, final float offset) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return;
        }
        final Item item = BuiltInRegistries.ITEM.get(id);
        final ItemStack stack = new ItemStack(item);
        if (stack.isEmpty()) {
            return;
        }
        this.spawnAtLocation(withMeta(stack, meta), offset);
    }

    /** 1.7.10's metas ride the item's damage value; this port's convention is the
     *  {@code thaumcraft:item_meta} data component (CatalogWands:315 is the
     *  shipped writer), so the drop carries the meta where the port's items read it. */
    private static ItemStack withMeta(final ItemStack stack, final int meta) {
        final ItemStack copy = stack.copy();
        copy.set(ModDataComponents.ITEM_META, meta);
        return copy;
    }

    // --------------------------------------------------------------- faction

    /**
     * {@code :101-103} — {@code func_142014_c(EntityLivingBase)}
     * ({@code canAttack}): a cultist may attack a cultist. This is the hook
     * {@code AICultistHurtByTarget} relies on to hand the gang's aggro to the
     * attacker — the mob's own {@code canAttack} is the 1.21.1 form
     * ({@code LivingEntity#canAttack(LivingEntity)}, {@code :899}).
     */
    @Override
    public boolean canAttack(final LivingEntity target) {
        return CultistAiLogic.canAttackFactionMember(target) || super.canAttack(target);
    }

    /**
     * {@code :105-107} — {@code func_70686_a(Class)} returns false for the
     * Cleric, the Leader and the Knight so the target selectors skip their own
     * kind. 1.21.1's hook is the {@code EntityType} form
     * ({@code Mob#canAttackType(EntityType<?>)}), so the decision reads the
     * registry id — the base never spawns a type to ask it (1.21.1's
     * {@code EntityType#create} returns Optional.empty() for a type with no
     * supplier, and the base has none).
     */
    @Override
    public boolean canAttackType(final EntityType<?> type) {
        if (isCultistType(type)) {
            return false;
        }
        return super.canAttackType(type);
    }

    // ----------------------------------------------------------- home anchor

    /** {@code :64-66} — {@code func_110158q(int)} (getHomeDist). */
    public int getHomeDist() {
        return this.homeDist;
    }

    /** {@code :68-71} — {@code func_110157n()} (getHomePosition), null when unset. */
    @Nullable
    public BlockPos getHomePosition() {
        return this.homePos;
    }

    /**
     * {@code :73-75} — {@code func_110164a(EntityLivingBase, int)}
     * ({@code setHomePosition(EntityLivingBase, int)}): the home is the other
     * mob's position. Used by {@code AIAltarFocus} when a cleric re-adopts an
     * altar ({@code AIAltarFocus.java:70}).
     */
    public void setHomePosition(final LivingEntity at, final int distance) {
        setHomePosition(at.blockPosition(), distance);                            // :74
    }

    /** {@code :77-80} — {@code func_110155e(BlockPos, int)} (the position form). */
    public void setHomePosition(final BlockPos pos, final int distance) {
        this.homePos = pos.immutable();                                           // :78-79
        this.homeDist = distance;                                                 // :80
        // 1.21.1's own restriction, so MoveTowardsRestrictionGoal (goal slot 6)
        // reads the same anchor the original's goal did.
        this.restrictTo(this.homePos, distance);
    }

    /** {@code :84-92} — {@code func_70037_a} reads {@code HomeD} only when the
     *  tag is present, then the three coordinates. */
    @Override
    public void addAdditionalSaveData(final CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        // :93-98 — func_70014_b: emit HomeD, and HomeX/Y/Z only when a home
        // exists and its radius is positive.
        compound.putInt(CultistAiLogic.NBT_HOME_D, this.homeDist);
        if (CultistAiLogic.writesHome(this.homePos != null, this.homeDist)) {
            compound.putInt(CultistAiLogic.NBT_HOME_X, this.homePos.getX());
            compound.putInt(CultistAiLogic.NBT_HOME_Y, this.homePos.getY());
            compound.putInt(CultistAiLogic.NBT_HOME_Z, this.homePos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(CultistAiLogic.NBT_HOME_D)) {                       // :86
            this.homeDist = compound.getInt(CultistAiLogic.NBT_HOME_D);           // :87
            this.homePos = new BlockPos(                                          // :88-90
                    compound.getInt(CultistAiLogic.NBT_HOME_X),
                    compound.getInt(CultistAiLogic.NBT_HOME_Y),
                    compound.getInt(CultistAiLogic.NBT_HOME_Z));
            this.restrictTo(this.homePos, this.homeDist);
        }
    }

    // ---------------------------------------------------------- equipment

    /**
     * The shared plate/robe writer. The original's
     * {@code func_70062_b(slot, stack)} takes 1.7.10's armour slot ordinals;
     * 1.21.1 wants an {@code EquipmentSlot}, and
     * {@code Mob#populateDefaultEquipmentSlots} is the seam that 1.21.1 calls
     * from {@code finalizeSpawn}, which is where 1.7.10's
     * {@code onInitialSpawn} → {@code addRandomEquipment} ran
     * ({@code EntityCultistKnight:45}, {@code EntityCultistCleric:55}).
     */
    protected final void equipFromTable(final java.util.Map<Integer, ResourceLocation> table) {
        for (final java.util.Map.Entry<Integer, ResourceLocation> entry : table.entrySet()) {
            final net.minecraft.world.entity.EquipmentSlot slot =
                    slotForOrdinal(entry.getKey());
            if (slot == null) {
                continue;
            }
            this.setItemSlot(slot, stackFor(entry.getValue()));
        }
    }

    /** The 1.7.10 armour ordinals: 0 mainhand, 1 boots, 2 legs, 3 chest, 4 helmet. */
    @Nullable
    static net.minecraft.world.entity.EquipmentSlot slotForOrdinal(final int ordinal) {
        return switch (ordinal) {
            case CultistAiLogic.SLOT_MAINHAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case CultistAiLogic.SLOT_BOOTS -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case CultistAiLogic.SLOT_LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case CultistAiLogic.SLOT_CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case CultistAiLogic.SLOT_HELMET -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            default -> null;
        };
    }

    /**
     * A stack for an id, or EMPTY when the id is not registered. The cultist
     * armour and weapon ids ({@code thaumcraft:helmcultistplate},
     * {@code thaumcraft:swordvoid}, …) are unregistered content — the ledger
     * rows for the cultist armour set and the void/thaumium weapon set are
     * {@code not_started} — so every equipment write is gated on the live
     * registry (BD-68's shape, docs/testing.md) and nothing is invented.
     */
    protected static ItemStack stackFor(final ResourceLocation id) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    /** The ambient sound of the family: the original's {@code thaumcraft:chant}
     *  ({@code EntityCultistCleric:173-175}) is an unregistered sound event —
     *  the audio lane owns the registry (docs/sound-events.md, D-004) — and
     *  firing a missing id warns on every ambient tick, so the family returns
     *  null and the id is pinned on {@link CultistAiLogic#SOUND_AMBIENT}. */
    @Nullable
    @Override
    public SoundEvent getAmbientSound() {
        return null;
    }

    /** {@code :81} — the home distance field is public in the original; the
     *  setter is the {@code setHomePosition} pair's twin. */
    public void setHomeDist(final int distance) {
        this.homeDist = distance;
    }

    /**
     * The {@code :105-107} exclusion in the target's {@code EntityType} form:
     * the live registry decides it, so the pair on this card plus the Leader on
     * card c5 all read true with no cross-card edit.
     */
    public boolean isCultistType(final EntityType<?> type) {
        return CatalogCultists.isCultistType(type);
    }

}
