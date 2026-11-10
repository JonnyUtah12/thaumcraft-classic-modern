package com.jonnyutah12.thaumcraftclassic.localization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;

/**
 * Reader for a 1.21.1 language file — `assets/<ns>/lang/<locale>.json`
 * (docs/architecture/localization.md). Such a file is exactly one flat JSON
 * object of string keys to string values, so this reader is dependency-free
 * (no JSON library): the main source set has none, and the same reader then
 * serves the in-game harness (LocalizationGameTest) and any local tool.
 *
 * <p>Malformed JSON throws — a language file that does not parse is the
 * TC-M7-01-b defect this card is about, never a silent empty mapping.</p>
 */
public final class LanguageJson {

    public static TreeMap<String, String> read(final Path file) throws IOException {
        return parse(Files.readString(file, StandardCharsets.UTF_8), file.toString());
    }

    public static TreeMap<String, String> parse(final String text, final String name) throws IOException {
        final TreeMap<String, String> entries = new TreeMap<>();
        final int[] at = new int[]{0};

        skipWhitespace(text, at);
        expect(text, at, '{', name);
        while (true) {
            skipWhitespace(text, at);
            if (peek(text, at) == '}') {
                at[0]++;
                break;
            }
            final String key = readJsonString(text, at, name);
            skipWhitespace(text, at);
            expect(text, at, ':', name);
            final String value = readJsonString(text, at, name);
            if (entries.putIfAbsent(key, value) != null) {
                throw new IOException(name + ": duplicate language key " + key);
            }
            skipWhitespace(text, at);
            if (peek(text, at) == ',') {
                at[0]++;
                continue;
            }
            break;
        }
        skipWhitespace(text, at);
        if (at[0] < text.length()) {
            throw new IOException(name + ": trailing content after the JSON object at offset " + at[0]);
        }
        return entries;
    }

    private static boolean skipWhitespace(final String text, final int[] at) {
        while (at[0] < text.length() && isWhitespace(text.charAt(at[0]))) {
            at[0]++;
        }
        return at[0] < text.length();
    }

    private static boolean isWhitespace(final char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private static char peek(final String text, final int[] at) {
        return at[0] < text.length() ? text.charAt(at[0]) : '\u0000';
    }

    private static void expect(final String text, final int[] at, final char want, final String name) throws IOException {
        if (at[0] < text.length() && text.charAt(at[0]) == want) {
            at[0]++;
            return;
        }
        final String got = at[0] < text.length() ? "'" + text.charAt(at[0]) + "'" : "end of file";
        throw new IOException(name + ": expected '" + want + "' at offset " + at[0] + ", got " + got);
    }

    /** One JSON string literal, with the escapes a language file needs. */
    private static String readJsonString(final String text, final int[] at, final String name) throws IOException {
        expect(text, at, '"', name);
        final StringBuilder out = new StringBuilder();
        while (at[0] < text.length()) {
            final char c = text.charAt(at[0]);
            if (c == '"') {
                at[0]++;
                return out.toString();
            }
            if (c == '\\') {
                at[0]++;
                if (at[0] >= text.length()) {
                    break;
                }
                final char escape = text.charAt(at[0]);
                switch (escape) {
                    case 'n': out.append('\n'); break;
                    case 't': out.append('\t'); break;
                    case 'r': out.append('\r'); break;
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case 'u':
                        if (at[0] + 4 < text.length()) {
                            out.append((char) Integer.parseInt(text.substring(at[0] + 1, at[0] + 5), 16));
                            at[0] += 4;
                        }
                        break;
                    default: out.append(escape); break;
                }
                at[0]++;
                continue;
            }
            out.append(c);
            at[0]++;
        }
        throw new IOException(name + ": unterminated string at offset " + at[0]);
    }

    private LanguageJson() {
    }
}
