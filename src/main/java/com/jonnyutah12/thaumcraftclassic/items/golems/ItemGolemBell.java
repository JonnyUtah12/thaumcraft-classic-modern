package com.jonnyutah12.thaumcraftclassic.items.golems;

import net.minecraft.world.item.Item;

/**
 * Golemancer's Bell (content-mapping.csv {@code thaumcraft:golembell}) — the
 * original's {@code thaumcraft.common.entities.golems.ItemGolemBell}.
 *
 * <p>Substrate only. The original's item class carries the marker
 * infrastructure the golem tasks run on — the static {@code getGolemId},
 * {@code getGolemHomeFace}, {@code getGolemHomeCoords}, {@code getMarkers},
 * {@code resetMarkers} and {@code changeMarkers} helpers, and the
 * {@code onItemRightClick} / {@code onEntityInteract} bodies that select a
 * golem, link or clear markers and pick a golem up. All of that needs the
 * {@code Marker} list and the golem's marker state, which is TC-M6-08-s-b
 * ("{@code GolemHelper} markers"), and the bell's on-golem interaction sits
 * with it. What this card owns is the registered item: the original
 * constructor's {@code func_77625_d(1)} (max stack size 1, no subtypes) and
 * its {@code item.GolemBell.name} → "Golemancer's Bell" label.</p>
 */
public class ItemGolemBell extends Item {

    public ItemGolemBell(final Properties properties) {
        super(properties);
    }
}
