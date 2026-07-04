package com.drakou111.neohouse.compat;

import net.minecraft.network.chat.ClickEvent;

public final class ClickEventHelper {
    private ClickEventHelper() {}

    public static ClickEvent runCommand(String command) {
        return new ClickEvent.RunCommand(command);
    }
}
