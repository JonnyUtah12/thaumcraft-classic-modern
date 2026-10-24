package com.jonnyutah12.thaumcraftclassic.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Purifying bath salts (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft/common/items/ItemBathSalts.java}
 * (Thaumcraft:ItemBathSalts). The whole behaviour is the dropped-item
 * lifespan: 200 ticks instead of the vanilla 6000 (the spa tile
 * {@code TileSpa}, already ported, consumes them from the player's
 * inventory; the original relied on the short despawn so spilled salts
 * vanish quickly). The original's {@code setHasSubtypes(false)} is the
 * 1.21.1 default and is dropped.</p>
 */
public class ItemBathSalts extends Item {

    /** Original {@code getEntityLifespan} return value. */
    public static final int DROPPED_LIFETIME_TICKS = 200;

    public ItemBathSalts() {
        super(new Item.Properties());
    }

    @Override
    public int getEntityLifespan(final ItemStack itemStack, final Level level) {
        return DROPPED_LIFETIME_TICKS;
    }
}
