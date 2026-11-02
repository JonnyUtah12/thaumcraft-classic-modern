package com.jonnyutah12.thaumcraftclassic.recipes;

import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPINE-4 (t_aebd14cf) tolerance contract for {@link RecipeDisplay} — the
 * shapes, built by hand.
 *
 * <p>The card's rule: a shipped row that resolves to <em>no item</em> must
 * still produce a display, never throw, because the arcane rows are gated on
 * {@code neoforge:item_exists} on items whose owning card has not registered
 * them, a {@code #tag} ingredient over an empty tag survives with zero stacks,
 * the infusion rows carrying the original's {@code Object[]} output have no
 * result item at all, and every {@code infusion_enchantment} row crafts in
 * place. A viewer that throws on such a row loses the whole category page.</p>
 *
 * <p>The rows here are built directly from the public recipe constructors, so
 * they exercise the display layer with no data pack, no registry boot and no
 * browser mod on the classpath — the same conditions as the game-test server.
 * {@code JeiRecipeCategoryGameTest} is the companion that walks the LIVE rows
 * of all four types; this test pins the shapes the live walk cannot force.</p>
 *
 * <p>Aspect costs are left empty on purpose: {@code Aspect.getAspect} resolves
 * through the mod's own aspect registry, which is not populated in a bare unit
 * JVM, so the cost lines are proved on the live rows by the game test instead.</p>
 */
class RecipeDisplayToleranceTest {

    // ------------------------------------------------------- arcane workbench

    @Test
    void shapelessArcaneRowToleratesAnEmptyIngredient() {
        final ArcaneWorkbenchRecipe recipe = new ArcaneWorkbenchRecipe("ALEMBIC", new AspectList(),
                List.of(), Map.of(),
                List.of(Ingredient.of(Items.GOLD_INGOT), Ingredient.EMPTY),
                new ItemStack(Items.GLASS_BOTTLE));

        final RecipeDisplay display = RecipeDisplay.of(recipe);

        assertEquals(2, display.inputs().size());
        assertTrue(display.inputs().get(0).resolvable(), "a real ingredient must resolve");
        assertFalse(display.inputs().get(1).resolvable(),
                "an empty Ingredient (the state a gated/unregistered item leaves behind) must be"
                        + " recorded as unresolvable, not thrown");
        assertTrue(display.inputs().get(1).stacks().isEmpty());
        assertTrue(display.output().resolvable());
        assertEquals(1, display.output().stacks().size());
        assertEquals(Items.GLASS_BOTTLE, display.output().stacks().get(0).getItem());
        assertFalse(display.gridLayout(), "a pattern-less row is drawn as a list, not a grid");
    }

    @Test
    void shapedArcaneRowLaysIntoTheNineGridCells() {
        final ArcaneWorkbenchRecipe recipe = new ArcaneWorkbenchRecipe("ALEMBIC", new AspectList(),
                List.of("AB", "C#"),
                Map.of("A", Ingredient.of(Items.OAK_PLANKS), "B", Ingredient.of(Items.IRON_INGOT),
                        "C", Ingredient.EMPTY),
                List.of(), new ItemStack(Items.CHEST));

        final RecipeDisplay display = RecipeDisplay.of(recipe);

        // The original's ShapedArcaneRecipe walks the 3x3 grid, so the display
        // emits GRID_SIZE cells: 9, in row-major order, blanks included.
        assertEquals(RecipeDisplay.GRID_SIZE, display.inputs().size());
        assertTrue(display.gridLayout());
        assertTrue(display.inputs().get(0).resolvable(), "cell 0 = A");
        assertTrue(display.inputs().get(1).resolvable(), "cell 1 = B");
        assertFalse(display.inputs().get(2).resolvable(), "cell 2 is a blank in the pattern");
        assertEquals("grid.2", display.inputs().get(2).name());
        // Two distinct tolerance causes, both drawn as an empty slot:
        // cell 3 = C, a key entry whose ingredient is EMPTY (a #tag over an
        // empty tag, or a gated item that left the row alive);
        // cell 4 = '#', a symbol with no key entry at all — named with the
        // symbol so a hand-authored row is diagnosable.
        assertFalse(display.inputs().get(3).resolvable(), "cell 3 = C, an empty ingredient");
        assertEquals("grid.3", display.inputs().get(3).name());
        assertFalse(display.inputs().get(4).resolvable(), "the '#' symbol has no key entry");
        assertEquals("grid.4?#", display.inputs().get(4).name());
        for (int cell = 6; cell < 9; cell++) {
            assertFalse(display.inputs().get(cell).resolvable(), "row 3 is absent from the pattern");
            assertTrue(display.inputs().get(cell).stacks().isEmpty());
        }
        // A symbol with no key entry is a hand-authored-row shape the codec
        // validation makes impossible in shipped data, and the display must
        // survive it anyway: the slot is named with the symbol, and unresolved.
        final RecipeDisplay broken = RecipeDisplay.of(new ArcaneWorkbenchRecipe("ALEMBIC",
                new AspectList(), List.of("ZZ"), Map.of(), List.of(), new ItemStack(Items.CHEST)));
        assertEquals(RecipeDisplay.GRID_SIZE, broken.inputs().size());
        assertFalse(broken.inputs().get(0).resolvable());
        assertTrue(broken.inputs().get(0).name().endsWith("?Z"),
                "a symbol with no key entry must be named, not dropped: " + broken.inputs().get(0).name());
    }

    // ---------------------------------------------------------------- crucible

    @Test
    void crucibleRowWithNoOutputAndNoCatalystStillRenders() {
        final CrucibleRecipe recipe = new CrucibleRecipe("recipe.1.crucible", "ALEMBIC",
                Ingredient.EMPTY, new AspectList(), ItemStack.EMPTY);

        final RecipeDisplay display = RecipeDisplay.of(recipe);

        assertEquals(1, display.inputs().size(), "the crucible has one slot: the thrown catalyst");
        assertFalse(display.inputs().get(0).resolvable());
        assertFalse(display.output().resolvable(),
                "an output that resolved to no item must be a tolerated entry, not an exception");
        assertTrue(display.output().stacks().isEmpty());
    }

    @Test
    void crucibleRowResolvesItsOutput() {
        final RecipeDisplay display = RecipeDisplay.of(new CrucibleRecipe("recipe.2.crucible",
                "ALEMBIC", Ingredient.of(Items.ROTTEN_FLESH), new AspectList(),
                new ItemStack(Items.GUNPOWDER)));
        assertTrue(display.inputs().get(0).resolvable());
        assertEquals(Items.GUNPOWDER, display.output().stacks().get(0).getItem());
    }

    // ---------------------------------------------------------------- infusion

    @Test
    void infusionRowWithTheOriginalNbtPairOutputHasNoItemAndSaysSo() {
        // The golem-placer / mask rows: the original's Object[] output is an NBT
        // pair, applied by TileInfusionMatrix.java:510-519 as a data component on
        // the infused item, so the craft's result IS its input.
        final InfusionCraftingRecipe recipe = new InfusionCraftingRecipe("recipe.49.infusion",
                "GOLEM", 0, new AspectList(), Ingredient.of(Items.CARVED_PUMPKIN),
                List.of(Ingredient.of(Items.IRON_INGOT), Ingredient.EMPTY),
                ItemStack.EMPTY, Optional.of("thaumcraft:golemplacer"), Optional.empty());

        final RecipeDisplay display = RecipeDisplay.of(recipe);

        assertEquals(3, display.inputs().size(), "central pedestal + components");
        assertEquals("central", display.inputs().get(0).name());
        assertEquals("component.1", display.inputs().get(2).name());
        assertFalse(display.inputs().get(2).resolvable(), "an empty component is tolerated");
        assertFalse(display.output().resolvable(),
                "the NBT-pair rows have no result item by design — the viewer draws an empty"
                        + " output slot, it does not drop the row");
        final String note = display.lines().stream()
                .map(l -> l.getString())
                .filter(l -> l.startsWith("output:"))
                .findFirst()
                .orElse("");
        assertTrue(note.contains("thaumcraft:golemplacer"),
                "the row must name its tag output in text: " + note);
        assertTrue(note.contains("TileInfusionMatrix.java:510-519"),
                "the note must cite where the pair is applied: " + note);
    }

    @Test
    void infusionRowWithAConcreteOutputResolves() {
        final RecipeDisplay display = RecipeDisplay.of(new InfusionCraftingRecipe(
                "recipe.50.infusion", "ALEMBIC", 0, new AspectList(),
                Ingredient.of(Items.GLASS_BOTTLE), List.of(Ingredient.of(Items.GLOWSTONE_DUST)),
                new ItemStack(Items.EXPERIENCE_BOTTLE), Optional.empty(), Optional.empty()));
        assertEquals(2, display.inputs().size());
        assertTrue(display.output().resolvable());
        assertEquals(Items.EXPERIENCE_BOTTLE, display.output().stacks().get(0).getItem());
    }

    // -------------------------------------------------- infusion enchanting

    @Test
    void enchantmentRowNamesItsTargetWithoutAnyRegistryAccess() {
        final ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "perplexity"));
        final InfusionEnchantmentRecipe recipe = new InfusionEnchantmentRecipe("recipe.201.infusion",
                "PERM", key, 0, new AspectList(),
                List.of(Ingredient.of(Items.ROTTEN_FLESH), Ingredient.EMPTY));

        // null registries on purpose: the display must build with no lookup.
        final RecipeDisplay display = RecipeDisplay.of(recipe, null);

        assertEquals(2, display.inputs().size());
        assertFalse(display.inputs().get(1).resolvable());
        assertFalse(display.output().resolvable(),
                "an infusion enchantment row crafts in place, so it has no item output");
        final String note = display.lines().stream()
                .map(l -> l.getString())
                .filter(l -> l.startsWith("enchantment:"))
                .findFirst()
                .orElse("");
        assertTrue(note.contains("thaumcraft:perplexity"), "the target id must be named: " + note);
        assertTrue(note.contains("not registered by this build"),
                "an unresolvable target must say so instead of throwing: " + note);
    }

    // ------------------------------------------------------- research + shape

    @Test
    void researchLineUsesTheGraphsOwnOriginalLabel() {
        // D-016: the label is the shipped graph's verbatim original string, not
        // a derived title — so the row must show exactly what entries.json carries.
        final ResearchEntryData entry = ResearchManager.graph().entries().stream()
                .filter(e -> e.nameEnUs() != null && !e.nameEnUs().isBlank())
                .findFirst()
                .orElseThrow(() -> new AssertionError("the shipped research graph carries no entry name"));

        final RecipeDisplay display = RecipeDisplay.of(new ArcaneWorkbenchRecipe(entry.key(),
                new AspectList(), List.of(), Map.of(), List.of(Ingredient.of(Items.GOLD_INGOT)),
                new ItemStack(Items.GLASS_BOTTLE)));
        final String line = display.lines().stream()
                .map(l -> l.getString())
                .filter(l -> l.startsWith("research:"))
                .findFirst()
                .orElse("");
        assertEquals("research: " + entry.nameEnUs(), line,
                "the research line for " + entry.key() + " must be the graph's own nameEnUs");
    }

    @Test
    void unknownResearchKeyFallsBackToTheId() {
        // A '@' note-key, or a row whose research belongs to a card not in the
        // graph yet: the gate matches on the id, so the display shows the id.
        final RecipeDisplay display = RecipeDisplay.of(new ArcaneWorkbenchRecipe("@unshippednote",
                new AspectList(), List.of(), Map.of(), List.of(Ingredient.of(Items.GOLD_INGOT)),
                new ItemStack(Items.GLASS_BOTTLE)));
        assertTrue(display.lines().stream().map(l -> l.getString())
                        .anyMatch(l -> l.equals("research: @unshippednote")),
                "display lines: " + display.lines());
    }

    @Test
    void layoutCapacitiesMatchTheGeometryTheCategoriesSizeFrom() {
        // The game test measures live rows against these; the unit leg pins the
        // constants the JEI panels are built from, so a capacity edit is deliberate.
        assertEquals(18, RecipeViewerLayout.SLOT_PITCH, "the vanilla container pitch");
        assertEquals(9, RecipeViewerLayout.ARCANE_MAX_INPUTS, "the original's 3x3 workbench grid");
        assertEquals(RecipeDisplay.GRID_SIZE, RecipeViewerLayout.ARCANE_MAX_INPUTS);
        assertEquals(1 + 15, RecipeViewerLayout.INFUSION_MAX_INPUTS, "central pedestal + 5x3 grid");
        assertEquals(15, RecipeViewerLayout.ENCHANTMENT_MAX_INPUTS, "component pedestals only");
        assertEquals(1, RecipeViewerLayout.CRUCIBLE_MAX_INPUTS);
        for (final int[] pair : List.of(new int[] {RecipeViewerLayout.ARCANE_WIDTH,
                        RecipeViewerLayout.ARCANE_HEIGHT, RecipeViewerLayout.ARCANE_MAX_INPUTS},
                new int[] {RecipeViewerLayout.CRUCIBLE_WIDTH, RecipeViewerLayout.CRUCIBLE_HEIGHT,
                        RecipeViewerLayout.CRUCIBLE_MAX_INPUTS},
                new int[] {RecipeViewerLayout.INFUSION_WIDTH, RecipeViewerLayout.INFUSION_HEIGHT,
                        RecipeViewerLayout.INFUSION_MAX_INPUTS},
                new int[] {RecipeViewerLayout.ENCHANTMENT_WIDTH, RecipeViewerLayout.ENCHANTMENT_HEIGHT,
                        RecipeViewerLayout.ENCHANTMENT_MAX_INPUTS})) {
            final int cols = (pair[2] + 2) / 3;
            assertTrue(pair[0] >= cols * RecipeViewerLayout.SLOT_PITCH + 12,
                    "a panel " + pair[0] + " wide cannot lay out " + pair[2] + " slots");
        }
    }
}
