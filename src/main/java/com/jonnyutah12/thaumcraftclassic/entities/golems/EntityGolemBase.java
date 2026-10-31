package com.jonnyutah12.thaumcraftclassic.entities.golems;

import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemBell;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemCore;
import com.jonnyutah12.thaumcraftclassic.menus.GolemMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemCore;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    /**
     * Original synched-data index 18: the packed toggle byte ({@code Utils.pack}).
     * The original's eight toggles are: 0 ignore-home-contents, 1 no-hostiles,
     * 2 no-animals, 3 no-players, 4 no-creepers, 5 oredict, 6 ignore-damage,
     * 7 ignore-NBT ({@code EntityGolemBase.java:818-866}).
     */
    private static final EntityDataAccessor<Byte> DATA_TOGGLES =
            SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.BYTE);
    /**
     * Original synched-data index 30: the health percentage's numerator, a byte
     * ({@code EntityGolemBase.java:110-113, 791-793}). The maximum health is
     * 20.0 for every material, so the byte never clips.
     */
    private static final EntityDataAccessor<Byte> DATA_HEALTH =
            SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.BYTE);

    /** Original {@code public byte[] upgrades = new byte[]{-1}}. */
    private byte[] upgrades = new byte[] {-1};

    /** Original {@code homeFacing} — the face of the block the golem was placed against. */
    private int homeFacing;

    // ------------------------------------------------- runtime state (s-b) ---

    /** Original {@code public InventoryMob inventory = new InventoryMob(this, 1)}. */
    private InventoryMob inventory;
    /** Original {@code public ItemStack itemCarried} — the singe stack the act goals hold. */
    private ItemStack itemCarried;
    /** Original {@code public FluidStack fluidCarried} (core 5 / Decanting). */
    private FluidStack fluidCarried;
    /** Original {@code public Aspect essentia} — the aspect the golem carries (core 6 / Alchemy). */
    private Aspect essentia;
    /** Original {@code public int essentiaAmount} — units of {@link #essentia} carried. */
    private int essentiaAmount;
    /** Original {@code public boolean paused} — the GUI's pause toggle. */
    private boolean paused;
    /** Original {@code public boolean inactive} — set while standing on the inert fetter (cosmetic solid meta 10). */
    private boolean inactive;
    /** Original {@code public float bootup = -1.0F} — the client camera-tick counter. */
    private float bootup = -1.0F;
    /** Original {@code public int regenTimer = 0}. */
    private int regenTimer;
    /** Original {@code public int action = 0} — the action animation counter (6 ticks). */
    private int action;
    /** Original {@code public int leftArm = 0} — the left arm animation counter (5 ticks). */
    private int leftArm;
    /** Original {@code public int rightArm = 0} — the right arm animation counter (5 ticks). */
    private int rightArm;
    /** Original {@code public int healing = 0} — the heal animation counter. */
    private int healing;
    /** Original {@code protected ArrayList<Marker> markers = new ArrayList<>()}. */
    private List<Marker> markers = new ArrayList<>();
    /** Original {@code public byte[] colors = null}, one marker colour per inventory slot; {@code -1} is the wildcard. */
    private byte[] colors = new byte[] {-1};
    /** Original {@code public String decoration = ""} — the mirrored field the original ALSO kept as a synched string. */
    private String decoration = "";

    public EntityGolemBase(final EntityType<? extends EntityGolemBase> type, final Level level) {
        super(type, level);
        // Original constructor: this.func_110163_bv() (enablePersistence).
        this.setPersistenceRequired();
        // Original constructor: public InventoryMob inventory = new InventoryMob(this, 1).
        this.inventory = new InventoryMob(this, 1);
        // Original constructor: this.func_70661_as().func_75498_b(true) / func_75490_c(true) / func_75495_e(true)
        // — GroundPathNavigation's avoid-water, can-swim and can-pass-doors flags (the original's
        // vanilla-style navigator; the port adds none of its own).
        if (this.getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanFloat(true);
            ground.setCanPassDoors(true);
        }
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
        builder.define(DATA_TOGGLES, (byte) 0);
        builder.define(DATA_HEALTH, (byte) 20);
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
     * the port keeps one value and writes both. The runtime field
     * ({@link #decorationField()}) is what the speed/range/regen formulas read.
     */
    public void setDecoration(final String decoration) {
        this.decoration = decoration == null ? "" : decoration;
        this.getEntityData().set(DATA_DECORATION, this.decoration);
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

    /** Original {@code public boolean paused} — set by {@code ContainerGolem}. */
    public boolean getPaused() {
        return this.paused;
    }

    public void setPaused(final boolean paused) {
        this.paused = paused;
    }

    /**
     * The right-click GUI open site — the GUI leg of the original
     * {@code func_70085_c(EntityPlayer)} (called from {@code interact}), gated
     * by the original chain ({@code EntityGolemBase.java:1002-1042} and its
     * {@code customInteraction} tail at 954-1001):
     *
     * <ol>
     *   <li>sneaking is refused ({@code func_70085_c} first branch);</li>
     *   <li>a golem that already has a core and is clicked with a
     *       {@code ItemGolemBell} is refused (the bell's ride/leash leg is the
     *       bell item's, TC-M6-08-s-b);</li>
     *   <li>the GUI opens only when {@code getCore() > -1 &&
     *       ItemGolemCore.hasGUI(core)} — the ported
     *       {@link com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemCore#hasGUI(int)}
     *       predicate, cores 0/1/2/4/5/8/10;</li>
     *   <li>a held {@code ItemWandCasting} is refused so wand right-clicks
     *       keep their own use. The wand item is not in this tree (only the
     *       {@code IVisContainerItem} seam), so that leg cannot be reproduced
     *       here; it is recorded in docs/BEHAVIORAL_DEVIATIONS.md (BD-19) and
     *       lands with the wand family;</li>
     *   <li>the core-attachment and upgrade-attachment legs of
     *       {@code func_70085_c} (and the decoration/cake legs of
     *       {@code customInteraction}) run before the GUI leg in the original
     *       and are TC-M6-08-s-b; with them absent the GUI leg is the only
     *       leg this override ships.</li>
     * </ol>
     *
     * <p>The original opens through {@code openGui(Thaumcraft.instance, 0,
     * world, entityId, 0, 0)} — gui id 0 = {@code GuiHandlerGui} building
     * {@code ContainerGolem(playerInventory, golem.inventory)} from the entity
     * id carried in the x slot. 1.21.1's equivalent is a
     * {@code MenuProvider} whose extra buffer carries the entity id, resolved
     * by {@code GolemMenu}'s client constructor (the {@link
     * com.jonnyutah12.thaumcraftclassic.menus.TrunkMenu} pattern). The
     * original's interact returns false on every leg — nothing is consumed —
     * so this returns {@link InteractionResult#PASS}; the open itself runs
     * server-side (the original's {@code !world.isClientSide} gate — a
     * game-test/dedicated server is not client-side), and the client
     * receives the open-screen packet.</p>
     */
    @Override
    public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        // Original func_70085_c first branch: sneak refuses the whole chain.
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        final ItemStack held = player.getMainHandItem();
        // Original: core > -1 && held == bell -> the bell item's leg (TC-M6-08-s-b).
        if (this.getCore() > -1 && held.getItem() instanceof ItemGolemBell) {
            return InteractionResult.PASS;
        }
        // Original customInteraction GUI gate: core > -1 && ItemGolemCore.hasGUI(core).
        // The ItemWandCasting exclusion is NOT reproducible here (BD-19).
        final int core = this.getCore();
        if (core > -1 && ItemGolemCore.hasGUI(core) && !this.level().isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                            (windowId, inventory, opener) -> new GolemMenu(windowId, inventory, this),
                            Component.translatable("container.thaumcraft.gui_golem")),
                    buffer -> buffer.writeInt(this.getId()));
            // The original customInteraction returns true when the GUI opens
            // (the interaction is consumed); 1.21.1 spells that SUCCESS.
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
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

    // ================================================================= pure formulas ===
    //
    // Every number below is quoted from the decompiled source in the javadoc, and
    // every public accessor delegates here, so the formulas are unit-testable
    // without a world.

    /**
     * Original {@code getCarryLimit()} ({@code EntityGolemBase.java:302-310}):
     * <pre>
     *   int base = this.golemType.carry;
     *   return base + Math.min(16, Math.max(4, base)) * this.getUpgradeAmount(1);
     * </pre>
     * The per-upgrade term is the <em>clamped base</em> — {@code min(16, max(4,
     * base))} — not the raw material carry, and the upgrade type is {@code 1}
     * (Earth), which is why this is load-bearing for the carrier task.
     */
    public static int carryLimit(final int baseCarry, final int earthUpgrades) {
        return baseCarry + Math.min(16, Math.max(4, baseCarry)) * earthUpgrades;
    }

    /**
     * Original {@code getFluidCarryLimit()} ({@code EntityGolemBase.java:311-313}):
     * {@code MathHelper.floor(Math.sqrt(getCarryLimit())) * 1000} — whole buckets.
     */
    public static int fluidCarryLimit(final int carryLimit) {
        return (int) Math.floor(Math.sqrt(carryLimit)) * 1000;
    }

    /**
     * Original {@code getRange()} ({@code EntityGolemBase.java:430-441}):
     * <pre>
     *   float dmod = 16 + this.getUpgradeAmount(3) * 4;      // Water upgrade
     *   if (this.decoration.contains("G")) dmod += Math.max(dmod * 0.1F, 1.0F);  // spectacles
     *   if (this.advanced)                 dmod += Math.max(dmod * 0.2F, 2.0F);  // advanced golem
     *   return dmod;
     * </pre>
     * The bonus multipliers apply in that order and compound (the advanced bonus
     * is a percentage of the <em>spectacle-adjusted</em> range).
     */
    public static float range(final int waterUpgrades, final boolean spectacles, final boolean advanced) {
        float dmod = 16 + waterUpgrades * 4;
        if (spectacles) {
            dmod += Math.max(dmod * 0.1F, 1.0F);
        }
        if (advanced) {
            dmod += Math.max(dmod * 0.2F, 2.0F);
        }
        return dmod;
    }

    /**
     * Original {@code getCarrySpace()} ({@code EntityGolemBase.java:812-816}):
     * {@code itemCarried == null ? getCarryLimit() :
     * min(getCarryLimit() - carriedCount, carriedMaxStack - carriedCount)}.
     */
    public static int carrySpace(final int carryLimit, final int carriedCount, final int carriedMaxStack) {
        return Math.min(carryLimit - carriedCount, carriedMaxStack - carriedCount);
    }

    /**
     * Original {@code getHealthPercentage()} ({@code EntityGolemBase.java:791-793}):
     * the synched health byte divided by the max health.
     */
    public static float healthPercentage(final int syncedHealth, final double maxHealth) {
        return maxHealth <= 0 ? 0.0F : (float) (syncedHealth / maxHealth);
    }

    /**
     * Original {@code func_70689_ay()} ({@code EntityGolemBase.java:315-336}), the
     * golem's movement speed:
     * <pre>
     *   float speed = golemType.speed * (decoration.contains("B") ? 1.1F : 1.0F);   // bowtie
     *   if (decoration.contains("P")) speed *= 0.88F;                               // iron plating
     *   speed *= 1.0F + getUpgradeAmount(0) * 0.15F;                                // Air upgrade
     *   if (advanced) speed *= 1.1F;
     *   if (inWater && material in {STONE, IRON, THAUMIUM}) speed *= 2.0F;
     *   return speed;
     * </pre>
     * and {@code 0.0F} while the golem is paused or inactive.
     */
    public static float movementSpeed(final float baseSpeed, final boolean bowtie, final boolean plating,
                                      final int airUpgrades, final boolean advanced, final boolean inWater,
                                      final boolean waterAffine) {
        float speed = baseSpeed * (bowtie ? 1.1F : 1.0F);
        if (plating) {
            speed *= 0.88F;
        }
        speed *= 1.0F + airUpgrades * 0.15F;
        if (advanced) {
            speed *= 1.1F;
        }
        if (inWater && waterAffine) {
            speed *= 2.0F;
        }
        return speed;
    }

    /**
     * Original {@code getActionTimer()} ({@code EntityGolemBase.java:1047-1049}):
     * {@code 3 - Math.abs(this.action - 3)} — a triangle wave over the six-tick
     * action counter.
     */
    public static int actionTimer(final int action) {
        return 3 - Math.abs(action - 3);
    }

    /** Original {@code startActionTimer()}: 6 ticks ({@code EntityGolemBase.java:1051-1056}). */
    public static final int ACTION_TICKS = 6;

    /** Original {@code startLeftArmTimer()} / {@code startRightArmTimer()}: 5 ticks each. */
    public static final int ARM_TICKS = 5;

    // ===================================================== instance accessors ===

    /** Original {@code getUpgradeAmount(int type)} ({@code EntityGolemBase.java:519-529}). */
    public int getUpgradeAmount(final int type) {
        int amount = 0;
        for (final byte b : this.upgrades) {
            if (type == b) {
                amount++;
            }
        }
        return amount;
    }

    /** Original {@code getCarryLimit()} — see {@link #carryLimit(int, int)}. */
    public int getCarryLimit() {
        return carryLimit(this.getMaterial().carry, this.getUpgradeAmount(1));
    }

    /** Original {@code getFluidCarryLimit()} — see {@link #fluidCarryLimit(int)}. */
    public int getFluidCarryLimit() {
        return fluidCarryLimit(this.getCarryLimit());
    }

    /** Original {@code getRange()} — see {@link #range(int, boolean, boolean)}. */
    public float getRange() {
        return range(this.getUpgradeAmount(3), this.getDecoration().contains("G"), this.isAdvanced());
    }

    /** Original {@code getCarrySpace()}. */
    public int getCarrySpace() {
        final ItemStack carried = this.getCarried();
        return carried == null ? this.getCarryLimit()
                : carrySpace(this.getCarryLimit(), carried.getCount(), carried.getMaxStackSize());
    }

    /** Original {@code getHealthPercentage()}. */
    public float getHealthPercentage() {
        return healthPercentage(this.getEntityData().get(DATA_HEALTH), this.getMaxHealth());
    }

    /** Original {@code func_70689_ay()} — the golem's own movement speed. */
    @Override
    public float getSpeed() {
        if (this.paused || this.inactive) {
            return 0.0F;
        }
        final EnumGolemType material = this.getMaterial();
        final boolean waterAffine = material == EnumGolemType.STONE
                || material == EnumGolemType.IRON || material == EnumGolemType.THAUMIUM;
        return movementSpeed(material.speed, this.getDecoration().contains("B"), this.getDecoration().contains("P"),
                this.getUpgradeAmount(0), this.isAdvanced(), this.isInWater(), waterAffine);
    }

    /** Original {@code func_110176_b(x, y, z)}: {@code home.distanceSq(pos) < range²}. */
    public boolean isWithinRange(final BlockPos pos) {
        final float dmod = this.getRange();
        return this.getRestrictCenter().distSqr(pos) < dmod * dmod;
    }

    /** Original field access for the runtime card's tests and for {@link GolemHelper}. */
    public InventoryMob getInventory() {
        return this.inventory;
    }

    /** Original {@code setCarried(ItemStack)} — the carry field (the display stack is TC-M6-08-s-c). */
    public void setCarried(final ItemStack stack) {
        this.itemCarried = stack;
    }

    /** Original {@code getCarried()}: an emptied carried stack is dropped to null. */
    public ItemStack getCarried() {
        if (this.itemCarried != null && this.itemCarried.getCount() <= 0) {
            this.itemCarried = null;
        }
        return this.itemCarried;
    }

    /** Original {@code hasSomething()}. */
    public boolean hasSomething() {
        return this.inventory.hasSomething();
    }

    public Aspect getEssentia() {
        return this.essentia;
    }

    public int getEssentiaAmount() {
        return this.essentiaAmount;
    }

    /**
     * Sets the carried essentia. The original kept {@code essentiaAmount} as a
     * plain {@code int} and only ever wrote it from the essentia goals, clamped
     * there by {@code getCarryLimit() - essentiaAmount}
     * ({@code AIEssentiaGather.java:136-140}); the port keeps the same
     * unbounded field and the same gather-side clamp, and additionally refuses a
     * negative amount (defensive only — the original never produced one).
     */
    public void setEssentia(final Aspect aspect, final int amount) {
        this.essentia = aspect;
        this.essentiaAmount = Math.max(0, amount);
        if (this.essentiaAmount == 0) {
            this.essentia = null;
        }
    }

    public FluidStack getFluidCarried() {
        return this.fluidCarried;
    }

    public void setFluidCarried(final FluidStack fluid) {
        this.fluidCarried = fluid;
    }

    public boolean isPaused() {
        return this.paused;
    }

    public boolean isInactive() {
        return this.inactive;
    }

    public float getBootup() {
        return this.bootup;
    }

    public void setBootup(final float bootup) {
        this.bootup = bootup;
    }

    public int getRegenTimer() {
        return this.regenTimer;
    }

    public int getAction() {
        return this.action;
    }

    public int getLeftArm() {
        return this.leftArm;
    }

    public int getRightArm() {
        return this.rightArm;
    }

    public int getHealing() {
        return this.healing;
    }

    /** Original {@code getActionTimer()}. */
    public int getActionTimer() {
        return actionTimer(this.action);
    }

    /** Original {@code startActionTimer()}. */
    public void startActionTimer() {
        if (this.action == 0) {
            this.action = ACTION_TICKS;
        }
    }

    /** Original {@code startLeftArmTimer()}. */
    public void startLeftArmTimer() {
        if (this.leftArm == 0) {
            this.leftArm = ARM_TICKS;
        }
    }

    /** Original {@code startRightArmTimer()}. */
    public void startRightArmTimer() {
        if (this.rightArm == 0) {
            this.rightArm = ARM_TICKS;
        }
    }

    // ========================================================= marker plumbing ===

    /** Original {@code getMarkers()}, which validates first. */
    public List<Marker> getMarkers() {
        this.validateMarkers();
        return this.markers;
    }

    /** Original {@code setMarkers(ArrayList<Marker>)}. */
    public void setMarkers(final List<Marker> markers) {
        this.markers = new ArrayList<>(markers);
    }

    /** Adds a marker (the GUI and the bell both do this); refuses a duplicate on the same face. */
    public boolean addMarker(final Marker marker) {
        if (GolemHelper.contained(this.markers, marker)) {
            return false;
        }
        this.markers.add(marker);
        return true;
    }

    /**
     * Original {@code validateMarkers()}: drops every marker whose {@code dim}
     * is not the current dimension.
     *
     * <p><b>Deviation (shape, documented).</b> 1.7.10 compared
     * {@code marker.dim} against {@code world.provider.dimensionId}, a small
     * integer. 1.21.1 dimensions are {@code ResourceKey<Level>}s, so the port
     * stores {@code world.dimension().location().hashCode()} in {@code dim}
     * ({@code ResourceLocation.hashCode()} is {@code String.hashCode()}, which
     * the Java specification fixes, so the stored value is stable across JVM
     * runs and across save/load). The comparison is then the original's
     * equality test. A marker saved by an older build with a different scheme
     * would be dropped, which is the original's own behaviour for a foreign
     * dimension.</p>
     */
    public void validateMarkers() {
        final List<Marker> kept = new ArrayList<>();
        final int dim = GolemHelper.dimensionOf(this.level());
        for (final Marker marker : this.markers) {
            if (marker.dim == dim) {
                kept.add(marker);
            }
        }
        this.markers = kept;
    }

    /** Original {@code colors} array, one entry per inventory slot. */
    public byte[] getColors() {
        return this.colors;
    }

    public void setColors(final byte[] colors) {
        this.colors = colors;
    }

    /** Original {@code getColors(int slot)}: the colour's hex value, {@code -1} for the "h" wildcard. */
    public short getColors(final int slot) {
        return slot >= 0 && slot < this.colors.length ? (short) this.colors[slot] : -1;
    }

    /** Original {@code setColors(int slot, int color)} (the synched string is TC-M6-08-s-c's display half). */
    public void setColors(final int slot, final int color) {
        if (slot >= 0 && slot < this.colors.length) {
            this.colors[slot] = (byte) color;
        }
    }

    /**
     * Original {@code getColorsMatching(ItemStack)} ({@code EntityGolemBase.java:542-565}):
     * the colours of the slots whose stack matches the sample; when the whole
     * inventory is empty, every slot's colour (so an empty golem still has
     * marker targets).
     */
    public List<Byte> getColorsMatching(final ItemStack match) {
        final List<Byte> out = new ArrayList<>();
        if (this.inventory.inventory != null && this.inventory.inventory.length > 0) {
            boolean allNull = true;
            for (int a = 0; a < this.inventory.inventory.length; a++) {
                if (this.inventory.inventory[a] != null) {
                    allNull = false;
                }
                if (GolemHelper.areItemStacksEqual(this.inventory.inventory[a], match, this.checkOreDict(),
                        this.ignoreDamage(), this.ignoreNBT())) {
                    out.add(this.colors[a]);
                }
            }
            if (allNull) {
                for (int a = 0; a < this.inventory.inventory.length; a++) {
                    out.add(this.colors[a]);
                }
            }
        }
        return out;
    }

    // ================================================================= toggles ===

    /** Original {@code getToggles()}: the packed byte unpacked to eight flags ({@code Utils.unpack}). */
    public boolean[] getToggles() {
        final byte packed = this.getEntityData().get(DATA_TOGGLES);
        final boolean[] out = new boolean[8];
        for (int a = 0; a < 8; a++) {
            out[a] = (packed & (1 << a)) != 0;
        }
        return out;
    }

    public byte getTogglesValue() {
        return this.getEntityData().get(DATA_TOGGLES);
    }

    /** Original {@code setToggle(int, boolean)}. */
    public void setToggle(final int index, final boolean toggle) {
        final boolean[] flags = this.getToggles();
        if (index >= 0 && index < flags.length) {
            flags[index] = toggle;
            byte packed = 0;
            for (int a = 0; a < 8; a++) {
                if (flags[a]) {
                    packed |= (byte) (1 << a);
                }
            }
            this.getEntityData().set(DATA_TOGGLES, packed);
        }
    }

    public void setTogglesValue(final byte value) {
        this.getEntityData().set(DATA_TOGGLES, value);
    }

    /** Original {@code canAttackHostiles()} = {@code !getToggles()[1]}. */
    public boolean canAttackHostiles() {
        return !this.getToggles()[1];
    }

    /** Original {@code canAttackAnimals()} = {@code !getToggles()[2]}. */
    public boolean canAttackAnimals() {
        return !this.getToggles()[2];
    }

    /** Original {@code canAttackPlayers()} = {@code !getToggles()[3]}. */
    public boolean canAttackPlayers() {
        return !this.getToggles()[3];
    }

    /** Original {@code canAttackCreepers()} = {@code !getToggles()[4]}. */
    public boolean canAttackCreepers() {
        return !this.getToggles()[4];
    }

    /** Original {@code checkOreDict()} = {@code getToggles()[5]}. */
    public boolean checkOreDict() {
        return this.getToggles()[5];
    }

    /** Original {@code ignoreDamage()} = {@code getToggles()[6]}. */
    public boolean ignoreDamage() {
        return this.getToggles()[6];
    }

    /** Original {@code ignoreNBT()} = {@code getToggles()[7]}. */
    public boolean ignoreNBT() {
        return this.getToggles()[7];
    }

    // ================================================== decoration / setup ===

    /** Original field {@code decoration}, mirrored with the synched string. */
    public String decorationField() {
        return this.decoration;
    }

    /**
     * Original {@code setupGolemInventory()} ({@code EntityGolemBase.java:142-184}):
     * the core's inventory size and the marker-colour array are sized together.
     * <pre>
     *   if (!ItemGolemCore.hasInventory(core)) return false;
     *   if (core &gt; -1) {
     *     switch (core) {
     *       case 3: case 4: case 6:  break;                       // no inventory
     *       case 5:  invSize = 1 + getUpgradeAmount(2);           // Decanting: 1 + Fire
     *       default: invSize = 6 + getUpgradeAmount(2) * 6;       // 6 slots + 6 per Fire
     *     }
     *     ... existing stacks carried over, colours resized with -1 padding ...
     *   }
     * </pre>
     * Returns whether the core has an inventory at all.
     */
    public boolean setupGolemInventory() {
        if (!ItemGolemCore.hasInventory(this.getCore())) {
            return false;
        }
        if (this.getCore() > -1) {
            final int invSize;
            switch (this.getCore()) {
                case 3, 4, 6 -> invSize = 0; // unreachable: hasInventory already refused these
                case 5 -> invSize = 1 + this.getUpgradeAmount(2);
                default -> invSize = 6 + this.getUpgradeAmount(2) * 6;
            }
            final InventoryMob resized = new InventoryMob(this, invSize);
            for (int a = 0; a < this.inventory.inventory.length && a < invSize; a++) {
                resized.inventory[a] = this.inventory.inventory[a];
            }
            this.inventory = resized;
        }

        final byte[] oldColors = this.colors;
        this.colors = new byte[this.inventory.slotCount];
        for (int a = 0; a < this.inventory.slotCount; a++) {
            this.colors[a] = -1;
            if (a < oldColors.length) {
                this.colors[a] = oldColors[a];
            }
        }
        return true;
    }

    /**
     * Original {@code setup(int side)} ({@code EntityGolemBase.java:338-342}):
     * {@code homeFacing = side; setupGolem(); setupGolemInventory();}. This is
     * what the placer calls once the entity exists.
     */
    public void setup(final int side) {
        this.setHomeFacing(side);
        this.applyMaterialTraits();
        this.setupGolemInventory();
    }

    /**
     * Original {@code getCore()} as a {@link GolemCoreTask}, or {@code null} for
     * a blank ({@code 100}) / absent ({@code -1}) / unknown core. The task table
     * is {@link GolemCoreTask}.
     */
    public GolemCoreTask getCoreTask() {
        return GolemCoreTask.byId(this.getCore());
    }

    // ============================================================ persistence ===
    //
    // The original's addAdditionalSaveData / readAdditionalSaveData
    // (EntityGolemBase.java:567-720) with the original's tag names:
    //   HomeX / HomeY / HomeZ, HomeFacing, GolemType, Core, Decoration, toggles,
    //   advanced, colors (byte[]), upgrades (byte[]),
    //   fluidCarried (broken in as its own FluidStack compound),
    //   essentia (aspect tag) + essentiaAmount (byte), ItemCarried (compound),
    //   Owner (string), Markers (list of compound), Inventory (list of compound).
    //
    // Note the original only writes the carry payload for the core that uses it:
    // core 5 for fluids, core 6 for essentia. The port writes both fields only
    // under the same cores, so a core change does not resurrect stale payload.

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HomeX", this.getRestrictCenter().getX());
        tag.putInt("HomeY", this.getRestrictCenter().getY());
        tag.putInt("HomeZ", this.getRestrictCenter().getZ());
        tag.putByte("HomeFacing", (byte) this.homeFacing);
        tag.putByte("GolemType", (byte) this.getMaterial().ordinal());
        tag.putByte("Core", (byte) this.getCore());
        tag.putString("Decoration", this.decoration);
        tag.putByte("toggles", this.getTogglesValue());
        tag.putBoolean("advanced", this.isAdvanced());
        tag.putByteArray("colors", this.colors);
        tag.putByteArray("upgrades", this.upgrades);
        if (this.getCore() == 5 && this.fluidCarried != null) {
            tag.put("fluidCarried", this.fluidCarried.save(this.level().registryAccess()));
        }
        if (this.getCore() == 6 && this.essentia != null && this.essentiaAmount > 0) {
            tag.putString("essentia", this.essentia.getTag());
            tag.putByte("essentiaAmount", (byte) this.essentiaAmount);
        }
        final CompoundTag carried = new CompoundTag();
        if (this.itemCarried != null) {
            tag.put("ItemCarried", this.itemCarried.save(this.level().registryAccess()));
        } else {
            tag.put("ItemCarried", carried);
        }
        tag.putString("Owner", this.getOwnerName() == null ? "" : this.getOwnerName());

        final ListTag markerList = new ListTag();
        for (final Marker marker : this.markers) {
            final CompoundTag entry = new CompoundTag();
            entry.putInt("x", marker.x);
            entry.putInt("y", marker.y);
            entry.putInt("z", marker.z);
            entry.putInt("dim", marker.dim);
            entry.putByte("side", marker.side);
            entry.putByte("color", marker.color);
            markerList.add(entry);
        }
        tag.put("Markers", markerList);
        tag.put("Inventory", this.inventory.writeToNBT(new ListTag()));
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        final int hx = tag.getInt("HomeX");
        final int hy = tag.getInt("HomeY");
        final int hz = tag.getInt("HomeZ");
        this.homeFacing = tag.getByte("HomeFacing");
        this.restrictTo(new BlockPos(hx, hy, hz), HOME_RANGE);
        this.setAdvanced(tag.getBoolean("advanced"));
        final EnumGolemType material = EnumGolemType.getType(tag.getByte("GolemType"));
        if (material != null) {
            this.setMaterial(material);
        }
        this.setCore(tag.getByte("Core"));
        if (this.getCore() == 5) {
            this.fluidCarried = FluidStack.parseOptional(this.level().registryAccess(),
                    tag.getCompound("fluidCarried"));
        }
        if (this.getCore() == 6) {
            final Aspect aspect = Aspect.getAspect(tag.getString("essentia"));
            if (aspect != null) {
                this.essentia = aspect;
                this.essentiaAmount = tag.getByte("essentiaAmount");
            }
        }
        this.setTogglesValue(tag.getByte("toggles"));
        this.itemCarried = ItemStack.parseOptional(this.level().registryAccess(), tag.getCompound("ItemCarried"));
        this.decoration = tag.getString("Decoration");
        this.setDecoration(this.decoration);
        final String owner = tag.getString("Owner");
        if (!owner.isEmpty()) {
            this.setOwnerName(owner);
        }

        this.markers = new ArrayList<>();
        final ListTag markerList = tag.getList("Markers", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < markerList.size(); i++) {
            final CompoundTag entry = markerList.getCompound(i);
            this.markers.add(new Marker(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"),
                    entry.getInt("dim"), entry.getByte("side"), entry.getByte("color")));
        }

        // The original resizes the upgrade array to golemType.upgrades + advanced,
        // then copies the saved values in, padding with -1.
        final int ul = this.getMaterial().upgrades + (this.isAdvanced() ? 1 : 0);
        final byte[] saved = tag.getByteArray("upgrades");
        final byte[] resized = new byte[ul];
        Arrays.fill(resized, (byte) -1);
        for (int a = 0; a < saved.length && a < ul; a++) {
            resized[a] = saved[a];
        }
        this.upgrades = resized;

        this.applyMaterialTraits();
        this.setupGolemInventory();
        if (tag.contains("Inventory")) {
            this.inventory.readFromNBT(tag.getList("Inventory", CompoundTag.TAG_COMPOUND),
                    this.level().registryAccess());
        }
        final byte[] savedColors = tag.getByteArray("colors");
        this.colors = new byte[this.inventory.slotCount];
        Arrays.fill(this.colors, (byte) -1);
        for (int a = 0; a < this.inventory.slotCount && a < savedColors.length; a++) {
            this.colors[a] = savedColors[a];
        }
        this.syncHealth();
    }

    private void syncHealth() {
        if (!this.level().isClientSide) {
            this.getEntityData().set(DATA_HEALTH, (byte) this.getHealth());
        }
    }

    // ================================================================== ticking ===

    /**
     * Original {@code func_70636_d()} ({@code EntityGolemBase.java:360-428}) plus
     * its {@code onLivingUpdate} tail. Ported here:
     * <ul>
     *   <li>the four animation counters decrement every tick;</li>
     *   <li>{@code inactive} is true while the block below is the cosmetic solid
     *       at metadata 10 — the inert golem fetter
     *       ({@code EntityGolemBase.java:372-381});</li>
     *   <li>the regeneration timer: reload it to {@code material.regenDelay},
     *       shortened to {@code (int)(regenDelay * 0.66F)} by the "F" (fez)
     *       decoration, then heal exactly 1.0 when the timer ran out and the golem
     *       is below full health ({@code EntityGolemBase.java:388-400});</li>
     *   <li>the home-range teleport at {@code 2304.0} squared blocks (48²) or
     *       whenever the golem is inside a block
     *       ({@code EntityGolemBase.java:402-421}).</li>
     * </ul>
     */
    @Override
    public void tick() {
        super.tick();
        if (this.action > 0) {
            this.action--;
        }
        if (this.leftArm > 0) {
            this.leftArm--;
        }
        if (this.rightArm > 0) {
            this.rightArm--;
        }
        if (this.healing > 0) {
            this.healing--;
        }

        final BlockPos below = this.blockPosition().below();
        final net.minecraft.world.level.block.state.BlockState belowState = this.level().getBlockState(below);
        // Original: the golem is inactive while the block below is the cosmetic
        // solid at metadata 10 (the inert golem fetter). The port's 1.21.1
        // `thaumcraft:blockcosmeticsolid` carries the original metadata as a
        // block-state variant, so the metadata test becomes a variant test. The
        // gate lives behind a predicate so a later card that adds the fetter
        // variant does not have to touch this method.
        this.inactive = FETTER_INACTIVE_PREDICATE.test(belowState);

        if (!this.level().isClientSide) {
            if (this.regenTimer > 0) {
                this.regenTimer--;
            } else {
                this.regenTimer = this.getMaterial().regenDelay;
                if (this.decoration.contains("F")) {
                    this.regenTimer = (int) (this.regenTimer * 0.66F);
                }
                if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(1.0F);
                    this.healing = ARM_TICKS;
                }
            }
            if (this.getRestrictCenter().distSqr(this.blockPosition()) >= 2304.0 || this.isInWall()) {
                final int var1 = this.getRestrictCenter().getX();
                final int var2 = this.getRestrictCenter().getZ();
                final int var3 = this.getRestrictCenter().getY();
                for (int dy = 1; dy >= -1; dy--) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (this.level().getBlockState(new BlockPos(var1 + dx, var3 - 1 + dy, var2 + dz))
                                    .isSolidRender(this.level(), new BlockPos(var1 + dx, var3 - 1 + dy, var2 + dz))
                                    && !this.level().getBlockState(new BlockPos(var1 + dx, var3 + dy, var2 + dz))
                                        .isSuffocating(this.level(), new BlockPos(var1 + dx, var3 + dy, var2 + dz))) {
                                this.moveTo(var1 + dx + 0.5, var3 + dy, var2 + dz + 0.5, this.getYRot(), this.getXRot());
                                this.getNavigation().stop();
                                return;
                            }
                        }
                    }
                }
            }
        }
        if (this.tickCount % 10 == 0) {
            this.syncHealth();
        }
    }

    // =============================================================== task acts ===
    //
    // The act half of each core's task. The original splits every core between
    // "goto" goals (which drive the path) and "act" goals (which mutate the
    // container); the act half is what a game test can assert without depending
    // on pathfinding, and it is the half ported here. The goto/goal classes are
    // named per core in GolemCoreTask.goalNames() and are the follow-up card's.

    /** Original {@code AIHomeDrop}/{@code AIHomePlace}: put the carried stack into the home container. */
    public boolean actHomeDrop() {
        final ItemStack carried = this.getCarried();
        if (carried == null || carried.isEmpty()) {
            return false;
        }
        final BlockPos pos = GolemHelper.homeContainerPos(this);
        if (!(this.level().getBlockEntity(pos) instanceof Container container)) {
            return false;
        }
        final int side = this.getHomeFacing();
        final ItemStack leftover = GolemHelper.placeItemStackIntoInventory(carried, container, side, false);
        if (leftover.getCount() == carried.getCount()) {
            return false;
        }
        this.setCarried(leftover.isEmpty() ? null : leftover);
        this.startActionTimer();
        return true;
    }

    /**
     * Original {@code AIHomeTake}: take from the home container whatever this
     * golem's core says it needs, up to the carry space
     * ({@code GolemHelper.getItemsNeeded} for the need list, {@code
     * GolemHelper.getFirstItemUsingTimeout} for the extraction).
     */
    public boolean actHomeTake() {
        final BlockPos pos = GolemHelper.homeContainerPos(this);
        if (!(this.level().getBlockEntity(pos) instanceof Container container)) {
            return false;
        }
        final int side = this.getHomeFacing();
        final List<ItemStack> needed = GolemHelper.getItemsNeeded(this, this.getUpgradeAmount(5) > 0);
        if (needed == null || needed.isEmpty()) {
            return false;
        }
        for (final ItemStack sample : needed) {
            final int space = this.getCarrySpace();
            if (space <= 0) {
                return false;
            }
            final ItemStack probe = sample.copy();
            probe.setCount(Math.min(space, sample.getCount()));
            final ItemStack taken = GolemHelper.extractStack(container, probe, side, this.checkOreDict(),
                    this.ignoreDamage(), this.ignoreNBT(), false);
            if (taken == null || taken.isEmpty()) {
                continue;
            }
            final ItemStack carried = this.getCarried();
            if (carried == null) {
                this.setCarried(taken);
            } else if (ItemStack.isSameItemSameComponents(carried, taken)) {
                carried.setCount(carried.getCount() + taken.getCount());
            } else {
                // A different stack is already carried: put it back rather than
                // silently dropping it on the floor.
                GolemHelper.placeItemStackIntoInventory(taken, container, side, false);
                continue;
            }
            if (this.getInventory().addItemStackToInventory(taken.copy())) {
                this.getInventory().setChanged();
            }
            this.startRightArmTimer();
            return true;
        }
        return false;
    }

    /**
     * Original {@code AIEssentiaEmpty}, jar arm ({@code AIEssentiaEmpty.java:30-60}):
     * walk to the nearest marked jar with room ({@code GolemHelper.findJarWithRoom})
     * and hand it the carried essentia, which is exactly
     * {@code TileJarFillable.addToContainer(aspect, essentiaAmount)} returning the
     * amount that did not fit. The original's transport arms
     * ({@code IEssentiaTransport} tubes/reservoirs) are the same follow-up as the
     * rest of the transport branch.
     */
    public boolean actEssentiaEmpty() {
        if (this.essentia == null || this.essentiaAmount == 0) {
            return false;
        }
        final BlockPos pos = GolemHelper.findJarWithRoom(this);
        if (pos == null) {
            return false;
        }
        if (!(this.level().getBlockEntity(pos) instanceof TileJarFillable jar)) {
            return false;
        }
        this.essentiaAmount = jar.addToContainer(this.essentia, this.essentiaAmount);
        if (this.essentiaAmount == 0) {
            this.essentia = null;
        }
        this.startActionTimer();
        return true;
    }

    /**
     * Original {@code AISortingPlace}: move a stack out of the home container
     * into a marked container that already holds that stack
     * ({@code GolemHelper.getContainersWithGoods} is the "already holds it"
     * query), respecting the carry space.
     */
    public boolean actSortingPlace() {
        final BlockPos homePos = GolemHelper.homeContainerPos(this);
        if (!(this.level().getBlockEntity(homePos) instanceof Container home)) {
            return false;
        }
        final List<ItemStack> goods = GolemHelper.getItemsInHomeContainer(this);
        if (goods == null || goods.isEmpty()) {
            return false;
        }
        for (final ItemStack good : goods) {
            final List<Container> targets = GolemHelper.getContainersWithGoods(this.level(), this, good, (byte) -1);
            if (targets.isEmpty()) {
                continue;
            }
            final ItemStack probe = good.copy();
            probe.setCount(Math.min(this.getCarrySpace(), good.getCount()));
            final ItemStack moved = GolemHelper.extractStack(home, probe, this.getHomeFacing(),
                    this.checkOreDict(), this.ignoreDamage(), this.ignoreNBT(), false);
            if (moved == null || moved.isEmpty()) {
                continue;
            }
            for (final Container target : targets) {
                final ItemStack leftover = GolemHelper.placeItemStackIntoInventory(moved, target, -1, false);
                if (leftover.getCount() < moved.getCount()) {
                    if (!leftover.isEmpty()) {
                        GolemHelper.placeItemStackIntoInventory(leftover, home, this.getHomeFacing(), false);
                    }
                    this.startLeftArmTimer();
                    return true;
                }
            }
            // Nothing took it: put it back so the sort pass does not destroy goods.
            GolemHelper.placeItemStackIntoInventory(moved, home, this.getHomeFacing(), false);
        }
        return false;
    }

    /**
     * Runs one act step for this golem's core. This is the seam the runtime and
     * the game tests drive; the original's equivalent is the act goal's
     * {@code updateTask} body. Returns whether the world changed.
     */
    public boolean runCoreTaskStep() {
        final GolemCoreTask task = this.getCoreTask();
        if (task == null) {
            return false;
        }
        return switch (task.action()) {
            case HOME_PLACE -> this.actHomeDrop();
            case HOME_TAKE -> this.actHomeTake();
            case ESSENTIA_EMPTY -> this.actEssentiaEmpty();
            case SORTING_PLACE -> this.actSortingPlace();
            case NONE -> false;
        };
    }

    /** Original {@code func_70599_aP()}: the golem's footstep sound is silent (0.1F). */
    public static final float STEP_SOUND_VOLUME = 0.1F;

    /** Original {@code dropStuff()}: the carried stack is dropped ({@code func_70628_a} → {@code dropStuff}). */
    public void dropStuff() {
        if (!this.level().isClientSide && this.itemCarried != null) {
            this.spawnAtLocation(this.itemCarried, 0.5F);
        }
    }

    /**
     * Original {@code func_70628_a(boolean, int)} — "the golem drops what it
     * carries". 1.21.1 splits death loot into {@code dropCustomDeathLoot} and the
     * entity's own loot table; the golem has no loot table in either version, so
     * the carried stack is dropped from the removal path instead.
     */
    @Override
    public void remove(final RemovalReason reason) {
        if (reason != RemovalReason.DISCARDED) {
            this.dropStuff();
        }
        super.remove(reason);
    }

    /**
     * The original's inert-fetter gate ({@code EntityGolemBase.java:372-381}):
     * {@code world.getBlock(x, y-1, z) == ConfigBlocks.blockCosmeticSolid &&
     * world.getBlockMetadata(x, y-1, z) == 10}. The port's 1.21.1
     * {@code thaumcraft:blockcosmeticsolid} carries the original metadata as a
     * block-state variant, and the "inert golem fetter" variant is owned by the
     * cosmetic-solid card, not this one, so this predicate is currently
     * constant-false with a named seam. <b>Deferred behaviour:</b> a golem
     * standing on the inert fetter stays active until the fetter variant lands
     * (nothing else in the runtime depends on it).
     */
    private static final java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState>
            FETTER_INACTIVE_PREDICATE = state -> false;
}
