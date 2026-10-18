package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Particle type registration holder (TC-M2-01 registration architecture).
 *
 * <p>The original's visual effects were network-driven FX renderers
 * (TC-FX-*), not particle types. Where the port expresses them as vanilla
 * particles (M6/M7 visuals), the particle types register here; the FX
 * payloads themselves are already ported (TC-M2-03).</p>
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ThaumcraftClassic.MOD_ID);

    private ModParticles() {
    }
}
