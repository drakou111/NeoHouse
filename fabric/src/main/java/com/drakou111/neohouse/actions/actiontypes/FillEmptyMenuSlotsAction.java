package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * For the "Append" override mode: with a custom menu's element grid open, fills only the script's
 * slots that are still empty and leaves elements that already exist alone. A menu smaller than the
 * script's is grown first (never shrunk), so the script's slots past its old size can be filled.
 */
public class FillEmptyMenuSlotsAction extends Action {
    private static final int SLOTS_PER_ROW = 9;
    private static final int STABLE_TICKS = 3;

    private final int rows;
    private final Map<Integer, Action> slotFills;
    private final Action grow;
    private final Action closeGrid;
    private final MenuSettling settling = new MenuSettling(STABLE_TICKS);
    private int waitedTicks = 0;
    private boolean started = false;
    private Action active = null;

    /**
     * {@code slotFills} fills each script slot from the open grid and returns to it; {@code grow}
     * resizes the menu to {@code rows} and reopens the grid; {@code closeGrid} closes it at the end.
     */
    public FillEmptyMenuSlotsAction(int rows, Map<Integer, Action> slotFills, Action grow, Action closeGrid) {
        this.rows = rows;
        this.slotFills = new LinkedHashMap<>(slotFills);
        this.grow = grow;
        this.closeGrid = closeGrid;
    }

    @Override
    public String getDebugName() {
        return "Fill the empty menu slots";
    }

    @Override
    public boolean run(Minecraft mc) {
        if (this.active == null) {
            AbstractContainerMenu menu = mc.player == null ? null : mc.player.containerMenu;
            if (!this.started) {
                this.started = true;
                this.settling.expectLag(MenuSettling.pingTicks(mc));
            }
            if (!hasSettled(mc, menu)) return false;

            this.active = plan(MenuSettling.containerSlots(menu));
            int notTaken = countSteps() - this.active.countSteps();
            if (notTaken > 0) SequenceManager.addSteps(-notTaken);
        }
        return this.active.run(mc);
    }

    private Action plan(List<Slot> grid) {
        List<Boolean> emptyElements = new ArrayList<>();
        for (Slot slot : grid) emptyElements.add(MenuSettling.isEmptyMenuElement(slot));

        List<Action> steps = new ArrayList<>();
        if (this.rows > grid.size() / SLOTS_PER_ROW) steps.add(this.grow);
        for (int slot : slotsToFill(emptyElements, this.slotFills.keySet())) steps.add(this.slotFills.get(slot));
        steps.add(this.closeGrid);
        return new GroupAction(steps);
    }

    /**
     * The script slots to fill, in order: those whose element is empty, and those past the end of the
     * grid, which are new and empty once the menu has grown.
     */
    static List<Integer> slotsToFill(List<Boolean> emptyElements, Collection<Integer> scriptSlots) {
        List<Integer> toFill = new ArrayList<>();
        for (int slot : scriptSlots)
            if (slot >= emptyElements.size() || emptyElements.get(slot)) toFill.add(slot);
        return toFill;
    }

    private boolean hasSettled(Minecraft mc, AbstractContainerMenu menu) {
        if (++this.waitedTicks > NeoHouseConfig.get().actionTimeoutTicks)
            throw new InterruptActionException("Timed out waiting for the menu elements to load.");
        if (!MenuSettling.isOpenMenu(mc, menu) || MenuSettling.containerSlots(menu).stream().noneMatch(Slot::hasItem)) {
            this.settling.reset();
            this.settling.expectLag(MenuSettling.pingTicks(mc));
            return false;
        }
        return this.settling.update(MenuSettling.snapshot(menu));
    }

    @Override
    public int countSteps() {
        int total = this.grow.countSteps() + this.closeGrid.countSteps();
        for (Action fill : this.slotFills.values()) total += fill.countSteps();
        return total;
    }

    @Override
    public boolean requiresCreative() {
        for (Action fill : this.slotFills.values()) if (fill.requiresCreative()) return true;
        return this.grow.requiresCreative() || this.closeGrid.requiresCreative();
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public List<Action> getChildren() {
        List<Action> children = new ArrayList<>(this.slotFills.values());
        children.add(this.grow);
        children.add(this.closeGrid);
        return children;
    }
}
