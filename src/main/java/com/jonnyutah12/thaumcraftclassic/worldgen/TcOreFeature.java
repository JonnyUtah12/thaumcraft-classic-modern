package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

/**
 * TC ore feature (TC-M5-05): the target-platform equivalent of the original
 * {@code generateOres} calls in {@code ThaumcraftWorldGenerator}.
 *
 * <p>Two placement paths, matching the original's two shapes:</p>
 * <ul>
 *   <li>{@code size == 1} — cinnabar and amber were single-block
 *       {@code world.setBlock} attempts guarded by
 *       {@code isReplaceableOreGen(stone)}; they are reproduced exactly here.
 *       (The vanilla vein algorithm degenerates at size 1 and places nothing,
 *       so delegating would silently drop both ores.)</li>
 *   <li>{@code size > 1} — infused stone used {@code WorldGenMinable(size 6)};
 *       the modern equivalent is the vanilla {@code minecraft:ore} vein
 *       algorithm, reused via {@link OreFeature}.</li>
 * </ul>
 *
 * <p>The difference from the plain {@code minecraft:ore} feature is ore-kind
 * selection: the original chose the blockCustomOre metadata once per vein
 * ({@code random.nextInt(6) + 1} for infused stone), producing single-kind
 * veins. Vanilla ore features pick the target per block, which with identical
 * host predicates would always place the first state. This feature picks one
 * state uniformly per vein/attempt — exactly the original distribution — then
 * defers to the vanilla vein algorithm with that state narrowed against every
 * host rock.</p>
 */
public class TcOreFeature extends Feature<TcOreConfiguration> {

    /** Stateless vanilla vein placer, reused for the ellipsoid algorithm. */
    private static final OreFeature VANILLA_VEIN = new OreFeature(OreConfiguration.CODEC);

    public TcOreFeature(final Codec<TcOreConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<TcOreConfiguration> context) {
        final TcOreConfiguration config = context.config();
        final BlockState state = config.states.get(context.random().nextInt(config.states.size()));

        if (config.size == 1) {
            return placeSingleAttempt(context, state);
        }

        final List<OreConfiguration.TargetBlockState> narrowed = new ArrayList<>(config.rocks.size());
        for (final RuleTest rock : config.rocks) {
            narrowed.add(OreConfiguration.target(rock, state));
        }
        final OreConfiguration vein = new OreConfiguration(narrowed, config.size);

        return VANILLA_VEIN.place(new FeaturePlaceContext<>(
                Optional.empty(), context.level(), context.chunkGenerator(), context.random(), context.origin(), vein));
    }

    /**
     * The original's single-block attempt: check the host block against the
     * rock predicates, replace in place. Written through the same
     * {@link BulkSectionAccess} path the vanilla vein algorithm uses, so
     * generation writes behave identically for both shapes.
     */
    private static boolean placeSingleAttempt(final FeaturePlaceContext<TcOreConfiguration> context,
            final BlockState state) {
        final BlockPos origin = context.origin();
        final BlockState host = context.level().getBlockState(origin);
        for (final RuleTest rock : context.config().rocks) {
            if (rock.test(host, context.random())) {
                try (BulkSectionAccess bulk = new BulkSectionAccess(context.level())) {
                    final LevelChunkSection section = bulk.getSection(origin);
                    if (section != null) {
                        section.setBlockState(
                                SectionPos.sectionRelative(origin.getX()),
                                SectionPos.sectionRelative(origin.getY()),
                                SectionPos.sectionRelative(origin.getZ()),
                                state, false);
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }
}
