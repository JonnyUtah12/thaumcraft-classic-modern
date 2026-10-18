package com.jonnyutah12.thaumcraftclassic.config;

/**
 * One option from the original Thaumcraft 4.2.3.5 config class
 * (thaumcraft/common/config/Config.java, decompiled — see
 * docs/config-reference.md for the human-readable inventory).
 *
 * <p>This is pure data: the same records drive the NeoForge
 * {@code ModConfigSpec} ({@link TcConfig}), the docs table
 * (docs/config-reference.md), and the parity unit tests. Any drift between
 * those three fails {@code ./gradlew test}.</p>
 *
 * @param category        original category name, verbatim (case-sensitive)
 * @param name            original option name, verbatim
 * @param type            declared value type
 * @param defaultValue    original config-file default (the value passed to
 *                        {@code Configuration.get}, which wins over any Java
 *                        field initializer — syncConfigurable always re-reads)
 * @param comment         original per-option comment, verbatim; "" when the
 *                        original set none
 * @param source          line reference into the decompiled Config.java
 * @param ported          false when the option cannot exist on the target
 *                        platform (see notPortedReason)
 * @param notPortedReason why the option is not implemented; null when ported
 */
public record ConfigOption(
        String category,
        String name,
        ConfigOptionType type,
        Object defaultValue,
        String comment,
        String source,
        boolean ported,
        String notPortedReason) {

    /** Dotted path as it appears in the config file (category.name). */
    public String path() {
        return category + "." + name;
    }

    /** Canonical default rendering used by docs/config-reference.md. */
    public String defaultLabel() {
        return String.valueOf(defaultValue);
    }
}
