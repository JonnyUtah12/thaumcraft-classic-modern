package com.jonnyutah12.thaumcraftclassic.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

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
 * <p><b>Backgrounds (docs/BEHAVIORAL_DEVIATIONS.md BD-37).</b> The original
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

    /** The flat vanilla panel fill and its 1 px frame (BD-37). */
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

    /** Authored stand-in for the original bar texels, which only ship in the overlay (BD-37). */
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
     * grid, so they are left unframed and the deviation is recorded (BD-37).
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


    /**
     * Centi-vis per vis — the same unit {@code wands/WandVis.CENTI_VIS_PER_VIS}
     * uses (the original's {@code * 100}, ItemWandCasting.java:314). Restated here
     * so this class keeps no dependency outside vanilla: it is the one unit the
     * workbench's synced cost slots travel in.
     */
    public static final int CENTI_VIS_PER_VIS = 100;
    /**
     * {@code GuiArcaneWorkbench.field_146999_f} = {@code xSize}
     * ({@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv:648}, "The X
     * size of the inventory window in pixels") — the constructor assigns it 190,
     * so the original panel is <b>190 px wide</b>: vanilla's 176 px player piece
     * plus a 14 px strip, which is why the player piece sits at x = 0 and the
     * strip beside it is filled with the panel tone.
     */
    public static final int WB_PANEL_WIDTH = 190;
    /**
     * {@code GuiArcaneWorkbench.field_147000_g} = {@code ySize} (same CSV, line
     * 649) — 234. The original's blit is {@code drawTexturedModalRect(x, y, 0, 0,
     * xSize, ySize)}, i.e. 190 wide by 234 tall, which covers its own hotbar row
     * at {@code y = 209}. The port derives the same figure from the original's
     * 151 px player-grid origin through {@link #panelHeight(int)}
     * ({@code 138 + 96 = 234}), so the port's panel is the original's 190x234
     * rect — asserted together in {@code ContainerLayoutTest} so a future width
     * or grid change cannot drift them apart.
     */
    public static final int WB_PANEL_HEIGHT = 234;
    /** {@code ContainerArcaneWorkbench}: the player grid at (16 + col*18, 151 + row*18). */
    public static final int WB_PLAYER_GRID_X = 16;
    public static final int WB_PLAYER_GRID_TOP = 151;
    public static final int WB_HOTBAR_TOP = 209;
    /**
     * The vanilla 176 px player piece goes at x = 0 — the original's player slots
     * at x = 16 keep vanilla's 8 px inset inside it — and the 14 px that the
     * 190 px panel has over vanilla's piece (190 − 176) is filled with the same
     * panel tone by {@code ScreenPanel.drawBackground}.
     */
    public static final int WB_PLAYER_PIECE_X = 0;
    /** The 3x3 grid: a 24 px pitch (not vanilla's 18) at (40 + col*24, 40 + row*24). */
    public static final int WB_GRID_X = 40;
    public static final int WB_GRID_Y = 40;
    public static final int WB_GRID_PITCH = 24;
    /** The result slot {@code (160,64)} and the wand slot {@code (160,24)}. */
    public static final int WB_RESULT_X = 160;
    public static final int WB_RESULT_Y = 64;
    public static final int WB_WAND_X = 160;
    public static final int WB_WAND_Y = 24;
    /**
     * The original's vis-cost display points, panel-relative, in
     * {@code Aspect.getPrimalAspects()} order — aer, terra, ignis, aqua, ordo,
     * perditio. That order is the original's own: {@code GuiArcaneWorkbench}
     * indexes {@code aspectLocs} by the position of each primal in
     * {@code Aspect.getPrimalAspects()}, which walks the static declaration order
     * of {@code thaumcraft/api/aspects/Aspect.java:18-23} (aer, terra, ignis,
     * aqua, ordo, perditio) — the order {@code aspect/AspectDefinitions} declares
     * verbatim, so index i means the same aspect on both sides. The hexagon walks
     * counter-clockwise: top, upper-left, lower-left, bottom, lower-right,
     * upper-right.
     */
    public static final int[] WB_ASPECT_CENTER_X = {72, 24, 24, 72, 120, 120};
    public static final int[] WB_ASPECT_CENTER_Y = {21, 43, 102, 124, 102, 43};
    /** {@code UtilsFX.drawTag} draws the tag 8 px up-left of its point, 16x16. */
    public static final int WB_ASPECT_BOX = 16;
    /**
     * The original's "Insufficient vis" notice:
     * {@code glTranslatef(var5 + 168, var6 + 46)} + {@code glScalef(0.5F, 0.5F, 0)}
     * + a centred draw at colour {@code 15625838} = {@code 0xEE000E}.
     */
    public static final int WB_INSUFFICIENT_X = 168;
    public static final int WB_INSUFFICIENT_Y = 46;
    public static final float WB_INSUFFICIENT_SCALE = 0.5F;
    public static final int WB_INSUFFICIENT_COLOUR = 0xFFEE000E;
    /**
     * The grey the original painted over the result slot while the wand could not
     * pay: {@code glColor4f(0.33F, 0.33F, 0.33F, 0.66F)} with
     * {@code glColorMask(false, ...)} around the item draw at
     * {@code (x + 160, y + 64)} (GuiArcaneWorkbench.java:69-92) — 0.66 alpha is
     * 0xA8. The port marks the slot with that grey; the original's greyed
     * <i>preview of the craft output</i> is not drawn, because the output item is
     * not synced to the client (BD-18).
     */
    public static final int WB_BLOCKED_TINT = 0xA8555555;
    /** The original's 3x3 anchors: {@code 40 + column*24} / {@code 40 + row*24}. */
    public static int workbenchGridX(final int column) {
        return WB_GRID_X + column * WB_GRID_PITCH;
    }
    public static int workbenchGridY(final int row) {
        return WB_GRID_Y + row * WB_GRID_PITCH;
    }
    /** The original's player anchors: {@code 16 + column*18} / {@code 151 + row*18}. */
    public static int workbenchPlayerX(final int column) {
        return WB_PLAYER_GRID_X + column * SLOT_FRAME;
    }
    public static int workbenchPlayerY(final int row) {
        return WB_PLAYER_GRID_TOP + row * SLOT_FRAME;
    }
    /** The 16x16 vis-cost box for one primal: 8 px up-left of the original's point. */
    public static int aspectBoxX(final int primalIndex) {
        return WB_ASPECT_CENTER_X[primalIndex] - WB_ASPECT_BOX / 2;
    }
    public static int aspectBoxY(final int primalIndex) {
        return WB_ASPECT_CENTER_Y[primalIndex] - WB_ASPECT_BOX / 2;
    }
    /**
     * The original's cost-tag alpha ({@code GuiArcaneWorkbench.func_146976_a}):
     * {@code 0.5F + (sin((gameTime + index*10) / 2.0F) * 0.2F - 0.2F)}, i.e. a
     * 0.1..0.5 pulse, replaced by a steady 1.0 when the wand can already pay that
     * aspect ({@code amt * 100 <= wand.getVis(...)}).
     */
    public static float aspectAlpha(final int gameTime, final int primalIndex, final boolean affordable) {
        if (affordable) {
            return 1.0F;
        }
        return 0.5F + (Mth.sin((gameTime + primalIndex * 10) / 2.0F) * 0.2F - 0.2F);
    }
    /** A fill colour for an alpha in 0..1 over an RGB triple (0xAARRGGBB). */
    public static int withAlpha(final int rgb, final float alpha) {
        final int clamped = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (clamped << 24) | (rgb & 0xFFFFFF);
    }
    /**
     * The cost the display shows for a synced centi-vis price: the original's
     * {@code cost.getAmount(primal)} is real vis and storage is centi-vis
     * ({@code wands/WandVis.CENTI_VIS_PER_VIS}, the original's {@code * 100} at
     * ItemWandCasting.java:314), so the display divides back by 100.
     */
    public static int displayVis(final int centiVis) {
        return centiVis / CENTI_VIS_PER_VIS;
    }

    /** The 1.7.10 {@code GuiContainer} default panel the original never overrode. */
    public static final int TAU_PANEL_HEIGHT = 166;
    /** {@code ContainerThaumatorium}: the catalyst slot at {@code (48,16)}. */
    public static final int TAU_CATALYST_X = 48;
    public static final int TAU_CATALYST_Y = 16;
    /** The original player grid at {@code (8,84)} and hotbar at {@code (8,142)}. */
    public static final int TAU_PLAYER_X = 8;
    /**
     * The recipe output box: {@code drawOutput} renders the item at
     * {@code (x+112, y+16)}, its tooltip hitbox is that same 16x16, and
     * {@code func_73864_a} queues the recipe from it.
     */
    public static final int TAU_OUTPUT_X = 112;
    public static final int TAU_OUTPUT_Y = 16;
    public static final int TAU_OUTPUT_SIZE = 16;
    /** The prev/next page arrows: 16x8 at {@code (128,16)} and {@code (128,24)}. */
    public static final int TAU_PAGE_X = 128;
    public static final int TAU_PAGE_PREV_Y = 16;
    public static final int TAU_PAGE_NEXT_Y = 24;
    public static final int TAU_PAGE_W = 16;
    public static final int TAU_PAGE_H = 8;
    /**
     * The aspect-window pager, drawn and clicked only while the current recipe
     * has more than {@link #TAU_VISIBLE_ASPECTS} aspects: 8x16 boxes at
     * {@code (32,40)} and {@code (136,40)}.
     */
    public static final int TAU_ASPECT_LEFT_X = 32;
    public static final int TAU_ASPECT_RIGHT_X = 136;
    public static final int TAU_ASPECT_PAGER_Y = 40;
    public static final int TAU_ASPECT_PAGER_W = 8;
    public static final int TAU_ASPECT_PAGER_H = 16;
    /**
     * The 48x48 knowledge-highlight overlay at {@code (104,8)}, drawn when the
     * queue is non-empty and the output is hovered or the current recipe is
     * queued; and the 24x24 white box at {@code (88,16)} that pulses whenever
     * the queue is non-empty ({@code GuiThaumatorium.java:102-120}).
     */
    public static final int TAU_HIGHLIGHT_X = 104;
    public static final int TAU_HIGHLIGHT_Y = 8;
    public static final int TAU_HIGHLIGHT_SIZE = 48;
    public static final int TAU_PULSE_X = 88;
    public static final int TAU_PULSE_Y = 16;
    public static final int TAU_PULSE_SIZE = 24;
    /**
     * The aspect row: a 16x16 {@code UtilsFX.drawTag} per aspect at
     * {@code (40 + 16*pos, 40)}, and — only while the current recipe is queued —
     * the essentia bar behind it: a 14x6 background at {@code (41 + 16*pos, 57)}
     * with an {@code i1}x4 coloured fill at {@code (42 + 16*pos, 58)},
     * {@code i1 = essentia.getAmount / recipe amount * 12.0F}. Six aspects are
     * visible per page.
     */
    public static final int TAU_TAG_X = 40;
    public static final int TAU_TAG_Y = 40;
    public static final int TAU_TAG_PITCH = 16;
    public static final int TAU_TAG_SIZE = 16;
    public static final int TAU_BAR_X = 41;
    public static final int TAU_BAR_Y = 57;
    public static final int TAU_BAR_W = 14;
    public static final int TAU_BAR_H = 6;
    public static final int TAU_BAR_FILL_X = 42;
    public static final int TAU_BAR_FILL_Y = 58;
    public static final int TAU_BAR_FILL_H = 4;
    public static final int TAU_BAR_FILL_SCALE = 12;
    /**
     * The bar fill <b>cell</b> in {@code gui_thaumatorium.png}: measured on the
     * shipped sheet, the alpha-opaque run at {@code (176,0)} is exactly
     * <b>12x4</b> (and the background cell at {@code (176,8)} is exactly 14x6),
     * so the fill scale is the cell's own width.
     */
    public static final int TAU_BAR_FILL_CELL = TAU_BAR_FILL_SCALE;
    public static final int TAU_VISIBLE_ASPECTS = 6;
    /**
     * The queue counter: {@code recipeHash.size() + "/" + maxRecipes} translated
     * to {@code (136,33)}, scaled 0.5, centred, white — drawn only when
     * {@code maxRecipes > 1} ({@code GuiThaumatorium.java:124-133}).
     */
    public static final int TAU_COUNT_X = 136;
    public static final int TAU_COUNT_Y = 33;
    public static final float TAU_COUNT_SCALE = 0.5F;
    public static final int TAU_COUNT_COLOUR = 0xFFFFFFFF;
    /**
     * The window's own sheet. Every element below is a rectangle inside it, so
     * one probe of this path decides the whole window: with the asset overlay
     * applied the original texels are drawn, and in the code-only jar this
     * build is gated on the screen keeps its flat stand-ins (D-004 — the
     * original bytes are All Rights Reserved, never committed).
     *
     * <p>The path is not a guess: {@code docs/asset-mapping.csv} row 487 maps
     * {@code textures/gui/gui_thaumatorium.png} to <b>itself</b> as
     * {@code ARR-overlay}, and {@code OriginalTextureOverlayTest.KEPT_DIRS}
     * lists {@code gui} among the directories the overlay copies unrenamed — so
     * the runtime path the probe asks for is exactly the path the overlay
     * installs. The reference copy measures 256x256, the size every original
     * GUI sheet is (class javadoc), and the gate reads the edge off the PNG
     * rather than trusting that.</p>
     */
    public static final ResourceLocation TAU_SHEET =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_thaumatorium.png");
    /**
     * The panel rect inside that sheet:
     * {@code drawTexturedModalRect(k, l, 0, 0, xSize, ySize)}
     * ({@code GuiThaumatorium.java:57}) — the sheet's top-left 176x166, i.e. the
     * same size the panel is, so the sheet is blitted one-for-one with no UV
     * offset.
     */
    public static final int TAU_SHEET_U = 0;
    public static final int TAU_SHEET_V = 0;
    public static final int TAU_SHEET_W = PANEL_WIDTH;
    public static final int TAU_SHEET_H = TAU_PANEL_HEIGHT;
    /**
     * The prev/next arrows' sheet cells: {@code (176,16)} disabled and
     * {@code (192,16)} enabled for prev, {@code (176,24)} / {@code (192,24)} for
     * next, each 16x8 ({@code GuiThaumatorium.java:73-84}). The enabled cell is
     * 16 texels right of the disabled one, exactly as the aspect pager's pair is
     * ({@code 184 -> 200}), so one shift constant covers all four buttons.
     */
    public static final int TAU_UV_ARROW_DISABLED = 176;
    public static final int TAU_UV_PREV_V = 16;
    public static final int TAU_UV_NEXT_V = 24;
    public static final int TAU_UV_ENABLED_SHIFT = 16;
    /**
     * The aspect pager's cells at sheet {@code v = 32}, 8x16: left
     * {@code 176} disabled / {@code 192} enabled, right {@code 184} disabled /
     * {@code 200} enabled ({@code GuiThaumatorium.java:86-97}).
     */
    public static final int TAU_UV_ASPECT_LEFT_DISABLED = 176;
    public static final int TAU_UV_ASPECT_RIGHT_DISABLED = 184;
    public static final int TAU_UV_ASPECT_PAGER_V = 32;
    /**
     * The essentia bar's cells: the 14x6 background at {@code (176,8)} and the
     * coloured fill strip at {@code (176,0)} — the fill is blitted
     * {@code i1} texels wide, 4 tall, in the aspect's own colour
     * ({@code GuiThaumatorium.java:147-151}).
     */
    public static final int TAU_UV_BAR_BACKGROUND_U = 176;
    public static final int TAU_UV_BAR_BACKGROUND_V = 8;
    public static final int TAU_UV_BAR_FILL_U = 176;
    public static final int TAU_UV_BAR_FILL_V = 0;
    /** The 24x24 queue box cell {@code (176,56)} and the 48x48 highlight {@code (176,96)}. */
    public static final int TAU_UV_PULSE_U = 176;
    public static final int TAU_UV_PULSE_V = 56;
    public static final int TAU_UV_HIGHLIGHT_U = 176;
    public static final int TAU_UV_HIGHLIGHT_V = 96;
    /**
     * The alpha this window's tags draw at: <b>1.0, with no pulse</b>. The card
     * that opened this work asked for the workbench's pulse
     * ({@code 0.5 + sin((ticks + i*10)/2)*0.2 - 0.2}, pinned to 1.0 when the
     * buffer covers the price), but that formula is
     * {@code GuiArcaneWorkbench.java:57}, and
     * {@code GuiThaumatorium.java:167} calls the SIX-argument
     * {@code UtilsFX.drawTag(x, y, aspect, amount, bonus, z)} — the overload at
     * {@code UtilsFX.java:802-804} that forwards {@code blend = 771, alpha =
     * 1.0F, bw = false}. The thaumatorium's own two pulses are the 24x24 queue
     * box ({@link #thaumatoriumQueuePulse}) and the grey output tint
     * ({@link #thaumatoriumDullPulse}); its tags never pulsed, so the port does
     * not pulse them (BD-19 records the measured call).
     */
    public static final float TAU_TAG_ALPHA = 1.0F;
    /** The prev/next arrow's sheet u for this enabled state. */
    public static int thaumatoriumArrowU(final boolean enabled) {
        return enabled ? TAU_UV_ARROW_DISABLED + TAU_UV_ENABLED_SHIFT : TAU_UV_ARROW_DISABLED;
    }
    /**
     * The aspect pager's sheet u. {@code side} is the original's own order: 0 =
     * the left box at {@code (32,40)}, 1 = the right box at {@code (136,40)} —
     * the two cells are 8 texels apart while the arrows' pair is 16 apart, so
     * the pair is stated per side, not derived from one base.
     */
    public static int thaumatoriumAspectPagerU(final int side, final boolean enabled) {
        final int disabled = side == 0 ? TAU_UV_ASPECT_LEFT_DISABLED : TAU_UV_ASPECT_RIGHT_DISABLED;
        return enabled ? disabled + TAU_UV_ENABLED_SHIFT : disabled;
    }
    /**
     * The white pulse of the 24x24 queue box:
     * {@code 0.6F + sin(gameTime / 5.0F) * 0.4F + 0.4F} (the client player's
     * {@code ticksExisted}; the port's screens run their pulse clock the same way
     * {@code ArcaneWorkbenchScreen} does).
     */
    public static float thaumatoriumQueuePulse(final int gameTime) {
        return 0.6F + Mth.sin(gameTime / 5.0F) * 0.4F + 0.4F;
    }
    /**
     * The grey tint pulse over the queueable output:
     * {@code 0.3F + sin(gameTime / 4.0F) * 0.3F + 0.3F} under
     * {@code glColor4f(0.5F, 0.5F, 0.5F, alpha)} ({@code GuiThaumatorium.java:182-183}).
     */
    public static float thaumatoriumDullPulse(final int gameTime) {
        return 0.3F + Mth.sin(gameTime / 4.0F) * 0.3F + 0.3F;
    }
    /** The grey of the original's dull tint — {@code glColor4f(0.5F, 0.5F, 0.5F, a)}. */
    public static final int TAU_DULL_GREY = 0x808080;
    /**
     * The original's bar fill width:
     * {@code (int)((float)essentia.getAmount(aspect) / recipeAmount * 12.0F)} —
     * verbatim, uncapped: when the tile holds more than the recipe needs the
     * original's fill runs past its 14 px background, and the port reproduces
     * that rather than tidying it.
     */
    /**
     * The fill width <b>as it is drawn</b>: the original's uncapped arithmetic,
     * saturated at the 12-texel fill cell. The 1.7.10 blit
     * ({@code GuiThaumatorium.java:150-152}) has no clamp and no zero guard, so
     * an overfull bar (5 of a needed 4 → 15) sampled the sheet's pixels
     * <i>right of</i> the fill cell — a 1.7.10 {@code drawTexturedModalRect}
     * reads raw texture coordinates. A 1.21.1 {@code GuiGraphics#blit} samples a
     * sprite's region, so asking for 15 texels from a 12-texel cell is not the
     * same read and a 0-wide blit is degenerate; the port saturates at the cell
     * and skips the empty bar. The arithmetic itself stays uncapped and pinned
     * by {@link #thaumatoriumBarFill} (BD-18's render-state list).
     */
    public static int thaumatoriumBarFillDrawn(final int have, final int need) {
        return Math.min(thaumatoriumBarFill(have, need), TAU_BAR_FILL_CELL);
    }
    /**
     * The fill width <b>as the source computes it</b>, uncapped:
     * {@code (int)(have / need * 12.0F)} — 5 of a needed 4 is 15.
     */
    public static int thaumatoriumBarFill(final int have, final int need) {
        if (need <= 0) {
            return 0;
        }
        return (int) ((float) have / (float) need * (float) TAU_BAR_FILL_SCALE);
    }
    /** The tag/bar slot for one visible aspect column. */
    public static int thaumatoriumTagX(final int position) {
        return TAU_TAG_X + position * TAU_TAG_PITCH;
    }
    public static int thaumatoriumBarX(final int position) {
        return TAU_BAR_X + position * TAU_TAG_PITCH;
    }
    public static int thaumatoriumBarFillX(final int position) {
        return TAU_BAR_FILL_X + position * TAU_TAG_PITCH;
    }
    /** The same price as the {@code float} the original's tag glyph printed. */
    public static float displayVisFloat(final int centiVis) {
        return centiVis / (float) CENTI_VIS_PER_VIS;
    }
    /**
     * {@code UtilsFX.drawTag} blits the tag as one quad from
     * {@code (x, y)} to {@code (x + 16, y + 16)} with UV {@code 0..1}
     * (UtilsFX.java:839-842) — a 16 px box, always.
     */
    public static final int TAG_SIZE = 16;
    /**
     * The original's GUIs pass the point the tag sits ON and shift the box half
     * its size up-left — {@code GuiArcaneWorkbench.java:65} calls
     * {@code drawTag(var5 + aspectLocs[count][0] - 8, var6 + aspectLocs[count][1] - 8, ...)}
     * — so the box top-left is the point minus 8.
     */
    public static final int TAG_CENTER_OFFSET = -8;
    /**
     * The aspect images are square and the original's quad maps the WHOLE texture
     * onto the 16 px box (UV {@code 0..1}, not a 16-texel window): the shipped
     * art is 32x32 ({@code reference/assets/thaumcraft/textures/aspects/ignis.png},
     * measured), so the renderer reads the real edge from the bytes
     * (the original's own {@code getTextureSize} reads the
     * {@code BufferedImage} width the same way, UtilsFX.java:739-758) and falls
     * back to 16 when there are no bytes to read.
     */
    public static final int TAG_TEXTURE_FALLBACK = 16;
    /** The original's amount glyph is drawn inside a {@code glScalef(0.5F, ...)} (UtilsFX.java:847). */
    public static final float TAG_AMOUNT_SCALE = 0.5F;
    /**
     * In the original's 0.5-scaled space the tag is 32 wide, which is the
     * {@code 32} in both of its glyph literals
     * ({@code 32 - sw + (int)x * 2} / {@code 32 - font.FONT_HEIGHT + (int)y * 2},
     * UtilsFX.java:861).
     */
    public static final int TAG_AMOUNT_ORIGIN = 2 * TAG_SIZE;
    /** The original's glyph colour is {@code 16777215} = white (UtilsFX.java:861). */
    public static final int TAG_AMOUNT_COLOUR = 0xFFFFFFFF;
    /**
     * The {@code bw} (black-and-white) tag — the 3-argument overload's variant,
     * {@code drawTag(x, y, aspect)} (UtilsFX.java:806-808), used by the in-world
     * tile renderers for a jar's / alembic's filter aspect
     * ({@code TileAlembicRenderer.java}, {@code TileJarRenderer.java}) — tints the
     * sprite {@code (0.1F, 0.1F, 0.1F)} at {@code alpha * 0.8F}
     * (UtilsFX.java:828, 836). The GUI tags, including the deconstruction table's,
     * are the NON-{@code bw} form: the aspect's own colour.
     */
    public static final float TAG_BW_TONE = 0.1F;
    /** The {@code bw} alpha factor (UtilsFX.java:828). */
    public static final float TAG_BW_ALPHA = 0.8F;
    /**
     * The bonus sparkle: a 16x16 quad of the particle sheet 4 px up-left of the
     * tag box, frame {@code 16 * (player.ticksExisted % 16)} at sheet v = 80
     * (UtilsFX.java:867-870 → {@code drawTexturedQuad(x - 4, y - 4, px, 80, 16, 16)},
     * whose UVs are {@code texel * 0.00390625F} = a 256-wide sheet).
     */
    public static final int TAG_SPARKLE_OFFSET = -4;
    public static final int TAG_SPARKLE_FRAMES = 16;
    public static final int TAG_SPARKLE_V = 80;
    public static final int TAG_SPARKLE_SHEET = 256;
    /** The sheet the bonus sparkle is cut from — {@code ParticleEngine.particleTexture}. */
    public static final ResourceLocation PARTICLE_SHEET =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    /** The tag box for a point the original passed to {@code drawTag}. */
    public static int tagBoxX(final int pointX) {
        return pointX + TAG_CENTER_OFFSET;
    }
    public static int tagBoxY(final int pointY) {
        return pointY + TAG_CENTER_OFFSET;
    }
    /**
     * The amount string the original's glyph printed:
     * {@code new DecimalFormat("#######.##").format(amount)}
     * ({@code UtilsFX.myFormatter}, UtilsFX.java:311 and 849) — so 200 centi-vis
     * of aer prints as {@code "2"} and 250 as {@code "2.5"}.
     */
    public static String tagAmountText(final float amount) {
        return TAG_AMOUNT_FORMAT.format(amount);
    }
    private static final java.text.DecimalFormat TAG_AMOUNT_FORMAT =
            new java.text.DecimalFormat("#######.##");
    /**
     * The glyph's x in the 0.5-scaled space: the original's
     * {@code 32 - sw + (int)x * 2} — right-aligned on the tag's right edge,
     * i.e. on screen at {@code boxX + 16 - width/2}.
     */
    public static int tagAmountScaledX(final int boxX, final int textWidth) {
        return TAG_AMOUNT_ORIGIN - textWidth + boxX * 2;
    }
    /**
     * The glyph's y in the 0.5-scaled space: the original's
     * {@code 32 - font.FONT_HEIGHT + (int)y * 2} — the font's own height, which
     * is 8 in 1.7.10 and 9 in 1.21.1, so the port passes the live font and the
     * glyph sits half a pixel lower on screen.
     */
    public static int tagAmountScaledY(final int boxY, final int fontHeight) {
        return TAG_AMOUNT_ORIGIN - fontHeight + boxY * 2;
    }
    /** The bonus sparkle's frame index for an age in client ticks (UtilsFX.java:869). */
    public static int tagSparkleFrame(final int ageTicks) {
        return TAG_SPARKLE_FRAMES * Math.floorMod(ageTicks, TAG_SPARKLE_FRAMES);
    }
    /**
     * The tag's size in the 0.5-scaled space — the original's local
     * {@code bx = 16 * 0.5F} (UtilsFX.java:845), which is 8.
     */
    public static final int TAG_AMOUNT_BOX = (int) (TAG_SIZE * TAG_AMOUNT_SCALE);
    /**
     * The bonus count glyph's literals, verbatim from UtilsFX.java:875-884:
     * {@code 8 - font.getStringWidth(text) / 2 + (int)a * 2} for x and
     * {@code 15 - font.FONT_HEIGHT + (int)y * 2} for y — centred on the tag's left
     * edge, near its top. (The {@code bx} in that method's outline loop is the
     * decompiler's shadowed ±1 loop variable, not the box size; only the outline
     * pass adds it.)
     */
    public static final int TAG_BONUS_TEXT_X_ORIGIN = 8;
    public static final int TAG_BONUS_TEXT_Y_ORIGIN = 15;
    public static int tagBonusScaledX(final int boxX, final int textWidth) {
        return TAG_BONUS_TEXT_X_ORIGIN - textWidth / 2 + boxX * 2;
    }
    public static int tagBonusScaledY(final int boxY, final int fontHeight) {
        return TAG_BONUS_TEXT_Y_ORIGIN - fontHeight + boxY * 2;
    }
    /** The alpha the tag is drawn at: the caller's, cut to 0.8 in {@code bw} mode. */
    public static float tagAlphaOf(final float alpha, final boolean bw) {
        return bw ? alpha * TAG_BW_ALPHA : alpha;
    }
    /**
     * The tag's flat tint — the same colour arithmetic the original put into
     * {@code glColor4f} (UtilsFX.java:826-836) and what the code-only build
     * fills its 16 px box with, since the sprite is not in the jar (D-004).
     */
    public static int tagTint(final int rgb, final float alpha, final boolean bw) {
        if (bw) {
            final int tone = Math.round(TAG_BW_TONE * 255.0F);
            return withAlpha((tone << 16) | (tone << 8) | tone, tagAlphaOf(alpha, true));
        }
        return withAlpha(rgb, alpha);
    }

    /**
     * {@code GuiResearchTable} sets {@code field_146999_f = 255} (xSize) and
     * {@code field_147000_g = 255} (ySize) in its constructor, and its background
     * is a 255x167 top piece at (0,0) plus an 184x88 player piece at (40,167) —
     * so the original's window is a <b>255 x 255</b> rect. The port derives its
     * height from the original's 175 px player-grid origin through
     * {@link #panelHeight(int)} and lands on 162 + 96 = <b>258</b>: vanilla's
     * player piece is 96 px tall, the original's own sheet piece 88 px, so the
     * port's panel is the original's 255 px wide and 3 px taller. Both halves are
     * asserted in {@code ContainerLayoutTest}, and BD-19 names the 3 px.
     */
    public static final int RT_PANEL_WIDTH = 255;
    public static final int RT_PANEL_HEIGHT = 255;
    /**
     * The 184x88 player piece sat at x = 40 in the original; vanilla's 176 px
     * {@code generic_54.png} piece goes at the same x, because the original's
     * player slots start at x = 48 — vanilla's 8 px inset inside the piece.
     */
    public static final int RT_PLAYER_PIECE_X = 40;
    /** {@code ContainerResearchTable}: player grid (48 + col*18, 175 + row*18), hotbar (48 + i*18, 233). */
    public static final int RT_PLAYER_GRID_X = 48;
    public static final int RT_PLAYER_GRID_TOP = 175;
    public static final int RT_HOTBAR_TOP = 233;
    /** The two machine slots: scribing tools at (14,10), research notes at (70,10). */
    public static final int RT_INK_X = 14;
    public static final int RT_INK_Y = 10;
    public static final int RT_NOTES_X = 70;
    public static final int RT_NOTES_Y = 10;
    /**
     * The discovered-aspect grid: origin {@code (10,40)}, 16 px cells, five per
     * row ({@code xx = drawn / 5 * 16}, {@code yy = drawn % 5 * 16}) and 25 drawn
     * per page — an 80x80 area. The page moves the start index by
     * {@code page * 5}, not by the page size, so pages overlap by twenty entries;
     * that is the original's own arithmetic ({@code ++count - 1 >= page * 5 &&
     * drawn < 25}), reproduced term for term.
     */
    public static final int RT_ASPECT_X = 10;
    public static final int RT_ASPECT_Y = 40;
    public static final int RT_ASPECT_CELL = 16;
    public static final int RT_ASPECT_COLS = 5;
    public static final int RT_PAGE_STRIDE = 5;
    public static final int RT_ASPECTS_PER_PAGE = 25;
    public static final int RT_ASPECT_AREA = 80;
    /**
     * The page rails: a 24x8 arrow at {@code (27,121)} steps back (only drawn and
     * clickable while {@code page > 0}) and one at {@code (51,121)} steps forward
     * (only while {@code page < lastPage}, {@code lastPage = (count - 20) / 5}).
     */
    public static final int RT_PAGE_Y = 121;
    public static final int RT_PAGE_PREV_X = 27;
    public static final int RT_PAGE_NEXT_X = 51;
    public static final int RT_PAGE_WIDTH = 24;
    public static final int RT_PAGE_HEIGHT = 8;
    /**
     * The combine button: a 32x16 plate at {@code (35,139)}, drawn only while both
     * selects are set, and the same rect is its hitbox. The original armed a
     * 200&nbsp;ms cooldown on press ({@code butcount2 = nanoTime() + 200000000L})
     * and drew a second, greyed plate over it while the cooldown ran.
     */
    public static final int RT_COMBINE_X = 35;
    public static final int RT_COMBINE_Y = 139;
    public static final int RT_COMBINE_WIDTH = 32;
    public static final int RT_COMBINE_HEIGHT = 16;
    public static final long RT_BUTTON_COOLDOWN_NANOS = 200_000_000L;
    /**
     * The two select wells, drawn as 16x16 tags at {@code (13,139)} and
     * {@code (71,139)} — that is {@code (x + 3, y + 99)} and {@code (x + 61,
     * y + 99)} from the aspect grid origin. Clicking a filled well clears it; the
     * original's clear hitboxes are {@code (11,137)} and {@code (71,137)}, two
     * pixels up-left of the first and flush with the second, and they are kept
     * exactly as the original numbers them.
     */
    public static final int RT_SELECT1_X = 13;
    public static final int RT_SELECT2_X = 71;
    public static final int RT_SELECT_Y = 139;
    public static final int RT_SELECT_SIZE = 16;
    public static final int RT_CLEAR1_X = 11;
    public static final int RT_CLEAR2_X = 71;
    public static final int RT_CLEAR_Y = 137;
    /** Both clear hitboxes are 16x16 (GuiResearchTable.java:654-671: {@code < 16}). */
    public static final int RT_CLEAR_SIZE = 16;
    /**
     * The drop targets of the original's aspect drag: a 32x32 box centred on
     * {@code (20,146)} for the first well and {@code (79,146)} for the second
     * ({@code mouseX = mx - (gx + 20); ... >= -16 && < 16}).
     */
    public static final int RT_DROP1_X = 20;
    public static final int RT_DROP2_X = 79;
    public static final int RT_DROP_Y = 146;
    public static final int RT_DROP_RADIUS = 16;
    /**
     * The duplicate (camera) plate: 24x24 at {@code (37,5)}, drawn and clickable
     * only while {@code RESEARCHDUPE} is researched, a note is in the notes slot
     * and that note is complete; pressing it sends menu button 5.
     */
    public static final int RT_DUPE_X = 37;
    public static final int RT_DUPE_Y = 5;
    public static final int RT_DUPE_SIZE = 24;
    /**
     * The out-of-ink notice: the original centred its two-line tooltip on
     * {@code gx + 157 - max(line0, line1)/2, gy + 84} whenever the scribing slot
     * was empty or its tool was fully used ({@code GuiResearchTable.java:213-228}).
     */
    public static final int RT_NOINK_X = 157;
    public static final int RT_NOINK_Y = 84;
    /** The alpha the original gave a faded tag: {@code faded ? 0.33F : 1.0F}. */
    public static final float RT_FADED_ALPHA = 0.33F;
    /**
     * Authored stand-ins for art the original took from
     * {@code gui/guiresearchtable2.png}, which ships only in the asset-overlay
     * build (D-004): the tone of its plates (the combine button, the two page
     * rails, the camera plate and the two clear boxes) and the dark wash it drew
     * over the combine button during its 200&nbsp;ms cooldown. No pixel of the
     * original is claimed; the original's own shadowed text is white
     * ({@code drawStringWithShadow(..., 0xFFFFFF)}) with its two notice lines ten
     * pixels apart at {@code y = 84} and {@code y = 94}.
     */
    public static final int RT_PLATE_FILL = 0xFF9C7A52;
    public static final int RT_PLATE_COOLDOWN_TINT = 0xA8404040;
    public static final int RT_NOTICE_COLOUR = 0xFFFFFFFF;
    public static final int RT_NOTICE_LINE_GAP = 10;
    /** Cell x of the original's aspect walk: {@code (drawn / 5) * 16} from {@code (10,40)}. */
    public static int researchTableGridX(final int drawn) {
        return RT_ASPECT_X + drawn / RT_ASPECT_COLS * RT_ASPECT_CELL;
    }
    /** Cell y of the original's aspect walk: {@code (drawn % 5) * 16} from {@code (10,40)}. */
    public static int researchTableGridY(final int drawn) {
        return RT_ASPECT_Y + drawn % RT_ASPECT_COLS * RT_ASPECT_CELL;
    }
    /** The entry the walk reads on page {@code page} at position {@code drawn}. */
    public static int researchTableIndexAt(final int drawn, final int page) {
        return page * RT_PAGE_STRIDE + drawn;
    }
    /**
     * The last page index for a discovered-aspect count, verbatim from
     * {@code GuiResearchTable.java:739}: {@code (discoveredAspects.size() - 20) / 5}.
     * The forward arrow is drawn and clickable only while {@code page < lastPage},
     * so a 49-aspect pool gives {@code (49 - 20) / 5 = 5} — pages 0..5.
     */
    public static int researchTableLastPage(final int discoveredCount) {
        return (discoveredCount - 20) / RT_PAGE_STRIDE;
    }
    /** Player-grid slot x: the original's {@code 48 + col * 18}, unclamped. */
    public static int researchTablePlayerX(final int col) {
        return RT_PLAYER_GRID_X + SLOT_FRAME * col;
    }
    /** Player-grid slot y: the original's {@code 175 + row * 18}, unclamped. */
    public static int researchTablePlayerY(final int row) {
        return RT_PLAYER_GRID_TOP + SLOT_FRAME * row;
    }
    /** Hotbar slot x: the original's {@code 48 + i * 18}, at {@code y = 233}. */
    public static int researchTableHotbarX(final int index) {
        return RT_PLAYER_GRID_X + SLOT_FRAME * index;
    }
    /**
     * The x the original centred the out-of-ink notice on: {@code 157 - width/2},
     * {@code width} being the wider of its two lines.
     */
    public static int researchTableNoInkX(final int widestLine) {
        return RT_NOINK_X - widestLine / 2;
    }
    /** The original's click test: {@code mx >= x && mx < x+w && my >= y && my < y+h}. */
    public static boolean inRect(final double mx, final double my, final int x, final int y,
                                 final int width, final int height) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }
    /**
     * The original's aspect-drag target: a 32x32 box centred on {@code (cx,cy)} —
     * each axis {@code d >= -16 && d < 16}.
     */
    public static boolean inDragTarget(final double mx, final double my, final int cx, final int cy) {
        final double dx = mx - cx;
        final double dy = my - cy;
        return dx >= -RT_DROP_RADIUS && dx < RT_DROP_RADIUS
                && dy >= -RT_DROP_RADIUS && dy < RT_DROP_RADIUS;
    }

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
