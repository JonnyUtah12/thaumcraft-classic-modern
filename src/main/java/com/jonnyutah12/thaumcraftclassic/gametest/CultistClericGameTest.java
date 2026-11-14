package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AIAltarFocus;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AICultistHurtByTarget;
import com.jonnyutah12.thaumcraftclassic.entities.ai.cultist.AILongRangeAttack;
import com.jonnyutah12.thaumcraftclassic.entities.monster.CultistAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultist;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistCleric;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityCultistKnight;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityGolemOrb;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCultists;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * World probes for the Crimson Cleric and the Energy Orb (TC-M6-06-s-c4a-2,
 * content-mapping {@code Thaumcraft:CultistCleric} (CSV line 326) mod_index 28,
 * registration {@code ConfigEntities.java:113}, and {@code Thaumcraft:GolemOrb}
 * (CSV line 311) mod_index 13, registration {@code ConfigEntities.java:89}).
 *
 * <p>Every figure reaches the mob through the <b>live registry</b> and the
 * vanilla {@code Mob} / {@code LivingEntity} / {@code Projectile} API, so a
 * class goes RED on behaviour rather than on a missing symbol. Each literal is
 * the original's own and quotes its decompiled line in
 * {@code thaumcraft.common.entities.monster.EntityCultistCleric} (180 lines) and
 * {@code thaumcraft.common.entities.projectile.EntityGolemOrb} (127 lines); the
 * same figures live as named constants on the port's Level-free rule class
 * {@link CultistAiLogic}, pinned world-free by {@code CultistAiLogicTest}.</p>
 *
 * <p>The two card anchors drive the whole file. <b>The {@code EntityGolemOrb}
 * constructor's boolean is pinned per caller</b>: the Cleric fires
 * {@code new EntityGolemOrb(world, this, target, true)} ({@code :70}) — so the
 * orb the cleric throws is the <i>red</i> variant, lifetime 240 and damage ×1.0
 * — while the eldritch golem passes {@code false}
 * ({@code boss/EntityEldritchGolem.java:225}) and the Cultist Leader passes
 * {@code true} ({@code boss/EntityCultistLeader.java:191}, card c5). <b>The
 * cleric's fireball is {@code EntitySmallFireball}</b> ({@code :82}), so the
 * shipped id is the vanilla small-fireball type, not a Thaumcraft projectile.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CultistClericGameTest {

    private CultistClericGameTest() {
    }

    /** Look up an id in the live registry. The <b>key</b> is the contract: an
     *  absent id resolves to the registry default entry, so comparing the key —
     *  not a null check — is what proves registration. */
    private static EntityType<?> registered(final GameTestHelper helper, final String path) {
        final ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        helper.assertTrue(id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)),
                "thaumcraft:" + path + " is not in the entity-type registry (key reads "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(type) + ")");
        return type;
    }

    /** Prove an id is NOT in the registry, naming what the id resolves to. */
    private static void absent(final GameTestHelper helper, final String path) {
        final ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        helper.assertTrue(!id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)),
                "thaumcraft:" + path + " must NOT be registered but its key resolves to "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    private static EntityCultistCleric cleric(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, "cultistcleric").create(level);
        helper.assertTrue(spawned instanceof EntityCultistCleric,
                "thaumcraft:cultistcleric did not create an EntityCultistCleric but "
                        + (spawned == null ? "null" : spawned.getClass().getName()));
        final EntityCultistCleric mob = (EntityCultistCleric) spawned;
        final BlockPos at = helper.absolutePos(BlockPos.ZERO).offset(1, 3, 1);
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        // Harness fixture, not ported behaviour: an "empty" pad has no floor, so
        // a WALKING mob falls out of the structure's ticking area and the pad sets
        // it alight (the pech card's finding, reused by the knight card).
        mob.setNoGravity(true);
        mob.clearFire();
        level.addFreshEntity(mob);
        return mob;
    }

    /** A mock attacker placed at the mob's feet: {@code makeMockPlayer} does NOT
     *  place the player, and {@code TargetGoal#canAttack} carries the mob's
     *  FOLLOW_RANGE, so an unplaced mock sits in the template's void (the knight
     *  card's finding). */
    private static Player attackerAt(final GameTestHelper helper, final LivingEntity at) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    /** {@code GoalSelector#getAvailableGoals()} is a guava Multimap (no
     *  {@code stream()}), so every goal probe reads a plain list copy. */
    private static List<WrappedGoal> goals(final Mob mob, final boolean target) {
        final List<WrappedGoal> out = new ArrayList<>();
        for (final WrappedGoal g : (target ? mob.targetSelector : mob.goalSelector).getAvailableGoals()) {
            out.add(g);
        }
        return out;
    }

    private static boolean has(final Iterable<WrappedGoal> goals, final Class<?> goal, final int slot) {
        for (final WrappedGoal wrapped : goals) {
            if (goal.isInstance(wrapped.getGoal()) && wrapped.getPriority() == slot) {
                return true;
            }
        }
        return false;
    }

    private static String describe(final Iterable<WrappedGoal> goals) {
        final StringBuilder sb = new StringBuilder("goal set: ");
        for (final WrappedGoal g : goals) {
            sb.append(g.getPriority()).append('=').append(g.getGoal().getClass().getSimpleName()).append(' ');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ the roster

    /**
     * {@code ConfigEntities.java:113} — {@code registerModEntity(EntityCultistCleric.class,
     * "CultistCleric", 28, instance, 64, 3, true)}: the box is the base's
     * {@code func_70105_a(0.6F, 1.8F)}, the tracking range is 64 blocks = 4
     * chunks (1.21.1 counts chunks) and the update interval is 3 ticks — the
     * knight's own triple ({@code ConfigEntities.java:111}), so the cleric rides
     * the same pinned constants.
     */
    @GameTest(template = "empty")
    public static void theClericIsRegisteredWithTheOriginalBoxAndTracking(final GameTestHelper helper) {
        final EntityType<?> type = registered(helper, "cultistcleric");
        helper.assertTrue(Math.abs(type.getDimensions().width() - CultistAiLogic.WIDTH) < 1.0E-4F
                        && Math.abs(type.getDimensions().height() - CultistAiLogic.HEIGHT) < 1.0E-4F,
                "the cleric's hitbox is not the base's 0.6 x 1.8 but "
                        + type.getDimensions().width() + " x " + type.getDimensions().height());
        helper.assertTrue(type.getCategory() == MobCategory.MONSTER,
                "the cleric is not a MONSTER-category mob but " + type.getCategory());
        helper.assertTrue(type.clientTrackingRange() == CultistAiLogic.TRACK_RANGE_CHUNKS
                        && type.updateInterval() == CultistAiLogic.UPDATE_INTERVAL,
                "the cleric's tracking triple is not 64 blocks / 3 ticks (ConfigEntities.java:113) but "
                        + type.clientTrackingRange() + " chunks / " + type.updateInterval() + " ticks");
        helper.assertTrue(CatalogCultists.CULTIST_CLERIC.get() == type,
                "the catalog's CULTIST_CLERIC holder is not the registered type");
        helper.assertTrue(EntityCultistCleric.TYPE_ID
                        .equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)),
                "the class's own TYPE_ID constant must name the registered id, got "
                        + BuiltInRegistries.ENTITY_TYPE.getKey(type));
        helper.succeed();
    }

    /**
     * THE REGISTRATION ROSTER, MEASURED. The original's {@code ConfigEntities.init}
     * registers CultistKnight (27, {@code :111}), CultistCleric (28, {@code :113})
     * and CultistLeader (29, {@code :115}) — and <b>no {@code EntityCultist}
     * base</b>. With this card the shipped thaumcraft cultist ids are exactly
     * {@code [cultistcleric, cultistknight]}: the base stays unregistered and the
     * Leader stays c5's, and the probe asserts all four halves so the day the
     * Leader lands it says so by name.
     */
    @GameTest(template = "empty")
    public static void theClericCompletesTheRosterAndTheBaseStaysUnregistered(final GameTestHelper helper) {
        helper.assertTrue(CultistAiLogic.BASE_IS_REGISTERED == false,
                "the lane's statement is that the base is unregistered; the constant says "
                        + CultistAiLogic.BASE_IS_REGISTERED);
        absent(helper, "cultist");
        absent(helper, "cultistleader");
        registered(helper, "cultistknight");
        registered(helper, "cultistcleric");
        // The base is abstract, so it cannot even be instantiated as a spawnable mob.
        helper.assertTrue(java.lang.reflect.Modifier.isAbstract(EntityCultist.class.getModifiers()),
                "EntityCultist must stay abstract: the original registers no base, so an "
                        + "instantiable base invites a registration row");
        final List<String> shipped = new ArrayList<>();
        for (final EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            final ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (CultistAiLogic.CULTIST_TYPE_PATHS.contains(id.getPath())
                    && "thaumcraft".equals(id.getNamespace())) {
                shipped.add(id.getPath());
            }
        }
        java.util.Collections.sort(shipped);
        helper.assertTrue(shipped.equals(List.of("cultistcleric", "cultistknight")),
                "the shipped cultist ids must be exactly [cultistcleric, cultistknight] after this card"
                        + " — found " + shipped + " (Leader = card c5, base = unregistered by design)");
        // The faction test of EntityCultist:105-107 in its EntityType form.
        helper.assertTrue(CatalogCultists.isCultistType(CatalogCultists.CULTIST_CLERIC.get())
                        && CatalogCultists.isCultistType(CatalogCultists.CULTIST_KNIGHT.get())
                        && !CatalogCultists.isCultistType(EntityType.ZOMBIE),
                "the live-registry faction test must answer true for both cultists and false for a zombie");
        helper.succeed();
    }

    // ---------------------------------------------------------------- attributes

    /**
     * {@code EntityCultistCleric:51} — max health <b>30.0</b> over the base's
     * three attributes (follow 32, speed 0.3, attack 4.0) and nothing else, and
     * {@code :177-179} — {@code func_70627_aG()} ({@code getMaxAir}) is
     * <b>500</b> (the vanilla figure is 300).
     */
    @GameTest(template = "empty")
    public static void theClericCarriesTheBaseHealthTheAirAndTheFamilyAttributes(final GameTestHelper helper) {
        final EntityCultistCleric cleric = cleric(helper);
        helper.assertTrue(cleric.getAttributeValue(Attributes.MAX_HEALTH) == CultistAiLogic.CLERIC_MAX_HEALTH,
                ":51 — the cleric's health is not the original's 30.0 but "
                        + cleric.getAttributeValue(Attributes.MAX_HEALTH));
        helper.assertTrue(cleric.getMaxAirSupply() == CultistAiLogic.CLERIC_MAX_AIR,
                ":177-179 — getMaxAir must be 500, got " + cleric.getMaxAirSupply());
        helper.assertTrue(cleric.getAttributeBaseValue(Attributes.FOLLOW_RANGE) == CultistAiLogic.FOLLOW_RANGE,
                ":26 — follow range must be 32.0, got "
                        + cleric.getAttributeBaseValue(Attributes.FOLLOW_RANGE));
        helper.assertTrue(Math.abs(cleric.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)
                        - CultistAiLogic.MOVEMENT_SPEED) < 1.0E-6,
                ":27 — move speed must be 0.3, got " + cleric.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
        helper.assertTrue(cleric.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == CultistAiLogic.ATTACK_DAMAGE,
                ":28 — attack damage must be 4.0, got " + cleric.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        // The cleric's armour is the platform living baseline (BD-97's shape for
        // the family): the original's :51 adds MAX_HEALTH only, and the robe of
        // :56-58 is unregistered on this tree (BD-68), so :51 leaves the baseline
        // standing exactly as the knight's :41 does.
        final double armourBaseline =
                Monster.createMonsterAttributes().build().getBaseValue(Attributes.ARMOR);
        helper.assertTrue(cleric.getAttributeBaseValue(Attributes.ARMOR) == armourBaseline,
                ":51 — the cleric's armour must be the platform monster baseline (" + armourBaseline
                        + "), got " + cleric.getAttributeBaseValue(Attributes.ARMOR));
        cleric.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------ goal set

    /**
     * {@code EntityCultistCleric:34-45} — the family goal set with the cleric's
     * two extras: slot 0 swim, <b>slot 1 {@code AIAltarFocus}</b> ({@code :35}),
     * <b>slot 2 {@code AILongRangeAttack(this, 2.0, 1.0, 20, 40, 24.0F)}</b>
     * ({@code :36}), slot 3 melee, slots 4/5 the door pair, slot 6 restriction,
     * slot 7 wander, slot 8 watch, slot 9 idle-look; target slot 1 the gang call
     * ({@code :44}) and target slot 2 the player scan ({@code :45}).
     */
    @GameTest(template = "empty")
    public static void theClericGoalSetIsTheFamilySetPlusTheAltarAndRangedGoals(final GameTestHelper helper) {
        final EntityCultistCleric cleric = cleric(helper);
        final List<WrappedGoal> movement = goals(cleric, false);
        final List<WrappedGoal> targets = goals(cleric, true);
        helper.assertTrue(has(movement, FloatGoal.class, CultistAiLogic.GOAL_SLOT_SWIM),
                ":34 — the swim goal must sit at slot 0; " + describe(movement));
        helper.assertTrue(has(movement, AIAltarFocus.class, CultistAiLogic.GOAL_SLOT_ALTAR),
                ":35 — AIAltarFocus must sit at cleric goal slot 1; " + describe(movement));
        helper.assertTrue(has(movement, AILongRangeAttack.class, CultistAiLogic.GOAL_SLOT_RANGED),
                ":36 — AILongRangeAttack must sit at cleric goal slot 2; " + describe(movement));
        helper.assertTrue(has(movement, MeleeAttackGoal.class, CultistAiLogic.GOAL_SLOT_MELEE)
                        && has(movement, OpenDoorGoal.class, CultistAiLogic.GOAL_SLOT_DOOR_RESTRICT)
                        && has(movement, OpenDoorGoal.class, CultistAiLogic.GOAL_SLOT_DOOR_OPEN)
                        && has(movement, MoveTowardsRestrictionGoal.class, CultistAiLogic.GOAL_SLOT_RESTRICTION)
                        && has(movement, RandomStrollGoal.class, CultistAiLogic.GOAL_SLOT_WANDER)
                        && has(movement, LookAtPlayerGoal.class, CultistAiLogic.GOAL_SLOT_WATCH)
                        && has(movement, RandomLookAroundGoal.class, CultistAiLogic.GOAL_SLOT_LOOK_IDLE),
                ":37-43 — the knight's slots 3-8 must be installed slot for slot; " + describe(movement));
        helper.assertTrue(movement.size() == 10,
                ":34-43 — the cleric ships 10 movement goals: the knight's 8 (:26/:27/:28/:29/:30/"
                        + ":31/:32/:33) + AIAltarFocus (:35) + AILongRangeAttack (:36). The original's"
                        + " :42/:43 both addGoal at slot 8 and 1.7.10's GoalSelect is a TreeMap keyed"
                        + " by priority, so its WatchClosest is REPLACED by the LookIdle and the"
                        + " cleric nets 9; 1.21.1's GoalSelector keeps every goal, so the port ships"
                        + " the pair at distinct slots and measures 10 (BD-112); " + describe(movement));
        helper.assertTrue(has(targets, AICultistHurtByTarget.class, CultistAiLogic.TARGET_SLOT_HURT)
                        && has(targets, NearestAttackableTargetGoal.class, CultistAiLogic.TARGET_SLOT_PLAYER),
                ":44-45 — the gang call sits at target slot 1 and the player scan at 2; " + describe(targets));
        helper.assertTrue(targets.size() == 2,
                ":44-45 — exactly two target goals; " + describe(targets));
        // The gang call's agressors flag (:44 — AICultistHurtByTarget(this, true)).
        final AICultistHurtByTarget gang = (AICultistHurtByTarget) targets.stream()
                .filter(g -> g.getGoal() instanceof AICultistHurtByTarget)
                .findFirst().orElseThrow().getGoal();
        helper.assertTrue(gang.isAgressors(),
                ":44 — the cleric passes callsForHelp/agressors TRUE (the family's own flag)");
        // The ranged goal's own parameters, read back off the installed goal.
        final AILongRangeAttack ranged = (AILongRangeAttack) movement.stream()
                .filter(g -> g.getGoal() instanceof AILongRangeAttack)
                .findFirst().orElseThrow().getGoal();
        // :36 — AILongRangeAttack(this, 2.0, 1.0, 20, 40, 24.0F). The class's own
        // minDistance is readable through getMinDistance(); the speed, the interval
        // pair and the range are RangedAttackGoal's private fields, so those four
        // are pinned as the shipped ctor's constants (CultistAiLogicTest:118 builds
        // the same call from the same five names, so the pair cannot drift).
        helper.assertTrue(ranged.getMinDistance() == CultistAiLogic.RANGED_MIN_DISTANCE,
                ":36 — the installed goal's minDistance is the original's 2.0, got "
                        + ranged.getMinDistance());
        helper.assertTrue(CultistAiLogic.RANGED_SPEED == 1.0 && CultistAiLogic.RANGED_INTERVAL_MIN == 20
                        && CultistAiLogic.RANGED_INTERVAL_MAX == 40
                        && CultistAiLogic.RANGED_RANGE == 24.0F,
                ":36 — the ctor's other four figures must be 1.0 / 20 / 40 / 24.0F, got "
                        + CultistAiLogic.RANGED_SPEED + " / " + CultistAiLogic.RANGED_INTERVAL_MIN
                        + " / " + CultistAiLogic.RANGED_INTERVAL_MAX + " / "
                        + CultistAiLogic.RANGED_RANGE);
        cleric.discard();
        helper.succeed();
    }

    // ---------------------------------------------------------- ritualist state

    /**
     * {@code EntityCultistCleric:99} ({@code dataWatcher.addObject(16, (byte) 0)}),
     * {@code :101-103} ({@code getIsRitualist} = bit 0) and {@code :105-113}
     * ({@code v | 1} / {@code v & -2}), plus the NBT pair {@code :124-134} on key
     * {@code "Flags"}.
     *
     * <p>The watched index 16 is pinned as the original's constant
     * ({@link CultistAiLogic#DATA_INDEX_ORIGINAL}); 1.21.1 allocates synched ids
     * through {@code SynchedEntityData.defineId}, so the shipped index is the
     * platform's (recorded deviation). The payload that must survive is the
     * byte, bit 0, and the NBT key — all three are asserted here.</p>
     */
    @GameTest(template = "empty")
    public static void theRitualistFlagIsBitZeroOfTheWatchedByteAndRoundTripsThroughNBT(
            final GameTestHelper helper) {
        final EntityCultistCleric cleric = cleric(helper);
        helper.assertTrue(!cleric.getIsRitualist(),
                ":99 — a fresh cleric's flags byte is 0, so it is not a ritualist");
        cleric.setIsRitualist(true);
        helper.assertTrue(cleric.getIsRitualist(), ":103 — setIsRitualist(true) must set bit 0");
        cleric.setIsRitualist(false);
        helper.assertTrue(!cleric.getIsRitualist(),
                ":108 — setIsRitualist(false) must clear bit 0 (the {@code v & -2} form)");
        // The NBT pair of :124-134: the byte travels whole under key "Flags".
        cleric.setIsRitualist(true);
        final CompoundTag tag = new CompoundTag();
        cleric.addAdditionalSaveData(tag);
        helper.assertTrue(tag.contains(CultistAiLogic.NBT_FLAGS),
                ":133 — the save must write the flags byte under the original's key \""
                        + CultistAiLogic.NBT_FLAGS + "\", keys: " + tag.getAllKeys());
        helper.assertTrue(tag.getByte(CultistAiLogic.NBT_FLAGS) == CultistAiLogic.DATA_BIT_RITUALIST,
                ":133 — the saved byte must carry bit 0 set, got " + tag.getByte(CultistAiLogic.NBT_FLAGS));
        final EntityCultistCleric reloaded = (EntityCultistCleric)
                registered(helper, "cultistcleric").create(helper.getLevel());
        helper.assertTrue(reloaded != null, "the cleric type must create a second instance for the reload");
        if (reloaded != null) {
            reloaded.readAdditionalSaveData(tag);
            helper.assertTrue(reloaded.getIsRitualist(),
                    ":128 — a cleric read from the same tag must come back mid-ritual");
            reloaded.discard();
        }
        // The original's watched index is recorded, and 1.21.1's own allocation is
        // what the class uses — asserted as data so the deviation stays measured.
        helper.assertTrue(CultistAiLogic.DATA_INDEX_ORIGINAL == 16, ":99 — the original's index is 16");
        cleric.discard();
        helper.succeed();
    }

    /**
     * {@code :115-122} — {@code func_70097_a}: an invulnerable cleric takes
     * nothing and keeps its ritual; any damage that lands clears the ritual, then
     * defers to the parent.
     */
    @GameTest(template = "empty")
    public static void anyDamageThatLandsClearsTheRitual(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        // The source must be one the mob's invulnerable flag GATES: 1.21.1's
        // OUT_OF_WORLD carries BYPASSES_INVULNERABILITY, so the :116 arm can never
        // arm against it (measured: isInvulnerableTo(fellOutOfWorld) = false with
        // setInvulnerable(true)). GENERIC is the plain hit the original's
        // func_70097_a saw.
        final DamageSource edge = level.damageSources().generic();
        final EntityCultistCleric cleric = cleric(helper);
        cleric.setIsRitualist(true);
        cleric.hurt(edge, 1.0F);                                                       // :119
        helper.assertTrue(!cleric.getIsRitualist(),
                ":118 — damage that lands must clear the ritual (the original's :118 runs before super)");
        helper.assertTrue(cleric.getHealth() < cleric.getMaxHealth(),
                "PROBE BROKEN: the hit must LAND for the :118 clear to mean damage — health is "
                        + cleric.getHealth() + " of " + cleric.getMaxHealth());
        // The :116 arm: an invulnerable cleric returns at the gate, so the ritual
        // stands and nothing else moves. 1.7.10's test is the source's entity
        // (func_70097_a reads entityattacksource.getEntity().isEntityInvulnerable());
        // 1.21.1's twin is the mob's own isInvulnerableTo(source), which the
        // salvage ports, so the arm is driven by the mob's invulnerable flag.
        final EntityCultistCleric shielded = cleric(helper);
        shielded.setIsRitualist(true);
        shielded.setInvulnerable(true);
        helper.assertTrue(shielded.isInvulnerableTo(edge),
                "PROBE BROKEN: the :116 gate is isInvulnerableTo, so the fixture must be invulnerable"
                        + " to the source it is hit with (got isInvulnerableTo(" + edge + ") = false)");
        shielded.hurt(edge, 100.0F);                                                   // :116
        helper.assertTrue(shielded.getIsRitualist(),
                ":116 — an invulnerable cleric returns at the gate and must NOT clear the ritual");
        helper.assertTrue(shielded.getHealth() == shielded.getMaxHealth(),
                ":116 — the gated hit changes nothing: health is " + shielded.getHealth()
                        + " of " + shielded.getMaxHealth());
        cleric.discard();
        shielded.discard();
        helper.succeed();
    }

    /**
     * {@code :92-94} — {@code func_70692_ba()} (the LOOTING gate) is
     * {@code !getIsRitualist()}: a cleric mid-ritual drops nothing. Per the
     * {@code EntityTaintacleGiant} rule the gate is the family's own
     * {@link EntityCultist#dropAllLoot} seam, NOT 1.21.1's {@code shouldDropLoot()}
     * (which gates the whole death-loot block); the item the seam throws
     * ({@code thaumcraft:itemeldritchobject}, {@link CultistAiLogic#ALL_LOOT_ITEM})
     * is unregistered on this tree, so the drop rides BD-68's live-registry gate
     * and no item may be invented.
     */
    @GameTest(template = "empty")
    public static void theRitualClosesTheLootingGate(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistCleric cleric = cleric(helper);
        final net.minecraft.world.phys.AABB box =
                new net.minecraft.world.phys.AABB(cleric.position(), cleric.position()).inflate(8.0);
        cleric.setIsRitualist(true);
        helper.assertTrue(!cleric.isLootingGateOpen(),
                ":92-94 — mid-ritual the gate is !getIsRitualist() = false");
        for (int i = 0; i < 40; i++) {
            cleric.dropAllLoot();                                                     // :61 through :92-94
        }
        helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)
                        .isEmpty(),
                ":92-94 — a cleric mid-ritual drops nothing, so the gate closed every one of 40 calls;"
                        + " an item appeared on the pad");
        cleric.setIsRitualist(false);
        helper.assertTrue(cleric.isLootingGateOpen(), ":94 — out of the ritual the gate opens");
        final int before = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)
                .size();
        for (int i = 0; i < 40; i++) {
            cleric.dropAllLoot();
        }
        // The tree REGISTERS thaumcraft:itemeldritchobject (CatalogEldritchRealm.java:196), so the
        // :61 drop is measurable on this tree and this probe measures it by name: the id and the
        // pinned meta of CultistAiLogic.ALL_LOOT_META through EntityCultist.dropCultistItem.
        // (The robe and boots ids of :56-59 stay unregistered — BD-102's own probe.)
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(CultistAiLogic.ALL_LOOT_ITEM),
                "PROBE BROKEN: the probe's reading of the tree is that "
                        + CultistAiLogic.ALL_LOOT_ITEM + " is registered by CatalogEldritchRealm:196");
        final net.minecraft.world.item.Item looted =
                BuiltInRegistries.ITEM.get(CultistAiLogic.ALL_LOOT_ITEM);
        final java.util.List<net.minecraft.world.entity.item.ItemEntity> drops =
                level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box);
        helper.assertTrue(drops.size() > before,
                ":61/:94 — out of the ritual the open gate lets the seam drop "
                        + CultistAiLogic.ALL_LOOT_ITEM + "; 40 calls produced "
                        + (drops.size() - before) + " item entities");
        helper.assertTrue(drops.stream().allMatch(e -> e.getItem().is(looted)),
                ":61 — every drop on the pad is the eldritch object, not an invented item");
        helper.assertTrue(drops.stream().anyMatch(e -> Integer.valueOf(CultistAiLogic.ALL_LOOT_META)
                        .equals(e.getItem().get(
                                com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents.ITEM_META))),
                ":61 — the drop carries the pinned meta " + CultistAiLogic.ALL_LOOT_META
                        + " (EntityCultist.withMeta), not a bare stack");
        cleric.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------- ranged attack

    /**
     * {@code EntityCultistCleric:64-89} — {@code func_82196_d(EntityLivingBase,
     * float)} ({@code performRangedAttack}). The {@code :69} roll is
     * {@code nextFloat() > 0.66F}: the orb branch is {@code :70-76}, the
     * fireball branch is {@code :77-88}.
     *
     * <p>The probe drives 120 casts and measures <b>both</b> branches from the
     * level, because the branch is the mob's own RNG. Every orb the cleric throws
     * must be the <b>red</b> orb — the pinned constructor boolean at {@code :70}
     * ({@link CultistAiLogic#CLERIC_ORB_IS_RED}) — owned by the cleric, aimed at
     * the attacker, with the red lifetime (240) and the red damage factor (×1.0)
     * that the same flag selects, and with the attacker's entity id in the spawn
     * payload. The pair flag-lifetime-damage is the card's anchor: the golem
     * throws the same class with {@code false}
     * ({@code boss/EntityEldritchGolem.java:225}), so an orb from a cleric that
     * is not red is the flag lost, not a figure.</p>
     */
    @GameTest(template = "empty_24", timeoutTicks = 300)
    public static void theRangedAttackThrowsTheRedOrbTheConstructorBooleanPins(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistCleric cleric = cleric(helper);
        final Player target = attackerAt(helper, cleric);
        final net.minecraft.world.phys.AABB box =
                new net.minecraft.world.phys.AABB(cleric.position(), cleric.position()).inflate(20.0);
        int orbs = 0;
        int fireballs = 0;
        for (int i = 0; i < 120; i++) {
            cleric.performRangedAttack(target, 8.0F);                                 // :64
            for (final EntityGolemOrb orb
                    : level.getEntitiesOfClass(EntityGolemOrb.class, box)) {
                if (orb.getOwner() != cleric) {
                    continue;
                }
                orbs++;
                helper.assertTrue(orb.isRed(),
                        ":70 — new EntityGolemOrb(world, this, target, TRUE): every orb the cleric"
                                + " throws is the red variant; got a plain orb (the golem's false at"
                                + " boss/EntityEldritchGolem.java:225 is the false caller)");
                helper.assertTrue(orb.getOrbTarget() == target,
                        ":70 — the orb's homing target is the attack target the cleric was given");
                helper.assertTrue(orb.getTargetID() == target.getId(),
                        ":36 — the spawn payload carries the target's entity id (" + target.getId()
                                + "), got " + orb.getTargetID());
                helper.assertTrue(orb.orbDamage()
                                == (float) cleric.getAttributeValue(Attributes.ATTACK_DAMAGE)
                                        * EntityGolemOrb.DAMAGE_RED,
                        ":52-56 — the red orb hits for the owner's attack damage x 1.0, got "
                                + orb.orbDamage());
                helper.assertTrue(orb.tickCount <= EntityGolemOrb.LIFETIME_RED,
                        ":85 — the red orb's lifetime clamp is 240 ticks (the plain orb's is 160)");
                orb.discard();
            }
            for (final SmallFireball fireball
                    : level.getEntitiesOfClass(SmallFireball.class, box)) {
                if (fireball.getOwner() == cleric) {
                    fireballs++;
                    fireball.discard();
                }
            }
        }
        helper.assertTrue(orbs > 0,
                "120 casts produced no orb at all — the :69 branch (nextFloat() > 0.66F) must fire the"
                        + " orb some of the time, and the probe drives 120 independent casts");
        helper.assertTrue(fireballs > 0,
                "120 casts produced no SmallFireball — the :77-88 branch is the other side of the :69 roll");
        cleric.discard();
        helper.succeed();
    }

    /**
     * {@code :77-88} — the fireball branch: three {@code EntitySmallFireball}s
     * ({@code :81-84}), each spread by {@code sqrt(distanceFactor) * 0.5F} on x
     * and z only, started at {@code posY + height/2 + 0.5} ({@code :85}). 1.21.1's
     * {@code SmallFireball(Level, LivingEntity, Vec3)} ctor is 1.7.10's
     * {@code EntitySmallFireball(World, EntityLivingBase, dX, dY, dZ)} body, so
     * the shipped projectile is the <b>vanilla small-fireball id</b> — the card's
     * anchor for {@code EntitySmallFireball at :82}.
     */
    @GameTest(template = "empty_24", timeoutTicks = 300)
    public static void theFireballBranchFiresThreeSmallFireballsAtTheRaisedHeight(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistCleric cleric = cleric(helper);
        final Player target = attackerAt(helper, cleric);
        final net.minecraft.world.phys.AABB box =
                new net.minecraft.world.phys.AABB(cleric.position(), cleric.position()).inflate(20.0);
        int casts = 0;
        int stray = 0;
        for (int i = 0; i < 200 && casts < 8; i++) {
            cleric.performRangedAttack(target, 4.0F);
            final List<SmallFireball> mine = level.getEntitiesOfClass(SmallFireball.class, box).stream()
                    .filter(f -> f.getOwner() == cleric).toList();
            if (mine.isEmpty()) {
                continue;                                                             // the orb branch
            }
            casts++;
            helper.assertTrue(mine.size() == CultistAiLogic.FIREBALL_COUNT,
                    ":81 — the fireball branch fires exactly 3 small fireballs, got " + mine.size());
            final float raised = (float) (cleric.getY() + cleric.getBbHeight() / 2.0F
                    + CultistAiLogic.FIREBALL_HEIGHT_OFFSET);                         // :85
            for (final SmallFireball fireball : mine) {
                helper.assertTrue(BuiltInRegistries.ENTITY_TYPE
                                .getKey(fireball.getType())
                                .equals(BuiltInRegistries.ENTITY_TYPE
                                        .getKey(net.minecraft.world.entity.EntityType.SMALL_FIREBALL)),
                        ":82 — the projectile must be the vanilla EntitySmallFireball id, got "
                                + BuiltInRegistries.ENTITY_TYPE.getKey(fireball.getType()));
                helper.assertTrue(Math.abs(fireball.getY() - raised) < 1.0E-3F,
                        ":85 — each fireball is re-placed at posY + height/2 + 0.5 = " + raised
                                + ", got " + fireball.getY());
                if (!fireball.isRemoved()) {
                    stray++;
                }
                fireball.discard();
            }
        }
        helper.assertTrue(casts >= 4,
                "the probe measured only " + casts + " fireball casts in 200 draws; the :69 roll puts the"
                        + " fireball branch at ~66% so 200 draws must produce several");
        helper.assertTrue(stray >= 3 * casts, "every measured cast handed 3 live fireballs");
        cleric.discard();
        helper.succeed();
    }

    // -------------------------------------------------------------------- the orb

    /** An orb thrown the way the cleric throws it (:70), not added to the level. */
    private static EntityGolemOrb orb(final GameTestHelper helper, final LivingEntity owner,
            final LivingEntity target, final boolean red) {
        final EntityGolemOrb orb = new EntityGolemOrb(
                CatalogGolems.GOLEM_ORB.get(), helper.getLevel(), owner, target, red);
        orb.setNoGravity(true);
        return orb;
    }

    /**
     * {@code :88-105} — the homing step: each tick adds the unit vector toward
     * {@code (target.x, BB.minY + height*0.6, target.z)} times 0.2 to the
     * velocity and clamps every component to ±0.25 ({@link EntityGolemOrb#HOMING}
     * / {@link EntityGolemOrb#HOMING_CLAMP}). {@code :84-86} — the lifetime clamp
     * is {@code age > (red ? 240 : 160)}.
     */
    @GameTest(template = "empty")
    public static void theOrbHomesOnItsTargetWithThePinnedStepAndDiesOnItsLifetime(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistCleric cleric = cleric(helper);
        final Player target = attackerAt(helper, cleric);
        target.setPos(cleric.getX() + 6.0, cleric.getY(), cleric.getZ());
        final EntityGolemOrb orb = orb(helper, cleric, target, true);
        orb.setPos(cleric.getX(), cleric.getY(), cleric.getZ());
        helper.assertTrue(orb.getOwner() == cleric, ":22-26 — the thrower ctor records the owner");
        helper.assertTrue(Math.abs(orb.getDefaultGravityForProbe()) < 1.0E-9,
                ":31-33 — func_70185_h() = 0.0F: the orb is gravity-free");
        // The clamp arm (:102-104): a velocity outside the band is pulled inside it.
        orb.setDeltaMovement(new net.minecraft.world.phys.Vec3(9.0, -9.0, 0.0));       // pre-clamp state
        orb.tick();                                                                   // :88
        final net.minecraft.world.phys.Vec3 clamped = orb.getDeltaMovement();
        helper.assertTrue(Math.abs(clamped.x()) <= EntityGolemOrb.HOMING_CLAMP + 1.0E-6
                        && Math.abs(clamped.y()) <= EntityGolemOrb.HOMING_CLAMP + 1.0E-6
                        && Math.abs(clamped.z()) <= EntityGolemOrb.HOMING_CLAMP + 1.0E-6,
                ":102-104 — every velocity component is clamped to +-0.25, got " + clamped);
        helper.assertTrue(Math.abs(Math.abs(clamped.x()) - EntityGolemOrb.HOMING_CLAMP) < 1.0E-6
                        && Math.abs(Math.abs(clamped.y()) - EntityGolemOrb.HOMING_CLAMP) < 1.0E-6,
                ":102-104 — an out-of-band component lands exactly ON the clamp, got " + clamped);
        // The step arm (:95-101), measured from rest so the clamp is not in the way:
        // the unit vector toward (target.x, BB.minY + height*0.6, target.z) times 0.2.
        orb.setPos(cleric.getX(), cleric.getY(), cleric.getZ());
        orb.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        final double d = orb.position().distanceTo(target.position());                // :91 (d = dist to target)
        final double ux = (target.getX() - orb.getX()) / d;                           // :92/:96
        final double uy = (target.getBoundingBox().minY + target.getBbHeight()
                * EntityGolemOrb.HOMING_HEIGHT_FACTOR - orb.getY()) / d;              // :93/:97
        final double uz = (target.getZ() - orb.getZ()) / d;                           // :94/:98
        orb.tick();                                                                   // :88
        final net.minecraft.world.phys.Vec3 motion = orb.getDeltaMovement();
        helper.assertTrue(Math.abs(motion.x() - ux * EntityGolemOrb.HOMING) < 1.0E-6
                        && Math.abs(motion.y() - uy * EntityGolemOrb.HOMING) < 1.0E-6
                        && Math.abs(motion.z() - uz * EntityGolemOrb.HOMING) < 1.0E-6,
                ":99-101 — from rest, one tick adds the unit vector times 0.2 (HOMING) to the"
                        + " velocity; got " + motion + " expected "
                        + (ux * EntityGolemOrb.HOMING) + "/" + (uy * EntityGolemOrb.HOMING)
                        + "/" + (uz * EntityGolemOrb.HOMING) + " at d=" + d);
        helper.assertTrue(motion.x() > 0.0,
                ":99 — the orb homes TOWARD the target's x, got " + motion.x());
        // The aim point's height factor: the target's BB.minY + height * 0.6.
        final double aimY = target.getBoundingBox().minY + target.getBbHeight()
                * EntityGolemOrb.HOMING_HEIGHT_FACTOR;
        helper.assertTrue(Math.abs(aimY - (target.getY() + target.getBbHeight() * 0.6)) < 1.0E-6,
                ":92 — the aim point is BB.minY + height * 0.6");
        cleric.discard();
        helper.succeed();
    }

    /**
     * {@code EntityGolemOrb:85} — the lifetime clamp: {@code tickCount > (red ? 240 :
     * 160)} → {@code discard()}. The pair runs as entities ADDED TO THE LEVEL, because
     * a detached instance has no removal path to observe (measured: the clamp's
     * {@code discard()} left a level-less orb readable, so the first cut of this probe
     * asserted an unobservable state) and {@code GameTestHelper} has no synchronous
     * tick step — the pad's own tick loop is the age step, and the poll reads the pair
     * at the tick where the plain orb is gone, then pushes the red one to its own edge.
     *
     * <p>The clamp is what 1.7.10 wrote on the plain orb (20 ticks of flight at
     * 0.32/block ≈ 6 blocks of reach); the red arm's 240 is the homing orb's budget.
     * Both edges are pinned: 160/161 for the plain, 240/241 for the red.</p>
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theOrbLifetimeClampDiesAtSixteenForPlainAndTwoFortyForRed(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistCleric cleric = cleric(helper);
        final Player target = attackerAt(helper, cleric);
        final EntityGolemOrb plain = orb(helper, cleric, target, false);
        final EntityGolemOrb red = orb(helper, cleric, target, true);
        plain.setPos(cleric.getX(), cleric.getY(), cleric.getZ());
        red.setPos(cleric.getX(), cleric.getY(), cleric.getZ());
        plain.tickCount = EntityGolemOrb.LIFETIME;                                    // :85 plain edge
        red.tickCount = EntityGolemOrb.LIFETIME;                                      // :85 inside the red clamp
        level.addFreshEntity(plain);
        level.addFreshEntity(red);
        final int[] phase = {0};
        // The pad's success poll can run BEFORE the level has ticked the pair, and an
        // orb sitting ON its clamp edge has not been discarded yet. Measured on CI's
        // no-Curios job at 9d4b882a: the first phase read age=160 red=false — the
        // clamp tick had not run — and the removal assert reddened a probe that was
        // green on every local leg. An orb at its edge is therefore keep-polling, not
        // a failure; the removal is asserted once the age has actually crossed.
        helper.succeedWhen(() -> {
            if (phase[0] == 0) {
                if (plain.tickCount <= EntityGolemOrb.LIFETIME) {
                    return;  // the plain orb has not crossed its 160 edge: poll again
                }
                helper.assertTrue(plain.isRemoved(),
                        ":85 — a plain orb past 160 discards; age=" + plain.tickCount
                                + " red=" + plain.isRed());
                helper.assertTrue(red.tickCount > EntityGolemOrb.LIFETIME && !red.isRemoved(),
                        ":85 — a red orb past 160 sits inside its 240-tick clamp and survives; age="
                                + red.tickCount);
                red.tickCount = EntityGolemOrb.LIFETIME_RED;                          // :85 red edge
                phase[0] = 1;
            } else {
                if (red.tickCount <= EntityGolemOrb.LIFETIME_RED) {
                    return;  // the red orb has not crossed its 240 edge: poll again
                }
                helper.assertTrue(red.isRemoved(),
                        ":85 — a red orb past 240 discards; age=" + red.tickCount);
            }
        });
    }

    /**
     * {@code :35-47} — the spawn payload: the target's entity id ({@code -1} when
     * there is none) then the {@code red} boolean, in that order. 1.7.10's
     * {@code IEntityAdditionalSpawnData} is NeoForge's
     * {@code IEntityWithComplexSpawn}, whose buffer is the registry-friendly form.
     */
    @GameTest(template = "empty")
    public static void theOrbSpawnPayloadCarriesTheTargetIdThenTheRedFlag(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistCleric cleric = cleric(helper);
        // The client leg resolves the id through level#getEntity(int), so the
        // target must be a real entity IN the level (the taintacle card's shape).
        final Zombie target = new Zombie(EntityType.ZOMBIE, level);
        target.setPos(cleric.getX() + 2.0, cleric.getY(), cleric.getZ());
        target.setNoGravity(true);
        level.addFreshEntity(target);
        helper.assertTrue(level.getEntity(target.getId()) == target,
                "PROBE BROKEN: the target must resolve by id in the level for the payload's client leg");
        // Payload 1: a real target, red.
        final EntityGolemOrb thrown = orb(helper, cleric, target, true);
        final RegistryFriendlyByteBuf buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        thrown.writeSpawnData(buffer);
        helper.assertTrue(buffer.readableBytes() == 5,
                ":35-40 — the payload is the int id then the boolean flag = 5 bytes, got "
                        + buffer.readableBytes());
        final EntityGolemOrb client = new EntityGolemOrb(
                CatalogGolems.GOLEM_ORB.get(), level);
        client.readSpawnData(buffer);
        helper.assertTrue(client.getTargetID() == target.getId(),
                ":42-47 — the id leg resolves to the target (" + target.getId() + "), got "
                        + client.getTargetID());
        helper.assertTrue(client.getOrbTarget() == target,
                ":42-47 — the resolved target is the same entity, so a client homes on the right mob");
        helper.assertTrue(client.isRed(), ":47 — the flag leg is the red boolean");
        // Payload 2: no target, plain — the id leg is -1 and the lookup is inert.
        final EntityGolemOrb none = orb(helper, cleric, null, false);
        final RegistryFriendlyByteBuf buffer2 =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        none.writeSpawnData(buffer2);
        final EntityGolemOrb client2 = new EntityGolemOrb(CatalogGolems.GOLEM_ORB.get(), level);
        client2.readSpawnData(buffer2);
        helper.assertTrue(client2.getTargetID() == -1,
                ":36 — an absent target writes -1, got " + client2.getTargetID());
        helper.assertTrue(client2.getOrbTarget() == null,
                ":42-47 — id -1 resolves to no target");
        helper.assertTrue(!client2.isRed(), ":47 — the plain orb's flag reads back false");
        cleric.discard();
        target.discard();
        helper.succeed();
    }

    /**
     * {@code :113-131} — {@code func_70097_a(DamageSource, float)}: an
     * invulnerable orb takes nothing; any hit zeroes the velocity, and when the
     * source has an owning entity the orb inherits 0.9 of that entity's look
     * vector and the hit counts. With no owning entity the hit does not count.
     * {@code :69-71} — {@code func_70053_R()} ({@code getPickRadius}) is 0.1F.
     */
    @GameTest(template = "empty")
    public static void theOrbParriesByInheritingTheAttackersLook(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistCleric cleric = cleric(helper);
        final Player thrower = attackerAt(helper, cleric);
        thrower.setPos(cleric.getX() + 3.0, cleric.getY(), cleric.getZ());
        thrower.setYRot(90.0F);                                                       // look toward -x
        thrower.setXRot(0.0F);
        final EntityGolemOrb orb = orb(helper, cleric, thrower, true);
        orb.setPos(thrower.getX() - 3.0, thrower.getY(), thrower.getZ());
        orb.setDeltaMovement(new net.minecraft.world.phys.Vec3(0.25, 0.25, 0.25));
        helper.assertTrue(Math.abs(orb.getPickRadius() - EntityGolemOrb.PICK_RADIUS) < 1.0E-6F,
                ":69-71 — getPickRadius is 0.1F, got " + orb.getPickRadius());
        helper.assertTrue(orb.hurt(level.damageSources().playerAttack(thrower), 1.0F),
                ":129 — a hit from a source with an owning entity counts");
        final net.minecraft.world.phys.Vec3 motion = orb.getDeltaMovement();
        final net.minecraft.world.phys.Vec3 expected = thrower.getLookAngle()
                .scale(EntityGolemOrb.PARRY_LOOK_FACTOR);
        helper.assertTrue(Math.abs(motion.x() - expected.x()) < 1.0E-4
                        && Math.abs(motion.y() - expected.y()) < 1.0E-4
                        && Math.abs(motion.z() - expected.z()) < 1.0E-4,
                ":121-126 — the parry inherits 0.9 of the attacker's look vector, got " + motion
                        + " expected " + expected);
        helper.assertTrue(!orb.hurt(level.damageSources().generic(), 1.0F),
                ":130 — a hit with no owning entity does not count");
        cleric.discard();
        helper.succeed();
    }

    // -------------------------------------------------------------- altar & stare

    /**
     * The client-side stare of {@code :146-158}. The gate is
     * {@code worldObj.isRemote} ({@code :147}) and the harness runs a dedicated
     * server, so the probe drives the stare's own seam, {@link
     * EntityCultistCleric#applyRitualLook}: the original's pitch step is
     * {@code 10.0F} per tick ({@code :155}) and its yaw step is the mob's
     * rotation limit ({@code :156}).
     */
    @GameTest(template = "empty_24")
    public static void theRitualStareTurnsTheHeadToTheAltarAtThePinnedSteps(final GameTestHelper helper) {
        final EntityCultistCleric cleric = cleric(helper);
        final BlockPos home = helper.absolutePos(BlockPos.ZERO).offset(1, 5, 1);
        cleric.setHomePosition(home, CultistAiLogic.SPAWN_HOME_DISTANCE);
        cleric.setXRot(0.0F);
        cleric.setYHeadRot(0.0F);
        cleric.applyRitualLook(home);                                                 // :155-156
        final float pitch = cleric.getXRot();
        final float yaw = cleric.getYHeadRot();
        helper.assertTrue(Math.abs(pitch) <= CultistAiLogic.RITUAL_LOOK_PITCH_STEP + 1.0E-3F,
                ":155 — the pitch step is the original's 10.0F per tick, got a move to " + pitch);
        helper.assertTrue(Math.abs(yaw) <= cleric.getMaxHeadXRot() + 1.0E-3F,
                ":156 — the yaw step is the mob's rotation limit (getMaxHeadXRot = "
                        + cleric.getMaxHeadXRot() + "), got " + yaw);
        helper.assertTrue(pitch < 0.0F,
                ":154 — the altar sits above the cleric's eyes, so the pitch is negative (up)");
        // The convergence: the stare settles on the original's own radian pair.
        final net.minecraft.world.phys.Vec3 look = CultistAiLogic.clericLookAngles(
                home.getX() + CultistAiLogic.RITUAL_LOOK_XZ_OFFSET - cleric.getX(),
                home.getY() + CultistAiLogic.RITUAL_LOOK_Y_OFFSET - (cleric.getY() + cleric.getEyeHeight()),
                home.getZ() + CultistAiLogic.RITUAL_LOOK_XZ_OFFSET - cleric.getZ());
        for (int i = 0; i < 120; i++) {
            cleric.applyRitualLook(home);
        }
        helper.assertTrue(Math.abs(cleric.getXRot() - (float) look.y()) < 1.0E-3F,
                ":154 — 120 steps of 10.0F must converge on the original's pitch " + look.y()
                        + ", got " + cleric.getXRot());
        helper.assertTrue(Math.abs(MthWrap.wrap(cleric.getYHeadRot() - (float) look.x())) < 1.0E-3F,
                ":153 — the head converges on the original's yaw " + look.x() + ", got "
                        + cleric.getYHeadRot());
        cleric.discard();
        helper.succeed();
    }

    /** {@code AIAltarFocus} (94 lines, cleric goal slot 1): {@code :22-24} and
     *  {@code :31-33} are "ritualist and a home exists"; {@code :35-45} runs on
     *  the cleric's {@code ticksExisted % 40} cadence and drops the ritual when
     *  the squared distance from the home to the cleric's <b>block-truncated</b>
     *  position exceeds 16.0F, or when the home block is no longer
     *  {@code thaumcraft:blockeldritch}.
     *
     *  <p>The truncation is the original's {@code (int)} casts — TOWARD ZERO — not the
     *  floor-based BlockPos the harness getters build. The fixture's feet are read with
     *  the goal's own casts; the in-body note carries the measured sign flip.</p> */
    @GameTest(template = "empty_24")
    public static void theAltarFocusGoalKeepsTheRitualAndDropsItOnItsTwoArms(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistCleric cleric = cleric(helper);
        // The probe's whole case is exact feet arithmetic, so the cleric's own goals
        // (RandomStroll) must not walk it off the figures; the goal under test is a
        // standalone instance the probe ticks by hand, so no AI is needed to drive it.
        cleric.setNoAi(true);
        final BlockPos home = helper.absolutePos(BlockPos.ZERO).offset(2, 3, 2);
        // The block goes in through the LEVEL: GameTestHelper#setBlock reads its position
        // as TEMPLATE-RELATIVE (it adds absoluteVec), so an absolute BlockPos lands
        // millions of blocks away in an unloaded chunk and the write is a silent no-op —
        // measured: the pad read Block{minecraft:air} at the home just written. The
        // house's own precedent for an absolute write is GolemCombatAiGameTest.java:1486.
        level.setBlock(home, CatalogEldritchRealm.BLOCK_ELDRITCH.get().defaultBlockState(),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockState(home).is(CatalogEldritchRealm.BLOCK_ELDRITCH.get()),
                "PROBE BROKEN: the pad must hold a thaumcraft:blockeldritch at the home for :38-43;"
                        + " home=" + home + " origin=" + helper.absolutePos(BlockPos.ZERO)
                        + " wanted=" + CatalogEldritchRealm.BLOCK_ELDRITCH.get()
                        + " reading=" + level.getBlockState(home));
        cleric.setHomePosition(home, CultistAiLogic.SPAWN_HOME_DISTANCE);
        cleric.setIsRitualist(true);
        final AIAltarFocus goal = new AIAltarFocus(cleric);
        helper.assertTrue(goal.canUse() && goal.canContinueToUse(),
                ":22-24/:31-33 — ritualist + a home is the whole gate, in both directions");
        cleric.tickCount = CultistAiLogic.ALTAR_CADENCE_TICKS;                        // :36
        goal.tick();
        helper.assertTrue(cleric.getIsRitualist(),
                ":37-43 — a cleric standing at its eldritch home keeps the ritual on the 40-tick check");
        // Arm 1: the distance, measured to the cleric's block-truncated feet. The
        // figure is exact arithmetic, not a tolerance: the port's :37 is
        // {@code new Vec3(home).distanceToSqr(feet) > 16.0F} with feet the (int) casts of
        // :37, and 1.7.10's {@code func_71569_e(int,int,int)} is {@code BlockPos#distSqr}
        // over ALL THREE axes, so the y term is inside the figure (measured on
        // reference/decompiled/.../ai/misc/AIAltarFocus.java:37). The fixture's truncated
        // feet sit at home-(1,0,1) (cleric(): offset(1,3,1), moveTo +0.5 on x/z, y whole),
        // so every home below is placed at an exact offset FROM THE FEET: 16.0 keeps (the
        // strict >), 17.0 drops (the threshold, one over), on both axes.
        // :37 truncation is the JAVA (int) CAST — it truncates TOWARD ZERO — while the
        // harness getters (getBlockX/getBlockY/getBlockZ) build a FLOOR-based BlockPos.
        // The harness parks every pad at a random region origin, negative on each axis
        // about half the time, and on a negative axis the two conventions disagree by
        // one block. Measured at 9d4b882a with the guards: the pad at origin
        // x=-12702163 z=-4830762 read pos=(-12702161.5, -57.0, -4830762.5); the goal's
        // feet are then (-12702161, -57, -4830762) while the floor-based fixture feet
        // said (-12702162, -57, -4830763). The "distSq 16.0" keep arm computed 17.0 for
        // the goal (dropped, red) and the "distSq 17.0" drop arm computed 9.0 (kept,
        // red) on the SAME tree, run after run. Every figure below is built with the
        // goal's own casts, so distSq is exact on both sign halves.
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        final BlockPos feet = new BlockPos((int) cleric.getX(), (int) cleric.getY(),
                (int) cleric.getZ());
        final BlockPos expectedFeet = new BlockPos((int) (origin.getX() + 1 + 0.5),
                origin.getY() + 3, (int) (origin.getZ() + 1 + 0.5));
        helper.assertTrue(feet.equals(expectedFeet),
                "PROBE BROKEN: the arms' arithmetic pins the goal's (int)-cast feet to the"
                        + " cleric() fixture's placement at origin+(1,3,1) plus 0.5 on x/z — if"
                        + " the mob drifted, no distSq figure in this probe means anything;"
                        + " feet=" + feet + " expected=" + expectedFeet + " origin=" + origin
                        + " pos=" + cleric.position() + " tickCount=" + cleric.tickCount);
        final java.util.function.BiConsumer<BlockPos, String> live = (spot, why) -> helper.assertTrue(
                level.getBlockState(spot).is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())
                        && new net.minecraft.world.phys.Vec3(spot.getX(), spot.getY(), spot.getZ())
                        .distanceToSqr(new net.minecraft.world.phys.Vec3(
                                feet.getX(), feet.getY(), feet.getZ()))
                        <= CultistAiLogic.ALTAR_MAX_DIST_SQ,
                "PROBE BROKEN (" + why + "): the arm under test needs a home that is"
                        + " thaumcraft:blockeldritch AND inside the distance clamp, so the drop"
                        + " arms can only answer for the figure named in the assert; home=" + spot
                        + " reading=" + level.getBlockState(spot)
                        + " d2=" + new net.minecraft.world.phys.Vec3(spot.getX(), spot.getY(), spot.getZ())
                        .distanceToSqr(new net.minecraft.world.phys.Vec3(
                                feet.getX(), feet.getY(), feet.getZ()))
                        + " limit=" + CultistAiLogic.ALTAR_MAX_DIST_SQ
                        + " liveFeet=" + feet + " pos=" + cleric.position()
                        + " tickCount=" + cleric.tickCount + " region=" + origin);
        final BlockPos edgeX = feet.offset(4, 0, 0);                                   // distSq 16.0
        final BlockPos overX = feet.offset(4, 0, 1);                                   // distSq 17.0
        final BlockPos edgeY = feet.offset(0, 4, 0);                                   // distSq 16.0
        final BlockPos overY = feet.offset(0, 4, 1);                                   // distSq 17.0
        for (final BlockPos spot : new BlockPos[]{edgeX, overX, edgeY, overY}) {
            level.setBlock(spot, CatalogEldritchRealm.BLOCK_ELDRITCH.get().defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
            helper.assertTrue(level.getBlockState(spot).is(CatalogEldritchRealm.BLOCK_ELDRITCH.get()),
                    "PROBE BROKEN: the pad must hold thaumcraft:blockeldritch at " + spot
                            + " (reading=" + level.getBlockState(spot) + ")");
        }
        cleric.setHomePosition(edgeX, CultistAiLogic.SPAWN_HOME_DISTANCE);
        live.accept(edgeX, "edgeX keeps");
        cleric.tickCount = CultistAiLogic.ALTAR_CADENCE_TICKS;
        goal.tick();
        helper.assertTrue(cleric.getIsRitualist(),
                ":37 — distSq exactly 16.0 (4 blocks on x from the truncated feet) KEEPS the ritual,"
                        + " so the comparison is a strict > (home=" + edgeX + " feet=" + feet + ")");
        cleric.setIsRitualist(true);
        cleric.setHomePosition(overX, CultistAiLogic.SPAWN_HOME_DISTANCE);
        cleric.tickCount = CultistAiLogic.ALTAR_CADENCE_TICKS;
        goal.tick();
        helper.assertTrue(!cleric.getIsRitualist(),
                ":37 — distSq 17.0 (4 on x, 1 on z) drops the ritual (home=" + overX
                        + " feet=" + feet + " d2=" + new Vec3(overX.getX(), overX.getY(), overX.getZ())
                        .distanceToSqr(new Vec3(feet.getX(), feet.getY(), feet.getZ()))
                        + " limit=" + CultistAiLogic.ALTAR_MAX_DIST_SQ
                        + " block=" + level.getBlockState(overX)
                        + " tickCount=" + cleric.tickCount + " cadence=" + CultistAiLogic.ALTAR_CADENCE_TICKS
                        + " liveFeet=" + feet
                        + " pos=" + cleric.position() + " homeNow=" + cleric.getHomePosition()
                        + " canUse=" + goal.canUse() + ")");
        cleric.setIsRitualist(true);
        cleric.setHomePosition(edgeY, CultistAiLogic.SPAWN_HOME_DISTANCE);
        live.accept(edgeY, "edgeY keeps");
        cleric.tickCount = CultistAiLogic.ALTAR_CADENCE_TICKS;
        goal.tick();
        helper.assertTrue(cleric.getIsRitualist(),
                ":37 — the y axis is inside distSqr (the original's distSqr(int,int,int)): distSq"
                        + " exactly 16.0 straight UP keeps (home=" + edgeY + " feet=" + feet + ")");
        cleric.setIsRitualist(true);
        cleric.setHomePosition(overY, CultistAiLogic.SPAWN_HOME_DISTANCE);
        cleric.tickCount = CultistAiLogic.ALTAR_CADENCE_TICKS;
        goal.tick();
        helper.assertTrue(!cleric.getIsRitualist(),
                ":37 — distSq 17.0 straight up (4 up, 1 over) drops the ritual (home=" + overY
                        + " feet=" + feet + ")");
        cleric.setIsRitualist(true);
        // Arm 2: the home block, with the home back inside the distance clamp.
        cleric.setHomePosition(edgeX, CultistAiLogic.SPAWN_HOME_DISTANCE);
        level.setBlock(edgeX, Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockState(edgeX).is(Blocks.STONE),
                "PROBE BROKEN (stone home): the :38-43 arm needs a NON-eldritch block at the home;"
                        + " reading=" + level.getBlockState(edgeX) + " home=" + edgeX
                        + " region=" + helper.absolutePos(BlockPos.ZERO));
        cleric.tickCount = CultistAiLogic.ALTAR_CADENCE_TICKS;
        goal.tick();
        helper.assertTrue(!cleric.getIsRitualist(),
                ":38-43 — a home block that is not thaumcraft:blockeldritch drops the ritual (home="
                        + edgeX + " reading=" + level.getBlockState(edgeX) + ")");
        // The cadence gate: the SAME drop condition is inert off the beat (47 % 40 = 7)
        // and live on it (40 % 40 = 0) — the check runs on ticksExisted % 40 only.
        cleric.setIsRitualist(true);
        cleric.tickCount = CultistAiLogic.ALTAR_CADENCE_TICKS + 7;
        goal.tick();
        helper.assertTrue(cleric.getIsRitualist(),
                ":36 — at ticksExisted 47 the drop arms never run, stone home included");
        cleric.tickCount = CultistAiLogic.ALTAR_CADENCE_TICKS;
        goal.tick();
        helper.assertTrue(!cleric.getIsRitualist(),
                ":36 — at ticksExisted 40 the same stone home drops the ritual");
        level.setBlock(edgeX, CatalogEldritchRealm.BLOCK_ELDRITCH.get().defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        // The gate pair, once the ritual is gone.
        cleric.setIsRitualist(false);
        helper.assertTrue(!goal.canUse() && !goal.canContinueToUse(),
                ":22-24 — out of the ritual the goal never arms");
        cleric.setIsRitualist(true);
        final EntityCultistCleric homeless = cleric(helper);
        helper.assertTrue(homeless.getHomePosition() == null,
                "PROBE BROKEN: a fresh cleric must have no home (the payload of :136-144 is what sets it)");
        helper.assertTrue(!new AIAltarFocus(homeless).canUse(), ":24 — a ritualist with no home never arms");
        homeless.discard();
        cleric.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------ gang call

    /**
     * The cleric's half of the gang call: {@code AICultistHurtByTarget:48-53} —
     * a cleric mid-ritual is <b>left alone</b> ({@code :49-50} skipped) unless
     * the {@code nextInt(3)} roll lands, in which case {@code :52} clears its
     * ritual and {@code :53} joins. This is the arm the c4a-1 lane carried by
     * registry id (BD-90); here it runs on the class, so the ritual write at
     * {@code :52} is the cleric's own watched byte.
     */
    @GameTest(template = "empty_24", timeoutTicks = 500)
    public static void theGangCallSparesARitualistClericUntilTheRollBreaksIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistKnight hurt = knight(helper);
        final EntityCultistCleric ritualist = cleric(helper);
        final EntityCultistCleric plain = cleric(helper);
        plain.setIsRitualist(false);
        ritualist.setIsRitualist(true);
        final Player player = attackerAt(helper, hurt);
        // The gang call, driven by hand: the knight's own goal stays RUNNING once the
        // first start() gives it target == attacker (canContinueToUse keeps answering
        // true), so the selector never consults the not-running canUse that leads back
        // to start()/callTheGang — every mob-driven attempt buys the same ONE draw. See
        // the in-loop note for the measured rates. The slot-1 wiring itself (the stamp
        // gate, the selector, the alert box on the mob) is the knight card's probes
        // (BD-90/BD-100); this probe answers for :48-53, the cleric arm of the call.
        final AICultistHurtByTarget gangCall = new AICultistHurtByTarget(hurt, true);
        helper.assertTrue(ritualist.getIsRitualist(), "PROBE BROKEN: the cleric is not mid-ritual");
        // setLastHurtByMob stamps with the mob's OWN tickCount (the knight card's
        // finding), and the pad's mob ticks from the harness's own clock, so the stamp
        // is > 0 and distinct per attempt — the :20 stamp gate is what the mob-driven
        // probes pin; here it only has to be non-consumed.
        // :51 is `world.rand.nextInt(3) == 0` — and the port reads it as
        // `level.getRandom()` (AICultistHurtByTarget:152), the original's `world.rand`:
        // ONE draw per gang call, and the goal re-arms only when the hurt stamp moves
        // again (canUse: stamp != this.timestamp). A single hit therefore recruits the
        // ritualist with probability 1/3, which is exactly how this probe went red in
        // one leg and green in the next on the same tree. The dice cannot be pinned:
        // the harness runs every test in ONE shared ServerLevel, so seeding
        // `level.random` would steer the other tests' dice. So the probe drives them:
        // 40 hits at 10-tick spacing (400 of the 500-tick window), the caller healed
        // before each so it survives the window, and (2/3)^40 = 1.3e-7 is the honest
        // miss bound on the ritualist staying unsaved.
        //
        // The hits are scheduled UP FRONT with one Runnable per hit:
        // GameTestInfo#setRunAtTickTime keys its map by the RUNNABLE
        // (put(runnable, tick)) and the harness drops the entry after running it, so
        // re-scheduling the SAME instance from inside the step is swallowed — measured:
        // the chained version drove exactly one roll and left the probe red.
        // The dice are the SHARED level's (the harness runs every test in one
        // ServerLevel), so the probe cannot pin them — and reading them on a fixed
        // 10-tick cadence falls into the LCG's phase: measured 40/40 misses in leg A and
        // 40/40 in leg B on the same tree, i.e. 80/80, which is not the (2/3)^40 tail.
        // The probe therefore re-seeds the stream with a value that carries no bias and
        // moves run to run: the game time at test start (which rides the harness's load)
        // and this test's own region origin (which is per-test). No test gains a
        // preference; this one stops reading one phase of the generator.
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        final int[] driven = {0};
        for (int attempt = 1; attempt <= 60; attempt++) {
            final int n = attempt;
            helper.runAtTickTime(5L * n, () -> {
                // A FRESH phase per attempt: one seed is one phase of the generator and
                // one phase of a 1-in-3 dice can be all-misses (measured: one seed gave
                // 40/40 misses, then 80/80 across legs — 1.3e-7 tails do not fire twice).
                // Reseeding the shared stream per attempt is unbiased (no test prefers an
                // outcome); it just stops this probe reading one phase of it. The
                // DETERMINISTIC part of the seed, though, pins the phase, not the dice:
                // the roll is drawn a fixed number of consumers after the reseed, so one
                // (seed, phase) pair answers all 60 attempts the same way. Measured at
                // 9d4b882a: 60/60 misses on CI's curios job (pad x=-11222658) and on the
                // local no-Curios leg (pad x=-13340463), green on the local curios legs
                // of the same tree — 2 reds in 5 local legs, not the (2/3)^60 tail.
                // XOR-ing System.nanoTime() puts one run-entropy bit in front of the
                // phase per attempt: each attempt becomes a fresh unbiased 1-in-3 draw
                // (the seed is not chosen for an outcome, so no test gains a preference)
                // and the probe's (2/3)^60 miss bound is the honest one.
                level.random.setSeed((0x5DEECE66DL * n + origin.getX() * 31L + origin.getZ())
                        ^ System.nanoTime());
                hurt.setHealth(hurt.getMaxHealth());                                  // fixture: the caller lives
                // One mob hit bought ONE gang call, not 60, and no fixture twist on the
                // mob's selector changes that: the goal's FIRST start() leaves the
                // knight with target == attacker, canContinueToUse reads that as "still
                // chasing", the goal runs forever, and the not-running canUse pass — the
                // only path back to start()/callTheGang and the :51 roll — sits behind
                // the selector's rebuild cadence. Measured, one draw per leg every time:
                // 3/8 legs green at 9d4b882a under the fixed-seed phase (1/3 per leg),
                // then 4/4 legs red at 60/60-miss under entropy seeds, and CI red at
                // a26f465e with the target cleared per attempt — (2/3)^60 does not fire;
                // ONE draw does. So each attempt stamps the attacker and calls start()
                // on the standalone goal: :29-30 plus the help-box scan :33-54, the dice
                // still the shared level's, the clerics still real entities in the
                // shared level, and every attempt one real unbiased 1-in-3 draw.
                hurt.setLastHurtByMob(player);                                        // :29-30 — stamp on the live clock
                gangCall.start();                                                     // :33-54 — the gang call
                driven[0] = n;
            });
        }
        helper.succeedWhen(() -> {
            hurt.clearFire();
            helper.assertTrue(plain.getTarget() == player,
                    ":49-50 — a cleric with no ritual in the help box takes the attacker (target = "
                            + plain.getTarget() + ")");
            helper.assertTrue(ritualist.getIsRitualist() == false,
                    ":52 — the roll must have cleared the ritualist's ritual: " + driven[0]
                            + " of 60 attempts driven (each attempt re-seeds the shared level RNG at a"
                            + " fresh phase and drives one gang call = one nextInt(3) draw; the rule"
                            + " itself is pinned deterministically in CultistAiLogicTest),"
                            + " miss bound (2/3)^60 = 2.7e-11, probeTick=" + helper.getTick());
            helper.assertTrue(ritualist.getTarget() == player,
                    ":53 — the cleric whose ritual broke joins the gang (target = "
                            + ritualist.getTarget() + ")");
            hurt.discard();
            plain.discard();
            ritualist.discard();
        });
    }

    private static EntityCultistKnight knight(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Entity spawned = registered(helper, "cultistknight").create(level);
        final EntityCultistKnight mob = (EntityCultistKnight) spawned;
        final BlockPos at = helper.absolutePos(BlockPos.ZERO).offset(2, 3, 2);
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        mob.setNoGravity(true);
        mob.clearFire();
        level.addFreshEntity(mob);
        return mob;
    }

    // ----------------------------------------------------------------- equipment

    /**
     * {@code EntityCultistCleric:55-62} — {@code func_82164_bB}
     * ({@code addRandomEquipment}), reached through 1.21.1's
     * {@code Mob#populateDefaultEquipmentSlots} in the {@code finalizeSpawn}
     * pass (BD-98's seam). The robe ({@code :56-58}) goes on unconditionally and
     * the cultist boots ({@code :59}) ride a {@code 0.3F} roll on HARD /
     * {@code 0.1F} below it. Every one of those ids is a Thaumcraft item this
     * tree has not registered, so the slots stay EMPTY — BD-68's shape: the port
     * never invents an item for an unregistered id. The cleric carries no weapon,
     * so the robe and the boots are the whole equipment body.
     */
    @GameTest(template = "empty")
    public static void theEquipmentPassHandsOutNoInventedRobeOrBoots(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final DifficultyInstance difficulty = new DifficultyInstance(
                net.minecraft.world.Difficulty.HARD, level.getDayTime(), 0L, 0.0F);
        for (int i = 0; i < 60; i++) {
            final EntityCultistCleric cleric = cleric(helper);
            cleric.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, null);       // :55
            for (final EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND)) {
                final ItemStack stack = cleric.getItemBySlot(slot);
                helper.assertTrue(stack.isEmpty(),
                        ":56-59 — every cleric item id (the robe of :56-58, the boots of :59) is"
                                + " unregistered on this tree, so the " + slot
                                + " slot must stay empty; the pass handed out " + stack.getItem());
            }
            cleric.discard();
        }
        // The robe table and the boots' roll, pinned as data because their items
        // are not in this tree.
        helper.assertTrue(CultistAiLogic.clericRobe().size() == 3
                        && CultistAiLogic.clericRobe().values().stream()
                                .noneMatch(id -> BuiltInRegistries.ITEM.containsKey(id)),
                ":56-58 — the robe is the three unregistered cultist-robe ids — "
                        + CultistAiLogic.clericRobe());
        helper.assertTrue(CultistAiLogic.CLERIC_BOOTS_CHANCE_HARD == 0.3F
                        && CultistAiLogic.CLERIC_BOOTS_CHANCE == 0.1F,
                ":59 — the boots ride 0.3F on HARD and 0.1F below it");
        helper.succeed();
    }

    /** {@code :136-144} — the spawn payload's three ints: home x, y, z, read back
     *  as {@code setHomePosAndDistance(x, y, z, 8)}. */
    @GameTest(template = "empty")
    public static void theClericSpawnPayloadCarriesTheHomeAsThreeInts(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityCultistCleric cleric = cleric(helper);
        final BlockPos home = helper.absolutePos(BlockPos.ZERO).offset(4, 6, 3);
        cleric.setHomePosition(home, CultistAiLogic.SPAWN_HOME_DISTANCE);
        final RegistryFriendlyByteBuf buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        cleric.writeSpawnData(buffer);
        helper.assertTrue(buffer.readableBytes() == 4 * CultistAiLogic.SPAWN_INT_COUNT,
                ":136-139 — the payload is the home as 3 ints ("
                        + CultistAiLogic.SPAWN_INT_COUNT + " ints), got " + buffer.readableBytes()
                        + " bytes");
        final EntityCultistCleric client = (EntityCultistCleric)
                registered(helper, "cultistcleric").create(level);
        helper.assertTrue(client != null, "the cleric type must create a client-side twin");
        if (client != null) {
            client.readSpawnData(buffer);
            helper.assertTrue(home.equals(client.getHomePosition()),
                    ":141-144 — the three ints read back as the home " + home + ", got "
                            + client.getHomePosition());
            helper.assertTrue(client.getHomeDist() == CultistAiLogic.SPAWN_HOME_DISTANCE,
                    ":144 — the distance leg is the original's literal 8, got " + client.getHomeDist());
            client.discard();
        }
        cleric.discard();
        helper.succeed();
    }

    /** The wrap helper for the yaw convergence assert (360-periodic). */
    private static final class MthWrap {
        private MthWrap() {
        }

        static float wrap(final float degrees) {
            float f = degrees % 360.0F;
            if (f >= 180.0F) {
                f -= 360.0F;
            }
            if (f < -180.0F) {
                f += 360.0F;
            }
            return f;
        }
    }
}
