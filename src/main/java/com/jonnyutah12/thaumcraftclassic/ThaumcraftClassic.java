package com.jonnyutah12.thaumcraftclassic;

import com.jonnyutah12.thaumcraftclassic.gametest.ModSmokeGameTest;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.slf4j.Logger;

/**
 * Mod entry point (common/server side). Client-only code lives under
 * {@code client} and is loaded through NeoForge's dist-separated entry point.
 *
 * <p>MOD_ID is the lowercased original mod id: Minecraft 1.21.1 requires
 * lowercase resource paths, so original content ids like
 * {@code Thaumcraft:elementium_ore} map to {@code thaumcraft:elementium_ore}
 * — the closest the target platform permits. See docs/porting-decisions.md.</p>
 *
 * <p>Feature milestones add DeferredRegister holders here; every holder must
 * be initialized from this constructor before any RegisterEvent fires, in
 * dependency order (components, blocks, items, block entities, tabs,
 * attachments).</p>
 */
@Mod(ThaumcraftClassic.MOD_ID)
public final class ThaumcraftClassic {
    public static final String MOD_ID = "thaumcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ThaumcraftClassic(final IEventBus modEventBus, final ModContainer modContainer) {
        // Registration holders are wired in by the content milestones
        // (M2+). Nothing may register before its holder is initialized.

        modEventBus.addListener(ThaumcraftClassic::registerGameTests);

        LOGGER.info("Thaumcraft Classic (unofficial port of Thaumcraft 4.2.3.5) initializing; target MC {} / NeoForge",
                "1.21.1");
    }

    private static void registerGameTests(final RegisterGameTestsEvent event) {
        event.register(ModSmokeGameTest.class);
    }
}
