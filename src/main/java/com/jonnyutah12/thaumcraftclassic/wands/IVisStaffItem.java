package com.jonnyutah12.thaumcraftclassic.wands;

/**
 * Marker for vis containers that behave as staffs/sceptres (TC-M6-01 will
 * implement this on the staff item).
 *
 * <p>Original: {@code ItemStaff extends ItemWandCasting}, and the arcane
 * workbench's wand slot explicitly rejected it
 * ({@code SlotLimitedByWand.isItemValid}: {@code instanceof ItemWandCasting
 * && !isStaff(stack)}; the same check gates shift-click routing in
 * {@code ContainerArcaneWorkbench.quickMove}). Amulet-type vis containers
 * ({@code ItemAmuletVis}) are not {@code ItemWandCasting} and were equally
 * rejected. This marker lets the port keep that exclusion before the staff
 * item class exists; the workbench slot accepts {@link IVisContainerItem}
 * stacks that do NOT implement it.</p>
 */
public interface IVisStaffItem {
}
