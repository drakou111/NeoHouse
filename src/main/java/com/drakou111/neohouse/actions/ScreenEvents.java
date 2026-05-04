package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.NeoHouse;
import com.drakou111.neohouse.actions.actiontypes.WaitForAction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.LinkedHashSet;
import java.util.Set;

@EventBusSubscriber(modid= NeoHouse.MOD_ID, bus=EventBusSubscriber.Bus.GAME, value= Dist.CLIENT)
public class ScreenEvents {
    private static final Set<WaitForAction> waitingActions = new LinkedHashSet<>();

    public static void watch(WaitForAction action) {
        waitingActions.add(action);
    }

    @SubscribeEvent
    public static void onOpening(ScreenEvent.Opening event) {
        for (WaitForAction action : waitingActions) {
            if (action.matchesScreen(event.getNewScreen(), true)) {
                action.markDone();
            }
        }
    }

    @SubscribeEvent
    public static void onClosing(ScreenEvent.Closing event) {
        for (WaitForAction action : waitingActions) {
            if (action.matchesScreen(event.getScreen(), false)) {
                action.markDone();
            }
        }
    }
}
