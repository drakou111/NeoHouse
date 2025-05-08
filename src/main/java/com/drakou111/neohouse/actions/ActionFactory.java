package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.actions.actiontypes.*;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

import java.util.ArrayList;
import java.util.List;

public class ActionFactory {

    /* === PRIMITIVES === */
    /* COMMANDS */
    public static Action chat_openMenu() { return new ChatAction("/menu"); }
    public static Action chat_openFunctions() { return new ChatAction("/function"); }
    public static Action chat_openEvents() { return new ChatAction("/eventaction"); }
    public static Action chat_openCommands() { return new ChatAction("/command"); }
    public static Action chat_openScoreboard() { return new ChatAction("/scoreboard"); }
    public static Action chat_openLayouts() { return new ChatAction("/layout"); }
    public static Action chat_openTeams() { return new ChatAction("/team"); }
    public static Action chat_openRegions() { return new ChatAction("/region"); }
    public static Action chat_openCustomMenu() { return new ChatAction("/custommenu"); }

    /* FROM MENU */
    public static Action housingMenu_openSystems() { return new SlotAction(31); }

    /* FROM SYSTEMS */
    public static Action systems_openCustomMenus() { return new SlotAction(19); }

    /* FUNCTIONS */
    public static Action function_findFunction(String name) {return new SearchAction(new int[]{ 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 }, name, null, 53, ContainerScreen.class, true); }
    public static Action function_addAction() { return new SlotAction(50); }

    /* CONDITIONAL */
    public static Action functionAddAction_addConditional() { return new SlotAction(10); }
    public static Action functionConditional_toggleMatchAny() { return new SlotAction(11); }

    public static Action functionConditional_openConditions() { return new SlotAction(10); }
    public static Action functionConditional_openIfs() { return new SlotAction(12); }
    public static Action functionConditional_openElses() { return new SlotAction(13); }

    public static Action functionConditional_exitConditions() { return new SlotAction(49); }
    public static Action functionConditional_exitIfs() { return new SlotAction(49); }
    public static Action functionConditional_exitElses() { return new SlotAction(49); }
    public static Action functionConditional_exit() { return new SlotAction(31); }


    /* AWAIT SERVER RESPONSE */
    public static Action wait_openAnvil() { return new WaitForAction(AnvilScreen.class, true); }
    public static Action wait_openContainer() { return new WaitForAction(ContainerScreen.class, true); }
    public static Action wait_closeContainer() { return new WaitForAction(ContainerScreen.class, false); }

    /* ANVIL */
    public static Action anvil_write(String text) { return new AnvilAction(text); }

    /* ANY */
    public static Action any_closeAllMenu() { return new CloseScreenAction(); }



    /* === GROUPS === */

    /* MOVEMENT */
    public static Action goto_function(String name, boolean hasUIOpened) {
        List<Action> list = new ArrayList<>();
        if (hasUIOpened) {
            list.add(any_closeAllMenu());
            list.add(wait_closeContainer());
        }
        list.add(chat_openFunctions());
        list.add(wait_openContainer());
        list.add(function_findFunction(name));
        list.add(wait_openContainer());
        return new GroupAction(list);
    }

    public static Action group_addActionConditional(boolean matchAny, List<Action> conditions, List<Action> ifs, List<Action> elses) {
        List<Action> list = new ArrayList<>();
        list.add(function_addAction());
        list.add(wait_openContainer());
        list.add(functionAddAction_addConditional());
        list.add(wait_openContainer());
        if (matchAny) {
            list.add(functionConditional_toggleMatchAny());
            list.add(wait_openContainer());
        }
        if (conditions != null && !conditions.isEmpty()) {
            list.add(functionConditional_openConditions());
            for (Action action : conditions) {
                list.add(action);
                list.add(wait_openContainer());
            }
            list.add(functionConditional_exitConditions());
        }
        if (ifs != null && !ifs.isEmpty()) {
            list.add(functionConditional_openIfs());
            for (Action action : ifs) {
                list.add(action);
                list.add(wait_openContainer());
            }
            list.add(functionConditional_exitIfs());
        }
        if (elses != null && !elses.isEmpty()) {
            list.add(functionConditional_openElses());
            for (Action action : elses) {
                list.add(action);
                list.add(wait_openContainer());
            }
            list.add(functionConditional_exitElses());
        }
        list.add(functionConditional_exit());

        return new GroupAction(list);
    }
}
