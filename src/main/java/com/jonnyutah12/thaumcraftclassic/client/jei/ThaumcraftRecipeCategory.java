package com.jonnyutah12.thaumcraftclassic.client.jei;

import com.jonnyutah12.thaumcraftclassic.recipes.RecipeDisplay;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipeViewerLayout;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Shared drawing for the four Thaumcraft recipe categories (SPINE-4).
 *
 * <p>Everything interpretive is already decided by {@code RecipeDisplay}; this
 * class only maps entries to slot coordinates and appends the text lines. That
 * split is what makes the JEI lane safe: the plugin package is the ONLY place
 * in the mod that may reference a {@code mezz.jei} type
 * (JeiPluginIsolationTest, bytecode scan), JEI loads it, and nothing in the
 * core mod — nor the game-test server, which runs with no JEI jar at all —
 * ever touches it.</p>
 */
abstract class ThaumcraftRecipeCategory<T> implements IRecipeCategory<T> {

    private final IGuiHelper gui;
    private final RecipeType<T> recipeType;
    private final String titleKey;
    private final ItemLike catalyst;
    private final int width;
    private final int height;
    @Nullable
    private IDrawable icon;

    protected ThaumcraftRecipeCategory(final IGuiHelper gui, final RecipeType<T> recipeType,
                                       final String titleKey, @Nullable final ItemLike catalyst,
                                       final int width, final int height) {
        this.gui = gui;
        this.recipeType = recipeType;
        this.titleKey = titleKey;
        this.catalyst = catalyst;
        this.width = width;
        this.height = height;
    }

    /** The rows to draw, in panel order. */
    protected abstract RecipeDisplay display(T recipe);

    @Override
    public final RecipeType<T> getRecipeType() {
        return this.recipeType;
    }

    /**
     * The category label is the ORIGINAL string, verbatim: the 1.7.10 book's
     * recipe pages head these same four recipe kinds with
     * {@code recipe.type.arcane} / {@code .crucible} / {@code .infusion} /
     * {@code .infusionenchantment} (decompiled {@code GuiResearchRecipe.java}:644,
     * :969, :1162, :1355; values en_US.lang:660-663), so the port ships those
     * keys and this category reuses them rather than inventing a label.
     */
    @Override
    public final Component getTitle() {
        return Component.translatable(this.titleKey);
    }

    @Override
    public final IDrawable getIcon() {
        if (this.icon == null) {
            this.icon = this.catalyst == null
                    ? this.gui.createBlankDrawable(16, 16)
                    : this.gui.createDrawableItemLike(this.catalyst);
        }
        return this.icon;
    }

    @Override
    public final int getWidth() {
        return this.width;
    }

    @Override
    public final int getHeight() {
        return this.height;
    }

    /** So JEI's recipe-id search finds a row by its data file name. */
    @Override
    @Nullable
    public ResourceLocation getRegistryName(final T recipe) {
        return recipe instanceof RecipeHolder<?> holder ? holder.id() : IRecipeCategory.super.getRegistryName(recipe);
    }

    /**
     * Slot placement for one display entry. An unresolvable entry — the state
     * the SPINE-4 contract is about — draws an empty slot and adds no
     * ingredient: JEI is never handed an empty stack, so the row renders
     * instead of throwing.
     */
    protected final IRecipeSlotBuilder addEntry(final IRecipeLayoutBuilder builder,
                                                final RecipeIngredientRole role, final int x,
                                                final int y, final RecipeDisplay.Entry entry) {
        final IRecipeSlotBuilder slot = builder.addSlot(role, x, y);
        slot.setSlotName(entry.name());
        if (role == RecipeIngredientRole.OUTPUT) {
            slot.setOutputSlotBackground();
        } else {
            slot.setStandardSlotBackground();
        }
        if (entry.resolvable()) {
            slot.addItemStacks(entry.stacks());
        }
        return slot;
    }

    protected final void addArrow(final GuiGraphics graphics, final int x, final int y) {
        this.gui.getRecipeArrow().draw(graphics, x, y);
    }

    /** The row's text lines (research gate, tag output, enchantment target). */
    @Override
    public void getTooltip(final ITooltipBuilder tooltip, final T recipe,
                           final IRecipeSlotsView slotsView, final double mouseX, final double mouseY) {
        final RecipeDisplay display = display(recipe);
        for (final Component line : display.lines()) {
            tooltip.add(line);
        }
        for (final RecipeDisplay.AspectCost cost : display.aspects()) {
            tooltip.add(Component.literal(cost.label() + " \u00d7" + cost.amount()));
        }
    }

    /** The client's registry access, or null on a build with no client (unit test). */
    @Nullable
    protected static HolderLookup.Provider clientRegistries() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return null;
        }
        final Level level = minecraft.level;
        return level == null ? null : level.registryAccess();
    }

    /** The panel geometry, exposed to subclasses for slot arithmetic. */
    protected static int pitch() {
        return RecipeViewerLayout.SLOT_PITCH;
    }
}
