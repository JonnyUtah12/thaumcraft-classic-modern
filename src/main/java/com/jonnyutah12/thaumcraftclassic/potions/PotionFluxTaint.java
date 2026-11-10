package com.jonnyutah12.thaumcraftclassic.potions;

import com.jonnyutah12.thaumcraftclassic.api.damagesource.DamageSourcesThaumcraft;
import com.jonnyutah12.thaumcraftclassic.api.entities.ITaintedMob;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Flux taint poisoning (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft/common/lib/potions/PotionFluxTaint.java}
 * (registered as Thaumcraft:potion.fluxtaint, color 6697847,
 * Config.java line 331). Taint mobs are healed by it; everything else takes
 * 1 taint damage while it is not wet (players always, other mobs only while
 * they still have air). The original's wet check is
 * {@code Entity.isWet()} (water, rain or bubble column), which maps to
 * {@link LivingEntity#isInWaterRainOrBubble()} in 1.21.1.</p>
 */
public class PotionFluxTaint extends MobEffect {

    public PotionFluxTaint() {
        super(MobEffectCategory.HARMFUL, 6697847);
    }

    /**
     * Original tick gate: every {@code 40 >> amplifier} ticks; an amplifier
     * of 6 or more shifts the period to 0, which the original treats as
     * "damage every tick".
     */
    public static boolean appliesThisTick(final int durationLeft, final int amplifier) {
        final int period = 40 >> amplifier;
        return period > 0 ? durationLeft % period == 0 : true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int durationLeft, final int amplifier) {
        return appliesThisTick(durationLeft, amplifier);
    }

    @Override
    public boolean applyEffectTick(final LivingEntity target, final int amplifier) {
        if (target instanceof ITaintedMob) {
            target.heal(1.0F);
        } else if (!target.isInWaterRainOrBubble() && !(target instanceof Player)) {
            target.hurt(DamageSourcesThaumcraft.taint(target.level()), 1.0F);
        } else if (!target.isInWaterRainOrBubble()
                && (target.getAirSupply() > 1 || target instanceof Player)) {
            target.hurt(DamageSourcesThaumcraft.taint(target.level()), 1.0F);
        }
        return true;
    }
}
