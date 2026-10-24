package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTags;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604cBlockEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Basic alchemical furnace (content-mapping.csv {@code thaumcraft:tilealchemyfurnace},
 * hosted on blockStoneDevice variant 0).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileAlchemyFurnace}. Burns
 * furnace fuel and "smelts" a single input item into its essentia aspects,
 * stored internally up to {@code maxVis}; the cook time is the input's vis
 * size × 10 ticks, shortened 12.5% per bellows facing the furnace (the
 * original's {@code 1.0F - 0.125F * bellows}). The stored aspects feed the
 * alembic stack above it — TileAlembic is TC-M4-05's, so the feed loop is
 * wired through {@link #feedAlembics()} and activates when that tile lands
 * (noted in the handoff).</p>
 */
public class TileAlchemyFurnace extends BlockEntity implements WorldlyContainer {

    public static final int SLOTS = 2;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int MAX_VIS = 100;

    public final ItemStack[] furnaceItemStacks = new ItemStack[SLOTS];
    public int furnaceBurnTime = 0;
    public int currentItemBurnTime = 0;
    public int furnaceCookTime = 0;
    public int smeltTime = 200;
    public boolean speedBoost = false;
    public AspectList aspects = new AspectList();
    public int vis = 0;
    public int maxVis = MAX_VIS;

    int bellows = -1;
    int count = 0;

    public TileAlchemyFurnace(final BlockPos pos, final BlockState state) {
        super(M604cBlockEntities.TILE_ALCHEMY_FURNACE.get(), pos, state);
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

        if (count % (speedBoost ? 20 : 40) == 0 && aspects.size() > 0) {
            feedAlembics();
        }

        if (furnaceBurnTime == 0 && canSmelt()) {
            currentItemBurnTime = furnaceBurnTime = fuelBurnTime(furnaceItemStacks[SLOT_FUEL]);
            if (furnaceBurnTime > 0) {
                flag1 = true;
                speedBoost = false;
                if (!furnaceItemStacks[SLOT_FUEL].isEmpty()) {
                    // Original: burning the TC coal-block item (itemResource
                    // meta 0) halves the alembic feed interval. itemResource
                    // is TC-M6-04a's; matched lazily by contract id + meta.
                    if (isTcCoalBlock(furnaceItemStacks[SLOT_FUEL])) {
                        speedBoost = true;
                    }
                    furnaceItemStacks[SLOT_FUEL].shrink(1);
                    if (furnaceItemStacks[SLOT_FUEL].isEmpty()) {
                        furnaceItemStacks[SLOT_FUEL]
                                .restoreContainerItem();
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

    /**
     * Original: push aspects up into the {@code TileAlembic} stack above the
     * furnace (fill matching filters first, then random aspects). TileAlembic
     * is TC-M4-05's tile; this loop no-ops until it registers, preserving the
     * original's structure and tick cadence.
     */
    private void feedAlembics() {
        // Wired when TileAlembic lands (TC-M4-05). See class javadoc.
    }

    public void getBellows() {
        bellows = TileBellows.getBellows(level, worldPosition, Direction.values());
    }

    public boolean isBurning() {
        return furnaceBurnTime > 0;
    }

    /** Original {@code canSmelt}: input must carry aspects and fit in the vis buffer. */
    private boolean canSmelt() {
        if (furnaceItemStacks[SLOT_INPUT].isEmpty()) {
            return false;
        }
        final AspectList al = objectTags(furnaceItemStacks[SLOT_INPUT]);
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
        if (canSmelt()) {
            final AspectList al = objectTags(furnaceItemStacks[SLOT_INPUT]);
            aspects.add(al);
            vis = aspects.visSize();
            furnaceItemStacks[SLOT_INPUT].shrink(1);
            if (furnaceItemStacks[SLOT_INPUT].isEmpty()) {
                furnaceItemStacks[SLOT_INPUT] = ItemStack.EMPTY;
            }
            setChanged();
        }
    }

    /**
     * Original {@code ThaumcraftCraftingManager.getObjectTags + getBonusTags}.
     * The crafting-derived bonus tags ride with TC-M4-03; the object-tag half
     * is live through {@link EssentiaTags}.
     */
    @Nullable
    private static AspectList objectTags(final ItemStack stack) {
        return EssentiaTags.getObjectTags(stack);
    }

    /** Original {@code TileEntityFurnace.getItemBurnTime} — vanilla fuel values. */
    private static int fuelBurnTime(final ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
    }

    /** The original's {@code new ItemStack(ConfigItems.itemResource, 1, 0)} fuel. */
    private static boolean isTcCoalBlock(final ItemStack stack) {
        final net.minecraft.resources.ResourceLocation id =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("thaumcraft") && id.getPath().equals("itemresource")
                && stack.getOrDefault(
                        com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents.ITEM_META.get(),
                        0) == 0;
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

    @Nullable
    public Aspect takeRandomAspect() {
        final List<Aspect> list = aspects.getAspects();
        if (list.isEmpty()) {
            return null;
        }
        final Aspect a = list.get(level.random.nextInt(list.size()));
        takeFromContainer(a, 1);
        return a;
    }

    // --- WorldlyContainer (menu + hopper surface) -----------------------------

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
        return furnaceItemStacks[slot];
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        furnaceItemStacks[slot] = stack;
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(final Direction side) {
        return new int[]{SLOT_INPUT, SLOT_FUEL};
    }

    @Override
    public boolean canPlaceItemThroughFace(final int index, final ItemStack stack, final Direction dir) {
        return index == SLOT_INPUT;
    }

    @Override
    public boolean canTakeItemThroughFace(final int index, final ItemStack stack, final Direction dir) {
        return false;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return slot == SLOT_FUEL ? fuelBurnTime(stack) > 0 : true;
    }

    // --- persistence (original NBT keys) --------------------------------------

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putShort("BurnTime", (short) furnaceBurnTime);
        tag.putShort("CookTime", (short) furnaceCookTime);
        tag.putBoolean("speedBoost", speedBoost);

        final ListTag items = new ListTag();
        for (int i = 0; i < furnaceItemStacks.length; i++) {
            if (!furnaceItemStacks[i].isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                furnaceItemStacks[i].save(entry);
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
        for (int i = 0; i < furnaceItemStacks.length; i++) {
            furnaceItemStacks[i] = ItemStack.EMPTY;
        }
        final ListTag items = tag.getList("Items", 10);
        for (int i = 0; i < items.size(); i++) {
            final CompoundTag entry = items.getCompound(i);
            final int slot = entry.getByte("Slot") & 0xFF;
            if (slot < furnaceItemStacks.length) {
                furnaceItemStacks[slot] = ItemStack.parseOptional(level.registryAccess(), entry);
            }
        }
        aspects = new AspectList();
        aspects.readFromNBT(tag);
        vis = aspects.visSize();
        currentItemBurnTime = fuelBurnTime(furnaceItemStacks[SLOT_FUEL]);
    }
}
