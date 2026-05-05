package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.compiler.enums.Param;
import com.drakou111.neohouse.constants.conditions.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/*********************\
 *   ADD CONDITION   *
 \********************/

public enum AddCondition {
    PORTAL_TYPE(true, "inPortalType",
                List.of(Param.BOOL.withName(PortalTypeInputs.INVERT), Param.enumOf(PortalType.class).withName(PortalTypeInputs.PORTAL_TYPE)),
                "True if the player is in the given portal type.", "end_portal_frame", "Portal Type"),
    PVP_ENABLED(true, "isPvpEnabled",
                List.of(Param.BOOL.withName(PvpEnabledInputs.INVERT)),
                "True if PvP is currently enabled in the housing.", "iron_sword", "PvP Enabled"),
    DAMAGE_CAUSE(true, "isDamageCause",
                 List.of(Param.BOOL.withName(DamageCauseInputs.INVERT), Param.enumOf(DamageCause.class).withName(DamageCauseInputs.CAUSE)),
                 "True if the damage cause matches the given value.", "diamond_sword", "Damage Cause"),
    DAMAGE_AMOUNT(true, "isDamageAmount",
                  List.of(Param.BOOL.withName(DamageAmountInputs.INVERT), Param.enumOf(Comparator.class).withName(DamageAmountInputs.COMPARATOR), Param.EXPR.withName(DamageAmountInputs.COMPARE_VALUE)),
                  "True if the damage amount matches the comparator.", "golden_sword", "Damage Amount"),
    BLOCK_TYPE(true, "isBlockType",
               List.of(Param.BOOL.withName(BlockTypeInputs.INVERT), Param.ITEM.withName(BlockTypeInputs.ITEM), Param.BOOL.withName(BlockTypeInputs.MATCH_TYPE_ONLY)),
               "True if the broken block matches the given type.", "grass_block", "Block Type"),
    IS_ITEM(true, "isItem",
            List.of(Param.BOOL.withName(IsItemInputs.INVERT), Param.ITEM.withName(IsItemInputs.ITEM), Param.enumOf(WhatCheck.class).withName(IsItemInputs.WHAT_TO_CHECK), Param.enumOf(WhereCheck.class).withName(IsItemInputs.WHERE_TO_CHECK), Param.enumOf(AmountCheck.class).withName(IsItemInputs.REQUIRED_AMOUNT)),
            "True if the item matches the criteria.", "chest", "Is Item"),
    FISHING_ENVIRONMENT(true, "isFishingEnvironment",
                        List.of(Param.BOOL.withName(FishingEnvironmentInputs.INVERT), Param.enumOf(FishingEnvironment.class).withName(FishingEnvironmentInputs.ENVIRONMENT)),
                        "True if the player is fishing in the given environment.", "water_bucket", "Fishing Environment"),
    GROUP(true, "isGroup",
          List.of(Param.BOOL.withName(GroupInputs.INVERT), Param.STRING.withName(GroupInputs.REQUIRED_GROUP), Param.BOOL.withName(GroupInputs.INCLUDE_HIGHER)),
          "True if the player is in the given group.", "player_head", "Required Group"),
    VARIABLE(false, "_", List.of(), "", "feather", "Variable Requirement"),
    PERMISSION(true, "hasPermission",
               List.of(Param.BOOL.withName(PermissionInputs.INVERT), Param.enumOf(Permission.class).withName(PermissionInputs.PERMISSION)),
               "True if the player has the given permission.", "filled_map", "Required Permission"),
    REGION(true, "inRegion",
           List.of(Param.BOOL.withName(RegionInputs.INVERT), Param.STRING.withName(RegionInputs.REGION)),
           "True if the player is inside the named region.", "grass_block", "Within Region"),
    HAS_ITEM(true, "hasItem",
             List.of(Param.BOOL.withName(HasItemInputs.INVERT), Param.ITEM.withName(HasItemInputs.ITEM), Param.enumOf(WhatCheck.class).withName(HasItemInputs.WHAT_TO_CHECK), Param.enumOf(WhereCheck.class).withName(HasItemInputs.WHERE_TO_CHECK), Param.enumOf(AmountCheck.class).withName(HasItemInputs.REQUIRED_AMOUNT)),
             "True if the player has an item matching criteria in their inventory.", "chest", "Has Item"),
    DOING_PARKOUR(true, "doingParkour",
                  List.of(Param.BOOL.withName(DoingParkourInputs.INVERT)),
                  "True if the player is currently on a parkour run.", "light_weighted_pressure_plate", "Doing Parkour"),
    POTION(true, "hasPotion",
           List.of(Param.BOOL.withName(EffectInputs.INVERT), Param.enumOf(Potion.class).withName(EffectInputs.EFFECT)),
           "True if the player has the given potion effect active.", "potion", "Has Potion Effect"),
    SNEAKING(true, "isSneaking",
             List.of(Param.BOOL.withName(SneakingInputs.INVERT)),
             "True if the player is currently sneaking.", "hay_block", "Player Sneaking"),
    FLYING(true, "isFlying",
           List.of(Param.BOOL.withName(FlyingInputs.INVERT)),
           "True if the player is currently flying.", "feather", "Player Flying"),
    HEALTH(true, "hasHealth",
           List.of(Param.BOOL.withName(HealthInputs.INVERT), Param.enumOf(Comparator.class).withName(HealthInputs.COMPARATOR), Param.EXPR.withName(HealthInputs.COMPARE_VALUE)),
           "True if the player's health matches the comparator.", "apple", "Player Health"),
    MAX_HEALTH(true, "hasMaxHealth",
               List.of(Param.BOOL.withName(MaxHealthInputs.INVERT), Param.enumOf(Comparator.class).withName(MaxHealthInputs.COMPARATOR), Param.EXPR.withName(MaxHealthInputs.COMPARE_VALUE)),
               "True if the player's max health matches the comparator.", "golden_apple", "Max Player Health"),
    HUNGER(true, "hasHunger",
           List.of(Param.BOOL.withName(HungerInputs.INVERT), Param.enumOf(Comparator.class).withName(HungerInputs.COMPARATOR), Param.EXPR.withName(HungerInputs.COMPARE_VALUE)),
           "True if the player's hunger matches the comparator.", "cooked_beef", "Player Hunger"),
    GAME_MODE(true, "hasGamemode",
              List.of(Param.BOOL.withName(GamemodeInputs.INVERT), Param.enumOf(Gamemode.class).withName(GamemodeInputs.GAMEMODE)),
              "True if the player is in the given game mode.", "daylight_detector", "Required Gamemode"),
    PLACEHOLDER(false, "_", List.of(Param.BOOL.withName(PlaceholderInputs.INVERT), Param.enumOf(Placeholder.class).withName(PlaceholderInputs.PLACEHOLDER), Param.enumOf(Comparator.class).withName(PlaceholderInputs.COMPARATOR), Param.EXPR.withName(PlaceholderInputs.COMPARE_VALUE)), "", "sign", "Placeholder Number Requirement"),
    TEAM(true, "hasTeam",
         List.of(Param.BOOL.withName(TeamInputs.INVERT), Param.STRING.withName(TeamInputs.REQUIRED_TEAM)),
         "True if the player is in the given team.", "player_head", "Required Team");

    public static final Map<String, AddCondition> BY_NAME = Arrays.stream(values())
                                                                  .filter(a -> a.canBeUsed)
                                                                  .collect(Collectors.toMap(a -> a.name, Function.identity()));
    public final boolean canBeUsed;
    public final String name;
    public final List<Param> params;
    public final String description;
    public final String texture;
    public final String displayName;

    AddCondition(boolean canBeUsed, String name, List<Param> params,
                 String description, String textureId, String displayName) {
        this.canBeUsed = canBeUsed;
        this.name = name;
        this.params = params;
        this.description = description;
        this.texture = textureId;
        this.displayName = displayName;
    }

    public static AddCondition searchByName(String name) {
        return BY_NAME.get(name);
    }

    public static AddCondition requireByName(String name) {
        AddCondition a = BY_NAME.get(name);
        if (a == null) throw new IllegalArgumentException("Unknown AddCondition name: " + name);
        return a;
    }
}
