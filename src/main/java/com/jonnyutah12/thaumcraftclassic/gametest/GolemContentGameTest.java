package com.jonnyutah12.thaumcraftclassic.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EnumGolemType;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemResearch;
import com.jonnyutah12.thaumcraftclassic.items.golems.ItemGolemPlacer;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import com.jonnyutah12.thaumcraftclassic.recipes.CrucibleRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TC-M6-08-s probes: the golem substrate is a real entity type with the
 * original's attributes, a placed golem is a live {@link EntityGolemBase}
 * carrying the material its placer stack named, and the GOLEMANCY gate really
 * gates — each material's shipped crucible row is live in the RecipeManager,
 * names that material's research entry, and refuses a thrower who has not read
 * it.
 *
 * <p>What this card does <b>not</b> claim: nothing here drives a golem task,
 * a marker, feeding or a GUI. Those are TC-M6-08-s-b / TC-M6-08-s-c, and a
 * probe asserting them would be asserting unshipped work.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GolemContentGameTest {

    /** The eight shipped placer crucible rows, in material order (recipe.32..39.crucible). */
    private static final String[] PLACER_ROWS = {
            "recipe.32.crucible", "recipe.33.crucible", "recipe.34.crucible", "recipe.35.crucible",
            "recipe.36.crucible", "recipe.37.crucible", "recipe.38.crucible", "recipe.39.crucible"};

    private GolemContentGameTest() {
    }

    /**
     * The entity type is {@code thaumcraft:golem}, it is the ported
     * {@link EntityGolemBase}, and its registration numbers are the original's
     * own: hitbox 0.4 × 0.95, tracking range 64, update interval 3, and the
     * {@code func_110147_ax} attribute set (health 20, attack 1, speed 0.6,
     * follow range 32) plus the constructor's step height 1.0.
     */
    @GameTest(template = "empty")
    public static void golemEntityTypeIsTheSubstrate(final GameTestHelper helper) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "golem");
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id),
                "thaumcraft:golem must be registered (content-mapping.csv Thaumcraft:Golem, kind=entity)");
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        helper.assertTrue(type == CatalogGolems.GOLEM.get(), "thaumcraft:golem resolves to the catalog holder");
        helper.assertTrue(type.getCategory() == MobCategory.MISC,
                "the golem family is MISC in 1.21.1 (the vanilla iron/snow golem category); got " + type.getCategory());
        helper.assertTrue(type.getWidth() == EntityGolemBase.WIDTH && type.getHeight() == EntityGolemBase.HEIGHT,
                "the original's func_70105_a(0.4F, 0.95F) hitbox, got "
                        + type.getWidth() + "x" + type.getHeight());
        helper.assertTrue(type.clientTrackingRange() == EntityGolemBase.TRACKING_RANGE
                        && type.updateInterval() == EntityGolemBase.UPDATE_INTERVAL,
                "the original registration registerModEntity(..., 64, 3, true): got range "
                        + type.clientTrackingRange() + ", interval " + type.updateInterval());

        final var attributes = EntityGolemBase.createAttributes().build();
        assertAttribute(helper, attributes, Attributes.MAX_HEALTH, EntityGolemBase.BASE_HEALTH, "MAX_HEALTH");
        assertAttribute(helper, attributes, Attributes.ATTACK_DAMAGE, EntityGolemBase.BASE_ATTACK_DAMAGE, "ATTACK_DAMAGE");
        assertAttribute(helper, attributes, Attributes.MOVEMENT_SPEED, EntityGolemBase.BASE_MOVEMENT_SPEED, "MOVEMENT_SPEED");
        assertAttribute(helper, attributes, Attributes.FOLLOW_RANGE, EntityGolemBase.BASE_FOLLOW_RANGE, "FOLLOW_RANGE");
        assertAttribute(helper, attributes, Attributes.STEP_HEIGHT, EntityGolemBase.STEP_HEIGHT, "STEP_HEIGHT");

        // Every registered entity type needs a live instance factory; a null here
        // means the attribute/type pair never made it into the level.
        helper.assertTrue(type.create(helper.getLevel()) instanceof EntityGolemBase,
                "the registered factory builds an EntityGolemBase");
        helper.succeed();
    }

    /**
     * A spawned golem carries the material variant it was given, with that
     * material's own max health and fire immunity — the two material-derived
     * figures {@code setupGolem()} applies.
     */
    @GameTest(template = "empty")
    public static void spawnedGolemCarriesItsMaterialVariant(final GameTestHelper helper) {
        final var level = helper.getLevel();
        for (final EnumGolemType material : List.of(EnumGolemType.CLAY, EnumGolemType.STRAW, EnumGolemType.THAUMIUM)) {
            final EntityGolemBase golem = CatalogGolems.GOLEM.get().create(level);
            helper.assertTrue(golem != null, "the golem type must build an instance");
            golem.setMaterial(material);
            golem.moveTo(helper.absolutePos(new BlockPos(2, 2, 2)).getX() + 0.5,
                    helper.absolutePos(new BlockPos(2, 2, 2)).getY(),
                    helper.absolutePos(new BlockPos(2, 2, 2)).getZ() + 0.5, 0.0F, 0.0F);
            golem.applyMaterialTraits();
            helper.assertTrue(level.addFreshEntity(golem), "the golem must join the level");
            helper.assertTrue(level.getEntity(golem.getId()) == golem,
                    "the spawned golem is live in the level (id " + golem.getId() + ")");
            helper.assertTrue(golem.getMaterial() == material,
                    "the material variant round-trips through the synched data: expected " + material
                            + ", got " + golem.getMaterial());
            helper.assertTrue(golem.getAttribute(Attributes.MAX_HEALTH).getBaseValue() == material.health,
                    material + " max health is the EnumGolemType health figure (" + material.health + "), got "
                            + golem.getAttribute(Attributes.MAX_HEALTH).getBaseValue());
            helper.assertTrue(golem.fireImmune() == material.fireResist,
                    material + " fire immunity is the enum's fireResist (" + material.fireResist + ")");
            helper.assertTrue(golem.upgradeSlotCount() == material.upgrades,
                    "an ordinary golem has the material's upgrade slots (" + material.upgrades + ")");
            golem.discard();
        }

        // The advanced flag adds the original's extra upgrade slot.
        final EntityGolemBase advanced = CatalogGolems.GOLEM.get().create(level);
        advanced.setMaterial(EnumGolemType.IRON);
        advanced.setAdvanced(true);
        advanced.applyMaterialTraits();
        helper.assertTrue(advanced.upgradeSlotCount() == EnumGolemType.IRON.upgrades + 1,
                "advanced golems get one more upgrade slot than the material's own count");
        // ... and the attack damage is 2 + strength + the strength upgrades (the original setupGolem formula).
        helper.assertTrue(advanced.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()
                        == 2 + EnumGolemType.IRON.strength,
                "IRON attack damage is 2 + strength (" + (2 + EnumGolemType.IRON.strength) + "), got "
                        + advanced.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
        advanced.discard();
        helper.succeed();
    }

    /**
     * The construction path: {@code ItemGolemPlacer.useOn} on a block spawns a
     * live golem of the material the stack's metadata names, and spends the
     * stack outside creative — the original's {@code onItemUseFirst} +
     * {@code spawnCreature}.
     */
    @GameTest(template = "empty")
    public static void placerConstructsTheGolem(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final BlockPos clicked = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.STONE);

        final ItemStack stack = new ItemStack(CatalogGolems.ITEM_GOLEM_PLACER.get());
        stack.set(ModDataComponents.ITEM_META.get(), EnumGolemType.IRON.ordinal());
        helper.assertTrue(ItemGolemPlacer.materialOf(stack) == EnumGolemType.IRON,
                "meta 6 is IRON (EnumGolemType order)");

        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final UseOnContext context = new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false));
        final InteractionResult result = stack.useOn(context);
        helper.assertTrue(result.consumesAction(),
                "placing a golem consumes the interaction (the original returns true server-side)");

        final List<EntityGolemBase> spawned = level.getEntitiesOfClass(EntityGolemBase.class,
                new AABB(clicked).inflate(4.0));
        helper.assertTrue(spawned.size() == 1,
                "exactly one golem is placed on the clicked face's neighbour, got " + spawned.size());
        final EntityGolemBase golem = spawned.get(0);
        helper.assertTrue(golem.getMaterial() == EnumGolemType.IRON,
                "the placed golem carries the placer stack's material (IRON), got " + golem.getMaterial());
        helper.assertTrue(golem.getAttribute(Attributes.MAX_HEALTH).getBaseValue() == EnumGolemType.IRON.health,
                "and that material's max health (" + EnumGolemType.IRON.health + ")");
        helper.assertTrue(golem.getHomeFacing() == Direction.UP.get3DDataValue(),
                "the golem's home facing is the clicked face (setup(side))");
        helper.assertTrue(golem.getOwnerName().equals(player.getGameProfile().getName()),
                "the placing player owns the golem, got '" + golem.getOwnerName() + "'");
        helper.assertTrue(stack.getCount() == 0,
                "the placer stack is spent outside creative, got " + stack.getCount());
        helper.succeed();
    }

    /**
     * The GOLEMANCY gate, wired to the shipped entries. For each of the eight
     * materials the crucible row that crafts its placer is enumerated from the
     * shipped data; a row whose {@code neoforge:item_exists} gate is satisfied
     * must be live in the RecipeManager, must name that material's research
     * entry, and must refuse a thrower who lacks it (the entry exists in the
     * shipped graph — asserted here — so the refusal has teeth).
     */
    @GameTest(template = "empty")
    public static void theMaterialGateRowsAreLiveAndGated(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var manager = level.getRecipeManager();
        int live = 0;
        final List<String> missing = new ArrayList<>();
        final List<String> wrongResearch = new ArrayList<>();
        final List<String> gated = new ArrayList<>();

        for (int i = 0; i < EnumGolemType.values().length; i++) {
            final EnumGolemType material = EnumGolemType.values()[i];
            final String rowId = PLACER_ROWS[i];
            final JsonObject shipped = shippedRow(helper, rowId);
            final List<String> unmet = unmetConditions(shipped);
            if (!unmet.isEmpty()) {
                // The row's gate names an item another card owns (the magic log,
                // the taint block, or the vanilla id 1.21 renamed). Verify that
                // it is not one of THIS card's items: a golem item missing from
                // the registries would silently drop the row.
                for (final String item : unmet) {
                    helper.assertTrue(!item.startsWith(ThaumcraftClassic.MOD_ID + ":itemgolem")
                                    && !(ThaumcraftClassic.MOD_ID + ":golembell").equals(item),
                            rowId + " is gated on " + item + ", which this card registers — the gate must not "
                                    + "hide a missing golem registration");
                }
                gated.add(rowId + " <- " + unmet);
                continue;
            }
            live++;
            final var holder = manager.byKey(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, rowId));
            if (holder.isEmpty()) {
                missing.add(rowId);
                continue;
            }
            final var recipe = holder.get().value();
            helper.assertTrue(recipe instanceof CrucibleRecipe,
                    rowId + " must decode as a CrucibleRecipe, got " + recipe.getClass().getSimpleName());
            final CrucibleRecipe crucible = (CrucibleRecipe) recipe;
            if (!GolemResearch.researchKey(material).equals(crucible.research())) {
                wrongResearch.add(rowId + " names '" + crucible.research() + "'");
            }
            // The gate itself: a thrower with no knowledge is refused.
            helper.assertTrue(!crucible.researchUnlocked(null),
                    rowId + " must refuse a thrower who has not read " + GolemResearch.researchKey(material));
        }

        helper.assertTrue(live >= 4,
                "at least four of the eight placer rows must pass their item_exists gate (the rest wait on "
                        + "items other cards own, and every one of those is checked to be foreign), got " + live
                        + " live, gated: " + gated);
        helper.assertTrue(missing.isEmpty(),
                "every ungated placer row must be live in the RecipeManager once the golem items are "
                        + "registered (a decoding failure drops a row in silence), missing: " + missing);
        helper.assertTrue(wrongResearch.isEmpty(),
                "each placer row must be gated on its own material's GOLEMANCY entry: " + wrongResearch);

        // The gate keys exist in the shipped graph (no new entries authored).
        for (final EnumGolemType material : EnumGolemType.values()) {
            final var entry = com.jonnyutah12.thaumcraftclassic.research.ResearchManager.graph()
                    .entry(GolemResearch.researchKey(material));
            helper.assertTrue(entry != null && GolemResearch.CATEGORY.equals(entry.category()),
                    GolemResearch.researchKey(material) + " must be a shipped GOLEMANCY entry");
        }
        ThaumcraftClassic.LOGGER.info("[TC-M6-08-s] golem data: {} of {} placer crucible rows live and "
                + "research-gated on their material's GOLEMANCY entry; gated: {}",
                live, EnumGolemType.values().length, gated);
        helper.succeed();
    }

    /** The shipped recipe JSON of a row id, read through the server's ResourceManager. */
    private static JsonObject shippedRow(final GameTestHelper helper, final String rowId) {
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID,
                "recipe/" + rowId + ".json");
        final var resource = helper.getLevel().getServer().getResourceManager().getResource(file);
        helper.assertTrue(resource.isPresent(), "the shipped data must contain " + file);
        try (var reader = resource.get().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (final IOException failure) {
            throw new AssertionError("unreadable recipe resource " + file, failure);
        }
    }

    /**
     * The items a row's {@code neoforge:item_exists} gates name that are not
     * registered; a non-empty result means the row legitimately does not load
     * yet. A condition this probe cannot evaluate is reported as its own entry
     * so it can never be mistaken for "fine".
     */
    private static List<String> unmetConditions(final JsonObject row) {
        final JsonElement conditions = row.get("neoforge:conditions");
        final List<String> unmet = new ArrayList<>();
        if (conditions == null || !conditions.isJsonArray()) {
            return unmet;
        }
        for (final JsonElement element : conditions.getAsJsonArray()) {
            final JsonObject condition = element.getAsJsonObject();
            if (!"neoforge:item_exists".equals(condition.get("type").getAsString())) {
                unmet.add("unevaluated condition " + condition.get("type").getAsString());
                continue;
            }
            final String item = condition.get("item").getAsString();
            if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(item))) {
                unmet.add(item);
            }
        }
        return unmet;
    }

    private static void assertAttribute(final GameTestHelper helper,
                                        final net.minecraft.world.entity.ai.attributes.AttributeSupplier attributes,
                                        final net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                        final double expected, final String what) {
        helper.assertTrue(attributes.getBaseValue(attribute) == expected,
                "the original func_110147_ax " + what + " is " + expected + ", got "
                        + attributes.getBaseValue(attribute));
    }
}
