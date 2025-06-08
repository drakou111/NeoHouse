package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.compiler.enums.ActionParamKind;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/*********************\
 *   ADD CONDITION   *
 \********************/

public enum AddCondition {
    PORTAL_TYPE(true, "inPortalType", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL)),
    PVP_ENABLED(true, "isPvpEnabled", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL)),
    DAMAGE_CAUSE(true, "isDamageCause", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL)),
    DAMAGE_AMOUNT(true, "isDamageAmount", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.NORMAL_EXPRESSION)),
    BLOCK_TYPE(true, "isBlockType", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL)),
    IS_ITEM(true, "isItem", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL)),
    FISHING_ENVIRONMENT(true, "isFishingEnvironment", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL)),
    GROUP(true, "isGroup", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL)),
    VARIABLE(false, "_", List.of()),
    PERMISSION(true, "hasPermission", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL)),
    REGION(true, "inRegion", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL)),
    HAS_ITEM(true, "hasItem", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL)),
    DOING_PARKOUR(true, "doingParkour", List.of(ActionParamKind.BOOL_LITERAL)),
    POTION(true, "hasPotion", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL)),
    SNEAKING(true, "isSneaking", List.of(ActionParamKind.BOOL_LITERAL)),
    FLYING(true, "isFlying", List.of(ActionParamKind.BOOL_LITERAL)),
    HEALTH(true, "hasHealth", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.NORMAL_EXPRESSION)),
    MAX_HEALTH(true, "hasMaxHealth", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.NORMAL_EXPRESSION)),
    HUNGER(true, "hasHunger", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.NORMAL_EXPRESSION)),
    GAME_MODE(true, "hasGamemode", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL)),
    PLACEHOLDER(false, "_", List.of(ActionParamKind.BOOL_LITERAL)),
    TEAM(true, "hasTeam", List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL));

    public final boolean canBeUsed;
    public final String name;
    public final List<ActionParamKind> params;

    AddCondition(boolean canBeUsed, String name, List<ActionParamKind> params) {
        this.canBeUsed = canBeUsed;
        this.name = name;
        this.params = params;
    }

    public static final Map<String, AddCondition> BY_NAME = Arrays.stream(values()).filter(a -> a.canBeUsed).collect(Collectors.toMap(a -> a.name, Function.identity()));

    public static AddCondition searchByName(String name) {
        return BY_NAME.get(name);
    }

    public static AddCondition requireByName(String name) {
        AddCondition a = BY_NAME.get(name);
        if (a == null) throw new IllegalArgumentException("Unknown AddCondition name: " + name);
        return a;
    }
}

