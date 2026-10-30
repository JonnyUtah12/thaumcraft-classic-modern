package com.jonnyutah12.thaumcraftclassic.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Locks the focus-pouch panel geometry to the original
 * ({@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiFocusPouch.java}
 * + {@code common/container/ContainerFocusPouch.java}).
 *
 * <p>Same contract as {@code GuiShellLayoutTest}, and the same reason: the
 * game-test server never opens a screen, so this is the only automated proof
 * the pouch's slots land on the 175x232 panel at all.</p>
 *
 * <p>Original coordinates: panel 175x232 ({@code GuiFocusPouch.java:19-20});
 * 18 focus slots at {@code (37 + i%6*18, 51 + i/6*18)}
 * ({@code ContainerFocusPouch.java:31-33}); player grid rows at
 * {@code 151 + row*18}, hotbar at {@code 209}
 * ({@code bindPlayerInventory}, lines 37-47); carrier-slot marker at
 * {@code (8 + currentItem*18, 209)}, 16x16 ({@code GuiFocusPouch.func_146979_b},
 * lines 23-32).</p>
 */
public class FocusPouchLayoutTest {

    @Test
    public void panelIsTheOriginalsOwnNonDefaultSize() {
        assertEquals(175, ContainerLayout.POUCH_PANEL_WIDTH,
                "GuiFocusPouch sets guiWidth = 175 (line 19)");
        assertEquals(232, ContainerLayout.POUCH_PANEL_HEIGHT,
                "GuiFocusPouch sets guiHeight = 232 (line 20)");
        assertEquals(176, ContainerLayout.PANEL_WIDTH,
                "the pouch panel is the one size in this family that is NOT the 1.7.10 default");
        assertEquals(6, ContainerLayout.POUCH_COLUMNS);
        assertEquals(3, ContainerLayout.POUCH_ROWS);
    }

    @Test
    public void focusGridIsSixByThreeAtTheOriginalsAnchor() {
        assertEquals(37, ContainerLayout.POUCH_GRID_X);
        assertEquals(51, ContainerLayout.POUCH_GRID_Y);
        assertEquals(37, ContainerLayout.pouchSlotX(0));
        assertEquals(51, ContainerLayout.pouchSlotY(0));
        assertEquals(127, ContainerLayout.pouchSlotX(5), "slot 5 = 37 + 5*18");
        assertEquals(69, ContainerLayout.pouchSlotY(6), "slot 6 starts row 2");
        assertEquals(127, ContainerLayout.pouchSlotX(17));
        assertEquals(87, ContainerLayout.pouchSlotY(17), "last row = 51 + 2*18");
        assertEquals(104, ContainerLayout.pouchGridBottom(), "87 + 17: the grid fits the panel");
    }

    @Test
    public void playerGridAndHotbarUseThePouchsOwnLowerRows() {
        assertEquals(151, ContainerLayout.POUCH_PLAYER_GRID_TOP,
                "ContainerFocusPouch.bindPlayerInventory: 151 + row*18");
        assertEquals(209, ContainerLayout.POUCH_HOTBAR_TOP,
                "ContainerFocusPouch.bindPlayerInventory: 209");
        assertEquals(151, ContainerLayout.POUCH_PLAYER_GRID_TOP,
                "the pouch does NOT share the 84 used by the default-size containers");
        assertEquals(84, ContainerLayout.PLAYER_GRID_TOP, "the default stays put");
        assertEquals(141, ContainerLayout.inventoryLabelY(ContainerLayout.POUCH_PLAYER_GRID_TOP),
                "the inventory label keeps cr1's rule — piece top (151-13) + 3 — so the label sits"
                        + " just above the pouch's own player row, as on every other screen");
    }

    @Test
    public void carrierMarkerSitsOnTheSlotThatCarriesThePouch() {
        assertEquals(8, ContainerLayout.POUCH_MARKER_X_ORIGIN,
                "GuiFocusPouch.func_146979_b: 8 + blockSlot*18");
        assertEquals(16, ContainerLayout.POUCH_MARKER_SIZE);
        assertEquals(8, ContainerLayout.pouchMarkerX(0));
        assertEquals(98, ContainerLayout.pouchMarkerX(5), "5th hotbar slot = 8 + 5*18");
        assertEquals(152, ContainerLayout.pouchMarkerX(8), "the 9th slot stays inside the 175 px panel");
        assertTrue(ContainerLayout.pouchMarkerX(8) + ContainerLayout.POUCH_MARKER_SIZE
                        <= ContainerLayout.POUCH_PANEL_WIDTH,
                "the marker is drawn at the hotbar row of a 175 px panel");
        assertEquals(209, ContainerLayout.POUCH_HOTBAR_TOP, "same y as the hotbar row it marks");
    }

    @Test
    public void vanillaPlayerPieceIsCroppedToThePouchPanel() {
        assertEquals(138, ContainerLayout.playerPanelTop(ContainerLayout.POUCH_PLAYER_GRID_TOP),
                "151 - 13, the same conversion the default-size screens use");
        assertEquals(94, ContainerLayout.playerPieceHeight(
                        ContainerLayout.POUCH_PANEL_HEIGHT, ContainerLayout.POUCH_PLAYER_GRID_TOP),
                "the 96 px vanilla piece loses its bottom 2 px to the 232 px sheet (BD-18)");
        assertEquals(232, ContainerLayout.playerPanelTop(ContainerLayout.POUCH_PLAYER_GRID_TOP)
                        + ContainerLayout.playerPieceHeight(ContainerLayout.POUCH_PANEL_HEIGHT,
                                ContainerLayout.POUCH_PLAYER_GRID_TOP),
                "the cropped piece ends exactly at the frame's bottom edge");
        assertEquals(167, ContainerLayout.panelHeight(ContainerLayout.PLAYER_GRID_TOP),
                "the derived default panel is piece top (84-13) + the 96 px piece");
        assertEquals(96, ContainerLayout.playerPieceHeight(
                        ContainerLayout.panelHeight(ContainerLayout.PLAYER_GRID_TOP),
                        ContainerLayout.PLAYER_GRID_TOP),
                "a default-size panel crops nothing, so the existing screens draw exactly as before");
    }

    @Test
    public void pouchGridAndCarrierConstantsAreNotStaleCopies() {
        // Cross-check the pouch coordinates against the shared slot pitch: if
        // Slot ever reports a 17 px interior, every pouch coordinate moves with
        // it, exactly as GuiShellLayoutTest guards the trunk coordinates.
        final int slotPitch = ContainerLayout.SLOT_FRAME;
        assertEquals(18, slotPitch);
        assertEquals(ContainerLayout.pouchSlotX(0) + slotPitch, ContainerLayout.pouchSlotX(1));
        assertEquals(ContainerLayout.pouchSlotY(0) + slotPitch, ContainerLayout.pouchSlotY(6));
        assertEquals(ContainerLayout.POUCH_GRID_X
                        + (ContainerLayout.POUCH_COLUMNS - 1) * slotPitch,
                ContainerLayout.pouchSlotX(ContainerLayout.POUCH_COLUMNS - 1),
                "the row pitch is the shared one, not a per-screen copy");
        assertEquals(ContainerLayout.pouchMarkerX(0) + slotPitch, ContainerLayout.pouchMarkerX(1));
        assertTrue(ContainerLayout.pouchGridBottom() < ContainerLayout.POUCH_PLAYER_GRID_TOP,
                "the focus grid must not overlap the player grid the original puts at 151");
    }
}
