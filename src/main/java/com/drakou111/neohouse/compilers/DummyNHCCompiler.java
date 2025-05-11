package com.drakou111.neohouse.compilers;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.ChatAction;
import com.drakou111.neohouse.exceptions.CompileException;

public class DummyNHCCompiler implements Compiler {
    @Override
    public Action compile(String source) throws CompileException {
        return new ChatAction("Executing NH script:\n" + source);
    }
}
