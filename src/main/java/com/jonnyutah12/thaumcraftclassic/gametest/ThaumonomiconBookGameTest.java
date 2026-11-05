package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemThaumonomicon;
import com.jonnyutah12.thaumcraftclassic.network.ModNetwork;
import com.jonnyutah12.thaumcraftclassic.network.payload.OpenThaumonomiconPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

import java.util.List;
import java.util.UUID;

/**
 * BOOK-1 (t_1e6b18cc): the Thaumonomicon's open path, measured on the
 * dedicated game-test server.
 *
 * <p><b>What a game test can prove here, and what it cannot.</b> The open is a
 * server-side act: the original's {@code ItemThaumonomicon.func_77659_a} body
 * sits behind {@code if (!world.field_72995_K)} ({@code ItemThaumonomicon.java:41}),
 * the research sync runs there, and {@code player.openGui(Thaumcraft.instance,
 * 12, world, 0, 0, 0)} ({@code :79}) is an {@code EntityPlayerMP} act that
 * hands the client its GuiOpen packet. All of that is server-side, so this
 * harness exercises it directly. What the harness can NEVER observe is the
 * window: a dedicated server has no client, so the screen is never
 * constructed, no key event ever reaches it, and {@code
 * RegisterMenuScreensEvent} never fires. The screen's open/close contract is
 * therefore pinned structurally in {@code ThaumonomiconOpenPathContractTest}
 * (plain JVM), and the honest statement — the pixels need the ATM10 client —
 * goes in docs/FEATURE_MATRIX.md. That is the same split
 * {@code TrunkScreenRegistrationTest} documents for the one screen pair that
 * works today.</p>
 *
 * <p><b>The decision this suite pins.</b> The browser registers no menu type:
 * the original's {@code GuiResearchBrowser} is a {@code GuiScreen}
 * ({@code GuiResearchBrowser.java:41}) with no container, so the port opens it
 * through the S2C payload {@code thaumcraft:open_thaumonomicon}
 * (docs/BEHAVIORAL_DEVIATIONS.md BD-22).
 * {@link #theBookRegistersNoMenuTypeAndTheOpenPayloadInstead} measures that
 * split on the live registries, so a later card that flips the design has to
 * rewrite this probe on purpose.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThaumonomiconBookGameTest {

    /** The gui row's id: {@code docs/content-mapping.csv} row GUI_THAUMONOMICON. */
    private static final ResourceLocation GUI_ROW_ID =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "gui_thaumonomicon");

    private ThaumonomiconBookGameTest() {
    }

    @GameTest(template = "empty")
    public static void bookItemIsRegisteredUnderTheLedgerId(final GameTestHelper helper) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath("thaumcraft", "itemthaumonomicon");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id),
                "docs/content-mapping.csv row Thaumcraft:ItemThaumonomicon (new_id " + id
                        + ") is not registered — the book has no open trigger in a world");
        final Item item = BuiltInRegistries.ITEM.get(id);
        helper.assertTrue(item instanceof ItemThaumonomicon,
                id + " must be the port's book item, got " + item.getClass().getName());
        // The label key is id-derived, which is what puts the book in the
        // creative/searchable tabs (the SPINE-2 contract).
        helper.assertTrue("item.thaumcraft.itemthaumonomicon".equals(item.getDescriptionId()),
                "the book's label key must be the id-derived item.thaumcraft.itemthaumonomicon, got "
                        + item.getDescriptionId());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void openPayloadIsRegisteredAndRoundTrips(final GameTestHelper helper) {
        boolean registered = false;
        for (final CustomPacketPayload.Type<?> type : ModNetwork.registeredTypes()) {
            if (type.id().equals(OpenThaumonomiconPayload.TYPE.id())) {
                registered = true;
                break;
            }
        }
        helper.assertTrue(registered,
                "thaumcraft:open_thaumonomicon is not registered on the channel — the book's "
                        + "server-initiated open would be dropped by NeoForge's handshake check");
        helper.assertTrue(OpenThaumonomiconPayload.TYPE.id().equals(
                        ResourceLocation.fromNamespaceAndPath("thaumcraft", "open_thaumonomicon")),
                "the open payload's id is the channel contract: " + OpenThaumonomiconPayload.TYPE.id());

        for (final String category : List.of("BASICS", OpenThaumonomiconPayload.EMPTY_CATEGORY)) {
            final ByteBuf buf = Unpooled.buffer();
            OpenThaumonomiconPayload.STREAM_CODEC.encode(buf, new OpenThaumonomiconPayload(category));
            final OpenThaumonomiconPayload back = OpenThaumonomiconPayload.STREAM_CODEC.decode(buf);
            helper.assertTrue(category.equals(back.category()),
                    "payload category did not survive the codec: sent \"" + category
                            + "\" got \"" + back.category() + "\"");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theOpenArmCompletesTheSiblingsOfCompletedResearch(final GameTestHelper helper) {
        final Pair pair = pickSiblingPair();
        if (pair == null) {
            helper.fail("the shipped graph has no entry with siblings — the research graph"
                    + " (201 entries / 6 categories) must be present for this probe to mean anything");
            return;
        }
        final ServerPlayer player = freshPlayer(helper);
        setKnowledge(player, PlayerKnowledgeData.empty().withResearchCompleted(pair.entry().key()));

        // The server arm, driven exactly as the item drives it. The payload send
        // rides the same call on a declared connection, so an unregistered or
        // mis-encoded payload throws here instead of passing silently.
        ItemThaumonomicon.openBook(player);

        final PlayerKnowledgeData after = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(after.knowsResearch(pair.entry().key()),
                "the probe's premise broke: the completed entry is gone: " + pair.entry().key());
        helper.assertTrue(after.knowsResearch(pair.sibling()),
                "the book's open arm must complete the siblings of everything completed "
                        + "(ItemThaumonomicon.java:56-64): " + pair.entry().key() + " is complete and its"
                        + " sibling " + pair.sibling() + " is not");

        // Steady state: a second open completes nothing new (the original's arm
        // only fills gaps — ItemThaumonomicon.java:60 `!isResearchComplete`).
        final int second = ItemThaumonomicon.completeSiblingsOfCompletedResearch(player);
        helper.assertTrue(second == 0,
                "a second open must complete nothing new, completed " + second + " keys");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bookStateSurvivesSaveAndReload(final GameTestHelper helper) {
        final Pair pair = pickSiblingPair();
        if (pair == null) {
            helper.fail("the shipped graph has no entry with siblings — research graph missing");
            return;
        }
        final ServerPlayer before = freshPlayer(helper);
        setKnowledge(before, PlayerKnowledgeData.empty().withResearchCompleted(pair.entry().key()));
        ItemThaumonomicon.openBook(before);

        // The disconnect -> reconnect path (the NBT round trip
        // PersistenceGameTest pins): the state the book shows lives in the
        // player's knowledge attachment, which is written into the player NBT.
        final CompoundTag saved = before.saveWithoutId(new CompoundTag());
        final ServerPlayer after = freshPlayer(helper);
        after.load(saved);

        final PlayerKnowledgeData reloaded = after.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(reloaded.knowsResearch(pair.entry().key()) && reloaded.knowsResearch(pair.sibling()),
                "the book's state did not survive save/reload: " + pair.entry().key() + " + "
                        + pair.sibling() + " -> " + reloaded.researchKeys());

        // The payload's own state survives a wire round trip — it is the only
        // state the open itself carries.
        final ByteBuf buf = Unpooled.buffer();
        final OpenThaumonomiconPayload sent = new OpenThaumonomiconPayload(pair.entry().category());
        OpenThaumonomiconPayload.STREAM_CODEC.encode(buf, sent);
        final OpenThaumonomiconPayload decoded = OpenThaumonomiconPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(sent.category().equals(decoded.category()),
                "the open payload lost its category across the codec: " + sent.category()
                        + " -> " + decoded.category());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theBookRegistersNoMenuTypeAndTheOpenPayloadInstead(final GameTestHelper helper) {
        // The decision, measured: the browser is containerless, so it has no
        // MenuType realization (BD-22). A menu type here would mean the port
        // invented a container the original never had.
        helper.assertTrue(!BuiltInRegistries.MENU.containsKey(GUI_ROW_ID),
                "thaumcraft:gui_thaumonomicon must NOT be a menu type while the browser is a"
                        + " containerless GuiScreen (GuiResearchBrowser.java:41); if the design"
                        + " changes, rewrite this probe on purpose");
        helper.assertTrue(BuiltInRegistries.MENU.keySet().stream().noneMatch(id ->
                        id.getNamespace().equals("thaumcraft") && id.getPath().contains("thaumonomicon")),
                "no thaumcraft menu type may name the thaumonomicon while the book is payload-opened");

        // The screen's title key is the card's key. A containerless screen gets
        // no title from the server (a menu's title rides the open-screen
        // packet), so the screen translates this key itself — the shipped file
        // must therefore carry it, read here from the mod's own resources.
        final String titleKey = "container.thaumcraft.thaumonomicon";
        final JsonObject lang = loadLang(helper);
        helper.assertTrue(lang.has(titleKey) && lang.get(titleKey).isJsonPrimitive()
                        && lang.get(titleKey).getAsString().length() > 0,
                titleKey + " must ship in en_us.json: it is the book screen's own title (BD-22)");
        helper.succeed();
    }

    /** The shipped lang file, read the way {@code LocalizationGameTest} reads it. */
    private static JsonObject loadLang(final GameTestHelper helper) {
        try (java.io.InputStream in = ThaumonomiconBookGameTest.class.getResourceAsStream(
                "/assets/" + ThaumcraftClassic.MOD_ID + "/lang/en_us.json")) {
            helper.assertTrue(in != null, "en_us.json must be on the mod classpath");
            return com.google.gson.JsonParser
                    .parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** An entry and one of its siblings (the shipped graph is the source of truth). */
    private record Pair(ResearchEntryData entry, String sibling) {
    }

    /** The first (entry, sibling) pair the shipped graph offers. */
    private static Pair pickSiblingPair() {
        for (final ResearchEntryData entry : ResearchManager.graph().entries()) {
            final List<String> siblings = entry.siblings();
            if (siblings != null && !siblings.isEmpty()
                    && ResearchManager.graph().entry(siblings.get(0)) != null) {
                return new Pair(entry, siblings.get(0));
            }
        }
        return null;
    }

    private static void setKnowledge(final ServerPlayer player, final PlayerKnowledgeData knowledge) {
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
    }

    /**
     * A real {@link ServerPlayer} with a declared connection, the shape
     * {@code ResearchNoteMenuGameTest} established for the research-note
     * screen's server half: {@code makeMockPlayer} is not a {@code ServerPlayer},
     * and the book's server arm — including its payload send — runs against one.
     * The ad-hoc channel set carries the payload ids the arms send, so the
     * {@code thaumcraft:open_thaumonomicon} send is a real send and not a
     * silent skip.
     */
    private static ServerPlayer freshPlayer(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(),
                        "tc-book-" + UUID.randomUUID().toString().substring(0, 8)), false);
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
        adHoc.add(OpenThaumonomiconPayload.TYPE.id());
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);

        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        player.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        player.setHealth(20.0F);
        player.getInventory().clearContent();
        return player;
    }
}
