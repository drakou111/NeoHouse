package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.config.NeoHouseConfig;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ChatEvents;
import net.minecraft.client.Minecraft;

public class WaitForChatAction extends Action {

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
            if (++extraWait >= extraDelayTicks) {
                ChatEvents.unwatch(this);
                return true;
            }
            return false;
        }
        if (++ticksWaited > NeoHouseConfig.get().actionTimeoutTicks) {
            ChatEvents.unwatch(this);
            return true;
        }
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
        ChatEvents.unwatch(this);
        matched = false;
        registered = false;
        ticksWaited = 0;
        extraWait = 0;
    }
}
