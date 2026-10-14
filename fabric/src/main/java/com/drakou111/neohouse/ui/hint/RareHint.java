package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;

public class RareHint implements HintProvider {
    private final String text;
    private final double chance;

    public RareHint(String text, double chance) {
        this.text = text;
        this.chance = chance;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        return Math.random() < chance;
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
