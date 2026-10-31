package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.tiles.TileTubeBuffer;
import com.jonnyutah12.thaumcraftclassic.tiles.TubeTransportLogic;
import com.jonnyutah12.thaumcraftclassic.wands.WandRayTracer;
import net.minecraft.core.BlockPos;
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
 * consumers). The face it reads is the sub-hit face: the original refined the
 * clicked side with {@code RayTracer.retraceBlock}'s {@code subHit} when the
 * player aimed at one of the tube's six face boxes (TC-M4-07-f — the same
 * geometry the wand toggles with), and {@link
 * com.jonnyutah12.thaumcraftclassic.wands.WandRayTracer} reproduces that clip,
 * so aiming a tube run reads the face you pointed at and not just the face you
 * clicked. The buffer branch is the original's: a {@link
 * com.jonnyutah12.thaumcraftclassic.tiles.TileTubeBuffer} reports no per-face
 * essentia and holds a shared pool, so it is read one line per aspect in the
 * original's sorted order.</p>
 */
public class ItemResonator extends Item {

    public ItemResonator(final Properties properties) {
        super(properties);
    }

    /**
     * The original's face-selection step (TC-M4-07-f):
     * {@code RayTracer.retraceBlock(world, player, x, y, z)} and, when the
     * sub-hit is one of the six face boxes, that face replaces the clicked side
     * — a sub-hit 6 (the centre box) or a miss keeps the clicked side.
     *
     * <p>Extracted as a seam so the world probe can read the face the geometry
     * selects (1.21.1 keeps no player-side chat log to read the printed lines
     * back from, so this is the observable half of the refinement).</p>
     */
    public static Direction readFace(final Level level, final Player player, final BlockPos pos,
                                     final Direction clicked) {
        final int subHit = WandRayTracer.retraceBlock(level, player, pos);
        return subHit >= 0 && subHit < TubeTransportLogic.SUBHIT_CENTRE
                ? Direction.values()[subHit] : clicked;
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockEntity tile = level.getBlockEntity(pos);
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
        final Direction face = readFace(level, player, pos, context.getClickedFace());

        final Aspect held = transport.getEssentiaType(face);
        if (tile instanceof TileTubeBuffer buffer) {
            // The original's second branch: no per-face essentia, one line per
            // aspect of the shared pool, in getAspectsSorted() order.
            for (final Aspect aspect : buffer.aspects.getAspectsSorted()) {
                player.displayClientMessage(
                        Component.translatable("tc.resonator1",
                                String.valueOf(buffer.aspects.getAmount(aspect)), aspect.getName()),
                        false);
            }
        } else if (held != null) {
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

        level.playSound(null, pos, ModSounds.ALEMBICKNOCK.get(),
                SoundSource.BLOCKS, 0.5F,
                1.9F + level.getRandom().nextFloat() * 0.1F);
        return InteractionResult.SUCCESS;
    }
}
