package com.jonnyutah12.thaumcraftclassic.config;

/**
 * Value type of a config option, exactly as declared by the original
 * Thaumcraft 4.2.3.5 config class (Forge 1.7.10 {@code Configuration.get}
 * infers the type from the default value's class: boolean/int/string).
 */
public enum ConfigOptionType {
    BOOLEAN("boolean", Boolean.class),
    INTEGER("int", Integer.class),
    STRING("string", String.class);

    private final String docLabel;
    private final Class<?> valueClass;

    ConfigOptionType(String docLabel, Class<?> valueClass) {
        this.docLabel = docLabel;
        this.valueClass = valueClass;
    }

    /** Label used in docs/config-reference.md and asserted by its parser test. */
    public String docLabel() {
        return docLabel;
    }

    public Class<?> valueClass() {
        return valueClass;
    }
}
