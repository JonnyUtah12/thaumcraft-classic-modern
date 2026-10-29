package com.jonnyutah12.thaumcraftclassic.wands;

import com.jonnyutah12.thaumcraftclassic.api.wands.WandCap;
import com.jonnyutah12.thaumcraftclassic.api.wands.WandRod;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The wand component tables (TC-M6-01): the port of
 * {@code ConfigItems.initializeWandComponents} + {@code postInit}
 * ({@code common/config/ConfigItems.java:287-322}), i.e. the 8 caps and the 18
 * rods (9 wand rods + 9 staff cores) with the original capacities, crafting
 * costs and cost modifiers.
 *
 * <p>Every value is transcribed from that registration block, in its
 * registration order — the order matters twice in the original: the rod map is
 * walked in insertion order by the wand recipe's rod lookup
 * ({@code ArcaneWandRecipe.getCraftingResult}) and by the pedestal's
 * cap/rod tag test, and the port's {@code LinkedHashMap}s preserve it.
 * The two conditional caps ({@code copper}, {@code silver}) were registered in
 * {@code postInit} when the ore dictionary saw the matching ingot
 * ({@code Config.foundCopperIngot} / {@code Config.foundSilverIngot}); 1.21.1
 * has no ingot-presence gate at registration time, so both are always
 * registered, and their recipe rows keep the original's gating through the
 * data-layer conditions on the ingredients (see docs/feature-tracking.md).</p>
 *
 * <p>This table is pure data and holds no item references: it is built during
 * {@code RegisterEvent} wiring, before items and aspects exist, so the
 * components carry the original's item id + damage value
 * ({@link #rodStack}/{@link #capStack} rebuild the stack on demand) and aspect
 * <em>tags</em> rather than {@code Aspect} objects.</p>
 *
 * <p>Original registration block, verbatim values:</p>
 * <pre>
 * WAND_CAP_IRON       iron       1.1F                       cost 1   meta 0
 * WAND_CAP_GOLD       gold       1.0F                       cost 3   meta 1
 * WAND_CAP_THAUMIUM   thaumium   0.9F                       cost 6   meta 2
 * WAND_CAP_VOID       void       0.8F                       cost 9   meta 7
 * WAND_CAP_COPPER     copper     1.1F  order+entropy 1.0F    cost 2   meta 3   (postInit)
 * WAND_CAP_SILVER     silver     1.0F  all four primals 0.95F cost 4   meta 4   (postInit)
 * WAND_ROD_WOOD       wood       25 vis  cost 1   minecraft:stick
 * WAND_ROD_GREATWOOD  greatwood  50      cost 3   meta 0
 * WAND_ROD_OBSIDIAN   obsidian   75      cost 6   meta 1  earth
 * WAND_ROD_BLAZE      blaze      75      cost 6   meta 6  fire   glow
 * WAND_ROD_ICE        ice        75      cost 6   meta 3  water
 * WAND_ROD_QUARTZ     quartz     75      cost 6   meta 4  order
 * WAND_ROD_BONE       bone       75      cost 6   meta 7  entropy
 * WAND_ROD_REED       reed       75      cost 6   meta 5  air
 * WAND_ROD_SILVERWOOD silverwood 100     cost 9   meta 2
 * STAFF_ROD_*         &lt;base&gt;_staff  125/175/250  cost 8/14/24  metas 50-57  (+ primal 250/32 meta 100)
 * </pre>
 */
public final class WandComponents {

    private static final ResourceLocation WAND_CAP_ITEM =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "wandcap");
    private static final ResourceLocation WAND_ROD_ITEM =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "wandrod");
    /** {@code WAND_ROD_WOOD} is registered on a plain stick, {@code ConfigItems.java:291}. */
    private static final ResourceLocation STICK_ITEM = ResourceLocation.withDefaultNamespace("stick");

    private static final Map<String, WandCap> CAPS = new LinkedHashMap<>();
    private static final Map<String, WandRod> RODS = new LinkedHashMap<>();
    private static final List<WandComponent> COMPONENTS = new ArrayList<>(16);
    private static final Map<String, WandComponent> BY_TAG = new LinkedHashMap<>();

    /** Original {@code ConfigItems.WAND_CAP_IRON}: the cap a stack without a cap component resolves to. */
    private static final WandCap DEFAULT_CAP;
    /** Original {@code ConfigItems.WAND_ROD_WOOD}: the rod a stack without a rod component resolves to. */
    private static final WandRod DEFAULT_ROD;

    /** Research gate recorded on the primal staff core (original: no research of its own). */
    public static final int WAND_ROD_COUNT = 18;
    public static final int WAND_CAP_COUNT = 6;

    static {
        // --- caps (ConfigItems.java:287-290, in registration order) ---------
        cap("iron", 1.1F, List.of(), 0.0F, 1, 0);
        cap("gold", 1.0F, List.of(), 0.0F, 3, 1);
        cap("thaumium", 0.9F, List.of(), 0.0F, 6, 2);
        cap("void", 0.8F, List.of(), 0.0F, 9, 7);
        // ConfigItems.postInit (:314-322) — see the class javadoc.
        cap("copper", 1.1F, List.of("ordo", "perditio"), 1.0F, 2, 3);
        cap("silver", 1.0F, List.of("aer", "terra", "ignis", "aqua"), 0.95F, 4, 4);

        // --- wand rods (ConfigItems.java:291-300) ---------------------------
        rod("wood", 25, 1, STICK_ITEM, 0, false, false, false, null, false);
        rod("greatwood", 50, 3, WAND_ROD_ITEM, 0, false, false, false, null, false);
        rod("obsidian", 75, 6, WAND_ROD_ITEM, 1, false, false, false, "terra", false);
        rod("blaze", 75, 6, WAND_ROD_ITEM, 6, false, true, false, "ignis", false);
        rod("ice", 75, 6, WAND_ROD_ITEM, 3, false, false, false, "aqua", false);
        rod("quartz", 75, 6, WAND_ROD_ITEM, 4, false, false, false, "ordo", false);
        rod("bone", 75, 6, WAND_ROD_ITEM, 7, false, false, false, "perditio", false);
        rod("reed", 75, 6, WAND_ROD_ITEM, 5, false, false, false, "aer", false);
        rod("silverwood", 100, 9, WAND_ROD_ITEM, 2, false, false, false, null, false);

        // --- staff cores (ConfigItems.java:301-310; StaffRod suffixes the tag) --
        rod("greatwood_staff", 125, 8, WAND_ROD_ITEM, 50, true, false, false, null, false);
        rod("obsidian_staff", 175, 14, WAND_ROD_ITEM, 51, true, false, false, "terra", false);
        rod("blaze_staff", 175, 14, WAND_ROD_ITEM, 56, true, true, false, "ignis", false);
        rod("ice_staff", 175, 14, WAND_ROD_ITEM, 53, true, false, false, "aqua", false);
        rod("quartz_staff", 175, 14, WAND_ROD_ITEM, 54, true, false, false, "ordo", false);
        rod("bone_staff", 175, 14, WAND_ROD_ITEM, 57, true, false, false, "perditio", false);
        rod("reed_staff", 175, 14, WAND_ROD_ITEM, 55, true, false, false, "aer", false);
        rod("silverwood_staff", 250, 24, WAND_ROD_ITEM, 52, true, false, false, null, false);
        rod("primal_staff", 250, 32, WAND_ROD_ITEM, 100, true, false, true, null, true);

        DEFAULT_CAP = CAPS.get("iron");
        DEFAULT_ROD = RODS.get("wood");

        // --- the registry entries, in docs/content-mapping.csv row order -----
        // caps first (:382-387), then the rod rows (:388-405): a rod row pair
        // sharing one tag collapses into one entry (see WandComponent).
        component("iron", "iron", null, null);
        component("gold", "gold", null, null);
        component("thaumium", "thaumium", null, null);
        component("void", "void", null, null);
        component("copper", "copper", null, null);
        component("silver", "silver", null, null);
        component("wood", null, "wood", null);
        component("greatwood", null, "greatwood", "greatwood_staff");
        component("obsidian", null, "obsidian", "obsidian_staff");
        component("blaze", null, "blaze", "blaze_staff");
        component("ice", null, "ice", "ice_staff");
        component("quartz", null, "quartz", "quartz_staff");
        component("bone", null, "bone", "bone_staff");
        component("reed", null, "reed", "reed_staff");
        component("silverwood", null, "silverwood", "silverwood_staff");
        component("primal", null, null, "primal_staff");
    }

    private static void cap(final String tag, final float base, final List<String> special,
                            final float specialModifier, final int craftCost, final int meta) {
        CAPS.put(tag, new WandCap(tag, base, List.copyOf(special), specialModifier, craftCost,
                WAND_CAP_ITEM, meta));
    }

    private static void rod(final String tag, final int capacity, final int craftCost,
                            final ResourceLocation itemId, final int meta, final boolean staff,
                            final boolean glowing, final boolean runes, final String primalAspect,
                            final boolean allPrimals) {
        RODS.put(tag, new WandRod(tag, capacity, craftCost, itemId, meta, staff, glowing, runes,
                primalAspect, allPrimals));
    }

    private static void component(final String tag, final String cap, final String wandRod,
                                  final String staffRod) {
        final WandComponent entry = new WandComponent(tag,
                cap == null ? null : CAPS.get(cap),
                wandRod == null ? null : RODS.get(wandRod),
                staffRod == null ? null : RODS.get(staffRod));
        COMPONENTS.add(entry);
        BY_TAG.put(tag, entry);
        if (BY_TAG.size() != COMPONENTS.size()) {
            throw new IllegalStateException("duplicate wand component tag: " + tag);
        }
    }

    // ------------------------------------------------------------------ lookups

    /** Every cap in original registration order (6). */
    public static Collection<WandCap> caps() {
        return Collections.unmodifiableCollection(CAPS.values());
    }

    /** Every rod in original registration order (9 wand rods + 9 staff cores). */
    public static Collection<WandRod> rods() {
        return Collections.unmodifiableCollection(RODS.values());
    }

    /** The registry entries of {@code thaumcraft:wand_component}, in CSV row order (16). */
    public static List<WandComponent> components() {
        return Collections.unmodifiableList(COMPONENTS);
    }

    /** The component registered under a tag, or null ({@code WandCap.caps.get(tag)}). */
    public static WandComponent component(final String tag) {
        return BY_TAG.get(tag);
    }

    /** A cap by tag, or null — the original {@code WandCap.caps.get(tag)}. */
    public static WandCap cap(final String tag) {
        return tag == null ? null : CAPS.get(tag);
    }

    /** A rod by tag, or null — the original {@code WandRod.rods.get(tag)}. */
    public static WandRod rod(final String tag) {
        return tag == null ? null : RODS.get(tag);
    }

    /** {@code ConfigItems.WAND_CAP_IRON} — the fallback of {@code getCap} on a bare stack. */
    public static WandCap defaultCap() {
        return DEFAULT_CAP;
    }

    /** {@code ConfigItems.WAND_ROD_WOOD} — the fallback of {@code getRod} on a bare stack. */
    public static WandRod defaultRod() {
        return DEFAULT_ROD;
    }

    // ------------------------------------------------------- stack reconstruction

    /**
     * The stack a cap is registered on (original {@code WandCap.getItem()}): the
     * port's {@code thaumcraft:wandcap} item carrying the cap's original damage
     * value in {@code thaumcraft:item_meta}.
     */
    public static ItemStack capStack(final WandCap cap) {
        return componentStack(cap.itemId(), cap.meta());
    }

    /** The stack a rod is registered on (original {@code WandRod.getItem()}). */
    public static ItemStack rodStack(final WandRod rod) {
        return componentStack(rod.itemId(), rod.meta());
    }

    private static ItemStack componentStack(final ResourceLocation itemId, final int meta) {
        final Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null) {
            throw new IllegalStateException("wand component item is not registered: " + itemId);
        }
        final ItemStack stack = new ItemStack(item);
        if (meta != 0) {
            stack.set(ModDataComponents.ITEM_META.get(), meta);
        }
        return stack;
    }

    private WandComponents() {
    }
}
