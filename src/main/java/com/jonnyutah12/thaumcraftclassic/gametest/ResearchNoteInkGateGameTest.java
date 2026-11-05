package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.menus.ResearchNoteMenu;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The ink gate of the research-note menu, driven edge-by-edge
 * (TC-M3-04-s-iii-b; the shipped live path is
 * {@code ResearchNoteMenuGameTest.aFundedWriteSpendsPoolInkAndCompletesTheNote}).
 *
 * <p>The rule under test is {@code ResearchManager.consumeInkFromTable}
 * ({@code ResearchManager.java:220-230}): the stack must be an
 * {@code IScribeTools}, ink exists while {@code damage < maxDamage} — the
 * comparison is strict, so a tool spent to EXACTLY max damage is dry — and the
 * committed call spends exactly one point while the probe call
 * ({@code doit = false}) spends nothing. The gate arithmetic (a tool of
 * maxDamage N pays exactly N writes) is what this harness pins; the game
 * harness only ever stages a fresh inkwell.</p>
 *
 * <p>These probes live in the game harness, not the unit suite: 1.21.1's
 * {@code Item} constructor registers an intrusive holder
 * ({@code Item.java:67}), so a synthetic scribing tool cannot be constructed
 * once the item registry is frozen, and the marker check needs a real
 * {@code IScribeTools} implementor. The shipped {@code items/ItemInkwell}
 * (TC-M3-04-s-v) is that implementor, and 1.21.1 durability is a component
 * pair, so the probes carry MAX_DAMAGE/DAMAGE on the stack and drive the exact
 * arithmetic the shipped 100-point window cannot stage.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchNoteInkGateGameTest {

    /** The shipped inkwell's own durability window ({@code ItemInkwell}, properties 100). */
    private static final int SHIPPED_DURABILITY = 100;

    private ResearchNoteInkGateGameTest() {
    }

    @GameTest(template = "empty")
    public static void noToolMeansNoInk(final GameTestHelper helper) {
        helper.assertTrue(!ResearchNoteMenu.hasInk(ItemStack.EMPTY),
                "an empty scribe slot reported ink: the original's gate is contents[0] instanceof IScribeTools");
        helper.assertTrue(!ResearchNoteMenu.consumeInk(ItemStack.EMPTY, true),
                "the commit arm returned true with no tool in the slot");
        helper.assertTrue(ResearchNoteMenu.inkRemaining(ItemStack.EMPTY) == 0,
                "the plate showed ink with no tool");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aDurabilityItemWithoutTheMarkerIsNotInk(final GameTestHelper helper) {
        final ItemStack plain = plainWithDurability();
        helper.assertTrue(plain.getMaxDamage() == 16,
                "the probe could not carry a durability window on a stone stack: " + plain.getMaxDamage());
        helper.assertTrue(!ResearchNoteMenu.hasInk(plain),
                "a durability item that is not an IScribeTools counted as ink (ResearchManager.java:221)");
        helper.assertTrue(ResearchNoteMenu.inkRemaining(plain) == 0,
                "a non-scribe's remaining durability leaked into the plate's ink value");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aFreshScribeHasInkAndTheCommitSpendsExactlyOnePoint(final GameTestHelper helper) {
        final ItemStack scribe = scribe(16, 0);
        helper.assertTrue(ResearchNoteMenu.hasInk(scribe), "a fresh scribing tool has no ink");
        helper.assertTrue(ResearchNoteMenu.consumeInk(scribe, false),
                "the probe call (doit = false) refused ink on a fresh tool");
        helper.assertTrue(scribe.getDamageValue() == 0,
                "the probe call (doit = false) spent durability: ResearchManager.java:222 gates the spend");

        helper.assertTrue(ResearchNoteMenu.consumeInk(scribe, true), "the committed write refused ink");
        helper.assertTrue(scribe.getDamageValue() == 1,
                "the committed write spent " + scribe.getDamageValue() + ", the original spends 1 (:223)");
        helper.assertTrue(ResearchNoteMenu.inkRemaining(scribe) == 15,
                "the plate's ink value is " + ResearchNoteMenu.inkRemaining(scribe) + ", the window is 16");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aSpentScribeIsOutOfInkAndCannotBeChargedFurther(final GameTestHelper helper) {
        final ItemStack scribe = scribe(3, 3);
        helper.assertTrue(scribe.getDamageValue() == 3,
                "the probe could not spend the tool to its max: " + scribe.getDamageValue());
        helper.assertTrue(!ResearchNoteMenu.hasInk(scribe),
                "a tool at damage == maxDamage still reported ink (the gate is <, not <=, :221)");
        helper.assertTrue(!ResearchNoteMenu.consumeInk(scribe, true),
                "the commit arm charged a dry tool");
        helper.assertTrue(scribe.getDamageValue() == 3,
                "the commit arm pushed the tool past its max damage: " + scribe.getDamageValue());
        helper.assertTrue(ResearchNoteMenu.inkRemaining(scribe) == 0, "the plate showed ink on a dry tool");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inkRunsOutExactlyAtTheMaxDamageTheScribeToolsCarries(final GameTestHelper helper) {
        final int capacity = 5;
        final ItemStack scribe = scribe(capacity, 0);
        int writes = 0;
        while (ResearchNoteMenu.consumeInk(scribe, true)) {
            writes++;
            helper.assertTrue(writes <= capacity + 1,
                    "the ink gate charged more writes than the tool's durability: " + writes);
        }
        helper.assertTrue(writes == capacity,
                "a tool of maxDamage " + capacity + " paid " + writes
                        + " writes; the original's damage<maxDamage gate pays exactly " + capacity);
        helper.assertTrue(ResearchNoteMenu.inkRemaining(scribe) == 0,
                "the plate still shows ink after the window is spent");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theShippedInkwellOpensWithAFullWindow(final GameTestHelper helper) {
        final ItemStack inkwell = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        helper.assertTrue(inkwell.getMaxDamage() == SHIPPED_DURABILITY,
                "the shipped scribing tool's durability window is " + inkwell.getMaxDamage()
                        + ", the original's ItemInkwell.java:23-29 declares 100");
        helper.assertTrue(ResearchNoteMenu.hasInk(inkwell), "a fresh shipped inkwell reported no ink");
        helper.assertTrue(ResearchNoteMenu.inkRemaining(inkwell) == SHIPPED_DURABILITY,
                "the plate shows " + ResearchNoteMenu.inkRemaining(inkwell) + " ink on a fresh inkwell");
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** The shipped scribing tool with an explicit durability window (the component pair). */
    private static ItemStack scribe(final int maxDamage, final int damage) {
        final ItemStack stack = new ItemStack(M3ResearchContent.ITEM_INKWELL.get());
        stack.set(DataComponents.MAX_DAMAGE, maxDamage);
        stack.set(DataComponents.DAMAGE, damage);
        return stack;
    }

    /** An ordinary item: a durability window without the marker must not count as ink. */
    private static ItemStack plainWithDurability() {
        final ItemStack stack = new ItemStack(Items.STONE);
        stack.set(DataComponents.MAX_DAMAGE, 16);
        stack.set(DataComponents.DAMAGE, 0);
        return stack;
    }
}
