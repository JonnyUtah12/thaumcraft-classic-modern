package com.jonnyutah12.thaumcraftclassic.client.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.lang.reflect.ParameterizedType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityMindSpider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M7-03-r's renderer coverage gate: <b>every {@code EntityType} the mod
 * registers is either bound to a renderer in {@link ModEntityRenderers} or named
 * as an un-drawn gap</b>, and the mind spider is on the rendered side of that
 * line.
 *
 * <p>Why this shape. {@code runGameTestServer} is a dedicated server, so nothing
 * in the repo's CI ever constructs a renderer; 1.21.1's default for an
 * entity type with no renderer is to draw <b>nothing</b>, silently. That is
 * exactly the state this card exists to close: the mob shipped
 * (TC-M6-06-s-c3-b, {@code f1c3f38f}) and the renderer lane TC-M7-03-s
 * ({@code t_215d6fd4}) closed with zero mind-spider references, so BD-78's
 * renderer half named a closed card. A coverage test in the house's
 * {@code MenuTypeScreenCoverageTest} shape — the catalogs are parsed and every
 * declared thing must appear in the client wiring — is what stops the next mob
 * from landing the same way. The strict form (every registered type rendered)
 * is not true of this repo today: the golem trio and the mind spider have
 * renderers and 24 ids do not, all of them listed below with the ledger cell
 * that owns them, so the enforceable contract is <i>rendered or declared</i>,
 * and a new un-named id fails.</p>
 *
 * <p>Level of proof: source level on a plain JVM. The provider map is read from
 * the real event in {@link MindSpiderRendererRegistrationTest}, which is where
 * the registry-level evidence for the mind spider lives; here the question is
 * coverage of the roster, which is a fact about the wiring source.</p>
 */
class EntityRendererCoverageTest {

    /** id → the renderer class the wiring binds, asserted exactly. */
    private static final Map<String, String> EXPECTED_RENDERERS = new LinkedHashMap<>();
    /** id → why it has no renderer and who owns that, asserted exactly. */
    private static final Map<String, String> EXPECTED_GAPS = new LinkedHashMap<>();

    static {
        // The four renderers the wiring registers. The golem trio is
        // TC-M6-08-s-c2 (ClientProxy.java:415, :457, :411); the mind spider is
        // this card (ClientProxy.java:450).
        EXPECTED_RENDERERS.put("golem", "RenderGolemBase");
        EXPECTED_RENDERERS.put("eldritchgolem", "RenderEldritchGolem");
        EXPECTED_RENDERERS.put("golembobber", "RenderGolemBobber");
        EXPECTED_RENDERERS.put("mindspider", "RenderMindSpider");

        // Every other registered entity type, with the cell that owns it. The
        // renderer lane TC-M7-03-s (t_215d6fd4) is closed, and its own handoff
        // ships none of these, so the notes say so and quote the reference's
        // registration line (client/ClientProxy.java) where it has one.
        EXPECTED_GAPS.put("wisp", "the reference binds RenderWisp at ClientProxy.java:417; the wisp "
                + "renderer lives on the UNMERGED wt/m7-renderers branch (the merge note of "
                + "ModEntityRenderers), and TC-M7-03-s (t_215d6fd4) closed with no wisp renderer");
        EXPECTED_GAPS.put("firebat", "the reference registers no renderer for it (0 hits for Firebat "
                + "in client/ClientProxy.java); the firebat row of docs/FEATURE_MATRIX.md keeps its "
                + "renderer on the TC-M7-03 lane, which closed at t_215d6fd4");
        EXPECTED_GAPS.put("brainyzombie", "the reference binds RenderBrainyZombie at "
                + "ClientProxy.java:426; the zombie pair's row of docs/FEATURE_MATRIX.md keeps "
                + "renderers on TC-M7-03, closed at t_215d6fd4");
        EXPECTED_GAPS.put("giantbrainyzombie", "the reference registers no renderer for the giant "
                + "(0 hits in client/ClientProxy.java); docs/FEATURE_MATRIX.md keeps it on TC-M7-03");
        EXPECTED_GAPS.put("pech", "the reference binds RenderPech at ClientProxy.java:429; the "
                + "TC-M6-06-s-c3-a row of docs/FEATURE_MATRIX.md names the renderer on TC-M7-03-s, "
                + "which closed at t_215d6fd4 without it");
        EXPECTED_GAPS.put("pechblast", "the reference binds RenderPechBlast at ClientProxy.java:425; "
                + "the same c3-a row names TC-M7-03-s, closed at t_215d6fd4 without it");
        EXPECTED_GAPS.put("golemorb", "the reference binds RenderElectricOrb at ClientProxy.java:421; the "
                + "golem card's ModEntityRenderers javadoc names the orb outside its six classes, so the "
                + "orb's renderer is its own reference file and no card mints it");
        EXPECTED_GAPS.put("dart", "the reference binds RenderDart at ClientProxy.java:432; the golem "
                + "card's ModEntityRenderers javadoc names RenderDart as its own reference file");
        EXPECTED_GAPS.put("specialitemgrate", "the reference draws it with a plain item renderer "
                + "(ClientProxy.java:410, new RenderItem()), whose 1.21.1 twin is the item entity "
                + "renderer and not a per-type mob renderer; no card mints it");
        final String taint = "the taint rows of docs/FEATURE_MATRIX.md name the renderers on "
                + "TC-M7-03-s, the lane that closed at t_215d6fd4 without them";
        EXPECTED_GAPS.put("fallingtaint", taint);
        EXPECTED_GAPS.put("taintedchicken", taint);
        EXPECTED_GAPS.put("taintedcow", taint);
        EXPECTED_GAPS.put("taintedcreeper", taint);
        EXPECTED_GAPS.put("taintedpig", taint);
        EXPECTED_GAPS.put("taintedsheep", taint);
        EXPECTED_GAPS.put("taintedvillager", taint);
        EXPECTED_GAPS.put("taintspider", taint);
        EXPECTED_GAPS.put("taintacle", taint);
        EXPECTED_GAPS.put("taintacletiny", taint);
        EXPECTED_GAPS.put("taintaclegiant", taint);
        EXPECTED_GAPS.put("taintspore", taint);
        EXPECTED_GAPS.put("taintswarmer", taint);
        EXPECTED_GAPS.put("taintswarm", taint);
        EXPECTED_GAPS.put("thaumslime", taint);
    }

    /** The six catalog files that create a {@code Registries.ENTITY_TYPE} register. */
    private static final List<String> ENTITY_CATALOGS = List.of(
            "CatalogEntities.java", "CatalogFurniture.java", "CatalogGolems.java",
            "CatalogMindSpider.java", "CatalogPech.java", "CatalogTaintMobs.java");

    private static final Path ROOT = repoRoot();
    private static final Path CATALOG_DIR = ROOT
            .resolve("src/main/java/com/jonnyutah12/thaumcraftclassic/registration/catalog");
    private static final Path WIRING_SOURCE = ROOT
            .resolve("src/main/java/com/jonnyutah12/thaumcraftclassic/client/entity/ModEntityRenderers.java");

    // ------------------------------------------------- the roster, parsed

    @Test
    void theRosterIsEveryEntityTypeTheCatalogsRegister() throws IOException {
        final Map<String, String> roster = roster();
        // 28 registered entity types as of main at f1c3f38f, from the six
        // catalogs named above. A new mob must appear here, and the test that
        // reads this map then forces it to be rendered or declared.
        assertEquals(28, roster.size(), "the registered EntityType roster: " + roster.keySet());
        assertTrue(roster.containsKey("mindspider"), "the mind spider is a registered entity type: "
                + roster.get("mindspider"));
        // The ids that live behind helper-indirected registrations are in the
        // roster too, so the parse is not reading only the direct calls.
        assertEquals("CatalogTaintMobs.java", roster.get("taintspider"),
                "registerWithEyeHeight(\"taintspider\", …) must land in the roster");
        assertEquals("CatalogMindSpider.java", roster.get("mindspider"));
        assertEquals("CatalogFurniture.java", roster.get("specialitemgrate"));
    }

    @Test
    void everyRegisteredEntityTypeIsRenderedOrDeclaredUnrendered() throws IOException {
        final Map<String, String> roster = roster();
        final Map<String, String> rendered = rendered();
        final Set<String> declared = new TreeSet<>(rendered.keySet());
        declared.addAll(EXPECTED_GAPS.keySet());
        final Set<String> missing = new TreeSet<>(roster.keySet());
        missing.removeAll(declared);
        assertTrue(missing.isEmpty(), "these registered entity types are neither rendered nor named "
                + "as a gap, so 1.21.1 would draw nothing for them silently: " + missing);
    }

    @Test
    void theDeclaredGapsAreExactlyTheUnrenderedRoster() throws IOException {
        // The map must not rot in either direction: a gap that gains a renderer
        // has to be removed from the declaration, and a new un-rendered mob has
        // to be added to it.
        final Set<String> unrendered = new TreeSet<>(roster().keySet());
        unrendered.removeAll(rendered().keySet());
        assertEquals(unrendered, new TreeSet<>(EXPECTED_GAPS.keySet()),
                "the declared un-rendered set must equal the roster minus the rendered set");
    }

    @Test
    void everyGapNoteNamesTheLedgerCellThatOwnsIt() {
        for (final Map.Entry<String, String> gap : EXPECTED_GAPS.entrySet()) {
            final String note = gap.getValue();
            assertTrue(note.contains("TC-M7-03") || note.contains("1.21.1 twin")
                            || note.contains("own reference file"),
                    gap.getKey() + ": a gap must name the renderer lane or the platform twin it waits on");
            assertTrue(note.contains("ClientProxy.java") || note.contains("docs/")
                            || note.contains("ModEntityRenderers") || note.contains("0 hits"),
                    gap.getKey() + ": a gap must be traceable to evidence, not a shrug");
        }
        assertTrue(!EXPECTED_GAPS.isEmpty(), "the gap set is not empty today");
        assertTrue(!EXPECTED_GAPS.containsKey("mindspider"),
                "the mind spider is not a gap: this card draws it");
    }

    // ------------------------------------------------- the wiring, parsed

    @Test
    void theWiringBindsExactlyTheFourRenderersItClaims() throws IOException {
        assertEquals(EXPECTED_RENDERERS, rendered(),
                "the id -> renderer pairs the wiring source establishes");
        // Every declared name must resolve to a real class in the client package,
        // so the map cannot claim a renderer that was never written.
        for (final Map.Entry<String, String> entry : EXPECTED_RENDERERS.entrySet()) {
            final Class<?> renderer = rendererClass(entry.getValue());
            assertEquals("com.jonnyutah12.thaumcraftclassic.client.entity", renderer.getPackageName(),
                    entry.getKey() + " -> " + entry.getValue() + " must live in the client package");
            assertTrue(net.minecraft.client.renderer.entity.EntityRenderer.class
                            .isAssignableFrom(renderer),
                    entry.getValue() + " must be an EntityRenderer, the thing 1.21.1's registration binds");
        }
        assertEquals(RenderMindSpider.class, rendererClass(EXPECTED_RENDERERS.get("mindspider")));
    }

    @Test
    void theMindSpiderIsRenderedThroughTheSpiderMobRendererItPorts() {
        // The card's contract in one assertion: the mind spider's renderer
        // exists, sits on the twin of the base the reference extends, is
        // parameterized on the port's own entity and spider model, and has the
        // provider constructor the registration event demands.
        assertEquals(net.minecraft.client.renderer.entity.MobRenderer.class,
                RenderMindSpider.class.getSuperclass(),
                "1.7.10 RenderMindSpider.java:18 extends RenderLiving; 1.21.1's twin is MobRenderer");
        assertEquals(EntityMindSpider.class, typeArgument(RenderMindSpider.class),
                "the renderer is parameterized on the port's mind spider");
        assertEquals(net.minecraft.client.model.SpiderModel.class, modelArgument(RenderMindSpider.class),
                "and on the platform's spider model, the twin of the reference's ModelSpider (:22)");
        try {
            final java.lang.reflect.Constructor<?> ctor = RenderMindSpider.class.getConstructor(
                    net.minecraft.client.renderer.entity.EntityRendererProvider.Context.class);
            assertTrue(java.lang.reflect.Modifier.isPublic(ctor.getModifiers()),
                    "the provider constructor the registration hands to 1.21.1 must be public");
        } catch (NoSuchMethodException e) {
            throw new AssertionError("RenderMindSpider must have the (Context) provider ctor", e);
        }
    }

    // ------------------------------------------------- the client boundary

    @Test
    void noServerSideSourceNamesTheMindSpiderRenderer() throws IOException {
        // ThaumonomiconScreenRegistrationTest.noServerSideSourceNamesTheClientScreen
        // is the house rule this mirrors: a client class named in server-painted
        // source is a class-loading hazard on a dedicated server. The mind
        // spider's own catalog holder may NAME the renderer in prose (the
        // javadoc is stripped here, exactly as that test strips it) and must not
        // reference it in code.
        final Path root = ROOT.resolve("src/main/java/com/jonnyutah12/thaumcraftclassic");
        final List<String> offenders;
        try (var paths = Files.walk(root)) {
            offenders = paths
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/client/"))
                    .filter(p -> {
                        try {
                            return stripComments(Files.readString(p, StandardCharsets.UTF_8))
                                    .contains("RenderMindSpider");
                        } catch (IOException | UncheckedIOException e) {
                            return false;
                        }
                    })
                    .map(p -> root.relativize(p).toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
        assertEquals(List.of(), offenders,
                "RenderMindSpider is client-only: only client/ may name it in code");
    }

    @Test
    void theMindSpiderRendererLivesInTheClientPackageAndIsNotSelfRegistering() throws IOException {
        assertEquals("com.jonnyutah12.thaumcraftclassic.client.entity",
                RenderMindSpider.class.getPackageName());
        final String source = read(WIRING_SOURCE);
        assertTrue(source.contains("registerMindSpider(event, CatalogMindSpider.MIND_SPIDER.get())"),
                "the production path passes the live thaumcraft:mindspider holder to the seam");
        assertTrue(source.contains("event.registerEntityRenderer(mindspider, RenderMindSpider::new)"),
                "the seam pairs the type with the renderer class");
        assertTrue(source.contains("value = Dist.CLIENT") && source.contains("@SubscribeEvent"),
                "the wiring is client-only and self-registering on the mod bus");
        assertTrue(!source.contains("registerLayerDefinition(MIND_SPIDER"),
                "the spider geometry is self-baked: SpiderModel.LAYER is vanilla's own "
                        + "ModelLayers.SPIDER, so the mod must not register that id");
        assertTrue(RenderMindSpider.class.getSimpleName().equals("RenderMindSpider")
                        && java.nio.file.Files.isRegularFile(ROOT.resolve(
                                "src/main/java/com/jonnyutah12/thaumcraftclassic/client/entity/"
                                        + "RenderMindSpider.java")),
                "the class the wiring names is the class this card ports");
    }

    // ------------------------------------------------- the parse

    /** registered entity-type id → the catalog file that registers it. */
    private static Map<String, String> roster() throws IOException {
        final Map<String, String> roster = new LinkedHashMap<>();
        for (final String file : ENTITY_CATALOGS) {
            final String source = flatten(read(CATALOG_DIR.resolve(file)));
            // Which DeferredRegister in this file is the entity registry? A file
            // can hold both (CatalogFurniture does), so the register's type and
            // its Registries constant decide, not its field name.
            final Set<String> registers = new LinkedHashSet<>();
            final Matcher field = Pattern
                    .compile("DeferredRegister<EntityType<\\?>>\\s+(\\w+)\\s*=\\s*DeferredRegister"
                            + "\\.create\\(\\s*Registries\\.ENTITY_TYPE")
                    .matcher(source);
            while (field.find()) {
                registers.add(field.group(1));
            }
            assertTrue(!registers.isEmpty(), file + " must create an EntityType register");
            for (final String register : registers) {
                final Matcher direct = Pattern
                        .compile("\\b" + register + "\\.register\\(\\s*\"([a-z0-9_]+)\"").matcher(source);
                while (direct.find()) {
                    roster.putIfAbsent(direct.group(1), file);
                }
            }
            // Indirect: a private helper that registers its own String parameter.
            // Its ids are the first literal argument of each call site.
            final Matcher helper = Pattern.compile("(\\w+)\\(\\s*final String name,").matcher(source);
            final Set<String> helpers = new LinkedHashSet<>();
            while (helper.find()) {
                helpers.add(helper.group(1));
            }
            for (final String name : helpers) {
                final Matcher call = Pattern.compile("\\b" + name + "\\(\\s*\"([a-z0-9_]+)\"")
                        .matcher(source);
                while (call.find()) {
                    roster.putIfAbsent(call.group(1), file);
                }
            }
        }
        return roster;
    }

    /** entity-type id → renderer simple name, read off the wiring's seam pairs. */
    private static Map<String, String> rendered() throws IOException {
        final String wiring = read(WIRING_SOURCE);
        final String flat = flatten(wiring);
        // 1. Each catalog's holder field -> registered id.
        final Map<String, String> holderIds = new LinkedHashMap<>();
        for (final String file : ENTITY_CATALOGS) {
            final String catalog = flatten(read(CATALOG_DIR.resolve(file)));
            final String simple = file.replace(".java", "");
            final Matcher holder = Pattern
                    .compile("(\\w+)\\s*=\\s*(?:\\w+\\.)?register\\(\\s*\"([a-z0-9_]+)\"").matcher(catalog);
            while (holder.find()) {
                holderIds.put(simple + "." + holder.group(1), holder.group(2));
            }
        }
        // 2. Each seam: its parameter names, in order.
        final Map<String, List<String>> seamParams = new LinkedHashMap<>();
        final Matcher seam = Pattern
                .compile("static void (\\w+)\\(final EntityRenderersEvent\\.RegisterRenderers event,"
                        + "([^)]*)\\)\\s*\\{").matcher(flat);
        while (seam.find()) {
            final List<String> params = new java.util.ArrayList<>();
            final Matcher param = Pattern.compile("EntityType<[^>]*>\\s+(\\w+)").matcher(seam.group(2));
            while (param.find()) {
                params.add(param.group(1));
            }
            seamParams.put(seam.group(1), params);
        }
        // 3. The body of each seam: parameter -> renderer class.
        final Map<String, String> paramRenderer = new LinkedHashMap<>();
        final Matcher pair = Pattern
                .compile("event\\.registerEntityRenderer\\((\\w+),\\s*(\\w+)::new\\)").matcher(flat);
        while (pair.find()) {
            paramRenderer.put(pair.group(1), pair.group(2));
        }
        // 4. The event handler: seam + positional arguments -> the live holders.
        final Map<String, String> rendered = new LinkedHashMap<>();
        final Matcher call = Pattern.compile("\\b(\\w+)\\(event,([^;]*)\\);").matcher(flat);
        while (call.find()) {
            final String seamName = call.group(1);
            final List<String> params = seamParams.get(seamName);
            if (params == null) {
                continue;
            }
            final List<String> args = new java.util.ArrayList<>();
            final Matcher arg = Pattern.compile("([A-Z]\\w*\\.\\w+)\\.get\\(\\)").matcher(call.group(2));
            while (arg.find()) {
                args.add(arg.group(1));
            }
            assertEquals(params.size(), args.size(),
                    seamName + " takes " + params + " and the handler passes " + args
                            + " - the seam and its call site must be read together");
            for (int i = 0; i < params.size(); i++) {
                final String id = holderIds.get(args.get(i));
                final String renderer = paramRenderer.get(params.get(i));
                assertTrue(id != null, "the wiring passes " + args.get(i) + ", which no catalog registers");
                assertTrue(renderer != null, "the seam " + seamName + " never binds " + params.get(i)
                        + " to a renderer");
                rendered.put(id, renderer);
            }
        }
        return rendered;
    }

    /** Newlines collapsed so a declaration that spans lines is one match. */
    private static String flatten(final String source) {
        return source.replaceAll("\\s+", " ");
    }

    private static String stripComments(final String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
    }

    private static String read(final Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "expected the source at " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Path repoRoot() {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        if (!Files.isRegularFile(csv)) {
            throw new AssertionError("content-mapping.csv not wired - run tests through ./gradlew test");
        }
        return csv.getParent().getParent();
    }

    /** A renderer the wiring declares, resolved to its class; a typo fails here. */
    private static Class<?> rendererClass(final String simpleName) {
        try {
            return Class.forName("com.jonnyutah12.thaumcraftclassic.client.entity." + simpleName);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("the wiring names a renderer class that does not exist: " + simpleName, e);
        }
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
}
