package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The original's {@code ItemCrystalEssence} (content-mapping.csv row
 * {@code thaumcraft:itemcrystalessence}, TC-M6-04a): like
 * {@link ItemWispEssence} but self-assigning — an unassigned crystal gets a
 * random aspect the moment it is crafted or enters an inventory (original
 * {@code onCraftedBy}/{@code onUpdate}), which is what the vis-crystal
 * harvest and crucible TRANS recipes rely on.
 *
 * <p>Tooltip/colour notes as in {@link ItemWispEssence}.</p>
 */
public class ItemCrystalEssence extends Item implements IEssentiaContainerItem {

    public ItemCrystalEssence(final Properties props) {
        super(props);
    }

    @Override
    public void inventoryTick(final ItemStack stack, final Level level, final net.minecraft.world.entity.Entity entity,
                              final int slotId, final boolean isSelected) {
        if (!level.isClientSide && getAspects(stack) == null) {
            assignRandomAspect(stack, level);
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public void onCraftedBy(final ItemStack stack, final Level level, final Player player) {
        if (getAspects(stack) == null) {
            assignRandomAspect(stack, level);
        }
    }

    private void assignRandomAspect(final ItemStack stack, final Level level) {
        final List<Aspect> all = Aspect.values();
        setAspects(stack, new AspectList().add(all.get(level.getRandom().nextInt(all.size())), 1));
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
