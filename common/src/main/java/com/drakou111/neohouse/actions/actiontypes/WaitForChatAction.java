package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ChatEvents;
import net.minecraft.client.Minecraft;

public class WaitForChatAction extends Action {
    private static final int TIMEOUT_TICKS = 200;

    private final String keyword;
    private final int extraDelayTicks;
    private boolean matched = false;
    private int extraWait = 0;
    private int ticksWaited = 0;
    private boolean registered = false;

    public WaitForChatAction(String keyword, int extraDelayTicks) {
        this.keyword = keyword;
        this.extraDelayTicks = extraDelayTicks;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!registered) {
            ChatEvents.watch(this);
            registered = true;
        }
        if (matched) {
            return ++extraWait >= extraDelayTicks;
        }
        if (++ticksWaited > TIMEOUT_TICKS) return true; // timed out — proceed anyway
        return false;
    }

    public boolean matches(String message) {
        return message.contains(keyword);
    }

    public void markMatched() {
        this.matched = true;
        this.extraWait = 0;
    }

    public void reset() {
        matched = false;
        registered = false;
        ticksWaited = 0;
        extraWait = 0;
    }
}
