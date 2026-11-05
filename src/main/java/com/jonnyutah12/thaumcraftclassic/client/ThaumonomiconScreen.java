package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.research.ResearchCategoryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The Thaumonomicon browser screen (BOOK-1 / t_1e6b18cc, content-mapping row
 * {@code Thaumcraft:GUI_THAUMONOMICON} → {@code thaumcraft:gui_thaumonomicon},
 * original {@code gui_id = 12}).
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
 * ({@code screenIsContainerlessLikeTheOriginal}) and by the game probe
 * {@code bookRegistersNoMenuType}.</p>
 *
 * <p><b>What this slice draws.</b> A placeholder only, as the card scopes it:
 * the title, and one literal line naming the category page the book opened on
 * and the entry count the graph has for it. The original's browser body —
 * the 256x230 pane ({@code :46-47}), the 224x196 pannable map region
 * ({@code :151}), the category tabs, the node grid, the hover popups
 * ({@code GuiResearchPopup}, 134 lines), the page renderer
 * ({@code GuiResearchRecipe}, 1613 lines) and the cheat sheet — is BD-13 and
 * stays on its own cards. The literal placeholder text is deliberate: a
 * placeholder must not mint lang keys that outlive it.</p>
 *
 * <p><b>Close path.</b> The original overrides no key handler and no
 * {@code doesGuiPauseGame}, so vanilla governs it: ESC closes the screen and
 * the screen is a pause screen in singleplayer. This class keeps both vanilla
 * defaults — the same "no override" fidelity the contract test pins
 * structurally, because the game-test harness is a dedicated server and can
 * never fire a key event at a screen.</p>
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
     * packet), so the screen translates this key itself, and it is the ONLY
     * lang key this slice's GUI half adds.
     */
    public static final String TITLE_KEY = "container.thaumcraft.thaumonomicon";

    /** The vanillaGui panel colour used for the placeholder plate. */
    static final int PLACEHOLDER_FILL = 0xFF1D1D2D;
    /** The placeholder text colour (vanilla's light-grey GUI text). */
    static final int PLACEHOLDER_TEXT = 0xFFE0E0E0;

    /**
     * The original's static category memory ({@code GuiResearchBrowser.java:63}
     * {@code private static String selectedCategory}) — the mechanism that
     * makes a reopen land on the page the player was reading.
     */
    private static String lastCategory = null;

    /** The category page this screen instance is showing. */
    private final String selectedCategory;

    public ThaumonomiconScreen(final String category) {
        super(Component.translatable(TITLE_KEY));
        this.selectedCategory = resolveCategory(category);
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
        return this.selectedCategory;
    }

    /** The test seam for the reopen memory (the original's static field). */
    public static String lastCategory() {
        return lastCategory;
    }

    /** The test seam: clear the memory (the state of a fresh client session). */
    public static void forgetCategory() {
        lastCategory = null;
    }

    @Override
    public void removed() {
        // The original's onGuiClosed writes its view state back into the
        // statics (:116-121). Same mechanism, one field in this slice.
        lastCategory = this.selectedCategory;
        super.removed();
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY,
                       final float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // Placeholder plate: the original's pane geometry is 256x230
        // (GuiResearchBrowser.java:46-47), centred the vanilla way.
        final int paneWidth = 256;
        final int paneHeight = 230;
        final int left = (this.width - paneWidth) / 2;
        final int top = (this.height - paneHeight) / 2;
        guiGraphics.fill(left, top, left + paneWidth, top + paneHeight, PLACEHOLDER_FILL);
        this.renderCenteredString(guiGraphics, this.title, left + paneWidth / 2, top + 12);
        final int entries = ResearchManager.graph().entriesIn(this.selectedCategory).size();
        this.renderCenteredString(guiGraphics,
                Component.literal("Category: " + this.selectedCategory + "  (" + entries + " entries)"),
                left + paneWidth / 2, top + 32);
        this.renderCenteredString(guiGraphics,
                Component.literal("Pages and navigation are not implemented (BD-13)."),
                left + paneWidth / 2, top + 48);
    }

    private void renderCenteredString(final GuiGraphics guiGraphics, final Component text,
                                      final int centerX, final int y) {
        guiGraphics.drawCenteredString(this.font, text, centerX, y, PLACEHOLDER_TEXT);
    }
}
