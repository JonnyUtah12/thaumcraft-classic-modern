package com.jonnyutah12.thaumcraftclassic.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Unnatural hunger (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft/common/lib/potions/PotionUnnaturalHunger.java}
 * (Thaumcraft:potion.unhunger, color 4482611, Config.java line 335). Every
 * tick it adds {@code 0.025 * (amplifier + 1)} exhaustion to the affected
 * player (the original's {@code EntityPlayer.addExhaustion}). The original's
 * {@code !world.isRemote} guard is implicit in 1.21.1: mob effects only tick
 * on the logical server.</p>
 */
public class PotionUnnaturalHunger extends MobEffect {

    public PotionUnnaturalHunger() {
        super(MobEffectCategory.HARMFUL, 4482611);
    }

    /** Original gate: the effect applies on every tick. */
    public static boolean appliesThisTick(final int durationLeft, final int amplifier) {
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int durationLeft, final int amplifier) {
        return appliesThisTick(durationLeft, amplifier);
    }

    @Override
    public boolean applyEffectTick(final LivingEntity target, final int amplifier) {
        if (target instanceof Player player) {
            player.getFoodData().addExhaustion(0.025F * (amplifier + 1));
        }
        return true;
    }
}
