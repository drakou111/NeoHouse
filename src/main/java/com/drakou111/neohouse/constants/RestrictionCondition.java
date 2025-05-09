package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

import java.util.List;

public enum RestrictionCondition {
    NONE(List.of(AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),

    COMMAND(List.of(AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),

    EVENT_JOIN(List.of(AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_QUIT(List.of(AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_DEATH(List.of(AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_KILL(List.of(AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_RESPAWN(List.of(AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_GROUP_CHANGE(List.of(AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_PVP_STATE_CHANGE(List.of(AddCondition.PVP_ENABLED, AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_FISH_CAUGHT(List.of(AddCondition.FISHING_ENVIRONMENT, AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_ENTER_PORTAL(List.of(AddCondition.PORTAL_TYPE, AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_DAMAGE(List.of(AddCondition.DAMAGE_CAUSE, AddCondition.DAMAGE_AMOUNT, AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_BREAK(List.of(AddCondition.BLOCK_TYPE, AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_START_PARKOUR(List.of(AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_END_PARKOUR(List.of(AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_DROP_ITEM(List.of(AddCondition.IS_ITEM, AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_PICK_UP_ITEM(List.of(AddCondition.IS_ITEM, AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_CHANGE_HELD_ITEM(List.of(AddCondition.IS_ITEM, AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_TOGGLE_SNEAK(List.of(AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM)),
    EVENT_TOGGLE_FLIGHT(List.of(AddCondition.GROUP, AddCondition.VARIABLE, AddCondition.PERMISSION, AddCondition.REGION, AddCondition.HAS_ITEM, AddCondition.DOING_PARKOUR, AddCondition.POTION, AddCondition.SNEAKING, AddCondition.FLYING, AddCondition.HEALTH, AddCondition.MAX_HEALTH, AddCondition.HUNGER, AddCondition.GAME_MODE, AddCondition.PLACEHOLDER, AddCondition.TEAM));

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
