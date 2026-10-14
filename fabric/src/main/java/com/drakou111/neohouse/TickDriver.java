package com.drakou111.neohouse;

import com.drakou111.neohouse.compat.ClientUi;
import com.drakou111.neohouse.compat.Keys;
import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.SequenceManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

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
            ClientUi.setScreen(client, pendingScreen);
            pendingScreen = null;
        }

        int cancelKey = CancelKeybind.CANCEL_KEY.getKeyCode();
        boolean cancelKeyDown = Keys.isDown(cancelKey);
        if (cancelKeyDown && !cancelKeyWasDown && SequenceManager.isRunning()) {
            SequenceManager.cancelAction();
            MessageUtils.debugToChat("Script cancelled.");
        }
        cancelKeyWasDown = cancelKeyDown;

        // Open key only fires in-game (no screen open) so it never clashes with typing.
        int openKey = CancelKeybind.OPEN_KEY.getKeyCode();
        boolean openKeyDown = Keys.isDown(openKey);
        if (openKeyDown && !openKeyWasDown && ClientUi.screen(client) == null) {
            NeoHouseScreens.openOrRestore();
        }
        openKeyWasDown = openKeyDown;

        NeoHouseScreens.track();
        SequenceManager.tick(client);
    }
}
