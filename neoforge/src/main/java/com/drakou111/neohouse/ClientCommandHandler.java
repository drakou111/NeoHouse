package com.drakou111.neohouse;

import com.drakou111.neohouse.actions.actiontypes.*;
import com.drakou111.neohouse.compiler.ScriptRunner;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Others;
import com.drakou111.neohouse.exceptions.PermissionException;
import com.drakou111.neohouse.ui.*;
import com.drakou111.neohouse.utils.ActionBuilder;
import com.drakou111.neohouse.utils.HousingCheck;
import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.SequenceManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@EventBusSubscriber(modid = NHConstants.MOD_ID, value = Dist.CLIENT)
public class ClientCommandHandler {

    private static final ScriptRunner NH_SCRIPT_RUNNER = ScriptRunner.INSTANCE;

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(buildCommand("neohouse"));
        dispatcher.register(buildCommand("nh"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand(String name) {
        return Commands.literal(name)
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
                           Minecraft.getInstance().setScreen(new HelpScreen(null));
                           return 1;
                       }))
                       .then(Commands.literal("config").executes(ctx -> {
                           if (Minecraft.getInstance().player == null) return 0;
                           Minecraft.getInstance().setScreen(new ConfigScreen(null));
                           return 1;
                       }))
                       .then(Commands.literal("nuke").executes(ctx -> {
                           if (Minecraft.getInstance().player == null) return 0;
                           try {
                               return handleNuke();
                           } catch (Exception e) {
                               MessageUtils.errorToChat(e.getMessage() != null ? e.getMessage() : e.toString());
                               return 0;
                           }
                       }))
                       .then(Commands.literal("delete")
                                     .then(Commands.literal("functions").executes(ctx -> {

                                         if (Minecraft.getInstance().player == null) return 0;
                                         try {
                                             return handleDeleteFunctions();
                                         } catch (Exception e) {
                                             MessageUtils.errorToChat(e.getMessage() != null ? e.getMessage() : e.toString());
                                             return 0;
                                         }
                                     }))
                                     .then(Commands.literal("events").executes(ctx -> {
                                         if (Minecraft.getInstance().player == null) return 0;
                                         try {
                                             return handleDeleteEvents();
                                         } catch (Exception e) {
                                             MessageUtils.errorToChat(e.getMessage() != null ? e.getMessage() : e.toString());
                                             return 0;
                                         }
                                     }))
                                     .then(Commands.literal("commands").executes(ctx -> {
                                         if (Minecraft.getInstance().player == null) return 0;
                                         try {
                                             return handleDeleteCommands();
                                         } catch (Exception e) {
                                             MessageUtils.errorToChat(e.getMessage() != null ? e.getMessage() : e.toString());
                                             return 0;
                                         }
                                     }))
                                     .then(Commands.literal("menus").executes(ctx -> {
                                         if (Minecraft.getInstance().player == null) return 0;
                                         try {
                                             return handleDeleteMenus();
                                         } catch (Exception e) {
                                             MessageUtils.errorToChat(e.getMessage() != null ? e.getMessage() : e.toString());
                                             return 0;
                                         }
                                     }))
                            )
                       .then(Commands.literal("copyitem").executes(ctx -> {
                           Minecraft mc = Minecraft.getInstance();
                           if (mc.player == null || mc.level == null) return 0;

                           ItemStack stack = mc.player.getMainHandItem();
                           if (stack.isEmpty()) {
                               MessageUtils.errorToChat("You must be holding an item to copy its data.");
                               return 0;
                           }

                           net.minecraft.resources.RegistryOps<net.minecraft.nbt.Tag> _ops =
                                   mc.level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                           net.minecraft.nbt.Tag fullNbt = net.minecraft.world.item.ItemStack.CODEC.encodeStart(_ops, stack).getOrThrow();

                           String snbt = fullNbt.toString();

                           mc.keyboardHandler.setClipboard(snbt);

                           MessageUtils.debugToChat("Full item data copied to clipboard!");
                           return 1;
                       }))
                       .then(Commands.literal("rerun").executes(ctx -> {
                           if (Minecraft.getInstance().player == null) return 0;
                           Path last = NH_SCRIPT_RUNNER.getLastScript();
                           if (last == null) {
                               MessageUtils.errorToChat("No script has been run yet this session.");
                               return 0;
                           }
                           try {
                               NH_SCRIPT_RUNNER.runScript(last);
                               MessageUtils.debugToChat("Running: " + last.getFileName());
                               return 1;
                           } catch (Exception e) {
                               MessageUtils.errorToChat(e.getMessage() != null ? e.getMessage() : e.toString());
                               return 0;
                           }
                       }))
                       .then(Commands.literal("giveitem")
                             .executes(ctx -> {
                                 if (Minecraft.getInstance().player == null) return 0;
                                 Minecraft.getInstance().setScreen(new InputDialogScreen(
                                         "Enter item data (SNBT format):",
                                         "", null,
                                         itemData -> handleGiveItem(itemData.trim())
                                 ));
                                 return 1;
                             })
                             .then(Commands.argument("item", StringArgumentType.greedyString())
                                   .executes(ctx -> {
                                       if (Minecraft.getInstance().player == null) return 0;
                                       handleGiveItem(StringArgumentType.getString(ctx, "item").trim());
                                       return 1;
                                   })
                             )
                        )
                       .then(Commands.literal("run")
                             .then(Commands.argument("path", StringArgumentType.greedyString())
                                   .suggests((ctx, builder) -> {
                                       Path root = getScriptsDir();
                                       try (var walk = Files.walk(root)) {
                                           walk.filter(p -> p.toString().endsWith(".nh"))
                                               .map(p -> root.relativize(p).toString().replace('\\', '/'))
                                               .map(s -> s.endsWith(".nh") ? s.substring(0, s.length() - 3) : s)
                                               .filter(s -> s.startsWith(builder.getRemaining()))
                                               .forEach(builder::suggest);
                                       } catch (IOException ignored) {}
                                       return builder.buildFuture();
                                   })
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
                                           MessageUtils.errorToChat(e.getMessage() != null ? e.getMessage() : e.toString());
                                           return 0;
                                       }
                                   })
                              )
                        );
    }

    private static int handleNuke() {
        Minecraft mc = Minecraft.getInstance();
        if (!HousingCheck.isInHousing()) {
            throw new PermissionException("You must be in Hypixel Housing to nuke.");
        }
        mc.setScreen(new InputDialogScreen(
                "Type \"Delete\" to permanently destroy\nALL functions, events, commands and menus.\n(Use '/neohouse delete <section>' to delete a specific section)",
                "",
                null,
                typed -> {
                    if (!typed.equals("Delete")) {
                        throw new PermissionException("Nuke cancelled — you didn't type \"Delete\".");
                    }
                    if (!HousingCheck.isInHousing()) {
                        throw new PermissionException("You must be in Hypixel Housing to nuke.");
                    }
                    SequenceManager.cancelAction();

                    ActionBuilder builder = new ActionBuilder()
                            .closeAll()
                            .sendChatAndWait(com.drakou111.neohouse.constants.Commands.FUNCTIONS)
                            .append(new DeleteAllFunctionsAction())
                            .closeAll()
                            .sendChatAndWait(com.drakou111.neohouse.constants.Commands.COMMANDS)
                            .append(new DeleteAllCommandsAction())
                            .closeAll()
                            .sendChatAndWait(com.drakou111.neohouse.constants.Commands.CUSTOM_MENUS)
                            .append(new DeleteAllMenusAction())
                            .closeAll()
                            .sendChatAndWait(com.drakou111.neohouse.constants.Commands.EVENT_ACTION);

                    for (Event event : Event.values()) {
                        builder.clickSlotAndWait(event.getSlotID(), true)
                               .append(new DeleteAllAction(10))
                               .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true);
                    }

                    SequenceManager.startAction(builder.closeAll().get());
                    MessageUtils.debugToChat("Nuking housing.");
                }
        ));
        return 1;
    }

    private static int handleDeleteFunctions() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new InputDialogScreen(
                "Type \"Delete\" to permanently destroy ALL functions.",
                "", null,
                typed -> {
                    if (!typed.equals("Delete"))
                        throw new PermissionException("Cancelled — you didn't type \"Delete\".");
                    if (!HousingCheck.isInHousing()) throw new PermissionException("You must be in Hypixel Housing.");
                    SequenceManager.cancelAction();
                    SequenceManager.startAction(new ActionBuilder()
                                                        .closeAll()
                                                        .sendChatAndWait(com.drakou111.neohouse.constants.Commands.FUNCTIONS)
                                                        .append(new DeleteAllFunctionsAction())
                                                        .closeAll()
                                                        .get());
                    MessageUtils.debugToChat("Deleting all functions...");
                }
        ));
        return 1;
    }

    private static int handleDeleteCommands() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new InputDialogScreen(
                "Type \"Delete\" to permanently destroy ALL commands.",
                "", null,
                typed -> {
                    if (!typed.equals("Delete"))
                        throw new PermissionException("Cancelled — you didn't type \"Delete\".");
                    if (!HousingCheck.isInHousing()) throw new PermissionException("You must be in Hypixel Housing.");
                    SequenceManager.cancelAction();
                    SequenceManager.startAction(new ActionBuilder()
                                                        .closeAll()
                                                        .sendChatAndWait(com.drakou111.neohouse.constants.Commands.COMMANDS)
                                                        .append(new DeleteAllCommandsAction())
                                                        .closeAll()
                                                        .get());
                    MessageUtils.debugToChat("Deleting all commands...");
                }
        ));
        return 1;
    }

    private static int handleDeleteEvents() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new InputDialogScreen(
                "Type \"Delete\" to permanently destroy ALL event actions.",
                "", null,
                typed -> {
                    if (!typed.equals("Delete"))
                        throw new PermissionException("Cancelled — you didn't type \"Delete\".");
                    if (!HousingCheck.isInHousing()) throw new PermissionException("You must be in Hypixel Housing.");
                    SequenceManager.cancelAction();
                    ActionBuilder builder = new ActionBuilder()
                            .closeAll()
                            .sendChatAndWait(com.drakou111.neohouse.constants.Commands.EVENT_ACTION);
                    for (Event event : Event.values()) {
                        builder.clickSlotAndWait(event.getSlotID(), true)
                               .append(new DeleteAllAction(10))
                               .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true);
                    }
                    SequenceManager.startAction(builder.closeAll().get());
                    MessageUtils.debugToChat("Deleting all event actions...");
                }
        ));
        return 1;
    }

    private static int handleDeleteMenus() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new InputDialogScreen(
                "Type \"Delete\" to permanently destroy ALL custom menus.",
                "", null,
                typed -> {
                    if (!typed.equals("Delete"))
                        throw new PermissionException("Cancelled — you didn't type \"Delete\".");
                    if (!HousingCheck.isInHousing()) throw new PermissionException("You must be in Hypixel Housing.");
                    SequenceManager.cancelAction();
                    SequenceManager.startAction(new ActionBuilder()
                                                        .closeAll()
                                                        .sendChatAndWait(com.drakou111.neohouse.constants.Commands.CUSTOM_MENUS)
                                                        .append(new DeleteAllMenusAction())
                                                        .closeAll()
                                                        .get());
                    MessageUtils.debugToChat("Deleting all custom menus...");
                }
        ));
        return 1;
    }

    private static void handleGiveItem(String itemData) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.player.getAbilities().instabuild) {
            MessageUtils.errorToChat("You must be in creative mode to give items.");
            return;
        }
        if (!HousingCheck.isInHousing()) {
            MessageUtils.errorToChat("You must be in Hypixel Housing to give items.");
            return;
        }
        SequenceManager.startAction(new CreativeGiveAction(itemData));
    }

    public static Path getScriptsDir() {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("neohouse-scripts");
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path marker = dir.resolve(".initialized");
            if (!Files.exists(marker)) {
                writeDefaultExample(dir.resolve("example.nh"));
                Files.createFile(marker);
            }
        } catch (IOException ignored) {
        }
        return dir;
    }

    private static void writeDefaultExample(Path example) {
        String content =
            "// ────────────────────────────\n" +
            "//   NeoHouse example script\n" +
            "// ────────────────────────────\n" +
            "\n" +
            "const long KILL_REWARD = 10;\n" +
            "const long VETERAN_THRESHOLD = 50;\n" +
            "\n" +
            "long g.totalJoins = 0;\n" +
            "long p.coins;\n" +
            "\n" +
            "// Functions\n" +
            "function greet() {\n" +
            "    message(\"Welcome, \" + ph.playerName() + \"! Coins: \" + p.coins);\n" +
            "}\n" +
            "\n" +
            "// Events\n" +
            "event onJoin {\n" +
            "    g.totalJoins += 1;\n" +
            "    greet();\n" +
            "    message(\"Total visits: \" + g.totalJoins);\n" +
            "}\n" +
            "\n" +
            "event onKill {\n" +
            "    p.coins += KILL_REWARD;\n" +
            "    message(\"Kill! Coins: \" + p.coins);\n" +
            "}\n" +
            "\n" +
            "// Commands\n" +
            "command coins(listed: true) {\n" +
            "    if (p.coins >= VETERAN_THRESHOLD) {\n" +
            "        message(\"&aYou are a veteran with \" + p.coins + \" coins!\");\n" +
            "    } else {\n" +
            "        message(\"&cNot yet veteran. Coins: \" + p.coins);\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "// Menus\n" +
            "menu shop(4) {\n" +
            "    slot0: ({count:1,id:\"minecraft:diamond\"}) {" +
            "       message(\"You clicked the diamond!\");" +
            "       closeMenu();" +
            "    }" +
            "}\n" +
            "\n" +
            "command resetcoins(listed: false) {\n" +
            "    p.coins = 0;\n" +
            "    message(\"&cCoins reset.\");\n" +
            "}\n";
        try {
            Files.writeString(example, content, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

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
