package com.jonnyutah12.thaumcraftclassic.research;

import java.util.List;

/**
 * One research entry of the original Thaumcraft 4.2.3.5 research graph
 * (a {@code new ResearchItem(...).registerResearchItem()} statement in the
 * decompiled ConfigResearch.java; upstream source file name
 * thaumcraft/common/lib/research/Research.java).
 *
 * <p>Data comes from {@code data/thaumcraft/research/entries.json},
 * extracted mechanically by scripts/extract_research_graph.py; method and
 * caveats in docs/research-graph-notes.md. Field semantics mirror the
 * original API exactly:</p>
 *
 * <ul>
 *   <li>{@code complexity} is the original constructor "complex" value after
 *   the constructor's 1..3 clamp; {@code complexityRaw} records the
 *   pre-clamp value when it differed (the original passed 0 for 25 stub
 *   entries, which the constructor raised to 1).</li>
 *   <li>{@code parentsHidden} are prerequisites that are not drawn as lines
 *   in the original browser; {@code siblings} are display-linked peers.</li>
 *   <li>{@code triggers} are the original item/entity/aspect discovery
 *   triggers ("explore triggers").</li>
 *   <li>{@code warp} is the warp applied on completion
 *   ({@code ThaumcraftApi.addWarpToResearch}).</li>
 * </ul>
 *
 * @param key           original research key (the registry key)
 * @param category      owning category key
 * @param column        browser grid column
 * @param row           browser grid row
 * @param complexity    effective point cost after the original clamp (null for virtual stubs)
 * @param complexityRaw pre-clamp constructor value, only when it differed
 * @param tags          essentia tags shown on the entry
 * @param icon          entry icon (item, texture, none, or unresolved)
 * @param flags         original boolean setter flags (sparse; absent = false)
 * @param parents       prerequisite research keys (drawn)
 * @param parentsHidden prerequisite research keys (hidden)
 * @param siblings      sibling display links
 * @param triggers      discovery triggers
 * @param warp          warp points applied on completion
 * @param nameKey       original lang key ({@code tc.research_name.<key>})
 * @param nameEnUs      original en_US.lang name, verbatim; null when the original had none (UNKNOWN)
 * @param textKey       original lang key ({@code tc.research_text.<key>})
 * @param textEnUs      original en_US.lang hover text, verbatim; null when the original had none (UNKNOWN)
 * @param pages         research pages in display order
 */
public record ResearchEntryData(
        String key,
        String category,
        int column,
        int row,
        Integer complexity,
        Integer complexityRaw,
        List<AspectTag> tags,
        Icon icon,
        Flags flags,
        List<String> parents,
        List<String> parentsHidden,
        List<String> siblings,
        Triggers triggers,
        int warp,
        String nameKey,
        String nameEnUs,
        String textKey,
        String textEnUs,
        List<ResearchPageData> pages) {

    /** An essentia tag: aspect tag name, original Aspect constant, amount. */
    public record AspectTag(String aspect, String constant, int amount) {
    }

    /**
     * An item reference. {@code item} is the original registered name with
     * namespace ({@code Thaumcraft:FocusFire}, {@code minecraft:wool});
     * {@code meta} 32767 is the original wildcard "any metadata";
     * {@code srgField} records the obfuscated 1.7.10 field the vanilla
     * reference came from (resolved via MCP stable_11 mappings).
     */
    public record ItemRef(String item, String srgField, int meta) {
    }

    /** Entry icon: kind item|texture|none|unknown. */
    public record Icon(String kind, String item, String srgField, Integer meta, String texture, String raw) {
    }

    /** Discovery triggers (original setItemTriggers/setEntityTriggers/setAspectTriggers). */
    public record Triggers(List<ItemRef> items, List<String> entities, List<String> aspects) {
    }

    /**
     * The original ResearchItem boolean setter flags. Absent JSON fields
     * deserialize to false, matching the original field defaults.
     */
    public record Flags(
            boolean special,
            boolean secondary,
            boolean round,
            boolean stub,
            boolean virtual,
            boolean concealed,
            boolean hidden,
            boolean lost,
            boolean autoUnlock) {
    }
}
