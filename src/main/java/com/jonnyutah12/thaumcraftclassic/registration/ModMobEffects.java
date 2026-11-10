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
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mob effect registration holder (TC-M2-01 registration architecture).
 *
 * <p>Every kind=potion row in docs/content-mapping.csv (the original's 9
 * registered potion effects: flux taint, warp, inversion, …) registers here
 * with its canonical id; TC-M6-05 implements them with original parameters.
 * The {@code new_id} column is the id contract
 * (docs/architecture/registration.md).</p>
 */
public final class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, ThaumcraftClassic.MOD_ID);

    // ---- TC-M6-05-s: the nine effects the original registers ----------------
    // Ids are the content-mapping.csv new_id column verbatim
    // (thaumcraft:potion.<name>, D-001 lower-case rule). 1.21.1 has no numeric
    // potion ids, so the original's Config.java id becomes the registry path
    // (BD: documented in docs/architecture/enchantments-potions.md); the
    // original colour is the MobEffectCategory + colour constructor argument,
    // and the original's isReady/onEffectTick pair maps to
    // MobEffect#shouldApplyEffectTickThisTick / #applyEffectTick.
    /** Taint Poison (original color 6697847, {@code Config.java:331}). */
    public static final DeferredHolder<MobEffect, PotionFluxTaint> POTION_FLUXTAINT =
            MOB_EFFECTS.register("potion.fluxtaint", PotionFluxTaint::new);

    /** Flux Flu (original color 6702199, {@code Config.java:332}). */
    public static final DeferredHolder<MobEffect, PotionVisExhaust> POTION_VISEXHAUST =
            MOB_EFFECTS.register("potion.visexhaust", PotionVisExhaust::new);

    /** Flux Phage (original color 6706551, {@code Config.java:333}). */
    public static final DeferredHolder<MobEffect, PotionInfectiousVisExhaust> POTION_INFVISEXHAUST =
            MOB_EFFECTS.register("potion.infvisexhaust", PotionInfectiousVisExhaust::new);

    /** Unnatural Hunger (original color 4482611, {@code Config.java:335}). */
    public static final DeferredHolder<MobEffect, PotionUnnaturalHunger> POTION_UNHUNGER =
            MOB_EFFECTS.register("potion.unhunger", PotionUnnaturalHunger::new);

    /** Warp Ward, the only beneficial effect (original color 14742263). */
    public static final DeferredHolder<MobEffect, PotionWarpWard> POTION_WARWARD =
            MOB_EFFECTS.register("potion.warpward", PotionWarpWard::new);

    /** Deadly Gaze (original color 6702131) — flag for the warp driver. */
    public static final DeferredHolder<MobEffect, PotionDeathGaze> POTION_DEATHGAZE =
            MOB_EFFECTS.register("potion.deathgaze", PotionDeathGaze::new);

    /** Blurred Vision (original color 8421504) — flag for the client overlay. */
    public static final DeferredHolder<MobEffect, PotionBlurredVision> POTION_BLURRED =
            MOB_EFFECTS.register("potion.blurred", PotionBlurredVision::new);

    /** Sun Scorned (original color 16308330). */
    public static final DeferredHolder<MobEffect, PotionSunScorned> POTION_SUNSCORNED =
            MOB_EFFECTS.register("potion.sunscorned", PotionSunScorned::new);

    /** Thaumarhia (original color 6702199). */
    public static final DeferredHolder<MobEffect, PotionThaumarhia> POTION_THAUMARHIA =
            MOB_EFFECTS.register("potion.thaumarhia", PotionThaumarhia::new);

    private ModMobEffects() {
    }
}
