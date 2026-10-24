package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604dFurniture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Magic mirror (content-mapping.csv {@code thaumcraft:tilemirror}).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileMirror}. A wall-mounted
 * mirror paired with another: item entities that touch the block are
 * teleported into the destination's output buffer and re-spawned on the far
 * side at a rate that degrades with instability (instability grows per
 * transported item, decays 1/20t, and the original also drains ORDER vis from
 * the vis net to reduce it — that hook defers to TC-M4-02 and is skipped
 * here). Link validity is re-checked with a backoff (inc 40→600t). Links
 * survive dimension changes via the stored level key.</p>
 */
public class TileMirror extends BlockEntity implements com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.TickingDeviceTile {

    public boolean linked = false;
    public int linkX;
    public int linkY;
    public int linkZ;
    @Nullable
    public ResourceKey<Level> linkDim;
    public int instability;
    int count = 0;
    int inc = 40;
    private final List<ItemStack> outputStacks = new ArrayList<>();

    public TileMirror(final BlockPos pos, final BlockState state) {
        super(M604dFurniture.TILE_MIRROR.get(), pos, state);
    }

    /** Original restoreLink(): point the destination back at this mirror. */
    public void restoreLink() {
        if (isDestinationValid()) {
            final ServerLevel targetWorld = levelOf(this.linkDim);
            if (targetWorld == null) {
                return;
            }
            final BlockEntity te = targetWorld.getBlockEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
            if (te instanceof TileMirror tm) {
                tm.linked = true;
                tm.linkX = this.worldPosition.getX();
                tm.linkY = this.worldPosition.getY();
                tm.linkZ = this.worldPosition.getZ();
                tm.linkDim = this.level.dimension();
                this.linked = true;
                this.setChanged();
                tm.setChanged();
            }
        }
    }

    public void invalidateLink() {
        final ServerLevel targetWorld = levelOf(this.linkDim);
        if (targetWorld != null && targetWorld.isLoaded(new BlockPos(this.linkX, this.linkY, this.linkZ))) {
            final BlockEntity te = targetWorld.getBlockEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
            if (te instanceof TileMirror tm) {
                tm.linked = false;
                this.setChanged();
                tm.setChanged();
            }
        }
    }

    public boolean isLinkValid() {
        if (!this.linked) {
            return false;
        }
        final ServerLevel targetWorld = levelOf(this.linkDim);
        if (targetWorld == null) {
            return false;
        }
        final BlockEntity te = targetWorld.getBlockEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
        if (te instanceof TileMirror tm && tm.linked
                && tm.linkX == this.worldPosition.getX() && tm.linkY == this.worldPosition.getY()
                && tm.linkZ == this.worldPosition.getZ() && tm.linkDim == this.level.dimension()) {
            return true;
        }
        this.linked = false;
        this.setChanged();
        return false;
    }

    public boolean isLinkValidSimple() {
        if (!this.linked) {
            return false;
        }
        final ServerLevel targetWorld = levelOf(this.linkDim);
        if (targetWorld == null) {
            return false;
        }
        final BlockEntity te = targetWorld.getBlockEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
        return te instanceof TileMirror tm && tm.linked
                && tm.linkX == this.worldPosition.getX() && tm.linkY == this.worldPosition.getY()
                && tm.linkZ == this.worldPosition.getZ() && tm.linkDim == this.level.dimension();
    }

    public boolean isDestinationValid() {
        final ServerLevel targetWorld = levelOf(this.linkDim);
        if (targetWorld == null) {
            return false;
        }
        final BlockEntity te = targetWorld.getBlockEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
        if (te instanceof TileMirror tm) {
            return !tm.isLinkValid();
        }
        this.linked = false;
        this.setChanged();
        return false;
    }

    /** Original transport(EntityItem): buffer the stack at the destination. */
    public boolean transport(final ItemEntity item) {
        if (this.level.isClientSide) {
            return false;
        }
        final ItemStack stack = item.getItem();
        if (this.linked && isLinkValid()) {
            final ServerLevel world = levelOf(this.linkDim);
            final BlockEntity target = world.getBlockEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
            if (target instanceof TileMirror tm) {
                tm.outputStacks.add(stack.copy());
                this.addInstability(null, stack.getCount());
                item.discard();
                this.setChanged();
                tm.setChanged();
                return true;
            }
        }
        return false;
    }

    public void eject() {
        if (!this.outputStacks.isEmpty() && this.count > 20) {
            final int i = this.level.random.nextInt(this.outputStacks.size());
            final ItemStack buffered = this.outputStacks.get(i);
            if (!buffered.isEmpty()) {
                final ItemStack outItem = buffered.copy();
                outItem.setCount(1);
                if (spawnItem(outItem)) {
                    buffered.shrink(1);
                    this.addInstability(null, 1);
                    if (buffered.getCount() <= 0) {
                        this.outputStacks.remove(i);
                    }
                    this.setChanged();
                }
            }
        }
    }

    public boolean spawnItem(final ItemStack stack) {
        final Direction face = facing();
        final ItemEntity item = new ItemEntity(this.level,
                this.worldPosition.getX() + 0.5 - face.getStepX() * 0.3,
                this.worldPosition.getY() + 0.5 - face.getStepY() * 0.3,
                this.worldPosition.getZ() + 0.5 - face.getStepZ() * 0.3,
                stack);
        item.setDeltaMovement(face.getStepX() * 0.15, face.getStepY() * 0.15, face.getStepZ() * 0.15);
        item.setPickUpDelay(20);
        this.level.addFreshEntity(item);
        return true;
    }

    /** The direction the mirror face points (block variant carries it). */
    private Direction facing() {
        final int meta = getBlockState().getBlock()
                instanceof com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase dev
                ? dev.variant(getBlockState()) : 0;
        return com.jonnyutah12.thaumcraftclassic.blocks.BlockMirror.facingFor(meta);
    }

    protected void addInstability(@Nullable final ServerLevel targetWorld, final int amt) {
        this.instability += amt;
        if (targetWorld != null) {
            final BlockEntity te = targetWorld.getBlockEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
            if (te instanceof TileMirror tm) {
                tm.instability += amt;
                if (tm.instability < 0) {
                    tm.instability = 0;
                }
                tm.setChanged();
            }
        }
    }

    public void serverTick() {
        final int tickrate = this.instability / 50;
        if (tickrate == 0 || this.count % (tickrate * tickrate) == 0) {
            eject();
        }
        checkInstability();
        if (this.count++ % this.inc == 0) {
            if (!isLinkValidSimple()) {
                if (this.inc < 600) {
                    this.inc += 20;
                }
                restoreLink();
            } else {
                this.inc = 40;
            }
        }
    }

    public void checkInstability() {
        if (this.instability > 0 && this.count % 20 == 0) {
            this.instability--;
            this.setChanged();
        }
        // Original also drains ORDER vis via VisNetHandler to cancel
        // instability; the vis net is TC-M4-02 (not started) — skipped here.
    }

    @Nullable
    ServerLevel levelOf(@Nullable final ResourceKey<Level> dim) {
        if (dim == null) {
            return null;
        }
        final MinecraftServer server = this.level.getServer();
        return server == null ? null : server.getLevel(dim);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("linked", this.linked);
        tag.putInt("linkX", this.linkX);
        tag.putInt("linkY", this.linkY);
        tag.putInt("linkZ", this.linkZ);
        if (this.linkDim != null) {
            tag.putString("linkDim", this.linkDim.location().toString());
        }
        tag.putInt("instability", this.instability);
        final ListTag items = new ListTag();
        for (final ItemStack stack : this.outputStacks) {
            if (!stack.isEmpty() && stack.getCount() > 0) {
                items.add(stack.save(registries));
            }
        }
        tag.put("Items", items);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.linked = tag.getBoolean("linked");
        this.linkX = tag.getInt("linkX");
        this.linkY = tag.getInt("linkY");
        this.linkZ = tag.getInt("linkZ");
        this.linkDim = tag.contains("linkDim")
                ? ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.parse(tag.getString("linkDim")))
                : null;
        this.instability = tag.getInt("instability");
        this.outputStacks.clear();
        for (final Tag entry : tag.getList("Items", Tag.TAG_COMPOUND)) {
            this.outputStacks.add(ItemStack.parseOptional(registries, (CompoundTag) entry));
        }
    }
}
