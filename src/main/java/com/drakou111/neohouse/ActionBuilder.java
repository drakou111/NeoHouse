package com.drakou111.neohouse;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.*;
import com.drakou111.neohouse.constants.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public class ActionBuilder {
    private final List<Action> actions = new ArrayList<>();

    public ActionBuilder clickSlot(int s) {
        actions.add(new SlotAction(s));
        return this;
    }

    public ActionBuilder clickSlotAndWait(int s) {
        actions.add(new SlotAction(s));
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
            actions.add(new SlotAction(Slots.GENERAL_NEXT_H6));
            actions.add(new WaitForAction(ContainerScreen.class, true));
        }
        return this;
    }

    public ActionBuilder chatInput(String string, int slot) {

        String capped = string.length() > StringLimits.CHAT_TEXT ? string.substring(0, StringLimits.CHAT_TEXT) : string;

        actions.add(new SlotAction(slot));
        actions.add(new WaitForAction(ContainerScreen.class, false));
        actions.add(new ChatAction(capped));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        return this;
    }

    public ActionBuilder anvilInput(String string, int slot) {

        String capped = string.length() > StringLimits.ANVIL_TEXT ? string.substring(0, StringLimits.ANVIL_TEXT) : string;

        actions.add(new SlotAction(slot));
        actions.add(new WaitForAction(AnvilScreen.class, true));
        actions.add(new AnvilAction(capped));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        return this;
    }

    public ActionBuilder search(String text, Item itemType) {
        actions.add(new SearchAction(Slots.GENERAL_LIST_H6, text, itemType, Slots.GENERAL_NEXT_H6, ContainerScreen.class, true));
        return this;
    }

    public ActionBuilder searchAndWait(String text, Item itemType) {
        actions.add(new SearchAction(Slots.GENERAL_LIST_H6, text, itemType, Slots.GENERAL_NEXT_H6, ContainerScreen.class, true));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        return this;
    }

    public ActionBuilder gotoFunction(boolean hasUIOpened, String name) {
        if (hasUIOpened) {
            actions.add(new CloseScreenAction());
            actions.add(new WaitForAction(ContainerScreen.class, false));
        }
        actions.add(new ChatAction(Commands.FUNCTIONS));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        actions.add(new SearchAction(Slots.GENERAL_LIST_H6, name, null, Slots.GENERAL_NEXT_H6, ContainerScreen.class, true));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        return this;
    }

    public ActionBuilder onlyIf(boolean condition, ActionBuilder ifInside) {
        if (condition) this.actions.addAll(ifInside.actions);
        return this;
    }

    public ActionBuilder onlyIfElse(boolean condition, ActionBuilder ifInside, ActionBuilder elseInside) {
        if (condition) this.actions.addAll(ifInside.actions);
        else this.actions.addAll(elseInside.actions);

        return this;
    }

    public ActionBuilder addAction(AddAction action, RestrictionAction restriction) {
        return this.clickSlotAndWait(Slots.GENERAL_ADD_H6).clickAndSelectSlot(action.getPage(restriction), action.getSlotID(restriction));
    }

    public ActionBuilder clickAndSelectSlot(int page, int slot) {
        return this.flipH6(page).clickSlotAndWait(slot);
    }

    public ActionBuilder append(Action action) {
        this.actions.add(action);
        return this;
    }

    public GroupAction get() {
        return new GroupAction(actions);
    }
}
