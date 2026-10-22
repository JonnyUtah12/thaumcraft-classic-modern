package com.jonnyutah12.thaumcraftclassic.nodes;

import java.util.Locale;

/**
 * Vis node modifiers (TC-M5-01). 1:1 port of {@code thaumcraft.api.nodes.NodeModifier}
 * (BRIGHT, PALE, FADING — the three values pinned by docs/content-mapping.csv rows
 * {@code Thaumcraft:nodemodifier.*}).
 *
 * <p>Registered as objects in the {@code thaumcraft:node_modifier} custom registry
 * ({@code registration/ModNodeDefs}); registry id {@code thaumcraft:nodemodifier.<name>},
 * short store id {@code "bright"} etc. A node may have no modifier — that is
 * represented by {@code null}, as in the original.</p>
 *
 * <p>Regeneration-rate effects (BRIGHT 400 / PALE 900 / FADING 0 ticks,
 * docs/balance-values.md §6.2) belong to TC-M5-02.</p>
 */
public enum NodeModifier {
    BRIGHT,
    PALE,
    FADING;

    /** Short store id, e.g. {@code "bright"} — the {@code WorldMagicData.Node.modifier} value. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Registry path, e.g. {@code "nodemodifier.bright"}. */
    public String registryPath() {
        return "nodemodifier." + id();
    }

    /** Lenient lookup; unknown ids read as "no modifier" (null), as the original absent field does. */
    public static NodeModifier fromId(final String id) {
        if (id != null) {
            for (final NodeModifier modifier : values()) {
                if (modifier.id().equals(id)) {
                    return modifier;
                }
            }
        }
        return null;
    }
}
