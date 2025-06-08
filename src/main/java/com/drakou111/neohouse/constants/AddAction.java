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
    CONDITIONAL(false, "_", List.of(ActionParamKind.DOUBLE_LITERAL)),
    CANCEL_EVENT(false, "_", List.of(ActionParamKind.DOUBLE_LITERAL)),
    CHANGE_GROUP(true, "setGroup", List.of(ActionParamKind.DOUBLE_LITERAL)),
    KILL(true, "kill", List.of(ActionParamKind.DOUBLE_LITERAL)),
    FULL_HEAL(true, "heal", List.of(ActionParamKind.DOUBLE_LITERAL)),
    TITLE(true, "showTitle", List.of(ActionParamKind.DOUBLE_LITERAL)),
    ACTION_BAR(true, "actionBar", List.of(ActionParamKind.DOUBLE_LITERAL)),
    CLEAR_INVENTORY(true, "clearInventory", List.of(ActionParamKind.DOUBLE_LITERAL)),
    MAX_HEALTH(true, "setMaxHealth", List.of(ActionParamKind.DOUBLE_LITERAL)),
    PARKOUR_CHECKPOINT(true, "triggerParkourCheckpoint", List.of(ActionParamKind.DOUBLE_LITERAL)),
    GIVE_ITEM(true, "giveItem", List.of(ActionParamKind.DOUBLE_LITERAL)),
    REMOVE_ITEM(true, "removeItem", List.of(ActionParamKind.DOUBLE_LITERAL)),
    CHAT_MESSAGE(true, "message", List.of(ActionParamKind.DOUBLE_LITERAL)),
    APPLY_POTION(true, "applyPotion", List.of(ActionParamKind.DOUBLE_LITERAL)),
    CLEAR_POTION(true, "clearPotion", List.of(ActionParamKind.DOUBLE_LITERAL)),
    GIVE_XP(true, "giveXp", List.of(ActionParamKind.DOUBLE_LITERAL)),
    SEND_LOBBY(true, "sendLobby", List.of(ActionParamKind.DOUBLE_LITERAL)),
    VARIABLE(false, "_", List.of(ActionParamKind.DOUBLE_LITERAL)),
    TELEPORT(true, "teleport", List.of(ActionParamKind.DOUBLE_LITERAL)),
    FAIL_PARKOUR(true, "failParkour", List.of(ActionParamKind.DOUBLE_LITERAL)),
    PLAY_SOUND(true, "playSound", List.of(ActionParamKind.DOUBLE_LITERAL)),
    COMPASS_TARGET(true, "setCompassTarget", List.of(ActionParamKind.DOUBLE_LITERAL)),
    SET_GAME_MODE(true, "setGamemode", List.of(ActionParamKind.DOUBLE_LITERAL)),
    CHANGE_HEALTH(true, "setHealth", List.of(ActionParamKind.DOUBLE_LITERAL)),
    CHANGE_HUNGER(true, "setHunger", List.of(ActionParamKind.DOUBLE_LITERAL)),
    RANDOM(false, "_", List.of(ActionParamKind.DOUBLE_LITERAL)),
    TRIGGER_FUNCTION(false, "_", List.of(ActionParamKind.DOUBLE_LITERAL)),
    APPLY_LAYOUT(true, "applyLayout", List.of(ActionParamKind.DOUBLE_LITERAL)),
    EXIT(false, "_", List.of(ActionParamKind.DOUBLE_LITERAL)),
    ENCHANT_HELD_ITEM(true, "enchantItem", List.of(ActionParamKind.DOUBLE_LITERAL)),
    PAUSE(true, "pause", List.of(ActionParamKind.DOUBLE_LITERAL)),
    SET_TEAM(true, "setTeam", List.of(ActionParamKind.DOUBLE_LITERAL)),
    DISPLAY_MENU(true, "displayMenu", List.of(ActionParamKind.DOUBLE_LITERAL)),
    DROP_ITEM(true, "dropItem", List.of(ActionParamKind.DOUBLE_LITERAL)),
    CHANGE_VELOCITY(true, "changeVelocity", List.of(ActionParamKind.DOUBLE_LITERAL)),
    LAUNCH_TO_TARGET(true, "launchToTarget", List.of(ActionParamKind.DOUBLE_LITERAL)),
    SET_WEATHER(true, "setWeather", List.of(ActionParamKind.DOUBLE_LITERAL)),
    SET_TIME(true, "setTime", List.of(ActionParamKind.DOUBLE_LITERAL)),
    NAME_TAG_DISPLAY(true, "setNameTagDisplay", List.of(ActionParamKind.DOUBLE_LITERAL));

    //TODO, change params

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
