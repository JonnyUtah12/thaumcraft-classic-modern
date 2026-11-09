package com.jonnyutah12.thaumcraftclassic.render.art;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import net.minecraft.resources.ResourceLocation;

/**
 * The jar's world art, computed from tile data (BD-18 slice 4, D-021).
 *
 * <p>The 1.7.10 original tessellates the fluid and the aspect label by hand in
 * {@code TileJarRenderer}: the fluid box is {@code (0.25, 0.0625, 0.25) -
 * (0.75, 0.0625 + level, 0.75)} where {@code level = amount / maxAmount *
 * 0.625F} ({@code TileJarRenderer.java:110-112}), tinted by the stored aspect
 * ({@code :114-116}) at a floor brightness of 200/255 ({@code :118-121}) with
 * the jar's own liquid icon, which the original registers as
 * {@code thaumcraft:animatedglow} ({@code BlockJar.java:42, 59}, used at
 * {@code TileJarRenderer.java:124}). The label is a billboard frame with a
 * {@code label.png} plate ({@code :72}) and the filter aspect's icon
 * ({@code :73-77}), rotated by the tile's facing ({@code :55-61}) and tilted by
 * {@code (filterTag.hashCode() + xCoord + facing) % 4 - 2} ({@code :65}).</p>
 *
 * <p>The 1.21.1 renderer draws the same geometry; only these formulas are the
 * contract, and the unit tests pin them. The fluid disappears with the stored
 * amount (the original leaves an empty jar with an empty box, and a TER must
 * not draw anything once the data is gone).</p>
 */
public final class JarWorldArt {

    /** The fluid's top face is 0.25..0.75 in X/Z, 0.0625 + level in Y. */
    public static final float FLUID_MIN_XZ = 0.25F;
    public static final float FLUID_MAX_XZ = 0.75F;
    public static final float FLUID_BASE_Y = 0.0625F;
    /** Full jar = 0.625 blocks of fluid (TileJarRenderer.java:110). */
    public static final float FILL_PER_UNIT = 0.625F;
    /** The label plate (TileJarRenderer.java:72, models/label.png). */
    public static final ResourceLocation LABEL_SPRITE =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "models/label");
    /**
     * The jar's liquid icon: the original's {@code BlockJar.iconLiquid} is
     * registered as {@code thaumcraft:animatedglow} (BlockJar.java:42, 59) and
     * the fluid tessellates with it (TileJarRenderer.java:124).
     */
    public static final ResourceLocation FLUID_SPRITE =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "block/animatedglow");
    /**
     * Packed block light for the fluid. The original's brightness is
     * {@code max(200, surrounding light)} ({@code TileJarRenderer.java:118-121})
     * and it tessellates the fluid with {@code glDisable(GL_LIGHTING)}
     * ({@code :109}); the 200/255 * 15 floor is 11, and the port draws the
     * constant (the live surrounding light is a delta recorded in BD-18).
     */
    public static final int FLUID_LIGHT = (int) (200.0F / 255.0F * 15.0F);

    private JarWorldArt() {
    }

    /** The fluid's height in blocks: amount / maxAmount * 0.625 (TileJarRenderer.java:110). */
    public static float fluidHeight(final int amount, final int maxAmount) {
        if (amount <= 0 || maxAmount <= 0) {
            return 0.0F;
        }
        return (float) amount / (float) maxAmount * FILL_PER_UNIT;
    }

    /**
     * The label frame's yaw for the tile's facing: 3 -> 180, 4 -> 270, 5 -> 90,
     * everything else 0 (TileJarRenderer.java:55-61).
     */
    public static int facingRotation(final int facing) {
        switch (facing) {
            case 3:
                return 180;
            case 4:
                return 270;
            case 5:
                return 90;
            default:
                return 0;
        }
    }

    /** The label's tilt: (filterTag.hashCode() + xCoord + facing) % 4 - 2 (TileJarRenderer.java:65). */
    public static float tiltDeg(final String filterTag, final int xCoord, final int facing) {
        return (filterTag.hashCode() + xCoord + facing) % 4 - 2;
    }

    /**
     * The filter aspect's icon as a blocks-atlas sprite id: the original draws
     * the label icon from the aspect's registered icon (TileJarRenderer.java:73-77),
     * the port's aspects publish {@code textures/aspects/<tag>.png}.
     */
    public static ResourceLocation aspectSprite(final Aspect aspect) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID,
                "aspects/" + aspect.getTag().toLowerCase(java.util.Locale.ROOT));
    }

    /** What one jar's data says to draw, with nothing read from a Level. */
    public static Plan plan(final TileJarFillable tile) {
        final float height = fluidHeight(tile.amount, tile.maxAmount());
        final int argb = tile.aspect == null ? 0xFFFFFF : tile.aspect.getColor();
        Label label = null;
        if (tile.aspectFilter != null) {
            label = new Label(tiltDeg(tile.aspectFilter.getTag(), tile.getBlockPos().getX(), tile.facing),
                    LABEL_SPRITE, aspectSprite(tile.aspectFilter));
        }
        return new Plan(height, argb, FLUID_LIGHT, label);
    }

    /**
     * @param fluidHeight the fluid box's height (0 draws no fluid at all)
     * @param fluidArgb   the stored aspect's colour
     * @param fluidLight  packed block light for the fluid pass
     * @param label       the filter label, null when the jar has no filter
     */
    public record Plan(float fluidHeight, int fluidArgb, int fluidLight, Label label) {
    }

    /** @param tiltDeg the identity-seeded tilt, label + icon sprites, all published */
    public record Label(float tiltDeg, ResourceLocation labelSprite, ResourceLocation aspectSprite) {
    }
}
