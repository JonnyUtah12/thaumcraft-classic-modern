package com.jonnyutah12.thaumcraftclassic.sound;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Sound-event registry: every sound event defined by the original
 * Thaumcraft 4.2.3.5 {@code assets/thaumcraft/sounds.json} (66 events over
 * 111 ogg files, docs/original-asset-inventory.md), registered under the
 * {@code thaumcraft} namespace with the original event ids.
 *
 * <p>The ogg files and the 1.21.1-converted {@code sounds.json} are All
 * Rights Reserved and are NOT committed; the build overlays them from the
 * local reference tree (docs/porting-decisions.md D-004, build.gradle
 * {@code overlayOriginalAssets}). This class is the committed code half of
 * that pair: it pins the event-id set so tests can assert registry ↔
 * sounds.json ↔ asset-mapping.csv parity even on CI without the assets.</p>
 *
 * <p>Conversion records (full per-event table: docs/sound-events.md):</p>
 * <ul>
 *   <li>Two original event ids contain uppercase letters, which 1.21.1
 *       resource ids reject ({@code [a-z0-9/._-]} only); they are renamed:
 *       {@code runicShieldCharge -> runic_shield_charge},
 *       {@code runicShieldEffect -> runic_shield_effect} (their oggs are
 *       renamed by the overlay; docs/asset-mapping.csv records both).</li>
 *   <li>The original registers every event under the 1.7.10 {@code master}
 *       category; 1.21.1 picks the category at the play site, so play sites
 *       must pass {@code SoundSource.MASTER} to stay faithful. The
 *       per-event {@code category} field is dropped in the converted
 *       sounds.json.</li>
 *   <li>{@code urnbreak} is registered for parity with the original
 *       sounds.json but is never played there either: the original step-sound
 *       name resolves to the vanilla {@code dig.*} namespace, so the event is
 *       a dead definition (docs/original-asset-inventory.md marks the ogg
 *       unused). No trigger site is wired.</li>
 *   <li>Ranges: 1.7.10 sounds use the fixed 16-block default;
 *       {@code createVariableRangeEvent} reproduces that default.</li>
 * </ul>
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, ThaumcraftClassic.MOD_ID);

    private static DeferredHolder<SoundEvent, SoundEvent> register(final String id) {
        return SOUND_EVENTS.register(id,
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, id)));
    }

    // --- machines / devices -------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> ALEMBICKNOCK = register("alembicknock");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUBBLE = register("bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAFTFAIL = register("craftfail");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAFTSTART = register("craftstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> CREAK = register("creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOORFAIL = register("doorfail");
    public static final DeferredHolder<SoundEvent, SoundEvent> EVILPORTAL = register("evilportal");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRELOOP = register("fireloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> INFUSER = register("infuser");
    public static final DeferredHolder<SoundEvent, SoundEvent> INFUSERSTART = register("infuserstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> PUMP = register("pump");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUMBLE = register("rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPILL = register("spill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUEEK = register("squeek");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOOL = register("tool");
    public static final DeferredHolder<SoundEvent, SoundEvent> UPGRADE = register("upgrade");

    // --- wand casting / focus tools -----------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> CAMERATICKS = register("cameraticks");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICE = register("ice");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOCK = register("shock");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWING = register("swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAND = register("wand");
    public static final DeferredHolder<SoundEvent, SoundEvent> WANDFAIL = register("wandfail");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND = register("wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZAP = register("zap");

    // --- research / GUI ------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> CAMERACLACK = register("cameraclack");
    public static final DeferredHolder<SoundEvent, SoundEvent> ERASE = register("erase");
    public static final DeferredHolder<SoundEvent, SoundEvent> HHOFF = register("hhoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> HHON = register("hhon");
    public static final DeferredHolder<SoundEvent, SoundEvent> KEY = register("key");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEARN = register("learn");
    public static final DeferredHolder<SoundEvent, SoundEvent> PAGE = register("page");
    public static final DeferredHolder<SoundEvent, SoundEvent> WRITE = register("write");

    // --- items ---------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> BRAIN = register("brain");
    public static final DeferredHolder<SoundEvent, SoundEvent> COINS = register("coins");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL = register("crystal");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT = register("heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAR = register("jar");
    public static final DeferredHolder<SoundEvent, SoundEvent> JACOBS = register("jacobs");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPERS = register("whispers");

    // --- taint / world ---------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> GORE = register("gore");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROOTS = register("roots");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWARM = register("swarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWARMATTACK = register("swarmattack");
    public static final DeferredHolder<SoundEvent, SoundEvent> TENTACLE = register("tentacle");
    public static final DeferredHolder<SoundEvent, SoundEvent> URNBREAK = register("urnbreak");

    // --- runic armor (renamed ids, see class javadoc) --------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNIC_SHIELD_CHARGE = register("runic_shield_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNIC_SHIELD_EFFECT = register("runic_shield_effect");

    // --- entities ---------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> CHANT = register("chant");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRABCLAW = register("crabclaw");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRABDEATH = register("crabdeath");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRABTALK = register("crabtalk");
    public static final DeferredHolder<SoundEvent, SoundEvent> EGATTACK = register("egattack");
    public static final DeferredHolder<SoundEvent, SoundEvent> EGDEATH = register("egdeath");
    public static final DeferredHolder<SoundEvent, SoundEvent> EGIDLE = register("egidle");
    public static final DeferredHolder<SoundEvent, SoundEvent> EGSCREECH = register("egscreech");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLY = register("fly");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEMIRONSHOOT = register("golemironshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> MONOLITH = register("monolith");
    public static final DeferredHolder<SoundEvent, SoundEvent> PECH_CHARGE = register("pech_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> PECH_DEATH = register("pech_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> PECH_DICE = register("pech_dice");
    public static final DeferredHolder<SoundEvent, SoundEvent> PECH_HIT = register("pech_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PECH_IDLE = register("pech_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PECH_TRADE = register("pech_trade");
    public static final DeferredHolder<SoundEvent, SoundEvent> WISPDEAD = register("wispdead");
    public static final DeferredHolder<SoundEvent, SoundEvent> WISPLIVE = register("wisplive");

    /** All 66 original sound events, in registration order. */
    private static final List<DeferredHolder<SoundEvent, SoundEvent>> ALL = List.of(
            ALEMBICKNOCK, BRAIN, BUBBLE, CAMERACLACK, CAMERATICKS, CHANT, COINS, CRABCLAW, CRABDEATH,
            CRABTALK, CRAFTFAIL, CRAFTSTART, CREAK, CRYSTAL, DOORFAIL, EGATTACK, EGDEATH, EGIDLE,
            EGSCREECH, ERASE, EVILPORTAL, FIRELOOP, FLY, GOLEMIRONSHOOT, GORE, HEARTBEAT, HHOFF, HHON,
            ICE, INFUSER, INFUSERSTART, JACOBS, JAR, KEY, LEARN, MONOLITH, PAGE, PECH_CHARGE, PECH_DEATH,
            PECH_DICE, PECH_HIT, PECH_IDLE, PECH_TRADE, PUMP, ROOTS, RUMBLE, RUNIC_SHIELD_CHARGE,
            RUNIC_SHIELD_EFFECT, SHOCK, SPILL, SQUEEK, SWARM, SWARMATTACK, SWING, TENTACLE, TOOL,
            UPGRADE, URNBREAK, WAND, WANDFAIL, WHISPERS, WIND, WISPDEAD, WISPLIVE, WRITE, ZAP);

    private ModSounds() {
    }

    /** The exact event-id set this registry defines — the parity contract for tests. */
    public static List<ResourceLocation> eventIds() {
        return ALL.stream().map(holder -> holder.getKey().location()).toList();
    }

    public static void register(final IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
