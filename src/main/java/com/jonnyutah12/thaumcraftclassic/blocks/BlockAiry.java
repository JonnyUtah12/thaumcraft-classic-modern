package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * Airy devices (content-mapping.csv {@code thaumcraft:blockairy}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockAiry} — invisible/aura
 * nodes and their neighbours, one block id, metadata 0-12. Meta map per
 * docs/catalog-triage.md: 0 vis node (TileNode, TC-M5-01), 1 nitor
 * (TileNitor, this card), 4 warding stone fence (TC-M6-04d), 5 energized
 * node (TC-M5-02); the rest have no tile. Physical properties are the
 * original's per-meta tables: hardness 2.0 on nodes, 100 on metas 10-11,
 * unbreakable on meta 12; light 15 on
 * metas 1-3, 8 on 0/5/10/11; 0.4-cube outline except metas 3, 4, 10-12;
 * collision only on metas 4 and 12; replaceable on 2, 3, 4, 10, 11; never
 * opaque, never side-solid. Known differences: explosion resistance is a
 * block-wide property in 1.21.1 (the original's per-meta 200/50/unbreakable
 * collapses to the common 200), the original's sound volume factor is not
 * expressible on {@code Properties.sound}, and the destroy-progress impulse
 * bonus ({@code hasImpulseWhenSoundSlow}) has no 1.21.1 equivalent.</p>
 *
 * <p>Deferred: the meta-1 nitor drop targets itemResource, owned by
 * TC-M6-04a — wired once that item's id convention lands (noted in the
 * handoff). Meta 4's conditional fence collision (only when the block below
 * is not a warding stone) is approximated by the plain full-box collision.</p>
 */
public class BlockAiry extends BlockDeviceBase {

    public static final int MAX_VARIANT = 12;

    public static final int VARIANT_NODE = 0;
    public static final int VARIANT_NITOR = 1;
    public static final int VARIANT_WARDING_FENCE = 4;
    public static final int VARIANT_NODE_ENERGIZED = 5;

    private static final VoxelShape OUTLINE = box(4.8, 4.8, 4.8, 11.2, 11.2, 11.2);
    private static final VoxelShape EMPTY = Shapes.empty();
    private static final VoxelShape FULL = Shapes.block();

    private static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, MAX_VARIANT);

    public BlockAiry() {
        super(VARIANT, BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .noOcclusion()
                .explosionResistance(200.0F)
                .sound(SoundType.WOOL)
                .lightLevel(state -> switch (state.getValue(VARIANT)) {
                    case 1, 2, 3 -> 15;
                    case 0, 5, 10, 11 -> 8;
                    default -> 0;
                }));
    }

    /** The {@code variant} property this block carries; the base cannot add it
     * (its field is unset while the superclass constructor runs). */
    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    /**
     * Original getBlockHardness per meta (2.0 / 100 / unbreakable). The
     * original's impulse bonus ({@code hasImpulseWhenSoundSlow}) has no
     * 1.21.1 equivalent, so the progress is the plain hardness curve —
     * recorded as a known difference.
     */
    @Override
    public float getDestroyProgress(final BlockState state, final net.minecraft.world.entity.player.Player player,
            final BlockGetter level, final BlockPos pos) {
        final int variant = variant(state);
        if (variant == 12) {
            return 0.0F;
        }
        final float hardness = variant == 10 || variant == 11 ? 100.0F : 2.0F;
        final float speed = player.getDestroySpeed(state);
        return speed > 1.0F ? speed / hardness / 30.0F : speed / hardness / 100.0F;
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
            final CollisionContext context) {
        return switch (variant(state)) {
            case 3, 4, 10, 11, 12 -> EMPTY;
            default -> OUTLINE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
            final CollisionContext context) {
        return switch (variant(state)) {
            case VARIANT_WARDING_FENCE, 12 -> FULL;
            default -> EMPTY;
        };
    }

    /** Original isReplaceable: metas 2, 3, 4, 10, 11. */
    @Override
    public boolean canBeReplaced(final BlockState state, final BlockPlaceContext context) {
        return switch (variant(state)) {
            case 2, 3, VARIANT_WARDING_FENCE, 10, 11 -> true;
            default -> false;
        };
    }
}
