package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.particles.ParticleTypes;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Crusted Taint / Tainted Soil / Block of Flesh (content-mapping.csv row
 * {@code thaumcraft:blocktaint}, original {@code BlockTaint}).
 *
 * <p>Original metadata 0=crust, 1=soil, 2=flesh rides a {@code variant}
 * property verbatim (D-001). Physical values are the original's: base
 * hardness 2.0 / resistance 10.0 with per-meta hardness overrides
 * (crust 1.75, soil 1.5, flesh 0.2 — {@code getBlockHardness}), the
 * {@code CustomSoundType("gore", 0.5, 0.8)} step/break set with the
 * footstep override to {@code thaumcraft:roots}.</p>
 *
 * <p>Behavior (spread, conversion, decay, gravity) is the shared engine in
 * {@link TaintManager}; drops follow the original exactly: soil → dirt,
 * flesh → 9x rotten flesh (silk-touch keeps the block, the original's
 * {@code canSilkHarvest} meta-2 branch), crust → itself.</p>
 *
 * <p>Client-only original behavior not ported here: the droplet particle
 * under crust ({@code randomDisplayTick}, TC-M7 client FX) and the biome
 * grass tint (the taint biome JSON carries the grass color instead).</p>
 */
public class BlockTaint extends Block {

    /** Original blockTaint metadata, verbatim 0-2. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

    public static final int META_CRUST = 0;
    public static final int META_SOIL = 1;
    public static final int META_FLESH = 2;

    /** Original {@code EntityFallingTaint} (TC-M6-06 owns the entity). */
    public static final ResourceLocation FALLING_TAINT_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fallingtaint");

    /** {@code CustomSoundType("gore", 0.5F, 0.8F)} + roots footstep override. */
    static final SoundType TAINT_SOUNDS = new SoundType(
            0.5F, 0.8F, ModSounds.GORE.get(), ModSounds.ROOTS.get(),
            ModSounds.GORE.get(), ModSounds.GORE.get(), ModSounds.GORE.get());

    public BlockTaint() {
        super(Properties.of()
                .mapColor(MapColor.STONE_GRAY)
                .strength(2.0F, 10.0F)
                .sound(TAINT_SOUNDS));
        registerDefaultState(stateDefinition.any().setValue(VARIANT, META_CRUST));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    public static BlockState stateFor(final int meta) {
        return BlocksRegisterHolder.BLOCK_TAINT.get().defaultBlockState().setValue(VARIANT, meta);
    }

    /** Per-meta hardness (original {@code getBlockHardness}). */
    @Override
    protected float destroySpeed(final BlockState state, @Nullable final Player player,
                                 final BlockGetter level, final BlockPos pos) {
        return switch (state.getValue(VARIANT)) {
            case META_CRUST -> 1.75F;
            case META_SOIL -> 1.5F;
            case META_FLESH -> 0.2F;
            default -> 2.0F;
        };
    }

    @Override
    public void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos,
                           final net.minecraft.util.RandomSource rand) {
        TaintManager.taintBlockTick(level, pos, state, rand);
    }

    /** Original {@code onEntityWalk}: taint poison, player 1/100, others 1/20. */
    @Override
    public void stepOn(final Level level, final BlockPos pos, final BlockState state, final Entity entity) {
        if (state.getValue(VARIANT) == META_FLESH) {
            return; // flesh does not taint (original md != 2 guard)
        }
        TaintManager.stepEffect(level, entity, false);
    }

    /** Original drops: soil → dirt item, flesh → 9x rotten flesh (silk keeps block), crust → self. */
    @Override
    public List<ItemStack> getDrops(final BlockState state, final ServerLevel level, final BlockPos pos,
                                    final BlockState toolState, @Nullable final Entity breaker) {
        final List<ItemStack> drops = new ArrayList<>();
        switch (state.getValue(VARIANT)) {
            case META_SOIL -> drops.add(new ItemStack(Items.DIRT));
            case META_FLESH -> {
                if (EnchantmentHelper.has(new ItemStack(toolState.getBlock()), Enchantments.SILK_TOUCH)) {
                    drops.add(new ItemStack(this));
                } else {
                    drops.add(new ItemStack(Items.ROTTEN_FLESH, 9));
                }
            }
            default -> drops.add(new ItemStack(this));
        }
        return drops;
    }

    /**
     * Port of {@code BlockTaint.tryToFall}: gravity for unsupported crust.
     * While TC-M6-06 has not registered {@code thaumcraft:fallingtaint} the
     * original's out-of-loaded-area fallback branch (direct block move) is
     * used, so gravity behaves from day one (docs/architecture/taint.md).
     */
    public static boolean tryToFall(final ServerLevel level, final BlockPos from, final BlockPos to) {
        if (!TaintManager.canFallBelow(level, to.below()) || to.getY() <= level.getMinBuildHeight()) {
            return false;
        }
        final var falling = BuiltInRegistries.ENTITY_TYPE.get(FALLING_TAINT_ID);
        if (falling != null) {
            final Entity entity = falling.create(level);
            if (entity != null) {
                entity.setPos(to.getCenter());
                level.addFreshEntity(entity);
                return true;
            }
        }
        BlockPos target = to;
        while (TaintManager.canFallBelow(level, target.below()) && target.getY() > level.getMinBuildHeight()) {
            target = target.below();
        }
        if (target.equals(to)) {
            return false;
        }
        final BlockState carried = level.getBlockState(from);
        level.setBlock(from, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
        level.setBlock(target, carried, UPDATE_ALL);
        return true;
    }

    /**
     * Client-side droplet FX under crust — the original's
     * {@code randomDisplayTick} used the TC droplet particle (TC-M7 client
     * FX); approximated with the vanilla drip until that lands.
     */
    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos,
                            final net.minecraft.util.RandomSource rand) {
        if (state.getValue(VARIANT) == META_CRUST && level.getBlockState(pos.below()).isAir()
                && rand.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.DRIPPING_WATER,
                    pos.getX() + 0.1 + rand.nextFloat() * 0.8, pos.getY(),
                    pos.getZ() + 0.1 + rand.nextFloat() * 0.8, 0.0, 0.0, 0.0);
        }
    }

    /** Holder indirection so {@link #stateFor} works before the holder resolves. */
    static final class BlocksRegisterHolder {
        static final net.neoforged.neoforge.registries.DeferredHolder<
                net.minecraft.world.level.block.Block, BlockTaint> BLOCK_TAINT =
                com.jonnyutah12.thaumcraftclassic.registration.catalog.M504Taint.BLOCK_TAINT;

        private BlocksRegisterHolder() {
        }
    }
}
