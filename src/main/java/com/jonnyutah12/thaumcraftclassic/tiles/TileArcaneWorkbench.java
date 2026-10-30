package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.recipes.ArcaneWorkbenchRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.wands.VisCostRules;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Arcane workbench (content-mapping.csv {@code thaumcraft:tilearcaneworkbench}).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcaneWorkbench} (an
 * {@code TileMagicWorkbench} subclass: 11 inventory slots — the 3x3 crafting
 * grid in 0..8, the result in 9, the wand in 10 — persisted as an
 * {@code Inventory}/{@code Slot} NBT list) plus
 * {@code ContainerArcaneWorkbench.updateContainer}, which looks the grid up
 * against the registered {@code IArcaneRecipe}s and pays the match's aspects
 * out of the wand's vis pool before the result appears. The
 * {@code ThaumcraftCraftingManager.findMatchingArcaneRecipe}/{@code
 * findMatchingArcaneRecipeAspects} pair is one lookup here
 * ({@link #findMatch}); the original called it twice per cycle, once for the
 * item and once for the cost list, with identical inputs.</p>
 *
 * <p>Out of this tile by design: the wand's real cap + gear discounts
 * (TC-M6-01 / TC-M8-02) — the cost goes through TC-M4-02's chain
 * ({@link VisCostRules#effectiveCosts} → {@link WandVis#consumeAllVisCrafting})
 * with the neutral cap {@link #NEUTRAL_CAP} until that card supplies one.</p>
 *
 * <p>TC-M7-04-m2 wired the GUI: the tile is a {@link net.minecraft.world.MenuProvider}
 * and {@link #updateCrafting} is the container-tick step the original's
 * {@code ContainerArcaneWorkbench.updateContainer} ran, so the cycle now runs from
 * the open menu ({@code menus/ArcaneWorkbenchMenu}) as it did in 1.7.10, not from
 * the block's use interaction. The vanilla {@code CraftingManager} step of
 * {@code updateContainer} (the workbench core is the arcane path, TC-M4-03-s) stays
 * out of scope.</p>
 */
public class TileArcaneWorkbench extends BlockEntity
        implements WorldlyContainer, net.minecraft.world.MenuProvider {

    /** Slots 0..8 the 3x3 grid, 9 the result, 10 the wand (original stackList). */
    private final ItemStack[] stacks = new ItemStack[11];

    /** Tile slot the result goes in — the original's slot 9 ({@code SlotCraftingArcaneWorkbench}). */
    public static final int SLOT_RESULT = 9;

    /** Tile slot the wand goes in — the original's slot 10 ({@code SlotLimitedByWand}). */
    public static final int SLOT_WAND = 10;

    /** The 3x3 grid is tile slots 0..8, the original's {@code craftMatrix}. */
    public static final int GRID_SIZE = 9;

    /**
     * The original reads the cap off {@code ItemWandCasting.getCap()}, which is
     * TC-M6-01's; until that card lands, the neutral 1.0 cap keeps the cost at
     * the recipe's own aspect prices, and the {@code crafting=true} flag keeps
     * the original's focus-term skip ({@code ItemWandCasting.java:266-268}).
     */
    public static final VisCostRules.WandCapCost NEUTRAL_CAP =
            new VisCostRules.WandCapCost(1.0F, null, 1.0F);

    public TileArcaneWorkbench(final BlockPos pos, final BlockState state) {
        super(CatalogArcaneWorkbench.TILE_ARCANE_WORKBENCH.get(), pos, state);
        for (int i = 0; i < this.stacks.length; i++) {
            this.stacks[i] = ItemStack.EMPTY;
        }
    }

    /**
     * The original {@code ContainerArcaneWorkbench.updateContainer} after the
     * vanilla step: first match wins, the wand slot pays the recipe's aspect
     * price all-or-nothing, and only then does slot 9 fill.
     */
    public boolean craft(@Nullable final Player player) {
        final ArcaneWorkbenchRecipe match = findMatch(player);
        if (match == null) {
            return false;
        }
        final ItemStack wand = this.stacks[10];
        if (wand.isEmpty()) {
            // Original: the result only appears once the ItemWandCasting in slot
            // 10 has paid; without a wand nothing is crafted.
            return false;
        }
        final VisPool pool = WandVis.poolOf(wand);
        final AspectList cost =
                VisCostRules.effectiveCosts(match.aspects(), NEUTRAL_CAP, null, 0, false, true);
        final VisPool paid = WandVis.consumeAllVisCrafting(pool, cost);
        if (paid == null) {
            // consumeAllVis is all-or-nothing: a short pool spends nothing.
            return false;
        }
        WandVis.setPool(wand, paid);
        this.stacks[9] = match.output().copy();
        setChanged();
        return true;
    }

    /**
     * The container tick of {@code ContainerArcaneWorkbench.updateContainer} — the
     * arcane step only: the original recomputed the cycle every container tick and
     * only ever filled slot 9 when it was empty, so an open menu pays once per
     * result, not once per tick.
     */
    public void updateCrafting(@Nullable final Player player) {
        if (this.stacks[9].isEmpty()) {
            craft(player);
        }
    }

    /**
     * The price the current match charges, in real vis, after
     * {@link VisCostRules#effectiveCosts} (the original's
     * {@code findMatchingArcaneRecipeAspects}, which is the same lookup with the
     * consumption-modifier chain applied). {@code null} when nothing matches.
     *
     * <p>The neutral cap is the tile's own {@link #NEUTRAL_CAP}, so the number the
     * menu syncs is exactly the number {@link #craft} pays — one source of truth.</p>
     */
    @Nullable
    public AspectList currentCost(@Nullable final Player player) {
        final ArcaneWorkbenchRecipe match = findMatch(player);
        return match == null ? null
                : VisCostRules.effectiveCosts(match.aspects(), NEUTRAL_CAP, null, 0, false, true);
    }

    /**
     * Real vis → centi-vis, the original's {@code * 100}
     * ({@code ItemWandCasting.java:314}, the port's
     * {@link com.jonnyutah12.thaumcraftclassic.wands.WandVis#consumeAllVisCrafting}).
     * The menu's data slots travel in centi-vis so the screen can compare them
     * against the wand's charge with no unit conversion of its own.
     */
    public static AspectList centiVisCost(final AspectList realCosts) {
        final AspectList centi = new AspectList();
        for (final Aspect aspect : realCosts.getAspects()) {
            centi.add(aspect, realCosts.getAmount(aspect) * WandVis.CENTI_VIS_PER_VIS);
        }
        return centi;
    }

    /** The wand slot's charge for one aspect, in centi-vis (0 without a wand). */
    public int wandCharge(final Aspect aspect) {
        return WandVis.getVis(WandVis.poolOf(this.stacks[10]), aspect);
    }

    /**
     * Whether the wand slot can pay a centi-vis price — the original's simulate
     * call {@code consumeAllVisCrafting(stack, player, cost, false)} from
     * {@code GuiArcaneWorkbench}, whose {@code doTake = false} means "check only".
     *
     * <p>The quirk is reproduced, not fixed: a match with an empty price list is
     * unpayable ({@code consumeAllVis} returns false for a zero-size list in the
     * original too), so such a recipe shows "Insufficient vis" and never crafts on
     * both sides.</p>
     */
    public boolean canWandPay(@Nullable final AspectList centiCosts) {
        return centiCosts != null
                && VisCostRules.canPay(WandVis.poolOf(this.stacks[10]), centiCosts);
    }

    /**
     * The take-time payment of {@code SlotCraftingArcaneWorkbench.onCraft}: the
     * original charged the vis twice — once in the container tick to reveal the
     * result, and again ({@code consumeAllVisCrafting(..., true)}) when the player
     * lifted it. The second charge is all-or-nothing and its failure does not stop
     * the take, exactly as in the original.
     */
    public boolean payVisOnTake(@Nullable final Player player) {
        final AspectList cost = currentCost(player);
        if (cost == null) {
            return false;
        }
        final VisPool pool = WandVis.poolOf(this.stacks[10]);
        final VisPool paid = WandVis.consumeAllVisCrafting(pool, cost);
        if (paid == null) {
            return false;
        }
        WandVis.setPool(this.stacks[10], paid);
        setChanged();
        return true;
    }

    /**
     * One item out of every grid cell, the original's {@code onCraft} loop
     * ({@code this.craftMatrix.decrStackSize(i, 1)} for slots 0..8). The original's
     * container-item branch (bucket-style returns) is not ported: no shipped arcane
     * row takes a container item, and 1.21.1 returns container items through the
     * recipe's own {@code getRemainingItems}, which this recipe type does not
     * declare (recorded in {@code docs/BEHAVIORAL_DEVIATIONS.md} BD-18).
     */
    public void consumeGrid() {
        for (int i = 0; i < 9; i++) {
            removeItem(i, 1);
        }
    }

    // --- menu seam (content-mapping.csv thaumcraft:gui_arcane_workbench) -----

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component
                .translatable("container.thaumcraft.gui_arcane_workbench");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(final int id,
            final net.minecraft.world.entity.player.Inventory inventory, final Player player) {
        return new com.jonnyutah12.thaumcraftclassic.menus.ArcaneWorkbenchMenu(id, inventory, this);
    }

    /** {@code ThaumcraftCraftingManager.findMatchingArcaneRecipe}: walk in load order. */
    @Nullable
    public ArcaneWorkbenchRecipe findMatch(@Nullable final Player player) {
        final RecipeManager recipes = this.level.getRecipeManager();
        if (recipes == null) {
            return null;
        }
        final List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            grid.add(this.stacks[i]);
        }
        final ArcaneWorkbenchRecipe.WorkbenchInput input =
                new ArcaneWorkbenchRecipe.WorkbenchInput(grid, player);
        for (final RecipeHolder<?> holder : recipes.getAllRecipesFor(
                CatalogArcaneWorkbench.ARCANE_WORKBENCH_TYPE.get())) {
            if (holder.value() instanceof ArcaneWorkbenchRecipe recipe
                    && recipe.matches(input, this.level)) {
                return recipe;
            }
        }
        return null;
    }

    @Override
    public int getContainerSize() {
        return this.stacks.length;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot < 0 || slot >= this.stacks.length ? ItemStack.EMPTY : this.stacks[slot];
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        if (slot < 0 || slot >= this.stacks.length) {
            return ItemStack.EMPTY;
        }
        final ItemStack current = this.stacks[slot];
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = current.copy();
        if (current.getCount() <= count) {
            this.stacks[slot] = ItemStack.EMPTY;
        } else {
            current.setCount(current.getCount() - count);
            removed.setCount(count);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (slot < 0 || slot >= this.stacks.length) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = this.stacks[slot];
        this.stacks[slot] = ItemStack.EMPTY;
        setChanged();
        return removed;
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack stack : this.stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.stacks.length; i++) {
            this.stacks[i] = ItemStack.EMPTY;
        }
        setChanged();
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (slot >= 0 && slot < this.stacks.length) {
            this.stacks[slot] = stack == null ? ItemStack.EMPTY : stack;
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return true;
    }

    // The original ISidedInventory contract: every face exposes only slot 10, and
    // only a wand may be put through it (TileMagicWorkbench.java:142-168).
    @Override
    public int[] getSlotsForFace(final Direction side) {
        return new int[]{10};
    }

    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack,
            @Nullable final Direction side) {
        // The original tested {@code instanceof ItemWandCasting && !isStaff}; the
        // port's seam for "holds vis" is the TC-M4-02 VIS_POOL component, which
        // the wand item (TC-M8-01) carries.
        return slot == 10 && !stack.isEmpty()
                && stack.getOrDefault(ModDataComponents.VIS_POOL.get(), null) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
        return slot == 10;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final ListTag items = new ListTag();
        for (int i = 0; i < this.stacks.length; i++) {
            if (!this.stacks[i].isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", this.stacks[i].save(registries));
                items.add(entry);
            }
        }
        // Original writeCustomNBT key.
        tag.put("Inventory", items);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < this.stacks.length; i++) {
            this.stacks[i] = ItemStack.EMPTY;
        }
        for (final Tag entry : tag.getList("Inventory", Tag.TAG_COMPOUND)) {
            final CompoundTag item = (CompoundTag) entry;
            final int slot = item.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.stacks.length && item.contains("Item")) {
                this.stacks[slot] = ItemStack.parseOptional(registries, item.getCompound("Item"));
            }
        }
    }

    /** Original {@code TileMagicWorkbench.stillValid}: the bench must still be in world. */
    @Override
    public boolean stillValid(final Player player) {
        return this.level.getBlockEntity(this.worldPosition) == this;
    }

    /** Drop the bench's contents when it breaks (the original destroyed its slots). */
    public void spillContents() {
        Containers.dropContents(this.level, this.worldPosition, this);
    }
}
