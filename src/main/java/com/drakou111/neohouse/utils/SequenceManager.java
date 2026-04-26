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

    /** Start running any Action (including an ActionSequence). */
    public static void startAction(Action action) {
        currentAction = action;
    }

    /** Cancel whatever is currently running. */
    public static void cancelAction() {
        currentAction = null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (currentAction == null) return;

        Minecraft mc = Minecraft.getInstance();

        boolean finished;
        try {
            finished = currentAction.run(mc);
        }
        catch (InterruptActionException e) {
            MessageUtils.errorToChat(e.getMessage());
            finished = true;
        }

        if (finished) {
            MessageUtils.debugToChat("Done!");
            currentAction = null;
        }
    }
}