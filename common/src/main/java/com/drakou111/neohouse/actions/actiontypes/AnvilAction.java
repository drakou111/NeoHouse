package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.inventory.ClickType;

public class AnvilAction extends Action {
    private static final int STABLE_TICKS  = 3;
    private static final int TIMEOUT_TICKS = 100;

    private final String name;
    private String  lastValue   = null;
    private int     stableTicks = 0;
    private boolean nameSet     = false;
    private int     ticks       = 0;

    public AnvilAction(String name) {
        this.name = name;
    }

    @Override
    public String getDebugName() { return "Anvil input: " + name; }

    public boolean run(Minecraft mc) {
        if (!(mc.screen instanceof AnvilScreen anvil)) throw new InterruptActionException("Not in anvil screen.");
        if (++ticks > TIMEOUT_TICKS) throw new InterruptActionException("Timed out waiting for anvil.");

        EditBox edit = anvil.children().stream()
                .filter(c -> c instanceof EditBox)
                .map(c -> (EditBox) c)
                .findFirst()
                .orElse(null);

        if (edit == null) return false;

        if (!nameSet) {
            // Wait for the field to stop changing (server finished populating/clearing it).
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

        // Wait for the server to confirm the rename by populating the output slot.
        if (!anvil.getMenu().getSlot(2).hasItem()) return false;

        mc.gameMode.handleInventoryMouseClick(anvil.getMenu().containerId, 2, 0, ClickType.PICKUP, mc.player);
        return true;
    }
}
