package com.jonnyutah12.thaumcraftclassic.entities;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCrucible;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

/**
 * Entity lifecycle hooks (original {@code thaumcraft.common.lib.events
 * .EventHandlerEntity}). TC-M4-05 needs the thrower tag: the crucible's
 * research gate ({@code findMatchingCrucibleRecipe(username, …)}) reads the
 * {@code "thrower"} entity-data string the original stamped on every
 * player-thrown item.
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class EventHandlerEntity {

    @SubscribeEvent
    public static void onItemToss(final ItemTossEvent event) {
        event.getEntity().getEntityData().setString(TileCrucible.THROWER_TAG,
                event.getPlayer().getGameProfile().getName());
    }

    private EventHandlerEntity() {
    }
}
