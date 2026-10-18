package com.jonnyutah12.thaumcraftclassic.config;

/**
 * Read-time value corrections the original config class applied after
 * {@code Configuration.get} — the original clamped in Java, not in the
 * config file, so out-of-range values stay in the file and are corrected on
 * every read. Reproduced here verbatim; {@link TcConfig} applies these in
 * its typed accessors.
 *
 * <p>Original sources (decompiled Config.java): golem_delay L427-429,
 * golem_ignore_delay L434-436, golem_link_quality L441-443,
 * special_node_rarity L457-459, runic_* L508/511/514
 * ({@code Math.max(...)}).</p>
 */
public final class ConfigNormalizers {

    private ConfigNormalizers() {
    }

    /**
     * Applies the original read-time correction for {@code option} to a raw
     * config value. Options without a correction return the value unchanged.
     */
    public static int normalizeInt(ConfigOption option, int raw) {
        if (!option.type().equals(ConfigOptionType.INTEGER)) {
            throw new IllegalArgumentException("not an int option: " + option.path());
        }
        return switch (option.path()) {
            // "if (golemDelay < 1) golemDelay = 1;"
            case "general.golem_delay" -> Math.max(1, raw);
            // "if (golemIgnoreDelay < 1000) golemIgnoreDelay = 1000;"
            case "general.golem_ignore_delay" -> Math.max(1000, raw);
            // "if (golemLinkQuality < 4) golemLinkQuality = 0;" — below 4 disables the fx entirely
            case "general.golem_link_quality" -> raw < 4 ? 0 : raw;
            // "if (specialNodeRarity < 3) specialNodeRarity = 3;"
            case "general.special_node_rarity" -> Math.max(3, raw);
            // "shieldRecharge = Math.max(500, rss.getInt());"
            case "Runic_Shielding.runic_recharge_speed" -> Math.max(500, raw);
            // "shieldWait = Math.max(0, rsd.getInt());"
            case "Runic_Shielding.runic_recharge_delay" -> Math.max(0, raw);
            // "shieldCost = Math.max(0, rsc.getInt());"
            case "Runic_Shielding.runic_cost" -> Math.max(0, raw);
            default -> raw;
        };
    }
}
