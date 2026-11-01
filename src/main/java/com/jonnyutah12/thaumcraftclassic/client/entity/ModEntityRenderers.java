package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBobber;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchGolem;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The golem renderer and model-layer registration — the 1.21.1 site of
 * Thaumcraft 4.2.3.5's {@code ClientProxy.registerRenderers()} golem block
 * ({@code reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:415}
 * for the golem, {@code :457} for the eldritch golem, {@code :411} for the
 * bobber). Card TC-M6-08-s-c2.
 *
 * <p>1.7.10 registers a renderer per entity type inside the proxy; 1.21.1
 * splits that into the two mod-bus events handled here:</p>
 * <ul>
 *   <li>{@link EntityRenderersEvent.RegisterLayerDefinitions} — all four model
 *       layers the golem renderers bake (body, damage pass, decoration,
 *       eldritch), in the reference's own order.</li>
 *   <li>{@link EntityRenderersEvent.RegisterRenderers} — the three renderers
 *       this card owns, in the proxy's own order: {@link RenderGolemBase} for
 *       {@code thaumcraft:golem}, {@link RenderEldritchGolem} for
 *       {@code thaumcraft:eldritchgolem} and {@link RenderGolemBobber} for
 *       {@code thaumcraft:golembobber}.</li>
 * </ul>
 *
 * <p><b>Why each pairing is legal.</b> 1.21.1 binds a renderer to the entity
 * type its type parameter names, so the two golem renderers pair with
 * {@code EntityType<EntityGolemBase>} and {@code EntityType<EntityEldritchGolem>}
 * — the eldritch golem is {@code Monster extends LivingEntity} in the port
 * ({@code entities/monster/EntityEldritchGolem.java:37}), exactly the shape the
 * reference's {@code RenderLiving} needed ({@code RenderEldritchGolem.java:16}).
 * {@code thaumcraft:golembobber} is {@code EntityGolemBobber extends Entity}
 * (no living base in either tree), so its renderer is a plain
 * {@code EntityRenderer}, and 1.7.10's screen-space {@code GL_LINE_STRIP} line
 * has no world-space twin (BD-24).</p>
 *
 * <p>Each handler has the two-line shape the wisp card (TC-M7-03-s) established:
 * the {@code @SubscribeEvent} method reads the live registry holders, and
 * {@link #registerLayers} / {@link #register} are the seam. {@code GolemRendererRegistrationTest} fires the real events through
 * the seam — with vanilla stand-in types, because a unit harness has the
 * registries frozen — and follows the call into 21.1's own provider map
 * ({@code EntityRenderers.PROVIDERS}) and layer-definition map
 * ({@code ClientHooks.layerDefinitions}), the two maps the client builds its
 * renderer table and its model set from.</p>
 *
 * <p>The parent card's two other projectile types — {@code thaumcraft:golemorb}
 * and {@code thaumcraft:dart} — are not registered here: the reference renders
 * them with {@code RenderDart} and an item renderer, classes outside this
 * card's six ({@code client/renderers/entity/RenderDart.java} is its own
 * reference file), and 1.21.1's default for an unregistered type is to render
 * nothing, which is the state the parent card shipped.</p>
 *
 * <p><b>Merge note for the integrator:</b> the unmerged {@code wt/m7-renderers}
 * branch carries a {@code ModEntityRenderers} of its own (the wisp renderer,
 * {@code register(event, CatalogEntities.WISP.get())}). This file is the same
 * class name by design — one mod has one renderer seam — so merging the two
 * branches means unioning the two {@code register} bodies into this class, not
 * keeping both files.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityRenderers {

    /** The golem body layer — the {@link ModelGolem#layer()} geometry. */
    public static final ModelLayerLocation GOLEM_LAYER = ModelGolem.LAYER;
    /** The golem damage pass — the same geometry, the {@code pass = 2} copy (:54-58). */
    public static final ModelLayerLocation GOLEM_DAMAGE_LAYER = ModelGolem.DAMAGE_LAYER;
    /** The decoration layer — the {@link ModelGolemAccessories#layer()} geometry (:120). */
    public static final ModelLayerLocation GOLEM_ACCESSORIES_LAYER = ModelGolemAccessories.LAYER;
    /** The eldritch golem layer — the {@link ModelEldritchGolem#layer()} geometry (:13). */
    public static final ModelLayerLocation ELDRITCH_GOLEM_LAYER = ModelEldritchGolem.LAYER;

    @SubscribeEvent
    public static void onRegisterLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        registerLayers(event);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        register(event, CatalogGolems.GOLEM.get(), CatalogGolems.ELDRITCH_GOLEM.get(),
                CatalogGolems.GOLEM_BOBBER.get());
    }

    /** The layer seam — all four layers, including the eldritch geometry. */
    static void registerLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GOLEM_LAYER, ModelGolem::layer);
        event.registerLayerDefinition(GOLEM_DAMAGE_LAYER, ModelGolem::layer);
        event.registerLayerDefinition(GOLEM_ACCESSORIES_LAYER, ModelGolemAccessories::layer);
        event.registerLayerDefinition(ELDRITCH_GOLEM_LAYER, ModelEldritchGolem::layer);
    }

    /**
     * The renderer seam, paired in the reference's registration order
     * ({@code ClientProxy.java:415} the golem, {@code :457} the eldritch golem,
     * {@code :411} the bobber). Each pairing is legal on the type the parent
     * card registered: {@code EntityEldritchGolem extends Monster} is a
     * {@code LivingEntity}, so the eldritch {@code LivingEntityRenderer} binds,
     * while {@code EntityGolemBobber extends Entity} is not, which is why its
     * renderer is a plain {@code EntityRenderer} (BD-24).
     */
    static void register(final EntityRenderersEvent.RegisterRenderers event,
            final EntityType<EntityGolemBase> golem, final EntityType<EntityEldritchGolem> eldritch,
            final EntityType<EntityGolemBobber> bobber) {
        // ClientProxy.java:415 — new RenderGolemBase(new ModelGolem(false))
        event.registerEntityRenderer(golem, RenderGolemBase::new);
        // ClientProxy.java:457 — new RenderEldritchGolem(new ModelEldritchGolem(), 0.5F)
        event.registerEntityRenderer(eldritch, RenderEldritchGolem::new);
        // ClientProxy.java:411 — new RenderGolemBobber()
        event.registerEntityRenderer(bobber, RenderGolemBobber::new);
    }

    private ModEntityRenderers() {
    }
}
