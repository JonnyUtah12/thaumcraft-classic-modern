package com.jonnyutah12.thaumcraftclassic.entities;

import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Traveling Trunk seam (content-mapping.csv {@code thaumcraft:trunkspawner}
 * and {@code thaumcraft:GUI_TRUNK}).
 *
 * <p>The trunk entity itself ({@code EntityTravelingTrunk}, CSV row
 * {@code thaumcraft:travelingtrunk}) is owned by TC-M6-06. This card owns the
 * spawner item and the trunk GUI, so both are written against this interface;
 * TC-M6-06's entity implements it. The method set is exactly what the original
 * call sites need:</p>
 * <ul>
 *   <li>{@code ItemTrunkSpawner.onItemUse} → setOwner / setCustomName /
 *       setUpgrade+setInvSize / {@code inventory.readFromNBT} — here
 *       {@link #setTrunkOwner(String)}, {@link #setTrunkUpgrade(byte)} and
 *       {@link #setTrunkStoredInventory(ItemContainerContents)} (the item's
 *       stored-inventory component, the original's "inventory" NBT).</li>
 *   <li>{@code ContainerTravelingTrunk} → {@code trunk.inventory},
 *       {@code trunk.getRows()}, {@code setOpen}, the stay-toggle button —
 *       here {@link #trunkInventory()}, {@link #trunkRows()},
 *       {@link #setTrunkOpen(boolean)} and {@link #toggleTrunkStay()}.</li>
 * </ul>
 */
public interface TrunkHost {

    /** The entity backing the trunk (position/sound access for the menu). */
    Entity asEntity();

    /** Original {@code EntityTravelingTrunk.setOwner(playerName)}. */
    void setTrunkOwner(String owner);

    /**
     * Original {@code setUpgrade(byte)} + {@code setInvSize()}: apply the
     * stored golem-upgrade byte and recompute the inventory size.
     */
    void setTrunkUpgrade(byte upgrade);

    /** Original {@code inventory.readFromNBT(stackNBT)} on spawn. */
    void setTrunkStoredInventory(ItemContainerContents contents);

    /** Original {@code trunk.inventory} (InventoryTrunk) as a live container. */
    Container trunkInventory();

    /** Original {@code trunk.getRows()}: inventory rows of 9 for the GUI. */
    int trunkRows();

    /** Original {@code setOpen}: gates the trunk's follow behaviour while open. */
    void setTrunkOpen(boolean open);

    /** Original stay-toggle button: flip the follow/stay mode. */
    void toggleTrunkStay();
}
