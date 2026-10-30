package com.jonnyutah12.thaumcraftclassic.world.outerlands;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * The Outer Lands chunk hook (TC-M6-04f-m; the original's
 * {@code ThaumcraftWorldGenerator.worldGeneration} →
 * {@code MazeHandler.generateEldritch(world, rand, chunkX, chunkZ)} call
 * site, which in 1.7.10 ran inside the Outer Lands chunk provider).
 *
 * <p>1.21.1 adaptation: the dimension's flat generator carries no
 * structure/feature pipeline (the faithful analogue of the original
 * provider, which filled nothing), so placement rides the load event: every
 * loaded Outer Lands chunk asks {@link MazeHandler#generateEldritch} for its
 * cell. The handler's "built" set is the guard the original got for free
 * from populate-fires-once: reloads re-fire the event, rooms do not rebuild,
 * decoration rolls do not re-run.</p>
 *
 * <p>Placement writes into neighbouring chunks load them, which re-fires
 * this hook for them — depth-bounded by the built set: a chunk is built at
 * most once, so the cascade stops.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class OuterLandsChunkHooks {

    @SubscribeEvent
    public static void onServerStart(final net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        MazeHandler.installServer(event.getServer());
    }

    @SubscribeEvent
    public static void onChunkLoad(final ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!OuterLands.isOuterLands(level)) {
            return;
        }
        if (!(event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk)) {
            // mid-generation proxies: the maze writes rooms directly, no hook
            return;
        }
        final ChunkPos pos = event.getChunk().getPos();
        MazeHandler.generateEldritch(level, MazeHandler.worldRandom(level), pos.x, pos.z);
    }

    private OuterLandsChunkHooks() {
    }
}
