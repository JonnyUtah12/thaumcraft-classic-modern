package com.jonnyutah12.thaumcraftclassic.config;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NeoForge 21.1 config binding for the original Thaumcraft 4.2.3.5 option
 * set. The spec is built entirely from {@link TcConfigCatalog} — names,
 * categories, types, defaults and comments are not repeated here, so the
 * catalog, docs/config-reference.md and the live spec cannot drift apart
 * silently (unit + gametest parity checks guard them).
 *
 * <p>One COMMON config file (thaumcraft-common.toml) mirrors the original's
 * single Thaumcraft.cfg: same categories, same option names, same
 * defaults. The original was also a single common-side file whose
 * client-relevant values were synced to clients (PacketConfig); NeoForge
 * COMMON configs are server-owned and synced, which matches that model.</p>
 *
 * <p>Out-of-range values: the original clamped in Java at read time and
 * left the file untouched; {@link ConfigNormalizers} reproduces those
 * clamps in the typed getters below, so the file keeps the user's value and
 * every read is corrected exactly like the original.</p>
 *
 * <p>Gameplay wiring: systems read options through the typed getters here
 * as they land (TC-M2+ milestones); nothing consumes them yet.</p>
 */
public final class TcConfig {

    private static final Map<ConfigOption, ModConfigSpec.ConfigValue<Object>> FIELDS = new LinkedHashMap<>();

    public static final ModConfigSpec SPEC = buildSpec();

    private TcConfig() {
    }

    /** Registers the config with FML; call once from the mod constructor. */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    // ---- typed accessors (the seam every later system reads through) --------

    public static boolean getBool(ConfigOption option) {
        Object value = raw(option);
        if (!(value instanceof Boolean b)) {
            throw wrongType(option, value);
        }
        return b;
    }

    /** Raw int value with the original read-time clamps applied. */
    public static int getInt(ConfigOption option) {
        Object value = raw(option);
        if (!(value instanceof Integer i)) {
            throw wrongType(option, value);
        }
        return ConfigNormalizers.normalizeInt(option, i);
    }

    public static String getString(ConfigOption option) {
        Object value = raw(option);
        if (!(value instanceof String s)) {
            throw wrongType(option, value);
        }
        return s;
    }

    /**
     * Comma-delimited string options (general.portablehole_blacklist): the
     * original split on "," and trimmed nothing; empty entries were skipped
     * by the consumer. Splitting and trimming matches the effective original
     * behaviour (Block.getBlockFromName ignores surrounding whitespace).
     */
    public static List<String> getStringList(ConfigOption option) {
        List<String> out = new ArrayList<>();
        for (String part : getString(option).split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    /** Live spec value tree; used by the parity gametest. */
    public static Map<ConfigOption, ModConfigSpec.ConfigValue<Object>> fields() {
        return FIELDS;
    }

    private static Object raw(ConfigOption option) {
        ModConfigSpec.ConfigValue<Object> field = FIELDS.get(option);
        if (field == null) {
            throw new IllegalArgumentException("option is not part of the spec: " + option.path()
                    + (option.ported() ? "" : " (not ported: " + option.notPortedReason() + ")"));
        }
        return field.get();
    }

    private static IllegalStateException wrongType(ConfigOption option, Object value) {
        return new IllegalStateException("config option " + option.path() + " holds "
                        + (value == null ? "null" : value.getClass().getName())
                        + ", expected " + option.type().valueClass().getName());
    }

    private static ModConfigSpec buildSpec() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        for (String category : TcConfigCatalog.CATEGORY_ORDER) {
            List<ConfigOption> options = TcConfigCatalog.optionsIn(category).stream()
                    .filter(ConfigOption::ported)
                    .toList();
            if (options.isEmpty()) {
                // Category exists only for not-ported options (Enchantments);
                // it disappears from the file, which docs/config-reference.md
                // records.
                continue;
            }
            String categoryComment = TcConfigCatalog.CATEGORY_COMMENTS.get(category);
            if (categoryComment != null && !categoryComment.isEmpty()) {
                builder.comment(categoryComment);
            }
            builder.push(category);
            for (ConfigOption option : options) {
                if (!option.comment().isEmpty()) {
                    builder.comment(option.comment());
                }
                FIELDS.put(option, builder.define(option.name(), option.defaultValue()));
            }
            builder.pop();
        }
        return builder.build();
    }

    static {
        // Fail fast at class init if the catalog and the built spec ever
        // disagree (e.g. a duplicate option name inside one category).
        if (FIELDS.size() != TcConfigCatalog.portedOptions().size()) {
            ThaumcraftClassic.LOGGER.error("TcConfig spec holds {} options but the catalog declares {} ported options",
                    FIELDS.size(), TcConfigCatalog.portedOptions().size());
        }
    }
}
