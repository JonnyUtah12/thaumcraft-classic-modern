package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.api.IScribeTools;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantDevice;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.menus.ResearchTableMenu;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.NotePuzzle;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * Research table (TC-M3-04; content-mapping.csv {@code thaumcraft:tileresearchtable},
 * hosted on blockTable variants 2-5).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileResearchTable}. Two slots
 * (scribe tool, research note); it accumulates environmental bonus aspects
 * every 600 ticks ({@code recalculateBonus}) that let the player pay a
 * combine/place cost from the table instead of their aspect pool, and it
 * owns the note-puzzle interaction: {@link #placeAspect} writes one hex
 * (consuming ink, pool or bonus), {@link #duplicate} copies a completed
 * note for a rising aspect cost.</p>
 *
 * <p>Deviations (docs/porting-decisions.md conventions): the original
 * flagged a completed note by setting its damage value to 64; the port's
 * single source of truth is the {@code complete} flag inside the
 * {@code thaumcraft:research_note} component (every original
 * {@code meta == 64} check reads the same bit the note data already
 * carried). The original's 1.7.10 {@code Material} classifier has no modern
 * equivalent; the environment scan classifies by block tags plus an
 * explicit circuit/piston set (see {@link MaterialLike}).</p>
 */
public class TileResearchTable extends BlockEntity implements WorldlyContainer,
        BlockDeviceBase.TickingDeviceTile, MenuProvider {

    public static final int BONUS_RECALC_TICKS = 600;
    /** Block event id: play the learn sound client-side (original blockEvent(1,1)). */
    public static final int EVENT_LEARN = 1;

    /**
     * Variants 2..5 in original ForgeDirection-ordinal order (N, S, W, E):
     * the base variant encodes the direction toward the top half, and the
     * top half carries {@code opposite + 4} (6..9). Same encoding as the
     * original's block metadata; used by the inkwell assembly and the
     * {@code BlockTable.neighborChanged} glue.
     */
    public static final Direction[] RESEARCH_DIRECTIONS =
            {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /** The base variant (2-5) whose top sits at {@code pos + direction}. */
    public static int baseVariant(final Direction dir) {
        return 2 + java.util.Arrays.asList(RESEARCH_DIRECTIONS).indexOf(dir);
    }

    /** The top variant (6-9) for a base pointing in {@code dir}. */
    public static int topVariant(final Direction dir) {
        return (baseVariant(dir) ^ 1) + 4;
    }

    /** The direction from a top variant (6-9) back toward its base. */
    public static Direction topToBaseDirection(final int topVariant) {
        return RESEARCH_DIRECTIONS[topVariant - 6];
    }

    private final ItemStack[] contents = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
    public AspectList bonusAspects = new AspectList();
    int nextRecalc = 0;
    /** Transient view of the note slot's component (the original's {@code data}). */
    @Nullable
    public ResearchNoteData data = null;

    public TileResearchTable(final BlockPos pos, final BlockState state) {
        super(M3ResearchContent.TILE_RESEARCH_TABLE.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Inventory (original IInventory; slot 0 scribe tool, slot 1 note)
    // ------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return contents[slot];
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        if (slot < 0 || slot >= contents.length || contents[slot].isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = contents[slot].split(count);
        if (removed.isEmpty()) {
            return ItemStack.EMPTY;
        }
        gatherResults();
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (slot < 0 || slot >= contents.length) {
            return ItemStack.EMPTY;
        }
        return contents[slot].split(1);
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        contents[slot] = stack == null ? ItemStack.EMPTY : stack;
        gatherResults();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (slot) {
            case 0 -> stack.getItem() instanceof IScribeTools;
            case 1 -> stack.getItem() instanceof ItemResearchNotes && !noteComplete(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(final Direction side) {
        return new int[0];
    }

    // The original exposed no hopper-facing slots on the research table.
    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
        return false;
    }

    private static void consumeOne(final ServerPlayer player, final ItemStack what) {
        final net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            final ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, what)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                return;
            }
        }
    }

    private static boolean noteComplete(final ItemStack stack) {
        final ResearchNoteData note = stack.get(ModDataComponents.RESEARCH_NOTE);
        return note != null && note.complete();
    }

    /** Original {@code TileResearchTable} stillValid (distance 64). */
    @Override
    public boolean stillValid(final Player player) {
        return level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
    }

    // ------------------------------------------------------------------
    // Note state
    // ------------------------------------------------------------------

    /** Original {@code gatherResults}: re-read the note from the slot. */
    public void gatherResults() {
        this.data = null;
        if (!contents[1].isEmpty() && contents[1].getItem() instanceof ItemResearchNotes) {
            this.data = contents[1].get(ModDataComponents.RESEARCH_NOTE);
        }
    }

    private void writeNote(final ResearchNoteData note) {
        contents[1].set(ModDataComponents.RESEARCH_NOTE, note);
        this.data = note;
        setChanged();
    }

    // ------------------------------------------------------------------
    // Ticking + environmental bonus (original canUpdate/recalculateBonus)
    // ------------------------------------------------------------------

    @Override
    public void serverTick() {
        if (nextRecalc++ > BONUS_RECALC_TICKS) {
            nextRecalc = 0;
            recalculateBonus();
            setChanged();
        }
    }

    /** Block material classes the original read via {@code Material}; no modern equivalent. */
    enum MaterialLike {
        AIR, FIRE, LAVA, GROUND, WATER, CIRCUITS, PISTON, OTHER;

        static MaterialLike of(final BlockState state) {
            if (state.isAir()) {
                return AIR;
            }
            if (state.is(BlockTags.FIRE)) {
                return FIRE;
            }
            if (state.is(Blocks.LAVA) || state.getFluidState().is(FluidTags.LAVA)) {
                return LAVA;
            }
            if (state.is(Blocks.WATER) || state.getFluidState().is(FluidTags.WATER)) {
                return WATER;
            }
            if (state.is(BlockTags.DIRT)) {
                return GROUND;
            }
            if (state.is(Blocks.REDSTONE_WIRE) || state.is(Blocks.REPEATER) || state.is(Blocks.COMPARATOR)
                    || state.is(Blocks.DAYLIGHT_DETECTOR)) {
                return CIRCUITS;
            }
            if (state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON) || state.is(Blocks.PISTON_HEAD)) {
                return PISTON;
            }
            return OTHER;
        }
    }

    /** A registered mod block by id, or null when its milestone has not registered it. */
    @Nullable
    private static Block registeredBlock(final String path) {
        final Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path));
        return block == null || block == Blocks.AIR ? null : block;
    }

    /** Variant of a registered variant-device block, or -1 when not one. */
    private static int variantOf(final BlockState state) {
        return state.getBlock() instanceof VariantDevice device ? device.variant(state) : -1;
    }

    /**
     * Original {@code recalculateBonus}: sky/height rolls plus a 17^3 scan
     * for essentia-rich surroundings (custom ores, crystals, ground, water,
     * lily pads, jars). Each qualifying scan hit merges one bonus copy of
     * the matching aspect and stops; caps are one copy per primal.
     */
    private void recalculateBonus() {
        final Aspect air = Aspect.getAspect("AIR");
        final Aspect fire = Aspect.getAspect("FIRE");
        final Aspect earth = Aspect.getAspect("EARTH");
        final Aspect water = Aspect.getAspect("WATER");
        final Aspect order = Aspect.getAspect("ORDER");
        final Aspect entropy = Aspect.getAspect("ENTROPY");
        if (air == null || fire == null || earth == null || water == null || order == null || entropy == null) {
            return;
        }

        if (!level.dimensionType().hasSkyLight()
                && level.getRawBrightness(worldPosition.above(), 0) < 4
                && !level.canSeeSky(worldPosition.above())
                && level.random.nextInt(20) == 0) {
            bonusAspects.merge(entropy, 1);
        }

        if (worldPosition.getY() > level.getMaxBuildHeight() * 0.5F && level.random.nextInt(20) == 0) {
            bonusAspects.merge(air, 1);
        }
        if (worldPosition.getY() > level.getMaxBuildHeight() * 0.66F && level.random.nextInt(20) == 0) {
            bonusAspects.merge(air, 1);
        }
        if (worldPosition.getY() > level.getMaxBuildHeight() * 0.75F && level.random.nextInt(20) == 0) {
            bonusAspects.merge(air, 1);
        }

        final Block customOre = registeredBlock("blockcustomore");
        final Block crystal = registeredBlock("blockcrystal");
        final Block jar = registeredBlock("blockjar");

        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                for (int y = -8; y <= 8; y++) {
                    final BlockPos at = worldPosition.offset(x, y, z);
                    if (at.getY() <= 0 || at.getY() >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    final BlockState state = level.getBlockState(at);
                    final MaterialLike material = MaterialLike.of(state);
                    final int oreVariant = customOre != null && state.is(customOre) ? variantOf(state) : -1;
                    final int crystalVariant = crystal != null && state.is(crystal) ? variantOf(state) : -1;
                    final int jarVariant = jar != null && state.is(jar) ? variantOf(state) : -1;

                    if (oreVariant == 1) {
                        if (bonusAspects.getAmount(air) < 1 && level.random.nextInt(20) == 0) {
                            bonusAspects.merge(air, 1);
                            return;
                        }
                    } else if (crystalVariant == 0) {
                        if (bonusAspects.getAmount(air) < 1 && level.random.nextInt(10) == 0) {
                            bonusAspects.merge(air, 1);
                            return;
                        }
                    } else if (material != MaterialLike.FIRE && material != MaterialLike.LAVA && oreVariant != 2) {
                        if (crystalVariant == 1) {
                            if (bonusAspects.getAmount(fire) < 1 && level.random.nextInt(10) == 0) {
                                bonusAspects.merge(fire, 1);
                                return;
                            }
                        } else if (material == MaterialLike.GROUND) {
                            if (bonusAspects.getAmount(earth) < 1 && level.random.nextInt(20) == 0) {
                                bonusAspects.merge(earth, 1);
                                return;
                            }
                        } else if (oreVariant == 4) {
                            if (bonusAspects.getAmount(earth) < 1 && level.random.nextInt(20) == 0) {
                                bonusAspects.merge(earth, 1);
                                return;
                            }
                        } else if (crystalVariant == 3) {
                            if (bonusAspects.getAmount(earth) < 1 && level.random.nextInt(10) == 0) {
                                bonusAspects.merge(earth, 1);
                                return;
                            }
                        } else if (material == MaterialLike.WATER) {
                            if (bonusAspects.getAmount(water) < 1 && level.random.nextInt(15) == 0) {
                                bonusAspects.merge(water, 1);
                                return;
                            }
                        } else if (oreVariant == 3) {
                            if (bonusAspects.getAmount(water) < 1 && level.random.nextInt(20) == 0) {
                                bonusAspects.merge(water, 1);
                                return;
                            }
                        } else if (crystalVariant == 2) {
                            if (bonusAspects.getAmount(water) < 1 && level.random.nextInt(10) == 0) {
                                bonusAspects.merge(water, 1);
                                return;
                            }
                        } else if (material != MaterialLike.CIRCUITS && material != MaterialLike.PISTON) {
                            if (oreVariant == 5) {
                                if (bonusAspects.getAmount(order) < 1 && level.random.nextInt(20) == 0) {
                                    bonusAspects.merge(order, 1);
                                    return;
                                }
                            } else if (crystalVariant == 4) {
                                if (bonusAspects.getAmount(order) < 1 && level.random.nextInt(10) == 0) {
                                    bonusAspects.merge(order, 1);
                                    return;
                                }
                            } else if (oreVariant == 6) {
                                if (bonusAspects.getAmount(entropy) < 1 && level.random.nextInt(20) == 0) {
                                    bonusAspects.merge(entropy, 1);
                                    return;
                                }
                            } else if (crystalVariant == 5) {
                                if (bonusAspects.getAmount(entropy) < 1 && level.random.nextInt(10) == 0) {
                                    bonusAspects.merge(entropy, 1);
                                    return;
                                }
                            }
                        } else if (bonusAspects.getAmount(order) < 1 && level.random.nextInt(20) == 0) {
                            bonusAspects.merge(order, 1);
                            return;
                        }
                    } else if (bonusAspects.getAmount(fire) < 1 && level.random.nextInt(20) == 0) {
                        bonusAspects.merge(fire, 1);
                        return;
                    }

                    if (state.is(Blocks.LILY_PAD) && level.random.nextInt(300) == 0
                            || jarVariant == 1 && level.random.nextInt(200) == 0) {
                        final List<Aspect> aspects = Aspect.values();
                        bonusAspects.merge(aspects.get(level.random.nextInt(aspects.size())), 1);
                        return;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Puzzle interaction (original placeAspect / duplicate)
    // ------------------------------------------------------------------

    /**
     * Original {@code placeAspect}: consume ink, then either place an
     * aspect (paying the player's pool, or a table bonus copy when the pool
     * is empty) or erase a player-placed hex (chance to refund the hex's
     * aspect into the pool for RESEARCHER1/2 players). Completion flips the
     * note's complete flag and fires the learn block event.
     *
     * <p>Server hardening beyond the original: the target hex must exist in
     * the grid, placement may only overwrite an empty (type 0) hex and
     * erasure only a player-placed (type 2) hex — the original relied on
     * its GUI for those invariants.</p>
     */
    public void placeAspect(final int q, final int r, @Nullable final Aspect aspect, final Player player) {
        if (data == null) {
            gatherResults();
        }
        if (!consumeInkFromStack(contents[0], false)) {
            return;
        }
        if (!contents[1].isEmpty() && contents[1].getItem() instanceof ItemResearchNotes
                && data != null && !data.complete()) {
            final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
            final boolean r1 = ResearchManager.isResearchComplete(knowledge, "RESEARCHER1");
            final boolean r2 = ResearchManager.isResearchComplete(knowledge, "RESEARCHER2");
            final Hex hex = new Hex(q, r);
            final HexEntry existing = data.entryAt(hex);
            if (existing == null || (aspect != null && existing.type() != 0)
                    || (aspect == null && existing.type() != 2)) {
                return;
            }
            final HexEntry he;
            if (aspect != null) {
                he = new HexEntry(aspect.getTag(), 2);
                if (r2 && level.random.nextFloat() < 0.1F) {
                    level.playSound(null, player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                            0.2F, 0.9F + level.random.nextFloat() * 0.2F);
                } else if (knowledge.aspectAmount(aspect.getTag()) <= 0) {
                    bonusAspects.remove(aspect, 1);
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    gatherResults();
                } else {
                    addPool(player, aspect.getTag(), -1);
                }
            } else {
                final float f = level.random.nextFloat();
                final String hexAspect = existing.aspectTag();
                if (hexAspect != null && (r1 && f < 0.25F || r2 && f < 0.5F)) {
                    level.playSound(null, player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                            0.2F, 0.9F + level.random.nextFloat() * 0.2F);
                    addPool(player, hexAspect, 1);
                }
                he = new HexEntry(null, 0);
            }

            writeNote(data.withHexEntry(hex, he));
            consumeInkFromStack(contents[0], true);
            if (!level.isClientSide) {
                final PlayerKnowledgeData discovered = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
                final ResearchNoteData completed = NotePuzzle.checkCompletion(data,
                        tag -> discovered.hasAspectDiscovered(tag));
                if (completed != null) {
                    writeNote(completed);
                    level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_LEARN, 1);
                }
            }
        }

        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        gatherResults();
    }

    private void addPool(final Player player, final String aspectTag, final int delta) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        final PlayerKnowledgeData updated = serverPlayer.getData(ModAttachments.PLAYER_KNOWLEDGE)
                .withAspectPoolAdded(aspectTag, delta);
        serverPlayer.setData(ModAttachments.PLAYER_KNOWLEDGE, updated);
        PacketDistributor.sendToPlayer(serverPlayer,
                new AspectPoolPayload(aspectTag, delta, updated.aspectAmount(aspectTag)));
    }

    /** Original {@code ResearchManager.consumeInkFromTable}. */
    static boolean consumeInkFromStack(final ItemStack stack, final boolean doit) {
        if (!stack.isEmpty() && stack.getItem() instanceof IScribeTools
                && stack.getDamageValue() < stack.getMaxDamage()) {
            if (doit) {
                stack.setDamageValue(stack.getDamageValue() + 1);
            }
            return true;
        }
        return false;
    }

    /**
     * Original {@code duplicate}: paper + ink sac plus an aspect-pool cost
     * of {@code tags + copies} per tag; on success the note's copies
     * increase and one more completed note lands in the table slot.
     */
    public void duplicate(final Player player) {
        if (data == null) {
            gatherResults();
        }
        if (contents[1].isEmpty() || !(contents[1].getItem() instanceof ItemResearchNotes)
                || data == null || !data.complete()
                || player.getInventory().countItem(Items.PAPER) <= 0
                || player.getInventory().countItem(Items.INK_SAC) <= 0) {
            return;
        }
        final ResearchEntryData entry = ResearchManager.graph().entry(data.key());
        if (entry == null || entry.tags() == null) {
            return;
        }
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        for (final ResearchEntryData.AspectTag tag : entry.tags()) {
            if (knowledge.aspectAmount(tag.aspect()) < tag.amount() + data.copies()) {
                return;
            }
        }
        for (final ResearchEntryData.AspectTag tag : entry.tags()) {
            addPool(player, tag.aspect(), -(tag.amount() + data.copies()));
        }
        consumeOne(player, new ItemStack(Items.PAPER));
        consumeOne(player, new ItemStack(Items.INK_SAC));
        level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_LEARN, 1);
        writeNote(data.withCopies(data.copies() + 1));
        contents[1].setCount(contents[1].getCount() + 1);
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        gatherResults();
    }

    // ------------------------------------------------------------------
    // Persistence + client sync
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final ListTag items = new ListTag();
        for (int i = 0; i < contents.length; i++) {
            if (!contents[i].isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                contents[i].save(registries, entry);
                items.add(entry);
            }
        }
        tag.put("Inventory", items);
        tag.putInt("nextRecalc", nextRecalc);
        final ListTag bonus = new ListTag();
        for (final Aspect aspect : bonusAspects.getAspects()) {
            if (aspect != null && bonusAspects.getAmount(aspect) > 0) {
                final CompoundTag entry = new CompoundTag();
                entry.putString("tag", aspect.getTag());
                bonus.add(entry);
            }
        }
        tag.put("bonusAspects", bonus);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Arrays.fill(contents, ItemStack.EMPTY);
        for (final Tag entry : tag.getList("Inventory", Tag.TAG_COMPOUND)) {
            final CompoundTag item = (CompoundTag) entry;
            final int slot = item.getByte("Slot") & 255;
            if (slot >= 0 && slot < contents.length) {
                contents[slot] = ItemStack.parseOptional(registries, item);
            }
        }
        nextRecalc = tag.getInt("nextRecalc");
        bonusAspects = new AspectList();
        for (final Tag entry : tag.getList("bonusAspects", Tag.TAG_COMPOUND)) {
            final Aspect aspect = Aspect.getAspect(((CompoundTag) entry).getString("tag"));
            if (aspect != null) {
                bonusAspects.merge(aspect, 1);
            }
        }
        gatherResults();
    }

    /**
     * The original synced the whole tile NBT to clients through the 1.7.10
     * update packet; the port only needs the bonus aspects (the slots travel
     * with the open menu), so the client packet carries just those.
     */
    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        final ListTag bonus = new ListTag();
        for (final Aspect aspect : bonusAspects.getAspects()) {
            if (aspect != null && bonusAspects.getAmount(aspect) > 0) {
                final CompoundTag entry = new CompoundTag();
                entry.putString("tag", aspect.getTag());
                bonus.add(entry);
            }
        }
        tag.put("bonusAspects", bonus);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (be, reg) -> getUpdateTag(reg));
    }

    @Override
    public void onDataPacket(final Connection connection, final ClientboundBlockEntityDataPacket packet,
            final HolderLookup.Provider registries) {
        if (packet != null && packet.getTag() != null) {
            bonusAspects = new AspectList();
            for (final Tag entry : packet.getTag().getList("bonusAspects", Tag.TAG_COMPOUND)) {
                final Aspect aspect = Aspect.getAspect(((CompoundTag) entry).getString("tag"));
                if (aspect != null) {
                    bonusAspects.merge(aspect, 1);
                }
            }
        }
    }

    /** Client-side block event: the learn sound (original func_145842_c). */
    @Override
    public boolean triggerEvent(final int eventId, final int param) {
        if (eventId == EVENT_LEARN) {
            if (level != null && level.isClientSide) {
                level.playSound(null, worldPosition, ModSounds.LEARN.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return true;
        }
        return super.triggerEvent(eventId, param);
    }

    // ------------------------------------------------------------------
    // Menu (original ContainerResearchTable via openGui)
    // ------------------------------------------------------------------

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new ResearchTableMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.researchtable");
    }
}
