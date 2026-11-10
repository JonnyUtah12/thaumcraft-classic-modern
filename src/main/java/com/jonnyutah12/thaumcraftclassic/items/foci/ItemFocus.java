package com.jonnyutah12.thaumcraftclassic.items.foci;

import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgradeType;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.foci.FocusCombinations;
import com.jonnyutah12.thaumcraftclassic.foci.FocusUpgradeList;
import com.jonnyutah12.thaumcraftclassic.foci.FocusUpgradeTrees;
import com.jonnyutah12.thaumcraftclassic.foci.Focuses;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import java.text.DecimalFormat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.TooltipFlag;

/**
 * A wand focus (TC-M6-02): the port of the shared half of
 * {@code thaumcraft.api.wands.ItemFocusBasic}
 * ({@code api-repos/thaumcraft-api/wands/ItemFocusBasic.java:24-90}) — the
 * identity, the price declaration and the tooltip — for each of the ten focal
 * items {@code ConfigItems.java:335-354} registered.
 *
 * <p>The original had one class per focus ({@code ItemFocusFire},
 * {@code ItemFocusShock}, ...) differing only in their declared values plus the
 * per-focus spell body. The port keeps <em>one</em> generic class whose identity
 * is a {@link Focus} table entry ({@link #focus()}), and registers it once per
 * docs/content-mapping.csv {@code item} row, so the "one registered object per
 * row" contract holds while the near-duplicate classes disappear. The spell
 * bodies are TC-M6-03-s and live behind the casting seam
 * ({@code casting/FocusBehavior}).</p>
 *
 * <h2>Stack shape</h2>
 * The original constructor set {@code maxStackSize = 1}, {@code canRepair =
 * false}, {@code setMaxDamage(0)} and the class reported
 * {@code EnumRarity.rare} ({@code ItemFocusBasic.java:26-32,86-89}); the port
 * expresses all four through {@link Item.Properties} in
 * {@code registration/catalog/CatalogFoci}.</p>
 *
 * <h2>Price</h2>
 * {@code getVisCost} returns the focus's table price — the value the original
 * returned on a stack with no upgrades. The nine foci whose price has
 * upgrade-dependent variants ({@code costBeam}/{@code costBall} on fire,
 * {@code costScatter}/{@code costBoulder} on frost, ...) resolve to their base
 * variant here, because the upgrade trees are per-focus behaviour
 * (TC-M6-03-s). The primal focus is the original's one rolled price
 * ({@code ItemFocusPrimal.getVisCost}, {@code System.currentTimeMillis() / 200}),
 * kept as {@link Focuses#primalCost(long)}.</p>
 */
public class ItemFocus extends Item {

    /** The original tooltip's amount format ({@code ItemFocusBasic.addInformation}). */
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#####.##");

    private final Focus focus;

    public ItemFocus(final Focus focus, final Properties properties) {
        super(properties);
        this.focus = focus;
    }

    /** This item's table entry. */
    public Focus focus() {
        return focus;
    }

    /** The focus a stack carries, or null when the stack is not a focus. */
    public static Focus focusOf(final ItemStack stack) {
        return stack.getItem() instanceof ItemFocus item ? item.focus : null;
    }

    /** The focus tag of a stack, or null — the identity the casting seam keys on. */
    public static String tagOf(final ItemStack stack) {
        final Focus focus = focusOf(stack);
        return focus == null ? null : focus.tag();
    }

    /**
     * Original {@code getVisCost}: the price for this stack's <em>combination</em>
     * of applied upgrades (TC-M6-03-s). The original resolved it with a ternary
     * chain over the upgrades ({@code isUpgradedWith(firebeam) ? costBeam :
     * isUpgradedWith(fireball) ? costBall : costBase},
     * {@code ItemFocusFire.java:62-64}); {@link FocusCombinations} keeps those
     * chains, their order and their values, so an upgraded focus is priced
     * exactly as the original priced it. The primal focus is still the one
     * rolled price.
     */
    public AspectList getVisCost(final ItemStack stack) {
        if (Focuses.PRIMAL_TAG.equals(focus.tag())) {
            return Focuses.primalCost(System.currentTimeMillis());
        }
        final AspectList combination = FocusCombinations.cost(focus.tag(), upgrades(stack));
        // Before the aspect registry is live (unit tests, registration wiring) a
        // resolved price is empty, so fall back to the declared combination price.
        return combination.size() == 0 ? Focus.resolveCost(FocusCombinations.resolve(focus.tag(), upgrades(stack)) == null
                ? focus.baseCost() : FocusCombinations.resolve(focus.tag(), upgrades(stack)).cost()) : combination;
    }

    /** Original {@code isVisCostPerTick} — the tooltip wording and the per-tick charge flag. */
    public boolean isVisCostPerTick(final ItemStack stack) {
        return focus.visCostPerTick();
    }

    /** Original {@code getFocusColor}: the orb colour the wand renders. */
    public int getFocusColor(final ItemStack stack) {
        return focus.focusColor();
    }

    /**
     * Original {@code getActivationCooldown}: the cooldown of the stack's
     * combination (TC-M6-03-s) — 0 for an unupgraded fire/trade/excavation/
     * portable-hole/warding focus, 1000 for a fireball fire focus, 500 for a
     * scattershot/iceboulder frost focus, 1000 for shock's earthshock, and so on
     * ({@code foci/FocusCombinations}). The uncombined value is the parent
     * slice's {@link Focus#activationCooldown()} for every focus, which
     * {@code FociParityTest} still pins.
     */
    public int getActivationCooldown(final ItemStack stack) {
        return FocusCombinations.cooldown(focus.tag(), upgrades(stack));
    }

    // ------------------------------------------------------------- picked block

    /**
     * The block this equal-trade focus carries — the original's
     * {@code ItemFocusBasic.getPickedBlock}, the {@code "picked"} compound on
     * the focus stack ({@code ItemFocusTrade.java:97-99,116-120}). The port
     * keeps the stack of the picked block's item
     * ({@code BlockItemUtils.getBlockItemStack}).
     */
    public static ItemStack getPickedBlock(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return unpackPicked(stack.getOrDefault(ModDataComponents.PICKED_BLOCK.get(),
                ItemContainerContents.EMPTY));
    }

    /**
     * Pack a picked stack into the one-slot container the pick components
     * carry. {@code ItemStack} cannot be a data component value (NeoForge
     * requires the type to declare {@code equals}/{@code hashCode}, and
     * 1.20.5 dropped it), so vanilla's {@code ItemContainerContents} wraps it.
     */
    public static ItemContainerContents packPicked(final ItemStack picked) {
        return picked == null || picked.isEmpty()
                ? ItemContainerContents.EMPTY
                : ItemContainerContents.fromItems(java.util.List.of(picked));
    }

    /** The inverse of {@link #packPicked}: an empty container is {@code EMPTY}. */
    public static ItemStack unpackPicked(final ItemContainerContents contents) {
        // copyOne is the first non-air stack, EMPTY when there is none.
        return contents == null ? ItemStack.EMPTY : contents.copyOne();
    }

    /** The original's {@code setPickedBlock}: an empty stack clears it. */
    public static void setPickedBlock(final ItemStack stack, final ItemStack picked) {
        if (picked == null || picked.isEmpty()) {
            stack.remove(ModDataComponents.PICKED_BLOCK.get());
        } else {
            stack.set(ModDataComponents.PICKED_BLOCK.get(), packPicked(picked));
        }
    }

    // ------------------------------------------------------------------ upgrades

    /** The upgrades applied to a focus stack — the original's {@code "upgrade"} tag list. */
    public static FocusUpgradeList upgrades(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FocusUpgradeList.NONE;
        }
        return stack.getOrDefault(ModDataComponents.FOCUS_UPGRADES.get(), FocusUpgradeList.NONE);
    }

    /** Writes the stack's applied upgrades; an empty list clears the component. */
    public static void setUpgrades(final ItemStack stack, final FocusUpgradeList upgrades) {
        if (upgrades == null || upgrades.applied().isEmpty()) {
            stack.remove(ModDataComponents.FOCUS_UPGRADES.get());
        } else {
            stack.set(ModDataComponents.FOCUS_UPGRADES.get(), upgrades);
        }
    }

    /**
     * Original {@code getUpgradeLevel} on this focus's stack — the level of an
     * applied upgrade, which is what the wand's potency/enlarge/extend/treasure
     * levels are derived from.
     */
    public int upgradeLevel(final ItemStack stack, final FocusUpgradeType type) {
        return upgrades(stack).level(type);
    }

    /** Original {@code isUpgradedWith}. */
    public boolean isUpgradedWith(final ItemStack stack, final FocusUpgradeType type) {
        return upgrades(stack).isUpgradedWith(type);
    }

    /** Original {@code getPossibleUpgradesByRank}: this focus's tree at one rank. */
    public FocusUpgradeType[] getPossibleUpgradesByRank(final ItemStack stack, final int rank) {
        final java.util.List<FocusUpgradeType> rankList = FocusUpgradeTrees.byRank(focus.tag(), rank);
        return rankList == null ? null : rankList.toArray(new FocusUpgradeType[0]);
    }

    /**
     * Original {@code canApplyUpgrade} ({@code ItemFocusFire.java:157-162} and the
     * three other overrides) plus the tree's own offer: whether {@code type} may
     * be applied at {@code rank}. {@code researchComplete} is the hellbat rule's
     * {@code VAMPBAT} research check, passed in so this is decidable without a
     * player.
     */
    public boolean canApplyUpgrade(final ItemStack stack, final FocusUpgradeType type, final int rank,
                                   final boolean researchComplete) {
        return FocusUpgradeTrees.canApply(focus.tag(), upgrades(stack), type, rank, researchComplete);
    }

    /**
     * Original {@code applyUpgrade} ({@code ItemFocusBasic.java:143-152}): the
     * upgrade lands in its own rank when that slot is free; false otherwise.
     */
    public boolean applyUpgrade(final ItemStack stack, final FocusUpgradeType type, final int rank) {
        final java.util.Optional<FocusUpgradeList> applied = upgrades(stack).withUpgrade(type, rank);
        applied.ifPresent(next -> setUpgrades(stack, next));
        return applied.isPresent();
    }

    /** Original {@code getMaxAreaSize}: the architect area a focus offers ({@code ItemFocusTrade.java:260-262}). */
    public int getMaxAreaSize(final ItemStack stack) {
        return switch (focus.tag()) {
            case "trade" -> 3 + upgrades(stack).level(com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgrades.ENLARGE) * 2;
            case "warding" -> 3 + upgrades(stack).level(com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgrades.ENLARGE);
            default -> 1;
        };
    }

    /** Original {@code getSortingHelper}: the pouch's fixed sort key prefix. */
    public String getSortingHelper(final ItemStack stack) {
        return focus.sortingHelper();
    }

    /** The research entry that gates this focus (an entry of the shipped research graph). */
    public String researchKey() {
        return focus.researchKey();
    }

    /**
     * Original {@code addInformation} (ItemFocusBasic.java:48-59): the
     * per-cast/per-tick cost line followed by one line per aspect, with the
     * amount rendered in whole vis. The upgrade list the original appended
     * after it needs {@code FocusUpgradeType}, which is TC-M6-03-s.
     */
    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final AspectList cost = getVisCost(stack);
        if (cost.size() == 0) {
            return;
        }
        tooltip.add(Component.translatable(isVisCostPerTick(stack)
                ? "item.thaumcraft.focus.cost2"
                : "item.thaumcraft.focus.cost1"));
        for (final Aspect aspect : cost.getAspectsSorted()) {
            final ChatFormatting color = ChatFormatting.getByCode(aspect.getChatcolor().charAt(0));
            tooltip.add(Component.literal(" ")
                    .append(Component.literal(aspect.getName())
                            .withStyle(color == null ? ChatFormatting.WHITE : color))
                    .append(Component.literal(" x "
                            + AMOUNT_FORMAT.format(cost.getAmount(aspect) / 100.0F))
                            .withStyle(ChatFormatting.RESET)));
        }
    }
}
