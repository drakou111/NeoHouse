package com.drakou111.neohouse.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Shows a message in the player's own chat, without sending anything to the server (26.x API). */
public final class ClientMessages {
    private ClientMessages() {}

    public static void show(Component message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(message);
    }
}
