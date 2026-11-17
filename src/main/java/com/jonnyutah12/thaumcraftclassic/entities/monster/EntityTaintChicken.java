package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic.GoalSpec;
import java.util.List;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Port of {@code thaumcraft.common.entities.monster.EntityTaintChicken}
 * (TC-M6-06-s-b). An {@code EntityMob} with a goal table in the original —
 * exactly what this class builds from {@link TaintedAnimalAiLogic}.
 * Box 0.5x0.8 ({@code func_70105_a}, ctor line 34), health 8.0 / attack 3.0
 * / speed 0.4 ({@code func_110147_ax} lines 51-53), armour 2
 * ({@code func_70658_aO} lines 64-65), voice pitch 0.7 ({@code func_70647_i}
 * lines 121-122). The original sets NO {@code field_70728_aV}, so no
 * {@code xpReward} is invented.
 *
 * <p>Deferred to their own rows: the egg-throw {@code func_70097_a} hook
 * (none in the original — the chicken is a plain attacker) and the flap
 * animation (TC-M7-03-s renderers).</p>
 */
public class EntityTaintChicken extends EntityTaintedAnimalBase {

    public EntityTaintChicken(final EntityType<? extends EntityTaintChicken> type, final Level level) {
        super(type, level);
        // ctor line 34: func_70105_a(0.5F, 0.8F); the original sets no
        // field_70728_aV, so no xpReward is invented here
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, TaintedAnimalAiLogic.CHICKEN.health())
                .add(Attributes.ATTACK_DAMAGE, TaintedAnimalAiLogic.CHICKEN.attack())
                .add(Attributes.MOVEMENT_SPEED, TaintedAnimalAiLogic.CHICKEN.movementSpeed())
                .add(Attributes.ARMOR, TaintedAnimalAiLogic.CHICKEN.armor());
    }

    @Override
    protected List<GoalSpec> goalTable() {
        return TaintedAnimalAiLogic.chickenGoals();
    }

    @Override
    protected List<GoalSpec> targetTable() {
        return TaintedAnimalAiLogic.chickenTargetGoals();
    }

    /** func_70647_i (getSoundPitch) → 1.21.1's getVoicePitch: 0.7. */
    @Override
    public float getVoicePitch() {
        return TaintedAnimalAiLogic.chickenSoundPitch();
    }

    // The original PERMUTES the vanilla slots (all three overrides quoted
    // with their strings): func_70673_aS (living) lines 117-118 returns
    // "mob.chicken.hurt", func_70639_aQ (hurt) lines 109-110 returns
    // "mob.chicken.say", func_70621_aR (death) lines 113-114 returns
    // "mob.chicken.hurt". 1.21.1: CHICKEN_AMBIENT = "mob.chicken.say",
    // CHICKEN_HURT = "mob.chicken.hurt".
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CHICKEN_HURT; // "mob.chicken.hurt" (117-118)
    }

    @Override
    protected SoundEvent getHurtSound(final net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.CHICKEN_AMBIENT; // "mob.chicken.say" (109-110)
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_HURT; // "mob.chicken.hurt" (113-114)
    }
}
