package com.jonnyutah12.thaumcraftclassic.world.outerlands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jonnyutah12.thaumcraftclassic.api.damagesource.DamageSourcesThaumcraft;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * TC-M6-04f-s parity evidence (unit half of the card's "registry + damage-type
 * tests" bar): the numeric contract table, the shipped damage-type data files
 * and their lang, and the content-mapping rows this card flipped.
 *
 * <p>Every number here is read off the decompiled original —
 * {@code BlockEldritch}, {@code BlockEldritchNothing}, {@code BlockFluidDeath},
 * {@code TileEldritch*}, {@code ItemEldritchObject},
 * {@code DamageSourceThaumcraft} — and {@link EldritchRealmValues} is the
 * single place the shipped code reads them from, so this test pins the table
 * against the original's values, not against the code.</p>
 */
class EldritchRealmValuesParityTest {

    // The test JVM's cwd is the build directory, not the repo root: the data
    // files come from the handles build.gradle wires into the test task
    // (recipes/InfusionRecipeDataTest.java:40 reads the same properties).
    private static Path resource(final String... parts) {
        final String path = System.getProperty("tc.modDataDir");
        assertNotNull(path, "system property 'tc.modDataDir' is not set — run tests through ./gradlew test");
        return Path.of(path).resolve("thaumcraft").resolve("damage_type")
                .resolve(Path.of(String.join("/", parts)));
    }

    private static JsonObject json(Path file) {
        try {
            return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void blockNumbersMatchTheOriginal() {
        // BlockEldritch: hardness 20 / resistance 1200 / ticking; getExpDrop
        // caps MAX_XP 3000 and MAX_STACK_XP 1500; eleven metas.
        assertEquals(20.0F, EldritchRealmValues.ELDRITCH_HARDNESS, 0.0F);
        assertEquals(1200.0F, EldritchRealmValues.ELDRITCH_RESISTANCE, 0.0F);
        assertEquals(20, EldritchRealmValues.ELDRITCH_TICK_PERIOD);
        assertEquals(3000, EldritchRealmValues.ELDRITCH_MAX_XP);
        assertEquals(1500, EldritchRealmValues.ELDRITCH_MAX_STACK_XP);
        assertEquals(11, EldritchRealmValues.ELDRITCH_VARIANT_COUNT);
        // Meta map of the original's BlockEldritch (altar .. crusted urn).
        assertEquals(0, EldritchRealmValues.ELDRITCH_ALTAR);
        assertEquals(1, EldritchRealmValues.ELDRITCH_OBELISK);
        assertEquals(2, EldritchRealmValues.ELDRITCH_OBELISK_TOP);
        assertEquals(3, EldritchRealmValues.ELDRITCH_CAP);
        assertEquals(6, EldritchRealmValues.ELDRITCH_DOORWAY);
        assertEquals(7, EldritchRealmValues.ELDRITCH_GATE);
        assertEquals(8, EldritchRealmValues.ELDRITCH_LOCK);
        assertEquals(9, EldritchRealmValues.ELDRITCH_CRAB_SPAWNER);
        assertEquals(10, EldritchRealmValues.ELDRITCH_TRAP);
        // getExpDrop per meta: obelisk 1000, obelisk top 0, caps/urns 500.
        assertEquals(1000, EldritchRealmValues.ELDRITCH_OBELISK_XP);
        assertEquals(0, EldritchRealmValues.ELDRITCH_OBELISK_TOP_XP);
        assertEquals(500, EldritchRealmValues.ELDRITCH_CAP_XP);

        // BlockEldritchNothing: hardness 0.2 / resistance 6,000,000; two metas.
        assertEquals(0.2F, EldritchRealmValues.NOTHING_HARDNESS, 0.0F);
        assertEquals(6_000_000.0F, EldritchRealmValues.NOTHING_RESISTANCE, 0.0F);
        assertEquals(2, EldritchRealmValues.NOTHING_VARIANT_COUNT);

        // BlockFluidDeath: 2.0 dissolve damage per contact (BlockFluidDeath:24).
        assertEquals(2.0F, EldritchRealmValues.DEATH_FLUID_DAMAGE, 0.0F);

        // ItemEldritchObject: setMaxDamage(0) — no durability bar
        // (ItemEldritchObject:34 func_77656_e(0)); the placer builds a
        // 7-cell column of altar + obelisk + obelisk tops after 6 empty cells.
        assertEquals(0, EldritchRealmValues.ELDRITCH_OBJECT_DURABILITY);
        assertEquals(6, EldritchRealmValues.PLACER_CLEAR_HEIGHT);
        assertEquals(EldritchRealmValues.PLACER_COLUMN_Y.length, EldritchRealmValues.PLACER_COLUMN_VARIANT.length);
        assertEquals(1, EldritchRealmValues.PLACER_COLUMN_Y[0]);
        assertEquals(EldritchRealmValues.ELDRITCH_ALTAR, EldritchRealmValues.PLACER_COLUMN_VARIANT[0]);
        assertEquals(EldritchRealmValues.ELDRITCH_OBELISK, EldritchRealmValues.PLACER_COLUMN_VARIANT[1]);
        for (int i = 2; i < EldritchRealmValues.PLACER_COLUMN_VARIANT.length; i++) {
            assertEquals(EldritchRealmValues.ELDRITCH_OBELISK_TOP, EldritchRealmValues.PLACER_COLUMN_VARIANT[i]);
        }
    }

    @Test
    void tileCadencesMatchTheOriginal() {
        // TileEldritchAltar: waves every 40 ticks from tick 80; max 4 eyes;
        // spawner arms at 3; clerics on the four corners (+-2), 3 of 4 needed
        // to switch to knight waves.
        assertEquals(80, EldritchRealmValues.ALTAR_FIRST_WAVE_TICKS);
        assertEquals(40, EldritchRealmValues.ALTAR_WAVE_PERIOD_TICKS);
        assertEquals(4, EldritchRealmValues.ALTAR_MAX_EYES);
        // BlockEldritch:250 arms the spawner at getEyes() >= 2 (cap < 4, :249).
        assertEquals(2, EldritchRealmValues.ALTAR_SPAWNER_EYES);
        assertEquals(2, EldritchRealmValues.ALTAR_CLERIC_OFFSET);
        assertEquals(3, EldritchRealmValues.ALTAR_CLERIC_SUCCESS);
        assertEquals(8, EldritchRealmValues.ALTAR_GUARD_CAP);
        assertEquals(24, EldritchRealmValues.ALTAR_GUARD_BOX_XZ);
        assertEquals(16, EldritchRealmValues.ALTAR_GUARD_BOX_Y);

        // TileEldritchObelisk: every 20 ticks, 6-cell radius, slowness +
        // weakness 40 ticks amplifier 0.
        assertEquals(20, EldritchRealmValues.OBELISK_TICK_PERIOD);
        assertEquals(6.0, EldritchRealmValues.OBELISK_RANGE, 0.0);
        assertEquals(40, EldritchRealmValues.OBELISK_EFFECT_TICKS);
        assertEquals(0, EldritchRealmValues.OBELISK_EFFECT_AMPLIFIER);

        // TileEldritchCrabSpawner: start 150, refill 50 + rand(50), fizz at
        // 15, reset 150 + rand(100), 16-cell player range, >5 crabs cap.
        assertEquals(150, EldritchRealmValues.CRAB_START_COUNT);
        assertEquals(50, EldritchRealmValues.CRAB_REFILL_MIN);
        assertEquals(50, EldritchRealmValues.CRAB_REFILL_SPAN);
        assertEquals(15, EldritchRealmValues.CRAB_FIZZ_COUNT);
        assertEquals(150, EldritchRealmValues.CRAB_RESET_MIN);
        assertEquals(100, EldritchRealmValues.CRAB_RESET_SPAN);
        assertEquals(16.0, EldritchRealmValues.CRAB_PLAYER_RANGE, 0.0);
        assertEquals(5, EldritchRealmValues.CRAB_ENTITY_CAP);

        // TileEldritchLock: fuse starts at 0, pump every 5 ticks, boss at 101.
        assertEquals(0, EldritchRealmValues.LOCK_ARMED_COUNT);
        assertEquals(5, EldritchRealmValues.LOCK_SOUND_PERIOD_TICKS);
        assertEquals(100, EldritchRealmValues.LOCK_BOSS_DELAY_TICKS);

        // BlockEldritch trap (meta 10): 500-tick cooldown.
        assertEquals(500, EldritchRealmValues.TRAP_COOLDOWN_TICKS);

        // The lock's boss rotation: golem, warden, crimson, taint (the
        // original's bossCount % 4 order).
        // TileEldritchLock:93 switch(bossCount % 4): 0 golem, 1 warden,
        // 2 crimson cultist, 3 taint — the rotation index is the count itself.
        assertEquals(List.of("golem", "warden", "crimson", "taint"),
                List.of(EldritchBossRoomHost.ROTATION));
        assertEquals("golem", EldritchBossRoomHost.bossOf(0));
        assertEquals("taint", EldritchBossRoomHost.bossOf(3));
        assertEquals("taint", EldritchBossRoomHost.bossOf(7));
        assertEquals("crimson", EldritchBossRoomHost.bossOf(10));
    }

    @Test
    void damageTypeFilesMatchTheOriginalsFlags() {
        final Map<String, String[]> expectations = Map.of(
                // message_id, effects (1.21.1 builds the death-message key as
                // death.attack.<message_id>; 21.1.259's DamageEffects has no
                // magic profile, so taint/dissolve ship hurt and the emitters
                // apply the original's armour bypass)
                "taint", new String[] {"thaumcraft.taint", "hurt"},
                "dissolve", new String[] {"thaumcraft.dissolve", "hurt"},
                "tentacle", new String[] {"thaumcraft.tentacle", "hurt"},
                "swarm", new String[] {"thaumcraft.swarm", "hurt"});
        for (final var entry : expectations.entrySet()) {
            final Path file = resource(entry.getKey() + ".json");
            assertTrue(Files.isRegularFile(file), file + " must ship");
            final JsonObject doc = json(file);
            assertEquals(entry.getValue()[0], doc.get("message_id").getAsString(),
                    entry.getKey() + " message_id");
            assertEquals(entry.getValue()[1], doc.get("effects").getAsString(),
                    entry.getKey() + " effects profile");
            assertEquals(0.1F, doc.get("exhaustion").getAsFloat(), 1.0E-4F,
                    entry.getKey() + " exhaustion multiplier");
        }
    }

    @Test
    void damageTypeMessagesResolveInEnglish() throws IOException {
        final Path lang = Path.of(System.getProperty("tc.modAssetsDir")).resolve("thaumcraft/lang/en_us.json");
        final JsonObject entries = JsonParser.parseString(Files.readString(lang)).getAsJsonObject();
        for (final String key : List.of(
                "death.attack.thaumcraft.taint", "death.attack.thaumcraft.taint.player",
                "death.attack.thaumcraft.dissolve", "death.attack.thaumcraft.dissolve.player",
                "death.attack.thaumcraft.tentacle",
                "death.attack.thaumcraft.swarm", "death.attack.thaumcraft.swarm.item")) {
            assertTrue(entries.has(key), key + " is referenced by a damage_type message_id and must ship");
        }
        // The Ancient Locking Mechanism's announcements (the original's four:
        // TileEldritchLock:136/220/300/340 — warden, golem, crimson, taint).
        for (final String key : List.of("tc.boss.crimson", "tc.boss.golem", "tc.boss.warden", "tc.boss.taint")) {
            assertTrue(entries.has(key), key + " is announced by the lock and must ship");
        }
    }

    @Test
    void damageSourceKeysMatchTheShippedData() {
        // The Java side must point at exactly the data/thaumcraft/damage_type
        // files above (the original's four DamageSourceThaumcraft sources).
        assertEquals("thaumcraft:taint", DamageSourcesThaumcraft.TAINT.location().toString());
        assertEquals("thaumcraft:dissolve", DamageSourcesThaumcraft.DISSOLVE.location().toString());
        assertEquals("thaumcraft:tentacle", DamageSourcesThaumcraft.TENTACLE.location().toString());
        assertEquals("thaumcraft:swarm", DamageSourcesThaumcraft.SWARM.location().toString());
    }

    @Test
    void eldritchMobIdsMatchTheOriginalRegistration() {
        // The Crimson Cult family names TC-M6-06 will register (entity
        // registration order in Thaumcraft.java), referenced by id from the
        // structure tiles.
        assertEquals("eldritchguardian", EldritchMobHost.GUARDIAN);
        assertEquals("cultist", EldritchMobHost.CULTIST);
        assertEquals("cultistcleric", EldritchMobHost.CLERIC);
        assertEquals("cultistknight", EldritchMobHost.KNIGHT);
        assertEquals("eldritchcrab", EldritchMobHost.CRAB);
        assertEquals("bobo", EldritchMobHost.BOBO);
        assertEquals("firemoth", EldritchMobHost.FIRE_MOTH);
    }

    @Test
    void contentMappingRowsAreFlippedToDone() throws IOException {
        final String path = System.getProperty("tc.contentMappingFile");
        final List<String> owned = List.of(
                "thaumcraft:blockeldritch", "thaumcraft:blockeldritchnothing",
                "thaumcraft:blockfluidpure", "thaumcraft:blockfluiddeath",
                "thaumcraft:fluidpure", "thaumcraft:fluiddeath",
                "thaumcraft:itembucketpure", "thaumcraft:itembucketdeath",
                "thaumcraft:itemeldritchobject",
                "thaumcraft:tileeldritchaltar", "thaumcraft:tileeldritchobelisk", "thaumcraft:tileeldritchcap",
                "thaumcraft:tileeldritchnothing", "thaumcraft:tileeldritchlock", "thaumcraft:tileeldritchcrabspawner");
        final var rows = com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv.read(Path.of(path));
        for (final String id : owned) {
            final var hits = rows.stream().filter(r -> r.newId().equals(id)).toList();
            assertEquals(1, hits.size(), id + " must have exactly one content-mapping row");
            // The tile rows' kind is tile_entity (inventory-only, keyed by
            // their block); block/fluid/item rows must resolve live, which
            // EldritchRealmGameTest.flippedRowsResolveLive checks in-world.
            // 'done' + 'tested' is the contract vocabulary (ContentMappingId
            // ContractTest.STATUSES): shipped and covered by the unit and
            // game tests on this card.
            assertEquals("done", hits.get(0).implementationStatus(), id + " row must read done");
            assertEquals("tested", hits.get(0).testStatus(), id + " row must read tested");
        }
    }
}
