package com.drakou111.neohouse.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class TooltipHelper {
    private TooltipHelper() {}

    private static final int MAX_WIDTH = 240;

    public static void render(GuiGraphics g, Font font, String text, int mouseX, int mouseY) {
        // Wrap long text so it does not overflow off the right edge of the screen.
        List<ClientTooltipComponent> lines = new ArrayList<>();
        for (FormattedCharSequence line : font.split(Component.literal(text), MAX_WIDTH))
            lines.add(ClientTooltipComponent.create(line));
        g.renderTooltip(font, lines, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }
}
