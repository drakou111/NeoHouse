package com.drakou111.neohouse.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TooltipHelper {
    private TooltipHelper() {}

    public static void render(GuiGraphics g, Font font, String text, int mouseX, int mouseY) {
        g.renderTooltip(font,
                List.of(ClientTooltipComponent.create(Component.literal(text).getVisualOrderText())),
                mouseX, mouseY,
                DefaultTooltipPositioner.INSTANCE,
                null);
    }
}
