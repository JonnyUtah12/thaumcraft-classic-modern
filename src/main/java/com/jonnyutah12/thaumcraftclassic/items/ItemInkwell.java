package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.api.IScribeTools;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileResearchTable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Scribing Tools / inkwell (TC-M3-04; content-mapping.csv
 * {@code thaumcraft:iteminkwell}).
 *
 * <p>Original: {@code thaumcraft.common.items.ItemInkwell} — max damage 100,
 * unstackable, implements {@link IScribeTools}. Its {@code onItemUseFirst}
 * assembles the research-table multiblock: right-click a table (any
 * {@code TileTable} variant except 6) that has a horizontal table neighbour
 * and the clicked half flips to a research-table base (variant 2-5, encoding
 * the direction toward the neighbour), the neighbour becomes the top
 * (variant 6-9, the opposite direction), the inkwell lands in the new
 * table's scribe slot, and one copy is consumed from the player unless
 * creative.</p>
 *
 * <p>Variant/direction mapping keeps the original metadata encoding exactly:
 * ForgeDirection ordinals 2..5 (north, south, west, east) are table variants
 * 2..5; the top half carries {@code opposite + 4}. {@link TileResearchTable}
 * reads the same encoding.</p>
 */
public class ItemInkwell extends Item implements IScribeTools {

    /** Original max damage 100 ({@code ItemInkwell} constructor). */
    public static final int MAX_INK = 100;

    public ItemInkwell(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final Player player = context.getPlayer();
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockTable table)) {
            return InteractionResult.PASS;
        }
        final int md = table.variant(state);
        if (md == 6 || !(level.getBlockEntity(pos) instanceof TileTable)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }

        for (final Direction dir : TileResearchTable.RESEARCH_DIRECTIONS) {
            final BlockPos adj = pos.relative(dir);
            final BlockState adjState = level.getBlockState(adj);
            if (!(adjState.getBlock() instanceof BlockTable adjTable)
                    || adjTable.variant(adjState) >= 6
                    || !(level.getBlockEntity(adj) instanceof TileTable)) {
                continue;
            }
            final int baseVariant = TileResearchTable.baseVariant(dir);
            final int topVariant = TileResearchTable.topVariant(dir);

            // Original setMeta + setTileEntity(new TileResearchTable()): the
            // variant change must also swap the block entity, so drop the old
            // tiles first — setBlock then creates the factory tile for the
            // new variant (the top keeps its plain TileTable).
            level.removeBlockEntity(pos);
            level.removeBlockEntity(adj);
            level.setBlockAndUpdate(pos, table.withVariant(baseVariant));
            level.setBlockAndUpdate(adj, adjTable.withVariant(topVariant));

            final BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileResearchTable researchTable) {
                researchTable.setItem(0, stack.copy());
                if (player != null && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                researchTable.setChanged();
            }
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            level.sendBlockUpdated(adj, level.getBlockState(adj), level.getBlockState(adj), 3);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
