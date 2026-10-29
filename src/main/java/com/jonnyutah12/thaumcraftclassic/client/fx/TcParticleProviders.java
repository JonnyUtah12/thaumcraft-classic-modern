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
        // TC-M7-02-b-s: the world-rendered bolt/beam family. Every type is a
        // sprite-set provider; the JSON frame lists carry the original sheets
        // (bolt_small/bolt_large, beam0..beam3, impact_0..15, arc,
        // sparkle_glow_0..3), sliced at build time (D-004).
        event.registerSpriteSet(ModParticles.BOLT.get(), TcBoltParticle.Provider::new);
        event.registerSpriteSet(ModParticles.BEAM.get(), TcBeamParticle.Provider::new);
        event.registerSpriteSet(ModParticles.BEAM_IMPACT.get(), TcBeamImpactParticle.Provider::new);
        event.registerSpriteSet(ModParticles.BEAM_GOLEM_BOSS.get(), TcGolemBeamParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ARC.get(), TcArcParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPARKLE_GLOW.get(), TcGlowSparkleParticle.Provider::new);
        // TC-M7-02-c-s-f1: the wisp family. The EG mote steps the 13-cell
        // strip the original indexes (FXWispEG.java:59, u = age%13/16,
        // v = 0.1875 -> cells 48..60 of the sheet grid), sliced at build time
        // like the sparkle cells. The wisp and the arcing wisp draw regions of
        // the VANILLA /misc/fx.png sheet (FXWisp.java:148-151 u 0..0.125
        // v 0.875..1; FXWispArcing.java:74-77 u 0..0.875 v 0.875..1), which
        // the reference tree does not ship, so they stand in on the Thaumcraft
        // glow sparkle frame (sparkle_0) — BD-18 in
        // docs/BEHAVIORAL_DEVIATIONS.md.
        event.registerSpriteSet(ModParticles.WISP.get(), TcWispParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WISP_EG.get(), TcWispEgParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WISP_ARCING.get(), TcWispArcingParticle.Provider::new);
    }

    private TcParticleProviders() {
    }
}
