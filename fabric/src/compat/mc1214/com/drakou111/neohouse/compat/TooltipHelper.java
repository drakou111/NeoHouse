package com.drakou111.neohouse.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TooltipHelper {
    private TooltipHelper() {}

    private static final int MAX_WIDTH = 240;

    public static void render(GuiGraphics g, Font font, String text, int mouseX, int mouseY) {
        // Wrap long text so it does not overflow off the right edge of the screen.
        g.renderTooltip(font, font.split(Component.literal(text), MAX_WIDTH), mouseX, mouseY);
    }
}
