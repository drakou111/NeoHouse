package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.config.NeoHouseConfig;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.AnvilAction;
import com.drakou111.neohouse.actions.actiontypes.ChatAction;
import com.drakou111.neohouse.actions.actiontypes.CloseScreenAction;
import com.drakou111.neohouse.actions.actiontypes.CreativeGiveAction;
import com.drakou111.neohouse.actions.actiontypes.EmptyAction;
import com.drakou111.neohouse.actions.actiontypes.GroupAction;
import com.drakou111.neohouse.actions.actiontypes.SearchAction;
import com.drakou111.neohouse.actions.actiontypes.SignAction;
import com.drakou111.neohouse.actions.actiontypes.SlotAction;
import com.drakou111.neohouse.actions.actiontypes.TextEntryAction;
import com.drakou111.neohouse.actions.actiontypes.WaitForAction;
import com.drakou111.neohouse.actions.actiontypes.WaitTicksAction;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.AddCondition;
import com.drakou111.neohouse.constants.Commands;
import com.drakou111.neohouse.constants.Others;
import com.drakou111.neohouse.constants.StringLimits;
import com.drakou111.neohouse.constants.others.CommandInputs;
import com.drakou111.neohouse.constants.others.CustomMenuInputs;
import com.drakou111.neohouse.constants.others.FunctionInputs;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ActionBuilder {
    private final List<Action> actions = new ArrayList<>();

    /** A wait sized to the configured click delay. Reads config at build time, not at construction. */
    private static WaitTicksAction clickDelay() {
        return new WaitTicksAction(NeoHouseConfig.get().clickDelay);
    }

    public ActionBuilder clickSlotAndWait(int s, boolean leftCLick) {
        actions.add(new SlotAction(s, leftCLick));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        actions.add(clickDelay());
        return this;
    }

    /** Puts an item into the open slot editor; the item data is only read when the action runs. */
    public ActionBuilder customItem(String itemId) {
        actions.add(new CreativeGiveAction(itemId));
        actions.add(clickDelay());
        actions.add(new SlotAction(1, true, true));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        actions.add(clickDelay());
        actions.add(new CreativeGiveAction(null));
        return this;
    }

    public ActionBuilder sendChatAndWait(String s) {
        actions.add(new ChatAction(s));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        actions.add(clickDelay());
        return this;
    }

    /** Clicks an input slot and types the value into the chat prompt it opens. */
    public ActionBuilder textInput(String string, int slot) {
        actions.add(new SlotAction(slot, true));
        actions.add(new TextEntryAction(cap(string, StringLimits.CHAT_TEXT), false));
        actions.add(clickDelay());
        return this;
    }

    /** Clicks a list's search bar, which stays an anvil even with chat inputs on, and types the query. */
    public ActionBuilder searchBarInput(String string, int slot) {
        actions.add(new SlotAction(slot, true));
        actions.add(new WaitForAction(AnvilScreen.class, true, true));
        actions.add(clickDelay());
        actions.add(new AnvilAction(cap(string, StringLimits.ANVIL_TEXT)));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        actions.add(clickDelay());
        return this;
    }

    public ActionBuilder search(String text) {
        return search(text, null, true, null, null);
    }

    public ActionBuilder search(String text, Item itemType, boolean leftClick, ActionBuilder onFound, ActionBuilder onNotFound) {
        actions.add(new SearchAction(text, itemType, leftClick, onFound != null ? onFound.get() : null, onNotFound != null ? onNotFound.get() : null));
        actions.add(clickDelay());
        return this;
    }

    /** Finds the item called {@code label}, clicks it and types {@code value} into the chat prompt it opens. */
    public ActionBuilder searchForText(String label, String value) {
        return searchForText(label, value, false);
    }

    /**
     * Like {@link #searchForText(String, String)}; with {@code anvilAllowed} the input may also open
     * an anvil (used where Housing keeps an anvil even with chat inputs on).
     */
    public ActionBuilder searchForText(String label, String value, boolean anvilAllowed) {
        int limit = anvilAllowed ? Math.min(StringLimits.CHAT_TEXT, StringLimits.ANVIL_TEXT) : StringLimits.CHAT_TEXT;
        actions.add(new SearchAction(null, label, null, true,
                ContainerScreen.class, false,
                new ActionBuilder()
                        .append(new TextEntryAction(cap(value, limit), anvilAllowed))
                        .append(clickDelay())
                        .get(),
                null));
        return this;
    }

    private static String cap(String text, int limit) {
        return text.length() > limit ? text.substring(0, limit) : text;
    }

    public ActionBuilder signInput(String searchText, String signText) {
        String capped = signText.length() > StringLimits.SIGN_TEXT ? signText.substring(0, StringLimits.SIGN_TEXT) : signText;
        actions.add(new SearchAction(null, searchText, null, true,
                                     AbstractSignEditScreen.class,
                                     new ActionBuilder()
                             .append(clickDelay())
                             .append(new SignAction(capped))
                             .append(new WaitForAction(ContainerScreen.class, true))
                             .append(clickDelay())
                             .get(),
                                     null));
        return this;
    }

    public ActionBuilder gotoFunction(boolean hasUIOpened, String name, boolean leftClick) {
        if (hasUIOpened) {
            actions.add(new CloseScreenAction());
            actions.add(clickDelay());
        }
        actions.add(new ChatAction(Commands.FUNCTIONS));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        actions.add(clickDelay());
        actions.add(new SearchAction(null, name, null, leftClick, null, null));
        actions.add(clickDelay());
        return this;
    }

    public ActionBuilder gotoCommand(boolean hasUIOpened, String name, boolean leftClick) {
        if (hasUIOpened) {
            actions.add(new CloseScreenAction());
            actions.add(clickDelay());
        }
        actions.add(new ChatAction(Commands.COMMANDS));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        actions.add(clickDelay());
        actions.add(new SearchAction("/" + name, null, leftClick, null, null));
        actions.add(clickDelay());
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

    public ActionBuilder addAction(AddAction action) {
        return this.clickSlotAndWait(Others.GENERAL_ADD_H6, true)
                   .search(action.displayName);
    }

    public ActionBuilder addCondition(AddCondition action) {
        return this.clickSlotAndWait(Others.GENERAL_ADD_H6, true)
                   .search(action.displayName);
    }

    public ActionBuilder closeAll() {
        actions.add(new CloseScreenAction());
        return this;
    }

    public ActionBuilder append(Action action) {
        this.actions.add(action);
        return this;
    }

    public ActionBuilder removeFunctionIfPresent(String name) {
        return this.search(name, null, false,
             new ActionBuilder()
                     .search(FunctionInputs.DELETE)
                     .clickSlotAndWait(Others.GENERAL_CONFIRM, true),

             new ActionBuilder().empty()
        );
    }

    public ActionBuilder removeCommandIfPresent(String name) {
        return this.search("/" + name, null, false,
                           new ActionBuilder()
                                   .search(CommandInputs.DELETE)
                                   .clickSlotAndWait(Others.GENERAL_CONFIRM, true),

                           new ActionBuilder().empty()
                          );
    }

    public ActionBuilder removeMenuIfPresent(String name) {
        return this.search(name, null, false,
                           new ActionBuilder()
                                   .search(CustomMenuInputs.DELETE)
                                   .clickSlotAndWait(Others.GENERAL_CONFIRM, true),

                           new ActionBuilder().empty()
                          );
    }

    public ActionBuilder gotoCustomMenu(String name, boolean leftClick) {
        actions.add(new CloseScreenAction());
        actions.add(clickDelay());
        actions.add(new ChatAction(Commands.CUSTOM_MENUS));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        actions.add(clickDelay());
        actions.add(new SearchAction(null, name, null, leftClick, null, null));
        return this;
    }

    public ActionBuilder copyContext() {
        actions.add(new SlotAction(Others.GENERAL_GO_COPY_PASTE_H6, false));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        actions.add(clickDelay());
        return this;
    }

    public ActionBuilder pasteContext() {
        actions.add(new SlotAction(Others.GENERAL_GO_COPY_PASTE_H6, true));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        actions.add(clickDelay());
        return this;
    }

    public ActionBuilder empty() {
        this.actions.add(new EmptyAction());
        return this;
    }

    public GroupAction get() {
        return new GroupAction(actions);
    }
}
