package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneWorkbench;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Arcane workbench menu (content-mapping.csv {@code thaumcraft:gui_arcane_workbench},
 * original gui_id 13 / {@code ContainerArcaneWorkbench}).
 *
 * <p><b>Slot order and anchors are the original's, verbatim</b>
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/container/ContainerArcaneWorkbench.java}):
 * menu slot 0 is the tile's result slot 9 at {@code (160,64)}, slot 1 the wand slot
 * (tile 10) at {@code (160,24)}, slots 2..10 the 3x3 grid (tile 0..8) on the
 * original's <b>24 px</b> pitch at {@code (40 + col*24, 40 + row*24)}, then the
 * player grid at {@code (16 + col*18, 151 + row*18)} and the hotbar at
 * {@code (16 + i*18, 209)} — the original's 190 x 234 panel, wider <b>and</b> taller
 * than vanilla's 176 x 166, which
 * is why {@code client/ContainerLayout} carries the workbench's own figures. The
 * anchors come from {@link ContainerLayout} rather than being restated here, so the
 * screen's frames and this menu's slots cannot drift.</p>
 *
 * <p><b>The craft cycle is the original's container tick.</b>
 * {@link #broadcastChanges()} is where 1.7.10 ran
 * {@code updateContainer}: on the server side it runs
 * {@link TileArcaneWorkbench#updateCrafting} — find the match, pay the wand, fill
 * slot 9 only while it is empty — and then writes the vis-cost display into the
 * menu's data slots. Those slots, not a client-side guess, are what the screen
 * draws: 0..5 the match's per-primal price in centi-vis (the original's
 * {@code * 100}), 6..11 the wand's per-primal charge in centi-vis, 12 the
 * "Insufficient vis" flag (the original's {@code consumeAllVisCrafting(..., false)}
 * simulate call, computed where the price and the pool actually live), 13 whether a
 * recipe matched at all.</p>
 *
 * <p><b>Taking the result is {@code SlotCraftingArcaneWorkbench.onCraft}</b>: one
 * item out of every grid cell, and the original's <i>second</i> vis charge
 * ({@code consumeAllVisCrafting(..., true)}) — the 1.7.10 workbench charged the vis
 * once to reveal the result and again to lift it, and that is reproduced rather
 * than tidied. The result slot accepts nothing.</p>
 *
 * <p>Two original gates are expressed through the port's seams (recorded in
 * {@code docs/BEHAVIORAL_DEVIATIONS.md} BD-18): {@code SlotLimitedByWand} gated on
 * {@code instanceof ItemWandCasting && !isStaff}, and the port's seam for "holds
 * vis" is the TC-M4-02 {@code thaumcraft:vis_pool} component — the same seam
 * {@link TileArcaneWorkbench#canPlaceItemThroughFace} uses, so the staff exclusion
 * arrives with the wand family (TC-M6-01). The original's
 * {@code FMLCommonHandler.firePlayerCraftingEvent} and {@code onCrafting} stat
 * hooks have no 1.21.1 analogue at this seam.</p>
 */
public class ArcaneWorkbenchMenu extends AbstractContainerMenu {

    /** Menu slot of the tile's result slot 9 (original: {@code SlotCraftingArcaneWorkbench}). */
    public static final int SLOT_RESULT = 0;
    /** Menu slot of the tile's wand slot 10 (original: {@code SlotLimitedByWand}). */
    public static final int SLOT_WAND = 1;
    /** Menu slots 2..10 are the tile's grid slots 0..8, row-major as in the original. */
    public static final int GRID_FIRST = 2;
    public static final int GRID_SIZE = 9;
    /** The player grid starts at menu slot 11, the hotbar at 38 (original offsets 11 / 38). */
    public static final int PLAYER_FIRST = GRID_FIRST + GRID_SIZE;
    public static final int HOTBAR_FIRST = PLAYER_FIRST + 27;
    public static final int SLOT_COUNT = PLAYER_FIRST + 36;

    /** Data slots: 0..5 the per-primal price, 6..11 the wand's per-primal charge. */
    public static final int DATA_COST = 0;
    public static final int DATA_WAND_CHARGE = 6;
    public static final int DATA_INSUFFICIENT = 12;
    public static final int DATA_HAS_MATCH = 13;
    public static final int DATA_COUNT = 14;
    /** The six primal aspects the original's hexagon of cost tags is drawn from. */
    public static final int PRIMALS = 6;

    private final TileArcaneWorkbench bench;
    private final Inventory playerInventory;
    private final int[] values = new int[DATA_COUNT];

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(final int index) {
            return ArcaneWorkbenchMenu.this.values[index];
        }

        @Override
        public void set(final int index, final int value) {
            ArcaneWorkbenchMenu.this.values[index] = value;
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ArcaneWorkbenchMenu(final int id, final Inventory playerInventory,
                               final TileArcaneWorkbench bench) {
        super(null, id);
        this.bench = bench;
        this.playerInventory = playerInventory;

        // Original slot order: result, wand, 3x3 grid, player grid, hotbar.
        addSlot(new ResultSlot(bench, 9, ContainerLayout.WB_RESULT_X, ContainerLayout.WB_RESULT_Y));
        addSlot(new WandSlot(bench, 10, ContainerLayout.WB_WAND_X, ContainerLayout.WB_WAND_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(bench, col + row * 3,
                        ContainerLayout.workbenchGridX(col), ContainerLayout.workbenchGridY(row)));
            }
        }
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    /** The original's {@code bindPlayerInventory}-equivalent loops at (16,151) / (16,209). */
    private void addPlayerInventory(final Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        ContainerLayout.workbenchPlayerX(col), ContainerLayout.workbenchPlayerY(row)));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    ContainerLayout.workbenchPlayerX(col), ContainerLayout.WB_HOTBAR_TOP));
        }
    }

    /** The tile this menu drives. */
    public TileArcaneWorkbench bench() {
        return this.bench;
    }

    // ---------------------------------------------------- synced vis display

    /**
     * The current match's price for primal {@code index}, in centi-vis (0 with no
     * match). Synced, never guessed: the server writes it from
     * {@link TileArcaneWorkbench#currentCost}, the exact number
     * {@link TileArcaneWorkbench#craft} pays.
     */
    public int getCost(final int primalIndex) {
        return this.values[DATA_COST + primalIndex];
    }

    /** The wand slot's charge for primal {@code index}, in centi-vis. */
    public int getWandCharge(final int primalIndex) {
        return this.values[DATA_WAND_CHARGE + primalIndex];
    }

    /** The original's "Insufficient vis" condition, computed server-side. */
    public boolean isInsufficientVis() {
        return this.values[DATA_INSUFFICIENT] != 0;
    }

    /** Whether the grid currently matches an arcane recipe. */
    public boolean hasMatch() {
        return this.values[DATA_HAS_MATCH] != 0;
    }

    /**
     * The server half of {@code updateContainer}: run the craft cycle, then refresh
     * the cost display. Called from {@link #broadcastChanges()}, which the server
     * calls every tick for the player's open menu, so the cycle and the display
     * update on the same beat the original used.
     */
    public void serverTick() {
        this.bench.updateCrafting(this.playerInventory.player);
        refreshCostDisplay();
    }

    /**
     * Write the six prices, the six charges, the affordability flag and the match
     * flag into the data slots. The client's copy of these arrives through
     * {@link ContainerData#set}, which is why the screen can draw real numbers.
     */
    private void refreshCostDisplay() {
        if (this.bench.getLevel() == null) {
            return;
        }
        final var cost = this.bench.currentCost(this.playerInventory.player);
        if (cost == null) {
            for (int i = 0; i < PRIMALS; i++) {
                this.values[DATA_COST + i] = 0;
                this.values[DATA_WAND_CHARGE + i] = this.bench.wandCharge(primal(i));
            }
            this.values[DATA_INSUFFICIENT] = 0;
            this.values[DATA_HAS_MATCH] = 0;
            return;
        }
        final var centi = TileArcaneWorkbench.centiVisCost(cost);
        for (int i = 0; i < PRIMALS; i++) {
            final var aspect = primal(i);
            this.values[DATA_COST + i] = centi.getAmount(aspect);
            this.values[DATA_WAND_CHARGE + i] = this.bench.wandCharge(aspect);
        }
        // The original's gate is `wand != null && cost != null
        // && !consumeAllVisCrafting(..., false)` (GuiArcaneWorkbench.java:93): with
        // no vis-carrier in slot 10 the original drew NO notice at all — the tags
        // just pulsed at their low alpha. The port keeps that gate, so the flag
        // means "there is a wand and it cannot pay", not "the grid wants vis".
        this.values[DATA_INSUFFICIENT] = holdsVis(this.bench.getItem(TileArcaneWorkbench.SLOT_WAND))
                && !this.bench.canWandPay(centi) ? 1 : 0;
        this.values[DATA_HAS_MATCH] = 1;
    }

    /** The {@code i}-th primal, in the original's declaration order (aer, terra, ignis, aqua, ordo, perditio). */
    private static com.jonnyutah12.thaumcraftclassic.aspect.Aspect primal(final int index) {
        return com.jonnyutah12.thaumcraftclassic.aspect.Aspect.getPrimalAspects().get(index);
    }

    @Override
    public void broadcastChanges() {
        if (this.bench.getLevel() != null && !this.bench.getLevel().isClientSide) {
            serverTick();
        }
        super.broadcastChanges();
    }

    // ------------------------------------------------------- slot behaviours

    /**
     * The original's {@code SlotLimitedByWand.mayPlace}: a wand (and not a staff).
     * The port's seam is the vis-pool component, shared with
     * {@link TileArcaneWorkbench#canPlaceItemThroughFace}.
     */
    static boolean holdsVis(final ItemStack stack) {
        return !stack.isEmpty()
                && stack.getOrDefault(ModDataComponents.VIS_POOL.get(), null) != null;
    }

    /** The wand slot: vis-carriers only, stack limit 64 as the original's slot declared. */
    private static final class WandSlot extends Slot {
        WandSlot(final TileArcaneWorkbench bench, final int index, final int x, final int y) {
            super(bench, index, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return holdsVis(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }

    /**
     * The result slot: output-only, and taking from it runs the original's
     * {@code SlotCraftingArcaneWorkbench.onCraft} — spend one of every grid cell
     * and pay the vis a second time.
     */
    private final class ResultSlot extends Slot {
        ResultSlot(final TileArcaneWorkbench bench, final int index, final int x, final int y) {
            super(bench, index, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(final Player player, final ItemStack stack) {
            ArcaneWorkbenchMenu.this.bench.payVisOnTake(player);
            ArcaneWorkbenchMenu.this.bench.consumeGrid();
            ArcaneWorkbenchMenu.this.bench.setChanged();
        }
    }

    // ---------------------------------------------------- shift-click routing

    /**
     * The original's {@code transferStackInSlot} branches, kept branch for branch:
     * the result shifts to the player inventory (scanning backwards from the
     * hotbar), a wand in the player inventory shifts to the wand slot, everything
     * else shifts player-grid ⇄ hotbar, and a machine slot shifts to the player.
     *
     * <p>That leaves the 3x3 grid unreachable by shift-click from the player side —
     * the original's rule, reproduced verbatim rather than "improved".</p>
     */
    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index == SLOT_RESULT) {
            if (!moveItemStackTo(stack, PLAYER_FIRST, SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_FIRST && index < HOTBAR_FIRST) {
            if (holdsVis(stack)) {
                if (!moveItemStackTo(stack, SLOT_WAND, SLOT_WAND + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, HOTBAR_FIRST, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_FIRST && index < SLOT_COUNT) {
            if (holdsVis(stack)) {
                if (!moveItemStackTo(stack, SLOT_WAND, SLOT_WAND + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_FIRST, HOTBAR_FIRST, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_FIRST, SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.bench.stillValid(player);
    }
}
