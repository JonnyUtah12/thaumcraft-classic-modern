package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The alchemical furnace's face machine, read out of the <b>shipped</b> JSON
 * (BD-18, t_fd5265f3).
 *
 * <p>4.2.3.5 picked the furnace's faces inside its render hooks, from live tile
 * data, every frame:</p>
 * <ul>
 *   <li>{@code BlockArcaneFurnace.java:43-49} registers 27 icons
 *       {@code thaumcraft:furnace<N>} for {@code N < 27} <em>skipping 8 and
 *       24</em>, so those two names are never registered and the port may never
 *       name them ({@code docs/asset-mapping.csv} publishes exactly the other
 *       25 rows);</li>
 *   <li>{@code calculateTexture} ({@code BlockArcaneFurnace.java:57-156}, the
 *       exact range verified in the decompiled tree) picks the face icon from
 *       the block's own metadata, the vertical-run level and {@code add};</li>
 *   <li>{@code calculateLevel} ({@code :158-177}) answers 9 for the mid of a
 *       vertical run of same-metadata furnaces, 18 for the top of one (the
 *       block above matches and the block below does not) and 0 standalone;</li>
 *   <li>{@code add = 3} when {@code BlockUtils.isBlockTouchingOnSide(world, x,
 *       y, z, this, 10, side)} — a metadata-10 nozzle touching that face with
 *       clean edges ({@code BlockArcaneFurnace.java:61-63},
 *       {@code BlockUtils.java:342-379});</li>
 *   <li>the ISMR ({@code BlockArcaneFurnaceRenderer.java:16-59}, 72 lines)
 *       draws the base plate's cube with {@code renderer.overrideBlockTexture =
 *       Blocks.lava.getIcon(0)} and a fire pass ({@code :19-23}), and the
 *       nozzle's art as three quads on the side of the base plate it finds in
 *       its west / east / north / (else) +Z branch ({@code :26-56}): the
 *       {@code furnace13} plate at {@code x - W10} (W10 = 0.625,
 *       {@code BlockRenderer.java:22}), the {@code furnace15} nozzle face at
 *       {@code x - 0.8} and a fire quad at {@code x - 0.9} drawn after the
 *       block is re-bounded to 1.5 tall, so the flame quad is 24/16 high.</li>
 * </ul>
 *
 * <p>The port ships two of the original's eleven metadata (0 = the base plate,
 * 10 = the nozzle; the shell metas 1-9 are TC-M4-06-m), so the reachable slice
 * of the face machine is the one this helper encodes. Per D-020 the tile
 * publishes the original's condition into block state properties
 * ({@code stack} = {@code calculateLevel}, {@code attached} = the touching
 * partner half) and the blockstate names a port-authored model per value.</p>
 */
final class ArcaneFurnaceFaceTable {

    private ArcaneFurnaceFaceTable() {
    }

    /** {@code Blocks.lava} (fields CSV line 1354) at icon index 0 — 1.21.1 has no {@code block/lava.png}. */
    static final String SPRITE_LAVA = "minecraft:block/lava_still";
    /** {@code Blocks.fire} (fields CSV line 1481) at icon index 0. */
    static final String SPRITE_FIRE = "minecraft:block/fire_0";
    static final String FURNACE6 = "thaumcraft:block/furnace6";
    static final String FURNACE7 = "thaumcraft:block/furnace7";
    static final String FURNACE13 = "thaumcraft:block/furnace13";
    static final String FURNACE15 = "thaumcraft:block/furnace15";

    /** The two icons {@code BlockArcaneFurnace.java:43-49} never registers. */
    static final Set<String> NEVER_REGISTERED = Set.of(
            "thaumcraft:block/furnace8", "thaumcraft:block/furnace24");

    /** The original's vertical-run answer, as the shipped property's value. */
    static final int STACK_STANDALONE = 0;
    static final int STACK_TOP = 1;
    static final int STACK_MID = 2;

    /** {@code attached} value meaning "no partner half touching" (the original's {@code add == 0}). */
    static final int ATTACHED_NONE = 6;

    /** One quad the ISMR drew outside the block's own cell. */
    record Art(String side, String texture, float centre, float top) {
    }

    /**
     * A shipped model, split the way the face machine is: the faces of the
     * element inside the block's cell (the body cube) and the quads the ISMR
     * drew outside it (the nozzle's art plates and flame).
     */
    record Model(String ref, Map<String, String> faces, List<Art> arts, Set<String> sprites) {
    }

    // ------------------------------------------------------ shipped JSON

    /** The model a shipped blockstate names for {@code state} (its key must match exactly once). */
    static String modelRef(final Block block, final BlockState state) {
        final JsonObject json = asset("blockstates/" + blockId(block) + ".json");
        if (json == null || !json.has("variants")) {
            return null;
        }
        final StateDefinition<Block, BlockState> definition = block.getStateDefinition();
        String found = null;
        int hits = 0;
        for (final Map.Entry<String, JsonElement> variant : json.getAsJsonObject("variants").entrySet()) {
            if (keyMatches(variant.getKey(), state, definition)) {
                final JsonElement model = variant.getValue().getAsJsonObject().get("model");
                if (model != null) {
                    found = model.getAsString();
                    hits++;
                }
            }
        }
        return hits == 1 ? found : null;
    }

    /** Parse a shipped {@code thaumcraft:block/…} model into {@link Model}; null when absent. */
    static Model model(final String modelRef) {
        if (modelRef == null || !modelRef.startsWith(ThaumcraftClassic.MOD_ID + ":")) {
            return null;
        }
        final JsonObject json = asset("models/" + modelRef.substring(modelRef.indexOf(':') + 1) + ".json");
        if (json == null) {
            return null;
        }
        final Map<String, String> aliases = new LinkedHashMap<>();
        if (json.has("textures")) {
            for (final Map.Entry<String, JsonElement> texture : json.getAsJsonObject("textures").entrySet()) {
                aliases.put(texture.getKey(), texture.getValue().getAsString());
            }
        }
        final Map<String, String> faces = new LinkedHashMap<>();
        final List<Art> arts = new ArrayList<>();
        final Set<String> sprites = new LinkedHashSet<>();
        // A parent-only model (cube/cube_all with a `textures` map and no
        // `elements`) carries no quads of its own: the faces below stay empty
        // and the probe reports the model as naming nothing, which is the truth
        // the face machine must not be able to hide behind.
        for (final JsonElement element : json.has("elements") ? json.getAsJsonArray("elements") : new com.google.gson.JsonArray()) {
            final JsonObject box = element.getAsJsonObject();
            final float[] from = xyz(box.getAsJsonArray("from"));
            final float[] to = xyz(box.getAsJsonArray("to"));
            final boolean inCell = from[0] >= 0 && from[1] >= 0 && from[2] >= 0
                    && to[0] <= 16 && to[1] <= 16 && to[2] <= 16;
            for (final Map.Entry<String, JsonElement> face : box.getAsJsonObject("faces").entrySet()) {
                final String raw = face.getValue().getAsJsonObject().get("texture").getAsString();
                final String sprite = raw.startsWith("#") ? aliases.get(raw.substring(1)) : raw;
                if (sprite == null) {
                    continue;
                }
                sprites.add(sprite);
                if (inCell) {
                    faces.put(face.getKey(), sprite);
                } else {
                    final int axis = switch (face.getKey()) {
                        case "west", "east" -> 0;
                        case "up", "down" -> 1;
                        default -> 2;
                    };
                    arts.add(new Art(face.getKey(), sprite, (from[axis] + to[axis]) / 2f, to[1]));
                }
            }
        }
        return new Model(modelRef, faces, arts, sprites);
    }

    // ------------------------------------------------- the original's table

    /**
     * The side the nozzle's art quads land on for an {@code attached} value —
     * the ISMR's 4-way branch order west, east, north, else the +Z (south)
     * fallback ({@code BlockArcaneFurnaceRenderer.java:26-56}), so a partner
     * above/below (or no partner at all) draws the south art.
     */
    static String artSideFor(final int attached) {
        return switch (attached) {
            case 4 -> "west";
            case 5 -> "east";
            case 2 -> "north";
            default -> "south";
        };
    }

    /**
     * The nozzle's own side faces: {@code calculateTexture}'s default branch
     * answers {@code level != 9 ? icon[7] : icon[6]} for metas the per-side
     * switches do not name — the metadata-10 nozzle names none of them
     * ({@code BlockArcaneFurnace.java:104-155}).
     */
    static String nozzleBodySide(final int stack) {
        return stack == STACK_MID ? FURNACE6 : FURNACE7;
    }

    /**
     * The nozzle's top/bottom faces. The original's answer for metadata 10 is
     * {@code icon[27]} on a 27-slot array — an out-of-range read, i.e. there is
     * no legitimate sprite for those faces (the ISMR's own draw is what the
     * player sees); the port names the side sprite it already ships for the
     * block, and the face-table probe pins that choice.
     */
    static String nozzleBodyCap() {
        return FURNACE7;
    }

    /** The original's metadata for a shipped variant (0 = meta 0, 1 = meta 10). */
    static int originalMeta(final int variant) {
        return variant == 1 ? 10 : 0;
    }

    /**
     * The port's {@code stack} value for the original's {@code calculateLevel}
     * (lives in {@code BlockArcaneFurnace} so the tile and the probes read one
     * implementation). Kept here as the citation anchor: 9 → mid, 18 → top, 0 →
     * standalone.
     */
    static int stackForLevel(final int level) {
        return switch (level) {
            case 9 -> STACK_MID;
            case 18 -> STACK_TOP;
            default -> STACK_STANDALONE;
        };
    }

    // ------------------------------------------------------------------ shell
    // TC-M4-06-m (t_5eb27d02): the face rule of the nine shell metas 1-9,
    // transcribed from calculateTexture (BlockArcaneFurnace.java:57-156) the same
    // way the nozzle's rules above were, so a probe can ask "what does the
    // original answer this face" for a shell cell and compare it with the shipped
    // model. The rule is the authority; the generator
    // (scripts/build_arcanefurnace_models.py) mirrors it, and the coverage probe
    // reads the shipped JSON back out.

    /** 1.7.10's side ordinals: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east. */
    static final List<String> SHELL_SIDES =
            List.of("down", "up", "north", "south", "west", "east");
    /** {@code calculateLevel}'s answer ({@code :158-177}) per shipped {@code stack} value. */
    static final int[] SHELL_LEVEL_FOR_STACK = {0, 18, 9};
    /** The caps' atlas index for a shell metadata — {@code (meta-1)%3 + (meta-1)/3*9} ({@code :78-84}). */
    static final int[] SHELL_GRID_OF_META = new int[10];
    /** The caps at the top of a vertical run ({@code :66-70}): meta → icon. */
    static final int[] SHELL_TOP_CAP_AT_18 = new int[10];
    /** The side faces ({@code :86-127}): 1.7.10 side → metadata → the icon's column. */
    static final Map<Integer, Map<Integer, Integer>> SHELL_SIDE_COLUMN = Map.of(
            2, Map.of(1, 2, 2, 1, 3, 0),   // north: metas 1, 2, 3
            3, Map.of(7, 0, 8, 1, 9, 2),   // south: metas 7, 8, 9
            4, Map.of(1, 0, 4, 1, 7, 2),   // west:  metas 1, 4, 7
            5, Map.of(3, 2, 6, 1, 9, 0));  // east:  metas 3, 6, 9

    static {
        for (int meta = 1; meta <= 9; meta++) {
            SHELL_GRID_OF_META[meta] = (meta - 1) % 3 + (meta - 1) / 3 * 9;
        }
        SHELL_TOP_CAP_AT_18[2] = 16;
        SHELL_TOP_CAP_AT_18[4] = 17;
        SHELL_TOP_CAP_AT_18[6] = 26;
        SHELL_TOP_CAP_AT_18[8] = 25;
    }

    /**
     * The icon {@code calculateTexture} answers for a shell cell: metadata
     * {@code meta} (1-9), the run level the {@code stack} property carries, and
     * the neighbour gate {@code add} (3 when a metadata-10 nozzle touches this
     * face, {@code :61-63}) on 1.7.10's side {@code side}.
     */
    static int shellIcon(final int meta, final int stack, final int add, final int side) {
        final int level = SHELL_LEVEL_FOR_STACK[stack];
        if (side == 0 || side == 1) {
            if (side == 1 && level == 18 && SHELL_TOP_CAP_AT_18[meta] != 0) {
                return SHELL_TOP_CAP_AT_18[meta];
            }
            if (add != 3) {
                return meta == 5 ? 10 : SHELL_GRID_OF_META[meta];
            }
            return 6;
        }
        final Map<Integer, Integer> column = SHELL_SIDE_COLUMN.get(side);
        final Integer columnIcon = column == null ? null : column.get(meta);
        return columnIcon != null ? columnIcon + level + add : (level != 9 ? 7 : 6);
    }

    /** The sprite the original answers for a shell face, in shipped-name form. */
    static String shellFace(final int meta, final int stack, final int attached, final String side) {
        final int ordinal = SHELL_SIDES.indexOf(side);
        return "thaumcraft:block/furnace" + shellIcon(meta, stack, ordinal == attached ? 3 : 0, ordinal);
    }

    // ------------------------------------------------------------- plumbing

    static String blockId(final Block block) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    private static boolean keyMatches(final String key, final BlockState state,
                                      final StateDefinition<Block, BlockState> definition) {
        if (key.isEmpty()) {
            return true;
        }
        for (final String pair : key.split(",")) {
            final int eq = pair.indexOf('=');
            if (eq <= 0) {
                return false;
            }
            final String name = pair.substring(0, eq);
            for (final Property<?> property : definition.getProperties()) {
                if (property.getName().equals(name)) {
                    if (!serialise(property, state.getValue(property)).equals(pair.substring(eq + 1))) {
                        return false;
                    }
                    break;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String serialise(final Property<T> property,
                                                              final Comparable<?> value) {
        return property.getName((T) value);
    }

    private static float[] xyz(final com.google.gson.JsonArray array) {
        final float[] out = new float[3];
        for (int i = 0; i < 3; i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    private static JsonObject asset(final String relativePath) {
        final String full = "/assets/" + ThaumcraftClassic.MOD_ID + "/" + relativePath;
        try (InputStream in = ArcaneFurnaceFaceTable.class.getResourceAsStream(full)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            ThaumcraftClassic.LOGGER.warn("[BD-18] unreadable asset {}", full, e);
            return null;
        }
    }

    /** The position a probe places the machine at, relative to its structure. */
    static BlockPos centre() {
        return new BlockPos(2, 3, 2);
    }

    /** The shipped state at a probe position (structure-relative → absolute). */
    static BlockState stateAt(final net.minecraft.gametest.framework.GameTestHelper helper, final BlockPos pos) {
        return helper.getLevel().getBlockState(helper.absolutePos(pos));
    }

    static Direction[] horizontalInOriginalOrder() {
        return new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
    }
}
