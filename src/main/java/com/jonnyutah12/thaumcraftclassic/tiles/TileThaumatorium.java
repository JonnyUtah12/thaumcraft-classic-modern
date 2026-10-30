package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.menus.ThaumatoriumMenu;
import com.jonnyutah12.thaumcraftclassic.recipes.CrucibleRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCrucible;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogThaumatorium;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.HolderLookup;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Thaumatorium base tile (content-mapping.csv {@code thaumcraft:tilethaumatorium},
 * original {@code thaumcraft.common.tiles.TileThaumatorium}, hosted by
 * {@code blockmetaldevice} metadata 10).
 *
 * <p><b>Scope of this tile (TC-M7-04-m2c, gui_thaumatorium).</b> It is the
 * window's data half: the single catalyst slot (the original's
 * {@code inputStack}, ISidedInventory slot 0, stack limit 64, all sides in/out),
 * the essentia buffer the GUI's bars display ({@code public AspectList
 * essentia}), and the craft <i>queue</i> the GUI's output box toggles —
 * the original's parallel lists {@code recipeHash}/{@code recipeEssentia}/
 * {@code recipePlayer}, keyed here by the recipe's data-file id (the 1.7.10
 * int {@code hash} was an object identity with no 1.21.1 analogue; the tile is
 * new to the port, so no legacy save carries the int form). The queue add is
 * the original's container click verbatim: append the recipe's aspect copy,
 * the clicking player's name, and reset {@code currentCraft}.</p>
 *
 * <p><b>What this tile deliberately does not run (BD-19).</b> The original's
 * server tick loop — heat, tube suction, the queued-craft walk, the output
 * ejection, the IAspectContainer/IEssentiaTransport wire-up — belongs to the
 * TileThaumatorium machine row of {@code docs/content-mapping.csv}, which this
 * card does not flip. The tile therefore has no ticker, and
 * {@code getUpgrades()} (the original's Mnemonic Matrix scan,
 * {@code TileThaumatorium.java:258-289}) runs its block-shape walk against
 * variant 12 and can never count: {@code TileBrainbox} is not ported, so no
 * brainbox tile exists to pass the original's {@code instanceof} test.
 * {@code maxRecipes} is the original's base value 1 until that card lands.</p>
 *
 * <p>Persistence is the original's key set: {@code facing}, {@code maxrec},
 * the essentia list under {@code Aspects}, {@code "Items"} slot 0 for the
 * catalyst and {@code OutputPlayer} for the queue's player names. The queue
 * list itself is stored under the original's {@code recipes} key as id strings
 * (the original's int-array form, decoded via a hash lookup, is documented in
 * BD-19).</p>
 */
public class TileThaumatorium extends BlockEntity
        implements WorldlyContainer, MenuProvider,
        com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.DeviceAnalogTile,
        com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.DeviceNeighborTile {

    /** The original's single slot count ({@code getSizeInventory} = 1). */
    public static final int SLOTS = 1;
    /** The original's stack limit ({@code getInventoryStackLimit} = 64). */
    public static final int STACK_LIMIT = 64;
    /** Menu tile slot of the catalyst. */
    public static final int SLOT_INPUT = 0;

    /** The original's {@code ItemStack inputStack} — here the one-slot container. */
    private final ItemStack[] stacks = new ItemStack[SLOTS];

    /** The original's {@code public AspectList essentia} — the GUI bar source. */
    private final AspectList essentia = new AspectList();

    /**
     * One queued craft: the recipe id (the original's {@code recipeHash} entry),
     * the copied aspect cost ({@code recipeEssentia}) and the queuer
     * ({@code recipePlayer}).
     */
    public record QueuedCraft(ResourceLocation recipeId, AspectList aspects, String player) {
    }

    private final List<QueuedCraft> queue = new ArrayList<>();

    /** The original's {@code maxRecipes} base value; matrices add 2 each. */
    private int maxRecipes = 1;

    /** The original's {@code currentCraft} index into the queue. */
    private int currentCraft = -1;

    /** The original's {@code facing}; consumed by the machine card's transport. */
    private Direction facing = Direction.UP;

    public TileThaumatorium(final BlockPos pos, final BlockState state) {
        super(CatalogThaumatorium.TILE_THAUMATORIUM.get(), pos, state);
        for (int i = 0; i < this.stacks.length; i++) {
            this.stacks[i] = ItemStack.EMPTY;
        }
    }

    // ------------------------------------------------------------ queue API

    /** A live view of the queue, in queue order (the original's list order). */
    public List<QueuedCraft> queue() {
        return this.queue;
    }

    /** The original's {@code recipeHash.contains(hash)}. */
    public boolean isQueued(final ResourceLocation recipeId) {
        for (final QueuedCraft queued : this.queue) {
            if (queued.recipeId().equals(recipeId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The original's {@code ContainerThaumatorium.handleWindowClick} body
     * (ContainerThaumatorium.java:72-99), verbatim in structure: a queued
     * recipe's entry is removed (all three parallel lists, and
     * {@code currentCraft = -1}); a new one appends the recipe's
     * {@code aspects.copy()} and the clicking player's name. The original
     * server side has <b>no</b> {@code maxRecipes} cap — the 1.7.10 client
     * gated the hitbox, and that gate lives in the screen. The
     * {@code markDirty} is the original's {@code func_70296_d()}.
     */
    public boolean toggleQueued(final ResourceLocation recipeId, final CrucibleRecipe recipe,
                                final Player player) {
        for (int a = 0; a < this.queue.size(); a++) {
            if (this.queue.get(a).recipeId().equals(recipeId)) {
                this.queue.remove(a);
                this.currentCraft = -1;
                this.setChanged();
                return true;
            }
        }
        this.queue.add(new QueuedCraft(recipeId, recipe.aspects().copy(),
                player.getGameProfile().getName()));
        this.setChanged();
        return true;
    }

    /** The original's {@code maxRecipes} (matrix-boosted once brainboxes land). */
    public int maxRecipes() {
        return this.maxRecipes;
    }

    /** The original's {@code currentCraft}; the machine card advances it. */
    public int currentCraft() {
        return this.currentCraft;
    }

    /** The essentia buffer the GUI's bars read; the machine card fills it. */
    public AspectList essentia() {
        return this.essentia;
    }

    /**
     * The original's {@code getUpgrades()} ({@code TileThaumatorium.java:258}):
     * walk y+0..1 over every side except DOWN and facing, count variant-12
     * {@code blockmetaldevice} neighbours whose {@code TileBrainbox} faces the
     * thaumatorium, +2 each, and trim the queue tail when the cap shrinks.
     * The brainbox clause can never fire — {@code TileBrainbox} is not ported
     * (content-mapping row for {@code thaumcraft:tilebrainbox}), no tile exists
     * at variant 12 to pass it — so the scan is kept shape-for-shape and stays
     * at the base 1. The original runs it from {@code BlockMetalDevice
     * .neighborChanged} (BlockMetalDevice.java:455/466); the port's seam is
     * {@link com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.DeviceNeighborTile}.
     */
    public void getUpgrades() {
        int mr = 1;
        if (this.level == null) {
            return;
        }
        final net.minecraft.world.level.block.Block metalDevice =
                BuiltInRegistries.BLOCK.get(ResourceLocation
                        .fromNamespaceAndPath(com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID,
                                "blockmetaldevice"));
        for (int yy = 0; yy <= 1; yy++) {
            for (final Direction dir : Direction.values()) {
                if (dir == Direction.DOWN || dir == this.facing) {
                    continue;
                }
                final BlockPos at = this.worldPosition.relative(dir).above(yy);
                final BlockState state = this.level.getBlockState(at);
                if (state.getBlock() == metalDevice
                        && state.getBlock() instanceof com.jonnyutah12.thaumcraftclassic.blocks.VariantDevice device
                        && device.variant(state) == 12) {
                    final BlockEntity be = this.level.getBlockEntity(at);
                    // The original's TileBrainbox-facing test; the brainbox
                    // tile is not ported, so this branch is inert by design.
                    if (be instanceof TileBrainboxFacing brainbox && brainbox.facing() == dir.getOpposite()) {
                        mr += 2;
                    }
                }
            }
        }
        if (mr != this.maxRecipes) {
            this.maxRecipes = mr;
            while (this.queue.size() > this.maxRecipes) {
                this.queue.remove(this.queue.size() - 1);
            }
            this.setChanged();
        }
    }

    /**
     * The seam the (unported) Mnemonic Matrix tile will implement when
     * {@code TileBrainbox} lands: a brainbox facing the thaumatorium (the
     * original's {@code ((TileBrainbox)te).facing == dir.getOpposite()}).
     */
    public interface TileBrainboxFacing {
        Direction facing();
    }

    @Override
    public void deviceNeighborChanged() {
        getUpgrades();
    }

    /**
     * The original's {@code BlockMetalDevice.func_149736_g} comparator branch
     * for a Thaumatorium: {@code Container.getRedstoneSignalValue(inventory)} —
     * vanilla's fill gradient, 1..15 by how full the catalyst slot is
     * ({@code 1.21.1's getRedstoneSignalFromContainer} is that same rule).
     */
    @Override
    public int getComparatorSignal() {
        return net.minecraft.world.inventory.AbstractContainerMenu
                .getRedstoneSignalFromContainer(this);
    }

    // -------------------------------------------------- WorldlyContainer

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot >= 0 && slot < this.stacks.length ? this.stacks[slot] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        final ItemStack stack = getItem(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack taken;
        if (stack.getCount() <= count) {
            taken = stack.copy();
            setItem(slot, ItemStack.EMPTY);
        } else {
            taken = stack.split(count);
            setItem(slot, stack);
        }
        this.setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        final ItemStack stack = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack stack : this.stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.stacks.length; i++) {
            this.stacks[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (slot >= 0 && slot < this.stacks.length) {
            this.stacks[slot] = stack;
            // The original clamps to the 64 stack limit on set
            // (TileThaumatorium.java:498-501).
            if (stack.getCount() > STACK_LIMIT) {
                stack.setCount(STACK_LIMIT);
            }
        }
        this.setChanged();
        // The original nudged its open container to recompute the recipe list
        // (eventHandler.setContainer); 1.21.1's menu recomputes every
        // broadcastChanges tick, so the list refreshes on the open menu itself.
    }

    /** The original's {@code isItemValidForSlot} — always true. */
    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return true;
    }

    @Override
    public int[] getSlotsForFace(final Direction side) {
        return new int[]{SLOT_INPUT};
    }

    /** The original's {@code canInsertItem} — always true. */
    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack,
                                           @Nullable final Direction side) {
        return true;
    }

    /** The original's {@code canExtractItem} — always true. */
    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack,
                                          final Direction side) {
        return true;
    }

    // ----------------------------------------------------- MenuProvider

    /**
     * The window title. The original's {@code getInventoryName} returned
     * {@code "container.alchemyfurnace"} — a 1.7.10 copy-paste that made the
     * Thaumatorium's title read "Alchemy Furnace". The port titles the window
     * for its own gui row instead; BD-19 records the string difference.
     */
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.thaumcraft.gui_thaumatorium");
    }

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory inventory,
                                            final Player player) {
        return new ThaumatoriumMenu(id, inventory, this);
    }

    /**
     * The original's {@code canInteractWith} (TileThaumatorium.java:515): the
     * tile must still be in world at its position, and the player within
     * 64 squared distance of its centre — vanilla's chest rule verbatim.
     */
    @Override
    public boolean stillValid(final Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    // ------------------------------------------------------- persistence

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("facing", (byte) this.facing.get3DDataValue());
        tag.putByte("maxrec", (byte) this.maxRecipes);
        this.essentia.writeToNBT(tag);
        final ListTag items = new ListTag();
        for (int i = 0; i < this.stacks.length; i++) {
            if (!this.stacks[i].isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", this.stacks[i].save(registries));
                items.add(entry);
            }
        }
        // Original writeNBT key for the catalyst.
        tag.put("Items", items);
        // Original "recipes" key; the port stores recipe ids (BD-19).
        final ListTag recipes = new ListTag();
        for (final QueuedCraft queued : this.queue) {
            recipes.add(StringTag.valueOf(queued.recipeId().toString()));
        }
        tag.put("recipes", recipes);
        // Original "OutputPlayer" key, index-aligned with the queue.
        final ListTag players = new ListTag();
        for (final QueuedCraft queued : this.queue) {
            players.add(StringTag.valueOf(queued.player()));
        }
        tag.put("OutputPlayer", players);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.facing = Direction.from3DDataValue(Math.max(0, Math.min(5, tag.getByte("facing"))));
        this.maxRecipes = tag.getByte("maxrec");
        this.essentia.readFromNBT(tag);
        for (int i = 0; i < this.stacks.length; i++) {
            this.stacks[i] = ItemStack.EMPTY;
        }
        for (final Tag entry : tag.getList("Items", Tag.TAG_COMPOUND)) {
            final CompoundTag item = (CompoundTag) entry;
            final int slot = item.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.stacks.length && item.contains("Item")) {
                this.stacks[slot] = ItemStack.parseOptional(registries, item.getCompound("Item"));
            }
        }
        // The original re-resolved int hashes through the recipe registry and
        // dropped unknowns; the port resolves ids and drops unknowns — the same
        // "drop what is not loaded today" rule.
        this.queue.clear();
        final ListTag recipes = tag.getList("recipes", Tag.TAG_STRING);
        final ListTag players = tag.getList("OutputPlayer", Tag.TAG_STRING);
        for (int a = 0; a < recipes.size(); a++) {
            final ResourceLocation id = ResourceLocation.tryParse(recipes.getString(a));
            if (id == null) {
                continue;
            }
            final CrucibleRecipe recipe = this.level == null
                    ? null : findCrucibleRecipe(this.level, id);
            if (recipe == null) {
                continue;
            }
            final String player = a < players.size() ? players.getString(a) : "";
            this.queue.add(new QueuedCraft(id, recipe.aspects().copy(), player));
        }
    }

    /** The live crucible recipe registered under {@code id} in the level's live
     * {@link net.minecraft.world.item.crafting.RecipeManager}, or null when the
     * data file is not loaded (conditions gated, datapack removed) — the
     * original's {@code getCrucibleRecipeFromHash} returning null.
     */
    @Nullable
    public static CrucibleRecipe findCrucibleRecipe(
            final net.minecraft.world.level.Level level, final ResourceLocation id) {
        for (final var holder : level.getRecipeManager()
                .getAllRecipesFor(CatalogCrucible.CRUCIBLE_TYPE.get())) {
            if (holder.id().equals(id)) {
                return holder.value();
            }
        }
        return null;
    }
}
