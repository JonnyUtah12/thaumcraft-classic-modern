package com.jonnyutah12.thaumcraftclassic.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Blurred vision (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft/common/lib/potions/PotionBlurredVision.java}
 * (Thaumcraft:potion.blurred, color 8421504, Config.java line 341). The
 * original's per-tick body is empty; the effect is purely a client screen
 * overlay (a warp symptom, rendered by the client milestone TC-M7-02). The
 * symptom that applies it ({@code WarpEvents}) is TC-M6-04d.</p>
 */
public class PotionBlurredVision extends MobEffect {

    public PotionBlurredVision() {
        super(MobEffectCategory.HARMFUL, 8421504);
    }
}
