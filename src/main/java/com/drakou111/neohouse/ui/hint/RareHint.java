package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;

import java.util.Random;

public class RareHint implements HintProvider {
    private final Random random = new Random();
    private final String text;
    private final double odds;

    public RareHint(String text, double odds) {
        this.text = text;
        this.odds = odds;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        return random.nextDouble() < odds;
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
