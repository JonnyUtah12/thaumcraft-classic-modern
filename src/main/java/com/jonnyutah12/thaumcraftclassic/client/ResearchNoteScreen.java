package com.jonnyutah12.thaumcraftclassic.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.menus.ResearchNoteMenu;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectCombinationToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPlaceToServerPayload;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Pixel;
import com.jonnyutah12.thaumcraftclassic.research.NotePuzzle;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector2i;

/**
 * The research-note screen (TC-M3-04-s-iii): the 1.21.1 port of
 * {@code client/gui/GuiResearchTable}, shown for the note item's container
 * ({@link ResearchNoteMenu}), so the content-mapping row
 * {@code thaumcraft:gui_research_table} flips with this card (BD-18's
 * "renders nothing headless" note is retired by it).
 *
 * <p>Every rectangle is the original's, taken from the decompiled screen: the
 * 255x255 panel (the original's height + 50, no background texture of its
 * own); the 5x5 aspect pool at (10, 40) of 16 px cells, listing every
 * DISCOVERED aspect sorted by name — an aspect whose pool is 0 is drawn faded
 * at a third alpha and stays pickable, as the original's amount-gated drawing
 * and ungated hit test allow ({@code GuiResearchTable:246}/{@code :272});
 * the page window {@code min(page * 2, 20)} with the 10x12 arrows at (0,
 * 128) / (100, 128); the two 16x16 select boxes at (13, 139) / (71, 139) with
 * the original's 32x32 release windows; the clear plates at (40, 139) / (98,
 * 139); the 20x20 combine plate at (38, 143) over its 28x14 dark backing at
 * (129, 142), with the original's 200 ms cooldown; the hex grid centred at
 * (161, 75) with {@code HEX_SIZE = 9} (BD-12, so the 56 px spacing and every
 * per-hex offset scale with it, and hex (0, 0) is the centre); the 22x24
 * {@code RESEARCHDUPE} copy button at (37, 5) and its copy-cost plate at (3,
 * 35, 101x35) with per-aspect costs of tag amount + copies, which the
 * original paints only while the pointer is over that button
 * ({@code :327}-{@code :366}); the ink-gated tooltip at (161 - 35 - width,
 * 75); the {@code RESEARCHER2} shift arm (a select box onto the other) and
 * the {@code RESEARCHER1} component preview; and the click/drag state machine
 * in the original's gate order.</p>
 *
 * <p>The client never mutates the note. A hex write, an erase and a combine
 * go out as the packets the original sent — {@code thaumcraft:aspect_place_to_server},
 * the erase as the same packet with no aspect, and
 * {@code thaumcraft:aspect_combination_to_server} — and the copy button is
 * {@code window_click 5}; the server ({@link ResearchNoteMenu}, reached
 * through {@code ServerNetworkHooks}) charges the ink and the aspect pool and
 * decides {@code complete} through {@code NotePuzzle.checkCompletion}, so a
 * client claim is only ever a request.</p>
 *
 * <p>Rendering is the BD-15 recovery plus its aspect-tag recovery: the
 * {@code scripts/sheetN.png} art is ARR-encrypted with no PNG shipped, so the
 * sheets are a flat panel, vanilla slot sprites and tinted hex fills — the
 * panel outline, the slot plates, the combine plate, the ink tooltip, the hex
 * fills and the veil all still communicate the layout — and an aspect tag
 * draws its {@code textures/aspects/<tag>.png} overlay sprite when that
 * resource exists and the aspect's own colour box when it does not. GL
 * primitives have no 1.21.1 GUI equivalent: the connection sweep draws as a
 * 1 px strip per segment (BD-19), the amount glyph's outline pass is
 * vanilla's drop shadow, and the drag orb (a 16x16 particle quad) is a flat
 * 16x16 fill in the aspect's colour.</p>
 */
public class ResearchNoteScreen extends AbstractContainerScreen<ResearchNoteMenu> {

    /** Original: the 255x255 panel. */
    private static final int PANEL_SIZE = 255;
    /** Original: {@code HEX_SIZE = 9} (BD-12). */
    private static final int HEX_SIZE = 9;
    /** Original: the hex grid centre {@code (x + 161, y + 75)}. */
    private static final int HEX_CENTRE_X = 161;
    private static final int HEX_CENTRE_Y = 75;
    /**
     * Original: the drawn grid is the 7x7 {@code HexPosition} loop. The port's
     * hex model is axial, so the region is the hex disc of radius 5 — the
     * smallest disc that covers every note the generator makes (its radius is
     * {@code 1 + min(3, complexity)}, so at most 4).
     */
    private static final int GRID_RADIUS = 5;

    private static final int POOL_X = 10;
    private static final int POOL_Y = 40;
    private static final int POOL_COLS = 5;
    private static final int POOL_ROWS = 5;
    private static final int CELL = 16;
    private static final int POOL_PAGE_SIZE = POOL_COLS * POOL_ROWS;
    /** Original: the pool page arrow hot zones, 10x12 at (0, 128) / (100, 128). */
    private static final int ARROW_Y = POOL_Y + 88;
    private static final int ARROW_W = 10;
    private static final int ARROW_H = 12;
    private static final int ARROW_LEFT_X = 0;
    private static final int ARROW_RIGHT_X = POOL_X + 90;

    /** Original: the 16x16 select boxes at (13, 139) and (71, 139). */
    private static final int SELECT_1_X = POOL_X + 3;
    private static final int SELECT_2_X = POOL_X + 61;
    private static final int SELECT_Y = POOL_Y + 99;
    /**
     * Original: the release test is {@code inside(mx - 20, my - 146, px, py,
     * 32, 32)} for select1 and {@code inside(mx - 58, my - 146, px, py, 32,
     * 32)} for select2 — a 32x32 window around each 16 px box, i.e. the box
     * widened 16 px on every side (the 10 px pool-cell margin of
     * {@code UtilsFX.inside} does not apply to these two tests).
     */
    private static final int SELECT_WINDOW = 16;

    /** Original: the 20x20 combine plate at (38, 143). */
    private static final int COMBINE_X = POOL_X + 28;
    private static final int COMBINE_Y = POOL_Y + 103;
    private static final int COMBINE_SIZE = 20;
    /** Original: {@code drawRect(129, 142, 157, 156, 0xFF000000 | 13158600)}. */
    private static final int COMBINE_BACK_X = POOL_X + 119;
    private static final int COMBINE_BACK_Y = POOL_Y + 102;
    private static final int COMBINE_BACK_W = 28;
    private static final int COMBINE_BACK_H = 14;
    /** Original: the clear-slot plates at (40, 139) and (98, 139). */
    private static final int CLEAR_1_X = POOL_X + 30;
    private static final int CLEAR_2_X = POOL_X + 88;
    private static final int CLEAR_Y = POOL_Y + 99;

    /** Original: the {@code RESEARCHDUPE} button, 22x24 at (37, 5). */
    private static final int DUPE_X = 37;
    private static final int DUPE_Y = 5;
    private static final int DUPE_W = 22;
    private static final int DUPE_H = 24;
    /** Original: the copy-cost plate (3, 35) size (101, 35). */
    private static final int COPY_PLATE_X = 3;
    private static final int COPY_PLATE_Y = 35;
    private static final int COPY_PLATE_W = 101;
    private static final int COPY_PLATE_H = 35;
    /**
     * Original: the no-ink tooltip at {@code (x + 157 - stringWidth, y + 84)}
     * — 157 = the hex centre column + (-4), 84 = the hex centre row + 9.
     */
    private static final int NOINK_ANCHOR_X = 157;
    private static final int NOINK_ANCHOR_Y = 84;
    /** The vanilla player-panel label, above the menu's hotbar row (y 233). */
    private static final int INVENTORY_LABEL_X = 8;
    private static final int INVENTORY_LABEL_Y = 224;

    private static final int PANEL_FILL = 0xFF1B120A;
    private static final int PANEL_FRAME = 0xFF5A4632;
    private static final int PLATE = 0xFF2A2A2A;
    private static final int COMBINE_BACK = 0xFFC8C8C8;
    private static final int BUTTON_FILL = 0xFF8B6D4F;
    private static final int BUTTON_HILITE = 0xFFC7A87E;
    private static final int HEX_FILL = 0x40FFFFFF;
    private static final int HEX_OUTLINE = 0x60FFFFFF;
    private static final int HEX_HILITE = 0xB0FFFFFF;
    private static final int VEIL = 0x80000000;
    /** Original: the connection line colour 0, 0.6, 0.8 at 3 px (BD-19). */
    private static final int LINE_COLOUR = 0xFF0099CC;
    private static final int TEXT = 0xFFFFFFFF;
    /**
     * Original: the {@code alpha(0.33f)} fade of an amount-0 pool entry;
     * 1.21.1 GUI colours carry alpha, so 0.33 is multiplied in by hand.
     */
    private static final int FADED_ALPHA = 85;

    /** The vanilla slot plate for the pool cells and select boxes. */
    private static final ResourceLocation CREATIVE_TABS =
            ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tabs.png");
    private static final int SLOT_SPRITE_U = 28;
    private static final int SLOT_SPRITE_V = 84;

    /**
     * Original: {@code renderedEffects & 4} — the 200 ms combine cooldown that
     * {@code Gui.updateScreen} ticks down by 50 per frame.
     */
    private int combineCooldownMs;
    /** Original: the pool page (the window is {@code min(page * 2, 20)}). */
    private int page;
    private Aspect draggedAspect;
    private Aspect select1;
    private Aspect select2;
    /** Aspect overlay sprites probed once each (BD-15 recovery). */
    private final Set<ResourceLocation> spritePresent = new HashSet<>();
    private final Set<ResourceLocation> spriteMissing = new HashSet<>();

    public ResearchNoteScreen(final ResearchNoteMenu menu, final net.minecraft.world.entity.player.Inventory inventory,
                              final Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_SIZE;
        this.imageHeight = PANEL_SIZE;
        // The original draws no window title on this panel (the scribe slot,
        // the note slot and the copy button own the top strip), so the title
        // is suppressed and only the player-panel caption is drawn.
        this.titleLabelX = INVENTORY_LABEL_X;
        this.titleLabelY = INVENTORY_LABEL_Y;
        this.inventoryLabelX = INVENTORY_LABEL_X;
        this.inventoryLabelY = INVENTORY_LABEL_Y;
    }

    @Override
    protected void renderLabels(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        // The 1.7.10 screen draws neither the window title nor a player-row
        // caption; the caption is kept as the 1.21.1 convention for the panel
        // of player slots, above the menu's hotbar row.
        guiGraphics.drawString(this.font, this.playerInventoryTitle, INVENTORY_LABEL_X, INVENTORY_LABEL_Y,
                0x5F5F5F, false);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        this.combineCooldownMs = Math.max(0, this.combineCooldownMs - 50);
        // AbstractContainerScreen renders the queued tooltip at the END of its
        // own pass, so the tooltip is queued before super.render (as the
        // original's drawScreen ordering puts its tooltips after the panels
        // and before the drag orb).
        queueTooltip(guiGraphics, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.draggedAspect != null) {
            // Original: the 16x16 drag orb, centred on the cursor, drawn last.
            guiGraphics.fill(mouseX - 8, mouseY - 8, mouseX + 8, mouseY + 8,
                    withAlpha(this.draggedAspect.getColor(), 255));
        }
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick,
                            final int mouseX, final int mouseY) {
        final int gx = this.leftPos;
        final int gy = this.topPos;
        final int mx = mouseX - gx;
        final int my = mouseY - gy;
        final ResearchNoteMenu menu = this.menu;
        final ResearchNoteData note = noteFromSlot();
        final int copies = note == null ? 0 : note.copies();

        // The sheets are ARR art, so the panel is a flat plate (BD-15).
        guiGraphics.fill(gx, gy, gx + PANEL_SIZE, gy + PANEL_SIZE, PANEL_FILL);
        guiGraphics.renderOutline(gx, gy, PANEL_SIZE, PANEL_SIZE, PANEL_FRAME);

        // ------------------------------------------------------ aspect pool
        final List<Aspect> pool = poolAspects();
        final int window = windowFor(this.page);
        final int from = window * POOL_PAGE_SIZE;
        for (int i = from; i < pool.size() && i < from + POOL_PAGE_SIZE; i++) {
            final Aspect aspect = pool.get(i);
            final int x = gx + POOL_X + (i - from) % POOL_COLS * CELL;
            final int y = gy + POOL_Y + (i - from) / POOL_COLS * CELL;
            drawTag(guiGraphics, x, y, aspect, menu.aspectPoolAmount(aspect.getTag()) == 0 ? FADED_ALPHA : 255);
            drawAmount(guiGraphics, x, y, Integer.toString(menu.aspectPoolAmount(aspect.getTag())));
        }

        // select boxes, their tags, and the clear plates
        blitSlotPlate(guiGraphics, gx + SELECT_1_X, gy + SELECT_Y);
        blitSlotPlate(guiGraphics, gx + SELECT_2_X, gy + SELECT_Y);
        blitSlotPlate(guiGraphics, gx + CLEAR_1_X, gy + CLEAR_Y);
        blitSlotPlate(guiGraphics, gx + CLEAR_2_X, gy + CLEAR_Y);
        drawTag(guiGraphics, gx + SELECT_1_X, gy + SELECT_Y, this.select1, 255);
        drawTag(guiGraphics, gx + SELECT_2_X, gy + SELECT_Y, this.select2, 255);

        // the combine plate: the 28x14 dark backing, then the 20x20 plate
        guiGraphics.fill(gx + COMBINE_BACK_X, gy + COMBINE_BACK_Y,
                gx + COMBINE_BACK_X + COMBINE_BACK_W, gy + COMBINE_BACK_Y + COMBINE_BACK_H, COMBINE_BACK);
        final boolean combineHot = inside(mx, my, COMBINE_X, COMBINE_Y, COMBINE_SIZE, COMBINE_SIZE);
        guiGraphics.fill(gx + COMBINE_X, gy + COMBINE_Y, gx + COMBINE_X + COMBINE_SIZE,
                gy + COMBINE_Y + COMBINE_SIZE, combineHot && this.combineCooldownMs == 0 ? BUTTON_HILITE : BUTTON_FILL);
        guiGraphics.renderOutline(gx + COMBINE_X, gy + COMBINE_Y, COMBINE_SIZE, COMBINE_SIZE, PANEL_FRAME);

        // the RESEARCHDUPE plate, and the copy-cost plate the original paints
        // only while the pointer is over that button
        final boolean dupeHot = inside(mx, my, DUPE_X, DUPE_Y, DUPE_W, DUPE_H);
        if (menu.canDuplicate()) {
            guiGraphics.fill(gx + DUPE_X, gy + DUPE_Y, gx + DUPE_X + DUPE_W, gy + DUPE_Y + DUPE_H,
                    dupeHot ? BUTTON_HILITE : BUTTON_FILL);
            guiGraphics.renderOutline(gx + DUPE_X, gy + DUPE_Y, DUPE_W, DUPE_H, PANEL_FRAME);
        }
        if (menu.canDuplicate() && dupeHot) {
            drawCopyCostPlate(guiGraphics, gx, gy, note, copies);
        }

        // ----------------------------------------------------------- hexes
        final int cx = gx + HEX_CENTRE_X;
        final int cy = gy + HEX_CENTRE_Y;
        final List<Hex> grid = new ArrayList<>(HexUtils.generateHexes(GRID_RADIUS).values());
        final Set<String> drawn = note == null ? Set.of() : note.hexes().keySet();

        // pass 1 — the empty hex plates (the hex2 sprite at 0.25 alpha) and
        // the hover highlight of a hex that is neither drawn nor connected
        for (final Hex hex : grid) {
            final Pixel pix = hex.toPixel(HEX_SIZE);
            final int hx = cx + (int) Math.round(pix.x()) - 8;
            final int hy = cy + (int) Math.round(pix.y()) - 8;
            final ResearchNoteData.HexEntry entry = note == null ? null : note.entryAt(hex);
            final boolean connected = entry != null && !entry.empty();
            guiGraphics.fill(hx, hy, hx + CELL, hy + CELL, HEX_FILL);
            guiGraphics.renderOutline(hx, hy, CELL, CELL, HEX_OUTLINE);
            if (this.select1 != null && !connected && !drawn.contains(hex.toString())
                    && inside(mx - HEX_CENTRE_X, my - HEX_CENTRE_Y, (int) Math.round(pix.x()) - 8,
                            (int) Math.round(pix.y()) - 8, CELL, CELL)) {
                guiGraphics.fill(hx, hy, hx + CELL, hy + CELL, HEX_HILITE);
            }
        }

        // pass 2 — the connection sweep and the veil over undiscovered hexes
        final List<Hex> sweep = connectionSweep(note);
        for (int i = 0; i + 1 < sweep.size(); i++) {
            final Pixel a = sweep.get(i).toPixel(HEX_SIZE);
            final Pixel b = sweep.get(i + 1).toPixel(HEX_SIZE);
            drawLine(guiGraphics, cx + (int) Math.round(a.x()), cy + (int) Math.round(a.y()),
                    cx + (int) Math.round(b.x()), cy + (int) Math.round(b.y()));
        }
        if (note != null) {
            for (final Hex hex : grid) {
                final ResearchNoteData.HexEntry entry = note.entryAt(hex);
                if ((entry == null || entry.empty()) && !drawn.contains(hex.toString())) {
                    final Pixel pix = hex.toPixel(HEX_SIZE);
                    final int hx = cx + (int) Math.round(pix.x()) - 8;
                    final int hy = cy + (int) Math.round(pix.y()) - 8;
                    guiGraphics.fill(hx, hy, hx + CELL, hy + CELL, VEIL);
                }
            }
        }

        // pass 3 — the aspect tags, each a 16x16 box centred on its hex
        for (final Hex hex : grid) {
            final ResearchNoteData.HexEntry entry = note == null ? null : note.entryAt(hex);
            final Aspect aspect = entry == null ? null : Aspect.getAspect(entry.aspectTag());
            if (aspect != null) {
                final Pixel pix = hex.toPixel(HEX_SIZE);
                drawTag(guiGraphics, cx + (int) Math.round(pix.x()) - 8, cy + (int) Math.round(pix.y()) - 8,
                        aspect, 255);
            }
        }

        // the page arrows (the original paints the field, then the arrow)
        if (window < pages(pool.size()) - 1) {
            guiGraphics.fill(gx + ARROW_RIGHT_X, gy + POOL_Y + 26, gx + ARROW_RIGHT_X + ARROW_W,
                    gy + POOL_Y + 120, PANEL_FILL);
            guiGraphics.fill(gx + ARROW_RIGHT_X, gy + ARROW_Y, gx + ARROW_RIGHT_X + ARROW_W, gy + ARROW_Y + ARROW_H,
                    inside(mx, my, ARROW_RIGHT_X, ARROW_Y, ARROW_W, ARROW_H) ? TEXT : 0xFFAAAAAA);
        }
        if (window > 0) {
            guiGraphics.fill(gx + ARROW_LEFT_X, gy + POOL_Y + 26, gx + ARROW_LEFT_X + ARROW_W,
                    gy + POOL_Y + 120, PANEL_FILL);
            guiGraphics.fill(gx + ARROW_LEFT_X, gy + ARROW_Y, gx + ARROW_LEFT_X + ARROW_W, gy + ARROW_Y + ARROW_H,
                    inside(mx, my, ARROW_LEFT_X, ARROW_Y, ARROW_W, ARROW_H) ? TEXT : 0xFFAAAAAA);
        }
    }

    /**
     * Original: the copy-cost plate — the dark plate, the
     * {@code tc.research.copy} header, and per tag the aspect tag box plus the
     * cost glyph at {@code amount + copies}, laid out on the original's
     * 12 + i * (width / total) + 5 / 50 + (i / 4) * 16 grid.
     */
    private void drawCopyCostPlate(final GuiGraphics guiGraphics, final int gx, final int gy,
                                   final ResearchNoteData note, final int copies) {
        guiGraphics.fill(gx + COPY_PLATE_X, gy + COPY_PLATE_Y,
                gx + COPY_PLATE_X + COPY_PLATE_W, gy + COPY_PLATE_Y + COPY_PLATE_H, PLATE);
        guiGraphics.drawString(this.font, Component.translatable("tc.research.copy"),
                gx + COPY_PLATE_X + 11, gy + COPY_PLATE_Y + 5, TEXT, false);
        final ResearchEntryData data = note == null ? null : ResearchManager.graph().entry(note.key());
        if (data == null || data.tags() == null) {
            return;
        }
        final int total = data.tags().size() * 2 + 3;
        for (int i = 0; i < data.tags().size(); i++) {
            final ResearchEntryData.AspectTag tag = data.tags().get(i);
            final Aspect aspect = Aspect.getAspect(tag.aspect());
            if (aspect == null) {
                continue;
            }
            final int px = gx + COPY_PLATE_X + 12 + i * COPY_PLATE_W / total + 5;
            final int py = gy + COPY_PLATE_Y + 15 + i / 4 * 16;
            drawTag(guiGraphics, px, py, aspect, 255);
            drawAmount(guiGraphics, px, py, Integer.toString(tag.amount() + copies));
        }
    }

    /**
     * Original: {@code UtilsFX.drawTag(x, y, aspect, 1.0, alpha)} — the 32x32
     * aspect sheet drawn centred on the 16 px cell (a -8 offset at 2x scale).
     * The overlay PNGs are ARR art, so a box in the aspect's own colour stands
     * in when the resource is absent (BD-15).
     */
    private void drawTag(final GuiGraphics guiGraphics, final int x, final int y,
                         final Aspect aspect, final int alpha) {
        if (aspect == null) {
            return;
        }
        final ResourceLocation image = aspect.getImage();
        if (hasResource(image)) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha / 255.0F);
            guiGraphics.blit(image, x - 8, y - 8, 0.0F, 0.0F, CELL * 2, CELL * 2, CELL * 2, CELL * 2);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            guiGraphics.fill(x, y, x + CELL, y + CELL, withAlpha(aspect.getColor(), alpha));
        }
    }

    /**
     * Original: {@code UtilsFX.drawNumber} at half scale — the glyph's right
     * edge sits on the 16 px cell's right edge, centred on its bottom half.
     * The outline pass is the vanilla drop shadow (BD-19 recovery).
     */
    private void drawAmount(final GuiGraphics guiGraphics, final int x, final int y, final String text) {
        final int width = this.font.width(text);
        final var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate((float) (x + CELL), (float) (y + CELL), 0.0F);
        pose.scale(0.5F, 0.5F, 1.0F);
        guiGraphics.drawString(this.font, text, -width, -this.font.lineHeight, 0xFFFFFF, true);
        pose.popPose();
    }

    /** BD-19: the connection segment as a 1 px strip (the original is a 3 px GL line). */
    private void drawLine(final GuiGraphics guiGraphics, final int x1, final int y1,
                          final int x2, final int y2) {
        final int dx = x2 - x1;
        final int dy = y2 - y1;
        final int steps = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));
        for (int i = 0; i <= steps; i++) {
            final int x = x1 + dx * i / steps;
            final int y = y1 + dy * i / steps;
            guiGraphics.fill(x, y, x + 1, y + 1, LINE_COLOUR);
        }
    }

    private void blitSlotPlate(final GuiGraphics guiGraphics, final int x, final int y) {
        guiGraphics.blit(CREATIVE_TABS, x, y, SLOT_SPRITE_U, SLOT_SPRITE_V, CELL, CELL);
    }

    // --------------------------------------------------------------- tooltip

    private void queueTooltip(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        final int gx = this.leftPos;
        final int gy = this.topPos;
        final int mx = mouseX - gx;
        final int my = mouseY - gy;
        final ResearchNoteMenu menu = this.menu;
        final Slot hovered = getSlotUnderMouse();
        final int scribeIndex = ResearchNoteMenu.SLOT_SCRIBE;

        // Original: the no-ink tooltip is gated on the SLOT under the pointer
        // — the scribe slot (14, 10) shows the empty-tool line, the note slot
        // (70, 10) the refill line — and blocks every other tooltip.
        if (!menu.hasInk() && hovered != null
                && (hovered.index == scribeIndex || hovered.index == ResearchNoteMenu.SLOT_NOTE)) {
            final Component line = Component.translatable(
                    hovered.index == scribeIndex ? "tile.researchtable.noink.0" : "tile.researchtable.noink.1");
            setNextTooltip(List.of(line), NOINK_ANCHOR_X - gx - this.font.width(line), NOINK_ANCHOR_Y - gy);
            return;
        }

        // Original: the copy button's own tooltip (the cost plate is drawn in
        // renderBg), then a hovered pool cell / select box shows that aspect.
        if (menu.canDuplicate() && inside(mx, my, DUPE_X, DUPE_Y, DUPE_W, DUPE_H)) {
            setNextTooltip(List.of(Component.translatable("tc.research.copy")), mouseX + 4, mouseY + 4);
            return;
        }
        final Aspect hoveredAspect = hoveredAspect(gx, gy, mouseX, mouseY);
        if (hoveredAspect != null) {
            // Original: aspect.getName() + getLocalizedDescription() = the
            // tc.aspect.<tag> key. That key family ships with the aspect lang
            // card, so the second line renders as its key until then — the
            // same note the Thaumonomicon screen carries for its captions.
            setNextTooltip(List.of(Component.literal(hoveredAspect.getName()),
                    Component.translatable("tc.aspect." + hoveredAspect.getTag())), mouseX, mouseY - 8);
            return;
        }

        // Original: a RESEARCHER1 hover over the grid previews the recipe's
        // two components (the _back plates being ARR art, so only the tags).
        if (knowsResearch("RESEARCHER1") && new Pixel(mx - HEX_CENTRE_X, my - HEX_CENTRE_Y).toHex(HEX_SIZE) != null) {
            final List<Aspect> components = noteComponents(noteFromSlot());
            if (!components.isEmpty()) {
                final List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(components.get(0).getName()));
                if (components.size() > 1) {
                    lines.add(Component.literal(components.get(1).getName()));
                }
                setNextTooltip(lines, mouseX + 8, mouseY + 8);
            }
        }
    }

    /** The pool cell / select box the pointer is over, honouring the page window. */
    private Aspect hoveredAspect(final int gx, final int gy, final int mouseX, final int mouseY) {
        final int mx = mouseX - gx;
        final int my = mouseY - gy;
        final List<Aspect> pool = poolAspects();
        final int from = windowFor(this.page) * POOL_PAGE_SIZE;
        for (int i = from; i < pool.size() && i < from + POOL_PAGE_SIZE; i++) {
            final int x = POOL_X + (i - from) % POOL_COLS * CELL;
            final int y = POOL_Y + (i - from) / POOL_COLS * CELL;
            if (inside(mx, my, x, y, CELL, CELL)) {
                return pool.get(i);
            }
        }
        if (inside(mx, my, SELECT_1_X, SELECT_Y, CELL, CELL)) {
            return this.select1;
        }
        if (inside(mx, my, SELECT_2_X, SELECT_Y, CELL, CELL)) {
            return this.select2;
        }
        return null;
    }

    private void setNextTooltip(final List<Component> lines, final int x, final int y) {
        final List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (final Component line : lines) {
            wrapped.addAll(this.font.split(line, 120));
        }
        // The original's drawCustomTooltip pins its box at an anchor, not at
        // the cursor, so the positioner returns a fixed point.
        final ClientTooltipPositioner positioner = (screenWidth, screenHeight, cursorX, cursorY, tipW, tipH) ->
                new Vector2i(Math.max(4, Math.min(x, screenWidth - tipW - 4)), Math.max(4, y));
        setTooltipForNextRenderPass(wrapped, positioner, true);
    }

    // ------------------------------------------------------ mouse (original)

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final int mx = (int) mouseX - this.leftPos;
        final int my = (int) mouseY - this.topPos;
        final ResearchNoteMenu menu = this.menu;

        // Original: the RESEARCHDUPE copy button (22x24 at (37, 5)) —
        // window_click 5, so the server pays ink, book and coal and owns the
        // copy count.
        if (inside(mx, my, DUPE_X, DUPE_Y, DUPE_W, DUPE_H)) {
            if (menu.canDuplicate() && this.combineCooldownMs == 0) {
                this.combineCooldownMs = 200;
                playLocal(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.3F, 1.0F);
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                        ResearchNoteMenu.BUTTON_DUPLICATE);
            }
            return true;
        }

        // Original: the combine plate (20x20 at (38, 143)) and its 200 ms gate.
        if (inside(mx, my, COMBINE_X, COMBINE_Y, COMBINE_SIZE, COMBINE_SIZE)) {
            combine();
            return true;
        }

        // Original: the RESEARCHER2 shift arm — a select box onto the other.
        if (knowsResearch("RESEARCHER2") && this.select1 != null && this.select2 != null
                && (overSelect1(mx, my) || overSelect2(mx, my))) {
            final Aspect source = overSelect1(mx, my) ? this.select1 : this.select2;
            final Aspect target = overSelect1(mx, my) ? this.select2 : this.select1;
            this.select1 = null;
            this.select2 = null;
            playLocal(ModSounds.HHOFF.value(), 0.2F, 0.6F + (float) Math.random() * 0.4F);
            if (target != source) {
                sendCombination(target, source);
            }
            return true;
        }

        // Original: the page arrows.
        if (inside(mx, my, ARROW_RIGHT_X, ARROW_Y, ARROW_W, ARROW_H)) {
            this.page = Math.min(10, this.page + 1);
            playLocal(ModSounds.CAMERACLACK.value(), 0.4F, 1.0F);
            return true;
        }
        if (inside(mx, my, ARROW_LEFT_X, ARROW_Y, ARROW_W, ARROW_H)) {
            this.page = Math.max(0, this.page - 1);
            playLocal(ModSounds.CAMERACLACK.value(), 0.4F, 1.0F);
            return true;
        }

        // Original: a pool press starts a drag (every DISCOVERED aspect is
        // listed; the faded amount-0 entries stay pickable).
        final List<Aspect> pool = poolAspects();
        final int from = windowFor(this.page) * POOL_PAGE_SIZE;
        for (int i = from; i < pool.size() && i < from + POOL_PAGE_SIZE; i++) {
            final int x = POOL_X + (i - from) % POOL_COLS * CELL;
            final int y = POOL_Y + (i - from) / POOL_COLS * CELL;
            if (inside(mx, my, x, y, CELL, CELL)) {
                this.draggedAspect = pool.get(i);
                playLocal(ModSounds.CAMERACLACK.value(), 0.4F, 1.0F);
                return true;
            }
        }

        // Original: pick up the contents of a select box.
        if (overSelect1(mx, my) && this.select1 != null) {
            this.draggedAspect = this.select1;
            this.select1 = null;
            playLocal(ModSounds.CAMERACLACK.value(), 0.4F, 1.0F);
            return true;
        }
        if (overSelect2(mx, my) && this.select2 != null) {
            this.draggedAspect = this.select2;
            this.select2 = null;
            playLocal(ModSounds.CAMERACLACK.value(), 0.4F, 1.0F);
            return true;
        }

        // Original: the clear plates empty a select box.
        if (inside(mx, my, CLEAR_1_X, CLEAR_Y, CELL, CELL)) {
            this.select1 = null;
            playLocal(ModSounds.KEY.value(), 0.3F, 1.0F);
            return true;
        }
        if (inside(mx, my, CLEAR_2_X, CLEAR_Y, CELL, CELL)) {
            this.select2 = null;
            playLocal(ModSounds.KEY.value(), 0.3F, 1.0F);
            return true;
        }

        final Hex hex = new Pixel(mx - HEX_CENTRE_X, my - HEX_CENTRE_Y).toHex(HEX_SIZE);
        if (hex != null) {
            final ResearchNoteData note = noteFromSlot();
            final ResearchNoteData.HexEntry entry = note == null ? null : note.entryAt(hex);
            final Aspect hexAspect = entry == null ? null : Aspect.getAspect(entry.aspectTag());
            // Original: the erase arm — RESEARCHER1 known, RESEARCHER2 gated,
            // the erased aspect's pool must be positive; the 0.25 / 0.5 refund
            // roll is the server's.
            if (knowsResearch("RESEARCHER1") && !knowsResearch("RESEARCHER2")
                    && hexAspect != null && this.menu.aspectPoolAmount(hexAspect.getTag()) > 0) {
                playLocal(ModSounds.ERASE.value(), 0.2F, 0.5F + (float) Math.random() * 0.5F);
                sendPlace(hex, null);
                return true;
            }
            // Original: pick the written aspect back off a hex.
            if (this.draggedAspect == null && hexAspect != null) {
                this.draggedAspect = hexAspect;
                sendPlace(hex, null);
                playLocal(ModSounds.CAMERACLACK.value(), 0.4F, 1.0F);
                return true;
            }
        }

        // Original: vanilla slot handling for the scribe / note / player slots.
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (this.draggedAspect == null) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        final int mx = (int) mouseX - this.leftPos;
        final int my = (int) mouseY - this.topPos;

        // Original: drop on select1 / select2 (the 32x32 release windows).
        if (overSelect1(mx, my)) {
            if (this.select1 != null && this.select1 == this.draggedAspect) {
                this.select1 = null;
            } else if (this.select1 == null) {
                this.select1 = this.draggedAspect;
            } else if (this.select2 == null) {
                this.select2 = this.draggedAspect;
            }
            this.draggedAspect = null;
            playLocal(ModSounds.HHON.value(), 0.3F, 1.0F);
            return true;
        }
        if (overSelect2(mx, my)) {
            if (this.select2 != null && this.select2 == this.draggedAspect) {
                this.select2 = null;
            } else if (this.select2 == null) {
                this.select2 = this.draggedAspect;
            }
            this.draggedAspect = null;
            playLocal(ModSounds.HHON.value(), 0.3F, 1.0F);
            return true;
        }

        final Hex hex = new Pixel(mx - HEX_CENTRE_X, my - HEX_CENTRE_Y).toHex(HEX_SIZE);
        if (hex != null) {
            final ResearchNoteData note = noteFromSlot();
            final ResearchNoteData.HexEntry entry = note == null ? null : note.entryAt(hex);
            final Aspect hexAspect = entry == null ? null : Aspect.getAspect(entry.aspectTag());
            if (hexAspect != null && hexAspect == this.draggedAspect) {
                // Original: dropping the aspect a hex already holds selects it.
                if (this.select1 == null) {
                    this.select1 = hexAspect;
                } else if (this.select2 == null) {
                    this.select2 = hexAspect;
                }
                this.draggedAspect = null;
                playLocal(ModSounds.HHON.value(), 0.3F, 1.0F);
                return true;
            }
            if (hexAspect == null) {
                if (this.select1 == null && this.select2 == null) {
                    this.select1 = this.draggedAspect;
                    this.draggedAspect = null;
                    playLocal(ModSounds.HHON.value(), 0.3F, 1.0F);
                    return true;
                }
                // Original: the hex write, as the original's packet. The server
                // charges the pool and the ink and validates completion.
                playLocal(ModSounds.WRITE.value(), 0.2F, 0.5F + (float) Math.random() * 0.5F);
                sendPlace(hex, this.draggedAspect);
                this.draggedAspect = null;
                return true;
            }
        }

        if (this.draggedAspect != null) {
            this.draggedAspect = null;
            playLocal(ModSounds.HHOFF.value(), 0.2F, 0.6F + (float) Math.random() * 0.4F);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        // Original: Escape clears both select boxes (the GUI has no clear arm
        // beyond the plates at (40, 139) / (98, 139)).
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (this.select1 != null || this.select2 != null) {
                this.select1 = null;
                this.select2 = null;
                playLocal(ModSounds.KEY.value(), 0.3F, 1.0F);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            if (this.select1 != null || this.select2 != null) {
                this.select1 = null;
                this.select2 = null;
                playLocal(ModSounds.KEY.value(), 0.3F, 1.0F);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        // Original: the wheel pages the pool while the pointer is over it.
        final int mx = (int) mouseX - this.leftPos;
        final int my = (int) mouseY - this.topPos;
        if (inside(mx, my, POOL_X, POOL_Y, POOL_COLS * CELL, POOL_ROWS * CELL)) {
            if (scrollY > 0) {
                this.page = Math.max(0, this.page - 1);
            } else if (scrollY < 0) {
                this.page = Math.min(10, this.page + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ------------------------------------------------------------- payloads

    private void combine() {
        if (this.select1 == null || this.select2 == null) {
            return;
        }
        if (this.combineCooldownMs > 0) {
            return;
        }
        this.combineCooldownMs = 200;
        playLocal(ModSounds.CAMERACLACK.value(), 0.4F, 1.0F);
        sendCombination(this.select1, this.select2);
    }

    /** Original: {@code PacketAspectCombinationToServer(aspect1, aspect2, bonus1, bonus2)}. */
    private void sendCombination(final Aspect aspect1, final Aspect aspect2) {
        // The tile-bonus arms (bonus1 / bonus2) belong to the research-table
        // tile, which this port has no equivalent of, so a combine always
        // claims from the aspect pool.
        PacketDistributor.sendToServer(new AspectCombinationToServerPayload(tablePos(),
                aspect1.getTag(), aspect2.getTag(), false, false));
    }

    /**
     * Original: {@code PacketAspectPlaceToServer(q, r, aspect)} — the erase
     * rides the same packet with no aspect. The packet carries the table's
     * position; this port has no table block, so it carries the player's own
     * position and {@code ServerNetworkHooks.placeAspect} routes by the open
     * menu (BD-20).
     */
    private void sendPlace(final Hex hex, final Aspect aspect) {
        PacketDistributor.sendToServer(new AspectPlaceToServerPayload(tablePos(),
                Optional.ofNullable(aspect).map(Aspect::getTag), hex.q(), hex.r()));
    }

    private net.minecraft.core.BlockPos tablePos() {
        return this.minecraft.player.blockPosition();
    }

    // --------------------------------------------------------------- helpers

    private void playLocal(final net.minecraft.sounds.SoundEvent event, final float volume, final float pitch) {
        this.minecraft.player.playSound(event, volume, pitch);
    }

    private boolean knowsResearch(final String key) {
        final PlayerKnowledgeData knowledge = this.menu.getKnowledge();
        return knowledge != null && ResearchManager.isResearchComplete(knowledge, key);
    }

    /**
     * The live note in the slot: the original re-reads {@code table.contents[1]}
     * every frame, so a slot change mid-session needs no re-open.
     */
    private ResearchNoteData noteFromSlot() {
        return ResearchNoteMenu.noteOf(this.menu.getSlot(ResearchNoteMenu.SLOT_NOTE).getItem());
    }

    /** Original: {@code getAspectsDiscovered} sorted by name — the full discovered list. */
    private List<Aspect> poolAspects() {
        final PlayerKnowledgeData knowledge = this.menu.getKnowledge();
        if (knowledge == null) {
            return List.of();
        }
        final List<Aspect> aspects = new ArrayList<>();
        for (final var entry : knowledge.aspectsDiscovered()) {
            final Aspect aspect = Aspect.getAspect(entry.key());
            if (aspect != null) {
                aspects.add(aspect);
            }
        }
        aspects.sort(Comparator.comparing(Aspect::getName));
        return aspects;
    }

    /** Original: the two recipe components of the note's entry (the preview arm). */
    private List<Aspect> noteComponents(final ResearchNoteData note) {
        final ResearchEntryData data = note == null ? null : ResearchManager.graph().entry(note.key());
        if (data == null || data.tags() == null) {
            return List.of();
        }
        final List<Aspect> components = new ArrayList<>();
        for (final ResearchEntryData.AspectTag tag : data.tags()) {
            final Aspect aspect = Aspect.getAspect(tag.aspect());
            if (aspect != null) {
                components.add(aspect);
            }
        }
        return components;
    }

    /**
     * The decorative connection sweep between written hexes, from the same
     * rule the server completes on ({@code NotePuzzle} connection walk). The
     * original's GUI reads the tile's connection map; the port walks the
     * written cells in grid order and links neighbours whose aspects connect.
     */
    private List<Hex> connectionSweep(final ResearchNoteData note) {
        if (note == null || (this.select1 == null && this.select2 == null)) {
            return List.of();
        }
        final List<Hex> sweep = new ArrayList<>();
        for (final Hex hex : note.hexes().values()) {
            final ResearchNoteData.HexEntry entry = note.entryAt(hex);
            if (entry == null || entry.empty()) {
                continue;
            }
            for (int direction = 0; direction < 6; direction++) {
                final Hex neighbour = hex.getNeighbour(direction);
                final ResearchNoteData.HexEntry other = note.entryAt(neighbour);
                if (other == null || other.empty()) {
                    continue;
                }
                if (NotePuzzle.componentsMatch(entry.aspectTag(), other.aspectTag())) {
                    if (sweep.isEmpty()) {
                        sweep.add(hex);
                    }
                    sweep.add(neighbour);
                }
            }
        }
        return sweep;
    }

    private static boolean overSelect1(final int mx, final int my) {
        return insideWindow(mx, my, SELECT_1_X, SELECT_Y, SELECT_WINDOW);
    }

    private static boolean overSelect2(final int mx, final int my) {
        return insideWindow(mx, my, SELECT_2_X, SELECT_Y, SELECT_WINDOW);
    }

    /**
     * Original: {@code UtilsFX.inside(mx, my, x, y, w, h)} — a plain rect test
     * with NO margin (the GUI calls it with pre-offset coordinates for the
     * pool cells, and with the 32x32 boxes for the select windows).
     */
    private static boolean inside(final int mx, final int my, final int x, final int y,
                                  final int width, final int height) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    /** The select-slot release window: the 16 px box widened by {@code margin}. */
    private static boolean insideWindow(final int mx, final int my, final int x, final int y, final int margin) {
        return inside(mx, my, x - margin, y - margin, CELL + margin * 2, CELL + margin * 2);
    }

    private static int windowFor(final int page) {
        return Math.min(Math.max(0, page) * 2, 20);
    }

    private static int pages(final int poolSize) {
        return Math.max(1, (poolSize + POOL_PAGE_SIZE - 1) / POOL_PAGE_SIZE);
    }

    private static int withAlpha(final int rgb, final int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private boolean hasResource(final ResourceLocation id) {
        if (this.spritePresent.contains(id)) {
            return true;
        }
        if (this.spriteMissing.contains(id)) {
            return false;
        }
        final boolean present = this.minecraft.getResourceManager() != null
                && this.minecraft.getResourceManager().getResource(id).isPresent();
        (present ? this.spritePresent : this.spriteMissing).add(id);
        return present;
    }
}
