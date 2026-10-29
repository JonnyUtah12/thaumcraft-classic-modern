package com.jonnyutah12.thaumcraftclassic.items.foci;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryItem;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.foci.FocusPouchContents;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The focus pouch (TC-M6-02): port of
 * {@code thaumcraft.common.items.wands.ItemFocusPouchBauble} +
 * {@code ItemFocusPouch} ({@code common/items/wands/ItemFocusPouch.java},
 * {@code ItemFocusPouchBauble.java}).
 *
 * <p>Ownership: the pouch is wand/focus work and belongs to TC-M6-02
 * (docs/catalog-triage.md, the TC-M6-02 section, and the note in
 * {@code registration/catalog/CatalogBaubles}), which is why it registers here
 * rather than with the TC-M8-02 baubles.</p>
 *
 * <h2>What the original was</h2>
 * An 18-slot, focus-only inventory whose contents lived on the pouch stack's
 * {@code "Inventory"} NBT list ({@link #getInventory}/{@link #setInventory},
 * {@code ItemFocusPouch.java:63-93}) and which opened a container GUI on
 * right-click ({@code func_77659a}, GUI id 5). It was registered as
 * {@code ItemFocusPouchBauble}, a {@code BaubleType.BELT} bauble with no
 * lifecycle callbacks ({@code ItemFocusPouchBauble.java:8-28}).</p>
 *
 * <h2>Port shape</h2>
 * <ul>
 *   <li>Contents live in the {@code thaumcraft:focus_pouch_contents} data
 *       component as a positional, bounded list ({@link FocusPouchContents}),
 *       so a slot index survives the save path the way the original's
 *       {@code "Slot"} byte did.</li>
 *   <li>The belt slot is expressed through {@link AccessoryItem}
 *       ({@link AccessorySlot#BELT}) — the port of {@code BaubleType.BELT}; the
 *       Curios bridge picks the item up by scanning {@code ModItems} for
 *       {@code AccessoryItem}s (TC-M8-01), so no code here depends on Curios.</li>
 *   <li>The container GUI (original {@code ContainerFocusPouch} /
 *       {@code GuiFocusPouch}, and the {@code GUI_FOCUS_POUCH} csv row) is
 *       TC-M7-04; the storage API below is what that GUI will drive. Right-click
 *       therefore does nothing yet rather than opening a screen that no slice
 *       has built.</li>
 * </ul>
 */
public class ItemFocusPouch extends Item implements AccessoryItem {

    /** The original pouch's slot count ({@code new ItemStack[18]}). */
    public static final int CAPACITY = FocusPouchContents.CAPACITY;

    public ItemFocusPouch(final Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------- inventory

    /** The pouch's contents ({@code ItemFocusPouch.getInventory} as a component). */
    public static FocusPouchContents contents(final ItemStack pouch) {
        return pouch.getOrDefault(ModDataComponents.FOCUS_POUCH_CONTENTS.get(), FocusPouchContents.EMPTY);
    }

    /** Writes the pouch's contents ({@code ItemFocusPouch.setInventory}). */
    public static void setContents(final ItemStack pouch, final FocusPouchContents contents) {
        if (contents.size() == 0) {
            pouch.remove(ModDataComponents.FOCUS_POUCH_CONTENTS.get());
        } else {
            pouch.set(ModDataComponents.FOCUS_POUCH_CONTENTS.get(), contents);
        }
    }

    /** Original {@code getInventory}: the fixed-length {@code ItemStack[18]}, empty slots included. */
    public static ItemStack[] getInventory(final ItemStack pouch) {
        return contents(pouch).toArray();
    }

    /** Original {@code setInventory}: writes a fixed-length array back, slot positions intact. */
    public static void setInventory(final ItemStack pouch, final ItemStack[] slots) {
        setContents(pouch, FocusPouchContents.fromArray(slots));
    }

    /** Every stored focus, in slot order ({@code getInventory} without the empty slots). */
    public static List<ItemStack> foci(final ItemStack pouch) {
        return contents(pouch).foci();
    }

    /** The stored focus with {@code tag}, if any. */
    public static Optional<ItemStack> find(final ItemStack pouch, final String tag) {
        return contents(pouch).byTag(tag);
    }

    /** Whether the pouch holds a focus with {@code tag}. */
    public static boolean hasFocus(final ItemStack pouch, final String tag) {
        return contents(pouch).find(tag) >= 0;
    }

    /**
     * The original {@code InventoryFocusPouch.isItemValidForSlot}
     * ({@code InventoryFocusPouch.java:69-71}): only an {@code ItemFocusBasic}
     * fits. {@link ItemFocus} is the port's focus class, which also makes a
     * nested pouch impossible (a pouch is not a focus) — the original was safe
     * for the same reason.
     */
    public static boolean isValidFocus(final ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemFocus;
    }

    /**
     * Stores {@code focus} in the lowest empty slot.
     *
     * @return true when it fit; false when the stack is not a focus or the pouch is full
     */
    public static boolean insert(final ItemStack pouch, final ItemStack focus) {
        if (!isValidFocus(focus)) {
            return false;
        }
        final FocusPouchContents current = contents(pouch);
        final int slot = current.firstFreeSlot();
        if (slot < 0) {
            return false;
        }
        setContents(pouch, current.with(slot, focus.copyWithCount(1)));
        return true;
    }

    /**
     * Removes and returns the stored focus with {@code tag} — the original's
     * drag-out of the pouch GUI.
     *
     * @return the removed focus, or empty when the pouch does not hold it
     */
    public static Optional<ItemStack> extract(final ItemStack pouch, final String tag) {
        final FocusPouchContents current = contents(pouch);
        final int slot = current.find(tag);
        if (slot < 0) {
            return Optional.empty();
        }
        final ItemStack removed = current.get(slot);
        setContents(pouch, current.with(slot, ItemStack.EMPTY));
        return Optional.of(removed);
    }

    /**
     * Takes the pouch's first stored focus without removing it — the lookup the
     * casting seam uses (see {@code casting/WandCasting}).
     */
    public static ItemStack firstFocus(final ItemStack pouch) {
        return contents(pouch).first();
    }

    // ------------------------------------------------------------- accessory

    /** Original {@code ItemFocusPouchBauble.getBaubleType} → {@code BaubleType.BELT}. */
    @Override
    public Set<AccessorySlot> applicableSlots(final ItemStack stack) {
        return Set.of(AccessorySlot.BELT);
    }

    /**
     * The original pouch's callbacks were all empty
     * ({@code ItemFocusPouchBauble.java:13-27}); stated explicitly so the
     * accessory contract is not left to a default.
     */
    @Override
    public void onWornTick(final ItemStack stack, final Player wearer) {
    }
}
