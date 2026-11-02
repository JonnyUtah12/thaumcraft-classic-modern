package com.jonnyutah12.thaumcraftclassic.client.jei;

import com.jonnyutah12.thaumcraftclassic.recipes.ArcaneWorkbenchRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.CrucibleRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.InfusionCraftingRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.InfusionEnchantmentRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogInfusion;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCrucible;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Where the plugin gets its rows (SPINE-4, t_aebd14cf).
 *
 * <p>The live source reads the client-side {@link RecipeManager}, which is what
 * a JEI page is drawn from; the seam exists so the plugin's registration logic
 * is unit-testable without a running client — {@code JeiPluginRegistrationTest}
 * injects rows (including a row that resolves to no item) and asserts what the
 * plugin hands to JEI.</p>
 *
 * <p>Rows arrive as {@link RecipeHolder}s, i.e. exactly the objects
 * {@code RecipeManager.getAllRecipesFor} returns, so nothing here re-filters
 * them: a row the RecipeManager accepted is a row JEI shows, and the
 * no-item-tolerance contract lives in {@code RecipeDisplay}, not here.</p>
 */
public interface JeiRecipeSource {

    List<RecipeHolder<ArcaneWorkbenchRecipe>> arcaneWorkbench();

    List<RecipeHolder<CrucibleRecipe>> crucible();

    List<RecipeHolder<InfusionCraftingRecipe>> infusion();

    List<RecipeHolder<InfusionEnchantmentRecipe>> infusionEnchantment();

    /** The rows the running client has loaded, read lazily per JEI registration. */
    static JeiRecipeSource live() {
        return new Client();
    }

    /** Everything empty — the shape a unit test starts from before adding rows. */
    static JeiRecipeSource empty() {
        return new JeiRecipeSource() {
            @Override
            public List<RecipeHolder<ArcaneWorkbenchRecipe>> arcaneWorkbench() {
                return List.of();
            }

            @Override
            public List<RecipeHolder<CrucibleRecipe>> crucible() {
                return List.of();
            }

            @Override
            public List<RecipeHolder<InfusionCraftingRecipe>> infusion() {
                return List.of();
            }

            @Override
            public List<RecipeHolder<InfusionEnchantmentRecipe>> infusionEnchantment() {
                return List.of();
            }
        };
    }

    @Nullable
    private static RecipeManager manager() {
        // Minecraft.getInstance() is null on a build with no client (unit tests, game-test server),
        // and `level` is a field in 1.21.1, not an accessor.
        final Minecraft minecraft = Minecraft.getInstance();
        final Level level = minecraft == null ? null : minecraft.level;
        return level == null ? null : level.getRecipeManager();
    }

    /** The production source. */
    final class Client implements JeiRecipeSource {

        @Override
        public List<RecipeHolder<ArcaneWorkbenchRecipe>> arcaneWorkbench() {
            final RecipeManager manager = manager();
            // The exact call the port's own machines make (TileArcaneWorkbench.java:117,
            // TileCrucible.java:254, TileInfusionMatrix.java:213,233): rows arrive as the
            // RecipeManager accepted them, so a row is shown exactly where the machine crafts it.
            return manager == null
                    ? List.of()
                    : manager.getAllRecipesFor(CatalogArcaneWorkbench.ARCANE_WORKBENCH_TYPE.get());
        }

        @Override
        public List<RecipeHolder<CrucibleRecipe>> crucible() {
            final RecipeManager manager = manager();
            return manager == null
                    ? List.of()
                    : manager.getAllRecipesFor(CatalogCrucible.CRUCIBLE_TYPE.get());
        }

        @Override
        public List<RecipeHolder<InfusionCraftingRecipe>> infusion() {
            final RecipeManager manager = manager();
            return manager == null
                    ? List.of()
                    : manager.getAllRecipesFor(CatalogInfusion.INFUSION_TYPE.get());
        }

        @Override
        public List<RecipeHolder<InfusionEnchantmentRecipe>> infusionEnchantment() {
            final RecipeManager manager = manager();
            return manager == null
                    ? List.of()
                    : manager.getAllRecipesFor(CatalogInfusion.INFUSION_ENCHANTMENT_TYPE.get());
        }
    }
}
