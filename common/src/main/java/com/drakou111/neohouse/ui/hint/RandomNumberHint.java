package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;

import java.time.Instant;
import java.util.Random;

public class RandomNumberHint implements HintProvider {
    private final Random random = new Random(Instant.now().toEpochMilli());
    private final String text;
    private final String toReplace;

    public RandomNumberHint(String text, String toReplace) {
        this.text = text;
        this.toReplace = toReplace;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        return true;
    }

    @Override
    public String getText(Minecraft mc) {
        return text.replace(toReplace, String.valueOf(random.nextInt(0, 1000000)));
    }
}
