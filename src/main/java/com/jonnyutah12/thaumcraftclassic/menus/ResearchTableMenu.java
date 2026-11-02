package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.tiles.TileResearchTable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The research table's menu — TC-M7-04-m2b, content-mapping row
 * {@code thaumcraft:gui_research_table} (original gui_id 10,
 * {@code thaumcraft.common.container.ContainerResearchTable}).
 *
 * <p><b>Slot order and anchors are the original's, verbatim</b>
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/container/ContainerResearchTable.java}):
 * menu slot 0 is the tile's scribing-tool slot at {@code (14,10)} — the original's
 * {@code SlotLimitedByClass(IScribeTools.class, tileEntity, 0, 14, 10)} — slot 1 the
 * research-note slot at {@code (70,10)}, then the player grid at
 * {@code (48 + col*18, 175 + row*18)} and the hotbar at {@code (48 + i*18, 233)}.
 * The original's panel is 255 wide by 255 tall, which is why
 * {@link ContainerLayout} carries the research table's own figures; the anchors are
 * read from there so this menu and {@code client/ResearchTableScreen} cannot drift.</p>
 *
 * <p><b>The aspect pool is synced, never guessed.</b> 1.7.10's GUI read the tile
 * and the player's knowledge directly — {@code tile.bonusAspects} and
 * {@code Thaumcraft.proxy.playerKnowledge.getAspectPoolFor(...)}. A 1.21.1 screen
 * gets only its menu, so {@link #broadcastChanges()} — where the original ran its
 * container tick — writes the pool the window draws into data slots: slot 0 the
 * discovered-aspect count, 1-3 the RESEARCHER1 / RESEARCHER2 / RESEARCHDUPE flags
 * the original's gates read, and from slot 4 a triple per discovered aspect:
 * its registry index (the order the original's grid walks, which is the order the
 * knowledge component stores discovery in), the player's pool for it, and this
 * table's bonus for it. The screen reads nothing else.</p>
 *
 * <p><b>Shift-click is the original's {@code transferStackInSlot}</b>, branch for
 * branch: an item in the two machine slots moves to the player inventory
 * ({@code moveItemStackTo(stack, 2, slots.size(), true)}), anything in the player
 * inventory moves to the machine slots ({@code moveItemStackTo(stack, 0, 2, false)}),
 * and the per-slot {@code mayPlace} gates — scribing tool in, research note in —
 * decide what lands.</p>
 *
 * <p><b>Buttons are the original's ids.</b> 1 is the "learn" chit the original's
 * container accepted in passing; 5 is the duplicate request, the original's
 * {@code case 5: tileEntity.duplicate(player)} (ContainerResearchTable.java:43).
 * Aspect combination is not a menu button: the GUI sends
 * {@code PacketAspectCombinationToServer}, and the port's matching
 * {@code AspectCombinationToServerPayload} carries the same eight fields —
 * see {@code client/ResearchTableScreen}.</p>
 */
public class ResearchTableMenu extends AbstractContainerMenu {

    /** Menu slot 0: the tile's scribing-tool slot ({@code IScribeTools}). */
    public static final int SLOT_INK = 0;
    /** Menu slot 1: the tile's research-note slot. */
    public static final int SLOT_NOTES = 1;
    /** The player grid starts at menu slot 2, the hotbar at 29 (original offsets 2 / 29). */
    public static final int PLAYER_FIRST = 2;
    public static final int HOTBAR_FIRST = PLAYER_FIRST + 27;
    public static final int SLOT_COUNT = PLAYER_FIRST + 36;

    /** The original's menu-button ids (its {@code transferStackInSlot} cases). */
    public static final int BUTTON_LEARN = 1;
    public static final int BUTTON_DUPLICATE = 5;

    /** Data slot 0: the discovered-aspect count the original's grid paginates over. */
    public static final int DATA_DISCOVERED_COUNT = 0;
    /** Data slots 1-3: the original's three research gates, read off the knowledge. */
    public static final int DATA_RESEARCHER1 = 1;
    public static final int DATA_RESEARCHER2 = 2;
    public static final int DATA_RESEARCH_DUPE = 3;
    /** From slot 4, three data slots per discovered aspect: index, pool, bonus. */
    public static final int DATA_INDEX_BASE = 4;
    public static final int DATA_POOL_OFFSET = 1;
    public static final int DATA_BONUS_OFFSET = 2;
    /** The rows the window can ever show: every aspect the port registers. */
    public static final int MAX_ROWS = Aspect.values().size();
    public static final int DATA_COUNT = DATA_INDEX_BASE + 3 * MAX_ROWS;

    private final TileResearchTable table;
    private final Inventory playerInventory;
    private final int[] values = new int[DATA_COUNT];

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(final int index) {
            return ResearchTableMenu.this.values[index];
        }

        @Override
        public void set(final int index, final int value) {
            ResearchTableMenu.this.values[index] = value;
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ResearchTableMenu(final int id, final Inventory playerInventory, final TileResearchTable table) {
        super(null, id);
        this.table = table;
        this.playerInventory = playerInventory;

        // Original slot order: scribing tool, research note, player grid, hotbar.
        addSlot(new InkSlot(table, TileResearchTable.SLOT_INK,
                ContainerLayout.RT_INK_X, ContainerLayout.RT_INK_Y));
        addSlot(new NotesSlot(table, TileResearchTable.SLOT_NOTES,
                ContainerLayout.RT_NOTES_X, ContainerLayout.RT_NOTES_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        ContainerLayout.researchTablePlayerX(col), ContainerLayout.researchTablePlayerY(row)));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    ContainerLayout.researchTableHotbarX(col), ContainerLayout.RT_HOTBAR_TOP));
        }
        addDataSlots(data);
    }

    /** The tile this menu reads and drives. */
    public TileResearchTable table() {
        return this.table;
    }

    /**
     * The wire value at {@code index} — the same {@code int} the synced
     * {@link ContainerData} carries. The screen reads the named accessors below;
     * this is public so the acceptance probe can check the sync contract
     * (BD-19) against the row indices it ships.
     */
    public int dataValue(final int index) {
        return index >= 0 && index < DATA_COUNT ? this.values[index] : 0;
    }

    /**
     * The scribing-tool slot: the original's {@code SlotLimitedByClass(IScribeTools
     * .class, tileEntity, 0, 14, 10)}. The port's seam for that class check is
     * {@link TileResearchTable#isScribeTool}, so the inkwell and the bottle of
     * taint both land in this slot when they register, and nothing else does.
     */
    private static final class InkSlot extends Slot {
        InkSlot(final TileResearchTable table, final int index, final int x, final int y) {
            super(table, index, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return TileResearchTable.isScribeTool(stack);
        }
    }

    /**
     * The note slot: the original's {@code SlotResearchNotes}, whose tile-side gate
     * is "the note item, under damage 64" (TileResearchTable.java:449-452 — 64 is
     * the finished-note marker, which the slot refuses). The id gate is the seam
     * BD-19 names: no note item is registered today, so this refuses everything.
     */
    private static final class NotesSlot extends Slot {
        private final TileResearchTable table;

        NotesSlot(final TileResearchTable table, final int index, final int x, final int y) {
            super(table, index, x, y);
            this.table = table;
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return this.table.canPlaceItem(TileResearchTable.SLOT_NOTES, stack);
        }
    }

    // ------------------------------------------------- the synced aspect display

    /**
     * The original's container tick. On the server it refreshes the display data;
     * on the client the data slots arrive through the vanilla container-data sync.
     */
    @Override
    public void broadcastChanges() {
        if (this.table.getLevel() == null || this.table.getLevel().isClientSide) {
            super.broadcastChanges();
            return;
        }
        // The knowledge read is an attachment lookup, which every Player carries,
        // so no ServerPlayer cast is needed here. In production the container's
        // player IS a ServerPlayer (this is the server-side container tick); the
        // game-test harness's mock player is a plain Player, and that is what lets
        // a probe assert this menu's sync contract.
        refreshDisplay(playerInventory.player);
        super.broadcastChanges();
    }

    /**
     * Write what the window draws: the count, the three research gates, and per
     * discovered aspect its registry index, the player's pool and this table's
     * bonus. The order is the knowledge component's discovery order, which is the
     * order the original's {@code getDiscoveredAspects} list walks.
     */
    private void refreshDisplay(final Player player) {
        PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        if (knowledge == null) {
            knowledge = PlayerKnowledgeData.empty();
        }
        values[DATA_DISCOVERED_COUNT] = knowledge.aspectsDiscovered().size();
        values[DATA_RESEARCHER1] = knowledge.knowsResearch(TileResearchTable.RESEARCHER1) ? 1 : 0;
        values[DATA_RESEARCHER2] = knowledge.knowsResearch(TileResearchTable.RESEARCHER2) ? 1 : 0;
        values[DATA_RESEARCH_DUPE] = knowledge.knowsResearch("RESEARCHDUPE") ? 1 : 0;
        int row = 0;
        for (final PlayerKnowledgeData.AspectEntry entry : knowledge.aspectsDiscovered()) {
            if (row >= MAX_ROWS) {
                break;
            }
            final Aspect aspect = Aspect.getAspect(entry.key());
            final int base = DATA_INDEX_BASE + 3 * row;
            values[base] = aspect == null ? -1 : Aspect.values().indexOf(aspect);
            values[base + DATA_POOL_OFFSET] = knowledge.aspectAmount(entry.key());
            values[base + DATA_BONUS_OFFSET] = aspect == null ? 0 : table.getBonus(aspect);
            row++;
        }
        for (; row < MAX_ROWS; row++) {
            final int base = DATA_INDEX_BASE + 3 * row;
            values[base] = -1;
            values[base + DATA_POOL_OFFSET] = 0;
            values[base + DATA_BONUS_OFFSET] = 0;
        }
    }

    /** The count the original's paging arithmetic runs on: {@code discoveredAspects.size()}. */
    public int getDiscoveredCount() {
        return values[DATA_DISCOVERED_COUNT];
    }

    /** The original's {@code isResearchKnown("RESEARCHER1")} gate. */
    public boolean knowsResearcher1() {
        return values[DATA_RESEARCHER1] != 0;
    }

    /** The original's {@code isResearchKnown("RESEARCHER2")} gate. */
    public boolean knowsResearcher2() {
        return values[DATA_RESEARCHER2] != 0;
    }

    /** The original's {@code isResearchKnown("RESEARCHDUPE")} gate, the camera plate. */
    public boolean knowsResearchDupe() {
        return values[DATA_RESEARCH_DUPE] != 0;
    }

    /** The {@code row}-th discovered aspect, as the synced registry index resolves it. */
    public Aspect aspectAt(final int row) {
        if (row < 0 || row >= MAX_ROWS) {
            return null;
        }
        final int index = values[DATA_INDEX_BASE + 3 * row];
        final java.util.List<Aspect> all = Aspect.values();
        return index < 0 || index >= all.size() ? null : all.get(index);
    }

    /** The player's pool for the {@code row}-th discovered aspect. */
    public int poolAt(final int row) {
        return row < 0 || row >= MAX_ROWS ? 0 : values[DATA_INDEX_BASE + 3 * row + DATA_POOL_OFFSET];
    }

    /** This table's bonus pool for the {@code row}-th discovered aspect. */
    public int bonusAt(final int row) {
        return row < 0 || row >= MAX_ROWS ? 0 : values[DATA_INDEX_BASE + 3 * row + DATA_BONUS_OFFSET];
    }

    /**
     * The original's out-of-ink condition, read from the tile the menu owns: the
     * GUI showed its notice from {@code tile.contents[0]} (GuiResearchTable
     * .java:213-228), and the port's screen reads the same slot through the
     * container's own inventory, which the vanilla item sync already carries.
     */
    public boolean isOutOfInk() {
        final ItemStack ink = getSlot(SLOT_INK).getItem();
        return ink.isEmpty() || ink.getDamageValue() >= ink.getMaxDamage();
    }

    // ------------------------------------------------------------- the originals

    /**
     * The original's {@code transferStackInSlot} (ContainerResearchTable.java:35-51)
     * verbatim: machine-slot items go to the player ({@code moveItemStackTo(stack, 2,
     * slots.size(), true)}), player items go to the two machine slots
     * ({@code moveItemStackTo(stack, 0, 2, false)}) and are gated by each slot's
     * {@code mayPlace}. The result of a refused move is an empty stack, as in the
     * original.
     */
    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index < PLAYER_FIRST) {
            if (!moveItemStackTo(stack, PLAYER_FIRST, SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, SLOT_INK, PLAYER_FIRST, false)) {
            return ItemStack.EMPTY;
        }
        slot.setChanged();
        return original;
    }

    /**
     * The original's button ids: case 1 is the chit its container accepted, case 5
     * is the duplicate request ({@code tileEntity.duplicate(player)}). Every other
     * id is refused, as the original's switch has no default branch that acts.
     */
    @Override
    public boolean clickMenuButton(final Player player, final int buttonId) {
        if (buttonId == BUTTON_LEARN) {
            return true;
        }
        if (buttonId == BUTTON_DUPLICATE) {
            table.duplicate(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(final Player player) {
        return table.stillValid(player);
    }
}
