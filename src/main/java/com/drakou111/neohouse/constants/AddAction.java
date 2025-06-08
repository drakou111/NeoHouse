package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.compiler.enums.ActionParamKind;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/******************\
 *   ADD ACTION   *
\******************/

public enum AddAction {
    CONDITIONAL(false, "_", List.of()),
    CANCEL_EVENT(false, "_", List.of()),
    CHANGE_GROUP(true, "setGroup", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL)),
    KILL(true, "kill", List.of()),
    FULL_HEAL(true, "heal", List.of()),
    TITLE(true, "title", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.LONG_LITERAL, ActionParamKind.LONG_LITERAL, ActionParamKind.LONG_LITERAL)),
    ACTION_BAR(true, "actionBar", List.of(ActionParamKind.STRING_LITERAL)),
    CLEAR_INVENTORY(true, "clearInventory", List.of()),
    MAX_HEALTH(true, "setMaxHealth", List.of(ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL)),
    PARKOUR_CHECKPOINT(true, "triggerParkourCheckpoint", List.of()),
    GIVE_ITEM(true, "giveItem", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL)),
    REMOVE_ITEM(true, "removeItem", List.of(ActionParamKind.STRING_LITERAL)),
    CHAT_MESSAGE(true, "message", List.of(ActionParamKind.STRING_LITERAL)),
    APPLY_POTION(true, "applyPotion", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.LONG_LITERAL, ActionParamKind.LONG_LITERAL, ActionParamKind.BOOL_LITERAL, ActionParamKind.BOOL_LITERAL)),
    CLEAR_POTION(true, "clearPotion", List.of()),
    GIVE_XP(true, "giveXp", List.of(ActionParamKind.LONG_LITERAL)),
    SEND_LOBBY(true, "sendLobby", List.of(ActionParamKind.STRING_LITERAL)),
    VARIABLE(false, "_", List.of()),
    TELEPORT(true, "teleport", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL)),
    FAIL_PARKOUR(true, "failParkour", List.of(ActionParamKind.STRING_LITERAL)),
    PLAY_SOUND(true, "playSound", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.DOUBLE_LITERAL, ActionParamKind.DOUBLE_LITERAL, ActionParamKind.STRING_LITERAL)),
    COMPASS_TARGET(true, "setCompassTarget", List.of(ActionParamKind.STRING_LITERAL)),
    SET_GAME_MODE(true, "setGamemode", List.of(ActionParamKind.STRING_LITERAL)),
    CHANGE_HEALTH(true, "setHealth", List.of(ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.STRING_LITERAL)),
    CHANGE_HUNGER(true, "setHunger", List.of(ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.STRING_LITERAL)),
    RANDOM(false, "_", List.of()),
    TRIGGER_FUNCTION(false, "_", List.of()),
    APPLY_LAYOUT(true, "applyLayout", List.of(ActionParamKind.STRING_LITERAL)),
    EXIT(false, "_", List.of()),
    ENCHANT_HELD_ITEM(true, "enchantItem", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.LONG_LITERAL)),
    PAUSE(true, "pause", List.of(ActionParamKind.LONG_LITERAL)),
    SET_TEAM(true, "setTeam", List.of(ActionParamKind.STRING_LITERAL)),
    DISPLAY_MENU(true, "displayMenu", List.of(ActionParamKind.STRING_LITERAL)),
    DROP_ITEM(true, "dropItem", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL, ActionParamKind.BOOL_LITERAL, ActionParamKind.BOOL_LITERAL, ActionParamKind.BOOL_LITERAL)),
    CHANGE_VELOCITY(true, "changeVelocity", List.of(ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.NORMAL_EXPRESSION)),
    LAUNCH_TO_TARGET(true, "launchToTarget", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.NORMAL_EXPRESSION)),
    SET_WEATHER(true, "setWeather", List.of(ActionParamKind.STRING_LITERAL)),
    SET_TIME(true, "setTime", List.of(ActionParamKind.STRING_LITERAL)),
    NAME_TAG_DISPLAY(true, "setNameTagDisplay", List.of(ActionParamKind.BOOL_LITERAL));

    public final boolean canBeUsed;
    public final String name;
    public final List<ActionParamKind> params;

    AddAction(boolean canBeUsed, String name, List<ActionParamKind> params) {
        this.canBeUsed = canBeUsed;
        this.name = name;
        this.params = params;
    }

    public static final Map<String, AddAction> BY_NAME = Arrays.stream(values()).filter(a -> a.canBeUsed).collect(Collectors.toMap(a -> a.name, Function.identity()));

    public static AddAction searchByName(String name) {
        return BY_NAME.get(name);
    }

    public static AddAction requireByName(String name) {
        AddAction a = BY_NAME.get(name);
        if (a == null) throw new IllegalArgumentException("Unknown AddAction name: " + name);
        return a;
    }
}
