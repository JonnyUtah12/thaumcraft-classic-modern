package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

/**
 * Config for {@link TcOreFeature}: the replaceable host rocks, the possible
 * ore states, and the vein size.
 *
 * <p>This mirrors what the original passed to {@code WorldGenMinable} /
 * {@code setBlock} per attempt (ThaumcraftWorldGenerator.generateOres): a
 * target rock predicate (stone — the only host rock that existed in 1.7.10;
 * the port adds deepslate, its 1.21.1 equivalent below the stone/deepslate
 * transition, so attempt frequency stays the original), a vein size
 * (1 for cinnabar/amber single-block attempts, 6 for infused stone), and the
 * state to place. TC's blockCustomOre carried its ore kind in metadata; the
 * port's {@code variant} property values are chosen per vein by
 * {@link TcOreFeature}, matching the original per-vein
 * {@code random.nextInt(6) + 1}.</p>
 */
public class TcOreConfiguration implements FeatureConfiguration {

    public static final Codec<TcOreConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(RuleTest.CODEC).fieldOf("rocks").forGetter(config -> config.rocks),
            Codec.list(BlockState.CODEC).fieldOf("states").forGetter(config -> config.states),
            Codec.intRange(1, 64).fieldOf("size").forGetter(config -> config.size)
    ).apply(instance, TcOreConfiguration::new));

    public final List<RuleTest> rocks;
    public final List<BlockState> states;
    public final int size;

    public TcOreConfiguration(final List<RuleTest> rocks, final List<BlockState> states, final int size) {
        this.rocks = List.copyOf(rocks);
        this.states = List.copyOf(states);
        this.size = size;
    }
}
