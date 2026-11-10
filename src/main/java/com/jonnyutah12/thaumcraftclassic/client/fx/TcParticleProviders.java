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
        // glow sparkle frame (sparkle_0) — BD-46 in
        // docs/BEHAVIORAL_DEVIATIONS.md.
        event.registerSpriteSet(ModParticles.WISP.get(), TcWispParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WISP_EG.get(), TcWispEgParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WISP_ARCING.get(), TcWispArcingParticle.Provider::new);
        // TC-M7-02-c-s-f5: the shared particle families. Each frame list is the
        // strip the original indexes into `textures/misc/particles.png`
        // (FXBlockRunes.java:51-54 → cells 96..111 precedent), derived from the
        // class's own UV lines and sliced at build time by
        // sliceOriginalParticleSprites:
        //   scorch         cells 151..159 (FXScorch.java:100-104)
        //   smoke_spiral   cells 1..5    (FXSmokeSpiral.java:31, u=part%16/16 v=0)
        //   smoke_trail    cells 1..5    (FXSmokeTrail.java:54, same row)
        //   vent           cells 1..5    (FXVent.java:95, same row)
        //   swarm          cells 71..78  (FXSwarm.java:56-59, u=part/16 v=0.25)
        //   bore_sparkle   cells 64..67  (FXBoreSparkle.java:59, part/16=4 v=0.25)
        //   vis_sparkle    cells 128..143 (FXVisSparkle.java:37, part/16=8 v=0.5)
        //   essentia_trail cell 25       (FXEssentiaTrail.java:127)
        //   sonic          the 15 whole-file ripple_*.png frames (FXSonic.java:75-85)
        event.registerSpriteSet(ModParticles.SCORCH.get(), TcScorchParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SMOKE_SPIRAL.get(), TcSmokeSpiralParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SMOKE_TRAIL.get(), TcSmokeTrailParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SWARM.get(), TcSwarmParticle.Provider::new);
        event.registerSpriteSet(ModParticles.VENT.get(), TcVentParticle.Provider::new);
        event.registerSpriteSet(ModParticles.BORE_SPARKLE.get(), TcBoreSparkleParticle.Provider::new);
        event.registerSpriteSet(ModParticles.VIS_SPARKLE.get(), TcVisSparkleParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ESSENTIA_TRAIL.get(),
                TcEssentiaTrailParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SONIC.get(), TcSonicParticle.Provider::new);
        //   drop   cells 112..114 (FXDrop.java:27,52,58,73 indices 112/113/114)
        //   burst  the 32-frame node-sheet strip u = age%32/32 (FXBurst.java:31),
        //          standing in on the glow sparkle frames (BD-67)
        event.registerSpriteSet(ModParticles.DROP.get(), TcDropParticle.Provider::new);
        event.registerSpriteSet(ModParticles.BURST.get(), TcBurstParticle.Provider::new);
    }

    private TcParticleProviders() {
    }
}
