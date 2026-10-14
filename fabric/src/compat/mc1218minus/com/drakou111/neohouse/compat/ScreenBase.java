package com.drakou111.neohouse.compat;

import com.drakou111.neohouse.ui.NeoHouseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class ScreenBase extends Screen implements NeoHouseScreen {
    protected ScreenBase(Component title) { super(title); }

    @Override public final boolean mouseClicked(double mx, double my, int btn) { return onMouseClicked(mx, my, btn); }
    @Override public final boolean mouseDragged(double mx, double my, int btn, double dx, double dy) { return onMouseDragged(mx, my, btn, dx, dy); }
    @Override public final boolean mouseReleased(double mx, double my, int btn) { return onMouseReleased(mx, my, btn); }
    @Override public final boolean keyPressed(int key, int scan, int mods) { return onKeyPressed(key, scan, mods); }

    protected boolean onMouseClicked(double mx, double my, int btn) { return superMouseClicked(mx, my, btn); }
    protected boolean onMouseDragged(double mx, double my, int btn, double dx, double dy) { return superMouseDragged(mx, my, btn, dx, dy); }
    protected boolean onMouseReleased(double mx, double my, int btn) { return superMouseReleased(mx, my, btn); }
    protected boolean onKeyPressed(int key, int scan, int mods) { return superKeyPressed(key, scan, mods); }

    protected final boolean superMouseClicked(double mx, double my, int btn) { return super.mouseClicked(mx, my, btn); }
    protected final boolean superMouseDragged(double mx, double my, int btn, double dx, double dy) { return super.mouseDragged(mx, my, btn, dx, dy); }
    protected final boolean superMouseReleased(double mx, double my, int btn) { return super.mouseReleased(mx, my, btn); }
    protected final boolean superKeyPressed(int key, int scan, int mods) { return super.keyPressed(key, scan, mods); }
}
