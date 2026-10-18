package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.network.ModNetwork;
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
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Network foundation gametest (TC-M2-03). Runs on the dedicated game-test
 * server, so it proves the whole payload set class-loads and registers on the
 * server dist (a registration failure would fail mod construction itself),
 * that every payload id is namespaced and unique, and that every stream codec
 * round-trips byte-exactly. Client-dist boot is covered by the client smoke
 * run in docs/testing.md.
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModNetworkGameTest {

    private ModNetworkGameTest() {
    }

    @GameTest(template = "empty")
    public static void payloadIdsAreNamespacedAndUnique(final GameTestHelper helper) {
        final List<CustomPacketPayload.Type<?>> types = ModNetwork.registeredTypes();
        helper.assertTrue(!types.isEmpty(), "no payloads were registered");

        final Set<ResourceLocation> seen = new HashSet<>();
        for (CustomPacketPayload.Type<?> type : types) {
            helper.assertTrue(ThaumcraftClassic.MOD_ID.equals(type.id().getNamespace()),
                    "payload id not namespaced to the mod: " + type.id());
            helper.assertTrue(seen.add(type.id()), "duplicate payload id: " + type.id());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void payloadCodecsRoundTrip(final GameTestHelper helper) {
        roundTrip(helper, new SyncWipePayload(), SyncWipePayload.STREAM_CODEC);
        roundTrip(helper, new SyncResearchPayload(List.of("BASICS", "INFUSION", "@CLUE1")),
                SyncResearchPayload.STREAM_CODEC);
        roundTrip(helper, new SyncScannedItemsPayload(List.of("minecraft:stone")),
                SyncScannedItemsPayload.STREAM_CODEC);
        roundTrip(helper, new SyncScannedEntitiesPayload(List.of("minecraft:cow")),
                SyncScannedEntitiesPayload.STREAM_CODEC);
        roundTrip(helper, new SyncScannedPhenomenaPayload(List.of("REMAK")),
                SyncScannedPhenomenaPayload.STREAM_CODEC);
        roundTrip(helper, new SyncAspectsPayload(List.of(
                new SyncAspectsPayload.AspectAmount("ignis", 1),
                new SyncAspectsPayload.AspectAmount("aer", 0))),
                SyncAspectsPayload.STREAM_CODEC);
        roundTrip(helper, new SyncWarpPayload(42, SyncWarpPayload.WARP_STICKY), SyncWarpPayload.STREAM_CODEC);
        roundTrip(helper, new WarpMessagePayload(-3, WarpMessagePayload.WARP_TEMPORARY), WarpMessagePayload.STREAM_CODEC);
        roundTrip(helper, new AspectPoolPayload("potentia", 5, 17), AspectPoolPayload.STREAM_CODEC);
        roundTrip(helper, new AspectDiscoveryPayload("ordo"), AspectDiscoveryPayload.STREAM_CODEC);
        roundTrip(helper, new ResearchCompletePayload("ALCHEMY"), ResearchCompletePayload.STREAM_CODEC);
        roundTrip(helper, new RunicChargePayload(7, 9, 10), RunicChargePayload.STREAM_CODEC);

        roundTrip(helper, new ScannedToServerPayload((byte) 1 & 0xFF, ResourceLocation.withDefaultNamespace("stone"),
                3, 12, "", "SCANNED"), ScannedToServerPayload.STREAM_CODEC);
        roundTrip(helper, new AspectCombinationToServerPayload(new BlockPos(1, 2, 3), "ignis", "aer", true, false),
                AspectCombinationToServerPayload.STREAM_CODEC);
        roundTrip(helper, new AspectPlaceToServerPayload(new BlockPos(1, 2, 3), Optional.of("ignis"), 2, 4),
                AspectPlaceToServerPayload.STREAM_CODEC);
        roundTrip(helper, new AspectPlaceToServerPayload(new BlockPos(0, 0, 0), Optional.empty(), 0, 0),
                AspectPlaceToServerPayload.STREAM_CODEC);
        roundTrip(helper, new PlayerCompleteToServerPayload("ALCHEMY", PlayerCompleteToServerPayload.REQUEST_NOTE),
                PlayerCompleteToServerPayload.STREAM_CODEC);

        roundTrip(helper, new BiomeChangePayload(10, -20, ResourceLocation.withDefaultNamespace("plains")),
                BiomeChangePayload.STREAM_CODEC);
        roundTrip(helper, new MiscEventPayload(MiscEventPayload.EVENT_MIST), MiscEventPayload.STREAM_CODEC);
        roundTrip(helper, new NotePayload(new BlockPos(4, 5, 6), NotePayload.NOTE_QUERY), NotePayload.STREAM_CODEC);
        roundTrip(helper, new NotePayload(new BlockPos(4, 5, 6), 12), NotePayload.STREAM_CODEC);
        roundTrip(helper, new BoreDigPayload(new BlockPos(7, 8, 9), 3), BoreDigPayload.STREAM_CODEC);
        roundTrip(helper, new FlyToServerPayload(true), FlyToServerPayload.STREAM_CODEC);
        roundTrip(helper, new FocusChangeToServerPayload("thaumcraft:focus_fire"), FocusChangeToServerPayload.STREAM_CODEC);
        roundTrip(helper, new ItemKeyToServerPayload(ItemKeyToServerPayload.KEY_WAND_MISC_TOGGLE),
                ItemKeyToServerPayload.STREAM_CODEC);

        roundTrip(helper, new FXBlockBubblePayload(new BlockPos(1, 2, 3), 0x00FF00), FXBlockBubblePayload.STREAM_CODEC);
        roundTrip(helper, new FXBlockSparklePayload(new BlockPos(1, 2, 3), 0x0000FF), FXBlockSparklePayload.STREAM_CODEC);
        roundTrip(helper, new FXBlockArcPayload(new BlockPos(1, 2, 3), 11), FXBlockArcPayload.STREAM_CODEC);
        roundTrip(helper, new FXBlockZapPayload(new Vec3(1.5, 2.5, 3.5), new Vec3(0.0, 1.0, 0.0)),
                FXBlockZapPayload.STREAM_CODEC);
        roundTrip(helper, new FXBlockDigPayload(new BlockPos(1, 2, 3), ResourceLocation.withDefaultNamespace("stone"),
                2, new BlockPos(1, 1, 3)), FXBlockDigPayload.STREAM_CODEC);
        roundTrip(helper, new FXEssentiaSourcePayload(new BlockPos(1, 2, 3), new BlockPos(0, 1, 0), 0xFF0000),
                FXEssentiaSourcePayload.STREAM_CODEC);
        roundTrip(helper, new FXInfusionSourcePayload(new BlockPos(1, 2, 3), new BlockPos(0, 1, 0), 0xFF0000),
                FXInfusionSourcePayload.STREAM_CODEC);
        roundTrip(helper, new FXShieldPayload(11, 12), FXShieldPayload.STREAM_CODEC);
        roundTrip(helper, new FXSonicPayload(11), FXSonicPayload.STREAM_CODEC);
        roundTrip(helper, new FXVisDrainPayload(new BlockPos(1, 2, 3), new BlockPos(0, 1, 0), 0x00FFFF),
                FXVisDrainPayload.STREAM_CODEC);
        roundTrip(helper, new FXWispZapPayload(11, 12), FXWispZapPayload.STREAM_CODEC);
        roundTrip(helper, new FXZapPayload(11, 12), FXZapPayload.STREAM_CODEC);
        roundTrip(helper, new FXBeamPulsePayload(11, 12, 0x123456), FXBeamPulsePayload.STREAM_CODEC);
        roundTrip(helper, new FXBeamPulseGolemBossPayload(11, 12), FXBeamPulseGolemBossPayload.STREAM_CODEC);

        helper.succeed();
    }

    private static <T extends CustomPacketPayload> void roundTrip(
            final GameTestHelper helper, final T sample, final StreamCodec<io.netty.buffer.ByteBuf, T> codec) {
        final var buffer = Unpooled.buffer();
        try {
            codec.encode(buffer, sample);
            final T decoded = codec.decode(buffer);
            helper.assertTrue(sample.equals(decoded),
                    "round-trip mismatch for " + sample.type().id() + ": " + sample + " != " + decoded);
            helper.assertTrue(buffer.readableBytes() == 0,
                    "trailing bytes for " + sample.type().id());
        } finally {
            buffer.release();
        }
    }
}
