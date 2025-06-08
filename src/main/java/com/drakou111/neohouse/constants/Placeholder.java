package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.enums.VariableType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Placeholder {
    SERVER_NAME("serverName", VariableType.STRING, List.of()),
    SERVER_SHORT_NAME("serverShortName", VariableType.STRING, List.of()),
    PLAYER_NAME("playerName", VariableType.STRING, List.of()),
    PLAYER_PING("playerPing", VariableType.STRING, List.of()),
    PLAYER_HEALTH("playerHealth", VariableType.DOUBLE, List.of()),
    PLAYER_MAX_HEALTH("playerMaxHealth", VariableType.DOUBLE, List.of()),
    PLAYER_HUNGER("playerHunger", VariableType.DOUBLE, List.of()),
    PLAYER_EXPERIENCE("playerExperience", VariableType.LONG, List.of()),
    PLAYER_LEVEL("playerLevel", VariableType.LONG, List.of()),
    PLAYER_VERSION("playerVersion", VariableType.STRING, List.of()),
    PLAYER_PROTOCOL("playerProtocol", VariableType.LONG, List.of()), //unsure of return type..
    PLAYER_GAMEMODE("playerGamemode", VariableType.STRING, List.of()),
    PLAYER_REGION_NAME("playerRegionName", VariableType.STRING, List.of()),
    PLAYER_POS_X("playerPosX", VariableType.DOUBLE, List.of()),
    PLAYER_POS_Y("playerPosY", VariableType.DOUBLE, List.of()),
    PLAYER_POS_Z("playerPosZ", VariableType.DOUBLE, List.of()),
    PLAYER_POS_YAW("playerPosYaw", VariableType.DOUBLE, List.of()),
    PLAYER_POS_PITCH("playerPosPitch", VariableType.DOUBLE, List.of()),
    PLAYER_BLOCK_X("playerBlockX", VariableType.LONG, List.of()),
    PLAYER_BLOCK_Y("playerBlockY", VariableType.LONG, List.of()),
    PLAYER_BLOCK_Z("playerBlockZ", VariableType.LONG, List.of()),
    PLAYER_GROUP_NAME("playerGroupName", VariableType.STRING, List.of()),
    PLAYER_GROUP_TAG("playerGroupTag", VariableType.STRING, List.of()),
    PLAYER_GROUP_PRIORITY("playerGroupPriority", VariableType.LONG, List.of()),
    PLAYER_GROUP_COLOR("playerGroupColor", VariableType.STRING, List.of()),
    PLAYER_TEAM_NAME("playerTeamName", VariableType.STRING, List.of()),
    PLAYER_TEAM_TAG("playerTeamTag", VariableType.STRING, List.of()),
    PLAYER_TEAM_COLOR("playerTeamColor", VariableType.STRING, List.of()),
    PLAYER_TEAM_PLAYERS("playerTeamPlayers", VariableType.LONG, List.of(VariableType.STRING)),
    PLAYER_PARKOUR_TICKS("playerParkourTicks", VariableType.LONG, List.of()),
    PLAYER_PARKOUR_FORMATTED("playerParkourFormatted", VariableType.STRING, List.of()),
    HOUSE_NAME("houseName", VariableType.STRING, List.of()),
    HOUSE_COOKIES("houseCookies", VariableType.LONG, List.of()),
    HOUSE_VISITING_RULES("houseVisitingRules", VariableType.STRING, List.of()),
    HOUSE_PLAYERS("housePlayers", VariableType.LONG, List.of()),
    DATE_DAY("dateDay", VariableType.LONG, List.of()),
    DATE_MONTH("dateMonth", VariableType.LONG, List.of()),
    DATE_YEAR("dateYear", VariableType.LONG, List.of()),
    DATE_HOUR("dateHour", VariableType.LONG, List.of()),
    DATE_MINUTE("dateMinute", VariableType.LONG, List.of()),
    DATE_SECONDS("dateSeconds", VariableType.LONG, List.of()),
    DATE_DAY_TIMEZONE("dateDayTimezone", VariableType.LONG, List.of(VariableType.STRING)),
    DATE_MONTH_TIMEZONE("dateMonthTimezone", VariableType.LONG, List.of(VariableType.STRING)),
    DATE_YEAR_TIMEZONE("dateYearTimezone", VariableType.LONG, List.of(VariableType.STRING)),
    DATE_HOUR_TIMEZONE("dateHourTimezone", VariableType.LONG, List.of(VariableType.STRING)),
    DATE_MINUTE_TIMEZONE("dateMinuteTimezone", VariableType.LONG, List.of(VariableType.STRING)),
    DATE_SECONDS_TIMEZONE("dateSecondsTimezone", VariableType.LONG, List.of(VariableType.STRING)),
    DATE_UNIX("dateUnix", VariableType.LONG, List.of()),
    DATE_UNIX_MS("dateUnixMs", VariableType.LONG, List.of()),
    RANDOM_WHOLE("randomWhole", VariableType.LONG, List.of()),
    RANDOM_WHOLE_CAPPED("randomWholeCapped", VariableType.LONG, List.of(VariableType.LONG)),
    RANDOM_WHOLE_RANGED("randomWholeRanged", VariableType.LONG, List.of(VariableType.LONG, VariableType.LONG)),
    RANDOM_DECIMAL("randomDecimal", VariableType.DOUBLE, List.of()),
    RANDOM_DECIMAL_CAPPED("randomDecimalCapped", VariableType.DOUBLE, List.of(VariableType.DOUBLE)),
    RANDOM_DECIMAL_RANGED("randomDecimalRanged", VariableType.DOUBLE, List.of(VariableType.DOUBLE, VariableType.DOUBLE));

    public final String name;
    public final VariableType type;
    public final List<VariableType> constTypes;

    Placeholder(String name, VariableType type, List<VariableType> constTypes) {
        this.name = name;
        this.type = type;
        this.constTypes = constTypes;
    }

    public static final Map<String, Placeholder> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(a -> a.name, Function.identity()));

    public static Placeholder searchByName(String name) {
        return BY_NAME.get(name);
    }

    public static Placeholder requireByName(String name) {
        Placeholder a = BY_NAME.get(name);
        if (a == null) throw new IllegalArgumentException("Unknown Placeholder name: " + name);
        return a;
    }
}
