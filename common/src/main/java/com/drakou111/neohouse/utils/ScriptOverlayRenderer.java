package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.NeoHouseConfig;
import com.drakou111.neohouse.UITheme;
import com.drakou111.neohouse.platform.Platforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ScriptOverlayRenderer {

    // Rolling window: timestamps (ms) of the last WINDOW step completions.
    private static final int WINDOW = 40;
    private static final long[] stepTimes  = new long[WINDOW];
    private static int           windowHead = 0;  // next write index
    private static int           windowSize = 0;  // how many entries are valid

    // Monotonic display: never let the fill bar shrink, even if totalSteps grows.
    private static int  lastCompleted    = -1;
    private static int  displayCompleted = 0;

    public static void render(GuiGraphics gfx, Minecraft mc, int screenW, int screenH) {
        if (!SequenceManager.isRunning()) {
            windowHead       = 0;
            windowSize       = 0;
            lastCompleted    = -1;
            displayCompleted = 0;
            return;
        }

        long now       = System.currentTimeMillis();
        int  completed = SequenceManager.getCompletedSteps();
        int  total     = SequenceManager.getTotalSteps();
        long elapsedMs = SequenceManager.getElapsedMs();

        // Record a timestamp for each newly completed step since the last frame.
        if (lastCompleted < 0) lastCompleted = completed;
        while (lastCompleted < completed) {
            stepTimes[windowHead] = now;
            windowHead  = (windowHead + 1) % WINDOW;
            windowSize  = Math.min(windowSize + 1, WINDOW);
            lastCompleted++;
        }

        // Advance the monotonic counter — never go backward.
        displayCompleted = Math.max(displayCompleted, completed);

        // Guard against totalSteps being stale/lower than actual completed.
        int safeTotal = Math.max(total, displayCompleted);

        // ETA from rolling window: use the span across the entries we have.
        String line2;
        if (windowSize >= 2 && safeTotal > displayCompleted) {
            int  oldestIdx   = (windowHead - windowSize + WINDOW) % WINDOW;
            long windowMs    = now - stepTimes[oldestIdx];
            if (windowMs > 0) {
                // windowSize timestamps → windowSize-1 inter-step intervals
                long msPerStep   = windowMs / (windowSize - 1);
                long remainingMs = msPerStep * (safeTotal - displayCompleted);
                line2 = "~" + formatTime(remainingMs) + " remaining";
            } else {
                line2 = formatTime(elapsedMs) + " elapsed";
            }
        } else {
            line2 = formatTime(elapsedMs) + " elapsed";
        }

        String line1 = "NeoHouse - Running...";
        String line3 = displayCompleted + " / " + safeTotal + " steps";
        String line4 = "Press [" + Platforms.get().getCancelKeyName() + "] to stop";

        var font    = mc.font;
        int padding = 5;
        int lineH   = font.lineHeight + 2;
        int barH    = 4;
        // Exclude the dynamic ETA line from width so the box never resizes.
        int textW   = Math.max(font.width(line1), Math.max(font.width(line3), font.width(line4)));
        int boxW    = textW + padding * 2;
        int boxH    = lineH * 4 + barH + 4 + padding * 2;
        int x       = Math.max(2, screenW - boxW - 5);
        int y       = Math.max(2, screenH - boxH - 5);

        gfx.fill(x, y, x + boxW, y + boxH, 0xAA000000);

        int ty = y + padding;
        gfx.drawString(font, line1, x + padding, ty, 0xFFFFFFFF, false); ty += lineH;
        gfx.drawString(font, line2, x + padding, ty, 0xFFAAAAAA, false); ty += lineH;

        // Progress bar — fill is computed from the monotonic counter.
        int barX = x + padding;
        int barW = boxW - padding * 2;
        UITheme theme = UITheme.byId(NeoHouseConfig.get().uiTheme);
        gfx.fill(barX, ty, barX + barW, ty + barH, 0xFF333333);
        int fill = safeTotal > 0 ? (int) Math.min(barW, (long) barW * displayCompleted / safeTotal) : 0;
        gfx.fill(barX, ty, barX + fill, ty + barH, theme.accent());
        ty += barH + 4;

        gfx.drawString(font, line3, x + padding, ty, 0xFF888888, false); ty += lineH;
        gfx.drawString(font, line4, x + padding, ty, 0xFF666666, false);
    }

    private static String formatTime(long ms) {
        long s = ms / 1000;
        if (s < 60) return String.format("%02ds", s);
        return String.format("%02dmin %02ds", s / 60, s % 60);
    }
}
