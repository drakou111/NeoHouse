package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.Others;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Waits until the open container has fully loaded, then runs one branch if it holds no content and
 * the other otherwise. Used by the "If Empty" override mode to decide whether an existing function,
 * command, event or menu may be filled. Waiting for the load matters: reading too early would see an
 * empty screen and overwrite content that simply hadn't arrived yet.
 */
public class IfContainerEmptyAction extends Action {
    private static final int FIRST_ACTION_SLOT = 10;
    private static final int STABLE_TICKS = 3;
    private static final int LOAD_TIMEOUT_TICKS = 100;

    private final String description;
    private final Predicate<AbstractContainerMenu> isLoaded;
    private final Predicate<AbstractContainerMenu> isEmpty;
    private final Action ifEmpty;
    private final Action ifNotEmpty;
    private final MenuSettling settling = new MenuSettling(STABLE_TICKS);
    private int waitedTicks = 0;
    private boolean decided = false;
    private Action active = null;

    private IfContainerEmptyAction(String description, Predicate<AbstractContainerMenu> isLoaded,
                                   Predicate<AbstractContainerMenu> isEmpty, Action ifEmpty, Action ifNotEmpty) {
        this.description = description;
        this.isLoaded = isLoaded;
        this.isEmpty = isEmpty;
        this.ifEmpty = ifEmpty;
        this.ifNotEmpty = ifNotEmpty;
    }

    /** For an action editor (function, command, event): empty when it lists no action. */
    public static IfContainerEmptyAction actionList(Action ifEmpty, Action ifNotEmpty) {
        return new IfContainerEmptyAction("action list",
                menu -> hasItem(menu, Others.GENERAL_ADD_H6),
                menu -> !hasItem(menu, FIRST_ACTION_SLOT),
                ifEmpty, ifNotEmpty);
    }

    /** For a custom menu's element grid: empty when every element is an "Empty Slot" glass pane. */
    public static IfContainerEmptyAction menuGrid(Action ifEmpty, Action ifNotEmpty) {
        return new IfContainerEmptyAction("menu elements",
                menu -> !containerSlots(menu).stream().allMatch(slot -> !slot.hasItem()),
                menu -> containerSlots(menu).stream()
                        .allMatch(slot -> !slot.hasItem() || slot.getItem().is(Items.GRAY_STAINED_GLASS_PANE)),
                ifEmpty, ifNotEmpty);
    }

    @Override
    public String getDebugName() {
        return "Check if " + this.description + " is empty";
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!this.decided) {
            AbstractContainerMenu menu = mc.player == null ? null : mc.player.containerMenu;
            if (menu == null || !hasSettled(menu)) return false;

            this.decided = true;
            this.active = this.isEmpty.test(menu) ? this.ifEmpty : this.ifNotEmpty;
            int taken = this.active != null ? this.active.countSteps() : 0;
            int total = countSteps();
            if (total > taken) SequenceManager.addSteps(-(total - taken));
        }
        if (this.active == null) return true;
        if (this.active.run(mc)) {
            this.active = null;
            return true;
        }
        return false;
    }

    private boolean hasSettled(AbstractContainerMenu menu) {
        if (++this.waitedTicks > LOAD_TIMEOUT_TICKS)
            throw new InterruptActionException("Timed out waiting for the " + this.description + " to load.");
        if (!this.isLoaded.test(menu)) {
            this.settling.reset();
            return false;
        }
        return this.settling.update(MenuSettling.snapshot(menu));
    }

    private static List<Slot> containerSlots(AbstractContainerMenu menu) {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) if (!(slot.container instanceof Inventory)) slots.add(slot);
        return slots;
    }

    private static boolean hasItem(AbstractContainerMenu menu, int slot) {
        return slot < menu.slots.size() && menu.slots.get(slot).hasItem();
    }

    @Override
    public int countSteps() {
        int empty = this.ifEmpty != null ? this.ifEmpty.countSteps() : 0;
        int notEmpty = this.ifNotEmpty != null ? this.ifNotEmpty.countSteps() : 0;
        return Math.max(empty, notEmpty);
    }

    @Override
    public boolean requiresCreative() {
        return (this.ifEmpty != null && this.ifEmpty.requiresCreative())
                || (this.ifNotEmpty != null && this.ifNotEmpty.requiresCreative());
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public List<Action> getChildren() {
        return Stream.of(this.ifEmpty, this.ifNotEmpty).filter(Objects::nonNull).toList();
    }
}
