package com.jonnyutah12.thaumcraftclassic.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.jonnyutah12.thaumcraftclassic.tools.ResearchOriginalLang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The research display strings the shipped graph references, read from the
 * file BOOK-3a generates (D-016 option 2).
 *
 * <p>BOOK-3a (<code>t_5a784296</code>) converts 707 of the graph's 711
 * {@code tc.*} keys out of the player's own extracted 1.7.10
 * {@code en_US.lang} into {@code assets/thaumcraft/lang/research_original.json}.
 * That file is a language <em>resource</em>, not a language file: 1.21.1's
 * {@code ClientLanguage.loadFrom} formats {@code lang/<locale>.json} for the
 * requested locale codes only, and NeoForge's
 * {@code net.neoforged.fml.i18n.I18nManager.loadTranslations} reads
 * {@code lang/<locale>.json} off the FML classpath, so a file named
 * {@code research_original.json} is never merged into the language by
 * itself. This class is the reader half of that consumer.</p>
 *
 * <p>Deliberately <b>plain</b>: no Minecraft, NeoForge or Mojang type, so a
 * headless server (the game-test harness, {@code runServer}) can build and
 * interrogate it, and the resolution rules are callable from a plain JUnit
 * probe. The client wiring — reading the resource through the
 * {@code ResourceManager} and merging the map into the live client language —
 * lives in {@code client/ResearchOriginalLanguage}, and the resource path is
 * taken from the generator's own constant
 * ({@link ResearchOriginalLang#GENERATED_RESOURCE_PATH}) so the reader can
 * never drift from the writer.</p>
 *
 * <p>The values are the original 1.7.10 strings <b>verbatim</b>, page markup
 * included ({@code <BR>}, the {@code §}-colour codes, the one {@code &}-code —
 * BD-22). Nothing here renders them; a renderer is BOOK-3b's scope.</p>
 */
public final class ResearchLexicon {

    /** The namespace the generated file is written into. */
    public static final String NAMESPACE = "thaumcraft";

    /** The {@code assets/<ns>/} prefix of a mod resource path. */
    public static final String ASSETS_PREFIX = "assets/" + NAMESPACE + "/";

    /**
     * The generated file's path inside the mod jar, verbatim from the
     * generator: {@code assets/thaumcraft/lang/research_original.json}.
     */
    public static final String GENERATED_ASSET_PATH =
            ResearchOriginalLang.GENERATED_RESOURCE_PATH;

    /**
     * The same path in {@link #NAMESPACE} resource-location form, i.e.
     * {@code lang/research_original.json} (a {@code ResourceLocation} path
     * drops the {@code assets/<ns>/} prefix).
     */
    public static final String GENERATED_RESOURCE_PATH =
            GENERATED_ASSET_PATH.substring(ASSETS_PREFIX.length());

    private static final ResearchLexicon EMPTY = new ResearchLexicon(Map.of());

    private final Map<String, String> values;

    private ResearchLexicon(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** The lexicon a code-only build sees: no original string is shipped (D-016). */
    public static ResearchLexicon empty() {
        return EMPTY;
    }

    /** A lexicon over an already-read map (what the pack-stack merge produces). */
    public static ResearchLexicon of(Map<String, String> values) {
        return values.isEmpty() ? EMPTY : new ResearchLexicon(values);
    }

    /**
     * Reads a flat {@code key -> string} JSON object — the shape
     * {@link ResearchOriginalLang#writeJson} writes.
     *
     * <p>Tolerant by design: a blank body, a body that is not an object, and
     * non-primitive values (arrays / objects, which the generated file never
     * carries but a pack overlay can) all resolve to "no entry" instead of
     * throwing. A language resource that fails to parse must never take the
     * client reload down with it; 1.21.1's own language loader logs and skips
     * such a file the same way.</p>
     */
    public static ResearchLexicon fromJson(String json) {
        if (json == null || json.isBlank()) {
            return EMPTY;
        }
        final Map<String, String> read = new LinkedHashMap<>();
        final JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonSyntaxException malformed) {
            return EMPTY;
        }
        if (root == null || !root.isJsonObject()) {
            return EMPTY;
        }
        for (Map.Entry<String, JsonElement> field : root.getAsJsonObject().entrySet()) {
            final JsonElement value = field.getValue();
            if (value.isJsonPrimitive()) {
                read.put(field.getKey(), value.getAsString());
            }
        }
        return new ResearchLexicon(read);
    }

    /** Reads the same shape from a stream, UTF-8, closing it. */
    public static ResearchLexicon fromStream(InputStream stream) throws IOException {
        try (InputStream in = stream;
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            final StringBuilder body = new StringBuilder();
            final char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                body.append(buffer, 0, read);
            }
            return fromJson(body.toString());
        }
    }

    /** The display string for {@code key}, or {@code null} when the file has no entry. */
    public String value(String key) {
        return values.get(key);
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    public Set<String> keys() {
        return values.keySet();
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** The whole map, unmodifiable; this is what gets merged into the language. */
    public Map<String, String> values() {
        return values;
    }

    // ------------------------------------------------------------------
    // the merge / resolution rules, as pure functions
    // ------------------------------------------------------------------

    /**
     * The language map the client renders with: {@code base} (the committed
     * {@code en_us.json} plus vanilla plus FML translations, i.e. what the
     * live language answers) overlaid with {@code generated} (the player's own
     * original research strings).
     *
     * <p>No key of {@code base} is dropped, and the generated file wins on a
     * collision. That collision set is measured empty on every build — the
     * committed lang defines 0 of the graph's 711 keys, which
     * {@code ResearchLexiconTest.committedLangKeepsZeroOfTheGraphKeys} pins —
     * so the ordering only decides the 4 keys BOOK-3b authors (those go in
     * {@code base}, and the generated file has no opinion about them).</p>
     */
    public static Map<String, String> merged(Map<String, String> base, Map<String, String> generated) {
        final Map<String, String> out = new LinkedHashMap<>(base);
        out.putAll(generated);
        return Collections.unmodifiableMap(out);
    }

    /**
     * Resolution rule, in the order the client must apply it:
     * <ol>
     *   <li>the generated original string, when the file defines the key
     *       (an overlay build: 707 of the 711);</li>
     *   <li>otherwise the live language's own answer — the port-authored
     *       {@code en_us.json} entry, which is how BOOK-3b's 4 keys land —
     *       when the language <em>has</em> the key, i.e. when {@code liveValue}
     *       is not the key itself;</li>
     *   <li>otherwise the key, unchanged. In a code-only build that is the
     *       answer for every research key, and it is the intended D-016
     *       outcome, not a defect.</li>
     * </ol>
     *
     * @param liveValue what the live language answers for {@code key}, i.e.
     *                  {@code Language.getInstance().getOrDefault(key)} — it
     *                  returns the key itself when the language has no entry.
     */
    public String display(String key, String liveValue) {
        final String original = values.get(key);
        if (original != null) {
            return original;
        }
        if (liveValue != null && !liveValue.equals(key)) {
            return liveValue;
        }
        return key;
    }

    /** {@code key -> value} view for logging/probes: never the values themselves. */
    @Override
    public String toString() {
        return "ResearchLexicon[" + values.size() + " keys]";
    }

    /** Every {@code tc.*} key the graph references, for a caller that has the JSON text. */
    public static Set<String> graphKeys(String entriesJson) {
        return ResearchOriginalLang.collectGraphKeys(entriesJson);
    }
}
