package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityWisp;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEntities;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Binds {@code thaumcraft:wisp} to {@link EntityWispRenderer} (TC-M7-03-s).
 *
 * <p>Original: {@code ClientProxy.setupEntityRenderers()}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:410})
 * registers exactly this one line in this slice —
 * {@code RenderingRegistry.registerEntityRenderingHandler(EntityWisp.class,
 * new RenderWisp())}. The other 43 lines of that method are other cards: their
 * entity classes, and therefore their renderers, do not exist yet.</p>
 *
 * <p>Client-only on the mod bus, so a dedicated server ({@code runServer},
 * {@code runGameTestServer}) never loads the renderer at all. The registration
 * itself is a small seam ({@link #register}) that the unit test drives with a
 * real {@link EntityRenderersEvent.RegisterRenderers}, which is the same call
 * the client fires.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityRenderers {

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        register(event, CatalogEntities.WISP.get());
    }

    /**
     * The registration seam: the production handler passes the live
     * {@code thaumcraft:wisp} type, and the unit test passes a throwaway type so
     * it can observe the vanilla renderer map without a bootstrapped registry.
     */
    static void register(final EntityRenderersEvent.RegisterRenderers event, final EntityType<EntityWisp> type) {
        event.registerEntityRenderer(type, EntityWispRenderer::new);
    }

    private ModEntityRenderers() {
    }
}
