package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogOuterLands;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchPortal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.Nullable;

/**
 * The Eldritch Portal block (content-mapping.csv
 * {@code thaumcraft:blockportaleldritch}; the original
 * {@code thaumcraft.common.blocks.BlockEldritchPortal}).
 *
 * <p>The original's numbers, ported verbatim: hardness {@code -1}
 * (unbreakable, {@code getHardness = -1}) with blast resistance 200 000
 * ({@code getExplosionResistance}), full bright ({@code getLightValue = 15}),
 * no selection box ({@code getSelectedBoundingBox = null}) and no collision
 * ({@code getCollisionBoundingBox = zero box}, {@code isSideSolid = false},
 * {@code isReplaceable = false}, leaves cannot replace it), pushes ignored
 * ({@code airyMaterial}), and it drops nothing
 * ({@code getItemDropped = Item(0) = air}) so the block carries
 * {@code noLootTable()}.</p>
 *
 * <p>The blank icon ({@code thaumcraft:blank}) and the open/close animation
 * ({@code opencount}) belong to the portal renderer — TC-M7-02. The
 * neighbour check ({@code onNeighborChange} asks for
 * {@code blockEldritch} above and below, marks the render update) lives as
 * {@link TileEldritchPortal#hasEldritchFrame()} so the renderer and the tests
 * read the same fact.</p>
 */
public class BlockEldritchPortal extends Block implements EntityBlock {

    public BlockEldritchPortal() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.FIRE)
                .noCollission()
                .noOcclusion()
                .lightLevel(state -> 15)
                .strength(-1.0F, 200000.0F)
                .pushReaction(PushReaction.IGNORE)
                .noLootTable()
                .sound(SoundType.GLASS));
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new TileEldritchPortal(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final net.minecraft.world.level.Level level,
                                                                  final BlockState state,
                                                                  final BlockEntityType<T> type) {
        return type == CatalogOuterLands.TILE_ELDRITCH_PORTAL.get()
                ? (BlockEntityTicker<T>) TileEldritchPortal.SERVER_TICKER
                : null;
    }
}
