package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * TC-M9-02 defect t_26348bbc: {@code ThaumcraftCommand.register} used to do
 * {@code Commands.literal(root).then(subcommands())} where {@code subcommands()}
 * returned {@code Commands.literal("help").then(literal("research") ...)} — the
 * whole research / aspect / warp tree hung off the {@code help} node, so the
 * canonical {@code /thaumcraft research list} the shipped help text advertises
 * answered "Incorrect argument for command". These probes run against the LIVE
 * server dispatcher (the tree the {@code RegisterCommandsEvent} registered, next
 * to every other mod's commands) and pin the fixed shape:
 *
 * <ul>
 *   <li>{@code thaumcraft research list} parses AND executes, printing exactly
 *       the shipped research graph (the 201-entry {@code ResearchManager.graph()});</li>
 *   <li>{@code thaumcraft help} and bare {@code thaumcraft} still work;</li>
 *   <li>the {@code aspect} and {@code warp} families parse at the root (aliases
 *       {@code tc} / {@code thaum} included), and the old nested path
 *       {@code thaumcraft help research ...} is dead: {@code help} is a leaf.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CommandTreeGameTest {

    private CommandTreeGameTest() {
    }

    /** Chat lines the probes capture, in send order, stripped of nothing. */
    private static final class Capture implements CommandSource {
        final List<String> lines = new ArrayList<>();

        @Override
        public void sendSystemMessage(final Component component) {
            lines.add(component.getString());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return true;
        }
    }

    /** A level-4 source so the level-2 {@code requires} gate passes, like the battery's console. */
    private static CommandSourceStack source(final GameTestHelper helper, final Capture capture) {
        final MinecraftServer server = helper.getLevel().getServer();
        return new CommandSourceStack(capture, Vec3.ZERO, Vec2.ZERO, helper.getLevel(), 4,
                "CommandTreeGameTest", Component.literal("CommandTreeGameTest"), server, null);
    }

    /**
     * THE regression probe: the canonical root path parses and runs on the live
     * dispatcher, and prints the shipped graph key-for-key, in graph order.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void researchListAtRootPrintsTheShippedGraph(final GameTestHelper helper) {
        final Capture capture = new Capture();
        final int result;
        try {
            result = helper.getLevel().getServer().getCommands().getDispatcher()
                    .execute("thaumcraft research list", source(helper, capture));
        } catch (final CommandSyntaxException e) {
            helper.fail("/thaumcraft research list did not parse on the live dispatcher: "
                    + e.getMessage() + " (raw: " + e.getRawMessage().getString() + ")");
            return;
        }
        final List<String> keys = new ArrayList<>();
        for (final String line : capture.lines) {
            if (line.startsWith("§5")) {
                keys.add(line.substring(2));
            }
        }
        final List<String> shipped = ResearchManager.graph().entries().stream()
                .map(ResearchEntryData::key).toList();
        helper.assertTrue(!keys.isEmpty(),
                "/thaumcraft research list printed no research keys (captured: " + capture.lines.size()
                        + " lines, first: " + (capture.lines.isEmpty() ? "<none>" : capture.lines.get(0)) + ")");
        helper.assertTrue(keys.equals(shipped),
                "/thaumcraft research list printed " + keys.size() + " keys, not the shipped graph's "
                        + shipped.size() + " keys in graph order");
        helper.assertTrue(new java.util.HashSet<>(keys).size() == keys.size(),
                "printed research keys are not distinct: " + keys.size() + " lines, "
                        + new java.util.HashSet<>(keys).size() + " distinct");
        helper.assertTrue(result >= 0, "listResearch returned a negative result");
        helper.succeed();
    }

    /** The help path stays reachable at the root, and bare usage keeps its strings. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void helpAndBareUsageStillWorkAtRoot(final GameTestHelper helper) {
        final Capture capture = new Capture();
        final var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        try {
            dispatcher.execute("thaumcraft help", source(helper, capture));
            dispatcher.execute("thaumcraft", source(helper, capture));
        } catch (final CommandSyntaxException e) {
            helper.fail("help/bare usage path did not parse: " + e.getMessage());
            return;
        }
        helper.assertTrue(capture.lines.contains("§3You can also use /thaum or /tc instead of /thaumcraft."),
                "/thaumcraft help did not print its first line verbatim (captured " + capture.lines.size()
                        + " lines)");
        helper.assertTrue(capture.lines.contains("§cInvalid arguments")
                        && capture.lines.contains("§cUse /thaumcraft help to get help"),
                "bare /thaumcraft did not print the invalid-args pair");
        // The help text advertises the canonical paths; pin that it says so.
        helper.assertTrue(capture.lines.contains("  /thaumcraft research <list|player> <all|reset|<research>>")
                        && capture.lines.contains("  /thaumcraft aspect <player> <aspect|all> <amount>")
                        && capture.lines.contains("  /thaumcraft warp <player> <add|set> <amount> <PERM|TEMP>"),
                "help text no longer advertises the canonical research/aspect/warp paths");
        helper.succeed();
    }

    /**
     * Tree shape: aspect/warp parse at the ROOT on all three roots, and the old
     * nested {@code help <sub>} form is dead — {@code help} is a leaf sibling.
     * A path is "live" only if Brigadier consumes it completely (the server
     * answers "Incorrect argument for command" for a partially-consumed line);
     * the family node must sit on the parse path.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void subcommandFamiliesParseAtTheRootNotUnderHelp(final GameTestHelper helper) {
        final Capture capture = new Capture();
        final CommandSourceStack source = source(helper, capture);
        final var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();

        final String[] mustParse = {
                "thaumcraft research list",
                "thaumcraft research Steve all",
                "thaumcraft research Steve reset",
                "thaumcraft research Steve BASICTHAUMATURGY",
                "thaumcraft aspect Steve ignis 10",
                "thaumcraft aspect Steve all 10",
                "thaumcraft warp Steve add 5",
                "thaumcraft warp Steve add -100 PERM",
                "thaumcraft warp Steve set 10",
                "thaumcraft warp Steve set 0 TEMP",
                "tc research list",
                "thaum research list",
                "tc aspect Steve aer 1",
                "thaum warp Steve add 3",
        };
        for (final String command : mustParse) {
            final var parsed = dispatcher.parse(command, source);
            helper.assertTrue(!parsed.getReader().canRead(),
                    command + " is not live: trailing input left unparsed (tree: " + describe(parsed) + ")");
            helper.assertTrue(describe(parsed).matches("(?s).*(research|aspect|warp).*"),
                    command + " did not resolve its family node on the parse path: " + describe(parsed));
        }

        final String[] mustBeDead = {
                "thaumcraft help research list",
                "thaumcraft help aspect Steve ignis 10",
                "thaumcraft help warp Steve add 5",
                "tc help research list",
        };
        for (final String command : mustBeDead) {
            // Brigadier 1.3.10: parse never throws; an unreachable path leaves
            // trailing input (the server then answers "Incorrect argument for
            // command", exactly what the ATM10 battery recorded pre-fix).
            final boolean dead = dispatcher.parse(command, source).getReader().canRead();
            helper.assertTrue(dead, command + " is reachable — the fixed tree keeps help as a "
                    + "leaf sibling, so the old nested path must not parse");
        }
        helper.succeed();
    }

    private static String describe(final com.mojang.brigadier.ParseResults<CommandSourceStack> parsed) {
        final StringBuilder sb = new StringBuilder();
        for (final var node : parsed.getContext().getNodes()) {
            sb.append(node.getNode().getClass().getSimpleName())
                    .append('(').append(node.getNode().getName()).append(") ");
        }
        return sb.toString();
    }
}
