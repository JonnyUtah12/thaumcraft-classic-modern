package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.core.BlockPos;

import com.jonnyutah12.thaumcraftclassic.api.entities.IEldritchMob;

/**
 * The shared boss body of {@code thaumcraft.common.entities.monster.boss.EntityThaumcraftBoss}
 * (259 lines, {@code extends EntityMob implements IBossDisplayData}). Every rule, constant and
 * interval is on the Level-free {@link ThaumcraftBossAiLogic}, pinned by
 * {@code ThaumcraftBossAiLogicTest}; this class is the wiring.
 *
 * <p><b>This class is deliberately UNREGISTERED.</b> The original has no {@code registerModEntity}
 * line for it ({@code ConfigEntities.java} registers Leader id 29, Portal 30, Golem 31 and Warden 26
 * and never the base), and it has no {@code docs/content-mapping.csv} row, exactly like the family's
 * unregistered {@code entities/monster/EntityCultist} base. It exists so the four registered bosses
 * share one rage/aggro/home/spawn-in body.</p>
 *
 * <p><b>The boss health bar is a client seam.</b> 1.7.10's marker interface
 * {@code IBossDisplayData} ({@code common/api/IBossDisplayData.java}, 0 members) is the hook the
 * client proxy's {@code ClientProxyTickHandler} reads to draw the on-screen bar. 1.21.1 has no such
 * interface, and the port ships no boss-bar renderer, so nothing here fakes one: the
 * {@code minecraft:boss_event} family is NOT created, because the original never used a vanilla boss
 * event — it drew the bar client-side from the marker. Recorded as a deviation.</p>
 */
public abstract class EntityThaumcraftBoss extends Monster {

    /**
     * {@code :24} — {@code datawatcher(14, Short.MAX_VALUE)}: the rage timer. 21.1.259 has no
     * {@code EntityDataSerializers.SHORT} (javap on the serializer list: BYTE/INT/LONG/FLOAT/...),
     * so the payload ships as the INT it is and the original's watched index 14 is recorded as data,
     * not as a shipped allocation — the family's BD-101 rule for the cleric's watched byte.
     */
    private static final EntityDataAccessor<Integer> ANGER =
            SynchedEntityData.defineId(EntityThaumcraftBoss.class, EntityDataSerializers.INT);

    /**
     * {@code :26} — {@code HashMap<Integer, Integer> aggro}: raw damage per ATTACKER ENTITY ID,
     * not per profile. 1.21.1's {@code Mob#getLastHurtByMob} is a single reference, so the map is
     * the port's own field (the family's rule, same as {@code EntityEldritchCrab:83}).
     */
    protected final Map<Integer, Integer> aggro = new HashMap<>();

    /** {@code :27} — the spawn-in countdown; the subclasses set it (golem 100, warden 150). */
    protected int spawnTimer = 0;

    protected EntityThaumcraftBoss(final EntityType<? extends EntityThaumcraftBoss> type,
            final Level level) {
        super(type, level);
        this.xpReward = ThaumcraftBossAiLogic.EXPERIENCE;                               // :31
        this.setCanPickUpLoot(ThaumcraftBossAiLogic.CAN_PICK_UP_LOOT);                  // :176 func_70648_aU()=true
        // :191 func_98052_bS()=false is 1.7.10's Entity#isAIDisabled — the NoAi-flag answer.
        // 21.1.259 has no overridable isAIDisabled seam on LivingEntity (javap: the flag is read
        // from the NoAI nbt byte in the spawn path), and the port never sets the flag, so the
        // original's `false` is the platform default and no override is needed.
    }

    /** {@code :51-55} — the base's attribute pass on top of {@code Monster}'s. */
    protected static AttributeSupplier.Builder bossAttributes() {
        return ThaumcraftBossAiLogic.onMonsterAttributes(Monster.createMonsterAttributes());
    }

    /** {@code :57-60} — the datawatcher definition (watched index 14, initial 0). */
    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANGER, 0);
    }

    // ------------------------------------------------------------------ home

    /** {@code :34-40} — {@code HomeD}/{@code HomeX}/{@code HomeY}/{@code HomeZ} ints. */
    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeD")) {                                                    // :36
            this.restrictTo(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"),
                    tag.getInt("HomeZ")), tag.getInt("HomeD"));                        // :37
        }
    }

    /** {@code :42-49} — written only while {@code func_70145_a(64.0)} is true. */
    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.hasRestriction() && this.getRestrictRadius() > 0.0F) {                 // :43
            final BlockPos home = this.getRestrictCenter();
            tag.putInt("HomeD", (int) this.getRestrictRadius());
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
        }
    }

    /** {@code :72-75} — {@code func_110171_b(x, y, z, 24)} on first spawn. */
    @Override
    public SpawnGroupData finalizeSpawn(final ServerLevelAccessor level, final DifficultyInstance difficulty,
            final MobSpawnType reason, final SpawnGroupData groupData) {
        this.restrictTo(this.blockPosition(), ThaumcraftBossAiLogic.HOME_RADIUS);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    // ------------------------------------------------------------------ rage / aggro

    /** {@code :77-79} — {@code getAnger()} reads the watched short. */
    public int getAnger() {
        return this.entityData.get(ANGER).intValue();
    }

    /** {@code :81-83} — {@code setAnger(int)} writes it. */
    public void setAnger(final int anger) {
        this.entityData.set(ANGER, anger);
    }

    public int getSpawnTimer() {
        return this.spawnTimer;
    }

    /** {@code :63} / {@code :172-173} — the materialisation window. */
    public boolean isSpawningIn() {
        return this.spawnTimer > 0;
    }

    /**
     * The damage leg of {@code :222-255}, run from every boss subclass' {@code hurt}:
     * the aggro entry, the rage rule (200-tick haste {@code (int)(d/15)}, resistance and
     * strength {@code (int)(d/40)}, anger 200) and the unconditional 35.0F clamp. The
     * original's {@code tc.boss.enrage} chat broadcast ({@code :241-248}) has no 1.21.1
     * seam in this tree — recorded in the rows' known_differences.
     */
    protected float bossDamageLeg(final DamageSource source, final float amount) {
        final Entity attacker = source.getEntity();
        if (attacker != null) {                                                         // :222-232
            recordAggro(attacker.getId(), amount);
        }
        final ThaumcraftBossAiLogic.Rage rage =
                ThaumcraftBossAiLogic.rageFor(amount, getAnger());                      // :234-251
        if (rage.enrages() && !this.level().isClientSide) {                             // :235
            setAnger(ThaumcraftBossAiLogic.RAGE_POTION_TICKS);                          // :240
            addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DIG_SPEED,
                    ThaumcraftBossAiLogic.RAGE_POTION_TICKS, rage.hasteAmplifier()));   // :237
            addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                    ThaumcraftBossAiLogic.RAGE_POTION_TICKS, rage.resistanceAmplifier())); // :238
            addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_BOOST,
                    ThaumcraftBossAiLogic.RAGE_POTION_TICKS, rage.strengthAmplifier())); // :239 (Potion.field_76424_c = strength = 21.1.259 MobEffects.DAMAGE_BOOST)
        }
        return rage.damageAfterClamp();                                                 // :250
    }

    public void setSpawnTimer(final int ticks) {
        this.spawnTimer = ticks;
    }

    /** The damage bookkeeping of {@code :222-232} — the subclasses' hurt pass calls this. */
    protected void recordAggro(final int attackerId, final float damage) {
        this.aggro.put(attackerId, ThaumcraftBossAiLogic.aggroEntry(this.aggro, attackerId, damage));
    }

    /**
     * The 20-tick retarget + player-scaling pass of {@code :120-168}. The original's quirks
     * (the incumbent-id lookup at {@code :131}, {@code ld = ei} at {@code :134}) live in
     * {@link ThaumcraftBossAiLogic#scanAggro}; the modifier add/remove order here is the
     * original's: strip all ten, then re-add up to {@code Math.min(5, players - 1)}.
     */
    protected void runAggroPass() {
        final LivingEntity current = this.getTarget();
        if (current == null) {                                                          // :120
            return;
        }
        final int currentId = current.getId();
        final ThaumcraftBossAiLogic.AggroScan scan = ThaumcraftBossAiLogic.scanAggro(this.aggro,
                currentId, this.aggro.getOrDefault(currentId, 0).intValue(),
                // 1.7.10's server lookup answers null for a removed entity (:131-132's
                // liveness half); 21.1.259's Optional carries removed entities, so the
                // adapter answers null for them.
                id -> {
                    final Entity found = this.level().getEntity(id);
                    return found == null || found.isRemoved() ? null : found;
                },
                entity -> this.distanceToSqr(entity),                                   // :132
                entity -> entity instanceof net.minecraft.world.entity.player.Player);  // :135
        scan.dropIds().forEach(this.aggro::remove);                                     // :139
        if (scan.switched()) {                                                          // :147-149
            final Entity next = this.level().getEntity(scan.targetId());
            if (next instanceof LivingEntity living) {
                this.setTarget(living);
            }
        }
        scaleForPlayers(scan.players());                                                // :154-168
    }

    /** {@code :154-168} — five HP + five damage modifiers, and the current-health rescale. */
    protected void scaleForPlayers(final int players) {
        final AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        final AttributeInstance attackDamage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        final double oldMax = maxHealth.getValue();
        for (int a = 0; a < ThaumcraftBossAiLogic.HPBUFF.length; a++) {                 // :155-159
            attackDamage.removeModifier(ThaumcraftBossAiLogic.DMGBUFF[a].id());
            maxHealth.removeModifier(ThaumcraftBossAiLogic.HPBUFF[a].id());
        }
        final int buffs = ThaumcraftBossAiLogic.buffCount(players);                     // :161
        for (int a = 0; a < buffs; a++) {
            maxHealth.addPermanentModifier(ThaumcraftBossAiLogic.HPBUFF[a]);
            attackDamage.addPermanentModifier(ThaumcraftBossAiLogic.DMGBUFF[a]);
        }
        this.setHealth(ThaumcraftBossAiLogic.rescaledHealth(oldMax, maxHealth.getValue(),
                this.getHealth()));                                                     // :166-167
    }

    // ------------------------------------------------------------------ ticking

    /** {@code :85-170} — the tick body, server half here, client half extracted. */
    @Override
    public void aiStep() {
        super.aiStep();                                                                 // :90
        if (this.spawnTimer > 0) {                                                      // :91-93
            this.spawnTimer--;
        }
        if (this.getAnger() > 0) {                                                      // :95-97
            this.setAnger(this.getAnger() - 1);
        }
        if (this.level().isClientSide) {                                                // :99 field_72995_K
            spawnAngerPuffClient();
            return;
        }
        final LivingEntity toAttack = this.getTarget();                                 // :105
        if (toAttack != null
                && toAttack.distanceToSqr(this.getX(), this.getY(), this.getZ())
                        > ThaumcraftBossAiLogic.TARGET_DROP_DISTANCE_SQR) {             // :106-107
            this.setTarget(null);                                                       // :108
        }
        if (this.tickCount % ThaumcraftBossAiLogic.HEAL_PERIOD_TICKS == 0) {            // :116-118
            this.heal(ThaumcraftBossAiLogic.HEAL_AMOUNT);
        }
        if (this.getTarget() != null
                && this.tickCount % ThaumcraftBossAiLogic.AGGRO_PERIOD_TICKS == 0) {    // :120
            runAggroPass();
        }
    }

    /**
     * The client-only arm of {@code :99-115}: the anger smoke puff on a 1-in-15 roll. The
     * game-test server is never client-side, so this half is an extracted method the unit test
     * drives through {@link ThaumcraftBossAiLogic#angerParticleFires(int)}.
     */
    protected void spawnAngerPuffClient() {
        // The original runs a ParticleBox inside the world.isRemote arm; the port's particle
        // family is TC-M7-02-s' renderer lane, and the roll gate is pinned on the rule class.
    }

    /**
     * {@code :63-65} — "the goals tick only while the spawn-in timer is idle". 1.21.1's goal-tick
     * pass is {@code Mob#serverAiStep()}, which is {@code final} and has no seam, so the gate
     * lives in each goal's {@code canUse}: the family's own {@code ai/boss/AIAttackOnCollide}
     * asks {@link #goalsMayTick()} (the goal the boss family lands first, cited there). The
     * goals the cleric lane landed ({@code AILongRangeAttack}, {@code AICultistHurtByTarget})
     * and the vanilla goals the original also installs ({@code Swimming},
     * {@code MoveTowardsRestriction}, {@code Wander}, {@code WatchClosest}, {@code LookIdle},
     * the player-target scan) have no such hook and can still fire during the 100/150-tick
     * materialisation — recorded as a deviation on the row; a later family pass can thread
     * the gate through those classes without moving any figure.
     */
    public boolean goalsMayTick() {
        return this.spawnTimer == ThaumcraftBossAiLogic.SPAWN_TIMER_MIN;                // :63
    }

    /** {@code :67-69} — a dead target is dropped every step, timer or no timer. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        final LivingEntity target = this.getTarget();
        if (target != null && !target.isAlive()) {                                      // :67-69
            this.setTarget(null);
        }
    }

    // ------------------------------------------------------------------ hooks

    /** {@code :172-173} — invulnerable while the spawn-in timer runs. */
    @Override
    public boolean isInvulnerableTo(final DamageSource source) {
        return super.isInvulnerableTo(source)
                || ThaumcraftBossAiLogic.invulnerableForSpawnTimer(this.spawnTimer);
    }

    /** {@code :180} — {@code func_70104_M() = super && !isInvulnerable()} → {@code isPushable()}. */
    @Override
    public boolean isPushable() {
        return super.isPushable() && ThaumcraftBossAiLogic.pushableForVulnerable(
                !this.isInvulnerable());
    }

    /** {@code :205} — {@code func_70692_ba()} false: a boss drops no death loot. */
    @Override
    public boolean shouldDropLoot() {
        return ThaumcraftBossAiLogic.DAPS_LOOT;
    }

    /**
     * {@code :213-216} — the base's custom drops: eldritch object meta 3, then
     * {@code itemLootbag} meta 2 at 1.5F. Both items are unregistered content (BD-68
     * gate shape) and the {@code :205} false flag ({@link #shouldDropLoot()}) gates
     * 21.1.259's whole {@code Mob#dropAllDeathLoot} block, so this hook is unreachable
     * exactly as it was in 1.7.10 (the same flag gated the same block there) — a probe
     * measures that a killed boss leaves no items. The Leader overrides the hook with
     * the lootbag-only rule ({@code EntityCultistLeader:147-150}).
     */
    @Override
    protected void dropCustomDeathLoot(final net.minecraft.server.level.ServerLevel level,
            final DamageSource source, final boolean recentlyHit) {
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(
                ThaumcraftBossAiLogic.ELDRITCH_OBJECT)) {                               // :214
            final net.minecraft.world.item.ItemStack object = new net.minecraft.world.item.ItemStack(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                            ThaumcraftBossAiLogic.ELDRITCH_OBJECT));
            object.setDamageValue(ThaumcraftBossAiLogic.ELDRITCH_OBJECT_META);          // :214 meta 3
            this.spawnAtLocation(object, 0.0F);                                         // :214 func_146069_c: no roll
        }
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(
                ThaumcraftBossAiLogic.LOOTBAG)) {                                       // :215
            final net.minecraft.world.item.ItemStack bag = new net.minecraft.world.item.ItemStack(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                            ThaumcraftBossAiLogic.LOOTBAG));
            bag.setDamageValue(ThaumcraftBossAiLogic.LOOTBAG_META);                     // :215 meta 2
            this.spawnAtLocation(bag, ThaumcraftBossAiLogic.LOOTBAG_CHANCE);            // :215
        }
    }

    /**
     * {@code :188} — the base overrides 1.7.10's death-tick body ({@code func_70110_aj}) with an
     * EMPTY one, so a boss never runs vanilla's "animate the death for 20 ticks, then remove" pass.
     * 1.21.1 has no such hook: {@code LivingEntity#deathUpdate} does not exist in 21.1.259 — the
     * {@code deathTime} counter is inline in {@code LivingEntity#aiStep}. The port therefore ships
     * NO override and a boss follows vanilla's death pass; the subclasses' death handlers (the
     * Portal's explosion, the Warden's frenzy swap) are ported verbatim and run from {@code die}.
     * Recorded as a deviation, and a probe must {@code discard()} anything it kills.
     */

    /** {@code :220-220} — the aggro map is cleared with the entity's target reset. */
    public void clearAggro() {
        this.aggro.clear();
    }

    /** The base's damage map, read-only view for probes and the subclasses. */
    public Map<Integer, Integer> aggroMap() {
        return this.aggro;
    }

    /**
     * The eldritch-party rule of {@code :215-218} — {@code isEnemyEntity} is the predicate
     * 1.7.10's {@code AINearestAttackableTarget} / {@code AIHurtByTarget} consult, and the port
     * hands it to the goal adapters as a {@code TargetingConditions} filter.
     */
    public static boolean isEnemyEntity(final Entity target) {
        return target instanceof IEldritchMob;                                          // :215-218
    }

    /** {@code :199-203} — the base's default-equipment/enchant hooks are empty: 1.21.1 has no
     * such hooks ({@code Mob#populateDefaultEquipmentSlots} is not on the spawn path), so the
     * subclasses run their own equipment pass from {@code finalizeSpawn} — the family's BD-98 rule. */
}
