package com.jonnyutah12.thaumcraftclassic.menus;

import com.jonnyutah12.thaumcraftclassic.api.IScribeTools;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ink gate of the research-note container, driven with a synthetic
 * scribing tool (TC-M3-04-s-iii-b).
 *
 * <p>The game harness cannot reach these arms: the port ships no item that
 * implements {@link IScribeTools} (the scribing tool is its own catalog row),
 * so in a game test the scribe slot can never hold ink and the write arm is
 * closed before it starts. A plain-JVM probe can: 1.21.1's durability is a
 * data component, so a locally built item with a {@code durability(...)}
 * property exercises the same arms the shipped code reads.</p>
 *
 * <p>The rule under test is {@code ResearchManager.consumeInkFromTable}
 * ({@code ResearchManager.java:220-230}): the stack must be an
 * {@code IScribeTools}, ink exists while {@code damage < maxDamage}, and the
 * committed call spends exactly one point.</p>
 */
class ResearchNoteInkGateTest {

    /** A scribing tool for the probe: the marker plus vanilla durability. */
    private static final class ProbeScribe extends Item implements IScribeTools {
        ProbeScribe(final int maxDamage) {
            super(new Item.Properties()
                    .stacksTo(1)
                    .durability(maxDamage));
        }
    }

    /** An ordinary item: durability without the marker must not count as ink. */
    private static final class ProbePlain extends Item {
        ProbePlain() {
            super(new Item.Properties()
                    .stacksTo(1)
                    .durability(16));
        }
    }

    @Test
    void noToolMeansNoInk() {
        assertFalse(ResearchNoteMenu.hasInk(ItemStack.EMPTY),
                "an empty scribe slot reported ink: the original's gate is contents[0] instanceof IScribeTools");
        assertFalse(ResearchNoteMenu.consumeInk(ItemStack.EMPTY, true),
                "the commit arm returned true with no tool in the slot");
        assertEquals(0, ResearchNoteMenu.inkRemaining(ItemStack.EMPTY),
                "the plate showed ink with no tool");
    }

    @Test
    void aDurabilityItemWithoutTheMarkerIsNotInk() {
        final ItemStack plain = new ItemStack(new ProbePlain());
        assertFalse(ResearchNoteMenu.hasInk(plain),
                "a durability item that is not an IScribeTools counted as ink (ResearchManager.java:221)");
        assertEquals(0, ResearchNoteMenu.inkRemaining(plain),
                "a non-scribe's remaining durability leaked into the plate's ink value");
    }

    @Test
    void aFreshScribeHasInkAndTheCommitSpendsExactlyOnePoint() {
        final ItemStack scribe = new ItemStack(new ProbeScribe(16));
        assertTrue(ResearchNoteMenu.hasInk(scribe), "a fresh scribing tool has no ink");
        assertTrue(ResearchNoteMenu.consumeInk(scribe, false),
                "the probe call (doit = false) refused ink on a fresh tool");
        assertEquals(0, scribe.getDamageValue(),
                "the probe call (doit = false) spent durability: ResearchManager.java:222 gates the spend");

        assertTrue(ResearchNoteMenu.consumeInk(scribe, true), "the committed write refused ink");
        assertEquals(1, scribe.getDamageValue(),
                "the committed write spent " + scribe.getDamageValue() + ", the original spends 1 (:223)");
        assertEquals(15, ResearchNoteMenu.inkRemaining(scribe),
                "the plate's ink value did not follow the spend");
    }

    @Test
    void aSpentScribeIsOutOfInkAndCannotBeChargedFurther() {
        final ItemStack scribe = new ItemStack(new ProbeScribe(3));
        scribe.setDamageValue(3);
        assertFalse(ResearchNoteMenu.hasInk(scribe),
                "a tool at damage == maxDamage still reported ink (the gate is <, not <=, :221)");
        assertFalse(ResearchNoteMenu.consumeInk(scribe, true),
                "the commit arm charged a dry tool");
        assertEquals(3, scribe.getDamageValue(), "the commit arm pushed the tool past its max damage");
        assertEquals(0, ResearchNoteMenu.inkRemaining(scribe), "the plate showed ink on a dry tool");
    }

    @Test
    void inkRunsOutExactlyAtTheMaxDamageThescribeToolsCarries() {
        final int capacity = 5;
        final ItemStack scribe = new ItemStack(new ProbeScribe(capacity));
        int writes = 0;
        while (ResearchNoteMenu.consumeInk(scribe, true)) {
            writes++;
            assertTrue(writes <= capacity + 1,
                    "the ink gate charged more writes than the tool's durability: " + writes);
        }
        assertEquals(capacity, writes,
                "a tool of maxDamage " + capacity + " paid " + writes
                        + " writes; the original's damage<maxDamage gate pays exactly " + capacity);
    }
}
