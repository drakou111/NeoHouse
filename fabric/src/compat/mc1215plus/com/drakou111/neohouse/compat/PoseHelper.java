package com.drakou111.neohouse.compat;

import net.minecraft.client.gui.GuiGraphics;

// 1.21.6+: g.pose() returns Matrix3x2fStack; push/pop/scale/translate are 2D only.
public final class PoseHelper {
    private PoseHelper() {}

    public static void push(GuiGraphics g) { g.pose().pushMatrix(); }
    public static void pop(GuiGraphics g)  { g.pose().popMatrix(); }

    public static void scale(GuiGraphics g, float x, float y) {
        g.pose().scale(x, y);
    }

    public static void translate(GuiGraphics g, float x, float y) {
        g.pose().translate(x, y);
    }
}
