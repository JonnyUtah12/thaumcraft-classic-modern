package com.jonnyutah12.thaumcraftclassic.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TC-M7-04-m3-a acceptance for the golem window's shared geometry: every
 * golem constant and formula {@link ContainerLayout} hands {@code GolemScreen}
 * (and the runtime card's future widgets), pinned without a client — the same
 * proof level {@link ContainerLayoutTest} established (the game-test server is
 * a dedicated server where {@code RegisterMenuScreensEvent} never fires, so
 * pixels are not provable here; the numbers are).
 *
 * <p>Every figure is the original's, transcribed from
 * {@code ContainerGolem.bindGolemInventory}/{@code bindPlayerInventory} and
 * {@code GuiGolem} (blits, button hitboxes, {@code mouseClicked});
 * {@link GolemGuiParityTest} pins the same numbers against the decompiled
 * source text so a drift here fails against the reference too.</p>
 */
class GolemLayoutTest {

    @Test
    void panelIsTheOriginals176x167() {
        // ContainerGolem's player grid sits at y=84 (rows) and 142 (hotbar):
        // 142 + 18 + 7 = 167 panel — the original 1.7.10 GuiContainer 166 plus
        // 1.21.1's 1 px label inset, derived from the grid, never hard-coded.
        assertEquals(176, ContainerLayout.PANEL_WIDTH);
        assertEquals(84, ContainerLayout.PLAYER_GRID_TOP,
                "ContainerGolem.bindPlayerInventory: 84 + i*18");
        assertEquals(142, ContainerLayout.PLAYER_HOTBAR_TOP,
                "ContainerGolem.bindPlayerInventory: 8 + i*18, 142");
        assertEquals(167, ContainerLayout.panelHeight(ContainerLayout.PLAYER_GRID_TOP));
    }

    @Test
    void windowCellsSitOnTheOriginal24x31Pitch() {
        // bindGolemInventory: (100 + a/2*28, 16 + a%2*31); GuiGolem
        // func_146976_a: the 24x24 frame four pixels up-left.
        assertEquals(96, ContainerLayout.golemFrameX(0));
        assertEquals(12, ContainerLayout.golemFrameY(0));
        assertEquals(96, ContainerLayout.golemFrameX(1));
        assertEquals(43, ContainerLayout.golemFrameY(1), "a%2*31: the second row sits at 12+31");
        assertEquals(124, ContainerLayout.golemFrameX(2), "a/2*28: the second column sits at 96+28");
        assertEquals(152, ContainerLayout.golemFrameX(5), "the third column sits at 96+56");
        assertEquals(43, ContainerLayout.golemFrameY(5));
        assertEquals(100, ContainerLayout.golemSlotX(0));
        assertEquals(16, ContainerLayout.golemSlotY(0));
        assertEquals(156, ContainerLayout.golemSlotX(5));
        assertEquals(47, ContainerLayout.golemSlotY(5));
        assertEquals(6, ContainerLayout.GOLEM_WINDOW_SLOTS,
                "the original window binds Math.min(6, slotCount) slots per page");
        // The 3x2 grid ends flush with the 176 px panel: last frame 152+24 = 176.
        assertEquals(176, ContainerLayout.golemFrameX(5) + ContainerLayout.GOLEM_WINDOW_FRAME);
    }

    @Test
    void scrollRailIsTheOriginal24x8BoxesAt111And135() {
        // GuiGolem: blit (111,68,24,8) up / (135,68,24,8) down; mouseClicked
        // sends func_146286_a(66/67) on exactly those boxes.
        assertEquals(111, ContainerLayout.GOLEM_SCROLL_UP_X);
        assertEquals(135, ContainerLayout.GOLEM_SCROLL_DOWN_X);
        assertEquals(68, ContainerLayout.GOLEM_SCROLL_Y);
        assertEquals(24, ContainerLayout.GOLEM_SCROLL_WIDTH);
        assertEquals(8, ContainerLayout.GOLEM_SCROLL_HEIGHT);
    }

    @Test
    void coreWidgetsAreTheOriginalPipBoxes() {
        // Guard (core 4): (104, 5 + 16*row, 8, 8), buttons 51..54.
        assertEquals(104, ContainerLayout.GOLEM_GUARD_X);
        assertEquals(5, ContainerLayout.GOLEM_GUARD_Y);
        assertEquals(16, ContainerLayout.GOLEM_GUARD_PITCH);
        assertEquals(8, ContainerLayout.GOLEM_GUARD_SIZE);
        assertEquals(51, ContainerLayout.GOLEM_GUARD_BUTTON_FIRST);
        // Fill (core 0): (62, 54, 8, 8).
        assertEquals(62, ContainerLayout.GOLEM_FILL_X);
        assertEquals(54, ContainerLayout.GOLEM_FILL_Y);
        assertEquals(8, ContainerLayout.GOLEM_FILL_SIZE);
        // Use (core 8): (42, 40 + 10*row, 8, 8).
        assertEquals(42, ContainerLayout.GOLEM_USE_X);
        assertEquals(40, ContainerLayout.GOLEM_USE_Y);
        assertEquals(10, ContainerLayout.GOLEM_USE_PITCH);
        assertEquals(8, ContainerLayout.GOLEM_USE_SIZE);
    }

    @Test
    void sortRowsShiftForTheSortingCore() {
        // GuiGolem (Upgrade-5 && canSort): (180, 24 + 10*row, 64, 8) and, for
        // core 10 (Sorting), (66, 12 + 10*row, 64, 8).
        assertEquals(180, ContainerLayout.golemSortBoxX(0));
        assertEquals(24, ContainerLayout.golemSortBoxY(0, 0));
        assertEquals(34, ContainerLayout.golemSortBoxY(0, 1));
        assertEquals(66, ContainerLayout.golemSortBoxX(10), "the Sorting core shifts the rows to x=66");
        assertEquals(36, ContainerLayout.golemSortBoxY(10, 0), "and down by shifty=12");
        assertEquals(64, ContainerLayout.GOLEM_SORT_WIDTH);
        assertEquals(8, ContainerLayout.GOLEM_SORT_HEIGHT);
        assertEquals(55, ContainerLayout.GOLEM_SORT_BUTTON_FIRST, "buttons 55/56/57");
    }

    @Test
    void colorTabsSitAboveTheFramesOnTheOriginalStrip() {
        // GuiGolem (UpgradeAmount >= 4): 24x12 at (96 + a/2*28, 4 + a%2*31);
        // the left 8 px decrement (button a), the +16 px 8 px increment
        // (button a + slotCount).
        assertEquals(4, ContainerLayout.GOLEM_TAB_Y);
        assertEquals(24, ContainerLayout.GOLEM_TAB_WIDTH);
        assertEquals(12, ContainerLayout.GOLEM_TAB_HEIGHT);
        assertEquals(8, ContainerLayout.GOLEM_TAB_BUTTON_WIDTH);
        assertEquals(16, ContainerLayout.GOLEM_TAB_PLUS_OFFSET);
    }

    @Test
    void textAndPortraitAnchorsAreTheOriginals() {
        // func_146286_b: 0.5-scaled drawCenteredString at (80, 22) -> (40, 11).
        assertEquals(40, ContainerLayout.GOLEM_BLURB_X);
        assertEquals(11, ContainerLayout.GOLEM_BLURB_Y);
        // The 3x30 entity portrait centre: (guiLeft+51, guiTop+75).
        assertEquals(51, ContainerLayout.GOLEM_PORTRAIT_X);
        assertEquals(75, ContainerLayout.GOLEM_PORTRAIT_Y);
    }

    @Test
    void theFrameIsTheSlotAnchorFourPixelsUpLeft() {
        for (int cell = 0; cell < ContainerLayout.GOLEM_WINDOW_SLOTS; cell++) {
            assertEquals(ContainerLayout.golemFrameX(cell) + ContainerLayout.GOLEM_SLOT_INSET,
                    ContainerLayout.golemSlotX(cell), "slot x = frame x + 4 (cell " + cell + ")");
            assertEquals(ContainerLayout.golemFrameY(cell) + ContainerLayout.GOLEM_SLOT_INSET,
                    ContainerLayout.golemSlotY(cell), "slot y = frame y + 4 (cell " + cell + ")");
        }
    }
}
