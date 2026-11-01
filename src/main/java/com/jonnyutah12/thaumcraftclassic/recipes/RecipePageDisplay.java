package com.jonnyutah12.thaumcraftclassic.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What a Thaumonomicon page draws as its recipe body (TC-M3-04-s-iv-b, closes
 * BD-19): the row's grid and its output, read out of the live
 * {@link RecipeManager}.
 *
 * <p>The original's recipe page took the recipe object its page carried and
 * drew its ingredients on a grid with the result beside it
 * ({@code thaumcraft.client.gui.GuiResearchRecipe#drawCraftingPage},
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchRecipe.java:755-880}),
 * so the picture was by construction the row. The port reaches the same
 * guarantee from the other side: the page carries only the row's <b>id</b>
 * (through {@link com.jonnyutah12.thaumcraftclassic.research.book.ResearchRecipeLinks},
 * which mirrors the content-mapping ledger's {@code research=} column) and this
 * class resolves that id in the manager the game is actually crafting with
 * ({@link #resolve}). A row gated out by its {@code neoforge:item_exists}
 * conditions is absent from the manager, so its page draws no body — the
 * display can never advertise a craft that the pack cannot perform.</p>
 *
 * <p>Ingredients render as the first stack of each slot's
 * {@link Ingredient#getItems()}: for the {@code neoforge:components} ingredient
 * the KnowFrag row uses, that stack carries {@code thaumcraft:item_meta: 9} —
 * knowledge fragments, not one of the other seventeen {@code itemresource}
 * metas. The original reached the same precision through the item's
 * {@code getDisplayName(ItemStack)} path (lines 791-793); the port pairs the
 * per-meta sprite with the per-meta name in the tooltip the screen draws.</p>
 *
 * @param id      the row's id, as resolved against the live manager
 * @param station the grid's shape, which picks the label the original printed
 * @param width   drawn grid columns, never above {@link RecipePageLayout#MAX_GRID}
 * @param height  drawn grid rows, never above {@link RecipePageLayout#MAX_GRID}
 * @param grid    the drawn cells, row-major, {@code width * height} of them
 * @param result  the row's output, the item the original drew beside the grid
 */
public record RecipePageDisplay(ResourceLocation id, Station station, int width, int height,
                                List<ItemStack> grid, ItemStack result) {

    private static final Logger LOGGER = LoggerFactory.getLogger("thaumcraft.RecipePageDisplay");

    /**
     * The label the original printed over the grid:
     * {@code recipe.type.workbench} for a shaped row and
     * {@code recipe.type.workbenchshapeless} for a shapeless one
     * ({@code GuiResearchRecipe.java:838-840} and {@code 887-889}); the shipped
     * lang values are the original's own strings
     * ({@code reference/assets/thaumcraft/lang/en_US.lang:657,659}).
     */
    public enum Station {
        WORKBENCH("recipe.type.workbench"),
        WORKBENCH_SHAPELESS("recipe.type.workbenchshapeless");

        private final String labelKey;

        Station(final String labelKey) {
            this.labelKey = labelKey;
        }

        public String labelKey() {
            return labelKey;
        }
    }

    public RecipePageDisplay {
        grid = List.copyOf(grid);
    }

    /**
     * Resolve one row by id from the live manager. Empty when the row is absent
     * (its {@code neoforge:item_exists} conditions are not met, so the pack
     * dropped it) or is not a crafting-table row: a page never draws a picture
     * of a craft it cannot perform.
     */
    public static Optional<RecipePageDisplay> resolve(final ResourceLocation id,
                                                       final RecipeManager recipes,
                                                       final HolderLookup.Provider registries) {
        final Optional<RecipeHolder<?>> held = recipes.byKey(id);
        if (held.isEmpty()) {
            return Optional.empty();
        }
        if (!(held.get().value() instanceof CraftingRecipe crafting)) {
            LOGGER.warn("Thaumonomicon recipe page: {} is not a crafting-table row, so its page "
                    + "draws no body.", id);
            return Optional.empty();
        }
        return Optional.of(of(id, crafting, registries));
    }

    /**
     * Read a row's picture: a shaped row draws its own pattern box, a shapeless
     * row is walked across a 3-wide box the way the original did
     * ({@code i % 3}, {@code i / 3}, lines 895-915 — {@link #shapelessShape}).
     */
    public static RecipePageDisplay of(final ResourceLocation id,
                                       final CraftingRecipe recipe,
                                       final HolderLookup.Provider registries) {
        final List<Ingredient> ingredients = recipe.getIngredients();
        final RecipePageLayout.Shape shape;
        final Station station;
        if (recipe instanceof ShapedRecipe shaped) {
            shape = new RecipePageLayout.Shape(
                    Math.min(RecipePageLayout.MAX_GRID, shaped.getWidth()),
                    Math.min(RecipePageLayout.MAX_GRID, shaped.getHeight()));
            station = Station.WORKBENCH;
        } else {
            shape = shapelessShape(ingredients.size());
            station = Station.WORKBENCH_SHAPELESS;
        }
        final List<ItemStack> grid = new ArrayList<>(shape.width() * shape.height());
        for (int cell = 0; cell < shape.width() * shape.height(); cell++) {
            grid.add(cell < ingredients.size() ? display(ingredients.get(cell)) : ItemStack.EMPTY);
        }
        return new RecipePageDisplay(id, station, shape.width(), shape.height(), grid,
                recipe.getResultItem(registries).copy());
    }

    /**
     * The box a shapeless row draws in: 3 columns, {@code ceil(n / 3)} rows,
     * capped at the original's 3x3 page grid. The original iterated
     * {@code for (int i = 0; i < ingredients.size(); i++)} and placed cell
     * {@code i} at {@code (i % 3, i / 3)} with an {@code i < 3} cap on the row
     * axis (lines 892-915), so 4 ingredients draw a second row and a 10th is
     * never drawn.
     */
    public static RecipePageLayout.Shape shapelessShape(final int ingredientCount) {
        final int columns = Math.min(RecipePageLayout.MAX_GRID, ingredientCount);
        final int rows = Math.min(RecipePageLayout.MAX_GRID,
                (ingredientCount + RecipePageLayout.MAX_GRID - 1) / RecipePageLayout.MAX_GRID);
        return new RecipePageLayout.Shape(columns, rows);
    }

    /** The stack a slot shows: the first of its accepted stacks, empty for a gap. */
    private static ItemStack display(final Ingredient ingredient) {
        if (ingredient.isEmpty() || ingredient.hasNoItems()) {
            return ItemStack.EMPTY;
        }
        final ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0].copy();
    }
}
