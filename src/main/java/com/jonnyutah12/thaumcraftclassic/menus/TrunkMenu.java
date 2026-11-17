package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.entities.TrunkHost;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Traveling Trunk inventory GUI (content-mapping.csv {@code thaumcraft:GUI_TRUNK},
 * original gui_id 2 / {@code ContainerTravelingTrunk}).
 *
 * <p>Original: {@code trunk.getRows()} rows of 9 slots over the trunk's live
 * {@code InventoryTrunk} (y = 15 + row·23), then the standard 3×9 + hotbar
 * player inventory (y = 118+k·18 / 176). Opening marks the trunk open and
 * plays the chest-open sound (0.5 / rand·0.1+0.9); closing reverses both. The
 * GUI's stay-toggle button (button 1) flips the trunk's follow mode. Shift-
 * clicks move between the trunk region and the player region.</p>
 *
 * <p>The trunk entity is TC-M6-06's; this menu is written against the
 * {@link TrunkHost} seam. TC-M6-06 opens it with
 * {@code player.openMenu(new SimpleMenuProvider(...), buf -> { buf.writeInt(id);
 * buf.writeInt(rows); })} — the extra buffer carries the entity id and row
 * count so the client menu can resolve the live entity and lay out the same
 * slots (the original rebuilt its container from the openGui x/y args the same
 * way).</p>
 *
 * <p>Stay-flag sync (TC-M7-04-m4, BD-17): the 1.7.10 GUI read
 * {@code themob.getStay()} because it held the mob; a 1.21.1 screen gets only
 * the menu, so the stay flag travels in container data slot
 * {@link #DATA_STAY} and {@link #isStaying()} feeds the screen's 10x10 stay
 * control, its enabled box, and its click text. The stay flag itself is an
 * entity field (registerTrunkSync); the mob's own network sync is the trunk
 * entity card's job (TC-M6-06).</p>
 */
public class TrunkMenu extends AbstractContainerMenu {

    /** Menu button id of the stay toggle (original buttonClicked(1)). */
    public static final int BUTTON_TOGGLE_STAY = 1;

    /** Data slot carrying the stay flag (1 = staying, 0 = following), synced to the client. */
    public static final int DATA_STAY = 0;

    private final TrunkHost trunk;
    private final int numRows;
    private final boolean clientSide;
    private int syncedStay;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(final int index) {
            return TrunkMenu.this.stayValue();
        }

        @Override
        public void set(final int index, final int value) {
            // Server -> client landing point; the screen reads isStaying().
            TrunkMenu.this.syncedStay = value;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    /** Server side: bound to the live trunk entity. */
    public TrunkMenu(final int id, final Inventory playerInventory, final TrunkHost trunk) {
        super(CatalogTrinketsLoot.GUI_TRUNK.get(), id);
        this.trunk = trunk;
        this.numRows = trunk.trunkRows();
        this.clientSide = playerInventory.player.level().isClientSide;
        build(trunk, playerInventory);
        addDataSlots(data);
        trunk.setTrunkOpen(true);
        playChest(trunk, SoundEvents.CHEST_OPEN);
    }

    /** Client side: resolve the entity from the open-menu extra buffer. */
    public TrunkMenu(final int id, final Inventory playerInventory, final RegistryFriendlyByteBuf extraData) {
        super(CatalogTrinketsLoot.GUI_TRUNK.get(), id);
        final int entityId = extraData.readInt();
        final int rows = extraData.readInt();
        TrunkHost resolved = null;
        if (playerInventory.player.level().getEntity(entityId) instanceof TrunkHost host) {
            resolved = host;
        }
        this.trunk = resolved;
        this.numRows = resolved != null ? rows : 0;
        this.clientSide = playerInventory.player.level().isClientSide;
        if (resolved != null) {
            build(resolved, playerInventory);
        } else {
            bindPlayerInventory(playerInventory);
        }
        addDataSlots(data);
    }

    private int stayValue() {
        if (this.clientSide) {
            // The screen's source of truth on the client is the data slot; the
            // entity's own stay sync is the trunk entity card's (TC-M6-06).
            return this.syncedStay;
        }
        return this.trunk != null && this.trunk.trunkStay() ? 1 : 0;
    }

    /**
     * The stay mode the GUI's 10x10 control shows: live entity state on the
     * server, the data-slot-synced value on the client (TC-M7-04-m4, BD-17 —
     * the original read {@code themob.getStay()} because its GUI held the mob).
     */
    public boolean isStaying() {
        return stayValue() == 1;
    }

    /** The trunk this menu is bound to (the client's resolved entity), or null. */
    public TrunkHost getTrunk() {
        return this.trunk;
    }

    /** The container data slots of this menu, for probes that prove the sync lands. */
    public ContainerData containerData() {
        return this.data;
    }

    /**
     * Row count the trunk grid was built with (original
     * {@code trunk.getRows()}). 1.21.1's container screen needs it to size its
     * panel, exactly as {@code ChestMenu.getRowCount()} feeds
     * {@code ContainerScreen}; a client menu whose trunk entity could not be
     * resolved reports 0, so the screen draws only the player grid.
     */
    public int getRowCount() {
        return this.numRows;
    }

    private void build(final TrunkHost host, final Inventory playerInventory) {
        // Original trunk grid: 9 columns, rows at y = 15 + row*23.
        for (int row = 0; row < this.numRows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(host.trunkInventory(), col + row * 9, 8 + col * 18, 15 + row * 23));
            }
        }
        bindPlayerInventory(playerInventory);
    }

    private void bindPlayerInventory(final Inventory inventoryPlayer) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventoryPlayer, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventoryPlayer, col, 8 + col * 18, 176));
        }
    }

    /** Original buttonClicked(1): flip the trunk's stay mode. */
    @Override
    public boolean clickMenuButton(final Player player, final int buttonId) {
        if (buttonId == BUTTON_TOGGLE_STAY && trunk != null && !player.level().isClientSide) {
            trunk.toggleTrunkStay();
            return true;
        }
        return false;
    }

    /** Original shift-click: trunk region ↔ player region. */
    @Override
    public ItemStack quickMoveStack(final Player playerMoving, final int index) {
        ItemStack result = ItemStack.EMPTY;
        final Slot slot = this.slots.get(index);
        final int trunkEnd = this.numRows * 9;
        if (slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < trunkEnd) {
                if (!this.moveItemStackTo(stack, trunkEnd, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, trunkEnd, false)) {
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

    /** Original onContainerClosed: mark closed + chest-closed sound. */
    @Override
    public void removed(final Player playerLeaving) {
        super.removed(playerLeaving);
        if (trunk != null && !playerLeaving.level().isClientSide) {
            trunk.setTrunkOpen(false);
            playChest(trunk, SoundEvents.CHEST_CLOSE);
        }
    }

    private static void playChest(final TrunkHost trunk, final net.minecraft.sounds.SoundEvent sound) {
        final var entity = trunk.asEntity();
        entity.level().playSound(null, entity, sound, SoundSource.BLOCKS, 0.5F,
                entity.level().getRandom().nextFloat() * 0.1F + 0.9F);
    }

    /** Original doCheckKeyBinds: always valid (the trunk follows the player). */
    @Override
    public boolean stillValid(final Player playerCheck) {
        return true;
    }
}
