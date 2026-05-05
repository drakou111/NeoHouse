package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.compiler.enums.VariableType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Placeholder {
    SERVER_NAME("serverName",           VariableType.STRING, List.of(), "server.name",           "The full name of this Housing server."),
    SERVER_SHORT_NAME("serverShortName",VariableType.STRING, List.of(), "server.shortName",      "The abbreviated server name."),
    PLAYER_NAME("playerName",           VariableType.STRING, List.of(), "player.name",            "The player's Minecraft username."),
    PLAYER_PING("playerPing",           VariableType.STRING, List.of(), "player.ping",            "The player's current network ping in ms."),
    PLAYER_HEALTH("playerHealth",       VariableType.DOUBLE, List.of(), "player.health",          "The player's current health."),
    PLAYER_MAX_HEALTH("playerMaxHealth",VariableType.DOUBLE, List.of(), "player.maxHealth",       "The player's maximum health."),
    PLAYER_HUNGER("playerHunger",       VariableType.DOUBLE, List.of(), "player.hunger",          "The player's current hunger/food level."),
    PLAYER_EXPERIENCE("playerExperience",VariableType.LONG, List.of(), "player.experience",      "The player's current XP points."),
    PLAYER_LEVEL("playerLevel",         VariableType.LONG,   List.of(), "player.level",           "The player's current XP level number."),
    PLAYER_VERSION("playerVersion",     VariableType.STRING, List.of(), "player.version",         "The Minecraft version the player is using."),
    PLAYER_PROTOCOL("playerProtocol",   VariableType.LONG,   List.of(), "player.protocol",        "The player's protocol version number."),
    PLAYER_GAMEMODE("playerGamemode",   VariableType.STRING, List.of(), "player.gamemode",        "The player's current game mode."),
    PLAYER_REGION_NAME("playerRegionName",VariableType.STRING,List.of(),"player.regionName",     "The name of the region the player is in."),
    PLAYER_POS_X("playerPosX",          VariableType.DOUBLE, List.of(), "player.pos.x",           "The player's precise X coordinate."),
    PLAYER_POS_Y("playerPosY",          VariableType.DOUBLE, List.of(), "player.pos.y",           "The player's precise Y coordinate."),
    PLAYER_POS_Z("playerPosZ",          VariableType.DOUBLE, List.of(), "player.pos.z",           "The player's precise Z coordinate."),
    PLAYER_POS_YAW("playerPosYaw",      VariableType.DOUBLE, List.of(), "player.pos.yaw",         "The player's horizontal rotation (yaw)."),
    PLAYER_POS_PITCH("playerPosPitch",  VariableType.DOUBLE, List.of(), "player.pos.pitch",       "The player's vertical rotation (pitch)."),
    PLAYER_BLOCK_X("playerBlockX",      VariableType.LONG,   List.of(), "player.block.x",         "The player's block X coordinate."),
    PLAYER_BLOCK_Y("playerBlockY",      VariableType.LONG,   List.of(), "player.block.y",         "The player's block Y coordinate."),
    PLAYER_BLOCK_Z("playerBlockZ",      VariableType.LONG,   List.of(), "player.block.z",         "The player's block Z coordinate."),
    PLAYER_GROUP_NAME("playerGroupName",VariableType.STRING, List.of(), "player.group.name",      "The player's group/rank name."),
    PLAYER_GROUP_TAG("playerGroupTag",  VariableType.STRING, List.of(), "player.group.tag",       "The player's group tag prefix."),
    PLAYER_GROUP_PRIORITY("playerGroupPriority",VariableType.LONG,List.of(),"player.group.priority","The player's group priority number."),
    PLAYER_GROUP_COLOR("playerGroupColor",VariableType.STRING,List.of(),"player.group.color",    "The player's group color code."),
    PLAYER_TEAM_NAME("playerTeamName",  VariableType.STRING, List.of(), "player.team.name",       "The player's current team name."),
    PLAYER_TEAM_TAG("playerTeamTag",    VariableType.STRING, List.of(), "player.team.tag",        "The player's team tag prefix."),
    PLAYER_TEAM_COLOR("playerTeamColor",VariableType.STRING, List.of(), "player.team.color",      "The player's team color code."),
    PLAYER_TEAM_PLAYERS("playerTeamPlayers",VariableType.LONG,List.of(VariableType.STRING),"player.team.players","Number of players in the team. Input: team name."),
    PLAYER_PARKOUR_TICKS("playerParkourTicks",VariableType.LONG,List.of(),"player.parkour.ticks","Ticks elapsed since the player started parkour."),
    PLAYER_PARKOUR_FORMATTED("playerParkourFormatted",VariableType.STRING,List.of(),"player.parkour.formatted","Elapsed parkour time as a formatted string."),
    HOUSE_NAME("houseName",             VariableType.STRING, List.of(), "house.name",             "The name of this Housing plot."),
    HOUSE_COOKIES("houseCookies",       VariableType.LONG,   List.of(), "house.cookies",          "The Housing plot's cookie count."),
    HOUSE_VISITING_RULES("houseVisitingRules",VariableType.STRING,List.of(),"house.visitingRules","The visiting rules of this Housing plot."),
    HOUSE_PLAYERS("housePlayers",       VariableType.LONG,   List.of(), "house.players",          "Number of players currently in the Housing."),
    DATE_DAY("dateDay",                 VariableType.LONG,   List.of(), "date.day",               "Current UTC day of the month (1–31)."),
    DATE_MONTH("dateMonth",             VariableType.LONG,   List.of(), "date.month",             "Current UTC month (1–12)."),
    DATE_YEAR("dateYear",               VariableType.LONG,   List.of(), "date.year",              "Current UTC year."),
    DATE_HOUR("dateHour",               VariableType.LONG,   List.of(), "date.hour",              "Current UTC hour (0–23)."),
    DATE_MINUTE("dateMinute",           VariableType.LONG,   List.of(), "date.minute",            "Current UTC minute (0–59)."),
    DATE_SECONDS("dateSeconds",         VariableType.LONG,   List.of(), "date.seconds",           "Current UTC seconds (0–59)."),
    DATE_DAY_TIMEZONE("dateDayTimezone",VariableType.LONG,   List.of(VariableType.STRING),"date.day.timezone","Day of month in the given timezone."),
    DATE_MONTH_TIMEZONE("dateMonthTimezone",VariableType.LONG,List.of(VariableType.STRING),"date.month.timezone","Month in the given timezone."),
    DATE_YEAR_TIMEZONE("dateYearTimezone",VariableType.LONG, List.of(VariableType.STRING),"date.year.timezone","Year in the given timezone."),
    DATE_HOUR_TIMEZONE("dateHourTimezone",VariableType.LONG, List.of(VariableType.STRING),"date.hour.timezone","Hour in the given timezone."),
    DATE_MINUTE_TIMEZONE("dateMinuteTimezone",VariableType.LONG,List.of(VariableType.STRING),"date.minute.timezone","Minute in the given timezone."),
    DATE_SECONDS_TIMEZONE("dateSecondsTimezone",VariableType.LONG,List.of(VariableType.STRING),"date.seconds.timezone","Seconds in the given timezone."),
    DATE_UNIX("dateUnix",               VariableType.LONG,   List.of(), "date.unix",              "Current Unix timestamp in seconds."),
    DATE_UNIX_MS("dateUnixMs",          VariableType.LONG,   List.of(), "date.unix.ms",           "Current Unix timestamp in milliseconds."),
    RANDOM_WHOLE("randomWhole",         VariableType.LONG,   List.of(), "random.whole",           "A random whole number (full range)."),
    RANDOM_WHOLE_CAPPED("randomWholeCapped",VariableType.LONG,List.of(VariableType.LONG),"random.whole","Random integer from 0 to max. Input: max."),
    RANDOM_WHOLE_RANGED("randomWholeRanged",VariableType.LONG,List.of(VariableType.LONG,VariableType.LONG),"random.whole","Random integer in [min, max]. Inputs: min, max."),
    RANDOM_DECIMAL("randomDecimal",     VariableType.DOUBLE, List.of(), "random.decimal",         "A random decimal between 0.0 and 1.0."),
    RANDOM_DECIMAL_CAPPED("randomDecimalCapped",VariableType.DOUBLE,List.of(VariableType.DOUBLE),"random.decimal","Random decimal 0.0 to max. Input: max."),
    RANDOM_DECIMAL_RANGED("randomDecimalRanged",VariableType.DOUBLE,List.of(VariableType.DOUBLE,VariableType.DOUBLE),"random.decimal","Random decimal in [min, max]. Inputs: min, max.");

    public final String name;
    public final VariableType type;
    public final List<VariableType> constTypes;
    public final String inGameName;
    public final String description;

    Placeholder(String name, VariableType type, List<VariableType> constTypes,
                String inGameName, String description) {
        this.name = name;
        this.type = type;
        this.constTypes = constTypes;
        this.inGameName = inGameName;
        this.description = description;
    }

    public static final Map<String, Placeholder> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(a -> a.name, Function.identity()));

    public static Placeholder searchByName(String name) { return BY_NAME.get(name); }

    public static Placeholder requireByName(String name) {
        Placeholder a = BY_NAME.get(name);
        if (a == null) throw new IllegalArgumentException("Unknown Placeholder name: " + name);
        return a;
    }
}
