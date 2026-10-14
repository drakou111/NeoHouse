package com.drakou111.neohouse.actions.actiontypes;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides when an open Housing menu has finished loading: it shows at least one item and its item
 * names haven't changed for a few ticks. Servers send a menu's contents after opening it (and a page
 * turn may send them piece by piece), so reading a menu any earlier can miss items that are there.
 */
final class MenuSettling {
    private final int stableTicksNeeded;
    private List<String> lastSnapshot = null;
    private int stableTicks = 0;

    MenuSettling(int stableTicksNeeded) {
        this.stableTicksNeeded = stableTicksNeeded;
    }

    /** Call once per tick; true once the snapshot has been loaded and unchanged long enough. */
    boolean update(List<String> snapshot) {
        boolean loaded = snapshot.stream().anyMatch(name -> !name.isEmpty());
        if (!loaded || !snapshot.equals(this.lastSnapshot)) {
            this.lastSnapshot = snapshot;
            this.stableTicks = 0;
            return false;
        }
        return ++this.stableTicks >= this.stableTicksNeeded;
    }

    void reset() {
        this.lastSnapshot = null;
        this.stableTicks = 0;
    }

    /** Item names of the menu's own slots (not the player's inventory); "" for an empty slot. */
    static List<String> snapshot(AbstractContainerMenu menu) {
        List<String> names = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) continue;
            names.add(slot.hasItem() ? slot.getItem().getHoverName().getString() : "");
        }
        return names;
    }
}
