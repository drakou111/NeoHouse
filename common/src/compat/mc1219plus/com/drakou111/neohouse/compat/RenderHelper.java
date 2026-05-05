package com.drakou111.neohouse.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class RenderHelper {
    private RenderHelper() {}

    public static void renderOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.submitOutline(x, y, w, h, color);
    }

    public static boolean hasShiftDown() {
        return Minecraft.getInstance().hasShiftDown();
    }
}
