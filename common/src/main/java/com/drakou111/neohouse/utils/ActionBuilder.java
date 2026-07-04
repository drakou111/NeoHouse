package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.config.NeoHouseConfig;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.*;
import com.drakou111.neohouse.constants.*;
import com.drakou111.neohouse.constants.others.CommandInputs;
import com.drakou111.neohouse.constants.others.CustomMenuInputs;
import com.drakou111.neohouse.constants.others.FunctionInputs;
import net.minecraft.client.Minecraft;
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

    public ActionBuilder customItem(String itemId) {
        if (Minecraft.getInstance().player == null)
            throw new IllegalStateException("Cannot build a custom-item action with no player in the world.");
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

    public ActionBuilder chatInput(String string, int slot) {
        String capped = string.length() > StringLimits.CHAT_TEXT ? string.substring(0, StringLimits.CHAT_TEXT) : string;

        actions.add(new SlotAction(slot, true));
        actions.add(new WaitForAction(ContainerScreen.class, false));
        actions.add(clickDelay());
        actions.add(new ChatAction(capped));
        actions.add(new WaitForAction(ContainerScreen.class, true));
        actions.add(clickDelay());
        return this;
    }

    public ActionBuilder anvilInput(String string, int slot) {
        String capped = string.length() > StringLimits.ANVIL_TEXT ? string.substring(0, StringLimits.ANVIL_TEXT) : string;

        actions.add(new SlotAction(slot, true));
        actions.add(new WaitForAction(AnvilScreen.class, true, true));
        actions.add(clickDelay());
        actions.add(new AnvilAction(capped));
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

    public ActionBuilder searchForAnvil(String text, String anvilText) {
        String capped = anvilText.length() > StringLimits.ANVIL_TEXT ? anvilText.substring(0, StringLimits.ANVIL_TEXT) : anvilText;
        actions.add(new SearchAction(null, text, null, true,
                                     AnvilScreen.class,
                                     new ActionBuilder()
                    .append(clickDelay())
                    .append(new AnvilAction(capped))
                    .append(new WaitForAction(ContainerScreen.class, true))
                    .append(clickDelay())
                    .get(),
                                     null));
        return this;
    }

    public ActionBuilder searchForChat(String text, String chatText) {
        String capped = chatText.length() > StringLimits.CHAT_TEXT ? chatText.substring(0, StringLimits.CHAT_TEXT) : chatText;
        actions.add(new SearchAction(null, text, null, true,
                     ContainerScreen.class, false,
                     new ActionBuilder()
                             .append(clickDelay())
                             .append(new ChatAction(capped))
                             .append(new WaitForAction(ContainerScreen.class, true))
                             .append(clickDelay())
                             .get(),
                     null));
        return this;
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
