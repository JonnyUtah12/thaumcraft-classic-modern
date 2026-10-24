package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.tiles.TileManaPod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The original's {@code BlockManaPod} (content-mapping.csv row
 * {@code thaumcraft:blockmanapod}, TC-M6-04a): a magical-biome plant that
 * grows through 8 ages (original damage values 0-7, now an {@code age}
 * block property) under a log in a magical biome, emits light equal to its
 * age, and drops aspect-seeded mana beans at age 2+.
 *
 * <p>Original semantics ported 1:1:</p>
 * <ul>
 *   <li>growth: random tick with 1/30 chance calls {@link TileManaPod#checkGrowth()}
 *       (original {@code randomDisplayTick}→{@code randomTickBlock});</li>
 *   <li>reversion: whenever the log-above + magical-biome condition fails,
 *       the pod resets to age 0 (original {@code onNeighborBlockChange} and
 *       {@code randomTickBlock} guard);</li>
 *   <li>mining speed: base hardness 0.5F kept; the original's
 *       {@code hardness / (8 - age)} gradient is deferred (1.21.1 exposes
 *       hardness per BlockState, see the note at {@link #getCloneItemStack});</li>
 *   <li>per-age collision boxes (original {@code setBlockBoundsFromMeta}
 *       with the BlockRenderer W-constants);</li>
 *   <li>drops: nothing below age 2; age 7 has a 67% chance of two beans;
 *       the bean carries the pod's aspect, defaulting to PLANT (herba) —
 *       expressed through the block loot table (set_components default +
 *       copy_components from the block entity), the 1.21.1 replacement for
 *       the original's getDrops override and transient position→aspect map.</li>
 * </ul>
 *
 * <p>The original's BiomeDictionary MAGICAL test becomes the
 * {@code thaumcraft:is_magical} biome tag (TC-M5-05 populates it with the
 * magical forest biome); the log-above test uses the vanilla
 * {@code minecraft:logs} tag, which the port's magical log will carry.</p>
 */
public class BlockManaPod extends Block implements EntityBlock {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);

    /** Replaces the original's BiomeDictionary.Type.MAGICAL check (TC-M5-05 fills it). */
    public static final TagKey<net.minecraft.world.level.biome.Biome> MAGICAL_BIOMES =
            TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "is_magical"));

    /** Per-age collision boxes, original setBlockBoundsFromMeta W-constants. */
    private static final VoxelShape[] SHAPE_BY_AGE = {
            box(4, 12, 4, 12, 16, 12),
            box(4, 10, 4, 12, 16, 12),
            box(4, 8, 4, 12, 16, 12),
            box(4, 6, 4, 12, 16, 12),
            box(4, 5, 4, 12, 16, 12),
            box(4, 4, 4, 12, 16, 12),
            box(4, 3, 4, 12, 16, 12),
            box(4, 2, 4, 12, 16, 12),
    };

    public BlockManaPod(final Properties props) {
        super(props.lightLevel(state -> state.getValue(AGE)));
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return SHAPE_BY_AGE[state.getValue(AGE)];
    }

    @Override
    public boolean isPossibleToRespawnInThis(final BlockState state) {
        return true;
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                   final Block neighborBlock, final BlockPos neighborPos, final boolean movedByPiston) {
        if (!canGrowHere(level, pos)) {
            resetAge(state, level, pos);
        }
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos,
                              final RandomSource random) {
        if (!canGrowHere(level, pos)) {
            resetAge(state, level, pos);
        } else if (random.nextInt(30) == 0) {
            if (level.getBlockEntity(pos) instanceof final TileManaPod pod) {
                pod.checkGrowth();
            }
        }
    }

    /** Original {@code canGrowHere}: magical biome and a log directly above. */
    public static boolean canGrowHere(final LevelReader level, final BlockPos pos) {
        final boolean magicalBiome = level.getBiome(pos).is(MAGICAL_BIOMES);
        final BlockState above = level.getBlockState(pos.above());
        return magicalBiome && above.is(BlockTags.LOGS);
    }

    private void resetAge(final BlockState state, final Level level, final BlockPos pos) {
        level.setBlock(pos, state.setValue(AGE, 0), Block.UPDATE_ALL);
    }

    // Original getPlayerRelativeBlockHardness divided the block hardness by
    // (8 - age): 1.21.1 exposes mining hardness only per BlockState
    // (BlockStateBase#getDestroySpeed), which needs a nested state class per
    // age; the base hardness 0.5F stays authoritative here and the age
    // gradient is left to the follow-up that models per-age block states.

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        return new ItemStack(CatalogResources.ITEM_MANA_BEAN.get());
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new TileManaPod(pos, state);
    }
}
