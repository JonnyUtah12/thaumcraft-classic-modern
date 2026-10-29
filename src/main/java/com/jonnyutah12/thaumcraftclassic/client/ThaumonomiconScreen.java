package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.research.book.BookEdge;
import com.jonnyutah12.thaumcraftclassic.research.book.BookFrame;
import com.jonnyutah12.thaumcraftclassic.research.book.BookNode;
import com.jonnyutah12.thaumcraftclassic.research.book.BookPageView;
import com.jonnyutah12.thaumcraftclassic.research.book.BookTab;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * The Thaumonomicon book screen (TC-M3-03-s) — the port's counterpart of the
 * original {@code thaumcraft.client.gui.GuiResearchBrowser}
 * (content-mapping.csv {@code thaumcraft:gui_thaumonomicon}, gui_id 12).
 *
 * <p>The screen owns no book rules. It renders exactly one
 * {@link BookFrame} — the category tabs, the placed graph nodes and drawn
 * prerequisite lines of the current category, and the open entry's page — and
 * forwards every input to the state machine in {@link ThaumonomiconBook}. That
 * split is what makes the headless game test meaningful: the probe drives the
 * same state machine through {@code research.book.ThaumonomiconHost} and
 * asserts on the frame this class would draw.</p>
 *
 * <p><b>Drawing is vanilla-only (docs/BEHAVIORAL_DEVIATIONS.md BD-17).</b> The
 * original drew {@code gui_researchback.png} as the page background and the six
 * {@code textures/misc/r_*.png} category icons on the tabs. Those files are All
 * Rights Reserved and are never committed (D-004), so a code-only jar has no
 * usable texture for either; the panel is therefore a flat vanilla-toned fill
 * with a frame, the tabs are labelled rectangles, and the node frames are
 * written with {@code GuiGraphics.fill}/{@code renderOutline} rather than
 * blitted from an atlas. Nothing here uses legacy GL: 1.21.1 draws through
 * {@link GuiGraphics}.</p>
 *
 * <p><b>Labels.</b> Tab and entry labels are the original lang keys
 * ({@code tc.research_category.*} / {@code tc.research_name.*}) resolved through
 * the game language system, exactly as {@code ResearchCategoryData} /
 * {@code ResearchEntryData} require at runtime; the verbatim {@code en_US}
 * strings those records carry are the ARR quotation kept for the parity tests
 * and are deliberately not rendered. Until the research lang keys ship (the
 * localization card scoped them to TC-M3-03/04) a missing key renders as the key
 * itself, which is the platform's normal missing-translation display and the
 * same behaviour every unlocalized vanilla key has.</p>
 *
 * <p>Out of scope for this card and not drawn here: the research minigames
 * (TC-M3-04-s), the crafting/aspect page bodies (their owning cards render
 * those), the original's zoom/scroll of the graph, and the note-completion
 * interaction.</p>
 */
@OnlyIn(Dist.CLIENT)
public class ThaumonomiconScreen extends Screen {

    /** The book's title key — the registered Thaumonomicon item's own name key. */
    static final String TITLE_KEY = "item.thaumcraft.itemthaumonomicon";

    /** Vanilla GUI panel base colour (the flat fill of a container panel). */
    static final int PANEL_FILL = 0xFFC6C6C6;
    /** Vanilla panel frame colour, drawn as a 1 px outline. */
    static final int PANEL_FRAME = 0xFF555555;
    /** Text colour on the light panel. */
    static final int TEXT_COLOR = 0xFF202020;
    /** World dim behind the book. */
    static final int DIM_COLOR = 0xC0101010;
    /** Muted colour for the graph area's own backing and empty-state line. */
    static final int GRAPH_FILL = 0xFFADADAD;

    /** Tab fill, selected and not. */
    static final int TAB_FILL = 0xFF8B8B8B;
    static final int TAB_SELECTED_FILL = 0xFFE2CF9A;
    /** Node fill by {@link BookNode.Kind}, its frame, and the open-entry highlight. */
    static final int NODE_FILL = 0xFF7D8B4F;
    static final int NODE_ROOT_FILL = 0xFFD8C070;
    static final int NODE_HIDDEN_FILL = 0xFF6E6E6E;
    static final int NODE_FRAME = 0xFF303030;
    static final int NODE_OPEN_FRAME = 0xFF101010;
    /** Prerequisite line colour. */
    static final int EDGE_COLOR = 0xFF404040;

    /** The vanilla 18x18 dark slot square, used for the page framing. */
    static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.withDefaultNamespace("container/slot");

    static final int TAB_HEIGHT = 16;
    /** Graph pane's preferred width; it shrinks on a narrow window. */
    static final int GRAPH_PREFERRED_WIDTH = 190;
    /** Node box size at the widest supported cell pitch. */
    static final int NODE_SIZE = 14;
    /** Cell pitch clamp: below this the graph is drawn compressed rather than unreadable. */
    static final int MIN_CELL = 7;
    static final int MAX_CELL = 24;
    /** Below this pitch a node label is omitted (it would overlap its neighbours). */
    static final int LABEL_MIN_CELL = 17;

    private final ThaumonomiconBook book;

    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private int graphLeft;
    private int graphTop;
    private int graphWidth;
    private int graphHeight;
    private int pageLeft;
    private int pageTop;
    private int pageWidth;
    private int pageHeight;

    public ThaumonomiconScreen(final ThaumonomiconBook book) {
        super(Component.translatable(TITLE_KEY));
        this.book = book;
    }

    /**
     * The {@link com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconHost}
     * implementation: put the book on screen. The client installs this once
     * ({@link ThaumonomiconClientHooks}); the game test installs its own
     * recording host instead, so no client class is ever reached on a server.
     */
    public static void open(final ThaumonomiconBook book) {
        Minecraft.getInstance().setScreen(new ThaumonomiconScreen(book));
    }

    /** A book does not pause a single-player world (the original did not either). */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(320, Math.max(200, this.width - 20));
        this.panelHeight = Math.min(210, Math.max(140, this.height - 20));
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;

        this.graphLeft = this.leftPos + 6;
        this.graphTop = this.topPos + TAB_HEIGHT + 6;
        this.graphWidth = Math.min(GRAPH_PREFERRED_WIDTH, this.panelWidth - 70);
        this.graphHeight = this.topPos + this.panelHeight - 24 - this.graphTop;

        this.pageLeft = this.graphLeft + this.graphWidth + 8;
        this.pageTop = this.graphTop;
        this.pageWidth = this.leftPos + this.panelWidth - 6 - this.pageLeft;
        this.pageHeight = this.graphHeight;

        final int buttonY = this.topPos + this.panelHeight - 20;
        final int buttonW = Math.max(24, (this.pageWidth - 8) / 3);
        addRenderableWidget(Button.builder(Component.literal("<"),
                        button -> this.book.previousPage())
                .bounds(this.pageLeft, buttonY, buttonW, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"),
                        button -> this.book.nextPage())
                .bounds(this.pageLeft + buttonW + 4, buttonY, buttonW, 16).build());
        addRenderableWidget(Button.builder(Component.literal("^"),
                        button -> this.book.back())
                .bounds(this.pageLeft + 2 * (buttonW + 4), buttonY, buttonW, 16).build());
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY,
                       final float partialTick) {
        // Dim the world behind the book without depending on the platform's
        // blur/panorama path, which a book does not want anyway.
        guiGraphics.fill(0, 0, this.width, this.height, DIM_COLOR);

        final BookFrame frame = this.book.frame();

        guiGraphics.fill(this.leftPos, this.topPos,
                this.leftPos + this.panelWidth, this.topPos + this.panelHeight, PANEL_FILL);
        guiGraphics.renderOutline(this.leftPos, this.topPos, this.panelWidth, this.panelHeight, PANEL_FRAME);
        guiGraphics.drawString(this.font, this.getTitle(), this.leftPos + 6, this.topPos + 4, TEXT_COLOR, false);

        renderTabs(guiGraphics, frame);
        renderGraph(guiGraphics, frame);
        renderPage(guiGraphics, frame);

        // Widgets (the navigation buttons) draw last, on top of the panel.
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderTabs(final GuiGraphics guiGraphics, final BookFrame frame) {
        final List<BookTab> tabs = frame.tabs();
        if (tabs.isEmpty()) {
            return;
        }
        final int y = this.topPos + TAB_HEIGHT - 2;
        for (int i = 0; i < tabs.size(); i++) {
            final BookTab tab = tabs.get(i);
            final int left = this.tabLeft(i, tabs.size());
            final int right = this.tabLeft(i + 1, tabs.size()) - 1;
            guiGraphics.fill(left, this.topPos + 1, right, y,
                    tab.selected() ? TAB_SELECTED_FILL : TAB_FILL);
            guiGraphics.renderOutline(left, this.topPos + 1, right - left, y - this.topPos - 1, NODE_FRAME);
            final String label = fit(Component.translatable(tab.nameKey()).getString(), right - left - 4);
            guiGraphics.drawString(this.font, label, left + 2, this.topPos + 4, TEXT_COLOR, false);
        }
    }

    private void renderGraph(final GuiGraphics guiGraphics, final BookFrame frame) {
        guiGraphics.fill(this.graphLeft, this.graphTop,
                this.graphLeft + this.graphWidth, this.graphTop + this.graphHeight, GRAPH_FILL);
        guiGraphics.renderOutline(this.graphLeft, this.graphTop, this.graphWidth, this.graphHeight, PANEL_FRAME);

        final List<BookNode> nodes = frame.visibleNodes();
        if (nodes.isEmpty()) {
            // Defensive: every shipped category has placed entries (the graph
            // build fails otherwise), so this pane is normally never empty.
            return;
        }
        final Placement placement = placement(nodes);

        for (final BookEdge edge : frame.edges()) {
            final BookNode parent = node(frame.visibleNodes(), edge.parentKey());
            final BookNode child = node(frame.visibleNodes(), edge.childKey());
            if (parent == null || child == null) {
                continue;
            }
            drawEdge(guiGraphics, placement, parent, child);
        }
        for (final BookNode node : nodes) {
            final int size = placement.size();
            final int x = placement.x(node);
            final int y = placement.y(node);
            final boolean open = node.key().equals(this.book.openEntryKey());
            guiGraphics.fill(x, y, x + size, y + size,
                    node.kind() == BookNode.Kind.ROOT ? NODE_ROOT_FILL : NODE_FILL);
            guiGraphics.renderOutline(x - 1, y - 1, size + 2, size + 2, open ? NODE_OPEN_FRAME : NODE_FRAME);
            if (placement.cell() >= LABEL_MIN_CELL) {
                final String label = fit(Component.translatable(node.nameKey()).getString(), placement.cell() - 2);
                guiGraphics.drawString(this.font, label, x, y + size + 1, TEXT_COLOR, false);
            }
        }
    }

    private void renderPage(final GuiGraphics guiGraphics, final BookFrame frame) {
        final BookPageView page = frame.page();
        if (page == null) {
            guiGraphics.drawString(this.font, Component.translatable(frame.categoryNameKey()).getString(),
                    this.pageLeft, this.pageTop, TEXT_COLOR, false);
            return;
        }
        guiGraphics.blitSprite(SLOT_SPRITE, this.pageLeft - 3, this.pageTop - 3,
                this.pageWidth + 6, this.pageHeight + 6);
        final String heading = fit(Component.translatable(page.titleKey()).getString(), this.pageWidth - 4);
        guiGraphics.drawString(this.font, heading, this.pageLeft, this.pageTop, TEXT_COLOR, false);
        if (page.count() > 0) {
            guiGraphics.drawString(this.font, (page.index() + 1) + "/" + page.count(),
                    this.pageLeft, this.pageTop + 10, TEXT_COLOR, false);
        }
        if (!page.revealed()) {
            // The original's text_concealed page: nothing to read yet.
            return;
        }
        final Component body = page.textKey() != null
                ? Component.translatable(page.textKey())
                : Component.literal(String.valueOf(page.kind()));
        final List<FormattedCharSequence> lines =
                this.font.split(body, Math.max(20, this.pageWidth - 4));
        int y = this.pageTop + 22;
        for (final FormattedCharSequence line : lines) {
            if (y + this.font.lineHeight > this.topPos + this.panelHeight - 24) {
                break;
            }
            guiGraphics.drawString(this.font, line, this.pageLeft, y, TEXT_COLOR, false);
            y += this.font.lineHeight;
        }
    }

    private void drawEdge(final GuiGraphics guiGraphics, final Placement placement,
                          final BookNode parent, final BookNode child) {
        final int size = placement.size();
        final int half = size / 2;
        final int px = placement.x(parent) + half;
        final int py = placement.y(parent) + half;
        final int cx = placement.x(child) + half;
        final int cy = placement.y(child) + half;
        // The original drew straight prerequisite lines; an L-route keeps the
        // segment axis-aligned without pulling in a line rasterizer.
        fillHorizontal(guiGraphics, px, cx, py);
        fillVertical(guiGraphics, cx, py, cy);
    }

    private static void fillHorizontal(final GuiGraphics guiGraphics, final int x1, final int x2, final int y) {
        guiGraphics.fill(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 1, EDGE_COLOR);
    }

    private static void fillVertical(final GuiGraphics guiGraphics, final int x, final int y1, final int y2) {
        guiGraphics.fill(x, Math.min(y1, y2), x + 1, Math.max(y1, y2) + 1, EDGE_COLOR);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        final BookFrame frame = this.book.frame();
        final List<BookTab> tabs = frame.tabs();
        for (int i = 0; i < tabs.size(); i++) {
            if (within(mouseX, mouseY, this.tabLeft(i, tabs.size()), this.topPos + 1,
                    this.tabLeft(i + 1, tabs.size()) - 1, this.topPos + TAB_HEIGHT - 2)) {
                return this.book.selectCategoryIndex(i);
            }
        }
        final List<BookNode> nodes = frame.visibleNodes();
        if (!nodes.isEmpty()) {
            final Placement placement = placement(nodes);
            for (final BookNode node : nodes) {
                final int size = placement.size();
                if (within(mouseX, mouseY, placement.x(node), placement.y(node),
                        placement.x(node) + size, placement.y(node) + size)) {
                    return this.book.selectEntry(node.key());
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        switch (keyCode) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT:
                return this.book.openEntryKey() != null ? this.book.previousPage() : this.book.previousCategory();
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT:
                return this.book.openEntryKey() != null ? this.book.nextPage() : this.book.nextCategory();
            case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE:
                return this.book.back();
            default:
                return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    /** Where tab {@code index} starts, for the tab bar laid over the panel. */
    private int tabLeft(final int index, final int tabCount) {
        final int usable = this.panelWidth - 12;
        return this.leftPos + 6 + (usable * index) / tabCount;
    }

    /**
     * The node layout for the current category: the original browser
     * coordinates scaled to fit the graph pane. The original zoomed and scrolled
     * instead (BD-17); this slice fits the whole category in one view.
     */
    private Placement placement(final List<BookNode> nodes) {
        int minColumn = Integer.MAX_VALUE;
        int maxColumn = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        for (final BookNode node : nodes) {
            minColumn = Math.min(minColumn, node.column());
            maxColumn = Math.max(maxColumn, node.column());
            minRow = Math.min(minRow, node.row());
            maxRow = Math.max(maxRow, node.row());
        }
        final int columns = Math.max(1, maxColumn - minColumn + 1);
        final int rows = Math.max(1, maxRow - minRow + 1);
        int cell = Math.min(MAX_CELL, Math.min((this.graphWidth - 10) / columns, (this.graphHeight - 10) / rows));
        cell = Math.max(MIN_CELL, cell);
        final int size = Math.min(NODE_SIZE, cell);
        final int originX = this.graphLeft + 5
                + Math.max(0, (this.graphWidth - 10 - (columns - 1) * cell - size) / 2);
        final int originY = this.graphTop + 5
                + Math.max(0, (this.graphHeight - 10 - (rows - 1) * cell - size) / 2);
        return new Placement(originX, originY, cell, size, minColumn, minRow);
    }

    private static BookNode node(final List<BookNode> nodes, final String key) {
        for (final BookNode node : nodes) {
            if (node.key().equals(key)) {
                return node;
            }
        }
        return null;
    }

    private static boolean within(final double mouseX, final double mouseY,
                                  final int x1, final int y1, final int x2, final int y2) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    /** Trim a label to the pixel width available (a missing key renders as itself). */
    private String fit(final String text, final int maxWidth) {
        if (maxWidth <= 0 || this.font.width(text) <= maxWidth) {
            return text;
        }
        final String ellipsis = "..";
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    /** Node layout of one frame, in screen coordinates. */
    private record Placement(int originX, int originY, int cell, int size, int minColumn, int minRow) {

        int x(final BookNode node) {
            return originX + (node.column() - minColumn) * cell;
        }

        int y(final BookNode node) {
            return originY + (node.row() - minRow) * cell;
        }
    }
}
