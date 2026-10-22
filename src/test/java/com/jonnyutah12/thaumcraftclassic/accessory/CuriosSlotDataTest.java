package com.jonnyutah12.thaumcraftclassic.accessory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M8-01: the Curios bridge is data-driven (Curios 9.5.1 registers slot
 * types from datapack JSON), so the shipped data files ARE the slot
 * registration. This test keeps them consistent with the frozen
 * {@link AccessorySlot} model: every slot type exists with the contracted
 * size, the entity grant covers players with exactly our slots, and every
 * slot has its item-routing tag present (an item tag is how items reach a
 * Curios slot; a missing tag silently means "nothing can be worn there").
 */
class CuriosSlotDataTest {

    @Test
    void slotTypeFilesMatchTheFrozenModel() throws IOException {
        final Path data = dataDir();
        for (final AccessorySlot slot : AccessorySlot.values()) {
            // Curios 9.5.1 loads slot types ONLY from the "curios" pack
            // namespace (CuriosSlotManager.apply filters on the resource
            // namespace); the slot id is the bare file name.
            final Path file = data.resolve("curios/curios/slots/" + slot.curiosSlotId() + ".json");
            assertTrue(Files.isRegularFile(file), "missing Curios slot type file: " + file);
            final JsonObject json = parse(file);
            assertEquals(slot.capacity(), json.get("size").getAsInt(),
                    slot.curiosSlotId() + " size does not match AccessorySlot.capacity()");
        }
    }

    @Test
    void entityGrantCoversPlayersWithExactlyOurSlots() throws IOException {
        final Path file = dataDir().resolve("thaumcraft/curios/entities/baubles.json");
        assertTrue(Files.isRegularFile(file), "missing Curios entity grant file: " + file);
        final JsonObject json = parse(file);
        final var entities = new java.util.ArrayList<String>();
        json.getAsJsonArray("entities").forEach(e -> entities.add(e.getAsString()));
        assertTrue(entities.contains("minecraft:player"), "grant does not cover players");
        final var slots = new java.util.ArrayList<String>();
        json.getAsJsonArray("slots").forEach(e -> slots.add(e.getAsString()));
        for (final AccessorySlot slot : AccessorySlot.values()) {
            assertTrue(slots.contains(slot.curiosSlotId()), "grant missing slot " + slot.curiosSlotId());
        }
        assertEquals(AccessorySlot.values().length, slots.size(),
                "grant contains slots outside the frozen model: " + slots);
    }

    @Test
    void everySlotHasItsItemRoutingTag() throws IOException {
        final Path data = dataDir();
        for (final AccessorySlot slot : AccessorySlot.values()) {
            final Path tag = data.resolve("curios/tags/item/" + slot.curiosItemTag() + ".json");
            assertTrue(Files.isRegularFile(tag), "missing Curios item tag: " + tag);
            parse(tag); // must be valid JSON
        }
    }

    private static JsonObject parse(final Path file) throws IOException {
        final var parsed = JsonParser.parseString(Files.readString(file));
        assertTrue(parsed.isJsonObject(), "not a JSON object: " + file);
        return parsed.getAsJsonObject();
    }

    private static Path dataDir() {
        final String value = System.getProperty("tc.modDataDir");
        assertNotNull(value, "tc.modDataDir system property not set (see build.gradle test task)");
        final Path dir = Path.of(value);
        assertTrue(Files.isDirectory(dir), "data directory not found: " + dir);
        return dir;
    }
}
