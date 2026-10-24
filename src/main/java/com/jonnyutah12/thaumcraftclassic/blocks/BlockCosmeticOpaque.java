package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

/**
 * TC cosmetic opaque block (content-mapping.csv {@code thaumcraft:blockcosmeticopaque}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockCosmeticOpaque} — one block
 * id, metadata 0 amber block, 1 amber bricks, 2 warded glass. The metas move
 * verbatim to a {@code variant} property (D-001, same convention as
 * {@link BlockCustomOre}). Physical properties are the original's: hardness
 * 1.5, resistance 5.0, stone step sound.</p>
 *
 * <p>Warded glass (variant 2) keeps the original's special cases: unbreakable
 * by hand (original returned hardness -1 while {@code Config.wardedStone} is
 * on, which is the shipped default — the config toggle is TC-M6 territory and
 * is not re-honored here), explosion resistance 999 (the original's
 * {@code getExplosionResistance} override; its {@code onBlockExploded} no-op
 * is redundant against 999 and not ported), and a {@code TileOwned} host —
 * the tile itself is TC-M6-04c's row and is supplied through
 * {@link DeviceTileFactory}; this class only provides the hook.</p>
 *
 * <p>Client-side scope notes (TC-M7-01): the 47-frame connected-texture
 * warded-glass icon selection ({@code getIcon} bit-matrix +
 * {@code BlockCosmeticOpaqueRenderer}) and the ward hit-effect particles have
 * no 1.21.1 equivalent yet. The original's per-meta light opacity (3 for the
 * amber variants) is not state-addressable in 1.21.1 and is left to the
 * blockstate models.</p>
 */
public class BlockCosmeticOpaque extends Block implements VariantDevice, EntityBlock {

    /** Original blockCosmeticOpaque metadata, verbatim 0-2 (see class javadoc). */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

    public static final int VARIANT_AMBER_BLOCK = 0;
    public static final int VARIANT_AMBER_BRICKS = 1;
    public static final int VARIANT_WARDED_GLASS = 2;

    public BlockCosmeticOpaque() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(1.5F, 5.0F)
                .sound(SoundType.STONE));
        registerDefaultState(stateDefinition.any().setValue(VARIANT, VARIANT_AMBER_BLOCK));
    }

    @Override
    public int variant(final BlockState state) {
        return state.getValue(VARIANT);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    /** Original {@code getBlockHardness}: -1 (unbreakable) for warded glass. */
    @Override
    public float getDestroySpeed(final BlockState state, final Player player) {
        return variant(state) == VARIANT_WARDED_GLASS ? -1.0F : super.getDestroySpeed(state, player);
    }

    /** Original {@code getExplosionResistance}: 999 for warded glass. */
    @Override
    public float getExplosionResistance(final BlockState state) {
        return variant(state) == VARIANT_WARDED_GLASS ? 999.0F : super.getExplosionResistance(state);
    }

    /**
     * Warded glass is a {@code TileOwned} host (original
     * {@code createTileEntity}); the tile class is TC-M6-04c's and registers
     * its supplier through {@link DeviceTileFactory}.
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return DeviceTileFactory.create(state, pos);
    }

    /** Preserve the variant through drops / pick-block (see {@link VariantStateItems}). */
    @Override
    public ItemStack getCloneItemStack(final BlockState state, final ServerLevel level,
                                      final BlockPos pos, final ItemStack tool) {
        return VariantStateItems.withVariant(super.getCloneItemStack(state, level, pos, tool), state);
    }
}
