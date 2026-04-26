package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.compiler.enums.ActionParamKind;
import net.minecraft.resources.ResourceLocation;

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
                List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL),
                "True if the player is in the given portal type.", "end_portal_frame"),
    PVP_ENABLED(true, "isPvpEnabled",
                List.of(ActionParamKind.BOOL_LITERAL),
                "True if PvP is currently enabled in the housing.", "iron_sword"),
    DAMAGE_CAUSE(true, "isDamageCause",
                 List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL),
                 "True if the damage cause matches the given value.", "diamond_sword"),
    DAMAGE_AMOUNT(true, "isDamageAmount",
                  List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.NORMAL_EXPRESSION),
                  "True if the damage amount matches the comparator.", "golden_sword"),
    BLOCK_TYPE(true, "isBlockType",
               List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL),
               "True if the broken block matches the given type.", "grass_block"),
    IS_ITEM(true, "isItem",
            List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL,
                    ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL),
            "True if an item in the player's inventory matches the criteria.", "chest"),
    FISHING_ENVIRONMENT(true, "isFishingEnvironment",
                        List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL),
                        "True if the player is fishing in the given environment (WATER/LAVA).", "fishing_rod"),
    GROUP(true, "isGroup",
          List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL),
          "True if the player is in the given group. includeHigher matches higher ranks.", "player_head"),
    VARIABLE(false, "_", List.of(), "", "feather"),
    PERMISSION(true, "hasPermission",
               List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL),
               "True if the player has the given permission node.", "filled_map"),
    REGION(true, "inRegion",
           List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL),
           "True if the player is inside the named region.", "grass_block"),
    HAS_ITEM(true, "hasItem",
             List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL,
                     ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL),
             "True if the player has an item matching criteria in their inventory.", "chest"),
    DOING_PARKOUR(true, "doingParkour",
                  List.of(ActionParamKind.BOOL_LITERAL),
                  "True if the player is currently on a parkour run.", "light_weighted_pressure_plate"),
    POTION(true, "hasPotion",
           List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL),
           "True if the player has the given potion effect active.", "potion"),
    SNEAKING(true, "isSneaking",
             List.of(ActionParamKind.BOOL_LITERAL),
             "True if the player is currently sneaking.", "hay_block"),
    FLYING(true, "isFlying",
           List.of(ActionParamKind.BOOL_LITERAL),
           "True if the player is currently flying.", "feather"),
    HEALTH(true, "hasHealth",
           List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.NORMAL_EXPRESSION),
           "True if the player's health matches the comparator.", "apple"),
    MAX_HEALTH(true, "hasMaxHealth",
               List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.NORMAL_EXPRESSION),
               "True if the player's max health matches the comparator.", "golden_apple"),
    HUNGER(true, "hasHunger",
           List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL, ActionParamKind.NORMAL_EXPRESSION),
           "True if the player's hunger matches the comparator.", "cooked_beef"),
    GAME_MODE(true, "hasGamemode",
              List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL),
              "True if the player is in the given game mode.", "daylight_detector"),
    PLACEHOLDER(false, "_", List.of(ActionParamKind.BOOL_LITERAL), "", "sign"),
    TEAM(true, "hasTeam",
         List.of(ActionParamKind.BOOL_LITERAL, ActionParamKind.STRING_LITERAL),
         "True if the player is in the given team.", "player_head");

    public static final Map<String, AddCondition> BY_NAME = Arrays.stream(values())
                                                                  .filter(a -> a.canBeUsed)
                                                                  .collect(Collectors.toMap(a -> a.name, Function.identity()));
    public final boolean canBeUsed;
    public final String name;
    public final List<ActionParamKind> params;
    public final String description;
    public final ResourceLocation texture;

    AddCondition(boolean canBeUsed, String name, List<ActionParamKind> params,
                 String description, String textureId) {
        this.canBeUsed = canBeUsed;
        this.name = name;
        this.params = params;
        this.description = description;
        this.texture = ResourceLocation.withDefaultNamespace(textureId);
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
