package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintacle;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintacleAiLogic;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * The Giant Taintacle (content-mapping.csv {@code thaumcraft:taintaclegiant},
 * row 345, original class {@code EntityTaintacleGiant}).
 *
 * <p>Port of {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityTaintacleGiant.java}
 * (131 lines). The original is a 24-line subclass of {@code EntityTaintacle} that
 * re-skins the tendril as a 1.1×6.0 boss with 125 health / 9 attack / 20 xp, adds a
 * data-watcher "anger" counter, an enrage on any hit over 35 damage, a 1-in-30-tick
 * 1-heart regen, an anger particle on the client, a champion roll on first spawn, a
 * never-spawn gate, a never-drown gate, a Looting-off gate and an eldritch-object
 * death drop guarded by "no other giant within 48 blocks". Every arithmetic rule and
 * constant lives in {@link TaintacleGiantAiLogic}, cited per decompiled line; this
 * class only walks state through it.</p>
 *
 * <p>1.7.10 → 1.21.1 hook map (the family's map, from
 * {@link EntityTaintacle}, plus the boss-specific rows):</p>
 * <ul>
 *   <li>{@code func_70105_a(w,h)} (:24) → {@link #getDefaultDimensions(Pose)};</li>
 *   <li>{@code func_110147_ax()} (:29-33) → {@link #createAttributes()};</li>
 *   <li>{@code func_110161_a(data)} (:35-38, {@code onInitialSpawn}) →
 *       {@link #finalizeSpawn(ServerLevelAccessor, DifficultyInstance, MobSpawnType, SpawnGroupData)};</li>
 *   <li>{@code func_70071_h_()} (:41-66, {@code onUpdate}) → {@link #tick()}, split
 *       into a server body (anger tick-down, boss bar, regen) and an extracted
 *       client body ({@link #angerParticles()}) so a game-test server can prove
 *       both halves;</li>
 *   <li>{@code func_70088_a()} (:68-70, {@code entityInit}: watcher 14 = Short) →
 *       {@link #defineSynchedData(SynchedEntityData.Builder)}; the 1.7.10 watcher
 *       also serialised into NBT for free, so the port saves it explicitly
 *       ({@link #addAdditionalSaveData(CompoundTag)});</li>
 *   <li>{@code func_70601_bi()} (:80-84, {@code getCanSpawnHere} false) →
 *       {@link #checkSpawnRules(LevelAccessor, MobSpawnType)};</li>
 *   <li>{@code func_70628_a(flag, i)} (:86-94, {@code dropFewItems}) →
 *       {@link #dropFromLootTable(DamageSource, boolean)}, the family's convention;
 *       the original does NOT call super, so the parent's goo/fungus drop is
 *       replaced, not added to;</li>
 *   <li>{@code func_70097_a(source, damage)} (:109-130) → {@link #hurt(DamageSource, float)};</li>
 *   <li>{@code func_70682_h(air)} (:104-106) → {@link #decreaseAirSupply(int)}
 *       (the 21.1.259 drown counter, {@code LivingEntity.java:584});</li>
 *   <li>{@code IBossDisplayData} (:21) → a {@link ServerBossEvent} fed from
 *       {@link #startSeenByPlayer(ServerPlayer)}/{@link #stopSeenByPlayer(ServerPlayer)}
 *       and re-progressed every tick — 21.1.259 has no {@code Boss} interface and no
 *       {@code net.minecraft.world.level.boss.BossBar} (verified: that package is
 *       absent from the 21.1.259 jar), so the vanilla shape is
 *       {@code WitherBoss.java:74-80, 352, 379-391}.</li>
 * </ul>
 *
 * <p>Deliberately NOT wired (pinned as rules in {@link TaintacleGiantAiLogic},
 * listed on this card's left_out_of_scope): the champion modifier application
 * (:36 → {@code EntityUtils.java:287-301}); {@code func_70648_aU()} = true
 * (:100-102), whose only 21.1.259 seam is {@code isInvulnerableTo(DamageSource)} —
 * wiring it would make the boss immune to all damage, which is NOT what 1.7.10 does
 * (1.7.10's no-arg {@code isInvulnerable()} gates a handful of non-damage systems);
 * and {@code func_70692_ba()} = false (:96-98), the Looting gate, which must NOT be
 * wired to 21.1.259's {@code LivingEntity#shouldDropLoot()} because that gates the
 * whole death-loot block ({@code LivingEntity.java:1465-1468}) and would delete the
 * code-driven eldritch drop this class ships.</p>
 */
public class EntityTaintacleGiant extends EntityTaintacle {

    /**
     * The anger counter — watcher 14, a Short ({@code EntityTaintacleGiant.java:70}).
     * 21.1.259 has no {@code EntityDataSerializers.SHORT}, so the accessor is an
     * INT and the Short narrowing stays at the set site
     * ({@link TaintacleGiantAiLogic#angerAsShort(int)}) — which is exactly where
     * the quirk lives (70000 becomes 4464, 65535 becomes -1), so the observable
     * behaviour is unchanged.
     */
    private static final EntityDataAccessor<Integer> DATA_ANGER =
            SynchedEntityData.defineId(EntityTaintacleGiant.class, EntityDataSerializers.INT);

    /**
     * {@code EntityTaintacleGiant.java:92} — {@code ConfigItems.itemEldritchObject}
     * (content-mapping.csv row 268, {@code thaumcraft:itemeldritchobject},
     * not_started). The drop is resolved at call time and skipped while the item
     * is unregistered, the same guarded-id pattern as
     * {@code TaintHooks.spawnGuarded} and {@code SmeltingBonuses.java:162-163}.
     */
    public static final ResourceLocation ELDRITCH_OBJECT_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "itemeldritchobject");

    /**
     * 21.1.259's boss-bar contract ({@code IBossDisplayData} at :21 has no
     * counterpart): name = the display name, which for an un-named giant resolves
     * the lang key {@code entity.thaumcraft.taintaclegiant}
     * ({@link TaintacleGiantAiLogic#BOSS_BAR_NAME_KEY}). The colour/overlay are
     * port-chosen: 1.7.10's boss bar has no colour, and the vanilla pink progress
     * bar is the closest read of it.
     */
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);

    public EntityTaintacleGiant(final EntityType<? extends EntityTaintacleGiant> type, final Level level) {
        super(type, level);
        // :25 — field_70728_aV = 20 (xpReward), on top of the family's 10.
        this.xpReward = TaintacleGiantAiLogic.XP;
    }

    /**
     * :29-33 — {@code super.func_110147_ax()} then MAX_HEALTH 125.0 and
     * ATTACK_DAMAGE 9.0. The super call registers maxHealth 50/attack 7
     * ({@code EntityTaintacle.java:58-62}) and the two lines overwrite them, so the
     * net attribute set is exactly the two values below (movement_speed keeps the
     * 0.3 default: the original never registers it and the tendril is rooted,
     * {@code EntityTaintacle.java:98-106}).
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, TaintacleGiantAiLogic.HEALTH)
                .add(Attributes.ATTACK_DAMAGE, TaintacleGiantAiLogic.ATTACK);
    }

    /** :24 — the 1.1×6.0 box; the eye height is inherited (the giant never overrides {@code func_70053_R}). */
    @Override
    protected EntityDimensions getDefaultDimensions(final Pose pose) {
        return EntityDimensions.fixed(TaintacleGiantAiLogic.WIDTH, TaintacleGiantAiLogic.HEIGHT)
                .withEyeHeight(TaintacleAiLogic.EYE_HEIGHT);
    }

    /** :68-70 — register the anger watcher (id 14, Short, 0). */
    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANGER, 0);
    }

    /** :73-75 — {@code getAnger()}. */
    public int getAnger() {
        return TaintacleGiantAiLogic.angerFromShort(this.getEntityData().get(DATA_ANGER).shortValue());
    }

    /** :77-79 — {@code setAnger(int)}, narrowed to the watcher's Short (kept verbatim). */
    public void setAnger(final int anger) {
        this.getEntityData().set(DATA_ANGER, (int) TaintacleGiantAiLogic.angerAsShort(anger));
    }

    /**
     * The 1.7.10 data watcher serialised into NBT for free; 1.21.1 does not persist
     * synched data, so the port writes it under {@link TaintacleGiantAiLogic#ANGER_NBT_KEY}.
     */
    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putShort(TaintacleGiantAiLogic.ANGER_NBT_KEY, TaintacleGiantAiLogic.angerAsShort(this.getAnger()));
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setAnger(tag.getShort(TaintacleGiantAiLogic.ANGER_NBT_KEY));
    }

    /** :80-84 — {@code getCanSpawnHere() = false}: spawn-egg, spawner, summon or a script only. */
    @Override
    public boolean checkSpawnRules(final LevelAccessor level, final MobSpawnType reason) {
        return TaintacleGiantAiLogic.NATURAL_SPAWN_ALLOWED;
    }

    /**
     * :35-38 — {@code onInitialSpawn} calls {@code EntityUtils.makeChampion(this, true)}.
     * Only the {@code persist} half lands here ({@code EntityUtils.java:303-305} →
     * {@code setPersistenceRequired()}): the giant never despawns. The champion
     * roll, the "tc.mobmod" champion attribute, the CHAMPION_HEALTH/CHAMPION_DAMAGE
     * modifiers, the 25-heart heal and the mod-name prefix belong to the
     * un-inventoried champion subsystem and are out of scope; the roll rule itself
     * is pinned in {@link TaintacleGiantAiLogic#championModIndex(int)} and
     * {@link TaintacleGiantAiLogic#CHAMPION_MOD_COUNT}.
     */
    @Override
    public SpawnGroupData finalizeSpawn(final ServerLevelAccessor level, final DifficultyInstance difficulty,
            final MobSpawnType reason, final SpawnGroupData spawnData) {
        final SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        if (TaintacleGiantAiLogic.CHAMPION_PERSIST) {
            this.setPersistenceRequired();
        }
        return data;
    }

    /**
     * :41-66 — {@code onUpdate}. The family's own server/client halves run inside
     * {@code super.tick()}; the anger tick-down is not side-gated in the original
     * (it runs on both sides and the watcher syncs), so it is not gated here.
     */
    @Override
    public void tick() {
        super.tick();
        // :43-45
        if (this.getAnger() > 0) {
            this.setAnger(TaintacleGiantAiLogic.angerAfterTick(this.getAnger()));
        }
        if (!this.level().isClientSide) {
            // IBossDisplayData (:21): the bar tracks the health fraction.
            this.bossEvent.setName(this.getDisplayName());
            this.bossEvent.setProgress(TaintacleGiantAiLogic.bossHealthProgress(
                    this.getHealth(), this.getMaxHealth()));
            // :63-65 — the server-only 1-heart regen.
            if (TaintacleGiantAiLogic.regenerates(this.tickCount)) {
                this.heal(TaintacleGiantAiLogic.REGEN_AMOUNT);
            }
            return;
        }
        angerParticles();
    }

    /**
     * :47-60 — the client half ({@code world.field_70170_p.field_72995_K}): one
     * "angryVillager" particle on a 1-in-15 roll while the anger is up, jittered
     * across the box width and riding the top of the 6-block box
     * ({@code field_70131_O} is {@code Entity.height}, not maxHealth — see
     * {@link TaintacleGiantAiLogic#PARTICLE_Y_HEIGHT_MULTIPLIER}). Extracted so a
     * game-test server, which is never client-side, can prove the server half
     * without this path.
     */
    private void angerParticles() {
        if (!TaintacleGiantAiLogic.rollsAngerParticle(
                this.random.nextInt(TaintacleGiantAiLogic.PARTICLE_ROLL_BOUND), this.getAnger())) {
            return;
        }
        final double d0 = TaintacleGiantAiLogic.particleVelocity(this.random.nextGaussian());
        final double d1 = TaintacleGiantAiLogic.particleVelocity(this.random.nextGaussian());
        final double d2 = TaintacleGiantAiLogic.particleVelocity(this.random.nextGaussian());
        final float width = this.getBbWidth();
        final double minY = this.getBoundingBox().minY;
        this.level().addParticle(ParticleTypes.ANGRY_VILLAGER,
                TaintacleGiantAiLogic.particleAxis(this.getX(), width, this.random.nextFloat()),
                TaintacleGiantAiLogic.particleY(minY, this.getBbHeight(), this.random.nextFloat()),
                TaintacleGiantAiLogic.particleAxis(this.getZ(), width, this.random.nextFloat()),
                d0, d1, d2);
    }

    /**
     * :109-130 — the enrage. Over 35 damage on the server with a calm anger, the
     * giant takes regeneration / strength / speed for 200 ticks at the damage-derived
     * amplifiers, sets its anger to 200, and tells the attacker in chat. The
     * damage of that hit is then capped to 35.
     */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (TaintacleGiantAiLogic.canEnrage(amount, this.getAnger(), this.level().isClientSide)) {
            // :112-118 — the original wraps the whole potion block in a
            // try/catch(Exception) and swallows it. Kept verbatim, including the
            // fact that a throw after the first effect leaves the later ones unset
            // and still sets no anger.
            try {
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                        TaintacleGiantAiLogic.ENRAGE_EFFECT_DURATION,
                        TaintacleGiantAiLogic.regenerationAmplifier(amount)));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                        TaintacleGiantAiLogic.ENRAGE_EFFECT_DURATION,
                        TaintacleGiantAiLogic.strengthAmplifier(amount)));
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                        TaintacleGiantAiLogic.ENRAGE_EFFECT_DURATION,
                        TaintacleGiantAiLogic.speedAmplifier(amount)));
                this.setAnger(TaintacleGiantAiLogic.ANGER_ON_ENRAGE);
            } catch (Exception ignored) {
                // :117-118 — verbatim: the original swallows it silently.
            }
            // :120-123 — "entity.thaumcraft.taintaclegiant <tc.boss.enrage>" to the
            // player who landed the hit. 1.7.10 resolved the lang string
            // server-side (StatCollector) and sent a literal; 1.21.1 sends a
            // translatable component, which is the same text with the client's
            // language.
            if (source.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component
                        .literal(this.getType().getDescriptionId() + " ")
                        .append(Component.translatable(TaintacleGiantAiLogic.ENRAGE_LANG_KEY)), false);
            }
        }
        return super.hurt(source, TaintacleGiantAiLogic.cappedDamage(amount, this.level().isClientSide));
    }

    /**
     * :86-94 — {@code dropFewItems}: with no other giant inside the 48-block cube,
     * drop the eldritch object (meta 3) at half the box height. The original calls
     * NO super, so the family's goo/fungus drop ({@code EntityTaintacle.java:299-307})
     * is replaced, not inherited.
     *
     * <p>The original's {@code EntityUtils.entityDropSpecialItem}
     * ({@code EntityUtils.java:260-279}) spawns a {@code ThaumcraftEntitySpecialItem}
     * with a 10-tick pickup delay and a 0.1 upward motion; the port has no
     * special-item entity, so it uses the family's {@code spawnAtLocation} drop seam
     * and keeps only the height ({@code getBbHeight() / 2}).</p>
     */
    @Override
    protected void dropFromLootTable(final DamageSource source, final boolean attackedRecently) {
        // EntityUtils.getEntitiesInRange excludes the caller by entity id
        // (EntityUtils.java:227); getEntitiesOfClass does not, so drop the self hit.
        final int others = this.level().getEntitiesOfClass(EntityTaintacleGiant.class,
                TaintacleGiantAiLogic.deathDropScanBox(this.getX(), this.getY(), this.getZ())).size() - 1;
        if (TaintacleGiantAiLogic.dropsEldritchObject(others)) {
            final Item eldritch = BuiltInRegistries.ITEM.get(ELDRITCH_OBJECT_ID);
            if (eldritch != null && eldritch != Items.AIR) {
                final ItemStack drop = new ItemStack(eldritch, 1);
                // The original's damage-value meta 3 travels on the port's meta
                // component (see ItemResource.stack).
                drop.set(ModDataComponents.ITEM_META.get(), TaintacleGiantAiLogic.DEATH_DROP_META);
                this.spawnAtLocation(drop, TaintacleGiantAiLogic.deathDropOffset(this.getBbHeight()));
            }
        }
        // :94 — no super call, on purpose (see the javadoc).
    }

    /**
     * :104-106 — {@code func_70682_h(int air)} returns the air unchanged, so the
     * drown counter never moves. The 21.1.259 seam is
     * {@code LivingEntity#decreaseAirSupply(int)} ({@code LivingEntity.java:584}).
     */
    @Override
    protected int decreaseAirSupply(final int currentAir) {
        return TaintacleGiantAiLogic.airSupplyAfterTick(currentAir);
    }

    /** The boss-bar handle, exposed for the game-test card. */
    public ServerBossEvent getBossEvent() {
        return this.bossEvent;
    }

    /** :21 — show the bar to every player tracking the boss. */
    @Override
    public void startSeenByPlayer(final ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    /** :21 — and drop it again when they stop tracking. */
    @Override
    public void stopSeenByPlayer(final ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }
}
