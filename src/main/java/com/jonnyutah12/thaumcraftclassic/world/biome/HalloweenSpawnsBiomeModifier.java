package com.jonnyutah12.thaumcraftclassic.world.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Calendar;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.MobSpawnSettingsBuilder;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * The Firebat's Halloween spawn entry (TC-M6-06-s-c6b): the original adds the
 * all-biome, weight-5 Firebat spawn list entry ONLY on the 31st of October, and
 * it reads that off the system calendar at registration time
 * ({@code ConfigEntities.java:178-181}:
 * {@code calendar.get(Calendar.MONTH) + 1 == 10 && calendar.get(Calendar.DAY_OF_MONTH) == 31}).
 *
 * <p>1.21.1's stock {@code neoforge:add_spawns} biome modifier is static data,
 * and NeoForge's registered datapack conditions are only
 * {@code and/false/item_exists/mod_loaded/not/or/tag_empty/true} — there is no
 * calendar condition. So the date gate becomes a biome modifier of the port's
 * own: the entry ships as data and the modifier applies it only while the
 * calendar says Halloween, which is the same lifetime the original's gate has
 * (the check runs once per biome-modifier application, i.e. per server start /
 * datapack reload, exactly like 1.7.10's preInit pass).</p>
 */
public record HalloweenSpawnsBiomeModifier(HolderSet<Biome> biomes,
        List<MobSpawnSettings.SpawnerData> spawners) implements BiomeModifier {

    /** The shipped codec: {@code thaumcraft:halloween_spawns}. */
    public static final MapCodec<HalloweenSpawnsBiomeModifier> CODEC =
            RecordCodecBuilder.mapCodec(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(HalloweenSpawnsBiomeModifier::biomes),
                    MobSpawnSettings.SpawnerData.CODEC.listOf()
                            .fieldOf("spawns").forGetter(HalloweenSpawnsBiomeModifier::spawners))
                    .apply(builder, HalloweenSpawnsBiomeModifier::new));

    /**
     * The original's calendar test, as data: month 10, day 31 and no other day
     * ({@code ConfigEntities.java:180}).
     */
    public static boolean isHalloween(final int monthOneBased, final int dayOfMonth) {
        return monthOneBased == 10 && dayOfMonth == 31;
    }

    /** The same read the original performs, against the running calendar. */
    public static boolean isHalloweenToday() {
        final Calendar calendar = Calendar.getInstance();
        return isHalloween(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void modify(final Holder<Biome> biome, final Phase phase,
            final ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && this.biomes.contains(biome) && isHalloweenToday()) {
            final MobSpawnSettingsBuilder spawns = builder.getMobSpawnSettings();
            for (final MobSpawnSettings.SpawnerData spawner : this.spawners) {
                // The category is the entity type's own, exactly as NeoForge's stock
                // AddSpawnsBiomeModifier does it (:161-168).
                spawns.addSpawn(spawner.type.getCategory(), spawner);
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
