package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Block item of a multi-variant mega-block: names the stack after its own
 * variant (TC-M7-01b / t_f40bbc0d).
 *
 * <p>The original 1.7.10 mega-blocks were one id whose metadata selected the
 * sub-kind, and the game rendered {@code tile.<id>.<meta>.name} for each
 * meta — an Arcane Bore Base read "Arcane Bore Base", not "Arcane Bellows".
 * 1.21.1 has no per-state block display name: a {@link BlockItem} shows its
 * block's single {@code block.<ns>.<path>} key, so every variant of the ten
 * mega-blocks rendered the original's meta-0 label ("Alchemical Furnace" for
 * the whole of blockStoneDevice). That is the defect this class closes.</p>
 *
 * <p>1.21.1's hook for a per-stack block name is
 * {@link #getDescriptionId(ItemStack)} — {@code Item.getName(ItemStack)}
 * builds its component from it, and {@code ItemStack.getHoverName()} goes
 * through that, so tooltips, {@code /give} feedback and container names all
 * follow the stack. The variant is read from the stack by
 * {@link VariantStateItems#variantOf(ItemStack)} (the vanilla
 * {@code block_state} component first, the ported {@code ITEM_META} component
 * second) and resolved through {@link BlockVariantNames}, the declaration of
 * which (block, meta) pairs the original named. A stack with no variant, a
 * variant the original left unnamed (blockMetalDevice 4, blockWoodenDevice 3,
 * blockAiry 6-9), or a block outside the mega-block families keeps the plain
 * block key, exactly as before.</p>
 *
 * <p>Placement applies the stack's component (the variant round-trip the
 * per-card item classes each carried by hand; the shared implementation lives
 * here now), and the block classes re-attach the component on drops and
 * pick-block, so a picked Infusion Pillar names itself.</p>
 */
public class VariantBlockItem extends BlockItem {

    public VariantBlockItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    @Override
    public String getDescriptionId(final ItemStack stack) {
        final Integer variant = VariantStateItems.variantOf(stack);
        if (variant != null) {
            // The same-id contract (docs/architecture/registration.md) keeps
            // the block id and this item's id equal; the block's key is the
            // one the original's tile.<id>.<meta>.name keys map onto.
            final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(getBlock());
            if (BlockVariantNames.declares(id.getPath(), variant)) {
                return BlockVariantNames.key(id.getPath(), variant);
            }
        }
        return super.getDescriptionId(stack);
    }

    /** Apply the stack's {@code block_state} component when placing (variant round-trip). */
    @Nullable
    @Override
    protected BlockState getPlacementState(final BlockPlaceContext context) {
        final BlockState base = super.getPlacementState(context);
        return base == null ? null : VariantStateItems.applyVariant(base, context.getItemInHand());
    }

    /**
     * A stack of this item carrying one variant — in both carriers the name
     * resolution reads: the vanilla {@code block_state} component (what the
     * shipped recipe results use) and the ported {@code ITEM_META} component
     * (what the multi-meta items and the localization audit use).
     */
    public ItemStack stackWithVariant(final int variant) {
        final ItemStack stack = new ItemStack(this);
        stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                new net.minecraft.world.item.component.BlockItemStateProperties(
                        java.util.Map.of(VariantStateItems.VARIANT_PROPERTY, Integer.toString(variant))));
        stack.set(ModDataComponents.ITEM_META.get(), variant);
        return stack;
    }
}
