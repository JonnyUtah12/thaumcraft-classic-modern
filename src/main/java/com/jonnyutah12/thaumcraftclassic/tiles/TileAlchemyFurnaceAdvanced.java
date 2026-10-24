package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTags;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604cBlockEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Advanced alchemical furnace (content-mapping.csv
 * {@code thaumcraft:tilealchemyfurnaceadvanced}, hosted on blockAlchemyFurnace
 * variant 0; its nozzle is variant 1).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced}.
 * Same burn-and-distil loop as the basic alchemical furnace, plus: a 4-slot
 * buffer that accepts items thrown at it or piped in from the sides, an
 * internal aspect store fed by the nozzles' suction network, and a
 * {@code getBellows} speed term identical to the basic furnace.</p>
 */
public class TileAlchemyFurnaceAdvanced extends TileOwned implements WorldlyContainer, IEssentiaTransport {

    public static final int SLOTS = 4;

    public final ItemStack[] contents = new ItemStack[SLOTS];
    public int furnaceBurnTime = 0;
    public int currentItemBurnTime = 0;
    public int furnaceCookTime = 0;
    public int smeltTime = 200;
    public boolean speedBoost = false;
    public AspectList aspects = new AspectList();
    public int vis = 0;
    public int maxVis = 100;

    int bellows = -1;
    int count = 0;

    public TileAlchemyFurnaceAdvanced(final BlockPos pos, final BlockState state) {
        super(M604cBlockEntities.TILE_ALCHEMY_FURNACE_ADVANCED.get(), pos, state);
    }

    public void serverTick() {
        final boolean wasBurning = furnaceBurnTime > 0;
        boolean flag1 = false;
        count++;
        if (furnaceBurnTime > 0) {
            furnaceBurnTime--;
        }

        if (bellows < 0) {
            getBellows();
        }

        if (furnaceBurnTime == 0 && canSmelt()) {
            currentItemBurnTime = furnaceBurnTime = fuelBurnTime(contents[0]);
            if (furnaceBurnTime > 0) {
                flag1 = true;
                speedBoost = false;
                if (!contents[0].isEmpty()) {
                    if (isTcCoalBlock(contents[0])) {
                        speedBoost = true;
                    }
                    contents[0].shrink(1);
                    if (contents[0].isEmpty()) {
                        contents[0] = ItemStack.EMPTY;
                    }
                }
            }
        }

        if (isBurning() && canSmelt()) {
            furnaceCookTime++;
            if (furnaceCookTime >= smeltTime) {
                furnaceCookTime = 0;
                smeltItem();
                flag1 = true;
            }
        } else {
            furnaceCookTime = 0;
        }

        if (wasBurning != (furnaceBurnTime > 0)) {
            flag1 = true;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        if (flag1) {
            setChanged();
        }
    }

    public void getBellows() {
        bellows = TileBellows.getBellows(level, worldPosition, Direction.values());
    }

    public boolean isBurning() {
        return furnaceBurnTime > 0;
    }

    /** Original {@code canSmelt}: slot 1..3 must hold an aspect-carrying item. */
    private boolean canSmelt() {
        ItemStack input = ItemStack.EMPTY;
        for (int i = 1; i < SLOTS; i++) {
            if (!contents[i].isEmpty()) {
                input = contents[i];
                break;
            }
        }
        if (input.isEmpty()) {
            return false;
        }
        final AspectList al = EssentiaTags.getObjectTags(input);
        if (al == null || al.size() == 0) {
            return false;
        }
        final int vs = al.visSize();
        if (vs > maxVis - vis) {
            return false;
        }
        smeltTime = (int) (vs * 10 * (1.0F - 0.125F * Math.max(0, bellows)));
        return true;
    }

    public void smeltItem() {
        if (!canSmelt()) {
            return;
        }
        for (int i = 1; i < SLOTS; i++) {
            if (!contents[i].isEmpty()) {
                final AspectList al = EssentiaTags.getObjectTags(contents[i]);
                aspects.add(al);
                vis = aspects.visSize();
                contents[i].shrink(1);
                if (contents[i].isEmpty()) {
                    contents[i] = ItemStack.EMPTY;
                }
                break;
            }
        }
        setChanged();
    }

    /** Original {@code onItemDropped}: thrown items enter the buffer. */
    public void onItemDropped(final ItemStack stack) {
        for (int i = 1; i < SLOTS; i++) {
            if (contents[i].isEmpty()) {
                contents[i] = stack.copy();
                stack.setCount(0);
                setChanged();
                return;
            }
            if (ItemStack.isSameItemSameComponents(contents[i], stack)
                    && contents[i].getCount() < contents[i].getMaxStackSize()) {
                contents[i].grow(1);
                stack.shrink(1);
                setChanged();
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
    }

    public int getVis() {
        return vis;
    }

    public AspectList getAspects() {
        return aspects;
    }

    public boolean takeFromContainer(final Aspect aspect, final int amount) {
        if (aspects.getAmount(aspect) >= amount) {
            aspects.reduce(aspect, amount);
            vis = aspects.visSize();
            setChanged();
            return true;
        }
        return false;
    }

    // --- WorldlyContainer ------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return contents[slot];
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        contents[slot] = stack;
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(final Direction side) {
        return new int[]{0, 1, 2, 3};
    }

    @Override
    public boolean canPlaceItemThroughFace(final int index, final ItemStack stack, final Direction dir) {
        return index != 0;
    }

    @Override
    public boolean canTakeItemThroughFace(final int index, final ItemStack stack, final Direction dir) {
        return false;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return slot == 0 ? fuelBurnTime(stack) > 0 : true;
    }

    // --- IEssentiaTransport (the nozzles' view of the aspect store) -----------

    @Override
    public boolean isConnectable(final Direction face) {
        return true;
    }

    @Override
    public boolean canInputFrom(final Direction face) {
        return false;
    }

    @Override
    public boolean canOutputTo(final Direction face) {
        return true;
    }

    @Override
    public void setSuction(final Aspect aspect, final int amount) {
        // original: no-op
    }

    @Override
    public Aspect getSuctionType(final Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        return 0;
    }

    @Override
    public int takeEssentia(final Aspect aspect, final int amount, final Direction face) {
        return takeFromContainer(aspect, amount) ? amount : 0;
    }

    @Override
    public int addEssentia(final Aspect aspect, final int amount, final Direction face) {
        return 0;
    }

    @Override
    public Aspect getEssentiaType(final Direction face) {
        return null;
    }

    @Override
    public int getEssentiaAmount(final Direction face) {
        return vis;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    // --- persistence (original NBT keys) ---------------------------------------

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putShort("BurnTime", (short) furnaceBurnTime);
        tag.putShort("CookTime", (short) furnaceCookTime);
        tag.putBoolean("speedBoost", speedBoost);

        final ListTag items = new ListTag();
        for (int i = 0; i < contents.length; i++) {
            if (!contents[i].isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                contents[i].save(entry);
                items.add(entry);
            }
        }
        tag.put("Items", items);
        aspects.writeToNBT(tag);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        furnaceBurnTime = tag.getShort("BurnTime");
        furnaceCookTime = tag.getShort("CookTime");
        speedBoost = tag.getBoolean("speedBoost");
        for (int i = 0; i < contents.length; i++) {
            contents[i] = ItemStack.EMPTY;
        }
        final ListTag items = tag.getList("Items", 10);
        for (int i = 0; i < items.size(); i++) {
            final CompoundTag entry = items.getCompound(i);
            final int slot = entry.getByte("Slot") & 0xFF;
            if (slot < contents.length) {
                contents[slot] = ItemStack.parseOptional(level.registryAccess(), entry);
            }
        }
        aspects = new AspectList();
        aspects.readFromNBT(tag);
        vis = aspects.visSize();
        currentItemBurnTime = fuelBurnTime(contents[0]);
    }

    private static int fuelBurnTime(final ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
    }

    private static boolean isTcCoalBlock(final ItemStack stack) {
        final net.minecraft.resources.ResourceLocation id =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("thaumcraft") && id.getPath().equals("itemresource")
                && stack.getOrDefault(
                        com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents.ITEM_META.get(),
                        0) == 0;
    }
}
