package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.api.damagesource.DamageSourceThaumcraft;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaint;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaintFibres;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTaint;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The taintacle (TC-M6-06-s-e, content-mapping.csv {@code thaumcraft:taintacle}).
 *
 * <p>Port of {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/EntityTaintacle.java}
 * (307 lines). Every number and branch is quoted from that file; the arithmetic
 * lives in {@link TaintacleAiLogic} so the JUnit suite can pin it without a world.</p>
 *
 * <p>1.7.10 → 1.21.1 hook map used here:</p>
 * <ul>
 *   <li>{@code func_70105_a(w,h)} + {@code func_70053_R()} (eye height) →
 *       {@link #getDimensions(Pose)} (1.21.1 carries the eye height inside
 *       {@link EntityDimensions});</li>
 *   <li>{@code func_70091_d(dx,dy,dz)} (moveEntity) → {@link #travel(Vec3)}: the
 *       original zeroes x/z and clamps upward motion to 0, i.e. the tendril is
 *       rooted in place ({@code EntityTaintacle.java:98-106});</li>
 *   <li>{@code func_70626_be()} (updateEntityActionState) →
 *       {@link #customServerAiStep()}; {@code func_70071_h_()} (onUpdate) →
 *       {@link #tick()} split into a server and a client half;</li>
 *   <li>{@code func_70782_k()} (findEntityTarget) → {@link #findTarget()}, called
 *       from the same place the original calls it (the onUpdate tail, :140-141);
 *       there is no target goal because the originals install none
 *       ({@link GoallessTaintMonster});</li>
 *   <li>{@code func_70652_k(entity)} (attackEntityAsMob) →
 *       {@link #doHurtTarget(Entity)}, keeping the original's own damage
 *       arithmetic and the {@code thaumcraft:tentacle} damage type;</li>
 *   <li>{@code func_70601_bi()} (getCanSpawnHere) →
 *       {@link #checkSpawnRules(LevelAccessor, MobSpawnType)};</li>
 *   <li>{@code func_70628_a(...)} (dropRareDrop) →
 *       {@link #dropFromLootTable(DamageSource, boolean)}: the port has no
 *       {@code loot_table/entities} rows, so the original's code-driven death drop
 *       is ported as code (see the note in {@code TaintacleAiLogic}).</li>
 * </ul>
 *
 * <p>Client-only visual: {@code Thaumcraft.proxy.tentacleAriseFX} (:136). The port
 * has no proxy FX seam, so the trigger rule ships pinned in
 * {@link TaintacleAiLogic#playsAriseEffect(int, float, boolean)} and the particle
 * itself is left to the client-FX card (see this card's left_out_of_scope).</p>
 */
public class EntityTaintacle extends GoallessTaintMonster {

    /**
     * The eldritch-biome branch of {@code spawnTentacles}
     * ({@code EntityTaintacle.java:213}, {@code :231-236}). The 1.7.10 eldritch
     * biome ({@code ThaumcraftWorldGenerator.biomeEldritchLands}) has no
     * content-mapping.csv biome row — only {@code config.Biomes.biome_eldritch}
     * (not_started) — so there is no id contract to code against yet and the
     * probe answers "not eldritch". The branch is written out, so it turns itself
     * on when the eldritch biome lands.
     */
    public static final String NO_ELDRITCH_BIOME_CONTRACT =
            "content-mapping.csv carries no biome row for the eldritch lands; "
                    + "Config.biomeEldritchID is config row thaumcraft:config.biomes.biome_eldritch (not_started)";

    /** {@code EntityTaintacle.java:26} — the client model's flail amplitude. */
    public float flailIntensity = 1.0F;

    /**
     * 1.7.10 {@code EntityLivingBase.field_70724_aR} ({@code attacks}): the melee
     * swing counter. ONE counter feeds both the melee gate (:153) and the tentacle
     * burst (:220), so a burst blocks melee and a melee swing blocks a burst.
     */
    private int attackTime;

    public EntityTaintacle(final EntityType<? extends GoallessTaintMonster> type, final Level level) {
        super(type, level);
        // :31 — xpReward = 10
        this.xpReward = TaintacleAiLogic.XP;
    }

    /** :58-62 — the original registers only maxHealth 50 and attackDamage 7. */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, TaintacleAiLogic.HEALTH)
                .add(Attributes.ATTACK_DAMAGE, TaintacleAiLogic.ATTACK);
        // movement_speed keeps 1.21.1's createMobAttributes default (0.3). The
        // original never registers it, and the tendril is rooted (travel clamp,
        // :98-106), so the value is inert.
    }

    /** :30 box + :54-56 eye height. */
    @Override
    protected EntityDimensions getDefaultDimensions(final Pose pose) {
        return EntityDimensions.fixed(TaintacleAiLogic.WIDTH, TaintacleAiLogic.HEIGHT)
                .withEyeHeight(TaintacleAiLogic.EYE_HEIGHT);
    }

    /** :68-70 — {@code canBePushed() = true} (the 1.21.1 seam is {@code isPushable}). */
    @Override
    public boolean isPushable() {
        return true;
    }

    /** :275-277 — ambient sound interval 200. */
    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    /** :279-281 — ambient "thaumcraft:roots". */
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ROOTS.get();
    }

    /** :291-293 — hurt "thaumcraft:tentacle". */
    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return ModSounds.TENTACLE.get();
    }

    /** :295-297 — death "thaumcraft:tentacle". */
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TENTACLE.get();
    }

    /**
     * 21.1.259: {@code Enchantments.X} is a {@code ResourceKey} and
     * {@code minecraft:enchantment} is a DYNAMIC (datapack) registry — it is
     * not in {@code BuiltInRegistries}, so the holder is resolved through the
     * level's {@code RegistryAccess}, the same accessor
     * {@code RegistrationGameTest} uses for its {@code kind=enchantment} rows.
     */
    static net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchHolder(
            final net.minecraft.core.RegistryAccess registryAccess,
            final net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        return registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(key);
    }

    /** :287-289 — volume = maxHealth / 8 (6.25 for a taintacle). */
    @Override
    protected float getSoundVolume() {
        return TaintacleAiLogic.soundVolume(this.getMaxHealth());
    }

    /**
     * :283-285 — pitch = 1.3 − maxHealth / 10. Kept verbatim: for a 50-health
     * taintacle that is −3.7 and the client clamps it; a taintlet (8) is 0.5 and
     * the giant (125) is −11.2. Pinning the formula is the point.
     */
    @Override
    public float getVoicePitch() {
        return TaintacleAiLogic.soundPitch(this.getMaxHealth());
    }

    /**
     * :98-106 — the movement integration hook: horizontal motion is thrown away
     * and any upward motion is clamped to 0. The tendril never walks and never
     * pops upward, only falls.
     */
    @Override
    public void travel(final Vec3 travelVector) {
        super.travel(new Vec3(0.0, Math.min(0.0, travelVector.y()), 0.0));
    }

    /** :34-51 — spawn gate (see {@link TaintacleAiLogic#canSpawnHere}). */
    @Override
    public boolean checkSpawnRules(final LevelAccessor level, final MobSpawnType reason) {
        if (!super.checkSpawnRules(level, reason)) {
            return false;
        }
        if (!(level instanceof Level world)) {
            // 1.21.1 hands spawn rules a LevelAccessor; the proximity scan needs
            // an entity query, which only a Level answers. Server spawning passes
            // a ServerLevel, so the real path always has one.
            return false;
        }
        final BlockPos here = this.blockPosition();
        // :42 scans the literal EntityTaintacle.class — subclasses (taintlet,
        // giant) count as neighbours too, exactly as in the original.
        final int neighbours = world.getEntitiesOfClass(EntityTaintacle.class,
                TaintacleAiLogic.spawnScanBox(this.getX(), this.getY(), this.getZ())).size();
        return TaintacleAiLogic.canSpawnHere(neighbours, standingOnTaint(world, here),
                TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(world, here)), true);
    }

    /**
     * :46-50 — taint fibres meta 0 (fibres) under the mob, or taint meta 1 (soil)
     * under the mob, and nothing else.
     */
    static boolean standingOnTaint(final Level level, final BlockPos pos) {
        final BlockState under = level.getBlockState(pos);
        return isFibresMeta(under, BlockTaintFibres.META_FIBRES)
                || isTaintMeta(under, BlockTaint.META_SOIL);
    }

    static boolean isFibresMeta(final BlockState state, final int meta) {
        return state.is(CatalogTaint.BLOCK_TAINT_FIBRES.get())
                && state.hasProperty(BlockTaintFibres.VARIANT)
                && state.getValue(BlockTaintFibres.VARIANT) == meta;
    }

    static boolean isTaintMeta(final BlockState state, final int meta) {
        return state.is(CatalogTaint.BLOCK_TAINT.get())
                && state.hasProperty(BlockTaint.VARIANT)
                && state.getValue(BlockTaint.VARIANT) == meta;
    }

    @Override
    public void tick() {
        super.tick();
        // :114-121 — server: one starve heart every 20 ticks while the biome under
        // the mob is not the taint biome.
        if (!this.level().isClientSide) {
            if (TaintacleAiLogic.starves(this.tickCount,
                    TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(this.level(), this.blockPosition())))) {
                this.hurt(this.level().damageSources().starve(), TaintacleAiLogic.STARVE_DAMAGE);
            }
            return;
        }
        // :123-137 — client only (world.field_72995_K): the flail ramp and the
        // arise effect. A game-test server is never client-side, so both halves
        // are split out and the rules are pinned in TaintacleAiLogic.
        final boolean hurtOrSwing = this.invulnerableTime > 0 || this.attackTime > 0;
        final LivingEntity target = this.getTarget();
        final boolean targetInReach = target != null
                && target.distanceTo(this) <= this.getMaxHealth();
        this.flailIntensity = TaintacleAiLogic.nextFlailIntensity(this.flailIntensity, this.tickCount,
                this.getMaxHealth(), hurtOrSwing, targetInReach);
        // :135-137 Thaumcraft.proxy.tentacleAriseFX(this) — no port FX seam.
    }

    /** :108-112 + :140-149 — the AI step the original hand-rolls. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.attackTime > 0) {
            this.attackTime--;
        }
        // :108-112 updateEntityActionState: face the target, 5 degrees a tick.
        if (this.getTarget() != null) {
            this.faceEntity(this.getTarget(), TaintacleAiLogic.FACE_SPEED);
        }
        // :140-149
        if (this.getTarget() == null) {
            this.setTarget(this.findTarget());
        } else if (this.getTarget().isAlive() && this.getAgitationState()) {
            final float dist = this.getTarget().distanceTo(this);
            if (this.canAttackTarget(this.getTarget()) && this.hasLineOfSight(this.getTarget())) {
                this.doAttack(this.getTarget(), dist);
            }
        } else {
            this.setTarget(null);
        }
    }

    /**
     * :144 — the original's {@code func_70685_l(entity)} ("can this entity be
     * attacked"): living, non-invulnerable, attackable type, plus line of sight.
     */
    protected boolean canAttackTarget(final LivingEntity target) {
        return super.canAttackTarget(target);
    }

    /** :152-164 — melee, else the far tentacle burst. */
    protected void doAttack(final Entity target, final float distance) {
        if (TaintacleAiLogic.canMelee(this.attackTime, distance, this.getMaxHealth(),
                verticalOverlap(target))) {
            this.attackTime = TaintacleAiLogic.ATTACK_COOLDOWN;      // :157
            this.doHurtTarget(target);                                // :158
            // :159 — playSound("thaumcraft:tentacle", volume, pitch)
            this.playSound(ModSounds.TENTACLE.get(), this.getSoundVolume(), this.getVoicePitch());
        } else if (TaintacleAiLogic.wantsTentacleBurst(this.attackTime, distance, this.getMaxHealth(),
                target.onGround(), this instanceof EntityTaintacleSmall)) {
            this.spawnTentacles(target);                              // :161
        }
    }

    /** :155-156 — the target's AABB must overlap the tendril's vertically. */
    private boolean verticalOverlap(final Entity target) {
        return target.getBoundingBox().maxY > this.getBoundingBox().minY
                && target.getBoundingBox().minY < this.getBoundingBox().maxY;
    }

    /** :72-96 — the target scan, box = maxHealth * 6 / 3 / 6, taint mobs ignored. */
    protected LivingEntity findTarget() {
        final AABB box = TaintacleAiLogic.targetScanBox(this.getX(), this.getY(), this.getZ(),
                this.getMaxHealth());
        final var found = this.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this && e.isAlive() && TaintacleAiLogic.validTarget(e instanceof com.jonnyutah12.thaumcraftclassic.api.entities.ITaintedMob));
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (final LivingEntity candidate : found) {
            final double d = candidate.distanceToSqr(this);
            if (d < bestDistance) {
                bestDistance = d;
                best = candidate;
            }
        }
        return best;
    }

    /** :251-253 — the agitation window (squared distance vs (maxHealth*7)²). */
    public boolean getAgitationState() {
        final LivingEntity target = this.getTarget();
        return TaintacleAiLogic.agitated(target != null,
                target == null ? Double.MAX_VALUE : target.distanceToSqr(this), this.getMaxHealth());
    }

    /**
     * :255-260 — the original's own yaw-only facing (it overrides 1.7.10's
     * {@code Entity.faceEntity}, which 1.21.1 does not carry).
     */
    public void faceEntity(final Entity target, final float speed) {
        final double dx = target.getX() - this.getX();
        final double dz = target.getZ() - this.getZ();
        final float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        this.setYRot(this.rotateTowards(this.getYRot(), targetYaw, speed));
    }

    /** :262-273 — clamp the turn to ±speed degrees. */
    protected float rotateTowards(final float current, final float target, final float maxDelta) {
        float delta = net.minecraft.util.Mth.wrapDegrees(target - current);
        if (delta > maxDelta) {
            delta = maxDelta;
        }
        if (delta < -maxDelta) {
            delta = -maxDelta;
        }
        return current + delta;
    }

    /** :166-207 — the original's hand-rolled {@code attackEntityAsMob}. */
    @Override
    public boolean doHurtTarget(final Entity target) {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);   // :167
        final MobEffectInstance strength = this.getEffect(MobEffects.DAMAGE_BOOST);
        if (strength != null) {
            damage += TaintacleAiLogic.STRENGTH_BONUS_PER_LEVEL << strength.getAmplifier();   // :168-170
        }
        final MobEffectInstance weakness = this.getEffect(MobEffects.WEAKNESS);
        if (weakness != null) {
            damage -= TaintacleAiLogic.WEAKNESS_PENALTY_PER_LEVEL << weakness.getAmplifier();  // :172-174
        }
        int knockback = 0;
        final ItemStack weapon = this.getMainHandItem();
        if (target instanceof LivingEntity livingTarget) {
            // :177-180 — 1.7.10 sums the attacker's sharpness/smite/bane bonus and
            // its knockback level off the held weapon. 1.21.1 has no mob-side
            // melee-enchantment helper, so the levels are read straight off the
            // stack; a taintacle carries no item, so both are 0 in play.
            damage += weaponEnchantmentBonus(this.registryAccess(), weapon, livingTarget);
            knockback += EnchantmentHelper.getItemEnchantmentLevel(enchHolder(this.registryAccess(), Enchantments.KNOCKBACK), weapon);
        }

        final DamageSource source = DamageSourceThaumcraft.causeTentacleDamage(this);
        final boolean hit = target.hurt(source, damage);                     // :182
        if (hit) {
            if (knockback > 0) {
                // :185-189 — knock the target along the tendril's facing, y fixed at 0.1
                target.setDeltaMovement(target.getDeltaMovement().add(
                        TaintacleAiLogic.knockbackX(this.getYRot(), knockback),
                        TaintacleAiLogic.KNOCKBACK_Y,
                        TaintacleAiLogic.knockbackZ(this.getYRot(), knockback)));
                // :190-191 — the tendril drags its own motion to 0.6
                final Vec3 motion = this.getDeltaMovement();
                this.setDeltaMovement(motion.x * TaintacleAiLogic.SELF_DRAG_AFTER_KNOCKBACK, motion.y,
                        motion.z * TaintacleAiLogic.SELF_DRAG_AFTER_KNOCKBACK);
            }
            final int fire = EnchantmentHelper.getItemEnchantmentLevel(enchHolder(this.registryAccess(), Enchantments.FIRE_ASPECT), weapon);
            if (fire > 0) {
                target.igniteForSeconds(TaintacleAiLogic.fireSeconds(fire));  // :194-197
            }
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // :199-203 — thorns and the enchantment hit effects
                EnchantmentHelper.doPostAttackEffects(serverLevel, target, source);
            }
        }
        return hit;
    }

    private static float weaponEnchantmentBonus(final net.minecraft.core.RegistryAccess registryAccess,
            final ItemStack weapon, final LivingEntity target) {
        float bonus = EnchantmentHelper.getItemEnchantmentLevel(enchHolder(registryAccess, Enchantments.SHARPNESS), weapon);
        bonus += EnchantmentHelper.getItemEnchantmentLevel(enchHolder(registryAccess, Enchantments.SMITE), weapon);
        // 1.7.10 gates this term on the arthropod CATEGORY; 21.1.259 exposes no
        // entity-category API, so the level is summed ungated (it is 0 for an
        // unarmed taintacle, so the melee number is unchanged).
        bonus += EnchantmentHelper.getItemEnchantmentLevel(enchHolder(registryAccess, Enchantments.BANE_OF_ARTHROPODS), weapon);
        return bonus;
    }

    /** :240-249 — being hit from beyond 16 blocks bursts taintlets at the attacker. */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (source.getEntity() != null
                && TaintacleAiLogic.burstsTentaclesWhenHurt(this instanceof EntityTaintacleSmall,
                        true, this.distanceTo(source.getEntity()), this.level().isClientSide)) {
            this.spawnTentacles(source.getEntity());
        }
        return super.hurt(source, amount);
    }

    /**
     * :209-238 — the taintlet burst. Cooldown 40 + rand(20) on the shared
     * {@code attackTime} counter, the taintlet spawns jittered around the
     * target's feet, and in the eldritch lands the ground converts to taint
     * fibres (1 in 4 gets the grass meta).
     */
    protected void spawnTentacles(final Entity target) {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        final BlockPos at = BlockPos.containing(target.getX(), target.getY(), target.getZ());
        final boolean eldritch = isEldritchBiome(TaintBiomes.biomeAt(serverLevel, at));
        final boolean taintGround = TaintManager.isTaintBlock(serverLevel.getBlockState(at))
                || TaintManager.isTaintBlock(serverLevel.getBlockState(at.below()));   // :216-218
        if (!eldritch && !(TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(serverLevel, at)) && taintGround)) {
            return;
        }
        this.attackTime = TaintacleAiLogic.tentacleCooldown(this.random);              // :220
        final RandomSource random = serverLevel.random;
        // :221-229 — the taintlet, jittered ±1 block in x/z on the target's feet.
        // TaintManager.spawnGuarded is the house seam: it no-ops until
        // thaumcraft:taintacletiny is registered, which this card does.
        final Vec3 at3 = new Vec3(target.getX() + random.nextFloat() - random.nextFloat(),
                target.getY(),
                target.getZ() + random.nextFloat() - random.nextFloat());
        TaintManager.spawnGuarded(serverLevel, TAINTLET_ID, at3);
        // :230
        this.playSound(ModSounds.TENTACLE.get(), this.getSoundVolume(), this.getVoicePitch());
        if (eldritch && serverLevel.isEmptyBlock(at)
                && isAdjacentToSolidBlock(serverLevel, at)) {
            // :231-236 — Utils.setBiomeAt + the fibres placement. Unreachable until
            // the eldritch biome has an id contract (see NO_ELDRITCH_BIOME_CONTRACT).
            TaintBiomes.setBiomeAtAndSync(serverLevel, at.getX(), at.getZ(),
                    TaintBiomes.taintBiomeHolder(serverLevel));
            serverLevel.setBlock(at, CatalogTaint.BLOCK_TAINT_FIBRES.get().defaultBlockState()
                    .setValue(BlockTaintFibres.VARIANT, TaintacleAiLogic.spreadFibresMeta(random)),
                    Block.UPDATE_ALL);                                            // :235
        }
    }

    /** The 1.7.10 spawn id of the taintlet ({@code thaumcraft:taintacletiny}). */
    public static final ResourceLocation TAINTLET_ID =
            ResourceLocation.fromNamespaceAndPath(
                    com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "taintacletiny");

    /** See {@link #NO_ELDRITCH_BIOME_CONTRACT}. */
    static boolean isEldritchBiome(final net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
        return false;
    }

    /**
     * 1.7.10 {@code BlockUtils.isAdjacentToSolidBlock} (cited at :233): any of the
     * six neighbours is a solid block.
     */
    static boolean isAdjacentToSolidBlock(final Level level, final BlockPos pos) {
        for (final net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isSolidRender(level, pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    /** :299-307 — the original's dropRareDrop: goo (meta 11) or fungus (meta 12). */
    @Override
    protected void dropFromLootTable(final DamageSource source, final boolean attackedRecently) {
        final ItemStack drop = com.jonnyutah12.thaumcraftclassic.items.ItemResource
                .stack(TaintacleAiLogic.rollsGoo(this.random)
                        ? TaintacleAiLogic.META_TAINTED_GOO : TaintacleAiLogic.META_TAINTED_FUNGUS);
        this.spawnAtLocation(drop, TaintacleAiLogic.dropOffset(this.getMaxHealth()));   // :301/:303
        super.dropFromLootTable(source, attackedRecently);                              // :306
    }

    /** The melee swing counter (1.7.10 {@code field_70724_aR}). */
    public int getAttackTime() {
        return this.attackTime;
    }

    /** For probes: force the swing counter. */
    public void setAttackTime(final int ticks) {
        this.attackTime = ticks;
    }
}
