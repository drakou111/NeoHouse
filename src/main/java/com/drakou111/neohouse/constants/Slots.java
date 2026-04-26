package com.drakou111.neohouse.constants;

public class Slots {
    /******************\
    *   HOUSING MENU   *
    \******************/
    public static final int HOUSING_MENU_SYSTEMS = 31;

    public static final int SYSTEMS_FUNCTIONS = 14;

    /***************\
    *   FUNCTIONS   *
    \***************/
    public static final int FUNCTION_DESCRIPTION = 13;
    public static final int FUNCTION_ICON = 15;
    public static final int FUNCTION_DELETE = 30;
    public static final int FUNCTION_AUTOMATIC_EXECUTION = 32;

    /*************\
    *   ACTIONS   *
    \*************/

    // CONDITIONAL
    public static final int ACTION_CONDITIONAL_CONDITIONALS = 10;
    public static final int ACTION_CONDITIONAL_MATCH_ANY = 11;
    public static final int ACTION_CONDITIONAL_IF = 12;
    public static final int ACTION_CONDITIONAL_ELSE = 13;

    // PLAYER GROUP
    public static final int ACTION_PLAYER_GROUP_VAL = 10;
    public static final int ACTION_PLAYER_GROUP_DEMOTION_PROTECTION = 11;

    // TITLE
    public static final int ACTION_TITLE_TITLE = 10;
    public static final int ACTION_TITLE_SUBTITLE = 11;
    public static final int ACTION_TITLE_FADE_IN = 12;
    public static final int ACTION_TITLE_STAY = 13;
    public static final int ACTION_TITLE_FADE_OUT = 14;

    // ACTION_BAR
    public static final int ACTION_ACTION_BAR_TEXT = 10;

    // MAX_HEALTH
    public static final int ACTION_MAX_HEALTH_VAL = 10;
    public static final int ACTION_MAX_HEALTH_OPERATION = 11;
    public static final int ACTION_MAX_HEALTH_HEAL_ON_CHANGE = 12;

    // GIVE_ITEM
    public static final int ACTION_GIVE_ITEM_ITEM = 10;
    public static final int ACTION_GIVE_ITEM_ALLOW_MULTIPLE = 11;
    public static final int ACTION_GIVE_ITEM_SLOT = 12;
    public static final int ACTION_GIVE_ITEM_REPLACE_EXISTING = 13;

    // REMOVE_ITEM
    public static final int ACTION_REMOVE_ITEM_ITEM = 10;

    // CHAT_MESSAGE
    public static final int ACTION_CHAT_MESSAGE_TEXT = 10;

    // APPLY_POTION
    public static final int ACTION_APPLY_POTION_EFFECT = 10;
    public static final int ACTION_APPLY_POTION_DURATION = 11;
    public static final int ACTION_APPLY_POTION_LEVEL = 12;
    public static final int ACTION_APPLY_POTION_OVERRIDE = 13;
    public static final int ACTION_APPLY_POTION_SHOW_ICON = 14;

    // GIVE_EXPERIENCE
    public static final int ACTION_GIVE_EXPERIENCE_VAL = 10;

    // SEND_LOBBY
    public static final int ACTION_SEND_LOBBY_LOBBY = 10;

    // SET_VARIABLE -> PLAYER/GLOBAL
    public static final int ACTION_SET_VARIABLE_HOLDER = 10;
    public static final int ACTION_SET_VARIABLE_VAR = 11;
    public static final int ACTION_SET_VARIABLE_OPERATION = 12;
    public static final int ACTION_SET_VARIABLE_VALUE = 13;
    public static final int ACTION_SET_VARIABLE_AUTOMATIC_UNSET = 14;

    // SET_VARIABLE -> TEAM
    public static final int ACTION_SET_TEAM_NAME = 11;
    public static final int ACTION_SET_TEAM_VARIABLE_VAR = 12;
    public static final int ACTION_SET_TEAM_VARIABLE_OPERATION = 13;
    public static final int ACTION_SET_TEAM_VARIABLE_VALUE = 14;
    public static final int ACTION_SET_TEAM_VARIABLE_AUTOMATIC_UNSET = 15;

    // TELEPORT
    public static final int ACTION_TELEPORT_LOCATION = 10;
    public static final int ACTION_TELEPORT_PREVENT_INSIDE_BLOCK = 11;

    // FAIL_PARKOUR
    public static final int ACTION_FAIL_PARKOUR_TEXT = 10;

    // PLAY_SOUND
    public static final int ACTION_PLAY_SOUND_TYPE = 10;
    public static final int ACTION_PLAY_SOUND_VOLUME = 11;
    public static final int ACTION_PLAY_SOUND_PITCH = 12;
    public static final int ACTION_PLAY_SOUND_LOCATION = 13;

    // COMPASS_TARGET
    public static final int ACTION_COMPASS_TARGET_LOCATION = 10;

    // GAME_MODE
    public static final int ACTION_GAME_MODE_TYPE = 10;

    // HEALTH
    public static final int ACTION_HEALTH_VAL = 10;
    public static final int ACTION_HEALTH_OPERATION = 11;

    // HUNGER
    public static final int ACTION_HUNGER_VAL = 10;
    public static final int ACTION_HUNGER_OPERATION = 11;

    // RANDOM
    public static final int ACTION_RANDOM_ACTIONS = 10;

    // TRIGGER FUNCTION
    public static final int ACTION_TRIGGER_FUNCTION_TYPE = 10;
    public static final int ACTION_TRIGGER_FUNCTION_ALL = 11;

    // LAYOUT
    public static final int ACTION_LAYOUT_TYPE = 10;

    // ENCHANT_HELD
    public static final int ACTION_ENCHANT_HELD_ENCHANT = 10;
    public static final int ACTION_ENCHANT_HELD_LEVEL = 11;

    // PAUSE
    public static final int ACTION_PAUSE_VAL = 10;

    // TEAM
    public static final int ACTION_TEAM_TYPE = 10;

    // DISPLAY_MENU
    public static final int ACTION_DISPLAY_MENU_TYPE = 10;

    // DROP_ITEM
    public static final int ACTION_DROP_ITEM_ITEM = 10;
    public static final int ACTION_DROP_ITEM_LOCATION = 11;
    public static final int ACTION_DROP_ITEM_DROP_NATURALLY = 12;
    public static final int ACTION_DROP_ITEM_DISABLE_MERGE = 13;
    public static final int ACTION_DROP_ITEM_PRIORITIZE_PLAYER = 14;
    public static final int ACTION_DROP_ITEM_DESPAWN_DURATION = 15;
    public static final int ACTION_DROP_ITEM_PICKUP_DELAY = 16;
    public static final int ACTION_DROP_ITEM_FALLBACK_INVENTORY = 19;

    // CHANGE_VELOCITY
    public static final int ACTION_CHANGE_VELOCITY_X = 10;
    public static final int ACTION_CHANGE_VELOCITY_Y = 11;
    public static final int ACTION_CHANGE_VELOCITY_Z = 12;

    // LAUNCH_TARGET
    public static final int ACTION_LAUNCH_TARGET_LOCATION = 10;
    public static final int ACTION_LAUNCH_TARGET_STRENGTH = 11;

    // SET_WEATHER
    public static final int ACTION_SET_WEATHER_WEATHER = 10;

    // SET_TIME
    public static final int ACTION_SET_TIME_TIME = 10;

    // NAME_TAG
    public static final int ACTION_NAME_TAG_TOGGLE = 10;

    /****************\
    *   CONDITIONS   *
    \****************/

    // GROUP
    public static final int CONDITION_GROUP_INVERT = 10;
    public static final int CONDITION_GROUP_TYPE = 11;
    public static final int CONDITION_GROUP_INCLUDE_HIGHER = 12;

    // VARIABLE (PLAYER/GLOBAL)
    public static final int CONDITION_VARIABLE_INVERT = 10;
    public static final int CONDITION_VARIABLE_HOLDER = 11;
    public static final int CONDITION_VARIABLE_VAR = 12;
    public static final int CONDITION_VARIABLE_COMPARATOR = 13;
    public static final int CONDITION_VARIABLE_VALUE = 14;
    public static final int CONDITION_VARIABLE_FALLBACK_VALUE = 15;


    // VARIABLE (TEAM)
    public static final int CONDITION_TEAM_VARIABLE_INVERT = 10;
    public static final int CONDITION_TEAM_VARIABLE_HOLDER = 11;
    public static final int CONDITION_TEAM_VARIABLE_TEAM_NAME = 12;
    public static final int CONDITION_TEAM_VARIABLE_VAR = 13;
    public static final int CONDITION_TEAM_VARIABLE_COMPARATOR = 14;
    public static final int CONDITION_TEAM_VARIABLE_VALUE = 15;
    public static final int CONDITION_TEAM_VARIABLE_FALLBACK_VALUE = 16;

    // PERMISSION
    public static final int CONDITION_PERMISSION_INVERT = 10;
    public static final int CONDITION_PERMISSION_TYPE = 11;

    // REGION
    public static final int CONDITION_REGION_INVERT = 10;
    public static final int CONDITION_REGION_TYPE = 11;

    // ITEM
    public static final int CONDITION_ITEM_INVERT = 10;
    public static final int CONDITION_ITEM_ITEM = 11;
    public static final int CONDITION_ITEM_WHAT_TO_CHECK = 12;
    public static final int CONDITION_ITEM_WHERE_TO_CHECK = 13;
    public static final int CONDITION_ITEM_AMOUNT  = 14;

    // DOING_PARKOUR
    public static final int CONDITION_DOING_PARKOUR_INVERT = 10;

    // EFFECT
    public static final int CONDITION_EFFECT_INVERT = 10;
    public static final int CONDITION_EFFECT_TYPE = 11;

    // SNEAKING
    public static final int CONDITION_SNEAKING_INVERT = 10;

    // FLYING
    public static final int CONDITION_FLYING_INVERT = 10;

    // HEALTH
    public static final int CONDITION_HEALTH_INVERT = 10;
    public static final int CONDITION_HEALTH_COMPARATOR = 11;
    public static final int CONDITION_HEALTH_VALUE = 12;

    // MAX_HEALTH
    public static final int CONDITION_MAX_HEALTH_INVERT = 10;
    public static final int CONDITION_MAX_HEALTH_COMPARATOR = 11;
    public static final int CONDITION_MAX_HEALTH_VALUE = 12;

    // HUNGER
    public static final int CONDITION_HUNGER_INVERT = 10;
    public static final int CONDITION_HUNGER_COMPARATOR = 11;
    public static final int CONDITION_HUNGER_VALUE = 12;

    // GAME_MODE
    public static final int CONDITION_GAME_MODE_INVERT = 10;
    public static final int CONDITION_GAME_MODE_TYPE = 11;

    // PLACEHOLDER
    public static final int CONDITION_PLACEHOLDER_INVERT = 10;
    public static final int CONDITION_PLACEHOLDER_VAR = 11;
    public static final int CONDITION_PLACEHOLDER_COMPARATOR = 12;
    public static final int CONDITION_PLACEHOLDER_VALUE = 13;

    // TEAM
    public static final int CONDITION_TEAM_INVERT = 10;
    public static final int CONDITION_TEAM_TYPE = 11;

    //PORTAL_TYPE
    public static final int CONDITION_PORTAL_TYPE_INVERT = 10;
    public static final int CONDITION_PORTAL_TYPE_TYPE = 11;

    //PVP_ENABLED
    public static final int CONDITION_PVP_ENABLED_INVERT = 10;

    // DAMAGE_CAUSE
    public static final int CONDITION_DAMAGE_CAUSE_INVERT = 10;
    public static final int CONDITION_DAMAGE_CAUSE_TYPE = 11;

    // DAMAGE_AMOUNT
    public static final int CONDITION_DAMAGE_AMOUNT_INVERT = 10;
    public static final int CONDITION_DAMAGE_AMOUNT_COMPARATOR = 11;
    public static final int CONDITION_DAMAGE_AMOUNT_VALUE = 12;

    // BLOCK_TYPE
    public static final int CONDITION_BLOCK_TYPE_INVERT = 10;
    public static final int CONDITION_BLOCK_TYPE_ITEM = 11;
    public static final int CONDITION_BLOCK_TYPE_MATCH_TYPE_ONLY = 12;

    // IS_ITEM
    public static final int CONDITION_IS_ITEM_INVERT = 10;
    public static final int CONDITION_IS_ITEM_ITEM = 11;
    public static final int CONDITION_IS_ITEM_WHAT_CHECK = 12;
    public static final int CONDITION_IS_ITEM_WHERE_CHECK = 13;
    public static final int CONDITION_IS_ITEM_AMOUNT = 14;

    // FISHING_ENVIRONMENT
    public static final int CONDITION_FISHING_ENVIRONMENT_INVERT = 10;
    public static final int CONDITION_FISHING_ENVIRONMENT_ENVIRONMENT = 11;


    /*************\
    *   GENERAL   *
    \*************/

    // Assuming height of 6 (most things)
    public static final int GENERAL_GO_BACK_H6 = 49;
    public static final int GENERAL_ADD_H6 = 50;
    public static final int GENERAL_NEXT_H6 = 53;
    public static final int[] GENERAL_LIST_H6 = new int[]{ 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };

    // Assuming height of 4 (some actions, like conditional)
    public static final int GENERAL_GO_BACK_H4 = 31;
    public static final int GENERAL_ADD_H4 = 32;
    public static final int GENERAL_NOTE_H4 = 32;
    public static final int GENERAL_NEXT_H4 = 35;
    public static final int GENERAL_CONFIRM = 11;
    public static final int[] GENERAL_LIST_H4 = new int[]{ 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25 };

}
