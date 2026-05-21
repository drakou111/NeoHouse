package com.drakou111.neohouse;

import com.drakou111.neohouse.compat.KeyMappingCompat;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.lwjgl.glfw.GLFW;

public class CancelKeybind {

    public static final KeyMappingCompat CANCEL_KEY = (KeyMappingCompat) KeyBindingHelper.registerKeyBinding(
            new KeyMappingCompat("key.neohouse.cancel", GLFW.GLFW_KEY_K)
    );

    public static void register() {
        // referenced to trigger class loading and registration
    }
}
