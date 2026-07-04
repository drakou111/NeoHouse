package com.drakou111.neohouse;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * NeoForge Brigadier wiring for {@code /nh} and {@code /neohouse}. All behaviour
 * lives in {@link CommandActions}; this class only maps it to the command tree.
 */
@EventBusSubscriber(modid = NHConstants.MOD_ID, value = Dist.CLIENT)
public class ClientCommandHandler {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(buildCommand("neohouse"));
        dispatcher.register(buildCommand("nh"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand(String name) {
        return Commands.literal(name)
                .executes(ctx -> CommandActions.openHome())
                .then(Commands.literal("files").executes(ctx -> CommandActions.openFiles()))
                .then(Commands.literal("help").executes(ctx -> CommandActions.openHelp()))
                .then(Commands.literal("config").executes(ctx -> CommandActions.openConfig()))
                .then(Commands.literal("nuke").executes(ctx -> CommandActions.guarded(CommandActions::handleNuke)))
                .then(Commands.literal("delete")
                        .then(Commands.literal("functions").executes(ctx -> CommandActions.guarded(CommandActions::handleDeleteFunctions)))
                        .then(Commands.literal("events").executes(ctx -> CommandActions.guarded(CommandActions::handleDeleteEvents)))
                        .then(Commands.literal("commands").executes(ctx -> CommandActions.guarded(CommandActions::handleDeleteCommands)))
                        .then(Commands.literal("menus").executes(ctx -> CommandActions.guarded(CommandActions::handleDeleteMenus)))
                )
                .then(Commands.literal("copyitem").executes(ctx -> CommandActions.copyHeldItem()))
                .then(Commands.literal("rerun").executes(ctx -> CommandActions.rerunLast()))
                .then(Commands.literal("giveitem")
                        .executes(ctx -> CommandActions.giveItemDialog())
                        .then(Commands.argument("item", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    CommandActions.handleGiveItem(StringArgumentType.getString(ctx, "item").trim());
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("list")
                        .then(Commands.literal("functions").executes(ctx -> CommandActions.listHelperFunctions()))
                        .then(Commands.literal("variables").executes(ctx -> CommandActions.listExtraVariables()))
                )
                .then(Commands.literal("run")
                        .then(Commands.argument("path", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    CommandActions.scriptSuggestions(builder.getRemaining()).forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> CommandActions.runPath(StringArgumentType.getString(ctx, "path")))
                        )
                );
    }
}
