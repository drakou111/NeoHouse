package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.Commands;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Others;
import com.drakou111.neohouse.constants.others.CommandInputs;
import com.drakou111.neohouse.constants.others.CustomMenuInputs;
import com.drakou111.neohouse.constants.others.FunctionInputs;
import com.drakou111.neohouse.utils.ActionBuilder;

public class MovementFactory {
    public static Action goToFunction(boolean hasUIOpen, String name) {
        return new ActionBuilder()
                .onlyIf(hasUIOpen, () -> new ActionBuilder()
                        .closeAll()
                )
                .sendChatAndWait(Commands.FUNCTIONS)
                .search(name)
                .get();
    }

    public static Action goToEvent(Event event) {
        return new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.EVENT_ACTION)
                .clickSlotAndWait(event.getSlotID(), true)
                .get();
    }

    public static Action exit(boolean hasUIOpen) {
        return new ActionBuilder()
                .onlyIf(hasUIOpen, () -> new ActionBuilder()
                        .closeAll()
                )
                .get();
    }


    // Creates (or recreates) a command with the given settings, ending with an open screen.
    public static Action goAndRecreateCommand(String name, boolean targeted, int priority, boolean listed) {
        boolean needsConfigure = targeted || priority != 0 || !listed;
        return new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.COMMANDS)
                .removeCommandIfPresent(name)
                .anvilInput(name, Others.GENERAL_ADD_H6)
                .onlyIf(needsConfigure, () -> new ActionBuilder()
                        .gotoCommand(true, name, false)
                        .onlyIf(targeted, () -> new ActionBuilder()
                                .search(CommandInputs.MODE))
                        .onlyIf(priority != 0, () -> new ActionBuilder()
                                .signInput(CommandInputs.GROUP_PRIORITY, String.valueOf(priority)))
                        .onlyIf(!listed, () -> new ActionBuilder()
                                .search(CommandInputs.LISTED))
                )
                .get();
    }

    // Creates (or recreates) a custom menu with the given name and row count.
    public static Action goAndRecreateMenu(String name, int rows) {
        String rowsLabel = rows == 1 ? "1 Row" : rows + " Rows";
        return new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.CUSTOM_MENUS)
                .removeMenuIfPresent(name)
                .anvilInput(name, Others.GENERAL_ADD_H6)
                .onlyIf(rows != 1, () -> new ActionBuilder()
                        .gotoCustomMenu(name, true)
                        .search(CustomMenuInputs.CHANGE_SIZE)
                        .search(rowsLabel)
                )
                .get();
    }

    // Navigates to a menu's slot editing grid (Edit Menu Elements screen).
    public static Action goToMenuGrid(String menuName) {
        return new ActionBuilder()
                .gotoCustomMenu(menuName, true)
                .search(CustomMenuInputs.EDIT_ELEMENTS)
                .get();
    }

    // Goes to and deletes the function (if any) and recreates it.
    public static Action goAndRecreateFunction(String functionName, String description, Integer iconItemID, boolean automaticExecution, int loopTime) {
        return new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.FUNCTIONS)
                .removeFunctionIfPresent(functionName)
                .anvilInput(functionName, Others.GENERAL_ADD_H6)
                .onlyIf((description != null && !description.trim().isEmpty()) || iconItemID != null || automaticExecution, () -> new ActionBuilder()
                        .gotoFunction(true, functionName, false)
                        .onlyIf(description != null && !description.trim().isEmpty(), () -> new ActionBuilder()
                                .searchForChat(FunctionInputs.DESCRIPTION, description)
                        )
                        .onlyIf(iconItemID != null, () -> new ActionBuilder()
                                .search(FunctionInputs.ICON)
                                .clickSlotAndWait(iconItemID, true)
                        )
                        .onlyIf(automaticExecution, () -> new ActionBuilder()
                                .searchForAnvil(FunctionInputs.AUTOMATIC_EXECUTION, String.valueOf(loopTime))
                        )
                        .gotoFunction(true, functionName, true)
                       )
                .get();
    }
}
