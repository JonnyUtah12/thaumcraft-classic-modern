package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistCleric;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistKnight;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCultists;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;

/**
 * {@code thaumcraft:cultistportal} — original
 * {@code thaumcraft.common.entities.monster.boss.EntityCultistPortal} (314 lines,
 * content-mapping {@code Thaumcraft:CultistPortal}, mod_index 30, line 328),
 * {@code extends EntityMob implements IBossDisplayData}. Registration
 * {@code ConfigEntities.java:117} — {@code (64, 20, false)}: tracking 64, update
 * interval 20, velocity NOT synced (the portal never moves).
 *
 * <p>The Crimson Portal is the summoner: a 500-health, 0-attack, knockback-immune
 * stationary pillar with NO goal at all ({@code :83} — its {@code func_70638_d}
 * is empty and the constructor registers none), that walks a 13-stage machine and
 * hands out Crimson Knights and Clerics, then a Praetor at stage 12. The stage
 * cadence, the scan radius and every roll are on {@link CultistPortalAiLogic}.</p>
 *
 * <p>Stripped: 1.21.1's {@code Monster} adds no goal in its constructor, so the
 * empty goal list is the shipped state and the probe asserts 0 goals on both
 * selectors. Seams (recorded, not faked): the banner/crate placements ({@code
 * :127-152}) name {@code blockWoodenDevice} meta 8, {@code blockLootCrate} and
 * {@code TileBanner} — unregistered blocks/tiles, so the placements are gated and
 * the RULE (tick window, meta roll, offsets, facings) ships pinned on the rule
 * class; the status-16 client pulse ({@code :291-299}) and the arc packets are the
 * FX lane's; the {@code IBossDisplayData} bar is the client seam the family ships
 * no bar for.</p>
 */
public class EntityCultistPortal extends EntityThaumcraftBoss {

    /** The registration figures — {@code ConfigEntities.java:117} (64/<b>20</b>). */
    public static final float WIDTH = CultistPortalAiLogic.WIDTH;
    public static final float HEIGHT = CultistPortalAiLogic.HEIGHT;
    public static final int TRACKING_RANGE = CultistPortalAiLogic.TRACKING_RANGE;
    public static final int UPDATE_INTERVAL = CultistPortalAiLogic.UPDATE_INTERVAL;

    /** {@code :32} — the stage. */
    private int stage = CultistPortalAiLogic.STAGE_INITIAL;
    /** {@code :33} — the stage counter. */
    private int stageCounter = CultistPortalAiLogic.COUNTER_INITIAL;
    /** {@code :34} — {@code public int pulse} (the client's materialisation pulse). */
    public int pulse;

    public EntityCultistPortal(final EntityType<? extends EntityCultistPortal> type, final Level level) {
        super(type, level);
        this.setSize(CultistPortalAiLogic.WIDTH, CultistPortalAiLogic.HEIGHT);          // :40
        this.xpReward = CultistPortalAiLogic.EXPERIENCE;                                // :39
        this.setNoGravity(true);                                                        // :80 (it never moves)
    }

    /** {@code :63-65} — 500 health, 0 attack, knockback resistance 1.0, over the boss pair. */
    public static AttributeSupplier.Builder createAttributes() {
        return ThaumcraftBossAiLogic.onMonsterAttributes(Monster.createMonsterAttributes())
                .add(Attributes.MAX_HEALTH, CultistPortalAiLogic.MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, CultistPortalAiLogic.ATTACK_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, CultistPortalAiLogic.KNOCKBACK_RESISTANCE);
    }

    public int getStage() {
        return this.stage;
    }

    public int getStageCounter() {
        return this.stageCounter;
    }

    // ------------------------------------------------------------------ no goals (:83)

    @Override
    protected void registerGoals() {
        // :83 — the original's updateActionRiding is EMPTY and its constructor registers
        // no goal: the portal is a stationary summoner. 1.21.1's Monster adds none either,
        // so the shipped goal list is empty on BOTH selectors (probed).
    }

    // ------------------------------------------------------------------ stage machine (:99-211)

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            this.stagePass();                                                           // :99-211 (server half)
        }
        super.aiStep();
        if (this.pulse > 0) {                                                           // :135
            this.pulse--;
        }
        contactDamage();                                                                // :257-261
    }

    private void stagePass() {
        if (this.stageCounter <= CultistPortalAiLogic.COUNTER_MIN) {                    // :101
            if (this.level().getNearbyPlayer(this, CultistPortalAiLogic.NO_PLAYER_RANGE) == null) {
                this.stageCounter = CultistPortalAiLogic.noPlayerCounter(               // :103
                        this.random.nextInt(CultistPortalAiLogic.NO_PLAYER_RANDOM));
                return;
            }
            pulseClient();                                                              // :104-105 (status 16 → FX seam)
            playSound(ModSounds.MONOLITH.get(), CultistPortalAiLogic.SOUND_VOLUME, 1.0F); // :104, :263
            final int timing = CultistPortalAiLogic.timingFor(cultistCount());          // :110, :213-218
            if (this.stage <= 4) {                                                      // :106-108
                this.stageCounter = CultistPortalAiLogic.earlyStageCounter(this.stage,
                        this.random.nextInt(Math.max(1, 10 - this.stage)));
                spawnMinions();                                                         // :109
            } else if (this.stage < CultistPortalAiLogic.STAGE_BOSS) {                  // :110-113
                this.stageCounter = CultistPortalAiLogic.midStageCounter(timing,
                        this.random.nextInt(Math.max(1, 5 + timing / 3)));
                spawnMinions();
            } else {                                                                    // :115-117
                this.stageCounter = CultistPortalAiLogic.bossStageCounter(timing,
                        this.random.nextInt(50));
                spawnBoss();                                                            // :118
            }
            this.stage++;                                                               // :119
            return;
        }
        this.stageCounter--;                                                            // :121
        if (CultistPortalAiLogic.isBannerTick(this.stageCounter, this.stage)) {         // :127-135
            placeBanners();                                                             // gated seam
        }
        if (CultistPortalAiLogic.isCrateTick(this.stageCounter, this.stage)) {          // :137-152
            placeCrate();                                                               // gated seam
        }
        if (CultistPortalAiLogic.healsForStage(this.stage)) {                           // :122-125
            heal(CultistPortalAiLogic.HEAL_PER_TICK);
        }
    }

    /** {@code :291-299} — the client's status-16 pulse; the FX lane owns the packet. */
    private void pulseClient() {
        this.pulse = CultistPortalAiLogic.PULSE_TICKS;                                  // :294
    }

    /** {@code :213-218} — the cultist scan the cadence scales from: radius 32.0. */
    public int cultistCount() {
        final AABB box = this.getBoundingBox().inflate(CultistPortalAiLogic.SCAN_RADIUS); // :215
        return this.level().getEntitiesOfClass(EntityCultist.class, box).size();        // :216-217
    }

    // ------------------------------------------------------------------ summoning (:220-255)

    /**
     * {@code :220-241} — the minion pass: knight when the roll clears 0.33, cleric
     * otherwise, at the portal's own cell with the ±spread offsets and the 0.25 lift;
     * the minion's home is the portal cell with radius 32; the portal hurts itself
     * above stage 12.
     */
    public void spawnMinions() {
        final boolean knight = CultistPortalAiLogic.spawnsKnight(this.random.nextDouble()); // :222
        final double x = this.getX() + this.random.nextInt(8) - this.random.nextInt(8);
        final double y = this.getY() + CultistPortalAiLogic.MINION_Y_OFFSET;            // :228
        final double z = this.getZ() + this.random.nextInt(8) - this.random.nextInt(8);
        final EntityCultist minion = knight
                ? new EntityCultistKnight(CatalogCultists.CULTIST_KNIGHT.get(), this.level())
                : new EntityCultistCleric(CatalogCultists.CULTIST_CLERIC.get(), this.level());
        minion.setPos(x, y, z);
        minion.setHomeDist(CultistPortalAiLogic.MINION_HOME_RADIUS);                    // :232
        minion.restrictTo(this.blockPosition(), CultistPortalAiLogic.MINION_HOME_RADIUS);
        this.level().addFreshEntity(minion);                                            // :233
        minion.playLivingSound();                                                       // :234
        this.playSound(ModSounds.WANDFAIL.get(), 1.0F, 1.0F);                           // :236-238
        final int self = CultistPortalAiLogic.selfDamage(this.stage, this.random.nextInt(5));
        if (self > 0) {                                                                 // :239-241
            hurt(this.damageSources().generic(), self);
        }
    }

    /** {@code :243-255} — the boss stage: one Praetor (onInitialSpawn → home → sound → add). */
    public void spawnBoss() {
        final EntityCultistLeader leader =
                new EntityCultistLeader(CatalogBosses.CULTIST_LEADER.get(), this.level()); // :244
        leader.setPos(this.getX(), this.getY() + CultistPortalAiLogic.MINION_Y_OFFSET, this.getZ());
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            leader.finalizeSpawn(serverLevel,
                    serverLevel.getCurrentDifficultyAt(this.blockPosition()),
                    net.minecraft.world.entity.MobSpawnType.TRAILS, null);              // :245 (onInitialSpawn)
        }
        leader.restrictTo(this.blockPosition(), CultistPortalAiLogic.MINION_HOME_RADIUS); // :247
        leader.playLivingSound();                                                       // :249
        this.level().addFreshEntity(leader);                                            // :251
        this.playSound(ModSounds.WANDFAIL.get(), 1.0F, 1.0F);                           // :252-254
    }

    // ------------------------------------------------------------------ banner / crate (gated seams)

    /** {@code :127-135} — the four banners at ±6 with facings 8/0/12/4: unregistered block + tile. */
    private void placeBanners() {
        // blockWoodenDevice meta 8 and TileBanner.setFacing are other cards' content; the
        // tick window, the offsets and the facing list ship pinned on CultistPortalAiLogic.
    }

    /** {@code :137-152} — the crate: unregistered {@code blockLootCrate}; the meta roll ships pinned. */
    private void placeCrate() {
        // The meta roll (2 / 1 / 0 at 0.05 / 0.2) and the ±rand(5) offsets are on the rule class.
    }

    // ------------------------------------------------------------------ contact (:257-261)

    private void contactDamage() {
        if (this.level().isClientSide) {
            return;
        }
        for (final Player player : this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(2.0))) {
            if (player.distanceToSqr(this) < CultistPortalAiLogic.CONTACT_DISTANCE_SQ) { // :258
                player.hurt(this.damageSources().magic(), CultistPortalAiLogic.CONTACT_DAMAGE); // :259
                this.playSound(ModSounds.ZAP.get(), 1.0F, 1.0F);                        // :260
            }
        }
    }

    // ------------------------------------------------------------------ flags

    /** {@code :38} — immune to fire. */
    @Override
    public boolean isFireImmune() {
        return CultistPortalAiLogic.FIRE_IMMUNE;
    }

    /** {@code :76} — the portal is never pushed. */
    @Override
    public boolean isPushable() {
        return CultistPortalAiLogic.PUSHABLE;
    }

    /** {@code :301-305} — the portal takes no potion effect at all. */
    @Override
    public boolean addEffect(final MobEffectInstance effect) {
        return false;                                                                   // :301-305
    }

    /** {@code :267} — the kill award is 540 XP, not the 30 value. */
    @Override
    public int getExperiencePoints(final Player player) {
        return CultistPortalAiLogic.EXPERIENCE_POINTS;
    }

    /** {@code :263} — the sound volume figure. */
    @Override
    protected float getSoundVolume() {
        return CultistPortalAiLogic.SOUND_VOLUME;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MONOLITH.get();                                                // :271-273
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return ModSounds.ZAP.get();                                                     // :275-277
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SHOCK.get();                                                   // :279-281
    }

    /** {@code :307-313} — death: the explosion, no block destruction, no fire. */
    @Override
    public void die(final DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY() + 1.5, this.getZ(),
                    CultistPortalAiLogic.DEATH_EXPLOSION,
                    net.minecraft.world.level.Explosion.DestructionType.NONE);          // :307-313
        }
    }

    // ------------------------------------------------------------------ save data

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("stage", this.stage);                                                // :51
        tag.putInt("stagecounter", this.stageCounter);                                  // :52
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.stage = tag.getInt("stage");                                               // :55-56
        this.stageCounter = tag.getInt("stagecounter");                                 // :57-58
    }

    /** The client colour figure, for probes ({@code :91}). */
    public static int clientColour() {
        return CultistPortalAiLogic.COLOUR;
    }

    /** {@code :95} — the renderer's max-height figure. */
    public static float maxHeight() {
        return CultistPortalAiLogic.MAX_HEIGHT;
    }
}
