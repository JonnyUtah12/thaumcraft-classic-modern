package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.world.biome.HalloweenSpawnsBiomeModifier;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers.AddSpawnsBiomeModifier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;


/**
 * The monster roster's biome spawn lists, measured (TC-M6-06-s-c6b).
 *
 * <p>The five {@code EntityRegistry.addSpawn} calls of
 * {@code ConfigEntities.initEntitySpawns} ({@code :157-187}) ship as NeoForge
 * biome modifiers under {@code data/thaumcraft/neoforge/biome_modifier/}. A
 * biome modifier is a datapack-registry element ({@code neoforge:biome_modifier},
 * registered by NeoForge at {@code NeoForgeMod.java:547}), so these probes read
 * the LOADED instances out of the server's registry access: that proves the
 * data shipped, decoded against the registered serializer, and carries the
 * original's weight / pack-size figures — the same accessor NeoForge's
 * biome-modification pass consumes.</p>
 *
 * <p>The biome sets are the original's, read on 1.21.1 terms:
 * "every biome with a monster spawn list" ({@code :161-163}) is the overworld +
 * Nether tag pair (the End's biomes carry an empty monster list, which is why
 * the 1.7.10 loop skips them); {@code Type.NETHER} ({@code :177}, {@code :186})
 * is {@code #minecraft:is_nether}; {@code Type.MAGICAL} ({@code :169}) is
 * {@code #neoforge:is_magical}, and the census of THAT set is the honest state
 * this class records: the shipped tag carries no biome, so the Pech's entry
 * selects nothing (BD row of this card), which is also the original's effective
 * state on modern Forge.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MonsterSpawnListGameTest {

    private static final String NS = ThaumcraftClassic.MOD_ID;

    /** The five shipped modifier files, one per original addSpawn call. */
    private static final String BRAINY = "tc_brainy_zombie_spawns";
    private static final String PECH = "tc_pech_spawns";
    private static final String FIREBAT_NETHER = "tc_firebat_nether_spawns";
    private static final String FIREBAT_HALLOWEEN = "tc_firebat_halloween_spawns";
    private static final String WISP = "tc_wisp_spawns";

    private static ResourceKey<Biome> biomeKey(final String path) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(path));
    }

    private static BiomeModifier modifier(final GameTestHelper helper, final Registry<BiomeModifier> mods,
            final String path) {
        final BiomeModifier modifier = mods.get(ResourceLocation.fromNamespaceAndPath(NS, path));
        helper.assertTrue(modifier != null,
                "the shipped biome modifier thaumcraft:" + path + " is not in the loaded "
                        + "neoforge:biome_modifier registry — the data is absent or did not decode");
        return modifier;
    }

    /** One shipped add_spawns entry: mob id, weight, min, max, figure citation. */
    private static void assertEntry(final GameTestHelper helper, final MobSpawnSettings.SpawnerData entry,
            final String mobId, final int weight, final int min, final int max, final String cite) {
        final ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entry.type);
        helper.assertTrue(type.equals(ResourceLocation.fromNamespaceAndPath(NS, mobId)),
                cite + " names " + type + ", not thaumcraft:" + mobId);
        final int shippedWeight = entry.getWeight().asInt();
        helper.assertTrue(shippedWeight == weight && entry.minCount == min && entry.maxCount == max,
                cite + " is weight " + weight + " pack " + min + "-" + max + " but the shipped entry is weight "
                        + shippedWeight + " pack " + entry.minCount + "-" + entry.maxCount);
    }

    private static AddSpawnsBiomeModifier asAddSpawns(final GameTestHelper helper, final BiomeModifier modifier,
            final String path) {
        helper.assertTrue(modifier instanceof AddSpawnsBiomeModifier,
                "thaumcraft:" + path + " loaded as " + modifier.codec() + ", not neoforge:add_spawns");
        final AddSpawnsBiomeModifier add = (AddSpawnsBiomeModifier) modifier;
        helper.assertTrue(add.spawners().size() == 1,
                "thaumcraft:" + path + " carries " + add.spawners().size() + " entries; the original adds exactly one");
        return add;
    }

    private static boolean carries(final GameTestHelper helper, final Registry<Biome> biomes,
            final net.minecraft.core.HolderSet<Biome> set, final String biomePath) {
        return set.contains(biomes.getHolderOrThrow(biomeKey(biomePath)));
    }

    /**
     * THE FOUR STOCK ENTRIES, MEASURED OFF THE LOADED DATA. Every figure is the
     * original's literal from the decompile, quoted in the assert message: the
     * Angry Zombie pair 10 / 1 / 1 ({@code :163}), the Pech pair 10 / 1 / 1
     * ({@code :171}), the Firebat Nether triple 10 / 1 / 2 ({@code :177}) and
     * the Wisp triple 5 / 1 / 1 ({@code :186}) — the Wisp's NETHER set is the
     * original's own choice, quoted verbatim (a port that "fixed" it to the
     * overworld would be the deviation).
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theShippedSpawnEntriesCarryTheOriginalsFigures(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final Registry<BiomeModifier> mods =
                level.registryAccess().registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        final Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);

        final AddSpawnsBiomeModifier brainy =
                asAddSpawns(helper, modifier(helper, mods, BRAINY), BRAINY);
        assertEntry(helper, brainy.spawners().get(0), "brainyzombie", 10, 1, 1,
                "ConfigEntities.java:163 (the Angry Zombie, weight 10, pack 1-1)");
        // :161-163 — every biome with a monster spawn list: overworld and Nether,
        // the End excluded (its monster list is empty, which is what the loop tests).
        helper.assertTrue(carries(helper, biomes, brainy.biomes(), "plains")
                        && carries(helper, biomes, brainy.biomes(), "deep_dark")
                        && carries(helper, biomes, brainy.biomes(), "crimson_forest")
                        && !carries(helper, biomes, brainy.biomes(), "end_highlands"),
                "the Angry Zombie's biome set is not the original's monster-list-bearing set "
                        + "(overworld + Nether in, the End out): " + brainy.biomes().size() + " biomes");

        final AddSpawnsBiomeModifier firebat =
                asAddSpawns(helper, modifier(helper, mods, FIREBAT_NETHER), FIREBAT_NETHER);
        assertEntry(helper, firebat.spawners().get(0), "firebat", 10, 1, 2,
                "ConfigEntities.java:177 (the Firebat over Type.NETHER, weight 10, pack 1-2)");
        helper.assertTrue(carries(helper, biomes, firebat.biomes(), "crimson_forest")
                        && carries(helper, biomes, firebat.biomes(), "nether_wastes")
                        && !carries(helper, biomes, firebat.biomes(), "plains"),
                "the Firebat's Nether entry is not Nether-only: " + firebat.biomes().size() + " biomes");

        final AddSpawnsBiomeModifier wisp = asAddSpawns(helper, modifier(helper, mods, WISP), WISP);
        assertEntry(helper, wisp.spawners().get(0), "wisp", 5, 1, 1,
                "ConfigEntities.java:186 (the Wisp over Type.NETHER, weight 5, pack 1-1)");
        helper.assertTrue(carries(helper, biomes, wisp.biomes(), "nether_wastes")
                        && !carries(helper, biomes, wisp.biomes(), "plains"),
                "the Wisp's set is not the original's Type.NETHER set: " + wisp.biomes().size() + " biomes");

        final AddSpawnsBiomeModifier pech = asAddSpawns(helper, modifier(helper, mods, PECH), PECH);
        assertEntry(helper, pech.spawners().get(0), "pech", 10, 1, 1,
                "ConfigEntities.java:171 (the Pech over Type.MAGICAL, weight 10, pack 1-1)");
        // The honest state of Type.MAGICAL on 1.21.1, measured (the c3-a card
        // measured the flag, this card measures the entry): the shipped
        // #neoforge:is_magical set is empty, so the entry selects nothing.
        final int magical = biomes.getOrCreateTag(Tags.Biomes.IS_MAGICAL).size();
        helper.assertTrue(magical == pech.biomes().size(),
                "the pech entry selects " + pech.biomes().size() + " biomes while the shipped "
                        + "#neoforge:is_magical tag carries " + magical + " — the entry and the tag must agree");
        if (magical == 0) {
            helper.assertTrue(pech.biomes().size() == 0,
                    "the pech entry selected biomes while #neoforge:is_magical is empty: "
                            + pech.biomes().size());
        }
        ThaumcraftClassic.LOGGER.info("[c6b] shipped monster spawn entries measured: brainyzombie 10/1-1 over {}"
                + " biomes, firebat 10/1-2 over {}, wisp 5/1-1 over {}, pech 10/1-1 over {} (#neoforge:is_magical"
                + " carries {} biomes)", brainy.biomes().size(), firebat.biomes().size(), wisp.biomes().size(),
                pech.biomes().size(), magical);
        helper.succeed();
    }

    /**
     * THE CALENDAR GATE, MEASURED. {@code ConfigEntities.java:178-181} adds the
     * all-biome weight-5 Firebat entry only when the system calendar says
     * October the 31st. 1.21.1 has no date condition in its registered
     * datapack-condition set, so the gate is the port's own modifier type —
     * this probe pins the rule as data (the arms), the loaded instance's
     * figures, and the live state (the entry applies exactly when the calendar
     * says Halloween, which is what the shipped game is running today).
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theHalloweenFirebatEntryIsCalendarGatedAndNetherPlusOverworld(
            final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final Registry<BiomeModifier> mods =
                level.registryAccess().registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        final Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);

        // The rule, as the original states it (:180).
        helper.assertTrue(HalloweenSpawnsBiomeModifier.isHalloween(10, 31),
                ":180 opens the entry on October 31 — the shipped rule refuses it");
        final List<String> closed = new ArrayList<>();
        for (final int[] day : new int[][] {{10, 30}, {11, 1}, {9, 31}, {1, 1}, {10, 1}, {12, 31}}) {
            if (HalloweenSpawnsBiomeModifier.isHalloween(day[0], day[1])) {
                closed.add(day[0] + "-" + day[1]);
            }
        }
        helper.assertTrue(closed.isEmpty(),
                "the shipped gate opens the entry outside October 31: " + closed);

        final BiomeModifier loaded = modifier(helper, mods, FIREBAT_HALLOWEEN);
        helper.assertTrue(loaded instanceof HalloweenSpawnsBiomeModifier,
                "thaumcraft:" + FIREBAT_HALLOWEEN + " loaded as " + loaded.codec()
                        + ", not the port's thaumcraft:halloween_spawns");
        final HalloweenSpawnsBiomeModifier halloween = (HalloweenSpawnsBiomeModifier) loaded;
        helper.assertTrue(halloween.spawners().size() == 1,
                ":181 adds exactly one entry; the shipped modifier carries "
                        + halloween.spawners().size());
        assertEntry(helper, halloween.spawners().get(0), "firebat", 5, 1, 2,
                "ConfigEntities.java:181 (the Halloween Firebat, weight 5, pack 1-2, all biomes)");
        helper.assertTrue(carries(helper, biomes, halloween.biomes(), "plains")
                        && carries(helper, biomes, halloween.biomes(), "crimson_forest")
                        && !carries(helper, biomes, halloween.biomes(), "end_highlands"),
                "the Halloween entry's set is not the original's `biomes` list "
                        + "(every biome, the End's empty monster list aside): " + halloween.biomes().size());

        // The live state: the entry is applied exactly when the calendar says
        // Halloween, and the probe records which side of that edge the run is on.
        final boolean today = HalloweenSpawnsBiomeModifier.isHalloweenToday();
        helper.assertTrue(today == MonthDay.now().equals(MonthDay.of(10, 31)),
                "the shipped gate disagrees with the running calendar: isHalloweenToday=" + today
                        + " while today is " + MonthDay.now());
        ThaumcraftClassic.LOGGER.info("[c6b] halloween firebat entry: gate opens only on 10-31; this run is on {}"
                + " (applied={})", MonthDay.now(), today);
        helper.succeed();
    }

    /**
     * THE MOB SIDE OF THE SPAWN DATA, MEASURED. A natural-spawn entry is a
     * {@link MobSpawnSettings.SpawnerData} over the mob's OWN category, and the
     * original's five calls all sit in {@code EnumCreatureType.monster}
     * ({@code :163}, {@code :171}, {@code :177}, {@code :181}, {@code :186}).
     * The spawn data lands in the MONSTER bucket only when the entity type's
     * category is MONSTER, so a category drift would move the entry into a
     * bucket the game never draws — this probe pins the five types' categories
     * against the shipped entries' buckets.
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theFiveEntriesRideTheMonsterCategoryOfTheirOwnType(
            final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final Registry<BiomeModifier> mods =
                level.registryAccess().registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        final List<String> drift = new ArrayList<>();
        for (final String file : List.of(BRAINY, PECH, FIREBAT_NETHER, FIREBAT_HALLOWEEN, WISP)) {
            final BiomeModifier modifier = modifier(helper, mods, file);
            final List<MobSpawnSettings.SpawnerData> entries = modifier instanceof AddSpawnsBiomeModifier add
                    ? add.spawners()
                    : ((HalloweenSpawnsBiomeModifier) modifier).spawners();
            for (final MobSpawnSettings.SpawnerData entry : entries) {
                final EntityType<?> type = entry.type;
                if (type.getCategory() != net.minecraft.world.entity.MobCategory.MONSTER) {
                    drift.add(BuiltInRegistries.ENTITY_TYPE.getKey(type) + " -> " + type.getCategory());
                }
            }
        }
        helper.assertTrue(drift.isEmpty(),
                "a shipped monster-roster spawn entry rides a non-MONSTER category, which moves it out of"
                        + " the bucket the original draws (all five calls pass EnumCreatureType.monster,"
                        + " :163/:171/:177/:181/:186): " + drift);
        helper.succeed();
    }

    private MonsterSpawnListGameTest() {
    }
}
