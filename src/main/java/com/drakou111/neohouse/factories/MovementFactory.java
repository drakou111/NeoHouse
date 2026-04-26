package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.Commands;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Slots;
import com.drakou111.neohouse.utils.ActionBuilder;

public class MovementFactory {
    public static Action goToFunction(boolean hasUIOpen, String name) {
        return new ActionBuilder()
                .onlyIf(hasUIOpen, () -> new ActionBuilder()
                        .closeAll()
                )
                .sendChatAndWait(Commands.FUNCTIONS)
                .searchH6(name, null, true, null, null)
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


    // Goes to and deletes the function (if any) and recreates it.
    public static Action goAndRecreateFunction(String functionName, String description, Integer iconItemID, boolean automaticExecution, int loopTime) {
        return new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.FUNCTIONS)
                .removeFunctionIfPresent(functionName)
                .anvilInput(functionName, Slots.GENERAL_ADD_H6)
                .onlyIf((description != null && !description.trim().isEmpty()) || iconItemID != null || automaticExecution, () -> new ActionBuilder()
                        .gotoFunction(true, functionName, false)
                        .onlyIf(description != null && !description.trim().isEmpty(), () -> new ActionBuilder()
                                .chatInput(description, Slots.FUNCTION_DESCRIPTION)
                               )
                        .onlyIf(iconItemID != null, () -> new ActionBuilder()
                                .clickSlotAndWait(Slots.FUNCTION_ICON, true)
                                .clickSlotAndWait(iconItemID, true)
                               )
                        .onlyIf(automaticExecution, () -> new ActionBuilder()
                                .anvilInput(String.valueOf(loopTime), Slots.FUNCTION_AUTOMATIC_EXECUTION)
                               )
                        .gotoFunction(true, functionName, true)
                       )
                .get();
    }

    // Deletes the function (if any) and recreates it.
    public static Action recreateFunction(String functionName, String description, Integer iconItemID, boolean automaticExecution, int loopTime) {
        return new ActionBuilder()
                .removeFunctionIfPresent(functionName)
                .anvilInput(functionName, Slots.GENERAL_ADD_H6)
                .onlyIf((description != null && !description.trim().isEmpty()) || iconItemID != null || automaticExecution, () -> new ActionBuilder()
                        .gotoFunction(true, functionName, false)
                        .onlyIf(description != null && !description.trim().isEmpty(), () -> new ActionBuilder()
                                .chatInput(description, Slots.FUNCTION_DESCRIPTION)
                        )
                        .onlyIf(iconItemID != null, () -> new ActionBuilder()
                                .clickSlotAndWait(Slots.FUNCTION_ICON, true)
                                .clickSlotAndWait(iconItemID, true)
                        )
                        .onlyIf(automaticExecution, () -> new ActionBuilder()
                                .anvilInput(String.valueOf(loopTime), Slots.FUNCTION_AUTOMATIC_EXECUTION)
                        )
                        .gotoFunction(true, functionName, true)
                )
                .get();
    }
}
