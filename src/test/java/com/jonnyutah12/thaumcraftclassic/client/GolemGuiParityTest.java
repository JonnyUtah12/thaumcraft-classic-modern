package com.jonnyutah12.thaumcraftclassic.client;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M7-04-m3-a parity gate: every golem-window figure this slice ships is
 * pinned against the <b>decompiled 1.7.10 source text</b> —
 * {@code common/entities/golems/ContainerGolem} (slot binding, player rows,
 * the pause-on-open/resume assignments, the scroll clamp, the button legs, the
 * click-sound tail), {@code client/gui/GuiGolem} (window frame and color-tab
 * blits, the 24x8 scroll arrows and their click sends, the 8x8 toggle pips,
 * the sort rows and the Sorting-core shift, the portrait, the blurb anchor),
 * {@code common/entities/InventoryMob} (the squared-distance 64.0 validity
 * bound) and {@code common/entities/golems/EntityGolemBase} (gui id 0 with
 * the entity id in the x argument, the {@code paused} field). If the original
 * ever moves a number this test names it; the shipped constants live in
 * {@link ContainerLayout} and are asserted in {@code GolemLayoutTest}.
 *
 * <p>The reference resolves by walking up from the test's working directory
 * (the gradle test cwd is not the project dir, and a {@code .worktrees/<task>}
 * checkout sits one level deeper) — the idiom the asset parity tests use. The
 * decompiled 1.7.10 tree is a local reference checkout and is NOT in the repo,
 * so when it is absent the test <b>assumption-skips</b>
 * ({@link org.junit.jupiter.api.Assumptions}) exactly as
 * {@code StructureParityTest} / the asset overlay tests do: on CI its three
 * cases are counted skipped, not failed.</p>
 */
class GolemGuiParityTest {

    private static String read(String suffix) {
        for (String root : new String[]{"../reference", "../../reference", "../../../reference",
                "../../../../reference"}) {
            final Path p = Path.of(root, "decompiled/thaumcraft/thaumcraft", suffix);
            if (Files.exists(p)) {
                try {
                    return Files.readString(p);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        // The reference tree is not checked out (CI): an absent original cannot
        // falsify a parity claim, so skip rather than fail — the same contract
        // StructureParityTest's assumeTrue(Files.exists(...)) establishes.
        Assumptions.assumeTrue(false, "original source not checked out: " + suffix);
        return null;
    }

    private static void requireAll(String haystack, String label, List<String> needles, StringBuilder failures) {
        if (haystack == null) {
            failures.append("reference source missing for ").append(label).append('\n');
            return;
        }
        for (String needle : needles) {
            if (!haystack.contains(needle)) {
                failures.append(label).append(" must contain: ").append(needle).append('\n');
            }
        }
    }

    @Test
    void containerGolemBindsTheShippedSlotsAndPauseLegs() {
        final StringBuilder failures = new StringBuilder();
        requireAll(read("common/entities/golems/ContainerGolem.java"), "ContainerGolem", List.of(
                // bindPlayerInventory: the 84 px grid rows and the 142 hotbar.
                "this.func_75146_a(new Slot(this.playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));",
                "this.func_75146_a(new Slot(this.playerInv, i, 8 + i * 18, 142));",
                // bindGolemInventory: the window slots and their slot classes.
                "100 + a / 2 * 28, 16 + a % 2 * 31",
                "new SlotGhostFluid(this.mobInv, a + this.currentScroll * 6, 100 + a / 2 * 28, 16 + a % 2 * 31)",
                "Math.min(6, slots)",
                // the pause legs and the scroll fields.
                "((EntityGolemBase)this.mobInv.ent).paused = true;",
                "((EntityGolemBase)this.mobInv.ent).paused = false;",
                "this.maxScroll = slots / 6 - 1;",
                // the button legs 66/67, 50..57, the two color ranges, the sound tail.
                "if (button == 66 && this.currentScroll > 0) {",
                "if (button == 67 && this.currentScroll < this.maxScroll) {",
                "if (button >= 50 && button <= 57) {",
                "if (button >= slots && button < slots * 2) {",
                "\"random.click\", 0.2F, 0.8F);"), failures);
        assertTrue(failures.isEmpty(), failures.toString());
    }

    @Test
    void guiGolemDrawsAndSendsAtTheShippedBoxes() {
        final StringBuilder failures = new StringBuilder();
        requireAll(read("client/gui/GuiGolem.java"), "GuiGolem", List.of(
                // the 24x24 window frame (slot anchor four pixels up-left) and
                // the 24x12 color tab strip above it.
                "this.func_73729_b(baseX + 96 + a / 2 * 28, baseY + 12 + a % 2 * 31, 184, typeLoc, 24, 24);",
                "this.func_73729_b(baseX + 96 + a / 2 * 28, baseY + 4 + a % 2 * 31, 72, 168, 24, 12);",
                // the 24x8 scroll arrows at (111,68)/(135,68) and their clicks.
                "this.func_73729_b(baseX + 111, baseY + 68, 0, 200, 24, 8);",
                "this.func_73729_b(baseX + 135, baseY + 68, 24, 200, 24, 8);",
                "int var7x = par1 - (baseX + 111);",
                "var7x = par1 - (baseX + 135);",
                ".func_78756_a(this.field_147002_h.field_75152_c, 66);",
                ".func_78756_a(this.field_147002_h.field_75152_c, 67);",
                // the 8x8 pips: guard (104,5+16row), Fill (62,54), Use (42,40+10row).
                "this.func_73729_b(baseX + 104, baseY + 5, 8, 168, 8, 8);",
                "this.func_73729_b(baseX + 62, baseY + 54, 8, 168, 8, 8);",
                "this.func_73729_b(baseX + 42, baseY + 40, 8, 168, 8, 8);",
                // the sort rows: x = core==10 ? 66 : 180, y = 24/34/44 + shifty.
                "int shiftx = this.golem.getCore() == 10 ? 66 : 180;",
                "this.func_73729_b(baseX + shiftx, baseY + 24 + shifty, 8, 168, 8, 8);",
                "this.func_73729_b(baseX + shiftx, baseY + 44 + shifty, 8, 168, 8, 8);",
                // the 3x-scaled golem portrait centre (51,75) and the blurb anchor (80,22).
                "this.drawGolem(this.field_146297_k, baseX + 51, baseY + 75, 30,",
                "func_74838_a(\"golemblurb.\" + this.golem.getCore() + \".text\"), 80, 22",
                // the button sends 50 (Fill/Use), 51+ax (guard), 55+axx (sort).
                ".func_78756_a(this.field_147002_h.field_75152_c, 50);",
                ".func_78756_a(this.field_147002_h.field_75152_c, 51 + ax);",
                ".func_78756_a(this.field_147002_h.field_75152_c, 55 + axx);"), failures);
        assertTrue(failures.isEmpty(), failures.toString());
    }

    @Test
    void validityAndOpenSiteAreTheOriginals() {
        final StringBuilder failures = new StringBuilder();
        // InventoryMob.canInteractWith: dead -> false, squared distance <= 64.0.
        // Shipped as GolemMenu.stillValid (server-authoritative).
        requireAll(read("common/entities/InventoryMob.java"), "InventoryMob", List.of(
                "entityplayer.func_70068_e(this.ent) <= 64.0"), failures);
        // EntityGolemBase: gui id 0 (the content-mapping gui_id) with the entity
        // id in the x argument (the ported open-menu bytebuf int); the paused
        // field the constructor/resume legs write.
        requireAll(read("common/entities/golems/EntityGolemBase.java"), "EntityGolemBase", List.of(
                "player.openGui(Thaumcraft.instance, 0, this.field_70170_p, this.func_145782_y(), 0, 0);",
                "public boolean paused = false;"), failures);
        assertTrue(failures.isEmpty(), failures.toString());
    }
}
