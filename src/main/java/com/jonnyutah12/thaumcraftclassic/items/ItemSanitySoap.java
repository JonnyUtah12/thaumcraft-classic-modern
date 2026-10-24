package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerWarpData;
import com.jonnyutah12.thaumcraftclassic.registration.ModMobEffects;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Sanitizing soap (TC-M6-05).
 *
 * <p>Original: {@code thaumcraft/common/items/ItemSanitySoap.java}
 * (Thaumcraft:ItemSanitySoap). A 10-second (200-tick) block-posing use:
 * during the final 5 ticks it extinguishes fire, and completing the full use
 * consumes one item and removes warp — one sticky warp with probability
 * {@code 0.33 + 0.25 (warp ward) + 0.25 (standing in purifying fluid)}, and
 * all temporary warp unconditionally. The client-side roots sound and
 * crucible-bubble particles are the client milestone (TC-M7-02).</p>
 *
 * <p>The purifying fluid block ({@code blockFluidPure}) is TC-M5-01
 * content; the bonus is registry-guarded and inert until it lands, mirroring
 * the original's unconditional reference to an always-present block.</p>
 */
public class ItemSanitySoap extends Item {

    /** Original {@code getUseDuration}: 200 ticks. */
    public static final int USE_DURATION_TICKS = 200;

    /** Original completion gate: {@code getUseDuration - timeLeft > 195}. */
    static final int COMPLETION_TICKS = 195;

    /** content-mapping.csv new_id of Thaumcraft:blockFluidPure. */
    static final ResourceLocation PURE_FLUID_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blockfluidpure");

    public ItemSanitySoap() {
        super(new Item.Properties());
    }

    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity entity) {
        return USE_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        // Original EnumAction.block.
        return UseAnim.BLOCK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player,
                                                 final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(final Level level, final LivingEntity livingEntity,
                          final ItemStack stack, final int remainingUseDuration) {
        if (USE_DURATION_TICKS - remainingUseDuration > COMPLETION_TICKS) {
            livingEntity.clearFire();
        }
    }

    @Override
    public void releaseUsing(final ItemStack stack, final Level level,
                             final LivingEntity livingEntity, final int timeLeft) {
        if (USE_DURATION_TICKS - timeLeft > COMPLETION_TICKS) {
            stack.shrink(1);
            if (!level.isClientSide() && livingEntity instanceof ServerPlayer player) {
                applyWarpCleansing(player, level.random);
            }
        }
    }

    /**
     * The original's {@code onPlayerStoppedUsing} server branch. The random
     * source is a parameter so the gametest can pin the sticky-warp roll.
     */
    public static void applyWarpCleansing(final ServerPlayer player, final RandomSource random) {
        final Level level = player.level();
        float chance = 0.33F;
        if (player.hasEffect(ModMobEffects.POTION_WARWARD)) {
            chance += 0.25F;
        }
        final Block pureFluid = BuiltInRegistries.BLOCK.get(PURE_FLUID_ID);
        final BlockPos pos = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        if (pureFluid != Blocks.AIR && level.getBlockState(pos).is(pureFluid)) {
            chance += 0.25F;
        }
        final PlayerWarpData warp = player.getData(ModAttachments.PLAYER_WARP);
        if (random.nextFloat() < chance && warp.warpSticky() > 0) {
            ResearchManager.applyWarp(player, -1, SyncWarpPayload.WARP_STICKY);
        }
        if (warp.warpTemp() > 0) {
            ResearchManager.applyWarp(player, -warp.warpTemp(), SyncWarpPayload.WARP_TEMPORARY);
        }
    }
}
