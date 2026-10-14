package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.compat.Gfx;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.UITheme;
import com.drakou111.neohouse.platform.Platforms;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class ScriptOverlayRenderer {

    private static final int WINDOW = 40;
    private static final long[] stepTimes  = new long[WINDOW];
    private static int windowHead = 0;
    private static int windowSize = 0;

    private static int lastCompleted    = -1;
    private static int displayCompleted = 0;

    private static @Nullable String cachedMobId = null;
    private static @Nullable LivingEntity cachedMobEntity = null;

    private static final long  MOB_SPIN_PERIOD_MS = 3000L;
    private static final float MOB_SCALE_BASE     = 35f;
    private static final int MOB_SCALE_MIN      = 8;
    private static final int MOB_SCALE_MAX      = 55;
    private static final int MOB_BOX_GAP        = 2;
    // Real entity IDs count up from 1, so a negative one can't collide.
    private static final int PREVIEW_ENTITY_ID  = -1;

    public static void render(Gfx gfx, Minecraft mc, int screenW, int screenH) {
        if (!SequenceManager.isRunning()) {
            windowHead = 0;
            windowSize = 0;
            lastCompleted = -1;
            displayCompleted = 0;
            cachedMobId = null;
            cachedMobEntity  = null;
            return;
        }

        long now       = System.currentTimeMillis();
        int  completed = SequenceManager.getCompletedSteps();
        int  total     = SequenceManager.getTotalSteps();
        long elapsedMs = SequenceManager.getElapsedMs();

        if (lastCompleted < 0) lastCompleted = completed;
        while (lastCompleted < completed) {
            stepTimes[windowHead] = now;
            windowHead  = (windowHead + 1) % WINDOW;
            windowSize  = Math.min(windowSize + 1, WINDOW);
            lastCompleted++;
        }

        displayCompleted = Math.max(displayCompleted, completed);

        int safeTotal = Math.max(total, displayCompleted);

        String line2;
        if (windowSize >= 2 && safeTotal > displayCompleted) {
            int  oldestIdx   = (windowHead - windowSize + WINDOW) % WINDOW;
            long windowMs    = now - stepTimes[oldestIdx];
            if (windowMs > 0) {
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

        var font = mc.font;
        int padding = 5;
        int lineH = font.lineHeight + 2;
        int barH = 4;
        int textW = Math.max(font.width(line1), Math.max(font.width(line3), font.width(line4)));
        int boxW = textW + padding * 2;
        int boxH = lineH * 4 + barH + 4 + padding * 2;
        int x = Math.max(2, screenW - boxW - 5);
        int y = Math.max(2, screenH - boxH - 5);

        gfx.fill(x, y, x + boxW, y + boxH, 0xAA000000);

        int ty = y + padding;
        gfx.drawString(font, line1, x + padding, ty, 0xFFFFFFFF, false); ty += lineH;
        gfx.drawString(font, line2, x + padding, ty, 0xFFAAAAAA, false); ty += lineH;

        int barX = x + padding;
        int barW = boxW - padding * 2;
        UITheme theme = NeoHouseConfig.get().uiTheme;
        gfx.fill(barX, ty, barX + barW, ty + barH, 0xFF333333);
        int fill = safeTotal > 0 ? (int) Math.min(barW, (long) barW * displayCompleted / safeTotal) : 0;
        gfx.fill(barX, ty, barX + fill, ty + barH, theme.accent());
        ty += barH + 4;

        gfx.drawString(font, line3, x + padding, ty, 0xFF888888, false); ty += lineH;
        gfx.drawString(font, line4, x + padding, ty, 0xFF666666, false);

        renderScriptMob(gfx, mc, x + boxW / 2, y, now);
    }

    private static void renderScriptMob(Gfx gfx, Minecraft mc, int centerX, int boxTopY, long now) {
        NeoHouseConfig cfg = NeoHouseConfig.get();
        if (!cfg.showScriptMob || mc.level == null) return;

        String mobId = cfg.scriptMobId;
        if (!mobId.equals(cachedMobId)) {
            cachedMobEntity = RenderHelper.createEntity(mc, mobId);
            // 26.2+ throws on getId() while the ID is still 0, and this entity is never added to a world.
            if (cachedMobEntity != null) cachedMobEntity.setId(PREVIEW_ENTITY_ID);
            cachedMobId = mobId;
        }
        if (cachedMobEntity == null) return;

        float angle = (now % MOB_SPIN_PERIOD_MS) / (float) MOB_SPIN_PERIOD_MS * 360f;
        float bbMax = Math.max(cachedMobEntity.getBbHeight(), cachedMobEntity.getBbWidth());
        int scale = (int) Math.max(MOB_SCALE_MIN, Math.min(MOB_SCALE_MAX, MOB_SCALE_BASE / Math.max(0.3f, bbMax)));

        int feetY = boxTopY - MOB_BOX_GAP;
        RenderHelper.renderSpinningEntity(gfx, mc, cachedMobEntity, centerX, feetY, scale, angle);
    }

    private static String formatTime(long ms) {
        long s = ms / 1000;
        if (s < 60) return String.format("%02ds", s);
        return String.format("%02dmin %02ds", s / 60, s % 60);
    }
}
