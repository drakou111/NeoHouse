package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

import java.util.EnumMap;
import java.util.Map;

/****************\
 *   ADD ACTION   *
 \****************/

public enum AddCondition {
    GROUP           (Map.of(RestrictionCondition.NONE, new PageSlot(0, 10))),
    VARIABLE        (Map.of(RestrictionCondition.NONE, new PageSlot(0, 11))),
    PERMISSION      (Map.of(RestrictionCondition.NONE, new PageSlot(0, 12))),
    REGION          (Map.of(RestrictionCondition.NONE, new PageSlot(0, 13))),
    HAS_ITEM        (Map.of(RestrictionCondition.NONE, new PageSlot(0, 14))),
    DOING_PARKOUR   (Map.of(RestrictionCondition.NONE, new PageSlot(0, 15))),
    POTION          (Map.of(RestrictionCondition.NONE, new PageSlot(0, 16))),
    SNEAKING        (Map.of(RestrictionCondition.NONE, new PageSlot(0, 19))),
    FLYING          (Map.of(RestrictionCondition.NONE, new PageSlot(0, 20))),
    HEALTH          (Map.of(RestrictionCondition.NONE, new PageSlot(0, 21))),
    MAX_HEALTH      (Map.of(RestrictionCondition.NONE, new PageSlot(0, 22))),
    HUNGER          (Map.of(RestrictionCondition.NONE, new PageSlot(0, 23))),
    GAME_MODE       (Map.of(RestrictionCondition.NONE, new PageSlot(0, 24))),
    PLACEHOLDER     (Map.of(RestrictionCondition.NONE, new PageSlot(0, 25))),
    TEAM            (Map.of(RestrictionCondition.NONE, new PageSlot(0, 28)));

    // TODO  (eg: events)
    //...

    private final EnumMap<RestrictionCondition, PageSlot> map;

    AddCondition(Map<RestrictionCondition, PageSlot> m) {
        this.map = new EnumMap<>(RestrictionCondition.class);
        this.map.putAll(m);
    }

    public int getPage(RestrictionCondition restriction) {
        return getPageSlot(restriction).page;
    }

    public int getSlotID(RestrictionCondition restriction) {
        return getPageSlot(restriction).slotID;
    }

    public PageSlot getPageSlot(RestrictionCondition restriction) {
        PageSlot ps = map.get(restriction);
        if (ps == null) {
            throw new IllegalArgumentException("No PageSlot defined for " + this.name() + " under " + restriction);
        }
        return ps;
    }
}

