package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * The elemental wisp — {@code thaumcraft:wisp}
 * (content-mapping.csv {@code Thaumcraft:Wisp}, mod_index 21).
 *
 * <p>Original: {@code thaumcraft.common.entities.monster.EntityWisp}, an
 * {@code EntityFlying implements IMob} whose element is a primal or compound
 * <em>aspect</em> carried in <code>field_70180_af</code> (synched-data index
 * 22) and written to NBT under {@code "Type"}. It installs <b>no</b> goal and
 * <b>no</b> navigation class: in 1.7.10 the whole AI — waypoint flight, the
 * 1000-in-1000 target search against players, the 20-tick zap cadence and both
 * damage rolls — runs by hand inside {@code func_70071_h_}. That body is
 * reproduced here in {@link #customServerAiStep()} (1.21.1's server-only AI
 * hook) and every number and branch it uses lives in {@link WispAiLogic}, so
 * the original is pinned by unit tests rather than by a running world.</p>
 *
 * <h2>Faithfulness notes</h2>
 * <ul>
 *   <li><b>Stale steering deltas are preserved.</b> The original computes the
 *       waypoint deltas, <em>then</em> re-picks the waypoint when it is out of
 *       range, and keeps steering toward the old deltas for the rest of that
 *       tick. That quirk is reproduced deliberately (see
 *       {@link #customServerAiStep()}).</li>
 *   <li><b>No goal is registered.</b> {@code registerGoals()} is overridden and
 *       left empty on purpose: adding a {@code FloatGoal} or a wander goal would
 *       change the original's motion. This is asserted by the game probe.</li>
 *   <li><b>Deferred to their own rows:</b> the wisp-essence drop
 *       ({@code ItemWispEssence} + {@code AspectList}, a TC-M6-04 item row), the
 *       zapping FX packet (TC-M7-02/05), the biome spawn lists that populate
 *       wisps in the world (worldgen/spawn row), and the 1.7.10
 *       {@code isValidLightLevel}'s temporary {@code field_73008_k} write (a
 *       thunder flag that has no 1.21.1 equivalent).</li>
 * </ul>
 */
public class EntityWisp extends FlyingMob implements Enemy {

    /** Synched-data index 22 in the original, an aspect tag string. */
    private static final EntityDataAccessor<String> DATA_TYPE =
            SynchedEntityData.defineId(EntityWisp.class, EntityDataSerializers.STRING);

    public int courseChangeCooldown;
    public double waypointX;
    public double waypointY;
    public double waypointZ;
    private LivingEntity targetedEntity;
    private int aggroCooldown;
    public int prevAttackCounter;
    public int attackCounter;

    public EntityWisp(final EntityType<? extends EntityWisp> type, final Level level) {
        super(type, level);
        this.waypointX = getX();
        this.waypointY = getY();
        this.waypointZ = getZ();
    }

    /** The original {@code func_110147_ax}: health 22.0 and attack damage 3.0;
     * the rest are the platform's flying-mob substrate. */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, WispAiLogic.MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, WispAiLogic.ATTACK_DAMAGE)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, WispAiLogic.AGGRO_RADIUS);
    }

    /** The original registers no goal at all — the AI is hand-rolled (see the
     * class javadoc). Left empty on purpose. */
    @Override
    protected void registerGoals() {
        // deliberately empty
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, "");
    }

    // ---------------------------------------------------------------- aspect

    /** Original {@code getType()} — renamed because 1.21.1's
     * {@code Entity#getType()} returns the registry type. */
    public String getWispType() {
        return getEntityData().get(DATA_TYPE);
    }

    /** Original {@code setType(String)}. */
    public void setWispType(final String tag) {
        getEntityData().set(DATA_TYPE, tag);
    }

    /** Resolves an aspect by name, ignoring case, so the eerie pool is pinned
     * by the original's <em>set</em> (DARKNESS..DEATH) without depending on the
     * tag casing of this port's aspect registry. */
    private static Aspect aspectByName(final String name) {
        for (final Aspect aspect : Aspect.values()) {
            if (aspect.getTag().equalsIgnoreCase(name)) {
                return aspect;
            }
        }
        return null;
    }

    private static String[] tagsOf(final List<Aspect> aspects) {
        final List<String> tags = new ArrayList<>(aspects.size());
        for (final Aspect aspect : aspects) {
            tags.add(aspect.getTag());
        }
        return tags.toArray(new String[0]);
    }

    /** The original's type pick, run once on the first server tick while the
     * type is still unset ({@code Aspect.getAspect(getType()) == null}). */
    private void pickInitialType() {
        final boolean eerie = level().getBiome(blockPosition()).unwrapKey()
                .map(key -> key.location().getPath().contains("eerie")
                        || key.location().getPath().contains("taint"))
                .orElse(false);
        final String tag = WispAiLogic.pickAspectTag(
                eerie,
                random.nextInt(WispAiLogic.EERIE_ASPECTS.length),
                random.nextInt(10),
                random.nextInt(Math.max(1, Aspect.getPrimalAspects().size())),
                random.nextInt(Math.max(1, Aspect.getCompoundAspects().size())),
                tagsOf(Aspect.getPrimalAspects()),
                tagsOf(Aspect.getCompoundAspects()));
        setWispType(tag);
    }

    // ------------------------------------------------------------------ tick

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        // 1. element pick (original: !world.isRemote && Aspect.getAspect(getType()) == null)
        if (Aspect.getAspect(getWispType()) == null) {
            pickInitialType();
        }

        // 2. the original discards itself on peaceful (func_70106_y() on difficulty 0)
        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        prevAttackCounter = attackCounter;

        // 3. waypoint upkeep. NOTE the original order: the deltas are computed
        //    first and the re-pick below does NOT refresh them, so this tick
        //    still steers toward the waypoint that was just abandoned.
        final double dx = waypointX - getX();
        final double dy = waypointY - getY();
        final double dz = waypointZ - getZ();
        final double distSq = dx * dx + dy * dy + dz * dz;

        if (!WispAiLogic.waypointInRange(dx, dy, dz)) {
            waypointX = getX() + WispAiLogic.waypointOffset(random.nextFloat() * 2.0F - 1.0F);
            waypointY = getY() + WispAiLogic.waypointOffset(random.nextFloat() * 2.0F - 1.0F);
            waypointZ = getZ() + WispAiLogic.waypointOffset(random.nextFloat() * 2.0F - 1.0F);
        }

        if (courseChangeCooldown-- <= 0) {
            courseChangeCooldown += WispAiLogic.nextCourseChangeCooldown(
                    random.nextInt(WispAiLogic.COURSE_CHANGE_BOUND));
            final double distance = Math.sqrt(distSq);
            if (distance > 0.0 && isCourseTraversable(waypointX, waypointY, waypointZ, distance)) {
                setDeltaMovement(getDeltaMovement().add(
                        WispAiLogic.steerComponent(dx, distance),
                        WispAiLogic.steerComponent(dy, distance),
                        WispAiLogic.steerComponent(dz, distance)));
            } else {
                waypointX = getX();
                waypointY = getY();
                waypointZ = getZ();
            }
        }

        // 4. target upkeep (original: roll 1000, decrement-on-short-circuit)
        if (targetedEntity != null && !targetedEntity.isAlive()) {
            targetedEntity = null;
        }
        final WispAiLogic.Aggro aggro = WispAiLogic.aggroTick(
                aggroCooldown, random.nextInt(WispAiLogic.AGGRO_ROLL_BOUND), targetedEntity != null);
        aggroCooldown = aggro.cooldown();
        if (aggro.attempt()) {
            final Player player = level().getNearestPlayer(this, WispAiLogic.AGGRO_RADIUS);
            if (player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()) {
                targetedEntity = player;
                aggroCooldown = WispAiLogic.AGGRO_COOLDOWN;
            }
        }

        // 5. zap
        if (targetedEntity != null
                && targetedEntity.distanceToSqr(this) < WispAiLogic.ATTACK_RANGE * WispAiLogic.ATTACK_RANGE) {
            final double toX = targetedEntity.getX() - getX();
            final double toY = targetedEntity.getBoundingBox().minY + targetedEntity.getBbHeight() / 2.0D
                    - (getY() + getBbHeight() / 2.0D);
            final double toZ = targetedEntity.getZ() - getZ();
            final float yaw = -((float) Math.atan2(toX, toZ)) * 180.0F / (float) Math.PI;
            setYRot(yaw);
            setYHeadRot(yaw);
            if (hasLineOfSight(targetedEntity)) {
                attackCounter++;
                if (WispAiLogic.atZapCadence(attackCounter)) {
                    playSound(ModSounds.ZAP.get(), 1.0F, 1.1F);
                    final double dealt = WispAiLogic.attackDamage(
                            WispAiLogic.targetIsStationary(targetedEntity.getDeltaMovement().x,
                                    targetedEntity.getDeltaMovement().y,
                                    targetedEntity.getDeltaMovement().z),
                            random.nextFloat(),
                            getAttributeValue(Attributes.ATTACK_DAMAGE));
                    if (dealt != WispAiLogic.NO_HIT) {
                        targetedEntity.hurt(damageSources().mobAttack(this), (float) dealt);
                    }
                    attackCounter = WispAiLogic.nextAttackCounterAfterZap(
                            random.nextInt(WispAiLogic.ATTACK_COUNTER_RESET_BOUND));
                }
            } else if (attackCounter > 0) {
                attackCounter--;
            }
        } else {
            final float yaw = -((float) Math.atan2(getDeltaMovement().x, getDeltaMovement().z))
                    * 180.0F / (float) Math.PI;
            setYRot(yaw);
            setYHeadRot(yaw);
            if (attackCounter > 0) {
                attackCounter--;
            }
        }
    }

    /** {@code isCourseTraversable(double,double,double,double)}, Level-free rule
     * applied to the world: walk the body's box along the ray to the waypoint,
     * reject a liquid waypoint, and require a non-air block in the 11-sample
     * column under it. */
    private boolean isCourseTraversable(final double tx, final double ty, final double tz,
            final double distance) {
        final double sx = (tx - getX()) / distance;
        final double sy = (ty - getY()) / distance;
        final double sz = (tz - getZ()) / distance;

        int collisions = 0;
        AABB box = getBoundingBox();
        for (int i = 1; i < (int) distance; i++) {
            box = box.move(sx, sy, sz);
            if (!level().noCollision(this, box)) {
                collisions = 1;
                break;
            }
        }

        final BlockPos target = BlockPos.containing(tx, ty, tz);
        final BlockState state = level().getBlockState(target);
        int firstSolidBelow = -1;
        for (int a = 0; a < WispAiLogic.COURSE_DROP_SAMPLES; a++) {
            if (!level().getBlockState(target.below(a)).isAir()) {
                firstSolidBelow = a;
                break;
            }
        }
        return WispAiLogic.isCourseTraversable(collisions,
                !state.getFluidState().isEmpty(), firstSolidBelow);
    }

    // ------------------------------------------------------------- behaviour

    @Override
    public boolean isPushable() {
        // original func_70041_e_() -> false
        return false;
    }

    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        // original func_70097_a: both the direct attacker and the source entity
        // retarget the wisp and re-arm a 200-tick aggro cooldown.
        if (source.getEntity() instanceof LivingEntity living) {
            targetedEntity = living;
            aggroCooldown = WispAiLogic.HURT_AGGRO_COOLDOWN;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean checkSpawnRules(final LevelAccessor level, final MobSpawnType spawnType) {
        // original func_70601_bi() + isValidLightLevel
        final List<EntityWisp> nearby = level().getEntitiesOfClass(EntityWisp.class,
                getBoundingBox().inflate(WispAiLogic.SPAWN_SCAN_RADIUS, WispAiLogic.SPAWN_SCAN_RADIUS,
                        WispAiLogic.SPAWN_SCAN_RADIUS));
        final boolean light = WispAiLogic.isValidLightLevel(
                level().getBrightness(LightLayer.SKY, blockPosition()),
                random.nextInt(WispAiLogic.SKY_LIGHT_ROLL_BOUND),
                level().getBrightness(LightLayer.BLOCK, blockPosition()),
                random.nextInt(WispAiLogic.LIGHT_LEVEL_ROLL_BOUND));
        return WispAiLogic.canSpawn(nearby.size(), difficultyId(level), light,
                super.checkSpawnRules(level, spawnType));
    }

    private static int difficultyId(final LevelAccessor level) {
        return level.getDifficulty() == Difficulty.PEACEFUL ? 0 : level.getDifficulty().getId();
    }

    @Override
    protected float getSoundVolume() {
        return WispAiLogic.SOUND_VOLUME; // original func_70599_aP()
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WISPLIVE.get(); // original func_70639_aQ()
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH; // original "random.fizz"
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WISPDEAD.get(); // original func_70673_aS()
    }

    // ------------------------------------------------------------------ data

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Type", getWispType());
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setWispType(tag.getString("Type"));
    }

    /** Exposed for the game probe: the aspect tag the entity will resolve, or
     * null when the stored tag is not a registered aspect. */
    public Aspect resolvedAspect() {
        return Aspect.getAspect(getWispType());
    }

    /** Exposed for the game probe: the 1.21.1 wrapper of the original's
     * {@code isValidLightLevel} branch, with explicit rolls. */
    public static boolean validLightLevel(final int skyLight, final int skyRoll,
            final int blockLight, final int lightRoll) {
        return WispAiLogic.isValidLightLevel(skyLight, skyRoll, blockLight, lightRoll);
    }

    /** Exposed for the game probe: the spawn gate with explicit inputs. */
    public static boolean canWispSpawn(final int nearbySameType, final int difficultyId,
            final boolean validLightLevel, final boolean superCheck) {
        return WispAiLogic.canSpawn(nearbySameType, difficultyId, validLightLevel, superCheck);
    }

    /** Exposed for the game probe: the 1.21.1 spawn predicate shape, so the
     * spawn lists card can register it without new logic. */
    public static boolean checkWispSpawnRules(final EntityType<EntityWisp> type,
            final LevelAccessor level, final MobSpawnType spawnType, final BlockPos pos,
            final RandomSource random) {
        final List<EntityWisp> nearby = level.getEntitiesOfClass(EntityWisp.class,
                new AABB(pos).inflate(WispAiLogic.SPAWN_SCAN_RADIUS));
        final boolean light = WispAiLogic.isValidLightLevel(
                level.getBrightness(LightLayer.SKY, pos), random.nextInt(WispAiLogic.SKY_LIGHT_ROLL_BOUND),
                level.getBrightness(LightLayer.BLOCK, pos), random.nextInt(WispAiLogic.LIGHT_LEVEL_ROLL_BOUND));
        final int difficulty = level.getDifficulty() == Difficulty.PEACEFUL ? 0
                : level.getDifficulty().getId();
        return WispAiLogic.canSpawn(nearby.size(), difficulty, light, true);
    }
}
