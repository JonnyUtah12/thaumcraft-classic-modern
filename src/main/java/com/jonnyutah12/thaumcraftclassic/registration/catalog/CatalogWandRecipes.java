package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.recipes.WandComponentIngredient;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * TC-M6-01-s-b catalog holder: the wand/sceptre assembly recipes' own seam.
 *
 * <p>Owns exactly one registry entry: the {@code thaumcraft:wand_component}
 * ingredient type, the JSON form of the original's
 * {@code ArcaneWandRecipe.checkItemEquals} (item + original damage value). The
 * 107 wand + 108 sceptre rows under {@code data/thaumcraft/recipe/}
 * ({@code wand.<cap>_<rod>.arcane_shaped.json} /
 * {@code sceptre.<cap>_<rod>.arcane_shaped.json}) name it in their
 * {@code key} map; the rows themselves ride the TC-M4-03-s
 * {@code thaumcraft:arcane_workbench} type + serializer pair, so this holder
 * registers no recipe type of its own.</p>
 *
 * <p>The 107/108 split is the original's: 6 caps x 18 rods, staff cores
 * included ({@code WandRod.rods.values()} holds them), minus the wood rod +
 * iron cap pair {@code ArcaneWandRecipe.getCraftingResult} refuses
 * ({@code !br.equals("wood") || !bc.equals("iron")}) — which the sceptre
 * recipe does not refuse.</p>
 *
 * <p>Out of this holder by design: the workbench block/entity/serializer pair
 * ({@code CatalogArcaneWorkbench}, TC-M4-03-s) and the wand items plus the
 * {@code thaumcraft:wand_component} <em>content</em> registry
 * ({@code CatalogWands}, TC-M6-01-s) — note the two ids share the name
 * {@code wand_component} in different registries (ingredient type vs. the
 * wand-component content registry); the registries are keyed separately.</p>
 */
public final class CatalogWandRecipes {

    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, ThaumcraftClassic.MOD_ID);

    /**
     * {@code {"type": "thaumcraft:wand_component", "item": "<id>", "meta": <original damage>}}.
     * Registered on the mod bus like every other holder in
     * {@code ModRegistration.init}; the ingredient codec is resolved lazily
     * against {@code BuiltInRegistries.ITEM}, so a row naming an unregistered
     * item loads and never matches (docs/porting-decisions.md).
     */
    public static final DeferredHolder<IngredientType<?>, IngredientType<WandComponentIngredient>>
            WAND_COMPONENT_INGREDIENT_TYPE = INGREDIENT_TYPES.register("wand_component",
                    () -> new IngredientType<>(WandComponentIngredient.CODEC));

    public static void init(final IEventBus bus) {
        INGREDIENT_TYPES.register(bus);
    }

    static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    private CatalogWandRecipes() {
    }
}
