package com.drakou111.neohouse.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Wraps tooltip text to a width, starting a new line at every {@code \n} (a blank line stays blank).
 * A line describing a setting option ("Warn: reported.") gets the option's name in its color.
 */
public final class TooltipLines {
    private TooltipLines() {}

    public static List<FormattedCharSequence> wrap(Font font, String text, int maxWidth) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(font.split(colored(paragraph), maxWidth));
        }
        return lines;
    }

    private static Component colored(String paragraph) {
        int colon = paragraph.indexOf(':');
        if (colon <= 0) return Component.literal(paragraph);
        Optional<ChatFormatting> color = OptionColors.formattingFor(paragraph.substring(0, colon));
        if (color.isEmpty()) return Component.literal(paragraph);

        // Siblings of an empty parent, so the rest of the line doesn't inherit the option's color.
        MutableComponent line = Component.empty();
        line.append(Component.literal(paragraph.substring(0, colon)).withStyle(color.get()));
        return line.append(Component.literal(paragraph.substring(colon)));
    }
}
