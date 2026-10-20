package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.PlayerCompleteToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ResearchCompletePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.player.PlayerWarpData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * TC-M3-02 acceptance: the research progression core against the real server
 * paths (design record: docs/architecture/research.md).
 *
 * <ul>
 *   <li>{@link #joinAutoUnlocksOriginalStartingResearch}: joining completes
 *       exactly the original's 22 autoUnlock entries and seeds the six primal
 *       aspect pools at 15 + rand(5).</li>
 *   <li>{@link #craftTriggerRevealsHiddenResearch}: crafting a trigger item runs
 *       the original createClue rules — BONEBOW (item triggers: bow, bone) is
 *       unlocked under its {@code @}-key, nothing else is.</li>
 *   <li>{@link #researchPurchaseSpendsAspectPool}: the Thaumonomicon completion
 *       pays the entry's tags (FIRE 3 + MAGIC 3 for FOCUSFIRE) out of the pool.</li>
 *   <li>{@link #researchWarpSplitsPermanentAndSticky}: JARBRAIN (warp 3) splits
 *       2 permanent + 1 sticky, eldritch counter to the new total.</li>
 *   <li>{@link #researchKnowledgeSurvivesDeath}: research + pool ride copyOnDeath.</li>
 *   <li>{@link #hiddenRevealRulesApplyToRealPlayers}: findHiddenResearch picks a
 *       valid hidden entry and the {@code @}-key completion persists.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchProgressionGameTest {

    /** The original starting-research set (every autoUnlock entry in ConfigResearch.java). */
    private static final Set<String> ORIGINAL_STARTING_RESEARCH = Set.of(
            "BASICTHAUMATURGY", "CAP_iron", "ROD_wood", "BASICARTIFACE", "ARCANESTONE", "GRATE",
            "TABLE", "ARCTABLE", "RESTABLE", "THAUMOMETER", "PHIAL", "CRUCIBLE", "ASPECTS", "PECH",
            "NODES", "WARP", "RESEARCH", "KNOWFRAG", "THAUMONOMICON", "ORE", "PLANTS", "ENCHANT");

    private static final List<String> PRIMAL_ASPECTS =
            List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio");

    private ResearchProgressionGameTest() {
    }

    @GameTest(template = "empty")
    public static void joinAutoUnlocksOriginalStartingResearch(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        ResearchManager.onPlayerJoined(player);

        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(ORIGINAL_STARTING_RESEARCH.equals(knowledge.researchKeys()),
                "auto-unlock set differs from the original starting research: " + knowledge.researchKeys());

        for (final String primal : PRIMAL_ASPECTS) {
            final int amount = knowledge.aspectAmount(primal);
            helper.assertTrue(amount >= 15 && amount <= 19,
                    "primal aspect pool not seeded (15..19): " + primal + "=" + amount);
        }
        helper.assertTrue(knowledge.aspectsDiscovered().size() == PRIMAL_ASPECTS.size(),
                "only the six primal aspects may be seeded on join");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void craftTriggerRevealsHiddenResearch(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);

        helper.assertTrue(ResearchManager.createClue(player, new ItemStack(Items.BOW), List.of()),
                "crafting a bow must reveal its hidden research clue");
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(knowledge.knowsResearch("@BONEBOW"), "BONEBOW clue was not unlocked");
        helper.assertTrue(!knowledge.knowsResearch("BONEBOW"),
                "clue reveals must complete the @-key, not the plain key");

        // A second clue for the same entry finds nothing (already revealed).
        helper.assertTrue(!ResearchManager.createClue(player, new ItemStack(Items.BONE), List.of()),
                "an already-revealed entry must not match again");
        // An item that triggers nothing reveals nothing.
        helper.assertTrue(!ResearchManager.createClue(player, new ItemStack(Items.COBBLESTONE), List.of()),
                "untriggered items must not reveal research");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void researchPurchaseSpendsAspectPool(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        ResearchManager.onPlayerJoined(player); // unlocks FOCUSFIRE's parent

        player.setData(ModAttachments.PLAYER_KNOWLEDGE,
                player.getData(ModAttachments.PLAYER_KNOWLEDGE)
                        .withAspectPoolAdded("ignis", 10)
                        .withAspectPoolAdded("praecantatio", 10));

        final PlayerKnowledgeData before = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        final int ignisBefore = before.aspectAmount("ignis");
        final int magicBefore = before.aspectAmount("praecantatio");

        ResearchManager.handlePlayerCompleteRequest(
                new PlayerCompleteToServerPayload("FOCUSFIRE", PlayerCompleteToServerPayload.REQUEST_COMPLETE),
                player);

        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(knowledge.knowsResearch("FOCUSFIRE"), "FOCUSFIRE was not completed");
        // FOCUSFIRE's tags are FIRE 3 + MAGIC 3 — exactly that must leave the pool.
        helper.assertTrue(knowledge.aspectAmount("ignis") == ignisBefore - 3
                        && knowledge.aspectAmount("praecantatio") == magicBefore - 3,
                "the entry's tags (FIRE 3 + MAGIC 3) must be deducted from the pool: ignis="
                        + ignisBefore + "→" + knowledge.aspectAmount("ignis") + " praecantatio="
                        + magicBefore + "→" + knowledge.aspectAmount("praecantatio"));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void researchWarpSplitsPermanentAndSticky(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);

        // JARBRAIN carries warp 3: the original splits it warp - warp/2 permanent,
        // warp/2 sticky, and the eldritch counter tracks the new total.
        helper.assertTrue(ResearchManager.completeResearch(player, "JARBRAIN"),
                "first completion must report success");
        helper.assertTrue(!ResearchManager.completeResearch(player, "JARBRAIN"),
                "a repeat completion must report false");

        final PlayerWarpData warp = player.getData(ModAttachments.PLAYER_WARP);
        helper.assertTrue(warp.warpPerm() == 2 && warp.warpSticky() == 1 && warp.warpCounter() == 3,
                "warp 3 must split into 2 permanent + 1 sticky with counter 3, got perm="
                        + warp.warpPerm() + " sticky=" + warp.warpSticky() + " counter=" + warp.warpCounter());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void researchKnowledgeSurvivesDeath(final GameTestHelper helper) {
        final ServerPlayer original = freshPlayer(helper);
        ResearchManager.onPlayerJoined(original);
        ResearchManager.handlePlayerCompleteRequest(
                new PlayerCompleteToServerPayload("FOCUSFIRE", PlayerCompleteToServerPayload.REQUEST_COMPLETE),
                original);
        original.setData(ModAttachments.PLAYER_WARP,
                original.getData(ModAttachments.PLAYER_WARP).addWarpSticky(4));

        final PlayerKnowledgeData knowledge = original.getData(ModAttachments.PLAYER_KNOWLEDGE);
        final PlayerWarpData warp = original.getData(ModAttachments.PLAYER_WARP);
        helper.assertTrue(knowledge.knowsResearch("FOCUSFIRE"), "fixture purchase failed");

        final ServerPlayer respawned = freshPlayer(helper);
        // keepInventory=false → PlayerEvent.Clone(wasDeath=true): only
        // copyOnDeath attachments carry over — both research and warp are flagged.
        respawned.restoreFrom(original, false);

        helper.assertTrue(respawned.getData(ModAttachments.PLAYER_KNOWLEDGE).equals(knowledge),
                "research knowledge (including the spent pool) must survive death");
        helper.assertTrue(respawned.getData(ModAttachments.PLAYER_WARP).equals(warp),
                "warp state must survive death");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hiddenRevealRulesApplyToRealPlayers(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final String key = ResearchManager.findHiddenResearch(
                player.getData(ModAttachments.PLAYER_KNOWLEDGE), 0L);
        helper.assertTrue(!"FAIL".equals(key), "a fresh player must have revealable hidden research");

        final var entry = ResearchManager.graph().entry(key);
        helper.assertTrue(entry != null && entry.flags().hidden(), "reveal must pick a hidden entry");
        helper.assertTrue(ResearchManager.completeResearch(player, "@" + key),
                "the @-key completion must succeed");
        helper.assertTrue(player.getData(ModAttachments.PLAYER_KNOWLEDGE).knowsResearch("@" + key),
                "the reveal must persist in the attachment");
        helper.succeed();
    }

    /**
     * A bare ServerPlayer in the test level with a dummy net handler, mirroring
     * {@code PersistenceGameTest}: real players always have a connection before
     * setData/restoreFrom, and the ad-hoc channel declares every payload this
     * milestone sends (attachment sync + research/warp/aspect-packet payloads) so
     * NeoForge's send guard accepts them.
     */
    private static ServerPlayer freshPlayer(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie =
                CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "tc-research-"
                        + UUID.randomUUID().toString().substring(0, 8)), false);
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
        adHoc.add(ResearchCompletePayload.TYPE.id());
        adHoc.add(AspectPoolPayload.TYPE.id());
        adHoc.add(SyncWarpPayload.TYPE.id());
        adHoc.add(WarpMessagePayload.TYPE.id());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        return player;
    }
}
