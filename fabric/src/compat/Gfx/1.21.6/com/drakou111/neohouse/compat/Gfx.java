package com.drakou111.neohouse.compat;

import com.drakou111.neohouse.utils.TooltipLines;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The drawing calls NeoHouse's screens use, over this Minecraft version's GUI graphics class.
 * Screens draw through this wrapper so the same UI code compiles for every supported version.
 */
public final class Gfx {
    private static final int TOOLTIP_MAX_WIDTH = 240;

    private final GuiGraphics graphics;

    public Gfx(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    /** The wrapped Minecraft object, for the other compat helpers. */
    GuiGraphics graphics() {
        return this.graphics;
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        this.graphics.fill(x1, y1, x2, y2, color);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
        this.graphics.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);
    }

    public void drawString(Font font, String text, int x, int y, int color) {
        this.graphics.drawString(font, text, x, y, color);
    }

    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        this.graphics.drawString(font, text, x, y, color, shadow);
    }

    public void drawString(Font font, Component text, int x, int y, int color) {
        this.graphics.drawString(font, text, x, y, color);
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        this.graphics.drawString(font, text, x, y, color, shadow);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        this.graphics.drawCenteredString(font, text, x, y, color);
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        this.graphics.drawCenteredString(font, text, x, y, color);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        this.graphics.renderItem(stack, x, y);
    }

    /** Draws a widget or a whole screen (e.g. the one behind a dialog). */
    public void render(Renderable renderable, int mouseX, int mouseY, float partialTick) {
        renderable.render(this.graphics, mouseX, mouseY, partialTick);
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
        List<ClientTooltipComponent> lines = new ArrayList<>();
        for (FormattedCharSequence line : TooltipLines.wrap(font, text, TOOLTIP_MAX_WIDTH))
            lines.add(ClientTooltipComponent.create(line));
        this.graphics.renderTooltip(font, lines, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }
}
