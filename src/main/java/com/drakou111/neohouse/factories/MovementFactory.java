package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.ActionBuilder;
import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.Commands;

public class MovementFactory {
    public static Action goToFunction(boolean hasUIOpen, String name) {
        return new ActionBuilder()
                .onlyIf(hasUIOpen, () -> new ActionBuilder()
                        .closeAll()
                )
                .sendChatAndWait(Commands.FUNCTIONS)
                .searchAndWait(name, null)
                .get();
    }

    public static Action exit(boolean hasUIOpen) {
        return new ActionBuilder()
                .onlyIf(hasUIOpen, () -> new ActionBuilder()
                        .closeAll()
                )
                .get();
    }

}
