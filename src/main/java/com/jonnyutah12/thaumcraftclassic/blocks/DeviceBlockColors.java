package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * The colour handlers for the appearances 1.7.10 computed from live data
 * (BD-18, t_065502ba).
 *
 * <h2>Candle (16 dye colours)</h2>
 * {@code BlockCandle.java:76-78} returned {@code Utils.colors[metadata]} from
 * {@code getBlockColor} (and the same value at {@code :52-54} for
 * {@code colorRepresentative}); the sprite is one greyscale stem shared by all
 * sixteen metas, so the colour was never in the art — it was a tint. The port
 * carries the metadata verbatim as the {@code variant} property (D-001), and
 * SPINE-4 shipped the stem with {@code tintindex 0} on every face, so the
 * handler below is the whole feature: {@link DeviceTints#colourForVariant(int)}
 * of the state's variant.
 *
 * <p>The same colour has to resolve for the item forms too — hand, inventory,
 * and the item frame, which is exactly the case the card called out. Those
 * render the {@code models/item/blockcandle.json} chain (a 3D parent of the
 * tinted stem) through {@code ItemColors}, with no {@code Level} and no state
 * to read, so the value comes off the stack's variant carrier
 * ({@link VariantStateItems#variantOf}). A stack carrying no variant — a plain
 * creative-tab stack — draws the untinted sprite and returns
 * {@code 0xFFFFFF}: that is the original's white candle, not a guess.</p>
 *
 * <h2>Levitator glow shell</h2>
 * The original tinted the shell faces in the renderer:
 * {@code BlockLifterRenderer.java:57} set the colour to
 * {@code BlockCustomOreItem.colors[4]} for the bottom glow face and
 * {@code :68} to {@code 0xDD11FF} for the four side faces. The port's
 * {@code blocklifter_lifter_glow} model carries those as {@code tintindex 0}
 * and {@code tintindex 1}, and this handler resolves both. The levitator ships
 * as a block with no {@code BlockItem} (see {@code CatalogFurniture}), so the
 * original's 3D inventory shells ({@code renderInventoryBlock}, lines 17-47)
 * have no port-side counterpart to tint — recorded in BD-18, not faked here.
 *
 * <h2>Why the bus is spelled out</h2>
 * {@code RegisterColorHandlersEvent} fires on the <b>mod</b> bus during client
 * setup; the annotation's default is the <b>game</b> bus, so a
 * subscriber that omits {@code bus =} never runs and the block silently draws
 * untinted. That is the defect {@code DeviceTintRegistrationTest} pins for
 * every colour subscriber in the port, including the pre-existing
 * {@link OreBlockColors}.
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DeviceBlockColors {

    /** White: the untinted sprite, which for the candle is the original's white variant. */
    private static final int NO_TINT = 0xFFFFFF;

    @SubscribeEvent
    public static void registerBlockColors(final RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) ->
                        DeviceTints.colourForVariant(state.getValue(BlockCandle.VARIANT)),
                CatalogDecorativeBlocks.BLOCK_CANDLE.get());
        event.register((state, level, pos, tintIndex) -> switch (tintIndex) {
            case 0 -> DeviceTints.LIFTER_GLOW_BOTTOM;
            case 1 -> DeviceTints.LIFTER_GLOW_SIDE;
            default -> NO_TINT;
        }, CatalogFurniture.BLOCK_LIFTER.get());
    }

    @SubscribeEvent
    public static void registerItemColors(final RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            final Integer variant = VariantStateItems.variantOf(stack);
            return variant == null ? NO_TINT : DeviceTints.colourForVariant(variant);
        }, CatalogDecorativeBlocks.ITEM_CANDLE.get());
    }

    private DeviceBlockColors() {
    }
}
