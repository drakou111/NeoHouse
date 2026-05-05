package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.utils.PageSlot;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Event {
    JOIN("onJoin",               new PageSlot(0, 10), "Fires when a player joins the housing.",                     "oak_door"),
    QUIT("onQuit",               new PageSlot(0, 11), "Fires when a player leaves the housing.",                    "dark_oak_door"),
    DEATH("onDeath",             new PageSlot(0, 12), "Fires when the player dies.",                                "bone"),
    KILL("onKill",               new PageSlot(0, 13), "Fires when the player kills another player.",                "diamond_sword"),
    RESPAWN("onRespawn",         new PageSlot(0, 14), "Fires when the player respawns after death.",                "apple"),
    GROUP_CHANGE("onGroupChange",      new PageSlot(0, 15), "Fires when the player's group/rank changes.",          "paper"),
    PVP_STATE_CHANGE("onPvpStateChange", new PageSlot(0, 16), "Fires when PvP is toggled on or off.",              "iron_sword"),
    FISH_CAUGHT("onFishCaught",        new PageSlot(0, 19), "Fires when the player catches a fish.",               "fishing_rod"),
    ENTER_PORTAL("onEnterPortal",      new PageSlot(0, 20), "Fires when the player enters a portal.",              "obsidian"),
    DAMAGE("onDamage",           new PageSlot(0, 21), "Fires when the player takes any damage.",                    "lava_bucket"),
    BLOCK_BREAK("onBlockBreak",        new PageSlot(0, 22), "Fires when the player breaks a block.",               "grass_block"),
    START_PARKOUR("onStartParkour",    new PageSlot(0, 23), "Fires when the player starts a parkour run.",         "light_weighted_pressure_plate"),
    END_PARKOUR("onEndParkour",        new PageSlot(0, 24), "Fires when the player completes a parkour run.",      "light_weighted_pressure_plate"),
    DROP_ITEM("onDropItem",            new PageSlot(0, 25), "Fires when the player drops an item.",                "dropper"),
    PICK_UP_ITEM("onPickUpItem",       new PageSlot(0, 28), "Fires when the player picks up an item.",             "hopper"),
    CHANGE_HELD_ITEM("onChangeHeldItem", new PageSlot(0, 29), "Fires when the player's held item slot changes.",   "book"),
    TOGGLE_SNEAK("onToggleSneak",      new PageSlot(0, 30), "Fires when the player starts or stops sneaking.",     "hay_block"),
    TOGGLE_FLIGHT("onToggleFlight",    new PageSlot(0, 31), "Fires when the player starts or stops flying.",       "feather");

    public final String name;
    public final PageSlot pageSlot;
    public final String description;
    public final String texture;

    Event(String name, PageSlot pageSlot, String description, String textureId) {
        this.name = name;
        this.pageSlot = pageSlot;
        this.description = description;
        this.texture = textureId;
    }

    private static final Map<String, Event> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(e -> e.name, Function.identity()));

    public static Event fromName(String name) { return BY_NAME.get(name); }

    public int getPage()   { return this.pageSlot.page; }
    public int getSlotID() { return this.pageSlot.slotID; }
}
