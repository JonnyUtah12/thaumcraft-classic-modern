package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

/**
 * In-game smoke test: proves the game boots, the mod loads, and its metadata
 * is readable at runtime. Registered through {@code RegisterGameTestsEvent}
 * only when the gametest system is active, so it never runs in production.
 * Run with {@code ./gradlew runGameTestServer}.
 *
 * <p>Gameplay behavior (aspects, research, vis, crafting, equipment) gets its
 * gametests next to the feature it verifies — see docs/testing.md.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModSmokeGameTest {

    private ModSmokeGameTest() {
    }

    @GameTest(template = "empty")
    public static void modLoadsInGame(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded(ThaumcraftClassic.MOD_ID),
                "mod " + ThaumcraftClassic.MOD_ID + " is not loaded in the game test server");

        Optional<? extends ModContainer> container = ModList.get().getModContainerById(ThaumcraftClassic.MOD_ID);
        helper.assertTrue(container.isPresent(), "no mod container for " + ThaumcraftClassic.MOD_ID);
        helper.assertTrue(!container.get().getModInfo().getVersion().toString().isEmpty(),
                "mod version metadata is empty at runtime");

        helper.succeed();
    }
}
