package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.menus.ThaumatoriumMenu;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Thaumatorium screen (content-mapping.csv {@code thaumcraft:gui_thaumatorium},
 * gui_id 3, original gui sheet {@code gui_thaumatorium.png}).
 *
 * <p>Parity target, measured off
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiThaumatorium.java}:
 * the 176&nbsp;x&nbsp;166 default panel, the catalyst slot at
 * {@code (48,16)}, the player grid at {@code (8,84)} and hotbar at
 * {@code (8,142)}, the page arrows (16x8) at {@code (128,16)}/{@code (128,24)}
 * shown while more than one recipe is listed, the aspect-window pager (8x16)
 * at {@code (32,40)}/{@code (136,40)} shown while the current recipe has more
 * than six aspects, the 48x48 knowledge highlight at {@code (104,8)} while the
 * queue is non-empty and the output is hovered or the current recipe queued,
 * the 24x24 queue box at {@code (88,16)} pulsing at
 * {@code 0.6 + sin(t/5)*0.4 + 0.4}, the six 16x16 aspect tags at
 * {@code (40 + 16p, 40)} with the amount text centred under them, the
 * essentia bars (14x6 background at {@code 41 + 16p, 57}, the
 * {@code have/need*12} fill at {@code 42 + 16p, 58}) while the current recipe
 * is queued, the output preview at {@code (112,16)} greyed at
 * {@code 0.3 + sin(t/4)*0.3 + 0.3} while queueable, and the queue counter at
 * {@code (136,33)} scaled 0.5. Every number lives in {@link ContainerLayout};
 * the drawing is the shared {@link ScreenPanel} recipe.</p>
 *
 * <p><b>Everything drawn is menu data.</b> The original computed the recipe
 * list, {@code index} and {@code startAspect} client-side on its shared
 * container; 1.21.1 screens see only the menu, so the list's visible count,
 * the page state, the queue state and the per-aspect (colour, needed, stored)
 * triples arrive as {@link ThaumatoriumMenu}'s data slots, and the output
 * preview rides in the menu's hidden slot. The clicks send the original's
 * raw button ids (list index / the pager button the original's field mutation
 * corresponds to), gated at the original's hitboxes — including its
 * queueability gate — and play the original's {@code hhon} /
 * {@code cameraclack} at the original's volumes.</p>
 *
 * <p><b>The display is drawn, not faked.</b> TC-M7-04-m2e put the window's own
 * glyphs on the panel: the panel, the four pager boxes, the 48x48 highlight,
 * the 24x24 queue box and both essentia bar cells are blitted from
 * {@link ContainerLayout#TAU_SHEET} ({@code gui_thaumatorium.png}, mapped to
 * itself by {@code docs/asset-mapping.csv} as {@code ARR-overlay}) through
 * {@link ScreenPanel#drawSheet}, and the six tags go through
 * {@link AspectTagRenderer#drawTag} — the port's {@code UtilsFX.drawTag},
 * TC-M7-04-m2d — with the recipe's amount. Every one of those draws is gated on
 * the bytes being in this jar (D-004: original art is All Rights Reserved and
 * ships in the asset-overlay build only; CI builds the code-only jar), and the
 * gate <b>reports</b> which side it took, so the fallback is documented
 * geometry rather than a silent path: the flat panel, the vanilla slot frames,
 * the outlined pager boxes and the aspect-coloured tag square of
 * TC-M7-04-m2c. {@code ThaumatoriumGameTest.theAspectDisplayCarriesTheAspectItself} pins the
 * mapped sheet, the 48 sprite paths and every UV this screen names. The original
 * drew no title (its foreground layer is empty), so the port draws one, as in
 * BD-18.</p>
 */
@OnlyIn(Dist.CLIENT)
public class ThaumatoriumScreen extends AbstractContainerScreen<ThaumatoriumMenu> {

    /** The lang key of the thaumatorium menu (the menu type is named for the gui row). */
    static final String TITLE_KEY = "container.thaumcraft.gui_thaumatorium";

    /**
     * The port's title position. The original drew no title at all
     * ({@code GuiThaumatorium.drawGuiContainerForegroundLayer} is empty), so
     * this is vanilla's position — the same choice BD-18 records for the
     * workbench.
     */
    static final int TITLE_X = ContainerLayout.titleLabelX();
    static final int TITLE_Y = ContainerLayout.titleLabelY();

    public ThaumatoriumScreen(final ThaumatoriumMenu menu, final Inventory inventory,
                              final Component title) {
        super(menu, inventory, title.getString().isEmpty() ? Component.translatable(TITLE_KEY) : title);
        this.imageWidth = ContainerLayout.PANEL_WIDTH;
        // The original's fixed 176x166 default panel (GuiThaumatorium never
        // sets xSize/ySize), not the shared derivation, which lands on 167.
        this.imageHeight = ContainerLayout.TAU_PANEL_HEIGHT;
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = ContainerLayout.titleLabelX();
        this.inventoryLabelY = ContainerLayout.inventoryLabelY(ContainerLayout.PLAYER_GRID_TOP);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY,
                       final float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        // The hidden preview slot gets no vanilla tooltip (isActive is false),
        // so the output box's tooltip — the original's drawOutput tail
        // (GuiThaumatorium.java:196-200) — is fired here.
        final ItemStack preview = this.menu.getPreviewStack();
        if (!preview.isEmpty() && this.menu.getCarried().isEmpty()
                && inBox(mouseX - this.leftPos, mouseY - this.topPos,
                        ContainerLayout.TAU_OUTPUT_X, ContainerLayout.TAU_OUTPUT_Y,
                        ContainerLayout.TAU_OUTPUT_SIZE, ContainerLayout.TAU_OUTPUT_SIZE)) {
            guiGraphics.renderTooltip(this.font, preview, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick,
                            final int mouseX, final int mouseY) {
        if (!ScreenPanel.drawSheet(guiGraphics, ContainerLayout.TAU_SHEET, this.leftPos, this.topPos,
                0, 0, ContainerLayout.TAU_SHEET_U, ContainerLayout.TAU_SHEET_V,
                ContainerLayout.TAU_SHEET_W, ContainerLayout.TAU_SHEET_H)) {
            // No sheet in this jar (the code-only build, D-004): the flat panel
            // and the vanilla slot frames TC-M7-04-m2c documents (BD-19).
            ScreenPanel.drawBackground(guiGraphics, this.leftPos, this.topPos,
                    ContainerLayout.PLAYER_GRID_TOP);
            for (final var slot : this.menu.slots) {
                if (slot.index == ThaumatoriumMenu.SLOT_PREVIEW) {
                    // The carrier slot: the original drew no slot frame here.
                    continue;
                }
                ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, slot);
            }
        }
        // With the sheet drawn, the slot frames are NOT added: 1.7.10 bakes them
        // into gui_thaumatorium.png (its drawGuiContainerBackgroundLayer blits
        // the whole panel), so a vanilla container/slot sprite on top is a
        // double frame, not the original.
        if (this.menu.getVisibleCount() <= 0) {
            return;
        }
        renderPagers(guiGraphics);
        renderQueueState(guiGraphics, mouseX, mouseY);
        renderAspects(guiGraphics);
        renderOutput(guiGraphics);
        renderQueueCounter(guiGraphics);
    }

    /**
     * The page arrows and the aspect-window pager. The original blitted
     * enabled/disabled sprite pairs from its sheet — the cells are
     * {@link ContainerLayout#thaumatoriumArrowU} /
     * {@link ContainerLayout#thaumatoriumAspectPagerU}, and
     * {@link ScreenPanel#drawSheet} draws them wherever the sheet is in the jar
     * (the asset-overlay build). Each button also appears under the original's
     * own conditions: the arrows while more than one recipe is listed, the
     * pager while the current recipe has more than six aspects.
     */
    private void renderPagers(final GuiGraphics guiGraphics) {
        final int visible = this.menu.getVisibleCount();
        if (visible > 1) {
            drawButton(guiGraphics, ContainerLayout.TAU_PAGE_X, ContainerLayout.TAU_PAGE_PREV_Y,
                    ContainerLayout.TAU_PAGE_W, ContainerLayout.TAU_PAGE_H,
                    ContainerLayout.thaumatoriumArrowU(this.menu.getIndex() > 0),
                    ContainerLayout.TAU_UV_PREV_V, this.menu.getIndex() > 0);
            drawButton(guiGraphics, ContainerLayout.TAU_PAGE_X, ContainerLayout.TAU_PAGE_NEXT_Y,
                    ContainerLayout.TAU_PAGE_W, ContainerLayout.TAU_PAGE_H,
                    ContainerLayout.thaumatoriumArrowU(this.menu.getIndex() < visible - 1),
                    ContainerLayout.TAU_UV_NEXT_V, this.menu.getIndex() < visible - 1);
        }
        final int aspects = this.menu.getAspectCount();
        if (aspects > ContainerLayout.TAU_VISIBLE_ASPECTS) {
            drawButton(guiGraphics, ContainerLayout.TAU_ASPECT_LEFT_X, ContainerLayout.TAU_ASPECT_PAGER_Y,
                    ContainerLayout.TAU_ASPECT_PAGER_W, ContainerLayout.TAU_ASPECT_PAGER_H,
                    ContainerLayout.thaumatoriumAspectPagerU(0, this.menu.getStartAspect() > 0),
                    ContainerLayout.TAU_UV_ASPECT_PAGER_V, this.menu.getStartAspect() > 0);
            drawButton(guiGraphics, ContainerLayout.TAU_ASPECT_RIGHT_X, ContainerLayout.TAU_ASPECT_PAGER_Y,
                    ContainerLayout.TAU_ASPECT_PAGER_W, ContainerLayout.TAU_ASPECT_PAGER_H,
                    ContainerLayout.thaumatoriumAspectPagerU(1, this.menu.getStartAspect() < aspects - 1),
                    ContainerLayout.TAU_UV_ASPECT_PAGER_V, this.menu.getStartAspect() < aspects - 1);
        }
    }

    /**
     * One pager box: its sheet cell (the original's enabled/disabled pair), and
     * in the code-only jar the outlined flat box filled dark while its click
     * gate is open — the geometry TC-M7-04-m2c documented, at the same 16x8 /
     * 8x16 rectangles (BD-19).
     */
    private void drawButton(final GuiGraphics guiGraphics, final int x, final int y,
                            final int width, final int height, final int sheetU, final int sheetV,
                            final boolean enabled) {
        if (ScreenPanel.drawSheet(guiGraphics, ContainerLayout.TAU_SHEET, this.leftPos, this.topPos,
                x, y, sheetU, sheetV, width, height)) {
            return;
        }
        ScreenPanel.fillArea(guiGraphics, this.leftPos, this.topPos, x, y, width, height,
                enabled ? ContainerLayout.PANEL_FRAME : ContainerLayout.PANEL_FILL);
        ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos, x, y, width, height);
    }

    /**
     * The two queue indicators the original drew only while the queue is
     * non-empty ({@code recipeHash.size() > 0}): the 48x48 highlight over the
     * output (hovered, or the current recipe queued) at sheet
     * {@code (176,96)}, and the 24x24 white box at {@code (176,56)} pulsing at
     * {@code 0.6 + sin(t/5)*0.4 + 0.4}.
     *
     * <p><b>Only the 24x24 box pulses.</b> The source assigns the alpha
     * <i>between</i> the two blits ({@code GuiThaumatorium.java:112-120}): the
     * highlight is blitted under the {@code glColor4f(1,1,1,1)} the method set at
     * its head, so it is opaque, and only the box that follows carries the sine.
     * The port reproduces that — the highlight is drawn at alpha 1.0, the box at
     * {@link ContainerLayout#thaumatoriumQueuePulse} through
     * {@link ContainerLayout#withAlpha}, the 1.21.1 spelling of
     * {@code glColor4f}. The flat frame (highlight) and alpha-filled square (box)
     * are the code-only fallback (BD-19).</p>
     */
    private void renderQueueState(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        if (this.menu.getQueueSize() <= 0) {
            return;
        }
        final float pulse = ContainerLayout.thaumatoriumQueuePulse(pulseTime());
        final boolean hoveredOutput =
                inBox(mouseX - this.leftPos, mouseY - this.topPos,
                        ContainerLayout.TAU_OUTPUT_X, ContainerLayout.TAU_OUTPUT_Y,
                        ContainerLayout.TAU_OUTPUT_SIZE, ContainerLayout.TAU_OUTPUT_SIZE);
        if (hoveredOutput || this.menu.isCurrentQueued()) {
            if (!ScreenPanel.drawSheet(guiGraphics, ContainerLayout.TAU_SHEET, this.leftPos, this.topPos,
                    ContainerLayout.TAU_HIGHLIGHT_X, ContainerLayout.TAU_HIGHLIGHT_Y,
                    ContainerLayout.TAU_UV_HIGHLIGHT_U, ContainerLayout.TAU_UV_HIGHLIGHT_V,
                    ContainerLayout.TAU_HIGHLIGHT_SIZE, ContainerLayout.TAU_HIGHLIGHT_SIZE)) {
                ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                        ContainerLayout.TAU_HIGHLIGHT_X, ContainerLayout.TAU_HIGHLIGHT_Y,
                        ContainerLayout.TAU_HIGHLIGHT_SIZE, ContainerLayout.TAU_HIGHLIGHT_SIZE);
            }
        }
        if (!ScreenPanel.drawSheet(guiGraphics, ContainerLayout.TAU_SHEET, this.leftPos, this.topPos,
                ContainerLayout.TAU_PULSE_X, ContainerLayout.TAU_PULSE_Y,
                ContainerLayout.TAU_UV_PULSE_U, ContainerLayout.TAU_UV_PULSE_V,
                ContainerLayout.TAU_PULSE_SIZE, ContainerLayout.TAU_PULSE_SIZE,
                ContainerLayout.withAlpha(0xFFFFFF, pulse))) {
            ScreenPanel.fillArea(guiGraphics, this.leftPos, this.topPos,
                    ContainerLayout.TAU_PULSE_X, ContainerLayout.TAU_PULSE_Y,
                    ContainerLayout.TAU_PULSE_SIZE, ContainerLayout.TAU_PULSE_SIZE,
                    ContainerLayout.withAlpha(0xFFFFFF, pulse));
        }
    }

    /**
     * The aspect row. The essentia bars — 14x6 background cell {@code (176,8)},
     * fill cell {@code (176,0)} blitted {@code i1} texels wide in the aspect's
     * own colour — are drawn only while the current recipe is queued, exactly
     * like the original's {@code recipeHash.contains(hash)} gate; the tags draw
     * for every current recipe, as the original's second loop did.
     *
     * <p>The tag itself is {@link AspectTagRenderer#drawTag} — the port's
     * {@code UtilsFX.drawTag} (TC-M7-04-m2d) — at the original's
     * {@code (40 + 16p, 40)} anchors, with the recipe's amount so the original's
     * amount glyph lands on the glyph, at {@link ContainerLayout#TAU_TAG_ALPHA}
     * = 1.0 with <b>no pulse</b>: {@code GuiThaumatorium.java:167-168} calls the
     * six-argument overload, which forwards {@code alpha = 1.0F}
     * ({@code UtilsFX.java:802-804}); the
     * {@code 0.5 + sin((ticks + i*10)/2)*0.2 - 0.2} curve is
     * {@code GuiArcaneWorkbench.java:57}, a different window (BD-19). The
     * renderer owns both branches of the D-004 gate: with the 48 sprites in the
     * jar it blits them, without them it fills the same 16 px box in the
     * aspect's colour, and in both cases it draws the amount glyph on the
     * original's transform — which is why the screen no longer draws the amount
     * text itself (that would double it).</p>
     */
    private void renderAspects(final GuiGraphics guiGraphics) {
        final boolean queued = this.menu.isCurrentQueued();
        for (int pos = 0; pos < ContainerLayout.TAU_VISIBLE_ASPECTS; pos++) {
            final Aspect aspect = this.menu.getDisplayAspect(pos);
            if (aspect == null) {
                // The synced column is empty. Gated on the synced aspect and not
                // on the colour int, because 0 is a legal aspect colour: the
                // number that tints a box cannot say whether a column is empty.
                continue;
            }
            final int rgb = aspect.getColor();
            final int need = this.menu.getDisplayNeed(pos);
            final int have = this.menu.getDisplayHave(pos);
            if (queued) {
                if (!ScreenPanel.drawSheet(guiGraphics, ContainerLayout.TAU_SHEET,
                        this.leftPos, this.topPos,
                        ContainerLayout.thaumatoriumBarX(pos), ContainerLayout.TAU_BAR_Y,
                        ContainerLayout.TAU_UV_BAR_BACKGROUND_U, ContainerLayout.TAU_UV_BAR_BACKGROUND_V,
                        ContainerLayout.TAU_BAR_W, ContainerLayout.TAU_BAR_H)) {
                    ScreenPanel.fillArea(guiGraphics, this.leftPos, this.topPos,
                            ContainerLayout.thaumatoriumBarX(pos), ContainerLayout.TAU_BAR_Y,
                            ContainerLayout.TAU_BAR_W, ContainerLayout.TAU_BAR_H,
                            ContainerLayout.PANEL_FRAME);
                }
                // The original's uncapped arithmetic — (int)(have / need *
                // 12.0F), 5 of a needed 4 is 15 — drawn saturated at the
                // 12-texel fill cell and skipped at zero
                // (ContainerLayout#thaumatoriumBarFillDrawn).
                final int fill = ContainerLayout.thaumatoriumBarFillDrawn(have, need);
                if (fill > 0) {
                    if (!ScreenPanel.drawSheet(guiGraphics, ContainerLayout.TAU_SHEET,
                            this.leftPos, this.topPos,
                            ContainerLayout.thaumatoriumBarFillX(pos), ContainerLayout.TAU_BAR_FILL_Y,
                            ContainerLayout.TAU_UV_BAR_FILL_U, ContainerLayout.TAU_UV_BAR_FILL_V,
                            fill, ContainerLayout.TAU_BAR_FILL_H,
                            ContainerLayout.withAlpha(rgb, 1.0F))) {
                        ScreenPanel.fillArea(guiGraphics, this.leftPos, this.topPos,
                                ContainerLayout.thaumatoriumBarFillX(pos), ContainerLayout.TAU_BAR_FILL_Y,
                                fill, ContainerLayout.TAU_BAR_FILL_H,
                                ContainerLayout.withAlpha(rgb, 1.0F));
                    }
                }
            }
            AspectTagRenderer.drawTag(guiGraphics, this.font,
                    this.leftPos + ContainerLayout.thaumatoriumTagX(pos),
                    this.topPos + ContainerLayout.TAU_TAG_Y,
                    aspect, (float) need, 0, ContainerLayout.TAU_TAG_ALPHA, false, pulseTime());
        }
    }

    /**
     * The current recipe's output at the original's box, greyed while the
     * queue will take it — the original tinted the item with
     * {@code glColor4f(0.5F, 0.5F, 0.5F, alpha)}; a {@code GuiGraphics}
     * overlay fill is the code-only stand-in for a modulation the API does
     * not expose (BD-19).
     */
    private void renderOutput(final GuiGraphics guiGraphics) {
        final ItemStack preview = this.menu.getPreviewStack();
        if (preview.isEmpty()) {
            return;
        }
        guiGraphics.renderItem(preview,
                this.leftPos + ContainerLayout.TAU_OUTPUT_X,
                this.topPos + ContainerLayout.TAU_OUTPUT_Y);
        if (this.menu.isQueueable()) {
            ScreenPanel.fillArea(guiGraphics, this.leftPos, this.topPos,
                    ContainerLayout.TAU_OUTPUT_X, ContainerLayout.TAU_OUTPUT_Y,
                    ContainerLayout.TAU_OUTPUT_SIZE, ContainerLayout.TAU_OUTPUT_SIZE,
                    ContainerLayout.withAlpha(ContainerLayout.TAU_DULL_GREY,
                            ContainerLayout.thaumatoriumDullPulse(pulseTime())));
        }
    }

    /**
     * The original's queue counter, drawn only when the cap is above 1 (a bare
     * thaumatorium never shows it): translate to {@code (136,33)}, scale 0.5,
     * draw {@code queueSize + "/" + maxRecipes} centred in white.
     */
    private void renderQueueCounter(final GuiGraphics guiGraphics) {
        final int max = this.menu.getMaxRecipes();
        if (max <= 1) {
            return;
        }
        final var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate((float) (this.leftPos + ContainerLayout.TAU_COUNT_X),
                (float) (this.topPos + ContainerLayout.TAU_COUNT_Y), 0.0F);
        pose.scale(ContainerLayout.TAU_COUNT_SCALE, ContainerLayout.TAU_COUNT_SCALE, 1.0F);
        guiGraphics.drawCenteredString(this.font,
                this.menu.getQueueSize() + "/" + max, 0, 0, ContainerLayout.TAU_COUNT_COLOUR);
        pose.popPose();
    }

    // --------------------------------------------------------- click gates

    /**
     * The original's {@code mouseClicked} hitboxes, gate for gate
     * ({@code GuiThaumatorium.java:211-269}): the 16x16 output box queues the
     * current recipe — only while the queue gate is open — and plays
     * {@code hhon} at 0.3; the two arrows page the recipe list and the two
     * boxes page the aspect window, each playing {@code cameraclack} at 0.4,
     * each with the original's own enabled condition.
     */
    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        final int visible = this.menu.getVisibleCount();
        final int index = this.menu.getIndex();
        if (visible <= 0 || index < 0 || index >= visible) {
            return false;
        }
        final int gx = (int) mouseX - this.leftPos;
        final int gy = (int) mouseY - this.topPos;

        if (inBox(gx, gy, ContainerLayout.TAU_OUTPUT_X, ContainerLayout.TAU_OUTPUT_Y,
                ContainerLayout.TAU_OUTPUT_SIZE, ContainerLayout.TAU_OUTPUT_SIZE)
                && this.menu.isQueueable()) {
            // The original sent the list index as the raw window button.
            sendButton(index);
            playSelect();
            return true;
        }
        if (visible > 1) {
            if (index > 0 && inBox(gx, gy, ContainerLayout.TAU_PAGE_X,
                    ContainerLayout.TAU_PAGE_PREV_Y, ContainerLayout.TAU_PAGE_W,
                    ContainerLayout.TAU_PAGE_H)) {
                sendButton(ThaumatoriumMenu.BUTTON_PREV_RECIPE);
                playClick();
                return true;
            }
            if (index < visible - 1 && inBox(gx, gy, ContainerLayout.TAU_PAGE_X,
                    ContainerLayout.TAU_PAGE_NEXT_Y, ContainerLayout.TAU_PAGE_W,
                    ContainerLayout.TAU_PAGE_H)) {
                sendButton(ThaumatoriumMenu.BUTTON_NEXT_RECIPE);
                playClick();
                return true;
            }
        }
        final int aspects = this.menu.getAspectCount();
        if (aspects > ContainerLayout.TAU_VISIBLE_ASPECTS) {
            final int start = this.menu.getStartAspect();
            if (start > 0 && inBox(gx, gy, ContainerLayout.TAU_ASPECT_LEFT_X,
                    ContainerLayout.TAU_ASPECT_PAGER_Y, ContainerLayout.TAU_ASPECT_PAGER_W,
                    ContainerLayout.TAU_ASPECT_PAGER_H)) {
                sendButton(ThaumatoriumMenu.BUTTON_ASPECT_LEFT);
                playClick();
                return true;
            }
            if (start < aspects - 1 && inBox(gx, gy, ContainerLayout.TAU_ASPECT_RIGHT_X,
                    ContainerLayout.TAU_ASPECT_PAGER_Y, ContainerLayout.TAU_ASPECT_PAGER_W,
                    ContainerLayout.TAU_ASPECT_PAGER_H)) {
                sendButton(ThaumatoriumMenu.BUTTON_ASPECT_RIGHT);
                playClick();
                return true;
            }
        }
        return false;
    }

    private void sendButton(final int buttonId) {
        if (this.minecraft != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    /** The original's {@code playButtonSelect}: {@code thaumcraft:hhon}, 0.3, 1.0. */
    private void playSelect() {
        playUiSound(ModSounds.HHON.get(), 0.3F);
    }

    /** The original's {@code playButtonClick}: {@code thaumcraft:cameraclack}, 0.4, 1.0. */
    private void playClick() {
        playUiSound(ModSounds.CAMERACLACK.get(), 0.4F);
    }

    /**
     * The original played these at the player's position in the world
     * ({@code playSoundAtEntity}); the port plays them as a UI sound — the
     * same two events, the same volumes, no echo off the panel (BD-19).
     */
    private void playUiSound(final net.minecraft.sounds.SoundEvent event, final float volume) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(event, volume, 1.0F));
        }
    }

    private static boolean inBox(final int x, final int y, final int boxX, final int boxY,
                                 final int width, final int height) {
        final int relX = x - boxX;
        final int relY = y - boxY;
        return relX >= 0 && relY >= 0 && relX < width && relY < height;
    }

    /**
     * The original's pulse clock — the client player's {@code ticksExisted}
     * ({@code field_70173_aa}); derived from the monotonic clock exactly as
     * {@code ArcaneWorkbenchScreen.pulseTime()} does.
     */
    private static int pulseTime() {
        return (int) (Util.getMillis() / 50L);
    }
}
