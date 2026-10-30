package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.registration.ModAspects;
import com.jonnyutah12.thaumcraftclassic.tiles.TileDeconstructionTable;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

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
 * <p><b>Aspect sync (TC-M7-04-m4).</b> The original GUI read
 * {@code tableInventory.aspect} because the 1.7.10 container held the tile;
 * 1.21.1 hands the screen only the menu, so the revealed aspect travels in
 * container data slot {@link #DATA_ASPECT} as the aspect's id in the
 * {@code thaumcraft:aspect} registry plus one — 0 means "no aspect revealed".
 * The registry id (not a hard-coded index) is the encoding the client and
 * server agree on without shipping a table of their own. The original synced
 * only {@code breaktime} (its {@code func_75132_a}/{@code func_75142_b});
 * this extra slot is the BD-17 closure for the aspect icon.</p>
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
                // The client's menu instance holds the client tile: the sync
                // lands here, so the screen's getter reads the synced value.
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
     * The aspect waiting to be collected, or null. Server side this is the
     * tile's live field; client side it is the value data slot
     * {@link #DATA_ASPECT} last synced in, so {@code DeconstructionTableScreen}
     * can draw the original's 16x16 tag at {@code (64,48)} (BD-17 closure,
     * TC-M7-04-m4).
     */
    @Nullable
    public Aspect getRevealedAspect() {
        return this.table.aspect;
    }

    /** The menu's container data slot carrying the break timer (original's own channel). */
    public static final int DATA_BREAK_TIME = 0;

    /** The menu's container data slot carrying the revealed aspect's registry id plus one (0 = none). */
    public static final int DATA_ASPECT = 1;

    /** The {@link #DATA_ASPECT} encoding: registry id + 1, 0 for no aspect. */
    public static int encodeAspectIndex(@Nullable final Aspect aspect) {
        if (aspect == null) {
            return 0;
        }
        final Registry<Aspect> registry = ModAspects.registry();
        final int id = registry == null ? -1 : registry.getId(aspect);
        return id < 0 ? 0 : id + 1;
    }

    /** The {@link #DATA_ASPECT} decoding; any id the registry does not know reads as "none". */
    @Nullable
    public static Aspect decodeAspectIndex(final int encoded) {
        if (encoded <= 0) {
            return null;
        }
        final Registry<Aspect> registry = ModAspects.registry();
        return registry == null ? null : registry.byId(encoded - 1);
    }

    /** The container data slots of this menu, for probes that prove the sync lands. */
    public ContainerData containerData() {
        return this.data;
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
