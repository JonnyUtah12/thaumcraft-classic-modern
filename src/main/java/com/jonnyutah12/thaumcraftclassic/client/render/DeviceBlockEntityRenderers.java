package com.jonnyutah12.thaumcraftclassic.client.render;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillableVoid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The TER registrations of BD-18 slice 4 (t_6563ce50) — D-021's decision,
 * per scene:
 *
 * <ul>
 *   <li><b>Jar (warded + void): TER.</b> The fluid height/tint and the label
 *   depend on {@code amount}/{@code aspect}/{@code aspectFilter}/{@code facing}
 *   — continuous, per-instance tile data (a fill meter from 0..maxAmount and a
 *   tag icon from ~70 aspects); no finite blockstate or ModelData set can
 *   carry it, and 1.7.10 drew it the same way
 *   ({@code TileJarRenderer.java:47-137}).</li>
 *   <li><b>Crystal: TER.</b> Six randomly-tilted cubes seeded by the tile's
 *   coordinates ({@code TileCrystalRenderer.java:80-101}) — per-instance
 *   geometry, no model can express it.</li>
 *   <li><b>Mirror: TER.</b> The pane swap + live portal box ride
 *   {@code TileMirror.isLinkValidSimple()} and {@code instability}
 *   ({@code TileMirrorRenderer.java:575-629}); a ModelData consumer would
 *   need the link validated on every neighbor change — the tile already
 *   tracks it, so the renderer just reads it.</li>
 *   <li><b>Spa water: TER.</b> New art over the tank ({@code TileSpa}
 *   amount, 0..5000 mB continuous); the block state carries no fluid.
 *   1.7.10 drew no spa water at all ({@code BlockStoneDeviceRenderer.java:240-251}),
 *   so there is no blockstate vocabulary to inherit.</li>
 *   <li><b>Tablecloth: static model, no TER.</b> The original's
 *   {@code TileTableRenderer.java:20-32} draw is pure metadata: a fixed
 *   geometry set, tinted white, rotated 90° for the pair-placed table
 *   ({@code :27}). A shipped model + blockstate key expresses all of that
 *   with zero per-frame cost, so D-021 declines the TER there
 *   ({@code models/block/blocktable_cloth.json}); the items the original
 *   rendered on the table ({@code TileTableRenderer.java:31}, reading the
 *   tile's inventory) stay deferred — {@code TileTable} exposes no synced
 *   item view to a client. Recorded in BD-18.</li>
 *   <li><b>Armature: declined.</b> The card's
 *   {@code BlockArmourersStandRenderer} / {@code BlockEntityDummyRenderer}
 *   citations are phantoms (no such files in {@code reference/decompiled/};
 *   {@code grep -rn -i armourer reference/decompiled} is empty) and armature
 *   is not a registered block in this port, so there is no scene to route.
 *   Pinned absent by {@code DeviceRenderRegistrationTest}.</li>
 * </ul>
 *
 * <p><b>Bus:</b> {@code EntityRenderersEvent.RegisterRenderers} fires on the
 * <b>mod</b> bus — the attribute is explicit because FML's default is the
 * game bus, the seat where {@code OreBlockColors} sat silently dead
 * (t_065502ba). <b>Dist:</b> {@code CLIENT} — the renderers are client
 * classes; loading them on a dedicated server is the classloading hazard
 * {@code ModClientScreens} records, and the game-test server must never see
 * them.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DeviceBlockEntityRenderers {

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        // The fillable jars: the fluid box + aspect label plan. The void jar
        // builds the generic renderer explicitly because TileJarFillableVoid
        // (extends TileJarFillable) is that registration's exact type parameter.
        event.registerBlockEntityRenderer(CatalogEssentiaStorage.TILE_JAR.get(), DeviceWorldRenderers.JarTer::new);
        event.registerBlockEntityRenderer(CatalogEssentiaStorage.TILE_JAR_VOID.get(),
                context -> new DeviceWorldRenderers.JarTer<TileJarFillableVoid>(context));
        // The crystal cluster: seeded shard geometry (the eldritch variant
        // declines its scene inside the renderer).
        event.registerBlockEntityRenderer(CatalogDecorativeBlocks.TILE_CRYSTAL.get(), DeviceWorldRenderers.CrystalTer::new);
        // The mirrors: pane swap + portal. One renderer for both tile classes,
        // as in the original (ClientProxy.java:496-498).
        event.registerBlockEntityRenderer(CatalogFurniture.TILE_MIRROR.get(), DeviceWorldRenderers.MirrorTer::new);
        event.registerBlockEntityRenderer(CatalogFurniture.TILE_MIRROR_ESSENTIA.get(), DeviceWorldRenderers.MirrorTer::new);
        // The spa water surface.
        event.registerBlockEntityRenderer(CatalogFurniture.TILE_SPA.get(), DeviceWorldRenderers.SpaTer::new);
    }

    private DeviceBlockEntityRenderers() {
    }
}
