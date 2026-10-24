package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The original's {@code ItemWispEssence} (content-mapping.csv row
 * {@code thaumcraft:itemwispessence}, TC-M6-04a): a single item whose
 * per-aspect identity lives in the essentia-container component (the
 * original read the same from the "Aspects" NBT). The aspect is assigned by
 * whoever produces the stack — melt/crucible recipes (TC-M4-05) and the
 * creative-tab per-aspect listing (TC-M6-04 tab wiring) — never by the item
 * itself, matching the original.
 *
 * <p>The original's tooltip hid undiscovered aspects; 1.21.1's
 * appendHoverText has no player context, so names show unconditionally
 * (knowledge gate deferred with the client knowledge mirror, TC-M2-02/M7).
 * The aspect-tinted item colour the original drew from the same NBT is a
 * client colour-handler concern (TC-M7).</p>
 */
public class ItemWispEssence extends Item implements IEssentiaContainerItem {

    public ItemWispEssence(final Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final AspectList aspects = getAspects(stack);
        if (aspects != null) {
            for (final Aspect aspect : aspects.getAspectsSorted()) {
                tooltip.add(Component.literal(aspect.getName() + " x" + aspects.getAmount(aspect)));
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
