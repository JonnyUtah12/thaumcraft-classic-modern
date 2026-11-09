package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemCore;
import com.jonnyutah12.thaumcraftclassic.menus.GolemMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The golem configuration screen (content-mapping.csv
 * {@code Thaumcraft:GUI_GOLEM}, gui_id 0, original
 * {@code thaumcraft.client.gui.GuiGolem}, 455 lines). TC-M7-04-m3-a.
 *
 * <p>Layout from {@link GolemMenu}: a 176 px wide panel whose height is
 * derived from the menu's 84 px player grid (the original's rows at
 * {@code 84}/{@code 142}, panel 166 in 1.7.10's default
 * {@code GuiContainer} size), plus the golem inventory window the original
 * blits at {@code (96 + a/2*28, 12 + a%2*31)} with its scroll rail at
 * {@code (111,68)}/{@code (135,68)}. The window is drawn only under the
 * original's own gate — {@code getCore() > -1 &&
 * ItemGolemCore.hasInventory(core)} — reading the <b>live entity</b> the menu
 * resolved (the core is synched data, so this is synced state, not a drawn
 * fake).</p>
 *
 * <p><b>Background (docs/BEHAVIORAL_DEVIATIONS.md BD-17).</b> The original
 * draws its whole panel from {@code guigolem.png} (256x256, overlay-only
 * per D-004, so not in the code-only jar): the panel, the 24x24
 * type-varied slot frames, the 24x8 scroll arrows, the 8x8 toggle pips, the
 * color-tab strips, the blurb text and the 3x30 golem portrait. This screen
 * draws the vanilla panel and player piece at the original coordinates and
 * frames the window cells with vanilla {@code minecraft:container/slot}
 * sprites — no original texel is referenced.</p>
 *
 * <p><b>What is not drawn, and why (BD-19).</b> The programming UI — the
 * guard-target toggles (core 4, buttons 51..54), the Fill amount toggle
 * (core 0, button 50), the Use action toggles (core 8, buttons 50..52), the
 * sort toggles (Upgrade-5, buttons 55..57) and the per-slot color tabs
 * (Upgrade-4, buttons {@code a}/{@code a+slotCount}) — reads
 * {@code golem.getToggles()}/{@code getColors()}/{@code getUpgradeAmount},
 * and the blurb line reads the {@code golemblurb.<core>.text}/{@code
 * golemthreat.<n>.text} lang set (plus the client's 1-in-4 threat roll).
 * None of that state ships in this tree: the toggle/color arrays and the
 * upgrade-array client sync are golem runtime (TC-M6-08-s-b), and the blurb
 * keys would be dead lang entries (the localization audit's reverse pass
 * fails dead keys). The screen draws none of it; every hitbox and gate is
 * pinned in {@code ContainerLayout} + {@code GolemLayoutTest} and lands with
 * the state. The 3x30 golem portrait is likewise absent: drawing the entity
 * in a GUI needs the golem renderer (TC-M6-08-s-c), and a box standing in
 * for it would be a drawn fake. The {@code (111,68)}/{@code (135,68)}
 * scroll boxes are framed and their clicks wired to menu buttons 66/67 at
 * the original hitboxes; while the menu's {@code maxScroll} clamp pins the
 * page at 0 the buttons are inert, as the original's are for a ≤ 6-slot
 * golem.</p>
 */
@OnlyIn(Dist.CLIENT)
public class GolemScreen extends AbstractContainerScreen<GolemMenu> {

    /** The shipped menu title key; the packet title wins when non-empty. */
    static final String TITLE_KEY = "container.thaumcraft.gui_golem";

    public GolemScreen(final GolemMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title.getString().isEmpty() ? Component.translatable(TITLE_KEY) : title);
        this.imageWidth = ContainerLayout.PANEL_WIDTH;
        this.imageHeight = ContainerLayout.panelHeight(ContainerLayout.PLAYER_GRID_TOP);
        this.titleLabelX = ContainerLayout.titleLabelX();
        this.titleLabelY = ContainerLayout.titleLabelY();
        this.inventoryLabelX = ContainerLayout.titleLabelX();
        this.inventoryLabelY = ContainerLayout.inventoryLabelY(ContainerLayout.PLAYER_GRID_TOP);
    }

    /**
     * The original's window gate: a core that carries an inventory. The
     * golem is the live entity the menu resolved (its core is synched data);
     * a menu whose entity is gone draws no window, exactly as the original's
     * container never bound one for a core without {@code hasInventory}.
     */
    static boolean windowVisible(final GolemMenu menu) {
        final EntityGolemBase golem = menu.getGolem();
        if (golem == null) {
            return false;
        }
        final int core = golem.getCore();
        return core > -1 && ItemGolemCore.hasInventory(core);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY,
                       final float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick,
                            final int mouseX, final int mouseY) {
        ScreenPanel.drawBackground(guiGraphics, this.leftPos, this.topPos, ContainerLayout.PLAYER_GRID_TOP);

        if (windowVisible(this.menu)) {
            // The six window cells at the original 24x24 frame boxes, framed
            // with vanilla slot sprites (BD-17): the slots themselves bind
            // when the mob inventory lands (BD-19).
            for (int cell = 0; cell < ContainerLayout.GOLEM_WINDOW_SLOTS; cell++) {
                ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                        ContainerLayout.golemFrameX(cell), ContainerLayout.golemFrameY(cell),
                        ContainerLayout.GOLEM_WINDOW_FRAME, ContainerLayout.GOLEM_WINDOW_FRAME);
            }
            // The scroll rail (the original blits the arrows only when
            // slotCount > 6; with no container the rail is framed, not active).
            ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                    ContainerLayout.GOLEM_SCROLL_UP_X, ContainerLayout.GOLEM_SCROLL_Y,
                    ContainerLayout.GOLEM_SCROLL_WIDTH, ContainerLayout.GOLEM_SCROLL_HEIGHT);
            ScreenPanel.frameArea(guiGraphics, this.leftPos, this.topPos,
                    ContainerLayout.GOLEM_SCROLL_DOWN_X, ContainerLayout.GOLEM_SCROLL_Y,
                    ContainerLayout.GOLEM_SCROLL_WIDTH, ContainerLayout.GOLEM_SCROLL_HEIGHT);
        }
    }

    /**
     * The original {@code func_73864_a}: the two scroll boxes
     * ({@code (111,68,24,8)} / {@code (135,68,24,8)}) send menu buttons 66/67
     * — same boxes, same buttons. The color-tab, guard/fill/use and sort
     * hitboxes of the original are not wired here while their widgets are not
     * drawn (BD-19); their geometry is pinned in {@code ContainerLayout}.
     */
    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || !windowVisible(this.menu) || this.minecraft == null) {
            return false;
        }
        final int relX = (int) mouseX - this.leftPos;
        final int relY = (int) mouseY - this.topPos;
        if (relY >= ContainerLayout.GOLEM_SCROLL_Y
                && relY < ContainerLayout.GOLEM_SCROLL_Y + ContainerLayout.GOLEM_SCROLL_HEIGHT) {
            if (relX >= ContainerLayout.GOLEM_SCROLL_UP_X
                    && relX < ContainerLayout.GOLEM_SCROLL_UP_X + ContainerLayout.GOLEM_SCROLL_WIDTH) {
                this.minecraft.gameMode.handleInventoryButtonClick(
                        this.menu.containerId, GolemMenu.BUTTON_SCROLL_UP);
                return true;
            }
            if (relX >= ContainerLayout.GOLEM_SCROLL_DOWN_X
                    && relX < ContainerLayout.GOLEM_SCROLL_DOWN_X + ContainerLayout.GOLEM_SCROLL_WIDTH) {
                this.minecraft.gameMode.handleInventoryButtonClick(
                        this.menu.containerId, GolemMenu.BUTTON_SCROLL_DOWN);
                return true;
            }
        }
        return false;
    }
}
