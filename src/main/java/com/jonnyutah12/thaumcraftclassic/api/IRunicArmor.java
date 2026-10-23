package com.jonnyutah12.thaumcraftclassic.api;

import net.minecraft.world.item.ItemStack;

/**
 * Port of {@code thaumcraft.api.IRunicArmor} (Azanor/thaumcraft-api,
 * branch 1.7.10 @ ced30ed, MIT; adapted per
 * docs/licensing-and-attribution.md). Original:
 * {@code api/IRunicArmor.java} — one method, {@code getRunicCharge}.
 *
 * <p>Anything implementing this contributes runic-shield capacity to its
 * wearer: the runic baubles (TC-M8-02, {@code items/baubles/ItemRingRunic /
 * ItemAmuletRunic / ItemGirdleRunic}) and the runic armors (TC-M6-07) all
 * carry the original's per-meta charge values through it. The original also
 * added a hardening bonus from the {@code "RS.HARDEN"} stack tag at read
 * time ({@code EventHandlerRunic.getFinalCharge}, {@code common/lib/events/
 * EventHandlerRunic.java:343-354}); the port carries that as the
 * {@code thaumcraft:runic_hardening} data component, applied by
 * {@code runic/RunicShielding#finalCharge}.</p>
 */
public interface IRunicArmor {

    /** Original {@code getRunicCharge(ItemStack)}: base capacity of this stack. */
    int getRunicCharge(ItemStack stack);
}
