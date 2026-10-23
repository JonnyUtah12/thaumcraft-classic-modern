package com.jonnyutah12.thaumcraftclassic.worldgen;

/**
 * Pure integer math for the original Thaumcraft 4.2.3.5 ore Y-bands
 * (TC-M5-05). Transcribed from {@code ThaumcraftWorldGenerator.generateOres}
 * (common/lib/world/ThaumcraftWorldGenerator.java L515-565) with no
 * Minecraft types, so the bands are unit-testable without the game classpath
 * (docs/testing.md: seeded tests for random processes).
 *
 * <p>The placement modifiers in this package consume these bands; the
 * configured/placed feature JSONs pin the same numbers as data. The parity
 * unit test (WorldGenDataParityTest) guards all three against drift.</p>
 */
public final class TcWorldGenMath {

    /**
     * Original build height (1.7.10 {@code World.getHeight()} = 256). The
     * original cinnabar band is {@code random.nextInt(world.getHeight() / 5)}
     * — absolute Y 0..50. The port keeps that absolute band rather than
     * height/5 of the 1.21.1 height (384), which would shift the ore below
     * its original position relative to sea level (docs/architecture/worldgen.md).
     */
    public static final int ORIGINAL_WORLD_HEIGHT = 256;

    /** Exclusive upper Y bound for cinnabar: original {@code height / 5}. */
    public static int cinnabarYBound(final int worldHeight) {
        return worldHeight / 5;
    }

    /**
     * Amber band: original {@code world.getHeightValue(x, z) - random.nextInt(25)}
     * — Y in [surfaceHeight - maxOffset, surfaceHeight - minOffset] inclusive,
     * where surfaceHeight is the build-limit heightmap value (first air above
     * terrain), exactly the value the original read.
     */
    public static int surfaceBelowY(final int surfaceHeight, final int minOffset,
            final int maxOffset, final int offsetRoll) {
        return surfaceHeight - (minOffset + offsetRoll);
    }

    /**
     * Infused-stone band: original {@code random.nextInt(Math.max(5,
     * world.getHeightValue(x, z) - 5))} — exclusive upper bound
     * {@code max(floor, surfaceHeight + surfaceOffset)}.
     */
    public static int surfaceCappedBound(final int floor, final int surfaceOffset,
            final int surfaceHeight) {
        return Math.max(floor, surfaceHeight + surfaceOffset);
    }

    private TcWorldGenMath() {
    }
}
