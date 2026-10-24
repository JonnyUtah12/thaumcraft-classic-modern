package com.jonnyutah12.thaumcraftclassic.items;

import net.minecraft.world.item.Item;

/**
 * Arcane key (content-mapping.csv {@code thaumcraft:arcanedoorkey},
 * "Iron Arcane Key / Gold Arcane Key").
 *
 * <p>Original: {@code thaumcraft.common.items.ItemKey} — a plain item with
 * two metadata variants (iron/gold); in 4.2.3.5 it has no behaviour code at
 * all (the gold key is a decorative/quest item; nothing reads it). The
 * single id contract row maps to one item; the two visual variants are a
 * model/texturing concern (TC-M7-01) via a {@code key_type} data component
 * when the art lands.</p>
 */
public class ItemKey extends Item {

    public ItemKey(final Item.Properties props) {
        super(props);
    }
}
