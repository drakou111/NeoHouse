package com.drakou111.neohouse;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class MessageUtils {
    public static void rawLogToChat(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal(message), false);
        }
    }
    public static void debugToChat(String message) {
        if (Minecraft.getInstance().player != null) {
            Component prefix = Component.literal("[NEO_HOUSE] ").withStyle(ChatFormatting.AQUA);
            Component body = Component.literal(message).withStyle(ChatFormatting.GRAY);
            Component full = Component.empty().append(prefix).append(body);
            Minecraft.getInstance().player.displayClientMessage(full, false);
        }
    }
}
