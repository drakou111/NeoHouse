package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.NeoHouse;
import com.drakou111.neohouse.actions.actiontypes.WaitForAction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.LinkedHashMap;
import java.util.Map;

@EventBusSubscriber(modid= NeoHouse.MOD_ID, bus=EventBusSubscriber.Bus.GAME, value= Dist.CLIENT)
public class ScreenEvents {
    private static final Map<WaitForAction, Void> waitingActions = new LinkedHashMap<>();

    public static void watch(WaitForAction action) {
        waitingActions.put(action, null);
    }

    @SubscribeEvent
    public static void onOpening(ScreenEvent.Opening event) {
        for (WaitForAction action : waitingActions.keySet()) {
            if (action.matchesScreen(event.getNewScreen(), true)) {
                action.markDone();
            }
        }
    }

    @SubscribeEvent
    public static void onClosing(ScreenEvent.Closing event) {
        for (WaitForAction action : waitingActions.keySet()) {
            if (action.matchesScreen(event.getScreen(), false)) {
                action.markDone();
            }
        }
    }
}

