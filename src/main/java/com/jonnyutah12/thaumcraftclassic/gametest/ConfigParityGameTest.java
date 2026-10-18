package com.jonnyutah12.thaumcraftclassic.gametest;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionAction;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.config.ConfigOption;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Config-system parity checks that need the real NeoForge runtime
 * (docs/testing.md layer 2):
 *
 * <ol>
 * <li>the live {@code ModConfigSpec} is registered and loaded, and its
 * value tree matches {@link TcConfigCatalog} exactly — same paths, same
 * comments, same value types (the docs-table-vs-catalog half of the same
 * guarantee is enforced by ConfigReferenceTableTest without Minecraft);</li>
 * <li>unknown options in the config file are tolerated: loading a file with
 * extra keys/categories never crashes the mod — FML's spec-correction
 * removes them and keeps valid values, mirroring the original Forge 1.7.10
 * behaviour of ignoring what it does not know.</li>
 * </ol>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConfigParityGameTest {

    private ConfigParityGameTest() {
    }

    @GameTest(template = "empty")
    public static void configSpecMatchesCatalog(GameTestHelper helper) {
        helper.assertTrue(TcConfig.SPEC.isLoaded(),
                "the Thaumcraft config spec was never loaded by FML — registerConfig() must run in the mod constructor");

        Map<List<String>, ModConfigSpec.ConfigValue<?>> live = new LinkedHashMap<>();
        collect(TcConfig.SPEC.getValues(), new ArrayList<>(), live);

        TreeSet<String> livePaths = new TreeSet<>();
        for (List<String> path : live.keySet()) {
            livePaths.add(String.join(".", path));
        }
        TreeSet<String> expectedPaths = new TreeSet<>();
        for (ConfigOption option : TcConfigCatalog.portedOptions()) {
            expectedPaths.add(option.path());
        }
        helper.assertTrue(livePaths.equals(expectedPaths),
                "live spec option set differs from the catalog; missing=" + diff(expectedPaths, livePaths)
                        + " extra=" + diff(livePaths, expectedPaths));

        for (ConfigOption option : TcConfigCatalog.portedOptions()) {
            ModConfigSpec.ConfigValue<?> field = live.get(List.of(option.category(), option.name()));
            if (field == null) {
                helper.fail("no spec field for " + option.path());
                return;
            }
            String expectedComment = option.comment().isEmpty() ? null : option.comment();
            String actualComment = field.getSpec().getComment();
            helper.assertTrue(java.util.Objects.equals(expectedComment, actualComment),
                    "comment mismatch for " + option.path() + ": expected <" + expectedComment
                            + "> got <" + actualComment + ">");
            Object value = field.get();
            helper.assertTrue(value != null && option.type().valueClass().isInstance(value),
                    "spec value for " + option.path() + " is not a " + option.type().docLabel());
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unknownOptionsAreTolerated(GameTestHelper helper) {
        // A config file written by an older/newer mod version, or hand-edited
        // with junk: the mod must load it, keep valid values, and drop what
        // it does not know — never crash.
        String toml = """
                [general]
                allow_mirrors = false
                totally_unknown_option = 42

                [this_category_does_not_exist]
                nope = "hello"

                [Biomes]
                taint_biome_weight = 7
                """;
        CommentedConfig parsed = new TomlParser().parse(toml);

        List<String> removed = new ArrayList<>();
        TcConfig.SPEC.correct(parsed, (action, path, incorrectValue, correctedValue) -> {
            if (action == CorrectionAction.REMOVE) {
                removed.add(String.join(".", path));
            }
        });

        helper.assertTrue(removed.contains("general.totally_unknown_option"),
                "unknown key in a known category was not removed: " + removed);
        helper.assertTrue(removed.contains("this_category_does_not_exist"),
                "unknown category was not removed: " + removed);
        helper.assertTrue(Boolean.FALSE.equals(parsed.get("general.allow_mirrors")),
                "valid override general.allow_mirrors=false must survive correction");
        helper.assertTrue(Integer.valueOf(7).equals(parsed.get("Biomes.taint_biome_weight")),
                "valid override Biomes.taint_biome_weight=7 must survive correction");
        helper.assertTrue(TcConfig.SPEC.isCorrect(parsed),
                "a corrected config must satisfy the spec");

        helper.succeed();
    }

    private static void collect(UnmodifiableConfig config, List<String> path,
            Map<List<String>, ModConfigSpec.ConfigValue<?>> out) {
        for (Map.Entry<String, Object> entry : config.valueMap().entrySet()) {
            path.add(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof ModConfigSpec.ConfigValue<?> configValue) {
                out.put(List.copyOf(path), configValue);
            } else if (value instanceof UnmodifiableConfig sub) {
                collect(sub, path, out);
            }
            path.remove(path.size() - 1);
        }
    }

    private static List<String> diff(TreeSet<String> a, TreeSet<String> b) {
        TreeSet<String> out = new TreeSet<>(a);
        out.removeAll(b);
        return List.copyOf(out);
    }
}
