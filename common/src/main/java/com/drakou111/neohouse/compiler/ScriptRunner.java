package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.GroupAction;
import com.drakou111.neohouse.compiler.enums.ActionParamKind;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.exceptions.CompilingException;
import com.drakou111.neohouse.exceptions.PermissionException;
import com.drakou111.neohouse.utils.HousingCheck;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ScriptRunner {
    public static final ScriptRunner INSTANCE = new ScriptRunner(new NHCompiler());

    private final Compiler compiler;
    private Path lastScript;

    public ScriptRunner(Compiler compiler) {
        this.compiler = compiler;
    }

    public Path getLastScript() {
        return lastScript;
    }

    public void runScript(Path nhFile) throws CompilingException, PermissionException {
        lastScript = nhFile;
        if (!HousingCheck.isInHousing()) {
            throw new PermissionException("You must be in Hypixel Housing to run scripts.");
        }
        String src;
        try {
            src = Files.readString(nhFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CompilingException(null, "Failed reading script: " + nhFile);
        }

        // Preprocess before the creative check so imported files are included in the scan
        String preprocessed = new Preprocessor(nhFile.getParent()).process(src);
        Action scriptAction = compiler.compile(preprocessed);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isCreative() && scriptAction.requiresCreative()) {
            throw new PermissionException("This script uses item actions. You must be in creative mode to run it.");
        }

        SequenceManager.startAction(new GroupAction(List.of(scriptAction)));
    }
}
