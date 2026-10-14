package com.drakou111.neohouse.compat;

import com.drakou111.neohouse.ui.NeoHouseScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class ScreenBase extends Screen implements NeoHouseScreen {
    protected ScreenBase(Component title) { super(title); }

    private MouseButtonEvent _me; private boolean _mc;
    private MouseButtonEvent _dme; private double _ddx, _ddy;
    private MouseButtonEvent _rme;
    private KeyEvent _ke;

    @Override
    public final boolean mouseClicked(MouseButtonEvent e, boolean consumed) {
        _me = e; _mc = consumed;
        boolean r = onMouseClicked(e.x(), e.y(), e.button());
        _me = null;
        return r;
    }

    @Override
    public final boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        _dme = e; _ddx = dx; _ddy = dy;
        boolean r = onMouseDragged(e.x(), e.y(), e.button(), dx, dy);
        _dme = null;
        return r;
    }

    @Override
    public final boolean mouseReleased(MouseButtonEvent e) {
        _rme = e;
        boolean r = onMouseReleased(e.x(), e.y(), e.button());
        _rme = null;
        return r;
    }

    @Override
    public final boolean keyPressed(KeyEvent e) {
        _ke = e;
        boolean r = onKeyPressed(e.key(), e.scancode(), e.modifiers());
        _ke = null;
        return r;
    }

    protected boolean onMouseClicked(double mx, double my, int btn) { return superMouseClicked(mx, my, btn); }
    protected boolean onMouseDragged(double mx, double my, int btn, double dx, double dy) { return superMouseDragged(mx, my, btn, dx, dy); }
    protected boolean onMouseReleased(double mx, double my, int btn) { return superMouseReleased(mx, my, btn); }
    protected boolean onKeyPressed(int key, int scan, int mods) { return superKeyPressed(key, scan, mods); }

    protected final boolean superMouseClicked(double mx, double my, int btn) {
        return _me != null && super.mouseClicked(_me, _mc);
    }
    protected final boolean superMouseDragged(double mx, double my, int btn, double dx, double dy) {
        return _dme != null && super.mouseDragged(_dme, _ddx, _ddy);
    }
    protected final boolean superMouseReleased(double mx, double my, int btn) {
        return _rme != null && super.mouseReleased(_rme);
    }
    protected final boolean superKeyPressed(int key, int scan, int mods) {
        return _ke != null && super.keyPressed(_ke);
    }

    // Rendering goes through Gfx so the screens are written once for every Minecraft version.
    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderScreen(new Gfx(graphics), mouseX, mouseY, partialTick);
    }

    @Override
    public final void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderScreenBackground(new Gfx(graphics), mouseX, mouseY, partialTick);
    }

    /** Draws the screen; call {@link #renderWidgets} to draw its buttons and other widgets. */
    protected void renderScreen(Gfx g, int mouseX, int mouseY, float partialTick) {
        renderWidgets(g, mouseX, mouseY, partialTick);
    }

    /** Draws behind everything else; nothing by default (no vanilla blur). */
    protected void renderScreenBackground(Gfx g, int mouseX, int mouseY, float partialTick) {}

    protected final void renderWidgets(Gfx g, int mouseX, int mouseY, float partialTick) {
        super.render(g.graphics(), mouseX, mouseY, partialTick);
    }
}
