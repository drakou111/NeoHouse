package com.drakou111.neohouse.constants;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum RestrictionAction {
    NONE              (block(AddAction.CANCEL_EVENT, AddAction.EXIT, AddAction.CLOSE_MENU)),
    CONDITIONAL       (block(AddAction.CONDITIONAL, AddAction.CANCEL_EVENT, AddAction.RANDOM, AddAction.CLOSE_MENU)),

    MENU              (block(AddAction.CANCEL_EVENT, AddAction.EXIT)),
    MENU_CONDITIONAL  (block(AddAction.CONDITIONAL, AddAction.CANCEL_EVENT, AddAction.RANDOM)),

    EVENT_JOIN             (block(AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_QUIT             (onlyAllow(AddAction.CONDITIONAL, AddAction.VARIABLE, AddAction.RANDOM, AddAction.TRIGGER_FUNCTION, AddAction.PAUSE)),
    EVENT_DEATH            (block(AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_KILL             (block(AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_RESPAWN          (block(AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_GROUP_CHANGE     (block(AddAction.CANCEL_EVENT, AddAction.CHANGE_GROUP, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_PVP_STATE_CHANGE (block(AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_FISH_CAUGHT      (block(AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_ENTER_PORTAL     (block(AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_DAMAGE           (block(AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_BLOCK_BREAK      (block(AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_START_PARKOUR    (block(AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_END_PARKOUR      (block(AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_DROP_ITEM        (block(AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_PICK_UP_ITEM     (block(AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_CHANGE_HELD_ITEM (block(AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_TOGGLE_SNEAK     (block(AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),
    EVENT_TOGGLE_FLIGHT    (block(AddAction.KILL, AddAction.SEND_LOBBY, AddAction.EXIT, AddAction.CLOSE_MENU)),

    EVENT_CONDITION_JOIN             (block(AddAction.CONDITIONAL, AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_QUIT             (onlyAllow(AddAction.VARIABLE, AddAction.TRIGGER_FUNCTION, AddAction.EXIT, AddAction.PAUSE)),
    EVENT_CONDITION_DEATH            (block(AddAction.CONDITIONAL, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_KILL             (block(AddAction.CONDITIONAL, AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_RESPAWN          (block(AddAction.CONDITIONAL, AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_GROUP_CHANGE     (block(AddAction.CONDITIONAL, AddAction.CANCEL_EVENT, AddAction.CHANGE_GROUP, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_PVP_STATE_CHANGE (block(AddAction.CONDITIONAL, AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_FISH_CAUGHT      (block(AddAction.CONDITIONAL, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_ENTER_PORTAL     (block(AddAction.CONDITIONAL, AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_DAMAGE           (block(AddAction.CONDITIONAL, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_BLOCK_BREAK      (block(AddAction.CONDITIONAL, AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_START_PARKOUR    (block(AddAction.CONDITIONAL, AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_END_PARKOUR      (block(AddAction.CONDITIONAL, AddAction.CANCEL_EVENT, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_DROP_ITEM        (block(AddAction.CONDITIONAL, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_PICK_UP_ITEM     (block(AddAction.CONDITIONAL, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_CHANGE_HELD_ITEM (block(AddAction.CONDITIONAL, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_TOGGLE_SNEAK     (block(AddAction.CONDITIONAL, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU)),
    EVENT_CONDITION_TOGGLE_FLIGHT    (block(AddAction.CONDITIONAL, AddAction.KILL, AddAction.SEND_LOBBY, AddAction.RANDOM, AddAction.CLOSE_MENU));

    private final Set<AddAction> blockedActions;

    RestrictionAction(Set<AddAction> blockedActions) {
        this.blockedActions = blockedActions;
    }

    public boolean isAllowed(AddAction action) {
        return !blockedActions.contains(action);
    }

    public RestrictionAction conditionalVariant() {
        String n = name();
        if (n.startsWith("EVENT_") && !n.startsWith("EVENT_CONDITION_")) {
            return RestrictionAction.valueOf(n.replace("EVENT_", "EVENT_CONDITION_"));
        }
        if (this == MENU) return MENU_CONDITIONAL;
        return CONDITIONAL;
    }

    public RestrictionCondition conditionContext() {
        String n = name();
        if (n.startsWith("EVENT_CONDITION_")) n = n.replace("EVENT_CONDITION_", "EVENT_");
        try {
            return RestrictionCondition.valueOf(n);
        } catch (IllegalArgumentException e) {
            return RestrictionCondition.NONE;
        }
    }

    private static Set<AddAction> block(AddAction... actions) {
        return Set.of(actions);
    }

    private static Set<AddAction> onlyAllow(AddAction... allowed) {
        Set<AddAction> allowedSet = Set.of(allowed);
        return Arrays.stream(AddAction.values())
                .filter(a -> !allowedSet.contains(a))
                .collect(Collectors.toUnmodifiableSet());
    }
}
