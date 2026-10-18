package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;
import com.drakou111.neohouse.config.UndeclaredVariableMode;
import com.drakou111.neohouse.config.UnknownCallMode;
import com.drakou111.neohouse.config.UnsetVariableMode;

import java.util.Objects;

/**
 * The user-configurable compiler switches, passed in explicitly so the compiler never reads global config.
 * {@code forbidVarType} rejects the var type everywhere, including the implicit var of an undeclared variable.
 */
public record CompileOptions(MultiCallMode multiCallMode, RecursionMode recursionMode,
                             GlobalCallMode globalCallMode, OverrideMode overrideMode,
                             UndeclaredVariableMode undeclaredVariableMode, boolean forbidVarType,
                             UnsetVariableMode unsetVariableMode, UnknownCallMode unknownCallMode) {

    public CompileOptions {
        Objects.requireNonNull(multiCallMode);
        Objects.requireNonNull(recursionMode);
        Objects.requireNonNull(globalCallMode);
        Objects.requireNonNull(overrideMode);
        Objects.requireNonNull(undeclaredVariableMode);
        Objects.requireNonNull(unsetVariableMode);
        Objects.requireNonNull(unknownCallMode);
    }

    /** Calls to functions and menus the script doesn't define are errors, as before that setting existed. */
    public CompileOptions(MultiCallMode multiCallMode, RecursionMode recursionMode,
                          GlobalCallMode globalCallMode, OverrideMode overrideMode,
                          UndeclaredVariableMode undeclaredVariableMode, boolean forbidVarType,
                          UnsetVariableMode unsetVariableMode) {
        this(multiCallMode, recursionMode, globalCallMode, overrideMode, undeclaredVariableMode, forbidVarType,
                unsetVariableMode, UnknownCallMode.ERROR);
    }

    /** No check for conditions on never-set variables, as before that setting existed. */
    public CompileOptions(MultiCallMode multiCallMode, RecursionMode recursionMode,
                          GlobalCallMode globalCallMode, OverrideMode overrideMode,
                          UndeclaredVariableMode undeclaredVariableMode, boolean forbidVarType) {
        this(multiCallMode, recursionMode, globalCallMode, overrideMode, undeclaredVariableMode, forbidVarType, UnsetVariableMode.OFF);
    }

    /** Undeclared variables allowed and var permitted, as before those settings existed. */
    public CompileOptions(MultiCallMode multiCallMode, RecursionMode recursionMode,
                          GlobalCallMode globalCallMode, OverrideMode overrideMode) {
        this(multiCallMode, recursionMode, globalCallMode, overrideMode, UndeclaredVariableMode.ALLOW, false);
    }

    public static CompileOptions fromConfig() {
        NeoHouseConfig config = NeoHouseConfig.get();
        return new CompileOptions(config.multiCallMode, config.recursionMode, config.globalCallMode, config.overrideMode,
                config.undeclaredVariableMode, config.forbidVarType, config.unsetVariableMode, config.unknownCallMode);
    }
}
