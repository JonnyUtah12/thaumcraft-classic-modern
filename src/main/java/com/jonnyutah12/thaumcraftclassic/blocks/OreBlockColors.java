package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Block color resolver for the infused-stone overlay (TC-M5-05 assets).
 *
 * <p>The original BlockCustomOreRenderer tinted the animated infusedore
 * overlay per metadata via glColor3f with {@code BlockCustomOreItem.colors};
 * the port carries the same values through the model's tintindex 0 faces.
 * Cinnabar/amber models have no tinted faces, so they are unaffected.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT)
public final class OreBlockColors {

    @SubscribeEvent
    public static void registerBlockColors(final RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) ->
                        tintIndex == 0 ? BlockCustomOreItem.COLORS[variantOf(state)] : 0xFFFFFF,
                ModBlocks.BLOCK_CUSTOM_ORE.get());
    }

    private static int variantOf(final BlockState state) {
        final int variant = state.getValue(BlockCustomOre.VARIANT);
        return Math.min(Math.max(variant, 0), BlockCustomOreItem.COLORS.length - 1);
    }

    private OreBlockColors() {
    }
}
