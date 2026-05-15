package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

public class SearchAction extends Action {
    private final int[] primarySlots;
    private final String targetName;
    private final Item itemType;
    private final boolean leftClick;
    private final boolean includePlayerInventory;
    private final Action onFound;
    private final Action onNotFound;
    private final boolean paged;
    private int searchMissTicks = 0;
    private static final int SEARCH_GRACE_TICKS = 6;

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

    public SearchAction(int[] primarySlots, String targetName, Item itemType, boolean leftClick, boolean includePlayerInventory, boolean paged, Class<? extends Screen> waitForAfterClick, boolean waitForOpen, Action onFound, Action onNotFound) {
        this.primarySlots = primarySlots;
        this.targetName = targetName;
        this.paged = paged;
        this.itemType = itemType;
        this.leftClick = leftClick;
        this.includePlayerInventory = includePlayerInventory;
        this.onFound = onFound;
        this.onNotFound = onNotFound;
        this.waitForUI = new WaitForAction(ContainerScreen.class, true);
        this.waitForFound = new WaitForAction(waitForAfterClick, waitForOpen);
    }

    public SearchAction(int[] primarySlots, String targetName, Item itemType, boolean leftClick, Class<? extends Screen> waitForAfterClick, boolean waitForOpen, Action onFound, Action onNotFound) {
        this(primarySlots, targetName, itemType, leftClick, false, true, waitForAfterClick, waitForOpen, onFound, onNotFound);
    }

    public SearchAction(int[] primarySlots, String targetName, Item itemType, boolean leftClick, Class<? extends Screen> waitForAfterClick, Action onFound, Action onNotFound) {
        this(primarySlots, targetName, itemType, leftClick, false, true, waitForAfterClick, true, onFound, onNotFound);
    }

    public SearchAction(int[] primarySlots, String targetName, Item itemType, boolean leftClick, Action onFound, Action onNotFound) {
        this(primarySlots, targetName, itemType, leftClick, false, true, ContainerScreen.class, true, onFound, onNotFound);
    }

    public SearchAction(String targetName, Item itemType, boolean leftClick, boolean includePlayerInventory, boolean paged,  Action onFound, Action onNotFound) {
        this(null, targetName, itemType, leftClick, includePlayerInventory, paged, ContainerScreen.class, true, onFound, onNotFound);
    }

    public SearchAction(String targetName, Item itemType, boolean leftClick, Action onFound, Action onNotFound) {
        this(null, targetName, itemType, leftClick, false, true, ContainerScreen.class, true, onFound, onNotFound);
    }

    @Override
    public boolean requiresCreative() {
        return (onFound != null && onFound.requiresCreative())
                || (onNotFound != null && onNotFound.requiresCreative());
    }

    @Override
    public boolean run(Minecraft mc) {
        if (mc.player == null) return true;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null) return true;

        int[] slots = primarySlots;
        if (slots == null) {
            slots = menu.slots.stream()
                    .filter(s -> includePlayerInventory || !(s.container instanceof Inventory))
                    .mapToInt(s -> s.index)
                    .toArray();
        }

        if (step == Step.STEP_SEARCH) {
            boolean found = false;

            for (int idx : slots) {
                if (idx < 0 || idx >= menu.slots.size()) continue;

                Slot s = menu.getSlot(idx);
                if (s.hasItem()
                    && s.getItem().getHoverName().getString().equals(targetName)
                    && (itemType == null || s.getItem().getItem() == itemType)
                    && mc.gameMode != null) {

                    mc.gameMode.handleInventoryMouseClick(
                            menu.containerId,
                            idx,
                            leftClick ? 0 : 1,
                            ClickType.PICKUP,
                            mc.player
                                                         );

                    step = Step.STEP_WAIT_FOUND;
                    searchMissTicks = 0;
                    found = true;
                    break;
                }
            }

            if (!found) {
                searchMissTicks++;
                if (searchMissTicks >= SEARCH_GRACE_TICKS) {
                    searchMissTicks = 0;
                    step = Step.STEP_CLICK_BACKUP;
                }
            }
        }
        else if (step == Step.STEP_CLICK_BACKUP) {
            Slot arrow = menu.getSlot(slots[slots.length - 1]);
            if (arrow.hasItem() && mc.gameMode != null) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, arrow.index, 0, ClickType.PICKUP, mc.player);
                step = Step.STEP_WAIT_UI;
            } else {
                step = Step.STEP_NOT_FOUND;
            }
        }
        else if (step == Step.STEP_WAIT_UI) {
            if (waitForUI.run(mc) && waitTick.run(mc)) {
                step = Step.STEP_SEARCH;
                searchMissTicks = 0;
                waitForUI.reset();
                waitTick.reset();
            }
        }
        else if (step == Step.STEP_WAIT_FOUND) {
            if (waitForFound.run(mc) && waitTick.run(mc)) {
                step = Step.STEP_FOUND;
                waitForFound.reset();
                waitTick.reset();
            }
        }
        else if (step == Step.STEP_FOUND) {
            if (onFound == null || onFound.run(mc)) step = Step.STEP_DONE;
        }
        else if (step == Step.STEP_NOT_FOUND) {
            if (onNotFound == null || onNotFound.run(mc)) step = Step.STEP_DONE;
            if (onNotFound == null) throw new InterruptActionException("Could not find item called '" + targetName + "'.");
        }
        else {
            return true;
        }
        return false;
    }
}
