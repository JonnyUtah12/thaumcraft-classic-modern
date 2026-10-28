package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.aspect.AspectKnowledge;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTags;
import com.jonnyutah12.thaumcraftclassic.network.payload.ScannedToServerPayload;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Thaumometer (content-mapping.csv {@code thaumcraft:itemthaumometer}).
 *
 * <p>Original: {@code thaumcraft.common.items.relics.ItemThaumometer} — hold
 * right-click for a 25-tick scan. Each client tick raycasts the crosshair
 * (entities first, range 10, then blocks); a valid target (one whose aspects
 * would actually reveal something new, the original
 * {@code ScanManager.isValidScanTarget} gate) becomes the pending scan; at
 * ≤5 ticks remaining the scan completes: the item stops being used and a
 * {@code PacketScannedToServer} with prefix "@" is sent. Camera-tick sounds
 * play every other tick while a valid scan is pending.</p>
 *
 * <p>1.21.1 realization: the crosshair raycast is vanilla
 * {@link Player#pick}; the scan payload is {@link ScannedToServerPayload}
 * (same scanType numbering as the original ScanResult: 1 = block/item,
 * 2 = entity, 3 = phenomena), validated client-side through
 * {@link AspectKnowledge#validScan} and re-validated server-side by
 * {@code ServerNetworkHooks.completeScan}. The original's NODE tile branch and
 * the {@code scanEventhandlers} phenomena loop have no port-side systems yet
 * (node milestone), so those branches return no scan, matching the server's
 * existing TODOs. The original's blockRunes particle effect is client FX
 * deferred to the model/particle milestone.</p>
 */
public class ItemThaumometer extends Item {

    /** Original ScanResult fields needed to complete a scan. */
    private record PendingScan(int scanType, ResourceLocation scannedId, int data,
                               int targetEntityId, String phenomena) {
    }

    /** Original stored the pending scan on the item instance; kept verbatim. */
    private PendingScan startScan;

    public ItemThaumometer(final Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity entity) {
        return 25;
    }

    /** Original useRightClick: start the 25-tick use on both sides. */
    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            startScan = doScan(level, player);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** Original onUsingTick (client, local player only). */
    @Override
    public void onUseTick(final Level level, final LivingEntity livingEntity, final ItemStack stack,
                          final int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || !player.level().isClientSide
                || !player.isLocalPlayer()) {
            return;
        }
        final PendingScan scan = doScan(level, player);
        if (scan != null && scan.equals(startScan)) {
            if (remainingUseDuration <= 5) {
                startScan = null;
                player.stopUsingItem();
                // Original: ScanManager.completeScan(p, scan, "@") then the
                // packet; the server-side completion is ServerNetworkHooks.
                PacketDistributor.sendToServer(new ScannedToServerPayload(
                        scan.scanType(), scan.scannedId(), scan.data(),
                        scan.targetEntityId(), scan.phenomena(), "@"));
            }
            if (remainingUseDuration % 2 == 0) {
                level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        ModSounds.CAMERATICKS.get(), net.minecraft.sounds.SoundSource.PLAYERS,
                        0.2F, 0.45F + level.getRandom().nextFloat() * 0.1F, false);
            }
        } else {
            startScan = null;
        }
    }

    /** Original onPlayerStoppedUsing clears the pending scan. */
    @Override
    public void releaseUsing(final ItemStack stack, final Level level, final LivingEntity livingEntity,
                             final int timeLeft) {
        startScan = null;
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.NONE;
    }

    /**
     * Original doScan: pointed entity → ScanResult(2); pointed block (not air,
     * not a node tile) → ScanResult(1) keyed by the block's pick item;
     * otherwise the phenomena handlers (no port-side system yet).
     */
    private PendingScan doScan(final Level level, final Player player) {
        final HitResult hit = player.pick(10.0F, 0.0F, true);
        if (hit instanceof EntityHitResult entityHit) {
            final Entity target = entityHit.getEntity();
            if (target != null && isScanValid(EssentiaTags.getEntityAspects(target), player)) {
                return new PendingScan(2, BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()), 0,
                        target.getId(), "");
            }
            return null;
        }
        if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            final BlockPos pos = blockHit.getBlockPos();
            final net.minecraft.world.level.block.Block block = level.getBlockState(pos).getBlock();
            if (block == net.minecraft.world.level.block.Blocks.AIR) {
                return null;
            }
            // Original keyed block scans by the pick-block item (or the
            // stacked-block fallback); the modern equivalent is the block's
            // item id — the server resolves scannedId through the item
            // registry (ServerNetworkHooks case 1).
            final ItemStack pick = block.getCloneItemStack(level.getBlockState(pos), blockHit, level, pos, player);
            final ResourceLocation scannedId = pick.isEmpty()
                    ? BuiltInRegistries.ITEM.getKey(block.asItem())
                    : BuiltInRegistries.ITEM.getKey(pick.getItem());
            final int data = 0; // meta is component-carried; the scan key uses 0
            if (isScanValid(EssentiaTags.getObjectTags(pick.isEmpty()
                    ? new ItemStack(block) : pick), player)) {
                return new PendingScan(1, scannedId, data, 0, "");
            }
            return null;
        }
        return null;
    }

    /** Original ScanManager.isValidScanTarget gate, client side. */
    private static boolean isScanValid(final AspectList aspects, final Player player) {
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        return knowledge != null && AspectKnowledge.validScan(aspects, knowledge);
    }
}
