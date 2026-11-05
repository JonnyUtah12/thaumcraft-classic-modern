package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.menus.ResearchNoteMenu;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.PlayerCompleteToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ResearchCompletePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import com.jonnyutah12.thaumcraftclassic.research.ResearchDetailModel;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/**
 * BOOK-5 (t_569e7f6e): the research-note puzzle wired into the book, end to
 * end on ONE tree. The three halves each shipped on their own lane and were
 * only ever tested against their own base — the note puzzle and the note item
 * on the research lane (TC-M3-04-s / -ii, {@code f689e43} / {@code 312c162}),
 * the book's note arm on the integrated BOOK lane (TC-M3-04-s-v /
 * INTEGRATE-BOOK-LANES, {@code 3ca8131}), the menu contract and the hex-grid
 * window on theirs (TC-M3-04-s-iii, NOTE-SCREEN-1 {@code b77cf88} and
 * NOTE-SCREEN-2 {@code 400656d}). This suite runs the chain across all of
 * them and asserts the joints, never the puzzle logic (the card forbids
 * re-litigating {@code NotePuzzle}/{@code HexUtils}/{@code ResearchNoteMenu}).
 *
 * <p>The chain the card names, in the order a player walks it:</p>
 * <ol>
 *   <li><b>open a note from the book</b> — the book's entry row sends
 *       {@code PlayerCompleteToServerPayload(GOLEMSTRAW, REQUEST_NOTE)}
 *       (BD-25) and {@code ResearchManager.handlePlayerCompleteRequest} mints
 *       a generated puzzle note for that entry, paying one inkwell durability
 *       ({@code ResearchManager.java:204-217}). Right-clicking that note is
 *       the port's seam to the note window (BD-26): it opens
 *       {@code thaumcraft:gui_research_note}, the menu the registered
 *       {@code ResearchNoteScreen} draws (BD-27).</li>
 *   <li><b>complete the aspect-matching puzzle</b> — the window's write arm
 *       (the model's {@code ClickAction}, sent as
 *       {@code AspectPlaceToServerPayload}) lands on
 *       {@code ResearchNoteMenu#placeAspect}, which spends 1 ink and 1 pool
 *       unit and stamps completion through {@code NotePuzzle.checkCompletion}
 *       — the server's claim only. The six {@code ContainerData} ints are the
 *       window's only view of the note, so a joint that breaks there shows up
 *       as a window that draws an unsolved note forever.</li>
 *   <li><b>the result feeds research knowledge</b> — the completed note is
 *       consumed by {@code ItemResearchNotes#use} →
 *       {@code completeWithSiblings} → {@code ResearchManager.completeResearch}
 *       (the ONE completion authority, BD-13/BD-25), which writes the
 *       {@code PLAYER_KNOWLEDGE} attachment the book resolves its detail panel
 *       out of ({@code ResearchDetailModel.detail}).</li>
 * </ol>
 *
 * <p>Per D-019 the expectations come from the shipped data. The note grid is
 * always the generator's own output for the entry under test (never a typed
 * cell map), the ink the window is shown is read back through
 * {@code ResearchNoteMenu.inkRemaining} and {@code hasInk}, and the panel
 * probe names no graph key at all: {@link #gatedPair()} walks
 * {@code ResearchManager.graph()} for the first tagged entry that is the sole
 * prerequisite of another entry, so both the entry and its gated child are what
 * the shipped 201-entry graph contains. Probes 1 and 2 keep the minted-note
 * pair the item lane pinned on the graph ({@link #NOTE_ENTRY} behind
 * {@link #NOTE_PARENT}) so the ink-spend and consumption joints line up with
 * that suite's own expectations.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThaumonomiconNoteIntegrationGameTest {

    private static final ResourceLocation NOTE_ID =
            ResourceLocation.parse(ThaumcraftClassic.MOD_ID + ":itemresearchnotes");

    /** A tagged entry with exactly one prerequisite: the book's note arm. */
    private static final String NOTE_ENTRY = "GOLEMSTRAW";
    /** Its only prerequisite, so the note's completion has its requisites met. */
    private static final String NOTE_PARENT = "HUNGRYCHEST";

    private ThaumonomiconNoteIntegrationGameTest() {
    }

    /**
     * Joint 1: the book's note arm hands over a note that opens the note
     * window, and the window's six data ints describe that note. The mint must
     * not complete anything: the book only asks (BD-25), the note item is the
     * authority (BD-13).
     */
    @GameTest(template = "empty")
    public static void theBookMintsAPuzzleNoteThatOpensTheHexGridWindow(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        setKnowledge(player, PlayerKnowledgeData.empty().withResearchCompleted(NOTE_PARENT));
        final ItemStack inkwell = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        player.getInventory().items.set(0, inkwell);
        player.getInventory().items.set(1, new ItemStack(Items.PAPER));

        ResearchManager.handlePlayerCompleteRequest(
                new PlayerCompleteToServerPayload(NOTE_ENTRY, PlayerCompleteToServerPayload.REQUEST_NOTE),
                player);

        final ItemStack note = findNote(player, NOTE_ENTRY);
        helper.assertTrue(note != null, "the book's note arm left no research note in the inventory");
        final ResearchNoteData scribed = note.get(ModDataComponents.RESEARCH_NOTE.get());
        helper.assertTrue(scribed != null && NOTE_ENTRY.equals(scribed.key()),
                "the minted note does not name the clicked entry: " + scribed);
        helper.assertTrue(!scribed.complete(), "a freshly scribed note must not arrive solved");
        helper.assertTrue(!scribed.hexEntries().isEmpty(),
                "the minted note carries no generated hex grid, so the window has nothing to draw");
        helper.assertTrue(!ResearchManager.isResearchComplete(player.getData(ModAttachments.PLAYER_KNOWLEDGE),
                        NOTE_ENTRY),
                "the book's note arm completed research — BD-25 says the book only asks");

        // The note item's right-click is the seam to the window (BD-26): the
        // open must land on the menu the registered screen draws (BD-27).
        final int held = player.getInventory().items.indexOf(note);
        player.getInventory().selected = held;
        final ItemResearchNotes noteItem = (ItemResearchNotes) BuiltInRegistries.ITEM.get(NOTE_ID);
        final InteractionResultHolder<ItemStack> used =
                noteItem.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(used.getResult() == InteractionResult.SUCCESS,
                "right-clicking an unsolved note did not open the note window: " + used.getResult());
        helper.assertTrue(player.containerMenu instanceof ResearchNoteMenu,
                "the note opened " + player.containerMenu.getClass().getSimpleName()
                        + ", not the thaumcraft:gui_research_note menu the screen binds to");
        helper.assertTrue(note.getCount() == 1, "opening the window consumed the note");

        // The player's shift-click arms (the original's own) put the tool and
        // the note in the two slots the window draws against. The menu is built
        // the way the item's provider builds it, over a data source the probe
        // can read: the six ints are the window's only view of the note.
        final SimpleContainerData data = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu menu = new ResearchNoteMenu(1, player.getInventory(), data);
        helper.assertTrue(menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).mayPlace(inkwell),
                "the scribe slot's gate rejects the shipped scribing tool the window's pool needs");
        helper.assertTrue(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).mayPlace(note),
                "the note slot's gate rejects the note the book just minted");
        menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).set(inkwell);
        menu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(note);
        menu.broadcastChanges();

        helper.assertTrue(data.get(ResearchNoteMenu.DATA_NOTE_PRESENT) == 1,
                "the window sees no note in the slot (DATA_NOTE_PRESENT="
                        + data.get(ResearchNoteMenu.DATA_NOTE_PRESENT) + ")");
        helper.assertTrue(data.get(ResearchNoteMenu.DATA_COMPLETE) == 0,
                "the window is told a freshly scribed note is solved — the grid would never draw");
        helper.assertTrue(data.get(ResearchNoteMenu.DATA_GRID_COUNT) == scribed.hexEntries().size(),
                "the window's grid count " + data.get(ResearchNoteMenu.DATA_GRID_COUNT)
                        + " is not the minted note's cell count " + scribed.hexEntries().size());
        // The ink the window sees is the tool the book's mint just spent a point
        // on: one durability used by the mint, and the menu publishes what the
        // shipped tool has left (BD-26 owns the arithmetic; this pins the joint).
        final int ink = data.get(ResearchNoteMenu.DATA_INK);
        helper.assertTrue(ink == ResearchNoteMenu.inkRemaining(inkwell),
                "the window is shown " + ink + " ink while the shipped tool reports "
                        + ResearchNoteMenu.inkRemaining(inkwell));
        helper.assertTrue(inkwell.getDamageValue() == 1,
                "the book's mint spent " + inkwell.getDamageValue()
                        + " durability on the scribing tool, the note arm spends 1");
        helper.assertTrue(ResearchNoteMenu.hasInk(inkwell),
                "the tool the mint left is out of ink (" + ink + "), so the window would open straight "
                        + "onto the out-of-ink block and no note the book mints could be scribed");
        helper.assertTrue(!ResearchManager.isResearchComplete(player.getData(ModAttachments.PLAYER_KNOWLEDGE),
                        NOTE_ENTRY),
                "opening the window completed research — the client never claims completion (BD-26)");
        helper.succeed();
    }

    /**
     * Joint 2: the window's write arm closes the grid on the server
     * (costs included, completion stamped by {@code NotePuzzle}), and the
     * completed note then feeds research knowledge through the ONE completion
     * authority. Both legs in one probe, because the card's contract is the
     * joint: a solved grid that never lands in the knowledge attachment is
     * exactly the failure this lane is here to catch.
     */
    @GameTest(template = "empty")
    public static void aGridClosedThroughTheWindowWriteArmFeedsResearchKnowledge(
            final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        setKnowledge(player, PlayerKnowledgeData.empty()
                .withResearchCompleted(NOTE_PARENT)
                .withAspectPoolAdded("ignis", 5)
                .withAspectPoolAdded("lux", 5));

        final ItemStack inkwell = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        final ItemStack note = noteStack(closableChain(NOTE_ENTRY), 0);
        final SimpleContainerData data = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu menu = new ResearchNoteMenu(1, player.getInventory(), data);
        menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).set(inkwell);
        menu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(note);

        // The window's write arm: one cell, one aspect, over the shipped menu.
        menu.placeAspect(1, 0, "lux", player);

        final ResearchNoteData written = ResearchNoteMenu.noteOf(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem());
        helper.assertTrue(written != null && written.complete(),
                "the write arm did not close the grid: " + written);
        helper.assertTrue(data.get(ResearchNoteMenu.DATA_COMPLETE) == 1,
                "the solved grid did not reach the window's data slot, so the panel would keep drawing the"
                        + " unsolved note");
        helper.assertTrue(inkwell.getDamageValue() == 1,
                "the write spent " + inkwell.getDamageValue() + " ink, the original spends 1 (:176)");
        helper.assertTrue(player.getData(ModAttachments.PLAYER_KNOWLEDGE).aspectAmount("lux") == 4,
                "the write paid " + (5 - player.getData(ModAttachments.PLAYER_KNOWLEDGE).aspectAmount("lux"))
                        + " from the pool, the original pays 1 (:148-152)");
        helper.assertTrue(!ResearchManager.isResearchComplete(player.getData(ModAttachments.PLAYER_KNOWLEDGE),
                        NOTE_ENTRY),
                "the write arm completed research — completion is the note item's claim, not the menu's");

        // The completed note is the research feed: use() → completeWithSiblings
        // → ResearchManager.completeResearch, and the note is consumed.
        player.getInventory().items.set(0, note);
        player.getInventory().selected = 0;
        final ItemResearchNotes noteItem = (ItemResearchNotes) BuiltInRegistries.ITEM.get(NOTE_ID);
        final InteractionResultHolder<ItemStack> used =
                noteItem.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(used.getResult() == InteractionResult.CONSUME,
                "the completed note did not finish the entry through the note item: " + used.getResult());
        helper.assertTrue(ResearchManager.isResearchComplete(player.getData(ModAttachments.PLAYER_KNOWLEDGE),
                        NOTE_ENTRY),
                "a solved, consumed note left " + NOTE_ENTRY + " unknown — the puzzle result never"
                        + " reached the knowledge attachment");
        helper.assertTrue(count(player, NOTE_ID) == 0,
                "the completed note survived its own completion (" + count(player, NOTE_ID) + " left)");
        helper.succeed();
    }

    /**
     * Joint 3: the knowledge the note wrote is the SAME store the book
     * resolves its detail panel out of, so the book shows the solved entry as
     * known and the entry gated behind it stops being locked. This is "the
     * result feeds research knowledge" in the only form the book can observe.
     *
     * <p>The entry and its gated child come out of the shipped graph at test
     * time (D-019 rule 3 — the way BOOK-4's state probe names no literal): the
     * first entry that carries essentia requirements and is the <em>sole</em>
     * prerequisite of another entry. The grid is closed through the window's
     * write arm, so the panel's rewrite is caused by a solved note and not by a
     * hand-written knowledge set.</p>
     */
    @GameTest(template = "empty")
    public static void theSolvedNoteRewritesTheBookPanelForEntryAndChild(final GameTestHelper helper) {
        final GatedPair pair = gatedPair();
        final String entry = pair.entry();
        final String child = pair.gatedChild();

        // The player knows the entry's prerequisites, so before the note is
        // solved the panel shows the entry as openable and its child as locked.
        PlayerKnowledgeData gated = PlayerKnowledgeData.empty();
        for (final String parent : ResearchManager.graph().entry(entry).parents()) {
            gated = gated.withResearchCompleted(parent);
        }
        final ResearchDetailModel details = ResearchDetailModel.forShippedGraph();
        helper.assertTrue(ResearchDetailModel.State.AVAILABLE == details.detail(entry, gated).state(),
                entry + " is not openable with its prerequisites known in the book's panel: "
                        + details.detail(entry, gated).state());
        helper.assertTrue(ResearchDetailModel.State.LOCKED == details.detail(child, gated).state(),
                child + " is not locked behind " + entry + " in the book's panel: "
                        + details.detail(child, gated).state());

        final ServerPlayer player = freshPlayer(helper);
        setKnowledge(player, gated.withAspectPoolAdded("ignis", 5).withAspectPoolAdded("lux", 5));

        final ItemStack inkwell = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        final ItemStack note = noteStack(closableChain(entry), 0);
        final SimpleContainerData data = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu menu = new ResearchNoteMenu(1, player.getInventory(), data);
        menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).set(inkwell);
        menu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(note);

        // The window's write arm closes the grid; the note item is what feeds
        // knowledge (BD-26: the menu never claims completion).
        menu.placeAspect(1, 0, "lux", player);
        final ResearchNoteData written =
                ResearchNoteMenu.noteOf(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem());
        helper.assertTrue(written != null && written.complete(),
                "the write arm did not close the grid for " + entry
                        + ", so the panel has nothing to rewrite: " + written);
        helper.assertTrue(data.get(ResearchNoteMenu.DATA_COMPLETE) == 1,
                "the solved grid never reached the window's data slot for " + entry);

        player.getInventory().items.set(0, note);
        player.getInventory().selected = 0;
        final ItemResearchNotes noteItem = (ItemResearchNotes) BuiltInRegistries.ITEM.get(NOTE_ID);
        helper.assertTrue(noteItem.use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                        .getResult() == InteractionResult.CONSUME,
                "the solved note did not finish " + entry + " through the note item");

        // The panel, resolved out of the player's own attachment, rewrites both
        // rows: the solved entry is known, its gated child is openable.
        final PlayerKnowledgeData solved = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(ResearchDetailModel.State.COMPLETED == details.detail(entry, solved).state(),
                "the book's panel does not report the note's entry as known: "
                        + details.detail(entry, solved).state());
        helper.assertTrue(ResearchDetailModel.State.AVAILABLE == details.detail(child, solved).state(),
                child + " stayed " + details.detail(child, solved).state()
                        + " after its sole gate " + entry + " was solved through the window");
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** An entry of the shipped graph and an entry gated on it alone. */
    private record GatedPair(String entry, String gatedChild) {
    }

    /**
     * The first entry in the shipped graph that carries essentia requirements
     * and is the only prerequisite of another entry. Nothing is named here: the
     * pair is what the shipped 201-entry graph contains (D-019 rule 3).
     */
    private static GatedPair gatedPair() {
        for (final ResearchEntryData candidate : ResearchManager.graph().entries()) {
            if (candidate.tags().isEmpty()) {
                continue;
            }
            for (final ResearchEntryData other : ResearchManager.graph().entries()) {
                if (other.parents().size() == 1 && other.parents().get(0).equals(candidate.key())) {
                    return new GatedPair(candidate.key(), other.key());
                }
            }
        }
        throw new IllegalStateException(
                "no entry in the shipped graph carries essentia requirements and gates another entry alone");
    }

    /**
     * A grid the window's write arm can close: required ignis cells two steps
     * apart (never neighbours, {@code HexUtils.NEIGHBOURS}) with one blank
     * cell between them, which touches both. The only write that connects the
     * chain is the parent aspect on the middle cell (lux = aer + ignis,
     * {@code AspectDefinitions}; {@code NotePuzzle.componentsMatch} is the
     * direct-component rule). The shape is the shipped menu suite's, so the
     * joint is under test and the puzzle rule is not re-derived here.
     */
    private static ResearchNoteData closableChain(final String entryKey) {
        final Map<String, HexEntry> cells = new LinkedHashMap<>();
        cells.put("0:0", new HexEntry("ignis", 1));
        cells.put("2:0", new HexEntry("ignis", 1));
        cells.put("1:0", new HexEntry(null, 0));
        return new ResearchNoteData(entryKey, 1, false, 0, cells);
    }

    private static ItemStack noteStack(final ResearchNoteData note, final int meta) {
        final ItemStack stack = new ItemStack(M3ResearchContent.ITEM_RESEARCH_NOTES.get());
        stack.set(ModDataComponents.RESEARCH_NOTE.get(), note);
        stack.set(ModDataComponents.ITEM_META.get(), meta);
        return stack;
    }

    private static ItemStack findNote(final ServerPlayer player, final String key) {
        final int slot = ResearchManager.noteSlot(player.getInventory().items, key);
        return slot >= 0 ? player.getInventory().items.get(slot) : null;
    }

    private static void setKnowledge(final ServerPlayer player, final PlayerKnowledgeData knowledge) {
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
    }

    private static int count(final ServerPlayer player, final ResourceLocation id) {
        int total = 0;
        for (final ItemStack stack : player.getInventory().items) {
            if (stack != null && stack.is(BuiltInRegistries.ITEM.get(id))) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * A real {@link ServerPlayer} with a declared connection, the way
     * {@code ResearchNoteMenuGameTest} and {@code ResearchNoteItemGameTest}
     * build one: {@code GameTestHelper#makeMockPlayer} is not a
     * {@code ServerPlayer}, and both the note arm and the menu's write arm run
     * against {@code ServerPlayer}. The completion leg sends the attachment
     * sync and the research payload, so the ad-hoc channel list mirrors the
     * sibling suites'.
     */
    private static ServerPlayer freshPlayer(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(),
                        "tc-book-note-" + UUID.randomUUID().toString().substring(0, 8)), false);
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
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        player.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        player.setHealth(20.0F);
        player.getInventory().clearContent();
        return player;
    }
}
