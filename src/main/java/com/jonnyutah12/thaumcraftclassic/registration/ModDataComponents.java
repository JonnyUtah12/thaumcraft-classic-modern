package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaContainer;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data component registration holder (TC-M2-01 registration architecture).
 *
 * <p>Replaces the original's ad-hoc ItemStack NBT keys (wand charge, vis
 * storage, research hints, ...) with typed {@code DataComponentType}s. The
 * component id set is defined by the feature milestones that need them
 * (M4+); this holder exists so component registration is wired before any
 * item construction reads a component (NeoForge orders
 * {@code data_component_type} before items for exactly that reason).</p>
 */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ThaumcraftClassic.MOD_ID);

    /**
     * Essentia contents of a stack (TC-M4-01): replaces the original
     * {@code IEssentiaContainerItem} ItemStack NBT key "Aspects" with a typed
     * component. Persistent (the original wrote it to stack NBT) and synced
     * for the aspect-tinted rendering the original read client-side
     * (docs/aspects.md).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EssentiaContainer>> ESSENTIA_CONTAINER =
            DATA_COMPONENT_TYPES.register("essentia_container",
                    () -> DataComponentType.<EssentiaContainer>builder()
                            .persistent(EssentiaContainer.CODEC)
                            .networkSynchronized(EssentiaContainer.STREAM_CODEC)
                            .build());

    private ModDataComponents() {
    }
}
