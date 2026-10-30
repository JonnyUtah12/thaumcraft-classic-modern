package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityWisp;
import com.jonnyutah12.thaumcraftclassic.entities.monster.WispAiLogic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * World probes for the elemental mob family (TC-M6-06-s). These prove the
 * original {@code EntityWisp} is not merely compilable: the entity type is in
 * the live registry with the original's hitbox and attributes, an instance
 * spawns alive in a real level and installs no goal, and the aspect type
 * round-trips through the original's {@code "Type"} save key. The pure rules
 * behind them are pinned world-free by {@code WispAiLogicTest}.
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ElementalMobGameTest {

    private ElementalMobGameTest() {
    }

    private static EntityWisp spawnWisp(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityWisp wisp = new EntityWisp(CatalogEntities.WISP.get(), level);
        wisp.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        level.addFreshEntity(wisp);
        return wisp;
    }

    @GameTest(template = "empty")
    public static void wispIsRegisteredWithTheOriginalHitboxAndAttributes(final GameTestHelper helper) {
        final EntityType<?> registered = BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "wisp"));
        helper.assertTrue(registered != null, "thaumcraft:wisp is not in the entity-type registry");
        helper.assertTrue("thaumcraft:wisp".equals(
                        BuiltInRegistries.ENTITY_TYPE.getKey(registered).toString()),
                "the wisp registered under the wrong id");
        helper.assertTrue(registered.getDimensions().width() == WispAiLogic.WIDTH
                        && registered.getDimensions().height() == WispAiLogic.HEIGHT,
                "the wisp hitbox is not the original 0.9 x 0.9");

        final EntityWisp wisp = new EntityWisp(CatalogEntities.WISP.get(), helper.getLevel());
        helper.assertTrue(wisp.getAttributeValue(Attributes.MAX_HEALTH) == WispAiLogic.MAX_HEALTH,
                "wisp health is not the original 22.0");
        helper.assertTrue(wisp.getAttributeValue(Attributes.ATTACK_DAMAGE) == WispAiLogic.ATTACK_DAMAGE,
                "wisp attack damage is not the original 3.0");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wispSpawnsAliveAndInstallsNoGoal(final GameTestHelper helper) {
        final EntityWisp wisp = spawnWisp(helper);
        helper.assertTrue(wisp.isAlive(), "the spawned wisp is not alive");
        helper.assertTrue(wisp.getType() == CatalogEntities.WISP.get(),
                "the spawned wisp is not the registered entity type");
        // The original installs no goal and no navigation class at all: its whole
        // AI is hand-rolled. A non-empty goal selector means someone modernised it.
        helper.assertTrue(wisp.goalSelector.getAvailableGoals().isEmpty(),
                "the wisp must install no goal — the original's AI is hand-rolled");
        helper.assertTrue(!wisp.isPushable(), "the original's canBePushed is false");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wispAspectTypeRoundTripsThroughTheOriginalSaveKey(final GameTestHelper helper) {
        final EntityWisp wisp = spawnWisp(helper);
        helper.assertTrue(wisp.resolvedAspect() == null,
                "a fresh wisp should carry no resolvable aspect before its first AI tick");

        final String tag = WispAiLogic.EERIE_ASPECTS[0];
        wisp.setWispType(tag);
        helper.assertTrue(tag.equals(wisp.getWispType()), "setWispType did not stick");

        final CompoundTag saved = new CompoundTag();
        wisp.addAdditionalSaveData(saved);
        helper.assertTrue(tag.equals(saved.getString("Type")),
                "the original writes the aspect to NBT under \"Type\"");

        final EntityWisp loaded = new EntityWisp(CatalogEntities.WISP.get(), helper.getLevel());
        loaded.readAdditionalSaveData(saved);
        helper.assertTrue(tag.equals(loaded.getWispType()),
                "the aspect tag did not survive a save/load round trip");
        helper.succeed();
    }
}
