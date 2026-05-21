package com.drakou111.neohouse.compat;

import com.drakou111.neohouse.platform.Platforms;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public final class RenderHelper {
    private RenderHelper() {}

    public static void renderOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.renderOutline(x, y, w, h, color);
    }

    public static void renderLogo(GuiGraphics g, int x, int y, int size, int argbColor) {
        Platforms.get().renderLogo(g, x, y, size, argbColor);
    }

    public static boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }
}
