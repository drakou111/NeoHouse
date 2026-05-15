package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.NHConstants;
import com.drakou111.neohouse.actions.actiontypes.WaitForAction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.LinkedHashSet;
import java.util.Set;

@EventBusSubscriber(modid = NHConstants.MOD_ID, value = Dist.CLIENT)
public class ScreenEvents {
    private static final Set<WaitForAction> waitingActions = new LinkedHashSet<>();

    public static void watch(WaitForAction action) {
        waitingActions.add(action);
    }

    public static void unwatch(WaitForAction action) {
        waitingActions.remove(action);
    }

    @SubscribeEvent
    public static void onOpening(ScreenEvent.Opening event) {
        waitingActions.removeIf(action -> {
            if (action.matchesScreen(event.getNewScreen(), true)) action.markDone();
            return action.isDone();
        });
    }

    @SubscribeEvent
    public static void onClosing(ScreenEvent.Closing event) {
        waitingActions.removeIf(action -> {
            if (action.matchesScreen(event.getScreen(), false)) action.markDone();
            return action.isDone();
        });
    }
}
