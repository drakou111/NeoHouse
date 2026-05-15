package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.CompilingException;

import java.nio.file.Path;

/**
 * Wraps NHCompiler and retains the last successfully compiled result.
 * Use a single instance per editor session to share "last known good" state.
 */
public class CompilerSession {
    private final NHCompiler compiler = new NHCompiler();
    private Action lastGoodResult = null;

    public Action getLastGoodResult() {
        return lastGoodResult;
    }

    /**
     * Compiles the source. On success, updates the last-good result and returns it.
     * On failure, throws without touching the cached result.
     */
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
