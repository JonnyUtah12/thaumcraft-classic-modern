package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Uniform Y band from an absolute floor up to a surface-relative cap
 * (TC-M5-05): Y in [min, max(floor, surfaceHeight + surface_offset)).
 *
 * <p>Original infused-stone rule (ThaumcraftWorldGenerator.generateOres
 * L545): {@code randPosY = random.nextInt(Math.max(5,
 * world.getHeightValue(x, z) - 5))} — uniform from the world bottom up to
 * five blocks under the surface, with a floor of 5 so the band never
 * collapses on flat ground. No vanilla placement expresses this
 * bottom-anchored, surface-capped band, hence this modifier.</p>
 */
public class SurfaceCappedUniformPlacement extends PlacementModifier {

    public static final MapCodec<SurfaceCappedUniformPlacement> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("min").forGetter(placement -> placement.min),
                    Codec.INT.fieldOf("floor").forGetter(placement -> placement.floor),
                    Codec.INT.fieldOf("surface_offset").forGetter(placement -> placement.surfaceOffset)
            ).apply(instance, SurfaceCappedUniformPlacement::new));

    private final int min;
    private final int floor;
    private final int surfaceOffset;

    public SurfaceCappedUniformPlacement(final int min, final int floor, final int surfaceOffset) {
        this.min = min;
        this.floor = floor;
        this.surfaceOffset = surfaceOffset;
    }

    @Override
    public Stream<BlockPos> getPositions(final PlacementContext context, final RandomSource random,
            final BlockPos pos) {
        final int surfaceHeight = context.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        final int bound = TcWorldGenMath.surfaceCappedBound(floor, surfaceOffset, surfaceHeight);
        if (bound <= min) {
            return Stream.empty();
        }
        return Stream.of(pos.atY(min + random.nextInt(bound - min)));
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModWorldgen.SURFACE_CAPPED_UNIFORM.get();
    }
}
