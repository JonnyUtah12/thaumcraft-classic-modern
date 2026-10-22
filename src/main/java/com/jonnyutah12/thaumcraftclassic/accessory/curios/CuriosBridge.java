package com.jonnyutah12.thaumcraftclassic.accessory.curios;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryItem;
import com.jonnyutah12.thaumcraftclassic.registration.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Optional Curios 9.5.1 bridge (TC-M8-01, D-005). This package is the only
 * place in the mod allowed to reference {@code top.theillusivec4.curios}
 * types — enforced by CuriosBridgeIsolationTest (bytecode guard) — and its
 * classes are only loaded when Curios is actually present
 * ({@code AccessoryService.init} branches on {@code ModList.isLoaded}).
 *
 * <p>Two halves, both verified against the real ATM10 jar via javap
 * (docs/architecture/accessories.md):</p>
 * <ul>
 *   <li><b>Slots</b> are data-driven in 9.5.1: our slot types ship as
 *       datapack files ({@code data/thaumcraft/curios/slots/*.json}, granted
 *       to players via {@code curios/entities/baubles.json}, items routed via
 *       {@code curios:<slot>} item tags). No slot registration code.</li>
 *   <li><b>Behavior</b> is attached with {@code CuriosApi.registerCurio(Item,
 *       ICurioItem)} for every registered item implementing our own
 *       {@link AccessoryItem} — item classes never implement Curios types
 *       themselves (the load-safety mechanism from the sibling project's
 *       verified design).</li>
 * </ul>
 */
public final class CuriosBridge {

    private static final CuriosProvider PROVIDER = new CuriosProvider();
    private static volatile boolean installed;

    /** Called once from AccessoryService.init when Curios is loaded. */
    public static CuriosProvider install(final IEventBus modEventBus) {
        if (!installed) {
            installed = true;
            modEventBus.addListener(CuriosBridge::onCommonSetup);
        }
        return PROVIDER;
    }

    public static CuriosProvider provider() {
        return PROVIDER;
    }

    private static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            int count = 0;
            for (final var entry : ModItems.ITEMS.getEntries()) {
                final Item item = entry.get();
                if (item instanceof AccessoryItem accessory) {
                    registerItem(item, accessory);
                    count++;
                }
            }
            ThaumcraftClassic.LOGGER.info(
                    "Curios accessory bridge: attached behavior for {} accessory item(s)", count);
        });
    }

    /**
     * Attaches our accessory behavior to one item under Curios. Automatic for
     * everything in {@code ModItems} at common setup; public for late or
     * out-of-holder registrations.
     */
    public static void registerItem(final Item item, final AccessoryItem behavior) {
        CuriosApi.registerCurio(item, new AccessoryCurioItem(behavior));
    }

    /**
     * Gametest probe: the registered size of one of our Curios slot types for
     * this player, or -1 when the slot type is absent. Lives here so the
     * game-test class itself stays free of Curios types (isolation guard).
     */
    public static int registeredSlotSize(final net.minecraft.world.entity.player.Player player,
                                        final com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot slot) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inv -> inv.getStacksHandler(slot.curiosSlotId()))
                .map(top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler::getSlots)
                .orElse(-1);
    }

    private CuriosBridge() {
    }
}
