package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.GroupAction;
import com.drakou111.neohouse.exceptions.CompilingException;
import com.drakou111.neohouse.exceptions.PermissionException;
import com.drakou111.neohouse.utils.HousingCheck;
import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ScriptRunner {
    public static final ScriptRunner INSTANCE = new ScriptRunner(new NHCompiler());

    private final NHCompiler compiler;
    private Path lastScript;
    private List<String> lastHelperNames = List.of();
    private List<String> lastExtraVariables = List.of();

    public List<String> getLastHelperNames() { return lastHelperNames; }
    public List<String> getLastExtraVariables() { return lastExtraVariables; }

    public ScriptRunner(NHCompiler compiler) {
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

        Action scriptAction = compiler.compile(src, nhFile);

        for (var warning : compiler.getLastWarnings()) {
            MessageUtils.warningToChat(warning);
        }

        int helpers = compiler.getLastHelperCount();
        lastHelperNames = compiler.getLastHelperNames();
        lastExtraVariables = compiler.getLastExtraVariables();

        if (helpers > 0) {
            MessageUtils.warningToChatWithLink(
                    helpers + " extra helper function" + (helpers == 1 ? " was" : "s were") + " created to stay within Hypixel's action limits.",
                    "CLICK HERE", "/nh list functions");
        }
        if (!lastExtraVariables.isEmpty()) {
            MessageUtils.warningToChatWithLink(
                    lastExtraVariables.size() + " extra compiler variable" + (lastExtraVariables.size() == 1 ? " was" : "s were") + " created.",
                    "CLICK HERE", "/nh list variables");
        }

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
        return mc.player != null && mc.player.getAbilities().instabuild;
    }


}
