package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

public enum Lobby {
    MAIN_LOBBY(new PageSlot(0, 10)),
    TOURNAMENT_HALL(new PageSlot(0, 11)),
    BLITZ_SG(new PageSlot(0, 12)),
    THE_TNT_GAMES(new PageSlot(0, 13)),
    MEGA_WALLS(new PageSlot(0, 14)),
    ARCADE_GAMES(new PageSlot(0, 15)),
    COPS_AND_CRIMS(new PageSlot(0, 16)),
    UHC_CHAMPIONS(new PageSlot(0, 19)),
    WARLORDS(new PageSlot(0, 20)),
    SMASH_HEROES(new PageSlot(0, 21)),
    HOUSING(new PageSlot(0, 22)),
    SKYWARS(new PageSlot(0, 23)),
    SPEED_UHC(new PageSlot(0, 24)),
    CLASSIC_GAMES(new PageSlot(0, 25)),
    PROTOTYPE(new PageSlot(0, 28)),
    BED_WARS(new PageSlot(0, 29)),
    MURDER_MYSTERY(new PageSlot(0, 30)),
    BUILD_BATTLE(new PageSlot(0, 31)),
    DUELS(new PageSlot(0, 32)),
    WOOL_GAMES(new PageSlot(0, 33));

    public final PageSlot pageSlot;

    Lobby(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
