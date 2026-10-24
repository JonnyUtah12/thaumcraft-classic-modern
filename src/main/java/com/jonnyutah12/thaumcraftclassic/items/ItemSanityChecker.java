package com.jonnyutah12.thaumcraftclassic.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Sanity checker relic (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft/common/items/relics/ItemSanityChecker.java}
 * (Thaumcraft:ItemSanityChecker). Stack size 1, uncommon rarity; the
 * original has no use logic — it is a research/curio relic (its flavour text
 * is lang content, TC-M7-01). The original's {@code setMaxDamage(0)} is the
 * 1.21.1 "not damageable" default and is dropped.</p>
 */
public class ItemSanityChecker extends Item {

    public ItemSanityChecker() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }
}
