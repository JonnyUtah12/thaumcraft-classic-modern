package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.loot.FortunedUniformProvider;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * <p>SPINE-1 (2026-11-02): drops are shipped — see
 * {@code data/thaumcraft/loot_table/blocks/blockcustomore.json} for the
 * per-variant drop tables (cinnabar drops itself, infused stone drops
 * primal shards {@code meta = variant-1} with the original
 * {@code 1 + rand.nextInt(2 + fortune)} curve via
 * {@link FortunedUniformProvider}, amber drops itemResource meta 6 with
 * {@code 1 + rand.nextInt(fortune + 1)} via vanilla
 * {@code uniform_bonus_count}, and a silk-touch pool drops the block with
 * its variant preserved — the original getDrops has no silk branch because
 * 1.7.10 vanilla harvestBlock short-circuits to the block item under silk
 * touch, and {@code canSilkHarvest} is true (the 1.21.1 default) with
 * {@code damageDropped(meta) = meta}). The exp curve is the one part a loot
 * table cannot express, ported as {@link #getExpDrop} below.</p>
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

    /**
     * SPINE-1, original getExpDrop verbatim
     * (reference/decompiled/thaumcraft/thaumcraft/common/blocks/BlockCustomOre.java
     * lines 115-120): infused stone (1-6) → rand(0,3) inclusive, amber (7) →
     * rand(1,4) inclusive, cinnabar (0) → the vanilla default (0 — the
     * original delegates to super for the else-of-else case, vanilla gives no
     * exp for a plain Block). NeoForge routes this through CommonHooks'
     * BlockDropsEvent handling (1.21.1 vanilla has no getExpDrop; the exp
     * orb pop is the NeoForge extension), so the value follows the same
     * fortune-independent path as the original (the original's exp formula
     * ignores fortune — the {@code fortune} it receives is used only for
     * item counts).
     */
    @Override
    public int getExpDrop(final BlockState state, final LevelAccessor level, final BlockPos pos,
            @Nullable final BlockEntity blockEntity, @Nullable final Entity breaker, final ItemStack tool) {
        final int variant = state.getValue(VARIANT);
        // Mth.getRandomIntegerInRange is not public in 1.21.1 (the original
        // used MathHelper.getRandomIntegerInRange, an inclusive draw):
        final net.minecraft.util.RandomSource random = level.getRandom();
        if (variant >= VARIANT_INFUSED_MIN && variant <= VARIANT_INFUSED_MAX) {
            return random.nextInt(3 - 0 + 1) + 0;
        }
        if (variant == VARIANT_AMBER) {
            return random.nextInt(4 - 1 + 1) + 1;
        }
        return 0;
    }
}
