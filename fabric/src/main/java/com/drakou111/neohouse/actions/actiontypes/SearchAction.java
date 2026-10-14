package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.config.NeoHouseConfig;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Finds the item with a given name in the open menu and clicks it, turning pages when needed.
 *
 * <p>The target is clicked as soon as it shows up. "Not on this page" is only concluded once the menu
 * has loaded (it shows items) and its contents have stayed the same for a few ticks, so a slow
 * server can't make a present item look missing. A page turn waits until the contents change.</p>
 */
public class SearchAction extends Action {
    private static final int STABLE_TICKS = 4;
    private static final int PAGE_TURN_TIMEOUT_TICKS = 60;
    private static final String NEXT_PAGE_WORD = "next";

    private final int[] primarySlots;
    private final String targetName;
    private final Item itemType;
    private final boolean leftClick;
    private final boolean includePlayerInventory;
    private final Action onFound;
    private final Action onNotFound;

    private final WaitForAction waitForFound;
    private final WaitTicksAction waitTick = new WaitTicksAction(1);

    private enum Step { SEARCH, TURN_PAGE, WAIT_FOUND, FOUND, NOT_FOUND, DONE }

    private Step step = Step.SEARCH;
    private final MenuSettling settling = new MenuSettling(STABLE_TICKS);
    private List<String> pageBeforeTurn = null;
    private final Set<List<String>> seenPages = new HashSet<>();
    private int stepTicks = 0;

    public SearchAction(int[] primarySlots, String targetName, Item itemType, boolean leftClick, boolean includePlayerInventory, Class<? extends Screen> waitForAfterClick, boolean waitForOpen, Action onFound, Action onNotFound) {
        this.primarySlots = primarySlots;
        this.targetName = targetName;
        this.itemType = itemType;
        this.leftClick = leftClick;
        this.includePlayerInventory = includePlayerInventory;
        this.onFound = onFound;
        this.onNotFound = onNotFound;
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
    public String getDebugName() { return "Search: " + (this.targetName != null ? this.targetName : "(item type)"); }

    @Override
    public boolean run(Minecraft mc) {
        if (mc.player == null) return true;
        AbstractContainerMenu menu = mc.player.containerMenu;

        switch (this.step) {
            case SEARCH -> search(mc, menu);
            case TURN_PAGE -> waitForPageTurn(menu);
            case WAIT_FOUND -> {
                if (this.waitForFound.run(mc) && this.waitTick.run(mc)) {
                    discardStepsOf(this.onNotFound, this.onFound);
                    this.step = Step.FOUND;
                }
            }
            case FOUND -> {
                if (this.onFound == null || this.onFound.run(mc)) this.step = Step.DONE;
            }
            case NOT_FOUND -> {
                if (this.onNotFound == null) throw new InterruptActionException("Could not find item called '" + this.targetName + "'.");
                if (this.onNotFound.run(mc)) this.step = Step.DONE;
            }
            case DONE -> {
                return true;
            }
        }
        return false;
    }

    private void search(Minecraft mc, AbstractContainerMenu menu) {
        int[] slots = searchSlots(menu);
        for (int index : slots) {
            if (index < 0 || index >= menu.slots.size()) continue;
            Slot slot = menu.getSlot(index);
            if (!matches(slot.getItem()) || mc.gameMode == null) continue;

            mc.gameMode.handleInventoryMouseClick(menu.containerId, index, this.leftClick ? 0 : 1, ClickType.PICKUP, mc.player);
            this.step = Step.WAIT_FOUND;
            return;
        }

        if (!hasSettled(menu, slots)) return;

        List<String> page = snapshot(menu, slots);
        Slot arrow = slots.length == 0 ? null : menu.getSlot(slots[slots.length - 1]);
        if (!this.seenPages.add(page) || arrow == null || !isNextPageArrow(arrow.getItem()) || mc.gameMode == null) {
            discardStepsOf(this.onFound, this.onNotFound);
            this.step = Step.NOT_FOUND;
            return;
        }

        mc.gameMode.handleInventoryMouseClick(menu.containerId, arrow.index, 0, ClickType.PICKUP, mc.player);
        this.pageBeforeTurn = page;
        this.stepTicks = 0;
        this.step = Step.TURN_PAGE;
    }

    private void waitForPageTurn(AbstractContainerMenu menu) {
        if (++this.stepTicks > PAGE_TURN_TIMEOUT_TICKS) {
            // The arrow did nothing: this was the last page.
            discardStepsOf(this.onFound, this.onNotFound);
            this.step = Step.NOT_FOUND;
            return;
        }
        if (snapshot(menu, searchSlots(menu)).equals(this.pageBeforeTurn)) return;
        resetSettling();
        this.step = Step.SEARCH;
    }

    /** True once the menu shows items and they haven't changed for a few ticks. */
    private boolean hasSettled(AbstractContainerMenu menu, int[] slots) {
        if (++this.stepTicks > NeoHouseConfig.get().actionTimeoutTicks)
            throw new InterruptActionException("Timed out waiting for the menu to load while looking for '" + this.targetName + "'.");

        return this.settling.update(snapshot(menu, slots));
    }

    private void resetSettling() {
        this.settling.reset();
        this.stepTicks = 0;
    }

    private boolean matches(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getHoverName().getString().equals(this.targetName)
                && (this.itemType == null || stack.getItem() == this.itemType);
    }

    private static boolean isNextPageArrow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (name.contains(NEXT_PAGE_WORD)) return true;
        // "Go Back" and "Previous Page" are arrows too.
        return stack.is(Items.ARROW) && !name.contains("back") && !name.contains("previous");
    }

    private int[] searchSlots(AbstractContainerMenu menu) {
        if (this.primarySlots != null) return this.primarySlots;
        List<Integer> indices = new ArrayList<>();
        for (Slot slot : menu.slots)
            if (this.includePlayerInventory || !(slot.container instanceof Inventory)) indices.add(slot.index);
        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    private static List<String> snapshot(AbstractContainerMenu menu, int[] slots) {
        List<String> names = new ArrayList<>(slots.length);
        for (int index : slots) {
            boolean filled = index >= 0 && index < menu.slots.size() && menu.getSlot(index).hasItem();
            names.add(filled ? menu.getSlot(index).getItem().getHoverName().getString() : "");
        }
        return names;
    }

    /** Removes the progress steps of the branch that won't run, beyond what the taken branch counts. */
    private static void discardStepsOf(Action skipped, Action taken) {
        int skippedSteps = skipped != null ? skipped.countSteps() : 0;
        int takenSteps = taken != null ? taken.countSteps() : 0;
        SequenceManager.addSteps(-Math.max(0, skippedSteps - takenSteps));
    }

    @Override
    public int countSteps() {
        int found = this.onFound != null ? this.onFound.countSteps() : 0;
        int notFound = this.onNotFound != null ? this.onNotFound.countSteps() : 0;
        return 1 + Math.max(found, notFound);
    }

    @Override
    public boolean requiresCreative() {
        return (this.onFound != null && this.onFound.requiresCreative())
                || (this.onNotFound != null && this.onNotFound.requiresCreative());
    }

    @Override
    public List<Action> getChildren() {
        return Stream.of(this.onFound, this.onNotFound).filter(Objects::nonNull).toList();
    }
}
