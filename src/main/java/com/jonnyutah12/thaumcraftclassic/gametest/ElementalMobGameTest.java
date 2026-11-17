package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityWisp;
import com.jonnyutah12.thaumcraftclassic.entities.monster.WispAiLogic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEntities;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    // ------------------------------------------------------------------ drop
    // The original's func_70628_a (EntityWisp.java:277-284) drops one
    // itemWispEssence stack carrying 2 units of the wisp's own aspect, and the
    // if at :278 gates the whole body on the type resolving in the aspect
    // registry. Both halves are proven in the world: a resolved wisp must drop
    // the labelled stack, and an unresolvable wisp must drop nothing.

    /** Kill the wisp the way a player does it: 1.21.1's
     * {@code dropAllDeathLoot} runs {@code dropCustomDeathLoot} only when the
     * damage source allows loot, so a player attack is the faithful seam. */
    private static void killAsPlayer(final GameTestHelper helper, final EntityWisp wisp) {
        final ServerLevel level = helper.getLevel();
        // javap 21.1.259: GameTestHelper.makeMockPlayer(GameType) returns Player
        // and the enum has SURVIVAL/CREATIVE/ADVENTURE/SPECTATOR/DEFAULT_MODE (no
        // GAME); damageSources().playerAttack takes a Player.
        final Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
        killer.setPos(wisp.getX(), wisp.getY(), wisp.getZ());
        wisp.hurt(level.damageSources().playerAttack(killer), 1000.0F);
        killer.discard();
    }

    private static java.util.List<ItemEntity> dropsNear(final GameTestHelper helper) {
        final AABB box = new AABB(
                Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(-2, -2, -2))),
                Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(6, 7, 6))));
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, box);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void wispDropsItsEssenceWithTwoUnitsOfItsOwnAspect(final GameTestHelper helper) {
        final int baseline = dropsNear(helper).size();
        final EntityWisp wisp = spawnWisp(helper);
        final String tag = WispAiLogic.EERIE_ASPECTS[0];
        wisp.setWispType(tag);
        killAsPlayer(helper, wisp);

        final java.util.List<ItemEntity> after = dropsNear(helper);
        helper.assertTrue(after.size() > baseline,
                "the wisp dropped nothing on death: the original's func_70628_a (:277-284) drops one "
                        + "wisp essence stack — " + after.size() + " item entities near the wisp, "
                        + baseline + " of them before the kill");

        ItemStack essence = ItemStack.EMPTY;
        for (final ItemEntity drop : after) {
            if (drop.getItem().is(CatalogResources.ITEM_WISP_ESSENCE.get())) {
                essence = drop.getItem();
            }
        }
        helper.assertTrue(!essence.isEmpty(),
                "the wisp's drop carries no wisp essence (one stack per death, :279)");
        helper.assertTrue(essence.getCount() == 1,
                "the original throws exactly one stack (:279-283), got " + essence.getCount());

        final AspectList aspects = essence.getItem() instanceof IEssentiaContainerItem container
                ? container.getAspects(essence) : null;
        helper.assertTrue(aspects != null && aspects.size() == 1,
                "the dropped essence carries no single aspect: the original labels it with "
                        + "the wisp's own aspect at :280-282");
        helper.assertTrue(aspects.getAmount(Aspect.getAspect(tag)) == WispAiLogic.ESSENCE_DROP,
                "the dropped essence does not carry 2 units of the wisp's aspect (:281)");
        helper.assertTrue(tag.equals(aspects.getAspects().get(0).getTag()),
                "the drop is labelled " + aspects.getAspects().get(0).getTag()
                        + ", not the wisp's own aspect " + tag);
        for (final ItemEntity drop : after) {
            drop.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void wispWithAnUnresolvableAspectDropsNoEssence(final GameTestHelper helper) {
        // The if at :278 (Aspect.getAspect(this.getType()) != null) is the gate:
        // a wisp whose stored type is not a registered aspect drops nothing.
        final EntityWisp wisp = spawnWisp(helper);
        helper.assertTrue(wisp.resolvedAspect() == null,
                "a fresh wisp must have no resolvable aspect for this probe to mean anything");
        killAsPlayer(helper, wisp);

        int essenceDrops = 0;
        for (final ItemEntity drop : dropsNear(helper)) {
            if (drop.getItem().is(CatalogResources.ITEM_WISP_ESSENCE.get())) {
                essenceDrops++;
            }
        }
        helper.assertTrue(essenceDrops == 0,
                "a wisp with no resolvable aspect dropped essence; :278 gates the drop");
        for (final ItemEntity drop : dropsNear(helper)) {
            drop.discard();
        }
        helper.succeed();
    }
}
