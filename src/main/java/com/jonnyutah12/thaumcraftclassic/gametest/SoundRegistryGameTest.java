package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Runtime proof that the TC-M7-05 sound-event registry is live: every one of
 * the 66 original Thaumcraft sound events (docs/sound-events.md) must be
 * present in the SOUND_EVENT registry when the game runs, and the mod must
 * register no others. The sound DEFINITIONS (sounds.json + oggs) are
 * client-side assets delivered by the D-004 overlay and are guarded
 * statically by SoundRegistryParityTest; this test covers the registry half
 * on the game-test server, where the asset half is not loaded.
 *
 * <p>Run with {@code ./gradlew runGameTestServer}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoundRegistryGameTest {

    private SoundRegistryGameTest() {
    }

    @GameTest(template = "empty")
    public static void allOriginalSoundEventsRegistered(GameTestHelper helper) {
        List<ResourceLocation> ids = ModSounds.eventIds();
        helper.assertTrue(ids.size() == 66,
                "expected 66 sound events, ModSounds declares " + ids.size());

        List<ResourceLocation> missing = ids.stream()
                .filter(id -> !BuiltInRegistries.SOUND_EVENT.containsKey(id))
                .toList();
        helper.assertTrue(missing.isEmpty(),
                "sound events missing from the registry: " + missing);

        long registered = BuiltInRegistries.SOUND_EVENT.holders()
                .filter(key -> ThaumcraftClassic.MOD_ID.equals(key.key().location().getNamespace()))
                .count();
        helper.assertTrue(registered == ids.size(),
                "thaumcraft-namespace sound events in the registry (" + registered
                        + ") must equal the declared set (" + ids.size() + ")");

        helper.succeed();
    }
}
