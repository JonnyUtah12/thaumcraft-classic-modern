package com.jonnyutah12.thaumcraftclassic.client.gui;

import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * TC-M3-03: shared drawing/text helpers for the two Thaumonomicon screens.
 *
 * <p>Ports the pieces of the original {@code UtilsFX} the book needs
 * (aspect tag rendering, legacy § text, 0.5× scaled tooltip text). The
 * original's custom {@code TCFontRenderer} is not ported (TC-M7 polish);
 * vanilla font is used, noted in docs/visual-parity.md.</p>
 */
public final class TcGuiSupport {

    /** Original {@code UtilsFX.drawTag} tag size. */
    public static final int TAG_SIZE = 16;

    private static final Map<String, AspectDefinitions.Definition> ASPECT_BY_TAG = buildAspectIndex();

    private TcGuiSupport() {
    }

    private static Map<String, AspectDefinitions.Definition> buildAspectIndex() {
        final Map<String, AspectDefinitions.Definition> index = new HashMap<>();
        for (final AspectDefinitions.Definition definition : AspectDefinitions.all()) {
            index.put(definition.tag(), definition);
        }
        return Map.copyOf(index);
    }

    public static AspectDefinitions.Definition aspectDefinition(final String tag) {
        return ASPECT_BY_TAG.get(tag);
    }

    /** Original {@code Aspect.getImage()}: {@code thaumcraft:textures/aspects/<tag>.png}. */
    public static ResourceLocation aspectTexture(final String tag) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft",
                "textures/aspects/" + tag.toLowerCase(Locale.ROOT) + ".png");
    }

    /**
     * Original {@code UtilsFX.drawTag}: aspect texture tinted with the aspect
     * color, plus the pool amount right/bottom-aligned inside the tag at 0.5×
     * scale (white with a 1px black outline, the original's 8-neighbour pass).
     */
    public static void drawTag(final GuiGraphics graphics, final String tag, final int x, final int y,
                              final int amount, final float alpha) {
        final AspectDefinitions.Definition definition = aspectDefinition(tag);
        final int color = definition == null ? 0xFFFFFF : definition.color();
        final float r = ((color >> 16) & 0xFF) / 255.0F;
        final float g = ((color >> 8) & 0xFF) / 255.0F;
        final float b = (color & 0xFF) / 255.0F;
        graphics.setColor(r, g, b, alpha);
        graphics.blit(aspectTexture(tag), x, y, 0, 0, TAG_SIZE, TAG_SIZE, TAG_SIZE, TAG_SIZE);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (amount > 0) {
            drawTagAmount(graphics, x, y, Integer.toString(amount));
        }
    }

    /** The original's 0.5× outlined amount text, right/bottom aligned in the tag. */
    private static void drawTagAmount(final GuiGraphics graphics, final int x, final int y, final String amount) {
        final Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        final int textWidth = font.width(amount);
        final int textX = 2 * (x + TAG_SIZE) - textWidth;
        final int textY = 2 * (y + TAG_SIZE) - font.lineHeight;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if ((dx == 0 || dy == 0) && (dx != 0 || dy != 0)) {
                    graphics.drawString(font, amount, textX + dx, textY + dy, 0x000000, false);
                }
            }
        }
        graphics.drawString(font, amount, textX, textY, 0xFFFFFF, false);
        graphics.pose().popPose();
    }

    /**
     * Resolve display text: the original lang key when a lang file provides it,
     * otherwise the verbatim en_US.lang string carried in the extracted data
     * (the port ships no lang file yet — TC-M7 owns lang generation).
     */
    public static String text(final String langKey, final String verbatimEnUs) {
        if (langKey != null && !langKey.isEmpty() && net.minecraft.client.resources.language.I18n.exists(langKey)) {
            return net.minecraft.client.resources.language.I18n.get(langKey);
        }
        if (verbatimEnUs != null) {
            return verbatimEnUs;
        }
        return langKey == null ? "" : langKey;
    }

    /**
     * Parse legacy § colour codes into a styled Component so vanilla word
     * wrapping keeps the original's inline formatting (TCFontRenderer parsed
     * § codes directly; Component.literal does not).
     */
    public static Component legacyText(final String raw) {
        final Component out = Component.empty();
        ChatFormatting current = ChatFormatting.WHITE;
        int i = 0;
        while (i < raw.length()) {
            final int mark = raw.indexOf('§', i);
            if (mark < 0 || mark + 1 >= raw.length()) {
                out.append(Component.literal(raw.substring(i)).withStyle(current));
                break;
            }
            if (mark > i) {
                out.append(Component.literal(raw.substring(i, mark)).withStyle(current));
            }
            final ChatFormatting format = ChatFormatting.getByCode(raw.charAt(mark + 1));
            if (format != null) {
                current = format;
            }
            i = mark + 2;
        }
        return out;
    }

    /** Draw text at a pose scale (the original's GL11.glScalef(0.5) tooltip passes). */
    public static void drawScaledText(final GuiGraphics graphics, final String text, final int x, final int y,
                                      final float scale, final int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(Minecraft.getInstance().font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    /** Draw legacy-§ text at a pose scale (the original's §-formatted tooltip strings). */
    public static void drawScaledTextLegacy(final GuiGraphics graphics, final String text, final int x,
                                            final int y, final float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(Minecraft.getInstance().font, legacyText(text), 0, 0, 0xFFFFFF, false);
        graphics.pose().popPose();
    }

    /** Parse a namespaced texture path from the extracted data (e.g. {@code thaumcraft:textures/misc/r_albedo.png}). */
    public static ResourceLocation dataTexture(final String path) {
        return ResourceLocation.parse(path);
    }
}
