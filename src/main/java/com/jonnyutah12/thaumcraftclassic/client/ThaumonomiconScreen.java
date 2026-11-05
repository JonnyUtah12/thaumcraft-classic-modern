package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.research.ResearchBrowserModel;
import com.jonnyutah12.thaumcraftclassic.research.ResearchCategoryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * The Thaumonomicon browser screen (BOOK-1 / t_1e6b18cc + BOOK-2 / t_10805b95;
 * content-mapping row {@code Thaumcraft:GUI_THAUMONOMICON} →
 * {@code thaumcraft:gui_thaumonomicon}, original {@code gui_id = 12}).
 *
 * <p><b>Containerless, on purpose.</b> The original is
 * {@code public class GuiResearchBrowser extends GuiScreen}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchBrowser.java:41}):
 * no {@code Container}, no slots, no {@code ContainerData}; 1.7.10 opens it as
 * a GuiScreen-only GUI through {@code player.openGui(Thaumcraft.instance, 12,
 * world, 0, 0, 0)} ({@code ItemThaumonomicon.java:79}). This class therefore
 * extends 1.21.1's {@link Screen} and NOT {@code AbstractContainerScreen}: the
 * port opens it with the S2C {@code thaumcraft:open_thaumonomicon} payload
 * (BD-22), not with {@code openMenu}, and registers no menu type. The reason
 * the menu route was rejected is stated in {@code OpenThaumonomiconPayload} and
 * pinned by {@code ThaumonomiconOpenPathContractTest}
 * ({@code screenExtendsScreenAndNotAContainerScreen}) and by the game probe
 * {@code theBookRegistersNoMenuTypeAndTheOpenPayloadInstead}.</p>
 *
 * <p><b>What BOOK-2 draws.</b> The category tab strip and the entry listing of
 * the selected category, both taken from {@link ResearchBrowserModel}, which
 * computes them out of {@code data/thaumcraft/research_graph/{categories,entries}.json}
 * (6 categories / 201 entries). Nothing in this class names a category key or
 * an entry key: the tabs are the graph's categories in registration order —
 * the original's tab loop over {@code ResearchCategories.researchCategories.keySet()}
 * ({@code :480-530}), 24x24 tabs at {@code paneLeft - 24},
 * {@code paneTop + i * 24} — and the rows are the selected category's entries
 * in the graph's order, the list {@code updateResearch()} builds at
 * {@code :101-103}. Clicking a tab switches the listing and clicking a row
 * opens that entry: the original's own mouse handling (tab hit test
 * {@code :745-765}, hit → {@code selectedCategory = obj; updateResearch();};
 * the node click that opens the entry's page at {@code :711} →
 * {@code GuiResearchRecipe}).</p>
 *
 * <p><b>Row list, not the node map.</b> The original pans a GL node grid
 * (24 px pitch, {@code :105-113}); the port lists the same rows in a column of
 * {@link ResearchBrowserModel#ROWS_PER_PAGE} per page with a page turn,
 * because 1.21.1 draws text through {@link GuiGraphics} and the port ships no
 * node art. The node icons, the hover popups ({@code GuiResearchPopup}, 134
 * lines), the recipe pages ({@code GuiResearchRecipe}, 1613 lines) and the
 * cheat sheet stay BD-13. A tab click plays no sound: the original plays
 * {@code thaumcraft:cameraclack} ({@code :766-776}) and the port registers
 * {@code cameraticks}, not that id — recorded in BD-13 with the rest of the
 * browser's FX.</p>
 *
 * <p><b>Labels are the graph's strings.</b> Row text, tab text and the entry
 * pane's body are the {@code nameEnUs} / {@code textEnUs} values of the JSON —
 * the original en_US strings, committed verbatim as quotation under D-016 — so
 * a row shows the entry's real name and the entry pane shows real text. The
 * book invents no copy: an entry the graph gives no name to is listed by its
 * key and shows an empty body. The original's {@code tc.research_name.*} /
 * {@code tc.research_text.*} lang keys are not in the port's lang file, so
 * per-locale translation of research names is follow-up work, stated in the
 * docs (BD-13) and not silently skipped.</p>
 *
 * <p><b>Close path.</b> The original overrides no key handler and no
 * {@code doesGuiPauseGame}, so vanilla governs it: ESC closes the screen and
 * the screen is a pause screen in singleplayer. This class keeps both vanilla
 * defaults — the same "no override" fidelity the contract test pins
 * structurally, because the game-test harness is a dedicated server and can
 * never fire a key event at a screen. {@link #mouseClicked} is the navigation
 * handler, the original's own {@code func_73864_a}.</p>
 *
 * <p><b>State across reopens.</b> The original remembers the category in the
 * static {@code selectedCategory} ({@code :63}) and recomputes its pan
 * position into the statics {@code lastX}/{@code lastY} in
 * {@code onGuiClosed} ({@code :116-121}); that is why reopening the book
 * lands where the player left it. {@link #lastCategory} + {@link #removed()}
 * are the same mechanism in 1.21.1, and an empty payload category resolves
 * through it.</p>
 */
@OnlyIn(Dist.CLIENT)
public class ThaumonomiconScreen extends Screen {

    /**
     * The screen's title key — the key the card names. A containerless screen
     * gets no title from the server (a menu's title rides the open-screen
     * packet), so the screen translates this key itself.
     */
    public static final String TITLE_KEY = "container.thaumcraft.thaumonomicon";

    /** The original's pane size ({@code GuiResearchBrowser.java:46-47}). */
    static final int PANE_WIDTH = 256;
    static final int PANE_HEIGHT = 230;

    /** The original's tab size ({@code :753-756}: 24x24 per category tab). */
    static final int TAB_SIZE = 24;
    /** The row pitch inside the pane (port-authored: 9 px font + 2 px leading). */
    static final int ROW_PITCH = 11;
    /** The y of the first listing row, inside the pane, under the header. */
    static final int ROWS_TOP = 40;

    /** The book pane's fill (the original's plate is an ARR texture, D-004). */
    static final int PANE_FILL = 0xFF1D1D2D;
    /** The listing plate, one step lighter than the pane. */
    static final int PLATE_FILL = 0xFF272738;
    /** The book's light-grey text. */
    static final int TEXT = 0xFFE0E0E0;
    /** The selected row / active tab band. */
    static final int HIGHLIGHT = 0xFF4C6B8A;
    /** The page footer and the "pick an entry" hint. */
    static final int DIM_TEXT = 0xFFA0A0B4;
    /** The flat plate behind the tab hover label. */
    static final int TOOLTIP_BG = 0xF0101020;

    /**
     * The original's static category memory ({@code GuiResearchBrowser.java:63}
     * {@code private static String selectedCategory}) — the mechanism that
     * makes a reopen land on the page the player was reading.
     */
    private static String lastCategory = null;

    /**
     * The listing this window draws: tabs, rows, page and selection, all
     * computed from the shipped research graph. The window keeps no list of its
     * own, which is what makes the listing the graph's and not the GUI's.
     */
    private final ResearchBrowserModel browser;

    public ThaumonomiconScreen(final String category) {
        super(Component.translatable(TITLE_KEY));
        this.browser = new ResearchBrowserModel(ResearchManager.graph());
        this.browser.selectCategory(resolveCategory(category));
    }

    /** The graph-derived listing this window draws (tabs, rows, page, selection). */
    public ResearchBrowserModel browser() {
        return this.browser;
    }

    /**
     * The client open: the S2C payload handler calls this on the client game
     * thread (the {@code enqueueWork} hop in {@code ModNetwork}), which is the
     * 1.21.1 equivalent of the client half of FML's GuiOpen packet. The class
     * is {@code @OnlyIn(Dist.CLIENT)}, so a dedicated server never resolves
     * this method — the payload is registered on both dists but only ever
     * received by a client.
     */
    public static void openFromServer(final String category) {
        Minecraft.getInstance().setScreen(new ThaumonomiconScreen(category));
    }

    /**
     * Empty payload category → the last category the player read, else the
     * first category of the graph: the original's
     * {@code if (selectedCategory == null) selectedCategory = categories.iterator().next()}
     * ({@code GuiResearchBrowser.java:96-99}).
     */
    static String resolveCategory(final String category) {
        if (category != null && !category.isEmpty()
                && ResearchManager.graph().category(category) != null) {
            return category;
        }
        if (lastCategory != null && ResearchManager.graph().category(lastCategory) != null) {
            return lastCategory;
        }
        for (final ResearchCategoryData candidate : ResearchManager.graph().categories()) {
            return candidate.key();
        }
        return "";
    }

    /** The category page this instance shows (also the state saved on close). */
    public String selectedCategory() {
        return this.browser.selectedCategory();
    }

    /** The test seam for the reopen memory (the original's static field). */
    public static String lastCategory() {
        return lastCategory;
    }

    /** The test seam: clear the memory (the state of a fresh client session). */
    public static void forgetCategory() {
        lastCategory = null;
    }

    /** The pane's left edge in the current window. */
    int paneLeft() {
        return (this.width - PANE_WIDTH) / 2;
    }

    /** The pane's top edge in the current window. */
    int paneTop() {
        return (this.height - PANE_HEIGHT) / 2;
    }

    @Override
    public void removed() {
        // The original's onGuiClosed writes its view state back into the
        // statics (:116-121). Same mechanism, one field in this slice.
        lastCategory = this.browser.selectedCategory();
        super.removed();
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        // The original's func_73864_a: a node click opens the entry (:711) and
        // a tab click switches the category (:745-765). The same three hit
        // tests on the port's geometry; super keeps vanilla's behaviour for
        // everything outside the book (and the vanilla ESC path is untouched).
        final int left = paneLeft();
        final int top = paneTop();

        // Tabs: 24x24 squares down the pane's left edge (:753-756, :752-761).
        final List<ResearchBrowserModel.Tab> tabs = this.browser.tabs();
        for (int i = 0; i < tabs.size(); i++) {
            final int tabTop = top + i * TAB_SIZE;
            if (mouseX >= left - TAB_SIZE - 1 && mouseX < left - 1
                    && mouseY >= tabTop && mouseY < tabTop + TAB_SIZE) {
                this.browser.selectCategory(tabs.get(i).key());
                return true;
            }
        }

        // Rows of the page on screen, top to bottom, from the model's window.
        final List<ResearchBrowserModel.Row> rows =
                this.browser.page(this.browser.selectedCategory(), this.browser.page());
        for (int i = 0; i < rows.size(); i++) {
            final int rowTop = top + ROWS_TOP + i * ROW_PITCH;
            if (mouseX >= left + 8 && mouseX < left + PANE_WIDTH - 8
                    && mouseY >= rowTop - 1 && mouseY < rowTop + ROW_PITCH - 1) {
                this.browser.selectEntry(rows.get(i).key());
                return true;
            }
        }

        // The footer band: left third turns back, right third turns forward.
        final int footerTop = footerTop();
        if (mouseY >= footerTop && mouseY < footerTop + ROW_PITCH + 2) {
            if (mouseX < left + PANE_WIDTH / 3) {
                return this.browser.prevPage() || super.mouseClicked(mouseX, mouseY, button);
            }
            if (mouseX >= left + 2 * PANE_WIDTH / 3) {
                return this.browser.nextPage() || super.mouseClicked(mouseX, mouseY, button);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The y of the page footer band (under the last possible row). */
    private int footerTop() {
        return paneTop() + ROWS_TOP + ResearchBrowserModel.ROWS_PER_PAGE * ROW_PITCH + 2;
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY,
                       final float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        final int left = paneLeft();
        final int top = paneTop();

        // The pane: the original's 256x230 plate (:46-47).
        guiGraphics.fill(left, top, left + PANE_WIDTH, top + PANE_HEIGHT, PANE_FILL);
        guiGraphics.drawCenteredString(this.font, this.title, left + PANE_WIDTH / 2, top + 8, TEXT);

        renderTabs(guiGraphics, left, top, mouseX, mouseY);

        final String category = this.browser.selectedCategory();
        guiGraphics.drawCenteredString(this.font, Component.literal(categoryLabel(category)),
                left + PANE_WIDTH / 2, top + 22, TEXT);

        // The listing: the entries of this category, ROWS_PER_PAGE to a page.
        final int page = this.browser.page();
        final List<ResearchBrowserModel.Row> rows = this.browser.page(category, page);
        final ResearchBrowserModel.Row selected = this.browser.selectedRow();
        guiGraphics.fill(left + 8, top + ROWS_TOP - 2, left + PANE_WIDTH - 8,
                top + ROWS_TOP + ResearchBrowserModel.ROWS_PER_PAGE * ROW_PITCH - 1, PLATE_FILL);
        for (int i = 0; i < rows.size(); i++) {
            final ResearchBrowserModel.Row row = rows.get(i);
            final int rowTop = top + ROWS_TOP + i * ROW_PITCH;
            final boolean isSelected = selected != null && selected.key().equals(row.key());
            if (isSelected) {
                guiGraphics.fill(left + 9, rowTop - 1, left + PANE_WIDTH - 9, rowTop + ROW_PITCH - 1, HIGHLIGHT);
            }
            final int number = page * ResearchBrowserModel.ROWS_PER_PAGE + i + 1;
            guiGraphics.drawString(this.font, Component.literal(number + ". " + trim(row.label(), 40)),
                    left + 12, rowTop, isSelected ? 0xFFFFFFFF : TEXT);
        }

        renderPageFooter(guiGraphics, left, top, category, page);
        renderEntry(guiGraphics, left, top, selected);
    }

    /** The category strip: one 24x24 tab per category of the graph (:480-530). */
    private void renderTabs(final GuiGraphics guiGraphics, final int left, final int top,
                            final int mouseX, final int mouseY) {
        final List<ResearchBrowserModel.Tab> tabs = this.browser.tabs();
        String hover = null;
        for (int i = 0; i < tabs.size(); i++) {
            final ResearchBrowserModel.Tab tab = tabs.get(i);
            final int tabTop = top + i * TAB_SIZE;
            final boolean active = tab.key().equals(this.browser.selectedCategory());
            final boolean over = mouseX >= left - TAB_SIZE - 1 && mouseX < left - 1
                    && mouseY >= tabTop && mouseY < tabTop + TAB_SIZE;
            guiGraphics.fill(left - TAB_SIZE - 1, tabTop, left - 1, tabTop + TAB_SIZE,
                    active ? HIGHLIGHT : (over ? 0xFF3A3A52 : PLATE_FILL));
            guiGraphics.drawCenteredString(this.font,
                    Component.literal(tab.label().substring(0, 1).toUpperCase()),
                    left - TAB_SIZE / 2 - 1, tabTop + 8, TEXT);
            if (over) {
                hover = tab.label() + "  (" + tab.entryCount() + ")";
            }
        }
        if (hover != null) {
            // The port has no icon art for a tab, so the name follows the
            // cursor on hover (the original's tab carries the category's icon
            // texture, :513-517). Drawn as plain text: the port's screen has no
            // tooltip helper on this platform version.
            guiGraphics.fill(mouseX + 4, mouseY - 3, mouseX + 8 + hover.length() * 6, mouseY + 9,
                    TOOLTIP_BG);
            guiGraphics.drawString(this.font, Component.literal(hover), mouseX + 7, mouseY, TEXT);
        }
    }

    /** The page band: turn back / page number / turn forward. */
    private void renderPageFooter(final GuiGraphics guiGraphics, final int left, final int top,
                                  final String category, final int page) {
        final int footerTop = top + ROWS_TOP + ResearchBrowserModel.ROWS_PER_PAGE * ROW_PITCH + 2;
        final int pages = this.browser.pageCount(category);
        guiGraphics.drawString(this.font, Component.literal(page > 0 ? "< prev" : ""),
                left + 12, footerTop, DIM_TEXT);
        guiGraphics.drawCenteredString(this.font, Component.literal("Page " + (page + 1) + " / " + pages),
                left + PANE_WIDTH / 2, footerTop, DIM_TEXT);
        guiGraphics.drawString(this.font, Component.literal(page + 1 < pages ? "next >" : ""),
                left + PANE_WIDTH - 46, footerTop, DIM_TEXT);
    }

    /** The selected entry: its own name, and the graph's own text for it. */
    private void renderEntry(final GuiGraphics guiGraphics, final int left, final int top,
                             final ResearchBrowserModel.Row selected) {
        final int entryTop = footerTop() + ROW_PITCH + 4;
        if (selected == null) {
            guiGraphics.drawCenteredString(this.font, Component.literal("Pick an entry"),
                    left + PANE_WIDTH / 2, entryTop, DIM_TEXT);
            return;
        }
        guiGraphics.drawString(this.font, Component.literal(trim(selected.label(), 46)),
                left + 12, entryTop, 0xFFFFFFFF);
        final String text = this.browser.selectedText();
        if (text.isEmpty()) {
            return;
        }
        final int textTop = entryTop + ROW_PITCH;
        final int linesAvailable = Math.max(1, (top + PANE_HEIGHT - 3 - textTop) / 9);
        final List<net.minecraft.util.FormattedCharSequence> lines =
                this.font.split(Component.literal(text.replace("<BR>", "  ")), PANE_WIDTH - 24);
        for (int i = 0; i < Math.min(lines.size(), linesAvailable); i++) {
            guiGraphics.drawString(this.font, lines.get(i), left + 12, textTop + i * 9, TEXT);
        }
    }

    private String categoryLabel(final String categoryKey) {
        for (final ResearchBrowserModel.Tab tab : this.browser.tabs()) {
            if (tab.key().equals(categoryKey)) {
                return tab.label();
            }
        }
        return categoryKey;
    }

    private static String trim(final String label, final int max) {
        return label.length() <= max ? label : label.substring(0, max - 1) + "...";
    }
}
