package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.config.NeoHouseConfig;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.inventory.ClickType;

public class AnvilAction extends Action {
    private static final int STABLE_TICKS  = 3;
    private static final int CONFIRM_TICKS = 4;

    private final String name;
    private String lastValue    = null;
    private int stableTicks  = 0;
    private boolean nameSet      = false;
    private int confirmTicks = 0;
    private int ticks        = 0;

    public AnvilAction(String name) {
        this.name = name;
    }

    @Override
    public String getDebugName() { return "Anvil input: " + name; }

    public boolean run(Minecraft mc) {
        if (!(mc.screen instanceof AnvilScreen anvil)) throw new InterruptActionException("Not in anvil screen.");
        if (++ticks > NeoHouseConfig.get().actionTimeoutTicks) throw new InterruptActionException("Timed out waiting for anvil.");

        EditBox edit = anvil.children().stream()
                .filter(c -> c instanceof EditBox)
                .map(c -> (EditBox) c)
                .findFirst()
                .orElse(null);

        if (edit == null) return false;

        if (!nameSet) {
            String current = edit.getValue();
            if (!current.equals(lastValue)) {
                lastValue   = current;
                stableTicks = 0;
                return false;
            }
            if (++stableTicks < STABLE_TICKS) return false;

            edit.setValue(name);
            anvil.getMenu().setItemName(name);
            nameSet = true;
            return false;
        }

        String current = edit.getValue();
        if (!current.equals(name)) {
            edit.setValue(name);
            anvil.getMenu().setItemName(name);
            confirmTicks = 0;
            return false;
        }

        if (++confirmTicks < CONFIRM_TICKS) return false;

        if (!anvil.getMenu().getSlot(2).hasItem()) return false;

        mc.gameMode.handleInventoryMouseClick(anvil.getMenu().containerId, 2, 0, ClickType.PICKUP, mc.player);
        return true;
    }
}
