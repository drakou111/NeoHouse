package com.drakou111.neohouse.compilers;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.CompileException;

public interface Compiler {
    Action compile(String source) throws CompileException;
}
