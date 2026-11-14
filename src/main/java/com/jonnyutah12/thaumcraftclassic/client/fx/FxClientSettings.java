package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The client options the original's FX senders read (TC-M7-02-c-s-f3).
 *
 * <p>{@code ClientProxy.particleCount(int)}
 * (reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:949-955)
 * reads the vanilla particle setting directly off the game options on every
 * spawn loop, so a block's {@code randomDisplayTick} needs the same read. This
 * is the port's ONE read of that option: {@code ClientEffectHooks} (the
 * packet-driven senders) and the senders that live on blocks, items and block
 * entities all go through it, and those are all
 * client-only code paths ({@code animateTick} is never invoked on a dedicated
 * server), so the client-only class reference is safe here.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class FxClientSettings {

    private FxClientSettings() {
    }

    /**
     * The vanilla particle setting id the original compares against 2 (minimal)
     * and 1 (decreased): ALL=0, DECREASED=1, MINIMAL=2, fed to
     * {@link FxParticleCounts#particleCount(int, int)}.
     */
    public static int particleSetting() {
        return Minecraft.getInstance().options.particles().get().getId();
    }
}
