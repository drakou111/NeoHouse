package com.drakou111.neohouse.constants;


import com.drakou111.neohouse.utils.PageSlot;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Event {
    JOIN("onJoin", new PageSlot(0, 10)),
    QUIT("onQuit", new PageSlot(0, 11)),
    DEATH("onDeath", new PageSlot(0, 12)),
    KILL("onKill", new PageSlot(0, 13)),
    RESPAWN("onRespawn", new PageSlot(0, 14)),
    GROUP_CHANGE("onGroupChange", new PageSlot(0, 15)),
    PVP_STATE_CHANGE("onPvpStateChange", new PageSlot(0, 16)),
    FISH_CAUGHT("onFishCaught", new PageSlot(0, 19)),
    ENTER_PORTAL("onEnterPortal", new PageSlot(0, 20)),
    DAMAGE("onDamage", new PageSlot(0, 21)),
    BLOCK_BREAK("onBlockBreak", new PageSlot(0, 22)),
    START_PARKOUR("onStartParkour", new PageSlot(0, 23)),
    END_PARKOUR("onEndParkour", new PageSlot(0, 24)),
    DROP_ITEM("onDropItem", new PageSlot(0, 25)),
    PICK_UP_ITEM("onPickUpItem", new PageSlot(0, 28)),
    CHANGE_HELD_ITEM("onChangeHeldItem", new PageSlot(0, 29)),
    TOGGLE_SNEAK("onToggleSneak", new PageSlot(0, 30)),
    TOGGLE_FLIGHT("onToggleFlight", new PageSlot(0, 31));

    public final PageSlot pageSlot;
    public final String name;

    Event(String name, PageSlot pageSlot) {
        this.name = name;
        this.pageSlot = pageSlot;
    }

    private static final Map<String, Event> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(e -> e.name, Function.identity()));

    public static Event fromName(String name) {
        return BY_NAME.get(name);
    }
    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
