package com.jonnyutah12.thaumcraftclassic.recipes;

/**
 * Where a crafting row goes on a Thaumonomicon page (TC-M3-04-s-iv-b), in the
 * original's own geometry.
 *
 * <p>Every constant is measured from the original's recipe-page renderer,
 * {@code thaumcraft.client.gui.GuiResearchRecipe#drawCraftingPage}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchRecipe.java:755-946},
 * reached from the page-kind dispatch at {@code 263-311}, crafting case at 296):
 * the crafting backdrop is a 52x52 texture region blitted at scale 2 from the
 * page's origin (lines 783-788 → a 104x104 box), its result frame a 16x16
 * region at scale 2 from {@code (20, 12)} of that box (line 789 → 32x32 at
 * {@code (40, 24)}), and the result item itself at {@code (x + 48, y + 32)}
 * (lines 798-799) with a 16x16 tooltip box (line 807). A shaped grid's cell
 * {@code (col, row)} is drawn at {@code (x + 16 + col * 32, y + 76 + row * 32)},
 * both axes capped at 3 and indexed {@code col + row * width} (lines 837-864);
 * a shapeless grid walks the same box with {@code i % 3} / {@code i / 3} over at
 * most 9 cells (lines 898-923). The station label is centred on {@code x + 56}
 * (lines 821-823 shaped, 887-890 shapeless). The items themselves are 16x16.</p>
 *
 * <p><b>The port's deltas (BD-17, D-004).</b> The original drew that backdrop
 * and its 32x32 result frame from {@code gui_researchbook_overlay.png}, an All
 * Rights Reserved file this repo never commits: the frame is a code-drawn box
 * and every cell is framed by the vanilla {@code minecraft:container/slot}
 * sprite (18x18, the same sprite the book already uses for its page panel).
 * The book panel is smaller than the original's 256x181 pane, so the block is
 * scaled to fit ({@link #scaleFor}); the geometry scales, the 16x16 item sprites
 * stay at native size — a scaled item would need a pose transform, and the
 * geometry is what the fit is about.</p>
 *
 * @param originX screen x of the block's local (0, 0)
 * @param originY screen y of the block's local (0, 0)
 * @param scale  the fit factor applied to the original's offsets
 */
public record RecipePageLayout(int originX, int originY, float scale) {

    /** The 52x52 backdrop at scale 2 — the block's width (GuiResearchRecipe.java:783-788). */
    public static final int BLOCK_WIDTH = 104;
    /** The block's height: grid top 76 + two 32 px pitches + a 16 px item row. */
    public static final int BLOCK_HEIGHT = 156;

    /** Grid origin and pitch, and the item box (GuiResearchRecipe.java:837-864, 898-923). */
    public static final int GRID_X = 16;
    public static final int GRID_Y = 76;
    public static final int PITCH = 32;
    public static final int ITEM = 16;
    /** The result item's box (GuiResearchRecipe.java:798-799), framed by a 32x32
     * result plate at {@code (40, 24)} (line 789). */
    public static final int RESULT_X = 48;
    public static final int RESULT_Y = 32;
    /** The station label's centre (GuiResearchRecipe.java:821-823, 887-890). */
    public static final int HEADER_CENTRE_X = 56;
    /** The vanilla slot sprite's natural size, 1 px larger than the item it frames. */
    public static final int SLOT = 18;
    /** The original draws at most a 3x3 grid on a page ({@code i < 3} caps on a
     * shaped page's axes, 837-838, and {@code i < 9} cells on a shapeless one, 898). */
    public static final int MAX_GRID = 3;
    /** A block this small is unreadable, so it is not scaled below this. */
    public static final float MIN_SCALE = 0.5F;

    /**
     * The grid box a row draws into, in the original's page units: a shaped row
     * its own pattern, a shapeless row {@code min(3, n)} columns by
     * {@code ceil(n / 3)} rows ({@link RecipePageDisplay#shapelessShape}).
     */
    public record Shape(int width, int height) {

        /** Number of cells the box holds, row-major. */
        public int cells() {
            return width * height;
        }
    }

    /** The largest scale at which the block fits the box, never above 1. */
    public static float scaleFor(final int availableWidth, final int availableHeight) {
        if (availableWidth <= 0 || availableHeight <= 0) {
            return MIN_SCALE;
        }
        final float fit = Math.min(availableWidth / (float) BLOCK_WIDTH,
                availableHeight / (float) BLOCK_HEIGHT);
        return Math.max(MIN_SCALE, Math.min(1.0F, fit));
    }

    /**
     * Place the block in the box starting at {@code (left, top)}: the original
     * draws it flush left, and the book's page pane is wider than the block, so
     * the caller centres it by passing the centred {@code left}.
     */
    public static RecipePageLayout fit(final int left, final int top,
                                       final int availableWidth, final int availableHeight) {
        return new RecipePageLayout(left, top, scaleFor(availableWidth, availableHeight));
    }

    /** Screen x of a local x offset. */
    public int x(final int localX) {
        return originX + Math.round(localX * scale);
    }

    /** Screen y of a local y offset. */
    public int y(final int localY) {
        return originY + Math.round(localY * scale);
    }

    /** A scaled local size. */
    public int size(final int localSize) {
        return Math.round(localSize * scale);
    }

    /** Top-left of the item drawn in grid column {@code col}. */
    public int cellX(final int col) {
        return x(GRID_X + col * PITCH);
    }

    /** Top-left of the item drawn in grid row {@code row}. */
    public int cellY(final int row) {
        return y(GRID_Y + row * PITCH);
    }

    /** Top-left of the vanilla slot square framing grid column {@code col}. */
    public int slotX(final int col) {
        return cellX(col) - Math.round(scale);
    }

    /** Top-left of the vanilla slot square framing grid row {@code row}. */
    public int slotY(final int row) {
        return cellY(row) - Math.round(scale);
    }

    /** Top-left of the result item. */
    public int resultX() {
        return x(RESULT_X);
    }

    /** Top-left of the result item. */
    public int resultY() {
        return y(RESULT_Y);
    }

    /** Top-left of the vanilla slot square framing the result item. */
    public int resultSlotX() {
        return resultX() - Math.round(scale);
    }

    /** Top-left of the vanilla slot square framing the result item. */
    public int resultSlotY() {
        return resultY() - Math.round(scale);
    }

    /** Top-left of a station label centred on the original's centre line. */
    public int headerX(final int textWidth) {
        return x(HEADER_CENTRE_X) - textWidth / 2;
    }

    /** The scaled size of every slot square the block draws. */
    public int slotSize() {
        return size(SLOT);
    }
}
