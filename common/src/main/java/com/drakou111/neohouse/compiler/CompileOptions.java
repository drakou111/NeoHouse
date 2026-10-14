package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;

import java.util.Objects;

/** The user-configurable compiler switches, passed in explicitly so the compiler never reads global config. */
public record CompileOptions(MultiCallMode multiCallMode, RecursionMode recursionMode,
                             GlobalCallMode globalCallMode, OverrideMode overrideMode) {

    public CompileOptions {
        Objects.requireNonNull(multiCallMode);
        Objects.requireNonNull(recursionMode);
        Objects.requireNonNull(globalCallMode);
        Objects.requireNonNull(overrideMode);
    }

    public static CompileOptions fromConfig() {
        NeoHouseConfig config = NeoHouseConfig.get();
        return new CompileOptions(config.multiCallMode, config.recursionMode, config.globalCallMode, config.overrideMode);
    }
}
