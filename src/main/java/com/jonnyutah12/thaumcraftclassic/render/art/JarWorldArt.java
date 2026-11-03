package com.jonnyutah12.thaumcraftclassic.render.art;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import net.minecraft.resources.ResourceLocation;

/**
 * The jar's world art as a data plan (BD-18, t_6563ce50, D-021).
 *
 * <p>1.7.10 drew the jar's contents inside its tile-entity renderer, from the
 * live tile, every frame — no blockstate, no item model:
 * {@code thaumcraft.client.renderers.tile.TileJarRenderer}.</p>
 *
 * <ul>
 *   <li>{@code renderLiquid} ({@code TileJarRenderer.java:105-137}): a fluid box
 *   from {@code y = 0.0625} up, x/z 0.25..0.75, whose height is
 *   {@code (float) te.amount / te.maxAmount * 0.625F} ({@code :110}); tint
 *   {@code te.aspect.getColor()} ({@code :115}); brightness
 *   {@code max(200, blockLight)} ({@code :118-120} — the block light is 0-15,
 *   so the 200 floor always wins); sprite {@code BlockJar.iconLiquid}, the
 *   published {@code block/animatedglow} row ({@code BlockJar.java:59}).</li>
 *   <li>The label: {@code :55-61} rotates the whole label frame by the facing
 *   (3 → 180°, 4 → 270°, 5 → 90°); {@code :65} computes the crooked tilt
 *   {@code (aspectFilter.getTag().hashCode() + xCoord + facing) % 4 - 2}
 *   degrees; {@code :72} draws the published {@code models/label} quad
 *   (size 0.5, offset (0, -0.4, 0.315)); {@code :74-82} draws the aspect tag
 *   icon centred on it ({@code UtilsFX.drawTag}, scale 0.021).</li>
 *   <li>The tile gates the art: liquid only {@code amount > 0}, label only
 *   {@code aspectFilter != null} ({@code :48, :52}).</li>
 * </ul>
 *
 * <p>Out of this plan, on purpose: the node-jar scale pulse ({@code :87-96},
 * TileJarNode is TC-M6-03, not ported) and the brain jar (TileJarBrain is
 * TC-M6-05, not ported — variant 1 has no tile in the port today). The
 * per-frame {@code tile.changed} pulse (the original reads the field at
 * {@code :33} of the tile) is a render-time animation, not tile data, and
 * rides with the renderer.</p>
 *
 * <p>Server-safe by construction (game tests run on a dedicated server): every
 * input is a tile field, and the only outputs are numbers, colours, and
 * {@link ResourceLocation} sprite ids. The renderer consumes the plan; the
 * probes read the plan directly.</p>
 */
public final class JarWorldArt {

    /** The published fluid sprite: {@code BlockJar.iconLiquid} (BlockJar.java:59). */
    public static final ResourceLocation FLUID_SPRITE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "block/animatedglow");

    /** The published label sprite ({@code TileJarRenderer.java:72}, CSV row textures/models/label.png). */
    public static final ResourceLocation LABEL_SPRITE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "models/label");

    /** {@code * 0.625F} in the original's height formula (TileJarRenderer.java:110). */
    public static final float FILL_PER_UNIT = 0.625F;

    /**
     * The original's fluid brightness floor, 200/255 of fullbright
     * ({@code TileJarRenderer.java:118-120}); 1.21.1 packs light as 0-15
     * channels, so the floor is 200 * 15 / 255 = 11.
     */
    public static final int FLUID_LIGHT = 11;

    /** Label tilt + sprites, {@code TileJarRenderer.java:65, 74-87}. */
    public record Label(float tiltDeg, ResourceLocation labelSprite, ResourceLocation aspectSprite) {
    }

    /**
     * Everything the jar's TER draws from tile data: the fluid box height in
     * blocks (0 = draw nothing), its tint (0 = no tint), its light, and the
     * label (null = draw none).
     */
    public record Plan(float fluidHeight, int fluidArgb, int fluidLight, Label label) {
    }

    /**
     * {@code (float) amount / maxAmount * 0.625F} ({@code TileJarRenderer.java:110}).
     * The original has no clamp: a full 500-unit jar is 3.125 tall, and the
     * fluid box overflows the cell exactly as it did in 1.7.10 — recorded, not
     * "fixed", in BD-18.
     */
    public static float fluidHeight(final int amount, final int maxAmount) {
        throw new UnsupportedOperationException("BD-18 TER art not wired (t_6563ce50)");
    }

    /**
     * The label-frame yaw from the facing, {@code TileJarRenderer.java:55-61}:
     * 3 → 180, 4 → 270, 5 → 90, everything else 0.
     */
    public static int facingRotation(final int facing) {
        throw new UnsupportedOperationException("BD-18 TER art not wired (t_6563ce50)");
    }

    /**
     * The crooked tilt, {@code TileJarRenderer.java:65}:
     * {@code (tag.hashCode() + xCoord + facing) % 4 - 2}, in degrees.
     */
    public static float tiltDeg(final String aspectTag, final int xCoord, final int facing) {
        throw new UnsupportedOperationException("BD-18 TER art not wired (t_6563ce50)");
    }

    /** The tag icon for a label: the published {@code textures/aspects/<tag>.png} rows. */
    public static ResourceLocation aspectSprite(final Aspect aspect) {
        throw new UnsupportedOperationException("BD-18 TER art not wired (t_6563ce50)");
    }

    /**
     * The tile read whole: liquid while {@code amount > 0}, tint
     * {@code aspect.getColor()}, label while {@code aspectFilter != null}.
     */
    public static Plan plan(final TileJarFillable tile) {
        throw new UnsupportedOperationException("BD-18 TER art not wired (t_6563ce50)");
    }

    private JarWorldArt() {
    }
}
