package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import com.jonnyutah12.thaumcraftclassic.taint.TaintRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

/**
 * Taint crust / soil / flesh block (content-mapping.csv row
 * {@code thaumcraft:blockTaint}, impl_class=BlockTaint; engine rules in
 * {@link TaintManager}, thresholds in {@link TaintRules}, design record
 * docs/architecture/taint.md).
 *
 * <p>Original {@code thaumcraft.common.blocks.BlockTaint}: one id, metadata
 * 0 crust, 1 soil, 2 flesh; 1.21.1 carries the meta verbatim
 * in a {@code variant} property (D-001). The original's properties, verbatim:
 * hardness 10.0 ({@code func_149752_b(10.0F)}), resistance 2.0
 * ({@code func_149711_c(2.0F)}), fully opaque, and the
 * per-meta {@code playerRelativeHardness} divisors 1.75 / 1.5 / 0.2 ride in
 * {@link #getDestroyProgress} (the 1.21.1 Properties bag is per-block, not
 * per-state).</p>
 *
 * <p>Behaviour ported: the random-tick spread/decay engine
 * ({@code func_149674_a}, lines 120-199 — delegated to
 * {@link TaintManager#taintBlockTick}), crust gravity through the
 * {@code canFallBelow} / {@code tryToFall} pair (original lines 231-283,
 * {@link TaintManager#canFallBelow}), the collision poison (original
 * {@code func_149724_b}: crust and soil only, player 1/100 for 80 ticks,
 * other living 1/20 for 160), the roots cue on a redstone pulse (original
 * {@code func_149696_a} event 1), and the per-variant pick-block stack
 * (original {@code getPickBlock}).</p>
 *
 * <p>Deferred, each on its owning row: the drops (crust nothing, soil dirt,
 * flesh 9 rotten flesh with silk for the block — {@code getItemDropped} /
 * {@code quantityDropped} / {@code canSilkHarvest}) are data, in
 * {@code loot_table/blocks/blocktaint.json}; the flesh's 4-light glow is the
 * original's {@code getLightValue} and is set here; the client droplet FX of
 * {@code randomDisplayTick} and the per-face icon/AO tint are the TC-M5-07
 * asset row. There is no fourth meta to carry: nothing in the decompiled tree
 * writes a {@code blockTaint} meta 3 (the aspect table keys 0 and 1, the flesh
 * recipe writes 2, the taint blob and the spread/decay paths write 0 and 1),
 * so the {@code variant} property, the name table and the creative tab all
 * stop at flesh. The {@code EntityFallingTaint}
 * spawn ({@code thaumcraft:fallingtaint}, TC-M6-06) is looked up at call time
 * by {@link TaintManager}; while it is unregistered the crust falls as a
 * vanilla gravity block instead, and the test asserts that no fall spawns the
 * TC-M6-06 entity today.</p>
 */
public class BlockTaint extends Block implements VariantDevice {

    /**
     * Original blockTaint metadata, verbatim (D-001): the original writes metas
     * 0 (crust), 1 (soil) and 2 (flesh) and nothing else — its aspect table
     * keys 0 and 1, the flesh is crafted as meta 2, and the taint blob /
     * spread / decay paths write only 0 and 1 — so the property carries
     * exactly those three, matching the three names in the variant table.
     */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

    public static final int META_CRUST = 0;
    public static final int META_SOIL = 1;
    public static final int META_FLESH = 2;

    /** Original {@code EntityFallingTaint} (content-mapping.csv {@code entity.FallingTaint}, TC-M6-06). */
    public static final ResourceLocation FALLING_TAINT_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fallingtaint");

    /** The original's {@code playerRelativeHardness} per meta (1.7.10 lines 220-230). */
    private static final float[] PLAYER_HARDNESS = {1.75F, 1.5F, 0.2F};

    public BlockTaint() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(10.0F, 2.0F)
                .sound(SoundType.ROOTS));
        registerDefaultState(stateDefinition.any().setValue(VARIANT, META_CRUST));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    public int variant(final BlockState state) {
        return state.getValue(VARIANT);
    }

    /** Original {@code func_149712_f}: the per-meta break-speed divisor. */
    @Override
    public float getDestroyProgress(final BlockState state, final Player player,
                                    final BlockGetter level, final BlockPos pos) {
        final int md = state.getValue(VARIANT);
        final float divisor = md >= 0 && md < PLAYER_HARDNESS.length ? PLAYER_HARDNESS[md] : 1.0F;
        return super.getDestroyProgress(state, player, level, pos) / divisor;
    }

    /** The tick engine (original {@code func_149674_a}, lines 120-199). */
    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos,
                              final net.minecraft.util.RandomSource rand) {
        TaintManager.taintBlockTick(level, pos, state, rand);
    }

    /**
     * Original {@code func_149724_b} (onEntityCollidedWithBlock): flesh is
     * inert, everything else poisons on its odds. The original's
     * {@code !func_70662_br()} potion-immunity guard is subsumed by
     * {@link net.minecraft.world.entity.LivingEntity#addEffect} in 1.21.1.
     */
    @Override
    protected void entityInside(final BlockState state, final Level level, final BlockPos pos,
                                final Entity entity) {
        if (state.getValue(VARIANT) == META_FLESH) {
            return;
        }
        TaintManager.stepEffect(level, entity, false);
    }

    /**
     * Original {@code func_149696_a} event 1: a redstone pulse on taint plays
     * the roots cue at 0.1 volume, 0.9 + rand*0.2 pitch. 1.21.1 delivers a
     * pulse as a neighbour notification, gated on the block actually
     * receiving signal (1.21.1 has no {@code isSignalConsumer} hook).
     */
    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                   final Block neighborBlock, final BlockPos neighborPos,
                                   final boolean movedByPiston) {
        if (level.isClientSide && level.hasSignal(pos, Direction.UP)) {
            level.playLocalSound(pos, ModSounds.ROOTS.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, TaintRules.ROOTS_VOLUME,
                    TaintRules.ROOTS_PITCH_BASE
                            + level.getRandom().nextFloat() * TaintRules.ROOTS_PITCH_SPREAD, false);
        }
    }
}
