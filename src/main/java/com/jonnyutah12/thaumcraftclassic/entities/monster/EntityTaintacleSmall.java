package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * The Taint Tendril / "taintlet" (TC-M6-06-s-e, content-mapping.csv
 * {@code thaumcraft:taintacletiny}, original class {@code EntityTaintacleSmall}).
 *
 * <p>Port of {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/EntityTaintacleSmall.java}
 * (45 lines): a 0.22×1.0 tendril with 8 health and 2 attack that lives exactly 200
 * ticks and then kills itself. It spawns from
 * {@link EntityTaintacle#spawnTentacles} and from the giant's burst.</p>
 *
 * <p>Quirks kept verbatim:</p>
 * <ul>
 *   <li>xpReward 0 (:15) — a taintlet is worth nothing;</li>
 *   <li>the lifetime check is {@code lifetime-- <= 0} (:28), so it self-destructs
 *       on tick 201 of its own counter and keeps decrementing afterwards, taking
 *       10 magic damage every tick after that ({@code DamageSource.magic}, :29);</li>
 *   <li>it never spawns from natural spawning (:34-36 → {@code checkSpawnRules}
 *       false) — it is spawn-on-demand content only;</li>
 *   <li>it drops nothing (:38-44 — the 1.7.10 {@code func_146068_u} returned item
 *       id 0 (air) and {@code dropRareDrop} is empty);</li>
 *   <li>the far-hit tentacle burst of the parent is switched off for taintlets
 *       ({@code EntityTaintacle.java:160}, {@code :241}).</li>
 * </ul>
 */
public class EntityTaintacleSmall extends EntityTaintacle {

    /** :10 — 200 ticks. */
    private int lifetime = TaintacleAiLogic.SMALL_LIFETIME_START;

    public EntityTaintacleSmall(final EntityType<? extends EntityTaintacleSmall> type, final Level level) {
        super(type, level);
        // :15 — xpReward 0
        this.xpReward = TaintacleAiLogic.SMALL_XP;
    }

    /** :19-23 — maxHealth 8, attackDamage 2 (on top of the parent's 50/7). */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, TaintacleAiLogic.SMALL_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, TaintacleAiLogic.SMALL_ATTACK);
    }

    /** :14 — the 0.22×1.0 box; eye height keeps the parent's 0.25 (:54-56). */
    @Override
    protected EntityDimensions getDefaultDimensions(final Pose pose) {
        return EntityDimensions.fixed(TaintacleAiLogic.SMALL_WIDTH, TaintacleAiLogic.SMALL_HEIGHT)
                .withEyeHeight(TaintacleAiLogic.EYE_HEIGHT);
    }

    /** :26-31 — the 200-tick life, then 10 magic damage a tick forever. */
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.lifetime-- <= 0) {
            this.hurt(this.level().damageSources().magic(), TaintacleAiLogic.SMALL_LIFETIME_DAMAGE);
        }
    }

    /** :33-36 — never a natural spawn. */
    @Override
    public boolean checkSpawnRules(final LevelAccessor level, final MobSpawnType reason) {
        return false;
    }

    /** :38-44 — a taintlet drops nothing (its drop item is air and dropRareDrop is empty). */
    @Override
    protected void dropFromLootTable(final DamageSource source, final boolean attackedRecently) {
        // Intentionally empty: no call to super, no goo, no fungus. The parent's
        // 50/50 goo/fungus drop (:299-307) is not inherited by a taintlet.
    }

    /** The self-destruct counter, for probes. */
    public int getLifetime() {
        return this.lifetime;
    }
}
