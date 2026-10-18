package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
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

    private ModDataComponents() {
    }
}
