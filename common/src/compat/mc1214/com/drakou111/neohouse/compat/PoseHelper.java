package com.drakou111.neohouse.compat;

import net.minecraft.client.gui.GuiGraphics;

public final class PoseHelper {
    private PoseHelper() {}

    public static void push(GuiGraphics g) { g.pose().pushPose(); }
    public static void pop(GuiGraphics g)  { g.pose().popPose(); }

    public static void scale(GuiGraphics g, float x, float y) {
        g.pose().scale(x, y, 1f);
    }

    public static void translate(GuiGraphics g, float x, float y) {
        g.pose().translate(x, y, 0);
    }
}
