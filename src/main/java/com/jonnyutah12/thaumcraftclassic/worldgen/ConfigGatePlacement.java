package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.jonnyutah12.thaumcraftclassic.config.ConfigOption;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Config gate for TC world features (TC-M5-05): passes the position through
 * only while the named original config option is enabled.
 *
 * <p>The original checked {@code Config.genCinnibar} / {@code genAmber} /
 * {@code genInfusedStone} before each per-chunk attempt loop
 * (ThaumcraftWorldGenerator.generateOres L518/530/542). Placing the check as
 * the first placement modifier reproduces that: with the option off the
 * position stream is empty and the feature never runs, and the option is read
 * live per chunk, so toggling it takes effect on the next generated chunk —
 * same as the original's per-attempt read.</p>
 */
public class ConfigGatePlacement extends PlacementModifier {

    public static final MapCodec<ConfigGatePlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.STRING.fieldOf("option").forGetter(placement -> placement.optionPath)
    ).apply(instance, ConfigGatePlacement::new));

    private final String optionPath;
    private final Optional<ConfigOption> option;

    public ConfigGatePlacement(final String optionPath) {
        this.optionPath = optionPath;
        // Resolve against the live spec now (datapack load): a typo in the
        // JSON fails at pack load, not silently at generation time.
        this.option = TcConfig.fields().keySet().stream()
                .filter(candidate -> candidate.path().equals(optionPath))
                .findFirst();
        if (option.isEmpty()) {
            throw new IllegalArgumentException(
                    "worldgen config gate references unknown config option: " + optionPath);
        }
    }

    @Override
    public Stream<BlockPos> getPositions(final PlacementContext context, final RandomSource random,
            final BlockPos pos) {
        return TcConfig.getBool(option.get()) ? Stream.of(pos) : Stream.empty();
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModWorldgen.CONFIG_GATE.get();
    }
}
