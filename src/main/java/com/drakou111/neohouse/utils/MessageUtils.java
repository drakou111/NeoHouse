package com.drakou111.neohouse.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class MessageUtils {

    public static void rawLogToChat(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal(message), false);
        }
    }

    private static void sendPrefixedMultiline(Object message, String prefixText,
                                              ChatFormatting prefixColor,
                                              ChatFormatting bodyColor) {
        if (Minecraft.getInstance().player == null) return;
        String text = String.valueOf(message).replace("\t", "    ");
        String[] lines = text.split("\n");
        MutableComponent full = Component.empty();
        for (int i = 0; i < lines.length; i++) {
            Component prefix = Component.literal(prefixText).withStyle(prefixColor);
            Component body = Component.literal(lines[i]).withStyle(bodyColor);
            full.append(prefix).append(body);
            if (i < lines.length - 1) {
                full.append(Component.literal("\n"));
            }
        }
        Minecraft.getInstance().player.displayClientMessage(full, false);
    }

    public static void debugToChat(Object message) {
        sendPrefixedMultiline(message, "[NEO_HOUSE] ", ChatFormatting.AQUA, ChatFormatting.GRAY);
    }

    public static void errorToChat(Object message) {
        sendPrefixedMultiline(message, "[ERROR] ", ChatFormatting.RED, ChatFormatting.RED);
    }

    public static void warningToChat(Object message) {
        sendPrefixedMultiline(message, "[WARNING] ", ChatFormatting.YELLOW, ChatFormatting.YELLOW);
    }
}