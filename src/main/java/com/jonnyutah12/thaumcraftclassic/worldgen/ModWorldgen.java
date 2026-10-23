package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Worldgen registration holder (TC-M5-05): the TC ore feature type and the
 * placement-modifier types the ore placed-features are built from.
 *
 * <p>Like {@code ModSounds}/{@code ModAttachments}, this holder is independent
 * of the block/item chain and is registered from the mod constructor next to
 * {@code ModRegistration.init} (constructor-time rule,
 * docs/architecture/registration.md). The features themselves are datapack
 * objects (data/thaumcraft/worldgen/...) wired into biomes by the
 * {@code neoforge:add_features} biome modifier — only their types are
 * registered here. See docs/architecture/worldgen.md.</p>
 */
public final class ModWorldgen {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, ThaumcraftClassic.MOD_ID);

    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, ThaumcraftClassic.MOD_ID);

    /** TC ore feature: vanilla vein algorithm with per-vein ore-kind choice. */
    public static final DeferredHolder<Feature<?>, TcOreFeature> TC_ORE =
            FEATURES.register("tc_ore", () -> new TcOreFeature(TcOreConfiguration.CODEC));

    /** Passes positions only while the named TC config option is enabled. */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<ConfigGatePlacement>>
            CONFIG_GATE = PLACEMENT_TYPES.register("config_gate", () -> () -> ConfigGatePlacement.CODEC);

    /** Y = surface - [min_offset, max_offset] (amber). */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<SurfaceBelowPlacement>>
            SURFACE_BELOW = PLACEMENT_TYPES.register("surface_below", () -> () -> SurfaceBelowPlacement.CODEC);

    /** Y = [min, max(floor, surface + surface_offset)) (infused stone). */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<SurfaceCappedUniformPlacement>>
            SURFACE_CAPPED_UNIFORM =
                    PLACEMENT_TYPES.register("surface_capped_uniform",
                            () -> () -> SurfaceCappedUniformPlacement.CODEC);

    public static void init(final IEventBus modEventBus) {
        FEATURES.register(modEventBus);
        PLACEMENT_TYPES.register(modEventBus);
    }

    private ModWorldgen() {
    }
}
