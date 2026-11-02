package com.jonnyutah12.thaumcraftclassic.client.jei;

import com.jonnyutah12.thaumcraftclassic.recipes.CrucibleRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipeDisplay;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipeViewerLayout;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCrucible;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The {@code thaumcraft:crucible} category (SPINE-4): the thrown catalyst, the
 * vis/essentia cost, the output. The catalyst block is the crucible itself —
 * the metal device's meta 0, whose tile {@code TileCrucible} runs the throw
 * check (CatalogCrucible.java:109 registers it).
 */
public final class CrucibleCategory extends ThaumcraftRecipeCategory<RecipeHolder<CrucibleRecipe>> {

    public static final RecipeType<RecipeHolder<CrucibleRecipe>> TYPE =
            RecipeType.createFromVanilla(CatalogCrucible.CRUCIBLE_TYPE.get());

    public CrucibleCategory(final IGuiHelper gui) {
        super(gui, TYPE, "recipe.type.crucible", CatalogDevices.ITEM_METAL_DEVICE.get(),
                RecipeViewerLayout.CRUCIBLE_WIDTH, RecipeViewerLayout.CRUCIBLE_HEIGHT);
    }

    @Override
    protected RecipeDisplay display(final RecipeHolder<CrucibleRecipe> recipe) {
        return RecipeDisplay.of(recipe.value());
    }

    @Override
    public void setRecipe(final IRecipeLayoutBuilder builder,
                          final RecipeHolder<CrucibleRecipe> recipe, final IFocusGroup focuses) {
        final RecipeDisplay display = display(recipe);
        if (!display.inputs().isEmpty()) {
            addEntry(builder, RecipeIngredientRole.INPUT, 12, 18, display.inputs().get(0));
        }
        addEntry(builder, RecipeIngredientRole.OUTPUT, 96, 18, display.output());
    }

    @Override
    public void draw(final RecipeHolder<CrucibleRecipe> recipe, final IRecipeSlotsView slotsView,
                     final GuiGraphics graphics, final double mouseX, final double mouseY) {
        addArrow(graphics, 68, 22);
    }
}
