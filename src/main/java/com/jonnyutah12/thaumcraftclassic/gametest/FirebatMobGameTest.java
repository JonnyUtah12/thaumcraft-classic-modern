package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * World probes for the firebat (TC-M6-06-s-c1, content-mapping
 * {@code Thaumcraft:Firebat}, mod_index 22).
 *
 * <p>Every probe reaches the mob through the <b>live registry</b> — an id
 * string, {@code BuiltInRegistries.ENTITY_TYPE} and the vanilla {@code Mob} /
 * {@code LivingEntity} API — and every figure below is the original's literal,
 * so the suite compiles against a tree with no firebat class at all and the
 * probes go RED on behaviour, not on a missing symbol. That is what makes them
 * a gate: an unregistered id, the wrong hitbox, the wrong attributes, a goal
 * the original never installed, fire or magic damage that lands, pushability,
 * or a save compound that renamed the original's {@code "BatFlags"} /
 * {@code "damBonus"} keys each fail a probe here.</p>
 *
 * <p>The literals are read from {@code thaumcraft.common.entities.monster.
 * EntityFireBat} (404 lines) and each assert quotes the line; the same figures
 * exist as named constants on {@code FirebatAiLogic}, pinned world-free by
 * {@code FirebatAiLogicTest}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FirebatMobGameTest {

    private FirebatMobGameTest() {
    }

    /** The registered firebat type. The id itself is the first contract: an
     * absent id resolves to the registry's default entry, so the key — not a
     * null check — is what proves registration. */
    private static EntityType<?> registeredFirebat(final GameTestHelper helper) {
        final ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "firebat");
        final EntityType<?> registered = BuiltInRegistries.ENTITY_TYPE.get(id);
        helper.assertTrue(registered != null, "thaumcraft:firebat is not in the entity-type registry");
        helper.assertTrue(id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(registered)),
                "thaumcraft:firebat is not in the entity-type registry (key reads "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(registered) + ")");
        return registered;
    }

    private static Mob spawnFirebat(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registeredFirebat(helper).create(level);
        helper.assertTrue(spawned instanceof Mob, "thaumcraft:firebat did not create a Mob instance");
        final Mob bat = (Mob) spawned;
        final BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
        bat.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(bat);
        return bat;
    }

    // ------------------------------------------------------------- registration

    @GameTest(template = "empty")
    public static void firebatIsRegisteredWithTheOriginalHitbox(final GameTestHelper helper) {
        // func_70105_a(0.5F, 0.9F) — EntityFireBat.java:30
        final EntityType<?> registered = registeredFirebat(helper);
        helper.assertTrue(registered.getDimensions().width() == 0.5F
                        && registered.getDimensions().height() == 0.9F,
                "the firebat hitbox is not the original 0.5 x 0.9 but "
                        + registered.getDimensions().width() + " x "
                        + registered.getDimensions().height() + " (:30)");
        // field_70178_ae = true — :32 (isImmuneToFire) -> Builder.fireImmune()
        helper.assertTrue(registered.fireImmune(), "the original sets isImmuneToFire at :32");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void firebatSpawnsWithTheOriginalAttributes(final GameTestHelper helper) {
        final Mob bat = spawnFirebat(helper);
        // func_110147_ax:75-76 — health 5.0 (the 15.0 is the devil bit, applied
        // at runtime), attack 1.0 for a non-summoned bat.
        helper.assertTrue(bat.getAttributeValue(Attributes.MAX_HEALTH) == 5.0,
                "firebat health is not the original 5.0 but "
                        + bat.getAttributeValue(Attributes.MAX_HEALTH) + " (:75)");
        helper.assertTrue(bat.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1.0,
                "firebat attack damage is not the original 1.0 but "
                        + bat.getAttributeValue(Attributes.ATTACK_DAMAGE) + " (:76)");
        helper.assertTrue(bat.isAlive(), "the spawned firebat is not alive");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void firebatInstallsNoGoalAndIsNotPushable(final GameTestHelper helper) {
        final Mob bat = spawnFirebat(helper);
        // func_70814_o() (1.7.10 isAIEnabled) is TRUE at :401-403 and the class
        // still installs no goal and no navigation class — the flight body is
        // written inside func_70626_be (:223-297). A non-empty selector means
        // someone modernised the AI.
        helper.assertTrue(bat.goalSelector.getAvailableGoals().isEmpty(),
                "the firebat must install no goal — the original's AI is hand-rolled (:401-403)");
        // func_70104_M() false (:69-71) AND func_70041_e_() false (:303-305),
        // both resolved to isPushable() in this port.
        helper.assertTrue(!bat.isPushable(), "the firebat must not be pushable (:69-71, :303-305)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void firebatRefusesFireAndMagicDamage(final GameTestHelper helper) {
        final Mob bat = spawnFirebat(helper);
        final ServerLevel level = helper.getLevel();
        // func_70097_a (:317-327) refuses damage while the bat is dead
        // (func_85032_ar()), from fire (func_76347_k() = isFireDamage) and from
        // magic (func_94541_c() = isMagicDamage).
        helper.assertTrue(!bat.hurt(level.damageSources().inFire(), 1.0F),
                "a firebat takes fire damage; the original refuses it at :317-327");
        helper.assertTrue(!bat.hurt(level.damageSources().magic(), 1.0F),
                "a firebat takes magic damage; the original refuses it at :317-327");
        helper.assertTrue(bat.isAlive() && bat.getHealth() == 5.0F,
                "the refused damage still took health off the bat: " + bat.getHealth());
        // A plain hit must still land — :317-327 is a refusal list, not blanket
        // invulnerability.
        helper.assertTrue(bat.hurt(level.damageSources().generic(), 1.0F),
                "a generic hit must land; :317-327 only refuses fire and magic");
        helper.assertTrue(bat.getHealth() == 4.0F,
                "the landed hit did not deal the original's damage: " + bat.getHealth());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void firebatSavesTheOriginalFlagByteAndDamageBonusKeys(final GameTestHelper helper) {
        final Mob bat = spawnFirebat(helper);
        // func_70014_b (:382-386) writes the synched flag byte under "BatFlags"
        // and the summoner's bonus under "damBonus"; func_70037_a (:376-380)
        // reads the same two keys back. A renamed key silently drops every
        // summon/explosive/devil/vampire bat an existing save carries.
        final CompoundTag saved = bat.saveWithoutId(new CompoundTag());
        helper.assertTrue(saved.contains("BatFlags"),
                "the firebat does not save the original's \"BatFlags\" key (:384)");
        helper.assertTrue(saved.contains("damBonus"),
                "the firebat does not save the original's \"damBonus\" key (:385)");
        helper.succeed();
    }
}
