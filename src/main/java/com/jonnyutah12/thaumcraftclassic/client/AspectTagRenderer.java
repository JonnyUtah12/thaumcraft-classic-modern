package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The port of {@code UtilsFX.drawTag} — the one method every original aspect tag
 * went through ({@code reference/decompiled/thaumcraft/thaumcraft/client/lib/UtilsFX.java:814-897};
 * its call sites are {@code GuiArcaneWorkbench.java:65},
 * {@code GuiDeconstructionTable.java:39}, {@code GuiThaumatorium.java:167},
 * {@code GuiResearchTable}, {@code GuiResearchRecipe} and the tile renderers).
 * TC-M7-04-m2d / t_9135351b.
 *
 * <p><b>What the original drew, part by part</b>, in the order its GL calls run
 * (every coordinate and colour is in {@link ContainerLayout}, where
 * {@code ContainerLayoutTest} can pin it without a client):</p>
 * <ol>
 *   <li>a 16x16 quad from {@code (x, y)} to {@code (x + 16, y + 16)} with UV
 *       {@code 0..1}, bound to {@code aspect.getImage()} =
 *       {@code thaumcraft:textures/aspects/<tag>.png}, tinted
 *       {@code glColor4f(colour/255, alpha)} from {@code aspect.getColor()}
 *       (UtilsFX.java:824-842), or {@code (0.1, 0.1, 0.1, alpha * 0.8)} in the
 *       {@code bw} variant (UtilsFX.java:828);</li>
 *   <li>when {@code amount > 0}: the amount at 0.5 scale, right-aligned on the
 *       tag's right edge and bottom-aligned inside the box, white
 *       ({@code 16777215}), formatted {@code "#######.##"} (UtilsFX.java:845-862);</li>
 *   <li>when {@code bonus > 0}: a 16x16 quad of the particle sheet 4 px up-left of
 *       the box, frame {@code 16 * (player.ticksExisted % 16)} at sheet v = 80
 *       (UtilsFX.java:865-870), plus the bonus count at 0.5 scale when it is
 *       more than 1 (UtilsFX.java:872-886).</li>
 * </ol>
 *
 * <p><b>1.21.1 rendering, no legacy GL.</b> The quad is one
 * {@link GuiGraphics#blit(ResourceLocation, int, int, int, int, float, float, int, int, int, int)}
 * call under {@link GuiGraphics#setColor(float, float, float, float)}, which is
 * the modern spelling of the original's bind-then-{@code glColor4f}: vanilla's
 * {@code position_tex} shader multiplies the sampled texel by
 * {@code ColorModulator} = the shader colour, so tint and alpha behave exactly as
 * {@code glColor4f} did. The whole shader colour is restored to
 * {@code (1, 1, 1, 1)} afterwards, the way the original's
 * {@code glColor4f(1,1,1,1)} tail did (UtilsFX.java:892).</p>
 *
 * <p><b>The overlay gate (D-004).</b> {@code docs/asset-mapping.csv} lists
 * {@code textures/aspects/<tag>.png} and {@code textures/misc/particles.png} as
 * {@code ARR-overlay}: All Rights Reserved bytes that ship only in the
 * asset-overlay build, never in the code-only jar this work is gated on. So the
 * renderer <b>asks the resource provider</b> whether the bytes exist
 * ({@link #tagTextureSize}, cached — the original cached its texture sizes the
 * same way, UtilsFX.java:309 and 739-758). With the overlay applied the real
 * sprite is tinted and drawn. Without it the tag is filled with the aspect's own
 * colour at the same alpha in the same 16 px box: the same geometry, the same
 * animation, the same amount glyph, no invented art, and no missing-texture
 * magenta. The path is not a guess — {@code OriginalTextureOverlayTest} pins the
 * CSV row for every {@code textures/aspects/} file and that
 * {@code aspects} is one of the directories the overlay copies <i>unrenamed</i>,
 * which is the same literal {@link Aspect#getImage()} builds.</p>
 *
 * <p><b>Three things this renderer cannot reproduce, all recorded in
 * docs/BEHAVIORAL_DEVIATIONS.md (BD-18).</b> The original set
 * {@code glAlphaFunc(GL_GREATER, 1/255)} (UtilsFX.java:820) to keep faintly
 * transparent texels; 1.21.1's {@code position_tex} shader discards a texel under
 * {@code a < 0.1}, so the softest edge of the original art is clipped. The amount
 * glyph's black outline was a 4-neighbour pass at ±1 in the 0.5-scaled space
 * (UtilsFX.java:852-859); 1.21.1's own item-label path is a single
 * drop-shadow, so {@code drawString(..., true)} is used — same text, same
 * anchor, same size, lighter outline. And the original drew the tag ADDITIVELY:
 * every GUI caller passes {@code blend = 771} = {@code GL_ONE}, so
 * {@code glBlendFunc(GL_SRC_ALPHA, GL_ONE)} (UtilsFX.java:819) composites the
 * tinted texels on top of the panel; 1.21.1's {@code blit} render type is
 * translucent ({@code SRC_ALPHA, ONE_MINUS_SRC_ALPHA}), which is the platform's
 * only GUI texturing path. Same colours, same alpha, same geometry; the
 * compositing over a bright panel is standard rather than additive.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class AspectTagRenderer {

    /**
     * Texture edge per image path, the original's {@code textureSizeCache}
     * (UtilsFX.java:309): an empty value means the bytes are not in this jar —
     * the code-only build (D-004) — and the tag draws as its flat colour box.
     * Resolved once per path; a resource-pack reload that adds an aspect sheet
     * is picked up on the next client start, as in the original.
     */
    private static final Map<ResourceLocation, OptionalInt> TAG_TEXTURE_SIZES = new ConcurrentHashMap<>();

    /**
     * The original's full signature, in its parameter order
     * ({@code drawTag(x, y, aspect, amount, bonus, z, blend, alpha, bw)}).
     *
     * @param gui      the screen's graphics
     * @param font     the screen's font, for the amount and bonus glyphs
     * @param x        absolute screen x of the tag box (the caller adds the panel origin)
     * @param y        absolute screen y of the tag box
     * @param aspect   the aspect to draw; {@code null} draws nothing, the original's
     *                 {@code if (aspect != null)} guard (UtilsFX.java:815)
     * @param amount   the vis amount printed at the tag's lower-right, 0 for none
     * @param bonus    the bonus count, 0 for no sparkle
     * @param alpha    the tag's alpha (the caller's pulse — see
     *                 {@link ContainerLayout#aspectAlpha})
     * @param bw       the original's black-and-white tag variant
     * @param ageTicks the client-tick age driving the sparkle frame: the original read
     *                 {@code player.ticksExisted} (UtilsFX.java:869), which a
     *                 world-free renderer cannot, so the caller passes its own clock
     *                 (every screen here already derives one in {@code pulseTime()})
     */
    public static void drawTag(final GuiGraphics gui, final Font font, final int x, final int y,
                               final Aspect aspect, final float amount, final int bonus,
                               final float alpha, final boolean bw, final int ageTicks) {
        if (aspect == null) {
            // UtilsFX.java:815 — a null aspect draws nothing at all; the deconstruction
            // table's empty box is the original's own empty box.
            return;
        }
        final float tintAlpha = ContainerLayout.tagAlphaOf(alpha, bw);
        final int rgb = aspect.getColor();
        final int tone = Math.round(ContainerLayout.TAG_BW_TONE * 255.0F);
        final float red = bw ? ContainerLayout.TAG_BW_TONE : ((rgb >> 16) & 0xFF) / 255.0F;
        final float green = bw ? ContainerLayout.TAG_BW_TONE : ((rgb >> 8) & 0xFF) / 255.0F;
        final float blue = bw ? ContainerLayout.TAG_BW_TONE : (rgb & 0xFF) / 255.0F;

        gui.setColor(red, green, blue, tintAlpha);
        try {
            final OptionalInt edge = tagTextureSize(aspect.getImage());
            if (edge.isPresent()) {
                // The whole texture onto the 16 px box — the original's UV 0..1 quad.
                final int size = edge.getAsInt();
                gui.blit(aspect.getImage(), x, y,
                        ContainerLayout.TAG_SIZE, ContainerLayout.TAG_SIZE,
                        0.0F, 0.0F, size, size, size, size);
            } else {
                // No bytes in this jar (D-004): the aspect's own colour in the original's
                // box at the original's alpha — the geometry, not an invented sprite.
                gui.fill(x, y, x + ContainerLayout.TAG_SIZE, y + ContainerLayout.TAG_SIZE,
                        ContainerLayout.tagTint(rgb, alpha, bw));
            }

            if (amount > 0.0F) {
                final String text = ContainerLayout.tagAmountText(amount);
                drawScaledGlyph(gui, font, text,
                        ContainerLayout.tagAmountScaledX(x, font.width(text)),
                        ContainerLayout.tagAmountScaledY(y, font.lineHeight),
                        ContainerLayout.TAG_AMOUNT_COLOUR);
            }

            if (bonus > 0) {
                drawSparkle(gui, x, y, ageTicks);
                if (bonus > 1) {
                    final String text = Integer.toString(bonus);
                    drawScaledGlyph(gui, font, text,
                            ContainerLayout.tagBonusScaledX(x, font.width(text)),
                            ContainerLayout.tagBonusScaledY(y, font.lineHeight),
                            ContainerLayout.TAG_AMOUNT_COLOUR);
                }
            }
        } finally {
            // UtilsFX.java:892 — the original put the colour back to opaque white.
            gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /**
     * The original's 6-argument overload — the deconstruction table's form
     * ({@code GuiDeconstructionTable.java:39}): {@code blend = 771}, alpha 1.0,
     * no {@code bw}, i.e. the aspect's full colour (UtilsFX.java:802-804).
     */
    public static void drawTag(final GuiGraphics gui, final Font font, final int x, final int y,
                               final Aspect aspect, final float amount, final int ageTicks) {
        drawTag(gui, font, x, y, aspect, amount, 0, 1.0F, false, ageTicks);
    }

    /**
     * The original's 3-argument overload: no amount, no bonus, alpha 1.0 and the
     * {@code bw} tint (UtilsFX.java:806-808). Its callers are the in-world tile
     * renderers that show a jar's or an alembic's filter aspect
     * ({@code TileAlembicRenderer.java}, {@code TileJarRenderer.java}), not the GUIs.
     */
    public static void drawTag(final GuiGraphics gui, final Font font, final int x, final int y,
                               final Aspect aspect) {
        drawTag(gui, font, x, y, aspect, 0.0F, 0, 1.0F, true, 0);
    }

    /**
     * The 0.5-scaled glyph pass (UtilsFX.java:846-862). The caller's coordinates
     * are the original's scaled-space literals, so a pose scale of 0.5 lands the
     * text exactly where the original's did.
     *
     * <p>The original drew a black outline by rendering the string at the four
     * unit offsets first (UtilsFX.java:852-859); 1.21.1's label path is vanilla's
     * single drop shadow, which is what the {@code dropShadow} flag asks for.</p>
     */
    private static void drawScaledGlyph(final GuiGraphics gui, final Font font, final String text,
                                        final int scaledX, final int scaledY, final int colour) {
        final var pose = gui.pose();
        pose.pushPose();
        pose.scale(ContainerLayout.TAG_AMOUNT_SCALE, ContainerLayout.TAG_AMOUNT_SCALE, 1.0F);
        gui.drawString(font, text, scaledX, scaledY, colour, true);
        pose.popPose();
    }

    /**
     * The bonus sparkle quad (UtilsFX.java:865-870): the sheet frame for this
     * age, 4 px up-left of the tag box, drawn at full white. Skipped when the
     * sheet is not in the jar (the same overlay gate as the aspect sprite).
     */
    private static void drawSparkle(final GuiGraphics gui, final int x, final int y, final int ageTicks) {
        final OptionalInt sheet = tagTextureSize(ContainerLayout.PARTICLE_SHEET);
        if (sheet.isEmpty()) {
            // The particle sheet is ARR-overlay too (docs/asset-mapping.csv row 1095):
            // no bytes, no sparkle. The tag itself is unaffected.
            return;
        }
        final int u = ContainerLayout.tagSparkleFrame(ageTicks);
        final int size = sheet.getAsInt();
        final int box = x + ContainerLayout.TAG_SPARKLE_OFFSET;
        final int top = y + ContainerLayout.TAG_SPARKLE_OFFSET;
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        gui.blit(ContainerLayout.PARTICLE_SHEET, box, top,
                ContainerLayout.TAG_SIZE, ContainerLayout.TAG_SIZE,
                (float) u, (float) ContainerLayout.TAG_SPARKLE_V,
                ContainerLayout.TAG_SIZE, ContainerLayout.TAG_SIZE, size, size);
    }

    /**
     * The edge of a shipped texture in texels, or empty when the bytes are not in
     * this jar. Reads the image once per path, like the original's
     * {@code getTextureSize} (UtilsFX.java:739-758) — that is also how the
     * renderer knows the overlay is applied: the aspect sheets are 32x32, so a
     * 16-texel UV window would crop them.
     */
    public static OptionalInt tagTextureSize(final ResourceLocation image) {
        return TAG_TEXTURE_SIZES.computeIfAbsent(image, AspectTagRenderer::readTextureSize);
    }

    /** True when the aspect's sprite ships in this jar, i.e. the asset overlay is applied. */
    public static boolean hasTagSprite(final Aspect aspect) {
        return aspect != null && tagTextureSize(aspect.getImage()).isPresent();
    }

    private static OptionalInt readTextureSize(final ResourceLocation image) {
        try {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                return OptionalInt.empty();
            }
            final Optional<Resource> resource = minecraft.getResourceManager().getResource(image);
            if (resource.isEmpty()) {
                return OptionalInt.empty();
            }
            try (InputStream in = resource.get().open()) {
                final BufferedImage bytes = ImageIO.read(in);
                return bytes == null ? OptionalInt.empty() : OptionalInt.of(bytes.getWidth());
            }
        } catch (Exception e) {
            // A texture that cannot be decoded is the same case as one that is absent:
            // draw the colour box. The original's getTextureSize did the same and
            // returned its default size (UtilsFX.java:753-756).
            return OptionalInt.empty();
        }
    }

    /** Forget the cached lookups; used by tests and by a resource-pack reload path. */
    public static void clearTextureCache() {
        TAG_TEXTURE_SIZES.clear();
    }

    private AspectTagRenderer() {
    }
}
