package com.drakou111.neohouse;

import com.drakou111.neohouse.compat.FabricHooks;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
        return FabricHooks.literal(name)
           .executes(ctx -> CommandActions.openHome())
           .then(FabricHooks.literal("files").executes(ctx -> CommandActions.openFiles()))
           .then(FabricHooks.literal("music").executes(ctx -> CommandActions.openMusic()))
           .then(FabricHooks.literal("help").executes(ctx -> CommandActions.openHelp()))
           .then(FabricHooks.literal("config").executes(ctx -> CommandActions.openConfig()))
           .then(FabricHooks.literal("nuke").executes(ctx -> CommandActions.guarded(CommandActions::handleNuke)))
           .then(FabricHooks.literal("delete")
                 .then(FabricHooks.literal("functions").executes(ctx -> CommandActions.guarded(CommandActions::handleDeleteFunctions)))
                 .then(FabricHooks.literal("events").executes(ctx -> CommandActions.guarded(CommandActions::handleDeleteEvents)))
                 .then(FabricHooks.literal("commands").executes(ctx -> CommandActions.guarded(CommandActions::handleDeleteCommands)))
                 .then(FabricHooks.literal("menus").executes(ctx -> CommandActions.guarded(CommandActions::handleDeleteMenus)))
            )
           .then(FabricHooks.literal("copyitem").executes(ctx -> CommandActions.copyHeldItem()))
           .then(FabricHooks.literal("rerun").executes(ctx -> CommandActions.rerunLast()))
           .then(FabricHooks.literal("giveitem")
                 .executes(ctx -> CommandActions.giveItemDialog())
                 .then(FabricHooks.argument("item", StringArgumentType.greedyString())
                       .executes(ctx -> {
                           CommandActions.handleGiveItem(StringArgumentType.getString(ctx, "item").trim());
                           return 1;
                       })
                 )
            )
           .then(FabricHooks.literal("list")
                 .then(FabricHooks.literal("functions").executes(ctx -> CommandActions.listHelperFunctions()))
                 .then(FabricHooks.literal("variables").executes(ctx -> CommandActions.listExtraVariables()))
                 .then(FabricHooks.literal("warnings").executes(ctx -> CommandActions.listRunWarnings()))
            )
           .then(FabricHooks.literal("run")
                 .then(FabricHooks.argument("path", StringArgumentType.greedyString())
                       .suggests((ctx, builder) -> {
                           CommandActions.scriptSuggestions(builder.getRemaining()).forEach(builder::suggest);
                           return builder.buildFuture();
                       })
                       .executes(ctx -> CommandActions.runPath(StringArgumentType.getString(ctx, "path")))
                  )
            );
    }
}
