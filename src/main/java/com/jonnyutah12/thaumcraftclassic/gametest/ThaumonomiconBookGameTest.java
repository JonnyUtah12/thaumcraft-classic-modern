package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemThaumonomicon;
import com.jonnyutah12.thaumcraftclassic.network.ModNetwork;
import com.jonnyutah12.thaumcraftclassic.network.payload.OpenThaumonomiconPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchBrowserModel;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    // ---------------------------------------------------------------- BOOK-2
    // The category tab strip and the entry listing, measured on the live graph.
    // The expected set is read out of the shipped JSON with a raw parser, so
    // the probe asserts the listing against the DATA (6 categories / 201
    // entries) and not against the loader that already parsed it: a hardcoded
    // id list in a probe can never go red, and the card's gate is that every
    // entry of the graph renders a row and nothing outside it does.

    @GameTest(template = "empty")
    public static void bookTabStripIsTheGraphsCategoriesInRegistrationOrder(final GameTestHelper helper) {
        final JsonObject categories = loadJsonResource(helper,
                "/data/" + ThaumcraftClassic.MOD_ID + "/research_graph/categories.json");

        final List<String> expectedKeys = new ArrayList<>();
        final Map<String, String> expectedLabels = new LinkedHashMap<>();
        final Map<String, Integer> expectedOrder = new LinkedHashMap<>();
        for (final com.google.gson.JsonElement element : categories.getAsJsonArray("categories")) {
            final JsonObject category = element.getAsJsonObject();
            expectedKeys.add(category.get("key").getAsString());
            expectedLabels.put(category.get("key").getAsString(), category.get("nameEnUs").getAsString());
            expectedOrder.put(category.get("key").getAsString(), category.get("order").getAsInt());
        }

        final ResearchBrowserModel model = ResearchBrowserModel.forShippedGraph();
        final List<ResearchBrowserModel.Tab> tabs = model.tabs();
        final List<String> tabKeys = new ArrayList<>();
        for (final ResearchBrowserModel.Tab tab : tabs) {
            tabKeys.add(tab.key());
        }
        helper.assertTrue(expectedKeys.size() == tabs.size(),
                "the tab strip renders " + tabs.size() + " tabs, the shipped categories.json has "
                        + expectedKeys.size() + " categories: " + tabKeys);
        helper.assertTrue(expectedKeys.equals(tabKeys),
                "the tab strip must be the graph's categories in the data's registration order "
                        + "(GuiResearchBrowser.java:480-491 iterates the category keySet), got " + tabKeys);
        for (final ResearchBrowserModel.Tab tab : tabs) {
            helper.assertTrue(expectedLabels.get(tab.key()).equals(tab.label()),
                    "tab " + tab.key() + " must carry the category display name from the graph, got \""
                            + tab.label() + "\"");
            helper.assertTrue(expectedOrder.get(tab.key()) == tab.order(),
                    "tab " + tab.key() + " must carry the order field from categories.json");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bookRowsCoverEveryGraphEntryAndNothingOutsideIt(final GameTestHelper helper) {
        final JsonObject entries = loadJsonResource(helper,
                "/data/" + ThaumcraftClassic.MOD_ID + "/research_graph/entries.json");

        final List<String> expectedKeys = new ArrayList<>();
        final Map<String, String> expectedCategory = new LinkedHashMap<>();
        final Map<String, String> expectedLabel = new LinkedHashMap<>();
        for (final com.google.gson.JsonElement element : entries.getAsJsonArray("entries")) {
            final JsonObject entry = element.getAsJsonObject();
            final String key = entry.get("key").getAsString();
            expectedKeys.add(key);
            expectedCategory.put(key, entry.get("category").getAsString());
            final com.google.gson.JsonElement label = entry.get("nameEnUs");
            expectedLabel.put(key, label == null || label.isJsonNull() ? key : label.getAsString());
        }
        helper.assertTrue(entries.get("entryCount").getAsInt() == expectedKeys.size(),
                "entries.json declares " + entries.get("entryCount").getAsInt() + " entries and carries "
                        + expectedKeys.size() + " — the data itself is the premise of this probe");

        final ResearchBrowserModel model = ResearchBrowserModel.forShippedGraph();

        // Every entry of the graph renders exactly one row, somewhere.
        final List<String> rendered = new ArrayList<>();
        for (final ResearchBrowserModel.Tab tab : model.tabs()) {
            for (final ResearchBrowserModel.Row row : model.rows(tab.key())) {
                rendered.add(row.key());
                helper.assertTrue(tab.key().equals(expectedCategory.get(row.key())),
                        "row " + row.key() + " is listed under tab " + tab.key() + " but the graph puts it in "
                                + expectedCategory.get(row.key()));
                final String label = expectedLabel.get(row.key());
                helper.assertTrue(label.equals(row.label()),
                        "row " + row.key() + " must be labelled from entries.json, got \"" + row.label() + "\"");
            }
        }
        helper.assertTrue(rendered.size() == expectedKeys.size(),
                "the book renders " + rendered.size() + " rows for " + expectedKeys.size()
                        + " entries of the shipped graph (a row missing, or a row rendered twice)");
        helper.assertTrue(new java.util.HashSet<>(rendered).equals(new java.util.HashSet<>(expectedKeys)),
                "the rendered row set is not exactly the entry set of the shipped graph");

        // The converse: an entry the graph does not have renders nothing.
        helper.assertTrue(model.row(BOOK2_PROBE_KEY) == null,
                "a key that is not in the graph must resolve to no row at all");
        for (final ResearchBrowserModel.Tab tab : model.tabs()) {
            helper.assertTrue(tab.entryCount() == model.rows(tab.key()).size(),
                    "tab " + tab.key() + " advertises " + tab.entryCount() + " entries and lists "
                            + model.rows(tab.key()).size());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bookNavigationWalksCategoriesAndEntriesAndRefusesStrangers(final GameTestHelper helper) {
        final JsonObject categories = loadJsonResource(helper,
                "/data/" + ThaumcraftClassic.MOD_ID + "/research_graph/categories.json");
        final List<String> expectedKeys = new ArrayList<>();
        for (final com.google.gson.JsonElement element : categories.getAsJsonArray("categories")) {
            expectedKeys.add(element.getAsJsonObject().get("key").getAsString());
        }

        final ResearchBrowserModel model = ResearchBrowserModel.forShippedGraph();

        for (final String category : expectedKeys) {
            helper.assertTrue(model.selectCategory(category),
                    "the tab for " + category + " must switch the listing (:757-761 sets selectedCategory)");
            helper.assertTrue(category.equals(model.selectedCategory()),
                    "the book must remember the tab it switched to (:63)");
            final List<ResearchBrowserModel.Row> rows = model.rows(category);
            helper.assertTrue(!rows.isEmpty(), "the " + category + " tab renders no entries at all");

            // Paging covers the tab's rows exactly once, in order.
            final int pages = model.pageCount(category);
            final List<String> paged = new ArrayList<>();
            for (int page = 0; page < pages; page++) {
                for (final ResearchBrowserModel.Row row : model.page(category, page)) {
                    paged.add(row.key());
                }
            }
            final List<String> listed = new ArrayList<>();
            for (final ResearchBrowserModel.Row row : rows) {
                listed.add(row.key());
            }
            helper.assertTrue(listed.equals(paged),
                    "paging " + category + " over " + pages + " pages must cover every row exactly once");

            // Rows open: first, last and the middle of the tab.
            for (final ResearchBrowserModel.Row row
                    : List.of(rows.get(0), rows.get(rows.size() / 2), rows.get(rows.size() - 1))) {
                helper.assertTrue(model.selectEntry(row.key()),
                        "the row " + row.key() + " must open the entry (the original opens its page, :711)");
                helper.assertTrue(row.key().equals(model.selectedEntry()),
                        "the book must remember the opened entry");
                helper.assertTrue(model.selectedRow() != null && row.key().equals(model.selectedRow().key()),
                        "selectedRow must be the opened entry " + row.key());
            }
        }

        // Page turning stays inside the tab's own page count.
        model.selectCategory(expectedKeys.get(0));
        int turns = 0;
        while (model.nextPage()) {
            turns++;
            helper.assertTrue(turns < 100, "nextPage must stop at the last page of a category");
        }
        helper.assertTrue(turns == model.pageCount(model.selectedCategory()) - 1,
                "nextPage overwran the last page of " + model.selectedCategory() + " (" + turns + " turns)");
        while (model.prevPage()) {
            // Back to the front; prevPage returns false on the first page.
        }
        helper.assertTrue(model.page() == 0, "prevPage must stop on the first page");

        // The negative half of the gate: a stranger renders no row and moves nothing.
        final String category = model.selectedCategory();
        final String opened = model.selectedEntry();
        helper.assertTrue(!model.selectEntry(BOOK2_PROBE_KEY),
                "selecting an entry the graph does not have must be refused, so no row can render for it");
        helper.assertTrue(java.util.Objects.equals(opened, model.selectedEntry()),
                "the refused entry selection must not move the book");
        helper.assertTrue(!model.selectCategory(BOOK2_PROBE_KEY),
                "the tab strip may not switch to a category the graph does not have");
        helper.assertTrue(category.equals(model.selectedCategory()),
                "the refused tab switch must not move the book");
        helper.succeed();
    }

    /** A key the shipped graph cannot contain (its keys are upper-case research keys). */
    private static final String BOOK2_PROBE_KEY = "t10805b95_not_in_the_graph";

    /** A shipped data resource, read raw: the expected set comes from the JSON. */
    private static JsonObject loadJsonResource(final GameTestHelper helper, final String path) {
        try (java.io.InputStream in = ThaumonomiconBookGameTest.class.getResourceAsStream(path)) {
            helper.assertTrue(in != null, path + " must be on the mod classpath (the shipped graph)");
            return com.google.gson.JsonParser
                    .parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
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
