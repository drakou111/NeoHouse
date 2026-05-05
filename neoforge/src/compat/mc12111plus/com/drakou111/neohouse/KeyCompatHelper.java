package com.drakou111.neohouse;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.InputEvent;

public final class KeyCompatHelper {
    private KeyCompatHelper() {}

    public static boolean matches(KeyMapping km, InputEvent.Key event) {
        return km.matches(event.getKeyEvent());
    }
}
