package com.jonnyutah12.thaumcraftclassic.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Item shown sinking through an item grate (content-mapping.csv
 * {@code thaumcraft:specialitemgrate}).
 *
 * <p>Original: {@code thaumcraft.common.entities.EntityItemGrate} — an
 * ItemEntity whose only override is canEntityStandOn: the open/closed item
 * grate (blockMetalDevice variants 5/6) is treated as non-solid so the item
 * drifts down through it. The grate tile (TC-M6-04d) spawns these; the
 * block itself is TC-M6-04c's blockMetalDevice, reached by id.</p>
 */
public class EntityItemGrate extends ItemEntity {

    public EntityItemGrate(final EntityType<? extends ItemEntity> type, final Level level) {
        super(type, level);
    }

    public EntityItemGrate(final Level level, final double x, final double y, final double z, final ItemStack stack) {
        super(level, x, y, z, stack);
    }

    @Override
    public boolean canEntityStandOn(final net.minecraft.world.level.block.state.BlockState state) {
        final net.minecraft.world.level.block.Block metalDevice =
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "blockmetaldevice"));
        if (state.getBlock() == metalDevice
                && state.getBlock() instanceof com.jonnyutah12.thaumcraftclassic.blocks.VariantDevice device) {
            final int variant = device.variant(state);
            if (variant == 5 || variant == 6) {
                return true;
            }
        }
        return super.canEntityStandOn(state);
    }

    /** Helper used by the tile: build one from a position + stack. */
    public static EntityItemGrate of(final Level level, final BlockPos pos, final ItemStack stack) {
        return new EntityItemGrate(level, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, stack.copy());
    }
}
