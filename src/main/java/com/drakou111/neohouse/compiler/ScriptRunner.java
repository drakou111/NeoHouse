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
    private final Compiler compiler;

    public ScriptRunner(Compiler compiler) {
        this.compiler = compiler;
    }

    public void runScript(Path nhFile) throws CompilingException, PermissionException {
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
        if (!isPlayerCreative() && sourceNeedsCreativeMode(preprocessed)) {
            throw new PermissionException("This script uses item actions (giveItem / removeItem / dropItem). You must be in creative mode to run it.");
        }
        Action scriptAction = compiler.compile(preprocessed);
        SequenceManager.startAction(new GroupAction(List.of(scriptAction)));
    }

    public void runScript(String source) throws CompilingException, PermissionException {
        if (!HousingCheck.isInHousing()) {
            throw new PermissionException("You must be in Hypixel Housing to run scripts.");
        }
        if (!isPlayerCreative() && sourceNeedsCreativeMode(source)) {
            throw new PermissionException("This script uses item actions (giveItem / removeItem / dropItem). You must be in creative mode to run it.");
        }
        Action scriptAction = compiler.compile(source);
        SequenceManager.startAction(new GroupAction(List.of(scriptAction)));
    }

    private static boolean isPlayerCreative() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getAbilities().instabuild;
    }

    private static boolean sourceNeedsCreativeMode(String source) {
        for (AddAction action : AddAction.values()) {
            if (action.params.stream().anyMatch(p -> p.kind() == ActionParamKind.ITEM_LITERAL)) {
                if (source.contains(action.name + "(")) {
                    return true;
                }
            }
        }
        return false;
    }
}
