package com.drakou111.neohouse.compilers;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.GroupAction;
import com.drakou111.neohouse.exceptions.CompileException;
import com.drakou111.neohouse.utils.SequenceManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ScriptRunner {
    private final Compiler compiler;

    public ScriptRunner(Compiler compiler) {
        this.compiler = compiler;
    }

    /**
     * Load the file, compile it, and run the resulting action sequence
     */
    public void runScript(Path nhFile) throws CompileException {
        try {
            String src = Files.readString(nhFile, StandardCharsets.UTF_8);
            runScript(src);
        } catch (IOException e) {
            throw new CompileException("Failed reading script: " + nhFile);
        }
    }

    /** Directly compile & run from raw text */
    public void runScript(String source) throws CompileException {
        Action scriptAction = compiler.compile(source);
        SequenceManager.startAction(new GroupAction(List.of(scriptAction)));
    }
}
