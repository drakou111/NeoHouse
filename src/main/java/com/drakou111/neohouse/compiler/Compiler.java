package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.CompilingException;

public interface Compiler {
    Action compile(String source) throws CompilingException;
}
