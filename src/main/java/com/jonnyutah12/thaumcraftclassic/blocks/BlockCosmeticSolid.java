package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelReader;

/**
 * TC cosmetic solid block (content-mapping.csv {@code thaumcraft:blockcosmeticsolid}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockCosmeticSolid} — one block
 * id, sixteen metadata variants, ported verbatim to a {@code variant} property
 * (D-001). Base properties are the original's: rock material, hardness 2.0,
 * resistance 10.0, stone step sound, opaque.</p>
 *
 * <p>Per-variant overrides ported from the original: hardness
 * ({@code getBlockHardness}: obsidian totem/tile/charged totem 30, thaumium
 * block + arcane stone + arcane bricks 4) and explosion resistance
 * ({@code getExplosionResistance}: 999 / 20 respectively); light value
 * (paving of travel 9, crusted stone 4); creature-spawn block (paving
 * travel/warding, ancient stone 2); the paving-of-travel speed+jump step
 * effect; the golem-fetter sky-light toggle (inactive 9 ↔ active 10); and the
 * drop remap active-fetter → inactive-fetter (original {@code quantityFromDrops}).
 * Beacon-base parity (original {@code isBeaconBase} true for every meta) is
 * data: {@code data/minecraft/tags/block/beacon_base_blocks.json}.</p>
 *
 * <p>Tile hosts (docs/catalog-triage.md shared multi-meta block map): variant
 * 3 warding stone is TC-M6-04d's {@code TileWardingStone} and variant 8
 * charged totem is a TC-M5-01 {@code TileNode} host — both supplied through
 * {@link DeviceTileFactory}; this class only provides the hook. The original's
 * meta-8 wisp-essence drop on break reads the node tile's aspects and needs
 * {@code ItemWispEssence} (TC-M6-04a) plus the aspect container API — it
 * rides with the TC-M5-01 TileNode row and is not implemented here.</p>
 *
 * <p>Client-side scope notes (TC-M7-01): the 27-icon atlas with per-meta
 * connected-texture selection ({@code getIcon}/{@code getBlockIcon}, the
 * ancient-stone random-tile and totem stacking logic), the warding-stone rune
 * particles ({@code randomDisplayTick}), the meta-8 destroy burst/sound and
 * the paving sparkle are model/render territory.</p>
 */
public class BlockCosmeticSolid extends Block implements VariantDevice, EntityBlock {

    /** Original blockCosmeticSolid metadata, verbatim 0-15. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);

    public static final int VARIANT_OBSIDIAN_TOTEM = 0;
    public static final int VARIANT_OBSIDIAN_TILE = 1;
    public static final int VARIANT_PAVING_TRAVEL = 2;
    public static final int VARIANT_PAVING_WARDING = 3;
    public static final int VARIANT_THAUMIUM_BLOCK = 4;
    public static final int VARIANT_TALLOW_BLOCK = 5;
    public static final int VARIANT_ARCANE_STONE = 6;
    public static final int VARIANT_ARCANE_STONE_BRICKS = 7;
    public static final int VARIANT_CHARGED_OBSIDIAN_TOTEM = 8;
    public static final int VARIANT_GOLEM_FETTER = 9;
    public static final int VARIANT_GOLEM_FETTER_ACTIVE = 10;
    public static final int VARIANT_ANCIENT_STONE = 11;
    public static final int VARIANT_ANCIENT_ROCK = 12;
    public static final int VARIANT_ANCIENT_STONE_2 = 13;
    public static final int VARIANT_CRUSTED_STONE = 14;
    public static final int VARIANT_ANCIENT_PEDESTAL = 15;

    public BlockCosmeticSolid() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0F, 10.0F)
                .sound(SoundType.STONE)
                .lightLevel(state -> switch (state.getValue(VARIANT)) {
                    case VARIANT_PAVING_TRAVEL -> 9;
                    case VARIANT_CRUSTED_STONE -> 4;
                    default -> 0;
                })
                .isValidSpawn((state, level, pos, type) -> switch (state.getValue(VARIANT)) {
                    case VARIANT_PAVING_TRAVEL, VARIANT_PAVING_WARDING, VARIANT_ANCIENT_STONE_2 -> false;
                    default -> true;
                }));
        registerDefaultState(stateDefinition.any().setValue(VARIANT, VARIANT_OBSIDIAN_TOTEM));
    }

    @Override
    public int variant(final BlockState state) {
        return state.getValue(VARIANT);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    /**
     * NOTE (no 1.21.x hook): the original's per-meta {@code getBlockHardness}
     * (obsidian totem/tile/charged totem 30, thaumium block + arcane stone +
     * arcane bricks 4) has no state-addressable equivalent — {@code destroyTime}
     * is per-block, so the base 2.0 applies to every meta. Same deferral as the
     * mana-pod row (TC-M6-04a); the values stay in this javadoc for the cards
     * that add per-state behaviour on top.
     */
    /** Original {@code getExplosionResistance} per-meta overrides; base 10.0 from the properties. */
    @Override
    public float getExplosionResistance(final BlockState state, final BlockGetter level, final BlockPos pos, final Explosion explosion) {
        return switch (variant(state)) {
            case VARIANT_OBSIDIAN_TOTEM, VARIANT_OBSIDIAN_TILE, VARIANT_CHARGED_OBSIDIAN_TOTEM -> 999.0F;
            case VARIANT_THAUMIUM_BLOCK, VARIANT_ARCANE_STONE, VARIANT_ARCANE_STONE_BRICKS -> 20.0F;
            default -> super.getExplosionResistance(state, level, pos, explosion);
        };
    }

    /**
     * The original {@code canCreatureSpawn} false-for-pavements rule now lives
     * in the {@code Properties.isValidSpawn} predicate on the properties (1.21.x
     * has no block-level spawn hook).
     */
    /**
     * Original {@code onEntityWalk}: walking the paving stone of travel grants
     * Speed II + Jump I for 2 s (amplifiers 1/0 verbatim; the client-side
     * sparkle is TC-M7-01).
     */
    @Override
    public void stepOn(final Level level, final BlockPos pos, final BlockState state, final Entity entity) {
        if (variant(state) == VARIANT_PAVING_TRAVEL && entity instanceof LivingEntity living) {
            if (!level.isClientSide) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1));
                living.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0));
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    /**
     * Original {@code onNeighborBlockChange}: the golem fetter activates under
     * sky light and deactivates when covered.
     */
    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                final Block neighborBlock, final BlockPos neighborPos, final boolean movedByPiston) {
        if (!level.isClientSide) {
            final int variant = variant(state);
            if (variant == VARIANT_GOLEM_FETTER && level.canSeeSky(pos)) {
                level.setBlock(pos, state.setValue(VARIANT, VARIANT_GOLEM_FETTER_ACTIVE), Block.UPDATE_ALL);
            } else if (variant == VARIANT_GOLEM_FETTER_ACTIVE && !level.canSeeSky(pos)) {
                level.setBlock(pos, state.setValue(VARIANT, VARIANT_GOLEM_FETTER), Block.UPDATE_ALL);
            }
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    /**
     * Original {@code quantityFromDrops}: breaking the active golem fetter
     * yields the inactive one.
     */
    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        final BlockState dropState = variant(state) == VARIANT_GOLEM_FETTER_ACTIVE
                ? state.setValue(VARIANT, VARIANT_GOLEM_FETTER)
                : state;
        return VariantStateItems.withVariant(super.getCloneItemStack(level, pos, state), dropState);
    }

    /**
     * Tile hosts: variant 3 warding stone (TC-M6-04d) and variant 8 node
     * host (TC-M5-01) via {@link DeviceTileFactory}.
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return DeviceTileFactory.create(state, pos);
    }
}
