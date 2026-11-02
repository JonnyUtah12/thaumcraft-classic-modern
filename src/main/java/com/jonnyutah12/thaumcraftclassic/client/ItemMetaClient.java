package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemMetaTints;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * BD-17 (t_f04c44fb) — the client half of the per-metadata appearance work:
 * the item colour handlers the original attached to its item classes
 * ({@code Item.getColorFromItemStack}).
 *
 * <p>The model half is data (the {@code overrides} arrays in
 * {@code models/item/*.json}, keyed on the {@code minecraft:custom_model_data}
 * mirror of {@code thaumcraft:item_meta} — see {@link
 * com.jonnyutah12.thaumcraftclassic.items.ItemMetaAppearance}), so nothing has
 * to be registered for it. Tinting is code: NeoForge 21.1.259 exposes
 * {@link RegisterColorHandlersEvent.Item} (mod bus, client only, measured in
 * the 21.1.259 sources) and the handler is asked per render layer, which is
 * the same (stack, pass) pair 1.7.10 passed to
 * {@code getColorFromItemStack}. The maths lives in {@link ItemMetaTints}
 * (server-loadable) so {@code ItemMetaTintTest} can assert the numbers
 * without a client.</p>
 *
 * <p>The items are resolved from the live registry by id, so this class cannot
 * drift from the catalogue: an id the table colours that is not shipped logs a
 * warning and the game test fails it.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ItemMetaClient {

    @SubscribeEvent
    public static void onRegisterItemColors(final RegisterColorHandlersEvent.Item event) {
        int registered = 0;
        for (final String path : ItemMetaTints.tintedIds()) {
            final Item item = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path));
            if (item == null || !BuiltInRegistries.ITEM.containsKey(
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path))) {
                ThaumcraftClassic.LOGGER.warn("[BD-17] the tint table colours '{}' but that id is not registered "
                        + "— the entry is dead and the item renders untinted", path);
                continue;
            }
            final ItemColor color = (stack, layer) -> ItemMetaTints.color(path, stack, layer);
            event.register(color, item);
            registered++;
        }
        ThaumcraftClassic.LOGGER.info("[BD-17] item colour handlers registered for {} of {} tinted ids "
                        + "(the original's Item.getColorFromItemStack)",
                registered, ItemMetaTints.tintedIds().size());
    }

    private ItemMetaClient() {
    }
}
