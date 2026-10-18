package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Creative tab registration holder (TC-M2-01 registration architecture).
 *
 * <p>The original exposed one creative tab; its contents are the registered
 * item set. Tabs reference block/item holders, so they register after items
 * (NeoForge's own event order guarantees this; the constructor order in
 * {@link ModRegistration} mirrors it for readability).</p>
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThaumcraftClassic.MOD_ID);

    private ModCreativeTabs() {
    }
}
