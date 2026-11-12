package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.menus.ArcaneWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Arcane workbench screen (content-mapping.csv {@code thaumcraft:gui_arcane_workbench},
 * gui_id 13, original gui sheet 256x256 {@code gui_arcaneworkbench.png}).
 *
 * <p>Parity target, measured off
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiArcaneWorkbench.java}
 * and {@code .../common/container/ContainerArcaneWorkbench.java}: a
 * <b>190&nbsp;x&nbsp;234 px</b> panel (the original's {@code field_146999_f = 190}
 * = {@code xSize} and {@code field_147000_g = 234} = {@code ySize}, per
 * {@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv:648-649} — the
 * pair is easy to read backwards, and the field CSV is what settles it), the 3x3
 * grid on the original's <b>24 px</b> pitch at {@code (40,40)} — not vanilla's 18
 * — the result at {@code (160,64)}, the wand slot at {@code (160,24)}, the player
 * grid at {@code (16,151)} and the hotbar at {@code (16,209)}, the "Insufficient
 * vis" notice translated to {@code (168,46)} and scaled 0.5 in colour
 * {@code 0xEE000E}, the grey blocked-result marker over {@code (160,64)}, and the
 * six vis-cost tags on the hexagon {@code (72,21) (24,43) (24,102) (72,124)
 * (120,102) (120,43)}. Every one of those numbers is a named constant in
 * {@link ContainerLayout}; the drawing is the shared {@link ScreenPanel} recipe, so
 * this class is geometry and synced numbers only.</p>
 *
 * <p><b>Backgrounds are vanilla at the original coordinates</b>
 * ({@code docs/BEHAVIORAL_DEVIATIONS.md} BD-17/BD-18): the original's 256x256
 * sheet is overlay-only (D-004), so there is no panel art to blit. The flat fill,
 * the frame and the 176 px vanilla player piece go at the original's x; the piece
 * sits at x = 0 (the slots at x = 16 keep vanilla's 8 px inset) and the 14 px the
 * 190 px panel has over it are filled with the same tone. The port derives its
 * height from the original's 151 px player-grid origin
 * ({@link ContainerLayout#panelHeight(int)}) and lands on {@code 138 + 96 = 234},
 * which is the original's own {@code ySize} — the panel the port draws is the
 * original's rect, and {@code ContainerLayoutTest} asserts both halves so a grid
 * move cannot quietly resize it.
 *
 * <p>The original's {@code drawGuiContainerForegroundLayer} is <b>empty</b>: its
 * window carries no title and no "Inventory" label. The port draws both, at the
 * vanilla positions, because its panel is drawn from vanilla art and an
 * unlabelled window is the one thing the original's art made unnecessary; that
 * difference is recorded in BD-18.</p>
 *
 * <p><b>The vis display is menu data, never a client-side guess.</b> The original
 * computed the cost list on the client and pulsed its tags with
 * {@code 0.5 + (sin((gameTime + i*10)/2) * 0.2 - 0.2)}. Here the six prices, the
 * six wand charges and the "Insufficient vis" flag arrive as this menu's data slots
 * (see {@link ArcaneWorkbenchMenu}), computed where the truth lives — the recipe
 * lookup and the wand's vis component — and the pulse's formula is the original's,
 * kept world-free in {@link ContainerLayout#aspectAlpha}.</p>
 *
 * <p><b>Each cost is the original's tag.</b> The original drew
 * {@code UtilsFX.drawTag} at each hexagon point (a 16x16 quad of the aspect's own
 * texture, tinted by the aspect colour at the pulse alpha, with the amount as a
 * 0.5-scale glyph inside the box); the port draws the same quad through
 * {@link AspectTagRenderer} (TC-M7-04-m2d). The aspect sheets are ARR-overlay
 * (D-004), so with the overlay applied the real sprite is tinted and drawn, and in
 * the code-only jar the same box carries the aspect's own colour at the same alpha
 * — the geometry and the animation are the original's in both builds, and no art
 * is invented. BD-18 keeps the two gaps that are not closed here.</p>
 */
@OnlyIn(Dist.CLIENT)
public class ArcaneWorkbenchScreen extends AbstractContainerScreen<ArcaneWorkbenchMenu> {

    /** The lang key of the arcane workbench menu (the menu type is named for the gui row). */
    static final String TITLE_KEY = "container.thaumcraft.gui_arcane_workbench";

    /**
     * The port's title position. The original drew <b>no title</b> at all
     * ({@code GuiArcaneWorkbench.drawGuiContainerForegroundLayer} is empty), so
     * there is no original x to copy: the port centres the container title in its
     * own 190 px panel at vanilla's {@code y = 6}, and BD-18 records the label the
     * original's own art made unnecessary.
     */
    static final int TITLE_X = ContainerLayout.WB_PANEL_WIDTH / 2;
    static final int TITLE_Y = 6;

    public ArcaneWorkbenchScreen(final ArcaneWorkbenchMenu menu, final Inventory inventory,
                                 final Component title) {
        super(menu, inventory, title.getString().isEmpty() ? Component.translatable(TITLE_KEY) : title);
        this.imageWidth = ContainerLayout.WB_PANEL_WIDTH;
        this.imageHeight = ContainerLayout.panelHeight(ContainerLayout.WB_PLAYER_GRID_TOP);
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = ContainerLayout.titleLabelX();
        this.inventoryLabelY = ContainerLayout.inventoryLabelY(ContainerLayout.WB_PLAYER_GRID_TOP);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY,
                       final float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        // The wand slot is the one the original left unlabelled; its rule (a wand
        // that carries vis, never a staff) is the least obvious thing on the panel.
        final Slot hovered = this.hoveredSlot;
        if (hovered != null && hovered.index == ArcaneWorkbenchMenu.SLOT_WAND) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("container.thaumcraft.wand_slot_hint"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick,
                            final int mouseX, final int mouseY) {
        ScreenPanel.drawBackground(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.WB_PLAYER_GRID_TOP, ContainerLayout.WB_PANEL_WIDTH,
                ContainerLayout.WB_PLAYER_PIECE_X);
        for (final Slot slot : this.menu.slots) {
            ScreenPanel.frameSlot(guiGraphics, this.leftPos, this.topPos, slot);
        }
        renderVisCosts(guiGraphics);
        markBlockedResult(guiGraphics);
    }

    /**
     * The grey the original painted over the result slot while the wand could not
     * pay: {@code glColor4f(0.33F, 0.33F, 0.33F, 0.66F)} around the item draw at
     * {@code (var5 + 160, var6 + 64)} — inside {@code drawGuiContainerBackgroundLayer},
     * so it sits under the slot's item exactly as the port's {@link #renderBg} does.
     *
     * <p>The original's greyed icon was a <i>preview of the craft output</i>: it
     * rendered {@code findMatchingArcaneRecipe(...)}'s result there, which the port
     * cannot do — that item is not synced (the tile's result slot stays empty until
     * the wand pays). So the port marks the slot with the original's grey and
     * geometry and stops there; the preview is a BD-18 follow-up, not a faked
     * display.</p>
     */
    private void markBlockedResult(final GuiGraphics guiGraphics) {
        if (!this.menu.isInsufficientVis()) {
            return;
        }
        ScreenPanel.fillArea(guiGraphics, this.leftPos, this.topPos,
                ContainerLayout.WB_RESULT_X, ContainerLayout.WB_RESULT_Y,
                ContainerLayout.SLOT_FRAME, ContainerLayout.SLOT_FRAME,
                ContainerLayout.WB_BLOCKED_TINT);
    }

    /**
     * The original's {@code drawScreen} tail — the hexagon of vis-cost tags and the
     * "Insufficient vis" notice — drawn from the menu's synced data slots.
     *
     * <p>The original drew this in the foreground, after the slot contents; the
     * notice sits at {@code y = 46}, below the wand slot's {@code y = 24..42}, and
     * each 16x16 tag is behind an empty area of the panel, so drawing it with the
     * background puts the same pixels at the same coordinates.</p>
     */
    private void renderVisCosts(final GuiGraphics guiGraphics) {
        final List<Aspect> primals = Aspect.getPrimalAspects();
        for (int i = 0; i < ArcaneWorkbenchMenu.PRIMALS && i < primals.size(); i++) {
            final int cost = this.menu.getCost(i);
            if (cost <= 0) {
                // The original skipped a tag whose amount was 0.
                continue;
            }
            final Aspect aspect = primals.get(i);
            final boolean affordable = this.menu.getWandCharge(i) >= cost;
            final float alpha = ContainerLayout.aspectAlpha(pulseTime(), i, affordable);
            /*
             * The tag itself — the original's UtilsFX.drawTag at
             * GuiArcaneWorkbench.java:65:
             *   drawTag(var5 + aspectLocs[count][0] - 8, var6 + aspectLocs[count][1] - 8,
             *           primal, (float) cost.getAmount(primal), 0, zLevel, 771, alpha)
             * The -8 shift is already inside ContainerLayout.aspectBoxX/Y, and the port's
             * price travels in centi-vis, so the amount is divided back out
             * (ContainerLayout.displayVisFloat). The alpha is the original's own
             * pulse (GuiArcaneWorkbench.java:57) — 0.1..0.5 while the wand is short of
             * that aspect, steady 1.0 when it can pay.
             *
             * This replaces TC-M7-04-m2's stand-ins (a flat colour block, a frame and the
             * amount as a 1.0-scale label under the box): the box is now the original's
             * 16x16 tag quad tinted by the aspect, with the amount glyph inside it at
             * 0.5 scale, right-aligned on the tag's right edge. What BD-18 still names:
             * the aspect sprite is ARR-overlay (D-004), so the code-only jar fills that
             * same box with the aspect's own colour at the same alpha, and the greyed
             * output preview needs a result-slot item this menu does not sync.
             */
            AspectTagRenderer.drawTag(guiGraphics, this.font,
                    this.leftPos + ContainerLayout.aspectBoxX(i),
                    this.topPos + ContainerLayout.aspectBoxY(i),
                    aspect, ContainerLayout.displayVisFloat(cost), 0, alpha, false, pulseTime());
        }

        if (this.menu.isInsufficientVis()) {
            drawInsufficientVis(guiGraphics);
        }
    }

    /**
     * The original's notice at the original's transform: translate to
     * {@code (left+168, top+46)}, scale 0.5, then draw centred on the translate
     * point — the original computes {@code ll = font.width(text) / 2} and draws at
     * {@code -ll}, i.e. centred at x = 0 in the scaled space, in colour
     * {@code 15625838} = {@code 0xEE000E} ({@code GuiArcaneWorkbench.java:93-112}).
     * That puts the text's centre at panel x = 168, inside the 190 px panel; its
     * text was hardcoded English in 1.7.10, and the port puts it in the lang file.
     */
    private void drawInsufficientVis(final GuiGraphics guiGraphics) {
        final var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate((float) (this.leftPos + ContainerLayout.WB_INSUFFICIENT_X),
                (float) (this.topPos + ContainerLayout.WB_INSUFFICIENT_Y), 0.0F);
        pose.scale(ContainerLayout.WB_INSUFFICIENT_SCALE, ContainerLayout.WB_INSUFFICIENT_SCALE, 1.0F);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("container.thaumcraft.insufficient_vis"),
                0, 0, ContainerLayout.WB_INSUFFICIENT_COLOUR);
        pose.popPose();
    }

    /**
     * The original's {@code this.mc.theInGameTime} — elapsed client ticks, the
     * phase its tag pulse ran on. Derived from the monotonic clock so the screen
     * never reads the world; the formula that turns it into an alpha is the
     * original's own ({@link Mth#sin}, in {@link ContainerLayout#aspectAlpha}).
     */
    private static int pulseTime() {
        return (int) (Util.getMillis() / 50L);
    }
}
