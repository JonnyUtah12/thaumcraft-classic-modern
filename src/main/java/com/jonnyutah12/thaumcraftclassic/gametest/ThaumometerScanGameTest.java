package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectDiscoveryPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ScannedToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncScannedPhenomenaPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ScanManager;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/**
 * THAUM-1 remainder acceptance: the thaumometer's scanType-3 phenomena/NODE
 * branch on the live server (the state-machine half of the feature; the
 * payload half already ships). Probes cover what the unit tests cannot: the
 * node record in the real {@code WorldMagicData} store resolving to the
 * original's phenomenon key, the completion writing the keyed phenomenon into
 * the live player attachment, the shared aspect tail feeding the pool with the
 * node's aspects, and the repeat-scan refusal.
 *
 * <p>Reference: {@code reference/decompiled/thaumcraft/thaumcraft/common/items/relics/ItemThaumometer.java}
 * L87-107 (the branch) and {@code common/lib/research/ScanManager.java}
 * L252-292, L328-333, L376-381, L435-441 (the gate and completion).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThaumometerScanGameTest {

    private static WorldMagicData.Node nodeAt(final WorldMagicData data, final ChunkPos chunk,
            final BlockPos pos) {
        for (final WorldMagicData.Node node : data.getNodes(chunk)) {
            if (node.x() == pos.getX() && node.y() == pos.getY() && node.z() == pos.getZ()) {
                return node;
            }
        }
        return null;
    }

    private static ScannedToServerPayload phenomenonRequest(final String phenomena, final String prefix) {
        return new ScannedToServerPayload(ScanManager.SCAN_TYPE_PHENOMENA,
                ResourceLocation.withDefaultNamespace("air"), 0, 0, phenomena, prefix);
    }

    @GameTest(template = "empty")
    public static void nodeRecordBecomesThePhenomenonKeyTheOriginalBuilds(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        final WorldMagicData data = WorldMagicData.get(level);
        data.addNode(new ChunkPos(pos), new WorldMagicData.Node(pos.getX(), pos.getY(), pos.getZ(),
                "normal", null, 10, Map.of("aer", 100, "ignis", 5)));

        final String phenomena = ScanManager.scanNode(level, pos).orElse(null);
        helper.assertTrue(phenomena != null, "a stored node must produce a phenomenon scan");
        helper.assertTrue(phenomena.startsWith(ScanManager.PHENOMENON_NODE),
                "the original's phenomenon string starts with NODE (ItemThaumometer L89), got " + phenomena);
        helper.assertTrue(phenomena.equals("NODE" + level.dimension().location() + ":"
                        + pos.getX() + ":" + pos.getY() + ":" + pos.getZ()),
                "the key must be the original's four-part TileNode id (L75), got " + phenomena);

        // The id reads back: ScanManager L436 does phenomena.replace("NODE","").
        final var parsed = ScanManager.parseNodeKey(ScanManager.stripPhenomenonPrefix(phenomena));
        helper.assertTrue(parsed.isPresent() && parsed.get().pos().equals(pos)
                        && parsed.get().dimension().equals(level.dimension().location()),
                "the node id must resolve back to its position and dimension, got " + parsed);

        // A block with no node record: the original's `tile instanceof INode`
        // test fails and doScan returns null (L88, L106).
        helper.assertTrue(ScanManager.scanNode(level, pos.above(8)).isEmpty(),
                "a block that is not a node yields no phenomenon scan");

        data.removeNode(new ChunkPos(pos), pos.getX(), pos.getY(), pos.getZ());
        helper.assertTrue(nodeAt(data, new ChunkPos(pos), pos) == null, "probe cleanup: node left behind");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nodeScanCompletionRecordsThePhenomenonAndFeedsTheAspectPool(
            final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        WorldMagicData.get(level).addNode(new ChunkPos(pos),
                new WorldMagicData.Node(pos.getX(), pos.getY(), pos.getZ(), "normal", null, 10,
                        Map.of("aer", 100, "ignis", 5)));

        final ServerPlayer player = freshPlayer(helper, "thaum-scan");
        final String phenomena = ScanManager.scanNode(level, pos).orElse("");
        helper.assertTrue(ScanManager.isValidScanTarget(
                        player.getData(ModAttachments.PLAYER_KNOWLEDGE), ScanManager.PREFIX_THAUMOMETER,
                        phenomena),
                "an unscanned node is a valid target (ScanManager L328-333)");

        helper.assertTrue(ScanManager.completeScan(phenomenonRequest(phenomena, ScanManager.PREFIX_THAUMOMETER),
                        player),
                "the completion must accept a node whose aspects the player can read (L437)");

        // L438: the phenomenon is stored as prefix + phenomena.
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(knowledge.scannedPhenomenaKeys().contains(ScanManager.PREFIX_THAUMOMETER + phenomena),
                "the keyed phenomenon must land in the player's scanned list, got "
                        + knowledge.scannedPhenomenaKeys());

        // The knowledge tail (L443-462) is the shared rule: generateNodeAspects
        // puts every container aspect in at max(4, amount/10) — AER 100 -> 10,
        // IGNIS 5 -> 4 — and a first discovery carries the +2 bonus.
        helper.assertTrue(knowledge.aspectAmount("aer") == 12 && knowledge.aspectAmount("ignis") == 6,
                "the node aspects must feed the pool at max(4, amount/10) + 2 discovery, got aer="
                        + knowledge.aspectAmount("aer") + " ignis=" + knowledge.aspectAmount("ignis"));

        final AspectList fromRecord = ScanManager.nodeAspects(level, phenomena);
        helper.assertTrue(fromRecord != null && fromRecord.getAmount(Aspect.getAspect("aer")) == 10
                        && fromRecord.getAmount(Aspect.getAspect("ignis")) == 4,
                "generateNodeAspects must floor each aspect at 4 and take a tenth above it, got "
                        + fromRecord);

        WorldMagicData.get(level).removeNode(new ChunkPos(pos), pos.getX(), pos.getY(), pos.getZ());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void repeatNodeScanIsRefusedAndForeignNodesStayUnscannable(
            final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        final BlockPos other = helper.absolutePos(new BlockPos(4, 2, 4));
        WorldMagicData.get(level).addNode(new ChunkPos(pos),
                new WorldMagicData.Node(pos.getX(), pos.getY(), pos.getZ(), "normal", null, 10,
                        Map.of("aer", 20)));
        WorldMagicData.get(level).addNode(new ChunkPos(other),
                new WorldMagicData.Node(other.getX(), other.getY(), other.getZ(), "normal", null, 10,
                        Map.of("aer", 30)));

        final ServerPlayer player = freshPlayer(helper, "thaum-rescan");
        final String first = ScanManager.scanNode(level, pos).orElse("");

        helper.assertTrue(ScanManager.completeScan(phenomenonRequest(first, ScanManager.PREFIX_THAUMOMETER),
                        player),
                "the first scan of a node completes");
        final int poolAfterFirst = player.getData(ModAttachments.PLAYER_KNOWLEDGE).aspectAmount("aer");

        // hasBeenScanned under either prefix (ScanManager L376-381) — and the
        // thaumometer gate (L328-333) refuses the same phenomenon key.
        helper.assertTrue(ScanManager.hasBeenScanned(player.getData(ModAttachments.PLAYER_KNOWLEDGE), first),
                "the node must read as already scanned");
        helper.assertTrue(!ScanManager.isValidScanTarget(player.getData(ModAttachments.PLAYER_KNOWLEDGE),
                        ScanManager.PREFIX_THAUMOMETER, first),
                "a rescan of the same node is refused");
        helper.assertTrue(!ScanManager.completeScan(phenomenonRequest(first, ScanManager.PREFIX_THAUMOMETER),
                        player),
                "the completion must refuse a repeated phenomenon");
        helper.assertTrue(player.getData(ModAttachments.PLAYER_KNOWLEDGE).aspectAmount("aer") == poolAfterFirst,
                "a refused scan must not feed the pool again");

        // A second node is a second phenomenon: it scans.
        final String second = ScanManager.scanNode(level, other).orElse("");
        helper.assertTrue(!second.equals(first), "two nodes must be two phenomena");
        helper.assertTrue(ScanManager.completeScan(phenomenonRequest(second, ScanManager.PREFIX_THAUMOMETER),
                        player),
                "a different node is a different phenomenon and completes");

        // The original's dimension gate (ScanManager L260): a key naming
        // another dimension resolves to no aspects, so the scan is refused.
        final String foreign = ScanManager.PHENOMENON_NODE + Level.NETHER.location() + ":"
                + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
        helper.assertTrue(ScanManager.nodeAspects(level, foreign) == null,
                "a node key from another dimension must resolve to no aspects");
        helper.assertTrue(!ScanManager.completeScan(phenomenonRequest(foreign, ScanManager.PREFIX_THAUMOMETER),
                        player),
                "a node in another dimension is not scannable here");

        // An empty node has no aspects: validScan refuses (L437 -> validScan L502-533).
        final BlockPos empty = helper.absolutePos(new BlockPos(5, 2, 5));
        WorldMagicData.get(level).addNode(new ChunkPos(empty),
                new WorldMagicData.Node(empty.getX(), empty.getY(), empty.getZ(), "normal", null, 0));
        final String emptyKey = ScanManager.scanNode(level, empty).orElse("");
        helper.assertTrue(!ScanManager.completeScan(phenomenonRequest(emptyKey, ScanManager.PREFIX_THAUMOMETER),
                        player),
                "an aspect-less node must not complete a scan");

        for (final BlockPos cleared : new BlockPos[] {pos, other, empty}) {
            WorldMagicData.get(level).removeNode(new ChunkPos(cleared),
                    cleared.getX(), cleared.getY(), cleared.getZ());
        }
        helper.succeed();
    }

    /**
     * A bare ServerPlayer with a dummy net handler (mirrors
     * AspectSystemGameTest); the ad-hoc channel declares the payloads a
     * completed scan sends so NeoForge's send guard accepts them.
     */
    private static ServerPlayer freshPlayer(final GameTestHelper helper, final String name) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), name + "-" + UUID.randomUUID().toString().substring(0, 8)),
                false);
        final ServerPlayer player =
                new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());

        final Channel nettyChannel = new EmbeddedChannel();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public Channel channel() {
                return nettyChannel;
            }
        };
        ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
        final var adHoc = ChannelAttributes.getOrCreateAdHocChannels(connection);
        adHoc.add(SyncAttachmentsPayload.TYPE.id());
        adHoc.add(SyncScannedPhenomenaPayload.TYPE.id());
        adHoc.add(AspectPoolPayload.TYPE.id());
        adHoc.add(AspectDiscoveryPayload.TYPE.id());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        return player;
    }
}
