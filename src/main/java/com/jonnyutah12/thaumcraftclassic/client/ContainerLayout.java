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

    /**
     * Centi-vis per vis — the same unit {@code wands/WandVis.CENTI_VIS_PER_VIS}
     * uses (the original's {@code * 100}, ItemWandCasting.java:314). Restated here
     * so this class keeps no dependency outside vanilla: it is the one unit the
     * workbench's synced cost slots travel in.
     */
    public static final int CENTI_VIS_PER_VIS = 100;

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

    // -------------------------------------------------- arcane workbench

    // Every figure below is transcribed from
    // reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiArcaneWorkbench.java
    // and .../common/container/ContainerArcaneWorkbench.java. The menu uses these
    // same constants for its slot anchors, so screen and menu cannot drift apart.

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

    // ------------------------------------------- arcane workbench geometry

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

    private ContainerLayout() {
    }
}
