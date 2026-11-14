package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityMindSpider;
import com.jonnyutah12.thaumcraftclassic.entities.monster.MindSpiderAiLogic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogMindSpider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M7-03-r's acceptance for the mind spider's render layer: {@code
 * RenderMindSpider} and the registration seam that puts it on
 * {@code thaumcraft:mindspider}.
 *
 * <p>What is proved, and at which level — the split the golem card
 * ({@code GolemRendererRegistrationTest}) and the wisp card
 * ({@code WispRendererRegistrationTest}) established, because {@code
 * runGameTestServer} is a dedicated server and can never construct a
 * renderer:</p>
 * <ol>
 *   <li><b>Contract level (plain JVM):</b> the renderer sits on {@link
 *       MobRenderer} — 1.21.1's twin of the {@code RenderLiving} the reference
 *       extends at {@code RenderMindSpider.java:18}, not on the platform's
 *       {@code SpiderRenderer}, whose 1.7.10 parent {@code RenderSpider} is the
 *       class the reference <i>copied</i> — is parameterized on the port's own
 *       entity and spider model, carries the provider constructor 21.1's
 *       registration demands, and addresses both original sheets by literal path
 *       ({@code :19}, {@code :80}).</li>
 *   <li><b>Registry level (plain JVM):</b> the real {@code RegisterRenderers}
 *       event is fired through the production seam {@link
 *       ModEntityRenderers#registerMindSpider}; the type ends up in 21.1's own
 *       {@code EntityRenderers.PROVIDERS} mapped to a provider that builds a
 *       {@code RenderMindSpider}. This is the assertion that makes "the mob
 *       shipped and nothing draws it" loud: the route the client takes is
 *       executed, not described.</li>
 *   <li><b>Figure level (plain JVM):</b> the 0.3 of {@code :101-108} is applied
 *       to the model pose by {@code scale}, is the same number as the entity
 *       box's 0.3, and is <b>not</b> the {@code minecraft:generic.scale}
 *       attribute — BD-78's measured decision, pinned so a later card cannot
 *       re-add the scale a second time and shrink the spider twice.</li>
 *   <li><b>Wiring level (source-level):</b> the production path hands the live
 *       {@code CatalogMindSpider.MIND_SPIDER} holder to the seam, the spider's
 *       geometry is self-baked (no layer registration, because that layer id is
 *       vanilla's own), and the renderer is client-only and not self-registering
 *       — so {@code runServer} never loads it.</li>
 * </ol>
 *
 * <p>What is NOT proved here, and is {@code not_proved} in the matrix: the
 * pixels. Both sheets are All Rights Reserved and live in the local-build
 * overlay only (D-004, rows 1035/1036 of {@code docs/asset-mapping.csv}), the
 * 1.7.10 spider geometry is not the 1.21.1 {@code SpiderModel} skeleton part for
 * part, and the fade of {@code :55-70} is expressed through 1.21.1's
 * model-prepare + render-type seams rather than a per-call {@code glColor4f}.
 * All of that is BD-79, and a human client look is what closes it.</p>
 */
class MindSpiderRendererRegistrationTest {

    private static final float EPS = 1.0E-4F;
    private static final Path MAIN = repoRoot()
            .resolve("src/main/java/com/jonnyutah12/thaumcraftclassic");

    /** The repo root, read from the property the build wires into the test JVM. */
    private static Path repoRoot() {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        if (!Files.isRegularFile(csv)) {
            throw new AssertionError("content-mapping.csv not wired - run tests through ./gradlew test");
        }
        return csv.getParent().getParent();
    }

    // ---------------------------------------------------- contract level

    @Test
    void theMindSpiderRendererIsTheLivingRendererTheReferenceExtends() throws Exception {
        assertEquals(MobRenderer.class, RenderMindSpider.class.getSuperclass(),
                "RenderMindSpider.java:18 — extends RenderLiving; 1.21.1's twin is MobRenderer");
        assertTrue(SpiderRenderer.class.isAssignableFrom(RenderMindSpider.class) == false,
                "the reference extends RenderLiving, not RenderSpider (RenderTaintSpider.java:20,26,31 "
                        + "carry the RenderSpider literals); inheriting SpiderRenderer would import "
                        + "the leg-shrink and the vanilla eye sheet, which this file does not have");
        assertSame(EntityMindSpider.class, typeArgument(RenderMindSpider.class),
                "the renderer is parameterized on the port's mind spider");
        assertSame(SpiderModel.class, modelArgument(RenderMindSpider.class),
                "and on the platform's spider model, the twin of the reference's ModelSpider (:22)");
        final java.lang.reflect.Constructor<?> ctor =
                RenderMindSpider.class.getConstructor(EntityRendererProvider.Context.class);
        assertTrue(Modifier.isPublic(ctor.getModifiers()),
                "21.1's registration takes a provider, so the (Context) constructor must be public");
        // :22 super(new ModelSpider(), 0.0F) — no shadow.
        assertEquals(0.0F, RenderMindSpider.SHADOW_RADIUS, EPS, "the figure the constructor hands super");
        assertEquals(0.0F, shadowRadiusOf(mindSpiderRenderer()), EPS, "the radius 21.1 stored");
        // :26-28 via :110-112 — setSpiderDeathMaxRotation(180.0F).
        assertEquals(180.0F, RenderMindSpider.DEATH_FLIP_DEGREES, EPS, "the reference's death flip");
        assertEquals(180.0F, mindSpiderRenderer().getFlipDegrees(null), EPS, "1.21.1's hook of the same figure");
        // The platform's own spider uses the same figure (SpiderRenderer.java:25-27
        // returns 180.0F), so the port is not inventing a flip; 1.21.1's default
        // elsewhere is 90.0F (LivingEntityRenderer.java:227-229), which is what a
        // renderer that forgot this hook would draw. That default is read from the
        // decompiled source, not asserted here: every vanilla living renderer bakes
        // its model through the context, so a unit harness with a bare context
        // cannot construct one (only this port's self-baking renderers can).
    }

    @Test
    void theMindSpiderDrawsTheReferenceSheetAndTheArrEyeSheet() throws Exception {
        // :19 the body, :80 the eyes. Both are referenced by path only (D-004).
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/taint_spider.png"),
                RenderMindSpider.SPIDER_TEXTURE, "RenderMindSpider.java:19");
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/taint_spider_eyes.png"),
                RenderMindSpider.SPIDER_EYES_TEXTURE, "RenderMindSpider.java:80 (UtilsFX.bindTexture)");
        final RenderMindSpider renderer = mindSpiderRenderer();
        assertEquals(RenderMindSpider.SPIDER_TEXTURE, renderer.getTextureLocation(null), ":30-32");
        // The eye layer is this file's own: 1.21.1's SpiderEyesLayer hardcodes
        // vanilla's sheet and takes no texture (SpiderEyesLayer.java:13,15).
        final RenderMindSpider.MindSpiderEyesLayer eyes =
                new RenderMindSpider.MindSpiderEyesLayer(renderer);
        assertEquals(RenderType.eyes(RenderMindSpider.SPIDER_EYES_TEXTURE), eyes.renderType(),
                "RenderType.eyes is 1.21.1's additive eye shader (:84 ONE,ONE and :83 no alpha test)");
        assertEquals(0xF000F0, RenderMindSpider.EYES_LIGHT,
                ":91-94 — char c0 = '\\uf0f0', out of 1.7.10's 0..240 lightmap range, clamped full bright");
        assertEquals(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, RenderMindSpider.EYES_LIGHT,
                "and 1.21.1's name for that value");
        assertEquals(0xFFFFFFFF, RenderMindSpider.EYES_COLOR, ":95-96 glColor4f(1,1,1,1)");
    }

    @Test
    void theRendererAddsTheEyeLayerAndNoPassTheReferenceDoesNot() throws Exception {
        // :72-74 is a renderLayers override that calls super and nothing, so this
        // renderer's only addition is the eye pass; a second invented pass would
        // put pixels on screen that the original never drew.
        assertEquals(1, layersOf(mindSpiderRenderer()).size(),
                ":72-74 — exactly one layer, the eye pass of :76-99");
        final String source = readSource("client/entity/RenderMindSpider.java");
        assertTrue(!source.contains("Tessellator") && !source.contains("addVertex"),
                "the reference draws no tessellator quad of its own");
        assertTrue(source.contains("this.addLayer(new MindSpiderEyesLayer(this))"),
                "the one layer, added in the constructor");
        assertTrue(source.contains("getParentModel().renderToBuffer(poseStack, buffer.getBuffer(EYES), EYES_LIGHT"),
                ":91-96 — the eye pass hands the model its own light and colour, through the "
                        + "5-argument renderToBuffer, so the body fade cannot leak in");
    }

    // ---------------------------------------------------- registry level

    @Test
    @SuppressWarnings("unchecked")
    void firingTheSeamRoutesTheMindSpiderIntoVanillasProviderMapAndBuildsIt() throws Exception {
        // The keys are incidental: a unit harness has the registries frozen, so the
        // live CatalogMindSpider holder cannot be resolved here and a vanilla spider
        // type stands in — it is a Mob, so the living binding the seam requires is
        // the same one thaumcraft:mindspider takes in a real client.
        final Map<EntityType<?>, EntityRendererProvider<?>> providers = vanillaProviders();
        // SPIDER is this test's own stand-in key, so nothing registered it earlier:
        // in a plain JVM EntityRenderers.bootstrap() has not run and the map is
        // empty, which is what makes the "entry appears" claim below the client's
        // own evidence route — 21.1 draws from this map, nothing else.
        final EntityType<EntityMindSpider> key = (EntityType<EntityMindSpider>) (EntityType<?>) EntityType.SPIDER;
        final EntityRendererProvider<?> before = providers.get(key);

        final EntityRenderersEvent.RegisterRenderers event = new EntityRenderersEvent.RegisterRenderers();
        ModEntityRenderers.registerMindSpider(event, key);

        assertTrue(providers.containsKey(key),
                "the registration must land in EntityRenderers' own map, the map the client draws from");
        assertTrue(before != providers.get(key),
                "RegisterRenderers -> EntityRenderers.register must write the entry for the type handed in");
        // A context with no parts: the spider geometry is self-baked (the 1.7.10
        // proxy hands the renderer no model, ClientProxy.java:450), so a provider
        // builds the renderer without a client.
        final EntityRenderer<?> built = providers.get(key).create(
                new EntityRendererProvider.Context(null, null, null, null, null, null, null));
        assertInstanceOf(RenderMindSpider.class, built, "the stored provider must build RenderMindSpider "
                + "(it pins the RenderMindSpider::new the seam hands the event)");
    }

    // ---------------------------------------------------- figure level

    @Test
    void theZeroPointThreeScaleIsTheRenderersAndNotTheEntitysAttribute() throws Exception {
        // :101-108 — glScalef(f1, f1, f1) with f1 = spiderScaleAmount() = 0.3F
        // (reference EntityMindSpider.java:67-70). In 1.21.1 that is the scale hook.
        assertEquals(0.3F, RenderMindSpider.SPIDER_SCALE, EPS, "the reference's spiderScaleAmount()");
        assertEquals(MindSpiderAiLogic.SCALE, RenderMindSpider.SPIDER_SCALE, EPS,
                "one number in this port: the renderer's scale is the entity box's 0.3, not a second figure");
        final RenderMindSpider renderer = mindSpiderRenderer();
        final PoseStack pose = new PoseStack();
        pose.pushPose();
        renderer.scale(null, pose, 0.0F);
        final float scaled = pose.last().pose().transformPosition(new org.joml.Vector3f(1.0F, 0.0F, 0.0F)).x();
        assertEquals(0.3F, scaled, EPS, "scale() multiplies the model pose by 0.3");
        pose.popPose();
        assertEquals(1.0F, pose.last().pose().m00(), EPS, "the hook must be applied on a pushed pose");

        // The other half of BD-78's measured decision: the box is already 0.3, so
        // the attribute must NOT also carry it — 1.21.1 multiplies the pose by
        // generic.scale (LivingEntityRenderer.java:97-98) before scale() runs, and
        // 0.3 x 0.3 is the double-shrink the ledger forbids.
        assertEquals(0.3F, MindSpiderAiLogic.WIDTH, EPS, "the registered box, EntityType.Builder.sized(...)");
        assertEquals(0.3F, MindSpiderAiLogic.HEIGHT, EPS, "the registered box");
        final String entity = readSource("entities/monster/EntityMindSpider.java");
        final String catalog = readSource("registration/catalog/CatalogMindSpider.java");
        assertTrue(entity.contains(".sized(MindSpiderAiLogic.WIDTH, MindSpiderAiLogic.HEIGHT)"),
                "the box is the same 0.3, registered once");
        // The grep token is the API's real name, read out of the decompiled MC
        // sources: the attribute is Attributes#SCALE (id "generic.scale"), read by
        // LivingEntity.getScale() (:552-554) and multiplied into the pose at
        // LivingEntityRenderer.java:97-98. Grepping only "GENERIC_SCALE" — the
        // pre-1.20.5 name — stays GREEN straight through the real mutation, because
        // the builder call in this API is `.add(Attributes.SCALE, 0.3)`. Proven by
        // mutation: with that line added to createAttributes(), the old assertion
        // passed and the double-shrink shipped.
        for (final String source : List.of(entity, catalog)) {
            assertTrue(!source.contains("Attributes.SCALE") && !source.contains("GENERIC_SCALE"),
                    "generic.scale must stay at its 1.0 default: the renderer owns the 0.3, so a second "
                            + "0.3 on the attribute would shrink the spider twice");
        }
    }

    @Test
    void theFadeIsTheOriginalAlphaCapAndTheBodyIsTranslucent() throws Exception {
        // :60 glColor4f(1, 1, 1, min(0.1F, age / 100.0F)) — a cap of 10 %, not a
        // fade to opaque. Read as a fade that caps at 10 %, and BD-79 says so.
        assertEquals(0.1F, RenderMindSpider.FADE_ALPHA_CAP, EPS, ":60 Math.min(0.1F, …)");
        assertEquals(100.0F, RenderMindSpider.FADE_AGE_DIVISOR, EPS, ":60 age / 100.0F");
        assertEquals(0.0F, RenderMindSpider.fadeAlpha(0), EPS, "age 0: no alpha at all");
        assertEquals(0.05F, RenderMindSpider.fadeAlpha(5), EPS, "age 5: 5/100");
        assertEquals(0.1F, RenderMindSpider.fadeAlpha(10), EPS, "age 10 reaches the cap");
        assertEquals(0.1F, RenderMindSpider.fadeAlpha(1200), EPS, "and never passes it");
        assertEquals(0x19FFFFFF, RenderMindSpider.fadeColor(10),
                "the alpha is the top byte of the ARGB int 1.21.1 hands a model: 0.1 * 255 = 25 = 0x19");
        assertEquals(0x00FFFFFF, RenderMindSpider.fadeColor(0), "age 0 is fully transparent white");
        // :62-63 GL_BLEND + :61 depth mask off -> 1.21.1's entity translucent type.
        final RenderMindSpider renderer = mindSpiderRenderer();
        assertEquals(RenderType.entityTranslucent(RenderMindSpider.SPIDER_TEXTURE),
                renderer.getRenderType(null, true, false, false),
                "the blended body draw is RenderType.entityTranslucent (RenderType.java:153-165, :850-852)");
        // The invisible, translucent and glowing arms stay the platform's: the
        // override fires only on the visible-body arm, so 1.21.1's own 15 %
        // translucent and outline draws survive (LivingEntityRenderer.java:143-150).
        // The invisible arm is left to super, which reads the entity (the
        // platform's armor/cull checks) — a harness with no entity cannot walk
        // it, so the delegation is pinned where it is written.
        assertTrue(readSource("client/entity/RenderMindSpider.java")
                        .contains(": super.getRenderType(spider, bodyVisible, translucent, glowing)"),
                "the non-visible arm delegates to super, so the platform's invisible/cull types survive");
        assertTrue(renderer.getRenderType(null, true, true, false)
                        .equals(RenderType.entityTranslucent(RenderMindSpider.SPIDER_TEXTURE)),
                "the reference overrides no translucent arm of its own, so the blended draw is the same");
    }

    @Test
    void theViewerGateIsTheOriginalThreeArmsCollapsedToOne() throws Exception {
        // :34-53 — all three doRender overloads carry the same test: draw when the
        // viewer is empty, or when the viewer is the local player's name.
        assertTrue(RenderMindSpider.drawsForViewer("", "Steve"),
                ":35 getViewer().length() == 0 — nobody mind-controlled it, everybody sees it");
        assertTrue(RenderMindSpider.drawsForViewer("Steve", "Steve"), ":35-36 the second arm");
        assertTrue(RenderMindSpider.drawsForViewer("Alex", "Steve") == false,
                "a mind-controlled spider is invisible to every other player");
        assertTrue(RenderMindSpider.drawsForViewer("Steve", null) == false,
                "a null local name can only satisfy the empty arm — 1.7.10 would have dereferenced "
                        + "a null current player on the second arm (:49)");
        final String source = readSource("client/entity/RenderMindSpider.java");
        assertEquals(1, occurrences(source, "public boolean shouldRender("),
                "1.21.1 has one visibility entry point, so the three overloads collapse to one override");
        assertTrue(source.contains("Minecraft.getInstance().player"),
                ":49 reads the render manager's current player; 1.21.1's twin is Minecraft.getInstance().player");
        final String entity = readSource("entities/monster/EntityMindSpider.java");
        assertTrue(entity.contains("EntityDataSerializers.STRING"),
                ":35 — watcher 23 is the synced string whose default is the empty name");
        assertTrue(entity.contains("public String getViewer()") && entity.contains("public void setViewer("),
                ":38-44 — the pair the renderer reads and the mind-control lane would write");
        assertTrue(writerOfViewerName().isEmpty(),
                "the seam exists, and no shipped class writes it — the empty arm is the live one "
                        + "until the mind-control lane (t_1a83f0db) lands, exactly as BD-78 records: "
                        + writerOfViewerName());
    }

    // ---------------------------------------------------- wiring level

    @Test
    void theWiringPassesTheLiveMindSpiderHolderAndRegistersNoSpiderLayer() throws IOException {
        final String wiring = readSource("client/entity/ModEntityRenderers.java");
        assertTrue(wiring.contains("registerMindSpider(event, CatalogMindSpider.MIND_SPIDER.get())"),
                "the production path binds the live thaumcraft:mindspider holder");
        assertTrue(wiring.contains("event.registerEntityRenderer(mindspider, RenderMindSpider::new)"),
                "the seam pairs the type with the renderer class, the twin of ClientProxy.java:450");
        assertTrue(wiring.contains("static void registerMindSpider(final EntityRenderersEvent.RegisterRenderers event,")
                        && wiring.contains("final EntityType<EntityMindSpider> mindspider)"),
                "the seam is its own, so the golem card's seam and test stay untouched");
        assertTrue(wiring.contains("bus = EventBusSubscriber.Bus.MOD") && wiring.contains("value = Dist.CLIENT"),
                "the wiring is mod-bus and client-only: a dedicated server never loads the renderer");
        assertTrue(wiring.contains("CatalogMindSpider.MIND_SPIDER.get()"),
                "the registration resolves the real holder, not a stand-in");
        // The spider geometry is vanilla's own (SpiderModel.LAYER == ModelLayers.SPIDER),
        // so a mod that registered that id would collide with vanilla's registration.
        assertTrue(!wiring.contains("MIND_SPIDER_LAYER") && !wiring.contains("SPIDER_LAYER"),
                "no spider layer may be registered by this mod");
        final String renderer = readSource("client/entity/RenderMindSpider.java");
        assertTrue(!renderer.contains("@EventBusSubscriber"), "the renderer must not self-register");
        assertNotNull(CatalogMindSpider.MIND_SPIDER, "the holder the wiring names exists");
    }

    @Test
    void theMindSpiderIsTheFourthRendererAndNothingMoreIsRegistered() throws IOException {
        // Comments stripped: the wiring's javadoc names the un-merged wisp branch
        // and the dart/orb renderers as out of scope, and prose about a gap is not
        // a registration of it.
        final String wiring = stripComments(readSource("client/entity/ModEntityRenderers.java"));
        assertEquals(4, occurrences(wiring, "event.registerEntityRenderer("),
                "the golem trio plus the mind spider, and no other renderer");
        for (final String absent : new String[] {"CatalogEntities.", "CatalogPech.", "CatalogTaintMobs.",
                "CatalogFurniture."}) {
            assertTrue(!wiring.contains(absent),
                    "the un-drawn mobs are this card's declared gaps, not its registrations: " + absent);
        }
    }

    // ---------------------------------------------------- harness helpers

    /** The renderer, built through the production seam from 21.1's own provider map. */
    private static RenderMindSpider mindSpiderRenderer() throws Exception {
        final EntityRenderersEvent.RegisterRenderers event = new EntityRenderersEvent.RegisterRenderers();
        ModEntityRenderers.registerMindSpider(event, vanillaKey());
        return RenderMindSpider.class.cast(vanillaProviders().get(vanillaKey()).create(bareContext()));
    }

    private static EntityType<EntityMindSpider> vanillaKey() {
        @SuppressWarnings("unchecked")
        final EntityType<EntityMindSpider> key = (EntityType<EntityMindSpider>) (EntityType<?>) EntityType.CAVE_SPIDER;
        return key;
    }

    private static EntityRendererProvider.Context bareContext() {
        return new EntityRendererProvider.Context(null, null, null, null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    private static Map<EntityType<?>, EntityRendererProvider<?>> vanillaProviders() throws Exception {
        final Field field = EntityRenderers.class.getDeclaredField("PROVIDERS");
        field.setAccessible(true);
        return (Map<EntityType<?>, EntityRendererProvider<?>>) field.get(null);
    }

    /** 1.21.1 keeps the layer list private; the layers a renderer added are read reflectively. */
    @SuppressWarnings("unchecked")
    private static List<Object> layersOf(final EntityRenderer<?> renderer) throws Exception {
        for (Class<?> owner = renderer.getClass(); owner != null; owner = owner.getSuperclass()) {
            try {
                final Field field = owner.getDeclaredField("layers");
                field.setAccessible(true);
                return (List<Object>) field.get(renderer);
            } catch (NoSuchFieldException ignored) {
                // walk up: LivingEntityRenderer owns the list
            }
        }
        throw new IllegalStateException("LivingEntityRenderer.layers moved: " + renderer.getClass());
    }

    private static float shadowRadiusOf(final EntityRenderer<?> renderer) throws Exception {
        final Field field = EntityRenderer.class.getDeclaredField("shadowRadius");
        field.setAccessible(true);
        return field.getFloat(renderer);
    }

    private static Class<?> typeArgument(final Class<?> renderer) {
        final ParameterizedType type = (ParameterizedType) renderer.getGenericSuperclass();
        return (Class<?>) type.getActualTypeArguments()[0];
    }

    private static Class<?> modelArgument(final Class<?> renderer) {
        final ParameterizedType type = (ParameterizedType) renderer.getGenericSuperclass();
        final java.lang.reflect.Type model = type.getActualTypeArguments()[1];
        // The port's own renderers parameterize the model (SpiderModel<EntityMindSpider>);
        // the raw class is the model's own name, which is what the pairing pins.
        if (model instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) model).getRawType();
        }
        return (Class<?>) model;
    }

    private static int occurrences(final String haystack, final String needle) {
        int count = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }

    private static String readSource(final String relative) throws IOException {
        final Path path = MAIN.resolve(relative);
        assertTrue(Files.isRegularFile(path), "expected the source at " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * Production classes that write the viewer name. The reference's writer is
     * the mind-control lane ({@code EntityMindSpider.java:42-44} is reached from
     * the spell's server side); that lane is the sibling card {@code t_1a83f0db},
     * so in this repo the seam has no caller and the renderer's empty-viewer arm
     * is the one that runs. A future card that wires it must land with its own
     * visibility test, and this assertion is where it fails first.
     */
    private static List<String> writerOfViewerName() throws IOException {
        try (var paths = Files.walk(MAIN)) {
            final List<String> writers = new java.util.ArrayList<>();
            for (final Path path : paths.filter(p -> p.getFileName().toString().endsWith(".java")).toList()) {
                final String name = path.getFileName().toString();
                if (name.equals("EntityMindSpider.java") || path.toString().contains("/gametest/")) {
                    continue;                                           // the seam, and its own probe
                }
                if (stripComments(Files.readString(path, StandardCharsets.UTF_8)).contains(".setViewer(")) {
                    writers.add(MAIN.relativize(path).toString());
                }
            }
            return writers;
        }
    }

    private static String stripComments(final String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
    }
}
