package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory;
import com.jonnyutah12.thaumcraftclassic.recipes.CrucibleRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.SimpleRecipeSerializer;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCrucible;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * TC-M4-05-s catalog holder: the crucible (alchemy) core — its block entity,
 * the (block, variant) binding that hosts it, and the recipe type + serializer
 * pair the crucible data files resolve through.
 *
 * <p>Owns exactly the content-mapping.csv rows this card flips: the
 * tile-entity row {@code thaumcraft:tilecrucible} (the original has no block
 * class of its own — {@code BlockMetalDevice.createTileEntity} hands metadata
 * 0 the {@code TileCrucible} class, so the port binds that tile to
 * {@code blockmetaldevice} variant 0 through {@link DeviceTileFactory} and
 * keeps the D-001 id contract on the tile-entity row), and the recipe rows
 * {@code thaumcraft:recipe.<0..48>.crucible} (54 instances, the BalancedShard
 * family expands to six) loaded from
 * {@code data/thaumcraft/recipe/recipe.<N>.crucible[.<idx>].json}, whose
 * {@code type} field names {@code thaumcraft:crucible}.</p>
 *
 * <p>The host block belongs to TC-M6-04c (the row's own note defers the
 * crucible host to this card), so it is looked up by id — a runtime, not a
 * compile-time, dependency, mirroring {@link CatalogInfusion}'s
 * {@code stoneDevice()} and keeping feature cards out of the device block
 * classes (docs/catalog-triage.md hotspot rule).</p>
 *
 * <p>Out of this holder by design: the crucible's client-side rendering and
 * particles (TC-M7-01), the essentia jar that refills it (TC-M4-06), the
 * alchemical furnace (TC-M4-06) and the {@code blockfluxgoo}/{@code
 * blockfluxgas} blocks its spill places (their own resource-card rows).</p>
 */
public final class CatalogCrucible {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    // --- recipe type + serializer ------------------------------------------

    /** The recipe type the 54 crucible data files name; only TileCrucible reads it. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<CrucibleRecipe>> CRUCIBLE_TYPE =
            RECIPE_TYPES.register("crucible",
                    () -> RecipeType.simple(id("crucible")));

    /** Decodes the {@code catalyst}/{@code aspects}/{@code result}/{@code research} JSON. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrucibleRecipe>> CRUCIBLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("crucible",
                    () -> new SimpleRecipeSerializer<CrucibleRecipe>(
                            CrucibleRecipe.CODEC, CrucibleRecipe.STREAM_CODEC));

    // --- the crucible's block entity ---------------------------------------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCrucible>> TILE_CRUCIBLE =
            BLOCK_ENTITY_TYPES.register("tilecrucible",
                    () -> BlockEntityType.Builder.of(TileCrucible::new,
                            metalDevice()).build(null));

    /**
     * The host block, looked up by id (TC-M6-04c owns {@code blockmetaldevice};
     * block-entity registration runs after the block registry flush).
     */
    private static Block metalDevice() {
        return BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blockmetaldevice"));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    /** One wiring line from ModRegistration.init (catalog-triage pattern). */
    public static void init(final IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        bus.addListener((final RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                wireDeviceTiles();
            }
        });
    }

    /** The (block, variant) → tile binding, per the original's createTileEntity. */
    private static void wireDeviceTiles() {
        final Block metal = metalDevice();
        if (metal == Blocks.AIR) {
            return; // blockmetaldevice not registered (TC-M6-04c unwired) — no crucible
        }
        // blockMetalDevice 0 = crucible: the original's createTileEntity metadata 0.
        DeviceTileFactory.register(metal, 0, TileCrucible::new);
    }

    private CatalogCrucible() {
    }
}
