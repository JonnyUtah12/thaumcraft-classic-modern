package com.jonnyutah12.thaumcraftclassic.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Deadly gaze (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft/common/lib/potions/PotionDeathGaze.java}
 * (Thaumcraft:potion.deathgaze, color 6702131, Config.java line 339). The
 * effect class has an empty per-tick body in the original; the behaviour
 * lives in {@code WarpEvents.checkDeathGaze} (driven from
 * {@code EventHandlerEntity.livingTick} at {@code tickCount % 10}), which
 * belongs to the warp-symptom milestone (TC-M6-04d). Registered here as the
 * flag that milestone consults.</p>
 */
public class PotionDeathGaze extends MobEffect {

    public PotionDeathGaze() {
        super(MobEffectCategory.HARMFUL, 6702131);
    }
}
