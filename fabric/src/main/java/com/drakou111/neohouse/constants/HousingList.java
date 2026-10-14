package com.drakou111.neohouse.constants;

import java.util.Optional;

/** The Housing lists NeoHouse reads entries from, with how to open them and how many entries Housing allows. */
public enum HousingList {
    FUNCTIONS(Commands.FUNCTIONS, null, 300, true, "", "function"),
    COMMANDS(Commands.COMMANDS, null, 50, true, "/", "command"),
    MENUS(Commands.CUSTOM_MENUS, null, 200, true, "", "menu"),
    TEAMS(Commands.TEAMS, null, 14, false, "", "team"),
    // Housing doesn't document a layout limit; the list pages like functions.
    LAYOUTS(Commands.LAYOUTS, null, Integer.MAX_VALUE, true, "", "layout"),
    GROUPS(Commands.MENU, "Permissions and Groups", 20, false, "", "group");

    /** Chat command that opens the list, or the menu holding {@link #openThrough}. */
    public final String openCommand;
    private final String openThrough;
    public final int maxEntries;
    /** Whether the list can span several pages (a next-page arrow in the last slot). */
    public final boolean paginated;
    /** What the list shows in front of an entry's name (commands are listed as {@code /name}). */
    private final String displayPrefix;
    public final String singularName;

    HousingList(String openCommand, String openThrough, int maxEntries, boolean paginated, String displayPrefix, String singularName) {
        this.openCommand = openCommand;
        this.openThrough = openThrough;
        this.maxEntries = maxEntries;
        this.paginated = paginated;
        this.displayPrefix = displayPrefix;
        this.singularName = singularName;
    }

    /** The item to click in {@link #openCommand}'s menu to reach the list, when the command doesn't open it directly. */
    public Optional<String> openThrough() {
        return Optional.ofNullable(this.openThrough);
    }

    /** The item name an entry called {@code name} has in the list. */
    public String displayName(String name) {
        return this.displayPrefix + name;
    }

    /** The entry's own name, given the item name it has in the list ({@code /spawn} is command {@code spawn}). */
    public String nameOf(String displayed) {
        return displayed.startsWith(this.displayPrefix) ? displayed.substring(this.displayPrefix.length()) : displayed;
    }
}
