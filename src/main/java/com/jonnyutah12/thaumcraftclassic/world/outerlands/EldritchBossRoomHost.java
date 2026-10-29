package com.jonnyutah12.thaumcraftclassic.world.outerlands;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The seam the Ancient Locking Mechanism uses to open a boss room
 * (TC-M6-04f-s; the original's {@code TileEldritchLock.doBossSpawn}).
 *
 * <p>The lock's fuse is shipped and complete: inserting the Runed Tablet starts
 * a 100-tick countdown with the pump sound every fifth tick, and on the 101st
 * tick the lock plays its chime, rolls the per-level boss counter and announces
 * the arriving boss to every player within 300 cells — the original's four
 * {@code tc.boss.*} messages, in the original's wording.</p>
 *
 * <p>What the original does after the announcement is build one of four boss
 * rooms (golem, warden, Crimson Portal, taint) out of the maze: it reads the
 * maze map of the 5×5 chunk neighbourhood for the room's chunk and exit,
 * raises obelisks, caps, urns, crates, pedestals and stairs, sets taint biome
 * cells and fibres over a 25×25 area, and spawns the boss (four taintacles on
 * the taint room). That is a maze-map, biome-edit and Crimson-Cult-boss
 * operation — TC-M6-04f-m (maze handler) and TC-M6-06 (the cult bosses) — so
 * the room is handed to this seam. The counter, the rotation
 * ({@code bossCount % 4}) and the announcement are the lock's own behaviour and
 * ship here; a card that registers a room builder gets the rooms.</p>
 */
public final class EldritchBossRoomHost {

    /** The four boss rooms of the original's lock, in {@code bossCount % 4} order. */
    public static final String[] ROTATION = {"golem", "warden", "crimson", "taint"};

    /** Builds a boss room; the lock's cell is the reference point. */
    @FunctionalInterface
    public interface RoomBuilder {
        void build(ServerLevel level, BlockPos lockPos, int exitFeature);
    }

    private static RoomBuilder builder;

    /** The level NBT key holding the boss counter (original {@code MapBossData}). */
    public static final String BOSS_COUNT_KEY = "thaumcraft:bossCount";

    /** Installs a boss-room builder (TC-M6-04f-m / TC-M6-06). */
    public static void setBuilder(final RoomBuilder installed) {
        builder = installed;
    }

    public static boolean hasBuilder() {
        return builder != null;
    }

    /**
     * The original's per-level counter: +1, plus a 25 % chance of a second.
     * The original kept this in a named {@code MapBossData} world-save
     * fragment; 1.21.1's equivalent is the vanilla {@code SavedData} store
     * the port already uses for world magic data
     * ({@code level.getDataStorage().computeIfAbsent(FACTORY, NAME)}).
     */
    public static int advanceBossCount(final ServerLevel level) {
        final BossCountData store = store(level);
        store.value += 1 + (level.getRandom().nextFloat() < 0.25F ? 1 : 0);
        store.setDirty();
        return store.value;
    }

    /** The current counter (the game test reads the seam's bookkeeping). */
    public static int bossCount(final ServerLevel level) {
        return store(level).value;
    }

    private static BossCountData store(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BossCountData.FACTORY, BOSS_COUNT_KEY);
    }

    /** The per-level boss counter, saved with the level (original {@code MapBossData}). */
    public static final class BossCountData extends net.minecraft.world.level.saveddata.SavedData {
        public static final net.minecraft.world.level.saveddata.SavedData.Factory<BossCountData> FACTORY =
                new net.minecraft.world.level.saveddata.SavedData.Factory<>(BossCountData::new,
                        (tag, registries) -> {
                            final BossCountData data = new BossCountData();
                            data.value = tag.getInt("value");
                            return data;
                        });

        private int value;

        int get() {
            return value;
        }

        void set(final int newValue) {
            value = newValue;
            setDirty(true);
        }

        @Override
        public net.minecraft.nbt.CompoundTag save(final net.minecraft.nbt.CompoundTag tag,
                                                  final net.minecraft.core.HolderLookup.Provider registries) {
            tag.putInt("value", value);
            return tag;
        }
    }

    /** The original's rotation: {@code bossCount % 4} picks the boss room. */
    public static String bossOf(final int bossCount) {
        return ROTATION[Math.floorMod(bossCount, ROTATION.length)];
    }

    /**
     * The original's announcement: every player within 300 cells of the lock
     * gets the boss's {@code tc.boss.<name>} line.
     */
    public static void announce(final ServerLevel level, final BlockPos lockPos, final String boss) {
        final Component message = Component.translatable("tc.boss." + boss);
        for (final ServerPlayer player : level.players()) {
            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(lockPos))
                    < BOSS_ANNOUNCE_RANGE * BOSS_ANNOUNCE_RANGE) {
                player.displayClientMessage(message, false);
            }
        }
    }

    /** The original's 300-cell announcement radius. */
    public static final double BOSS_ANNOUNCE_RANGE = 300.0;

    /** The room build, handed to the card that owns the maze map and the bosses. */
    public static void buildRoom(final ServerLevel level, final BlockPos lockPos, final int exitFeature) {
        if (builder != null) {
            builder.build(level, lockPos, exitFeature);
        }
    }

    private EldritchBossRoomHost() {
    }
}
