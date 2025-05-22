package com.drakou111.neohouse;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.GroupAction;
import com.drakou111.neohouse.compilers.DummyNHCompiler;
import com.drakou111.neohouse.compilers.ScriptRunner;
import com.drakou111.neohouse.exceptions.CompileException;
import com.drakou111.neohouse.factories.MovementFactory;
import com.drakou111.neohouse.utils.ActionBuilder;
import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.SequenceManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = NeoHouse.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientCommandHandler {

    private static final ScriptRunner NH_SCRIPT_RUNNER = new ScriptRunner(new DummyNHCompiler());

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("neohouse").then(
                Commands.literal("example").executes(ctx -> {
                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player != null) {
                        List<Action> actions = new ArrayList<>();

                        actions.add(new ActionBuilder().sendChatAndWait(com.drakou111.neohouse.constants.Commands.FUNCTIONS).get());
                        actions.add(MovementFactory.recreateFunction("testing", "some testing", null, true, 45));

                        SequenceManager.startAction(new GroupAction(actions));
                    }
                    return 1; // success
                })
            )
            .then(Commands.literal("run")
            .then(Commands.argument("path", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String rawPath = StringArgumentType.getString(ctx, "path");
                        LocalPlayer player = Minecraft.getInstance().player;
                        if (player == null) {
                            MessageUtils.errorToChat("No player context!");
                            return 0;
                        }

                        // strip quotes if present
                        String cleaned = rawPath;
                        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
                                (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
                            cleaned = cleaned.substring(1, cleaned.length() - 1);
                        }

                        Path scriptFile = Paths.get(cleaned);
                        String fileName = scriptFile.getFileName().toString().toLowerCase();
                        if (!(fileName.endsWith(".nh") || fileName.endsWith(".nhc"))) {
                            MessageUtils.errorToChat("Unsupported extension for file: " + fileName + " (must be .nh or .nhc)");
                            return 0;
                        }

                        try {
                            if (fileName.endsWith(".nh")) {
                                NH_SCRIPT_RUNNER.runScript(scriptFile);
                                MessageUtils.debugToChat("Running script: " + scriptFile);
                                return 1;
                            }
                            return 0;
                        } catch (CompileException e) {
                            MessageUtils.errorToChat("Script compile/run error: " + e.getMessage());
                            return 0;
                        }
                    })
                )
            )
        );
    }
}
