package com.jonnyutah12.thaumcraftclassic.menus;

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
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.network.FriendlyByteBuf;
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
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The research-note container (TC-M3-04-s-iii): the 1.21.1 shape of the
 * original's note GUI — the client half was {@code client/gui/GuiResearchTable}
 * and the server half was {@code common/container/ContainerResearchTable}
 * driving {@code common/tiles/TileResearchTable}, because the original's note
 * grid lived in the table's tile, not in the item. The port has no research
 * table tile (the tile-backed table is its own card), so this menu owns the
 * grid state that rides the note stack: the original's tile logic is ported
 * into the arms below and every grid write arrives through
 * {@code thaumcraft:aspect_place_to_server}, which
 * {@code ServerNetworkHooks.placeAspect} routes here.
 *
 * <p>Slots, verbatim from {@code ContainerResearchTable}: the scribe-tools slot
 * (14, 10) gated by {@code SlotLimitedByClass(IScribeTools.class)}, the note
 * slot (70, 10) gated by the original's {@code isItemValidForSlot} (a research
 * note whose metadata is below 64 — a completed note cannot re-enter), and the
 * standard 27 + 9 player rows. The original's 20x20 {@code SlotResearchResult}
 * output slot at (30, 146) is a research-table slot whose item the TABLE
 * produces (its shift-click returns straight to the player); it is not part of
 * the note GUI's contract, so it is not ported here — see BD-20.</p>
 *
 * <p>Container data — the original's per-slot container data, as 1.21.1 data
 * slots: note present, complete, drawn-hex count, copies, ink remaining and
 * the note's colour (the original's {@code TileResearchTable} container data
 * {@code 0..5}: note metadata, complete, hex count, copies, ink, colour).</p>
 *
 * <p>The server is the authority on everything that matters. A client hex
 * write is a request; the write charges the aspect pool and the ink, and the
 * completion check runs here through {@link NotePuzzle#checkCompletion} (the
 * single completion authority from TC-M3-04-s) — a client can never set
 * {@code complete} itself, and the copy arm re-validates
 * {@code complete} server-side too. The ink/durability source is the
 * scribing-tool item of the TC-M3-02 card: the marker
 * {@link IScribeTools} is ported verbatim, and ink is the original's
 * damage-based counter ({@code damage < maxDamage} means ink, one point per
 * write), so with no scribing-tool item registered the gate is inert and the
 * screen shows the original's out-of-ink tooltip.</p>
 */
public class ResearchNoteMenu extends AbstractContainerMenu {

    /** Original: the scribe-tools slot ({@code TileResearchTable} slot 0). */
    public static final int SLOT_SCRIBE = 0;
    /** Original: the note slot ({@code TileResearchTable} slot 1). */
    public static final int SLOT_NOTE = 1;
    /** The first of the 27 + 9 player slots. */
    public static final int FIRST_PLAYER_SLOT = 2;

    /**
     * Original: {@code GuiResearchTable mouseClicked} sends
     * {@code windowClick(windowId, 5)} for the copy button, and
     * {@code ContainerResearchTable.buttonClick} case 5 calls
     * {@code TileResearchTable.duplicate}.
     */
    public static final int BUTTON_DUPLICATE = 5;

    // Container-data indices (the original's per-slot container data).
    public static final int DATA_NOTE_PRESENT = 0;
    public static final int DATA_COMPLETE = 1;
    public static final int DATA_GRID_COUNT = 2;
    public static final int DATA_COPIES = 3;
    public static final int DATA_INK = 4;
    public static final int DATA_COLOR = 5;
    /** The width of the note-state {@code ContainerData} (the six values above). */
    public static final int DATA_COUNT = 6;

    /** Original: the table's two inventory slots. */
    private final SimpleContainer container = new SimpleContainer(2);
    private final Player player;
    /**
     * The client half reads its mirror through the synced {@code dataSlots}
     * list (the same pattern as {@link TrunkMenu}); the server fills it from
     * the slot state in {@link #broadcastChanges()}.
     */
    private final ContainerData data;
    private final Random random = new Random();

    public ResearchNoteMenu(final int id, final Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainerData(DATA_COUNT));
    }

    /**
     * The server constructor: the menu's {@code ContainerData} is
     * server-authoritative — the vanilla menu data slots send the server's
     * values to every tracking client, so the client mirror is read-only.
     */
    public ResearchNoteMenu(final int id, final Inventory playerInventory, final ContainerData dataSource) {
        super(M3ResearchContent.GUI_RESEARCH_TABLE.get(), id);
        this.player = playerInventory.player;
        this.data = dataSource;

        // Original slot coordinates. The scribe gate is the original's
        // SlotLimitedByClass(IScribeTools.class); the note gate is the
        // original's TileResearchTable.isItemValidForSlot(1, stack) — a
        // research note under metadata 64.
        this.addSlot(new Slot(this.container, SLOT_SCRIBE, 14, 10) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return !stack.isEmpty() && stack.getItem() instanceof IScribeTools;
            }
        });
        this.addSlot(new Slot(this.container, SLOT_NOTE, 70, 10) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                if (stack.isEmpty() || !(stack.getItem() instanceof ItemResearchNotes)) {
                    return false;
                }
                if (ItemResearchNotes.storedMeta(stack) >= ItemResearchNotes.META_DISCOVERY_FAMILY) {
                    return false; // original: metadata < 64 only
                }
                final ResearchNoteData note = noteOf(stack);
                return note == null || !note.complete(); // meta 64 == complete
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 175 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 233));
        }

        addDataSlots(this.data);
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
     * Original {@code ResearchManager.updateData(contents[1], data)} — write the
     * note state back onto the stack. The original's component is a
     * {@code List<Integer>}-style meta list and {@code ItemStack.set} mutates in
     * place; {@code ResearchNoteData} is an immutable record, so the write is a
     * full replace (BD-18's component note).
     */
    private void writeNote(final ResearchNoteData dataToWrite) {
        final ItemStack stack = this.container.getItem(SLOT_NOTE);
        if (stack.isEmpty()) {
            return;
        }
        stack.set(ModDataComponents.RESEARCH_NOTE.get(), dataToWrite);
        this.container.setChanged();
    }

    // ------------------------------------------------------------- ink (BD-20)

    /**
     * Original {@code ResearchManager.consumeInkFromTable}: an
     * {@link IScribeTools} stack in the scribe slot has ink while
     * {@code damage < maxDamage}; with {@code commit} the write spends one
     * damage point. The port's scribing-tool item (and so its ink capacity)
     * belongs to the TC-M3-02 card, so with no such item registered there is
     * never ink — the original's out-of-ink gate then blocks every write and
     * the screen shows {@code tile.researchtable.noink}.
     */
    private boolean consumeInk(final boolean commit) {
        final ItemStack tool = this.container.getItem(SLOT_SCRIBE);
        if (!(tool.getItem() instanceof IScribeTools) || tool.getDamageValue() >= tool.getMaxDamage()) {
            return false;
        }
        if (commit) {
            tool.setDamageValue(tool.getDamageValue() + 1);
            this.container.setChanged();
        }
        return true;
    }

    /** The screen's ink gate: original {@code !hasInk} → {@code tile.researchtable.noink}. */
    public boolean hasInk() {
        return consumeInk(false);
    }

    private int inkRemaining() {
        final ItemStack tool = this.container.getItem(SLOT_SCRIBE);
        if (!(tool.getItem() instanceof IScribeTools)) {
            return 0;
        }
        return Math.max(0, tool.getMaxDamage() - tool.getDamageValue());
    }

    // ----------------------------------------------------------- the grid arm

    /**
     * The hex write / erase, ported from {@code TileResearchTable.placeAspect}
     * verbatim (its gate order, its cost branches, its completion tail).
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
        if (!consumeInk(false)) {
            return; // original: the whole body is inside the ink gate
        }
        final ItemStack noteStack = this.container.getItem(SLOT_NOTE);
        if (noteStack.isEmpty() || !(noteStack.getItem() instanceof ItemResearchNotes)
                || dataState == null
                || ItemResearchNotes.storedMeta(noteStack) >= ItemResearchNotes.META_DISCOVERY_FAMILY) {
            return;
        }
        final PlayerKnowledgeData initial = knowledge();
        final boolean r1 = ResearchManager.isResearchComplete(initial, "RESEARCHER1");
        final boolean r2 = ResearchManager.isResearchComplete(initial, "RESEARCHER2");
        final Hex hex = new Hex(q, r);
        HexEntry entry;
        if (aspectTag != null) {
            entry = new HexEntry(aspectTag, 2);
            if (r2 && this.random.nextFloat() < 0.1F) {
                // Original: a 1-in-10 orb jingle on a successful write.
                sender.level().playSound(sender, sender.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.2F, 0.9F + this.random.nextFloat() * 0.2F);
            } else if (initial.aspectAmount(aspectTag) <= 0) {
                // Original: the write is paid from the TABLE's bonus pool
                // (bonusAspects, rebuilt from the player's pool by
                // recalculateBonus). The port has no research table tile and no
                // bonus pool, so the write goes unpaid and the grid is
                // re-gathered, exactly as the original's rebuild path ends
                // (BD-20).
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
                // Original: the erase refunds one unit of the erased aspect.
                sender.level().playSound(sender, sender.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.2F, 0.9F + this.random.nextFloat() * 0.2F);
                final PlayerKnowledgeData refunded = knowledge()
                        .withAspectPoolAdded(current.aspectTag(), 1);
                setKnowledge(refunded);
                PacketDistributor.sendToPlayer(sender,
                        new AspectPoolPayload(current.aspectTag(), 0, refunded.aspectAmount(current.aspectTag())));
            }
            entry = new HexEntry(null, 0);
        }

        dataState = dataState.withHexEntry(hex, entry);
        writeNote(dataState);
        consumeInk(true); // the original spends the ink point after the write

        // Original: ResearchManager.checkResearchCompletion — the completion
        // authority (NotePuzzle.checkCompletion in this port). The client's
        // request never claims completion; the server decides it.
        final ResearchNoteData completed = NotePuzzle.checkCompletion(dataState,
                tag -> knowledge().hasAspectDiscovered(tag));
        if (completed != null) {
            dataState = completed;
            writeNote(dataState);
            // Original: the meta-64 write plus the table's block event, whose
            // client receiver plays thaumcraft:learn (1.0, 1.0) at the table.
            // The virtual table is the player, so the sound rides the player.
            sender.level().playSound(null, sender, ModSounds.LEARN.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        broadcastChanges();
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
     * The client's copy-button availability (the plate + cost panel),
     * mirroring the server gates in {@link #duplicate}.
     */
    public boolean canDuplicate() {
        final ResearchNoteData dataState = note();
        return dataState != null && dataState.complete() && hasInk()
                && hasItem(net.minecraft.world.item.Items.BOOK)
                && hasItem(net.minecraft.world.item.Items.COAL);
    }

    /**
     * Original {@code InventoryPlayer.hasItem(item, meta)} — 1.21.1's
     * {@code Inventory} has no {@code hasItem}/{@code countItem}, so the scan is
     * explicit (this is the gate the original's {@code requiresItems} runs).
     */
    private boolean hasItem(final net.minecraft.world.item.Item item) {
        final Inventory inventory = this.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Original {@code TileResearchTable.duplicate}: pay ink, one book and one
     * coal, charge the entry's tag cost (amount + copies) per tag against the
     * aspect pool, then append a copy of the note and bump its copy count. The
     * copy count is the note's own state, so it rides the note component.
     */
    public void duplicate() {
        if (this.player.level().isClientSide) {
            return;
        }
        final ItemStack noteStack = this.container.getItem(SLOT_NOTE);
        final ResearchNoteData dataState = noteOf(noteStack);
        if (dataState == null || !dataState.complete() || !consumeInk(false)) {
            return; // original: data != null && the ink gate
        }
        if (!hasItem(net.minecraft.world.item.Items.BOOK)
                || !hasItem(net.minecraft.world.item.Items.COAL)) {
            return; // original: requiresItems(book) && requiresItems(coal)
        }
        final var entry = ResearchManager.graph().entry(dataState.key());
        if (entry == null) {
            return; // original: getResearch(data.key) — an unknown key never copies
        }
        final PlayerKnowledgeData knowledge = knowledge();
        if (entry.tags() != null) {
            for (final var tag : entry.tags()) {
                final int cost = tag.amount() + dataState.copies();
                if (knowledge.aspectAmount(tag.aspect()) < cost) {
                    return; // original: pool < amount + copies -> nothing happens
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
        }
        consumeInk(true);
        // Original: InventoryUtils.consumeInventoryItem(book, 0) + (coal, 0).
        this.player.getInventory().removeItem(new ItemStack(net.minecraft.world.item.Items.BOOK));
        this.player.getInventory().removeItem(new ItemStack(net.minecraft.world.item.Items.COAL));
        // Original: data.copies++ + updateData + addResearchNote(copy) — the copy
        // count is the note's own state, so it rides the note component (BD-18's
        // component note), and the extra copy goes to the player.
        final ResearchNoteData bumped = dataState.withCopies(dataState.copies() + 1);
        noteStack.set(ModDataComponents.RESEARCH_NOTE.get(), bumped);
        this.container.setChanged();
        final ItemStack copy = noteStack.copy();
        copy.set(ModDataComponents.RESEARCH_NOTE.get(), bumped);
        if (!this.player.getInventory().add(copy)) {
            this.player.drop(copy, false); // original addResearchNote drops the overflow
        }
        // Original: the block event's client receiver plays thaumcraft:learn at
        // the table; the virtual table here is the player.
        this.player.level().playSound(null, this.player, ModSounds.LEARN.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        broadcastChanges();
    }

    /** Original {@code ContainerResearchTable.buttonClick}: only case 5 exists. */
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
        super.broadcastChanges();
        if (this.player.level().isClientSide) {
            return;
        }
        final ResearchNoteData dataState = note();
        this.data.set(DATA_NOTE_PRESENT, dataState == null ? 0 : 1);
        this.data.set(DATA_COMPLETE, dataState != null && dataState.complete() ? 1 : 0);
        this.data.set(DATA_GRID_COUNT, dataState == null ? 0 : dataState.hexEntries().size());
        this.data.set(DATA_COPIES, dataState == null ? 0 : dataState.copies());
        this.data.set(DATA_INK, inkRemaining());
        this.data.set(DATA_COLOR, dataState == null ? 0 : dataState.color());
    }

    /**
     * The client mirror of the note state. 1.21.1 keeps the menu's
     * {@code dataSlots} list private, so the mirror is captured on the
     * {@code setData} arm the tracking client walks as the server's
     * {@code ContainerData} values arrive (the same seam {@code TrunkMenu}
     * uses for its synced state).
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
     * Original shift-click: the container slots merge into the player
     * inventory and player slots merge back into the container (the base
     * 1.7.10 {@code transferSlot} behaviour, expressed with the 1.21.1
     * per-slot gates so the scribe/note gates are honoured).
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
            } else if (stack.getItem() instanceof IScribeTools) {
                if (!this.moveItemStackTo(stack, SLOT_SCRIBE, SLOT_SCRIBE + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot.mayPlace(stack) && !this.moveItemStackTo(stack, SLOT_NOTE, SLOT_NOTE + 1, false)) {
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

    /** Closing the GUI returns the scribe tools and the note to the player. */
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

    /** The window title (the original's container name). */
    public static Component title() {
        return Component.translatable("container.thaumcraft.gui_research_table");
    }
}
