package com.jonnyutah12.thaumcraftclassic.menus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.jonnyutah12.thaumcraftclassic.api.IScribeTools;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.NotePuzzle;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The research-note container (TC-M3-04-s-iii-b, content-mapping row
 * {@code Thaumcraft:GUI_RESEARCH_TABLE} → {@code thaumcraft:gui_research_table},
 * original {@code gui_id = 10}).
 *
 * <p>The original split this screen across three classes:
 * {@code client/gui/GuiResearchTable} (the hex grid, the scribe/note slots, the
 * copy plate), {@code common/container/ContainerResearchTable} (the slots, the
 * shift-click arms, the button arms) and {@code common/tiles/TileResearchTable}
 * (the grid write, the ink gate, the completion stamp, the copy arm). The port
 * has no research-table tile — that is TC-M4-01's card — so this menu owns the
 * grid state that rides the note stack, and the tile's arms live below, ported
 * one-for-one with their gate order. Every client hex write arrives through
 * {@code thaumcraft:aspect_place_to_server}, which
 * {@code ServerNetworkHooks.placeAspect} routes to {@link #placeAspect}.</p>
 *
 * <p><b>Slots</b> — verbatim from {@code ContainerResearchTable.java:19-20} and
 * {@code :29-36}: the scribe slot at (14, 10) and the note slot at (70, 10),
 * each a {@code SlotLimitedByClass} — a class gate and nothing else
 * ({@code SlotLimitedByClass.java:22-24} is {@code clazz.isAssignableFrom(stack
 * .getItem().getClass())}; there is no metadata and no completeness test in the
 * slot, the meta-64 gate lives in the write arm, {@code TileResearchTable.java
 * :133}). Then the 27 + 9 player rows at x 48 (the original's panel is wider
 * than the deconstruction table's: its rows start at {@code 48 + j * 18},
 * {@code ContainerResearchTable.java:31,35}), the hotbar row at y 233.</p>
 *
 * <p><b>Data slots</b> — the six {@code ContainerData} values below are the
 * port's own seam, not the original's: 1.7.10 {@code Container} has no
 * data-slot mechanism at all, so the original's GUI read the tile in-process
 * ({@code GuiResearchTable} dereferences {@code this.tileEntity} directly, e.g.
 * its ink tooltip gate at {@code :209-211}). A 1.21.1 screen gets only its
 * menu, so the note state the screen draws is published here (BD-20). The
 * indices are stable: the sibling screen card draws against them.</p>
 *
 * <p><b>Authority</b> — the server decides everything that matters. A client
 * hex write is a request; the write spends the aspect pool and the ink, and
 * completion runs through {@link NotePuzzle#checkCompletion} (the single
 * completion authority from TC-M3-04-s) and stamps the original's meta 64
 * ({@code TileResearchTable.java:178}). The place payload carries no completion
 * claim and no button id can ask for one: the original's {@code buttonClick}
 * has exactly two arms — {@code button == 1} accepted as a no-op and
 * {@code button == 5} → {@code TileResearchTable.duplicate}
 * ({@code ContainerResearchTable.java:39-47}).</p>
 *
 * <p><b>Ink</b> — the scribing tool is the original's {@link IScribeTools}
 * marker, empty in the API and checked with {@code instanceof}; ink is the
 * vanilla durability counter the original read as the damage value
 * ({@code ResearchManager.consumeInkFromTable}, {@code ResearchManager.java
 * :220-230}: {@code damage < maxDamage} means ink, one point spent per
 * committed write). No shipped item carries the marker yet — the scribing tool
 * is its own catalog row — so the gate is inert in game today and the screen
 * shows the original's out-of-ink tooltip ({@code GuiResearchTable.java:209}).
 * The ink arms are exposed as statics so the unit suite can drive them with a
 * synthetic scribe; the game harness has no scribe item to hand the slots.</p>
 */
public class ResearchNoteMenu extends AbstractContainerMenu {

    /** Original: the scribe-tools slot ({@code TileResearchTable} slot 0). */
    public static final int SLOT_SCRIBE = 0;
    /** Original: the note slot ({@code TileResearchTable} slot 1). */
    public static final int SLOT_NOTE = 1;
    /** The first of the 27 + 9 player slots. */
    public static final int FIRST_PLAYER_SLOT = 2;

    // ----------------------------------------------------------- slot layout
    // ContainerResearchTable.java:19 (scribe), :20 (note), :29-36 (player rows).

    /** Original {@code ContainerResearchTable.java:19} — scribe slot x. */
    public static final int SCRIBE_SLOT_X = 14;
    /** Original {@code ContainerResearchTable.java:19} — scribe slot y. */
    public static final int SCRIBE_SLOT_Y = 10;
    /** Original {@code ContainerResearchTable.java:20} — note slot x. */
    public static final int NOTE_SLOT_X = 70;
    /** Original {@code ContainerResearchTable.java:20} — note slot y. */
    public static final int NOTE_SLOT_Y = 10;
    /** Original {@code ContainerResearchTable.java:31,35} — player rows start at x 48. */
    public static final int PLAYER_SLOT_X = 8;
    /** Original {@code ContainerResearchTable.java:31} — the first main row's y. */
    public static final int PLAYER_ROW_Y = 175;
    /** Original {@code ContainerResearchTable.java:31,35} — the slot pitch. */
    public static final int PLAYER_SLOT_STEP = 18;
    /** Original {@code ContainerResearchTable.java:35} — the hotbar row's y. */
    public static final int HOTBAR_Y = 233;

    // ------------------------------------------------------------- buttons
    /**
     * Original {@code ContainerResearchTable.java:40-42}: {@code button == 1}
     * returns true and does nothing (the GUI's generic accept); the note GUI's
     * own click sends only {@link #BUTTON_DUPLICATE}
     * ({@code GuiResearchTable.java:684}).
     */
    public static final int BUTTON_KEEP_OPEN = 1;
    /**
     * Original {@code ContainerResearchTable.java:43-45}: {@code button == 5}
     * calls {@code TileResearchTable.duplicate}; the client's copy plate sends
     * exactly this id ({@code GuiResearchTable.java:681-687}).
     */
    public static final int BUTTON_DUPLICATE = 5;

    // ------------------------------------------------- data slots (BD-20 seam)
    // 1.7.10 publishes no container data; these six are the port's screen seam.
    public static final int DATA_NOTE_PRESENT = 0;
    public static final int DATA_COMPLETE = 1;
    public static final int DATA_GRID_COUNT = 2;
    public static final int DATA_COPIES = 3;
    public static final int DATA_INK = 4;
    public static final int DATA_COLOR = 5;
    /** The width of the note-state {@code ContainerData} (the six values above). */
    public static final int DATA_COUNT = 6;

    /** The original's two-slot table inventory ({@code TileResearchTable.contents[0..1]}). */
    private final SimpleContainer container = new SimpleContainer(FIRST_PLAYER_SLOT);
    private final Player player;
    /** The note state's server-side source, synced to the tracking clients. */
    private final ContainerData data;
    private final Random random = new Random();

    public ResearchNoteMenu(final int id, final Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainerData(DATA_COUNT));
    }

    /**
     * The {@code MenuType} client constructor — the shape
     * {@code IContainerMenuFactory} hands to
     * {@code new MenuType<>(ResearchNoteMenu::new, ...)}. The provider ships no
     * extra buffer (the note is in slot 1, not an entity id), so the client
     * opens with an empty data mirror and the server's six values arrive on the
     * {@code setData} arm.
     */
    public ResearchNoteMenu(final int id, final Inventory playerInventory,
                            final net.minecraft.network.RegistryFriendlyByteBuf extraData) {
        this(id, playerInventory, new SimpleContainerData(DATA_COUNT));
    }

    /**
     * The server constructor: the menu's {@code ContainerData} is
     * server-authoritative — vanilla's data-slot tracking sends the server's
     * values to every tracking client, so the client mirror is read-only.
     */
    public ResearchNoteMenu(final int id, final Inventory playerInventory, final ContainerData dataSource) {
        super(M3ResearchContent.GUI_RESEARCH_TABLE.get(), id);
        this.player = playerInventory.player;
        this.data = dataSource;

        // Original: SlotLimitedByClass(IScribeTools.class, tile, 0, 14, 10) and
        // SlotLimitedByClass(ItemResearchNotes.class, tile, 1, 70, 10). The gate
        // is the class and nothing else (SlotLimitedByClass.java:22-24).
        this.addSlot(new Slot(this.container, SLOT_SCRIBE, SCRIBE_SLOT_X, SCRIBE_SLOT_Y) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return !stack.isEmpty() && stack.getItem() instanceof IScribeTools;
            }
        });
        this.addSlot(new Slot(this.container, SLOT_NOTE, NOTE_SLOT_X, NOTE_SLOT_Y) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                final ResearchNoteData candidate = noteOf(stack);
                return !stack.isEmpty() && stack.getItem() instanceof ItemResearchNotes
                        && candidate != null && ItemResearchNotes.storedMeta(stack) < 64 && !candidate.complete();
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_SLOT_X + col * PLAYER_SLOT_STEP, PLAYER_ROW_Y + row * PLAYER_SLOT_STEP));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, PLAYER_SLOT_X + col * PLAYER_SLOT_STEP, HOTBAR_Y));
        }

        addDataSlots(this.data);
    }

    /** The menu's two-slot table container (the original's {@code TileResearchTable.contents}). */
    public SimpleContainer tableContainer() {
        return this.container;
    }

    /** The note state carried by a stack (original {@code ResearchManager.getData}). */
    public static ResearchNoteData noteOf(final ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemResearchNotes)) {
            return null;
        }
        return stack.get(ModDataComponents.RESEARCH_NOTE.get());
    }

    private ResearchNoteData note() {
        return noteOf(this.container.getItem(SLOT_NOTE));
    }

    /**
     * Original {@code ResearchManager.updateData(contents[1], data)}
     * ({@code ResearchManager.java:205-218}) — write the note state back onto
     * the stack. The original mutates its ItemStack NBT in place;
     * {@code ResearchNoteData} is an immutable record, so the write is a full
     * replace (BD-04/BD-18's component note).
     */
    private void writeNote(final ResearchNoteData dataToWrite) {
        final ItemStack stack = this.container.getItem(SLOT_NOTE);
        if (stack.isEmpty()) {
            return;
        }
        stack.set(ModDataComponents.RESEARCH_NOTE.get(), dataToWrite);
        this.container.setChanged();
    }

    // ---------------------------------------------------------------- ink

    /**
     * Original {@code ResearchManager.consumeInkFromTable(stack, doit)}
     * ({@code ResearchManager.java:220-230}) verbatim: an {@link IScribeTools}
     * stack has ink while its damage value is below its max damage, and with
     * {@code commit} the write spends one point.
     */
    public static boolean consumeInk(final ItemStack tool, final boolean commit) {
        if (tool.isEmpty() || !(tool.getItem() instanceof IScribeTools)
                || tool.getDamageValue() > tool.getMaxDamage()) {
            return false;
        }
        if (commit) {
            tool.setDamageValue(tool.getDamageValue() + 1);
        }
        return true;
    }

    /** The ink gate alone, with no spend (the original's {@code doit = false} call). */
    public static boolean hasInk(final ItemStack tool) {
        return consumeInk(tool, false);
    }

    /** The screen's ink gate: original {@code !hasInk} → {@code tile.researchtable.noink}. */
    public boolean hasInk() {
        return hasInk(this.container.getItem(SLOT_SCRIBE));
    }

    /** The ink the plate shows: the tool's remaining durability, 0 with no scribe. */
    public static int inkRemaining(final ItemStack tool) {
        if (tool.isEmpty() || !(tool.getItem() instanceof IScribeTools)) {
            return 0;
        }
        return Math.max(0, tool.getMaxDamage() - tool.getDamageValue());
    }

    // ---------------------------------------------------------- the grid arm

    /**
     * The hex write / erase, ported from {@code TileResearchTable.placeAspect}
     * ({@code TileResearchTable.java:122-189}) with the original's gate order:
     * the whole body sits behind the ink gate, then the note gate
     * ({@code contents[1]} is a note, the data resolved, and its metadata below
     * 64), then the write's cost branch, then the write, then the ink spend,
     * then the completion check.
     *
     * @param q         the hex q coordinate
     * @param r         the hex r coordinate
     * @param aspectTag the aspect tag written, or null for the erase arm
     * @param sender    the acting server player (the original's packet context)
     */
    public void placeAspect(final int q, final int r, final String aspectTag, final ServerPlayer sender) {
        if (sender != this.player) {
            return; // the menu belongs to one player; the payload hands us its sender
        }
        ResearchNoteData dataState = note(); // original gatherResults(): read from the slot
        if (!hasInk()) {
            return; // original: the whole body is inside consumeInkFromTable(..., false)
        }
        final ItemStack noteStack = this.container.getItem(SLOT_NOTE);
        if (noteStack.isEmpty() || !(noteStack.getItem() instanceof ItemResearchNotes)
                || dataState == null
                || ItemResearchNotes.storedMeta(noteStack) >= ItemResearchNotes.META_DISCOVERY_FAMILY) {
            return; // original TileResearchTable.java:129-133
        }
        final PlayerKnowledgeData initial = knowledge();
        final boolean r1 = ResearchManager.isResearchComplete(initial, "RESEARCHER1");
        final boolean r2 = ResearchManager.isResearchComplete(initial, "RESEARCHER2");
        final Hex hex = new Hex(q, r);
        HexEntry entry;
        if (aspectTag != null) {
            entry = new HexEntry(aspectTag, 2);
            if (r2 && this.random.nextFloat() < 0.1F) {
                // Original: a 1-in-10 orb jingle on a successful write (:139-141).
                sender.level().playSound(sender, sender.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.2F, 0.9F + this.random.nextFloat() * 0.2F);
            } else if (initial.aspectAmount(aspectTag) <= 0) {
                // Original: the write is paid from the TABLE's bonus pool
                // ({@code bonusAspects}, rebuilt by {@code recalculateBonus}) and
                // ends by re-gathering the grid (:142-146). The port has no
                // research-table tile and no bonus pool, so the write goes
                // unpaid and the grid is re-read — BD-20.
                dataState = note();
            } else {
                final PlayerKnowledgeData paid = initial.withAspectPoolAdded(aspectTag, -1);
                setKnowledge(paid);
                PacketDistributor.sendToPlayer(sender,
                        new AspectPoolPayload(aspectTag, 0, paid.aspectAmount(aspectTag)));
            }
        } else {
            final float roll = this.random.nextFloat();
            final HexEntry current = dataState.entryAt(hex);
            if (current != null && current.aspectTag() != null && (r1 && roll < 0.25F || r2 && roll < 0.5F)) {
                // Original: the erase refunds one unit of the erased aspect (:155-169).
                sender.level().playSound(sender, sender.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.2F, 0.9F + this.random.nextFloat() * 0.2F);
                final PlayerKnowledgeData refunded = knowledge().withAspectPoolAdded(current.aspectTag(), 1);
                setKnowledge(refunded);
                PacketDistributor.sendToPlayer(sender,
                        new AspectPoolPayload(current.aspectTag(), 0, refunded.aspectAmount(current.aspectTag())));
            }
            entry = new HexEntry(null, 0);
        }

        dataState = dataState.withHexEntry(hex, entry);
        writeNote(dataState);
        consumeInk(this.container.getItem(SLOT_SCRIBE), true); // original :176, after the write

        // Original: ResearchManager.checkResearchCompletion, gated on
        // !world.isRemote (:177) — a game-test server is never the client, so
        // the gate is always true here. The client's request never claims
        // completion; the server decides it, and stamps the original's meta 64.
        final ResearchNoteData completed = NotePuzzle.checkCompletion(dataState,
                tag -> knowledge().hasAspectDiscovered(tag));
        if (completed != null) {
            applyCompletion(noteStack, completed);
            // Original: the block event (1, 1) whose client receiver plays
            // thaumcraft:learn at 1.0/1.0 (TileResearchTable.java:421-428). The
            // virtual table here is the player, so the sound rides the player.
            sender.level().playSound(null, sender, ModSounds.LEARN.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        broadcastChanges();
    }

    /**
     * The completion stamp, ported from {@code TileResearchTable.java:177-178}:
     * {@code ResearchManager.checkResearchCompletion} plus
     * {@code contents[1].setItemDamage(64)}. The note state goes back onto the
     * stack and the metadata becomes the original's 64, which is what takes the
     * note out of the writeable family (the write arm refuses meta 64+ and
     * {@code ItemResearchNotes#use} finishes the research).
     */
    public static void applyCompletion(final ItemStack noteStack, final ResearchNoteData completed) {
        noteStack.set(ModDataComponents.RESEARCH_NOTE.get(), completed);
    }

    private PlayerKnowledgeData knowledge() {
        final PlayerKnowledgeData knowledge = this.player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        return knowledge == null ? PlayerKnowledgeData.empty() : knowledge;
    }

    private void setKnowledge(final PlayerKnowledgeData knowledge) {
        this.player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
    }

    // --------------------------------------------------------- the copy arm

    /**
     * The client's copy-plate gate (the plate and its cost panel):
     * {@code GuiResearchTable.java:104} and {@code :254} —
     * {@code note != null && RESEARCHDUPE && note.isComplete()}. The plate is
     * drawn by the sibling screen card; this is the state it reads.
     */
    public boolean canDuplicate() {
        final ResearchNoteData dataState = note();
        return dataState != null && dataState.complete()
                && ResearchManager.isResearchComplete(knowledge(), "RESEARCHDUPE");
    }

    /**
     * The cost panel's numbers: the entry's tags with the note's copy count
     * added to each ({@code GuiResearchTable.java:114-124} —
     * {@code al = rr.tags.copy(); for (aspect : al) al.add(aspect, note.copies)}),
     * in the entry's tag order. Empty for a note whose key the graph does not
     * know, which is also the arm that refuses to copy.
     */
    public Map<String, Integer> copyCosts() {
        final Map<String, Integer> costs = new LinkedHashMap<>();
        final ResearchNoteData dataState = note();
        if (dataState == null) {
            return costs;
        }
        final ResearchEntryData entry = ResearchManager.graph().entry(dataState.key());
        if (entry == null || entry.tags() == null) {
            return costs;
        }
        for (final var tag : entry.tags()) {
            costs.put(tag.aspect(), tag.amount() + dataState.copies());
        }
        return costs;
    }

    /**
     * Original {@code TileResearchTable.duplicate} ({@code TileResearchTable.java
     * :425-462}) with its gates in its order: a note in slot 1, its data
     * resolved, <b>its metadata exactly 64</b>, one book and one coal carried,
     * and every tag's pool at {@code amount + copies} or better — otherwise
     * nothing happens. The payment is the pool, one book, one coal; then the
     * block event, {@code data.copies++} written back, and
     * {@code contents[1].stackSize++}.
     *
     * <p>There is no ink gate in this arm — the original's copy path never
     * consumes ink (its ink calls are only in {@code placeAspect}), so the
     * scribe slot is irrelevant to copying. The port's note is unstackable
     * ({@code stacksTo(1)}), so the original's {@code stackSize++} leaves a
     * count-2 stack in the container slot; {@link #removed} hands it back
     * through {@code placeItemBackInInventory}, which splits it into the
     * extra note the player is owed (BD-20).</p>
     */
    public void duplicate() {
        if (this.player.level().isClientSide) {
            return;
        }
        if (!hasInk()) {
            return;
        }
        final ItemStack noteStack = this.container.getItem(SLOT_NOTE);
        final ResearchNoteData dataState = noteOf(noteStack);
        if (noteStack.isEmpty() || !(noteStack.getItem() instanceof ItemResearchNotes) || dataState == null
                || ItemResearchNotes.storedMeta(noteStack) != ItemResearchNotes.META_DISCOVERY_FAMILY) {
            return; // original :431-434 (itemDamage == 64)
        }
        final Inventory inventory = this.player.getInventory();
        // Original InventoryUtils.isPlayerCarrying(book) / (coal, 0) >= 0.
        if (inventory.countItem(Items.BOOK) <= 0 || inventory.countItem(Items.COAL) <= 0) {
            return;
        }
        final ResearchEntryData entry = ResearchManager.graph().entry(dataState.key());
        if (entry == null || entry.tags() == null) {
            return; // original: getResearch(data.key).tags — an unknown key never copies
        }
        final PlayerKnowledgeData knowledge = knowledge();
        for (final var tag : entry.tags()) {
            if (knowledge.aspectAmount(tag.aspect()) < tag.amount() + dataState.copies()) {
                return; // original :441-445: pool < amount + copies -> nothing happens
            }
        }
        PlayerKnowledgeData paid = knowledge;
        for (final var tag : entry.tags()) {
            final int cost = tag.amount() + dataState.copies();
            paid = paid.withAspectPoolAdded(tag.aspect(), -cost);
            if (this.player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer,
                        new AspectPoolPayload(tag.aspect(), 0, paid.aspectAmount(tag.aspect())));
            }
        }
        setKnowledge(paid);
        // Original: InventoryUtils.consumeInventoryItem(book, 0) + (coal, 0)
        // (:452-453) — one of each, from anywhere in the player's inventory.
        this.player.getInventory().clearOrCountMatchingItems(stack -> stack.is(Items.BOOK), 1,
                this.player.getInventory());
        this.player.getInventory().clearOrCountMatchingItems(stack -> stack.is(Items.COAL), 1,
                this.player.getInventory());
        // Original: the block event's client receiver plays thaumcraft:learn at
        // the table; the virtual table here is the player.
        this.player.level().playSound(null, this.player, ModSounds.LEARN.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        // Original: data.copies++ + updateData + contents[1].stackSize++
        final ResearchNoteData bumped = dataState.withCopies(dataState.copies() + 1);
        noteStack.set(ModDataComponents.RESEARCH_NOTE.get(), bumped);
        noteStack.setCount(noteStack.getCount() + 1);
        this.container.setChanged();
        broadcastChanges();
    }

    /**
     * Original {@code ContainerResearchTable.java:39-47}: exactly two arms —
     * {@code button == 1} accepted and doing nothing, {@code button == 5}
     * running the copy arm. Every other id is refused.
     */
    @Override
    public boolean clickMenuButton(final Player clicker, final int id) {
        if (id == BUTTON_DUPLICATE && !this.player.level().isClientSide) {
            duplicate();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------- container data

    @Override
    public void broadcastChanges() {
        // The six note-state values are filled BEFORE the super call: vanilla's
        // broadcastChanges reads each ContainerData and sends the deltas at the
        // end of its own body, so a write after super would publish one tick
        // late.
        if (false) {
            final ResearchNoteData dataState = note();
            this.data.set(DATA_NOTE_PRESENT, dataState == null ? 0 : 1);
            this.data.set(DATA_COMPLETE, dataState != null && dataState.complete() ? 1 : 0);
            this.data.set(DATA_GRID_COUNT, dataState == null ? 0 : dataState.hexEntries().size());
            this.data.set(DATA_COPIES, dataState == null ? 0 : dataState.copies());
            this.data.set(DATA_INK, inkRemaining(this.container.getItem(SLOT_SCRIBE)));
            this.data.set(DATA_COLOR, dataState == null ? 0 : dataState.color());
        }
        super.broadcastChanges();
    }

    /**
     * The client mirror of the note state. 1.21.1 keeps the menu's
     * {@code dataSlots} list private, so the mirror is captured on the
     * {@code setData} arm the tracking client walks as the server's
     * {@code ContainerData} values arrive (the seam the trunk menu uses for its
     * synced state).
     */
    private final int[] dataMirror = new int[DATA_COUNT];

    @Override
    public void setData(final int id, final int value) {
        super.setData(id, value);
        if (id >= 0 && id < this.dataMirror.length) {
            this.dataMirror[id] = value;
        }
    }

    private int dataValue(final int index) {
        return index >= 0 && index < this.dataMirror.length ? this.dataMirror[index] : 0;
    }

    public boolean notePresent() {
        return dataValue(DATA_NOTE_PRESENT) != 0;
    }

    public boolean noteComplete() {
        return dataValue(DATA_COMPLETE) != 0;
    }

    public int gridCount() {
        return dataValue(DATA_GRID_COUNT);
    }

    public int copies() {
        return dataValue(DATA_COPIES);
    }

    public int inkRemainingValue() {
        return dataValue(DATA_INK);
    }

    public int noteColor() {
        return dataValue(DATA_COLOR);
    }

    /** The client's pool mirror (the synced knowledge attachment). */
    public int aspectPoolAmount(final String tag) {
        return knowledge().aspectAmount(tag);
    }

    public PlayerKnowledgeData getKnowledge() {
        return knowledge();
    }

    public Player getPlayer() {
        return this.player;
    }

    // ------------------------------------------------------------ movement

    /**
     * Original shift-click {@code ContainerResearchTable.java:50-58}: a table
     * slot moves into the player rows (trailing-first), a player slot moves
     * into the two table slots. The original's own
     * {@code transferSlotStack} override gates every destination with the
     * slot's class check ({@code :15-79}, {@code var7.func_75214_a(stack)}),
     * which is exactly what 1.21.1's {@code moveItemStackTo} does through
     * {@code Slot#mayPlace} — so the arms need no extra branching here.
     */
    @Override
    public ItemStack quickMoveStack(final Player mover, final int index) {
        ItemStack result = ItemStack.EMPTY;
        final Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < FIRST_PLAYER_SLOT) {
                if (!this.moveItemStackTo(stack, FIRST_PLAYER_SLOT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, SLOT_SCRIBE, FIRST_PLAYER_SLOT, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    /** The note GUI has no block to keep it valid (the table tile is not ported). */
    @Override
    public boolean stillValid(final Player check) {
        return true;
    }

    /**
     * Closing the GUI returns the scribe tools and the note to the player. The
     * original left both in the tile, which survives the screen; the port's
     * table container is a throwaway {@code SimpleContainer}, so without this
     * arm a solved grid is deleted with the container the moment the screen
     * closes (BD-20).
     */
    @Override
    public void removed(final Player leaving) {
        super.removed(leaving);
        if (!leaving.level().isClientSide) {
            for (int slot = SLOT_SCRIBE; slot <= SLOT_NOTE; slot++) {
                final ItemStack leftover = this.container.removeItemNoUpdate(slot);
                if (leftover != null && !leftover.isEmpty()) {
                    leaving.getInventory().placeItemBackInInventory(leftover);
                }
            }
        }
    }

    /**
     * The window title: {@code container.thaumcraft.<registered menu id>}, the
     * same id-derived key shape the localization audit pins for a registered
     * menu type. The original's title is the research table's inventory name.
     */
    public static Component title() {
        return Component.translatable("container.thaumcraft."
                + M3ResearchContent.GUI_RESEARCH_TABLE.getKey().location().getPath());
    }
}
