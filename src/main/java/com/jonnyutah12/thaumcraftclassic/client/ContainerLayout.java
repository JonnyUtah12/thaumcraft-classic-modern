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

    // ------------------------------------------- alchemical furnace (gui_id 9)

    /**
     * The original alchemical-furnace screen and container:
     * {@code GuiAlchemyFurnace} keeps the 1.7.10 {@code GuiContainer} default
     * panel (176x166) over the vanilla player grid at {@code 84}/{@code 142}
     * (the {@code PLAYER_GRID_TOP}/{@code PLAYER_HOTBAR_TOP} constants), and
     * {@code ContainerAlchemyFurnace} places its two machine slots at
     * {@code (80,8)} (the aspect-limited input) and {@code (80,48)} (the fuel
     * slot). All three widgets are bottom-anchored fills:
     * the 16x20 fire at {@code (80,26)}
     * ({@code getBurnTimeRemainingScaled(20)} over {@code currentItemBurnTime},
     * guard 200), the 9x46 cook rail at {@code (106,13)}
     * ({@code getCookProgressScaled(46)} over {@code smeltTime}), and the 8x48
     * vis column at {@code (61,12)} ({@code getContentsScaled(48)} over the
     * 50-unit tank) inside the 10x55 tank frame at {@code (60,8)}.
     */
    public static final int FURNACE_SLOT_INPUT_X = 80;
    public static final int FURNACE_SLOT_INPUT_Y = 8;
    public static final int FURNACE_SLOT_FUEL_X = 80;
    public static final int FURNACE_SLOT_FUEL_Y = 48;

    public static final int FURNACE_FIRE_X = 80;
    public static final int FURNACE_FIRE_TOP = 26;
    public static final int FURNACE_FIRE_WIDTH = 16;
    public static final int FURNACE_FIRE_PIXELS = 20;

    public static final int FURNACE_COOK_X = 106;
    public static final int FURNACE_COOK_TOP = 13;
    public static final int FURNACE_COOK_WIDTH = 9;
    public static final int FURNACE_COOK_PIXELS = 46;

    public static final int FURNACE_TANK_X = 60;
    public static final int FURNACE_TANK_TOP = 8;
    public static final int FURNACE_TANK_WIDTH = 10;
    public static final int FURNACE_TANK_HEIGHT = 55;

    public static final int FURNACE_VIS_X = 61;
    public static final int FURNACE_VIS_TOP = 12;
    public static final int FURNACE_VIS_WIDTH = 8;
    public static final int FURNACE_VIS_PIXELS = 48;

    /** Original {@code getBurnTimeRemainingScaled} guard: a 0 burn max reads as 200. */
    public static final int FURNACE_FALLBACK_BURN_TIME = 200;

    /** Authored stand-in for the original cook-bar texels, overlay-only (BD-17). */
    public static final int FURNACE_COOK_FILL = 0xFFE8AF5E;

    // ------------------------------------------------- arcane bore (gui_id 15)

    /**
     * The original bore screen: {@code GuiArcaneBore} overrides the panel to
     * 176x141 and {@code ContainerArcaneBore} binds the player grid at
     * {@code 59} with the hotbar at {@code 117} — a short panel. The vanilla
     * derivation from the player origin (this class's rule) gives 142; the
     * 1 px sheet difference is the original's own rounding, recorded in the
     * {@link #BORE_SHEET_HEIGHT} doc, not applied.
     */
    public static final int BORE_PLAYER_GRID_TOP = 59;
    public static final int BORE_PLAYER_HOTBAR_TOP = 117;

    /** The original's literal {@code field_147000_g = 141}; the derived panel is 142. */
    public static final int BORE_SHEET_HEIGHT = 141;

    /** {@code ContainerArcaneBore}: the excavation-focus slot at {@code (26,18)}. */
    public static final int BORE_SLOT_FOCUS_X = 26;
    public static final int BORE_SLOT_FOCUS_Y = 18;

    /** {@code ContainerArcaneBore}: the pickaxe slot at {@code (74,18)}. */
    public static final int BORE_SLOT_PICKAXE_X = 74;
    public static final int BORE_SLOT_PICKAXE_Y = 18;

    /**
     * The 16x16 "about to break" icon {@code GuiArcaneBore} blits over the
     * pickaxe slot when {@code damage + 1 >= maxDamage}.
     */
    public static final int BORE_BROKEN_X = 74;
    public static final int BORE_BROKEN_Y = 18;
    public static final int BORE_BROKEN_SIZE = 16;

    /** Authored stand-in for the original broken-tool texels (BD-17). */
    public static final int BORE_BROKEN_FILL = 0xFFB6412F;

    /**
     * The 0.5-scaled readout block: {@code GuiArcaneBore} translates to
     * {@code (112,8)}, scales 0.5, and draws Width at {@code (0,0)}, Speed at
     * {@code (0,10)}, "Other properties:" at {@code (0,24)}, then a property
     * list starting at {@code y = 34} stepping 9.
     */
    public static final int BORE_TEXT_X = 112;
    public static final int BORE_TEXT_Y = 8;
    public static final int BORE_TEXT_WIDTH_Y = 0;
    public static final int BORE_TEXT_SPEED_Y = 10;
    public static final int BORE_TEXT_OTHER_Y = 24;
    public static final int BORE_TEXT_ROWS_TOP = 34;
    public static final int BORE_TEXT_ROW_STEP = 9;

    /** The original's three property colours (GuiArcaneBore decimal literals). */
    public static final int BORE_COLOR_NATIVE = 0xFFC0C0C0;   // 12632256
    public static final int BORE_COLOR_FORTUNE = 0xFFEEC64A;  // 15648330
    public static final int BORE_COLOR_SILK = 0xFF8080FF;     // 8421631
    public static final int BORE_COLOR_TEXT = 0xFFFFFFFF;     // 16777215

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
     * A bottom-anchored gauge fill, the original's scaled-getter shape shared by
     * every alchemical-furnace widget: {@code getCookProgressScaled(46)},
     * {@code getContentsScaled(48)} and {@code getBurnTimeRemainingScaled(20)}
     * are all {@code value * pixels / max} clamped into the rail, with a
     * non-positive max or value reading as empty. The fill's top edge is
     * {@link #barTop(int, int, int)} of its rail, so every gauge grows upward
     * from its rail bottom — exactly how the three furnace blits anchor.
     */
    public static int scaledProgress(final int value, final int max, final int pixels) {
        if (value <= 0 || max <= 0) {
            return 0;
        }
        final int fill = value * pixels / max;
        return Math.min(fill, pixels);
    }

    /**
     * The bore's drawn width, {@code GuiArcaneBore}'s
     * {@code 1 + (area + maxRadius) * 2}.
     */
    public static int boreWidth(final int area, final int maxRadius) {
        return 1 + (area + maxRadius) * 2;
    }

    /**
     * The bore's break-icon gate: {@code GuiArcaneBore} blits the 16x16 icon
     * when {@code damage + 1 >= maxDamage} on the pickaxe slot's stack.
     */
    public static boolean borePickaxeNearBroken(final int damage, final int maxDamage) {
        return damage + 1 >= maxDamage;
    }

    private ContainerLayout() {
    }
}
