package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * TC custom ore block (content-mapping.csv {@code thaumcraft:blockcustomore}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockCustomOre} — one block id
 * whose metadata selected the ore kind: 0 cinnabar, 1-6 infused stone
 * (air/fire/water/earth/order/entropy), 7 amber-bearing stone. 1.21.1 has no
 * block metadata, so the kind moves to a {@code variant} property with the
 * same 0-7 values; the id itself is unchanged (D-001). Physical properties
 * are the original's: rock material, hardness 5.0, resistance 1.5, pickaxe
 * required with no tier floor (original setToolType(pickaxe) with unset
 * harvest level — any pickaxe harvests), silk-touch allowed (canSilkHarvest
 * true — the 1.21.1 default).</p>
 *
 * <p>Scope note (TC-M5-05): registered here because ore worldgen needs it.
 * Per-kind drop tables (itemShard for infused variants, itemResource meta 6
 * for amber — original getDrops) and the 8-variant creative-tab listing need
 * items that arrive with TC-M6-04; until then the block drops itself, which
 * is already the original behavior for the cinnabar variant.</p>
 */
public class BlockCustomOre extends Block {

    /** Original blockCustomOre metadata, verbatim 0-7 (see class javadoc). */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 7);

    public static final int VARIANT_CINNABAR = 0;
    public static final int VARIANT_INFUSED_MIN = 1;
    public static final int VARIANT_INFUSED_MAX = 6;
    public static final int VARIANT_AMBER = 7;

    public BlockCustomOre() {
        super(Properties.of()
                .strength(5.0F, 1.5F)
                .requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(VARIANT, VARIANT_CINNABAR));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }
}
