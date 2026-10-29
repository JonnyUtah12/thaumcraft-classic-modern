package com.jonnyutah12.thaumcraftclassic.items.foci;

import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.foci.Focuses;
import java.text.DecimalFormat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
     * Original {@code getVisCost} on an unupgraded stack (see the class javadoc
     * for the upgrade-dependent variants and the primal roll). Amounts are
     * centi-vis; the TC-M4-02 cost chain applies the cap/gear modifiers at
     * payment time, exactly as the original did.
     */
    public AspectList getVisCost(final ItemStack stack) {
        return Focuses.PRIMAL_TAG.equals(focus.tag())
                ? Focuses.primalCost(System.currentTimeMillis())
                : focus.cost();
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
     * Original {@code getActivationCooldown} on an unupgraded stack, in
     * milliseconds. The upgrade-dependent variants ({@code fireball} 1000,
     * {@code chainlightning}/{@code earthshock}, {@code scattershot}/
     * {@code iceboulder}) land with TC-M6-03-s.
     */
    public int getActivationCooldown(final ItemStack stack) {
        return focus.activationCooldown();
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
