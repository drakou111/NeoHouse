package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.utils.ActionBuilder;

public class OtherFactory {
    public static Action deleteAll() {
        return new ActionBuilder()
                .deleteAllAndWait()
                .get();
    }
}
