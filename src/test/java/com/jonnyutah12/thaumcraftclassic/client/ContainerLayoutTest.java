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

    // --------------------------------------------- alchemical furnace (gui_id 9)

    @Test
    void alchemicalFurnaceWidgetsAreTheOriginalsBlits() {
        // ContainerAlchemyFurnace's two slots.
        assertEquals(80, ContainerLayout.FURNACE_SLOT_INPUT_X);
        assertEquals(8, ContainerLayout.FURNACE_SLOT_INPUT_Y);
        assertEquals(80, ContainerLayout.FURNACE_SLOT_FUEL_X);
        assertEquals(48, ContainerLayout.FURNACE_SLOT_FUEL_Y);
        // GuiAlchemyFurnace's three scaled blits, at their rails.
        assertEquals(80, ContainerLayout.FURNACE_FIRE_X);
        assertEquals(26, ContainerLayout.FURNACE_FIRE_TOP);
        assertEquals(16, ContainerLayout.FURNACE_FIRE_WIDTH);
        assertEquals(20, ContainerLayout.FURNACE_FIRE_PIXELS, "getBurnTimeRemainingScaled(20)");
        assertEquals(106, ContainerLayout.FURNACE_COOK_X);
        assertEquals(13, ContainerLayout.FURNACE_COOK_TOP);
        assertEquals(9, ContainerLayout.FURNACE_COOK_WIDTH);
        assertEquals(46, ContainerLayout.FURNACE_COOK_PIXELS, "getCookProgressScaled(46)");
        assertEquals(60, ContainerLayout.FURNACE_TANK_X);
        assertEquals(8, ContainerLayout.FURNACE_TANK_TOP);
        assertEquals(10, ContainerLayout.FURNACE_TANK_WIDTH);
        assertEquals(55, ContainerLayout.FURNACE_TANK_HEIGHT);
        assertEquals(61, ContainerLayout.FURNACE_VIS_X);
        assertEquals(12, ContainerLayout.FURNACE_VIS_TOP);
        assertEquals(8, ContainerLayout.FURNACE_VIS_WIDTH);
        assertEquals(48, ContainerLayout.FURNACE_VIS_PIXELS, "getContentsScaled(48)");
        assertEquals(200, ContainerLayout.FURNACE_FALLBACK_BURN_TIME, "the original's 0-burn guard");
    }

    @Test
    void scaledProgressMatchesTheOriginalScaledGetters() {
        // getCookProgressScaled(46): cook * 46 / smeltTime, bottom-anchored.
        assertEquals(46, ContainerLayout.scaledProgress(100, 100, 46));
        assertEquals(23, ContainerLayout.scaledProgress(50, 100, 46));
        assertEquals(0, ContainerLayout.scaledProgress(0, 100, 46), "an idle furnace draws no fill");
        assertEquals(0, ContainerLayout.scaledProgress(10, 0, 46), "a zero max reads as empty, no divide by zero");
        assertEquals(46, ContainerLayout.scaledProgress(200, 100, 46), "the fill clamps at the rail height");
        // The 1.7.10 furnace: cookTime 60 of a smeltTime 100 (the port's tile
        // divides over its own furnaceMaxCookTime; the shape is the same).
        assertEquals(27, ContainerLayout.scaledProgress(60, 100, 46));
        assertEquals(13 + 46 - 27,
                ContainerLayout.barTop(ContainerLayout.FURNACE_COOK_TOP, ContainerLayout.FURNACE_COOK_PIXELS,
                        ContainerLayout.scaledProgress(60, 100, 46)),
                "the fill's top edge sits so the bar grows from the rail bottom (13+46)");
    }

    // -------------------------------------------------- arcane bore (gui_id 15)

    @Test
    void arcaneBoreWidgetsAreTheOriginalsBlits() {
        // ContainerArcaneBore: focus slot, pickaxe slot, short player grid.
        assertEquals(26, ContainerLayout.BORE_SLOT_FOCUS_X);
        assertEquals(18, ContainerLayout.BORE_SLOT_FOCUS_Y);
        assertEquals(74, ContainerLayout.BORE_SLOT_PICKAXE_X);
        assertEquals(18, ContainerLayout.BORE_SLOT_PICKAXE_Y);
        assertEquals(59, ContainerLayout.BORE_PLAYER_GRID_TOP);
        assertEquals(117, ContainerLayout.BORE_PLAYER_HOTBAR_TOP);
        assertEquals(59 + 58, ContainerLayout.BORE_PLAYER_HOTBAR_TOP, "the original's 58 px grid→hotbar gap");
        // GuiArcaneBore: the panel override and the readout block.
        assertEquals(141, ContainerLayout.BORE_SHEET_HEIGHT, "the original's field_147000_g");
        assertEquals(142, ContainerLayout.panelHeight(ContainerLayout.BORE_PLAYER_GRID_TOP),
                "the vanilla derivation is 142; the 1 px sheet difference is the original's rounding");
        assertEquals(74, ContainerLayout.BORE_BROKEN_X);
        assertEquals(18, ContainerLayout.BORE_BROKEN_Y);
        assertEquals(16, ContainerLayout.BORE_BROKEN_SIZE);
        assertEquals(112, ContainerLayout.BORE_TEXT_X);
        assertEquals(8, ContainerLayout.BORE_TEXT_Y);
        assertEquals(0, ContainerLayout.BORE_TEXT_WIDTH_Y);
        assertEquals(10, ContainerLayout.BORE_TEXT_SPEED_Y);
        assertEquals(24, ContainerLayout.BORE_TEXT_OTHER_Y);
        assertEquals(34, ContainerLayout.BORE_TEXT_ROWS_TOP);
        assertEquals(9, ContainerLayout.BORE_TEXT_ROW_STEP);
    }

    @Test
    void boreReadoutFormulasAreTheOriginals() {
        // GuiArcaneBore: "Width: " + (1 + (area + maxRadius) * 2).
        assertEquals(1, ContainerLayout.boreWidth(0, 0));
        assertEquals(11, ContainerLayout.boreWidth(1, 4));
        assertEquals(25, ContainerLayout.boreWidth(1, 11));
        // The break icon gate: damage + 1 >= maxDamage.
        assertTrue(ContainerLayout.borePickaxeNearBroken(150, 151));
        assertTrue(ContainerLayout.borePickaxeNearBroken(151, 151));
        assertEquals(false, ContainerLayout.borePickaxeNearBroken(149, 151));
        // GuiArcaneBore's three decimal color literals.
        assertEquals(0xFF000000 | 12632256, ContainerLayout.BORE_COLOR_NATIVE, "12632256 native");
        assertEquals(0xFF000000 | 15648330, ContainerLayout.BORE_COLOR_FORTUNE, "15648330 fortune");
        assertEquals(0xFF000000 | 8421631, ContainerLayout.BORE_COLOR_SILK, "8421631 silk");
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
