package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

/**
 * TC cosmetic wood slab (content-mapping.csv rows
 * {@code thaumcraft:blockcosmeticslabwood} +
 * {@code thaumcraft:blockcosmeticdoubleslabwood}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockCosmeticWoodSlab} — the
 * 1.7.10 pair blockSlabWood/blockDoubleSlabWood, two types (0 greatwood,
 * 1 silverwood) whose icons mirror blockWoodenDevice plank metas 6/7
 * (TC-M6-04c's row). In 1.21.1 one {@link SlabBlock} carries the whole pair:
 * the vanilla {@code type} property (bottom/top/double) replaces the second
 * block id, and the original type value moves verbatim into a
 * {@code variant} property (D-001). The legacy
 * {@code thaumcraft:blockcosmeticdoubleslabwood} id is registered as a second
 * instance of this class so the content-mapping row resolves; nothing places
 * it (the merged slab block covers the double form).</p>
 *
 * <p>Properties are the original's: wood material (resistance 5.0), and —
 * as with the stairs — no hardness was ever set, so hardness 0.0 is ported
 * verbatim. The original's double-slab drop quantity (2 single slabs,
 * {@code quantityFromDrops}) is kept: breaking a merged double slab yields
 * two slabs of the same variant.</p>
 */
public class BlockCosmeticWoodSlab extends SlabBlock {

    /** Original blockCosmeticSlabWood type values, verbatim. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 1);

    public static final int VARIANT_GREATWOOD = 0;
    public static final int VARIANT_SILVERWOOD = 1;

    public BlockCosmeticWoodSlab() {
        super(Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(0.0F, 5.0F)
                .sound(SoundType.WOOD));
        registerDefaultState(stateDefinition.any()
                .setValue(VARIANT, VARIANT_GREATWOOD)
                .setValue(TYPE, SlabType.BOTTOM));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    /**
     * Original {@code quantityFromDrops}: a double wood slab returns two
     * single slabs (same variant). Single/bottom forms fall through to the
     * default block drop.
     */
    @Override
    public void getDrops(final BlockState state, final ServerLevel level, final BlockPos pos,
                         @Nullable final BlockEntity blockEntity, final ItemStack tool) {
        if (state.getValue(TYPE) == SlabType.DOUBLE) {
            final ItemStack drop = VariantStateItems.withVariant(new ItemStack(this), state);
            drop.setCount(2);
            popResource(level, pos, drop);
        } else {
            super.getDrops(state, level, pos, blockEntity, tool);
        }
    }

    /** Preserve the variant through drops / pick-block (see {@link VariantStateItems}). */
    @Override
    public ItemStack getCloneItemStack(final BlockState state, final ServerLevel level,
                                      final BlockPos pos, final ItemStack tool) {
        return VariantStateItems.withVariant(super.getCloneItemStack(state, level, pos, tool), state);
    }
}
