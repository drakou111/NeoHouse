package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.function.Predicate;

public class ConditionalHint implements HintProvider {
    private final Predicate<Minecraft> condition;
    private final String text;

    public ConditionalHint(Predicate<Minecraft> condition, String text) {
        this.condition = condition;
        this.text = text;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        return condition.test(mc);
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
