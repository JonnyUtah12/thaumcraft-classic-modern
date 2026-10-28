package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.blocks.VariantDevice;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.BlockTags;

import java.util.List;

/**
 * Warding stone (content-mapping.csv {@code thaumcraft:tilewardingstone},
 * hosted on blockCosmeticSolid variant 3 — TC-M6-04b's block).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileWardingStone}. While not
 * redstone-powered it nudges airborne non-player creatures along their facing
 * (the "warding" push), and every 100 ticks keeps the decorative warding
 * aura (blockAiry variant 3, TC-M6-04b) stacked two blocks above itself.
 * The aura block is reached by id so this card does not depend on the
 * sibling's holder class; if it is not registered the upkeep is skipped.</p>
 */
public class TileWardingStone extends BlockEntity implements com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.TickingDeviceTile {

    int count = 0;

    public TileWardingStone(final BlockPos pos, final BlockState state) {
        super(CatalogFurniture.TILE_WARDING_STONE.get(), pos, state);
    }

    public boolean gettingPower() {
        return this.level.hasNeighborSignal(this.worldPosition);
    }

    public void serverTick() {
        if (this.count == 0) {
            this.count = this.level.random.nextInt(100);
        }

        if (this.count % 5 == 0 && !this.gettingPower()) {
            // MC 1.21.1 AABB takes Vec3 corners (the old BlockPos pair overload is gone);
            // this covers the stone block plus the two blocks above it, then widened 0.1.
            final List<LivingEntity> targets = this.level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(Vec3.atLowerCornerOf(this.worldPosition),
                            Vec3.atLowerCornerOf(this.worldPosition.above(3))).inflate(0.1, 0.1, 0.1),
                    e -> !e.onGround() && !(e instanceof Player));
            for (final LivingEntity e : targets) {
                final float yaw = (e.getYRot() + 180.0F) * Mth.PI / 180.0F;
                e.setDeltaMovement(-Mth.sin(yaw) * 0.2F, -0.1, Mth.cos(yaw) * 0.2F);
            }
        }

        if (++this.count % 100 == 0) {
            maintainAura(this.worldPosition.above());
            maintainAura(this.worldPosition.above(2));
        }
    }

    private void maintainAura(final BlockPos pos) {
        final BlockState current = this.level.getBlockState(pos);
        if (isAura(current)) {
            return;
        }
        final net.minecraft.world.level.block.Block aura = auraBlock();
        // 1.21.1 removed Block#isReplaceable(level, pos); the REPLACEABLE block tag
        // (BlockTags) is its replacement for "may be overwritten here".
        if (aura == Blocks.AIR || !current.is(BlockTags.REPLACEABLE)) {
            return;
        }
        this.level.setBlock(pos, aura.defaultBlockState(), 3);
    }

    private boolean isAura(final BlockState state) {
        final net.minecraft.world.level.block.Block aura = auraBlock();
        return state.getBlock() == aura
                && aura instanceof VariantDevice device && device.variant(state) == 3;
    }

    private static net.minecraft.world.level.block.Block auraBlock() {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(
                        com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "blockairy"));
    }
}
