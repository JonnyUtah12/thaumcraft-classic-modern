package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * TC-M3-02: game-bus wiring for research progression.
 *
 * <ul>
 *   <li>Join: original {@code EventHandlerEntity.playerLoggedIn} tail — seed primal
 *       aspect pools for a fresh player and auto-unlock the starting research set.</li>
 *   <li>Craft: original {@code EventHandlerWorld.onCrafting} warp branch
 *       ({@code ThaumcraftApi.addWarpToItem} table), plus the card's item-craft
 *       unlock trigger: crafting an item that is an item-trigger of a hidden entry
 *       runs the original {@code createClue} reveal rules against it. (The original
 *       fired {@code createClue} only from {@code ScanManager}; the craft hook is
 *       the port's trigger wiring per TC-M3-02, reusing the original matcher.)</li>
 * </ul>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class ResearchEvents {

    private ResearchEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResearchManager.onPlayerJoined(player);
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(final PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResearchManager.applyCraftingWarp(player, event.getCrafting());
            ResearchManager.createClue(player, event.getCrafting(), java.util.List.of());
        }
    }
}
