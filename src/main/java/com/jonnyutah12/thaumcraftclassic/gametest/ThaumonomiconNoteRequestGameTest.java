package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.api.IScribeTools;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.PlayerCompleteToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ResearchCompletePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.research.book.BookNoteRequest;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconKnowledge;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/**
 * TC-M3-04-s-v: the Thaumonomicon's note-completion interaction against the
 * live server (the original {@code GuiResearchBrowser.func_73864_a} click +
 * {@code PacketPlayerCompleteToServer.onMessage}).
 *
 * <p>The probes drive the payload handler the book's click reaches —
 * {@link ResearchManager#handlePlayerCompleteRequest} — on a real
 * {@link ServerPlayer} with a declared connection, the way
 * {@code ResearchNoteItemGameTest} does, because the completion arm answers
 * with payloads a mock player's null connection cannot carry. The point of
 * every probe is the card's seam rule: the book only <em>asks</em>. The note
 * arm hands over a puzzle note and spends one durability use of the scribe
 * tool; it never completes research, which stays the authority of
 * {@code ResearchManager.completeResearch} reached through the note item
 * ({@code ItemResearchNotes.completeWithSiblings}) and the purchase arm.</p>
 *
 * <p>Entries used are real graph rows: {@code GOLEMSTRAW} (tags, not secondary
 * → the note family; parent {@code HUNGRYCHEST}), {@code HUNGRYCHEST}
 * (secondary, tags {@code fames 3 + vacuos 3}, no parents → the purchase
 * family) and {@code GOLEMBELL} (parent {@code GOLEMSTRAW} → locked until that
 * note is solved).</p>
 *
 * <ul>
 *   <li>{@link #scribeToolsAreTheShippedInkSource}: {@code iteminkwell} is
 *       registered, is an {@link IScribeTools}, carries the original's
 *       stack-1/durability-100 constructor, and the ink gate is the scribe tool
 *       plus paper.</li>
 *   <li>{@link #noteRequestPaysOneInkUseAndHandsOverAPuzzleNote}: the note arm
 *       spends exactly one durability use, leaves the paper, and puts an
 *       unsolved generated note for the entry in the inventory — and the entry
 *       is still unknown, because the book does not complete research.</li>
 *   <li>{@link #carryingTheNoteMakesAFurtherRequestFree}: the original's
 *       {@code getResearchSlot} gate — a second request while the note is
 *       carried is a no-op that costs no ink.</li>
 *   <li>{@link #requestWithoutInkIsRejected}: scribe tool without paper,
 *       and paper without a scribe tool: no note, and no ink spent.</li>
 *   <li>{@link #lockedEntryRequestIsRejected}: the server's requisites gate,
 *       so a spoofed request for a locked entry cannot buy a note.</li>
 *   <li>{@link #noteDropsWhenTheInventoryIsFull}: the original's
 *       {@code dropPlayerItemWithRandomChoice} fallback.</li>
 *   <li>{@link #purchaseRequestCompletesThroughTheShippedAuthority}: the
 *       purchase arm pays the tag costs out of the aspect pool and completes
 *       through {@code completeResearch} — the same path the note item drives,
 *       not a second one.</li>
 *   <li>{@link #bookDecisionMatchesTheOriginalArms}: the click decision the
 *       screen computes, against the real graph, a real inventory and the real
 *       config difficulty — get-note, has-note, no-scribing-tools, missing
 *       prerequisite, purchase, can't-purchase, known.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThaumonomiconNoteRequestGameTest {

    private static final ResourceLocation INKWELL_ID =
            ResourceLocation.parse(ThaumcraftClassic.MOD_ID + ":iteminkwell");
    private static final ResourceLocation NOTE_ID =
            ResourceLocation.parse(ThaumcraftClassic.MOD_ID + ":itemresearchnotes");

    /** Primary family (tags, not secondary): the note arm. */
    private static final String NOTE_ENTRY = "GOLEMSTRAW";
    /** Its only prerequisite. */
    private static final String NOTE_PARENT = "HUNGRYCHEST";
    /** Secondary family with tags: the purchase arm. */
    private static final String PURCHASE_ENTRY = "HUNGRYCHEST";
    /** Gated on GOLEMSTRAW, so locked for every player who has not solved it. */
    private static final String LOCKED_ENTRY = "GOLEMBELL";

    private ThaumonomiconNoteRequestGameTest() {
    }

    @GameTest(template = "empty")
    public static void scribeToolsAreTheShippedInkSource(final GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(INKWELL_ID),
                "thaumcraft:iteminkwell is missing from the item registry");
        final Item registered = BuiltInRegistries.ITEM.get(INKWELL_ID);
        helper.assertTrue(registered == M3ResearchContent.ITEM_INKWELL.get(),
                "the registered scribe tool is not the M3ResearchContent holder: " + registered);
        helper.assertTrue(registered instanceof IScribeTools,
                "the scribe tool does not implement IScribeTools, so consumeInkFromPlayer cannot see it");

        final ItemStack stack = new ItemStack(registered);
        helper.assertTrue(stack.getMaxStackSize() == 1,
                "the original's setMaxStackSize(1) is missing: stacks to " + stack.getMaxStackSize());
        helper.assertTrue(stack.getMaxDamage() == 100,
                "the original's setMaxDamage(100) is missing: durability is " + stack.getMaxDamage());
        helper.assertTrue(stack.getDescriptionId().equals("item.thaumcraft.iteminkwell"),
                "the name key is " + stack.getDescriptionId());

        helper.assertTrue(ResearchManager.hasScribingInk(rows(stack, new ItemStack(Items.PAPER))),
                "a scribe tool with ink plus paper is the original's hasScribestuff (:104-108)");
        final ItemStack dry = new ItemStack(registered);
        dry.setDamageValue(dry.getMaxDamage());
        helper.assertTrue(!ResearchManager.hasScribingInk(rows(dry, new ItemStack(Items.PAPER))),
                "a scribe tool out of ink is not a source (getDamageValue < getMaxDamage)");
        helper.assertTrue(!ResearchManager.hasScribingInk(rows(new ItemStack(Items.PAPER))),
                "paper alone is not a scribe tool");
        helper.assertTrue(ResearchManager.consumeInkFromPlayer(rows(new ItemStack(registered)), false),
                "consumeInkFromPlayer's gate is the scribe tool alone (ResearchManager.java:204-217)");
        helper.assertTrue(!ResearchManager.carriesPaper(rows(new ItemStack(registered))),
                "the paper is the other half of the gate, and it is not consumed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noteRequestPaysOneInkUseAndHandsOverAPuzzleNote(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        player.setData(ModAttachments.PLAYER_KNOWLEDGE,
                PlayerKnowledgeData.empty().withResearchCompleted(NOTE_PARENT));
        final ItemStack inkwell = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        player.getInventory().items.set(0, inkwell);
        player.getInventory().items.set(1, new ItemStack(Items.PAPER));

        ResearchManager.handlePlayerCompleteRequest(
                new PlayerCompleteToServerPayload(NOTE_ENTRY, PlayerCompleteToServerPayload.REQUEST_NOTE),
                player);

        final ItemStack note = findNote(player, NOTE_ENTRY);
        helper.assertTrue(note != null, "the note request left no research note in the inventory");
        final ResearchNoteData data = note.get(ModDataComponents.RESEARCH_NOTE.get());
        helper.assertTrue(data != null && NOTE_ENTRY.equals(data.key()),
                "the note does not name " + NOTE_ENTRY + ": " + data);
        helper.assertTrue(!data.complete(), "a freshly scribed note must not be solved");
        helper.assertTrue(data.copies() == 0, "a freshly scribed note has no copies: " + data.copies());
        helper.assertTrue(!data.hexEntries().isEmpty(), "the note carries no generated hex grid");

        helper.assertTrue(inkwell.getDamageValue() == 1,
                "the note costs exactly one durability use (damageItem(1, player)): " + inkwell.getDamageValue());
        helper.assertTrue(count(player, new ItemStack(Items.PAPER)) == 1,
                "the original consumes durability from the scribe tool, never the paper");
        helper.assertTrue(count(player, new ItemStack(M3ResearchContent.ITEM_RESEARCH_NOTES.get())) == 1,
                "exactly one note should have landed");

        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(!ResearchManager.isResearchComplete(knowledge, NOTE_ENTRY),
                "the book's note arm must never complete research — the note item is the authority");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void carryingTheNoteMakesAFurtherRequestFree(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        player.setData(ModAttachments.PLAYER_KNOWLEDGE,
                PlayerKnowledgeData.empty().withResearchCompleted(NOTE_PARENT));
        final ItemStack inkwell = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        player.getInventory().items.set(0, inkwell);
        player.getInventory().items.set(1, new ItemStack(Items.PAPER));

        ResearchManager.handlePlayerCompleteRequest(
                new PlayerCompleteToServerPayload(NOTE_ENTRY, PlayerCompleteToServerPayload.REQUEST_NOTE),
                player);
        ResearchManager.handlePlayerCompleteRequest(
                new PlayerCompleteToServerPayload(NOTE_ENTRY, PlayerCompleteToServerPayload.REQUEST_NOTE),
                player);

        helper.assertTrue(count(player, new ItemStack(M3ResearchContent.ITEM_RESEARCH_NOTES.get())) == 1,
                "the original's getResearchSlot gate: a player carrying the note gets no second note");
        helper.assertTrue(inkwell.getDamageValue() == 1,
                "the second request is the original's early return — no extra ink: " + inkwell.getDamageValue());
        helper.assertTrue(ResearchManager.noteSlot(player.getInventory().items, NOTE_ENTRY) >= 0,
                "getResearchSlot must find the carried note");
        helper.assertTrue(ResearchManager.noteSlot(player.getInventory().items, LOCKED_ENTRY) < 0,
                "getResearchSlot must not match a note for a different entry");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void requestWithoutInkIsRejected(final GameTestHelper helper) {
        // Scribe tool, no paper: the server's inventory.hasItem(paper) gate
        // (ResearchManager.java:108, Items.field_151121_aF = paper).
        final ServerPlayer noSac = freshPlayer(helper);
        noSac.setData(ModAttachments.PLAYER_KNOWLEDGE,
                PlayerKnowledgeData.empty().withResearchCompleted(NOTE_PARENT));
        final ItemStack tool = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        noSac.getInventory().items.set(0, tool);
        ResearchManager.handlePlayerCompleteRequest(
                new PlayerCompleteToServerPayload(NOTE_ENTRY, PlayerCompleteToServerPayload.REQUEST_NOTE),
                noSac);
        helper.assertTrue(findNote(noSac, NOTE_ENTRY) == null,
                "paper is required (ResearchManager.java:108), no note without one");
        helper.assertTrue(tool.getDamageValue() == 0,
                "the rejected request must not spend ink: " + tool.getDamageValue());

        // Paper, no scribe tool: consumeInkFromPlayer finds no source.
        final ServerPlayer noTool = freshPlayer(helper);
        noTool.setData(ModAttachments.PLAYER_KNOWLEDGE,
                PlayerKnowledgeData.empty().withResearchCompleted(NOTE_PARENT));
        noTool.getInventory().items.set(0, new ItemStack(Items.PAPER));
        ResearchManager.handlePlayerCompleteRequest(
                new PlayerCompleteToServerPayload(NOTE_ENTRY, PlayerCompleteToServerPayload.REQUEST_NOTE),
                noTool);
        helper.assertTrue(findNote(noTool, NOTE_ENTRY) == null,
                "a scribe tool is required (GuiResearchBrowser.java:104-108), no note without one");
        helper.assertTrue(count(noTool, new ItemStack(Items.PAPER)) == 1,
                "the paper is never consumed by the gate");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lockedEntryRequestIsRejected(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        // GOLEMSTRAW unknown, so GOLEMBELL's prerequisite is unmet. The player
        // is fully equipped for scribing: the gate is the requisites check, not
        // the ink, which is what makes this a spoof-resistance probe.
        player.setData(ModAttachments.PLAYER_KNOWLEDGE,
                PlayerKnowledgeData.empty().withResearchCompleted("INFUSION"));
        final ItemStack inkwell = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        player.getInventory().items.set(0, inkwell);
        player.getInventory().items.set(1, new ItemStack(Items.PAPER));

        ResearchManager.handlePlayerCompleteRequest(
                new PlayerCompleteToServerPayload(LOCKED_ENTRY, PlayerCompleteToServerPayload.REQUEST_NOTE),
                player);

        helper.assertTrue(findNote(player, LOCKED_ENTRY) == null,
                "doesPlayerHaveRequisites gates the request server-side, as the original's "
                        + "PacketPlayerCompleteToServer.onMessage did");
        helper.assertTrue(inkwell.getDamageValue() == 0, "a rejected request spends no ink");
        helper.assertTrue(!ResearchManager.isResearchComplete(player.getData(ModAttachments.PLAYER_KNOWLEDGE),
                NOTE_PARENT), "the probe's own starting state: its parent HUNGRYCHEST is unknown, so "
                        + "GOLEMBELL is locked and GOLEMSTRAW is the note family");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noteDropsWhenTheInventoryIsFull(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        player.setData(ModAttachments.PLAYER_KNOWLEDGE,
                PlayerKnowledgeData.empty().withResearchCompleted(NOTE_PARENT));
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            player.getInventory().items.set(slot, new ItemStack(Items.DIRT, 64));
        }
        player.getInventory().items.set(0, new ItemStack(M3ResearchContent.ITEM_INKWELL.get()));
        player.getInventory().items.set(1, new ItemStack(Items.PAPER));

        ResearchManager.handlePlayerCompleteRequest(
                new PlayerCompleteToServerPayload(NOTE_ENTRY, PlayerCompleteToServerPayload.REQUEST_NOTE),
                player);

        helper.assertTrue(findNote(player, NOTE_ENTRY) == null,
                "a full inventory cannot take the note");
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        final List<ItemEntity> drops = player.level().getEntities(EntityType.ITEM,
                new AABB(origin.getX() - 8, origin.getY() - 8, origin.getZ() - 8,
                        origin.getX() + 8, origin.getY() + 8, origin.getZ() + 8),
                entity -> true);
        boolean seen = false;
        for (final ItemEntity drop : drops) {
            final ResearchNoteData data = drop.getItem().get(ModDataComponents.RESEARCH_NOTE.get());
            if (drop.getItem().getItem() == M3ResearchContent.ITEM_RESEARCH_NOTES.get()
                    && data != null && NOTE_ENTRY.equals(data.key())) {
                seen = true;
            }
        }
        helper.assertTrue(seen,
                "the note the inventory cannot hold lands at the player's feet (dropPlayerItemWithRandomChoice)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void purchaseRequestCompletesThroughTheShippedAuthority(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        PlayerKnowledgeData knowledge = PlayerKnowledgeData.empty();
        for (final ResearchEntryData.AspectTag tag : ResearchManager.graph().entry(PURCHASE_ENTRY).tags()) {
            knowledge = knowledge.withAspectPoolAdded(tag.aspect(), tag.amount());
        }
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
        final ItemStack inkwell = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        player.getInventory().items.set(0, inkwell);

        ResearchManager.handlePlayerCompleteRequest(
                new PlayerCompleteToServerPayload(PURCHASE_ENTRY, PlayerCompleteToServerPayload.REQUEST_COMPLETE),
                player);

        final PlayerKnowledgeData after = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(ResearchManager.isResearchComplete(after, PURCHASE_ENTRY),
                "the purchase arm completes the entry");
        for (final ResearchEntryData.AspectTag tag : ResearchManager.graph().entry(PURCHASE_ENTRY).tags()) {
            helper.assertTrue(after.aspectAmount(tag.aspect()) == 0,
                    "the tag cost was paid out of the aspect pool: " + tag.aspect() + " = "
                            + after.aspectAmount(tag.aspect()));
        }
        helper.assertTrue(inkwell.getDamageValue() == 0,
                "a purchase pays aspects, never ink (the original's secondary-research rule)");
        helper.assertTrue(count(player, new ItemStack(M3ResearchContent.ITEM_RESEARCH_NOTES.get())) == 0,
                "a purchase never prints a note");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bookDecisionMatchesTheOriginalArms(final GameTestHelper helper) {
        final int difficulty = com.jonnyutah12.thaumcraftclassic.config.TcConfig
                .getInt(com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog.RESEARCH_DIFFICULTY);
        final var graph = ResearchManager.graph();
        PlayerKnowledgeData funded = PlayerKnowledgeData.empty().withResearchCompleted(NOTE_PARENT);
        for (final ResearchEntryData.AspectTag tag : graph.entry(PURCHASE_ENTRY).tags()) {
            funded = funded.withAspectPoolAdded(tag.aspect(), tag.amount());
        }
        final PlayerKnowledgeData rich = funded;
        final ThaumonomiconKnowledge knowsParent = key -> NOTE_PARENT.equals(key);
        final ThaumonomiconKnowledge knowsNothing = key -> false;
        final ThaumonomiconKnowledge knowsPurchase = key -> PURCHASE_ENTRY.equals(key);

        final ItemStack inkwell = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        final List<ItemStack> inkRows = rows(inkwell, new ItemStack(Items.PAPER));
        final List<ItemStack> dryRows = List.of(new ItemStack(Items.DIRT));

        // The note family, equipped: the original's "Click to get a research
        // note for this research" (GuiResearchBrowser.java:616).
        final BookNoteRequest.Request noteArm = decide(graph, NOTE_ENTRY, knowsParent,
                difficulty, true, false, rich);
        helper.assertTrue(noteArm.arm() == BookNoteRequest.Arm.NOTE
                        && BookNoteRequest.KEY_GET_PRIM.equals(noteArm.hintKey()),
                "an unlocked primary entry with scribing ink is the note arm: " + noteArm.hintKey());

        // Carrying the note: "You already have this research note in your
        // inventory" (:614), a dead click.
        final BookNoteRequest.Request carried = decide(graph, NOTE_ENTRY, knowsParent,
                difficulty, true, true, rich);
        helper.assertTrue(!carried.live() && BookNoteRequest.KEY_HAS_NOTE.equals(carried.hintKey()),
                "a carried note blocks a second request: " + carried.hintKey());

        // No scribing ink: "You need scribing tools and paper..." (:618).
        final BookNoteRequest.Request dry = decide(graph, NOTE_ENTRY, knowsParent,
                difficulty, false, false, rich);
        helper.assertTrue(!dry.live() && BookNoteRequest.KEY_SHORT_PRIM.equals(dry.hintKey()),
                "no ink names the missing tools: " + dry.hintKey());

        // Locked: "Missing required research!" (:560), the only line the
        // original draws in its dark-salmon prerequisite colour.
        final BookNoteRequest.Request locked = decide(graph, LOCKED_ENTRY, knowsNothing,
                difficulty, true, false, rich);
        helper.assertTrue(!locked.live() && BookNoteRequest.KEY_MISSING.equals(locked.hintKey()),
                "unmet prerequisites are the missing-research line: " + locked.hintKey());
        helper.assertTrue(BookNoteRequest.colorFor(BookNoteRequest.KEY_MISSING) == BookNoteRequest.COLOR_MISSING,
                "the prerequisite line keeps the original's colour");

        // The purchase family, payable and not: "Click to purchase this
        // research" (:656) / "You do not have the required aspects..." (:658).
        final BookNoteRequest.Request payable = decide(graph, PURCHASE_ENTRY, knowsNothing,
                difficulty, true, false, rich);
        helper.assertTrue(payable.arm() == BookNoteRequest.Arm.PURCHASE
                        && BookNoteRequest.KEY_PURCHASE.equals(payable.hintKey()),
                "a payable secondary (difficulty 0) is the purchase arm: " + payable.hintKey());
        final BookNoteRequest.Request broke = decide(graph, PURCHASE_ENTRY, knowsNothing,
                difficulty, true, false, PlayerKnowledgeData.empty());
        helper.assertTrue(!broke.live() && BookNoteRequest.KEY_SHORT.equals(broke.hintKey()),
                "an unpayable secondary is the short line: " + broke.hintKey());

        // Known entry: the original opens the entry view and sends nothing.
        final BookNoteRequest.Request known = decide(graph, PURCHASE_ENTRY, knowsPurchase,
                difficulty, true, false, rich);
        helper.assertTrue(!known.live() && known.hintKey() == null,
                "a known entry is not a request and shows no hint line");

        // The gate the screen reads from its inventory view, on a real list.
        helper.assertTrue(ResearchManager.hasScribingInk(inkRows), "the equipped rows carry ink");
        helper.assertTrue(!ResearchManager.hasScribingInk(dryRows), "dirt is not ink");
        helper.succeed();
    }

    // ------------------------------------------------------------------ probes

    private static BookNoteRequest.Request decide(final com.jonnyutah12.thaumcraftclassic.research.ResearchGraph graph,
                                                  final String key,
                                                  final ThaumonomiconKnowledge knowledge,
                                                  final int difficulty,
                                                  final boolean scribingInk,
                                                  final boolean carriesNote,
                                                  final PlayerKnowledgeData pool) {
        return BookNoteRequest.decide(graph, graph.entry(key), knowledge, difficulty,
                scribingInk, carriesNote, tag -> pool.aspectAmount(tag));
    }

    private static ItemStack findNote(final ServerPlayer player, final String key) {
        return ResearchManager.noteSlot(player.getInventory().items, key) >= 0
                ? player.getInventory().items.get(ResearchManager.noteSlot(player.getInventory().items, key))
                : null;
    }

    private static int count(final ServerPlayer player, final ItemStack probe) {
        int total = 0;
        for (final ItemStack stack : player.getInventory().items) {
            if (stack != null && stack.is(probe.getItem())) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static List<ItemStack> rows(final ItemStack... stacks) {
        final List<ItemStack> rows = new java.util.ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            rows.add(ItemStack.EMPTY);
        }
        for (int i = 0; i < stacks.length; i++) {
            rows.set(i, stacks[i]);
        }
        return rows;
    }

    /** A real {@link ServerPlayer} with a connection that carries our payloads. */
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
        return player;
    }
}
