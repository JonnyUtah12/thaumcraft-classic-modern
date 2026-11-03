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
 *   <li>main shard ({@code :79-87}): yaw {@code (nextFloat()-nextFloat())*5},
 *   pitch the same (4 floats), then the draw consumes a shade pair
 *   ({@code nextInt(10)}, {@code nextFloat()}) and three scale floats —
 *   x {@code (0.15+r*0.075)*1.1}, y {@code (0.5+r*0.1)*1.1},
 *   z {@code (0.15+r*0.05)*1.1} ({@code :56}).</li>
 *   <li>shards a = 1..5 ({@code :91-101}): yaw {@code nextInt(36)+72*a}, pitch
 *   {@code 15+nextInt(15)} ({@code :99}), then the same 1+1+3 consume, size 0.8.</li>
 *   <li>tint: {@code BlockCustomOreItem.colors[md + 1]}, except variant 6
 *   (mixed) which mains on {@code colors[5]} and cycles the shard tints
 *   {@code colors[a]}, {@code colors[6]} for a = 5 ({@code :74-76, :95}).
 *   The port carries that array verbatim as
 *   {@link BlockCustomOreItem#COLORS}.</li>
 *   <li>light: the draw lights {@code (int) (210F * shade)} ({@code :57})
 *   — a per-frame pulse around a 210/255 floor; the plan exposes the floor
 *   (210 * 15 / 255 = 12) and the renderer keeps the pulse.</li>
 *   <li>placement: the cluster sits on its supporting face —
 *   {@code translateFromOrientation} ({@code :17-38}) offsets the pivot by
 *   ±0.3/1.3 from the cell and rotates the model per orientation; the port's
 *   {@code TileCrystal.getOrientation()} carries that value.</li>
 * </ul>
 *
 * <p>Variant 7 (strange/eldritch) is a {@code TileEldritchCrystal} host with
 * its own renderer in the original ({@code TileEldritchCrystalRenderer}) and
 * is out of this plan. The plan is pure: same inputs, same shard list.</p>
 */
public final class CrystalWorldArt {

    /** The published crystal sprite ({@code TileCrystalRenderer.java:79}). */
    public static final ResourceLocation CRYSTAL_SPRITE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "models/crystal");

    /** Main-shard size multiplier ({@code TileCrystalRenderer.java:86}). */
    public static final float MAIN_SIZE = 1.1F;

    /** Surrounding-shard size multiplier ({@code TileCrystalRenderer.java:100}). */
    public static final float SHARD_SIZE = 0.8F;

    /** The 210/255 brightness floor in 1.21.1's 0-15 light channels. */
    public static final int CRYSTAL_LIGHT = 12;

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
        throw new UnsupportedOperationException("BD-18 TER art not wired (t_6563ce50)");
    }

    /**
     * The cluster tint: {@code BlockCustomOreItem.colors[variant + 1]}, the
     * mixed cluster (6) mains on {@code colors[5]}
     * ({@code TileCrystalRenderer.java:74-76}).
     */
    public static int tintFor(final int variant) {
        throw new UnsupportedOperationException("BD-18 TER art not wired (t_6563ce50)");
    }

    /**
     * The per-orientation shard tint for the mixed cluster:
     * {@code colors[a == 5 ? 6 : a]} ({@code TileCrystalRenderer.java:95}).
     */
    public static int mixedShardTint(final int shardIndex) {
        throw new UnsupportedOperationException("BD-18 TER art not wired (t_6563ce50)");
    }

    /**
     * Pivot + model rotation for a supporting face,
     * {@code translateFromOrientation} ({@code TileCrystalRenderer.java:17-38}):
     * {dx, dy, dz, rotateXDeg, rotateYDeg}, the model drawn upside-down for
     * {@link Direction#DOWN} (the original's 180° X rotation, {@code :23}).
     */
    public static float[] orientationPlacement(final Direction orientation) {
        throw new UnsupportedOperationException("BD-18 TER art not wired (t_6563ce50)");
    }

    private CrystalWorldArt() {
    }
}
