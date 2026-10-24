package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The original's {@code ItemShard} (content-mapping.csv row
 * {@code thaumcraft:itemshard}, TC-M6-04a): six primal shards plus the
 * balanced shard (metas 0-6: air, fire, water, earth, order, entropy,
 * balanced). Pure data item — the original has no behavior beyond per-meta
 * names and the creative-tab listing; the shard→crystal crafting and the
 * vis-crystal ore-dictionary entries ride with their recipe owners.
 *
 * <p>Meta model identical to {@link ItemResource}: the
 * {@code thaumcraft:item_meta} component (default 0) carries the original
 * damage value; per-meta lang keys are
 * {@code item.thaumcraft.itemshard.<meta>}.</p>
 */
public class ItemShard extends Item {

    /** Original metas 0..6 (meta 6 = balanced shard). */
    public static final int META_COUNT = 7;

    public ItemShard(final Properties props) {
        super(props);
    }

    /** A stack of one original-metadata shard, original {@code new ItemStack(item, 1, meta)}. */
    public static ItemStack stack(final int meta) {
        if (meta < 0 || meta >= META_COUNT) {
            throw new IllegalArgumentException("ItemShard meta out of range: " + meta);
        }
        final ItemStack stack = new ItemStack(com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources.ITEM_SHARD.get());
        stack.set(ModDataComponents.ITEM_META.get(), meta);
        return stack;
    }

    @Override
    public Component getName(final ItemStack stack) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath() + "."
                + stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0));
    }
}
