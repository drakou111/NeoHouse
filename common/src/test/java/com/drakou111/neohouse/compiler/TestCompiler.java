package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;

import java.nio.file.Path;

/** Shared entry point for compiler tests: runs the front end with fixed default options. */
final class TestCompiler {
    static final CompileOptions DEFAULT_OPTIONS = new CompileOptions(
            MultiCallMode.FIX, RecursionMode.WARN, GlobalCallMode.ERROR, OverrideMode.ALWAYS);

    private TestCompiler() {}

    static MainNode analyze(String source) {
        return analyze(source, null, DEFAULT_OPTIONS);
    }

    static MainNode analyze(String source, Path scriptsRoot, CompileOptions options) {
        return new NHCompiler().analyze(source, scriptsRoot, options);
    }
}
