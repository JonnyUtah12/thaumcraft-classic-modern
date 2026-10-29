package com.jonnyutah12.thaumcraftclassic.client.fx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

/**
 * The client-side {@code EssentiaHandler.sourceFX} map
 * (EssentiaHandler.java:36, drained by ClientTickEventsFML.java:218-261), kept
 * free of any client class so its packet and tick bookkeeping is testable on
 * the game-test server ({@code TcSourceFx} is the client subscriber that feeds
 * it and spawns through {@link TcEffects}).
 *
 * <p>Semantics transcribed from the reference: {@code PacketFXEssentiaSource}
 * keys an entry by {@code consumer:source:colour} (its own
 * {@code x:y:z:tx:ty:tz:color} string, PacketFXEssentiaSource.java:56) and
 * lands/refreshes it with a 15-tick budget, moving it to the back of the
 * iteration order (the original's remove-and-put on a {@code HashMap} — a
 * {@link LinkedHashMap} makes that order explicit). Each client tick spends one
 * trail mote per entry (no {@code particleCount} gate — the original calls
 * {@code ClientProxy.essentiaTrailFx} directly), decrements the budget, and
 * drops the entry once it reaches zero.</p>
 */
public final class TcEssentiaSourceMap {

    private TcEssentiaSourceMap() {
    }

    /** One {@code EssentiaHandler.EssentiaSourceFX} row. */
    public static final class Entry {
        public final BlockPos consumer;
        public final BlockPos source;
        public final int color;
        public int ticks;

        Entry(final BlockPos consumer, final BlockPos source, final int color, final int ticks) {
            this.consumer = consumer;
            this.source = source;
            this.color = color;
            this.ticks = ticks;
        }
    }

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    /** The packet key the original builds: {@code x:y:z:tx:ty:tz:color}. */
    public static String key(final BlockPos consumer, final BlockPos source, final int color) {
        return consumer.getX() + ":" + consumer.getY() + ":" + consumer.getZ() + ":"
                + source.getX() + ":" + source.getY() + ":" + source.getZ() + ":" + color;
    }

    /** Clears the map (gametest isolation). */
    public static void reset() {
        ENTRIES.clear();
    }

    /** How many trails are live (the original's {@code sourceFX.size()}). */
    public static int entryCount() {
        return ENTRIES.size();
    }

    /** The tick budget of one entry, or -1 when it is gone. */
    public static int ticksAt(final BlockPos consumer, final BlockPos source, final int color) {
        final Entry fx = ENTRIES.get(key(consumer, source, color));
        return fx == null ? -1 : fx.ticks;
    }

    /** The keys in drain (insertion) order. */
    public static List<String> order() {
        return List.copyOf(ENTRIES.keySet());
    }

    /**
     * {@code PacketFXEssentiaSource.onMessage} (PacketFXEssentiaSource.java:52-68):
     * the consumer is the packet position, the source is {@code pos - offset}
     * (the original's {@code x - dx} arithmetic); a landing packet sets the
     * budget to 15 and re-puts the entry so it drains last.
     */
    public static void onEssentiaSource(final BlockPos consumer, final BlockPos offset, final int color) {
        final BlockPos source = consumer.subtract(offset);
        final String key = key(consumer, source, color);
        final Entry existing = ENTRIES.remove(key);
        if (existing != null) {
            existing.ticks = TcFxPlans.ESSENTIA_SOURCE_TICKS;
        }
        ENTRIES.put(key, existing != null ? existing
                : new Entry(consumer, source, color, TcFxPlans.ESSENTIA_SOURCE_TICKS));
    }

    /**
     * One client tick of the drain loop (ClientTickEventsFML.java:218-261): one
     * trail mote per live entry, the tail-of-life shrink inside
     * {@link TcFxPlans#essentiaTrailTick}, then the decrement and the re-put
     * that keeps the round-robin order.
     *
     * @param consumerIsMatrix the caller's lookup for the original's
     *                        {@code tile instanceof TileInfusionMatrix} branch,
     *                        which pulls the trail target down a block
     */
    public static List<ParticleSpawn> drain(final Predicate<BlockPos> consumerIsMatrix,
            final int tickCount, final RandomSource rand) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        for (final String key : order()) {
            final Entry fx = ENTRIES.get(key);
            if (fx == null) {
                continue;
            }
            if (fx.ticks <= 0) {
                ENTRIES.remove(key);
                continue;
            }
            plan.addAll(TcFxPlans.essentiaTrailTick(
                    fx.source.getX(), fx.source.getY(), fx.source.getZ(),
                    fx.consumer.getX(), fx.consumer.getY(), fx.consumer.getZ(),
                    fx.color, fx.ticks, tickCount, consumerIsMatrix.test(fx.consumer), rand));
            fx.ticks--;
            ENTRIES.remove(key);
            ENTRIES.put(key, fx); // the original's re-put keeps iteration order
        }
        return plan;
    }
}
