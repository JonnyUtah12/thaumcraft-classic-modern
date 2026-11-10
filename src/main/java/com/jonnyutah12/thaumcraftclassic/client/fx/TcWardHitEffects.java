package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticOpaque;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

/**
 * The ward hexagram's sender (TC-M7-02-c-s-f4) — the 1.21.1 shape of the
 * original's {@code Block.addHitEffects} override.
 *
 * <p>In 1.7.10 the ward flash hangs off a world-side block method:
 * {@code BlockWarded.addHitEffects(World, MovingObjectPosition, EffectRenderer)}
 * (BlockWarded.java:64-70) and {@code BlockCosmeticOpaque.addHitEffects}
 * (BlockCosmeticOpaque.java:68-82) read the hit face and the
 * {@code (hitVec - pos)} offsets off the moving-object position and call
 * {@code ClientProxy.blockWard(world, x, y, z, side, f, f1, f2)}
 * (ClientProxy.java:980-984). Returning true means "handled": vanilla's
 * digging particles are suppressed. Warded glass (metadata 2) is the only
 * variant that fires — the amber metas fall through to vanilla, the
 * {@code else return false} of BlockCosmeticOpaque.java:79-81.</p>
 *
 * <p>1.21.1 moved that hook to the client, with the same three arguments in the
 * same order: {@link IClientBlockExtensions#addHitEffects(BlockState, Level, HitResult, ParticleEngine)}
 * (the {@code MovingObjectPosition} is the {@code HitResult}, the
 * {@code EffectRenderer} is the {@code ParticleEngine}) and it is installed per
 * block by {@link RegisterClientExtensionsEvent}. Warded glass is
 * {@code thaumcraft:blockcosmeticopaque} variant 2
 * ({@link BlockCosmeticOpaque#VARIANT_WARDED_GLASS}), the one landed block with
 * ward behaviour, so the extension registers against it.</p>
 *
 * <p>{@code BlockWarded} is not landed — its card owns it. When it lands, its
 * hit effect is this same extension (the two ward blocks share the rule in the
 * original, both firing unconditionally), and adding the block to the
 * registration below is the whole change.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TcWardHitEffects {

    /**
     * The ward flash on a hit: fires for warded glass, suppresses vanilla's
     * digging particles, and hands {@code (pos, hitVec - pos, side)} — the
     * parameters of {@code ClientProxy.blockWard}, verbatim — to
     * {@link TcEffects#wardBlock}.
     *
     * @return true for warded glass (the original's {@code return true}), false
     *         for the amber variants (the original's {@code else return false},
     *         which lets vanilla's particles through)
     */
    public static boolean addHitEffects(final BlockState state, final Level level,
            final HitResult target, final ParticleEngine manager) {
        if (!(target instanceof BlockHitResult blockHit)
                || blockHit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        if (state.getValue(BlockCosmeticOpaque.VARIANT) != BlockCosmeticOpaque.VARIANT_WARDED_GLASS) {
            return false;
        }
        ward(level, blockHit);
        return true;
    }

    /** {@code ClientProxy.blockWard(world, pos, side, hitVec - pos)} (ClientProxy.java:980-984). */
    public static void ward(final Level level, final BlockHitResult target) {
        final BlockPos pos = target.getBlockPos();
        final Vec3 hit = target.getLocation();
        TcEffects.wardBlock(level, pos, target.getDirection(),
                hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ(),
                RandomSource.create());
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(final RegisterClientExtensionsEvent event) {
        event.registerBlock(TcWardHitEffects::addHitEffects,
                CatalogDecorativeBlocks.BLOCK_COSMETIC_OPAQUE.get());
    }

    private TcWardHitEffects() {
    }
}
