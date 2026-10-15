package com.drakou111.neohouse.utils;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Wraps tooltip text to a width, starting a new line at every {@code \n} (a blank line stays blank). */
public final class TooltipLines {
    private TooltipLines() {}

    public static List<FormattedCharSequence> wrap(Font font, String text, int maxWidth) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(font.split(Component.literal(paragraph), maxWidth));
        }
        return lines;
    }
}
