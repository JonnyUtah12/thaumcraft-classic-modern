package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.jonnyutah12.thaumcraftclassic.tiles.TileAlchemyFurnaceAdvanced;

/**
 * Advanced alchemical furnace (content-mapping.csv {@code thaumcraft:blockalchemyfurnace}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockAlchemyFurnace} — the
 * advanced alchemical furnace frame: meta 0 is the core (TileAlchemyFurnaceAdvanced,
 * processes thrown items), meta 1 the essentia nozzle (TileAlchemyFurnaceAdvancedNozzle),
 * metas 2-4 the static frame parts. Physical properties are the original's:
 * metal material, hardness 3.0, resistance 17.0, metal step sound.</p>
 *
 * <p>Drops per the original: the core drops the basic alchemical furnace item
 * (blockStoneDevice meta 0 — the advanced furnace is an upgrade frame around
 * the basic one), and the frame parts drop the matching blockMetalDevice
 * items. The heat-driven dynamic light (up to 12) is TE-driven and deferred.</p>
 */
public class BlockAlchemyFurnace extends BlockDeviceBase {

    public static final int MAX_VARIANT = 4;

    public static final int VARIANT_CORE = 0;
    public static final int VARIANT_NOZZLE = 1;

    private static final VoxelShape FULL_SHAPE = Shapes.block();

    public BlockAlchemyFurnace() {
        super(MAX_VARIANT, BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F, 17.0F)
                .sound(SoundType.METAL));
    }

    @Override
    public VoxelShape getShape(final BlockState state, final net.minecraft.world.level.BlockGetter level,
            final BlockPos pos, final CollisionContext context) {
        return FULL_SHAPE;
    }

    /** Original {@code onEntityCollidedWithBlock} on meta 0: feed items to the furnace. */
    @Override
    public void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (variant(state) != VARIANT_CORE || level.isClientSide || !(entity instanceof ItemEntity item)) {
            return;
        }
        final BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof TileAlchemyFurnaceAdvanced furnace) {
            final ItemStack stack = item.getItem().copy();
            if (furnace.process(stack)) {
                item.getItem().shrink(1);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.2F, 1.0F + level.random.nextFloat() * 0.4F);
                if (item.getItem().isEmpty()) {
                    item.discard();
                }
            }
        }
    }

    /** Original {@code getItemDropped}: core → basic alchemical furnace item,
     *  frame parts → the matching metal-device items. */
    @Override
    public void getDrops(final BlockState state, final ServerLevel level, final BlockPos pos,
            final LootContext params) {
        final int variant = variant(state);
        if (variant == VARIANT_CORE) {
            final Item stoneDevice = BuiltInRegistries.ITEM
                    .get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "blockstonedevice"));
            if (stoneDevice != net.minecraft.world.item.Items.AIR) {
                // The block item's default state is variant 0 — the basic
                // alchemical furnace, matching the original's damageDropped.
                popResource(level, pos, new ItemStack(stoneDevice));
                return;
            }
        } else if (variant >= 1) {
            final Item metalDevice = BuiltInRegistries.ITEM
                    .get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "blockmetaldevice"));
            if (metalDevice != net.minecraft.world.item.Items.AIR) {
                popResource(level, pos, new ItemStack(metalDevice));
                return;
            }
        }
        super.getDrops(state, level, pos, params);
    }
}
