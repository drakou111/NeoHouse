package com.drakou111.neohouse.compat;

import com.drakou111.neohouse.ui.NeoHouseScreen;
import net.minecraft.client.gui.GuiGraphics;
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
