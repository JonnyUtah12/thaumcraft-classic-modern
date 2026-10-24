package com.jonnyutah12.thaumcraftclassic.items;

import net.minecraft.world.item.Item;

/**
 * The original's {@code ItemTripleMeatTreat} (content-mapping.csv row
 * {@code thaumcraft:triplemeattreat}, TC-M6-04a). All of the original's
 * behavior is constructor data — {@code super(6, 0.8F, true)} plus
 * {@code setAlwaysEdible()} plus
 * {@code setPotionEffect(Potion.regeneration.id, 5, 0, .66F)} (5 seconds =
 * 100 ticks of regeneration at 66% chance) — and that lives in the food
 * properties attached at registration (CatalogResources). The default 32-tick
 * eat duration is unchanged, as in the original.
 */
public class ItemTripleMeatTreat extends Item {

    public ItemTripleMeatTreat(final Properties props) {
        super(props);
    }
}
