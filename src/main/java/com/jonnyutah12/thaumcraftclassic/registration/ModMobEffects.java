package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
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

    private ModMobEffects() {
    }
}
