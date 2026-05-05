package com.drakou111.neohouse.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TooltipHelper {
    private TooltipHelper() {}

    public static void render(GuiGraphics g, Font font, String text, int mouseX, int mouseY) {
        g.renderTooltip(font, Component.literal(text), mouseX, mouseY);
    }
}
