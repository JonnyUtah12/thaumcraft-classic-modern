package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnace;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the alchemical furnace screen (the original {@code gui_id} 9,
 * {@code ContainerAlchemyFurnace}).
 *
 * <p>Geometry cites the original container exactly: the input slot at
 * {@code (80,8)} — {@code ContainerAlchemyFurnace} wraps that slot in
 * {@code SlotLimitedHasAspects}, whose port analogue is the tile's own smelt
 * gate, {@link TileArcaneFurnace#canSmelt} (the tile model smelts and
 * destroys inputs with no smelt result, per its class doc, so the input slot
 * accepts exactly the smeltable class) — and the second slot at
 * {@code (80,48)}, plain in the original (its fuel slot); the port's
 * {@link TileArcaneFurnace} has no fuel slot, so it stays a plain machine
 * slot. The vanilla player grid sits at {@code 84}/{@code 142}
 * ({@link ContainerLayout#PLAYER_GRID_TOP}); the original keeps the 1.7.10
 * {@code GuiContainer} default panel.</p>
 *
 * <p>The cook progress travels in two data slots (0 = furnaceCookTime,
 * 1 = furnaceMaxCookTime), the same {@link ContainerData} shape the
 * deconstruction and spa menus ship, so the screen draws from synced state
 * only.</p>
 *
 * <p>{@link #quickMoveStack} reproduces the original {@code func_82846_b}
 * branch for branch: machine slots shift-click out to the player, a
 * machine-class item shifts in through the input slot first, and the player's
 * hotbar and main grid trade places. The original's fuel/aspect branches
 * target the 1.7.10 aspect-fuel economy; the port's tile burns nothing, so
 * the machine-class branch tests {@link TileArcaneFurnace#canSmelt} (the
 * tile's own fill-input test) in place of the fuel test, keeping the
 * input-first fill order.</p>
 */
public final class AlchemyFurnaceMenu extends AbstractContainerMenu {

    /** The player grid starts at slot index 2, vanilla's convention. */
    private static final int PLAYER_INVENTORY_START = 2;
    /** The original's {@code par2 >= 2 && par2 < 29} main-grid bound. */
    private static final int PLAYER_MAIN_END = PLAYER_INVENTORY_START + 27;
    /** The original's {@code par2 >= 29 && par2 < 38} hotbar bound. */
    private static final int PLAYER_END = PLAYER_INVENTORY_START + 36;

    private final TileArcaneFurnace furnace;
    private final ContainerData data;

    public AlchemyFurnaceMenu(final int windowId, final Inventory playerInventory,
                              final TileArcaneFurnace furnace) {
        super(null, windowId);
        this.furnace = furnace;
        this.checkContainerSize(furnace, TileArcaneFurnace.SLOTS);
        // ContainerAlchemyFurnace: slot 0 at (80,8) (SlotLimitedHasAspects), slot 1 at (80,48).
        addSlot(new Slot(furnace, 0,
                ContainerLayout.FURNACE_SLOT_INPUT_X, ContainerLayout.FURNACE_SLOT_INPUT_Y) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                // SlotLimitedHasAspects's port analogue: the tile's own smelt gate.
                return furnace.canSmeltStack(stack);
            }
        });
        addSlot(new Slot(furnace, 1,
                ContainerLayout.FURNACE_SLOT_FUEL_X, ContainerLayout.FURNACE_SLOT_FUEL_Y));
        // Vanilla player grid (the original GuiContainer default): 3x9 at y = 84, hotbar at 142.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, ContainerLayout.PLAYER_GRID_TOP + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column,
                    8 + column * 18, ContainerLayout.PLAYER_HOTBAR_TOP));
        }
        this.data = new ContainerData() {
            @Override
            public int get(final int index) {
                return index == 0 ? furnace.getFurnaceCookTime() : furnace.getFurnaceMaxCookTime();
            }

            @Override
            public void set(final int index, final int value) {
                if (index == 0) {
                    furnace.setFurnaceCookTime(value);
                } else {
                    furnace.setFurnaceMaxCookTime(value);
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        addDataSlots(this.data);
    }

    @Override
    public boolean stillValid(final Player player) {
        return furnace.stillValid(player);
    }

    /** The synced cook progress for the screen's 46 px cook rail (data slot 0). */
    public int getCookTime() {
        return data.get(0);
    }

    /** The synced cook duration the progress divides over (data slot 1). */
    public int getMaxCookTime() {
        return data.get(1);
    }

    /**
     * Write through the same {@link ContainerData} instance {@link #addDataSlots}
     * registered, so a probe can exercise the exact set path the server uses
     * when a client clicks the window.
     */
    public void setDataValue(final int index, final int value) {
        data.set(index, value);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack stack = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return stack;
        }
        final ItemStack slotStack = slot.getItem();
        stack = slotStack.copy();
        if (index == 0 || index == 1) {
            // Machine slots shift-click out to the whole player inventory.
            if (!slotStack.isEmpty()
                    && !moveItemStackTo(slotStack, PLAYER_INVENTORY_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (furnace.canSmeltStack(slotStack)) {
            // Machine-class item: fill the GUI slots, input first — the
            // original's fill order (isItemFuel replaced by the tile's own
            // smelt test; the tile burns nothing, per its class doc).
            if (!moveItemStackTo(slotStack, 0, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_MAIN_END) {
            if (!moveItemStackTo(slotStack, PLAYER_MAIN_END, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_MAIN_END && index < PLAYER_END) {
            if (!moveItemStackTo(slotStack, PLAYER_INVENTORY_START, PLAYER_MAIN_END, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return stack;
    }
}
