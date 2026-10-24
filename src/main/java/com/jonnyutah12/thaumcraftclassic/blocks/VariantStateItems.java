package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared helper for the multi-variant catalog blocks (TC-M6-04b and later
 * catalog cards; the convention is pinned in the {@code ITEM_META} javadoc of
 * {@link com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents}):
 * original block metadata lives in an {@code IntegerProperty} named
 * {@code variant}, and multi-meta block items carry the placed state through
 * the vanilla {@code minecraft:block_state} component
 * ({@link BlockItemStateProperties}).
 *
 * <p>Recipe JSONs address variants the same way: results carry
 * {@code "components": {"minecraft:block_state": {"variant": "N"}}} and
 * ingredients match them with a {@code neoforge:components} ingredient. Block
 * classes call {@link #variantComponent(BlockState)} from
 * {@code getCloneItemStack} so drops and pick-block preserve the variant.</p>
 */
public final class VariantStateItems {

    /** The property name every multi-variant catalog block uses (D-001: original meta values verbatim). */
    public static final String VARIANT_PROPERTY = "variant";

    /**
     * The {@code minecraft:block_state} component value carrying this state's
     * {@code variant} property, for {@code stack.set(DataComponents.BLOCK_STATE, …)}.
     */
    public static BlockItemStateProperties variantComponent(final BlockState state) {
        final Property<?> variant = state.getBlock().getStateDefinition().getProperty(VARIANT_PROPERTY);
        if (variant == null) {
            return BlockItemStateProperties.EMPTY;
        }
        final Map<String, String> properties = new HashMap<>();
        properties.put(VARIANT_PROPERTY, name(state, variant));
        return new BlockItemStateProperties(properties);
    }

    private static <T extends Comparable<T>> String name(final BlockState state, final Property<T> property) {
        return property.getName(state.getValue(property));
    }

    /** Convenience: attach the variant component to a fresh block-item stack. */
    public static ItemStack withVariant(final ItemStack stack, final BlockState state) {
        stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE, variantComponent(state));
        return stack;
    }

    /**
     * Merge the {@code minecraft:block_state} component of a held stack into
     * a placement state (vanilla {@code BlockItem} ignores the component when
     * placing; variant items must apply it themselves via
     * {@code getPlacementState}). Unknown properties are skipped by the
     * component's own {@code apply}.
     */
    public static BlockState applyVariant(final BlockState placement, final ItemStack stack) {
        final BlockItemStateProperties component =
                stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        return component == null || component.isEmpty() ? placement : component.apply(placement);
    }

    private VariantStateItems() {
    }
}
