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
 * The client tick subscriber for the two source-FX maps of TC-M7-02-c-s:
 * {@code EssentiaHandler.sourceFX} (essentia drain trails,
 * EssentiaHandler.java:36 + ClientTickEventsFML.java:218-261) and
 * {@code TileInfusionMatrix.sourceFX} (infusion matrix draws,
 * TileInfusionMatrix.java:69 + 864-947).
 *
 * <p>Both originals are client-runtime maps keyed by a coordinate string and
 * drained once per client tick: a packet lands/refreshes an entry with a tick
 * budget, the tick loop spends motes from it, decrements, and drops it at zero.
 * The essentia side lives in {@link TcEssentiaSourceMap} — the same bookkeeping
 * with no client class in sight, so the game-test server can prove the packet
 * and budget semantics; this class keeps the {@code ClientLevel} lookups
 * (matrices, pedestals, the player encoded in the colour) and the
 * {@link TcEffects} hand-off. The infusion map stays here because its every
 * branch needs the client world's block entities.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT)
public final class TcSourceFx {

    private TcSourceFx() {
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

    /**
     * The original's {@code TileInfusionMatrix.sourceFX} (insertion-ordered:
     * the original's remove-and-put, see the class javadoc).
     */
    public static final Map<String, InfusionFx> INFUSION_SOURCE = new LinkedHashMap<>();

    /** The client tick count the trail burst count rides ({@code ClientTickEventsFML.tickCount}). */
    private static int tickCount;

    /** Clears both maps and the tick counter (gametest isolation). */
    public static void reset() {
        TcEssentiaSourceMap.reset();
        INFUSION_SOURCE.clear();
        tickCount = 0;
    }

    /** {@code PacketFXEssentiaSource.onMessage}: land/refresh one drain trail. */
    public static void onEssentiaSource(final BlockPos pos, final BlockPos offset, final int color) {
        TcEssentiaSourceMap.onEssentiaSource(pos, offset, color);
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

    /** The tick budget of one infusion draw entry, or -1 when it is gone. */
    public static int infusionTicksAt(final BlockPos source, final int color) {
        final InfusionFx fx = INFUSION_SOURCE
                .get(source.getX() + ":" + source.getY() + ":" + source.getZ() + ":" + color);
        return fx == null ? -1 : fx.ticks;
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
        drainEssentia(level);
        drainInfusion(level, setting);
    }

    /**
     * The {@code EssentiaHandler.sourceFX} drain loop
     * (ClientTickEventsFML.java:218-261): one trail mote per entry per tick,
     * source → consumer centre, consumer y −1 for a matrix consumer, the
     * tail-5-ticks shrink, and the tick decrement. The bookkeeping itself is
     * {@link TcEssentiaSourceMap#drain}.
     */
    private static void drainEssentia(final ClientLevel level) {
        TcEffects.spawn(level, TcEssentiaSourceMap.drain(
                pos -> level.getBlockEntity(pos) instanceof TileInfusionMatrix,
                tickCount, level.getRandom()));
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
}
