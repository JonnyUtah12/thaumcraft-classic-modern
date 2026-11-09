package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.TrunkMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The Traveling Trunk container screen (content-mapping.csv
 * {@code thaumcraft:GUI_TRUNK}, original {@code GuiTravelingTrunk} for
 * gui_id 2). First container screen authored for the port (TC-M7-04-s-cr1):
 * there is no screen code in the {@code wip-salvage} tree to port, so this is
 * written from scratch against 1.21.1's {@link AbstractContainerScreen} and
 * registered through {@code RegisterMenuScreensEvent} in
 * {@link ModClientScreens}.
 *
 * <p>Layout is inherited from {@link TrunkMenu} verbatim, which carries the
 * original slot offsets: the trunk grid is 9 columns at
 * {@code x = 8 + col*18}, {@code y = 15 + row*23} (the original's 23 px row
 * pitch, wider than vanilla's 18 px), and the player inventory is the standard
 * 3x9 grid at {@code y = 118 + row*18} with the hotbar at {@code y = 176}.</p>
 *
 * <p><b>Background (docs/BEHAVIORAL_DEVIATIONS.md BD-17).</b> The original drew
 * {@code guitrunkbase.png}, a texture that only ships in an asset-overlay build
 * (the original assets are All Rights Reserved and are never committed, D-004),
 * so it is not in the code-only jar this card is gated on. The panel is
 * therefore drawn with vanilla content only: a flat vanilla-toned panel behind
 * the trunk grid, one vanilla {@code minecraft:container/slot} sprite per trunk
 * slot (so the frames sit on the original 23 px pitch instead of being baked
 * into a 18 px-pitch texture), and the vanilla player-inventory panel piece
 * {@code textures/gui/container/generic_54.png} — the same piece
 * {@code ContainerScreen} blits for a chest — under the player grid. No
 * original asset is referenced, and no legacy GL ({@code RenderSystem}
 * texture-binding, {@code GlStateManager}) is used: 1.21.1 draws through
 * {@link GuiGraphics}.</p>
 *
 * <p>The title is the menu's display name, i.e. the component the server's
 * {@code IMenuProvider} puts in the open-screen packet; the trunk has no
 * provider yet (the trunk entity is TC-M6-06's and only a {@code TrunkHost}
 * seam exists), so an empty display name falls back to the shipped menu key
 * {@code container.thaumcraft.gui_trunk} = "Traveling Trunk" rather than
 * rendering blank. No {@code §} colour codes are used anywhere: 1.21.1
 * formatting is carried by {@link Component} styles.</p>
 *
 * <p>Not implemented here, and on their own cards: the original's stay-toggle
 * button (the menu already answers it through
 * {@link TrunkMenu#BUTTON_TOGGLE_STAY} / {@code clickMenuButton}, the GUI
 * button is TC-M7-04 follow-up work) and the machine screens whose content has
 * no menu type yet. The hand-mirror and deconstruction-table screens listed
 * here as follow-ups in cr1 are done: they landed in TC-M7-04-s (t_7eb3e438)
 * as {@link HandMirrorScreen} and {@link DeconstructionTableScreen}, together
 * with {@link MagicBoxScreen} and {@link SpaScreen}, which is every
 * {@code MenuType} the mod registers.</p>
 */
@OnlyIn(Dist.CLIENT)
public class TrunkScreen extends AbstractContainerScreen<TrunkMenu> {

    /** The lang key of the trunk menu (already shipped for the menu type). */
    static final String TITLE_KEY = "container.thaumcraft.gui_trunk";

    /** Vanilla GUI panel base colour (the flat fill of a container panel). */
    static final int PANEL_FILL = 0xFFC6C6C6;
    /** Vanilla panel frame colour, drawn as a 1 px outline. */
    static final int PANEL_FRAME = 0xFF555555;

    /** The vanilla 18x18 dark slot square, from the GUI sprite atlas. */
    static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.withDefaultNamespace("container/slot");

    /**
     * The vanilla player-inventory panel piece: the 176x96 region at
     * {@code (0, 126)} of {@code generic_54.png}, the same blit
     * {@code ContainerScreen} does for a chest.
     */
    static final ResourceLocation PLAYER_PANEL =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    static final int PLAYER_PANEL_U = 0;
    static final int PLAYER_PANEL_V = 126;
    static final int PLAYER_PANEL_WIDTH = 176;
    static final int PLAYER_PANEL_HEIGHT = 96;

    /**
     * Vertical offset between the top of the vanilla player panel piece and the
     * first player slot row inside it, measured off {@code ContainerScreen}: a
     * 3-row chest blits the piece at {@code containerRows*18 + 17 = 71} and its
     * first player row is at {@code 84}, so 13.
     */
    static final int PLAYER_PANEL_TO_FIRST_SLOT = 13;

    /** {@link TrunkMenu} puts the player grid at y = 118. */
    static final int PLAYER_GRID_TOP = 118;

    /** Where the vanilla player panel piece starts, for this menu's layout. */
    static final int PLAYER_PANEL_TOP = PLAYER_GRID_TOP - PLAYER_PANEL_TO_FIRST_SLOT;

    /** Panel width: 9 columns of 18 px with the vanilla 8 px inset each side. */
    static final int PANEL_WIDTH = 176;

    /** Total panel height: the trunk region plus the vanilla player piece. */
    static final int PANEL_HEIGHT = PLAYER_PANEL_TOP + PLAYER_PANEL_HEIGHT;

    private final int trunkRows;

    public TrunkScreen(final TrunkMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title.getString().isEmpty() ? Component.translatable(TITLE_KEY) : title);
        this.trunkRows = Math.max(0, menu.getRowCount());
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        // titleLabelY stays above the first trunk row (y = 15, frame top 14).
        this.titleLabelX = 8;
        this.titleLabelY = 4;
        // Three rows below the piece top, matching vanilla's inventoryLabelY.
        this.inventoryLabelX = 8;
        this.inventoryLabelY = PLAYER_PANEL_TOP + 3;
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY,
                       final float partialTick) {
        // super.render draws the background, every active slot (items + the
        // carried stack + the hover highlight) and the labels.
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick,
                            final int mouseX, final int mouseY) {
        // The trunk grid region: a flat panel because the original texture is
        // not in a code-only jar (BD-15).
        guiGraphics.fill(this.leftPos, this.topPos,
                this.leftPos + PANEL_WIDTH, this.topPos + PLAYER_PANEL_TOP, PANEL_FILL);
        guiGraphics.renderOutline(this.leftPos, this.topPos, PANEL_WIDTH, PANEL_HEIGHT, PANEL_FRAME);

        // The player inventory: the vanilla chest panel piece, aligned to the
        // menu's own 118 px player row.
        guiGraphics.blit(PLAYER_PANEL,
                this.leftPos, this.topPos + PLAYER_PANEL_TOP,
                PLAYER_PANEL_U, PLAYER_PANEL_V, PLAYER_PANEL_WIDTH, PLAYER_PANEL_HEIGHT);

        // One vanilla slot square per trunk slot, on the original 23 px pitch.
        // The player rows are already framed by the panel piece above.
        final int trunkSlotCount = this.trunkRows * 9;
        for (int i = 0; i < trunkSlotCount && i < this.menu.slots.size(); i++) {
            final Slot slot = this.menu.slots.get(i);
            guiGraphics.blitSprite(SLOT_SPRITE, slot.x - 1, slot.y - 1, 18, 18);
        }
    }
}
