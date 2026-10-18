package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.network.payload.AspectCombinationToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPlaceToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.FlyToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.FocusChangeToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ItemKeyToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.NotePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.PlayerCompleteToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ScannedToServerPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server handlers for every C2S payload. Each method runs on the server game
 * thread (the {@code enqueueWork} hop in {@link ModNetwork}).
 *
 * <p>The original handlers validated the embedded playerid/dimension against
 * the connection; NeoForge already delivers the payload with its authenticated
 * sender via {@link IPayloadContext#player()}, so that whole class of spoofing
 * checks is gone — the sender here is authoritative.</p>
 */
final class ServerPayloadHandlers {

    private ServerPayloadHandlers() {
    }

    // --- player knowledge actions ---

    static void handleScanned(final ScannedToServerPayload payload, final IPayloadContext context) {
        ServerNetworkHooks.completeScan(payload, context.player());
    }

    static void handleAspectCombination(final AspectCombinationToServerPayload payload, final IPayloadContext context) {
        ServerNetworkHooks.combineAspects(payload, context.player());
    }

    static void handleAspectPlace(final AspectPlaceToServerPayload payload, final IPayloadContext context) {
        ServerNetworkHooks.placeAspect(payload, context.player());
    }

    static void handlePlayerComplete(final PlayerCompleteToServerPayload payload, final IPayloadContext context) {
        ServerNetworkHooks.completeResearchRequest(payload, context.player());
    }

    // --- misc actions ---

    static void handleFly(final FlyToServerPayload payload, final IPayloadContext context) {
        ServerNetworkHooks.setHover(payload.hover(), context.player());
    }

    static void handleFocusChange(final FocusChangeToServerPayload payload, final IPayloadContext context) {
        ServerNetworkHooks.changeFocus(payload.focus(), context.player());
    }

    static void handleItemKey(final ItemKeyToServerPayload payload, final IPayloadContext context) {
        ServerNetworkHooks.itemKey(payload.key(), context.player());
    }

    static void handleNoteQuery(final NotePayload payload, final Player player) {
        ServerNetworkHooks.answerNoteQuery(payload, player);
    }
}
