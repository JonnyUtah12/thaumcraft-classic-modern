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

    // ------------------------------------------------- golem window (gui_id 0)

    /**
     * The golem inventory window: {@code ContainerGolem.bindGolemInventory}
     * binds six slots at {@code (100 + a/2*28, 16 + a%2*31)} — a 3x2 cell
     * grid on a 28x31 pitch, the original's own page window over
     * {@code a + currentScroll*6} — and {@code GuiGolem.func_146976_a} blits
     * the 24x24 type-varied frame for each at {@code (96 + a/2*28,
     * 12 + a%2*31)}: the frame is the slot anchor four pixels up-left.
     */
    public static final int GOLEM_WINDOW_X = 96;
    public static final int GOLEM_WINDOW_Y = 12;
    public static final int GOLEM_WINDOW_FRAME = 24;
    public static final int GOLEM_WINDOW_COL_PITCH = 28;
    public static final int GOLEM_WINDOW_ROW_PITCH = 31;
    public static final int GOLEM_WINDOW_SLOTS = 6;
    public static final int GOLEM_SLOT_INSET = 4;

    /**
     * The scroll rail: {@code GuiGolem} blits the 24x8 arrows at
     * {@code (111,68)} (up, enabled when {@code currentScroll > 0}) and
     * {@code (135,68)} (down, enabled when {@code currentScroll < maxScroll}),
     * and {@code mouseClicked} sends menu buttons 66/67 on those boxes
     * (the {@code slots > 6} gate).
     */
    public static final int GOLEM_SCROLL_UP_X = 111;
    public static final int GOLEM_SCROLL_DOWN_X = 135;
    public static final int GOLEM_SCROLL_Y = 68;
    public static final int GOLEM_SCROLL_WIDTH = 24;
    public static final int GOLEM_SCROLL_HEIGHT = 8;

    /**
     * The per-slot color tab the original draws above each window frame when
     * the golem carries an Upgrade-4: a 24x12 strip at {@code (96 + a/2*28,
     * 4 + a%2*31)}; the left 8 px decrement the slot color (menu button
     * {@code a + currentScroll*6}), the right 8 px (offset 16) increment it
     * (menu button {@code a + currentScroll*6 + slotCount}).
     */
    public static final int GOLEM_TAB_Y = 4;
    public static final int GOLEM_TAB_WIDTH = 24;
    public static final int GOLEM_TAB_HEIGHT = 12;
    public static final int GOLEM_TAB_BUTTON_WIDTH = 8;
    public static final int GOLEM_TAB_PLUS_OFFSET = 16;

    /**
     * The core-4 (Guard) attack-target toggles: 8x8 boxes at {@code (104,
     * 5 + 16*row)} for rows 0..3, sending menu buttons 51..54 (Monsters,
     * Animals, Players, Creepers).
     */
    public static final int GOLEM_GUARD_X = 104;
    public static final int GOLEM_GUARD_Y = 5;
    public static final int GOLEM_GUARD_PITCH = 16;
    public static final int GOLEM_GUARD_SIZE = 8;
    public static final int GOLEM_GUARD_BUTTON_FIRST = 51;

    /** The core-0 (Fill) amount toggle: 8x8 at {@code (62,54)}, menu button 50. */
    public static final int GOLEM_FILL_X = 62;
    public static final int GOLEM_FILL_Y = 54;
    public static final int GOLEM_FILL_SIZE = 8;

    /** The core-8 (Use) action toggles: 8x8 at {@code (42, 40 + 10*row)}, buttons 50/51/52. */
    public static final int GOLEM_USE_X = 42;
    public static final int GOLEM_USE_Y = 40;
    public static final int GOLEM_USE_PITCH = 10;
    public static final int GOLEM_USE_SIZE = 8;

    /**
     * The sort toggles (Upgrade-5 + {@code ItemGolemCore.canSort}): 64x8 rows
     * at {@code (shiftx, 24 + 10*row + shifty)} with
     * {@code (shiftx, shifty) = core == 10 ? (66, 12) : (180, 0)}, sending
     * menu buttons 55/56/57 (ore-dictionary, damage, NBT).
     */
    public static final int GOLEM_SORT_X = 180;
    public static final int GOLEM_SORT_Y = 24;
    public static final int GOLEM_SORT_PITCH = 10;
    public static final int GOLEM_SORT_WIDTH = 64;
    public static final int GOLEM_SORT_HEIGHT = 8;
    public static final int GOLEM_SORT_SORTING_X = 66;
    public static final int GOLEM_SORT_SORTING_Y = 12;
    public static final int GOLEM_SORT_BUTTON_FIRST = 55;

    /** The original's threat/blurb text anchor: 0.5-scaled {@code (80,22)} → panel {@code (40,11)}. */
    public static final int GOLEM_BLURB_X = 40;
    public static final int GOLEM_BLURB_Y = 11;

    /** The 3x30-scaled golem portrait center: {@code (baseX+51, baseY+75)}. */
    public static final int GOLEM_PORTRAIT_X = 51;
    public static final int GOLEM_PORTRAIT_Y = 75;

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
     * The golem window frame for a page cell {@code a} (0..5): {@code
     * (96 + a/2*28, 12 + a%2*31)} — {@code GuiGolem.func_146976_a}.
     */
    public static int golemFrameX(final int cell) {
        return GOLEM_WINDOW_X + cell / 2 * GOLEM_WINDOW_COL_PITCH;
    }

    public static int golemFrameY(final int cell) {
        return GOLEM_WINDOW_Y + cell % 2 * GOLEM_WINDOW_ROW_PITCH;
    }

    /**
     * The golem window slot anchor for a page cell {@code a}: {@code
     * (100 + a/2*28, 16 + a%2*31)} — {@code ContainerGolem.bindGolemInventory}.
     */
    public static int golemSlotX(final int cell) {
        return golemFrameX(cell) + GOLEM_SLOT_INSET;
    }

    public static int golemSlotY(final int cell) {
        return golemFrameY(cell) + GOLEM_SLOT_INSET;
    }

    /** The sort-toggle x for a core: the Sorting core (10) shifts to 66, the rest sit at 180. */
    public static int golemSortBoxX(final int core) {
        return core == 10 ? GOLEM_SORT_SORTING_X : GOLEM_SORT_X;
    }

    /** The sort-toggle y for a core row: 24 (+ 12 for the Sorting core) + 10*row. */
    public static int golemSortBoxY(final int core, final int row) {
        return GOLEM_SORT_Y + row * GOLEM_SORT_PITCH + (core == 10 ? GOLEM_SORT_SORTING_Y : 0);
    }

    private ContainerLayout() {
    }
}
