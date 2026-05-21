package com.drakou111.neohouse;

import com.drakou111.neohouse.compat.KeyMappingCompat;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class CancelKeybind {

    public static final KeyMapping CANCEL_KEY = new KeyMappingCompat(
            "key.neohouse.cancel",
            GLFW.GLFW_KEY_K
    );

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((RegisterKeyMappingsEvent e) -> e.register(CANCEL_KEY));
    }
}
