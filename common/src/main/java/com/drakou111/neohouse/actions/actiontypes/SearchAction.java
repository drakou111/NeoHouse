package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class SearchAction extends Action {
    private final int[] primarySlots;
    private final String targetName;
    private final Item itemType;
    private final boolean leftClick;
    private final boolean includePlayerInventory;
    private final Action onFound;
    private final Action onNotFound;
    private static final int STABLE_TICKS = 3;
    private static final int TIMEOUT_TICKS = 20;
    private static final int MAX_RECHECKS = 1;
    private int lastNonEmptyCount = -1;
    private int stableTicks = 0;
    private int totalSearchTicks = 0;
    private int recheckCount = 0;

    private final WaitForAction waitForUI;
    private final WaitForAction waitForFound;
    private final WaitTicksAction waitTick = new WaitTicksAction(1);

    private enum Step {
        STEP_SEARCH,
        STEP_CLICK_BACKUP,
        STEP_WAIT_UI,
        STEP_WAIT_FOUND,
        STEP_FOUND,
        STEP_NOT_FOUND,
        STEP_DONE
    }

    private Step step = Step.STEP_SEARCH;

    public SearchAction(int[] primarySlots, String targetName, Item itemType, boolean leftClick, boolean includePlayerInventory, Class<? extends Screen> waitForAfterClick, boolean waitForOpen, Action onFound, Action onNotFound) {
        this.primarySlots = primarySlots;
        this.targetName = targetName;
        this.itemType = itemType;
        this.leftClick = leftClick;
        this.includePlayerInventory = includePlayerInventory;
        this.onFound = onFound;
        this.onNotFound = onNotFound;
        this.waitForUI = new WaitForAction(ContainerScreen.class, true, true);
        this.waitForFound = new WaitForAction(waitForAfterClick, waitForOpen, waitForAfterClick != ContainerScreen.class);
    }

    public SearchAction(int[] primarySlots, String targetName, Item itemType, boolean leftClick, Class<? extends Screen> waitForAfterClick, boolean waitForOpen, Action onFound, Action onNotFound) {
        this(primarySlots, targetName, itemType, leftClick, false, waitForAfterClick, waitForOpen, onFound, onNotFound);
    }

    public SearchAction(int[] primarySlots, String targetName, Item itemType, boolean leftClick, Class<? extends Screen> waitForAfterClick, Action onFound, Action onNotFound) {
        this(primarySlots, targetName, itemType, leftClick, false, waitForAfterClick, true, onFound, onNotFound);
    }

    public SearchAction(int[] primarySlots, String targetName, Item itemType, boolean leftClick, Action onFound, Action onNotFound) {
        this(primarySlots, targetName, itemType, leftClick, false, ContainerScreen.class, true, onFound, onNotFound);
    }

    public SearchAction(String targetName, Item itemType, boolean leftClick, Action onFound, Action onNotFound) {
        this(null, targetName, itemType, leftClick, false, ContainerScreen.class, true, onFound, onNotFound);
    }

    @Override
    public String getDebugName() { return "Search: " + (targetName != null ? targetName : "(item type)"); }

    @Override
    public boolean run(Minecraft mc) {
        if (mc.player == null) return true;
        AbstractContainerMenu menu = mc.player.containerMenu;

        int[] slots = primarySlots;
        if (slots == null) {
            slots = menu.slots.stream()
                    .filter(s -> includePlayerInventory || !(s.container instanceof Inventory))
                    .mapToInt(s -> s.index)
                    .toArray();
        }

        if (step == Step.STEP_SEARCH) {
            int nonEmpty = 0;
            for (int idx : slots) {
                if (idx >= 0 && idx < menu.slots.size() && menu.getSlot(idx).hasItem()) nonEmpty++;
            }

            if (nonEmpty != lastNonEmptyCount) {
                lastNonEmptyCount = nonEmpty;
                stableTicks = 0;
            } else {
                stableTicks++;
            }

            totalSearchTicks++;

            boolean found = false;
            for (int idx : slots) {
                if (idx < 0 || idx >= menu.slots.size()) continue;
                Slot s = menu.getSlot(idx);
                if (s.hasItem()
                        && s.getItem().getHoverName().getString().equals(targetName)
                        && (itemType == null || s.getItem().getItem() == itemType)
                        && mc.gameMode != null) {

                    mc.gameMode.handleInventoryMouseClick(
                            menu.containerId, idx,
                            leftClick ? 0 : 1, ClickType.PICKUP, mc.player);

                    step = Step.STEP_WAIT_FOUND;
                    resetStability();
                    found = true;
                    break;
                }
            }

            if (!found) {
                boolean settled  = stableTicks >= STABLE_TICKS;
                boolean timedOut = totalSearchTicks >= TIMEOUT_TICKS;
                if (settled || timedOut) {
                    recheckCount++;
                    resetStability();
                    if (recheckCount > MAX_RECHECKS || timedOut) {
                        recheckCount = 0;
                        step = Step.STEP_CLICK_BACKUP;
                    }
                }
            }
        }
        else if (step == Step.STEP_CLICK_BACKUP) {
            if (slots.length == 0) {
                throw new InterruptActionException("No slots available to search.");
            }
            Slot arrow = menu.getSlot(slots[slots.length - 1]);
            if (arrow.hasItem() && mc.gameMode != null) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, arrow.index, 0, ClickType.PICKUP, mc.player);
                step = Step.STEP_WAIT_UI;
            } else {
                int foundSteps    = onFound    != null ? onFound.countSteps()    : 0;
                int notFoundSteps = onNotFound != null ? onNotFound.countSteps() : 0;
                SequenceManager.addSteps(-(Math.max(0, foundSteps - notFoundSteps)));
                step = Step.STEP_NOT_FOUND;
            }
        }
        else if (step == Step.STEP_WAIT_UI) {
            if (waitForUI.run(mc) && waitTick.run(mc)) {
                step = Step.STEP_SEARCH;
                recheckCount = 0;
                waitForUI.reset();
                waitTick.reset();
            }
        }
        else if (step == Step.STEP_WAIT_FOUND) {
            boolean done;
            done = waitForFound.run(mc) && waitTick.run(mc);
            if (done) {
                int foundSteps    = onFound    != null ? onFound.countSteps()    : 0;
                int notFoundSteps = onNotFound != null ? onNotFound.countSteps() : 0;
                SequenceManager.addSteps(-(Math.max(0, notFoundSteps - foundSteps)));
                step = Step.STEP_FOUND;
                waitForFound.reset();
                waitTick.reset();
            }
        }
        else if (step == Step.STEP_FOUND) {
            if (onFound == null || onFound.run(mc)) step = Step.STEP_DONE;
        }
        else if (step == Step.STEP_NOT_FOUND) {
            if (onNotFound == null) throw new InterruptActionException("Could not find item called '" + targetName + "'.");
            if (onNotFound.run(mc)) step = Step.STEP_DONE;
        }
        else {
            return true;
        }
        return false;
    }

    @Override
    public int countSteps() {
        int found    = onFound    != null ? onFound.countSteps()    : 0;
        int notFound = onNotFound != null ? onNotFound.countSteps() : 0;
        return 1 + Math.max(found, notFound);
    }

    @Override
    public boolean requiresCreative() {
        return (onFound != null && onFound.requiresCreative())
                || (onNotFound != null && onNotFound.requiresCreative());
    }

    private void resetStability() {
        lastNonEmptyCount = -1;
        stableTicks      = 0;
        totalSearchTicks = 0;
    }

    @Override
    public List<Action> getChildren() {
        return Stream.of(this.onFound, this.onNotFound).filter(Objects::nonNull).toList();
    }
}
