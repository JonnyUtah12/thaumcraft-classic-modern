package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * The Furious Zombie — {@code thaumcraft:giantbrainyzombie} (content-mapping
 * {@code Thaumcraft:GiantBrainyZombie}, mod_index 20).
 *
 * <p>Original: {@code thaumcraft.common.entities.monster.EntityGiantBrainyZombie},
 * 90 lines, {@code extends EntityBrainyZombie}. Its whole surface, read from the
 * source: experience value 15 ({@code :14}), a self-scaling hitbox driven by an
 * <b>anger</b> data value ({@code :15-16}, {@code :22-24}, {@code :30-41}), a
 * 0.4 leap goal ({@code :17}), health 60.0 / attack 7.0 ({@code :52-53}), +0.1
 * anger per hit capped at 2.0 ({@code :44-46}), an attack-damage rewrite every
 * tick from the anger ({@code :27}), doubled drop rolls AND doubled drop stacks
 * ({@code :57-73}), and a replacement of vanilla {@code dropAllLoot} with a
 * 4-way drop of its own ({@code :75-89}).</p>
 *
 * <h2>Shape mapping</h2>
 * <ul>
 *   <li><b>The anger value</b> ({@code :30-41}) is a data-watcher value at index
 *       20 initialised to {@code new Float(1.0F)} — so it is synced to clients
 *       (the size is visible) and it is what the save round-trips. 1.21.1's
 *       equivalent is a {@code SynchedEntityData} accessor defined for this
 *       class ({@link #DATA_ANGER}); the save key is
 *       {@link BrainyZombieAiLogic#ANGER_TAG}.</li>
 *   <li><b>The size</b> ({@code :15-16}, {@code :24}) multiplies a 0.6 x 1.8
 *       base by {@code 1.2 + anger}. 1.21.1 already carries an entity's size
 *       factor as the {@code generic.scale} attribute
 *       ({@code Attributes.SCALE}, {@code Attributes.java:83}):
 *       {@code LivingEntity#getScale()} reads it (:528-530),
 *       {@code getDimensions} scales the box by it (:3250) and {@code aiStep}
 *       refreshes the box the tick it changes (:2440-2443). The original's
 *       factor is that same number, so the port writes
 *       {@code 1.2 + anger} to the SCALE base — the ctor's
 *       {@code func_70105_a(width * s, height * s)} and the tick's
 *       {@code func_70105_a(0.6F * s, 1.8F * s)} both become one attribute
 *       write, and the entity type's {@code .sized(0.6F, 1.8F)} is the base the
 *       factor multiplies.</li>
 *   <li>{@code func_70636_d} ({@code :20-28}) — {@code onLivingUpdate}: super
 *       first, then the decay-and-resize inside the {@code anger > 1.0} branch
 *       ({@code :22-25}), then the attack-damage write ({@code :27}) every tick.
 *       The 1.21.1 server-AI hook is {@link #customServerAiStep()} (the
 *       resolution this repo already uses for {@code func_70626_be}). The
 *       damage write lands on the attribute <b>base</b>, matching 1.7.10's
 *       {@code func_111128_a} (setAttribute).</li>
 *   <li>{@code func_70097_a} ({@code :43-47}) — the anger bump happens
 *       <b>before</b> {@code super.attackEntityFrom}, i.e. a fatal hit still
 *       raises the anger first. 1.21.1's hook is
 *       {@code hurt(DamageSource, float)}.</li>
 *   <li>{@code func_70628_a} ({@code :56-73}) — {@code dropFewItems}: two loops
 *       of six world-RNG coin flips, each drop <b>2</b> rotten flesh (twice the
 *       angry zombie's rolls and twice its stack), then the same looting gate
 *       throwing the brain at 2.0. No super call, so the angry zombie's body is
 *       replaced, exactly as the original's is.</li>
 *   <li>{@code func_70099_a(int)} ({@code :75-89}) — 1.7.10's
 *       {@code dropAllLoot(fortune)}: the {@code func_70099_a} CALLS in this
 *       same file ({@code :71}, {@code :78}, {@code :87}) take
 *       {@code (ItemStack, float)}, so {@code entityDropItem} is
 *       {@code func_70099_a(ItemStack, float)} and the {@code (int)} override
 *       here is 1.7.10's {@code dropAllLoot(int)} — the same override
 *       {@code EntityPech.java:348}, {@code EntityCultist.java:60} and the
 *       bosses make. 1.21.1's per-death equipment hook is
 *       {@code LivingEntity#dropEquipment()}, called from the private
 *       {@code dropAllLoot(DamageSource, boolean)}; replacing it reproduces the
 *       original's replacement. Its 4-way pick uses the <b>entity's</b> RNG
 *       ({@code field_70146_Z}, fields CSV), not the world's — the split between
 *       the two RNGs in the original is preserved. The two {@code itemResource}
 *       cases ride this port's {@code thaumcraft:item_meta} component through
 *       {@link ItemResource#stack(int)} (meta 2 = thaumium ingot, meta 6 =
 *       amber); carrot and potato are the vanilla items the fields CSV resolves
 *       ({@code field_151172_bF}, {@code field_151174_bG}).</li>
 *   <li>Registration args {@code (64, 3, true)} ({@code ConfigEntities.java:97}).</li>
 * </ul>
 *
 * <p>Every figure lives in {@link BrainyZombieAiLogic} and is pinned world-free
 * by {@code BrainyZombieAiLogicTest}; the live registry id, the scaled hitbox,
 * the attributes, the experience value, the leap goal, the anger growth / decay
 * / attack rewrite / box shrink and the doubled drops are proven in the world by
 * {@code ZombieMobGameTest}.</p>
 */
public class EntityGiantBrainyZombie extends EntityBrainyZombie {

    /** {@code :30-32} — {@code dataWatcher} index 20, {@code new Float(1.0F)}. */
    private static final EntityDataAccessor<Float> DATA_ANGER =
            SynchedEntityData.defineId(EntityGiantBrainyZombie.class, EntityDataSerializers.FLOAT);

    public EntityGiantBrainyZombie(final EntityType<? extends Zombie> type, final Level level) {
        super(type, level);
        this.xpReward = BrainyZombieAiLogic.FURIOUS_XP;                 // :14 (field_70728_aV)
        // :15-16 — the ctor sizes itself at (1.2 + anger), and the anger data
        // value is 1.0 at this point (:32), i.e. the base box doubled by 2.2.
        this.getAttribute(Attributes.SCALE).setBaseValue(
                BrainyZombieAiLogic.scaleFor(BrainyZombieAiLogic.ANGER_START));
        this.refreshDimensions();
    }

    /** The original's registration args are {@code (64, 3, true)} —
     *  {@code ConfigEntities.java:97}. The base pair is the giant's own
     *  0.6 x 1.8, not the angry zombie's box ({@code :24}). */
    /** Named apart from the parent's {@code buildType}: a static method with
     *  the same signature in a subclass would hide the parent's and Java
     *  forbids the narrower return type there. */
    public static EntityType<EntityGiantBrainyZombie> buildFuriousType(final MobCategory category) {
        return EntityType.Builder.of(EntityGiantBrainyZombie::new, category)
                .sized(BrainyZombieAiLogic.FURIOUS_BASE_WIDTH, BrainyZombieAiLogic.FURIOUS_BASE_HEIGHT) // :24
                .eyeHeight(1.74F)                                                                       // EntityType.java:771 (never overridden)
                .clientTrackingRange(BrainyZombieAiLogic.TRACK_RANGE_CHUNKS)                            // 64 blocks (:97)
                .updateInterval(BrainyZombieAiLogic.UPDATE_INTERVAL)                                    // 3 (:97)
                .build("giantbrainyzombie");
    }

    /** {@code func_110147_ax} ({@code :49-54}) over the angry zombie's figures —
     *  the pair's attribute supplier entries replace the inherited ones. */
    public static AttributeSupplier.Builder createAttributes() {
        return EntityBrainyZombie.createAttributes()
                .add(Attributes.MAX_HEALTH, BrainyZombieAiLogic.FURIOUS_MAX_HEALTH)   // :52
                .add(Attributes.ATTACK_DAMAGE, BrainyZombieAiLogic.FURIOUS_ATTACK_BASE); // :53
    }

    /** {@code :17} — the giant's own goal, on top of the pair's goal shape. */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(BrainyZombieAiLogic.FURIOUS_LEAP_SLOT,
                new LeapAtTargetGoal(this, BrainyZombieAiLogic.FURIOUS_LEAP_CHANCE));
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANGER, BrainyZombieAiLogic.ANGER_START);      // :30-32
    }

    /** {@code :35-37}. */
    public float getAnger() {
        return this.entityData.get(DATA_ANGER);
    }

    /** {@code :39-41}. */
    public void setAnger(final float anger) {
        this.entityData.set(DATA_ANGER, anger);
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat(BrainyZombieAiLogic.ANGER_TAG, this.getAnger());
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(BrainyZombieAiLogic.ANGER_TAG)) {
            this.setAnger(tag.getFloat(BrainyZombieAiLogic.ANGER_TAG));
        }
    }

    /** {@code :43-47} — the anger bump runs before super, so even a killing
     *  blow raises the anger. */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        this.setAnger(BrainyZombieAiLogic.angerAfterHit(this.getAnger()));   // :45
        return super.hurt(source, amount);                                     // :46
    }

    /** {@code :20-28} — super first, then the decay-and-resize branch and the
     *  every-tick attack-damage rewrite.
     *
     *  <p><b>The cadence seam is {@code aiStep}, not {@code customServerAiStep}</b>
     *  (the resolution this repo uses for {@code func_70626_be} in mobs whose
     *  body is a goal-shaped AI). 1.21.1's {@code Mob#serverAiStep} gates
     *  {@code customServerAiStep} behind its idle throttle: an idle mob with no
     *  target and no position to act on returns early and pushes
     *  {@code lastActionTime} forward by {@code (20 + rand(20)) * 3} ticks, so a
     *  0.002-per-tick decay would run at a fraction of the original's rate (the
     *  measured shape in {@code ZombieMobGameTest} was ~18 steps in 60 probe
     *  ticks). The original's {@code func_70636_d} is a plain per-tick update, so
     *  the body rides {@code aiStep} — every tick, as the original's did — and is
     *  gated to the server, because every write it makes (the data watcher value,
     *  the attribute base, the box) is server-authoritative in 1.21.1.</p> */
    @Override
    public void aiStep() {
        super.aiStep();                                        // :21
        if (this.level().isClientSide) {
            return;
        }
        final float anger = this.getAnger();
        if (anger > BrainyZombieAiLogic.ANGER_FLOOR) {                     // :22
            final float decayed = BrainyZombieAiLogic.angerAfterTick(anger);  // :23
            this.setAnger(decayed);
            // :24 — the resize. In 1.21.1 the size factor IS the scale
            // attribute, and LivingEntity#aiStep refreshes the box from it.
            this.getAttribute(Attributes.SCALE)
                    .setBaseValue(BrainyZombieAiLogic.scaleFor(decayed));
            // The original's func_70105_a(0.6F * s, 1.8F * s) resizes in the
            // same tick it recomputes the factor; 1.21.1 refreshes the box from
            // the scale attribute inside LivingEntity#aiStep, so the port asks
            // for the refresh here and the resize stays same-tick.
            this.refreshDimensions();
        }
        // :27 — every tick, unconditionally.
        final AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        final double wanted = BrainyZombieAiLogic.attackDamageFor(this.getAnger());
        if (attack.getBaseValue() != wanted) {
            attack.setBaseValue(wanted);
        }
    }

    /** {@code :56-73} — two loops of six world-RNG coin flips, each drop 2
     *  rotten flesh, then the looting gate with the brain thrown at 2.0. No
     *  super call: the original replaces the angry zombie's body. */
    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source,
            final boolean causedByPlayer) {
        final int looting = lootingLevel(source);                                  // :57 int i
        for (int loop = 0; loop < BrainyZombieAiLogic.FURIOUS_DROP_LOOPS; loop++) { // :58, :64
            for (int a = 0; a < BrainyZombieAiLogic.FURIOUS_ROLLS_PER_LOOP; a++) {
                if (level.getRandom().nextBoolean()) {                             // :59, :65
                    // :60, :66 func_145779_a(rotten_flesh, 2) — TWO per true
                    // flip (the original doubles the angry zombie's stack too).
                    // The count rides the ItemStack: 1.21.1's
                    // spawnAtLocation(ItemLike, int) treats its int as the Y
                    // OFFSET (Entity.java:1853-1855).
                    this.spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH,
                            BrainyZombieAiLogic.FURIOUS_FLESH_STACK),
                            BrainyZombieAiLogic.FLESH_DROP_OFFSET_Y);
                }
            }
        }
        if (BrainyZombieAiLogic.dropsBrain(level.getRandom().nextInt(
                BrainyZombieAiLogic.BRAIN_ROLL_MODULO), looting) && brainItemRegistered()) {
            this.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                            net.minecraft.core.registries.BuiltInRegistries.ITEM.get(BRAIN_ITEM_ID)),
                    BrainyZombieAiLogic.FURIOUS_BRAIN_OFFSET_Y);                   // :70-72 (Y offset 2.0)
        }
    }

    /** {@code :75-89} — the replacement of vanilla {@code dropAllLoot}. The pick
     *  uses the entity's own RNG, as the original's {@code field_70146_Z} does. */
    @Override
    protected void dropEquipment() {
        switch (BrainyZombieAiLogic.lootPick(this.getRandom().nextInt(
                BrainyZombieAiLogic.LOOT_PICK_MODULO))) {                          // :76
            case BrainyZombieAiLogic.LOOT_PICK_THAUMIUM ->
                    this.spawnAtLocation(ItemResource.stack(BrainyZombieAiLogic.LOOT_THAUMIUM_META),
                            BrainyZombieAiLogic.LOOT_THROW_OFFSET_Y);              // :78
            case BrainyZombieAiLogic.LOOT_PICK_CARROT ->
                    this.spawnAtLocation(new ItemStack(Items.CARROT,
                            BrainyZombieAiLogic.LOOT_VEGETABLE_COUNT),
                            BrainyZombieAiLogic.LOOT_VEGETABLE_OFFSET_Y);          // :81
            case BrainyZombieAiLogic.LOOT_PICK_POTATO ->
                    this.spawnAtLocation(new ItemStack(Items.POTATO,
                            BrainyZombieAiLogic.LOOT_VEGETABLE_COUNT),
                            BrainyZombieAiLogic.LOOT_VEGETABLE_OFFSET_Y);          // :84
            default ->
                    this.spawnAtLocation(ItemResource.stack(BrainyZombieAiLogic.LOOT_AMBER_META),
                            BrainyZombieAiLogic.LOOT_THROW_OFFSET_Y);              // :87
        }
    }
}
