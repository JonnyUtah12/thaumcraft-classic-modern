package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockStoneDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory;
import com.jonnyutah12.thaumcraftclassic.recipes.InfusionCraftingRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.InfusionEnchantmentRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.SimpleRecipeSerializer;
import com.jonnyutah12.thaumcraftclassic.tiles.TileInfusionMatrix;
import com.jonnyutah12.thaumcraftclassic.tiles.TileInfusionPillar;
import com.jonnyutah12.thaumcraftclassic.tiles.TilePedestal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * TC-M4-04 catalog holder: the infusion altar core (runic matrix + infusion
 * pillars) and the two recipe types its data files resolve through.
 *
 * <p>Owns exactly the content-mapping.csv rows this card flips: the tile-entity
 * rows {@code thaumcraft:tileinfusionstone} (the original registers the {@code
 * TileInfusionMatrix} class under the string {@code "TileInfusionStone"}, so
 * the D-001 id contract keeps that name) and {@code
 * thaumcraft:tileinfusionpillar}, both hosted on {@code
 * blockstonedevice} (row owned by TC-M6-04c, whose javadoc defers the
 * non-furnace metas to their feature cards), plus the recipe rows {@code
 * thaumcraft:recipe.<49..111>.infusion} / {@code recipe.<201..224>.
 * infusion_enchantment} loaded from {@code data/thaumcraft/recipe/} whose
 * {@code type} field names {@code thaumcraft:infusion} / {@code
 * thaumcraft:infusion_enchantment}.</p>
 *
 * <p>The original's {@code BlockStoneDevice.createTileEntity} hands meta 1
 * (arcane pedestal) the same {@code TilePedestal} class TC-M6-04d owns; the
 * binding lives here because the altar is that pedestal's only consumer and no
 * other card binds blockStoneDevice 1 (docs/catalog-triage.md hotspot rule:
 * feature cards never edit the device block classes). Out of this holder by
 * design: the matrix's client-side particles/runes (TC-M7-01) and the
 * essentia jars that feed it (TC-M4-06).</p>
 */
public final class CatalogInfusion {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    // --- recipe types + serializers ---------------------------------------

    /** The recipe type the 63 {@code recipe.<49..111>.infusion} files name. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<InfusionCraftingRecipe>> INFUSION_TYPE =
            RECIPE_TYPES.register("infusion",
                    () -> RecipeType.simple(id("infusion")));

    /** Decodes their {@code input}/{@code components}/{@code aspects} JSON. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InfusionCraftingRecipe>> INFUSION_SERIALIZER =
            RECIPE_SERIALIZERS.register("infusion",
                    () -> new SimpleRecipeSerializer<InfusionCraftingRecipe>(
                            InfusionCraftingRecipe.CODEC, InfusionCraftingRecipe.STREAM_CODEC));

    /** The recipe type the 24 {@code recipe.<201..224>.infusion_enchantment} files name. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<InfusionEnchantmentRecipe>> INFUSION_ENCHANTMENT_TYPE =
            RECIPE_TYPES.register("infusion_enchantment",
                    () -> RecipeType.simple(id("infusion_enchantment")));

    /** Decodes their {@code enchantment}/{@code components}/{@code aspects} JSON. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InfusionEnchantmentRecipe>> INFUSION_ENCHANTMENT_SERIALIZER =
            RECIPE_SERIALIZERS.register("infusion_enchantment",
                    () -> new SimpleRecipeSerializer<InfusionEnchantmentRecipe>(
                            InfusionEnchantmentRecipe.CODEC, InfusionEnchantmentRecipe.STREAM_CODEC));

    // --- the altar's block entities ---------------------------------------

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileInfusionMatrix>> TILE_INFUSION_MATRIX =
            BLOCK_ENTITY_TYPES.register("tileinfusionstone",
                    () -> BlockEntityType.Builder.of(TileInfusionMatrix::new,
                            stoneDevice()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileInfusionPillar>> TILE_INFUSION_PILLAR =
            BLOCK_ENTITY_TYPES.register("tileinfusionpillar",
                    () -> BlockEntityType.Builder.of(TileInfusionPillar::new,
                            stoneDevice()).build(null));

    /**
     * The host block, looked up by id (TC-M6-04c owns {@code
     * blockstonedevice}; the lookup keeps the binding a runtime dependency, not
     * a compile-time one, and block-entity registration runs after the block
     * registry flush).
     */
    private static Block stoneDevice() {
        return BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blockstonedevice"));
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

    /** The (block, variant) → tile bindings, per the original's createTileEntity. */
    private static void wireDeviceTiles() {
        final Block stone = stoneDevice();
        if (stone == net.minecraft.world.level.block.Blocks.AIR) {
            return; // blockstonedevice not registered (TC-M6-04c unwired) — no altar
        }
        // blockStoneDevice 1 = arcane pedestal: the original BlockStoneDevice
        // createTileEntity meta 1, and the altar's centre (the matrix reads the
        // pedestal two rows below it). The tile class itself is TC-M6-04d's.
        DeviceTileFactory.register(stone, 1, TilePedestal::new);
        DeviceTileFactory.register(stone, BlockStoneDevice.VARIANT_RUNIC_MATRIX,
                TileInfusionMatrix::new);
        DeviceTileFactory.register(stone, BlockStoneDevice.VARIANT_PILLAR_A,
                TileInfusionPillar::new);
        DeviceTileFactory.register(stone, BlockStoneDevice.VARIANT_PILLAR_B,
                TileInfusionPillar::new);
    }

    private CatalogInfusion() {
    }
}
