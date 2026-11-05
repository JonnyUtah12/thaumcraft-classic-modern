package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.api.damagesource.DamageSourcesThaumcraft;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * BD-20 acceptance: the eldritch damage types' armour/magic flags, and Liquid
 * Death's depth-aware contact damage — measured on a real entity in a real
 * level, not read off the shipped JSON.
 *
 * <p>The original registers {@code taint} as
 * {@code DamageSourceThaumcraft("taint").setDamageBypassesArmor().setMagicDamage()}
 * and {@code dissolve} with {@code setDamageBypassesArmor()}
 * ({@code thaumcraft/api/damagesource/DamageSourceThaumcraft.java:8,11}). 1.21
 * deleted the 1.20.x {@code BYPASSES_ARMOR} {@code DamageEffects} profiles, so
 * on 21.1.259 the meaning is a damage-type <em>tag</em>
 * ({@code LivingEntity.getDamageAfterArmorAbsorb} reads
 * {@code DamageTypeTags.BYPASSES_ARMOR}): {@code minecraft:bypasses_armor}
 * names both types and {@code neoforge:is_magic} names taint. Liquid Death
 * deals {@code world.getBlockMetadata(x, y, z) + 1} in the original
 * ({@code BlockFluidDeath.func_149670_a}, BlockFluidDeath.java:42) — 1 at the
 * source block, 8 at the shallowest flowing step — and this emitter must read
 * the fluid level of the cell it is called on.</p>
 *
 * <p>What makes these probes evidence rather than assertions: the armour
 * probes do NOT check that a damage number is large. Every one of them equips
 * the same full diamond set on the same kind of target and the control probe
 * first measures a <em>non-bypassing</em> vanilla source on it (a mob attack —
 * deliberately not {@code minecraft:generic}, which is itself in
 * {@code bypasses_armor} in 1.21 and would silently pass), so a harness that
 * failed to apply armour fails loudly there instead of passing the Thaumcraft
 * probes vacuously. The Liquid Death probes pin the original's two documented
 * endpoints (1 and 8); the constant this card removed (2.0F) fails both.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EldritchDamageGameTest {

    /** Diamond helmet 3 + chest 8 + legs 6 + boots 3. */
    private static final int DIAMOND_ARMOUR = 3 + 8 + 6 + 3;

    /** The probe's fixed base damage: below the zombie's 20 so nothing dies. */
    private static final float BASE_DAMAGE = 10.0F;

    private EldritchDamageGameTest() {
    }

    /**
     * The control: on the same diamond-armoured target, a plain vanilla source
     * that does not bypass armour IS reduced. If this probe passes, the harness
     * really is applying the armour, which is what gives the two Thaumcraft
     * probes below their meaning.
     */
    @GameTest(template = "empty")
    public static void vanillaDamageIsReducedByDiamondArmour(final GameTestHelper helper) {
        final Zombie target = armouredZombie(helper);
        final DamageSource vanilla = helper.getLevel().damageSources().mobAttack(spawnZombie(helper));
        helper.assertTrue(!vanilla.is(DamageTypeTags.BYPASSES_ARMOR),
                "the control source must be armour-respecting; minecraft:generic is in "
                + "bypasses_armor in 1.21 and is not a control");
        final float taken = hit(helper, target, vanilla, BASE_DAMAGE);
        helper.assertTrue(taken < BASE_DAMAGE - 1.0E-4F,
                "BD-20 control: a vanilla source must be reduced by the diamond set's "
                + target.getArmorValue() + " armour points, but the target took the full "
                + taken + " of " + BASE_DAMAGE + " — the harness is not applying armour, so the "
                + "taint/dissolve probes would prove nothing");
        helper.succeed();
    }

    /**
     * The original's {@code taint} was {@code setDamageBypassesArmor()}: the
     * same diamond set that reduced the control absorbs none of it.
     */
    @GameTest(template = "empty")
    public static void taintBypassesDiamondArmour(final GameTestHelper helper) {
        final Zombie target = armouredZombie(helper);
        final DamageSource taint = DamageSourcesThaumcraft.taint(helper.getLevel());
        helper.assertTrue(taint.is(DamageTypeTags.BYPASSES_ARMOR),
                "thaumcraft:taint must be named in minecraft:bypasses_armor");
        final float taken = hit(helper, target, taint, BASE_DAMAGE);
        helper.assertTrue(Math.abs(taken - BASE_DAMAGE) < 1.0E-4F,
                "thaumcraft:taint must bypass armour (the original's setDamageBypassesArmor): the "
                + target.getArmorValue() + " armour points absorbed " + (BASE_DAMAGE - taken)
                + " of " + BASE_DAMAGE + ", target took " + taken);
        helper.succeed();
    }

    /**
     * The original's {@code dissolve} was {@code setDamageBypassesArmor()} too
     * (and is NOT magic — it must only carry the one tag).
     */
    @GameTest(template = "empty")
    public static void dissolveBypassesDiamondArmour(final GameTestHelper helper) {
        final Zombie target = armouredZombie(helper);
        final DamageSource dissolve = DamageSourcesThaumcraft.dissolve(helper.getLevel());
        helper.assertTrue(dissolve.is(DamageTypeTags.BYPASSES_ARMOR),
                "thaumcraft:dissolve must be named in minecraft:bypasses_armor");
        helper.assertTrue(!dissolve.is(Tags.DamageTypes.IS_MAGIC),
                "the original's dissolve is setDamageBypassesArmor() only — it must not be magic");
        final float taken = hit(helper, target, dissolve, BASE_DAMAGE);
        helper.assertTrue(Math.abs(taken - BASE_DAMAGE) < 1.0E-4F,
                "thaumcraft:dissolve must bypass armour (the original's setDamageBypassesArmor): the "
                + target.getArmorValue() + " armour points absorbed " + (BASE_DAMAGE - taken)
                + " of " + BASE_DAMAGE + ", target took " + taken);
        helper.succeed();
    }

    /**
     * The original's {@code taint} was also {@code setMagicDamage()} — on
     * 21.1.259 that classification is the {@code neoforge:is_magic}
     * damage-type tag, and the two types must come out differently.
     */
    @GameTest(template = "empty")
    public static void taintClassifiesAsMagicAndDissolveDoesNot(final GameTestHelper helper) {
        helper.assertTrue(DamageSourcesThaumcraft.taint(helper.getLevel())
                        .is(Tags.DamageTypes.IS_MAGIC),
                "the original's taint is setMagicDamage(): thaumcraft:taint must be named in "
                + "neoforge:is_magic");
        helper.assertTrue(!DamageSourcesThaumcraft.dissolve(helper.getLevel())
                        .is(Tags.DamageTypes.IS_MAGIC),
                "the original's dissolve is not setMagicDamage(): it must not be in neoforge:is_magic");
        helper.succeed();
    }

    /**
     * Liquid Death over its source block: the original's
     * {@code world.getBlockMetadata(...) + 1} at metadata 0 is 1.0, where the
     * constant this card removed dealt 2.0.
     */
    @GameTest(template = "empty")
    public static void liquidDeathAtASourceBlockDealsOne(final GameTestHelper helper) {
        final BlockState source = CatalogEldritchRealm.BLOCK_FLUID_DEATH.get().defaultBlockState();
        helper.assertTrue(source.getFluidState().isSource(),
                "the default blockfluiddeath state must be the fluid source");
        final float taken = liquidDeathContact(helper, source);
        helper.assertTrue(Math.abs(taken - 1.0F) < 1.0E-4F,
                "a Liquid Death source block deals the original's metadata 0 + 1 = 1.0, but the "
                + "target took " + taken);
        helper.succeed();
    }

    /**
     * Liquid Death in flow: the original's metadata N + 1, at both ends of the
     * flowing range — level 1 deals 2.0, the shallowest step (level 7) deals
     * 8.0. The constant 2.0 that shipped dealt 2.0 at both.
     */
    @GameTest(template = "empty")
    public static void liquidDeathScalesWithTheFluidLevel(final GameTestHelper helper) {
        final BlockState levelOne = CatalogEldritchRealm.BLOCK_FLUID_DEATH.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, 1);
        helper.assertTrue(levelOne.getFluidState().getValue(FlowingFluid.LEVEL) == 7,
                "the block's level 1 must be the fluid state's level 7 (LiquidBlock builds "
                + "getFlowing(8 - level)), got " + levelOne.getFluidState().getValue(FlowingFluid.LEVEL));
        final float atLevelOne = liquidDeathContact(helper, levelOne);
        helper.assertTrue(Math.abs(atLevelOne - 2.0F) < 1.0E-4F,
                "Liquid Death at level 1 deals the original's metadata 1 + 1 = 2.0, but the target "
                + "took " + atLevelOne);

        final BlockState levelSeven = CatalogEldritchRealm.BLOCK_FLUID_DEATH.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, 7);
        helper.assertTrue(levelSeven.getFluidState().getValue(FlowingFluid.LEVEL) == 1,
                "the block's level 7 must be the shallowest flowing step (fluid state level 1), got "
                + levelSeven.getFluidState().getValue(FlowingFluid.LEVEL));
        final float atLevelSeven = liquidDeathContact(helper, levelSeven);
        helper.assertTrue(Math.abs(atLevelSeven - 8.0F) < 1.0E-4F,
                "the shallowest flowing step (level 7) deals the original's metadata 7 + 1 = 8.0, but "
                + "the target took " + atLevelSeven + " — a constant emitter cannot produce both this "
                + "and the source block's 1.0");
        helper.succeed();
    }

    // ------------------------------------------------------------------

    /**
     * A zombie in the full diamond set, with the equipment attribute modifiers
     * applied. 1.21.1 applies them in {@code LivingEntity.tick()} through
     * {@code detectEquipmentUpdates} (which is private), so the probe takes the
     * one tick the shipped entity takes — no attribute is hand-set.
     */
    private static Zombie armouredZombie(final GameTestHelper helper) {
        final Zombie target = spawnZombie(helper);
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        target.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        target.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        target.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        target.tick();
        helper.assertTrue(target.getArmorValue() >= DIAMOND_ARMOUR,
                "the full diamond set must give the target at least its " + DIAMOND_ARMOUR
                + " armour points, but the target has " + target.getArmorValue()
                + " — the armour modifiers were never applied, so this probe cannot test the bypass");
        return reset(target);
    }

    /** One contact event of the death fluid, driven through the block itself. */
    private static float liquidDeathContact(final GameTestHelper helper, final BlockState state) {
        final BlockPos cell = new BlockPos(0, 1, 0);
        final Zombie target = reset(spawnZombie(helper));
        final float before = target.getHealth();
        CatalogEldritchRealm.BLOCK_FLUID_DEATH.get()
                .entityInside(state, helper.getLevel(), helper.absolutePos(cell), target);
        final float taken = before - target.getHealth();
        helper.assertTrue(target.getHealth() > 0.0F,
                "the probe needs the target alive after the contact (health " + target.getHealth() + ")");
        return taken;
    }

    private static Zombie spawnZombie(final GameTestHelper helper) {
        final Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 1, 0));
        target.setNoAi(true);
        return reset(target);
    }

    private static Zombie reset(final Zombie target) {
        target.setHealth(target.getMaxHealth());
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        return target;
    }

    /** One hit, returning the health the target actually lost. */
    private static float hit(final GameTestHelper helper, final Zombie target,
                             final DamageSource source, final float amount) {
        final float before = target.getHealth();
        final boolean damaged = target.hurt(source, amount);
        final float taken = before - target.getHealth();
        helper.assertTrue(damaged, "the probe's hit did not land (" + source.getMsgId()
                + "): the target must take " + amount);
        helper.assertTrue(target.getHealth() > 0.0F,
                "the probe needs the target alive after the hit (health " + target.getHealth() + ")");
        reset(target);
        return taken;
    }
}
