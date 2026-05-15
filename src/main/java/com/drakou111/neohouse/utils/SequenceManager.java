package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.NeoHouse;
import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = NeoHouse.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SequenceManager {
    private static Action currentAction = null;

    public static void startAction(Action action) {
        currentAction = action;
    }

    public static void cancelAction() {
        currentAction = null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (currentAction == null) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && !mc.player.getAbilities().instabuild && currentAction.requiresCreative()) {
            MessageUtils.errorToChat("Script stopped: creative mode is required for item actions.");
            currentAction = null;
            return;
        }

        boolean finished;
        try {
            finished = currentAction.run(mc);
        } catch (InterruptActionException e) {
            MessageUtils.errorToChat(e.getMessage() != null ? e.getMessage() : e.toString());
            currentAction = null;
            return;
        } catch (Exception e) {
            MessageUtils.errorToChat("Script aborted due to unexpected error: " + e);
            currentAction = null;
            return;
        }

        if (finished) {
            MessageUtils.debugToChat("Done!");
            currentAction = null;
        }
    }
}
