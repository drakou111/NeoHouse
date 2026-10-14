package com.drakou111.neohouse.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The drawing calls NeoHouse's screens use, over this Minecraft version's GUI graphics class.
 * Screens draw through this wrapper so the same UI code compiles for every supported version.
 */
public final class Gfx {
    private static final int TOOLTIP_MAX_WIDTH = 240;

    private final GuiGraphicsExtractor graphics;

    public Gfx(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    /** The wrapped Minecraft object, for the other compat helpers. */
    GuiGraphicsExtractor graphics() {
        return this.graphics;
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        this.graphics.fill(x1, y1, x2, y2, color);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
        this.graphics.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);
    }

    public void drawString(Font font, String text, int x, int y, int color) {
        this.graphics.text(font, text, x, y, color);
    }

    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        this.graphics.text(font, text, x, y, color, shadow);
    }

    public void drawString(Font font, Component text, int x, int y, int color) {
        this.graphics.text(font, text, x, y, color);
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        this.graphics.text(font, text, x, y, color, shadow);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        this.graphics.centeredText(font, text, x, y, color);
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        this.graphics.centeredText(font, text, x, y, color);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        this.graphics.item(stack, x, y);
    }

    /** Draws a widget or a whole screen (e.g. the one behind a dialog). */
    public void render(Renderable renderable, int mouseX, int mouseY, float partialTick) {
        renderable.extractRenderState(this.graphics, mouseX, mouseY, partialTick);
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        this.graphics.enableScissor(x1, y1, x2, y2);
    }

    public void disableScissor() {
        this.graphics.disableScissor();
    }

    public void pushPose() {
        this.graphics.pose().pushMatrix();
    }

    public void popPose() {
        this.graphics.pose().popMatrix();
    }

    public void scale(float x, float y) {
        this.graphics.pose().scale(x, y);
    }

    public void translate(float x, float y) {
        this.graphics.pose().translate(x, y);
    }

    /** A tooltip for {@code text} at the mouse, wrapped so it doesn't run off the screen. */
    public void tooltip(Font font, String text, int mouseX, int mouseY) {
        this.graphics.setTooltipForNextFrame(font.split(Component.literal(text), TOOLTIP_MAX_WIDTH), mouseX, mouseY);
    }
}
