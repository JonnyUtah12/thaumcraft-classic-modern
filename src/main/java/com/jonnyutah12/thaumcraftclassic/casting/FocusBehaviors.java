package com.jonnyutah12.thaumcraftclassic.casting;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The focus-tag to spell-body table the TC-M6-02 casting seam dispatches
 * through (TC-M6-03-s fills it).
 *
 * <p>The original resolved a focus's behaviour through virtual dispatch on its
 * item class; the port registers one behaviour per focus tag, so the seam is
 * data. <strong>This slice registers none</strong>: every focus's spell body is
 * TC-M6-03-s, and a cast of a focus with no behaviour reports
 * {@link WandCasting.Status#UNSUPPORTED} <em>without</em> paying vis — an
 * unimplemented focus must not be a way to burn a wand's charge.</p>
 *
 * <p>Registration is open to later cards and to game tests alike (the parent's
 * {@code WandGameTest} pattern registers its ids inside the probe and removes
 * them in a {@code finally}); a test can therefore drive the whole payment path
 * with a probe behaviour and prove the cost chain and refund arithmetic
 * without shipping a real spell.</p>
 */
public final class FocusBehaviors {

    private static final Map<String, FocusBehavior> BY_TAG = new LinkedHashMap<>();

    /** Registers (or replaces) the behaviour for a focus tag. */
    public static void register(final String tag, final FocusBehavior behavior) {
        if (tag == null || behavior == null) {
            throw new IllegalArgumentException("a focus behaviour needs a tag and a body");
        }
        BY_TAG.put(tag, behavior);
    }

    /** The behaviour for a focus tag, or null when the focus's body is not ported yet. */
    public static FocusBehavior get(final String tag) {
        return tag == null ? null : BY_TAG.get(tag);
    }

    /** Whether a focus tag has a spell body. */
    public static boolean isRegistered(final String tag) {
        return get(tag) != null;
    }

    /** The tags with a registered behaviour. */
    public static Set<String> registeredTags() {
        return Set.copyOf(BY_TAG.keySet());
    }

    /** Removes a behaviour — the test-side unregister in a {@code finally}. */
    public static void unregister(final String tag) {
        BY_TAG.remove(tag);
    }

    private FocusBehaviors() {
    }
}
