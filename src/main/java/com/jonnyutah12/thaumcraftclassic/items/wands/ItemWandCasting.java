package com.jonnyutah12.thaumcraftclassic.items.wands;

import com.jonnyutah12.thaumcraftclassic.api.wands.WandCap;
import com.jonnyutah12.thaumcraftclassic.api.wands.WandRod;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogWands;
import com.jonnyutah12.thaumcraftclassic.wands.IVisContainerItem;
import com.jonnyutah12.thaumcraftclassic.wands.VisContainerItem;
import com.jonnyutah12.thaumcraftclassic.wands.VisCostRules;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.jonnyutah12.thaumcraftclassic.wands.WandComponents;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import com.jonnyutah12.thaumcraftclassic.wands.WornGearDiscounts;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The wand item (TC-M6-01; content-mapping.csv row {@code thaumcraft:wandcasting},
 * original {@code common/items/wands/ItemWandCasting.java}): rod + cap identity,
 * capacity from the rod, vis storage in the shared {@code thaumcraft:vis_pool}
 * component, and the original's cost-modifier arithmetic.
 *
 * <h2>Identity (original NBT keys → typed components)</h2>
 * The original wrote {@code "cap"} / {@code "rod"} tag strings and a
 * {@code "sceptre"} marker on the stack's NBT
 * ({@code setCap}/{@code setRod}, {@code ItemWandCasting.java:490-524}) and its
 * damage value carried {@code capCraftCost × rodCraftCost}
 * ({@code ArcaneWandRecipe.getCraftingResult}, {@code ArcaneSceptreRecipe} ×1.5).
 * The port keeps the three tag/marker keys as
 * {@code thaumcraft:wand_cap} / {@code thaumcraft:wand_rod} /
 * {@code thaumcraft:wand_sceptre} data components and the damage value in the
 * shared {@code thaumcraft:item_meta} component
 * ({@link ModDataComponents#ITEM_META}). Reads preserve the original's
 * defaults: an absent key resolves to {@code WAND_CAP_IRON} / {@code WAND_ROD_WOOD}
 * ({@code getCap}/{@code getRod}), and {@code isSceptre} is a <em>presence</em>
 * test ({@code func_74764_b("sceptre")}).</p>
 *
 * <h2>Capacity and vis pool</h2>
 * {@code getMaxVis = rod.capacity × (isSceptre ? 150 : 100)} in centi-vis
 * (original {@code ItemWandCasting.java:82-84}), delegated to
 * {@link WandVis#maxVis}. Stored charge lives in {@link VisPool} through
 * {@link WandVis} — the same seam {@code ItemAmuletVis} (TC-M8-02) uses, so the
 * vis amulet's worn-tick transfer and the wand pedestal reach the wand
 * unchanged through {@link VisContainerItem} / {@link IVisContainerItem}.</p>
 *
 * <h2>Cost chain</h2>
 * {@code getConsumptionModifier} is the TC-M4-02 chain: cap profile →
 * worn-gear discount (TC-M8-02) → frugal (foci, absent ⇒ 0) → sceptre −0.1 →
 * floor 0.1 ({@link VisCostRules#consumptionModifier}). {@code consumeVis} /
 * {@code consumeAllVis} / {@code consumeAllVisCrafting} apply it with the
 * original's truncating int cast and all-or-nothing payment.</p>
 *
 * <h2>Rod on-update</h2>
 * {@code inventoryTick} runs the original's {@code func_77663_a} →
 * {@code WandRodPrimalOnUpdate} body: fixed-primal rods top up 1 vis whenever
 * the charge is below {@code maxVis/10} on every 200th player tick; the primal
 * staff core does the same every 50th tick on a random under-filled primal
 * ({@code WandRodPrimalOnUpdate.java:19-45}). The random pick is exposed as
 * {@link #primalTopUpTarget} so tests can pin the rule with a seeded
 * {@link RandomSource}.</p>
 *
 * <h2>Deliberately not here (other cards)</h2>
 * <ul>
 *   <li>foci (slot, upgrades, potency/frugal levels) — TC-M6-02
 *       {@code t_01f994cf}; the frugal term is therefore level 0, which is
 *       exactly the original's value without a focus;</li>
 *   <li>casting: {@code onItemUseFirst}/{@code onItemRightClick}/{@code onUsingTick}
 *       and {@code IArchitect} — the casting entry point does not exist yet
 *       (preflight note on this card);</li>
 *   <li>the staff rods' melee attribute modifier the original appended in
 *       {@code setRod} (+6 attack damage, {@code ItemWandCasting.java:503-514})</li>
 *   <li>the wand HUD/GUI and the client wand model renderer (rod/cap textures)
 *       — TC-M7-01/TC-M7-04;</li>
 *   <li>the tooltip's per-aspect consumption percent and focus cost lines: the
 *       original needed the wearer (worn-gear discounts) and a shift state,
 *       neither of which 1.21.1's {@code appendHoverText} receives.</li>
 * </ul>
 */
public class ItemWandCasting extends Item implements VisContainerItem, IVisContainerItem {

    /** The original's hard-coded sceptre research gate ({@code ArcaneSceptreRecipe.matches}). */
    public static final String RESEARCH_SCEPTRE = "SCEPTRE";

    /** Original {@code getMaxVis} multiplier for a sceptre ({@code ItemWandCasting.java:83}). */
    public static final int SCEPTRE_CAPACITY_PERCENT = 150;

    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#######.##");

    public ItemWandCasting(final Properties props) {
        super(props);
    }

    // ------------------------------------------------------------ identity

    /** The original's {@code getRod}: an absent component resolves to the wood rod. */
    public static WandRod rodOf(final ItemStack stack) {
        final WandRod rod = WandComponents.rod(stack.getOrDefault(ModDataComponents.WAND_ROD.get(), ""));
        return rod == null ? WandComponents.defaultRod() : rod;
    }

    /** The original's {@code getCap}: an absent component resolves to the iron cap. */
    public static WandCap capOf(final ItemStack stack) {
        final WandCap cap = WandComponents.cap(stack.getOrDefault(ModDataComponents.WAND_CAP.get(), ""));
        return cap == null ? WandComponents.defaultCap() : cap;
    }

    /** Original {@code isSceptre}: the marker key's presence, not its value. */
    public static boolean isSceptre(final ItemStack stack) {
        return stack.has(ModDataComponents.WAND_SCEPTRE.get());
    }

    /** Original {@code isStaff}: the rod is a staff core. */
    public static boolean isStaff(final ItemStack stack) {
        return rodOf(stack).staff();
    }

    /** Original {@code hasRunes}: only the primal staff core has runes. */
    public static boolean hasRunes(final ItemStack stack) {
        final WandRod rod = rodOf(stack);
        return rod.staff() && rod.runes();
    }

    /**
     * The original's recipe-result damage value: {@code capCraftCost × rodCraftCost},
     * or {@code (int)(cost × 1.5)} for a sceptre
     * ({@code ArcaneWandRecipe.getCraftingResult}, {@code ArcaneSceptreRecipe}).
     */
    public static int damageValue(final WandCap cap, final WandRod rod, final boolean sceptre) {
        final int cost = cap.craftCost() * rod.craftCost();
        return sceptre ? (int) (cost * 1.5F) : cost;
    }

    /**
     * The stack the original's wand recipe produces: a wand with the cap/rod
     * components set and the craft cost as its damage value
     * ({@code new ItemStack(itemWandCasting, 1, cost)} + {@code setCap}/{@code setRod},
     * plus the {@code "sceptre"} marker for the sceptre recipe).
     */
    public static ItemStack stack(final WandCap cap, final WandRod rod, final boolean sceptre) {
        final ItemStack stack = new ItemStack(CatalogWands.WAND_CASTING.get());
        stack.set(ModDataComponents.WAND_CAP.get(), cap.tag());
        stack.set(ModDataComponents.WAND_ROD.get(), rod.tag());
        if (sceptre) {
            stack.set(ModDataComponents.WAND_SCEPTRE.get(), Boolean.TRUE);
        }
        final int damage = damageValue(cap, rod, sceptre);
        if (damage != 0) {
            stack.set(ModDataComponents.ITEM_META.get(), damage);
        }
        return stack;
    }

    /** {@code stack(cap, rod, false)}. */
    public static ItemStack stack(final WandCap cap, final WandRod rod) {
        return stack(cap, rod, false);
    }

    // ------------------------------------------------------------ capacity

    /** Original {@code getMaxVis} in centi-vis. */
    @Override
    public int getMaxVis(final ItemStack stack) {
        return WandVis.maxVis(rodOf(stack).capacity(), isSceptre(stack));
    }

    // ---------------------------------------------------------------- pool

    @Override
    public int getVis(final ItemStack stack, final Aspect aspect) {
        return WandVis.poolOf(stack).getVis(aspect);
    }

    @Override
    public void storeVis(final ItemStack stack, final Aspect aspect, final int amount) {
        // Original storeVis writes the NBTTagInt absolutely, zero included.
        WandVis.setPool(stack, WandVis.withVis(WandVis.poolOf(stack), aspect, amount));
    }

    @Override
    public AspectList getAspectsWithRoom(final ItemStack stack) {
        return WandVis.getAspectsWithRoom(WandVis.poolOf(stack), getMaxVis(stack));
    }

    @Override
    public AspectList getAllVis(final ItemStack stack) {
        // Original merges every primal, zero included.
        final AspectList out = new AspectList();
        for (final Aspect aspect : Aspect.getPrimalAspects()) {
            out.add(aspect, getVis(stack, aspect));
        }
        return out;
    }

    @Override
    public int addVis(final ItemStack stack, final Aspect aspect, final int amount, final boolean doit) {
        final WandVis.AddResult result = WandVis.addVis(WandVis.poolOf(stack), aspect, amount, getMaxVis(stack));
        if (doit) {
            WandVis.setPool(stack, result.pool());
        }
        return result.leftover();
    }

    @Override
    public int addRealVis(final ItemStack stack, final Aspect aspect, final int centiAmount, final boolean doit) {
        final WandVis.AddResult result =
                WandVis.addRealVis(WandVis.poolOf(stack), aspect, centiAmount, getMaxVis(stack));
        if (doit) {
            WandVis.setPool(stack, result.pool());
        }
        return result.leftover();
    }

    // ---------------------------------------------------------- cost chain

    /**
     * The original {@code getConsumptionModifier} (lines 254-274) for this
     * stack's cap, through the TC-M4-02 engine. {@code frugalLevel} is 0
     * (foci are TC-M6-02); the original skipped that term entirely when the
     * wand had no focus, so the value is identical.
     */
    public float getConsumptionModifier(final ItemStack stack, final Player player,
                                        final Aspect aspect, final boolean crafting) {
        return VisCostRules.consumptionModifier(capOf(stack).costProfile(),
                player == null ? null : WornGearDiscounts.sourceFor(player),
                aspect, 0, isSceptre(stack), crafting);
    }

    /**
     * Original {@code consumeVis} (lines 305-312): the amount is modified with
     * the truncating int cast, then spent all-or-nothing on that one aspect.
     */
    public boolean consumeVis(final ItemStack stack, final Player player, final Aspect aspect,
                              final int amount, final boolean crafting) {
        final int cost = VisCostRules.applyCost(amount,
                getConsumptionModifier(stack, player, aspect, crafting));
        final VisPool paid = WandVis.consumeVis(WandVis.poolOf(stack), aspect, cost);
        if (paid == null) {
            return false;
        }
        WandVis.setPool(stack, paid);
        return true;
    }

    /**
     * Original {@code consumeAllVis} (lines 325-347): every aspect's cost is
     * modified, then every aspect must be covered before any is subtracted. The
     * original only wrote the result when {@code doit && !world.isRemote}.
     */
    @Override
    public boolean consumeAllVis(final ItemStack stack, final Player player,
                                 final AspectList centiCosts, final boolean doit, final boolean crafting) {
        if (centiCosts == null || centiCosts.size() == 0) {
            return false;
        }
        final AspectList costs = VisCostRules.effectiveCosts(centiCosts, capOf(stack).costProfile(),
                player == null ? null : WornGearDiscounts.sourceFor(player),
                0, isSceptre(stack), crafting);
        final VisPool paid = WandVis.consumeAllVis(WandVis.poolOf(stack), costs);
        if (paid == null) {
            return false;
        }
        if (doit && (player == null || !player.level().isClientSide)) {
            WandVis.setPool(stack, paid);
        }
        return true;
    }

    /** Whole-vis variant of {@link #consumeAllVis} — the original's {@code consumeAllVisCrafting} (lines 314-322). */
    public boolean consumeAllVisCrafting(final ItemStack stack, final Player player,
                                         final AspectList realCosts, final boolean doit) {
        if (realCosts == null || realCosts.size() == 0) {
            return false;
        }
        return consumeAllVis(stack, player, WandVis.scaleToCenti(realCosts), doit, true);
    }

    // -------------------------------------------------------- rod on-update

    /**
     * The original {@code WandRodPrimalOnUpdate} tick period: 200 player ticks
     * for a fixed-primal rod, 50 for the primal staff core
     * ({@code WandRodPrimalOnUpdate.java:21,26}).
     */
    public static int rodTickInterval(final WandRod rod) {
        return rod.primalAspectTag() != null ? 200 : 50;
    }

    /**
     * The aspect a rod's on-update would top up for a given pool, or null when
     * none qualifies: fixed-primal rods pick their own aspect, the primal staff
     * core picks uniformly among the primals below {@code maxVis / 10}
     * (original {@code WandRodPrimalOnUpdate.java:27-44}; the pick is passed the
     * RNG so it is testable).
     */
    public static Aspect primalTopUpTarget(final VisPool pool, final int maxVis,
                                           final WandRod rod, final RandomSource random) {
        return primalTopUpTarget(pool, maxVis, rod, Aspect.getPrimalAspects(), random);
    }

    /**
     * The same rule over a caller-supplied primal list — the aspects are passed
     * in so the rule is unit-testable without the live aspect registry (the
     * original resolved them once, at construction:
     * {@code WandRodPrimalOnUpdate.primals}).
     */
    static Aspect primalTopUpTarget(final VisPool pool, final int maxVis, final WandRod rod,
                                    final List<Aspect> primals, final RandomSource random) {
        if (!rod.hasOnUpdate()) {
            return null;
        }
        final int threshold = maxVis / 10;
        if (rod.primalAspectTag() != null) {
            for (final Aspect aspect : primals) {
                if (aspect.getTag().equals(rod.primalAspectTag())) {
                    return WandVis.getVis(pool, aspect) < threshold ? aspect : null;
                }
            }
            return null;
        }
        final List<Aspect> candidates = new ArrayList<>();
        for (final Aspect aspect : primals) {
            if (WandVis.getVis(pool, aspect) < threshold) {
                candidates.add(aspect);
            }
        }
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * Original {@code func_77663_a} (lines 380-386): server side only, the rod's
     * on-update runs from the holder's inventory tick.
     */
    @Override
    public void inventoryTick(final ItemStack stack, final Level level, final Entity entity,
                              final int slot, final boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        final WandRod rod = rodOf(stack);
        if (!rod.hasOnUpdate() || player.tickCount % rodTickInterval(rod) != 0) {
            return;
        }
        final Aspect target = primalTopUpTarget(WandVis.poolOf(stack), getMaxVis(stack), rod,
                player.getRandom());
        if (target != null) {
            addVis(stack, target, 1, true);
        }
    }

    // ---------------------------------------------------------------- display

    /**
     * Original {@code func_77653_i} (lines 117-134): a format string with the
     * cap, rod (staff suffix stripped) and object fragments substituted. The
     * port keeps one key per fragment, lowercased per D-001:
     * {@code item.thaumcraft.wandcasting.name}, {@code ….wandcasting.<cap>.cap},
     * {@code ….wandcasting.<rod>.rod} and {@code ….wandcasting.{wand,staff,sceptre}.obj}.
     */
    @Override
    public Component getName(final ItemStack stack) {
        final String object = isStaff(stack) ? "staff.obj"
                : isSceptre(stack) ? "sceptre.obj" : "wand.obj";
        return Component.translatable("item.thaumcraft.wandcasting.name",
                Component.translatable("item.thaumcraft.wandcasting." + capOf(stack).tag() + ".cap"),
                Component.translatable("item.thaumcraft.wandcasting." + rodOf(stack).baseTag() + ".rod"),
                Component.translatable("item.thaumcraft.wandcasting." + object));
    }

    /**
     * Original {@code func_77624_a} capacity line + stored-aspect line. The
     * per-aspect consumption percentage needs the wearer (worn-gear discounts)
     * and a shift state, which 1.21.1's {@code appendHoverText} does not receive;
     * the focus lines belong to TC-M6-02 (see the class javadoc).
     */
    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                               final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(Component.translatable("item.capacity.text")
                .append(" " + (getMaxVis(stack) / WandVis.CENTI_VIS_PER_VIS))
                .withStyle(ChatFormatting.GOLD));
        final VisPool pool = WandVis.poolOf(stack);
        for (final Aspect aspect : Aspect.getPrimalAspects()) {
            if (pool.centiVis().containsKey(aspect.getTag())) {
                final ChatFormatting color = ChatFormatting.getByCode(aspect.getChatcolor().charAt(0));
                tooltip.add(Component.literal(" ")
                        .append(Component.literal(aspect.getName())
                                .withStyle(color == null ? ChatFormatting.WHITE : color))
                        .append(Component.literal(" x "
                                        + AMOUNT_FORMAT.format(getVis(stack, aspect) / 100.0F))
                                .withStyle(ChatFormatting.WHITE)));
            }
        }
    }

    // ------------------------------------------------------ IVisContainerItem

    @Override
    public String getCapTag(final ItemStack stack) {
        return capOf(stack).tag();
    }

    @Override
    public String getRodTag(final ItemStack stack) {
        return rodOf(stack).tag();
    }
}
