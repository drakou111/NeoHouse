package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.CompilingException;

import java.nio.file.Path;

public interface Compiler {
    Action compile(String source) throws CompilingException;

    default Action compile(String source, Path scriptsRoot) throws CompilingException {
        return compile(source);
    }
}
