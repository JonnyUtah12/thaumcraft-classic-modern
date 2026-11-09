package com.jonnyutah12.thaumcraftclassic.render.art;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockCustomOreItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The crystal cluster's world art as a data plan (BD-18, t_6563ce50, D-021).
 *
 * <p>1.7.10 built each cluster's look inside its tile-entity renderer,
 * {@code thaumcraft.client.renderers.tile.TileCrystalRenderer}: six tilted
 * cubes (the published {@code models/crystal} sprite, {@code :79}) whose
 * geometry is drawn from a {@link Random} seeded with the tile's own
 * placement — {@code new Random(metadata + xCoord + yCoord * zCoord)}
 * ({@code :80}) — so the same cluster at the same spot always draws the same
 * shards, and a cluster elsewhere differs. The plan reproduces the original's
 * exact draw order so the seed produces the original's shards:</p>
 *
 * <ul>
 *   <li>main shard ({@code :81-91}): yaw {@code (nextFloat()-nextFloat())*5},
 *   pitch the same (4 floats), then the draw consumes a shade pair
 *   ({@code nextInt(10)}, {@code nextFloat()}, {@code :43}) and three scale
 *   floats — x {@code (0.15+r*0.075)*1.1}, y {@code (0.5+r*0.1)*1.1},
 *   z {@code (0.15+r*0.05)*1.1} ({@code :56}), size 1.1 ({@code :90}).</li>
 *   <li>shards a = 1..5 ({@code :93-101}): yaw {@code nextInt(36)+72*a}
 *   ({@code :98}), pitch {@code 15+nextInt(15)} ({@code :99}), size 0.8
 *   ({@code :100}), then the same 1+1+3 consume.</li>
 *   <li>tint: {@code BlockCustomOreItem.colors[md + 1]}, except variant 6
 *   (mixed) which mains on {@code colors[5]} and cycles the shard tints
 *   {@code colors[a]}, {@code colors[6]} for a = 5 ({@code :74-76, :95}).
 *   The port carries that array verbatim as
 *   {@link BlockCustomOreItem#COLORS}.</li>
 *   <li>light: the draw lights {@code (int) (210F * shade)} ({@code :57}) with
 *   {@code shade} in {@code 0.925..1} ({@code :43}) — a per-frame pulse around
 *   a 210/255 floor; the plan exposes the floor (210 * 15 / 255 = 12).</li>
 *   <li>placement: the cluster sits on its supporting face —
 *   {@code translateFromOrientation} ({@code :20-39}) offsets the pivot by
 *   ±0.3/1.3 from the cell and rotates the model per orientation; the port's
 *   {@code TileCrystal.getOrientation()} carries that value.</li>
 * </ul>
 *
 * <p>Variant 7 (strange/eldritch) is a {@code TileEldritchCrystal} host with
 * its own renderer in the original ({@code TileEldritchCrystalRenderer}, over a
 * {@code TileEldritchCrystal} the port does not ship) and draws no cluster
 * here; the original indexes {@code colors[md + 1]} for it, which is out of
 * bounds on the seven-entry {@code colors} array — the port clamps and the
 * renderer declines the scene (recorded in BD-18). The plan is pure: same
 * inputs, same shard list.</p>
 */
public final class CrystalWorldArt {

    /** The published crystal sprite ({@code TileCrystalRenderer.java:79}). */
    public static final ResourceLocation CRYSTAL_SPRITE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "models/crystal");

    /** Main-shard size multiplier ({@code TileCrystalRenderer.java:90}). */
    public static final float MAIN_SIZE = 1.1F;

    /** Surrounding-shard size multiplier ({@code TileCrystalRenderer.java:100}). */
    public static final float SHARD_SIZE = 0.8F;

    /** The 210/255 brightness floor in 1.21.1's 0-15 light channels ({@code :57, :43}). */
    public static final int CRYSTAL_LIGHT = (int) (210.0F / 255.0F * 15.0F);

    /** The mixed cluster's variant (the original's {@code md == 6}). */
    public static final int MIXED = 6;

    /** One cube: yaw/pitch in degrees and the per-axis scale multipliers. */
    public record Shard(float yawDeg, float pitchDeg, float scaleX, float scaleY, float scaleZ) {
    }

    /**
     * The cluster's six cubes, in the original's consume order (main first):
     * {@code TileCrystalRenderer.java:80-101}. The seed is
     * {@code variant + pos.getX() + pos.getY() * pos.getZ()}.
     */
    public static List<Shard> shards(final int variant, final BlockPos pos) {
        final Random rand = new Random(variant + pos.getX() + pos.getY() * pos.getZ());
        final List<Shard> shards = new ArrayList<>(6);
        // Java evaluates a call's arguments left to right, so the original's
        // consume order per shard is: the angles at the call site (:86-87 /
        // :98-99), then the shade pair inside drawCrystal (:43), then the three
        // scales on the glScalef line (:56). The plan drops the shade's value
        // (no player-time pulse) but keeps its consume, so every later shard
        // keeps the original's numbers.
        final float mainYaw = (rand.nextFloat() - rand.nextFloat()) * 5.0F;
        final float mainPitch = (rand.nextFloat() - rand.nextFloat()) * 5.0F;
        consumeShade(rand);
        shards.add(new Shard(mainYaw, mainPitch,
                scale(rand, 0.15F, 0.075F) * MAIN_SIZE,
                scale(rand, 0.5F, 0.1F) * MAIN_SIZE,
                scale(rand, 0.15F, 0.05F) * MAIN_SIZE));
        for (int a = 1; a < 6; a++) {
            final float yaw = rand.nextInt(36) + 72 * a;
            final float pitch = 15 + rand.nextInt(15);
            consumeShade(rand);
            final float scaleX = scale(rand, 0.15F, 0.075F) * SHARD_SIZE;
            final float scaleY = scale(rand, 0.5F, 0.1F) * SHARD_SIZE;
            final float scaleZ = scale(rand, 0.15F, 0.05F) * SHARD_SIZE;
            shards.add(new Shard(yaw, pitch, scaleX, scaleY, scaleZ));
        }
        return List.copyOf(shards);
    }

    /**
     * The cluster tint: {@code BlockCustomOreItem.colors[variant + 1]}, the
     * mixed cluster (6) mains on {@code colors[5]}
     * ({@code TileCrystalRenderer.java:74-76}). Variant 7 (eldritch) is clamped
     * to the last tint — the original runs off the end of the array there.
     */
    public static int tintFor(final int variant) {
        if (variant == MIXED) {
            return BlockCustomOreItem.COLORS[5];
        }
        final int index = Math.min(Math.max(variant + 1, 0), BlockCustomOreItem.COLORS.length - 1);
        return BlockCustomOreItem.COLORS[index];
    }

    /**
     * The per-orientation shard tint for the mixed cluster:
     * {@code colors[a == 5 ? 6 : a]} ({@code TileCrystalRenderer.java:95}).
     */
    public static int mixedShardTint(final int shardIndex) {
        final int index = shardIndex == 5 ? 6 : shardIndex;
        return BlockCustomOreItem.COLORS[Math.min(Math.max(index, 0), BlockCustomOreItem.COLORS.length - 1)];
    }

    /**
     * Pivot + model rotation for a supporting face,
     * {@code translateFromOrientation} ({@code TileCrystalRenderer.java:20-39}):
     * {dx, dy, dz, rotateXDeg, rotateYDeg, rotateZDeg} — the model drawn
     * upside-down for {@link Direction#DOWN} (180° X, {@code :23}), the side
     * faces rotated -90/+90 about X ({@code :28, :31}) or +90/-90 about Z
     * ({@code :34, :37}).
     */
    public static float[] orientationPlacement(final Direction orientation) {
        switch (orientation) {
            case DOWN:
                return new float[] {0.5F, 1.3F, 0.5F, 180.0F, 0.0F, 0.0F};
            case UP:
                return new float[] {0.5F, -0.3F, 0.5F, 0.0F, 0.0F, 0.0F};
            case NORTH:
                return new float[] {0.5F, 0.5F, 1.3F, -90.0F, 0.0F, 0.0F};
            case SOUTH:
                return new float[] {0.5F, 0.5F, -0.3F, 90.0F, 0.0F, 0.0F};
            case WEST:
                return new float[] {1.3F, 0.5F, 0.5F, 0.0F, 0.0F, 90.0F};
            case EAST:
                return new float[] {-0.3F, 0.5F, 0.5F, 0.0F, 0.0F, -90.0F};
            default:
                return new float[] {0.5F, -0.3F, 0.5F, 0.0F, 0.0F, 0.0F};
        }
    }

    /** True when this variant draws a cluster here (variant 7 is the eldritch host). */
    public static boolean drawsCluster(final int variant) {
        return variant >= 0 && variant <= MIXED;
    }

    private static float scale(final Random rand, final float base, final float spread) {
        return base + rand.nextFloat() * spread;
    }

    /**
     * The original's per-shard shade consume: {@code rand.nextInt(10)} and
     * {@code rand.nextFloat()} inside {@code drawCrystal}
     * ({@code TileCrystalRenderer.java:43}). The port has no player-time pulse,
     * but the consume must stay so every later shard keeps the original's value.
     */
    private static void consumeShade(final Random rand) {
        rand.nextInt(10);
        rand.nextFloat();
    }

    private CrystalWorldArt() {
    }
}
