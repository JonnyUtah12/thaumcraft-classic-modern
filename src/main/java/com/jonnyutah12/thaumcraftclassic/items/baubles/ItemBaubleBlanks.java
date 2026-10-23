package com.jonnyutah12.thaumcraftclassic.items.baubles;

import java.util.List;
import java.util.Set;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.api.IRunicArmor;
import com.jonnyutah12.thaumcraftclassic.api.IVisDiscountGear;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;

/**
 * Bauble blanks (TC-M8-02): port of
 * {@code thaumcraft.common.items.baubles.ItemBaubleBlanks}
 * ({@code common/items/baubles/ItemBaubleBlanks.java}).
 *
 * <p>Original metas preserved verbatim (damage values →
 * {@code thaumcraft:item_meta}): 0 = amulet blank, 1 = ring blank,
 * 2 = belt blank, 3-8 = apprentice rings, one per primal aspect in
 * {@code Aspect.getPrimalAspects()} order (the original indexed
 * {@code getPrimalAspects().get(meta - 3)} for the discount, the aspect
 * colour and the tooltip, lines 57-102).</p>
 *
 * <ul>
 * <li>slot routing = the original's {@code getBaubleType} switch
 *     (lines 57-70): metas 1,3-8 → ring, 2 → belt, else amulet;</li>
 * <li>vis discount = the original's {@code getVisDiscount} (line 85):
 *     1% toward the stack's own primal aspect, 0 otherwise — consumed by
 *     {@code wands/WornGearDiscounts} (TC-M8-02 aggregation);</li>
 * <li>{@code getRunicCharge} = 0, as the original implemented
 *     {@code IRunicArmor} with a zero charge (line 111).</li>
 * </ul>
 */
public class ItemBaubleBlanks extends MetaBaubleItem implements IRunicArmor, IVisDiscountGear {

    public static final int META_AMULET = 0;
    public static final int META_RING = 1;
    public static final int META_BELT = 2;
    /** First apprentice-ring meta; 3..8, one per primal aspect. */
    public static final int META_APPRENTICE_FIRST = 3;
    public static final int META_APPRENTICE_LAST = 8;
    public static final int META_COUNT = 9;

    public ItemBaubleBlanks(final Properties props) {
        super(props);
    }

    /** Variant stack factory — the original's {@code new ItemStack(this, 1, meta)}. */
    public ItemStack stack(final int meta) {
        return metaStack(this, meta);
    }

    /** The original's {@code getBaubleType} switch, meta-for-meta (lines 57-70). */
    public static Set<AccessorySlot> slotsForMeta(final int meta) {
        return switch (meta) {
            case META_RING, META_APPRENTICE_FIRST, 4, 5, 6, 7, META_APPRENTICE_LAST ->
                    Set.of(AccessorySlot.RING);
            case META_BELT -> Set.of(AccessorySlot.BELT);
            default -> Set.of(AccessorySlot.AMULET);
        };
    }

    /**
     * The primal aspect tag an apprentice-ring meta maps to; null for
     * non-apprentice metas. The original indexed
     * {@code Aspect.getPrimalAspects().get(meta - 3)} — declaration order,
     * aer, terra, ignis, aqua, ordo, perditio ({@code api/aspects/
     * Aspect.java:17-22}), which the port's {@link AspectDefinitions}
     * declaration order preserves row-for-row. Tag-based so the mapping is
     * unit-testable without the live aspect registry.
     */
    public static String apprenticeTag(final int meta) {
        if (meta < META_APPRENTICE_FIRST || meta > META_APPRENTICE_LAST) {
            return null;
        }
        final List<String> primalTags = new java.util.ArrayList<>();
        for (final var definition : com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions.all()) {
            if (definition.primal()) {
                primalTags.add(definition.tag());
            }
        }
        return primalTags.get(meta - META_APPRENTICE_FIRST);
    }

    /** The primal aspect an apprentice-ring meta maps to (null before the registry exists). */
    public static Aspect apprenticeAspect(final int meta) {
        final String tag = apprenticeTag(meta);
        return tag == null ? null : Aspect.getAspect(tag);
    }

    /**
     * The original's discount rule (line 85): 1% when the queried aspect is
     * the stack's own primal. The original compared Aspect identity; port
     * aspects are registry singletons, so tag equality is the same test.
     */
    public static int visDiscountPercent(final int meta, final Aspect aspect) {
        final String own = apprenticeTag(meta);
        return own != null && aspect != null && own.equals(aspect.getTag()) ? 1 : 0;
    }

    @Override
    public Set<AccessorySlot> applicableSlots(final ItemStack stack) {
        return slotsForMeta(storedMeta(stack));
    }

    @Override
    public int getVisDiscount(final ItemStack stack, final Player player, final Aspect aspect) {
        return visDiscountPercent(storedMeta(stack), aspect);
    }

    @Override
    public int getRunicCharge(final ItemStack stack) {
        return 0;
    }

    @Override
    public Component getName(final ItemStack stack) {
        final Aspect own = apprenticeAspect(storedMeta(stack));
        if (own != null) {
            // Original composed the .3 name with a literal "%TYPE" replaced by
            // the aspect name (lines 92-96); the port passes the aspect name as
            // a translation argument — TC-M7-01 converts the lang key's %TYPE
            // placeholder to %s.
            return Component.translatable(
                    "item.thaumcraft.itembaubleblanks." + META_APPRENTICE_FIRST, own.getName());
        }
        return perMetaName(stack);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                               final List<Component> tooltip, final TooltipFlag flag) {
        final Aspect own = apprenticeAspect(storedMeta(stack));
        if (own != null) {
            // Original: DARK_PURPLE "<AspectName> tc.discount: 1%" (lines 98-102).
            tooltip.add(Component.literal(own.getName() + " ")
                    .append(Component.translatable("tc.discount"))
                    .append(": 1%")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
