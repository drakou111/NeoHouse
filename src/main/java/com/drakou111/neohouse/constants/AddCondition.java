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
    PORTAL_TYPE(true, "inPortalType", List.of()),
    PVP_ENABLED(true, "isPvpEnabled", List.of()),
    DAMAGE_CAUSE(true, "isDamageCause", List.of()),
    DAMAGE_AMOUNT(true, "isDamageAmount", List.of()),
    BLOCK_TYPE(true, "isBlockType", List.of()),
    IS_ITEM(true, "isItem", List.of()),
    FISHING_ENVIRONMENT(true, "isFishingEnvironment", List.of()),
    GROUP(true, "isGroup", List.of()),
    VARIABLE(false, "_", List.of()),
    PERMISSION(true, "hasPermission", List.of()),
    REGION(true, "inRegion", List.of()),
    HAS_ITEM(true, "hasItem", List.of()),
    DOING_PARKOUR(true, "doingParkour", List.of()),
    POTION(true, "hasPotion", List.of()),
    SNEAKING(true, "isSneaking", List.of()),
    FLYING(true, "isFlying", List.of()),
    HEALTH(true, "hasHealth", List.of()),
    MAX_HEALTH(true, "hasMaxHealth", List.of()),
    HUNGER(true, "hasHunger", List.of()),
    GAME_MODE(true, "hasGamemode", List.of()),
    PLACEHOLDER(false, "_", List.of()),
    TEAM(true, "hasTeam", List.of());

    //TODO, change params

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

