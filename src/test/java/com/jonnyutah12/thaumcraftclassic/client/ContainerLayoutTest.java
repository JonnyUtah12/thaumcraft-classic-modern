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

    // ------------------------------ TC-M7-04-m2c: the thaumatorium geometry

    @Test
    void thaumatoriumGeometryIsTheOriginals() {
        // GuiThaumatorium never sets xSize/ySize, so its panel is 1.7.10's
        // GuiContainer default 176x166. The window takes that literal pair:
        // the shared derivation (panelHeight = playerGridTop - 13 + 96) lands
        // on 167 for the original's 84 px grid, because vanilla's own rule is
        // imageHeight = player-grid origin + 83. The one pixel is the delta
        // between the original's fixed default and vanilla's derivation, so
        // both numbers are pinned here and the screen uses the original's.
        assertEquals(176, ContainerLayout.PANEL_WIDTH);
        assertEquals(166, ContainerLayout.TAU_PANEL_HEIGHT);
        assertEquals(167, ContainerLayout.panelHeight(ContainerLayout.PLAYER_GRID_TOP),
                "vanilla's derivation for an 84 px player grid — one pixel over the original's 166");

        // The tag row: 16x16 tags at (40 + 16*pos, 40), bars at (41 + 16*pos, 57)
        // with the fill column at 42 + 16*pos — all on the 16 px pitch.
        assertEquals(40, ContainerLayout.thaumatoriumTagX(0));
        assertEquals(120, ContainerLayout.thaumatoriumTagX(5));
        assertEquals(16, ContainerLayout.thaumatoriumTagX(5) - ContainerLayout.thaumatoriumTagX(4),
                "the tag pitch is the original's 16 px");
        assertEquals(41, ContainerLayout.thaumatoriumBarX(0));
        assertEquals(42, ContainerLayout.thaumatoriumBarFillX(0));
        assertEquals(6, ContainerLayout.TAU_VISIBLE_ASPECTS);

        // The original's bar fill is (int)(have / need * 12.0F), uncapped — a
        // full-plus-1 buffer runs one pixel past the 14 px background, and the
        // port keeps that arithmetic verbatim.
        assertEquals(15, ContainerLayout.thaumatoriumBarFill(5, 4));
        assertEquals(12, ContainerLayout.thaumatoriumBarFill(4, 4));
        assertEquals(3, ContainerLayout.thaumatoriumBarFill(1, 4));
        assertEquals(0, ContainerLayout.thaumatoriumBarFill(0, 4));
        assertEquals(0, ContainerLayout.thaumatoriumBarFill(5, 0));

        // The pulses, at the original's formulas: the queue box's
        // 0.6 + sin(t/5)*0.4 + 0.4 and the output tint's
        // 0.3 + sin(t/4)*0.3 + 0.3 (GuiThaumatorium.java:118, 182-183).
        assertEquals(1.0F, ContainerLayout.thaumatoriumQueuePulse(0), 0.0001F);
        assertEquals(0.6F, ContainerLayout.thaumatoriumDullPulse(0), 0.0001F);

        // The queue counter's anchor: (136,33), scaled 0.5, centred, white.
        assertEquals(136, ContainerLayout.TAU_COUNT_X);
        assertEquals(33, ContainerLayout.TAU_COUNT_Y);
        assertEquals(0.5F, ContainerLayout.TAU_COUNT_SCALE, 0.0001F);
    }

    /**
     * TC-M7-04-m2e: the sheet rectangles this window now blits, transcribed from
     * {@code GuiThaumatorium.func_146976_a} / {@code drawAspects}
     * ({@code reference/decompiled/…/GuiThaumatorium.java:50-160}).
     *
     * <p>This is the card's whole claim in one place: the panel, the four pager
     * boxes, the 48x48 highlight, the 24x24 queue box and both essentia bar
     * cells are named by <b>the original's own UVs</b>, at the same destination
     * rectangles its blits used — so the screen draws the shipped sheet, and the
     * flat stand-ins survive only where the bytes are absent (D-004). The
     * numbers are transcriptions, not inventions: each line cites the source
     * call it came from, and the destination sizes are the geometry constants
     * TC-M7-04-m2c already pinned, so a UV that drifted from a size would show
     * up as a mismatch here.</p>
     */
    @Test
    void thaumatoriumGlyphCellsAreTheOriginalsSheetRects() {
        // The panel: drawTexturedModalRect(k, l, 0, 0, xSize, ySize), i.e. the
        // sheet's top-left 176x166 — the same pair the geometry test pins.
        assertEquals(0, ContainerLayout.TAU_SHEET_U);
        assertEquals(0, ContainerLayout.TAU_SHEET_V);
        assertEquals(ContainerLayout.PANEL_WIDTH, ContainerLayout.TAU_SHEET_W);
        assertEquals(ContainerLayout.TAU_PANEL_HEIGHT, ContainerLayout.TAU_SHEET_H);

        // The prev/next arrows at (128,16)/(128,24), 16x8: the source blits u 192
        // when the button is live and 176 when it is not, on rows 16 and 24.
        assertEquals(16, ContainerLayout.TAU_PAGE_W);
        assertEquals(8, ContainerLayout.TAU_PAGE_H);
        assertEquals(176, ContainerLayout.thaumatoriumArrowU(false));
        assertEquals(192, ContainerLayout.thaumatoriumArrowU(true));
        assertEquals(16, ContainerLayout.TAU_UV_PREV_V);
        assertEquals(24, ContainerLayout.TAU_UV_NEXT_V);

        // The aspect pager at (32,40)/(136,40), 8x16: 176/192 for the left box
        // and 184/200 for the right, all on row 32 — the pair is 8 texels apart,
        // unlike the arrows' 16, so each side is stated, not derived.
        assertEquals(8, ContainerLayout.TAU_ASPECT_PAGER_W);
        assertEquals(16, ContainerLayout.TAU_ASPECT_PAGER_H);
        assertEquals(176, ContainerLayout.thaumatoriumAspectPagerU(0, false));
        assertEquals(192, ContainerLayout.thaumatoriumAspectPagerU(0, true));
        assertEquals(184, ContainerLayout.thaumatoriumAspectPagerU(1, false));
        assertEquals(200, ContainerLayout.thaumatoriumAspectPagerU(1, true));
        assertEquals(32, ContainerLayout.TAU_UV_ASPECT_PAGER_V);

        // The essentia bar: the 14x6 background cell at (176,8) and the coloured
        // fill strip at (176,0), blitted i1 texels wide, 4 tall.
        assertEquals(176, ContainerLayout.TAU_UV_BAR_BACKGROUND_U);
        assertEquals(8, ContainerLayout.TAU_UV_BAR_BACKGROUND_V);
        assertEquals(14, ContainerLayout.TAU_BAR_W);
        assertEquals(6, ContainerLayout.TAU_BAR_H);
        assertEquals(176, ContainerLayout.TAU_UV_BAR_FILL_U);
        assertEquals(0, ContainerLayout.TAU_UV_BAR_FILL_V);
        assertEquals(4, ContainerLayout.TAU_BAR_FILL_H);

        // The 24x24 queue box cell (176,56) and the 48x48 highlight (176,96).
        assertEquals(176, ContainerLayout.TAU_UV_PULSE_U);
        assertEquals(56, ContainerLayout.TAU_UV_PULSE_V);
        assertEquals(24, ContainerLayout.TAU_PULSE_SIZE);
        assertEquals(176, ContainerLayout.TAU_UV_HIGHLIGHT_U);
        assertEquals(96, ContainerLayout.TAU_UV_HIGHLIGHT_V);
        assertEquals(48, ContainerLayout.TAU_HIGHLIGHT_SIZE);

        // Every cell is inside the 256x256 sheet the reference measures, and the
        // window is 176 wide: the widest cell this screen blits starts at x 136
        // and is 8 wide, so no blit runs off the panel.
        assertTrue(ContainerLayout.TAU_UV_ENABLED_SHIFT + ContainerLayout.TAU_UV_ASPECT_RIGHT_DISABLED <= 256);
        assertTrue(ContainerLayout.TAU_ASPECT_RIGHT_X + ContainerLayout.TAU_ASPECT_PAGER_W
                <= ContainerLayout.PANEL_WIDTH);

        // The tag's alpha: the SIX-argument drawTag (the thaumatorium's call,
        // GuiThaumatorium.java:167-168) forwards 1.0F with no time term. The
        // workbench's pulse formula belongs to GuiArcaneWorkbench.java:57 and is
        // asserted in pulseKeepsTheOriginalsBandAndPhase — it is NOT this
        // window's tag alpha, and pinning 1.0 here is what keeps the two apart.
        assertEquals(1.0F, ContainerLayout.TAU_TAG_ALPHA, 0.0001F);
    }

    /**
     * The path this window blits must be the path the overlay installs — the
     * other half of D-004. {@code docs/asset-mapping.csv} is the recorded
     * contract; {@code OriginalTextureOverlayTest} proves the copy keeps the
     * {@code gui} directory unrenamed, and this pins the row itself.
     */
    @Test
    void thaumatoriumSheetIsTheMappedOverlayAsset() throws Exception {
        final java.nio.file.Path csv = java.nio.file.Path.of(
                System.getProperty("tc.assetMappingFile", ""));
        assertTrue(java.nio.file.Files.isRegularFile(csv),
                "asset-mapping.csv not wired — run tests through ./gradlew test");

        final String path = ContainerLayout.TAU_SHEET.getPath();
        final String namespace = ContainerLayout.TAU_SHEET.getNamespace();
        assertEquals("thaumcraft", namespace, "the sheet is the mod's own asset");
        assertEquals("textures/gui/gui_thaumatorium.png", path);

        String row = null;
        for (final String line : java.nio.file.Files.readAllLines(csv)) {
            if (line.startsWith("textures/gui/gui_thaumatorium.png,")) {
                row = line;
                break;
            }
        }
        assertTrue(row != null, "docs/asset-mapping.csv has no gui_thaumatorium.png row");
        final String[] fields = row.split(",", 5);
        assertEquals(fields[0], path, "the overlay maps the sheet to ITSELF — the runtime path "
                + "the screen asks for is the path the overlay installs");
        assertEquals("ARR-overlay", fields[3],
                "the sheet is All Rights Reserved: it ships in the overlay build only, which is "
                        + "exactly the case the screen's gate branches on (D-004)");
    }

    // ------------------------------------------- aspect tag (UtilsFX.drawTag)
    //
    // TC-M7-04-m2d. The renderer (client/AspectTagRenderer) draws pixels and so
    // lives where no test in this repo can see them; every number it draws FROM
    // is here, transcribed from
    // reference/decompiled/thaumcraft/thaumcraft/client/lib/UtilsFX.java:814-897.

    @Test
    void tagBoxIsTheOriginalsSixteenPixelsAtThePointMinusEight() {
        assertEquals(16, ContainerLayout.TAG_SIZE,
                "UtilsFX.java:839-842 blits (x,y)..(x+16,y+16): the tag box is 16 px");
        assertEquals(-8, ContainerLayout.TAG_CENTER_OFFSET,
                "GuiArcaneWorkbench.java:65 passes the point minus 8");
        assertEquals(64, ContainerLayout.tagBoxX(72));
        assertEquals(13, ContainerLayout.tagBoxY(21));
        assertEquals(ContainerLayout.TAG_SIZE, ContainerLayout.WB_ASPECT_BOX,
                "the workbench's vis box is the same 16 px tag box");
        assertEquals(ContainerLayout.TAG_SIZE, ContainerLayout.DECON_ASPECT_SIZE,
                "the deconstruction table's box is the same 16 px tag box");
        assertEquals(64, ContainerLayout.DECON_ASPECT_X, "GuiDeconstructionTable.java:39 draws at (64,48)");
        assertEquals(48, ContainerLayout.DECON_ASPECT_Y);
        for (int i = 0; i < ContainerLayout.WB_ASPECT_CENTER_X.length; i++) {
            assertEquals(ContainerLayout.WB_ASPECT_CENTER_X[i] + ContainerLayout.TAG_CENTER_OFFSET,
                    ContainerLayout.aspectBoxX(i), "workbench tag " + i + " must sit on the original's point");
            assertEquals(ContainerLayout.WB_ASPECT_CENTER_Y[i] + ContainerLayout.TAG_CENTER_OFFSET,
                    ContainerLayout.aspectBoxY(i));
        }
    }

    @Test
    void amountGlyphUsesTheOriginalsHalfScaleLiterals() {
        assertEquals(0.5F, ContainerLayout.TAG_AMOUNT_SCALE, 0.0F, "UtilsFX.java:847 glScalef(0.5F,..)");
        assertEquals(32, ContainerLayout.TAG_AMOUNT_ORIGIN, "the 32 in the original's two glyph literals");
        assertEquals(0xFFFFFFFF, ContainerLayout.TAG_AMOUNT_COLOUR, "16777215 (UtilsFX.java:861)");
        assertEquals(32 - 8 + 64 * 2, ContainerLayout.tagAmountScaledX(64, 8),
                "the original's x is 32 - sw + (int)x * 2 — right-aligned on the tag's right edge");
        assertEquals(32 - 8 + 13 * 2, ContainerLayout.tagAmountScaledY(13, 8),
                "the original's y is 32 - font.FONT_HEIGHT + (int)y * 2");
        assertEquals(8, ContainerLayout.TAG_AMOUNT_BOX, "the original's bx = 16 * 0.5F");
        assertEquals(8 - 3 + 64 * 2, ContainerLayout.tagBonusScaledX(64, 6),
                "the bonus glyph's x is 8 - width/2 + a*2 (UtilsFX.java:882)");
        assertEquals(15 - 8 + 13 * 2, ContainerLayout.tagBonusScaledY(13, 8),
                "the bonus glyph's y is 15 - font.FONT_HEIGHT + y*2 (UtilsFX.java:884)");
    }

    @Test
    void amountTextIsTheOriginalsDecimalFormat() {
        // UtilsFX.myFormatter = new DecimalFormat("#######.##") over REAL vis (UtilsFX.java:311,849).
        assertEquals("2", ContainerLayout.tagAmountText(ContainerLayout.displayVisFloat(200)));
        assertEquals("2.5", ContainerLayout.tagAmountText(ContainerLayout.displayVisFloat(250)));
        assertEquals("0.25", ContainerLayout.tagAmountText(0.25F));
        assertEquals("0", ContainerLayout.tagAmountText(0.0F));
        assertEquals("1234.57", ContainerLayout.tagAmountText(1234.5678F),
                "two decimals, no thousands separator, no forced digits");
        assertEquals(2.5F, ContainerLayout.displayVisFloat(250), 0.0F,
                "the centi-vis the menu syncs is the original's * 100");
        assertEquals(2, ContainerLayout.displayVis(250), "the int the vis row shows stays the floor");
    }

    @Test
    void blackAndWhiteTagIsTheOriginalsToneAndAlpha() {
        assertEquals(0.1F, ContainerLayout.TAG_BW_TONE, 0.0F, "glColor4f(0.1F, 0.1F, 0.1F, ..) (UtilsFX.java:828)");
        assertEquals(0.8F, ContainerLayout.TAG_BW_ALPHA, 0.0F, "alpha * 0.8F (UtilsFX.java:828)");
        assertEquals(0.8F, ContainerLayout.tagAlphaOf(1.0F, true), 0.0F);
        assertEquals(1.0F, ContainerLayout.tagAlphaOf(1.0F, false), 0.0F);
        assertEquals(0xCC1A1A1A, ContainerLayout.tagTint(0xFF55AA, 1.0F, true),
                "tone Math.round(0.1F*255)=26, alpha Math.round(0.8F*255)=204");
        assertEquals(0x80FF55AA, ContainerLayout.tagTint(0xFF55AA, 0.5F, false),
                "a colour tag keeps the aspect's colour at the caller's alpha");
    }

    @Test
    void sparkleFramesWalkTheOriginalsSixteenStep() {
        assertEquals(0, ContainerLayout.tagSparkleFrame(0));
        assertEquals(16, ContainerLayout.tagSparkleFrame(1));
        assertEquals(240, ContainerLayout.tagSparkleFrame(15));
        assertEquals(0, ContainerLayout.tagSparkleFrame(16), "16 * (age % 16) (UtilsFX.java:869)");
        assertEquals(240, ContainerLayout.tagSparkleFrame(-1),
                "a negative age wraps inside the sheet (Math.floorMod), it does not go off it");
        assertEquals(-4, ContainerLayout.TAG_SPARKLE_OFFSET, "drawTexturedQuad(x - 4, y - 4, px, 80, 16, 16)");
        assertEquals(80, ContainerLayout.TAG_SPARKLE_V);
        assertEquals(256, ContainerLayout.TAG_SPARKLE_SHEET, "the sheet's UVs are texel * 0.00390625F");
        assertEquals(16, ContainerLayout.TAG_TEXTURE_FALLBACK,
                "the fallback edge when there are no bytes to measure (D-004)");
        assertEquals("thaumcraft", ContainerLayout.PARTICLE_SHEET.getNamespace());
        assertEquals("textures/misc/particles.png", ContainerLayout.PARTICLE_SHEET.getPath(),
                "ParticleEngine.particleTexture, the sheet the fx family already slices");
    }

    @Test
    void pulseKeepsTheOriginalsBandAndPhase() {
        // GuiArcaneWorkbench.java:57 — 0.5 + (sin((age + i*10)/2) * 0.2 - 0.2), and 1.0
        // when the wand can pay. The 15-period sine the card body quotes is the runic
        // matrix's (TileRunicMatrixRenderer.java:106); the 0.4/0.6 pair is the aura
        // HUD's (ClientTickEventsFML.java:549). BD-18 records the difference.
        assertEquals(1.0F, ContainerLayout.aspectAlpha(0, 0, true), 0.0F);
        assertEquals(0.3F, ContainerLayout.aspectAlpha(0, 0, false), 1.0E-5F);
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int age = 0; age < 4000; age++) {
            for (int i = 0; i < 6; i++) {
                final float alpha = ContainerLayout.aspectAlpha(age, i, false);
                min = Math.min(min, alpha);
                max = Math.max(max, alpha);
            }
        }
        assertTrue(min >= 0.1F - 1.0E-5F && max <= 0.5F + 1.0E-5F,
                "the pulse must stay in the original's 0.1..0.5 band, got " + min + ".." + max);
        assertTrue(min < 0.101F && max > 0.499F, "the pulse must reach both ends of the band");
        for (int i = 0; i < 6; i++) {
            assertEquals(ContainerLayout.aspectAlpha(200 + i * 10, 0, false),
                    ContainerLayout.aspectAlpha(200, i, false), 1.0E-5F,
                    "the phase offset is index*10, the original's (age + i*10) / 2");
        }
        assertEquals(Mth.sin(0.0F), 0.0F, 1.0E-6F, "the port's sine is the vanilla one the formula names");
    }
}
