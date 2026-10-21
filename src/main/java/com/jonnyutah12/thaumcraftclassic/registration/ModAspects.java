package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * The {@code thaumcraft:aspect} custom registry (TC-M4-01): the 48 aspects
 * from {@link AspectDefinitions} as real registry objects, replacing the
 * original's static {@code Aspect.aspect[]} map. The registry itself is
 * created on the mod bus via {@link NewRegistryEvent} (synced to clients,
 * matching the original's aspect sync packet) and the holders land through a
 * standard {@link DeferredRegister}.
 *
 * <p>NOTE: this file was reconstructed on 2026-10-19 during TC-M6-04a after
 * the original TC-M4-01 working copy was lost from the shared workspace
 * (never committed). It satisfies the exact contract the surviving
 * {@link Aspect} static accessors and {@link ModRegistration} use:
 * {@link #register(IEventBus)}, {@link #registry()} (null before the
 * registry event, as Aspect's null-guards expect) and
 * {@link #aspectId(String)}. If the original resurfaces, prefer it.</p>
 */
public final class ModAspects {

    public static final ResourceKey<Registry<Aspect>> ASPECT_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "aspect"));

    public static final DeferredRegister<Aspect> ASPECTS =
            DeferredRegister.create(ASPECT_REGISTRY_KEY, ThaumcraftClassic.MOD_ID);

    private static volatile Registry<Aspect> registry;

    public static void register(final IEventBus modEventBus) {
        for (final AspectDefinitions.Definition definition : AspectDefinitions.all()) {
            ASPECTS.register(definition.tag(), () -> new Aspect(definition));
        }
        ASPECTS.register(modEventBus);
        modEventBus.addListener(ModAspects::onNewRegistry);
    }

    private static void onNewRegistry(final NewRegistryEvent event) {
        registry = event.create(new RegistryBuilder<>(ASPECT_REGISTRY_KEY).sync(true));
    }

    /** The live aspect registry, or null before {@link NewRegistryEvent} (callers null-guard). */
    public static Registry<Aspect> registry() {
        return registry;
    }

    /** Registry id for an aspect tag, e.g. {@code thaumcraft:ignis}. */
    public static ResourceLocation aspectId(final String tag) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, tag);
    }

    private ModAspects() {
    }
}
