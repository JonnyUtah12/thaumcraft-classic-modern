package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

/**
 * TC ore feature (TC-M5-05): the target-platform equivalent of the original
 * {@code WorldGenMinable} calls in
 * {@code ThaumcraftWorldGenerator.generateOres}.
 *
 * <p>The difference from the vanilla {@code minecraft:ore} feature is ore-kind
 * selection: the original chose the blockCustomOre metadata once per vein
 * ({@code random.nextInt(6) + 1} for infused stone), producing single-kind
 * veins. Vanilla ore features pick the target per block, which with identical
 * host predicates would always place the first state. This feature picks one
 * state uniformly per vein — exactly the original distribution — then defers
 * to the vanilla vein algorithm with that state narrowed against every host
 * rock.</p>
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

        final List<OreConfiguration.TargetBlockState> narrowed = new ArrayList<>(config.rocks.size());
        for (final RuleTest rock : config.rocks) {
            narrowed.add(OreConfiguration.target(rock, state));
        }
        final OreConfiguration vein = new OreConfiguration(narrowed, config.size);

        return VANILLA_VEIN.place(new FeaturePlaceContext<>(
                Optional.empty(), context.level(), context.chunkGenerator(), context.random(), context.origin(), vein));
    }
}
