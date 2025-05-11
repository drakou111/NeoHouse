package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.*;
import com.drakou111.neohouse.constants.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ActionBuilder {
    private final List<Action> actions = new ArrayList<>();

    public ActionBuilder clickSlot(int s, boolean leftCLick) {
        actions.add(new SlotAction(s, leftCLick));
        return this;
    }

    public ActionBuilder clickSlotAndWait(int s, boolean leftCLick) {
        actions.add(new SlotAction(s, leftCLick));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        return this;
    }

    public ActionBuilder sendChat(String s) {
        actions.add(new ChatAction(s));
        return this;
    }

    public ActionBuilder sendChatAndWait(String s) {
        actions.add(new ChatAction(s));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        return this;
    }

    public ActionBuilder waitForClassOpen(Class<? extends Screen> c) {
        actions.add(new WaitForAction(c, true));
        return this;
    }

    public ActionBuilder waitForClassClose(Class<? extends Screen> c) {
        actions.add(new WaitForAction(c, true));
        return this;
    }

    public ActionBuilder flipH6(int pages) {
        for (int i = 0; i < pages; i++) {
            actions.add(new SlotAction(Slots.GENERAL_NEXT_H6, true));
            actions.add(new WaitForAction(ContainerScreen.class, true));
        }
        return this;
    }

    public ActionBuilder chatInput(String string, int slot) {

        String capped = string.length() > StringLimits.CHAT_TEXT ? string.substring(0, StringLimits.CHAT_TEXT) : string;

        actions.add(new SlotAction(slot, true));
        actions.add(new WaitForAction(ContainerScreen.class, false));
        actions.add(new ChatAction(capped));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        return this;
    }

    public ActionBuilder anvilInput(String string, int slot) {

        String capped = string.length() > StringLimits.ANVIL_TEXT ? string.substring(0, StringLimits.ANVIL_TEXT) : string;

        actions.add(new SlotAction(slot, true));
        actions.add(new WaitForAction(AnvilScreen.class, true));
        actions.add(new AnvilAction(capped));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        return this;
    }

    public ActionBuilder search(String text, Item itemType, boolean leftClick, ActionBuilder onFound, ActionBuilder onNotFound) {
        actions.add(new SearchAction(Slots.GENERAL_LIST_H6, text, itemType, Slots.GENERAL_NEXT_H6, leftClick, onFound != null ? onFound.get() : null, onNotFound != null ? onNotFound.get() : null));
        return this;
    }

    public ActionBuilder gotoFunction(boolean hasUIOpened, String name, boolean leftClick) {
        if (hasUIOpened) {
            actions.add(new CloseScreenAction());
            actions.add(new WaitTicksAction(1));
        }
        actions.add(new ChatAction(Commands.FUNCTIONS));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        actions.add(new SearchAction(Slots.GENERAL_LIST_H6, name, null, Slots.GENERAL_NEXT_H6, leftClick, null, null));
        return this;
    }

    public ActionBuilder onlyIf(boolean condition, Supplier<ActionBuilder> ifInside) {
        if (condition) {
            ActionBuilder branch = ifInside.get();
            this.actions.addAll(branch.actions);
        }
        return this;
    }

    public ActionBuilder onlyIfElse(boolean condition, Supplier<ActionBuilder> ifInside, Supplier<ActionBuilder> elseInside) {
        ActionBuilder branch;

        if (condition) branch = ifInside.get();
        else branch = elseInside.get();

        this.actions.addAll(branch.actions);

        return this;
    }

    public ActionBuilder addAction(AddAction action, RestrictionAction restriction) {
        return this.clickSlotAndWait(Slots.GENERAL_ADD_H6, true).clickAndSelectSlot(restriction.pageSlotFor(action).page, restriction.pageSlotFor(action).slotID);
    }

    public ActionBuilder addCondition(AddCondition action, RestrictionCondition restriction) {
        return this.clickSlotAndWait(Slots.GENERAL_ADD_H6, true).clickAndSelectSlot(restriction.pageSlotFor(action).page, restriction.pageSlotFor(action).slotID);
    }

    public ActionBuilder clickAndSelectSlot(int page, int slot) {
        return this.flipH6(page).clickSlotAndWait(slot, true);
    }

    public ActionBuilder closeAll() {
        actions.add(new CloseScreenAction());
        return this;
    }

    public ActionBuilder append(Action action) {
        this.actions.add(action);
        return this;
    }

    public ActionBuilder deleteAllAndWait() {
        this.actions.add(new DeleteAllAction(Slots.GENERAL_LIST_H6[0]));
        return this;
    }

    public ActionBuilder removeFunctionIfPresent(String name) {
        return this.search(name, null, false,
                    new ActionBuilder().clickSlotAndWait(Slots.FUNCTION_DELETE, true).clickSlotAndWait(Slots.GENERAL_CONFIRM, true),
                    new ActionBuilder().empty() // Do nothing
                );
    }

    public ActionBuilder empty() {
        this.actions.add(new EmptyAction());
        return this;
    }

    public GroupAction get() {
        return new GroupAction(actions);
    }
}
