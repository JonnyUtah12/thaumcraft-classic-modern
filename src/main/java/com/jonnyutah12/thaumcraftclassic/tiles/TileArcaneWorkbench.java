package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.recipes.ArcaneWorkbenchRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.wands.VisCostRules;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
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
 * <p>Out of this tile by design: {@code gui_arcane_workbench} (TC-M7-04) — the
 * original drove the cycle from that GUI, here the cycle runs on the block's
 * use interaction ({@link BlockArcaneWorkbench#useWithoutItem}); the vanilla
 * {@code CraftingManager} step of {@code updateContainer} (the workbench core
 * is the arcane path, TC-M4-03-s); and the wand's real cap + gear discounts
 * (TC-M6-01 / TC-M8-02) — the cost goes through TC-M4-02's chain
 * ({@link VisCostRules#effectiveCosts} → {@link WandVis#consumeAllVisCrafting})
 * with the neutral cap {@link #NEUTRAL_CAP} until that card supplies one.</p>
 */
public class TileArcaneWorkbench extends BlockEntity implements WorldlyContainer {

    /** Slots 0..8 the 3x3 grid, 9 the result, 10 the wand (original stackList). */
    private final ItemStack[] stacks = new ItemStack[11];

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

    /** {@code ThaumcraftCraftingManager.findMatchingArcaneRecipe}: walk in load order. */
    @Nullable
    private ArcaneWorkbenchRecipe findMatch(@Nullable final Player player) {
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
