package com.drakou111.neohouse;

import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = NHConstants.MOD_ID, value = Dist.CLIENT)
public class TickDriver {

    private static Screen pendingScreen = null;

    /**
     * Opens a screen on the next client tick. Opening directly from a command
     * executor doesn't work: the chat screen is closed (set to null) AFTER the
     * command runs, clobbering the screen we just set. Deferring one tick avoids it.
     */
    public static void openScreenNextTick(Screen screen) {
        pendingScreen = screen;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (pendingScreen != null) {
            Minecraft.getInstance().setScreen(pendingScreen);
            pendingScreen = null;
        }
        NeoHouseScreens.track();
        SequenceManager.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        if (KeyCompatHelper.matches(CancelKeybind.OPEN_KEY, event)) {
            NeoHouseScreens.openOrRestore();
            return;
        }
        if (!KeyCompatHelper.matches(CancelKeybind.CANCEL_KEY, event)) return;
        if (!SequenceManager.isRunning()) return;
        SequenceManager.cancelAction();
        MessageUtils.debugToChat("Script cancelled.");
    }
}
