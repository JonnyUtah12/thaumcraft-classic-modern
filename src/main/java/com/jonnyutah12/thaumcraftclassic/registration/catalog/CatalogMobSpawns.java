package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.world.biome.HalloweenSpawnsBiomeModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Mob-spawn catalog holder (TC-M6-06-s-c6b): the biome spawn lists of the
 * monster roster.
 *
 * <p>The original adds them in {@code ConfigEntities.initEntitySpawns}
 * ({@code :157-187}): the Angry Zombie over every biome that carries a monster
 * spawn list ({@code :160-166}, weight 10, pack 1-1), the Pech over the
 * {@code BiomeDictionary} MAGICAL set ({@code :168-174}, 10, 1-1), the Firebat
 * over the NETHER set ({@code :176-177}, 10, 1-2) plus an all-biome entry that
 * the calendar gate opens only on the 31st of October ({@code :178-181}, 5,
 * 1-2), and the Wisp over the NETHER set ({@code :185-187}, 5, 1-1). Every
 * gate sits on a {@code Config} toggle ({@code spawnAngryZombie},
 * {@code spawnPech}, {@code spawnFireBat}, {@code spawnWisp}).</p>
 *
 * <p>1.21.1's answer is data: the shipped {@code neoforge:add_spawns} biome
 * modifiers under {@code data/thaumcraft/neoforge/biome_modifier/} carry the
 * weights and pack sizes verbatim (one file per original {@code addSpawn}
 * call), and the port's own {@code thaumcraft:halloween_spawns} modifier type
 * registered here carries the calendar gate. The four config toggles have no
 * data-side twin — the deviation is ledgered (the card's BD row), and the
 * MAGICAL set is measured, not assumed (the shipped
 * {@code #neoforge:is_magical} tag carries no biome today, so the Pech's entry
 * selects nothing, which is the original's effective state on modern Forge
 * too).</p>
 */
public final class CatalogMobSpawns {

    /**
     * The port's own biome-modifier serializer registry surface. NeoForge
     * registers its stock modifiers the same way
     * ({@code NeoForgeMod.java:191}, {@code :251} for {@code add_spawns}); the
     * modifier INSTANCES are datapack data under
     * {@code data/<ns>/neoforge/biome_modifier/}, the serializer is code.
     */
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
                    ThaumcraftClassic.MOD_ID);

    /** {@code thaumcraft:halloween_spawns} — the calendar-gated spawn entry. */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<HalloweenSpawnsBiomeModifier>> HALLOWEEN_SPAWNS =
            BIOME_MODIFIER_SERIALIZERS.register("halloween_spawns",
                    () -> HalloweenSpawnsBiomeModifier.CODEC);

    private CatalogMobSpawns() {
    }

    public static void init(final IEventBus modEventBus) {
        BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
    }
}
