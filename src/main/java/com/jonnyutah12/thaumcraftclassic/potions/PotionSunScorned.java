package com.jonnyutah12.thaumcraftclassic.potions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Sun scorned (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft/common/lib/potions/PotionSunScorned.java}
 * (Thaumcraft:potion.sunscorned, color 16308330, Config.java line 343).
 * Every 40 ticks: in direct sky visibility, at light &gt; 0.5 the target
 * catches fire (4 s) with probability {@code (light - 0.4) * 2 / 30}; at
 * light &lt; 0.25 it heals 1 with probability {@code 1 - light * 2}. The
 * original reads the light via {@code Entity.getBrightness(1.0F)} (max of
 * block and sky light, normalized 0..1); the 1.21.1 equivalent is
 * {@code Level.getMaxLocalRawBrightness(pos) / 15.0F}.</p>
 */
public class PotionSunScorned extends MobEffect {

    public PotionSunScorned() {
        super(MobEffectCategory.HARMFUL, 16308330);
    }

    /** Original tick gate: every 40 ticks. */
    public static boolean appliesThisTick(final int durationLeft, final int amplifier) {
        return durationLeft % 40 == 0;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int durationLeft, final int amplifier) {
        return appliesThisTick(durationLeft, amplifier);
    }

    @Override
    public boolean applyEffectTick(final LivingEntity target, final int amplifier) {
        final BlockPos pos = BlockPos.containing(target.getX(), target.getY(), target.getZ());
        final float light = target.level().getMaxLocalRawBrightness(pos) / 15.0F;
        if (light > 0.5F
                && target.level().random.nextFloat() * 30.0F < (light - 0.4F) * 2.0F
                && target.level().canSeeSky(pos)) {
            target.igniteForSeconds(4);
        } else if (light < 0.25F && target.level().random.nextFloat() > light * 2.0F) {
            target.heal(1.0F);
        }
        return true;
    }
}
