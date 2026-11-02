package com.jonnyutah12.thaumcraftclassic.client.jei;

import com.jonnyutah12.thaumcraftclassic.recipes.ArcaneWorkbenchRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipeDisplay;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipeViewerLayout;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The {@code thaumcraft:arcane_workbench} category (SPINE-4): the 3x3 workbench
 * grid, the row's essentia cost, and the output — the original's
 * {@code ShapedArcaneRecipe} / {@code ShapelessArcaneRecipe} pair, which the
 * port merged into one type (ArcaneWorkbenchRecipe.java:19-24).
 */
public final class ArcaneWorkbenchCategory
        extends ThaumcraftRecipeCategory<RecipeHolder<ArcaneWorkbenchRecipe>> {

    public static final RecipeType<RecipeHolder<ArcaneWorkbenchRecipe>> TYPE =
            RecipeType.createFromVanilla(CatalogArcaneWorkbench.ARCANE_WORKBENCH_TYPE.get());

    public ArcaneWorkbenchCategory(final IGuiHelper gui) {
        super(gui, TYPE, "recipe.type.arcane", CatalogArcaneWorkbench.ARCANE_WORKBENCH_ITEM.get(),
                RecipeViewerLayout.ARCANE_WIDTH, RecipeViewerLayout.ARCANE_HEIGHT);
    }

    @Override
    protected RecipeDisplay display(final RecipeHolder<ArcaneWorkbenchRecipe> recipe) {
        return RecipeDisplay.of(recipe.value());
    }

    @Override
    public void setRecipe(final IRecipeLayoutBuilder builder,
                          final RecipeHolder<ArcaneWorkbenchRecipe> recipe,
                          final IFocusGroup focuses) {
        final RecipeDisplay display = display(recipe);
        for (int i = 0; i < display.inputs().size(); i++) {
            final int col = i % RecipeViewerLayout.ARCANE_COLS;
            final int row = i / RecipeViewerLayout.ARCANE_COLS;
            addEntry(builder, RecipeIngredientRole.INPUT,
                    12 + col * RecipeViewerLayout.SLOT_PITCH,
                    8 + row * RecipeViewerLayout.SLOT_PITCH,
                    display.inputs().get(i));
        }
        addEntry(builder, RecipeIngredientRole.OUTPUT, 96, 26, display.output());
    }

    @Override
    public void draw(final RecipeHolder<ArcaneWorkbenchRecipe> recipe,
                     final IRecipeSlotsView slotsView, final GuiGraphics graphics,
                     final double mouseX, final double mouseY) {
        addArrow(graphics, 68, 30);
    }
}
