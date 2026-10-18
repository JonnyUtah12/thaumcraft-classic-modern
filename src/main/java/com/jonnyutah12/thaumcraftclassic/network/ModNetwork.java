package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.network.payload.AspectCombinationToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectDiscoveryPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPlaceToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.BiomeChangePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.BoreDigPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.FlyToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.FocusChangeToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ItemKeyToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.MiscEventPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.NotePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.PlayerCompleteToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ResearchCompletePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.RunicChargePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ScannedToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncAspectsPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncResearchPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncScannedEntitiesPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncScannedItemsPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncScannedPhenomenaPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWipePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBeamPulseGolemBossPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBeamPulsePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBlockArcPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBlockBubblePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBlockDigPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBlockSparklePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBlockZapPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXEssentiaSourcePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXInfusionSourcePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXShieldPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXSonicPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXVisDrainPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXWispZapPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXZapPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Networking foundation: registers every payload on both dists via
 * {@link RegisterPayloadHandlersEvent} (the NeoForge replacement for the
 * original {@code thaumcraft.common.lib.network.PacketHandler}
 * SimpleNetworkWrapper).
 *
 * <p>Every payload is a 1:1 port of an original message class; the full
 * inventory and the three classes that intentionally have no payload are
 * documented in docs/architecture/networking.md.</p>
 *
 * <p>Dist safety: registration runs identically on both dists (required for
 * the channel to match on handshake). S2C handlers only ever execute on the
 * client and C2S handlers only on the server, because a payload is only
 * received on the side it is addressed to; each handler additionally hops onto
 * the game thread with {@link IPayloadContext#enqueueWork(Runnable)} before
 * touching game state, mirroring the original {@code @SideOnly(Side.CLIENT)}
 * handlers.</p>
 */
public final class ModNetwork {

    /**
     * Channel protocol version. Bump on any breaking change to a payload
     * codec; NeoForge refuses connections whose versions differ.
     */
    public static final String CHANNEL_VERSION = "1";

    /** Populated during {@link #register}; consumed by the network gametest. */
    private static final List<CustomPacketPayload.Type<?>> REGISTERED_TYPES = new ArrayList<>();

    private ModNetwork() {
    }

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(CHANNEL_VERSION);

        // --- player knowledge sync (S2C) — original common/lib/network/playerdata ---
        toClient(registrar, SyncWipePayload.TYPE, SyncWipePayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncWipe);
        toClient(registrar, SyncResearchPayload.TYPE, SyncResearchPayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncResearch);
        toClient(registrar, SyncScannedItemsPayload.TYPE, SyncScannedItemsPayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncScannedItems);
        toClient(registrar, SyncScannedEntitiesPayload.TYPE, SyncScannedEntitiesPayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncScannedEntities);
        toClient(registrar, SyncScannedPhenomenaPayload.TYPE, SyncScannedPhenomenaPayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncScannedPhenomena);
        toClient(registrar, SyncAspectsPayload.TYPE, SyncAspectsPayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncAspects);
        toClient(registrar, SyncWarpPayload.TYPE, SyncWarpPayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncWarp);
        toClient(registrar, WarpMessagePayload.TYPE, WarpMessagePayload.STREAM_CODEC, ClientPayloadHandlers::handleWarpMessage);
        toClient(registrar, AspectPoolPayload.TYPE, AspectPoolPayload.STREAM_CODEC, ClientPayloadHandlers::handleAspectPool);
        toClient(registrar, AspectDiscoveryPayload.TYPE, AspectDiscoveryPayload.STREAM_CODEC, ClientPayloadHandlers::handleAspectDiscovery);
        toClient(registrar, ResearchCompletePayload.TYPE, ResearchCompletePayload.STREAM_CODEC, ClientPayloadHandlers::handleResearchComplete);
        toClient(registrar, RunicChargePayload.TYPE, RunicChargePayload.STREAM_CODEC, ClientPayloadHandlers::handleRunicCharge);

        // --- client-initiated actions (C2S) — original ...ToServer classes ---
        toServer(registrar, ScannedToServerPayload.TYPE, ScannedToServerPayload.STREAM_CODEC, ServerPayloadHandlers::handleScanned);
        toServer(registrar, AspectCombinationToServerPayload.TYPE, AspectCombinationToServerPayload.STREAM_CODEC, ServerPayloadHandlers::handleAspectCombination);
        toServer(registrar, AspectPlaceToServerPayload.TYPE, AspectPlaceToServerPayload.STREAM_CODEC, ServerPayloadHandlers::handleAspectPlace);
        toServer(registrar, PlayerCompleteToServerPayload.TYPE, PlayerCompleteToServerPayload.STREAM_CODEC, ServerPayloadHandlers::handlePlayerComplete);

        // --- misc — original common/lib/network/misc ---
        toClient(registrar, BiomeChangePayload.TYPE, BiomeChangePayload.STREAM_CODEC, ClientPayloadHandlers::handleBiomeChange);
        toClient(registrar, MiscEventPayload.TYPE, MiscEventPayload.STREAM_CODEC, ClientPayloadHandlers::handleMiscEvent);
        toClient(registrar, BoreDigPayload.TYPE, BoreDigPayload.STREAM_CODEC, ClientPayloadHandlers::handleBoreDig);
        toServer(registrar, FlyToServerPayload.TYPE, FlyToServerPayload.STREAM_CODEC, ServerPayloadHandlers::handleFly);
        toServer(registrar, FocusChangeToServerPayload.TYPE, FocusChangeToServerPayload.STREAM_CODEC, ServerPayloadHandlers::handleFocusChange);
        toServer(registrar, ItemKeyToServerPayload.TYPE, ItemKeyToServerPayload.STREAM_CODEC, ServerPayloadHandlers::handleItemKey);

        // PacketNote was one class registered on both sides with a -1 query
        // sentinel; the same shape is kept with a bidirectional registration.
        REGISTERED_TYPES.add(NotePayload.TYPE);
        registrar.playBidirectional(NotePayload.TYPE, NotePayload.STREAM_CODEC, (payload, context) -> {
            if (context.flow() == PacketFlow.CLIENTBOUND) {
                context.enqueueWork(() -> ClientPayloadHandlers.handleNote(payload));
            } else {
                context.enqueueWork(() -> ServerPayloadHandlers.handleNoteQuery(payload, context.player()));
            }
        });

        // --- visual effects (S2C) — original common/lib/network/fx ---
        toClient(registrar, FXBlockBubblePayload.TYPE, FXBlockBubblePayload.STREAM_CODEC, ClientPayloadHandlers::handleFxBlockBubble);
        toClient(registrar, FXBlockSparklePayload.TYPE, FXBlockSparklePayload.STREAM_CODEC, ClientPayloadHandlers::handleFxBlockSparkle);
        toClient(registrar, FXBlockArcPayload.TYPE, FXBlockArcPayload.STREAM_CODEC, ClientPayloadHandlers::handleFxBlockArc);
        toClient(registrar, FXBlockZapPayload.TYPE, FXBlockZapPayload.STREAM_CODEC, ClientPayloadHandlers::handleFxBlockZap);
        toClient(registrar, FXBlockDigPayload.TYPE, FXBlockDigPayload.STREAM_CODEC, ClientPayloadHandlers::handleFxBlockDig);
        toClient(registrar, FXEssentiaSourcePayload.TYPE, FXEssentiaSourcePayload.STREAM_CODEC, ClientPayloadHandlers::handleFxEssentiaSource);
        toClient(registrar, FXInfusionSourcePayload.TYPE, FXInfusionSourcePayload.STREAM_CODEC, ClientPayloadHandlers::handleFxInfusionSource);
        toClient(registrar, FXShieldPayload.TYPE, FXShieldPayload.STREAM_CODEC, ClientPayloadHandlers::handleFxShield);
        toClient(registrar, FXSonicPayload.TYPE, FXSonicPayload.STREAM_CODEC, ClientPayloadHandlers::handleFxSonic);
        toClient(registrar, FXVisDrainPayload.TYPE, FXVisDrainPayload.STREAM_CODEC, ClientPayloadHandlers::handleFxVisDrain);
        toClient(registrar, FXWispZapPayload.TYPE, FXWispZapPayload.STREAM_CODEC, ClientPayloadHandlers::handleFxWispZap);
        toClient(registrar, FXZapPayload.TYPE, FXZapPayload.STREAM_CODEC, ClientPayloadHandlers::handleFxZap);
        toClient(registrar, FXBeamPulsePayload.TYPE, FXBeamPulsePayload.STREAM_CODEC, ClientPayloadHandlers::handleFxBeamPulse);
        toClient(registrar, FXBeamPulseGolemBossPayload.TYPE, FXBeamPulseGolemBossPayload.STREAM_CODEC, ClientPayloadHandlers::handleFxBeamPulseGolemBoss);
    }

    private static <T extends CustomPacketPayload> void toClient(
            final PayloadRegistrar registrar,
            final CustomPacketPayload.Type<T> type,
            final StreamCodec<ByteBuf, T> codec,
            final Consumer<T> handler) {
        REGISTERED_TYPES.add(type);
        registrar.playToClient(type, codec, (payload, context) -> context.enqueueWork(() -> handler.accept(payload)));
    }

    private static <T extends CustomPacketPayload> void toServer(
            final PayloadRegistrar registrar,
            final CustomPacketPayload.Type<T> type,
            final StreamCodec<ByteBuf, T> codec,
            final BiConsumer<T, IPayloadContext> handler) {
        REGISTERED_TYPES.add(type);
        registrar.playToServer(type, codec, (payload, context) -> context.enqueueWork(() -> handler.accept(payload, context)));
    }

    /** The payload types registered by this mod, in registration order. */
    public static List<CustomPacketPayload.Type<?>> registeredTypes() {
        return Collections.unmodifiableList(REGISTERED_TYPES);
    }
}
