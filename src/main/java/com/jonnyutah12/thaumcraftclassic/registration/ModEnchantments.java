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
 * the JSON payloads; TC-M6-05 adds the two enchantments the original actually
 * registers (Haste, Repair — per TC-M0-04 the other enchantment classes are
 * dead code) with original weights/applicability. The registration harness
 * resolves kind=enchantment rows through the level's dynamic registry
 * lookup.</p>
 */
public final class ModEnchantments {

    /** Canonical id for an enchantment entry (id contract: content-mapping.csv new_id). */
    static ResourceKey<Enchantment> id(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path));
    }

    /**
     * Thaumcraft:ench_haste (TC-M6-05). Boots-only speed bonus applied per
     * tick by {@code enchantment/EnchantmentBehaviours} (original
     * {@code EventHandlerEntity.updateSpeed}); JSON payload
     * data/thaumcraft/enchantment/ench_haste.json.
     */
    public static final ResourceKey<Enchantment> ENCHANTMENT_HASTE = id("ench_haste");

    /**
     * Thaumcraft:ench_repair (TC-M6-05). Mends IRepairable items for vis
     * every 40 ticks (original {@code EventHandlerEntity.doRepair}); JSON
     * payload data/thaumcraft/enchantment/ench_repair.json. The original's
     * canCombine exclusion of Unbreaking is carried by the
     * {@code #thaumcraft:exclusive_set/repair} enchantment tag.
     */
    public static final ResourceKey<Enchantment> ENCHANTMENT_REPAIR = id("ench_repair");

    private ModEnchantments() {
    }
}
