package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Enchantment id holder (TC-M2-01 registration architecture).
 *
 * <p>Not a {@code DeferredRegister}: in 1.21.1 enchantments are a datapack
 * registry ({@code minecraft:enchantment} is loaded from
 * {@code data/<ns>/enchantment/*.json}) and is not part of
 * {@code BuiltInRegistries}, so NeoForge never fires a {@code RegisterEvent}
 * for it — a DeferredRegister for it would silently never populate (verified
 * against NeoForge 21.1.259 {@code GameData.postRegisterEvents}).</p>
 *
 * <p>The id contract is therefore a set of {@link ResourceKey} constants plus
 * the JSON payloads; TC-M6-05-s-e1c shipped the two enchantments the original
 * actually registers (Haste, Repair — per TC-M0-04 the other enchantment
 * classes are dead code) as
 * {@code data/thaumcraft/enchantment/{ench_haste,ench_repair}.json}, which is
 * what makes {@code recipe.201}/{@code recipe.202.infusion_enchantment.json}
 * resolvable. The effect bodies (mend-from-XP, speed) are the follow-up slice.
 * The registration harness resolves kind=enchantment rows through the level's
 * dynamic registry lookup.</p>
 */
public final class ModEnchantments {

    /**
     * Repair (original {@code ThaumcraftApi.enchantRepair}): the payload of
     * {@code recipe.201.infusion_enchantment.json}.
     */
    public static final ResourceKey<Enchantment> REPAIR = id("ench_repair");

    /**
     * Haste (original {@code ThaumcraftApi.enchantHaste}): the payload of
     * {@code recipe.202.infusion_enchantment.json}.
     */
    public static final ResourceKey<Enchantment> HASTE = id("ench_haste");

    /** Canonical id for an enchantment entry (id contract: content-mapping.csv new_id). */
    static ResourceKey<Enchantment> id(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path));
    }

    private ModEnchantments() {
    }
}
