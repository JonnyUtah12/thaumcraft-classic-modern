package com.jonnyutah12.thaumcraftclassic.api.wands;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.wands.VisCostRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * A wand cap (TC-M6-01): port of {@code thaumcraft.api.wands.WandCap}
 * ({@code api/wands/WandCap.java}) — the original's registration record for a
 * cap, minus the item stack it held.
 *
 * <p>Original fields preserved 1:1: {@code tag}, {@code baseCostModifier},
 * {@code specialCostModifierAspects}, {@code specialCostModifier},
 * {@code craftCost} and the built texture path
 * ({@code textures/models/wand_cap_<tag>.png}, here {@code models/wand_cap_<tag>}
 * because 1.21.1 resolves {@code textures/} from the resource pack root —
 * consumed by the wand's client renderer, TC-M7-01). The original's
 * {@code ItemStack item} field becomes {@link #itemId()} + {@link #meta()}
 * because no item holder can be dereferenced while the registration tables are
 * being built (items are registered later in the mod bus's lifecycle);
 * {@code wands.WandComponents} rebuilds the stack on demand.</p>
 *
 * <p>{@code specialCostModifierAspects} is kept as aspect <em>tags</em>: the
 * tables are built during {@code RegisterEvent} wiring, before the
 * {@code thaumcraft:aspect} registry exists, so {@code Aspect} objects cannot
 * be resolved yet. {@link #costProfile()} resolves them lazily into the
 * TC-M4-02 cost-engine record.</p>
 *
 * <p>The original never assigned {@code specialCostModifier} in the
 * two-argument constructor, so a cap without special aspects carries
 * {@code 0.0F} there — preserved, and never read in that case
 * ({@code ItemWandCasting.getConsumptionModifier} only reaches it when the
 * aspect is in the special list).</p>
 */
public record WandCap(String tag,
                      float baseCostModifier,
                      List<String> specialCostModifierAspectTags,
                      float specialCostModifier,
                      int craftCost,
                      ResourceLocation itemId,
                      int meta) {

    /** Original {@code WandCap.getResearch()}: {@code "CAP_" + tag}. */
    public String research() {
        return "CAP_" + this.tag;
    }

    /** Original {@code getTexture()}, repathed for 1.21.1 (see the class javadoc). */
    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "models/wand_cap_" + this.tag);
    }

    /** Whether the original's special-aspect branch can trigger for this cap. */
    public boolean hasSpecialCostModifiers() {
        return !this.specialCostModifierAspectTags.isEmpty();
    }

    /**
     * The cost-relevant half of this cap in the shape the TC-M4-02 cost engine
     * consumes ({@link VisCostRules.WandCapCost}). {@code specialCostModifierAspects}
     * resolves to {@code null} when the cap has none, exactly as the original
     * left the field.
     */
    public VisCostRules.WandCapCost costProfile() {
        List<Aspect> special = null;
        if (!this.specialCostModifierAspectTags.isEmpty()) {
            special = new ArrayList<>(this.specialCostModifierAspectTags.size());
            for (final String aspectTag : this.specialCostModifierAspectTags) {
                final Aspect aspect = Aspect.getAspect(aspectTag);
                if (aspect != null) {
                    special.add(aspect);
                }
            }
        }
        return new VisCostRules.WandCapCost(this.baseCostModifier, special, this.specialCostModifier);
    }
}
