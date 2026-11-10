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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * Port of {@code thaumcraft.common.entities.monster.EntityTaintCow}
 * (TC-M6-06-s-b). Box 0.9x1.3 ({@code func_70105_a}, ctor line 26), health
 * 40.0 / attack 6.0 / speed 0.27 ({@code func_110147_ax} lines 43-45),
 * voice pitch 0.7 ({@code func_70647_i} lines 76-77), volume 0.4
 * ({@code func_70599_aP} lines 80-81). The cow sets no armour override.
 * {@code EntityTaintPig} is the same class with pig numbers (its ctor
 * lines 24-38 — goals, targets and the door flag — are byte-identical to
 * the cow's).
 */
public class EntityTaintCow extends EntityTaintedAnimalBase {

    private final TaintedAnimalAiLogic.Stats stats;

    public EntityTaintCow(final EntityType<? extends EntityTaintCow> type, final Level level) {
        this(TaintedAnimalAiLogic.COW, type, level);
    }

    protected EntityTaintCow(final TaintedAnimalAiLogic.Stats stats,
            final EntityType<? extends EntityTaintedAnimalBase> type, final Level level) {
        super(type, level);
        this.stats = stats;
        // no xpReward: the original sets no field_70728_aV
        // func_75491_a(true) (EntityTaintCow line 27, EntityTaintPig line
        // 27): the taint animals open doors — 1.21.1's flag
        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributesFor(TaintedAnimalAiLogic.COW);
    }

    public static AttributeSupplier.Builder createPigAttributes() {
        return createAttributesFor(TaintedAnimalAiLogic.PIG);
    }

    private static AttributeSupplier.Builder createAttributesFor(final TaintedAnimalAiLogic.Stats stats) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, stats.health())
                .add(Attributes.ATTACK_DAMAGE, stats.attack())
                .add(Attributes.MOVEMENT_SPEED, stats.movementSpeed())
                .add(Attributes.ARMOR, stats.armor());
    }

    @Override
    protected List<GoalSpec> goalTable() {
        return TaintedAnimalAiLogic.cowGoals();
    }

    @Override
    protected List<GoalSpec> targetTable() {
        return TaintedAnimalAiLogic.cowTargetGoals();
    }

    /** func_70647_i (getSoundPitch) → 1.21.1's getVoicePitch: 0.7. */
    @Override
    public float getVoicePitch() {
        return TaintedAnimalAiLogic.cowSoundPitch();
    }

    /** func_70599_aP (line 80-81): 0.4 — the only animal volume override. */
    @Override
    protected float getSoundVolume() {
        return TaintedAnimalAiLogic.cowSoundVolume();
    }

    // The original permutes the slots: func_70639_aQ (hurt) lines 60-61
    // "mob.cow.say", func_70621_aR (death) lines 64-65 "mob.cow.hurt",
    // func_70673_aS (living) lines 68-69 "mob.cow.hurt".
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COW_HURT; // "mob.cow.hurt" (68-69)
    }

    @Override
    protected SoundEvent getHurtSound(final net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.COW_AMBIENT; // "mob.cow.say" (60-61)
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COW_HURT; // "mob.cow.hurt" (64-65)
    }

    /** The pig twin ({@code EntityTaintPig}, ctor lines 24-38 identical). */
    public static class Pig extends EntityTaintCow {
        public Pig(final EntityType<? extends Pig> type, final Level level) {
            super(TaintedAnimalAiLogic.PIG, type, level);
        }

        @Override
        protected List<GoalSpec> goalTable() {
            return TaintedAnimalAiLogic.pigGoals();
        }

        @Override
        protected List<GoalSpec> targetTable() {
            return TaintedAnimalAiLogic.pigTargetGoals();
        }

        @Override
        public float getVoicePitch() {
            return TaintedAnimalAiLogic.pigSoundPitch();
        }

        // The original permutes the slots: func_70639_aQ (hurt) lines 82-83
        // "mob.pig.say", func_70621_aR (death) lines 86-87 "mob.pig.say",
        // func_70673_aS (living) lines 90-91 "mob.pig.death".
        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.PIG_DEATH; // "mob.pig.death" (90-91)
        }

        @Override
        protected SoundEvent getHurtSound(final net.minecraft.world.damagesource.DamageSource source) {
            return SoundEvents.PIG_AMBIENT; // "mob.pig.say" (82-83)
        }

        @Override
        protected SoundEvent getDeathSound() {
            return SoundEvents.PIG_AMBIENT; // "mob.pig.say" (86-87)
        }
    }
}
