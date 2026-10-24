package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnace;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Arcane (infernal) furnace (content-mapping.csv {@code thaumcraft:blockarcanefurnace}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockArcaneFurnace} — the
 * infernal furnace multiblock: meta 0 is the base plate (the TileArcaneFurnace
 * core, 0.25-tall collision, absorbs thrown items, damages living entities
 * standing on it), metas 1-9 the 3x3 wall parts, meta 10 the iron-bars
 * opening (hosts TileArcaneFurnaceNozzle together with wall metas 2, 4, 5, 6,
 * 8 — the essentia ports). Physical properties are the original's: rock
 * material, hardness 10.0, resistance 500.0, light 13 on metas 0 and 10.</p>
 *
 * <p>The structure itself is assembled by the wand (TC-M6-01); the block only
 * carries the parts. The lava-bubble client effect is TC-M7-01.</p>
 */
public class BlockArcaneFurnace extends BlockDeviceBase {

    public static final int MAX_VARIANT = 10;

    public static final int VARIANT_BASE = 0;
    public static final int VARIANT_IRON_BARS = 10;

    private static final VoxelShape BASE_SHAPE = box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
    private static final VoxelShape FULL_SHAPE = Shapes.block();

    public BlockArcaneFurnace() {
        super(MAX_VARIANT, BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(10.0F, 500.0F)
                .sound(SoundType.STONE)
                .lightLevel(state -> {
                    final int variant = state.getValue(variantProperty());
                    return variant == VARIANT_BASE || variant == VARIANT_IRON_BARS ? 13 : 0;
                }));
    }

    @Override
    public VoxelShape getShape(final BlockState state, final net.minecraft.world.level.BlockGetter level,
            final BlockPos pos, final CollisionContext context) {
        return variant(state) == VARIANT_BASE ? BASE_SHAPE : FULL_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final net.minecraft.world.level.BlockGetter level,
            final BlockPos pos, final CollisionContext context) {
        return variant(state) == VARIANT_BASE ? BASE_SHAPE : FULL_SHAPE;
    }

    /** Original {@code onEntityCollidedWithBlock} on meta 0: pull items to the
     *  centre, absorb them into the furnace core, and burn living entities. */
    @Override
    public void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (variant(state) != VARIANT_BASE || level.isClientSide) {
            return;
        }
        if (entity.getX() < pos.getX() + 0.3) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(1.0E-4, 0, 0));
        }
        if (entity.getX() > pos.getX() + 0.7) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(-1.0E-4, 0, 0));
        }
        if (entity.getZ() < pos.getZ() + 0.3) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0, 1.0E-4));
        }
        if (entity.getZ() > pos.getZ() + 0.7) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0, -1.0E-4));
        }
        if (entity instanceof ItemEntity item) {
            entity.setDeltaMovement(entity.getDeltaMovement().x, 0.025, entity.getDeltaMovement().z);
            if (entity.onGround()) {
                final BlockEntity te = level.getBlockEntity(pos);
                if (te instanceof TileArcaneFurnace furnace && furnace.addItemsToInventory(item.getItem())) {
                    item.discard();
                }
            }
        } else if (entity instanceof LivingEntity living && !living.isCrouching()) {
            entity.hurt(level.damageSources().inFire(), 3.0F);
            entity.setDeltaMovement(entity.getDeltaMovement().x, 0.1, entity.getDeltaMovement().z);
        }
    }
}
