package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.GroupAction;
import com.drakou111.neohouse.exceptions.CompilingException;
import com.drakou111.neohouse.exceptions.PermissionException;
import com.drakou111.neohouse.utils.HousingCheck;
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

    public void runScript(Path nhFile) throws CompilingException, PermissionException {
        try {
            String src = Files.readString(nhFile, StandardCharsets.UTF_8);
            runScript(src);
        } catch (IOException e) {
            throw new CompilingException(null, "Failed reading script: " + nhFile);
        }
    }

    public void runScript(String source) throws CompilingException, PermissionException {
        if (!HousingCheck.isInHousing()) {
            throw new PermissionException("You must be in Hypixel Housing to run scripts.");
        }
        Action scriptAction = compiler.compile(source);
        SequenceManager.startAction(new GroupAction(List.of(scriptAction)));
    }
}
