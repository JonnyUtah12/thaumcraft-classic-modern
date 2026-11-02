package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectCombinations;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.items.IScribeTools;
import com.jonnyutah12.thaumcraftclassic.menus.ResearchTableMenu;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The research table's block entity — TC-M7-04-m2b, content-mapping row
 * {@code thaumcraft:tile.researchtable} (original gui_id 10, hosted on
 * {@code blockTable} variants 2-5, the original's {@code ResearchTable} device).
 *
 * <p>The original is {@code thaumcraft.common.tiles.TileResearchTable} (464
 * lines): an {@code IInventory} of <b>two</b> slots — slot 0 the scribing tool,
 * slot 1 the research note — plus a {@code bonusAspects} {@code AspectList} the
 * surrounding world feeds it, a 600-tick {@code nextRecalc} counter, and the
 * actions the GUI drives: {@code placeAspect} (the hex-minigame drop,
 * TileResearchTable.java:122-186), {@code duplicate} (its :425-463) and
 * {@code combineAspects}.</p>
 *
 * <p><b>What landed here.</b> The two slots with the original's per-slot gates
 * ({@code isItemValidForSlot}, its :439-453), the {@code bonusAspects} pool saved
 * under the original's key and shape, the ink path — the {@code IScribeTools}
 * seam, {@code consumeInkFromTable}'s damage test, and the out-of-ink condition
 * the GUI shows — and the whole of the original's aspect combination: the
 * RESEARCHER1 gate, the RESEARCHER2 gate on an already-known result, payment of
 * one of each aspect from the player's pool <b>or</b> from this table's bonus
 * pool, and the discovery bonus on a first find.</p>
 *
 * <p><b>What did not, and why.</b> The note lane. {@code placeAspect} writes into
 * {@code ResearchNoteData.hexEntries}/{@code hexes} and marks the note complete
 * at damage 64; {@code duplicate} requires a note at damage 64 with a
 * {@code ResearchNoteData} (its research key and {@code copies}) plus a compass
 * and a book in the player's inventory. That data lane and the note item itself
 * are the note/minigame cards: {@code thaumcraft:itemresearchnotes} is a
 * {@code partial} row of docs/content-mapping.csv and its {@code TcComponents.NOTE}
 * component lives on the unmerged {@code wt/note-item} branch. So the notes slot
 * gates on the note id (nothing registered matches it today), {@code duplicate}
 * keeps the menu-button seam and refuses, and {@code recalculateBonus} — the
 * original's per-table ambient aspect roll — is not run because its sources are
 * the M4 device tiles. BD-19 states all three; the note card replaces the
 * bodies, not the seams.</p>
 */
public class TileResearchTable extends BlockEntity implements WorldlyContainer, MenuProvider {

    /** The original's slot 0: the scribing tool ({@code IScribeTools}). */
    public static final int SLOT_INK = 0;
    /** The original's slot 1: the research note ({@code ItemResearchNotes}). */
    public static final int SLOT_NOTES = 1;
    /** The original's {@code getSizeInventory() = 2} (TileResearchTable.java:306-308). */
    public static final int CONTAINER_SIZE = 2;
    /** The original's {@code getInventoryStackLimit() = 64}. */
    public static final int STACK_LIMIT = 64;

    /**
     * The note item's id. The notes slot gates on this id because
     * {@code ItemResearchNotes} is not registered in the port (BD-19); the
     * original's gate was {@code itemstack.getItem() == ConfigItems.itemResearchNotes
     * && itemstack.getItemDamage() < 64} — the damage test is kept, the item test
     * is the seam that becomes it.
     */
    public static final ResourceLocation RESEARCH_NOTES_ID =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "itemresearchnotes");

    /**
     * The same id as a registry key, which is what {@link ItemStack#is} compares
     * against in 1.21.1. Resolved at call time, so the id lives in this one place.
     */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.Item> RESEARCH_NOTES =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,
                    RESEARCH_NOTES_ID);

    /** The original's {@code RESEARCHER1} / {@code RESEARCHER2} gates. */
    public static final String RESEARCHER1 = "RESEARCHER1";
    public static final String RESEARCHER2 = "RESEARCHER2";

    private final ItemStack[] contents = new ItemStack[CONTAINER_SIZE];

    /** The original's {@code bonusAspects}: aspects this table offers for free. */
    private final AspectList bonusAspects = new AspectList();

    /** The original's {@code nextRecalc}, persisted under its own key. */
    private int nextRecalc = 0;

    public TileResearchTable(final BlockPos pos, final BlockState state) {
        super(CatalogFurniture.TILE_RESEARCH_TABLE.get(), pos, state);
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            contents[i] = ItemStack.EMPTY;
        }
    }

    // -------------------------------------------------------------- the two slots

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public int getMaxStackSize() {
        return STACK_LIMIT;
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot < 0 || slot >= contents.length ? ItemStack.EMPTY : contents[slot];
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        final ItemStack stored = getItem(slot);
        if (stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack taken = stored.split(count);
        if (stored.isEmpty()) {
            setItem(slot, ItemStack.EMPTY);
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        final ItemStack stored = getItem(slot);
        if (stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        setItem(slot, ItemStack.EMPTY);
        return stored;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (slot < 0 || slot >= contents.length) {
            return;
        }
        contents[slot] = stack;
        // The original's clamp (TileResearchTable.java:345-352).
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < contents.length; i++) {
            contents[i] = ItemStack.EMPTY;
        }
        setChanged();
    }

    /**
     * The original's per-slot gate, {@code isItemValidForSlot} (its :439-453):
     * slot 0 takes an {@code IScribeTools}, slot 1 a research note under
     * damage 64 (64 is the original's "this note is finished" marker — a finished
     * note goes in the slot for {@code duplicate}, never onto the grid). The
     * container's slots use the same two tests, so a hopper and a drag cannot
     * disagree.
     */
    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (slot) {
            case SLOT_INK -> isScribeTool(stack);
            case SLOT_NOTES -> isResearchNote(stack) && stack.getDamageValue() < 64;
            default -> false;
        };
    }

    /** The {@code IScribeTools} seam — the original's {@code instanceof IScribeTools}. */
    public static boolean isScribeTool(final ItemStack stack) {
        return IScribeTools.is(stack);
    }

    /**
     * The note seam: the original's {@code == ConfigItems.itemResearchNotes}. No
     * such item is registered in the port (BD-19), so this is false for every
     * stack today and the notes slot refuses everything.
     */
    private static boolean isResearchNote(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // Resolved at call time from the id above: 1.21.1 has no == against a
        // registry entry, so the lookup is the seam. The id is not registered
        // today, the lookup is empty, and the gate refuses every stack.
        final Item notes = BuiltInRegistries.ITEM.get(RESEARCH_NOTES);
        return notes != null && stack.is(notes);
    }

    /**
     * Hopper faces: the original's tile is a plain {@code IInventory}, not
     * {@code ISidedInventory}, so in 1.7.10 a hopper reached both slots subject to
     * {@code isItemValidForSlot} and could pull them out. The port exposes both
     * slots on every face with the same validity gate — the closest current-API
     * reading of that, and deliberately not the deconstruction table's invented
     * face lists.
     */
    @Override
    public int[] getSlotsForFace(final Direction side) {
        return new int[]{SLOT_INK, SLOT_NOTES};
    }

    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
        return true;
    }

    // ----------------------------------------------------------------------- ink

    /**
     * The original's ink rule, {@code ResearchManager.consumeInkFromTable}
     * (ResearchManager.java:220-230) transcribed: a scribing tool with damage
     * left, spending one damage when {@code doit}. The research table calls it
     * first as a test and then with {@code doit = true} — the note lane's
     * two-damages-per-copy payment (TileResearchTable.java:95-100, 176).
     */
    public static boolean consumeInkFromTable(final ItemStack stack, final boolean doit) {
        if (!isScribeTool(stack)) {
            return false;
        }
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return false;
        }
        if (doit) {
            stack.setDamageValue(stack.getDamageValue() + 1);
        }
        return true;
    }

    /** The test call: {@code consumeInkFromTable(contents[0], false)}. */
    public boolean canConsumeInk() {
        return consumeInkFromTable(getItem(SLOT_INK), false);
    }

    /** The spend call: the same rule with {@code doit = true}. */
    public boolean consumeInk() {
        if (!consumeInkFromTable(getItem(SLOT_INK), true)) {
            return false;
        }
        setChanged();
        return true;
    }

    /**
     * The condition the original's GUI tested for its out-of-ink notice
     * ({@code GuiResearchTable.java:213-228}): the scribing slot empty, or its
     * tool fully used.
     */
    public boolean isOutOfInk() {
        final ItemStack stack = getItem(SLOT_INK);
        return stack.isEmpty() || stack.getDamageValue() >= stack.getMaxDamage();
    }

    // ------------------------------------------------------------- bonus aspects

    /** The original's public {@code bonusAspects} field. */
    public AspectList getBonusAspects() {
        return bonusAspects;
    }

    public int getBonus(final Aspect aspect) {
        return aspect == null ? 0 : bonusAspects.getAmount(aspect);
    }

    /** The original's {@code hasBonusAspect}. */
    public boolean hasBonusAspect(final Aspect aspect) {
        return aspect != null && bonusAspects.getAmount(aspect) > 0;
    }

    /** The original's {@code useBonusAspect}: remove one, mark dirty, resync. */
    public void useBonusAspect(final Aspect aspect) {
        if (!hasBonusAspect(aspect)) {
            return;
        }
        bonusAspects.remove(aspect, 1);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getNextRecalc() {
        return nextRecalc;
    }

    /**
     * The original's {@code recalculateBonus()} (its :188-303): the ambient roll
     * that feeds this pool from the world above the table (a light source gives
     * AIR, running water WATER, lava FIRE, and so on) every 600 ticks. Its sources
     * are the world-scan and M4 device surfaces, so the port's bonus pool stays
     * what the player and a future importer put into it. BD-19; the named
     * follow-up is the M4 device slice.
     */
    public void recalculateBonus() {
        // Intentionally empty, as the original's method is when its world
        // conditions never fire: nothing in the port feeds a research table yet.
    }

    // ----------------------------------------------------------- combineAspects

    /**
     * The original's aspect combination (the research table's own
     * {@code combineAspects}, and the shape the GUI's sneak-compound shortcut
     * takes through {@code PacketAspectCombinationToServer}, whose handler is
     * reproduced at its :71-126):
     *
     * <ol>
     *   <li>the player must know {@code RESEARCHER1} — the table's own gate;</li>
     *   <li>the result is the compound whose two declared components are this
     *       pair, in either order ({@code ResearchManager.getCombinationResult},
     *       which the port has as {@link AspectCombinations#getCombinationResult});</li>
     *   <li>a result the player already knows needs {@code RESEARCHER2}, else the
     *       combination is refused (the original sends its "researcher2"
     *       tooltip packet);</li>
     *   <li>pay <b>one of each</b> aspect: from the player's pool when it has one
     *       — the original's own precedence, {@code pool > 0 || !fromTable} pays
     *       the player even when a bonus is set — otherwise from this table's
     *       bonus pool, which must have one;</li>
     *   <li>on a first discovery of the result, +2 to each input's pool and the
     *       result discovered (+1), with the pool payloads the original sent.</li>
     * </ol>
     *
     * <p>Returns false on every refusal, paying nothing, as the original returns
     * without touching the pool. {@code aspect1} is the first select, paid with
     * {@code fromTable1} when its well was fed from the table's bonus pool.</p>
     *
     * <p>The player is a {@code Player}, not a {@code ServerPlayer}: the rule is
     * a write to the knowledge attachment, which any holder carries, and the
     * pool payloads are pushed only when that player is a server player (the
     * production caller, {@code ServerNetworkHooks.combineAspects}, always has
     * one — the original's handler had one by construction because the packet
     * came off the network). That keeps the rule provable by a game-test probe,
     * whose mock player is a plain {@code Player}.</p>
     */
    public boolean combineAspects(final Player player, final Aspect aspect1, final Aspect aspect2,
                                  final boolean fromTable1, final boolean fromTable2) {
        if (level == null || level.isClientSide || player == null || aspect1 == null || aspect2 == null) {
            return false;
        }
        PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        if (knowledge == null) {
            knowledge = PlayerKnowledgeData.empty();
        }
        if (!knowledge.knowsResearch(RESEARCHER1)) {
            return false;
        }
        final Aspect result = AspectCombinations.getCombinationResult(aspect1, aspect2);
        final boolean known = result != null && knowledge.hasAspectDiscovered(result.getTag());
        if (known && !knowledge.knowsResearch(RESEARCHER2)) {
            return false;
        }
        final int amount1 = knowledge.aspectAmount(aspect1.getTag());
        final int amount2 = knowledge.aspectAmount(aspect2.getTag());
        // The original's gate: each aspect needs a pool unit, or a flag saying its
        // well came from the table.
        if (amount1 <= 0 && !fromTable1) {
            return false;
        }
        if (amount2 <= 0 && !fromTable2) {
            return false;
        }
        // The original pays the player whenever their pool has a unit, and only
        // falls back to the table's bonus when the pool is empty and the flag is set.
        PlayerKnowledgeData paid = knowledge;
        if (amount1 > 0) {
            paid = paid.withAspectPoolAdded(aspect1.getTag(), -1);
        } else {
            if (!hasBonusAspect(aspect1)) {
                return false;
            }
            useBonusAspect(aspect1);
        }
        if (paid.aspectAmount(aspect2.getTag()) > 0) {
            paid = paid.withAspectPoolAdded(aspect2.getTag(), -1);
        } else {
            if (!hasBonusAspect(aspect2)) {
                return false;
            }
            useBonusAspect(aspect2);
        }
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, paid);
        sendPool(player, aspect1, paid.aspectAmount(aspect1.getTag()));
        sendPool(player, aspect2, paid.aspectAmount(aspect2.getTag()));
        if (result != null && !known) {
            // The original: add(2, aspect1, aspect2) — +2 to each input's pool,
            // marking both discovered — then discover the result.
            PlayerKnowledgeData bonus = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
            if (bonus == null) {
                bonus = PlayerKnowledgeData.empty();
            }
            bonus = bonus.withAspectDiscovered(aspect1.getTag(), 2)
                    .withAspectDiscovered(aspect2.getTag(), 2)
                    .withAspectDiscovered(result.getTag(), 1);
            player.setData(ModAttachments.PLAYER_KNOWLEDGE, bonus);
            sendPool(player, aspect1, bonus.aspectAmount(aspect1.getTag()));
            sendPool(player, aspect2, bonus.aspectAmount(aspect2.getTag()));
            sendPool(player, result, bonus.aspectAmount(result.getTag()));
            // The original also calls ResearchManager.completeAspects(username, res,
            // discoveredResearch), which reveals research pages that cost these
            // aspects. The port's research manager has no such entry point (TC-M3-02
            // ships the completion request the Thaumonomicon sends, not an
            // aspect-driven reveal), and the named follow-up is t_9135351b; the
            // discovery above is real, the auto-reveal is not.
        }
        return true;
    }

    /**
     * The original's {@code PacketAspectPool(tag, 0, pool)} after a payment. The
     * payload needs a {@code ServerPlayer}, so a non-server player simply gets
     * the write, not the reply.
     */
    private static void sendPool(final Player player, final Aspect aspect, final int pool) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new AspectPoolPayload(aspect.getTag(), 0, pool));
        }
    }

    /**
     * The original's {@code duplicate} (its :425-463): copy a finished note,
     * paying one of every aspect in the entry's cost plus one extra per copy
     * already made, and consuming a compass and a writable book from the player.
     *
     * <p>It cannot run in the port today. It opens with
     * {@code gatherResults()} → {@code ResearchManager.getData(contents[1])}, the
     * note's {@code ResearchNoteData} (the research key it copies and its
     * {@code copies} count), and requires the note at damage 64 — the note item
     * and that data component are the note card's lane
     * ({@code TcComponents.NOTE}, unmerged on {@code wt/note-item}); the notes
     * slot therefore cannot hold a note at all. This body keeps the seam the GUI
     * presses — menu button 5, the original's {@code case 5: tileEntity
     * .duplicate(player)} at ContainerResearchTable.java:43 — and refuses. BD-19
     * states it; the note card replaces this body.</p>
     */
    public void duplicate(final Player player) {
        if (level == null || level.isClientSide || player == null) {
            return;
        }
        ThaumcraftClassic.LOGGER.debug(
                "research table duplicate refused at {}: the note data lane (TcComponents.NOTE) is "
                        + "unmerged, so slot {} can hold no finished note to copy (BD-19)",
                worldPosition, SLOT_NOTES);
    }

    /**
     * The original's {@code placeAspect(q, r, aspect, player)} (its :122-186): the
     * hex-minigame drop. It writes a {@code ResearchManager.HexEntry} into
     * {@code data.hexEntries}/{@code data.hexes} keyed by the hex coordinate, pays
     * one aspect from the pool or this table's bonus, spends one ink, and on a
     * completed note sets the note to damage 64. Every one of those steps is the
     * note's data component, so the seam stays here: the payload arrives
     * ({@code AspectPlaceToServerPayload}, decoded field-for-field) and the
     * research minigame card owns this body. BD-19; the named follow-up is
     * {@code wt/note-item} / t_9135351b.
     */
    public void placeAspect(final int q, final int r, final Aspect aspect, final Player player) {
        ThaumcraftClassic.LOGGER.debug(
                "research table placeAspect ignored at {}: ({},{}) = {} — the hex grid lives on the "
                        + "note's data component, which is unmerged (BD-19)",
                worldPosition, q, r, aspect == null ? "(clear)" : aspect.getName());
    }

    // ------------------------------------------------------------- persistence

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // The original's keys and shape (TileResearchTable.java:43-95): "Inventory"
        // with a "Slot" byte and the inline item, "nextRecalc", and "bonusAspects"
        // as a list of {tag} compounds — one entry per aspect.
        final ListTag items = new ListTag();
        for (int i = 0; i < contents.length; i++) {
            final ItemStack stack = contents[i];
            if (stack.isEmpty()) {
                continue;
            }
            final CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) i);
            entry.put("Item", stack.save(registries));
            items.add(entry);
        }
        tag.put("Inventory", items);
        tag.putInt("nextRecalc", nextRecalc);
        final ListTag bonus = new ListTag();
        for (final Aspect aspect : bonusAspects.getAspects()) {
            if (aspect != null && bonusAspects.getAmount(aspect) > 0) {
                final CompoundTag entry = new CompoundTag();
                entry.putString("tag", aspect.getName());
                bonus.add(entry);
            }
        }
        tag.put("bonusAspects", bonus);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < contents.length; i++) {
            contents[i] = ItemStack.EMPTY;
        }
        final ListTag items = tag.getList("Inventory", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(CONTAINER_SIZE, items.size()); i++) {
            final CompoundTag entry = items.getCompound(i);
            final int slot = entry.getByte("Slot") & 255;
            if (slot >= 0 && slot < contents.length && entry.contains("Item")) {
                contents[slot] = ItemStack.parseOptional(registries, entry.getCompound("Item"));
            }
        }
        nextRecalc = tag.getInt("nextRecalc");
        for (final Tag element : tag.getList("bonusAspects", Tag.TAG_COMPOUND)) {
            if (element instanceof CompoundTag entry && entry.contains("tag", Tag.TAG_STRING)) {
                final Aspect aspect = Aspect.getAspect(entry.getString("tag"));
                if (aspect != null) {
                    bonusAspects.merge(aspect, 1);
                }
            }
        }
    }

    // ---------------------------------------------------------- menu / block wire

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.thaumcraft.gui_research_table");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new ResearchTableMenu(containerId, inventory, this);
    }

    @Override
    public boolean stillValid(final Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return null;
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return null;
    }
}
