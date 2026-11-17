package com.jonnyutah12.thaumcraftclassic.world.outerlands;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * TC-M6-04f-m: the Outer Lands plane ids (the original's
 * {@code Config.dimensionOuterId = -42} and its biome/dimension registration,
 * content-mapping.csv rows {@code thaumcraft:dimension.-42} and
 * {@code thaumcraft:biome.biomegeneldritch}).
 *
 * <p>1.7.10 identified dimensions by int id ({@code -42}) through
 * {@code DimensionManager.registerProviderType/registerDimension}; 1.21.1
 * identifies them by {@link ResourceKey} and builds the provider entirely out
 * of data: the dimension type lives at
 * {@code data/thaumcraft/dimension_type/outerlands.json} (the
 * {@code WorldProviderOuter} flags: no skylight, no weather, no beds, the
 * original's 128-cell height as {@code height}/{@code logical_height}, the
 * 0.15 fog factor as {@code ambient_light}, frozen noon as
 * {@code time_of_day}), the dimension at
 * {@code data/thaumcraft/dimension/dimension.-42.json} (the int id survives
 * verbatim in the frozen content-mapping path), and the void terrain with the
 * Eldritch biome fixed over it at the dimension's {@code minecraft:flat}
 * generator with an empty layer list — the original's
 * {@code ChunkProviderOuter}, which filled every block with {@code null}
 * (pure air) and stamped every column with
 * {@code WorldChunkManagerHell(biomeEldritchLands, 0)}.</p>
 *
 * <p>The family stays data-driven, which is 1.21.1's shape for a dimension:
 * {@code RegistryDataLoader.DIMENSION_REGISTRIES} loads
 * {@code dimension_type}, {@code biome} and the {@code dimension} (level
 * stem) JSONs, and {@code MinecraftServer.createLevels} creates a
 * {@link ServerLevel} per level stem the world knows. One measurement worth
 * carrying forward: the game-test harness bakes its world over an
 * {@code EMPTY} level-stem registry
 * ({@code net/minecraft/gametest/framework/GameTestServer.create}:
 * {@code new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable())
 * .freeze()} + the flat preset), so inside {@code runGameTestServer} no
 * datapack — mod or vanilla — gets a second live dimension. The probes
 * therefore assert the Outer Lands where it is observable: the live
 * {@code dimension_type}/{@code biome} registry entries, and the shipped
 * dimension JSON decoded by {@code LevelStem.CODEC}.</p>
 */
public final class OuterLands {

    /** The dimension id, frozen by content-mapping.csv {@code new_id}. */
    public static final ResourceKey<Level> OUTER_LANDS =
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "dimension.-42"));

    /** The level stem type: the ported WorldProviderOuter, as a data JSON. */
    public static final ResourceKey<net.minecraft.world.level.dimension.DimensionType> DIMENSION_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE,
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "outerlands"));

    /** The Eldritch biome (original {@code BiomeGenEldritch}, default id 195). */
    public static final ResourceKey<Biome> BIOME_ELDRITCH =
            ResourceKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "biome.biomegeneldritch"));

    /** The y the original builds every maze room at ({@code GenPortal}/{@code GenBossRoom} pass {@code y = 50}). */
    public static final int ROOM_Y = 50;

    private OuterLands() {
    }

    public static boolean isOuterLands(final ResourceKey<Level> dimension) {
        return OUTER_LANDS.equals(dimension);
    }

    public static boolean isOuterLands(final Level level) {
        return isOuterLands(level.dimension());
    }

    /** The Outer Lands level of a server, or null while it is not loaded. */
    public static ServerLevel outerLands(final MinecraftServer server) {
        return server == null ? null : server.getLevel(OUTER_LANDS);
    }

    /**
     * The original's {@code ForgeDirection} ordinal encoding (UNKNOWN 0,
     * DOWN 1, UP 2, NORTH 3, SOUTH 4, WEST 5, EAST 6) as used by the tile
     * facing bytes in the tile NBT — the shipped tiles store it verbatim
     * (TileEldritchLock.setFacing, TileEldritchCrabSpawner.setFacing), so
     * placement code converts 1.21.1 {@link net.minecraft.core.Direction}s
     * through this mapping, exactly where the original passed
     * {@code dir.ordinal()}.
     */
    public static byte forgeOrdinal(final net.minecraft.core.Direction direction) {
        return (byte) (direction == null ? 0 : direction.get3DDataValue() + 1);
    }
}
