package com.jonnyutah12.thaumcraftclassic.client.jei;

import com.jonnyutah12.thaumcraftclassic.recipes.InfusionEnchantmentRecipe;
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
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The {@code thaumcraft:infusion_enchantment} category (SPINE-4): the component
 * pedestals and the essentia cost, with the target enchantment named in the
 * tooltip.
 *
 * <p>There is deliberately NO output slot: the original's
 * {@code InfusionEnchantmentRecipe.matches} crafts in place — it takes the
 * components, spends the vis, and adds one level of the enchantment to the
 * central item (InfusionEnchantmentRecipe.java:117-152). The tooltip line says
 * which enchantment, resolved through the live registry: the 24 shipped rows
 * name 7 enchantment ids (3 thaumcraft, 4 vanilla), so a registered target
 * shows its real label and an unregistered one shows the id plus "not
 * registered by this build" — both without throwing (SPINE-4 tolerance).</p>
 */
public final class InfusionEnchantmentCategory
        extends ThaumcraftRecipeCategory<RecipeHolder<InfusionEnchantmentRecipe>> {

    public static final RecipeType<RecipeHolder<InfusionEnchantmentRecipe>> TYPE =
            RecipeType.createFromVanilla(CatalogInfusion.INFUSION_ENCHANTMENT_TYPE.get());

    public InfusionEnchantmentCategory(final IGuiHelper gui) {
        super(gui, TYPE, "recipe.type.infusionenchantment", CatalogDevices.ITEM_STONE_DEVICE.get(),
                RecipeViewerLayout.ENCHANTMENT_WIDTH, RecipeViewerLayout.ENCHANTMENT_HEIGHT);
    }

    @Override
    protected RecipeDisplay display(final RecipeHolder<InfusionEnchantmentRecipe> recipe) {
        return RecipeDisplay.of(recipe.value(), clientRegistries());
    }

    @Override
    public void setRecipe(final IRecipeLayoutBuilder builder,
                          final RecipeHolder<InfusionEnchantmentRecipe> recipe,
                          final IFocusGroup focuses) {
        final RecipeDisplay display = display(recipe);
        for (int i = 0; i < display.inputs().size(); i++) {
            final int col = i % RecipeViewerLayout.ENCHANTMENT_COMPONENT_COLS;
            final int row = i / RecipeViewerLayout.ENCHANTMENT_COMPONENT_COLS;
            addEntry(builder, RecipeIngredientRole.INPUT,
                    12 + col * RecipeViewerLayout.SLOT_PITCH,
                    8 + row * RecipeViewerLayout.SLOT_PITCH,
                    display.inputs().get(i));
        }
    }
}
