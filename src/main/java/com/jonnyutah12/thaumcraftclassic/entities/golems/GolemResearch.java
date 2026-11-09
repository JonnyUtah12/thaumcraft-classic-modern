package com.jonnyutah12.thaumcraftclassic.entities.golems;

import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;

/**
 * The GOLEMANCY research gate for the eight golem materials
 * (content-mapping.csv {@code thaumcraft:golemancy}, kind=research_category).
 *
 * <p>The shipped research graph already carries one entry per material under
 * category {@code GOLEMANCY} — {@code GOLEMSTRAW}, {@code GOLEMWOOD},
 * {@code GOLEMTALLOW}, {@code GOLEMCLAY}, {@code GOLEMFLESH},
 * {@code GOLEMSTONE}, {@code GOLEMIRON}, {@code GOLEMTHAUMIUM} — plus 31 more
 * GOLEMANCY entries (cores, upgrades, the bell, the tiny decorations,
 * advanced golems, the fetter, the hungry chest, the traveling trunk). This
 * class only maps code to those keys; it authors and renames nothing
 * (TC-M3-01 shipped the graph, 201 entries / 6 categories, and this card
 * leaves it byte-identical).</p>
 *
 * <p>The original is {@code ConfigResearch.initializeResearch}: each material's
 * entry key is {@code "GOLEM" + material name}, and the crucible recipes that
 * produce the matching placer carry that key as their {@code research} field —
 * which is where the gate is actually enforced
 * ({@code CrucibleRecipe.researchUnlocked} → {@code ResearchManager
 * .isResearchComplete}). {@code ItemGolemPlacer} itself has <b>no</b> research
 * check in 4.2.3.5, so this card deliberately does not add one: a placement
 * gate the original does not have would be an invented behavioural deviation.
 * The gate is proven against the shipped data by
 * {@code GolemContentGameTest} (each material's crucible row is live in the
 * RecipeManager, names the material's entry, and refuses a thrower who lacks
 * it).</p>
 */
public final class GolemResearch {

    /** Category key of every golem entry (original {@code ConfigResearch} category list). */
    public static final String CATEGORY = "GOLEMANCY";

    /** Prefix of the eight per-material entries. */
    public static final String MATERIAL_PREFIX = "GOLEM";

    private GolemResearch() {
    }

    /** The shipped graph entry key for a material: {@code STRAW → GOLEMSTRAW}. */
    public static String researchKey(final EnumGolemType material) {
        return MATERIAL_PREFIX + material.name();
    }

    /**
     * Original {@code ResearchManager.isResearchComplete(username, key)} for a
     * material — the same gate the crucible recipe rows apply.
     */
    public static boolean isUnlocked(final PlayerKnowledgeData knowledge, final EnumGolemType material) {
        return ResearchManager.isResearchComplete(knowledge, researchKey(material));
    }

    /** True when the material's own entry is the banished/absent kind of key (never in this graph). */
    public static boolean isNoteKey(final EnumGolemType material) {
        return researchKey(material).startsWith("@");
    }
}
