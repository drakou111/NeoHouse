package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.compat.ClickEventHelper;
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
        sendPrefixedMultiline(message, "[NEOHOUSE] ", ChatFormatting.AQUA, ChatFormatting.GRAY);
    }

    public static void errorToChat(Object message) {
        sendPrefixedMultiline(message, "[ERROR] ", ChatFormatting.RED, ChatFormatting.RED);
    }

    public static void warningToChat(Object message) {
        sendPrefixedMultiline(message, "[WARNING] ", ChatFormatting.YELLOW, ChatFormatting.YELLOW);
    }

    public static void warningToChatWithLink(String message, String linkText, String command) {
        if (Minecraft.getInstance().player == null) return;
        MutableComponent full = Component.empty();
        full.append(Component.literal("[WARNING] ").withStyle(ChatFormatting.YELLOW));
        full.append(Component.literal(message).withStyle(ChatFormatting.YELLOW));
        full.append(Component.literal(" "));
        full.append(Component.literal("[" + linkText + "]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GOLD)
                        .withUnderlined(true)
                        .withClickEvent(ClickEventHelper.runCommand(command))));
        Minecraft.getInstance().player.displayClientMessage(full, false);
    }
}