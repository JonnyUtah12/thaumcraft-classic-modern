package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Mind Spider — {@code thaumcraft:mindspider} (content-mapping line 288,
 * mod_index 24). Original:
 * {@code thaumcraft.common.entities.monster.EntityMindSpider}, 115 lines,
 * {@code extends EntitySpider}.
 *
 * <p>Read from the source, the class adds five things to the vanilla spider and
 * switches two systems off:</p>
 * <ul>
 *   <li>a <b>harmless</b> mode (data value 22, {@code :32-56}): it arms a
 *       1200-tick lifespan ({@code :52}), zeroes the death xp ({@code :22-24}),
 *       removes the crosshair pick radius ({@code :79-81}) and gates the melee
 *       hit ({@code :98-102});</li>
 *   <li>a <b>viewer</b> name (data value 23, {@code :38-44}) — the mind-control
 *       seam the Thaumcraft spell writes; it ships as data, the riding GUI is
 *       out of scope;</li>
 *   <li>a <b>lifespan</b> fuse, server-side only ({@code :72-77});</li>
 *   <li>attributes pinned to health 1.0 / attack 1.0 ({@code :26-30});</li>
 *   <li>the AI switch forced ON: {@code func_70782_k → isNoAi = true}
 *       ({@code :90-92}), so the whole inherited goal set is inert;</li>
 *   <li>and every drop shut off ({@code :83-88}: {@code getDropItemId =
 *       Item.getItemById(0)} → null, {@code dropFewItems} empty).</li>
 * </ul>
 *
 * <p>Every figure is on {@link MindSpiderAiLogic}. The goal reconciliation
 * (parent slots transcribed from 1.21.1 {@code Spider.java:54-65} minus the
 * post-1.7.10 armadillo-avoidance goal) is documented there and asserted by
 * {@code MindSpiderMobGameTest}; the 1.7.10 vanilla {@code EntitySpider}
 * decompile is not vendored, so no parent slot is invented.</p>
 */
public class EntityMindSpider extends Spider {

    /** {@code :34} — watcher index 22, {@code Byte(0)}: the harmless flag. */
    private static final EntityDataAccessor<Byte> DATA_HARMLESS =
            SynchedEntityData.defineId(EntityMindSpider.class, EntityDataSerializers.BYTE);
    /** {@code :35} — watcher index 23, {@code ""}: the viewer name. */
    private static final EntityDataAccessor<String> DATA_VIEWER =
            SynchedEntityData.defineId(EntityMindSpider.class, EntityDataSerializers.STRING);

    /** {@code :14} — {@code Integer.MAX_VALUE}: a normal mind spider never dies
     *  of old age. */
    private int lifeSpan = MindSpiderAiLogic.DEFAULT_LIFESPAN;

    public EntityMindSpider(final EntityType<? extends EntityMindSpider> type, final Level level) {
        super(type, level);
        // :18 — func_70105_a(0.3F, 0.3F). 1.21.1 rebuilds a mob's box from the
        // EntityType's size (LivingEntity#refreshDimensions), so the figure
        // rides buildType's sized(0.3, 0.3) rather than a constructor call that
        // refreshDimensions would overwrite.
        this.xpReward = MindSpiderAiLogic.XP;                              // :19 (field_70728_aV)
        // The inherited goal set reconciled to 1.7.10. The ONLY provably
        // post-1.7.10 goal is 1.21.1's AvoidEntityGoal against the ARMADILLO
        // (Spider.java:56): the armadillo is a 1.20.11 mob, so the 1.7.10
        // EntitySpider cannot own a goal that runs from it. The 1.7.10 vanilla
        // EntitySpider decompile is not in reference/, so every other goal of
        // the 1.21.1 parent is kept as the transcription of the 1.7.10 set
        // (the c2 convention) and the removal is asserted by
        // MindSpiderMobGameTest.goalSetIsTheSpiderParentsMinusTheArmadilloGoal.
        this.goalSelector.removeAllGoals(g -> g instanceof AvoidEntityGoal<?>);
    }

    /**
     * {@code ConfigEntities.java:105-106} — tracking 64, update 3, velocity
     * updates ON. The 0.3x0.3 box is the constructor's own
     * {@code func_70105_a(0.3F, 0.3F)} ({@code :18}); the 1.7.10 client hook
     * {@code spiderScaleAmount() = 0.3F} ({@code :67-70}) is a RENDER scale and
     * belongs to the renderer (TC-M7-03-s), so {@code generic.scale} stays 1.0
     * here and the box is not double-shrunk. The tracking figure is handed over
     * in CHUNKS because 1.21.1's builder measures it in chunks (64 blocks = 4).
     */
    public static EntityType<EntityMindSpider> buildType(final MobCategory category) {
        return EntityType.Builder.of(EntityMindSpider::new, category)
                .sized(MindSpiderAiLogic.WIDTH, MindSpiderAiLogic.HEIGHT)
                .clientTrackingRange(MindSpiderAiLogic.TRACK_RANGE_CHUNKS)   // 64 blocks = 4 chunks
                .updateInterval(MindSpiderAiLogic.UPDATE_INTERVAL)
                .setShouldReceiveVelocityUpdates(MindSpiderAiLogic.VELOCITY_UPDATES)
                .build("mindspider");
    }

    /** {@code :26-30} — the parent's attribute map, then maxHealth
     *  ({@code field_111267_a}) and attackDamage ({@code field_111264_e}) set to
     *  1.0. The movement speed is untouched, as in the original. */
    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes()
                .add(Attributes.MAX_HEALTH, MindSpiderAiLogic.MAX_HEALTH)      // :28
                .add(Attributes.ATTACK_DAMAGE, MindSpiderAiLogic.ATTACK_DAMAGE); // :29
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HARMLESS, (byte) 0);                               // :34
        builder.define(DATA_VIEWER, "");                                        // :35
    }

    // ---- harmless mode (:46-56) ----

    public boolean isHarmless() {
        return this.entityData.get(DATA_HARMLESS) != 0;                        // :46-48
    }

    /**
     * {@code :50-56}. Note the original's asymmetry: arming harmless mode sets
     * the 1200-tick fuse, clearing it does NOT take the fuse back off. Kept.
     */
    public void setHarmless(final boolean harmless) {
        if (harmless) {
            this.lifeSpan = MindSpiderAiLogic.HARMLESS_LIFESPAN;               // :51-53
        }
        this.entityData.set(DATA_HARMLESS, (byte) (harmless ? 1 : 0));         // :55
    }

    public int lifeSpan() {
        return this.lifeSpan;
    }

    // ---- viewer (:38-44) ----

    public String getViewer() {
        return this.entityData.get(DATA_VIEWER);                               // :38-40
    }

    public void setViewer(final String player) {
        this.entityData.set(DATA_VIEWER, String.valueOf(player));              // :42-44
    }

    // ---- behaviour ----

    /** {@code :58-60} — {@code func_70647_i() = 0.7F}. */
    @Override
    protected float getSoundVolume() {
        return MindSpiderAiLogic.SOUND_VOLUME;
    }

    /**
     * {@code :90-92} — {@code func_145773_az() = true}: the AI is switched off
     * unconditionally. 1.21.1 gates the goal engine the same way
     * ({@code Mob#isEffectiveAi() = super.isEffectiveAi() && !isNoAi()},
     * {@code Mob.java:1421}), so the inherited spider goals are installed (the
     * family's rule: the original's set, reconciled) and never run. The
     * {@code NoAi} <i>data flag</i> is deliberately NOT set — this class
     * overrides the hook, exactly as the original overrides the watcher read.
     */
    @Override
    public boolean isNoAi() {
        return true;
    }

    /**
     * {@code :62-65} — {@code func_70782_k()}: the parent's target hook
     * re-pointed at {@code world.getClosestPlayer(this, 12.0)}. 21.1.259's
     * accessor is {@code EntityGetter#getNearestPlayer(Entity, double)}
     * ({@code EntityGetter.java:102}) — the same nearest-player-in-range query. 1.21.1's
     * {@code Spider} has no such hook (targeting is goal-based) and the mob is
     * NoAi, so the original's override is inert; it ships as a named method so
     * the 12.0 figure is pinned and probed instead of dropped.
     */
    public Player closestPlayerTarget() {
        return this.level().getNearestPlayer(this, MindSpiderAiLogic.CLOSEST_PLAYER_RANGE);   // EntityGetter.java:102 (no getClosestPlayer in 21.1.259)
    }

    /** {@code :72-77} — the server-only lifespan fuse. */
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && MindSpiderAiLogic.expires(this.tickCount, this.lifeSpan)) {
            this.discard();                                                    // :75
        }
    }

    /** {@code :79-81} — a harmless spider cannot be crosshair-picked. */
    @Override
    public float getPickRadius() {
        return this.isHarmless()
                ? MindSpiderAiLogic.PICK_RADIUS_HARMLESS
                : MindSpiderAiLogic.PICK_RADIUS_NORMAL;
    }

    /** {@code :94-96} — {@code func_70041_e_() = false}. */
    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * {@code :98-102} — {@code func_70785_a(Entity, float)}
     * (attackEntityAsMob): the melee hit runs only for a non-harmless spider.
     * 1.21.1's seam is {@code Mob#doHurtTarget(Entity)} (no strafe argument) and
     * it is where the spider's bite (the "poison" of the card's scope line)
     * lives — the gate therefore gates the bite too.
     */
    @Override
    public boolean doHurtTarget(final Entity target) {
        if (!MindSpiderAiLogic.attacks(this.isHarmless())) {
            return false;                                                      // :99
        }
        return super.doHurtTarget(target);                                     // :100
    }

    /**
     * {@code :22-24} — {@code func_70693_a(EntityPlayer)}: a harmless spider
     * grants no experience. 1.21.1's entry point
     * ({@code LivingEntity#getExperienceReward(ServerLevel, Entity)}) is
     * {@code final}, so the gate rides {@code getBaseExperienceReward()} — the
     * Looting term 1.21.1 applies on top of the base yields 0 for a 0-xp mob,
     * which is the same net result the original's gate produces. Probed.
     */
    @Override
    protected int getBaseExperienceReward() {
        return MindSpiderAiLogic.experienceFor(this.isHarmless(), this.xpReward);
    }

    /**
     * {@code :83-88} — {@code getDropItemId} returns {@code Item.getItemById(0)}
     * (null) and {@code dropFewItems} has an empty body: the mind spider drops
     * nothing. 1.21.1 runs death drops through the loot table inside
     * {@code dropCustomDeathLoot}, so the faithful port is the empty override
     * (the family's seam — {@code EntityWisp}, {@code EntityBrainyZombie}).
     */
    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source,
            final boolean recentlyHit) {
        // :87-88 — the original's empty dropFewItems body
    }

    /** {@code :110-114} — the two NBT keys. */
    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(MindSpiderAiLogic.NBT_HARMLESS, (byte) (this.isHarmless() ? 1 : 0));   // :112
        tag.putString(MindSpiderAiLogic.NBT_VIEWER, this.getViewer());                     // :113
    }

    /**
     * {@code :104-108}. The original writes the <b>watcher</b> directly — it
     * does not call {@code setHarmless} — so a loaded harmless spider keeps
     * {@code lifeSpan = Integer.MAX_VALUE} and outlives the 1200-tick fuse that
     * arming harmless mode would have set. That original asymmetry is preserved
     * here (the flag is written without touching the fuse) and recorded.
     */
    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        final byte harmless = tag.getByte(MindSpiderAiLogic.NBT_HARMLESS);      // :106
        this.entityData.set(DATA_HARMLESS, harmless);
        this.entityData.set(DATA_VIEWER, tag.getString(MindSpiderAiLogic.NBT_VIEWER)); // :107
    }
}
