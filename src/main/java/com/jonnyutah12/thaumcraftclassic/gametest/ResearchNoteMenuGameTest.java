package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
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
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraphLoader;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/**
 * TC-M3-04-s-iii-b: the research-note screen's server half — the
 * {@code thaumcraft:gui_research_table} menu type, its slot contract, its
 * synced note state and the note's persistence round trip.
 *
 * <p>The hex-grid rendering is the sibling card: a screen class is client-only
 * and needs a window, and the game-test harness is a dedicated server, so no
 * headless gate here can see a pixel. What runs on the server is everything
 * this card ships — the registration, the two table slots and their gates, the
 * ink gate, the write arm, the copy arm, the six {@code ContainerData} values
 * that replace the original's in-process tile reads, and the note state's
 * survival through the container, the saved-stack codec and the screen closing.</p>
 *
 * <p>Every expectation is derived from the container the menu was built with
 * and from the note record in slot 1 — no hardcoded slot list, item list or
 * note list, because a hardcoded expectation can never fail.</p>
 *
 * <ul>
 *   <li>{@link #theMenuCarriesTheContainersSlotContract}: the id is in the live
 *       menu registry, the registered object is the card's holder, the
 *       {@code MenuType} factory builds the class, and the layout is the
 *       original's — the two table slots at (14, 10)/(70, 10)
 *       ({@code ContainerResearchTable.java:19-20}), then one menu slot per
 *       {@code Inventory} slot of THIS player at the original's
 *       {@code 48 + col * 18} / {@code 175 + row * 18} and {@code 48 + i * 18}
 *       / 233 ({@code :31,35}), plus the six data slots.</li>
 *   <li>{@link #theSlotGatesAreTheOriginalsClassChecks}: both gates are the
 *       original's {@code SlotLimitedByClass} class test and nothing else
 *       ({@code SlotLimitedByClass.java:22-24}) — the note slot takes any
 *       research note, meta 64 and a completed note included, and refuses
 *       stone; the scribe slot takes only an {@code IScribeTools} stack, and
 *       the port ships none, so nothing passes it today.</li>
 *   <li>{@link #inkGateBlocksEveryGridWrite}: the original's ink gate wraps the
 *       whole write body ({@code TileResearchTable.java:127}) — with no scribe
 *       in slot 0 a write changes nothing on the note, completes nothing, and
 *       the synced ink value is 0.</li>
 *   <li>{@link #theDataSlotsAreTheScreensOnlyViewOfTheNote}: the six values
 *       carry note-present, complete, grid count, copies, ink and colour, all
 *       read off the note record in the slot; the client menu sees the note only
 *       through them, and an out-of-range index moves no mirror, which pins the
 *       width at exactly {@code DATA_COUNT}.</li>
 *   <li>{@link #theClientCannotCompleteANote}: the place payload carries no
 *       completion claim; the button arms are the original's two
 *       ({@code ContainerResearchTable.java:39-47}) — id 1 accepted as a no-op,
 *       id 5 the copy, everything else refused — and an unsolved note stays
 *       unsolved with 0 copies.</li>
 *   <li>{@link #theCopyArmIsTheOriginalsArms}: {@code TileResearchTable.java
 *       :425-462} — meta exactly 64, one book, one coal, and every tag's pool
 *       at {@code amount + copies} or better; no ink. On success the copy count
 *       goes up, the note in the slot grows by one (the original's
 *       {@code contents[1].stackSize++}) and exactly one book and one coal are
 *       spent. On a short pool nothing at all changes.</li>
 *   <li>{@link #completionStampsTheOriginalsMeta64}: the completion arm stamps
 *       the note's metadata to 64 ({@code TileResearchTable.java:178}), which
 *       is what opens the copy arm and closes the write arm.</li>
 *   <li>{@link #theNoteStateSurvivesTheServerRoundTrip}: the state rides the
 *       {@code thaumcraft:research_note} component, so it survives (a) the
 *       container's own NBT and a reopened menu, (b) the saved-stack codec the
 *       player/world save uses, and (c) the menu closing, which hands the note
 *       back to the player — the arm that keeps a solved grid from dying with
 *       the throwaway container.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchNoteMenuGameTest {

    private static final ResourceLocation MENU_ID =
            ResourceLocation.parse(ThaumcraftClassic.MOD_ID + ":gui_research_table");
    private static final String MENU_TITLE_KEY = "container.thaumcraft.gui_research_table";
    private static final String NOTE_KEY = "item." + ThaumcraftClassic.MOD_ID + ".itemresearchnotes";
    /** The original's completed-note metadata (TileResearchTable.java:178). */
    private static final int META_COMPLETED = 64;

    private ResearchNoteMenuGameTest() {
    }

    @GameTest(template = "empty")
    public static void theMenuCarriesTheContainersSlotContract(final GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.MENU.containsKey(MENU_ID),
                "thaumcraft:gui_research_table is missing from the menu registry");
        helper.assertTrue(BuiltInRegistries.MENU.get(MENU_ID) == M3ResearchContent.GUI_RESEARCH_TABLE.get(),
                "the registered menu type is not the M3ResearchContent holder");
        final ResearchNoteMenu builtByTheType = M3ResearchContent.GUI_RESEARCH_TABLE.get()
                .create(0, freshPlayer(helper).getInventory(), null);
        helper.assertTrue(ResearchNoteMenu.class == builtByTheType.getClass(),
                "the MenuType factory does not build ResearchNoteMenu: " + builtByTheType.getClass());

        final ServerPlayer player = freshPlayer(helper);
        final Inventory inventory = player.getInventory();
        final ResearchNoteMenu menu = newMenu(player);

        // Derived from the container the menu was handed, not a fixed number:
        // the original's two table slots plus one menu slot per Inventory slot.
        final int expectedSlots = ResearchNoteMenu.FIRST_PLAYER_SLOT + inventory.getContainerSize();
        helper.assertTrue(menu.slots.size() == expectedSlots,
                "the menu laid out " + menu.slots.size() + " slots, the container has "
                        + inventory.getContainerSize() + " inventory slots plus the original's "
                        + ResearchNoteMenu.FIRST_PLAYER_SLOT + " table slots");

        // The two table slots, at the original's coordinates
        // (ContainerResearchTable.java:19 scribe (14, 10), :20 note (70, 10)).
        final Slot scribe = menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE);
        final Slot note = menu.getSlot(ResearchNoteMenu.SLOT_NOTE);
        helper.assertTrue(scribe.x == 14 && scribe.y == 10,
                "the scribe slot is not the original's (14, 10) of ContainerResearchTable.java:19: "
                        + scribe.x + "," + scribe.y);
        helper.assertTrue(note.x == 70 && note.y == 10,
                "the note slot is not the original's (70, 10) of ContainerResearchTable.java:20: "
                        + note.x + "," + note.y);
        helper.assertTrue(scribe.container == note.container,
                "the two table slots are not backed by the menu's own container");
        helper.assertTrue(scribe.container.getContainerSize() == ResearchNoteMenu.FIRST_PLAYER_SLOT,
                "the table container is not the original's two-slot pair: "
                        + scribe.container.getContainerSize());

        // Every player slot of the container is in the menu, in order, bound to
        // this player's Inventory, at the original's arithmetic: rows at
        // 48 + col*18 / 175 + row*18 and the hotbar at 48 + i*18 / 233
        // (ContainerResearchTable.java:29-36).
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            final Slot slot = menu.getSlot(ResearchNoteMenu.FIRST_PLAYER_SLOT + index);
            helper.assertTrue(slot.container == inventory,
                    "menu slot " + (ResearchNoteMenu.FIRST_PLAYER_SLOT + index)
                            + " is not bound to the player's inventory");
            helper.assertTrue(slot.getSlotIndex() == index,
                    "menu slot " + (ResearchNoteMenu.FIRST_PLAYER_SLOT + index) + " reads inventory slot "
                            + slot.getSlotIndex() + ", expected " + index);
            final int expectedX;
            final int expectedY;
            if (index < 27) {
                expectedX = ResearchNoteMenu.PLAYER_SLOT_X + index % 9 * ResearchNoteMenu.PLAYER_SLOT_STEP;
                expectedY = ResearchNoteMenu.PLAYER_ROW_Y + index / 9 * ResearchNoteMenu.PLAYER_SLOT_STEP;
            } else {
                expectedX = ResearchNoteMenu.PLAYER_SLOT_X
                        + (index - 27) * ResearchNoteMenu.PLAYER_SLOT_STEP;
                expectedY = ResearchNoteMenu.HOTBAR_Y;
            }
            helper.assertTrue(slot.x == expectedX && slot.y == expectedY,
                    "menu slot " + (ResearchNoteMenu.FIRST_PLAYER_SLOT + index) + " is at (" + slot.x + ", "
                            + slot.y + "), the original's ContainerResearchTable.java:31,35 puts it at ("
                            + expectedX + ", " + expectedY + ")");
        }

        // The data slots the sibling screen draws against: an empty table
        // publishes the screen's "no note" state, and the six indices are the
        // whole contract (1.21.1 keeps the menu's dataSlots list private, so the
        // width is proved at the setData arm in the data-slot probe).
        final SimpleContainerData emptyData = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu empty = new ResearchNoteMenu(2, player.getInventory(), emptyData);
        empty.broadcastChanges();
        helper.assertTrue(emptyData.get(ResearchNoteMenu.DATA_NOTE_PRESENT) == 0
                        && emptyData.get(ResearchNoteMenu.DATA_COMPLETE) == 0
                        && emptyData.get(ResearchNoteMenu.DATA_GRID_COUNT) == 0,
                "an empty note table does not publish the screen's no-note state");
        helper.assertTrue(MENU_TITLE_KEY.equals("container.thaumcraft."
                        + M3ResearchContent.GUI_RESEARCH_TABLE.getKey().location().getPath()),
                "the menu's title key is not the registered id's key: " + MENU_TITLE_KEY);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theSlotGatesAreTheOriginalsClassChecks(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final ResearchNoteMenu menu = newMenu(player);

        // Original: SlotLimitedByClass(IScribeTools.class, ...) — a class check.
        // The port ships no scribing tool, so nothing passes it today.
        helper.assertTrue(!menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).mayPlace(new ItemStack(Items.STONE)),
                "the scribe slot accepted a non-scribing tool");
        helper.assertTrue(!menu.getSlot(ResearchNoteMenu.SLOT_SCRIBE).mayPlace(noteStack(puzzleNote(3), 0)),
                "the scribe slot accepted a research note");

        // Original: SlotLimitedByClass(ItemResearchNotes.class, ...) — the gate
        // is the class and NOTHING else (SlotLimitedByClass.java:22-24). The
        // meta-64 and completion gates live in the write arm
        // (TileResearchTable.java:129-133), not in the slot, so every note the
        // player owns fits the slot.
        final Slot noteSlot = menu.getSlot(ResearchNoteMenu.SLOT_NOTE);
        helper.assertTrue(noteSlot.mayPlace(noteStack(puzzleNote(3), 0)),
                "the note slot refused an unsolved puzzle note");
        helper.assertTrue(noteSlot.mayPlace(noteStack(completedNote(), META_COMPLETED)),
                "the note slot refused a meta-64 note: the original's slot gate is a class check "
                        + "(ContainerResearchTable.java:20), the meta gate is TileResearchTable.java:133");
        helper.assertTrue(noteSlot.mayPlace(noteStack(completedNote(), 0)),
                "the note slot refused a completed note: the original's slot gate is a class check");
        helper.assertTrue(!noteSlot.mayPlace(new ItemStack(Items.STONE)),
                "the note slot accepted stone");
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
        // ResearchManager.consumeInkFromTable(contents[0], false) — :127.
        menu.placeAspect(0, 0, "aer", player);
        menu.placeAspect(1, 0, null, player);

        final ResearchNoteData after = ResearchNoteMenu.noteOf(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem());
        helper.assertTrue(after != null && after.hexEntries().size() == note.hexEntries().size(),
                "a hex write changed the grid with no ink: "
                        + (after == null ? "note gone" : after.hexEntries().size() + " cells vs "
                        + note.hexEntries().size()));
        helper.assertTrue(after != null && !after.complete(), "an inkless write completed the note");
        helper.assertTrue(data.get(ResearchNoteMenu.DATA_INK) == 0,
                "the synced ink value is " + data.get(ResearchNoteMenu.DATA_INK) + " with no scribing tool");
        helper.assertTrue(data.get(ResearchNoteMenu.DATA_NOTE_PRESENT) == 1,
                "the note-present data slot did not follow the slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theDataSlotsAreTheScreensOnlyViewOfTheNote(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final SimpleContainerData serverData = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu serverMenu = new ResearchNoteMenu(1, player.getInventory(), serverData);
        final ResearchNoteData note = completedNote();
        serverMenu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(noteStack(note, 3));
        serverMenu.broadcastChanges();

        // Every value is read off the note record in the slot.
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_NOTE_PRESENT) == 1, "note-present did not sync");
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_COMPLETE) == 1,
                "complete did not sync: the slot's note is complete");
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_GRID_COUNT) == note.hexEntries().size(),
                "the grid count did not sync: " + serverData.get(ResearchNoteMenu.DATA_GRID_COUNT)
                        + " vs " + note.hexEntries().size());
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_COPIES) == note.copies(),
                "copies did not sync: " + serverData.get(ResearchNoteMenu.DATA_COPIES) + " vs " + note.copies());
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_COLOR) == note.color(),
                "colour did not sync: " + serverData.get(ResearchNoteMenu.DATA_COLOR) + " vs " + note.color());
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_INK) == 0,
                "the ink value reported ink with no scribing tool");

        // The client half: a menu of the same type, no note in its slot, sees
        // the note state ONLY through the synced data values.
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
        helper.assertTrue(clientMenu.copies() == note.copies(),
                "the copies mirror did not follow setData: " + clientMenu.copies());
        helper.assertTrue(clientMenu.noteColor() == note.color(),
                "the colour mirror did not follow setData: " + clientMenu.noteColor());
        helper.assertTrue(clientMenu.inkRemainingValue() == 0, "the ink mirror reported ink with no tool");

        // The width: an index past the last data value moves no mirror, which
        // pins the screen's contract at exactly DATA_COUNT values.
        clientMenu.setData(ResearchNoteMenu.DATA_COUNT, 7);
        clientMenu.setData(-1, 7);
        helper.assertTrue(clientMenu.gridCount() == note.hexEntries().size()
                        && clientMenu.copies() == note.copies() && clientMenu.noteColor() == note.color(),
                "an out-of-range data index moved the mirror: the screen's data width is not "
                        + ResearchNoteMenu.DATA_COUNT);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theClientCannotCompleteANote(final GameTestHelper helper) {
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
        menu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(noteStack(note, 0));

        // Original ContainerResearchTable.java:39-47: exactly two arms — id 1
        // accepted as a no-op, id 5 the copy. Everything else is refused.
        for (int id = 0; id <= 8; id++) {
            if (id == ResearchNoteMenu.BUTTON_KEEP_OPEN || id == ResearchNoteMenu.BUTTON_DUPLICATE) {
                continue;
            }
            helper.assertTrue(!menu.clickMenuButton(player, id),
                    "the menu accepted button id " + id + "; the original has only 1 and 5 "
                            + "(ContainerResearchTable.java:39-47)");
        }
        helper.assertTrue(menu.clickMenuButton(player, ResearchNoteMenu.BUTTON_KEEP_OPEN),
                "the menu refused the original's accepted no-op button 1 (ContainerResearchTable.java:40-42)");
        helper.assertTrue(menu.clickMenuButton(player, ResearchNoteMenu.BUTTON_DUPLICATE),
                "the menu refused the original's RESEARCHDUPE case 5 (ContainerResearchTable.java:43-45)");

        // Neither arm completed the note: the write arm is ink-gated and the
        // copy arm requires meta 64, so no client request stamped completion.
        final ResearchNoteData after = ResearchNoteMenu.noteOf(menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem());
        helper.assertTrue(after != null && !after.complete(),
                "a client arm stamped complete on an unsolved note");
        helper.assertTrue(after != null && after.copies() == 0, "a client arm bumped the copy count");
        helper.assertTrue(data.get(ResearchNoteMenu.DATA_COMPLETE) == 0,
                "the synced complete flag is not the state in the slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theCopyArmIsTheOriginalsArms(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        // Clear the harness kit so the book/coal counts are the probe's own.
        final Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            inventory.setItem(slot, ItemStack.EMPTY);
        }

        // A real graph entry supplies the tags and the cost: derived, not typed.
        final String key = aTaggedResearchKey();
        final ResearchEntryData entry = ResearchManager.graph().entry(key);
        helper.assertTrue(entry != null && entry.tags() != null && !entry.tags().isEmpty(),
                "the shipped graph has no tagged entry to copy: " + key);

        final SimpleContainerData data = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu menu = new ResearchNoteMenu(1, player.getInventory(), data);
        final ItemStack stack = noteStack(new ResearchNoteData(key, 1, true, 0, twoCells()), META_COMPLETED);
        final Slot noteSlot = menu.getSlot(ResearchNoteMenu.SLOT_NOTE);
        noteSlot.set(stack);

        // No scribe in slot 0: the original's copy arm has no ink gate, so the
        // plate must light on the note alone (GuiResearchTable.java:104,254 —
        // note != null && RESEARCHDUPE && note.isComplete()).
        // The plate gate is the original's: note + RESEARCHDUPE + complete, and
        // NO ink (GuiResearchTable.java:104,254). The scribe slot is empty here,
        // so a plate that needs ink fails this assertion.
        setKnowledge(player, fundedPool(entry, 0).withResearchCompleted("RESEARCHDUPE"));
        helper.assertTrue(menu.canDuplicate(),
                "the copy plate stayed dark with a completed meta-64 note, RESEARCHDUPE earned and an "
                        + "empty scribe slot: the port put an ink gate on the copy arm the original never had");

        // The cost panel's numbers, derived from the entry: amount + copies.
        final Map<String, Integer> costs = menu.copyCosts();
        helper.assertTrue(costs.size() == entry.tags().size(),
                "the cost panel shows " + costs.size() + " tags for an entry with " + entry.tags().size());
        for (final var tag : entry.tags()) {
            helper.assertTrue(costs.getOrDefault(tag.aspect(), -1) == tag.amount(),
                    "the cost panel for " + tag.aspect() + " is " + costs.get(tag.aspect())
                            + ", the entry's amount is " + tag.amount() + " with 0 copies");
        }

        // A short pool: the original returns and NOTHING changes (:441-445).
        inventory.add(new ItemStack(Items.BOOK, 3));
        inventory.add(new ItemStack(Items.COAL, 3));
        setKnowledge(player, fundedPool(entry, -1).withResearchCompleted("RESEARCHDUPE"));
        menu.duplicate();
        helper.assertTrue(ResearchNoteMenu.noteOf(noteSlot.getItem()).copies() == 0,
                "the copy arm ran on a short pool");
        helper.assertTrue(count(player, Items.BOOK) == 3 && count(player, Items.COAL) == 3,
                "the copy arm spent book/coal it refused to use");

        // The funded run: pool >= amount + copies for every tag.
        final PlayerKnowledgeData funded = fundedPool(entry, 0).withResearchCompleted("RESEARCHDUPE");
        setKnowledge(player, funded);
        menu.duplicate();

        final ResearchNoteData copied = ResearchNoteMenu.noteOf(noteSlot.getItem());
        helper.assertTrue(copied.copies() == 1,
                "the copy count is " + copied.copies() + " after a funded copy (the original's data.copies++)");
        helper.assertTrue(noteSlot.getItem().getCount() == 2,
                "the note in the slot is " + noteSlot.getItem().getCount() + " tall; the original's "
                        + "TileResearchTable.java:459 is contents[1].stackSize++");
        helper.assertTrue(count(player, Items.BOOK) == 2,
                "the copy spent " + (3 - count(player, Items.BOOK)) + " books, the original spends 1");
        helper.assertTrue(count(player, Items.COAL) == 2,
                "the copy spent " + (3 - count(player, Items.COAL)) + " coal, the original spends 1");
        for (final var tag : entry.tags()) {
            helper.assertTrue(player.getData(ModAttachments.PLAYER_KNOWLEDGE).aspectAmount(tag.aspect())
                            == funded.aspectAmount(tag.aspect()) - tag.amount(),
                    "the pool for " + tag.aspect() + " paid "
                            + (funded.aspectAmount(tag.aspect())
                            - player.getData(ModAttachments.PLAYER_KNOWLEDGE).aspectAmount(tag.aspect()))
                            + ", the original pays amount + copies = " + tag.amount());
        }
        helper.assertTrue(data.get(ResearchNoteMenu.DATA_COPIES) == 1,
                "the synced copy count did not follow the copy: " + data.get(ResearchNoteMenu.DATA_COPIES));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void completionStampsTheOriginalsMeta64(final GameTestHelper helper) {
        final ItemStack stack = noteStack(puzzleNote(3), 0);
        helper.assertTrue(ItemResearchNotes.storedMeta(stack) != META_COMPLETED,
                "a fresh puzzle note must not start at the completed meta");
        final ResearchNoteData completed = completedNote();

        // TileResearchTable.java:177-178: checkResearchCompletion, then
        // contents[1].setItemDamage(64).
        ResearchNoteMenu.applyCompletion(stack, completed);
        helper.assertTrue(ItemResearchNotes.storedMeta(stack) == META_COMPLETED,
                "the completion arm did not stamp meta 64 (TileResearchTable.java:178): meta is "
                        + ItemResearchNotes.storedMeta(stack));
        helper.assertTrue(ItemResearchNotes.META_DISCOVERY_FAMILY == META_COMPLETED,
                "the port's completed-note meta is not the original's 64: "
                        + ItemResearchNotes.META_DISCOVERY_FAMILY);
        helper.assertTrue(completed.equals(ResearchNoteMenu.noteOf(stack)),
                "the completion arm did not write the completed state back onto the stack");

        // The stamp is what makes the note copyable and not writeable: meta 64
        // is the gate the copy arm requires and the write arm refuses.
        final ServerPlayer player = freshPlayer(helper);
        final ResearchNoteMenu menu = new ResearchNoteMenu(1, player.getInventory(),
                new SimpleContainerData(ResearchNoteMenu.DATA_COUNT));
        menu.getSlot(ResearchNoteMenu.SLOT_NOTE).set(stack);
        menu.broadcastChanges();
        helper.assertTrue(menu.noteComplete(), "the completed note's complete data slot did not sync");
        helper.assertTrue(!menu.canDuplicate(),
                "the plate lit for a note whose RESEARCHDUPE research the player has not earned");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theNoteStateSurvivesTheServerRoundTrip(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final ResearchNoteData grid = puzzleNote(5);
        final ItemStack original = noteStack(grid, 0);

        final SimpleContainerData serverData = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu menu = new ResearchNoteMenu(1, player.getInventory(), serverData);
        final Slot noteSlot = menu.getSlot(ResearchNoteMenu.SLOT_NOTE);
        noteSlot.set(original);
        menu.broadcastChanges();

        // (a) The synced values are read off the state in the slot.
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_NOTE_PRESENT) == 1,
                "the note-present data slot did not follow the slot");
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_COMPLETE) == (grid.complete() ? 1 : 0),
                "the complete data slot is not the state in the slot");
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_GRID_COUNT) == grid.hexEntries().size(),
                "the grid count is " + serverData.get(ResearchNoteMenu.DATA_GRID_COUNT) + ", the slot holds "
                        + grid.hexEntries().size() + " cells");
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_COPIES) == grid.copies(),
                "the copy count is not the state in the slot");
        helper.assertTrue(serverData.get(ResearchNoteMenu.DATA_COLOR) == grid.color(),
                "the colour data slot is not the state in the slot");

        // (b) The container's own NBT — the save shape the port's containers use
        // — round-trips into a fresh menu with the grid state intact: the state
        // rides the stack, so no side table is needed for it to survive.
        final SimpleContainer table = menu.tableContainer();
        final ListTag saved = table.createTag(helper.getLevel().registryAccess());
        final SimpleContainer restored = new SimpleContainer(table.getContainerSize());
        restored.fromTag(saved, helper.getLevel().registryAccess());
        final ItemStack throughContainer = restored.getItem(ResearchNoteMenu.SLOT_NOTE);
        helper.assertTrue(!throughContainer.isEmpty(),
                "the note vanished through the container NBT round trip (" + saved.size() + " entries saved)");
        helper.assertTrue(grid.equals(ResearchNoteMenu.noteOf(throughContainer)),
                "the grid state did not survive the container NBT round trip: "
                        + ResearchNoteMenu.noteOf(throughContainer) + " vs " + grid);

        final SimpleContainerData reopenedData = new SimpleContainerData(ResearchNoteMenu.DATA_COUNT);
        final ResearchNoteMenu reopened = new ResearchNoteMenu(1, player.getInventory(), reopenedData);
        reopened.getSlot(ResearchNoteMenu.SLOT_NOTE).set(throughContainer);
        reopened.broadcastChanges();
        for (int index = 0; index < ResearchNoteMenu.DATA_COUNT; index++) {
            helper.assertTrue(reopenedData.get(index) == serverData.get(index),
                    "the reopened menu's data slot " + index + " reads " + reopenedData.get(index)
                            + ", the first menu published " + serverData.get(index));
        }

        // (c) The saved-stack codec — the path the player and world save use —
        // carries the same state, so the grid survives a server restart.
        final CompoundTag stackTag = (CompoundTag) original.save(helper.getLevel().registryAccess());
        final ItemStack decoded = ItemStack.parseOptional(helper.getLevel().registryAccess(), stackTag);
        helper.assertTrue(!decoded.isEmpty(), "the note stack did not decode from its saved tag");
        helper.assertTrue(grid.equals(ResearchNoteMenu.noteOf(decoded)),
                "the saved-stack codec lost the grid state: "
                        + ResearchNoteMenu.noteOf(decoded) + " vs " + grid);

        // (d) Closing the menu hands the note back with its grid state intact.
        // The table container is a throwaway SimpleContainer (the original's
        // pair lived in the tile), so without that arm a solved grid dies with
        // the container when the screen closes.
        menu.removed(player);
        final Inventory inventory = player.getInventory();
        ResearchNoteData throughClose = null;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ResearchNoteData candidate = ResearchNoteMenu.noteOf(inventory.getItem(slot));
            if (candidate != null) {
                throughClose = candidate;
                break;
            }
        }
        helper.assertTrue(throughClose != null,
                "closing the note menu did not return the note to the player's inventory");
        helper.assertTrue(grid.equals(throughClose),
                "the grid state did not survive the menu closing: " + throughClose + " vs " + grid);
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** A graph entry that carries essentia tags, so the copy arm has a cost. */
    private static String aTaggedResearchKey() {
        for (final ResearchEntryData entry : ResearchGraphLoader.loadDefault().entries()) {
            if (entry.tags() != null && !entry.tags().isEmpty()) {
                return entry.key();
            }
        }
        throw new IllegalStateException("the shipped graph has no tagged entry");
    }

    /** A pool that can pay every tag (offset -1 forces the short-pool case). */
    private static PlayerKnowledgeData fundedPool(final ResearchEntryData entry, final int offset) {
        PlayerKnowledgeData knowledge = PlayerKnowledgeData.empty();
        for (final var tag : entry.tags()) {
            knowledge = knowledge.withAspectPoolAdded(tag.aspect(), tag.amount() + 5 + offset);
        }
        return knowledge;
    }

    private static void setKnowledge(final ServerPlayer player, final PlayerKnowledgeData knowledge) {
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
    }

    private static int count(final ServerPlayer player, final net.minecraft.world.item.Item item) {
        return player.getInventory().countItem(item);
    }

    private static Map<String, HexEntry> twoCells() {
        final Map<String, HexEntry> cells = new LinkedHashMap<>();
        cells.put("0:0", new HexEntry("ignis", 1));
        cells.put("1:0", new HexEntry("lux", 1));
        return cells;
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
        return new ResearchNoteData(NOTE_KEY, 2, true, 1, twoCells());
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
