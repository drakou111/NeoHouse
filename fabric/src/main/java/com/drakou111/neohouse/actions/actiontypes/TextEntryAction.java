package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

/**
 * Types a value into the text prompt that clicking a Housing input opens. With Housing's chat inputs
 * enabled (which NeoHouse assumes) the menu closes and the value is sent in chat; the menu then
 * reopens. An anvil opening instead means chat inputs are off, which is reported as an error, unless
 * this input is known to always use an anvil.
 */
public class TextEntryAction extends Action {
    public static final String CHAT_INPUTS_HINT = "Housing opened an anvil where NeoHouse expected a chat prompt. "
            + "Turn on chat inputs in your Housing settings, then run the script again.";

    // Ticks to watch for an anvil after the menu closes: some servers close the menu just before opening it.
    private static final int ANVIL_GRACE_TICKS = 2;

    private enum Step { WAIT_FOR_PROMPT, ANVIL, REOPEN }

    private final String text;
    private final boolean anvilAllowed;
    private final WaitForAction waitForReopen = new WaitForAction(ContainerScreen.class, true);
    private Step step = Step.WAIT_FOR_PROMPT;
    private AnvilAction anvil;
    private int closedTicks = 0;
    private int waitedTicks = 0;

    public TextEntryAction(String text, boolean anvilAllowed) {
        this.text = text;
        this.anvilAllowed = anvilAllowed;
    }

    @Override
    public String getDebugName() {
        return "Enter text: " + this.text;
    }

    @Override
    public boolean run(Minecraft mc) {
        return switch (this.step) {
            case WAIT_FOR_PROMPT -> {
                waitForPrompt(mc);
                yield false;
            }
            case ANVIL -> {
                if (this.anvil.run(mc)) this.step = Step.REOPEN;
                yield false;
            }
            case REOPEN -> this.waitForReopen.run(mc);
        };
    }

    private void waitForPrompt(Minecraft mc) {
        if (++this.waitedTicks > NeoHouseConfig.get().actionTimeoutTicks)
            throw new InterruptActionException("Timed out waiting for the input prompt for '" + this.text + "'.");

        if (mc.screen instanceof AnvilScreen) {
            if (!this.anvilAllowed) throw new InterruptActionException(CHAT_INPUTS_HINT);
            this.anvil = new AnvilAction(this.text);
            this.step = Step.ANVIL;
            return;
        }
        if (mc.screen instanceof ContainerScreen) {
            this.closedTicks = 0;
            return;
        }
        if (++this.closedTicks < ANVIL_GRACE_TICKS) return;

        if (mc.player == null) throw new InterruptActionException("No player to type '" + this.text + "' in chat.");
        mc.player.connection.sendChat(this.text);
        this.step = Step.REOPEN;
    }
}
