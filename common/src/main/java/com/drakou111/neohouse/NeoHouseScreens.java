package com.drakou111.neohouse;

import com.drakou111.neohouse.ui.HomeScreen;
import com.drakou111.neohouse.ui.InputDialogScreen;
import com.drakou111.neohouse.ui.NeoHouseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Tracks the most recently open NeoHouse menu so the "Open NeoHouse" keybind can
 * either open the home page or restore whatever menu the player last closed.
 */
public final class NeoHouseScreens {
    private NeoHouseScreens() {}

    private static Screen lastScreen = null;

    /** Call every client tick: remember the current screen while it's a NeoHouse menu. */
    public static void track() {
        Screen current = Minecraft.getInstance().screen;
        // Skip transient input/confirm dialogs - they aren't a place worth restoring to.
        if (current instanceof NeoHouseScreen && !(current instanceof InputDialogScreen)) lastScreen = current;
    }

    /** Makes {@code screen} the one the keybind restores, e.g. after leaving another screen for good. */
    public static void remember(Screen screen) {
        lastScreen = screen;
    }

    /**
     * Open NeoHouse via the keybind. If a NeoHouse menu is already open, do nothing.
     * Otherwise restore the last menu the player had open, or the home page if none.
     */
    public static void openOrRestore() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen instanceof NeoHouseScreen) return;
        Screen target = lastScreen != null ? lastScreen : new HomeScreen(CommandActions::getScriptsDir);
        mc.setScreen(target);
    }
}
