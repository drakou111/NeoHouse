package com.drakou111.neohouse.compat;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public abstract class WidgetBase extends AbstractWidget {
    protected WidgetBase(int x, int y, int w, int h, Component msg) { super(x, y, w, h, msg); }

    @Override public final boolean keyPressed(int key, int scan, int mods) { return onKeyPressed(key, scan, mods); }
    @Override public final boolean charTyped(char c, int mods) { return onCharTyped(c, mods); }
    @Override public final boolean mouseClicked(double mx, double my, int btn) { return onMouseClicked(mx, my, btn); }
    @Override public final boolean mouseDragged(double mx, double my, int btn, double dx, double dy) { return onMouseDragged(mx, my, btn, dx, dy); }
    @Override public final boolean mouseReleased(double mx, double my, int btn) { return onMouseReleased(mx, my, btn); }

    protected boolean onKeyPressed(int key, int scan, int mods) { return false; }
    protected boolean onCharTyped(char c, int mods) { return false; }
    protected boolean onMouseClicked(double mx, double my, int btn) { return false; }
    protected boolean onMouseDragged(double mx, double my, int btn, double dx, double dy) { return false; }
    protected boolean onMouseReleased(double mx, double my, int btn) { return superMouseReleased(mx, my, btn); }

    protected final boolean superMouseReleased(double mx, double my, int btn) { return super.mouseReleased(mx, my, btn); }
}
