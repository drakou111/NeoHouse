package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;

public class UnderWaterHint implements HintProvider {
    private final String text;

    public UnderWaterHint(String text) {
        this.text = text;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        if (mc.player == null) return false;
        return mc.player.isUnderWater() && !mc.player.isCreative();
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
