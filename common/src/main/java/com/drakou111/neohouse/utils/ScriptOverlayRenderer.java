package com.drakou111.neohouse.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ScriptOverlayRenderer {

    public static void render(GuiGraphics gfx, Minecraft mc, int screenW, int screenH) {
        if (!SequenceManager.isRunning()) return;

        long elapsedMs   = SequenceManager.getElapsedMs();
        int  completed   = SequenceManager.getCompletedSteps();
        int  total       = SequenceManager.getTotalSteps();

        String line1 = "Importing script...";
        String line2;
        if (completed > 2 && elapsedMs > 1500 && total > 0) {
            long estimatedTotalMs = elapsedMs * total / completed;
            long remainingMs = Math.max(0, estimatedTotalMs - elapsedMs);
            line2 = "~" + formatTime(remainingMs) + " remaining";
        } else {
            line2 = formatTime(elapsedMs) + " elapsed";
        }

        var font    = mc.font;
        int padding = 5;
        int lineH   = font.lineHeight + 2;
        int textW   = Math.max(font.width(line1), font.width(line2));
        int boxW    = textW + padding * 2;
        int boxH    = lineH * 2 + padding * 2;
        int x       = screenW - boxW - 5;
        int y       = screenH - boxH - 5;

        gfx.fill(x, y, x + boxW, y + boxH, 0xAA000000);
        gfx.drawString(font, line1, x + padding, y + padding,          0xFFFFFFFF, false);
        gfx.drawString(font, line2, x + padding, y + padding + lineH,  0xFFAAAAAA, false);
    }

    private static String formatTime(long ms) {
        long s = ms / 1000;
        if (s < 60) return s + "s";
        return (s / 60) + "m " + (s % 60) + "s";
    }
}
