package com.jonnyutah12.thaumcraftclassic.entities.golems;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.level.Level;

/**
 * The golem entity substrate — {@code thaumcraft:golem}, the original's
 * {@code thaumcraft.common.entities.golems.EntityGolemBase}
 * (content-mapping.csv {@code Thaumcraft:Golem}, kind=entity, mod_index 17).
 *
 * <p><b>Scope.</b> This card (TC-M6-08-s) owns the entity type, its
 * attributes and dimensions and the construction state the placer fills in:
 * the eight-material variant, the advanced flag, the animation core, the
 * decoration string, the upgrade array, the home facing and the owner name.
 * The original's behaviour — the animation-core task system, {@code
 * GolemHelper} markers, feeding, carry limits, persistence and the goal
 * installation inside {@code setupGolem()} — is TC-M6-08-s-b; this class
 * deliberately installs <b>no</b> goal and reproduces only the attribute half
 * of {@code setupGolem()} ({@link #applyMaterialTraits()}). Nothing here is
 * registered as a natural spawn: the original registers the golem through
 * {@code ConfigEntities.init} only, with no biome spawn list, so this port
 * registers the type alone.</p>
 *
 * <h2>Numbers, all from the decompiled source</h2>
 * <ul>
 *   <li>{@code EntityGolemBase} constructor: {@code func_70105_a(0.4F, 0.95F)}
 *       (hitbox), {@code field_70138_W = 1.0F} (step height, 1.21.1's
 *       {@code Attributes.STEP_HEIGHT}), {@code func_110163_bv()}
 *       ({@code setPersistenceRequired}), and the navigation flags
 *       {@code func_75498_b(true)} / {@code func_75490_c(true)} /
 *       {@code func_75495_e(true)} (avoid-water / swim / enter-doors; those are
 *       {@code GroundPathNavigation} state and belong to the runtime card).</li>
 *   <li>{@code func_110147_ax()}: max health 20.0, attack damage 1.0 (the
 *       original registers the attribute and then sets it), movement speed 0.6,
 *       follow range 32.0.</li>
 *   <li>{@code ConfigEntities.init}: {@code EntityRegistry.registerModEntity(
 *       EntityGolemBase.class, "Golem", id++, Thaumcraft.instance, 64, 3, true)}
 *       — tracking range 64, update interval 3.</li>
 * </ul>
 *
 * <p><b>1.7.10 → 1.21.1 shapes.</b> The original extends the vanilla
 * {@code net.minecraft.entity.monster.EntityGolem}; 1.21.1's equivalent base is
 * {@code AbstractGolem}. The original's {@code field_70178_ae = true} (per
 * material) becomes an override of {@code Entity.fireImmune()}, since 1.21.1
 * has no per-instance immunity flag. The original's synched-data indices
 * (17 owner, 19 material, 20 decoration, 21 core) become named accessors; the
 * 30/health-percentage accessor and the upgrades-as-hex-string accessor are
 * renderer state and stay with TC-M6-08-s-c.</p>
 */
public class EntityGolemBase extends AbstractGolem {

    /** Original {@code func_70105_a(0.4F, 0.95F)} — the entity type's width. */
    public static final float WIDTH = 0.4F;
    /** Original {@code func_70105_a(0.4F, 0.95F)} — the entity type's height. */
    public static final float HEIGHT = 0.95F;

    /** Original {@code func_110147_ax}: {@code MAX_HEALTH = 20.0}. */
    public static final double BASE_HEALTH = 20.0;
    /** Original {@code func_110147_ax}: {@code ATTACK_DAMAGE = 1.0}. */
    public static final double BASE_ATTACK_DAMAGE = 1.0;
    /** Original {@code func_110147_ax}: {@code MOVEMENT_SPEED = 0.6}. */
    public static final double BASE_MOVEMENT_SPEED = 0.6;
    /** Original {@code func_110147_ax}: {@code FOLLOW_RANGE = 32.0}. */
    public static final double BASE_FOLLOW_RANGE = 32.0;

    /** Original constructor {@code field_70138_W = 1.0F} — the step height. */
    public static final double STEP_HEIGHT = 1.0;

    /** Original {@code registerModEntity(..., 64, 3, true)} — client tracking range. */
    public static final int TRACKING_RANGE = 64;
    /** Original {@code registerModEntity(..., 64, 3, true)} — update interval. */
    public static final int UPDATE_INTERVAL = 3;

    /**
     * Original {@code ItemGolemPlacer.spawnCreature}:
     * {@code golem.func_110171_b(x, y, z, 32)} — {@code setHomeArea(..., 32)}.
     * 1.21.1's equivalent is {@code Mob.restrictTo(BlockPos, int)}.
     */
    public static final int HOME_RANGE = 32;

    /** Original synched-data index 19: the material ordinal ({@code golemType = EnumGolemType.WOOD} by default). */
    private static final EntityDataAccessor<Integer> DATA_MATERIAL =
            SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.INT);
    /** The original's plain {@code advanced} field, carried in spawn data ({@code writeSpawnData}). */
    private static final EntityDataAccessor<Boolean> DATA_ADVANCED =
            SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.BOOLEAN);
    /** Original synched-data index 21: the animation core id, {@code -1} when empty. */
    private static final EntityDataAccessor<Integer> DATA_CORE =
            SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.INT);
    /** Original synched-data index 20: the decoration string ({@code ""} when none). */
    private static final EntityDataAccessor<String> DATA_DECORATION =
            SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.STRING);
    /** Original synched-data index 17: the owner's name. */
    private static final EntityDataAccessor<String> DATA_OWNER =
            SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.STRING);

    /** Original {@code public byte[] upgrades = new byte[]{-1}}. */
    private byte[] upgrades = new byte[] {-1};

    /** Original {@code homeFacing} — the face of the block the golem was placed against. */
    private int homeFacing;

    public EntityGolemBase(final EntityType<? extends EntityGolemBase> type, final Level level) {
        super(type, level);
        // Original constructor: this.func_110163_bv() (enablePersistence).
        this.setPersistenceRequired();
    }

    /**
     * The original {@code func_110147_ax()} attribute set verbatim, plus the
     * constructor's step height. 1.21.1 takes a step height as an attribute
     * ({@code Attributes.STEP_HEIGHT}, default 0.6), which is where the
     * original's {@code field_70138_W = 1.0F} lands.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, BASE_FOLLOW_RANGE)
                .add(Attributes.STEP_HEIGHT, STEP_HEIGHT);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // Original field default: public EnumGolemType golemType = EnumGolemType.WOOD.
        builder.define(DATA_MATERIAL, EnumGolemType.WOOD.ordinal());
        builder.define(DATA_ADVANCED, false);
        builder.define(DATA_CORE, -1);
        builder.define(DATA_DECORATION, "");
        builder.define(DATA_OWNER, "");
    }

    // ------------------------------------------------------------- material ---

    /**
     * Original {@code getGolemType()} ({@code EnumGolemType.getType(af.get(19))}).
     * The original could return null for a corrupt value; the port falls back to
     * {@link EnumGolemType#WOOD} (the original's own field default) so nothing
     * downstream can dereference null.
     */
    public EnumGolemType getMaterial() {
        final SynchedEntityData data = this.getEntityData();
        if (data == null) {
            // Only reachable while the superclass constructor is still running,
            // before the synched-data builder is built.
            return EnumGolemType.WOOD;
        }
        final EnumGolemType material = EnumGolemType.getType(data.get(DATA_MATERIAL));
        return material == null ? EnumGolemType.WOOD : material;
    }

    /** Sets the material variant (original {@code golemType = type} in the placer's constructor). */
    public void setMaterial(final EnumGolemType material) {
        this.getEntityData().set(DATA_MATERIAL, material.ordinal());
    }

    public boolean isAdvanced() {
        return this.getEntityData().get(DATA_ADVANCED);
    }

    /** Original {@code advanced} (set from the placer stack's {@code advanced} tag). */
    public void setAdvanced(final boolean advanced) {
        this.getEntityData().set(DATA_ADVANCED, advanced);
    }

    /** Original {@code getCore()}: the animation core id, {@code -1} when the golem has none. */
    public int getCore() {
        final SynchedEntityData data = this.getEntityData();
        return data == null ? -1 : data.get(DATA_CORE);
    }

    /** Original {@code setCore(byte)}. */
    public void setCore(final int core) {
        this.getEntityData().set(DATA_CORE, core);
    }

    /** Original {@code getGolemDecoration()}. */
    public String getDecoration() {
        final SynchedEntityData data = this.getEntityData();
        return data == null ? "" : data.get(DATA_DECORATION);
    }

    /**
     * Original {@code setGolemDecoration(String)} — note the original writes its
     * {@code decoration} field to the synched slot and ignores its parameter, so
     * the port keeps one value and writes both.
     */
    public void setDecoration(final String decoration) {
        this.getEntityData().set(DATA_DECORATION, decoration);
    }

    /** Original {@code getOwnerName()}. */
    public String getOwnerName() {
        final SynchedEntityData data = this.getEntityData();
        return data == null ? "" : data.get(DATA_OWNER);
    }

    /** Original {@code setOwner(String)}. */
    public void setOwnerName(final String owner) {
        this.getEntityData().set(DATA_OWNER, owner);
    }

    public int getHomeFacing() {
        return this.homeFacing;
    }

    /** Original {@code setup(side)}: {@code this.homeFacing = side}. */
    public void setHomeFacing(final int side) {
        this.homeFacing = side;
    }

    // -------------------------------------------------------------- upgrades ---

    public byte[] upgrades() {
        return this.upgrades;
    }

    /**
     * The upgrade slot count: the original sizes the array as
     * {@code golemType.upgrades + (advanced ? 1 : 0)} (constructor
     * {@code EntityGolemBase(World, EnumGolemType, boolean)}).
     */
    public int upgradeSlotCount() {
        return this.getMaterial().upgrades + (this.isAdvanced() ? 1 : 0);
    }

    /** Original {@code setUpgrade(int slot, byte upgrade)} — the array half (the synch half is TC-M6-08-s-c). */
    public void setUpgrade(final int slot, final byte upgrade) {
        if (slot >= 0 && slot < this.upgrades.length) {
            this.upgrades[slot] = upgrade;
        }
    }

    public void setUpgrades(final byte[] upgrades) {
        this.upgrades = upgrades;
    }

    /**
     * Original {@code getUpgradeAmount(int type)}: the number of slots holding
     * that upgrade type.
     */
    public int upgradeAmount(final int type) {
        int amount = 0;
        for (final byte b : this.upgrades) {
            if (type == b) {
                amount++;
            }
        }
        return amount;
    }

    // ------------------------------------------------------------ attributes ---

    /** Original {@code getGolemStrength()}: {@code type.strength + getUpgradeAmount(1)}. */
    public int getGolemStrength() {
        return this.getMaterial().strength + this.upgradeAmount(1);
    }

    /**
     * The attribute half of the original {@code setupGolem()}
     * ({@code EntityGolemBase.java:185-216}), pinned line by line:
     *
     * <ul>
     *   <li>max health = {@code golemType.health + (decoration.contains("H") ?
     *       5 : 0)} — the "H" (top hat) bonus is the original's {@code bonus}
     *       variable, wrapped there in a try/catch because the decoration read
     *       could throw;</li>
     *   <li>attack damage = {@code 2 + getGolemStrength() + getUpgradeAmount(1)}
     *       ({@code +2} more when the decoration contains "M", the mace arm);</li>
     *   <li>{@code fireResist} → {@code isImmuneToFire} (see
     *       {@link #fireImmune()}).</li>
     * </ul>
     *
     * <p>The rest of {@code setupGolem()} — the goal installation switch over the
     * animation core, {@code setupGolemInventory()} and the fluid/essentia
     * display stacks — is TC-M6-08-s-b.</p>
     */
    public void applyMaterialTraits() {
        final EnumGolemType material = this.getMaterial();
        final int hatBonus = this.getDecoration().contains("H") ? 5 : 0;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(material.health + hatBonus);
        int damage = 2 + this.getGolemStrength() + this.upgradeAmount(1);
        if (this.getDecoration().contains("M")) {
            damage += 2;
        }
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
    }

    /**
     * Original {@code field_70178_ae = true} for {@code CLAY}, {@code STONE},
     * {@code IRON} and {@code THAUMIUM} ({@code EnumGolemType.fireResist}),
     * set in {@code setupGolem()}. 1.21.1 has no per-instance fire-immunity
     * flag, so the material predicate is the override.
     */
    @Override
    public boolean fireImmune() {
        return this.getMaterial().fireResist;
    }

}
