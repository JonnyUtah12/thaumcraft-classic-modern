package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneWorkbench;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * Arcane workbench block (content-mapping.csv {@code thaumcraft:tilearcaneworkbench}).
 *
 * <p>Original: there is no block class in 4.2.3.5 — {@code ConfigBlocks.java:370}
 * registers {@code TileArcaneWorkbench} as a tile entity and the tile itself is
 * the block. 1.21.1 binds block entities to blocks, so this class is the port's
 * host: it keeps the csv row's id ({@code thaumcraft:tilearcaneworkbench}) and
 * creates the workbench's {@link TileArcaneWorkbench} entity. Material values are
 * the port's (the original tile declared no properties): wood-strength bench.</p>
 */
public class BlockArcaneWorkbench extends Block implements EntityBlock {

    public BlockArcaneWorkbench() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0F)
                .sound(net.minecraft.world.level.block.SoundType.WOOD));
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new TileArcaneWorkbench(pos, state);
    }

    /**
     * {@code BlockArcaneWorkbench.func_149727_b} — the original's
     * {@code onBlockActivated} opened {@code ContainerArcaneWorkbench} and returned
     * true; the sneak-with-a-wand branch is vanilla's own, which reaches this method
     * only when the player is not sneaking.
     *
     * <p>TC-M7-04-m2 replaces TC-M4-03-s's interim GUI-less stand-in (a plain
     * {@code craft(player)} on the use interaction) with the original's menu open.
     * The craft cycle now runs from {@code ArcaneWorkbenchMenu.broadcastChanges} —
     * the 1.7.10 {@code updateContainer} seam — while the menu is open, and stops
     * when it closes, exactly as in 1.7.10.</p>
     */
    @Override
    public net.minecraft.world.InteractionResult useWithoutItem(final BlockState state, final Level level,
            final BlockPos pos, final Player player, final BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MenuProvider provider)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, pos);
        }
        return net.minecraft.world.InteractionResult.CONSUME;
    }
}
