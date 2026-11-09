package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.menus.ResearchNoteMenu;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPlaceToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ResearchCompletePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
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
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/**
 * TC-M3-04-s-iii: the research-note screen's server half — the menu, its slot
 * gates, its container-side note sync, and the rule that the client's word
 * never completes a note (originals {@code ContainerResearchTable},
 * {@code TileResearchTable.placeAspect}/{@code duplicate},
 * {@code GuiResearchTable}'s click arms).
 *
 * <p>The screen itself ({@code client/ResearchNoteScreen}) is client rendering
 * and needs a window: its coverage is the headless gates that can see it —
 * {@code MenuTypeScreenCoverageTest} (every registered menu type has a
 * registered screen, so the hex-grid screen is bound to
 * {@code thaumcraft:gui_research_table}) and {@code LocalizationGameTest}
 * (every registered menu type needs a title key, so
 * {@code container.thaumcraft.gui_research_table} ships with it). These probes
 * cover what runs on the server: the menu's contract with the screen.</p>
 *
 * <ul>
 *   <li>{@link #menuTypeIsRegisteredWithTheOriginalSlotShape}:
 *       {@code thaumcraft:gui_research_table} is in the live menu registry, the
 *       holder is the one {@code ModClientScreens} binds, and a constructed
 *       menu carries the original's slots (scribe 0, note 1, 36 player slots)
 *       and the six data slots that replace the original's per-slot container
 *       data.</li>
 *   <li>{@link #slotGatesFollowTheOriginal}: the scribe slot takes only
 *       scribing tools (the original's {@code SlotLimitedByClass(IScribeTools)}),
 *       and the note slot takes an unsolved puzzle note while it refuses stone,
 *       the discovery family (meta 64+) and a completed note (the original's
 *       {@code isItemValidForSlot(1, stack)} pair of gates).</li>
 *   <li>{@link #inkGateBlocksEveryGridWrite}: with no scribing tool in the
 *       scribe slot the original's ink gate wraps the whole write — a hex write
 *       changes nothing on the note, {@code hasInk()} is false and the synced
 *       ink value is 0 (BD-20: the scribing tool is TC-M3-02's card, so this
 *       is the shipped state today).</li>
 *   <li>{@link #dataSlotsCarryTheNoteStateToTheClient}: the six data slots
 *       carry note-present, complete, grid count, copies, ink and colour in the
 *       original's per-slot order, and the client-side mirror of a solved note
 *       reads them back through {@code setData} — the container-side note sync
 *       of the card's scope.</li>
 *   <li>{@link #theServerIsTheOnlyCompletionAuthority}: the write packet the
 *       screen sends carries no completion claim (its record components are
 *       position, hex and aspect), every menu button id except the original's
 *       {@code RESEARCHDUPE} case 5 is refused, and case 5 runs the original's
 *       copy arm — which the ink gate stops, so no client request can stamp
 *       {@code complete} on a note. A note the server has not solved stays
 *       {@code complete = false}.</li>
 *   <li>{@link #copyArmNeedsInkBookAndCoal}: the original's
 *       {@code RESEARCHDUPE} gates — a completed note with a book and coal in
 *       the inventory still cannot be copied without ink, and its copy count
 *       and stack count stay untouched.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchNoteScreenGameTest {

    private static final ResourceLocation MENU_ID =
            ResourceLocation.parse(ThaumcraftClassic.MOD_ID + ":gui_research_table");
    private static final String NOTE_KEY = "item." + ThaumcraftClassic.MOD_ID + ".itemresearchnotes";

    private ResearchNoteScreenGameTest() {
    }

    @GameTest(template = "empty")
    public static void menuTypeIsRegisteredWithTheOriginalSlotShape(final GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.MENU.containsKey(MENU_ID),
                "thaumcraft:gui_research_table is missing from the menu registry");
        final DeferredHolder<?, ?> holder = M3ResearchContent.GUI_RESEARCH_TABLE;
        helper.assertTrue(BuiltInRegistries.MENU.get(MENU_ID) == holder.get(),
                "the registered menu type is not the M3ResearchContent holder");

        final ServerPlayer player = freshPlayer(helper);
        final ResearchNoteMenu menu = newMenu(player);
        // Original ContainerResearchTable: slot 0 scribe (14, 10), slot 1 note
        // (70, 10), then the 36 player slots.
        helper.assertTrue(menu.slots.size() == 38,
                "expected the two table slots plus 36 player slots, got " + menu.slots.size());
        final Slot scribe = menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE);
        final Slot note = menu.getSlot(ResearchNoteMenu.SLOT_NOTE);
        helper.assertTrue(scribe.x == 14 && scribe.y == 10,
                "the scribe slot is not at the original's (14, 10): " + scribe.x + "," + scribe.y);
        helper.assertTrue(note.x == 70 && note.y == 10,
                "the note slot is not at the original's (70, 10): " + note.x + "," + note.y);
        helper.assertTrue(menu.getSlot(ResearchNoteMenu.FIRST_PLAYER_SLOT).x == 8
                        && menu.getSlot(ResearchNoteMenu.FIRST_PLAYER_SLOT).y == 233,
                "the player hotbar row is not at the original's (8, 233)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void slotGatesFollowTheOriginal(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final ResearchNoteMenu menu = newMenu(player);

        // Original: SlotLimitedByClass(IScribeTools.class) — the port has no
        // scribing tool item registered yet (TC-M3-02), so nothing passes.
        helper.assertTrue(!menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).mayPlace(new ItemStack(Items.STONE)),
                "the scribe slot accepted a non-scribing tool");
        helper.assertTrue(!menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).mayPlace(noteStack(puzzleNote(3), 0)),
                "the scribe slot accepted a research note");

        // Original TileResearchTable.isItemValidForSlot(1, stack): a note with
        // metadata below 64.
        helper.assertTrue(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).mayPlace(noteStack(puzzleNote(3), 0)),
                "the note slot refused an unsolved puzzle note");
        helper.assertTrue(!menu.getSlot(ResearchNoteMenu.SLOT_NOTE).mayPlace(new ItemStack(Items.STONE)),
                "the note slot accepted stone");
        helper.assertTrue(!menu.getSlot(ResearchNoteMenu.SLOT_NOTE).mayPlace(noteStack(puzzleNote(3), 64)),
                "the note slot accepted the discovery family (meta 64)");
        helper.assertTrue(!menu.getSlot(ResearchNoteMenu.SLOT_NOTE).mayPlace(noteStack(completedNote(), 0)),
                "the note slot accepted a completed note (the original's data.getComplete() gate)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inkGateBlocksEveryGridWrite(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final SimpleContainerData data = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu menu = new ResearchNoteMenu(1, player.getInventory(), data);
        final ResearchNoteData note = puzzleNote(4);
        menu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(noteStack(note, 0));

        helper.assertTrue(!menu.hasInk(), "the menu reported ink with no scribing tool in the slot");

        // Original TileResearchTable.placeAspect: the entire body sits behind
        // ResearchManager.consumeInkFromTable(world, table, false).
        menu.placeAspect(0, 0, "aer", player);
        menu.placeAspect(1, 0, null, player);

        final ResearchNoteData after = ResearchNoteMenu.noteOf(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem());
        helper.assertTrue(after != null && after.hexEntries().size() == note.hexEntries().size(),
                "a hex write changed the grid with no ink: "
                        + (after == null ? "note gone" : after.hexEntries().size() + " cells"));
        helper.assertTrue(after != null && !after.complete(),
                "an inkless write completed the note");
        helper.assertTrue(data.get(ResearchNoteMenu.DATA_INK) == 0,
                "the synced ink value is " + data.get(ResearchNoteMenu.DATA_INK) + " with no scribing tool");
        helper.assertTrue(data.get(ResearchNoteMenu.DATA_NOTE_PRESENT) == 1,
                "the note-present data slot did not follow the slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dataSlotsCarryTheNoteStateToTheClient(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final SimpleContainerData serverData = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu serverMenu = new ResearchNoteMenu(1, player.getInventory(), serverData);
        final ResearchNoteData note = completedNote();
        serverMenu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(noteStack(note, 0));
        serverMenu.broadcastChanges();

        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_NOTE_PRESENT) == 1, "note-present did not sync");
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_COMPLETE) == 1, "complete did not sync");
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_GRID_COUNT) == note.hexEntries().size(),
                "the grid count did not sync: " + serverData.get(ResearchNoteMenu.DATA_GRID_COUNT)
                        + " vs " + note.hexEntries().size());
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_COPIES) == note.copies(), "copies did not sync");
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_COLOR) == note.color(), "colour did not sync");

        // The client half: the tracking client receives the server's values on
        // setData, and the menu's mirrors read them back. A fresh menu with the
        // same data (the client's own container, empty until the sync lands)
        // sees the note state only through the data slots.
        final ResearchNoteMenu clientMenu = new ResearchNoteMenu(1, player.getInventory());
        helper.assertTrue(!clientMenu.notePresent() && !clientMenu.noteComplete(),
                "the client mirror reported a note before any data arrived");
        for (int index = 0; index < ResearchNoteMenu.DATA_COUNT; index++) {
            clientMenu.setData(index, serverData.get(index));
        }
        helper.assertTrue(clientMenu.notePresent(), "the note-present mirror did not follow setData");
        helper.assertTrue(clientMenu.noteComplete(), "the complete mirror did not follow setData");
        helper.assertTrue(clientMenu.gridCount() == note.hexEntries().size(),
                "the grid-count mirror did not follow setData: " + clientMenu.gridCount());
        helper.assertTrue(clientMenu.copies() == note.copies(), "the copies mirror did not follow setData");
        helper.assertTrue(clientMenu.noteColor() == note.color(), "the colour mirror did not follow setData");
        helper.assertTrue(clientMenu.inkRemainingValue() == 0, "the ink mirror reported ink with no tool");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theServerIsTheOnlyCompletionAuthority(final GameTestHelper helper) {
        // The packet the screen sends for a hex write carries no completion
        // claim — the client's word is a grid write, nothing else.
        final java.util.List<String> components = new java.util.ArrayList<>();
        for (final java.lang.reflect.RecordComponent component
                : AspectPlaceToServerPayload.class.getRecordComponents()) {
            components.add(component.getName());
        }
        helper.assertTrue(components.contains("q") && components.contains("r")
                        && components.contains("aspectTag") && !components.contains("complete"),
                "the place packet carries a completion claim: " + components);

        final ServerPlayer player = freshPlayer(helper);
        final SimpleContainerData data = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu menu = new ResearchNoteMenu(1, player.getInventory(), data);
        final ResearchNoteData note = puzzleNote(4);
        final ItemStack stack = noteStack(note, 0);
        menu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(stack);

        // Original ContainerResearchTable.buttonClick: only case 5 exists, so a
        // client button press can never ask for completion.
        for (int id = 0; id <= 8; id++) {
            if (id == ResearchNoteMenu.BUTTON_DUPLICATE) {
                continue;
            }
            helper.assertTrue(!menu.clickMenuButton(player, id),
                    "the menu accepted button id " + id + " (the original has only case 5)");
        }
        helper.assertTrue(menu.clickMenuButton(player, ResearchNoteMenu.BUTTON_DUPLICATE),
                "the menu refused the original's RESEARCHDUPE case 5");

        // The unsolved note stays unsolved: the copy arm needs ink, and the
        // write arm is ink-gated, so no client request stamped completion.
        final ResearchNoteData after = ResearchNoteMenu.noteOf(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem());
        helper.assertTrue(after != null && !after.complete(),
                "a client arm stamped complete on an unsolved note");
        helper.assertTrue(after != null && after.copies() == 0, "a client arm bumped the copy count");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void copyArmNeedsInkBookAndCoal(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final ResearchNoteMenu menu = newMenu(player);
        final ResearchNoteData note = completedNote();
        final ItemStack stack = noteStack(note, 0);
        menu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(stack);

        // Original GuiResearchTable/TileResearchTable RESEARCHDUPE gates:
        // data.getComplete(), the ink gate, and requiresItems(book)/(coal).
        helper.assertTrue(!menu.canDuplicate(), "the copy plate lit with no ink, no book and no coal");

        player.getInventory().add(new ItemStack(Items.BOOK, 3));
        player.getInventory().add(new ItemStack(Items.COAL, 3));
        helper.assertTrue(holds(player, Items.BOOK) && holds(player, Items.COAL),
                "the book and coal did not reach the inventory");
        helper.assertTrue(!menu.canDuplicate(), "the copy plate lit with no ink");

        menu.duplicate();
        final ResearchNoteData after = ResearchNoteMenu.noteOf(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem());
        helper.assertTrue(after != null && after.copies() == 0,
                "the copy arm ran without ink: copies = " + (after == null ? "gone" : after.copies()));
        helper.assertTrue(holds(player, Items.BOOK) && holds(player, Items.COAL),
                "the copy arm spent the book and coal it did not need to spend");
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** Original {@code InventoryPlayer.hasItem(item)} — 1.21.1 has no such helper. */
    private static boolean holds(final ServerPlayer player, final net.minecraft.world.item.Item item) {
        final net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static ResearchNoteMenu newMenu(final ServerPlayer player) {
        return new ResearchNoteMenu(1, player.getInventory(), new SimpleContainerData(ResearchNoteMenu.DATA_COUNT));
    }

    /** A note with {@code count} required cells, unsolved (the generator's shape). */
    private static ResearchNoteData puzzleNote(final int count) {
        final Map<String, HexEntry> cells = new LinkedHashMap<>();
        int q = 0;
        for (int i = 0; i < count; i++) {
            cells.put(q + ":0", new HexEntry("ignis", 1));
            q++;
        }
        cells.put(count + ":0", new HexEntry(null, 0)); // a blank cell to fill
        return new ResearchNoteData(NOTE_KEY, 1, false, 0, cells);
    }

    private static ResearchNoteData completedNote() {
        final Map<String, HexEntry> cells = new LinkedHashMap<>();
        cells.put("0:0", new HexEntry("ignis", 1));
        cells.put("1:0", new HexEntry("lux", 1));
        return new ResearchNoteData(NOTE_KEY, 2, true, 1, cells);
    }

    private static ItemStack noteStack(final ResearchNoteData note, final int meta) {
        final ItemStack stack = new ItemStack(M3ResearchContent.ITEM_RESEARCH_NOTES.get());
        stack.set(ModDataComponents.RESEARCH_NOTE.get(), note);
        stack.set(ModDataComponents.ITEM_META.get(), meta);
        return stack;
    }

    /**
     * A real {@link ServerPlayer} with a declared connection, the way
     * {@code PersistenceGameTest} and {@code ResearchNoteItemGameTest} build
     * one: {@code GameTestHelper#makeMockPlayer} is not a {@code ServerPlayer},
     * and the menu arms run against {@code ServerPlayer}.
     */
    private static ServerPlayer freshPlayer(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(),
                        "tc-note-screen-" + UUID.randomUUID().toString().substring(0, 8)), false);
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
