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

        // Fast pre-compile check: scan source text before spending time on compilation
        if (!isPlayerCreative() && sourceNeedsCreativeMode(preprocessed)) {
            throw new PermissionException("This script uses item actions (giveItem / removeItem / dropItem / menus). You must be in creative mode to run it.");
        }

        Action scriptAction = compiler.compile(preprocessed);

        // Post-compile safety check using the fully-built action tree
        if (!isPlayerCreative() && scriptAction.requiresCreative()) {
            throw new PermissionException("This script uses item actions (giveItem / removeItem / dropItem / menus). You must be in creative mode to run it.");
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) mc.setScreen(null);
        SequenceManager.startAction(new GroupAction(List.of(scriptAction)));
    }

    private static boolean isPlayerCreative() {
        Minecraft mc = Minecraft.getInstance();
        // Use instabuild (not isCreative()) — on Hypixel Housing, LocalPlayer.isCreative()
        // checks the server-reported GameType (always CREATIVE for the owner) rather than
        // the actual ability flag. instabuild is what determines whether the player can
        // actually use creative-mode actions and is correctly set to false in survival.
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
        // Menu slot declarations always require creative (items must be placed in the menu grid)
        return source.matches("(?s).*slot\\d+\\s*:\\s*\\(.*");
    }

}
