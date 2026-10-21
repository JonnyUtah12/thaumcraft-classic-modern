package com.jonnyutah12.thaumcraftclassic.aspect;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-01 acceptance: every aspect is defined with the original values.
 *
 * <p>Two independent oracles are checked against {@link AspectDefinitions}:</p>
 * <ul>
 *   <li>{@link OriginalAspectTable} — the frozen full field set (tag, color,
 *       chatcolor, blend, components, declaration order) mechanically
 *       extracted from the decompiled original {@code Aspect.java}.</li>
 *   <li>The TC-M0-04 inventory (docs/original-content-inventory.md section 1,
 *       passed via {@code tc.contentInventoryFile}) — the 48 constant
 *       names/tags/colors as recorded at baseline.</li>
 * </ul>
 *
 * <p>Plus the registry-free behaviour of {@link Aspect} itself: getters,
 * primal rule ({@code components != 2}), {@code getName()} capitalisation,
 * the original image path, and the documented pre-registry failure of
 * {@code getComponents()}.</p>
 */
class AspectDefinitionsParityTest {

    @Test
    void definitionsMatchFrozenOriginalTableExactly() {
        final List<AspectDefinitions.Definition> defs = AspectDefinitions.all();
        assertEquals(OriginalAspectTable.ROWS.size(), defs.size(),
                "aspect count differs from the original 48");

        for (int i = 0; i < OriginalAspectTable.ROWS.size(); i++) {
            final OriginalAspectTable.Row row = OriginalAspectTable.ROWS.get(i);
            final AspectDefinitions.Definition def = defs.get(i);
            assertEquals(row.tag(), def.tag(), "declaration order / tag mismatch at index " + i);
            assertEquals(row.color(), def.color(), row.tag() + ": color differs");
            assertEquals(row.chatcolor(), def.chatcolor(), row.tag() + ": chatcolor differs");
            assertEquals(row.blend(), def.blend(), row.tag() + ": blend differs");
            assertEquals(row.components(), def.components(), row.tag() + ": components differ");
        }
    }

    @Test
    void definitionsMatchContentInventorySectionOne() throws IOException {
        final Path inventory = Path.of(requiredProperty("tc.contentInventoryFile"));
        final List<String[]> rows = parseInventoryAspectSection(inventory);
        assertEquals(48, rows.size(),
                "inventory section 1 must list exactly 48 aspects (TC-M0-04 baseline)");

        final List<AspectDefinitions.Definition> defs = AspectDefinitions.all();
        for (int i = 0; i < rows.size(); i++) {
            final String constant = rows.get(i)[0];
            final String tag = rows.get(i)[1];
            final int color = Integer.parseInt(rows.get(i)[2].substring(1), 16);
            final AspectDefinitions.Definition def = defs.get(i);
            assertEquals(tag, def.tag(), "inventory order vs definitions: index " + i);
            assertEquals(color, def.color(), constant + " (" + tag + "): color differs from inventory");
        }
    }

    @Test
    void primalSetIsTheOriginalSix() {
        final List<String> primal = new ArrayList<>();
        for (final AspectDefinitions.Definition def : AspectDefinitions.all()) {
            if (def.primal()) {
                primal.add(def.tag());
            }
        }
        assertEquals(List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio"), primal,
                "the primal set must be exactly the original six, in declaration order");
    }

    @Test
    void everyCompoundDeclaresItsComponentsBeforeItself() {
        // The original built compounds in static initialisers, so each pair
        // must already exist when the compound is constructed. The generated
        // definitions must keep that topological order.
        final Set<String> seen = new HashSet<>();
        for (final AspectDefinitions.Definition def : AspectDefinitions.all()) {
            for (final String component : def.components()) {
                assertTrue(seen.contains(component),
                        def.tag() + " references component " + component + " declared later");
            }
            seen.add(def.tag());
        }
        assertEquals(48, seen.size());
    }

    @Test
    void aspectObjectsCarryOriginalValuesWithoutARegistry() {
        for (final AspectDefinitions.Definition def : AspectDefinitions.all()) {
            final Aspect aspect = new Aspect(def);
            assertEquals(def.tag(), aspect.getTag());
            assertEquals(def.color(), aspect.getColor());
            assertEquals(def.chatcolor(), aspect.getChatcolor());
            assertEquals(def.blend(), aspect.getBlend());
            assertEquals(def.components(), aspect.getComponentTags());
            assertEquals(def.primal(), aspect.isPrimal(), def.tag() + ": primal flag");
            assertFalse(def.chatcolor() != null && !def.primal(),
                    def.tag() + ": compounds must not carry a chatcolor, as in the original");

            // Original WordUtils.capitalizeFully on a single lowercase word.
            assertEquals(def.tag().substring(0, 1).toUpperCase(java.util.Locale.ROOT) + def.tag().substring(1),
                    aspect.getName(), def.tag() + ": getName()");

            // Original image path: thaumcraft:textures/aspects/<tag>.png
            assertEquals(ResourceLocation.fromNamespaceAndPath(
                            "thaumcraft", "textures/aspects/" + def.tag() + ".png"),
                    aspect.getImage(), def.tag() + ": image path");
        }
    }

    @Test
    void componentsResolveThroughTheLiveRegistryWithIdentity() {
        // The unit-test JVM boots FML (NeoForge's junit platform), so the
        // thaumcraft:aspect registry is live here; getComponents must resolve
        // to the very registry objects (identity), and primals stay null as
        // in the original.
        final AspectDefinitions.Definition metallum = AspectDefinitions.all().stream()
                .filter(d -> d.tag().equals("metallum")).findFirst().orElseThrow();
        final Aspect aspect = new Aspect(metallum);
        assertNull(new Aspect(AspectDefinitions.all().get(0)).getComponents(),
                "primal components are null, as in the original");
        final List<Aspect> components = aspect.getComponents();
        assertEquals(2, components.size(), "metallum has two components");
        // Original order: METAL = new Aspect("metallum", ..., {EARTH, CRYSTAL}).
        assertEquals(Aspect.getAspect("terra"), components.get(0));
        assertEquals(Aspect.getAspect("vitreus"), components.get(1));
        assertTrue(components.get(0) == Aspect.getAspect("terra")
                        && components.get(1) == Aspect.getAspect("vitreus"),
                "components must be the registry objects themselves (identity)");
    }

    // ------------------------------------------------------------------ util

    private static List<String[]> parseInventoryAspectSection(Path inventory) throws IOException {
        final List<String> lines = Files.readAllLines(inventory);
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("## 1. Aspects")) {
                start = i;
                break;
            }
        }
        assertTrue(start >= 0, "inventory section 1 (Aspects) not found");
        final Pattern row = Pattern.compile(
                "^\\|\\s*([A-Z_]+)\\s*\\|\\s*([a-z]+)\\s*\\|\\s*\\([^)]*\\)\\s*\\|\\s*#([0-9A-Fa-f]{6})\\s*\\|\\s*$");
        final List<String[]> out = new ArrayList<>();
        for (int i = start + 1; i < lines.size(); i++) {
            if (lines.get(i).startsWith("## ")) {
                break;
            }
            final Matcher m = row.matcher(lines.get(i));
            if (m.matches()) {
                out.add(new String[]{m.group(1), m.group(2), "#" + m.group(3)});
            }
        }
        return out;
    }

    private static String requiredProperty(String name) {
        final String value = System.getProperty(name);
        assertNotNull(value, "system property '" + name + "' is not set — run tests through ./gradlew test");
        return value;
    }
}
