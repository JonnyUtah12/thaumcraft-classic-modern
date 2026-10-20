package com.jonnyutah12.thaumcraftclassic.command;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.player.PlayerWarpData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * TC-M3-02: port of the original {@code thaumcraft.common.lib.events.CommandThaumcraft}
 * ({@code /thaumcraft}, aliases {@code /thaum} and {@code /tc}).
 *
 * <p>Structure, permission level (2), argument ranges (aspect amount min 1,
 * warp-set min 0, warp-add -100..100) and every chat string are kept verbatim,
 * including the original's "has reset you research." typo. Two documented
 * divergences:</p>
 * <ul>
 *   <li>Aspect names: the original also accepted the localized display name
 *       ("Fire"); the port matches the aspect tag only until lang assets exist
 *       (TC-M8). The tag list below is the frozen 48-tag set from
 *       docs/original-content-inventory.md §1 — the aspect registry itself is
 *       TC-M4-01's; this command must not depend on it.</li>
 *   <li>The original's help text says an unspecified warp kind "will just add
 *       normal warp", but its code's else-branch is sticky; the port follows the
 *       code, as always.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class ThaumcraftCommand {

    /** The 48 original aspect tags, in inventory order (docs/original-content-inventory.md §1). */
    public static final List<String> ASPECT_TAGS = List.of(
            "aer", "terra", "ignis", "aqua", "ordo", "perditio", "vacuos", "lux",
            "tempestas", "motus", "gelum", "vitreus", "victus", "venenum", "potentia", "permutatio",
            "metallum", "mortuus", "volatus", "tenebrae", "spiritus", "sano", "iter", "alienis",
            "praecantatio", "auram", "vitium", "limus", "herba", "arbor", "bestia", "corpus",
            "exanimis", "cognitio", "sensus", "humanus", "messis", "perfodio", "instrumentum",
            "meto", "telum", "tutamen", "fames", "lucrum", "fabrico", "pannus", "machina",
            "vinculum");

    private ThaumcraftCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        for (final String root : List.of("thaumcraft", "thaum", "tc")) {
            dispatcher.register(Commands.literal(root)
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> invalidArgs(context.getSource()))
                    .then(subcommands()));
        }
    }

    private static ArgumentBuilder<CommandSourceStack, ?> subcommands() {
        return Commands.literal("help").executes(context -> help(context.getSource()))
                .then(Commands.literal("research")
                        .then(Commands.literal("list").executes(context -> listResearch(context.getSource())))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("all")
                                        .executes(context -> giveAllResearch(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"))))
                                .then(Commands.literal("reset")
                                        .executes(context -> resetResearch(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"))))
                                .then(Commands.argument("research", StringArgumentType.string())
                                        .executes(context -> giveResearch(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "research"))))))
                .then(Commands.literal("aspect")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("aspect", StringArgumentType.word())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> giveAspect(context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "aspect"),
                                                        IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("warp")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("add")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100))
                                                .executes(context -> addWarp(context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "amount"), ""))
                                                .then(Commands.argument("kind", StringArgumentType.word())
                                                        .executes(context -> addWarp(context.getSource(),
                                                                EntityArgument.getPlayer(context, "player"),
                                                                IntegerArgumentType.getInteger(context, "amount"),
                                                                StringArgumentType.getString(context, "kind"))))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> setWarp(context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "amount"), ""))
                                                .then(Commands.argument("kind", StringArgumentType.word())
                                                        .executes(context -> setWarp(context.getSource(),
                                                                EntityArgument.getPlayer(context, "player"),
                                                                IntegerArgumentType.getInteger(context, "amount"),
                                                                StringArgumentType.getString(context, "kind"))))))));
    }

    private static int invalidArgs(final CommandSourceStack source) {
        say(source, "§cInvalid arguments");
        say(source, "§cUse /thaumcraft help to get help");
        return 0;
    }

    private static int help(final CommandSourceStack source) {
        say(source, "§3You can also use /thaum or /tc instead of /thaumcraft.");
        say(source, "§3Use this to give research to a player.");
        say(source, "  /thaumcraft research <list|player> <all|reset|<research>>");
        say(source, "§3Use this to give aspect research points to a player.");
        say(source, "  /thaumcraft aspect <player> <aspect|all> <amount>");
        say(source, "§3Use this to give set a players warp level.");
        say(source, "  /thaumcraft warp <player> <add|set> <amount> <PERM|TEMP>");
        say(source, "  not specifying perm or temp will just add normal warp");
        return 1;
    }

    // ------------------------------------------------------------------ research

    private static int listResearch(final CommandSourceStack source) {
        for (final ResearchEntryData entry : ResearchManager.graph().entries()) {
            say(source, "§5" + entry.key());
        }
        return 0;
    }

    private static int giveResearch(final CommandSourceStack source, final ServerPlayer player,
                                   final String research) {
        if (ResearchManager.graph().entry(research) == null) {
            say(source, "§cResearch does not exist.");
            return 0;
        }
        giveRecursiveResearch(player, research);
        say(player, "§5" + source.getTextName() + " gave you " + research + " research and its requisites.");
        say(source, "§5Success!");
        return 1;
    }

    /** Original {@code giveRecursiveResearch}: the entry, then parents, hidden parents and siblings. */
    private static void giveRecursiveResearch(final ServerPlayer player, final String research) {
        if (ResearchManager.isResearchComplete(player.getData(ModAttachments.PLAYER_KNOWLEDGE), research)) {
            return;
        }
        ResearchManager.completeResearch(player, research);
        final ResearchEntryData entry = ResearchManager.graph().entry(research);
        if (entry == null) {
            return;
        }
        final List<List<String>> groups = new ArrayList<>();
        if (entry.parents() != null) {
            groups.add(entry.parents());
        }
        if (entry.parentsHidden() != null) {
            groups.add(entry.parentsHidden());
        }
        if (entry.siblings() != null) {
            groups.add(entry.siblings());
        }
        for (final List<String> group : groups) {
            for (final String child : group) {
                giveRecursiveResearch(player, child);
            }
        }
    }

    private static int giveAllResearch(final CommandSourceStack source, final ServerPlayer player) {
        for (final ResearchEntryData entry : ResearchManager.graph().entries()) {
            ResearchManager.completeResearch(player, entry.key());
        }
        say(player, "§5" + source.getTextName() + " has given you all research.");
        say(source, "§5Success!");
        return 1;
    }

    private static int resetResearch(final CommandSourceStack source, final ServerPlayer player) {
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, new PlayerKnowledgeData(
                List.of(), knowledge.aspectsDiscovered(), knowledge.scannedObjects(),
                knowledge.scannedEntities(), knowledge.scannedPhenomena()));
        for (final ResearchEntryData entry : ResearchManager.graph().entries()) {
            final ResearchEntryData.Flags flags = entry.flags();
            if (flags != null && flags.autoUnlock()) {
                ResearchManager.completeResearch(player, entry.key());
            }
        }
        say(player, "§5" + source.getTextName() + " has reset you research.");
        say(source, "§5Success!");
        return 1;
    }

    // -------------------------------------------------------------------- aspect

    private static int giveAspect(final CommandSourceStack source, final ServerPlayer player,
                                 final String aspect, final int amount) {
        if (aspect.equalsIgnoreCase("all")) {
            PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE);
            for (final String tag : ASPECT_TAGS) {
                knowledge = knowledge.withAspectPoolAdded(tag, amount);
            }
            player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
            say(player, "§5" + source.getTextName() + " gave you " + amount + " of all the aspects.");
            say(source, "§5Success!");
            return 1;
        }
        final String tag = aspect.toLowerCase(java.util.Locale.ROOT);
        if (!ASPECT_TAGS.contains(tag)) {
            say(source, "§cAspect does not exist.");
            return 0;
        }
        final PlayerKnowledgeData knowledge = player.getData(ModAttachments.PLAYER_KNOWLEDGE)
                .withAspectPoolAdded(tag, amount);
        player.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
        // The original prints the localized aspect name; lang assets are TC-M8, so the
        // tag stands in until then.
        say(player, "§5" + source.getTextName() + " gave you " + amount + " " + tag);
        say(source, "§5Success!");
        return 1;
    }

    // ---------------------------------------------------------------------- warp

    private static int setWarp(final CommandSourceStack source, final ServerPlayer player,
                              final int amount, final String kind) {
        PlayerWarpData warp = player.getData(ModAttachments.PLAYER_WARP);
        final int syncKind;
        if (kind.equalsIgnoreCase("PERM")) {
            warp = warp.withWarpPerm(amount);
            syncKind = SyncWarpPayload.WARP_PERMANENT;
        } else if (kind.equalsIgnoreCase("TEMP")) {
            warp = warp.withWarpTemp(amount);
            syncKind = SyncWarpPayload.WARP_TEMPORARY;
        } else {
            warp = warp.withWarpSticky(amount);
            syncKind = SyncWarpPayload.WARP_STICKY;
        }
        player.setData(ModAttachments.PLAYER_WARP, warp);
        final int value = switch (syncKind) {
            case SyncWarpPayload.WARP_PERMANENT -> warp.warpPerm();
            case SyncWarpPayload.WARP_TEMPORARY -> warp.warpTemp();
            default -> warp.warpSticky();
        };
        PacketDistributor.sendToPlayer(player, new SyncWarpPayload(value, syncKind));
        say(player, "§5" + source.getTextName() + " set your warp to " + amount);
        say(source, "§5Success!");
        return 1;
    }

    private static int addWarp(final CommandSourceStack source, final ServerPlayer player,
                              final int amount, final String kind) {
        PlayerWarpData warp = player.getData(ModAttachments.PLAYER_WARP);
        final int syncKind;
        if (kind.equalsIgnoreCase("PERM")) {
            warp = warp.addWarpPerm(amount);
            syncKind = SyncWarpPayload.WARP_PERMANENT;
        } else if (kind.equalsIgnoreCase("TEMP")) {
            warp = warp.addWarpTemp(amount);
            syncKind = SyncWarpPayload.WARP_TEMPORARY;
        } else {
            warp = warp.addWarpSticky(amount);
            syncKind = SyncWarpPayload.WARP_STICKY;
        }
        player.setData(ModAttachments.PLAYER_WARP, warp);
        final int value = switch (syncKind) {
            case SyncWarpPayload.WARP_PERMANENT -> warp.warpPerm();
            case SyncWarpPayload.WARP_TEMPORARY -> warp.warpTemp();
            default -> warp.warpSticky();
        };
        PacketDistributor.sendToPlayer(player, new SyncWarpPayload(value, syncKind));
        PacketDistributor.sendToPlayer(player, new WarpMessagePayload(amount, syncKind));
        say(player, "§5" + source.getTextName() + " added " + amount + " warp to your total.");
        say(source, "§5Success!");
        return 1;
    }

    private static void say(final CommandSourceStack source, final String message) {
        source.sendSystemMessage(Component.literal(message));
    }

    private static void say(final ServerPlayer player, final String message) {
        player.displayClientMessage(Component.literal(message), false);
    }
}
