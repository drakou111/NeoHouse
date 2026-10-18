package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compat.Gfx;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Draws code text with the formatting of Minecraft's & codes. Every style keeps each character's
 * plain width, so columns and the cursor line up: bold is drawn twice, one pixel apart, instead of
 * with Minecraft's wider bold glyphs.
 */
final class StyledText {
    static final int BOLD = 1;
    static final int STRIKETHROUGH = 2;
    static final int UNDERLINE = 4;
    static final int ITALIC = 8;

    private StyledText() {
    }

    /** The style flag a formatting code character sets, or 0 for anything else (including &k). */
    static int flagFor(char code) {
        return switch (Character.toLowerCase(code)) {
            case 'l' -> BOLD;
            case 'm' -> STRIKETHROUGH;
            case 'n' -> UNDERLINE;
            case 'o' -> ITALIC;
            default -> 0;
        };
    }

    /** Draws {@code text} and returns its plain width. */
    static int draw(Gfx g, Font font, String text, int x, int y, int color, int style) {
        if (style == 0) {
            g.drawString(font, text, x, y, color, false);
            return font.width(text);
        }

        MutableComponent styled = Component.literal(text);
        if ((style & STRIKETHROUGH) != 0) styled = styled.withStyle(ChatFormatting.STRIKETHROUGH);
        if ((style & UNDERLINE) != 0) styled = styled.withStyle(ChatFormatting.UNDERLINE);
        if ((style & ITALIC) != 0) styled = styled.withStyle(ChatFormatting.ITALIC);
        g.drawString(font, styled, x, y, color, false);
        if ((style & BOLD) != 0) g.drawString(font, styled, x + 1, y, color, false);
        return font.width(text);
    }
}
