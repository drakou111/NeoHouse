package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.utils.PageSlot;

public enum Permission {
    FLY(new PageSlot(0, 10)),
    WOOD_DOOR(new PageSlot(0, 11)),
    IRON_DOOR(new PageSlot(0, 12)),
    WOOD_TRAP_DOOR(new PageSlot(0, 13)),
    IRON_TRAP_DOOR(new PageSlot(0, 14)),
    FENCE_GATE(new PageSlot(0, 15)),
    BUTTON(new PageSlot(0, 16)),
    LEVER(new PageSlot(0, 19)),
    USE_LAUNCH_PADS(new PageSlot(0, 20)),
    TP(new PageSlot(0, 21)),
    TP_OTHER_PLAYERS(new PageSlot(0, 22)),
    JUKEBOX(new PageSlot(0, 23)),
    KICK(new PageSlot(0, 24)),
    BAN(new PageSlot(0, 25)),
    MUTE(new PageSlot(0, 28)),
    PET_SPAWNING(new PageSlot(0, 29)),
    BUILD(new PageSlot(0, 30)),
    OFFLINE_BUILD(new PageSlot(0, 31)),
    FLUID(new PageSlot(0, 32)),
    PRO_TOOLS(new PageSlot(0, 33)),
    USE_CHESTS(new PageSlot(0, 34)),
    USE_ENDER_CHESTS(new PageSlot(1, 10)),
    ITEM_EDITOR(new PageSlot(1, 11)),
    SWITCH_GAME_MODE(new PageSlot(1, 12)),
    EDIT_VARIABLES(new PageSlot(1, 13)),
    CHANGE_PLAYER_GROUP(new PageSlot(1, 14)),
    CHANGE_GAME_RULES(new PageSlot(1, 15)),
    HOUSING_MENU(new PageSlot(1, 16)),
    TEAM_CHAT_SPY(new PageSlot(1, 19)),
    EDIT_ACTIONS(new PageSlot(1, 20)),
    EDIT_REGIONS(new PageSlot(1, 21)),
    EDIT_SCOREBOARD(new PageSlot(1, 22)),
    EDIT_EVENT_ACTIONS(new PageSlot(1, 23)),
    EDIT_COMMANDS(new PageSlot(1, 24)),
    EDIT_FUNCTIONS(new PageSlot(1, 25)),
    EDIT_INVENTORY_LAYOUTS(new PageSlot(1, 28)),
    EDIT_TEAMS(new PageSlot(1, 29)),
    EDIT_CUSTOM_MENUS(new PageSlot(1, 30)),
    VIEW_ANALYTICS(new PageSlot(1, 31)),
    ITEM_MAILBOX(new PageSlot(1, 32)),
    ITEM_EGG_HUNT(new PageSlot(1, 33)),
    ITEM_TELEPORT_PAD(new PageSlot(1, 34)),
    ITEM_LAUNCH_PAD(new PageSlot(2, 10)),
    ITEM_ACTION_PAD(new PageSlot(2, 11)),
    ITEM_HOLOGRAM(new PageSlot(2, 12)),
    ITEM_NPC(new PageSlot(2, 13)),
    ITEM_ACTION_BUTTON(new PageSlot(2, 14)),
    ITEM_LEADERBOARD(new PageSlot(2, 15)),
    ITEM_TRASH_CAN(new PageSlot(2, 16)),
    ITEM_BIOME_STICK(new PageSlot(2, 19));


    public final PageSlot pageSlot;

    Permission(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
