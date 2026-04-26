package com.drakou111.neohouse;

import com.drakou111.neohouse.ui.FileBrowserScreen;
import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = NeoHouse.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class CancelKeybind {

    private static final String CATEGORY = "key.categories.neohouse";

    public static final KeyMapping CANCEL_KEY = new KeyMapping(
            "key.neohouse.cancel",
            GLFW.GLFW_KEY_K,
            CATEGORY
    );

    public static final KeyMapping FILES_KEY = new KeyMapping(
            "key.neohouse.files",
            GLFW.GLFW_KEY_O,
            CATEGORY
    );

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CANCEL_KEY);
        event.register(FILES_KEY);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft mc = Minecraft.getInstance();

        if (CANCEL_KEY.matches(event.getKey(), event.getScanCode())) {
            SequenceManager.cancelAction();
            MessageUtils.debugToChat("Cancelled.");
        }

        if (FILES_KEY.matches(event.getKey(), event.getScanCode())) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new FileBrowserScreen(ClientCommandHandler.getScriptsDir()));
            }
        }
    }
}
