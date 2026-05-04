package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;

import java.time.LocalTime;

public class TimeOfDayHint implements HintProvider {
    private final String text;
    private final int startHour;
    private final int endHour;

    public TimeOfDayHint(String text, int startHour, int endHour) {
        this.text      = text;
        this.startHour = startHour;
        this.endHour   = endHour;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        int hour = LocalTime.now().getHour();
        if (startHour <= endHour) {
            return hour >= startHour && hour < endHour;
        } else {
            return hour >= startHour || hour < endHour;
        }
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
