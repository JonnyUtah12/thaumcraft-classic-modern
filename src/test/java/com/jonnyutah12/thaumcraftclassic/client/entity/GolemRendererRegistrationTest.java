package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBobber;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EnumGolemType;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchGolem;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-08-s-c2 acceptance for the golem rendering stack: the six classes
 * {@code RenderGolemBase}, {@code ModelGolem}, {@code ModelGolemAccessories},
 * {@code RenderGolemBobber}, {@code RenderEldritchGolem},
 * {@code ModelEldritchGolem}.
 *
 * <p>What is proved, and at which level — the honest split, in the shape the
 * wisp card (TC-M7-03-s, {@code WispRendererRegistrationTest}) established:</p>
 * <ol>
 *   <li><b>Contract level (plain JVM):</b> each renderer has 1.21.1's provider
 *       constructor shape, extends the base its 1.7.10 parent rendered through,
 *       is parameterized on the port's own entity type, and addresses the
 *       original textures by literal path — the nine body sheets of
 *       {@code RenderGolemBase.func_110775_a}, the decoration and damage sheets,
 *       the eldritch sheet, and vanilla's particle sheet for the bobber.</li>
 *   <li><b>Registry level (plain JVM):</b> both real client events are fired
 *       through the production seam. {@code RegisterRenderers} ends with the
 *       three types in 21.1's own {@code EntityRenderers.PROVIDERS}, mapped to
 *       providers that construct the three renderer classes; {@code
 *       RegisterLayerDefinitions} ends with the four model layers in 21.1's own
 *       layer-definition map ({@code ClientHooks.layerDefinitions}), and every
 *       registered supplier bakes into a real {@code ModelPart} tree.</li>
 *   <li><b>Geometry level (plain JVM):</b> the baked trees carry the original's
 *       part names, rotation points ({@code setRotationPoint} →
 *       {@code PartPose.offset}) and box bounds ({@code addBox} →
 *       {@code ModelPart.Cube} min/max), including the shipped
 *       {@code f2 = 30} offset of {@code ModelGolem}'s constructor, the
 *       {@code + 0.5F} chest deformation, and the 1.7.10 leg-swing wave.
 *       Baking is pure Java, so this exercises the same code the client
 *       runs.</li>
 *   <li><b>Wiring level (source-level):</b> the handler is self-registering on
 *       the MOD bus, client-only, and the production path passes the live
 *       {@code thaumcraft:golem}, {@code thaumcraft:eldritchgolem} and
 *       {@code thaumcraft:golembobber} holders into the seam — so
 *       {@code runServer}/{@code runGameTestServer} never loads a renderer.</li>
 * </ol>
 *
 * <p>What is NOT proved here, and is recorded as {@code not_proved} in
 * {@code docs/FEATURE_MATRIX.md}: the rendered pixels. The texture contents
 * behind every {@code thaumcraft} path are All Rights Reserved and stay in the
 * local-build overlay (D-004), the bucket {@code .obj} mesh is not ported, and
 * the two per-part GL blend sub-passes of the decoration sheet have no 1.21.1
 * render-type seam (BD-41). A human client look is what closes those.</p>
 */
class GolemRendererRegistrationTest {

    private static final float EPS = 1.0E-4F;

    // ---------------------------------------------------- contract level

    @Test
    void golemRendererIsTheLivingRendererForTheGolemWithTheOriginalShadowRadius() throws Exception {
        assertEquals(LivingEntityRenderer.class, RenderGolemBase.class.getSuperclass(),
                "1.7.10 RenderGolemBase extends RenderLiving; 1.21.1's twin is LivingEntityRenderer");
        assertSame(EntityGolemBase.class, typeArgument(RenderGolemBase.class),
                "the renderer is parameterized on the port's golem");
        assertSame(ModelGolem.class, modelTypeArgument(RenderGolemBase.class),
                "and on the port's ModelGolem");
        assertNotNull(RenderGolemBase.class.getConstructor(EntityRendererProvider.Context.class));
        // RenderGolemBase.java:53 — super(modelBase, 0.25F)
        assertEquals(0.25F, shadowRadiusOf(golemRenderer()), EPS, "the golem's shadow radius");
    }

    @Test
    void eldritchRendererIsAPlainLivingRendererOnTheMonsterTypeAsTheReferenceIs() throws Exception {
        // The reference extends RenderLiving directly (RenderEldritchGolem.java:16) —
        // NOT RenderGolemBase — and its entity is EntityThaumcraftBoss -> EntityMob
        // (common/entities/monster/boss/EntityEldritchGolem.java:35,
        // EntityThaumcraftBoss.java:25), so 1.21.1's LivingEntityRenderer is the
        // faithful shape and the golem passes are not inherited in either tree.
        assertEquals(LivingEntityRenderer.class, RenderEldritchGolem.class.getSuperclass(),
                "RenderEldritchGolem.java:16 — extends RenderLiving, 1.21.1's twin is LivingEntityRenderer");
        assertTrue(RenderGolemBase.class.isAssignableFrom(RenderEldritchGolem.class) == false,
                "the reference does not inherit the golem passes, so neither may this port");
        assertSame(EntityEldritchGolem.class, typeArgument(RenderEldritchGolem.class));
        assertSame(ModelEldritchGolem.class, modelTypeArgument(RenderEldritchGolem.class));
        final RenderEldritchGolem eldritch = eldritchRenderer();
        assertEquals(RenderEldritchGolem.TEXTURE, eldritch.getTextureLocation((EntityEldritchGolem) null),
                "RenderEldritchGolem.java:18/:25-27 — textures/models/eldritch_golem.png");
        // The shadow radius is the figure the registration site hands the constructor,
        // not one this class picks: new RenderEldritchGolem(new ModelEldritchGolem(), 0.5F).
        assertEquals(0.5F, RenderEldritchGolem.SHADOW_RADIUS, EPS, "ClientProxy.java:457");
        assertEquals(0.5F, shadowRadiusOf(eldritch), EPS, "the stored shadow radius is the registration's 0.5F");
        // :31 glScalef(2.15, 2.15, 2.15) — the pre-render scale, on the model's pose.
        assertEquals(2.15F, RenderEldritchGolem.PRE_RENDER_SCALE, EPS, "RenderEldritchGolem.java:31");
        final com.mojang.blaze3d.vertex.PoseStack pose = new com.mojang.blaze3d.vertex.PoseStack();
        pose.pushPose();
        eldritch.scale(null, pose, 0.0F);
        final float scaledX = pose.last().pose().transformPosition(new org.joml.Vector3f(1.0F, 0.0F, 0.0F)).x();
        assertEquals(2.15F, scaledX, 1.0E-4F, "the scale hook multiplies the model pose by 2.15");
        pose.popPose();
        // :35-37 GL_BLEND + alphaFunc -> the entity translucent render type on the eldritch sheet.
        assertEquals(net.minecraft.client.renderer.RenderType.entityTranslucent(RenderEldritchGolem.TEXTURE),
                eldritch.getRenderType((EntityEldritchGolem) null, false, false, false),
                "the blended + alpha-tested entity draw is the twin of the source's GL state");
    }

    @Test
    void theEldritchRendererEmitsNoPassThatTheReferenceDoesNot() throws Exception {
        // The reference (47 lines) emits NOTHING of its own: no bowtie, no icon quad,
        // no second model. A renderer that drew an invented pass would put pixels on
        // screen that the original never drew, so the absence is pinned at source level.
        final String source = readSource("client/entity/RenderEldritchGolem.java");
        assertTrue(!source.contains("addVertex"), "no invented geometry pass may be emitted");
        assertTrue(!source.contains("Tessellator") && !source.contains("VertexConsumer"),
                "the reference draws no tessellator quads of its own");
        assertTrue(source.contains("PRE_RENDER_SCALE") && source.contains("getRenderType"),
                "the two figures the reference does carry — the 2.15 scale and the GL state — must be ported");
    }

    @Test
    void bobberRendererIsThePlainEntityRendererForTheBobberOnVanillasParticleSheet() throws Exception {
        assertEquals(EntityRenderer.class, RenderGolemBobber.class.getSuperclass(),
                "1.7.10 RenderGolemBobber extends RenderLivingBase; 1.21.1's twin is EntityRenderer");
        assertSame(EntityGolemBobber.class, typeArgument(RenderGolemBobber.class));
        assertNotNull(RenderGolemBobber.class.getConstructor(EntityRendererProvider.Context.class));
        assertEquals(ResourceLocation.withDefaultNamespace("textures/particle/particles.png"),
                RenderGolemBobber.PARTICLE_TEXTURE,
                "RenderGolemBobber.java:16 — the vanilla particle sheet, referenced by path only");
        assertEquals(RenderGolemBobber.PARTICLE_TEXTURE,
                bobberRenderer().getTextureLocation((EntityGolemBobber) null), ":90-92 (the entity is ignored)");
    }

    @Test
    void theGolemTexturesAreTheNineMaterialArmsPlusDecorationAndDamage() {
        // RenderGolemBase.java:43-50, the switch of :269-290, and the two binds at :120/:126.
        assertSheet(RenderGolemBase.TEXTURE_STRAW, "golem_straw.png", EnumGolemType.STRAW);
        assertSheet(RenderGolemBase.TEXTURE_WOOD, "golem_wood.png", EnumGolemType.WOOD);
        assertSheet(RenderGolemBase.TEXTURE_CLAY, "golem_clay.png", EnumGolemType.CLAY);
        assertSheet(RenderGolemBase.TEXTURE_STONE, "golem_stone.png", EnumGolemType.STONE);
        assertSheet(RenderGolemBase.TEXTURE_IRON, "golem_iron.png", EnumGolemType.IRON);
        assertSheet(RenderGolemBase.TEXTURE_TALLOW, "golem_tallow.png", EnumGolemType.TALLOW);
        assertSheet(RenderGolemBase.TEXTURE_FLESH, "golem_flesh.png", EnumGolemType.FLESH);
        assertSheet(RenderGolemBase.TEXTURE_THAUMIUM, "golem_thaumium.png", EnumGolemType.THAUMIUM);
        assertEquals(RenderGolemBase.TEXTURE_DECORATION,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_decoration.png"));
        assertEquals(RenderGolemBase.TEXTURE_DAMAGE,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_damage.png"));
        // :288 — the switch's default arm is the player skin sheet.
        assertEquals("minecraft", RenderGolemBase.textureFor(null).getNamespace());
        assertTrue(RenderGolemBase.textureFor(null).getPath().startsWith("textures/entity/player/")
                        && RenderGolemBase.textureFor(null).getPath().contains("steve"),
                "the default arm is vanilla's player skin (DefaultPlayerSkin), as AbstractClientPlayer.field_110314_b was");
    }

    @Test
    void theBucketSheetIsReferedByPathOnlyAndTheObjMeshIsNotPorted() throws Exception {
        // RenderGolemBase.java:233 binds textures/models/bucket.png for the fluid pass;
        // :234 loads textures/models/bucket.obj through AdvancedModelLoader, which
        // NeoForge 21.1 has no twin for. Both stay ledger rows (asset-mapping 881/882).
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/bucket.png"),
                RenderGolemBase.TEXTURE_BUCKET);
        final String source = readSource("client/entity/RenderGolemBase.java");
        assertTrue(source.contains("AdvancedModelLoader"), "the delta must be documented, not silent");
        assertTrue(source.contains(".obj"), "the bucket mesh's absence is named in the source");
    }

    // ---------------------------------------------------- registry level

    @Test
    @SuppressWarnings("unchecked")
    void firingTheRendererEventRoutesAllThreeGolemsIntoTheVanillaProviderMapAndBuildsThem() throws Exception {
        final Map<EntityType<?>, EntityRendererProvider<?>> providers = vanillaProviders();
        final EntityType<EntityGolemBase> golemKey = (EntityType<EntityGolemBase>) (EntityType<?>) EntityType.ZOMBIE;
        final EntityType<EntityEldritchGolem> eldritchKey =
                (EntityType<EntityEldritchGolem>) (EntityType<?>) EntityType.COW;
        final EntityType<EntityGolemBobber> bobberKey =
                (EntityType<EntityGolemBobber>) (EntityType<?>) EntityType.ARMOR_STAND;
        final EntityRendererProvider<?> golemBefore = providers.get(golemKey);

        // The keys are incidental: a unit harness has the registries frozen, so the
        // live CatalogGolems holders cannot be resolved here and vanilla types stand
        // in. What is asserted is the route the production call takes. The eldritch
        // stand-in is a LIVING vanilla type on purpose: the real row is a Monster,
        // which is a LivingEntity, and RenderEldritchGolem is a LivingEntityRenderer.
        final EntityRenderersEvent.RegisterRenderers event = new EntityRenderersEvent.RegisterRenderers();
        ModEntityRenderers.register(event, golemKey, eldritchKey, bobberKey);

        for (final EntityType<?> key : new EntityType<?>[] {golemKey, eldritchKey, bobberKey}) {
            assertTrue(providers.containsKey(key), "the registration must land in EntityRenderers' own map: " + key);
        }
        assertNotSame(golemBefore, providers.get(golemKey),
                "RegisterRenderers -> EntityRenderers.register must replace the entry for the type handed in");

        // A context with no parts: the model is self-baked (the 1.7.10 proxy built
        // its models the same way — `new ModelGolem(false)` at ClientProxy.java:415),
        // so a provider builds the renderer without a client.
        final EntityRendererProvider.Context bare = new EntityRendererProvider.Context(
                null, null, null, null, null, null, null);
        assertInstanceOf(RenderGolemBase.class, providers.get(golemKey).create(bare),
                "the stored provider must build RenderGolemBase (pins RenderGolemBase::new)");
        assertInstanceOf(RenderEldritchGolem.class, providers.get(eldritchKey).create(bare),
                "and RenderEldritchGolem (pins RenderEldritchGolem::new)");
        assertInstanceOf(RenderGolemBobber.class, providers.get(bobberKey).create(bare),
                "and RenderGolemBobber (pins RenderGolemBobber::new)");
    }

    @Test
    void theThreePairingsMatchTheEntityTypesTheParentCardRegistered() throws Exception {
        // 1.21.1 binds a renderer to the entity type in its type parameter, so the
        // three pairings are only legal if the port\'s entity classes have the bases
        // the renderers need. Read from the source, not assumed:
        final String eldritch = readSource("entities/monster/EntityEldritchGolem.java");
        final String bobber = readSource("entities/golems/EntityGolemBobber.java");
        final String golem = readSource("entities/golems/EntityGolemBase.java");
        assertTrue(eldritch.contains("class EntityEldritchGolem extends Monster"),
                "the eldritch golem is a Monster — a LivingEntity, so the LivingEntityRenderer binds");
        assertTrue(golem.contains("class EntityGolemBase extends"),
                "the golem keeps its own base class; its renderer is parameterized on that class");
        assertTrue(bobber.contains("class EntityGolemBobber extends Entity"),
                "the bobber is a plain Entity (as in 1.7.10), so its renderer is a plain EntityRenderer");
        assertTrue(!LivingEntityRenderer.class.isAssignableFrom(RenderGolemBobber.class),
                "the bobber renderer is not a living renderer");
        assertEquals(EntityRenderer.class, RenderGolemBobber.class.getSuperclass(),
                "RenderGolemBobber extends EntityRenderer — the twin of 1.7.10 Render");
    }

        @Test
    @SuppressWarnings("unchecked")
    void firingTheLayerEventRegistersFourGolemLayersThatAllBakeRealTrees() throws Exception {
        final Map<ModelLayerLocation, Supplier<net.minecraft.client.model.geom.builders.LayerDefinition>> layers =
                vanillaLayerDefinitions();
        final EntityRenderersEvent.RegisterLayerDefinitions event =
                new EntityRenderersEvent.RegisterLayerDefinitions();
        ModEntityRenderers.registerLayers(event);

        for (final ModelLayerLocation location : new ModelLayerLocation[] {ModEntityRenderers.GOLEM_LAYER,
                ModEntityRenderers.GOLEM_DAMAGE_LAYER, ModEntityRenderers.GOLEM_ACCESSORIES_LAYER,
                ModEntityRenderers.ELDRITCH_GOLEM_LAYER}) {
            assertTrue(layers.containsKey(location), "the layer must reach 21.1's layer-definition map: " + location);
            final ModelPart root = layers.get(location).get().bakeRoot();
            assertTrue(root != null && !childrenOf(root).isEmpty(),
                    "the registered supplier must bake a non-empty tree: " + location);
        }
        // The damage pass is the same geometry as the body, the reference's second
        // ModelGolem with pass = 2 (RenderGolemBase.java:54-58).
        assertEquals(partNames(layers.get(ModEntityRenderers.GOLEM_LAYER).get().bakeRoot()),
                partNames(layers.get(ModEntityRenderers.GOLEM_DAMAGE_LAYER).get().bakeRoot()),
                "the damage layer carries the body's six parts");
    }

    // ---------------------------------------------------- geometry level

    @Test
    void golemModelCarriesTheSixOriginalPartsAndTheShippedRotationPoints() {
        final ModelPart root = ModelGolem.layer().bakeRoot();
        assertArrayEquals(new String[] {"golemBody", "golemHead", "golemLeftArm", "golemLeftLeg", "golemRightArm",
                "golemRightLeg"}, partNames(root).stream().sorted().toArray(String[]::new),
                "ModelGolem.java:20-46, the six parts of the constructor (the tree's own part names)");

        // ModelGolem.java:22 f2 = 30 (the shipped `new ModelGolem(false)`), so
        // :26 head y = 0 + f2, :40/:44 leg y = 18 + f2.
        assertPose(root.getChild("golemHead"), 0.0F, 30.0F, -2.0F);
        assertPose(root.getChild("golemBody"), 0.0F, 30.0F, 0.0F);
        assertPose(root.getChild("golemRightArm"), 0.0F, 30.0F, 0.0F);
        assertPose(root.getChild("golemLeftArm"), 0.0F, 30.0F, 0.0F);
        assertPose(root.getChild("golemRightLeg"), -4.0F, 48.0F, 0.0F);
        assertPose(root.getChild("golemLeftLeg"), 5.0F, 48.0F, 0.0F);

        // The boxes: addBox(u, v, x, y, z, w, h, d, scale) -> Cube min/max.
        assertCube(root.getChild("golemHead"), 0, -4.0F, -11.0F, -5.5F, 4.0F, -2.0F, 2.5F); // :27 8x9x8
        final List<ModelPart.Cube> body = cubes(root.getChild("golemBody"));
        assertEquals(2, body.size(), ":30 and :31 — the chest is two boxes, the only two-box part");
        assertBounds(body.get(0), -8.0F, -2.0F, -6.0F, 8.0F, 10.0F, 5.0F); // :30 16x12x11
        assertVertexBounds(body.get(0), -8.0F, -2.0F, -6.0F, 8.0F, 10.0F, 5.0F);
        // :31 addBox(-4.5, 10, -3, 9, 5, 6, f1 + 0.5F) — the deformation. 1.21.1
        // bakes a CubeDeformation into the polygon vertices, not into the cube's
        // min/max fields, so the grown box is read off the geometry it draws.
        assertEquals(-4.5F, body.get(1).minX, EPS, ":31's box origin, before the deformation");
        assertVertexBounds(body.get(1), -5.0F, 9.5F, -3.5F, 5.0F, 15.5F, 3.5F);
        assertCube(root.getChild("golemRightArm"), 0, -12.0F, -2.5F, -3.0F, -8.0F, 22.5F, 3.0F); // :34 4x25x6
        assertCube(root.getChild("golemLeftArm"), 0, 8.0F, -2.5F, -3.0F, 12.0F, 22.5F, 3.0F); // :38 4x25x6
        assertCube(root.getChild("golemRightLeg"), 0, -3.5F, -3.0F, -3.0F, 2.5F, 13.0F, 2.0F); // :41 6x16x5
        assertCube(root.getChild("golemLeftLeg"), 0, -3.5F, -3.0F, -3.0F, 2.5F, 13.0F, 2.0F); // :45 6x16x5
    }

    @Test
    void accessoriesModelCarriesAllFourteenDecorationPartsOfTheReference() {
        final ModelPart root = ModelGolemAccessories.layer().bakeRoot();
        // ModelGolemAccessories.java:38-189 — 14 parts, in the reference's order.
        final String[] expected = {"golemHeadFez", "golemPlate", "golemPlateLeft", "golemPlateRight",
                "golemHeadHat", "golemHeadGlasses", "golemVisor", "golemHeadHatRim", "golemDartgun",
                "golemMace", "golemBowtie", "golemHeadJar", "golemHeadBrain", "golemEvilHead"};
        for (final String name : expected) {
            assertTrue(root.hasChild(name), "the decoration model must bake part " + name);
        }
        assertEquals(expected.length, childrenOf(root).size(), "exactly the reference's 14 parts");
        // The reference's rotation points all use the same f2 = 30 body offset
        // (its constructor takes (0, 30) from RenderGolemBase.java:60).
        assertPose(root.getChild("golemHeadFez"), 0.0F, 30.0F, -2.0F);
        assertPose(root.getChild("golemPlate"), 0.0F, 30.0F, 0.0F);
        // The fez box: :38-39 tex(0,94) box(-4.5,-15,-6) 9x7x9
        assertCube(root.getChild("golemHeadFez"), 0, -4.5F, -15.0F, -6.0F, 4.5F, -8.0F, 3.0F);
    }

    @Test
    void eldritchModelCarriesTheCloakCollarTorsoAndNestedShoulderTree() {
        final ModelPart root = ModelEldritchGolem.layer().bakeRoot();
        final String[] roots = {"Cloak1", "Cloak3", "Cloak2", "CloakCL", "CloakCR", "Head", "Head2",
                "CollarL", "CollarR", "CollarB", "CollarF", "CollarBlack", "Frontcloth0", "Frontcloth1",
                "Frontcloth2", "Torso", "ArmR", "ArmL", "BackpanelR1", "WaistR1", "WaistR2", "WaistR3",
                "LegR", "WaistL1", "WaistL2", "WaistL3", "BackpanelL1", "LegL"};
        for (final String name : roots) {
            assertTrue(root.hasChild(name), "the eldritch model must bake root part " + name);
        }
        for (final String shoulder : new String[] {"ShoulderR", "ShoulderR0", "ShoulderR1", "ShoulderR2"}) {
            assertTrue(root.getChild("ArmR").hasChild(shoulder), "ArmR must carry the nested " + shoulder);
        }
        for (final String shoulder : new String[] {"ShoulderL", "ShoulderL0", "ShoulderL1", "ShoulderL2"}) {
            assertTrue(root.getChild("ArmL").hasChild(shoulder), "ArmL must carry the nested " + shoulder);
        }
        // ModelEldritchGolem.java:303-331 — the legs are the biped's, at x = ±5.5.
        final ModelPart legR = root.getChild("LegR");
        final ModelPart legL = root.getChild("LegL");
        assertEquals(-legR.x, legL.x, EPS, "the legs mirror about the body's centre line");
        assertEquals(legR.y, legL.y, EPS);
    }

    @Test
    void theLegSwingWaveIsTheOriginalTriangleWave() {
        // ModelGolem.java:163-165 func_78172_a(f, f2) =
        //   (|f % f2 - f2/2| - f2/4) / (f2/4)
        assertEquals(1.0F, ModelGolem.wave(0.0F, 13.0F), EPS, "the swing's peak at phase 0");
        assertEquals(0.0F, ModelGolem.wave(3.25F, 13.0F), EPS, "the zero crossing at a quarter period");
        assertEquals(-1.0F, ModelGolem.wave(6.5F, 13.0F), EPS, "the trough at half a period");
        assertEquals(1.0F, ModelGolem.wave(13.0F, 13.0F), EPS, "and the period repeats");
        assertEquals(-0.04F, ModelGolem.wave(1.3F, 5.0F), EPS,
                "the strike wave of :142 (period 5) is the formula's own value: |1.3 % 5 - 2.5| = 1.2, (1.2 - 1.25) / 1.25");
    }

    @Test
    void theHealTintAndDamageAlphaAreTheOriginalGlColours() {
        // ModelGolem.java:81-85 glColor3f(0.5F + h1, 0.9F + h2, 0.5F + h1), h1 = heal/10, h2 = heal/5
        assertEquals(0xFFFFFFFF, ModelGolem.healingTint(0, 0), "no healing, no tint");
        assertEquals(0xFFFFFFFF, ModelGolem.healingTint(10, 0), "a full heal saturates every channel");
        assertEquals(0xFFB2FFB2, ModelGolem.healingTint(2, 0), "heal 2: r = g = 0.7, g = 1.0");
        assertEquals(0xFFFFFFFF, ModelGolem.healingTint(2, 2), ":81 the tint is pass 0 only");
        // RenderGolemBase.java:128 glColor4f(1, 1, 1, 1 - healthPercentage)
        assertEquals(0xFFFFFFFF, RenderGolemBase.damageTint(0.0F), "a golem at full health hides the pass");
        assertEquals(0x7FFFFFFF, RenderGolemBase.damageTint(0.5F),
                "half health: alpha truncates to 127, so the packed int is 127 << 24 | 0xFFFFFF");
        assertEquals(0x00FFFFFF, RenderGolemBase.damageTint(1.0F), "death, fully transparent");
    }

    @Test
    void theDecorationAndDamageGatesAreTheOriginalConditions() {
        // :119 (deco.length() > 0 || advanced) and :125 (healthPercentage < 1).
        // The gates are pure on the two figures they read, so they are pinned here.
        assertTrue(RenderGolemBase.showsDecorationFor("P", true));
        assertTrue(RenderGolemBase.showsDecorationFor("P", false));
        assertTrue(RenderGolemBase.showsDecorationFor("", true), "advanced alone opens the layer");
        assertTrue(!RenderGolemBase.showsDecorationFor("", false), "an undecorated, plain golem draws nothing");
        assertTrue(RenderGolemBase.showsDamageFor(0.999F));
        assertTrue(!RenderGolemBase.showsDamageFor(1.0F), "a healthy golem skips the damage pass");
    }

    @Test
    void theBobberBallIsTheOriginalTileOfVanillasParticleSheet() {
        // RenderGolemBobber.java:25-30, b0 = 1, b1 = 2 on a 128x128 sheet of 8x8 tiles
        assertArrayEquals(new float[] {0.0625F, 0.125F, 0.125F, 0.1875F}, RenderGolemBobber.ballUv(1, 2), EPS,
                "f2 = (1*8+0)/128, f3 = (1*8+8)/128, f4 = (2*8+0)/128, f5 = (2*8+8)/128");
        // :33-34 f7 = f8 = 0.5; :38-41 the four corners and their UV pairs
        assertArrayEquals(new float[] {-0.5F, -0.5F, 0.0F, 0.0625F, 0.1875F,
                0.5F, -0.5F, 0.0F, 0.125F, 0.1875F,
                0.5F, 0.5F, 0.0F, 0.125F, 0.125F,
                -0.5F, 0.5F, 0.0F, 0.0625F, 0.125F}, RenderGolemBobber.ballCorners(RenderGolemBobber.ballUv(1, 2)),
                EPS, "the ball quad, in the source's vertex order");
        assertEquals(16, RenderGolemBobber.STRIP_SEGMENTS, ":77 byte b2 = 16 segments");
    }

    @Test
    void theBobberLineSagIsTheOriginalStripFormula() {
        // :81 addVertex(xx + d11*f12, yy + d12*(f12*f12 + f12)*0.5 + 0.25, zz + d13*f12)
        assertArrayEquals(new float[] {0.0F, 0.25F, 0.0F}, RenderGolemBobber.linePoint(10.0, 4.0, -2.0, 0.0F),
                EPS, "t = 0 sits at the ball's top (+0.25, :68 d8 = y + 0.25)");
        assertArrayEquals(new float[] {5.0F, 1.75F, -1.0F}, RenderGolemBobber.linePoint(10.0, 4.0, -2.0, 0.5F),
                EPS, "t = 0.5: y = 4*(0.25+0.5)*0.5 + 0.25");
        assertArrayEquals(new float[] {10.0F, 4.25F, -2.0F}, RenderGolemBobber.linePoint(10.0, 4.0, -2.0, 1.0F),
                EPS, "t = 1 lands on the hand (d12 measured from d8)");
    }

    @Test
    void theIconQuadsKeepTheOriginalVertexOrderAndUvTransposition() {
        // ItemRenderer.func_78439_a (:84, :164, :213): (0,0,z) uv00, (w,0,z) uv10,
        // (w,h,z) uv11, (0,h,z) uv01 — 16x16 icon units for the core icon.
        assertArrayEquals(new float[] {0.0F, 0.0F, 0.2F, 0.0F, 0.5F,
                16.0F, 0.0F, 0.2F, 0.25F, 0.5F,
                16.0F, 16.0F, 0.2F, 0.25F, 0.75F,
                0.0F, 16.0F, 0.2F, 0.0F, 0.75F},
                RenderGolemBase.iconQuadCorners(new float[] {0.0F, 0.25F, 0.5F, 0.75F}, 16.0F, 16.0F, 0.2F),
                EPS, "the core icon's quad");
        // :108-111 the upgrade strip's unit quad: (f1,f4) (f3,f4) (f3,f2) (f1,f2)
        assertArrayEquals(new float[] {0.0F, 0.0F, 0.0F, 0.0F, 0.5F,
                1.0F, 0.0F, 0.0F, 0.25F, 0.5F,
                1.0F, 1.0F, 0.0F, 0.25F, 0.75F,
                0.0F, 1.0F, 0.0F, 0.0F, 0.75F},
                RenderGolemBase.stripQuadCorners(new float[] {0.0F, 0.5F, 0.25F, 0.75F}), EPS,
                "the upgrade slot quad");
        // :79-84 the source hands (minV, maxV, minU, maxU) into the callee's
        // (minU, maxU, minV, maxV) slots — a transposition, ported as-is.
        assertArrayEquals(new float[] {0.5F, 0.75F, 0.0F, 0.25F},
                RenderGolemBase.transposedUv(0.0F, 0.25F, 0.5F, 0.75F), EPS,
                "the UV transposition of the reference's argument order");
    }

    @Test
    void theCoreAndUpgradeTransformsAreThePinnedFiguresOfTheReference() {
        // :73-76 rotate180(X), translate(0.0875, -0.96, 0.15 + deco.contains("P")?0.03:0),
        // scale 0.175, rotate180(Y); :89-98 strip at -0.05 - shift*(n-1)/2 + shift*a, -1.106,
        // 0.099, scale 0.1, shift 0.08.
        assertArrayEquals(new float[] {0.0875F, -0.96F, 0.18F},
                RenderGolemBase.coreIconTranslation("P"), EPS, "a decorated golem lifts the core icon 0.03");
        assertArrayEquals(new float[] {0.0875F, -0.96F, 0.15F},
                RenderGolemBase.coreIconTranslation("B"), EPS, "undecorated: the plain 0.15");
        assertEquals(0.175F, RenderGolemBase.CORE_ICON_SCALE, EPS, ":75");
        assertEquals(0.08F, RenderGolemBase.UPGRADE_SHIFT, EPS, ":89 float f6 = 0.08F");
        // three upgrades: slots at -0.05 - 0.08*1 = -0.13, -0.05, 0.03
        assertArrayEquals(new float[] {-0.13F, -0.05F, 0.03F}, RenderGolemBase.upgradeSlots(3), EPS, ":97");
        assertEquals(-1.106F, RenderGolemBase.UPGRADE_Y, EPS, ":97");
        assertEquals(0.099F, RenderGolemBase.UPGRADE_Z, EPS, ":97");
        assertEquals(0.1F, RenderGolemBase.UPGRADE_SCALE, EPS, ":98");
    }

    @Test
    void theSwayIsTheOriginalSixAndAHalfDegreeBodyRock() {
        // :141-142 glRotatef(6.5F * func_78172_a(var4 + 6, 13), 0, 0, 1)
        assertEquals(6.5F, RenderGolemBase.SWAY_DEGREES, EPS, "the amplitude");
        assertEquals(13.0F, RenderGolemBase.SWAY_PERIOD, EPS, "the wave period");
        assertEquals(6.0F, RenderGolemBase.SWAY_PHASE_OFFSET, EPS, "the + 6.0F of :140");
        // The wave's own extremes: +1 at phase 0 and -1 at half a period, so the
        // body rocks to exactly +/- the amplitude, never past it.
        assertEquals(6.5F, RenderGolemBase.SWAY_DEGREES * ModelGolem.wave(0.0F, RenderGolemBase.SWAY_PERIOD), EPS,
                "the peak of the rock");
        assertEquals(-6.5F, RenderGolemBase.SWAY_DEGREES * ModelGolem.wave(6.5F, RenderGolemBase.SWAY_PERIOD), EPS,
                "the trough of the rock");
    }

    // ---------------------------------------------------- wiring level

    @Test
    void wiringIsModBusClientOnlyAndPassesTheThreeLiveHolders() throws Exception {
        final String source = readSource("client/entity/ModEntityRenderers.java");
        assertTrue(source.contains("@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID,"),
                "the renderer wiring must be self-registering for this mod");
        assertTrue(source.contains("bus = EventBusSubscriber.Bus.MOD"),
                "EntityRenderersEvent is a mod-bus event in NeoForge 21.1");
        assertTrue(source.contains("value = Dist.CLIENT"),
                "client-only, so a dedicated server never loads the renderers");
        assertTrue(source.contains("@SubscribeEvent"), "21.1 needs @SubscribeEvent on the handlers");
        assertTrue(source.contains("EntityRenderersEvent.RegisterRenderers event"),
                "the renderer handler must take RegisterRenderers");
        assertTrue(source.contains("EntityRenderersEvent.RegisterLayerDefinitions event"),
                "the layer handler must take RegisterLayerDefinitions");
        assertTrue(source.contains("CatalogGolems.GOLEM.get()")
                        && source.contains("CatalogGolems.ELDRITCH_GOLEM.get()")
                        && source.contains("CatalogGolems.GOLEM_BOBBER.get()"),
                "the production path passes the live thaumcraft:golem / eldritchgolem / golembobber holders");
        assertTrue(source.contains("event.registerEntityRenderer(golem, RenderGolemBase::new)")
                        && source.contains("event.registerEntityRenderer(eldritch, RenderEldritchGolem::new)")
                        && source.contains("event.registerEntityRenderer(bobber, RenderGolemBobber::new)"),
                "the seam pairs each type with its renderer");
        assertTrue(source.contains("event.registerLayerDefinition(GOLEM_LAYER, ModelGolem::layer)")
                        && source.contains("event.registerLayerDefinition(GOLEM_ACCESSORIES_LAYER, ModelGolemAccessories::layer)")
                        && source.contains("event.registerLayerDefinition(ELDRITCH_GOLEM_LAYER, ModelEldritchGolem::layer)"),
                "the four model layers are registered from their own suppliers");
    }

    @Test
    void rendererClassesDoNotSelfRegisterAndTheGolemFamilyIsTheOnlyThingRegistered() throws Exception {
        for (final String name : new String[] {"RenderGolemBase.java", "RenderGolemBobber.java",
                "RenderEldritchGolem.java", "ModelGolem.java", "ModelGolemAccessories.java",
                "ModelEldritchGolem.java"}) {
            final String source = readSource("client/entity/" + name);
            assertTrue(!source.contains("@EventBusSubscriber"), name + " must not self-register");
        }
        final String wiring = readSource("client/entity/ModEntityRenderers.java");
        // The card's scope: three renderers, and the dart/orb renderers are not this card's.
        assertTrue(!wiring.contains("CatalogGolems.DART"), "the dart renderer is a separate reference class");
        assertTrue(!wiring.contains("CatalogGolems.GOLEM_ORB"), "the orb has no renderer in the reference's golem block");
    }

    @Test
    void theSixClassesAreTheCardsSixClasses() throws Exception {
        // No invented class names: every one of the six exists, and the renderer for
        // a golem is the one the reference's proxy hands the entity type.
        for (final Class<?> expected : new Class<?>[] {RenderGolemBase.class, ModelGolem.class,
                ModelGolemAccessories.class, RenderGolemBobber.class, RenderEldritchGolem.class,
                ModelEldritchGolem.class}) {
            assertEquals("com.jonnyutah12.thaumcraftclassic.client.entity", expected.getPackageName(),
                    expected.getSimpleName() + " lives in the port's client/entity package");
        }
        // ModelGolem keeps the original's public `pass` field (line 18).
        assertEquals(0, new ModelGolem(ModelGolem.layer().bakeRoot()).pass, "ModelGolem.java:18 pass = 0");
        final ModelGolem damage = new ModelGolem(ModelGolem.layer().bakeRoot());
        damage.pass = 2; // :56
        assertEquals(2, damage.pass, "the damage copy of RenderGolemBase.java:56");
    }

    // ---------------------------------------------------- harness helpers

    private static void assertSheet(final ResourceLocation texture, final String file, final EnumGolemType material) {
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/" + file), texture,
                "RenderGolemBase.java's sheet for " + material);
        assertEquals(texture, RenderGolemBase.textureFor(material),
                "func_110775_a (:269-290) must return the sheet for " + material);
    }

    /**
     * The renderer's shadow radius. 1.21.1 makes {@code getShadowRadius(T)}
     * protected and entity-dependent, so the value the constructor stored is
     * read from the field the platform keeps it in.
     */
    private static float shadowRadiusOf(final EntityRenderer<?> renderer) {
        try {
            final Field field = EntityRenderer.class.getDeclaredField("shadowRadius");
            field.setAccessible(true);
            return field.getFloat(renderer);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("EntityRenderer.shadowRadius moved: " + e.getMessage(), e);
        }
    }

    /** The golem renderer, built from the provider the real event stored. */
    @SuppressWarnings("unchecked")
    private static RenderGolemBase golemRenderer() throws Exception {
        return (RenderGolemBase) throughTheSeam(EntityType.PLAYER, RenderGolemBase::new);
    }

    /** The eldritch renderer, built from the provider the real event stored. */
    @SuppressWarnings("unchecked")
    private static RenderEldritchGolem eldritchRenderer() throws Exception {
        return (RenderEldritchGolem) throughTheSeam(EntityType.PLAYER, RenderEldritchGolem::new);
    }

    /** The bobber renderer, built from the provider the real event stored. */
    @SuppressWarnings("unchecked")
    private static RenderGolemBobber bobberRenderer() throws Exception {
        return (RenderGolemBobber) throughTheSeam(EntityType.PLAYER, RenderGolemBobber::new);
    }

    /**
     * Register one renderer through {@code RegisterRenderers} on a throwaway
     * vanilla key (a unit harness has the registries frozen), then build it from
     * the provider 21.1 stored. A context with no parts: the models are
     * self-baked, as the 1.7.10 proxy baked them ({@code new ModelGolem(false)}
     * at ClientProxy.java:416), so a provider needs no client.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EntityRenderer<?> throughTheSeam(final EntityType<?> key,
            final EntityRendererProvider provider) throws Exception {
        final EntityRenderersEvent.RegisterRenderers event = new EntityRenderersEvent.RegisterRenderers();
        event.registerEntityRenderer((EntityType) key, provider);
        return vanillaProviders().get(key).create(
                new EntityRendererProvider.Context(null, null, null, null, null, null, null));
    }

    /** NeoForge 21.1's own layer-definition map, behind ClientHooks (private). */
    @SuppressWarnings("unchecked")
    private static Map<ModelLayerLocation, Supplier<net.minecraft.client.model.geom.builders.LayerDefinition>>
            vanillaLayerDefinitions() {
        try {
            final Field field = ClientHooks.class.getDeclaredField("layerDefinitions");
            field.setAccessible(true);
            return (Map<ModelLayerLocation, Supplier<net.minecraft.client.model.geom.builders.LayerDefinition>>)
                    field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ClientHooks.layerDefinitions moved: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<EntityType<?>, EntityRendererProvider<?>> vanillaProviders() throws Exception {
        final Field field = EntityRenderers.class.getDeclaredField("PROVIDERS");
        field.setAccessible(true);
        return (Map<EntityType<?>, EntityRendererProvider<?>>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static List<ModelPart.Cube> cubes(final ModelPart part) {
        try {
            final Field field = ModelPart.class.getDeclaredField("cubes");
            field.setAccessible(true);
            return (List<ModelPart.Cube>) field.get(part);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ModelPart.cubes moved: " + e.getMessage(), e);
        }
    }

    /**
     * A baked tree's child parts. 1.21.1's {@code ModelPart} keeps its
     * {@code children} map private (only {@code getChild}/{@code hasChild} are
     * public), so the names are read reflectively — the same route the cube
     * bounds are read, and baking itself is the production code path.
     */
    @SuppressWarnings("unchecked")
    private static java.util.Map<String, ModelPart> childrenOf(final ModelPart part) {
        try {
            final Field field = ModelPart.class.getDeclaredField("children");
            field.setAccessible(true);
            return (java.util.Map<String, ModelPart>) field.get(part);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("ModelPart.children moved: " + ex.getMessage(), ex);
        }
    }

    private static java.util.Set<String> partNames(final ModelPart root) {
        return childrenOf(root).keySet();
    }

    private static void assertPose(final ModelPart part, final float x, final float y, final float z) {
        assertEquals(x, part.x, EPS, "rotation point x of " + part);
        assertEquals(y, part.y, EPS, "rotation point y of " + part);
        assertEquals(z, part.z, EPS, "rotation point z of " + part);
    }

    /**
     * The box a cube actually draws. 1.21.1 applies {@code CubeDeformation} to
     * the polygon vertices (the cube's public {@code minX..maxZ} keep the
     * undeformed origin), so parity for a deformed box is measured on the
     * geometry — the same coordinates {@code ModelPart.Cube.compile} submits.
     * For an undeformed box this agrees with {@code minX..maxZ} exactly, which
     * the chest's first box pins.
     */
    private static float[] cubeVertexBounds(final ModelPart.Cube cube) {
        try {
            final Field polygonsField = ModelPart.Cube.class.getDeclaredField("polygons");
            polygonsField.setAccessible(true);
            final Object[] polygons = (Object[]) polygonsField.get(cube);
            final Field verticesField = polygons[0].getClass().getDeclaredField("vertices");
            verticesField.setAccessible(true);
            final float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                    -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            for (final Object polygon : polygons) {
                for (final Object vertex : (Object[]) verticesField.get(polygon)) {
                    final Field posField = vertex.getClass().getDeclaredField("pos");
                    posField.setAccessible(true);
                    final org.joml.Vector3f pos = (org.joml.Vector3f) posField.get(vertex);
                    b[0] = Math.min(b[0], pos.x);
                    b[1] = Math.min(b[1], pos.y);
                    b[2] = Math.min(b[2], pos.z);
                    b[3] = Math.max(b[3], pos.x);
                    b[4] = Math.max(b[4], pos.y);
                    b[5] = Math.max(b[5], pos.z);
                }
            }
            return b;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ModelPart.Cube geometry moved: " + e.getMessage(), e);
        }
    }

    private static void assertVertexBounds(final ModelPart.Cube cube, final float minX, final float minY,
            final float minZ, final float maxX, final float maxY, final float maxZ) {
        assertArrayEquals(new float[] {minX, minY, minZ, maxX, maxY, maxZ}, cubeVertexBounds(cube), EPS,
                "the drawn box, read off the polygon vertices");
    }

    private static void assertCube(final ModelPart part, final int index, final float minX, final float minY,
            final float minZ, final float maxX, final float maxY, final float maxZ) {
        assertBounds(cubes(part).get(index), minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void assertBounds(final ModelPart.Cube cube, final float minX, final float minY, final float minZ,
            final float maxX, final float maxY, final float maxZ) {
        assertEquals(minX, cube.minX, EPS, "box minX");
        assertEquals(minY, cube.minY, EPS, "box minY");
        assertEquals(minZ, cube.minZ, EPS, "box minZ");
        assertEquals(maxX, cube.maxX, EPS, "box maxX");
        assertEquals(maxY, cube.maxY, EPS, "box maxY");
        assertEquals(maxZ, cube.maxZ, EPS, "box maxZ");
    }

    private static Class<?> typeArgument(final Class<?> rendererClass) {
        final java.lang.reflect.Type superType = rendererClass.getGenericSuperclass();
        assertTrue(superType instanceof java.lang.reflect.ParameterizedType, "the base must be parameterized");
        return (Class<?>) ((java.lang.reflect.ParameterizedType) superType).getActualTypeArguments()[0];
    }

    private static Class<?> modelTypeArgument(final Class<?> rendererClass) {
        final java.lang.reflect.Type superType = rendererClass.getGenericSuperclass();
        return (Class<?>) ((java.lang.reflect.ParameterizedType) superType).getActualTypeArguments()[1];
    }

    private static String readSource(final String relativeToSourceRoot) throws Exception {
        final java.nio.file.Path csv = java.nio.file.Path.of(
                System.getProperty("tc.contentMappingFile", ""));
        final java.nio.file.Path repoRoot = csv.getParent().getParent();
        final java.nio.file.Path source = repoRoot.resolve(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/" + relativeToSourceRoot);
        assertTrue(java.nio.file.Files.isRegularFile(source), "expected the source at " + source);
        return java.nio.file.Files.readString(source, java.nio.charset.StandardCharsets.UTF_8);
    }
}
