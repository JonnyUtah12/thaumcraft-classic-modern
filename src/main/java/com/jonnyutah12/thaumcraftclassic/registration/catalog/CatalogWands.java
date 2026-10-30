package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandCap;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandCasting;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandRod;
import com.jonnyutah12.thaumcraftclassic.registration.ModItems;
import com.jonnyutah12.thaumcraftclassic.wands.WandComponent;
import com.jonnyutah12.thaumcraftclassic.wands.WandComponents;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * TC-M6-01 catalog holder: the wand family — the wand item, the cap item, the
 * rod/staff-core item, and the {@code thaumcraft:wand_component} registry that
 * carries the original's rod/cap tables.
 *
 * <p>Owns exactly the content-mapping.csv rows this card flips:
 * {@code thaumcraft:wandcasting} (kind {@code item}), {@code thaumcraft:wandcap}
 * and {@code thaumcraft:wandrod} (kind {@code item}) and the 24
 * {@code wand_component} rows (6 caps + 9 wand rods + 9 staff cores, which
 * share ids per ContentMappingIdContractTest's documented shared-id group and
 * therefore land as the 16 registry entries of {@code WandComponent}).</p>
 *
 * <p>The registry is created the way {@code ModAspects} creates the aspect
 * registry (TC-M4-01): a {@link RegistryBuilder} on {@link NewRegistryEvent}
 * plus a {@link DeferredRegister} for the entries, all attached from the mod
 * bus in the mod constructor, before any {@code RegisterEvent} fires
 * (docs/architecture/registration.md). The registry key is pinned in
 * docs/architecture/registration.md and in
 * {@code RegistrationGameTest.CUSTOM_KINDS}.</p>
 *
 * <p>The cap and rod crafting recipes (iron caps from ingots, cores from
 * materials — {@code ConfigRecipes} {@code WandCap*} / {@code WandRod*} rows)
 * already shipped with the arcane workbench data (TC-M4-03-s,
 * {@code data/thaumcraft/recipe/recipe.139-152.arcane_shaped.json}) and are
 * gated on these item ids through {@code neoforge:item_exists} conditions, so
 * they start resolving as soon as these holders exist. The wand and sceptre
 * <em>assembly</em> recipes are the dynamic {@code IArcaneRecipe}s
 * {@code ArcaneWandRecipe} / {@code ArcaneSceptreRecipe}, which the original
 * registered directly on {@code ThaumcraftApi.getCraftingRecipes()} rather than
 * through {@code ConfigResearch.recipes}; see docs/feature-tracking.md for the
 * state of that slice.</p>
 */
public final class CatalogWands {

    /** The {@code thaumcraft:wand_component} registry key (docs/architecture/registration.md). */
    public static final ResourceKey<Registry<WandComponent>> WAND_COMPONENT_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "wand_component"));

    public static final DeferredRegister<WandComponent> WAND_COMPONENTS =
            DeferredRegister.create(WAND_COMPONENT_REGISTRY_KEY, ThaumcraftClassic.MOD_ID);

    private static volatile Registry<WandComponent> registry;

    /**
     * The wand (original {@code ItemWandCasting}): unstackable, uncommon, no
     * durability ({@code ItemWandCasting.java:57-62}).
     */
    public static final DeferredItem<ItemWandCasting> WAND_CASTING =
            ModItems.ITEMS.register("wandcasting",
                    () -> new ItemWandCasting(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.UNCOMMON)));

    /** The cap item (original {@code ItemWandCap}): stack 64, 9 damage-value variants. */
    public static final DeferredItem<ItemWandCap> WAND_CAP =
            ModItems.ITEMS.register("wandcap",
                    () -> new ItemWandCap(new Item.Properties()));

    /** The rod / staff-core item (original {@code ItemWandRod}): stack 64, 17 damage-value variants. */
    public static final DeferredItem<ItemWandRod> WAND_ROD =
            ModItems.ITEMS.register("wandrod",
                    () -> new ItemWandRod(new Item.Properties()));

    /**
     * Attach the wand-component registry + its entries (one wiring line in
     * {@code ModRegistration}). Adds the items to the shared {@link ModItems}
     * holder, so this call must precede {@code ModItems.ITEMS.register(bus)} —
     * the same class-init rule as {@code CatalogBaubles.init}.
     */
    public static void init(final IEventBus modEventBus) {
        for (final WandComponent component : WandComponents.components()) {
            WAND_COMPONENTS.register(component.tag(), () -> component);
        }
        WAND_COMPONENTS.register(modEventBus);
        modEventBus.addListener(CatalogWands::onNewRegistry);
    }

    private static void onNewRegistry(final NewRegistryEvent event) {
        registry = event.create(new RegistryBuilder<>(WAND_COMPONENT_REGISTRY_KEY).sync(true));
    }

    /** The live wand-component registry, or null before {@link NewRegistryEvent}. */
    public static Registry<WandComponent> registry() {
        return registry;
    }

    private CatalogWands() {
    }
}
