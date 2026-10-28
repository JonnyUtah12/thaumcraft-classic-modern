package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Essentia Resonator (content-mapping.csv {@code thaumcraft:itemresonator}).
 *
 * <p>Original: {@code thaumcraft.common.items.relics.ItemResonator} —
 * right-click an essentia-transport tile and it reads the tile aloud in chat:
 * the essentia held on the clicked face ("tc.resonator1"), the face's suction
 * type/amount ("tc.resonator2", defaulting to "tc.resonator3" = "Untyped"),
 * then plays "thaumcraft:alembicknock" at 0.5 / 1.9+rand·0.1. Non-transport
 * tiles are ignored; the client half only swung the arm.</p>
 *
 * <p>1.21.1 realization: the reading runs through the shared
 * {@link IEssentiaTransport} seam (TC-M4-06/07 providers, TC-M6-04c
 * consumers) — the original's {@code RayTracer.retraceBlock} subHit face
 * refinement is unnecessary because the modern hit result already carries the
 * clicked face. The original's second branch (tube buffers, which report no
 * per-face essentia but hold a shared pool) has no port-side tile yet:
 * TC-M4-07's buffer tiles will implement {@link IEssentiaTransport}, and if
 * their pool needs a dedicated read the branch re-lands there. Until then the
 * resonator reads exactly the tiles that exist.</p>
 */
public class ItemResonator extends Item {

    public ItemResonator(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockEntity tile = level.getBlockEntity(context.getClickedPos());
        if (!(tile instanceof IEssentiaTransport transport)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        final Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        final Direction face = context.getClickedFace();

        final Aspect held = transport.getEssentiaType(face);
        if (held != null) {
            player.displayClientMessage(
                    Component.translatable("tc.resonator1",
                            String.valueOf(transport.getEssentiaAmount(face)), held.getName()),
                    false);
        }

        final Aspect suction = transport.getSuctionType(face);
        player.displayClientMessage(
                Component.translatable("tc.resonator2",
                        String.valueOf(transport.getSuctionAmount(face)),
                        suction != null ? suction.getName() : Component.translatable("tc.resonator3")),
                false);

        level.playSound(null, context.getClickedPos(), ModSounds.ALEMBICKNOCK.get(),
                SoundSource.BLOCKS, 0.5F,
                1.9F + level.getRandom().nextFloat() * 0.1F);
        return InteractionResult.SUCCESS;
    }
}
