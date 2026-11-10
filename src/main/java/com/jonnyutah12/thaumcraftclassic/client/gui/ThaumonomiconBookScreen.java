package com.jonnyutah12.thaumcraftclassic.client.gui;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchPageData;
import com.jonnyutah12.thaumcraftclassic.research.ThaaumonomiconModel;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.sounds.SoundSource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * TC-M3-03: the open Thaumonomicon research page — port of the original
 * {@code thaumcraft.client.gui.GuiResearchRecipe}.
 *
 * <p>Parity: 256x181 book pane at 1.3× scale, page layout (title header with
 * bookmark strip, wrapped text at 139px, aspect pages with 32px tags +
 * component formulas, bobbing page-turn arrows), history stack and the
 * back-to-browser key handling are taken from the original. Deviations
 * (docs/visual-parity.md): recipe pages draw nothing until their recipe types
 * are ported (the original likewise draws nothing for an unresolvable recipe);
 * the hover item-grid over aspect tags needs the essentia item-tag bridge
 * (TC-M7 polish); IMAGE pages are no-ops in the original and stay no-ops.</p>
 */
public class ThaumonomiconBookScreen extends Screen {

    static final int PANE_W = 256;
    static final int PANE_H = 181;

    static final ResourceLocation RESEARCH_BOOK =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "textures/gui/gui_researchbook.png");

    /** Original static history: {entryKey, page} pairs pushed on navigation. */
    private static final Deque<Object[]> HISTORY = new ArrayDeque<>();

    private final String entryKey;
    private int page;
    private final Double restoreX;
    private final Double restoreY;

    private int hoveredAspectX = -1;
    private int hoveredAspectY = -1;

    public ThaumonomiconBookScreen(final String entryKey, final int page) {
        this(entryKey, page, null, null);
    }

    public ThaumonomiconBookScreen(final String entryKey, final int page, final Double restoreX, final Double restoreY) {
        super(Component.empty());
        this.entryKey = entryKey;
        this.page = page;
        this.restoreX = restoreX;
        this.restoreY = restoreY;
    }

    private List<ResearchPageData> pages() {
        return ThaumonomiconModel.pagesFor(ResearchManager.graph(), entryKey,
                minecraft.player == null ? com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData.empty()
                        : minecraft.player.getData(com.jonnyutah12.thaumcraftclassic.player.ModAttachments.PLAYER_KNOWLEDGE));
    }

    private void playPageSound() {
        if (minecraft.player != null) {
            minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(),
                    minecraft.player.getZ(), ModSounds.PAGE.value(), SoundSource.PLAYERS, 0.25F, 1.0F, false);
        }
    }

    // ----------------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final int sw = (this.width - (int) (PANE_W * 1.3F)) / 2;
        final int sh = (this.height - (int) (PANE_H * 1.3F)) / 2;
        final List<ResearchPageData> pages = pages();

        if (page < pages.size() - 2 && mouseX >= sw + 261 && mouseX <= sw + 275 && mouseY >= sh + 189
                && mouseY <= sh + 199) {
            HISTORY.push(new Object[]{entryKey, page});
            page += 2;
            playPageSound();
            return true;
        }
        if (page > 0 && mouseX >= sw - 17 && mouseX <= sw - 3 && mouseY >= sh + 189 && mouseY <= sh + 199) {
            HISTORY.push(new Object[]{entryKey, page});
            page -= 2;
            playPageSound();
            return true;
        }
        if (!HISTORY.isEmpty() && mouseX >= sw + 118 && mouseX <= sw + 138 && mouseY >= sh + 189
                && mouseY <= sh + 201) {
            final Object[] prev = HISTORY.pop();
            minecraft.setScreen(new ThaumonomiconBookScreen((String) prev[0], (Integer) prev[1], restoreX, restoreY));
            playPageSound();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        // Original: ESC (and the book keybind) clear history and return to the browser at the same map position.
        if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            HISTORY.clear();
            this.minecraft.setScreen(new ThaumonomiconScreen(restoreX, restoreY));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------------- render

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0000000, 0xD0101010);

        final int sw = (this.width - (int) (PANE_W * 1.3F)) / 2;
        final int sh = (this.height - (int) (PANE_H * 1.3F)) / 2;

        // Book background at 1.3× (original translate + scale).
        graphics.pose().pushPose();
        graphics.pose().translate(sw, sh, 0.0F);
        graphics.pose().scale(1.3F, 1.3F, 1.0F);
        graphics.blit(RESEARCH_BOOK, 0, 0, 0, 0, PANE_W, PANE_H, 256, 256);
        graphics.pose().popPose();

        final List<ResearchPageData> pages = pages();
        final int maxPages = pages.size();
        if (maxPages > 0) {
            drawPage(graphics, pages, Math.min(page, maxPages - 1), 0, sw, sh, mouseX, mouseY);
            if (page + 1 < maxPages) {
                drawPage(graphics, pages, page + 1, 1, sw, sh, mouseX, mouseY);
            }
        }

        // Page-turn controls (original drawArrows; bob = sin(tick/3)*0.2+0.1).
        final long tick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        final float bob = (float) (Math.sin(tick / 3.0F) * 0.2F + 0.1F);
        if (!HISTORY.isEmpty()) {
            drawScaledBlit(graphics, sw + 118, sh + 189, 38, 202, 20, 12, bob);
        }
        if (page > 0) {
            drawScaledBlit(graphics, sw - 16, sh + 190, 0, 184, 12, 8, bob);
        }
        if (page < maxPages - 2) {
            drawScaledBlit(graphics, sw + 262, sh + 190, 12, 184, 12, 8, bob);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Original drawTexturedModalRectScaled: center-anchored blit at 1+scale. */
    private void drawScaledBlit(final GuiGraphics graphics, final int x, final int y, final int u, final int v,
                                final int w, final int h, final float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x + w / 2.0F, y + h / 2.0F, 0.0F);
        graphics.pose().scale(1.0F + scale, 1.0F + scale, 1.0F);
        graphics.blit(RESEARCH_BOOK, -w / 2, -h / 2, u, v, w, h, 256, 256);
        graphics.pose().popPose();
    }

    /** Original drawPage: header, then per-type page rendering, left (side 0) or right (side 1). */
    private void drawPage(final GuiGraphics graphics, final List<ResearchPageData> pages, final int pageIndex,
                          final int side, final int sw, final int sh, final int mouseX, final int mouseY) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            return;
        }
        final ResearchPageData pageData = pages.get(pageIndex);
        int x = sw + 30 + side * 152;
        int y = sh + 30;

        if (pageIndex == 0 && side == 0) {
            // Title header: bookmark strip + centered entry name (shrunk past 130px).
            final ResearchEntryData entry = ResearchManager.graph().entry(entryKey);
            if (entry != null) {
                graphics.blit(RESEARCH_BOOK, sw + 118, sh + 10, 192, 0, 20, 20, 256, 256);
                final String name = ThaumonomiconScreen.entryName(entry);
                final int shrink = Math.min(1, (int) (Math.max(0.0F, this.font.width(name) - 130.0F) / 10.0F));
                final int textX = sw + 128 - this.font.width(name) / 2 + shrink * 5;
                graphics.drawString(this.font, name, textX, sh + 15, 3158064, false);
            }
            y += 25;
        }

        switch (pageData.type()) {
            case TEXT, TEXT_CONCEALED -> {
                final String text = pageData.text() == null ? "" : pageData.text();
                graphics.drawWordWrap(this.font, TcGuiSupport.legacyText(text), x - 15 + side * 152, y - 10, 139,
                        0x000000);
            }
            case ASPECTS -> drawAspectPage(graphics, pageData, x, y, side, sw, sh, mouseX, mouseY);
            case IMAGE, ASPECT_IMAGE, RESEARCH, WARP_RESEARCH, WORKBENCH, CRUCIBLE, INFUSION, ARCANE_FURNACE,
                 CENTRIFUGE, HANDHELD, ELDRITCH, UNKNOWN -> {
                // Original: IMAGE pages render nothing; recipe pages render nothing
                // until their recipe type is ported (TC-M4+).
            }
        }
    }

    /** Original drawAspectPage: 32px tags, names, component formulas, hover backdrop. */
    private void drawAspectPage(final GuiGraphics graphics, final ResearchPageData pageData, final int x, final int y,
                                final int side, final int sw, final int sh, final int mouseX, final int mouseY) {
        int count = 0;
        for (final ResearchEntryData.AspectTag tag : pageData.aspects()) {
            final Aspect aspect = Aspect.getAspect(tag.aspect());
            if (aspect == null) {
                continue;
            }
            final int tx = x - 8 + side * 152;
            final int ty = y - 8 + count * 50;

            // Hover backdrop (_back.png at 2.5× of the 16px cell = 40px).
            if (mouseX >= tx && mouseX < tx + 40 && mouseY >= ty && mouseY < ty + 40) {
                graphics.pose().pushPose();
                graphics.pose().translate(tx - 5, ty - 5, 0.0F);
                graphics.pose().scale(2.5F, 2.5F, 1.0F);
                graphics.blit(TcGuiSupport.aspectTexture("_back"), 0, 0, 0, 0, 16, 16, 16, 16);
                graphics.pose().popPose();
            }

            // 32px tag + amount (original drawTag at scale 2).
            graphics.pose().pushPose();
            graphics.pose().translate(tx, ty, 0.0F);
            graphics.pose().scale(2.0F, 2.0F, 1.0F);
            TcGuiSupport.drawTag(graphics, tag.aspect(), 0, 0, tag.amount(), 1.0F);
            graphics.pose().popPose();

            // Localized name centered under the tag.
            final String name = aspect.getName();
            graphics.drawString(this.font, name, tx + 16 - this.font.width(name) / 2, ty + 33, 5263440, false);

            if (!aspect.isPrimal()) {
                final Aspect left = aspect.getComponents().get(0);
                final Aspect right = aspect.getComponents().get(1);
                final int offset = Math.max(0, this.font.width(name) - 100);
                graphics.drawString(this.font, name, tx + 16 - offset + 50, ty + 30, 5263440, true);

                graphics.pose().pushPose();
                graphics.pose().translate(tx + 54, ty + 4, 0.0F);
                graphics.pose().scale(1.5F, 1.5F, 1.0F);
                TcGuiSupport.drawTag(graphics, left.getTag(), 0, 0, 0, 1.0F);
                graphics.pose().popPose();
                graphics.pose().pushPose();
                graphics.pose().translate(tx + 96, ty + 4, 0.0F);
                graphics.pose().scale(1.5F, 1.5F, 1.0F);
                TcGuiSupport.drawTag(graphics, right.getTag(), 0, 0, 0, 1.0F);
                graphics.pose().popPose();

                final String leftName = left.getName();
                final String rightName = right.getName();
                graphics.drawString(this.font, leftName, tx + 54 - this.font.width(leftName) / 2 + 8, ty + 30,
                        5263440, true);
                graphics.drawString(this.font, rightName, tx + 96 - this.font.width(rightName) / 2 + 8, ty + 30,
                        5263440, true);
                graphics.drawString(this.font, "=", tx + 39, ty + 12, 10066329, false);
                graphics.drawString(this.font, "+", tx + 83, ty + 12, 10066329, false);
            } else {
                graphics.drawString(this.font, TcGuiSupport.text("tc.aspect.primal", "Primal Aspect"), tx + 48,
                        ty + 12, 4473924, false);
            }
            count++;
        }
    }
}
