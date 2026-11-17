package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The monster roster's 15 spawner eggs, measured (TC-M6-06-s-c6b / t_eae59d1a,
 * landed by t_29266e7d).
 *
 * <p>The original shipped ONE item — {@code ItemSpawnerEgg}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ItemSpawnerEgg.java:27-28}),
 * a static {@code spawnList} of {@code EntityEggStuff} filled by
 * {@code addMapping(name, primary, secondary)} at {@code ConfigEntities.java:96-124}
 * and registered at {@code ConfigItems.java:465-466}. 1.21.1 has one item per
 * mob, so each of the ledger's 15 {@code Thaumcraft:spawnegg.<path>} rows
 * (content-mapping ids {@code thaumcraft:spawnegg.<path>}, the dot kept verbatim
 * per BD-40) ships as its own {@code DeferredSpawnEggItem} in the catalog holder
 * that owns the mob.</p>
 *
 * <p>These two probes measure the pair of figures the rows carry and nothing
 * else in the suite pins: that the row's dotted id resolves to a registered
 * spawn egg that targets the row's OWN mob, and that the egg tints with the
 * original's {@code addMapping} colour pair in the original's layer order
 * ({@code ItemSpawnerEgg#func_82790_a} answers layer 0 = the first colour =
 * background, layer 1 = the second, which is the order
 * {@code SpawnEggItem#getColor(int)} answers in). The egg display names ride
 * {@code LocalizationGameTest} (its expected-key set is derived from the live
 * registry, so it covers the 15 keys by construction) and the egg models ride
 * {@code ItemModelCoverageGameTest} (same derivation, one
 * {@code models/item/<id>.json} per registered item).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public class SpawnEggGameTest {

    /** One ledger egg row: the dotted id path, the original's colour pair, the cite. */
    private record Egg(String path, int primary, int secondary, String cite) {
    }

    /**
     * All 15 rows of this family, in the original's {@code addMapping} order
     * ({@code ConfigEntities.java:96-124}); the ints are the decompiled
     * literals, hex 0xFFC0FF / 0x8000 / 0x4000 / 0xFFFFFF / 0xC00000 /
     * 0x400040 / 0xAAAAAA / 0x404040 / 0x222222 / 0x552222 / 0xFF5055 /
     * 0x000080 / 0x800000 / 0x505050 / 0xFF50FF / 0x555555 / 0x550000 /
     * 0x557755.
     */
    private static final List<Egg> EGGS = List.of(
            new Egg("brainyzombie", 16761087, 32768, "ConfigEntities.java:96 addMapping(\"BrainyZombie\", 16761087, 32768)"),
            new Egg("giantbrainyzombie", 16761087, 16384, "ConfigEntities.java:98 addMapping(\"GiantBrainyZombie\", 16761087, 16384)"),
            new Egg("wisp", 16761087, 16777215, "ConfigEntities.java:100 addMapping(\"Wisp\", 16761087, 16777215)"),
            new Egg("firebat", 16761087, 12582912, "ConfigEntities.java:102 addMapping(\"Firebat\", 16761087, 12582912)"),
            new Egg("pech", 16761087, 4194368, "ConfigEntities.java:104 addMapping(\"Pech\", 16761087, 4194368)"),
            new Egg("mindspider", 11184810, 4210752, "ConfigEntities.java:106 addMapping(\"MindSpider\", 11184810, 4210752)"),
            new Egg("eldritchguardian", 2236962, 4210752, "ConfigEntities.java:108 addMapping(\"EldritchGuardian\", 2236962, 4210752)"),
            new Egg("eldritchwarden", 5579298, 4210752, "ConfigEntities.java:110 addMapping(\"EldritchWarden\", 5579298, 4210752)"),
            new Egg("cultistknight", 16732245, 128, "ConfigEntities.java:112 addMapping(\"CultistKnight\", 16732245, 128)"),
            new Egg("cultistcleric", 16732245, 8388608, "ConfigEntities.java:114 addMapping(\"CultistCleric\", 16732245, 8388608)"),
            new Egg("cultistleader", 16732245, 5263440, "ConfigEntities.java:116 addMapping(\"CultistLeader\", 16732245, 5263440)"),
            new Egg("cultistportal", 16732245, 16732415, "ConfigEntities.java:118 addMapping(\"CultistPortal\", 16732245, 16732415)"),
            new Egg("eldritchgolem", 5592405, 4210752, "ConfigEntities.java:120 addMapping(\"EldritchGolem\", 5592405, 4210752) - EldritchGolemAiLogic.EGG_PRIMARY / EGG_SECONDARY"),
            new Egg("eldritchcrab", 5592405, 5570560, "ConfigEntities.java:122 addMapping(\"EldritchCrab\", 5592405, 5570560)"),
            new Egg("inhabitedzombie", 5601109, 5570560, "ConfigEntities.java:124 addMapping(\"InhabitedZombie\", 5601109, 5570560)"));

    private static SpawnEggItem eggOf(final String path) {
        final ResourceLocation itemId =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "spawnegg." + path);
        final Item item = BuiltInRegistries.ITEM.get(itemId);
        return item instanceof SpawnEggItem spawnEgg ? spawnEgg : null;
    }

    /**
     * The egg rows ship on the ledger's dotted ids and each egg targets the mob
     * of its own row: the row `Thaumcraft:spawnegg.<path>` (id
     * `thaumcraft:spawnegg.<path>`) pairs with the mob row `thaumcraft:<path>`.
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theFifteenEggsAreRegisteredOnTheLedgerIdsAndTargetTheirOwnMob(
            final GameTestHelper helper) {
        for (final Egg egg : EGGS) {
            final ResourceLocation itemId =
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "spawnegg." + egg.path());
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(itemId),
                    "the ledger id " + itemId + " names no registered item (" + egg.cite() + ")");
            final SpawnEggItem spawnEgg = eggOf(egg.path());
            helper.assertTrue(spawnEgg != null,
                    itemId + " is a " + BuiltInRegistries.ITEM.get(itemId).getClass().getSimpleName()
                            + ", not a spawn egg (" + egg.cite() + ")");
            final ResourceLocation mobId =
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, egg.path());
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(mobId),
                    "the egg " + itemId + " points at " + mobId + ", which is not registered");
            final EntityType<?> mob = BuiltInRegistries.ENTITY_TYPE.get(mobId);
            final EntityType<?> target = spawnEgg.getType(ItemStack.EMPTY);
            helper.assertTrue(target == mob,
                    itemId + " spawns " + BuiltInRegistries.ENTITY_TYPE.getKey(target)
                            + ", not the mob of its own row " + mobId);
            helper.assertTrue(spawnEgg.spawnsEntity(ItemStack.EMPTY, mob),
                    itemId + " does not answer spawnsEntity for " + mobId);
        }
        helper.succeed();
    }

    /**
     * Each egg carries the original's `addMapping` pair in the original's layer
     * order: layer 0 is the first colour (the background of
     * `ItemSpawnerEgg#func_82790_a`), layer 1 the second, the order
     * `SpawnEggItem#getColor(int)` answers in.
     */
    @GameTest(template = "empty", setupTicks = 2L)
    public static void theFifteenEggsCarryTheOriginalAddMappingColourPair(final GameTestHelper helper) {
        for (final Egg egg : EGGS) {
            final ResourceLocation itemId =
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "spawnegg." + egg.path());
            final SpawnEggItem spawnEgg = eggOf(egg.path());
            helper.assertTrue(spawnEgg != null, itemId + " is not a registered spawn egg");
            helper.assertTrue(spawnEgg.getColor(0) == egg.primary() && spawnEgg.getColor(1) == egg.secondary(),
                    itemId + " tints " + Integer.toHexString(spawnEgg.getColor(0)) + "/"
                            + Integer.toHexString(spawnEgg.getColor(1)) + ", not the original's "
                            + Integer.toHexString(egg.primary()) + "/" + Integer.toHexString(egg.secondary())
                            + " (" + egg.cite() + ")");
        }
        helper.succeed();
    }
}
