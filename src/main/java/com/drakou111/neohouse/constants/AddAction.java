package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

import java.util.EnumMap;
import java.util.Map;

/****************\
 *   ADD ACTION   *
 \****************/

public enum AddAction {
    CONDITIONAL         (Map.of(RestrictionAction.NONE, new PageSlot(0, 10),        RestrictionAction.CONDITIONAL_OR_RANDOM, null)),
    CHANGE_GROUP        (Map.of(RestrictionAction.NONE, new PageSlot(0, 11),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 10))),
    KILL                (Map.of(RestrictionAction.NONE, new PageSlot(0, 12),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 11))),
    FULL_HEAL           (Map.of(RestrictionAction.NONE, new PageSlot(0, 13),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 12))),
    TITLE               (Map.of(RestrictionAction.NONE, new PageSlot(0, 14),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 13))),
    ACTION_BAR          (Map.of(RestrictionAction.NONE, new PageSlot(0, 15),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 14))),
    CLEAR_INVENTORY     (Map.of(RestrictionAction.NONE, new PageSlot(0, 16),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 15))),
    MAX_HEALTH          (Map.of(RestrictionAction.NONE, new PageSlot(0, 19),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 16))),
    PARKOUR_CHECKPOINT  (Map.of(RestrictionAction.NONE, new PageSlot(0, 20),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 19))),
    GIVE_ITEM           (Map.of(RestrictionAction.NONE, new PageSlot(0, 21),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 20))),
    REMOVE_ITEM         (Map.of(RestrictionAction.NONE, new PageSlot(0, 22),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 21))),
    CHAT_MESSAGE        (Map.of(RestrictionAction.NONE, new PageSlot(0, 23),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 22))),
    APPLY_POTION        (Map.of(RestrictionAction.NONE, new PageSlot(0, 24),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 23))),
    CLEAR_POTION        (Map.of(RestrictionAction.NONE, new PageSlot(0, 25),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 24))),
    GIVE_XP             (Map.of(RestrictionAction.NONE, new PageSlot(0, 28),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 25))),
    SEND_LOBBY          (Map.of(RestrictionAction.NONE, new PageSlot(0, 29),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 28))),
    VARIABLE            (Map.of(RestrictionAction.NONE, new PageSlot(0, 30),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 29))),
    TELEPORT            (Map.of(RestrictionAction.NONE, new PageSlot(0, 31),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 30))),
    FAIL_PARKOUR        (Map.of(RestrictionAction.NONE, new PageSlot(0, 32),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 31))),
    PLAY_SOUND          (Map.of(RestrictionAction.NONE, new PageSlot(0, 33),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 32))),
    COMPASS_TARGET      (Map.of(RestrictionAction.NONE, new PageSlot(0, 34),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 33))),
    SET_GAME_MODE       (Map.of(RestrictionAction.NONE, new PageSlot(1, 10),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(0, 34))),
    CHANGE_HEALTH       (Map.of(RestrictionAction.NONE, new PageSlot(1, 11),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 10))),
    CHANGE_HUNGER       (Map.of(RestrictionAction.NONE, new PageSlot(1, 12),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 11))),
    RANDOM              (Map.of(RestrictionAction.NONE, new PageSlot(1, 13),        RestrictionAction.CONDITIONAL_OR_RANDOM, null)),
    TRIGGER_FUNCTION    (Map.of(RestrictionAction.NONE, new PageSlot(1, 14),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 12))),
    APPLY_LAYOUT        (Map.of(RestrictionAction.NONE, new PageSlot(1, 15),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 13))),
    EXIT                (Map.of(RestrictionAction.NONE, null,                               RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 14))),
    ENCHANT_HELD_ITEM   (Map.of(RestrictionAction.NONE, new PageSlot(1, 16),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 15))),
    PAUSE               (Map.of(RestrictionAction.NONE, new PageSlot(1, 19),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 16))),
    SET_TEAM            (Map.of(RestrictionAction.NONE, new PageSlot(1, 20),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 19))),
    DISPLAY_MENU        (Map.of(RestrictionAction.NONE, new PageSlot(1, 21),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 20))),
    DROP_ITEM           (Map.of(RestrictionAction.NONE, new PageSlot(1, 22),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 21))),
    CHANGE_VELOCITY     (Map.of(RestrictionAction.NONE, new PageSlot(1, 23),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 22))),
    LAUNCH_TO_TARGET    (Map.of(RestrictionAction.NONE, new PageSlot(1, 24),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 23))),
    SET_WEATHER         (Map.of(RestrictionAction.NONE, new PageSlot(1, 25),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 24))),
    SET_TIME            (Map.of(RestrictionAction.NONE, new PageSlot(1, 28),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 25))),
    NAME_TAG_DISPLAY    (Map.of(RestrictionAction.NONE, new PageSlot(1, 29),        RestrictionAction.CONDITIONAL_OR_RANDOM, new PageSlot(1, 28)));

    // TODO  (eg: events)
    //...

    private final EnumMap<RestrictionAction, PageSlot> map;

    AddAction(Map<RestrictionAction, PageSlot> m) {
        this.map = new EnumMap<>(RestrictionAction.class);
        this.map.putAll(m);
    }

    public int getPage(RestrictionAction restriction) {
        return getPageSlot(restriction).page;
    }

    public int getSlotID(RestrictionAction restriction) {
        return getPageSlot(restriction).slotID;
    }

    public PageSlot getPageSlot(RestrictionAction restriction) {
        PageSlot ps = map.get(restriction);
        if (ps == null) {
            throw new IllegalArgumentException("No PageSlot defined for " + this.name() + " under " + restriction);
        }
        return ps;
    }
}
