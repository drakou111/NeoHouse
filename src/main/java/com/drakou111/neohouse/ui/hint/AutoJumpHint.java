package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;

import java.time.LocalTime;

public class AutoJumpHint implements HintProvider {
    private final String text;

    public AutoJumpHint(String text) {
        this.text = text;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        if (mc.player == null) return false;
        return mc.player.isAutoJumpEnabled();
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
