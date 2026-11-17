package com.jonnyutah12.thaumcraftclassic.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Port of {@code thaumcraft.api.IGoggles} (Azanor/thaumcraft-api, branch
 * 1.7.10 @ ced30ed, MIT; adapted per docs/licensing-and-attribution.md).
 * Original: {@code api/IGoggles.java} — one method, {@code showIngamePopups}.
 *
 * <p>Original javadoc: "Equipped head slot items that extend this class will be
 * able to perform most functions that goggles of revealing can apart from
 * viewing nodes, which is handled by {@code IRevealer}." The port keeps the
 * split: nodes go through {@link IRevealer}, the in-world aspect popups
 * (jar/barrel contents on hover) through this contract, whose consumer is the
 * TC-M7-01 visual pass.</p>
 */
public interface IGoggles {

    /** Original {@code showIngamePopups(ItemStack, EntityLivingBase)}. */
    boolean showIngamePopups(ItemStack stack, LivingEntity wearer);
}
