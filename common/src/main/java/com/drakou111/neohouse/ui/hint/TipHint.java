package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;

public class TipHint implements HintProvider {
    private final String tip;

    public TipHint(String tip) {
        this.tip = tip;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        return true;
    }

    @Override
    public String getText(Minecraft mc) {
        return "Tip: " + tip;
    }
}
