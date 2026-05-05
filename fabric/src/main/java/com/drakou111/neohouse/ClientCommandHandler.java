package com.drakou111.neohouse;

import com.drakou111.neohouse.actions.actiontypes.DeleteAllAction;
import com.drakou111.neohouse.actions.actiontypes.DeleteAllCommandsAction;
import com.drakou111.neohouse.actions.actiontypes.DeleteAllFunctionsAction;
import com.drakou111.neohouse.actions.actiontypes.DeleteAllMenusAction;
import com.drakou111.neohouse.compiler.ScriptRunner;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Others;
import com.drakou111.neohouse.exceptions.PermissionException;
import com.drakou111.neohouse.ui.*;
import com.drakou111.neohouse.utils.ActionBuilder;
import com.drakou111.neohouse.utils.HousingCheck;
import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.SequenceManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientCommandHandler {

    private static final ScriptRunner NH_SCRIPT_RUNNER = ScriptRunner.INSTANCE;

    // Defers screen opening to the next tick via TickDriver.
    // mc.execute() runs synchronously on the game thread, so it can't be used here —
    // the chat screen would close AFTER our setScreen, replacing it with null.
    private static int openScreen(java.util.function.Supplier<net.minecraft.client.gui.screens.Screen> screenFactory) {
        if (Minecraft.getInstance().player == null) return 0;
        TickDriver.openScreenNextTick(screenFactory.get());
        return 1;
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildCommand("neohouse"));
            dispatcher.register(buildCommand("nh"));
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommand(String name) {
        return ClientCommandManager.literal(name)
           .executes(ctx -> openScreen(() -> new HomeScreen(ClientCommandHandler::getScriptsDir)))
           .then(ClientCommandManager.literal("files").executes(ctx -> openScreen(() -> new FileBrowserScreen(getScriptsDir()))))
           .then(ClientCommandManager.literal("help").executes(ctx -> openScreen(() -> new HelpScreen(null))))
           .then(ClientCommandManager.literal("config").executes(ctx -> openScreen(() -> new ConfigScreen(null))))
           .then(ClientCommandManager.literal("nuke").executes(ctx -> {
               if (Minecraft.getInstance().player == null) return 0;
               try {
                   return handleNuke();
               } catch (Exception e) {
                   MessageUtils.errorToChat(e.getMessage() != null ? e.getMessage() : e.toString());
                   return 0;
               }
           }))
           .then(ClientCommandManager.literal("delete")
                 .then(ClientCommandManager.literal("functions").executes(ctx -> {
                     if (Minecraft.getInstance().player == null) return 0;
                     try {
                         return handleDeleteFunctions();
                     } catch (Exception e) {
                         MessageUtils.errorToChat(e.toString());
                         return 0;
                     }
                 }))
                 .then(ClientCommandManager.literal("events").executes(ctx -> {
                     if (Minecraft.getInstance().player == null) return 0;
                     try {
                         return handleDeleteEvents();
                     } catch (Exception e) {
                         MessageUtils.errorToChat(e.toString());
                         return 0;
                     }
                 }))
                 .then(ClientCommandManager.literal("commands").executes(ctx -> {
                     if (Minecraft.getInstance().player == null) return 0;
                     try {
                         return handleDeleteCommands();
                     } catch (Exception e) {
                         MessageUtils.errorToChat(e.toString());
                         return 0;
                     }
                 }))
                 .then(ClientCommandManager.literal("menus").executes(ctx -> {
                     if (Minecraft.getInstance().player == null) return 0;
                     try {
                         return handleDeleteMenus();
                     } catch (Exception e) {
                         MessageUtils.errorToChat(e.toString());
                         return 0;
                     }
                 }))
            )
           .then(ClientCommandManager.literal("copyitem").executes(ctx -> {
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
               mc.keyboardHandler.setClipboard(fullNbt.toString());
               MessageUtils.debugToChat("Full item data copied to clipboard!");
               return 1;
           }))
           .then(ClientCommandManager.literal("rerun").executes(ctx -> {
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
                   MessageUtils.errorToChat(e.toString());
                   return 0;
               }
           }))
           .then(ClientCommandManager.literal("run")
                 .then(ClientCommandManager.argument("path", StringArgumentType.greedyString())
                       .executes(ctx -> {
                           if (Minecraft.getInstance().player == null) {
                               MessageUtils.errorToChat("No player context!");
                               return 0;
                           }

                           Path scriptFile = resolveScriptPath(StringArgumentType.getString(ctx, "path"));
                           if (scriptFile == null)
                               return 0;

                           try {
                               NH_SCRIPT_RUNNER.runScript(scriptFile);
                               MessageUtils.debugToChat("Running: " + scriptFile.getFileName());
                               return 1;
                           } catch (Exception e) {
                               MessageUtils.errorToChat(e.toString());
                               return 0;
                           }
                       })
                  )
            );
    }

    private static int handleNuke() {
        if (!HousingCheck.isInHousing()) {
            throw new PermissionException("You must be in Hypixel Housing to nuke.");
        }
        TickDriver.openScreenNextTick(new InputDialogScreen(
                "Type \"Delete\" to permanently destroy\nALL functions, events, commands and menus.\n(Use '/neohouse delete <section>' to delete a specific section)",
                "", null,
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
        TickDriver.openScreenNextTick(new InputDialogScreen(
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
        TickDriver.openScreenNextTick(new InputDialogScreen(
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
        TickDriver.openScreenNextTick(new InputDialogScreen(
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
        TickDriver.openScreenNextTick(new InputDialogScreen(
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

    public static Path getScriptsDir() {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("neohouse-scripts");
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException ignored) {
            }
        }
        return dir;
    }

    private static Path resolveScriptPath(String raw) {
        String s = raw.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1);
        }
        if (!s.contains(".")) s = s + ".nh";
        Path p = Paths.get(s);
        if (!p.isAbsolute()) p = getScriptsDir().resolve(p);
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
