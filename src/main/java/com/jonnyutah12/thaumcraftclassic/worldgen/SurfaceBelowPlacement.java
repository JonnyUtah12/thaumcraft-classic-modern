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
 * Surface-relative Y band below terrain: Y = surfaceHeight - [min_offset,
 * max_offset] (TC-M5-05).
 *
 * <p>Original amber rule (ThaumcraftWorldGenerator.generateOres L533):
 * {@code randPosY = world.getHeightValue(x, z) - random.nextInt(25)} — 0..24
 * blocks under the build-limit heightmap. {@link Heightmap.Types#WORLD_SURFACE}
 * is the target-platform equivalent of 1.7.10's heightMap (first air above
 * terrain), read during the decoration stage exactly as the original read it
 * during chunk generation.</p>
 */
public class SurfaceBelowPlacement extends PlacementModifier {

    public static final MapCodec<SurfaceBelowPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 512).fieldOf("min_offset").forGetter(placement -> placement.minOffset),
            Codec.intRange(0, 512).fieldOf("max_offset").forGetter(placement -> placement.maxOffset)
    ).apply(instance, SurfaceBelowPlacement::new));

    private final int minOffset;
    private final int maxOffset;

    public SurfaceBelowPlacement(final int minOffset, final int maxOffset) {
        this.minOffset = minOffset;
        this.maxOffset = maxOffset;
    }

    @Override
    public Stream<BlockPos> getPositions(final PlacementContext context, final RandomSource random,
            final BlockPos pos) {
        final int surfaceHeight = context.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        final int roll = random.nextInt(maxOffset - minOffset + 1);
        return Stream.of(pos.atY(TcWorldGenMath.surfaceBelowY(surfaceHeight, minOffset, maxOffset, roll)));
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModWorldgen.SURFACE_BELOW.get();
    }
}
