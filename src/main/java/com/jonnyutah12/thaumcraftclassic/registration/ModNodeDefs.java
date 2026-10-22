package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.nodes.NodeModifier;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * Custom registries for vis node types and modifiers (TC-M5-01).
 *
 * <p>Same pattern as {@link ModAspects}: the registries are created in
 * {@link NewRegistryEvent} (the only point where custom registries can be
 * registered) and the enum constants are attached through
 * {@link DeferredRegister}s registered on the mod bus in the same handler, so
 * the {@code thaumcraft:nodetype.*} / {@code thaumcraft:nodemodifier.*} ids
 * pinned by docs/content-mapping.csv exist as real registry entries. The
 * registration contract is asserted by {@code RegistrationGameTest}.</p>
 */
@EventBusSubscriber(modid = "thaumcraft", bus = EventBusSubscriber.Bus.MOD)
public final class ModNodeDefs {

    public static final ResourceKey<Registry<NodeType>> NODE_TYPE_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "node_type"));
    public static final ResourceKey<Registry<NodeModifier>> NODE_MODIFIER_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "node_modifier"));

    private static final DeferredRegister<NodeType> NODE_TYPES =
            DeferredRegister.create(NODE_TYPE_REGISTRY_KEY, "thaumcraft");
    private static final DeferredRegister<NodeModifier> NODE_MODIFIERS =
            DeferredRegister.create(NODE_MODIFIER_REGISTRY_KEY, "thaumcraft");

    static {
        for (final NodeType type : NodeType.values()) {
            NODE_TYPES.register(type.registryPath(), () -> type);
        }
        for (final NodeModifier modifier : NodeModifier.values()) {
            NODE_MODIFIERS.register(modifier.registryPath(), () -> modifier);
        }
    }

    private static volatile Registry<NodeType> nodeTypeRegistry;
    private static volatile Registry<NodeModifier> nodeModifierRegistry;

    /** The live {@code thaumcraft:node_type} registry; null before registration completes. */
    public static Registry<NodeType> nodeTypes() {
        return nodeTypeRegistry;
    }

    /** The live {@code thaumcraft:node_modifier} registry; null before registration completes. */
    public static Registry<NodeModifier> nodeModifiers() {
        return nodeModifierRegistry;
    }

    @SubscribeEvent
    static void onNewRegistry(final NewRegistryEvent event) {
        nodeTypeRegistry = event.create(new RegistryBuilder<>(NODE_TYPE_REGISTRY_KEY).sync(true));
        nodeModifierRegistry = event.create(new RegistryBuilder<>(NODE_MODIFIER_REGISTRY_KEY).sync(true));
        // Attach the enum constants to the mod bus so the RegisterEvent pass fills
        // the registries created above.
        final net.neoforged.bus.api.IEventBus bus =
                ModList.get().getModContainerById("thaumcraft").orElseThrow().getEventBus();
        NODE_TYPES.register(bus);
        NODE_MODIFIERS.register(bus);
    }

    private ModNodeDefs() {
    }
}
