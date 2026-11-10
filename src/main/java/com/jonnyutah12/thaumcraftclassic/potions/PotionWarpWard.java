package com.jonnyutah12.thaumcraftclassic.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Warp ward (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft/common/lib/potions/PotionWarpWard.java}
 * (Thaumcraft:potion.warpward, bad=false, color 14742263, Config.java line
 * 337). The effect itself has no per-tick behaviour; it is a flag consulted
 * by the warp-event roll ({@code WarpEvents.checkWarpEvent}, TC-M6-04d) and
 * by the sanity soap's sticky-warp removal chance
 * ({@code ItemSanitySoap}, ported here). The original's
 * {@code func_76404_a(0.25)} magic-color modifier is potion-render data and
 * belongs to the client milestone (TC-M7-01).</p>
 */
public class PotionWarpWard extends MobEffect {

    public PotionWarpWard() {
        super(MobEffectCategory.BENEFICIAL, 14742263);
    }
}
