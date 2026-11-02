package com.jonnyutah12.thaumcraftclassic.recipes;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A browser-independent view of ONE shipped recipe row: exactly what a recipe
 * viewer has to draw, computed by code the port runs by itself (SPINE-4,
 * t_aebd14cf).
 *
 * <p>Why this layer exists: the JEI plugin sits behind a {@code compileOnly}
 * dependency and is loaded only BY JEI, so every byte of the display has to be
 * producible — and testable — on a machine with no browser mod installed and on
 * the game-test server, where no {@code mezz.jei} class exists at all. The
 * plugin is therefore a thin renderer over this record; all row interpretation
 * lives here.</p>
 *
 * <p><b>The contract this class exists to guarantee</b> (SPINE-4 definition of
 * done): a shipped row may resolve to <em>no item at all</em>, and every
 * factory below must still return a display for it, never throw. Two live
 * causes in the shipped data pack (255 rows):</p>
 * <ul>
 *   <li>An {@link Ingredient} can be empty: the extractor gates a row on
 *       {@code neoforge:item_exists}, so an unregistered item drops the whole
 *       row, but a {@code #tag} ingredient over a tag that is empty at runtime
 *       survives with zero stacks — {@code Ingredient.getItems()} is
 *       length 0.</li>
 *   <li>An infusion row can have no item output: the golem-placer / mask rows
 *       carry the original's {@code Object[]} output as an NBT pair, and
 *       {@code TileInfusionMatrix.java:510-519} applies that pair as a
 *       {@code DataComponents.CUSTOM_DATA} component on the infused item, so
 *       the craft's result IS the input item and {@code getResultItem()} is
 *       {@link ItemStack#EMPTY} by design.</li>
 * </ul>
 *
 * <p>Such an entry ships as {@link Entry#resolvable} = {@code false} with an
 * empty stack list; a viewer draws nothing there and the {@link #lines()} carry
 * the id text so the row is still legible. Pinned by
 * {@code RecipeDisplayToleranceTest} (the shapes, built by hand) and
 * {@code JeiRecipeCategoryGameTest} (every live row of all four custom types in
 * the shipped data pack).</p>
 */
public record RecipeDisplay(List<Entry> inputs, Entry output, List<AspectCost> aspects,
                            List<Component> lines, boolean gridLayout) {

    /** One slot of the layout: the stacks a viewer may draw, and whether it may draw any. */
    public record Entry(String name, List<ItemStack> stacks, boolean resolvable) {

        /** An ingredient: an empty ingredient is a TOLERATED state, not an error. */
        public static Entry of(final String name, final Ingredient ingredient) {
            final ItemStack[] stacks = ingredient.getItems();
            return new Entry(name, List.of(stacks), stacks.length > 0);
        }

        /** A concrete output stack: {@link ItemStack#EMPTY} is a tolerated state. */
        public static Entry of(final String name, final ItemStack stack) {
            return stack.isEmpty()
                    ? new Entry(name, List.of(), false)
                    : new Entry(name, List.of(stack.copy()), true);
        }

        /** Nothing to draw — the row names no item for this slot. */
        public static Entry none(final String name) {
            return new Entry(name, List.of(), false);
        }
    }

    /** One essentia/vis cost line: the original's aspect tag plus its amount. */
    public record AspectCost(String tag, String label, int amount) {
    }

    public RecipeDisplay {
        inputs = List.copyOf(inputs);
        output = output == null ? Entry.none("output") : output;
        aspects = List.copyOf(aspects);
        lines = List.copyOf(lines);
    }

    /** The 3x3 grid cell of a pattern row, in the order a viewer draws slots. */
    public static final int GRID_SIZE = 9;

    // ------------------------------------------------------------- factories

    /**
     * Arcane workbench row. The shaped kind lays its pattern into the 3x3 grid
     * (the original's {@code ShapedArcaneRecipe.checkMatch} walks that grid, so
     * a viewer must show it as a grid); the shapeless kind lists its
     * ingredients.
     */
    public static RecipeDisplay of(final ArcaneWorkbenchRecipe recipe) {
        final List<Entry> inputs = new ArrayList<>();
        final boolean grid = !recipe.pattern().isEmpty();
        if (grid) {
            final Map<String, Ingredient> key = recipe.key();
            final List<String> pattern = recipe.pattern();
            for (int y = 0; y < GRID_SIZE / 3; y++) {
                final String row = y < pattern.size() ? pattern.get(y) : "";
                for (int x = 0; x < 3; x++) {
                    final char symbol = x < row.length() ? row.charAt(x) : ' ';
                    if (symbol == ' ') {
                        inputs.add(Entry.none("grid." + (x + y * 3)));
                        continue;
                    }
                    // The codec's own validation makes a symbol with no key entry
                    // impossible (ArcaneWorkbenchRecipe.checkMatch throws on one),
                    // but the display must survive a hand-authored row too.
                    final Ingredient ingredient = key.get(String.valueOf(symbol));
                    inputs.add(ingredient == null
                            ? Entry.none("grid." + (x + y * 3) + "?" + symbol)
                            : Entry.of("grid." + (x + y * 3), ingredient));
                }
            }
        } else {
            for (int i = 0; i < recipe.ingredients().size(); i++) {
                inputs.add(Entry.of("ingredient." + i, recipe.ingredients().get(i)));
            }
        }
        final List<Component> lines = new ArrayList<>();
        addResearchLine(lines, recipe.research());
        return new RecipeDisplay(inputs, Entry.of("output", recipe.output()),
                costs(recipe.aspects()), lines, grid);
    }

    /** Crucible row: the thrown catalyst is the single input slot. */
    public static RecipeDisplay of(final CrucibleRecipe recipe) {
        final List<Component> lines = new ArrayList<>();
        addResearchLine(lines, recipe.research());
        // CrucibleRecipe has no output() accessor; getResultItem ignores its argument
        // (CrucibleRecipe.java:164-167 returns the row's own stack), so null registries are safe here
        // and in a unit test with no registry access.
        return new RecipeDisplay(List.of(Entry.of("catalyst", recipe.catalyst())),
                Entry.of("output", recipe.getResultItem(null)), costs(recipe.aspects()), lines, false);
    }

    /**
     * Infusion altar row: the central pedestal item plus the component
     * pedestals. The output is the concrete stack when the row has one; a row
     * with the original's NBT-pair output has no item to show, so the entry is
     * unresolvable and the line names the pair (see the class contract).
     */
    public static RecipeDisplay of(final InfusionCraftingRecipe recipe) {
        final List<Entry> inputs = new ArrayList<>();
        inputs.add(Entry.of("central", recipe.input()));
        for (int i = 0; i < recipe.components().size(); i++) {
            inputs.add(Entry.of("component." + i, recipe.components().get(i)));
        }
        final Entry output = Entry.of("output", recipe.concreteOutput() == null
                ? ItemStack.EMPTY : recipe.concreteOutput());
        final List<Component> lines = new ArrayList<>();
        addResearchLine(lines, recipe.research());
        if (!output.resolvable()) {
            lines.add(Component.literal("output: " + (recipe.outputTagLabel() == null
                            ? "unspecified" : recipe.outputTagLabel())
                    + " — carried on the infused item (TileInfusionMatrix.java:510-519)")
                    .withStyle(ChatFormatting.GRAY));
        }
        return new RecipeDisplay(inputs, output, costs(recipe.aspects()), lines, true);
    }

    /**
     * Infusion enchantment row: component pedestals only — the craft's result
     * is the central item with one more level of the target enchantment, so
     * there is no item output. {@code registries} may be null (no lookup), and
     * the enchantment may be unregistered (the row ships as data naming an id
     * a later card owns); both cases draw text instead of throwing.
     */
    public static RecipeDisplay of(final InfusionEnchantmentRecipe recipe,
                                   @Nullable final HolderLookup.Provider registries) {
        final List<Entry> inputs = new ArrayList<>();
        for (int i = 0; i < recipe.components().size(); i++) {
            inputs.add(Entry.of("component." + i, recipe.components().get(i)));
        }
        final List<Component> lines = new ArrayList<>();
        addResearchLine(lines, recipe.research());
        final Holder<Enchantment> target =
                registries == null ? null : recipe.enchantment(registries);
        lines.add(Component.literal("enchantment: " + (target == null
                        ? recipe.enchantmentKey().location() + " (not registered by this build)"
                        : target.value().description().getString()))
                .withStyle(ChatFormatting.GRAY));
        return new RecipeDisplay(inputs, Entry.none("output"), costs(recipe.aspects()), lines, true);
    }

    // ---------------------------------------------------------------- helpers

    /** The original's aspect list, in the order the row declares it. */
    private static List<AspectCost> costs(final AspectList aspects) {
        final List<AspectCost> out = new ArrayList<>();
        for (final Aspect aspect : aspects.getAspects()) {
            out.add(new AspectCost(aspect.getTag(), aspect.getName(), aspects.getAmount(aspect)));
        }
        return out;
    }

    /**
     * The research the row is gated on, named by the shipped graph's own
     * original string ({@code tc.research_name.<key>} value, extracted verbatim
     * into {@code research_graph/entries.json}); an id the graph does not know
     * (a {@code @} note-key, or a row from a card whose research is not in the
     * graph yet) falls back to the id, which is what the gate itself matches on.
     */
    private static void addResearchLine(final List<Component> lines, final String research) {
        if (research == null || research.isEmpty()) {
            return;
        }
        final ResearchEntryData entry = ResearchManager.graph().entry(research);
        final String label = entry != null && entry.nameEnUs() != null ? entry.nameEnUs() : research;
        lines.add(Component.literal("research: " + label).withStyle(ChatFormatting.ITALIC));
    }
}
