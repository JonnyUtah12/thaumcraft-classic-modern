package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.potions.PotionBlurredVision;
import com.jonnyutah12.thaumcraftclassic.potions.PotionDeathGaze;
import com.jonnyutah12.thaumcraftclassic.potions.PotionFluxTaint;
import com.jonnyutah12.thaumcraftclassic.potions.PotionInfectiousVisExhaust;
import com.jonnyutah12.thaumcraftclassic.potions.PotionSunScorned;
import com.jonnyutah12.thaumcraftclassic.potions.PotionThaumarhia;
import com.jonnyutah12.thaumcraftclassic.potions.PotionUnnaturalHunger;
import com.jonnyutah12.thaumcraftclassic.potions.PotionVisExhaust;
import com.jonnyutah12.thaumcraftclassic.potions.PotionWarpWard;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mob effect registration holder (TC-M2-01 registration architecture).
 *
 * <p>Every kind=potion row in docs/content-mapping.csv (the original's 9
 * registered potion effects) registers here with its canonical id; TC-M6-05
 * implements them with original parameters (tick gates, colors and effect
 * bodies in {@code potions/}, mirroring the original class names). The
 * {@code new_id} column is the id contract
 * (docs/architecture/registration.md).</p>
 *
 * <p>The original registered these through config-assigned numeric potion ids
 * ({@code Config.getNextPotionId}); 1.21.1 registries are id-based, so the
 * numeric-id allocation and its collision handling are dropped (documented in
 * docs/architecture/enchantments-potions.md).</p>
 */
public final class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, ThaumcraftClassic.MOD_ID);

    /** Thaumcraft:potion.fluxtaint (Taint Poison). */
    public static final DeferredHolder<MobEffect, PotionFluxTaint> POTION_FLUXTAINT =
            MOB_EFFECTS.register("potion.fluxtaint", PotionFluxTaint::new);

    /** Thaumcraft:potion.visexhaust (Flux Flu). */
    public static final DeferredHolder<MobEffect, PotionVisExhaust> POTION_VISEXHAUST =
            MOB_EFFECTS.register("potion.visexhaust", PotionVisExhaust::new);

    /** Thaumcraft:potion.infvisexhaust (Flux Phage). */
    public static final DeferredHolder<MobEffect, PotionInfectiousVisExhaust> POTION_INFVISEXHAUST =
            MOB_EFFECTS.register("potion.infvisexhaust", PotionInfectiousVisExhaust::new);

    /** Thaumcraft:potion.unhunger (Unnatural Hunger). */
    public static final DeferredHolder<MobEffect, PotionUnnaturalHunger> POTION_UNHUNGER =
            MOB_EFFECTS.register("potion.unhunger", PotionUnnaturalHunger::new);

    /** Thaumcraft:potion.warpward (Warp Ward). */
    public static final DeferredHolder<MobEffect, PotionWarpWard> POTION_WARWARD =
            MOB_EFFECTS.register("potion.warpward", PotionWarpWard::new);

    /** Thaumcraft:potion.deathgaze (Deadly Gaze). */
    public static final DeferredHolder<MobEffect, PotionDeathGaze> POTION_DEATHGAZE =
            MOB_EFFECTS.register("potion.deathgaze", PotionDeathGaze::new);

    /** Thaumcraft:potion.blurred (Blurred Vision). */
    public static final DeferredHolder<MobEffect, PotionBlurredVision> POTION_BLURRED =
            MOB_EFFECTS.register("potion.blurred", PotionBlurredVision::new);

    /** Thaumcraft:potion.sunscorned (Sun Scorned). */
    public static final DeferredHolder<MobEffect, PotionSunScorned> POTION_SUNSCORNED =
            MOB_EFFECTS.register("potion.sunscorned", PotionSunScorned::new);

    /** Thaumcraft:potion.thaumarhia (Thaumarhia). */
    public static final DeferredHolder<MobEffect, PotionThaumarhia> POTION_THAUMARHIA =
            MOB_EFFECTS.register("potion.thaumarhia", PotionThaumarhia::new);

    private ModMobEffects() {
    }
}
