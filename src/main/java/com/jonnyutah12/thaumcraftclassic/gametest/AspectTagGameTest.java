package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTable;
import com.jonnyutah12.thaumcraftclassic.client.ContainerLayout;
import com.jonnyutah12.thaumcraftclassic.menus.DeconstructionTableMenu;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import com.jonnyutah12.thaumcraftclassic.tiles.TileDeconstructionTable;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * TC-M7-04-m2d (t_9135351b) acceptance: the seams the aspect-tag renderer runs on.
 *
 * <p>The renderer itself draws pixels and so lives on the client, which a
 * dedicated-server game test can never reach — the same limit every GUI card in
 * this port recorded. What CAN be pinned here is everything the drawing is
 * computed from, and the one piece of state the deconstruction table's tag needs
 * and used not to have:</p>
 *
 * <ul>
 *   <li>{@link #aspectTagSeamCarriesTheOriginalsDrawTagGeometry}: the box is the
 *       original's 16 px, its top-left is the point the original's GUIs passed
 *       minus 8 (GuiArcaneWorkbench.java:65), the six hexagon points are the
 *       original's six literals, the pulse is the original's 0.1..0.5 band with
 *       the affordable override, the amount is the original's
 *       {@code DecimalFormat("#######.##")} over real vis, the glyph anchors are
 *       the original's {@code 32 - sw + x*2} / {@code 32 - fontHeight + y*2}, the
 *       sparkle frames are the original's {@code 16 * (age % 16)}, and the
 *       black-and-white tag runs at the original's 0.8 alpha.</li>
 *   <li>{@link #deconstructionRevealedAspectTravelsTheMenuDataChannel}: the
 *       revealed aspect reaches the screen as the menu's <b>second</b> container
 *       data slot (BD-17), the index channel is total over everything the box can
 *       show, {@code -1} is the original's empty box in both directions, and an
 *       out-of-range index degrades to the empty box instead of throwing — the
 *       state the screen's tag draw and its gated collect click both read.</li>
 *   <li>{@link #deconRevealIsAlwaysInTheIndexDomain}: what the tile itself
 *       reveals ({@code breakItem}, the roll over the reduced primal list) is
 *       always encodable, so the index channel cannot lose an aspect, and the
 *       break still consumes its item.</li>
 * </ul>
 *
 * <p><b>Not proved here, and the card says so:</b> the drawn quad, its tint, the
 * 0.5-scale glyph, the sprite-vs-flat-square branch of the overlay gate (D-004)
 * and the sparkle's sheet frame are pixels on a client; the game-test harness is a
 * dedicated server and never fires {@code RegisterMenuScreensEvent}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AspectTagGameTest {

    private AspectTagGameTest() {
    }

    /** The table family's deconstruction variant (CatalogFurnitureGameTest's own idiom). */
    private static final int VARIANT_DECONSTRUCTION_TABLE = 14;

    // --- helpers ---------------------------------------------------------------

    /** Place the deconstruction table at (1,1,1) and hand back its entity. */
    private static TileDeconstructionTable tableAt(final GameTestHelper helper) {
        final BlockTable tables = CatalogFurniture.BLOCK_TABLE.get();
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, tables.withVariant(VARIANT_DECONSTRUCTION_TABLE));
        final BlockEntity entity = helper.getBlockEntity(pos);
        helper.assertTrue(entity instanceof TileDeconstructionTable,
                "the deconstruction table's variant must create its TileDeconstructionTable entity");
        return (TileDeconstructionTable) entity;
    }

    private static Aspect primal(final String tag) {
        final Aspect aspect = Aspect.getAspect(tag);
        assert aspect != null : "unknown aspect tag " + tag;
        return aspect;
    }

    // --- probes ----------------------------------------------------------------

    @GameTest(template = "empty")
    public static void aspectTagSeamCarriesTheOriginalsDrawTagGeometry(final GameTestHelper helper) {
        // The box: UtilsFX.java:839-842 blits (x,y)..(x+16,y+16), and every caller
        // passes the point minus half that size.
        helper.assertTrue(ContainerLayout.TAG_SIZE == 16,
                "the original's tag quad is 16 px (UtilsFX.java:839)");
        helper.assertTrue(ContainerLayout.TAG_CENTER_OFFSET == -ContainerLayout.TAG_SIZE / 2,
                "the original's GUIs pass the point minus 8 (GuiArcaneWorkbench.java:65)");
        helper.assertTrue(ContainerLayout.tagBoxX(72) == 64 && ContainerLayout.tagBoxY(21) == 13,
                "the tag box for the original's (72,21) point is (64,13)");
        helper.assertTrue(ContainerLayout.WB_ASPECT_CENTER_X.length == 6
                        && ContainerLayout.WB_ASPECT_CENTER_Y.length == 6,
                "the original's vis hexagon is six points");
        final int[] hexX = {72, 24, 24, 72, 120, 120};
        final int[] hexY = {21, 43, 102, 124, 102, 43};
        for (int i = 0; i < hexX.length; i++) {
            helper.assertTrue(ContainerLayout.WB_ASPECT_CENTER_X[i] == hexX[i]
                            && ContainerLayout.WB_ASPECT_CENTER_Y[i] == hexY[i],
                    "hexagon point " + i + " must be the original's (" + hexX[i] + "," + hexY[i] + ")");
            helper.assertTrue(ContainerLayout.aspectBoxX(i) == hexX[i] + ContainerLayout.TAG_CENTER_OFFSET
                            && ContainerLayout.aspectBoxY(i) == hexY[i] + ContainerLayout.TAG_CENTER_OFFSET,
                    "the workbench's box for point " + i + " must be the original's drawTag x,y");
        }

        // The pulse: GuiArcaneWorkbench.java:57 — 0.1..0.5 while the wand is short of
        // that aspect, steady 1.0 when it can pay. (The card body's "sin(age/15)*0.4+0.6"
        // is not this GUI's formula; the 15-period sine is the runic matrix's, and the
        // 0.4/0.6 pair is the aura HUD's — see BD-18.)
        helper.assertTrue(ContainerLayout.aspectAlpha(0, 0, true) == 1.0F,
                "an affordable aspect pulses at a steady 1.0, the original's override");
        helper.assertTrue(Math.abs(ContainerLayout.aspectAlpha(0, 0, false) - 0.3F) < 1.0E-5F,
                "age 0, index 0: sin(0)*0.2-0.2 + 0.5 = 0.3");
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int age = 0; age < 4000; age++) {
            for (int index = 0; index < 6; index++) {
                final float alpha = ContainerLayout.aspectAlpha(age, index, false);
                min = Math.min(min, alpha);
                max = Math.max(max, alpha);
            }
        }
        helper.assertTrue(min >= 0.1F - 1.0E-5F && max <= 0.5F + 1.0E-5F,
                "the pulse must stay inside the original's 0.1..0.5 band, got " + min + ".." + max);
        helper.assertTrue(min < 0.101F && max > 0.499F,
                "the pulse must reach both ends of the original's band, got " + min + ".." + max);
        for (int index = 0; index < 6; index++) {
            helper.assertTrue(Math.abs(ContainerLayout.aspectAlpha(200 + index * 10, 0, false)
                            - ContainerLayout.aspectAlpha(200, index, false)) < 1.0E-5F,
                    "the original offsets the pulse's phase by index*10 ((age + i*10) / 2), so tag "
                            + index + " at age 200 must equal tag 0 at age " + (200 + index * 10));
        }

        // The amount: the original printed `myFormatter.format(amount)` — real vis,
        // "#######.##" (UtilsFX.java:311, 849). 200 centi-vis is 2 vis.
        helper.assertTrue("2".equals(ContainerLayout.tagAmountText(
                        ContainerLayout.displayVisFloat(200))),
                "200 centi-vis must print as the original's \"2\", got "
                        + ContainerLayout.tagAmountText(ContainerLayout.displayVisFloat(200)));
        helper.assertTrue("2.5".equals(ContainerLayout.tagAmountText(
                        ContainerLayout.displayVisFloat(250))),
                "a half vis must print with the original's two decimals, got "
                        + ContainerLayout.tagAmountText(ContainerLayout.displayVisFloat(250)));
        helper.assertTrue("0".equals(ContainerLayout.tagAmountText(0.0F)),
                "a zero amount prints \"0\"; the callers skip a 0-amount tag, as the original did");

        // The glyph anchors: UtilsFX.java:861 in the 0.5-scaled space.
        helper.assertTrue(ContainerLayout.TAG_AMOUNT_ORIGIN == 32
                        && ContainerLayout.TAG_AMOUNT_SCALE == 0.5F
                        && ContainerLayout.TAG_AMOUNT_COLOUR == 0xFFFFFFFF,
                "the glyph is 0.5 scale in 16777215 (UtilsFX.java:847,861)");
        helper.assertTrue(ContainerLayout.tagAmountScaledX(64, 8) == 32 - 8 + 64 * 2,
                "the glyph's x is the original's 32 - sw + x*2");
        helper.assertTrue(ContainerLayout.tagAmountScaledY(13, 8) == 32 - 8 + 13 * 2,
                "the glyph's y is the original's 32 - fontHeight + y*2");
        helper.assertTrue(ContainerLayout.tagBonusScaledX(64, 6) == 8 - 3 + 128
                        && ContainerLayout.tagBonusScaledY(13, 8) == 15 - 8 + 26,
                "the bonus glyph is the original's 8 - sw/2 + a*2 / 15 - fontHeight + y*2 (UtilsFX.java:875-884)");

        // The sparkle: 16 * (ticks % 16) frames of the 256 sheet at v = 80.
        helper.assertTrue(ContainerLayout.tagSparkleFrame(0) == 0
                        && ContainerLayout.tagSparkleFrame(1) == 16
                        && ContainerLayout.tagSparkleFrame(15) == 240
                        && ContainerLayout.tagSparkleFrame(16) == 0,
                "the bonus sparkle walks 16 frames of the sheet and wraps (UtilsFX.java:869)");
        helper.assertTrue(ContainerLayout.TAG_SPARKLE_V == 80
                        && ContainerLayout.TAG_SPARKLE_OFFSET == -4
                        && ContainerLayout.TAG_SPARKLE_SHEET == 256,
                "the sparkle is 4 px up-left of the tag, v = 80, on the 256 particle sheet");

        // The bw tag — the 3-argument overload's variant, the in-world tile renderers'
        // (TileAlembicRenderer/TileJarRenderer): 0.1 tone at 0.8 alpha.
        helper.assertTrue(Math.abs(ContainerLayout.tagAlphaOf(1.0F, true) - 0.8F) < 1.0E-6F
                        && ContainerLayout.tagAlphaOf(1.0F, false) == 1.0F,
                "the bw tag draws at alpha * 0.8 (UtilsFX.java:828) and the non-bw tag at the caller's alpha");
        helper.assertTrue(ContainerLayout.tagTint(0xFF55AA, 1.0F, true) == 0xCC1A1A1A,
                "the bw tint is the original's glColor4f(0.1F, 0.1F, 0.1F, 0.8F): tone 26, alpha 204");
        helper.assertTrue(ContainerLayout.tagTint(0xFF55AA, 0.5F, false) == 0x80FF55AA,
                "a colour tag keeps the aspect's own colour at the caller's alpha (0.5 -> 128)");
        // GuiDeconstructionTable.java:39 calls the SIX-argument overload, which forwards
        // (771, 1.0F, false) (UtilsFX.java:803) — the box is the aspect's full colour at
        // full alpha, NOT the tile renderers' black-and-white tag.
        helper.assertTrue(ContainerLayout.tagTint(0xFF55AA, 1.0F, false) == 0xFFFF55AA,
                "the deconstruction box draws bw = false: the aspect's own colour at alpha 1.0");

        // The deconstruction table's box: (64,48), 16 px, i.e. the original's tag box.
        helper.assertTrue(ContainerLayout.DECON_ASPECT_X == 64
                        && ContainerLayout.DECON_ASPECT_Y == 48
                        && ContainerLayout.DECON_ASPECT_SIZE == ContainerLayout.TAG_SIZE,
                "the decon box is the original's drawTag(64, 48, ...) 16 px tag (GuiDeconstructionTable.java:39)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deconstructionRevealedAspectTravelsTheMenuDataChannel(final GameTestHelper helper) {
        final TileDeconstructionTable table = tableAt(helper);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final DeconstructionTableMenu menu =
                new DeconstructionTableMenu(1, player.getInventory(), table);

        // The channel: 1.7.10's screen read the tile; this screen gets the menu, and the
        // menu's container data is the only path (BD-17). Slot 0 stays the break timer.
        helper.assertTrue(menu.dataSlotCount() == 2,
                "the menu must sync the break timer AND the revealed aspect, got "
                        + menu.dataSlotCount() + " data slots");
        helper.assertTrue(menu.dataValue(DeconstructionTableMenu.DATA_BREAK_TIME) == table.breaktime,
                "data slot 0 is still the break timer the bar reads");
        helper.assertTrue(menu.dataValue(DeconstructionTableMenu.DATA_ASPECT) == -1,
                "an empty table shows the original's empty box (-1)");
        helper.assertTrue(menu.getRevealedAspect() == null,
                "an empty table has no aspect to draw");

        // Reveal one, the way the tile does: a primal from its reduced tag list.
        table.aspect = primal("ignis");
        final int wire = menu.dataValue(DeconstructionTableMenu.DATA_ASPECT);
        helper.assertTrue(wire == DeconstructionTableMenu.encodeAspectIndex(table.aspect) && wire >= 0,
                "data slot 1 must carry ignis's primal index, got " + wire);
        helper.assertTrue(menu.getRevealedAspect() == table.aspect,
                "the screen reads the revealed aspect through the menu");

        // What the client's ContainerData#set writes back from that wire value.
        helper.assertTrue(DeconstructionTableMenu.decodeAspectIndex(wire) == table.aspect,
                "the index must round-trip to the same aspect the renderer tints");
        for (final Aspect primal : Aspect.getPrimalAspects()) {
            final int index = DeconstructionTableMenu.encodeAspectIndex(primal);
            helper.assertTrue(index >= 0 && DeconstructionTableMenu.decodeAspectIndex(index) == primal,
                    "the index channel must be total over " + primal.getTag());
        }
        helper.assertTrue(DeconstructionTableMenu.encodeAspectIndex(null) == -1,
                "no aspect is -1 on the wire");
        helper.assertTrue(DeconstructionTableMenu.decodeAspectIndex(-1) == null,
                "-1 is the original's empty box on the client");
        helper.assertTrue(DeconstructionTableMenu.decodeAspectIndex(9999) == null,
                "an out-of-range index is the empty box, not a crash");
        helper.assertTrue(DeconstructionTableMenu.decodeAspectIndex(
                        Aspect.getPrimalAspects().size()) == null,
                "one past the last primal is the empty box");

        // The gated collect click (BD-17) reads this same state, so an empty box sends
        // nothing and a loaded box is clickable: the gate is exactly getRevealedAspect().
        helper.assertTrue(menu.getRevealedAspect() != null,
                "a loaded box passes the screen's click gate");
        table.aspect = null;
        helper.assertTrue(menu.dataValue(DeconstructionTableMenu.DATA_ASPECT) == -1
                        && menu.getRevealedAspect() == null,
                "collecting empties the box in the same channel the screen reads");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deconRevealIsAlwaysInTheIndexDomain(final GameTestHelper helper) {
        final TileDeconstructionTable table = tableAt(helper);
        final ItemStack coalOre = new ItemStack(Items.COAL_ORE);
        coalOre.setCount(64);

        // The shipped essentia table gives coal ore terra 1 + potentia 2 + ignis 1
        // (data/thaumcraft/essentia/tags.json) — every one of them a primal, so the
        // reduced list the tile rolls over is in the index channel by definition.
        helper.assertTrue(table.canAccept(coalOre),
                "an aspect-bearing item must be accepted into the table's slot");
        table.setItem(0, coalOre);

        int reveals = 0;
        for (int attempt = 0; attempt < 400 && !table.getItem(0).isEmpty(); attempt++) {
            table.breakItem();
            if (table.aspect != null) {
                reveals++;
                helper.assertTrue(DeconstructionTableMenu.encodeAspectIndex(table.aspect) >= 0,
                        "the tile revealed " + table.aspect.getTag()
                                + ", which the menu's index channel cannot carry");
                table.aspect = null;
            }
        }
        helper.assertTrue(table.getItem(0).isEmpty(),
                "each break must consume one of the seeded stack, leaving "
                        + table.getItem(0).getCount());
        helper.assertTrue(reveals <= 64, "a reveal must come from a consumed item");
        helper.assertTrue(table.aspect == null || DeconstructionTableMenu.encodeAspectIndex(table.aspect) >= 0,
                "whatever the table is holding right now must be encodable ("
                        + (table.aspect == null ? "empty" : table.aspect.getTag()) + ")");
        // The domain is the list the tile rolls from, and the encoding is positional in
        // it. Aspect.getPrimalAspects() is isPrimal() = "the component list is not
        // exactly two entries" (aspect/Aspect.java:66-68, the original's own test), and
        // in this port that resolves to EXACTLY the original's six, in the original's
        // declaration order: aer, terra, ignis, aqua, ordo, perditio
        // (reference/decompiled/thaumcraft/thaumcraft/api/aspects/Aspect.java:17-22 —
        // potentia is a compound of ORDER + FIRE, line 31). The port registers from
        // AspectDefinitions, which is extracted from that same file, so the two lists
        // agree by construction, and what the wire needs is exactly that agreement.
        final List<String> rollDomain = Aspect.getPrimalAspects().stream().map(Aspect::getTag).toList();
        helper.assertTrue(List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio").equals(rollDomain),
                "the primal domain must be the original's six (Aspect.java:17-22) in its declaration "
                        + "order, got " + rollDomain);
        helper.assertTrue(rollDomain.size() >= 6
                        && Aspect.getPrimalAspects().stream().allMatch(a ->
                                DeconstructionTableMenu.decodeAspectIndex(DeconstructionTableMenu.encodeAspectIndex(a)) == a),
                "every aspect in the roll domain must round-trip through the index channel");
        helper.succeed();
    }
}
