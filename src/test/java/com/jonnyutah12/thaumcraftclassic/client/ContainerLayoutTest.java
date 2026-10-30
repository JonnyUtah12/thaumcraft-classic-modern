package com.jonnyutah12.thaumcraftclassic.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M7-04-s acceptance for the shared screen geometry: every constant and every
 * formula {@link ContainerLayout} hands the screens, pinned without a client.
 *
 * <p>This is the level at which a screen can be proved in this repo at all. The
 * unit harness has no window and no bootstrapped registries, and the game-test
 * server is a dedicated server where {@code RegisterMenuScreensEvent} never
 * fires, so neither can observe a drawn GUI. What <i>can</i> be pinned is the
 * arithmetic — and that is exactly where a screen's parity lives once the
 * original's blit calls are transcribed: the panel size, the 13 px player-piece
 * offset, the 1 px slot-frame offset, the 46 px break rail and the bar's
 * downward drain.</p>
 *
 * <p>The last group is the cross-check that keeps this crate honest:
 * {@code TrunkScreen} (TC-M7-04-s-cr1) has its own copies of these numbers, and
 * they must agree with {@link ContainerLayout}. If someone changes one and not
 * the other, this test fails.</p>
 *
 * <p>NOT proved here, and stated as such in {@code docs/FEATURE_MATRIX.md}: that
 * any of it renders correctly on a real client.</p>
 */
class ContainerLayoutTest {

    // ---------------------------------------------------- panel geometry

    @Test
    void panelWidthIsTheVanillaContainerPanelWidth() {
        assertEquals(176, ContainerLayout.PANEL_WIDTH,
                "both original GUI sheets and 1.21.1's AbstractContainerScreen use a 176 px panel");
    }

    @Test
    void playerPieceIsTheVanillaGeneric54Piece() {
        assertEquals(0, ContainerLayout.PLAYER_PANEL_U);
        assertEquals(126, ContainerLayout.PLAYER_PANEL_V);
        assertEquals(96, ContainerLayout.PLAYER_PANEL_HEIGHT);
        assertEquals(13, ContainerLayout.PLAYER_PANEL_TO_FIRST_SLOT,
                "vanilla: a 3-row chest blits the piece at 71 and its first player row is at 84");
        assertEquals(84, ContainerLayout.PLAYER_GRID_TOP,
                "every original container in this crate puts the player grid at y = 84");
        assertEquals(142, ContainerLayout.PLAYER_HOTBAR_TOP);
        assertEquals(0, ContainerLayout.inventoryLabelY(84) - (84 - 13) - 3,
                "the inventory label sits 3 px below the piece top");
    }

    @Test
    void panelHeightIsDerivedFromTheMenusPlayerGridOrigin() {
        assertEquals(71, ContainerLayout.playerPanelTop(84));
        assertEquals(167, ContainerLayout.panelHeight(84));
        assertEquals(74, ContainerLayout.inventoryLabelY(84));
        // The trunk menu's grid is at 118, the original's 23 px row pitch.
        assertEquals(105, ContainerLayout.playerPanelTop(118));
        assertEquals(201, ContainerLayout.panelHeight(118));
    }

    @Test
    void centringMatchesTheOriginalAndVanillaArithmetic() {
        // The original: (screenWidth - xSize) / 2, (screenHeight - ySize) / 2.
        assertEquals(125, ContainerLayout.leftPos(427));
        assertEquals(0, ContainerLayout.leftPos(176));
        assertEquals(36, ContainerLayout.topPos(240, 167));
        assertEquals(20, ContainerLayout.topPos(240, 200));
    }

    // ------------------------------------------------------- slot frames

    @Test
    void slotFramesSitOnePixelUpLeftOfTheItemAnchor() {
        assertEquals(17, ContainerLayout.slotFrameX(18));
        assertEquals(17, ContainerLayout.slotFrameY(18));
        assertEquals(79, ContainerLayout.slotFrameX(80));
        assertEquals(23, ContainerLayout.slotFrameY(24));
        assertEquals(18, ContainerLayout.SLOT_FRAME);
    }

    @Test
    void gridRowsRoundUpOnTheNineWideGrid() {
        assertEquals(0, ContainerLayout.gridRows(0));
        assertEquals(3, ContainerLayout.gridRows(27));
        assertEquals(4, ContainerLayout.gridRows(28));
        assertEquals(24, ContainerLayout.gridRows(216), "the magic box's 27*8 slot cap");
    }

    @Test
    void machineRegionTestUsesTheFrameExtentNotTheAnchor() {
        final int region = ContainerLayout.playerPanelTop(ContainerLayout.PLAYER_GRID_TOP);
        assertEquals(71, region);
        // The original's own three box rows sit at 18/36/54 and end at 71.
        assertTrue(ContainerLayout.fitsInMachineRegion(18, region));
        assertTrue(ContainerLayout.fitsInMachineRegion(36, region));
        assertTrue(ContainerLayout.fitsInMachineRegion(54, region),
                "the third row's frame ends at y = 71, exactly the piece top");
        assertFalse(ContainerLayout.fitsInMachineRegion(72, region),
                "a fourth row would draw over the player piece");
        assertFalse(ContainerLayout.fitsInMachineRegion(54, region - 1));
    }

    // ---------------------------------------------------- decon break bar

    @Test
    void breakBarRailIsTheOriginalsBlit() {
        assertEquals(40, ContainerLayout.DECON_BREAK_TIME, "TileDeconstructionTable.BREAK_TIME");
        assertEquals(93, ContainerLayout.DECON_BAR_X, "GuiDeconstructionTable blits the bar at k + 93");
        assertEquals(15, ContainerLayout.DECON_BAR_TOP);
        assertEquals(9, ContainerLayout.DECON_BAR_WIDTH);
        assertEquals(46, ContainerLayout.DECON_BAR_PIXELS, "getBreakTimeScaled(46)");
        assertEquals(64, ContainerLayout.DECON_ASPECT_X, "the aspect box the original draws at (64,48)");
        assertEquals(48, ContainerLayout.DECON_ASPECT_Y);
        assertEquals(16, ContainerLayout.DECON_ASPECT_SIZE);
    }

    @Test
    void breakBarHeightIsGetBreakTimeScaled() {
        assertEquals(46, ContainerLayout.scaledBarHeight(40, 46), "a full break fills the rail");
        assertEquals(23, ContainerLayout.scaledBarHeight(20, 46), "breaktime * 46 / 40");
        assertEquals(1, ContainerLayout.scaledBarHeight(1, 46));
        assertEquals(0, ContainerLayout.scaledBarHeight(0, 46), "breaktime 0 paints nothing");
        assertEquals(0, ContainerLayout.scaledBarHeight(-3, 46), "a negative timer never paints");
        assertEquals(46, ContainerLayout.scaledBarHeight(41, 46), "clamped to the rail");
    }

    @Test
    void breakBarDrainsDownwardFromTheRailTop() {
        assertEquals(15, ContainerLayout.barTop(15, 46, 46));
        assertEquals(38, ContainerLayout.barTop(15, 46, 23));
        assertEquals(61, ContainerLayout.barTop(15, 46, 0), "an empty bar leaves the rail bottom");
    }

    // ------------------------------------------------------- arcane spa

    @Test
    void spaTankAndMixBoxAreTheOriginalsBlits() {
        assertEquals(106, ContainerLayout.SPA_TANK_X);
        assertEquals(11, ContainerLayout.SPA_TANK_TOP);
        assertEquals(10, ContainerLayout.SPA_TANK_WIDTH);
        assertEquals(55, ContainerLayout.SPA_TANK_HEIGHT);
        assertEquals(107, ContainerLayout.SPA_FLUID_X);
        assertEquals(15, ContainerLayout.SPA_FLUID_TOP);
        assertEquals(48, ContainerLayout.SPA_FLUID_PIXELS, "the original's 48 px fluid column");
        assertEquals(89, SpaScreen.MIX_X);
        assertEquals(35, SpaScreen.MIX_Y);
        assertEquals(8, SpaScreen.MIX_SIZE);
    }

    // ------------------------------------------------------- resources

    @Test
    void slotSpriteAndPlayerPanelAreVanillaPaths() {
        assertEquals("minecraft", ContainerLayout.SLOT_SPRITE.getNamespace());
        assertEquals("container/slot", ContainerLayout.SLOT_SPRITE.getPath());
        assertEquals("minecraft", ContainerLayout.PLAYER_PANEL.getNamespace());
        assertEquals("textures/gui/container/generic_54.png", ContainerLayout.PLAYER_PANEL.getPath());
    }

    @Test
    void titleLabelsUseTheCratesEightByFourOrigin() {
        assertEquals(8, ContainerLayout.titleLabelX());
        assertEquals(4, ContainerLayout.titleLabelY());
    }

    // ------------------------------------- no drift against cr1 (TrunkScreen)

    @Test
    void cr1TrunkScreenAgreesWithTheSharedLayout() {
        assertEquals(TrunkScreen.PANEL_WIDTH, ContainerLayout.PANEL_WIDTH,
                "cr1's panel width and the shared one must not drift apart");
        assertEquals(TrunkScreen.PANEL_HEIGHT, ContainerLayout.panelHeight(TrunkScreen.PLAYER_GRID_TOP),
                "cr1 derives its height from its own 118 px player grid");
        assertEquals(TrunkScreen.PLAYER_PANEL_TOP, ContainerLayout.playerPanelTop(TrunkScreen.PLAYER_GRID_TOP));
        assertEquals(TrunkScreen.PLAYER_PANEL_TO_FIRST_SLOT, ContainerLayout.PLAYER_PANEL_TO_FIRST_SLOT);
        assertEquals(TrunkScreen.PLAYER_PANEL_HEIGHT, ContainerLayout.PLAYER_PANEL_HEIGHT);
        assertEquals(TrunkScreen.PLAYER_PANEL_U, ContainerLayout.PLAYER_PANEL_U);
        assertEquals(TrunkScreen.PLAYER_PANEL_V, ContainerLayout.PLAYER_PANEL_V);
        assertEquals(TrunkScreen.PLAYER_PANEL, ContainerLayout.PLAYER_PANEL);
        assertEquals(TrunkScreen.SLOT_SPRITE, ContainerLayout.SLOT_SPRITE);
        assertEquals(TrunkScreen.PANEL_FILL, ContainerLayout.PANEL_FILL);
        assertEquals(TrunkScreen.PANEL_FRAME, ContainerLayout.PANEL_FRAME);
    }

    // ---------------------------------- TC-M7-04-m4: synced-state geometry

    @Test
    void spaFluidColumnScalesTheWayTheOriginalBlitDid() {
        // GuiSpa: bar = fluidAmount / capacity, (int)(48 - 48*bar) px of the
        // (107,15) 48 px column left blank at the top — the fill is
        // floor(amount * 48 / capacity) px rising from the bottom.
        assertEquals(0, ContainerLayout.scaledFluidHeight(0, 5000, 48), "an empty tank paints nothing");
        assertEquals(9, ContainerLayout.scaledFluidHeight(1000, 5000, 48), "1000/5000 of 48 px");
        assertEquals(47, ContainerLayout.scaledFluidHeight(4999, 5000, 48), "the fill floors");
        assertEquals(48, ContainerLayout.scaledFluidHeight(5000, 5000, 48), "full paints the rail");
        assertEquals(48, ContainerLayout.scaledFluidHeight(6000, 5000, 48), "clamped, never past the rail");
        assertEquals(0, ContainerLayout.scaledFluidHeight(-5, 5000, 48), "a negative amount never paints");
        assertEquals(0, ContainerLayout.scaledFluidHeight(5, 0, 48), "a zero capacity never paints");
        assertEquals(15, ContainerLayout.fluidTop(15, 48, 48), "full starts at the column top");
        assertEquals(54, ContainerLayout.fluidTop(15, 48, 9), "9 px sit at the column bottom");
        assertEquals(63, ContainerLayout.fluidTop(15, 48, 0), "empty starts at the column floor");
    }

    @Test
    void spaMixAndTooltipBoxesAreTheOriginalsBoxes() {
        // GuiSpa: the 8x8 mix indicator at (89,35); its tooltip box is the
        // 10x10 at (88,34) and the tank's tooltip box the 10x55 at (104,10).
        assertEquals(89, ContainerLayout.SPA_MIX_X);
        assertEquals(35, ContainerLayout.SPA_MIX_Y);
        assertEquals(8, ContainerLayout.SPA_MIX_SIZE);
        assertEquals(SpaScreen.MIX_X, ContainerLayout.SPA_MIX_X,
                "the screen's click box and the shared geometry must not drift");
        assertEquals(SpaScreen.MIX_Y, ContainerLayout.SPA_MIX_Y);
        assertEquals(SpaScreen.MIX_SIZE, ContainerLayout.SPA_MIX_SIZE);
        assertEquals(88, ContainerLayout.SPA_MIX_TIP_X);
        assertEquals(34, ContainerLayout.SPA_MIX_TIP_Y);
        assertEquals(10, ContainerLayout.SPA_MIX_TIP_SIZE);
        assertEquals(104, ContainerLayout.SPA_TANK_TIP_X);
        assertEquals(10, ContainerLayout.SPA_TANK_TIP_Y);
        assertEquals(10, ContainerLayout.SPA_TANK_TIP_WIDTH);
        assertEquals(55, ContainerLayout.SPA_TANK_TIP_HEIGHT);
        assertEquals(0xFF8A5CC8, ContainerLayout.SPA_MIX_ON_FILL,
                "the authored on-texel is the crate's indicator colour");
    }

    @Test
    void magicBoxPageMathIsTheOriginalsRowBind() {
        // ContainerMagicBox.bindBoxInventory(row): 3 rows of 9 = 27 slots per page.
        assertEquals(27, ContainerLayout.MAGIC_BOX_PAGE_SLOTS);
        assertEquals(1, ContainerLayout.magicBoxPageCount(0), "an empty box is one page");
        assertEquals(1, ContainerLayout.magicBoxPageCount(27), "one box is one page");
        assertEquals(2, ContainerLayout.magicBoxPageCount(28), "the 28th slot opens page 2");
        assertEquals(2, ContainerLayout.magicBoxPageCount(54));
        assertEquals(8, ContainerLayout.magicBoxPageCount(216), "the original 8-box cluster");
        assertEquals(0, ContainerLayout.magicBoxPageStart(0));
        assertEquals(27, ContainerLayout.magicBoxPageStart(1), "the original (j + row) * 9");
        assertEquals(54, ContainerLayout.magicBoxPageStart(2));
        assertEquals(1, ContainerLayout.clampPage(5, 2), "clamped to the last page");
        assertEquals(0, ContainerLayout.clampPage(-1, 2), "clamped to page 0");
        assertEquals(1, ContainerLayout.clampPage(1, 2), "an in-range page is untouched");
    }

    @Test
    void magicBoxPageWidgetSitsInTheTitleBand() {
        assertEquals(152, ContainerLayout.MAGIC_BOX_PAGE_PREV_X);
        assertEquals(2, ContainerLayout.MAGIC_BOX_PAGE_PREV_Y);
        assertEquals(164, ContainerLayout.MAGIC_BOX_PAGE_NEXT_X);
        assertEquals(2, ContainerLayout.MAGIC_BOX_PAGE_NEXT_Y);
        assertEquals(10, ContainerLayout.MAGIC_BOX_PAGE_BUTTON_SIZE);
        assertTrue(ContainerLayout.MAGIC_BOX_PAGE_NEXT_X + ContainerLayout.MAGIC_BOX_PAGE_BUTTON_SIZE <= 176,
                "the arrows stay inside the 176 px panel");
        assertTrue(ContainerLayout.MAGIC_BOX_PAGE_NEXT_Y + ContainerLayout.MAGIC_BOX_PAGE_BUTTON_SIZE
                        <= ContainerLayout.playerPanelTop(84),
                "the title band is above the player panel piece");
    }

    @Test
    void trunkStayBoxKeepsTheOriginalsInclusiveEdge() {
        // GuiTravelingTrunk: k1 >= 0 && l1 >= 0 && k1 < 10 && l1 <= 10 at (112,0).
        assertEquals(112, ContainerLayout.TRUNK_STAY_X);
        assertEquals(0, ContainerLayout.TRUNK_STAY_Y);
        assertEquals(10, ContainerLayout.TRUNK_STAY_WIDTH);
        assertEquals(10, ContainerLayout.TRUNK_STAY_HEIGHT);
        assertTrue(ContainerLayout.inTrunkStayBox(0, 0));
        assertTrue(ContainerLayout.inTrunkStayBox(9, 0));
        assertTrue(ContainerLayout.inTrunkStayBox(9, 10), "the <= 10 keeps row 10 inside");
        assertFalse(ContainerLayout.inTrunkStayBox(10, 0), "the < 10 closes column 10");
        assertFalse(ContainerLayout.inTrunkStayBox(0, 11), "row 11 is outside");
        assertFalse(ContainerLayout.inTrunkStayBox(-1, 0), "left of the panel is outside");
        assertFalse(ContainerLayout.inTrunkStayBox(0, -1), "above the panel is outside");
        assertEquals(0xFF8A5CC8, ContainerLayout.TRUNK_STAY_ON_FILL,
                "the authored on-texel is the crate's indicator colour");
    }
}
