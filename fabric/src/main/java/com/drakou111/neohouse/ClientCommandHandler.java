package com.drakou111.neohouse;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/**
 * Fabric Brigadier wiring for {@code /nh} and {@code /neohouse}. All behaviour
 * lives in {@link CommandActions}; this class only maps it to the command tree.
 */
public class ClientCommandHandler {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildCommand("neohouse"));
            dispatcher.register(buildCommand("nh"));
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommand(String name) {
        return ClientCommandManager.literal(name)
           .executes(ctx -> CommandActions.openHome())
           .then(ClientCommandManager.literal("files").executes(ctx -> CommandActions.openFiles()))
           .then(ClientCommandManager.literal("help").executes(ctx -> CommandActions.openHelp()))
           .then(ClientCommandManager.literal("config").executes(ctx -> CommandActions.openConfig()))
           .then(ClientCommandManager.literal("nuke").executes(ctx -> CommandActions.guarded(CommandActions::handleNuke)))
           .then(ClientCommandManager.literal("delete")
                 .then(ClientCommandManager.literal("functions").executes(ctx -> CommandActions.guarded(CommandActions::handleDeleteFunctions)))
                 .then(ClientCommandManager.literal("events").executes(ctx -> CommandActions.guarded(CommandActions::handleDeleteEvents)))
                 .then(ClientCommandManager.literal("commands").executes(ctx -> CommandActions.guarded(CommandActions::handleDeleteCommands)))
                 .then(ClientCommandManager.literal("menus").executes(ctx -> CommandActions.guarded(CommandActions::handleDeleteMenus)))
            )
           .then(ClientCommandManager.literal("copyitem").executes(ctx -> CommandActions.copyHeldItem()))
           .then(ClientCommandManager.literal("rerun").executes(ctx -> CommandActions.rerunLast()))
           .then(ClientCommandManager.literal("giveitem")
                 .executes(ctx -> CommandActions.giveItemDialog())
                 .then(ClientCommandManager.argument("item", StringArgumentType.greedyString())
                       .executes(ctx -> {
                           CommandActions.handleGiveItem(StringArgumentType.getString(ctx, "item").trim());
                           return 1;
                       })
                 )
            )
           .then(ClientCommandManager.literal("list")
                 .then(ClientCommandManager.literal("functions").executes(ctx -> CommandActions.listHelperFunctions()))
                 .then(ClientCommandManager.literal("variables").executes(ctx -> CommandActions.listExtraVariables()))
            )
           .then(ClientCommandManager.literal("run")
                 .then(ClientCommandManager.argument("path", StringArgumentType.greedyString())
                       .suggests((ctx, builder) -> {
                           CommandActions.scriptSuggestions(builder.getRemaining()).forEach(builder::suggest);
                           return builder.buildFuture();
                       })
                       .executes(ctx -> CommandActions.runPath(StringArgumentType.getString(ctx, "path")))
                  )
            );
    }
}
