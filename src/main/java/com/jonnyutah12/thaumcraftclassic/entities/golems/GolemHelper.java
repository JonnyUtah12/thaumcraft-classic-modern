package com.jonnyutah12.thaumcraftclassic.entities.golems;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillableVoid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The golem marker infrastructure — the original's
 * {@code thaumcraft.common.entities.golems.GolemHelper} (TC-M6-08-s-b). Every
 * task resolves its targets through here: a marker (position + face + colour)
 * on a block is what makes that block visible to a golem, so this class, not
 * the goal classes, is the functional core of the task system.
 *
 * <h2>Pinned from the source</h2>
 * <ul>
 *   <li>{@link #ADJACENT_RANGE} = {@code 4.0} ({@code GolemHelper.java:29}) —
 *       used as {@code golem.getDistanceSq(...) < 4.0} for the
 *       "adjacent to the golem" variants, i.e. a squared comparison against
 *       4.0, so the real radius is 2.0 blocks. Preserved exactly (the original
 *       never takes the square root).</li>
 *   <li>{@link #getMarkedContainers} — {@code GolemHelper.java:34-48}: every
 *       marker whose block entity is a container, in marker order, double
 *       chests added twice (once via the primary and once via
 *       {@code InventoryUtils.getDoubleChest}).</li>
 *   <li>{@link #getMarkedSides} / {@link #contained} — {@code GolemHelper.java:133-162}:
 *       the six faces of a marked block, each matched with
 *       {@link Marker#equalsFuzzy} against the golem's markers with the queried
 *       colour, so colour {@code -1} matches any colour.</li>
 *   <li>{@link #findJarWithRoom} — {@code GolemHelper.java:283-458}: the
 *       original's six-pass jar preference order (filtered jar with room →
 *       empty filtered jar → filtered void jar that is full → unfiltered jar
 *       with room → empty unfiltered jar → unfiltered void jar), nearest first,
 *       void jars pushed to the back with a {@code + dmod} distance penalty.
 *       Order and penalties preserved.</li>
 *   <li>{@link #getConnectedJars} — {@code GolemHelper.java:460-470}: the
 *       flood fill over the six neighbours that builds the candidate jar set
 *       from a marked jar. The static {@code jarlist} map is preserved (and
 *       {@link #clearJarListForTest()} added, because a static map would
 *       otherwise leak across game tests — see the note there).</li>
 *   <li>{@link #getItemsNeeded} — {@code GolemHelper.java:624-646}, whose
 *       switch is pinned by {@link GolemCoreTask.ItemsNeeded}.</li>
 * </ul>
 *
 * <h2>1.7.10 → 1.21.1 adaptations (all shape-only unless noted)</h2>
 * <ul>
 *   <li>{@code IInventory} → {@link Container}; {@code ISidedInventory}'s
 *       {@code getSlotsForFace(int)} → {@link WorldlyContainer#getSlotsForFace(Direction)}.
 *       Forge direction ordinals map 1:1 onto {@link Direction} ordinals
 *       (DOWN, UP, NORTH, SOUTH, WEST, EAST), which is what
 *       {@link #directionOf(int)} relies on.</li>
 *   <li>{@code ChunkCoordinates} → {@link BlockPos}; {@code Vec3} for a block
 *       target → {@link BlockPos} (the original only ever used a
 *       {@code ChunkCoordinates}'s integer fields as a block coordinate).</li>
 *   <li>{@code IFluidHandler} → NeoForge's fluid capability
 *       ({@link Capabilities.FluidHandler#BLOCK}); {@code FluidRegistry}
 *       integer fluid ids → {@link Fluid} registry entries, so
 *       {@link #getReggedLiquids()} returns {@code Fluid} values where the
 *       original returned {@code Integer} ids. <b>Deviation</b> (type only —
 *       the set is the same registry content).</li>
 *   <li>{@code InventoryUtils.placeItemStackIntoInventory / extractStack /
 *       attemptExtraction / areItemStacksEqual / inventoryContains} have no
 *       port yet, so the four private methods at the bottom of this class are
 *       the minimal seam they need, named after the originals.</li>
 *   <li>Essentia jars are <b>not</b> re-abstracted: {@link TileJarFillable}
 *       (shipped by TC-M4-06-p) is used directly, exactly as the card requires.</li>
 * </ul>
 */
public final class GolemHelper {

    /** Original {@code GolemHelper.java:29} — squared, per the original's own usage. */
    public static final double ADJACENT_RANGE = 4.0;

    /** Original static {@code jarlist}: the connected-jar candidates, keyed {@code "x:y:z"}. */
    private static final Map<String, TileJarFillable> JAR_LIST = new HashMap<>();

    /** Original static {@code reggedLiquids}, resolved once. */
    private static List<Fluid> reggedLiquids = null;

    /** Original static {@code itemTimeout}: {@code (golemId, stack) -> expiry millis}. */
    private static final List<SortingItemTimeout> ITEM_TIMEOUT = new ArrayList<>();

    private GolemHelper() {
    }

    /**
     * Clears the two static caches. The original had no such method — its
     * {@code jarlist} was cleared at the end of every {@code findJarWithRoom}
     * call and its {@code itemTimeout} list was per-JVM. Game tests still need a
     * deterministic reset (a leaked {@code itemTimeout} entry would make the
     * next probe read a timeout from a previous one), so this seam exists for
     * tests and for a world unload hook.
     */
    public static void clearJarListForTest() {
        JAR_LIST.clear();
    }

    /** Clears the item-timeout list ({@code Config.golemIgnoreDelay} bookkeeping). */
    public static void clearItemTimeoutsForTest() {
        ITEM_TIMEOUT.clear();
    }

    /** Original {@code ForgeDirection.getOrientation(i)} → the matching modern face. */
    public static Direction directionOf(final int ordinal) {
        return Direction.values()[Math.floorMod(ordinal, 6)];
    }

    /** The block a task resolves "the home container" to: {@code home - facing} ({@code GolemHelper.java:193-195}). */
    public static BlockPos homeContainerPos(final EntityGolemBase golem) {
        final Direction facing = directionOf(golem.getHomeFacing());
        return golem.getRestrictCenter().relative(facing.getOpposite());
    }

    // ------------------------------------------------------------- containers ---

    /** Original {@code getMarkedContainers}. */
    public static List<Container> getMarkedContainers(final Level world, final EntityGolemBase golem) {
        final List<Container> results = new ArrayList<>();
        for (final Marker marker : golem.getMarkers()) {
            if (marker.dim != dimensionOf(world)) {
                continue;
            }
            final BlockEntity tile = world.getBlockEntity(new BlockPos(marker.x, marker.y, marker.z));
            if (tile instanceof Container container) {
                results.add(container);
                final Container doubleChest = getDoubleChest(tile);
                if (doubleChest != null) {
                    results.add(doubleChest);
                }
            }
        }
        return results;
    }

    /** Original {@code getMarkedContainersAdjacentToGolem}: marked containers within the squared {@code 4.0}. */
    public static List<Container> getMarkedContainersAdjacentToGolem(final Level world, final EntityGolemBase golem) {
        final List<Container> results = new ArrayList<>();
        for (final Container container : getMarkedContainers(world, golem)) {
            final BlockEntity tile = (BlockEntity) container;
            if (golem.distanceToSqr(tile.getBlockPos().getX() + 0.5, tile.getBlockPos().getY() + 0.5,
                    tile.getBlockPos().getZ() + 0.5) < ADJACENT_RANGE) {
                results.add(container);
                final Container doubleChest = getDoubleChest(tile);
                if (doubleChest != null) {
                    results.add(doubleChest);
                }
            }
        }
        return results;
    }

    /**
     * Original {@code getMarkedBlocksAdjacentToGolem}: the marked blocks of a
     * colour that are <em>not</em> containers and are within the squared
     * {@code 4.0} — the blocks a golem places against or uses in place.
     */
    public static List<BlockPos> getMarkedBlocksAdjacentToGolem(final Level world, final EntityGolemBase golem,
                                                                final byte color) {
        final List<BlockPos> results = new ArrayList<>();
        for (final Marker marker : golem.getMarkers()) {
            final BlockEntity tile = world.getBlockEntity(new BlockPos(marker.x, marker.y, marker.z));
            if ((marker.color == color || color == -1)
                    && !(tile instanceof Container)
                    && golem.distanceToSqr(marker.x + 0.5, marker.y + 0.5, marker.z + 0.5) < ADJACENT_RANGE) {
                results.add(new BlockPos(marker.x, marker.y, marker.z));
            }
        }
        return results;
    }

    /** Original {@code getContainersWithRoom(world, golem, color)} — probes the golem's carried stack. */
    public static List<Container> getContainersWithRoom(final Level world, final EntityGolemBase golem,
                                                        final byte color) {
        return getContainersWithRoom(world, golem, color, golem.getCarried());
    }

    /**
     * Original {@code getContainersWithRoom(world, golem, color, itemToMatch)}:
     * a marked container has room when placing the stack on one of its marked
     * faces changes it — i.e. the leftover after a simulated place is not
     * stack-equal to the input.
     */
    public static List<Container> getContainersWithRoom(final Level world, final EntityGolemBase golem,
                                                        final byte color, final ItemStack itemToMatch) {
        final List<Container> results = new ArrayList<>();
        if (itemToMatch == null) {
            return results;
        }
        for (final Container inventory : getMarkedContainers(world, golem)) {
            for (final int side : getMarkedSides(golem, (BlockEntity) inventory, color)) {
                final ItemStack result = placeItemStackIntoInventory(itemToMatch, inventory, side, true);
                if (!ItemStack.matches(result, itemToMatch)) {
                    results.add(inventory);
                    break;
                }
                final Container doubleChest = getDoubleChest((BlockEntity) inventory);
                if (doubleChest != null) {
                    final ItemStack result2 = placeItemStackIntoInventory(itemToMatch, doubleChest, side, true);
                    if (!ItemStack.matches(result2, itemToMatch)) {
                        results.add(doubleChest);
                    }
                }
            }
        }
        return results;
    }

    /** Original {@code getMarkedSides(golem, tile, color)}. */
    public static List<Integer> getMarkedSides(final EntityGolemBase golem, final BlockEntity tile, final byte color) {
        return getMarkedSides(golem, tile.getBlockPos().getX(), tile.getBlockPos().getY(),
                tile.getBlockPos().getZ(), dimensionOf(tile.getLevel()), color);
    }

    /**
     * Original {@code getMarkedSides(golem, x, y, z, dim, color)}: for each of
     * the six faces, whether a marker on that face matches the golem's markers
     * with this colour (the colour {@code -1} wildcard is matched by
     * {@link Marker#equalsFuzzy}, called with the query marker as {@code this}).
     */
    public static List<Integer> getMarkedSides(final EntityGolemBase golem, final int x, final int y, final int z,
                                               final int dim, final byte color) {
        final List<Integer> out = new ArrayList<>();
        final List<Marker> markers = golem.getMarkers();
        if (markers == null || markers.isEmpty()) {
            return out;
        }
        for (int a = 0; a < 6; a++) {
            final Marker marker = new Marker(x, y, z, dim, (byte) a, color);
            if (contained(markers, marker)) {
                out.add(a);
            }
        }
        return out;
    }

    /**
     * Original {@code contained(list, marker)} — {@code marker.equalsFuzzy(mark)}
     * over the list, so the <em>query</em> marker's colour {@code -1} matches
     * everything.
     */
    public static boolean contained(final List<Marker> list, final Marker marker) {
        for (final Marker mark : list) {
            if (marker.equalsFuzzy(mark)) {
                return true;
            }
        }
        return false;
    }

    /** Original {@code getContainersWithGoods}: marked containers that can yield the sample from a marked face. */
    public static List<Container> getContainersWithGoods(final Level world, final EntityGolemBase golem,
                                                         final ItemStack goods, final byte color) {
        final List<Container> results = new ArrayList<>();
        if (goods == null) {
            return results;
        }
        for (final Container inventory : getMarkedContainers(world, golem)) {
            try {
                for (final int side : getMarkedSides(golem, (BlockEntity) inventory, color)) {
                    final ItemStack extracted = extractStack(inventory, goods, side, golem.checkOreDict(),
                            golem.ignoreDamage(), golem.ignoreNBT(), false);
                    if (extracted != null) {
                        results.add(inventory);
                        break;
                    }
                    final Container doubleChest = getDoubleChest((BlockEntity) inventory);
                    if (doubleChest != null && extractStack(doubleChest, goods, side, golem.checkOreDict(),
                            golem.ignoreDamage(), golem.ignoreNBT(), false) != null) {
                        results.add(doubleChest);
                        break;
                    }
                }
            } catch (final RuntimeException ignored) {
                // The original swallowed every exception here too (GolemHelper.java:180-181).
            }
        }
        return results;
    }

    /**
     * Original {@code getMissingItems}: what the golem still wants from its home
     * container. Toggle 0 short-circuits to "a copy of everything I carry" —
     * the original reads {@code golem.getToggles()[0]} for "ignore the home
     * container's contents", its first (unnamed in the decompilation) toggle.
     */
    public static List<ItemStack> getMissingItems(final EntityGolemBase golem) {
        final Direction facing = directionOf(golem.getHomeFacing());
        final BlockPos home = golem.getRestrictCenter();
        final BlockPos containerPos = home.relative(facing.getOpposite());
        final int slotCount = golem.getInventory().slotCount;
        if (golem.getToggles()[0]) {
            final List<ItemStack> copies = new ArrayList<>();
            for (int q = 0; q < slotCount; q++) {
                final ItemStack toCheck = golem.getInventory().inventory[q];
                if (toCheck != null) {
                    copies.add(toCheck.copy());
                }
            }
            return copies;
        }

        final BlockEntity first = golem.level().getBlockEntity(containerPos);
        if (!(first instanceof Container)) {
            return null;
        }
        final List<ItemStack> needed = new ArrayList<>();
        BlockEntity tile = first;
        outer:
        for (int q = 0; q < slotCount; q++) {
            final ItemStack toCheck = golem.getInventory().inventory[q];
            if (toCheck == null) {
                continue;
            }
            int foundAmount = 0;
            boolean repeat = true;
            boolean didRepeat = false;
            while (repeat) {
                if (didRepeat) {
                    repeat = false;
                }
                if (tile instanceof WorldlyContainer sided) {
                    final int[] slots = sided.getSlotsForFace(facing);
                    for (final int slot : slots) {
                        if (areItemStacksEqual(sided.getItem(slot), toCheck, golem.checkOreDict(),
                                golem.ignoreDamage(), golem.ignoreNBT())) {
                            foundAmount += sided.getItem(slot).getCount();
                            if (foundAmount >= golem.getInventory().getAmountNeededSmart(sided.getItem(slot),
                                    golem.getUpgradeAmount(5) > 0)) {
                                continue outer;
                            }
                        }
                    }
                } else if (tile instanceof Container container) {
                    final int size = container.getContainerSize();
                    for (int l = 0; l < size; l++) {
                        if (areItemStacksEqual(container.getItem(l), toCheck, golem.checkOreDict(),
                                golem.ignoreDamage(), golem.ignoreNBT())) {
                            foundAmount += container.getItem(l).getCount();
                            if (foundAmount >= golem.getInventory().getAmountNeededSmart(container.getItem(l),
                                    golem.getUpgradeAmount(5) > 0)) {
                                continue outer;
                            }
                        }
                    }
                } else {
                    break;
                }
                if (!didRepeat && getDoubleChest(tile) instanceof BlockEntity joined) {
                    // The original re-ran the scan against the joined half of a
                    // double chest. See GolemHelper.getDoubleChest for why the
                    // port's join seam is inert in 21.1.259; this branch is kept
                    // so the original's loop shape survives the adaptation.
                    tile = joined;
                    didRepeat = true;
                } else {
                    repeat = false;
                }
            }
            final ItemStack ret = toCheck.copy();
            ret.setCount(ret.getCount() - foundAmount);
            needed.add(ret);
        }
        return needed;
    }

    // -------------------------------------------------- essentia jar targeting ---

    /**
     * Original {@code findJarWithRoom}: the jar (or reservoir/transport) with
     * room for the golem's carried aspect, nearest first, in the original's
     * six-pass preference order. Returns the block the golem should walk to, or
     * {@code null}. Only {@link TileJarFillable} /
     * {@link TileJarFillableVoid} participate here; the original's
     * essence-transport arms live with the transport branch and are out of this
     * card's scope (noted in the docs).
     */
    public static BlockPos findJarWithRoom(final EntityGolemBase golem) {
        final Level world = golem.level();
        double dmod = golem.getRange();
        dmod *= dmod;
        final List<TileJarFillable> jars = new ArrayList<>();

        for (final Marker marker : golem.getMarkers()) {
            if (marker.dim != dimensionOf(world)) {
                continue;
            }
            final BlockEntity tile = world.getBlockEntity(new BlockPos(marker.x, marker.y, marker.z));
            if (tile instanceof TileJarFillable jar
                    && golem.getRestrictCenter().distSqr(jar.getBlockPos()) <= dmod) {
                jars.add(jar);
            }
        }

        if (jars.isEmpty()) {
            return null;
        }

        JAR_LIST.clear();
        for (final TileJarFillable jar : jars) {
            JAR_LIST.put(key(jar.getBlockPos()), jar);
            getConnectedJars(jar);
        }

        final Aspect carried = golem.getEssentia();
        final int carriedAmount = golem.getEssentiaAmount();
        List<TileJarFillable> candidates = new ArrayList<>();

        // Pass 1: a filtered jar already holding this aspect, with room.
        for (final TileJarFillable jar : JAR_LIST.values()) {
            if (jar.aspect != null && jar.amount > 0 && jar.amount < jar.maxAmount()
                    && jar.aspectFilter != null && carried != null && carriedAmount > 0
                    && jar.aspect.equals(carried) && jar.doesContainerAccept(carried)) {
                candidates.add(jar);
            }
        }
        // Pass 2: an empty filtered jar.
        if (candidates.isEmpty()) {
            for (final TileJarFillable jar : JAR_LIST.values()) {
                if ((jar.aspect == null || jar.amount == 0) && jar.aspectFilter != null
                        && jar.doesContainerAccept(carried)) {
                    candidates.add(jar);
                }
            }
        }
        // Pass 3: a full void jar with a filter for this aspect.
        if (candidates.isEmpty()) {
            for (final TileJarFillable jar : JAR_LIST.values()) {
                if (jar.aspect != null && jar.amount >= jar.maxAmount() && jar instanceof TileJarFillableVoid
                        && jar.aspectFilter != null && carried != null && carriedAmount > 0
                        && jar.aspect.equals(carried) && jar.doesContainerAccept(carried)) {
                    candidates.add(jar);
                }
            }
        }
        // Pass 4: an unfiltered jar already holding this aspect, with room.
        if (candidates.isEmpty()) {
            for (final TileJarFillable jar : JAR_LIST.values()) {
                if (jar.aspect != null && jar.amount > 0 && jar.amount < jar.maxAmount()
                        && jar.aspectFilter == null && carried != null && carriedAmount > 0
                        && jar.aspect.equals(carried) && jar.doesContainerAccept(carried)) {
                    candidates.add(jar);
                }
            }
        }
        // Pass 5: an empty unfiltered non-void jar.
        if (candidates.isEmpty()) {
            for (final TileJarFillable jar : JAR_LIST.values()) {
                if ((jar.aspect == null || jar.amount == 0) && jar.aspectFilter == null
                        && !(jar instanceof TileJarFillableVoid) && jar.doesContainerAccept(carried)) {
                    candidates.add(jar);
                }
            }
        }
        // Pass 6: an unfiltered void jar holding this aspect.
        if (candidates.isEmpty()) {
            for (final TileJarFillable jar : JAR_LIST.values()) {
                if (jar.aspect != null && jar instanceof TileJarFillableVoid && jar.aspectFilter == null
                        && carried != null && carriedAmount > 0
                        && jar.aspect.equals(carried) && jar.doesContainerAccept(carried)) {
                    candidates.add(jar);
                }
            }
        }

        BlockPos dest = null;
        double dist = Double.MAX_VALUE;
        for (final TileJarFillable jar : candidates) {
            double d = golem.getRestrictCenter().distSqr(jar.getBlockPos());
            if (jar instanceof TileJarFillableVoid) {
                d += dmod;
            }
            if (d < dist) {
                dist = d;
                dest = jar.getBlockPos();
            }
        }
        JAR_LIST.clear();
        return dest;
    }

    /**
     * Original {@code getConnectedJars}: the recursive six-neighbour flood fill
     * from a marked jar into {@link #JAR_LIST}.
     */
    static void getConnectedJars(final TileJarFillable jar) {
        final Level world = jar.getLevel();
        if (world == null) {
            return;
        }
        for (int dir = 0; dir < 6; dir++) {
            final BlockPos next = jar.getBlockPos().relative(Direction.values()[dir]);
            if (!JAR_LIST.containsKey(key(next))) {
                if (world.getBlockEntity(next) instanceof TileJarFillable other) {
                    JAR_LIST.put(key(other.getBlockPos()), other);
                    getConnectedJars(other);
                }
            }
        }
    }

    private static String key(final BlockPos pos) {
        return pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    // --------------------------------------------------------------- liquids ---

    /**
     * Original {@code getReggedLiquids}: the registered fluids, resolved once.
     * <b>Deviation (shape):</b> 1.7.10 fluid ids were integers and this returned
     * {@code ArrayList<Integer>}; 1.21.1 fluids are registry entries, so this
     * returns {@link Fluid}s. The membership is the same registry content.
     */
    public static List<Fluid> getReggedLiquids() {
        if (reggedLiquids == null) {
            final List<Fluid> fluids = new ArrayList<>();
            for (final Fluid fluid : BuiltInRegistries.FLUID) {
                if (fluid.isSource(fluid.defaultFluidState())) {
                    fluids.add(fluid);
                }
            }
            if (!fluids.contains(Fluids.WATER)) {
                fluids.add(Fluids.WATER);
            }
            if (!fluids.contains(Fluids.LAVA)) {
                fluids.add(Fluids.LAVA);
            }
            reggedLiquids = fluids;
        }
        return new ArrayList<>(reggedLiquids);
    }

    /**
     * Original {@code getMissingLiquids}: which of the registered fluids the
     * container at the golem's home face can still be filled with, honouring the
     * golem's current carried fluid and the fluid-container items in its
     * inventory. Uses the NeoForge fluid capability where the original used
     * {@code IFluidHandler}.
     */
    public static List<FluidStack> getMissingLiquids(final EntityGolemBase golem) {
        final List<FluidStack> out = new ArrayList<>();
        final BlockEntity tile = golem.level().getBlockEntity(homeContainerPos(golem));
        if (tile == null) {
            return out;
        }
        final IFluidHandler handler = golem.level().getCapability(Capabilities.FluidHandler.BLOCK,
                tile.getBlockPos(), null);
        if (handler == null) {
            return out;
        }
        final FluidStack carried = golem.getFluidCarried();
        for (final Fluid fluid : getReggedLiquids()) {
            if ((carried == null || carried.getAmount() <= 0 || carried.getFluid() == fluid)
                    && handler.isFluidValid(0, new FluidStack(fluid, 1))) {
                final FluidStack probe = new FluidStack(fluid, Integer.MAX_VALUE);
                out.add(probe);
            }
        }
        return out;
    }

    /**
     * Original {@code findPossibleLiquid}: the nearest marked fluid handler (or
     * marked fluid block) within {@link EntityGolemBase#getRange()}.
     * <b>Deviation (shape):</b> the original returned a {@code Vec3} built from a
     * {@code ChunkCoordinates}'s integer fields; the port returns the
     * {@link BlockPos} those fields held.
     */
    public static BlockPos findPossibleLiquid(final FluidStack liquid, final EntityGolemBase golem) {
        final Level world = golem.level();
        final double range = golem.getRange();
        final double rangeSq = range * range;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (final IFluidHandler handler : getMarkedFluidHandlers(liquid, world, golem)) {
            final BlockEntity tile = (BlockEntity) handler;
            final double d = golem.distanceToSqr(tile.getBlockPos().getX() + 0.5,
                    tile.getBlockPos().getY() + 0.5, tile.getBlockPos().getZ() + 0.5);
            if (d <= rangeSq && d < bestDistance) {
                bestDistance = d;
                best = tile.getBlockPos();
            }
        }
        if (best != null) {
            return best;
        }
        for (final BlockPos coord : getMarkedFluidBlocks(liquid, world, golem)) {
            final double d = golem.distanceToSqr(coord.getX() + 0.5, coord.getY() + 0.5, coord.getZ() + 0.5);
            if (d <= rangeSq && d < bestDistance) {
                bestDistance = d;
                best = coord;
            }
        }
        return best;
    }

    /** Original {@code getMarkedFluidHandlersAdjacentToGolem}: marked handlers within the squared {@code 4.0} that can yield the liquid. */
    public static List<Marker> getMarkedFluidHandlersAdjacentToGolem(final FluidStack liquid, final Level world,
                                                                    final EntityGolemBase golem) {
        final List<Marker> results = new ArrayList<>();
        for (final Marker marker : golem.getMarkers()) {
            if (marker.dim != dimensionOf(world)) {
                continue;
            }
            final BlockEntity tile = world.getBlockEntity(new BlockPos(marker.x, marker.y, marker.z));
            if (tile == null) {
                continue;
            }
            final IFluidHandler handler = world.getCapability(Capabilities.FluidHandler.BLOCK,
                    tile.getBlockPos(), directionOf(marker.side));
            if (handler == null) {
                continue;
            }
            if (golem.distanceToSqr(tile.getBlockPos().getX() + 0.5, tile.getBlockPos().getY() + 0.5,
                    tile.getBlockPos().getZ() + 0.5) < ADJACENT_RANGE) {
                final FluidStack drained = handler.drain(new FluidStack(liquid.getFluid(), 1),
                        IFluidHandler.FluidAction.SIMULATE);
                if (drained != null && drained.getAmount() > 0) {
                    results.add(marker);
                }
            }
        }
        return results;
    }

    /** Original {@code getMarkedFluidHandlers}: marked blocks whose handler can yield the liquid. */
    public static List<IFluidHandler> getMarkedFluidHandlers(final FluidStack liquid, final Level world,
                                                             final EntityGolemBase golem) {
        final List<IFluidHandler> results = new ArrayList<>();
        for (final Marker marker : golem.getMarkers()) {
            if (marker.dim != dimensionOf(world)) {
                continue;
            }
            final BlockEntity tile = world.getBlockEntity(new BlockPos(marker.x, marker.y, marker.z));
            if (tile == null) {
                continue;
            }
            final IFluidHandler handler = world.getCapability(Capabilities.FluidHandler.BLOCK,
                    tile.getBlockPos(), directionOf(marker.side));
            if (handler == null) {
                continue;
            }
            final FluidStack drained = handler.drain(new FluidStack(liquid.getFluid(), 1),
                    IFluidHandler.FluidAction.SIMULATE);
            if (drained != null && drained.getAmount() > 0) {
                results.add(handler);
            }
        }
        return results;
    }

    /**
     * Original {@code getMarkedFluidBlocks}: marked blocks that are the liquid
     * itself. 1.21.1 has a real {@code FluidState}, so the original's
     * "the block's fluid matches and the metadata is 0 (a source block)" becomes
     * "the block's fluid matches and the state is the source state" — same
     * content, no metadata.
     */
    public static List<BlockPos> getMarkedFluidBlocks(final FluidStack liquid, final Level world,
                                                      final EntityGolemBase golem) {
        final List<BlockPos> results = new ArrayList<>();
        for (final Marker marker : golem.getMarkers()) {
            if (marker.dim != dimensionOf(world)) {
                continue;
            }
            final BlockPos pos = new BlockPos(marker.x, marker.y, marker.z);
            final var state = world.getFluidState(pos);
            if (!state.isEmpty() && state.getType() == liquid.getFluid() && state.isSource()) {
                results.add(pos);
            }
        }
        return results;
    }

    // ----------------------------------------------------------- items needed ---

    /**
     * Original {@code getItemsNeeded(golem, fuzzy)}: core-dependent. Cores 1 and
     * 8 start from the golem's own inventory ("what I carry is what I want
     * refilled"); core 10 starts from the home container; every other core has
     * no list. The three filters are the original's {@code filterEmptyCore} /
     * {@code filterUseCore} / {@code filterSortCore}.
     */
    public static List<ItemStack> getItemsNeeded(final EntityGolemBase golem, final boolean fuzzy) {
        return switch (golem.getCore()) {
            case 1 -> {
                final List<ItemStack> needed = golem.getInventory().getItemsNeeded(golem.getUpgradeAmount(5) > 0);
                yield needed.isEmpty() ? null : filterEmptyCore(golem, needed);
            }
            case 8 -> {
                final List<ItemStack> needed = golem.getInventory().getItemsNeeded(golem.getUpgradeAmount(5) > 0);
                yield needed.isEmpty() ? null : filterUseCore(golem, needed);
            }
            case 10 -> {
                final List<ItemStack> in = getItemsInHomeContainer(golem);
                yield in == null ? null : filterSortCore(golem, in);
            }
            default -> null;
        };
    }

    private static List<ItemStack> filterEmptyCore(final EntityGolemBase golem, final List<ItemStack> in) {
        final List<ItemStack> out = new ArrayList<>();
        for (final ItemStack itemToMatch : in) {
            if (!isOnTimeOut(golem, itemToMatch) && findSomethingEmptyCore(golem, itemToMatch)) {
                out.add(itemToMatch);
            }
        }
        return out;
    }

    private static List<ItemStack> filterUseCore(final EntityGolemBase golem, final List<ItemStack> in) {
        final List<ItemStack> out = new ArrayList<>();
        for (final ItemStack itemToMatch : in) {
            if (!isOnTimeOut(golem, itemToMatch) && findSomethingUseCore(golem, itemToMatch)) {
                out.add(itemToMatch);
            }
        }
        return out;
    }

    private static List<ItemStack> filterSortCore(final EntityGolemBase golem, final List<ItemStack> in) {
        final List<ItemStack> out = new ArrayList<>();
        for (final ItemStack itemToMatch : in) {
            if (!isOnTimeOut(golem, itemToMatch) && findSomethingSortCore(golem, itemToMatch)) {
                out.add(itemToMatch);
            }
        }
        return out;
    }

    /** Original {@code findSomethingEmptyCore}: is there a marked destination for this stack? */
    public static boolean findSomethingEmptyCore(final EntityGolemBase golem, final ItemStack itemToMatch) {
        for (final byte color : golem.getColorsMatching(itemToMatch)) {
            final List<Container> markers = getContainersWithRoom(golem.level(), golem, color, itemToMatch);
            if (!markers.isEmpty()) {
                final BlockPos container = homeContainerPos(golem);
                final double range = golem.getRange();
                for (final Container container2 : markers) {
                    final BlockPos pos = ((BlockEntity) container2).getBlockPos();
                    final double distance = golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (distance <= range * range
                            && (pos.getX() != container.getX() || pos.getY() != container.getY()
                                || pos.getZ() != container.getZ())) {
                        return true;
                    }
                }
            }
        }
        for (final byte color : golem.getColorsMatching(itemToMatch)) {
            for (final Marker marker : golem.getMarkers()) {
                if ((marker.color == color || color == -1)
                        && !(golem.level().getBlockEntity(new BlockPos(marker.x, marker.y, marker.z))
                             instanceof Container)) {
                    return true;
                }
            }
        }
        ITEM_TIMEOUT.add(new SortingItemTimeout(golem.getId(), itemToMatch.copy(),
                System.currentTimeMillis() + GolemConfig.GOLEM_IGNORE_DELAY));
        return false;
    }

    /** Original {@code findSomethingUseCore}: a marked block with the right empty face to use the item on. */
    public static boolean findSomethingUseCore(final EntityGolemBase golem, final ItemStack itemToMatch) {
        final List<Byte> matching = golem.getColorsMatching(itemToMatch);
        for (final byte col : matching) {
            for (final Marker marker : golem.getMarkers()) {
                if ((marker.color == col || col == -1)
                        && (!golem.getToggles()[0]
                            || golem.level().isEmptyBlock(new BlockPos(marker.x, marker.y, marker.z)))
                        && (golem.getToggles()[0]
                            || !golem.level().isEmptyBlock(new BlockPos(marker.x, marker.y, marker.z)))) {
                    final Direction opp = directionOf(marker.side);
                    final BlockPos target = new BlockPos(marker.x, marker.y, marker.z).relative(opp);
                    if (golem.level().isEmptyBlock(target)) {
                        return true;
                    }
                }
            }
        }
        ITEM_TIMEOUT.add(new SortingItemTimeout(golem.getId(), itemToMatch.copy(),
                System.currentTimeMillis() + GolemConfig.GOLEM_IGNORE_DELAY));
        return false;
    }

    /** Original {@code findSomethingSortCore}: a marked container that already holds this stack. */
    public static boolean findSomethingSortCore(final EntityGolemBase golem, final ItemStack itemToMatch) {
        final List<Container> markers = getContainersWithRoom(golem.level(), golem, (byte) -1, itemToMatch);
        if (!markers.isEmpty()) {
            final BlockPos container = homeContainerPos(golem);
            final double range = golem.getRange();
            for (final Container entry : markers) {
                final BlockPos pos = ((BlockEntity) entry).getBlockPos();
                final double distance = golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (distance <= range * range
                        && (pos.getX() != container.getX() || pos.getY() != container.getY()
                            || pos.getZ() != container.getZ())) {
                    for (final int side : getMarkedSides(golem, (BlockEntity) entry, (byte) -1)) {
                        if (inventoryContains(entry, itemToMatch, side, golem.checkOreDict(),
                                golem.ignoreDamage(), golem.ignoreNBT())) {
                            return true;
                        }
                    }
                }
            }
        }
        ITEM_TIMEOUT.add(new SortingItemTimeout(golem.getId(), itemToMatch.copy(),
                System.currentTimeMillis() + GolemConfig.GOLEM_IGNORE_DELAY));
        return false;
    }

    /** Original {@code isOnTimeOut}: is this (golem, stack) pair still inside its ignore window? */
    public static boolean isOnTimeOut(final EntityGolemBase golem, final ItemStack stack) {
        final SortingItemTimeout probe = new SortingItemTimeout(golem.getId(), stack, 0L);
        if (ITEM_TIMEOUT.contains(probe)) {
            final int q = ITEM_TIMEOUT.indexOf(probe);
            final SortingItemTimeout found = ITEM_TIMEOUT.get(q);
            if (System.currentTimeMillis() < found.time) {
                return true;
            }
            ITEM_TIMEOUT.remove(q);
        }
        return false;
    }

    /**
     * Original {@code validTargetForItem}: can this stack go anywhere, for this
     * core? Core 1/8/10 route through their own finders; every other core asks
     * whether the home container still wants it.
     */
    public static boolean validTargetForItem(final EntityGolemBase golem, final ItemStack stack) {
        if (isOnTimeOut(golem, stack)) {
            return false;
        }
        switch (golem.getCore()) {
            case 1:
                return findSomethingEmptyCore(golem, stack);
            case 8:
                return findSomethingUseCore(golem, stack);
            case 10:
                return findSomethingSortCore(golem, stack);
            default:
                final List<ItemStack> neededList = getItemsNeeded(golem, golem.getUpgradeAmount(5) > 0);
                if (neededList != null && !neededList.isEmpty()) {
                    for (final ItemStack ss : neededList) {
                        if (areItemStacksEqual(ss, golem.getCarried(), golem.checkOreDict(),
                                golem.ignoreDamage(), golem.ignoreNBT())) {
                            return true;
                        }
                    }
                }
                ITEM_TIMEOUT.add(new SortingItemTimeout(golem.getId(), stack.copy(),
                        System.currentTimeMillis() + GolemConfig.GOLEM_IGNORE_DELAY));
                return false;
        }
    }

    /** Original {@code getItemsInHomeContainer}: a copy of every stack the home container exposes on the home face. */
    public static List<ItemStack> getItemsInHomeContainer(final EntityGolemBase golem) {
        final Direction facing = directionOf(golem.getHomeFacing());
        final BlockPos containerPos = homeContainerPos(golem);
        final BlockEntity tile = golem.level().getBlockEntity(containerPos);
        if (!(tile instanceof Container inv)) {
            return null;
        }
        int[] slots;
        if (inv instanceof WorldlyContainer sided) {
            slots = sided.getSlotsForFace(facing);
        } else {
            slots = new int[inv.getContainerSize()];
            for (int a = 0; a < slots.length; a++) {
                slots[a] = a;
            }
        }
        final List<ItemStack> out = new ArrayList<>();
        for (final int slot : slots) {
            if (!inv.getItem(slot).isEmpty()) {
                out.add(inv.getItem(slot).copy());
            }
        }
        return out;
    }

    /**
     * Original {@code GolemHelper.SortingItemTimeout}: a (golem, stack) pair with
     * an expiry. The {@code Comparable} implementation compared by
     * {@code golemId} then stack count then stack; equality is golemId + stack,
     * which is what {@code isOnTimeOut} keys on.
     */
    public static final class SortingItemTimeout implements Comparable<SortingItemTimeout> {
        final ItemStack stack;
        final int golemId;
        final long time;

        public SortingItemTimeout(final int golemId, final ItemStack stack, final long time) {
            this.stack = stack;
            this.golemId = golemId;
            this.time = time;
        }

        @Override
        public int compareTo(final SortingItemTimeout other) {
            if (this.golemId != other.golemId) {
                return this.golemId - other.golemId;
            }
            if (this.stack.getCount() != other.stack.getCount()) {
                return this.stack.getCount() - other.stack.getCount();
            }
            return Integer.compare(ItemStack.hashItemAndComponents(this.stack),
                    ItemStack.hashItemAndComponents(other.stack));
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof SortingItemTimeout other)) {
                return false;
            }
            return this.golemId == other.golemId && ItemStack.isSameItemSameComponents(this.stack, other.stack);
        }

        @Override
        public int hashCode() {
            return this.golemId * 31 + ItemStack.hashItemAndComponents(this.stack);
        }
    }

    // --------------------------------------------------- inventory item seam ---

    /**
     * The port's {@code InventoryUtils.placeItemStackIntoInventory}: tries to
     * place {@code stack} into {@code inventory} on the given face, returning
     * what did not fit. With {@code simulate}, nothing is mutated — which is how
     * the original's {@code getContainersWithRoom} probes for room without
     * moving anything.
     */
    static ItemStack placeItemStackIntoInventory(final ItemStack stack, final Container inventory,
                                                 final int side, final boolean simulate) {
        if (stack == null || stack.isEmpty()) {
            return stack;
        }
        final int[] slots = slotsFor(inventory, side);
        ItemStack remaining = stack.copy();
        for (final int slot : slots) {
            if (remaining.isEmpty()) {
                break;
            }
            if (!sidedCanPlace(inventory, slot, remaining, side)) {
                continue;
            }
            final ItemStack existing = inventory.getItem(slot);
            if (existing.isEmpty()) {
                final int move = Math.min(remaining.getCount(), inventory.getMaxStackSize());
                if (!simulate) {
                    final ItemStack placed = remaining.copy();
                    placed.setCount(move);
                    inventory.setItem(slot, placed);
                    inventory.setChanged();
                }
                remaining.shrink(move);
            } else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                final int space = Math.min(inventory.getMaxStackSize(), existing.getMaxStackSize())
                        - existing.getCount();
                if (space > 0) {
                    final int move = Math.min(space, remaining.getCount());
                    if (!simulate) {
                        existing.setCount(existing.getCount() + move);
                        inventory.setChanged();
                    }
                    remaining.shrink(move);
                }
            }
        }
        return remaining;
    }

    /**
     * The port's {@code InventoryUtils.extractStack}: pulls {@code sample}'s
     * count out of the container on a face, returning the extracted stack or
     * {@code null} when nothing matched. With {@code simulate} nothing is
     * mutated.
     */
    static ItemStack extractStack(final Container inventory, final ItemStack sample, final int side,
                                  final boolean oredict, final boolean ignoreDamage, final boolean ignoreNBT,
                                  final boolean simulate) {
        if (sample == null) {
            return null;
        }
        for (final int slot : slotsFor(inventory, side)) {
            final ItemStack found = inventory.getItem(slot);
            if (found.isEmpty() || !areItemStacksEqual(found, sample, oredict, ignoreDamage, ignoreNBT)) {
                continue;
            }
            final int amount = Math.min(found.getCount(), sample.getCount());
            if (amount <= 0) {
                continue;
            }
            final ItemStack extracted = found.copy();
            extracted.setCount(amount);
            if (!simulate) {
                found.shrink(amount);
                if (found.isEmpty()) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                }
                inventory.setChanged();
            }
            return extracted;
        }
        return null;
    }

    /** The port's {@code InventoryUtils.inventoryContains}. */
    static boolean inventoryContains(final Container inventory, final ItemStack sample, final int side,
                                     final boolean oredict, final boolean ignoreDamage, final boolean ignoreNBT) {
        for (final int slot : slotsFor(inventory, side)) {
            if (areItemStacksEqual(inventory.getItem(slot), sample, oredict, ignoreDamage, ignoreNBT)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The port's {@code InventoryUtils.areItemStacksEqual(stack1, stack2, oredict,
     * ignoreDamage, ignoreNBT)}. An empty stack never matches; the exact branch is
     * item + components; {@code ignoreNBT} falls back to item identity and
     * {@code ignoreDamage} additionally allows a differing damage value.
     */
    static boolean areItemStacksEqual(final ItemStack stack1, final ItemStack stack2, final boolean oredict,
                                      final boolean ignoreDamage, final boolean ignoreNBT) {
        if (stack1 == null || stack2 == null || stack1.isEmpty() || stack2.isEmpty()) {
            return false;
        }
        if (!ignoreNBT) {
            return ItemStack.isSameItemSameComponents(stack1, stack2);
        }
        if (!ignoreDamage) {
            return ItemStack.isSameItem(stack1, stack2)
                    && stack1.getDamageValue() == stack2.getDamageValue();
        }
        return ItemStack.isSameItem(stack1, stack2);
    }

    private static int[] slotsFor(final Container inventory, final int side) {
        if (inventory instanceof WorldlyContainer sided && side >= 0) {
            return sided.getSlotsForFace(directionOf(side));
        }
        final int[] slots = new int[inventory.getContainerSize()];
        for (int a = 0; a < slots.length; a++) {
            slots[a] = a;
        }
        return slots;
    }

    private static boolean sidedCanPlace(final Container inventory, final int slot, final ItemStack stack,
                                         final int side) {
        return !(inventory instanceof WorldlyContainer sided && side >= 0)
                || sided.canPlaceItemThroughFace(slot, stack, directionOf(side));
    }

    /**
     * Original {@code InventoryUtils.getDoubleChest}: 1.21.1's
     * {@code ChestBlockEntity} joins its neighbour through
     * {@code getCapability} rather than a utility, and the port's
     * {@link #getMarkedContainers} resolves the joined neighbour through the
     * level's own container accessor. Returns {@code null} when the block
     * entity is not a container with a joined neighbour.
     */
    /**
     * Original {@code InventoryUtils.getDoubleChest}.
     *
     * <p><b>Deviation (documented).</b> 1.7.10's {@code TileEntityChest} exposed
     * its joined neighbour through a utility; 1.21.1's {@code ChestBlockEntity}
     * resolves the joined half through its own block's container lookup
     * ({@code ChestBlockEntity.getContainer} is not public in 21.1.259), and a
     * marked double chest is already reachable as a single {@link Container}
     * through {@code Level.getBlockEntity}. The port therefore returns
     * {@code null}: a marked double chest is seen <em>once</em> rather than
     * twice. Consequence: the original's duplicate-entry behaviour when a marked
     * chest is half of a double chest no longer doubles a candidate list, which
     * changes only list multiplicity, never which block is chosen (every
     * consumer takes the first/nearest match).
     */
    static Container getDoubleChest(final BlockEntity tile) {
        return null;
    }

    /** The world's dimension identity, compared against {@link Marker#dim}. */
    static int dimensionOf(final Level world) {
        return world.dimension().location().hashCode();
    }
}
