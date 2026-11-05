package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.ResearchNoteMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The research-note screen — the MINIMAL placeholder for
 * {@code thaumcraft:gui_research_note} (content-mapping.csv row 972, original
 * {@code GuiResearchTable}, TC-M3-04-s-iii).
 *
 * <p><b>Why a placeholder ships at all (docs/BEHAVIORAL_DEVIATIONS.md BD-20,
 * TEMPORARY, the BD-22 / BOOK-1 precedent wording).</b> A registered
 * {@code MenuType} without a screen mapping fails the build's screen-coverage
 * gate (the TrunkScreenRegistrationTest / MenuTypeScreenCoverage lineage), and
 * the hex-grid presentation — the grid cells, the scribe/note slot art, the
 * RESEARCHDUPE plate, the erase/copy buttons and the aspect tooltips of
 * {@code GuiResearchTable.java} — is its own presentation card (NOTE-SCREEN-2).
 * So this card registers the menu contract and this plain panel: a flat
 * vanilla-toned panel, the menu title, and the two texts the contract already
 * publishes state for — the original's out-of-ink tooltip lines
 * ({@code GuiResearchTable.java:209-215}, keys
 * {@code tile.researchtable.noink.0/1}) while the scribe slot has no ink, and
 * the copy-cost header ({@code GuiResearchTable.java:327}, key
 * {@code tc.research.copy}) while the copy plate's gate is lit. The strings are
 * the original's, verbatim; no original texture is referenced (ARR, D-004) and
 * no legacy GL is used.</p>
 *
 * <p>The slot sprites and the player-inventory rows render through vanilla
 * {@link AbstractContainerScreen}: the menu carries the original's slot
 * coordinates, so the panel shows the original's layout in vanilla parts. The
 * ink and copy gates read the synced slot state, the same state the hex screen
 * will draw against — this panel is the contract's first consumer.</p>
 */
@OnlyIn(Dist.CLIENT)
public class ResearchNoteScreen extends AbstractContainerScreen<ResearchNoteMenu> {

    /** The lang key of the note menu (the registered id's key, shipped). */
    static final String TITLE_KEY = "container.thaumcraft.gui_research_note";

    /** The original's two out-of-ink lines (GuiResearchTable.java:209-215). */
    static final String NO_INK_LINE_0 = "tile.researchtable.noink.0";
    static final String NO_INK_LINE_1 = "tile.researchtable.noink.1";
    /** The original's copy-cost header (GuiResearchTable.java:327). */
    static final String COPY_HEADER = "tc.research.copy";

    /** Vanilla GUI panel base colour (the flat fill of a container panel). */
    static final int PANEL_FILL = 0xFFC6C6C6;
    /** Vanilla panel frame colour, drawn as a 1 px outline. */
    static final int PANEL_FRAME = 0xFF555555;
    /** The ink warning's text colour: the vanilla container label grey. */
    static final int LABEL_COLOUR = 0xFF404040;

    public ResearchNoteScreen(final ResearchNoteMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        // The original's panel: 176 wide, and the player rows run to the hotbar
        // at y 233 + 18 (ContainerResearchTable.java:31,35), so 251 tall.
        this.imageWidth = 176;
        this.imageHeight = 251;
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick,
                            final int mouseX, final int mouseY) {
        // BD-20 placeholder panel: vanilla fill and frame, no original texture
        // (the TrunkScreen precedent: a code-only jar has no research-table GUI
        // texture, ARR, D-004). Slots draw through vanilla renderSlots.
        final int left = this.leftPos;
        final int top = this.topPos;
        graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_FILL);
        graphics.renderOutline(left, top, this.imageWidth, this.imageHeight, PANEL_FRAME);
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        // The title: the menu display name from the open-screen packet, whose
        // translatable key is TITLE_KEY (the same key the localization audit
        // requires for the registered menu type).
        super.renderLabels(graphics, mouseX, mouseY);
        if (!this.menu.hasInk()) {
            // The original draws these two lines as the scribe-slot tooltip when
            // consumeInkFromTable(contents[0], false) is false; the placeholder
            // has no tooltip pass, so it draws them under the scribe slot.
            graphics.drawString(this.font, Component.translatable(NO_INK_LINE_0),
                    ResearchNoteMenu.SCRIBE_SLOT_X, ResearchNoteMenu.SCRIBE_SLOT_Y + 22, LABEL_COLOUR);
            graphics.drawString(this.font, Component.translatable(NO_INK_LINE_1),
                    ResearchNoteMenu.SCRIBE_SLOT_X, ResearchNoteMenu.SCRIBE_SLOT_Y + 32, LABEL_COLOUR);
        }
        if (this.menu.canDuplicate()) {
            // The plate gate is lit (note + RESEARCHDUPE + complete, NO ink —
            // GuiResearchTable.java:104,254); the plate itself and its cost
            // panel are NOTE-SCREEN-2 (BD-20), the header is this contract's.
            graphics.drawString(this.font, Component.translatable(COPY_HEADER),
                    ResearchNoteMenu.NOTE_SLOT_X - ResearchNoteMenu.NOTE_SLOT_X % 4,
                    ResearchNoteMenu.NOTE_SLOT_Y + 22, LABEL_COLOUR);
        }
    }
}
