package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The original's {@code ItemNugget} (content-mapping.csv row
 * {@code thaumcraft:itemnugget}, TC-M6-04a): metal nuggets, quicksilver
 * drops and native clusters in one damage-value item. Valid metas are
 * {@code 0-7} (iron, copper, tin, silver, lead, quicksilver drop, thaumium,
 * void metal), {@code 16-21} (native iron/copper/tin/silver/lead/cinnabar
 * clusters) and {@code 31} (native gold cluster) — the original's icon array
 * is 64 wide but only these are ever used.
 *
 * <p>Meta model identical to {@link ItemResource}: the
 * {@code thaumcraft:item_meta} component (default 0) carries the original
 * damage value; per-meta lang keys are {@code item.thaumcraft.itemnugget.<meta>}.
 * The smelting/crucible recipes keyed to these metas ride with their recipe
 * owners (TC-M6-04c furnace, TC-M4-05 crucible).</p>
 */
public class ItemNugget extends Item {

    /** The original's used damage values (creative listing + ore-dictionary). */
    public static final Set<Integer> VALID_METAS;

    static {
        final Set<Integer> metas = new TreeSet<>();
        for (int meta = 0; meta <= 7; meta++) {
            metas.add(meta);
        }
        for (int meta = 16; meta <= 21; meta++) {
            metas.add(meta);
        }
        metas.add(31);
        VALID_METAS = Set.copyOf(metas);
    }

    public ItemNugget(final Properties props) {
        super(props);
    }

    /** A stack of one original-metadata nugget/cluster, original {@code new ItemStack(item, 1, meta)}. */
    public static ItemStack stack(final int meta) {
        if (!VALID_METAS.contains(meta)) {
            throw new IllegalArgumentException("ItemNugget meta out of range: " + meta);
        }
        final ItemStack stack = new ItemStack(CatalogResources.ITEM_NUGGET.get());
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
