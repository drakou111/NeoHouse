package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;

import java.time.LocalDate;
import java.time.MonthDay;

public class SeasonalHint implements HintProvider {
    private final MonthDay from;
    private final MonthDay to;
    private final String text;

    public SeasonalHint(int fromMonth, int fromDay, int toMonth, int toDay, String text) {
        this.from = MonthDay.of(fromMonth, fromDay);
        this.to   = MonthDay.of(toMonth, toDay);
        this.text = text;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        MonthDay today = MonthDay.from(LocalDate.now());
        if (from.compareTo(to) <= 0) {
            return today.compareTo(from) >= 0 && today.compareTo(to) <= 0;
        }
        return today.compareTo(from) >= 0 || today.compareTo(to) <= 0;
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
