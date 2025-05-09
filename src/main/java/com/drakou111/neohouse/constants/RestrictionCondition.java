package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

import java.util.List;

public enum RestrictionCondition {
    NONE(List.of()),
    EVENT_JOIN(List.of()),
    EVENT_QUIT(List.of()),
    EVENT_DEATH(List.of()),
    EVENT_KILL(List.of()),
    EVENT_RESPAWN(List.of()),
    EVENT_GROUP_CHANGE(List.of()),
    EVENT_PVP_STATE_CHANGE(List.of()),
    EVENT_FISH_CAUGHT(List.of()),
    EVENT_ENTER_PORTAL(List.of()),
    EVENT_DAMAGE(List.of()),
    EVENT_BREAK(List.of()),
    EVENT_START_PARKOUR(List.of()),
    EVENT_END_PARKOUR(List.of()),
    EVENT_DROP_ITEM(List.of()),
    EVENT_PICK_UP_ITEM(List.of()),
    EVENT_CHANGE_HELD_ITEM(List.of()),
    EVENT_TOGGLE_SNEAK(List.of()),
    EVENT_TOGGLE_FLIGHT(List.of());

    private final List<AddCondition> allowedConditions;
    RestrictionCondition(List<AddCondition> allowedConditions) {
        this.allowedConditions = allowedConditions;
    }

    public PageSlot pageSlotFor(AddCondition condition) {
        int idx = allowedConditions.indexOf(condition);
        if (idx < 0) return null;
        int slotId = Slots.GENERAL_LIST_H6[idx % Slots.GENERAL_LIST_H6.length];
        int page = idx / Slots.GENERAL_LIST_H6.length;
        return new PageSlot(page, slotId);
    }
}
