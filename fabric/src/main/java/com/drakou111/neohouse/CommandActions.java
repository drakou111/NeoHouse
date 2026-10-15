package com.drakou111.neohouse;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.CreativeGiveAction;
import com.drakou111.neohouse.actions.actiontypes.DeleteAllAction;
import com.drakou111.neohouse.actions.actiontypes.DeleteListEntriesAction;
import com.drakou111.neohouse.compat.InventoryHelper;
import com.drakou111.neohouse.compiler.ScriptRunner;
import com.drakou111.neohouse.constants.Commands;
import com.drakou111.neohouse.constants.HousingList;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Others;
import com.drakou111.neohouse.exceptions.PermissionException;
import com.drakou111.neohouse.platform.Platforms;
import com.drakou111.neohouse.ui.ConfigScreen;
import com.drakou111.neohouse.ui.FileBrowserScreen;
import com.drakou111.neohouse.ui.HelpScreen;
import com.drakou111.neohouse.ui.HomeScreen;
import com.drakou111.neohouse.ui.InputDialogScreen;
import com.drakou111.neohouse.utils.ActionBuilder;
import com.drakou111.neohouse.utils.HousingCheck;
import com.drakou111.neohouse.utils.LibraryManager;
import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.MusicFolderMerge;
import com.drakou111.neohouse.utils.RunWarnings;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class CommandActions {

    private static final ScriptRunner SCRIPT_RUNNER = ScriptRunner.INSTANCE;
    private static final String OLD_MUSIC_FOLDER = "neohouse-music";

    private CommandActions() {}

    private static boolean noPlayer() {
        return Minecraft.getInstance().player == null;
    }

    public static int guarded(IntSupplier action) {
        try {
            return action.getAsInt();
        } catch (Exception e) {
            MessageUtils.errorToChat(e);
            return 0;
        }
    }

    public static int openScreen(Supplier<Screen> factory) {
        if (noPlayer()) return 0;
        Platforms.get().openScreenNextTick(factory.get());
        return 1;
    }

    // Screens opened by command get the home page as parent, so their Back button leads somewhere.
    public static int openHome()   { return openScreen(CommandActions::home); }
    public static int openFiles()  { return openScreen(() -> new FileBrowserScreen(getScriptsDir(), home())); }
    /** Music lives in the same folder as scripts now; kept so /nh music still works. */
    public static int openMusic()  { return openFiles(); }
    public static int openHelp()   { return openScreen(() -> new HelpScreen(home())); }
    public static int openConfig() { return openScreen(() -> new ConfigScreen(home())); }

    private static HomeScreen home() {
        return new HomeScreen(CommandActions::getScriptsDir);
    }

    public static int handleNuke() {
        if (noPlayer()) return 0;
        if (!HousingCheck.isInHousing()) {
            throw new PermissionException("You must be in Hypixel Housing to nuke.");
        }
        Platforms.get().openScreenNextTick(new InputDialogScreen(
                "Type \"Delete\" to permanently destroy\nALL functions, events, commands and menus.\n(Use '/neohouse delete <section>'\nto delete a specific section)",
                "", null,
                typed -> {
                    requireConfirmation(typed, "Nuke cancelled - you didn't type \"Delete\".");
                    ActionBuilder builder = new ActionBuilder();
                    deleteEntries(builder, HousingList.FUNCTIONS);
                    deleteEntries(builder, HousingList.COMMANDS);
                    deleteEntries(builder, HousingList.MENUS);
                    appendEventDeletes(builder);
                    SequenceManager.startAction(builder.closeAll().get());
                    MessageUtils.debugToChat("Nuking housing.");
                }
        ));
        return 1;
    }

    public static int handleDeleteFunctions() {
        return confirmDelete("Type \"Delete\" to permanently destroy ALL functions.",
                "Deleting all functions...",
                () -> deleteEntries(new ActionBuilder(), HousingList.FUNCTIONS).get());
    }

    public static int handleDeleteCommands() {
        return confirmDelete("Type \"Delete\" to permanently destroy ALL commands.",
                "Deleting all commands...",
                () -> deleteEntries(new ActionBuilder(), HousingList.COMMANDS).get());
    }

    public static int handleDeleteMenus() {
        return confirmDelete("Type \"Delete\" to permanently destroy ALL custom menus.",
                "Deleting all custom menus...",
                () -> deleteEntries(new ActionBuilder(), HousingList.MENUS).get());
    }

    /** Opens {@code list} and deletes every entry in it. */
    private static ActionBuilder deleteEntries(ActionBuilder builder, HousingList list) {
        return builder.closeAll()
                .sendChatAndWait(list.openCommand)
                .append(new DeleteListEntriesAction(list))
                .closeAll();
    }

    public static int handleDeleteEvents() {
        return confirmDelete("Type \"Delete\" to permanently destroy ALL event actions.",
                "Deleting all event actions...",
                () -> {
                    ActionBuilder builder = new ActionBuilder()
                            .closeAll()
                            .sendChatAndWait(Commands.EVENT_ACTION);
                    appendEventDeletes(builder);
                    return builder.closeAll().get();
                });
    }

    private static int confirmDelete(String prompt, String workingMsg, Supplier<Action> sequence) {
        if (noPlayer()) return 0;
        Platforms.get().openScreenNextTick(new InputDialogScreen(
                prompt, "", null,
                typed -> {
                    requireConfirmation(typed, "Cancelled - you didn't type \"Delete\".");
                    SequenceManager.cancelAction();
                    SequenceManager.startAction(sequence.get());
                    MessageUtils.debugToChat(workingMsg);
                }
        ));
        return 1;
    }

    private static void requireConfirmation(String typed, String cancelMsg) {
        if (!typed.equals("Delete")) throw new PermissionException(cancelMsg);
        if (!HousingCheck.isInHousing()) throw new PermissionException("You must be in Hypixel Housing.");
        SequenceManager.cancelAction();
    }

    private static void appendEventDeletes(ActionBuilder builder) {
        for (Event event : Event.values()) {
            builder.clickSlotAndWait(event.getSlotID(), true)
                   .append(new DeleteAllAction(10))
                   .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true);
        }
    }

    public static int copyHeldItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return 0;

        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) {
            MessageUtils.errorToChat("You must be holding an item to copy its data.");
            return 0;
        }

        RegistryOps<Tag> ops = mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag fullNbt = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        mc.keyboardHandler.setClipboard(fullNbt.toString());
        MessageUtils.debugToChat("Full item data copied to clipboard!");
        return 1;
    }

    public static int giveItemDialog() {
        if (noPlayer()) return 0;
        Platforms.get().openScreenNextTick(new InputDialogScreen(
                "Enter item data (SNBT format):",
                "", null,
                itemData -> handleGiveItem(itemData.trim())
        ));
        return 1;
    }

    public static void handleGiveItem(String itemData) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.player.getAbilities().instabuild) {
            MessageUtils.errorToChat("You must be in creative mode to give items.");
            return;
        }
        int hotbarSlot = InventoryHelper.getSelectedHotbarSlot(mc.player);
        // The creative-mode slot packet uses container indices; hotbar slots are 36-44.
        SequenceManager.startAction(new CreativeGiveAction(itemData, 36 + hotbarSlot));
        MessageUtils.debugToChat("Item placed in selected hotbar slot " + (hotbarSlot + 1) + ".");
    }

    public static int rerunLast() {
        if (noPlayer()) return 0;
        Path last = SCRIPT_RUNNER.getLastScript();
        if (last == null) {
            MessageUtils.errorToChat("No script has been run yet this session.");
            return 0;
        }
        return run(last);
    }

    public static int runPath(String rawPath) {
        if (noPlayer()) {
            MessageUtils.errorToChat("No player context!");
            return 0;
        }
        Path scriptFile = resolveScriptPath(rawPath);
        if (scriptFile == null) return 0;
        return run(scriptFile);
    }

    private static int run(Path scriptFile) {
        try {
            SCRIPT_RUNNER.runScript(scriptFile);
            MessageUtils.debugToChat("Running: " + scriptFile.getFileName());
            return 1;
        } catch (Exception e) {
            MessageUtils.errorToChat(e);
            return 0;
        }
    }

    public static int listHelperFunctions() {
        if (noPlayer()) return 0;
        List<String> names = SCRIPT_RUNNER.getLastHelperNames();
        if (names.isEmpty()) {
            MessageUtils.debugToChat("No helper functions were created in the last run.");
        } else {
            MessageUtils.debugToChat("Helper functions (" + names.size() + "):\n" + String.join(", ", names));
        }
        return 1;
    }

    public static int listRunWarnings() {
        if (noPlayer()) return 0;
        RunWarnings.replay();
        return 1;
    }

    public static int listExtraVariables() {
        if (noPlayer()) return 0;
        List<String> vars = SCRIPT_RUNNER.getLastExtraVariables();
        if (vars.isEmpty()) {
            MessageUtils.debugToChat("No extra compiler variables were created in the last run.");
        } else {
            MessageUtils.debugToChat("Extra variables (" + vars.size() + "):\n" + String.join(", ", vars));
        }
        return 1;
    }

    public static Path getScriptsDir() {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("neohouse-scripts");
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path marker = dir.resolve(".initialized");
            if (!Files.exists(marker)) Files.createFile(marker);
        } catch (IOException ignored) {
        }
        LibraryManager.init(dir);
        mergeOldMusicFolder(dir);
        return dir;
    }


    /** Scripts and music used to have separate folders; move the old music folder's files in, once. */
    private static void mergeOldMusicFolder(Path scriptsDir) {
        Path oldMusicDir = Minecraft.getInstance().gameDirectory.toPath().resolve(OLD_MUSIC_FOLDER);
        try {
            int moved = MusicFolderMerge.merge(oldMusicDir, scriptsDir);
            if (moved > 0) MessageUtils.debugToChat("Moved " + moved + " music file" + (moved == 1 ? "" : "s")
                    + " from " + OLD_MUSIC_FOLDER + " into neohouse-scripts (scripts and music now share one folder).");
        } catch (IOException e) {
            MessageUtils.errorToChat("Could not move your music from " + OLD_MUSIC_FOLDER + " into neohouse-scripts: " + e.getMessage());
        }
    }

    public static List<String> scriptSuggestions(String remaining) {
        List<String> out = new ArrayList<>();
        Path root = getScriptsDir();
        try (var walk = Files.walk(root)) {
            walk.filter(p -> p.toString().endsWith(".nh"))
                .map(p -> root.relativize(p).toString().replace('\\', '/'))
                .map(s -> s.endsWith(".nh") ? s.substring(0, s.length() - 3) : s)
                .filter(s -> s.startsWith(remaining))
                .forEach(out::add);
        } catch (IOException ignored) {}
        return out;
    }

    public static Path resolveScriptPath(String raw) {
        String s = raw.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1);
        }

        if (!s.contains(".")) s = s + ".nh";

        Path p = Paths.get(s);
        if (!p.isAbsolute()) p = getScriptsDir().resolve(p);

        String name = p.getFileName().toString().toLowerCase();
        if (!name.endsWith(".nh")) {
            MessageUtils.errorToChat("Unsupported extension '" + name + "' - only .nh files are supported.");
            return null;
        }
        if (!Files.exists(p)) {
            MessageUtils.errorToChat("Script not found: " + p);
            return null;
        }
        return p;
    }
}
