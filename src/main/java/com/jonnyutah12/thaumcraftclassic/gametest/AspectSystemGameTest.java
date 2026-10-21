package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectCombinations;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectKnowledge;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTags;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectDiscoveryPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ResearchCompletePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
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
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

import java.util.UUID;

/**
 * TC-M4-01 acceptance: the aspect system against the live server — the
 * {@code thaumcraft:aspect} registry, the data-driven essentia tables, the
 * original discovery rules on a real player attachment, and the
 * registry-backed combination/reduction rules (design record:
 * docs/aspects.md).
 *
 * <ul>
 *   <li>{@link #aspectRegistryCarriesTheOriginalSet}: 48 registered aspects
 *       (6 primal / 42 compound), original values on the registry objects,
 *       and {@code getCombinationResult} identity semantics.</li>
 *   <li>{@link #essentiaTablesLoadAndResolveInGame}: the shipped tables load
 *       with the original entry counts and resolve item/entity lookups
 *       (stone → TERRA 2, cow → BEAST 3 + EARTH 3, player → MAN 4 + three
 *       seeded aspects).</li>
 *   <li>{@link #discoveryFlowMatchesOriginalRules}: first discovery +2,
 *       repeat gains, the validScan parent gate, thaumometer discovery-only
 *       scans, and the registry-prefix +1 — all on the real
 *       {@code PLAYER_KNOWLEDGE} attachment with the discovery/pool packets
 *       accepted by the connection.</li>
 *   <li>{@link #reduceAndCullUseTheRegisteredObjects}: reduceToPrimals and
 *       cullTags walk the live registry with the original arithmetic.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AspectSystemGameTest {

    private AspectSystemGameTest() {
    }

    @GameTest(template = "empty")
    public static void aspectRegistryCarriesTheOriginalSet(final GameTestHelper helper) {
        helper.assertTrue(Aspect.values().size() == 48,
                "the aspect registry must hold exactly 48 aspects, got " + Aspect.values().size());
        helper.assertTrue(Aspect.getPrimalAspects().size() == 6
                        && Aspect.getCompoundAspects().size() == 42,
                "the primal/compound split must be 6/42");

        final Aspect ignis = Aspect.getAspect("ignis");
        helper.assertTrue(ignis != null && ignis.getColor() == 16734721
                        && "c".equals(ignis.getChatcolor()) && ignis.isPrimal(),
                "ignis must carry the original color/chatcolor/primal flag");
        final Aspect vacuos = Aspect.getAspect("vacuos");
        helper.assertTrue(vacuos != null && vacuos.getBlend() == 771
                        && vacuos.getComponents().size() == 2
                        && vacuos.getComponents().get(0) == Aspect.getAspect("aer")
                        && vacuos.getComponents().get(1) == Aspect.getAspect("perditio"),
                "vacuos must carry blend 771 and identity-stable AIR+ENTROPY components");
        helper.assertTrue(Aspect.getAspect("no_such_aspect") == null,
                "unknown tags resolve to null, as in the original");

        // Original ResearchManager.getCombinationResult: identity match on the
        // declared component pair, either order; everything else is null.
        final Aspect terra = Aspect.getAspect("terra");
        final Aspect ordo = Aspect.getAspect("ordo");
        final Aspect vitreus = Aspect.getAspect("vitreus");
        helper.assertTrue(AspectCombinations.getCombinationResult(terra, ordo) == vitreus,
                "TERRA + ORDER must combine to CRYSTAL");
        helper.assertTrue(AspectCombinations.getCombinationResult(ordo, terra) == vitreus,
                "combination is order-independent");
        helper.assertTrue(AspectCombinations.getCombinationResult(ignis, ignis) == null,
                "undeclared pairs have no result");
        helper.assertTrue(AspectCombinations.getCombinationResult(vitreus, terra)
                        == Aspect.getAspect("metallum"),
                "CRYSTAL + EARTH must combine to METAL");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void essentiaTablesLoadAndResolveInGame(final GameTestHelper helper) {
        EssentiaTags.ensureLoaded();
        helper.assertTrue(EssentiaTags.isLoaded(), "the essentia tables must load");
        helper.assertTrue(EssentiaTags.itemTagCount() == 287
                        && EssentiaTags.oredictTagCount() == 56
                        && EssentiaTags.entityTagCount() == 116,
                "shipped table counts differ from the original ConfigAspects extraction: "
                        + EssentiaTags.itemTagCount() + "/" + EssentiaTags.oredictTagCount()
                        + "/" + EssentiaTags.entityTagCount());

        // The original registers stone by ore-dict name ("stone" -> TERRA 2).
        // 1.21.1 has no ore-dictionary: the entry is kept as data for a
        // future compat layer and the item lookup must NOT resolve it
        // (documented seam, docs/aspects.md).
        final var stoneOredict = EssentiaTags.oredictTags("stone");
        helper.assertTrue(stoneOredict.size() == 1
                        && stoneOredict.get(0).aspects().get("terra") == 2,
                "the ore-dict table must hold stone -> TERRA 2");
        final AspectList stoneItem = EssentiaTags.getObjectTags(
                new ItemStack(Items.STONE));
        helper.assertTrue(stoneItem == null || stoneItem.size() == 0,
                "ore-dict entries are not consulted by the item lookup yet");

        // Wildcard-meta fallback: dirt is registered as meta 32767 (TERRA 2).
        final AspectList dirt = EssentiaTags.getObjectTags(
                new ItemStack(Items.DIRT));
        helper.assertTrue(dirt != null && dirt.getAmount(Aspect.getAspect("terra")) == 2,
                "dirt must resolve through the wildcard meta to TERRA 2");

        // ScanManager.generateEntityAspects: the entity table...
        final AspectList cow = EssentiaTags.getEntityAspects(
                new Cow(net.minecraft.world.entity.EntityType.COW, helper.getLevel()));
        helper.assertTrue(cow != null && cow.getAmount(Aspect.getAspect("bestia")) == 3
                        && cow.getAmount(Aspect.getAspect("terra")) == 3,
                "a cow must scan as BEAST 3 + EARTH 3");

        // ...and the player special case: MAN 4 plus three aspects seeded from
        // the name hash (total vis 16).
        final ServerPlayer player = freshPlayer(helper);
        final AspectList self = EssentiaTags.getEntityAspects(player);
        helper.assertTrue(self != null && self.getAmount(Aspect.getAspect("humanus")) == 4
                        && self.visSize() == 16,
                "a player must scan as MAN 4 + three seeded aspects (vis 16), got " + self);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void discoveryFlowMatchesOriginalRules(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final Aspect ignis = Aspect.getAspect("ignis");
        final Aspect lux = Aspect.getAspect("lux");
        final Aspect aer = Aspect.getAspect("aer");

        // First discovery: +2 bonus, discovery + pool packets, pool updated.
        final int first = AspectKnowledge.checkAndSyncAspectKnowledge(player, ignis, 5);
        helper.assertTrue(first == 7, "first discovery must save 5 + 2, got " + first);
        PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(knowledge.hasAspectDiscovered("ignis") && knowledge.aspectAmount("ignis") == 7,
                "the attachment must hold the discovery and the pool of 7");

        // Repeat gain: no bonus, plain add.
        helper.assertTrue(AspectKnowledge.checkAndSyncAspectKnowledge(player, ignis, 5) == 5,
                "repeat gains carry no bonus");
        knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(knowledge.aspectAmount("ignis") == 12, "the pool must accumulate to 12");

        // validScan gate: a compound scans only when both parents are known.
        final AspectList compoundScan = new AspectList().add(lux, 1);
        helper.assertTrue(!AspectKnowledge.validScan(compoundScan, knowledge),
                "LUX must not scan while AER is undiscovered");
        helper.assertTrue(!AspectKnowledge.validScan(new AspectList(), knowledge)
                        && !AspectKnowledge.validScan(null, knowledge),
                "empty scans are never valid");
        player.setData(ModAttachments.PLAYER_KNOWLEDGE,
                knowledge.withAspectDiscovered("aer", 1));
        knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(AspectKnowledge.validScan(compoundScan, knowledge),
                "LUX scans once both parents are discovered");

        // completeScan knowledge tail: amount flows through the discovery
        // rules; the thaumometer zeroes the amount (discovery only) and the
        // registry prefix adds +1.
        final AspectList gained = AspectKnowledge.applyScanKnowledge(player,
                new AspectList().add(lux, 5), false, false);
        helper.assertTrue(gained.getAmount(lux) == 7,
                "a first LUX scan must gain 5 + 2, got " + gained.getAmount(lux));
        helper.assertTrue(player.getData(ModAttachments.PLAYER_KNOWLEDGE).aspectAmount("lux") == 7,
                "the LUX pool must be 7");

        final ServerPlayer probe = freshPlayer(helper);
        final AspectList probeGained = AspectKnowledge.applyScanKnowledge(probe,
                new AspectList().add(ignis, 5), true, false);
        helper.assertTrue(probeGained.getAmount(ignis) == 2
                        && probe.getData(ModAttachments.PLAYER_KNOWLEDGE).aspectAmount("ignis") == 2,
                "a thaumometer scan discovers without vis: pool must be the +2 only");

        final ServerPlayer registered = freshPlayer(helper);
        final AspectList prefixGained = AspectKnowledge.applyScanKnowledge(registered,
                new AspectList().add(aer, 5), false, true);
        helper.assertTrue(prefixGained.getAmount(aer) == 8,
                "the registry prefix adds +1 before the discovery bonus: expected 8, got "
                        + prefixGained.getAmount(aer));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void reduceAndCullUseTheRegisteredObjects(final GameTestHelper helper) {
        final Aspect terra = Aspect.getAspect("terra");
        final Aspect ordo = Aspect.getAspect("ordo");
        final Aspect metallum = Aspect.getAspect("metallum");

        // Original ResearchManager.reduceToPrimals: METAL -> EARTH + CRYSTAL,
        // CRYSTAL -> EARTH + ORDER => EARTH 2 + ORDER 1.
        final AspectList reduced = AspectCombinations.reduceToPrimals(
                new AspectList().add(metallum, 1));
        helper.assertTrue(reduced.getAmount(terra) == 2 && reduced.getAmount(ordo) == 1
                        && reduced.size() == 2,
                "reduceToPrimals(METAL) must be EARTH 2 + ORDER 1, got " + reduced);

        // Original ThaumcraftApiHelper.cullTags: trim to 6 by the weighted
        // value; the two pure primals go first (x0.9).
        final AspectList many = new AspectList()
                .add(Aspect.getAspect("aer"), 10).add(Aspect.getAspect("ignis"), 10)
                .add(Aspect.getAspect("lux"), 10).add(metallum, 10)
                .add(Aspect.getAspect("potentia"), 10).add(terra, 10)
                .add(ordo, 10).add(Aspect.getAspect("vitreus"), 10);
        final AspectList culled = AspectCombinations.cullTags(many);
        helper.assertTrue(culled.size() == 6 && culled.getAmount(terra) == 10
                        && culled.getAmount(Aspect.getAspect("lux")) == 10
                        && culled.getAmount(Aspect.getAspect("aer")) == 0
                        && culled.getAmount(Aspect.getAspect("ignis")) == 0,
                "cullTags must drop the two lowest-weighted aspects, got " + culled);
        helper.succeed();
    }

    /**
     * A bare ServerPlayer with a dummy net handler (mirrors
     * ResearchProgressionGameTest); the ad-hoc channel declares the payloads
     * this milestone sends so NeoForge's send guard accepts them.
     */
    private static ServerPlayer freshPlayer(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie =
                CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "tc-aspect-"
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
        adHoc.add(AspectDiscoveryPayload.TYPE.id());
        adHoc.add(SyncWarpPayload.TYPE.id());
        adHoc.add(WarpMessagePayload.TYPE.id());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        return player;
    }
}
