package com.jonnyutah12.thaumcraftclassic.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;

/**
 * The original's {@code ItemNuggetEdible} (content-mapping.csv rows
 * {@code thaumcraft:itemnuggetchicken/beef/pork/fish}, TC-M6-04a): one
 * class instantiated four times, once per meat nugget. Food values are the
 * original's {@code super(1, 0.3F, false)} — nutrition 1, saturation 0.3 —
 * and the 10-tick eat time is the original's {@code func_77626_a} override.
 *
 * <p>The original's {@code isMeat=true}-style flag has no 1.21.1 equivalent
 * in FoodProperties (meat-ness is not modelled in 1.21.1 at all); the
 * original passed {@code false} here anyway.</p>
 */
public class ItemNuggetEdible extends Item {

    /** Original field_77855_a: 10-tick eat duration. */
    private static final int EAT_DURATION_TICKS = 10;

    public ItemNuggetEdible(final Properties props) {
        super(props);
    }

    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity entity) {
        return EAT_DURATION_TICKS;
    }
}
