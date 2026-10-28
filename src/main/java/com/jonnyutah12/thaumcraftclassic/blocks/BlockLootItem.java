package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block item for the loot containers (original {@code BlockLootItem}).
 *
 * <p>The original carried the rarity in the block item's metadata and showed
 * it through {@code getRarity} (name color + tooltip). 1.21.1 carries the
 * variant on the shared ITEM_META component instead: placement maps
 * ITEM_META → the {@code variant} property, and the rarity comes from the
 * vanilla {@code RARITY} data component set on the stack (creative-tab
 * entries and tests build stacks via {@link BlockLoot#withRarity} plus
 * {@link #applyRarity}).</p>
 */
public class BlockLootItem extends BlockItem {

    public BlockLootItem(final Block block, final Item.Properties properties) {
        super(block, properties);
    }

    /** Original placement kept the metadata; ITEM_META → variant property. */
    @Override
    protected BlockState getPlacementState(final BlockPlaceContext context) {
        final Integer meta = context.getItemInHand().get(ModDataComponents.ITEM_META.get());
        if (meta != null && meta >= BlockLoot.RARITY_COMMON && meta <= BlockLoot.RARITY_RARE) {
            return this.getBlock().defaultBlockState().setValue(BlockLoot.VARIANT, meta);
        }
        return this.getBlock().defaultBlockState();
    }
}
