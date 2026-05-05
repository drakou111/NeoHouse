package com.drakou111.neohouse.compat;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class WidgetBase extends AbstractWidget {
    protected WidgetBase(int x, int y, int w, int h, Component msg) { super(x, y, w, h, msg); }

    private MouseButtonEvent _rme;

    @Override public final boolean keyPressed(KeyEvent e) { return onKeyPressed(e.key(), e.scancode(), e.modifiers()); }
    @Override public final boolean charTyped(CharacterEvent e) { return onCharTyped((char) e.codepoint(), e.modifiers()); }
    @Override public final boolean mouseClicked(MouseButtonEvent e, boolean consumed) { return onMouseClicked(e.x(), e.y(), e.button()); }
    @Override public final boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { return onMouseDragged(e.x(), e.y(), e.button(), dx, dy); }
    @Override
    public final boolean mouseReleased(MouseButtonEvent e) {
        _rme = e;
        boolean r = onMouseReleased(e.x(), e.y(), e.button());
        _rme = null;
        return r;
    }

    protected boolean onKeyPressed(int key, int scan, int mods) { return false; }
    protected boolean onCharTyped(char c, int mods) { return false; }
    protected boolean onMouseClicked(double mx, double my, int btn) { return false; }
    protected boolean onMouseDragged(double mx, double my, int btn, double dx, double dy) { return false; }
    protected boolean onMouseReleased(double mx, double my, int btn) { return superMouseReleased(mx, my, btn); }

    protected final boolean superMouseReleased(double mx, double my, int btn) {
        return _rme != null && super.mouseReleased(_rme);
    }
}
