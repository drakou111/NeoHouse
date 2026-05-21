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

        int boundKey = CancelKeybind.CANCEL_KEY.getKeyCode();
        long windowHandle = GLFW.glfwGetCurrentContext();
        boolean cancelKeyDown = windowHandle != 0L && boundKey >= 0
                && GLFW.glfwGetKey(windowHandle, boundKey) == GLFW.GLFW_PRESS;
        if (cancelKeyDown && !cancelKeyWasDown && SequenceManager.isRunning()) {
            SequenceManager.cancelAction();
            MessageUtils.debugToChat("Script cancelled.");
        }
        cancelKeyWasDown = cancelKeyDown;
        SequenceManager.tick(client);
    }
}
