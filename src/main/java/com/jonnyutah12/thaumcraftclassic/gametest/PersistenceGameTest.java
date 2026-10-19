package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.player.PlayerWarpData;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TC-M2-02 acceptance: the persistence substrate survives the real save/load paths.
 *
 * <ul>
 *   <li>{@link #worldMagicDataSurvivesSaveAndReload}: write → {@code SavedData.save}
 *       flush (the exact write the level-save pass performs) into a private temp
 *       directory → fresh {@code DimensionDataStorage} over it (the exact
 *       unload→reload read path) → every field intact.</li>
 *   <li>{@link #playerDataSurvivesSaveAndLoad}: attachments ride in the vanilla
 *       player NBT; serialize → deserialize reproduces them (the disconnect→
 *       reconnect path).</li>
 *   <li>{@link #playerAttachmentsCopyOnDeath}: {@code restoreFrom(keepInventory=false)}
 *       fires {@code PlayerEvent.Clone(wasDeath=true)}, which copies only
 *       {@code copyOnDeath} attachments — both of ours must survive.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PersistenceGameTest {

    private PersistenceGameTest() {
    }

    @GameTest(template = "empty")
    public static void worldMagicDataSurvivesSaveAndReload(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final WorldMagicData data = WorldMagicData.get(level);

        // The gametest server reuses run/world across invocations, so key off a chunk
        // position derived from this run's randomized test origin: no stale data from
        // earlier runs can exist there, and no other test touches it.
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        final ChunkPos pos = new ChunkPos((origin.getX() >> 4) + 16, (origin.getZ() >> 4) + 16);
        data.setMagicGenDone(pos, true);
        data.setAura(pos, 120, 40);
        data.setAuraAspect(pos, "auram", 7);
        data.setFlux(pos, 11, 22);
        data.setTaint(pos, 5);
        data.addNode(pos, new WorldMagicData.Node(48, 64, -32, "infernal", 3, Map.of("ignis", 2)));
        data.setWorldValue("auraCap", 99);

        // Flush exactly as the level-save pass does (DimensionDataStorage.save invokes
        // SavedData.save over a <name>.dat file), then reload through the vanilla
        // load path (DimensionDataStorage.computeIfAbsent reads it back). The round
        // trip goes through a private temp directory: run/world is shared with other
        // sessions (a live runClient autosaves into it) and persists across runs, so
        // the shared folder is a collision source, not a reliable test fixture.
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("tc-persistence");
            // Same serializer and same on-disk envelope as the level-save pass
            // (SavedData.save(File, Provider) wraps the payload under "data" and
            // stamps DataVersion). Written synchronously here because NeoForge
            // patches that method to write on an async worker thread, which
            // races the immediate read-back below.
            final CompoundTag root = new CompoundTag();
            root.put("data", data.save(new CompoundTag(), level.registryAccess()));
            NbtUtils.addCurrentDataVersion(root);
            NbtIo.writeCompressed(root, tempDir.resolve(WorldMagicData.NAME + ".dat"));

            final MinecraftServer server = level.getServer();
            final DimensionDataStorage reloaded = new DimensionDataStorage(
                    tempDir.toFile(), server.getFixerUpper(), server.registryAccess());
            final WorldMagicData r = reloaded.computeIfAbsent(WorldMagicData.FACTORY, WorldMagicData.NAME);
            if (r == null) {
                helper.fail("world magic data did not reload from disk");
                return;
            }

            helper.assertTrue(r.getVersion() == WorldMagicData.CURRENT_VERSION, "schema version not persisted");
            helper.assertTrue(r.isMagicGenDone(pos), "magic-gen chunk marker not persisted");
            helper.assertTrue(r.getAuraDensity(pos) == 120 && r.getAuraPurity(pos) == 40, "aura values not persisted");
            helper.assertTrue(r.getAuraAspects(pos).equals(Map.of("auram", 7)), "aura aspects not persisted");
            helper.assertTrue(r.getFluxAir(pos) == 11 && r.getFluxGround(pos) == 22, "flux values not persisted");
            helper.assertTrue(r.getTaint(pos) == 5, "taint value not persisted");
            final List<WorldMagicData.Node> nodes = r.getNodes(pos);
            helper.assertTrue(nodes.size() == 1, "node count wrong after reload: " + nodes.size());
            if (!nodes.isEmpty()) {
                final WorldMagicData.Node node = nodes.get(0);
                helper.assertTrue(node.x() == 48 && node.y() == 64 && node.z() == -32
                                && node.type().equals("infernal") && node.intensity() == 3
                                && node.aspects().equals(Map.of("ignis", 2)),
                        "node record not persisted exactly");
            }
            helper.assertTrue(r.getWorldValue("auraCap") == 99, "world-level value not persisted");
            helper.succeed();
        } catch (IOException e) {
            helper.fail("could not create/write temp data dir: " + e);
        } finally {
            if (tempDir != null) {
                try {
                    Files.deleteIfExists(tempDir.resolve(WorldMagicData.NAME + ".dat"));
                    Files.deleteIfExists(tempDir);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            }
        }
    }

    @GameTest(template = "empty")
    public static void playerDataSurvivesSaveAndLoad(final GameTestHelper helper) {
        final PlayerKnowledgeData knowledge = sampleKnowledge();
        final PlayerWarpData warp = sampleWarp();

        final Player original = helper.makeMockPlayer(GameType.SURVIVAL);
        original.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
        original.setData(ModAttachments.PLAYER_WARP, warp);

        // The disconnect→reconnect path: the server writes the player to NBT on
        // save and reads it back on the next login; attachments live inside it.
        final CompoundTag saved = original.saveWithoutId(new CompoundTag());
        final Player reconnected = helper.makeMockPlayer(GameType.SURVIVAL);
        reconnected.load(saved);

        helper.assertTrue(reconnected.getData(ModAttachments.PLAYER_KNOWLEDGE).equals(knowledge),
                "player knowledge lost across save/load");
        helper.assertTrue(reconnected.getData(ModAttachments.PLAYER_WARP).equals(warp),
                "player warp lost across save/load");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void playerAttachmentsCopyOnDeath(final GameTestHelper helper) {
        final PlayerKnowledgeData knowledge = sampleKnowledge();
        final PlayerWarpData warp = sampleWarp();

        final ServerLevel level = helper.getLevel();
        final MinecraftServer server = level.getServer();

        final CommonListenerCookie originalCookie =
                CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "tc-cod-original"), false);
        final ServerPlayer original =
                new ServerPlayer(server, level, originalCookie.gameProfile(), originalCookie.clientInformation());
        // Real respawns always have a connection before restoreFrom (PlayerList.respawn
        // assigns it first), and NeoForge's attachment sync sends via player.connection
        // (AttachmentSync adds the player itself to its own sync targets). Give both
        // players a dummy net handler so setData exercises the real S2C sync path.
        attachDummyConnection(server, original, originalCookie);
        original.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
        original.setData(ModAttachments.PLAYER_WARP, warp);

        final CommonListenerCookie respawnCookie =
                CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "tc-cod-respawn"), false);
        final ServerPlayer respawned =
                new ServerPlayer(server, level, respawnCookie.gameProfile(), respawnCookie.clientInformation());
        attachDummyConnection(server, respawned, respawnCookie);

        // keepInventory=false → PlayerEvent.Clone(wasDeath=true): NeoForge copies
        // only attachments flagged copyOnDeath. Both of ours are flagged.
        respawned.restoreFrom(original, false);

        helper.assertTrue(respawned.getData(ModAttachments.PLAYER_KNOWLEDGE).equals(knowledge),
                "player knowledge not copied on death");
        helper.assertTrue(respawned.getData(ModAttachments.PLAYER_WARP).equals(warp),
                "player warp not copied on death");
        helper.succeed();
    }

    /**
     * Attaches a dummy net handler to a bare ServerPlayer, mirroring the invariant
     * that real players always have a connection before setData/restoreFrom. The
     * Connection's own channel field stays null (packets are queued, never written),
     * but {@code channel()} returns an EmbeddedChannel so NeoForge's send guard
     * (NetworkRegistry.checkPacket → ChannelAttributes) can inspect channel
     * attributes, with our attachment-sync payload declared on an ad-hoc channel.
     */
    private static void attachDummyConnection(final MinecraftServer server, final ServerPlayer player,
            final CommonListenerCookie cookie) {
        final Channel nettyChannel = new EmbeddedChannel();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public Channel channel() {
                return nettyChannel;
            }
        };
        ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
        ChannelAttributes.getOrCreateAdHocChannels(connection).add(SyncAttachmentsPayload.TYPE.id());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
    }

    private static PlayerKnowledgeData sampleKnowledge() {
        return PlayerKnowledgeData.empty()
                .withResearchCompleted("THEORYcraft")
                .withAspectDiscovered("aer", 15)
                .withScannedObject("thaumcraft:stone_amber")
                .withScannedEntity("thaumcraft:Taintacle")
                .withScannedPhenomena("TC.ENTRANCE.AIR");
    }

    private static PlayerWarpData sampleWarp() {
        return PlayerWarpData.empty()
                .addWarpPerm(4)
                .addWarpTemp(2)
                .addWarpSticky(1)
                .withWarpCounter(3)
                .withRunicCharge(50);
    }
}
