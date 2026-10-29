package com.jonnyutah12.thaumcraftclassic.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Port of {@code thaumcraft.api.nodes.IRevealer} (Thaumcraft 4.2.3.5,
 * {@code api/nodes/IRevealer.java}; adapted per
 * docs/licensing-and-attribution.md). One method, {@code showNodes}.
 *
 * <p>Equipping such a stack in the head slot reveals aura nodes: the original's
 * node renderer gated on
 * the head slot (index 3) item {@code instanceof IRevealer} and
 * showNodes(stack, viewer)} ({@code client/renderers/tile/TileNodeRenderer.java:187-189}).
 * The port reads the same rule in {@code nodes/NodeReveal}.</p>
 */
public interface IRevealer {

    /** Original {@code showNodes(ItemStack, EntityLivingBase)}. */
    boolean showNodes(ItemStack stack, LivingEntity wearer);
}
