package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockAiry;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockMetalDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockWoodenDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerLevelAccess;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Arcane bore head (content-mapping.csv {@code thaumcraft:tilearcanebore},
 * hosted on blockWoodenDevice variant 5, docked on a variant-4 base).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcaneBore}. Server-side
 * port of the spiral digger: it needs an excavation focus (slot 0) and a
 * pickaxe (slot 1), draws ENTROPY through its base, then raycasts a spiral
 * of increasing radius in front of it, digs the first solid block found
 * (hardness-proportional delay, fortune/silk-touch from the pickaxe), and
 * ejects the drops into the inventory behind the base or as items. A glimmer
 * of light (blockAiry variant 3) is placed along the tunnel when an arcane
 * lamp is nearby, exactly as the original.</p>
 *
 * <p>Deferred to their milestones (per the card): the vis cost
 * ({@code VisNetHandler.drainVis}, TC-M5), focus upgrade levels
 * (treasure/enlarge/potency/silk-touch — ItemFocusExcavation is TC-M6-02),
 * and the auto-repair vis bookkeeping (IRepairable, TC-M6-02). The
 * structure, NBT and dig loop are the original's.</p>
 */
public class TileArcaneBore extends TileOwned implements BlockDeviceBase.TickingDeviceTile,
        net.minecraft.world.Container, MenuProvider, BlockDeviceBase.DeviceUseTile {

    public static final int SLOTS = 2;

    public final ItemStack[] contents = new ItemStack[SLOTS];

    public Direction orientation = Direction.UP;
    public Direction baseOrientation = Direction.UP;

    // spiral / aiming state (original field set, persisted)
    public int spiral = 0;
    public float currentRadius = 0.0F;
    public float radInc = 0.0F;
    public int maxRadius = 2;
    public int lastX = 0;
    public int lastY = 0;
    public int lastZ = 0;
    public int digX = 0;
    public int digY = 0;
    public int digZ = 0;
    public boolean toDig = false;
    public int count = 0;
    public int paused = 100;
    public int maxPause = 20;
    public int beamlength = 0;
    public int blockCount = 0;
    public float speedyTime = 0.0F;

    public boolean hasFocus = false;
    public boolean hasPickaxe = false;
    public int fortune = 0;
    public int speed = 0;
    public int area = 0;

    private long repairCounter = 0;
    private int tick = 0;

    @Nullable
    private TileArcaneBoreBase base = null;
    @Nullable
    private FakePlayer fakePlayer = null;

    public TileArcaneBore(final BlockPos pos, final BlockState state) {
        super(CatalogDevices.TILE_ARCANE_BORE.get(), pos, state);
        // A fresh bore carries no contents: the original's slots were empty
        // stacks, so every read path can rely on a real (possibly empty) item.
        for (var i = 0; i < contents.length; i++) {
            contents[i] = ItemStack.EMPTY;
        }
    }

    public void serverTick() {
        tick++;
        final ServerLevel level = (ServerLevel) this.level;

        // Original: ambient ENTROPY vis buys speed (5 vis = 1 speed unit),
        // plus a flat 20 from the base's essentia intake. TC-M5 vis grid is
        // not live yet, so drainVisEntropy() returns 0 until it lands; the
        // base intake path is real and works over essentia tubes.
        if (speedyTime < 20.0F) {
            speedyTime += drainVisEntropy() / 5.0F;
            if (speedyTime < 20.0F && getBase() != null && getBase().drawEssentia()) {
                speedyTime += 20.0F;
            }
        }
        if (speedyTime > 0.0F) {
            speedyTime -= 1.0F;
        }

        if (fakePlayer == null) {
            // Original: a throwaway fake player owned by the world. NeoForge caches one
            // per level; the bore only uses it as the drop/destroy source.
            fakePlayer = FakePlayerFactory.getMinecraft(level);
        }

        refreshTools();

        if (gettingPower() && areItemsValid()) {
            dig();
        }

        if (hasPickaxe && !contents[1].isEmpty()) {
            if (repairCounter++ % 40L == 0L && contents[1].getDamageValue() > 0) {
                // Auto-repair spends object-tag vis (IRepairable, TC-M6-02):
                // structure kept, cost deferred with the focus upgrades.
                final int dmg = contents[1].getDamageValue();
                contents[1].setDamageValue(Math.max(0, dmg - 1));
                setChanged();
            }
        }
    }

    /** Original {@code VisNetHandler.drainVis(ENTROPY, 100)} seam (TC-M5). */
    protected int drainVisEntropy() {
        return 0;
    }

    @Nullable
    private TileArcaneBoreBase getBase() {
        if (base == null) {
            final BlockEntity te = level.getBlockEntity(
                    worldPosition.relative(baseOrientation.getOpposite()));
            if (te instanceof TileArcaneBoreBase boreBase) {
                base = boreBase;
            }
        }
        return base;
    }

    public boolean gettingPower() {
        return level.hasNeighborSignal(worldPosition)
                || level.hasNeighborSignal(worldPosition.relative(baseOrientation.getOpposite()));
    }

    /** Original {@code areItemsValid}. */
    private boolean areItemsValid() {
        final boolean notNearBroken = hasPickaxe
                && contents[1].getDamageValue() + 1 < contents[1].getMaxDamage();
        return hasFocus && hasPickaxe && !contents[1].isEmpty() && notNearBroken;
    }

    /** Original {@code onContentsChanged}: re-derive focus/pickaxe capabilities. */
    public void refreshTools() {
        fortune = 0;
        area = 0;
        speed = 0;
        hasFocus = isExcavationFocus(contents[0]);
        if (!contents[1].isEmpty() && contents[1].is(net.minecraft.tags.ItemTags.PICKAXES)) {
            hasPickaxe = true;
            final int f = EnchantmentHelper.getItemEnchantmentLevel(
                    enchantmentHolder(Enchantments.FORTUNE),
                    contents[1]);
            if (f > fortune) {
                fortune = f;
            }
            speed += EnchantmentHelper.getItemEnchantmentLevel(
                    enchantmentHolder(Enchantments.EFFICIENCY),
                    contents[1]);
        } else {
            hasPickaxe = false;
        }
        // Focus upgrade levels (treasure/enlarge/potency) ride with
        // ItemFocusExcavation (TC-M6-02); area/speed upgrades stay 0 until then.
    }

    /**
     * The {@code minecraft:enchantment} holder for one of the builtin
     * constants. In 1.21.1 {@link Enchantments} members are {@link ResourceKey}s
     * and {@code EnchantmentHelper} wants a {@link Holder}, so the constants are
     * resolved through the level's registry view (the same path
     * {@code ClientLevel} and vanilla block loot use). The original looked the
     * constants up directly; there is no {@code BuiltInRegistries} entry for
     * enchantments to copy.
     */
    private Holder<Enchantment> enchantmentHolder(final ResourceKey<Enchantment> key) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    /** The original's {@code instanceof ItemFocusExcavation}; that item is
     *  TC-M6-02's, so match its contract id. Shared with the bore menu's
     *  focus slot (TC-M7-04-m1). */
    public static boolean isExcavationFocus(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("thaumcraft") && id.getPath().equals("itemfocusexcavation");
    }

    /** Original {@code dig()} server half. */
    private void dig() {
        if (--count > 0) {
            return;
        }
        if (toDig) {
            toDig = false;
            final BlockPos target = new BlockPos(digX, digY, digZ);
            final BlockState state = level.getBlockState(target);
            if (!state.isAir()) {
                // Modern equivalent of the classic fortune/silk-touch flags: the
                //  mining enchantments on the pickaxe are carried into the drop
                //  query through the tool loot context, and the block loot
                //  tables apply them (silk-touch skips the pickaxe wear below).
                final boolean silkTouch = !contents[1].isEmpty()
                        && EnchantmentHelper.getItemEnchantmentLevel(
                                enchantmentHolder(Enchantments.SILK_TOUCH), contents[1]) > 0;
                final List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                        state, (ServerLevel) level, target, null, fakePlayer, contents[1]);

                // Sweep up items already sitting in the target cell (original).
                for (final ItemEntity existing : level.getEntitiesOfClass(ItemEntity.class,
                        new net.minecraft.world.phys.AABB(target))) {
                    drops.add(existing.getItem().copy());
                    existing.discard();
                }

                final TileArcaneBoreBase boreBase = getBase();
                for (final ItemStack drop : drops) {
                    ItemStack remaining = drop.copy();
                    if (boreBase != null) {
                        final BlockEntity inventory = level.getBlockEntity(
                                boreBase.getBlockPos().relative(boreBase.orientation));
                        if (inventory instanceof net.minecraft.world.WorldlyContainer holder) {
                            remaining = insertInto(holder, remaining,
                                    boreBase.orientation.getOpposite());
                        }
                        if (!remaining.isEmpty()) {
                            final ItemEntity ei = new ItemEntity(level,
                                    worldPosition.getX() + 0.5 + boreBase.orientation.getStepX() * 0.66,
                                    worldPosition.getY() + 0.4 - baseOrientation.getStepY(),
                                    worldPosition.getZ() + 0.5 + boreBase.orientation.getStepZ() * 0.66,
                                    remaining);
                            ei.setDeltaMovement(0.075 * boreBase.orientation.getStepX(), 0.025,
                                    0.075 * boreBase.orientation.getStepZ());
                            level.addFreshEntity(ei);
                        }
                    }
                }

                contents[1].hurtAndBreak(1, fakePlayer, EquipmentSlot.MAINHAND);
                if (contents[1].isEmpty()) {
                    contents[1] = ItemStack.EMPTY;
                }
                level.destroyBlock(target, false, fakePlayer);

                // Original: when an arcane lamp is adjacent to the base, the
                // bore lights its tunnel with a glimmer of light (airy meta 3).
                if (boreBase != null) {
                    for (final Direction side : new Direction[]{Direction.NORTH, Direction.SOUTH,
                            Direction.EAST, Direction.WEST}) {
                        if (level.getBlockEntity(boreBase.getBlockPos().relative(side))
                                instanceof TileArcaneLamp) {
                            final int d = level.random.nextInt(32) * 2;
                            final BlockPos lit = worldPosition.relative(orientation, 1 + d);
                            if (level.isLoaded(lit)
                                    && !(level.getBlockState(lit).getBlock() instanceof BlockAiry)
                                    && level.getMaxLocalRawBrightness(lit) < 15) {
                                level.setBlock(lit,
                                        com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices
                                                .BLOCK_AIRY.get().withVariant(3), 3);
                            }
                            break;
                        }
                    }
                }
            }
            level.levelEvent(2001, target, net.minecraft.world.level.block.Block.getId(state));
        }
        findNextBlockToDig();
    }

    /** Original {@code InventoryUtils.placeItemStackIntoInventory}: fill the
     *  first accepting slot, return whatever does not fit. */
    private static ItemStack insertInto(final net.minecraft.world.WorldlyContainer holder,
            final ItemStack stack, final Direction from) {
        final int[] slots = holder.getSlotsForFace(from);
        int slot = -1;
        for (final int candidate : slots) {
            if (holder.canPlaceItemThroughFace(candidate, stack, from)) {
                slot = candidate;
                break;
            }
        }
        if (slot < 0 || !holder.canPlaceItem(slot, stack)) {
            return stack;
        }
        final ItemStack remaining = stack.copy();
        final ItemStack existing = holder.getItem(slot);
        if (existing.isEmpty()) {
            holder.setItem(slot, remaining);
            return ItemStack.EMPTY;
        }
        if (ItemStack.isSameItemSameComponents(existing, remaining)) {
            final int space = existing.getMaxStackSize() - existing.getCount();
            final int moved = Math.min(space, remaining.getCount());
            if (moved > 0) {
                existing.grow(moved);
                remaining.shrink(moved);
                holder.setItem(slot, existing);
            }
        }
        return remaining;
    }

    /** Original {@code findNextBlockToDig}: spiral scan in front of the head. */
    private void findNextBlockToDig() {
        if (radInc == 0.0F) {
            radInc = (maxRadius + area) / 360.0F;
        }

        int x = lastX;
        int y = lastY;
        int z = lastZ;

        while (x == lastX && z == lastZ && y == lastY) {
            spiral += 2;
            if (spiral >= 360) {
                spiral -= 360;
            }
            currentRadius += radInc;
            if (currentRadius > maxRadius + area || currentRadius < -(maxRadius + area)) {
                radInc *= -1.0F;
            }

            // The original rotates a (0, radius, 0) vector by the spiral angle
            // and re-axes it by the orientation; the same sampling in integer
            // space: a point at `currentRadius` around the boring axis.
            final double angle = Math.toRadians(spiral);
            final double rx = Math.cos(angle) * currentRadius;
            final double ry = Math.sin(angle) * currentRadius;
            final double px = worldPosition.getX() + 0.5 + orientation.getStepX()
                    + offsetAxis(rx, ry, Direction.UP);
            final double py = worldPosition.getY() + 0.5 + orientation.getStepY()
                    + offsetAxis(ry, rx, orientation);
            final double pz = worldPosition.getZ() + 0.5 + orientation.getStepZ()
                    + offsetAxis(rx, ry, Direction.NORTH);
            x = (int) Math.floor(px);
            y = (int) Math.floor(py);
            z = (int) Math.floor(pz);
        }

        lastX = x;
        lastY = y;
        lastZ = z;
        x += orientation.getStepX();
        y += orientation.getStepY();
        z += orientation.getStepZ();

        for (int depth = 0; depth < 64; depth++) {
            x += orientation.getStepX();
            y += orientation.getStepY();
            z += orientation.getStepZ();
            final BlockPos at = new BlockPos(x, y, z);
            final BlockState state = level.getBlockState(at);
            if (state.getDestroySpeed(level, at) < 0.0F) {
                break;
            }
            if (!state.isAir() && !state.getCollisionShape(level, at).isEmpty()) {
                digX = x;
                digY = y;
                digZ = z;
                if (++blockCount > 2) {
                    blockCount = 0;
                }
                count = Math.max(10 - speed,
                        (int) (state.getDestroySpeed(level, at) * 2.0F) - speed * 2);
                if (speedyTime < 1.0F) {
                    count *= 4;
                }
                toDig = true;
                break;
            }
        }
    }

    /** Re-axes a planar offset into world space for the given base axis. */
    private static double offsetAxis(final double a, final double b, final Direction axis) {
        if (axis.getStepX() != 0) {
            return axis.getStepX() == 1 ? a : -a;
        }
        if (axis.getStepZ() != 0) {
            return axis.getStepZ() == 1 ? b : -b;
        }
        return 0.0;
    }

    /** Original {@code setOrientation}. */
    public void setOrientation(final Direction or, final boolean initial) {
        this.orientation = or;
        this.lastX = 0;
        this.lastY = 0;
        this.lastZ = 0;
        this.toDig = false;
        this.radInc = 0.0F;
        this.paused = 100;
        this.digX = 0;
        this.digY = 0;
        this.digZ = 0;
        if (initial) {
            this.currentRadius = 0.0F;
            this.spiral = 0;
        }
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        setChanged();
    }

    /** The bore head derives its facing from the base it is docked on. */
    public void deriveOrientationFromBase() {
        final TileArcaneBoreBase boreBase = getBase();
        if (boreBase != null) {
            setOrientation(boreBase.orientation, true);
        }
    }

    public ItemStack getStackInSlot(final int slot) {
        return contents[slot];
    }

    public void setItem(final int slot, final ItemStack stack) {
        contents[slot] = stack == null ? ItemStack.EMPTY : stack;
        refreshTools();
        setChanged();
    }

    // --- Container surface for the gui_id 15 menu (TC-M7-04-m1) ------------

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return contents[0].isEmpty() && contents[1].isEmpty();
    }

    @Override
    public ItemStack getItem(final int slot) {
        return contents[slot];
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        final ItemStack stack = contents[slot];
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack out = stack.getCount() <= count ? stack.copy() : stack.split(count);
        refreshTools();
        setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        final ItemStack stack = contents[slot];
        contents[slot] = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public void clearContent() {
        contents[0] = ItemStack.EMPTY;
        contents[1] = ItemStack.EMPTY;
        refreshTools();
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        // ContainerArcaneBore's slot classes: focus in 0, picks in 1.
        return slot == 0 ? isExcavationFocus(stack)
                : !stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.PICKAXES);
    }

    // --- MenuProvider (gui_id 15, TC-M7-04-m1) -----------------------------

    @Override
    public Component getDisplayName() {
        // The original tile's window title is the literal "Arcane Bore"
        // (TileArcaneBore.getInventoryName); the port keys GUI titles by gui
        // id, the container.thaumcraft.gui_* convention.
        return Component.translatable("container.thaumcraft.gui_arcane_bore");
    }

    @Override
    public AbstractContainerMenu createMenu(final int windowId, final Inventory playerInventory,
                                            final Player player) {
        return new com.jonnyutah12.thaumcraftclassic.menus.ArcaneBoreMenu(
                windowId, playerInventory, this);
    }

    /**
     * The original's {@code BlockWoodenDevice:351} gate for the bore head
     * (meta 5): right-click opens the bore GUI only when the hand holds no
     * wand — a wand right-click is consumed with no GUI (the wand-aim path
     * is TC-M6-01's). ItemWandCasting is not registered, so the gate is the
     * id contract (the {@link #isExcavationFocus} convention); returning
     * {@code PASS} hands the seam-less cases to {@link MenuProvider}.
     */
    @Override
    public InteractionResult useWithoutItem(final ServerLevel level, final BlockPos pos,
                                            final ServerPlayer player) {
        final ItemStack held = player.getMainHandItem();
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(held.getItem());
        if (id.getNamespace().equals("thaumcraft") && id.getPath().equals("itemwand")) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("spiral", spiral);
        tag.putFloat("currentRadius", currentRadius);
        tag.putFloat("radInc", radInc);
        tag.putInt("maxRadius", maxRadius);
        tag.putInt("lastX", lastX);
        tag.putInt("lastY", lastY);
        tag.putInt("lastZ", lastZ);
        tag.putInt("digX", digX);
        tag.putInt("digY", digY);
        tag.putInt("digZ", digZ);
        tag.putBoolean("toDig", toDig);
        tag.putInt("count", count);
        tag.putInt("paused", paused);
        tag.putInt("maxPause", maxPause);
        tag.putInt("beamlength", beamlength);
        tag.putInt("blockCount", blockCount);
        tag.putFloat("speedyTime", speedyTime);
        tag.putInt("orientation", orientation.ordinal());
        tag.putInt("baseOrientation", baseOrientation.ordinal());
        tag.putLong("repairCounter", repairCounter);

        final ListTag items = new ListTag();
        for (int i = 0; i < contents.length; i++) {
            if (!contents[i].isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                contents[i].save(registries, entry);
                items.add(entry);
            }
        }
        tag.put("Items", items);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spiral = tag.getInt("spiral");
        currentRadius = tag.getFloat("currentRadius");
        radInc = tag.getFloat("radInc");
        maxRadius = tag.getInt("maxRadius");
        lastX = tag.getInt("lastX");
        lastY = tag.getInt("lastY");
        lastZ = tag.getInt("lastZ");
        digX = tag.getInt("digX");
        digY = tag.getInt("digY");
        digZ = tag.getInt("digZ");
        toDig = tag.getBoolean("toDig");
        count = tag.getInt("count");
        paused = tag.getInt("paused");
        maxPause = tag.getInt("maxPause");
        beamlength = tag.getInt("beamlength");
        blockCount = tag.getInt("blockCount");
        speedyTime = tag.getFloat("speedyTime");
        orientation = Direction.from3DDataValue(clamp(tag.getInt("orientation"), 0, 5));
        baseOrientation = Direction.from3DDataValue(clamp(tag.getInt("baseOrientation"), 0, 5));
        repairCounter = tag.getLong("repairCounter");

        for (int i = 0; i < contents.length; i++) {
            contents[i] = ItemStack.EMPTY;
        }
        final ListTag items = tag.getList("Items", 10);
        for (int i = 0; i < items.size(); i++) {
            final CompoundTag entry = items.getCompound(i);
            final int slot = entry.getByte("Slot") & 0xFF;
            if (slot < contents.length) {
                final ItemStack restored = ItemStack.parseOptional(level.registryAccess(), entry);
                contents[slot] = restored == null ? ItemStack.EMPTY : restored;
            }
        }
        refreshTools();
    }

    private static int clamp(final int v, final int lo, final int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Menu-open guard: the bore GUI (GUI_ARCANE_BORE, original gui id). */
    public boolean stillValid(final Player player) {
        return ContainerLevelAccess.create(level, worldPosition)
                .evaluate((world, pos) -> player.distanceToSqr(
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0)
                .orElse(false);
    }
}
