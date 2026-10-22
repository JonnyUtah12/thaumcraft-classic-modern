package com.jonnyutah12.thaumcraftclassic.nodes;

import java.util.Locale;

/**
 * Vis node types (TC-M5-01). 1:1 port of {@code thaumcraft.api.nodes.NodeType}
 * (NORMAL, UNSTABLE, DARK, TAINTED, HUNGRY, PURE — the six values pinned by
 * docs/content-mapping.csv rows {@code Thaumcraft:nodetype.*}).
 *
 * <p>The constants are registered as objects in the {@code thaumcraft:node_type}
 * custom registry (see {@code registration/ModNodeDefs}); the registry id is
 * {@code thaumcraft:nodetype.<name>}. The short {@link #id()} ("normal", …) is
 * what {@code WorldMagicData.Node.type} stores, matching the store's existing
 * short-string convention (docs/architecture/persistence.md).</p>
 *
 * <p>Behaviour semantics (production, depletion, hard-mode effects) belong to
 * TC-M5-02; this enum carries identity only, exactly like the original.</p>
 */
public enum NodeType {
    NORMAL,
    UNSTABLE,
    DARK,
    TAINTED,
    HUNGRY,
    PURE;

    /** Short store id, e.g. {@code "dark"} — the {@code WorldMagicData.Node.type} value. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Registry path, e.g. {@code "nodetype.dark"} — the {@code thaumcraft:node_type} entry id. */
    public String registryPath() {
        return "nodetype." + id();
    }

    /** Lenient lookup: unknown/null ids fall back to NORMAL, mirroring the store's lenient codec. */
    public static NodeType fromId(final String id) {
        if (id != null) {
            for (final NodeType type : values()) {
                if (type.id().equals(id)) {
                    return type;
                }
            }
        }
        return NORMAL;
    }
}
