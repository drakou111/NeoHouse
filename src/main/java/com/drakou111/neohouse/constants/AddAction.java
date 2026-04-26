package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.compiler.enums.ActionParamKind;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/******************\
 *   ADD ACTION   *
\******************/

public enum AddAction {
    CONDITIONAL(25, false, "_", List.of(), "", "redstone"),
    CANCEL_EVENT(1,  false, "_", List.of(), "", "tnt"),
    CHANGE_GROUP(1, true, "setGroup",
            List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL),
            "Sets the player's group/rank. demote=true allows lower rank.", "player_head"),
    KILL(1, true, "kill", List.of(),
            "Kills the player instantly.", "iron_bars"),
    FULL_HEAL(5, true, "heal", List.of(),
            "Fully restores the player's health and hunger.", "golden_apple"),
    TITLE(5, true, "title",
            List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL,
                    ActionParamKind.LONG_LITERAL, ActionParamKind.LONG_LITERAL, ActionParamKind.LONG_LITERAL),
            "Shows title/subtitle. fadeIn, stay, fadeOut are in ticks.", "book"),
    ACTION_BAR(5, true, "actionBar",
            List.of(ActionParamKind.STRING_LITERAL),
            "Displays a message in the player's action bar.", "writable_book"),
    CLEAR_INVENTORY(1, true, "clearInventory", List.of(),
            "Removes all items from the player's inventory.", "stone"),
    MAX_HEALTH(5, true, "setMaxHealth",
            List.of(ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL),
            "Changes max health. Operator: SET/ADD/MULTIPLY. heal=true heals to new max.", "dandelion"),
    PARKOUR_CHECKPOINT(1, true, "triggerParkourCheckpoint", List.of(),
            "Sets the player's current position as a parkour checkpoint.", "light_weighted_pressure_plate"),
    GIVE_ITEM(40, true, "giveItem",
            List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL,
                    ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL),
            "Gives an item from a housing slot. Args: slot, allowMultiple, targetSlots, replace.", "chest"),
    REMOVE_ITEM(40, true, "removeItem",
            List.of(ActionParamKind.STRING_LITERAL),
            "Removes a housing item from the player's inventory.", "cauldron"),
    CHAT_MESSAGE(20, true, "message",
            List.of(ActionParamKind.STRING_LITERAL),
            "Sends a chat message to the player.", "map"),
    APPLY_POTION(22, true, "applyPotion",
            List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.LONG_LITERAL,
                    ActionParamKind.LONG_LITERAL, ActionParamKind.BOOL_LITERAL, ActionParamKind.BOOL_LITERAL),
            "Applies a potion. Args: type, duration, amplifier, ambient, showParticles.", "potion"),
    CLEAR_POTION(5, true, "clearPotion", List.of(),
            "Removes all active potion effects from the player.", "glass_bottle"),
    GIVE_XP(5, true, "giveXp",
            List.of(ActionParamKind.LONG_LITERAL),
            "Awards XP levels to the player.", "experience_bottle"),
    SEND_LOBBY(1, true, "sendLobby",
            List.of(ActionParamKind.STRING_LITERAL),
            "Sends the player to a Hypixel lobby.", "bookshelf"),
    VARIABLE(25, false, "_", List.of(), "", "feather"),
    TELEPORT(5, true, "teleport",
            List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.BOOL_LITERAL),
            "Teleports the player to a named location or custom coords.", "ender_pearl"),
    FAIL_PARKOUR(1, true, "failParkour",
            List.of(ActionParamKind.STRING_LITERAL),
            "Fails the player's current parkour run with a reason message.", "light_weighted_pressure_plate"),
    PLAY_SOUND(25, true, "playSound",
            List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.DOUBLE_LITERAL,
                    ActionParamKind.DOUBLE_LITERAL, ActionParamKind.STRING_LITERAL),
            "Plays a sound. Args: sound, volume, pitch, location.", "note_block"),
    COMPASS_TARGET(5, true, "setCompassTarget",
            List.of(ActionParamKind.STRING_LITERAL),
            "Points the player's compass at a named location.", "compass"),
    SET_GAME_MODE(1, true, "setGamemode",
            List.of(ActionParamKind.STRING_LITERAL),
            "Changes the player's game mode (SURVIVAL/CREATIVE/ADVENTURE/SPECTATOR).", "daylight_detector"),
    CHANGE_HEALTH(5, true, "setHealth",
            List.of(ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.STRING_LITERAL),
            "Changes the player's health. Operators: SET/ADD/MULTIPLY.", "apple"),
    CHANGE_HUNGER(5, true, "setHunger",
            List.of(ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.STRING_LITERAL),
            "Changes the player's hunger level. Operators: SET/ADD/MULTIPLY.", "cooked_beef"),
    RANDOM(25, false, "_", List.of(), "", "ender_chest"),
    TRIGGER_FUNCTION(10, false, "_", List.of(), "", "activator_rail"),
    APPLY_LAYOUT(5, true, "applyLayout",
            List.of(ActionParamKind.STRING_LITERAL),
            "Applies a saved inventory layout to the player.", "iron_axe"),
    EXIT(1, false, "_", List.of(), "", "bedrock"),
    ENCHANT_HELD_ITEM(25, true, "enchantItem",
            List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.LONG_LITERAL),
            "Enchants the held item. Args: enchantment name, level.", "enchanted_book"),
    PAUSE(30, true, "pause",
            List.of(ActionParamKind.LONG_LITERAL),
            "Pauses execution of this function for N ticks.", "red_bed"),
    SET_TEAM(1, true, "setTeam",
            List.of(ActionParamKind.STRING_LITERAL),
            "Assigns the player to a named team.", "oak_sign"),
    DISPLAY_MENU(10, true, "displayMenu",
            List.of(ActionParamKind.STRING_LITERAL),
            "Opens a housing inventory menu for the player.", "chest"),
    DROP_ITEM(5, true, "dropItem",
            List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.STRING_LITERAL,
                    ActionParamKind.BOOL_LITERAL, ActionParamKind.BOOL_LITERAL,
                    ActionParamKind.LONG_LITERAL, ActionParamKind.LONG_LITERAL,
                    ActionParamKind.BOOL_LITERAL, ActionParamKind.BOOL_LITERAL),
            "Drops an item from a housing slot at a location.", "dropper"),
    CHANGE_VELOCITY(5, true, "changeVelocity",
            List.of(ActionParamKind.NORMAL_EXPRESSION, ActionParamKind.NORMAL_EXPRESSION,
                    ActionParamKind.NORMAL_EXPRESSION),
            "Sets the player's velocity vector (x, y, z).", "slime_block"),
    LAUNCH_TO_TARGET(5, true, "launchToTarget",
            List.of(ActionParamKind.STRING_LITERAL, ActionParamKind.NORMAL_EXPRESSION),
            "Launches the player toward a target location at a given speed.", "slime_ball"),
    SET_WEATHER(5, true, "setWeather",
            List.of(ActionParamKind.STRING_LITERAL),
            "Sets the housing weather (SUNNY/RAINY/SNOWY).", "water_bucket"),
    SET_TIME(5, true, "setTime",
            List.of(ActionParamKind.STRING_LITERAL),
            "Sets the world time (DAWN/NOON/DUSK/MIDNIGHT or custom ticks).", "clock"),
    NAME_TAG_DISPLAY(5, true, "setNameTagDisplay",
            List.of(ActionParamKind.BOOL_LITERAL),
            "Shows or hides the player's name tag above their head.", "name_tag");

    public final int maxCount;
    public final boolean canBeUsed;
    public final String name;
    public final List<ActionParamKind> params;
    public final String description;
    public final ResourceLocation texture;

    AddAction(int maxCount, boolean canBeUsed, String name, List<ActionParamKind> params,
              String description, String textureId) {
        this.maxCount = maxCount;
        this.canBeUsed = canBeUsed;
        this.name = name;
        this.params = params;
        this.description = description;
        this.texture = ResourceLocation.withDefaultNamespace(textureId);
    }

    public static final Map<String, AddAction> BY_NAME = Arrays.stream(values())
            .filter(a -> a.canBeUsed)
            .collect(Collectors.toMap(a -> a.name, Function.identity()));

    public static AddAction searchByName(String name) { return BY_NAME.get(name); }

    public static AddAction requireByName(String name) {
        AddAction a = BY_NAME.get(name);
        if (a == null) throw new IllegalArgumentException("Unknown AddAction name: " + name);
        return a;
    }
}
