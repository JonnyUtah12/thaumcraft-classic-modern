package com.jonnyutah12.thaumcraftclassic.client;

import net.minecraft.resources.ResourceLocation;

/**
 * The shared, world-free container-panel geometry the port's authored screens
 * draw with (TC-M7-04-s). Every value here is either a vanilla constant that
 * {@code AbstractContainerScreen} / {@code ContainerScreen} themselves use, or
 * a coordinate measured off the original client class the screen replaces
 * ({@code reference/decompiled/thaumcraft/thaumcraft/client/gui/}).
 *
 * <p>Why it exists: the port has no client window in its unit or game-test
 * harness, so a screen's arithmetic is the only part of it that can be pinned
 * by a test. Keeping the arithmetic in static {@code int} functions means
 * {@code ContainerLayoutTest} can assert the geometry without constructing a
 * {@code Screen} or a {@code Minecraft} client, and the screens stay thin
 * drawing code.</p>
 *
 * <p><b>Panel geometry.</b> Every original GUI sheet in the reference tree is
 * 256x256 with the panel drawn at (0,0), and the 1.7.10 default
 * {@code GuiContainer} size is 176x166 — the same 176 px width 1.21.1's
 * {@code AbstractContainerScreen} defaults to. The height is not a constant:
 * the player inventory piece of {@code generic_54.png} is blitted at
 * {@code y = 71} for the original player grid at {@code y = 84} (vanilla: a
 * 3-row chest blits it at {@code rows*18 + 17 = 71} and its first player row is
 * at {@code 84}), so it is derived from the menu's own player-row origin — the
 * same derivation {@code TrunkScreen} (TC-M7-04-s-cr1) does with its 118 px
 * grid, which is why its numbers are cross-checked against this class in the
 * tests rather than duplicated here.</p>
 *
 * <p><b>Backgrounds (docs/BEHAVIORAL_DEVIATIONS.md BD-17).</b> The original
 * sheets ({@code guihandmirror.png}, {@code gui_decontable.png},
 * {@code gui_spa.png}) ship only in the asset-overlay build; the original
 * assets are All Rights Reserved and are never committed (D-004), so the
 * code-only jar this work is gated on cannot reference them. The screens draw
 * the vanilla panel colours and the vanilla
 * {@code minecraft:container/slot} sprite instead, at the original
 * coordinates.</p>
 */
public final class ContainerLayout {

    // ------------------------------------------------------------ panel

    /** 1.21.1's {@code AbstractContainerScreen} default panel width (and the originals'). */
    public static final int PANEL_WIDTH = 176;

    /** The 18x18 vanilla slot frame, and the 18 px grid pitch. */
    public static final int SLOT_FRAME = 18;

    /** Columns in the player inventory grid (and in every machine grid here). */
    public static final int GRID_COLUMNS = 9;

    /** Vanilla player-inventory piece inside {@code generic_54.png}: {@code (0,126,176,96)}. */
    public static final int PLAYER_PANEL_U = 0;
    public static final int PLAYER_PANEL_V = 126;
    public static final int PLAYER_PANEL_HEIGHT = 96;

    /** Vanilla: piece top = first player row - 13 (3-row chest: 71 and 84). */
    public static final int PLAYER_PANEL_TO_FIRST_SLOT = 13;

    /** The player grid / hotbar origin every original container here uses. */
    public static final int PLAYER_GRID_TOP = 84;
    public static final int PLAYER_HOTBAR_TOP = 142;

    /** The flat vanilla panel fill and its 1 px frame (BD-17). */
    public static final int PANEL_FILL = 0xFFC6C6C6;
    public static final int PANEL_FRAME = 0xFF555555;

    /** The vanilla 18x18 dark slot square, from the GUI sprite atlas. */
    public static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.withDefaultNamespace("container/slot");

    /** The vanilla player-inventory panel piece, blitted as vanilla does for a chest. */
    public static final ResourceLocation PLAYER_PANEL =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    // ------------------------------------------- deconstruction table bar

    /** {@code TileDeconstructionTable.BREAK_TIME} — the original's 40-tick break. */
    public static final int DECON_BREAK_TIME = 40;

    /**
     * The original break bar: {@code GuiDeconstructionTable.func_146976_a} blits a
     * 9 px column at {@code (k+93, l+15+46-i1)} with
     * {@code i1 = TileDeconstructionTable.getBreakTimeScaled(46) = breaktime*46/40},
     * so the bar drains downward from {@code y = 15}.
     */
    public static final int DECON_BAR_X = 93;
    public static final int DECON_BAR_TOP = 15;
    public static final int DECON_BAR_WIDTH = 9;
    public static final int DECON_BAR_PIXELS = 46;

    /** The 16x16 aspect box the original drew the revealed aspect into ({@code (64,48)}). */
    public static final int DECON_ASPECT_X = 64;
    public static final int DECON_ASPECT_Y = 48;
    public static final int DECON_ASPECT_SIZE = 16;

    /** Authored stand-in for the original bar texels, which only ship in the overlay (BD-17). */
    public static final int DECON_BAR_FILL = 0xFF8A5CC8;

    // ------------------------------------------------------- arcane spa

    /**
     * The original spa's tank column and mix indicator:
     * {@code GuiSpa.func_146976_a} blits the 10x55 tank frame at {@code (106,11)},
     * the fluid column at {@code (107,15)} up to 48 px, and the 8x8 mix
     * indicator at {@code (89,35)}.
     */
    public static final int SPA_TANK_X = 106;
    public static final int SPA_TANK_TOP = 11;
    public static final int SPA_TANK_WIDTH = 10;
    public static final int SPA_TANK_HEIGHT = 55;
    public static final int SPA_FLUID_X = 107;
    public static final int SPA_FLUID_TOP = 15;
    public static final int SPA_FLUID_WIDTH = 10;
    public static final int SPA_FLUID_PIXELS = 48;

    /**
     * The original 8x8 mix indicator at {@code (89,35)}: {@code GuiSpa} blits
     * sheet {@code (208,16)} when {@code getMix()} is true and {@code (208,32)}
     * when it is false, so the port fills the box on and frames it off (BD-17:
     * the sheet is overlay-only). Its tooltip hitbox is the 10x10 at
     * {@code (88,34)}, and the tank's fluid tooltip hitbox is the 10x55 at
     * {@code (104,10)} — both measured off {@code GuiSpa.func_73863_a}.
     */
    public static final int SPA_MIX_X = 89;
    public static final int SPA_MIX_Y = 35;
    public static final int SPA_MIX_SIZE = 8;
    public static final int SPA_MIX_TIP_X = 88;
    public static final int SPA_MIX_TIP_Y = 34;
    public static final int SPA_MIX_TIP_SIZE = 10;
    public static final int SPA_TANK_TIP_X = 104;
    public static final int SPA_TANK_TIP_Y = 10;
    public static final int SPA_TANK_TIP_WIDTH = 10;
    public static final int SPA_TANK_TIP_HEIGHT = 55;

    /** Authored stand-in for the original mix-on texel, which is overlay-only (BD-17). */
    public static final int SPA_MIX_ON_FILL = 0xFF8A5CC8;

    // ------------------------------------------------- magic-box paging

    /**
     * The authored page widget (the original container carried the row in
     * {@code bindBoxInventory(row)}; its GUI had no paging control — the
     * ClientHandler swapped in a {@code ContainerChest} over all rows, and
     * this port keeps the container-rebind model instead, BD-17). Two 10x10
     * arrows sit in the panel's title band above the third box row.
     */
    public static final int MAGIC_BOX_PAGE_PREV_X = 152;
    public static final int MAGIC_BOX_PAGE_PREV_Y = 2;
    public static final int MAGIC_BOX_PAGE_NEXT_X = 164;
    public static final int MAGIC_BOX_PAGE_NEXT_Y = 2;
    public static final int MAGIC_BOX_PAGE_BUTTON_SIZE = 10;

    /** Page counter text, drawn between the title and the prev arrow. */
    public static final int MAGIC_BOX_PAGE_LABEL_X = 118;
    public static final int MAGIC_BOX_PAGE_LABEL_Y = 4;
    public static final int MAGIC_BOX_PAGE_LABEL_COLOR = 0x404040;

    /** The original {@code bindBoxInventory} row: 3 rows of 9 slots per page. */
    public static final int MAGIC_BOX_PAGE_SLOTS = 27;

    // ---------------------------------------------------- trunk stay

    /**
     * The original stay-toggle control: {@code GuiTravelingTrunk} blits the
     * 10x10 {@code (176,0)} texel at {@code (112,0)} while {@code getStay()}
     * is true (its base sits in the sheet) and its click box is
     * {@code k1 < 10 && l1 <= 10} — an 11-px tall inclusive box, kept verbatim.
     */
    public static final int TRUNK_STAY_X = 112;
    public static final int TRUNK_STAY_Y = 0;
    public static final int TRUNK_STAY_WIDTH = 10;
    public static final int TRUNK_STAY_HEIGHT = 10;

    /** Authored stand-in for the original stay-on texel, which is overlay-only (BD-17). */
    public static final int TRUNK_STAY_ON_FILL = 0xFF8A5CC8;

    // ------------------------------------------------------------- geometry

    /** Panel top for a given player-grid origin (vanilla's 13 px piece offset). */
    public static int playerPanelTop(final int playerGridTop) {
        return playerGridTop - PLAYER_PANEL_TO_FIRST_SLOT;
    }

    /** Total panel height for a given player-grid origin. */
    public static int panelHeight(final int playerGridTop) {
        return playerPanelTop(playerGridTop) + PLAYER_PANEL_HEIGHT;
    }

    /** Vanilla centring: the panel's left edge for a given window width. */
    public static int leftPos(final int screenWidth) {
        return (screenWidth - PANEL_WIDTH) / 2;
    }

    /** Vanilla centring: the panel's top edge for a given window height. */
    public static int topPos(final int screenHeight, final int height) {
        return (screenHeight - height) / 2;
    }

    /** The vanilla slot sprite sits one pixel up-left of the slot's item anchor. */
    public static int slotFrameX(final int slotX) {
        return slotX - 1;
    }

    public static int slotFrameY(final int slotY) {
        return slotY - 1;
    }

    /** Rows needed for a flat slot count on an 9-wide grid. */
    public static int gridRows(final int slotCount) {
        return (slotCount + GRID_COLUMNS - 1) / GRID_COLUMNS;
    }

    /**
     * True when a machine slot's 18x18 frame lies fully inside the machine region
     * of the panel (above the player piece). The frame is drawn one pixel up-left
     * of the slot anchor, so its last row is {@code slotY + 17}. Slots past the
     * region are still live in the menu — the magic box exposes up to 216 of
     * them, the original paged them — but framing them would draw over the player
     * grid, so they are left unframed and the deviation is recorded (BD-17).
     */
    public static boolean fitsInMachineRegion(final int slotY, final int machineRegionHeight) {
        return slotY + SLOT_FRAME - 1 <= machineRegionHeight;
    }

    /** Vanilla {@code inventoryLabelY} for a derived panel (cr1's trunk uses piece top + 3). */
    public static int inventoryLabelY(final int playerGridTop) {
        return playerPanelTop(playerGridTop) + 3;
    }

    /** The title baseline every screen in this crate uses (8 px inset, 4 px down). */
    public static int titleLabelX() {
        return 8;
    }

    public static int titleLabelY() {
        return 4;
    }

    /**
     * The original break bar's remaining height:
     * {@code getBreakTimeScaled(pixels)} = {@code breaktime * pixels / BREAK_TIME},
     * clamped so a bar never paints outside its 46 px rail.
     */
    public static int scaledBarHeight(final int breaktime, final int pixels) {
        if (breaktime <= 0) {
            return 0;
        }
        if (breaktime >= DECON_BREAK_TIME) {
            return pixels;
        }
        return breaktime * pixels / DECON_BREAK_TIME;
    }

    /** The original bar's top edge for a fill height (it drains downward). */
    public static int barTop(final int barTop, final int pixels, final int fill) {
        return barTop + pixels - fill;
    }

    /**
     * The original spa fluid column height: {@code GuiSpa} computes
     * {@code bar = fluidAmount / capacity} and leaves the top
     * {@code (int)(48 - 48*bar)} px of the 48 px column blank, i.e. the
     * column fills from the bottom with {@code floor(amount * 48 / capacity)}
     * pixels. Clamped: an empty or over-full tank never paints outside the rail.
     */
    public static int scaledFluidHeight(final int amount, final int capacity, final int pixels) {
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }
        if (amount >= capacity) {
            return pixels;
        }
        return (int)((long)amount * pixels / capacity);
    }

    /** The original fluid column's top edge for a fill height (it fills from the bottom). */
    public static int fluidTop(final int columnTop, final int pixels, final int fill) {
        return columnTop + pixels - fill;
    }

    /**
     * Pages the magic box spans: the original {@code ContainerMagicBox} binds
     * 3 rows of 9 per {@code bindBoxInventory(row)} call, so a box of
     * {@code boxSlotCount} contents spans {@code ceil(count / 27)} pages —
     * at least one, so a lone 27-slot box has a page and no live arrows.
     */
    public static int magicBoxPageCount(final int boxSlotCount) {
        if (boxSlotCount <= 0) {
            return 1;
        }
        return (boxSlotCount + MAGIC_BOX_PAGE_SLOTS - 1) / MAGIC_BOX_PAGE_SLOTS;
    }

    /** The first box-slot index of a page (the original's {@code (j + row) * 9}). */
    public static int magicBoxPageStart(final int page) {
        return page * MAGIC_BOX_PAGE_SLOTS;
    }

    /** Clamp a page index into the box's page range. */
    public static int clampPage(final int page, final int pageCount) {
        return Math.max(0, Math.min(page, pageCount - 1));
    }

    /**
     * The original stay-button box: {@code k1 >= 0 && l1 >= 0 && k1 < 10 &&
     * l1 <= 10} — the {@code <=} keeps row 10 inside the box, reproduced verbatim.
     */
    public static boolean inTrunkStayBox(final int relX, final int relY) {
        return relX >= 0 && relY >= 0 && relX < TRUNK_STAY_WIDTH && relY <= TRUNK_STAY_HEIGHT;
    }

    private ContainerLayout() {
    }
}
