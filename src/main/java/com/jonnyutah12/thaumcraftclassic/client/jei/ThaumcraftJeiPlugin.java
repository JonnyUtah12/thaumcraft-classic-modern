package com.jonnyutah12.thaumcraftclassic.client.jei;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * The JEI entry point for the port's four custom recipe kinds (SPINE-4,
 * t_aebd14cf).
 *
 * <p><b>Discovery, not wiring.</b> JEI finds this class through
 * {@code @JeiPlugin} and calls it; nothing in the core mod references it, and
 * nothing in the core mod references a {@code mezz.jei} type at all. On a
 * machine with no JEI installed, JEI's own mod never loads, this class is never
 * loaded, and the port's load path is byte-for-byte the same as before this
 * card — the shape the card requires ("the mod loads identically without
 * JEI"). {@code JeiPluginIsolationTest} enforces the direction with a bytecode
 * scan, mirroring the CuriosBridge guard.</p>
 *
 * <p><b>Optional by dependency shape.</b> JEI is {@code compileOnly} plus a
 * localRuntime devjar and is never bundled into the port's jar (D-016; the
 * Curios precedent) — see the build file's "Third-party integration jars"
 * block. The card's own note is that "items are in JEI" needs no code: every
 * registered item is listed for free by JEI, and SPINE-2's creative tab put
 * every catalog item in a public tab. This plugin exists only for what JEI
 * cannot infer — the four custom recipe categories.</p>
 *
 * <p>Everything drawn comes from {@code RecipeDisplay}; this class moves lists
 * from {@link JeiRecipeSource} into JEI and asks for the catalyst bindings.
 * Recipe transfer (JEI click → the port's containers) is out of this card's
 * scope and left to the follow-up noted in {@code docs/jei-integration.md}.</p>
 */
@JeiPlugin
public final class ThaumcraftJeiPlugin implements IModPlugin {

    /** JEI namespaces plugin data by this id; stable so translations survive. */
    public static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "thaumcraft_classic_recipes");

    private final JeiRecipeSource source;

    /** The constructor JEI instantiates. */
    public ThaumcraftJeiPlugin() {
        this(JeiRecipeSource.live());
    }

    /** The seam {@code JeiPluginRegistrationTest} injects rows through. */
    public ThaumcraftJeiPlugin(final JeiRecipeSource source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(final IRecipeCategoryRegistration registration) {
        final IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new ArcaneWorkbenchCategory(gui),
                new CrucibleCategory(gui),
                new InfusionCategory(gui),
                new InfusionEnchantmentCategory(gui));
    }

    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        registration.addRecipes(ArcaneWorkbenchCategory.TYPE, source.arcaneWorkbench());
        registration.addRecipes(CrucibleCategory.TYPE, source.crucible());
        registration.addRecipes(InfusionCategory.TYPE, source.infusion());
        registration.addRecipes(InfusionEnchantmentCategory.TYPE, source.infusionEnchantment());
    }

    /** The blocks that open each category's page: the machines themselves. */
    @Override
    public void registerRecipeCatalysts(final IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(CatalogArcaneWorkbench.ARCANE_WORKBENCH_ITEM.get()),
                ArcaneWorkbenchCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(CatalogDevices.ITEM_METAL_DEVICE.get()),
                CrucibleCategory.TYPE);
        // The stone device carries both altar kinds: infusion stone and pedestal.
        registration.addRecipeCatalyst(new ItemStack(CatalogDevices.ITEM_STONE_DEVICE.get()),
                InfusionCategory.TYPE, InfusionEnchantmentCategory.TYPE);
    }
}
