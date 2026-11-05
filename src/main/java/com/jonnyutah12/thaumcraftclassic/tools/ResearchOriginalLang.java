package com.jonnyutah12.thaumcraftclassic.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Build-time converter for D-016 option 2: the original Thaumcraft research
 * display strings are <em>generated</em> at build time from the player's own
 * extracted assets, and never committed.
 *
 * <p>This class is deliberately plain: no Gradle, no Minecraft, no Gson — it
 * runs as a {@code JavaExec} build step and every step of it is callable from a
 * plain JUnit probe. Two inputs, one output:</p>
 *
 * <ul>
 *   <li>input: a 1.7.10 {@code .lang} file ({@code ../reference/assets/thaumcraft/lang/en_US.lang}),
 *   CRLF, {@code #} comments, {@code key=value} with the value running to end of
 *   line, Properties-style escapes ({@code \n}, {@code \r}, {@code \t}, {@code \\}, {@code \}{@code uXXXX});</li>
 *   <li>input: the shipped research graph
 *   ({@code data/thaumcraft/research_graph/entries.json}), whose rows reference
 *   the display keys: each row's {@code nameKey} and {@code textKey}, plus each
 *   page's {@code textKey};</li>
 *   <li>output: a flat {@code key -> value} JSON object carrying only the keys
 *   the graph references, sorted, 2-space indented, UTF-8, no BOM.</li>
 * </ul>
 *
 * <p>The graph side is derived, never hardcoded, and derived <b>structurally</b>:
 * the walk reads the same graph <em>fields</em> the runtime records expose
 * ({@code ResearchEntryData.nameKey/textKey/pages}, {@code
 * ResearchPageData.textKey}), so the build-time key set is the set the running
 * game references. With the 4.2.3.5 graph that is 712 keys (201 name + 201 text
 * + 310 page); 708 of them exist in the original lang and the 4 the original
 * never defined are authored by BOOK-3b.</p>
 *
 * <p>The derivation used to be a prefix walk — "every {@code tc.} string in the
 * document" — which measured 711. It missed {@code VAMPBAT} page 0's
 * {@code textKey = "focus.upgrade.vampirebats.text"}: a key the original lang
 * defines, outside the {@code tc.} namespace, so that page reached an overlay
 * build as a raw key (t_6799fd3f). A prefix filter is also over-inclusive in
 * principle — any {@code tc.}-prefixed string anywhere in the graph would be
 * collected as if it were a display key.</p>
 *
 * <p>The 1.7.10 page markup is carried through <b>verbatim</b> — {@code <BR>},
 * the {@code §}-colour codes and the one {@code &}-code — because this step is a
 * conversion, not a renderer.</p>
 *
 * <p>CLI:</p>
 * <pre>
 * generate &lt;langFile&gt; &lt;entriesJson&gt; &lt;outJson&gt;
 * check    &lt;langFile&gt; &lt;entriesJson&gt; &lt;committedLangJson&gt; &lt;jarPath&gt;
 * </pre>
 */
public final class ResearchOriginalLang {

    /**
     * The graph document's array of research rows. Mirrors the field
     * {@code ResearchGraphLoader.EntryFile.entries} binds with Gson; a rename on
     * one side has to show up on the other, which the unit probes' cross-check
     * against Gson catches.
     */
    static final String FIELD_ENTRIES = "entries";
    /** {@code ResearchEntryData.nameKey} — the entry's title key. */
    static final String FIELD_NAME_KEY = "nameKey";
    /** {@code ResearchEntryData.textKey} / {@code ResearchPageData.textKey}. */
    static final String FIELD_TEXT_KEY = "textKey";
    /** {@code ResearchEntryData.pages} — the entry's page list. */
    static final String FIELD_PAGES = "pages";

    /** Where the generated file lives inside the mod jar (and under a fixture root). */
    public static final String GENERATED_RESOURCE_PATH =
            "assets/thaumcraft/lang/research_original.json";

    /** The 1.7.10 lang file inside a reference-assets / fixture root. */
    public static final String REFERENCE_LANG_PATH = "thaumcraft/lang/en_US.lang";

    private ResearchOriginalLang() {
    }

    // ------------------------------------------------------------------
    // .lang parsing
    // ------------------------------------------------------------------

    /**
     * Parses a 1.7.10 {@code .lang} file body.
     *
     * <p>Rules, measured on the real 1794-line 4.2.3.5 {@code en_US.lang}:</p>
     * <ul>
     *   <li>a leading UTF-8 BOM is stripped;</li>
     *   <li>lines are split on {@code \n}; a trailing {@code \r} is dropped (the
     *   file is CRLF);</li>
     *   <li>blank lines and lines whose first non-blank character is {@code #}
     *   are skipped (the real file has 249 comment lines);</li>
     *   <li>the key is the text before the <b>first</b> {@code =}, trimmed; the
     *   value is everything after it, verbatim — not left-trimmed and not
     *   right-trimmed (17 real lines carry a leading space, 85 a trailing one,
     *   and a value may itself contain {@code =});</li>
     *   <li>Properties-style escapes are decoded ({@code \n}, {@code \r},
     *   {@code \t}, {@code \f}, {@code \\}, {@code \}{@code uXXXX}); an unknown escape
     *   keeps its character ({@code \:} -> {@code :});</li>
     *   <li>a line with no {@code =} is ignored; a duplicate key takes the last
     *   value.</li>
     * </ul>
     */
    public static Map<String, String> parseLang(String text) {
        Map<String, String> out = new LinkedHashMap<>();
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        for (String raw : text.split("\n", -1)) {
            String line = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
            String probe = line.trim();
            if (probe.isEmpty() || probe.charAt(0) == '#') {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }
            out.put(line.substring(0, eq).trim(), unescape(line.substring(eq + 1)));
        }
        return out;
    }

    /** Reads and parses a {@code .lang} file as UTF-8. */
    public static Map<String, String> parseLangFile(Path path) throws IOException {
        return parseLang(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    /** Decodes the Properties-style escapes the 1.7.10 lang loader understood. */
    static String unescape(String value) {
        if (value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                out.append(c);
                continue;
            }
            char next = value.charAt(++i);
            switch (next) {
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'f' -> out.append('\f');
                case '\\' -> out.append('\\');
                case 'u' -> {
                    if (i + 4 < value.length()) {
                        int code = parseHex4(value, i + 1);
                        if (code >= 0) {
                            out.append((char) code);
                            i += 4;
                            continue;
                        }
                    }
                    out.append('u');
                }
                default -> out.append(next);
            }
        }
        return out.toString();
    }

    private static int parseHex4(String text, int start) {
        int code = 0;
        for (int i = start; i < start + 4; i++) {
            int digit = Character.digit(text.charAt(i), 16);
            if (digit < 0) {
                return -1;
            }
            code = code * 16 + digit;
        }
        return code;
    }

    // ------------------------------------------------------------------
    // JSON
    // ------------------------------------------------------------------

    /**
     * Parses JSON into plain JDK types: {@code Map<String,Object>} for objects,
     * {@code List<Object>} for arrays, {@code String}, {@code Long}/{@code Double},
     * {@code Boolean} and {@code null}.
     *
     * <p>A hand-written reader keeps this build step dependency-free (the tool
     * source set carries no Minecraft and no Gson); the unit probes cross-check
     * it against Gson on the real 402 KB graph and on the writer's output.</p>
     */
    public static Object parseJson(String text) {
        JsonReader reader = new JsonReader(text);
        reader.skipWhitespace();
        Object value = reader.readValue();
        reader.skipWhitespace();
        if (!reader.atEnd()) {
            throw new IllegalArgumentException("trailing content at offset " + reader.position());
        }
        return value;
    }

    /** Parses a flat {@code key -> string} JSON object. */
    public static Map<String, String> parseFlatJson(String text) {
        Object parsed = parseJson(text);
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("expected a JSON object");
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            out.put(String.valueOf(entry.getKey()),
                    value instanceof String s ? s : String.valueOf(value));
        }
        return out;
    }

    /** Reads a UTF-8 file, tolerating (and stripping) a BOM. */
    public static String readUtf8(Path path) throws IOException {
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    /**
     * Every display key the shipped research graph references: each row's
     * {@code nameKey} and {@code textKey}, plus each page's {@code textKey}.
     *
     * <p>Structural, with no namespace filter — the walk reads the graph fields
     * the runtime records expose, so a page keyed {@code
     * focus.upgrade.vampirebats.text} is collected exactly like a {@code tc.}
     * one, and a {@code tc.}-prefixed string that is <em>not</em> a display key
     * (an icon path, a prerequisite id) is not. Derived, never hardcoded.</p>
     */
    public static Set<String> collectGraphKeys(String entriesJson) {
        Set<String> keys = new TreeSet<>();
        Object document = parseJson(entriesJson);
        for (Object row : asList(document instanceof Map<?, ?> doc ? doc.get(FIELD_ENTRIES) : document)) {
            if (row instanceof Map<?, ?> entry) {
                addDisplayKey(keys, entry, FIELD_NAME_KEY);
                addDisplayKey(keys, entry, FIELD_TEXT_KEY);
                for (Object page : asList(entry.get(FIELD_PAGES))) {
                    if (page instanceof Map<?, ?> pageMap) {
                        addDisplayKey(keys, pageMap, FIELD_TEXT_KEY);
                    }
                }
            }
        }
        return keys;
    }

    private static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static void addDisplayKey(Set<String> keys, Map<?, ?> object, String field) {
        if (object.get(field) instanceof String s && !s.isBlank()) {
            keys.add(s);
        }
    }

    /** Keys the graph references and the supplied lang actually defines. */
    public static Map<String, String> selectGraphKeys(Set<String> graphKeys, Map<String, String> lang) {
        Map<String, String> out = new TreeMap<>();
        for (String key : graphKeys) {
            String value = lang.get(key);
            if (value != null) {
                out.put(key, value);
            }
        }
        return out;
    }

    /** Graph-referenced keys the supplied lang does not define. */
    public static List<String> missingGraphKeys(Set<String> graphKeys, Map<String, String> lang) {
        List<String> missing = new ArrayList<>();
        for (String key : graphKeys) {
            if (!lang.containsKey(key)) {
                missing.add(key);
            }
        }
        return missing;
    }

    /** The generated JSON object: sorted keys, 2-space indent, final newline. */
    public static String writeJson(Map<String, String> values) {
        Map<String, String> sorted = new TreeMap<>(values);
        StringBuilder out = new StringBuilder();
        if (sorted.isEmpty()) {
            return "{}\n";
        }
        out.append("{\n");
        int written = 0;
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            out.append("  ").append(quote(entry.getKey())).append(": ")
                    .append(quote(entry.getValue()));
            if (++written < sorted.size()) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append("}\n");
        return out.toString();
    }

    /** Writes {@link #writeJson} to disk as UTF-8, creating the parents. */
    public static void writeJsonFile(Path path, Map<String, String> values) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, writeJson(values).getBytes(StandardCharsets.UTF_8));
    }

    static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    // ------------------------------------------------------------------
    // generate / check
    // ------------------------------------------------------------------

    /** generate &lt;langFile&gt; &lt;entriesJson&gt; &lt;outJson&gt; */
    static int generate(String langFile, String entriesJson, String outJson) throws IOException {
        Set<String> graphKeys = collectGraphKeys(readUtf8(Path.of(entriesJson)));
        Map<String, String> lang = parseLangFile(Path.of(langFile));
        Map<String, String> selected = selectGraphKeys(graphKeys, lang);
        writeJsonFile(Path.of(outJson), selected);
        List<String> missing = missingGraphKeys(graphKeys, lang);
        System.out.println("generated research lang: " + selected.size() + " keys from "
                + lang.size() + " lang keys over " + graphKeys.size() + " graph-referenced keys"
                + " -> " + outJson);
        System.out.println("  graph keys with no original string: " + missing.size()
                + (missing.isEmpty() ? "" : " " + missing));
        return 0;
    }

    /**
     * check &lt;langFile&gt; &lt;entriesJson&gt; &lt;committedLangJson&gt; &lt;jarPath&gt;
     *
     * <p>Fails when a graph-referenced key that the supplied original lang
     * defines is neither committed nor generated; also pins D-016 (the committed
     * lang defines <b>0</b> of the graph keys) and proves the generated file
     * reaches the jar with the lang's values verbatim. Graph keys the original
     * lang never defined (the 4 of 4.2.3.5) are reported as pending -- they are
     * authored by BOOK-3b, not generated here.</p>
     */
    static int check(String langFile, String entriesJson, String committedLangJson, String jarPath)
            throws IOException {
        Set<String> graphKeys = collectGraphKeys(readUtf8(Path.of(entriesJson)));
        Map<String, String> committed = parseFlatJson(readUtf8(Path.of(committedLangJson)));
        Set<String> committedGraph = new TreeSet<>(graphKeys);
        committedGraph.retainAll(committed.keySet());

        Path langPath = Path.of(langFile);
        if (!Files.isRegularFile(langPath)) {
            System.out.println("FAIL: no original lang at " + langPath
                    + " -- nothing to generate from");
            return 1;
        }
        Map<String, String> lang = parseLangFile(langPath);
        Map<String, String> expected = selectGraphKeys(graphKeys, lang);

        Map<String, String> generated = new TreeMap<>();
        try (ZipFile jar = new ZipFile(Path.of(jarPath).toFile())) {
            ZipEntry entry = jar.getEntry(GENERATED_RESOURCE_PATH);
            if (entry != null) {
                try (InputStream in = jar.getInputStream(entry)) {
                    generated = parseFlatJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }

        Set<String> generatedNotGraph = new TreeSet<>(generated.keySet());
        generatedNotGraph.removeAll(graphKeys);
        List<String> notGenerated = new ArrayList<>();
        List<String> valueMismatch = new ArrayList<>();
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String value = generated.get(entry.getKey());
            if (value == null) {
                notGenerated.add(entry.getKey());
            } else if (!value.equals(entry.getValue())) {
                valueMismatch.add(entry.getKey());
            }
        }
        List<String> pending = missingGraphKeys(graphKeys, lang);

        System.out.println("checkGeneratedResearchLang -- graph " + graphKeys.size()
                + " keys, original lang " + lang.size() + " keys"
                + " (" + count(lang.keySet(), "tc.research_name.") + " name / "
                + count(lang.keySet(), "tc.research_text.") + " text / "
                + count(lang.keySet(), "tc.research_page.") + " page)");
        System.out.println("  graph x original lang (must be generated): " + expected.size());
        System.out.println("  committed " + committed.size() + " keys, of which graph-referenced: "
                + committedGraph.size());
        System.out.println("  jar " + GENERATED_RESOURCE_PATH + ": " + generated.size() + " keys");
        System.out.println("  graph keys with no original string (authored by BOOK-3b): "
                + pending.size() + (pending.isEmpty() ? "" : " " + preview(pending)));

        int failures = 0;
        if (!committedGraph.isEmpty()) {
            System.out.println("FAIL: the committed en_us.json defines " + committedGraph.size()
                    + " graph-referenced keys, D-016 option 2 keeps 0: " + preview(committedGraph));
            failures++;
        }
        if (!generatedNotGraph.isEmpty()) {
            System.out.println("FAIL: the jar's generated file defines keys the graph does not "
                    + "reference: " + preview(generatedNotGraph));
            failures++;
        }
        if (!notGenerated.isEmpty()) {
            System.out.println("FAIL: " + notGenerated.size() + " graph-referenced keys the "
                    + "original lang defines are neither committed nor generated: "
                    + preview(notGenerated));
            failures++;
        }
        if (!valueMismatch.isEmpty()) {
            System.out.println("FAIL: " + valueMismatch.size() + " generated values differ from the "
                    + "original lang: " + preview(valueMismatch));
            failures++;
        }
        if (expected.isEmpty() && generated.isEmpty() && committedGraph.isEmpty()) {
            System.out.println("note: nothing to generate from this overlay source (0 graph keys "
                    + "in the original lang) -- boundaries checked, 0 generated");
        }
        if (failures > 0) {
            System.out.println("FAIL: " + failures + " contract violation(s)");
            return 1;
        }
        System.out.println("OK: " + generated.size() + " generated = " + expected.size()
                + " graph-referenced lang keys, 0 committed, " + pending.size() + " pending");
        return 0;
    }

    private static int count(Set<String> keys, String prefix) {
        int n = 0;
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                n++;
            }
        }
        return n;
    }

    private static String preview(java.util.Collection<String> keys) {
        List<String> sorted = new ArrayList<>(keys);
        sorted.sort(String::compareTo);
        if (sorted.size() <= 8) {
            return sorted.toString();
        }
        return sorted.subList(0, 8) + " ... (+" + (sorted.size() - 8) + " more)";
    }

    public static void main(String[] args) throws IOException {
        int exit = run(args);
        if (exit != 0) {
            System.exit(exit);
        }
    }

    /** Testable entry point: no {@code System.exit}. */
    public static int run(String[] args) throws IOException {
        if (args.length == 4 && args[0].equals("generate")) {
            return generate(args[1], args[2], args[3]);
        }
        if (args.length == 5 && args[0].equals("check")) {
            return check(args[1], args[2], args[3], args[4]);
        }
        System.err.println("usage: generate <langFile> <entriesJson> <outJson>");
        System.err.println("       check <langFile> <entriesJson> <committedLangJson> <jarPath>");
        return 2;
    }

    // ------------------------------------------------------------------
    // minimal JSON reader
    // ------------------------------------------------------------------

    private static final class JsonReader {
        private final String text;
        private int index;

        JsonReader(String text) {
            this.text = text;
        }

        int position() {
            return index;
        }

        boolean atEnd() {
            return index >= text.length();
        }

        void skipWhitespace() {
            while (index < text.length()) {
                char c = text.charAt(index);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    index++;
                } else {
                    return;
                }
            }
        }

        Object readValue() {
            skipWhitespace();
            if (atEnd()) {
                throw new IllegalArgumentException("unexpected end of JSON");
            }
            char c = text.charAt(index);
            return switch (c) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> readNumber();
            };
        }

        private Map<String, Object> readObject() {
            Map<String, Object> out = new LinkedHashMap<>();
            index++; // '{'
            skipWhitespace();
            if (peek() == '}') {
                index++;
                return out;
            }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                out.put(key, readValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    index++;
                } else if (c == '}') {
                    index++;
                    return out;
                } else {
                    throw new IllegalArgumentException(
                            "expected ',' or '}' at offset " + index);
                }
            }
        }

        private List<Object> readArray() {
            List<Object> out = new ArrayList<>();
            index++; // '['
            skipWhitespace();
            if (peek() == ']') {
                index++;
                return out;
            }
            while (true) {
                out.add(readValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    index++;
                } else if (c == ']') {
                    index++;
                    return out;
                } else {
                    throw new IllegalArgumentException(
                            "expected ',' or ']' at offset " + index);
                }
            }
        }

        private String readString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw new IllegalArgumentException("unterminated string at offset " + index);
                }
                char c = text.charAt(index++);
                if (c == '"') {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (atEnd()) {
                    throw new IllegalArgumentException("unterminated escape at offset " + index);
                }
                char esc = text.charAt(index++);
                switch (esc) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (index + 4 > text.length()) {
                            throw new IllegalArgumentException(
                                    "truncated \\u escape at offset " + index);
                        }
                        int code = parseHex4(text, index);
                        if (code < 0) {
                            throw new IllegalArgumentException(
                                    "bad \\u escape at offset " + index);
                        }
                        out.append((char) code);
                        index += 4;
                    }
                    default -> throw new IllegalArgumentException(
                            "bad escape '\\" + esc + "' at offset " + (index - 1));
                }
            }
        }

        private Object readLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw new IllegalArgumentException("bad literal at offset " + index);
            }
            index += literal.length();
            return value;
        }

        private Object readNumber() {
            int start = index;
            if (peek() == '-') {
                index++;
            }
            boolean floating = false;
            while (!atEnd()) {
                char c = text.charAt(index);
                if (c >= '0' && c <= '9') {
                    index++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    floating = true;
                    index++;
                } else {
                    break;
                }
            }
            String number = text.substring(start, index);
            if (number.isEmpty()) {
                throw new IllegalArgumentException("expected a value at offset " + start);
            }
            return floating ? (Object) Double.valueOf(number) : (Object) Long.valueOf(number);
        }

        private char peek() {
            if (atEnd()) {
                throw new IllegalArgumentException("unexpected end of JSON");
            }
            return text.charAt(index);
        }

        private void expect(char expected) {
            if (atEnd() || text.charAt(index) != expected) {
                throw new IllegalArgumentException(
                        "expected '" + expected + "' at offset " + index);
            }
            index++;
        }
    }
}
