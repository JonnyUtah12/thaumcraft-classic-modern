package com.jonnyutah12.thaumcraftclassic.accessory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.jonnyutah12.thaumcraftclassic.accessory.curios.CuriosBridge;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single entry point of the accessory layer (TC-M8-01, D-005).
 *
 * <p>Provider selection happens once, at mod construction: with Curios
 * loaded the Curios 9.5.1 bridge is active and the fallback is inert (no
 * double-equip, no duplicate state); without it the built-in fallback is
 * active. The bridge class is only class-loaded on the Curios branch, so a
 * no-Curios runtime never touches a Curios type (guarded by
 * CuriosBridgeIsolationTest — no core class outside {@code accessory/curios}
 * may reference {@code top.theillusivec4.curios}).</p>
 *
 * <p>All gameplay effects (TC-M8-02 gear, TC-M6-07 runic armor) consume the
 * worn-set queries here and behave identically under either provider.</p>
 */
public final class AccessoryService {

    public static final String CURIOS_MOD_ID = "curios";

    private static final Logger LOGGER = LoggerFactory.getLogger("thaumcraft/accessories");
    private static final FallbackProvider FALLBACK = new FallbackProvider();

    private static volatile AccessoryProvider provider = FALLBACK;
    private static volatile boolean initialized;

    /** Called once from the mod constructor. */
    public static void init(final IEventBus modEventBus) {
        if (ModList.get().isLoaded(CURIOS_MOD_ID)) {
            provider = CuriosBridge.install(modEventBus);
            LOGGER.info("Accessory layer: Curios bridge active (slots {}",
                    java.util.Arrays.toString(
                            java.util.Arrays.stream(AccessorySlot.values()).map(AccessorySlot::curiosSlotId).toArray()));
        } else {
            provider = FALLBACK;
            LOGGER.info("Accessory layer: built-in fallback active (no accessory mod loaded)");
        }
        initialized = true;
    }

    /** The active provider; the fallback before {@link #init} (safe default). */
    public static AccessoryProvider provider() {
        return provider;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean curiosActive() {
        // id-based on purpose: comparing against CuriosBridge.provider() would
        // class-load the bridge (and its Curios types) on a no-Curios runtime.
        return "curios".equals(provider.id());
    }

    // ---- worn-set queries for effect code (provider-independent) ----

    /** Current worn accessories, ordered by slot then index. Never contains empties. */
    public static List<WornAccessory> worn(final Player player) {
        return provider.worn(player);
    }

    /** Just the worn stacks, in the same order. */
    public static List<ItemStack> wornStacks(final Player player) {
        final List<ItemStack> stacks = new ArrayList<>();
        for (final WornAccessory worn : worn(player)) {
            stacks.add(worn.stack());
        }
        return stacks;
    }

    public static boolean isWorn(final Player player, final Item item) {
        return worn(player).stream().anyMatch(w -> w.stack().is(item));
    }

    public static Optional<WornAccessory> findWorn(final Player player, final Predicate<ItemStack> predicate) {
        return worn(player).stream().filter(w -> predicate.test(w.stack())).findFirst();
    }

    public static List<WornAccessory> findAllWorn(final Player player, final Predicate<ItemStack> predicate) {
        final List<WornAccessory> matches = new ArrayList<>();
        for (final WornAccessory worn : worn(player)) {
            if (predicate.test(worn.stack())) {
                matches.add(worn);
            }
        }
        return matches;
    }

    private AccessoryService() {
    }
}
