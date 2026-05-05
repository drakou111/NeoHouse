package com.drakou111.neohouse;

import com.drakou111.neohouse.actions.ScreenEvents;
import com.drakou111.neohouse.utils.SequenceManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class TickDriver {

    private static Screen previousScreen = null;
    private static Screen pendingScreen = null;

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

        Screen current = client.screen;
        if (current != previousScreen) {
            if (previousScreen != null) ScreenEvents.onScreenClosed(previousScreen);
            if (current != null) ScreenEvents.onScreenOpened(current);
            previousScreen = current;
        }
        SequenceManager.tick(client);
    }
}
