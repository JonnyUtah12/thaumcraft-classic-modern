package com.jonnyutah12.thaumcraftclassic.aspect;

import java.util.List;

/**
 * Frozen copy of the original Thaumcraft 4.2.3.5 aspect table, extracted
 * mechanically from the decompiled {@code thaumcraft/api/aspects/Aspect.java}
 * static finals (reference tree, TC-M0-04 baseline). Every field of every row
 * is an original value: Java constant name, tag, display color, chat color
 * code (null for compounds), blend mode, and component tags in declaration
 * order (empty for the six primal aspects).
 *
 * <p>This is the drift guard for {@link AspectDefinitions}: the definitions
 * were generated from the same source, so the parity tests compare the port
 * against this frozen table rather than against itself. If the ported table
 * ever changes without a matching change here, the tests fail.</p>
 */
final class OriginalAspectTable {

    record Row(String constant, String tag, int color, String chatcolor, int blend,
               List<String> components) {
    }

    static final List<Row> ROWS = List.of(
            new Row("AIR", "aer", 16777086, "e", 1, List.of()),
            new Row("EARTH", "terra", 5685248, "2", 1, List.of()),
            new Row("FIRE", "ignis", 16734721, "c", 1, List.of()),
            new Row("WATER", "aqua", 3986684, "3", 1, List.of()),
            new Row("ORDER", "ordo", 14013676, "7", 1, List.of()),
            new Row("ENTROPY", "perditio", 4210752, "8", 771, List.of()),
            new Row("VOID", "vacuos", 8947848, null, 771, List.of("aer", "perditio")),
            new Row("LIGHT", "lux", 16774755, null, 1, List.of("aer", "ignis")),
            new Row("WEATHER", "tempestas", 16777215, null, 1, List.of("aer", "aqua")),
            new Row("MOTION", "motus", 13487348, null, 1, List.of("aer", "ordo")),
            new Row("COLD", "gelum", 14811135, null, 1, List.of("ignis", "perditio")),
            new Row("CRYSTAL", "vitreus", 8454143, null, 1, List.of("terra", "ordo")),
            new Row("LIFE", "victus", 14548997, null, 1, List.of("aqua", "terra")),
            new Row("POISON", "venenum", 9039872, null, 1, List.of("aqua", "perditio")),
            new Row("ENERGY", "potentia", 12648447, null, 1, List.of("ordo", "ignis")),
            new Row("EXCHANGE", "permutatio", 5735255, null, 1, List.of("perditio", "ordo")),
            new Row("METAL", "metallum", 11908557, null, 1, List.of("terra", "vitreus")),
            new Row("DEATH", "mortuus", 8943496, null, 1, List.of("victus", "perditio")),
            new Row("FLIGHT", "volatus", 15198167, null, 1, List.of("aer", "motus")),
            new Row("DARKNESS", "tenebrae", 2236962, null, 1, List.of("vacuos", "lux")),
            new Row("SOUL", "spiritus", 15461371, null, 1, List.of("victus", "mortuus")),
            new Row("HEAL", "sano", 16723764, null, 1, List.of("victus", "ordo")),
            new Row("TRAVEL", "iter", 14702683, null, 1, List.of("motus", "terra")),
            new Row("ELDRITCH", "alienis", 8409216, null, 1, List.of("vacuos", "tenebrae")),
            new Row("MAGIC", "praecantatio", 9896128, null, 1, List.of("vacuos", "potentia")),
            new Row("AURA", "auram", 16761087, null, 1, List.of("praecantatio", "aer")),
            new Row("TAINT", "vitium", 8388736, null, 1, List.of("praecantatio", "perditio")),
            new Row("SLIME", "limus", 129024, null, 1, List.of("victus", "aqua")),
            new Row("PLANT", "herba", 109568, null, 1, List.of("victus", "terra")),
            new Row("TREE", "arbor", 8873265, null, 1, List.of("aer", "herba")),
            new Row("BEAST", "bestia", 10445833, null, 1, List.of("motus", "victus")),
            new Row("FLESH", "corpus", 15615885, null, 1, List.of("mortuus", "bestia")),
            new Row("UNDEAD", "exanimis", 3817472, null, 1, List.of("motus", "mortuus")),
            new Row("MIND", "cognitio", 16761523, null, 1, List.of("ignis", "spiritus")),
            new Row("SENSES", "sensus", 1038847, null, 1, List.of("aer", "spiritus")),
            new Row("MAN", "humanus", 16766912, null, 1, List.of("bestia", "cognitio")),
            new Row("CROP", "messis", 14791537, null, 1, List.of("herba", "humanus")),
            new Row("MINE", "perfodio", 14471896, null, 1, List.of("humanus", "terra")),
            new Row("TOOL", "instrumentum", 4210926, null, 1, List.of("humanus", "ordo")),
            new Row("HARVEST", "meto", 15641986, null, 1, List.of("messis", "instrumentum")),
            new Row("WEAPON", "telum", 12603472, null, 1, List.of("instrumentum", "ignis")),
            new Row("ARMOR", "tutamen", 49344, null, 1, List.of("instrumentum", "terra")),
            new Row("HUNGER", "fames", 10093317, null, 1, List.of("victus", "vacuos")),
            new Row("GREED", "lucrum", 15121988, null, 1, List.of("humanus", "fames")),
            new Row("CRAFT", "fabrico", 8428928, null, 1, List.of("humanus", "instrumentum")),
            new Row("CLOTH", "pannus", 15395522, null, 1, List.of("instrumentum", "bestia")),
            new Row("MECHANISM", "machina", 8421536, null, 1, List.of("motus", "instrumentum")),
            new Row("TRAP", "vinculum", 10125440, null, 1, List.of("motus", "perditio")));

    private OriginalAspectTable() {
    }
}
