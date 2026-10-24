package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The state carried by one research note (TC-M3-04).
 *
 * <p>Port of {@code thaumcraft.common.lib.research.ResearchNoteData} plus
 * the {@code ResearchManager.HexEntry} inner class (TC 4.2.3.5). The
 * original was a mutable holder written to the note item's NBT under the
 * keys {@code key}, {@code color}, {@code complete}, {@code copies} and
 * {@code hexgrid} (a list of {@code {hexq byte, hexr byte, type byte,
 * aspect string}} compounds). The port is an immutable record carried on the
 * note stack as the {@code thaumcraft:research_note} data component; the
 * codec writes exactly the original tag layout (byte numerics included) so a
 * future legacy-import path can read TC4 item NBT with it, and reads
 * leniently (any numeric width).</p>
 *
 * <p>Hex map keys are the original {@code "q:r"} strings; the hex
 * coordinates are derived from the key, so the original's parallel
 * {@code hexes} map is not duplicated here.</p>
 *
 * <p>Hex entry types keep the original numbering: 0 = empty slot,
 * 1 = required aspect (printed on the note), 2 = player-placed aspect.</p>
 */
public record ResearchNoteData(
        String key,
        int color,
        boolean complete,
        int copies,
        Map<String, HexEntry> hexEntries) {

    /** One grid cell: the aspect tag (null for empty cells) and the type. */
    public record HexEntry(String aspectTag, int type) {

        public boolean empty() {
            return aspectTag == null || aspectTag.isEmpty();
        }
    }

    /** A hexgrid row in the original NBT shape. */
    private record HexGridRow(int q, int r, String aspectTag, int type) {
    }

    /**
     * One hexgrid entry. {@code Codec.BYTE} writes NBT bytes (the original
     * width) and reads any numeric width, matching the original's
     * {@code getByte} leniency.
     */
    private static final Codec<HexGridRow> HEX_ROW = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BYTE.fieldOf("hexq").forGetter(row -> (byte) row.q()),
            Codec.BYTE.fieldOf("hexr").forGetter(row -> (byte) row.r()),
            Codec.BYTE.fieldOf("type").forGetter(row -> (byte) row.type()),
            Codec.STRING.optionalFieldOf("aspect", "").forGetter(row -> row.aspectTag() == null ? "" : row.aspectTag()))
            .apply(instance, (q, r, type, aspect) -> new HexGridRow(q, r, aspect.isEmpty() ? null : aspect, type)));

    /** The original {@code hexgrid} list layout, exposed as a "q:r"-keyed map. */
    private static Codec<Map<String, HexEntry>> hexGridCodec() {
        return new ListCodec<>(HEX_ROW).xmap(
                rows -> {
                    Map<String, HexEntry> map = new LinkedHashMap<>();
                    for (HexGridRow row : rows) {
                        map.put(new Hex(row.q(), row.r()).toString(),
                                new HexEntry(row.aspectTag(), row.type()));
                    }
                    return map;
                },
                map -> {
                    List<HexGridRow> rows = new ArrayList<>();
                    for (Map.Entry<String, HexEntry> e : map.entrySet()) {
                        Hex hex = Hex.fromKey(e.getKey());
                        rows.add(new HexGridRow(hex.q(), hex.r(), e.getValue().aspectTag(), e.getValue().type()));
                    }
                    return rows;
                });
    }

    public static final Codec<ResearchNoteData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("key", "").forGetter(ResearchNoteData::key),
            Codec.INT.optionalFieldOf("color", 0).forGetter(ResearchNoteData::color),
            Codec.BOOL.optionalFieldOf("complete", false).forGetter(ResearchNoteData::complete),
            Codec.INT.optionalFieldOf("copies", 0).forGetter(ResearchNoteData::copies),
            hexGridCodec().forGetter(ResearchNoteData::hexEntries))
            .apply(instance, ResearchNoteData::new));

    public static final StreamCodec<ByteBuf, ResearchNoteData> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);

    public ResearchNoteData {
        hexEntries = hexEntries == null ? Map.of() : Map.copyOf(hexEntries);
    }

    /** The hex coordinates keyed the way the original keyed them. */
    public Map<String, Hex> hexes() {
        Map<String, Hex> out = new LinkedHashMap<>();
        for (String k : hexEntries.keySet()) {
            out.put(k, Hex.fromKey(k));
        }
        return out;
    }

    public HexEntry entryAt(Hex hex) {
        return hexEntries.get(hex.toString());
    }

    public ResearchNoteData withHexEntry(Hex hex, HexEntry entry) {
        Map<String, HexEntry> next = new HashMap<>(hexEntries);
        next.put(hex.toString(), entry);
        return new ResearchNoteData(key, color, complete, copies, next);
    }

    public ResearchNoteData without(Iterable<String> hexKeys) {
        Map<String, HexEntry> next = new HashMap<>(hexEntries);
        for (String k : hexKeys) {
            next.remove(k);
        }
        return new ResearchNoteData(key, color, complete, copies, next);
    }

    public ResearchNoteData withComplete(boolean value) {
        return new ResearchNoteData(key, color, value, copies, hexEntries);
    }

    public ResearchNoteData withCopies(int value) {
        return new ResearchNoteData(key, color, complete, value, hexEntries);
    }
}
