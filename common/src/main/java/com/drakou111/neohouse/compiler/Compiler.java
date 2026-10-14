package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.CompilingException;

import java.nio.file.Path;

public interface Compiler {
    Action compile(String source) throws CompilingException;

    /** @param sourceFile the script's file, used to resolve its imports; {@code null} for an unsaved script */
    default Action compile(String source, Path sourceFile) throws CompilingException {
        return compile(source);
    }
}
