package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.tiles.TileInfusionMatrix;
import com.jonnyutah12.thaumcraftclassic.tiles.TilePedestal;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The client-side source-FX maps of TC-M7-02-c-s:
 * {@code EssentiaHandler.sourceFX} (essentia drain trails,
 * EssentiaHandler.java:36 + ClientTickEventsFML.java:218-261) and
 * {@code TileInfusionMatrix.sourceFX} (infusion matrix draws,
 * TileInfusionMatrix.java:69 + 864-947).
 *
 * <p>Both originals are client-runtime maps keyed by a coordinate string and
 * drained once per client tick: a packet lands/refreshes an entry with a tick
 * budget, the tick loop spends motes from it, decrements, and drops it at
 * zero. The original used {@code HashMap} + remove-and-put so iteration
 * order follows insertion — a {@link LinkedHashMap} keeps that order
 * explicitly. The per-tick geometry is the pure
 * {@link TcFxPlans#essentiaTrailTick} / {@link TcFxPlans#infusionPlayerTick}
 * / {@link TcFxPlans#infusionPedestalTick} transcription; this class only
 * owns the maps, the client lookups (entities, pedestals, matrices), and the
 * tick bookkeeping, spawning through {@link TcEffects}.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT)
public final class TcSourceFx {

    private TcSourceFx() {
    }

    /** One {@code EssentiaHandler.EssentiaSourceFX} row. */
    static final class EssentiaFx {
        final BlockPos consumer;
        final BlockPos source;
        final int color;
        int ticks;

        EssentiaFx(final BlockPos consumer, final BlockPos source, final int color, final int ticks) {
            this.consumer = consumer;
            this.source = source;
            this.color = color;
            this.ticks = ticks;
        }
    }

    /** One {@code TileInfusionMatrix.SourceFX} row (fx.loc = the source block). */
    static final class InfusionFx {
        final BlockPos matrix;
        final BlockPos source;
        final int color;
        int ticks;

        InfusionFx(final BlockPos matrix, final BlockPos source, final int color, final int ticks) {
            this.matrix = matrix;
            this.source = source;
            this.color = color;
            this.ticks = ticks;
        }
    }

    /** The original's {@code EssentiaHandler.sourceFX} (insertion-ordered, see class javadoc). */
    public static final Map<String, EssentiaFx> ESSENTIA_SOURCE = new LinkedHashMap<>();
    /** The original's {@code TileInfusionMatrix.sourceFX} (insertion-ordered, see class javadoc). */
    public static final Map<String, InfusionFx> INFUSION_SOURCE = new LinkedHashMap<>();

    /** The client tick count the trail burst count rides ({@code ClientTickEventsFML.tickCount}). */
    private static int tickCount;

    /** Clears both maps and the tick counter (gametest isolation). */
    public static void reset() {
        ESSENTIA_SOURCE.clear();
        INFUSION_SOURCE.clear();
        tickCount = 0;
    }

    /**
     * {@code PacketFXEssentiaSource.onMessage} (PacketFXEssentiaSource.java:41-64):
     * refresh the entry's budget to 15 (moving it to the back of the order)
     * or insert one. The consumer is the packet position, the source is
     * {@code pos - offset} (the original's {@code x - dx} arithmetic).
     */
    public static void onEssentiaSource(final BlockPos pos, final BlockPos offset, final int color) {
        final BlockPos source = pos.subtract(offset);
        final String key = key(pos, source, color);
        final EssentiaFx existing = ESSENTIA_SOURCE.remove(key);
        if (existing != null) {
            existing.ticks = TcFxPlans.ESSENTIA_SOURCE_TICKS;
        }
        ESSENTIA_SOURCE.put(key, existing != null ? existing
                : new EssentiaFx(pos, source, color, TcFxPlans.ESSENTIA_SOURCE_TICKS));
    }

    /**
     * {@code PacketFXInfusionSource.onMessage} (PacketFXInfusionSource.java:41-70):
     * only fires when the packet position holds an infusion matrix on the
     * client; a pedestal at the source lifts the budget from 15 to 60; the
     * key is {@code source:color} (the original's {@code tx:ty:tz:color}).
     */
    public static void onInfusionSource(final ClientLevel level, final BlockPos pos,
            final BlockPos offset, final int color) {
        if (!(level.getBlockEntity(pos) instanceof TileInfusionMatrix)) {
            return; // the original's `tile instanceof TileInfusionMatrix` gate
        }
        final BlockPos source = pos.subtract(offset);
        final String key = source.getX() + ":" + source.getY() + ":" + source.getZ() + ":" + color;
        int count = 15;
        if (level.getBlockEntity(source) instanceof TilePedestal) {
            count = 60;
        }
        final InfusionFx existing = INFUSION_SOURCE.remove(key);
        if (existing != null) {
            existing.ticks = count;
        }
        INFUSION_SOURCE.put(key, existing != null ? existing
                : new InfusionFx(pos, source, color, count));
    }

    /** {@code ClientTickEventsFML.onClientTick(Phase.START)}: the tickCount++ (line 216). */
    @SubscribeEvent
    public static void onClientTickStart(final ClientTickEvent.Pre event) {
        tickCount++;
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        final int setting = Minecraft.getInstance().options.particles().get().getId();
        drainEssentia(level, setting);
        drainInfusion(level, setting);
    }

    /**
     * The {@code EssentiaHandler.sourceFX} drain loop
     * (ClientTickEventsFML.java:218-261): one trail mote per entry per tick,
     * source → consumer centre, consumer y −1 for a matrix consumer, the
     * tail-5-ticks shrink, and the tick decrement.
     */
    private static void drainEssentia(final ClientLevel level, final int setting) {
        for (final String key : ESSENTIA_SOURCE.keySet().toArray(new String[0])) {
            final EssentiaFx fx = ESSENTIA_SOURCE.get(key);
            if (fx.ticks <= 0) {
                ESSENTIA_SOURCE.remove(key);
            } else {
                final boolean matrixConsumer =
                        level.getBlockEntity(fx.consumer) instanceof TileInfusionMatrix;
                TcEffects.spawn(level, TcFxPlans.essentiaTrailTick(
                        fx.source.getX(), fx.source.getY(), fx.source.getZ(),
                        fx.consumer.getX(), fx.consumer.getY(), fx.consumer.getZ(),
                        fx.color, fx.ticks, tickCount, matrixConsumer, level.getRandom()));
                fx.ticks--;
                ESSENTIA_SOURCE.remove(key);
                ESSENTIA_SOURCE.put(key, fx); // the original's re-put keeps iteration order
            }
        }
    }

    /**
     * The {@code TileInfusionMatrix.sourceFX} drain loop
     * (TileInfusionMatrix.java:864-947): a source at the matrix itself draws
     * from the player encoded in the colour; a pedestal source draws the
     * pedestal stack; anything else kills the entry.
     */
    private static void drainInfusion(final ClientLevel level, final int setting) {
        for (final String key : INFUSION_SOURCE.keySet().toArray(new String[0])) {
            final InfusionFx fx = INFUSION_SOURCE.get(key);
            if (fx.ticks <= 0) {
                INFUSION_SOURCE.remove(key);
                continue;
            }
            if (fx.source.equals(fx.matrix)) {
                final Entity player = level.getEntity(fx.color);
                if (player != null) {
                    TcEffects.spawn(level, TcFxPlans.infusionPlayerTick(
                            fx.matrix.getX(), fx.matrix.getY(), fx.matrix.getZ(),
                            player.getX(), player.getBoundingBox().minY, player.getZ(),
                            player.getBbWidth(), player.getBbHeight(), setting, level.getRandom()));
                }
            } else if (level.getBlockEntity(fx.source) instanceof TilePedestal pedestal) {
                final ItemStack stack = pedestal.getItem(0);
                if (!stack.isEmpty()) {
                    final Block block = Block.byItem(stack.getItem());
                    final BlockState state = block == null ? null : block.defaultBlockState();
                    TcEffects.spawn(level, TcFxPlans.infusionPedestalTick(
                            fx.source.getX(), fx.source.getY(), fx.source.getZ(),
                            fx.matrix.getX(), fx.matrix.getY(), fx.matrix.getZ(),
                            state, stack, setting, level.getRandom()));
                }
            } else {
                fx.ticks = 0; // the original's `else { fx.ticks = 0; }` (line 938)
            }
            fx.ticks--;
            INFUSION_SOURCE.remove(key);
            INFUSION_SOURCE.put(key, fx);
        }
    }

    private static String key(final BlockPos pos, final BlockPos source, final int color) {
        return pos.getX() + ":" + pos.getY() + ":" + pos.getZ() + ":"
                + source.getX() + ":" + source.getY() + ":" + source.getZ() + ":" + color;
    }
}
