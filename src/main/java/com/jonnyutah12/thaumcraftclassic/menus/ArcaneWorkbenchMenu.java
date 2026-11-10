package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.recipes.ArcaneWorkbenchRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.M403RecipeSerializers;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M403ArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.wands.VisCostRules;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Arcane workbench container (TC-M4-03; content-mapping.csv gui row
 * {@code thaumcraft:gui_arcane_workbench}).
 *
 * <p>Original: {@code ContainerArcaneWorkbench} +
 * {@code SlotCraftingArcaneWorkbench} + {@code SlotLimitedByWand}. Semantics
 * ported term-for-term:</p>
 * <ul>
 *   <li>result resolution order: vanilla crafting match first, and only if it
 *       misses, an arcane match — and the arcane result is offered only when
 *       the research gate passes AND the wand in slot 10 can pay the full
 *       cost ({@code consumeAllVisCrafting(..., false)} simulation in the
 *       original's updateCraftingResult);</li>
 *   <li>payment happens on take, after the grid is consumed
 *       ({@code SlotCraftingArcaneWorkbench.onPickupFromSlot});</li>
 *   <li>the crafting event ({@code firePlayerCraftingEvent} →
 *       {@code ItemCraftedEvent}, the research-trigger feed) fires from the
 *       result slot exactly like vanilla crafting;</li>
 *   <li>shift-click routing: result → inventory; wand (not staff) → wand
 *       slot; everything else → grid/inventory as vanilla;</li>
 *   <li>the grid lives in the tile and survives closing the GUI (the
 *       original stored it in NBT — unlike the vanilla crafting table).</li>
 * </ul>
 *
 * <p>Slot indices: 0 result, 1 wand, 2-10 grid, 11-37 inventory, 38-46
 * hotbar. Positions are the original GUI coordinates (190x234 texture).</p>
 */
public class ArcaneWorkbenchMenu extends AbstractContainerMenu {

    public static final int SLOT_RESULT = 0;
    public static final int SLOT_WAND = 1;
    public static final int SLOT_GRID_START = 2;
    public static final int SLOT_INV_START = 11;

    /**
     * Identity cap/gear parameters until the wand items land (TC-M6-01 cap
     * tables, TC-M8-02 gear discounts): modifier 1.0, no specials, no
     * discounts — the original's behaviour for a bare wand with no cap data.
     * The full TC-M0-06 chain is already implemented in {@link VisCostRules};
     * wiring it to real items is those cards' job.
     */
    private static final VisCostRules.WandCapCost IDENTITY_CAP =
            new VisCostRules.WandCapCost(1.0F, java.util.List.of(), 1.0F);

    /** What the GUI draws: matched recipe, effective centi-vis cost, flags. */
    public record Overlay(ArcaneWorkbenchRecipe recipe, AspectList effectiveCost,
                         boolean payable, boolean researchLocked) {

        public static final Overlay NONE = new Overlay(null, null, false, false);
    }

    private final TileArcaneWorkbench tile;
    private final Player player;
    private final Level level;
    private final ResultContainer resultContainer = new ResultContainer();
    private final TileCraftGrid craftGrid = new TileCraftGrid();

    /** Last server-resolved state; the client recomputes it for the overlay. */
    private ArcaneWorkbenchRecipe lastArcane;

    public ArcaneWorkbenchMenu(final int windowId, final Inventory playerInventory,
                               final TileArcaneWorkbench tile) {
        super(M403ArcaneWorkbench.MENU_TYPE.get(), windowId);
        this.tile = tile;
        this.player = playerInventory.player;
        this.level = this.player.level();

        this.addSlot(new ArcaneResultSlot(this.player, this.craftGrid, this.resultContainer,
                0, 160, 64));
        this.addSlot(new WandSlot(tile, TileArcaneWorkbench.SLOT_WAND, 160, 24));

        // Original grid geometry: 24px pitch, origin (40,40).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(tile, col + row * 3, 40 + col * 24, 40 + row * 24));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        16 + col * 18, 151 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 16 + col * 18, 209));
        }

        this.updateResult();
    }

    /** Client factory: the tile is looked up from the block pos written by
     *  {@link TileArcaneWorkbench#writeScreenOpeningData}. */
    public ArcaneWorkbenchMenu(final int windowId, final Inventory playerInventory,
                               final RegistryFriendlyByteBuf buf) {
        this(windowId, playerInventory, resolveTile(playerInventory.player.level(), buf.readBlockPos()));
    }

    private static TileArcaneWorkbench resolveTile(final Level level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TileArcaneWorkbench tile) {
            return tile;
        }
        throw new IllegalStateException("no arcane workbench at " + pos);
    }

    @Override
    public boolean stillValid(final Player player) {
        return tile.stillValid(player);
    }

    // ------------------------------------------------------------- resolution

    /** Shared matcher: vanilla first, then research-gated + vis-gated arcane. */
    private Optional<RecipeHolder<CraftingRecipe>> findVanilla(final CraftingInput input) {
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(h -> (RecipeHolder<CraftingRecipe>) h);
    }

    /** The raw arcane match for the current grid, ignoring gate/payment. */
    public ArcaneWorkbenchRecipe findArcaneMatch() {
        final CraftingInput input = craftGrid.asCraftInput();
        return level.getRecipeManager()
                .getRecipeFor(M403RecipeSerializers.ARCANE_WORKBENCH_TYPE.get(), input, level)
                .map(h -> h.value())
                .orElse(null);
    }

    /**
     * The GUI overlay state, computed on both sides (the original GUI called
     * findMatchingArcaneRecipe + consumeAllVisCrafting(simulate) client-side;
     * recipes, knowledge and the wand slot are all synced to the client).
     */
    public Overlay overlay() {
        final ArcaneWorkbenchRecipe arcane = findArcaneMatch();
        if (arcane == null) {
            return Overlay.NONE;
        }
        if (!researchKnown(arcane)) {
            return new Overlay(arcane, null, false, true);
        }
        final AspectList cost = effectiveCost(arcane);
        final ItemStack wand = tile.getItem(TileArcaneWorkbench.SLOT_WAND);
        final boolean payable = !wand.isEmpty()
                && VisCostRules.canPay(WandVis.poolOf(wand), cost);
        return new Overlay(arcane, cost, payable, false);
    }

    private boolean researchKnown(final ArcaneWorkbenchRecipe recipe) {
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        return knowledge != null && ResearchManager.isResearchComplete(knowledge, recipe.research());
    }

    /**
     * Recipe price → centi-vis with the consumption chain applied
     * (original consumeAllVisCrafting: ×100 then per-aspect
     * getConsumptionModifier(crafting=true)).
     */
    private AspectList effectiveCost(final ArcaneWorkbenchRecipe recipe) {
        final AspectList centi = new AspectList();
        for (final var aspect : recipe.visCost().getAspects()) {
            centi.add(aspect, recipe.visCost().getAmount(aspect) * WandVis.CENTI_VIS_PER_VIS);
        }
        return VisCostRules.effectiveCosts(centi, IDENTITY_CAP, null, 0, false, true);
    }

    /** Server-side result refresh (original updateCraftingResult). */
    private void updateResult() {
        if (level.isClientSide) {
            return;
        }
        final CraftingInput input = craftGrid.asCraftInput();
        ItemStack display = ItemStack.EMPTY;
        lastArcane = null;

        final Optional<RecipeHolder<CraftingRecipe>> vanilla = findVanilla(input);
        if (vanilla.isPresent()) {
            final RecipeHolder<CraftingRecipe> holder = vanilla.get();
            resultContainer.setRecipeUsed(holder);
            final ItemStack assembled = holder.value().assemble(input, level.registryAccess());
            if (assembled.isItemEnabled(level.enabledFeatures())) {
                display = assembled;
            }
        } else {
            final Overlay overlay = overlay();
            if (overlay.recipe() != null) {
                lastArcane = overlay.recipe();
                if (overlay.payable()) {
                    display = overlay.recipe().assemble(input, level.registryAccess());
                }
            }
        }

        resultContainer.setItem(0, display);
        broadcastFullState();
    }

    @Override
    public void slotsChanged(final Container source) {
        updateResult();
    }

    // ------------------------------------------------------------- slot logic

    /**
     * Result slot: vanilla consumption + crafting event (ResultSlot fires
     * firePlayerCraftingEvent → ItemCraftedEvent, the research-trigger feed),
     * then the arcane payment and container-item handling from the original
     * SlotCraftingArcaneWorkbench.
     */
    private final class ArcaneResultSlot extends ResultSlot {

        ArcaneResultSlot(final Player player, final CraftingContainer craftContainer,
                         final Container resultContainer, final int slot,
                         final int x, final int y) {
            super(player, craftContainer, resultContainer, slot, x, y);
        }

        @Override
        public void onTake(final Player takingPlayer, final ItemStack stack) {
            // Capture the match BEFORE the grid is consumed (the original read
            // findMatchingArcaneRecipeAspects from the full matrix, and that
            // matcher applies the research gate but NOT the vanilla-priority
            // rule — the original paid vis whenever a gated arcane recipe
            // matched, even if a vanilla recipe won the display).
            ArcaneWorkbenchRecipe arcane = lastArcane;
            if (arcane == null) {
                final Overlay overlay = overlay();
                if (overlay.recipe() != null && !overlay.researchLocked()) {
                    arcane = overlay.recipe();
                }
            }
            final NonNullList<ItemStack> gridBefore = NonNullList.create();
            for (int i = 0; i < TileArcaneWorkbench.GRID_SIZE; i++) {
                gridBefore.add(tile.getItem(i).copy());
            }

            super.onTake(takingPlayer, stack);

            if (arcane != null) {
                payVis(arcane);
                restoreContainerItems(takingPlayer, gridBefore);
            }
        }

        /**
         * Original SlotCraftingArcaneWorkbench container-item handling:
         * vanilla's remaining-items pass only knows vanilla recipes, so for
         * arcane matches the port re-implements the original's explicit
         * hasContainerItem loop (return to the empty grid slot; a broken
         * tool fires PlayerDestroyItemEvent instead).
         */
        private void restoreContainerItems(final Player takingPlayer,
                                          final NonNullList<ItemStack> gridBefore) {
            for (int i = 0; i < TileArcaneWorkbench.GRID_SIZE; i++) {
                final ItemStack before = gridBefore.get(i);
                if (before.isEmpty() || !before.hasContainerItem()) {
                    continue;
                }
                if (!tile.getItem(i).isEmpty()) {
                    continue; // slot still holds items (multi-craft); no return yet
                }
                final ItemStack container = before.getContainerItem();
                if (container.isEmpty()) {
                    continue;
                }
                if (container.isDamaged() && container.getDamageValue() > container.getMaxDamage()) {
                    net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(takingPlayer, container,
                            net.minecraft.world.InteractionHand.MAIN_HAND);
                    continue;
                }
                tile.setItem(i, container);
            }
        }
    }

    private void payVis(final ArcaneWorkbenchRecipe recipe) {
        final ItemStack wand = tile.getItem(TileArcaneWorkbench.SLOT_WAND);
        if (wand.isEmpty()) {
            return;
        }
        final AspectList cost = effectiveCost(recipe);
        final var pool = WandVis.consumeAllVis(WandVis.poolOf(wand), cost);
        if (pool != null) {
            WandVis.setPool(wand, pool);
        }
        // pool == null: the original still crafted (the simulate check gated
        // display, not payment); keep that behaviour.
    }

    /** Wand slot: wands only, staffs rejected (original SlotLimitedByWand). */
    private static final class WandSlot extends Slot {
        WandSlot(final Container container, final int slot, final int x, final int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return TileArcaneWorkbench.isWandSlotStack(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }

    // ------------------------------------------------------------ quickMove

    @Override
    public void clicked(final int slotId, final int button, final ClickType clickType,
                       final Player player) {
        // Original ContainerArcaneWorkbench.func_75144_a: hotbar-swap always
        // targets hotbar index 1, and the result/wand slots never see a
        // non-zero button (no right-click split / collect off them). The
        // original's canShiftCraft guard (func_94530_a) is unnecessary here:
        // 1.21.1 double-click crafting only scans the player inventory, never
        // the tile's slots.
        if (clickType == ClickType.SWAP) {
            super.clicked(slotId, 1, clickType, player);
            return;
        }
        final int effectiveButton =
                (slotId == SLOT_RESULT || slotId == SLOT_WAND) && button > 0 ? 0 : button;
        super.clicked(slotId, effectiveButton, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(final Player playerIn, final int index) {
        // Original ContainerArcaneWorkbench.func_82846_b, branch for branch:
        // result → whole player inventory (reverse); main-inventory/hotbar
        // wands (not staffs) → wand slot; main inventory otherwise → hotbar;
        // hotbar otherwise → main inventory; grid/wand slot → player
        // inventory. (Note: the original does NOT shift-click into the grid.)
        final Slot clicked = this.slots.get(index);
        if (!clicked.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack clickedStack = clicked.getItem();
        final ItemStack original = clickedStack.copy();

        if (index == SLOT_RESULT) {
            if (!this.moveItemStackTo(clickedStack, SLOT_INV_START, 47, true)) {
                return ItemStack.EMPTY;
            }
            clicked.onQuickCraft(clickedStack, original);
        } else if (index >= SLOT_INV_START && index < 38) {
            if (TileArcaneWorkbench.isWandSlotStack(clickedStack)) {
                if (!this.moveItemStackTo(clickedStack, SLOT_WAND, SLOT_WAND + 1, false)) {
                    return ItemStack.EMPTY;
                }
                clicked.onQuickCraft(clickedStack, original);
            } else if (!this.moveItemStackTo(clickedStack, 38, 47, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= 38 && index < 47) {
            if (TileArcaneWorkbench.isWandSlotStack(clickedStack)) {
                if (!this.moveItemStackTo(clickedStack, SLOT_WAND, SLOT_WAND + 1, false)) {
                    return ItemStack.EMPTY;
                }
                clicked.onQuickCraft(clickedStack, original);
            } else if (!this.moveItemStackTo(clickedStack, SLOT_INV_START, 38, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(clickedStack, SLOT_INV_START, 47, false)) {
            return ItemStack.EMPTY;
        }

        if (clickedStack.isEmpty()) {
            clicked.set(ItemStack.EMPTY);
        } else {
            clicked.setChanged();
        }

        if (clickedStack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        clicked.onTake(playerIn, clickedStack);
        return original;
    }

    // ------------------------------------------------------- tile-backed grid

    /**
     * The 3x3 grid lives in the tile (original: the tile IS the craft
     * matrix), so it persists across GUI close/reopen and chunk reload.
     */
    private final class TileCraftGrid implements CraftingContainer {

        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }

        @Override
        public List<ItemStack> getItems() {
            final List<ItemStack> out = new java.util.ArrayList<>();
            for (int i = 0; i < TileArcaneWorkbench.GRID_SIZE; i++) {
                out.add(tile.getItem(i));
            }
            return out;
        }

        @Override
        public int getContainerSize() {
            return TileArcaneWorkbench.GRID_SIZE;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < TileArcaneWorkbench.GRID_SIZE; i++) {
                if (!tile.getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(final int slot) {
            return tile.getItem(slot);
        }

        @Override
        public void setItem(final int slot, final ItemStack stack) {
            tile.setItem(slot, stack);
        }

        @Override
        public ItemStack removeItem(final int slot, final int count) {
            return tile.removeItem(slot, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(final int slot) {
            return tile.removeItemNoUpdate(slot);
        }

        @Override
        public void setChanged() {
            ArcaneWorkbenchMenu.this.slotsChanged(this);
        }

        @Override
        public boolean stillValid(final Player player) {
            return tile.stillValid(player);
        }

        @Override
        public void fillStackedContents(final net.minecraft.world.entity.player.StackedContents contents) {
            for (int i = 0; i < TileArcaneWorkbench.GRID_SIZE; i++) {
                contents.accountSimpleStack(getItem(i));
            }
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < TileArcaneWorkbench.GRID_SIZE; i++) {
                tile.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
