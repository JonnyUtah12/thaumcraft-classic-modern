package com.jonnyutah12.thaumcraftclassic.client.gui;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.network.payload.PlayerCompleteToServerPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchCategoryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ThaaumonomiconModel;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * TC-M3-03: the Thaumonomicon research browser — port of the original
 * {@code thaumcraft.client.gui.GuiResearchBrowser}.
 *
 * <p>Layout constants, atlas offsets, colors, pan bounds, hover hitboxes and
 * click semantics are taken verbatim from the original (per-block parity
 * comments; checklist in docs/visual-parity.md). All state decisions (which
 * categories/tabs, which nodes, which lines, node states) come from
 * {@link ThaumonomiconModel}; this class only draws and dispatches input.</p>
 *
 * <p>Known deviations (recorded in docs/visual-parity.md): vanilla font
 * instead of TCFontRenderer; static node icons instead of the original's
 * animated {@code cycleItemStack} variants; the highlightedItem particle
 * flash (the original's in-game item highlight feature) is not wired yet.</p>
 */
public class ThaumonomiconScreen extends Screen {

    /** Original paneWidth/paneHeight. */
    static final int PANE_W = 256;
    static final int PANE_H = 230;

    static final ResourceLocation RESEARCH_SHEET =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "textures/gui/gui_research.png");
    static final ResourceLocation NODES =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "textures/misc/nodes.png");

    /** Original static lastX/lastY: the browser reopens where it was left. */
    private static int lastCol = -5;
    private static int lastRow = -6;

    private double mapX;
    private double mapY;
    private double targetX;
    private double targetY;

    private String selectedCategory;

    private boolean dragging;
    private int lastMouseX;
    private int lastMouseY;

    private long popuptime;
    private String popupMessage = "";

    private ThaumonomiconModel.EntryView hovered;

    public ThaumonomiconScreen() {
        this(null, null);
    }

    /** Restore-position variant (original {@code GuiResearchBrowser(x, y)} from the book screen). */
    public ThaumonomiconScreen(final Double restoreX, final Double restoreY) {
        super(Component.empty());
        if (restoreX != null && restoreY != null) {
            this.mapX = this.targetX = restoreX;
            this.mapY = this.targetY = restoreY;
        } else {
            // Original: lastX * 24 - 141 / 2 - 12, lastY * 24 - 141 / 2.
            this.mapX = this.targetX = lastCol * 24 - 70;
            this.mapY = this.targetY = lastRow * 24 - 70;
        }
        final List<ResearchCategoryData> categories =
                ThaumonomiconModel.visibleCategories(ResearchManager.graph(), knowledge());
        this.selectedCategory = categories.isEmpty() ? null : categories.get(0).key();
    }

    private PlayerKnowledgeData knowledge() {
        return minecraft == null || minecraft.player == null
                ? PlayerKnowledgeData.empty()
                : minecraft.player.getData(ModAttachments.PLAYER_KNOWLEDGE);
    }

    private int difficulty() {
        return TcConfig.getInt(TcConfigCatalog.RESEARCH_DIFFICULTY);
    }

    // ----------------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        this.popuptime = System.currentTimeMillis() - 1L;
        final int paneLeft = (this.width - PANE_W) / 2;
        final int paneTop = (this.height - PANE_H) / 2;
        final int mapX0 = paneLeft + 16;
        final int mapY0 = paneTop + 17;

        if (this.hovered != null) {
            final ResearchEntryData entry = this.hovered.entry();
            final PlayerKnowledgeData knowledge = knowledge();
            if (!knowledge.researchKeys().contains(entry.key())
                    && ThaumonomiconModel.canUnlockForDisplay(knowledge, entry)) {
                // Original mouseClicked: purchase (secondary) or note request (primary).
                if (this.hovered.costMode() == ThaumonomiconModel.CostMode.SECONDARY) {
                    boolean enough = true;
                    for (final ResearchEntryData.AspectTag tag : entry.tags()) {
                        if (knowledge.aspectAmount(tag.aspect()) < tag.amount()) {
                            enough = false;
                            break;
                        }
                    }
                    if (enough) {
                        sendComplete(entry.key(), PlayerCompleteToServerPayload.REQUEST_COMPLETE);
                    }
                } else if (hasScribestuff() && researchSlot(entry.key()) == -1) {
                    sendComplete(entry.key(), PlayerCompleteToServerPayload.REQUEST_NOTE);
                    this.popuptime = System.currentTimeMillis() + 3000L;
                    this.popupMessage = TcGuiSupport.text("tc.research.popup",
                                    "Research note added to your inventory for %1$s")
                            .replace("%1$s", entryName(entry));
                }
                return true;
            }
            if (knowledge.researchKeys().contains(entry.key())) {
                // Original: open the book at page 0, carrying the map position.
                this.minecraft.setScreen(new ThaumonomiconBookScreen(entry.key(), 0, this.mapX, this.mapY));
                return true;
            }
        }

        // Category tabs (original hit test: 24x24 at paneLeft-24, count*24).
        final List<ResearchCategoryData> categories =
                ThaumonomiconModel.visibleCategories(ResearchManager.graph(), knowledge());
        int count = 0;
        for (final ResearchCategoryData category : categories) {
            final int tabLeft = paneLeft - 24 + (count >= 9 ? 280 : 0);
            final int tabTop = paneTop + (count % 9) * 24;
            if (mouseX >= tabLeft && mouseX < tabLeft + 24 && mouseY >= tabTop && mouseY < tabTop + 24) {
                this.selectedCategory = category.key();
                playButtonClick();
                return true;
            }
            count++;
        }

        // Press inside the map area starts a pan (original isMouseButtonDown 0→1).
        if (button == 0 && mouseX >= mapX0 && mouseX < mapX0 + 224 && mouseY >= mapY0 && mouseY < mapY0 + 196) {
            this.dragging = true;
            this.lastMouseX = (int) mouseX;
            this.lastMouseY = (int) mouseY;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button,
                               final double dragX, final double dragY) {
        if (this.dragging) {
            // Original: guiMapX -= (mx - mouseX); target snaps to current while dragging.
            this.targetX -= mouseX - this.lastMouseX;
            this.targetY -= mouseY - this.lastMouseY;
            this.mapX = this.targetX;
            this.mapY = this.targetY;
            this.lastMouseX = (int) mouseX;
            this.lastMouseY = (int) mouseY;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        this.dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        // Original onGuiClosed: remember the map position for the next open.
        lastCol = (int) ((this.mapX + 70 + 12.0) / 24.0);
        lastRow = (int) ((this.mapY + 70.0) / 24.0);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // original does not pause the game
    }

    private void sendComplete(final String key, final int requestType) {
        final ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null) {
            PacketDistributor.sendToServer(new PlayerCompleteToServerPayload(key, requestType));
        }
    }

    /** Original hasScribestuff: the player carries ink (scribing tools + paper check). */
    private boolean hasScribestuff() {
        return this.minecraft.player != null && this.minecraft.player.getInventory().countItem(Items.INK_SAC) > 0;
    }

    /**
     * Original {@code ResearchManager.getResearchSlot}: index of a research note
     * for this key in the inventory. Research notes are TC-M3-04 (not yet
     * ported), so there is never one — the primary path always offers a new note.
     */
    private int researchSlot(final String key) {
        return -1;
    }

    private void playButtonClick() {
        if (this.minecraft.player != null) {
            this.minecraft.level.playLocalSound(this.minecraft.player.getX(), this.minecraft.player.getY(),
                    this.minecraft.player.getZ(), ModSounds.CAMERACLACK.value(), SoundSource.PLAYERS,
                    0.4F, 1.0F, false);
        }
    }

    // ---------------------------------------------------------------------- render

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        // Original drawDefaultBackground (flat dark gradient, no world blur).
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0000000, 0xD0101010);

        // Original updateScreen easing (0.85/frame toward the target).
        final double dx = this.targetX - this.mapX;
        final double dy = this.targetY - this.mapY;
        if (dx * dx + dy * dy < 4.0) {
            this.mapX += dx;
            this.mapY += dy;
        } else {
            this.mapX += dx * 0.85;
            this.mapY += dy * 0.85;
        }

        final int paneLeft = (this.width - PANE_W) / 2;
        final int paneTop = (this.height - PANE_H) / 2;
        final int mapX0 = paneLeft + 16;
        final int mapY0 = paneTop + 17;

        final PlayerKnowledgeData knowledge = knowledge();
        final int difficulty = difficulty();
        final List<ResearchCategoryData> categories =
                ThaumonomiconModel.visibleCategories(ResearchManager.graph(), knowledge);
        if (this.selectedCategory == null || categories.stream().noneMatch(c -> c.key().equals(this.selectedCategory))) {
            this.selectedCategory = categories.isEmpty() ? null : categories.get(0).key();
        }
        if (this.selectedCategory == null) {
            return;
        }

        // Pan bounds (original updateResearch): minCol*24-85 / minRow*24-112 / maxCol*24-112 / maxRow*24-61.
        final ThaumonomiconModel.CategoryBounds bounds =
                ThaumonomiconModel.categoryBounds(ResearchManager.graph(), this.selectedCategory);
        final double mapTop = bounds.minColumn() * 24 - 85;
        final double mapLeft = bounds.minRow() * 24 - 112;
        final double mapBottom = bounds.maxColumn() * 24 - 112;
        final double mapRight = bounds.maxRow() * 24 - 61;
        this.targetX = clamp(this.targetX, mapTop, mapBottom - 1);
        this.targetY = clamp(this.targetY, mapLeft, mapRight - 1);
        final int scrollX = (int) clamp(this.mapX, mapTop, mapBottom - 1);
        final int scrollY = (int) clamp(this.mapY, mapLeft, mapRight - 1);

        final ResearchCategoryData category = categories.stream()
                .filter(c -> c.key().equals(this.selectedCategory)).findFirst().orElseThrow();

        // Panned category background (original: scale 2, UV = scroll fraction * 288/316, halved).
        final double spanX = Math.abs(mapTop - mapBottom);
        final double spanY = Math.abs(mapLeft - mapRight);
        final int vx = spanX == 0 ? 0 : (int) ((scrollX - mapTop) / spanX * 288.0F);
        final int vy = spanY == 0 ? 0 : (int) ((scrollY - mapLeft) / spanY * 316.0F);
        graphics.pose().pushPose();
        graphics.pose().translate(mapX0, mapY0, 0.0F);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.blit(TcGuiSupport.dataTexture(category.background()), 0, 0, vx / 2, vy / 2, 112, 98, 256, 256);
        graphics.pose().popPose();

        // Connection lines (before nodes, original order).
        for (final ThaumonomiconModel.LinkView link :
                ThaumonomiconModel.links(ResearchManager.graph(), knowledge, this.selectedCategory)) {
            final ResearchEntryData from = ResearchManager.graph().entry(link.from());
            final ResearchEntryData to = ResearchManager.graph().entry(link.to());
            if (from == null || to == null) {
                continue;
            }
            final int x1 = from.column() * 24 - scrollX + 11 + mapX0;
            final int y1 = from.row() * 24 - scrollY + 11 + mapY0;
            final int x2 = to.column() * 24 - scrollX + 11 + mapX0;
            final int y2 = to.row() * 24 - scrollY + 11 + mapY0;
            drawConnection(graphics, x1, y1, x2, y2, link.kind());
        }

        // Nodes.
        this.hovered = null;
        final long gameFrame = this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime();
        for (final ThaumonomiconModel.EntryView view :
                ThaumonomiconModel.visibleEntries(ResearchManager.graph(), knowledge, this.selectedCategory, difficulty)) {
            final ResearchEntryData entry = view.entry();
            final int nx = entry.column() * 24 - scrollX + mapX0;
            final int ny = entry.row() * 24 - scrollY + mapY0;
            if (nx < mapX0 - 24 || ny < mapY0 - 24 || nx > mapX0 + 224 || ny > mapY0 + 196) {
                continue;
            }
            drawNode(graphics, view, nx, ny, difficulty, gameFrame);
            if (mouseX >= mapX0 && mouseY >= mapY0 && mouseX < mapX0 + 224 && mouseY < mapY0 + 196
                    && mouseX >= nx && mouseX <= nx + 22 && mouseY >= ny && mouseY <= ny + 22) {
                this.hovered = view;
            }
        }

        // Category tabs.
        int count = 0;
        for (final ResearchCategoryData tab : categories) {
            final boolean selected = tab.key().equals(this.selectedCategory);
            final boolean swop = count >= 9;
            final int slot = count % 9;
            final int s0 = swop ? 264 : 0;
            final int s1 = selected ? 0 : 24;
            final int tabLeft = paneLeft - 24 + s0;
            final int tabTop = paneTop + slot * 24;
            graphics.blit(RESEARCH_SHEET, tabLeft, tabTop, 152 + s1, 232, 24, 24, 256, 256);
            graphics.blit(TcGuiSupport.dataTexture(tab.icon()), paneLeft - 19 + s0, tabTop + 4,
                    0, 0, 16, 16, 16, 16);
            if (!selected) {
                graphics.blit(RESEARCH_SHEET, tabLeft, tabTop, 200, 232, 24, 24, 256, 256);
            }
            if (mouseX >= tabLeft && mouseX < tabLeft + 24 && mouseY >= tabTop && mouseY < tabTop + 24) {
                graphics.drawString(this.font, categoryName(tab), mouseX, mouseY - 8, 0xFFFFFF);
            }
            count++;
        }

        // Pane frame on top (original draws it last).
        graphics.blit(RESEARCH_SHEET, paneLeft, paneTop, 0, 0, PANE_W, PANE_H, 256, 256);

        // Note popup (original popuptime window, 3s).
        if (this.popuptime > System.currentTimeMillis()) {
            final int cx = paneLeft + 128;
            final int cy = paneTop + 128;
            final List<net.minecraft.network.chat.FormattedText> lines = this.font.getSplitter()
                    .splitLines(TcGuiSupport.legacyText(this.popupMessage), 150, Style.EMPTY);
            final int halfHeight = lines.size() * this.font.lineHeight / 2;
            graphics.fill(cx - 78, cy - halfHeight - 3, cx + 78, cy + halfHeight + 3, 0xFF000000);
            int ly = cy - halfHeight;
            for (final net.minecraft.network.chat.FormattedText line : lines) {
                graphics.drawString(this.font, line, cx - this.font.width(line) / 2, ly, -7302913, false);
                ly += this.font.lineHeight;
            }
        }

        if (this.hovered != null) {
            drawTooltip(graphics, this.hovered, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // ------------------------------------------------------------------- drawing

    /** Original drawForbidden: animated warp-node strip from nodes.png, 80x80 at the node center. */
    private void drawForbidden(final GuiGraphics graphics, final int x, final int y, final long gameFrame) {
        final int frame = 31 - (int) (gameFrame % 32);
        // Original color 4456533 (0x440055), alpha 0.66.
        graphics.setColor(0x44 / 255.0F, 0.0F, 0x55 / 255.0F, 0.66F);
        graphics.blit(NODES, x - 40, y - 40, frame * 64, 320, 64, 64, 2048, 2048);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Original node pass: state shade, frame atlas selection, special overlay, icon. */
    private void drawNode(final GuiGraphics graphics, final ThaumonomiconModel.EntryView view,
                          final int nx, final int ny, final int difficulty, final long gameFrame) {
        final ResearchEntryData entry = view.entry();
        if (entry.warp() > 0) {
            drawForbidden(graphics, nx + 11, ny + 11, gameFrame);
        }
        final float shade = switch (view.state()) {
            case COMPLETED -> 1.0F;
            case UNLOCKABLE -> (float) (Math.sin(gameFrame % 600L / 600.0 * Math.PI * 2.0) * 0.25 + 0.75);
            case LOCKED -> 0.3F;
        };
        graphics.setColor(shade, shade, shade, 1.0F);
        final ThaumonomiconModel.NodeFrame frame = ThaumonomiconModel.nodeFrame(entry, difficulty);
        graphics.blit(RESEARCH_SHEET, nx - 2, ny - 2, frame.u(), frame.v(), 26, 26, 256, 256);
        if (frame.specialOverlay()) {
            graphics.blit(RESEARCH_SHEET, nx - 2, ny - 2,
                    ThaumonomiconModel.NodeFrame.SPECIAL_U, ThaumonomiconModel.NodeFrame.SPECIAL_V, 26, 26, 256, 256);
        }
        // Original: locked icons drop to 0.1 (item icons) / 0.2 (texture icons).
        if (view.state() == ThaumonomiconModel.EntryState.LOCKED) {
            graphics.setColor(0.1F, 0.1F, 0.1F, 1.0F);
        }
        drawIcon(graphics, entry, nx + 3, ny + 3);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Entry icon: item (resolved through the port's id normalization) or texture. */
    private void drawIcon(final GuiGraphics graphics, final ResearchEntryData entry, final int x, final int y) {
        final ResearchEntryData.Icon icon = entry.icon();
        if (icon == null || icon.kind() == null) {
            return;
        }
        if ("item".equals(icon.kind()) && icon.item() != null) {
            final String id = ResearchManager.normalizedItemId(icon.item());
            if (id == null) {
                return;
            }
            final Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            if (item != Items.AIR) {
                // Static first variant; the original's cycleItemStack animation is TC-M7 polish.
                graphics.renderItem(new ItemStack(item), x, y);
            }
        } else if ("texture".equals(icon.kind()) && icon.texture() != null) {
            graphics.blit(TcGuiSupport.dataTexture(icon.texture()), x, y, 0, 0, 16, 16, 16, 16);
        }
    }

    /**
     * Original drawLine: 3px GL line sampled every 2px; wiggling links offset
     * by sin((tick+a)/7)*5*(1-phase), color and alpha fade along the line
     * (alpha 0.6 base, * phase for wiggles).
     */
    private void drawConnection(final GuiGraphics graphics, final int x, final int y, final int x2, final int y2,
                                final ThaumonomiconModel.LinkView.LinkKind kind) {
        final float r;
        final float g;
        final float b;
        final boolean wiggle;
        switch (kind) {
            case DONE -> { r = 0.1F; g = 0.1F; b = 0.1F; wiggle = false; }
            case SIBLING_DONE -> { r = 0.1F; g = 0.1F; b = 0.2F; wiggle = false; }
            case GREEN -> { r = 0.0F; g = 1.0F; b = 0.0F; wiggle = true; }
            default -> { r = 0.0F; g = 0.0F; b = 1.0F; wiggle = true; }
        }
        final double d3 = x - x2;
        final double d4 = y - y2;
        final float dist = (float) Math.sqrt(d3 * d3 + d4 * d4);
        final int inc = Math.max(1, (int) (dist / 2.0F));
        final long tick = this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime();
        for (int a = 0; a <= inc; a++) {
            float r2 = r;
            float g2 = g;
            float b2 = b;
            float mx = 0.0F;
            float my = 0.0F;
            float op = 0.6F;
            if (wiggle) {
                final float phase = (float) a / inc;
                mx = (float) Math.sin((tick + a) / 7.0F) * 5.0F * (1.0F - phase);
                my = (float) Math.sin((tick + a) / 5.0F) * 5.0F * (1.0F - phase);
                r2 *= 1.0F - phase;
                g2 *= 1.0F - phase;
                b2 *= 1.0F - phase;
                op *= phase;
            }
            final int px = (int) (x - d3 / inc * a + mx);
            final int py = (int) (y - d4 / inc * a + my);
            final int argb = ((int) (op * 255.0F) << 24)
                    | ((int) (r2 * 255.0F) << 16) | ((int) (g2 * 255.0F) << 8) | (int) (b2 * 255.0F);
            graphics.fill(px - 1, py - 1, px + 2, py + 2, argb);
        }
    }

    /** Original hover tooltip block (locked variant vs. unlockable/complete variant). */
    private void drawTooltip(final GuiGraphics graphics, final ThaumonomiconModel.EntryView view,
                             final int mouseX, final int mouseY) {
        final ResearchEntryData entry = view.entry();
        final int tx = mouseX + 6;
        final int ty = mouseY - 4;
        final String name = entryName(entry);
        final boolean canUnlock = view.state() != ThaumonomiconModel.EntryState.LOCKED;

        if (!canUnlock) {
            final String missing = TcGuiSupport.text("tc.researchmissing", "Missing required research!");
            final int w = Math.max(this.font.width(name), (int) (this.font.width(missing) / 1.5F));
            final int h = this.font.lineHeight;
            graphics.fill(tx - 3, ty - 3, tx + w + 3, ty + h + 10, 0xF0000000);
            TcGuiSupport.drawScaledText(graphics, missing, tx, ty + 12, 0.5F, -9416624);
        } else {
            final boolean secondary = view.costMode() == ThaumonomiconModel.CostMode.SECONDARY;
            final boolean primary = view.costMode() == ThaumonomiconModel.CostMode.PRIMARY;
            final String hover = entryText(entry);
            int w = Math.max(this.font.width(name), (int) (this.font.width(hover) / 1.9F));
            int h = this.font.lineHeight + 5;
            if (primary) {
                h += 9;
                w = Math.max(w, (int) (this.font.width(TcGuiSupport.text("tc.research.shortprim",
                        "You need scribing tools and paper to get this research note")) / 1.9F));
            }
            if (secondary) {
                h += 29;
                w = Math.max(w, (int) (this.font.width(TcGuiSupport.text("tc.research.short",
                        "You do not have the required aspects to purchase this")) / 1.9F));
            }
            final int warp = Math.min(entry.warp(), 5);
            final String wte = TcGuiSupport.text("tc.forbidden", "§l§5Forbidden knowledge (%n)§0")
                    .replace("%n", TcGuiSupport.text("tc.forbidden.level." + warp, warpLevelName(warp)));
            if (entry.warp() > 0) {
                h += 9;
                w = Math.max(w, (int) (stripLegacy(wte) / 1.9F));
            }
            graphics.fill(tx - 3, ty - 3, tx + w + 3, ty + h + 6, 0xF0000000);

            // Hover text, 0.5× scale, wrapped to the box width (original drawSplitString pass).
            graphics.pose().pushPose();
            graphics.pose().translate(tx, ty + h - 1, 0.0F);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            int ly = 0;
            for (final net.minecraft.network.chat.FormattedText line : this.font.getSplitter()
                    .splitLines(TcGuiSupport.legacyText(hover), Math.max(1, w * 2), Style.EMPTY)) {
                graphics.drawString(this.font, line, 0, ly, -7302913, false);
                ly += this.font.lineHeight;
            }
            graphics.pose().popPose();

            if (entry.warp() > 0) {
                TcGuiSupport.drawScaledTextLegacy(graphics, wte, tx, ty + h + 8, 0.5F);
                h += 9;
            }

            if (primary) {
                final String line;
                final int color;
                if (researchSlot(entry.key()) >= 0) {
                    line = TcGuiSupport.text("tc.research.hasnote",
                            "You already have this research note in your inventory");
                    color = 16753920;
                } else if (hasScribestuff()) {
                    line = TcGuiSupport.text("tc.research.getprim",
                            "Click to get a research note for this research");
                    color = 8900331;
                } else {
                    line = TcGuiSupport.text("tc.research.shortprim",
                            "You need scribing tools and paper to get this research note");
                    color = 14423100;
                }
                TcGuiSupport.drawScaledText(graphics, line, tx, ty + h + 8, 0.5F, color);
            } else if (secondary) {
                final PlayerKnowledgeData knowledge = knowledge();
                boolean enough = true;
                int cc = 0;
                for (final ResearchEntryData.AspectTag tag : entry.tags()) {
                    if (knowledge.hasAspectDiscovered(tag.aspect())) {
                        float alpha = 1.0F;
                        if (knowledge.aspectAmount(tag.aspect()) < tag.amount()) {
                            final long tick = this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime();
                            alpha = (float) (Math.sin(tick % 600L / 600.0 * Math.PI * 2.0) * 0.25 + 0.75);
                            enough = false;
                        }
                        TcGuiSupport.drawTag(graphics, tag.aspect(), tx + cc * 16, ty + h + 8, tag.amount(), alpha);
                    } else {
                        enough = false;
                        graphics.setColor(0.5F, 0.5F, 0.5F, 0.5F);
                        graphics.blit(TcGuiSupport.aspectTexture("_unknown"), tx + cc * 16, ty + h + 8,
                                0, 0, 16, 16, 16, 16);
                        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }
                    cc++;
                }
                final String line = enough
                        ? TcGuiSupport.text("tc.research.purchase", "Click to purchase this research")
                        : TcGuiSupport.text("tc.research.short",
                                "You do not have the required aspects to purchase this");
                TcGuiSupport.drawScaledText(graphics, line, tx, ty + h + 27, 0.5F, enough ? 8900331 : 14423100);
            }
        }

        // Name last, on top (original colors: unlockable white/orange, locked grey).
        final boolean special = entry.flags() != null && entry.flags().special();
        final int nameColor = canUnlock ? (special ? -128 : -1) : (special ? -8355776 : -8355712);
        graphics.drawString(this.font, name, tx, ty, nameColor, false);
    }

    private static double stripLegacy(final String s) {
        return net.minecraft.client.Minecraft.getInstance().font.width(
                s.replaceAll("§.", ""));
    }

    private static String warpLevelName(final int level) {
        return switch (level) {
            case 1 -> "Mostly Harmless";
            case 2 -> "Minor";
            case 3 -> "Moderate";
            case 4 -> "Dangerous";
            case 5 -> "Taboo";
            default -> "";
        };
    }

    static String entryName(final ResearchEntryData entry) {
        return TcGuiSupport.text(entry.nameKey(), entry.nameEnUs());
    }

    static String entryText(final ResearchEntryData entry) {
        return TcGuiSupport.text(entry.textKey(), entry.textEnUs());
    }

    static String categoryName(final ResearchCategoryData category) {
        return TcGuiSupport.text(category.nameKey(), category.nameEnUs());
    }

    private static double clamp(final double value, final double min, final double max) {
        return Math.max(min, Math.min(max, value));
    }
}
