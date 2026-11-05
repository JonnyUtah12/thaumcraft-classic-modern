package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.network.ClientNetworkData;
import com.jonnyutah12.thaumcraftclassic.network.payload.PlayerCompleteToServerPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchBrowserModel;
import com.jonnyutah12.thaumcraftclassic.research.ResearchCategoryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchDetailModel;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.book.BookNoteRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
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
 * <p><b>What BOOK-4 adds.</b> The per-entry detail panel: the selected entry's
 * essentia requirements, its unlock state, and its related-research links
 * ({@code parents}, {@code parentsHidden}, {@code siblings}), resolved by
 * {@link ResearchDetailModel} out of the shipped graph and out of the player's
 * own {@link PlayerKnowledgeData} attachment — the same synced store the
 * server gates completions on, so the panel cannot show a state the server
 * disagrees with (TC-M3-01/02; no new store). The original composes the same
 * information in its hover card ({@code GuiResearchBrowser.java:540-660}: the
 * {@code tc.researchmissing} card when {@code canUnlockResearch} is false, the
 * warp line, the tag list with the pool check) and reads the same three edge
 * arrays for the node's lines ({@code :288-333}); the port has no GL canvas
 * and no hover popup, so the panel is a text column and a link is a row that
 * opens that entry, which is what the original's node click does ({@code :711}).
 * Every line the panel draws is a line {@link ResearchDetailModel.Detail#lines()}
 * computed — the window adds no copy of its own, and the entry the graph names
 * nothing is labelled by its key (the BOOK-3 posture: render the key, do not
 * fail). The recipe pages ({@code GuiResearchRecipe}, 1613 lines: the page
 * renderer, its aspect art and its minigames) and the node art stay BD-13.</p>
 *
 * <p><b>What the merge adds (INTEGRATE-BOOK-LANES / t_ee886c7e).</b> The
 * research lane's note/purchase affordance, moved off its node-browser screen
 * and onto this screen's entry rows: the right button sends
 * {@link BookNoteRequest}'s arm for the row under the cursor (the original's
 * whole node click), the left button opens the row and then runs the same arm
 * (a no-op for an entry already known, so BOOK-2/BOOK-4 rows behave as before),
 * the five-state hint line is the original's own hover line drawn beside the
 * cursor and repeated in the detail panel, and the 3000 ms
 * {@code tc.research.popup} is the original's popup centred on this 256x230
 * pane. The decision itself is not here: it is {@code BookNoteRequest}, and the
 * only thing that completes research stays
 * {@code ResearchManager.completeResearch} behind
 * {@code ResearchManager.handlePlayerCompleteRequest} (BD-13). This is the
 * merged screen: the port has ONE Thaumonomicon, opened by the containerless
 * payload of BOOK-1 and no other path.</p>
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

    /**
     * The listing column's width. BOOK-4 hands the rest of the pane's width to
     * the detail panel: the original has no such column (its detail is a hover
     * card at the cursor, {@code GuiResearchBrowser.java:540-660}, and the
     * entry's own page opens in {@code GuiResearchRecipe}), so the split is
     * port-authored and the row labels are trimmed to it.
     */
    static final int LIST_WIDTH = 140;
    /** The detail panel's left edge, inside the pane (right of the listing). */
    static final int DETAIL_LEFT = 148;
    /** The detail panel's text width. */
    static final int DETAIL_WIDTH = PANE_WIDTH - DETAIL_LEFT - 8;
    /** The detail panel's row pitch (9 px font + leading). */
    static final int DETAIL_PITCH = 9;

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
    /** The locked state: the original draws a locked node at 0.3 brightness and names it {@code tc.researchmissing}. */
    static final int STATE_LOCKED = 0xFFC06060;
    /** The available state: the original pulses an unlockable node ({@code :370-373}). */
    static final int STATE_AVAILABLE = 0xFF7EC87E;
    /** A related-research link row (the original's node, clickable at {@code :711}). */
    static final int LINK_TEXT = 0xFF9AB4D8;
    /** The band behind a link row under the cursor. */
    static final int LINK_HOVER = 0xFF33405C;

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

    /**
     * The detail resolver this window draws: aspect requirements, unlock state
     * and related-research links for the selected entry, computed out of the
     * graph and the player's knowledge. The window keeps no list of its own
     * here either (D-019 rule 2).
     */
    private final ResearchDetailModel details;

    /**
     * The affordance of the row under the cursor (TC-M3-04-s-v, merged onto
     * this screen): the arm a click there sends, plus the original's five-state
     * hint line for it. Recomputed every frame, because the original recomputed
     * it in its tooltip pass ({@code GuiResearchBrowser.java:554-660}) and the
     * inputs are live inventory, pool and config values.
     */
    private BookNoteRequest.Request hint =
            new BookNoteRequest.Request(BookNoteRequest.Arm.NONE, null, null);

    /** Wall-clock ms until which the "got a research note!" popup draws (:722). */
    private long popupUntil;

    /** The popup line — the original's {@code tc.research.popup} text (:723-726). */
    private Component popupText;

    /** Where the cursor was when {@link #hint} was computed. */
    private int hoverX;
    private int hoverY;

    /**
     * The link rows drawn by the last frame, in pane coordinates: the click
     * targets of the detail panel. Rebuilt every render, because the panel's
     * content and the window's size are what decide them.
     */
    private final List<DetailHit> detailHits = new ArrayList<>();

    /** One clickable link row of the detail panel. */
    private record DetailHit(String key, int left, int top, int right, int bottom) {
    }

    public ThaumonomiconScreen(final String category) {
        super(Component.translatable(TITLE_KEY));
        this.browser = new ResearchBrowserModel(ResearchManager.graph());
        this.details = new ResearchDetailModel(this.browser.graph());
        this.browser.selectCategory(resolveCategory(category));
    }

    /** The graph-derived listing this window draws (tabs, rows, page, selection). */
    public ResearchBrowserModel browser() {
        return this.browser;
    }

    /** The detail resolver this window draws (aspects, unlock state, links). */
    public ResearchDetailModel details() {
        return this.details;
    }

    /**
     * The player's knowledge, read from the synced attachment — the server's
     * research store, not a client copy: {@code ModAttachments.PLAYER_KNOWLEDGE}
     * is {@code .sync(...)()} so the client holds the server's value, and the
     * server gates every completion against that same record
     * ({@code ResearchManager}, TC-M3-01/02). No player (a dedicated server,
     * which is where no screen exists anyway) reads as the empty store.
     */
    public static PlayerKnowledgeData playerKnowledge() {
        final Player player = Minecraft.getInstance().player;
        if (player == null) {
            return PlayerKnowledgeData.empty();
        }
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        return knowledge == null ? PlayerKnowledgeData.empty() : knowledge;
    }

    /**
     * The detail this window shows: the selected entry resolved against the
     * player's knowledge, or null with nothing selected.
     */
    public ResearchDetailModel.Detail selectedDetail() {
        final ResearchBrowserModel.Row selected = this.browser.selectedRow();
        return selected == null ? null : this.details.detail(selected.key(), playerKnowledge());
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

        // Detail-panel link rows: the port's stand-in for the original's node
        // click, which opens that entry (:711). Checked first because the panel
        // owns the pane's right column.
        for (final DetailHit hit : this.detailHits) {
            if (mouseX >= hit.left() && mouseX < hit.right()
                    && mouseY >= hit.top() && mouseY < hit.bottom()) {
                return this.browser.selectEntry(hit.key()) || super.mouseClicked(mouseX, mouseY, button);
            }
        }

        // Rows of the page on screen, top to bottom, from the model's window.
        final List<ResearchBrowserModel.Row> rows =
                this.browser.page(this.browser.selectedCategory(), this.browser.page());
        for (int i = 0; i < rows.size(); i++) {
            final int rowTop = top + ROWS_TOP + i * ROW_PITCH;
            if (mouseX >= left + 8 && mouseX < left + LIST_WIDTH
                    && mouseY >= rowTop - 1 && mouseY < rowTop + ROW_PITCH - 1) {
                final String key = rows.get(i).key();
                if (button == 1) {
                    // Right button: the note/purchase arm on its own — the
                    // original's whole node-click behaviour (it never opened a
                    // page for an entry the player did not already know).
                    requestEntry(key);
                    return true;
                }
                final boolean opened = this.browser.selectEntry(key);
                if (opened) {
                    // The original cleared the popup when it opened an entry
                    // view (popuptime = now - 1, :683).
                    this.popupUntil = System.currentTimeMillis() - 1L;
                }
                // The arm is a no-op for a known entry, so a known row behaves
                // exactly as BOOK-2/BOOK-4 left it.
                requestEntry(key);
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
        updateHover(mouseX, mouseY);

        // The pane: the original's 256x230 plate (:46-47).
        guiGraphics.fill(left, top, left + PANE_WIDTH, top + PANE_HEIGHT, PANE_FILL);
        guiGraphics.drawCenteredString(this.font, this.title, left + PANE_WIDTH / 2, top + 8, TEXT);

        renderTabs(guiGraphics, left, top, mouseX, mouseY);

        final String category = this.browser.selectedCategory();
        guiGraphics.drawCenteredString(this.font, Component.literal(categoryLabel(category)),
                left + PANE_WIDTH / 2, top + 22, TEXT);

        // The listing: the entries of this category, ROWS_PER_PAGE to a page,
        // in the pane's left column.
        final int page = this.browser.page();
        final List<ResearchBrowserModel.Row> rows = this.browser.page(category, page);
        final ResearchBrowserModel.Row selected = this.browser.selectedRow();
        guiGraphics.fill(left + 8, top + ROWS_TOP - 2, left + LIST_WIDTH - 2,
                top + ROWS_TOP + ResearchBrowserModel.ROWS_PER_PAGE * ROW_PITCH - 1, PLATE_FILL);
        for (int i = 0; i < rows.size(); i++) {
            final ResearchBrowserModel.Row row = rows.get(i);
            final int rowTop = top + ROWS_TOP + i * ROW_PITCH;
            final boolean isSelected = selected != null && selected.key().equals(row.key());
            if (isSelected) {
                guiGraphics.fill(left + 9, rowTop - 1, left + LIST_WIDTH - 3, rowTop + ROW_PITCH - 1, HIGHLIGHT);
            }
            final int number = page * ResearchBrowserModel.ROWS_PER_PAGE + i + 1;
            guiGraphics.drawString(this.font, Component.literal(number + ". " + trim(row.label(), 24)),
                    left + 12, rowTop, isSelected ? 0xFFFFFFFF : TEXT);
        }

        renderPageFooter(guiGraphics, left, top, category, page);
        renderDetailPanel(guiGraphics, left, top, selected, mouseX, mouseY);
        renderHoverHint(guiGraphics, left, top);
        renderPopup(guiGraphics, left, top);
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

    /**
     * The per-entry detail panel (BOOK-4). Every line is one
     * {@link ResearchDetailModel.Detail.Line} computed out of the graph and the
     * player's knowledge — state, missing prerequisites, warp, essentia
     * requirements, and one clickable row per related entry — followed by the
     * graph's own text for the entry. The window adds no copy of its own, wraps
     * the lines to the column's width, and counts the lines it cannot fit
     * instead of inventing them. A link row opens that entry, which is the
     * original's node click ({@code GuiResearchBrowser.java:711}).
     */
    private void renderDetailPanel(final GuiGraphics guiGraphics, final int left, final int top,
                                   final ResearchBrowserModel.Row selected, final int mouseX, final int mouseY) {
        this.detailHits.clear();
        final int panelLeft = left + DETAIL_LEFT;
        final int panelTop = top + ROWS_TOP - 2;
        final int panelBottom = top + PANE_HEIGHT - 4;
        guiGraphics.fill(panelLeft - 4, panelTop, left + PANE_WIDTH - 4, panelBottom, PLATE_FILL);

        final ResearchDetailModel.Detail detail = selected == null
                ? null : this.details.detail(selected.key(), playerKnowledge());
        if (detail == null) {
            guiGraphics.drawString(this.font, Component.literal("Pick an entry"),
                    panelLeft, panelTop + 6, DIM_TEXT);
            return;
        }

        // The panel's text, wrapped to the column: the entry's own label, the
        // model's lines, then the graph's own text for the entry.
        final List<FlatLine> flat = new ArrayList<>();
        wrap(flat, detail.label(), 0xFFFFFFFF, null);
        final List<ResearchDetailModel.Detail.Line> lines = detail.displayLines();
        for (int i = 0; i < lines.size(); i++) {
            final ResearchDetailModel.Detail.Line line = lines.get(i);
            final int color = i == 0 ? stateColor(detail.state())
                    : line.linkKey() != null ? LINK_TEXT : TEXT;
            wrap(flat, line.text(), color, line.linkKey());
        }
        // The affordance line: the original's per-state hint for the hovered
        // row (its tooltip pass, GuiResearchBrowser.java:608-618 / :651-659),
        // merged onto this panel. The five states and their colours are
        // BookNoteRequest's; this window only draws them.
        if (selected != null && selected.key().equals(this.hint.key())
                && this.hint.hintKey() != null) {
            wrap(flat, Component.translatable(this.hint.hintKey()).getString(),
                    0xFF000000 | BookNoteRequest.colorFor(this.hint.hintKey()), null);
        }
        final String body = this.browser.selectedText();
        if (!body.isEmpty()) {
            wrap(flat, body, TEXT, null);
        }

        int y = panelTop + 3;
        int shown = 0;
        for (final FlatLine line : flat) {
            if (y + DETAIL_PITCH > panelBottom) {
                break;
            }
            final int lineTop = y;
            if (line.key() != null) {
                final boolean over = mouseX >= panelLeft - 3 && mouseX < panelLeft + DETAIL_WIDTH
                        && mouseY >= lineTop - 1 && mouseY < lineTop + DETAIL_PITCH - 1;
                if (over) {
                    guiGraphics.fill(panelLeft - 3, lineTop - 1, panelLeft + DETAIL_WIDTH,
                            lineTop + DETAIL_PITCH - 1, LINK_HOVER);
                }
                this.detailHits.add(new DetailHit(line.key(), panelLeft - 3, lineTop - 1,
                        panelLeft + DETAIL_WIDTH, lineTop + DETAIL_PITCH - 1));
            }
            guiGraphics.drawString(this.font, line.text(), panelLeft, lineTop, line.color());
            y += DETAIL_PITCH;
            shown++;
        }
        if (shown < flat.size()) {
            guiGraphics.drawString(this.font, Component.literal("+" + (flat.size() - shown) + " more"),
                    panelLeft, y, DIM_TEXT);
        }
    }

    /** One wrapped row of the detail panel, with its colour and click target. */
    private record FlatLine(FormattedCharSequence text, int color, String key) {
    }

    /** Append one panel line, wrapped to the column's width, as FlatLine rows. */
    private void wrap(final List<FlatLine> out, final String text, final int color, final String key) {
        for (final FormattedCharSequence line
                : this.font.split(Component.literal(text.replace("<BR>", "  ")), DETAIL_WIDTH)) {
            out.add(new FlatLine(line, color, key));
        }
    }

    /** The state's colour: the original dims a locked node and pulses an open one (:370-376). */
    private static int stateColor(final ResearchDetailModel.State state) {
        return switch (state) {
            case COMPLETED -> DIM_TEXT;
            case AVAILABLE -> STATE_AVAILABLE;
            case LOCKED -> STATE_LOCKED;
        };
    }

    // ---------------------------------- the note / purchase arm (TC-M3-04-s-v)

    /**
     * The client's own readings for a click, as the original read them
     * in-process at {@code GuiResearchBrowser.java:683-727}: the
     * {@code research_difficulty} config value, the scribing kit
     * ({@code hasScribestuff}, {@code :104-108}), whether a note for this entry
     * is already carried, and the synced aspect pools. The server re-runs every
     * one of those gates on its own copy
     * ({@code ResearchManager.handlePlayerCompleteRequest} +
     * {@code ResearchManager.createResearchNoteForPlayer}), so a client that
     * lies about them gets nothing.
     */
    private ResearchDetailModel.AffordanceContext affordanceContext(final String key) {
        final List<ItemStack> rows =
                this.minecraft == null || this.minecraft.player == null
                        ? List.of() : this.minecraft.player.getInventory().items;
        final var pools = ClientNetworkData.aspectPools();
        return new ResearchDetailModel.AffordanceContext(
                TcConfig.getInt(TcConfigCatalog.RESEARCH_DIFFICULTY),
                ResearchManager.hasScribingInk(rows),
                ResearchManager.noteSlot(rows, key) >= 0,
                tag -> pools.getOrDefault(tag, 0));
    }

    /** What a click on {@code key} would ask the server for. */
    public BookNoteRequest.Request requestFor(final String key) {
        return this.details.affordance(key, playerKnowledge(), affordanceContext(key));
    }

    /**
     * Send the row's request on the single server path: the same
     * {@code PlayerCompleteToServerPayload} the note item's chain drives
     * research through — type 0 for an aspect purchase, type 1 for a note. The
     * book never completes research itself; {@code Arm.NOTE} asks the server
     * for a puzzle note and the note item is what completes the entry.
     *
     * <p>The popup is set on the click, as the original's was
     * ({@code :717-727}): the original had no reply packet to wait for and the
     * gates are identical on both sides, so the note is normally there.</p>
     */
    private void requestEntry(final String key) {
        final BookNoteRequest.Request request = requestFor(key);
        this.hint = request;
        switch (request.arm()) {
            case NONE -> {
                return;
            }
            case PURCHASE -> PacketDistributor.sendToServer(new PlayerCompleteToServerPayload(
                    key, PlayerCompleteToServerPayload.REQUEST_COMPLETE));
            case NOTE -> {
                PacketDistributor.sendToServer(new PlayerCompleteToServerPayload(
                        key, PlayerCompleteToServerPayload.REQUEST_NOTE));
                this.popupText = BookNoteRequest.popupMessage(ResearchManager.graph().entry(key));
                this.popupUntil = System.currentTimeMillis() + BookNoteRequest.POPUP_MILLIS;
            }
        }
    }

    /** The row under the cursor of the current page, or null (the original's node hit test). */
    private String rowAt(final int mouseX, final int mouseY) {
        final int left = paneLeft();
        final int top = paneTop();
        final List<ResearchBrowserModel.Row> rows =
                this.browser.page(this.browser.selectedCategory(), this.browser.page());
        for (int i = 0; i < rows.size(); i++) {
            final int rowTop = top + ROWS_TOP + i * ROW_PITCH;
            if (mouseX >= left + 8 && mouseX < left + LIST_WIDTH
                    && mouseY >= rowTop - 1 && mouseY < rowTop + ROW_PITCH - 1) {
                return rows.get(i).key();
            }
        }
        return null;
    }

    /** Recompute the hovered row's affordance (the original's render-time tooltip pass). */
    private void updateHover(final int mouseX, final int mouseY) {
        this.hoverX = mouseX;
        this.hoverY = mouseY;
        final String row = rowAt(mouseX, mouseY);
        this.hint = row == null
                ? new BookNoteRequest.Request(BookNoteRequest.Arm.NONE, null, null)
                : requestFor(row);
    }

    /**
     * The hint line, drawn beside the cursor inside the book pane (the original
     * drew it beside the hovered node, {@code :554-660}); the colour is the
     * original's per-state colour and the box behind it is the original's
     * translucent black ({@code :558}).
     */
    private void renderHoverHint(final GuiGraphics guiGraphics, final int left, final int top) {
        if (this.hint.hintKey() == null) {
            return;
        }
        final String text = Component.translatable(this.hint.hintKey()).getString();
        final int width = this.font.width(text);
        final int x = Math.max(left + 2, Math.min(this.hoverX - width / 2, left + PANE_WIDTH - width - 3));
        final int y = top + PANE_HEIGHT - this.font.lineHeight - 4;
        guiGraphics.fill(x - 3, y - 3, x + width + 3, y + this.font.lineHeight + 2, 0x80000000);
        guiGraphics.drawString(this.font, text, x, y - 1,
                0xFF000000 | BookNoteRequest.colorFor(this.hint.hintKey()), false);
    }

    /**
     * The "got a research note!" popup: the original wrapped the message at
     * 150 px ({@code font.split(popupmessage, 150)}) and drew it centred on the
     * graph pane — {@code xq = (width - paneWidth)/2 + 128},
     * {@code yq = (height - paneHeight)/2 + 128} ({@code :187-191}) — in its
     * khaki colour, while {@code popuptime} was ahead of the clock. The book's
     * 256x230 pane is the same pane the original's centre sat in, so the
     * centre is this pane's centre.
     */
    private void renderPopup(final GuiGraphics guiGraphics, final int left, final int top) {
        if (this.popupText == null || System.currentTimeMillis() > this.popupUntil) {
            return;
        }
        final List<FormattedCharSequence> lines =
                this.font.split(this.popupText, BookNoteRequest.POPUP_WRAP);
        final int blockHeight = lines.size() * this.font.lineHeight;
        final int centreX = left + PANE_WIDTH / 2;
        final int centreY = top + PANE_HEIGHT / 2;
        int y = centreY - blockHeight / 2;
        for (final FormattedCharSequence line : lines) {
            final int x = centreX - this.font.width(line) / 2;
            guiGraphics.drawString(this.font, line, x, y,
                    0xFF000000 | BookNoteRequest.COLOR_POPUP, false);
            y += this.font.lineHeight;
        }
    }

    /** The last popup text set by a note request (test seam; null before the first). */
    public Component popupText() {
        return this.popupText;
    }

    /** The affordance of the row under the cursor, as of the last frame. */
    public BookNoteRequest.Request hint() {
        return this.hint;
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
