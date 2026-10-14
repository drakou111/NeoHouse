package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.Commands;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Others;
import com.drakou111.neohouse.constants.others.CommandInputs;
import com.drakou111.neohouse.constants.others.CustomMenuInputs;
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


    // Adds a command to the open Commands list and applies its settings.
    public static Action createCommand(String name, boolean targeted, int priority, boolean listed) {
        boolean needsConfigure = targeted || priority != 0 || !listed;
        return new ActionBuilder()
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
                .closeAll()
                .get();
    }

    // Adds a custom menu to the open Custom Menus list and gives it the requested size.
    public static Action createMenu(String name, int rows) {
        return new ActionBuilder()
                .anvilInput(name, Others.GENERAL_ADD_H6)
                .onlyIf(rows != 1, () -> new ActionBuilder().append(setMenuSize(name, rows)))
                .closeAll()
                .get();
    }

    // Opens a custom menu's settings and changes its row count.
    public static Action setMenuSize(String name, int rows) {
        String rowsLabel = rows == 1 ? "1 Row" : rows + " Rows";
        return new ActionBuilder()
                .gotoCustomMenu(name, true)
                .search(CustomMenuInputs.CHANGE_SIZE)
                .search(rowsLabel)
                .closeAll()
                .get();
    }

    // Navigates to a menu's slot editing grid (Edit Menu Elements screen).
    public static Action goToMenuGrid(String menuName) {
        return new ActionBuilder()
                .gotoCustomMenu(menuName, true)
                .search(CustomMenuInputs.EDIT_ELEMENTS)
                .get();
    }

}
