package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.tiles.TileDeconstructionTable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Deconstruction table menu (content-mapping.csv {@code thaumcraft:gui_decon_table},
 * original {@code ContainerDeconstructionTable}).
 *
 * <p>One input slot (items that carry aspects only) plus the player
 * inventory; the break-time progress is a data slot. Menu button 1 is the
 * original's "collect the revealed aspect into your research pool" action —
 * it adds the aspect to the player's aspect pool via the knowledge
 * attachment (original {@code PlayerKnowledge.addAspectPool}).</p>
 *
 * <p><b>Two data slots, both in the original's channel.</b> 1.7.10's
 * {@code GuiDeconstructionTable} drew its tag as {@code UtilsFX.drawTag(64, 48,
 * this.table.getData().getRevealedAspect())} — line 39 — because the screen held
 * the tile, whose {@code aspect} field is server-side state. 1.21.1 hands a screen
 * only its menu, and the tile's {@code getUpdatePacket} carries only the item
 * stacks, so the revealed aspect travels in the menu's container data (BD-17):
 * index 0 the break timer, index 1 the aspect's index in
 * {@link Aspect#getPrimalAspects()} (the tile picks from that list, so the index is
 * total, and {@code -1} is the empty box).</p>
 */
public class DeconstructionTableMenu extends AbstractContainerMenu {

    private final TileDeconstructionTable table;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(final int index) {
            if (index == DATA_ASPECT) {
                return encodeAspectIndex(DeconstructionTableMenu.this.table.aspect);
            }
            return DeconstructionTableMenu.this.table.breaktime;
        }

        @Override
        public void set(final int index, final int value) {
            if (index == DATA_ASPECT) {
                DeconstructionTableMenu.this.table.aspect = decodeAspectIndex(value);
            } else {
                DeconstructionTableMenu.this.table.breaktime = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public DeconstructionTableMenu(final int id, final Inventory playerInventory, final TileDeconstructionTable table) {
        super(null, id);
        this.table = table;
        this.addSlot(new Slot(table, 0, 64, 16) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return DeconstructionTableMenu.this.table.canPlaceItem(0, stack);
            }
        });
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    final void addPlayerInventory(final Inventory playerInventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    /**
     * The tile's break timer, in the original's units: it is set to
     * {@code TileDeconstructionTable.BREAK_TIME} when a break starts and counts
     * down, so the original's bar was
     * {@code getBreakTimeScaled(46) = breaktime * 46 / 40} px tall and drained
     * downward. The client's menu instance holds the client tile, which the
     * data slot below syncs into ({@link ContainerData#set}), so a screen can
     * read the live value through this getter (TC-M7-04-s).
     */
    public int getBreakTime() {
        return this.table.breaktime;
    }

    /**
     * The menu's container data slot carrying the break timer (original's own
     * channel), now index 0 of two.
     */
    public static final int DATA_BREAK_TIME = 0;

    /**
     * The menu's second container data slot: the revealed aspect, as its index in
     * {@link Aspect#getPrimalAspects()}, or {@code -1} for an empty box. The tile
     * chose it with {@code primals.getAspects().get(random.nextInt(size))} over that
     * same list, so the index is total over everything the box can show.
     */
    public static final int DATA_ASPECT = 1;

    /**
     * How many container data slots this menu syncs. The screen's whole view of
     * tile state is this channel, which is why the tag needs slot 1 to exist at all
     * (BD-17); {@code AspectTagGameTest} pins the count so a third value cannot
     * silently drop the aspect.
     */
    public int dataSlotCount() {
        return this.data.getCount();
    }

    /**
     * The wire value one data slot carries — the number the client's
     * {@code ContainerData#set} receives and writes back into the client's tile.
     * Probed by {@code AspectTagGameTest}; reading it is read-only.
     */
    public int dataValue(final int index) {
        return this.data.get(index);
    }

    /**
     * The aspect sitting in the table's box, ready to be collected. On the server
     * this is the tile's own field; on the client it is the value the data slot
     * above wrote back, which is why the screen can finally draw the original's tag
     * (BD-17). {@code null} is the original's empty box.
     */
    @Nullable
    public Aspect getRevealedAspect() {
        return this.table.aspect;
    }

    /**
     * The wire value for a revealed aspect: its primal index, or {@code -1} for none.
     * Compared by tag, not by reference — the registry interns its instances, but the
     * channel's contract is the tag, and a tag comparison cannot be broken by a future
     * registry that hands out copies.
     */
    public static int encodeAspectIndex(@Nullable final Aspect aspect) {
        if (aspect == null) {
            return -1;
        }
        final List<Aspect> primals = Aspect.getPrimalAspects();
        for (int i = 0; i < primals.size(); i++) {
            if (primals.get(i).getTag().equals(aspect.getTag())) {
                return i;
            }
        }
        return -1;
    }

    /** The aspect for a wire value; anything out of range is the empty box. */
    @Nullable
    public static Aspect decodeAspectIndex(final int index) {
        final List<Aspect> primals = Aspect.getPrimalAspects();
        return index >= 0 && index < primals.size() ? primals.get(index) : null;
    }

    // MC 1.21.1 declares clickMenuButton as boolean (true = the button did something).
    @Override
    public boolean clickMenuButton(final Player player, final int buttonId) {
        if (buttonId == 1 && this.table.aspect != null && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            final PlayerKnowledgeData knowledge = sp.getData(ModAttachments.PLAYER_KNOWLEDGE);
            sp.setData(ModAttachments.PLAYER_KNOWLEDGE,
                    knowledge.withAspectPoolAdded(this.table.aspect.getTag(), 1));
            this.table.aspect = null;
            this.table.setChanged();
            sp.level().blockEntityChanged(this.table.getBlockPos());
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        // Original shift-click rules: input slot only accepts aspect-bearing
        // items (enforced by mayPlace); everything else uses default movement.
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        if (!moveItemStackTo(stack, 1, this.slots.size(), false)) {
            return ItemStack.EMPTY;
        }
        // MC 1.21.1 Slot has no onQuickMove hook; moveItemStackTo already
        // marks the container changed via Slot#set, so nothing extra is needed.
        return stack.copy();
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.table.stillValid(player);
    }
}
