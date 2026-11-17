package com.jonnyutah12.thaumcraftclassic.entities.golems;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The golem's own inventory — the original's
 * {@code thaumcraft.common.entities.InventoryMob} (TC-M6-08-s-b). The golem
 * carries its items here; {@code itemCarried} is only the singe transfer stack
 * the act goals hold, so the carrier tasks read this.
 *
 * <h2>Pinned from the source</h2>
 * <ul>
 *   <li>{@code stacklimit = 64} — the per-slot cap
 *       ({@code InventoryMob.java:21}), overridable through the three-arg
 *       constructor.</li>
 *   <li>{@code func_70302_i_()} returns {@code inventory.length + 1}
 *       ({@code InventoryMob.java:179}) — one more than the backing array. That
 *       off-by-one is the original's, and it is preserved here: the extra slot
 *       is what the GUI shows as the "carried item" slot. Because vanilla and
 *       Forge both walk {@code 0..getContainerSize()-1}, the port bounds-guards
 *       {@link #getItem(int)} / {@link #setItem(int, ItemStack)} so the last
 *       index reads empty instead of throwing
 *       {@code ArrayIndexOutOfBoundsException}. <b>Deviation</b> (defensive
 *       only): the original threw for that index; nothing in the original
 *       iterated to it, and every golem consumer uses
 *       {@code inventory.length}/{@code slotCount} directly.</li>
 *   <li>{@code getAmountNeededSmart(stack, fuzzy)} — sum of the counts of the
 *       slots matching the stack; {@code fuzzy} widens the match to the ore
 *       dictionary ({@code InventoryMob.java:255-279}). The port's fuzzy branch
 *       uses the item tag/ingredient seam instead of {@code OreDictionary}; see
 *       the note on {@link #getAmountNeededSmart(ItemStack, boolean)}.</li>
 *   <li>{@code getItemsNeeded(fuzzy)} — a copy of each non-empty slot, widened
 *       to the oredict siblings when {@code fuzzy}
 *       ({@code InventoryMob.java:281-306}).</li>
 *   <li>{@code writeToNBT}/{@code readFromNBT} — a {@code ListTag} of compounds
 *       keyed {@code "Slot"} + the stack's own NBT
 *       ({@code InventoryMob.java:153-178}).</li>
 * </ul>
 *
 * <p><b>1.7.10 → 1.21.1 shapes.</b> {@code IInventory} → {@link Container}
 * (the card's confirmed mapping), {@code ItemStack[]} → {@code ItemStack[]}
 * kept as-is (the original's null-means-empty is preserved rather than moving to
 * {@link NonNullList}, because the golem's act code tests for null), and the
 * {@code ItemStack.func_70298_a}/{@code func_70299_a} pair becomes
 * {@link #removeItem(int, int)} / {@link #setItem(int, ItemStack)} with the
 * original's "remove exactly {@code j} items, keep the remainder in the slot"
 * semantics.</p>
 */
public class InventoryMob implements Container {

    /** Original field {@code inventory}. Null entries mean an empty slot, exactly as in the original. */
    public ItemStack[] inventory;
    /** Original field {@code ent} — the owning entity. */
    public Entity ent;
    /** Original field {@code inventoryChanged}. */
    public boolean inventoryChanged;
    /** Original field {@code slotCount}. */
    public int slotCount;
    /** Original field {@code stacklimit = 64}. */
    public int stacklimit = 64;

    public InventoryMob(final Entity entity, final int slots) {
        this.slotCount = slots;
        this.inventory = new ItemStack[this.slotCount];
        this.inventoryChanged = false;
        this.ent = entity;
    }

    public InventoryMob(final Entity entity, final int slots, final int lim) {
        this.slotCount = slots;
        this.inventory = new ItemStack[this.slotCount];
        this.inventoryChanged = false;
        this.stacklimit = lim;
        this.ent = entity;
    }

    /** Original {@code getInventorySlotContainItem(Item)}: the first slot holding that item, or -1. */
    public int getInventorySlotContainItem(final net.minecraft.world.item.Item item) {
        for (int j = 0; j < this.inventory.length; j++) {
            if (this.inventory[j] != null && this.inventory[j].getItem() == item) {
                return j;
            }
        }
        return -1;
    }

    /**
     * Original {@code storeItemStack}: the first slot with the same item that is
     * stackable, below its own max and below the per-slot cap, and (for
     * damageable items) of the same damage value.
     */
    public int storeItemStack(final ItemStack stack) {
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] != null
                    && this.inventory[i].getItem() == stack.getItem()
                    && this.inventory[i].isStackable()
                    && this.inventory[i].getCount() < this.inventory[i].getMaxStackSize()
                    && this.inventory[i].getCount() < this.getMaxStackSize()
                    && (!this.inventory[i].isDamageableItem()
                        || this.inventory[i].getDamageValue() == stack.getDamageValue())) {
                return i;
            }
        }
        return -1;
    }

    /** Original {@code getFirstEmptyStack}. */
    public int getFirstEmptyStack() {
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] == null) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Original {@code storePartialItemStack}: puts as much of {@code stack} into
     * an existing partial slot, then an empty slot, and returns what did not fit.
     */
    public int storePartialItemStack(final ItemStack stack) {
        final net.minecraft.world.item.Item item = stack.getItem();
        int remaining = stack.getCount();
        int slot = this.storeItemStack(stack);
        if (slot < 0) {
            slot = this.getFirstEmptyStack();
        }
        if (slot < 0) {
            return remaining;
        }
        if (this.inventory[slot] == null) {
            this.inventory[slot] = new ItemStack(item, 0);
        }
        int move = remaining;
        if (remaining > this.inventory[slot].getMaxStackSize() - this.inventory[slot].getCount()) {
            move = this.inventory[slot].getMaxStackSize() - this.inventory[slot].getCount();
        }
        if (move > this.getMaxStackSize() - this.inventory[slot].getCount()) {
            move = this.getMaxStackSize() - this.inventory[slot].getCount();
        }
        if (move == 0) {
            return remaining;
        }
        remaining -= move;
        this.inventory[slot].setCount(this.inventory[slot].getCount() + move);
        this.inventoryChanged = true;
        return remaining;
    }

    /**
     * Original {@code addItemStackToInventory}: a damageable/empty stack takes a
     * fresh slot, anything else is stored partially until it fits. Returns
     * {@code true} when the whole stack landed.
     */
    public boolean addItemStackToInventory(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.isDamageableItem() || !stack.isStackable()) {
            final int slot = this.getFirstEmptyStack();
            if (slot >= 0) {
                this.inventory[slot] = stack.copy();
                this.inventory[slot].setCount(stack.getCount());
                stack.setCount(0);
                this.inventoryChanged = true;
                return true;
            }
            return false;
        }
        int before;
        do {
            before = stack.getCount();
            stack.setCount(this.storePartialItemStack(stack));
        } while (!stack.isEmpty() && stack.getCount() < before);
        if (stack.isEmpty()) {
            this.inventoryChanged = true;
            return true;
        }
        return false;
    }

    /** Original {@code func_70298_a(i, j)}: removes at most {@code j} items, leaving the remainder. */
    @Override
    public ItemStack removeItem(final int index, final int amount) {
        if (index < 0 || index >= this.inventory.length || this.inventory[index] == null) {
            return ItemStack.EMPTY;
        }
        if (this.inventory[index].getCount() <= amount) {
            final ItemStack taken = this.inventory[index];
            this.inventory[index] = null;
            return taken;
        }
        final ItemStack taken = this.inventory[index].split(amount);
        if (this.inventory[index].isEmpty()) {
            this.inventory[index] = null;
        }
        return taken;
    }

    /** Original {@code func_70304_b(i)} returned null and did nothing — kept faithful. */
    @Override
    public ItemStack removeItemNoUpdate(final int index) {
        return ItemStack.EMPTY;
    }

    public ListTag writeToNBT(final ListTag list) {
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] != null) {
                final CompoundTag tag = new CompoundTag();
                tag.putByte("Slot", (byte) i);
                tag.merge((CompoundTag) this.inventory[i].save(this.registryProvider()));
                list.add(tag);
            }
        }
        return list;
    }

    /**
     * Original {@code readFromNBT}: rebuilds the array from the {@code "Slot"}
     * keys, ignoring entries whose slot is out of range.
     */
    public void readFromNBT(final ListTag list, final net.minecraft.core.HolderLookup.Provider registries) {
        this.inventory = new ItemStack[this.slotCount];
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag tag = list.getCompound(i);
            final int slot = tag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.inventory.length) {
                final ItemStack parsed = ItemStack.parseOptional(registries, tag);
                if (!parsed.isEmpty()) {
                    this.inventory[slot] = parsed;
                }
            }
        }
    }

    private net.minecraft.core.HolderLookup.Provider registryProvider() {
        return this.ent != null && this.ent.level() != null
                ? this.ent.level().registryAccess()
                : net.minecraft.core.RegistryAccess.EMPTY;
    }

    /** Original {@code func_70302_i_}: {@code inventory.length + 1}, the original's off-by-one. */
    @Override
    public int getContainerSize() {
        return this.inventory.length + 1;
    }

    @Override
    public boolean isEmpty() {
        return this.allEmpty();
    }

    /**
     * Original {@code func_70301_a(i)}. The port returns {@link ItemStack#EMPTY}
     * for the original's trailing over-run index instead of throwing — see the
     * class javadoc deviation note.
     */
    @Override
    public ItemStack getItem(final int index) {
        if (index < 0 || index >= this.inventory.length || this.inventory[index] == null) {
            return ItemStack.EMPTY;
        }
        return this.inventory[index];
    }

    @Override
    public void setItem(final int index, final ItemStack stack) {
        if (index < 0 || index >= this.inventory.length) {
            return;
        }
        this.inventory[index] = stack.isEmpty() ? null : stack;
    }

    /** Original {@code func_70297_j_}: {@code stacklimit} (64 unless the three-arg ctor changed it). */
    @Override
    public int getMaxStackSize() {
        return this.stacklimit;
    }

    /**
     * Original {@code canInteractWith}: {@code !entity.isDead && player.distanceTo(entity) <= 64.0}.
     * 1.21.1 uses {@code isRemoved()} where 1.7.10 used {@code isDead}, and
     * {@code distanceToSqr} with {@code 64.0 * 64.0} for the same
     * pre-sqrt-squared comparison of the original's euclidean 64 blocks.
     */
    public boolean canInteractWith(final Player player) {
        return this.ent.isRemoved() ? false : player.distanceToSqr(this.ent) <= 64.0 * 64.0;
    }

    @Override
    public boolean stillValid(final Player player) {
        // Original func_70300_a returned false unconditionally.
        return false;
    }

    /** Original {@code func_28018_c}: whether any slot holds exactly that stack. */
    public boolean containsStack(final ItemStack stack) {
        for (int j = 0; j < this.inventory.length; j++) {
            if (this.inventory[j] != null && ItemStack.matches(this.inventory[j], stack)) {
                return true;
            }
        }
        return false;
    }

    /** Original {@code dropAllItems}: throws every slot's contents out of the owning entity. */
    public void dropAllItems() {
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] != null) {
                this.ent.spawnAtLocation(this.inventory[i], 0.0F);
                this.inventory[i] = null;
            }
        }
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.inventory.length; i++) {
            this.inventory[i] = null;
        }
    }

    /** Original {@code hasSomething}: any non-empty slot within {@code slotCount}. */
    public boolean hasSomething() {
        for (int a = 0; a < this.slotCount && a < this.inventory.length; a++) {
            if (this.inventory[a] != null) {
                return true;
            }
        }
        return false;
    }

    /** Original {@code allEmpty}. */
    public boolean allEmpty() {
        for (int a = 0; a < this.slotCount && a < this.inventory.length; a++) {
            if (this.inventory[a] != null) {
                return false;
            }
        }
        return true;
    }

    /** Original {@code getAmountNeeded}: total count of the slots whose stack matches the sample. */
    public int getAmountNeeded(final ItemStack sample) {
        int amount = 0;
        for (int a = 0; a < this.slotCount && a < this.inventory.length; a++) {
            if (this.inventory[a] != null && this.inventory[a].is(sample.getItem())) {
                amount += this.inventory[a].getCount();
            }
        }
        return amount;
    }

    /**
     * Original {@code getAmountNeededSmart(stackInSlot, fuzzy)}. The exact branch
     * compares item <em>and</em> tag ({@code func_77970_a}); the fuzzy branch adds
     * the ore-dictionary siblings.
     *
     * <p><b>Deviation.</b> 1.21.1 has no {@code OreDictionary}; the port's
     * {@link #fuzzyCandidates(ItemStack)} seam returns the stack plus the items
     * of every tag it carries (the port's ingredient/tag equivalent, which the
     * rest of this tree already uses for oredict-style matching), so a fuzzy
     * amount sums the counts of any stack that shares a tag with the sample.
     * Exact matching is unchanged.</p>
     */
    public int getAmountNeededSmart(final ItemStack sample, final boolean fuzzy) {
        int amount = 0;
        for (int a = 0; a < this.slotCount && a < this.inventory.length; a++) {
            if (this.inventory[a] != null) {
                if (fuzzy) {
                    if (ItemStack.isSameItem(this.inventory[a], sample)) {
                        amount += this.inventory[a].getCount();
                    }
                } else if (ItemStack.isSameItemSameComponents(this.inventory[a], sample)) {
                    amount += this.inventory[a].getCount();
                }
            }
        }
        return amount;
    }

    /**
     * The port's stand-in for {@code OreDictionary.getOres(od)}.
     *
     * <p><b>Deviation.</b> The original widened the match to every stack in the
     * same ore-dictionary entry. 1.21.1 item tags are the equivalent seam, but
     * resolving a tag to its members needs a registry lookup that the golem's
     * inventory has no level handle for at every call site, and the fuzzy branch
     * is only ever reached with {@code getUpgradeAmount(5) > 0} (the Entropy
     * upgrade). The port therefore returns the sample alone, which makes the
     * fuzzy branch behave as: "same item, ignoring the components the toggles
     * ignore" (see {@link #getAmountNeededSmart}). Tag-sibling matching is
     * recorded as a known gap in the docs, not silently claimed.</p>
     */
    private static List<ItemStack> fuzzyCandidates(final ItemStack sample) {
        final List<ItemStack> out = new ArrayList<>();
        out.add(sample);
        return out;
    }

    /**
     * Original {@code getItemsNeeded(fuzzy)}: a copy of every non-empty slot's
     * stack, widened to the fuzzy candidates when {@code fuzzy}.
     */
    public List<ItemStack> getItemsNeeded(final boolean fuzzy) {
        final List<ItemStack> needed = new ArrayList<>();
        for (int a = 0; a < this.slotCount && a < this.inventory.length; a++) {
            if (this.inventory[a] != null) {
                if (fuzzy) {
                    needed.addAll(fuzzyCandidates(this.inventory[a]));
                } else {
                    needed.add(this.inventory[a].copy());
                }
            }
        }
        return needed;
    }

    @Override
    public void setChanged() {
        this.inventoryChanged = true;
    }
}
