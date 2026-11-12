package com.jonnyutah12.thaumcraftclassic.client;

import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    // -------------------------------- arcane workbench (TC-M7-04-m2, gui_id 13)

    @Test
    void workbenchPanelIsTheOriginals190By234() {
        // field_146999_f is xSize and field_147000_g is ySize
        // (reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv:648-649), and
        // GuiArcaneWorkbench's constructor assigns 190 to the first, 234 to the
        // second. Read the pair backwards and the panel comes out 234 wide — which
        // is how this slice's first pass drew it, and why the assertion names the CSV.
        assertEquals(190, ContainerLayout.WB_PANEL_WIDTH,
                "xSize — vanilla's 176 plus a 14 px strip");
        assertEquals(234, ContainerLayout.WB_PANEL_HEIGHT, "ySize — the height the original blits");
        assertEquals(234, ContainerLayout.panelHeight(ContainerLayout.WB_PLAYER_GRID_TOP),
                "the port derives its height from the original's 151 px player grid: 138 + 96");
        assertEquals(ContainerLayout.WB_PANEL_HEIGHT, ContainerLayout.panelHeight(ContainerLayout.WB_PLAYER_GRID_TOP),
                "the derived height lands on the original's own ySize, so the port draws the original's rect");
        assertEquals(0, ContainerLayout.WB_PLAYER_PIECE_X,
                "the vanilla 176 px player piece sits at x=0 so the slots at x=16 keep vanilla's inset");
        assertTrue(ContainerLayout.WB_PANEL_WIDTH - ContainerLayout.PANEL_WIDTH == 14,
                "the strip ScreenPanel fills beside the player piece is 190 - 176 = 14 px");
    }

    @Test
    void theOriginalsOwnBlitCoversItsHotbarRow() {
        // The original blits (xSize, ySize) = (190, 234); its hotbar sits at
        // y = 209 and is 18 tall, so 227 <= 234: the original's art covers its own
        // hotbar, and the port's 234-derived height puts every row on screen too.
        assertTrue(ContainerLayout.WB_HOTBAR_TOP + ContainerLayout.SLOT_FRAME <= ContainerLayout.WB_PANEL_HEIGHT,
                "hotbar bottom 227 must sit inside the original's 234 px ySize");
        assertTrue(ContainerLayout.WB_PLAYER_GRID_X + 9 * ContainerLayout.SLOT_FRAME <= ContainerLayout.WB_PANEL_WIDTH,
                "the 176 px player row must sit inside the original's 190 px xSize");
    }

    @Test
    void workbenchSlotsUseTheOriginalsAnchorsAndPitch() {
        assertEquals(40, ContainerLayout.workbenchGridX(0));
        assertEquals(64, ContainerLayout.workbenchGridX(1), "the original's grid pitch is 24, not vanilla's 18");
        assertEquals(88, ContainerLayout.workbenchGridX(2));
        assertEquals(40, ContainerLayout.workbenchGridY(0));
        assertEquals(64, ContainerLayout.workbenchGridY(1));
        assertEquals(88, ContainerLayout.workbenchGridY(2));
        assertEquals(160, ContainerLayout.WB_RESULT_X);
        assertEquals(64, ContainerLayout.WB_RESULT_Y);
        assertEquals(160, ContainerLayout.WB_WAND_X);
        assertEquals(24, ContainerLayout.WB_WAND_Y);
        assertEquals(16, ContainerLayout.workbenchPlayerX(0));
        assertEquals(160, ContainerLayout.workbenchPlayerX(8));
        assertEquals(151, ContainerLayout.workbenchPlayerY(0));
        assertEquals(187, ContainerLayout.workbenchPlayerY(2), "151 + 2*18: the original's pitch here is vanilla's");
        assertEquals(209, ContainerLayout.WB_HOTBAR_TOP, "the original's hotbar row, 4 px below the player grid");
    }

    @Test
    void everyWorkbenchSlotAnchorFitsThePanelItDraws() {
        final int panelHeight = ContainerLayout.panelHeight(ContainerLayout.WB_PLAYER_GRID_TOP);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                assertTrue(ContainerLayout.workbenchGridX(col) + ContainerLayout.SLOT_FRAME
                                <= ContainerLayout.WB_PANEL_WIDTH,
                        "grid slot (" + row + "," + col + ") inside the 190 px panel");
                assertTrue(ContainerLayout.workbenchGridY(row) + ContainerLayout.SLOT_FRAME <= panelHeight,
                        "grid slot (" + row + "," + col + ") inside the panel height");
            }
        }
        for (int col = 0; col < 9; col++) {
            assertTrue(ContainerLayout.workbenchPlayerX(col) + ContainerLayout.SLOT_FRAME
                    <= ContainerLayout.WB_PANEL_WIDTH);
        }
        assertTrue(ContainerLayout.WB_RESULT_Y + ContainerLayout.SLOT_FRAME <= panelHeight);
        assertTrue(ContainerLayout.WB_WAND_Y + ContainerLayout.SLOT_FRAME <= panelHeight);
        assertTrue(ContainerLayout.WB_HOTBAR_TOP + ContainerLayout.SLOT_FRAME <= panelHeight,
                "the hotbar row is inside the panel the port draws, exactly as the original's own "
                        + "(xSize, ySize) = (190, 234) blit covers it");
    }

    @Test
    void workbenchAspectHexagonIsTheOriginalsSixPoints() {
        assertArrayEquals(new int[] {72, 24, 24, 72, 120, 120}, ContainerLayout.WB_ASPECT_CENTER_X,
                "aer, terra, ignis, aqua, ordo, perditio — the order Aspect.getPrimalAspects() walks");
        assertArrayEquals(new int[] {21, 43, 102, 124, 102, 43}, ContainerLayout.WB_ASPECT_CENTER_Y);
        assertEquals(16, ContainerLayout.WB_ASPECT_BOX, "UtilsFX.drawTag draws a 16x16 tag at its point");
        assertEquals(64, ContainerLayout.aspectBoxX(0), "the box is 8 px up-left of the point");
        assertEquals(13, ContainerLayout.aspectBoxY(0));
        assertEquals(16, ContainerLayout.aspectBoxX(1));
        assertEquals(35, ContainerLayout.aspectBoxY(1));
        assertTrue(ContainerLayout.WB_ASPECT_CENTER_X[5] + ContainerLayout.WB_ASPECT_BOX / 2
                <= ContainerLayout.WB_PANEL_WIDTH);
    }

    @Test
    void workbenchAspectAlphaIsTheOriginalsPulse() {
        assertEquals(1.0F, ContainerLayout.aspectAlpha(0, 0, true),
                "an affordable aspect drew at full alpha");
        assertEquals(0.3F, ContainerLayout.aspectAlpha(0, 0, false), 1e-6,
                "0.5 + (sin(0/2)*0.2 - 0.2) = 0.3");
        assertEquals(0.5F + (Mth.sin(11 / 2.0F) * 0.2F - 0.2F),
                ContainerLayout.aspectAlpha(1, 1, false), 1e-6,
                "the original's phase is (gameTime + index*10) / 2.0F");
        for (int time = 0; time < 400; time++) {
            for (int index = 0; index < 6; index++) {
                final float alpha = ContainerLayout.aspectAlpha(time, index, false);
                assertTrue(alpha >= 0.1F - 1e-5F && alpha <= 0.5F + 1e-5F,
                        "the pulse lives between 0.1 and 0.5, exactly as the original's formula allows");
            }
        }
    }

    @Test
    void workbenchColourCarriesAlphaAndCostsDivideByTheOriginalsHundred() {
        assertEquals(0xFFFFFFFF, ContainerLayout.withAlpha(0xFFFFFF, 1.0F));
        assertEquals(0x00FFFFFF, ContainerLayout.withAlpha(0xFFFFFF, 0.0F));
        assertEquals(0x80FF0000, ContainerLayout.withAlpha(0xFF0000, 0.5F));
        assertEquals(0xFF123456, ContainerLayout.withAlpha(0x123456, 2.0F), "alpha is clamped, not wrapped");
        assertEquals(0x00123456, ContainerLayout.withAlpha(0x123456, -1.0F));
        assertEquals(100, ContainerLayout.CENTI_VIS_PER_VIS,
                "the original's * 100 (ItemWandCasting.java:314) — the unit the cost slots travel in");
        assertEquals(3, ContainerLayout.displayVis(300));
        assertEquals(0, ContainerLayout.displayVis(99), "the original's integer division");
    }

    @Test
    void workbenchNoticeIsTheOriginalsTransformTextAndColour() {
        assertEquals(168, ContainerLayout.WB_INSUFFICIENT_X);
        assertEquals(46, ContainerLayout.WB_INSUFFICIENT_Y);
        assertEquals(0.5F, ContainerLayout.WB_INSUFFICIENT_SCALE, 0.0F, "the original's glScalef(0.5F, 0.5F, 0)");
        assertEquals(0xFFEE000E, ContainerLayout.WB_INSUFFICIENT_COLOUR,
                "the original's colour int 15625838, with full alpha for 1.21.1");
        assertEquals(0xEE000E, ContainerLayout.WB_INSUFFICIENT_COLOUR & 0xFFFFFF);
        assertEquals(0xA8555555, ContainerLayout.WB_BLOCKED_TINT,
                "the original's glColor4f(0.33, 0.33, 0.33, 0.66) over the blocked result — 0.66 alpha is 0xA8");
        assertEquals(0x555555, ContainerLayout.WB_BLOCKED_TINT & 0xFFFFFF, "the grey itself");
        assertTrue(ContainerLayout.WB_INSUFFICIENT_X < ContainerLayout.WB_PANEL_WIDTH,
                "the notice is centred on x=168 (the original draws at -width/2 after the translate), "
                        + "so the anchor itself must sit inside the 190 px panel");
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
}
