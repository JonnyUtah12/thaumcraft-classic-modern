package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEtherealBloom;

/**
 * The thaumic plant family (content-mapping.csv row
 * {@code thaumcraft:blockCustomPlant} → {@code thaumcraft:blockcustomplant},
 * original {@code thaumcraft.common.blocks.BlockCustomPlant}; TC-M5-04-s-b —
 * this is the cell the Ethereal Bloom lives in, the cleansing half of the
 * taint slice per D-012; design record docs/architecture/taint.md).
 *
 * <p>One id, six metas, verbatim (D-001), carried in a {@code variant}
 * property: 0 greatwood sapling, 1 silverwood sapling, 2 shimmerleaf,
 * 3 cinderpearl, <b>4 the Ethereal Bloom (purifier seed)</b>, 5 manashroom.
 * The original's properties, transcribed: {@code Material.plants} (hardness 0
 * → {@code instabreak}, and that material's step sound is the grass one),
 * {@code MapColor.plants}, non-opaque, bounds {@code (0.1, 0, 0.1)}–{@code
 * (0.9, 0.8, 0.9)} ({@code func_149647_a(0.1F, 0.0F, 0.1F, 0.9F, 0.8F,
 * 0.9F)}), drops itself for every meta ({@code func_149650_a} returns the
 * block's item with no fortune or quantity roll — the loot table), and the
 * light levels of {@code getLightValue}: metas 1, 2, 3 and 5 glow 8, the bloom
 * (4) glows 15, the greatwood sapling (0) does not glow.</p>
 *
 * <p>Placement: the original's {@code BlockCustomPlantItem.onItemUse} is the
 * 1.7.10 hand-written version of what 1.21.1's {@code Item#place} does — a
 * top-face click, a soil the plant type accepts, an air cell above, the block
 * placed with its meta, the block's own placement sound, one item consumed.
 * The soil rule is the plant-type mapping of {@code getPlantType} (meta 3 →
 * Desert, metas 4/5 → Cave, else Plains) and lands here as {@link
 * #canSustainAt}, reached from {@link #canSurvive} — 1.21.1 removed the
 * {@code Block#canSustainPlant} hook this mapping ships (the item-side hook is
 * NeoForge's {@code SpecialPlantable}), and the placement gate that survives is
 * {@code canSurvive}, which is exactly the original's soil test. The sound
 * formula the original spells out by hand ({@code volume = (hardness + 1) /
 * 2}, {@code pitch = size * 0.8}) is 1.7.10 vanilla's own formula for a placed
 * block, which 1.21.1 runs for every block item, so no custom item class is
 * needed. The variant round-trip (meta 4 in the stack → meta 4 in the world)
 * is the shared {@link VariantBlockItem#getPlacementState}.</p>
 *
 * <p><b>Meta 4, the bloom, is this card's slice</b>: {@link #newBlockEntity}
 * returns {@link TileEtherealBloom} for variant 4 only (the original's
 * {@code hasTileEntity(md == 4)} / {@code createTileEntity} pair), and the
 * tile's update runs the cleansing roll — the ±7 scan, the three-biome gate,
 * the squared-distance gate, and the restore of the column's biome to the one
 * the world generates there (see {@code TaintManager.etherealBloomCleanse}).
 * The bloom's 3×3×3 stalk-and-leaves shell is drawn from the tile by
 * {@code TileEtherealBloomRenderer} in the original and never places blocks,
 * so there is no world shape of it to port; drawing it is the TC-M5-07 asset
 * row.</p>
 *
 * <p>Deferred to the rows that own them: the sapling growth of metas 0/1
 * ({@code growGreatTree} / {@code growSilverTree}, gated on {@code light >= 9}
 * and a 1-in-25 / 1-in-50 roll — those need {@code WorldGenGreatwoodTrees} /
 * {@code WorldGenSilverwoodTrees}, rows {@code worldgen.greatwood_trees} and
 * {@code worldgen.silverwood_trees}), the shimmerleaf / cinderpearl /
 * manashroom crop growth and the manashroom's {@code entityInside} potion
 * effect (the plants row; this card refuses to guess which 1.7.10 {@code
 * Potion} field {@code field_76431_k} is), and every icon, model and particle
 * (TC-M5-07). Nothing here is a stub with an invented threshold: a deferred
 * rule is not implemented at all, and its numbers stay in the reference
 * tree.</p>
 */
public class BlockCustomPlant extends Block implements net.minecraft.world.level.block.EntityBlock {

    /** The original's six metadata values, verbatim (D-001). */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 5);

    /** Meta 4: the Ethereal Bloom, the only meta with a tile entity. */
    public static final int META_BLOOM = 4;
    /** Meta 3: cinderpearl — the original's Desert plant type. */
    public static final int META_CINDERPEARL = 3;
    /** Meta 5: manashroom — the original's Cave plant type, with the bloom. */
    public static final int META_MANASHROOM = 5;

    /** {@code getFlammability} of the original's plant family (100). */
    public static final int FLAMMABILITY = 100;
    /** {@code getFireSpreadSpeed} of the original's plant family (60). */
    public static final int FIRE_SPREAD_SPEED = 60;

    /** The original's block bounds, 0.1..0.9 on x/z and 0..0.8 on y. */
    private static final VoxelShape SHAPE = Shapes.box(1.6D, 0.0D, 1.6D, 14.4D, 12.8D, 14.4D);

    public BlockCustomPlant(final Properties props) {
        // Material.plants: hardness 0, and that material's step sound is the
        // grass one. The light levels ride per state (the original's
        // getLightValue is per-metadata).
        super(props.noOcclusion().instabreak().sound(SoundType.GRASS)
                .lightLevel(state -> lightFor(state.getValue(VARIANT))));
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0));
    }

    /**
     * The original's {@code getLightValue}: metas 1, 2, 3 and 5 → 8; the bloom
     * (4) → 15; the greatwood sapling (0) → the {@code Block} default (0).
     */
    public static int lightFor(final int meta) {
        if (meta == META_BLOOM) {
            return 15;
        }
        return meta == 1 || meta == 2 || meta == 3 || meta == 5 ? 8 : 0;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        // The original's hasTileEntity / createTileEntity pair: only meta 4
        // gets a tile, and the tile is the whole cleansing engine.
        return state.getValue(VARIANT) == META_BLOOM ? new TileEtherealBloom(pos, state) : null;
    }

    /**
     * The bloom's ticker, asked for on BOTH sides: the original's
     * {@code TileEntity.update()} runs on the client (the one-shot
     * {@code thaumcraft:roots} cue) and on the server (the cleansing roll), and
     * 1.21.1's default ticker is built from {@code state.hasBlockEntity()},
     * which is true for every variant of this family — so answering the question
     * per variant is what keeps the other five metas (and the tick list) inert.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            final net.minecraft.world.level.Level level, final BlockState state,
            final net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return state.getValue(VARIANT) == META_BLOOM
                ? (net.minecraft.world.level.block.entity.BlockEntityTicker<T>)
                        ((net.minecraft.world.level.block.entity.BlockEntityTicker<TileEtherealBloom>)
                                (l, p, s, be) -> be.tick())
                : null;
    }

    /**
     * The soil rule behind the original's {@code getPlantType} mapping, in the
     * shape this platform can express (1.21.1 has no per-variant plant-type
     * registry; the plant types are the soil sets below).
     *
     * <ul><li><b>Plains</b> (metas 0, 1, 2): the {@code #dirt} family — 1.7.10
     * Forge's {@code BlockDirt.canSustainPlant(Plains)} accepted every dirt
     * subtype, and 1.21.1's {@code minecraft:dirt} tag is that set (dirt,
     * coarse dirt, grass block, podzol, mycelium, moss block, dirt path,
     * rooted dirt).</li>
     * <li><b>Desert</b> (meta 3, cinderpearl): the {@code #sand} tag plus
     * terracotta (1.7.10's {@code hardened_clay}).</li>
     * <li><b>Cave</b> (meta 4 only, the purifier seed): mycelium and podzol —
     * Forge's {@code EnumPlantType.Cave} is the mushroom soil set
     * ({@code BlockMycelium.canSustainPlant} answers Cave, podzol is the
     * subtype that does the same). The purifier seed therefore plants on
     * mycelium and podzol, not on plain dirt — the original's rule, kept
     * exactly. {@code getPlantType} (BlockCustomPlant.java:88-95) special-cases
     * ONLY metas 3 and 4: the manashroom (5) and both saplings are
     * <b>Plains</b>, i.e. the dirt family, which is why meta 5 is not in the
     * Cave branch here.</li></ul>
     */
    public static boolean canSustainAt(final BlockGetter level, final BlockPos support, final int meta) {
        final BlockState below = level.getBlockState(support);
        if (meta == META_CINDERPEARL) {
            return below.is(BlockTags.SAND) || below.is(Blocks.TERRACOTTA);
        }
        if (meta == META_BLOOM) {
            return below.is(Blocks.MYCELIUM) || below.is(Blocks.PODZOL);
        }
        return below.is(BlockTags.DIRT);
    }

    /**
     * The original's fire behaviour for the whole family: {@code
     * getFlammability} 100 and {@code getFireSpreadSpeed} 60
     * (BlockCustomPlant.java:186-193), i.e. exactly the values 1.21.1 gives
     * hay/grass-family plants. Verbatim, all six metas.
     */
    @Override
    public int getFlammability(final BlockState state, final net.minecraft.world.level.BlockGetter level,
                               final BlockPos pos, final net.minecraft.core.Direction face) {
        return FLAMMABILITY;
    }

    @Override
    public int getFireSpreadSpeed(final BlockState state, final net.minecraft.world.level.BlockGetter level,
                                  final BlockPos pos, final net.minecraft.core.Direction face) {
        return FIRE_SPREAD_SPEED;
    }

    /**
     * The placement gate: the cell below must be soil the variant accepts.
     * {@code Item#place} routes the support question through
     * {@code canSurvive}, which is where the original's
     * {@code canSustainPlant(world, x, y, z, UP, plantable)} test survives in
     * this mapping.
     */
    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        return canSustainAt(level, pos.below(), state.getValue(VARIANT));
    }

    /** The original's plant is not walkable pathfinding-wise (Material.plants). */
    @Override
    protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return false;
    }
}
