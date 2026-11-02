package com.jonnyutah12.thaumcraftclassic.client.jei;

import com.jonnyutah12.thaumcraftclassic.recipes.InfusionCraftingRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipeDisplay;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipeViewerLayout;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogInfusion;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The {@code thaumcraft:infusion} category (SPINE-4): the central pedestal item
 * on the left, the component pedestals as a grid, the result under the central
 * slot, and the vis + essentia cost in the tooltip.
 *
 * <p>The result slot is empty for the rows whose output is the original's
 * NBT-pair form (golem placer, masks): {@code TileInfusionMatrix.java:510-519}
 * applies that pair to the infused item, so the craft's result IS its input.
 * Those rows are drawn, not dropped — the SPINE-4 tolerance rule.</p>
 */
public final class InfusionCategory extends ThaumcraftRecipeCategory<RecipeHolder<InfusionCraftingRecipe>> {

    public static final RecipeType<RecipeHolder<InfusionCraftingRecipe>> TYPE =
            RecipeType.createFromVanilla(CatalogInfusion.INFUSION_TYPE.get());

    public InfusionCategory(final IGuiHelper gui) {
        super(gui, TYPE, "recipe.type.infusion", CatalogDevices.ITEM_STONE_DEVICE.get(),
                RecipeViewerLayout.INFUSION_WIDTH, RecipeViewerLayout.INFUSION_HEIGHT);
    }

    @Override
    protected RecipeDisplay display(final RecipeHolder<InfusionCraftingRecipe> recipe) {
        return RecipeDisplay.of(recipe.value());
    }

    @Override
    public void setRecipe(final IRecipeLayoutBuilder builder,
                          final RecipeHolder<InfusionCraftingRecipe> recipe,
                          final IFocusGroup focuses) {
        final RecipeDisplay display = display(recipe);
        // inputs = central, then components (RecipeDisplay.of ordering)
        for (int i = 0; i < display.inputs().size(); i++) {
            if (i == 0) {
                addEntry(builder, RecipeIngredientRole.INPUT, 12, 30, display.inputs().get(0));
                continue;
            }
            final int index = i - 1;
            final int col = index % RecipeViewerLayout.INFUSION_COMPONENT_COLS;
            final int row = index / RecipeViewerLayout.INFUSION_COMPONENT_COLS;
            addEntry(builder, RecipeIngredientRole.INPUT,
                    36 + col * RecipeViewerLayout.SLOT_PITCH,
                    8 + row * RecipeViewerLayout.SLOT_PITCH,
                    display.inputs().get(i));
        }
        addEntry(builder, RecipeIngredientRole.OUTPUT, 12, 66, display.output());
    }

    @Override
    public void draw(final RecipeHolder<InfusionCraftingRecipe> recipe,
                     final IRecipeSlotsView slotsView, final GuiGraphics graphics,
                     final double mouseX, final double mouseY) {
        addArrow(graphics, 34, 68);
    }
}
