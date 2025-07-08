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
    CONDITIONAL(15, false, "_", List.of()),
    CANCEL_EVENT(1, false, "_", List.of()),
    CHANGE_GROUP(1, true, "setGroup", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL)),
    KILL(1, true, "kill", List.of()),
    FULL_HEAL(1, true, "heal", List.of()),
    TITLE(1, true, "title", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.LONG_LITERAL, ActionParamKind.LONG_LITERAL, ActionParamKind.LONG_LITERAL)),
    ACTION_BAR(1, true, "actionBar", List.of(ActionParamKind.STRING_LITERAL)),
    CLEAR_INVENTORY(1, true, "clearInventory", List.of()),
    MAX_HEALTH(1, true, "setMaxHealth", List.of(ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL)),
    PARKOUR_CHECKPOINT(1, true, "triggerParkourCheckpoint", List.of()),
    GIVE_ITEM(1, true, "giveItem", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL)),
    REMOVE_ITEM(1, true, "removeItem", List.of(ActionParamKind.STRING_LITERAL)),
    CHAT_MESSAGE(1, true, "message", List.of(ActionParamKind.STRING_LITERAL)),
    APPLY_POTION(1, true, "applyPotion", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.LONG_LITERAL, ActionParamKind.LONG_LITERAL, ActionParamKind.BOOL_LITERAL, ActionParamKind.BOOL_LITERAL)),
    CLEAR_POTION(1, true, "clearPotion", List.of()),
    GIVE_XP(1, true, "giveXp", List.of(ActionParamKind.LONG_LITERAL)),
    SEND_LOBBY(1, true, "sendLobby", List.of(ActionParamKind.STRING_LITERAL)),
    VARIABLE(1, false, "_", List.of()),
    TELEPORT(1, true, "teleport", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL)),
    FAIL_PARKOUR(1, true, "failParkour", List.of(ActionParamKind.STRING_LITERAL)),
    PLAY_SOUND(1, true, "playSound", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.DOUBLE_LITERAL, ActionParamKind.DOUBLE_LITERAL, ActionParamKind.STRING_LITERAL)),
    COMPASS_TARGET(1, true, "setCompassTarget", List.of(ActionParamKind.STRING_LITERAL)),
    SET_GAME_MODE(1, true, "setGamemode", List.of(ActionParamKind.STRING_LITERAL)),
    CHANGE_HEALTH(1, true, "setHealth", List.of(ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.STRING_LITERAL)),
    CHANGE_HUNGER(1, true, "setHunger", List.of(ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.STRING_LITERAL)),
    RANDOM(1, false, "_", List.of()),
    TRIGGER_FUNCTION(1, false, "_", List.of()),
    APPLY_LAYOUT(1, true, "applyLayout", List.of(ActionParamKind.STRING_LITERAL)),
    EXIT(1, false, "_", List.of()),
    ENCHANT_HELD_ITEM(1, true, "enchantItem", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.LONG_LITERAL)),
    PAUSE(1, true, "pause", List.of(ActionParamKind.LONG_LITERAL)),
    SET_TEAM(1, true, "setTeam", List.of(ActionParamKind.STRING_LITERAL)),
    DISPLAY_MENU(1, true, "displayMenu", List.of(ActionParamKind.STRING_LITERAL)),
    DROP_ITEM(1, true, "dropItem", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL, ActionParamKind.BOOL_LITERAL, ActionParamKind.BOOL_LITERAL, ActionParamKind.BOOL_LITERAL)),
    CHANGE_VELOCITY(1, true, "changeVelocity", List.of(ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.NORMAL_EXPRESSION)),
    LAUNCH_TO_TARGET(1, true, "launchToTarget", List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.NORMAL_EXPRESSION)),
    SET_WEATHER(1, true, "setWeather", List.of(ActionParamKind.STRING_LITERAL)),
    SET_TIME(1, true, "setTime", List.of(ActionParamKind.STRING_LITERAL)),
    NAME_TAG_DISPLAY(1, true, "setNameTagDisplay", List.of(ActionParamKind.BOOL_LITERAL));

    public final int maxCount;
    public final boolean canBeUsed;
    public final String name;
    public final List<ActionParamKind> params;

    AddAction(int maxCount, boolean canBeUsed, String name, List<ActionParamKind> params) {
        this.maxCount = maxCount;
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
