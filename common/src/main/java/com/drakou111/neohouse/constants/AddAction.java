package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.compiler.enums.Param;
import com.drakou111.neohouse.constants.actions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum AddAction {
    CONDITIONAL(25, false, "_", List.of(), "", "redstone", "Conditional"),
    CANCEL_EVENT(1,  true, "Cancel", List.of(), "", "tnt", "Cancel Event"),
    CHANGE_GROUP(1, true, "setGroup",
            List.of(Param.STRING.withName(PlayerGroupInputs.GROUP), Param.BOOL.withName(PlayerGroupInputs.DEMOTION_PROTECTION)),
            "Sets the player's group/rank.", "player_head", "Change Player's Group"),
    KILL(1, true, "kill", List.of(),
            "Kills the player instantly.", "iron_bars", "Kill Player"),
    FULL_HEAL(5, true, "heal", List.of(),
            "Fully restores the player's health and hunger.", "golden_apple", "Full Heal"),
    TITLE(5, true, "title",
            List.of(Param.STRING.withName(TitleInputs.TITLE), Param.STRING.withName(TitleInputs.SUBTITLE), Param.longRange(0, 3).withName(TitleInputs.FADEIN), Param.longRange(0, 10).withName(TitleInputs.STAY), Param.longRange(0, 3).withName(TitleInputs.FADEOUT)),
            "Shows title/subtitle. fadeIn, stay and fadeOut are in seconds.", "book", "Display Title"),
    ACTION_BAR(5, true, "actionBar",
            List.of(Param.STRING.withName(ActionBarInputs.MESSAGE)),
            "Displays a message in the player's action bar.", "writable_book", "Display Action Bar"),
    CLEAR_INVENTORY(1, true, "clearInventory", List.of(),
            "Removes all items from the player's inventory.", "stone", "Reset Inventory"),
    MAX_HEALTH(5, true, "setMaxHealth",
            List.of(Param.EXPR.withName(MaxHealthInputs.MAX_HEALTH), Param.enumOf(RestrictedOperator.class).withName(MaxHealthInputs.MODE), Param.BOOL.withName(MaxHealthInputs.HEAL_ON_CHANGE)),
            "Changes max health.", "dandelion", "Change Max Health"),
    PARKOUR_CHECKPOINT(1, true, "triggerParkourCheckpoint", List.of(),
            "Sets the player's current position as a parkour checkpoint.", "light_weighted_pressure_plate", "Parkour Checkpoint"),
    GIVE_ITEM(40, true, "giveItem",
            List.of(Param.ITEM.withName(GiveItemInputs.ITEM), Param.BOOL.withName(GiveItemInputs.ALLOW_MULTIPLE), Param.enumOf(InventorySlots.class).withName(GiveItemInputs.INVENTORY_SLOT), Param.BOOL.withName(GiveItemInputs.REPLACE_ITEM)),
            "Gives an item to the player.", "chest", "Give Item"),
    REMOVE_ITEM(40, true, "removeItem",
            List.of(Param.ITEM.withName(RemoveItemInputs.ITEM)),
            "Removes an item from the player's inventory.", "cauldron", "Remove Item"),
    CHAT_MESSAGE(20, true, "message",
            List.of(Param.STRING.withName(ChatMessageInputs.MESSAGE)),
            "Sends a chat message to the player.", "map", "Send a Chat Message"),
    APPLY_POTION(22, true, "applyPotion",
            List.of(Param.enumOf(Potion.class).withName(ApplyPotionInputs.EFFECT), Param.longRange(1, 2_592_000).withName(ApplyPotionInputs.DURATION), Param.longRange(1, 10).withName(ApplyPotionInputs.LEVEL), Param.BOOL.withName(ApplyPotionInputs.OVERRIDE), Param.BOOL.withName(ApplyPotionInputs.SHOW_ICON)),
            "Applies a potion.", "potion", "Apply Potion Effect"),
    CLEAR_POTION(5, true, "clearPotion", List.of(),
            "Removes all active potion effects from the player.", "glass_bottle", "Clear All Potion Effects"),
    GIVE_XP(5, true, "giveXp",
            List.of(Param.longRange(1, Integer.MAX_VALUE).withName(GiveExperienceInputs.LEVELS)),
            "Awards XP levels to the player.", "experience_bottle", "Give Experience Levels"),
    SEND_LOBBY(1, true, "sendLobby",
            List.of(Param.enumOf(Lobby.class).withName(SendLobbyInputs.LOCATION)),
            "Sends the player to a Hypixel lobby.", "bookshelf", "Send to Lobby"),
    VARIABLE(25, false, "_", List.of(), "", "feather", "Change Variable"),
    TELEPORT(5, true, "teleport",
            List.of(Param.LOCATION.withName(TeleportInputs.LOCATION), Param.BOOL.withName(TeleportInputs.PREVENT_TELEPORT_INSIDE_BLOCKS)),
            "Teleports the player to a named location or custom coords. Either set 'Location' to coordinates or to a type like CURRENT.", "ender_pearl", "Teleport Player"),
    FAIL_PARKOUR(1, true, "failParkour",
            List.of(Param.STRING.withName(FailParkourInputs.REASON)),
            "Fails the player's current parkour run with a reason message.", "light_weighted_pressure_plate", "Fail Parkour"),
    PLAY_SOUND(25, true, "playSound",
            List.of(Param.enumOf(Sound.class).withName(PlaySoundInputs.SOUND), Param.doubleRange(0D, 2D).withName(PlaySoundInputs.VOLUME), Param.doubleRange(0D, 2D).withName(PlaySoundInputs.PITCH), Param.LOCATION.withName(PlaySoundInputs.LOCATION)),
            "Plays a sound.", "note_block", "Play Sound"),
    COMPASS_TARGET(5, true, "setCompassTarget",
            List.of(Param.LOCATION.withName(CompassTargetInputs.LOCATION)),
            "Points the player's compass at a named location.", "compass", "Set Compass Target"),
    SET_GAME_MODE(1, true, "setGamemode",
            List.of(Param.enumOf(Gamemode.class).withName(GamemodeInputs.GAMEMODE)),
            "Changes the player's game mode.", "daylight_detector", "Set Gamemode"),
    CHANGE_HEALTH(5, true, "setHealth",
            List.of(Param.EXPR.withName(HealthInputs.HEALTH), Param.enumOf(RestrictedOperator.class).withName(HealthInputs.MODE)),
            "Changes the player's health.", "apple", "Change Health"),
    CHANGE_HUNGER(5, true, "setHunger",
            List.of(Param.EXPR.withName(HungerInputs.LEVEL), Param.enumOf(RestrictedOperator.class).withName(HungerInputs.MODE)),
            "Changes the player's hunger level.", "cooked_beef", "Change Hunger Level"),
    RANDOM(25, false, "_", List.of(), "", "ender_chest", "Random Action"),
    TRIGGER_FUNCTION(10, false, "_", List.of(), "", "activator_rail", "Trigger Function"),
    APPLY_LAYOUT(5, true, "applyLayout",
            List.of(Param.STRING.withName(LayoutInputs.LAYOUT)),
            "Applies a layout to the player.", "iron_axe", "Apply Inventory Layout"),
    EXIT(1, false, "_", List.of(), "", "bedrock", "Exit"),
    ENCHANT_HELD_ITEM(25, true, "enchantItem",
            List.of(Param.enumOf(Enchant.class).withName(EnchantHeldInputs.ENCHANTMENT), Param.longRange(1, 10).withName(EnchantHeldInputs.LEVEL)),
            "Enchants the held item.", "enchanted_book", "Enchant Held Item"),
    PAUSE(30, true, "pause",
            List.of(Param.longRange(1, 6_000).withName(PauseInputs.TICKS)),
            "Pauses execution of this function for N ticks.", "red_bed", "Pause Execution"),
    SET_TEAM(1, true, "setTeam",
            List.of(Param.STRING.withName(TeamInputs.TEAM)),
            "Assigns the player to a named team.", "oak_sign", "Set Player Team"),
    DISPLAY_MENU(10, true, "displayMenu",
                 List.of(Param.STRING.withName(DisplayMenuInputs.MENU)),
                 "Opens a housing inventory menu for the player.", "chest", "Display Menu"),
    CLOSE_MENU(1, true, "closeMenu", List.of(),
                 "Closes the current menu.", "chest", "Close Menu"),
    DROP_ITEM(5, true, "dropItem",
            List.of(Param.ITEM.withName(DropItemInputs.ITEM), Param.LOCATION.withName(DropItemInputs.LOCATION), Param.BOOL.withName(DropItemInputs.DROP_NATURALLY), Param.BOOL.withName(DropItemInputs.PREVENT_MERGE), Param.longRange(1, 18_000).withName(DropItemInputs.DESPAWN_DURATION), Param.longRange(0, 18_000).withName(DropItemInputs.PICKUP_DELAY), Param.BOOL.withName(DropItemInputs.PRIORITIZE_PLAYER), Param.BOOL.withName(DropItemInputs.FALLBACK_INVENTORY)),
            "Drops an item at a location.", "dropper", "Drop Item"),
    CHANGE_VELOCITY(5, true, "changeVelocity",
            List.of(Param.EXPR.withName(ChangeVelocityInputs.X_DIRECTION), Param.EXPR.withName(ChangeVelocityInputs.Y_DIRECTION), Param.EXPR.withName(ChangeVelocityInputs.Z_DIRECTION)),
            "Sets the player's velocity vector (x, y, z).", "slime_block", "Change Velocity"),
    LAUNCH_TO_TARGET(5, true, "launchToTarget",
            List.of(Param.LOCATION.withName(LaunchTargetInputs.TARGET_LOCATION), Param.EXPR.withName(LaunchTargetInputs.STRENGTH)),
            "Launches the player toward a target location.", "slime_ball", "Launch to Target"),
    SET_WEATHER(5, true, "setWeather",
            List.of(Param.enumOf(Weather.class).withName(SetWeatherInputs.WEATHER)),
            "Sets the weather for the player.", "water_bucket", "Set Player Weather"),
    SET_TIME(5, true, "setTime",
            List.of(Param.enumOf(Time.class).withName(SetTimeInputs.TIME)),
            "Sets the time for the player.", "clock", "Set Player Time"),
    NAME_TAG_DISPLAY(5, true, "setNameTagDisplay",
            List.of(Param.BOOL.withName(NameTagInputs.DISPLAY)),
            "Shows or hides the player's name tag above their head.", "name_tag", "Toggle Nametag Display");

    public final int maxCount;
    public final boolean canBeUsed;
    public final String name;
    public final List<Param> params;
    public final String description;
    public final String texture;
    public final String displayName;

    AddAction(int maxCount, boolean canBeUsed, String name, List<Param> params,
              String description, String textureId, String displayName) {
        this.maxCount = maxCount;
        this.canBeUsed = canBeUsed;
        this.name = name;
        this.params = params;
        this.description = description;
        this.texture = textureId;
        this.displayName = displayName;
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
