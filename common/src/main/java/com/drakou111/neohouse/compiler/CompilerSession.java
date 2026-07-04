package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.exceptions.CompilingException;

import java.nio.file.Path;
import java.util.List;

public class CompilerSession {
    private final NHCompiler compiler = new NHCompiler();
    private Action lastGoodResult = null;

    public Action getLastGoodResult() {
        return lastGoodResult;
    }

    public List<CompileWarning> getLastWarnings() {
        return compiler.getLastWarnings();
    }

    public Action compile(String source, Path scriptsRoot) throws CompilingException {
        Action result = compiler.compile(source, scriptsRoot);
        lastGoodResult = result;
        return result;
    }

    public Action compile(String source) throws CompilingException {
        return compile(source, null);
    }

    public boolean hasLastGoodResult() {
        return lastGoodResult != null;
    }
}
