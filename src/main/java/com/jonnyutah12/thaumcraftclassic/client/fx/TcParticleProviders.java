package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Binds the ported particle types to their providers (TC-M7-02-s).
 *
 * <p>Both are registered through {@code registerSpriteSet}, the multi-texture
 * path: the frame list comes from the particle definition JSON
 * ({@code assets/thaumcraft/particles/{sparkle,bubble}.json}), which is how
 * the original's frame-advance and pop-frame behaviour is expressed in
 * 1.21.1 — {@code FXGeneric.setParticleTextureIndex} and
 * {@code FXBubble}'s {@code particle++} both index a frame list, and
 * {@code SpriteSet} is the modern frame list. The frames themselves are sliced
 * out of the original 16×16 cell grid in {@code textures/misc/particles.png}
 * by the {@code sliceOriginalParticleSprites} build task.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TcParticleProviders {

    @SubscribeEvent
    public static void onRegisterParticleProviders(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SPARCLE.get(), TcSparkleParticle.Provider::new);
        event.registerSpriteSet(ModParticles.BUBBLE.get(), TcBubbleParticle.Provider::new);
        // TC-M7-02-c-s: wisp family, sender motes, and the entity-space shells.
        event.registerSpriteSet(ModParticles.WISP.get(), TcWispParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WISP_EG.get(), TcWispEgParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WISP_ARCING.get(), TcWispArcingParticle.Provider::new);
        event.registerSpriteSet(ModParticles.VIS_SPARKLE.get(), TcVisSparkleParticle.Provider::new);
        event.registerSpriteSet(ModParticles.BORE_SPARKLE.get(), TcBoreSparkleParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ESSENTIA_TRAIL.get(), TcEssentiaTrailParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SHIELD_RUNES.get(), TcShieldRunesParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SHIELD_RUNES_MOB.get(), TcShieldRunesParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SONIC.get(), TcSonicParticle.Provider::new);
    }

    private TcParticleProviders() {
    }
}
