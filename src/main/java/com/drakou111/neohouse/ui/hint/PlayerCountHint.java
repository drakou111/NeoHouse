package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;

public class PlayerCountHint implements HintProvider {
    private final String text;
    private final int min;
    private final int max;

    public PlayerCountHint(String text, int min, int max) {
        this.text = text;
        this.min  = min;
        this.max  = max;
    }

    public PlayerCountHint(String text, int exact) {
        this(text, exact, exact);
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        if (mc == null || mc.level == null) return false;
        int count = 0;
        for (var player : mc.level.players()) {
            if (player.isControlledByOrIsLocalPlayer()) count++;
            if (count > max) return false;
        }
        return count >= min;
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
