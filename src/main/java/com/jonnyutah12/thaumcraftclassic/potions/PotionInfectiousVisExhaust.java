package com.jonnyutah12.thaumcraftclassic.potions;

import com.jonnyutah12.thaumcraftclassic.registration.ModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Infectious vis exhaustion (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft/common/lib/potions/PotionInfectiousVisExhaust.java}
 * (Thaumcraft:potion.infvisexhaust, color 6706551, Config.java line 333).
 * Every 40 ticks it spreads: at amplifier &gt; 0 it gives every visible
 * living entity within 8 blocks infectious exhaustion at amplifier - 1 for
 * 3600 ticks (keeping a higher existing amplifier); at amplifier 0 it
 * converts to plain vis exhaustion (amp 0, 3600 ticks) on those entities.
 * The spread is deterministic, so the gametest can pin it.</p>
 */
public class PotionInfectiousVisExhaust extends MobEffect {

    public PotionInfectiousVisExhaust() {
        super(MobEffectCategory.HARMFUL, 6706551);
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
        for (final LivingEntity nearby : target.level().getEntitiesOfClass(
                LivingEntity.class, target.getBoundingBox().inflate(8.0))) {
            if (nearby == target || nearby.isInvisible()) {
                continue;
            }
            if (amplifier > 0) {
                int amp = amplifier - 1;
                if (nearby.hasEffect(ModMobEffects.POTION_INFVISEXHAUST)) {
                    amp = Math.max(amp, nearby.getEffect(ModMobEffects.POTION_INFVISEXHAUST).getAmplifier());
                }
                nearby.addEffect(new MobEffectInstance(ModMobEffects.POTION_INFVISEXHAUST, 3600, amp));
            } else {
                nearby.addEffect(new MobEffectInstance(ModMobEffects.POTION_VISEXHAUST, 3600, 0));
            }
        }
        return true;
    }
}
