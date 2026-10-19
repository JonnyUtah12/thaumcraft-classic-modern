package com.jonnyutah12.thaumcraftclassic;

import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.gametest.ConfigParityGameTest;
import com.jonnyutah12.thaumcraftclassic.gametest.ModNetworkGameTest;
import com.jonnyutah12.thaumcraftclassic.gametest.ModSmokeGameTest;
import com.jonnyutah12.thaumcraftclassic.gametest.RegistrationGameTest;
import com.jonnyutah12.thaumcraftclassic.gametest.SoundRegistryGameTest;
import com.jonnyutah12.thaumcraftclassic.network.ModNetwork;
import com.jonnyutah12.thaumcraftclassic.registration.ModRegistration;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
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
        // Configuration first: every later system reads options through
        // TcConfig's typed accessors (docs/config-reference.md).
        TcConfig.register(modContainer);

        // TC-M2-01 registration architecture: every DeferredRegister holder
        // (blocks, items, block entities, entities, data components, tabs,
        // particles, mob effects) is attached here, in dependency order,
        // before any RegisterEvent can fire. Id contract + harness:
        // docs/architecture/registration.md.
        ModRegistration.init(modEventBus);

        // TC-M7-05 sound-event registry: the 66 original TC sound events
        // (definitions + oggs arrive via the D-004 asset overlay; the id set
        // is pinned here and guarded by SoundRegistryParityTest/GameTest).
        ModSounds.register(modEventBus);

        // Networking payloads register on both dists before any connection
        // can be accepted (docs/architecture/networking.md).
        modEventBus.addListener(ModNetwork::register);

        modEventBus.addListener(ThaumcraftClassic::registerGameTests);

        LOGGER.info("Thaumcraft Classic (unofficial port of Thaumcraft 4.2.3.5) initializing; target MC {} / NeoForge",
                "1.21.1");
    }

    private static void registerGameTests(final RegisterGameTestsEvent event) {
        event.register(ModSmokeGameTest.class);
        event.register(ConfigParityGameTest.class);
        event.register(ModNetworkGameTest.class);
        event.register(RegistrationGameTest.class);
        event.register(SoundRegistryGameTest.class);
    }
}
