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
import net.minecraft.world.level.LevelReader;

/**
 * TC cosmetic stone slab (content-mapping.csv rows
 * {@code thaumcraft:blockcosmeticslabstone} +
 * {@code thaumcraft:blockcosmeticdoubleslabstone}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockCosmeticStoneSlab} — the
 * 1.7.10 pair blockSlabStone/blockDoubleSlabStone, two types (0 arcane ←
 * blockCosmeticSolid meta 7, 1 eldritch ← blockCosmeticSolid meta 11). In
 * 1.21.1 one {@link SlabBlock} carries the pair (vanilla {@code type}
 * property for the double form, original type value in a {@code variant}
 * property, D-001); the legacy {@code thaumcraft:blockcosmeticdoubleslabstone}
 * id is registered as a second instance of this class so the content-mapping
 * row resolves.</p>
 *
 * <p>Properties are the original's: rock material (resistance 30.0), hardness
 * never set (0.0F, ported verbatim). The original's double-slab drop quantity
 * is 1 — the vanilla default — so no drop override is needed.</p>
 */
public class BlockCosmeticStoneSlab extends SlabBlock {

    /** Original blockCosmeticSlabStone type values, verbatim. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 1);

    public static final int VARIANT_ARCANE = 0;
    public static final int VARIANT_ELDRITCH = 1;

    public BlockCosmeticStoneSlab() {
        super(Properties.of()
                .mapColor(MapColor.STONE)
                .strength(0.0F, 30.0F)
                .sound(SoundType.STONE));
        registerDefaultState(stateDefinition.any()
                .setValue(VARIANT, VARIANT_ARCANE)
                .setValue(TYPE, SlabType.BOTTOM));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    /** Preserve the variant through drops / pick-block (see {@link VariantStateItems}). */
    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        return VariantStateItems.withVariant(super.getCloneItemStack(level, pos, state), state);
    }
}
