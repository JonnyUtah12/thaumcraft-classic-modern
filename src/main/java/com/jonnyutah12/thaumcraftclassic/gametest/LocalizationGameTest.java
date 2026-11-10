package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.localization.LanguageJson;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M7-01-b language contract (docs/architecture/localization.md).
 *
 * <p>The player-reported symptom is a tooltip showing the literal key
 * {@code block.thaumcraft.blockcustomore}. 1.21.1 resolves an id's display
 * string from the language file {@code assets/<ns>/lang/en_us.json} keyed
 * {@code <type>.<namespace>.<path>}; TC 4.2.3.5 shipped its strings as
 * {@code .lang} files in the 1.7.10 {@code tile.<Name>.name} form, which
 * 1.21.1 never reads. With no language file in the mod, EVERY name renders as
 * its raw registry key.</p>
 *
 * <p>The harness works over the ids that are actually registered at runtime —
 * the block, item and entity-type registries filtered to the mod namespace —
 * not over an inventory, and reports the registered-vs-named counts. {@link
 * #blockcustomoreIsNotARawKey} pins the reported id itself.</p>
 *
 * <p>Needs the {@code gameTestServer} run configuration, which sets
 * {@code -Dtc.langPackDir=<abs path to the built mod's assets/thaumcraft/lang>}
 * — the packaged language files, i.e. the same artifact the game loads
 * (build/generated/resources/orig-lang -> build/resources/main).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LocalizationGameTest {

    private LocalizationGameTest() {
    }

    /** kind -> the registry whose ids 1.21.1 gives a display name to. */
    private static final Map<String, ResourceKey<? extends Registry<?>>> NAMED_KINDS = Map.of(
            "block", Registries.BLOCK,
            "item", Registries.ITEM,
            "entity", Registries.ENTITY_TYPE);

    /** The key form a 1.21.1 language file must use. */
    private static final Pattern LANGUAGE_KEY =
            Pattern.compile("(block|item|entity|fluid)\\.[a-z][a-z0-9\\-]*\\.[a-z0-9._/\\-]+");

    /** Key forms only the 1.7.10 `.lang` format used — must not survive. */
    private static final Pattern LEGACY_NAME_KEY =
            Pattern.compile("(tile|item|entity)\\.[A-Za-z0-9_.]*\\.name");

    private static final String LANG_DIR_PROPERTY = "tc.langPackDir";

    /** The built mod's English language file, or null when there is none. */
    private static Map<String, String> languageOfBuiltMod(final GameTestHelper helper) {
        final String dir = System.getProperty(LANG_DIR_PROPERTY, "");
        if (dir.isEmpty()) {
            helper.assertTrue(false,
                    "tc." + LANG_DIR_PROPERTY + " is not set — the gameTestServer run config must point at the built mod's language files");
            return null;
        }
        final Path file = Path.of(dir).resolve("en_us.json");
        if (!Files.isRegularFile(file)) {
            helper.assertTrue(false,
                    "the built mod carries no language file at " + file
                            + " — 1.21.1 renders every id as its raw registry key (the TC-M7-01-b defect)");
            return null;
        }
        try {
            return LanguageJson.read(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The language key 1.21.1 uses for a registered id. */
    private static String languageKey(final String kind, final ResourceLocation id) {
        return kind + "." + id.getNamespace() + "." + id.getPath();
    }

    private static List<String> registeredModIds() {
        final List<String> ids = new ArrayList<>();
        for (final Map.Entry<String, ResourceKey<? extends Registry<?>>> entry : NAMED_KINDS.entrySet()) {
            final Registry<?> registry = BuiltInRegistries.REGISTRY.get(entry.getValue().location());
            if (registry == null) {
                continue;
            }
            for (final ResourceLocation id : registry.keySet()) {
                if (id.getNamespace().equals(ThaumcraftClassic.MOD_ID)) {
                    ids.add(entry.getKey() + " " + id);
                }
            }
        }
        return ids;
    }

    @GameTest(template = "empty")
    public static void blockcustomoreIsNotARawKey(final GameTestHelper helper) {
        final Map<String, String> language = languageOfBuiltMod(helper);
        if (language == null) {
            return;
        }

        final ResourceLocation ore = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blockcustomore");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ore),
                "thaumcraft:blockcustomore is not registered — the id contract (TC-M2-01) must hold first");

        final String key = languageKey("block", ore);
        helper.assertTrue(language.containsKey(key),
                "no language entry for " + key + ", so the tooltip renders the raw registry key");
        final String name = language.get(key);
        helper.assertTrue(!name.isBlank() && !name.equals(key) && !name.equals(ore.toString()),
                "the display name of " + ore + " is still a raw key: " + name);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyRegisteredIdHasALanguageName(final GameTestHelper helper) {
        final Map<String, String> language = languageOfBuiltMod(helper);
        if (language == null) {
            return;
        }

        for (final Map.Entry<String, String> entry : language.entrySet()) {
            final String key = entry.getKey();
            helper.assertTrue(LANGUAGE_KEY.matcher(key).matches(),
                    "language key " + key + " is not <type>.<namespace>.<path> as 1.21.1 requires");
            helper.assertTrue(!LEGACY_NAME_KEY.matcher(key).matches(),
                    "language key " + key + " is still in the 1.7.10 .lang form");
            helper.assertTrue(!entry.getValue().isBlank(), "language key " + key + " maps to an empty name");
        }

        final List<String> missing = new ArrayList<>();
        int registered = 0;
        int named = 0;
        for (final String pair : registeredModIds()) {
            final String kind = pair.substring(0, pair.indexOf(' '));
            final ResourceLocation id = ResourceLocation.parse(pair.substring(pair.indexOf(' ') + 1));
            registered++;
            final String key = languageKey(kind, id);
            final String name = language.get(key);
            if (name == null || name.isBlank() || name.equals(key) || name.equals(id.toString())) {
                if (missing.size() < 15) {
                    missing.add(kind + " " + id + " -> " + key);
                }
                continue;
            }
            named++;
        }

        ThaumcraftClassic.LOGGER
                .info("[TC-M7-01-b] language coverage: {} registered ids in named registries, {} with a display name; "
                        + "en_us.json holds {} keys; {} of them have no original counterpart and come from src/main/lang-port-authored/en_us.json",
                        registered, named, language.size(), registered - named);
        helper.assertTrue(registered > 0, "no mod ids are registered — this harness would prove nothing");
        helper.assertTrue(missing.isEmpty(),
                registered + " ids are registered, only " + named
                        + " resolve to a display name: " + missing);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void languageCoversTheOriginalNameKeys(final GameTestHelper helper) {
        final String csv = System.getProperty("tc.contentMappingFile", "");
        helper.assertTrue(!csv.isEmpty() && Files.isRegularFile(Path.of(csv)),
                "tc.contentMappingFile is not set or missing — the gameTestServer run config must provide it");
        final List<ContentMappingCsv.Row> rows;
        try {
            rows = ContentMappingCsv.read(Path.of(csv));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        final Map<String, String> language = languageOfBuiltMod(helper);
        if (language == null) {
            return;
        }

        int expected = 0;
        final List<String> missing = new ArrayList<>();
        for (final ContentMappingCsv.Row row : rows) {
            if (!"done".equals(row.implementationStatus()) || !NAMED_KINDS.containsKey(row.kind())) {
                continue;
            }
            expected++;
            final ResourceLocation id = ResourceLocation.parse(row.newId());
            if (language.containsKey(languageKey(row.kind(), id))) {
                continue;
            }
            if (missing.size() < 15) {
                missing.add(row.kind() + " " + row.newId());
            }
        }
        ThaumcraftClassic.LOGGER
                .info("[TC-M7-01-b] contract coverage: {} done block/item/entity rows, {} have a language key",
                        expected, expected - missing.size());
        helper.assertTrue(missing.isEmpty(),
                "done ids without a language key: " + missing);
        helper.succeed();
    }
}
