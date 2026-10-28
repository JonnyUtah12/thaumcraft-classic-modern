package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectCombinations;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.nodes.INode;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import com.jonnyutah12.thaumcraftclassic.wands.IVisContainerItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Wand recharge pedestal (content-mapping.csv {@code thaumcraft:tilewandpedestal},
 * hosted on blockStoneDevice variant 9 — TC-M6-04c's block).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileWandPedestal}. Holds a wand
 * (or vis amulet) and transfers 1 centi-vis per 5 ticks from the nearest vis
 * node within range 10 (never below vis 1, except iron-cap/wood-rod wands
 * which may drain to 0); a compound recharge focus (stoneDevice variant 8)
 * placed on top converts complex node aspects to primals on transfer.</p>
 *
 * <p>Cross-card hooks: nodes are TC-M5-02 (reached through the shared
 * {@link INode} contract) and wands/amulets are TC-M6-01/TC-M8-02 (reached
 * through {@link IVisContainerItem}). Both are not started, so the charging
 * loop is structurally complete but inert until they land.</p>
 */
public class TileWandPedestal extends BlockEntity implements WorldlyContainer, com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.TickingDeviceTile {

    private final ItemStack[] inventory = new ItemStack[1];
    private int counter = 0;
    private boolean somethingChanged = false;
    private boolean draining = false;
    @Nullable
    private List<BlockPos> nodes = null;
    private int nodeScan = 0;

    public TileWandPedestal(final BlockPos pos, final BlockState state) {
        super(CatalogFurniture.TILE_WAND_PEDESTAL.get(), pos, state);
        // A fresh pedestal holds no item: the original's slot was an empty
        // stack, so every read path can rely on a real (possibly empty) item.
        this.inventory[0] = ItemStack.EMPTY;
    }

    public void serverTick() {
        this.counter++;
        if (this.nodes == null || this.nodeScan++ % 200 == 0) {
            this.nodes = findNodes();
        }
        if (this.counter % 5 == 0 && this.nodes != null && !this.nodes.isEmpty() && !this.inventory[0].isEmpty()) {
            boolean hasThingy = false;
            final BlockState above = this.level.getBlockState(this.worldPosition.above());
            // blockStoneDevice is TC-M6-04c's shared device block; reached by
            // id so this card does not depend on the sibling's holder class.
            if (above.getBlock() == net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "blockstonedevice"))
                    && above.getBlock() instanceof com.jonnyutah12.thaumcraftclassic.blocks.VariantDevice device
                    && device.variant(above) == 8) {
                hasThingy = true;
            }

            final IVisContainerItem visItem = this.inventory[0].getItem() instanceof IVisContainerItem item ? item : null;
            if (visItem != null) {
                int min = 1;
                if ("iron".equals(visItem.getCapTag(this.inventory[0]))
                        || "wood".equals(visItem.getRodTag(this.inventory[0]))) {
                    min = 0;
                }
                final AspectList as = visItem.getAspectsWithRoom(this.inventory[0]);
                this.draining = false;
                if (as != null && as.size() > 0) {
                    outer:
                    for (final BlockPos co : this.nodes) {
                        final BlockEntity te = this.level.getBlockEntity(co);
                        if (te instanceof INode node) {
                            for (final Aspect aspect : as.getAspects()) {
                                if (node.getAspects().getAmount(aspect) > min) {
                                    visItem.addVis(this.inventory[0], aspect, 1, true);
                                    node.takeFromContainer(aspect, 1);
                                    this.somethingChanged = true;
                                    this.draining = true;
                                    break outer;
                                }
                            }
                            if (hasThingy) {
                                for (final Aspect aspectx : node.getAspects().getAspects()) {
                                    if (aspectx != null && !aspectx.isPrimal()) {
                                        final AspectList primals = AspectCombinations.reduceToPrimals(new AspectList().add(aspectx, 1));
                                        for (final Aspect aspect2 : as.getAspects()) {
                                            if (primals.getAmount(aspect2) > 0 && node.getAspects().getAmount(aspectx) > min) {
                                                visItem.addVis(this.inventory[0], aspect2, 1, true);
                                                node.takeFromContainer(aspectx, 1);
                                                this.somethingChanged = true;
                                                this.draining = true;
                                                break outer;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Original findNodes(): vis nodes (INode block entities) within range 10. */
    @Nullable
    private List<BlockPos> findNodes() {
        final List<BlockPos> found = new ArrayList<>();
        for (int x = -10; x <= 10; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -10; z <= 10; z++) {
                    final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                            this.worldPosition.getX() + x, this.worldPosition.getY() + y, this.worldPosition.getZ() + z);
                    if (this.level.isLoaded(cursor) && this.level.getBlockEntity(cursor) instanceof INode) {
                        found.add(cursor.immutable());
                    }
                }
            }
        }
        return found;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot == 0 && this.inventory[0] != null ? this.inventory[0] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        // 1.21.1 has no inherited container to delegate to; mirror the vanilla
        // remove-item result for this single slot.
        if (slot != 0 || this.inventory[0].isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack current = this.inventory[0];
        final ItemStack removed = current.copy();
        if (current.getCount() <= count) {
            this.inventory[0] = ItemStack.EMPTY;
        } else {
            current.setCount(current.getCount() - count);
            removed.setCount(count);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (slot == 0 && !this.inventory[0].isEmpty()) {
            final ItemStack removed = this.inventory[0];
            this.inventory[0] = ItemStack.EMPTY;
            setChanged();
            return removed;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isEmpty() {
        return this.inventory[0].isEmpty();
    }

    @Override
    public void clearContent() {
        this.inventory[0] = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (slot == 0) {
            this.inventory[0] = stack == null ? ItemStack.EMPTY : stack;
            setChanged();
        }
    }

    /** Original isItemValidForSlot: only vis-carrying items (wand/amulet). */
    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return stack.getItem() instanceof IVisContainerItem;
    }

    @Override
    public int[] getSlotsForFace(final net.minecraft.core.Direction side) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, @Nullable final net.minecraft.core.Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final net.minecraft.core.Direction side) {
        return false;
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getCenter()) <= 64.0;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final ListTag items = new ListTag();
        for (int i = 0; i < this.inventory.length; i++) {
            if (!this.inventory[i].isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", this.inventory[i].save(registries));
                items.add(entry);
            }
        }
        tag.put("Items", items);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory[0] = ItemStack.EMPTY;
        for (final Tag entry : tag.getList("Items", Tag.TAG_COMPOUND)) {
            final CompoundTag item = (CompoundTag) entry;
            final int slot = item.getByte("Slot") & 255;
            if (slot == 0 && item.contains("Item")) {
                this.inventory[0] = ItemStack.parseOptional(registries, item.getCompound("Item"));
            }
        }
    }
}
