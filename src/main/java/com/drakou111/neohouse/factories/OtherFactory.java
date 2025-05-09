package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.ActionBuilder;
import com.drakou111.neohouse.actions.Action;

public class OtherFactory {
    public static Action deleteAll() {
        return new ActionBuilder()
                .deleteAllAndWait()
                .get();
    }
}
