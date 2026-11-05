package com.jonnyutah12.thaumcraftclassic.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the shipped research graph on disk. The card's data-safety rule exists
 * because these files have been silently deleted from a worktree before; a
 * missing or truncated graph must fail the suite, not quietly shrink it.
 */
class ResearchGraphResourceGuardTest {

    private static final String GRAPH_RESOURCE_DIR = "src/main/resources/data/thaumcraft/research_graph";

    private static Path graphFile(String name) {
        final Path found = ReferenceOrigins.projectFile(GRAPH_RESOURCE_DIR + "/" + name);
        assertTrue(found != null, "missing " + GRAPH_RESOURCE_DIR + "/" + name
                + " (the research graph must stay present in the worktree)");
        return found;
    }

    @Test
    void researchGraphDataFilesArePresentOnDisk() {
        graphFile("entries.json");
        graphFile("categories.json");
    }

    @Test
    void entriesJsonCarries201EntriesAndTheLoaderAgrees() throws IOException {
        final JsonObject root = JsonParser.parseString(Files.readString(graphFile("entries.json"))).getAsJsonObject();
        assertEquals(1, root.get("formatVersion").getAsInt());
        assertEquals(201, root.get("entryCount").getAsInt(), "the original graph has 201 entries");
        assertEquals(201, root.getAsJsonArray("entries").size());
        assertEquals(201, ResearchGraphLoader.loadDefault().entryCount());
    }

    @Test
    void categoriesJsonCarriesTheSixOriginalCategories() throws IOException {
        final JsonObject root =
                JsonParser.parseString(Files.readString(graphFile("categories.json"))).getAsJsonObject();
        assertEquals(6, root.getAsJsonArray("categories").size());
        assertEquals(6, ResearchGraphLoader.loadDefault().categoryCount());
    }
}
