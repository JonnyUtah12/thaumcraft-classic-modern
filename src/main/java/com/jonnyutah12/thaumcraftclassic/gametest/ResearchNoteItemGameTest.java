package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ResearchCompletePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import com.jonnyutah12.thaumcraftclassic.research.NotePuzzle;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/**
 * TC-M3-04-s-ii: the research note item against the live server paths
 * (original {@code thaumcraft.common.items.ItemResearchNotes}).
 *
 * <p>Each probe drives the shipped item through {@link ItemStack#use} — the
 * call {@code ServerPlayerGameMode#useItem} makes for a right click with no
 * block behind it, verified against the 1.21.1 sources
 * ({@code ItemStack#use} → {@code Item#use(level, player, hand)}) — on a real
 * {@link ServerPlayer} whose connection exists, so the payloads the research
 * manager sends on completion reach a declared channel. The mock player from
 * {@code GameTestHelper#makeMockPlayer} is not a {@code ServerPlayer}, which is
 * why these probes build one the way {@code PersistenceGameTest} does.</p>
 *
 * <ul>
 *   <li>{@link #registrationAndComponentResolve}: the id and
 *       {@code thaumcraft:research_note} are in the live registries, the note
 *       is unstackable, its registered rarity is rare, the discovery family is
 *       epic per stack, and the component carries both codecs.</li>
 *   <li>{@link #completedNoteFinishesResearchSiblingsAndIsConsumed}: branch 1
 *       — the completed note finishes its research, sweeps the siblings the new
 *       knowledge unlocks, and spends itself.</li>
 *   <li>{@link #unmetRequisitesKeepTheNote}: a completed note whose entry has
 *       unmet requisites answers in chat and is <em>not</em> consumed.</li>
 *   <li>{@link #blankNoteWithNothingToRevealDropsFragments}: branch 2's FAIL
 *       arm — 7-9 knowledge fragments ({@code itemresource} meta 9) in one
 *       drop, no research granted, note spent.</li>
 *   <li>{@link #blankNoteRevealsAFreshPuzzleNote}: branch 2's success arm — the
 *       blank becomes a meta-0 note stamped with the day's hidden entry,
 *       unsolved, with a generated hex grid.</li>
 *   <li>{@link #namesAndRarityFollowTheOriginalMetaThreshold}: the original's
 *       meta-64 name/rarity split, and the blank discovery metas 24/42.</li>
 *   <li>{@link #noteTooltipCarriesTheOriginalLines}: the two blank-note lines,
 *       the research name/subtitle pair, and the warp line clamped at level 5.</li>
 *   <li>{@link #noteComponentSurvivesNbtAndWireCodecs}: the component's
 *       persistent codec writes the original tag layout (byte-width hexgrid
 *       cells) and both codecs round-trip a generated note.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchNoteItemGameTest {

    private static final ResourceLocation NOTE_ID =
            ResourceLocation.parse(ThaumcraftClassic.MOD_ID + ":itemresearchnotes");
    private static final ResourceLocation COMPONENT_ID =
            ResourceLocation.parse(ThaumcraftClassic.MOD_ID + ":research_note");
    private static final String NOTE_KEY = "item." + ThaumcraftClassic.MOD_ID + ".itemresearchnotes";

    private ResearchNoteItemGameTest() {
    }

    @GameTest(template = "empty")
    public static void registrationAndComponentResolve(final GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(NOTE_ID),
                "thaumcraft:itemresearchnotes is missing from the item registry");
        final Item registered = BuiltInRegistries.ITEM.get(NOTE_ID);
        helper.assertTrue(registered == M3ResearchContent.ITEM_RESEARCH_NOTES.get(),
                "the registered item is not the M3ResearchContent holder: " + registered);
        helper.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(COMPONENT_ID),
                "thaumcraft:research_note is missing from the data component registry");

        final DataComponentType<ResearchNoteData> component = ModDataComponents.RESEARCH_NOTE.get();
        helper.assertTrue(component.codec() != null, "the research_note persistent codec is null");
        helper.assertTrue(component.streamCodec() == ResearchNoteData.STREAM_CODEC,
                "the component's network codec is not the note's own stream codec");

        final ItemStack plain = new ItemStack(registered);
        helper.assertTrue(plain.getMaxStackSize() == 1,
                "the original's setMaxStackSize(1) is missing: stacks to " + plain.getMaxStackSize());
        helper.assertTrue(plain.getRarity() == Rarity.RARE,
                "the registered rarity is " + plain.getRarity() + ", the original's getRarity is rare");
        helper.assertTrue(ItemResearchNotes.puzzleNote(note(helper, "JARBRAIN")).getRarity() == Rarity.RARE,
                "a filled note should be rare");
        helper.assertTrue(ItemResearchNotes.discoveryFamilyStack(64).getRarity() == Rarity.EPIC,
                "the discovery family should be epic");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void completedNoteFinishesResearchSiblingsAndIsConsumed(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        player.setData(ModAttachments.PLAYER_KNOWLEDGE,
                PlayerKnowledgeData.empty().withResearchCompleted("HUNGRYCHEST"));

        // GOLEMSTRAW (parent HUNGRYCHEST) with siblings COREGATHER + GOLEMBELL,
        // which are gated on GOLEMSTRAW alone: the sweep must complete them in
        // the same click that finishes the parent.
        final ItemStack stack = ItemResearchNotes.puzzleNote(
                NotePuzzle.generate(ResearchManager.graph().entry("GOLEMSTRAW"), new Random(3L))
                        .withComplete(true));
        give(player, stack);

        final net.minecraft.world.InteractionResultHolder<ItemStack> used =
                stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(used.getResult().consumesAction(),
                "the completed note did not consume the click: " + used.getResult());

        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(ResearchManager.isResearchComplete(knowledge, "GOLEMSTRAW"),
                "the completed note did not finish GOLEMSTRAW");
        helper.assertTrue(ResearchManager.isResearchComplete(knowledge, "GOLEMBELL"),
                "the sibling sweep missed GOLEMBELL");
        helper.assertTrue(ResearchManager.isResearchComplete(knowledge, "COREGATHER"),
                "the sibling sweep missed COREGATHER");
        helper.assertTrue(stack.getCount() == 0,
                "the original consumes the note; it is still " + stack.getCount() + " strong");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unmetRequisitesKeepTheNote(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        player.setData(ModAttachments.PLAYER_KNOWLEDGE,
                PlayerKnowledgeData.empty().withResearchCompleted("CRUCIBLE"));

        // JARBRAIN sits behind INFUSION (itself behind DISTILESSENTIA).
        final ItemStack stack = ItemResearchNotes.puzzleNote(
                NotePuzzle.generate(ResearchManager.graph().entry("JARBRAIN"), new Random(5L))
                        .withComplete(true));
        give(player, stack);

        stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        helper.assertTrue(!ResearchManager.isResearchComplete(knowledge, "JARBRAIN"),
                "JARBRAIN completed with its INFUSION requisite unmet");
        helper.assertTrue(stack.getCount() == 1,
                "the note was spent even though hasRequisites failed: " + stack.getCount());
        helper.assertTrue(ResearchManager.isResearchComplete(knowledge, "CRUCIBLE"),
                "the failed click cleared the player's knowledge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blankNoteWithNothingToRevealDropsFragments(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);

        // Seeding every hidden entry that carries tags leaves findHiddenResearch
        // with no candidate, which is the original's doCheck FAIL arm.
        PlayerKnowledgeData knowledge = PlayerKnowledgeData.empty();
        int hidden = 0;
        for (final ResearchEntryData entry : ResearchManager.graph().entries()) {
            if (entry.flags() != null && entry.flags().hidden()
                    && entry.tags() != null && !entry.tags().isEmpty()) {
                knowledge = knowledge.withResearchCompleted(entry.key());
                hidden++;
            }
        }
        helper.assertTrue(hidden >= 20, "the shipped graph lost its hidden entries: " + hidden);
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
        helper.assertTrue("FAIL".equals(ResearchManager.findHiddenResearch(knowledge, helper.getLevel().getGameTime())),
                "the seeded player should have nothing left to reveal");

        final int before = countFragments(helper.getLevel(), player);
        final ItemStack stack = ItemResearchNotes.blankDiscoveryNote(ItemResearchNotes.META_BLANK_DISCOVERY_B);
        give(player, stack);
        helper.assertTrue(ItemResearchNotes.storedMeta(stack) == ItemResearchNotes.META_BLANK_DISCOVERY_B,
                "the blank discovery note lost its meta stamp");

        stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        final int dropped = countFragments(helper.getLevel(), player) - before;
        helper.assertTrue(dropped >= 7 && dropped <= 9,
                "the failed reveal dropped " + dropped + " fragments, the original drops 7 + rand(3)");
        helper.assertTrue(stack.getCount() == 0, "the spent blank note survived");
        helper.assertTrue(player.getData(ModAttachments.PLAYER_KNOWLEDGE).researchKeys().equals(knowledge.researchKeys()),
                "the failed reveal granted research");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blankNoteRevealsAFreshPuzzleNote(final GameTestHelper helper) {
        final ServerPlayer player = freshPlayer(helper);
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        final String expected = ResearchManager.findHiddenResearch(knowledge, helper.getLevel().getGameTime());
        helper.assertTrue(!"FAIL".equals(expected),
                "a fresh player must have revealable hidden research for the note to stamp");

        final ItemStack stack = ItemResearchNotes.blankDiscoveryNote(ItemResearchNotes.META_BLANK_DISCOVERY_A);
        give(player, stack);
        stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(stack.getCount() == 1, "the revealed note vanished: " + stack.getCount());
        helper.assertTrue(ItemResearchNotes.storedMeta(stack) == 0,
                "the revealed note kept meta " + ItemResearchNotes.storedMeta(stack) + ", the original resets it to 0");
        final ResearchNoteData stamped = stack.get(ModDataComponents.RESEARCH_NOTE.get());
        helper.assertTrue(stamped != null, "the revealed note carries no research_note component");
        helper.assertTrue(expected.equals(stamped.key()),
                "the note was stamped " + stamped.key() + ", the day's reveal is " + expected);
        helper.assertTrue(!stamped.complete(), "the fresh note arrived already solved");
        helper.assertTrue(!stamped.hexEntries().isEmpty(), "the fresh note has no hex grid to solve");
        helper.assertTrue(stamped.hexEntries().values().stream().anyMatch(cell -> !cell.empty()),
                "the fresh note's grid has no tags on it");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void namesAndRarityFollowTheOriginalMetaThreshold(final GameTestHelper helper) {
        final Item note = M3ResearchContent.ITEM_RESEARCH_NOTES.get();

        for (final int meta : new int[] {0, 24, 42, 63}) {
            final ItemStack stack = ItemResearchNotes.stack(meta);
            helper.assertTrue(NOTE_KEY.equals(keyOf(note.getName(stack))),
                    "meta " + meta + " named " + keyOf(note.getName(stack)) + ", expected " + NOTE_KEY);
            helper.assertTrue(stack.getRarity() == Rarity.RARE,
                    "meta " + meta + " is " + stack.getRarity() + ", the original's getRarity is rare below 64");
        }
        for (final int meta : new int[] {64, 96, 127}) {
            final ItemStack stack = ItemResearchNotes.discoveryFamilyStack(meta);
            helper.assertTrue((NOTE_KEY + ".discovery").equals(keyOf(note.getName(stack))),
                    "meta " + meta + " named " + keyOf(note.getName(stack))
                            + ", expected " + NOTE_KEY + ".discovery");
            helper.assertTrue(stack.getRarity() == Rarity.EPIC,
                    "meta " + meta + " is " + stack.getRarity() + ", the original's getRarity is epic at 64+");
            helper.assertTrue(ItemResearchNotes.storedMeta(stack) == meta,
                    "the discovery-family stack lost meta " + meta);
        }
        helper.assertTrue(ItemResearchNotes.isBlankDiscoveryMeta(ItemResearchNotes.META_BLANK_DISCOVERY_A)
                        && ItemResearchNotes.isBlankDiscoveryMeta(ItemResearchNotes.META_BLANK_DISCOVERY_B)
                        && !ItemResearchNotes.isBlankDiscoveryMeta(0)
                        && !ItemResearchNotes.isBlankDiscoveryMeta(64),
                "the blank discovery metas are the original's 24 and 42");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noteTooltipCarriesTheOriginalLines(final GameTestHelper helper) {
        final Item note = M3ResearchContent.ITEM_RESEARCH_NOTES.get();

        final List<String> blank = tooltipKeys(note,
                ItemResearchNotes.blankDiscoveryNote(ItemResearchNotes.META_BLANK_DISCOVERY_A));
        helper.assertTrue(blank.contains(ItemResearchNotes.KEY_BLANK_UNKNOWN_1),
                "the blank note lost its first tooltip line: " + blank);
        helper.assertTrue(blank.contains(ItemResearchNotes.KEY_BLANK_UNKNOWN_2),
                "the blank note lost its second tooltip line: " + blank);

        // OCULUS is warp 6 in the shipped graph: the original clamps at 5.
        final List<String> warped = tooltipKeys(note,
                ItemResearchNotes.puzzleNote(note(helper, "OCULUS")));
        helper.assertTrue(warped.contains("tc.research_name.OCULUS"),
                "the note tooltip did not name its research: " + warped);
        helper.assertTrue(warped.contains("tc.research_text.OCULUS"),
                "the note tooltip lost the research subtitle: " + warped);
        helper.assertTrue(warped.contains(ItemResearchNotes.KEY_FORBIDDEN),
                "the warp 6 note lost its forbidden-knowledge line: " + warped);
        helper.assertTrue(warped.contains(ItemResearchNotes.KEY_FORBIDDEN_LEVEL_PREFIX + 5),
                "warp 6 must clamp to the level 5 label: " + warped);
        helper.assertTrue(!warped.contains(ItemResearchNotes.KEY_FORBIDDEN_LEVEL_PREFIX + 6),
                "the tooltip named a warp level the original never shows: " + warped);

        final List<String> mid = tooltipKeys(note, ItemResearchNotes.puzzleNote(note(helper, "JARBRAIN")));
        helper.assertTrue(mid.contains(ItemResearchNotes.KEY_FORBIDDEN_LEVEL_PREFIX + 3),
                "the warp 3 note should name level 3: " + mid);

        final List<String> free = tooltipKeys(note, ItemResearchNotes.puzzleNote(note(helper, "GOLEMSTRAW")));
        helper.assertTrue(!free.contains(ItemResearchNotes.KEY_FORBIDDEN),
                "a zero-warp research grew a warp line: " + free);
        helper.assertTrue(free.contains("tc.research_name.GOLEMSTRAW"),
                "the zero-warp note lost its research name: " + free);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noteComponentSurvivesNbtAndWireCodecs(final GameTestHelper helper) {
        final DataComponentType<ResearchNoteData> component = ModDataComponents.RESEARCH_NOTE.get();
        final ResearchNoteData generated = NotePuzzle.generate(
                ResearchManager.graph().entry("THAUMIUM"), new Random(29L));
        helper.assertTrue(!generated.hexEntries().isEmpty(), "the generated note has no cells to encode");

        final Tag tag = component.codec().encodeStart(NbtOps.INSTANCE, generated).result().orElse(null);
        helper.assertTrue(tag instanceof CompoundTag, "the note did not encode to a compound tag: " + tag);
        final CompoundTag compound = (CompoundTag) tag;
        helper.assertTrue(generated.key().equals(compound.getString("key")),
                "the encoded note lost its key: " + compound);
        helper.assertTrue(compound.get("hexgrid") instanceof ListTag,
                "the note's hexgrid is not the original's list layout: " + compound);
        final ListTag grid = (ListTag) compound.get("hexgrid");
        helper.assertTrue(grid.size() == generated.hexEntries().size(),
                "the encoded grid has " + grid.size() + " rows, expected " + generated.hexEntries().size());
        final CompoundTag cell = (CompoundTag) grid.get(0);
        helper.assertTrue(cell.get("hexq") instanceof ByteTag && cell.get("hexr") instanceof ByteTag
                        && cell.get("type") instanceof ByteTag,
                "the grid cells lost the original's byte widths: " + cell);

        final ResearchNoteData fromNbt =
                component.codec().decode(NbtOps.INSTANCE, compound).result().orElseThrow().getFirst();
        helper.assertTrue(fromNbt.equals(generated), "the note did not survive its own tag round trip");

        final ByteBuf wire = Unpooled.buffer();
        ResearchNoteData.STREAM_CODEC.encode(wire, generated);
        helper.assertTrue(ResearchNoteData.STREAM_CODEC.decode(wire).equals(generated),
                "the note did not survive the network round trip (the original synced it with the stack)");

        // The component rides a real stack through the item's own save path.
        final ItemStack stack = ItemResearchNotes.puzzleNote(generated);
        final CompoundTag stackTag = (CompoundTag) stack.save(helper.getLevel().registryAccess());
        final ItemStack restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), stackTag);
        helper.assertTrue(!restored.isEmpty()
                        && generated.equals(restored.get(ModDataComponents.RESEARCH_NOTE.get())),
                "the note did not ride the stack's own save/parse path");
        helper.succeed();
    }

    // ---- helpers ------------------------------------------------------------

    private static ResearchNoteData note(final GameTestHelper helper, final String key) {
        return NotePuzzle.generate(ResearchManager.graph().entry(key), new Random(7L));
    }

    /** The translatable key of a tooltip line, resolved through its arguments. */
    private static List<String> tooltipKeys(final Item note, final ItemStack stack) {
        final List<Component> tooltip = new ArrayList<>();
        note.appendHoverText(stack, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.Default.NORMAL);
        final List<String> keys = new ArrayList<>();
        for (final Component line : tooltip) {
            keys.add(keyOf(line));
            if (line.getContents() instanceof TranslatableContents translatable) {
                for (final Object argument : translatable.getArgs()) {
                    if (argument instanceof Component inner) {
                        keys.add(keyOf(inner));
                    }
                }
            }
        }
        return keys;
    }

    private static String keyOf(final Component component) {
        return component.getContents() instanceof TranslatableContents translatable
                ? translatable.getKey()
                : component.getString();
    }

    private static int countFragments(final net.minecraft.server.level.ServerLevel level,
                                      final ServerPlayer player) {
        final AABB area = new AABB(player.blockPosition()).inflate(8.0);
        int total = 0;
        for (final ItemEntity drop : level.getEntities(EntityType.ITEM, area, entity -> true)) {
            final ItemStack stack = drop.getItem();
            if (stack.is(CatalogResources.ITEM_RESOURCE.get())
                    && ItemResource.META_KNOWLEDGE_FRAGMENT == ItemResource.storedMeta(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void give(final ServerPlayer player, final ItemStack stack) {
        player.getInventory().setItem(0, stack);
        player.getInventory().selected = 0;
    }

    /**
     * A bare {@link ServerPlayer} with a declared ad-hoc channel, mirroring
     * {@code ResearchProgressionGameTest} / {@code PersistenceGameTest}: the
     * note's completion path sends the research-complete payload and the
     * research manager syncs attachments and warp, and NeoForge's send guard
     * rejects undeclared payloads on a connection that has them.
     */
    private static ServerPlayer freshPlayer(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(),
                        "tc-note-" + UUID.randomUUID().toString().substring(0, 8)), false);
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
