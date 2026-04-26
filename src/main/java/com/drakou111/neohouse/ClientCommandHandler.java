package com.drakou111.neohouse;

import com.drakou111.neohouse.compiler.NHCompiler;
import com.drakou111.neohouse.compiler.ScriptRunner;
import com.drakou111.neohouse.ui.ConfigScreen;
import com.drakou111.neohouse.ui.FileBrowserScreen;
import com.drakou111.neohouse.ui.HelpScreen;
import com.drakou111.neohouse.ui.HomeScreen;
import com.drakou111.neohouse.utils.MessageUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@EventBusSubscriber(modid = NeoHouse.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientCommandHandler {

    private static final ScriptRunner NH_SCRIPT_RUNNER = new ScriptRunner(new NHCompiler());

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("neohouse")
            .executes(ctx -> {
                if (Minecraft.getInstance().player == null) return 0;
                Minecraft.getInstance().setScreen(new HomeScreen(ClientCommandHandler::getScriptsDir));
                return 1;
            })
            .then(Commands.literal("files").executes(ctx -> {
                if (Minecraft.getInstance().player == null) return 0;
                Minecraft.getInstance().setScreen(new FileBrowserScreen(getScriptsDir()));
                return 1;
            }))
            .then(Commands.literal("help").executes(ctx -> {
                if (Minecraft.getInstance().player == null) return 0;
                Minecraft.getInstance().setScreen(new HelpScreen());
                return 1;
            }))
            .then(Commands.literal("config").executes(ctx -> {
                if (Minecraft.getInstance().player == null) return 0;
                Minecraft.getInstance().setScreen(new ConfigScreen());
                return 1;
            }))
            .then(Commands.literal("run")
                .then(Commands.argument("path", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        if (Minecraft.getInstance().player == null) {
                            MessageUtils.errorToChat("No player context!");
                            return 0;
                        }
                        Path scriptFile = resolveScriptPath(StringArgumentType.getString(ctx, "path"));
                        if (scriptFile == null) return 0;
                        try {
                            NH_SCRIPT_RUNNER.runScript(scriptFile);
                            MessageUtils.debugToChat("Running: " + scriptFile.getFileName());
                            return 1;
                        } catch (Exception e) {
                            MessageUtils.errorToChat(e.getMessage());
                            return 0;
                        }
                    })
                )
            )
        );
    }

    /** Returns the neohouse-scripts/ folder inside the game directory, creating it if absent. */
    public static Path getScriptsDir() {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("neohouse-scripts");
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException ignored) {}
        }
        return dir;
    }

    /**
     * Resolves a script path argument:
     *  - strips surrounding quotes
     *  - auto-appends .nh if no extension is present
     *  - resolves bare filenames relative to the neohouse-scripts/ folder
     *  - validates the extension and that the file exists
     */
    private static Path resolveScriptPath(String raw) {
        String s = raw.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1);
        }

        // auto-add .nh if no extension
        if (!s.contains(".")) s = s + ".nh";

        Path p = Paths.get(s);
        if (!p.isAbsolute()) {
            p = getScriptsDir().resolve(p);
        }

        String name = p.getFileName().toString().toLowerCase();
        if (!name.endsWith(".nh")) {
            MessageUtils.errorToChat("Unsupported extension '" + name + "' — only .nh files are supported.");
            return null;
        }
        if (!Files.exists(p)) {
            MessageUtils.errorToChat("Script not found: " + p);
            return null;
        }
        return p;
    }
}
