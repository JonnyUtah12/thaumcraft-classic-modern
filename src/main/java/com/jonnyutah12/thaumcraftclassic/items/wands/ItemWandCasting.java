package com.jonnyutah12.thaumcraftclassic.items.wands;

import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgrades;
import com.jonnyutah12.thaumcraftclassic.api.wands.WandCap;
import com.jonnyutah12.thaumcraftclassic.api.wands.WandRod;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.casting.WandCasting;
import com.jonnyutah12.thaumcraftclassic.foci.FocusUpgradeList;
import com.jonnyutah12.thaumcraftclassic.foci.Focuses;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocus;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFoci;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;

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
 *   <li>per-focus spell bodies and the focus upgrade trees — TC-M6-03-s
 *       ({@code casting/FocusBehavior} is the seam, registered empty here), so
 *       {@link #frugalLevel} is 0 — exactly the original's value without a
 *       fitted focus;</li>
 *   <li>{@code IWandable} block/tile interaction and the architect overlay:
 *       {@link #use} is the casting entry point only (the pedestal seam is
 *       TC-M6-04d, the client overlay TC-M7-04);</li>
 *   <li>the staff rods' melee attribute modifier the original appended in
 *       {@code setRod} (+6 attack damage, {@code ItemWandCasting.java:503-514})</li>
 *   <li>the wand GUI and the client wand model renderer (rod/cap textures) —
 *       TC-M7-01/TC-M7-04. Until the GUI lands, a focus is fitted through
 *       {@link #setFocus};</li>
 *   <li>the tooltip's per-aspect consumption percent line: the original needed
 *       the wearer (worn-gear discounts) and a shift state, neither of which
 *       1.21.1's {@code appendHoverText} receives.</li>
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

    // ------------------------------------------------------------------ focus

    /**
     * The focus tag the wand carries, or null — the original's {@code "focus"}
     * NBT key, kept as the focus's registered identity (see
     * {@code ModDataComponents#WAND_FOCUS} for why a tag and not a stack).
     */
    public static String focusTag(final ItemStack stack) {
        final String tag = stack.getOrDefault(ModDataComponents.WAND_FOCUS.get(), "");
        return tag.isEmpty() ? null : tag;
    }

    /**
     * The focus stack the wand carries, rebuilt from its stored tag — the
     * original {@code getFocusItem} ({@code ItemWandCasting.java:462-469}).
     * Empty when the wand carries no focus or when the tag names no registered
     * focus item.
     */
    public static ItemStack focusItem(final ItemStack stack) {
        final String tag = focusTag(stack);
        if (tag == null) {
            return ItemStack.EMPTY;
        }
        final DeferredItem<ItemFocus> item = CatalogFoci.item(tag);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack focusStack = new ItemStack(item.get());
        // The original stored the focus stack itself, upgrades included; the port
        // rebuilds it from the tag and restores the upgrades it snapshotted (TC-M6-03-s).
        ItemFocus.setUpgrades(focusStack, focusUpgrades(stack));
        // The equal-trade pick is the focus's other per-stack state: the
        // original's wand stored the focus stack, so the "picked" compound a
        // sneak-pick wrote (ItemFocusTrade.java:93-95) came back on the next
        // cast. The port snapshots it on the wand and restores it here.
        ItemFocus.setPickedBlock(focusStack, focusPicked(stack));
        return focusStack;
    }

    /** The applied upgrades of the wand's focus — the original's stored stack's upgrade list. */
    public static FocusUpgradeList focusUpgrades(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.WAND_FOCUS_UPGRADES.get(), FocusUpgradeList.NONE);
    }

    /**
     * The block the wand's focus has picked (TC-M6-03-p) — the original's
     * {@code "picked"} compound on the focus stack the wand stored
     * ({@code ItemFocusBasic.getPickedBlock}, read by
     * {@code ItemFocusTrade.java:97-99,116-120}).
     */
    public static ItemStack focusPicked(final ItemStack stack) {
        return ItemFocus.unpackPicked(stack.getOrDefault(
                ModDataComponents.WAND_FOCUS_PICKED.get(), ItemContainerContents.EMPTY));
    }

    /**
     * Record (or clear, with an empty stack) the pick of the focus the wand
     * carries. The original wrote it straight onto the stored focus stack on a
     * sneak-pick ({@code ItemFocusTrade.java:93-95}) and charged nothing; the
     * port's focus is rebuilt from its tag on every cast, so the pick's home is
     * this snapshot — written by {@code casting/EffectPerformers.performSwap}.
     */
    public static void setFocusPicked(final ItemStack stack, final ItemStack picked) {
        if (picked == null || picked.isEmpty()) {
            stack.remove(ModDataComponents.WAND_FOCUS_PICKED.get());
        } else {
            stack.set(ModDataComponents.WAND_FOCUS_PICKED.get(), ItemFocus.packPicked(picked));
        }
    }

    /** The focus the wand carries, or null — the original {@code getFocus}. */
    public static Focus focus(final ItemStack stack) {
        return Focuses.byTag(focusTag(stack));
    }

    /**
     * Sets the wand's focus, or clears it — the original {@code setFocus}
     * ({@code ItemWandCasting.java:471-477}), whose {@code focus == null} branch
     * removed the {@code "focus"} key. Anything that is not a focus clears the
     * wand, which is the original's own guard (it cast the stored stack's item
     * to {@code ItemFocusBasic} and would have thrown on anything else).
     */
    public static void setFocus(final ItemStack stack, final ItemStack focusStack) {
        final String tag = ItemFocus.tagOf(focusStack);
        if (tag == null) {
            stack.remove(ModDataComponents.WAND_FOCUS.get());
            stack.remove(ModDataComponents.WAND_FOCUS_UPGRADES.get());
            stack.remove(ModDataComponents.WAND_FOCUS_PICKED.get());
            return;
        }
        stack.set(ModDataComponents.WAND_FOCUS.get(), tag);
        // The focus's applied upgrades are snapshotted with it, the way the
        // original's stored focus stack carried its own upgrade tag list (TC-M6-03-s).
        final FocusUpgradeList upgrades = ItemFocus.upgrades(focusStack);
        if (upgrades.applied().isEmpty()) {
            stack.remove(ModDataComponents.WAND_FOCUS_UPGRADES.get());
        } else {
            stack.set(ModDataComponents.WAND_FOCUS_UPGRADES.get(), upgrades);
        }
        // ...and so the original's stored stack carried its "picked" compound
        // (ItemFocusTrade.java:93-99), the equal-trade pick travels too (TC-M6-03-p).
        setFocusPicked(stack, ItemFocus.getPickedBlock(focusStack));
    }

    /** Clears the wand's focus — the original's {@code setFocus(stack, null)}. */
    public static void clearFocus(final ItemStack stack) {
        setFocus(stack, ItemStack.EMPTY);
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
     * stack's cap, through the TC-M4-02 engine. {@code frugalLevel} is
     * {@link #frugalLevel} — 0 in this slice (see that method).
     */
    public float getConsumptionModifier(final ItemStack stack, final Player player,
                                        final Aspect aspect, final boolean crafting) {
        return VisCostRules.consumptionModifier(capOf(stack).costProfile(),
                player == null ? null : WornGearDiscounts.sourceFor(player),
                aspect, frugalLevel(stack), isSceptre(stack), crafting);
    }

    /**
     * Original {@code getFocusFrugal} ({@code ItemWandCasting.java:287-289}):
     * the {@code frugal} upgrade level of the wand's focus.
     *
     * <p>TC-M6-03-s landed the upgrade tables, so this is the focus's real
     * level: the original returned 0 when the wand carried no focus
     * ({@code getFocus(is) == null}), which is what an empty upgrade list gives
     * here.</p>
     */
    public static int frugalLevel(final ItemStack stack) {
        return focusUpgradeLevel(stack, FocusUpgrades.FRUGAL);
    }

    /** Original {@code getFocusPotency} ({@code ItemWandCasting.java:277-280}): the focus's level plus the runed wand's +1. */
    public static int focusPotency(final ItemStack stack) {
        return focusUpgradeLevel(stack, FocusUpgrades.POTENCY) + (hasRunes(stack) ? 1 : 0);
    }

    /** Original {@code getFocusTreasure} ({@code ItemWandCasting.java:283-284}). */
    public static int focusTreasure(final ItemStack stack) {
        return focusUpgradeLevel(stack, FocusUpgrades.TREASURE);
    }

    /** Original {@code getFocusEnlarge} ({@code ItemWandCasting.java:291-292}). */
    public static int focusEnlarge(final ItemStack stack) {
        return focusUpgradeLevel(stack, FocusUpgrades.ENLARGE);
    }

    /** Original {@code getFocusExtend} ({@code ItemWandCasting.java:295-296}). */
    public static int focusExtend(final ItemStack stack) {
        return focusUpgradeLevel(stack, FocusUpgrades.EXTEND);
    }

    /**
     * {@code this.getFocus(stack).getUpgradeLevel(this.getFocusItem(stack), type)}
     * ({@code ItemWandCasting.java:278-296}): 0 without a focus.
     */
    private static int focusUpgradeLevel(final ItemStack stack, final com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgradeType type) {
        return ItemFocus.upgrades(focusItem(stack)).level(type);
    }

    /**
     * The whole-price form of {@link #getConsumptionModifier}: the original
     * {@code consumeAllVis} cost loop ({@code ItemWandCasting.java:327-331}),
     * which modifies every aspect of a price before any of it is paid. The
     * casting entry point prices a focus with this and pays with
     * {@link #consumeAllVis}, so the two can never disagree.
     */
    public AspectList effectiveCost(final ItemStack stack, final Player player,
                                    final AspectList baseCosts, final boolean crafting) {
        return VisCostRules.effectiveCosts(baseCosts, capOf(stack).costProfile(),
                player == null ? null : WornGearDiscounts.sourceFor(player),
                frugalLevel(stack), isSceptre(stack), crafting);
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
        final AspectList costs = effectiveCost(stack, player, centiCosts, crafting);
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

    // -------------------------------------------------------------- casting

    /**
     * The casting entry point (TC-M6-02): the original's {@code func_77659_a} →
     * {@code ItemFocusBasic.onFocusRightClick} chain
     * ({@code ItemWandCasting.java:526-560}), reduced to what this slice owns —
     * focus resolution, the TC-M4-02 price, the vis payment, and the delegate
     * call into the per-focus seam ({@code casting/FocusBehavior}, filled by
     * TC-M6-03-s).
     *
     * <p>The original also handled {@code IWandable} blocks/tiles and the
     * architect overlay in the same method; both belong to other cards (the
     * wand-pedestal seam is TC-M6-04d, the client overlay TC-M7-04).</p>
     *
     * <p>Return shape: nothing to cast is a pass-through (the original fell
     * through to {@code super}); a cast that could not be paid, or whose payment
     * was refunded because the focus had no effect, fails <em>without</em> a side
     * effect; a paid, effected cast succeeds. The mutating half is server-side
     * only — {@link WandCasting#cast} reports {@link WandCasting.Status#CLIENT}
     * on the client, which maps to a pass-through, matching the original's
     * {@code world.isRemote} gates.</p>
     */
    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        final WandCasting.CastResult result = WandCasting.cast(this, stack, player, level, hand);
        if (result.status() == WandCasting.Status.CHANNELED) {
            // The original's per-tick foci started an unbounded use on the right-click
            // (p.func_71008_a(itemstack, Integer.MAX_VALUE)) and did their work in
            // onUsingFocusTick; the port starts the same channel here.
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return switch (result.status()) {
            case CLIENT, NO_FOCUS, UNSUPPORTED -> InteractionResultHolder.pass(stack);
            case UNPAID, NO_EFFECT -> InteractionResultHolder.fail(stack);
            case CAST -> InteractionResultHolder.success(stack);
            case CHANNELED -> InteractionResultHolder.consume(stack);
        };
    }

    /**
     * Original {@code getItemUseAction}/{@code getMaxItemUseDuration} for the
     * per-tick foci: the original's own use duration was
     * {@code Integer.MAX_VALUE}, i.e. the channel ran until the button was
     * released or the vis ran out.
     */
    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity entity) {
        return Integer.MAX_VALUE;
    }

    /**
     * Original {@code onUsingFocusTick} ({@code ItemWandCasting.java:596-620}
     * dispatching into each focus's {@code onUsingFocusTick}, e.g.
     * {@code ItemFocusFire.java:104-137}): one tick of the channel — pay the
     * combination's price and fire. A tick that could not be paid, or produced
     * nothing, ends the channel, which is the original's
     * {@code p.func_71034_by()} (stop using) on a failed pre-check.
     */
    @Override
    public void onUseTick(final Level level, final LivingEntity livingEntity, final ItemStack stack,
                          final int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }
        final WandCasting.CastResult result = WandCasting.channelTick(this, stack, player, level,
                player.getUsedItemHand());
        if (result.status() != WandCasting.Status.CAST) {
            player.stopUsingItem();
        }
    }

    /**
     * Original {@code onPlayerStoppedUsingFocus} for the per-tick foci: fire and
     * shock simply stopped, excavation cleared its per-player break state
     * ({@code ItemFocusExcavation.func_77615_a}, lines 286-317). The port keeps
     * no per-player break state in this slice (the dig's block mutation is the
     * excavation machinery's, see {@code casting/FocusSpells}), so this is the
     * original's no-op half.
     */
    @Override
    public void releaseUsing(final ItemStack stack, final Level level, final LivingEntity livingEntity,
                             final int timeCharged) {
    }

    /** The original wand had no vanilla use animation; the cast FX are TC-M7's. */
    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.NONE;
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
        // Original ItemWandCasting.func_77624_a: the fitted focus's own display
        // name, bold+italic green, appended after the charge lines. TC-M6-02.
        final ItemStack focusStack = focusItem(stack);
        if (!focusStack.isEmpty()) {
            tooltip.add(Component.literal("")
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.GREEN)
                    .append(focusStack.getHoverName()));
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
