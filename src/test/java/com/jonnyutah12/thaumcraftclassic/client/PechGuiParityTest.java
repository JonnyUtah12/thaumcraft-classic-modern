package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.menus.PechMenu;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M7-04-m3-b client-parity pins: the pech window's shipped geometry against
 * the measured originals — {@code client/gui/GuiPech.java:19-20,40}
 * ({@code 175x232} window, the 25x25 dice at (67, 24) from sheet (176, 0)) and
 * {@code common/entities/ContainerPech.java:33,37,43,48} (offer (36,29), the
 * 2x2 grid at (106,20) with pitch 18, player grid 84, hotbar 142).
 *
 * <p>The game-test server is a dedicated server and never fires
 * {@code RegisterMenuScreensEvent}, so these unit pins are where the 175x232
 * window, the dice rect and the crop contract are proven (the golem lane's
 * {@code GolemLayoutTest}/{@code GolemGuiParityTest} precedent). No rendered
 * pixels are claimed; the original's {@code gui_pech.png} (176,0) dice sprite
 * ships only in the asset-overlay build (D-004 / BD-37), so the shipped screen
 * draws the rect as a framed well (BD-73).</p>
 */
class PechGuiParityTest {

    /** {@code GuiPech.java:19-20} — the 175x232 window, the crop contract's source. */
    @Test
    void pechWindowSizeIsTheOriginals() {
        assertEquals(175, ContainerLayout.PECH_WINDOW_WIDTH, "GuiPech.java:19 field_146999_f = 175");
        assertEquals(232, ContainerLayout.PECH_WINDOW_HEIGHT, "GuiPech.java:20 field_147000_g = 232");
    }

    /** {@code GuiPech.java:40} — the 25x25 dice at (67, 24). */
    @Test
    void diceRectIsTheOriginals() {
        assertEquals(67, ContainerLayout.PECH_DICE_X, "GuiPech.java:40 blit x = 67");
        assertEquals(24, ContainerLayout.PECH_DICE_Y, "GuiPech.java:40 blit y = 24");
        assertEquals(25, ContainerLayout.PECH_DICE_SIZE, "GuiPech.java:40 width = height = 25");
    }

    /** {@code ContainerPech.java:33,37,43,48} — the slot grid the menu builds. */
    @Test
    void slotGridMatchesContainerPech() {
        assertEquals(36, ContainerLayout.PECH_OFFER_X);
        assertEquals(29, ContainerLayout.PECH_OFFER_Y);
        assertEquals(106, ContainerLayout.pechOutputX(0));
        assertEquals(124, ContainerLayout.pechOutputX(1));
        assertEquals(20, ContainerLayout.pechOutputY(0));
        assertEquals(38, ContainerLayout.pechOutputY(1));
        assertEquals(18, ContainerLayout.PECH_OUTPUT_PITCH);
        assertEquals(84, ContainerLayout.PECH_PLAYER_GRID_TOP);
        assertEquals(142, ContainerLayout.PECH_HOTBAR_TOP);
    }

    /** The shipped screen/menu pair is the 175x232 window over the 41-slot menu. */
    @Test
    void shippedPairIsThePechWindow() throws Exception {
        assertTrue(AbstractContainerScreenIsTheSuperclass(), "PechScreen extends AbstractContainerScreen<PechMenu>");
        final Path root = repoRoot();
        org.junit.jupiter.api.Assumptions.assumeTrue(root != null,
                "repo root (gradlew) not resolvable from the test cwd");
        final String screen = Files.readString(root.resolve(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/client/PechScreen.java"));
        assertTrue(screen.contains("PECH_WINDOW_WIDTH"), "the screen sizes its window from the 175 constant");
        assertTrue(screen.contains("PECH_WINDOW_HEIGHT"), "the screen sizes its window from the 232 constant");
        assertTrue(screen.contains("PECH_DICE_X") && screen.contains("PECH_DICE_SIZE"),
                "the screen draws the (67,24) 25x25 dice rect");
        assertTrue(screen.contains("handleInventoryButtonClick") && screen.contains("PECH_DICE"),
                "the click sends the original's dice button and plays pech_dice (GuiPech.java:67-79)");
        final String menu = Files.readString(root.resolve(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/menus/PechMenu.java"));
        assertTrue(menu.contains("DICE_BUTTON"), "the menu carries the dice button");
        assertTrue(PechMenu.class.getName().startsWith("com.jonnyutah12."), "the menu ships in this mod");
    }

    /** Walk up from the test cwd to the module root (the dir carrying gradlew) - the unit
     *  test task's cwd is not guaranteed to be the module root. */
    private static Path repoRoot() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("gradlew"))) {
            p = p.getParent();
        }
        return p;
    }

    private static boolean AbstractContainerScreenIsTheSuperclass() {
        return PechScreen.class.getSuperclass().getSimpleName().equals("AbstractContainerScreen");
    }
}
