package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityWisp;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M7-03-s acceptance for the FIRST entity renderer the port authors.
 *
 * <p>What is proved, and at which level — the honest split, because a renderer
 * only ever runs inside a real client and this harness has no window and no
 * bootstrapped registries:</p>
 * <ol>
 *   <li><b>Contract level (this class, plain JVM):</b> {@link EntityWispRenderer}
 *       is an {@code EntityRenderer<EntityWisp>} with 1.21.1's provider
 *       constructor shape {@code (EntityRendererProvider.Context)}, and it
 *       addresses the two original textures by literal path.</li>
 *   <li><b>Registry level (this class, plain JVM):</b> the wiring is invoked
 *       with a real {@link EntityRenderersEvent.RegisterRenderers} — the event
 *       the client fires — and 21.1's own renderer map ends up holding the type
 *       that was passed, mapped to a provider. That map
 *       ({@code EntityRenderers.PROVIDERS}) is the one
 *       {@code createEntityRenderers} turns into the live
 *       {@code EntityType -> EntityRenderer} table, so this is the registration
 *       the client uses, not a look-alike. A throwaway {@code EntityType} is
 *       passed through the seam because the unit harness has the registries
 *       frozen and cannot resolve {@code CatalogEntities.WISP}.</li>
 *   <li><b>Wiring level (this class, source-level):</b> the production handler
 *       is self-registering on the MOD bus, client-only, and passes the live
 *       {@code thaumcraft:wisp} holder into that same seam — so a dedicated
 *       server ({@code runServer}, {@code runGameTestServer}) never loads the
 *       renderer and the one registered type is the wisp.</li>
 * </ol>
 *
 * <p>What is NOT proved here: the rendered pixels. A client-side render behind
 * {@code world.field_72995_K} needs a human look; that is stated as
 * {@code not_proven} in {@code docs/FEATURE_MATRIX.md}.</p>
 */
class WispRendererRegistrationTest {

    private static final ResourceLocation WISP_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/wisp.png");

    // ---------------------------------------------------- contract level

    @Test
    void rendererExtendsEntityRendererForTheWisp() {
        assertEquals(EntityRenderer.class, EntityWispRenderer.class.getSuperclass(),
                "1.21.1 renderers extend EntityRenderer");
        final Type superType = EntityWispRenderer.class.getGenericSuperclass();
        assertTrue(superType instanceof ParameterizedType, "the base must be parameterized");
        assertEquals(EntityWisp.class, ((ParameterizedType) superType).getActualTypeArguments()[0],
                "the renderer must be parameterized on EntityWisp, not on another entity");
    }

    @Test
    void rendererHasThe1211ProviderConstructorShape() throws Exception {
        assertNotNull(EntityWispRenderer.class.getConstructor(EntityRendererProvider.Context.class),
                "RegisterRenderersEvent's EntityRendererProvider is created from a Context");
    }

    @Test
    void rendererAddressesTheTwoOriginalTextures() {
        assertEquals(WISP_TEXTURE, EntityWispRenderer.WISP_TEXTURE,
                "UtilsFX.bindTexture(\"textures/misc/wisp.png\") — the 4x4 body sheet");
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png"),
                EntityWispRenderer.SHELL_TEXTURE,
                "ParticleEngine.particleTexture — the glow-shell atlas");
        assertEquals(ThaumcraftClassic.MOD_ID, EntityWispRenderer.WISP_TEXTURE.getNamespace());
        assertTrue(EntityWispRenderer.SHELL_TEXTURE.getPath().startsWith("textures/misc/"),
                "misc stays plural, as in the 1.21.1 client jar");
    }

    // ---------------------------------------------------- registry level

    /**
     * Fires the real client event through the seam and follows the call all the
     * way to the provider 21.1 stores, then builds a renderer from it.
     *
     * <p>The key is deliberately incidental: the harness's registries are frozen
     * (a fresh {@code EntityType} throws {@code IllegalStateException: Registry
     * is already frozen} out of {@code MappedRegistry.createIntrusiveHolder}), so
     * the live {@code CatalogEntities.WISP} holder cannot be resolved here and an
     * already-bootstrapped vanilla type stands in. What is asserted is the route
     * the production call takes — {@code RegisterRenderers} ->
     * {@code EntityRenderers.register} -> the provider map the client builds its
     * renderer table from — and that the stored provider constructs
     * {@link EntityWispRenderer}, which pins the {@code EntityWispRenderer::new}
     * method reference on the wire.</p>
     */
    @Test
    @SuppressWarnings("unchecked")
    void firingTheClientEventRoutesTheSeamIntoTheVanillaRendererMapAndBuildsOurRenderer() throws Exception {
        final Map<EntityType<?>, EntityRendererProvider<?>> providers = vanillaProviders();
        final EntityType<EntityWisp> standInType = (EntityType<EntityWisp>) (EntityType<?>) EntityType.ITEM;
        final EntityRendererProvider<?> beforeProvider = providers.get(standInType);

        final EntityRenderersEvent.RegisterRenderers event =
                new EntityRenderersEvent.RegisterRenderers();
        ModEntityRenderers.register(event, standInType);

        final EntityRendererProvider<?> afterProvider = providers.get(standInType);
        assertNotNull(afterProvider, "the registration must land in EntityRenderers' own provider map");
        assertNotSame(beforeProvider, afterProvider,
                "RegisterRenderers -> EntityRenderers.register must replace the entry for the type handed in");

        // A provider context with no parts: EntityRenderer only stores the
        // dispatcher, so the renderer is constructible without a client.
        final EntityRendererProvider.Context context = new EntityRendererProvider.Context(
                null, null, null, null, null, null, null);
        assertInstanceOf(EntityWispRenderer.class, afterProvider.create(context),
                "the provider stored by the client event must build the wisp renderer");
    }

    // ---------------------------------------------------- wiring level

    @Test
    void handlerIsSelfRegisteringOnTheModBusClientOnlyAndPassesTheLiveWispType() throws Exception {
        final String source = readSource("client/entity/ModEntityRenderers.java");
        assertTrue(source.contains("@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID,"),
                "the renderer wiring must be self-registering for this mod");
        assertTrue(source.contains("bus = EventBusSubscriber.Bus.MOD"),
                "EntityRenderersEvent is a mod-bus event in NeoForge 21.1");
        assertTrue(source.contains("value = Dist.CLIENT"),
                "the wiring is client-only, so a dedicated server never loads the renderer class");
        assertTrue(source.contains("@SubscribeEvent"), "21.1 needs @SubscribeEvent on the handler");
        assertTrue(source.contains("EntityRenderersEvent.RegisterRenderers event"),
                "the handler must take RegisterRenderers");
        assertTrue(source.contains("register(event, CatalogEntities.WISP.get())"),
                "the production path must pass the live thaumcraft:wisp holder into the seam");
        assertTrue(source.contains("event.registerEntityRenderer(type, EntityWispRenderer::new)"),
                "the seam must pair the entity type with EntityWispRenderer");
    }

    @Test
    void rendererClassDoesNotLeakOntoTheServerSideOfTheModBus() throws Exception {
        // The renderer itself carries no bus annotation: it is only ever reached
        // through the Dist.CLIENT subscriber above, which is what keeps
        // runServer/runGameTestServer from loading OpenGL-touching classes.
        final String source = readSource("client/entity/EntityWispRenderer.java");
        assertTrue(source.contains("extends EntityRenderer<EntityWisp>"),
                "the renderer extends the vanilla base for the wisp");
        assertTrue(!source.contains("@EventBusSubscriber"),
                "the renderer must not self-register; ModEntityRenderers owns the wiring");
    }

    @Test
    void renderTypesAreBuiltLazilySoTheServerNeverTouchesThem() throws Exception {
        final String source = readSource("client/entity/WispRenderTypes.java");
        assertTrue(source.contains("ADDITIVE_TRANSPARENCY"),
                "the original's GL_SRC_ALPHA/GL_ONE blend is the additive transparency shard");
        assertTrue(source.contains("COLOR_WRITE"), "glDepthMask(false) is a colour-only write mask");
        assertTrue(source.contains("static final class Body") && source.contains("static final class Shell"),
                "each pass lives in its own nested holder, so a RenderType is built only on "
                        + "first use by the client");
    }

    // ---------------------------------------------------- harness helpers

    /** {@code EntityRenderers.PROVIDERS} — the map the client's renderer table is built from. */
    @SuppressWarnings("unchecked")
    private static Map<EntityType<?>, EntityRendererProvider<?>> vanillaProviders() throws Exception {
        final Field field = EntityRenderers.class.getDeclaredField("PROVIDERS");
        field.setAccessible(true);
        return (Map<EntityType<?>, EntityRendererProvider<?>>) field.get(null);
    }

    /** A source file under this worktree's {@code src/main/java}. */
    private static String readSource(final String relativeToSourceRoot) throws Exception {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        final Path repoRoot = csv.getParent().getParent();
        final Path source = repoRoot.resolve(
                "src/main/java/com/jonnyutah12/thaumcraftclassic/" + relativeToSourceRoot);
        assertTrue(Files.isRegularFile(source), "expected the wiring source at " + source);
        return Files.readString(source, StandardCharsets.UTF_8);
    }
}
