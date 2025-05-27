package com.drakou111.neohouse.compilers.ast.enums;


import java.util.HashMap;
import java.util.Map;

public enum Event {
    JOIN("onJoin"),
    QUIT("onQuit"),
    DEATH("onDeath"),
    KILL("onKill"),
    RESPAWN("onRespawn"),
    GROUP_CHANGE("onGroupChange"),
    PVP_STATE_CHANGE("onPvpStateChange"),
    FISH_CAUGHT("onFishCaught"),
    ENTER_PORTAL("onEnterPortal"),
    DAMAGE("onDamage"),
    BLOCK_BREAK("onBlockBreak"),
    START_PARKOUR("onStartParkour"),
    END_PARKOUR("onEndParkour"),
    DROP_ITEM("onDropItem"),
    PICK_UP_ITEM("onPickUpItem"),
    CHANGE_HELD_ITEM("onChangeHeldItem"),
    TOGGLE_SNEAK("onToggleSneak"),
    TOGGLE_FLIGHT("onToggleFlight");

    public final String name;

    Event(String name) {
        this.name = name;
    }

    private static final Map<String, Event> BY_NAME = new HashMap<>();

    static {
        for (Event e : values()) {
            BY_NAME.put(e.name, e);
        }
    }

    public static Event fromName(String name) {
        return BY_NAME.get(name);
    }
}
