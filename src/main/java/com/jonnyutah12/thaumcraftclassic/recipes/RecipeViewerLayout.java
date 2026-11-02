package com.jonnyutah12.thaumcraftclassic.recipes;

/**
 * Panel geometry for the recipe viewers (SPINE-4, t_aebd14cf) — kept OUTSIDE
 * the browser plugin package on purpose.
 *
 * <p>Two consumers: the JEI categories size their panels from these constants,
 * and {@code JeiRecipeCategoryGameTest} asserts every live shipped row fits
 * them. A game test may not touch a {@code mezz.jei} type (the no-JEI run has
 * no such class to load — guarded by JeiPluginIsolationTest), so the capacity
 * the layout promises has to be readable by core code. The probe measures the
 * real data, so a future row that outgrows a panel turns the game test red
 * instead of silently clipping slots.</p>
 *
 * <p>The capacities carry headroom over the current data (measured on this
 * branch: arcane pattern rows ≤ 3 → 9 grid cells; infusion components ≤ 9;
 * infusion_enchantment components ≤ 3), so adding rows does not require a
 * geometry change.</p>
 */
public final class RecipeViewerLayout {

    /** One item slot plus its border — the vanilla container pitch. */
    public static final int SLOT_PITCH = 18;

    // --- thaumcraft:arcane_workbench ---------------------------------------
    public static final int ARCANE_COLS = 3;
    public static final int ARCANE_ROWS = 3;
    /** 9 grid cells: the original's 3x3 workbench grid, shaped and shapeless alike. */
    public static final int ARCANE_MAX_INPUTS = ARCANE_COLS * ARCANE_ROWS;
    public static final int ARCANE_WIDTH = 125;
    public static final int ARCANE_HEIGHT = 72;

    // --- thaumcraft:crucible ------------------------------------------------
    /** One slot: the item thrown into the crucible. */
    public static final int CRUCIBLE_MAX_INPUTS = 1;
    public static final int CRUCIBLE_WIDTH = 125;
    public static final int CRUCIBLE_HEIGHT = 54;

    // --- thaumcraft:infusion -------------------------------------------------
    /** Central pedestal + a 5x3 component grid (data max today: 1 + 9). */
    public static final int INFUSION_COMPONENT_COLS = 5;
    public static final int INFUSION_COMPONENT_ROWS = 3;
    public static final int INFUSION_MAX_INPUTS =
            1 + INFUSION_COMPONENT_COLS * INFUSION_COMPONENT_ROWS;
    public static final int INFUSION_WIDTH = 130;
    public static final int INFUSION_HEIGHT = 96;

    // --- thaumcraft:infusion_enchantment -------------------------------------
    /** Component pedestals only: the craft's result is the enchanted input. */
    public static final int ENCHANTMENT_COMPONENT_COLS = 5;
    public static final int ENCHANTMENT_COMPONENT_ROWS = 3;
    public static final int ENCHANTMENT_MAX_INPUTS =
            ENCHANTMENT_COMPONENT_COLS * ENCHANTMENT_COMPONENT_ROWS;
    public static final int ENCHANTMENT_WIDTH = 125;
    public static final int ENCHANTMENT_HEIGHT = 72;

    private RecipeViewerLayout() {
    }
}
