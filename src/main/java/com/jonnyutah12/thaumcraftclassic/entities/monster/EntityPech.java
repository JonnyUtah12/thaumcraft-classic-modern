package com.jonnyutah12.thaumcraftclassic.entities.monster;

import java.util.List;

import javax.annotation.Nullable;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTags;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem;
import com.jonnyutah12.thaumcraftclassic.entities.ai.pech.AIPechItemEntityGoto;
import com.jonnyutah12.thaumcraftclassic.entities.ai.pech.AIPechTradePlayer;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityPechBlast;
import com.jonnyutah12.thaumcraftclassic.items.ItemManaBean;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogPech;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;

/**
 * The Pech — {@code thaumcraft:pech} (content-mapping {@code Thaumcraft:Pech}, row 290, mod_index 23),
 * the original's largest mob: 830 lines at
 * {@code thaumcraft.common.entities.monster.EntityPech},
 * {@code extends EntityMob implements IRangedAttackMob}.
 *
 * <p>Every figure lives on {@link PechAiLogic} citing its decompiled line, and
 * each override here carries that line as a trailing comment. The shape: the
 * mainhand item picks the attack mode ({@code :166-184} → {@code :444-462}) — a
 * bow fires an arrow with difficulty-scaled damage and punch ({@code :464-479}),
 * the (unported) wand fires an {@link EntityPechBlast} ({@code :480-489}),
 * anything else is melee; a pech hit by a player enrages every pech in a
 * 32x16x32 box ({@code :516-537}) on a {@code 400 + nextInt(400)} fuse
 * ({@code :494-505}); anger decays 1/tick ({@code :544-549}) and gates target
 * acquisition ({@code :440-442}); {@code playLivingSound} ({@code :399-418}) is
 * a trade ping — a 1-in-3 roll scans the 4x2x4 box, and a second pech in it
 * sends event 17 plus the trade sound, otherwise event 16 (the original sends
 * events, not chat); the six-entry valued table plus the greed aspect
 * ({@link PechAiLogic#GREED_ASPECT_ID} — 1.7.10's {@code Aspect.GREED} is this
 * port's {@code lucrum}, {@code AspectDefinitions.java:70}) drives the 9-slot
 * loot array and the {@code nextInt(10) < value} tame ({@code :612-692}); the
 * loot array drops at 0.88 per slot on death ({@code :324-346}) and
 * {@code dropAllLoot} is replaced by one itemResource meta 9
 * ({@code :348-350}); the client draws the vanilla villager particle pair at
 * the original's rolls (15 angry while enraged, 25 tamed) and 0.02 gaussian
 * ({@code :565-596}, {@code :352-397}).</p>
 *
 * <p>Not here, each with its reason (the row's {@code known_differences} and the
 * card's {@code left_out_of_scope}): the trade GUI ({@code :701} opens
 * {@code openGui(Thaumcraft, 1, ...)} — the container/menu lane; the goal, the
 * flags and the rules ship, the window does not); the wand equipment branch
 * ({@code thaumcraft:wandcasting} IS registered — ledger row 190 {@code
 * Thaumcraft:WandCasting}, {@code done,tested} — but the roll that equips it
 * also builds the Pech's focus, {@code thaumcraft:focuspech} ledger row 201
 * {@code not_started}, through a {@code setFocus} seam the port has no twin of,
 * so the roll ships inert and the mage form (type 1) is unreachable from a
 * natural spawn, BD-47); the boss
 * bar (1.21.1 has no {@code Mob#isBoss()}, BD-48); the 1.7.10 {@code EntityMob}
 * random-armour super call (1.21.1 moved it to difficulty-scaled item tags on
 * {@code Zombie}; {@code Monster} has no hook); the village branch of the spawn
 * gate (1.21.1 removed the village-position API, BD-49); the overworld-only
 * spawn factor, which in 1.21.1 lives in the spawn-table registration (c6's
 * mob_spawn row 347); and the hooks {@code func_82162_bC} ({@code :181}) and
 * {@code func_98053_h} ({@code :182-183}) — {@code reference/mcp-mappings/}
 * ships only the fields CSV, so their MCP names are unresolvable and no 1.21.1
 * analogue is invented. The original's explicit
 * {@code findEntityToAttack}/{@code setEntityToAttack} pair ({@code :546-548})
 * is 1.21.1's target selector, which runs in {@code serverAiStep}; the port
 * keeps the cadence and the event that follow it.</p>
 */
public class EntityPech extends Monster implements RangedAttackMob {

    /** {@code :221} — watcher 13, {@code Byte(0)}: the pech type. */
    private static final EntityDataAccessor<Byte> DATA_TYPE =
            SynchedEntityData.defineId(EntityPech.class, EntityDataSerializers.BYTE);
    /** {@code :222} — watcher 14, {@code Short(0)}: the anger counter. 1.21.1
     *  has no short serializer, so the counter rides an integer. */
    private static final EntityDataAccessor<Integer> DATA_ANGER =
            SynchedEntityData.defineId(EntityPech.class, EntityDataSerializers.INT);
    /** {@code :223} — watcher 16, {@code Byte(0)}: tamed. */
    private static final EntityDataAccessor<Byte> DATA_TAMED =
            SynchedEntityData.defineId(EntityPech.class, EntityDataSerializers.BYTE);

    /**
     * {@code :60} — {@code ItemStack[9] loot}. The original's slots are
     * reference-typed and start null; 1.21.1's {@code ItemStack} is a value
     * object, so the array is filled with {@link ItemStack#EMPTY} and an EMPTY
     * slot is the port's representation of the original's null slot. Every
     * {@code == null} test of the original reads as {@code isEmpty()} here.
     */
    private final ItemStack[] loot = new ItemStack[PechAiLogic.LOOT_SLOTS];
    /** {@code :61} — {@code EntityPlayer angryAt}, never saved, as in the original. */
    @Nullable
    private Player angryAt;
    /** {@code :69} — {@code public boolean trading}. */
    public boolean trading;
    /** {@code :70} — the mumble level (decays x0.75 per tick, {@code :541-544}). */
    private float mumble;
    /** {@code :71} — the growl cadence counter ({@code :556-560}). */
    private int chargecount;
    /** {@code :72} — {@code private boolean updateAINextTick}. */
    private boolean updateAINextTick;

    // :63-67 — the goal instances the combat swap adds and removes.
    @Nullable
    private MeleeAttackGoal meleeAttack;
    @Nullable
    private RangedAttackGoal arrowAttack;
    @Nullable
    private RangedAttackGoal blastAttack;
    @Nullable
    private AvoidEntityGoal<Player> avoid;

    public EntityPech(final EntityType<? extends EntityPech> type, final Level level) {
        super(type, level);
        // :92 — func_70105_a(0.6F, 1.8F) rides buildType's sized(...)
        // :87 — this.experiencePoints = 5
        this.xpReward = PechAiLogic.XP;
        // :110 — equipmentDropChances[0] = 0.2F (the mainhand drop chance)
        this.setDropChance(EquipmentSlot.MAINHAND, PechAiLogic.EQUIP_DROP_CHANCE_MAINHAND);
        // :60 — the 9 loot slots exist from construction; the original's null
        // slots are EMPTY here (see the lootSlots javadoc).
        java.util.Arrays.fill(this.loot, ItemStack.EMPTY);
    }

    /**
     * {@code ConfigEntities.java:103-104} —
     * {@code registerModEntity("Pech", EntityPech.class, 12, ..., 64, 3, true)}:
     * tracking 64 blocks, update interval 3, velocity updates ON. The eye height
     * is {@code :211-213}'s 0.66 factor of the 1.8 box.
     */
    public static EntityType<EntityPech> buildType(final MobCategory category) {
        return EntityType.Builder.of(EntityPech::new, category)
                .sized(PechAiLogic.WIDTH, PechAiLogic.HEIGHT)
                .eyeHeight((float) PechAiLogic.eyeHeight())
                .clientTrackingRange(PechAiLogic.TRACK_RANGE_CHUNKS)  // 64 blocks = 4 chunks
                .updateInterval(PechAiLogic.UPDATE_INTERVAL)
                .setShouldReceiveVelocityUpdates(PechAiLogic.VELOCITY_UPDATES)
                .build("pech");
    }

    /**
     * {@code :254-259} — maxHealth 30, attackDamage 6, movementSpeed 0.5.
     * {@code :507-514} ({@code func_70658_aO}, getTotalArmorValue) is
     * {@code super + 2} capped at 20: 1.21.1's armour is the {@code ARMOR}
     * attribute (this family's seam, c2), so the +2 is the base value and the
     * cap rides 1.21.1's own armour maths.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()                                    // 1.21.1's builder factory (Mob#createMobAttributes)
                .add(Attributes.MAX_HEALTH, PechAiLogic.MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, PechAiLogic.ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, PechAiLogic.MOVEMENT_SPEED)
                .add(Attributes.ARMOR, PechAiLogic.ARMOR_BONUS);                   // :507-514
    }

    // ---- goals (:63-67, :95-108, :444-462) ----

    /**
     * Exactly the original's goal set at the original slots — 1.21.1's
     * {@code Monster} installs no goals of its own, so what is installed here is
     * the original's list, cited per goal to its {@code :95-105} line.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(PechAiLogic.GOAL_SLOT_SWIM, new FloatGoal(this));                   // :95
        this.goalSelector.addGoal(PechAiLogic.GOAL_SLOT_TRADE, new AIPechTradePlayer(this));          // :96
        this.goalSelector.addGoal(PechAiLogic.GOAL_SLOT_ITEM_PICKUP, new AIPechItemEntityGoto(this)); // :97
        this.goalSelector.addGoal(PechAiLogic.GOAL_SLOT_DOOR, new OpenDoorGoal(this, true));          // :98
        this.goalSelector.addGoal(PechAiLogic.GOAL_SLOT_RESTRICTION,
                new MoveTowardsRestrictionGoal(this, PechAiLogic.RESTRICTION_SPEED));                 // :99
        // :100 — 1.7.10's EntityAIVillage(mob, 1.0F, false) takes the speed and
        // the day flag; 1.21.1's ctor additionally takes a re-path interval (the
        // vanilla default 4) and a door-permission supplier. 1.7.10's goal has no
        // door-permission concept, so this passes the permissive supplier; the
        // pech's door behaviour is the OpenDoorGoal at slot 5.
        this.goalSelector.addGoal(PechAiLogic.GOAL_SLOT_VILLAGE, new MoveThroughVillageGoal(
                this, PechAiLogic.VILLAGE_SPEED, false, 4, () -> true));
        this.goalSelector.addGoal(PechAiLogic.GOAL_SLOT_WANDER,
                new RandomStrollGoal(this, PechAiLogic.WANDER_SPEED));                                // :101
        this.goalSelector.addGoal(PechAiLogic.GOAL_SLOT_WATCH_PLAYER, new LookAtPlayerGoal(
                this, Player.class, PechAiLogic.WATCH_PLAYER_RANGE, PechAiLogic.WATCH_PLAYER_SPEED)); // :102
        // :103 — 1.7.10's EntityAIWatchClosest hardcodes the 0.02 chance that
        // 1.21.1's ctor takes as a parameter.
        this.goalSelector.addGoal(PechAiLogic.GOAL_SLOT_WATCH,
                new LookAtPlayerGoal(this, LivingEntity.class, PechAiLogic.WATCH_RANGE, 0.02F));
        this.goalSelector.addGoal(PechAiLogic.GOAL_SLOT_LOOK_IDLE, new RandomLookAroundGoal(this));   // :104
        // :105 — EntityAIHurtByTarget(this, false): 1.21.1's ctor has no
        // alert-same-type flag (the shape is setAlertOthers()); not calling it IS
        // the original's false.
        this.targetSelector.addGoal(PechAiLogic.TARGET_SLOT_HURT, new HurtByTargetGoal(this));
        // :106-108 — the combat goal is installed server-side only in the
        // original; 1.21.1's goal engine runs inside serverAiStep, so the gate is
        // faithful and inert on the client.
        if (this.level() != null && !this.level().isClientSide) {
            this.setCombatTask();
        }
    }

    /**
     * {@code :444-462} — remove all three attack goals, install the one the
     * current type wants at slot 2, then set the player-avoidance goal to the
     * tamed state (a tamed pech does not run from players). The instances are
     * built here because {@code registerGoals()} runs from the super constructor.
     */
    public void setCombatTask() {
        if (this.meleeAttack == null) {
            this.meleeAttack = new MeleeAttackGoal(this, PechAiLogic.MELEE_SPEED, true);              // :63
            this.arrowAttack = new RangedAttackGoal(this, PechAiLogic.WAND_SPEED,
                    PechAiLogic.WAND_MIN_TICKS, PechAiLogic.WAND_MAX_RANGE,
                    PechAiLogic.WAND_RANGE);                                                          // :64
            this.blastAttack = new RangedAttackGoal(this, PechAiLogic.BLAST_SPEED,
                    PechAiLogic.BLAST_MIN_TICKS, PechAiLogic.BLAST_MAX_RANGE,
                    PechAiLogic.BLAST_RANGE);                                                         // :65
            this.avoid = new AvoidEntityGoal<>(this, Player.class, PechAiLogic.AVOID_RANGE,
                    PechAiLogic.AVOID_SPEED_CLOSE, PechAiLogic.AVOID_SPEED_FAR);                       // :67
        }
        this.goalSelector.removeGoal(this.meleeAttack);        // :445
        this.goalSelector.removeGoal(this.arrowAttack);        // :446
        this.goalSelector.removeGoal(this.blastAttack);        // :447
        switch (this.getPechType()) {
            case PechAiLogic.TYPE_BOW:
                this.goalSelector.addGoal(PechAiLogic.COMBAT_SLOT_ARROW, this.arrowAttack);  // :452
                break;
            case PechAiLogic.TYPE_WAND:
                this.goalSelector.addGoal(PechAiLogic.COMBAT_SLOT_BLAST, this.blastAttack);  // :453
                break;
            default:
                this.goalSelector.addGoal(PechAiLogic.COMBAT_SLOT_MELEE, this.meleeAttack);  // :449
        }
        if (this.isTamed()) {
            this.goalSelector.removeGoal(this.avoid);                                        // :459
        } else {
            this.goalSelector.removeGoal(this.avoid);                                        // :460
            this.goalSelector.addGoal(PechAiLogic.COMBAT_SLOT_AVOID, this.avoid);
        }
    }

    // ---- synced state (:221-248) ----

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, (byte) PechAiLogic.TYPE_NORMAL);   // :221
        builder.define(DATA_ANGER, 0);                               // :222
        builder.define(DATA_TAMED, (byte) 0);                        // :223
    }

    public int getPechType() {
        return this.entityData.get(DATA_TYPE);                       // :226-228
    }

    public void setPechType(final int type) {
        this.entityData.set(DATA_TYPE, (byte) type);                 // :230-232
    }

    public boolean isTamed() {
        return this.entityData.get(DATA_TAMED) != 0;                 // :234-236
    }

    public void setTamed(final boolean tamed) {
        this.entityData.set(DATA_TAMED, (byte) (tamed ? 1 : 0));     // :238-240
        if (tamed) {
            this.level().broadcastEntityEvent(this, PechAiLogic.EVENT_TAMED);   // :641 (5 happy particles)
        }
    }

    public int getAnger() {
        return this.entityData.get(DATA_ANGER);                      // :242-244
    }

    public void setAnger(final int anger) {
        this.entityData.set(DATA_ANGER, anger);                      // :246-248
    }

    @Nullable
    public Player getAngryAt() {
        return this.angryAt;                                         // :61
    }

    public float getMumble() {
        return this.mumble;                                          // :70
    }

    /**
     * {@code :73-87} — {@code func_70005_c_} (getCommandSenderName): the pech
     * names ITSELF from its type, not from its entity type. The original reads
     * {@code entity.Thaumcraft.Pech.name} for type 0 (Pech Forager, {@code :79}
     * plus the {@code :85} default), {@code ...Pech.1.name} for type 1 (Pech
     * Mage, the wand pech, {@code :81}) and {@code ...Pech.2.name} for type 2
     * (Pech Stalker, the bow pech, {@code :83}); a custom name wins over all
     * three ({@code :74-75}, {@code func_94056_bM} / {@code func_94057_bL}).
     * 1.21.1 has no per-type entity name, so the same three keys ride this
     * override: {@code super} already resolves the custom-name case
     * ({@code Entity#hasCustomName}), leaving only the type switch.
     */
    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        if (this.hasCustomName()) {
            return super.getDisplayName();                                             // :74-75
        }
        final int type = this.getPechType();
        if (type == PechAiLogic.TYPE_WAND) {
            return net.minecraft.network.chat.Component.translatable("entity.thaumcraft.pech.1");   // :81
        }
        if (type == PechAiLogic.TYPE_BOW) {
            return net.minecraft.network.chat.Component.translatable("entity.thaumcraft.pech.2");   // :83
        }
        return net.minecraft.network.chat.Component.translatable("entity.thaumcraft.pech");         // :79, :85
    }

    /** The 9 loot slots ({@code :60}); the probes inspect them. */
    public ItemStack[] lootSlots() {
        return this.loot;
    }

    /** Probe seam: the total count of one item across the 9 loot slots. */
    public int lootCount(final Item item) {
        int total = 0;
        for (final ItemStack stack : this.loot) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Probe seam: write one loot slot ({@code :60} array, no drop logic). */
    public void setLootSlot(final int index, final ItemStack stack) {
        this.loot[index] = stack;
    }

    // ---- equipment and spawn (:113-118, :120-164, :166-184, :185-209) ----

    /**
     * {@code :113-118} — {@code func_70062_b(int, ItemStack)}
     * (setCurrentItemOrArmor): a mainhand equip on the server re-derives the
     * attack goal next tick. 1.21.1's seam is
     * {@code Mob#setItemSlot(EquipmentSlot, ItemStack)}; every original call
     * site passes slot 0 = mainhand.
     */
    @Override
    public void setItemSlot(final EquipmentSlot slot, final ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (!this.level().isClientSide && slot == EquipmentSlot.MAINHAND) {
            this.updateAINextTick = true;                                                 // :116
        }
    }

    /** {@code :172-174} — the wand branch's live gate: 1.7.10's item-instance
     *  equality is a registry-id test here. {@code thaumcraft:wandcasting} is
     *  REGISTERED (content-mapping row 190 {@code Thaumcraft:WandCasting},
     *  {@code done,tested}, registered by the wand family at
     *  {@code registration/catalog/CatalogWands.java:70}), so this check reads
     *  TRUE at runtime and the type-derivation gate of {@code :172} is live.
     *  What keeps the mage out of a world today is the EQUIP roll, not this
     *  gate — see {@link #addRandomEquipment()} and BD-47. */
    private static boolean wandRegistered() {
        return BuiltInRegistries.ITEM.getHolder(PechAiLogic.WAND_ITEM).isPresent();
    }

    /**
     * {@code :120-164} — {@code func_82164_bB()} (addRandomEquipment): the
     * {@code nextInt(20)} mainhand roll, resolved through
     * {@link PechAiLogic#equipmentFor} (stone sword; bows; stone axe; iron
     * sword; iron axe; fishing rod; stone pickaxe; iron pickaxe). Rolls 0 and 12
     * (the decompiled {@code :123-135}) build ONE wand stack and set its focus to
     * {@code ConfigItems.itemFocusPech} before equipping:
     * {@code setFocus(wand, focus)} then six {@code addVis} calls (EARTH / ENTROPY
     * / WATER at {@code 2 + nextInt(6)}, AIR / FIRE / ORDER at {@code nextInt(4)}).
     * The port ships {@code ItemWandCasting.addVis} (the same 4-arg shape) but NO
     * focus seam, and {@code thaumcraft:focuspech} (ledger row 201) is
     * {@code not_started}, so the roll ships as {@code null} and the pech stays
     * melee — the one deviation the wand lane's registration does NOT close
     * (BD-47). {@link #wandRegistered()} stays as the {@code :172} gate the
     * original has; it now reads true.
     *
     * <p>The original opens with {@code super.func_82164_bB()} — the 1.7.10
     * {@code EntityMob} random-armour roll, which 1.21.1 moved to
     * difficulty-scaled item tags on {@code Zombie}; {@code Monster} has no such
     * hook, so there is no super call to port (recorded, not invented).</p>
     */
    protected void addRandomEquipment() {
        final ResourceLocation itemId = PechAiLogic.equipmentFor(this.random.nextInt(20));  // :122
        if (itemId == null) {
            return;                                                                        // :123-146
        }
        final Holder<Item> item = BuiltInRegistries.ITEM.getHolder(itemId).orElse(null);
        if (item == null) {
            return;
        }
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));                      // :148-163
    }

    /**
     * {@code :166-184} — {@code func_110161_a} (finalizeSpawn): equip, then read
     * the mainhand back into the pech type through
     * {@link PechAiLogic#pechTypeFor(boolean, boolean)} (wand 1 / bow 2 / melee
     * 0), and set the mainhand drop chance to 0.1 in the wand case
     * ({@code :173}). The original calls its own super twice ({@code :167},
     * {@code :184}) — kept verbatim, the second call's value returned.
     *
     * <p>1.7.10's bow test is {@code instanceof ItemBow}; 1.21.1 has no such
     * hook, so this port compares the mainhand id with the vanilla bow — the
     * only bow the port ships.</p>
     *
     * <p>{@code :181} ({@code func_82162_bC}) and {@code :182-183}
     * ({@code func_98053_h}) are vanilla hooks whose MCP names are unresolvable
     * from the vendored mappings (fields CSV only), so they ship as documented
     * no-ops; the wand's magical-biome vis charges ({@code :126-143},
     * {@link PechAiLogic#WAND_VIS_BASE} and friends) belong to that same inert
     * wand-roll branch (BD-47).</p>
     */
    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(final ServerLevelAccessor level,
            final DifficultyInstance difficulty, final MobSpawnType reason,
            @Nullable final SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnGroupData); // :167
        this.addRandomEquipment();                                                            // :168
        final ItemStack mainhand = this.getItemBySlot(EquipmentSlot.MAINHAND);                // :170
        if (!mainhand.isEmpty()) {
            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(mainhand.getItem());
            final boolean wand = wandRegistered() && id.equals(PechAiLogic.WAND_ITEM);
            final boolean bow = id.equals(PechAiLogic.BOW_ITEM);
            this.setPechType(PechAiLogic.pechTypeFor(wand, bow));                             // :172-178
            if (wand) {
                this.setDropChance(EquipmentSlot.MAINHAND, PechAiLogic.EQUIP_DROP_CHANCE_WAND); // :173
            }
            // :181 — func_82162_bC(): unresolved vanilla hook (see javadoc).
        }
        data = super.finalizeSpawn(level, difficulty, reason, data);                           // :184
        return data;
    }

    /**
     * {@code :185-209} — {@code func_70601_bi} (getCanSpawnHere): the
     * magical-biome gate ({@code :187}), the taint / magical-forest / eerie
     * exclusions ({@code :188-192}) and the at-most-4-pechs-in-a-16-inflated-box
     * cap ({@code :193-200}). 1.21.1's seam is
     * {@code Mob#checkSpawnRules(LevelAccessor, MobSpawnType)} — this repo's
     * resolution of that hook. The overworld-only factor ({@code :186}) rides
     * the spawn-table registration (c6's mob_spawn row 347); the village factor
     * is BD-49, and 1.21.1's own ground/light rules come from the super call.
     */
    @Override
    public boolean checkSpawnRules(final LevelAccessor level, final MobSpawnType reason) {
        if (!super.checkSpawnRules(level, reason)) {
            return false;
        }
        final Holder<Biome> biome = level.getBiome(this.blockPosition());                      // :187
        if (!PechAiLogic.isMagicBiomeTag(biome.is(Tags.Biomes.IS_MAGICAL))) {
            return false;
        }
        final ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        final ResourceLocation id = key == null ? null : key.location();
        if (PechAiLogic.isExcludedBiome(matches(id, PechAiLogic.BIOME_TAINT),
                matches(id, PechAiLogic.BIOME_MAGICAL_FOREST),
                matches(id, PechAiLogic.BIOME_EERIE))) {                                       // :188-192
            return false;
        }
        int pechs = 0;                                                                         // :193-200
        for (final Entity entity : level.getEntities(this,
                this.getBoundingBox().inflate(PechAiLogic.SPAWN_BOX_INFLATE))) {
            if (entity instanceof EntityPech) {
                pechs++;
            }
        }
        return pechs <= PechAiLogic.MAX_PECHS_IN_BOX;
    }

    private static boolean matches(@Nullable final ResourceLocation id, final ResourceLocation other) {
        return id != null && id.equals(other);
    }

    // ---- persistence (:261-298) ----

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("PechType", (byte) this.getPechType());                                    // :266
        tag.putShort("Anger", (short) this.getAnger());                                        // :267
        tag.putByte("Tamed", (byte) (this.isTamed() ? 1 : 0));                                 // :268
        final ListTag list = new ListTag();                                                    // :269
        for (final ItemStack stack : this.loot) {
            final CompoundTag entry = new CompoundTag();
            if (!stack.isEmpty()) {
                entry.merge((CompoundTag) stack.save(this.registryAccess()));                     // :272
            }
            list.add(entry);                                                                   // :273
        }
        tag.put("Loot", list);                                                                 // :274
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setPechType(tag.getByte("PechType"));                                             // :279
        this.setAnger(tag.getShort("Anger"));                                                  // :280
        this.setTamed(tag.getByte("Tamed") == 1);                                              // :281
        final ListTag list = tag.getList("Loot", Tag.TAG_COMPOUND);                            // :282
        for (int i = 0; i < this.loot.length; i++) {
            // The original loads an item from the i-th compound; a compound with
            // no id is the empty-slot shape, which parseOptional reproduces.
            this.loot[i] = i < list.size()
                    ? ItemStack.parseOptional(this.registryAccess(), list.getCompound(i))
                    : ItemStack.EMPTY;                                                         // :283-286
        }
        if (this.getPechType() > 0) {
            this.setCombatTask();                                                              // :288
        }
    }

    // ---- drops (:324-350) ----

    /**
     * {@code :324-346} — {@code func_70628_a} (dropFewItems): every loot slot
     * drops at {@code rand < 0.88} ({@code :333}) at the box-width offset
     * ({@code :334}); {@code nextInt(10) < 1 + looting} throws the itemResource
     * meta 18 ({@code :341-344}); and {@code 1 + looting} mana beans (meta 0,
     * with an empty essence list, the shape {@code EntityWisp}/{@code TileSpinningBed}
     * use for a dropped bean). The rolls read the pech's own RNG, as 1.7.10's
     * {@code field_70146_Z} does. The looting level is the {@code func_70692_ba}
     * value — NOT 1.21.1's {@code shouldDropLoot}, which gates the whole
     * death-loot block (the {@code EntityTaintacleGiant} rule).
     */
    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source,
            final boolean recentlyHit) {
        final int looting = lootingLevel(source);
        for (final ItemStack stack : this.loot) {                                              // :331-338
            if (!stack.isEmpty() && this.random.nextFloat() < PechAiLogic.LOOT_DROP_CHANCE) {
                this.spawnAtLocation(stack, (float) this.getBbWidth());
            }
        }
        if (PechAiLogic.dropsResource(this.random.nextInt(PechAiLogic.MANA_BEAN_ROLL), looting)) { // :341
            this.spawnAtLocation(ItemResource.stack(PechAiLogic.RESOURCE_DROP_META),
                    (float) this.getBbWidth());                                                // :344
        }
        for (int i = 0; i < PechAiLogic.manaBeanCount(looting); i++) {
            final ItemStack bean = new ItemStack(CatalogResources.ITEM_MANA_BEAN.get());
            ((ItemManaBean) bean.getItem()).setAspects(bean, new AspectList());
            this.spawnAtLocation(bean, (float) this.getBbWidth());
        }
    }

    /**
     * {@code :348-350} — {@code func_70099_a(int)} ({@code dropAllLoot(fortune)}):
     * the original replaces the parent's equipment drop with one itemResource,
     * meta 9. 1.21.1's per-death equipment hook is
     * {@code LivingEntity#dropEquipment()} (this family's seam, c2).
     */
    @Override
    protected void dropEquipment() {
        this.spawnAtLocation(ItemResource.stack(PechAiLogic.DROP_ALL_LOOT_META),
                (float) this.getBbWidth());                                                    // :348-350
    }

    /** The looting level of the kill: {@code minecraft.enchantment} is a
     *  datapack registry, so the holder resolves through the killer's registry
     *  access (this repo's pattern, {@code TileArcaneBore.java:216}). */
    protected static int lootingLevel(final DamageSource source) {
        final Entity killer = source.getEntity();
        if (killer instanceof LivingEntity living) {
            final Holder<Enchantment> looting = living.registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.LOOTING);
            return EnchantmentHelper.getEnchantmentLevel(looting, living);
        }
        return 0;
    }

    // ---- client signals (:352-397) ----

    /**
     * {@code :352-397} — {@code func_70103_a} (handleStatusEvent): 16 sets the
     * mumble to PI, 17 to 2*PI, 18 draws 5 happy particles and 19 draws 5 angry
     * ones ({@code :354-372}); every other id goes to the parent. 1.21.1 renamed
     * the receiver to {@code Entity#handleEntityEvent(byte)}
     * ({@code Entity.java:2312}); the sender is
     * {@code Level#broadcastEntityEvent(Entity, byte)}
     * ({@code Level.java:1018}), so the server side sends through the level.
     */
    @Override
    public void handleEntityEvent(final byte eventId) {
        if (eventId == PechAiLogic.EVENT_MUMBLE) {
            this.mumble = (float) Math.PI;                                                     // :355
        } else if (eventId == PechAiLogic.EVENT_MUMBLE_TRADE) {
            this.mumble = (float) Math.PI * 2.0F;                                              // :358
        } else if (eventId == PechAiLogic.EVENT_TAMED) {
            for (int i = 0; i < PechAiLogic.PARTICLE_COUNT; i++) {
                drawVillagerParticles(true);                                                   // :360-365
            }
        } else if (eventId == PechAiLogic.EVENT_ANGRY) {
            for (int i = 0; i < PechAiLogic.PARTICLE_COUNT; i++) {
                drawVillagerParticles(false);                                                  // :366-372
            }
        } else {
            super.handleEntityEvent(eventId);
        }
    }

    /** The original's particle-box formula: a width-spread x/z
     *  ({@code getX() + rand * width - depth}), a {@code 0.5 + height * rand} y,
     *  and a 0.02 gaussian. Shared by the event draws and the per-tick draws. */
    private void drawVillagerParticles(final boolean happy) {
        if (!this.level().isClientSide) {
            return;
        }
        final double sx = this.random.nextGaussian() * PechAiLogic.PARTICLE_SIGMA;
        final double sy = this.random.nextGaussian() * PechAiLogic.PARTICLE_SIGMA;
        final double sz = this.random.nextGaussian() * PechAiLogic.PARTICLE_SIGMA;
        final double px = this.getX() + this.random.nextFloat() * this.getBbWidth() * 2.0F
                - this.getBbWidth();
        final double py = this.getY() + 0.5 + this.random.nextFloat() * this.getBbHeight();
        final double pz = this.getZ() + this.random.nextFloat() * this.getBbWidth() * 2.0F
                - this.getBbWidth();
        this.level().addParticle(happy ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER,
                px, py, pz, sx, sy, sz);
    }

    // ---- sound (:399-438) ----

    /**
     * {@code :399-418} — {@code func_70642_aH} (playLivingSound): server-side, a
     * 1-in-3 roll scans the 4x2x4 box; a second pech in it sends event 17 plus
     * the trade sound and returns, otherwise event 16. Then the parent's ambient
     * play. The original sends events, not chat lines.
     */
    @Override
    public void playAmbientSound() {                                                            // 1.21.1: Mob#playAmbientSound is public
        if (!this.level().isClientSide) {                                                      // :401
            if (this.random.nextInt(3) == 0) {
                final List<Entity> nearby = this.level().getEntities(this,
                        this.getBoundingBox().expandTowards(4.0, 2.0, 4.0));                   // :403
                for (final Entity entity : nearby) {
                    if (entity instanceof EntityPech) {                                        // :406-410
                        this.level().broadcastEntityEvent(this, PechAiLogic.EVENT_MUMBLE_TRADE);
                        this.playSound(ModSounds.PECH_TRADE.get(), this.getSoundVolume(),
                                this.getVoicePitch());
                        return;
                    }
                }
            }
            this.level().broadcastEntityEvent(this, PechAiLogic.EVENT_MUMBLE);                     // :415
        }
        super.playAmbientSound();                                                              // :418
    }

    /** {@code :420-422} — {@code func_70627_aG} = 120. */
    @Override
    public int getAmbientSoundInterval() {
        return PechAiLogic.AMBIENT_SOUND_INTERVAL;
    }

    /** {@code :424-426} — {@code func_70647_i} = 0.4. */
    @Override
    public float getVoicePitch() {
        return PechAiLogic.VOICE_PITCH;
    }

    /** {@code :430-432} — {@code func_70599_aP} = 0.4. */
    @Override
    protected float getSoundVolume() {
        return PechAiLogic.VOICE_PITCH;
    }

    /** {@code :428-430} — {@code func_70639_aQ} = thaumcraft:pech_idle. */
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.PECH_IDLE.get();
    }

    /** {@code :432-434} — {@code func_70621_aR} = thaumcraft:pech_hit. */
    @Nullable
    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return ModSounds.PECH_HIT.get();
    }

    /** {@code :436-438} — {@code func_70673_aS} = thaumcraft:pech_death. */
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PECH_DEATH.get();
    }

    /**
     * {@code :440-442} — {@code func_70782_k} (findEntityToAttack): a pech only
     * acquires a target while angry. 1.21.1's seam is the target getter (the
     * goal engine reads it), gated on
     * {@link PechAiLogic#hasTargetPermission(int)} with the original's parent
     * range on {@code setNoAggressive}.
     */
    @Nullable
    @Override
    public LivingEntity getTarget() {
        return PechAiLogic.hasTargetPermission(this.getAnger()) ? super.getTarget() : null;
    }

    // ---- attack modes (:444-492) ----

    /**
     * {@code :464-492} — {@code func_70590_b} (attackEntityWithRangedAttack).
     * Type 2 fires an arrow at motion 1.6 with inaccuracy
     * {@code 14 - difficulty*4} and damage
     * {@code 2.0 + gaussian*0.25 + difficulty*0.11*power}: the gaussian is the
     * WORLD RNG ({@code :468}) and the punch roll the ENTITY RNG ({@code :472}) —
     * the original's split — and the difficulty is 1.21.1's live value behind
     * 1.7.10's {@code worldObj.difficultySetting}. Type 1 fires an
     * {@link EntityPechBlast} with the call-site arguments {@code (1, 0,
     * nextFloat() < 0.1F)} ({@code :481}), aimed at
     * {@code targetY + eyeHeight - 1.500000023841858} with the flat-arc
     * compensation, at motion 1.5 / spread 4.0 ({@code :482-485}).
     */
    @Override
    public void performRangedAttack(final LivingEntity target, final float distanceFactor) {
        if (this.getPechType() == PechAiLogic.TYPE_BOW) {                                      // :465
            final int difficulty = this.level().getDifficulty().getId();
            // 1.21.1's Arrow has no (level, shooter, velocity, inaccuracy) ctor:
            // the shape is (level, owner, pickupStack, firedFromWeapon)
            // (Arrow.java:34) and the motion is set by shoot(...) below. The
            // pickup stack is EMPTY — 1.7.10's mob-arrow pickup mode lives in
            // the vanilla EntityArrow ctor, which is not vendored under
            // reference/, so no pickup behaviour is invented (recorded).
            final Arrow arrow = new Arrow(this.level(), this, ItemStack.EMPTY,
                    this.getMainHandItem());                                    // :466 + the 1.21.1 firedFromWeapon seam
            arrow.setBaseDamage(PechAiLogic.arrowDamage(distanceFactor,
                    this.level().random.nextGaussian(), difficulty));                           // :467-471
            arrow.setPos(this.getX(), this.getY() + this.getEyeHeight(), this.getZ());
            arrow.shoot(target.getX() - this.getX(),
                    target.getY() + target.getEyeHeight() * 0.333F
                            - this.getY() - this.getEyeHeight(),
                    target.getZ() - this.getZ(),
                    (float) PechAiLogic.ARROW_SPEED,
                    (float) PechAiLogic.arrowInaccuracy(difficulty));                           // :466
            // :471-473 — Power and Punch read the pech's HELD ITEM in 1.7.10 and
            // are applied at impact in 1.21.1 through the `firedFromWeapon`
            // association passed above (AbstractArrow.java:450), so there is
            // nothing to compute here: a pech holding an enchanted bow fires an
            // enchanted arrow. A pech with no bow (the unported wand branch is
            // type 2, not type 1) never reaches this leg.
            this.level().addFreshEntity(arrow);
            this.level().playSound(null, this, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                    PechAiLogic.BOW_SOUND_VOLUME,
                    (float) PechAiLogic.bowSoundPitch(this.random.nextFloat()));                // :476
        } else {
            final EntityPechBlast blast = new EntityPechBlast(CatalogPech.PECH_BLAST.get(),
                    this.level(), this, PechAiLogic.TYPE_WAND, 0,
                    this.random.nextFloat() < PechAiLogic.BLAST_SMOKE_CHANCE);                  // :481
            final double dx = target.getX() - this.getX();                                     // :482
            final double dy = PechAiLogic.blastAimY(target.getY(), target.getEyeHeight(),
                    this.getY());
            final double dz = target.getZ() - this.getZ();
            blast.shoot(dx, dy + PechAiLogic.blastArc(Mth.sqrt((float) (dx * dx + dz * dz))),
                    dz, PechAiLogic.BLAST_MOTION, PechAiLogic.BLAST_SPREAD);                    // :483-485
            this.level().addFreshEntity(blast);
            this.level().playSound(null, this, ModSounds.ICE.get(), SoundSource.NEUTRAL,
                    0.4F, 1.0F + this.random.nextFloat() * 0.1F);                               // :486-487
        }
    }

    // ---- anger (:494-537) ----

    /**
     * {@code :494-505} — {@code becomeAngryAt}: remember the player and set this
     * pech's fuse to {@code 400 + nextInt(400)} (the roll REPLACES the counter). The original does NOT
     * re-scan here — the 32x16x32 scan lives in the hurt override
     * ({@code :516-537}).
     */
    public void becomeAngryAt(final Entity source) {
        this.angryAt = source instanceof Player player ? player : null;                      // :495 (field_70789_a)
        // :495 — 1.7.10's `field_70789_a` (revengeTarget, the field the
        // hurt-by-target goal reads) is gone in 21.1.259: the mob's re-target
        // state is `LivingEntity.lastHurtByMob` (LivingEntity.java:237) plus
        // this.setTarget below. Record the attacker in the modern field.
        if (source instanceof LivingEntity living) {
            this.setLastHurtByMob(living);                                                   // :495
        }
        if (this.getAnger() <= 0) {                                                          // :496
            this.level().broadcastEntityEvent(this, PechAiLogic.EVENT_ANGRY);                // :497 (byte 19)
            this.playSound(ModSounds.PECH_CHARGE.get(), this.getSoundVolume(),
                    this.getVoicePitch());                                                   // :498
        }
        // :502 — the original's `func_70624_b((EntityLivingBase) par1Entity)` cast is
        // unconditional; its only caller (the hurt handler, :516-537) passes a player, so
        // the instanceof guard below is the 1.21.1-safe form of the same assignment.
        if (source instanceof LivingEntity living) {
            this.setTarget(living);                                                          // :502
        }
        this.setAnger(PechAiLogic.rollAnger(this.random.nextInt(PechAiLogic.ANGER_JITTER))); // :503 (the original ASSIGNS the roll; it does not add)
        this.setTamed(false);                                                                // :504
        this.updateAINextTick = true;                                                        // :505
    }

    /**
     * {@code :516-537} — {@code func_70097_a} (attackEntityFrom): the
     * AI-disabled early exit ({@code func_85032_ar} → {@code isNoAi()}), then a
     * player attacker enrages every pech in the 32x16x32 box and this pech, then
     * vanilla.
     */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (this.isNoAi()) {                                                                   // :518
            return false;
        }
        final Entity entity = source.getEntity();                                              // :521
        if (entity instanceof Player) {
            final List<Entity> list = this.level().getEntities(this,
                    this.getBoundingBox().inflate(32.0, 16.0, 32.0));                          // :523
            for (final Entity e : list) {
                if (e instanceof EntityPech pech) {
                    pech.becomeAngryAt(entity);                                                // :526-528
                }
            }
            this.becomeAngryAt(entity);                                                        // :531
        }
        return super.hurt(source, amount);                                                     // :534
    }

    // ---- per-tick bodies (:539-610) ----

    /**
     * {@code :539-598} — {@code func_70071_h_} (onUpdate), pre-super body in
     * order: mumble decays x0.75; anger decays 1/tick; while angry with a target
     * held, the growl cadence runs (the 100-tick counter, the charge sound, then
     * event 17); and the client draws the vanilla villager particles at rolls 15
     * (angry) and 25 (tamed). 1.21.1's per-tick mob body is {@code aiStep} —
     * {@code Mob#serverAiStep} idles {@code customServerAiStep} (this family's
     * measured fact) — so the body rides {@code aiStep} exactly as the original's
     * rides {@code onUpdate}. The explicit target-find/set pair
     * ({@code :546-548}) is 1.21.1's target selector; the port keeps the cadence
     * and the event that follow it.
     */
    @Override
    public void aiStep() {
        if (this.mumble > 0.0F) {
            this.mumble *= PechAiLogic.MUMBLE_DECAY;                                           // :541-544
        }
        if (this.getAnger() > 0) {
            this.setAnger(this.getAnger() - 1);                                                // :546-549
        }
        if (this.getAnger() > 0 && this.getTarget() != null) {                                 // :551-556
            if (this.chargecount > 0) {
                this.chargecount--;                                                            // :557
            }
            if (this.chargecount == 0) {
                this.chargecount = PechAiLogic.CHARGE_CADENCE;                                 // :559
                this.playSound(ModSounds.PECH_CHARGE.get(), this.getSoundVolume(),
                        this.getVoicePitch());                                                 // :560
            }
            this.level().broadcastEntityEvent(this, PechAiLogic.EVENT_MUMBLE_TRADE);               // :563
        }
        if (this.level().isClientSide && this.random.nextInt(PechAiLogic.ANGRY_PARTICLE_ROLL) == 0
                && this.getAnger() > 0) {
            drawVillagerParticles(false);                                                      // :565-580
        }
        if (this.level().isClientSide && this.random.nextInt(PechAiLogic.TAMED_PARTICLE_ROLL) == 0
                && this.isTamed()) {
            drawVillagerParticles(true);                                                       // :582-596
        }
        super.aiStep();
    }

    /**
     * {@code :600-610} — {@code func_70619_bc} (onEntityUpdate): the
     * mainhand-changed flag clears and re-derives the combat goal; after the
     * parent, {@code ticksExisted % 40 == 0} heals 1.0. 1.21.1's seam for that
     * hook is {@code Mob#customServerAiStep} (this repo's resolution), and it
     * runs every tick, so the %40 gate is kept verbatim.
     */
    @Override
    protected void customServerAiStep() {
        if (this.updateAINextTick) {                                                           // :601
            this.updateAINextTick = false;                                                     // :602
            this.setCombatTask();                                                              // :603
        }
        super.customServerAiStep();
        if (this.tickCount % 40 == 0) {                                                        // :607-609
            this.heal(1.0F);
        }
    }

    // ---- trade and pickups (:612-745) ----

    /**
     * {@code :612-630} — {@code canPickup}: while the pech is wild, an item that
     * is in the valued table is always worth chasing; otherwise it needs a free
     * slot (a slot whose count reached 0 is cleared first, {@code :617-619}) or a
     * stack that merges into a slot with room. 1.7.10's
     * {@code InventoryUtils.areItemStacksEqualStrict} (same item, same NBT) is
     * 21.1.259's {@link ItemStack#isSameItemSameComponents(ItemStack, ItemStack)}
     * ({@code ItemStack.java:632}) — component equality is the modern form of the
     * original's strict NBT equality.
     */
    public boolean canPickup(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;                                                                        // :613-614
        }
        if (!this.isTamed()
                && PechAiLogic.VALUED_ITEMS.containsKey(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return true;                                                                         // :615
        }
        for (int a = 0; a < this.loot.length; a++) {                                             // :616-628
            if (!this.loot[a].isEmpty() && this.loot[a].getCount() <= 0) {
                this.loot[a] = ItemStack.EMPTY;                                                   // :617-619
            }
            if (this.loot[a].isEmpty()) {
                return true;                                                                      // :620-622
            }
            if (ItemStack.isSameItemSameComponents(stack, this.loot[a])
                    && stack.getCount() + this.loot[a].getCount() <= this.loot[a].getMaxStackSize()) {
                return true;                                                                      // :623-627
            }
        }
        return false;                                                                             // :629
    }

    /**
     * {@code :619-692} — {@code pickupItem}: absorbs the stack and returns what
     * is LEFT (the original returns {@code null} when the whole stack went in,
     * and the pickup goal writes the remainder back to the item entity,
     * {@code AIPechItemEntityGoto.java:140}). Three passes, verbatim:
     *
     * <ol><li>while wild, a <b>valued</b> stack gives up ONE item per call, and
     * the {@code nextInt(10) < getValue(stack)} roll tames the pech, flags the AI
     * for a re-path and sends event 18 (the 5 happy particles, {@code :360-365})
     * — {@code :622-629};</li>
     * <li>the merge pass over the 9 loot slots ({@code :631-654}), transferring
     * the whole stack when it fits and exactly the room left otherwise;</li>
     * <li>the empty-slot copy pass ({@code :656-668}).</li></ol>
     *
     * <p>The argument stack is mutated in place, as the original mutates the item
     * entity's stack. The original's slots are reference-typed and start null;
     * 1.21.1's {@code ItemStack} is a value object, so an EMPTY slot is this
     * port's representation of the original's null slot (the original nulls a
     * slot the moment its count hits 0, {@code :617}) — every {@code == null}
     * test of the original reads as {@code isEmpty()} here.</p>
     */
    @Nullable
    public ItemStack pickupItem(final ItemStack picked) {
        // The original reassigns its parameter (`entityItem = null` once the stack
        // is drained), so the port mutates a local alias of it.
        ItemStack entityItem = picked;
        if (entityItem.isEmpty()) {
            return entityItem;                                                                    // :620-622
        }
        if (!this.isTamed() && this.isValued(entityItem)) {                                       // :622
            if (this.random.nextInt(PechAiLogic.TAME_ROLL_BOUND) < this.getValue(entityItem)) {   // :623
                this.setTamed(true);                                                              // :624
                this.updateAINextTick = true;                                                     // :625
                this.level().broadcastEntityEvent(this, PechAiLogic.EVENT_TAMED);                 // :626 (byte 18)
            }
            entityItem.shrink(1);                                                                 // :628
            return entityItem.isEmpty() ? null : entityItem;                                      // :629
        }
        for (int a = 0; a < this.loot.length; a++) {                                              // :631-654
            if (!this.loot[a].isEmpty() && this.loot[a].getCount() <= 0) {
                this.loot[a] = ItemStack.EMPTY;                                                   // :632-634
            }
            if (!entityItem.isEmpty() && entityItem.getCount() > 0 && !this.loot[a].isEmpty()
                    && this.loot[a].getCount() < this.loot[a].getMaxStackSize()
                    && ItemStack.isSameItemSameComponents(entityItem, this.loot[a])) {             // :636-640
                final int want = entityItem.getCount();
                final int transfer = PechAiLogic.mergeTransfer(this.loot[a].getCount(),
                        this.loot[a].getMaxStackSize(), want);                                    // :641-650
                this.loot[a].setCount(this.loot[a].getCount() + transfer);
                entityItem.shrink(transfer);
                if (transfer >= want) {
                    return null;                                                                  // :645-647
                }
            }
            if (!entityItem.isEmpty() && entityItem.getCount() <= 0) {
                entityItem = ItemStack.EMPTY;                                                     // :652-654
            }
        }
        for (int a = 0; a < this.loot.length; a++) {                                              // :656-668
            if (!this.loot[a].isEmpty() && this.loot[a].getCount() <= 0) {
                this.loot[a] = ItemStack.EMPTY;                                                   // :657-659
            }
            if (!entityItem.isEmpty() && entityItem.getCount() > 0 && this.loot[a].isEmpty()) {
                this.loot[a] = entityItem.copy();                                                 // :662-664
                return null;
            }
        }
        return entityItem.isEmpty() ? null : entityItem;                                          // :670-672
    }

    /**
     * {@code :707-722} — {@code isValued}: in the valued table, or its aspect
     * list carries the greed aspect (1.7.10's {@code Aspect.GREED} = this port's
     * {@code lucrum}, {@code AspectDefinitions.java:70}). The two aspect sources
     * are the tag table ({@code ThaumcraftCraftingManager.getObjectTags}) and the
     * stack's own carried essentia: the original's second source is
     * {@code getBonusTags(item, objectTags)} ({@code :714}), which merged the
     * stack's NBT essentia — in this port that is the
     * {@code thaumcraft:essentia_container} component behind
     * {@link IEssentiaContainerItem#getAspects(ItemStack)} (the family's
     * item-essence seam). Recorded in the row's {@code known_differences}.
     */
    public boolean isValued(final ItemStack item) {
        if (item.isEmpty()) {
            return false;                                                                         // :708-710
        }
        if (PechAiLogic.VALUED_ITEMS.containsKey(BuiltInRegistries.ITEM.getKey(item.getItem()))) {
            return true;                                                                          // :711
        }
        final AspectList al = PechAiLogic.mergeAspectLists(EssentiaTags.getObjectTags(item),
                carriedEssentia(item));                                                           // :713-714
        return al != null
                && al.getAmount(Aspect.getAspect(PechAiLogic.GREED_ASPECT_ID)) > 0;               // :715-717
    }

    /** The stack's own carried essentia — the port's {@code getBonusTags} side. */
    @Nullable
    private static AspectList carriedEssentia(final ItemStack stack) {
        return stack.getItem() instanceof IEssentiaContainerItem container
                ? container.getAspects(stack)
                : null;
    }

    /**
     * The 1.21.1 seam for the trade goal's 1.7.10 {@code field_70133_I}
     * ({@code velocityChanged},
     * {@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv}). 21.1.259
     * keeps that flag private with no getter (the motion setters write it), so
     * the observable state it guards — the pech is not being moved this tick —
     * is measured from the live delta movement. Recorded as a deviation; the
     * rule itself stays on {@link PechAiLogic#canStartTrade}.
     */
    public boolean isStandingStill() {
        return this.getDeltaMovement().lengthSqr() < PechAiLogic.STANDING_STILL_EPSILON_SQ;
    }

    /**
     * {@code :694-705} — {@code func_70085_c} (processRightClickEntity): start a
     * trade when not trading, not tamed, the pech is not attacking a player, and
     * the held item is not a name tag. The GUI the original opens there
     * ({@code openGui(Thaumcraft, 1, ...)}) is the container/menu lane —
     * {@code left_out_of_scope}; the goal, the flags and the rules ship.
     */
    @Override
    protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        if (!this.trading && !this.isTamed() && !(this.getTarget() instanceof Player)
                && !player.getMainHandItem().is(Items.NAME_TAG)) {                             // :696-699
            this.trading = true;                                                               // :700
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);                                                // :704
    }

    /**
     * {@code :707-735} — {@code getValue}: the six-entry table (mana bean 1,
     * gold ingot 2, golden apple 2, ender pearl 3, diamond 4, emerald 5) via
     * {@link PechAiLogic#tableValue}, else the stack's essence list carrying the
     * greed aspect (1.7.10's {@code Aspect.GREED} = this port's {@code lucrum}),
     * capped at {@link PechAiLogic#VALUE_CAP}. A negative table value means "not
     * in the table", so the aspect pass runs.
     */
    public int getValue(final ItemStack stack) {
        final int table = PechAiLogic.tableValue(BuiltInRegistries.ITEM.getKey(stack.getItem())); // :707-722
        if (table >= 0) {
            return table;
        }
        final AspectList list = PechAiLogic.mergeAspectLists(EssentiaTags.getObjectTags(stack),
                carriedEssentia(stack));                                                        // :729-730
        if (list != null) {
            final int greed = list.getAmount(Aspect.getAspect(PechAiLogic.GREED_ASPECT_ID));    // :731
            if (greed > 0) {
                return PechAiLogic.aspectValue(greed);                                         // :732 (Math.min(32, amount))
            }
        }
        return 0;
    }

    /** {@code :639-644} — the tame roll: {@code nextInt(10) < value}. */
    public boolean tryTame(final ItemStack stack) {
        final int value = getValue(stack);                                                     // :640
        if (PechAiLogic.tames(this.random.nextInt(PechAiLogic.TAME_ROLL_BOUND), value)) {        // :641
            this.setTamed(true);
            return true;
        }
        return false;
    }
}
