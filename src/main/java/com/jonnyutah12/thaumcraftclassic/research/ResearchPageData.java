package com.jonnyutah12.thaumcraftclassic.research;

import com.google.gson.annotations.SerializedName;

/**
 * One page of a research entry, mirroring the original
 * {@code thaumcraft.api.research.ResearchPage} shape: a page type plus the
 * payload fields that type uses (all others null). Page type names are the
 * original {@code PageType} enum constants lowercased.
 *
 * <p>Recipe references are the original {@code ConfigResearch.recipes} map
 * keys (validated against ConfigRecipes.java at extraction time). The
 * {@code inline:} prefix marks recipe objects the original constructed
 * inline in ConfigResearch.java instead of registering them in the map
 * (sceptre recipes, runic augmentation) — see docs/research-graph-notes.md.</p>
 *
 * @param type        page type
 * @param textKey     original lang key for page text (text/text_concealed/image)
 * @param textEnUs    original en_US.lang page text, verbatim (ARR quotation)
 * @param requires    research key gating a text_concealed page
 * @param recipe      single recipes-map key (single-recipe pages)
 * @param recipes     recipes-map keys in display order (array pages)
 * @param recipeGroup recipes-map key holding a recipe list (compound pages)
 * @param input       input item for smelting pages
 * @param aspects     aspect display list for aspect pages
 */
public record ResearchPageData(
        Type type,
        String textKey,
        String textEnUs,
        String requires,
        String recipe,
        java.util.List<String> recipes,
        String recipeGroup,
        ResearchEntryData.ItemRef input,
        java.util.List<ResearchEntryData.AspectTag> aspects) {

    /** Original ResearchPage.PageType, lowercased for the data format. */
    public enum Type {
        @SerializedName("text") TEXT,
        @SerializedName("text_concealed") TEXT_CONCEALED,
        @SerializedName("image") IMAGE,
        @SerializedName("aspects") ASPECTS,
        @SerializedName("crucible_crafting") CRUCIBLE_CRAFTING,
        @SerializedName("arcane_crafting") ARCANE_CRAFTING,
        @SerializedName("normal_crafting") NORMAL_CRAFTING,
        @SerializedName("infusion_crafting") INFUSION_CRAFTING,
        @SerializedName("compound_crafting") COMPOUND_CRAFTING,
        @SerializedName("infusion_enchantment") INFUSION_ENCHANTMENT,
        @SerializedName("smelting") SMELTING;
    }
}
