package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.compat.ClickEventHelper;
import com.drakou111.neohouse.compat.ClientMessages;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.exceptions.CompilingException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class MessageUtils {

    public static void rawLogToChat(String message) {
        if (Minecraft.getInstance().player != null) {
            ClientMessages.show(Component.literal(message));
        }
    }

    private static final String ERROR_PREFIX = "[ERROR] ";
    private static final String WARNING_PREFIX = "[WARNING] ";
    private static final String TAB_AS_SPACES = "    ";

    /** Sends each line as its own chat message: a newline inside one component renders as a glyph. */
    private static void sendPrefixedMultiline(Object message, String prefixText,
                                              ChatFormatting prefixColor,
                                              ChatFormatting bodyColor) {
        if (Minecraft.getInstance().player == null) return;
        for (String line : printable(String.valueOf(message)).split("\n")) {
            MutableComponent full = Component.empty()
                    .append(Component.literal(prefixText).withStyle(prefixColor))
                    .append(Component.literal(line).withStyle(bodyColor));
            ClientMessages.show(full);
        }
    }

    /**
     * Shows a compiler diagnostic as a location header plus the source line with the offending token
     * highlighted in place. Chat uses a proportional font, so a separate caret line could never line up.
     */
    private static void sendDiagnostic(Token token, String detail, String prefixText, ChatFormatting color) {
        if (Minecraft.getInstance().player == null) return;
        if (token == null) {
            sendPrefixedMultiline(detail, prefixText, color, color);
            return;
        }
        sendPrefixedMultiline(CompilingException.location(token) + ": " + detail, prefixText, color, color);

        String line = token.lineContent == null ? "" : token.lineContent.replace("\r", "");
        if (line.isBlank()) return;
        int start = Math.max(0, Math.min(token.column - 1, line.length()));
        int end = Math.min(line.length(), start + Math.max(1, token.text.length()));
        MutableComponent excerpt = Component.empty()
                .append(Component.literal(prefixText).withStyle(color))
                .append(Component.literal(printable(line.substring(0, start)).stripLeading()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(printable(line.substring(start, end))).withStyle(style -> style.withColor(color).withUnderlined(true)))
                .append(Component.literal(printable(line.substring(end))).withStyle(ChatFormatting.GRAY));
        ClientMessages.show(excerpt);
    }

    private static String printable(String text) {
        return text.replace("\r", "").replace("\t", TAB_AS_SPACES);
    }

    public static void debugToChat(Object message) {
        sendPrefixedMultiline(message, "[NEOHOUSE] ", ChatFormatting.AQUA, ChatFormatting.GRAY);
    }

    public static void errorToChat(Object message) {
        sendPrefixedMultiline(message, ERROR_PREFIX, ChatFormatting.RED, ChatFormatting.RED);
    }

    /** Reports a failure: compiler errors get their source excerpt, anything else just its message. */
    public static void errorToChat(Throwable error) {
        if (error instanceof CompilingException compileError) {
            sendDiagnostic(compileError.errorToken, compileError.detail, ERROR_PREFIX, ChatFormatting.RED);
            return;
        }
        errorToChat((Object) (error.getMessage() != null ? error.getMessage() : error.toString()));
    }

    public static void warningToChat(Object message) {
        sendPrefixedMultiline(message, WARNING_PREFIX, ChatFormatting.YELLOW, ChatFormatting.YELLOW);
    }

    public static void warningToChat(CompileWarning warning) {
        sendDiagnostic(warning.token(), warning.message(), WARNING_PREFIX, ChatFormatting.YELLOW);
    }

    public static void warningToChatWithLink(String message, String linkText, String command) {
        if (Minecraft.getInstance().player == null) return;
        MutableComponent full = Component.empty();
        full.append(Component.literal(WARNING_PREFIX).withStyle(ChatFormatting.YELLOW));
        full.append(Component.literal(message).withStyle(ChatFormatting.YELLOW));
        full.append(Component.literal(" "));
        full.append(Component.literal("[" + linkText + "]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GOLD)
                        .withUnderlined(true)
                        .withClickEvent(ClickEventHelper.runCommand(command))));
        ClientMessages.show(full);
    }
}