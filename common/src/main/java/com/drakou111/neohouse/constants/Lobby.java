package com.drakou111.neohouse.constants;

public enum Lobby {
    MAIN_LOBBY("Main Lobby"),
    TOURNAMENT_HALL("Tournament Hall"),
    BLITZ_SG("Blitz SG"),
    THE_TNT_GAMES("The TNT Games"),
    MEGA_WALLS("Mega Walls"),
    ARCADE_GAMES("Arcade Games"),
    COPS_AND_CRIMS("Cops and Crims"),
    UHC_CHAMPIONS("UHC Champions"),
    WARLORDS("Warlords"),
    SMASH_HEROES("Smash Heroes"),
    HOUSING("Housing"),
    SKYWARS("SkyWars"),
    SPEED_UHC("Speed UHC"),
    CLASSIC_GAMES("Classic Games"),
    PROTOTYPE("Prototype"),
    BED_WARS("Bed Wars"),
    MURDER_MYSTERY("Murder Mystery"),
    BUILD_BATTLE("Build Battle"),
    DUELS("Duels"),
    WOOL_GAMES("Wool Games");

    public final String displayName;

    Lobby(String displayName) {
        this.displayName = displayName;
    }
}
