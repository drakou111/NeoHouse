package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;

public class AlwaysShowHint implements HintProvider {
    private final String text;

    public AlwaysShowHint(String text) {
        this.text = text;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        return true;
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
