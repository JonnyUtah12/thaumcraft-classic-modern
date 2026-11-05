package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemThaumonomicon;
import com.jonnyutah12.thaumcraftclassic.network.ModNetwork;
import com.jonnyutah12.thaumcraftclassic.network.payload.OpenThaumonomiconPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchBrowserModel;
import com.jonnyutah12.thaumcraftclassic.research.ResearchDetailModel;
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

    // ---------------------------------------------------------------- BOOK-4
    // The per-entry detail: aspect requirements, unlock state and the
    // parents / parentsHidden / siblings links, measured on the live graph and
    // on a real ServerPlayer's knowledge attachment. The expected sets are read
    // out of entries.json with a raw parser and the expected state is computed
    // through ResearchManager, so the probes assert the panel against the DATA
    // and against the SERVER'S OWN research semantics — not against a list in
    // this file and not against a second store (D-019 rule 3).

    @GameTest(template = "empty")
    public static void bookDetailResolvesEveryGraphEdgeAndEveryLinkOpensARow(final GameTestHelper helper) {
        final JsonObject entries = loadJsonResource(helper,
                "/data/" + ThaumcraftClassic.MOD_ID + "/research_graph/entries.json");
        final ResearchDetailModel details = ResearchDetailModel.forShippedGraph();
        final ResearchBrowserModel browser = ResearchBrowserModel.forShippedGraph();
        final PlayerKnowledgeData empty = PlayerKnowledgeData.empty();

        int edges = 0;
        int hiddenEdges = 0;
        int siblingEdges = 0;
        int aspectRequirements = 0;
        for (final com.google.gson.JsonElement element : entries.getAsJsonArray("entries")) {
            final JsonObject entry = element.getAsJsonObject();
            final String key = entry.get("key").getAsString();
            final ResearchDetailModel.Detail detail = details.detail(key, empty);
            helper.assertTrue(detail != null,
                    "the shipped graph's own entry " + key + " resolves no detail — the panel would be blank");
            helper.assertTrue(key.equals(detail.key()) && entry.get("category").getAsString().equals(detail.category()),
                    key + ": the detail is the graph's entry, in the graph's category");

            final List<String> expected = new ArrayList<>();
            for (final String kind : ResearchDetailModel.linkKinds()) {
                for (final com.google.gson.JsonElement ref : entry.getAsJsonArray(kind)) {
                    expected.add(kind + " " + ref.getAsString());
                }
            }
            final List<String> actual = new ArrayList<>();
            for (final ResearchDetailModel.Link link : detail.links()) {
                actual.add(link.kind() + " " + link.key());
                if (ResearchDetailModel.KIND_PARENTS_HIDDEN.equals(link.kind())) {
                    hiddenEdges++;
                }
                if (ResearchDetailModel.KIND_SIBLINGS.equals(link.kind())) {
                    siblingEdges++;
                }
            }
            helper.assertTrue(expected.equals(actual),
                    key + ": the panel's related research must be exactly the entry's parents, parentsHidden and"
                            + " siblings in entries.json (GuiResearchBrowser.java:288-333 reads the same arrays),"
                            + " got " + actual);
            edges += detail.links().size();

            for (final ResearchDetailModel.Link link : detail.links()) {
                final ResearchEntryData target = ResearchManager.graph().entry(link.key());
                helper.assertTrue(link.inGraph(),
                        key + " -> " + link.key() + ": the graph carries the target, so the link must resolve");
                final String named = target == null ? null : target.nameEnUs();
                final String label = named == null || named.isBlank() ? link.key() : named;
                helper.assertTrue(label.equals(link.label()),
                        key + " -> " + link.key() + ": a link is labelled from the graph, and an entry the graph"
                                + " names nothing is labelled with its KEY (the BOOK-3 posture), got \""
                                + link.label() + "\"");
            }

            helper.assertTrue(entry.getAsJsonArray("tags").size() == detail.aspects().size(),
                    key + ": the panel's aspect requirements are the entry's tags in the data ("
                            + entry.getAsJsonArray("tags").size() + " declared, " + detail.aspects().size()
                            + " resolved)");
            aspectRequirements += detail.aspects().size();

            // Every link the panel offers as openable is a row the browser can
            // open: the click target of a related-research row exists.
            for (final ResearchDetailModel.Link link : detail.links()) {
                final ResearchDetailModel.Detail.Line line = lineFor(detail, link);
                helper.assertTrue(line != null && link.key().equals(line.linkKey()),
                        key + " -> " + link.key() + ": a resolvable link is a line the panel can open");
                helper.assertTrue(browser.row(link.key()) != null,
                        key + " -> " + link.key() + ": the link's target has no row in the book, so the panel"
                                + " would point at nothing");
                helper.assertTrue(browser.selectEntry(link.key()),
                        key + " -> " + link.key() + ": the panel's link must open that entry"
                                + " (the original's node click, :711)");
            }
        }
        helper.assertTrue(edges > 0 && hiddenEdges > 0 && siblingEdges > 0,
                "the sweep saw " + edges + " edges, " + hiddenEdges + " hidden parents, " + siblingEdges
                        + " siblings — the graph's edges are the contract, all three kinds must be surfaced");
        helper.assertTrue(aspectRequirements > 0,
                "the sweep resolved no essentia requirement at all, while entries.json carries them");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bookDetailUnlockStateIsTheServersResearchStore(final GameTestHelper helper) {
        // The entry is picked out of the graph, never named here: the first
        // entry with prerequisites whose targets the graph all carries.
        String key = null;
        for (final ResearchEntryData entry : ResearchManager.graph().entries()) {
            if (entry.parents() == null || entry.parents().isEmpty()) {
                continue;
            }
            boolean allResolvable = true;
            for (final String parent : ResearchDetailModel.references(entry, ResearchDetailModel.KIND_PARENT)) {
                allResolvable &= ResearchManager.graph().entry(parent) != null;
            }
            for (final String parent : ResearchDetailModel.references(entry, ResearchDetailModel.KIND_PARENTS_HIDDEN)) {
                allResolvable &= ResearchManager.graph().entry(parent) != null;
            }
            if (allResolvable) {
                key = entry.key();
                break;
            }
        }
        helper.assertTrue(key != null,
                "the shipped graph has no entry with resolvable prerequisites — the graph (201 entries / 6"
                        + " categories) must be present for this probe to mean anything");

        final ServerPlayer player = freshPlayer(helper);
        setKnowledge(player, PlayerKnowledgeData.empty());
        final ResearchDetailModel details = ResearchDetailModel.forShippedGraph();

        // An empty store: locked, with the prerequisites named — and the panel's
        // answer is the same one ResearchManager gives for the same store.
        ResearchDetailModel.Detail detail =
                details.detail(key, player.getData(ModAttachments.PLAYER_KNOWLEDGE));
        helper.assertTrue(ResearchDetailModel.State.LOCKED == detail.state(),
                key + " has prerequisites and the player has completed nothing: " + detail.state());
        helper.assertTrue(!detail.missingPrerequisites().isEmpty(),
                key + ": the locked panel names the prerequisites it is missing");
        helper.assertTrue(detail.lines().contains(ResearchDetailModel.STATE_LOCKED_TEXT),
                key + ": the locked panel shows the original's own card text (tc.researchmissing)");

        // The server's arm completes the prerequisites. The panel reads them
        // back out of the player's attachment: no second store is consulted.
        final ResearchEntryData entry = ResearchManager.graph().entry(key);
        for (final String parent : ResearchDetailModel.references(entry, ResearchDetailModel.KIND_PARENT)) {
            helper.assertTrue(ResearchManager.completeResearch(player, parent),
                    "the server must complete the prerequisite " + parent + " through its own research arm");
        }
        for (final String parent
                : ResearchDetailModel.references(entry, ResearchDetailModel.KIND_PARENTS_HIDDEN)) {
            helper.assertTrue(ResearchManager.completeResearch(player, parent),
                    "the server must complete the hidden prerequisite " + parent + " through its own arm");
        }
        detail = details.detail(key, player.getData(ModAttachments.PLAYER_KNOWLEDGE));
        helper.assertTrue(ResearchDetailModel.State.AVAILABLE == detail.state(),
                key + " must read available once its prerequisites sit in the player's store, got "
                        + detail.state());
        helper.assertTrue(detail.missingPrerequisites().isEmpty(),
                key + ": the missing list must be empty once the prerequisites are complete");
        for (final ResearchDetailModel.Link link : detail.links(ResearchDetailModel.KIND_PARENT)) {
            helper.assertTrue(link.completed(), link.key() + " is completed, and its link row must say so");
        }

        // Completing the entry itself, through the same server arm, flips it to known.
        helper.assertTrue(ResearchManager.completeResearch(player, key),
                "the server must complete " + key + " itself");
        final ResearchDetailModel.Detail done =
                details.detail(key, player.getData(ModAttachments.PLAYER_KNOWLEDGE));
        helper.assertTrue(ResearchDetailModel.State.COMPLETED == done.state(),
                key + ": a completed entry reads known to the panel, got " + done.state());
        helper.assertTrue(done.lines().contains(ResearchDetailModel.STATE_KNOWN_TEXT),
                key + ": the panel's first line names the state it resolved");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bookDetailAspectsAreTheGraphTagsAndThePlayersPool(final GameTestHelper helper) {
        final ResearchEntryData entry = firstEntryWithTags();
        helper.assertTrue(entry != null,
                "the shipped graph has no entry with essentia tags — entries.json carries them on 172 entries");
        final ServerPlayer player = freshPlayer(helper);
        setKnowledge(player, PlayerKnowledgeData.empty());
        final ResearchDetailModel details = ResearchDetailModel.forShippedGraph();

        final ResearchDetailModel.Detail broke =
                details.detail(entry.key(), player.getData(ModAttachments.PLAYER_KNOWLEDGE));
        helper.assertTrue(!broke.aspects().isEmpty(), entry.key() + ": the panel resolves the entry's tags");
        helper.assertTrue(!broke.aspectsAffordable(),
                entry.key() + ": an empty aspect pool makes nothing affordable");
        for (final ResearchDetailModel.AspectRequirement requirement : broke.aspects()) {
            helper.assertTrue(!requirement.discovered(),
                    entry.key() + " -> " + requirement.aspect() + ": an empty store has discovered no aspect");
        }
        helper.assertTrue(broke.lines().stream().anyMatch(line -> line.startsWith(ResearchDetailModel.ASPECTS_LABEL)),
                entry.key() + ": the panel carries the essentia line: " + broke.lines());
        helper.assertTrue(broke.lines().contains(ResearchDetailModel.ASPECTS_SHORT_TEXT),
                entry.key() + ": the panel shows tc.research.short when the pool is below the cost (:655)");

        // Fill the pool through the store, one point below the first tag's cost.
        PlayerKnowledgeData armed = PlayerKnowledgeData.empty();
        final List<ResearchEntryData.AspectTag> tags = entry.tags();
        for (int i = 0; i < tags.size(); i++) {
            final ResearchEntryData.AspectTag tag = tags.get(i);
            armed = armed.withAspectDiscovered(tag.aspect(), i == 0 && tag.amount() > 0 ? tag.amount() - 1 : tag.amount());
        }
        setKnowledge(player, armed);
        final ResearchDetailModel.Detail short_ =
                details.detail(entry.key(), player.getData(ModAttachments.PLAYER_KNOWLEDGE));
        helper.assertTrue(!short_.aspectsAffordable(),
                entry.key() + ": a pool one below the cost is not affordable (the original pulses that tag, :625-630)");
        helper.assertTrue(short_.aspects().get(0).discovered(),
                entry.key() + ": the tag is discovered once it sits in the store");
        helper.assertTrue(short_.lines().contains(ResearchDetailModel.ASPECTS_SHORT_TEXT),
                entry.key() + ": the short line stays while the pool is short");

        PlayerKnowledgeData paid = armed;
        for (final ResearchEntryData.AspectTag tag : tags) {
            paid = paid.withAspectDiscovered(tag.aspect(), tag.amount() - paid.aspectAmount(tag.aspect()));
        }
        setKnowledge(player, paid);
        final ResearchDetailModel.Detail full =
                details.detail(entry.key(), player.getData(ModAttachments.PLAYER_KNOWLEDGE));
        helper.assertTrue(full.aspectsAffordable(),
                entry.key() + ": a pool that covers every tag is affordable (tc.research.purchase, :653)");
        helper.assertTrue(!full.lines().contains(ResearchDetailModel.ASPECTS_SHORT_TEXT),
                entry.key() + ": the short line is gone once the pool covers the cost");
        helper.succeed();
    }

    /** The panel line a link renders (matched on the link's own text). */
    private static ResearchDetailModel.Detail.Line lineFor(final ResearchDetailModel.Detail detail,
                                                          final ResearchDetailModel.Link link) {
        for (final ResearchDetailModel.Detail.Line line : detail.displayLines()) {
            if (link.key().equals(line.linkKey()) && line.text().equals(link.text())) {
                return line;
            }
        }
        return null;
    }

    /** The first entry of the graph that carries essentia tags. */
    private static ResearchEntryData firstEntryWithTags() {
        for (final ResearchEntryData entry : ResearchManager.graph().entries()) {
            if (entry.tags() != null && !entry.tags().isEmpty()) {
                return entry;
            }
        }
        return null;
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
