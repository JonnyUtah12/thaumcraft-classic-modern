package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.IEventBus;

/**
 * TC-M6-05-s-e1b catalog holder: the two enchantments the original registers
 * (Haste, Repair). The other three {@code Enchantment*} classes in the jar are
 * dead code in 4.2.3.5 (TC-M0-04 triage), so this card ships exactly two.
 *
 * <p>Original: {@code thaumcraft.common.lib.enchantment.EnchantmentRepair}
 * (minCost {@code 20 + (lv-1)*10}, maxCost {@code min+50}, weight 2,
 * {@code EnumEnchantmentType.all}, applicability {@code IRepairable} or
 * {@code ItemBook}) and {@code EnchantmentHaste} (minCost {@code 15 +
 * (lv-1)*9}, maxCost {@code min+50}, weight 3, {@code armor}, applicability
 * boots-slot armor, hover harness or {@code ItemBook}). They are referenced
 * from {@code ConfigRecipes.initializeInfusionEnchantmentRecipes} as
 * {@code Enchantment.field_77331_b[ThaumcraftApi.enchantRepair]} — the
 * {@code ThaumcraftApi.enchantRepair/enchantHaste} ints are the 1.7.10
 * enchantment-id config keys, ported as config options by TC-M1 (kept here as
 * the link only).</p>
 *
 * <h2>Registration mechanism (1.21.1, verified on this stack)</h2>
 * <p>In 1.21.1 the ENCHANTMENT registry is a datapack (dynamic) registry:
 * every vanilla enchantment — including all display strings — ships as
 * {@code data/minecraft/enchantment/<id>.json}, and
 * {@code GameData.postRegisterEvents} (NeoForge 21.1.259) only fires
 * {@code RegisterEvent} for registries in {@code BuiltInRegistries.REGISTRY},
 * never for this one. A {@code DeferredRegister} for
 * {@code Registries.ENCHANTMENT} would therefore silently never populate; the
 * same rule is recorded in {@code ModEnchantments} (wip-salvage) and in the
 * {@code ModRegistration} javadoc. The real {@code Enchantment} holders are
 * produced by the two payloads this card ships —
 * {@code data/thaumcraft/enchantment/{ench_repair,ench_haste}.json} — with
 * their applicability domains carried by item tags
 * ({@code data/thaumcraft/tags/item/enchantments/}) and their conflict
 * exclusion by an enchantment tag ({@code data/thaumcraft/tags/enchantment/}).
 * This class owns the id contract (content-mapping.csv rows
 * {@code Thaumcraft:ench_haste}/{@code Thaumcraft:ench_repair}, kind
 * {@code enchantment}); {@code RegistrationGameTest.implementedContentIdsResolve}
 * resolves such rows through the level's {@code HolderLookup.Provider}, which
 * is exactly how those two rows are now marked {@code done}/{@code tested}.</p>
 *
 * <p>Deferred to later cards: the tick behaviours for the two effects
 * (Repair's 40-tick vis mending sweep and Haste's speed bonus — the original's
 * {@code EventHandlerEntity.doRepair}/{@code updateSpeed}, sketched on the
 * wip-salvage branch as {@code enchantment/EnchantmentBehaviours}), the
 * {@code IRepairable} item domain itself (TC-M6-02 — the tag this card ships
 * carries the vanilla tool tags as the stand-in domain until those items land),
 * and the {@code ItemBook} leg of both applicability rules (books are not
 * ported yet).</p>
 */
public final class CatalogEnchantments {

    /** {@code thaumcraft:ench_repair} — the Repair enchantment id contract. */
    public static final ResourceKey<Enchantment> ENCHANTMENT_REPAIR = id("ench_repair");

    /** {@code thaumcraft:ench_haste} — the Haste enchantment id contract. */
    public static final ResourceKey<Enchantment> ENCHANTMENT_HASTE = id("ench_haste");

    private static ResourceKey<Enchantment> id(final String path) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path));
    }

    /** One wiring line from ModRegistration.init (catalog-triage pattern). */
    public static void init(final IEventBus bus) {
        // No bus registration: see the class doc — in 1.21.1 the ENCHANTMENT
        // registry is datapack-loaded and never receives a RegisterEvent, so
        // the two payloads under data/thaumcraft/enchantment/ are the
        // registration itself. This call is the house-pattern anchor for this
        // card's row in ModRegistration.init and the place a future code-side
        // enchantment hook would attach.
    }

    private CatalogEnchantments() {
    }
}
