package com.drakou111.neohouse;

import com.drakou111.neohouse.compat.FabricHooks;
import com.drakou111.neohouse.compat.KeyMappingCompat;
import com.drakou111.neohouse.compat.Keys;

public class CancelKeybind {

    public static final KeyMappingCompat CANCEL_KEY = FabricHooks.registerKey(
            new KeyMappingCompat("key.neohouse.cancel", Keys.K)
    );

    public static final KeyMappingCompat OPEN_KEY = FabricHooks.registerKey(
            new KeyMappingCompat("key.neohouse.open", Keys.N)
    );

    public static void register() {
        // referenced to trigger class loading and registration
    }
}
