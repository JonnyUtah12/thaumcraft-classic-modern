package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.network.ClientNetworkData;
import com.jonnyutah12.thaumcraftclassic.registration.ModItems;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconBook;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconHost;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconKnowledge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The client half of the Thaumonomicon's open path (TC-M3-03-s).
 *
 * <p>{@code value = Dist.CLIENT} keeps this class off a dedicated server
 * entirely, which is the whole point: the shared
 * {@code research.book.ThaumonomiconHost} seam is installed here, so nothing in
 * the common source set has to name {@link ThaumonomiconScreen} (a
 * {@code Screen} subclass) and {@code runGameTestServer} / {@code runServer}
 * never load a client class. This mirrors the repo's existing client wiring
 * ({@code ModClientScreens}, {@code ClientNetworkEvents}).</p>
 *
 * <p>The original opened the browser from
 * {@code ItemThaumonomicon.onItemRightClick}'s {@code @SideOnly(Side.CLIENT)}
 * branch; right-clicking the registered item is therefore the port's trigger.
 * {@link PlayerInteractEvent.RightClickItem} is a game-bus event, so this
 * subscriber needs no {@code bus =} override.</p>
 *
 * <p>The book is opened with the client's synced knowledge
 * ({@link ClientNetworkData#completedResearch()}, filled from
 * {@code SyncResearchPayload}), so hidden entries stay hidden for a player who
 * has not revealed them and concealed pages stay concealed until their
 * prerequisite is known.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT)
public final class ThaumonomiconClientHooks {

    private ThaumonomiconClientHooks() {
    }

    @SubscribeEvent
    static void onRightClickItem(final PlayerInteractEvent.RightClickItem event) {
        if (!event.getItemStack().is(ModItems.THAUMONOMICON.get())) {
            return;
        }
        if (!event.getEntity().level().isClientSide()) {
            return;
        }
        // Install the display on first use: the host must exist before the
        // first openBook call, and this is the only place that can legitimately
        // name the client-only screen class.
        if (ThaumonomiconHost.installed() == null) {
            ThaumonomiconHost.install(ThaumonomiconScreen::open);
        }
        ThaumonomiconHost.openBook(ThaumonomiconBook.open(
                com.jonnyutah12.thaumcraftclassic.research.ResearchManager.graph(),
                ThaumonomiconKnowledge.of(ClientNetworkData.completedResearch())));
        event.setCanceled(true);
    }
}
