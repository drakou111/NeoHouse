package com.drakou111.neohouse;

import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.SequenceManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public class TickDriver {

    private static Screen pendingScreen = null;
    private static boolean cancelKeyWasDown = false;
    private static boolean openKeyWasDown = false;

    public static void openScreenNextTick(Screen screen) {
        pendingScreen = screen;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TickDriver::onTick);
    }

    private static void onTick(Minecraft client) {
        if (pendingScreen != null) {
            client.setScreen(pendingScreen);
            pendingScreen = null;
        }

        long windowHandle = GLFW.glfwGetCurrentContext();

        int cancelKey = CancelKeybind.CANCEL_KEY.getKeyCode();
        boolean cancelKeyDown = windowHandle != 0L && cancelKey >= 0
                && GLFW.glfwGetKey(windowHandle, cancelKey) == GLFW.GLFW_PRESS;
        if (cancelKeyDown && !cancelKeyWasDown && SequenceManager.isRunning()) {
            SequenceManager.cancelAction();
            MessageUtils.debugToChat("Script cancelled.");
        }
        cancelKeyWasDown = cancelKeyDown;

        // Open key only fires in-game (no screen open) so it never clashes with typing.
        int openKey = CancelKeybind.OPEN_KEY.getKeyCode();
        boolean openKeyDown = windowHandle != 0L && openKey >= 0
                && GLFW.glfwGetKey(windowHandle, openKey) == GLFW.GLFW_PRESS;
        if (openKeyDown && !openKeyWasDown && client.screen == null) {
            NeoHouseScreens.openOrRestore();
        }
        openKeyWasDown = openKeyDown;

        NeoHouseScreens.track();
        SequenceManager.tick(client);
    }
}
