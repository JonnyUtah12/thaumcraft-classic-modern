package com.jonnyutah12.thaumcraftclassic.potions;

import com.jonnyutah12.thaumcraftclassic.registration.ModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Vis exhaustion (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft/api/potions/PotionVisExhaust.java}
 * (Thaumcraft:potion.visexhaust, color 6702199, Config.java line 332). The
 * effect itself has no per-tick behaviour; its amplifier feeds the wand
 * vis-discount penalty in the original
 * {@code WandManager.getVisDiscountFactor}: {@code total -= (max(visExhaust,
 * infVisExhaust) + 1) * 10} centi-vis. TC-M6-01 (wand casting) calls
 * {@link #exhaustAmplifier(LivingEntity)} and feeds it to
 * {@code wands/VisCostRules}, which already models that modifier.</p>
 */
public class PotionVisExhaust extends MobEffect {

    public PotionVisExhaust() {
        super(MobEffectCategory.HARMFUL, 6702199);
    }

    /**
     * The original's {@code Math.max(level1, level2)} over both exhaustion
     * effects (WandManager.java lines 70-79). Returns -1 when the entity
     * has neither effect, i.e. no penalty applies.
     */
    public static int exhaustAmplifier(final LivingEntity entity) {
        int visExhaust = -1;
        int infVisExhaust = -1;
        if (entity.hasEffect(ModMobEffects.POTION_VISEXHAUST)) {
            visExhaust = entity.getEffect(ModMobEffects.POTION_VISEXHAUST).getAmplifier();
        }
        if (entity.hasEffect(ModMobEffects.POTION_INFVISEXHAUST)) {
            infVisExhaust = entity.getEffect(ModMobEffects.POTION_INFVISEXHAUST).getAmplifier();
        }
        return Math.max(visExhaust, infVisExhaust);
    }
}
