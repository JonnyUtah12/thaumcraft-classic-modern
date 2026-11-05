package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.research.ResearchLexicon;
import com.mojang.logging.LogUtils;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merges the generated research display strings into the client language
 * (the consumer half of D-016 option 2; BOOK-3a <code>t_5a784296</code>
 * shipped the generator, this card ships the reader).
 *
 * <p><b>Why a merge is needed at all.</b> BOOK-3a writes
 * {@code assets/thaumcraft/lang/research_original.json}. Neither language
 * loader on this tree reads a file with that name: 1.21.1's
 * {@code ClientLanguage.loadFrom} formats {@code "lang/%s.json"} for each
 * requested locale code, and NeoForge's
 * {@code net.neoforged.fml.i18n.I18nManager.loadTranslations} does
 * {@code getResourceAsStream("lang/" + locale + ".json")}. So without this
 * class, {@code Component.translatable(entry.nameKey())} in the Thaumonomicon
 * renders a raw {@code tc.research_*} key even in an overlay build.</p>
 *
 * <p><b>The merge.</b> {@code net.neoforged.neoforge} exposes
 * {@link Language#inject(Language)} (the same call vanilla's
 * {@code LanguageManager.onResourceManagerReload} makes after
 * {@code ClientLanguage.loadFrom}), and {@code TranslatableContents.decompose}
 * re-reads {@link Language#getInstance()} whenever that instance's identity
 * changes. So the merge is: read the generated file through the
 * {@code ResourceManager} (the same route a locale file takes, so pack
 * overlays apply the same way), build a {@link Language} whose map is
 * {@code live ∪ generated}, and inject it. Every translatable component in the
 * game — book entry names, hover text, page bodies, chat lines — then resolves
 * against the merged map, with no change to any UI class.</p>
 *
 * <p><b>Order-independence.</b> Mod reload listeners registered through
 * {@link RegisterClientReloadListenersEvent} and vanilla's
 * {@code LanguageManager} share one reload stack, and this listener has no
 * contract about running after it. The {@link TickGuard} closes that hole:
 * whenever the live language is not the instance this class injected — after a
 * language change in the options screen, an F3+T reload, a pack toggle — the
 * merge is re-applied on the next client tick. It is a reference comparison
 * against a volatile field, so the steady-state cost is nil, and it is
 * idempotent by construction.</p>
 *
 * <p><b>What stays untouched.</b> Client-only: {@code value = Dist.CLIENT} on
 * both subscribers, so a dedicated server ({@code runServer},
 * {@code runGameTestServer}) never loads this class, and the server's own
 * {@code Language} (fed by NeoForge's {@code LanguageHook}) is left alone. The
 * strings are never committed: in a code-only build the resource is absent,
 * {@link #lexicon()} is empty, and {@link #applyMerge()} injects
 * <em>nothing</em> — the client language stays exactly vanilla's, and the
 * research UI renders raw keys, which is the intended D-016 outcome.</p>
 *
 * <p><b>Known seam, recorded as BD-30.</b> {@code I18n} caches its language
 * ({@code I18n.language}, refreshed only by the package-private
 * {@code I18n.setLanguage} that {@code LanguageManager} calls), so a direct
 * {@code I18n.get} on a research key does not see this merge. Research display
 * text must therefore come from {@link #display(String)} /
 * {@link #displayComponent(String)} (which consult the lexicon first) or from
 * a translatable component; the shipped tree has no {@code I18n.get} call
 * today, and the book lane's screens use {@code Component.translatable}.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ResearchOriginalLanguage {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** {@code thaumcraft:lang/research_original.json} — the path BOOK-3a writes. */
    public static final ResourceLocation GENERATED =
            ResourceLocation.fromNamespaceAndPath(ResearchLexicon.NAMESPACE,
                    ResearchLexicon.GENERATED_RESOURCE_PATH);

    private static volatile ResearchLexicon lexicon = ResearchLexicon.empty();

    /** The instance this class injected; the identity the guard compares against. */
    @Nullable
    private static volatile Language injected;

    /** The map currently merged: the player's own strings, or empty (code-only build). */
    public static ResearchLexicon lexicon() {
        return lexicon;
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(final RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) ResearchOriginalLanguage::readLexicon);
    }

    /**
     * Reads the generated resource into {@link #lexicon} and applies the
     * merge. The {@code ResourceManager} stack is read first, in 1.21.1's pack
     * order, so a user pack that ships the same path overlays the mod's file
     * exactly as it overlays a locale file; when that route yields nothing the
     * classloader route is read (the route NeoForge's {@code I18nManager}
     * uses, and the only lang route a dedicated server has — measured on this
     * tree, the game-test server's {@code ResourceManager} returns an empty
     * stack for {@code thaumcraft:lang/en_us.json}).
     *
     * <p>A missing file on both routes (the code-only artifact) leaves the
     * lexicon empty and the merge a no-op; a broken one is logged and skipped,
     * never fatal.</p>
     */
    public static void readLexicon(final ResourceManager resources) {
        Map<String, String> read = new LinkedHashMap<>();
        for (final Resource resource : resources.getResourceStack(GENERATED)) {
            try (InputStream in = resource.open()) {
                read = ResearchLexicon.merged(read, ResearchLexicon.fromStream(in).values());
            } catch (IOException | RuntimeException broken) {
                LOGGER.warn("Skipped research lang file {} from pack {} ({})",
                        GENERATED, resource.sourcePackId(), broken);
            }
        }
        String route = "resource-stack";
        if (read.isEmpty()) {
            final ResearchLexicon shipped =
                    ResearchLexicon.fromClasspath(ResearchLexicon.GENERATED_ASSET_PATH);
            if (!shipped.isEmpty()) {
                read = shipped.values();
                route = "classpath";
            }
        }
        installLexicon(ResearchLexicon.of(read));
        applyMerge();
        LOGGER.info("research lang merge: {} keys from {} via {}", lexicon.size(), GENERATED, route);
    }

    /**
     * Publishes the lexicon the merge reads. This is the reload listener's own
     * write, exposed as a seam so a probe can drive the merge without a
     * client: a {@code Screen} is never constructed on the game-test server,
     * and a {@code ResourceManager} cannot be built in a plain JVM unit test.
     */
    public static void installLexicon(final ResearchLexicon published) {
        lexicon = published == null ? ResearchLexicon.empty() : published;
    }

    /**
     * Injects the merged language, if there is anything to merge and the live
     * language is not already the merged one. Idempotent, and safe to call
     * from the reload stack and from the client tick alike.
     *
     * @return the injected instance, or {@code null} when nothing was applied
     *         (an empty lexicon, i.e. a code-only build, or an already-merged
     *         live language)
     */
    @Nullable
    public static Language applyMerge() {
        final Language base = Language.getInstance();
        if (base == injected) {
            return null;
        }
        final ResearchLexicon current = lexicon;
        if (current.isEmpty()) {
            // Code-only build: no original string is shipped (D-016), so the
            // live language stays vanilla's and research keys render as keys.
            injected = null;
            return null;
        }
        final Language merged = mergedLanguage(base, current.values());
        injected = merged;
        Language.inject(merged);
        return merged;
    }

    /**
     * The merged language over {@code base}: every key the live language
     * answers is preserved, the generated strings overlay it, and the
     * behavioural half of {@code base} — the RTL flag and the bidi
     * {@code getVisualOrder} a {@code ClientLanguage} supplies, plus
     * component-form translations — is delegated, not replaced. That is why
     * merging cannot regress a right-to-left locale: the wrapper adds map
     * entries and inherits everything else.
     */
    public static Language mergedLanguage(final Language base, final Map<String, String> generated) {
        final Map<String, String> map = ResearchLexicon.merged(base.getLanguageData(), generated);
        return new Language() {
            @Override
            public String getOrDefault(String key, String defaultValue) {
                return map.getOrDefault(key, defaultValue);
            }

            @Override
            public boolean has(String id) {
                return map.containsKey(id);
            }

            @Override
            public boolean isDefaultRightToLeft() {
                return base.isDefaultRightToLeft();
            }

            @Override
            public FormattedCharSequence getVisualOrder(FormattedText text) {
                return base.getVisualOrder(text);
            }

            @Override
            public Map<String, String> getLanguageData() {
                return map;
            }

            @Override
            public @Nullable Component getComponent(String key) {
                return base.getComponent(key);
            }
        };
    }

    /**
     * The display string for a research key: the generated original, else the
     * live language's own answer (the port-authored {@code en_us.json}, which
     * is how BOOK-3b's 4 keys land), else the key unchanged. Rules in
     * {@link ResearchLexicon#display(String, String)}.
     */
    public static String display(final String key) {
        return lexicon.display(key, Language.getInstance().getOrDefault(key));
    }

    /** The research key as a literal component, resolved through {@link #display(String)}. */
    public static Component displayComponent(final String key) {
        return Component.literal(display(key));
    }

    private ResearchOriginalLanguage() {
    }

    /**
     * Order-independence guard (game bus, client only): re-applies the merge
     * whenever the live language is not the instance this class injected, so a
     * vanilla language reload that lands after this class's listener — a
     * language switch, F3+T, a pack toggle — cannot leave the research strings
     * unmerged. Steady state is one volatile reference read per client tick.
     */
    @EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT)
    public static final class TickGuard {

        @SubscribeEvent
        public static void onClientTick(final ClientTickEvent.Pre event) {
            if (Language.getInstance() != injected) {
                applyMerge();
            }
        }

        private TickGuard() {
        }
    }
}
