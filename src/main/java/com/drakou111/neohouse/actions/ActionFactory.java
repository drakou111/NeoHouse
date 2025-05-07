package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.State;
import com.drakou111.neohouse.actions.actiontypes.ChatAction;
import com.drakou111.neohouse.actions.actiontypes.CloseScreenAction;
import com.drakou111.neohouse.actions.actiontypes.SlotAction;
import com.drakou111.neohouse.actions.actiontypes.WaitForAction;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

public class ActionFactory {

    public static Action openMenuFromCommand() { return new ChatAction("/menu", State.NONE, State.HOUSING_MENU); }
    public static Action openFunctionsFromCommand() { return new ChatAction("/function", State.NONE, State.FUNCTIONS); }
    public static Action openEventsFromCommand() { return new ChatAction("/eventaction", State.NONE, State.EVENTS); }
    public static Action openCommandsFromCommand() { return new ChatAction("/command", State.NONE, State.COMMANDS); }
    public static Action openScoreboardFromCommand() {return new ChatAction("/scoreboard", State.NONE, State.SCOREBOARD); }
    public static Action openLayoutsFromCommand() {return new ChatAction("/layout", State.NONE, State.LAYOUTS); }
    public static Action openTeamsFromCommand() {return new ChatAction("/team", State.NONE, State.TEAMS); }
    public static Action openRegionsFromCommand() {return new ChatAction("/region", State.NONE, State.REGIONS); }
    public static Action openCustomMenuFromCommand() {return new ChatAction("/custommenu", State.NONE, State.CUSTOM_MENUS); }

    public static Action openSystemsInHousingMenu() { return new SlotAction(31, State.HOUSING_MENU, State.SYSTEMS); }
    public static Action openCustomMenusInSystems() { return new SlotAction(19, State.SYSTEMS, State.CUSTOM_MENUS); }
    public static Action waitForOpenContainer() { return new WaitForAction(ContainerScreen.class, true, null, null); }
    public static Action waitForCloseContainer() { return new WaitForAction(ContainerScreen.class, false, null, null); }
    public static Action closeAllMenu() { return new CloseScreenAction(null, State.NONE); }
}
