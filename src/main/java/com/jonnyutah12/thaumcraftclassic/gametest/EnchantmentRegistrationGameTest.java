package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.recipes.InfusionEnchantmentRecipe;
import com.jonnyutah12.thaumcraftclassic.registration.ModEnchantments;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogInfusion;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Enchantment registration (TC-M6-05-s-e1c): the two payload entries are live
 * in the running game, and the two shipped infusion rows that name them now
 * resolve a real holder.
 *
 * <p>One probe, three claims, all read from the live game rather than from the
 * files (the file half is
 * {@code src/test/java/.../enchantment/EnchantmentIdsTest.java}):</p>
 * <ol>
 *   <li>{@code thaumcraft:ench_repair} and {@code thaumcraft:ench_haste} are
 *       in the {@code minecraft:enchantment} registry of the running server.
 *       In 1.21.1 that registry is datapack-driven, so the entries come from
 *       {@code data/thaumcraft/enchantment/*.json} and the live lookup is
 *       {@code level.registryAccess()} — {@code BuiltInRegistries.ENCHANTMENT}
 *       stays empty for modded payloads (see
 *       {@code registration/ModEnchantments}: a DeferredRegister for it
 *       silently never populates), which is why the probe reads the level's
 *       registry provider, the same accessor
 *       {@code RegistrationGameTest.DATAPACK_KINDS} resolves
 *       kind=enchantment rows through.</li>
 *   <li>Each entry's display name is a translatable key that ships in
 *       {@code en_us.json}, so the enchantment renders as "Repair"/"Haste"
 *       rather than a raw key.</li>
 *   <li>Both {@code recipe.*.infusion_enchantment} rows naming these ids are
 *       loaded in the live {@code RecipeManager} and resolve a non-null
 *       enchantment holder — the defect this card closes (the rows used to
 *       load and never match).</li>
 * </ol>
 *
 * <p>Not asserted: the effect bodies. Neither payload carries an
 * {@code effects} component yet, so Repair does not mend and Haste does not
 * speed anything up; that is the follow-up slice recorded in
 * docs/feature-tracking.md.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnchantmentRegistrationGameTest {

    /** contract id -> lang key the payload's description component must use. */
    private static final List<Map.Entry<ResourceKey<Enchantment>, String>> CONTRACT = List.of(
            Map.entry(ModEnchantments.REPAIR, "enchantment.thaumcraft.ench_repair"),
            Map.entry(ModEnchantments.HASTE, "enchantment.thaumcraft.ench_haste"));

    private EnchantmentRegistrationGameTest() {
    }

    @GameTest(template = "empty")
    public static void enchantmentIdsAreRegisteredAndTheirRecipesResolve(final GameTestHelper helper) {
        final Registry<Enchantment> enchantments =
                helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        final JsonObject lang = loadLang(helper);

        int registered = 0;
        for (final Map.Entry<ResourceKey<Enchantment>, String> entry : CONTRACT) {
            final ResourceKey<Enchantment> key = entry.getKey();
            final ResourceLocation id = key.location();
            helper.assertTrue(enchantments.containsKey(id),
                    "minecraft:enchantment has no " + id + " — the datapack payload under "
                            + "data/thaumcraft/enchantment/" + id.getPath() + ".json did not register");
            final Holder<Enchantment> holder = enchantments.getHolderOrThrow(key);

            final Component description = holder.value().description();
            helper.assertTrue(description.getContents() instanceof TranslatableContents,
                    id + " description is not a translatable component: " + description);
            final String key$ = ((TranslatableContents) description.getContents()).getKey();
            helper.assertTrue(entry.getValue().equals(key$),
                    id + " description key is " + key$ + ", expected " + entry.getValue());
            helper.assertTrue(lang.has(entry.getValue())
                            && lang.get(entry.getValue()).getAsString().length() > 0,
                    entry.getValue() + " is not shipped in assets/thaumcraft/lang/en_us.json");
            registered++;
        }

        // The recipe half: every shipped row naming one of the two ids must be
        // loaded AND resolve a holder. Before this slice both rows loaded and
        // resolved null, so the row could never match a pedestal.
        final var registries = helper.getLevel().registryAccess();
        int resolvableRows = 0;
        for (final RecipeHolder<InfusionEnchantmentRecipe> row
                : helper.getLevel().getRecipeManager().getAllRecipesFor(CatalogInfusion.INFUSION_ENCHANTMENT_TYPE.get())) {
            final ResourceKey<Enchantment> named = row.value().enchantmentKey();
            for (final Map.Entry<ResourceKey<Enchantment>, String> entry : CONTRACT) {
                if (!entry.getKey().equals(named)) {
                    continue;
                }
                helper.assertTrue(row.value().enchantment(registries) != null,
                        "the loaded row " + row.id() + " names " + named.location()
                                + " but resolves no enchantment holder, so it can never match");
                resolvableRows++;
            }
        }
        helper.assertTrue(resolvableRows == CONTRACT.size(),
                "expected the 2 infusion_enchantment rows naming the registered ids to be loaded, found "
                        + resolvableRows + " (recipe.201.infusion_enchantment / recipe.202.infusion_enchantment)");

        ThaumcraftClassic.LOGGER.info(
                "[EnchantmentRegistrationGameTest] {}/{} thaumcraft enchantments registered in "
                        + "minecraft:enchantment, {} infusion rows resolve their holder, lang entries: {}",
                registered, CONTRACT.size(), resolvableRows, CONTRACT.size());
        helper.succeed();
    }

    /** Same two-step read as {@code LocalizationGameTest.loadLang}: classpath first, source tree fallback. */
    private static JsonObject loadLang(final GameTestHelper helper) {
        String text = null;
        try (InputStream in = EnchantmentRegistrationGameTest.class.getResourceAsStream(
                "/assets/" + ThaumcraftClassic.MOD_ID + "/lang/en_us.json")) {
            if (in != null) {
                text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (text == null) {
            final String csv = System.getProperty("tc.contentMappingFile", "");
            helper.assertTrue(!csv.isEmpty(), "tc.contentMappingFile must be set by the gameTestServer run config");
            final Path file = Path.of(csv).toAbsolutePath().getParent().getParent()
                    .resolve("src/main/resources/assets/" + ThaumcraftClassic.MOD_ID + "/lang/en_us.json");
            helper.assertTrue(Files.isRegularFile(file), "en_us.json not found on the classpath or at " + file);
            try {
                text = Files.readString(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        final var parsed = JsonParser.parseString(text);
        helper.assertTrue(parsed.isJsonObject(), "en_us.json must be a JSON object");
        return parsed.getAsJsonObject();
    }
}
